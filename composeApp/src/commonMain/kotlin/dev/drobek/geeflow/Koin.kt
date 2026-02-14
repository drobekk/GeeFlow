package dev.drobek.geeflow

import dev.drobek.geeflow.data.platformDataModule
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.KoinApplication
import org.koin.core.annotation.Module
import org.koin.plugin.module.dsl.koinConfiguration

import org.koin.ksp.generated.module

@KoinApplication
class GeeFlowApp

@Module
@Configuration
@ComponentScan("dev.drobek.geeflow")
class AppModule

fun koinConfiguration() = koinConfiguration<GeeFlowApp> {
    modules(AppModule().module, platformDataModule)
}
