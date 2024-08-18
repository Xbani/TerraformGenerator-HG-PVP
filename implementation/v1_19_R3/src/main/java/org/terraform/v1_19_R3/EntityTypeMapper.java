package org.terraform.v1_19_R3;

import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.NotNull;
import org.terraform.main.TerraformGeneratorPlugin;

import java.util.HashMap;

@SuppressWarnings("UnstableApiUsage")
public class EntityTypeMapper {

    private static final HashMap<EntityType, String> obsNames = new HashMap<>(){{
        put(EntityType.ALLAY, "b");
        put(EntityType.AREA_EFFECT_CLOUD, "c");
        put(EntityType.ARMOR_STAND, "d");
        put(EntityType.ARROW, "e");
        put(EntityType.AXOLOTL, "f");
        put(EntityType.BAT, "g");
        put(EntityType.BEE, "h");
        put(EntityType.BLAZE, "i");
        put(EntityType.BLOCK_DISPLAY, "j");
        put(EntityType.BOAT, "k");
        put(EntityType.CAMEL, "l");
        put(EntityType.CAT, "m");
        put(EntityType.CAVE_SPIDER, "n");
        put(EntityType.CHEST_BOAT, "o");
        put(EntityType.MINECART_CHEST, "p");
        put(EntityType.CHICKEN, "q");
        put(EntityType.COD, "r");
        put(EntityType.MINECART_COMMAND, "s");
        put(EntityType.COW, "t");
        put(EntityType.CREEPER, "u");
        put(EntityType.DOLPHIN, "v");
        put(EntityType.DONKEY, "w");
        put(EntityType.DRAGON_FIREBALL, "x");
        put(EntityType.DROWNED, "y");
        put(EntityType.EGG, "z");
        put(EntityType.ELDER_GUARDIAN, "A");
        put(EntityType.ENDER_CRYSTAL, "B");
        put(EntityType.ENDER_DRAGON, "C");
        put(EntityType.ENDER_PEARL, "D");
        put(EntityType.ENDERMAN, "E");
        put(EntityType.ENDERMITE, "F");
        put(EntityType.EVOKER, "G");
        put(EntityType.EVOKER_FANGS, "H");
        put(EntityType.THROWN_EXP_BOTTLE, "I");
        put(EntityType.EXPERIENCE_ORB, "J");
        //put(EntityType.EYE_OF_ENDER, "K");
        put(EntityType.FALLING_BLOCK, "L");
        put(EntityType.FIREWORK, "M");
        put(EntityType.FOX, "N");
        put(EntityType.FROG, "O");
        put(EntityType.MINECART_FURNACE, "P");
        put(EntityType.GHAST, "Q");
        put(EntityType.GIANT, "R");
        put(EntityType.GLOW_ITEM_FRAME, "S");
        put(EntityType.GLOW_SQUID, "T");
        put(EntityType.GOAT, "U");
        put(EntityType.GUARDIAN, "V");
        put(EntityType.HOGLIN, "W");
        put(EntityType.MINECART_HOPPER, "X");
        put(EntityType.HORSE, "Y");
        put(EntityType.HUSK, "Z");
        put(EntityType.ILLUSIONER, "aa");
        put(EntityType.INTERACTION, "ab");
        put(EntityType.IRON_GOLEM, "ac");
        put(EntityType.DROPPED_ITEM, "ad");
        put(EntityType.ITEM_DISPLAY, "ae");
        put(EntityType.ITEM_FRAME, "af");
        put(EntityType.FIREBALL, "ag");
        put(EntityType.LEASH_HITCH, "ah");
        put(EntityType.LIGHTNING, "ai");
        put(EntityType.LLAMA, "aj");
        put(EntityType.LLAMA_SPIT, "ak");
        put(EntityType.MAGMA_CUBE, "al");
        put(EntityType.MARKER, "am");
        put(EntityType.MINECART, "an");
        put(EntityType.MUSHROOM_COW, "ao");
        put(EntityType.MULE, "ap");
        put(EntityType.OCELOT, "aq");
        put(EntityType.PAINTING, "ar");
        put(EntityType.PANDA, "as");
        put(EntityType.PARROT, "at");
        put(EntityType.PHANTOM, "au");
        put(EntityType.PIG, "av");
        put(EntityType.PIGLIN, "aw");
        put(EntityType.PIGLIN_BRUTE, "ax");
        put(EntityType.PILLAGER, "ay");
        put(EntityType.POLAR_BEAR, "az");
        put(EntityType.SPLASH_POTION, "aA");
        put(EntityType.PUFFERFISH, "aB");
        put(EntityType.RABBIT, "aC");
        put(EntityType.RAVAGER, "aD");
        put(EntityType.SALMON, "aE");
        put(EntityType.SHEEP, "aF");
        put(EntityType.SHULKER, "aG");
        put(EntityType.SHULKER_BULLET, "aH");
        put(EntityType.SILVERFISH, "aI");
        put(EntityType.SKELETON, "aJ");
        put(EntityType.SKELETON_HORSE, "aK");
        put(EntityType.SLIME, "aL");
        put(EntityType.SMALL_FIREBALL, "aM");
        put(EntityType.SNIFFER, "aN");
        put(EntityType.SNOWMAN, "aO");
        put(EntityType.SNOWBALL, "aP");
        put(EntityType.MINECART_MOB_SPAWNER, "aQ");
        put(EntityType.SPECTRAL_ARROW, "aR");
        put(EntityType.SPIDER, "aS");
        put(EntityType.SQUID, "aT");
        put(EntityType.STRAY, "aU");
        put(EntityType.STRIDER, "aV");
        put(EntityType.TADPOLE, "aW");
        put(EntityType.TEXT_DISPLAY, "aX");
        put(EntityType.PRIMED_TNT, "aY");
        put(EntityType.MINECART_TNT, "aZ");
        put(EntityType.TRADER_LLAMA, "ba");
        put(EntityType.TRIDENT, "bb");
        put(EntityType.TROPICAL_FISH, "bc");
        put(EntityType.TURTLE, "bd");
        put(EntityType.VEX, "be");
        put(EntityType.VILLAGER, "bf");
        put(EntityType.VINDICATOR, "bg");
        put(EntityType.WANDERING_TRADER, "bh");
        put(EntityType.WARDEN, "bi");
        put(EntityType.WITCH, "bj");
        put(EntityType.WITHER, "bk");
        put(EntityType.WITHER_SKELETON, "bl");
        put(EntityType.WITHER_SKULL, "bm");
        put(EntityType.WOLF, "bn");
        put(EntityType.ZOGLIN, "bo");
        put(EntityType.ZOMBIE, "bp");
        put(EntityType.ZOMBIE_HORSE, "bq");
        put(EntityType.ZOMBIE_VILLAGER, "br");
        put(EntityType.ZOMBIFIED_PIGLIN, "bs");
        put(EntityType.PLAYER, "bt");
        put(EntityType.FISHING_HOOK, "bu");
    }};
	public static @NotNull String getObfsNameFromBukkitEntityType(@NotNull EntityType e){
        String name = obsNames.get(e);
        if(name == null) {
            TerraformGeneratorPlugin.logger.error("INVALID ENTITY REQUESTED: " + e);
            return "";
        }
        return name;
	}
}
