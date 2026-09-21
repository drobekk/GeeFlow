package app.geeflow.ui.components

internal fun String.extractFloat(): Float? =
    toFloatOrNull() ?: filter { it.isDigit() || it == '.' || it == '-' }.toFloatOrNull()
