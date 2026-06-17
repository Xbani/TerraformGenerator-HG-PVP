package org.terraform.cave;

import org.bukkit.Material;
import org.bukkit.generator.ChunkGenerator;
import org.jetbrains.annotations.NotNull;
import org.terraform.coregen.ChunkCache;
import org.terraform.coregen.HeightMap;
import org.terraform.coregen.bukkit.TerraformGenerator;
import org.terraform.data.TerraformWorld;
import org.terraform.main.TerraformGeneratorPlugin;
import org.terraform.main.config.TConfig;

import java.util.Random;

public class TerrainControlSmallLakeGen {
    private static final int CHUNK_SIZE = 16;
    private static final int LAKE_SIZE_XZ = 16;
    private static final int LAKE_SIZE_Y = 8;
    private static final int LAKE_LIQUID_HEIGHT = 4;

    public void populate(@NotNull TerraformWorld tw,
                         int chunkX,
                         int chunkZ,
                         @NotNull ChunkGenerator.ChunkData chunkData,
                         @NotNull ChunkCache cache)
    {
        if (!TConfig.areCavesEnabled() || !TConfig.c.CAVES_TERRAIN_CONTROL_SMALL_LAKES_ENABLED) {
            return;
        }

        Random seedRandom = new Random(tw.getSeed());
        long worldLong1 = seedRandom.nextLong() / 2L * 2L + 1L;
        long worldLong2 = seedRandom.nextLong() / 2L * 2L + 1L;

        for (int sourceChunkX = chunkX - 1; sourceChunkX <= chunkX; sourceChunkX++) {
            for (int sourceChunkZ = chunkZ - 1; sourceChunkZ <= chunkZ; sourceChunkZ++) {
                Random random = new Random(sourceChunkX * worldLong1 + sourceChunkZ * worldLong2 ^ tw.getSeed());
                generateLakeAttempts(random,
                        tw,
                        sourceChunkX,
                        sourceChunkZ,
                        chunkX,
                        chunkZ,
                        chunkData,
                        cache,
                        Material.WATER,
                        TConfig.c.CAVES_TERRAIN_CONTROL_SMALL_LAKES_WATER_ENABLED,
                        TConfig.c.CAVES_TERRAIN_CONTROL_SMALL_LAKES_WATER_FREQUENCY,
                        TConfig.c.CAVES_TERRAIN_CONTROL_SMALL_LAKES_WATER_RARITY,
                        TConfig.c.CAVES_TERRAIN_CONTROL_SMALL_LAKES_WATER_MIN_ALTITUDE,
                        TConfig.c.CAVES_TERRAIN_CONTROL_SMALL_LAKES_WATER_MAX_ALTITUDE,
                        TConfig.c.CAVES_TERRAIN_CONTROL_SMALL_LAKES_WATER_MIN_ELLIPSOIDS,
                        TConfig.c.CAVES_TERRAIN_CONTROL_SMALL_LAKES_WATER_MAX_ELLIPSOIDS,
                        TConfig.c.CAVES_TERRAIN_CONTROL_SMALL_LAKES_WATER_HORIZONTAL_SIZE_MIN,
                        TConfig.c.CAVES_TERRAIN_CONTROL_SMALL_LAKES_WATER_HORIZONTAL_SIZE_MAX,
                        TConfig.c.CAVES_TERRAIN_CONTROL_SMALL_LAKES_WATER_VERTICAL_SIZE_MIN,
                        TConfig.c.CAVES_TERRAIN_CONTROL_SMALL_LAKES_WATER_VERTICAL_SIZE_MAX);
                generateLakeAttempts(random,
                        tw,
                        sourceChunkX,
                        sourceChunkZ,
                        chunkX,
                        chunkZ,
                        chunkData,
                        cache,
                        Material.LAVA,
                        TConfig.c.CAVES_TERRAIN_CONTROL_SMALL_LAKES_LAVA_ENABLED,
                        TConfig.c.CAVES_TERRAIN_CONTROL_SMALL_LAKES_LAVA_FREQUENCY,
                        TConfig.c.CAVES_TERRAIN_CONTROL_SMALL_LAKES_LAVA_RARITY,
                        TConfig.c.CAVES_TERRAIN_CONTROL_SMALL_LAKES_LAVA_MIN_ALTITUDE,
                        TConfig.c.CAVES_TERRAIN_CONTROL_SMALL_LAKES_LAVA_MAX_ALTITUDE,
                        TConfig.c.CAVES_TERRAIN_CONTROL_SMALL_LAKES_LAVA_MIN_ELLIPSOIDS,
                        TConfig.c.CAVES_TERRAIN_CONTROL_SMALL_LAKES_LAVA_MAX_ELLIPSOIDS,
                        TConfig.c.CAVES_TERRAIN_CONTROL_SMALL_LAKES_LAVA_HORIZONTAL_SIZE_MIN,
                        TConfig.c.CAVES_TERRAIN_CONTROL_SMALL_LAKES_LAVA_HORIZONTAL_SIZE_MAX,
                        TConfig.c.CAVES_TERRAIN_CONTROL_SMALL_LAKES_LAVA_VERTICAL_SIZE_MIN,
                        TConfig.c.CAVES_TERRAIN_CONTROL_SMALL_LAKES_LAVA_VERTICAL_SIZE_MAX);
            }
        }
    }

