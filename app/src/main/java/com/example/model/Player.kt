package com.example.model

data class TrailPoint(
    val x: Float,
    val y: Float,
    val alpha: Float
)

class Player(
    startX: Float = 24f,
    startY: Float = 24f
) {
    var x: Float = startX
    var y: Float = startY
    var vx: Float = 0f
    var vy: Float = 0f

    // In our 160x96 logical retro resolution, 1 tile = 8x8 pixels.
    // The ball has radius 3.2f logical pixels (~6.4x6.4 px diameter) for nimble platforming.
    val radius: Float = 3.2f

    var respawnX: Float = startX
    var respawnY: Float = startY

    var lastBounceSurface: SurfaceType = SurfaceType.NORMAL
    var bounceCount: Int = 0

    // Boost / Pump action state: pressing Boost gives extra bounce height on impact or a mid-air hop
    var boostQueuedTicks: Int = 0
    var airBoostAvailable: Boolean = true

    // Visual pixel squash-and-stretch feedback
    var squashFrames: Int = 0
    var stretchFrames: Int = 0
    var invulnerableFrames: Int = 0
    var hitFreezeFrames: Int = 0

    val trail: ArrayDeque<TrailPoint> = ArrayDeque(8)

    fun resetTo(spawnX: Float, spawnY: Float, fullReset: Boolean = false) {
        x = spawnX
        y = spawnY
        vx = 0f
        vy = 0f
        if (fullReset) {
            respawnX = spawnX
            respawnY = spawnY
        }
        boostQueuedTicks = 0
        airBoostAvailable = true
        squashFrames = 0
        stretchFrames = 0
        invulnerableFrames = 45
        hitFreezeFrames = 0
        trail.clear()
    }

    fun recordTrail() {
        if (trail.size >= 6) {
            trail.removeFirst()
        }
        trail.addLast(TrailPoint(x, y, 1f))
    }
}
