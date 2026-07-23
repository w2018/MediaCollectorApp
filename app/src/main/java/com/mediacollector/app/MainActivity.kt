package com.mediacollector.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.mediacollector.app.data.settings.DarkMode
import com.mediacollector.app.data.settings.SettingsStore
import com.mediacollector.app.ui.navigation.MainApp
import com.mediacollector.app.ui.theme.MediaCollectorTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var settingsStore: SettingsStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val dmState = settingsStore.darkMode.collectAsState(initial = DarkMode.SYSTEM)
            val darkMode = dmState.value
            val systemDark = isSystemInDarkTheme()

            val useDarkTheme = when (darkMode) {
                DarkMode.SYSTEM -> systemDark
                DarkMode.DARK -> true
                DarkMode.LIGHT -> false
            }

            MediaCollectorTheme(darkTheme = useDarkTheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainApp()
                }
            }
        }
    }
}
