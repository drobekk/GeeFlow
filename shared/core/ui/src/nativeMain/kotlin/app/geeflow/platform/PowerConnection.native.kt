package app.geeflow.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.UIKit.UIDevice
import platform.UIKit.UIDeviceBatteryState
import platform.UIKit.UIDeviceBatteryStateDidChangeNotification

@Composable
actual fun rememberIsConnectedToPower(): Boolean {
    val device = UIDevice.currentDevice
    var connected by remember { mutableStateOf(false) }

    DisposableEffect(device) {
        val previous = device.batteryMonitoringEnabled
        device.batteryMonitoringEnabled = true
        fun update() {
            connected = device.batteryState == UIDeviceBatteryState.UIDeviceBatteryStateCharging ||
                device.batteryState == UIDeviceBatteryState.UIDeviceBatteryStateFull
        }
        val center = NSNotificationCenter.defaultCenter
        val observer = center.addObserverForName(
            name = UIDeviceBatteryStateDidChangeNotification,
            `object` = device,
            queue = NSOperationQueue.mainQueue,
        ) { update() }
        update()
        onDispose {
            center.removeObserver(observer)
            device.batteryMonitoringEnabled = previous
        }
    }
    return connected
}
