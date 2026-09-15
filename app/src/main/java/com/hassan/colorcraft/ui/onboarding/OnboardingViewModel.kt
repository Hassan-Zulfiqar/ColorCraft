package com.hassan.colorcraft.ui.onboarding

import android.app.Application
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hassan.colorcraft.settingsDataStore
import kotlinx.coroutines.launch

private val HAS_SEEN_ONBOARDING_KEY = booleanPreferencesKey("has_seen_onboarding")

class OnboardingViewModel(
    application: Application
) : AndroidViewModel(application) {

    fun markOnboardingComplete() {
        viewModelScope.launch {
            getApplication<Application>().settingsDataStore.edit { it[HAS_SEEN_ONBOARDING_KEY] = true }
        }
    }
}
