package org.terraform.coregen.floating;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.WorldInfo;
import org.jetbrains.annotations.NotNull;
import org.terraform.biome.BiomeBank;
import org.terraform.coregen.HeightMap;
import org.terraform.coregen.bukkit.TerraformGenerator;
import org.terraform.main.config.TConfig;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** Uses the native Bukkit pipeline, so normal terrain transforms cannot fill the void. */
public final class FloatingIslandGenerator extends TerraformGenerator {
    private static final Map<String, FloatingIslandGenerator> WORLDS = new ConcurrentHashMap<>();
    private final FloatingIslandSettings settings;
    private final int[] spawn;
    private final Map<Long, Map<Integer, BiomeBank>> palettes = new ConcurrentHashMap<>();

    public FloatingIslandGenerator(String worldName, FloatingIslandSettings settings) {
        this.settings = settings;
        spawn = settings.texture.nearestLand(HeightMap.spawnCenterX, HeightMap.spawnCenterZ);
        // An enabled spawn shape must fit on the supplied mask. Never silently fill black pixels.
        int radius = "advanced".equalsIgnoreCase(TConfig.c.HEIGHT_MAP_SPAWN_MODE)
                ? Math.max(0, TConfig.c.HEIGHT_MAP_SPAWN_ADVANCED_RADIUS)
                : "simple".equalsIgnoreCase(TConfig.c.HEIGHT_MAP_SPAWN_MODE)
                  ? Math.max(0, TConfig.c.HEIGHT_MAP_SPAWN_FLAT_RADIUS) : 0;
        if ("advanced".equalsIgnoreCase(TConfig.c.HEIGHT_MAP_SPAWN_MODE)
                && radius > 0 && settings.maxThickness < 6) {
            throw new IllegalArgumentException("Advanced spawn needs max-thickness >= 6");
        }
        if (radius > 4096) throw new IllegalArgumentException("Spawn radius exceeds texture limit");
        if (radius > 0) {
            for (int dx = -radius; dx <= radius; dx++) for (int dz = -radius; dz <= radius; dz++) {
                if ((long) dx * dx + (long) dz * dz > (long) radius * radius) continue;
                if (settings.texture.color(HeightMap.spawnCenterX + dx, HeightMap.spawnCenterZ + dz) == 0) {
                    throw new IllegalArgumentException("Spawn core intersects void; paint land over the spawn disc or reduce its radius");
                }
            }
        }
        WORLDS.put(worldName, this);
    }

    public static FloatingIslandGenerator forWorld(String worldName) { return WORLDS.get(worldName); }
    public static void clearWorlds() { WORLDS.clear(); }

    public BiomeBank biome(long seed, int x, int z) {
        return palettes.computeIfAbsent(seed, settings::paletteFor).get(settings.texture.color(x, z));
    }

    public int surface(long seed, int x, int z, int minY) {
        if (settings.texture.color(x, z) == 0) return minY - 1;
        double base = settings.texture.surface(seed, x, z, settings.altitude, settings.relief,
                settings.bevelWidth, settings.bevelDepth);
        return (int) Math.floor(HeightMap.applyFloatingSpawnHeight(x, z, base, settings.altitude));
    }

    public int thickness(int x, int z) {
        int depth = settings.texture.thickness(x, z, settings.minThickness, settings.maxThickness, settings.bevelWidth);
        if (HeightMap.isInsideSpawnCoreArea(x, z)) depth = Math.max(depth, HeightMap.getSpawnCoreRequiredDepth(x, z));
        return depth;
    }

