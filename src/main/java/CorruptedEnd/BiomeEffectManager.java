package CorruptedEnd;

import org.bukkit.*;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Set;
import java.util.HashSet;

public class BiomeEffectManager implements Listener {
    private final JavaPlugin plugin;
    private final Map<UUID, BiomeType> playerBiomes = new HashMap<>();
    private final Set<UUID> frozenPlayers = new HashSet<>();

    public BiomeEffectManager(JavaPlugin plugin) {
        this.plugin = plugin;
        new BukkitRunnable() {
            @Override
            public void run() {
                checkBiomeEffects();
            }
        }.runTaskTimer(plugin, 0L, 40L);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() &&
                event.getFrom().getBlockY() == event.getTo().getBlockY() &&
                event.getFrom().getBlockZ() == event.getTo().getBlockZ()) return;

        Player player = event.getPlayer();
        if (!player.getWorld().getName().equals(CorruptedEnd.WORLD_NAME)) return;

        Biome customBiome = player.getWorld().getBiome(player.getLocation());
        BiomeType currentBiome = determineBiomeFromCustom(customBiome);

        BiomeType previousBiome = playerBiomes.get(player.getUniqueId());

        if (currentBiome != previousBiome) {
            playerBiomes.put(player.getUniqueId(), currentBiome);
            onBiomeChange(player, previousBiome, currentBiome);
        }
    }

    private BiomeType determineBiomeFromCustom(Biome customBiome) {
        if (customBiome == null || customBiome.getKey() == null) return BiomeType.SCULK_PLAINS;

        String key = customBiome.getKey().getKey(); // Retorna "celestial_forest" si el datapack funcionó
        try {
            return BiomeType.valueOf(key.toUpperCase());
        } catch (IllegalArgumentException e) {
            return BiomeType.SCULK_PLAINS;
        }
    }

    private void checkBiomeEffects() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.getWorld().getName().equals(CorruptedEnd.WORLD_NAME)) continue;
            BiomeType currentBiome = playerBiomes.get(player.getUniqueId());
            if (currentBiome != null) applyBiomeEffects(player, currentBiome);
        }
    }

    private void onBiomeChange(Player player, BiomeType from, BiomeType to) {
        if (from != null) removeBiomeEffects(player, from);
        player.sendMessage(ChatColor.GRAY + "Entrando a " + ChatColor.AQUA + to.getName());

        switch (to) {
            case CELESTIAL_FOREST:
                player.playSound(player.getLocation(), Sound.BLOCK_BEACON_AMBIENT, 0.5f, 1.5f);
                break;
            case OBSIDIAN_PEAKS:
                player.playSound(player.getLocation(), Sound.AMBIENT_CAVE, 0.8f, 0.5f);
                break;
            case CRIMSON_WASTES:
                player.playSound(player.getLocation(), Sound.AMBIENT_CRIMSON_FOREST_MOOD, 0.6f, 1.0f);
                break;
            default:
                player.playSound(player.getLocation(), Sound.AMBIENT_WARPED_FOREST_MOOD, 0.4f, 1.0f);
        }
    }

    private void applyBiomeEffects(Player player, BiomeType biome) {
        switch (biome) {
            case CELESTIAL_FOREST:
                if (!frozenPlayers.contains(player.getUniqueId())) {
                    frozenPlayers.add(player.getUniqueId());
                    player.setFreezeTicks(Integer.MAX_VALUE);
                }
                if (!player.hasPotionEffect(PotionEffectType.NAUSEA)) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 300, 0, false, false, false));
                }
                if (System.currentTimeMillis() % 3000 < 1000) {
                    player.getWorld().spawnParticle(Particle.SNOWFLAKE,
                            player.getLocation().add(0, 1, 0), 5, 0.5, 0.5, 0.5);
                }
                break;
            case OBSIDIAN_PEAKS:
                if (!player.hasPotionEffect(PotionEffectType.DARKNESS)) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 30, 0, false, false, false));
                }
                if (System.currentTimeMillis() % 2000 < 1000) {
                    player.getWorld().spawnParticle(Particle.SQUID_INK,
                            player.getLocation().add(0, 1, 0), 3, 0.3, 0.3, 0.3);
                }
                break;
        }
    }

    private void removeBiomeEffects(Player player, BiomeType biome) {
        switch (biome) {
            case CELESTIAL_FOREST:
                if (frozenPlayers.contains(player.getUniqueId())) {
                    frozenPlayers.remove(player.getUniqueId());
                    player.setFreezeTicks(0);
                }
                break;
            case OBSIDIAN_PEAKS:
                player.removePotionEffect(PotionEffectType.DARKNESS);
                break;
        }
    }

    private void cleanPlayerEffects(Player player) {
        UUID uuid = player.getUniqueId();
        playerBiomes.remove(uuid);
        frozenPlayers.remove(uuid);
        player.setFreezeTicks(0);
        player.removePotionEffect(PotionEffectType.NAUSEA);
        player.removePotionEffect(PotionEffectType.DARKNESS);
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        if (event.getFrom().getName().equals(CorruptedEnd.WORLD_NAME)) {
            cleanPlayerEffects(event.getPlayer());
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        cleanPlayerEffects(event.getPlayer());
    }
}