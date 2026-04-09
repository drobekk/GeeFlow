package dev.drobek.geeflow.data.device

import dev.drobek.geeflow.data.brew.DataBrewModule
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module

@Module(includes = [DataBrewModule::class])
@ComponentScan("dev.drobek.geeflow.data.device")
class DataDeviceModule
