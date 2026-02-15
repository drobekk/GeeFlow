package dev.drobek.geeflow.domain.device.usecase

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ParseDeviceQrCodeUseCaseTest {
    private val useCase = ParseDeviceQrCodeUseCase()

    @Test
    fun `parsing valid QR code URL returns device`() {
        val url = "https://services.wendougee.com/services?data=" +
                "%7B%22deviceid%22%3A%22B021372115%22%2C%22blemac%22%3A%2231CDB2DD364E%22%2C%22name%22%3A%22DATA%22%2C%22type%22%3A%22coffee%22%2C%22color%22%3A%22white%22%7D"

        val device = useCase(url)

        assertNotNull(device)
        assertEquals("B021372115", device.serialNumber)
        assertEquals("31CDB2DD364E", device.macAddress)
        assertEquals("DATA", device.name)
    }

    @Test
    fun `parsing valid QR code URL with spaces returns device`() {
        val url = "https://services.wendougee.com/services?data=" +
                "{\"deviceid\":\"B021372115\",\"blemac\":\"31CDB2DD364E\",\"name\":\"DATA\",\"type\":\"coffee\",\"color\":\"white\"}"

        val device = useCase(url)

        assertNotNull(device)
        assertEquals("B021372115", device.serialNumber)
        assertEquals("31CDB2DD364E", device.macAddress)
        assertEquals("DATA", device.name)
    }

    @Test
    fun `parsing invalid QR code URL returns null`() {
        val url = "https://example.com"
        val device = useCase(url)
        assertNull(device)
    }
}
