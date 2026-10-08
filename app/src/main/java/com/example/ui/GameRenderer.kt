package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.testTag
import com.example.engine.Camera
import com.example.model.ActiveMonoColors
import com.example.model.CollectibleType
import com.example.model.GameSettings
import com.example.model.HazardType
import com.example.model.Level
import com.example.model.PixelParticle
import com.example.model.Player
import com.example.model.TileType
import com.example.model.toColors
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
fun GameRenderer(
    level: Level,
    player: Player,
    camera: Camera,
    particles: List<PixelParticle>,
    settings: GameSettings,
    frameTick: Long,
    modifier: Modifier = Modifier
) {
    val palette = settings.paletteMode.toColors()

    Box(
        modifier = modifier
            .background(palette.bgDeep)
            .testTag("game_renderer_canvas")
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Reference frameTick so Compose invalidates Canvas every 60Hz tick
            val tick = frameTick

            val logicalW = Camera.LOGICAL_WIDTH
            val logicalH = Camera.LOGICAL_HEIGHT

            // Preserve exact aspect ratio without stretching pixels
            val scale = min(size.width / logicalW, size.height / logicalH)
            val viewportPxW = logicalW * scale
            val viewportPxH = logicalH * scale
            val offsetX = (size.width - viewportPxW) * 0.5f
            val offsetY = (size.height - viewportPxH) * 0.5f

            // 1. Draw retro viewport background
            drawRect(
                color = palette.bgDeep,
                topLeft = Offset(offsetX, offsetY),
                size = Size(viewportPxW, viewportPxH)
            )

            // Subtle background parallax pixel horizon dots
            drawRetroBackgroundGrid(
                offsetX = offsetX,
                offsetY = offsetY,
                scale = scale,
                camX = camera.snappedX,
                camY = camera.snappedY,
                palette = palette
            )

            val camX = camera.snappedX
            val camY = camera.snappedY

            val minTileX = (camX / Level.TILE_SIZE.toInt() - 1).coerceAtLeast(0)
            val maxTileX = ((camX + logicalW.toInt()) / Level.TILE_SIZE.toInt() + 1)
                .coerceAtMost(level.widthInTiles - 1)
            val minTileY = (camY / Level.TILE_SIZE.toInt() - 1).coerceAtLeast(0)
            val maxTileY = ((camY + logicalH.toInt()) / Level.TILE_SIZE.toInt() + 1)
                .coerceAtMost(level.heightInTiles - 1)

            // 2. Draw visible tiles
            for (gy in minTileY..maxTileY) {
                for (gx in minTileX..maxTileX) {
                    val rawTile = level.tiles[gy][gx]
                    if (rawTile == TileType.EMPTY) continue

                    val lx = gx * 8 - camX
                    val ly = gy * 8 - camY

                    when (rawTile) {
                        TileType.SOLID_BRICK -> drawBrickTile(offsetX, offsetY, scale, lx, ly, palette)
                        TileType.SOLID_METAL -> drawMetalTile(offsetX, offsetY, scale, lx, ly, palette)
                        TileType.SPRING_PAD -> drawSpringTile(offsetX, offsetY, scale, lx, ly, palette, tick)
                        TileType.DAMP_FLOOR -> drawDampTile(offsetX, offsetY, scale, lx, ly, palette)
                        TileType.CRUMBLE_TILE -> {
                            val state = level.crumbleBlocks[gx to gy]
                            if (state == null || state.isSolid) {
                                val jitter = if (state?.triggered == true && (tick % 2L == 0L)) 1 else 0
                                drawCrumbleTile(offsetX, offsetY, scale, lx + jitter, ly, palette, state?.triggered == true)
                            } else {
                                drawRegeneratingCrumbleOutline(offsetX, offsetY, scale, lx, ly, palette)
                            }
                        }
                        TileType.SLOPE_UP_RIGHT -> drawSlopeUpRightTile(offsetX, offsetY, scale, lx, ly, palette)
                        TileType.SLOPE_DOWN_RIGHT -> drawSlopeDownRightTile(offsetX, offsetY, scale, lx, ly, palette)
                        TileType.SPIKE_UP -> drawSpikeUpTile(offsetX, offsetY, scale, lx, ly, palette)
                        TileType.SPIKE_DOWN -> drawSpikeDownTile(offsetX, offsetY, scale, lx, ly, palette)
                        TileType.SPIKE_LEFT, TileType.SPIKE_RIGHT -> drawSpikeUpTile(offsetX, offsetY, scale, lx, ly, palette)
                        TileType.CHECKPOINT -> {
                            val cp = level.checkpoints.firstOrNull { it.gridX == gx && it.gridY == gy }
                            drawCheckpointTile(offsetX, offsetY, scale, lx, ly, palette, cp?.activated == true, tick)
                        }
                        TileType.EXIT_PORTAL -> {
                            drawExitPortalTile(offsetX, offsetY, scale, lx, ly, palette, level.isExitUnlocked, tick)
                        }
                        TileType.EMPTY -> {}
                    }
                }
            }

            // 3. Draw Moving Platforms
            for (plat in level.movingPlatforms) {
                val lx = plat.x.roundToInt() - camX
                val ly = plat.y.roundToInt() - camY
                val pw = plat.width.roundToInt()
                val ph = plat.height.roundToInt()
                if (lx + pw >= 0 && lx <= logicalW && ly + ph >= 0 && ly <= logicalH) {
                    drawMovingPlatform(offsetX, offsetY, scale, lx, ly, pw, ph, palette, tick)
                }
            }

            // 4. Draw Collectibles (Rings, Data Gems, 1-UP Orbs)
            for (item in level.collectibles) {
                if (item.collected && item.collectAnimTicks <= 0) continue
                val lx = item.x.roundToInt() - camX
                val ly = item.y.roundToInt() - camY
                if (lx in -8..logicalW.toInt() + 8 && ly in -8..logicalH.toInt() + 8) {
                    if (item.collected) {
                        // Burst ring effect on pickup
                        drawLogicalHollowRect(offsetX, offsetY, scale, lx - 4, ly - 4, 8, 8, palette.pixelBright)
                    } else {
                        val bob = if ((tick / 14L + item.id) % 2L == 0L) 0 else -1
                        when (item.type) {
                            CollectibleType.RING -> drawCollectibleRing(offsetX, offsetY, scale, lx, ly + bob, palette)
                            CollectibleType.DATA_GEM -> drawCollectibleGem(offsetX, offsetY, scale, lx, ly + bob, palette)
                            CollectibleType.EXTRA_LIFE -> drawCollectibleLife(offsetX, offsetY, scale, lx, ly + bob, palette, tick)
                        }
                    }
                }
            }

            // 5. Draw Patrolling Hazards
            for (hazard in level.hazards) {
                val lx = hazard.x.roundToInt() - camX
                val ly = hazard.y.roundToInt() - camY
                if (lx in -8..logicalW.toInt() + 8 && ly in -8..logicalH.toInt() + 8) {
                    drawHazardEntity(offsetX, offsetY, scale, lx, ly, hazard.type, palette, tick)
                }
            }

            // 6. Draw Player Trail & Bouncing Pixel Ball
            val trailSize = player.trail.size
            for (idx in 0 until trailSize) {
                val tp = player.trail.elementAtOrNull(idx) ?: continue
                val tlx = tp.x.roundToInt() - camX
                val tly = tp.y.roundToInt() - camY
                if (tlx in 0 until logicalW.toInt() && tly in 0 until logicalH.toInt()) {
                    val color = if (idx >= trailSize - 2) palette.pixelMid else palette.pixelDim
                    drawLogicalPixel(offsetX, offsetY, scale, tlx, tly, color)
                }
            }

            val px = player.x.roundToInt() - camX
            val py = player.y.roundToInt() - camY
            val blinkSkip = player.invulnerableFrames > 0 && (player.invulnerableFrames / 3) % 2 == 1
            if (!blinkSkip) {
                drawPixelBall(
                    offsetX = offsetX,
                    offsetY = offsetY,
                    scale = scale,
                    centerX = px,
                    centerY = py,
                    squash = player.squashFrames > 0,
                    stretch = player.stretchFrames > 0,
                    palette = palette
                )
            }

            // 7. Draw Pixel Sparks / Particles
            for (i in 0 until particles.size) {
                val p = particles.getOrNull(i) ?: continue
                val plx = p.x.roundToInt() - camX
                val ply = p.y.roundToInt() - camY
                if (plx in 0 until logicalW.toInt() && ply in 0 until logicalH.toInt()) {
                    drawLogicalPixel(
                        offsetX,
                        offsetY,
                        scale,
                        plx,
                        ply,
                        if (p.bright) palette.pixelBright else palette.pixelMid
                    )
                }
            }

            // 8. Optional Retro LCD Pixel Grid & Scanlines
            if (settings.pixelGridEnabled && scale >= 3f) {
                drawPixelMatrixGrid(offsetX, offsetY, scale, logicalW.toInt(), logicalH.toInt(), palette.bgDeep)
            }
            if (settings.scanlinesEnabled) {
                drawScanlinesOverlay(offsetX, offsetY, scale, logicalW.toInt(), logicalH.toInt(), palette.bgDeep)
            }

            // 9. Crisp 2-pixel outer LCD frame border
            drawLogicalHollowRect(
                offsetX = offsetX,
                offsetY = offsetY,
                scale = scale,
                lx = 0,
                ly = 0,
                w = logicalW.toInt(),
                h = logicalH.toInt(),
                color = palette.pixelMid
            )
        }
    }
}

