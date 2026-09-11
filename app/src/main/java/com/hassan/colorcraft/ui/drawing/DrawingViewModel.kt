package com.hassan.colorcraft.ui.drawing

import android.app.Application
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.hassan.colorcraft.R
import com.hassan.colorcraft.data.db.entity.SketchEntity
import com.hassan.colorcraft.data.repository.DrawingRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

    fun updateTitleOnly(sketchId: Long, title: String) {
        viewModelScope.launch {
            repository.updateSketchTitle(sketchId, title, System.currentTimeMillis())
        }
    }

    fun saveSketch(
        bitmap: Bitmap,
        existingSketchId: Long?,
        title: String,
        exportToGallery: Boolean,
        onSaved: (sketchId: Long, shareableUri: Uri?, galleryExportSucceeded: Boolean) -> Unit
    ) {
        viewModelScope.launch {
            val (sketchId, shareableUri, galleryExportSucceeded) = withContext(Dispatchers.IO) {
                val sketchesDir = File(getApplication<Application>().filesDir, "sketches")
                if (!sketchesDir.exists()) {
                    sketchesDir.mkdirs()
                }

                val existing = existingSketchId?.let { repository.getSketchById(it) }

                val (resultId, sketchFile) = if (existing != null) {
                    val file = File(existing.filePath)
                    FileOutputStream(file).use { stream ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                    }
                    repository.updateSketch(
                        existing.copy(title = title, updatedAt = System.currentTimeMillis())
                    )
                    existing.id to file
                } else {
                    val file = File(sketchesDir, "sketch_${System.currentTimeMillis()}.png")
                    FileOutputStream(file).use { stream ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                    }
                    val now = System.currentTimeMillis()
                    val newId = repository.insertSketch(
                        SketchEntity(
                            filePath = file.absolutePath,
                            title = title,
                            createdAt = now,
                            updatedAt = now
                        )
                    )
                    newId to file
                }

                var galleryExportSucceeded = false
                val galleryUri = if (exportToGallery) {
                    try {
                        val flattenedForGallery = flattenOntoThemeBackground(bitmap)
                        val timestamp = System.currentTimeMillis()
                        val values = ContentValues().apply {
                            put(MediaStore.Images.Media.DISPLAY_NAME, "${title}_$timestamp.png")
                            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/ColorCraft")
                        }
                        val resolver = getApplication<Application>().contentResolver
                        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                        val outputStream = uri?.let { resolver.openOutputStream(it) }
                        if (uri != null && outputStream != null) {
                            outputStream.use { out ->
                                flattenedForGallery.compress(Bitmap.CompressFormat.PNG, 100, out)
                            }
                            galleryExportSucceeded = true
                        }
                        uri
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to export drawing to gallery", e)
                        null
                    }
                } else {
                    null
                }

                val shareableUri = if (galleryExportSucceeded && galleryUri != null) {
                    galleryUri
                } else {
                    val shareCacheDir = File(getApplication<Application>().filesDir, "share_cache")
                    if (!shareCacheDir.exists()) {
                        shareCacheDir.mkdirs()
                    }
                    val shareCacheFile = File(shareCacheDir, "share_$resultId.png")
                    val flattenedForShare = flattenOntoThemeBackground(bitmap)
                    FileOutputStream(shareCacheFile).use { stream ->
                        flattenedForShare.compress(Bitmap.CompressFormat.PNG, 100, stream)
                    }
                    FileProvider.getUriForFile(
                        getApplication<Application>(),
                        "${getApplication<Application>().packageName}.fileprovider",
                        shareCacheFile
                    )
                }

                Triple(resultId, shareableUri, galleryExportSucceeded)
            }

            onSaved(sketchId, shareableUri, galleryExportSucceeded)
        }
    }

    private fun flattenOntoThemeBackground(bitmap: Bitmap): Bitmap {
        val flattened = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(flattened)
        canvas.drawColor(ContextCompat.getColor(getApplication(), R.color.color_background))
        canvas.drawBitmap(bitmap, 0f, 0f, null)
        return flattened
    }

    companion object {
        private const val TAG = "DrawingViewModel"
    }
}
