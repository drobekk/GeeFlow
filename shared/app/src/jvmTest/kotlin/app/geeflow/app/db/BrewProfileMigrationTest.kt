package app.geeflow.app.db

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlin.test.Test
import kotlin.test.assertEquals

class BrewProfileMigrationTest {

    @Test
    fun `when version one profiles migrate then Single Dose settings receive defaults`() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        driver.use { driver ->
            driver.execute(null, "CREATE TABLE brew_profiles (id INTEGER PRIMARY KEY, name TEXT NOT NULL)", 0)
            driver.execute(null, "INSERT INTO brew_profiles (id, name) VALUES (1, 'Espresso')", 0)

            AppDatabase.Schema.migrate(driver, oldVersion = 1, newVersion = 2)
            val settings = driver.executeQuery(
                null,
                "SELECT singleDoseEnabled, singleDoseGrindingSize, singleDoseGrindingSpeed FROM brew_profiles WHERE id = 1",
                { cursor ->
                    cursor.next()
                    QueryResult.Value(
                        Triple(cursor.getLong(0), cursor.getLong(1), cursor.getLong(2)),
                    )
                },
                0,
            ).value

            assertEquals(Triple(0L, 300L, 500L), settings)
        }
    }
}
