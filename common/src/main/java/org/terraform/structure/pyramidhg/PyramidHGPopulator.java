package org.terraform.structure.pyramidhg;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Stairs;
import org.jetbrains.annotations.NotNull;
import org.terraform.biome.BiomeBank;
import org.terraform.coregen.HeightMap;
import org.terraform.coregen.bukkit.TerraformGenerator;
import org.terraform.coregen.populatordata.PopulatorDataAbstract;
import org.terraform.data.CoordPair;
import org.terraform.data.MegaChunk;
import org.terraform.data.SimpleBlock;
import org.terraform.data.TerraformWorld;
import org.terraform.data.Wall;
import org.terraform.main.TerraformGeneratorPlugin;
import org.terraform.main.config.TConfig;
import org.terraform.structure.SingleMegaChunkStructurePopulator;
import org.terraform.structure.room.CubeRoom;
import org.terraform.structure.room.RoomLayout;
import org.terraform.structure.room.RoomLayoutGenerator;
import org.terraform.utils.GenUtils;
import org.terraform.utils.MazeSpawner;
import org.terraform.utils.noise.FastNoise;
import org.terraform.utils.noise.NoiseCacheHandler;

import java.util.Random;

public class PyramidHGPopulator extends SingleMegaChunkStructurePopulator {
    private static final int PYRAMID_RADIUS = 40;
    private static final int PYRAMID_HEIGHT = 40;

    @Override
    public boolean canSpawn(@NotNull TerraformWorld tw, int chunkX, int chunkZ, BiomeBank biome) {
        return isEnabled()
               && biome == BiomeBank.DESERT
               && GenUtils.chance(tw.getHashedRand(chunkX, chunkZ, 763451),
                (int) (TConfig.c.STRUCTURES_PYRAMID_HG_SPAWNRATIO * 10000),
                10000
        );
    }

    @Override
    public void populate(@NotNull TerraformWorld tw, @NotNull PopulatorDataAbstract data) {
        if (!isEnabled()) {
            return;
        }
        CoordPair coords = new MegaChunk(data.getChunkX(), data.getChunkZ()).getCenterBiomeSectionBlockCoords();
        int x = coords.x();
        int z = coords.z();
        int y = HeightMap.getBlockHeight(tw, x, z);
        try {
            spawnPyramid(tw, tw.getHashedRand(x, y, z, 8211222), data, x, y, z);
        }
        catch (Throwable e) {
            TerraformGeneratorPlugin.logger.stackTrace(e);
        }
    }

    public void spawnPyramid(@NotNull TerraformWorld tw,
                             @NotNull Random random,
                             @NotNull PopulatorDataAbstract data,
                             int x,
                             int y,
                             int z)
    {
        y -= 10;
        TerraformGeneratorPlugin.logger.info("Spawning PyramidHG at: " + x + "," + z);
        if (y >= TerraformGenerator.seaLevel + 3) {
            spawnSandBase(tw, data, x, y, z);
        }
        else {
            spawnSandBase(tw, data, x, TerraformGenerator.seaLevel + 3, z);
            y = TerraformGenerator.seaLevel - 7;
        }
        spawnPyramidBase(data, x, y, z);

        int levelY = y + 8;
        int range = 40;
        RoomLayoutGenerator level = new RoomLayoutGenerator(
                tw.getHashedRand(x, levelY, z),
                RoomLayout.RANDOM_BRUTEFORCE,
                1000,
                x,
                levelY,
                z,
                range
        );
        level.setRoomMinX(7);
        level.setRoomMaxX(10);
        level.setRoomMinZ(7);
        level.setRoomMaxZ(10);
        level.setRoomMaxHeight(6);
        level.setPathPopulator(new PyramidHGPathPopulator(tw.getHashedRand(x, levelY, z, 2253)));

        MazeSpawner maze = new MazeSpawner();
        maze.setMazePeriod(5);
        maze.setWidthZ(range + 10);
        level.setMazePathGenerator(maze);
        level.registerRoomPopulator(new PyramidHGSpawnerRoom(random, false, false));

        CubeRoom mainRoom = new CubeRoom(20, 20, 20, x, levelY, z);
        mainRoom.setRoomPopulator(new PyramidHGMainRoom(tw.getHashedRand(x, levelY, z, 1121), true, true));
        level.getRooms().add(mainRoom);

        int entranceZ = z + 5 + range / 2;
        int entranceHeight = 4 + GenUtils.getHighestGround(data, x, entranceZ) - levelY;
        CubeRoom entrance = new CubeRoom(9, 9, Math.max(6, entranceHeight), x, levelY, entranceZ);
        entrance.setRoomPopulator(new PyramidHGEntranceRoom(random, BlockFace.NORTH));
        level.getRooms().add(entrance);

        level.calculateRoomPlacement(false);
        level.fill(data, tw, Material.SANDSTONE, Material.CUT_SANDSTONE);
    }

