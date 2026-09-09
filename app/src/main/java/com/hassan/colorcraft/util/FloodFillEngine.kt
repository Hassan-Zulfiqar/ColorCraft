package com.hassan.colorcraft.util

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.abs

class FloodFillEngine {

    fun floodFill(bitmap: Bitmap, startX: Int, startY: Int, fillColor: Int, tolerance: Int = 30): Bitmap? {
        val width = bitmap.width
        val height = bitmap.height

        if (startX < 0 || startX >= width || startY < 0 || startY >= height) {
            return null
        }

        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val startIndex = startY * width + startX
        val targetColor = pixels[startIndex]

        val distanceFromBlack = abs(Color.red(targetColor)) +
            abs(Color.green(targetColor)) +
            abs(Color.blue(targetColor))

        if (distanceFromBlack <= OUTLINE_PROTECTION_THRESHOLD) {
            return null
        }

        if (targetColor == fillColor) {
            return null
        }

        val targetRed = Color.red(targetColor)
        val targetGreen = Color.green(targetColor)
        val targetBlue = Color.blue(targetColor)

        val visited = BooleanArray(width * height)
        val queue = ArrayDeque<Int>()
        queue.addLast(startIndex)
        visited[startIndex] = true

        while (queue.isNotEmpty()) {
            val index = queue.removeFirst()
            val pixelColor = pixels[index]

            val distance = abs(Color.red(pixelColor) - targetRed) +
                abs(Color.green(pixelColor) - targetGreen) +
                abs(Color.blue(pixelColor) - targetBlue)

            if (distance > tolerance) {
                continue
            }

            pixels[index] = fillColor

            val x = index % width
            val y = index / width

            if (x > 0) enqueueIfUnvisited(index - 1, visited, queue)
            if (x < width - 1) enqueueIfUnvisited(index + 1, visited, queue)
            if (y > 0) enqueueIfUnvisited(index - width, visited, queue)
            if (y < height - 1) enqueueIfUnvisited(index + width, visited, queue)
        }

        val result = bitmap.copy(bitmap.config ?: Bitmap.Config.ARGB_8888, true)
        result.setPixels(pixels, 0, width, 0, 0, width, height)
        return result
    }

    private fun enqueueIfUnvisited(index: Int, visited: BooleanArray, queue: ArrayDeque<Int>) {
        if (!visited[index]) {
            visited[index] = true
            queue.addLast(index)
        }
    }

    companion object {
        private const val OUTLINE_PROTECTION_THRESHOLD = 45
    }
}
