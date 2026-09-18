package app.geeflow.presentation.feature.device.dashboard.model

import app.geeflow.data.brew.model.BrewProfile
import app.geeflow.data.brew.model.Condition
import geeflow.shared.feature.device.dashboard.generated.resources.Res
import geeflow.shared.feature.device.dashboard.generated.resources.profile_editor_description
import org.jetbrains.compose.resources.getString

suspend fun BrewProfile.displayDescription(): String {
    val conditionText = when (val cond = finishCondition) {
        null -> ""
        is Condition.Weight -> "${cond.target.toInt()}g"
        is Condition.Volume -> "${cond.target.toInt()}ml"
    }

    val generatedDescription = if (description.isBlank() && recording == null) {
        getString(
            Res.string.profile_editor_description,
            steps.size,
            steps.sumOf { it.time },
        )
    } else {
        description
    }

    return if (conditionText.isNotEmpty() && generatedDescription.isNotEmpty()) {
        "$conditionText • $generatedDescription"
    } else if (conditionText.isNotEmpty()) {
        conditionText
    } else {
        generatedDescription
    }
}
