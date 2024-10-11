package com.ariesninja.listeners;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerEventListener implements Listener {

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Get the player who died
        Player player = event.getPlayer();

        // Set the player's game mode to Spectator upon death
        player.setGameMode(GameMode.SPECTATOR);

        // Teleport the player to the specified location
        Location teleportLocation = new Location(player.getWorld(), -82, 160, -35);
        player.teleport(teleportLocation);
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        // Get the player who died
        Player player = event.getEntity();

        // Set the player's game mode to Spectator upon death
        player.setGameMode(GameMode.SPECTATOR);

        // Teleport the player to the specified location
        Location teleportLocation = new Location(player.getWorld(), -82, 160, -35);
        player.teleport(teleportLocation);
    }
}
