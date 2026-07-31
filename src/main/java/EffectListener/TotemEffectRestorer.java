package EffectListener;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class TotemEffectRestorer implements Listener {

    private final Plugin plugin;

    // Lista de efectos que NO deben borrarse al usar un tótem
    private static final Set<PotionEffectType> CUSTOM_EFFECTS = Set.of(
            PotionEffectType.WEAVING,      // Corrupture
            PotionEffectType.LUCK,         // Corrupción
            PotionEffectType.OOZING,       // Drenaje
            PotionEffectType.WIND_CHARGED, // Eco Muerto
            PotionEffectType.UNLUCK        // Confusión
    );

    public TotemEffectRestorer(Plugin plugin) {
        this.plugin = plugin;
    }

    // MONITOR asegura que leemos el evento justo antes de que se limpie todo, 
    // y solo si el tótem REALMENTE va a funcionar (ignoreCancelled = true).
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTotemPop(EntityResurrectEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();

        List<PotionEffect> effectsToKeep = new ArrayList<>();

        // Extraer los efectos custom actuales
        for (PotionEffect effect : player.getActivePotionEffects()) {
            if (CUSTOM_EFFECTS.contains(effect.getType())) {
                effectsToKeep.add(effect);
            }
        }

        if (!effectsToKeep.isEmpty()) {
            // Reaplicar exactamente los mismos efectos 1 tick después
            // para que sobrevivan al clear vanilla del tótem.
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline() && !player.isDead()) {
                    for (PotionEffect effect : effectsToKeep) {
                        player.addPotionEffect(effect);
                    }
                }
            }, 1L);
        }
    }
}