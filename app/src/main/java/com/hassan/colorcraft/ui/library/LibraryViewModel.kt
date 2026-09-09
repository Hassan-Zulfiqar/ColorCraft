package com.hassan.colorcraft.ui.library

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hassan.colorcraft.data.db.entity.ColoringPageEntity
import com.hassan.colorcraft.data.repository.ColoringRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class LibraryViewModel(
    private val repository: ColoringRepository
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    private val difficultyFilter = MutableStateFlow<String?>(null)

    private val _filteredPages = MutableLiveData<List<ColoringPageEntity>>()
    val filteredPages: LiveData<List<ColoringPageEntity>> = _filteredPages

    private val _totalPageCount = MutableLiveData<Int>()
    val totalPageCount: LiveData<Int> = _totalPageCount

    init {
        viewModelScope.launch {
            combine(
                repository.getAllPages(),
                searchQuery,
                difficultyFilter
            ) { pages, query, difficulty ->
                pages
                    .filter { it.title.contains(query, ignoreCase = true) }
                    .filter { difficulty == null || it.difficultyLabel == difficulty }
            }.collect { result ->
                _filteredPages.postValue(result)
            }
        }
        viewModelScope.launch {
            _totalPageCount.postValue(repository.getPageCount())
        }
    }

    fun onSearchQueryChanged(query: String) {
        searchQuery.value = query
    }

    fun onDifficultyFilterChanged(difficulty: String?) {
        difficultyFilter.value = difficulty
    }
}
