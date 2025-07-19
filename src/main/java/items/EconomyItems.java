package items;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemRarity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;

public class EconomyItems {

    public static ItemStack createVithiumCoin() {
        ItemStack item = new ItemStack(Material.ECHO_SHARD);
        ItemMeta meta = item.getItemMeta();

        // Nombre del item con formato JSON-like
        meta.setDisplayName(ChatColor.YELLOW + "Vithiums " + ChatColor.GRAY + "۞");
        meta.setCustomModelData(2000);

        List<String> lore = new ArrayList<>();
        lore.add(" ");
        lore.add(ChatColor.GRAY + "Se dice que estas " + ChatColor.GOLD + "monedas");
        lore.add(ChatColor.GRAY + "eran utilizadas por " + ChatColor.DARK_PURPLE + "antiguas civilizaciones " + ChatColor.GRAY + "y que aún poseen");
        lore.add(ChatColor.GRAY + "un" + ChatColor.GOLD + " gran valor" + ChatColor.GRAY + ".");
        lore.add(" ");

        meta.setLore(lore);
        meta.setRarity(ItemRarity.EPIC);
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack createVithiumToken() {
        ItemStack item = new ItemStack(Material.ECHO_SHARD);
        ItemMeta meta = item.getItemMeta();

        // Nombre del item con formato JSON-like
        meta.setDisplayName(ChatColor.GOLD + "Vithiums Fichas " + ChatColor.GRAY + "۞");
        meta.setCustomModelData(2010);

        List<String> lore = new ArrayList<>();
        lore.add(" ");
        lore.add(ChatColor.GRAY + "Fichas que se utilizan para");
        lore.add(ChatColor.GRAY + "el " + ChatColor.YELLOW + "casino" + ChatColor.GRAY + ".");
        lore.add(" ");

        meta.setLore(lore);
        meta.setRarity(ItemRarity.EPIC);
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack createMochila() {
        ItemStack item = new ItemStack(Material.ECHO_SHARD);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ChatColor.of("#ffffcc") + "" + ChatColor.BOLD + "Mochila");
        meta.setCustomModelData(2020);

        List<String> lore = new ArrayList<>();
        lore.add(" ");
        lore.add(ChatColor.of("#33cccc") + "Inventario adicional");
        lore.add(" ");
        lore.add(ChatColor.GRAY + "Uso:");
        lore.add(ChatColor.GRAY + "> " + ChatColor.WHITE + "Click derecho");
        lore.add(" ");

        meta.setLore(lore);
        meta.setRarity(ItemRarity.EPIC);
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack createEnderBag() {
        ItemStack item = new ItemStack(Material.ECHO_SHARD);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ChatColor.of("#cc99cc") + "" + ChatColor.BOLD + "Ender Bag");
        meta.setCustomModelData(2030);

        List<String> lore = new ArrayList<>();
        lore.add(" ");
        lore.add(ChatColor.GRAY + "Abre el inventario del");
        lore.add(ChatColor.LIGHT_PURPLE + "Ender Chest" + ChatColor.GRAY + ".");
        lore.add(" ");
        lore.add(ChatColor.GRAY + "Uso:");
        lore.add(ChatColor.GRAY + "> " + ChatColor.WHITE + "Click derecho");
        lore.add(" ");

        meta.setLore(lore);
        meta.setRarity(ItemRarity.EPIC);
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack createGancho() {
        ItemStack item = new ItemStack(Material.FISHING_ROD);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ChatColor.of("#ffffcc") + "" + ChatColor.BOLD + "Gancho");
        meta.setCustomModelData(10);

        List<String> lore = new ArrayList<>();
        lore.add(" ");
        lore.add(ChatColor.of("#66cc99") + "Hace que te desplaces");
        lore.add(ChatColor.of("#66cc99") + "más rápido" + ChatColor.GRAY + ".");
        lore.add(" ");
        lore.add(ChatColor.GRAY + "Uso:");
        lore.add(ChatColor.GRAY + "> " + ChatColor.WHITE + "Click derecho");
        lore.add(" ");

        meta.setLore(lore);
        meta.setRarity(ItemRarity.EPIC);
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack createManzanaPanico() {
        ItemStack item = new ItemStack(Material.GOLDEN_APPLE);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ChatColor.of("#ff6666") + "" + ChatColor.BOLD + "Manzana del Pánico");
        meta.setCustomModelData(10);

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.of("#ffcc99") + "Esta manzana te otorga estos");
        lore.add(ChatColor.of("#ffcc99") + "efectos por 5 segundos" + ChatColor.GRAY + ":");
        lore.add("");
        lore.add(ChatColor.GRAY + "> " + ChatColor.of("#ffff66") + "Absorción 5");
        lore.add(ChatColor.GRAY + "> " + ChatColor.of("#cc99cc") + "Regeneración 3");
        lore.add(ChatColor.GRAY + "> " + ChatColor.of("#cc3300") + "Saturación 1");
        lore.add("");

        meta.setLore(lore);
        meta.setRarity(ItemRarity.EPIC);
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack createYunqueReparadorNivel1() {
        ItemStack item = new ItemStack(Material.ECHO_SHARD);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ChatColor.of("#99ccff") + "" + ChatColor.BOLD + "Yunque de Hierro Reparador");
        meta.setCustomModelData(2040);

        List<String> lore = new ArrayList<>();
        lore.add(" ");
        lore.add(ChatColor.of("#eeeeee") + "Repara un 25% la armadura");
        lore.add(ChatColor.of("#eeeeee") + "equipada" + ChatColor.GRAY + ".");
        lore.add(" ");
        lore.add(ChatColor.GRAY + "Uso:");
        lore.add(ChatColor.GRAY + "> " + ChatColor.WHITE + "Click derecho");
        lore.add(" ");

        meta.setLore(lore);
        meta.setRarity(ItemRarity.EPIC);
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack createYunqueReparadorNivel2() {
        ItemStack item = new ItemStack(Material.ECHO_SHARD);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ChatColor.of("#ffcc00") + "" + ChatColor.BOLD + "Yunque de Oro Reparador");
        meta.setCustomModelData(2050);

        List<String> lore = new ArrayList<>();
        lore.add(" ");
        lore.add(ChatColor.of("#ff9999") + "Repara un 100% la armadura");
        lore.add(ChatColor.of("#ff9999") + "equipada" + ChatColor.GRAY + ".");
        lore.add(" ");
        lore.add(ChatColor.GRAY + "Uso:");
        lore.add(ChatColor.GRAY + "> " + ChatColor.WHITE + "Click derecho");

        meta.setLore(lore);
        meta.setRarity(ItemRarity.EPIC);
        item.setItemMeta(meta);
        return item;
    }

    public static void applyPanicAppleEffects(Player player) {
        // Efectos de 5 segundos (100 ticks)
        player.addPotionEffect(new PotionEffect(
                PotionEffectType.ABSORPTION,
                100,
                4,
                false,
                false
        ));

        player.addPotionEffect(new PotionEffect(
                PotionEffectType.REGENERATION,
                100,
                2,
                false,
                false
        ));

        player.addPotionEffect(new PotionEffect(
                PotionEffectType.SATURATION,
                100,
                0,
                false,
                false
        ));
    }
}