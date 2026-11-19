package app.geeflow.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/** Select a complete value so partial keypad input never becomes a setting. */
@Composable
fun GeeFlowSettingOptions(
    title: String,
    options: List<String>,
    current: Float?,
    unit: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val selectedIndex = options.indexOfFirst { it.toFloatOrNull() == current }
    GeeFlowDialog(onDismissRequest = onDismiss) {
        Column(Modifier.widthIn(max = 420.dp).fillMaxWidth().padding(16.dp)) {
            GeeFlowDialogTopBar(title = title, onCloseClick = onDismiss)
            LazyColumn(
                Modifier.fillMaxWidth().heightIn(max = 340.dp),
                state = rememberLazyListState((selectedIndex - 2).coerceAtLeast(0)),
            ) {
                items(options) { option ->
                    val isSelected = option.toFloatOrNull() == current
                    Text(
                        text = if (unit.isBlank() || unit == "°") "$option$unit" else "$option $unit",
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.fillMaxWidth()
                            .semantics { selected = isSelected }
                            .clickable { onSelect(option); onDismiss() }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                    )
                }
            }
        }
    }
}
