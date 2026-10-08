package com.example.engine

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AudioManager
import com.example.data.LevelProgressEntity
import com.example.data.SaveManager
import com.example.model.CollectibleType
import com.example.model.GameSettings
import com.example.model.GameState
import com.example.model.Level
import com.example.model.LevelClearResult
import com.example.model.PixelParticle
import com.example.model.Player
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class HudState(
    val levelNumber: Int = 1,
    val levelTitle: String = "FIRST SIGNAL",
    val levelSubtitle: String = "",
    val ringsCollected: Int = 0,
    val totalRings: Int = 4,
    val exitUnlocked: Boolean = false,
    val lives: Int = 3,
    val score: Int = 0,
    val elapsedSeconds: Int = 0,
    val statusBannerText: String? = null
)

class Game(application: Application) : AndroidViewModel(application) {
    val saveManager = SaveManager(application)
    val audioManager = AudioManager(application)
    val levelManager = LevelManager()
    val inputManager = InputManager()
    val physics = Physics()
    val collisionSystem = CollisionSystem(physics)
    val camera = Camera()

    val settings: StateFlow<GameSettings> = saveManager.settings
    val highestUnlockedLevel: StateFlow<Int> = saveManager.highestUnlockedLevel
    val lastPlayedLevel: StateFlow<Int> = saveManager.lastPlayedLevel
    val highScore: StateFlow<Int> = saveManager.highScore

    val levelProgressList: StateFlow<List<LevelProgressEntity>> = saveManager.levelProgressFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _gameState = MutableStateFlow(GameState.MAIN_MENU)
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    // Tracks whether Settings was opened from Pause menu vs Main Menu
    private var settingsReturnState: GameState = GameState.MAIN_MENU

    private val _hudState = MutableStateFlow(HudState())
    val hudState: StateFlow<HudState> = _hudState.asStateFlow()

    private val _levelClearResult = MutableStateFlow<LevelClearResult?>(null)
    val levelClearResult: StateFlow<LevelClearResult?> = _levelClearResult.asStateFlow()

    // Frame tick state used by GameRenderer Canvas to trigger 60 FPS redraws
    private val _frameTick = MutableStateFlow(0L)
    val frameTick: StateFlow<Long> = _frameTick.asStateFlow()

    var currentLevel: Level = levelManager.loadLevel(1)
        private set

    val player: Player = Player(currentLevel.spawnX, currentLevel.spawnY)
    val particles: MutableList<PixelParticle> = mutableListOf()

    private var lives: Int = 3
    private var totalRunScore: Int = 0
    private var stageScore: Int = 0
    private var stageTicksElapsed: Int = 0
    private var bannerTicksRemaining: Int = 0
    private var bannerMessage: String? = null

    private var loopJob: Job? = null

    init {
        // Keep AudioManager flags synced with saved settings
        viewModelScope.launch {
            settings.collect { s ->
                audioManager.soundEnabled = s.soundEnabled
                audioManager.musicEnabled = s.musicEnabled
                audioManager.hapticsEnabled = s.hapticsEnabled
                if (_gameState.value == GameState.GAMEPLAY) {
                    if (s.musicEnabled) {
                        audioManager.startGameplayMusic()
                    } else {
                        audioManager.stopGameplayMusic()
                    }
                }
            }
        }
        startGameLoop()
    }

    private fun startGameLoop() {
        if (loopJob?.isActive == true) return
        loopJob = viewModelScope.launch {
            while (isActive) {
                if (_gameState.value == GameState.GAMEPLAY) {
                    stepSingleFrame()
                }
                delay(16L) // ~60 FPS fixed retro tick
            }
        }
    }

