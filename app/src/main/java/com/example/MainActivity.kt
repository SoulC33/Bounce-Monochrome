package com.example

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.engine.Game
import com.example.model.GameState
import com.example.model.toColors
import com.example.ui.AboutScreen
import com.example.ui.GameplayScreen
import com.example.ui.LevelSelectScreen
import com.example.ui.MainMenuScreen
import com.example.ui.SettingsScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private val gameViewModel: Game by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val gameState by gameViewModel.gameState.collectAsStateWithLifecycle()
            val settings by gameViewModel.settings.collectAsStateWithLifecycle()

            LaunchedEffect(gameState) {
                val controller = WindowCompat.getInsetsController(window, window.decorView)
                controller.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                if (gameState == GameState.GAMEPLAY) {
                    controller.hide(WindowInsetsCompat.Type.systemBars())
                } else {
                    controller.show(WindowInsetsCompat.Type.systemBars())
                }
            }

            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = settings.paletteMode.toColors().bgDeep
                ) {
                    MonoBounceApp(
                        game = gameViewModel,
                        onExitApp = {
                            gameViewModel.audioManager.stopGameplayMusic()
                            finishAffinity()
                        }
                    )
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        if (gameViewModel.gameState.value == GameState.GAMEPLAY) {
            gameViewModel.pauseGame()
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val state = gameViewModel.gameState.value
        if (state == GameState.GAMEPLAY || state == GameState.PAUSED) {
            val isDown = event.action == KeyEvent.ACTION_DOWN
            val isUp = event.action == KeyEvent.ACTION_UP
            if (isDown || isUp) {
                when (event.keyCode) {
                    KeyEvent.KEYCODE_A, KeyEvent.KEYCODE_DPAD_LEFT -> {
                        gameViewModel.inputManager.setKeyLeftState(isDown)
                        return true
                    }
                    KeyEvent.KEYCODE_D, KeyEvent.KEYCODE_DPAD_RIGHT -> {
                        gameViewModel.inputManager.setKeyRightState(isDown)
                        return true
                    }
                    KeyEvent.KEYCODE_SPACE, KeyEvent.KEYCODE_W, KeyEvent.KEYCODE_DPAD_UP -> {
                        gameViewModel.inputManager.setKeyBoostState(isDown)
                        return true
                    }
                    KeyEvent.KEYCODE_ESCAPE, KeyEvent.KEYCODE_P -> {
                        if (isDown && event.repeatCount == 0) {
                            gameViewModel.togglePauseFromKeyboard()
                        }
                        return true
                    }
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }
}

@Composable
fun MonoBounceApp(
    game: Game,
    onExitApp: () -> Unit = {}
) {
    val gameState by game.gameState.collectAsStateWithLifecycle()
    val settings by game.settings.collectAsStateWithLifecycle()
    val highestUnlocked by game.highestUnlockedLevel.collectAsStateWithLifecycle()
    val lastPlayed by game.lastPlayedLevel.collectAsStateWithLifecycle()
    val highScore by game.highScore.collectAsStateWithLifecycle()
    val progressList by game.levelProgressList.collectAsStateWithLifecycle()
    val hudState by game.hudState.collectAsStateWithLifecycle()
    val levelClearResult by game.levelClearResult.collectAsStateWithLifecycle()
    val frameTick by game.frameTick.collectAsStateWithLifecycle()

    when (gameState) {
        GameState.MAIN_MENU -> {
            MainMenuScreen(
                settings = settings,
                highestUnlockedLevel = highestUnlocked,
                lastPlayedLevel = lastPlayed,
                highScore = highScore,
                onContinueGame = { game.continueSavedGame() },
                onNewGame = { game.startNewGame() },
                onLevelSelect = { game.openLevelSelect() },
                onOpenSettings = { game.openSettings(fromPause = false) },
                onOpenAbout = { game.openAbout() },
                onExitApp = onExitApp
            )
        }

        GameState.LEVEL_SELECT -> {
            LevelSelectScreen(
                settings = settings,
                blueprints = game.levelManager.blueprints,
                highestUnlockedLevel = highestUnlocked,
                progressList = progressList,
                onSelectLevel = { lvl -> game.launchLevel(lvl) },
                onUnlockAll = { game.unlockAllLevels() },
                onBack = { game.returnToMainMenu() }
            )
        }

        GameState.SETTINGS -> {
            SettingsScreen(
                settings = settings,
                onUpdateSettings = { transform -> game.updateSettings(transform) },
                onUnlockAllLevels = { game.unlockAllLevels() },
                onResetProgress = { game.resetAllProgress() },
                onBack = { game.closeSettings() }
            )
        }

        GameState.ABOUT -> {
            AboutScreen(
                settings = settings,
                onBack = { game.returnToMainMenu() }
            )
        }

        GameState.GAMEPLAY,
        GameState.PAUSED,
        GameState.LEVEL_COMPLETE,
        GameState.GAME_OVER -> {
            GameplayScreen(
                gameState = gameState,
                hudState = hudState,
                level = game.currentLevel,
                player = game.player,
                camera = game.camera,
                particles = game.particles,
                settings = settings,
                frameTick = frameTick,
                inputManager = game.inputManager,
                levelClearResult = levelClearResult,
                onPause = { game.pauseGame() },
                onResume = { game.resumeGame() },
                onRestartLevel = { game.retryCurrentLevel() },
                onNextLevel = { game.proceedToNextLevel() },
                onOpenSettingsFromPause = { game.openSettings(fromPause = true) },
                onOpenLevelSelect = { game.openLevelSelect() },
                onReturnToMainMenu = { game.returnToMainMenu() }
            )
        }
    }
}
