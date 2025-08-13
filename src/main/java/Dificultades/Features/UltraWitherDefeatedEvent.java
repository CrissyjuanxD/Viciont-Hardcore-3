package Dificultades.Features;

import org.bukkit.entity.Wither;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class UltraWitherDefeatedEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final Wither wither;

    public UltraWitherDefeatedEvent(Wither wither) {
        this.wither = wither;
    }

    public Wither getWither() {
        return wither;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}