package com.hassan.colorcraft.ui.coloring

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.FrameLayout
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.hassan.colorcraft.databinding.ActivityColoringBinding
import org.koin.androidx.viewmodel.ext.android.viewModel

class ColoringActivity : AppCompatActivity() {

    private lateinit var binding: ActivityColoringBinding
    private val viewModel: ColoringViewModel by viewModel()
    private lateinit var canvasView: ColoringCanvasView
    private lateinit var swatchAdapter: ColorSwatchAdapter
    private var pendingSaveBitmap: Bitmap? = null

    private val requestWritePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        pendingSaveBitmap?.let { bitmap ->
            viewModel.saveArtwork(bitmap, true) { uri -> showSaveResultSnackbar(uri) }
        }
        pendingSaveBitmap = null
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

        binding.backButton.setOnClickListener { finish() }
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

        binding.saveButton.setOnClickListener {
            val bitmap = canvasView.getCurrentBitmap() ?: return@setOnClickListener

            MaterialAlertDialogBuilder(this)
                .setTitle("Save Artwork")
                .setMessage("Save a copy to your device's gallery, or just keep your progress inside the app?")
                .setPositiveButton("Save to Gallery") { _, _ ->
                    saveWithGalleryExport(bitmap)
                }
                .setNegativeButton("Just Save Progress") { _, _ ->
                    viewModel.saveArtwork(bitmap, false) { _ ->
                        Snackbar.make(binding.root, "Progress saved", Snackbar.LENGTH_SHORT).show()
                    }
                }
                .show()
        }

        viewModel.loadPage(pageId)
    }

    private fun saveWithGalleryExport(bitmap: Bitmap) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            viewModel.saveArtwork(bitmap, true) { uri -> showSaveResultSnackbar(uri) }
        } else {
            val permissionState = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            if (permissionState == PackageManager.PERMISSION_GRANTED) {
                viewModel.saveArtwork(bitmap, true) { uri -> showSaveResultSnackbar(uri) }
            } else {
                pendingSaveBitmap = bitmap
                requestWritePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
    }

    private fun showSaveResultSnackbar(uri: Uri?) {
        val message = if (uri != null) {
            "Artwork saved"
        } else {
            "Saved, but couldn't add to gallery"
        }
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
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
