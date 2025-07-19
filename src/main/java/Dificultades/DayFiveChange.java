package Dificultades;

import Dificultades.CustomMobs.*;
import Dificultades.Features.MobCapManager;
import Handlers.DayHandler;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class DayFiveChange implements Listener {
    private final JavaPlugin plugin;
    private boolean isApplied = false;
    private final DayHandler dayHandler;
    private final CorruptedCreeper corruptedCreeper;
    private final Bombita bombita;
    private final Random random = new Random();

    public DayFiveChange(JavaPlugin plugin, DayHandler handler) {
        this.plugin = plugin;
        this.dayHandler = handler;
        this.corruptedCreeper = new CorruptedCreeper(plugin);
        this.bombita = new Bombita(plugin);
    }

    public void apply() {
        if (!isApplied) {
            Bukkit.getPluginManager().registerEvents(this, plugin);
            isApplied = true;
            corruptedCreeper.apply();

        }
    }

    public void revert() {
        if (isApplied) {
            isApplied = false;
            corruptedCreeper.revert();
            HandlerList.unregisterAll(this);
        }
    }

    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (!isApplied) return;

        if (event.getLocation().getWorld().getEnvironment() != World.Environment.NORMAL) {
            return;
        }

        if (event.getEntityType() == EntityType.CREEPER &&
                !isAlreadyCustomCreeper(event.getEntity())) {

            // Solo en día 5 o posteriores
            if (dayHandler.getCurrentDay() >= 5 && random.nextInt(3) == 0) {
                Creeper creeper = (Creeper) event.getEntity();

                // Marcar inmediatamente como custom para evitar race conditions
                creeper.getPersistentDataContainer().set(
                        corruptedCreeper.getCorruptedCreeperKey(),
                        PersistentDataType.BYTE,
                        (byte)1
                );

                corruptedCreeper.spawnCorruptedCreeper(creeper.getLocation());
                creeper.remove();
            }
        }
    }

    private boolean isAlreadyCustomCreeper(Entity creeper) {
        // Verificar ambas keys de mobs custom
        return creeper.getPersistentDataContainer().has(corruptedCreeper.getCorruptedCreeperKey(), PersistentDataType.BYTE) ||
                creeper.getPersistentDataContainer().has(bombita.getBombitaKey(), PersistentDataType.BYTE);
    }

}
