package Dificultades.Features;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class AltarActivateEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();
    private final Player player;
    private final Location location;
    private final String altarType;
    private boolean cancelled = false;
    private int cooldownSeconds = 0; // El listener decidirá cuánto tiempo poner

    public AltarActivateEvent(Player player, Location location, String altarType) {
        this.player = player;
        this.location = location;
        this.altarType = altarType;
    }

    public Player getPlayer() { return player; }
    public Location getLocation() { return location; }
    public String getAltarType() { return altarType; }

    public void setCooldownSeconds(int seconds) {
        this.cooldownSeconds = seconds;
    }

    public int getCooldownSeconds() {
        return cooldownSeconds;
    }

    @Override
    public boolean isCancelled() { return cancelled; }

    @Override
    public void setCancelled(boolean cancel) { this.cancelled = cancel; }

    @Override
    public @NotNull HandlerList getHandlers() { return handlers; }

    public static HandlerList getHandlerList() { return handlers; }
}