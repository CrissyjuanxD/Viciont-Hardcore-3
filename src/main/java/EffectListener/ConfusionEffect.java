package EffectListener;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.potion.PotionEffectType;

import java.util.*;


public class ConfusionEffect implements CustomEffect {

    private final Plugin plugin;
    private final Map<UUID, BukkitRunnable> activeEffects = new HashMap<>();
    private final Map<UUID, Float> lastDeltaYaw = new HashMap<>();
    private final Map<UUID, Float> lastDeltaPitch = new HashMap<>();
    private final Random seedRandom = new Random();

    // --- Tuning base (nivel I / amplifier 0) ---
    private static final float BASE_AMPLITUDE_YAW = 3.0f;
    private static final float BASE_AMPLITUDE_PITCH = 1.8f;
    private static final float BASE_FREQUENCY = 0.55f; // "velocidad" del ruido

    // --- Escalado por nivel (fuerte, como se pidió) ---
    private static final float AMPLITUDE_PER_LEVEL = 2.6f;   // se suma por cada amplifier extra
    private static final float FREQUENCY_PER_LEVEL = 0.18f;  // ruido más rápido en niveles altos
    private static final float MAX_AMPLITUDE_YAW = 45.0f;    // cap para que siga siendo jugable
    private static final float MAX_AMPLITUDE_PITCH = 25.0f;
    private static final int DRIFT_START_LEVEL = 2;          // a partir de Unluck III (amplifier 2) hay drift

    private static final int FADE_TICKS = 15; // ticks de fade-in y fade-out (2 ticks = 1 tick de juego *2 => 0.1s/tick aprox)

    public ConfusionEffect(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void applyEffect(Player player, int durationSeconds, int amplifier) {
        removeEffect(player);

        UUID id = player.getUniqueId();
        long noiseSeedYaw = seedRandom.nextLong();
        long noiseSeedPitch = seedRandom.nextLong();
        long noiseSeedDrift = seedRandom.nextLong();

        int safeAmplifier = Math.max(0, amplifier);

        float amplitudeYaw = Math.min(MAX_AMPLITUDE_YAW, BASE_AMPLITUDE_YAW + AMPLITUDE_PER_LEVEL * safeAmplifier);
        float amplitudePitch = Math.min(MAX_AMPLITUDE_PITCH, BASE_AMPLITUDE_PITCH + AMPLITUDE_PER_LEVEL * 0.6f * safeAmplifier);
        float frequency = BASE_FREQUENCY + FREQUENCY_PER_LEVEL * safeAmplifier;
        float driftStrength = safeAmplifier >= DRIFT_START_LEVEL
                ? (safeAmplifier - DRIFT_START_LEVEL + 1) * 4.0f
                : 0f;

        lastDeltaYaw.put(id, 0f);
        lastDeltaPitch.put(id, 0f);

        PerlinNoise1D noiseYaw = new PerlinNoise1D(noiseSeedYaw);
        PerlinNoise1D noisePitch = new PerlinNoise1D(noiseSeedPitch);
        PerlinNoise1D noiseDrift = new PerlinNoise1D(noiseSeedDrift);

        BukkitRunnable task = new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = durationSeconds * 20;

            @Override
            public void run() {
                if (ticks * 2 >= maxTicks
                        || !player.isOnline()
                        || !player.hasPotionEffect(getTriggerEffectType())) {
                    removeEffect(player);
                    return;
                }

                applyShakeTick(player, ticks, maxTicks / 2, amplitudeYaw, amplitudePitch, frequency,
                        driftStrength, noiseYaw, noisePitch, noiseDrift);
                ticks++;
            }
        };

        task.runTaskTimer(plugin, 0L, 2L);
        activeEffects.put(id, task);
    }

