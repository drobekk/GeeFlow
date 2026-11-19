package app.geeflow.presentation.feature.device.add

import app.geeflow.data.device.NearbyDevicesController
import app.geeflow.data.device.model.Device
import app.geeflow.data.device.model.DeviceConnection
import app.geeflow.platform.permissions.DeniedException
import app.geeflow.platform.permissions.PermissionBluetoothConnect
import app.geeflow.platform.permissions.PermissionBluetoothScan
import app.geeflow.platform.permissions.PermissionsController
import app.geeflow.presentation.feature.device.add.AddDeviceViewState.ManualEntry
import app.geeflow.presentation.feature.device.add.AddDeviceViewState.ManualError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch

/** Owns a bounded scan. Cancelling an older request cannot stop or overwrite a newer request. */
internal class ManualPairingController(
    private val scope: CoroutineScope,
    private val permissions: PermissionsController,
    private val nearby: NearbyDevicesController,
    private val read: () -> ManualEntry,
    private val update: (ManualEntry) -> Unit,
    private val confirm: (Device, Boolean) -> Unit,
) {
    private var job: Job? = null
    private var generation = 0

    fun stop() {
        generation++
        job?.cancel()
        job = null
        if (read().searching) nearby.stopScanning()
        update(read().copy(searching = false))
    }

    @Suppress("TooGenericExceptionCaught")
    fun search() {
        stop()
        val error = read().validationError()
        if (error != null) {
            update(read().copy(error = error))
            return
        }
        update(read().copy(searchStarted = true, searching = true, error = null, devices = emptyList()))
        val request = generation
        job = scope.launch {
            try {
                permissions.providePermission(PermissionBluetoothScan)
                permissions.providePermission(PermissionBluetoothConnect)
                ensureActive()
                nearby.startManualScanning(read().serialNumber.trim())
                delay(ManualScanDurationMs)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: DeniedException) {
                update(read().copy(error = ManualError.PermissionDenied))
            } catch (_: Exception) {
                update(read().copy(error = ManualError.ScanFailed))
            } finally {
                if (request == generation) {
                    nearby.stopScanning()
                    update(read().copy(searching = false))
                }
            }
        }
    }

    fun select(id: String) {
        val entry = read()
        val item = entry.devices.find { (it.device.connection as? DeviceConnection.Ble)?.peripheralId == id } ?: return
        val device = entry.confirmedDevice(item) ?: return
        stop()
        confirm(device, item.serialMatches)
    }
}

private const val ManualScanDurationMs = 20_000L
