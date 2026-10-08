package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.LevelProgressEntity
import com.example.engine.LevelBlueprint
import com.example.model.ActiveMonoColors
import com.example.model.ControlLayoutPreset
import com.example.model.GameSettings
import com.example.model.MonoPaletteMode
import com.example.model.toColors

@Composable
fun MainMenuScreen(
    settings: GameSettings,
    highestUnlockedLevel: Int,
    lastPlayedLevel: Int,
    highScore: Int,
    onContinueGame: () -> Unit,
    onNewGame: () -> Unit,
    onLevelSelect: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAbout: () -> Unit,
    onExitApp: () -> Unit
) {
    val palette = settings.paletteMode.toColors()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.bgDeep)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 520.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Retro LCD top status bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, palette.pixelMid)
                    .background(palette.bgSurface)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SIGNAL: [||||] MONO-LCD",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.pixelMid
                )
                Text(
                    text = "HI-SCORE: %05d".format(highScore),
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.pixelBright
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Main Retro Title Card with Monochrome Pixel Art Banner
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(3.dp, palette.pixelBright)
                    .background(palette.bgSurface)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "BOUNCE",
                    style = MaterialTheme.typography.displayLarge,
                    color = palette.pixelBright,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag("main_menu_title")
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "PRE-COLOR MONO EDITION // 10 STAGES",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.pixelMid,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Monochrome greenish LCD pixel banner image framed inside LCD bezel
                val lcdGreenMatrix = ColorMatrix(
                    floatArrayOf(
                        0.24f, 0.48f, 0.10f, 0f, 8f,
                        0.30f, 0.62f, 0.14f, 0f, 18f,
                        0.22f, 0.44f, 0.10f, 0f, 6f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(116.dp)
                        .border(2.dp, palette.pixelMid)
                        .background(palette.bgDeep)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_retro_banner_1791341478395),
                        contentDescription = "Monochrome pixel art bouncing ball illustration",
                        contentScale = ContentScale.Crop,
                        colorFilter = ColorFilter.colorMatrix(lcdGreenMatrix),
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .background(palette.bgDeep.copy(alpha = 0.85f))
                            .border(1.dp, palette.pixelMid)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "STAGE %02d/10 READY".format(highestUnlockedLevel),
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.pixelBright
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Classic Nokia-style Menu Options
            if (highestUnlockedLevel > 1 || lastPlayedLevel > 1) {
                RetroMenuButton(
                    text = "> CONTINUE (STAGE %02d)".format(highestUnlockedLevel),
                    subtitle = "RESUME FROM HIGHEST UNLOCKED STAGE",
                    palette = palette,
                    primary = true,
                    testTag = "menu_continue_button",
                    onClick = onContinueGame
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            RetroMenuButton(
                text = "> NEW GAME",
                subtitle = "START FRESH RUN FROM STAGE 01",
                palette = palette,
                primary = highestUnlockedLevel <= 1,
                testTag = "menu_new_game_button",
                onClick = onNewGame
            )
            Spacer(modifier = Modifier.height(10.dp))

            RetroMenuButton(
                text = "> LEVEL SELECT",
                subtitle = "CHOOSE STAGE (01 - 10) & VIEW RECORDS",
                palette = palette,
                primary = false,
                testTag = "menu_level_select_button",
                onClick = onLevelSelect
            )
            Spacer(modifier = Modifier.height(10.dp))

            RetroMenuButton(
                text = "> SETTINGS",
                subtitle = "SOUND, PALETTE, CRT & TOUCH CONTROLS",
                palette = palette,
                primary = false,
                testTag = "menu_settings_button",
                onClick = onOpenSettings
            )
            Spacer(modifier = Modifier.height(10.dp))

            RetroMenuButton(
                text = "> ABOUT",
                subtitle = "MANUAL, TILE LEGEND & CONTROLS GUIDE",
                palette = palette,
                primary = false,
                testTag = "menu_about_button",
                onClick = onOpenAbout
            )
            Spacer(modifier = Modifier.height(10.dp))

            RetroMenuButton(
                text = "> EXIT",
                subtitle = "QUIT MONOBOUNCE TO HOME SCREEN",
                palette = palette,
                primary = false,
                testTag = "menu_exit_button",
                onClick = onExitApp
            )

            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "KEYS: [A/D] OR [LEFT/RIGHT] MOVE  •  [SPACE] BOOST  •  [ESC] PAUSE",
                style = MaterialTheme.typography.labelSmall,
                color = palette.pixelDim,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun RetroMenuButton(
    text: String,
    subtitle: String? = null,
    palette: ActiveMonoColors,
    primary: Boolean = false,
    testTag: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (subtitle != null) 60.dp else 50.dp)
            .clip(RoundedCornerShape(0.dp))
            .clickable(onClick = onClick)
            .testTag(testTag),
        color = if (primary) palette.pixelBright else palette.bgSurface,
        border = BorderStroke(2.dp, if (primary) palette.pixelBright else palette.pixelMid),
        shape = RoundedCornerShape(0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleLarge,
                color = if (primary) palette.bgDeep else palette.pixelBright
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (primary) palette.bgSurface else palette.pixelMid
                )
            }
        }
    }
}

