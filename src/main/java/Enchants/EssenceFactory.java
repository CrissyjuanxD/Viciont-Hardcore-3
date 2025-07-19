package Enchants;

import org.bukkit.Material;
import org.bukkit.inventory.ItemRarity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.ChatColor;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class EssenceFactory {

    public static ItemStack createProtectionEssence() {
        return createEssence("Esencia de Protección", 2);
    }

    public static ItemStack createUnbreakingEssence() {
        return createEssence("Esencia de Irrompibilidad", 3);
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

    public static ItemStack createPowerEssence() {
        return createEssence("Esencia de Poder", 14);
    }

    private static ItemStack createEssence(String name, int customModelData) {
        ItemStack essence = new ItemStack(Material.IRON_NUGGET);
        ItemMeta meta = essence.getItemMeta();
        name = ChatColor.BLUE + name;
        meta.setDisplayName(name);
        meta.setCustomModelData(customModelData);
        int usos = new Random().nextInt(3) + 1;

        // lore con usos restantes
        List<String> lore = new ArrayList<>();
        lore.add(" ");
        lore.add(ChatColor.DARK_PURPLE + "Con esta Esencia podrás desbloquear");
        lore.add(ChatColor.DARK_PURPLE + "el encantamiento correspondiente");
        lore.add(ChatColor.DARK_PURPLE + "en la " + ChatColor.GOLD + "Mesa de Encantamientos Mejorada");
        lore.add(" ");
        lore.add(ChatColor.GRAY + "Usos restantes: " + usos);
        meta.setLore(lore);
        meta.setRarity(ItemRarity.RARE);

        //contador de usos entre el 1 y el 3
        PersistentDataContainer data = meta.getPersistentDataContainer();
        data.set(new NamespacedKey("vicionthardcore3", "uses"), PersistentDataType.INTEGER, usos);

        essence.setItemMeta(meta);
        return essence;
    }

    public static ItemStack createVoidEssence() {
        ItemStack essence = new ItemStack(Material.IRON_NUGGET);
        ItemMeta meta = essence.getItemMeta();
        meta.setDisplayName(ChatColor.DARK_GRAY + "Esencia Vacía");
        meta.setCustomModelData(20);

        // lore vacía
        List<String> lore = new ArrayList<>();
        lore.add(" ");
        lore.add(ChatColor.DARK_PURPLE + "Esta esencia vacía que puede");
        lore.add(ChatColor.DARK_PURPLE + "que puede llegar a alcanzar");
        lore.add(ChatColor.DARK_PURPLE + "poderes inimaginables");
        lore.add(" ");
        meta.setLore(lore);
        meta.setRarity(ItemRarity.RARE);

        essence.setItemMeta(meta);
        return essence;
    }
}
