package items;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.UUID;

public class EnderiteTools {

    // Colores temáticos del End
    private static final ChatColor PRIMARY_COLOR = ChatColor.of("#9c59d1");
    private static final ChatColor SECONDARY_COLOR = ChatColor.of("#5d24ac");
    private static final ChatColor LORE_COLOR = ChatColor.of("#c78dff");

    public static ItemStack createEnderiteSword() {
        ItemStack item = new ItemStack(Material.NETHERITE_SWORD);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(PRIMARY_COLOR + "" + ChatColor.BOLD + "Espada de Enderite");
        meta.setLore(Arrays.asList(
                "",
                LORE_COLOR + "Forjada con los restos de",
                LORE_COLOR + "criaturas del End.",
                "",
                SECONDARY_COLOR + "» Daño: " + ChatColor.WHITE + "10",
                SECONDARY_COLOR + "» Velocidad de ataque: " + ChatColor.WHITE + "1.7"
        ));

        // Atributos mejorados (+2 sobre netherite)
        meta.addAttributeModifier(Attribute.GENERIC_ATTACK_DAMAGE, new AttributeModifier(
                UUID.randomUUID(),
                "attack_damage",
                10,  // Netherite: 8, Enderite: 10
                AttributeModifier.Operation.ADD_NUMBER,
                EquipmentSlot.HAND
        ));

        meta.addAttributeModifier(Attribute.GENERIC_ATTACK_SPEED, new AttributeModifier(
                UUID.randomUUID(),
                "attack_speed",
                -2.3,  // Velocidad de ataque
                AttributeModifier.Operation.ADD_NUMBER,
                EquipmentSlot.HAND
        ));

        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        meta.setCustomModelData(2); // Modelo custom para texturas
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack createEnderiteAxe() {
        ItemStack item = new ItemStack(Material.NETHERITE_AXE);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(PRIMARY_COLOR + "" + ChatColor.BOLD + "Hacha de Enderite");
        meta.setLore(Arrays.asList(
                "",
                LORE_COLOR + "Forjada con los restos de",
                LORE_COLOR + "criaturas del End.",
                "",
                SECONDARY_COLOR + "» Daño: " + ChatColor.WHITE + "11",
                SECONDARY_COLOR + "» Velocidad de ataque: " + ChatColor.WHITE + "0.9",
                SECONDARY_COLOR + "» Eficiencia al minar: " + ChatColor.WHITE + "+1"

        ));

        meta.addAttributeModifier(Attribute.GENERIC_ATTACK_DAMAGE, new AttributeModifier(
                UUID.randomUUID(),
                "attack_damage",
                11,  // Netherite: 9, Enderite: 11
                AttributeModifier.Operation.ADD_NUMBER,
                EquipmentSlot.HAND
        ));

        meta.addAttributeModifier(Attribute.GENERIC_ATTACK_SPEED, new AttributeModifier(
                UUID.randomUUID(),
                "attack_speed",
                -3.1,
                AttributeModifier.Operation.ADD_NUMBER,
                EquipmentSlot.HAND
        ));

        meta.addAttributeModifier(Attribute.PLAYER_BLOCK_BREAK_SPEED, new AttributeModifier(
                UUID.randomUUID(),
                "axe_speed",
                1,  // Aumento de velocidad
                AttributeModifier.Operation.ADD_NUMBER,
                EquipmentSlot.HAND
        ));

        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        meta.setCustomModelData(3);
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack createEnderitePickaxe() {
        ItemStack item = new ItemStack(Material.NETHERITE_PICKAXE);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(PRIMARY_COLOR + "" + ChatColor.BOLD + "Pico de Enderite");
        meta.setLore(Arrays.asList(
                "",
                LORE_COLOR + "Forjada con los restos de",
                LORE_COLOR + "criaturas del End.",
                "",
                SECONDARY_COLOR + "» Eficiencia al minar: " + ChatColor.WHITE + "+1"
        ));

        // Atributo de eficiencia mejorada
        meta.addAttributeModifier(Attribute.PLAYER_BLOCK_BREAK_SPEED, new AttributeModifier(
                UUID.randomUUID(),
                "mining_speed",
                1,
                AttributeModifier.Operation.ADD_NUMBER,
                EquipmentSlot.HAND
        ));

        // La eficiencia real se maneja con enchantments
        meta.setCustomModelData(4);
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack createEnderiteShovel() {
        ItemStack item = new ItemStack(Material.NETHERITE_SHOVEL);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(PRIMARY_COLOR + "" + ChatColor.BOLD + "Pala de Enderite");
        meta.setLore(Arrays.asList(
                "",
                LORE_COLOR + "Forjada con los restos de",
                LORE_COLOR + "criaturas del End.",
                "",
                SECONDARY_COLOR + "» Eficiencia al minar: " + ChatColor.WHITE + "+1"
        ));

        meta.addAttributeModifier(Attribute.PLAYER_BLOCK_BREAK_SPEED, new AttributeModifier(
                UUID.randomUUID(),
                "dig_speed",
                1,
                AttributeModifier.Operation.ADD_NUMBER,
                EquipmentSlot.HAND
        ));

        meta.setCustomModelData(5);
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack createEnderiteHoe() {
        ItemStack item = new ItemStack(Material.NETHERITE_HOE);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(PRIMARY_COLOR + "" + ChatColor.BOLD + "Azada de Enderite");
        meta.setLore(Arrays.asList(
                "",
                LORE_COLOR + "Forjada con los restos de",
                LORE_COLOR + "criaturas del End.",
                "",
                SECONDARY_COLOR + "» Rango de ataque: " + ChatColor.WHITE + "+3"
        ));

        meta.addAttributeModifier(Attribute.PLAYER_ENTITY_INTERACTION_RANGE, new AttributeModifier(
                UUID.randomUUID(),
                "hoe_range",
                3,
                AttributeModifier.Operation.ADD_NUMBER,
                EquipmentSlot.HAND
        ));

        meta.setCustomModelData(6);
        item.setItemMeta(meta);
        return item;
    }
}
