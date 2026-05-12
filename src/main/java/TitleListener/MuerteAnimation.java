package TitleListener;

import com.viciontmedia.api.ViciontMediaAPI;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class MuerteAnimation {
    private final JavaPlugin plugin;

    // Retraso maestro de 1 segundo (20 ticks)
    private static final long RETRASO_INICIAL = 20L;
    // Tiempos antiguos + el retraso inicial
    private static final long SOUND_DELAY_TICKS = 160L + RETRASO_INICIAL;
    private static final long ANIMATION_DURATION_TICKS = 122L + RETRASO_INICIAL;

    public MuerteAnimation(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void playAnimation(Player deadPlayer, String jsonMessage) {

        // --- 1. INSTANTÁNEO (TICK 0) ---
        // El GIF sale al instante para todos
        Bukkit.getOnlinePlayers().forEach(p -> {
            ViciontMediaAPI.sendMedia(p, "Rueda_Muerte.gif", "minecraft:custom.emuerte", 1, 600, "50,40", 100, false, false);
        });

        // FEEDBACK INMEDIATO SOLO PARA EL JUGADOR QUE MURIÓ (Evita que sienta que es un bug)
        deadPlayer.playSound(deadPlayer.getLocation(), "minecraft:entity.allay.death", 10.0f, 0.7f);
        deadPlayer.playSound(deadPlayer.getLocation(), "minecraft:entity.blaze.death", 10.0f, 0.7f);

        // --- 2. RETRASADO 1 SEGUNDO (TICK 20) ---
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            // Efectos iniciales y sonidos para el RESTO de los jugadores
            Bukkit.getOnlinePlayers().forEach(p -> {

                // Solo suena para los demás, porque el muerto ya lo escuchó al instante
                if (!p.equals(deadPlayer)) {
                    p.playSound(p.getLocation(), "minecraft:entity.allay.death", 10.0f, 0.7f);
                    p.playSound(p.getLocation(), "minecraft:entity.blaze.death", 10.0f, 0.7f);
                }
            });

            // Lentitud a los mobs
            Bukkit.getWorlds().forEach(world -> {
                for (Entity entity : world.getEntities()) {
                    if (entity instanceof LivingEntity && !(entity instanceof Player)) {
                        ((LivingEntity) entity).addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 160, 1, true, false, false));
                    }
                }
            });
        }, RETRASO_INICIAL);

        // Programar el mensaje Tellraw (Si se usa)
        if (jsonMessage != null && !jsonMessage.isEmpty()) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "tellraw @a " + jsonMessage);
            }, ANIMATION_DURATION_TICKS);
        }

        // Programar el sonido retardado del caballo esqueleto
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Bukkit.getOnlinePlayers().forEach(p ->
                    p.playSound(p.getLocation(), Sound.ENTITY_SKELETON_HORSE_DEATH, 100.0f, 0.5f)
            );
        }, SOUND_DELAY_TICKS);
    }
}