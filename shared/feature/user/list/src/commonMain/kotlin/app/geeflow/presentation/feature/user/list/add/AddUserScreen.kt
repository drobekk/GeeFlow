package app.geeflow.presentation.feature.user.list.add

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.geeflow.navigation.Navigator
import app.geeflow.navigation.NavigatorEffect
import app.geeflow.presentation.feature.user.list.add.AddUserEvent.BackClicked
import app.geeflow.presentation.feature.user.list.add.AddUserEvent.SaveNameClicked
import app.geeflow.ui.components.GeeFlowAgreementCheckbox
import app.geeflow.ui.components.GeeFlowDialogTopBar
import app.geeflow.ui.components.GeeFlowOutlinedTextField
import app.geeflow.ui.components.VerticalSpacer
import geeflow.shared.core.ui.generated.resources.common_confirm
import geeflow.shared.feature.user.list.generated.resources.Res
import geeflow.shared.feature.user.list.generated.resources.user_list_add_profile_name_hint
import geeflow.shared.feature.user.list.generated.resources.user_list_add_profile_title
import org.jetbrains.compose.resources.stringResource
import geeflow.shared.core.ui.generated.resources.Res as CoreRes

@Composable
internal fun AddUserScreen(viewModel: AddUserViewModel, navigator: Navigator) {
    NavigatorEffect(navigator, viewModel.navEvent)

    Content(onEvent = viewModel::handleEvent)
}

@Composable
private fun Content(
    onEvent: (AddUserEvent) -> Unit = {},
) {
    val nameState = rememberTextFieldState()
    var termsAgreed by rememberSaveable { mutableStateOf(false) }
    Surface(shape = MaterialTheme.shapes.large) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            GeeFlowDialogTopBar(
                title = stringResource(Res.string.user_list_add_profile_title),
                onCloseClick = { onEvent(BackClicked) },
            )
            VerticalSpacer(16.dp)
            GeeFlowOutlinedTextField(
                state = nameState,
                lineLimits = TextFieldLineLimits.SingleLine,
                label = { Text(stringResource(Res.string.user_list_add_profile_name_hint)) },
                modifier = Modifier.fillMaxWidth(),
            )
            VerticalSpacer(16.dp)
            GeeFlowAgreementCheckbox(
                checked = termsAgreed,
                onCheckedChange = { termsAgreed = it },
                modifier = Modifier.fillMaxWidth(),
            )
            VerticalSpacer(24.dp)
            Button(
                onClick = { onEvent(SaveNameClicked(nameState.text.toString())) },
                enabled = nameState.text.isNotBlank() && termsAgreed,
                modifier = Modifier.align(Alignment.End),
            ) {
                Text(stringResource(CoreRes.string.common_confirm))
            }
        }
    }
}
