package com.hassan.colorcraft.ui.library

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doOnTextChanged
import androidx.recyclerview.widget.GridLayoutManager
import com.hassan.colorcraft.R
import com.hassan.colorcraft.databinding.ActivityLibraryBinding
import com.hassan.colorcraft.ui.home.HomeAdapter
import org.koin.androidx.viewmodel.ext.android.viewModel

class LibraryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLibraryBinding
    private val viewModel: LibraryViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLibraryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val adapter = HomeAdapter { _ ->
            // TODO: Phase 4 - navigate to ColoringActivity with the tapped page's id
            Toast.makeText(this, "Coloring screen coming in Phase 4", Toast.LENGTH_SHORT).show()
        }
        binding.libraryRecyclerView.layoutManager = GridLayoutManager(this, 2)
        binding.libraryRecyclerView.adapter = adapter

        var totalCount: Int? = null

        viewModel.totalPageCount.observe(this) { count ->
            totalCount = count
            binding.librarySubtitleText.text = getString(R.string.library_subtitle_format, count)
        }

        viewModel.filteredPages.observe(this) { pages ->
            adapter.submitList(pages)

            val currentTotal = totalCount
            if (currentTotal != null) {
                binding.resultCountText.text =
                    getString(R.string.library_result_count_format, pages.size, currentTotal)
            }
        }

        binding.searchInput.doOnTextChanged { text, _, _, _ ->
            viewModel.onSearchQueryChanged(text?.toString().orEmpty())
        }

        binding.difficultyFilterGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            val checkedId = checkedIds.firstOrNull() ?: return@setOnCheckedStateChangeListener
            val difficulty = when (checkedId) {
                binding.chipAll.id -> null
                binding.chipEasy.id -> "Easy"
                binding.chipMedium.id -> "Medium"
                binding.chipDetailed.id -> "Detailed"
                else -> null
            }
            viewModel.onDifficultyFilterChanged(difficulty)
        }

        binding.backButton.setOnClickListener {
            finish()
        }
    }
}
