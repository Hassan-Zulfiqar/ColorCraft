package com.hassan.colorcraft.ui.splash

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.hassan.colorcraft.data.db.dao.ColoringPageDao
import com.hassan.colorcraft.data.seed.DatabasePrepopulator
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashViewModel(
    application: Application,
    private val databasePrepopulator: DatabasePrepopulator,
    private val coloringPageDao: ColoringPageDao
) : AndroidViewModel(application) {

    private val _isReadyToNavigate = MutableLiveData(false)
    val isReadyToNavigate: LiveData<Boolean> = _isReadyToNavigate

    init {
        viewModelScope.launch {
            coroutineScope {
                val minDisplayTime = async { delay(1500) }
                val seedDatabase = async { databasePrepopulator.seedIfNeeded(getApplication(), coloringPageDao) }
                minDisplayTime.await()
                seedDatabase.await()
            }
            _isReadyToNavigate.value = true
        }
    }
}
