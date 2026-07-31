package Gui;

import java.util.HashMap;
import java.util.Map;

public class ViciontRecipeRegistry {

    private final Map<String, ViciontRecipe> recipes = new HashMap<>();
    private final ViciontItemRegistry itemRegistry;

    public ViciontRecipeRegistry(ViciontItemRegistry itemRegistry) {
        this.itemRegistry = itemRegistry;
        registerAllRecipes();
    }

    private void registerAllRecipes() {
        // ==========================================
        // TUS RECETAS AQUÍ
        // ==========================================

        // TIPO 1: MESA DE CRAFTEO (3x3)
        ViciontRecipe recipeCarne = new ViciontRecipe("view_recipe_corrupted_steak", 1, "Mesa de Crafteo", itemRegistry.getItem("corrupted_steak"))
                .shape(
                        "CCC",
                        "CSC",
                        "CCC"
                )
                .setIngredient('C', itemRegistry.getItem("corrupted_rotten_flesh")) // Usamos el ítem custom oculto
                .setIngredient('S', ViciontItem.vanilla("minecraft:cooked_beef", "default")); // Usamos vainilla rápido
        recipes.put(recipeCarne.getId(), recipeCarne);

        ViciontRecipe recipeplacadiamante = new ViciontRecipe("view_recipe_diamond_plate", 1, "Mesa de Crafteo", itemRegistry.getItem("diamond_plate"))
                .shape(
                        "CIC",
                        "IDI",
                        "CIC"
                )
                .setIngredient('I', ViciontItem.vanilla("minecraft:iron_ingot", "default"))
                .setIngredient('C', ViciontItem.vanilla("minecraft:copper_block", "default"))
                .setIngredient('D', ViciontItem.vanilla("minecraft:diamond_block", "default"));
        recipes.put(recipeplacadiamante.getId(), recipeplacadiamante);

        ViciontRecipe recipemesa_mejorada = new ViciontRecipe("view_recipe_mesa_enc_mejorada", 1, "Mesa de Crafteo", itemRegistry.getItem("mesa_enc_mejorada"))
                .shape(
                        "IBI",
                        "PEP",
                        "LGL"
                )
                .setIngredient('I', ViciontItem.vanilla("minecraft:iron_block", "default"))
                .setIngredient('B', ViciontItem.vanilla("minecraft:book", "default"))
                .setIngredient('P', itemRegistry.getItem("diamond_plate"))
                .setIngredient('E', ViciontItem.vanilla("minecraft:enchanting_table", "default"))
                .setIngredient('G', ViciontItem.vanilla("minecraft:gold_block", "default"))
                .setIngredient('L', ViciontItem.vanilla("minecraft:bookshelf", "default"));
        recipes.put(recipemesa_mejorada.getId(), recipemesa_mejorada);

        // TIPO 2: HORNO (Input, Fuel)
        ViciontRecipe recipeScrap = new ViciontRecipe("view_recipe_corrupted_scrap", 2, "Horno", itemRegistry.getItem("corrupted_scrap"))
                .shape("IF")
                .setIngredient('I', itemRegistry.getItem("corrupted_debris"))
                .setIngredient('F', ViciontItem.vanilla("minecraft:coal", "default"));
        recipes.put(recipeScrap.getId(), recipeScrap);

        // TIPO 3: HERRERÍA (Template, Armor, Ingot)
        ViciontRecipe recipeCasco = new ViciontRecipe("view_recipe_netherite_helmet", 3, "Mesa de Herrería", itemRegistry.getItem("netherite_helmet"))
                .shape("TAI")
                .setIngredient('T', ViciontItem.vanilla("minecraft:netherite_upgrade_smithing_template", "default"))
                .setIngredient('A', ViciontItem.vanilla("minecraft:diamond_helmet", "default"))
                .setIngredient('I', ViciontItem.vanilla("minecraft:netherite_ingot", "default"));
        recipes.put(recipeCasco.getId(), recipeCasco);

        // TIPO 4: MESA RÚNICA (4 Ítems)
        ViciontRecipe recipeRunica = new ViciontRecipe("view_recipe_runic_chestplate", 4, "Mesa Rúnica", itemRegistry.getItem("runic_chestplate"))
                .shape("ABCD")
                .setIngredient('A', ViciontItem.vanilla("minecraft:netherite_chestplate", "Netherite Chestplate"))
                .setIngredient('B', ViciontItem.vanilla("minecraft:echo_shard", "Chestplate Netherite Upgrade", 305, "#cc3366", true))
                .setIngredient('C', ViciontItem.vanilla("minecraft:echo_shard", "default"))
                .setIngredient('D', ViciontItem.vanilla("minecraft:netherite_ingot", "Corrupted Netherite Ingot", 5, "#9900cc", true));
        recipes.put(recipeRunica.getId(), recipeRunica);
    }

    public ViciontRecipe getRecipe(String actionId) {
        return recipes.get(actionId);
    }
}