package dev.drobek.geeflow

import dev.bluefalcon.BlueFalcon
import dev.drobek.geeflow.data.db.DatabaseDriverFactory
import dev.drobek.geeflow.data.db.NativeDatabaseDriverFactory
import org.koin.core.module.Module
import org.koin.dsl.module
import platform.UIKit.UIApplication

actual val platformModule: Module = module {
    single<DatabaseDriverFactory> { NativeDatabaseDriverFactory() }
    single { BlueFalcon(null, UIApplication.sharedApplication) }
}
