package app.geeflow.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import geeflow.shared.core.ui.generated.resources.Res
import geeflow.shared.core.ui.generated.resources.common_confirm
import geeflow.shared.core.ui.generated.resources.device_model_select
import geeflow.shared.core.ui.generated.resources.device_model_title
import org.jetbrains.compose.resources.stringResource

@Composable
fun DeviceModelPicker(
    models: List<Pair<String, String>>,
    onSelect: (String) -> Unit,
    onClose: () -> Unit,
    initialModelId: String? = null,
) {
    var selectedId by rememberSaveable(initialModelId, models) {
        mutableStateOf(initialModelId?.takeIf { id -> models.any { it.first == id } })
    }
    Column(Modifier.padding(24.dp).verticalScroll(rememberScrollState())) {
        GeeFlowDialogTopBar(title = stringResource(Res.string.device_model_title), onCloseClick = onClose)
        Text(
            text = stringResource(Res.string.device_model_select),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 16.dp),
        )
        Column(Modifier.selectableGroup()) {
            models.forEach { (id, label) ->
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .selectable(selected = selectedId == id, role = Role.RadioButton, onClick = { selectedId = id })
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = selectedId == id, onClick = null)
                    Text(label, modifier = Modifier.padding(start = 16.dp))
                }
            }
        }
        Button(
            onClick = { selectedId?.let(onSelect) },
            enabled = selectedId != null,
            modifier = Modifier.align(Alignment.End).padding(top = 16.dp),
        ) {
            Text(stringResource(Res.string.common_confirm))
        }
    }
}
