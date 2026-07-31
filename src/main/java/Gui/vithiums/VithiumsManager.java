package Gui.vithiums;

import Handlers.DatabaseManager;
import items.EconomyItems;
import items.EconomyItemsFunctions;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class VithiumsManager implements Listener {

    private final JavaPlugin plugin;
    private final DatabaseManager dbManager;
    private File configFile;
    private FileConfiguration config;

    private boolean topEnabled = false;
    private final Set<String> excludedPlayers = new HashSet<>();
    private final NamespacedKey backpackKey;

    private List<Map.Entry<String, Integer>> topCache = new ArrayList<>();
    private final Map<UUID, Integer> cachedTotals = new ConcurrentHashMap<>();

    public VithiumsManager(JavaPlugin plugin, DatabaseManager dbManager) {
        this.plugin = plugin;
        this.dbManager = dbManager;
        this.backpackKey = new NamespacedKey(plugin, "backpack_uuid");
        loadConfig();

        Bukkit.getPluginManager().registerEvents(this, plugin);
        startTasks();
    }

    private void loadConfig() {
        File dir = new File(plugin.getDataFolder(), "vithiums");
        if (!dir.exists()) dir.mkdirs();

        configFile = new File(dir, "vithiums_config.yml");
        if (!configFile.exists()) {
            try { configFile.createNewFile(); } catch (IOException ignored) {}
        }

        config = YamlConfiguration.loadConfiguration(configFile);
        excludedPlayers.addAll(config.getStringList("excluded_players"));
    }

    public void saveConfig() {
        config.set("excluded_players", new ArrayList<>(excludedPlayers));
        try { config.save(configFile); } catch (IOException ignored) {}
    }

    public boolean isTopEnabled() { return topEnabled; }

    public void setTopEnabled(boolean topEnabled) {
        this.topEnabled = topEnabled;
        if (!topEnabled) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                VithiumsTopHud.removeHud(p);
            }
        } else {
            forceUpdateTop();
        }
    }

    public void addExcluded(String name) { excludedPlayers.add(name); saveConfig(); }
    public void removeExcluded(String name) { excludedPlayers.remove(name); saveConfig(); }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!e.getPlayer().isOnline()) return;

            updatePlayerTotalAsync(e.getPlayer());

            if (!topEnabled) {
                VithiumsTopHud.removeHud(e.getPlayer());
            } else {
                forceUpdateTop();
            }
        }, 120L);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        if (e.getView().getTitle().contains("Monedero")) {
            updatePlayerTotalAsync((Player) e.getPlayer());
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (e.getView().getTitle().contains("Monedero")) {
            Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> {
                if (e.getWhoClicked() instanceof Player) {
                    updatePlayerTotalAsync((Player) e.getWhoClicked());
                }
            }, 2L);
        }
    }

    public void updatePlayerTotalAsync(Player p) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            int total = calculatePhysicalVithiums(p);
            Integer lastTotal = cachedTotals.get(p.getUniqueId());

            if (lastTotal == null || lastTotal != total) {
                cachedTotals.put(p.getUniqueId(), total);
                dbManager.setVithiums(p.getUniqueId(), total);
                VithiumsPermHud.updateHud(p, total);

                if (topEnabled) {
                    forceUpdateTop();
                }
            }
        });
    }

    public void forceUpdateTop() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            updateTopCache();
            for (Player p : Bukkit.getOnlinePlayers()) {
                VithiumsTopHud.updateHud(plugin, p, topCache);
            }
        });
    }

    private void startTasks() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                updatePlayerTotalAsync(p);
            }
            if (topEnabled) {
                forceUpdateTop();
            }
        }, 200L, 200L);
    }

    private void updateTopCache() {
        topCache = dbManager.getTopVithiumsExcluding(excludedPlayers, 15);
    }

    // SOLUCIÓN 2: Búsqueda Universal de Monederos
    public int calculatePhysicalVithiums(Player player) {
        int total = 0;
        Set<String> processedUuids = new HashSet<>();

        // 1. Buscar en la Base de Datos TODOS los monederos que le pertenezcan (estén donde estén)
        List<DatabaseManager.BackpackInfo> backpacks = dbManager.getPlayerBackpacks(player.getUniqueId());
        for (DatabaseManager.BackpackInfo info : backpacks) {
            if (info.itemName != null && info.itemName.contains("Monedero")) {
                processedUuids.add(info.uuid);
                total += countVithiumsInBackpack(info.uuid);
            }
        }

        // 2. Revisar su inventario por si tiene un Monedero recién crafteado/conseguido que no ha guardado en DB
        List<ItemStack> itemsToCheck = new ArrayList<>();
        itemsToCheck.addAll(Arrays.asList(player.getInventory().getContents()));
        itemsToCheck.addAll(Arrays.asList(player.getEnderChest().getContents()));

        for (ItemStack item : itemsToCheck) {
            if (item == null || item.getType() == Material.AIR) continue;
            if (isMonedero(item)) {
                String uuid = item.getItemMeta().getPersistentDataContainer().get(backpackKey, PersistentDataType.STRING);
                if (uuid != null && !processedUuids.contains(uuid)) {
                    processedUuids.add(uuid);
                    total += countVithiumsInBackpack(uuid);
                }
            }
        }

        return total;
    }

    // Lógica para extraer los Vithiums de forma segura y en tiempo real
    private int countVithiumsInBackpack(String uuid) {
        int subTotal = 0;
        ItemStack[] contents = null;
        boolean isOpen = false;

        // ¿Alguien lo tiene abierto ahora mismo?
        for (Map.Entry<UUID, String> entry : EconomyItemsFunctions.mochilasAbiertas.entrySet()) {
            if (entry.getValue().equals(uuid)) {
                Player pViewer = Bukkit.getPlayer(entry.getKey());
                if (pViewer != null && pViewer.getOpenInventory() != null) {
                    contents = pViewer.getOpenInventory().getTopInventory().getContents();
                    isOpen = true;
                    break;
                }
            }
        }

        // Si no está abierto, buscar en caché o BD
        if (!isOpen) {
            contents = EconomyItemsFunctions.mochilasCache.get(uuid);
            if (contents == null) {
                contents = dbManager.loadBackpackContents(uuid);
            }
        }

        if (contents != null) {
            for (ItemStack c : contents) {
                if (isVithiumCoin(c)) subTotal += c.getAmount();
            }
        }
        return subTotal;
    }

    public boolean addPhysicalVithiums(Player player, int amount) {
        int remaining = amount;
        Set<String> targetUuids = new LinkedHashSet<>();

        // Prioridad 1: Monederos en mano/inventario
        for (ItemStack item : player.getInventory().getContents()) {
            if (isMonedero(item)) {
                String uuid = item.getItemMeta().getPersistentDataContainer().get(backpackKey, PersistentDataType.STRING);
                if (uuid != null) targetUuids.add(uuid);
            }
        }
        // Prioridad 2: Monederos guardados en DB (en cofres)
        for (DatabaseManager.BackpackInfo info : dbManager.getPlayerBackpacks(player.getUniqueId())) {
            if (info.itemName != null && info.itemName.contains("Monedero")) {
                targetUuids.add(info.uuid);
            }
        }

        for (String uuid : targetUuids) {
            if (remaining <= 0) break;

            ItemStack[] contents = EconomyItemsFunctions.mochilasCache.get(uuid);
            if (contents == null) contents = dbManager.loadBackpackContents(uuid);
            if (contents == null) contents = new ItemStack[36];

            boolean changed = false;
            for (int i = 0; i < contents.length; i++) {
                if (remaining <= 0) break;
                ItemStack c = contents[i];
                if (c == null || c.getType() == Material.AIR) {
                    int toAdd = Math.min(remaining, 64);
                    ItemStack vithium = EconomyItems.createVithiumCoin();
                    vithium.setAmount(toAdd);
                    contents[i] = vithium;
                    remaining -= toAdd;
                    changed = true;
                } else if (isVithiumCoin(c) && c.getAmount() < 64) {
                    int space = 64 - c.getAmount();
                    int toAdd = Math.min(remaining, space);
                    c.setAmount(c.getAmount() + toAdd);
                    remaining -= toAdd;
                    changed = true;
                }
            }
            if (changed) {
                EconomyItemsFunctions.mochilasCache.put(uuid, contents);
                try { dbManager.saveBackpack(uuid, player.getUniqueId(), player.getName(), "Monedero", 3, contents); }
                catch (Exception ignored) {}
            }
        }
        updatePlayerTotalAsync(player);
        return remaining == 0;
    }

    public boolean removePhysicalVithiums(Player player, int amount) {
        int remaining = amount;
        Set<String> targetUuids = new LinkedHashSet<>();

        for (ItemStack item : player.getInventory().getContents()) {
            if (isMonedero(item)) {
                String uuid = item.getItemMeta().getPersistentDataContainer().get(backpackKey, PersistentDataType.STRING);
                if (uuid != null) targetUuids.add(uuid);
            }
        }
        for (DatabaseManager.BackpackInfo info : dbManager.getPlayerBackpacks(player.getUniqueId())) {
            if (info.itemName != null && info.itemName.contains("Monedero")) {
                targetUuids.add(info.uuid);
            }
        }

        for (String uuid : targetUuids) {
            if (remaining <= 0) break;

            ItemStack[] contents = EconomyItemsFunctions.mochilasCache.get(uuid);
            if (contents == null) contents = dbManager.loadBackpackContents(uuid);
            if (contents == null) continue;

            boolean changed = false;
            for (int i = 0; i < contents.length; i++) {
                if (remaining <= 0) break;
                ItemStack c = contents[i];
                if (isVithiumCoin(c)) {
                    if (c.getAmount() <= remaining) {
                        remaining -= c.getAmount();
                        contents[i] = new ItemStack(Material.AIR);
                        changed = true;
                    } else {
                        c.setAmount(c.getAmount() - remaining);
                        remaining = 0;
                        changed = true;
                    }
                }
            }
            if (changed) {
                EconomyItemsFunctions.mochilasCache.put(uuid, contents);
                try { dbManager.saveBackpack(uuid, player.getUniqueId(), player.getName(), "Monedero", 3, contents); }
                catch (Exception ignored) {}
            }
        }
        updatePlayerTotalAsync(player);
        return remaining == 0;
    }

    private boolean isMonedero(ItemStack item) {
        return item != null && item.hasItemMeta() && item.getItemMeta().hasCustomModelData() && item.getItemMeta().getCustomModelData() == 2025;
    }

    private boolean isVithiumCoin(ItemStack item) {
        return item != null && item.hasItemMeta() && item.getItemMeta().hasCustomModelData() && item.getItemMeta().getCustomModelData() == 2000;
    }
}