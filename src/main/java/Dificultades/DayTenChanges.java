package Dificultades;

import Dificultades.CustomMobs.CorruptedSpider;
import Dificultades.CustomMobs.CorruptedZombies;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Spider;
import org.bukkit.entity.Zombie;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public class DayTenChanges implements Listener {
    private final JavaPlugin plugin;
    private boolean isApplied = false;
    private final CorruptedZombies corruptedZombies;
    private final CorruptedSpider corruptedSpider;

    public DayTenChanges(JavaPlugin plugin) {
        this.plugin = plugin;
        this.corruptedZombies = new CorruptedZombies(plugin);
        this.corruptedSpider = new CorruptedSpider(plugin);
    }

    public void apply() {
        if (!isApplied) {
            //Bukkit.getPluginManager().registerEvents(this, plugin);
            isApplied = true;
            //replaceAllMobs();
        }
    }

    public void revert() {
        if (isApplied) {
            isApplied = false;
        }
    }

/*    public void replaceAllMobs() {
        if (!isApplied) {
            return; // Si los cambios no están aplicados, no hacer nada
        }

        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity instanceof Zombie && !corruptedZombies.isCorrupted((Zombie) entity)) {
                    Zombie zombie = (Zombie) entity;
                    corruptedZombies.spawnCorruptedZombie(zombie.getLocation());
                    zombie.remove();
                } else if (entity instanceof Spider && !corruptedSpider.isCorrupted((Spider) entity)) {
                    Spider spider = (Spider) entity;
                    corruptedSpider.spawnCorruptedSpider(spider.getLocation());
                    spider.remove();
                }
            }
        }
    }*/
}