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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
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
import dev.drobek.geeflow.ui.EventsDispatcher
import dev.drobek.geeflow.ui.VerticalSpacer
import dev.drobek.geeflow.ui.components.GeeFlowIconButton
import dev.drobek.geeflow.ui.components.GeeFlowOutlinedTextField
import dev.drobek.geeflow.ui.components.GeeFlowScaffold
import dev.drobek.geeflow.ui.conditional
import dev.drobek.geeflow.ui.icons.AppLogo
import dev.drobek.geeflow.ui.icons.GeeFlowIcon
import dev.drobek.geeflow.ui.isExpanded
import dev.drobek.geeflow.ui.theme.GeeFlowScreenPreview
import dev.drobek.geeflow.ui.theme.GeeFlowTheme
import geeflow.composeapp.generated.resources.Res
import geeflow.composeapp.generated.resources.app_name
import geeflow.composeapp.generated.resources.common_confirm
import geeflow.composeapp.generated.resources.common_user_name
import geeflow.composeapp.generated.resources.intro_screen_set_up
import geeflow.composeapp.generated.resources.intro_screen_welcome_message
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun IntroScreen(introNavigation: IntroNavigation) {
    val viewModel = koinViewModel<IntroViewModel>()

    IntroScreenContent(
        onEvent = viewModel::handleEvent
    )

    EventsDispatcher(viewModel.events) {
        when (it) {
            is Navigation.AddDevice -> {
                introNavigation.clearBackStack()
                introNavigation.showAddDevice()
            }
        }
    }
}

@Composable
private fun IntroScreenContent(
    onEvent: (IntroEvent) -> Unit = {}
) {
    GeeFlowScaffold(
        topBar = { Logo() },
        content = {
            Form(
                onEvent = onEvent,
                modifier = Modifier.padding(it)
            )
        }
    )
}

@Composable
private fun Logo(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .conditional(
                condition = isExpanded(),
                ifTrue = { fillMaxHeight() },
                ifFalse = { fillMaxWidth() }
            )
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .systemBarsPadding()
            .padding(60.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = rememberVectorPainter(GeeFlowIcon.AppLogo),
            modifier = Modifier.size(140.dp),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        VerticalSpacer(24.dp)
        Text(
            text = stringResource(Res.string.app_name),
            style = MaterialTheme.typography.displayMedium
        )
        Text(
            text = stringResource(Res.string.intro_screen_welcome_message),
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
private fun Form(
    onEvent: (IntroEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .imePadding()
            .navigationBarsPadding()
            .padding(60.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val userNameTextFieldState = rememberTextFieldState()
        Text(
            text = stringResource(Res.string.intro_screen_set_up),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
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
            label = { Text(stringResource(Res.string.common_user_name)) }
        )
        VerticalSpacer(32.dp)
        VerticalSpacer(1f)
        GeeFlowIconButton(
            painter = rememberVectorPainter(Icons.Filled.Check),
            enabled = userNameTextFieldState.text.isNotEmpty(),
            contentDescription = stringResource(Res.string.common_confirm),
            onClick = { onEvent(IntroEvent.ConfirmClicked(userNameTextFieldState.text.toString())) }
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
