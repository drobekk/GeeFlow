package dev.drobek.geeflow

import dev.drobek.geeflow.data.db.DatabaseDriverFactory
import dev.drobek.geeflow.data.db.JvmDatabaseDriverFactory
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module = module {
    single<DatabaseDriverFactory> { JvmDatabaseDriverFactory() }
}
