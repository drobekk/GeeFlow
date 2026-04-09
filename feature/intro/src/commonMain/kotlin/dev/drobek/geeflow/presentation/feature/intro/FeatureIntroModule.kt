package dev.drobek.geeflow.presentation.feature.intro

import dev.drobek.geeflow.domain.user.DomainUserModule
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module

@Module(includes = [DomainUserModule::class])
@ComponentScan("dev.drobek.geeflow.presentation.feature.intro")
class FeatureIntroModule
