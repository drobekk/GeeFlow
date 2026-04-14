package dev.drobek.geeflow.presentation.feature.user.settings.appearance

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.drobek.geeflow.data.user.model.AppTheme
import dev.drobek.geeflow.navigation.Navigator
import dev.drobek.geeflow.navigation.NavigatorEffect
import dev.drobek.geeflow.platform.isFullScreenSupported
import dev.drobek.geeflow.platform.isKeepScreenOnSupported
import dev.drobek.geeflow.platform.rememberLanguageSettingsLauncher
import dev.drobek.geeflow.presentation.feature.user.settings.appearance.AppearanceSettingsEvent.AppThemeClicked
import dev.drobek.geeflow.presentation.feature.user.settings.appearance.AppearanceSettingsEvent.BackClicked
import dev.drobek.geeflow.presentation.feature.user.settings.appearance.AppearanceSettingsEvent.DarkModeClicked
import dev.drobek.geeflow.presentation.feature.user.settings.appearance.AppearanceSettingsEvent.FullScreenChanged
import dev.drobek.geeflow.presentation.feature.user.settings.appearance.AppearanceSettingsEvent.KeepScreenOnChanged
import dev.drobek.geeflow.presentation.feature.user.settings.appearance.components.AppearanceDialogs
import dev.drobek.geeflow.ui.components.GeeFlowListItem
import dev.drobek.geeflow.ui.components.GeeFlowScaffold
import dev.drobek.geeflow.ui.components.GeeFlowToggleListItem
import dev.drobek.geeflow.ui.isWidthExpanded
import dev.drobek.geeflow.ui.theme.GeeFlowScreenPreview
import dev.drobek.geeflow.ui.theme.GeeFlowTheme
import geeflow.feature.user.settings.generated.resources.Res
import geeflow.feature.user.settings.generated.resources.user_settings_appearance_app_theme
import geeflow.feature.user.settings.generated.resources.user_settings_appearance_dark_mode
import geeflow.feature.user.settings.generated.resources.user_settings_appearance_full_screen
import geeflow.feature.user.settings.generated.resources.user_settings_appearance_full_screen_description
import geeflow.feature.user.settings.generated.resources.user_settings_appearance_keep_screen_on
import geeflow.feature.user.settings.generated.resources.user_settings_appearance_keep_screen_on_description
import geeflow.feature.user.settings.generated.resources.user_settings_appearance_language
import geeflow.feature.user.settings.generated.resources.user_settings_appearance_language_description
import geeflow.feature.user.settings.generated.resources.user_settings_appearance_title
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
        currentDarkMode = viewState.darkMode,
        currentAppTheme = viewState.appTheme,
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
        subtitle = null,
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
            .systemBarsPadding()
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
        GeeFlowListItem(
            title = stringResource(Res.string.user_settings_appearance_dark_mode),
            subtitle = stringResource(viewState.darkMode.titleRes),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onEvent(DarkModeClicked) }
                .padding(vertical = 16.dp, horizontal = GeeFlowTheme.spacing.contentHorizontal),
        )
        HorizontalDivider(modifier = Modifier.padding(horizontal = GeeFlowTheme.spacing.contentHorizontal))
        GeeFlowListItem(
            title = stringResource(Res.string.user_settings_appearance_app_theme),
            subtitle = stringResource(viewState.appTheme.titleRes),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onEvent(AppThemeClicked) }
                .padding(vertical = 16.dp, horizontal = GeeFlowTheme.spacing.contentHorizontal),
        )
        if (isLanguageSupported) {
            HorizontalDivider(modifier = Modifier.padding(horizontal = GeeFlowTheme.spacing.contentHorizontal))
            GeeFlowListItem(
                title = stringResource(Res.string.user_settings_appearance_language),
                subtitle = stringResource(Res.string.user_settings_appearance_language_description),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onLanguageClicked)
                    .padding(vertical = 16.dp, horizontal = GeeFlowTheme.spacing.contentHorizontal),
            )
        }
        if (isFullScreenSupported) {
            HorizontalDivider(modifier = Modifier.padding(horizontal = GeeFlowTheme.spacing.contentHorizontal))
            GeeFlowToggleListItem(
                title = stringResource(Res.string.user_settings_appearance_full_screen),
                subtitle = stringResource(Res.string.user_settings_appearance_full_screen_description),
                checked = viewState.fullScreenMode,
                onCheckedChanged = { onEvent(FullScreenChanged(it)) },
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp, horizontal = GeeFlowTheme.spacing.contentHorizontal),
            )
        }
        if (isKeepScreenOnSupported) {
            HorizontalDivider(modifier = Modifier.padding(horizontal = GeeFlowTheme.spacing.contentHorizontal))
            GeeFlowToggleListItem(
                title = stringResource(Res.string.user_settings_appearance_keep_screen_on),
                subtitle = stringResource(Res.string.user_settings_appearance_keep_screen_on_description),
                checked = viewState.keepScreenOn,
                onCheckedChanged = { onEvent(KeepScreenOnChanged(it)) },
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp, horizontal = GeeFlowTheme.spacing.contentHorizontal),
            )
        }
    }
}

@Composable
@GeeFlowScreenPreview
private fun PreviewLight() = GeeFlowTheme(false) {
    Content(
        viewState = AppearanceSettingsViewState(),
        isLanguageSupported = true,
    )
}

@Composable
@GeeFlowScreenPreview
private fun PreviewDark() = GeeFlowTheme(true) {
    Content(
        viewState = AppearanceSettingsViewState(appTheme = AppTheme.SYSTEM),
        isLanguageSupported = false,
    )
}
