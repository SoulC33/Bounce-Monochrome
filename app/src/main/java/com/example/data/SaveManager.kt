package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.model.ControlLayoutPreset
import com.example.model.GameSettings
import com.example.model.MonoPaletteMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@Entity(tableName = "level_progress")
data class LevelProgressEntity(
    @PrimaryKey val levelNumber: Int,
    val unlocked: Boolean,
    val completed: Boolean = false,
    val bestScore: Int = 0,
    val bestRings: Int = 0,
    val totalRings: Int = 0,
    val bestTimeSeconds: Int = 0
)

@Dao
interface LevelProgressDao {
    @Query("SELECT * FROM level_progress ORDER BY levelNumber ASC")
    fun observeAllProgress(): Flow<List<LevelProgressEntity>>

    @Query("SELECT * FROM level_progress ORDER BY levelNumber ASC")
    suspend fun getAllProgressOnce(): List<LevelProgressEntity>

    @Query("SELECT * FROM level_progress WHERE levelNumber = :levelNumber LIMIT 1")
    suspend fun getLevelProgress(levelNumber: Int): LevelProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProgress(entity: LevelProgressEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<LevelProgressEntity>)

    @Query("DELETE FROM level_progress")
    suspend fun clearAll()
}

@Database(
    entities = [LevelProgressEntity::class],
    version = 1,
    exportSchema = false
)
abstract class BounceDatabase : RoomDatabase() {
    abstract fun levelProgressDao(): LevelProgressDao

    companion object {
        @Volatile
        private var INSTANCE: BounceDatabase? = null

        fun getInstance(context: Context): BounceDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    BounceDatabase::class.java,
                    "mono_bounce_progress.db"
                ).fallbackToDestructiveMigration(dropAllTables = true).build().also {
                    INSTANCE = it
                }
            }
        }
    }
}

