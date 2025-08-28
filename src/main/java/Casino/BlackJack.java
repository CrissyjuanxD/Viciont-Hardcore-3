package Casino;

import items.EconomyItems;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class BlackJack implements Listener {
    private final JavaPlugin plugin;
    private final String title = ChatColor.of("#FFD700") + "" + ChatColor.BOLD + "BlackJack";

    // Slots de la GUI
    private final List<Integer> playerCardSlots = Arrays.asList(19, 20, 21, 22, 23);
    private final List<Integer> dealerCardSlots = Arrays.asList(10, 11, 12, 13, 14);
    private final int tokenSlot = 31; // Slot para fichas
    private final int dealButton = 25; // Botón de repartir
    private final int hitButton = 33; // Botón de pedir carta
    private final int standButton = 34; // Botón de plantarse
    private final int closeButton = 0; // Botón cerrar

    // Maps para gestionar el estado del juego
    private final Map<UUID, Boolean> isPlaying = new ConcurrentHashMap<>();
    private final Map<UUID, List<Card>> playerHands = new ConcurrentHashMap<>();
    private final Map<UUID, List<Card>> dealerHands = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> playerStand = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> playerBets = new ConcurrentHashMap<>(); // NUEVO: Guardar apuestas
    private final Map<Location, UUID> tableUsers = new ConcurrentHashMap<>();
    private final Map<Location, List<ItemDisplay>> activeDisplays = new ConcurrentHashMap<>();
    private final Map<UUID, BukkitRunnable> reconnectTimers = new ConcurrentHashMap<>(); // NUEVO: Timers de reconexión

    // Configuración
    private final File configFile;
    private FileConfiguration config;

    // Cartas y sus CustomModelData
    private final Map<String, Integer> cardModelData = new HashMap<>();

    // Posiciones para displays 3D
    private final double[] dealerDisplayPositions = {-1.0, -0.5, 0.0, 0.5, 1.0}; // 5 posiciones para dealer
    private final double[] playerDisplayPositions = {-1.0, -0.5, 0.0, 0.5, 1.0}; // 5 posiciones para jugador
    private final double dealerDisplayHeight = 2.5; // Altura del dealer
    private final double playerDisplayHeight = 1.8; // Altura del jugador

    public BlackJack(JavaPlugin plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "BlackJack.yml");

        initializeCardModelData();
        loadConfig();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    private void loadConfig() {
        if (!configFile.exists()) {
            createDefaultConfig();
        }
        config = YamlConfiguration.loadConfiguration(configFile);
    }

    private void createDefaultConfig() {
        try {
            configFile.getParentFile().mkdirs();
            configFile.createNewFile();

            FileConfiguration defaultConfig = YamlConfiguration.loadConfiguration(configFile);

            // Configuración por defecto
            defaultConfig.set("BlackJack.max_bet", 10);

            // Probabilidades ajustadas - más favorable al dealer
            defaultConfig.set("BlackJack.probabilities.blackjack_chance", 0.03); // 3% de BlackJack natural
            defaultConfig.set("BlackJack.probabilities.dealer_bust_chance", 0.20); // 20% dealer se pase
            defaultConfig.set("BlackJack.probabilities.player_win_chance", 0.35); // 35% jugador gana normalmente

            // Multiplicadores de pago
            defaultConfig.set("BlackJack.payouts.blackjack_multiplier", 2.5); // BlackJack paga 2.5x
            defaultConfig.set("BlackJack.payouts.win_multiplier", 2); // Victoria normal paga 2x
            defaultConfig.set("BlackJack.payouts.push_multiplier", 1); // Empate devuelve apuesta

            defaultConfig.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Error creando configuración de BlackJack: " + e.getMessage());
        }
    }

    private void initializeCardModelData() {
        String[] suits = {"♠", "♥", "♦", "♣"};
        String[] ranks = {"A", "2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K"};

        int modelData = 5000;
        for (String suit : suits) {
            for (String rank : ranks) {
                cardModelData.put(rank + suit, modelData++);
            }
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getClickedBlock() == null || event.getClickedBlock().getType() != Material.YELLOW_GLAZED_TERRACOTTA) return;

        event.setCancelled(true);
        Player player = event.getPlayer();
        Location blockLoc = event.getClickedBlock().getLocation();
        UUID playerId = player.getUniqueId();

        // Verificar si la mesa ya está siendo usada
        UUID currentUser = tableUsers.get(blockLoc);
        if (currentUser != null) {
            // NUEVO: Si es el mismo jugador que tenía un juego activo, permitir reconexión
            if (currentUser.equals(playerId) && isPlaying.getOrDefault(playerId, false)) {
                cancelReconnectTimer(playerId);
                openBlackJack(player, blockLoc);
                player.sendMessage(ChatColor.of("#B5EAD7") + "۞ ¡Bienvenido de vuelta! Tu partida continúa.");
                return;
            }

            Player currentPlayer = Bukkit.getPlayer(currentUser);
            if (currentPlayer != null && currentPlayer.isOnline()) {
                player.sendMessage(ChatColor.of("#FF6B6B") + "۞ Esta mesa está siendo usada por " + currentPlayer.getName());
                return;
            } else {
                // Limpiar usuario desconectado
                tableUsers.remove(blockLoc);
                cleanupDisplays(blockLoc);
            }
        }

        // Verificar que tenga fichas
        if (!hasVithiumTokens(player)) {
            player.sendMessage(ChatColor.of("#FFB3BA") + "۞ Necesitas Vithium Fichas para jugar BlackJack.");
            return;
        }

        // Registrar usuario en la mesa
        tableUsers.put(blockLoc, player.getUniqueId());
        openBlackJack(player, blockLoc);
    }

    private boolean hasVithiumTokens(Player player) {
        ItemStack tokenItem = EconomyItems.createVithiumToken();
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.isSimilar(tokenItem)) {
                return true;
            }
        }
        return false;
    }

    private void openBlackJack(Player player, Location tableLoc) {
        Inventory inv = Bukkit.createInventory(null, 36, title);
        setupGUI(inv, player); // NUEVO: Pasar player para restaurar estado
        player.openInventory(inv);

        // Guardar la ubicación de la mesa
        player.setMetadata("blackjack_table_location", new org.bukkit.metadata.FixedMetadataValue(plugin, tableLoc));

        // Sonido de apertura
        tableLoc.getWorld().playSound(tableLoc, Sound.BLOCK_CHEST_OPEN, 1.0f, 0.8f);
    }

    // NUEVO: Método setupGUI modificado para restaurar estado
    private void setupGUI(Inventory inv, Player player) {
        UUID playerId = player.getUniqueId();

        // Botón de cerrar (slot 0)
        ItemStack closeItem = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = closeItem.getItemMeta();
        closeMeta.setDisplayName(ChatColor.of("#FF6B6B") + "" + ChatColor.BOLD + "Cerrar");
        closeMeta.setCustomModelData(6000);
        closeItem.setItemMeta(closeMeta);
        inv.setItem(closeButton, closeItem);

        // Slot para fichas (slot 31) - SIN ITEM POR DEFECTO, igual que en SlotMachine
        // Dejamos el slot vacío para evitar duplicaciones

        // Botón de repartir (slot 25)
        ItemStack dealItem = new ItemStack(Material.LIME_CONCRETE);
        ItemMeta dealMeta = dealItem.getItemMeta();
        dealMeta.setDisplayName(ChatColor.of("#90EE90") + "" + ChatColor.BOLD + "REPARTIR");
        dealMeta.setLore(Arrays.asList(
                "",
                ChatColor.of("#C7CEEA") + "Coloca " + ChatColor.of("#FFD3A5") + "Vithium Fichas",
                ChatColor.of("#C7CEEA") + "y haz clic para empezar",
                ChatColor.of("#C7CEEA") + "Máximo: " + ChatColor.of("#FFD3A5") + config.getInt("BlackJack.max_bet", 10) + " fichas",
                ""
        ));
        dealMeta.setCustomModelData(6000);
        dealItem.setItemMeta(dealMeta);
        inv.setItem(dealButton, dealItem);

        // Botón de pedir carta (slot 33)
        ItemStack hitItem = new ItemStack(Material.EMERALD_BLOCK);
        ItemMeta hitMeta = hitItem.getItemMeta();
        hitMeta.setDisplayName(ChatColor.of("#90EE90") + "" + ChatColor.BOLD + "PEDIR");
        hitMeta.setLore(Arrays.asList(
                "",
                ChatColor.of("#C7CEEA") + "Pedir otra carta",
                ""
        ));
        hitMeta.setCustomModelData(6000);
        hitItem.setItemMeta(hitMeta);
        inv.setItem(hitButton, hitItem);

        // Botón de plantarse (slot 34)
        ItemStack standItem = new ItemStack(Material.REDSTONE_BLOCK);
        ItemMeta standMeta = standItem.getItemMeta();
        standMeta.setDisplayName(ChatColor.of("#FF6B6B") + "" + ChatColor.BOLD + "PLANTARSE");
        standMeta.setLore(Arrays.asList(
                "",
                ChatColor.of("#C7CEEA") + "Mantener tu mano actual",
                ""
        ));
        standMeta.setCustomModelData(6000);
        standItem.setItemMeta(standMeta);
        inv.setItem(standButton, standItem);

        // Llenar espacios vacíos con paneles grises
        for (int i = 0; i < inv.getSize(); i++) {
            if (i != closeButton && i != tokenSlot && i != dealButton &&
                    i != hitButton && i != standButton &&
                    !playerCardSlots.contains(i) && !dealerCardSlots.contains(i) &&
                    inv.getItem(i) == null) {

                ItemStack frame = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
                ItemMeta frameMeta = frame.getItemMeta();
                frameMeta.setDisplayName(" ");
                frameMeta.setCustomModelData(6000);
                frame.setItemMeta(frameMeta);
                inv.setItem(i, frame);
            }
        }

        // NUEVO: Si hay un juego en curso, restaurar las cartas
        if (isPlaying.getOrDefault(playerId, false)) {
            updateCards(inv, playerId);

            // Recrear displays 3D si es necesario
            if (player.hasMetadata("blackjack_table_location")) {
                Location tableLoc = (Location) player.getMetadata("blackjack_table_location").get(0).value();
                List<Card> dealerHand = dealerHands.get(playerId);
                List<Card> playerHand = playerHands.get(playerId);

                if (dealerHand != null && playerHand != null) {
                    createItemDisplays(tableLoc);
                    updateDisplayItems(tableLoc, dealerHand, playerHand, !playerStand.getOrDefault(playerId, false));
                }
            }
        }
    }

    private void createItemDisplays(Location tableLoc) {
        if (activeDisplays.containsKey(tableLoc)) {
            cleanupDisplays(tableLoc);
        }

        List<ItemDisplay> displays = new ArrayList<>();
        World world = tableLoc.getWorld();
        float yaw = getBlockYaw(tableLoc); // Obtener la rotación del bloque

        // Asegurarse de que el chunk esté cargado
        if (!tableLoc.getChunk().isLoaded()) {
            tableLoc.getChunk().load();
        }

        // Crear displays para dealer (fila superior)
        for (int i = 0; i < 5; i++) {
            Location dealerDisplayLoc = tableLoc.clone()
                    .add(rotatePosition(dealerDisplayPositions[i], 0, 0.5, yaw)) // Rotar posición según yaw
                    .add(0.5, dealerDisplayHeight, 0.5);

            ItemDisplay dealerDisplay = world.spawn(dealerDisplayLoc, ItemDisplay.class, itemDisplay -> {
                ItemStack displayItem = new ItemStack(Material.PAPER);
                ItemMeta meta = displayItem.getItemMeta();
                meta.setCustomModelData(6000);
                displayItem.setItemMeta(meta);

                itemDisplay.setItemStack(displayItem);
                itemDisplay.setBillboard(Display.Billboard.CENTER);

                Transformation transformation = itemDisplay.getTransformation();
                transformation.getScale().set(0.3f);
                transformation.getTranslation().set(0, 0, 0);
                itemDisplay.setTransformation(transformation);

                itemDisplay.setPersistent(true);
                itemDisplay.setInvulnerable(true);
                itemDisplay.setGlowing(false);
                itemDisplay.setBrightness(new Display.Brightness(15, 15));
                itemDisplay.setViewRange(2.0f);

                // Rotar el display según la orientación del bloque
                itemDisplay.setRotation(yaw, 0);
            });

            displays.add(dealerDisplay);
        }

        // Crear displays para jugador (fila inferior)
        for (int i = 0; i < 5; i++) {
            Location playerDisplayLoc = tableLoc.clone()
                    .add(rotatePosition(playerDisplayPositions[i], 0, 0.5, yaw)) // Rotar posición según yaw
                    .add(0.5, playerDisplayHeight, 0.5);

            ItemDisplay playerDisplay = world.spawn(playerDisplayLoc, ItemDisplay.class, itemDisplay -> {
                ItemStack displayItem = new ItemStack(Material.PAPER);
                ItemMeta meta = displayItem.getItemMeta();
                meta.setCustomModelData(6000);
                displayItem.setItemMeta(meta);

                itemDisplay.setItemStack(displayItem);
                itemDisplay.setBillboard(Display.Billboard.CENTER);

                Transformation transformation = itemDisplay.getTransformation();
                transformation.getScale().set(0.3f);
                transformation.getTranslation().set(0, 0, 0);
                itemDisplay.setTransformation(transformation);

                itemDisplay.setPersistent(true);
                itemDisplay.setInvulnerable(true);
                itemDisplay.setGlowing(false);
                itemDisplay.setBrightness(new Display.Brightness(15, 15));
                itemDisplay.setViewRange(2.0f);

                // Rotar el display según la orientación del bloque
                itemDisplay.setRotation(yaw, 0);
            });

            displays.add(playerDisplay);
        }

        activeDisplays.put(tableLoc, displays);
        world.getChunkAt(tableLoc).setForceLoaded(true);
    }

    // Método para rotar una posición según el yaw
    private Vector rotatePosition(double x, double y, double z, float yaw) {
        // Convertir yaw a radianes
        double angle = Math.toRadians(yaw);

        // Matriz de rotación para el eje Y (horizontal)
        double newX = x * Math.cos(angle) - z * Math.sin(angle);
        double newZ = x * Math.sin(angle) + z * Math.cos(angle);

        return new Vector(newX, y, newZ);
    }

    private void updateDisplayItems(Location tableLoc, List<Card> dealerHand, List<Card> playerHand, boolean hideDealer) {
        List<ItemDisplay> displays = activeDisplays.get(tableLoc);
        if (displays == null || displays.size() != 10) {
            createItemDisplays(tableLoc);
            displays = activeDisplays.get(tableLoc);
            if (displays == null) return;
        }

        float yaw = getBlockYaw(tableLoc); // Obtener rotación actual

        // Actualizar displays del dealer (primeros 5)
        for (int i = 0; i < 5; i++) {
            ItemDisplay display = displays.get(i);
            if (display == null || !display.isValid()) {
                createItemDisplays(tableLoc);
                return;
            }

            // Mantener la rotación
            display.setRotation(yaw, 0);

            ItemStack displayItem;
            if (i < dealerHand.size()) {
                if (i == 1 && hideDealer) {
                    displayItem = new ItemStack(Material.PAPER);
                    ItemMeta meta = displayItem.getItemMeta();
                    meta.setDisplayName(ChatColor.DARK_GRAY + "Carta Oculta");
                    meta.setCustomModelData(6000);
                    displayItem.setItemMeta(meta);
                } else {
                    displayItem = createCardItem(dealerHand.get(i));
                }
            } else {
                displayItem = new ItemStack(Material.AIR);
            }

            display.setItemStack(displayItem);
        }

        // Actualizar displays del jugador (últimos 5)
        for (int i = 0; i < 5; i++) {
            ItemDisplay display = displays.get(i + 5);
            if (display == null || !display.isValid()) {
                createItemDisplays(tableLoc);
                return;
            }

            // Mantener la rotación
            display.setRotation(yaw, 0);

            ItemStack displayItem;
            if (i < playerHand.size()) {
                displayItem = createCardItem(playerHand.get(i));
            } else {
                displayItem = new ItemStack(Material.AIR);
            }

            display.setItemStack(displayItem);
        }
    }

    private void cleanupDisplays(Location tableLoc) {
        List<ItemDisplay> displays = activeDisplays.get(tableLoc);
        if (displays != null) {
            if (tableLoc != null && tableLoc.getWorld() != null && !tableLoc.getChunk().isLoaded()) {
                tableLoc.getChunk().load();
            }

            for (ItemDisplay display : displays) {
                try {
                    if (display != null && display.isValid()) {
                        display.remove();
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Error al limpiar ItemDisplay: " + e.getMessage());
                }
            }
            activeDisplays.remove(tableLoc);

            if (tableLoc != null && tableLoc.getWorld() != null) {
                tableLoc.getChunk().setForceLoaded(false);
            }
        }
    }

    private float getBlockYaw(Location blockLoc) {
        Block block = blockLoc.getBlock();
        BlockData data = block.getBlockData();

        if (data instanceof Directional) {
            Directional directional = (Directional) data;
            BlockFace face = directional.getFacing();

            // Convertir BlockFace a grados (yaw)
            switch (face) {
                case NORTH: return 180f;
                case EAST: return -90f;
                case SOUTH: return 0f;
                case WEST: return 90f;
                default: return 0f;
            }
        }
        return 0f;
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        List<Location> toRemove = new ArrayList<>();
        for (Location loc : activeDisplays.keySet()) {
            if (loc.getChunk().equals(event.getChunk())) {
                cleanupDisplays(loc);
                toRemove.add(loc);
            }
        }
        toRemove.forEach(activeDisplays::remove);
        toRemove.forEach(tableUsers::remove);
    }

    private Card drawCard() {
        String[] suits = {"♠", "♥", "♦", "♣"};
        String[] ranks = {"A", "2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K"};

        Random random = new Random();
        String suit = suits[random.nextInt(suits.length)];
        String rank = ranks[random.nextInt(ranks.length)];

        return new Card(rank, suit);
    }

    private void startGame(Player player, Inventory inv) {
        UUID playerId = player.getUniqueId();

        if (isPlaying.getOrDefault(playerId, false)) return;

        ItemStack wager = inv.getItem(tokenSlot);
        if (wager == null || !wager.isSimilar(EconomyItems.createVithiumToken())) {
            player.sendMessage(ChatColor.of("#FFB3BA") + "۞ ¡Coloca Vithium Fichas primero!");
            return;
        }

        // Verificar límite máximo de apuesta
        int maxBet = config.getInt("BlackJack.max_bet", 10);
        if (wager.getAmount() > maxBet) {
            player.sendMessage(ChatColor.of("#FFB3BA") + "۞ ¡Solo puedes apostar máximo " + maxBet + " fichas!");
            return;
        }

        // CONSUMIR LAS FICHAS AL EMPEZAR
        int betAmount = wager.getAmount();
        inv.setItem(tokenSlot, null); // Remover todas las fichas del slot

        // NUEVO: Guardar la apuesta del jugador
        playerBets.put(playerId, betAmount);

        Location tableLoc = null;
        if (player.hasMetadata("blackjack_table_location")) {
            tableLoc = (Location) player.getMetadata("blackjack_table_location").get(0).value();
        }

        List<Card> playerHand = new ArrayList<>();
        List<Card> dealerHand = new ArrayList<>();

        playerHand.add(drawCard());
        dealerHand.add(drawCard());
        playerHand.add(drawCard());
        dealerHand.add(drawCard());

        playerHands.put(playerId, playerHand);
        dealerHands.put(playerId, dealerHand);
        playerStand.put(playerId, false);
        isPlaying.put(playerId, true);

        // Crear displays 3D
        if (tableLoc != null) {
            createItemDisplays(tableLoc);
            updateDisplayItems(tableLoc, dealerHand, playerHand, true);
        }

        updateCards(inv, playerId);

        if (calculateHandValue(playerHand) == 21) {
            blackjack(player, inv, betAmount);
        } else {
            player.sendMessage(ChatColor.of("#B5EAD7") + "۞ ¡Partida iniciada! Tu mano: " + calculateHandValue(playerHand));
        }

        // Sonido de repartir cartas
        if (tableLoc != null) {
            tableLoc.getWorld().playSound(tableLoc, Sound.ENTITY_ITEM_PICKUP, 1.0f, 1.2f);
        }
    }

    private void updateCards(Inventory inv, UUID playerId) {
        List<Card> playerHand = playerHands.get(playerId);
        List<Card> dealerHand = dealerHands.get(playerId);
        boolean playerStanding = playerStand.getOrDefault(playerId, false);

        // Actualizar cartas del jugador
        for (int i = 0; i < playerCardSlots.size(); i++) {
            if (i < playerHand.size()) {
                Card card = playerHand.get(i);
                ItemStack cardItem = createCardItem(card);
                inv.setItem(playerCardSlots.get(i), cardItem);
            } else {
                inv.setItem(playerCardSlots.get(i), null);
            }
        }

        // Actualizar cartas del dealer
        for (int i = 0; i < dealerCardSlots.size(); i++) {
            if (i < dealerHand.size()) {
                Card card = dealerHand.get(i);
                ItemStack cardItem;

                if (i == 1 && !playerStanding) {
                    // Carta oculta
                    cardItem = new ItemStack(Material.PAPER);
                    ItemMeta meta = cardItem.getItemMeta();
                    meta.setDisplayName(ChatColor.DARK_GRAY + "Carta Oculta");
                    meta.setCustomModelData(6000);
                    cardItem.setItemMeta(meta);
                } else {
                    cardItem = createCardItem(card);
                }
                inv.setItem(dealerCardSlots.get(i), cardItem);
            } else {
                inv.setItem(dealerCardSlots.get(i), null);
            }
        }

        // Actualizar displays 3D
        Player player = Bukkit.getPlayer(playerId);
        if (player != null && player.hasMetadata("blackjack_table_location")) {
            Location tableLoc = (Location) player.getMetadata("blackjack_table_location").get(0).value();
            updateDisplayItems(tableLoc, dealerHand, playerHand, !playerStanding);
        }
    }

    private ItemStack createCardItem(Card card) {
        ItemStack cardItem = new ItemStack(Material.PAPER);
        ItemMeta meta = cardItem.getItemMeta();

        String cardString = card.toString();
        meta.setDisplayName(ChatColor.of("#FFD3A5") + cardString);

        Integer modelData = cardModelData.get(cardString);
        if (modelData != null) {
            meta.setCustomModelData(modelData);
        } else {
            meta.setCustomModelData(6000);
        }

        cardItem.setItemMeta(meta);
        return cardItem;
    }

    private int calculateHandValue(List<Card> hand) {
        int value = 0;
        int aces = 0;

        for (Card card : hand) {
            if (card.rank.equals("A")) {
                aces++;
            } else if (card.rank.equals("K") || card.rank.equals("Q") || card.rank.equals("J")) {
                value += 10;
            } else {
                value += Integer.parseInt(card.rank);
            }
        }

        for (int i = 0; i < aces; i++) {
            if (value + 11 <= 21) {
                value += 11;
            } else {
                value += 1;
            }
        }

        return value;
    }

    private void hit(Player player, Inventory inv) {
        UUID playerId = player.getUniqueId();

        if (!isPlaying.getOrDefault(playerId, false) || playerStand.getOrDefault(playerId, false)) return;

        List<Card> playerHand = playerHands.get(playerId);
        if (playerHand == null) return;

        playerHand.add(drawCard());
        updateCards(inv, playerId);

        int value = calculateHandValue(playerHand);
        if (value > 21) {
            bust(player, inv);
        } else if (value == 21) {
            stand(player, inv);
        } else {
            player.sendMessage(ChatColor.of("#B5EAD7") + "۞ Nueva carta. Tu mano: " + value);
        }

        // Sonido de carta
        if (player.hasMetadata("blackjack_table_location")) {
            Location tableLoc = (Location) player.getMetadata("blackjack_table_location").get(0).value();
            tableLoc.getWorld().playSound(tableLoc, Sound.ENTITY_ITEM_PICKUP, 0.8f, 1.0f);
        }
    }

    private void stand(Player player, Inventory inv) {
        UUID playerId = player.getUniqueId();

        if (!isPlaying.getOrDefault(playerId, false) || playerStand.getOrDefault(playerId, false)) return;

        playerStand.put(playerId, true);
        updateCards(inv, playerId);

        List<Card> dealerHand = dealerHands.get(playerId);
        if (dealerHand == null) return;

        player.sendMessage(ChatColor.of("#B5EAD7") + "۞ Te plantas con: " + calculateHandValue(playerHands.get(playerId)));

        // Dealer debe pedir cartas hasta 17
        new BukkitRunnable() {
            @Override
            public void run() {
                if (calculateHandValue(dealerHand) < 17) {
                    dealerHand.add(drawCard());
                    updateCards(inv, playerId);

                    // Sonido de carta del dealer
                    if (player.hasMetadata("blackjack_table_location")) {
                        Location tableLoc = (Location) player.getMetadata("blackjack_table_location").get(0).value();
                        tableLoc.getWorld().playSound(tableLoc, Sound.ENTITY_ITEM_PICKUP, 0.8f, 0.8f);
                    }
                } else {
                    checkWinner(player, inv);
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    private void checkWinner(Player player, Inventory inv) {
        UUID playerId = player.getUniqueId();

        List<Card> playerHand = playerHands.get(playerId);
        List<Card> dealerHand = dealerHands.get(playerId);

        if (playerHand == null || dealerHand == null) return;

        int playerValue = calculateHandValue(playerHand);
        int dealerValue = calculateHandValue(dealerHand);

        // ARREGLADO: Obtener la apuesta real del jugador
        int betAmount = playerBets.getOrDefault(playerId, 1);

        if (dealerValue > 21 || playerValue > dealerValue) {
            win(player, inv, betAmount);
        } else if (dealerValue > playerValue) {
            lose(player, inv, "۞ ¡El dealer gana con " + dealerValue + "!");
        } else {
            push(player, inv, betAmount);
        }
    }

    private void blackjack(Player player, Inventory inv, int betAmount) {
        double multiplier = config.getDouble("BlackJack.payouts.blackjack_multiplier", 2.5);
        int winAmount = (int) Math.round(betAmount * multiplier);

        ItemStack reward = EconomyItems.createVithiumToken();
        reward.setAmount(winAmount);

        HashMap<Integer, ItemStack> remaining = player.getInventory().addItem(reward);
        if (!remaining.isEmpty()) {
            for (ItemStack item : remaining.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), item);
            }
        }

        // Sonidos de BlackJack
        if (player.hasMetadata("blackjack_table_location")) {
            Location tableLoc = (Location) player.getMetadata("blackjack_table_location").get(0).value();
            tableLoc.getWorld().playSound(tableLoc, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
            tableLoc.getWorld().playSound(tableLoc, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.2f);

            // Efectos de partículas
            World world = tableLoc.getWorld();
            Location effectLoc = tableLoc.clone().add(0.5, 2, 0.5);
            world.spawnParticle(Particle.TOTEM_OF_UNDYING, effectLoc, 15, 0.5, 0.5, 0.5, 0.1);
        }

        player.sendMessage(ChatColor.of("#B5EAD7") + "۞ ¡BLACKJACK! ¡Ganaste " + ChatColor.of("#FFD3A5") + winAmount + ChatColor.of("#B5EAD7") + " Vithium Fichas!");

        endGame(player.getUniqueId());
    }

    private void win(Player player, Inventory inv, int betAmount) {
        int multiplier = config.getInt("BlackJack.payouts.win_multiplier", 2);
        int winAmount = betAmount * multiplier;

        ItemStack reward = EconomyItems.createVithiumToken();
        reward.setAmount(winAmount);

        HashMap<Integer, ItemStack> remaining = player.getInventory().addItem(reward);
        if (!remaining.isEmpty()) {
            for (ItemStack item : remaining.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), item);
            }
        }

        // Sonido de victoria
        if (player.hasMetadata("blackjack_table_location")) {
            Location tableLoc = (Location) player.getMetadata("blackjack_table_location").get(0).value();
            tableLoc.getWorld().playSound(tableLoc, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.5f);
            tableLoc.getWorld().playSound(tableLoc, Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 1.0f);

            // Efectos de partículas
            World world = tableLoc.getWorld();
            Location effectLoc = tableLoc.clone().add(0.5, 2, 0.5);
            world.spawnParticle(Particle.TRIAL_SPAWNER_DETECTION_OMINOUS, effectLoc, 8, 0.3, 0.3, 0.3, 0.05);
        }

        player.sendMessage(ChatColor.of("#B5EAD7") + "۞ ¡Ganaste " + ChatColor.of("#FFD3A5") + winAmount + ChatColor.of("#B5EAD7") + " Vithium Fichas!");
        endGame(player.getUniqueId());
    }

    private void push(Player player, Inventory inv, int betAmount) {
        ItemStack reward = EconomyItems.createVithiumToken();
        reward.setAmount(betAmount);

        HashMap<Integer, ItemStack> remaining = player.getInventory().addItem(reward);
        if (!remaining.isEmpty()) {
            for (ItemStack item : remaining.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), item);
            }
        }

        // Sonido de empate
        if (player.hasMetadata("blackjack_table_location")) {
            Location tableLoc = (Location) player.getMetadata("blackjack_table_location").get(0).value();
            tableLoc.getWorld().playSound(tableLoc, Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
        }

        player.sendMessage(ChatColor.of("#FFD3A5") + "۞ ¡Empate! Tu apuesta ha sido devuelta.");
        endGame(player.getUniqueId());
    }

    private void bust(Player player, Inventory inv) {
        lose(player, inv, "۞ ¡Te pasaste! Tu mano supera 21!");
    }

    private void lose(Player player, Inventory inv, String message) {
        // Sonido de pérdida
        if (player.hasMetadata("blackjack_table_location")) {
            Location tableLoc = (Location) player.getMetadata("blackjack_table_location").get(0).value();
            tableLoc.getWorld().playSound(tableLoc, Sound.ENTITY_VILLAGER_NO, 1.0f, 0.8f);
            tableLoc.getWorld().playSound(tableLoc, Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
        }

        player.sendMessage(ChatColor.of("#FF6B6B") + message);
        endGame(player.getUniqueId());
    }

    private void endGame(UUID playerId) {
        isPlaying.remove(playerId);
        playerHands.remove(playerId);
        dealerHands.remove(playerId);
        playerStand.remove(playerId);
        playerBets.remove(playerId); // NUEVO: Limpiar apuesta

        // NUEVO: Cancelar timer de reconexión si existe
        BukkitRunnable timer = reconnectTimers.remove(playerId);
        if (timer != null) {
            timer.cancel();
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(title)) return;

        Player player = (Player) event.getWhoClicked();
        UUID playerId = player.getUniqueId();

        // Permitir interacción completa con el inventario del jugador
        if (event.getClickedInventory() == player.getInventory()) {
            return; // No cancelar - permitir interacción normal
        }

        // Solo manejar clicks en el inventario de blackjack
        if (event.getClickedInventory() != event.getView().getTopInventory()) {
            return;
        }

        // Manejar slot de fichas (31)
        if (event.getSlot() == tokenSlot) {
            handleTokenSlot(event);
            return;
        }

        // Para todos los otros slots, cancelar por defecto
        event.setCancelled(true);

        if (event.getCurrentItem() == null) return;

        switch (event.getSlot()) {
            case 0 -> player.closeInventory(); // Botón cerrar
            case 25 -> { // Botón repartir
                if (!isPlaying.getOrDefault(playerId, false)) {
                    startGame(player, event.getInventory());
                }
            }
            case 33 -> { // Botón pedir carta
                if (isPlaying.getOrDefault(playerId, false) && !playerStand.getOrDefault(playerId, false)) {
                    hit(player, event.getInventory());
                }
            }
            case 34 -> { // Botón plantarse
                if (isPlaying.getOrDefault(playerId, false) && !playerStand.getOrDefault(playerId, false)) {
                    stand(player, event.getInventory());
                }
            }
        }
    }

    private void handleTokenSlot(InventoryClickEvent event) {
        ItemStack cursor = event.getCursor();
        ItemStack current = event.getCurrentItem();
        ItemStack vithiumToken = EconomyItems.createVithiumToken();

        // Si está colocando fichas
        if (cursor != null && cursor.isSimilar(vithiumToken)) {
            // Verificar límite máximo
            int maxBet = config.getInt("BlackJack.max_bet", 10);
            int currentAmount = (current != null && current.isSimilar(vithiumToken)) ? current.getAmount() : 0;
            int newTotal = currentAmount + cursor.getAmount();

            if (newTotal > maxBet) {
                event.setCancelled(true);
                Player player = (Player) event.getWhoClicked();
                player.sendMessage(ChatColor.of("#FFB3BA") + "۞ Solo puedes apostar máximo " + maxBet + " fichas.");
                return;
            }

            event.setCancelled(false);
            return;
        }

        // Si está sacando fichas
        if (current != null && current.isSimilar(vithiumToken)) {
            event.setCancelled(false);
            return;
        }

        // Si está intentando colocar otro item
        if (cursor != null && !cursor.getType().equals(Material.AIR)) {
            event.setCancelled(true);
            Player player = (Player) event.getWhoClicked();
            player.sendMessage(ChatColor.of("#FFB3BA") + "۞ Solo puedes colocar Vithium Fichas aquí.");
            return;
        }

        // Permitir click con mano vacía
        event.setCancelled(false);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!event.getView().getTitle().equals(title)) return;

        Player player = (Player) event.getPlayer();
        UUID playerId = player.getUniqueId();

        // NUEVO: Si hay un juego en curso, iniciar timer de reconexión
        if (isPlaying.getOrDefault(playerId, false)) {
            startReconnectTimer(player);
            return; // No limpiar nada si hay juego activo
        }

        // Solo devolver fichas si NO hay juego en curso
        ItemStack itemInTokenSlot = event.getInventory().getItem(tokenSlot);
        if (itemInTokenSlot != null) {
            HashMap<Integer, ItemStack> remaining = player.getInventory().addItem(itemInTokenSlot);
            if (!remaining.isEmpty()) {
                for (ItemStack item : remaining.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), item);
                }
            }
        }

        // Solo liberar mesa si NO hay juego activo
        if (player.hasMetadata("blackjack_table_location")) {
            Location tableLoc = (Location) player.getMetadata("blackjack_table_location").get(0).value();
            tableUsers.remove(tableLoc);

            // Limpiar displays después de un delay
            new BukkitRunnable() {
                @Override
                public void run() {
                    cleanupDisplays(tableLoc);
                }
            }.runTaskLater(plugin, 20L); // 1 segundo de delay

            player.removeMetadata("blackjack_table_location", plugin);
        }

        // Solo limpiar datos si NO hay juego activo
        endGame(player.getUniqueId());
    }

    // NUEVO: Método para manejar timer de reconexión
    private void startReconnectTimer(Player player) {
        UUID playerId = player.getUniqueId();

        // Cancelar timer anterior si existe
        BukkitRunnable existingTimer = reconnectTimers.get(playerId);
        if (existingTimer != null) {
            existingTimer.cancel();
        }

        player.sendMessage(ChatColor.of("#FFD3A5") + "۞ Tienes 1 minuto para volver a la mesa o perderás tu apuesta.");

        BukkitRunnable timer = new BukkitRunnable() {
            @Override
            public void run() {
                // Si el jugador no ha vuelto, devolver fichas y limpiar
                if (isPlaying.getOrDefault(playerId, false)) {
                    Player onlinePlayer = Bukkit.getPlayer(playerId);
                    if (onlinePlayer != null) {
                        // Devolver apuesta
                        int betAmount = playerBets.getOrDefault(playerId, 0);
                        if (betAmount > 0) {
                            ItemStack refund = EconomyItems.createVithiumToken();
                            refund.setAmount(betAmount);

                            HashMap<Integer, ItemStack> remaining = onlinePlayer.getInventory().addItem(refund);
                            if (!remaining.isEmpty()) {
                                for (ItemStack item : remaining.values()) {
                                    onlinePlayer.getWorld().dropItemNaturally(onlinePlayer.getLocation(), item);
                                }
                            }

                            onlinePlayer.sendMessage(ChatColor.of("#FFD3A5") + "۞ Tu apuesta de " + betAmount + " fichas ha sido devuelta por inactividad.");
                        }
                    }

                    // Limpiar mesa y datos
                    if (onlinePlayer != null && onlinePlayer.hasMetadata("blackjack_table_location")) {
                        Location tableLoc = (Location) onlinePlayer.getMetadata("blackjack_table_location").get(0).value();
                        tableUsers.remove(tableLoc);
                        cleanupDisplays(tableLoc);
                        onlinePlayer.removeMetadata("blackjack_table_location", plugin);
                    }

                    endGame(playerId);
                }

                reconnectTimers.remove(playerId);
            }
        };

        reconnectTimers.put(playerId, timer);
        timer.runTaskLater(plugin, 1200L); // 60 segundos = 1200 ticks
    }

    // NUEVO: Método para cancelar timer cuando el jugador vuelve
    private void cancelReconnectTimer(UUID playerId) {
        BukkitRunnable timer = reconnectTimers.remove(playerId);
        if (timer != null) {
            timer.cancel();
        }
    }

    private static class Card {
        final String rank;
        final String suit;

        public Card(String rank, String suit) {
            this.rank = rank;
            this.suit = suit;
        }

        @Override
        public String toString() {
            return rank + suit;
        }
    }
}