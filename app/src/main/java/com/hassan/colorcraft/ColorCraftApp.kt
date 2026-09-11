package com.hassan.colorcraft

import android.app.Application
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.hassan.colorcraft.di.appModule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings_prefs")

val DARK_MODE_KEY = booleanPreferencesKey("dark_mode_enabled")

class ColorCraftApp : Application() {

    override fun onCreate() {
        super.onCreate()

        runBlocking {
            val prefs = applicationContext.settingsDataStore.data.first()
            val storedIsDark = prefs[DARK_MODE_KEY]
            if (storedIsDark != null) {
                AppCompatDelegate.setDefaultNightMode(
                    if (storedIsDark) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
                )
            }
        }

        startKoin {
            androidContext(this@ColorCraftApp)
            modules(appModule)
        }
    }
}
