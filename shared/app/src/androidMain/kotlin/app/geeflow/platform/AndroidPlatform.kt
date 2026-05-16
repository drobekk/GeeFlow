package app.geeflow.platform

import android.content.Context
import app.geeflow.core.presentation.Platform
import app.geeflow.core.presentation.Platform.Type.Android
import org.koin.core.annotation.Single

@Single
class AndroidPlatform(val context: Context) : Platform {
    override val type: Platform.Type = Android
}
