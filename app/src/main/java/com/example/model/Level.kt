package com.example.model

class Level(
    val levelNumber: Int,
    val title: String,
    val subtitle: String,
    val parSeconds: Int,
    val widthInTiles: Int,
    val heightInTiles: Int,
    val tiles: Array<Array<TileType>>,
    val spawnX: Float,
    val spawnY: Float,
    val exitGridX: Int,
    val exitGridY: Int,
    val collectibles: MutableList<Collectible>,
    val hazards: MutableList<Hazard>,
    val movingPlatforms: MutableList<MovingPlatform>,
    val crumbleBlocks: MutableMap<Pair<Int, Int>, CrumbleBlockState>,
    val checkpoints: MutableList<CheckpointState>
) {
    companion object {
        const val TILE_SIZE = 8f
    }

    val pixelWidth: Float get() = widthInTiles * TILE_SIZE
    val pixelHeight: Float get() = heightInTiles * TILE_SIZE

    val totalRings: Int = collectibles.count { it.type == CollectibleType.RING }
    val totalGems: Int = collectibles.count { it.type == CollectibleType.DATA_GEM }

    val ringsCollected: Int
        get() = collectibles.count { it.type == CollectibleType.RING && it.collected }

    val gemsCollected: Int
        get() = collectibles.count { it.type == CollectibleType.DATA_GEM && it.collected }

    val isExitUnlocked: Boolean
        get() = ringsCollected >= totalRings

    fun getTileAtGrid(gx: Int, gy: Int): TileType {
        if (gy < 0 || gy >= heightInTiles || gx < 0 || gx >= widthInTiles) {
            // Left and right boundaries act as solid walls; above top is empty; below bottom is pit
            return if (gx < 0 || gx >= widthInTiles) TileType.SOLID_METAL else TileType.EMPTY
        }
        val tile = tiles[gy][gx]
        if (tile == TileType.CRUMBLE_TILE) {
            val state = crumbleBlocks[gx to gy]
            if (state != null && !state.isSolid) {
                return TileType.EMPTY
            }
        }
        return tile
    }

    fun isSolidTile(tile: TileType): Boolean {
        return tile == TileType.SOLID_BRICK ||
            tile == TileType.SOLID_METAL ||
            tile == TileType.SPRING_PAD ||
            tile == TileType.DAMP_FLOOR ||
            tile == TileType.CRUMBLE_TILE
    }

    fun isSpikeTile(tile: TileType): Boolean {
        return tile == TileType.SPIKE_UP ||
            tile == TileType.SPIKE_DOWN ||
            tile == TileType.SPIKE_LEFT ||
            tile == TileType.SPIKE_RIGHT
    }
}
