package app.geeflow.presentation.feature.device.dashboard.main

/** Move explicitly marked source/implementation sentences to details without discarding text. */
internal fun recipeDescriptionSections(description: String): Pair<String, String> {
    val source = Regex("""(?i)\bsource:\s*""").find(description)
    val body = source?.let { description.substring(0, it.range.first) } ?: description
    val sourceText = source?.let { description.substring(it.range.first).trim() }.orEmpty()
    val technical = Regex(
        """(?i)\b(telemetry|implementation|headroom|does not specify|not a copied|not copied|unsupported feature|source mapping|https?://)\b""",
    )
    val primary = mutableListOf<String>()
    val details = mutableListOf<String>()
    body.trim().split(Regex("""(?<=[.!?])\s+""")).filter { it.isNotBlank() }.forEach { sentence ->
        (if (technical.containsMatchIn(sentence)) details else primary).add(sentence)
    }
    if (sourceText.isNotEmpty()) details.add(sourceText)
    return primary.joinToString(" ") to details.joinToString("\n\n")
}
