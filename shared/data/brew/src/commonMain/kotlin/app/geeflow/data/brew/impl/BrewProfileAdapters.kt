package app.geeflow.data.brew.impl

import app.cash.sqldelight.ColumnAdapter
import app.geeflow.data.brew.model.BrewProgram
import app.geeflow.data.brew.model.Condition
import app.geeflow.data.brew.model.FreeHandRecording
import app.geeflow.data.brew.model.ProfileExecutionTrace
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

object FreeHandRecordingAdapter : ColumnAdapter<FreeHandRecording, String> {
    override fun decode(databaseValue: String): FreeHandRecording = Json.decodeFromString(databaseValue)
    override fun encode(value: FreeHandRecording): String = Json.encodeToString(value)
}

object BrewProgramAdapter : ColumnAdapter<BrewProgram, String> {
    override fun decode(databaseValue: String): BrewProgram = Json.decodeFromString(databaseValue)
    override fun encode(value: BrewProgram): String = Json.encodeToString(value)
}

object ProfileExecutionTraceAdapter : ColumnAdapter<ProfileExecutionTrace, String> {
    override fun decode(databaseValue: String): ProfileExecutionTrace = Json.decodeFromString(databaseValue)
    override fun encode(value: ProfileExecutionTrace): String = Json.encodeToString(value)
}
