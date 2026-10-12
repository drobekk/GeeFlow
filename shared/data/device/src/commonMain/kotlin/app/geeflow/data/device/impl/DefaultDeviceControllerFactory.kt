package app.geeflow.data.device.impl

import app.geeflow.data.device.DeviceController
import app.geeflow.data.device.DeviceControllerFactory
import app.geeflow.data.device.impl.controller.DemoDeviceController
import app.geeflow.data.device.impl.controller.WendougeeBleTransport
import app.geeflow.data.device.impl.controller.WendougeeController
import app.geeflow.data.device.model.SupportedDevice
import kotlinx.coroutines.CoroutineScope
import org.koin.core.annotation.Single

@Single(binds = [DeviceControllerFactory::class])
internal class DefaultDeviceControllerFactory(
    private val scope: CoroutineScope,
    private val transport: WendougeeBleTransport,
) : DeviceControllerFactory {
    override fun create(model: SupportedDevice): DeviceController = when (model) {
        SupportedDevice.GeeFlowDemo -> DemoDeviceController(scope)
        else -> WendougeeController(model = model, scope = scope, transport = transport)
    }
}
