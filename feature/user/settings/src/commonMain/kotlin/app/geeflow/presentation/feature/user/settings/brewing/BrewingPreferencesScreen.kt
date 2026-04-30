package app.geeflow.presentation.feature.user.settings.brewing

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.tooling.preview.PreviewWrapper
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.geeflow.navigation.Navigator
import app.geeflow.navigation.NavigatorEffect
import app.geeflow.presentation.feature.user.settings.brewing.BrewingPreferencesEvent.AutoConnectChanged
import app.geeflow.presentation.feature.user.settings.brewing.BrewingPreferencesEvent.BackClicked
import app.geeflow.presentation.feature.user.settings.brewing.BrewingPreferencesEvent.RestoreDefaultProfilesClicked
import app.geeflow.presentation.feature.user.settings.brewing.BrewingPreferencesEvent.TemperatureUnitClicked
import app.geeflow.presentation.feature.user.settings.brewing.components.BrewingPreferencesDialogs
import app.geeflow.ui.components.GeeFlowNavigationListItem
import app.geeflow.ui.components.GeeFlowScaffold
import app.geeflow.ui.components.GeeFlowToggleListItem
import app.geeflow.ui.isWidthExpanded
import app.geeflow.ui.modifier.geeFlowInsetsEndPadding
import app.geeflow.ui.theme.GeeFlowPreviewWrapper
import app.geeflow.ui.theme.GeeFlowScreenPreview
import app.geeflow.ui.theme.GeeFlowTheme
import geeflow.feature.user.settings.generated.resources.Res
import geeflow.feature.user.settings.generated.resources.user_settings_brewing_auto_connect
import geeflow.feature.user.settings.generated.resources.user_settings_brewing_auto_connect_description
import geeflow.feature.user.settings.generated.resources.user_settings_brewing_preferences_description
import geeflow.feature.user.settings.generated.resources.user_settings_brewing_preferences_title
import geeflow.feature.user.settings.generated.resources.user_settings_brewing_restore_default_profiles
import geeflow.feature.user.settings.generated.resources.user_settings_brewing_restore_default_profiles_description
import geeflow.feature.user.settings.generated.resources.user_settings_brewing_temperature_unit
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun BrewingPreferencesScreen(
    viewModel: BrewingPreferencesViewModel,
    navigator: Navigator,
) {
    val viewState by viewModel.viewState.collectAsStateWithLifecycle()

    NavigatorEffect(navigator, viewModel.navEvent)

    Content(
        viewState = viewState,
        onEvent = viewModel::handleEvent,
    )

    BrewingPreferencesDialogs(
        dialog = viewState.dialog,
        currentTemperatureUnit = viewState.temperatureUnit,
        onEvent = viewModel::handleEvent,
    )
}

@Composable
private fun Content(
    viewState: BrewingPreferencesViewState,
    onEvent: (BrewingPreferencesEvent) -> Unit = {},
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
    viewState: BrewingPreferencesViewState,
    onEvent: (BrewingPreferencesEvent) -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    GeeFlowScaffold(
        title = stringResource(Res.string.user_settings_brewing_preferences_title),
        subtitle = stringResource(Res.string.user_settings_brewing_preferences_description),
        navIconClick = { onEvent(BackClicked) },
        scrollBehavior = scrollBehavior,
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        content = { paddingValues ->
            BrewingContent(
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
    viewState: BrewingPreferencesViewState,
    onEvent: (BrewingPreferencesEvent) -> Unit,
) {
    BrewingContent(
        viewState = viewState,
        onEvent = onEvent,
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = GeeFlowTheme.spacing.contentVertical)
            .geeFlowInsetsEndPadding(),
    )
}

@Composable
private fun BrewingContent(
    viewState: BrewingPreferencesViewState,
    onEvent: (BrewingPreferencesEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        GeeFlowToggleListItem(
            title = stringResource(Res.string.user_settings_brewing_auto_connect),
            subtitle = stringResource(Res.string.user_settings_brewing_auto_connect_description),
            checked = viewState.autoConnect,
            onCheckedChanged = { onEvent(AutoConnectChanged(it)) },
            modifier = Modifier.fillMaxWidth(),
        )
        GeeFlowNavigationListItem(
            title = stringResource(Res.string.user_settings_brewing_temperature_unit),
            subtitle = stringResource(viewState.temperatureUnit.titleRes),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onEvent(TemperatureUnitClicked) },
        )
        GeeFlowNavigationListItem(
            title = stringResource(Res.string.user_settings_brewing_restore_default_profiles),
            subtitle = stringResource(Res.string.user_settings_brewing_restore_default_profiles_description),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onEvent(RestoreDefaultProfilesClicked) },
        )
    }
}

@PreviewWrapper(GeeFlowPreviewWrapper::class)
@Composable
@GeeFlowScreenPreview
private fun Preview() {
    Content(viewState = BrewingPreferencesViewState())
}
