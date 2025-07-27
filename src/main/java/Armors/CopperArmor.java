package Armors;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.event.Listener;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.UUID;

public class CopperArmor implements Listener {
    private final JavaPlugin plugin;

    public CopperArmor(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public static ItemStack createCopperHelmet() {
        ItemStack item = new ItemStack(Material.CHAINMAIL_HELMET);
        ItemMeta meta = item.getItemMeta();

        // Nombre y lore
        meta.setDisplayName(ChatColor.of("#b87333") + "" + ChatColor.BOLD + "Casco de Cobre");
        meta.setLore(Arrays.asList(
                "",
                ChatColor.of("#cc9966") + "Una armadura que puede",
                ChatColor.of("#cc9966") + "proteger contra la ira",
                ChatColor.of("#cc9966") + "de los cielos.",
                "",
                ChatColor.GRAY + "El conjunto completo de esta",
                ChatColor.GRAY + "armadura otorga: " + ChatColor.WHITE,
                ChatColor.of("#ffcc66") + "" + ChatColor.BOLD + "+25% " + ChatColor.of("#dddddd") + ChatColor.BOLD + "Resistencia contra Rayos",
                ""
        ));

        meta.addAttributeModifier(Attribute.GENERIC_ARMOR, new AttributeModifier(
                UUID.randomUUID(),
                "armor_modifier",
                2,
                AttributeModifier.Operation.ADD_NUMBER,
                EquipmentSlot.HEAD
        ));
        meta.setCustomModelData(2);
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack createCopperChestplate() {
        ItemStack item = new ItemStack(Material.CHAINMAIL_CHESTPLATE);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ChatColor.of("#b87333") + "" + ChatColor.BOLD + "Peto de Cobre");
        meta.setLore(Arrays.asList(
                "",
                ChatColor.of("#cc9966") + "Una armadura que puede",
                ChatColor.of("#cc9966") + "proteger contra la ira",
                ChatColor.of("#cc9966") + "de los cielos.",
                "",
                ChatColor.GRAY + "El conjunto completo de esta",
                ChatColor.GRAY + "armadura otorga: " + ChatColor.WHITE,
                ChatColor.of("#ffcc66") + "" + ChatColor.BOLD + "+25% " + ChatColor.of("#dddddd") + ChatColor.BOLD + "Resistencia contra Rayos",
                ""
        ));

        meta.addAttributeModifier(Attribute.GENERIC_ARMOR, new AttributeModifier(
                UUID.randomUUID(),
                "armor_modifier",
                4,
                AttributeModifier.Operation.ADD_NUMBER,
                EquipmentSlot.CHEST
        ));
        meta.setCustomModelData(2);
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack createCopperLeggings() {
        ItemStack item = new ItemStack(Material.CHAINMAIL_LEGGINGS);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ChatColor.of("#b87333") + "" + ChatColor.BOLD + "Grebas de Cobre");
        meta.setLore(Arrays.asList(
                "",
                ChatColor.of("#cc9966") + "Una armadura que puede",
                ChatColor.of("#cc9966") + "proteger contra la ira",
                ChatColor.of("#cc9966") + "de los cielos.",
                "",
                ChatColor.GRAY + "El conjunto completo de esta",
                ChatColor.GRAY + "armadura otorga: " + ChatColor.WHITE,
                ChatColor.of("#ffcc66") + "" + ChatColor.BOLD + "+25% " + ChatColor.of("#dddddd") + ChatColor.BOLD + "Resistencia contra Rayos",
                ""
        ));

        meta.addAttributeModifier(Attribute.GENERIC_ARMOR, new AttributeModifier(
                UUID.randomUUID(),
                "armor_modifier",
                4,
                AttributeModifier.Operation.ADD_NUMBER,
                EquipmentSlot.LEGS
        ));
        meta.setCustomModelData(2);
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack createCopperBoots() {
        ItemStack item = new ItemStack(Material.CHAINMAIL_BOOTS);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ChatColor.of("#b87333") + "" + ChatColor.BOLD + "Botas de Cobre");
        meta.setLore(Arrays.asList(
                "",
                ChatColor.of("#cc9966") + "Una armadura que puede",
                ChatColor.of("#cc9966") + "proteger contra la ira",
                ChatColor.of("#cc9966") + "de los cielos.",
                "",
                ChatColor.GRAY + "El conjunto completo de esta",
                ChatColor.GRAY + "armadura otorga: " + ChatColor.WHITE,
                ChatColor.of("#ffcc66") + "" + ChatColor.BOLD + "+25% " + ChatColor.of("#dddddd") + ChatColor.BOLD + "Resistencia contra Rayos",
                ""
        ));

        meta.addAttributeModifier(Attribute.GENERIC_ARMOR, new AttributeModifier(
                UUID.randomUUID(),
                "armor_modifier",
                2,
                AttributeModifier.Operation.ADD_NUMBER,
                EquipmentSlot.FEET
        ));
        meta.setCustomModelData(2);
        item.setItemMeta(meta);
        return item;
    }
}
