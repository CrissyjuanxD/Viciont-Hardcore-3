package EffectListener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class EcoMuertoEffect implements CustomEffect, Listener {

    private final Plugin plugin;
    private final Map<UUID, BukkitRunnable> activeEffects = new HashMap<>();
    private final Set<UUID> playersWithEffect = new HashSet<>();

    public EcoMuertoEffect(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void applyEffect(Player player, int durationSeconds, int amplifier) {
        removeEffect(player);

        UUID id = player.getUniqueId();

        BukkitRunnable task = new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = durationSeconds * 20;

            @Override
            public void run() {
                if (ticks >= maxTicks || !player.isOnline() || !player.hasPotionEffect(getTriggerEffectType())) {
                    removeEffect(player);
                    return;
                }
                ticks++;
            }
        };

        task.runTaskTimer(plugin, 0L, 20L);
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
        return PotionEffectType.WIND_CHARGED;
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
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onResurrect(EntityResurrectEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();

        if (playersWithEffect.contains(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }
}