    @Override
    public void generateNoise(@NotNull WorldInfo info, @NotNull Random ignored, int cx, int cz,
                              @NotNull ChunkData data) {
        validateWorldHeight(info);
        for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
            int wx = (cx << 4) + x, wz = (cz << 4) + z;
            BiomeBank bank = biome(info.getSeed(), wx, wz);
            if (bank == null) continue;
            int top = surface(info.getSeed(), wx, wz, info.getMinHeight());
            int bottom = top - thickness(wx, wz) + 1;
            data.setRegion(x, bottom, z, x + 1, top + 1, z + 1, Material.STONE);
            Material[] crust = bank.getHandler().getSurfaceCrust(columnRandom(info.getSeed(), wx, wz));
            // Keep a supporting stone bottom under gravity-affected crusts.
            for (int i = 0; i < crust.length && top - i > bottom; i++) {
                data.setBlock(x, top - i, z, crust[i].isSolid() ? crust[i] : Material.STONE);
            }
        }
    }

    @Override
    public void generateSurface(@NotNull WorldInfo info, @NotNull Random ignored, int cx, int cz,
                                @NotNull ChunkData data) {
        decorate(info, cx, cz, data);
    }

    @Override
    public int getBaseHeight(@NotNull WorldInfo info, @NotNull Random random, int x, int z,
                             @NotNull org.bukkit.HeightMap heightMap) {
        return surface(info.getSeed(), x, z, info.getMinHeight()) + 1;
    }

    private void validateWorldHeight(WorldInfo info) {
        double low = settings.altitude - settings.relief - settings.bevelDepth;
        double high = settings.altitude + settings.relief;
        if ("advanced".equalsIgnoreCase(TConfig.c.HEIGHT_MAP_SPAWN_MODE)
                && TConfig.c.HEIGHT_MAP_SPAWN_ADVANCED_RADIUS > 0) {
            low = Math.min(low, Math.min(TConfig.c.HEIGHT_MAP_SPAWN_ADVANCED_CENTER_Y, TConfig.c.HEIGHT_MAP_SPAWN_ADVANCED_EDGE_Y));
            high = Math.max(high, Math.max(TConfig.c.HEIGHT_MAP_SPAWN_ADVANCED_CENTER_Y, TConfig.c.HEIGHT_MAP_SPAWN_ADVANCED_EDGE_Y));
        }
        if (low - Math.max(settings.maxThickness, 6) < info.getMinHeight() || high + 8 >= info.getMaxHeight()) {
            throw new IllegalArgumentException("Floating island altitude/depth or spawn Y does not fit world height");
        }
    }

    private static Random columnRandom(long seed, int x, int z) {
        return new Random(seed ^ x * 0x9E3779B97F4A7C15L ^ z * 0xC2B2AE3D27D4EB4FL);
    }

    private void decorate(WorldInfo info, int cx, int cz, ChunkData data) {
        // Four independent tree sites per chunk; crowns fit entirely in their 8x8 cells.
        for (int x = 4; x < 16; x += 8) for (int z = 4; z < 16; z += 8) {
            int wx = (cx << 4) + x, wz = (cz << 4) + z;
            Random random = columnRandom(info.getSeed() ^ 918273L, wx, wz);
            if (random.nextDouble() >= settings.treeChance
                    || Math.abs((long) wx - spawn[0]) <= 4 && Math.abs((long) wz - spawn[1]) <= 4
                    || HeightMap.isInsideSpawnCoreArea(wx, wz)
                    || settings.texture.edgeDistance(wx, wz) < 4) continue;
            BiomeBank bank = biome(info.getSeed(), wx, wz);
            if (bank == null || !supportsTrees(bank)) continue;
            int top = surface(info.getSeed(), wx, wz, info.getMinHeight());
            if (data.getType(x, top, z) != Material.GRASS_BLOCK) continue;
            int trunkHeight = 4 + random.nextInt(2);
            Material log = bank.name().contains("TAIGA") ? Material.SPRUCE_LOG
                    : bank.name().contains("BIRCH") ? Material.BIRCH_LOG
                    : bank.name().contains("JUNGLE") ? Material.JUNGLE_LOG : Material.OAK_LOG;
            Material leaves = log == Material.SPRUCE_LOG ? Material.SPRUCE_LEAVES
                    : log == Material.BIRCH_LOG ? Material.BIRCH_LEAVES
                    : log == Material.JUNGLE_LOG ? Material.JUNGLE_LEAVES : Material.OAK_LEAVES;
            for (int dy = trunkHeight - 2; dy <= trunkHeight + 1; dy++) {
                int r = dy > trunkHeight ? 1 : 2;
                for (int dx = -r; dx <= r; dx++) for (int dz = -r; dz <= r; dz++) {
                    if (Math.abs(dx) == r && Math.abs(dz) == r) continue;
                    // No generated vegetation is allowed over a void column.
                    if (settings.texture.color(wx + dx, wz + dz) != 0
                            && data.getType(x + dx, top + dy, z + dz) == Material.AIR) {
                        var leafData = (org.bukkit.block.data.type.Leaves) leaves.createBlockData();
                        leafData.setPersistent(true);
                        data.setBlock(x + dx, top + dy, z + dz, leafData);
                    }
                }
            }
            for (int dy = 1; dy <= trunkHeight; dy++) data.setBlock(x, top + dy, z, log);
        }
    }

    private static boolean supportsTrees(BiomeBank bank) {
        String name = bank.name();
        return name.contains("FOREST") || name.contains("TAIGA") || name.contains("JUNGLE")
                || name.equals("PLAINS") || name.equals("CHERRY_GROVE");
    }

    @Override
    public BiomeProvider getDefaultBiomeProvider(@NotNull WorldInfo info) {
        List<Biome> biomes = new ArrayList<>();
        biomes.add(Biome.THE_VOID);
        for (BiomeBank bank : settings.paletteFor(info.getSeed()).values()) {
            Biome biome = bank.getHandler().getBiome();
            if (!biomes.contains(biome)) biomes.add(biome);
        }
        List<Biome> available = List.copyOf(biomes);
        return new BiomeProvider() {
            @Override public @NotNull Biome getBiome(@NotNull WorldInfo world, int x, int y, int z) {
                BiomeBank bank = biome(world.getSeed(), x, z);
                return bank == null ? Biome.THE_VOID : bank.getHandler().getBiome();
            }
            @Override public @NotNull List<Biome> getBiomes(@NotNull WorldInfo world) { return available; }
        };
    }

    @Override
    public Location getFixedSpawnLocation(@NotNull World world, @NotNull Random random) {
        validateWorldHeight(world);
        return new Location(world, spawn[0] + 0.5,
                surface(world.getSeed(), spawn[0], spawn[1], world.getMinHeight()) + 1, spawn[1] + 0.5);
    }

    @Override public @NotNull List<BlockPopulator> getDefaultPopulators(@NotNull World world) { return List.of(); }
    @Override public boolean shouldGenerateStructures() { return false; }
}
