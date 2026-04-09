package dev.drobek.geeflow.domain.user

import dev.drobek.geeflow.data.user.DataUserModule
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module

@Module(includes = [DataUserModule::class])
@ComponentScan("dev.drobek.geeflow.domain.user")
class DomainUserModule
