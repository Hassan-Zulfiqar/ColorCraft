package com.hassan.colorcraft.ui.drawing

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hassan.colorcraft.data.db.entity.SketchEntity
import com.hassan.colorcraft.data.repository.DrawingRepository
import kotlinx.coroutines.launch
import java.io.File

class DrawingGalleryViewModel(
    private val repository: DrawingRepository
) : ViewModel() {

    private val _sketches = MutableLiveData<List<SketchEntity>>()
    val sketches: LiveData<List<SketchEntity>> = _sketches

    init {
        viewModelScope.launch {
            repository.getAllSketches().collect { list ->
                _sketches.postValue(list)
            }
        }
    }

    fun deleteSketch(sketch: SketchEntity) {
        viewModelScope.launch {
            repository.deleteSketch(sketch)
            try {
                File(sketch.filePath).delete()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete sketch file: ${sketch.filePath}", e)
            }
        }
    }

    companion object {
        private const val TAG = "DrawingGalleryViewModel"
    }
}
