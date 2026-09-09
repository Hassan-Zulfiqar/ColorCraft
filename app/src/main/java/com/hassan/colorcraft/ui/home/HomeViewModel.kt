package com.hassan.colorcraft.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hassan.colorcraft.data.db.entity.ColoringPageEntity
import com.hassan.colorcraft.data.repository.ColoringRepository
import kotlinx.coroutines.launch

class HomeViewModel(
    private val repository: ColoringRepository
) : ViewModel() {

    private val _libraryPreview = MutableLiveData<List<ColoringPageEntity>>()
    val libraryPreview: LiveData<List<ColoringPageEntity>> = _libraryPreview

    private val _totalPageCount = MutableLiveData<Int>()
    val totalPageCount: LiveData<Int> = _totalPageCount

    init {
        viewModelScope.launch {
            repository.getAllPages().collect { pages ->
                _libraryPreview.postValue(pages.take(6))
            }
        }
        viewModelScope.launch {
            _totalPageCount.postValue(repository.getPageCount())
        }
    }
}
