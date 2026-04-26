package app.geeflow.presentation.feature.user.settings.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.tooling.preview.PreviewWrapper
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.geeflow.navigation.Navigator
import app.geeflow.navigation.NavigatorEffect
import app.geeflow.presentation.feature.user.settings.profile.ProfileSettingsEvent.BackClicked
import app.geeflow.presentation.feature.user.settings.profile.ProfileSettingsEvent.DeleteClicked
import app.geeflow.presentation.feature.user.settings.profile.ProfileSettingsEvent.RenameClicked
import app.geeflow.ui.components.GeeFlowNavigationListItem
import app.geeflow.ui.components.GeeFlowScaffold
import app.geeflow.ui.isWidthExpanded
import app.geeflow.ui.theme.GeeFlowPreviewWrapper
import app.geeflow.ui.theme.GeeFlowScreenPreview
import app.geeflow.ui.theme.GeeFlowTheme
import geeflow.feature.user.settings.generated.resources.Res
import geeflow.feature.user.settings.generated.resources.user_settings_profile_change_name
import geeflow.feature.user.settings.generated.resources.user_settings_profile_change_name_description
import geeflow.feature.user.settings.generated.resources.user_settings_profile_change_picture
import geeflow.feature.user.settings.generated.resources.user_settings_profile_change_picture_description
import geeflow.feature.user.settings.generated.resources.user_settings_profile_delete
import geeflow.feature.user.settings.generated.resources.user_settings_profile_delete_description
import geeflow.feature.user.settings.generated.resources.user_settings_profile_description
import geeflow.feature.user.settings.generated.resources.user_settings_profile_title
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ProfileSettingsScreen(
    viewModel: ProfileSettingsViewModel,
    navigator: Navigator,
) {
    val viewState by viewModel.viewState.collectAsStateWithLifecycle()

    NavigatorEffect(navigator, viewModel.navEvent)

    Content(viewState = viewState, onEvent = viewModel::handleEvent)

    ProfileSettingsDialogs(
        dialog = viewState.dialog,
        currentName = viewState.name,
        onEvent = viewModel::handleEvent,
    )
}

@Composable
private fun Content(
    viewState: ProfileSettingsViewState,
    onEvent: (ProfileSettingsEvent) -> Unit = {},
) {
    if (isWidthExpanded()) {
        ExpandedContent(onEvent = onEvent)
    } else {
        CompactContent(viewState = viewState, onEvent = onEvent)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CompactContent(
    viewState: ProfileSettingsViewState,
    onEvent: (ProfileSettingsEvent) -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    GeeFlowScaffold(
        title = viewState.name.ifEmpty { stringResource(Res.string.user_settings_profile_title) },
        subtitle = stringResource(Res.string.user_settings_profile_description),
        navIconClick = { onEvent(BackClicked) },
        scrollBehavior = scrollBehavior,
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        content = { paddingValues ->
            SettingsContent(
                onEvent = onEvent,
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(paddingValues)
                    .padding(vertical = GeeFlowTheme.spacing.contentVertical),
            )
        },
    )
}

@Composable
private fun ExpandedContent(
    onEvent: (ProfileSettingsEvent) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(vertical = GeeFlowTheme.spacing.contentVertical),
    ) {
        SettingsContent(onEvent = onEvent)
    }
}

@Composable
private fun SettingsContent(
    onEvent: (ProfileSettingsEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        GeeFlowNavigationListItem(
            title = stringResource(Res.string.user_settings_profile_change_name),
            subtitle = stringResource(Res.string.user_settings_profile_change_name_description),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onEvent(RenameClicked) },
        )
        GeeFlowNavigationListItem(
            title = stringResource(Res.string.user_settings_profile_change_picture),
            subtitle = stringResource(Res.string.user_settings_profile_change_picture_description),
            enabled = false,
            modifier = Modifier.fillMaxWidth(),
        )
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.error) {
            GeeFlowNavigationListItem(
                title = stringResource(Res.string.user_settings_profile_delete),
                subtitle = stringResource(Res.string.user_settings_profile_delete_description),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onEvent(DeleteClicked) },
            )
        }
    }
}

@PreviewWrapper(GeeFlowPreviewWrapper::class)
@Composable
@GeeFlowScreenPreview
private fun Preview() {
    Content(ProfileSettingsViewState(name = "Barista"))
}
