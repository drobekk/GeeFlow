package app.geeflow.domain.device.usecase

import app.geeflow.data.device.model.Device
import app.geeflow.data.device.model.DeviceConnection
import app.geeflow.data.device.model.DeviceManufacturer
import app.geeflow.data.device.model.SupportedDevice
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
    @SerialName("category_code") val categoryCode: String? = null,
)

@Factory
class ParseDeviceQrCodeUseCase {
    private val json = Json { ignoreUnknownKeys = true }

    @Suppress("TooGenericExceptionCaught")
    operator fun invoke(url: String): Device? {
        return try {
            val dataJson = url.substringAfter(
                "?",
                ""
            ).split("&").firstOrNull { it.startsWith("data=") }?.substringAfter("=").orEmpty()
            if (dataJson.isEmpty()) return null

            val decodedJson = decodeUrl(dataJson)
            val qrData = json.decodeFromString<QrDeviceData>(decodedJson)

            val formattedMac = formatMac(qrData.macAddress)
            val categoryModel = SupportedDevice.byCategoryCode(qrData.categoryCode)
            val model = categoryModel ?: SupportedDevice.fromDeviceName(qrData.name)
            Device(
                id = 0L,
                name = qrData.name,
                manufacturer = DeviceManufacturer.WENDOUGEE,
                model = model?.model.orEmpty(),
                modelId = model?.id,
                modelSource = model?.let { if (categoryModel != null) "qr-category" else "qr-name" },
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

    private fun formatMac(raw: String): String {
        val normalized = raw.replace(":", "").replace("-", "").uppercase()
        require(normalized.matches(Regex("[0-9A-F]{12}"))) { "Invalid BLE MAC" }
        return normalized.chunked(2).joinToString(":")
    }

    private fun decodeUrl(value: String): String {
        val bytes = mutableListOf<Byte>()
        var index = 0
        while (index < value.length) {
            when (value[index]) {
                '%' -> {
                    require(index + 2 < value.length) { "Incomplete URL escape" }
                    bytes += value.substring(index + 1, index + URL_ESCAPE_LENGTH).toInt(URL_HEX_RADIX).toByte()
                    index += URL_ESCAPE_LENGTH
                }
                '+' -> {
                    bytes += ' '.code.toByte()
                    index++
                }
                else -> {
                    val end = value.indexOfAny(charArrayOf('%', '+'), index).let { if (it < 0) value.length else it }
                    bytes += value.substring(index, end).encodeToByteArray().toList()
                    index = end
                }
            }
        }
        return bytes.toByteArray().decodeToString(throwOnInvalidSequence = true)
    }
}

private const val URL_HEX_RADIX = 16
private const val URL_ESCAPE_LENGTH = 3