private fun DrawScope.drawLogicalPixel(
    offsetX: Float,
    offsetY: Float,
    scale: Float,
    lx: Int,
    ly: Int,
    color: Color
) {
    if (lx < 0 || lx >= Camera.LOGICAL_WIDTH.toInt() || ly < 0 || ly >= Camera.LOGICAL_HEIGHT.toInt()) return
    drawRect(
        color = color,
        topLeft = Offset(offsetX + lx * scale, offsetY + ly * scale),
        size = Size(scale + 0.35f, scale + 0.35f)
    )
}

private fun DrawScope.drawLogicalRect(
    offsetX: Float,
    offsetY: Float,
    scale: Float,
    lx: Int,
    ly: Int,
    w: Int,
    h: Int,
    color: Color
) {
    val startX = lx.coerceIn(0, Camera.LOGICAL_WIDTH.toInt())
    val startY = ly.coerceIn(0, Camera.LOGICAL_HEIGHT.toInt())
    val endX = (lx + w).coerceIn(0, Camera.LOGICAL_WIDTH.toInt())
    val endY = (ly + h).coerceIn(0, Camera.LOGICAL_HEIGHT.toInt())
    if (endX <= startX || endY <= startY) return

    drawRect(
        color = color,
        topLeft = Offset(offsetX + startX * scale, offsetY + startY * scale),
        size = Size((endX - startX) * scale, (endY - startY) * scale)
    )
}

