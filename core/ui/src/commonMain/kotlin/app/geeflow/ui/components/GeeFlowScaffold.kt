package app.geeflow.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.minus
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import app.geeflow.ui.isWidthExpanded
import app.geeflow.ui.modifier.WaveOrientation
import app.geeflow.ui.modifier.geeFlowInsets
import geeflow.core.ui.generated.resources.Res
import geeflow.core.ui.generated.resources.common_go_back
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeeFlowScaffold(
    title: String,
    subtitle: String?,
    snackbarHost: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    floatingActionButtonPosition: FabPosition = FabPosition.End,
    navIconPainter: Painter? = rememberVectorPainter(image = Icons.AutoMirrored.Filled.ArrowBack),
    navIconClick: () -> Unit = {},
    navIconContentDescription: String? = stringResource(Res.string.common_go_back),
    containerColor: Color = MaterialTheme.colorScheme.background,
    contentColor: Color = contentColorFor(containerColor),
    scrollBehavior: TopAppBarScrollBehavior? = null,
    modifier: Modifier = Modifier,
    contentWindowInsets: WindowInsets = WindowInsets.geeFlowInsets,
    content: @Composable (paddingValues: PaddingValues) -> Unit,
) {
    val topBarExpanded: @Composable () -> Unit = {
        GeeFlowTopBar(
            title = title,
            subtitle = subtitle,
            navIconPainter = navIconPainter,
            scrollBehavior = null,
            navIconContentDescription = navIconContentDescription,
            navIconClick = navIconClick,
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .widthIn(max = 300.dp),
        )
    }

    val topBarCompact: @Composable () -> Unit = {
        GeeFlowTopBar(
            title = title,
            subtitle = subtitle,
            navIconPainter = navIconPainter,
            scrollBehavior = scrollBehavior,
            navIconContentDescription = navIconContentDescription,
            navIconClick = navIconClick,
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .fillMaxWidth(),
        )
    }

    if (isWidthExpanded()) {
        GeeFlowScaffoldExpanded(
            topBar = topBarExpanded,
            snackbarHost = snackbarHost,
            floatingActionButton = floatingActionButton,
            floatingActionButtonPosition = floatingActionButtonPosition,
            containerColor = containerColor,
            contentColor = contentColor,
            modifier = modifier,
            contentWindowInsets = contentWindowInsets,
            content = content,
        )
    } else {
        GeeFlowScaffoldCompact(
            topBar = topBarCompact,
            snackbarHost = snackbarHost,
            floatingActionButton = floatingActionButton,
            floatingActionButtonPosition = floatingActionButtonPosition,
            containerColor = containerColor,
            contentColor = contentColor,
            modifier = modifier,
            contentWindowInsets = contentWindowInsets,
            content = content,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeeFlowScaffold(
    topBar: @Composable () -> Unit,
    snackbarHost: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    floatingActionButtonPosition: FabPosition = FabPosition.End,
    containerColor: Color = MaterialTheme.colorScheme.background,
    contentColor: Color = contentColorFor(containerColor),
    modifier: Modifier = Modifier,
    contentWindowInsets: WindowInsets = WindowInsets.geeFlowInsets,
    content: @Composable (paddingValues: PaddingValues) -> Unit,
) {
    if (isWidthExpanded()) {
        GeeFlowScaffoldExpanded(
            topBar = topBar,
            snackbarHost = snackbarHost,
            floatingActionButton = floatingActionButton,
            floatingActionButtonPosition = floatingActionButtonPosition,
            containerColor = containerColor,
            contentColor = contentColor,
            modifier = modifier,
            contentWindowInsets = contentWindowInsets,
            content = content,
        )
    } else {
        GeeFlowScaffoldCompact(
            topBar = topBar,
            snackbarHost = snackbarHost,
            floatingActionButton = floatingActionButton,
            floatingActionButtonPosition = floatingActionButtonPosition,
            containerColor = containerColor,
            contentColor = contentColor,
            modifier = modifier,
            contentWindowInsets = contentWindowInsets,
            content = content,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeeFlowScaffoldExpanded(
    topBar: @Composable () -> Unit,
    snackbarHost: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    floatingActionButtonPosition: FabPosition = FabPosition.End,
    containerColor: Color = MaterialTheme.colorScheme.background,
    contentColor: Color = contentColorFor(containerColor),
    modifier: Modifier = Modifier,
    contentWindowInsets: WindowInsets = WindowInsets.geeFlowInsets,
    content: @Composable (paddingValues: PaddingValues) -> Unit,
) = Scaffold(
    modifier = modifier,
    snackbarHost = snackbarHost,
    contentWindowInsets = contentWindowInsets,
    containerColor = containerColor,
    contentColor = contentColor,
    content = { paddingValues ->
        Row {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .background(color = MaterialTheme.colorScheme.surfaceContainer)
                    .padding(start = contentWindowInsets.asPaddingValues().calculateStartPadding(LayoutDirection.Ltr)),
            ) {
                topBar()
            }
            WaveDivider(
                color = MaterialTheme.colorScheme.surfaceContainer,
                orientation = WaveOrientation.Vertical,
            )
            Box {
                val startPadding = contentWindowInsets.asPaddingValues().calculateStartPadding(LayoutDirection.Ltr)
                content(paddingValues - PaddingValues(start = startPadding))
                Box(
                    Modifier
                        .align(
                            when (floatingActionButtonPosition) {
                                FabPosition.Start -> Alignment.BottomStart
                                FabPosition.End -> Alignment.BottomEnd
                                FabPosition.Center -> Alignment.BottomCenter
                                else -> Alignment.BottomEnd
                            },
                        )
                        .padding(16.dp)
                        .padding(bottom = paddingValues.calculateBottomPadding()),
                    content = { floatingActionButton() },
                )
            }
        }
    },
)

@Composable
fun GeeFlowScaffoldCompact(
    topBar: @Composable () -> Unit,
    snackbarHost: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    floatingActionButtonPosition: FabPosition = FabPosition.End,
    containerColor: Color = MaterialTheme.colorScheme.background,
    contentColor: Color = contentColorFor(containerColor),
    modifier: Modifier = Modifier,
    contentWindowInsets: WindowInsets = WindowInsets.navigationBars,
    content: @Composable (paddingValues: PaddingValues) -> Unit,
) = Scaffold(
    modifier = modifier,
    snackbarHost = snackbarHost,
    floatingActionButton = floatingActionButton,
    floatingActionButtonPosition = floatingActionButtonPosition,
    contentWindowInsets = contentWindowInsets,
    containerColor = containerColor,
    contentColor = contentColor,
    topBar = {
        Column {
            topBar()
            WaveDivider(
                color = MaterialTheme.colorScheme.surfaceContainer,
                orientation = WaveOrientation.Horizontal,
                reversed = true,
            )
        }
    },
    content = content,
)