    fun stepSingleFrame() {
        val level = currentLevel
        stageTicksElapsed++

        if (bannerTicksRemaining > 0) {
            bannerTicksRemaining--
            if (bannerTicksRemaining == 0) {
                bannerMessage = null
            }
        }

        if (player.hitFreezeFrames > 0) {
            player.hitFreezeFrames--
            _frameTick.value = _frameTick.value + 1L
            return
        }

        // 1. Update player physics & air-boost
        physics.updatePlayerPhysics(
            player = player,
            input = inputManager,
            onAirBoostTriggered = {
                audioManager.playBoost()
            }
        )

        // 2. Update moving platforms, patrolling hazards, crumble blocks, and particles
        physics.updateWorldEntities(level, particles)

        // 3. Resolve collisions
        val events = collisionSystem.stepAndResolve(player, level, particles)

        // 4. Record trail every 3 ticks
        if (stageTicksElapsed % 3 == 0) {
            player.recordTrail()
        }

        // 5. Update 2D side-scrolling camera
        camera.update(player, level)

        // 6. Process collision events & audio
        events.bouncedSurface?.let { surface ->
            audioManager.playBounce(surface)
        }

        for (item in events.collectedItems) {
            stageScore += item.type.points
            totalRunScore += item.type.points
            when (item.type) {
                CollectibleType.RING -> {
                    audioManager.playCollectRing()
                }
                CollectibleType.DATA_GEM -> {
                    audioManager.playCollectGem()
                }
                CollectibleType.EXTRA_LIFE -> {
                    lives = (lives + 1).coerceAtMost(9)
                    audioManager.playExtraLife()
                    showStatusBanner("1-UP // EXTRA BALL!")
                }
            }
        }

        if (events.activatedCheckpoint) {
            stageScore += 150
            totalRunScore += 150
            audioManager.playCheckpoint()
            showStatusBanner("CHECKPOINT SAVED")
        }

        if (events.exitUnlockedNow) {
            audioManager.playExitUnlocked()
            showStatusBanner("EXIT GATE OPEN!")
        }

        if (events.hitHazardOrPit) {
            handlePlayerHitHazard()
        } else if (events.reachedUnlockedExit) {
            handleStageCompleted()
        }

        syncHudState()
        _frameTick.value = _frameTick.value + 1L
    }

    private fun handlePlayerHitHazard() {
        audioManager.playHazardHit()
        lives--
        inputManager.clearAll()

        if (lives <= 0) {
            lives = 0
            syncHudState()
            audioManager.stopGameplayMusic()
            audioManager.playGameOver()
            _gameState.value = GameState.GAME_OVER
        } else {
            // Restore all fragile crumble blocks on respawn so player is never stranded
            for (crumble in currentLevel.crumbleBlocks.values) {
                crumble.triggered = false
                crumble.ticksRemaining = 22
                crumble.respawnTicks = 0
            }
            player.resetTo(player.respawnX, player.respawnY, fullReset = false)
            player.hitFreezeFrames = 14
            camera.snapToPlayer(player, currentLevel)
            showStatusBanner("BALL LOST // $lives LEFT")
        }
    }

    private fun handleStageCompleted() {
        inputManager.clearAll()
        audioManager.stopGameplayMusic()
        audioManager.playLevelComplete()

        val elapsedSec = (stageTicksElapsed / 60).coerceAtLeast(1)
        val timeBonus = maxOf(0, (currentLevel.parSeconds - elapsedSec) * 15)
        val lifeBonus = lives * 200
        val finalStageScore = stageScore + 500 + timeBonus + lifeBonus
        totalRunScore += 500 + timeBonus + lifeBonus

        val isNewHigh = saveManager.recordLevelCompleted(
            levelNumber = currentLevel.levelNumber,
            stageScore = finalStageScore,
            totalRunScore = totalRunScore,
            ringsCollected = currentLevel.ringsCollected,
            totalRings = currentLevel.totalRings,
            elapsedSeconds = elapsedSec
        )

        _levelClearResult.value = LevelClearResult(
            levelNumber = currentLevel.levelNumber,
            levelTitle = currentLevel.title,
            ringsCollected = currentLevel.ringsCollected,
            totalRings = currentLevel.totalRings,
            gemsCollected = currentLevel.gemsCollected,
            totalGems = currentLevel.totalGems,
            elapsedSeconds = elapsedSec,
            parSeconds = currentLevel.parSeconds,
            livesRemaining = lives,
            stageScore = finalStageScore,
            totalRunScore = totalRunScore,
            isNewHighScore = isNewHigh,
            hasNextLevel = currentLevel.levelNumber < SaveManager.TOTAL_LEVELS
        )

        syncHudState()
        _gameState.value = GameState.LEVEL_COMPLETE
    }

    private fun showStatusBanner(message: String, durationTicks: Int = 95) {
        bannerMessage = message
        bannerTicksRemaining = durationTicks
    }

