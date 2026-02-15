package dev.drobek.geeflow.platform

import org.koin.core.annotation.Single

@Single
class IOSPlatform : Platform {
    override val type: Platform.Type = Platform.Type.IOS
}
