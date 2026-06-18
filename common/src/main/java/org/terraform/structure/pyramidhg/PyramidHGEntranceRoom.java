package org.terraform.structure.pyramidhg;

import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.jetbrains.annotations.NotNull;
import org.terraform.coregen.populatordata.PopulatorDataAbstract;
import org.terraform.data.SimpleBlock;
import org.terraform.data.Wall;
import org.terraform.structure.room.CubeRoom;
import org.terraform.structure.room.RoomPopulatorAbstract;
import org.terraform.utils.BlockUtils;
import org.terraform.utils.GenUtils;

import java.util.Random;

public class PyramidHGEntranceRoom extends RoomPopulatorAbstract {
    private final BlockFace entranceFace;

    public PyramidHGEntranceRoom(Random rand, BlockFace entranceFace) {
        super(rand, false, false);
        this.entranceFace = entranceFace;
    }

    @Override
    public void populate(@NotNull PopulatorDataAbstract data, @NotNull CubeRoom room) {
        int surfaceOffset = room.getHeight() - 5;
        int[] roomUpper = room.getUpperCorner();
        int[] roomLower = room.getLowerCorner();

        for (int h = 0; h <= 6; h++) {
            int[] upper = room.getUpperCorner(-(6 - h));
            int[] lower = room.getLowerCorner(-(6 - h));
            for (int x = lower[0]; x <= upper[0]; x++) {
                for (int z = lower[1]; z <= upper[1]; z++) {
                    if ((x > roomLower[0] && x < roomUpper[0] && z > roomLower[1] && z < roomUpper[1]) || h == 6) {
                        continue;
                    }
                    data.setType(x,
                            room.getY() + surfaceOffset + h,
                            z,
                            h == 2 ? Material.CHISELED_RED_SANDSTONE : GenUtils.randChoice(
                                    Material.SANDSTONE,
                                    Material.SMOOTH_SANDSTONE
                            )
                    );
                    if (h == 0) {
                        BlockUtils.setDownUntilSolid(x,
                                room.getY() + surfaceOffset - 1,
                                z,
                                data,
                                Material.SANDSTONE
                        );
                    }
                }
            }
        }

        Wall tunnel = new Wall(
                new SimpleBlock(data, room.getX(), room.getY() + surfaceOffset + 1, room.getZ()),
                entranceFace.getOppositeFace()
        ).getFront(3);
        for (int depth = 0; depth <= 8; depth++) {
            tunnel = tunnel.getFront();
            tunnel.Pillar(4, rand, Material.AIR);
            tunnel.getLeft().Pillar(3, rand, Material.AIR);
            tunnel.getRight().Pillar(3, rand, Material.AIR);
        }
    }

    @Override
    public boolean canPopulate(@NotNull CubeRoom room) {
        return false;
    }
}
