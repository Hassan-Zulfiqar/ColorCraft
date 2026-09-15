package com.hassan.colorcraft.ui.splash

import android.app.Application
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.hassan.colorcraft.data.db.dao.ColoringPageDao
import com.hassan.colorcraft.data.seed.DatabasePrepopulator
import com.hassan.colorcraft.settingsDataStore
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private val HAS_SEEN_ONBOARDING_KEY = booleanPreferencesKey("has_seen_onboarding")

class SplashViewModel(
    application: Application,
    private val databasePrepopulator: DatabasePrepopulator,
    private val coloringPageDao: ColoringPageDao
) : AndroidViewModel(application) {

    private val _isReadyToNavigate = MutableLiveData(false)
    val isReadyToNavigate: LiveData<Boolean> = _isReadyToNavigate

    private val _hasSeenOnboarding = MutableLiveData(false)
    val hasSeenOnboarding: LiveData<Boolean> = _hasSeenOnboarding

    init {
        viewModelScope.launch {
            coroutineScope {
                val minDisplayTime = async { delay(1500) }
                val seedDatabase = async { databasePrepopulator.seedIfNeeded(getApplication(), coloringPageDao) }
                minDisplayTime.await()
                seedDatabase.await()
            }
            val prefs = getApplication<Application>().settingsDataStore.data.first()
            _hasSeenOnboarding.value = prefs[HAS_SEEN_ONBOARDING_KEY] ?: false
            _isReadyToNavigate.value = true
        }
    }
}
