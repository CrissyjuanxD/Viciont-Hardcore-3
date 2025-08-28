package Armors;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;


import java.util.*;

public class CorruptedArmor implements Listener {

    private final JavaPlugin plugin;

    public CorruptedArmor(JavaPlugin plugin) {
        this.plugin = plugin;
    }


    public static ItemStack createCorruptedHelmet() {
        ItemStack item = new ItemStack(Material.NETHERITE_HELMET);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ChatColor.of("#9966ff") + "" + ChatColor.BOLD + "Corrupted Netherite Helmet");
        meta.setLore(Arrays.asList(
                "",
                ChatColor.of("#996699") + "Una armadura forjada con",
                ChatColor.of("#996699") + "esencias de criaturas",
                ChatColor.of("#996699") + "extrañas.",
                "",
                ChatColor.GRAY + "El conjunto completo de esta",
                ChatColor.GRAY + "armadura otorga: " + ChatColor.WHITE,
                ChatColor.of("#ff6600") + "" + ChatColor.BOLD + "+2 " + ChatColor.GOLD + ChatColor.BOLD + "Corazones Extras",
                ""
        ));

        meta.addAttributeModifier(Attribute.GENERIC_ARMOR, new AttributeModifier(
                UUID.randomUUID(),
                "armor_modifier",
                3,
                AttributeModifier.Operation.ADD_NUMBER,
                EquipmentSlot.HEAD
        ));
        meta.addAttributeModifier(Attribute.GENERIC_ARMOR_TOUGHNESS, new AttributeModifier(
                UUID.randomUUID(),
                "toughness_modifier",
                4,
                AttributeModifier.Operation.ADD_NUMBER,
                EquipmentSlot.HEAD
        ));
        meta.addAttributeModifier(Attribute.GENERIC_KNOCKBACK_RESISTANCE, new AttributeModifier(
                UUID.randomUUID(),
                "knockback_modifier",
                0.1,
                AttributeModifier.Operation.ADD_NUMBER,
                EquipmentSlot.HEAD
        ));
        meta.addAttributeModifier(Attribute.GENERIC_MAX_HEALTH, new AttributeModifier(
                UUID.randomUUID(),
                "health_modifier",
                1,
                AttributeModifier.Operation.ADD_NUMBER,
                EquipmentSlot.HEAD
        ));
        meta.setCustomModelData(2);
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack createCorruptedChestplate() {
        ItemStack item = new ItemStack(Material.NETHERITE_CHESTPLATE);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ChatColor.of("#9966ff") + "" + ChatColor.BOLD + "Corrupted Netherite Chestplate");
        meta.setLore(Arrays.asList(
                "",
                ChatColor.of("#996699") + "Una armadura forjada con",
                ChatColor.of("#996699") + "esencias de criaturas",
                ChatColor.of("#996699") + "extrañas.",
                "",
                ChatColor.GRAY + "El conjunto completo de esta",
                ChatColor.GRAY + "armadura otorga: " + ChatColor.WHITE,
                ChatColor.of("#ff6600") + "" + ChatColor.BOLD + "+2 " + ChatColor.GOLD + ChatColor.BOLD + "Corazones Extras",
                ""
        ));

        meta.addAttributeModifier(Attribute.GENERIC_ARMOR, new AttributeModifier(
                UUID.randomUUID(),
                "armor_modifier",
                9,
                AttributeModifier.Operation.ADD_NUMBER,
                EquipmentSlot.CHEST
        ));
        meta.addAttributeModifier(Attribute.GENERIC_ARMOR_TOUGHNESS, new AttributeModifier(
                UUID.randomUUID(),
                "toughness_modifier",
                4,
                AttributeModifier.Operation.ADD_NUMBER,
                EquipmentSlot.CHEST
        ));
        meta.addAttributeModifier(Attribute.GENERIC_KNOCKBACK_RESISTANCE, new AttributeModifier(
                UUID.randomUUID(),
                "knockback_modifier",
                0.1,
                AttributeModifier.Operation.ADD_NUMBER,
                EquipmentSlot.CHEST
        ));
        meta.addAttributeModifier(Attribute.GENERIC_MAX_HEALTH, new AttributeModifier(
                UUID.randomUUID(),
                "health_modifier",
                1,
                AttributeModifier.Operation.ADD_NUMBER,
                EquipmentSlot.CHEST
        ));
        meta.setCustomModelData(2);
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack createCorruptedLeggings() {
        ItemStack item = new ItemStack(Material.NETHERITE_LEGGINGS);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ChatColor.of("#9966ff") + "" + ChatColor.BOLD + "Corrupted Netherite Leggings");
        meta.setLore(Arrays.asList(
                "",
                ChatColor.of("#996699") + "Una armadura forjada con",
                ChatColor.of("#996699") + "esencias de criaturas",
                ChatColor.of("#996699") + "extrañas.",
                "",
                ChatColor.GRAY + "El conjunto completo de esta",
                ChatColor.GRAY + "armadura otorga: " + ChatColor.WHITE,
                ChatColor.of("#ff6600") + "" + ChatColor.BOLD + "+2 " + ChatColor.GOLD + ChatColor.BOLD + "Corazones Extras",
                ""
        ));

