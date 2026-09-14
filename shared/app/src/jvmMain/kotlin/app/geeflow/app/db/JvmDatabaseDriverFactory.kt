package app.geeflow.app.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.io.File
import java.util.Properties

class JvmDatabaseDriverFactory : DatabaseDriverFactory {
    override fun createDriver(): SqlDriver {
        val appData = System.getenv("APPDATA")?.let {
            File(it, "GeeFlow")
        } ?: File(System.getProperty("user.home"), ".geeflow")

        if (!appData.exists()) appData.mkdirs()

        val dbFile = File(appData, "app.db")
        return JdbcSqliteDriver("jdbc:sqlite:${dbFile.absolutePath}", Properties(), AppDatabase.Schema)
    }
}
