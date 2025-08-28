package Commands;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ShopHandler implements Listener {
    private final JavaPlugin plugin;
    private final NamespacedKey shopKey;
    private final NamespacedKey shopIdKey;
    private final Map<UUID, String> editingShops = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> editingSlots = new ConcurrentHashMap<>();
    private final Map<UUID, String> playersWaitingForInput = new ConcurrentHashMap<>();
    private final Map<UUID, String> editingType = new ConcurrentHashMap<>();
    private final File tradesFile;

    public ShopHandler(JavaPlugin plugin) {
        this.plugin = plugin;
        this.shopKey = new NamespacedKey(plugin, "shop_type");
        this.shopIdKey = new NamespacedKey(plugin, "shop_id");
        this.tradesFile = new File(plugin.getDataFolder(), "tradeos.yml");

        if (!tradesFile.exists()) {
            try {
                tradesFile.getParentFile().mkdirs();
                tradesFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Error creando archivo tradeos.yml: " + e.getMessage());
            }
        } else {
            cleanupDeadShops();
        }

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void spawnShop(String shopType, Location location) {
        Villager villager = (Villager) location.getWorld().spawnEntity(location, EntityType.VILLAGER);

        String shopId = UUID.randomUUID().toString();

        villager.setCustomName(ChatColor.GOLD + "" + ChatColor.BOLD + shopType);
        villager.setCustomNameVisible(true);
        villager.setAI(false);
        villager.setSilent(true);
        villager.setInvulnerable(true);
        villager.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(0.0);

        villager.getPersistentDataContainer().set(shopKey, PersistentDataType.STRING, shopType);
        villager.getPersistentDataContainer().set(shopIdKey, PersistentDataType.STRING, shopId);

        initializeEmptyTrades(villager);
        saveShopTrades(shopId, villager.getRecipes());
    }

    private void initializeEmptyTrades(Villager villager) {
        List<MerchantRecipe> recipes = new ArrayList<>();

        for (int i = 0; i < 12; i++) {
            ItemStack emptyPaper = createEmptyTradeItem();
            MerchantRecipe recipe = new MerchantRecipe(emptyPaper, 999);
            recipe.addIngredient(emptyPaper);
            recipes.add(recipe);
        }

        villager.setRecipes(recipes);
    }

    private ItemStack createEmptyTradeItem() {
        ItemStack paper = new ItemStack(Material.PAPER);
        ItemMeta meta = paper.getItemMeta();
        meta.setDisplayName(ChatColor.GRAY + "Vacío");
        meta.setCustomModelData(100);
        paper.setItemMeta(meta);
        return paper;
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Villager)) return;

        Villager villager = (Villager) event.getRightClicked();
        Player player = event.getPlayer();

        if (!villager.getPersistentDataContainer().has(shopKey, PersistentDataType.STRING)) return;

        if (player.isSneaking() && player.isOp()) {
            event.setCancelled(true);
            openShopConfigGUI(player, villager);
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Villager)) return;

        Villager villager = (Villager) event.getEntity();
        if (villager.getPersistentDataContainer().has(shopKey, PersistentDataType.STRING)) {
            if (event.getCause() != EntityDamageEvent.DamageCause.KILL) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onEntityDeath(org.bukkit.event.entity.EntityDeathEvent event) {
        if (event.getEntity() instanceof Villager) {
            Villager villager = (Villager) event.getEntity();
            if (villager.getPersistentDataContainer().has(shopIdKey, PersistentDataType.STRING)) {
                String shopId = villager.getPersistentDataContainer().get(shopIdKey, PersistentDataType.STRING);
                removeShopFromFile(shopId);
            }
        }
    }

    private void removeShopFromFile(String shopId) {
        FileConfiguration config = YamlConfiguration.loadConfiguration(tradesFile);
        if (config.contains("shops." + shopId)) {
            config.set("shops." + shopId, null);
            try {
                config.save(tradesFile);
                plugin.getLogger().info("Tienda eliminada: " + shopId);
            } catch (IOException e) {
                plugin.getLogger().severe("Error eliminando tienda: " + e.getMessage());
            }
        }
    }

    private void openShopConfigGUI(Player player, Villager villager) {
        String shopType = villager.getPersistentDataContainer().get(shopKey, PersistentDataType.STRING);
        String shopId = villager.getPersistentDataContainer().get(shopIdKey, PersistentDataType.STRING);

        Inventory gui = Bukkit.createInventory(null, 54, ChatColor.GOLD + "Configurar Tienda: " + shopType);

        editingShops.put(player.getUniqueId(), shopId);
        loadShopTrades(shopId, villager);

        // Configurar los paneles
        ItemStack greenGlass = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        ItemStack editItem = new ItemStack(Material.INK_SAC);
        ItemMeta editMeta = editItem.getItemMeta();
        editMeta.setDisplayName(ChatColor.DARK_PURPLE + "Editar Tradeo");
        editItem.setItemMeta(editMeta);

        // Rellenar el inventario
        for (int i = 0; i < 54; i++) {
            int row = i / 9;
            int col = i % 9;

            if (col == 4) {
                gui.setItem(i, greenGlass);
            } else if (col < 4 || col > 4) {
                gui.setItem(i, editItem);
            }
        }

        // Configurar los tradeos
        for (int trade = 0; trade < 12; trade++) {
            int row, baseSlot;

            if (trade < 6) {
                row = trade;
                baseSlot = row * 9;
            } else {
                row = trade - 6;
                baseSlot = row * 9 + 5;
            }

            int ingredient1Slot = baseSlot;
            int ingredient2Slot = baseSlot + 1;
            int resultSlot = baseSlot + 2;

            MerchantRecipe recipe = villager.getRecipes().size() > trade ? villager.getRecipes().get(trade) : null;

            if (recipe != null && !recipe.getResult().getType().equals(Material.PAPER)) {
                // Tradeo configurado
                ItemStack ing1 = recipe.getIngredients().size() > 0 ? recipe.getIngredients().get(0) : new ItemStack(Material.AIR);
                ItemStack ing2 = recipe.getIngredients().size() > 1 ? recipe.getIngredients().get(1) : new ItemStack(Material.AIR);

                gui.setItem(ingredient1Slot, createEditOption("Ingrediente 1", trade, ing1));
                gui.setItem(ingredient2Slot, createEditOption("Ingrediente 2", trade, ing2));
                gui.setItem(resultSlot, createEditOption("Resultado", trade, recipe.getResult()));
            } else {
                // Tradeo no configurado
                gui.setItem(ingredient1Slot, createConfigSlotItem("Ingrediente 1", trade));
                gui.setItem(ingredient2Slot, createConfigSlotItem("Ingrediente 2", trade));
                gui.setItem(resultSlot, createConfigSlotItem("Resultado", trade));
            }
        }

        player.openInventory(gui);
    }

    private ItemStack createEditOption(String type, int tradeNumber, ItemStack currentItem) {
        if (currentItem == null || currentItem.getType() == Material.AIR) {
            currentItem = new ItemStack(Material.PAPER);
        }

        ItemStack item = currentItem.clone();
        ItemMeta meta = item.getItemMeta();

        if (meta == null) {
            meta = Bukkit.getItemFactory().getItemMeta(item.getType());
            if (meta == null) {
                meta = Bukkit.getItemFactory().getItemMeta(Material.PAPER);
            }
        }

        List<String> lore = new ArrayList<>();
        if (meta.hasLore()) {
            lore.addAll(meta.getLore());
            lore.add("");
        }

        lore.add(ChatColor.YELLOW + "Click para editar " + type);
        lore.add(ChatColor.GRAY + "Tradeo " + (tradeNumber + 1));
        lore.add("");
        lore.add(ChatColor.RED + "Click derecho para borrar este componente");
        lore.add(ChatColor.DARK_RED + "Shift + Click para borrar todo el tradeo");

        meta.setLore(lore);
        meta.setDisplayName(ChatColor.GOLD + type + " del Tradeo " + (tradeNumber + 1));
        item.setItemMeta(meta);

        return item;
    }

    private ItemStack createConfigSlotItem(String type, int tradeNumber) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ChatColor.YELLOW + type + " - Tradeo " + (tradeNumber + 1));
        meta.setLore(Arrays.asList(
                "",
                ChatColor.GRAY + "Click para configurar",
                ChatColor.GRAY + "Escribe en chat: <item> <cantidad>",
                ChatColor.GRAY + "Ejemplo: diamond 5",
                ChatColor.RED + "Escribe 'eliminar' para borrar el tradeo",
                ""
        ));
        meta.setCustomModelData(101 + tradeNumber);

        item.setItemMeta(meta);
        return item;
    }


    @EventHandler
    public void onInventoryClick(org.bukkit.event.inventory.InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();

        if (title.startsWith(ChatColor.GOLD + "Configurar Tienda:")) {
            event.setCancelled(true);

            if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) return;

            // Prevenir shift click
            if (event.getClick().isShiftClick()) {
                event.setCancelled(true);
                player.updateInventory();
            }

            ItemMeta meta = event.getCurrentItem().getItemMeta();

            if (meta != null && meta.hasLore() &&
                    (meta.getLore().contains(ChatColor.YELLOW + "Click para editar Ingrediente 1") ||
                            meta.getLore().contains(ChatColor.YELLOW + "Click para editar Ingrediente 2") ||
                            meta.getLore().contains(ChatColor.YELLOW + "Click para editar Resultado"))) {
                // Es un tradeo configurado - manejar la edición
                handleEditTrade(player, event.getCurrentItem(), event.getClick());
                return;
            }

            if (meta == null || !meta.hasCustomModelData()) return;

            int customModelData = meta.getCustomModelData();
            if (customModelData < 101 || customModelData > 112) return;

            int tradeNumber = customModelData - 101;
            int slot = event.getSlot();

            String slotType = determineSlotType(slot, tradeNumber);
            if (slotType == null) return;

            editingSlots.put(player.getUniqueId(), tradeNumber);
            editingType.put(player.getUniqueId(), slotType);
            playersWaitingForInput.put(player.getUniqueId(), "trade_config");

            player.closeInventory();
            player.sendMessage(ChatColor.GOLD + "Configura " + slotType + " para el Tradeo " + (tradeNumber + 1));
            player.sendMessage(ChatColor.GRAY + "Formato: <item> <cantidad>");
            player.sendMessage(ChatColor.GRAY + "Ejemplo: diamond 5 o nether_emblem 1");
            player.sendMessage(ChatColor.RED + "Escribe 'eliminar' para borrar todo el tradeo");
            player.sendMessage(ChatColor.GRAY + "Escribe 'cancelar' para abortar");
        }
    }

    private void handleEditTrade(Player player, ItemStack clickedItem, ClickType clickType) {
        ItemMeta meta = clickedItem.getItemMeta();
        String displayName = meta.getDisplayName();

        // Extraer información del tradeo
        int tradeNumber = -1;
        String slotType = "";

        if (displayName.contains("Ingrediente 1")) {
            slotType = "Ingrediente 1";
        } else if (displayName.contains("Ingrediente 2")) {
            slotType = "Ingrediente 2";
        } else if (displayName.contains("Resultado")) {
            slotType = "Resultado";
        } else {
            player.sendMessage(ChatColor.RED + "Error: No se pudo determinar el tipo de slot");
            return;
        }

        try {
            String numberStr = displayName.split("Tradeo ")[1];
            tradeNumber = Integer.parseInt(numberStr) - 1;
        } catch (Exception e) {
            player.sendMessage(ChatColor.RED + "Error al procesar el tradeo");
            return;
        }

        // Prevenir que el jugador obtenga el item
        player.updateInventory();

        if (clickType.isRightClick()) {
            // Borrar componente específico
            editingSlots.put(player.getUniqueId(), tradeNumber);
            editingType.put(player.getUniqueId(), slotType);
            playersWaitingForInput.put(player.getUniqueId(), "delete_component");

            player.closeInventory();
            player.sendMessage(ChatColor.GOLD + "¿Borrar " + slotType + " del Tradeo " + (tradeNumber + 1) + "?");
            player.sendMessage(ChatColor.GREEN + "Escribe 'confirmar' para borrar o 'cancelar' para abortar");
        } else if (clickType.isShiftClick()) {
            // Borrar todo el tradeo
            editingSlots.put(player.getUniqueId(), tradeNumber);
            playersWaitingForInput.put(player.getUniqueId(), "delete_trade");

            player.closeInventory();
            player.sendMessage(ChatColor.GOLD + "¿Borrar TODO el Tradeo " + (tradeNumber + 1) + "?");
            player.sendMessage(ChatColor.GREEN + "Escribe 'confirmar' para borrar o 'cancelar' para abortar");
        } else {
            // Editar componente
            editingSlots.put(player.getUniqueId(), tradeNumber);
            editingType.put(player.getUniqueId(), slotType);
            playersWaitingForInput.put(player.getUniqueId(), "trade_config");

            player.closeInventory();
            player.sendMessage(ChatColor.GOLD + "Configura " + slotType + " para el Tradeo " + (tradeNumber + 1));
            player.sendMessage(ChatColor.GRAY + "Formato: <item> <cantidad>");
            player.sendMessage(ChatColor.GRAY + "Ejemplo: diamond 5 o nether_emblem 1");
            player.sendMessage(ChatColor.RED + "Escribe 'eliminar' para borrar este componente");
            player.sendMessage(ChatColor.GRAY + "Escribe 'cancelar' para abortar");
        }
    }

    private String determineSlotType(int slot, int tradeNumber) {
        int row, baseSlot;

        if (tradeNumber < 6) {
            // Primera columna (tradeos 1-6)
            row = tradeNumber;
            baseSlot = row * 9;
        } else {
            // Segunda columna (tradeos 7-12)
            row = tradeNumber - 6;
            baseSlot = row * 9 + 5;
        }

        if (slot == baseSlot) return "Ingrediente 1";
        if (slot == baseSlot + 1) return "Ingrediente 2";
        if (slot == baseSlot + 2) return "Resultado";

        return null;
    }

    @EventHandler
    public void onPlayerChat(PlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (!playersWaitingForInput.containsKey(uuid)) return;

        event.setCancelled(true);
        String input = event.getMessage().trim();

        if (input.equalsIgnoreCase("cancelar")) {
            handleCancel(player, uuid);
            return;
        }

        String action = playersWaitingForInput.get(uuid);

        if (action.equals("delete_component") || action.equals("delete_trade")) {
            if (input.equals("confirmar")) {
                int tradeNumber = editingSlots.get(uuid);
                String shopId = editingShops.get(uuid);

                if (shopId != null) {
                    Villager villager = findVillagerByShopId(shopId);
                    if (villager != null) {
                        if (action.equals("delete_component")) {
                            String slotType = editingType.get(uuid);
                            ItemStack emptyItem = new ItemStack(Material.AIR);
                            updateVillagerTrade(villager, tradeNumber, slotType, emptyItem);
                            player.sendMessage(ChatColor.GREEN + slotType + " del Tradeo " + (tradeNumber + 1) + " borrado");
                        } else {
                            resetTrade(villager, tradeNumber);
                            player.sendMessage(ChatColor.GREEN + "Tradeo " + (tradeNumber + 1) + " borrado completamente");
                        }
                        saveShopTrades(shopId, villager.getRecipes());
                    }
                }

                cleanupPlayerData(uuid);
                reopenGUI(player, uuid);
            } else {
                player.sendMessage(ChatColor.RED + "Acción cancelada");
                cleanupPlayerData(uuid);
                reopenGUI(player, uuid);
            }
            return;
        }

        if (input.equalsIgnoreCase("eliminar")) {
            handleDelete(player, uuid);
            return;
        }

        String[] parts = input.split(" ");
        if (parts.length != 2) {
            player.sendMessage(ChatColor.RED + "Formato inválido. Usa: <item> <cantidad>");
            return;
        }

        String itemName = parts[0];
        int amount;

        try {
            amount = Integer.parseInt(parts[1]);
            if (amount <= 0 || amount > 64) {
                player.sendMessage(ChatColor.RED + "La cantidad debe estar entre 1 y 64");
                return;
            }
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "La cantidad debe ser un número válido");
            return;
        }

        handleItemInput(player, uuid, itemName, amount);
    }

    private void handleCancel(Player player, UUID uuid) {
        player.sendMessage(ChatColor.RED + "Configuración cancelada");
        cleanupPlayerData(uuid);

        new BukkitRunnable() {
            @Override
            public void run() {
                String shopId = editingShops.get(uuid);
                if (shopId != null) {
                    Villager villager = findVillagerByShopId(shopId);
                    if (villager != null) {
                        openShopConfigGUI(player, villager);
                    }
                }
            }
        }.runTask(plugin);
    }

    private void handleDelete(Player player, UUID uuid) {
        new BukkitRunnable() {
            @Override
            public void run() {
                int tradeNumber = editingSlots.get(uuid);
                String shopId = editingShops.get(uuid);

                if (shopId != null) {
                    Villager villager = findVillagerByShopId(shopId);
                    if (villager != null) {
                        resetTrade(villager, tradeNumber);
                        saveShopTrades(shopId, villager.getRecipes());
                        player.sendMessage(ChatColor.GREEN + "Tradeo " + (tradeNumber + 1) + " eliminado completamente");
                    }
                }

                cleanupPlayerData(uuid);
                reopenGUI(player, uuid);
            }
        }.runTask(plugin);
    }

    private void handleItemInput(Player player, UUID uuid, String itemName, int amount) {
        new BukkitRunnable() {
            @Override
            public void run() {
                ItemStack item = createItemFromName(itemName, amount);
                if (item == null) {
                    player.sendMessage(ChatColor.RED + "Item no reconocido: " + itemName);
                    return;
                }

                int tradeNumber = editingSlots.get(uuid);
                String slotType = editingType.get(uuid);
                String shopId = editingShops.get(uuid);

                if (shopId != null) {
                    Villager villager = findVillagerByShopId(shopId);
                    if (villager != null) {
                        updateVillagerTrade(villager, tradeNumber, slotType, item);
                        saveShopTrades(shopId, villager.getRecipes());
                        player.sendMessage(ChatColor.GREEN + slotType + " configurado para Tradeo " + (tradeNumber + 1));

                        // Verificar que el jugador todavía tenga un inventario abierto
                        if (player.getOpenInventory() != null &&
                                player.getOpenInventory().getTitle().startsWith(ChatColor.GOLD + "Configurar Tienda:")) {
                            updateGUISlot(player, tradeNumber, slotType, item);
                        } else {
                            // Si el inventario está cerrado, abrirlo de nuevo
                            openShopConfigGUI(player, villager);
                            updateGUISlot(player, tradeNumber, slotType, item);
                        }
                    }
                }

                cleanupPlayerData(uuid);
                reopenGUI(player, uuid);
            }
        }.runTask(plugin);
    }

    private void reopenGUI(Player player, UUID uuid) {
        String shopId = editingShops.get(uuid);
        if (shopId != null) {
            Villager villager = findVillagerByShopId(shopId);
            if (villager != null) {
                openShopConfigGUI(player, villager);
            }
        }
    }

    private void resetTrade(Villager villager, int tradeNumber) {
        List<MerchantRecipe> recipes = new ArrayList<>(villager.getRecipes());

        // Asegurar que hay suficientes recetas
        while (recipes.size() <= tradeNumber) {
            ItemStack emptyPaper = createEmptyTradeItem();
            MerchantRecipe recipe = new MerchantRecipe(emptyPaper, 999);
            recipe.addIngredient(emptyPaper);
            recipes.add(recipe);
        }

        // Crear receta vacía
        ItemStack emptyPaper = createEmptyTradeItem();
        MerchantRecipe emptyRecipe = new MerchantRecipe(emptyPaper, 999);
        emptyRecipe.addIngredient(emptyPaper);

        recipes.set(tradeNumber, emptyRecipe);
        villager.setRecipes(recipes);
    }

    private void updateGUISlot(Player player, int tradeNumber, String slotType, ItemStack item) {
        if (player == null || !player.isOnline()) return;

        InventoryView openInventory = player.getOpenInventory();
        if (openInventory == null) return;

        Inventory gui = openInventory.getTopInventory();
        if (gui == null || gui.getSize() != 54) return; // Asegurarse que es un inventario de 54 slots

        int row, baseSlot;

        if (tradeNumber < 6) {
            // Primera columna (tradeos 1-6)
            row = tradeNumber;
            baseSlot = row * 9;
        } else {
            // Segunda columna (tradeos 7-12)
            row = tradeNumber - 6;
            baseSlot = row * 9 + 5;
        }

        int targetSlot = -1;
        switch (slotType) {
            case "Ingrediente 1":
                targetSlot = baseSlot;
                break;
            case "Ingrediente 2":
                targetSlot = baseSlot + 1;
                break;
            case "Resultado":
                targetSlot = baseSlot + 2;
                break;
        }

        // Verificar que el slot esté dentro de los límites
        if (targetSlot >= 0 && targetSlot < 54) {
            ItemStack displayItem = item.clone();
            ItemMeta meta = displayItem.getItemMeta();
            if (meta != null) {
                List<String> lore = new ArrayList<>();
                lore.add("");
                lore.add(ChatColor.GREEN + "Configurado: " + item.getType().name());
                lore.add(ChatColor.GREEN + "Cantidad: " + item.getAmount());
                lore.add("");
                meta.setLore(lore);
                displayItem.setItemMeta(meta);
            }
            gui.setItem(targetSlot, displayItem);
        } else {
            plugin.getLogger().warning("Intento de acceder a slot inválido: " + targetSlot + " para tradeNumber: " + tradeNumber);
        }
    }

    private ItemStack createItemFromName(String itemName, int amount) {
        try {
            Material material = Material.valueOf(itemName.toUpperCase());
            return new ItemStack(material, amount);
        } catch (IllegalArgumentException e) {
            return createCustomItem(itemName, amount);
        }
    }

    private ItemStack createCustomItem(String itemName, int amount) {
        // Usar la misma lógica que ItemsCommands para items custom
        ItemStack item = null;

        switch (itemName.toLowerCase()) {
            case "nether_emblem":
                item = items.EmblemItems.createNetherEmblem();
                break;
            case "overworld_emblem":
                item = items.EmblemItems.createOverworldEmblem();
                break;
            case "end_relic":
                item = items.EmblemItems.createEndEmblem();
                break;
            case "vithiums":
                item = items.EconomyItems.createVithiumCoin();
                break;
            case "vithiums_fichas":
                item = items.EconomyItems.createVithiumToken();
                break;
            case "mochila":
                item = items.EconomyItems.createNormalMochila();
                break;
            case "mochila_verde":
                item = items.EconomyItems.createGreenMochila();
                break;
            case "mochila_roja":
                item = items.EconomyItems.createRedMochila();
                break;
            case "mochila_azul":
                item = items.EconomyItems.createBlueMochila();
                break;
            case "mochila_morada":
                item = items.EconomyItems.createPurpleMochila();
                break;
            case "mochila_negra":
                item = items.EconomyItems.createBlackMochila();
                break;
            case "mochila_blanca":
                item = items.EconomyItems.createWhiteMochila();
                break;
            case "mochila_amarilla":
                item = items.EconomyItems.createYellowMochila();
                break;
            case "enderbag":
                item = items.EconomyItems.createEnderBag();
                break;
            case "gancho":
                item = items.EconomyItems.createGancho();
                break;
            case "panic_apple":
                item = items.EconomyItems.createManzanaPanico();
                break;
            case "yunque_nivel_1":
                item = items.EconomyItems.createYunqueReparadorNivel1();
                break;
            case "yunque_nivel_2":
                item = items.EconomyItems.createYunqueReparadorNivel2();
                break;
            case "corrupted_golden_apple":
                item = items.CorruptedGoldenApple.createCorruptedGoldenApple();
                break;
            case "apilate_gold_block":
                item = items.CorruptedGoldenApple.createApilateGoldBlock();
                break;
            case "orbe_de_vida":
                item = items.ReviveItems.createResurrectOrb();
                break;
            case "fragmento_infernal":
                item = items.EmblemItems.createFragmentoInfernal();
                break;
            case "pepita_infernal":
                item = items.EmblemItems.createPepitaInfernal();
                break;
            case "corrupted_nether_star":
                item = items.EmblemItems.createcorruptedNetherStar();
                break;
            case "corrupted_steak":
                item = Dificultades.DayOneChanges.corruptedSteak();
                break;
            case "placa_diamante":
                item = Enchants.EnhancedEnchantmentTable.createDiamondPlate();
                break;
            case "mesa_encantamientos_mejorada":
                item = Enchants.EnhancedEnchantmentTable.createEnhancedEnchantmentTable();
                break;
            case "enderite_sword":
                item = items.EnderiteTools.createEnderiteSword();
                break;
            case "enderite_axe":
                item = items.EnderiteTools.createEnderiteAxe();
                break;
            case "enderite_pickaxe":
                item = items.EnderiteTools.createEnderitePickaxe();
                break;
            case "enderite_shovel":
                item = items.EnderiteTools.createEnderiteShovel();
                break;
            case "enderite_hoe":
                item = items.EnderiteTools.createEnderiteHoe();
                break;
            case "corrupted_netherite_scrap":
                item = items.CorruptedNetheriteItems.createCorruptedScrapNetherite();
                break;
            case "corrupted_netherite_ingot":
                item = items.CorruptedNetheriteItems.createCorruptedNetheriteIngot();
                break;
            case "corrupted_powder":
                item = items.CorruptedMobItems.createCorruptedPowder();
                break;
            case "corrupted_rotten":
                item = items.CorruptedMobItems.createCorruptedMeet();
                break;
            case "corrupted_spidereyes":
                item = items.CorruptedMobItems.createCorruptedSpiderEye();
                break;
            case "corrupted_ancient_debris":
                item = new Blocks.CorruptedAncientDebris(plugin).createcorruptedancientdebris();
                break;
            case "guardian_shulker_heart":
                item = new Blocks.GuardianShulkerHeart(plugin).createGuardianShulkerHeart();
                break;
            case "endstalactitas":
                item = Blocks.Endstalactitas.createEndstalactita();
                break;
            case "toxicspidereye":
                item = items.ItemsTotems.createToxicSpiderEye();
                break;
            case "infernalcreeperpowder":
                item = items.ItemsTotems.createInfernalCreeperPowder();
                break;
            case "whiteenderpearl":
                item = items.ItemsTotems.createWhiteEnderPearl();
                break;
            case "specialtotem":
                item = items.ItemsTotems.createSpecialTotem();
                break;
            case "varita_guardian_blaze":
                item = items.BlazeItems.createBlazeRod();
                break;
            case "polvo_guardian_blaze":
                item = items.BlazeItems.createGuardianBlazePowder();
                break;
            case "ultra_pocion_resistencia_fuego":
                item = items.BlazeItems.createPotionOfFireResistance();
                break;
            case "guardian_shulker_shell":
                item = items.EndItems.createGuardianShulkerShell();
                break;
            case "enderite_nugget":
                item = items.EndItems.createEnderiteNugget(1);
                break;
            case "enderite_fragment":
                item = items.EndItems.createFragmentoEnderite();
                break;
            case "end_amatist":
                item = items.EndItems.createEndAmatist(1);
                break;
            case "enderite_ingot":
                item = items.EndItems.createIngotEnderite();
                break;
            case "enderite_upgrades":
                item = items.EndItems.createEnderiteUpgrades();
                break;
            case "upgrade_vacio":
                item = items.UpgradeNTItems.createUpgradeVacio();
                break;
            case "fragmento_upgrade":
                item = items.UpgradeNTItems.createFragmentoUpgrade();
                break;
            case "duplicador":
                item = items.UpgradeNTItems.createDuplicador();
                break;
            default:
                return null; // Item no reconocido
        }

        if (item != null) {
            item.setAmount(amount);
        }

        return item;
    }

    private void updateVillagerTrade(Villager villager, int tradeNumber, String slotType, ItemStack item) {
        List<MerchantRecipe> recipes = new ArrayList<>(villager.getRecipes());

        // Asegurar que hay suficientes recetas
        while (recipes.size() <= tradeNumber) {
            ItemStack emptyPaper = createEmptyTradeItem();
            MerchantRecipe recipe = new MerchantRecipe(emptyPaper, 999);
            recipe.addIngredient(emptyPaper);
            recipes.add(recipe);
        }

        MerchantRecipe currentRecipe = recipes.get(tradeNumber);
        List<ItemStack> ingredients = new ArrayList<>(currentRecipe.getIngredients());
        ItemStack result = currentRecipe.getResult();

        switch (slotType) {
            case "Ingrediente 1":
                if (item.getType() == Material.AIR) {
                    if (!ingredients.isEmpty()) {
                        ingredients.set(0, createEmptyTradeItem());
                    }
                } else {
                    if (ingredients.isEmpty()) {
                        ingredients.add(item);
                    } else {
                        ingredients.set(0, item);
                    }
                }
                break;

            case "Ingrediente 2":
                if (item.getType() == Material.AIR) {
                    if (ingredients.size() > 1) {
                        ingredients.remove(1);
                    }
                } else {
                    if (ingredients.size() < 2) {
                        // Asegurar que hay un primer ingrediente
                        if (ingredients.isEmpty()) {
                            ingredients.add(createEmptyTradeItem());
                        }
                        ingredients.add(item);
                    } else {
                        ingredients.set(1, item);
                    }
                }
                break;

            case "Resultado":
                result = (item.getType() == Material.AIR) ? createEmptyTradeItem() : item;
                break;
        }

        // Crear nueva receta
        MerchantRecipe newRecipe = new MerchantRecipe(
                (result == null || result.getType() == Material.AIR) ? createEmptyTradeItem() : result,
                999
        );

        // Añadir ingredientes válidos
        for (ItemStack ingredient : ingredients) {
            if (ingredient != null && !ingredient.getType().isAir()) {
                newRecipe.addIngredient(ingredient);
            }
        }

        // Asegurar que hay al menos un ingrediente
        if (newRecipe.getIngredients().isEmpty()) {
            newRecipe.addIngredient(createEmptyTradeItem());
        }

        recipes.set(tradeNumber, newRecipe);
        villager.setRecipes(recipes);
    }

    private void saveShopTrades(String shopId, List<MerchantRecipe> recipes) {
        FileConfiguration config = YamlConfiguration.loadConfiguration(tradesFile);

        // Limpiar trades antiguos para esta tienda
        config.set("shops." + shopId + ".trades", null);

        for (int i = 0; i < recipes.size(); i++) {
            MerchantRecipe recipe = recipes.get(i);
            String basePath = "shops." + shopId + ".trades." + i;

            // Guardar ingredientes
            List<ItemStack> ingredients = recipe.getIngredients();
            for (int j = 0; j < ingredients.size(); j++) {
                ItemStack ingredient = ingredients.get(j);
                // Solo guardar ingredientes válidos
                if (ingredient != null && !ingredient.getType().isAir()) {
                    config.set(basePath + ".ingredients." + j, ingredient);
                }
            }

            // Guardar resultado (asegurarse de que no es nulo)
            ItemStack result = recipe.getResult();
            if (result != null && !result.getType().isAir()) {
                config.set(basePath + ".result", result);
            } else {
                config.set(basePath + ".result", createEmptyTradeItem());
            }

            config.set(basePath + ".maxUses", recipe.getMaxUses());
        }

        try {
            config.save(tradesFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Error guardando tradeos: " + e.getMessage());
        }
    }

    private void loadShopTrades(String shopId, Villager villager) {
        FileConfiguration config = YamlConfiguration.loadConfiguration(tradesFile);

        if (!config.contains("shops." + shopId)) return;

        List<MerchantRecipe> recipes = new ArrayList<>();

        for (int i = 0; i < 12; i++) {
            String basePath = "shops." + shopId + ".trades." + i;

            if (config.contains(basePath)) {
                ItemStack result = config.getItemStack(basePath + ".result");
                int maxUses = config.getInt(basePath + ".maxUses", 999);

                if (result != null) {
                    MerchantRecipe recipe = new MerchantRecipe(result, maxUses);

                    for (int j = 0; j < 2; j++) {
                        ItemStack ingredient = config.getItemStack(basePath + ".ingredients." + j);
                        if (ingredient != null && !ingredient.getType().isAir()) {
                            recipe.addIngredient(ingredient);
                        }
                    }

                    recipes.add(recipe);
                } else {
                    ItemStack emptyPaper = createEmptyTradeItem();
                    MerchantRecipe recipe = new MerchantRecipe(emptyPaper, 999);
                    recipe.addIngredient(emptyPaper);
                    recipes.add(recipe);
                }
            } else {
                ItemStack emptyPaper = createEmptyTradeItem();
                MerchantRecipe recipe = new MerchantRecipe(emptyPaper, 999);
                recipe.addIngredient(emptyPaper);
                recipes.add(recipe);
            }
        }

        villager.setRecipes(recipes);
    }

    private Villager findVillagerByShopId(String shopId) {
        for (World world : Bukkit.getWorlds()) {
            for (Villager villager : world.getEntitiesByClass(Villager.class)) {
                if (villager.getPersistentDataContainer().has(shopIdKey, PersistentDataType.STRING)) {
                    String villagerShopId = villager.getPersistentDataContainer().get(shopIdKey, PersistentDataType.STRING);
                    if (shopId.equals(villagerShopId)) {
                        return villager;
                    }
                }
            }
        }
        return null;
    }

    private void cleanupPlayerData(UUID uuid) {
        editingShops.remove(uuid);
        editingSlots.remove(uuid);
        editingType.remove(uuid);
        playersWaitingForInput.remove(uuid);
    }

    private void cleanupDeadShops() {
        FileConfiguration config = YamlConfiguration.loadConfiguration(tradesFile);

        if (!config.contains("shops")) {
            return;
        }

        Set<String> existingShopIds = new HashSet<>();

        // Recoger todos los IDs de aldeanos vivos
        for (World world : Bukkit.getWorlds()) {
            for (Villager villager : world.getEntitiesByClass(Villager.class)) {
                if (villager.getPersistentDataContainer().has(shopIdKey, PersistentDataType.STRING)) {
                    String shopId = villager.getPersistentDataContainer().get(shopIdKey, PersistentDataType.STRING);
                    existingShopIds.add(shopId);
                }
            }
        }

        // Obtener todos los IDs guardados en el YAML
        Set<String> savedShopIds = new HashSet<>(config.getConfigurationSection("shops").getKeys(false));

        // Encontrar IDs que están en el YAML pero no en aldeanos vivos
        savedShopIds.removeAll(existingShopIds);

        // Eliminar esos IDs del YAML
        for (String deadShopId : savedShopIds) {
            config.set("shops." + deadShopId, null);
        }

        // Guardar los cambios si hubo eliminaciones
        if (!savedShopIds.isEmpty()) {
            try {
                config.save(tradesFile);
                plugin.getLogger().info("Se eliminaron " + savedShopIds.size() + " tiendas de aldeanos que ya no existen");
            } catch (IOException e) {
                plugin.getLogger().severe("Error al limpiar tiendas eliminadas: " + e.getMessage());
            }
        }
    }

    @EventHandler
    public void onInventoryClose(org.bukkit.event.inventory.InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;

        Player player = (Player) event.getPlayer();
        String title = event.getView().getTitle();

        if (title.startsWith(ChatColor.GOLD + "Configurar Tienda:")) {
            if (!playersWaitingForInput.containsKey(player.getUniqueId())) {
                cleanupPlayerData(player.getUniqueId());
            }
        }
    }
}