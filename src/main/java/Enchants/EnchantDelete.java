package Enchants;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.entity.VillagerAcquireTradeEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class EnchantDelete implements Listener {
    private final Set<Enchantment> prohibitedEnchantments = new HashSet<>(Arrays.asList(
            Enchantment.PROTECTION,
            Enchantment.UNBREAKING,
            Enchantment.EFFICIENCY,
            Enchantment.SHARPNESS,
            Enchantment.LOOTING,
            Enchantment.SMITE,
            Enchantment.FEATHER_FALLING,
            Enchantment.DEPTH_STRIDER,
            Enchantment.POWER,
            Enchantment.FORTUNE,
            Enchantment.BANE_OF_ARTHROPODS
    ));

    public EnchantDelete(JavaPlugin plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onEnchantItem(EnchantItemEvent event) {
        ItemStack item = event.getItem();
        Map<Enchantment, Integer> enchantsToAdd = event.getEnchantsToAdd();
        boolean removedAny = enchantsToAdd.entrySet().removeIf(entry -> prohibitedEnchantments.contains(entry.getKey()));

        if (removedAny) {
            item.addUnsafeEnchantments(enchantsToAdd);
            event.getEnchanter().sendMessage(ChatColor.GRAY + "No se puede encantar con este encantamiento, y si se encantó, se ha removido del ítem.");
        }
    }

    @EventHandler
    public void onVillagerInventoryOpen(InventoryOpenEvent event) {
        if (event.getInventory().getHolder() instanceof Villager villager) {
            List<MerchantRecipe> updatedRecipes = new ArrayList<>();

            for (MerchantRecipe recipe : villager.getRecipes()) {
                ItemStack result = recipe.getResult();
                if (result.getType() == Material.ENCHANTED_BOOK) {
                    EnchantmentStorageMeta meta = (EnchantmentStorageMeta) result.getItemMeta();
                    if (meta != null && meta.getStoredEnchants().keySet().stream().anyMatch(prohibitedEnchantments::contains)) {
                        continue; // Saltar libros con encantamientos prohibidos
                    }
                } else if (result.getItemMeta() != null && result.getItemMeta().hasEnchants()) {
                    ItemMeta meta = result.getItemMeta();
                    for (Enchantment enchant : prohibitedEnchantments) {
                        if (meta.hasEnchant(enchant)) {
                            meta.removeEnchant(enchant);
                        }
                    }
                    result.setItemMeta(meta);
                    MerchantRecipe newRecipe = new MerchantRecipe(result, recipe.getMaxUses());
                    newRecipe.setIngredients(recipe.getIngredients());
                    newRecipe.setExperienceReward(recipe.hasExperienceReward());
                    newRecipe.setVillagerExperience(recipe.getVillagerExperience());
                    newRecipe.setPriceMultiplier(recipe.getPriceMultiplier());
                    updatedRecipes.add(newRecipe);
                    continue;
                }

                updatedRecipes.add(recipe);
            }

            villager.setRecipes(updatedRecipes);
        }
    }

    @EventHandler
    public void onVillagerAcquireTrade(VillagerAcquireTradeEvent event) {
        ItemStack result = event.getRecipe().getResult();

        if (result.getType() == Material.ENCHANTED_BOOK) {
            EnchantmentStorageMeta meta = (EnchantmentStorageMeta) result.getItemMeta();
            if (meta != null && meta.getStoredEnchants().keySet().stream().anyMatch(prohibitedEnchantments::contains)) {
                event.setCancelled(true);
                return;
            }
        } else if (result.getItemMeta() != null && result.getItemMeta().hasEnchants()) {
            ItemMeta meta = result.getItemMeta();
            boolean modified = false;

            for (Enchantment enchant : prohibitedEnchantments) {
                if (meta.hasEnchant(enchant)) {
                    meta.removeEnchant(enchant);
                    modified = true;
                }
            }

            if (modified) {
                result.setItemMeta(meta);
                MerchantRecipe recipe = event.getRecipe();
                MerchantRecipe newRecipe = new MerchantRecipe(result, recipe.getMaxUses());
                newRecipe.setIngredients(recipe.getIngredients());
                newRecipe.setExperienceReward(recipe.hasExperienceReward());
                newRecipe.setVillagerExperience(recipe.getVillagerExperience());
                newRecipe.setPriceMultiplier(recipe.getPriceMultiplier());
                event.setRecipe(newRecipe);
            }
        }
    }
}
