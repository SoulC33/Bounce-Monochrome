package com.example.engine

import com.example.model.CheckpointState
import com.example.model.Collectible
import com.example.model.CollectibleType
import com.example.model.CrumbleBlockState
import com.example.model.Hazard
import com.example.model.HazardType
import com.example.model.Level
import com.example.model.MovingPlatform
import com.example.model.TileType

data class LevelBlueprint(
    val levelNumber: Int,
    val title: String,
    val subtitle: String,
    val parSeconds: Int,
    val mapRows: List<String>
)

class LevelManager {

    val blueprints: List<LevelBlueprint> = listOf(
        // LEVEL 1: Tutorial / Simple Terrain
        LevelBlueprint(
            levelNumber = 1,
            title = "FIRST SIGNAL",
            subtitle = "TUTORIAL // LEARN THE BOUNCE",
            parSeconds = 35,
            mapRows = listOf(
                "MMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMM",
                "M..........................................M",
                "M..........................................M",
                "M..............O............*..............M",
                "M............#####........#####............M",
                "M..........................................M",
                "M.......O.............C.............O......M",
                "M.....#####.........#####.........#####....M",
                "M..........................................M",
                "M..S.........O..../#######\\....O........E..M",
                "M############^#################A#########MMM",
                "MMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMM"
            )
        ),

        // LEVEL 2: Basic Gaps and Platforms
        LevelBlueprint(
            levelNumber = 2,
            title = "GAP JUMPER",
            subtitle = "BASIC GAPS & STEPPING STONES",
            parSeconds = 45,
            mapRows = listOf(
                "MMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMM",
                "M..................................................M",
                "M.....................*..O..*......................M",
                "M....................#########.....................M",
                "M..................................................M",
                "M..........O...........................O...........M",
                "M........#####.......C.......O.......#####.........M",
                "M...................#####..#####...................M",
                "M..................................................M",
                "M..S...*.......O...........................*....E..M",
                "M######..#####^###..#####....#####..######..#######M",
                "M######..#########..#####....#####..######..#######M"
            )
        ),

        // LEVEL 3: Moving Platforms
        LevelBlueprint(
            levelNumber = 3,
            title = "SHIFTING PLATES",
            subtitle = "TIMED MOVING CARRIERS OVER VOIDS",
            parSeconds = 55,
            mapRows = listOf(
                "MMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMM",
                "M......................................................M",
                "M........................*...O...*.....................M",
                "M.......................###########....................M",
                "M......................................................M",
                "M..........O.................C................O........M",
                "M..................U.................H.................M",
                "M........#####.............#####.......................M",
                "M...........................................#####......M",
                "M..S...............O.................*..............E..M",
                "M######..H.....#########...H.....######.....H....######M",
                "M######........#########.........######..........######M"
            )
        ),

        // LEVEL 4: Narrow Platforms & Crumbly Bridges
        LevelBlueprint(
            levelNumber = 4,
            title = "RAZOR LEDGES",
            subtitle = "NARROW PILLARS & FRAGILE TILES",
            parSeconds = 60,
            mapRows = listOf(
                "MMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMM",
                "M........................................................M",
                "M................*..........O..........+.................M",
                "M...............===........###........===................M",
                "M........................................................M",
                "M........O..................C..................O.........M",
                "M.......###....===........#####........===....###........M",
                "M........................................................M",
                "M..S.........O..........O.......*..........O..........E..M",
                "M#####..##..##^##..===..##.....###..===..###..##..#######M",
                "M#####..##..#####.......##.....###.......###..##..#######M",
                "M#####..##..#####.......##.....###.......###..##..#######M"
            )
        ),

        // LEVEL 5: Vertical Sections (Multi-tier Ascent & Descent)
        LevelBlueprint(
            levelNumber = 5,
            title = "HIGH TOWER",
            subtitle = "VERTICAL SHAFTS & SPRING LIFTS",
            parSeconds = 70,
            mapRows = listOf(
                "MMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMM",
                "M..........................................M",
                "M....O.........*.....C.....*.........O.....M",
                "M..#####.....#################.....#####...M",
                "M....................M.....................M",
                "M.........O..........M..........O..........M",
                "M.......#####........M........#####........M",
                "M....................M.....................M",
                "M..*.................M.................*...M",
                "M#####......O........M........O......#####.M",
                "M.........#####......M......#####..........M",
                "M....................M.....................M",
                "M....O...............M...............*.....M",
                "M..#####.............M.............#####...M",
                "M....................M.....................M",
                "M..S........*........M........O.........E..M",
                "M######^##########^##M#####^######A########M",
                "MMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMM"
            )
        ),

        // LEVEL 6: More Dangerous Obstacles (Spike Foundry)
        LevelBlueprint(
            levelNumber = 6,
            title = "SPIKE FOUNDRY",
            subtitle = "STATIC SPIKES & PATROL BLADES",
            parSeconds = 70,
            mapRows = listOf(
                "MMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMM",
                "M............VVV.........................VVV...............M",
                "M..........................................................M",
                "M...................O...............O......................M",
                "M.................#####...........#####....................M",
                "M..........................................................M",
                "M........O..................C...................O..........M",
                "M......#####....X.........#####.........Y.....#####........M",
                "M..........................................................M",
                "M..S.........*........O...........*.........O...........E..M",
                "M######AA######^####AA#####...######AA#####^#####AA########M",
                "MMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMM"
            )
        ),

        // LEVEL 7: Faster Scrolling Momentum Course
        LevelBlueprint(
            levelNumber = 7,
            title = "OVERCLOCKED RUN",
            subtitle = "HIGH-SPEED SLOPES & SPRING CHAINS",
            parSeconds = 65,
            mapRows = listOf(
                "MMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMM",
                "M................................................................M",
                "M..............O.................+.................O.............M",
                "M............/###\\.............#####............./###\\...........M",
                "M................................................................M",
                "M.......O..............*.........C.........*..............O......M",
                "M...../###\\..........=====.....#####.....=====........../###\\....M",
                "M................................................................M",
                "M..S.........O...................O...................O........E..M",
                "M#####/###\\##^##/###\\..^..#####^###^#####..^../###\\##^######/####M",
                "M####################.....###############.....###################M",
                "MMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMM"
            )
        ),

        // LEVEL 8: More Complicated Platform Layouts (Labyrinth Matrix)
        LevelBlueprint(
            levelNumber = 8,
            title = "LABYRINTH MATRIX",
            subtitle = "UPPER & LOWER CHAMBERS // MULTI-PATH",
            parSeconds = 85,
            mapRows = listOf(
                "MMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMM",
                "M..........................................................M",
                "M....O............*..........O..........*............O.....M",
                "M..#####....H...#####......#####......#####...H....#####...M",
                "M..........................................................M",
                "M.........#####.......MMMM.......MMMM.......#####..........M",
                "M.....................M..M...C...M..M......................M",
                "M....*.......O........M..#########..M........O.......*.....M",
                "M..#####...#####......M.............M......#####...#####...M",
                "M.....................M......O......M......................M",
                "M.........U...........M....#####....M...........U..........M",
                "M..........................................................M",
                "M..S............O............*............O.............E..M",
                "M######^#####AA####^#####..#####..#####^####AA#####^#######M",
                "MMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMM"
            )
        ),

        // LEVEL 9: Combination of Previous Mechanics
        LevelBlueprint(
            levelNumber = 9,
            title = "SYSTEM GAUNTLET",
            subtitle = "MOVING CARRIERS, SLOPES, SPIKES & FRAGILES",
            parSeconds = 90,
            mapRows = listOf(
                "MMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMM",
                "M..................VVV.....................VVV.................M",
                "M...........O...................+...................O..........M",
                "M.........#####....===........#####........===....#####........M",
                "M..............................................................M",
                "M....................O..........C..........O...................M",
                "M.......U..........#####......#####......#####..........U......M",
                "M..............................................................M",
                "M.............X.................*.................Y............M",
                "M..S....O...........*.......................*...........O...E..M",
                "M#####/###\\..H..===^===..#####AA#####..===^===..H../###\\#######M",
                "M##########..............############..............############M"
            )
        ),

        // LEVEL 10: Final Challenging Master Level
        LevelBlueprint(
            levelNumber = 10,
            title = "CORE OVERRIDE",
            subtitle = "FINAL STAGE // MASTER MONO CHALLENGE",
            parSeconds = 110,
            mapRows = listOf(
                "MMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMM",
                "M.............VVV...............VVV...............VVV..............M",
                "M......O...............*.........O.........+...............O.......M",
                "M....#####....===....#####.....#####.....#####....===....#####.....M",
                "M..................................................................M",
                "M..................X...........................X...................M",
                "M...........O..................C...................O...............M",
                "M........./###\\......H.......#####.......H......./###\\.............M",
                "M..................................................................M",
                "M....U.................Y.................Y.................U.......M",
                "M..................................................................M",
                "M..S.........*.........O.................O.........*............E..M",
                "M#####..##^##..===..#####AA###^###AA#####..===..##^##..####AA######M",
                "M#####..#####.......#####################.......#####..############M"
            )
        )
    )

