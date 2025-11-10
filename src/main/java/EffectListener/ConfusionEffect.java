package EffectListener;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public class ConfusionEffect implements CustomEffect {
    private final Plugin plugin;
    private final Map<UUID, BukkitRunnable> activeEffects = new HashMap<>();
    private final Random random = new Random();
    private final Map<UUID, Float> originalYaw = new HashMap<>();
    private final Map<UUID, Float> originalPitch = new HashMap<>();
    private final Map<UUID, Integer> shakePatterns = new HashMap<>();

    public ConfusionEffect(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void applyEffect(Player player, int durationSeconds) {
        
        removeEffect(player);

        originalYaw.put(player.getUniqueId(), player.getLocation().getYaw());
        originalPitch.put(player.getUniqueId(), player.getLocation().getPitch());
        shakePatterns.put(player.getUniqueId(), random.nextInt(4)); 

        BukkitRunnable task = new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = durationSeconds * 20;
            int pattern = shakePatterns.get(player.getUniqueId());
            float baseYaw = originalYaw.get(player.getUniqueId());
            float basePitch = originalPitch.get(player.getUniqueId());

            @Override
            public void run() {
                if (ticks >= maxTicks || !player.isOnline() || !player.hasPotionEffect(getTriggerEffectType())) {
                    removeEffect(player);
                    return;
                }
                
                applySmoothCameraShake(player, ticks, pattern, baseYaw, basePitch);
                ticks++;
            }
        };

        task.runTaskTimer(plugin, 0L, 2L);
        activeEffects.put(player.getUniqueId(), task);
    }

    @Override
    public void removeEffect(Player player) {
        UUID playerId = player.getUniqueId();
        BukkitRunnable task = activeEffects.get(playerId);

        if (task != null) {
            task.cancel();
            activeEffects.remove(playerId);
            restoreOriginalRotation(player);
            originalYaw.remove(playerId);
            originalPitch.remove(playerId);
            shakePatterns.remove(playerId);
        }
    }

    @Override
    public PotionEffectType getTriggerEffectType() {
        return PotionEffectType.UNLUCK;
    }

    @Override
    public boolean isEffectActive(Player player) {
        return activeEffects.containsKey(player.getUniqueId());
    }

    @Override
    public void cleanup() {
        for (Map.Entry<UUID, BukkitRunnable> entry : activeEffects.entrySet()) {
            Player player = plugin.getServer().getPlayer(entry.getKey());
            if (player != null) {
                restoreOriginalRotation(player);
            }
            entry.getValue().cancel();
        }
        activeEffects.clear();
        originalYaw.clear();
        originalPitch.clear();
        shakePatterns.clear();
    }

    private void applySmoothCameraShake(Player player, int tick, int pattern, float baseYaw, float basePitch) {
        if (!player.isOnline()) return;
        
        float frequency = 0.3f;
        float amplitudeYaw = 8.0f; 
        float amplitudePitch = 5.0f;

        float yawVariation = calculateShake(tick, pattern, frequency, amplitudeYaw, 0);
        float pitchVariation = calculateShake(tick, pattern, frequency, amplitudePitch, 1);

        float newYaw = baseYaw + yawVariation;
        float newPitch = Math.max(-90, Math.min(90, basePitch + pitchVariation));

        setPlayerRotation(player, newYaw, newPitch);
    }

    private float calculateShake(int tick, int pattern, float frequency, float amplitude, int offset) {
        float time = tick * frequency + offset * 2.0f;

        switch (pattern) {
            case 0: 
                return (float) (Math.sin(time) * amplitude * 0.7f + Math.cos(time * 0.8f) * amplitude * 0.3f);
            case 1: 
                return (float) (Math.sin(time) * amplitude * (1.0f - Math.abs(Math.sin(time * 0.5f))));
            case 2: 
                return (float) ((Math.sin(time) + 0.5f * Math.sin(time * 2.3f) + 0.3f * Math.sin(time * 3.7f)) * amplitude * 0.4f);
            case 3: 
                return (float) (Math.sin(time) * Math.cos(time * 0.7f) * amplitude);
            default: 
                return (float) (Math.sin(time) * amplitude * 0.5f);
        }
    }

    private void setPlayerRotation(Player player, float yaw, float pitch) {
        try {
            player.setRotation(yaw, pitch);
        } catch (NoSuchMethodError e) {
        }
    }

    private void restoreOriginalRotation(Player player) {
        UUID playerId = player.getUniqueId();
        Float originalY = originalYaw.get(playerId);
        Float originalP = originalPitch.get(playerId);
        if (originalY != null && originalP != null) {
            setPlayerRotation(player, originalY, originalP);
        }
    }
}