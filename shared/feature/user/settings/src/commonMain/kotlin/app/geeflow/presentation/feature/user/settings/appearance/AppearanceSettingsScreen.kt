package app.geeflow.presentation.feature.user.settings.appearance

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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.geeflow.data.user.model.AppTheme
import app.geeflow.navigation.Navigator
import app.geeflow.navigation.NavigatorEffect
import app.geeflow.platform.isFullScreenSupported
import app.geeflow.platform.isKeepScreenOnSupported
import app.geeflow.platform.rememberLanguageSettingsLauncher
import app.geeflow.presentation.feature.user.settings.appearance.AppearanceSettingsEvent.AppThemeClicked
import app.geeflow.presentation.feature.user.settings.appearance.AppearanceSettingsEvent.BackClicked
import app.geeflow.presentation.feature.user.settings.appearance.AppearanceSettingsEvent.CustomColorClicked
import app.geeflow.presentation.feature.user.settings.appearance.AppearanceSettingsEvent.DarkModeClicked
import app.geeflow.presentation.feature.user.settings.appearance.AppearanceSettingsEvent.FullScreenChanged
import app.geeflow.presentation.feature.user.settings.appearance.AppearanceSettingsEvent.KeepScreenOnChanged
import app.geeflow.presentation.feature.user.settings.appearance.AppearanceSettingsEvent.PaletteStyleClicked
import app.geeflow.presentation.feature.user.settings.appearance.components.AppearanceDialogs
import app.geeflow.ui.components.GeeFlowNavigationListItem
import app.geeflow.ui.components.GeeFlowScaffold
import app.geeflow.ui.components.GeeFlowToggleListItem
import app.geeflow.ui.isWidthExpanded
import app.geeflow.ui.modifier.geeFlowInsetsEndPadding
import app.geeflow.ui.theme.GeeFlowPreviewWrapper
import app.geeflow.ui.theme.GeeFlowScreenPreview
import app.geeflow.ui.theme.GeeFlowTheme
import geeflow.shared.feature.user.settings.generated.resources.Res
import geeflow.shared.feature.user.settings.generated.resources.user_settings_appearance_app_theme
import geeflow.shared.feature.user.settings.generated.resources.user_settings_appearance_custom_color
import geeflow.shared.feature.user.settings.generated.resources.user_settings_appearance_custom_color_description
import geeflow.shared.feature.user.settings.generated.resources.user_settings_appearance_dark_mode
import geeflow.shared.feature.user.settings.generated.resources.user_settings_appearance_description
import geeflow.shared.feature.user.settings.generated.resources.user_settings_appearance_full_screen
import geeflow.shared.feature.user.settings.generated.resources.user_settings_appearance_full_screen_description
import geeflow.shared.feature.user.settings.generated.resources.user_settings_appearance_keep_screen_on
import geeflow.shared.feature.user.settings.generated.resources.user_settings_appearance_keep_screen_on_description
import geeflow.shared.feature.user.settings.generated.resources.user_settings_appearance_language
import geeflow.shared.feature.user.settings.generated.resources.user_settings_appearance_language_description
import geeflow.shared.feature.user.settings.generated.resources.user_settings_appearance_palette_style
import geeflow.shared.feature.user.settings.generated.resources.user_settings_appearance_title
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun AppearanceSettingsScreen(
    viewModel: AppearanceSettingsViewModel,
    navigator: Navigator,
) {
    val viewState by viewModel.viewState.collectAsStateWithLifecycle()
    val languageLauncher = rememberLanguageSettingsLauncher()

    NavigatorEffect(navigator, viewModel.navEvent)

    Content(
        viewState = viewState,
        isLanguageSupported = languageLauncher != null,
        onLanguageClicked = { languageLauncher?.invoke() },
        onEvent = viewModel::handleEvent,
    )

    AppearanceDialogs(
        dialog = viewState.dialog,
        currentThemeMode = viewState.themeMode,
        currentAppTheme = viewState.appTheme,
        currentPaletteStyle = viewState.paletteStyle,
        currentCustomSeedColor = viewState.customSeedColor,
        onEvent = viewModel::handleEvent,
    )
}

