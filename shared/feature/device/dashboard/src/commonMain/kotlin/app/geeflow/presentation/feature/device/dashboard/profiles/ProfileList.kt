@file:Suppress("TooManyFunctions")

package app.geeflow.presentation.feature.device.dashboard.profiles

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import app.geeflow.domain.device.recipe.RecipeControl
import app.geeflow.domain.device.recipe.RecipeValueSource
import app.geeflow.domain.device.recipe.LiveRecipeDocument
import app.geeflow.domain.device.recipe.LiveRecipeCompiler
import app.geeflow.presentation.feature.device.dashboard.livecontrol.rememberLiveBrewServiceStarter
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.geeflow.data.brew.model.BrewProgram
import app.geeflow.data.brew.model.Condition
import app.geeflow.data.brew.model.FreeHandControlMode
import app.geeflow.data.brew.model.PhaseControl
import app.geeflow.presentation.feature.device.dashboard.profiles.ProfileListViewState.HistoryBrew
import app.geeflow.presentation.feature.device.dashboard.profiles.ProfileListViewState.Profile
import app.geeflow.ui.components.GeeFlowSwipeToRevealBox
import app.geeflow.ui.components.SwipeToRevealBoxValue
import app.geeflow.ui.components.rememberSwipeToRevealBoxState
import app.geeflow.ui.text.ClearText
import app.geeflow.ui.text.clearText
import app.geeflow.ui.theme.GeeFlowTheme
import geeflow.shared.feature.device.dashboard.generated.resources.Res
import geeflow.shared.feature.device.dashboard.generated.resources.brew_history_empty
import geeflow.shared.feature.device.dashboard.generated.resources.profile_list_add_favorite
import geeflow.shared.feature.device.dashboard.generated.resources.profile_list_all
import geeflow.shared.feature.device.dashboard.generated.resources.profile_list_favorites
import geeflow.shared.feature.device.dashboard.generated.resources.profile_list_filter_flow
import geeflow.shared.feature.device.dashboard.generated.resources.profile_list_filter_mixed
import geeflow.shared.feature.device.dashboard.generated.resources.profile_list_filter_other
import geeflow.shared.feature.device.dashboard.generated.resources.profile_list_filter_pressure
import geeflow.shared.feature.device.dashboard.generated.resources.profile_list_filter_recorded
import geeflow.shared.feature.device.dashboard.generated.resources.profile_list_history
import geeflow.shared.feature.device.dashboard.generated.resources.profile_list_new_profile
import geeflow.shared.feature.device.dashboard.generated.resources.profile_list_no_matches
import geeflow.shared.feature.device.dashboard.generated.resources.profile_list_no_target
import geeflow.shared.feature.device.dashboard.generated.resources.profile_list_remove_favorite
import geeflow.shared.feature.device.dashboard.generated.resources.profile_list_search
import geeflow.shared.feature.device.dashboard.generated.resources.profile_list_selected
import geeflow.shared.feature.device.dashboard.generated.resources.profile_list_show_profiles
import geeflow.shared.feature.device.dashboard.generated.resources.profile_list_target_volume
import geeflow.shared.feature.device.dashboard.generated.resources.profile_list_target_weight
import geeflow.shared.feature.device.dashboard.generated.resources.profile_sync_close
import geeflow.shared.feature.device.dashboard.generated.resources.profile_sync_connected
import geeflow.shared.feature.device.dashboard.generated.resources.profile_sync_error
import geeflow.shared.feature.device.dashboard.generated.resources.profile_sync_not_configured
import geeflow.shared.feature.device.dashboard.generated.resources.profile_sync_ready
import geeflow.shared.feature.device.dashboard.generated.resources.profile_sync_offline
import geeflow.shared.feature.device.dashboard.generated.resources.profile_sync_syncing
import geeflow.shared.feature.device.dashboard.generated.resources.profile_sync_title
import kotlinx.coroutines.launch
import geeflow.shared.feature.device.dashboard.generated.resources.dashboard_review_recipe
import geeflow.shared.feature.device.dashboard.generated.resources.dashboard_recipe_unavailable
import geeflow.shared.feature.device.dashboard.generated.resources.dashboard_import_recipe
import geeflow.shared.feature.device.dashboard.generated.resources.dashboard_recipe_actions
import geeflow.shared.feature.device.dashboard.generated.resources.dashboard_scale_required
import geeflow.shared.feature.device.dashboard.generated.resources.dashboard_ready_recipe
import geeflow.shared.feature.device.dashboard.generated.resources.dashboard_edit_recipe
import org.jetbrains.compose.resources.stringResource
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
internal fun ProfileList(
    viewState: ProfileListViewState,
    onEvent: (ProfileListEvent) -> Unit,
    modifier: Modifier = Modifier,
    phone: Boolean = false,
    navigateOnProfileSelect: Boolean = true,
    liveScaleConnected: Boolean = false,
    onLiveRecipeSelected: (LiveRecipeDocument) -> Unit = {},
    onLiveRecipeFavoriteChanged: (LiveRecipeDocument) -> Unit = {},
) {
    val liveStore = rememberLiveBrewServiceStarter().recipes
    val localRecipes by liveStore.recipes.collectAsState()
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedFilter by rememberSaveable { mutableStateOf(ProfileFilter.All) }
    var showProfileLibraryDialog by remember { mutableStateOf(false) }
    val filterOptions = ProfileFilter.entries
    val filteredProfiles = remember(viewState.profiles, searchQuery, selectedFilter) {
        viewState.profiles.filter { profile ->
            val matchesFilter = when (selectedFilter) {
                ProfileFilter.All -> true
                ProfileFilter.Favorites -> profile.favorite
                else -> profile.controlKind == selectedFilter.kind
            }
            val matchesSearch = searchQuery.isBlank() ||
                profile.name.contains(searchQuery, ignoreCase = true) ||
                profile.description.contains(searchQuery, ignoreCase = true)
            matchesFilter && matchesSearch
        }
    }
    val filteredHistory = remember(viewState.history, searchQuery) {
        viewState.history.filter { brew ->
            searchQuery.isBlank() || brew.name.contains(searchQuery, true) || brew.description.contains(searchQuery, true)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surface)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val openProfileLibrary = {
            onEvent(ProfileListEvent.SyncProfileLibraryNow)
            showProfileLibraryDialog = true
        }
        var actionsOpen by remember { mutableStateOf(false) }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(Modifier.weight(1f)) {
                ProfileSearchField(
                    value = searchQuery,
                    compact = phone,
                    onValueChange = {
                        searchQuery = it
                        onEvent(ProfileListEvent.SearchQueryChanged(it))
                    },
                )
            }
            if (!viewState.showHistory) {
                if (phone) {
                    var filtersOpen by remember { mutableStateOf(false) }
                    Box {
                        TextButton(onClick = { filtersOpen = true }) {
                            Text(selectedFilter.label(), maxLines = 1)
                            Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                        }
                        DropdownMenu(expanded = filtersOpen, onDismissRequest = { filtersOpen = false }) {
                            filterOptions.forEach { filter ->
                                DropdownMenuItem(text = { Text(filter.label()) }, onClick = {
                                    selectedFilter = filter
                                    filtersOpen = false
                                })
                            }
                        }
                    }
                }
            }
            if (!viewState.showHistory) ProfileLibraryButton(viewState.profileLibrarySyncState, onClick = openProfileLibrary)
            val historyLabel = stringResource(
                if (viewState.showHistory) Res.string.profile_list_show_profiles else Res.string.profile_list_history,
            )
            if (phone) {
                IconButton(onClick = { onEvent(ProfileListEvent.HistoryClicked) }) {
                    Icon(Icons.Filled.History, contentDescription = historyLabel)
                }
            } else {
                TextButton(onClick = { onEvent(ProfileListEvent.HistoryClicked) }) {
                    Icon(Icons.Filled.History, contentDescription = null)
                    Text(historyLabel, modifier = Modifier.padding(start = 4.dp))
                }
            }
            Box {
                IconButton(onClick = { actionsOpen = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = stringResource(Res.string.dashboard_recipe_actions))
                }
                DropdownMenu(expanded = actionsOpen, onDismissRequest = { actionsOpen = false }) {
                    val actions = listOf(
                        Res.string.profile_list_new_profile to ProfileListEvent.AddProfileClicked,
                        Res.string.dashboard_import_recipe to ProfileListEvent.OpenLiveRecipe(),
                    )
                    actions.forEach { (label, event) ->
                        DropdownMenuItem(text = { Text(stringResource(label)) }, onClick = {
                            actionsOpen = false
                            onEvent(event)
                        })
                    }
                }
            }
        }

        if (!phone && !viewState.showHistory) {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                filterOptions.forEach { filter ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                        label = { Text(filter.label(), style = MaterialTheme.typography.labelMedium) },
                    )
                }
            }
        }

        if (!viewState.showHistory) {
            ProfilesColumn(
                profiles = filteredProfiles,
                localRecipes = localRecipes.filter { recipe ->
                    val pressure = recipe.phases.any { it.control == RecipeControl.Pressure }
                    val flow = recipe.phases.any { it.control == RecipeControl.MachineFlow || it.control == RecipeControl.CupFlow }
                    val kind = when { pressure && flow -> ProfileControlKind.Mixed; pressure -> ProfileControlKind.Pressure; flow -> ProfileControlKind.Flow; else -> ProfileControlKind.Other }
                    (searchQuery.isBlank() || recipe.name.contains(searchQuery, true)) &&
                        when (selectedFilter) {
                            ProfileFilter.All -> true
                            ProfileFilter.Favorites -> recipe.favorite
                            else -> selectedFilter.kind == kind
                        }
                },
                liveScaleConnected = liveScaleConnected,
                searchActive = searchQuery.isNotBlank() || selectedFilter != ProfileFilter.All,
                smartScaleConnected = viewState.smartScaleConnected,
                onEvent = onEvent,
                onLiveRecipeSelected = onLiveRecipeSelected,
                onLiveRecipeFavoriteChanged = onLiveRecipeFavoriteChanged,
                phone = phone,
                navigateOnProfileSelect = navigateOnProfileSelect,
                modifier = Modifier.weight(1f).fillMaxWidth(),
            )
        } else {
            HistoryColumn(
                history = filteredHistory,
                loading = viewState.historyLoading,
                onEvent = onEvent,
                modifier = Modifier.weight(1f).fillMaxWidth(),
            )
        }

    }

    if (showProfileLibraryDialog) {
        val syncStatus = when (viewState.profileLibrarySyncState.phase) {
            ProfileLibrarySyncState.Phase.NotConfigured ->
                stringResource(Res.string.profile_sync_not_configured)
            ProfileLibrarySyncState.Phase.Ready ->
                stringResource(Res.string.profile_sync_ready)
            ProfileLibrarySyncState.Phase.Syncing ->
                stringResource(Res.string.profile_sync_syncing)
            ProfileLibrarySyncState.Phase.Synced ->
                stringResource(Res.string.profile_sync_connected)
            ProfileLibrarySyncState.Phase.Offline ->
                stringResource(Res.string.profile_sync_offline)
            ProfileLibrarySyncState.Phase.Error ->
                viewState.profileLibrarySyncState.message ?: stringResource(Res.string.profile_sync_error)
        }
        AlertDialog(
            onDismissRequest = { showProfileLibraryDialog = false },
            title = { Text(stringResource(Res.string.profile_sync_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(syncStatus)
                    viewState.profileLibrarySyncState.message?.let { Text(it) }
                }
            },
            confirmButton = {
                TextButton(onClick = { showProfileLibraryDialog = false }) {
                    Text(stringResource(Res.string.profile_sync_close))
                }
            },
        )
    }
}

