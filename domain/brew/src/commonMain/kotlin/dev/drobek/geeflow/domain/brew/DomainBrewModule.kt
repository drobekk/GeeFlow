package dev.drobek.geeflow.domain.brew

import dev.drobek.geeflow.data.brew.DataBrewModule
import dev.drobek.geeflow.domain.device.DomainDeviceModule
import dev.drobek.geeflow.domain.user.DomainUserModule
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module

@Module(includes = [DataBrewModule::class, DomainUserModule::class, DomainDeviceModule::class])
@ComponentScan("dev.drobek.geeflow.domain.brew")
class DomainBrewModule
