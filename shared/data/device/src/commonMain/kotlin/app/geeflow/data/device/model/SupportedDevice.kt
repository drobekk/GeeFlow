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
        categoryCode = "10011"
    )
    data object Lita : SupportedDevice(DeviceManufacturer.WENDOUGEE, "LITA", id = "wendougee.lita")

    data object GeeFlowDemo : SupportedDevice(DeviceManufacturer.GEEFLOW, "Demo", "V1", "geeflow.demo")

    companion object {
        fun all(): List<SupportedDevice> = listOf(
            WendougeeDataS,
            Lita,
            GeeFlowDemo
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
                ?: Lita.takeIf { manufacturer.equals(DeviceManufacturer.WENDOUGEE, true) && model in LEGACY_LITA_NAMES }

        fun fromDeviceName(name: String): SupportedDevice? {
            val normalized = name.uppercase().replace('_', ' ').replace('-', ' ')
            val tokens = normalized.split(Regex("[^A-Z0-9]+")).filter { it.isNotEmpty() }
            if ("LITA" in tokens) return Lita
            if (Regex("\\bDATA\\s*S\\b").containsMatchIn(normalized)) return WendougeeDataS
            return null
        }

        fun byId(id: String?): SupportedDevice? = all().find { it.id == id } ?: Lita.takeIf { id in LEGACY_LITA_IDS }

        fun byCategoryCode(code: String?): SupportedDevice? = when (code) {
            "10011" -> WendougeeDataS
            "10021", "10022", "10023", "10024", "10025" -> Lita
            else -> null
        }

        // Old variant identifiers remain readable but are not separate selectable models.
        private val LEGACY_LITA_IDS = setOf(
            "wendougee.lita.ba",
            "wendougee.lita.la",
            "wendougee.lita.br",
            "wendougee.lita.lr",
            "wendougee.lita.low",
            "wendougee.lita.high",
            "wendougee.lita.water-low",
            "wendougee.lita.water-high",
            "wendougee.lita.super",
        )
        private val LEGACY_LITA_NAMES = setOf(
            "LITA BA",
            "LITA LA",
            "LITA BR",
            "LITA LR",
            "LITA Low",
            "LITA High",
            "LITA Water Low",
            "LITA Water High",
            "LITA Super",
        )
    }
}

val Device.supportedDevice: SupportedDevice?
    get() = if (modelId != null) SupportedDevice.byId(modelId) else SupportedDevice.find(manufacturer, model, version)

val Device.isDemo: Boolean get() = supportedDevice == SupportedDevice.GeeFlowDemo

object DeviceManufacturer {
    const val WENDOUGEE = "Wendougee"
    const val GEEFLOW = "GeeFlow"
}
