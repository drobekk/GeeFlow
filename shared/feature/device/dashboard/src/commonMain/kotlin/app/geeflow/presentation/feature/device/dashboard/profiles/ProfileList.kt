package app.geeflow.presentation.feature.device.dashboard.profiles

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Arrangement.Absolute.spacedBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.InsertLink
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.hapticfeedback.HapticFeedbackType.Companion.LongPress
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewWrapper
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.geeflow.presentation.feature.device.dashboard.main.getMockProfileListViewState
import app.geeflow.presentation.feature.device.dashboard.profiles.ProfileListViewState.HistoryBrew
import app.geeflow.presentation.feature.device.dashboard.profiles.ProfileListViewState.Profile
import app.geeflow.ui.animations.slideTransform
import app.geeflow.ui.components.GeeFlowSwipeToRevealBox
import app.geeflow.ui.components.HorizontalSpacer
import app.geeflow.ui.components.SwipeToRevealBoxValue
import app.geeflow.ui.components.rememberSwipeToRevealBoxState
import app.geeflow.ui.icons.Experiment
import app.geeflow.ui.icons.GeeFlowIcon
import app.geeflow.ui.modifier.squareSize
import app.geeflow.ui.theme.GeeFlowComponentPreview
import app.geeflow.ui.theme.GeeFlowPreviewWrapper
import app.geeflow.ui.theme.disabled
import geeflow.shared.feature.device.dashboard.generated.resources.Res
import geeflow.shared.feature.device.dashboard.generated.resources.brew_history_empty
import geeflow.shared.feature.device.dashboard.generated.resources.experimental_title
import geeflow.shared.feature.device.dashboard.generated.resources.profile_list_add_profile
import geeflow.shared.feature.device.dashboard.generated.resources.profile_list_bind
import geeflow.shared.feature.device.dashboard.generated.resources.profile_list_delete
import geeflow.shared.feature.device.dashboard.generated.resources.profile_list_duplicate
import geeflow.shared.feature.device.dashboard.generated.resources.profile_list_search
import geeflow.shared.feature.device.dashboard.generated.resources.profile_list_show_history
import geeflow.shared.feature.device.dashboard.generated.resources.profile_list_show_profiles
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
internal fun ProfileList(
    viewState: ProfileListViewState,
    onEvent: (ProfileListEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    ProfileListContent(
        viewState = viewState,
        onEvent = onEvent,
        modifier = modifier,
    )
}

@Composable
private fun ProfileListContent(
    viewState: ProfileListViewState,
    onEvent: (ProfileListEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    var searchQuery by remember { mutableStateOf("") }
    var isSearchExpanded by remember { mutableStateOf(false) }

    val topContentPadding by animateDpAsState(if (isSearchExpanded) 78.dp else 0.dp)
    val verticalBias by animateFloatAsState(if (isSearchExpanded) -1.0f else 1.0f)

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.shapes.large),
    ) {
        AnimatedContent(
            targetState = viewState.showHistory,
            transitionSpec = { slideTransform(targetState) },
        ) { showHistory ->
            if (showHistory) {
                HistoryColumn(
                    history = viewState.history,
                    loading = viewState.historyLoading,
                    topContentPadding = topContentPadding,
                    onEvent = onEvent,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                ProfilesColumn(
                    viewState = viewState,
                    searchQuery = searchQuery,
                    topContentPadding = topContentPadding,
                    onEvent = onEvent,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        BottomBar(
            searchQuery = searchQuery,
            searchExpanded = isSearchExpanded,
            historyShown = viewState.showHistory,
            onSearchQueryChange = {
                searchQuery = it
                onEvent(ProfileListEvent.SearchQueryChanged(it))
            },
            onExpandedChange = { isSearchExpanded = it },
            onEvent = onEvent,
            modifier = Modifier.align(BiasAlignment(0.0f, verticalBias)),
        )
    }
}

@Composable
private fun ProfilesColumn(
    viewState: ProfileListViewState,
    searchQuery: String,
    topContentPadding: Dp,
    onEvent: (ProfileListEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val filteredProfiles = remember(viewState.profiles, searchQuery) {
        if (searchQuery.isBlank()) {
            viewState.profiles
        } else {
            viewState.profiles.filter {
                it.name.contains(
                    searchQuery,
                    ignoreCase = true,
                ) || it.description.contains(
                    searchQuery,
                    ignoreCase = true,
                )
            }
        }
    }

    val haptic = LocalHapticFeedback.current
    val lazyListState = rememberLazyListState()
    val reorderableState = rememberReorderableLazyListState(
        lazyListState = lazyListState,
        onMove = { from, to ->
            onEvent(ProfileListEvent.Reordered(from.index, to.index))
            haptic.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
        },
    )

    LazyColumn(
        state = lazyListState,
        modifier = modifier,
        contentPadding = PaddingValues(top = topContentPadding, bottom = 72.dp),
    ) {
        items(filteredProfiles, key = { it.id }) { profile ->
            ReorderableItem(
                state = reorderableState,
                enabled = !profile.bound,
                key = profile.id,
            ) { isDragging ->
                ProfileItem(
                    profile = profile,
                    smartScaleConnected = viewState.smartScaleConnected,
                    onEvent = onEvent,
                    isDragging = isDragging,
                    modifier = Modifier
                        .animateItem()
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .longPressDraggableHandle(
                            enabled = !profile.bound,
                            onDragStarted = { haptic.performHapticFeedback(LongPress) },
                        ),
                )
            }
        }
    }
}

/**
 * Brews are fetched a page at a time — reaching [LoadMoreThreshold] items from the end asks the
 * view model for the next page, which is a no-op once the whole history has been read.
 */
@Composable
private fun HistoryColumn(
    history: List<HistoryBrew>,
    loading: Boolean,
    topContentPadding: Dp,
    onEvent: (ProfileListEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val lazyListState = rememberLazyListState()
    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisible = lazyListState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            lastVisible >= lazyListState.layoutInfo.totalItemsCount - LoadMoreThreshold
        }
    }

    LaunchedEffect(shouldLoadMore, history.size) {
        if (shouldLoadMore && history.isNotEmpty()) onEvent(ProfileListEvent.LoadMoreHistory)
    }

    Box(modifier = modifier) {
        LazyColumn(
            state = lazyListState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = topContentPadding, bottom = 64.dp),
        ) {
            items(history, key = { it.id }) { brew ->
                HistoryItem(
                    brew = brew,
                    onClick = { onEvent(ProfileListEvent.HistoryBrewSelected(brew.id)) },
                    modifier = Modifier.animateItem(),
                )
            }
        }
        if (loading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter))
        }
        if (history.isEmpty() && !loading) {
            Text(
                text = stringResource(Res.string.brew_history_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.Center).padding(horizontal = 24.dp),
            )
        }
    }
}

@Composable
private fun HistoryItem(
    brew: HistoryBrew,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val backgroundColor = when {
        brew.selected -> MaterialTheme.colorScheme.surfaceContainerHigh
        else -> MaterialTheme.colorScheme.surfaceContainer
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = brew.name,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = brew.description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun BottomBar(
    searchQuery: String,
    searchExpanded: Boolean,
    historyShown: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onExpandedChange: (Boolean) -> Unit,
    onEvent: (ProfileListEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val topBrush = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.surfaceContainer,
            Color.Transparent,
        ),
    )
    val bottomBrush = Brush.verticalGradient(
        colors = listOf(
            Color.Transparent,
            MaterialTheme.colorScheme.surfaceContainer,
        ),
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(brush = if (searchExpanded) topBrush else bottomBrush)
            .padding(16.dp)
            .height(48.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AnimatedVisibility(
            !searchExpanded,
            enter = expandHorizontally(clip = false) + fadeIn(),
            exit = shrinkHorizontally(clip = false) + fadeOut(),
        ) {
            Row(
                modifier = Modifier
                    .padding(end = 16.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest, CircleShape)
                    .padding(horizontal = 6.dp),
            ) {
                ToggleButton(
                    checked = historyShown,
                    shapes = ToggleButtonDefaults.shapes(
                        shape = CircleShape,
                        checkedShape = CircleShape,
                        pressedShape = CircleShape,
                    ),
                    onCheckedChange = { onEvent(ProfileListEvent.HistoryClicked) },
                ) {
                    Icon(
                        painter = rememberVectorPainter(
                            if (historyShown) Icons.AutoMirrored.Filled.List else Icons.Filled.History,
                        ),
                        contentDescription = if (historyShown) {
                            stringResource(Res.string.profile_list_show_profiles)
                        } else {
                            stringResource(Res.string.profile_list_show_history)
                        },
                    )
                }
                AnimatedVisibility(!historyShown) {
                    IconButton(
                        onClick = { onEvent(ProfileListEvent.AddProfileClicked) },
                    ) {
                        Icon(
                            painter = rememberVectorPainter(Icons.Filled.Add),
                            contentDescription = stringResource(Res.string.profile_list_add_profile),
                        )
                    }
                }
            }
        }
        SearchBar(
            searchQuery = searchQuery,
            expanded = searchExpanded,
            onSearchQueryChange = onSearchQueryChange,
            onExpandedChange = onExpandedChange,
            modifier = Modifier.fillMaxHeight(),
        )
    }
}

@Composable
private fun SearchBar(
    searchQuery: String,
    expanded: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onExpandedChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(expanded) {
        if (expanded) {
            delay(FocusDelayMs)
            focusRequester.requestFocus()
        } else {
            onSearchQueryChange("")
        }
    }

    Row(
        modifier = modifier.background(color = MaterialTheme.colorScheme.background, shape = CircleShape),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AnimatedVisibility(
            modifier = Modifier.weight(1f, false),
            visible = expanded,
            enter = expandHorizontally(expandFrom = Alignment.End) + fadeIn(),
            exit = shrinkHorizontally(shrinkTowards = Alignment.End) + fadeOut(),
        ) {
            BasicTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .padding(start = 16.dp),
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                ),
                singleLine = true,
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = { innerTextField ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = stringResource(Res.string.profile_list_search),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            )
                        }
                        innerTextField()
                    }
                },
            )
        }

        AnimatedContent(
            targetState = expanded,
            contentAlignment = Alignment.Center,
        ) { currentExpanded ->
            if (currentExpanded) {
                IconButton(
                    modifier = Modifier.aspectRatio(1f),
                    onClick = {
                        onExpandedChange(false)
                        onSearchQueryChange("")
                    },
                ) {
                    Icon(
                        painter = rememberVectorPainter(Icons.Filled.Cancel),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                IconButton(
                    modifier = Modifier.aspectRatio(1f),
                    onClick = { onExpandedChange(true) },
                ) {
                    Icon(
                        painter = rememberVectorPainter(Icons.Filled.Search),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileItem(
    profile: Profile,
    smartScaleConnected: Boolean,
    onEvent: (ProfileListEvent) -> Unit,
    isDragging: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val swipeToDismissBoxState = rememberSwipeToRevealBoxState()
    val scope = rememberCoroutineScope()

    GeeFlowSwipeToRevealBox(
        state = swipeToDismissBoxState,
        modifier = modifier.fillMaxSize(),
        enableDismissFromStartToEnd = false,
        gesturesEnabled = !isDragging,
        backgroundContent = {
            ProfileItemRevealContent(
                profile = profile,
                modifier = Modifier.fillMaxSize(),
                onEvent = {
                    scope.launch {
                        swipeToDismissBoxState.dismiss(SwipeToRevealBoxValue.Settled)
                        onEvent(it)
                    }
                },
            )
        },
    ) {
        ProfileItemContent(
            profile = profile,
            smartScaleConnected = smartScaleConnected,
            onProfileClick = { id -> onEvent(ProfileListEvent.ProfileSelected(id)) },
        )
    }
}

@Composable
private fun ProfileItemRevealContent(
    profile: Profile,
    onEvent: (ProfileListEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.background(MaterialTheme.colorScheme.surface),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        IconButton(
            onClick = { onEvent(ProfileListEvent.RemoveProfileClicked(profile.id)) },
            enabled = !profile.bound,
            colors = IconButtonDefaults.iconButtonColors(
                contentColor = MaterialTheme.colorScheme.error,
            ),
        ) {
            Icon(
                painter = rememberVectorPainter(Icons.Filled.Delete),
                contentDescription = stringResource(Res.string.profile_list_delete),
            )
        }
        IconButton(
            onClick = { onEvent(ProfileListEvent.DuplicateProfileClicked(profile.id)) },
            colors = IconButtonDefaults.iconButtonColors(
                contentColor = MaterialTheme.colorScheme.secondary,
            ),
        ) {
            Icon(
                painter = rememberVectorPainter(Icons.Filled.ContentCopy),
                contentDescription = stringResource(Res.string.profile_list_duplicate),
            )
        }
        IconButton(
            onClick = { onEvent(ProfileListEvent.EditProfileClicked(profile.id)) },
            colors = IconButtonDefaults.iconButtonColors(
                contentColor = MaterialTheme.colorScheme.primary,
            ),
        ) {
            Icon(
                painter = rememberVectorPainter(Icons.Filled.Edit),
                contentDescription = stringResource(Res.string.profile_list_search),
            )
        }
        IconButton(
            onClick = { onEvent(ProfileListEvent.BindProfileClicked(profile.id)) },
            enabled = profile.canBind,
            colors = IconButtonDefaults.iconButtonColors(
                contentColor = MaterialTheme.colorScheme.tertiary,
            ),
        ) {
            Icon(
                painter = rememberVectorPainter(Icons.Filled.Link),
                contentDescription = stringResource(Res.string.profile_list_bind),
            )
        }
    }
}

@Composable
private fun ProfileItemContent(
    profile: Profile,
    smartScaleConnected: Boolean,
    onProfileClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val backgroundColor = when {
        profile.selected -> MaterialTheme.colorScheme.surfaceContainerHigh
        else -> MaterialTheme.colorScheme.surfaceContainer
    }
    val numberColor = when {
        profile.selected -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val numberTextColor = when {
        profile.selected -> MaterialTheme.colorScheme.onPrimary
        else -> MaterialTheme.colorScheme.onSurface
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .clickable { onProfileClick(profile.id) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AnimatedContent(
            targetState = profile.bound,
        ) { bound ->
            if (bound) {
                Icon(
                    modifier = Modifier
                        .size(ProfileBadgeSize)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                        .padding(4.dp),
                    painter = rememberVectorPainter(Icons.Filled.InsertLink),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                )
            } else {
                Text(
                    text = profile.number,
                    style = MaterialTheme.typography.titleSmall,
                    color = numberTextColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .size(ProfileBadgeSize)
                        .background(numberColor, CircleShape)
                        .squareSize()
                        .padding(vertical = 6.dp),
                )
            }
        }
        HorizontalSpacer(12.dp)
        Column(Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = spacedBy(4.dp),
            ) {
                Text(
                    text = profile.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f, false),
                )
                if (profile.experimental) {
                    Icon(
                        GeeFlowIcon.Experiment,
                        contentDescription = stringResource(Res.string.experimental_title),
                        modifier = Modifier.size(16.dp),
                    )
                }
                if (profile.brewByWeight) {
                    val backgroundColor = if (smartScaleConnected) {
                        MaterialTheme.colorScheme.tertiary
                    } else {
                        MaterialTheme.colorScheme.tertiary.disabled()
                    }
                    Box(modifier = Modifier.background(backgroundColor, CircleShape).size(8.dp))
                }
            }
            Text(
                text = profile.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private val ProfileBadgeSize = 32.dp
private const val FocusDelayMs = 150L
private const val LoadMoreThreshold = 5

@PreviewWrapper(GeeFlowPreviewWrapper::class)
@Composable
@GeeFlowComponentPreview
private fun Preview() {
    ProfileList(
        viewState = getMockProfileListViewState(),
        onEvent = {},
        modifier = Modifier.padding(16.dp),
    )
}
