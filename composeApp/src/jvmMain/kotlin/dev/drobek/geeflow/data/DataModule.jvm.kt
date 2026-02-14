package dev.drobek.geeflow.data

import dev.drobek.geeflow.data.db.DatabaseDriverFactory
import dev.drobek.geeflow.data.db.JvmDatabaseDriverFactory
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformDataModule: Module = module {
    single<DatabaseDriverFactory> { JvmDatabaseDriverFactory() }
}
