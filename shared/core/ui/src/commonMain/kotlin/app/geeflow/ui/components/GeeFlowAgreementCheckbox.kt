package app.geeflow.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.tooling.preview.PreviewWrapper
import app.geeflow.ui.GeeFlowUrls
import app.geeflow.ui.theme.GeeFlowComponentPreview
import app.geeflow.ui.theme.GeeFlowPreviewWrapper
import geeflow.shared.core.ui.generated.resources.Res
import geeflow.shared.core.ui.generated.resources.agreement_and
import geeflow.shared.core.ui.generated.resources.agreement_prefix
import geeflow.shared.core.ui.generated.resources.agreement_privacy
import geeflow.shared.core.ui.generated.resources.agreement_terms
import org.jetbrains.compose.resources.stringResource

@Composable
fun GeeFlowAgreementCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val linkStyle = TextLinkStyles(
        style = SpanStyle(
            color = MaterialTheme.colorScheme.primary,
            textDecoration = TextDecoration.Underline,
        ),
    )
    val annotatedText = buildAnnotatedString {
        append(stringResource(Res.string.agreement_prefix))
        withLink(LinkAnnotation.Url(GeeFlowUrls.TermsAndConditions, linkStyle)) {
            append(stringResource(Res.string.agreement_terms))
        }
        append(stringResource(Res.string.agreement_and))
        withLink(LinkAnnotation.Url(GeeFlowUrls.PrivacyPolicy, linkStyle)) {
            append(stringResource(Res.string.agreement_privacy))
        }
    }

    Row(
        modifier = modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = { onCheckedChange(!checked) },
        ),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
        Text(
            text = annotatedText,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@PreviewWrapper(GeeFlowPreviewWrapper::class)
@Composable
@GeeFlowComponentPreview
private fun Preview() {
    GeeFlowAgreementCheckbox(
        checked = true,
        onCheckedChange = {},
    )
}
