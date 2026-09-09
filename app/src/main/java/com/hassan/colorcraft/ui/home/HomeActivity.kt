package com.hassan.colorcraft.ui.home

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.hassan.colorcraft.R
import com.hassan.colorcraft.databinding.ActivityHomeBinding
import com.hassan.colorcraft.ui.library.LibraryActivity
import org.koin.androidx.viewmodel.ext.android.viewModel

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private val viewModel: HomeViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val adapter = HomeAdapter { _ ->
            // TODO: Phase 4 - navigate to ColoringActivity with the tapped page's id
            Toast.makeText(this, "Coloring screen coming in Phase 4", Toast.LENGTH_SHORT).show()
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
            // TODO: Phase 5 - navigate to DrawingActivity
            Toast.makeText(this, "Drawing screen coming in Phase 5", Toast.LENGTH_SHORT).show()
        }

        binding.seeAllText.setOnClickListener {
            startActivity(Intent(this, LibraryActivity::class.java))
        }

        binding.settingsButton.setOnClickListener {
            // TODO: Phase 6 - navigate to SettingsActivity
            Toast.makeText(this, "Settings screen coming in Phase 6", Toast.LENGTH_SHORT).show()
        }
    }
}
