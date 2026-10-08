package com.example

import com.example.engine.Camera
import com.example.engine.CollisionSystem
import com.example.engine.InputManager
import com.example.engine.LevelManager
import com.example.engine.Physics
import com.example.model.PixelParticle
import com.example.model.Player
import com.example.model.SurfaceType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun allTenLevels_parseWithValidSpawnExitAndRings() {
        val levelManager = LevelManager()
        assertEquals(10, levelManager.blueprints.size)

        for (lvlNum in 1..10) {
            val level = levelManager.loadLevel(lvlNum)
            assertEquals(lvlNum, level.levelNumber)
            assertTrue("Level $lvlNum should have at least 4 rings", level.totalRings >= 4)
            assertTrue("Spawn X within bounds for level $lvlNum", level.spawnX > 0f && level.spawnX < level.pixelWidth)
            assertTrue("Spawn Y within bounds for level $lvlNum", level.spawnY > 0f && level.spawnY < level.pixelHeight)
            assertTrue("Level $lvlNum should have checkpoints", level.checkpoints.isNotEmpty())
        }
    }

    @Test
    fun playerPhysics_normalAutoBounceClearsThreeTilePlatforms() {
        val levelManager = LevelManager()
        val level = levelManager.loadLevel(1)
        val player = Player(level.spawnX, level.spawnY)
        val input = InputManager()
        val physics = Physics()
        val collision = CollisionSystem(physics)
        val particles = mutableListOf<PixelParticle>()

        var bouncedSurface: SurfaceType? = null
        var floorContactBottomY = 0f
        var minApexBottomY = Float.MAX_VALUE

        for (tick in 0 until 90) {
            physics.updatePlayerPhysics(player, input) {}
            val events = collision.stepAndResolve(player, level, particles)
            if (events.bouncedSurface != null && bouncedSurface == null) {
                bouncedSurface = events.bouncedSurface
                floorContactBottomY = player.y + player.radius
            }
            if (bouncedSurface != null) {
                minApexBottomY = minOf(minApexBottomY, player.y + player.radius)
            }
        }

        assertNotNull("Player should automatically bounce on the floor", bouncedSurface)
        val risePixels = floorContactBottomY - minApexBottomY
        // 3 tiles = 24px; normal auto-bounce must clear at least 26px to land on 3-row platforms effortlessly
        assertTrue("Normal auto-bounce should rise > 26px (actual: $risePixels)", risePixels > 26f)
    }

    @Test
    fun movingPlatform_doesNotSnapPlayerFromHighMidAir() {
        val level = LevelManager().loadLevel(3)
        val plat = level.movingPlatforms.first()
        // Place player 35px high above the moving platform, starting to fall slowly
        val highStartY = plat.y - 35f
        val player = Player(plat.x + plat.width * 0.5f, highStartY)
        player.vy = 0.2f

        val physics = Physics()
        val collision = CollisionSystem(physics)
        val events = collision.stepAndResolve(player, level, mutableListOf())

        assertEquals("Player high above platform should not bounce yet", null, events.bouncedSurface)
        assertTrue("Player should still be high in the air", player.y < plat.y - 25f)
    }

    @Test
    fun camera_clampsWithinLevelBoundaries() {
        val level = LevelManager().loadLevel(1)
        val player = Player(0f, 0f)
        val camera = Camera()
        camera.snapToPlayer(player, level)
        assertTrue(camera.x >= 0f)
        assertTrue(camera.y >= 0f)
    }
}
