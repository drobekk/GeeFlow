package dev.drobek.geeflow.app

import dev.drobek.geeflow.PlatformModule
import dev.drobek.geeflow.data.brew.DataBrewModule
import dev.drobek.geeflow.data.db.DataDbModule
import dev.drobek.geeflow.data.device.DataDeviceModule
import dev.drobek.geeflow.data.user.DataUserModule
import dev.drobek.geeflow.domain.brew.DomainBrewModule
import dev.drobek.geeflow.domain.device.DomainDeviceModule
import dev.drobek.geeflow.domain.user.DomainUserModule
import dev.drobek.geeflow.presentation.feature.device.add.FeatureDeviceAddModule
import dev.drobek.geeflow.presentation.feature.device.dashboard.FeatureDeviceDashboardModule
import dev.drobek.geeflow.presentation.feature.device.list.FeatureDeviceListModule
import dev.drobek.geeflow.presentation.feature.device.settings.FeatureDeviceSettingsModule
import dev.drobek.geeflow.presentation.feature.intro.FeatureIntroModule
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

@Module(
    includes = [
        PlatformModule::class,
        DataDbModule::class,
        DataBrewModule::class,
        DataUserModule::class,
        DataDeviceModule::class,
        DomainUserModule::class,
        DomainDeviceModule::class,
        DomainBrewModule::class,
        FeatureDeviceListModule::class,
        FeatureDeviceAddModule::class,
        FeatureDeviceDashboardModule::class,
        FeatureDeviceSettingsModule::class,
        FeatureIntroModule::class,
    ]
)
@Configuration
@ComponentScan("dev.drobek.geeflow")
class AppModule

@Singleton
class AppCoroutineScope : CoroutineScope {
    override val coroutineContext: CoroutineContext = SupervisorJob() + Dispatchers.Default
}
