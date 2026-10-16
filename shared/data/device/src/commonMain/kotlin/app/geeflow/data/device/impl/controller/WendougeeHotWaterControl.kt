package app.geeflow.data.device.impl.controller

import app.geeflow.data.device.DeviceHotWaterControl
import app.geeflow.data.device.model.DeviceCapability
import app.geeflow.data.device.model.DeviceState.BrewStatus
import kotlinx.coroutines.sync.withLock

internal class WendougeeHotWaterControl(private val owner: WendougeeController) : DeviceHotWaterControl {
    override suspend fun start() = owner.operationMutex.withLock {
        check(DeviceCapability.HotWaterDispensing in owner.capabilities)
        val active = owner.requireSession()
        owner.refreshBrewStatus(active)
        check(owner.mutableState.value.brewStatus == BrewStatus.Idle) { "Machine is busy" }
        active.modbus.writeSingleRegister(WendougeeRegisters.HOT_WATER, 1)
    }

    override suspend fun stop() = owner.operationMutex.withLock {
        check(DeviceCapability.HotWaterDispensing in owner.capabilities)
        owner.requireSession().modbus.writeSingleRegister(WendougeeRegisters.HOT_WATER, 0)
    }
}
