package dev.drobek.geeflow.core.presentation

interface Platform {
    val type: Type

    enum class Type {
        Desktop, Android, IOS
    }
}
