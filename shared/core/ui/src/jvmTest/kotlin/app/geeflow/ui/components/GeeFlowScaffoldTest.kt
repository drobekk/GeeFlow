@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class)

package app.geeflow.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertLeftPositionInRootIsEqualTo
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertTrue

class GeeFlowScaffoldTest {
    @Test
    fun expandedDetailScrollsWithoutHiddenHeaderConsumingGestures() = runComposeUiTest {
        var scrollPosition = 0
        setContent {
            val scrollState = rememberScrollState()
            scrollPosition = scrollState.value
            MaterialTheme {
                GeeFlowDetailScaffold(
                    title = "Detail",
                    expanded = true,
                    scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(),
                ) { padding ->
                    Column(Modifier.fillMaxSize().verticalScroll(scrollState).padding(padding).testTag("scroll")) {
                        Box(Modifier.height(2000.dp))
                    }
                }
            }
        }
        onNodeWithTag("scroll").performTouchInput { swipeUp() }
        runOnIdle { assertTrue(scrollPosition > 0) }
    }

    @Test
    fun horizontalWaveHasVisibleBackground() = runComposeUiTest {
        setContent {
            MaterialTheme(colorScheme = lightColorScheme(background = Color.White, surfaceContainer = Color.Black)) {
                GeeFlowScaffoldFrame(
                    header = { Box(Modifier.fillMaxWidth().height(80.dp).testTag("header")) },
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                ) { }
            }
        }
        val header = onNodeWithTag("header").fetchSemanticsNode().boundsInRoot
        val pixels = onRoot().captureToImage().toPixelMap()
        val waveStart = header.bottom.toInt()
        val waveEnd = (header.bottom + header.height / 4).toInt().coerceAtMost(pixels.height)
        val hasWave = (waveStart until waveEnd).any { y ->
            val row = (0 until pixels.width).map { x -> pixels[x, y] }
            row.any { it.red < 0.1f } && row.any { it.red > 0.9f }
        }
        assertTrue(hasWave, "The wave must reveal the page background rather than a solid header-colored strip")
    }

    @Test
    fun detailHeaderFollowsPanePresentation() = runComposeUiTest {
        var expanded by mutableStateOf(false)
        setContent {
            MaterialTheme {
                GeeFlowDetailScaffold(title = "Detail title", expanded = expanded) { padding ->
                    Text("Detail content", Modifier.padding(padding))
                }
            }
        }
        onNodeWithText("Detail title").assertExists()
        runOnIdle { expanded = true }
        onNodeWithText("Detail title").assertDoesNotExist()
        onNodeWithText("Detail content").assertExists()
    }

    @Test
    fun listKeepsHeaderInBothPresentations() = runComposeUiTest {
        var expanded by mutableStateOf(false)
        setContent {
            MaterialTheme {
                GeeFlowListScaffold(title = "Settings", expanded = expanded) { padding ->
                    Text("Category", Modifier.padding(padding))
                }
            }
        }
        onNodeWithText("Settings").assertExists()
        runOnIdle { expanded = true }
        onNodeWithText("Settings").assertExists()
        onNodeWithText("Category").assertExists()
    }

    @Test
    fun contentDoesNotApplyCutoutTwice() = runComposeUiTest {
        val insets = WindowInsets(left = 40.dp)
        setContent {
            MaterialTheme {
                GeeFlowScaffoldFrame(header = null, contentWindowInsets = insets) { padding ->
                    Box(Modifier.fillMaxSize().padding(padding).windowInsetsPadding(insets)) {
                        Text("Content", Modifier.testTag("content"))
                    }
                }
            }
        }
        onNodeWithTag("content").assertLeftPositionInRootIsEqualTo(40.dp)
    }

    @Test
    fun movingHeaderPreservesRememberedContent() = runComposeUiTest {
        var wide by mutableStateOf(false)
        setContent {
            MaterialTheme {
                Box {
                    GeeFlowScaffoldFrame(
                        sideHeader = wide,
                        header = { Text(if (wide) "Side header" else "Top header") },
                    ) { padding ->
                        var count by remember { mutableStateOf(0) }
                        TextButton(onClick = { count++ }, modifier = Modifier.padding(padding)) {
                            Text("Count $count")
                        }
                    }
                }
            }
        }
        onNodeWithText("Top header").assertExists()
        onNodeWithText("Count 0").performClick()
        runOnIdle { wide = true }
        onNodeWithText("Side header").assertExists()
        onNodeWithText("Count 1").assertExists()
    }
}