private fun DrawScope.drawLogicalHollowRect(
    offsetX: Float,
    offsetY: Float,
    scale: Float,
    lx: Int,
    ly: Int,
    w: Int,
    h: Int,
    color: Color
) {
    drawLogicalRect(offsetX, offsetY, scale, lx, ly, w, 1, color)
    drawLogicalRect(offsetX, offsetY, scale, lx, ly + h - 1, w, 1, color)
    drawLogicalRect(offsetX, offsetY, scale, lx, ly, 1, h, color)
    drawLogicalRect(offsetX, offsetY, scale, lx + w - 1, ly, 1, h, color)
}

private fun DrawScope.drawRetroBackgroundGrid(
    offsetX: Float,
    offsetY: Float,
    scale: Float,
    camX: Int,
    camY: Int,
    palette: ActiveMonoColors
) {
    val parallaxX = (camX / 3) % 16
    val parallaxY = (camY / 3) % 16
    var gx = -parallaxX
    while (gx < Camera.LOGICAL_WIDTH.toInt()) {
        var gy = -parallaxY
        while (gy < Camera.LOGICAL_HEIGHT.toInt()) {
            drawLogicalPixel(offsetX, offsetY, scale, gx, gy, palette.bgSurface)
            gy += 16
        }
        gx += 16
    }
}

