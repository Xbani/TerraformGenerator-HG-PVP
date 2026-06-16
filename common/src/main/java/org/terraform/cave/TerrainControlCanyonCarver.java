package org.terraform.cave;

import org.bukkit.Material;
import org.bukkit.generator.ChunkGenerator;
import org.jetbrains.annotations.NotNull;
import org.terraform.coregen.ChunkCache;
import org.terraform.data.TerraformWorld;
import org.terraform.main.TerraformGeneratorPlugin;
import org.terraform.main.config.TConfig;
import org.terraform.utils.BlockUtils;

import java.util.Random;

public class TerrainControlCanyonCarver {
    private static final int CHUNK_SIZE = 16;
    private static final int CHECK_AREA_SIZE = 8;
    private static final float PI = 3.141593F;

    private final float[] heightFactors = new float[1024];

    public void carve(@NotNull TerraformWorld tw,
                      int chunkX,
                      int chunkZ,
                      @NotNull ChunkGenerator.ChunkData chunkData,
                      @NotNull ChunkCache cache)
    {
        if (!TConfig.areCavesEnabled() || !TConfig.c.CAVES_TERRAIN_CONTROL_CANYONS_ENABLED) {
            return;
        }

        Random seedRandom = new Random(tw.getSeed());
        long worldLong1 = seedRandom.nextLong();
        long worldLong2 = seedRandom.nextLong();

        for (int sourceChunkX = chunkX - CHECK_AREA_SIZE; sourceChunkX <= chunkX + CHECK_AREA_SIZE; sourceChunkX++) {
            for (int sourceChunkZ = chunkZ - CHECK_AREA_SIZE; sourceChunkZ <= chunkZ + CHECK_AREA_SIZE; sourceChunkZ++) {
                long sourceSeedX = sourceChunkX * worldLong1;
                long sourceSeedZ = sourceChunkZ * worldLong2;
                Random sourceRandom = new Random(sourceSeedX ^ sourceSeedZ ^ tw.getSeed());
                generateSourceChunk(sourceRandom, sourceChunkX, sourceChunkZ, chunkX, chunkZ, chunkData, cache);
            }
        }
    }

    private void generateSourceChunk(@NotNull Random random,
                                     int sourceChunkX,
                                     int sourceChunkZ,
                                     int generatingChunkX,
                                     int generatingChunkZ,
                                     @NotNull ChunkGenerator.ChunkData chunkData,
                                     @NotNull ChunkCache cache)
    {
        if (random.nextInt(100) >= clamp(TConfig.c.CAVES_TERRAIN_CONTROL_CANYONS_RARITY, 0, 100)) {
            return;
        }

        int minAltitude = clamp(TConfig.c.CAVES_TERRAIN_CONTROL_CANYONS_MIN_ALTITUDE,
                TerraformGeneratorPlugin.injector.getMinY() + 1,
                chunkData.getMaxHeight() - 8);
        int maxAltitude = clamp(TConfig.c.CAVES_TERRAIN_CONTROL_CANYONS_MAX_ALTITUDE,
                minAltitude + 1,
                chunkData.getMaxHeight() - 7);
        int minLength = Math.max(1, TConfig.c.CAVES_TERRAIN_CONTROL_CANYONS_MIN_LENGTH);
        int maxLength = Math.max(minLength + 1, TConfig.c.CAVES_TERRAIN_CONTROL_CANYONS_MAX_LENGTH);
        int canyonCount = Math.max(1, TConfig.c.CAVES_TERRAIN_CONTROL_CANYONS_LEVELS);

        double x = (sourceChunkX << 4) + random.nextInt(CHUNK_SIZE);
        double y = random.nextInt(maxAltitude - minAltitude) + minAltitude;
        double z = (sourceChunkZ << 4) + random.nextInt(CHUNK_SIZE);

        for (int i = 0; i < canyonCount; i++) {
            float yaw = random.nextFloat() * PI * 2.0F;
            float pitch = (random.nextFloat() - 0.5F) * 2.0F / 8.0F;
            float width = (random.nextFloat() * 2.0F + random.nextFloat()) * 2.0F;
            int length = random.nextInt(maxLength - minLength) + minLength;

            carvePath(random.nextLong(),
                    generatingChunkX,
                    generatingChunkZ,
                    chunkData,
                    cache,
                    x,
                    y,
                    z,
                    width,
                    yaw,
                    pitch,
                    length,
                    TConfig.c.CAVES_TERRAIN_CONTROL_CANYONS_DEPTH);
        }
    }

