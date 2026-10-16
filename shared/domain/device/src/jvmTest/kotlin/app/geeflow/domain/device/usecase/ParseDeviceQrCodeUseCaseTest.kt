package app.geeflow.domain.device.usecase

import app.geeflow.data.device.model.DeviceConnection
import app.geeflow.data.device.model.SupportedDevice
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ParseDeviceQrCodeUseCaseTest {
    @Test
    fun `when QR contains device data then normalizes MAC address`() = runTest {
        val useCase = ParseDeviceQrCodeUseCase()
        val url = "https://machine/?data=%7B%22blemac%22%3A%22aabbccddeeff%22%2C%22name%22%3A%22Machine%22%7D"

        val result = useCase(url)

        assertEquals("Machine", result?.name)
        assertEquals(DeviceConnection.Ble("AA:BB:CC:DD:EE:FF", "AA:BB:CC:DD:EE:FF"), result?.connection)
    }

    @Test
    fun `when QR has no data then returns null`() = runTest {
        val useCase = ParseDeviceQrCodeUseCase()
        val url = "https://machine/"

        val result = useCase(url)

        assertNull(result)
    }

    @Test
    fun `when QR has invalid JSON then returns null`() = runTest {
        val useCase = ParseDeviceQrCodeUseCase()
        val url = "https://machine/?data=invalid"

        val result = useCase(url)

        assertNull(result)
    }

    @Test
    fun `when QR type is not a model category then leaves model for manual selection`() {
        val useCase = ParseDeviceQrCodeUseCase()
        val url = "https://machine/?data={\"blemac\":\"aabbccddeeff\",\"name\":\"Wendougee\",\"type\":\"10011\"}"

        val device = useCase(url)

        assertNull(device?.modelId)
        assertEquals("", device?.model)
    }

    @Test
    fun `when category is unknown but name identifies LITA then records the name as source`() {
        val useCase = ParseDeviceQrCodeUseCase()
        val url = "https://machine/?data={\"blemac\":\"aabbccddeeff\",\"name\":\"LITA BA\",\"category_code\":\"99999\"}"

        val device = useCase(url)

        assertEquals(SupportedDevice.LitaBA.id, device?.modelId)
        assertEquals("qr-name", device?.modelSource)
    }

    @Test
    fun `when QR identifies only the LITA family then requires variant selection`() {
        val useCase = ParseDeviceQrCodeUseCase()
        val url = "https://machine/?data={\"blemac\":\"aabbccddeeff\",\"name\":\"Machine\",\"category_code\":\"10024\"}"

        val device = useCase(url)

        assertNull(device?.modelId)
        assertNull(device?.modelSource)
    }
}