private fun DrawScope.drawBrickTile(
    offsetX: Float,
    offsetY: Float,
    scale: Float,
    lx: Int,
    ly: Int,
    palette: ActiveMonoColors
) {
    // Base fill
    drawLogicalRect(offsetX, offsetY, scale, lx, ly, 8, 8, palette.pixelMid)
    // Top bright pixel edge for crisp platform readability
    drawLogicalRect(offsetX, offsetY, scale, lx, ly, 8, 1, palette.pixelBright)
    // Brick mortar lines
    drawLogicalRect(offsetX, offsetY, scale, lx, ly + 3, 8, 1, palette.bgDeep)
    drawLogicalRect(offsetX, offsetY, scale, lx, ly + 7, 8, 1, palette.bgDeep)
    drawLogicalRect(offsetX, offsetY, scale, lx + 3, ly + 1, 1, 2, palette.bgDeep)
    drawLogicalRect(offsetX, offsetY, scale, lx + 7, ly + 4, 1, 3, palette.bgDeep)
}

private fun DrawScope.drawMetalTile(
    offsetX: Float,
    offsetY: Float,
    scale: Float,
    lx: Int,
    ly: Int,
    palette: ActiveMonoColors
) {
    drawLogicalRect(offsetX, offsetY, scale, lx, ly, 8, 8, palette.pixelDim)
    drawLogicalHollowRect(offsetX, offsetY, scale, lx, ly, 8, 8, palette.pixelMid)
    // Corner rivets
    drawLogicalPixel(offsetX, offsetY, scale, lx + 2, ly + 2, palette.pixelBright)
    drawLogicalPixel(offsetX, offsetY, scale, lx + 5, ly + 2, palette.pixelBright)
    drawLogicalPixel(offsetX, offsetY, scale, lx + 2, ly + 5, palette.pixelBright)
    drawLogicalPixel(offsetX, offsetY, scale, lx + 5, ly + 5, palette.pixelBright)
}

private fun DrawScope.drawSpringTile(
    offsetX: Float,
    offsetY: Float,
    scale: Float,
    lx: Int,
    ly: Int,
    palette: ActiveMonoColors,
    tick: Long
) {
    // Bright top bounce plate
    val pulse = if ((tick / 10L) % 2L == 0L) 0 else 1
    drawLogicalRect(offsetX, offsetY, scale, lx, ly + pulse, 8, 2, palette.pixelBright)
    // Diagonal spring coils
    drawLogicalPixel(offsetX, offsetY, scale, lx + 1, ly + 3, palette.pixelMid)
    drawLogicalPixel(offsetX, offsetY, scale, lx + 6, ly + 3, palette.pixelMid)
    drawLogicalRect(offsetX, offsetY, scale, lx + 2, ly + 4, 4, 1, palette.pixelBright)
    drawLogicalPixel(offsetX, offsetY, scale, lx + 1, ly + 5, palette.pixelMid)
    drawLogicalPixel(offsetX, offsetY, scale, lx + 6, ly + 5, palette.pixelMid)
    // Solid base anchor
    drawLogicalRect(offsetX, offsetY, scale, lx, ly + 6, 8, 2, palette.pixelMid)
}

private fun DrawScope.drawDampTile(
    offsetX: Float,
    offsetY: Float,
    scale: Float,
    lx: Int,
    ly: Int,
    palette: ActiveMonoColors
) {
    drawLogicalRect(offsetX, offsetY, scale, lx, ly, 8, 8, palette.bgSurface)
    drawLogicalRect(offsetX, offsetY, scale, lx, ly, 8, 1, palette.pixelDim)
    for (py in 1 until 8 step 2) {
        for (px in (py % 2) until 8 step 2) {
            drawLogicalPixel(offsetX, offsetY, scale, lx + px, ly + py, palette.pixelDim)
        }
    }
}