    @Override
    public void removeEffect(Player player) {
        UUID playerId = player.getUniqueId();
        BukkitRunnable task = activeEffects.get(playerId);

        if (task != null) {
            task.cancel();
            activeEffects.remove(playerId);
            clearAppliedDelta(player);
            lastDeltaYaw.remove(playerId);
            lastDeltaPitch.remove(playerId);
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
                clearAppliedDelta(player);
            }
            entry.getValue().cancel();
        }
        activeEffects.clear();
        lastDeltaYaw.clear();
        lastDeltaPitch.clear();
    }

    private void applyShakeTick(Player player, int tick, int totalRunnableTicks,
                                float amplitudeYaw, float amplitudePitch, float frequency,
                                float driftStrength,
                                PerlinNoise1D noiseYaw, PerlinNoise1D noisePitch, PerlinNoise1D noiseDrift) {
        if (!player.isOnline()) return;

        UUID id = player.getUniqueId();

        float currentYaw = player.getLocation().getYaw() - lastDeltaYaw.getOrDefault(id, 0f);
        float currentPitch = player.getLocation().getPitch() - lastDeltaPitch.getOrDefault(id, 0f);

        float fade = fadeMultiplier(tick, totalRunnableTicks);

        float t = tick * frequency * 0.1f;

        float yawNoise = (noiseYaw.noise(t) * 2f - 1f) * amplitudeYaw;
        float pitchNoise = (noisePitch.noise(t) * 2f - 1f) * amplitudePitch;

        float drift = 0f;
        if (driftStrength > 0f) {
            drift = (noiseDrift.noise(t * 0.15f) * 2f - 1f) * driftStrength;
        }

        float deltaYaw = (yawNoise + drift) * fade;
        float deltaPitch = pitchNoise * fade;

        float newYaw = currentYaw + deltaYaw;
        float newPitch = clampPitch(currentPitch + deltaPitch);

        float appliedDeltaPitch = newPitch - currentPitch;

        setPlayerRotation(player, newYaw, newPitch);

        lastDeltaYaw.put(id, deltaYaw);
        lastDeltaPitch.put(id, appliedDeltaPitch);
    }

    private float fadeMultiplier(int tick, int totalRunnableTicks) {
        float fadeIn = tick >= FADE_TICKS ? 1.0f : smoothstep(tick / (float) FADE_TICKS);

        if (totalRunnableTicks <= 0) return fadeIn;

        int ticksRemaining = totalRunnableTicks - tick;
        float fadeOut = ticksRemaining >= FADE_TICKS ? 1.0f : smoothstep(ticksRemaining / (float) FADE_TICKS);

        return Math.min(fadeIn, fadeOut);
    }

    private float smoothstep(float x) {
        x = Math.max(0f, Math.min(1f, x));
        return x * x * (3f - 2f * x);
    }

    private float clampPitch(float pitch) {
        return Math.max(-90f, Math.min(90f, pitch));
    }

    private void setPlayerRotation(Player player, float yaw, float pitch) {
        try {
            player.setRotation(yaw, pitch);
        } catch (NoSuchMethodError e) {
        }
    }

    private void clearAppliedDelta(Player player) {
        UUID id = player.getUniqueId();
        Float dYaw = lastDeltaYaw.get(id);
        Float dPitch = lastDeltaPitch.get(id);
        if (dYaw == null && dPitch == null) return;
        if (!player.isOnline()) return;

        float realYaw = player.getLocation().getYaw() - (dYaw != null ? dYaw : 0f);
        float realPitch = player.getLocation().getPitch() - (dPitch != null ? dPitch : 0f);
        setPlayerRotation(player, realYaw, clampPitch(realPitch));
    }

    private static class PerlinNoise1D {
        private final int[] perm = new int[512];

        PerlinNoise1D(long seed) {
            int[] p = new int[256];
            for (int i = 0; i < 256; i++) p[i] = i;

            Random rnd = new Random(seed);
            for (int i = 255; i > 0; i--) {
                int j = rnd.nextInt(i + 1);
                int tmp = p[i];
                p[i] = p[j];
                p[j] = tmp;
            }
            for (int i = 0; i < 512; i++) perm[i] = p[i & 255];
        }

        float noise(float x) {
            int xi = (int) Math.floor(x) & 255;
            float xf = x - (float) Math.floor(x);
            float u = fade(xf);

            int a = perm[xi];
            int b = perm[xi + 1];

            float gradA = grad(a, xf);
            float gradB = grad(b, xf - 1);

            float result = lerp(gradA, gradB, u) * 2f;
            return (result + 1f) / 2f;
        }

        private float fade(float t) {
            return t * t * t * (t * (t * 6 - 15) + 10);
        }

        private float lerp(float a, float b, float t) {
            return a + t * (b - a);
        }

        private float grad(int hash, float x) {
            return (hash & 1) == 0 ? x : -x;
        }
    }
}