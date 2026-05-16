package app.geeflow.domain.user

import app.geeflow.data.user.DataUserModule
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module

@Module(includes = [DataUserModule::class])
@ComponentScan("app.geeflow.domain.user")
class DomainUserModule
