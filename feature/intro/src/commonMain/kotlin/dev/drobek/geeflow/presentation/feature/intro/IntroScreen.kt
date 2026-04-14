package dev.drobek.geeflow.presentation.feature.intro

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.drobek.geeflow.navigation.Navigator
import dev.drobek.geeflow.navigation.NavigatorEffect
import dev.drobek.geeflow.ui.components.GeeFlowIconButton
import dev.drobek.geeflow.ui.components.GeeFlowOutlinedTextField
import dev.drobek.geeflow.ui.components.GeeFlowScaffold
import dev.drobek.geeflow.ui.components.VerticalSpacer
import dev.drobek.geeflow.ui.icons.GeeFlowIcon
import dev.drobek.geeflow.ui.icons.Logo
import dev.drobek.geeflow.ui.isWidthExpanded
import dev.drobek.geeflow.ui.modifier.conditional
import dev.drobek.geeflow.ui.theme.GeeFlowScreenPreview
import dev.drobek.geeflow.ui.theme.GeeFlowTheme
import geeflow.core.ui.generated.resources.app_name
import geeflow.core.ui.generated.resources.common_confirm
import geeflow.core.ui.generated.resources.common_user_name
import geeflow.feature.intro.generated.resources.Res
import geeflow.feature.intro.generated.resources.intro_screen_set_up
import geeflow.feature.intro.generated.resources.intro_screen_welcome_message
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import geeflow.core.ui.generated.resources.Res as UiRes

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
        topBar = { Logo() },
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
private fun Logo(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .conditional(
                condition = isWidthExpanded(),
                ifTrue = { fillMaxHeight() },
                ifFalse = { fillMaxWidth() },
            )
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .statusBarsPadding()
            .padding(60.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            painter = rememberVectorPainter(GeeFlowIcon.Logo),
            modifier = Modifier.size(140.dp),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
        VerticalSpacer(24.dp)
        Text(
            text = stringResource(UiRes.string.app_name),
            style = MaterialTheme.typography.displayMedium,
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
            .navigationBarsPadding()
            .padding(60.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        val userNameTextFieldState = rememberTextFieldState()
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
                if (userNameTextFieldState.text.isNotEmpty()) {
                    onEvent(IntroEvent.ConfirmClicked(userNameTextFieldState.text.toString()))
                }
            },
            label = { Text(stringResource(UiRes.string.common_user_name)) },
        )
        VerticalSpacer(32.dp)
        VerticalSpacer(1f)
        GeeFlowIconButton(
            painter = rememberVectorPainter(Icons.Filled.Check),
            enabled = userNameTextFieldState.text.isNotEmpty(),
            contentDescription = stringResource(UiRes.string.common_confirm),
            onClick = { onEvent(IntroEvent.ConfirmClicked(userNameTextFieldState.text.toString())) },
        )
    }
}

@Composable
@GeeFlowScreenPreview
private fun PreviewLight() = GeeFlowTheme(false) {
    IntroScreenContent()
}

@Composable
@GeeFlowScreenPreview
private fun PreviewDark() = GeeFlowTheme(true) {
    IntroScreenContent()
}
