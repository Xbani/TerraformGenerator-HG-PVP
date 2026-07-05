package org.terraform.coregen;

import org.terraform.biome.BiomeBank;
import org.terraform.biome.BiomeSection;
import org.terraform.coregen.bukkit.TerraformGenerator;
import org.terraform.coregen.populatordata.PopulatorDataAbstract;
import org.terraform.data.CoordPair;
import org.terraform.data.TerraformWorld;
import org.terraform.main.config.TConfig;
import org.terraform.utils.GenUtils;
import org.terraform.utils.datastructs.ConcurrentLRUCache;
import org.terraform.utils.noise.FastNoise;
import org.terraform.utils.noise.FastNoise.NoiseType;
import org.terraform.utils.noise.NoiseCacheHandler;
import org.terraform.utils.noise.NoiseCacheHandler.NoiseCacheEntry;

import java.util.HashMap;

public enum HeightMap {
    /**
     * Current river depth, also returns negative values if on dry ground.
     */
    RIVER {
        @Override
        public double getHeight(TerraformWorld tw, int x, int z) {
            FastNoise noise = NoiseCacheHandler.getNoise(tw, NoiseCacheEntry.HEIGHTMAP_RIVER, world -> {
                FastNoise n = new FastNoise((int) world.getSeed());
                n.SetNoiseType(NoiseType.PerlinFractal);
                n.SetFrequency(TConfig.c.HEIGHT_MAP_RIVER_FREQUENCY);
                n.SetFractalOctaves(5);
                return n;
            });
            return 15 - 200 * Math.abs(noise.GetNoise(x, z));
        }
    }, CORE {
        @Override
        public double getHeight(TerraformWorld tw, int x, int z) {
            FastNoise noise = NoiseCacheHandler.getNoise(tw, NoiseCacheEntry.HEIGHTMAP_CORE, world -> {
                FastNoise n = new FastNoise((int) world.getSeed());
                n.SetNoiseType(NoiseType.SimplexFractal);
                n.SetFractalOctaves(2);
                n.SetFrequency(TConfig.c.HEIGHT_MAP_CORE_FREQUENCY);
                return n;
            });

            double height = 10 * noise.GetNoise(x, z) + 7 + TerraformGenerator.seaLevel;

            if (height > TerraformGenerator.seaLevel + 10) {
                height = (height - TerraformGenerator.seaLevel - 10) * 0.1 + TerraformGenerator.seaLevel + 10;
            }

            if (TConfig.c.HEIGHT_MAP_FLATTEN_CUBOID_ENABLED) {
                height = forceFlatEdges(
                        x, z,
                        height,
                        TConfig.c.HEIGHT_MAP_FLATTEN_CUBOID_MIN_X,
                        TConfig.c.HEIGHT_MAP_FLATTEN_CUBOID_MIN_Z,
                        TConfig.c.HEIGHT_MAP_FLATTEN_CUBOID_MAX_X,
                        TConfig.c.HEIGHT_MAP_FLATTEN_CUBOID_MAX_Z,
                        TConfig.c.HEIGHT_MAP_FLATTEN_CUBOID_Y,
                        TConfig.c.HEIGHT_MAP_FLATTEN_CUBOID_FADE_DISTANCE
                );
            }

            return height;
        }

        private static double forceFlatEdges(
                int x, int z,
                double height,
                int minX, int minZ, int maxX, int maxZ,
                int flatY,
                int fadeDistance
        ) {
            if (x < minX || x > maxX || z < minZ || z > maxZ) return height;

            if (fadeDistance <= 0) return flatY;

            int dx = Math.min(Math.abs(x - minX), Math.abs(x - maxX));
            int dz = Math.min(Math.abs(z - minZ), Math.abs(z - maxZ));
            int d = Math.min(dx, dz);

            if (d >= fadeDistance) return height;

            double t = (double) d / (double) fadeDistance; // 0=edge -> 1=fadeDistance
            return flatY + (height - flatY) * t;
        }
    }, ATTRITION {
        @Override
        public double getHeight(TerraformWorld tw, int x, int z) {
            FastNoise perlin = NoiseCacheHandler.getNoise(tw, NoiseCacheEntry.HEIGHTMAP_ATTRITION, world -> {
                FastNoise n = new FastNoise((int) world.getSeed() + 113);
                n.SetNoiseType(NoiseType.PerlinFractal);
                n.SetFractalOctaves(4);
                n.SetFrequency(0.02f);
                return n;
            });

            double height = perlin.GetNoise(x, z) * 2 * 7;
            return Math.max(0, height);
        }
    };