@Composable
fun LevelSelectScreen(
    settings: GameSettings,
    blueprints: List<LevelBlueprint>,
    highestUnlockedLevel: Int,
    progressList: List<LevelProgressEntity>,
    onSelectLevel: (Int) -> Unit,
    onUnlockAll: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    val palette = settings.paletteMode.toColors()
    val progressMap = progressList.associateBy { it.levelNumber }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.bgDeep)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(16.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 580.dp)
        ) {
            // Header row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, palette.pixelBright)
                    .background(palette.bgSurface)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier
                        .height(48.dp)
                        .clickable(onClick = onBack)
                        .testTag("level_select_back_button"),
                    color = palette.bgDeep,
                    border = BorderStroke(2.dp, palette.pixelMid),
                    shape = RoundedCornerShape(0.dp)
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "< MENU",
                            style = MaterialTheme.typography.titleMedium,
                            color = palette.pixelBright
                        )
                    }
                }

                Text(
                    text = "SELECT STAGE",
                    style = MaterialTheme.typography.headlineMedium,
                    color = palette.pixelBright
                )

                if (highestUnlockedLevel < blueprints.size) {
                    Surface(
                        modifier = Modifier
                            .height(48.dp)
                            .clickable(onClick = onUnlockAll)
                            .testTag("unlock_all_levels_button"),
                        color = palette.bgDeep,
                        border = BorderStroke(2.dp, palette.pixelMid),
                        shape = RoundedCornerShape(0.dp)
                    ) {
                        Box(
                            modifier = Modifier.padding(horizontal = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "UNLOCK ALL",
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.pixelBright
                            )
                        }
                    }
                } else {
                    Text(
                        text = "10/10 OPEN",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.pixelMid
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 20.dp)
            ) {
                items(blueprints, key = { it.levelNumber }) { bp ->
                    val progress = progressMap[bp.levelNumber]
                    val isUnlocked = bp.levelNumber <= highestUnlockedLevel || (progress?.unlocked == true)
                    val isCompleted = progress?.completed == true

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = isUnlocked) { onSelectLevel(bp.levelNumber) }
                            .testTag("level_card_${bp.levelNumber}"),
                        color = if (isUnlocked) palette.bgSurface else palette.bgDeep,
                        border = BorderStroke(
                            width = 2.dp,
                            color = if (isUnlocked) palette.pixelBright else palette.pixelDim
                        ),
                        shape = RoundedCornerShape(0.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "STAGE %02d // %s".format(bp.levelNumber, bp.title),
                                        style = MaterialTheme.typography.titleLarge,
                                        color = if (isUnlocked) palette.pixelBright else palette.pixelDim
                                    )
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = bp.subtitle,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isUnlocked) palette.pixelMid else palette.pixelDim
                                )
                                if (isCompleted && progress != null) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "CLEAR! BEST: %04d PTS  •  RINGS: %d/%d  •  TIME: %ds".format(
                                            progress.bestScore,
                                            progress.bestRings,
                                            progress.totalRings,
                                            progress.bestTimeSeconds
                                        ),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = palette.pixelBright
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Box(
                                modifier = Modifier
                                    .border(
                                        1.dp,
                                        if (isUnlocked) palette.pixelBright else palette.pixelDim
                                    )
                                    .background(if (isUnlocked) palette.pixelBright else palette.bgDeep)
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = when {
                                        !isUnlocked -> "LOCKED"
                                        isCompleted -> "REPLAY >"
                                        else -> "PLAY >"
                                    },
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (isUnlocked) palette.bgDeep else palette.pixelDim
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(
    settings: GameSettings,
    onUpdateSettings: ((GameSettings) -> GameSettings) -> Unit,
    onUnlockAllLevels: () -> Unit,
    onResetProgress: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    val palette = settings.paletteMode.toColors()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.bgDeep)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(16.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 580.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, palette.pixelBright)
                    .background(palette.bgSurface)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier
                        .height(48.dp)
                        .clickable(onClick = onBack)
                        .testTag("settings_back_button"),
                    color = palette.bgDeep,
                    border = BorderStroke(2.dp, palette.pixelMid),
                    shape = RoundedCornerShape(0.dp)
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "< BACK",
                            style = MaterialTheme.typography.titleMedium,
                            color = palette.pixelBright
                        )
                    }
                }
                Text(
                    text = "SYSTEM SETTINGS",
                    style = MaterialTheme.typography.headlineMedium,
                    color = palette.pixelBright
                )
                Spacer(modifier = Modifier.width(48.dp))
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 1. AUDIO & HAPTICS
            SectionHeader("AUDIO & PIEZO SYNTH", palette)
            SettingToggleRow(
                label = "RETRO SOUND FX",
                subtitle = "Synthesized 1-bit bounce, ring & hazard tones",
                checked = settings.soundEnabled,
                palette = palette,
                testTag = "toggle_sound_fx",
                onToggle = { onUpdateSettings { s -> s.copy(soundEnabled = !s.soundEnabled) } }
            )
            Spacer(modifier = Modifier.height(8.dp))
            SettingToggleRow(
                label = "CHIPTUNE PULSE MUSIC",
                subtitle = "Monophonic background melody during stages",
                checked = settings.musicEnabled,
                palette = palette,
                testTag = "toggle_music",
                onToggle = { onUpdateSettings { s -> s.copy(musicEnabled = !s.musicEnabled) } }
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Volume Controller Panel (SFX & Music Volume Bars + Sliders)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, palette.pixelMid)
                    .background(palette.bgSurface)
                    .padding(14.dp)
            ) {
                val sfxBars = (settings.sfxVolumePercent / 10).coerceIn(0, 10)
                val sfxBarText = "|".repeat(sfxBars) + ".".repeat(10 - sfxBars)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SFX VOLUME",
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.pixelBright
                    )
                    Text(
                        text = "[$sfxBarText] ${settings.sfxVolumePercent}%",
                        style = MaterialTheme.typography.labelLarge,
                        color = palette.pixelBright
                    )
                }
                Slider(
                    value = settings.sfxVolumePercent.toFloat(),
                    onValueChange = { v ->
                        val rounded = (v.toInt() / 5) * 5
                        onUpdateSettings { s ->
                            s.copy(
                                sfxVolumePercent = rounded.coerceIn(0, 100),
                                soundEnabled = rounded > 0
                            )
                        }
                    },
                    valueRange = 0f..100f,
                    steps = 19,
                    modifier = Modifier.testTag("slider_sfx_volume"),
                    colors = SliderDefaults.colors(
                        thumbColor = palette.pixelBright,
                        activeTrackColor = palette.pixelBright,
                        inactiveTrackColor = palette.pixelDim
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))

                val musicBars = (settings.musicVolumePercent / 10).coerceIn(0, 10)
                val musicBarText = "|".repeat(musicBars) + ".".repeat(10 - musicBars)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "MUSIC VOLUME",
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.pixelBright
                    )
                    Text(
                        text = "[$musicBarText] ${settings.musicVolumePercent}%",
                        style = MaterialTheme.typography.labelLarge,
                        color = palette.pixelBright
                    )
                }
                Slider(
                    value = settings.musicVolumePercent.toFloat(),
                    onValueChange = { v ->
                        val rounded = (v.toInt() / 5) * 5
                        onUpdateSettings { s ->
                            s.copy(
                                musicVolumePercent = rounded.coerceIn(0, 100),
                                musicEnabled = rounded > 0
                            )
                        }
                    },
                    valueRange = 0f..100f,
                    steps = 19,
                    modifier = Modifier.testTag("slider_music_volume"),
                    colors = SliderDefaults.colors(
                        thumbColor = palette.pixelBright,
                        activeTrackColor = palette.pixelBright,
                        inactiveTrackColor = palette.pixelDim
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            SettingToggleRow(
                label = "TACTILE HAPTICS",
                subtitle = "Subtle vibration pulse on bounce and impact",
                checked = settings.hapticsEnabled,
                palette = palette,
                testTag = "toggle_haptics",
                onToggle = { onUpdateSettings { s -> s.copy(hapticsEnabled = !s.hapticsEnabled) } }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 2. DISPLAY & MONOCHROME PALETTE
            SectionHeader("MONOCHROME LCD DISPLAY", palette)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val nextIdx = (settings.paletteMode.ordinal + 1) % MonoPaletteMode.entries.size
                        onUpdateSettings { s -> s.copy(paletteMode = MonoPaletteMode.entries[nextIdx]) }
                    }
                    .testTag("cycle_palette_button"),
                color = palette.bgSurface,
                border = BorderStroke(2.dp, palette.pixelMid),
                shape = RoundedCornerShape(0.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "PALETTE: ${settings.paletteMode.displayName}",
                            style = MaterialTheme.typography.titleMedium,
                            color = palette.pixelBright
                        )
                        Text(
                            text = settings.paletteMode.description,
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.pixelMid
                        )
                    }
                    Text(
                        text = "[CHANGE]",
                        style = MaterialTheme.typography.labelLarge,
                        color = palette.pixelBright
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            SettingToggleRow(
                label = "LCD PIXEL MATRIX GRID",
                subtitle = "Crisp dot-matrix grid between logical pixels",
                checked = settings.pixelGridEnabled,
                palette = palette,
                testTag = "toggle_pixel_grid",
                onToggle = { onUpdateSettings { s -> s.copy(pixelGridEnabled = !s.pixelGridEnabled) } }
            )
            Spacer(modifier = Modifier.height(8.dp))
            SettingToggleRow(
                label = "SUBTLE SCANLINES",
                subtitle = "Classic handheld horizontal scanline pass",
                checked = settings.scanlinesEnabled,
                palette = palette,
                testTag = "toggle_scanlines",
                onToggle = { onUpdateSettings { s -> s.copy(scanlinesEnabled = !s.scanlinesEnabled) } }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 3. TOUCH CONTROLS CUSTOMIZATION
            SectionHeader("TOUCHSCREEN CONTROLS", palette)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val nextLayout = ControlLayoutPreset.entries[
                            (settings.controlLayout.ordinal + 1) % ControlLayoutPreset.entries.size
                        ]
                        onUpdateSettings { s -> s.copy(controlLayout = nextLayout) }
                    }
                    .testTag("cycle_control_layout_button"),
                color = palette.bgSurface,
                border = BorderStroke(2.dp, palette.pixelMid),
                shape = RoundedCornerShape(0.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "LAYOUT: ${settings.controlLayout.label}",
                            style = MaterialTheme.typography.titleMedium,
                            color = palette.pixelBright
                        )
                        Text(
                            text = "Tap to switch Left/Right & Boost button arrangement",
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.pixelMid
                        )
                    }
                    Text(
                        text = "[SWAP]",
                        style = MaterialTheme.typography.labelLarge,
                        color = palette.pixelBright
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Button Size & Position Sliders
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, palette.pixelMid)
                    .background(palette.bgSurface)
                    .padding(14.dp)
            ) {
                Text(
                    text = "BUTTON SIZE: ${settings.buttonSizeDp} DP",
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.pixelBright
                )
                Slider(
                    value = settings.buttonSizeDp.toFloat(),
                    onValueChange = { v ->
                        onUpdateSettings { s -> s.copy(buttonSizeDp = v.toInt()) }
                    },
                    valueRange = 54f..88f,
                    steps = 5,
                    colors = SliderDefaults.colors(
                        thumbColor = palette.pixelBright,
                        activeTrackColor = palette.pixelBright,
                        inactiveTrackColor = palette.pixelDim
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "EDGE MARGIN: ${settings.buttonHorizontalPaddingDp} DP",
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.pixelBright
                )
                Slider(
                    value = settings.buttonHorizontalPaddingDp.toFloat(),
                    onValueChange = { v ->
                        onUpdateSettings { s -> s.copy(buttonHorizontalPaddingDp = v.toInt()) }
                    },
                    valueRange = 6f..42f,
                    steps = 5,
                    colors = SliderDefaults.colors(
                        thumbColor = palette.pixelBright,
                        activeTrackColor = palette.pixelBright,
                        inactiveTrackColor = palette.pixelDim
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "BOTTOM OFFSET: ${settings.buttonVerticalOffsetDp} DP",
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.pixelBright
                )
                Slider(
                    value = settings.buttonVerticalOffsetDp.toFloat(),
                    onValueChange = { v ->
                        onUpdateSettings { s -> s.copy(buttonVerticalOffsetDp = v.toInt()) }
                    },
                    valueRange = 0f..40f,
                    steps = 4,
                    colors = SliderDefaults.colors(
                        thumbColor = palette.pixelBright,
                        activeTrackColor = palette.pixelBright,
                        inactiveTrackColor = palette.pixelDim
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4. DATA & TESTING OPTIONS
            SectionHeader("STAGE DATA & PROGRESS", palette)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .clickable(onClick = onUnlockAllLevels)
                        .testTag("settings_unlock_all_button"),
                    color = palette.bgSurface,
                    border = BorderStroke(2.dp, palette.pixelMid),
                    shape = RoundedCornerShape(0.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "UNLOCK ALL 10",
                            style = MaterialTheme.typography.labelLarge,
                            color = palette.pixelBright
                        )
                    }
                }
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .clickable(onClick = onResetProgress)
                        .testTag("settings_reset_progress_button"),
                    color = palette.bgSurface,
                    border = BorderStroke(2.dp, palette.pixelDim),
                    shape = RoundedCornerShape(0.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "RESET SAVE",
                            style = MaterialTheme.typography.labelLarge,
                            color = palette.pixelMid
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SectionHeader(title: String, palette: ActiveMonoColors) {
    Text(
        text = "// $title",
        style = MaterialTheme.typography.labelLarge,
        color = palette.pixelMid,
        modifier = Modifier.padding(bottom = 6.dp)
    )
}

@Composable
private fun SettingToggleRow(
    label: String,
    subtitle: String,
    checked: Boolean,
    palette: ActiveMonoColors,
    testTag: String,
    onToggle: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .testTag(testTag),
        color = palette.bgSurface,
        border = BorderStroke(2.dp, if (checked) palette.pixelBright else palette.pixelDim),
        shape = RoundedCornerShape(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.pixelBright
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.pixelMid
                )
            }
            Box(
                modifier = Modifier
                    .border(1.dp, palette.pixelBright)
                    .background(if (checked) palette.pixelBright else palette.bgDeep)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = if (checked) "ON" else "OFF",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (checked) palette.bgDeep else palette.pixelMid
                )
            }
        }
    }
}

