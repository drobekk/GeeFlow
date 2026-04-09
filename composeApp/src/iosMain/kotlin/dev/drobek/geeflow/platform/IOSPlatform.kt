package dev.drobek.geeflow.platform

import dev.drobek.geeflow.core.presentation.Platform
import org.koin.core.annotation.Single

@Single
class IOSPlatform : Platform {
    override val type: Platform.Type = Platform.Type.IOS
}
