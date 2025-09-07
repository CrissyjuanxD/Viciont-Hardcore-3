package CorruptedEnd;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.ChatColor;

import java.util.Arrays;
import java.util.Random;

public class LootManager {
    private final JavaPlugin plugin;
    private final Random random = new Random();

    public LootManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void giveLootChest(Player player, LootType type) {
        ItemStack chest = new ItemStack(Material.CHEST);
        ItemMeta meta = chest.getItemMeta();

        meta.setDisplayName(ChatColor.RESET + type.getDisplayName());
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "Una misteriosa caja del Corrupted End",
                ChatColor.DARK_GRAY + "Click derecho para abrir",
                ChatColor.DARK_PURPLE + "Tipo: " + type.getDisplayName()
        ));

        // Añadir metadata personalizada para identificar el tipo
        meta.getPersistentDataContainer().set(
                new org.bukkit.NamespacedKey(plugin, "loot_type"),
                org.bukkit.persistence.PersistentDataType.STRING,
                type.name()
        );

        chest.setItemMeta(meta);

        // Intentar dar el ítem al jugador
        if (player.getInventory().firstEmpty() != -1) {
            player.getInventory().addItem(chest);
        } else {
            // Si no hay espacio, dropear en el mundo
            player.getWorld().dropItem(player.getLocation(), chest);
            player.sendMessage(ChatColor.YELLOW + "Tu inventario está lleno. La caja se droppeo en el suelo.");
        }
    }

    public ItemStack[] generateLoot(LootType type) {
        switch (type) {
            case CAJA_CORRUPTA:
                return generateCorruptedLoot();
            case CAJA_INFESTADA:
                return generateInfestedLoot();
            default:
                return new ItemStack[0];
        }
    }

    private ItemStack[] generateCorruptedLoot() {
        ItemStack[] loot = new ItemStack[random.nextInt(4) + 3]; // 3-6 items

        // Items posibles para Caja Corrupta
        ItemStack[] possibleItems = {
                new ItemStack(Material.GOLD_INGOT, random.nextInt(8) + 1),
                new ItemStack(Material.DIAMOND, random.nextInt(3) + 1),
                new ItemStack(Material.GOLDEN_CARROT, random.nextInt(5) + 1),
                new ItemStack(Material.TOTEM_OF_UNDYING, 1),
                new ItemStack(Material.GOLD_BLOCK, random.nextInt(2) + 1),
                new ItemStack(Material.GOLDEN_APPLE, random.nextInt(2) + 1),
                new ItemStack(Material.EXPERIENCE_BOTTLE, random.nextInt(10) + 5),
        };

        // Probabilidades especiales
        for (int i = 0; i < loot.length; i++) {
            if (random.nextDouble() < 0.15 && containsItem(loot, Material.TOTEM_OF_UNDYING) == -1) {
                // 15% probabilidad de totem (solo uno por caja)
                loot[i] = new ItemStack(Material.TOTEM_OF_UNDYING, 1);
            } else {
                // Item aleatorio normal
                loot[i] = possibleItems[random.nextInt(possibleItems.length)].clone();
            }
        }

        return loot;
    }

    private ItemStack[] generateInfestedLoot() {
        ItemStack[] loot = new ItemStack[random.nextInt(3) + 4]; // 4-6 items

        // Items posibles para Caja Infestada (más valiosos)
        ItemStack[] possibleItems = {
                new ItemStack(Material.TOTEM_OF_UNDYING, random.nextInt(2) + 1),
                new ItemStack(Material.EMERALD, random.nextInt(6) + 3),
                new ItemStack(Material.GOLDEN_APPLE, random.nextInt(3) + 1),
                new ItemStack(Material.GOLD_INGOT, random.nextInt(12) + 5),
                new ItemStack(Material.DIAMOND, random.nextInt(4) + 2),
                new ItemStack(Material.NETHERITE_SCRAP, random.nextInt(2) + 1),
        };

        // Probabilidades especiales para caja infestada
        boolean hasEnchantedApple = false;

        for (int i = 0; i < loot.length; i++) {
            if (random.nextDouble() < 0.25 && !hasEnchantedApple) {
                // 25% probabilidad de manzana dorada encantada
                loot[i] = new ItemStack(Material.ENCHANTED_GOLDEN_APPLE, 1);
                hasEnchantedApple = true;
            } else if (random.nextDouble() < 0.4 && containsItem(loot, Material.TOTEM_OF_UNDYING) < 2) {
                // 40% probabilidad de totem (máximo 2 por caja)
                loot[i] = new ItemStack(Material.TOTEM_OF_UNDYING, 1);
            } else {
                // Item aleatorio normal
                loot[i] = possibleItems[random.nextInt(possibleItems.length)].clone();
            }
        }

        return loot;
    }

    private int containsItem(ItemStack[] items, Material material) {
        int count = 0;
        for (ItemStack item : items) {
            if (item != null && item.getType() == material) {
                count++;
            }
        }
        return count;
    }
}

enum LootType {
    CAJA_CORRUPTA("Caja Corrupta"),
    CAJA_INFESTADA("Caja Infestada");

    private final String displayName;

    LootType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}