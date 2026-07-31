package Enchants;

import org.bukkit.Material;
import org.bukkit.inventory.ItemRarity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import net.md_5.bungee.api.ChatColor;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class EssenceFactory {

    public static ItemStack createProtectionEssence(int usos) { return createEssence("Esencia de Protección", 2, usos); }
    public static ItemStack createUnbreakingEssence(int usos) { return createEssence("Esencia de Irrompibilidad", 3, usos); }
    public static ItemStack createEfficiencyEssence(int usos) { return createEssence("Esencia de Eficiencia", 5, usos); }
    public static ItemStack createFortuneEssence(int usos) { return createEssence("Esencia de Fortuna", 6, usos); }
    public static ItemStack createSharpnessEssence(int usos) { return createEssence("Esencia de Filo", 7, usos); }
    public static ItemStack createSmiteEssence(int usos) { return createEssence("Esencia de Castigo", 8, usos); }
    public static ItemStack createBaneOfArthropodsEssence(int usos) { return createEssence("Esencia de Perdición de los Artrópodos", 9, usos); }
    public static ItemStack createFeatherFallingEssence(int usos) { return createEssence("Esencia de Caída de Pluma", 10, usos); }
    public static ItemStack createLootingEssence(int usos) { return createEssence("Esencia de Saqueo", 11, usos); }
    public static ItemStack createDepthStriderEssence(int usos) { return createEssence("Esencia de Agilidad Acuática", 12, usos); }
    public static ItemStack createPowerEssence(int usos) { return createEssence("Esencia de Poder", 14, usos); }

    private static ItemStack createEssence(String name, int customModelData, int usosSolicitados) {
        ItemStack essence = new ItemStack(Material.IRON_NUGGET);
        ItemMeta meta = essence.getItemMeta();

        String displayName = ChatColor.of("#bae1ff") + name;
        meta.setDisplayName(displayName);
        meta.setCustomModelData(customModelData);

        int usos = (usosSolicitados > 0) ? usosSolicitados : new Random().nextInt(3) + 1;

        List<String> lore = new ArrayList<>();
        lore.add(" ");
        lore.add(ChatColor.of("#c8b6ff") + "Con esta Esencia podrás desbloquear");
        lore.add(ChatColor.of("#c8b6ff") + "su encantamiento correspondiente en la");
        lore.add(ChatColor.of("#ffdfba") + "Mesa de Encantamientos Mejorada");
        lore.add(" ");
        lore.add(ChatColor.of("#ffb3ba") + "Usos restantes: " + ChatColor.WHITE + usos);
        meta.setLore(lore);
        meta.setRarity(ItemRarity.RARE);

        PersistentDataContainer data = meta.getPersistentDataContainer();
        data.set(new NamespacedKey("vicionthardcore3", "uses"), PersistentDataType.INTEGER, usos);

        essence.setItemMeta(meta);
        return essence;
    }

    public static ItemStack createVoidEssence() {
        ItemStack essence = new ItemStack(Material.IRON_NUGGET);
        ItemMeta meta = essence.getItemMeta();

        meta.setDisplayName(ChatColor.of("#bdb2ff") + "Esencia Vacía");
        meta.setCustomModelData(20);

        List<String> lore = new ArrayList<>();
        lore.add(" ");
        lore.add(ChatColor.of("#c8b6ff") + "Esta esencia vacía");
        lore.add(ChatColor.of("#c8b6ff") + "puede llegar a alcanzar");
        lore.add(ChatColor.of("#c8b6ff") + "poderes " + ChatColor.of("#ffb3ba") + "inimaginables...");
        lore.add(" ");
        meta.setLore(lore);
        meta.setRarity(ItemRarity.RARE);

        essence.setItemMeta(meta);
        return essence;
    }
}