package app.geeflow.data.brew.impl

import app.cash.sqldelight.ColumnAdapter
import app.geeflow.data.brew.model.Condition
import app.geeflow.data.brew.model.ProfileStep
import kotlinx.serialization.json.Json

object ConditionAdapter : ColumnAdapter<Condition, String> {
    override fun decode(databaseValue: String): Condition = Json.decodeFromString(databaseValue)
    override fun encode(value: Condition): String = Json.encodeToString(value)
}

object ProfileStepsAdapter : ColumnAdapter<List<ProfileStep>, String> {
    override fun decode(databaseValue: String): List<ProfileStep> = Json.decodeFromString(databaseValue)
    override fun encode(value: List<ProfileStep>): String = Json.encodeToString(value)
}
