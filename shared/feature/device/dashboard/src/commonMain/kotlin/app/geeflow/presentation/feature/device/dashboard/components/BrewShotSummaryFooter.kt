package app.geeflow.presentation.feature.device.dashboard.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.geeflow.data.brew.model.BrewProgram
import app.geeflow.data.brew.model.BrewScaleSample
import app.geeflow.data.brew.model.PhaseTransition
import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardEvent
import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardViewState
import app.geeflow.presentation.feature.device.dashboard.main.shotDurationSeconds
import app.geeflow.presentation.feature.device.dashboard.model.ChartData
import app.geeflow.presentation.feature.device.dashboard.model.historyTableData
import app.geeflow.presentation.feature.device.dashboard.model.HistoryTableRow
import app.geeflow.presentation.feature.device.dashboard.profiles.ProfileListViewState
import geeflow.shared.feature.device.dashboard.generated.resources.Res
import geeflow.shared.feature.device.dashboard.generated.resources.dashboard_history_index
import geeflow.shared.feature.device.dashboard.generated.resources.dashboard_history_last_sample
import geeflow.shared.feature.device.dashboard.generated.resources.dashboard_shot_date
import geeflow.shared.feature.device.dashboard.generated.resources.dashboard_decent_shot_history
import geeflow.shared.feature.device.dashboard.generated.resources.dashboard_shot_dose
import geeflow.shared.feature.device.dashboard.generated.resources.dashboard_shot_dose_missing
import geeflow.shared.feature.device.dashboard.generated.resources.dashboard_grind
import geeflow.shared.feature.device.dashboard.generated.resources.dashboard_shot_preinfusion
import geeflow.shared.feature.device.dashboard.generated.resources.dashboard_shot_extraction
import geeflow.shared.feature.device.dashboard.generated.resources.dashboard_shot_total
import geeflow.shared.feature.device.dashboard.generated.resources.dashboard_shot_time
import geeflow.shared.feature.device.dashboard.generated.resources.dashboard_shot_weight
import geeflow.shared.feature.device.dashboard.generated.resources.dashboard_shot_water
import geeflow.shared.feature.device.dashboard.generated.resources.dashboard_shot_brew_temperature
import geeflow.shared.feature.device.dashboard.generated.resources.dashboard_shot_pump_flow
import geeflow.shared.feature.device.dashboard.generated.resources.dashboard_shot_pressure
import geeflow.shared.feature.device.dashboard.generated.resources.dashboard_shot_estimated_split
import geeflow.shared.feature.device.dashboard.generated.resources.dashboard_shot_estimated_volume
import geeflow.shared.feature.device.dashboard.generated.resources.dashboard_shot_older
import geeflow.shared.feature.device.dashboard.generated.resources.dashboard_shot_newer
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

@Composable
internal fun BrewShotSummaryFooter(
    state: DeviceDashboardViewState,
    profiles: ProfileListViewState,
    measurements: Map<Float, ChartData>,
    scaleSamples: List<BrewScaleSample>,
    phaseProgram: BrewProgram?,
    phaseTransitions: List<PhaseTransition>,
    inProgress: Boolean,
    onEvent: (DeviceDashboardEvent) -> Unit,
    onHistoryNavigate: (String) -> Unit,
    onHistoryGrindSettingChanged: (String, String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val brew = state.brew
    val historyTable = historyTableData(phaseProgram, phaseTransitions, measurements, scaleSamples,
        brew.shotDurationSeconds, brew.finalWeightGrams, brew.recordedPhaseIds)
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val historyIndex = profiles.history.indexOfFirst { it.id == brew.historyId?.toString() }
        val narrow = maxWidth < 600.dp
        Card(
            Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            if (narrow) {
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ShotMetadata(
                        state = state,
                        historyItems = profiles.history,
                        historyIndex = historyIndex,
                        onHistoryNavigate = onHistoryNavigate,
                        onHistoryGrindSettingChanged = onHistoryGrindSettingChanged,
                        onEvent = onEvent,
                        showGrind = brew.historyId == null,
                    )
                    HistoryTable(historyTable.preinfusion, historyTable.extraction, historyTable.total)
                }
            } else {
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            stringResource(Res.string.dashboard_decent_shot_history),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                        )
                        ShotMetadata(
                            state = state,
                            historyItems = profiles.history,
                            historyIndex = historyIndex,
                            onHistoryNavigate = onHistoryNavigate,
                            onHistoryGrindSettingChanged = onHistoryGrindSettingChanged,
                            onEvent = onEvent,
                            modifier = Modifier.weight(1f),
                            compact = true,
                            showGrind = false,
                            singleLine = true,
                        )
                    }
                    HistoryTable(historyTable.preinfusion, historyTable.extraction, historyTable.total)
                }
            }
        }
    }
}

