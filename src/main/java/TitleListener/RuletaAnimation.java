package TitleListener;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

public class RuletaAnimation {
    private final JavaPlugin plugin;
    private static final int TOTAL_FRAMES = 267;
    private static final String FRAME_PREFIX = "\uE3C6"; // Unicode inicial del primer frame
    private static final int TICKS_DURATION = 170; // Duración en ticks para la animación (8 segundos x 20 ticks)

    public RuletaAnimation(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void playAnimation(Player player, String jsonMessage) {

        // Aplicar efectos a jugadores
        player.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 250, 1, true, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 250, 0, true, false, false));
        // Reproducir sonido
        player.playSound(player.getLocation(), "minecraft:custom.ruleta", 1.0f, 1.0f);

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
