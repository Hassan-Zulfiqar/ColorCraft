package com.hassan.colorcraft.ui.common

import android.app.Application
import android.content.Context
import android.graphics.Color
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private val Context.colorPickerDataStore: DataStore<Preferences> by preferencesDataStore(name = "color_picker_prefs")

private val RECENT_COLORS_KEY = stringPreferencesKey("recent_colors")

class ColorPickerViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val _recentColors = MutableLiveData<List<Int>>(emptyList())
    val recentColors: LiveData<List<Int>> = _recentColors

    init {
        viewModelScope.launch {
            getApplication<Application>().colorPickerDataStore.data.collect { preferences ->
                val encoded = preferences[RECENT_COLORS_KEY] ?: ""
                _recentColors.postValue(decodeColors(encoded))
            }
        }
    }

    suspend fun addRecentColor(color: Int) {
        val preferences = getApplication<Application>().colorPickerDataStore.data.first()
        val encoded = preferences[RECENT_COLORS_KEY] ?: ""
        val currentColors = decodeColors(encoded).toMutableList()

        currentColors.remove(color)
        currentColors.add(0, color)

        val truncated = currentColors.take(MAX_RECENT_COLORS)
        val newEncoded = truncated.joinToString(",") { encodeColor(it) }

        getApplication<Application>().colorPickerDataStore.edit { prefs ->
            prefs[RECENT_COLORS_KEY] = newEncoded
        }
    }

    private fun decodeColors(encoded: String): List<Int> {
        return encoded.split(",")
            .filter { it.isNotBlank() }
            .mapNotNull { hex ->
                try {
                    Color.parseColor(hex)
                } catch (e: IllegalArgumentException) {
                    null
                }
            }
    }

    private fun encodeColor(color: Int): String = "#%08X".format(color)

    companion object {
        private const val MAX_RECENT_COLORS = 10
    }
}
