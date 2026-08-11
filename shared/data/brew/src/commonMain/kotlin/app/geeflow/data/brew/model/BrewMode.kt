package app.geeflow.data.brew.model

/**
 * How a brew was started: a paddle long press, a stored profile, or the freehand control screen.
 */
enum class BrewMode {
    Manual,
    Profile,
    Freehand,
}
