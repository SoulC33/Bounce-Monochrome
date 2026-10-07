package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.engine.Camera
import com.example.engine.HudState
import com.example.engine.InputManager
import com.example.model.ActiveMonoColors
import com.example.model.ControlLayoutPreset
import com.example.model.GameSettings
import com.example.model.GameState
import com.example.model.Level
import com.example.model.LevelClearResult
import com.example.model.PixelParticle
import com.example.model.Player
import com.example.model.toColors

@Composable
fun GameplayScreen(
    gameState: GameState,
    hudState: HudState,
    level: Level,
    player: Player,
    camera: Camera,
    particles: List<PixelParticle>,
    settings: GameSettings,
    frameTick: Long,
    inputManager: InputManager,
    levelClearResult: LevelClearResult?,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onRestartLevel: () -> Unit,
    onNextLevel: () -> Unit,
    onOpenSettingsFromPause: () -> Unit,
    onOpenLevelSelect: () -> Unit,
    onReturnToMainMenu: () -> Unit
) {
    BackHandler {
        when (gameState) {
            GameState.GAMEPLAY -> onPause()
            GameState.PAUSED -> onResume()
            else -> onReturnToMainMenu()
        }
    }

    val palette = settings.paletteMode.toColors()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(gameState) {
        if (gameState == GameState.GAMEPLAY) {
            runCatching { focusRequester.requestFocus() }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            inputManager.clearAll()
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.bgDeep)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { event ->
                val isDown = event.type == KeyEventType.KeyDown
                when (event.key) {
                    Key.A, Key.DirectionLeft -> {
                        inputManager.setKeyLeftState(isDown)
                        true
                    }
                    Key.D, Key.DirectionRight -> {
                        inputManager.setKeyRightState(isDown)
                        true
                    }
                    Key.Spacebar, Key.W, Key.DirectionUp -> {
                        inputManager.setKeyBoostState(isDown)
                        true
                    }
                    Key.Escape, Key.P -> {
                        if (isDown) {
                            if (gameState == GameState.GAMEPLAY) onPause() else if (gameState == GameState.PAUSED) onResume()
                        }
                        true
                    }
                    else -> false
                }
            }
    ) {
        val isPortrait = maxHeight >= maxWidth

        if (isPortrait) {
            PortraitHandheldLayout(
                hudState = hudState,
                level = level,
                player = player,
                camera = camera,
                particles = particles,
                settings = settings,
                palette = palette,
                frameTick = frameTick,
                inputManager = inputManager,
                onPause = onPause
            )
        } else {
            LandscapeArcadeLayout(
                hudState = hudState,
                level = level,
                player = player,
                camera = camera,
                particles = particles,
                settings = settings,
                palette = palette,
                frameTick = frameTick,
                inputManager = inputManager,
                onPause = onPause
            )
        }

        // Modal overlays for PAUSED, LEVEL_COMPLETE, and GAME_OVER
        when (gameState) {
            GameState.PAUSED -> {
                PauseModalOverlay(
                    hudState = hudState,
                    palette = palette,
                    onResume = onResume,
                    onRestart = onRestartLevel,
                    onSettings = onOpenSettingsFromPause,
                    onMainMenu = onReturnToMainMenu
                )
            }
            GameState.LEVEL_COMPLETE -> {
                if (levelClearResult != null) {
                    LevelCompleteModalOverlay(
                        result = levelClearResult,
                        palette = palette,
                        onNextLevel = onNextLevel,
                        onReplay = onRestartLevel,
                        onLevelSelect = onOpenLevelSelect,
                        onMainMenu = onReturnToMainMenu
                    )
                }
            }
            GameState.GAME_OVER -> {
                GameOverModalOverlay(
                    hudState = hudState,
                    palette = palette,
                    onRetry = onRestartLevel,
                    onLevelSelect = onOpenLevelSelect,
                    onMainMenu = onReturnToMainMenu
                )
            }
            else -> {}
        }
    }
}

