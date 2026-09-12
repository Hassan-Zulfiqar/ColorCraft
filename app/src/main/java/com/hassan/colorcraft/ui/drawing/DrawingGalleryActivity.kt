package com.hassan.colorcraft.ui.drawing

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.hassan.colorcraft.R
import com.hassan.colorcraft.databinding.ActivityDrawingGalleryBinding
import com.hassan.colorcraft.ui.common.AppConfirmDialog
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
                AppConfirmDialog.show(
                    context = this,
                    iconRes = R.drawable.ic_trash,
                    title = "Delete Drawing?",
                    message = "This will permanently delete this drawing. This can't be undone.",
                    positiveText = "Cancel",
                    onPositiveClick = {},
                    destructiveText = "Delete",
                    onDestructiveClick = { viewModel.deleteSketch(sketch) }
                )
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
