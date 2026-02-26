package dev.drobek.geeflow

import dev.bluefalcon.ApplicationContext
import dev.bluefalcon.BlueFalcon
import dev.drobek.geeflow.data.db.DatabaseDriverFactory
import dev.drobek.geeflow.data.db.JvmDatabaseDriverFactory
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module = module {
    single<DatabaseDriverFactory> { JvmDatabaseDriverFactory() }
    single {
        // TODO Compile dll https://github.com/Reedyuk/blue-falcon?tab=readme-ov-file#windows
        BlueFalcon(
            log = null,
            context = ApplicationContext(),
            autoDiscoverAllServicesAndCharacteristics = true
        )
    }
}
