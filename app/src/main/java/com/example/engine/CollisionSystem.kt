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

        val prevX = player.x
        val prevY = player.y

        // 1. Horizontal movement & X-axis tile collision (with slope-seam & ledge-lip awareness)
        player.x += player.vx
        resolveHorizontalTileCollisions(player, prevX, level)

        // 2. Vertical movement & Y-axis slope / moving-platform / solid-tile collision
        player.y += player.vy

        val handledBySlope = resolveSlopeCollisions(player, prevY, level, events, particles)
        if (!handledBySlope) {
            val handledByPlatform = resolveMovingPlatformCollisions(player, prevY, level, events, particles)
            if (!handledByPlatform) {
                resolveVerticalTileCollisions(player, prevY, level, events, particles)
            }
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

    private fun resolveHorizontalTileCollisions(player: Player, prevX: Float, level: Level) {
        val r = player.radius
        val currentCenterGx = floor(prevX / Level.TILE_SIZE).toInt()
        val currentFootGy = floor((player.y + r - 0.5f) / Level.TILE_SIZE).toInt()
        val currentCenterGy = floor(player.y / Level.TILE_SIZE).toInt()

        val onSlopeAtFoot = level.getTileAtGrid(currentCenterGx, currentFootGy).let {
            it == TileType.SLOPE_UP_RIGHT || it == TileType.SLOPE_DOWN_RIGHT
        }
        val onSlopeAtCenter = level.getTileAtGrid(currentCenterGx, currentCenterGy).let {
            it == TileType.SLOPE_UP_RIGHT || it == TileType.SLOPE_DOWN_RIGHT
        }

        // Inset vertical bounds slightly so top/bottom grazing doesn't snag on vertical seams
        val minGy = floor((player.y - r + 1.5f) / Level.TILE_SIZE).toInt()
        val maxGy = floor((player.y + r - 1.5f) / Level.TILE_SIZE).toInt()

        if (player.vx > 0f) {
            val rightGx = floor((player.x + r) / Level.TILE_SIZE).toInt()
            for (gy in minGy..maxGy) {
                val tile = level.getTileAtGrid(rightGx, gy)
                if (!level.isSolidTile(tile)) continue

                // If climbing a SLOPE_UP_RIGHT onto a flush platform tile at the same row, do not block
                if ((onSlopeAtFoot && gy == currentFootGy) || (onSlopeAtCenter && gy == currentCenterGy)) {
                    continue
                }

                // Ledge lip forgiveness: if the ball's bottom is within 2.4px of the top of this tile
                // and the space above this tile is open, step up onto the ledge instead of stopping
                val tileTopY = gy * Level.TILE_SIZE
                val aboveTile = level.getTileAtGrid(rightGx, gy - 1)
                if ((player.y + r) - tileTopY <= 2.4f && !level.isSolidTile(aboveTile)) {
                    player.y = tileTopY - r - 0.01f
                    continue
                }

                player.x = rightGx * Level.TILE_SIZE - r - 0.01f
                player.vx = 0f
                break
            }
        } else if (player.vx < 0f) {
            val leftGx = floor((player.x - r) / Level.TILE_SIZE).toInt()
            for (gy in minGy..maxGy) {
                val tile = level.getTileAtGrid(leftGx, gy)
                if (!level.isSolidTile(tile)) continue

                // If climbing a SLOPE_DOWN_RIGHT leftward onto a flush platform tile at the same row, do not block
                if ((onSlopeAtFoot && gy == currentFootGy) || (onSlopeAtCenter && gy == currentCenterGy)) {
                    continue
                }

                // Ledge lip forgiveness
                val tileTopY = gy * Level.TILE_SIZE
                val aboveTile = level.getTileAtGrid(leftGx, gy - 1)
                if ((player.y + r) - tileTopY <= 2.4f && !level.isSolidTile(aboveTile)) {
                    player.y = tileTopY - r - 0.01f
                    continue
                }

                player.x = (leftGx + 1) * Level.TILE_SIZE + r + 0.01f
                player.vx = 0f
                break
            }
        }
    }

    private fun resolveSlopeCollisions(
        player: Player,
        prevY: Float,
        level: Level,
        events: CollisionStepEvents,
        particles: MutableList<PixelParticle>
    ): Boolean {
        val r = player.radius
        val gx = floor(player.x / Level.TILE_SIZE).toInt()
        val footGy = floor((player.y + r) / Level.TILE_SIZE).toInt()
        val centerGy = floor(player.y / Level.TILE_SIZE).toInt()

        for (gy in listOf(centerGy, footGy)) {
            val tile = level.getTileAtGrid(gx, gy)
            if (tile == TileType.SLOPE_UP_RIGHT || tile == TileType.SLOPE_DOWN_RIGHT) {
                val localX = (player.x - gx * Level.TILE_SIZE).coerceIn(0f, Level.TILE_SIZE)
                val floorY = if (tile == TileType.SLOPE_UP_RIGHT) {
                    (gy + 1) * Level.TILE_SIZE - localX
                } else {
                    gy * Level.TILE_SIZE + localX
                }

                // Only resolve top-surface slope bounce if approaching from above (not jumping into underside from below)
                val wasAboveSlope = (prevY + r) <= (floorY + 4.5f)
                if (wasAboveSlope && (player.y + r) >= floorY && player.vy >= 0f) {
                    player.y = floorY - r - 0.01f
                    val boost = player.boostQueuedTicks > 0
                    player.vy = physics.computeBounceVelocity(SurfaceType.SLOPE, boost)
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
    ): Boolean {
        if (player.vy < 0f) return false
        val r = player.radius

        for (plat in level.movingPlatforms) {
            val withinX = (player.x + r * 0.70f) >= plat.x && (player.x - r * 0.70f) <= (plat.x + plat.width)
            // Must have been above (or at) the platform's top surface on the previous tick
            // AND must now be intersecting the platform's top surface!
            val wasAbove = (prevY + r) <= (plat.prevY + 3.5f)
            val isNowTouchingTop = (player.y + r) >= plat.y && (player.y + r) <= (plat.y + plat.height + 3.5f)

            if (withinX && wasAbove && isNowTouchingTop) {
                player.y = plat.y - r - 0.01f
                player.x = (player.x + plat.deltaX).coerceIn(r + 8f, level.pixelWidth - r - 8f)
                val boost = player.boostQueuedTicks > 0
                player.vy = physics.computeBounceVelocity(SurfaceType.MOVING_PLATFORM, boost) + minOf(0f, plat.deltaY)
                onPlayerBounced(player, SurfaceType.MOVING_PLATFORM, boost, events, particles)
                return true
            }
        }
        return false
    }

    private fun resolveVerticalTileCollisions(
        player: Player,
        prevY: Float,
        level: Level,
        events: CollisionStepEvents,
        particles: MutableList<PixelParticle>
    ) {
        val r = player.radius

        if (player.vy >= 0f) {
            val minGx = floor((player.x - r + 1.1f) / Level.TILE_SIZE).toInt()
            val maxGx = floor((player.x + r - 1.1f) / Level.TILE_SIZE).toInt()
            val bottomGy = floor((player.y + r) / Level.TILE_SIZE).toInt()
            val tileTopY = bottomGy * Level.TILE_SIZE

            // Ensure the ball was above or near the top of this tile row before landing
            if ((prevY + r) > tileTopY + 4.5f) return

            var hitTile: TileType? = null
            for (gx in minGx..maxGx) {
                val tile = level.getTileAtGrid(gx, bottomGy)
                if (level.isSolidTile(tile)) {
                    if (hitTile == null || tile == TileType.SPRING_PAD) {
                        hitTile = tile
                    }
                }
            }

            if (hitTile != null) {
                player.y = tileTopY - r - 0.01f
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
            // Ceiling collision when moving upward: use a narrower horizontal inset (r - 1.5f)
            // so grazing the vertical corner of an overhead ledge doesn't bonk the player downward
            val minGx = floor((player.x - r + 1.5f) / Level.TILE_SIZE).toInt()
            val maxGx = floor((player.x + r - 1.5f) / Level.TILE_SIZE).toInt()
            val topGy = floor((player.y - r) / Level.TILE_SIZE).toInt()
            val tileBottomY = (topGy + 1) * Level.TILE_SIZE

            if ((prevY - r) < tileBottomY - 4.5f) return

            for (gx in minGx..maxGx) {
                val tile = level.getTileAtGrid(gx, topGy)
                if (level.isSolidTile(tile)) {
                    player.y = tileBottomY + r + 0.01f
                    player.vy = abs(player.vy) * 0.35f
                    player.squashFrames = 3
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

        // Static spike tiles check (forgiving inner hitbox so pixel corners feel fair)
        val checkRadius = player.radius * 0.62f
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
            if (dist < player.radius * 0.72f) {
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
            val pickupRadius = if (item.type == CollectibleType.RING) player.radius + 4.5f else player.radius + 3.5f
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
            if (dist <= player.radius + 5.8f) {
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
        return dist <= player.radius + 5.5f
    }

    private fun spawnBounceSparks(
        particles: MutableList<PixelParticle>,
        x: Float,
        y: Float,
        count: Int
    ) {
        if (particles.size > 60) return
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
        if (particles.size > 80) return
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
