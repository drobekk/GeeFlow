package app.geeflow.presentation.feature.user.settings.main

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.plus
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.PreviewWrapper
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.geeflow.navigation.Navigator
import app.geeflow.navigation.NavigatorEffect
import app.geeflow.presentation.feature.user.settings.AboutSettings
import app.geeflow.presentation.feature.user.settings.AppearanceSettings
import app.geeflow.presentation.feature.user.settings.BrewingPreferencesSettings
import app.geeflow.presentation.feature.user.settings.ProfileSettings
import app.geeflow.presentation.feature.user.settings.main.UserSettingsEvent.BackClicked
import app.geeflow.presentation.feature.user.settings.main.UserSettingsEvent.ChangeUserClicked
import app.geeflow.presentation.feature.user.settings.main.UserSettingsEvent.ItemClicked
import app.geeflow.presentation.feature.user.settings.main.UserSettingsViewState.Item
import app.geeflow.ui.components.GeeFlowListScaffold
import app.geeflow.ui.components.isListDetailExpanded
import app.geeflow.ui.theme.GeeFlowPreviewWrapper
import app.geeflow.ui.theme.GeeFlowScreenPreview
import app.geeflow.ui.theme.GeeFlowTheme
import geeflow.shared.feature.user.settings.generated.resources.Res
import geeflow.shared.feature.user.settings.generated.resources.user_settings_about
import geeflow.shared.feature.user.settings.generated.resources.user_settings_about_description
import geeflow.shared.feature.user.settings.generated.resources.user_settings_appearance
import geeflow.shared.feature.user.settings.generated.resources.user_settings_appearance_description
import geeflow.shared.feature.user.settings.generated.resources.user_settings_brewing_preferences
import geeflow.shared.feature.user.settings.generated.resources.user_settings_brewing_preferences_description
import geeflow.shared.feature.user.settings.generated.resources.user_settings_change
import geeflow.shared.feature.user.settings.generated.resources.user_settings_profile
import geeflow.shared.feature.user.settings.generated.resources.user_settings_profile_description
import geeflow.shared.feature.user.settings.generated.resources.user_settings_subtitle
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun UserSettingsScreen(
    viewModel: UserSettingsViewModel,
    navigator: Navigator,
) {
    val viewState by viewModel.viewState.collectAsStateWithLifecycle()
    val selectedIndex = when (navigator.getCurrentDestination()) {
        is ProfileSettings -> viewState.items.indexOfFirst { it is Item.Profile }
        is BrewingPreferencesSettings -> viewState.items.indexOfFirst { it is Item.BrewingPreferences }
        is AppearanceSettings -> viewState.items.indexOfFirst { it is Item.AppearanceDisplay }
        is AboutSettings -> viewState.items.indexOfFirst { it is Item.About }
        else -> null
    }.takeIf { it != null && it >= 0 }

    NavigatorEffect(navigator, viewModel.navEvent)

    Content(
        viewState = viewState,
        selectedIndex = selectedIndex,
        onEvent = viewModel::handleEvent,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Content(
    viewState: UserSettingsViewState,
    selectedIndex: Int? = null,
    onEvent: (UserSettingsEvent) -> Unit = {},
) {
    val expanded = isListDetailExpanded()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val listState = rememberLazyListState()
    GeeFlowListScaffold(
        title = viewState.userName,
        subtitle = stringResource(Res.string.user_settings_subtitle),
        navIconClick = { onEvent(BackClicked) },
        expanded = expanded,
        scrollBehavior = scrollBehavior,
        modifier = Modifier,
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = padding + PaddingValues(
                horizontal = if (expanded) 24.dp else GeeFlowTheme.spacing.contentHorizontal,
                vertical = if (expanded) 8.dp else GeeFlowTheme.spacing.contentVertical,
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            itemsIndexed(viewState.items) { index, item ->
                val selected = expanded && index == selectedIndex
                SettingsItem(
                    item = item,
                    onEvent = onEvent,
                    backgroundColor = if (expanded) {
                        MaterialTheme.colorScheme.background
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerLow
                    },
                    modifier = Modifier
                        .then(
                            if (selected) {
                                Modifier.border(2.dp, MaterialTheme.colorScheme.primary, MaterialTheme.shapes.large)
                            } else {
                                Modifier
                            },
                        )
                        .clip(MaterialTheme.shapes.large),
                )
            }
        }
    }
}

@Composable
private fun SettingsItem(
    item: Item,
    onEvent: (UserSettingsEvent) -> Unit,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .background(backgroundColor)
            .fillMaxWidth()
            .clickable(onClick = { onEvent(ItemClicked(item)) })
            .padding(start = 24.dp, top = 16.dp, bottom = 16.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.titleMedium,
                color = contentColor,
            )
            Text(
                text = item.description,
                style = MaterialTheme.typography.labelMedium,
                color = contentColor,
            )
        }
        if (item is Item.Profile) {
            TextButton(
                onClick = { onEvent(ChangeUserClicked) },
                colors = ButtonDefaults.textButtonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary,
                ),
            ) {
                Text(text = stringResource(Res.string.user_settings_change))
            }
        }
    }
}

@Composable
private fun previewViewState() = UserSettingsViewState(
    userName = "Barista",
    items = listOf(
        Item.Profile(
            stringResource(Res.string.user_settings_profile),
            stringResource(Res.string.user_settings_profile_description),
        ),
        Item.BrewingPreferences(
            stringResource(Res.string.user_settings_brewing_preferences),
            stringResource(Res.string.user_settings_brewing_preferences_description),
        ),
        Item.AppearanceDisplay(
            stringResource(Res.string.user_settings_appearance),
            stringResource(Res.string.user_settings_appearance_description),
        ),
        Item.About(
            stringResource(Res.string.user_settings_about),
            stringResource(Res.string.user_settings_about_description),
        ),
    ),
)

@PreviewWrapper(GeeFlowPreviewWrapper::class)
@Composable
@GeeFlowScreenPreview
private fun Preview() {
    Content(previewViewState(), selectedIndex = 2)
}
