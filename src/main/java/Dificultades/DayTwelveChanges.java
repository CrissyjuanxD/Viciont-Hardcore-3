package Dificultades;

import Dificultades.CustomMobs.BatBoom;
import Dificultades.CustomMobs.Bombita;
import Dificultades.CustomMobs.CorruptedCreeper;
import Handlers.DayHandler;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Random;

public class DayTwelveChanges implements Listener {
    private final JavaPlugin plugin;
    private final DayHandler dayHandler;
    private boolean isApplied = false;
    private final Random random = new Random();

    private final BatBoom batBoom;
    private final Bombita bombita;
    private final CorruptedCreeper corruptedCreeper;

    public DayTwelveChanges(JavaPlugin plugin, DayHandler handler) {
        this.plugin = plugin;
        this.dayHandler = handler;
        this.batBoom = new BatBoom(plugin);
        this.bombita = new Bombita(plugin);
        this.corruptedCreeper = new CorruptedCreeper(plugin);
    }

    public void apply() {
        if (!isApplied) {
            isApplied = true;
            Bukkit.getPluginManager().registerEvents(this, plugin);
            batBoom.apply();
        }
    }

    public void revert() {
        if (isApplied) {
            isApplied = false;
            batBoom.revert();
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

            // Solo a partir del día 9
            if (dayHandler.getCurrentDay() >= 14 && random.nextInt(3) == 0) {
                Creeper creeper = (Creeper) event.getEntity();

                // Marcar inmediatamente como custom
                creeper.getPersistentDataContainer().set(
                        bombita.getBombitaKey(),
                        PersistentDataType.BYTE,
                        (byte)1
                );

                bombita.spawnBombita(creeper.getLocation());
                creeper.remove();
            }
        }
    }

    private boolean isAlreadyCustomCreeper(Entity creeper) {
        return creeper.getPersistentDataContainer().has(bombita.getBombitaKey(), PersistentDataType.BYTE) ||
                creeper.getPersistentDataContainer().has(corruptedCreeper.getCorruptedCreeperKey(), PersistentDataType.BYTE);
    }
}
