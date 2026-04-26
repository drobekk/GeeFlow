package app.geeflow.app

import app.geeflow.PlatformModule
import app.geeflow.data.brew.DataBrewModule
import app.geeflow.data.db.DataDbModule
import app.geeflow.data.device.DataDeviceModule
import app.geeflow.data.user.DataUserModule
import app.geeflow.domain.brew.DomainBrewModule
import app.geeflow.domain.device.DomainDeviceModule
import app.geeflow.domain.user.DomainUserModule
import app.geeflow.presentation.feature.device.add.FeatureDeviceAddModule
import app.geeflow.presentation.feature.device.dashboard.FeatureDeviceDashboardModule
import app.geeflow.presentation.feature.device.list.FeatureDeviceListModule
import app.geeflow.presentation.feature.device.settings.FeatureDeviceSettingsModule
import app.geeflow.presentation.feature.intro.FeatureIntroModule
import app.geeflow.presentation.feature.user.list.FeatureUserListModule
import app.geeflow.presentation.feature.user.settings.FeatureUserSettingsModule
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
        FeatureUserSettingsModule::class,
        FeatureUserListModule::class,
    ],
)
@Configuration
@ComponentScan("app.geeflow")
class AppModule

@Suppress("InjectDispatcher")
@Singleton
class AppCoroutineScope : CoroutineScope {
    override val coroutineContext: CoroutineContext = SupervisorJob() + Dispatchers.Default
}
