package app.geeflow.domain.device.recipe

import kotlinx.serialization.json.*
import kotlin.test.*

class DecentRecipeTest {
    @Test fun roundTripRetainsSourceAndCompilesForLita() {
        val raw = checkNotNull(javaClass.getResource("/live_recipes/best_practice.json")).readText()
        val source = DecentRecipe.parse(raw)
        val steps = source.getValue("steps").jsonArray
        val edited = JsonObject(source + ("title" to JsonPrimitive("Edited")))
        val exported = DecentRecipe.parse(DecentRecipe.encode(edited))
        assertEquals(source - "title", exported - "title")
        assertEquals(JsonPrimitive("93.00"), steps.first().jsonObject["temperature"])
        val snapshot = DecentRecipe.compile(exported, "adaptive")
        assertEquals(88, snapshot.settings.temperatureC)
        assertEquals(101_000L, snapshot.settings.deadlineMillis)
        assertEquals(12_000L, snapshot.phases[1].maximumDurationMillis)
        assertEquals(0L, snapshot.phases[5].maximumDurationMillis)
        assertEquals(RecipeValueSource.Capture, snapshot.phases.last().rampStart.source)
    }

    @Test fun lastTimedStageDeterminesTemperature() {
        val source = DecentRecipe.newProfile()
        val first = JsonObject(DecentRecipe.newStep() + mapOf("temperature" to JsonPrimitive("91.6"),
            "seconds" to JsonPrimitive("0.123"), "sensor" to JsonPrimitive("water")))
        val marker = JsonObject(DecentRecipe.newStep() + mapOf("temperature" to JsonPrimitive("85"), "seconds" to JsonPrimitive("0")))
        val edited = JsonObject(source + ("steps" to JsonArray(listOf(first, marker))))
        val snapshot = DecentRecipe.compile(edited, "new")
        assertEquals(92, snapshot.settings.temperatureC)
        assertEquals(123L, snapshot.phases.first().maximumDurationMillis)
        assertTrue(snapshot.unsupportedFeatures.isEmpty())
        assertEquals(edited, DecentRecipe.authoredSource(snapshot))
    }
}
