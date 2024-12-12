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
        // Agregar efectos al jugador
        player.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 50, 1, true, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 50, 0, true, false, false));
        player.playSound(player.getLocation(), "minecraft:custom.ruleta", 1.0f, 1.0f);

        double framesPerTick = (double) TOTAL_FRAMES / TICKS_DURATION;

        new BukkitRunnable() {
            int frame = 0;
            double accumulatedFrames = 0; // Acumulador para frames fraccionados

            @Override
            public void run() {
                if (frame >= TOTAL_FRAMES) {
                    // Enviar el JSON solo una vez, después de la animación
                    if (!jsonMessage.isEmpty()) {
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "tellraw " + player.getName() + " " + jsonMessage);
                    }
                    cancel();
                    return;
                }

                // Mostrar el frame actual con interpolación
                String unicodeFrame = String.valueOf((char) (FRAME_PREFIX.codePointAt(0) + frame));
                player.sendTitle(unicodeFrame, "", 0, 40, 0);

                // Calcular y avanzar frames suavemente
                accumulatedFrames += framesPerTick;
                while (accumulatedFrames >= 1) {
                    frame++;
                    accumulatedFrames--;
                }
            }
        }.runTaskTimer(plugin, 0, 1);
    }
}
