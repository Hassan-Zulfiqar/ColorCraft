package com.hassan.colorcraft.ui.drawing

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.hassan.colorcraft.data.db.entity.SketchEntity
import com.hassan.colorcraft.data.repository.DrawingRepository
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

class DrawingViewModel(
    application: Application,
    private val repository: DrawingRepository
) : AndroidViewModel(application) {

    val swatchColors: List<Int> = listOf(
        "#EF4444", "#F97316", "#F59E0B", "#EAB308", "#84CC16", "#22C55E",
        "#14B8A6", "#06B6D4", "#3B82F6", "#6366F1", "#8B5CF6", "#EC4899",
        "#92400E", "#1C1B1F"
    ).map { Color.parseColor(it) }

    private val _canvasBitmap = MutableLiveData<Bitmap?>(null)
    val canvasBitmap: LiveData<Bitmap?> = _canvasBitmap

    private val _currentColor = MutableLiveData(swatchColors.first())
    val currentColor: LiveData<Int> = _currentColor

    private val _sketchLoadFailed = MutableLiveData<Unit>()
    val sketchLoadFailed: LiveData<Unit> = _sketchLoadFailed

    private val _currentTitle = MutableLiveData<String>()
    val currentTitle: LiveData<String> = _currentTitle

    fun loadExistingSketch(sketchId: Long) {
        viewModelScope.launch {
            val sketch = repository.getSketchById(sketchId)
            if (sketch != null && File(sketch.filePath).exists()) {
                val decoded = BitmapFactory.decodeFile(sketch.filePath)
                _canvasBitmap.postValue(decoded)
                _currentTitle.postValue(sketch.title)
            } else {
                _sketchLoadFailed.postValue(Unit)
            }
        }
    }

    fun selectColor(color: Int) {
        _currentColor.value = color
    }

    fun saveSketch(bitmap: Bitmap, existingSketchId: Long?, title: String, onSaved: (Long) -> Unit) {
        viewModelScope.launch {
            val sketchesDir = File(getApplication<Application>().filesDir, "sketches")
            if (!sketchesDir.exists()) {
                sketchesDir.mkdirs()
            }

            val existing = existingSketchId?.let { repository.getSketchById(it) }

            val resultId = if (existing != null) {
                val file = File(existing.filePath)
                FileOutputStream(file).use { stream ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                }
                repository.updateSketch(
                    existing.copy(title = title, updatedAt = System.currentTimeMillis())
                )
                existing.id
            } else {
                val file = File(sketchesDir, "sketch_${System.currentTimeMillis()}.png")
                FileOutputStream(file).use { stream ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                }
                val now = System.currentTimeMillis()
                repository.insertSketch(
                    SketchEntity(
                        filePath = file.absolutePath,
                        title = title,
                        createdAt = now,
                        updatedAt = now
                    )
                )
            }

            onSaved(resultId)
        }
    }
}