@Composable
private fun ProfileLibraryButton(syncState: ProfileLibrarySyncState, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(40.dp)) {
        Icon(
            Icons.Filled.Cloud,
            contentDescription = stringResource(Res.string.profile_sync_title),
            tint = when (syncState.phase) {
                ProfileLibrarySyncState.Phase.Synced -> MaterialTheme.colorScheme.primary
                ProfileLibrarySyncState.Phase.Error -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}

@Composable
private fun ProfileSearchField(value: String, compact: Boolean, onValueChange: (String) -> Unit) {
    val focusRequester = remember { FocusRequester() }
    val fieldHeight = if (compact) 40.dp else 48.dp
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(fieldHeight)
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Filled.Search,
            contentDescription = stringResource(Res.string.profile_list_search),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(8.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f).height(fieldHeight).focusRequester(focusRequester),
            textStyle = (if (compact) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge)
                .copy(color = MaterialTheme.colorScheme.onSurface),
            singleLine = true,
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            decorationBox = { innerTextField ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty()) {
                        Text(
                            stringResource(Res.string.profile_list_search),
                            style = if (compact) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    innerTextField()
                }
            },
        )
        AnimatedVisibility(value.isNotEmpty()) {
            TextButton(onClick = { onValueChange("") }, modifier = Modifier.heightIn(min = 48.dp)) {
                Icon(Icons.Filled.Close, contentDescription = clearText(ClearText.ClearSearch))
            }
        }
    }
}

