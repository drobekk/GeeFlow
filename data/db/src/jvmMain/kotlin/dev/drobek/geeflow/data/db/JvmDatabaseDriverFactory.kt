package dev.drobek.geeflow.data.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.util.Properties

class JvmDatabaseDriverFactory : DatabaseDriverFactory {
    override fun createDriver(): SqlDriver = JdbcSqliteDriver("jdbc:sqlite:app.db", Properties(), AppDatabase.Schema)
}
