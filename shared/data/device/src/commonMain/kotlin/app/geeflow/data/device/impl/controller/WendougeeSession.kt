package app.geeflow.data.device.impl.controller

import app.geeflow.data.device.ble.modbus.ModbusNotifications
import app.geeflow.data.device.ble.modbus.ModbusSession
import kotlinx.coroutines.flow.MutableSharedFlow

internal class WendougeeSession(
    val link: WendougeeBleConnection,
    val modbus: ModbusSession,
    val responses: MutableSharedFlow<ByteArray>,
    val controlResponses: MutableSharedFlow<ByteArray> = MutableSharedFlow(),
    val frames: ModbusNotifications = ModbusNotifications(),
    val controlFrames: WendougeeControlFrames = WendougeeControlFrames(),
)
