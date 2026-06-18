package org.terraform.structure.pyramidhg;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.NotNull;
import org.terraform.coregen.populatordata.PopulatorDataAbstract;
import org.terraform.data.SimpleBlock;
import org.terraform.structure.room.CubeRoom;
import org.terraform.structure.room.RoomPopulatorAbstract;

import java.util.Random;

public class PyramidHGSpawnerRoom extends RoomPopulatorAbstract {
    private static final EntityType[] SPAWNER_TYPES = {
            EntityType.HUSK,
            EntityType.CREEPER,
            EntityType.SILVERFISH,
            EntityType.ENDERMITE,
            EntityType.ZOMBIE,
            EntityType.SKELETON,
            EntityType.CAVE_SPIDER
    };

    public PyramidHGSpawnerRoom(Random rand, boolean forceSpawn, boolean unique) {
        super(rand, forceSpawn, unique);
    }

    @Override
    public void populate(@NotNull PopulatorDataAbstract data, @NotNull CubeRoom room) {
        SimpleBlock center = new SimpleBlock(data, room.getX(), room.getY() + 1, room.getZ());
        center.getDown().setType(Material.CHISELED_RED_SANDSTONE);

        int choice = rand.nextInt(8);
        if (choice == 7) {
            data.setChargedCreeperSpawner(center.getX(), center.getY(), center.getZ());
        }
        else {
            data.setSpawner(center.getX(), center.getY(), center.getZ(), SPAWNER_TYPES[choice]);
        }
    }

    @Override
    public boolean canPopulate(@NotNull CubeRoom room) {
        return true;
    }
}
