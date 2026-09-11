package com.hassan.colorcraft.ui.drawing

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.FrameLayout
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.doOnTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.slider.Slider
import com.google.android.material.snackbar.Snackbar
import com.hassan.colorcraft.R
import com.hassan.colorcraft.databinding.ActivityDrawingBinding
import com.hassan.colorcraft.ui.coloring.ColorSwatchAdapter
import com.hassan.colorcraft.ui.common.ColorPickerBottomSheet
import org.koin.androidx.viewmodel.ext.android.viewModel

class DrawingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDrawingBinding
    private val viewModel: DrawingViewModel by viewModel()
    private lateinit var canvasView: DrawingCanvasView
    private lateinit var swatchAdapter: ColorSwatchAdapter
    private var currentSketchId: Long? = null
    private var isDirty: Boolean = false
    private var isFirstUndoRedoCallback: Boolean = true
    private val handler = Handler(Looper.getMainLooper())
    private var pendingTitleSaveRunnable: Runnable? = null
    private var pendingSaveBitmap: Bitmap? = null
    private var pendingSaveFlowComplete: (() -> Unit)? = null

    private val requestWritePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        val bitmap = pendingSaveBitmap
        val onComplete = pendingSaveFlowComplete
        pendingSaveBitmap = null
        pendingSaveFlowComplete = null

        if (bitmap != null) {
            performGallerySave(bitmap, onComplete ?: {})
        } else {
            onComplete?.invoke()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityDrawingBinding.inflate(layoutInflater)
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

        val sketchIdPresent = intent.extras?.containsKey(EXTRA_SKETCH_ID) == true
        if (sketchIdPresent) {
            val id = intent.getLongExtra(EXTRA_SKETCH_ID, 0L)
            currentSketchId = id
            viewModel.loadExistingSketch(id)
        }

        canvasView = DrawingCanvasView(this)
        binding.drawingCanvasContainer.addView(
            canvasView,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        if (!sketchIdPresent) {
            canvasView.loadBitmap(null)
            binding.drawingTitleInput.setText("Untitled Sketch")
            binding.drawingTitleInput.selectAll()
        }

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

        viewModel.canvasBitmap.observe(this) { bitmap ->
            if (bitmap != null) {
                canvasView.loadBitmap(bitmap)
            }
        }

        viewModel.sketchLoadFailed.observe(this) {
            currentSketchId = null
            canvasView.loadBitmap(null)
        }

        viewModel.currentTitle.observe(this) { title ->
            binding.drawingTitleInput.setText(title)
        }

        binding.drawingTitleInput.doOnTextChanged { _, _, _, _ ->
            pendingTitleSaveRunnable?.let { handler.removeCallbacks(it) }

            val runnable = Runnable {
                val sketchId = currentSketchId
                if (sketchId != null) {
                    viewModel.updateTitleOnly(sketchId, binding.drawingTitleInput.text.toString())
                }
            }
            pendingTitleSaveRunnable = runnable
            handler.postDelayed(runnable, TITLE_SAVE_DEBOUNCE_MS)
        }

        viewModel.currentColor.observe(this) { color ->
            canvasView.setStrokeColor(color)
            swatchAdapter.setSelectedColor(color)
        }

        binding.backButton.setOnClickListener { handleBackPress() }
        binding.undoButton.setOnClickListener { canvasView.undo() }
        binding.redoButton.setOnClickListener { canvasView.redo() }

        binding.toolBrushButton.setOnClickListener { selectTool(DrawTool.BRUSH) }
        binding.toolPencilButton.setOnClickListener { selectTool(DrawTool.PENCIL) }
        binding.toolEraserButton.setOnClickListener { selectTool(DrawTool.ERASER) }
        selectTool(DrawTool.BRUSH)

        binding.brushSizeSlider.addOnChangeListener { slider, value, fromUser ->
            if (!fromUser) return@addOnChangeListener
            canvasView.setStrokeWidth(value)
            updateBrushSizePreview(slider, value)
        }
        canvasView.setStrokeWidth(binding.brushSizeSlider.value)
        updateBrushSizePreview(binding.brushSizeSlider, binding.brushSizeSlider.value)

        binding.openColorPickerButton.setOnClickListener {
            ColorPickerBottomSheet.newInstance(viewModel.currentColor.value ?: Color.RED)
                .show(supportFragmentManager, "color_picker")
        }

        binding.clearCanvasButton.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle("Clear Drawing?")
                .setMessage("This will erase everything on this canvas. This can't be undone.")
                .setPositiveButton("Clear") { _, _ -> canvasView.clearCanvas() }
                .setNegativeButton("Cancel", null)
                .show()
        }

        binding.saveButton.setOnClickListener {
            val bitmap = canvasView.getCurrentBitmap() ?: return@setOnClickListener
            showSaveDialog(bitmap)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        pendingTitleSaveRunnable?.let { handler.removeCallbacks(it) }
    }

    private fun handleBackPress() {
        if (!isDirty) {
            finish()
            return
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Unsaved Changes")
            .setMessage("You have unsaved drawing changes. Would you like to save before leaving?")
            .setPositiveButton("Save & Exit") { _, _ ->
                val bitmap = canvasView.getCurrentBitmap()
                if (bitmap == null) {
                    finish()
                } else {
                    viewModel.saveSketch(bitmap, currentSketchId, currentTitleOrDefault(), false) { newId, _, _ ->
                        currentSketchId = newId
                        finish()
                    }
                }
            }
            .setNegativeButton("Discard & Exit") { _, _ -> finish() }
            .setNeutralButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun showSaveDialog(bitmap: Bitmap, onSaveFlowComplete: () -> Unit = {}) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Save Drawing")
            .setMessage("Save a copy to your device's gallery, or just keep your progress inside the app?")
            .setPositiveButton("Save to Gallery") { _, _ ->
                saveWithGalleryExport(bitmap, onSaveFlowComplete)
            }
            .setNegativeButton("Just Save Progress") { _, _ ->
                viewModel.saveSketch(bitmap, currentSketchId, currentTitleOrDefault(), false) { newId, shareableUri, _ ->
                    currentSketchId = newId
                    isDirty = false
                    showSnackbarWithShareAction("Progress saved", shareableUri)
                    onSaveFlowComplete()
                }
            }
            .show()
    }

    private fun saveWithGalleryExport(bitmap: Bitmap, onComplete: () -> Unit = {}) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            performGallerySave(bitmap, onComplete)
        } else {
            val permissionState = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            if (permissionState == PackageManager.PERMISSION_GRANTED) {
                performGallerySave(bitmap, onComplete)
            } else {
                pendingSaveBitmap = bitmap
                pendingSaveFlowComplete = onComplete
                requestWritePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
    }

    private fun performGallerySave(bitmap: Bitmap, onComplete: () -> Unit) {
        viewModel.saveSketch(bitmap, currentSketchId, currentTitleOrDefault(), true) { newId, shareableUri, galleryExportSucceeded ->
            currentSketchId = newId
            isDirty = false
            val message = if (galleryExportSucceeded) {
                "Drawing saved"
            } else {
                "Saved, but couldn't add to gallery"
            }
            showSnackbarWithShareAction(message, shareableUri)
            onComplete()
        }
    }

    private fun showSnackbarWithShareAction(message: String, shareableUri: Uri?) {
        val snackbar = Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT)
        if (shareableUri != null) {
            snackbar.setAction("Share") { shareDrawing(shareableUri) }
        }
        snackbar.show()
    }

    private fun shareDrawing(uri: Uri) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, null))
    }

    private fun currentTitleOrDefault(): String {
        val typedTitle = binding.drawingTitleInput.text?.toString()?.trim()
        return if (typedTitle.isNullOrEmpty()) "Untitled Sketch" else typedTitle
    }

    private fun selectTool(tool: DrawTool) {
        canvasView.setTool(tool)
        applyToolButtonStyle(binding.toolBrushButton, tool == DrawTool.BRUSH)
        applyToolButtonStyle(binding.toolPencilButton, tool == DrawTool.PENCIL)
        applyToolButtonStyle(binding.toolEraserButton, tool == DrawTool.ERASER)
    }

    private fun applyToolButtonStyle(button: MaterialButton, isSelected: Boolean) {
        val cornerRadiusPx = TOOL_BUTTON_CORNER_RADIUS_DP * resources.displayMetrics.density
        button.backgroundTintList = null
        button.background = if (isSelected) {
            GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(
                    getColor(R.color.color_primary_start),
                    getColor(R.color.color_primary_end)
                )
            ).apply {
                cornerRadius = cornerRadiusPx
            }
        } else {
            GradientDrawable().apply {
                setColor(getColor(R.color.color_surface_alt))
                cornerRadius = cornerRadiusPx
            }
        }

        val contentColor = if (isSelected) Color.WHITE else getColor(R.color.color_text_primary)
        button.setTextColor(contentColor)
        button.iconTint = ColorStateList.valueOf(contentColor)
    }

    private fun updateBrushSizePreview(slider: Slider, value: Float) {
        val range = slider.valueTo - slider.valueFrom
        val fraction = if (range == 0f) 0f else (value - slider.valueFrom) / range
        val previewSizeDp = MIN_PREVIEW_SIZE_DP + fraction * (MAX_PREVIEW_SIZE_DP - MIN_PREVIEW_SIZE_DP)
        val previewSizePx = (previewSizeDp * resources.displayMetrics.density).toInt()

        val layoutParams = binding.brushSizePreview.layoutParams
        layoutParams.width = previewSizePx
        layoutParams.height = previewSizePx
        binding.brushSizePreview.layoutParams = layoutParams
    }

    companion object {
        private const val EXTRA_SKETCH_ID = "extra_sketch_id"
        private const val MIN_PREVIEW_SIZE_DP = 8f
        private const val MAX_PREVIEW_SIZE_DP = 32f
        private const val TOOL_BUTTON_CORNER_RADIUS_DP = 100f
        private const val TITLE_SAVE_DEBOUNCE_MS = 600L

        fun newIntent(context: Context, sketchId: Long? = null): Intent {
            return Intent(context, DrawingActivity::class.java).apply {
                if (sketchId != null) {
                    putExtra(EXTRA_SKETCH_ID, sketchId)
                }
            }
        }
    }
}
