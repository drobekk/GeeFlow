@file:Suppress("TooManyFunctions")

package app.geeflow.data.device.impl.controller

import app.geeflow.data.device.DeviceAccessoryControl
import app.geeflow.data.device.impl.controller.WendougeeCommands.CMD_SCALE_SCAN
import app.geeflow.data.device.impl.controller.WendougeeCommands.CMD_SCALE_SEARCH_QUERY
import co.touchlab.kermit.Logger
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout

internal class WendougeeAccessoryControl(private val owner: WendougeeController) : DeviceAccessoryControl {
    private val deviceState get() = owner.mutableState
    private val integrationFlags get() = owner.accessoryFlags
    private val scaleSettingsMutex get() = owner.accessorySettingsMutex
    private val accessoryRefresh get() = owner.accessoryRefreshCoordinator
    private fun requireSession() = owner.requireSession()
    private suspend fun writeControl(active: WendougeeSession, bytes: ByteArray) = owner.writeControl(active, bytes)
    private suspend fun readIntegrationFlags(active: WendougeeSession) = owner.readIntegrationFlags(active)
    private suspend fun requestAccessoryStatus(active: WendougeeSession) = owner.requestAccessoryStatus(active)

    override suspend fun refreshAccessories() = accessoryRefresh.refresh {
        val active = requireSession()
        readIntegrationFlags(active)
        if (!deviceState.value.smartScaleEnabled && !deviceState.value.singleDoseGrinderEnabled) return@refresh
        owner.updateScales(listOfNotNull(deviceState.value.smartScale))
        owner.updateGrinders(listOfNotNull(deviceState.value.singleDoseGrinder))
        writeControl(active, CMD_SCALE_SCAN)
        requestAccessoryStatus(active)
    }

    override suspend fun setSingleDoseGrinderConnectivity(enabled: Boolean) {
        scaleSettingsMutex.withLock {
            val active = requireSession()
            val flags = integrationFlags.value ?: run {
                writeControl(active, CMD_SCALE_SEARCH_QUERY)
                withTimeout(SCALE_SETTINGS_TIMEOUT_MS) { integrationFlags.mapNotNull { it }.first() }
            }
            writeControl(active, buildIntegrationConnectivityFrame(flags, SINGLE_DOSE_ENABLED_MASK, enabled))
            owner.updateFlags(if (enabled) flags or SINGLE_DOSE_ENABLED_MASK else flags and SINGLE_DOSE_ENABLED_MASK.inv())
            owner.updateState {
                it.copy(
                    singleDoseGrinderEnabled = enabled,
                    singleDoseGrinder = if (enabled) it.singleDoseGrinder else null,
                    singleDoseGrinderSearchActive = enabled && it.singleDoseGrinderSearchActive,
                )
            }
            if (enabled) {
                delay(SCALE_SEARCH_AFTER_ENABLE_DELAY_MS)
                requestSingleDoseGrinderList()
            } else {
                owner.updateGrinders(emptyList())
            }
        }
    }

    override suspend fun requestSingleDoseGrinderList() = refreshAccessories()

    override suspend fun connectSingleDoseGrinder(name: String) {
        val active = requireSession()
        writeControl(active, buildScaleFrame(SCALE_CMD_CONNECT, name))
    }

    override suspend fun disconnectSingleDoseGrinder() {
        val name = deviceState.value.singleDoseGrinder?.name ?: return
        val active = requireSession()
        writeControl(active, buildScaleFrame(SCALE_CMD_DISCONNECT, name))
    }

    override suspend fun runSingleDoseGrinder(grindingSize: Int, grindingSpeed: Int) {
        val grinder = deviceState.value.singleDoseGrinder
        require(grinder?.isConnected == true) { "Single Dose grinder is disconnected" }
        val standing = requireNotNull(grinder.standing) { "Single Dose grinder status is unavailable" }
        require(grinder.operationStatus == null || grinder.operationStatus == 0) { "Single Dose grinder is busy" }
        val active = requireSession()
        writeControl(active, buildSingleDoseGrindFrame(standing, grindingSize, grindingSpeed))
    }

    override suspend fun setSmartScaleConnectivity(enabled: Boolean) {
        scaleSettingsMutex.withLock {
            val active = requireSession()
            val flags = integrationFlags.value ?: run {
                writeControl(active, CMD_SCALE_SEARCH_QUERY)
                withTimeout(SCALE_SETTINGS_TIMEOUT_MS) { integrationFlags.mapNotNull { it }.first() }
            }
            writeControl(active, buildScaleConnectivityFrame(flags, enabled))
            owner.updateFlags(if (enabled) flags or SCALE_ENABLED_MASK else flags and SCALE_ENABLED_MASK.inv())
            owner.updateState {
                it.copy(
                    smartScaleEnabled = enabled,
                    smartScale = if (enabled) it.smartScale else null,
                    smartScaleSearchActive = enabled && it.smartScaleSearchActive,
                    weight = if (enabled) it.weight else null,
                    weightRate = if (enabled) it.weightRate else null,
                )
            }
            if (enabled) {
                delay(SCALE_SEARCH_AFTER_ENABLE_DELAY_MS)
                requestSmartScaleList()
            } else {
                owner.updateScales(emptyList())
            }
        }
    }

    override suspend fun requestSmartScaleList() = refreshAccessories()

    override suspend fun connectSmartScale(name: String) {
        Logger.withTag(TAG).i { "Connecting smart scale: $name" }
        val active = requireSession()
        writeControl(active, buildScaleFrame(SCALE_CMD_CONNECT, name))
    }

    override suspend fun disconnectSmartScale() {
        val name = deviceState.value.smartScale?.name ?: run {
            Logger.withTag(TAG).w { "Disconnect called but no scale is connected" }
            return
        }
        Logger.withTag(TAG).i { "Disconnecting smart scale: $name" }
        val active = requireSession()
        writeControl(active, buildScaleFrame(SCALE_CMD_DISCONNECT, name))
    }
}
