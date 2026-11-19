package app.geeflow.domain.device.recipe

import kotlinx.serialization.json.*

/** The source JSON is also the editable document. The LITA snapshot is derived from it. */
object DecentRecipe {
    fun parse(text: String): JsonObject {
        require(text.length <= 2_000_000) { "Recipe file exceeds 2 MB" }
        val source = Json.parseToJsonElement(text).jsonObject
        require(source["version"] == JsonPrimitive("2") && source["steps"] is JsonArray) {
            "Select a Decent JSON v2 profile"
        }
        require(source.getValue("steps").jsonArray.all { it is JsonObject }) { "Each step must be an object" }
        return source
    }

    fun encode(source: JsonObject): String = Json { prettyPrint = true }.encodeToString(source)

    fun authoredSource(document: LiveRecipeDocument): JsonObject? =
        document.originalSourceJson?.takeIf { document.sourceIdentity == "decent-authored-v2" }?.let(::parse)

    fun compile(source: JsonObject, id: String): LiveRecipeDocument =
        LiveRecipeImport.parse(encode(source), id, "decent-authored-v2")

    fun newProfile(): JsonObject = parse("""
        {
          "title": "New recipe", "author": "", "notes": "", "beverage_type": "espresso",
          "steps": [${newStep()}], "tank_temperature": "0", "target_weight": "36",
          "target_volume": "0", "target_volume_count_start": "0",
          "legacy_profile_type": "settings_2c", "type": "advanced", "lang": "en",
          "hidden": "0", "reference_file": "", "changes_since_last_espresso": "", "version": "2"
        }
    """.trimIndent())

    fun newStep(): JsonObject = Json.parseToJsonElement("""
        {"name":"New stage","temperature":"92","sensor":"coffee","pump":"pressure",
         "transition":"fast","pressure":"3","flow":"0","seconds":"5","volume":"0",
         "weight":"0","limiter":{"value":"0","range":"0.6"}}
    """.trimIndent()).jsonObject
}