    private void carvePath(long seed,
                           int generatingChunkX,
                           int generatingChunkZ,
                           @NotNull ChunkGenerator.ChunkData chunkData,
                           @NotNull ChunkCache cache,
                           double x,
                           double y,
                           double z,
                           float width,
                           float yaw,
                           float pitch,
                           int length,
                           double depth)
    {
        Random random = new Random(seed);
        double chunkCenterX = (generatingChunkX << 4) + 8;
        double chunkCenterZ = (generatingChunkZ << 4) + 8;
        float yawVelocity = 0.0F;
        float pitchVelocity = 0.0F;

        float heightFactor = 1.0F;
        for (int i = 0; i < chunkData.getMaxHeight() && i < heightFactors.length; i++) {
            if (i == 0 || random.nextInt(3) == 0) {
                heightFactor = 1.0F + random.nextFloat() * random.nextFloat();
            }
            heightFactors[i] = heightFactor * heightFactor;
        }

        for (int step = 0; step < length; step++) {
            double horizontalRadius = 1.5D + Math.sin(step * PI / length) * width;
            double verticalRadius = horizontalRadius * depth;

            horizontalRadius *= random.nextFloat() * 0.25D + 0.75D;
            verticalRadius *= random.nextFloat() * 0.25D + 0.75D;

            float pitchCos = (float) Math.cos(pitch);
            float pitchSin = (float) Math.sin(pitch);
            x += Math.cos(yaw) * pitchCos;
            y += pitchSin;
            z += Math.sin(yaw) * pitchCos;

            pitch *= 0.7F;
            pitch += pitchVelocity * 0.05F;
            yaw += yawVelocity * 0.05F;

            pitchVelocity *= 0.8F;
            yawVelocity *= 0.5F;
            pitchVelocity += (random.nextFloat() - random.nextFloat()) * random.nextFloat() * 2.0F;
            yawVelocity += (random.nextFloat() - random.nextFloat()) * random.nextFloat() * 4.0F;

            if (random.nextInt(4) == 0) {
                continue;
            }

            double distanceX = x - chunkCenterX;
            double distanceZ = z - chunkCenterZ;
            double remainingSteps = length - step;
            double maxReach = width + 18.0F;
            if (distanceX * distanceX + distanceZ * distanceZ - remainingSteps * remainingSteps > maxReach * maxReach) {
                return;
            }

            if (x < chunkCenterX - 16.0D - horizontalRadius * 2.0D
                || z < chunkCenterZ - 16.0D - horizontalRadius * 2.0D
                || x > chunkCenterX + 16.0D + horizontalRadius * 2.0D
                || z > chunkCenterZ + 16.0D + horizontalRadius * 2.0D)
            {
                continue;
            }

            carveEllipsoid(generatingChunkX, generatingChunkZ, chunkData, cache, x, y, z, horizontalRadius, verticalRadius);
        }
    }

