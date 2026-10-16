package app.geeflow.data.device.model

sealed class SupportedDevice(
    val manufacturer: String,
    val model: String,
    val version: String = "",
    val id: String,
    val categoryCode: String? = null,
) {
    data object WendougeeDataS : SupportedDevice(
        manufacturer = DeviceManufacturer.WENDOUGEE,
        model = "Data-S",
        version = "V180",
        id = "wendougee.data-s",
        categoryCode = "10011",
    )

    data object LitaBA : SupportedDevice(
        manufacturer = DeviceManufacturer.WENDOUGEE,
        model = "LITA BA",
        id = "wendougee.lita.ba",
    )

    data object LitaLA : SupportedDevice(
        manufacturer = DeviceManufacturer.WENDOUGEE,
        model = "LITA LA",
        id = "wendougee.lita.la",
    )

    data object GeeFlowDemo : SupportedDevice(
        manufacturer = DeviceManufacturer.GEEFLOW,
        model = "Demo",
        version = "V1",
        id = "geeflow.demo",
    )

    val isLita: Boolean get() = this == LitaBA || this == LitaLA

    companion object {
        fun all(): List<SupportedDevice> = listOf(
            WendougeeDataS,
            LitaBA,
            LitaLA,
            GeeFlowDemo,
        )

        fun manufacturers(): List<String> = all().map { it.manufacturer }.distinct()
        fun modelsFor(manufacturer: String): List<String> = all().filter {
            it.manufacturer == manufacturer
        }.map { it.model }

        fun versionsFor(manufacturer: String, model: String): List<SupportedDevice> =
            all().filter { it.manufacturer == manufacturer && it.model == model }

        @Suppress("UnusedParameter")
        fun find(manufacturer: String, model: String, version: String): SupportedDevice? =
            all().find { it.manufacturer.equals(manufacturer, true) && it.model.equals(model, true) }

        fun fromDeviceName(name: String): SupportedDevice? {
            val normalized = name.uppercase().replace('_', ' ').replace('-', ' ')
            if (Regex("\\bLITA\\s+LA\\b").containsMatchIn(normalized)) return LitaLA
            if (Regex("\\bLITA\\s+BA\\b").containsMatchIn(normalized)) return LitaBA
            if (Regex("\\bDATA\\s*S\\b").containsMatchIn(normalized)) return WendougeeDataS
            return null
        }

        fun byId(id: String?): SupportedDevice? =
            all().find { it.id == id }

        fun byCategoryCode(code: String?): SupportedDevice? = when (code) {
            "10011" -> WendougeeDataS
            else -> null
        }
    }
}

val Device.supportedDevice: SupportedDevice?
    get() = if (modelId != null) SupportedDevice.byId(modelId) else SupportedDevice.find(manufacturer, model, version)

val Device.isDemo: Boolean get() = supportedDevice == SupportedDevice.GeeFlowDemo

object DeviceManufacturer {
    const val WENDOUGEE = "Wendougee"
    const val GEEFLOW = "GeeFlow"
}
