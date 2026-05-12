package TitleListener;

import com.viciontmedia.api.ViciontMediaAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class RuletaAnimation {
    private final JavaPlugin plugin;

    public RuletaAnimation(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void playAnimation(Player player, String color, String mode, String pos, String jsonMessage) {

        String gifName = switch (color.toLowerCase()) {
            case "verde" -> "Rueda_Verde.gif";
            case "naranja" -> "Rueda_Naranja.gif";
            case "morado" -> "Rueda_Morada.gif";
            case "rosa" -> "Rueda_Rosa.gif";
            default -> "Rueda_Verde.gif";
        };

        // Lógica de posición y tamaño
        int size = 550;
        String vPos = "50,40";
        boolean overlay = false;

        if (!pos.equalsIgnoreCase("center")) {
            size = 300;
            vPos = pos; // "topleft", "topright", etc.
            overlay = true;
        }

        // FASE 1: INICIO (TICK 0)
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 210, 0, true, false, false));
        ViciontMediaAPI.sendMedia(player, gifName, "minecraft:custom.ruleta", 1, size, vPos, 90, overlay, false);

        for (Entity entity : player.getWorld().getEntities()) {
            if (entity instanceof LivingEntity && !(entity instanceof Player)) {
                ((LivingEntity) entity).addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 200, 1, true, false, false));
            }
        }

        // FASE 2: RESOLUCIÓN (9s después = 180 ticks)
        long delayAfterRuleta = 180L;

        if (color.equalsIgnoreCase("rosa") && mode.equalsIgnoreCase("evento")) {
            // Iniciar Video a los 9s
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                ViciontMediaAPI.sendMedia(player, "Momento_Evento.mp4", null, -1, 0, "center", 55, true, false);
            }, delayAfterRuleta);

            // Activar DISCO 3.5s después del video (9s + 3.5s = 12.5s total / 250 ticks)
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                ViciontMediaAPI.sendShaderApply(player, "wobble");
            }, delayAfterRuleta + 80L);

            // Apagar DISCO (Dura 29s el video)
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                ViciontMediaAPI.sendShaderRemove(player, "wobble");
            }, delayAfterRuleta + (29 * 20L));

            // Mensaje Final (30s después del video)
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (jsonMessage != null && !jsonMessage.isEmpty()) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "tellraw " + player.getName() + " " + jsonMessage);
                    player.playSound(player.getLocation(), "minecraft:custom.noti", 1.0f, 1.3f);
                }
            }, delayAfterRuleta + (30 * 20L));

        } else {
            // MODO NORMAL: Solo mensaje si existe
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (jsonMessage != null && !jsonMessage.isEmpty()) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "tellraw " + player.getName() + " " + jsonMessage);
                    player.playSound(player.getLocation(), "minecraft:custom.noti", 1.0f, 1.3f);
                }
            }, delayAfterRuleta);
        }
    }
}