@Composable
private fun Content(
    viewState: AppearanceSettingsViewState,
    isLanguageSupported: Boolean = false,
    onLanguageClicked: () -> Unit = {},
    onEvent: (AppearanceSettingsEvent) -> Unit = {},
) {
    if (isWidthExpanded()) {
        ExpandedContent(
            viewState = viewState,
            isLanguageSupported = isLanguageSupported,
            onLanguageClicked = onLanguageClicked,
            onEvent = onEvent,
        )
    } else {
        CompactContent(
            viewState = viewState,
            isLanguageSupported = isLanguageSupported,
            onLanguageClicked = onLanguageClicked,
            onEvent = onEvent,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CompactContent(
    viewState: AppearanceSettingsViewState,
    isLanguageSupported: Boolean,
    onLanguageClicked: () -> Unit,
    onEvent: (AppearanceSettingsEvent) -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    GeeFlowScaffold(
        title = stringResource(Res.string.user_settings_appearance_title),
        subtitle = stringResource(Res.string.user_settings_appearance_description),
        navIconClick = { onEvent(BackClicked) },
        scrollBehavior = scrollBehavior,
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        content = { paddingValues ->
            AppearanceContent(
                viewState = viewState,
                isLanguageSupported = isLanguageSupported,
                onLanguageClicked = onLanguageClicked,
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
    viewState: AppearanceSettingsViewState,
    isLanguageSupported: Boolean,
    onLanguageClicked: () -> Unit,
    onEvent: (AppearanceSettingsEvent) -> Unit,
) {
    AppearanceContent(
        viewState = viewState,
        isLanguageSupported = isLanguageSupported,
        onLanguageClicked = onLanguageClicked,
        onEvent = onEvent,
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .geeFlowInsetsEndPadding()
            .padding(vertical = GeeFlowTheme.spacing.contentVertical),
    )
}

@Composable
private fun AppearanceContent(
    viewState: AppearanceSettingsViewState,
    isLanguageSupported: Boolean,
    onLanguageClicked: () -> Unit,
    onEvent: (AppearanceSettingsEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        GeeFlowNavigationListItem(
            title = stringResource(Res.string.user_settings_appearance_dark_mode),
            subtitle = stringResource(viewState.themeMode.titleRes),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onEvent(DarkModeClicked) },
        )
        GeeFlowNavigationListItem(
            title = stringResource(Res.string.user_settings_appearance_app_theme),
            subtitle = stringResource(viewState.appTheme.titleRes),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onEvent(AppThemeClicked) },
        )
        if (viewState.appTheme != AppTheme.SYSTEM) {
            GeeFlowNavigationListItem(
                title = stringResource(Res.string.user_settings_appearance_palette_style),
                subtitle = stringResource(viewState.paletteStyle.titleRes),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp)
                    .clickable { onEvent(PaletteStyleClicked) },
            )
        }
        if (viewState.appTheme == AppTheme.CUSTOM) {
            GeeFlowNavigationListItem(
                title = stringResource(Res.string.user_settings_appearance_custom_color),
                subtitle = stringResource(Res.string.user_settings_appearance_custom_color_description),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp)
                    .clickable { onEvent(CustomColorClicked) },
            )
        }
        if (isLanguageSupported) {
            GeeFlowNavigationListItem(
                title = stringResource(Res.string.user_settings_appearance_language),
                subtitle = stringResource(Res.string.user_settings_appearance_language_description),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onLanguageClicked),
            )
        }
        if (isFullScreenSupported) {
            GeeFlowToggleListItem(
                title = stringResource(Res.string.user_settings_appearance_full_screen),
                subtitle = stringResource(Res.string.user_settings_appearance_full_screen_description),
                checked = viewState.fullScreenMode,
                onCheckedChanged = { onEvent(FullScreenChanged(it)) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (isKeepScreenOnSupported) {
            GeeFlowToggleListItem(
                title = stringResource(Res.string.user_settings_appearance_keep_screen_on),
                subtitle = stringResource(Res.string.user_settings_appearance_keep_screen_on_description),
                checked = viewState.keepScreenOn,
                onCheckedChanged = { onEvent(KeepScreenOnChanged(it)) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@PreviewWrapper(GeeFlowPreviewWrapper::class)
@Composable
@GeeFlowScreenPreview
private fun Preview() {
    Content(
        viewState = AppearanceSettingsViewState(),
        isLanguageSupported = true,
    )
}
