package com.ariesninja;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

public final class Game4G4P extends JavaPlugin {

    private BukkitTask itemTask;  // To track the scheduled task

    @Override
    public void onEnable() {
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
    }

    @Override
    public void onDisable() {
        // Cancel the task if the plugin is disabled
        stopGivingItems();
    }

    // Start the task to give items every 5 seconds
    private void startGivingItems() {
        itemTask = Bukkit.getScheduler().runTaskTimer(this, () -> {
            // Get a random item and give it to all players
            ItemStack randomItem = getRandomItem();
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.getInventory().addItem(randomItem);
                player.sendMessage("You received a " + randomItem.getType().toString());
            }
        }, 0L, 100L);  // 100 ticks = 5 seconds
    }

    // Stop the task
    private void stopGivingItems() {
        if (itemTask != null) {
            itemTask.cancel();
            itemTask = null;
        }
    }

    // Generate a random item
    private ItemStack getRandomItem() {
        // Get all possible materials (items)
        List<Material> materials = Arrays.asList(Material.values());
        Random random = new Random();
        Material randomMaterial;

        // Keep selecting items until a valid item (not AIR) is found
        do {
            randomMaterial = materials.get(random.nextInt(materials.size()));
        } while (!randomMaterial.isItem());

        return new ItemStack(randomMaterial);
    }
}
