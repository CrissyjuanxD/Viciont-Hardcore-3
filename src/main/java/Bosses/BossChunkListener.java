package Bosses;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Bee;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public class BossChunkListener implements Listener {

    private final JavaPlugin plugin;
    private final NamespacedKey bossKey;

    public BossChunkListener(JavaPlugin plugin) {
        this.plugin = plugin;
        this.bossKey = new NamespacedKey(plugin, "is_queen_bee");
    }

    @EventHandler
    public void onEntitiesLoad(EntitiesLoadEvent event) {
        for (Entity entity : event.getEntities()) {
            checkAndRestoreBoss(entity);
        }
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        for (Entity entity : event.getChunk().getEntities()) {
            checkAndRestoreBoss(entity);
        }
    }

    private void checkAndRestoreBoss(Entity entity) {
        if (!(entity instanceof Bee bee)) return;

        if (!entity.getPersistentDataContainer().has(bossKey, PersistentDataType.BYTE)) return;

        if (bee.isDead() || bee.getHealth() <= 0) {
            bee.remove();
            return;
        }

        if (QueenBeeHandler.ACTIVE_BOSSES.containsKey(bee.getUniqueId())) {
            return;
        }

        bee.setAI(true);
        bee.setInvulnerable(false);
        bee.setGravity(true);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (bee.isValid() && !bee.isDead()) {
                if (!QueenBeeHandler.ACTIVE_BOSSES.containsKey(bee.getUniqueId())) {
                    new QueenBeeHandler(plugin, bee);
                    plugin.getLogger().info("Abeja Reina reactivada y IA forzada en: " + bee.getLocation());
                }
            }
        }, 1L);
    }
}