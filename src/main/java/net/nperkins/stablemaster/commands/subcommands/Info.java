package net.nperkins.stablemaster.commands.subcommands;

import com.google.common.base.Joiner;
import net.nperkins.stablemaster.LangString;
import net.nperkins.stablemaster.StableMaster;
import net.nperkins.stablemaster.commands.CommandInfo;
import net.nperkins.stablemaster.commands.SubCommand;
import net.nperkins.stablemaster.data.Stable;
import net.nperkins.stablemaster.data.StabledHorse;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.*;

import java.util.ArrayList;
import java.util.List;

public class Info extends SubCommand {

    public Info() {
        setOwnerRequired(false);
        setName("info");
    }

    public void handle(CommandInfo commandInfo) {
        final Player player = (Player) commandInfo.getSender();

        StableMaster.commandQueue.put(player, this);
        new LangString("punch-animal").send(player);
    }

    public void handleInteract(Stable stable, Player player, Tameable animal) {
        final Animals a = (Animals) animal;

        List<String> riderNames = new ArrayList<>();
        boolean isHorse = false;
        if (animal instanceof AbstractHorse) {
            isHorse = true;
            StabledHorse stabledHorse = stable.getHorse((AbstractHorse) animal);
            riderNames = stabledHorse.getRiderNames();
        }

        // Get permission level of user to compare with those in the config.
        ConfigurationSection config = StableMaster.getPlugin().getConfig().getConfigurationSection("command.info");
        Integer permissionLevel;
        if (player == animal.getOwner())
            permissionLevel = 1;
        else if (riderNames.contains(player.getName()))
            permissionLevel = 2;
        else
            permissionLevel = 3;

        // Get the min level for players with the bypass permission.
        if (canBypass(player) && config.getInt("bypass-as-level") < permissionLevel)
            permissionLevel = config.getInt("bypass-as-level");

        // Print the info
        new LangString("command.info.header").send(player);

        // Owner of the animal
        if (config.getInt("owner") >= permissionLevel) {
            new LangString("command.info.owner", animal.getOwner().getName()).send(player);
        }

        // Players allowed to ride the horse
        if (isHorse && config.getInt("permitted-riders") >= permissionLevel) {
            String permitted = riderNames.isEmpty() ? new LangString("command.info.no-riders").getMessage() : Joiner.on(", ").join(riderNames);
            new LangString("command.info.permitted-riders", permitted).send(player);
        }

        // Current and maximum health
        if (config.getInt("health") >= permissionLevel) {
            Double hearts = a.getHealth() / 2;
            Double maxHearts = a.getMaxHealth() / 2;
            new LangString("command.info.health", hearts, maxHearts).send(player);
        }

        // Jump strength
        if (isHorse && config.getInt("jump-strength") >= permissionLevel) {
            Double str = ((AbstractHorse) animal).getJumpStrength();
            Double height = -0.1817584952 * str*str*str + 3.689713992 * str*str + 2.128599134 * str - 0.343930367;
            new LangString("command.info.jump-strength", str, height).send(player);
        }

        // Max Speed
        if (config.getInt("max-speed") >= permissionLevel) {
            Double speed = a.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).getValue();
            Double blocksPerSecond = speed * 43.1;
            new LangString("command.info.max-speed", speed, blocksPerSecond).send(player);
        }

        // What type of animal it is
        if (config.getInt("variant") >= permissionLevel) {
            String variant;
            switch (a.getType()) {
                case HORSE:
                    Horse h = (Horse) animal;
                    variant = h.getColor() + ", " + h.getStyle();
                    break;
                case OCELOT:
                    variant = ((Ocelot) animal).getCatType().toString();
                    break;
                case PARROT:
                    variant = ((Parrot) animal).getVariant().toString();
                    break;
                default:
                    variant = a.getType().toString();
                    break;
            }
            new LangString("command.info.variant", variant).send(player);
        }
    }
}
