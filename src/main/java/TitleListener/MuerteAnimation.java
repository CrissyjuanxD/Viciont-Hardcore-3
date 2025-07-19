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

public class MuerteAnimation {
    private final JavaPlugin plugin;
    private static final int TOTAL_FRAMES = 184;
    private static final String FRAME_PREFIX = "\uE851";
    private static final double ANIMATION_DURATION = 6.12;
    private static final int SOUND_DELAY_TICKS = 160;

    private static int ongoingAnimations = 0;

    public MuerteAnimation(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public synchronized void playAnimation(Player player, String jsonMessage) {
        if (ongoingAnimations == 0) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "gamerule sendCommandFeedback false");
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "tick rate 30");
        }

        ongoingAnimations++;

        Bukkit.getOnlinePlayers().forEach(p -> {
            p.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 160, 1, true, false, false));
            p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 160, 0, true, false, false));
            p.playSound(p.getLocation(), "minecraft:custom.emuerte", 10.0f, 1.0f);
            p.playSound(p.getLocation(), "minecraft:entity.allay.death", 10.0f, 0.7f);
            p.playSound(p.getLocation(), "minecraft:entity.blaze.death", 10.0f, 0.7f);
        });

        Bukkit.getWorlds().forEach(world -> {
            for (Entity entity : world.getEntities()) {
                if (entity instanceof LivingEntity && !(entity instanceof Player)) {
                    ((LivingEntity) entity).addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 160, 1, true, false, false));
                }
            }
        });

        new BukkitRunnable() {
            @Override
            public void run() {
                Bukkit.getOnlinePlayers().forEach(p -> p.playSound(p.getLocation(), "minecraft:entity.skeleton_horse.death", 100.0f, 0.5f));
            }
        }.runTaskLater(plugin, SOUND_DELAY_TICKS);

        List<String> unicodeFrames = generateUnicodeFrames(FRAME_PREFIX, TOTAL_FRAMES);

        new BukkitRunnable() {
            int frameIndex = 0;

            @Override
            public void run() {
                if (frameIndex >= TOTAL_FRAMES) {
                    if (!jsonMessage.isEmpty()) {
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "tellraw @a " + jsonMessage);
                    }
                    synchronized (MuerteAnimation.this) {
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
                String fixedFrame = "§r" + "\u2009".repeat(300) + unicodeFrame + "\u2009".repeat(300);
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