private fun DrawScope.drawCrumbleTile(
    offsetX: Float,
    offsetY: Float,
    scale: Float,
    lx: Int,
    ly: Int,
    palette: ActiveMonoColors,
    triggered: Boolean
) {
    val mainColor = if (triggered) palette.pixelDim else palette.pixelMid
    drawLogicalRect(offsetX, offsetY, scale, lx, ly, 8, 6, mainColor)
    drawLogicalRect(offsetX, offsetY, scale, lx, ly, 8, 1, palette.pixelBright)
    // Crack zigzag pixels
    drawLogicalPixel(offsetX, offsetY, scale, lx + 2, ly + 2, palette.bgDeep)
    drawLogicalPixel(offsetX, offsetY, scale, lx + 3, ly + 3, palette.bgDeep)
    drawLogicalPixel(offsetX, offsetY, scale, lx + 5, ly + 2, palette.bgDeep)
    drawLogicalPixel(offsetX, offsetY, scale, lx + 4, ly + 4, palette.bgDeep)
    // jagged bottom teeth
    drawLogicalPixel(offsetX, offsetY, scale, lx + 1, ly + 6, mainColor)
    drawLogicalPixel(offsetX, offsetY, scale, lx + 4, ly + 6, mainColor)
    drawLogicalPixel(offsetX, offsetY, scale, lx + 6, ly + 6, mainColor)
}

private fun DrawScope.drawRegeneratingCrumbleOutline(
    offsetX: Float,
    offsetY: Float,
    scale: Float,
    lx: Int,
    ly: Int,
    palette: ActiveMonoColors
) {
    drawLogicalPixel(offsetX, offsetY, scale, lx + 1, ly + 1, palette.pixelDim)
    drawLogicalPixel(offsetX, offsetY, scale, lx + 6, ly + 1, palette.pixelDim)
    drawLogicalPixel(offsetX, offsetY, scale, lx + 1, ly + 5, palette.pixelDim)
    drawLogicalPixel(offsetX, offsetY, scale, lx + 6, ly + 5, palette.pixelDim)
}

private fun DrawScope.drawSlopeUpRightTile(
    offsetX: Float,
    offsetY: Float,
    scale: Float,
    lx: Int,
    ly: Int,
    palette: ActiveMonoColors
) {
    for (col in 0 until 8) {
        val topRow = 7 - col
        drawLogicalRect(offsetX, offsetY, scale, lx + col, ly + topRow, 1, 8 - topRow, palette.pixelMid)
        drawLogicalPixel(offsetX, offsetY, scale, lx + col, ly + topRow, palette.pixelBright)
    }
}

private fun DrawScope.drawSlopeDownRightTile(
    offsetX: Float,
    offsetY: Float,
    scale: Float,
    lx: Int,
    ly: Int,
    palette: ActiveMonoColors
) {
    for (col in 0 until 8) {
        val topRow = col
        drawLogicalRect(offsetX, offsetY, scale, lx + col, ly + topRow, 1, 8 - topRow, palette.pixelMid)
        drawLogicalPixel(offsetX, offsetY, scale, lx + col, ly + topRow, palette.pixelBright)
    }
}

private fun DrawScope.drawSpikeUpTile(
    offsetX: Float,
    offsetY: Float,
    scale: Float,
    lx: Int,
    ly: Int,
    palette: ActiveMonoColors
) {
    // Two crisp 4px-wide pixel spikes pointing upward
    for (spikeOffset in listOf(0, 4)) {
        val sx = lx + spikeOffset
        drawLogicalPixel(offsetX, offsetY, scale, sx + 1, ly + 2, palette.pixelBright)
        drawLogicalPixel(offsetX, offsetY, scale, sx + 2, ly + 2, palette.pixelBright)
        drawLogicalRect(offsetX, offsetY, scale, sx + 1, ly + 3, 2, 2, palette.pixelBright)
        drawLogicalRect(offsetX, offsetY, scale, sx, ly + 5, 4, 3, palette.pixelMid)
        drawLogicalPixel(offsetX, offsetY, scale, sx + 1, ly + 5, palette.pixelBright)
    }
}

