package dev.drobek.geeflow.app

import dev.drobek.geeflow.PlatformModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.KoinApplication
import org.koin.core.annotation.Module
import org.koin.core.annotation.Singleton
import kotlin.coroutines.CoroutineContext

@KoinApplication
class GeeFlowApp

@Module(includes = [PlatformModule::class])
@Configuration
@ComponentScan("dev.drobek.geeflow")
class AppModule

@Singleton
class AppCoroutineScope : CoroutineScope {
    override val coroutineContext: CoroutineContext = SupervisorJob() + Dispatchers.Default
}
