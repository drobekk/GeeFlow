package app.geeflow.presentation.feature.user.settings.about.licenses

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.plus
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import app.geeflow.navigation.NavEvent
import app.geeflow.navigation.Navigator
import app.geeflow.ui.components.GeeFlowScaffold
import app.geeflow.ui.isWidthExpanded
import app.geeflow.ui.theme.GeeFlowTheme
import com.mikepenz.aboutlibraries.ui.compose.m3.LibrariesContainer
import com.mikepenz.aboutlibraries.ui.compose.produceLibraries
import geeflow.feature.user.settings.generated.resources.Res
import geeflow.feature.user.settings.generated.resources.user_settings_about_licenses
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LicensesListScreen(
    navigator: Navigator,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val libraries by produceLibraries {
        Res.readBytes("files/aboutlibraries.json").decodeToString()
    }
    val modifier = if (!isWidthExpanded()) Modifier.nestedScroll(scrollBehavior.nestedScrollConnection) else Modifier
    GeeFlowScaffold(
        title = stringResource(Res.string.user_settings_about_licenses),
        subtitle = null,
        navIconClick = { navigator.navigate(NavEvent.Back) },
        scrollBehavior = scrollBehavior,
        modifier = modifier,
        content = { paddingValues ->
            LibrariesContainer(
                libraries = libraries,
                contentPadding = paddingValues + PaddingValues(vertical = GeeFlowTheme.spacing.contentVertical),
                modifier = Modifier
                    .fillMaxSize(),
            )
        },
    )
}
