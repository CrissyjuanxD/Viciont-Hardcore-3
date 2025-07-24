package Dificultades;

import Dificultades.CustomMobs.GuardianShulker;
import Dificultades.CustomMobs.Iceologer;
import Handlers.DayHandler;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Random;

public class DayFifteenChanges implements Listener {
    private final JavaPlugin plugin;
    private boolean isApplied = false;
    private final DayHandler dayHandler;
    private final Random random = new Random();

    public DayFifteenChanges(JavaPlugin plugin, DayHandler handler) {
        this.plugin = plugin;
        this.dayHandler = handler;
    }

    public void apply() {
        if (!isApplied) {
            isApplied = true;
            Bukkit.getPluginManager().registerEvents(this, plugin);
        }
    }

    public void revert() {
        if (isApplied) {
            isApplied = false;
            HandlerList.unregisterAll(this);
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!isApplied) return;

        if (event.getCause() == EntityDamageEvent.DamageCause.LIGHTNING) {
            event.setDamage(event.getDamage() * 3);
        }
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        if (!isApplied) return;

        World world = event.getWorld();
        if (world.getEnvironment() == World.Environment.NORMAL ||
                world.getEnvironment() == World.Environment.NETHER ||
                world.getEnvironment() == World.Environment.THE_END ||
                world.getEnvironment() == World.Environment.CUSTOM) {
            world.setGameRule(GameRule.NATURAL_REGENERATION, false);
        }
    }
}
