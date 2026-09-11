package com.hassan.colorcraft.ui.drawing

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.hassan.colorcraft.databinding.ActivityDrawingGalleryBinding
import org.koin.androidx.viewmodel.ext.android.viewModel

class DrawingGalleryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDrawingGalleryBinding
    private val viewModel: DrawingGalleryViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityDrawingGalleryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val adapter = SketchAdapter(
            onItemClick = { sketch ->
                startActivity(DrawingActivity.newIntent(this, sketch.id))
            },
            onItemLongClick = { sketch ->
                MaterialAlertDialogBuilder(this)
                    .setTitle("Delete Drawing?")
                    .setMessage("This will permanently delete this drawing. This can't be undone.")
                    .setPositiveButton("Delete") { _, _ -> viewModel.deleteSketch(sketch) }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        )
        binding.sketchesRecyclerView.layoutManager = GridLayoutManager(this, 2)
        binding.sketchesRecyclerView.adapter = adapter

        viewModel.sketches.observe(this) { sketches ->
            adapter.submitList(sketches)

            if (sketches.isEmpty()) {
                binding.emptyStateContainer.visibility = View.VISIBLE
                binding.sketchesRecyclerView.visibility = View.GONE
            } else {
                binding.emptyStateContainer.visibility = View.GONE
                binding.sketchesRecyclerView.visibility = View.VISIBLE
            }
        }

        binding.newDrawingCard.setOnClickListener {
            startActivity(DrawingActivity.newIntent(this))
        }

        binding.backButton.setOnClickListener { finish() }
    }

    companion object {
        fun newIntent(context: Context): Intent {
            return Intent(context, DrawingGalleryActivity::class.java)
        }
    }
}
