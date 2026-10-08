package com.example.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.LcdBacklightLight
import com.example.ui.theme.LcdBacklightMid
import com.example.ui.theme.LcdPixelBlack
import com.example.ui.theme.LcdPixelDark
import com.example.ui.theme.LcdPixelMid
import com.example.ui.theme.MonoBlack
import com.example.ui.theme.MonoDarkGray
import com.example.ui.theme.MonoLightGray
import com.example.ui.theme.MonoMidGray
import com.example.ui.theme.MonoWhite
import com.example.ui.theme.PureBlack
import com.example.ui.theme.PureDarkGray
import com.example.ui.theme.PureLightGray
import com.example.ui.theme.PureMidGray
import com.example.ui.theme.PureWhite

enum class GameState {
    MAIN_MENU,
    LEVEL_SELECT,
    GAMEPLAY,
    PAUSED,
    LEVEL_COMPLETE,
    GAME_OVER,
    SETTINGS,
    ABOUT
}

enum class TileType {
    EMPTY,
    SOLID_BRICK,
    SOLID_METAL,
    SPRING_PAD,
    DAMP_FLOOR,
    CRUMBLE_TILE,
    SLOPE_UP_RIGHT,
    SLOPE_DOWN_RIGHT,
    SPIKE_UP,
    SPIKE_DOWN,
    SPIKE_LEFT,
    SPIKE_RIGHT,
    CHECKPOINT,
    EXIT_PORTAL
}

enum class SurfaceType(val bounceMultiplier: Float) {
    NORMAL(1.0f),
    SPRING(1.42f),
    DAMPENED(0.68f),
    SLOPE(1.04f),
    MOVING_PLATFORM(1.05f)
}

enum class MonoPaletteMode(val displayName: String, val description: String) {
    CLASSIC_DARK("MONO GREEN-DARK", "Dark olive screen with backlit greenish-gray LCD pixels"),
    INVERTED_LCD("NOKIA LCD BACKLIT", "Dark olive ink pixels on classic greenish LCD backlight"),
    HIGH_CONTRAST("CRISP PHOSPHOR", "High-contrast dark green & bright mint-white pixels")
}

data class ActiveMonoColors(
    val bgDeep: Color,
    val bgSurface: Color,
    val pixelDim: Color,
    val pixelMid: Color,
    val pixelBright: Color
)

fun MonoPaletteMode.toColors(): ActiveMonoColors = when (this) {
    MonoPaletteMode.CLASSIC_DARK -> ActiveMonoColors(
        bgDeep = MonoBlack,
        bgSurface = MonoDarkGray,
        pixelDim = MonoMidGray,
        pixelMid = MonoLightGray,
        pixelBright = MonoWhite
    )
    MonoPaletteMode.INVERTED_LCD -> ActiveMonoColors(
        bgDeep = LcdBacklightLight,
        bgSurface = LcdBacklightMid,
        pixelDim = LcdPixelMid,
        pixelMid = LcdPixelDark,
        pixelBright = LcdPixelBlack
    )
    MonoPaletteMode.HIGH_CONTRAST -> ActiveMonoColors(
        bgDeep = PureBlack,
        bgSurface = PureDarkGray,
        pixelDim = PureMidGray,
        pixelMid = PureLightGray,
        pixelBright = PureWhite
    )
}

enum class ControlLayoutPreset(val label: String) {
    DPAD_LEFT_BOOST_RIGHT("D-PAD LEFT / BOOST RIGHT"),
    SPLIT_EDGES("SPLIT L/R EDGES + RIGHT BOOST"),
    DPAD_RIGHT_BOOST_LEFT("BOOST LEFT / D-PAD RIGHT")
}

data class GameSettings(
    val soundEnabled: Boolean = true,
    val musicEnabled: Boolean = true,
    val sfxVolumePercent: Int = 85,
    val musicVolumePercent: Int = 70,
    val hapticsEnabled: Boolean = true,
    val pixelGridEnabled: Boolean = true,
    val scanlinesEnabled: Boolean = true,
    val paletteMode: MonoPaletteMode = MonoPaletteMode.CLASSIC_DARK,
    val buttonSizeDp: Int = 68, // 56 (Small), 68 (Medium), 82 (Large)
    val buttonHorizontalPaddingDp: Int = 16,
    val buttonVerticalOffsetDp: Int = 12,
    val controlLayout: ControlLayoutPreset = ControlLayoutPreset.DPAD_LEFT_BOOST_RIGHT
)

data class LevelClearResult(
    val levelNumber: Int,
    val levelTitle: String,
    val ringsCollected: Int,
    val totalRings: Int,
    val gemsCollected: Int,
    val totalGems: Int,
    val elapsedSeconds: Int,
    val parSeconds: Int,
    val livesRemaining: Int,
    val stageScore: Int,
    val totalRunScore: Int,
    val isNewHighScore: Boolean,
    val hasNextLevel: Boolean
)
