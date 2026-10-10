package app.geeflow.presentation.feature.device.dashboard.profiles

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.InsertLink
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.geeflow.presentation.feature.device.dashboard.profiles.ProfileListViewState.Profile
import app.geeflow.ui.icons.GeeFlowIcon
import app.geeflow.ui.icons.Grinder
import geeflow.shared.feature.device.dashboard.generated.resources.Res
import geeflow.shared.feature.device.dashboard.generated.resources.profile_editor_single_dose_set_grinder
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ProfileBadge(profile: Profile, onGrinderClick: (String) -> Unit) {
    val badge = when {
        profile.selected && profile.singleDoseEnabled -> ProfileBadgeType.Grinder
        profile.bound && profile.selected -> ProfileBadgeType.BoundSelected
        profile.bound -> ProfileBadgeType.Bound
        profile.selected -> ProfileBadgeType.Selected
        else -> ProfileBadgeType.Plain
    }
    val colors = MaterialTheme.colorScheme
    val borderWidth by animateDpAsState(
        targetValue = when (badge) {
            ProfileBadgeType.Grinder -> ProfileBadgeSize / 2
            ProfileBadgeType.Selected, ProfileBadgeType.BoundSelected -> 2.dp
            ProfileBadgeType.Plain, ProfileBadgeType.Bound -> 0.dp
        },
        animationSpec = tween(BadgeTransitionDurationMillis),
        label = "profileBadgeBorderWidth",
    )
    val contentColor by animateColorAsState(
        targetValue = when (badge) {
            ProfileBadgeType.Grinder -> colors.onPrimary
            ProfileBadgeType.Selected, ProfileBadgeType.BoundSelected -> colors.primary
            ProfileBadgeType.Bound -> colors.onSurfaceVariant
            ProfileBadgeType.Plain -> colors.onSurface
        },
        animationSpec = tween(BadgeTransitionDurationMillis),
        label = "profileBadgeContentColor",
    )
    val badgeModifier = Modifier
        .size(ProfileBadgeSize)
        .clip(CircleShape)
        .drawBehind {
            val radius = size.minDimension / 2f
            val strokeWidth = borderWidth.toPx()
            drawCircle(colors.surfaceContainerHigh, radius = radius)
            if (strokeWidth > 0f) {
                drawCircle(
                    color = colors.primary,
                    radius = radius - strokeWidth / 2f,
                    style = Stroke(width = strokeWidth),
                )
            }
        }
        .then(if (badge == ProfileBadgeType.Grinder) Modifier.clickable { onGrinderClick(profile.id) } else Modifier)
    Box(modifier = badgeModifier, contentAlignment = Alignment.Center) {
        val content = when (badge) {
            ProfileBadgeType.Grinder -> ProfileBadgeContent.Grinder
            ProfileBadgeType.Bound, ProfileBadgeType.BoundSelected -> ProfileBadgeContent.Bound
            ProfileBadgeType.Selected, ProfileBadgeType.Plain -> ProfileBadgeContent.Number
        }
        AnimatedContent(
            targetState = content,
            modifier = Modifier.size(ProfileBadgeSize),
            contentAlignment = Alignment.Center,
            transitionSpec = {
                fadeIn(tween(ContentFadeDurationMillis, delayMillis = ContentFadeDurationMillis))
                    .togetherWith(fadeOut(tween(ContentFadeDurationMillis)))
            },
            label = "profileBadgeContent",
        ) { currentContent ->
            Box(modifier = Modifier.size(ProfileBadgeSize), contentAlignment = Alignment.Center) {
                when (currentContent) {
                    ProfileBadgeContent.Grinder -> Icon(
                        imageVector = GeeFlowIcon.Grinder,
                        contentDescription = stringResource(Res.string.profile_editor_single_dose_set_grinder),
                        modifier = Modifier.size(18.dp),
                        tint = contentColor,
                    )
                    ProfileBadgeContent.Bound -> Icon(
                        painter = rememberVectorPainter(Icons.Filled.InsertLink),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = contentColor,
                    )
                    ProfileBadgeContent.Number -> Text(
                        text = profile.number,
                        style = MaterialTheme.typography.titleSmall,
                        color = contentColor,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

private enum class ProfileBadgeType { Grinder, BoundSelected, Bound, Selected, Plain }

private enum class ProfileBadgeContent { Grinder, Bound, Number }

private val ProfileBadgeSize = 32.dp
private const val ContentFadeDurationMillis = 110
private const val BadgeTransitionDurationMillis = ContentFadeDurationMillis * 2
