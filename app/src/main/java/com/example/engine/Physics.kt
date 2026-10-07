package com.example.engine

import com.example.model.Level
import com.example.model.PixelParticle
import com.example.model.Player
import com.example.model.SurfaceType
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

class Physics {
    companion object {
        // Tuned for crisp, responsive 60 FPS arcade feel in 160x96 logical pixel space (8px = 1 tile)
        const val GRAVITY = 0.145f
        const val MAX_FALL_SPEED = 3.25f

        const val ACCEL_X = 0.22f
        const val TURN_ACCEL_X = 0.34f
        const val MAX_SPEED_X = 1.65f
        const val FRICTION_X = 0.80f

        // Automatic bounce velocities (negative Y is up)
        // Normal bounce (-2.55f) reaches ~22.4px (~2.8 tiles) high
        // Boosted bounce (-3.05f) reaches ~32.0px (~4.0 tiles) high
        // Spring bounce (-3.65f) reaches ~45.9px (~5.7 tiles) high
        // Dampened bounce (-1.75f) reaches ~10.5px (~1.3 tiles) high
        const val BASE_BOUNCE_VY = -2.55f
        const val BOOST_BOUNCE_VY = -3.08f
        const val SPRING_BOUNCE_VY = -3.65f
        const val DAMP_BOUNCE_VY = -1.72f
        const val AIR_BOOST_IMPULSE_VY = -2.42f
    }

    fun updatePlayerPhysics(
        player: Player,
        input: InputManager,
        onAirBoostTriggered: () -> Unit
    ) {
        if (player.invulnerableFrames > 0) {
            player.invulnerableFrames--
        }
        if (player.squashFrames > 0) {
            player.squashFrames--
        }
        if (player.stretchFrames > 0) {
            player.stretchFrames--
        }
        if (player.boostQueuedTicks > 0) {
            player.boostQueuedTicks--
        }

        // Check if player tapped or held Boost
        val justBoosted = input.consumeBoostJustPressed()
        if (justBoosted || input.boostHeld) {
            player.boostQueuedTicks = 10
        }

        // Mid-air boost hop: allows one active mid-air upward boost per bounce cycle for extra vertical control
        if (justBoosted && player.airBoostAvailable && player.vy > -1.2f) {
            player.vy = minOf(player.vy, AIR_BOOST_IMPULSE_VY)
            player.airBoostAvailable = false
            player.stretchFrames = 6
            onAirBoostTriggered()
        }

        // Horizontal movement: crisp acceleration and instant turn-around responsiveness
        val left = input.moveLeft
        val right = input.moveRight

        when {
            left && !right -> {
                val accel = if (player.vx > 0f) TURN_ACCEL_X else ACCEL_X
                player.vx = (player.vx - accel).coerceAtLeast(-MAX_SPEED_X)
            }
            right && !left -> {
                val accel = if (player.vx < 0f) TURN_ACCEL_X else ACCEL_X
                player.vx = (player.vx + accel).coerceAtMost(MAX_SPEED_X)
            }
            else -> {
                player.vx *= FRICTION_X
                if (abs(player.vx) < 0.04f) {
                    player.vx = 0f
                }
            }
        }

        // Apply gravity
        player.vy = (player.vy + GRAVITY).coerceAtMost(MAX_FALL_SPEED)
    }

    fun computeBounceVelocity(surface: SurfaceType, boostActive: Boolean): Float {
        return when (surface) {
            SurfaceType.SPRING -> if (boostActive) SPRING_BOUNCE_VY * 1.07f else SPRING_BOUNCE_VY
            SurfaceType.DAMPENED -> if (boostActive) BASE_BOUNCE_VY * 0.88f else DAMP_BOUNCE_VY
            SurfaceType.SLOPE -> if (boostActive) BOOST_BOUNCE_VY else BASE_BOUNCE_VY * 1.03f
            SurfaceType.MOVING_PLATFORM -> if (boostActive) BOOST_BOUNCE_VY * 1.03f else BASE_BOUNCE_VY * 1.04f
            SurfaceType.NORMAL -> if (boostActive) BOOST_BOUNCE_VY else BASE_BOUNCE_VY
        }
    }

    fun updateWorldEntities(level: Level, particles: MutableList<PixelParticle>) {
        // Update moving platforms
        for (plat in level.movingPlatforms) {
            plat.prevX = plat.x
            plat.prevY = plat.y
            plat.phase += 0.035f
            if (plat.phase > (2f * PI).toFloat()) {
                plat.phase -= (2f * PI).toFloat()
            }
            if (plat.rangeX > 0f) {
                plat.x = plat.startX + sin(plat.phase * (plat.speedX.coerceAtLeast(0.5f))) * plat.rangeX
            }
            if (plat.rangeY > 0f) {
                plat.y = plat.startY + sin(plat.phase * (plat.speedY.coerceAtLeast(0.5f))) * plat.rangeY
            }
        }

        // Update patrolling hazards
        for (hazard in level.hazards) {
            hazard.phase += 0.045f
            if (hazard.phase > (2f * PI).toFloat()) {
                hazard.phase -= (2f * PI).toFloat()
            }
            if (hazard.rangeX > 0f) {
                hazard.x = hazard.startX + sin(hazard.phase * (hazard.speedX.coerceAtLeast(0.5f))) * hazard.rangeX
            }
            if (hazard.rangeY > 0f) {
                hazard.y = hazard.startY + sin(hazard.phase * (hazard.speedY.coerceAtLeast(0.5f))) * hazard.rangeY
            }
        }

        // Update crumble tiles (crack after bounce, then regenerate after 3 seconds)
        for (state in level.crumbleBlocks.values) {
            if (state.triggered) {
                if (state.ticksRemaining > 0) {
                    state.ticksRemaining--
                    if (state.ticksRemaining == 0) {
                        state.respawnTicks = 180 // Regenerates after 3 seconds so player is never soft-locked
                    }
                } else if (state.respawnTicks > 0) {
                    state.respawnTicks--
                    if (state.respawnTicks == 0) {
                        state.triggered = false
                        state.ticksRemaining = 20
                    }
                }
            }
        }

        // Update collectible animations
        for (item in level.collectibles) {
            if (item.collected && item.collectAnimTicks > 0) {
                item.collectAnimTicks--
            }
        }

        // Update pixel particles
        val iterator = particles.iterator()
        while (iterator.hasNext()) {
            val p = iterator.next()
            p.x += p.vx
            p.y += p.vy
            p.vy += 0.04f
            p.ticksLeft--
            if (p.ticksLeft <= 0) {
                iterator.remove()
            }
        }
    }
}
