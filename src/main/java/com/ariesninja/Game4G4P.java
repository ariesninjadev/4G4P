package com.ariesninja;

import com.ariesninja.listeners.PlayerEventListener;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

public final class Game4G4P extends JavaPlugin {

    private long itemGiveCooldown = 100L; // Default to 100 ticks (5 seconds)

    private BukkitTask itemTask;  // To track the scheduled task

    @Override
    public void onEnable() {
        // Register the listeners
        getServer().getPluginManager().registerEvents(new PlayerEventListener(), this);
        // Register the commands
        getCommand("startitems").setExecutor((sender, command, label, args) -> {
            if (itemTask == null) {
                startGivingItems();
                sender.sendMessage("Item giving task started!");
            } else {
                sender.sendMessage("Item giving task is already running!");
            }
            return true;
        });

        getCommand("stopitems").setExecutor((sender, command, label, args) -> {
            if (itemTask != null) {
                stopGivingItems();
                sender.sendMessage("Item giving task stopped!");
            } else {
                sender.sendMessage("No item giving task is currently running!");
            }
            return true;
        });

        getCommand("setitemcooldown").setExecutor((sender, command, label, args) -> {
            if (args.length != 1) {
                sender.sendMessage("Usage: /setitemcooldown <ticks>");
                return true;
            }

            try {
                long newCooldown = Long.parseLong(args[0]);
                if (newCooldown < 0) {
                    sender.sendMessage("Cooldown cannot be negative.");
                    return true;
                }

                // Update the cooldown
                itemGiveCooldown = newCooldown;
                sender.sendMessage("Item give cooldown set to " + newCooldown + " ticks.");
            } catch (NumberFormatException e) {
                sender.sendMessage("Please enter a valid number.");
            }

            return true;
        });

    }

    @Override
    public void onDisable() {
        // Cancel the task if the plugin is disabled
        stopGivingItems();
    }

    // Start the task to give items every 5 seconds
    private void startGivingItems() {
        // Check if the itemTask is already running to prevent multiple calls
        if (itemTask != null) {
            return; // Exit if the task is already running
        }

        // Define the corners of the area to clear
        int minX = -125;
        int minY = 0;
        int minZ = -80;
        int maxX = -25;
        int maxY = 260;
        int maxZ = 25;

        // Remove blocks in the specified area, excluding Bedrock
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Location location = new Location(Bukkit.getWorld("world"), x, y, z);
                    Material blockType = location.getBlock().getType();

                    // Set the block to air if it's not Bedrock
                    if (blockType != Material.BEDROCK) {
                        location.getBlock().setType(Material.AIR);
                    }
                }
            }
        }

        // Define the unique spawn locations
        Location[] locations = {
                new Location(Bukkit.getWorld("world"), -90.5, 135, -42.5), // Location 1
                new Location(Bukkit.getWorld("world"), -75.5, 135, -42.5), // Location 2
                new Location(Bukkit.getWorld("world"), -75.5, 135, -27.5), // Location 3
                new Location(Bukkit.getWorld("world"), -90.5, 135, -27.5)  // Location 4
        };

        // Clear inventories and teleport players to unique locations when the command is first run
        Player[] players = Bukkit.getOnlinePlayers().toArray(new Player[0]);
        for (int i = 0; i < players.length; i++) {
            Player player = players[i];

            // Clear player's inventory
            player.getInventory().clear();

            // Set player's game mode to Survival
            player.setGameMode(GameMode.SURVIVAL);

            // Teleport the player to their assigned location
            if (i < locations.length) {
                player.teleport(locations[i]);
            }
        }


        // Start the repeating task to give random items
        itemTask = Bukkit.getScheduler().runTaskTimer(this, () -> {
            // Iterate through all online players
            for (Player player : Bukkit.getOnlinePlayers()) {
                // Get a random item for the current player
                ItemStack randomItem = getRandomItem();

                // Check if the player's inventory has space for the item
                if (player.getInventory().firstEmpty() == -1) {
                    // Inventory is full, send a message in red
                    player.sendMessage("§cYour inventory is full!"); // §c is the color code for red
                } else {
                    // Give the random item to the player
                    player.getInventory().addItem(randomItem);
                    player.sendMessage("You received a " + randomItem.getType());
                }
            }
        }, 0L, itemGiveCooldown);
    }


    private void stopGivingItems() {
        // Set all online players to Spectator mode
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.setGameMode(GameMode.SPECTATOR);
        }

        // Stop the task
        if (itemTask != null) {
            itemTask.cancel();
            itemTask = null;
        }
    }


    // Generate a random item
    private ItemStack getRandomItem() {
        List<Material> materials = Arrays.asList(Material.values());
        Random random = new Random();
        Material randomMaterial;

        do {
            randomMaterial = materials.get(random.nextInt(materials.size()));
        } while (!randomMaterial.isItem() || randomMaterial == Material.BEDROCK ||
                (randomMaterial.toString().endsWith("_SPAWN_EGG") && random.nextInt(100) < 80));

        return new ItemStack(randomMaterial);
    }

}
