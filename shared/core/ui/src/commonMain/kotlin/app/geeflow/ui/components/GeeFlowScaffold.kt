@file:OptIn(ExperimentalMaterial3Api::class)

package app.geeflow.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import app.geeflow.ui.GeeFlowInsets
import app.geeflow.ui.isWidthExpanded
import app.geeflow.ui.modifier.WaveOrientation
import geeflow.shared.core.ui.generated.resources.Res
import geeflow.shared.core.ui.generated.resources.common_go_back
import org.jetbrains.compose.resources.stringResource

/** Standalone page: the header moves beside the content when the window width is expanded. */
@Composable
fun GeeFlowScaffold(
    title: String,
    subtitle: String? = null,
    snackbarHost: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    floatingActionButtonPosition: FabPosition = FabPosition.End,
    navIconPainter: Painter? = rememberVectorPainter(Icons.AutoMirrored.Filled.ArrowBack),
    navIconClick: () -> Unit = {},
    navIconContentDescription: String? = stringResource(Res.string.common_go_back),
    containerColor: Color = MaterialTheme.colorScheme.background,
    contentColor: Color = contentColorFor(containerColor),
    scrollBehavior: TopAppBarScrollBehavior? = null,
    modifier: Modifier = Modifier,
    contentWindowInsets: WindowInsets = GeeFlowInsets.content,
    bottomBar: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    val sideHeader = isWidthExpanded()
    GeeFlowScaffoldFrame(
        modifier = if (!sideHeader && scrollBehavior != null) {
            modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
        } else {
            modifier
        },
        sideHeader = sideHeader,
        header = {
            GeeFlowTopBar(
                title = title,
                subtitle = subtitle,
                navIconPainter = navIconPainter,
                navIconClick = navIconClick,
                navIconContentDescription = navIconContentDescription,
                scrollBehavior = if (sideHeader) null else scrollBehavior,
                windowInsets = WindowInsets(0, 0, 0, 0),
                modifier = Modifier.fillMaxWidth(),
            )
        },
        snackbarHost = snackbarHost,
        floatingActionButton = floatingActionButton,
        floatingActionButtonPosition = floatingActionButtonPosition,
        containerColor = containerColor,
        contentColor = contentColor,
        contentWindowInsets = contentWindowInsets,
        bottomBar = bottomBar,
        content = content,
    )
}

/** Custom header, e.g. the welcome logo. Header insets are owned by the scaffold. */
@Composable
fun GeeFlowScaffold(
    topBar: @Composable (sideHeader: Boolean) -> Unit,
    snackbarHost: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    floatingActionButtonPosition: FabPosition = FabPosition.End,
    containerColor: Color = MaterialTheme.colorScheme.background,
    contentColor: Color = contentColorFor(containerColor),
    modifier: Modifier = Modifier,
    contentWindowInsets: WindowInsets = GeeFlowInsets.content,
    bottomBar: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    val sideHeader = isWidthExpanded()
    GeeFlowScaffoldFrame(
        modifier = modifier,
        sideHeader = sideHeader,
        header = { topBar(sideHeader) },
        snackbarHost = snackbarHost,
        floatingActionButton = floatingActionButton,
        floatingActionButtonPosition = floatingActionButtonPosition,
        containerColor = containerColor,
        contentColor = contentColor,
        contentWindowInsets = contentWindowInsets,
        bottomBar = bottomBar,
        content = content,
    )
}

/** Each frame owns its bars/overlays. Content applies the supplied padding exactly once. */
@Composable
internal fun GeeFlowScaffoldFrame(
    header: (@Composable () -> Unit)?,
    modifier: Modifier = Modifier,
    sideHeader: Boolean = false,
    horizontalWave: Boolean = true,
    contentWindowInsets: WindowInsets = GeeFlowInsets.content,
    containerColor: Color = MaterialTheme.colorScheme.background,
    contentColor: Color = contentColorFor(containerColor),
    snackbarHost: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    floatingActionButtonPosition: FabPosition = FabPosition.End,
    bottomBar: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    val currentContent by rememberUpdatedState(content)
    val stableContent = remember {
        movableContentOf<PaddingValues> { padding ->
            Box(Modifier.fillMaxSize().consumeWindowInsets(padding)) { currentContent(padding) }
        }
    }
    Row(modifier.fillMaxSize()) {
        if (sideHeader && header != null) {
            Box(
                Modifier.fillMaxHeight()
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .windowInsetsPadding(contentWindowInsets.only(WindowInsetsSides.Vertical + WindowInsetsSides.Start))
                    .width(SideHeaderWidth),
            ) { header() }
            WaveDivider(MaterialTheme.colorScheme.surfaceContainer, orientation = WaveOrientation.Vertical)
        }
        Scaffold(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            containerColor = containerColor,
            contentColor = contentColor,
            contentWindowInsets = if (sideHeader) {
                contentWindowInsets.only(WindowInsetsSides.Vertical + WindowInsetsSides.End)
            } else {
                contentWindowInsets
            },
            topBar = {
                if (!sideHeader && header != null) {
                    Column {
                        Box(
                            Modifier
                                .background(MaterialTheme.colorScheme.surfaceContainer)
                                .windowInsetsPadding(
                                    contentWindowInsets.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
                                ),
                        ) { header() }
                        if (horizontalWave) {
                            WaveDivider(
                                MaterialTheme.colorScheme.surfaceContainer,
                                orientation = WaveOrientation.Horizontal,
                                reversed = true,
                            )
                        }
                    }
                }
            },
            snackbarHost = snackbarHost,
            floatingActionButton = floatingActionButton,
            floatingActionButtonPosition = floatingActionButtonPosition,
            bottomBar = bottomBar,
            content = { stableContent(it) },
        )
    }
}

private val SideHeaderWidth = 300.dp
