package RunicSmithing;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

public class RunicCraftEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final Player player;
    private final ItemStack result;
    private final RunicRecipe recipe;

    public RunicCraftEvent(Player player, ItemStack result, RunicRecipe recipe) {
        this.player = player;
        this.result = result;
        this.recipe = recipe;
    }

    public Player getPlayer() {
        return player;
    }

    public ItemStack getResult() {
        return result;
    }

    public RunicRecipe getRecipe() {
        return recipe;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}