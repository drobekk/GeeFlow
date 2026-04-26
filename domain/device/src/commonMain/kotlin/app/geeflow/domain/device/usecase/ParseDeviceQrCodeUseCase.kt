package app.geeflow.domain.device.usecase

import app.geeflow.data.device.model.Device
import app.geeflow.data.device.model.DeviceConnection
import co.touchlab.kermit.Logger
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Factory

@Serializable
private data class QrDeviceData(
    @SerialName("deviceid")
    val deviceId: String? = null,
    @SerialName("blemac")
    val macAddress: String,
    val name: String,
    val type: String? = null,
    val color: String? = null,
)

@Factory
class ParseDeviceQrCodeUseCase {
    private val json = Json { ignoreUnknownKeys = true }

    @Suppress("TooGenericExceptionCaught")
    operator fun invoke(url: String): Device? {
        return try {
            val dataJson = url.substringAfter("data=", "")
            if (dataJson.isEmpty()) return null

            val decodedJson = decodeUrl(dataJson)
            val qrData = json.decodeFromString<QrDeviceData>(decodedJson)

            val formattedMac = formatMac(qrData.macAddress)
            Device(
                id = 0L,
                name = qrData.name,
                connection = DeviceConnection.Ble(
                    peripheralId = formattedMac,
                    macAddress = formattedMac,
                ),
            )
        } catch (e: Exception) {
            Logger.e(throwable = e) { "Failed to parse device QR code from URL: $url" }
            null
        }
    }

    /** Normalises a BLE MAC to "AA:BB:CC:DD:EE:FF" regardless of input format. */
    private fun formatMac(raw: String): String =
        raw.uppercase().replace(":", "").chunked(2).joinToString(":")

    private fun decodeUrl(url: String): String = url
        .replace("%7B", "{")
        .replace("%7D", "}")
        .replace("%22", "\"")
        .replace("%3A", ":")
        .replace("%2C", ",")
        .replace("%20", " ")
        .replace("%5B", "[")
        .replace("%5D", "]")
}
