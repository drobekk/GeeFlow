@file:Suppress("MaxLineLength", "MaximumLineLength", "LongMethod") // Literal BLE capture fixtures.

package app.geeflow.data.device.impl.controller

import app.geeflow.data.brew.model.BrewDataPoint
import app.geeflow.data.brew.model.BrewProfile
import app.geeflow.data.brew.model.BrewProgram
import app.geeflow.data.brew.model.Condition
import app.geeflow.data.brew.model.FreeHandControlMode
import app.geeflow.data.brew.model.FreeHandRecording
import app.geeflow.data.brew.model.FreeHandSample
import app.geeflow.data.device.ble.modbus.ModbusFrame
import app.geeflow.domain.exception.RecordingCapacityExceededException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RecordedProfileCaptureTest {
    @Test
    fun rejects83SecondRecordingBeforeProducingUploadCommands() {
        val recording = FreeHandRecording(
            FreeHandControlMode.Pressure,
            List(167) { FreeHandSample(it * 500L, BrewDataPoint(9f, 0f, it * 2.75f, 5.5f, 0f)) },
        )
        val profile = BrewProfile(
            userId = 1,
            name = "Long pressure recording",
            description = "",

            finishCondition = Condition.Volume(457f),
            program = BrewProgram.Recording(recording),
        )
        assertFailsWith<RecordingCapacityExceededException> { WendougeeProfileCompiler().buildProfileWrites(profile) }
    }

    @Test
    fun pressureCaptureMatchesEveryUploadedByte() {
        // Captured original-app telemetry and upload, 2026-09-11. No generated expected values.
        val recording = FreeHandRecording(
            FreeHandControlMode.Pressure,
            listOf(
                FreeHandSample(0L, BrewDataPoint(0.8f, 0.0f, 2.0f, 0.0f, 0.0f)),
                FreeHandSample(375L, BrewDataPoint(0.8f, 0.0f, 3.0f, 4.0f, 0.0f)),
                FreeHandSample(900L, BrewDataPoint(0.8f, 0.0f, 4.0f, 4.0f, 0.0f)),
                FreeHandSample(1387L, BrewDataPoint(0.8f, 0.0f, 6.0f, 3.0f, 0.0f)),
                FreeHandSample(1875L, BrewDataPoint(0.8f, 0.0f, 8.0f, 3.0f, 0.0f)),
                FreeHandSample(2401L, BrewDataPoint(0.8f, 0.0f, 9.0f, 3.0f, 0.0f)),
                FreeHandSample(2889L, BrewDataPoint(0.8f, 0.0f, 10.0f, 3.0f, 0.0f)),
                FreeHandSample(3486L, BrewDataPoint(0.7f, 0.0f, 11.0f, 2.0f, 0.0f)),
                FreeHandSample(3901L, BrewDataPoint(0.7f, 0.0f, 12.0f, 2.0f, 0.0f)),
                FreeHandSample(4387L, BrewDataPoint(0.6f, 0.0f, 13.0f, 1.0f, 0.0f)),
                FreeHandSample(4912L, BrewDataPoint(0.6f, 0.0f, 15.0f, 1.0f, 0.0f)),
                FreeHandSample(5400L, BrewDataPoint(2.9f, 0.0f, 19.0f, 4.0f, 0.0f)),
                FreeHandSample(5888L, BrewDataPoint(2.9f, 0.0f, 23.0f, 4.0f, 0.0f)),
                FreeHandSample(6412L, BrewDataPoint(8.0f, 0.0f, 26.0f, 7.0f, 0.0f)),
                FreeHandSample(6901L, BrewDataPoint(6.8f, 0.0f, 30.0f, 7.0f, 0.0f)),
                FreeHandSample(7388L, BrewDataPoint(6.8f, 0.0f, 33.0f, 6.0f, 0.0f)),
                FreeHandSample(7913L, BrewDataPoint(6.9f, 0.0f, 36.0f, 6.0f, 0.0f)),
                FreeHandSample(8400L, BrewDataPoint(6.9f, 0.0f, 40.0f, 6.0f, 0.0f)),
                FreeHandSample(8888L, BrewDataPoint(6.9f, 0.0f, 44.0f, 6.0f, 0.0f)),
                FreeHandSample(9412L, BrewDataPoint(6.9f, 0.0f, 47.0f, 7.0f, 0.0f)),
                FreeHandSample(9900L, BrewDataPoint(6.8f, 0.0f, 51.0f, 7.0f, 0.0f)),
                FreeHandSample(10387L, BrewDataPoint(6.8f, 0.0f, 54.0f, 6.0f, 0.0f)),
                FreeHandSample(10877L, BrewDataPoint(6.9f, 0.0f, 58.0f, 6.0f, 0.0f)),
                FreeHandSample(11401L, BrewDataPoint(6.9f, 0.0f, 61.0f, 6.0f, 0.0f)),
                FreeHandSample(11887L, BrewDataPoint(6.9f, 0.0f, 65.0f, 6.0f, 0.0f)),
                FreeHandSample(12487L, BrewDataPoint(6.9f, 0.0f, 69.0f, 6.0f, 0.0f)),
                FreeHandSample(12900L, BrewDataPoint(6.6f, 0.0f, 71.0f, 6.0f, 0.0f)),
                FreeHandSample(13387L, BrewDataPoint(6.6f, 0.0f, 74.0f, 6.0f, 0.0f)),
                FreeHandSample(13913L, BrewDataPoint(3.9f, 0.0f, 76.0f, 6.0f, 0.0f)),
                FreeHandSample(14400L, BrewDataPoint(3.9f, 0.0f, 79.0f, 5.0f, 0.0f)),
                FreeHandSample(14889L, BrewDataPoint(4.0f, 0.0f, 82.0f, 5.0f, 0.0f)),
                FreeHandSample(15375L, BrewDataPoint(4.0f, 0.0f, 84.0f, 5.0f, 0.0f)),
                FreeHandSample(16012L, BrewDataPoint(4.0f, 0.0f, 87.0f, 5.0f, 0.0f)),
                FreeHandSample(16387L, BrewDataPoint(3.3f, 0.0f, 89.0f, 5.0f, 0.0f)),
                FreeHandSample(16912L, BrewDataPoint(3.3f, 0.0f, 91.0f, 5.0f, 0.0f)),
                FreeHandSample(17513L, BrewDataPoint(1.8f, 0.0f, 93.0f, 3.0f, 0.0f)),
                FreeHandSample(17888L, BrewDataPoint(1.8f, 0.0f, 95.0f, 3.0f, 0.0f)),
                FreeHandSample(18412L, BrewDataPoint(5.7f, 0.0f, 99.0f, 5.0f, 0.0f)),
                FreeHandSample(18900L, BrewDataPoint(5.7f, 0.0f, 103.0f, 5.0f, 0.0f)),
                FreeHandSample(19387L, BrewDataPoint(6.9f, 0.0f, 106.0f, 6.0f, 0.0f)),
                FreeHandSample(19913L, BrewDataPoint(6.9f, 0.0f, 109.0f, 6.0f, 0.0f)),
                FreeHandSample(20400L, BrewDataPoint(6.9f, 0.0f, 113.0f, 7.0f, 0.0f)),
                FreeHandSample(20889L, BrewDataPoint(6.9f, 0.0f, 117.0f, 7.0f, 0.0f)),
                FreeHandSample(21412L, BrewDataPoint(6.8f, 0.0f, 120.0f, 6.0f, 0.0f)),
            )
        )
        val profile = BrewProfile(
            userId = 1,
            name = "Capture",
            description = "",

            finishCondition = Condition.Volume(120f),
            program = BrewProgram.Recording(recording)
        )
        val expected = listOf(
            "011002f800408000080008000800080008000800080007000700060006001d001d005000440044004500450045004500440044004500450045004500420042002700270028002800280021002100120012003900390045004500450045004400440044004400440044004400440044004400440044004400440044004400440044004400440044dea7",
            "0110033800408000440044004400440044004400440044004400440044004400440044004400440044004400440044004400440044004400440044004400440044004400440044004400440044004400440044004400440044004400440044004400440044004400440044004400440044004400440044004400440044004400440044004400440e7e",
            "01100378004080000200030004000600080009000a000b000c000d000f00130017001a001e002100240028002c002f00330036003a003d004100450047004a004c004f0052005400570059005b005d005f00630067006a006d00710075007800000000000000000000000000000000000000000000000000000000000000000000000000000000cb85",
            "011003b800408000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000783f7b",
            "011003f800408000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000003169",
            "0110043800408000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000005f09",
            "011005dc004080000000280028001e001e001e001e00140014000a000a0028002800460046003c003c003c003c00460046003c003c003c003c003c003c003c003c003200320032003200320032001e001e00320032003c003c00460046003c003c003c003c003c003c003c003c003c003c003c003c003c003c003c003c003c003c003c003c003ce5c9",
            "0110061c004080003c003c003c003c003c003c003c003c003c003c003c003c003c003c003c003c003c003c003c003c003c003c003c003c003c003c003c003c003c003c003c003c003c003c003c003c003c003c003c003c003c003c003c003c003c003c003c003c003c003c003c003c003c003c003c003c003c003c003c003c003c003c003c003c6607",
            "0110004f000102000169af",
            "011000570001020004aa74",
            "0106016a0000a82a",
            "010601660078680b",
            "0106016e0000e9eb",
            "011001640001027f7fdf64",
        )
        val actual = WendougeeProfileCompiler().buildProfileWrites(profile).map { write ->
            val frame = when (write) {
                is ProfileWrite.Single -> ModbusFrame.writeSingleRegister(1, write.register, write.value)
                is ProfileWrite.Multiple -> ModbusFrame.writeMultipleRegisters(1, write.register, write.values)
            }
            frame.joinToString("") { (it.toInt() and 255).toString(16).padStart(2, '0') }
        }
        assertEquals(expected, actual)
    }

    @Test
    fun flowCaptureMatchesEveryUploadedByte() {
        // Captured original-app telemetry and upload, 2026-09-11. No generated expected values.
        val recording = FreeHandRecording(
            FreeHandControlMode.Flow,
            listOf(
                FreeHandSample(0L, BrewDataPoint(0.9f, 0.0f, 2.0f, 2.0f, 0.0f)),
                FreeHandSample(414L, BrewDataPoint(0.9f, 0.0f, 3.0f, 2.0f, 0.0f)),
                FreeHandSample(901L, BrewDataPoint(0.8f, 0.0f, 5.0f, 3.0f, 0.0f)),
                FreeHandSample(1425L, BrewDataPoint(0.8f, 0.0f, 6.0f, 3.0f, 0.0f)),
                FreeHandSample(1914L, BrewDataPoint(0.8f, 0.0f, 8.0f, 3.0f, 0.0f)),
                FreeHandSample(2401L, BrewDataPoint(0.8f, 0.0f, 9.0f, 3.0f, 0.0f)),
                FreeHandSample(3038L, BrewDataPoint(0.8f, 0.0f, 11.0f, 2.0f, 0.0f)),
                FreeHandSample(3413L, BrewDataPoint(0.8f, 0.0f, 11.0f, 2.0f, 0.0f)),
                FreeHandSample(3900L, BrewDataPoint(0.8f, 0.0f, 13.0f, 2.0f, 0.0f)),
                FreeHandSample(4426L, BrewDataPoint(0.8f, 0.0f, 13.0f, 2.0f, 0.0f)),
                FreeHandSample(4913L, BrewDataPoint(0.8f, 0.0f, 16.0f, 1.0f, 0.0f)),
                FreeHandSample(5401L, BrewDataPoint(0.8f, 0.0f, 19.0f, 1.0f, 0.0f)),
                FreeHandSample(5926L, BrewDataPoint(8.2f, 0.0f, 24.0f, 7.0f, 0.0f)),
                FreeHandSample(6413L, BrewDataPoint(8.2f, 0.0f, 28.0f, 7.0f, 0.0f)),
                FreeHandSample(6901L, BrewDataPoint(10.7f, 0.0f, 33.0f, 8.0f, 0.0f)),
                FreeHandSample(7427L, BrewDataPoint(10.7f, 0.0f, 36.0f, 8.0f, 0.0f)),
                FreeHandSample(7914L, BrewDataPoint(8.7f, 0.0f, 40.0f, 7.0f, 0.0f)),
                FreeHandSample(8401L, BrewDataPoint(5.8f, 0.0f, 43.0f, 7.0f, 0.0f)),
                FreeHandSample(8927L, BrewDataPoint(5.8f, 0.0f, 46.0f, 6.0f, 0.0f)),
                FreeHandSample(9413L, BrewDataPoint(4.6f, 0.0f, 49.0f, 6.0f, 0.0f)),
                FreeHandSample(9901L, BrewDataPoint(4.6f, 0.0f, 51.0f, 5.0f, 0.0f)),
                FreeHandSample(10426L, BrewDataPoint(5.1f, 0.0f, 54.0f, 5.0f, 0.0f)),
                FreeHandSample(10914L, BrewDataPoint(5.1f, 0.0f, 58.0f, 5.0f, 0.0f)),
                FreeHandSample(11402L, BrewDataPoint(6.2f, 0.0f, 61.0f, 5.0f, 0.0f)),
                FreeHandSample(11927L, BrewDataPoint(6.2f, 0.0f, 65.0f, 6.0f, 0.0f)),
                FreeHandSample(12415L, BrewDataPoint(7.0f, 0.0f, 68.0f, 6.0f, 0.0f)),
                FreeHandSample(12901L, BrewDataPoint(7.0f, 0.0f, 72.0f, 6.0f, 0.0f)),
                FreeHandSample(13426L, BrewDataPoint(6.8f, 0.0f, 75.0f, 6.0f, 0.0f)),
                FreeHandSample(13914L, BrewDataPoint(6.8f, 0.0f, 78.0f, 6.0f, 0.0f)),
                FreeHandSample(14401L, BrewDataPoint(6.2f, 0.0f, 82.0f, 6.0f, 0.0f)),
                FreeHandSample(14926L, BrewDataPoint(6.2f, 0.0f, 85.0f, 6.0f, 0.0f)),
                FreeHandSample(15414L, BrewDataPoint(5.8f, 0.0f, 88.0f, 6.0f, 0.0f)),
                FreeHandSample(15902L, BrewDataPoint(5.8f, 0.0f, 91.0f, 6.0f, 0.0f)),
                FreeHandSample(16426L, BrewDataPoint(5.9f, 0.0f, 94.0f, 6.0f, 0.0f)),
                FreeHandSample(16916L, BrewDataPoint(5.9f, 0.0f, 98.0f, 6.0f, 0.0f)),
                FreeHandSample(17402L, BrewDataPoint(6.0f, 0.0f, 101.0f, 6.0f, 0.0f)),
                FreeHandSample(17926L, BrewDataPoint(6.2f, 0.0f, 104.0f, 6.0f, 0.0f)),
                FreeHandSample(18417L, BrewDataPoint(6.2f, 0.0f, 108.0f, 6.0f, 0.0f)),
                FreeHandSample(18902L, BrewDataPoint(6.3f, 0.0f, 111.0f, 6.0f, 0.0f)),
                FreeHandSample(19425L, BrewDataPoint(6.3f, 0.0f, 114.0f, 6.0f, 0.0f)),
                FreeHandSample(19914L, BrewDataPoint(6.3f, 0.0f, 118.0f, 6.0f, 0.0f)),
                FreeHandSample(20401L, BrewDataPoint(6.3f, 0.0f, 121.0f, 6.0f, 0.0f)),
                FreeHandSample(20926L, BrewDataPoint(6.1f, 0.0f, 124.0f, 6.0f, 0.0f)),
                FreeHandSample(21413L, BrewDataPoint(6.1f, 0.0f, 127.0f, 6.0f, 0.0f)),
                FreeHandSample(21903L, BrewDataPoint(6.0f, 0.0f, 131.0f, 6.0f, 0.0f)),
                FreeHandSample(22426L, BrewDataPoint(6.0f, 0.0f, 134.0f, 6.0f, 0.0f)),
                FreeHandSample(22914L, BrewDataPoint(6.0f, 0.0f, 137.0f, 6.0f, 0.0f)),
                FreeHandSample(23401L, BrewDataPoint(6.0f, 0.0f, 140.0f, 6.0f, 0.0f)),
                FreeHandSample(24003L, BrewDataPoint(6.1f, 0.0f, 144.0f, 6.0f, 0.0f)),
                FreeHandSample(24413L, BrewDataPoint(6.1f, 0.0f, 147.0f, 6.0f, 0.0f)),
                FreeHandSample(24901L, BrewDataPoint(5.4f, 0.0f, 149.0f, 6.0f, 0.0f)),
                FreeHandSample(25427L, BrewDataPoint(5.4f, 0.0f, 151.0f, 6.0f, 0.0f)),
                FreeHandSample(25913L, BrewDataPoint(2.5f, 0.0f, 152.0f, 4.0f, 0.0f)),
                FreeHandSample(26513L, BrewDataPoint(1.1f, 0.0f, 154.0f, 2.0f, 0.0f)),
                FreeHandSample(26926L, BrewDataPoint(1.1f, 0.0f, 156.0f, 2.0f, 0.0f)),
                FreeHandSample(27413L, BrewDataPoint(5.6f, 0.0f, 160.0f, 2.0f, 0.0f)),
                FreeHandSample(27901L, BrewDataPoint(5.6f, 0.0f, 164.0f, 6.0f, 0.0f)),
                FreeHandSample(28426L, BrewDataPoint(11.4f, 0.0f, 169.0f, 6.0f, 0.0f)),
                FreeHandSample(28916L, BrewDataPoint(11.4f, 0.0f, 174.0f, 9.0f, 0.0f)),
                FreeHandSample(29440L, BrewDataPoint(12.6f, 0.0f, 179.0f, 9.0f, 0.0f)),
                FreeHandSample(29926L, BrewDataPoint(12.6f, 0.0f, 184.0f, 9.0f, 0.0f)),
                FreeHandSample(30527L, BrewDataPoint(12.9f, 0.0f, 189.0f, 9.0f, 0.0f)),
                FreeHandSample(30901L, BrewDataPoint(12.9f, 0.0f, 193.0f, 9.0f, 0.0f)),
                FreeHandSample(31426L, BrewDataPoint(9.7f, 0.0f, 195.0f, 9.0f, 0.0f)),
                FreeHandSample(31914L, BrewDataPoint(9.7f, 0.0f, 197.0f, 7.0f, 0.0f)),
                FreeHandSample(32401L, BrewDataPoint(1.7f, 0.0f, 198.0f, 1.0f, 0.0f)),
                FreeHandSample(32925L, BrewDataPoint(1.7f, 0.0f, 198.0f, 1.0f, 0.0f)),
                FreeHandSample(33413L, BrewDataPoint(0.2f, 0.0f, 199.0f, 1.0f, 0.0f)),
                FreeHandSample(34014L, BrewDataPoint(1.0f, 0.0f, 201.0f, 1.0f, 0.0f)),
                FreeHandSample(34427L, BrewDataPoint(1.0f, 0.0f, 203.0f, 3.0f, 0.0f)),
                FreeHandSample(34914L, BrewDataPoint(6.1f, 0.0f, 207.0f, 3.0f, 0.0f)),
                FreeHandSample(35402L, BrewDataPoint(6.1f, 0.0f, 211.0f, 8.0f, 0.0f)),
                FreeHandSample(35927L, BrewDataPoint(11.3f, 0.0f, 216.0f, 8.0f, 0.0f)),
                FreeHandSample(36417L, BrewDataPoint(11.3f, 0.0f, 220.0f, 9.0f, 0.0f)),
                FreeHandSample(36901L, BrewDataPoint(11.9f, 0.0f, 225.0f, 9.0f, 0.0f)),
                FreeHandSample(37428L, BrewDataPoint(11.9f, 0.0f, 229.0f, 9.0f, 0.0f)),
                FreeHandSample(37914L, BrewDataPoint(11.1f, 0.0f, 234.0f, 9.0f, 0.0f)),
            )
        )
        val profile = BrewProfile(
            userId = 1,
            name = "Capture",
            description = "",

            finishCondition = Condition.Volume(234f),
            program = BrewProgram.Recording(recording)
        )
        val expected = listOf(
            "011002f800408000090009000800080008000800080008000800080008000800520052006b006b0057003a003a002e002e00330033003e003e0046004600440044003e003e003a003a003b003b003c003e003e003f003f003f003f003d003d003c003c003c003c003d003d003600360019000b000b003800380072007200780078007800780061a557",
            "011003380040800061001100110002000a000a003d003d0071007100770077006f006f006f006f006f006f006f006f006f006f006f006f006f006f006f006f006f006f006f006f006f006f006f006f006f006f006f006f006f006f006f006f006f006f006f006f006f006f006f006f006f006f006f006f006f006f006f006f006f006f006f006f03f4",
            "01100378004080000200030005000600080009000b000b000d000d001000130018001c002100240028002b002e003100330036003a003d004100440048004b004e005200550058005b005e006200650068006c006f007200760079007c007f008300860089008c00900093009500970098009a009c00a000a400a900ae00b300b800bd00c100c35df3",
            "011003b800408000c500c600c600c700c900cb00cf00d300d800dc00e100e500ea0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000ea2633",
            "011003f800408000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000003169",
            "0110043800408000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000005f09",
            "011005dc00408000140014001e001e001e001e0014001400140014000a000a004600460050005000460046003c003c0032003200320032003c003c003c003c003c003c003c003c003c003c003c003c003c003c003c003c003c003c003c003c003c003c003c003c003c003c003c003c0028001400140014003c003c005a005a005a005a005a005aaf8a",
            "0110061c0040800046000a000a000a000a001e001e00500050005a005a005a005a005a005a005a005a005a005a005a005a005a005a005a005a005a005a005a005a005a005a005a005a005a005a005a005a005a005a005a005a005a005a005a005a005a005a005a005a005a005a005a005a005a005a005a005a005a005a005a005a005a005a005a28a0",
            "0110004f000102000169af",
            "011000570001020004aa74",
            "0106016a000169ea",
            "0106016600eae9a6",
            "0106016e0000e9eb",
            "011001640001027f7fdf64",
        )
        val actual = WendougeeProfileCompiler().buildProfileWrites(profile).map { write ->
            val frame = when (write) {
                is ProfileWrite.Single -> ModbusFrame.writeSingleRegister(1, write.register, write.value)
                is ProfileWrite.Multiple -> ModbusFrame.writeMultipleRegisters(1, write.register, write.values)
            }
            frame.joinToString("") { (it.toInt() and 255).toString(16).padStart(2, '0') }
        }
        assertEquals(expected, actual)
    }

    @Test
    fun weightTargetMatchesEveryUploadedByte() {
        // Captured original-app telemetry and upload, 2026-09-11. No generated expected values.
        val recording = FreeHandRecording(
            FreeHandControlMode.Flow,
            listOf(
                FreeHandSample(0L, BrewDataPoint(0.9f, 0.0f, 2.0f, 2.0f, 0.0f)),
                FreeHandSample(414L, BrewDataPoint(0.9f, 0.0f, 3.0f, 2.0f, 0.0f)),
                FreeHandSample(901L, BrewDataPoint(0.8f, 0.0f, 5.0f, 3.0f, 0.0f)),
                FreeHandSample(1425L, BrewDataPoint(0.8f, 0.0f, 6.0f, 3.0f, 0.0f)),
                FreeHandSample(1914L, BrewDataPoint(0.8f, 0.0f, 8.0f, 3.0f, 0.0f)),
                FreeHandSample(2401L, BrewDataPoint(0.8f, 0.0f, 9.0f, 3.0f, 0.0f)),
                FreeHandSample(3038L, BrewDataPoint(0.8f, 0.0f, 11.0f, 2.0f, 0.0f)),
                FreeHandSample(3413L, BrewDataPoint(0.8f, 0.0f, 11.0f, 2.0f, 0.0f)),
                FreeHandSample(3900L, BrewDataPoint(0.8f, 0.0f, 13.0f, 2.0f, 0.0f)),
                FreeHandSample(4426L, BrewDataPoint(0.8f, 0.0f, 13.0f, 2.0f, 0.0f)),
                FreeHandSample(4913L, BrewDataPoint(0.8f, 0.0f, 16.0f, 1.0f, 0.0f)),
                FreeHandSample(5401L, BrewDataPoint(0.8f, 0.0f, 19.0f, 1.0f, 0.0f)),
                FreeHandSample(5926L, BrewDataPoint(8.2f, 0.0f, 24.0f, 7.0f, 0.0f)),
                FreeHandSample(6413L, BrewDataPoint(8.2f, 0.0f, 28.0f, 7.0f, 0.0f)),
                FreeHandSample(6901L, BrewDataPoint(10.7f, 0.0f, 33.0f, 8.0f, 0.0f)),
                FreeHandSample(7427L, BrewDataPoint(10.7f, 0.0f, 36.0f, 8.0f, 0.0f)),
                FreeHandSample(7914L, BrewDataPoint(8.7f, 0.0f, 40.0f, 7.0f, 0.0f)),
                FreeHandSample(8401L, BrewDataPoint(5.8f, 0.0f, 43.0f, 7.0f, 0.0f)),
                FreeHandSample(8927L, BrewDataPoint(5.8f, 0.0f, 46.0f, 6.0f, 0.0f)),
                FreeHandSample(9413L, BrewDataPoint(4.6f, 0.0f, 49.0f, 6.0f, 0.0f)),
                FreeHandSample(9901L, BrewDataPoint(4.6f, 0.0f, 51.0f, 5.0f, 0.0f)),
                FreeHandSample(10426L, BrewDataPoint(5.1f, 0.0f, 54.0f, 5.0f, 0.0f)),
                FreeHandSample(10914L, BrewDataPoint(5.1f, 0.0f, 58.0f, 5.0f, 0.0f)),
                FreeHandSample(11402L, BrewDataPoint(6.2f, 0.0f, 61.0f, 5.0f, 0.0f)),
                FreeHandSample(11927L, BrewDataPoint(6.2f, 0.0f, 65.0f, 6.0f, 0.0f)),
                FreeHandSample(12415L, BrewDataPoint(7.0f, 0.0f, 68.0f, 6.0f, 0.0f)),
                FreeHandSample(12901L, BrewDataPoint(7.0f, 0.0f, 72.0f, 6.0f, 0.0f)),
                FreeHandSample(13426L, BrewDataPoint(6.8f, 0.0f, 75.0f, 6.0f, 0.0f)),
                FreeHandSample(13914L, BrewDataPoint(6.8f, 0.0f, 78.0f, 6.0f, 0.0f)),
                FreeHandSample(14401L, BrewDataPoint(6.2f, 0.0f, 82.0f, 6.0f, 0.0f)),
                FreeHandSample(14926L, BrewDataPoint(6.2f, 0.0f, 85.0f, 6.0f, 0.0f)),
                FreeHandSample(15414L, BrewDataPoint(5.8f, 0.0f, 88.0f, 6.0f, 0.0f)),
                FreeHandSample(15902L, BrewDataPoint(5.8f, 0.0f, 91.0f, 6.0f, 0.0f)),
                FreeHandSample(16426L, BrewDataPoint(5.9f, 0.0f, 94.0f, 6.0f, 0.0f)),
                FreeHandSample(16916L, BrewDataPoint(5.9f, 0.0f, 98.0f, 6.0f, 0.0f)),
                FreeHandSample(17402L, BrewDataPoint(6.0f, 0.0f, 101.0f, 6.0f, 0.0f)),
                FreeHandSample(17926L, BrewDataPoint(6.2f, 0.0f, 104.0f, 6.0f, 0.0f)),
                FreeHandSample(18417L, BrewDataPoint(6.2f, 0.0f, 108.0f, 6.0f, 0.0f)),
                FreeHandSample(18902L, BrewDataPoint(6.3f, 0.0f, 111.0f, 6.0f, 0.0f)),
                FreeHandSample(19425L, BrewDataPoint(6.3f, 0.0f, 114.0f, 6.0f, 0.0f)),
                FreeHandSample(19914L, BrewDataPoint(6.3f, 0.0f, 118.0f, 6.0f, 0.0f)),
                FreeHandSample(20401L, BrewDataPoint(6.3f, 0.0f, 121.0f, 6.0f, 0.0f)),
                FreeHandSample(20926L, BrewDataPoint(6.1f, 0.0f, 124.0f, 6.0f, 0.0f)),
                FreeHandSample(21413L, BrewDataPoint(6.1f, 0.0f, 127.0f, 6.0f, 0.0f)),
                FreeHandSample(21903L, BrewDataPoint(6.0f, 0.0f, 131.0f, 6.0f, 0.0f)),
                FreeHandSample(22426L, BrewDataPoint(6.0f, 0.0f, 134.0f, 6.0f, 0.0f)),
                FreeHandSample(22914L, BrewDataPoint(6.0f, 0.0f, 137.0f, 6.0f, 0.0f)),
                FreeHandSample(23401L, BrewDataPoint(6.0f, 0.0f, 140.0f, 6.0f, 0.0f)),
                FreeHandSample(24003L, BrewDataPoint(6.1f, 0.0f, 144.0f, 6.0f, 0.0f)),
                FreeHandSample(24413L, BrewDataPoint(6.1f, 0.0f, 147.0f, 6.0f, 0.0f)),
                FreeHandSample(24901L, BrewDataPoint(5.4f, 0.0f, 149.0f, 6.0f, 0.0f)),
                FreeHandSample(25427L, BrewDataPoint(5.4f, 0.0f, 151.0f, 6.0f, 0.0f)),
                FreeHandSample(25913L, BrewDataPoint(2.5f, 0.0f, 152.0f, 4.0f, 0.0f)),
                FreeHandSample(26513L, BrewDataPoint(1.1f, 0.0f, 154.0f, 2.0f, 0.0f)),
                FreeHandSample(26926L, BrewDataPoint(1.1f, 0.0f, 156.0f, 2.0f, 0.0f)),
                FreeHandSample(27413L, BrewDataPoint(5.6f, 0.0f, 160.0f, 2.0f, 0.0f)),
                FreeHandSample(27901L, BrewDataPoint(5.6f, 0.0f, 164.0f, 6.0f, 0.0f)),
                FreeHandSample(28426L, BrewDataPoint(11.4f, 0.0f, 169.0f, 6.0f, 0.0f)),
                FreeHandSample(28916L, BrewDataPoint(11.4f, 0.0f, 174.0f, 9.0f, 0.0f)),
                FreeHandSample(29440L, BrewDataPoint(12.6f, 0.0f, 179.0f, 9.0f, 0.0f)),
                FreeHandSample(29926L, BrewDataPoint(12.6f, 0.0f, 184.0f, 9.0f, 0.0f)),
                FreeHandSample(30527L, BrewDataPoint(12.9f, 0.0f, 189.0f, 9.0f, 0.0f)),
                FreeHandSample(30901L, BrewDataPoint(12.9f, 0.0f, 193.0f, 9.0f, 0.0f)),
                FreeHandSample(31426L, BrewDataPoint(9.7f, 0.0f, 195.0f, 9.0f, 0.0f)),
                FreeHandSample(31914L, BrewDataPoint(9.7f, 0.0f, 197.0f, 7.0f, 0.0f)),
                FreeHandSample(32401L, BrewDataPoint(1.7f, 0.0f, 198.0f, 1.0f, 0.0f)),
                FreeHandSample(32925L, BrewDataPoint(1.7f, 0.0f, 198.0f, 1.0f, 0.0f)),
                FreeHandSample(33413L, BrewDataPoint(0.2f, 0.0f, 199.0f, 1.0f, 0.0f)),
                FreeHandSample(34014L, BrewDataPoint(1.0f, 0.0f, 201.0f, 1.0f, 0.0f)),
                FreeHandSample(34427L, BrewDataPoint(1.0f, 0.0f, 203.0f, 3.0f, 0.0f)),
                FreeHandSample(34914L, BrewDataPoint(6.1f, 0.0f, 207.0f, 3.0f, 0.0f)),
                FreeHandSample(35402L, BrewDataPoint(6.1f, 0.0f, 211.0f, 8.0f, 0.0f)),
                FreeHandSample(35927L, BrewDataPoint(11.3f, 0.0f, 216.0f, 8.0f, 0.0f)),
                FreeHandSample(36417L, BrewDataPoint(11.3f, 0.0f, 220.0f, 9.0f, 0.0f)),
                FreeHandSample(36901L, BrewDataPoint(11.9f, 0.0f, 225.0f, 9.0f, 0.0f)),
                FreeHandSample(37428L, BrewDataPoint(11.9f, 0.0f, 229.0f, 9.0f, 0.0f)),
                FreeHandSample(37914L, BrewDataPoint(11.1f, 0.0f, 234.0f, 9.0f, 0.0f)),
            )
        )
        val profile = BrewProfile(
            userId = 1,
            name = "Capture",
            description = "",

            finishCondition = Condition.Weight(235f),
            program = BrewProgram.Recording(recording)
        )
        val expected = listOf(
            "011002f800408000090009000800080008000800080008000800080008000800520052006b006b0057003a003a002e002e00330033003e003e0046004600440044003e003e003a003a003b003b003c003e003e003f003f003f003f003d003d003c003c003c003c003d003d003600360019000b000b003800380072007200780078007800780061a557",
            "011003380040800061001100110002000a000a003d003d0071007100770077006f006f006f006f006f006f006f006f006f006f006f006f006f006f006f006f006f006f006f006f006f006f006f006f006f006f006f006f006f006f006f006f006f006f006f006f006f006f006f006f006f006f006f006f006f006f006f006f006f006f006f006f03f4",
            "01100378004080000200030005000600080009000b000b000d000d001000130018001c002100240028002b002e003100330036003a003d004100440048004b004e005200550058005b005e006200650068006c006f007200760079007c007f008300860089008c00900093009500970098009a009c00a000a400a900ae00b300b800bd00c100c35df3",
            "011003b800408000c500c600c600c700c900cb00cf00d300d800dc00e100e500ea0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000ea2633",
            "011003f800408000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000003169",
            "0110043800408000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000eb1f46",
            "011005dc00408000140014001e001e001e001e0014001400140014000a000a004600460050005000460046003c003c0032003200320032003c003c003c003c003c003c003c003c003c003c003c003c003c003c003c003c003c003c003c003c003c003c003c003c003c003c003c003c0028001400140014003c003c005a005a005a005a005a005aaf8a",
            "0110061c0040800046000a000a000a000a001e001e00500050005a005a005a005a005a005a005a005a005a005a005a005a005a005a005a005a005a005a005a005a005a005a005a005a005a005a005a005a005a005a005a005a005a005a005a005a005a005a005a005a005a005a005a005a005a005a005a005a005a005a005a005a005a005a005a28a0",
            "0110004f0001020000a86f",
            "011000570001020004aa74",
            "0106016a000169ea",
            "0106016600eb2866",
            "0106016e0000e9eb",
            "011001640001027f7fdf64",
        )
        val actual = WendougeeProfileCompiler().buildProfileWrites(profile).map { write ->
            val frame = when (write) {
                is ProfileWrite.Single -> ModbusFrame.writeSingleRegister(1, write.register, write.value)
                is ProfileWrite.Multiple -> ModbusFrame.writeMultipleRegisters(1, write.register, write.values)
            }
            frame.joinToString("") { (it.toInt() and 255).toString(16).padStart(2, '0') }
        }
        assertEquals(expected, actual)
    }
}
