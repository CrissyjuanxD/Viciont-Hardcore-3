package TitleListener;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

public class MuerteAnimation {
    private final JavaPlugin plugin;
    private static final int TOTAL_FRAMES = 184;
    private static final String FRAME_PREFIX = "\uE851"; // Unicode inicial del primer frame
    private static final int TICKS_DURATION = 120; // Duración en ticks para la animación (8 segundos x 20 ticks)
    private static final int SOUND_DELAY_TICKS = 100; // Retraso para sonidos adicionales (5 segundos x 20 ticks)

    public MuerteAnimation(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void playAnimation(Player player, String jsonMessage) {

        // Reproducir sonido
        player.playSound(player.getLocation(), "minecraft:custom.emuerte", 300.0f, 1.0f);
        player.playSound(player.getLocation(), "minecraft:entity.allay.death", 100.0f, 0.7f);
        player.playSound(player.getLocation(), "minecraft:entity.blaze.death", 100.0f, 0.7f);
        // Programar reproducción del sonido de skeleton_horse después de 5 segundos
        new BukkitRunnable() {
            @Override
            public void run() {
                player.playSound(player.getLocation(), "minecraft:entity.skeleton_horse.death", 100.0f, 0.5f);
            }
        }.runTaskLater(plugin, SOUND_DELAY_TICKS);


        // Calcular frames por tick
        double framesPerTick = (double) TOTAL_FRAMES / TICKS_DURATION;

        new BukkitRunnable() {
            int frame = 0;
            double accumulatedFrames = 0; // Acumulador para avanzar frames cada tick

            @Override
            public void run() {
                if (frame >= TOTAL_FRAMES) {
                    // Al finalizar, enviar mensaje JSON si existe
                    if (!jsonMessage.isEmpty()) {
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "tellraw @a " + jsonMessage);
                    }
                    cancel();
                    return;
                }

                // Mostrar el frame actual
                String unicodeFrame = String.valueOf((char) (FRAME_PREFIX.codePointAt(0) + frame));
                player.sendTitle(unicodeFrame, "", 0, 5, 0); // Mostrar por 5 ticks

                // Incrementar el acumulador y calcular avance de frames
                accumulatedFrames += framesPerTick;
                while (accumulatedFrames >= 1) {
                    frame++; // Avanza a los frames acumulados
                    accumulatedFrames--; // Ajusta el acumulador
                }
            }
        }.runTaskTimer(plugin, 0, 1); // Ejecuta cada tick
    }
}
