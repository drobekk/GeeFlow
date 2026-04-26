package app.geeflow.data.user

import app.geeflow.data.db.DataDbModule
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module

@Module(includes = [DataDbModule::class])
@ComponentScan("app.geeflow.data.user")
class DataUserModule
