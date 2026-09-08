package com.hassan.colorcraft.data.repository

import com.hassan.colorcraft.data.db.dao.ColoringPageDao
import com.hassan.colorcraft.data.db.dao.ColoringProgressDao
import com.hassan.colorcraft.data.db.entity.ColoringPageEntity
import com.hassan.colorcraft.data.db.entity.ColoringProgressEntity
import kotlinx.coroutines.flow.Flow

class ColoringRepositoryImpl(
    private val coloringPageDao: ColoringPageDao,
    private val coloringProgressDao: ColoringProgressDao
) : ColoringRepository {

    override fun getAllPages(): Flow<List<ColoringPageEntity>> =
        coloringPageDao.getAllPages()

    override fun searchPages(query: String): Flow<List<ColoringPageEntity>> =
        coloringPageDao.searchPages(query)

    override suspend fun getPageById(id: String): ColoringPageEntity? =
        coloringPageDao.getPageById(id)

    override suspend fun getProgressForPage(pageId: String): ColoringProgressEntity? =
        coloringProgressDao.getProgressForPage(pageId)

    override suspend fun saveProgress(progress: ColoringProgressEntity) =
        coloringProgressDao.upsertProgress(progress)

    override suspend fun deleteProgress(pageId: String) =
        coloringProgressDao.deleteProgressForPage(pageId)
}