@Composable
private fun PortraitHandheldLayout(
    hudState: HudState,
    level: Level,
    player: Player,
    camera: Camera,
    particles: List<PixelParticle>,
    settings: GameSettings,
    palette: ActiveMonoColors,
    frameTick: Long,
    inputManager: InputManager,
    onPause: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Top Nokia LCD Status HUD
        RetroHudTopBar(
            hudState = hudState,
            palette = palette,
            onPause = onPause
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 2. Retro Handheld Screen Housing with 160x96 Aspect-Preserved Viewport
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = true)
                .border(3.dp, palette.pixelMid)
                .background(palette.bgSurface)
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Stage title strip above LCD glass
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "STAGE %02d: %s".format(hudState.levelNumber, hudState.levelTitle),
                    style = MaterialTheme.typography.labelLarge,
                    color = palette.pixelBright
                )
                Text(
                    text = if (hudState.exitUnlocked) "GATE: [OPEN]" else "GATE: [LOCKED]",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (hudState.exitUnlocked) palette.pixelBright else palette.pixelMid
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                GameRenderer(
                    level = level,
                    player = player,
                    camera = camera,
                    particles = particles,
                    settings = settings,
                    frameTick = frameTick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(Camera.LOGICAL_WIDTH / Camera.LOGICAL_HEIGHT)
                )

                // Status Banner Toast inside LCD
                hudState.statusBannerText?.let { banner ->
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 10.dp)
                            .border(2.dp, palette.pixelBright)
                            .background(palette.bgDeep.copy(alpha = 0.92f))
                            .padding(horizontal = 12.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = banner,
                            style = MaterialTheme.typography.labelLarge,
                            color = palette.pixelBright
                        )
                    }
                }
            }

            // Bottom bezel branding strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "MONO-MATRIX 160x96",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.pixelDim
                )
                Text(
                    text = "TIME: %02d:%02d".format(hudState.elapsedSeconds / 60, hudState.elapsedSeconds % 60),
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.pixelMid
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 3. Dedicated Tactile Control Deck below the screen
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, palette.pixelDim)
                .background(palette.bgSurface)
                .padding(
                    start = settings.buttonHorizontalPaddingDp.dp,
                    end = settings.buttonHorizontalPaddingDp.dp,
                    top = 14.dp,
                    bottom = (14 + settings.buttonVerticalOffsetDp).dp
                )
        ) {
            TouchControlRow(
                settings = settings,
                palette = palette,
                inputManager = inputManager
            )
        }
    }
}

@Composable
private fun LandscapeArcadeLayout(
    hudState: HudState,
    level: Level,
    player: Player,
    camera: Camera,
    particles: List<PixelParticle>,
    settings: GameSettings,
    palette: ActiveMonoColors,
    frameTick: Long,
    inputManager: InputManager,
    onPause: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            RetroHudTopBar(
                hudState = hudState,
                palette = palette,
                onPause = onPause
            )
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                GameRenderer(
                    level = level,
                    player = player,
                    camera = camera,
                    particles = particles,
                    settings = settings,
                    frameTick = frameTick,
                    modifier = Modifier
                        .fillMaxHeight()
                        .aspectRatio(Camera.LOGICAL_WIDTH / Camera.LOGICAL_HEIGHT)
                )

                hudState.statusBannerText?.let { banner ->
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 8.dp)
                            .border(2.dp, palette.pixelBright)
                            .background(palette.bgDeep.copy(alpha = 0.92f))
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = banner,
                            style = MaterialTheme.typography.labelLarge,
                            color = palette.pixelBright
                        )
                    }
                }
            }
        }

        // Overlay Touch Controls at bottom edges in Landscape
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(
                    horizontal = settings.buttonHorizontalPaddingDp.dp,
                    vertical = settings.buttonVerticalOffsetDp.dp
                )
        ) {
            TouchControlRow(
                settings = settings,
                palette = palette,
                inputManager = inputManager
            )
        }
    }
}

@Composable
private fun RetroHudTopBar(
    hudState: HudState,
    palette: ActiveMonoColors,
    onPause: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, palette.pixelBright)
            .background(palette.bgSurface)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Rings counter (e.g. O: 3/5)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "RINGS:%d/%d".format(hudState.ringsCollected, hudState.totalRings),
                style = MaterialTheme.typography.titleMedium,
                color = palette.pixelBright,
                modifier = Modifier.testTag("hud_rings_text")
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "BALLS:%d".format(hudState.lives),
                style = MaterialTheme.typography.titleMedium,
                color = palette.pixelBright,
                modifier = Modifier.testTag("hud_lives_text")
            )
        }

        Text(
            text = "%05d".format(hudState.score),
            style = MaterialTheme.typography.titleMedium,
            color = palette.pixelBright,
            modifier = Modifier.testTag("hud_score_text")
        )

        // Pause Button (minimum 48dp touch target)
        Surface(
            modifier = Modifier
                .size(width = 54.dp, height = 48.dp)
                .clickable(onClick = onPause)
                .testTag("hud_pause_button"),
            color = palette.bgDeep,
            border = BorderStroke(2.dp, palette.pixelBright),
            shape = RoundedCornerShape(0.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "||",
                    style = MaterialTheme.typography.titleLarge,
                    color = palette.pixelBright
                )
            }
        }
    }
}

