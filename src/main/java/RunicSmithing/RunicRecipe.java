package RunicSmithing;

import org.bukkit.inventory.ItemStack;
import java.util.HashMap;
import java.util.Map;

public class RunicRecipe {
    private final String id;
    private final ItemStack result;
    private final Map<Integer, ItemStack> ingredients;

    public RunicRecipe(String id, ItemStack result) {
        this.id = id;
        this.result = result;
        this.ingredients = new HashMap<>();
    }

    public void addIngredient(int slot, int amount, ItemStack item) {
        if (slot == 1 || slot == 3 || slot == 5 || slot == 7) {
            ItemStack clone = item.clone();
            clone.setAmount(amount);
            ingredients.put(slot, clone);
        }
    }

    public void addIngredient(int slot, ItemStack item) {
        addIngredient(slot, item.getAmount(), item);
    }

    public ItemStack getResult() {
        return result.clone();
    }

    public Map<Integer, ItemStack> getIngredients() {
        return ingredients;
    }

    public String getId() {
        return id;
    }
}