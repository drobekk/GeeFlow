@file:Suppress("TooGenericExceptionCaught")

package app.geeflow.data.device.impl

import app.geeflow.data.device.DeviceController
import app.geeflow.data.device.DeviceControllerFactory
import app.geeflow.data.device.DeviceControllerProvider
import app.geeflow.data.device.DeviceRepository
import app.geeflow.data.device.model.supportedDevice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Single

@Single(binds = [DeviceControllerProvider::class])
class DefaultDeviceControllerProvider(
    private val deviceRepository: DeviceRepository,
    private val factory: DeviceControllerFactory,
    private val scope: CoroutineScope,
) : DeviceControllerProvider {
    private val mutableCurrentDeviceId = MutableStateFlow<Long?>(null)
    override val currentDeviceId = mutableCurrentDeviceId.asStateFlow()
    private val controllers = mutableMapOf<Long, DeviceController>()
    private val connectionMutex = Mutex()
    private var connecting: Deferred<Unit>? = null

    override fun getController(deviceId: Long): DeviceController = controllers.getOrPut(deviceId) {
        val device = requireNotNull(deviceRepository.getDeviceById(deviceId)) { "Device does not exist" }
        factory.create(requireNotNull(device.supportedDevice) { "Device model must be selected before connecting" })
    }

    override fun observeController(deviceId: Long): Flow<DeviceController> = deviceRepository.devices
        .map { devices -> devices.firstOrNull { it.id == deviceId }?.supportedDevice?.id }
        .distinctUntilChanged()
        .filterNotNull()
        .map { getController(deviceId) }

    override suspend fun connect(deviceId: Long) {
        val operation = connectionMutex.withLock {
            connecting?.cancelAndJoin()
            val device = requireNotNull(deviceRepository.getDeviceById(deviceId)) { "Device does not exist" }
            val controller = getController(deviceId)
            currentDeviceId.value?.let { controllers[it]?.connection?.disconnect() }
            mutableCurrentDeviceId.value = deviceId
            scope.async {
                try {
                    val resolved = controller.connection.connect(device)
                    if (resolved != device.connection) deviceRepository.updateConnection(deviceId, resolved)
                } catch (failure: Throwable) {
                    mutableCurrentDeviceId.value = null
                    throw failure
                }
            }.also { connecting = it }
        }
        try {
            operation.await()
        } catch (failure: Throwable) {
            withContext(NonCancellable) { operation.cancelAndJoin() }
            throw failure
        }
    }

    override suspend fun forgetController(deviceId: Long) = connectionMutex.withLock {
        if (currentDeviceId.value == deviceId) connecting?.cancelAndJoin()
        controllers.remove(deviceId)?.connection?.disconnect()
        if (currentDeviceId.value == deviceId) mutableCurrentDeviceId.value = null
    }

    override suspend fun disconnectCurrent() = connectionMutex.withLock {
        connecting?.cancelAndJoin()
        connecting = null
        currentDeviceId.value?.let { controllers[it]?.connection?.disconnect() }
        mutableCurrentDeviceId.value = null
    }
}
