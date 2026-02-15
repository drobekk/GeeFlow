package dev.drobek.geeflow.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.KeyboardActionHandler
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextFieldLabelScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

@Composable
fun GeeFlowOutlinedTextField(
    state: TextFieldState,
    modifier: Modifier = Modifier,
    shape: Shape = CircleShape,
    contentPadding: PaddingValues = PaddingValues(vertical = 16.dp, horizontal = 24.dp),
    label: @Composable (TextFieldLabelScope.() -> Unit)? = null,
    enabled: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    onKeyboardAction: KeyboardActionHandler? = null,
) = OutlinedTextField(
    state = state,
    modifier = modifier,
    label = label,
    enabled = enabled,
    contentPadding = contentPadding,
    shape = shape,
    keyboardOptions = keyboardOptions,
    onKeyboardAction = onKeyboardAction
)
