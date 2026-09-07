package com.hassan.colorcraft.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.hassan.colorcraft.data.db.entity.ColoringProgressEntity

@Dao
interface ColoringProgressDao {

    @Query("SELECT * FROM coloring_progress WHERE pageId = :pageId")
    suspend fun getProgressForPage(pageId: String): ColoringProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProgress(progress: ColoringProgressEntity)

    @Query("DELETE FROM coloring_progress WHERE pageId = :pageId")
    suspend fun deleteProgressForPage(pageId: String)
}