    private void generateLakeAttempts(@NotNull Random sourceRandom,
                                      @NotNull TerraformWorld tw,
                                      int sourceChunkX,
                                      int sourceChunkZ,
                                      int generatingChunkX,
                                      int generatingChunkZ,
                                      @NotNull ChunkGenerator.ChunkData chunkData,
                                      @NotNull ChunkCache cache,
                                      @NotNull Material material,
                                      boolean enabled,
                                      int frequency,
                                      int rarity,
                                      int minAltitude,
                                      int maxAltitude,
                                      int minEllipsoids,
                                      int maxEllipsoids,
                                      double horizontalSizeMin,
                                      double horizontalSizeMax,
                                      double verticalSizeMin,
                                      double verticalSizeMax)
    {
        if (!enabled) {
            return;
        }

        int safeFrequency = clamp(frequency, 1, 100);
        int safeRarity = clamp(rarity, 0, 100);
        int safeMinAltitude = clamp(minAltitude, TerraformGeneratorPlugin.injector.getMinY() + 5, chunkData.getMaxHeight() - LAKE_SIZE_Y);
        int safeMaxAltitude = clamp(maxAltitude, safeMinAltitude + 1, chunkData.getMaxHeight() - LAKE_SIZE_Y + 1);
        int safeMinEllipsoids = Math.max(1, minEllipsoids);
        int safeMaxEllipsoids = Math.max(safeMinEllipsoids, maxEllipsoids);
        double safeHorizontalSizeMin = Math.max(0.1D, horizontalSizeMin);
        double safeHorizontalSizeMax = Math.max(safeHorizontalSizeMin, horizontalSizeMax);
        double safeVerticalSizeMin = Math.max(0.1D, verticalSizeMin);
        double safeVerticalSizeMax = Math.max(safeVerticalSizeMin, verticalSizeMax);

        for (int attempt = 0; attempt < safeFrequency; attempt++) {
            if (sourceRandom.nextDouble() * 100.0D > safeRarity) {
                continue;
            }

            int x = (sourceChunkX << 4) + 8 + sourceRandom.nextInt(CHUNK_SIZE);
            int z = (sourceChunkZ << 4) + 8 + sourceRandom.nextInt(CHUNK_SIZE);
            spawnLake(sourceRandom,
                    tw,
                    generatingChunkX,
                    generatingChunkZ,
                    chunkData,
                    cache,
                    x,
                    z,
                    safeMinAltitude,
                    safeMaxAltitude,
                    material,
                    safeMinEllipsoids,
                    safeMaxEllipsoids,
                    safeHorizontalSizeMin,
                    safeHorizontalSizeMax,
                    safeVerticalSizeMin,
                    safeVerticalSizeMax);
        }
    }

