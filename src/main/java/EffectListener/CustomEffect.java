package EffectListener;

import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

public interface CustomEffect {

    void applyEffect(Player player, int durationSeconds, int amplifier);

    default void applyEffect(Player player, int durationSeconds) {
        applyEffect(player, durationSeconds, 0);
    }

    void removeEffect(Player player);

    PotionEffectType getTriggerEffectType();


    boolean isEffectActive(Player player);


    default void cleanup() {
    }
}