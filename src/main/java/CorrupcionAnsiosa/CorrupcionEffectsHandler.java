package CorrupcionAnsiosa;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

public class CorrupcionEffectsHandler {
    private final Plugin plugin;
    private final CorrupcionAnsiosaManager corruptionManager;

    public CorrupcionEffectsHandler(Plugin plugin, CorrupcionAnsiosaManager corruptionManager) {
        this.plugin = plugin;
        this.corruptionManager = corruptionManager;
    }

    public void applyCorruptionEffects(Player player, int currentDay) {
        if (!corruptionManager.isEnabled()) return;

        PlayerCorruptionData data = corruptionManager.getPlayerData(player);
        double corruption = data.getCorruption();
        int level = data.getCorruptionLevel(currentDay);

        player.removePotionEffect(PotionEffectType.SLOWNESS);
        player.removePotionEffect(PotionEffectType.POISON);
        player.removePotionEffect(PotionEffectType.WEAKNESS);
        player.removePotionEffect(PotionEffectType.HUNGER);
        player.removePotionEffect(PotionEffectType.DARKNESS);

        if (level == 1) {
            applyLevel1Effects(player, corruption);
        } else if (level == 2) {
            applyLevel2Effects(player, corruption);
        } else if (level == 3) {
            applyLevel3Effects(player, corruption);
        }
    }

    private void applyLevel1Effects(Player player, double corruption) {
        if (corruption < 90) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 400, 1)); // 20 segundos
            player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 400, 0));
            player.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 400, 1));
        }
        if (corruption < 80) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 600, 0)); // 30 segundos
        }
        if (corruption < 70) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 800, 1)); // 40 segundos
            player.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, 800, 3));
        }
        if (corruption < 60) {
            // Efecto de no romper/colocar bloques se maneja en eventos
        }
        if (corruption < 50) {
            startHealthDrain(player, 2.0, 40); // 2 corazones cada 2 segundos
        }
    }

    private void applyLevel2Effects(Player player, double corruption) {
        if (corruption < 90) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 800, 3));
            player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 800, 0));
            player.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 800, 4));
        }
        if (corruption < 80) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 1600, 4));
            player.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, 1600, 9));
        }
        if (corruption < 70) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 1200, 0));
        }
        if (corruption < 60) {
            // Efecto de no romper/colocar bloques
        }
        if (corruption < 50) {
            startHealthDrain(player, 6.0, 40); // 6 corazones cada 2 segundos
        }
    }

    private void applyLevel3Effects(Player player, double corruption) {
        if (corruption < 90) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 1400, 9));
            player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 1400, 0));
            player.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 1400, 19));
        }
        if (corruption < 80) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 2400, 9));
            player.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, 2400, 9));
        }
        if (corruption < 70) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 4800, 0));
        }
        if (corruption < 60) {
            // Efecto de no romper/colocar bloques
        }
        if (corruption < 50) {
            // Consumir totems cada 5 segundos se maneja en el totem handler
        }
    }

    private void startHealthDrain(Player player, double hearts, int durationSeconds) {
        new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = durationSeconds * 10; // Cada 2 segundos = 40 ticks

            @Override
            public void run() {
                if (ticks >= maxTicks || !player.isOnline() || player.isDead()) {
                    this.cancel();
                    return;
                }

                if (ticks % 40 == 0) { // Cada 2 segundos (40 ticks)
                    double damage = hearts;
                    if (player.getHealth() > damage) {
                        player.damage(damage);
                    }
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
}