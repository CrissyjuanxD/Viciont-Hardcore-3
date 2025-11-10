package EffectListener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public class CustomEffectManager implements Listener {
    private final Map<PotionEffectType, CustomEffect> registeredEffects = new HashMap<>();
    private final Set<UUID> playersWithEffects = new HashSet<>();

    // Registrar un efecto custom
    public void registerEffect(CustomEffect effect) {
        registeredEffects.put(effect.getTriggerEffectType(), effect);
    }

    // Desregistrar un efecto
    public void unregisterEffect(PotionEffectType effectType) {
        registeredEffects.remove(effectType);
    }

    // Obtener un efecto registrado
    public CustomEffect getEffect(PotionEffectType effectType) {
        return registeredEffects.get(effectType);
    }

    // Listener para cuando un jugador obtiene un efecto de poción
    @EventHandler
    public void onPlayerPotionEffect(org.bukkit.event.entity.EntityPotionEffectEvent event) {
        if (!(event.getEntity() instanceof Player)) return;

        Player player = (Player) event.getEntity();
        PotionEffectType effectType = event.getModifiedType();

        CustomEffect customEffect = registeredEffects.get(effectType);
        if (customEffect != null) {
            switch (event.getAction()) {
                case ADDED:
                case CHANGED:
                    PotionEffect newEffect = event.getNewEffect();
                    if (newEffect != null) {
                        int duration = newEffect.getDuration() / 20;
                        customEffect.applyEffect(player, duration);
                        playersWithEffects.add(player.getUniqueId());
                    }
                    break;

                case REMOVED:
                case CLEARED:
                    customEffect.removeEffect(player);
                    playersWithEffects.remove(player.getUniqueId());
                    break;

                default:
                    break;
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        // Remover todos los efectos custom del jugador que se va
        for (CustomEffect effect : registeredEffects.values()) {
            effect.removeEffect(player);
        }
        playersWithEffects.remove(player.getUniqueId());
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Si el jugador se reconecta y tenía efectos de poción activos,
        // reaplicar los efectos custom correspondientes
        Player player = event.getPlayer();

        for (PotionEffectType effectType : registeredEffects.keySet()) {
            if (player.hasPotionEffect(effectType)) {
                PotionEffect potionEffect = player.getPotionEffect(effectType);
                if (potionEffect != null) {
                    CustomEffect customEffect = registeredEffects.get(effectType);
                    int duration = potionEffect.getDuration() / 20;
                    customEffect.applyEffect(player, duration);
                    playersWithEffects.add(player.getUniqueId());
                }
            }
        }
    }

    // Aplicar efecto manualmente (útil para comandos)
    public void applyEffectManually(Player player, PotionEffectType effectType, int duration) {
        CustomEffect customEffect = registeredEffects.get(effectType);
        if (customEffect != null) {
            customEffect.applyEffect(player, duration);
        }
    }

    // Remover efecto manualmente
    public void removeEffectManually(Player player, PotionEffectType effectType) {
        CustomEffect customEffect = registeredEffects.get(effectType);
        if (customEffect != null) {
            customEffect.removeEffect(player);
        }
    }

    // Limpiar todos los efectos (al desactivar el plugin)
    public void cleanupAllEffects() {
        for (CustomEffect effect : registeredEffects.values()) {
            if (effect instanceof ConfusionEffect) {
                ((ConfusionEffect) effect).cleanup();
            }
            // Agregar cleanup para otros tipos de efectos si es necesario
        }
        registeredEffects.clear();
        playersWithEffects.clear();
    }
}