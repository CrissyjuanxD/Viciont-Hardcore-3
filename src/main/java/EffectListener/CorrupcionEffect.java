package EffectListener;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class CorrupcionEffect implements CustomEffect, Listener {

    private final Plugin plugin;
    private final Map<UUID, BukkitRunnable> activeEffects = new HashMap<>();
    private final Map<UUID, Integer> activeAmplifier = new HashMap<>();
    private final Set<UUID> playersWithEffect = new HashSet<>();

    // Lista de jugadores que van a morir por corrupción (se limpia en el evento de muerte o resurrección)
    private final Set<UUID> pendingCorruptionKill = new HashSet<>();

    public CorrupcionEffect(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void applyEffect(Player player, int durationSeconds, int amplifier) {
        removeEffect(player);

        UUID id = player.getUniqueId();
        activeAmplifier.put(id, Math.max(0, amplifier));

        BukkitRunnable task = new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = durationSeconds * 20;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    removeEffect(player);
                    return;
                }

                if (!player.hasPotionEffect(getTriggerEffectType())) {
                    removeEffect(player);
                    return;
                }

                if (ticks >= maxTicks) {
                    killByCorruption(player);
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
        activeAmplifier.remove(id);
    }

    @Override
    public PotionEffectType getTriggerEffectType() {
        return PotionEffectType.LUCK;
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
        activeAmplifier.clear();
        pendingCorruptionKill.clear();
    }

    private void killByCorruption(Player player) {
        if (!player.isOnline()) return;
        UUID id = player.getUniqueId();

        // 1. Lo marcamos como "Muerte inminente por Corrupción"
        pendingCorruptionKill.add(id);

        // 2. Lo matamos ignorando la armadura.
        // Esto dispara PlayerDeathEvent o EntityResurrectEvent.
        player.setHealth(0.0);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();

        // Si el jugador muere y estaba en la lista, cambiamos el mensaje
        if (pendingCorruptionKill.remove(player.getUniqueId())) {
            event.setDeathMessage(player.getName() + " fue consumido por la corrupción.");
        }
    }

    @EventHandler
    public void onConsume(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        UUID id = player.getUniqueId();

        if (!playersWithEffect.contains(id)) return;

        Integer amplifier = activeAmplifier.get(id);

        // Nivel 1 (amplifier 0) puede curarse. Nivel 2+ (amplifier > 0) NO.
        if (amplifier == null || amplifier > 0) return;

        Material itemType = event.getItem().getType();
        boolean isGoldenApple = itemType == Material.GOLDEN_APPLE || itemType == Material.ENCHANTED_GOLDEN_APPLE;
        if (!isGoldenApple) return;

        if (!player.hasPotionEffect(PotionEffectType.WEAKNESS)) return;

        player.removePotionEffect(PotionEffectType.LUCK);
    }
}