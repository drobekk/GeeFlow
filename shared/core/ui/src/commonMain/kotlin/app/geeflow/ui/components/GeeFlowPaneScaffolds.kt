@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3AdaptiveApi::class)

package app.geeflow.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.LocalListDetailSceneScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import app.geeflow.ui.GeeFlowInsets
import app.geeflow.ui.modifier.WaveOrientation

/** RootNavigation uses the default strategy which only takes over for multi-pane scenes. */
@Composable
fun isListDetailExpanded(): Boolean = LocalListDetailSceneScope.current != null

/** The header stays above the list even when navigation displays a detail beside it. */
@Composable
fun GeeFlowListScaffold(
    title: String,
    subtitle: String? = null,
    navIconClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    expanded: Boolean = isListDetailExpanded(),
    scrollBehavior: TopAppBarScrollBehavior? = null,
    snackbarHost: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    val scrollModifier = if (scrollBehavior != null) {
        Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
    } else {
        Modifier
    }
    Row(modifier.then(scrollModifier).fillMaxSize()) {
        GeeFlowScaffoldFrame(
            modifier = Modifier.weight(1f),
            contentWindowInsets = if (expanded) GeeFlowInsets.startPane else GeeFlowInsets.content,
            containerColor = if (expanded) MaterialTheme.colorScheme.surfaceContainer else MaterialTheme.colorScheme.background,
            horizontalWave = !expanded,
            header = {
                GeeFlowTopBar(
                    title = title,
                    subtitle = subtitle,
                    navIconClick = navIconClick,
                    navIconPainter = rememberVectorPainter(Icons.AutoMirrored.Filled.ArrowBack),
                    scrollBehavior = scrollBehavior,
                    windowInsets = WindowInsets(0, 0, 0, 0),
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            snackbarHost = snackbarHost,
            floatingActionButton = floatingActionButton,
            content = content,
        )
        if (expanded) {
            WaveDivider(MaterialTheme.colorScheme.surfaceContainer, orientation = WaveOrientation.Vertical)
        }
    }
}

/** Standalone detail has a back header; an embedded detail owns only its outer edges. */
@Composable
fun GeeFlowDetailScaffold(
    title: String,
    subtitle: String? = null,
    navIconClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    expanded: Boolean = isListDetailExpanded(),
    scrollBehavior: TopAppBarScrollBehavior? = null,
    snackbarHost: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    GeeFlowScaffoldFrame(
        modifier = if (!expanded && scrollBehavior != null) {
            modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
        } else {
            modifier
        },
        contentWindowInsets = if (expanded) GeeFlowInsets.endPane else GeeFlowInsets.content,
        header = if (expanded) {
            null
        } else {
            {
                GeeFlowTopBar(
                    title = title,
                    subtitle = subtitle,
                    navIconClick = navIconClick,
                    scrollBehavior = scrollBehavior,
                    windowInsets = WindowInsets(0, 0, 0, 0),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        snackbarHost = snackbarHost,
        floatingActionButton = floatingActionButton,
        bottomBar = bottomBar,
        content = content,
    )
}
