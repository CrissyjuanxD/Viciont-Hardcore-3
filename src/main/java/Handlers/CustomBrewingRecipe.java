package Handlers;

import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Consumer;

public class CustomBrewingRecipe {
    private final ItemStack ingredient;
    private final Consumer<ItemStack> resultFunction;

    public CustomBrewingRecipe(ItemStack ingredient, Consumer<ItemStack> resultFunction) {
        this.ingredient = ingredient;
        this.resultFunction = resultFunction;
    }

    public boolean matches(ItemStack input) {
        return input != null && input.isSimilar(ingredient);
    }

    public void applyResult(ItemStack potion) {
        resultFunction.accept(potion);
    }
}
