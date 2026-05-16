package app.geeflow.domain.device

import app.geeflow.data.device.DataDeviceModule
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module

@Module(includes = [DataDeviceModule::class])
@ComponentScan("app.geeflow.domain.device")
class DomainDeviceModule
