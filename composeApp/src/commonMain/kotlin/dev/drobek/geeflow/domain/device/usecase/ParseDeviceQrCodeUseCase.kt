package dev.drobek.geeflow.domain.device.usecase

import dev.drobek.geeflow.domain.device.model.Device
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Factory

@Serializable
private data class QrDeviceData(
    @SerialName("deviceid")
    val deviceId: String,
    @SerialName("blemac")
    val macAddress: String,
    val name: String,
    val type: String? = null,
    val color: String? = null
)

@Factory
class ParseDeviceQrCodeUseCase {
    private val json = Json { ignoreUnknownKeys = true }

    operator fun invoke(url: String): Device? {
        return try {
            val dataJson = url.substringAfter("data=", "")
            if (dataJson.isEmpty()) return null

            val decodedJson = decodeUrl(dataJson)
            val qrData = json.decodeFromString<QrDeviceData>(decodedJson)

            Device(
                serialNumber = qrData.deviceId,
                name = qrData.name,
                macAddress = qrData.macAddress
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun decodeUrl(url: String): String {
        return url
            .replace("%7B", "{")
            .replace("%7D", "}")
            .replace("%22", "\"")
            .replace("%3A", ":")
            .replace("%2C", ",")
            .replace("%20", " ")
            .replace("%5B", "[")
            .replace("%5D", "]")
    }
}
