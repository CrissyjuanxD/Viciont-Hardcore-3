package mobcap.optimization;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import mobcap.config.MobCapConfig;

import java.util.concurrent.atomic.AtomicInteger;

public class MobCapOptimizer {
    private final JavaPlugin plugin;
    private final MobCapConfig config;

    public MobCapOptimizer(JavaPlugin plugin, MobCapConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    public int getOptimizedMobCap(int originalMobCap) {
        int playerCount = Bukkit.getOnlinePlayers().size();

        if (playerCount <= config.getOptimizationPlayerThreshold() ||
                originalMobCap < config.getOptimizationMobCapThreshold()) {
            return originalMobCap;
        }

        // Calcular factor de optimización basado en número de jugadores
        double optimizationFactor = calculateOptimizationFactor(playerCount);
        int optimizedCap = (int) (originalMobCap * optimizationFactor);

        // Asegurar un mínimo razonable
        int minimumCap = Math.max(70, (int) (originalMobCap * config.getOptimizationMinimumPercentage()));
        optimizedCap = Math.max(optimizedCap, minimumCap);

        plugin.getLogger().info(String.format(
                "Optimizing mob cap: %d -> %d (players: %d, factor: %.2f)",
                originalMobCap, optimizedCap, playerCount, optimizationFactor
        ));

        return optimizedCap;
    }

    private double calculateOptimizationFactor(int playerCount) {
        int threshold = config.getOptimizationPlayerThreshold();
        if (playerCount <= threshold) {
            return 1.0;
        }

        // Reducción gradual basada en configuración
        double reductionFactor = config.getOptimizationReductionFactor();
        double minimumPercentage = config.getOptimizationMinimumPercentage();

        // Calcular reducción progresiva
        int excessPlayers = playerCount - threshold;
        double reductionPerPlayer = (1.0 - minimumPercentage) / 40.0; // Reducción gradual hasta 40 jugadores extra
        double reduction = Math.min(1.0 - minimumPercentage, excessPlayers * reductionPerPlayer);

        return Math.max(minimumPercentage, 1.0 - reduction);
    }

    public void optimizeMobCap() {
        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                int totalMobs = getTotalMobCount();
                int playerCount = Bukkit.getOnlinePlayers().size();

                // Si hay demasiados mobs por jugador, aplicar limpieza
                double mobsPerPlayer = playerCount > 0 ? (double) totalMobs / playerCount : totalMobs;

                if (mobsPerPlayer > 15) {
                    performMobCleanup();
                }

            } catch (Exception e) {
                plugin.getLogger().warning("Error during mob cap optimization: " + e.getMessage());
            }
        });
    }

    private int getTotalMobCount() {
        AtomicInteger totalMobs = new AtomicInteger(0);

        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity instanceof Monster) {
                    totalMobs.incrementAndGet();
                }
            }
        }

        return totalMobs.get();
    }

    private void performMobCleanup() {
        int removedMobs = 0;
        int maxCleanup = config.getOptimizationMaxCleanupPerCycle();
        int cleanupDistance = config.getOptimizationCleanupDistance();

        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity instanceof Monster &&
                        !(entity instanceof LivingEntity && ((LivingEntity) entity).getCustomName() != null)) {

                    // Solo remover mobs que estén lejos de jugadores
                    boolean farFromPlayers = true;
                    for (Player player : world.getPlayers()) {
                        if (entity.getLocation().distance(player.getLocation()) < cleanupDistance) {
                            farFromPlayers = false;
                            break;
                        }
                    }

                    if (farFromPlayers) {
                        entity.remove();
                        removedMobs++;

                        if (removedMobs >= maxCleanup) {
                            break;
                        }
                    }
                }
            }

            if (removedMobs >= maxCleanup) {
                break;
            }
        }

        if (removedMobs > 0) {
            plugin.getLogger().info("Mob cleanup: removed " + removedMobs + " distant mobs");
        }
    }
}