    public static final int defaultSeaLevel = 62;
    public static final float heightAmplifier = TConfig.c.HEIGHT_MAP_LAND_HEIGHT_AMPLIFIER;
    public static final int MASK_RADIUS = 5;
    public static final int MASK_DIAMETER = (MASK_RADIUS * 2) + 1;
    public static final int MASK_VOLUME = MASK_DIAMETER*MASK_DIAMETER;
    private static final int upscaleSize = 3;
    public static int spawnFlatRadiusSquared = -324534;
    private static SpawnMode spawnMode = SpawnMode.SIMPLE;
    private static int spawnSimpleRadiusSquared = -1;
    private static int spawnAdvancedRadius = 32;
    private static int spawnAdvancedRadiusSquared = 32 * 32;
    private static int spawnAdvancedCenterY = 68;
    private static int spawnAdvancedEdgeY = 72;
    private static int spawnAdvancedBlendDistance = 16;
    private static int spawnAdvancedOuterRadius = 48;
    private static int spawnAdvancedOuterRadiusSquared = 48 * 48;
    private static final ConcurrentLRUCache<BiomeSection, SectionBlurCache> BLUR_CACHE = new ConcurrentLRUCache<>(
        "BLUR_CACHE",64, (sect)->{
            SectionBlurCache newCache = new SectionBlurCache(
                    sect,
                    new float[BiomeSection.sectionWidth+MASK_DIAMETER][BiomeSection.sectionWidth+MASK_DIAMETER],
                    new float[BiomeSection.sectionWidth+MASK_DIAMETER][BiomeSection.sectionWidth+MASK_DIAMETER]);
            newCache.fillCache();
            return newCache;
        }
    );

    /**
     * Returns the average increase or decrease in height for surrounding blocks compared to the provided height at those coords.
     * 1.5 for a radius of 3 is considered steep.
     * Does noise calculations to find the true core height
     */
    public static double getNoiseGradient(TerraformWorld tw, int x, int z, int radius) {
        double totalChangeInGradient = 0;
        int count = 0;
        double centerNoise = getBlockHeight(tw, x, z);
        for (int nx = -radius; nx <= radius; nx++) {
            for (int nz = -radius; nz <= radius; nz++) {
                if (nx == 0 && nz == 0) {
                    continue;
                }
                // Bukkit.getLogger().info(nx + "," + nz + ":"+(getHeight(tw,x+nx,z+nz)-centerNoise));
                totalChangeInGradient += Math.abs(getBlockHeight(tw, x + nx, z + nz) - centerNoise);
                count++;
            }
        }

        return totalChangeInGradient / count;
    }

    /**
     * Returns the average increase or decrease in height for surrounding blocks compared to the provided height at those coords.
     * 1.5 for a radius of 3 is considered steep.
     * Does GenUtils.getHighestGround to get height values.
     */
    /*TODO: There are several calls to this in Biome Handlers.
     * Write a version that uses transformed height.
     * 10/4/2025: Is this not already transformed height???
     */
    public static double getTrueHeightGradient(PopulatorDataAbstract data, int x, int z, int radius) {
        double totalChangeInGradient = 0;
        int count = 0;
        double centerNoise = GenUtils.getHighestGround(data, x, z); // getBlockHeight(tw, x, z);
        for (int nx = -radius; nx <= radius; nx++) {
            for (int nz = -radius; nz <= radius; nz++) {
                if (nx == 0 && nz == 0) {
                    continue;
                }
                // Bukkit.getLogger().info(nx + "," + nz + ":"+(getHeight(tw,x+nx,z+nz)-centerNoise));
                totalChangeInGradient += Math.abs(GenUtils.getHighestGround(data, x + nx, z + nz) - centerNoise);
                count++;
            }
        }

        return totalChangeInGradient / count;
    }

    public static double getRawRiverDepth(TerraformWorld tw, int x, int z) {
        if (isInsideSpawnNoRiverArea(x, z)) {
            return 0;
        }
        double depth = HeightMap.RIVER.getHeight(tw, x, z);
        return Math.max(0, depth);
    }

