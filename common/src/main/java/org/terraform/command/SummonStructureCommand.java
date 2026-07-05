package org.terraform.command;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.terraform.command.contants.InvalidArgumentException;
import org.terraform.command.contants.TerraCommand;
import org.terraform.command.contants.TerraCommandArgument;
import org.terraform.coregen.populatordata.PopulatorDataAbstract;
import org.terraform.coregen.populatordata.PopulatorDataPostGen;
import org.terraform.data.TerraformWorld;
import org.terraform.main.TerraformGeneratorPlugin;
import org.terraform.structure.StructurePopulator;
import org.terraform.utils.BlockUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Stack;

public class SummonStructureCommand extends TerraCommand {

    public SummonStructureCommand(TerraformGeneratorPlugin plugin, String... aliases) {
        super(plugin, aliases);
        this.parameters.add(new LocateCommand.StructurePopulatorArgument("structureType", false));
        this.parameters.add(new RawStringArgument("target", true));
        this.parameters.add(new RawStringArgument("y", true));
        this.parameters.add(new RawStringArgument("z", true));
    }

    @Override
    public @NotNull String getDefaultDescription() {
        return "Spawns a TerraformGenerator structure at here, a player, or supplied coordinates.";
    }

    @Override
    public boolean canConsoleExec() {
        return true;
    }

    @Override
    public boolean hasPermission(@NotNull CommandSender sender) {
        return sender.hasPermission("terraformgenerator.summon");
    }

    @Override
    public void execute(@NotNull CommandSender sender, @NotNull Stack<String> args) throws InvalidArgumentException {
        String structureName = getNextArg(args);
        if (structureName == null) {
            throw new InvalidArgumentException("Usage: /terra summon <structure/populator> [here|player|x y z|x,y,z]");
        }

        StructurePopulator populator = new LocateCommand.StructurePopulatorArgument("structureType", false).parse(
                sender,
                structureName
        );
        if (populator == null) {
            throw new InvalidArgumentException("Structure type does not exist");
        }

        Location target = parseTarget(sender, args);
        PopulatorDataPostGen data = new PopulatorDataPostGen(target.getWorld()
                                                                   .getChunkAt(target.getBlockX() >> 4,
                                                                           target.getBlockZ() >> 4
                                                                   ));
        TerraformWorld tw = TerraformWorld.get(target.getWorld());
        Random random = populator.getHashedRandom(tw, target.getBlockX() >> 4, target.getBlockZ() >> 4);

        Method method = findSummonMethod(populator.getClass());
        if (method == null) {
            throw new InvalidArgumentException("No direct spawn method is available for "
                                               + populator.getClass().getSimpleName());
        }

        try {
            method.setAccessible(true);
            method.invoke(populator, buildMethodArgs(method,
                    tw,
                    random,
                    data,
                    target.getBlockX(),
                    target.getBlockY(),
                    target.getBlockZ()
            ));
            sender.sendMessage(ChatColor.GREEN
                               + "Spawned "
                               + populator.getClass().getSimpleName()
                               + " at "
                               + target.getBlockX()
                               + ", "
                               + target.getBlockY()
                               + ", "
                               + target.getBlockZ());
        }
        catch (IllegalAccessException e) {
            throw new InvalidArgumentException("Could not access spawn method: " + method.getName());
        }
        catch (InvocationTargetException e) {
            Throwable cause = e.getCause() == null ? e : e.getCause();
            TerraformGeneratorPlugin.logger.stackTrace(cause);
            throw new InvalidArgumentException("Structure spawn failed: " + cause.getClass().getSimpleName());
        }
    }

    private @NotNull Location parseTarget(@NotNull CommandSender sender, @NotNull Stack<String> args)
            throws InvalidArgumentException
    {
        if (args.isEmpty()) {
            return getSenderLocation(sender);
        }

        String first = args.pop();
        if (first.equalsIgnoreCase("here")) {
            return getSenderLocation(sender);
        }

        Player targetPlayer = Bukkit.getPlayerExact(first);
        if (targetPlayer != null) {
            return targetPlayer.getLocation();
        }

        Location base = sender instanceof Player player ? player.getLocation() : null;
        if (first.contains(",")) {
            String[] parts = first.split(",", -1);
            if (parts.length != 3) {
                throw new InvalidArgumentException("Coordinates must be x,y,z");
            }
            return parseCoordinates(sender, base, parts[0], parts[1], parts[2]);
        }

        if (args.size() < 2) {
            throw new InvalidArgumentException("Target must be here, a player, x,y,z, or x y z");
        }

        String y = args.pop();
        String z = args.pop();
        return parseCoordinates(sender, base, first, y, z);
    }

    private @NotNull Location parseCoordinates(@NotNull CommandSender sender,
                                               @Nullable Location base,
                                               @NotNull String xArg,
                                               @NotNull String yArg,
                                               @NotNull String zArg)
            throws InvalidArgumentException
    {
        World world = base != null ? base.getWorld() : Bukkit.getWorld("world");
        if (world == null) {
            throw new InvalidArgumentException("Console coordinate summon requires a world named \"world\" or a player target");
        }

        double x = parseCoordinate(xArg, base == null ? 0 : base.getX(), base != null);
        double y = parseCoordinate(yArg, base == null ? 0 : base.getY(), base != null);
        double z = parseCoordinate(zArg, base == null ? 0 : base.getZ(), base != null);
        return new Location(world, x, y, z);
    }

