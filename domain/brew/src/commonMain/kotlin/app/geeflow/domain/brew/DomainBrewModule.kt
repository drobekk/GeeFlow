package app.geeflow.domain.brew

import app.geeflow.data.brew.DataBrewModule
import app.geeflow.domain.device.DomainDeviceModule
import app.geeflow.domain.user.DomainUserModule
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module

@Module(includes = [DataBrewModule::class, DomainUserModule::class, DomainDeviceModule::class])
@ComponentScan("app.geeflow.domain.brew")
class DomainBrewModule
