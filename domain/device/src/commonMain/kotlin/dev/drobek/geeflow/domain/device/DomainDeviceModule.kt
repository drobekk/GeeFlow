package dev.drobek.geeflow.domain.device

import dev.drobek.geeflow.data.device.DataDeviceModule
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module

@Module(includes = [DataDeviceModule::class])
@ComponentScan("dev.drobek.geeflow.domain.device")
class DomainDeviceModule
