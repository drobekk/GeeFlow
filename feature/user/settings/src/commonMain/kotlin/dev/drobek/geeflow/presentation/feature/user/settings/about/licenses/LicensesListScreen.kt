package dev.drobek.geeflow.presentation.feature.user.settings.about.licenses

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.mikepenz.aboutlibraries.ui.compose.m3.LibrariesContainer
import com.mikepenz.aboutlibraries.ui.compose.produceLibraries
import dev.drobek.geeflow.navigation.NavEvent
import dev.drobek.geeflow.navigation.Navigator
import dev.drobek.geeflow.ui.components.GeeFlowScaffold
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
    GeeFlowScaffold(
        title = stringResource(Res.string.user_settings_about_licenses),
        subtitle = null,
        navIconClick = { navigator.navigate(NavEvent.Back) },
        scrollBehavior = scrollBehavior,
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        content = { paddingValues ->
            LibrariesContainer(
                libraries = libraries,
                contentPadding = paddingValues,
                modifier = Modifier.fillMaxSize(),
            )
        },
    )
}
