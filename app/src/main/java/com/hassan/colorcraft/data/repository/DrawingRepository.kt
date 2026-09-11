package com.hassan.colorcraft.data.repository

import com.hassan.colorcraft.data.db.entity.SketchEntity
import kotlinx.coroutines.flow.Flow

interface DrawingRepository {
    fun getAllSketches(): Flow<List<SketchEntity>>
    suspend fun getSketchById(id: Long): SketchEntity?
    suspend fun insertSketch(sketch: SketchEntity): Long
    suspend fun updateSketch(sketch: SketchEntity)
    suspend fun deleteSketch(sketch: SketchEntity)
}
