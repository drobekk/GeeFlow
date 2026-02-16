package dev.drobek.geeflow.navigation

interface Navigation {
    fun back()
    fun clearBackStack()

    val isAtRoot: Boolean
}
