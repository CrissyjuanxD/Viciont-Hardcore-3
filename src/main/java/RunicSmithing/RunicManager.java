package RunicSmithing;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class RunicManager {
    private static final List<RunicRecipe> recipes = new ArrayList<>();

    public static void registerRecipe(RunicRecipe recipe) {
        recipes.add(recipe);
    }

    public static List<RunicRecipe> getRecipes() {
        return recipes;
    }

    public static void clearRecipes() {
        recipes.clear();
    }

    // Busca una receta que coincida con los ítems puestos en la mesa
    public static RunicRecipe matchRecipe(ItemStack s1, ItemStack s3, ItemStack s5, ItemStack s7) {
        for (RunicRecipe recipe : recipes) {
            if (matches(recipe, 1, s1) &&
                    matches(recipe, 3, s3) &&
                    matches(recipe, 5, s5) &&
                    matches(recipe, 7, s7)) {
                return recipe;
            }
        }
        return null;
    }

    private static boolean matches(RunicRecipe recipe, int slot, ItemStack input) {
        ItemStack required = recipe.getIngredients().get(slot);

        // Si la receta no pide nada en este slot, el input debe ser null o aire
        if (required == null) {
            return input == null || input.getType().isAir();
        }

        // Si la receta pide algo, el input no puede ser nulo
        if (input == null) return false;

        // IMPORTANTE: Usamos isSimilarLenient para que acepte items dañados/encantados
        // y comprobamos que la cantidad sea suficiente.
        return isSimilarLenient(input, required) && input.getAmount() >= required.getAmount();
    }

    /**
     * Compara dos items ignorando la durabilidad y los encantamientos.
     */
    public static boolean isSimilarLenient(ItemStack input, ItemStack required) {
        if (input.getType() != required.getType()) return false;

        // Si no tienen meta, son iguales (ya comprobamos el tipo)
        if (!input.hasItemMeta() && !required.hasItemMeta()) return true;

        ItemMeta inputMeta = input.getItemMeta();
        ItemMeta requiredMeta = required.getItemMeta();

        // Si el item requerido tiene meta pero el input no, fallamos (salvo que el meta esté vacío)
        if (required.hasItemMeta() && !input.hasItemMeta()) return false;

        // --- 1. DISPLAY NAME ---
        if (requiredMeta.hasDisplayName()) {
            if (!inputMeta.hasDisplayName() || !inputMeta.getDisplayName().equals(requiredMeta.getDisplayName())) {
                return false;
            }
        }

        // --- 2. CUSTOM MODEL DATA (LA CLAVE DEL FIX) ---
        if (requiredMeta.hasCustomModelData()) {
            if (!inputMeta.hasCustomModelData() || inputMeta.getCustomModelData() != requiredMeta.getCustomModelData()) {
                return false;
            }
        } else {
            if (inputMeta.hasCustomModelData()) {
                return false;
            }
        }

        return true;
    }
}