@Composable
private fun ProfilesColumn(
    profiles: List<Profile>,
    localRecipes: List<LiveRecipeDocument> = emptyList(),
    liveScaleConnected: Boolean,
    searchActive: Boolean,
    smartScaleConnected: Boolean,
    onEvent: (ProfileListEvent) -> Unit,
    onLiveRecipeSelected: (LiveRecipeDocument) -> Unit,
    onLiveRecipeFavoriteChanged: (LiveRecipeDocument) -> Unit,
    phone: Boolean,
    navigateOnProfileSelect: Boolean,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val listState = rememberLazyListState()
    val reorderState = rememberReorderableLazyListState(
        lazyListState = listState,
        onMove = { from, to ->
            if (from.index in profiles.indices && to.index in profiles.indices) {
                onEvent(ProfileListEvent.Reordered(from.index, to.index))
                haptic.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
            }
        },
    )
    if (profiles.isEmpty() && localRecipes.isEmpty()) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                stringResource(Res.string.profile_list_no_matches),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    Box(modifier) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(end = 12.dp),
            contentPadding = PaddingValues(vertical = 2.dp),
            verticalArrangement = Arrangement.spacedBy(if (phone) 4.dp else 8.dp),
        ) {
            items(profiles, key = { it.id }) { profile ->
                ReorderableItem(
                    state = reorderState,
                    enabled = !searchActive && !profile.bound,
                    key = profile.id,
                ) { isDragging ->
                    Column {
                        ProfileItem(
                            profile = profile,
                            phone = phone,
                            navigateOnProfileSelect = navigateOnProfileSelect,
                            smartScaleConnected = smartScaleConnected,
                            onEvent = onEvent,
                            isDragging = isDragging,
                            modifier = Modifier
                                .animateItem()
                                .longPressDraggableHandle(
                                    enabled = !searchActive && !profile.bound,
                                    onDragStarted = { haptic.performHapticFeedback(HapticFeedbackType.LongPress) },
                                ),
                        )
                        if (phone) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    }
                }
            }
            items(localRecipes, key = { "live:${it.id}" }) { recipe ->
                Column(Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.fillMaxWidth().clickable { onLiveRecipeSelected(recipe) }
                            .padding(horizontal = if (phone) 8.dp else 12.dp, vertical = if (phone) 6.dp else 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(
                            Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(if (phone) 2.dp else 4.dp),
                        ) {
                            Text(recipe.name, style = if (phone) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.titleMedium, maxLines = if (phone) 1 else 2, overflow = TextOverflow.Ellipsis)
                            Text(
                                "${recipe.settings.doseGrams.toDouble().formatOneDecimal()} g / ${recipe.settings.yieldGrams.toDouble().formatOneDecimal()} g",
                                style = MaterialTheme.typography.bodySmall,
                            )
                            val readiness = when {
                                recipe.mappings.isNotEmpty() && !recipe.mappingsAccepted -> Res.string.dashboard_review_recipe
                                LiveRecipeCompiler.issues(recipe).isNotEmpty() -> Res.string.dashboard_recipe_unavailable
                                !liveScaleConnected -> Res.string.dashboard_scale_required
                                else -> Res.string.dashboard_ready_recipe
                            }
                            Text(stringResource(readiness), style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        val targets = remember(recipe.phases) { livePreviewTargets(recipe) }
                        if (phone) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconToggleButton(checked = recipe.favorite, onCheckedChange = { onLiveRecipeFavoriteChanged(recipe.copy(favorite = it)) }, modifier = Modifier.size(48.dp)) {
                                    Icon(
                                        if (recipe.favorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                        contentDescription = if (recipe.favorite) stringResource(Res.string.profile_list_remove_favorite) else stringResource(Res.string.profile_list_add_favorite),
                                        tint = if (recipe.favorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                IconButton(onClick = { onEvent(ProfileListEvent.OpenLiveRecipe(recipe.id)) }, modifier = Modifier.size(48.dp)) {
                                    Icon(Icons.Filled.Edit, contentDescription = stringResource(Res.string.dashboard_edit_recipe))
                                }
                            }
                        } else {
                            Column(
                                Modifier.width(100.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                ProfileLinePreview(targets)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconToggleButton(checked = recipe.favorite, onCheckedChange = { onLiveRecipeFavoriteChanged(recipe.copy(favorite = it)) }) {
                                        Icon(
                                            if (recipe.favorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                            contentDescription = if (recipe.favorite) stringResource(Res.string.profile_list_remove_favorite) else stringResource(Res.string.profile_list_add_favorite),
                                            tint = if (recipe.favorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    IconButton(onClick = { onEvent(ProfileListEvent.OpenLiveRecipe(recipe.id)) }) {
                                        Icon(Icons.Filled.Edit, contentDescription = stringResource(Res.string.dashboard_edit_recipe))
                                    }
                                }
                            }
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                }
            }
        }
        ProfileScrollbar(listState, Modifier.align(Alignment.CenterEnd).fillMaxHeight().width(6.dp))
    }
}

@Composable
private fun ProfileScrollbar(state: LazyListState, modifier: Modifier = Modifier) {
    val track = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
    val thumb = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
    Canvas(modifier) {
        if (!state.canScrollForward && !state.canScrollBackward) return@Canvas
        val info = state.layoutInfo
        val visible = info.visibleItemsInfo
        if (visible.isEmpty() || info.totalItemsCount == 0) return@Canvas
        val viewport = (info.viewportEndOffset - info.viewportStartOffset).toFloat()
        val average = visible.map { it.size }.average().toFloat().coerceAtLeast(1f)
        val total = average * info.totalItemsCount
        val height = (size.height * viewport / total).coerceIn(24.dp.toPx().coerceAtMost(size.height), size.height)
        val fraction = (state.firstVisibleItemIndex + state.firstVisibleItemScrollOffset / average) /
            (info.totalItemsCount - viewport / average).coerceAtLeast(1f)
        val y = (size.height - height) * fraction.coerceIn(0f, 1f)
        drawRoundRect(track, cornerRadius = CornerRadius(size.width))
        drawRoundRect(thumb, Offset(0f, y), Size(size.width, height), CornerRadius(size.width))
    }
}

@Composable
private fun HistoryColumn(
    history: List<HistoryBrew>,
    loading: Boolean,
    onEvent: (ProfileListEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            lastVisible >= listState.layoutInfo.totalItemsCount - LoadMoreThreshold
        }
    }
    LaunchedEffect(shouldLoadMore, history.size) {
        if (shouldLoadMore && history.isNotEmpty()) onEvent(ProfileListEvent.LoadMoreHistory)
    }
    Box(modifier = modifier) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items(history, key = { it.id }) { brew ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .background(
                            if (brew.selected) {
                                MaterialTheme.colorScheme.secondaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerLow
                            }
                        )
                        .clickable { onEvent(ProfileListEvent.HistoryBrewSelected(brew.id)) }
                        .padding(12.dp),
                ) {
                    Text(brew.name, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                    Text(
                        brew.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        if (loading) LinearProgressIndicator(Modifier.fillMaxWidth().align(Alignment.TopCenter))
        if (history.isEmpty() && !loading) {
            Text(
                stringResource(Res.string.brew_history_empty),
                modifier = Modifier.align(Alignment.Center).padding(24.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ProfileItem(
    profile: Profile,
    phone: Boolean,
    navigateOnProfileSelect: Boolean,
    smartScaleConnected: Boolean,
    onEvent: (ProfileListEvent) -> Unit,
    isDragging: Boolean,
    modifier: Modifier = Modifier,
) {
    val swipeState = rememberSwipeToRevealBoxState()
    val scope = rememberCoroutineScope()
    GeeFlowSwipeToRevealBox(
        state = swipeState,
        modifier = modifier.fillMaxWidth(),
        enableDismissFromStartToEnd = false,
        gesturesEnabled = !isDragging,
        backgroundContent = {
            ProfileActions(
                profile = profile,
                modifier = Modifier.fillMaxSize(),
                onAction = { event ->
                    scope.launch {
                        swipeState.dismiss(SwipeToRevealBoxValue.Settled)
                        onEvent(event)
                    }
                },
            )
        },
    ) {
        ProfileCard(
            profile = profile,
            phone = phone,
            navigateOnProfileSelect = navigateOnProfileSelect,
            smartScaleConnected = smartScaleConnected,
            onClick = {
                onEvent(
                    if (navigateOnProfileSelect) ProfileListEvent.ProfileSelected(profile.id)
                    else ProfileListEvent.ProfilePreviewSelected(profile.id),
                )
            },
            onFavoriteClick = { onEvent(ProfileListEvent.ToggleFavoriteClicked(profile.id)) },
        )
    }
}

@Composable
private fun ProfileActions(profile: Profile, modifier: Modifier = Modifier, onAction: (ProfileListEvent) -> Unit) {
    Row(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ActionButton(
            clearText(ClearText.Edit),
            Icons.Filled.Edit,
            enabled = true,
            modifier = Modifier.weight(1f)
        ) { onAction(ProfileListEvent.EditProfileClicked(profile.id)) }
        ActionButton(
            clearText(ClearText.Copy),
            Icons.Filled.ContentCopy,
            enabled = true,
            modifier = Modifier.weight(1f)
        ) { onAction(ProfileListEvent.DuplicateProfileClicked(profile.id)) }
        ActionButton(
            clearText(ClearText.Bind),
            Icons.Filled.Link,
            enabled = profile.canBind,
            modifier = Modifier.weight(1f)
        ) { onAction(ProfileListEvent.BindProfileClicked(profile.id)) }
        ActionButton(
            clearText(ClearText.Delete),
            Icons.Filled.Delete,
            enabled = !profile.bound,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.weight(1f)
        ) {
            onAction(ProfileListEvent.RemoveProfileClicked(profile.id))
        }
    }
}

@Composable
private fun ActionButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean,
    tint: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    TextButton(onClick = onClick, enabled = enabled, modifier = modifier.heightIn(min = 48.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 1)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProfileCard(
    profile: Profile,
    phone: Boolean,
    navigateOnProfileSelect: Boolean,
    smartScaleConnected: Boolean,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit,
) {
    val selectedColor = MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(
                if (profile.selected) {
                    selectedColor.copy(alpha = 0.055f).compositeOver(MaterialTheme.colorScheme.surface)
                } else {
                    MaterialTheme.colorScheme.surface
                },
            )
            .then(
                if (!phone || profile.selected) {
                    Modifier.border(
                        width = if (profile.selected) 1.5.dp else 0.5.dp,
                        color = if (profile.selected) selectedColor.copy(alpha = 0.55f)
                        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        shape = MaterialTheme.shapes.medium,
                    )
                } else Modifier
            )
            .clickable(role = Role.RadioButton, onClick = onClick)
            .semantics { selected = profile.selected }
            .padding(if (phone) 8.dp else 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(if (phone) 4.dp else 8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (profile.selected) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = stringResource(Res.string.profile_list_selected),
                        tint = selectedColor,
                        modifier = Modifier.size(16.dp),
                    )
                }
                Text(
                    profile.name,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, lineHeight = 22.sp),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = if (phone) 1 else 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                TargetLabel(profile.finishCondition)
                if (profile.experimental) StatusLabel(clearText(ClearText.Experimental), MaterialTheme.colorScheme.tertiary)
                if (profile.bound) StatusLabel(clearText(ClearText.Bound), MaterialTheme.colorScheme.secondary)
                if (profile.brewByWeight && !smartScaleConnected) {
                    StatusLabel(clearText(ClearText.ScaleNeeded), MaterialTheme.colorScheme.error)
                }
            }
            profile.description.takeIf { it.isNotBlank() && (!phone || profile.selected) }?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, maxLines = if (phone) 1 else 5, overflow = TextOverflow.Ellipsis)
            }
        }
        val targets = remember(profile.program, profile.targetData) {
            profile.previewTargets()
        }
        if (phone) {
            Row(
                Modifier.width(104.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ProfileLinePreview(targets, Modifier.weight(1f).height(48.dp))
                favoriteButton(profile.favorite, selectedColor, onFavoriteClick)
            }
        } else {
            Column(
                Modifier.width(100.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                ProfileLinePreview(targets)
                favoriteButton(profile.favorite, selectedColor, onFavoriteClick)
            }
        }
    }
}

internal data class PreviewTarget(
    val seconds: Float,
    val pressure: Float? = null,
    val waterFlow: Float? = null,
    val cupFlow: Float? = null,
)

internal fun Profile.previewTargets(): List<PreviewTarget> {
    val recordedMode = (program as? BrewProgram.Recording)?.recording?.controlMode
    return targetData.entries.sortedBy { it.key }.map { (time, data) ->
        val showFlow = when (recordedMode) {
            FreeHandControlMode.Flow -> true
            FreeHandControlMode.Pressure -> false
            null -> data.pressure <= 0f && data.volumePerSecond > 0f
        }
        PreviewTarget(
            seconds = time,
            pressure = data.pressure.takeIf { !showFlow && it > 0f },
            waterFlow = data.volumePerSecond.takeIf { showFlow && it > 0f },
        )
    }
}

private fun livePreviewTargets(recipe: LiveRecipeDocument): List<PreviewTarget> = buildList {
    var elapsedMillis = 0L
    for (phase in recipe.phases) {
        val fixed = phase.target.value.takeIf { phase.target.source == RecipeValueSource.Fixed }
        val start = PreviewTarget(
            seconds = elapsedMillis / 1_000f,
            pressure = fixed.takeIf { phase.control == RecipeControl.Pressure },
            waterFlow = fixed.takeIf { phase.control == RecipeControl.MachineFlow },
            cupFlow = fixed.takeIf { phase.control == RecipeControl.CupFlow },
        )
        elapsedMillis += phase.maximumDurationMillis.coerceAtLeast(1)
        add(start)
        add(start.copy(seconds = elapsedMillis / 1_000f))
    }
}

@Composable
internal fun RecipeCurvePreview(profile: Profile, modifier: Modifier = Modifier.fillMaxWidth().height(64.dp)) {
    val targets = remember(profile.program, profile.targetData) {
        profile.previewTargets()
    }
    ProfileLinePreview(targets, modifier)
}

@Composable
private fun favoriteButton(checked: Boolean, selectedColor: Color, onClick: () -> Unit) {
    IconToggleButton(checked = checked, onCheckedChange = { onClick() }, modifier = Modifier.size(48.dp)) {
        Icon(
            imageVector = if (checked) Icons.Filled.Star else Icons.Outlined.StarBorder,
            contentDescription = stringResource(if (checked) Res.string.profile_list_remove_favorite else Res.string.profile_list_add_favorite),
            tint = if (checked) selectedColor else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun ProfileLinePreview(
    points: List<PreviewTarget>,
    modifier: Modifier = Modifier.fillMaxWidth().height(64.dp),
) {
    if (points.size < 2) return
    val description = clearText(ClearText.ProgrammedTargets)
    val pressure = GeeFlowTheme.colors.pressure
    val water = GeeFlowTheme.colors.water
    val cup = GeeFlowTheme.colors.waterVariant
    Canvas(modifier.semantics { contentDescription = description }) {
        val duration = points.last().seconds.coerceAtLeast(0.001f)
        fun line(color: Color, value: (PreviewTarget) -> Float?) {
            if (points.none { (value(it) ?: 0f) > 0f }) return
            fun point(sample: PreviewTarget): Offset? = value(sample)?.takeIf { it.isFinite() }?.let {
                Offset(size.width * (sample.seconds / duration).coerceIn(0f, 1f),
                    size.height * (0.94f - 0.88f * (it / 12f).coerceIn(0f, 1f)))
            }
            var previous: Offset? = null
            for (sample in points) {
                val next = point(sample)
                val start = previous
                if (start != null && next != null) drawLine(color, start, next, strokeWidth = 2.dp.toPx())
                previous = next
            }
        }
        line(pressure) { it.pressure }
        line(water) { it.waterFlow }
        line(cup) { it.cupFlow }
    }
}

@Composable
private fun TargetLabel(condition: Condition?) {
    val text = when (condition) {
        is Condition.Weight -> stringResource(
            Res.string.profile_list_target_weight,
            condition.target.toDouble().formatOneDecimal()
        )
        is Condition.Volume -> stringResource(
            Res.string.profile_list_target_volume,
            condition.target.toDouble().formatOneDecimal()
        )
        null -> stringResource(Res.string.profile_list_no_target)
    }
    Text(
        text,
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun StatusLabel(label: String, color: Color) {
    Text(
        label,
        modifier = Modifier.clip(CircleShape).background(color.copy(alpha = 0.12f)).padding(horizontal = 6.dp, vertical = 2.dp),
        style = MaterialTheme.typography.labelSmall,
        color = color,
        maxLines = 1,
    )
}

private enum class ProfileFilter(val kind: ProfileControlKind?) {
    All(null),
    Favorites(null),
    Pressure(ProfileControlKind.Pressure),
    Flow(ProfileControlKind.Flow),
    Mixed(ProfileControlKind.Mixed),
    Recorded(ProfileControlKind.Recorded),
    Other(ProfileControlKind.Other),
}

@Composable
private fun ProfileFilter.label(): String = when (this) {
    ProfileFilter.All -> stringResource(Res.string.profile_list_all)
    ProfileFilter.Favorites -> stringResource(Res.string.profile_list_favorites)
    ProfileFilter.Pressure -> ProfileControlKind.Pressure.label()
    ProfileFilter.Flow -> ProfileControlKind.Flow.label()
    ProfileFilter.Mixed -> ProfileControlKind.Mixed.label()
    ProfileFilter.Recorded -> ProfileControlKind.Recorded.label()
    ProfileFilter.Other -> ProfileControlKind.Other.label()
}

@Composable
private fun ProfileControlKind.label(): String = when (this) {
    ProfileControlKind.Pressure -> stringResource(Res.string.profile_list_filter_pressure)
    ProfileControlKind.Flow -> stringResource(Res.string.profile_list_filter_flow)
    ProfileControlKind.Mixed -> stringResource(Res.string.profile_list_filter_mixed)
    ProfileControlKind.Recorded -> stringResource(Res.string.profile_list_filter_recorded)
    ProfileControlKind.Other -> stringResource(Res.string.profile_list_filter_other)
}

private fun Double.formatOneDecimal() = ((this * DecimalPrecision).toInt() / DecimalPrecision).toString()

private const val LoadMoreThreshold = 5

private const val DecimalPrecision = 10.0