    public static double getPreciseHeight(TerraformWorld tw, int x, int z) {
        ChunkCache cache = TerraformGenerator.getCache(tw, x>>4, z>>4);

        double cachedValue = cache.getHeightMapHeight(x, z);
        if (cachedValue != ChunkCache.CHUNKCACHE_INVAL) {
            return cachedValue;
        }

        double height = getRiverlessHeight(tw, x, z);
        height = applySpawnAreaHeight(x, z, height);
        height = getFlatSwampHeight(tw, x, z, height);

        // River Depth
        double depth = getEffectiveRiverDepth(tw, x, z);

        // Normal scenario: Shallow area
        if (height - depth >= TerraformGenerator.seaLevel - 15) {
            height -= depth;

            // Fix for underwater river carving: Don't carve deeply
        }
        else if (height > TerraformGenerator.seaLevel - 15 && height - depth < TerraformGenerator.seaLevel - 15) {
            height = TerraformGenerator.seaLevel - 15;
        }

        if (heightAmplifier != 1f && height > TerraformGenerator.seaLevel) {
            height += heightAmplifier * (height - TerraformGenerator.seaLevel);
        }

        cache.cacheHeightMap(x, z, height);
        return height;
    }

    /**
     * Do not fucking call this anywhere outside the SectionBlurCache.
     * This method used to cause cache thrashing as it can leave the chunk
     * boundaries. This was fixed by just caching this with a stack
     * variable, as it happened to be called in one place.
     * <br><br>
     * Do not call this anywhere else.
     * @param x raw x coordinate
     * @param z raw z coordinate
     * @param dominantBiomeHeights This is a cache value for local caching.
     * @return The dominant biome's height calculation. Must be blurred to be coherent with other biomes.
     */
    static float getDominantBiomeHeight(TerraformWorld tw, int x, int z, HashMap<CoordPair, Float> dominantBiomeHeights) {
        CoordPair key = new CoordPair(x,z);
        Float h = dominantBiomeHeights.get(key);
        if (h == null) {
            // Upscale the biome
            // This comes from computing each biome height one time per upscaleSize blocks
            if (x % upscaleSize != 0 && z % upscaleSize != 0) {
                h = getDominantBiomeHeight(tw, x - (x % upscaleSize), z - (z % upscaleSize), dominantBiomeHeights);
            }
            else {
                h = (float) BiomeBank.calculateHeightIndependentBiome(tw, x, z).getHandler().calculateHeight(tw, x, z);
                if (spawnMode == SpawnMode.SIMPLE && isInsideRadiusSquared(x, z, spawnSimpleRadiusSquared)) {
                    h = (float) HeightMap.CORE.getHeight(tw, x, z);
                }
            }
            dominantBiomeHeights.put(key, h);
        }
        return h;
    }

    /**
     * Biome calculations are done here as well.
     * <br>
     * This function is responsible for applying blurring to merge biomes together
     *
     * @return Near-final world height without rivers accounted for
     */
    public static double getRiverlessHeight(TerraformWorld tw, int x, int z) {

        // int maskDiameterSquared = maskDiameter*maskDiameter;
        double coreHeight;

        BiomeSection sect = BiomeBank.getBiomeSectionFromBlockCoords(tw, x, z);

        // This will calculate a blur height
        coreHeight = BLUR_CACHE.get(sect).getBlurredHeight(x,z);

        coreHeight += HeightMap.ATTRITION.getHeight(tw, x, z);

        return coreHeight;
    }

    public static int getBlockHeight(TerraformWorld tw, int x, int z) {
        return (int) getPreciseHeight(tw, x, z);
    }

    public abstract double getHeight(TerraformWorld tw, int x, int z);

