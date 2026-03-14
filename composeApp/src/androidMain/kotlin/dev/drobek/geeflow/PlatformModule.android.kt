package dev.drobek.geeflow

import android.util.Log
import dev.bluefalcon.BlueFalcon
import dev.bluefalcon.Logger
import dev.drobek.geeflow.data.db.AndroidDatabaseDriverFactory
import dev.drobek.geeflow.data.db.DatabaseDriverFactory
import dev.drobek.geeflow.data.users.impl.createAndroidDataStore
import org.koin.android.ext.koin.androidApplication
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module = module {
    single<DatabaseDriverFactory> { AndroidDatabaseDriverFactory(get()) }
    single { createAndroidDataStore(androidApplication()) }
    single {
        BlueFalcon(context = androidApplication(), log = object : Logger {
            override fun error(message: String, cause: Throwable?) {
                Log.e("BlueFalcon", message, cause)
            }

            override fun warn(message: String, cause: Throwable?) {
                Log.w("BlueFalcon", message, cause)
            }

            override fun info(message: String, cause: Throwable?) {
                Log.i("BlueFalcon", message, cause)
            }

            override fun debug(message: String, cause: Throwable?) {
                Log.d("BlueFalcon", message, cause)
            }
        }
        )
    }
}
