package dev.drobek.geeflow.presentation.feature.user.settings.about

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.tooling.preview.PreviewWrapper
import dev.drobek.geeflow.BuildKonfig
import dev.drobek.geeflow.navigation.Navigator
import dev.drobek.geeflow.navigation.NavigatorEffect
import dev.drobek.geeflow.presentation.feature.user.settings.about.AboutEvent.BackClicked
import dev.drobek.geeflow.presentation.feature.user.settings.about.AboutEvent.LicensesClicked
import dev.drobek.geeflow.presentation.feature.user.settings.about.AboutEvent.PrivacyPolicyClicked
import dev.drobek.geeflow.presentation.feature.user.settings.about.AboutEvent.SourceCodeClicked
import dev.drobek.geeflow.ui.components.GeeFlowListItem
import dev.drobek.geeflow.ui.components.GeeFlowNavigationListItem
import dev.drobek.geeflow.ui.components.GeeFlowScaffold
import dev.drobek.geeflow.ui.isWidthExpanded
import dev.drobek.geeflow.ui.theme.GeeFlowPreviewWrapper
import dev.drobek.geeflow.ui.theme.GeeFlowScreenPreview
import dev.drobek.geeflow.ui.theme.GeeFlowTheme
import geeflow.feature.user.settings.generated.resources.Res
import geeflow.feature.user.settings.generated.resources.user_settings_about_description
import geeflow.feature.user.settings.generated.resources.user_settings_about_licenses
import geeflow.feature.user.settings.generated.resources.user_settings_about_licenses_description
import geeflow.feature.user.settings.generated.resources.user_settings_about_privacy_policy
import geeflow.feature.user.settings.generated.resources.user_settings_about_privacy_policy_description
import geeflow.feature.user.settings.generated.resources.user_settings_about_source_code
import geeflow.feature.user.settings.generated.resources.user_settings_about_source_code_description
import geeflow.feature.user.settings.generated.resources.user_settings_about_title
import geeflow.feature.user.settings.generated.resources.user_settings_about_version
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun AboutScreen(
    viewModel: AboutViewModel,
    navigator: Navigator,
) {
    val uriHandler = LocalUriHandler.current

    NavigatorEffect(navigator, viewModel.navEvent)

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is AboutViewModelEvent.OpenUrl -> uriHandler.openUri(event.url)
            }
        }
    }

    Content(onEvent = viewModel::handleEvent)
}

@Composable
private fun Content(
    onEvent: (AboutEvent) -> Unit = {},
) {
    if (isWidthExpanded()) {
        ExpandedContent(onEvent = onEvent)
    } else {
        CompactContent(onEvent = onEvent)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CompactContent(
    onEvent: (AboutEvent) -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    GeeFlowScaffold(
        title = stringResource(Res.string.user_settings_about_title),
        subtitle = stringResource(Res.string.user_settings_about_description),
        navIconClick = { onEvent(BackClicked) },
        scrollBehavior = scrollBehavior,
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        content = { paddingValues ->
            AboutContent(
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
    onEvent: (AboutEvent) -> Unit,
) {
    AboutContent(
        onEvent = onEvent,
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .systemBarsPadding()
            .padding(vertical = GeeFlowTheme.spacing.contentVertical),
    )
}

@Composable
private fun AboutContent(
    onEvent: (AboutEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        GeeFlowNavigationListItem(
            title = stringResource(Res.string.user_settings_about_privacy_policy),
            subtitle = stringResource(Res.string.user_settings_about_privacy_policy_description),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onEvent(PrivacyPolicyClicked) },
        )
        GeeFlowNavigationListItem(
            title = stringResource(Res.string.user_settings_about_source_code),
            subtitle = stringResource(Res.string.user_settings_about_source_code_description),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onEvent(SourceCodeClicked) },
        )
        GeeFlowNavigationListItem(
            title = stringResource(Res.string.user_settings_about_licenses),
            subtitle = stringResource(Res.string.user_settings_about_licenses_description),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onEvent(LicensesClicked) },
        )
        GeeFlowListItem(
            title = stringResource(Res.string.user_settings_about_version),
            subtitle = BuildKonfig.APP_VERSION,
            modifier = Modifier
                .fillMaxWidth(),
        )
    }
}

@PreviewWrapper(GeeFlowPreviewWrapper::class)
@Composable
@GeeFlowScreenPreview
private fun Preview() {
    Content()
}
