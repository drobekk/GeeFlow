package app.geeflow.data.device

import app.geeflow.data.brew.DataBrewModule
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module

@Module(includes = [DataBrewModule::class])
@ComponentScan("app.geeflow.data.device")
class DataDeviceModule