        meta.addAttributeModifier(Attribute.GENERIC_ARMOR, new AttributeModifier(
                UUID.randomUUID(),
                "armor_modifier",
                6,
                AttributeModifier.Operation.ADD_NUMBER,
                EquipmentSlot.LEGS
        ));
        meta.addAttributeModifier(Attribute.GENERIC_ARMOR_TOUGHNESS, new AttributeModifier(
                UUID.randomUUID(),
                "toughness_modifier",
                4,
                AttributeModifier.Operation.ADD_NUMBER,
                EquipmentSlot.LEGS
        ));
        meta.addAttributeModifier(Attribute.GENERIC_KNOCKBACK_RESISTANCE, new AttributeModifier(
                UUID.randomUUID(),
                "knockback_modifier",
                0.1,
                AttributeModifier.Operation.ADD_NUMBER,
                EquipmentSlot.LEGS
        ));
        meta.addAttributeModifier(Attribute.GENERIC_MAX_HEALTH, new AttributeModifier(
                UUID.randomUUID(),
                "health_modifier",
                1,
                AttributeModifier.Operation.ADD_NUMBER,
                EquipmentSlot.LEGS
        ));
        meta.setCustomModelData(2);
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack createCorruptedBoots() {
        ItemStack item = new ItemStack(Material.NETHERITE_BOOTS);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ChatColor.of("#9966ff") + "" + ChatColor.BOLD + "Corrupted Netherite Boots");
        meta.setLore(Arrays.asList(
                "",
                ChatColor.of("#996699") + "Una armadura forjada con",
                ChatColor.of("#996699") + "esencias de criaturas",
                ChatColor.of("#996699") + "extrañas.",
                "",
                ChatColor.GRAY + "El conjunto completo de esta",
                ChatColor.GRAY + "armadura otorga: " + ChatColor.WHITE,
                ChatColor.of("#ff6600") + "" + ChatColor.BOLD + "+2 " + ChatColor.GOLD + ChatColor.BOLD + "Corazones Extras",
                ""
        ));

        meta.addAttributeModifier(Attribute.GENERIC_ARMOR, new AttributeModifier(
                UUID.randomUUID(),
                "armor_modifier",
                4,
                AttributeModifier.Operation.ADD_NUMBER,
                EquipmentSlot.FEET
        ));
        meta.addAttributeModifier(Attribute.GENERIC_ARMOR_TOUGHNESS, new AttributeModifier(
                UUID.randomUUID(),
                "toughness_modifier",
                4,
                AttributeModifier.Operation.ADD_NUMBER,
                EquipmentSlot.FEET
        ));
        meta.addAttributeModifier(Attribute.GENERIC_KNOCKBACK_RESISTANCE, new AttributeModifier(
                UUID.randomUUID(),
                "knockback_modifier",
                0.1,
                AttributeModifier.Operation.ADD_NUMBER,
                EquipmentSlot.FEET
        ));
        meta.addAttributeModifier(Attribute.GENERIC_MAX_HEALTH, new AttributeModifier(
                UUID.randomUUID(),
                "health_modifier",
                1,
                AttributeModifier.Operation.ADD_NUMBER,
                EquipmentSlot.FEET
        ));
        meta.setCustomModelData(2);
        item.setItemMeta(meta);
        return item;
    }

    // En tu clase CorruptedArmor, añade este método para verificar si es la armadura custom
    public static boolean isCorruptedArmor(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;

        ItemMeta meta = item.getItemMeta();
        if (!meta.hasDisplayName()) return false;

        String name = meta.getDisplayName();
        return name.contains("Corrupted Netherite");
    }
}