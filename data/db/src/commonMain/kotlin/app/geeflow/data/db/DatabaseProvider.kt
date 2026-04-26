package app.geeflow.data.db

import org.koin.core.annotation.Singleton

@Singleton
class DatabaseProvider(databaseDriverFactory: DatabaseDriverFactory) {
    val database = AppDatabase(
        driver = databaseDriverFactory.createDriver(),
    )
}
