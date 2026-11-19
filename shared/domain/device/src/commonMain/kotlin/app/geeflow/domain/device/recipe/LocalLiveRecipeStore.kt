package app.geeflow.domain.device.recipe

import app.geeflow.domain.device.LiveControlSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Separate local storage, deliberately outside BrewProfileRepository and its sync path. */
class LocalLiveRecipeStore(private val read: () -> String?, private val write: (String) -> Unit) {
    @Serializable private data class Collection(val version: Int = 1, val recipes: List<LiveRecipeDocument> = emptyList())
    private val json = Json { encodeDefaults = true }
    private val loaded = runCatching { read()?.let { json.decodeFromString<Collection>(it).also { collection -> require(collection.version == 1) } } ?: Collection() }
    val loadError: String? = loaded.exceptionOrNull()?.message
    private val mutableRecipes = MutableStateFlow(loaded.getOrNull()?.recipes.orEmpty())
    val recipes = mutableRecipes.asStateFlow()

    fun save(document: LiveRecipeDocument) {
        check(loadError == null) { "Local recipe storage could not be read; existing data has been preserved: $loadError" }
        require(document.version == 1 && document.id.isNotBlank())
        val updated = recipes.value.filterNot { it.id == document.id } + LiveRecipeJson.decode(LiveRecipeJson.encode(document))
        write(json.encodeToString(Collection(recipes = updated)))
        mutableRecipes.value = updated
    }

    fun delete(id: String) {
        check(loadError == null)
        val updated = recipes.value.filterNot { it.id == id }
        write(json.encodeToString(Collection(recipes = updated)))
        mutableRecipes.value = updated
    }
}

fun LiveRecipeDocument.controlSettings() = LiveControlSettings(
    basePressureBar = settings.basePressureBar,
    maximumPressureBar = settings.pressureCeilingBar,
    slewBarPerSecond = settings.pressureSlewBarPerSecond,
    proportionalGainBarPerGramPerSecond = settings.cupFlowGains.kp,
    integralGainBarPerGram = settings.cupFlowGains.ki,
    commandIntervalMillis = settings.commandIntervalMillis,
    shotDeadlineMillis = settings.deadlineMillis,
)
