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
        }.runTaskTimer(plugin, 0L, 40L);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        // Optimización: Solo verificar si cambió de bloque
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() &&
                event.getFrom().getBlockY() == event.getTo().getBlockY() &&
                event.getFrom().getBlockZ() == event.getTo().getBlockZ()) return;

        Player player = event.getPlayer();
        if (!player.getWorld().getName().equals(CorruptedEnd.WORLD_NAME)) return;

        // --- OPTIMIZACIÓN MASIVA: DETECCIÓN NATIVA ---
        // Obtenemos el bioma directamente del juego (0 coste de CPU)
        Biome vanillaBiome = player.getWorld().getBiome(player.getLocation());
        BiomeType currentBiome = determineBiomeFromVanilla(vanillaBiome);

        BiomeType previousBiome = playerBiomes.get(player.getUniqueId());

        // Si entramos a un bioma nuevo
        if (currentBiome != previousBiome) {
            playerBiomes.put(player.getUniqueId(), currentBiome);
            onBiomeChange(player, previousBiome, currentBiome);
        }
    }

    private BiomeType determineBiomeFromVanilla(Biome vanillaBiome) {
        switch (vanillaBiome) {
            case END_MIDLANDS:
                return BiomeType.CELESTIAL_FOREST;

            case SOUL_SAND_VALLEY:
                return BiomeType.OBSIDIAN_PEAKS;

            case END_HIGHLANDS:
                return BiomeType.CRIMSON_WASTES;

            case END_BARRENS:
            default:
                return BiomeType.SCULK_PLAINS;
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

                    // Partículas de hielo
                    player.getWorld().spawnParticle(Particle.SNOWFLAKE,
                            player.getLocation().add(0, 1, 0), 5, 0.5, 0.5, 0.5);
                }
                break;

            case OBSIDIAN_PEAKS:
                // Efecto de oscuridad infinita
                PotionEffect darkness = new PotionEffect(PotionEffectType.DARKNESS, 30, 0, false, false, false);
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
                }
                break;
            case OBSIDIAN_PEAKS:
                // Remover efecto de oscuridad
                player.removePotionEffect(PotionEffectType.DARKNESS);
                break;
        }
    }

    private void cleanPlayerEffects(Player player) {
        UUID uuid = player.getUniqueId();

        playerBiomes.remove(uuid);
        frozenPlayers.remove(uuid);

        // Limpiar efectos visuales y de poción
        player.setFreezeTicks(0);

        // Solo quitamos los efectos específicos de la dimensión para no borrar otros buffs
        if (player.hasPotionEffect(PotionEffectType.NAUSEA)) {
            player.removePotionEffect(PotionEffectType.NAUSEA);
        }
        if (player.hasPotionEffect(PotionEffectType.DARKNESS)) {
            player.removePotionEffect(PotionEffectType.DARKNESS);
        }
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        // Si venía del Corrupted End y se fue a otro lado...
        if (event.getFrom().getName().equals(CorruptedEnd.WORLD_NAME)) {
            cleanPlayerEffects(event.getPlayer());
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        cleanPlayerEffects(event.getPlayer());
    }

    public void cleanup() {
        playerBiomes.clear();
        frozenPlayers.clear();
    }
}