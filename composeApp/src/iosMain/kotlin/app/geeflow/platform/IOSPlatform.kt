package app.geeflow.platform

import app.geeflow.core.presentation.Platform
import org.koin.core.annotation.Single

@Single
class IOSPlatform : Platform {
    override val type: Platform.Type = Platform.Type.IOS
}
