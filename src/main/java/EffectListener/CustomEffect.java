package EffectListener;

import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

public interface CustomEffect {
    // Aplicar el efecto a un jugador
    void applyEffect(Player player, int duration);

    // Remover el efecto de un jugador
    void removeEffect(Player player);

    // Tipo de efecto de poción que activa este efecto custom
    PotionEffectType getTriggerEffectType();

    // Verificar si el efecto está activo en un jugador
    boolean isEffectActive(Player player);

    // Método opcional para cleanup si el efecto necesita liberar recursos
    default void cleanup() {
        // Implementación por defecto vacía
    }
}