    private void spawnLake(@NotNull Random random,
                           @NotNull TerraformWorld tw,
                           int generatingChunkX,
                           int generatingChunkZ,
                           @NotNull ChunkGenerator.ChunkData chunkData,
                           @NotNull ChunkCache cache,
                           int x,
                           int z,
                           int minAltitude,
                           int maxAltitude,
                           @NotNull Material material,
                           int minEllipsoids,
                           int maxEllipsoids,
                           double horizontalSizeMin,
                           double horizontalSizeMax,
                           double verticalSizeMin,
                           double verticalSizeMax)
    {
        x -= 8;
        z -= 8;

        int y = random.nextInt(maxAltitude - minAltitude) + minAltitude;
        while (y > 5 && isEmpty(tw, generatingChunkX, generatingChunkZ, chunkData, x, y, z)) {
            y--;
        }

        if (y <= 4) {
            return;
        }

        y -= 4;
        boolean[] lakeShape = new boolean[LAKE_SIZE_XZ * LAKE_SIZE_XZ * LAKE_SIZE_Y];

        int ellipsoidCount = random.nextInt(maxEllipsoids - minEllipsoids + 1) + minEllipsoids;
        for (int ellipsoid = 0; ellipsoid < ellipsoidCount; ellipsoid++) {
            double sizeX = randomRange(random, horizontalSizeMin, horizontalSizeMax);
            double sizeY = randomRange(random, verticalSizeMin, verticalSizeMax);
            double sizeZ = randomRange(random, horizontalSizeMin, horizontalSizeMax);
            double centerX = random.nextDouble() * (16.0D - sizeX - 2.0D) + 1.0D + sizeX / 2.0D;
            double centerY = random.nextDouble() * (8.0D - sizeY - 4.0D) + 2.0D + sizeY / 2.0D;
            double centerZ = random.nextDouble() * (16.0D - sizeZ - 2.0D) + 1.0D + sizeZ / 2.0D;

            for (int lakeX = 1; lakeX < 15; lakeX++) {
                for (int lakeZ = 1; lakeZ < 15; lakeZ++) {
                    for (int lakeY = 1; lakeY < 7; lakeY++) {
                        double normalizedX = (lakeX - centerX) / (sizeX / 2.0D);
                        double normalizedY = (lakeY - centerY) / (sizeY / 2.0D);
                        double normalizedZ = (lakeZ - centerZ) / (sizeZ / 2.0D);
                        if (normalizedX * normalizedX + normalizedY * normalizedY + normalizedZ * normalizedZ < 1.0D) {
                            lakeShape[index(lakeX, lakeZ, lakeY)] = true;
                        }
                    }
                }
            }
        }

        if (!canPlaceLake(lakeShape, tw, generatingChunkX, generatingChunkZ, chunkData, x, y, z, material)) {
            return;
        }

        carveLake(lakeShape, generatingChunkX, generatingChunkZ, chunkData, cache, x, y, z, material);
    }

