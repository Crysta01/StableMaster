package net.nperkins.stablemaster;

import net.nperkins.stablemaster.commands.CoreCommand;
import net.nperkins.stablemaster.data.Stable;
import net.nperkins.stablemaster.data.StabledHorse;
import net.nperkins.stablemaster.listeners.*;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class StableMaster extends JavaPlugin {

    public static List<Chunk> horseChunk = new ArrayList<>();

    public static ConcurrentHashMap<Player, OfflinePlayer> addRiderQueue = new ConcurrentHashMap<Player, OfflinePlayer>();
    public static ConcurrentHashMap<Player, OfflinePlayer> delRiderQueue = new ConcurrentHashMap<Player, OfflinePlayer>();
    public static ConcurrentHashMap<Player, OfflinePlayer> giveQueue = new ConcurrentHashMap<Player, OfflinePlayer>();
    public static ConcurrentHashMap<Player, String> renameQueue = new ConcurrentHashMap<Player, String>();
    public static ConcurrentHashMap<Player, Object> TeleportQueue = new ConcurrentHashMap<Player, Object>();
    public static ArrayList<Player> infoQueue = new ArrayList<Player>();

    private static Plugin plugin;
    private static Configuration configuration;
    private static File dataFolder;
    private static File pluginFolder;
    private static HashMap<OfflinePlayer, Stable> stables = new HashMap<OfflinePlayer, Stable>();


    public static String playerMessage(String msg) {
        String prefix = configuration.getLang().getString("prefix");
        return (ChatColor.translateAlternateColorCodes('&', prefix + msg));
    }

    public static String langMessage(String key) {
        String prefix = configuration.getLang().getString("prefix");
        return (ChatColor.translateAlternateColorCodes('&', prefix + configuration.getLang().getString(key)));
    }

    @Override
    public void onEnable() {
        plugin = this;

        // Create configuration instance
        configuration = new Configuration();
        configuration.createAllFiles();

        pluginFolder = getDataFolder();
        dataFolder = new File(pluginFolder + File.separator + "stables");

        createDataFolders();

        // Register listeners
        getServer().getPluginManager().registerEvents(new ChunkListener(), this);
        getServer().getPluginManager().registerEvents(new EntityDamageByEntityListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerInteractEntityListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerQuitListener(), this);

        // Register commands
        this.getCommand("stablemaster").setExecutor(new CoreCommand());

        for (Player p : this.getServer().getOnlinePlayers()) {
            this.loadStable(p);
        }

    }

    @Override
    public void onDisable() {

        Iterator it = stables.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pairs = (Map.Entry) it.next();
            this.saveStable((Stable) pairs.getValue());
            it.remove();
        }
    }

    public static void loadStable(OfflinePlayer player) {
        File stableFile = new File(dataFolder + File.separator + player.getUniqueId().toString() + ".yml");
        YamlConfiguration yamlFile = YamlConfiguration.loadConfiguration(stableFile);
        Stable stable = new Stable(player);
        if (yamlFile.contains("horses")) {
            Set<String> horses = yamlFile.getConfigurationSection("horses").getKeys(false);
            HashMap<String, StabledHorse> horseMap = new HashMap<String, StabledHorse>();
            for (String s : horses) {
                StabledHorse horse = new StabledHorse();
                horse.setUniqueID(s);
                horse.setRiders(yamlFile.getStringList("horses." + s + ".riders"));
                horseMap.put(s, horse);
            }
            stable.setHorses(horseMap);
        }
        stables.put(player, stable);
    }

    public static void saveStable(Stable stable) {
        File stableFile = new File(dataFolder + File.separator + stable.getOwner() + ".yml");
        if (stable.getHorses().isEmpty()) {
            if (stableFile.exists()) {
                stableFile.delete();
            }
            return;
        }
        YamlConfiguration yamlFile = YamlConfiguration.loadConfiguration(stableFile);
        yamlFile.set("owner", stable.getOwner());
        // Clear out horses current stable dat
        if (yamlFile.contains("horses")) {
            Set<String> horses = yamlFile.getConfigurationSection("horses").getKeys(false);
            for (String s : horses) {
                if (stable.getHorses().keySet().contains(s)) {
                    yamlFile.set("horses." + s + ".riders",null);
                } else {
                    yamlFile.set("horses." + s, null);
                }
            }
        }
        // Save new stable data
        for (String horse : stable.getHorses().keySet()) {
            StabledHorse sh = stable.getHorse(horse);
            yamlFile.set("horses." + horse + ".riders", sh.getRiders());
        }
        try {
            yamlFile.save(stableFile);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static void unloadStable(OfflinePlayer player) {
        Stable s = stables.get(player);
        saveStable(s);
        stables.remove(player);
    }

    public static Stable getStable(OfflinePlayer player) {
        if (!stables.containsKey(player)) {
            loadStable(player);
        }
        return stables.get(player);
    }

    private static void createDataFolders() {
        if (!pluginFolder.exists()) {
            try {
                pluginFolder.mkdir();
            } catch (Exception e) {
                Bukkit.getServer().getLogger().log(Level.WARNING, e.getMessage());
            }
        }

        if (!dataFolder.exists()) {
            try {
                dataFolder.mkdir();
            } catch (Exception e) {
                Bukkit.getServer().getLogger().log(Level.WARNING, e.getMessage());
            }
        }
    }

    public static Plugin getPlugin() {
        return plugin;
    }

}
