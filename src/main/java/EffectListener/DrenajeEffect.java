package EffectListener;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class DrenajeEffect implements CustomEffect, Listener {

    private final Plugin plugin;
    private final Map<UUID, BukkitRunnable> activeEffects = new HashMap<>();
    private final Set<UUID> playersWithEffect = new HashSet<>();

    private final Set<UUID> pendingCorruptionDeath = new HashSet<>();

    private static final int DAMAGE_INTERVAL_TICKS = 40; // cada 2 segundos
    private static final double HALF_HEART_HP = 1.0D;    // medio corazón = 1 HP

    public DrenajeEffect(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void applyEffect(Player player, int durationSeconds, int amplifier) {
        removeEffect(player);

        UUID id = player.getUniqueId();
        int safeAmplifier = Math.max(0, amplifier);
        double damagePerTick = HALF_HEART_HP * (safeAmplifier + 1);

        BukkitRunnable task = new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = durationSeconds * 20;

            @Override
            public void run() {
                if (ticks >= maxTicks || !player.isOnline() || !player.hasPotionEffect(getTriggerEffectType())) {
                    removeEffect(player);
                    return;
                }

                if (ticks % DAMAGE_INTERVAL_TICKS == 0) {
                    drainTick(player, damagePerTick);
                }

                ticks++;
            }
        };

        task.runTaskTimer(plugin, 0L, 1L);
        activeEffects.put(id, task);
        playersWithEffect.add(id);
    }

    @Override
    public void removeEffect(Player player) {
        UUID id = player.getUniqueId();
        BukkitRunnable task = activeEffects.get(id);

        if (task != null) {
            task.cancel();
            activeEffects.remove(id);
        }
        playersWithEffect.remove(id);
    }

    @Override
    public PotionEffectType getTriggerEffectType() {
        return PotionEffectType.OOZING;
    }

    @Override
    public boolean isEffectActive(Player player) {
        return playersWithEffect.contains(player.getUniqueId());
    }

    @Override
    public void cleanup() {
        for (BukkitRunnable task : activeEffects.values()) {
            task.cancel();
        }
        activeEffects.clear();
        playersWithEffect.clear();
        pendingCorruptionDeath.clear();
    }

    private void drainTick(Player player, double damage) {
        if (!player.isOnline()) return;

        playDrainFeedback(player);

        UUID id = player.getUniqueId();
        boolean willBeLethal = player.getHealth() - damage <= 0;

        if (willBeLethal) {
            pendingCorruptionDeath.add(id);
        }

        player.damage(damage);

        if (willBeLethal && player.isOnline() && player.getHealth() > 0) {
            pendingCorruptionDeath.remove(id);
        }
    }

    private void playDrainFeedback(Player player) {
        Location loc = player.getLocation();
        if (loc.getWorld() == null) return;

        loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_DRINK, 0.8f, 0.6f);
        loc.getWorld().playSound(loc, Sound.BLOCK_SCULK_SHRIEKER_SHRIEK, 0.5f, 1.6f);
        loc.getWorld().spawnParticle(Particle.DUST,
                loc.clone().add(0, 1.0, 0), 20, 0.4, 0.6, 0.4,
                new Particle.DustOptions(org.bukkit.Color.fromRGB(80, 0, 80), 1.2f));
        loc.getWorld().spawnParticle(Particle.SQUID_INK, loc.clone().add(0, 1.0, 0), 6, 0.3, 0.5, 0.3, 0.01);
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        UUID id = player.getUniqueId();

        if (pendingCorruptionDeath.remove(id)) {
            event.setDeathMessage(player.getName() + " fue consumido por la corrupción");
        }
    }
}