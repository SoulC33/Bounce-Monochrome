package com.example.engine

import com.example.model.Collectible
import com.example.model.CollectibleType
import com.example.model.Level
import com.example.model.PixelParticle
import com.example.model.Player
import com.example.model.SurfaceType
import com.example.model.TileType
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.random.Random

data class CollisionStepEvents(
    var bouncedSurface: SurfaceType? = null,
    var boostedBounce: Boolean = false,
    val collectedItems: MutableList<Collectible> = mutableListOf(),
    var activatedCheckpoint: Boolean = false,
    var exitUnlockedNow: Boolean = false,
    var hitHazardOrPit: Boolean = false,
    var reachedUnlockedExit: Boolean = false
)

class CollisionSystem(private val physics: Physics) {

    fun stepAndResolve(
        player: Player,
        level: Level,
        particles: MutableList<PixelParticle>
    ): CollisionStepEvents {
        val events = CollisionStepEvents()
        val ringsBefore = level.ringsCollected

        // 1. Horizontal movement & X-axis tile collision
        player.x += player.vx
        resolveHorizontalTileCollisions(player, level)

        // 2. Vertical movement & Y-axis tile / slope / moving-platform collision
        val prevY = player.y
        player.y += player.vy

        // Check slope tiles before standard solid blocks so diagonal ramps feel seamless
        val handledBySlope = resolveSlopeCollisions(player, level, events, particles)
        if (!handledBySlope) {
            resolveMovingPlatformCollisions(player, prevY, level, events, particles)
            resolveVerticalTileCollisions(player, level, events, particles)
        }

        // 3. Check hazards (spikes, patrolling saws/pistons, and bottomless pits)
        if (checkHazardOrPitCollision(player, level)) {
            events.hitHazardOrPit = true
            spawnBurstParticles(particles, player.x, player.y, count = 14)
            return events
        }

        // 4. Check collectibles (Rings, Data Gems, Extra Lives)
        checkCollectibles(player, level, events, particles)
        if (ringsBefore < level.totalRings && level.ringsCollected >= level.totalRings) {
            events.exitUnlockedNow = true
            spawnBurstParticles(
                particles,
                (level.exitGridX + 0.5f) * Level.TILE_SIZE,
                (level.exitGridY + 0.5f) * Level.TILE_SIZE,
                count = 16
            )
        }

        // 5. Check checkpoints
        checkCheckpoints(player, level, events, particles)

        // 6. Check level exit portal
        if (level.isExitUnlocked && checkExitPortal(player, level)) {
            events.reachedUnlockedExit = true
            spawnBurstParticles(particles, player.x, player.y, count = 18)
        }

        return events
    }

    private fun resolveHorizontalTileCollisions(player: Player, level: Level) {
        val r = player.radius
        val minGy = floor((player.y - r + 1.2f) / Level.TILE_SIZE).toInt()
        val maxGy = floor((player.y + r - 1.2f) / Level.TILE_SIZE).toInt()

        if (player.vx > 0f) {
            val rightGx = floor((player.x + r) / Level.TILE_SIZE).toInt()
            for (gy in minGy..maxGy) {
                val tile = level.getTileAtGrid(rightGx, gy)
                if (level.isSolidTile(tile)) {
                    player.x = rightGx * Level.TILE_SIZE - r - 0.01f
                    player.vx = -player.vx * 0.45f // Slight wall rebound
                    break
                }
            }
        } else if (player.vx < 0f) {
            val leftGx = floor((player.x - r) / Level.TILE_SIZE).toInt()
            for (gy in minGy..maxGy) {
                val tile = level.getTileAtGrid(leftGx, gy)
                if (level.isSolidTile(tile)) {
                    player.x = (leftGx + 1) * Level.TILE_SIZE + r + 0.01f
                    player.vx = -player.vx * 0.45f
                    break
                }
            }
        }
    }

