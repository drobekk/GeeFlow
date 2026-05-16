package app.geeflow.presentation.feature.user.settings.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.tooling.preview.PreviewWrapper
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.geeflow.navigation.Navigator
import app.geeflow.navigation.NavigatorEffect
import app.geeflow.presentation.feature.user.settings.profile.ProfileSettingsEvent.BackClicked
import app.geeflow.presentation.feature.user.settings.profile.ProfileSettingsEvent.ChangePictureClicked
import app.geeflow.presentation.feature.user.settings.profile.ProfileSettingsEvent.DeleteClicked
import app.geeflow.presentation.feature.user.settings.profile.ProfileSettingsEvent.PhotoFilePicked
import app.geeflow.presentation.feature.user.settings.profile.ProfileSettingsEvent.RemovePhotoClicked
import app.geeflow.presentation.feature.user.settings.profile.ProfileSettingsEvent.RenameClicked
import app.geeflow.presentation.feature.user.settings.profile.ProfileSettingsViewModelEvent.OpenPhotoPicker
import app.geeflow.ui.EventsDispatcher
import app.geeflow.ui.components.GeeFlowNavigationListItem
import app.geeflow.ui.components.GeeFlowScaffold
import app.geeflow.ui.isWidthExpanded
import app.geeflow.ui.modifier.geeFlowInsetsEndPadding
import app.geeflow.ui.theme.GeeFlowPreviewWrapper
import app.geeflow.ui.theme.GeeFlowScreenPreview
import app.geeflow.ui.theme.GeeFlowTheme
import geeflow.shared.feature.user.settings.generated.resources.Res
import geeflow.shared.feature.user.settings.generated.resources.user_settings_profile_change_name
import geeflow.shared.feature.user.settings.generated.resources.user_settings_profile_change_name_description
import geeflow.shared.feature.user.settings.generated.resources.user_settings_profile_change_picture
import geeflow.shared.feature.user.settings.generated.resources.user_settings_profile_change_picture_description
import geeflow.shared.feature.user.settings.generated.resources.user_settings_profile_delete
import geeflow.shared.feature.user.settings.generated.resources.user_settings_profile_delete_description
import geeflow.shared.feature.user.settings.generated.resources.user_settings_profile_description
import geeflow.shared.feature.user.settings.generated.resources.user_settings_profile_remove_picture
import geeflow.shared.feature.user.settings.generated.resources.user_settings_profile_remove_picture_description
import geeflow.shared.feature.user.settings.generated.resources.user_settings_profile_title
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.readBytes
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ProfileSettingsScreen(
    viewModel: ProfileSettingsViewModel,
    navigator: Navigator,
) {
    val viewState by viewModel.viewState.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()

    NavigatorEffect(navigator, viewModel.navEvent)

    val photoPicker = rememberFilePickerLauncher(type = FileKitType.Image) { file ->
        coroutineScope.launch {
            file?.readBytes()?.let { viewModel.handleEvent(PhotoFilePicked(it)) }
        }
    }

    EventsDispatcher(viewModel.events) { event ->
        when (event) {
            OpenPhotoPicker -> photoPicker.launch()
        }
    }

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
        ExpandedContent(viewState = viewState, onEvent = onEvent)
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
                viewState = viewState,
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
    viewState: ProfileSettingsViewState,
    onEvent: (ProfileSettingsEvent) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = GeeFlowTheme.spacing.contentVertical)
            .geeFlowInsetsEndPadding(),
    ) {
        SettingsContent(viewState = viewState, onEvent = onEvent)
    }
}

@Composable
private fun SettingsContent(
    viewState: ProfileSettingsViewState,
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
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onEvent(ChangePictureClicked) },
        )
        if (viewState.photoFileName != null) {
            GeeFlowNavigationListItem(
                title = stringResource(Res.string.user_settings_profile_remove_picture),
                subtitle = stringResource(Res.string.user_settings_profile_remove_picture_description),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onEvent(RemovePhotoClicked) },
            )
        }
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
