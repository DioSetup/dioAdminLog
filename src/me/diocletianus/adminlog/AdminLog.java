package me.diocletianus.adminlog;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

public class AdminLog extends JavaPlugin implements Listener, CommandExecutor {
    private FileConfiguration config;
    private Map<String, BufferedWriter> logWriters;

    @Override
    public void onEnable() {
        loadConfig();
        logWriters = new HashMap<>();
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("adminlog").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("adminlog")) {
            if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
                reloadConfig();
                sender.sendMessage("Config reloaded.");
                return true;
            }
        }
        return false;
    }

    @EventHandler
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        String playerName = event.getPlayer().getName();
        List<String> loggedPlayers = config.getStringList("logged_players");

        if (loggedPlayers.contains(playerName)) {
            String command = event.getMessage();
            logToFile(playerName, "executed command: " + command);
        }
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        String playerName = event.getPlayer().getName();
        List<String> loggedPlayers = config.getStringList("logged_players");

        if (loggedPlayers.contains(playerName)) {
            ItemStack droppedItem = event.getItemDrop().getItemStack();
            String item = droppedItem.getType().toString();
            int amount = droppedItem.getAmount();
            logToFile(playerName, "dropped " + amount + " " + item);
        }
    }

    private void loadConfig() {
        getConfig().options().copyDefaults(true);
        saveDefaultConfig();
        config = getConfig();
    }

    private void logToFile(String playerName, String message) {
        BufferedWriter writer = logWriters.get(playerName);

        if (writer == null) {
            try {
                String logFileName = playerName.toLowerCase() + ".txt";
                File logFile = new File(getDataFolder(), logFileName);
                if (!logFile.exists()) {
                    logFile.createNewFile();
                }
                writer = new BufferedWriter(new FileWriter(logFile, true));
                logWriters.put(playerName, writer);
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        }

        try {
            String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            writer.write("[" + timestamp + "] " + message);
            writer.newLine();
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDisable() {
        for (BufferedWriter writer : logWriters.values()) {
            try {
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        logWriters.clear();
    }
}