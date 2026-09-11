package com.hassan.colorcraft.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.hassan.colorcraft.data.db.dao.ColoringPageDao
import com.hassan.colorcraft.data.db.dao.ColoringProgressDao
import com.hassan.colorcraft.data.db.dao.SketchDao
import com.hassan.colorcraft.data.db.entity.ColoringPageEntity
import com.hassan.colorcraft.data.db.entity.ColoringProgressEntity
import com.hassan.colorcraft.data.db.entity.SketchEntity

@Database(
    entities = [
        ColoringPageEntity::class,
        ColoringProgressEntity::class,
        SketchEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun coloringPageDao(): ColoringPageDao
    abstract fun coloringProgressDao(): ColoringProgressDao
    abstract fun sketchDao(): SketchDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "colorcraft.db"
                ).fallbackToDestructiveMigration(true)
                    .build().also { INSTANCE = it }
            }
        }
    }
}
