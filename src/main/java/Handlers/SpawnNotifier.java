package Handlers;

import Managers.MobManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntitySpawnEvent;

public class SpawnNotifier implements Listener {

    private final MobManager mobManager;

    public SpawnNotifier(MobManager mobManager) {
        this.mobManager = mobManager;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntitySpawn(EntitySpawnEvent event) {
        mobManager.notifyEntitySpawned(event.getEntity());
    }
}