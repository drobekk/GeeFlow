package app.geeflow.presentation.feature.device.dashboard.model

import app.geeflow.data.brew.model.ExitCondition

internal data class ChartPhaseBoundary(
    val seconds: Double,
    val stepNumber: Int? = null,
    val conditions: List<ExitCondition> = emptyList(),
    val matchedConditions: Set<ExitCondition> = emptySet(),
    val actual: Boolean = false,
)
