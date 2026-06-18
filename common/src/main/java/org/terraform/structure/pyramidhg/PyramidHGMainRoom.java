package org.terraform.structure.pyramidhg;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Bisected.Half;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.type.Stairs;
import org.jetbrains.annotations.NotNull;
import org.terraform.coregen.TerraLootTable;
import org.terraform.coregen.populatordata.PopulatorDataAbstract;
import org.terraform.data.SimpleBlock;
import org.terraform.data.Wall;
import org.terraform.main.TerraformGeneratorPlugin;
import org.terraform.schematic.SchematicParser;
import org.terraform.schematic.TerraSchematic;
import org.terraform.structure.room.CubeRoom;
import org.terraform.structure.room.RoomPopulatorAbstract;
import org.terraform.utils.BlockUtils;

import java.util.Map.Entry;
import java.util.Random;

public class PyramidHGMainRoom extends RoomPopulatorAbstract {
    public PyramidHGMainRoom(Random rand, boolean forceSpawn, boolean unique) {
        super(rand, forceSpawn, unique);
    }

    @Override
    public void populate(@NotNull PopulatorDataAbstract data, @NotNull CubeRoom room) {
        SimpleBlock base = new SimpleBlock(data, room.getX(), room.getY() + 1, room.getZ());

        for (BlockFace face : BlockUtils.directBlockFaces) {
            placeStatue(base.getRelative(face, 4), face.getOppositeFace());
            placeLootChest(data, base, face);
        }

        for (BlockFace face : BlockUtils.xzDiagonalPlaneBlockFaces) {
            placePillar(new Wall(base.getRelative(face, 6)), room.getHeight() - 1);
        }

        SimpleBlock floorCenter = base.getDown();
        floorCenter.setType(Material.BLUE_TERRACOTTA);
        for (BlockFace face : BlockUtils.xzDiagonalPlaneBlockFaces) {
            floorCenter.getRelative(face).setType(Material.ORANGE_TERRACOTTA);
            new Wall(floorCenter.getRelative(face).getRelative(face).getUp()).Pillar(
                    room.getHeight(),
                    rand,
                    Material.CUT_SANDSTONE
            );
        }
        for (BlockFace face : BlockUtils.directBlockFaces) {
            floorCenter.getRelative(face, 2).setType(Material.ORANGE_TERRACOTTA);
        }

        SimpleBlock ceiling = floorCenter.getUp(room.getHeight());
        ceiling.setType(Material.BLUE_TERRACOTTA);
        for (BlockFace face : BlockUtils.xzDiagonalPlaneBlockFaces) {
            ceiling.getRelative(face).setType(Material.ORANGE_TERRACOTTA);
        }
        for (BlockFace face : BlockUtils.directBlockFaces) {
            ceiling.getRelative(face, 2).setType(Material.ORANGE_TERRACOTTA);
        }

        for (Entry<Wall, Integer> entry : room.getFourWalls(data, 0).entrySet()) {
            Wall wall = entry.getKey();
            for (int i = 0; i < entry.getValue(); i++) {
                if (i % 2 == 0 && i != 0 && i != entry.getValue() - 1) {
                    wall.getUp(4).Pillar(10, rand, Material.CHISELED_RED_SANDSTONE);
                }
                wall = wall.getLeft();
            }
        }

        for (Entry<Wall, Integer> entry : room.getFourWalls(data, 1).entrySet()) {
            Wall wall = entry.getKey().getRelative(0, room.getHeight() - 2, 0);
            for (int i = 0; i < entry.getValue(); i++) {
                Stairs stair = (Stairs) Bukkit.createBlockData(Material.RED_SANDSTONE_STAIRS);
                stair.setFacing(wall.getDirection().getOppositeFace());
                stair.setHalf(Half.TOP);
                wall.setBlockData(stair);
                wall = wall.getLeft();
            }
        }
    }

    private void placeLootChest(@NotNull PopulatorDataAbstract data,
                                @NotNull SimpleBlock center,
                                @NotNull BlockFace direction)
    {
        SimpleBlock target = center.getRelative(direction, 2);
        Directional chest = (Directional) Bukkit.createBlockData(Material.CHEST);
        chest.setFacing(direction.getOppositeFace());
        target.setBlockData(chest);
        data.lootTableChest(target.getX(), target.getY(), target.getZ(), TerraLootTable.SIMPLE_DUNGEON);
    }

    private void placePillar(@NotNull Wall base, int height) {
        for (BlockFace face : BlockUtils.xzDiagonalPlaneBlockFaces) {
            base.getRelative(face).Pillar(height, rand, Material.CUT_SANDSTONE, Material.SMOOTH_SANDSTONE);
        }
        for (BlockFace face : BlockUtils.directBlockFaces) {
            base.getRelative(face).Pillar(height,
                    true,
                    rand,
                    Material.CUT_SANDSTONE,
                    Material.CHISELED_SANDSTONE,
                    Material.AIR,
                    Material.AIR,
                    Material.AIR,
                    Material.CHISELED_SANDSTONE
            );
        }
        base.Pillar(height, rand, Material.CHISELED_RED_SANDSTONE);
    }

    private void placeStatue(@NotNull SimpleBlock base, @NotNull BlockFace direction) {
        try {
            TerraSchematic schematic = TerraSchematic.load("pharoah-statue", base);
            schematic.parser = new SchematicParser();
            schematic.setFace(direction);
            schematic.apply();
        }
        catch (Throwable e) {
            TerraformGeneratorPlugin.logger.stackTrace(e);
        }
    }

    @Override
    public boolean canPopulate(@NotNull CubeRoom room) {
        return true;
    }
}