    public static void initSpawnArea() {
        spawnMode = SpawnMode.fromConfig(TConfig.c.HEIGHT_MAP_SPAWN_MODE);

        int simpleRadius = Math.max(-1, TConfig.c.HEIGHT_MAP_SPAWN_FLAT_RADIUS);
        spawnSimpleRadiusSquared = squareIfPositive(simpleRadius);

        spawnAdvancedRadius = Math.max(0, TConfig.c.HEIGHT_MAP_SPAWN_ADVANCED_RADIUS);
        spawnAdvancedRadiusSquared = squareIfPositive(spawnAdvancedRadius);
        spawnAdvancedCenterY = TConfig.c.HEIGHT_MAP_SPAWN_ADVANCED_CENTER_Y;
        spawnAdvancedEdgeY = TConfig.c.HEIGHT_MAP_SPAWN_ADVANCED_EDGE_Y;
        spawnAdvancedBlendDistance = Math.max(0, TConfig.c.HEIGHT_MAP_SPAWN_ADVANCED_BLEND_DISTANCE);
        spawnAdvancedOuterRadius = spawnAdvancedRadius + spawnAdvancedBlendDistance;
        spawnAdvancedOuterRadiusSquared = squareIfPositive(spawnAdvancedOuterRadius);

        if (spawnMode == SpawnMode.SIMPLE) {
            spawnFlatRadiusSquared = spawnSimpleRadiusSquared;
        }
        else if (spawnMode == SpawnMode.ADVANCED) {
            spawnFlatRadiusSquared = spawnAdvancedOuterRadiusSquared;
        }
        else {
            spawnFlatRadiusSquared = -1;
        }
    }

    private static double getEffectiveRiverDepth(TerraformWorld tw, int x, int z) {
        double depth = getRawRiverDepth(tw, x, z);
        if (!TConfig.c.HEIGHT_MAP_FLAT_RIVER_ENABLED) {
            return depth;
        }
        return Math.min(depth, Math.max(0, TConfig.c.HEIGHT_MAP_FLAT_RIVER_MAX_DEPTH));
    }

    private static double getFlatSwampHeight(TerraformWorld tw, int x, int z, double height) {
        if (!TConfig.c.HEIGHT_MAP_FLAT_SWAMP_ENABLED) {
            return height;
        }

        BiomeBank bank = BiomeBank.calculateHeightIndependentBiome(tw, x, z);
        if (bank != BiomeBank.SWAMP && bank != BiomeBank.MANGROVE) {
            return height;
        }

        return Math.min(height, TerraformGenerator.seaLevel - Math.max(0, TConfig.c.HEIGHT_MAP_FLAT_SWAMP_MIN_WATER_DEPTH));
    }

    private static double applySpawnAreaHeight(int x, int z, double baseHeight) {
        if (spawnMode != SpawnMode.ADVANCED || spawnAdvancedRadius <= 0) {
            return baseHeight;
        }

        double distance = Math.sqrt((double) x * (double) x + (double) z * (double) z);
        if (distance <= spawnAdvancedRadius) {
            double t = smoothStep(distance / (double) spawnAdvancedRadius);
            return lerp(spawnAdvancedCenterY, spawnAdvancedEdgeY, t);
        }

        if (spawnAdvancedBlendDistance <= 0 || distance >= spawnAdvancedOuterRadius) {
            return baseHeight;
        }

        double t = smoothStep((distance - spawnAdvancedRadius) / (double) spawnAdvancedBlendDistance);
        return lerp(spawnAdvancedEdgeY, baseHeight, t);
    }

    private static boolean isInsideSpawnNoRiverArea(int x, int z) {
        return switch (spawnMode) {
            case SIMPLE -> isInsideRadiusSquared(x, z, spawnSimpleRadiusSquared);
            case ADVANCED -> isInsideRadiusSquared(x, z, spawnAdvancedOuterRadiusSquared);
            case NONE -> false;
        };
    }

    private static int squareIfPositive(int radius) {
        return radius > 0 ? radius * radius : -1;
    }

    private static boolean isInsideRadiusSquared(int x, int z, int radiusSquared) {
        return radiusSquared > 0 && ((double) x * (double) x + (double) z * (double) z) < radiusSquared;
    }

    private static double smoothStep(double value) {
        double t = Math.max(0, Math.min(1, value));
        return t * t * (3 - 2 * t);
    }

    private static double lerp(double from, double to, double t) {
        return from + (to - from) * t;
    }

    private enum SpawnMode {
        NONE,
        SIMPLE,
        ADVANCED;

        private static SpawnMode fromConfig(String value) {
            if (value == null) {
                return SIMPLE;
            }

            for (SpawnMode mode : values()) {
                if (mode.name().equalsIgnoreCase(value)) {
                    return mode;
                }
            }
            return SIMPLE;
        }
    }
}