@Composable
private fun ShotMetadata(
    state: DeviceDashboardViewState,
    historyItems: List<ProfileListViewState.HistoryBrew>,
    historyIndex: Int,
    onHistoryNavigate: (String) -> Unit,
    onHistoryGrindSettingChanged: (String, String?) -> Unit,
    onEvent: (DeviceDashboardEvent) -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    showGrind: Boolean = true,
    singleLine: Boolean = false,
) {
    val brew = state.brew
    val isHistory = state.brew.historyId != null
    if (compact) {
        Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (isHistory) {
                IconButton(
                    onClick = { historyItems.getOrNull(historyIndex + 1)?.let { onHistoryNavigate(it.id) } },
                    enabled = historyIndex >= 0 && historyIndex + 1 < historyItems.size,
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.dashboard_shot_older))
                }
            }
            if (singleLine) {
                Text(
                    listOfNotNull(
                        if (isHistory && historyIndex >= 0) stringResource(Res.string.dashboard_history_index, historyIndex + 1) else null,
                        brew.startedAt?.let(::formatShotDate) ?: stringResource(Res.string.dashboard_shot_date),
                        brew.name.ifBlank { null },
                        brew.doseGrams?.let { stringResource(Res.string.dashboard_shot_dose, it.oneDecimal()) },
                    ).joinToString(" · "),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            } else {
                Column(Modifier.weight(if (showGrind) .85f else 1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(
                        brew.startedAt?.let(::formatShotDate) ?: stringResource(Res.string.dashboard_shot_date),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        listOfNotNull(
                            brew.name.ifBlank { null },
                            brew.doseGrams?.let { stringResource(Res.string.dashboard_shot_dose, it.oneDecimal()) },
                        ).joinToString(" · ").ifBlank { "—" },
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (showGrind) {
                OutlinedTextField(
                    value = if (isHistory || brew.startedAt != null) brew.grindSetting.orEmpty() else state.grindDraft,
                    onValueChange = {
                        if (isHistory || brew.startedAt != null) {
                            onEvent(DeviceDashboardEvent.GrindSettingChanged(it))
                            if (isHistory) brew.historyId?.let { id ->
                                onHistoryGrindSettingChanged(id.toString(), it.trim().takeIf(String::isNotEmpty))
                            }
                        } else onEvent(DeviceDashboardEvent.GrindDraftChanged(it))
                    },
                    label = { Text(stringResource(Res.string.dashboard_grind)) },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f).heightIn(min = 52.dp),
                )
            }
            if (isHistory) {
                IconButton(
                    onClick = { historyItems.getOrNull(historyIndex - 1)?.let { onHistoryNavigate(it.id) } },
                    enabled = historyIndex > 0,
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = stringResource(Res.string.dashboard_shot_newer))
                }
            }
        }
    } else Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isHistory) {
                IconButton(
                    onClick = { historyItems.getOrNull(historyIndex + 1)?.let { onHistoryNavigate(it.id) } },
                    enabled = historyIndex >= 0 && historyIndex + 1 < historyItems.size,
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.dashboard_shot_older))
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    brew.startedAt?.let(::formatShotDate) ?: stringResource(Res.string.dashboard_shot_date),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    brew.name.ifBlank { "—" },
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    brew.doseGrams?.let { stringResource(Res.string.dashboard_shot_dose, it.oneDecimal()) }
                        ?: stringResource(Res.string.dashboard_shot_dose_missing),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
            if (isHistory) {
                IconButton(
                    onClick = { historyItems.getOrNull(historyIndex - 1)?.let { onHistoryNavigate(it.id) } },
                    enabled = historyIndex > 0,
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = stringResource(Res.string.dashboard_shot_newer))
                }
            }
        }
        if (showGrind) OutlinedTextField(
            value = if (isHistory || brew.startedAt != null) brew.grindSetting.orEmpty() else state.grindDraft,
            onValueChange = {
                if (isHistory || brew.startedAt != null) {
                    onEvent(DeviceDashboardEvent.GrindSettingChanged(it))
                    if (isHistory) brew.historyId?.let { id ->
                        onHistoryGrindSettingChanged(id.toString(), it.trim().takeIf(String::isNotEmpty))
                    }
                }
                else onEvent(DeviceDashboardEvent.GrindDraftChanged(it))
            },
            label = { Text(stringResource(Res.string.dashboard_grind)) },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun HistoryTable(
    preinfusion: HistoryTableRow,
    extraction: HistoryTableRow,
    total: HistoryTableRow,
) {
    val weights = listOf(1.4f, .9f, .9f, .9f, 1f, 1f, 1f)
    val headers = listOf("", stringResource(Res.string.dashboard_shot_time),
        stringResource(Res.string.dashboard_shot_weight), stringResource(Res.string.dashboard_shot_water),
        stringResource(Res.string.dashboard_shot_brew_temperature), stringResource(Res.string.dashboard_shot_pump_flow),
        stringResource(Res.string.dashboard_shot_pressure))
    val preinfusionLabel = stringResource(Res.string.dashboard_shot_preinfusion)
    val extractionLabel = stringResource(Res.string.dashboard_shot_extraction)
    Column(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
      Column(Modifier.width(720.dp)) {
        Text(stringResource(Res.string.dashboard_history_last_sample), style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        SummaryRow(headers, weights, header = true)
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        HistoryTableRowContent(preinfusion, preinfusionLabel, weights)
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .5f))
        HistoryTableRowContent(extraction, extractionLabel, weights)
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .5f))
        HistoryTableRowContent(total, stringResource(Res.string.dashboard_shot_total), weights, total = true)
      }
    }
}

