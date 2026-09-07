package com.hassan.colorcraft.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "coloring_progress",
    foreignKeys = [
        ForeignKey(
            entity = ColoringPageEntity::class,
            parentColumns = ["id"],
            childColumns = ["pageId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ColoringProgressEntity(
    @PrimaryKey
    val pageId: String,
    val savedBitmapPath: String,
    val lastEditedAt: Long
)