    private double parseCoordinate(@NotNull String arg, double base, boolean hasBase) throws InvalidArgumentException {
        try {
            if (arg.startsWith("~")) {
                if (!hasBase) {
                    throw new InvalidArgumentException("Relative coordinates require a player sender");
                }
                if (arg.length() == 1) {
                    return base;
                }
                return base + Double.parseDouble(arg.substring(1));
            }
            return Double.parseDouble(arg);
        }
        catch (NumberFormatException e) {
            throw new InvalidArgumentException("Invalid coordinate: " + arg);
        }
    }

    private @NotNull Location getSenderLocation(@NotNull CommandSender sender) throws InvalidArgumentException {
        if (!(sender instanceof Player player)) {
            throw new InvalidArgumentException("Console must specify a player target or absolute coordinates");
        }
        return player.getLocation();
    }

    private @Nullable Method findSummonMethod(@NotNull Class<?> populatorClass) {
        List<Method> methods = new ArrayList<>();
        Class<?> current = populatorClass;
        while (current != null && current != Object.class) {
            methods.addAll(Arrays.asList(current.getDeclaredMethods()));
            current = current.getSuperclass();
        }

        return methods.stream()
                      .filter(method -> method.getName().startsWith("spawn"))
                      .filter(method -> !Modifier.isAbstract(method.getModifiers()))
                      .filter(this::isSupportedSpawnSignature)
                      .max(Comparator.comparingInt(this::scoreSpawnMethod))
                      .orElse(null);
    }

    private boolean isSupportedSpawnSignature(@NotNull Method method) {
        Class<?>[] types = method.getParameterTypes();
        if (matches(types,
                TerraformWorld.class,
                Random.class,
                PopulatorDataAbstract.class,
                int.class,
                int.class,
                int.class
        ))
        {
            return true;
        }
        if (matches(types,
                TerraformWorld.class,
                Random.class,
                PopulatorDataAbstract.class,
                int.class,
                int.class,
                int.class,
                boolean.class
        ))
        {
            return true;
        }
        if (matches(types,
                TerraformWorld.class,
                Random.class,
                PopulatorDataAbstract.class,
                int.class,
                int.class,
                int.class,
                BlockFace.class
        ))
        {
            return true;
        }
        if (matches(types, TerraformWorld.class, PopulatorDataAbstract.class, int.class, int.class, int.class)) {
            return true;
        }
        if (matches(types, int.class, int.class, TerraformWorld.class, Random.class, PopulatorDataAbstract.class)) {
            return true;
        }
        return matches(types, int.class, int.class, int.class, TerraformWorld.class, Random.class, PopulatorDataAbstract.class);
    }

    private int scoreSpawnMethod(@NotNull Method method) {
        int score = method.getName().equals("spawnStructure") ? 100 : 0;
        Class<?>[] types = method.getParameterTypes();
        if (types.length >= 6 && types[0] == TerraformWorld.class && types[1] == Random.class) {
            score += 50;
        }
        if (method.getName().toLowerCase(Locale.ENGLISH).contains("base")
            || method.getName().toLowerCase(Locale.ENGLISH).contains("entrance"))
        {
            score -= 50;
        }
        return score + types.length;
    }

    private boolean matches(Class<?> @NotNull [] actual, Class<?> @NotNull ... expected) {
        if (actual.length != expected.length) {
            return false;
        }
        for (int i = 0; i < actual.length; i++) {
            if (!expected[i].isAssignableFrom(actual[i]) && actual[i] != expected[i]) {
                return false;
            }
        }
        return true;
    }

    private Object @NotNull [] buildMethodArgs(@NotNull Method method,
                                               @NotNull TerraformWorld tw,
                                               @NotNull Random random,
                                               @NotNull PopulatorDataPostGen data,
                                               int x,
                                               int y,
                                               int z)
    {
        Class<?>[] types = method.getParameterTypes();
        if (matches(types,
                TerraformWorld.class,
                Random.class,
                PopulatorDataAbstract.class,
                int.class,
                int.class,
                int.class
        ))
        {
            return new Object[] {tw, random, data, x, y, z};
        }
        if (matches(types,
                TerraformWorld.class,
                Random.class,
                PopulatorDataAbstract.class,
                int.class,
                int.class,
                int.class,
                boolean.class
        ))
        {
            return new Object[] {tw, random, data, x, y, z, tw.getBiomeBank(x, z).toString().contains("BADLANDS")};
        }
        if (matches(types,
                TerraformWorld.class,
                Random.class,
                PopulatorDataAbstract.class,
                int.class,
                int.class,
                int.class,
                BlockFace.class
        ))
        {
            return new Object[] {tw, random, data, x, y, z, BlockUtils.getDirectBlockFace(random)};
        }
        if (matches(types, TerraformWorld.class, PopulatorDataAbstract.class, int.class, int.class, int.class)) {
            return new Object[] {tw, data, x, y, z};
        }
        if (matches(types, int.class, int.class, TerraformWorld.class, Random.class, PopulatorDataAbstract.class)) {
            return new Object[] {x, z, tw, random, data};
        }
        return new Object[] {x, y, z, tw, random, data};
    }

    private static class RawStringArgument extends TerraCommandArgument<String> {

        public RawStringArgument(String name, boolean isOptional) {
            super(name, isOptional);
        }

        @Override
        public @Nullable String parse(CommandSender sender, String value) {
            return value;
        }

        @Override
        public @NotNull String validate(CommandSender sender, String value) {
            return "";
        }

        @Override
        public @NotNull ArrayList<String> getTabOptions(String @NotNull [] args) {
            if (args.length == 3) {
                ArrayList<String> values = new ArrayList<>();
                values.add("here");
                Bukkit.getOnlinePlayers().forEach(player -> values.add(player.getName()));
                return values;
            }
            return new ArrayList<>();
        }
    }
}
