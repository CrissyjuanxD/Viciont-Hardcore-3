package CorruptedEnd;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
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
    private final Map<UUID, Long> lastBiomeCheck = new HashMap<>();
    private final Set<UUID> frozenPlayers = new HashSet<>();

    public BiomeEffectManager(JavaPlugin plugin) {
        this.plugin = plugin;

        // Task para verificar efectos de bioma
        new BukkitRunnable() {
            @Override
            public void run() {
                checkBiomeEffects();
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!player.getWorld().getName().equals(CorruptedEnd.WORLD_NAME)) return;

        // Verificar cambio de bloque para optimizar
        if (event.getFrom().getBlock().equals(event.getTo().getBlock())) return;

        // Limitar verificaciones muy frecuentes
        long currentTime = System.currentTimeMillis();
        Long lastCheck = lastBiomeCheck.get(player.getUniqueId());
        if (lastCheck != null && currentTime - lastCheck < 500) return; // 0.5 segundos

        lastBiomeCheck.put(player.getUniqueId(), currentTime);

        Location loc = event.getTo();
        BiomeType currentBiome = determineBiome(loc);
        BiomeType previousBiome = playerBiomes.get(player.getUniqueId());

        if (currentBiome != previousBiome) {
            playerBiomes.put(player.getUniqueId(), currentBiome);
            onBiomeChange(player, previousBiome, currentBiome);
        }
    }

    private void checkBiomeEffects() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.getWorld().getName().equals(CorruptedEnd.WORLD_NAME)) continue;

            BiomeType currentBiome = playerBiomes.get(player.getUniqueId());
            if (currentBiome != null) {
                applyBiomeEffects(player, currentBiome);
            }
        }
    }

    private BiomeType determineBiome(Location location) {
        // Verificar bloques en un área más amplia y a diferentes alturas
        Map<Material, Integer> blockCounts = new HashMap<>();

        for (int dx = -5; dx <= 5; dx++) {
            for (int dz = -5; dz <= 5; dz++) {
                for (int dy = -10; dy <= 10; dy++) { // Verificar más alturas
                    Location checkLoc = location.clone().add(dx, dy, dz);
                    if (checkLoc.getY() < 0 || checkLoc.getY() > 255) continue;

                    Material blockType = checkLoc.getBlock().getType();
                    blockCounts.put(blockType, blockCounts.getOrDefault(blockType, 0) + 1);
                }
            }
        }

        // Determinar bioma basado en bloques predominantes
        if (blockCounts.getOrDefault(Material.BLUE_TERRACOTTA, 0) > 8) {
            return BiomeType.CELESTIAL_FOREST;
        } else if (blockCounts.getOrDefault(Material.OBSIDIAN, 0) > 12) {
            return BiomeType.OBSIDIAN_PEAKS;
        } else if (blockCounts.getOrDefault(Material.CRIMSON_HYPHAE, 0) > 8) {
            return BiomeType.CRIMSON_WASTES;
        } else {
            return BiomeType.SCULK_PLAINS;
        }
    }

    private void onBiomeChange(Player player, BiomeType from, BiomeType to) {
        // Remover efectos del bioma anterior
        if (from != null) {
            removeBiomeEffects(player, from);
        }

        // Mensaje de cambio de bioma
        player.sendMessage(ChatColor.GRAY + "Entrando a " + ChatColor.AQUA + to.getName());

        // Sonido de cambio de bioma
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
                // Efecto de congelación infinita
                if (!frozenPlayers.contains(player.getUniqueId())) {
                    frozenPlayers.add(player.getUniqueId());

                    player.setFreezeTicks(Integer.MAX_VALUE);
                }

                // Aplicar náusea
                PotionEffect nausea = new PotionEffect(PotionEffectType.NAUSEA, 300, 0, false, false, false);
                if (!player.hasPotionEffect(PotionEffectType.NAUSEA)) {
                    player.addPotionEffect(nausea);
                }

                // Mensaje y partículas ocasionales
                if (System.currentTimeMillis() % 3000 < 1000) {
                    player.sendMessage(ChatColor.BLUE + "El frío celestial te congela...");

                    // Partículas de hielo
                    player.getWorld().spawnParticle(Particle.SNOWFLAKE,
                            player.getLocation().add(0, 1, 0), 5, 0.5, 0.5, 0.5);
                }
                break;

            case OBSIDIAN_PEAKS:
                // Efecto de oscuridad infinita
                PotionEffect darkness = new PotionEffect(PotionEffectType.DARKNESS, Integer.MAX_VALUE, 0, false, false, false);
                if (!player.hasPotionEffect(PotionEffectType.DARKNESS)) {
                    player.addPotionEffect(darkness);
                }

                // Partículas de oscuridad
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
                // Remover congelación al salir del bioma
                if (frozenPlayers.contains(player.getUniqueId())) {
                    frozenPlayers.remove(player.getUniqueId());
                    player.setFreezeTicks(0); // Quitar congelación
                    player.sendMessage(ChatColor.GREEN + "Ya no sientes el frío celestial.");
                }
                break;
            case OBSIDIAN_PEAKS:
                // Remover efecto de oscuridad
                player.removePotionEffect(PotionEffectType.DARKNESS);
                player.sendMessage(ChatColor.YELLOW + "La oscuridad se desvanece...");
                break;
        }
    }

    // Método para limpiar jugadores que se desconectan
    public void cleanup() {
        playerBiomes.clear();
        lastBiomeCheck.clear();
        frozenPlayers.clear();
    }
}