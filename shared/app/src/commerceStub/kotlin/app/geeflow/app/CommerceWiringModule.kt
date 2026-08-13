package app.geeflow.app

import app.geeflow.commerce.NoOpCommerceModule
import org.koin.core.annotation.Module

@Module(includes = [NoOpCommerceModule::class])
class CommerceWiringModule
