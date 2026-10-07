package com.example.model

enum class CollectibleType(val points: Int) {
    RING(250),       // Required to unlock the stage exit gate
    DATA_GEM(100),   // Bonus score collectible
    EXTRA_LIFE(500)  // +1 Life orb
}

data class Collectible(
    val id: Int,
    val gridX: Int,
    val gridY: Int,
    val x: Float,
    val y: Float,
    val type: CollectibleType,
    var collected: Boolean = false,
    var collectAnimTicks: Int = 0
)

enum class HazardType {
    PATROL_BLADE,      // Horizontal or vertical patrolling pixel saw/blade
    PISTON_CRUSHER,    // Heavy oscillating block hazard
    SPARK_DRONE        // Fast compact bouncing/patrolling spark
}

data class Hazard(
    val id: Int,
    val startX: Float,
    val startY: Float,
    var x: Float,
    var y: Float,
    val width: Float = 6f,
    val height: Float = 6f,
    val speedX: Float = 0f,
    val speedY: Float = 0f,
    val rangeX: Float = 0f,
    val rangeY: Float = 0f,
    val type: HazardType = HazardType.PATROL_BLADE,
    var phase: Float = 0f
)

data class MovingPlatform(
    val id: Int,
    val startX: Float,
    val startY: Float,
    var x: Float,
    var y: Float,
    var prevX: Float = startX,
    var prevY: Float = startY,
    val width: Float = 24f,  // 3 tiles wide by default (24 logical px)
    val height: Float = 5f,
    val speedX: Float = 0f,
    val speedY: Float = 0f,
    val rangeX: Float = 0f,
    val rangeY: Float = 0f,
    var phase: Float = 0f
) {
    val deltaX: Float get() = x - prevX
    val deltaY: Float get() = y - prevY
}

data class CrumbleBlockState(
    val gridX: Int,
    val gridY: Int,
    var triggered: Boolean = false,
    var ticksRemaining: Int = 22,
    var respawnTicks: Int = 0
) {
    val isSolid: Boolean get() = !triggered || ticksRemaining > 0
}

data class CheckpointState(
    val gridX: Int,
    val gridY: Int,
    val worldX: Float,
    val worldY: Float,
    var activated: Boolean = false
)

data class PixelParticle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var ticksLeft: Int,
    val maxTicks: Int,
    val bright: Boolean = true
)
