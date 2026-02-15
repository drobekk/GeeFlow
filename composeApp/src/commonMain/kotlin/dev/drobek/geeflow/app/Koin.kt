package dev.drobek.geeflow.app

import dev.drobek.geeflow.platformModule
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.KoinApplication
import org.koin.core.annotation.Module
import org.koin.plugin.module.dsl.koinConfiguration

@KoinApplication
class GeeFlowApp

@Module
@Configuration
@ComponentScan("dev.drobek.geeflow")
class AppModule

fun koinConfiguration() = koinConfiguration<GeeFlowApp> {
    modules(platformModule)
}