@Composable
private fun TouchControlRow(
    settings: GameSettings,
    palette: ActiveMonoColors,
    inputManager: InputManager
) {
    val btnSize = settings.buttonSizeDp.dp

    when (settings.controlLayout) {
        ControlLayoutPreset.DPAD_LEFT_BOOST_RIGHT -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ArcadeHoldButton(
                        label = "<",
                        subLabel = "LEFT",
                        width = btnSize,
                        height = btnSize,
                        palette = palette,
                        testTag = "control_left_button",
                        onPressedChanged = { inputManager.setTouchLeftState(it) }
                    )
                    ArcadeHoldButton(
                        label = ">",
                        subLabel = "RIGHT",
                        width = btnSize,
                        height = btnSize,
                        palette = palette,
                        testTag = "control_right_button",
                        onPressedChanged = { inputManager.setTouchRightState(it) }
                    )
                }

                ArcadeHoldButton(
                    label = "▲",
                    subLabel = "BOOST",
                    width = (settings.buttonSizeDp + 18).dp,
                    height = btnSize,
                    palette = palette,
                    testTag = "control_boost_button",
                    onPressedChanged = { inputManager.setTouchBoostState(it) }
                )
            }
        }

        ControlLayoutPreset.SPLIT_EDGES -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ArcadeHoldButton(
                    label = "<",
                    subLabel = "LEFT",
                    width = (settings.buttonSizeDp + 8).dp,
                    height = btnSize,
                    palette = palette,
                    testTag = "control_left_button",
                    onPressedChanged = { inputManager.setTouchLeftState(it) }
                )

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ArcadeHoldButton(
                        label = "▲",
                        subLabel = "BOOST",
                        width = btnSize,
                        height = btnSize,
                        palette = palette,
                        testTag = "control_boost_button",
                        onPressedChanged = { inputManager.setTouchBoostState(it) }
                    )
                    ArcadeHoldButton(
                        label = ">",
                        subLabel = "RIGHT",
                        width = (settings.buttonSizeDp + 8).dp,
                        height = btnSize,
                        palette = palette,
                        testTag = "control_right_button",
                        onPressedChanged = { inputManager.setTouchRightState(it) }
                    )
                }
            }
        }

        ControlLayoutPreset.DPAD_RIGHT_BOOST_LEFT -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ArcadeHoldButton(
                    label = "▲",
                    subLabel = "BOOST",
                    width = (settings.buttonSizeDp + 18).dp,
                    height = btnSize,
                    palette = palette,
                    testTag = "control_boost_button",
                    onPressedChanged = { inputManager.setTouchBoostState(it) }
                )

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ArcadeHoldButton(
                        label = "<",
                        subLabel = "LEFT",
                        width = btnSize,
                        height = btnSize,
                        palette = palette,
                        testTag = "control_left_button",
                        onPressedChanged = { inputManager.setTouchLeftState(it) }
                    )
                    ArcadeHoldButton(
                        label = ">",
                        subLabel = "RIGHT",
                        width = btnSize,
                        height = btnSize,
                        palette = palette,
                        testTag = "control_right_button",
                        onPressedChanged = { inputManager.setTouchRightState(it) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ArcadeHoldButton(
    label: String,
    subLabel: String,
    width: Dp,
    height: Dp,
    palette: ActiveMonoColors,
    testTag: String,
    onPressedChanged: (Boolean) -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .size(width = width.coerceAtLeast(48.dp), height = height.coerceAtLeast(48.dp))
            .border(
                width = 3.dp,
                color = if (isPressed) palette.pixelBright else palette.pixelMid
            )
            .background(if (isPressed) palette.pixelBright else palette.bgDeep)
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    isPressed = true
                    onPressedChanged(true)
                    try {
                        waitForUpOrCancellation()
                    } finally {
                        isPressed = false
                        onPressedChanged(false)
                    }
                }
            }
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                style = MaterialTheme.typography.headlineLarge,
                color = if (isPressed) palette.bgDeep else palette.pixelBright
            )
            Text(
                text = subLabel,
                style = MaterialTheme.typography.labelSmall,
                color = if (isPressed) palette.bgSurface else palette.pixelMid
            )
        }
    }
}

@Composable
private fun PauseModalOverlay(
    hudState: HudState,
    palette: ActiveMonoColors,
    onResume: () -> Unit,
    onRestart: () -> Unit,
    onSettings: () -> Unit,
    onMainMenu: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.bgDeep.copy(alpha = 0.86f))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 420.dp)
                .border(3.dp, palette.pixelBright)
                .background(palette.bgSurface)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "// PAUSED //",
                style = MaterialTheme.typography.displayMedium,
                color = palette.pixelBright,
                modifier = Modifier.testTag("pause_modal_title")
            )
            Text(
                text = "STAGE %02d: %s".format(hudState.levelNumber, hudState.levelTitle),
                style = MaterialTheme.typography.titleMedium,
                color = palette.pixelMid
            )
            Spacer(modifier = Modifier.height(6.dp))

            RetroMenuButton(
                text = "> RESUME GAME",
                palette = palette,
                primary = true,
                testTag = "pause_resume_button",
                onClick = onResume
            )
            RetroMenuButton(
                text = "> RESTART STAGE",
                palette = palette,
                testTag = "pause_restart_button",
                onClick = onRestart
            )
            RetroMenuButton(
                text = "> SETTINGS & CONTROLS",
                palette = palette,
                testTag = "pause_settings_button",
                onClick = onSettings
            )
            RetroMenuButton(
                text = "> MAIN MENU",
                palette = palette,
                testTag = "pause_main_menu_button",
                onClick = onMainMenu
            )
        }
    }
}

