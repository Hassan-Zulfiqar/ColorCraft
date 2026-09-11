package com.hassan.colorcraft.ui.settings

import android.app.Application
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.hassan.colorcraft.DARK_MODE_KEY
import com.hassan.colorcraft.settingsDataStore
import kotlinx.coroutines.launch

class SettingsViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val _isDarkModeEnabled = MutableLiveData(false)
    val isDarkModeEnabled: LiveData<Boolean> = _isDarkModeEnabled

    init {
        viewModelScope.launch {
            getApplication<Application>().settingsDataStore.data.collect { preferences ->
                val storedIsDark = preferences[DARK_MODE_KEY]
                val effectiveIsDark = storedIsDark ?: run {
                    val uiMode = getApplication<Application>().resources.configuration.uiMode
                    (uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
                }
                _isDarkModeEnabled.postValue(effectiveIsDark)
            }
        }
    }

    fun setDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            getApplication<Application>().settingsDataStore.edit { it[DARK_MODE_KEY] = enabled }
            AppCompatDelegate.setDefaultNightMode(
                if (enabled) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
            )
        }
    }
}
