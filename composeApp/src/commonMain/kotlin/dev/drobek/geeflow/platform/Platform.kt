package dev.drobek.geeflow.platform

interface Platform {
    val type: Type

    enum class Type {
        Desktop, Android, IOS
    }
}
