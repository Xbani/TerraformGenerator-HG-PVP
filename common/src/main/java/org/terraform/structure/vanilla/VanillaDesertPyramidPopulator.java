package org.terraform.structure.vanilla;

import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.jetbrains.annotations.NotNull;
import org.terraform.biome.BiomeBank;
import org.terraform.coregen.TerraLootTable;
import org.terraform.coregen.populatordata.PopulatorDataAbstract;
import org.terraform.data.TerraformWorld;
import org.terraform.main.config.TConfig;

import java.util.Random;

public class VanillaDesertPyramidPopulator extends VanillaStructurePopulator {

    @Override
    protected boolean canSpawnInBiome(@NotNull BiomeBank biome) {
        return biome == BiomeBank.DESERT;
    }

    @Override
    protected boolean isStructureEnabled() {
        return TConfig.c.STRUCTURES_VANILLA_DESERT_PYRAMID_ENABLED;
    }

    @Override
    protected double getSpawnRatio() {
        return TConfig.c.STRUCTURES_VANILLA_DESERT_PYRAMID_SPAWNRATIO;
    }

    @Override
    protected String getLogName() {
        return "vanilla desert pyramid";
    }

    @Override
    protected void spawnStructure(@NotNull TerraformWorld tw,
                                  @NotNull Random random,
                                  @NotNull PopulatorDataAbstract data,
                                  int x,
                                  int y,
                                  int z,
                                  @NotNull BlockFace facing)
    {
        fill(data, x, y, z, facing, -11, -12, -11, 11, 14, 11, Material.AIR);
        spawnPyramidShell(data, x, y, z, facing);
        spawnTowers(data, x, y, z, facing);
        spawnInterior(data, x, y, z, facing);
        spawnTrapChamber(data, x, y, z, facing);
    }

    private static void spawnPyramidShell(@NotNull PopulatorDataAbstract data,
                                          int x,
                                          int y,
                                          int z,
                                          @NotNull BlockFace facing)
    {
        for (int dy = 0; dy <= 10; dy++) {
            int radius = 10 - dy;
            fill(data, x, y, z, facing, -radius, dy, -radius, radius, dy, radius, Material.SANDSTONE);
            if (radius > 2) {
                fill(data, x, y, z, facing, -radius + 1, dy, -radius + 1, radius - 1, dy, radius - 1, Material.AIR);
            }
            for (int i = -radius; i <= radius; i++) {
                setBlock(data, x, y, z, facing, i, dy, -radius, Material.CUT_SANDSTONE);
                setBlock(data, x, y, z, facing, i, dy, radius, Material.CUT_SANDSTONE);
                setBlock(data, x, y, z, facing, -radius, dy, i, Material.CUT_SANDSTONE);
                setBlock(data, x, y, z, facing, radius, dy, i, Material.CUT_SANDSTONE);
            }
        }

        fill(data, x, y, z, facing, -11, -1, -11, 11, -1, 11, Material.SANDSTONE);
        fill(data, x, y, z, facing, -1, 1, 7, 1, 3, 11, Material.AIR);
        fill(data, x, y, z, facing, -1, 1, 10, 1, 1, 13, Material.SANDSTONE);
    }

    private static void spawnTowers(@NotNull PopulatorDataAbstract data,
                                    int x,
                                    int y,
                                    int z,
                                    @NotNull BlockFace facing)
    {
        int[][] corners = {{-8, -8}, {-8, 8}, {8, -8}, {8, 8}};
        for (int[] corner : corners) {
            fill(data, x, y, z, facing, corner[0] - 2, 0, corner[1] - 2, corner[0] + 2, 9, corner[1] + 2, Material.SANDSTONE);
            fill(data, x, y, z, facing, corner[0] - 1, 1, corner[1] - 1, corner[0] + 1, 7, corner[1] + 1, Material.AIR);
            setBlock(data, x, y, z, facing, corner[0], 10, corner[1], Material.ORANGE_TERRACOTTA);
        }
    }

    private static void spawnInterior(@NotNull PopulatorDataAbstract data,
                                      int x,
                                      int y,
                                      int z,
                                      @NotNull BlockFace facing)
    {
        fill(data, x, y, z, facing, -5, 1, -5, 5, 5, 5, Material.AIR);
        fill(data, x, y, z, facing, -5, 0, -5, 5, 0, 5, Material.SANDSTONE);
        for (int i = -2; i <= 2; i++) {
            setBlock(data, x, y, z, facing, i, 0, 0, Material.BLUE_TERRACOTTA);
            setBlock(data, x, y, z, facing, 0, 0, i, Material.BLUE_TERRACOTTA);
        }
        setBlock(data, x, y, z, facing, -1, 0, -1, Material.ORANGE_TERRACOTTA);
        setBlock(data, x, y, z, facing, 1, 0, -1, Material.ORANGE_TERRACOTTA);
        setBlock(data, x, y, z, facing, -1, 0, 1, Material.ORANGE_TERRACOTTA);
        setBlock(data, x, y, z, facing, 1, 0, 1, Material.ORANGE_TERRACOTTA);
    }

    private static void spawnTrapChamber(@NotNull PopulatorDataAbstract data,
                                         int x,
                                         int y,
                                         int z,
                                         @NotNull BlockFace facing)
    {
        fill(data, x, y, z, facing, -1, -11, -1, 1, 0, 1, Material.AIR);
        fill(data, x, y, z, facing, -4, -11, -4, 4, -7, 4, Material.SANDSTONE);
        fill(data, x, y, z, facing, -3, -10, -3, 3, -8, 3, Material.AIR);
        setBlock(data, x, y, z, facing, 0, -11, 0, Material.STONE_PRESSURE_PLATE);
        setBlock(data, x, y, z, facing, 0, -12, 0, Material.TNT);
        setBlock(data, x, y, z, facing, 1, -12, 0, Material.TNT);
        setBlock(data, x, y, z, facing, -1, -12, 0, Material.TNT);
        setBlock(data, x, y, z, facing, 0, -12, 1, Material.TNT);
        setBlock(data, x, y, z, facing, 0, -12, -1, Material.TNT);
        placeLootChest(data, x, y, z, facing, 0, -9, -4);
        placeLootChest(data, x, y, z, facing, 0, -9, 4);
        placeLootChest(data, x, y, z, facing, -4, -9, 0);
        placeLootChest(data, x, y, z, facing, 4, -9, 0);
    }

    private static void placeLootChest(@NotNull PopulatorDataAbstract data,
                                       int x,
                                       int y,
                                       int z,
                                       @NotNull BlockFace facing,
                                       int relX,
                                       int relY,
                                       int relZ)
    {
        int[] rotated = rotate(relX, relZ, facing);
        data.lootTableChest(x + rotated[0], y + relY, z + rotated[1], TerraLootTable.DESERT_PYRAMID);
    }

    @Override
    public @NotNull Random getHashedRandom(@NotNull TerraformWorld world, int chunkX, int chunkZ) {
        return world.getHashedRand(98237142, chunkX, chunkZ);
    }
}
