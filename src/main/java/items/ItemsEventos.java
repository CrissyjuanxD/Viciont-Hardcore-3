package items;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.FoodComponent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;

public class ItemsEventos implements Listener {

    private final JavaPlugin plugin;
    private final NamespacedKey manzanaKey;
    private final NamespacedKey plumaKey;

    public ItemsEventos(JavaPlugin plugin) {
        this.plugin = plugin;
        this.manzanaKey = new NamespacedKey(plugin, "manzana_vida");
        this.plumaKey = new NamespacedKey(plugin, "pluma_levitacion");
    }

    // --------------------------------------------------------
    // CREACIÓN DE ITEMS
    // --------------------------------------------------------

    public ItemStack createManzanaVida() {
        ItemStack item = new ItemStack(Material.APPLE);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ChatColor.of("#EC6451") + ChatColor.BOLD.toString() + "Manzana de la Vida");

            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(ChatColor.of("#EBAB65") + "Consúmela para obtener:");
            lore.add(ChatColor.of("#EC6451") + "• " + ChatColor.of("#EBAB65") + "Curación Instantanea I");
            meta.setLore(lore);

            meta.setCustomModelData(100);

            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

            FoodComponent food = meta.getFood();
            food.setCanAlwaysEat(true);
            meta.setFood(food);

            PersistentDataContainer data = meta.getPersistentDataContainer();
            data.set(manzanaKey, PersistentDataType.BYTE, (byte) 1);

            item.setItemMeta(meta);
        }
        return item;
    }

    public ItemStack createPlumaLevitacion() {
        ItemStack item = new ItemStack(Material.FEATHER);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ChatColor.of("#518BEC") + ChatColor.BOLD.toString() + "Pluma de Levitación");

            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(ChatColor.of("#65BCEB") + "Click derecho para usar.");
            lore.add(ChatColor.of("#65BCEB") + "Te dará la habilidad de:");
            lore.add(ChatColor.of("#518BEC") + "• " + ChatColor.of("#65BCEB") + "Levitación XI " + ChatColor.GRAY + "(1 segundos)");
            meta.setLore(lore);

            meta.setCustomModelData(100);

            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

            PersistentDataContainer data = meta.getPersistentDataContainer();
            data.set(plumaKey, PersistentDataType.BYTE, (byte) 1);

            item.setItemMeta(meta);
        }
        return item;
    }


    public boolean isManzanaVida(ItemStack item) {
        if (item == null || item.getType() != Material.APPLE) return false;
        if (!item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(manzanaKey, PersistentDataType.BYTE);
    }

    public boolean isPlumaLevitacion(ItemStack item) {
        if (item == null || item.getType() != Material.FEATHER) return false;
        if (!item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(plumaKey, PersistentDataType.BYTE);
    }


    @EventHandler
    public void onConsumeManzana(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (isManzanaVida(item)) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.INSTANT_HEALTH, 1, 0, true, true));

            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 2.0f);
            player.playSound(player.getLocation(), Sound.ENTITY_ARMADILLO_EAT, 1.0f, 2.0f);
        }
    }


    @EventHandler
    public void onInteractPluma(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        Action action = event.getAction();

        if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
            if (isPlumaLevitacion(item)) {
                event.setCancelled(true);

                if (player.hasCooldown(Material.FEATHER)) {
                    return;
                }

                player.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 21, 10, true, true));

                player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 1.0f, 2.0f);
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_BREATH, 0.5f, 2.0f);

                if (player.getGameMode() != org.bukkit.GameMode.CREATIVE) {
                    item.setAmount(item.getAmount() - 1);
                }

                player.setCooldown(Material.FEATHER, 20);
            }
        }
    }
}