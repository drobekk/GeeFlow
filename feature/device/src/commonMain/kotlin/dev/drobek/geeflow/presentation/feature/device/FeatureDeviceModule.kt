package dev.drobek.geeflow.presentation.feature.device

import dev.drobek.geeflow.data.device.DataDeviceModule
import dev.drobek.geeflow.domain.brew.DomainBrewModule
import dev.drobek.geeflow.domain.device.DomainDeviceModule
import dev.drobek.geeflow.domain.user.DomainUserModule
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module

@Module(
    includes = [
        DomainDeviceModule::class,
        DomainBrewModule::class,
        DomainUserModule::class,
        DataDeviceModule::class,
    ]
)
@ComponentScan("dev.drobek.geeflow.presentation.feature.device")
class FeatureDeviceModule
