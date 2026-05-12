package ShopSystem;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShopListeners implements Listener {

    private final ShopManager shopManager;
    private final ShopGUI shopGUI;

    public ShopListeners(ShopManager shopManager, ShopGUI shopGUI) {
        this.shopManager = shopManager;
        this.shopGUI = shopGUI;
    }

    @EventHandler
    public void onInteract(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Villager)) return;
        Villager villager = (Villager) event.getRightClicked();
        if (!villager.getPersistentDataContainer().has(shopManager.shopKey, PersistentDataType.STRING)) return;

        event.setCancelled(true);
        Player player = event.getPlayer();
        String clickedId = villager.getPersistentDataContainer().get(shopManager.shopIdKey, PersistentDataType.STRING);

        boolean hasPendingEdit = shopManager.editingTradeIndex.containsKey(player.getUniqueId());
        String currentShopId = shopManager.activeShops.get(player.getUniqueId());
        boolean isSameShop = clickedId != null && clickedId.equals(currentShopId);

        if (player.isOp() && player.isSneaking()) {
            if (hasPendingEdit && isSameShop) {
                ItemStack handItem = player.getInventory().getItemInMainHand();

                if (!shopManager.isEmpty(handItem)) {
                    int tradeIdx = shopManager.editingTradeIndex.get(player.getUniqueId());
                    String slotType = shopManager.editingSlotType.get(player.getUniqueId());

                    shopManager.updateVillagerTrade(villager, tradeIdx, slotType, handItem.clone());
                    shopManager.saveShopTrades(currentShopId, villager.getRecipes());

                    player.sendMessage(ChatColor.GREEN + "Tradeo actualizado: " + slotType + " -> " + handItem.getType());
                    player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
                } else {
                    player.sendMessage(ChatColor.GRAY + "Cancelado. Abriendo menú...");
                }

                shopManager.editingTradeIndex.remove(player.getUniqueId());
                shopManager.editingSlotType.remove(player.getUniqueId());
            }
            shopGUI.openConfigGUI(player, villager);
        } else {
            shopGUI.openShopGUI(player, villager);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        String title = event.getView().getTitle();
        if (title != null && (title.startsWith(ChatColor.GOLD + "" + ChatColor.BOLD + "Tienda:") ||
                title.startsWith(ChatColor.RED + "" + ChatColor.BOLD + "Configurar:"))) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();

        if (title == null) return;
        boolean isShop = title.startsWith(ChatColor.GOLD + "" + ChatColor.BOLD + "Tienda:");
        boolean isConfig = title.startsWith(ChatColor.RED + "" + ChatColor.BOLD + "Configurar:");

        if (!isShop && !isConfig) return;
        event.setCancelled(true);

        if (event.getClickedInventory() != event.getView().getTopInventory()) return;

        int slot = event.getSlot();
        int[] info = getTradeSlotInfo(slot);

        if (info == null || info[1] == 2) return;

        int tradeIndex = info[0];
        String typeStr = (info[1] == 0) ? "Ingrediente 1" : (info[1] == 1) ? "Ingrediente 2" : "Producto";

        if (isConfig) {
            handleConfigClick(player, event, tradeIndex, typeStr);
        } else if (isShop) {
            if (info[1] == 3) {
                handleShopClick(player, tradeIndex);
            }
        }
    }

    private void handleShopClick(Player player, int tradeIndex) {
        if (player.hasCooldown(Material.STRUCTURE_VOID)) return;

        String shopId = shopManager.activeShops.get(player.getUniqueId());
        if (shopId == null) return;
        Villager villager = shopManager.getVillagerById(shopId);
        if (villager == null) return;

        List<MerchantRecipe> recipes = villager.getRecipes();
        if (tradeIndex >= recipes.size()) return;

        MerchantRecipe recipe = recipes.get(tradeIndex);
        ItemStack result = recipe.getResult();
        if (shopManager.isEmpty(result)) return;

        ItemStack ing1 = recipe.getIngredients().size() > 0 ? recipe.getIngredients().get(0) : null;
        ItemStack ing2 = recipe.getIngredients().size() > 1 ? recipe.getIngredients().get(1) : null;

        if (hasRequiredItems(player, ing1, ing2)) {
            executeTransaction(player, ing1, ing2);

            player.getInventory().addItem(result.clone()).values().forEach(item ->
                    player.getWorld().dropItem(player.getLocation(), item));

            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
            player.setCooldown(Material.STRUCTURE_VOID, 6);
        } else {
            player.sendMessage(ChatColor.RED + "No tienes los materiales suficientes.");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
        }
    }

    private void handleConfigClick(Player player, InventoryClickEvent event, int tradeIndex, String typeStr) {
        String shopId = shopManager.activeShops.get(player.getUniqueId());
        if (shopId == null) {
            player.sendMessage(ChatColor.RED + "Sesión de tienda perdida. Por favor, cierra el inventario y vuelve a abrirlo.");
            return;
        }

        Villager villager = shopManager.getVillagerById(shopId);
        if (villager == null) return;

        if (event.isRightClick()) {
            shopManager.updateVillagerTrade(villager, tradeIndex, typeStr, shopManager.createEmptyTradeItem());
            shopManager.saveShopTrades(shopId, villager.getRecipes());
            player.sendMessage(ChatColor.RED + "Slot limpiado.");
            shopGUI.openConfigGUI(player, villager);
        } else if (event.isLeftClick()) {
            shopManager.editingTradeIndex.put(player.getUniqueId(), tradeIndex);
            shopManager.editingSlotType.put(player.getUniqueId(), typeStr);
            player.closeInventory();
            player.sendMessage(ChatColor.GREEN + "Editando Tradeo #" + (tradeIndex + 1) + " - " + typeStr);
            player.sendMessage(ChatColor.YELLOW + "Opciones:");
            player.sendMessage(ChatColor.GRAY + "1. Usa " + ChatColor.AQUA + "/trade <item> <cantidad>");
            player.sendMessage(ChatColor.GRAY + "2. Agáchate y click derecho al aldeano con el item en la mano.");
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
        }
    }

    private boolean hasRequiredItems(Player player, ItemStack ing1, ItemStack ing2) {
        Map<ItemStack, Integer> requirements = new HashMap<>();
        addRequirement(requirements, ing1);
        addRequirement(requirements, ing2);

        for (Map.Entry<ItemStack, Integer> entry : requirements.entrySet()) {
            if (!hasSpecificAmount(player, entry.getKey(), entry.getValue())) {
                return false;
            }
        }
        return true;
    }

    private void executeTransaction(Player player, ItemStack ing1, ItemStack ing2) {
        Map<ItemStack, Integer> requirements = new HashMap<>();
        addRequirement(requirements, ing1);
        addRequirement(requirements, ing2);

        for (Map.Entry<ItemStack, Integer> entry : requirements.entrySet()) {
            takeSpecificAmount(player, entry.getKey(), entry.getValue());
        }
    }

    private void addRequirement(Map<ItemStack, Integer> map, ItemStack req) {
        if (shopManager.isEmpty(req)) return;
        for (ItemStack key : map.keySet()) {
            if (shopManager.isMatch(key, req)) {
                map.put(key, map.get(key) + req.getAmount());
                return;
            }
        }
        map.put(req, req.getAmount());
    }

    private boolean hasSpecificAmount(Player player, ItemStack req, int requiredAmount) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (shopManager.isMatch(item, req)) count += item.getAmount();
        }
        return count >= requiredAmount;
    }

    private void takeSpecificAmount(Player player, ItemStack req, int amountToTake) {
        int needed = amountToTake;
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (shopManager.isMatch(item, req)) {
                if (item.getAmount() <= needed) {
                    needed -= item.getAmount();
                    player.getInventory().setItem(i, null);
                } else {
                    item.setAmount(item.getAmount() - needed);
                    needed = 0;
                    break;
                }
            }
        }
    }

    private int[] getTradeSlotInfo(int slot) {
        if (slot < 9) return null;
        int col = slot % 9;
        if (col == 4) return null;

        int row = slot / 9;
        int index = (row - 1) + (col >= 5 ? 5 : 0);
        int type = col >= 5 ? col - 5 : col;

        return new int[]{index, type};
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Player player = (Player) event.getPlayer();
        String title = event.getView().getTitle();

        if (title != null && (title.startsWith(ChatColor.GOLD + "" + ChatColor.BOLD + "Tienda:") ||
                title.startsWith(ChatColor.RED + "" + ChatColor.BOLD + "Configurar:"))) {

            Inventory top = event.getView().getTopInventory();
            int[] emptySlots = {4, 13, 22, 31, 40, 49};
            for (int slot : emptySlots) {
                ItemStack leftover = top.getItem(slot);
                if (leftover != null && leftover.getType() != Material.AIR) {
                    player.getInventory().addItem(leftover).values().forEach(item ->
                            player.getWorld().dropItem(player.getLocation(), item));
                    top.setItem(slot, null);
                }
            }

            Bukkit.getScheduler().runTaskLater(shopManager.getPlugin(), () -> {
                String currentTitle = player.getOpenInventory().getTitle();
                boolean stillInShop = currentTitle.startsWith(ChatColor.GOLD + "" + ChatColor.BOLD + "Tienda:") ||
                        currentTitle.startsWith(ChatColor.RED + "" + ChatColor.BOLD + "Configurar:");

                if (!stillInShop && !shopManager.editingTradeIndex.containsKey(player.getUniqueId())) {
                    shopManager.activeShops.remove(player.getUniqueId());
                }
            }, 1L);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        shopManager.activeShops.remove(event.getPlayer().getUniqueId());
        shopManager.editingTradeIndex.remove(event.getPlayer().getUniqueId());
        shopManager.editingSlotType.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Villager) {
            Villager v = (Villager) event.getEntity();
            if (v.getPersistentDataContainer().has(shopManager.shopKey, PersistentDataType.STRING)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof Villager) {
            Villager v = (Villager) event.getEntity();
            if (v.getPersistentDataContainer().has(shopManager.shopIdKey, PersistentDataType.STRING)) {
                String id = v.getPersistentDataContainer().get(shopManager.shopIdKey, PersistentDataType.STRING);
                shopManager.removeShopFromFile(id);
            }
        }
    }
}