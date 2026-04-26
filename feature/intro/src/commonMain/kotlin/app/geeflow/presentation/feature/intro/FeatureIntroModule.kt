package app.geeflow.presentation.feature.intro

import app.geeflow.domain.user.DomainUserModule
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module

@Module(includes = [DomainUserModule::class])
@ComponentScan("app.geeflow.presentation.feature.intro")
class FeatureIntroModule
