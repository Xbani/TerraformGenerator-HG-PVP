package org.terraform.command;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.terraform.command.contants.InvalidArgumentException;
import org.terraform.command.contants.TerraCommand;
import org.terraform.coregen.PopulatorDataPostGen;
import org.terraform.data.TerraformWorld;
import org.terraform.main.TerraformGeneratorPlugin;
import org.terraform.structure.village.plains.PlainsVillagePopulator;

import java.util.Random;
import java.util.Stack;

public class PlainsVillageCommand extends TerraCommand {

    public PlainsVillageCommand(TerraformGeneratorPlugin plugin, String... aliases) {
        super(plugin, aliases);
    }

    @Override
    public String getDefaultDescription() {
        return "Plains Village spawner";
    }

    @Override
    public boolean canConsoleExec() {
        return false;
    }

    @Override
    public boolean hasPermission(CommandSender sender) {

        return sender.isOp();
    }

    @Override
    public void execute(CommandSender sender, Stack<String> args)
            throws InvalidArgumentException {

        Player p = (Player) sender;
        PopulatorDataPostGen data = new PopulatorDataPostGen(p.getLocation().getChunk());
        int x = p.getLocation().getBlockX();
        int y = p.getLocation().getBlockY();
        int z = p.getLocation().getBlockZ();
        TerraformWorld tw = TerraformWorld.get(p.getWorld());
        Random random = new Random();
        new PlainsVillagePopulator().spawnPlainsVillage(tw, random, data, x, y, z);
        p.sendMessage("Complete.");
    }

}
