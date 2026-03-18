package dev.drobek.geeflow.presentation.feature.device.dashboard.profiles

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.InsertLink
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.unit.dp
import dev.drobek.geeflow.presentation.feature.device.dashboard.getMockProfileListViewState
import dev.drobek.geeflow.presentation.feature.device.dashboard.profiles.ProfileListViewState.Profile
import dev.drobek.geeflow.ui.HorizontalSpacer
import dev.drobek.geeflow.ui.theme.GeeFlowScreenPreview
import dev.drobek.geeflow.ui.theme.GeeFlowTheme
import geeflow.composeapp.generated.resources.Res
import geeflow.composeapp.generated.resources.profile_list_search
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun ProfileList(
    viewState: ProfileListViewState,
    onEvent: (ProfileListEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    ProfileListContent(
        viewState = viewState,
        onEvent = onEvent,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ProfileListContent(
    viewState: ProfileListViewState,
    onEvent: (ProfileListEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var isSearchExpanded by remember { mutableStateOf(false) }

    val topContentPadding by animateDpAsState(if (isSearchExpanded) 78.dp else 0.dp)
    val verticalBias by animateFloatAsState(if (isSearchExpanded) -1.0f else 1.0f)

    val filteredProfiles = remember(viewState.profiles, searchQuery) {
        if (searchQuery.isBlank()) viewState.profiles
        else viewState.profiles.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
                    it.description.contains(searchQuery, ignoreCase = true)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(16.dp))
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = topContentPadding, bottom = 64.dp)
        ) {
            items(filteredProfiles) {
                ProfileItem(
                    profile = it,
                    onProfileClick = { id -> onEvent(ProfileListEvent.ProfileSelected(id)) }
                )
            }
        }
        BottomBar(
            searchQuery = searchQuery,
            searchExpanded = isSearchExpanded,
            onSearchQueryChange = { searchQuery = it },
            onExpandedChange = { isSearchExpanded = it },
            onEvent = onEvent,
            modifier = Modifier.align(BiasAlignment(0.0f, verticalBias))
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun BottomBar(
    searchQuery: String,
    searchExpanded: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onExpandedChange: (Boolean) -> Unit,
    onEvent: (ProfileListEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val topBrush = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.surfaceContainer,
            MaterialTheme.colorScheme.surfaceContainer,
            Color.Transparent
        )
    )
    val bottomBrush = Brush.verticalGradient(
        colors = listOf(
            Color.Transparent,
            MaterialTheme.colorScheme.surfaceContainer,
            MaterialTheme.colorScheme.surfaceContainer
        )
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(brush = if (searchExpanded) topBrush else bottomBrush)
            .padding(16.dp)
            .height(48.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        AnimatedVisibility(
            !searchExpanded,
            enter = expandHorizontally(clip = false) + fadeIn(),
            exit = shrinkHorizontally(clip = false) + fadeOut()
        ) {
            Row(
                modifier = Modifier
                    .padding(end = 16.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest, CircleShape)
                    .padding(horizontal = 6.dp)
            ) {
                ToggleButton(
                    checked = false,
                    shapes = ToggleButtonDefaults.shapes(
                        shape = CircleShape,
                        checkedShape = CircleShape,
                        pressedShape = CircleShape
                    ),
                    onCheckedChange = { onEvent(ProfileListEvent.HistoryClicked) }
                ) {
                    Icon(
                        painter = rememberVectorPainter(Icons.Filled.History),
                        contentDescription = null
                    )
                }
                IconButton(
                    onClick = { onEvent(ProfileListEvent.AddProfileClicked) }
                ) {
                    Icon(
                        painter = rememberVectorPainter(Icons.Filled.Add),
                        contentDescription = null
                    )
                }
            }
        }
        SearchBar(
            searchQuery = searchQuery,
            expanded = searchExpanded,
            onSearchQueryChange = onSearchQueryChange,
            onExpandedChange = onExpandedChange,
            modifier = Modifier.fillMaxHeight()
        )
    }
}


@Composable
private fun SearchBar(
    searchQuery: String,
    expanded: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onExpandedChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(expanded) {
        if (expanded) {
            delay(150)
            focusRequester.requestFocus()
        } else {
            onSearchQueryChange("")
        }
    }

    Row(
        modifier = modifier.background(color = MaterialTheme.colorScheme.background, shape = CircleShape),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        AnimatedVisibility(
            modifier = Modifier.weight(1f, false),
            visible = expanded,
            enter = expandHorizontally(expandFrom = Alignment.End) + fadeIn(),
            exit = shrinkHorizontally(shrinkTowards = Alignment.End) + fadeOut()
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
                    color = MaterialTheme.colorScheme.onSurface
                ),
                singleLine = true,
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = { innerTextField ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = stringResource(Res.string.profile_list_search),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                        innerTextField()
                    }
                }
            )
        }

        AnimatedContent(
            targetState = expanded,
            contentAlignment = Alignment.Center
        ) { expanded ->
            if (expanded) {
                IconButton(
                    modifier = Modifier.aspectRatio(1f),
                    onClick = {
                        onExpandedChange(false)
                        onSearchQueryChange("")
                    }
                ) {
                    Icon(
                        painter = rememberVectorPainter(Icons.Filled.Cancel),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                IconButton(
                    modifier = Modifier.aspectRatio(1f),
                    onClick = { onExpandedChange(true) }
                ) {
                    Icon(
                        painter = rememberVectorPainter(Icons.Filled.Search),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileItem(
    profile: Profile,
    onProfileClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when {
        profile.selected -> MaterialTheme.colorScheme.surfaceContainerHigh
        else -> Color.Transparent
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
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = profile.number,
            style = MaterialTheme.typography.titleSmall,
            color = numberTextColor,
            modifier = Modifier
                .background(numberColor, CircleShape)
                .padding(horizontal = 12.dp, vertical = 6.dp)
        )
        HorizontalSpacer(12.dp)
        Column(Modifier.weight(1f)) {
            Text(
                text = profile.name,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = profile.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        AnimatedVisibility(
            visible = profile.bound,
            modifier = Modifier.padding(start = 8.dp)
        ) {
            Icon(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.tertiary, CircleShape)
                    .padding(4.dp),
                painter = rememberVectorPainter(Icons.Filled.InsertLink),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onTertiary
            )
        }
    }
}

@Composable
@GeeFlowScreenPreview
private fun PreviewLight() = GeeFlowTheme(false) {
    ProfileList(
        viewState = getMockProfileListViewState(),
        onEvent = {},
        modifier = Modifier.padding(16.dp)
    )
}

@Composable
@GeeFlowScreenPreview
private fun PreviewDark() = GeeFlowTheme(true) {
    ProfileList(
        viewState = getMockProfileListViewState(),
        onEvent = {},
        modifier = Modifier.padding(16.dp)
    )
}
