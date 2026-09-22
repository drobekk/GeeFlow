package app.geeflow.domain.device.usecase

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

fun maintenanceDay(): Long = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date.toEpochDays()