    private void spawnSandBase(@NotNull TerraformWorld tw,
                               @NotNull PopulatorDataAbstract data,
                               int x,
                               int y,
                               int z)
    {
        int squareRadius = 45;
        FastNoise elevationNoise = NoiseCacheHandler.getNoise(tw,
                NoiseCacheHandler.NoiseCacheEntry.STRUCTURE_PYRAMID_BASEELEVATOR,
                world -> {
                    FastNoise noise = new FastNoise((int) world.getSeed());
                    noise.SetNoiseType(FastNoise.NoiseType.PerlinFractal);
                    noise.SetFrequency(0.007f);
                    noise.SetFractalOctaves(6);
                    return noise;
                }
        );
        FastNoise edgeNoise = NoiseCacheHandler.getNoise(tw,
                NoiseCacheHandler.NoiseCacheEntry.STRUCTURE_PYRAMID_BASEFUZZER,
                world -> {
                    FastNoise noise = new FastNoise((int) world.getSeed());
                    noise.SetNoiseType(FastNoise.NoiseType.PerlinFractal);
                    noise.SetFrequency(0.01f);
                    noise.SetFractalOctaves(8);
                    return noise;
                }
        );

        for (int nx = x - squareRadius; nx <= x + squareRadius; nx++) {
            for (int nz = z - squareRadius; nz <= z + squareRadius; nz++) {
                int height = GenUtils.getHighestGround(data, nx, nz);
                Material material = data.getType(nx, height, nz);
                int originalHeight = height;
                int raised = 0;
                int newHeight = Math.max(y - 1, y + Math.round(elevationNoise.GetNoise(nx, nz) * 5) - 1);
                while (height < newHeight) {
                    raised++;
                    if (!data.getType(nx, height + 1, nz).isSolid()
                        || data.getType(nx, height + 1, nz) == Material.CACTUS)
                    {
                        data.setType(nx, height + 1, nz, material);
                    }
                    height++;
                }

                if (raised == 0) {
                    continue;
                }
                int xDistance = (int) (Math.abs(nx - x) + Math.abs(edgeNoise.GetNoise(nx - 80, nz - 80) * 25));
                int zDistance = (int) (Math.abs(nz - z) + Math.abs(edgeNoise.GetNoise(nx - 80, nz - 80) * 25));
                if (xDistance <= squareRadius - 10 && zDistance <= squareRadius - 10) {
                    continue;
                }
                int distance = Math.max(xDistance, zDistance);
                float target = originalHeight
                               + raised * ((((float) squareRadius - 5) - distance) / 5f)
                               + Math.abs(edgeNoise.GetNoise(nx, nz) * 30);
                target = Math.max(target, originalHeight);
                while (height > target) {
                    if (data.getType(nx, height, nz) == material) {
                        data.setType(nx,
                                height,
                                nz,
                                height > TerraformGenerator.seaLevel ? Material.AIR : Material.WATER
                        );
                    }
                    height--;
                }
            }
        }
    }

