package app.geeflow.presentation.feature.user.list

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.plus
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewWrapper
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.geeflow.navigation.Navigator
import app.geeflow.navigation.NavigatorEffect
import app.geeflow.ui.components.GeeFlowScaffold
import app.geeflow.ui.components.GeeFlowUserAvatar
import app.geeflow.ui.modifier.geeFlowInsetsEndPadding
import app.geeflow.ui.theme.GeeFlowPreviewWrapper
import app.geeflow.ui.theme.GeeFlowScreenPreview
import app.geeflow.ui.theme.GeeFlowTheme
import geeflow.feature.user.list.generated.resources.Res
import geeflow.feature.user.list.generated.resources.user_list_screen_add_profile
import geeflow.feature.user.list.generated.resources.user_list_screen_empty
import geeflow.feature.user.list.generated.resources.user_list_screen_subtitle
import geeflow.feature.user.list.generated.resources.user_list_screen_title
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun UserListScreen(viewModel: UserListViewModel, navigator: Navigator) {
    val viewState by viewModel.viewState.collectAsStateWithLifecycle()

    UserListContent(
        viewState = viewState,
        onEvent = viewModel::handleEvent,
    )

    NavigatorEffect(navigator, viewModel.navEvent)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UserListContent(
    viewState: UserListViewState,
    onEvent: (UserListEvent) -> Unit = {},
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    GeeFlowScaffold(
        title = stringResource(Res.string.user_list_screen_title),
        subtitle = stringResource(Res.string.user_list_screen_subtitle),
        scrollBehavior = scrollBehavior,
        navIconPainter = rememberVectorPainter(Icons.AutoMirrored.Filled.ArrowBack),
        navIconClick = { onEvent(UserListEvent.BackClicked) },
        floatingActionButton = {
            FloatingActionButton(
                modifier = Modifier
                    .padding(
                        horizontal = GeeFlowTheme.spacing.fabHorizontal,
                        vertical = GeeFlowTheme.spacing.fabVertical,
                    )
                    .geeFlowInsetsEndPadding(),
                onClick = { onEvent(UserListEvent.AddClicked) },
            ) {
                Icon(
                    painter = rememberVectorPainter(Icons.Filled.Add),
                    contentDescription = stringResource(Res.string.user_list_screen_add_profile),
                )
            }
        },
        content = {
            UserList(
                users = viewState.users,
                onEvent = onEvent,
                contentPadding = it,
                modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            )
        },
    )
}

@Composable
private fun UserList(
    users: List<UserListViewState.User>,
    onEvent: (UserListEvent) -> Unit,
    contentPadding: PaddingValues = PaddingValues(),
    modifier: Modifier = Modifier,
) = LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = contentPadding + PaddingValues(
        horizontal = GeeFlowTheme.spacing.contentHorizontal,
        vertical = GeeFlowTheme.spacing.contentVertical,
    ),
    verticalArrangement = Arrangement.spacedBy(16.dp),
) {
    if (users.isEmpty()) {
        item { ListEmptyItem(modifier = Modifier.fillParentMaxSize()) }
    }
    items(users) { UserItem(user = it, onEvent = onEvent) }
    item { Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars)) }
}

@Composable
private fun UserItem(
    user: UserListViewState.User,
    onEvent: (UserListEvent) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .clickable(onClick = { onEvent(UserListEvent.UserClicked(user)) })
            .padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        GeeFlowUserAvatar(
            photoFileName = user.photoFileName,
            modifier = Modifier.size(40.dp),
        )
        Text(
            text = user.name,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        if (user.selected) {
            Icon(
                painter = rememberVectorPainter(Icons.Filled.Check),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun ListEmptyItem(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(Res.string.user_list_screen_empty),
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.labelLarge,
        textAlign = TextAlign.Center,
    )
}

@PreviewWrapper(GeeFlowPreviewWrapper::class)
@Composable
@GeeFlowScreenPreview
private fun Preview() {
    UserListContent(
        UserListViewState(
            users = listOf(
                UserListViewState.User(id = 1L, name = "Barista", selected = true, photoFileName = null),
                UserListViewState.User(id = 2L, name = "Guest", selected = false, photoFileName = null),
            ),
        ),
    )
}
