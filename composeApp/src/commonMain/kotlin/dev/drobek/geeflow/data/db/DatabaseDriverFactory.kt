package dev.drobek.geeflow.data.db

import app.cash.sqldelight.db.SqlDriver
import dev.drobek.geeflow.AppDatabase

interface DatabaseDriverFactory {
    fun createDriver(): SqlDriver
}

