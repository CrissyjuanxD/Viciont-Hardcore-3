package items;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;
import vct.hardcore3.ViciontHardcore3;

import java.util.ArrayList;
import java.util.List;

public class DoubleLifeTotem implements Listener {

    private final ViciontHardcore3 plugin;
    private final NamespacedKey usesKey;

    public DoubleLifeTotem(ViciontHardcore3 plugin) {
        this.plugin = plugin;
        this.usesKey = new NamespacedKey(plugin, "double_life_totem_uses");
    }

    public ItemStack createDoubleLifeTotem() {
        ItemStack totem = new ItemStack(Material.TOTEM_OF_UNDYING);
        ItemMeta meta = totem.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ChatColor.of("#9966ff") + ChatColor.BOLD.toString() + "Tótem de Doble Vida");

            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(ChatColor.of("#cc0066") + "Este tótem se activará");
            lore.add(ChatColor.of("#cc0066") + ChatColor.BOLD.toString() + "dos veces.");
            lore.add("");
            lore.add(ChatColor.GRAY + ChatColor.BOLD.toString() + "Úsalo en el momento");
            lore.add(ChatColor.GRAY + ChatColor.BOLD.toString() + "indicado.");
            meta.setLore(lore);

            // Asignar CustomModelData inicial (2)
            meta.setCustomModelData(2);

            // Guardar que tiene 2 usos
            PersistentDataContainer data = meta.getPersistentDataContainer();
            data.set(usesKey, PersistentDataType.INTEGER, 2);

            totem.setItemMeta(meta);
        }
        return totem;
    }

    public boolean isDoubleLifeTotem(ItemStack item) {
        if (item == null || item.getType() != Material.TOTEM_OF_UNDYING) return false;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;

        PersistentDataContainer data = meta.getPersistentDataContainer();
        return data.has(usesKey, PersistentDataType.INTEGER);
    }

    private int getUsesLeft(ItemStack item) {
        if (!isDoubleLifeTotem(item)) return 0;
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer data = meta.getPersistentDataContainer();
        return data.getOrDefault(usesKey, PersistentDataType.INTEGER, 0);
    }

    private void setUsesLeft(ItemStack item, int uses) {
        if (!isDoubleLifeTotem(item)) return;
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer data = meta.getPersistentDataContainer();
        data.set(usesKey, PersistentDataType.INTEGER, uses);
        item.setItemMeta(meta);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onTotemUse(EntityResurrectEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();

        // Obtener ítems en ambas manos
        ItemStack mainHandItem = player.getInventory().getItemInMainHand();
        ItemStack offHandItem = player.getInventory().getItemInOffHand();

        // Si la mano principal tiene un tótem, se activa éste (incluso si es normal)
        if (mainHandItem != null && mainHandItem.getType() == Material.TOTEM_OF_UNDYING) {
            // Si es un tótem normal (sin la marca de doble vida), se activa el normal y no procesamos el otro
            if (!isDoubleLifeTotem(mainHandItem)) {
                return;
            }
            // En caso de que la mano principal sea el tótem de doble vida, se procesa ese
            final boolean usedInMainHand = true;
            int usesLeft = getUsesLeft(mainHandItem);

            if (usesLeft > 1) {
                // Crear un nuevo tótem con 1 uso restante
                ItemStack newTotem = createDoubleLifeTotem();
                setUsesLeft(newTotem, 1);
                modifyTotemAfterFirstUse(newTotem);

                // Al usar lambda, usamos variables finales o efectivamente finales
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    player.getInventory().setItemInMainHand(newTotem);
                }, 1L);
            } else if (usesLeft == 1) {
                // En el segundo uso, se consume el tótem de doble vida
                setUsesLeft(mainHandItem, 0);
            }
            return;
        }

        // Si la mano principal no tenía un tótem, se verifica la off-hand
        if (offHandItem != null && offHandItem.getType() == Material.TOTEM_OF_UNDYING && isDoubleLifeTotem(offHandItem)) {
            final boolean usedInMainHand = false; // variable para la lambda (aunque no se usa, se deja por claridad)
            int usesLeft = getUsesLeft(offHandItem);

            if (usesLeft > 1) {
                ItemStack newTotem = createDoubleLifeTotem();
                setUsesLeft(newTotem, 1);
                modifyTotemAfterFirstUse(newTotem);

                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    player.getInventory().setItemInOffHand(newTotem);
                }, 1L);
            } else if (usesLeft == 1) {
                setUsesLeft(offHandItem, 0);
            }
        }
    }

    private void modifyTotemAfterFirstUse(ItemStack totem) {
        ItemMeta meta = totem.getItemMeta();
        if (meta != null) {
            // Cambiar CustomModelData a 1
            meta.setCustomModelData(1);

            // Agregar un encantamiento invisible
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);

            // Actualizar el lore
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(ChatColor.of("#cc0066") + "Este tótem se activará");
            lore.add(ChatColor.of("#cc0066") + ChatColor.BOLD.toString() + "una vez más.");
            lore.add("");
            lore.add(ChatColor.GRAY + ChatColor.BOLD.toString() + "Úsalo en el momento");
            lore.add(ChatColor.GRAY + ChatColor.BOLD.toString() + "indicado.");
            meta.setLore(lore);

            totem.setItemMeta(meta);
        }
    }
}
