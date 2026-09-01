package org.terraform.structure.hgpvp;

import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.NotNull;
import org.terraform.biome.BiomeBank;
import org.terraform.biome.BiomeClimate;
import org.terraform.biome.BiomeType;
import org.terraform.coregen.populatordata.PopulatorDataAbstract;
import org.terraform.data.MegaChunk;
import org.terraform.data.SimpleBlock;
import org.terraform.data.TerraformWorld;
import org.terraform.main.TerraformGeneratorPlugin;
import org.terraform.main.config.TConfig;
import org.terraform.schematic.SchematicParser;
import org.terraform.schematic.TerraSchematic;
import org.terraform.structure.MultiMegaChunkStructurePopulator;
import org.terraform.structure.StructureLocator;
import org.terraform.utils.BlockUtils;
import org.terraform.utils.GenUtils;

import java.io.FileNotFoundException;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;

/**
 * Places the HG-PvP WorldEdit schematics through the guaranteed, post-generation
 * structure path. This is intentionally separate from the normal small-structure
 * registry because several of these schematics span more than a LimitedRegion.
 */
public abstract class HGPvPSchematicPopulator extends MultiMegaChunkStructurePopulator {

    private static final String RESOURCE_FOLDER = "hgpvp/schematics/";
    private static final int CHANCE_PRECISION = 1_000_000;
    private static final EnumSet<BiomeBank> FOREST_BIOMES = EnumSet.of(
            BiomeBank.FOREST,
            BiomeBank.FLOWER_FOREST,
            BiomeBank.JUNGLE,
            BiomeBank.SPARSE_JUNGLE,
            BiomeBank.BAMBOO_FOREST,
            BiomeBank.SCARLET_FOREST,
            BiomeBank.CHERRY_GROVE,
            BiomeBank.TAIGA,
            BiomeBank.SNOWY_TAIGA,
            BiomeBank.DARK_FOREST,
            BiomeBank.PALE_FOREST,
            BiomeBank.BIRCH_MOUNTAINS,
            BiomeBank.FORESTED_MOUNTAINS,
            BiomeBank.FORESTED_PEAKS
    );
    private static final EnumSet<BiomeBank> BONFIRE_BIOMES = EnumSet.of(
            BiomeBank.PLAINS,
            BiomeBank.ELEVATED_PLAINS,
            BiomeBank.SAVANNA,
            BiomeBank.SHATTERED_SAVANNA,
            BiomeBank.SHATTERED_SAVANNA_PEAK
    );
    private static final EnumSet<BiomeBank> WORM_TOWER_BIOMES = EnumSet.of(
            BiomeBank.SWAMP,
            BiomeBank.MANGROVE,
            BiomeBank.MUSHROOM_ISLANDS,
            BiomeBank.MUSHROOM_BEACH,
            BiomeBank.MUDFLATS
    );

    private final String schematicFile;
    private final int salt;
    private final int horizontalRadius;
    private final boolean replaceLightWithCaveAir;
    private final boolean suppressVegetation;

    protected HGPvPSchematicPopulator(@NotNull String schematicFile,
                                      int salt,
                                      int horizontalRadius,
                                      boolean replaceLightWithCaveAir,
                                      boolean suppressVegetation)
    {
        this.schematicFile = schematicFile;
        this.salt = salt;
        this.horizontalRadius = horizontalRadius;
        this.replaceLightWithCaveAir = replaceLightWithCaveAir;
        this.suppressVegetation = suppressVegetation;
    }

