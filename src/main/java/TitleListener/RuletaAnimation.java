package TitleListener;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;

public class RuletaAnimation {
    private final JavaPlugin plugin;
    private static final int TOTAL_FRAMES = 267;
    private static final String FRAME_PREFIX = "\uE3C6";
    private static final double ANIMATION_DURATION = 8.25;
    private static final int SOUND_DELAY_TICKS = 140;

    private static int ongoingAnimations = 0;

    public RuletaAnimation(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public synchronized void playAnimation(Player player, String jsonMessage) {
        if (ongoingAnimations == 0) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "gamerule sendCommandFeedback false");
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "tick rate 32");
        }
        ongoingAnimations++;

        player.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 210, 1, true, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 210, 0, true, false, false));
        player.playSound(player.getLocation(), "minecraft:custom.ruleta", 3.0f, 1.0f);

        Bukkit.getWorlds().forEach(world -> {
            for (Entity entity : world.getEntities()) {
                if (entity instanceof LivingEntity && !(entity instanceof Player)) {
                    ((LivingEntity) entity).addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 200, 1, true, false, false));
                }
            }
        });

        List<String> unicodeFrames = generateUnicodeFrames(FRAME_PREFIX, TOTAL_FRAMES);

        new BukkitRunnable() {
            int frameIndex = 0;

            @Override
            public void run() {
                if (frameIndex >= TOTAL_FRAMES) {
                    // Finalizar animación
                    if (!jsonMessage.isEmpty()) {
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "tellraw " + player.getName() + " " + jsonMessage);
                        player.playSound(player.getLocation(), "minecraft:custom.noti", 1.0f, 1.3f);

                    }

                    synchronized (RuletaAnimation.this) {
                        ongoingAnimations--;
                        if (ongoingAnimations == 0) {
                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "tick rate 20");
                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "gamerule sendCommandFeedback true");
                        }
                    }
                    cancel();
                    return;
                }

                String unicodeFrame = unicodeFrames.get(frameIndex);
                String fixedFrame = "§r" + "\u2009".repeat(150) + unicodeFrame + "\u2009".repeat(150);
                Bukkit.getOnlinePlayers().forEach(p -> p.sendTitle(fixedFrame, "", 0, 10, 0));

                frameIndex++;

            }
        }.runTaskTimer(plugin, 0, 1);
    }

    private List<String> generateUnicodeFrames(String prefix, int totalFrames) {
        List<String> unicodeFrames = new ArrayList<>();
        int baseCodePoint = prefix.codePointAt(0);
        for (int i = 0; i < totalFrames; i++) {
            unicodeFrames.add(String.valueOf((char) (baseCodePoint + i)));
        }
        return unicodeFrames;
    }
}