@Composable
private fun HistoryTableRowContent(row: HistoryTableRow, name: String, weights: List<Float>, total: Boolean = false) {
    SummaryRow(listOf(name, row.durationSeconds.asPreciseSeconds(), row.weightGrams.asGrams(),
        row.volumeMl.asMillilitres(false), row.temperatureC?.let { "${it.oneDecimal()} °C" } ?: "—",
        row.flowMlPerSecond?.let { "${it.oneDecimal()} ml/s" } ?: "—",
        row.pressureBar?.let { "${it.oneDecimal()} bar" } ?: "—"), weights, total = total)
}

@Composable
private fun SummaryRow(
    cells: List<String>,
    weights: List<Float>,
    header: Boolean = false,
    total: Boolean = false,
    estimated: Boolean = false,
    labelColor: androidx.compose.ui.graphics.Color? = null,
) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 1.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        cells.forEachIndexed { index, cell ->
            val label = if (index == 0 && estimated) "$cell · ${stringResource(Res.string.dashboard_shot_estimated_split)}" else cell
            Text(
                label,
                modifier = Modifier.weight(weights[index]),
                style = when {
                    header -> MaterialTheme.typography.labelSmall
                    total -> MaterialTheme.typography.labelMedium
                    else -> MaterialTheme.typography.bodySmall
                }.copy(fontFeatureSettings = "tnum"),
                color = if (index == 0 && labelColor != null) labelColor
                else if (header) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                fontWeight = if (total) FontWeight.SemiBold else FontWeight.Normal,
                textAlign = if (index == 0) TextAlign.Start else TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = if (header) 11.sp else 12.sp,
            )
        }
    }
}

private fun Double?.asPreciseSeconds(): String = this?.takeIf { it.isFinite() }?.let {
    if (it > 0.0 && it < 0.05) "<0.1 s" else "${(it * 10).roundToInt() / 10.0} s"
} ?: "—"
private fun Float?.asGrams(): String = this?.takeIf { it.isFinite() }?.let { "${it.oneDecimal()} g" } ?: "—"
private fun Float?.asMillilitres(estimated: Boolean): String = this?.takeIf { it.isFinite() }?.let {
    "${if (estimated) "≈" else ""}${it.roundToInt()} ml"
} ?: "—"
private fun Float.oneDecimal(): String {
    val tenths = (this * 10f).roundToInt()
    val fraction = kotlin.math.abs(tenths % 10)
    return "${tenths / 10}.$fraction"
}

private fun formatShotDate(instant: kotlin.time.Instant): String =
    ShotDateTimeFormat.format(instant.toLocalDateTime(TimeZone.currentSystemDefault()))

private val ShotDateTimeFormat = LocalDateTime.Format {
    year()
    char('-')
    monthNumber()
    char('-')
    day()
    char(' ')
    hour()
    char(':')
    minute()
}