    private void carveEllipsoid(int generatingChunkX,
                                int generatingChunkZ,
                                @NotNull ChunkGenerator.ChunkData chunkData,
                                @NotNull ChunkCache cache,
                                double x,
                                double y,
                                double z,
                                double horizontalRadius,
                                double verticalRadius)
    {
        int chunkBlockX = generatingChunkX << 4;
        int chunkBlockZ = generatingChunkZ << 4;
        int minLocalX = clamp(floor(x - horizontalRadius) - chunkBlockX - 1, 0, CHUNK_SIZE);
        int maxLocalX = clamp(floor(x + horizontalRadius) - chunkBlockX + 1, 0, CHUNK_SIZE);
        int minY = clamp(floor(y - verticalRadius) - 1, chunkData.getMinHeight() + 1, chunkData.getMaxHeight() - 8);
        int maxY = clamp(floor(y + verticalRadius) + 1, chunkData.getMinHeight() + 1, chunkData.getMaxHeight() - 8);
        int minLocalZ = clamp(floor(z - horizontalRadius) - chunkBlockZ - 1, 0, CHUNK_SIZE);
        int maxLocalZ = clamp(floor(z + horizontalRadius) - chunkBlockZ + 1, 0, CHUNK_SIZE);

        if (!TConfig.c.CAVES_TERRAIN_CONTROL_CANYONS_ALLOW_FLOODED && containsWater(chunkData,
                minLocalX,
                maxLocalX,
                minLocalZ,
                maxLocalZ,
                minY,
                maxY))
        {
            return;
        }

        for (int localX = minLocalX; localX < maxLocalX; localX++) {
            double normalizedX = (localX + chunkBlockX + 0.5D - x) / horizontalRadius;
            for (int localZ = minLocalZ; localZ < maxLocalZ; localZ++) {
                double normalizedZ = (localZ + chunkBlockZ + 0.5D - z) / horizontalRadius;
                boolean grassFound = false;
                if (normalizedX * normalizedX + normalizedZ * normalizedZ >= 1.0D) {
                    continue;
                }

                for (int localY = maxY; localY >= minY; localY--) {
                    double normalizedY = ((localY - 1) + 0.5D - y) / verticalRadius;
                    int heightFactorIndex = Math.max(0, Math.min(localY - 1, heightFactors.length - 1));
                    if ((normalizedX * normalizedX + normalizedZ * normalizedZ) * heightFactors[heightFactorIndex]
                        + normalizedY * normalizedY / 6.0D >= 1.0D)
                    {
                        continue;
                    }

                    Material material = chunkData.getType(localX, localY, localZ);
                    if (material == Material.GRASS_BLOCK) {
                        grassFound = true;
                    }
                    if (!canReplace(material)) {
                        continue;
                    }

                    if (localY - 1 < TConfig.c.CAVES_TERRAIN_CONTROL_CANYONS_LAVA_LEVEL) {
                        chunkData.setBlock(localX, localY, localZ, Material.LAVA);
                        cache.cacheNonSolid(localX, localY, localZ);
                    }
                    else {
                        chunkData.setBlock(localX, localY, localZ, Material.CAVE_AIR);
                        cache.cacheNonSolid(localX, localY, localZ);
                        if (grassFound && chunkData.getType(localX, localY - 1, localZ) == Material.DIRT) {
                            chunkData.setBlock(localX, localY - 1, localZ, Material.GRASS_BLOCK);
                        }
                    }
                }
                updateHeightCache(chunkData, cache, localX, localZ);
            }
        }
    }

    private boolean containsWater(@NotNull ChunkGenerator.ChunkData chunkData,
                                  int minLocalX,
                                  int maxLocalX,
                                  int minLocalZ,
                                  int maxLocalZ,
                                  int minY,
                                  int maxY)
    {
        for (int localX = minLocalX; localX < maxLocalX; localX++) {
            for (int localZ = minLocalZ; localZ < maxLocalZ; localZ++) {
                for (int localY = maxY + 1; localY >= minY - 1; localY--) {
                    if (localY < chunkData.getMinHeight() || localY >= chunkData.getMaxHeight()) {
                        continue;
                    }
                    Material material = chunkData.getType(localX, localY, localZ);
                    if (material == Material.WATER) {
                        return true;
                    }
                    if (localY != minY - 1
                        && localX != minLocalX
                        && localX != maxLocalX - 1
                        && localZ != minLocalZ
                        && localZ != maxLocalZ - 1)
                    {
                        localY = minY;
                    }
                }
            }
        }
        return false;
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

    private boolean canReplace(@NotNull Material material) {
        return BlockUtils.isStoneLike(material);
    }

    private int floor(double value) {
        int integer = (int) value;
        return value < integer ? integer - 1 : integer;
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(value, max));
    }
}
