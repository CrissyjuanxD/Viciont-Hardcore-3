package Habilidades;

import Handlers.DayHandler;
import items.CorruptedNetheriteItems;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class HabilidadesGUI implements Listener {

    private final JavaPlugin plugin;
    private final HabilidadesManager manager;
    private final DayHandler dayHandler;
    private final String GUI_TITLE = "\u3301\u3301" + ChatColor.WHITE + "\u3300";

    public HabilidadesGUI(JavaPlugin plugin, HabilidadesManager manager, DayHandler dayHandler) {
        this.plugin = plugin;
        this.manager = manager;
        this.dayHandler = dayHandler;
    }

    public void openHabilidadesGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 54, GUI_TITLE);

        fillGUIWithItems(gui, player);

        player.openInventory(gui);
        player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 0.5f, 1.0f);
    }

    private void fillGUIWithItems(Inventory gui, Player player) {
        int currentDay = dayHandler.getCurrentDay();

        gui.setItem(10, createHabilidadItem(player, HabilidadesType.VITALIDAD, 1, 4, currentDay));
        gui.setItem(12, createHabilidadItem(player, HabilidadesType.VITALIDAD, 2, 8, currentDay));
        gui.setItem(14, createHabilidadItem(player, HabilidadesType.VITALIDAD, 3, 14, currentDay));
        gui.setItem(16, createHabilidadItem(player, HabilidadesType.VITALIDAD, 4, 21, currentDay));

        gui.setItem(28, createHabilidadItem(player, HabilidadesType.RESISTENCIA, 1, 4, currentDay));
        gui.setItem(30, createHabilidadItem(player, HabilidadesType.RESISTENCIA, 2, 8, currentDay));
        gui.setItem(32, createHabilidadItem(player, HabilidadesType.RESISTENCIA, 3, 14, currentDay));
        gui.setItem(34, createHabilidadItem(player, HabilidadesType.RESISTENCIA, 4, 21, currentDay));

        gui.setItem(46, createHabilidadItem(player, HabilidadesType.AGILIDAD, 1, 4, currentDay));
        gui.setItem(48, createHabilidadItem(player, HabilidadesType.AGILIDAD, 2, 8, currentDay));
        gui.setItem(50, createHabilidadItem(player, HabilidadesType.AGILIDAD, 3, 14, currentDay));
        gui.setItem(52, createHabilidadItem(player, HabilidadesType.AGILIDAD, 4, 21, currentDay));
    }

    private ItemStack createHabilidadItem(Player player, HabilidadesType type, int level, int requiredDay, int currentDay) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();

        boolean isUnlocked = manager.hasHabilidad(player.getUniqueId(), type, level);
        boolean isDayUnlocked = currentDay >= requiredDay;
        boolean canUnlock = manager.canUnlock(player.getUniqueId(), type, level);

        int modelData = getModelData(type, level, isDayUnlocked);
        meta.setCustomModelData(modelData);

        String displayName = getDisplayName(type, level, isUnlocked, isDayUnlocked);
        meta.setDisplayName(displayName);

        List<String> lore = getLore(type, level, isUnlocked, isDayUnlocked, canUnlock);
        meta.setLore(lore);

        if (isUnlocked) {
            meta.addEnchant(Enchantment.LUCK_OF_THE_SEA, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }

        item.setItemMeta(meta);
        return item;
    }

    private int getModelData(HabilidadesType type, int level, boolean isDayUnlocked) {
        int baseModel = 0;

        switch (type) {
            case VITALIDAD:
                baseModel = level - 1;
                break;
            case RESISTENCIA:
                baseModel = 4 + (level - 1);
                break;
            case AGILIDAD:
                baseModel = 8 + (level - 1);
                break;
        }

        if (isDayUnlocked) {
            return 20000 + baseModel;
        } else {
            return 10000 + baseModel;
        }
    }

    private String getDisplayName(HabilidadesType type, int level, boolean isUnlocked, boolean isDayUnlocked) {
        String name = type.getDisplayName() + " Nivel " + level;

        if (isUnlocked) {
            return ChatColor.of("#C77DFF") + "" + ChatColor.BOLD + name + " ✓";
        } else if (isDayUnlocked) {
            return ChatColor.of("#9D4EDD") + "" + ChatColor.BOLD + name;
        } else {
            return ChatColor.of("#6B6B6B") + "" + ChatColor.BOLD + name;
        }
    }

    private List<String> getLore(HabilidadesType type, int level, boolean isUnlocked, boolean isDayUnlocked, boolean canUnlock) {
        List<String> lore = new ArrayList<>();

        if (isUnlocked) {
            lore.add(ChatColor.of("#7FFF00") + "✓ Desbloqueado");
            lore.add("");
        } else if (!isDayUnlocked) {
            lore.add(ChatColor.of("#4A4A4A") + "Bloqueado hasta el día " + getRequiredDay(level));
            lore.add("");
            return lore;
        } else if (!canUnlock) {
            lore.add(ChatColor.of("#FF6B6B") + "Debes desbloquear el nivel anterior");
            lore.add("");
            return lore;
        }

        lore.addAll(getHabilidadDescription(type, level));

        if (!isUnlocked && isDayUnlocked && canUnlock) {
            lore.add("");
            lore.add(ChatColor.of("#E0AAFF") + "Costo:");
            lore.add(ChatColor.of("#C77DFF") + "• " + getCostXP(level) + " niveles de experiencia");
            lore.add(ChatColor.of("#C77DFF") + "• 5 Corrupted Netherite Ingot");
            lore.add("");
            lore.add(ChatColor.of("#9D4EDD") + "Click para desbloquear");
        }

        return lore;
    }

    private List<String> getHabilidadDescription(HabilidadesType type, int level) {
        List<String> desc = new ArrayList<>();

        switch (type) {
            case VITALIDAD:
                switch (level) {
                    case 1:
                        desc.add(ChatColor.of("#E0AAFF") + "Otorga +2 corazones extras");
                        desc.add(ChatColor.of("#E0AAFF") + "permanentes");
                        break;
                    case 2:
                        desc.add(ChatColor.of("#E0AAFF") + "Otorga +2 corazones extras");
                        desc.add(ChatColor.of("#E0AAFF") + "permanentes");
                        break;
                    case 3:
                        desc.add(ChatColor.of("#E0AAFF") + "Otorga +2 corazones extras");
                        desc.add(ChatColor.of("#E0AAFF") + "permanentes y probabilidad");
                        desc.add(ChatColor.of("#E0AAFF") + "de Absorción III (10s)");
                        break;
                    case 4:
                        desc.add(ChatColor.of("#E0AAFF") + "Otorga +2 corazones extras");
                        desc.add(ChatColor.of("#E0AAFF") + "permanentes y probabilidad");
                        desc.add(ChatColor.of("#E0AAFF") + "de Absorción IV (10s)");
                        break;
                }
                break;
            case RESISTENCIA:
                switch (level) {
                    case 1:
                        desc.add(ChatColor.of("#E0AAFF") + "Otorga Resistencia I");
                        desc.add(ChatColor.of("#E0AAFF") + "permanente");
                        break;
                    case 2:
                        desc.add(ChatColor.of("#E0AAFF") + "15% de probabilidad de");
                        desc.add(ChatColor.of("#E0AAFF") + "bloquear proyectiles hostiles");
                        break;
                    case 3:
                        desc.add(ChatColor.of("#E0AAFF") + "15% de probabilidad de");
                        desc.add(ChatColor.of("#E0AAFF") + "bloquear golpes hostiles");
                        break;
                    case 4:
                        desc.add(ChatColor.of("#E0AAFF") + "Otorga Resistencia II");
                        desc.add(ChatColor.of("#E0AAFF") + "permanente");
                        break;
                }
                break;
            case AGILIDAD:
                switch (level) {
                    case 1:
                        desc.add(ChatColor.of("#E0AAFF") + "Otorga Velocidad I");
                        desc.add(ChatColor.of("#E0AAFF") + "permanente");
                        break;
                    case 2:
                        desc.add(ChatColor.of("#E0AAFF") + "Otorga Salto II");
                        desc.add(ChatColor.of("#E0AAFF") + "permanente");
                        break;
                    case 3:
                        desc.add(ChatColor.of("#E0AAFF") + "Permite realizar un");
                        desc.add(ChatColor.of("#E0AAFF") + "doble salto");
                        break;
                    case 4:
                        desc.add(ChatColor.of("#E0AAFF") + "Velocidad II permanente");
                        desc.add(ChatColor.of("#E0AAFF") + "y triple salto");
                        break;
                }
                break;
        }

        return desc;
    }

    private int getRequiredDay(int level) {
        switch (level) {
            case 1: return 4;
            case 2: return 8;
            case 3: return 14;
            case 4: return 21;
            default: return 1;
        }
    }

    private int getCostXP(int level) {
        switch (level) {
            case 1: return 35;
            case 2: return 45;
            case 3: return 55;
            case 4: return 65;
            default: return 35;
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(GUI_TITLE)) return;

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();

        if (slot >= 54) return;

        HabilidadesType type = getTypeFromSlot(slot);
        int level = getLevelFromSlot(slot);

        if (type == null || level == 0) return;

        handleHabilidadClick(player, type, level);
    }

    private HabilidadesType getTypeFromSlot(int slot) {
        if (slot == 10 || slot == 12 || slot == 14 || slot == 16) {
            return HabilidadesType.VITALIDAD;
        } else if (slot == 28 || slot == 30 || slot == 32 || slot == 34) {
            return HabilidadesType.RESISTENCIA;
        } else if (slot == 46 || slot == 48 || slot == 50 || slot == 52) {
            return HabilidadesType.AGILIDAD;
        }
        return null;
    }

    private int getLevelFromSlot(int slot) {
        if (slot == 10 || slot == 28 || slot == 46) return 1;
        if (slot == 12 || slot == 30 || slot == 48) return 2;
        if (slot == 14 || slot == 32 || slot == 50) return 3;
        if (slot == 16 || slot == 34 || slot == 52) return 4;
        return 0;
    }

    private void handleHabilidadClick(Player player, HabilidadesType type, int level) {
        if (manager.hasHabilidad(player.getUniqueId(), type, level)) {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            player.sendMessage(ChatColor.of("#FF6B6B") + "Ya tienes esta habilidad desbloqueada.");
            return;
        }

        int currentDay = dayHandler.getCurrentDay();
        int requiredDay = getRequiredDay(level);

        if (currentDay < requiredDay) {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            player.sendMessage(ChatColor.of("#6B6B6B") + "Esta habilidad se desbloquea en el día " + requiredDay + ".");
            return;
        }

        if (!manager.canUnlock(player.getUniqueId(), type, level)) {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            player.sendMessage(ChatColor.of("#FF6B6B") + "Debes desbloquear el nivel anterior primero.");
            return;
        }

        int costXP = getCostXP(level);
        if (player.getLevel() < costXP) {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            player.sendMessage(ChatColor.of("#FF6B6B") + "Necesitas " + costXP + " niveles de experiencia.");
            return;
        }

        if (!hasCorruptedNetherite(player, 5)) {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            player.sendMessage(ChatColor.of("#FF6B6B") + "Necesitas 5 Corrupted Netherite Ingot.");
            return;
        }

        removeCorruptedNetherite(player, 5);
        player.setLevel(player.getLevel() - costXP);

        manager.unlockHabilidad(player.getUniqueId(), type, level);

        player.closeInventory();

        HabilidadesEffects effects = new HabilidadesEffects(plugin);
        effects.playUnlockAnimation(player, type, level);

        openHabilidadesGUI(player);
    }

    private boolean hasCorruptedNetherite(Player player, int amount) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.isSimilar(CorruptedNetheriteItems.createCorruptedNetheriteIngot())) {
                count += item.getAmount();
                if (count >= amount) return true;
            }
        }
        return false;
    }

    private void removeCorruptedNetherite(Player player, int amount) {
        int remaining = amount;
        ItemStack corruptedIngot = CorruptedNetheriteItems.createCorruptedNetheriteIngot();

        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item != null && item.isSimilar(corruptedIngot)) {
                if (item.getAmount() > remaining) {
                    item.setAmount(item.getAmount() - remaining);
                    remaining = 0;
                    break;
                } else {
                    remaining -= item.getAmount();
                    player.getInventory().setItem(i, null);
                    if (remaining <= 0) break;
                }
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getView().getTitle().equals(GUI_TITLE)) {
            event.setCancelled(true);
        }
    }
}
