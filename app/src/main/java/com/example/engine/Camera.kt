package com.example.engine

import com.example.model.Level
import com.example.model.Player
import kotlin.math.abs
import kotlin.math.roundToInt

class Camera(
    val viewportWidth: Float = LOGICAL_WIDTH,
    val viewportHeight: Float = LOGICAL_HEIGHT
) {
    companion object {
        // Fixed logical retro resolution: 160x96 pixels (20x12 tiles of 8x8 pixels)
        // Inspired by classic monochrome Nokia handheld displays, widened for modern aspect ratios
        const val LOGICAL_WIDTH = 160f
        const val LOGICAL_HEIGHT = 96f
    }

    var x: Float = 0f
        private set
    var y: Float = 0f
        private set

    // Integer-snapped coordinates for crisp pixel-art rendering without sub-pixel shimmer
    val snappedX: Int get() = x.roundToInt()
    val snappedY: Int get() = y.roundToInt()

    fun snapToPlayer(player: Player, level: Level) {
        val maxX = (level.pixelWidth - viewportWidth).coerceAtLeast(0f)
        val maxY = (level.pixelHeight - viewportHeight).coerceAtLeast(0f)
        x = (player.x - viewportWidth * 0.45f).coerceIn(0f, maxX)
        y = (player.y - viewportHeight * 0.52f).coerceIn(0f, maxY)
    }

    fun update(player: Player, level: Level) {
        val maxX = (level.pixelWidth - viewportWidth).coerceAtLeast(0f)
        val maxY = (level.pixelHeight - viewportHeight).coerceAtLeast(0f)

        // Subtle look-ahead in direction of horizontal velocity, with a small classic deadzone
        val lookAheadX = when {
            player.vx > 0.35f -> 10f
            player.vx < -0.35f -> -10f
            else -> 0f
        }
        val desiredX = (player.x + lookAheadX - viewportWidth * 0.48f).coerceIn(0f, maxX)
        val desiredY = (player.y - viewportHeight * 0.54f).coerceIn(0f, maxY)

        // Slightly rigid retro camera: steps briskly when outside deadzone
        val dx = desiredX - x
        if (abs(dx) > 4f) {
            val stepX = (dx * 0.28f).coerceIn(-3.2f, 3.2f)
            x = (x + stepX).coerceIn(0f, maxX)
        } else if (abs(dx) > 0.5f && abs(player.vx) < 0.1f) {
            x = (x + dx * 0.15f).coerceIn(0f, maxX)
        }

        val dy = desiredY - y
        if (abs(dy) > 6f) {
            val stepY = (dy * 0.26f).coerceIn(-3.0f, 3.0f)
            y = (y + stepY).coerceIn(0f, maxY)
        }
    }
}
