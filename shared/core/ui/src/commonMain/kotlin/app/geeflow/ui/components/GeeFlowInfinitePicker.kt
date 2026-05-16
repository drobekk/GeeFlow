package app.geeflow.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewWrapper
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.geeflow.ui.theme.GeeFlowComponentPreview
import app.geeflow.ui.theme.GeeFlowPreviewWrapper
import kotlinx.coroutines.launch

@Composable
fun GeeFlowInfinitePicker(
    items: List<String>,
    selected: String,
    onSelectionChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    hint: String? = null,
) {
    if (items.isEmpty()) return

    val itemHeightSp = 38.sp
    val itemHeight = with(LocalDensity.current) { itemHeightSp.toDp() }
    val totalHeight = itemHeight * VisibleItemsCount
    val density = LocalDensity.current
    val itemHeightPx = with(density) { itemHeight.toPx() }

    val initialIndex = remember(items, selected) {
        val selectedIndex = items.indexOf(selected).coerceAtLeast(0)
        val middle = Int.MAX_VALUE / 2
        val offset = middle % items.size
        middle - offset + selectedIndex - 1
    }

    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    val snapFlingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    val centerIndex by remember {
        derivedStateOf {
            val offset = listState.firstVisibleItemScrollOffset
            if (offset > itemHeightPx / 2) {
                listState.firstVisibleItemIndex + 2
            } else {
                listState.firstVisibleItemIndex + 1
            }
        }
    }
    var isEditing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val isDragged by listState.interactionSource.collectIsDraggedAsState()
    LaunchedEffect(isDragged) {
        if (isDragged) isEditing = false
    }

    LaunchedEffect(selected, items) {
        if (items.getOrNull(centerIndex % items.size) != selected) {
            listState.scrollToItem(initialIndex)
        }
    }

    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress) {
            val selectedItem = items[centerIndex % items.size]
            if (selectedItem != selected) {
                onSelectionChanged(selectedItem)
            }
        }
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.large,
        modifier = modifier.height(totalHeight),
    ) {
        Box(contentAlignment = Alignment.Center) {
            LazyColumn(
                state = listState,
                userScrollEnabled = enabled,
                flingBehavior = snapFlingBehavior,
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                items(
                    count = Int.MAX_VALUE,
                    key = { it },
                ) { index ->
                    val item = items[index % items.size]
                    Text(
                        text = item,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 1f else 0.38f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .height(itemHeight)
                            .fillMaxWidth()
                            .clickable(enabled = enabled && !isEditing, indication = null, interactionSource = null) {
                                scope.launch { listState.scrollToItem((index - 1).coerceAtLeast(0)) }
                                onSelectionChanged(item)
                            }
                            .padding(horizontal = 32.dp, vertical = 8.dp)
                            .wrapContentHeight(Alignment.CenterVertically),
                    )
                }
            }

            Surface(
                color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(itemHeight)
                    .padding(horizontal = 8.dp)
                    .scrollable(
                        state = listState,
                        reverseDirection = true,
                        orientation = Orientation.Vertical,
                        flingBehavior = snapFlingBehavior,
                        enabled = enabled && !isEditing,
                    )
                    .pointerInput(enabled, isEditing) {
                        if (enabled && !isEditing) {
                            detectTapGestures {
                                isEditing = true
                            }
                        }
                    },
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (isEditing) {
                        PickerSearchField(
                            items = items,
                            onSelectionChanged = onSelectionChanged,
                            listState = listState,
                            keyboardOptions = keyboardOptions,
                            hint = hint,
                            onDismiss = { isEditing = false },
                        )
                    } else {
                        val selectedItem = remember(items, centerIndex) { items[centerIndex % items.size] }
                        Text(
                            text = selectedItem,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (enabled) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .fillMaxSize()
                                .wrapContentHeight(Alignment.CenterVertically),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PickerSearchField(
    items: List<String>,
    onSelectionChanged: (String) -> Unit,
    listState: LazyListState,
    keyboardOptions: KeyboardOptions,
    hint: String?,
    onDismiss: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var searchQuery by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    var wasFocused by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    val textStyle = MaterialTheme.typography.bodyLarge.copy(
        color = MaterialTheme.colorScheme.onPrimary,
        textAlign = TextAlign.Center,
    )

    BasicTextField(
        value = searchQuery,
        onValueChange = { newQuery ->
            searchQuery = newQuery
            val matchIndex = items.indexOfFirst {
                it.startsWith(newQuery, ignoreCase = true)
            }
            if (matchIndex != -1) {
                scope.launch {
                    val currentFirst = listState.firstVisibleItemIndex
                    val base = currentFirst - (currentFirst % items.size)
                    val target = base + matchIndex - 1
                    listState.animateScrollToItem(target)
                }
            }
        },
        textStyle = textStyle,
        cursorBrush = SolidColor(MaterialTheme.colorScheme.onPrimary),
        singleLine = true,
        keyboardOptions = keyboardOptions.copy(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(
            onDone = {
                val matchIndex = items.indexOfFirst { it.startsWith(searchQuery, ignoreCase = true) }
                if (matchIndex != -1) {
                    onSelectionChanged(items[matchIndex])
                }
                onDismiss()
            },
        ),
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
            .onFocusChanged {
                if (it.isFocused) {
                    wasFocused = true
                } else if (wasFocused) {
                    onDismiss()
                }
            }
            .padding(horizontal = 16.dp),
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                if (searchQuery.isEmpty() && hint != null) {
                    Text(
                        text = hint,
                        style = textStyle.copy(
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f),
                        ),
                        textAlign = TextAlign.Center,
                    )
                }
                innerTextField()
            }
        },
    )
}

private const val VisibleItemsCount = 3

@PreviewWrapper(GeeFlowPreviewWrapper::class)
@Composable
@GeeFlowComponentPreview
private fun Preview() {
    Box(Modifier.padding(16.dp)) {
        GeeFlowInfinitePicker(
            items = (100..126).map { it.toString() },
            selected = "110",
            onSelectionChanged = {},
            keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
            hint = "Enter",
        )
    }
}