@Composable
fun AboutScreen(
    settings: GameSettings,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    val palette = settings.paletteMode.toColors()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.bgDeep)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(16.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 580.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, palette.pixelBright)
                    .background(palette.bgSurface)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier
                        .height(48.dp)
                        .clickable(onClick = onBack)
                        .testTag("about_back_button"),
                    color = palette.bgDeep,
                    border = BorderStroke(2.dp, palette.pixelMid),
                    shape = RoundedCornerShape(0.dp)
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "< MENU",
                            style = MaterialTheme.typography.titleMedium,
                            color = palette.pixelBright
                        )
                    }
                }
                Text(
                    text = "OPERATOR MANUAL",
                    style = MaterialTheme.typography.headlineMedium,
                    color = palette.pixelBright
                )
                Spacer(modifier = Modifier.width(48.dp))
            }

            Spacer(modifier = Modifier.height(14.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, palette.pixelMid)
                    .background(palette.bgSurface)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "MONOBOUNCE // PRE-COLOR HANDHELD EDITION",
                    style = MaterialTheme.typography.titleLarge,
                    color = palette.pixelBright
                )
                Text(
                    text = "Designed in the spirit of early-2000s monochrome mobile games. Uses a fixed 160x96 logical pixel canvas scaled with integer-sharp nearest-neighbor edges and a synthesized 1-bit square-wave audio engine.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = palette.pixelMid
                )
                Text(
                    text = "HOW TO PLAY:",
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.pixelBright
                )
                Text(
                    text = "• AUTO-BOUNCE: Your sphere bounces automatically on every floor, slope, and moving carrier.\n" +
                        "• BOOST / PUMP [▲]: Hold or tap BOOST to bounce higher or execute one mid-air upward boost per bounce cycle.\n" +
                        "• RINGS [O]: Collect all Rings in a stage to unlock the Exit Portal [E].\n" +
                        "• SPRING PADS [^]: Launch the sphere high into vertical shafts.\n" +
                        "• FRAGILE TILES [=]: Crack when bounced on, then regenerate after 3 seconds.\n" +
                        "• CHECKPOINTS [C]: Save your respawn position if you hit spikes or patrol blades.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = palette.pixelBright
                )
                Text(
                    text = "KEYBOARD & TOUCH CONTROLS:",
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.pixelBright
                )
                Text(
                    text = "• Touch [<] and [>] or Keyboard [A]/[D] or [Left]/[Right] to steer horizontally.\n" +
                        "• Touch [▲ BOOST] or Keyboard [SPACE]/[W]/[UP] to boost bounce height.\n" +
                        "• Touch [||] or Keyboard [ESC]/[P] to pause.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = palette.pixelMid
                )
            }
        }
    }
}
