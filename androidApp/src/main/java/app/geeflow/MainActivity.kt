package app.geeflow

import android.app.LocaleManager
import android.os.Build
import android.os.Bundle
import android.os.LocaleList
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import app.geeflow.app.App

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyChineseDefault()
        enableEdgeToEdge()

        setContent {
            App { finish() }
        }
    }

    private fun applyChineseDefault() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val preferences = getSharedPreferences("custom_presentation", MODE_PRIVATE)
        if (preferences.getBoolean("chinese_default_applied", false)) return
        preferences.edit().putBoolean("chinese_default_applied", true).apply()
        val manager = getSystemService(LocaleManager::class.java)
        if (manager.applicationLocales.isEmpty) {
            manager.applicationLocales = LocaleList.forLanguageTags("zh-CN")
        }
    }
}
