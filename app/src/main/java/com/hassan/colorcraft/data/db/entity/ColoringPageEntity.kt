package com.hassan.colorcraft.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "coloring_pages")
data class ColoringPageEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val difficultyScore: Int,
    val difficultyLabel: String,
    val assetPath: String,
    val thumbnailPath: String
)
