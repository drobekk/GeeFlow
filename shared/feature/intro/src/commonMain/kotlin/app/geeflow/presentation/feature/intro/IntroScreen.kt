package app.geeflow.presentation.feature.intro

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewWrapper
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.geeflow.navigation.Navigator
import app.geeflow.navigation.NavigatorEffect
import app.geeflow.ui.components.GeeFlowAgreementCheckbox
import app.geeflow.ui.components.GeeFlowIconButton
import app.geeflow.ui.components.GeeFlowOutlinedTextField
import app.geeflow.ui.components.GeeFlowScaffold
import app.geeflow.ui.components.VerticalSpacer
import app.geeflow.ui.icons.GeeFlowIcon
import app.geeflow.ui.icons.Logo
import app.geeflow.ui.theme.GeeFlowPreviewWrapper
import app.geeflow.ui.theme.GeeFlowScreenPreview
import geeflow.shared.core.ui.generated.resources.app_name
import geeflow.shared.core.ui.generated.resources.common_confirm
import geeflow.shared.core.ui.generated.resources.common_user_name
import geeflow.shared.feature.intro.generated.resources.Res
import geeflow.shared.feature.intro.generated.resources.intro_screen_set_up
import geeflow.shared.feature.intro.generated.resources.intro_screen_welcome_message
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import geeflow.shared.core.ui.generated.resources.Res as UiRes

@Composable
fun IntroScreen(navigator: Navigator) {
    val viewModel = koinViewModel<IntroViewModel>()

    IntroScreenContent(
        onEvent = viewModel::handleEvent,
    )

    NavigatorEffect(navigator, viewModel.navEvent)
}

@Composable
private fun IntroScreenContent(
    onEvent: (IntroEvent) -> Unit = {},
) {
    GeeFlowScaffold(
        topBar = { sideHeader -> Logo(expanded = sideHeader) },
        content = {
            Form(
                onEvent = onEvent,
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(it),
            )
        },
        modifier = Modifier.imePadding(),
    )
}

@Composable
private fun Logo(expanded: Boolean, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .layout { measurable, constraints ->
                val resolvedConstraints = if (expanded) {
                    constraints.copy(minHeight = constraints.maxHeight)
                } else {
                    constraints.copy(minWidth = constraints.maxWidth)
                }
                val placeable = measurable.measure(resolvedConstraints)
                layout(placeable.width, placeable.height) {
                    placeable.placeRelative(0, 0)
                }
            }
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        VerticalSpacer(24.dp)
        Icon(
            painter = rememberVectorPainter(GeeFlowIcon.Logo),
            modifier = Modifier.size(120.dp),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
        VerticalSpacer(24.dp)
        Text(
            text = stringResource(UiRes.string.app_name),
            style = MaterialTheme.typography.displayMedium,
            maxLines = 1,
            autoSize = TextAutoSize.StepBased(
                minFontSize = 24.sp,
                maxFontSize = 48.sp,
            ),
        )
        Text(
            text = stringResource(Res.string.intro_screen_welcome_message),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun Form(
    onEvent: (IntroEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(60.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        val userNameTextFieldState = rememberTextFieldState()
        var termsAgreed by rememberSaveable { mutableStateOf(false) }
        Text(
            text = stringResource(Res.string.intro_screen_set_up),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
        VerticalSpacer(16.dp)
        GeeFlowOutlinedTextField(
            state = userNameTextFieldState,
            lineLimits = TextFieldLineLimits.SingleLine,
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
            onKeyboardAction = {
                if (userNameTextFieldState.text.isNotEmpty() && termsAgreed) {
                    onEvent(IntroEvent.ConfirmClicked(userNameTextFieldState.text.toString()))
                }
            },
            label = { Text(stringResource(UiRes.string.common_user_name)) },
        )
        VerticalSpacer(16.dp)
        GeeFlowAgreementCheckbox(
            checked = termsAgreed,
            onCheckedChange = { termsAgreed = it },
            modifier = Modifier.fillMaxWidth(),
        )
        VerticalSpacer(32.dp)
        VerticalSpacer(1f)
        GeeFlowIconButton(
            painter = rememberVectorPainter(Icons.Filled.Check),
            enabled = userNameTextFieldState.text.isNotEmpty() && termsAgreed,
            contentDescription = stringResource(UiRes.string.common_confirm),
            onClick = {
                if (userNameTextFieldState.text.isNotEmpty() && termsAgreed) {
                    onEvent(IntroEvent.ConfirmClicked(userNameTextFieldState.text.toString()))
                }
            },
        )
    }
}

@PreviewWrapper(GeeFlowPreviewWrapper::class)
@Composable
@GeeFlowScreenPreview
private fun Preview() {
    IntroScreenContent()
}