    private fun resolveSlopeCollisions(
        player: Player,
        level: Level,
        events: CollisionStepEvents,
        particles: MutableList<PixelParticle>
    ): Boolean {
        val r = player.radius
        val gx = floor(player.x / Level.TILE_SIZE).toInt()
        val footGy = floor((player.y + r) / Level.TILE_SIZE).toInt()
        val centerGy = floor(player.y / Level.TILE_SIZE).toInt()

        for (gy in listOf(footGy, centerGy)) {
            val tile = level.getTileAtGrid(gx, gy)
            if (tile == TileType.SLOPE_UP_RIGHT || tile == TileType.SLOPE_DOWN_RIGHT) {
                val localX = (player.x - gx * Level.TILE_SIZE).coerceIn(0f, Level.TILE_SIZE)
                val floorY = if (tile == TileType.SLOPE_UP_RIGHT) {
                    // Ascending left-to-right: bottom-left (gy+1)*8 to top-right gy*8
                    (gy + 1) * Level.TILE_SIZE - localX
                } else {
                    // Descending left-to-right: top-left gy*8 to bottom-right (gy+1)*8
                    gy * Level.TILE_SIZE + localX
                }

                if (player.y + r >= floorY && player.vy >= 0f) {
                    player.y = floorY - r
                    val boost = player.boostQueuedTicks > 0
                    player.vy = physics.computeBounceVelocity(SurfaceType.SLOPE, boost)
                    // Slight slope horizontal nudge
                    val slopeNudge = if (tile == TileType.SLOPE_UP_RIGHT) -0.22f else 0.22f
                    player.vx = (player.vx + slopeNudge).coerceIn(-Physics.MAX_SPEED_X * 1.15f, Physics.MAX_SPEED_X * 1.15f)
                    onPlayerBounced(player, SurfaceType.SLOPE, boost, events, particles)
                    return true
                }
            }
        }
        return false
    }

    private fun resolveMovingPlatformCollisions(
        player: Player,
        prevY: Float,
        level: Level,
        events: CollisionStepEvents,
        particles: MutableList<PixelParticle>
    ) {
        if (player.vy < 0f) return
        val r = player.radius

        for (plat in level.movingPlatforms) {
            val withinX = player.x + r * 0.75f >= plat.x && player.x - r * 0.75f <= plat.x + plat.width
            val wasAbove = (prevY + r) <= (plat.prevY + 2.5f)
            val isNowTouching = (player.y + r) >= plat.y && (player.y - r) <= (plat.y + plat.height)

            if (withinX && (wasAbove || isNowTouching)) {
                player.y = plat.y - r - 0.01f
                player.x += plat.deltaX
                val boost = player.boostQueuedTicks > 0
                player.vy = physics.computeBounceVelocity(SurfaceType.MOVING_PLATFORM, boost) + minOf(0f, plat.deltaY)
                onPlayerBounced(player, SurfaceType.MOVING_PLATFORM, boost, events, particles)
                return
            }
        }
    }

    private fun resolveVerticalTileCollisions(
        player: Player,
        level: Level,
        events: CollisionStepEvents,
        particles: MutableList<PixelParticle>
    ) {
        val r = player.radius
        val minGx = floor((player.x - r + 1.0f) / Level.TILE_SIZE).toInt()
        val maxGx = floor((player.x + r - 1.0f) / Level.TILE_SIZE).toInt()

        if (player.vy >= 0f) {
            val bottomGy = floor((player.y + r) / Level.TILE_SIZE).toInt()
            var hitTile: TileType? = null
            var hitGx = minGx

            for (gx in minGx..maxGx) {
                val tile = level.getTileAtGrid(gx, bottomGy)
                if (level.isSolidTile(tile)) {
                    // Prioritize spring pad if touching multiple tiles
                    if (hitTile == null || tile == TileType.SPRING_PAD) {
                        hitTile = tile
                        hitGx = gx
                    }
                }
            }

            if (hitTile != null) {
                player.y = bottomGy * Level.TILE_SIZE - r - 0.01f
                val surface = when (hitTile) {
                    TileType.SPRING_PAD -> SurfaceType.SPRING
                    TileType.DAMP_FLOOR -> SurfaceType.DAMPENED
                    else -> SurfaceType.NORMAL
                }

                // Trigger crumble block if any touched tile is a crumble tile
                for (gx in minGx..maxGx) {
                    if (level.getTileAtGrid(gx, bottomGy) == TileType.CRUMBLE_TILE) {
                        level.crumbleBlocks[gx to bottomGy]?.triggered = true
                    }
                }

                val boost = player.boostQueuedTicks > 0
                player.vy = physics.computeBounceVelocity(surface, boost)
                onPlayerBounced(player, surface, boost, events, particles)
            }
        } else {
            // Ceiling collision when moving upward
            val topGy = floor((player.y - r) / Level.TILE_SIZE).toInt()
            for (gx in minGx..maxGx) {
                val tile = level.getTileAtGrid(gx, topGy)
                if (level.isSolidTile(tile)) {
                    player.y = (topGy + 1) * Level.TILE_SIZE + r + 0.01f
                    player.vy = abs(player.vy) * 0.45f
                    player.squashFrames = 4
                    spawnBounceSparks(particles, player.x, player.y - r, 3)
                    break
                }
            }
        }
    }

