package com.hassan.colorcraft.ui.home

import android.content.Intent
import android.os.Bundle
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.hassan.colorcraft.R
import com.hassan.colorcraft.databinding.ActivityHomeBinding
import com.hassan.colorcraft.ui.coloring.ColoringActivity
import com.hassan.colorcraft.ui.common.AppConfirmDialog
import com.hassan.colorcraft.ui.drawing.DrawingGalleryActivity
import com.hassan.colorcraft.ui.library.LibraryActivity
import com.hassan.colorcraft.ui.settings.SettingsActivity
import org.koin.androidx.viewmodel.ext.android.viewModel

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private val viewModel: HomeViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        onBackPressedDispatcher.addCallback(this) {
            handleBackPress()
        }

        val adapter = HomeAdapter { page ->
            startActivity(ColoringActivity.newIntent(this, page.id))
        }
        binding.coloringPagesRecyclerView.layoutManager = GridLayoutManager(this, 2)
        binding.coloringPagesRecyclerView.adapter = adapter

        viewModel.libraryPreview.observe(this) { pages ->
            adapter.submitList(pages)
        }

        viewModel.totalPageCount.observe(this) { count ->
            binding.coloringCountBadgeText.text = getString(R.string.home_library_count_format, count)
        }

        binding.drawNowButton.setOnClickListener {
            startActivity(DrawingGalleryActivity.newIntent(this))
        }

        binding.seeAllText.setOnClickListener {
            startActivity(Intent(this, LibraryActivity::class.java))
        }

        binding.settingsButton.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    private fun handleBackPress() {
        if (!isTaskRoot) {
            finish()
            return
        }

        AppConfirmDialog.show(
            context = this,
            iconRes = R.drawable.ic_exit_dialog,
            title = "Exit ColorCraft?",
            message = "Are you sure you want to exit the app?",
            positiveText = "Cancel",
            onPositiveClick = {},
            destructiveText = "Exit",
            onDestructiveClick = { finish() }
        )
    }
}
