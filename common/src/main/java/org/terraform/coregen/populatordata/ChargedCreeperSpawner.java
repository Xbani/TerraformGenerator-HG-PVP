package org.terraform.coregen.populatordata;

import org.bukkit.Bukkit;
import org.bukkit.block.CreatureSpawner;
import org.terraform.main.TerraformGeneratorPlugin;

import java.lang.reflect.Method;

final class ChargedCreeperSpawner {
    private static final String CHARGED_CREEPER_DATA = "{id:\"minecraft:creeper\",powered:1b}";

    private ChargedCreeperSpawner() {}

    static boolean apply(CreatureSpawner spawner) {
        try {
            Method getEntityFactory = Bukkit.class.getMethod("getEntityFactory");
            Object entityFactory = getEntityFactory.invoke(null);

            Class<?> entityFactoryClass = Class.forName("org.bukkit.entity.EntityFactory");
            Object snapshot = entityFactoryClass
                    .getMethod("createEntitySnapshot", String.class)
                    .invoke(entityFactory, CHARGED_CREEPER_DATA);

            Class<?> entitySnapshotClass = Class.forName("org.bukkit.entity.EntitySnapshot");
            Class<?> baseSpawnerClass = Class.forName("org.bukkit.spawner.BaseSpawner");
            baseSpawnerClass.getMethod("setSpawnedEntity", entitySnapshotClass).invoke(spawner, snapshot);
            return true;
        }
        catch (ReflectiveOperationException | RuntimeException e) {
            TerraformGeneratorPlugin.logger.error(
                    "This server does not support powered creeper SpawnData; using a normal creeper spawner."
            );
            TerraformGeneratorPlugin.logger.stackTrace(e);
            return false;
        }
    }
}