private fun DrawScope.drawSpikeDownTile(
    offsetX: Float,
    offsetY: Float,
    scale: Float,
    lx: Int,
    ly: Int,
    palette: ActiveMonoColors
) {
    for (spikeOffset in listOf(0, 4)) {
        val sx = lx + spikeOffset
        drawLogicalRect(offsetX, offsetY, scale, sx, ly, 4, 3, palette.pixelMid)
        drawLogicalRect(offsetX, offsetY, scale, sx + 1, ly + 3, 2, 3, palette.pixelBright)
        drawLogicalPixel(offsetX, offsetY, scale, sx + 1, ly + 6, palette.pixelBright)
    }
}

private fun DrawScope.drawCheckpointTile(
    offsetX: Float,
    offsetY: Float,
    scale: Float,
    lx: Int,
    ly: Int,
    palette: ActiveMonoColors,
    activated: Boolean,
    tick: Long
) {
    // Pole
    drawLogicalRect(offsetX, offsetY, scale, lx + 2, ly + 1, 1, 7, palette.pixelBright)
    // Base
    drawLogicalRect(offsetX, offsetY, scale, lx + 1, ly + 7, 4, 1, palette.pixelMid)
    // Flag
    val flagColor = if (activated) palette.pixelBright else palette.pixelDim
    val wave = if (activated && (tick / 8L) % 2L == 0L) 1 else 0
    drawLogicalRect(offsetX, offsetY, scale, lx + 3, ly + 1 + wave, 4, 3, flagColor)
}

private fun DrawScope.drawExitPortalTile(
    offsetX: Float,
    offsetY: Float,
    scale: Float,
    lx: Int,
    ly: Int,
    palette: ActiveMonoColors,
    unlocked: Boolean,
    tick: Long
) {
    // Outer archway (spans 10x10 logical pixels centered around tile)
    drawLogicalHollowRect(offsetX, offsetY, scale, lx - 1, ly - 2, 10, 10, palette.pixelBright)
    if (!unlocked) {
        // Locked gate bars
        drawLogicalRect(offsetX, offsetY, scale, lx + 1, ly, 1, 7, palette.pixelDim)
        drawLogicalRect(offsetX, offsetY, scale, lx + 3, ly, 1, 7, palette.pixelDim)
        drawLogicalRect(offsetX, offsetY, scale, lx + 5, ly, 1, 7, palette.pixelDim)
        drawLogicalRect(offsetX, offsetY, scale, lx, ly + 3, 8, 1, palette.pixelMid)
    } else {
        // Animated open pixel vortex
        val phase = ((tick / 6L) % 3L).toInt()
        val innerColor = if (phase == 0) palette.pixelBright else palette.pixelMid
        drawLogicalHollowRect(offsetX, offsetY, scale, lx + 1, ly, 6, 6, innerColor)
        if (phase != 1) {
            drawLogicalRect(offsetX, offsetY, scale, lx + 3, ly + 2, 2, 2, palette.pixelBright)
        }
    }
}

private fun DrawScope.drawMovingPlatform(
    offsetX: Float,
    offsetY: Float,
    scale: Float,
    lx: Int,
    ly: Int,
    pw: Int,
    ph: Int,
    palette: ActiveMonoColors,
    tick: Long
) {
    drawLogicalRect(offsetX, offsetY, scale, lx, ly, pw, ph, palette.pixelMid)
    drawLogicalRect(offsetX, offsetY, scale, lx, ly, pw, 1, palette.pixelBright)
    drawLogicalRect(offsetX, offsetY, scale, lx + 1, ly + ph - 1, pw - 2, 1, palette.pixelDim)
    // Animated track chevrons
    val shift = ((tick / 6L) % 4L).toInt()
    var cx = lx + 2 + shift
    while (cx < lx + pw - 2) {
        drawLogicalPixel(offsetX, offsetY, scale, cx, ly + 2, palette.bgDeep)
        cx += 4
    }
}

