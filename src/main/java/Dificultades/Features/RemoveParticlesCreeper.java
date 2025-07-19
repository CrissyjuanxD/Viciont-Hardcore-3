package Dificultades.Features;

import org.bukkit.entity.Creeper;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffectType;

public class RemoveParticlesCreeper implements Listener {
    private final JavaPlugin plugin;

    public RemoveParticlesCreeper(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onCreeperExplode(EntityExplodeEvent event) {
        if (event.getEntity() instanceof Creeper creeper) {
            creeper.removePotionEffect(PotionEffectType.SPEED);
            creeper.removePotionEffect(PotionEffectType.SLOWNESS);
            creeper.removePotionEffect(PotionEffectType.JUMP_BOOST);
            creeper.removePotionEffect(PotionEffectType.INVISIBILITY);
            creeper.removePotionEffect(PotionEffectType.REGENERATION);
            creeper.removePotionEffect(PotionEffectType.FIRE_RESISTANCE);
            creeper.removePotionEffect(PotionEffectType.WATER_BREATHING);
            creeper.removePotionEffect(PotionEffectType.STRENGTH);
            creeper.removePotionEffect(PotionEffectType.MINING_FATIGUE);
            creeper.removePotionEffect(PotionEffectType.RESISTANCE);
            creeper.removePotionEffect(PotionEffectType.HEALTH_BOOST);
            creeper.removePotionEffect(PotionEffectType.NIGHT_VISION);
            creeper.removePotionEffect(PotionEffectType.CONDUIT_POWER);
            creeper.removePotionEffect(PotionEffectType.DOLPHINS_GRACE);
            creeper.removePotionEffect(PotionEffectType.SATURATION);
            creeper.removePotionEffect(PotionEffectType.GLOWING);
            creeper.removePotionEffect(PotionEffectType.LEVITATION);
            creeper.removePotionEffect(PotionEffectType.LUCK);
            creeper.removePotionEffect(PotionEffectType.UNLUCK);
            creeper.removePotionEffect(PotionEffectType.SLOW_FALLING);
        }
    }
}
