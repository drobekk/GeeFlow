package dev.drobek.geeflow.platform

import android.content.Context
import dev.drobek.geeflow.core.presentation.Platform
import dev.drobek.geeflow.core.presentation.Platform.Type.Android
import org.koin.core.annotation.Single

@Single
class AndroidPlatform(val context: Context) : Platform {
    override val type: Platform.Type = Android
}
