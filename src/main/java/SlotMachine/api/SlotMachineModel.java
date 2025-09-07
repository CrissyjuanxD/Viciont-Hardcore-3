package SlotMachine.api;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import java.util.UUID;

/**
 * Modelo de API para SlotMachine - Basado en DTools3
 */
public class SlotMachineModel {

    private final String id;
    private final Location location;
    private UUID modelUUID;
    private Entity entity;
    private boolean isActive;
    private String currentAnimation;
    private long lastUsed;

    public SlotMachineModel(String id, Location location) {
        this.id = id;
        this.location = location;
        this.isActive = false;
        this.lastUsed = 0;
    }

    // Getters y Setters
    public String getId() { return id; }
    public Location getLocation() { return location; }

    public Entity getEntity() { return entity; }
    public void setEntity(Entity entity) { this.entity = entity; }

    public UUID getModelUUID() { return modelUUID; }
    public void setModelUUID(UUID modelUUID) { this.modelUUID = modelUUID; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public String getCurrentAnimation() { return currentAnimation; }
    public void setCurrentAnimation(String currentAnimation) { this.currentAnimation = currentAnimation; }

    public long getLastUsed() { return lastUsed; }
    public void setLastUsed(long lastUsed) { this.lastUsed = lastUsed; }
}