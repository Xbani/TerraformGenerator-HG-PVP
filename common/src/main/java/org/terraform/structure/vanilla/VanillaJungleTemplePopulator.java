package org.terraform.structure.vanilla;

import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.jetbrains.annotations.NotNull;
import org.terraform.biome.BiomeBank;
import org.terraform.coregen.TerraLootTable;
import org.terraform.coregen.populatordata.PopulatorDataAbstract;
import org.terraform.data.TerraformWorld;
import org.terraform.main.config.TConfig;
import org.terraform.utils.GenUtils;

import java.util.Random;

public class VanillaJungleTemplePopulator extends VanillaStructurePopulator {

    @Override
    protected boolean canSpawnInBiome(@NotNull BiomeBank biome) {
        return biome == BiomeBank.JUNGLE || biome == BiomeBank.BAMBOO_FOREST;
    }

    @Override
    protected boolean isStructureEnabled() {
        return TConfig.c.STRUCTURES_VANILLA_JUNGLE_TEMPLE_ENABLED;
    }

    @Override
    protected double getSpawnRatio() {
        return TConfig.c.STRUCTURES_VANILLA_JUNGLE_TEMPLE_SPAWNRATIO;
    }

    @Override
    protected String getLogName() {
        return "vanilla jungle temple";
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
        fill(data, x, y, z, facing, -6, -3, -7, 6, 9, 7, Material.AIR);
        spawnFoundation(random, data, x, y, z, facing);
        spawnLevels(random, data, x, y, z, facing);
        spawnDecorations(random, data, x, y, z, facing);
        spawnLoot(data, x, y, z, facing);
    }

    private static void spawnFoundation(@NotNull Random random,
                                        @NotNull PopulatorDataAbstract data,
                                        int x,
                                        int y,
                                        int z,
                                        @NotNull BlockFace facing)
    {
        for (int rx = -5; rx <= 5; rx++) {
            for (int rz = -6; rz <= 6; rz++) {
                for (int dy = -3; dy <= -1; dy++) {
                    setBlock(data, x, y, z, facing, rx, dy, rz, stone(random));
                }
            }
        }
    }

    private static void spawnLevels(@NotNull Random random,
                                    @NotNull PopulatorDataAbstract data,
                                    int x,
                                    int y,
                                    int z,
                                    @NotNull BlockFace facing)
    {
        fill(data, x, y, z, facing, -5, 0, -6, 5, 0, 6, stone(random));
        fill(data, x, y, z, facing, -5, 1, -6, 5, 4, 6, stone(random));
        fill(data, x, y, z, facing, -4, 1, -5, 4, 4, 5, Material.AIR);

        fill(data, x, y, z, facing, -4, 5, -5, 4, 5, 5, stone(random));
        fill(data, x, y, z, facing, -4, 6, -5, 4, 8, 5, stone(random));
        fill(data, x, y, z, facing, -3, 6, -4, 3, 8, 4, Material.AIR);

        fill(data, x, y, z, facing, -3, 9, -4, 3, 9, 4, stone(random));
        fill(data, x, y, z, facing, -1, 1, 5, 1, 3, 7, Material.AIR);
        fill(data, x, y, z, facing, -1, 6, 4, 1, 7, 6, Material.AIR);
        fill(data, x, y, z, facing, -1, 0, -6, 1, 0, -4, stone(random));
    }

    private static void spawnDecorations(@NotNull Random random,
                                         @NotNull PopulatorDataAbstract data,
                                         int x,
                                         int y,
                                         int z,
                                         @NotNull BlockFace facing)
    {
        for (int dy = 1; dy <= 8; dy++) {
            setBlock(data, x, y, z, facing, -5, dy, -6, stone(random));
            setBlock(data, x, y, z, facing, 5, dy, -6, stone(random));
            setBlock(data, x, y, z, facing, -5, dy, 6, stone(random));
            setBlock(data, x, y, z, facing, 5, dy, 6, stone(random));
        }
        for (int rz = -4; rz <= 4; rz += 4) {
            setBlock(data, x, y, z, facing, -2, 6, rz, Material.CHISELED_STONE_BRICKS);
            setBlock(data, x, y, z, facing, 2, 6, rz, Material.CHISELED_STONE_BRICKS);
        }
        setBlock(data, x, y, z, facing, -3, 2, -4, Material.LEVER);
        setBlock(data, x, y, z, facing, -3, 2, -2, Material.LEVER);
        setBlock(data, x, y, z, facing, -3, 2, 0, Material.LEVER);
        fill(data, x, y, z, facing, -2, -2, -5, 2, -2, 5, Material.AIR);
        fill(data, x, y, z, facing, -1, -1, -5, 1, -1, 5, stone(random));
    }

    private static void spawnLoot(@NotNull PopulatorDataAbstract data,
                                  int x,
                                  int y,
                                  int z,
                                  @NotNull BlockFace facing)
    {
        placeLootChest(data, x, y, z, facing, 0, 1, -4, TerraLootTable.JUNGLE_TEMPLE);
        placeLootChest(data, x, y, z, facing, 3, -2, 3, TerraLootTable.JUNGLE_TEMPLE);
        placeLootChest(data, x, y, z, facing, -3, 2, 2, TerraLootTable.JUNGLE_TEMPLE_DISPENSER);
    }

    private static void placeLootChest(@NotNull PopulatorDataAbstract data,
                                       int x,
                                       int y,
                                       int z,
                                       @NotNull BlockFace facing,
                                       int relX,
                                       int relY,
                                       int relZ,
                                       @NotNull TerraLootTable lootTable)
    {
        int[] rotated = rotate(relX, relZ, facing);
        data.lootTableChest(x + rotated[0], y + relY, z + rotated[1], lootTable);
    }

    private static @NotNull Material stone(@NotNull Random random) {
        return GenUtils.randChoice(random,
                Material.COBBLESTONE,
                Material.COBBLESTONE,
                Material.MOSSY_COBBLESTONE,
                Material.MOSSY_COBBLESTONE
        );
    }

    @Override
    public @NotNull Random getHashedRandom(@NotNull TerraformWorld world, int chunkX, int chunkZ) {
        return world.getHashedRand(63821759, chunkX, chunkZ);
    }
}
