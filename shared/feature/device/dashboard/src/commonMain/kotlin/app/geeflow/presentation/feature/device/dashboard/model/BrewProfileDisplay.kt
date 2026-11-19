package app.geeflow.presentation.feature.device.dashboard.model

import app.geeflow.data.brew.model.BrewProfile
import app.geeflow.data.brew.model.Condition
import app.geeflow.ui.text.ClearText
import geeflow.shared.feature.device.dashboard.generated.resources.Res
import geeflow.shared.feature.device.dashboard.generated.resources.profile_editor_description
import org.jetbrains.compose.resources.getString

suspend fun BrewProfile.displayDescription(): String {
    val conditionText = when (val cond = finishCondition) {
        null -> ""
        is Condition.Weight -> "${cond.target.toInt()}g"
        is Condition.Volume -> "${cond.target.toInt()}ml"
    }
    val generatedDescription = baseDescription()

    return if (conditionText.isNotEmpty() && generatedDescription.isNotEmpty()) {
        "$conditionText • $generatedDescription"
    } else if (conditionText.isNotEmpty()) {
        conditionText
    } else {
        generatedDescription
    }
}

suspend fun BrewProfile.profileCardDescription(): String = baseDescription()

private suspend fun BrewProfile.baseDescription(): String {
    return if (description.isBlank() && recording == null) {
        getString(
            Res.string.profile_editor_description,
            steps.size,
            steps.sumOf { it.time },
        )
    } else {
        presetDescriptionResource(description)?.let { getString(it.resource) } ?: description
    }
}

private fun presetDescriptionResource(description: String): ClearText? = when (description) {
    "Lever-style: gentle preinfusion, 9 bar peak with declining pressure" -> ClearText.RecipeCremina
    "Flow preinfusion, 30s bloom pause, then 2.2 ml/s extraction" -> ClearText.RecipeBlooming
    "Long low-flow preinfusion (1.5 ml/s) followed by 9 bar extraction" -> ClearText.RecipeSlayer
    "Low 6 bar peak declining to 4 bar, forgiving and smooth" -> ClearText.RecipeGentle
    "SOUP: low-pressure, high-flow extraction for sweetness & clarity, 20g in 60g out" -> ClearText.RecipeZuppa
    "Fast 6 bar extraction for coarse grind, 15s shot" -> ClearText.RecipeTurbo
    "Spring lever: 3 bar infusion, 9.5 bar peak, declining to 4.5 bar" -> ClearText.RecipeLondinium
    "High flow rate (4.5 ml/s) for light roasts, 90g out" -> ClearText.RecipeRao
    "Batch filter style: constant 3 ml/s flow through paper filter" -> ClearText.RecipeFilter
    "SOUP Lungo: extended low-pressure extraction for filter-like cup, 20g in 110g out" -> ClearText.RecipeZuppaLungo
    "Long extraction for high yield, 80g out" -> ClearText.RecipeEspressoLungo
    "Traditional 9 bar extraction, 18g in 36g out" -> ClearText.RecipeItaliano
    else -> null
}
