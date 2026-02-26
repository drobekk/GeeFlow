package dev.drobek.geeflow.app

import dev.drobek.geeflow.platformModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.KoinApplication
import org.koin.core.annotation.Module
import org.koin.core.annotation.Singleton
import org.koin.plugin.module.dsl.koinConfiguration
import kotlin.coroutines.CoroutineContext

@KoinApplication
class GeeFlowApp

@Module
@Configuration
@ComponentScan("dev.drobek.geeflow")
class AppModule

@Singleton
class AppCoroutineScope : CoroutineScope {
    override val coroutineContext: CoroutineContext = SupervisorJob() + Dispatchers.Default
}


fun koinConfiguration() = koinConfiguration<GeeFlowApp> {
    modules(platformModule)
}