@Composable
private fun LevelCompleteModalOverlay(
    result: LevelClearResult,
    palette: ActiveMonoColors,
    onNextLevel: () -> Unit,
    onReplay: () -> Unit,
    onLevelSelect: () -> Unit,
    onMainMenu: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.bgDeep.copy(alpha = 0.90f))
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 440.dp)
                .border(3.dp, palette.pixelBright)
                .background(palette.bgSurface)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "STAGE %02d CLEAR!".format(result.levelNumber),
                style = MaterialTheme.typography.displayMedium,
                color = palette.pixelBright,
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag("level_complete_title")
            )
            Text(
                text = result.levelTitle,
                style = MaterialTheme.typography.titleMedium,
                color = palette.pixelMid
            )

            if (result.isNewHighScore) {
                Box(
                    modifier = Modifier
                        .border(1.dp, palette.pixelBright)
                        .background(palette.pixelBright)
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "★ NEW HIGH SCORE! ★",
                        style = MaterialTheme.typography.labelLarge,
                        color = palette.bgDeep
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, palette.pixelMid)
                    .background(palette.bgDeep)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                StatLine("RINGS COLLECTED", "${result.ringsCollected} / ${result.totalRings}", palette)
                StatLine("BONUS DATA GEMS", "${result.gemsCollected} / ${result.totalGems}", palette)
                StatLine("CLEAR TIME", "${result.elapsedSeconds}s (PAR ${result.parSeconds}s)", palette)
                StatLine("BALLS REMAINING", "${result.livesRemaining}", palette)
                StatLine("STAGE SCORE", "${result.stageScore}", palette)
                StatLine("TOTAL RUN SCORE", "${result.totalRunScore}", palette)
            }

            Spacer(modifier = Modifier.height(4.dp))

            if (result.hasNextLevel) {
                RetroMenuButton(
                    text = "> NEXT STAGE (STAGE %02d)".format(result.levelNumber + 1),
                    palette = palette,
                    primary = true,
                    testTag = "level_complete_next_button",
                    onClick = onNextLevel
                )
            }
            RetroMenuButton(
                text = "> REPLAY STAGE",
                palette = palette,
                primary = !result.hasNextLevel,
                testTag = "level_complete_replay_button",
                onClick = onReplay
            )
            RetroMenuButton(
                text = "> LEVEL SELECT",
                palette = palette,
                testTag = "level_complete_select_button",
                onClick = onLevelSelect
            )
            RetroMenuButton(
                text = "> MAIN MENU",
                palette = palette,
                testTag = "level_complete_menu_button",
                onClick = onMainMenu
            )
        }
    }
}

@Composable
private fun GameOverModalOverlay(
    hudState: HudState,
    palette: ActiveMonoColors,
    onRetry: () -> Unit,
    onLevelSelect: () -> Unit,
    onMainMenu: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.bgDeep.copy(alpha = 0.90f))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 420.dp)
                .border(3.dp, palette.pixelBright)
                .background(palette.bgSurface)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "GAME OVER",
                style = MaterialTheme.typography.displayMedium,
                color = palette.pixelBright,
                modifier = Modifier.testTag("game_over_title")
            )
            Text(
                text = "OUT OF BALLS ON STAGE %02d".format(hudState.levelNumber),
                style = MaterialTheme.typography.titleMedium,
                color = palette.pixelMid
            )
            Text(
                text = "FINAL SCORE: %05d".format(hudState.score),
                style = MaterialTheme.typography.headlineMedium,
                color = palette.pixelBright
            )

            Spacer(modifier = Modifier.height(4.dp))

            RetroMenuButton(
                text = "> RETRY STAGE %02d".format(hudState.levelNumber),
                palette = palette,
                primary = true,
                testTag = "game_over_retry_button",
                onClick = onRetry
            )
            RetroMenuButton(
                text = "> LEVEL SELECT",
                palette = palette,
                testTag = "game_over_select_button",
                onClick = onLevelSelect
            )
            RetroMenuButton(
                text = "> MAIN MENU",
                palette = palette,
                testTag = "game_over_menu_button",
                onClick = onMainMenu
            )
        }
    }
}

@Composable
private fun StatLine(label: String, value: String, palette: ActiveMonoColors) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = palette.pixelMid
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            color = palette.pixelBright
        )
    }
}
