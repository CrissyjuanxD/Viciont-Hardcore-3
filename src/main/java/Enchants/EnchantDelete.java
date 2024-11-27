package Enchants;

import org.bukkit.ChatColor;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.entity.VillagerAcquireTradeEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class EnchantDelete implements Listener {
    private final Set<Enchantment> prohibitedEnchantments = new HashSet<>(Arrays.asList(
            Enchantment.PROTECTION,
            Enchantment.UNBREAKING,
            Enchantment.MENDING,
            Enchantment.EFFICIENCY,
            Enchantment.SILK_TOUCH,
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

        // Remover encantamientos prohibidos del mapa de encantamientos
        Map<Enchantment, Integer> enchantsToAdd = event.getEnchantsToAdd();
        boolean removedAny = enchantsToAdd.entrySet().removeIf(entry -> prohibitedEnchantments.contains(entry.getKey()));

        // Si se eliminó algún encantamiento, actualizar el item encantado
        if (removedAny) {
            item.addUnsafeEnchantments(enchantsToAdd);
            event.getEnchanter().sendMessage(ChatColor.GRAY + "No se puede encantar con este encantamiento, y si se encanto, se ha removido del item");
        }
    }

    @EventHandler
    public void onVillagerInventoryOpen(InventoryOpenEvent event) {
        if (event.getInventory().getHolder() instanceof Villager villager) {
            List<MerchantRecipe> recipes = new ArrayList<>(villager.getRecipes());

            recipes.removeIf(recipe -> {
                ItemStack result = recipe.getResult();
                if (result.getType() == org.bukkit.Material.ENCHANTED_BOOK) {
                    EnchantmentStorageMeta meta = (EnchantmentStorageMeta) result.getItemMeta();
                    return meta != null && meta.getStoredEnchants().keySet().stream().anyMatch(prohibitedEnchantments::contains);
                }
                return false;
            });

            villager.setRecipes(recipes);
        }
    }


    @EventHandler
    public void onVillagerAcquireTrade(VillagerAcquireTradeEvent event) {
        ItemStack result = event.getRecipe().getResult();
        if (result.getType() == org.bukkit.Material.ENCHANTED_BOOK) {
            EnchantmentStorageMeta meta = (EnchantmentStorageMeta) result.getItemMeta();
            if (meta != null && meta.getStoredEnchants().keySet().stream().anyMatch(prohibitedEnchantments::contains)) {
                event.setCancelled(true);
            }
        }
    }
}
