package com.hassan.colorcraft.ui.coloring

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
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.hassan.colorcraft.data.db.entity.ColoringPageEntity
import com.hassan.colorcraft.data.db.entity.ColoringProgressEntity
import com.hassan.colorcraft.data.repository.ColoringRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class ColoringViewModel(
    application: Application,
    private val repository: ColoringRepository
) : AndroidViewModel(application) {

    val swatchColors: List<Int> = listOf(
        "#EF4444", "#F97316", "#F59E0B", "#EAB308", "#84CC16", "#22C55E",
        "#14B8A6", "#06B6D4", "#3B82F6", "#6366F1", "#8B5CF6", "#EC4899",
        "#92400E", "#1C1B1F"
    ).map { Color.parseColor(it) }

    private val _pageInfo = MutableLiveData<ColoringPageEntity?>()
    val pageInfo: LiveData<ColoringPageEntity?> = _pageInfo

    private val _pageBitmap = MutableLiveData<Bitmap?>()
    val pageBitmap: LiveData<Bitmap?> = _pageBitmap

    private val _originalBitmap = MutableLiveData<Bitmap?>()
    val originalBitmap: LiveData<Bitmap?> = _originalBitmap

    private val _currentFillColor = MutableLiveData(swatchColors.first())
    val currentFillColor: LiveData<Int> = _currentFillColor

    fun loadPage(pageId: String) {
        viewModelScope.launch {
            val page = repository.getPageById(pageId)
            _pageInfo.postValue(page)

            if (page != null) {
                val pristineDecoded = getApplication<Application>().assets.open(page.assetPath).use { input ->
                    BitmapFactory.decodeStream(input)
                }
                val pristineFlattened = flattenOnWhite(pristineDecoded)
                _originalBitmap.postValue(pristineFlattened)

                val progress = repository.getProgressForPage(pageId)
                val displayBitmap = if (progress != null && File(progress.savedBitmapPath).exists()) {
                    val resumedDecoded = BitmapFactory.decodeFile(progress.savedBitmapPath)
                    flattenOnWhite(resumedDecoded)
                } else {
                    pristineFlattened
                }
                _pageBitmap.postValue(displayBitmap)
            } else {
                Log.e(TAG, "No page found for id: $pageId")
            }
        }
    }

    fun selectColor(color: Int) {
        _currentFillColor.value = color
    }

    private fun flattenOnWhite(decoded: Bitmap): Bitmap {
        val flattened = Bitmap.createBitmap(decoded.width, decoded.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(flattened)
        canvas.drawColor(Color.WHITE)
        canvas.drawBitmap(decoded, 0f, 0f, null)
        return flattened
    }

    fun saveArtwork(
        bitmap: Bitmap,
        exportToGallery: Boolean,
        onSaved: (shareableUri: Uri?, roomSaveSucceeded: Boolean, galleryExportSucceeded: Boolean) -> Unit
    ) {
        viewModelScope.launch {
            val page = pageInfo.value
            if (page == null) {
                Log.e(TAG, "Cannot save artwork: no page currently loaded")
                return@launch
            }

            val (shareableUri, roomSaveSucceeded, galleryExportSucceeded) = withContext(Dispatchers.IO) {
                val progressDir = File(getApplication<Application>().filesDir, "coloring_progress")
                if (!progressDir.exists()) {
                    progressDir.mkdirs()
                }
                val progressFile = File(progressDir, "${page.id}.png")
                FileOutputStream(progressFile).use { stream ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                }

                val roomSaveSucceeded = try {
                    repository.saveProgress(
                        ColoringProgressEntity(
                            pageId = page.id,
                            savedBitmapPath = progressFile.absolutePath,
                            lastEditedAt = System.currentTimeMillis()
                        )
                    )
                    true
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to save progress to Room", e)
                    false
                }

                var galleryExportSucceeded = false
                val galleryUri = if (exportToGallery) {
                    try {
                        val timestamp = System.currentTimeMillis()
                        val values = ContentValues().apply {
                            put(MediaStore.Images.Media.DISPLAY_NAME, "${page.title}_$timestamp.png")
                            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/ColorCraft")
                        }
                        val resolver = getApplication<Application>().contentResolver
                        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                        val outputStream = uri?.let { resolver.openOutputStream(it) }
                        if (uri != null && outputStream != null) {
                            outputStream.use { out ->
                                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                            }
                            galleryExportSucceeded = true
                        }
                        uri
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to export artwork to gallery", e)
                        null
                    }
                } else {
                    null
                }

                val shareableUri = galleryUri ?: FileProvider.getUriForFile(
                    getApplication<Application>(),
                    "${getApplication<Application>().packageName}.fileprovider",
                    progressFile
                )

                Triple(shareableUri, roomSaveSucceeded, galleryExportSucceeded)
            }

            onSaved(shareableUri, roomSaveSucceeded, galleryExportSucceeded)
        }
    }

    companion object {
        private const val TAG = "ColoringViewModel"
    }
}
