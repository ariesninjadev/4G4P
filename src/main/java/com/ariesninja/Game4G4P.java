package com.ariesninja;

import com.ariesninja.listeners.PlayerEventListener;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.*;

import java.util.*;

public final class Game4G4P extends JavaPlugin {

    private ScoreboardManager manager;
    private Scoreboard scoreboard;
    private Objective objective;
    private final Map<Player, Integer> winCountMap = new HashMap<>();

    private long itemGiveCooldown = 100L; // Default to 100 ticks (5 seconds)

    private BukkitTask itemTask;  // To track the scheduled task

    @Override
    public void onEnable() {
        // Register the listeners
        getServer().getPluginManager().registerEvents(new PlayerEventListener(), this);

        // Initialize scoreboard
        manager = Bukkit.getScoreboardManager();
        scoreboard = manager.getNewScoreboard();
        objective = scoreboard.registerNewObjective("wins", "dummy", "Player Wins");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        // Register the commands
        getCommand("startround").setExecutor((sender, command, label, args) -> {

            setScoreboardForPlayers();

            if (itemTask == null) {
                // Start the countdown before starting the game
                new BukkitRunnable() {
                    int countdown = 3; // Start countdown from 3

                    @Override
                    public void run() {
                        if (countdown > 0) {
                            // Send countdown message to all players
                            for (Player player : Bukkit.getOnlinePlayers()) {
                                player.sendMessage("§eGame starts in " + countdown + "...");
                            }
                            countdown--;
                        } else {
                            // Start the game once countdown is finished
                            startGivingItems();
                            for (Player player : Bukkit.getOnlinePlayers()) {
                                player.sendMessage("§aGame started!");
                            }
                            cancel(); // Stop the countdown task
                        }
                    }
                }.runTaskTimer(this, 0L, 20L); // 20 ticks = 1 second
            } else {
                sender.sendMessage("Game is already running!");
            }
            return true;
        });


        getCommand("stopround").setExecutor((sender, command, label, args) -> {
            if (itemTask != null) {
                stopGivingItems();
                sender.sendMessage("Game stopped!");
            } else {
                sender.sendMessage("No game is currently running!");
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

        getCommand("resetscores").setExecutor((sender, command, label, args) -> {
            resetWinCounts();
            sender.sendMessage("Success!");
            return true;
        });

    }

    @Override
    public void onDisable() {
        // Cancel the task if the plugin is disabled
        stopGivingItems();
    }

    public void resetWinCounts() {
        winCountMap.clear();  // Clear the win count map
        scoreboard.clearSlot(DisplaySlot.SIDEBAR);  // Clear current scores from the sidebar

        // Rebuild the scoreboard and re-assign it to all players
        for (Player player : Bukkit.getOnlinePlayers()) {
            updateScoreboard(player, 0);  // Reset each player's scoreboard to 0 wins
        }
    }


    public void addWin(Player player) {
        int wins = winCountMap.getOrDefault(player, 0) + 1;
        winCountMap.put(player, wins);
        updateScoreboard(player, wins);
    }

    public void updateScoreboard(Player player, int wins) {
        Score score = objective.getScore(player.getName());
        score.setScore(wins); // Update the player's score with their win count
    }

    public void setScoreboardForPlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.setScoreboard(scoreboard);
        }
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

        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (!(entity instanceof Player)) {
                    entity.remove(); // Kills/removes the entity
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

            // Reset player's health and hunger
            player.setHealth(player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue()); // Reset to max health
            player.setFoodLevel(20); // Max hunger level
            player.setSaturation(20.0f); // Full saturation

            // Teleport the player to their assigned location
            if (i < locations.length) {
                player.teleport(locations[i]);
            }
        }


        // Start the repeating task to give random items
        itemTask = Bukkit.getScheduler().runTaskTimer(this, () -> {
            // List to keep track of players in Survival mode
            List<Player> survivalPlayers = new ArrayList<>();

            // Iterate through all online players
            for (Player player : Bukkit.getOnlinePlayers()) {
                // Only give items to players in Survival mode
                if (player.getGameMode() == GameMode.SURVIVAL) {
                    survivalPlayers.add(player); // Add to list of survival players

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
            }

            // Check if only one player is left in Survival mode
            if (survivalPlayers.size() == 1) {
                // Award a win to the last surviving player
                Player winner = survivalPlayers.get(0);
                addWin(winner);  // Add win count for the player

                // Send a message to all players announcing the winner
                String winnerMessage = "§a" + winner.getName() + " won the round!";
                for (Player player : Bukkit.getOnlinePlayers()) {
                    player.sendMessage(winnerMessage);
                }

                // Stop the round and set everyone to Spectator mode
                stopGivingItems();
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
