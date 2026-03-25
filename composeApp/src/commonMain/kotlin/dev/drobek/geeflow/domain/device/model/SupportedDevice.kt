package dev.drobek.geeflow.domain.device.model

sealed class SupportedDevice(
    val manufacturer: String,
    val model: String,
    val version: String
) {
    data object WendougeeDataS : SupportedDevice("Wendougee", "Data-S", "V180")
    data object GeeFlowDemo : SupportedDevice("GeeFlow", "Demo", "V1")

    companion object {
        fun all(): List<SupportedDevice> = listOf(WendougeeDataS, GeeFlowDemo)
        fun manufacturers(): List<String> = all().map { it.manufacturer }.distinct()
        fun modelsFor(manufacturer: String): List<String> =
            all().filter { it.manufacturer == manufacturer }.map { it.model }.distinct()
        fun versionsFor(manufacturer: String, model: String): List<SupportedDevice> =
            all().filter { it.manufacturer == manufacturer && it.model == model }
        fun find(manufacturer: String, model: String, version: String): SupportedDevice? =
            all().find { it.manufacturer == manufacturer && it.model == model && it.version == version }
    }
}

val Device.supportedDevice: SupportedDevice?
    get() = SupportedDevice.find(manufacturer, model, version)