    @Override
    public final void populate(@NotNull TerraformWorld tw, @NotNull PopulatorDataAbstract data) {
        if (!isEnabled()) {
            return;
        }

        MegaChunk mc = new MegaChunk(data.getChunkX(), data.getChunkZ());
        for (int[] coords : getCoordsFromMegaChunk(tw, mc)) {
            int x = coords[0];
            int z = coords[1];
            if (x >> 4 != data.getChunkX() || z >> 4 != data.getChunkZ() || !canSpawnAt(tw, mc, x, z)) {
                continue;
            }

            int y = GenUtils.getHighestGround(data, x, z) + getYOffset();
            Random random = getHashedRandom(tw, data.getChunkX(), data.getChunkZ());
            BlockFace facing = BlockUtils.getDirectBlockFace(random);
            try {
                String resourceName = RESOURCE_FOLDER + schematicFile.substring(0, schematicFile.length() - ".schem".length());
                TerraSchematic schematic = TerraSchematic.load(resourceName, new SimpleBlock(data, x, y, z));
                schematic.setFace(facing);
                schematic.parser = new HGPvPSchematicParser(replaceLightWithCaveAir);
                schematic.apply();
                postPlace(data, x, y, z, facing);
                TerraformGeneratorPlugin.logger.info("Spawning HG-PvP schematic "
                                                     + schematicFile
                                                     + " at "
                                                     + x
                                                     + ","
                                                     + y
                                                     + ","
                                                     + z);
            }
            catch (FileNotFoundException e) {
                TerraformGeneratorPlugin.logger.error("Missing bundled HG-PvP schematic: " + schematicFile);
                TerraformGeneratorPlugin.logger.stackTrace(e);
            }
            catch (Throwable e) {
                TerraformGeneratorPlugin.logger.error("Failed to place HG-PvP schematic "
                                                      + schematicFile
                                                      + " at "
                                                      + x
                                                      + ","
                                                      + y
                                                      + ","
                                                      + z);
                TerraformGeneratorPlugin.logger.stackTrace(e);
            }
        }
    }

    @Override
    public final boolean canSpawn(@NotNull TerraformWorld tw, int chunkX, int chunkZ) {
        if (!isEnabled()) {
            return false;
        }

        MegaChunk mc = new MegaChunk(chunkX, chunkZ);
        for (int[] coords : getCoordsFromMegaChunk(tw, mc)) {
            if (coords[0] >> 4 == chunkX
                && coords[1] >> 4 == chunkZ
                && canSpawnAt(tw, mc, coords[0], coords[1]))
            {
                return true;
            }
        }
        return false;
    }

