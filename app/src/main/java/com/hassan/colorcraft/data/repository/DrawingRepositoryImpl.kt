package com.hassan.colorcraft.data.repository

import com.hassan.colorcraft.data.db.dao.SketchDao
import com.hassan.colorcraft.data.db.entity.SketchEntity
import kotlinx.coroutines.flow.Flow

class DrawingRepositoryImpl(
    private val sketchDao: SketchDao
) : DrawingRepository {

    override fun getAllSketches(): Flow<List<SketchEntity>> =
        sketchDao.getAllSketches()

    override suspend fun getSketchById(id: Long): SketchEntity? =
        sketchDao.getSketchById(id)

    override suspend fun insertSketch(sketch: SketchEntity): Long =
        sketchDao.insertSketch(sketch)

    override suspend fun updateSketch(sketch: SketchEntity) =
        sketchDao.updateSketch(sketch)

    override suspend fun deleteSketch(sketch: SketchEntity) =
        sketchDao.deleteSketch(sketch)
}
