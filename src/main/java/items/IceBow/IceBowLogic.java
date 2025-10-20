package items.IceBow;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.UUID;

/**
 * Clase responsable de toda la lógica de funcionamiento del Ice Bow
 */
public class IceBowLogic implements Listener {
    private final JavaPlugin plugin;
    private final Map<UUID, Long> playerBowCooldowns;
    private final NamespacedKey playerIceArrowKey;
    private final IceBowItem iceBowItem;

    public IceBowLogic(JavaPlugin plugin, Map<UUID, Long> playerBowCooldowns) {
        this.plugin = plugin;
        this.playerBowCooldowns = playerBowCooldowns;
        this.playerIceArrowKey = new NamespacedKey(plugin, "player_ice_arrow");
        this.iceBowItem = new IceBowItem(plugin);
    }

    /**
     * Método para que el Iceologer dispare el arco de hielo
     * @param shooter El Evoker que dispara
     * @param target El jugador objetivo
     * @param iceologerKey La clave del Iceologer
     */
    public void shootIceBow(Evoker shooter, Player target, NamespacedKey iceologerKey) {
        // Reproducir sonido de disparo
        shooter.getWorld().playSound(shooter.getLocation(), Sound.ENTITY_SKELETON_SHOOT, 1.0f, 0.8f);

        // Crear y disparar la flecha
        Arrow arrow = shooter.launchProjectile(Arrow.class);
        arrow.getPersistentDataContainer().set(iceologerKey, PersistentDataType.BYTE, (byte) 1);

        // Calcular la trayectoria hacia el jugador con mejor precisión
        Location targetLoc = target.getLocation().add(0, 1, 0);
        // Predecir la posición del jugador basado en su velocidad
        Vector playerVelocity = target.getVelocity();
        double distance = shooter.getEyeLocation().distance(targetLoc);
        double timeToReach = distance / 2.0; // velocidad de la flecha
        targetLoc.add(playerVelocity.clone().multiply(timeToReach));

        Vector direction = targetLoc.toVector()
                .subtract(shooter.getEyeLocation().toVector()).normalize();
        arrow.setVelocity(direction.multiply(2.5)); // Velocidad ligeramente mayor

        // Efectos visuales
        shooter.getWorld().spawnParticle(Particle.SNOWFLAKE,
                shooter.getEyeLocation(), 10, 0.3, 0.3, 0.3, 0.1);

        // Efectos de sonido adicionales
        shooter.getWorld().playSound(shooter.getLocation(), Sound.BLOCK_SNOW_BREAK, 0.8f, 1.5f);
    }

    /**
     * Maneja cuando un jugador dispara el arco de hielo
     */
    @EventHandler
    public void onPlayerShootBow(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        if (!iceBowItem.isIceBow(event.getBow())) {
            return;
        }

        // Verificar cooldown
        UUID playerId = player.getUniqueId();
        long currentTime = System.currentTimeMillis();

        if (playerBowCooldowns.containsKey(playerId)) {
            long lastUse = playerBowCooldowns.get(playerId);
            long remainingCooldown = (lastUse + 10000) - currentTime; // 10 segundos

            if (remainingCooldown > 0) {
                event.setCancelled(true);

                // Mostrar mensaje de cooldown

                // Efectos de error
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.7f, 1.2f);
                return;
            }
        }

        // Establecer cooldown
        playerBowCooldowns.put(playerId, currentTime);

        // Marcar la flecha del jugador
        if (event.getProjectile() instanceof Arrow arrow) {
            arrow.getPersistentDataContainer().set(playerIceArrowKey, PersistentDataType.BYTE, (byte) 1);

            // Efectos visuales cuando dispara
            player.getWorld().spawnParticle(Particle.SNOWFLAKE,
                    player.getEyeLocation(), 15, 0.4, 0.4, 0.4, 0.1);
            player.getWorld().playSound(player.getLocation(),
                    Sound.ENTITY_PLAYER_HURT_FREEZE, 0.5f, 1.8f);
        }

    }

    /**
     * Maneja el daño causado por las flechas del arco de hielo
     */
    public void handleArrowDamage(EntityDamageByEntityEvent event, Arrow arrow, NamespacedKey iceologerKey) {
        // Flecha del Iceologer
        if (arrow.getPersistentDataContainer().has(iceologerKey, PersistentDataType.BYTE)) {
            if (event.getEntity() instanceof LivingEntity entity) {
                applyIceologerArrowEffect(entity);
            }
        }
        // Flecha de jugador con arco de hielo
        else if (arrow.getPersistentDataContainer().has(playerIceArrowKey, PersistentDataType.BYTE)) {
            if (event.getEntity() instanceof LivingEntity entity) {
                applyPlayerArrowEffect(entity);
            }
        }
    }

    /**
     * Aplica efectos cuando una flecha del Iceologer impacta
     */
    private void applyIceologerArrowEffect(LivingEntity entity) {
        // 8 segundos de congelación (160 ticks)
        entity.setFreezeTicks(entity.getFreezeTicks() + 160);

        // Efectos de pociones
        entity.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 1));
        entity.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 80, 0));

        // Efectos visuales y sonoros
        World world = entity.getWorld();
        Location loc = entity.getLocation().add(0, 1, 0);

        world.spawnParticle(Particle.SNOWFLAKE, loc, 20, 0.5, 0.5, 0.5, 0.1);
        world.spawnParticle(Particle.CLOUD, loc, 8, 0.3, 0.3, 0.3, 0.05);
        world.playSound(entity.getLocation(), Sound.ENTITY_PLAYER_HURT_FREEZE, 1f, 0.1f);
        world.playSound(entity.getLocation(), Sound.BLOCK_GLASS_BREAK, 0.8f, 0.6f);
    }

    /**
     * Aplica efectos cuando una flecha de jugador con arco de hielo impacta
     */
    private void applyPlayerArrowEffect(LivingEntity entity) {
        // Solo 3 segundos de congelación para jugadores (60 ticks)
        entity.setFreezeTicks(entity.getFreezeTicks() + 60);

        // Efectos más suaves
        entity.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 0));

        // Efectos visuales y sonoros reducidos
        World world = entity.getWorld();
        Location loc = entity.getLocation().add(0, 1, 0);

        world.spawnParticle(Particle.SNOWFLAKE, loc, 10, 0.3, 0.3, 0.3, 0.1);
        world.playSound(entity.getLocation(), Sound.ENTITY_PLAYER_HURT_FREEZE, 0.5f, 1.2f);

    }

    /**
     * Limpia los cooldowns antiguos de los jugadores
     */
    public void cleanOldCooldowns() {
        long currentTime = System.currentTimeMillis();
        playerBowCooldowns.entrySet().removeIf(entry ->
                currentTime - entry.getValue() > 300000); // Limpiar entradas de más de 5 minutos
    }

    /**
     * Obtiene el tiempo restante de cooldown para un jugador
     * @param playerId UUID del jugador
     * @return Tiempo restante en milisegundos, 0 si no hay cooldown
     */
    public long getRemainingCooldown(UUID playerId) {
        if (!playerBowCooldowns.containsKey(playerId)) {
            return 0;
        }

        long lastUse = playerBowCooldowns.get(playerId);
        long currentTime = System.currentTimeMillis();
        long remainingTime = (lastUse + 10000) - currentTime; // 10 segundos de cooldown

        return Math.max(0, remainingTime);
    }

    /**
     * Remueve el cooldown de un jugador (útil para comandos de admin)
     * @param playerId UUID del jugador
     */
    public void removeCooldown(UUID playerId) {
        playerBowCooldowns.remove(playerId);
    }
}