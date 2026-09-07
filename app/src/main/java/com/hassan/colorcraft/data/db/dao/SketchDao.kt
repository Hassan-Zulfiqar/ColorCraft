package com.hassan.colorcraft.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.hassan.colorcraft.data.db.entity.SketchEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SketchDao {

    @Query("SELECT * FROM sketches")
    fun getAllSketches(): Flow<List<SketchEntity>>

    @Insert
    suspend fun insertSketch(sketch: SketchEntity): Long

    @Delete
    suspend fun deleteSketch(sketch: SketchEntity)
}
