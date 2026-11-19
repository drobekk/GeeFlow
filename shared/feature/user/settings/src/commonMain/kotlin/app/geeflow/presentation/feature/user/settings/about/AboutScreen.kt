package app.geeflow.presentation.feature.user.settings.about

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewWrapper
import app.geeflow.BuildKonfig
import app.geeflow.navigation.Navigator
import app.geeflow.navigation.NavigatorEffect
import app.geeflow.presentation.feature.user.settings.about.AboutEvent.BackClicked
import app.geeflow.presentation.feature.user.settings.about.AboutEvent.LicensesClicked
import app.geeflow.ui.components.GeeFlowDetailScaffold
import app.geeflow.ui.components.GeeFlowListItem
import app.geeflow.ui.components.GeeFlowNavigationListItem
import app.geeflow.ui.theme.GeeFlowPreviewWrapper
import app.geeflow.ui.theme.GeeFlowScreenPreview
import app.geeflow.ui.theme.GeeFlowTheme
import geeflow.shared.feature.user.settings.generated.resources.Res
import geeflow.shared.feature.user.settings.generated.resources.user_settings_about_description
import geeflow.shared.feature.user.settings.generated.resources.user_settings_about_licenses
import geeflow.shared.feature.user.settings.generated.resources.user_settings_about_licenses_description
import geeflow.shared.feature.user.settings.generated.resources.user_settings_about_title
import geeflow.shared.feature.user.settings.generated.resources.user_settings_about_version
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun AboutScreen(
    viewModel: AboutViewModel,
    navigator: Navigator,
) {
    NavigatorEffect(navigator, viewModel.navEvent)
    Content(onEvent = viewModel::handleEvent)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Content(onEvent: (AboutEvent) -> Unit = {}) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    GeeFlowDetailScaffold(
        title = stringResource(Res.string.user_settings_about_title),
        plain = true,
        navIconClick = { onEvent(BackClicked) },
        scrollBehavior = scrollBehavior,
        modifier = Modifier,
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
private fun AboutContent(
    onEvent: (AboutEvent) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        GeeFlowNavigationListItem(
            title = stringResource(Res.string.user_settings_about_licenses),
            subtitle = stringResource(Res.string.user_settings_about_licenses_description),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onEvent(LicensesClicked) },
        )
        GeeFlowListItem(
            title = stringResource(Res.string.user_settings_about_version),
            subtitle = "${BuildKonfig.APP_VERSION} (${BuildKonfig.APP_VERSION_CODE})",
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@PreviewWrapper(GeeFlowPreviewWrapper::class)
@Composable
@GeeFlowScreenPreview
private fun Preview() {
    Content()
}
