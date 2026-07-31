package CorruptedEnd;

import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Random;

public class MobSpawnManager implements Listener {
    private final JavaPlugin plugin;
    private final Random random = new Random();

    public MobSpawnManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void startSpawning() {
        // Generador maneja todo
    }

    @EventHandler
    public void onMobSpawn(CreatureSpawnEvent event) {
        if (!event.getLocation().getWorld().getName().equals(CorruptedEnd.WORLD_NAME)) return;

        if (event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.NATURAL &&
                event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.CHUNK_GEN) return;

        LivingEntity entity = event.getEntity();

        if (entity.getType() == EntityType.ENDERMAN) {
            if (random.nextBoolean()) {
                entity.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, Integer.MAX_VALUE, 1));
                entity.setCustomName("§5Corrupted Enderman");
                entity.setCustomNameVisible(false);
            }
        } else if (entity.getType() == EntityType.PHANTOM) {
            entity.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 1));
        }
    }
}