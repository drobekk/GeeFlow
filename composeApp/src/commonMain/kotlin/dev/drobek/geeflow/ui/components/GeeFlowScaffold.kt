package dev.drobek.geeflow.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.FabPosition
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp
import dev.drobek.geeflow.ui.conditional
import dev.drobek.geeflow.ui.isExpanded
import geeflow.composeapp.generated.resources.Res
import geeflow.composeapp.generated.resources.common_go_back
import org.jetbrains.compose.resources.stringResource

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
    modifier: Modifier = Modifier,
    contentWindowInsets: WindowInsets = WindowInsets.systemBars,
    content: @Composable (paddingValues: PaddingValues) -> Unit
) {
    val topBar: @Composable () -> Unit = {
        GeeFlowTopBar(
            title = title,
            subtitle = subtitle,
            navIconPainter = navIconPainter,
            navIconContentDescription = navIconContentDescription,
            navIconClick = navIconClick,
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .conditional(
                    condition = isExpanded(),
                    ifTrue = { fillMaxHeight().widthIn(max = 300.dp) },
                    ifFalse = { fillMaxWidth() }
                )
        )
    }
    if (isExpanded()) {
        GeeFlowScaffoldExpanded(
            topBar = topBar,
            snackbarHost = snackbarHost,
            floatingActionButton = floatingActionButton,
            floatingActionButtonPosition = floatingActionButtonPosition,
            containerColor = containerColor,
            contentColor = contentColor,
            modifier = modifier,
            contentWindowInsets = contentWindowInsets,
            content = content
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
            content = content
        )
    }
}

@Composable
fun GeeFlowScaffold(
    topBar: @Composable () -> Unit,
    snackbarHost: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    floatingActionButtonPosition: FabPosition = FabPosition.End,
    containerColor: Color = MaterialTheme.colorScheme.background,
    contentColor: Color = contentColorFor(containerColor),
    modifier: Modifier = Modifier,
    contentWindowInsets: WindowInsets = WindowInsets.systemBars,
    content: @Composable (paddingValues: PaddingValues) -> Unit
) {
    if (isExpanded()) {
        GeeFlowScaffoldExpanded(
            topBar = topBar,
            snackbarHost = snackbarHost,
            floatingActionButton = floatingActionButton,
            floatingActionButtonPosition = floatingActionButtonPosition,
            containerColor = containerColor,
            contentColor = contentColor,
            modifier = modifier,
            contentWindowInsets = contentWindowInsets,
            content = content
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
            content = content
        )
    }
}

@Composable
fun GeeFlowScaffoldExpanded(
    topBar: @Composable () -> Unit,
    snackbarHost: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    floatingActionButtonPosition: FabPosition = FabPosition.End,
    containerColor: Color = MaterialTheme.colorScheme.background,
    contentColor: Color = contentColorFor(containerColor),
    modifier: Modifier = Modifier,
    contentWindowInsets: WindowInsets = WindowInsets.systemBars,
    content: @Composable (paddingValues: PaddingValues) -> Unit
) = Scaffold(
    modifier = modifier,
    snackbarHost = snackbarHost,
    contentWindowInsets = contentWindowInsets,
    containerColor = containerColor,
    contentColor = contentColor,
    content = { paddingValues ->
        Row {
            topBar()
            WaveDivider(
                color = MaterialTheme.colorScheme.surfaceContainer,
                orientation = WaveOrientation.Vertical
            )
            Box {
                content(paddingValues)
                Box(
                    Modifier
                        .align(
                            when (floatingActionButtonPosition) {
                                FabPosition.Start -> Alignment.BottomStart
                                FabPosition.End -> Alignment.BottomEnd
                                FabPosition.Center -> Alignment.BottomCenter
                                else -> Alignment.BottomEnd
                            }
                        )
                        .padding(16.dp)
                        .padding(bottom = paddingValues.calculateBottomPadding()),
                    content = { floatingActionButton() }
                )
            }
        }
    }
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
    content: @Composable (paddingValues: PaddingValues) -> Unit
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
                orientation = WaveOrientation.Horizontal
            )
        }
    },
    content = content
)
