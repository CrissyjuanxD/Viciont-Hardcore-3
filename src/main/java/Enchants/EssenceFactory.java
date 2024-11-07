package Enchants;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class EssenceFactory {

    // Método para crear la esencia de Protección
    public static ItemStack createProtectionEssence() {
        return createEssence("Esencia de Protección", 2);
    }

    // Método para crear la esencia de Irrompibilidad
    public static ItemStack createUnbreakingEssence() {
        return createEssence("Esencia de Irrompibilidad", 3);
    }

    // Método para crear la esencia de Reparación (Mending)
    public static ItemStack createMendingEssence() {
        return createEssence("Esencia de Reparación", 4);
    }

    // Método para crear la esencia de Eficiencia
    public static ItemStack createEfficiencyEssence() {
        return createEssence("Esencia de Eficiencia", 5);
    }

    // Método para crear la esencia de Fortuna
    public static ItemStack createFortuneEssence() {
        return createEssence("Esencia de Fortuna", 6);
    }

    // Método para crear la esencia de Filo (Sharpness)
    public static ItemStack createSharpnessEssence() {
        return createEssence("Esencia de Filo", 7);
    }

    // Método para crear la esencia de Castigo (Smite)
    public static ItemStack createSmiteEssence() {
        return createEssence("Esencia de Castigo", 8);
    }

    // Método para crear la esencia de Perdición de los Artrópodos (Bane of Arthropods)
    public static ItemStack createBaneOfArthropodsEssence() {
        return createEssence("Esencia de Perdición de los Artrópodos", 9);
    }

    // Método para crear la esencia de Caída de Pluma (Feather Falling)
    public static ItemStack createFeatherFallingEssence() {
        return createEssence("Esencia de Caída de Pluma", 10);
    }

    // Método para crear la esencia de Saqueo (Looting)
    public static ItemStack createLootingEssence() {
        return createEssence("Esencia de Saqueo", 11);
    }

    // Método para crear la esencia de Propulsión Acuática (Depth Strider)
    public static ItemStack createDepthStriderEssence() {
        return createEssence("Esencia de Agilidad Acuática", 12);
    }

    // Método para crear la esencia de Toque de Seda (Silk Touch)
    public static ItemStack createSilkTouchEssence() {
        return createEssence("Esencia de Toque de Seda", 13);
    }

    // Método para crear la esencia de Poder (Power)
    public static ItemStack createPowerEssence() {
        return createEssence("Esencia de Poder", 14);
    }

    // Método genérico para crear una esencia con nombre, brillo y CustomModelData específicos
    private static ItemStack createEssence(String name, int customModelData) {
        ItemStack essence = new ItemStack(Material.IRON_NUGGET);
        ItemMeta meta = essence.getItemMeta();
        meta.setDisplayName(name);
        meta.setCustomModelData(customModelData);

        // Agregar brillo al ítem con un encantamiento oculto
        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

        // Iniciar el contador de usos en 0
        PersistentDataContainer data = meta.getPersistentDataContainer();
        data.set(new NamespacedKey("vicionthardcore3", "uses"), PersistentDataType.INTEGER, 0);

        essence.setItemMeta(meta);
        return essence;
    }
}
