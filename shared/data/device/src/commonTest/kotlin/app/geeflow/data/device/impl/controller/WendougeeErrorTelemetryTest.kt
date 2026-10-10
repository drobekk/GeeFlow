package app.geeflow.data.device.impl.controller

import app.geeflow.data.device.ble.modbus.ModbusCrc
import app.geeflow.data.device.ble.modbus.ModbusFrame
import app.geeflow.data.device.model.DeviceError
import app.geeflow.data.device.model.DeviceState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WendougeeErrorTelemetryTest {
    @Test
    fun `when telemetry reports an alarm code then decodes its type and clears it on zero`() {
        var state = DeviceState()
        val parser = WendougeeFrameParser(onStateUpdate = { state = state.it() })
        val types = listOf(
            DeviceError.Type.WaterShortage,
            DeviceError.Type.HeatingTimeout,
            DeviceError.Type.WaterReplenishmentTimeout,
            DeviceError.Type.ExtractionTimeout,
            DeviceError.Type.PressureSensorMissing,
            DeviceError.Type.SteamBoilerSensorFailure,
            DeviceError.Type.BrewBoilerSensorFailure,
            DeviceError.Type.WaterLevelAbnormal,
        )

        types.forEachIndexed { index, type ->
            val code = index + 1

            parser.handleIncomingFrame(telemetry(code), "data")

            assertEquals(code, state.error?.code)
            assertEquals(type, state.error?.type)
            assertEquals(code == 1 || code == 8, state.waterLevelAlarm)
            assertTrue(state.hasError)
        }

        parser.handleIncomingFrame(telemetry(0), "data")

        assertNull(state.error)
        assertFalse(state.waterLevelAlarm)
        assertFalse(state.hasError)
    }

    @Test
    fun `when telemetry reports an unknown uint16 error then keeps it visible without a water alarm`() {
        var state = DeviceState()
        val parser = WendougeeFrameParser(onStateUpdate = { state = state.it() })

        parser.handleIncomingFrame(telemetry(0x0102), "data")

        assertEquals(258, state.error?.code)
        assertNull(state.error?.type)
        assertTrue(state.hasError)
        assertFalse(state.waterLevelAlarm)

        parser.handleIncomingFrame(telemetry(0).copyOf(10), "data")

        assertEquals(258, state.error?.code)
    }

    @Test
    fun `when base configuration is refreshed then preserves separately read water alarm setting`() {
        var state = DeviceState()
        val parser = WendougeeFrameParser(onStateUpdate = { state = state.it() })
        val configFrame = ModbusCrc.append(byteArrayOf(1, 3, 74) + ByteArray(74))
        parser.handleIncomingFrame(configFrame, "data")
        state = state.copy(config = requireNotNull(state.config).copy(waterAlarmEnabled = true))

        parser.handleIncomingFrame(configFrame, "data")

        assertTrue(requireNotNull(state.config).waterAlarmEnabled)
    }

    @Test
    fun `when water alarm commands are built then use register 396 with FC10 values one and zero and FC03 readback`() {
        val register = WendougeeRegisters.WATER_ALARM

        val enableFrame = ModbusFrame.writeMultipleRegisters(1, register, listOf(1))
        val disableFrame = ModbusFrame.writeMultipleRegisters(1, register, listOf(0))
        val readFrame = ModbusFrame.readHoldingRegisters(1, register, 1)

        assertEquals(396, WendougeeRegisters.WATER_ALARM)
        assertEquals(
            "0110018c0001020001685c",
            WendougeeFrameParser.toHexString(enableFrame),
        )
        assertEquals(
            "0110018c0001020000a99c",
            WendougeeFrameParser.toHexString(disableFrame),
        )
        assertEquals(
            "0103018c0001441d",
            WendougeeFrameParser.toHexString(readFrame),
        )
    }

    private fun telemetry(code: Int): ByteArray {
        val registers = ByteArray(40)
        registers[3] = 99 // Adjacent timestamp must not be mistaken for an error.
        registers[4] = (code shr 8).toByte()
        registers[5] = code.toByte()
        return ModbusCrc.append(byteArrayOf(1, 3, 40) + registers)
    }
}
