package Dificultades;

import Dificultades.CustomMobs.*;
import Handlers.DayHandler;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Random;

public class DaySixteenChanges implements Listener {
    private final JavaPlugin plugin;
    private final DayHandler dayHandler;
    private boolean isApplied = false;
    private final Random random = new Random();

    private final DarkCreeper darkCreeper;
    private final DarkVex darkVex;
    private final DarkSkeleton darkSkeleton;

    public DaySixteenChanges(JavaPlugin plugin, DayHandler handler) {
        this.plugin = plugin;
        this.dayHandler = handler;
        this.darkCreeper = new DarkCreeper(plugin);
        this.darkVex = new DarkVex(plugin);
        this.darkSkeleton = new DarkSkeleton(plugin);
    }

    public void apply() {
        if (!isApplied) {
            isApplied = true;
            Bukkit.getPluginManager().registerEvents(this, plugin);
            darkCreeper.apply();
            darkVex.apply();
            darkSkeleton.apply();
        }
    }

    public void revert() {
        if (isApplied) {
            isApplied = false;
            darkCreeper.revert();
            darkVex.revert();
            darkSkeleton.revert();
            HandlerList.unregisterAll(this);
        }
    }
}
