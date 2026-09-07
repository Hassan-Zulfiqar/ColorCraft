package com.hassan.colorcraft.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.hassan.colorcraft.data.db.entity.ColoringPageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ColoringPageDao {

    @Query("SELECT * FROM coloring_pages")
    fun getAllPages(): Flow<List<ColoringPageEntity>>

    @Query("SELECT * FROM coloring_pages WHERE id = :id")
    suspend fun getPageById(id: String): ColoringPageEntity?

    @Query("SELECT * FROM coloring_pages WHERE title LIKE '%' || :query || '%'")
    fun searchPages(query: String): Flow<List<ColoringPageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(pages: List<ColoringPageEntity>)

    @Query("SELECT COUNT(*) FROM coloring_pages")
    suspend fun getCount(): Int
}
