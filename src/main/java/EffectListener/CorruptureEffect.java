package EffectListener;

import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class CorruptureEffect implements CustomEffect, Listener {
    private final Plugin plugin;
    private final Map<UUID, BukkitRunnable> activeEffects = new HashMap<>();
    private final Set<UUID> playersWithEffect = new HashSet<>();

    public CorruptureEffect(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void applyEffect(Player player, int durationSeconds) {
        removeEffect(player);

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
        activeEffects.put(player.getUniqueId(), task);
        playersWithEffect.add(player.getUniqueId());
    }

    @Override
    public void removeEffect(Player player) {
        UUID playerId = player.getUniqueId();
        BukkitRunnable task = activeEffects.get(playerId);

        if (task != null) {
            task.cancel();
            activeEffects.remove(playerId);
            playersWithEffect.remove(playerId);
        }
    }

    @Override
    public PotionEffectType getTriggerEffectType() {
        return PotionEffectType.WEAVING;
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

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (playersWithEffect.contains(player.getUniqueId())) {
            event.setCancelled(true);
            playCorruptionSounds(player);
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (playersWithEffect.contains(player.getUniqueId())) {
            event.setCancelled(true);
            playCorruptionSounds(player);
        }
    }

    private void playCorruptionSounds(Player player) {
        player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_BREAK, 1.0f, 2f);
        player.playSound(player.getLocation(), Sound.BLOCK_TRIAL_SPAWNER_OPEN_SHUTTER, 1.0f, 2f);
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 1.0f, 1.5f);
    }
}