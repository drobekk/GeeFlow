package app.geeflow.data.brew.impl

import app.cash.sqldelight.ColumnAdapter
import app.geeflow.data.brew.model.BrewDataPoint
import kotlinx.serialization.json.Json

object BrewDataPointsAdapter : ColumnAdapter<Map<Float, BrewDataPoint>, String> {
    override fun decode(databaseValue: String): Map<Float, BrewDataPoint> = Json.decodeFromString(databaseValue)
    override fun encode(value: Map<Float, BrewDataPoint>): String = Json.encodeToString(value)
}