    private void spawnPyramidBase(@NotNull PopulatorDataAbstract data, int x, int y, int z) {
        for (int height = 0; height < PYRAMID_HEIGHT; height++) {
            int radius = PYRAMID_RADIUS - height;
            for (int nx = -radius; nx <= radius; nx++) {
                for (int nz = -radius; nz <= radius; nz++) {
                    data.setType(x + nx,
                            y + height,
                            z + nz,
                            GenUtils.randChoice(Material.SANDSTONE, Material.SMOOTH_SANDSTONE)
                    );
                    if (Math.abs(nx) == radius && Math.abs(nz) == radius) {
                        if (!data.getType(x + nx, y + height + 1, z + nz).isSolid()) {
                            data.setType(x + nx, y + height + 1, z + nz, Material.SANDSTONE_WALL);
                        }
                        if (height == 38) {
                            data.setType(x + nx, y + 40, z + nz, Material.CAMPFIRE);
                        }
                    }
                    else if (GenUtils.chance(1, 20)) {
                        BlockFace direction = getOuterFace(nx, nz, radius);
                        if (direction != null) {
                            Stairs stairs = (Stairs) Bukkit.createBlockData(GenUtils.randChoice(
                                    Material.SANDSTONE_STAIRS,
                                    Material.SMOOTH_SANDSTONE_STAIRS
                            ));
                            stairs.setFacing(direction);
                            data.setBlockData(x + nx, y + height, z + nz, stairs);
                        }
                    }
                }
            }
        }

        data.setType(x, y + 40, z, Material.GOLD_BLOCK);
        data.setType(x, y + 41, z, Material.SANDSTONE_WALL);
        spawnSurfaceDecals(data, x, y, z);
    }

    private static BlockFace getOuterFace(int nx, int nz, int radius) {
        if (nx == -radius) return BlockFace.EAST;
        if (nx == radius) return BlockFace.WEST;
        if (nz == -radius) return BlockFace.SOUTH;
        if (nz == radius) return BlockFace.NORTH;
        return null;
    }

    private static void spawnSurfaceDecals(@NotNull PopulatorDataAbstract data, int x, int y, int z) {
        int elevation = 14;
        for (int height = elevation; height <= elevation + 16; height++) {
            int radius = PYRAMID_RADIUS - height;
            int carveLength = height - elevation;
            if (carveLength > 8) {
                carveLength = 16 - carveLength;
            }
            for (int nx : new int[] {-radius, 0, radius}) {
                for (int nz : new int[] {-radius, 0, radius}) {
                    if (nx != 0 && nz != 0) {
                        continue;
                    }
                    Wall wall = createDecalWall(data, x + nx, y + height, z + nz, nx, nz, radius);
                    if (wall == null) {
                        continue;
                    }
                    for (int i = 0; i <= carveLength; i++) {
                        if (carveLength == 0) {
                            if (height == elevation) wall.getFront().setType(Material.SANDSTONE_WALL);
                            else if (height == elevation + 16) wall.getRear().getUp(2).setType(Material.SANDSTONE_WALL);
                        }
                        wall.getLeft(i).setType(Material.AIR);
                        wall.getLeft(i).getRear().setType(Material.CUT_RED_SANDSTONE);
                        wall.getRight(i).setType(Material.AIR);
                        wall.getRight(i).getRear().setType(Material.CUT_RED_SANDSTONE);
                        if (i == carveLength) {
                            wall.getRight(i + 1).getUp().setType(Material.SANDSTONE_WALL);
                            wall.getLeft(i + 1).getUp().setType(Material.SANDSTONE_WALL);
                        }
                    }
                }
            }
        }
    }

    private static Wall createDecalWall(@NotNull PopulatorDataAbstract data,
                                        int x,
                                        int y,
                                        int z,
                                        int nx,
                                        int nz,
                                        int radius)
    {
        SimpleBlock block = new SimpleBlock(data, x, y, z);
        if (nx == -radius) return new Wall(block, BlockFace.WEST);
        if (nx == radius) return new Wall(block, BlockFace.EAST);
        if (nz == -radius) return new Wall(block, BlockFace.NORTH);
        if (nz == radius) return new Wall(block, BlockFace.SOUTH);
        return null;
    }

    @Override
    public @NotNull Random getHashedRandom(@NotNull TerraformWorld world, int chunkX, int chunkZ) {
        return world.getHashedRand(82917299, chunkX, chunkZ);
    }

    @Override
    public boolean isEnabled() {
        return TConfig.areStructuresEnabled()
               && BiomeBank.isBiomeEnabled(BiomeBank.DESERT)
               && TConfig.c.STRUCTURES_PYRAMID_HG_ENABLED;
    }
}