    fun loadLevel(levelNumber: Int): Level {
        val index = (levelNumber - 1).coerceIn(0, blueprints.lastIndex)
        val bp = blueprints[index]
        return parseBlueprint(bp)
    }

    private fun parseBlueprint(bp: LevelBlueprint): Level {
        val height = bp.mapRows.size
        val width = bp.mapRows.maxOfOrNull { it.length } ?: 32
        val tiles = Array(height) { Array(width) { TileType.EMPTY } }

        var spawnX = 24f
        var spawnY = 24f
        var exitGridX = width - 4
        var exitGridY = height - 3

        val collectibles = mutableListOf<Collectible>()
        val hazards = mutableListOf<Hazard>()
        val movingPlatforms = mutableListOf<MovingPlatform>()
        val crumbleBlocks = mutableMapOf<Pair<Int, Int>, CrumbleBlockState>()
        val checkpoints = mutableListOf<CheckpointState>()

        var collectibleId = 1
        var hazardId = 1
        var platformId = 1

        for (gy in 0 until height) {
            val row = bp.mapRows[gy]
            for (gx in 0 until width) {
                val ch = if (gx < row.length) row[gx] else '.'
                val centerX = (gx + 0.5f) * Level.TILE_SIZE
                val centerY = (gy + 0.5f) * Level.TILE_SIZE

                when (ch) {
                    '#' -> tiles[gy][gx] = TileType.SOLID_BRICK
                    'M' -> tiles[gy][gx] = TileType.SOLID_METAL
                    '^' -> tiles[gy][gx] = TileType.SPRING_PAD
                    '~' -> tiles[gy][gx] = TileType.DAMP_FLOOR
                    '=' -> {
                        tiles[gy][gx] = TileType.CRUMBLE_TILE
                        crumbleBlocks[gx to gy] = CrumbleBlockState(gridX = gx, gridY = gy)
                    }
                    '/' -> tiles[gy][gx] = TileType.SLOPE_UP_RIGHT
                    '\\' -> tiles[gy][gx] = TileType.SLOPE_DOWN_RIGHT
                    'A' -> tiles[gy][gx] = TileType.SPIKE_UP
                    'V' -> tiles[gy][gx] = TileType.SPIKE_DOWN
                    '<' -> tiles[gy][gx] = TileType.SPIKE_LEFT
                    '>' -> tiles[gy][gx] = TileType.SPIKE_RIGHT
                    'S' -> {
                        spawnX = centerX
                        spawnY = centerY
                    }
                    'E' -> {
                        tiles[gy][gx] = TileType.EXIT_PORTAL
                        exitGridX = gx
                        exitGridY = gy
                    }
                    'C' -> {
                        tiles[gy][gx] = TileType.CHECKPOINT
                        checkpoints.add(
                            CheckpointState(
                                gridX = gx,
                                gridY = gy,
                                worldX = centerX,
                                worldY = centerY
                            )
                        )
                    }
                    'O' -> {
                        collectibles.add(
                            Collectible(
                                id = collectibleId++,
                                gridX = gx,
                                gridY = gy,
                                x = centerX,
                                y = centerY,
                                type = CollectibleType.RING
                            )
                        )
                    }
                    '*' -> {
                        collectibles.add(
                            Collectible(
                                id = collectibleId++,
                                gridX = gx,
                                gridY = gy,
                                x = centerX,
                                y = centerY,
                                type = CollectibleType.DATA_GEM
                            )
                        )
                    }
                    '+' -> {
                        collectibles.add(
                            Collectible(
                                id = collectibleId++,
                                gridX = gx,
                                gridY = gy,
                                x = centerX,
                                y = centerY,
                                type = CollectibleType.EXTRA_LIFE
                            )
                        )
                    }
                    'H' -> {
                        val px = gx * Level.TILE_SIZE
                        val py = gy * Level.TILE_SIZE
                        movingPlatforms.add(
                            MovingPlatform(
                                id = platformId++,
                                startX = px,
                                startY = py,
                                x = px,
                                y = py,
                                width = 22f,
                                height = 5f,
                                speedX = 1.0f,
                                speedY = 0f,
                                rangeX = 14f,
                                rangeY = 0f,
                                phase = (platformId * 1.1f)
                            )
                        )
                    }
                    'U' -> {
                        val px = gx * Level.TILE_SIZE
                        val py = gy * Level.TILE_SIZE
                        movingPlatforms.add(
                            MovingPlatform(
                                id = platformId++,
                                startX = px,
                                startY = py,
                                x = px,
                                y = py,
                                width = 20f,
                                height = 5f,
                                speedX = 0f,
                                speedY = 1.0f,
                                rangeX = 0f,
                                rangeY = 14f,
                                phase = (platformId * 0.9f)
                            )
                        )
                    }
                    'X' -> {
                        val hx = gx * Level.TILE_SIZE + 1f
                        val hy = gy * Level.TILE_SIZE + 1f
                        hazards.add(
                            Hazard(
                                id = hazardId++,
                                startX = hx,
                                startY = hy,
                                x = hx,
                                y = hy,
                                width = 6f,
                                height = 6f,
                                speedX = 1.15f,
                                speedY = 0f,
                                rangeX = 14f,
                                rangeY = 0f,
                                type = HazardType.PATROL_BLADE,
                                phase = hazardId * 0.7f
                            )
                        )
                    }
                    'Y' -> {
                        val hx = gx * Level.TILE_SIZE + 1f
                        val hy = gy * Level.TILE_SIZE + 1f
                        hazards.add(
                            Hazard(
                                id = hazardId++,
                                startX = hx,
                                startY = hy,
                                x = hx,
                                y = hy,
                                width = 6f,
                                height = 6f,
                                speedX = 0f,
                                speedY = 1.25f,
                                rangeX = 0f,
                                rangeY = 10f,
                                type = HazardType.PISTON_CRUSHER,
                                phase = hazardId * 1.3f
                            )
                        )
                    }
                }
            }
        }

        return Level(
            levelNumber = bp.levelNumber,
            title = bp.title,
            subtitle = bp.subtitle,
            parSeconds = bp.parSeconds,
            widthInTiles = width,
            heightInTiles = height,
            tiles = tiles,
            spawnX = spawnX,
            spawnY = spawnY,
            exitGridX = exitGridX,
            exitGridY = exitGridY,
            collectibles = collectibles,
            hazards = hazards,
            movingPlatforms = movingPlatforms,
            crumbleBlocks = crumbleBlocks,
            checkpoints = checkpoints
        )
    }
}