    private boolean canPlaceLake(boolean @NotNull [] lakeShape,
                                 @NotNull TerraformWorld tw,
                                 int generatingChunkX,
                                 int generatingChunkZ,
                                 @NotNull ChunkGenerator.ChunkData chunkData,
                                 int x,
                                 int y,
                                 int z,
                                 @NotNull Material material)
    {
        for (int lakeX = 0; lakeX < LAKE_SIZE_XZ; lakeX++) {
            for (int lakeZ = 0; lakeZ < LAKE_SIZE_XZ; lakeZ++) {
                for (int lakeY = 0; lakeY < LAKE_SIZE_Y; lakeY++) {
                    if (lakeShape[index(lakeX, lakeZ, lakeY)] || !touchesLake(lakeShape, lakeX, lakeZ, lakeY)) {
                        continue;
                    }

                    Material existing = getMaterial(tw,
                            generatingChunkX,
                            generatingChunkZ,
                            chunkData,
                            x + lakeX,
                            y + lakeY,
                            z + lakeZ);
                    if (lakeY >= LAKE_LIQUID_HEIGHT && isLiquid(existing)) {
                        return false;
                    }
                    if (lakeY < LAKE_LIQUID_HEIGHT && !existing.isSolid() && existing != material) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private void carveLake(boolean @NotNull [] lakeShape,
                           int generatingChunkX,
                           int generatingChunkZ,
                           @NotNull ChunkGenerator.ChunkData chunkData,
                           @NotNull ChunkCache cache,
                           int x,
                           int y,
                           int z,
                           @NotNull Material material)
    {
        for (int lakeX = 0; lakeX < LAKE_SIZE_XZ; lakeX++) {
            for (int lakeZ = 0; lakeZ < LAKE_SIZE_XZ; lakeZ++) {
                for (int lakeY = 0; lakeY < LAKE_SIZE_Y; lakeY++) {
                    if (!lakeShape[index(lakeX, lakeZ, lakeY)]) {
                        continue;
                    }

                    int blockX = x + lakeX;
                    int blockY = y + lakeY;
                    int blockZ = z + lakeZ;
                    if (!isInGeneratingChunk(generatingChunkX, generatingChunkZ, blockX, blockZ)
                        || blockY < chunkData.getMinHeight()
                        || blockY >= chunkData.getMaxHeight())
                    {
                        continue;
                    }

                    int localX = blockX - (generatingChunkX << 4);
                    int localZ = blockZ - (generatingChunkZ << 4);
                    chunkData.setBlock(localX, blockY, localZ, lakeY < LAKE_LIQUID_HEIGHT ? material : Material.CAVE_AIR);
                    cache.cacheNonSolid(localX, blockY, localZ);
                }
            }
        }

        for (int lakeX = 0; lakeX < LAKE_SIZE_XZ; lakeX++) {
            for (int lakeZ = 0; lakeZ < LAKE_SIZE_XZ; lakeZ++) {
                int blockX = x + lakeX;
                int blockZ = z + lakeZ;
                if (isInGeneratingChunk(generatingChunkX, generatingChunkZ, blockX, blockZ)) {
                    updateHeightCache(chunkData, cache, blockX - (generatingChunkX << 4), blockZ - (generatingChunkZ << 4));
                }
            }
        }
    }

    private boolean touchesLake(boolean @NotNull [] lakeShape, int lakeX, int lakeZ, int lakeY) {
        return lakeX < 15 && lakeShape[index(lakeX + 1, lakeZ, lakeY)]
               || lakeX > 0 && lakeShape[index(lakeX - 1, lakeZ, lakeY)]
               || lakeZ < 15 && lakeShape[index(lakeX, lakeZ + 1, lakeY)]
               || lakeZ > 0 && lakeShape[index(lakeX, lakeZ - 1, lakeY)]
               || lakeY < 7 && lakeShape[index(lakeX, lakeZ, lakeY + 1)]
               || lakeY > 0 && lakeShape[index(lakeX, lakeZ, lakeY - 1)];
    }

    private boolean isEmpty(@NotNull TerraformWorld tw,
                            int generatingChunkX,
                            int generatingChunkZ,
                            @NotNull ChunkGenerator.ChunkData chunkData,
                            int blockX,
                            int blockY,
                            int blockZ)
    {
        return getMaterial(tw, generatingChunkX, generatingChunkZ, chunkData, blockX, blockY, blockZ).isAir();
    }

    private Material getMaterial(@NotNull TerraformWorld tw,
                                 int generatingChunkX,
                                 int generatingChunkZ,
                                 @NotNull ChunkGenerator.ChunkData chunkData,
                                 int blockX,
                                 int blockY,
                                 int blockZ)
    {
        if (blockY < chunkData.getMinHeight() || blockY >= chunkData.getMaxHeight()) {
            return Material.BEDROCK;
        }
        if (!isInGeneratingChunk(generatingChunkX, generatingChunkZ, blockX, blockZ)) {
            int terrainHeight = HeightMap.getBlockHeight(tw, blockX, blockZ);
            if (blockY <= terrainHeight) {
                return Material.STONE;
            }
            if (blockY <= TerraformGenerator.seaLevel) {
                return Material.WATER;
            }
            return Material.AIR;
        }
        return chunkData.getType(blockX - (generatingChunkX << 4), blockY, blockZ - (generatingChunkZ << 4));
    }

    private boolean isInGeneratingChunk(int generatingChunkX, int generatingChunkZ, int blockX, int blockZ) {
        return blockX >= (generatingChunkX << 4)
               && blockX < (generatingChunkX << 4) + CHUNK_SIZE
               && blockZ >= (generatingChunkZ << 4)
               && blockZ < (generatingChunkZ << 4) + CHUNK_SIZE;
    }

    private void updateHeightCache(@NotNull ChunkGenerator.ChunkData chunkData,
                                   @NotNull ChunkCache cache,
                                   int localX,
                                   int localZ)
    {
        short current = cache.getTransformedHeight(localX, localZ);
        if (current < chunkData.getMinHeight() || current >= chunkData.getMaxHeight()) {
            return;
        }
        if (chunkData.getType(localX, current, localZ).isSolid()) {
            return;
        }

        for (int y = current - 1; y >= chunkData.getMinHeight(); y--) {
            if (chunkData.getType(localX, y, localZ).isSolid()) {
                cache.writeTransformedHeight(localX, localZ, (short) y);
                return;
            }
        }
        cache.writeTransformedHeight(localX, localZ, (short) chunkData.getMinHeight());
    }

    private boolean isLiquid(@NotNull Material material) {
        return material == Material.WATER || material == Material.LAVA;
    }

    private double randomRange(@NotNull Random random, double min, double max) {
        return random.nextDouble() * (max - min) + min;
    }

    private int index(int lakeX, int lakeZ, int lakeY) {
        return (lakeX * LAKE_SIZE_XZ + lakeZ) * LAKE_SIZE_Y + lakeY;
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(value, max));
    }
}