    private fun syncHudState() {
        _hudState.value = HudState(
            levelNumber = currentLevel.levelNumber,
            levelTitle = currentLevel.title,
            levelSubtitle = currentLevel.subtitle,
            ringsCollected = currentLevel.ringsCollected,
            totalRings = currentLevel.totalRings,
            exitUnlocked = currentLevel.isExitUnlocked,
            lives = lives,
            score = totalRunScore,
            elapsedSeconds = stageTicksElapsed / 60,
            statusBannerText = bannerMessage
        )
    }

    // --- Navigation & Menu Actions ---

    fun startNewGame() {
        audioManager.playButtonPress()
        lives = 3
        totalRunScore = 0
        launchLevel(1)
    }

    fun continueSavedGame() {
        audioManager.playButtonPress()
        lives = 3
        totalRunScore = 0
        val targetLevel = highestUnlockedLevel.value.coerceIn(1, SaveManager.TOTAL_LEVELS)
        launchLevel(targetLevel)
    }

    fun launchLevel(levelNumber: Int) {
        val clamped = levelNumber.coerceIn(1, SaveManager.TOTAL_LEVELS)
        saveManager.recordLastPlayedLevel(clamped)
        currentLevel = levelManager.loadLevel(clamped)
        particles.clear()
        stageScore = 0
        stageTicksElapsed = 0
        if (lives <= 0) {
            lives = 3
        }
        inputManager.clearAll()
        player.resetTo(currentLevel.spawnX, currentLevel.spawnY, fullReset = true)
        camera.snapToPlayer(player, currentLevel)
        showStatusBanner("STAGE %02d // %s".format(currentLevel.levelNumber, currentLevel.title), 110)
        syncHudState()
        _frameTick.value = _frameTick.value + 1L
        _gameState.value = GameState.GAMEPLAY
        audioManager.startGameplayMusic()
    }

    fun retryCurrentLevel() {
        audioManager.playButtonPress()
        lives = 3
        launchLevel(currentLevel.levelNumber)
    }

    fun proceedToNextLevel() {
        audioManager.playButtonPress()
        val next = (currentLevel.levelNumber + 1).coerceAtMost(SaveManager.TOTAL_LEVELS)
        launchLevel(next)
    }

    fun pauseGame() {
        if (_gameState.value == GameState.GAMEPLAY) {
            audioManager.playMenuSelect()
            inputManager.clearAll()
            audioManager.stopGameplayMusic()
            _gameState.value = GameState.PAUSED
        }
    }

    fun resumeGame() {
        if (_gameState.value == GameState.PAUSED) {
            audioManager.playButtonPress()
            inputManager.clearAll()
            _gameState.value = GameState.GAMEPLAY
            audioManager.startGameplayMusic()
        }
    }

    fun togglePauseFromKeyboard() {
        when (_gameState.value) {
            GameState.GAMEPLAY -> pauseGame()
            GameState.PAUSED -> resumeGame()
            else -> {}
        }
    }

    fun openLevelSelect() {
        audioManager.playMenuSelect()
        audioManager.stopGameplayMusic()
        _gameState.value = GameState.LEVEL_SELECT
    }

    fun openSettings(fromPause: Boolean = false) {
        audioManager.playMenuSelect()
        settingsReturnState = if (fromPause) GameState.PAUSED else GameState.MAIN_MENU
        _gameState.value = GameState.SETTINGS
    }

    fun closeSettings() {
        audioManager.playMenuSelect()
        _gameState.value = settingsReturnState
    }

    fun openAbout() {
        audioManager.playMenuSelect()
        _gameState.value = GameState.ABOUT
    }

    fun returnToMainMenu() {
        audioManager.playMenuSelect()
        inputManager.clearAll()
        audioManager.stopGameplayMusic()
        _gameState.value = GameState.MAIN_MENU
    }

    fun updateSettings(transform: (GameSettings) -> GameSettings) {
        audioManager.playMenuSelect()
        saveManager.updateSettings(transform)
    }

    fun unlockAllLevels() {
        audioManager.playExtraLife()
        saveManager.unlockAllLevels()
    }

    fun resetAllProgress() {
        audioManager.playHazardHit()
        saveManager.resetProgress()
    }

    override fun onCleared() {
        super.onCleared()
        audioManager.stopGameplayMusic()
    }
}
