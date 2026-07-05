package org.terraform.structure.vanilla;

import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.jetbrains.annotations.NotNull;
import org.terraform.biome.BiomeBank;
import org.terraform.coregen.HeightMap;
import org.terraform.coregen.populatordata.PopulatorDataAbstract;
import org.terraform.data.MegaChunk;
import org.terraform.data.TerraformWorld;
import org.terraform.main.TerraformGeneratorPlugin;
import org.terraform.main.config.TConfig;
import org.terraform.structure.SingleMegaChunkStructurePopulator;
import org.terraform.utils.BlockUtils;
import org.terraform.utils.GenUtils;

import java.util.Random;

public abstract class VanillaStructurePopulator extends SingleMegaChunkStructurePopulator {

    @Override
    public boolean canSpawn(@NotNull TerraformWorld tw, int chunkX, int chunkZ, BiomeBank biome) {
        return isEnabled()
               && canSpawnInBiome(biome)
               && GenUtils.chance(getHashedRandom(tw, chunkX, chunkZ),
                (int) (getSpawnRatio() * 10000),
                10000
        );
    }

    @Override
    public void populate(@NotNull TerraformWorld tw, @NotNull PopulatorDataAbstract data) {
        if (!isEnabled()) {
            return;
        }

        MegaChunk mc = new MegaChunk(data.getChunkX(), data.getChunkZ());
        int[] coords = mc.getCenterBiomeSectionBlockCoords();
        int x = coords[0];
        int z = coords[1];
        int y = HeightMap.getBlockHeight(tw, x, z) + getYOffset();
        Random random = getHashedRandom(tw, data.getChunkX(), data.getChunkZ());
        BlockFace facing = BlockUtils.getDirectBlockFace(random);

        try {
            spawnStructure(tw, random, data, x, y, z, facing);
            TerraformGeneratorPlugin.logger.info("Spawning " + getLogName() + " at " + x + "," + y + "," + z);
        }
        catch (Throwable e) {
            TerraformGeneratorPlugin.logger.error("Something went wrong trying to place " + getLogName()
                                                  + " at "
                                                  + x
                                                  + ", "
                                                  + y
                                                  + ", "
                                                  + z);
            TerraformGeneratorPlugin.logger.stackTrace(e);
        }
    }

    @Override
    public boolean isEnabled() {
        return TConfig.areStructuresEnabled() && isStructureEnabled();
    }

    @Override
    public int getChunkBufferDistance() {
        return 2;
    }

    protected int getYOffset() {
        return 1;
    }

    protected static void setBlock(@NotNull PopulatorDataAbstract data,
                                   int originX,
                                   int originY,
                                   int originZ,
                                   @NotNull BlockFace facing,
                                   int relX,
                                   int relY,
                                   int relZ,
                                   @NotNull Material material)
    {
        int[] rotated = rotate(relX, relZ, facing);
        data.setType(originX + rotated[0], originY + relY, originZ + rotated[1], material);
    }

    protected static void fill(@NotNull PopulatorDataAbstract data,
                               int originX,
                               int originY,
                               int originZ,
                               @NotNull BlockFace facing,
                               int minX,
                               int minY,
                               int minZ,
                               int maxX,
                               int maxY,
                               int maxZ,
                               @NotNull Material material)
    {
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    setBlock(data, originX, originY, originZ, facing, x, y, z, material);
                }
            }
        }
    }

    protected static int @NotNull [] rotate(int relX, int relZ, @NotNull BlockFace facing) {
        return switch (facing) {
            case EAST -> new int[] {-relZ, relX};
            case SOUTH -> new int[] {-relX, -relZ};
            case WEST -> new int[] {relZ, -relX};
            default -> new int[] {relX, relZ};
        };
    }

    protected abstract boolean canSpawnInBiome(@NotNull BiomeBank biome);

    protected abstract boolean isStructureEnabled();

    protected abstract double getSpawnRatio();

    protected abstract String getLogName();

    protected abstract void spawnStructure(@NotNull TerraformWorld tw,
                                           @NotNull Random random,
                                           @NotNull PopulatorDataAbstract data,
                                           int x,
                                           int y,
                                           int z,
                                           @NotNull BlockFace facing);
}