class SaveManager(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val database = BounceDatabase.getInstance(context)
    private val dao = database.levelProgressDao()
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _settings = MutableStateFlow(loadSettingsFromPrefs())
    val settings: StateFlow<GameSettings> = _settings.asStateFlow()

    private val _highestUnlockedLevel = MutableStateFlow(prefs.getInt(KEY_HIGHEST_UNLOCKED, 1).coerceIn(1, TOTAL_LEVELS))
    val highestUnlockedLevel: StateFlow<Int> = _highestUnlockedLevel.asStateFlow()

    private val _lastPlayedLevel = MutableStateFlow(prefs.getInt(KEY_LAST_PLAYED_LEVEL, 1).coerceIn(1, TOTAL_LEVELS))
    val lastPlayedLevel: StateFlow<Int> = _lastPlayedLevel.asStateFlow()

    private val _highScore = MutableStateFlow(prefs.getInt(KEY_HIGH_SCORE, 0))
    val highScore: StateFlow<Int> = _highScore.asStateFlow()

    val levelProgressFlow: Flow<List<LevelProgressEntity>> = dao.observeAllProgress()

    init {
        ioScope.launch {
            val existing = dao.getAllProgressOnce()
            if (existing.isEmpty()) {
                val initial = (1..TOTAL_LEVELS).map { lvl ->
                    LevelProgressEntity(
                        levelNumber = lvl,
                        unlocked = lvl <= _highestUnlockedLevel.value,
                        completed = false,
                        bestScore = 0,
                        bestRings = 0,
                        totalRings = 0,
                        bestTimeSeconds = 0
                    )
                }
                dao.upsertAll(initial)
            }
        }
    }

    private fun loadSettingsFromPrefs(): GameSettings {
        val paletteOrdinal = prefs.getInt(KEY_PALETTE_MODE, 0)
        val palette = MonoPaletteMode.entries.getOrElse(paletteOrdinal) { MonoPaletteMode.CLASSIC_DARK }

        val layoutOrdinal = prefs.getInt(KEY_CONTROL_LAYOUT, 0)
        val layout = ControlLayoutPreset.entries.getOrElse(layoutOrdinal) { ControlLayoutPreset.DPAD_LEFT_BOOST_RIGHT }

        return GameSettings(
            soundEnabled = prefs.getBoolean(KEY_SOUND_ENABLED, true),
            musicEnabled = prefs.getBoolean(KEY_MUSIC_ENABLED, true),
            hapticsEnabled = prefs.getBoolean(KEY_HAPTICS_ENABLED, true),
            pixelGridEnabled = prefs.getBoolean(KEY_PIXEL_GRID, true),
            scanlinesEnabled = prefs.getBoolean(KEY_SCANLINES, true),
            paletteMode = palette,
            buttonSizeDp = prefs.getInt(KEY_BUTTON_SIZE_DP, 68).coerceIn(52, 92),
            buttonHorizontalPaddingDp = prefs.getInt(KEY_BUTTON_H_PADDING_DP, 16).coerceIn(4, 48),
            buttonVerticalOffsetDp = prefs.getInt(KEY_BUTTON_V_OFFSET_DP, 12).coerceIn(0, 48),
            controlLayout = layout
        )
    }

    fun updateSettings(transform: (GameSettings) -> GameSettings) {
        val updated = transform(_settings.value)
        _settings.value = updated
        prefs.edit()
            .putBoolean(KEY_SOUND_ENABLED, updated.soundEnabled)
            .putBoolean(KEY_MUSIC_ENABLED, updated.musicEnabled)
            .putBoolean(KEY_HAPTICS_ENABLED, updated.hapticsEnabled)
            .putBoolean(KEY_PIXEL_GRID, updated.pixelGridEnabled)
            .putBoolean(KEY_SCANLINES, updated.scanlinesEnabled)
            .putInt(KEY_PALETTE_MODE, updated.paletteMode.ordinal)
            .putInt(KEY_BUTTON_SIZE_DP, updated.buttonSizeDp)
            .putInt(KEY_BUTTON_H_PADDING_DP, updated.buttonHorizontalPaddingDp)
            .putInt(KEY_BUTTON_V_OFFSET_DP, updated.buttonVerticalOffsetDp)
            .putInt(KEY_CONTROL_LAYOUT, updated.controlLayout.ordinal)
            .apply()
    }

    fun recordLastPlayedLevel(levelNumber: Int) {
        val clamped = levelNumber.coerceIn(1, TOTAL_LEVELS)
        _lastPlayedLevel.value = clamped
        prefs.edit().putInt(KEY_LAST_PLAYED_LEVEL, clamped).apply()
    }

    fun recordLevelCompleted(
        levelNumber: Int,
        stageScore: Int,
        totalRunScore: Int,
        ringsCollected: Int,
        totalRings: Int,
        elapsedSeconds: Int
    ): Boolean {
        val nextLevel = (levelNumber + 1).coerceAtMost(TOTAL_LEVELS)
        if (nextLevel > _highestUnlockedLevel.value) {
            _highestUnlockedLevel.value = nextLevel
            prefs.edit().putInt(KEY_HIGHEST_UNLOCKED, nextLevel).apply()
        }
        val isNewHigh = totalRunScore > _highScore.value
        if (isNewHigh) {
            _highScore.value = totalRunScore
            prefs.edit().putInt(KEY_HIGH_SCORE, totalRunScore).apply()
        }

        ioScope.launch {
            val current = dao.getLevelProgress(levelNumber)
            val bestScore = maxOf(current?.bestScore ?: 0, stageScore)
            val bestRings = maxOf(current?.bestRings ?: 0, ringsCollected)
            val bestTime = if (current == null || current.bestTimeSeconds == 0) {
                elapsedSeconds
            } else {
                minOf(current.bestTimeSeconds, elapsedSeconds)
            }
            dao.upsertProgress(
                LevelProgressEntity(
                    levelNumber = levelNumber,
                    unlocked = true,
                    completed = true,
                    bestScore = bestScore,
                    bestRings = bestRings,
                    totalRings = totalRings,
                    bestTimeSeconds = bestTime
                )
            )
            if (levelNumber < TOTAL_LEVELS) {
                val nextExisting = dao.getLevelProgress(nextLevel)
                dao.upsertProgress(
                    (nextExisting ?: LevelProgressEntity(levelNumber = nextLevel, unlocked = true))
                        .copy(unlocked = true)
                )
            }
        }
        return isNewHigh
    }

    fun unlockAllLevels() {
        _highestUnlockedLevel.value = TOTAL_LEVELS
        prefs.edit().putInt(KEY_HIGHEST_UNLOCKED, TOTAL_LEVELS).apply()
        ioScope.launch {
            val currentList = dao.getAllProgressOnce().associateBy { it.levelNumber }
            val updated = (1..TOTAL_LEVELS).map { lvl ->
                val existing = currentList[lvl]
                existing?.copy(unlocked = true) ?: LevelProgressEntity(levelNumber = lvl, unlocked = true)
            }
            dao.upsertAll(updated)
        }
    }

    fun resetProgress() {
        _highestUnlockedLevel.value = 1
        _lastPlayedLevel.value = 1
        _highScore.value = 0
        prefs.edit()
            .putInt(KEY_HIGHEST_UNLOCKED, 1)
            .putInt(KEY_LAST_PLAYED_LEVEL, 1)
            .putInt(KEY_HIGH_SCORE, 0)
            .apply()
        ioScope.launch {
            dao.clearAll()
            val initial = (1..TOTAL_LEVELS).map { lvl ->
                LevelProgressEntity(
                    levelNumber = lvl,
                    unlocked = lvl == 1
                )
            }
            dao.upsertAll(initial)
        }
    }

    companion object {
        const val TOTAL_LEVELS = 10
        private const val PREFS_NAME = "mono_bounce_prefs"
        private const val KEY_HIGHEST_UNLOCKED = "highest_unlocked"
        private const val KEY_LAST_PLAYED_LEVEL = "last_played_level"
        private const val KEY_HIGH_SCORE = "high_score"
        private const val KEY_SOUND_ENABLED = "sound_enabled"
        private const val KEY_MUSIC_ENABLED = "music_enabled"
        private const val KEY_HAPTICS_ENABLED = "haptics_enabled"
        private const val KEY_PIXEL_GRID = "pixel_grid_enabled"
        private const val KEY_SCANLINES = "scanlines_enabled"
        private const val KEY_PALETTE_MODE = "palette_mode"
        private const val KEY_BUTTON_SIZE_DP = "button_size_dp"
        private const val KEY_BUTTON_H_PADDING_DP = "button_h_padding_dp"
        private const val KEY_BUTTON_V_OFFSET_DP = "button_v_offset_dp"
        private const val KEY_CONTROL_LAYOUT = "control_layout"
    }
}
