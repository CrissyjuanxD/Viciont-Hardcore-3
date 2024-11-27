package Enchants;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.ChatColor;

import java.util.ArrayList;
import java.util.List;

public class EssenceFactory {

    public static ItemStack createProtectionEssence() {
        return createEssence("Esencia de Protección", 2);
    }

    public static ItemStack createUnbreakingEssence() {
        return createEssence("Esencia de Irrompibilidad", 3);
    }

    public static ItemStack createMendingEssence() {
        return createEssence("Esencia de Reparación", 4);
    }

    public static ItemStack createEfficiencyEssence() {
        return createEssence("Esencia de Eficiencia", 5);
    }

    public static ItemStack createFortuneEssence() {
        return createEssence("Esencia de Fortuna", 6);
    }

    public static ItemStack createSharpnessEssence() {
        return createEssence("Esencia de Filo", 7);
    }

    public static ItemStack createSmiteEssence() {
        return createEssence("Esencia de Castigo", 8);
    }

    public static ItemStack createBaneOfArthropodsEssence() {
        return createEssence("Esencia de Perdición de los Artrópodos", 9);
    }

    public static ItemStack createFeatherFallingEssence() {
        return createEssence("Esencia de Caída de Pluma", 10);
    }

    public static ItemStack createLootingEssence() {
        return createEssence("Esencia de Saqueo", 11);
    }

    public static ItemStack createDepthStriderEssence() {
        return createEssence("Esencia de Agilidad Acuática", 12);
    }

    public static ItemStack createSilkTouchEssence() {
        return createEssence("Esencia de Toque de Seda", 13);
    }

    public static ItemStack createPowerEssence() {
        return createEssence("Esencia de Poder", 14);
    }

    private static ItemStack createEssence(String name, int customModelData) {
        ItemStack essence = new ItemStack(Material.IRON_NUGGET);
        ItemMeta meta = essence.getItemMeta();
        name = ChatColor.BLUE + name;
        meta.setDisplayName(name);
        meta.setCustomModelData(customModelData);

        // lore con usos restantes
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.DARK_PURPLE + "Con esta Esencia podrás encantar");
        lore.add(ChatColor.DARK_PURPLE + "cualquier ítem en la " + ChatColor.GOLD + "Mesa de Encantamientos Mejorada");
        lore.add(" ");
        lore.add(ChatColor.GRAY + "Usos restantes: 4");
        meta.setLore(lore);

        //contador de usos en 4
        PersistentDataContainer data = meta.getPersistentDataContainer();
        data.set(new NamespacedKey("vicionthardcore3", "uses"), PersistentDataType.INTEGER, 4);

        essence.setItemMeta(meta);
        return essence;
    }
}