private fun DrawScope.drawCollectibleRing(
    offsetX: Float,
    offsetY: Float,
    scale: Float,
    centerX: Int,
    centerY: Int,
    palette: ActiveMonoColors
) {
    // Classic Nokia Bounce vertical pixel hoop (5x7 logical pixels with hollow center)
    val left = centerX - 2
    val top = centerY - 3
    drawLogicalRect(offsetX, offsetY, scale, left + 1, top, 3, 1, palette.pixelBright)
    drawLogicalRect(offsetX, offsetY, scale, left + 1, top + 6, 3, 1, palette.pixelBright)
    drawLogicalRect(offsetX, offsetY, scale, left, top + 1, 1, 5, palette.pixelBright)
    drawLogicalRect(offsetX, offsetY, scale, left + 4, top + 1, 1, 5, palette.pixelMid)
    drawLogicalPixel(offsetX, offsetY, scale, left + 4, top + 1, palette.pixelBright)
}

private fun DrawScope.drawCollectibleGem(
    offsetX: Float,
    offsetY: Float,
    scale: Float,
    centerX: Int,
    centerY: Int,
    palette: ActiveMonoColors
) {
    // 5x5 pixel diamond data gem
    drawLogicalPixel(offsetX, offsetY, scale, centerX, centerY - 2, palette.pixelBright)
    drawLogicalRect(offsetX, offsetY, scale, centerX - 1, centerY - 1, 3, 1, palette.pixelBright)
    drawLogicalRect(offsetX, offsetY, scale, centerX - 2, centerY, 5, 1, palette.pixelMid)
    drawLogicalPixel(offsetX, offsetY, scale, centerX, centerY, palette.pixelBright)
    drawLogicalRect(offsetX, offsetY, scale, centerX - 1, centerY + 1, 3, 1, palette.pixelMid)
    drawLogicalPixel(offsetX, offsetY, scale, centerX, centerY + 2, palette.pixelBright)
}

private fun DrawScope.drawCollectibleLife(
    offsetX: Float,
    offsetY: Float,
    scale: Float,
    centerX: Int,
    centerY: Int,
    palette: ActiveMonoColors,
    tick: Long
) {
    val c = if ((tick / 8L) % 2L == 0L) palette.pixelBright else palette.pixelMid
    drawLogicalHollowRect(offsetX, offsetY, scale, centerX - 2, centerY - 2, 5, 5, c)
    drawLogicalPixel(offsetX, offsetY, scale, centerX, centerY, palette.pixelBright)
}

private fun DrawScope.drawHazardEntity(
    offsetX: Float,
    offsetY: Float,
    scale: Float,
    lx: Int,
    ly: Int,
    type: HazardType,
    palette: ActiveMonoColors,
    tick: Long
) {
    when (type) {
        HazardType.PATROL_BLADE, HazardType.SPARK_DRONE -> {
            // Rotating 6x6 pixel saw-blade hazard
            drawLogicalRect(offsetX, offsetY, scale, lx + 1, ly + 1, 4, 4, palette.pixelBright)
            drawLogicalRect(offsetX, offsetY, scale, lx + 2, ly + 2, 2, 2, palette.bgDeep)
            val spin = ((tick / 4L) % 2L).toInt()
            if (spin == 0) {
                drawLogicalPixel(offsetX, offsetY, scale, lx + 2, ly, palette.pixelBright)
                drawLogicalPixel(offsetX, offsetY, scale, lx + 3, ly + 5, palette.pixelBright)
                drawLogicalPixel(offsetX, offsetY, scale, lx, ly + 3, palette.pixelBright)
                drawLogicalPixel(offsetX, offsetY, scale, lx + 5, ly + 2, palette.pixelBright)
            } else {
                drawLogicalPixel(offsetX, offsetY, scale, lx, ly, palette.pixelBright)
                drawLogicalPixel(offsetX, offsetY, scale, lx + 5, ly, palette.pixelBright)
                drawLogicalPixel(offsetX, offsetY, scale, lx, ly + 5, palette.pixelBright)
                drawLogicalPixel(offsetX, offsetY, scale, lx + 5, ly + 5, palette.pixelBright)
            }
        }
        HazardType.PISTON_CRUSHER -> {
            drawLogicalRect(offsetX, offsetY, scale, lx, ly, 6, 6, palette.pixelMid)
            drawLogicalHollowRect(offsetX, offsetY, scale, lx, ly, 6, 6, palette.pixelBright)
            drawLogicalPixel(offsetX, offsetY, scale, lx + 1, ly + 6, palette.pixelBright)
            drawLogicalPixel(offsetX, offsetY, scale, lx + 4, ly + 6, palette.pixelBright)
        }
    }
}

