package dev.drobek.geeflow.data.user

import dev.drobek.geeflow.data.db.DataDbModule
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module

@Module(includes = [DataDbModule::class])
@ComponentScan("dev.drobek.geeflow.data.user")
class DataUserModule
