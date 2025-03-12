package TitleListener;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

public class RuletaAnimation {
    private final JavaPlugin plugin;
    private static final int TOTAL_FRAMES = 267; // Total de frames de la animación
    private static final String FRAME_PREFIX = "\uE3C6"; // Prefijo del unicode
    private static final int TICKS_DURATION = 165; // Duración total en ticks (~8 segundos)

    public RuletaAnimation(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void playAnimation(Player player, String jsonMessage) {
        // Efectos visuales iniciales
        player.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, TICKS_DURATION + 20, 1, true, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, TICKS_DURATION + 20, 0, true, false, false));
        player.playSound(player.getLocation(), "minecraft:custom.ruleta", 3.0f, 1.0f);

        // Calcula cuántos frames mostrar por tick
        double framesPerTick = (double) TOTAL_FRAMES / TICKS_DURATION;

        new BukkitRunnable() {
            int frame = 0; // Frame actual
            double accumulatedFrames = 0; // Frames acumulados

            @Override
            public void run() {
                if (frame >= TOTAL_FRAMES) {
                    // Cuando termina la animación
                    if (!jsonMessage.isEmpty()) {
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "tellraw " + player.getName() + " " + jsonMessage);
                        player.playSound(player.getLocation(), "minecraft:custom.noti", 1.0f, 1.3f);
                    }
                    cancel();
                    return;
                }

                // Mostrar el frame actual
                String unicodeFrame = String.valueOf((char) (FRAME_PREFIX.codePointAt(0) + frame));
                player.sendTitle(unicodeFrame, "", 0, 10, 0); // Cada título se muestra por 2 ticks

                // Calcular y avanzar frames
                accumulatedFrames += framesPerTick;
                while (accumulatedFrames >= 1) {
                    frame++;
                    accumulatedFrames--;
                }
            }
        }.runTaskTimer(plugin, 0, 1); // Ejecutar cada tick (20 veces por segundo)
    }
}
