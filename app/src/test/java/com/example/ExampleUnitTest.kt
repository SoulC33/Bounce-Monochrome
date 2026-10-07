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
    fun playerPhysicsAndCollision_autoBouncesOnSolidFloor() {
        val levelManager = LevelManager()
        val level = levelManager.loadLevel(1)
        val player = Player(level.spawnX, level.spawnY)
        val input = InputManager()
        val physics = Physics()
        val collision = CollisionSystem(physics)
        val particles = mutableListOf<PixelParticle>()

        var bouncedSurface: SurfaceType? = null
        for (tick in 0 until 60) {
            physics.updatePlayerPhysics(player, input) {}
            val events = collision.stepAndResolve(player, level, particles)
            if (events.bouncedSurface != null) {
                bouncedSurface = events.bouncedSurface
                break
            }
        }

        assertNotNull("Player should automatically bounce on the floor within 60 ticks", bouncedSurface)
        assertTrue("Velocity Y after bounce should be upward (negative)", player.vy < 0f)
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
