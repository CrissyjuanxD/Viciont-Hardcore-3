package Dificultades;

import Dificultades.CustomMobs.Bombita;
import Dificultades.CustomMobs.CorruptedCreeper;
import Dificultades.CustomMobs.GuardianShulker;
import Handlers.DayHandler;
import org.bukkit.Bukkit;
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

public class DayFourteenChanges implements Listener {
    private final JavaPlugin plugin;
    private boolean isApplied = false;
    private final DayHandler dayHandler;
    private final GuardianShulker guardianShulker;
    private final Random random = new Random();

    public DayFourteenChanges(JavaPlugin plugin, DayHandler handler) {
        this.plugin = plugin;
        this.dayHandler = handler;
        this.guardianShulker = new GuardianShulker(plugin);
    }

    public void apply() {
        if (!isApplied) {
            isApplied = true;
            Bukkit.getPluginManager().registerEvents(this, plugin);
            guardianShulker.apply();

        }
    }

    public void revert() {
        if (isApplied) {
            isApplied = false;
            guardianShulker.revert();
            HandlerList.unregisterAll(this);
        }
    }
}
