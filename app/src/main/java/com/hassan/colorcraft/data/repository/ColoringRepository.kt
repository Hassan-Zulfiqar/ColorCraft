package com.hassan.colorcraft.data.repository

import com.hassan.colorcraft.data.db.entity.ColoringPageEntity
import com.hassan.colorcraft.data.db.entity.ColoringProgressEntity
import kotlinx.coroutines.flow.Flow

interface ColoringRepository {
    fun getAllPages(): Flow<List<ColoringPageEntity>>
    fun searchPages(query: String): Flow<List<ColoringPageEntity>>
    suspend fun getPageCount(): Int
    suspend fun getPageById(id: String): ColoringPageEntity?
    suspend fun getProgressForPage(pageId: String): ColoringProgressEntity?
    suspend fun saveProgress(progress: ColoringProgressEntity)
    suspend fun deleteProgress(pageId: String)
}
