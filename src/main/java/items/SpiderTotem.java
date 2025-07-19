package items;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import vct.hardcore3.ViciontHardcore3;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class SpiderTotem implements Listener {

    private final JavaPlugin plugin;
    private final NamespacedKey spiderTotemKey;

    public SpiderTotem(JavaPlugin plugin) {
        this.plugin = plugin;
        this.spiderTotemKey = new NamespacedKey(plugin, "spider_totem");
    }

    public ItemStack createSpiderTotem() {
        ItemStack totem = new ItemStack(Material.TOTEM_OF_UNDYING);
        ItemMeta meta = totem.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ChatColor.of("#66ff99") + ChatColor.BOLD.toString() + "Spider Totem");

            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(ChatColor.of("#33cc99") + "Este tótem te otorgará");
            lore.add(ChatColor.of("#663366") + ChatColor.BOLD.toString() + "Absorción 3 " + ChatColor.of("#33cc99") + "por " + ChatColor.of("#663366") + ChatColor.BOLD + "15 " + ChatColor.of("#33cc99") + "segundos.");
            lore.add("");
            lore.add(ChatColor.GRAY + ChatColor.BOLD.toString() + "No elimina efectos al usarse.");
            lore.add("");
            meta.setLore(lore);

            meta.setCustomModelData(4);
            PersistentDataContainer data = meta.getPersistentDataContainer();
            data.set(spiderTotemKey, PersistentDataType.BYTE, (byte) 1);

            totem.setItemMeta(meta);
        }
        return totem;
    }

    public boolean isSpiderTotem(ItemStack item) {
        if (item == null || item.getType() != Material.TOTEM_OF_UNDYING) return false;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;

        PersistentDataContainer data = meta.getPersistentDataContainer();
        return data.has(spiderTotemKey, PersistentDataType.BYTE);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onTotemUse(EntityResurrectEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();

        ItemStack usedTotem = null;
        boolean isMainHand = false;

        if (isSpiderTotem(player.getInventory().getItemInMainHand())) {
            usedTotem = player.getInventory().getItemInMainHand();
            isMainHand = true;
        } else if (isSpiderTotem(player.getInventory().getItemInOffHand())) {
            usedTotem = player.getInventory().getItemInOffHand();
        }

        if (usedTotem == null) return;

        // Guardar los efectos actuales ANTES de que el totem los elimine
        Collection<PotionEffect> currentEffects = new ArrayList<>(player.getActivePotionEffects());

        new BukkitRunnable() {
            @Override
            public void run() {
                // 1. Restaurar todos los efectos que tenía antes
                for (PotionEffect effect : currentEffects) {
                    player.addPotionEffect(effect);
                }

                player.addPotionEffect(new PotionEffect(
                        PotionEffectType.ABSORPTION,
                        20 * 15,
                        2,
                        true,
                        true
                ));
            }
        }.runTaskLater(plugin, 1L);
    }
}