    public final boolean suppressesVegetationInChunk(@NotNull TerraformWorld tw, int chunkX, int chunkZ) {
        if (!suppressVegetation || !isEnabled()) {
            return false;
        }

        MegaChunk center = new MegaChunk(chunkX, chunkZ);
        int chunkRadius = getChunkBufferDistance();
        for (int mx = -1; mx <= 1; mx++) {
            for (int mz = -1; mz <= 1; mz++) {
                MegaChunk mc = center.getRelative(mx, mz);
                for (int[] coords : getCoordsFromMegaChunk(tw, mc)) {
                    if (Math.abs((coords[0] >> 4) - chunkX) <= chunkRadius
                        && Math.abs((coords[1] >> 4) - chunkZ) <= chunkRadius
                        && canSpawnAt(tw, mc, coords[0], coords[1]))
                    {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public int[][] getCoordsFromMegaChunk(@NotNull TerraformWorld tw, @NotNull MegaChunk mc) {
        return new int[][] {mc.getRandomCoords(tw.getHashedRand(salt, mc.getX(), mc.getZ()))};
    }

    @Override
    public int[] getNearestFeature(@NotNull TerraformWorld world, int rawX, int rawZ) {
        return StructureLocator.locateMultiMegaChunkStructure(world, new MegaChunk(rawX, 0, rawZ), this, -1);
    }

    @Override
    public final boolean isEnabled() {
        if (!TConfig.areStructuresEnabled()) {
            return false;
        }
        List<String> active = TConfig.c.STRUCTURES_HGPVP_SCHEMATICS_ACTIVE;
        return active != null && active.stream().anyMatch(schematicFile::equalsIgnoreCase);
    }

    @Override
    public final @NotNull Random getHashedRandom(@NotNull TerraformWorld world, int chunkX, int chunkZ) {
        return world.getHashedRand(salt * 31, chunkX, chunkZ);
    }

    @Override
    public final int getChunkBufferDistance() {
        return Math.max(1, (horizontalRadius + 15) / 16);
    }

    public final @NotNull String getSchematicFile() {
        return schematicFile;
    }

    private boolean canSpawnAt(@NotNull TerraformWorld tw, @NotNull MegaChunk mc, int x, int z) {
        if (!isValidBiome(tw.getBiomeBank(x, z))) {
            return false;
        }
        double ratio = Math.max(0.0d, Math.min(1.0d, getSpawnRatio()));
        return GenUtils.chance(
                tw.getHashedRand(salt * 31 + 1, mc.getX(), mc.getZ()),
                (int) Math.round(ratio * CHANCE_PRECISION),
                CHANCE_PRECISION
        );
    }

    protected abstract boolean isValidBiome(@NotNull BiomeBank biome);

    protected abstract double getSpawnRatio();

    protected abstract int getYOffset();

    protected void postPlace(@NotNull PopulatorDataAbstract data,
                             int x,
                             int y,
                             int z,
                             @NotNull BlockFace facing)
    {
    }

    protected static int @NotNull [] rotate(int relX, int relZ, @NotNull BlockFace facing) {
        return switch (facing) {
            case EAST -> new int[] {-relZ, relX};
            case SOUTH -> new int[] {-relX, -relZ};
            case WEST -> new int[] {relZ, -relX};
            default -> new int[] {relX, relZ};
        };
    }

    private static final class HGPvPSchematicParser extends SchematicParser {
        private final boolean replaceLightWithCaveAir;

        private HGPvPSchematicParser(boolean replaceLightWithCaveAir) {
            this.replaceLightWithCaveAir = replaceLightWithCaveAir;
        }

        @Override
        public void applyData(@NotNull SimpleBlock block, @NotNull BlockData data) {
            if (replaceLightWithCaveAir && data.getMaterial() == Material.LIGHT) {
                block.setType(Material.CAVE_AIR);
                return;
            }
            super.applyData(block, data);
        }
    }

    public static final class AmongUsPopulator extends HGPvPSchematicPopulator {
        public AmongUsPopulator() {
            super("AmongUs.schem", 724_001, 7, false, true);
        }

        @Override
        public int[][] getCoordsFromMegaChunk(@NotNull TerraformWorld tw, @NotNull MegaChunk mc) {
            int x = TConfig.c.STRUCTURES_HGPVP_AMONG_US_X;
            int z = TConfig.c.STRUCTURES_HGPVP_AMONG_US_Z;
            return mc.containsXZBlockCoords(x, z) ? new int[][] {{x, z}} : new int[0][2];
        }

        @Override
        public int[] getNearestFeature(@NotNull TerraformWorld world, int rawX, int rawZ) {
            int x = TConfig.c.STRUCTURES_HGPVP_AMONG_US_X;
            int z = TConfig.c.STRUCTURES_HGPVP_AMONG_US_Z;
            return canSpawn(world, x >> 4, z >> 4) ? new int[] {x, z} : null;
        }

        @Override
        protected boolean isValidBiome(@NotNull BiomeBank biome) {
            return true;
        }

        @Override
        protected double getSpawnRatio() {
            return TConfig.c.STRUCTURES_HGPVP_AMONG_US_SPAWNRATIO;
        }

        @Override
        protected int getYOffset() {
            return TConfig.c.STRUCTURES_HGPVP_AMONG_US_Y_OFFSET;
        }
    }

    public static final class BonfirePopulator extends HGPvPSchematicPopulator {
        public BonfirePopulator() {
            super("Bonfire.schem", 724_002, 13, false, true);
        }

        @Override
        protected boolean isValidBiome(@NotNull BiomeBank biome) {
            return BONFIRE_BIOMES.contains(biome);
        }

        @Override
        protected double getSpawnRatio() {
            return TConfig.c.STRUCTURES_HGPVP_BONFIRE_SPAWNRATIO;
        }

        @Override
        protected int getYOffset() {
            return TConfig.c.STRUCTURES_HGPVP_BONFIRE_Y_OFFSET;
        }
    }

    public static final class IronEggPopulator extends HGPvPSchematicPopulator {
        public IronEggPopulator() {
            super("IronEgg.schem", 724_003, 17, false, true);
        }

        @Override
        protected boolean isValidBiome(@NotNull BiomeBank biome) {
            return biome.getType().isDry();
        }

        @Override
        protected double getSpawnRatio() {
            return TConfig.c.STRUCTURES_HGPVP_IRON_EGG_SPAWNRATIO;
        }

        @Override
        protected int getYOffset() {
            return TConfig.c.STRUCTURES_HGPVP_IRON_EGG_Y_OFFSET;
        }
    }

    public static final class JungleTemplePopulator extends HGPvPSchematicPopulator {
        public JungleTemplePopulator() {
            super("JungleTemple.schem", 724_004, 72, true, true);
        }

        @Override
        protected boolean isValidBiome(@NotNull BiomeBank biome) {
            return FOREST_BIOMES.contains(biome);
        }

        @Override
        protected double getSpawnRatio() {
            double configured = TConfig.c.STRUCTURES_HGPVP_JUNGLE_TEMPLE_SPAWNRATIO;
            return configured < 0.0d ? TConfig.c.STRUCTURES_PYRAMID_SPAWNRATIO : configured;
        }

        @Override
        protected int getYOffset() {
            return TConfig.c.STRUCTURES_HGPVP_JUNGLE_TEMPLE_Y_OFFSET;
        }

        @Override
        protected void postPlace(@NotNull PopulatorDataAbstract data,
                                 int x,
                                 int y,
                                 int z,
                                 @NotNull BlockFace facing)
        {
            int[] rotated = rotate(-6, 31, facing);
            EntityType bogged;
            try {
                bogged = EntityType.valueOf("BOGGED");
            }
            catch (IllegalArgumentException ignored) {
                bogged = EntityType.SKELETON;
            }
            data.setSpawner(x + rotated[0], y + 10, z + rotated[1], bogged);
        }
    }

    public static final class ReversedTreePopulator extends HGPvPSchematicPopulator {
        public ReversedTreePopulator() {
            super("ReversedTree.schem", 724_005, 23, false, true);
        }

        @Override
        protected boolean isValidBiome(@NotNull BiomeBank biome) {
            return FOREST_BIOMES.contains(biome);
        }

        @Override
        protected double getSpawnRatio() {
            return TConfig.c.STRUCTURES_HGPVP_REVERSED_TREE_SPAWNRATIO;
        }

        @Override
        protected int getYOffset() {
            return TConfig.c.STRUCTURES_HGPVP_REVERSED_TREE_Y_OFFSET;
        }
    }

    public static final class SculkTreePopulator extends HGPvPSchematicPopulator {
        public SculkTreePopulator() {
            super("SculkTree.schem", 724_006, 12, true, true);
        }

        @Override
        protected boolean isValidBiome(@NotNull BiomeBank biome) {
            return FOREST_BIOMES.contains(biome);
        }

        @Override
        protected double getSpawnRatio() {
            return TConfig.c.STRUCTURES_HGPVP_SCULK_TREE_SPAWNRATIO;
        }

        @Override
        protected int getYOffset() {
            return TConfig.c.STRUCTURES_HGPVP_SCULK_TREE_Y_OFFSET;
        }
    }

    public static final class SovietBunkerPopulator extends HGPvPSchematicPopulator {
        public SovietBunkerPopulator() {
            super("Sovietbunker.schem", 724_007, 54, true, true);
        }

        @Override
        protected boolean isValidBiome(@NotNull BiomeBank biome) {
            return biome.getType() == BiomeType.MOUNTAINOUS
                   || biome.getType() == BiomeType.HIGH_MOUNTAINOUS
                   || biome.getClimate() == BiomeClimate.COLD
                   || biome.getClimate() == BiomeClimate.SNOWY;
        }

        @Override
        protected double getSpawnRatio() {
            return TConfig.c.STRUCTURES_HGPVP_SOVIET_BUNKER_SPAWNRATIO;
        }

        @Override
        protected int getYOffset() {
            return TConfig.c.STRUCTURES_HGPVP_SOVIET_BUNKER_Y_OFFSET;
        }
    }

    public static final class WormTowerPopulator extends HGPvPSchematicPopulator {
        public WormTowerPopulator() {
            super("WormTower.schem", 724_008, 8, false, true);
        }

        @Override
        protected boolean isValidBiome(@NotNull BiomeBank biome) {
            return WORM_TOWER_BIOMES.contains(biome);
        }

        @Override
        protected double getSpawnRatio() {
            return TConfig.c.STRUCTURES_HGPVP_WORM_TOWER_SPAWNRATIO;
        }

        @Override
        protected int getYOffset() {
            return TConfig.c.STRUCTURES_HGPVP_WORM_TOWER_Y_OFFSET;
        }
    }
}
