package com.hassan.colorcraft.data.seed

import android.content.Context
import com.hassan.colorcraft.data.db.dao.ColoringPageDao
import com.hassan.colorcraft.data.db.entity.ColoringPageEntity
import org.json.JSONObject

class DatabasePrepopulator {

    suspend fun seedIfNeeded(context: Context, coloringPageDao: ColoringPageDao) {
        if (coloringPageDao.getCount() != 0) return

        val availableFullFiles = context.assets.list("coloring_pages/full")?.toSet() ?: emptySet()

        val json = context.assets.open("coloring_pages/collection.json").use { input ->
            JSONObject(input.bufferedReader().readText())
        }

        val entities = mutableListOf<ColoringPageEntity>()
        val keys = json.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val id = key.removeSuffix(".svg")

            if ("$id.png" !in availableFullFiles) continue

            val difficultyScore = json.getJSONObject(key)
                .getJSONObject("difficulty")
                .getInt("difficultyNumber")

            val difficultyLabel = when {
                difficultyScore < 30 -> "Easy"
                difficultyScore <= 100 -> "Medium"
                else -> "Detailed"
            }

            val title = id.split("_")
                .joinToString(" ") { word -> word.replaceFirstChar { it.uppercaseChar() } }

            entities.add(
                ColoringPageEntity(
                    id = id,
                    title = title,
                    difficultyScore = difficultyScore,
                    difficultyLabel = difficultyLabel,
                    assetPath = "coloring_pages/full/$id.png",
                    thumbnailPath = "coloring_pages/thumbnails/$id.png"
                )
            )
        }

        coloringPageDao.insertAll(entities)
    }
}
