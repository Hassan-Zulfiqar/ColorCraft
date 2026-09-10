package com.hassan.colorcraft.ui.coloring

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.FrameLayout
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.hassan.colorcraft.databinding.ActivityColoringBinding
import com.hassan.colorcraft.ui.common.ColorPickerBottomSheet
import org.koin.androidx.viewmodel.ext.android.viewModel

class ColoringActivity : AppCompatActivity() {

    private lateinit var binding: ActivityColoringBinding
    private val viewModel: ColoringViewModel by viewModel()
    private lateinit var canvasView: ColoringCanvasView
    private lateinit var swatchAdapter: ColorSwatchAdapter
    private var pendingSaveBitmap: Bitmap? = null
    private var pendingSaveFlowComplete: (() -> Unit)? = null
    private var isDirty: Boolean = false
    private var isFirstUndoRedoCallback: Boolean = true

    private val requestWritePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        val bitmap = pendingSaveBitmap
        val onComplete = pendingSaveFlowComplete
        pendingSaveBitmap = null
        pendingSaveFlowComplete = null

        if (bitmap != null) {
            viewModel.saveArtwork(bitmap, true) { shareableUri, roomSaveSucceeded, galleryExportSucceeded ->
                if (roomSaveSucceeded) isDirty = false
                showSaveResultSnackbar(shareableUri, galleryExportSucceeded)
                onComplete?.invoke()
            }
        } else {
            onComplete?.invoke()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val pageId = intent.getStringExtra(EXTRA_PAGE_ID)
        if (pageId == null) {
            finish()
            return
        }

        binding = ActivityColoringBinding.inflate(layoutInflater)
        setContentView(binding.root)

        onBackPressedDispatcher.addCallback(this) {
            handleBackPress()
        }

        supportFragmentManager.setFragmentResultListener(
            ColorPickerBottomSheet.REQUEST_KEY,
            this
        ) { _, bundle ->
            val color = bundle.getInt(ColorPickerBottomSheet.RESULT_COLOR_KEY)
            viewModel.selectColor(color)
        }

        canvasView = ColoringCanvasView(this)
        binding.coloringCanvasContainer.addView(
            canvasView,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        swatchAdapter = ColorSwatchAdapter(viewModel.swatchColors) { color ->
            viewModel.selectColor(color)
        }
        binding.colorSwatchRecyclerView.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.colorSwatchRecyclerView.adapter = swatchAdapter

        canvasView.onUndoRedoStateChanged = { canUndo, canRedo ->
            binding.undoButton.isEnabled = canUndo
            binding.redoButton.isEnabled = canRedo
            if (isFirstUndoRedoCallback) {
                isFirstUndoRedoCallback = false
            } else {
                isDirty = true
            }
        }

        viewModel.pageBitmap.observe(this) { bitmap ->
            if (bitmap != null) {
                canvasView.loadBitmap(bitmap)
            }
        }

        viewModel.originalBitmap.observe(this) { bitmap ->
            if (bitmap != null) {
                canvasView.setOriginalBitmap(bitmap)
            }
        }

        viewModel.pageInfo.observe(this) { page ->
            if (page != null) {
                binding.drawingTitleText.text = page.title
            }
        }

        viewModel.currentFillColor.observe(this) { color ->
            canvasView.setFillColor(color)
            swatchAdapter.setSelectedColor(color)
        }

        binding.backButton.setOnClickListener { handleBackPress() }
        binding.undoButton.setOnClickListener { canvasView.undo() }
        binding.redoButton.setOnClickListener { canvasView.redo() }
        binding.toolZoomButton.setOnClickListener { canvasView.toggleZoom() }
        binding.toolResetButton.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle("Reset Drawing?")
                .setMessage("This will erase all your coloring on this page. This can't be undone.")
                .setPositiveButton("Reset") { _, _ -> canvasView.resetToOriginal() }
                .setNegativeButton("Cancel", null)
                .show()
        }

        binding.openColorPickerButton.setOnClickListener {
            ColorPickerBottomSheet.newInstance(viewModel.currentFillColor.value ?: Color.RED)
                .show(supportFragmentManager, "color_picker")
        }

        binding.saveButton.setOnClickListener {
            val bitmap = canvasView.getCurrentBitmap() ?: return@setOnClickListener
            showSaveDialog(bitmap)
        }

        viewModel.loadPage(pageId)
    }

    private fun handleBackPress() {
        if (!isDirty) {
            finish()
            return
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Unsaved Changes")
            .setMessage("You have unsaved coloring changes. Would you like to save before leaving?")
            .setPositiveButton("Save & Exit") { _, _ ->
                val bitmap = canvasView.getCurrentBitmap()
                if (bitmap == null) {
                    finish()
                } else {
                    showSaveDialog(bitmap) { finish() }
                }
            }
            .setNegativeButton("Discard & Exit") { _, _ -> finish() }
            .setNeutralButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun showSaveDialog(bitmap: Bitmap, onSaveFlowComplete: () -> Unit = {}) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Save Artwork")
            .setMessage("Save a copy to your device's gallery, or just keep your progress inside the app?")
            .setPositiveButton("Save to Gallery") { _, _ ->
                saveWithGalleryExport(bitmap, onSaveFlowComplete)
            }
            .setNegativeButton("Just Save Progress") { _, _ ->
                viewModel.saveArtwork(bitmap, false) { shareableUri, _, _ ->
                    showSnackbarWithShareAction("Progress saved", shareableUri)
                    onSaveFlowComplete()
                }
            }
            .show()
    }

    private fun saveWithGalleryExport(bitmap: Bitmap, onComplete: () -> Unit = {}) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            viewModel.saveArtwork(bitmap, true) { shareableUri, roomSaveSucceeded, galleryExportSucceeded ->
                if (roomSaveSucceeded) isDirty = false
                showSaveResultSnackbar(shareableUri, galleryExportSucceeded)
                onComplete()
            }
        } else {
            val permissionState = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            if (permissionState == PackageManager.PERMISSION_GRANTED) {
                viewModel.saveArtwork(bitmap, true) { shareableUri, roomSaveSucceeded, galleryExportSucceeded ->
                    if (roomSaveSucceeded) isDirty = false
                    showSaveResultSnackbar(shareableUri, galleryExportSucceeded)
                    onComplete()
                }
            } else {
                pendingSaveBitmap = bitmap
                pendingSaveFlowComplete = onComplete
                requestWritePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
    }

    private fun showSaveResultSnackbar(shareableUri: Uri?, galleryExportSucceeded: Boolean) {
        val message = if (galleryExportSucceeded) {
            "Artwork saved"
        } else {
            "Saved, but couldn't add to gallery"
        }
        showSnackbarWithShareAction(message, shareableUri)
    }

    private fun showSnackbarWithShareAction(message: String, shareableUri: Uri?) {
        val snackbar = Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT)
        if (shareableUri != null) {
            snackbar.setAction("Share") { shareArtwork(shareableUri) }
        }
        snackbar.show()
    }

    private fun shareArtwork(uri: Uri) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, null))
    }

    companion object {
        private const val EXTRA_PAGE_ID = "extra_page_id"

        fun newIntent(context: Context, pageId: String): Intent {
            return Intent(context, ColoringActivity::class.java).apply {
                putExtra(EXTRA_PAGE_ID, pageId)
            }
        }
    }
}