    private fun onPlayerBounced(
        player: Player,
        surface: SurfaceType,
        boosted: Boolean,
        events: CollisionStepEvents,
        particles: MutableList<PixelParticle>
    ) {
        player.lastBounceSurface = surface
        player.bounceCount++
        player.airBoostAvailable = true
        player.boostQueuedTicks = 0
        player.squashFrames = if (surface == SurfaceType.SPRING || boosted) 6 else 4
        events.bouncedSurface = surface
        events.boostedBounce = boosted
        spawnBounceSparks(
            particles,
            player.x,
            player.y + player.radius,
            count = if (surface == SurfaceType.SPRING || boosted) 6 else 3
        )
    }

    private fun checkHazardOrPitCollision(player: Player, level: Level): Boolean {
        // Bottomless pit check
        if (player.y - player.radius > level.pixelHeight + 6f) {
            return true
        }
        if (player.invulnerableFrames > 0) {
            return false
        }

        // Static spike tiles check (slightly forgiving inner hitbox so pixel corners feel fair)
        val checkRadius = player.radius * 0.68f
        val minGx = floor((player.x - checkRadius) / Level.TILE_SIZE).toInt()
        val maxGx = floor((player.x + checkRadius) / Level.TILE_SIZE).toInt()
        val minGy = floor((player.y - checkRadius) / Level.TILE_SIZE).toInt()
        val maxGy = floor((player.y + checkRadius) / Level.TILE_SIZE).toInt()

        for (gy in minGy..maxGy) {
            for (gx in minGx..maxGx) {
                val tile = level.getTileAtGrid(gx, gy)
                if (level.isSpikeTile(tile)) {
                    return true
                }
            }
        }

        // Dynamic patrolling hazards check
        for (hazard in level.hazards) {
            val closestX = player.x.coerceIn(hazard.x, hazard.x + hazard.width)
            val closestY = player.y.coerceIn(hazard.y, hazard.y + hazard.height)
            val dist = hypot(player.x - closestX, player.y - closestY)
            if (dist < player.radius * 0.78f) {
                return true
            }
        }

        return false
    }

    private fun checkCollectibles(
        player: Player,
        level: Level,
        events: CollisionStepEvents,
        particles: MutableList<PixelParticle>
    ) {
        for (item in level.collectibles) {
            if (item.collected) continue
            val pickupRadius = if (item.type == CollectibleType.RING) player.radius + 4.2f else player.radius + 3.2f
            val dist = hypot(player.x - item.x, player.y - item.y)
            if (dist <= pickupRadius) {
                item.collected = true
                item.collectAnimTicks = 12
                events.collectedItems.add(item)
                spawnBounceSparks(particles, item.x, item.y, count = 7)
            }
        }
    }

    private fun checkCheckpoints(
        player: Player,
        level: Level,
        events: CollisionStepEvents,
        particles: MutableList<PixelParticle>
    ) {
        for (cp in level.checkpoints) {
            if (cp.activated) continue
            val dist = hypot(player.x - cp.worldX, player.y - cp.worldY)
            if (dist <= player.radius + 5.5f) {
                cp.activated = true
                player.respawnX = cp.worldX
                player.respawnY = cp.worldY - 2f
                events.activatedCheckpoint = true
                spawnBurstParticles(particles, cp.worldX, cp.worldY, count = 10)
            }
        }
    }

    private fun checkExitPortal(player: Player, level: Level): Boolean {
        val exitCenterX = (level.exitGridX + 0.5f) * Level.TILE_SIZE
        val exitCenterY = (level.exitGridY + 0.5f) * Level.TILE_SIZE
        val dist = hypot(player.x - exitCenterX, player.y - exitCenterY)
        return dist <= player.radius + 5.0f
    }

    private fun spawnBounceSparks(
        particles: MutableList<PixelParticle>,
        x: Float,
        y: Float,
        count: Int
    ) {
        repeat(count) {
            val vx = (Random.nextFloat() - 0.5f) * 1.4f
            val vy = -Random.nextFloat() * 1.1f - 0.2f
            particles.add(
                PixelParticle(
                    x = x,
                    y = y,
                    vx = vx,
                    vy = vy,
                    ticksLeft = Random.nextInt(10, 18),
                    maxTicks = 18,
                    bright = it % 2 == 0
                )
            )
        }
    }

    private fun spawnBurstParticles(
        particles: MutableList<PixelParticle>,
        x: Float,
        y: Float,
        count: Int
    ) {
        repeat(count) { i ->
            val angle = (i.toFloat() / count) * 6.28318f
            val speed = 0.6f + Random.nextFloat() * 1.1f
            particles.add(
                PixelParticle(
                    x = x,
                    y = y,
                    vx = kotlin.math.cos(angle) * speed,
                    vy = kotlin.math.sin(angle) * speed - 0.3f,
                    ticksLeft = Random.nextInt(14, 26),
                    maxTicks = 26,
                    bright = i % 2 == 0
                )
            )
        }
    }
}