private fun DrawScope.drawPixelBall(
    offsetX: Float,
    offsetY: Float,
    scale: Float,
    centerX: Int,
    centerY: Int,
    squash: Boolean,
    stretch: Boolean,
    palette: ActiveMonoColors
) {
    if (squash) {
        // Squashed impact frame (8px wide x 5px tall)
        drawLogicalRect(offsetX, offsetY, scale, centerX - 2, centerY - 1, 5, 1, palette.pixelBright)
        drawLogicalRect(offsetX, offsetY, scale, centerX - 3, centerY, 7, 2, palette.pixelBright)
        drawLogicalRect(offsetX, offsetY, scale, centerX - 2, centerY + 2, 5, 1, palette.pixelMid)
        drawLogicalPixel(offsetX, offsetY, scale, centerX - 1, centerY, palette.bgDeep)
    } else if (stretch) {
        // Stretched upward boost frame (5px wide x 8px tall)
        drawLogicalRect(offsetX, offsetY, scale, centerX - 1, centerY - 3, 3, 1, palette.pixelBright)
        drawLogicalRect(offsetX, offsetY, scale, centerX - 2, centerY - 2, 5, 5, palette.pixelBright)
        drawLogicalRect(offsetX, offsetY, scale, centerX - 1, centerY + 3, 3, 1, palette.pixelMid)
        drawLogicalPixel(offsetX, offsetY, scale, centerX - 1, centerY - 1, palette.bgDeep)
    } else {
        // Standard crisp 7x7 pixel rounded sphere
        //   XXX
        //  XXXXX
        // XXXXXXX
        // XXXXXXX
        // XXXXXXX
        //  XXXXX
        //   XXX
        drawLogicalRect(offsetX, offsetY, scale, centerX - 1, centerY - 3, 3, 1, palette.pixelBright)
        drawLogicalRect(offsetX, offsetY, scale, centerX - 2, centerY - 2, 5, 1, palette.pixelBright)
        drawLogicalRect(offsetX, offsetY, scale, centerX - 3, centerY - 1, 7, 3, palette.pixelBright)
        drawLogicalRect(offsetX, offsetY, scale, centerX - 2, centerY + 2, 5, 1, palette.pixelMid)
        drawLogicalRect(offsetX, offsetY, scale, centerX - 1, centerY + 3, 3, 1, palette.pixelMid)

        // Retro specular pixel glint on upper-left of ball
        drawLogicalPixel(offsetX, offsetY, scale, centerX - 1, centerY - 1, palette.bgDeep)
    }
}

private fun DrawScope.drawPixelMatrixGrid(
    offsetX: Float,
    offsetY: Float,
    scale: Float,
    logicalW: Int,
    logicalH: Int,
    gridColor: Color
) {
    val lineAlphaColor = gridColor.copy(alpha = 0.22f)
    val totalW = logicalW * scale
    val totalH = logicalH * scale

    for (x in 0..logicalW) {
        val px = offsetX + x * scale
        drawLine(
            color = lineAlphaColor,
            start = Offset(px, offsetY),
            end = Offset(px, offsetY + totalH),
            strokeWidth = 1f
        )
    }
    for (y in 0..logicalH) {
        val py = offsetY + y * scale
        drawLine(
            color = lineAlphaColor,
            start = Offset(offsetX, py),
            end = Offset(offsetX + totalW, py),
            strokeWidth = 1f
        )
    }
}

private fun DrawScope.drawScanlinesOverlay(
    offsetX: Float,
    offsetY: Float,
    scale: Float,
    logicalW: Int,
    logicalH: Int,
    scanlineColor: Color
) {
    val overlayColor = scanlineColor.copy(alpha = 0.16f)
    val totalW = logicalW * scale
    for (y in 0 until logicalH step 2) {
        val py = offsetY + y * scale
        drawRect(
            color = overlayColor,
            topLeft = Offset(offsetX, py),
            size = Size(totalW, maxOf(1f, scale * 0.35f))
        )
    }
}
