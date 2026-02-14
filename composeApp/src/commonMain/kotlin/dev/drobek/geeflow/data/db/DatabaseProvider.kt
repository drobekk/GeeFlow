package dev.drobek.geeflow.data.db

import dev.drobek.geeflow.AppDatabase
import org.koin.core.annotation.Singleton

@Singleton
class DatabaseProvider(databaseDriverFactory: DatabaseDriverFactory) {
    val database = AppDatabase(databaseDriverFactory.createDriver())
}
