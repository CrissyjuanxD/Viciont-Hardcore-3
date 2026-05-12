package Casino;

import items.EconomyItems;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class BlackJack implements Listener {
    private final JavaPlugin plugin;
    private final CasinoManager manager;
    private final String title = ChatColor.of("#228B22") + "" + ChatColor.BOLD + "BlackJack";

    // Slots GUI
    private final List<Integer> dealerCardSlots = Arrays.asList(11, 12, 13, 14, 15);
    private final List<Integer> playerCardSlots = Arrays.asList(29, 30, 31, 32, 33);
    private final int dealerHeadSlot = 10;
    private final int playerHeadSlot = 28;

    private final int tokenSlot = 49;
    private final int dealButton = 53;
    private final int hitButton = 48;
    private final int standButton = 50;

    // Estado del Juego
    private final Map<UUID, Boolean> isPlaying = new ConcurrentHashMap<>();
    private final Map<UUID, List<Card>> playerHands = new ConcurrentHashMap<>();
    private final Map<UUID, List<Card>> dealerHands = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> playerStand = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> playerBets = new ConcurrentHashMap<>();
    private final Map<Location, UUID> tableUsers = new ConcurrentHashMap<>();

    private final Map<UUID, BukkitRunnable> reconnectTimers = new ConcurrentHashMap<>();

    private final File configFile;
    private FileConfiguration config;
    private final Map<String, Integer> cardModelData = new HashMap<>();

    public BlackJack(JavaPlugin plugin, CasinoManager manager) {
        this.plugin = plugin;
        this.manager = manager;
        this.configFile = new File(plugin.getDataFolder(), "BlackJack.yml");

        initializeCardModelData();
        loadConfig();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
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

    public void reloadConfig() { loadConfig(); }

    private void loadConfig() {
        if (!configFile.exists()) createDefaultConfig();
        config = YamlConfiguration.loadConfiguration(configFile);
    }

    private void createDefaultConfig() {
        try {
            configFile.getParentFile().mkdirs();
            configFile.createNewFile();
            FileConfiguration def = YamlConfiguration.loadConfiguration(configFile);
            def.set("BlackJack.min_bet", 5);
            def.set("BlackJack.max_bet", 64);
            def.set("BlackJack.payouts.blackjack_multiplier", 2.5);
            def.set("BlackJack.payouts.win_multiplier", 2);
            def.save(configFile);
        } catch (IOException e) { e.printStackTrace(); }
    }

    // Obtenedor de materiales a prueba de errores de versión
    private Material getSafeMaterial(String name, Material fallback) {
        Material mat = Material.matchMaterial(name);
        return mat != null ? mat : fallback;
    }

    // --- INTERACCIÓN Y RECONEXIÓN ---

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getClickedBlock() == null) return;

        Location loc = event.getClickedBlock().getLocation();
        if (!manager.isTable(loc) || !manager.getTableType(loc).equals("blackjack")) return;

        event.setCancelled(true);
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        if (tableUsers.containsKey(loc)) {
            UUID currentUser = tableUsers.get(loc);

            if (currentUser.equals(playerId)) {
                if (reconnectTimers.containsKey(playerId)) {
                    cancelReconnectTimer(playerId);
                    player.sendMessage(ChatColor.of("#B5EAD7") + "۞ ¡Bienvenido de vuelta! Tu partida continúa.");
                }
                openBlackJack(player, loc);
                return;
            } else {
                player.sendMessage(ChatColor.of("#FF6B6B") + "۞ Esta mesa está ocupada.");
                return;
            }
        }

        tableUsers.put(loc, playerId);
        manager.setGameActive(loc, true);
        openBlackJack(player, loc);
    }

    private void openBlackJack(Player player, Location tableLoc) {
        Inventory inv = Bukkit.createInventory(null, 54, title);
        setupGUI(inv, player);
        player.openInventory(inv);
        player.setMetadata("blackjack_loc", new org.bukkit.metadata.FixedMetadataValue(plugin, tableLoc));

        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 1.0f, 0.8f);
    }

    private void setupGUI(Inventory inv, Player player) {
        ItemStack pane = new ItemStack(Material.LIME_DYE);
        ItemMeta meta = pane.getItemMeta();
        meta.setDisplayName(" ");
        pane.setItemMeta(meta);

        for(int i=0; i<54; i++) {
            if (dealerCardSlots.contains(i) || playerCardSlots.contains(i) ||
                    i == dealerHeadSlot || i == playerHeadSlot ||
                    i == tokenSlot || i == dealButton || i == hitButton || i == standButton) continue;
            inv.setItem(i, pane);
        }

        ItemStack dealerHead = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta dMeta = (SkullMeta) dealerHead.getItemMeta();
        dMeta.setDisplayName(ChatColor.RED + "Crupier");
        dMeta.setOwner("MHF_Villager");
        dealerHead.setItemMeta(dMeta);
        inv.setItem(dealerHeadSlot, dealerHead);

        ItemStack playerHead = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta pMeta = (SkullMeta) playerHead.getItemMeta();
        pMeta.setDisplayName(ChatColor.GREEN + player.getName());
        pMeta.setOwningPlayer(player);
        playerHead.setItemMeta(pMeta);
        inv.setItem(playerHeadSlot, playerHead);

        ItemStack deal = new ItemStack(getSafeMaterial("RESIN_BRICK", Material.BRICK));
        ItemMeta dealMeta = deal.getItemMeta();
        dealMeta.setDisplayName(ChatColor.of("#90EE90") + "" + ChatColor.BOLD + "REPARTIR");
        dealMeta.setLore(Arrays.asList(
                "",
                ChatColor.of("#C7CEEA") + "Mínimo: " + ChatColor.of("#FFD3A5") + config.getInt("BlackJack.min_bet") + " fichas",
                ChatColor.of("#C7CEEA") + "Máximo: " + ChatColor.of("#FFD3A5") + config.getInt("BlackJack.max_bet") + " fichas",
                ""
        ));
        deal.setItemMeta(dealMeta);
        inv.setItem(dealButton, deal);

        if (isPlaying.getOrDefault(player.getUniqueId(), false)) {
            updateCards(inv, player.getUniqueId());
            setGameButtons(inv);
        } else {
            ItemStack waitingPane = new ItemStack(Material.ORANGE_DYE);
            ItemMeta wpMeta = waitingPane.getItemMeta();
            wpMeta.setDisplayName(" ");
            waitingPane.setItemMeta(wpMeta);

            inv.setItem(hitButton, waitingPane);
            inv.setItem(standButton, waitingPane);
        }
    }

    private void setGameButtons(Inventory inv) {
        ItemStack hit = new ItemStack(Material.EMERALD_BLOCK);
        ItemMeta hitM = hit.getItemMeta();
        hitM.setDisplayName(ChatColor.of("#90EE90") + "" + ChatColor.BOLD + "PEDIR CARTA");
        hit.setItemMeta(hitM);
        inv.setItem(hitButton, hit);

        ItemStack stand = new ItemStack(Material.REDSTONE_BLOCK);
        ItemMeta standM = stand.getItemMeta();
        standM.setDisplayName(ChatColor.of("#FF6B6B") + "" + ChatColor.BOLD + "PLANTARSE");
        stand.setItemMeta(standM);
        inv.setItem(standButton, stand);

        ItemStack usedDeal = new ItemStack(Material.LIME_DYE);
        ItemMeta usedDealMeta = usedDeal.getItemMeta();
        usedDealMeta.setDisplayName(" ");
        usedDeal.setItemMeta(usedDealMeta);
        inv.setItem(dealButton, usedDeal);
    }

    private void startGame(Player player, Inventory inv) {
        UUID id = player.getUniqueId();
        if (isPlaying.getOrDefault(id, false)) return;

        ItemStack bet = inv.getItem(tokenSlot);
        ItemStack token = EconomyItems.createVithiumToken();

        if (bet == null || !bet.isSimilar(token)) {
            player.sendMessage(ChatColor.of("#FFB3BA") + "۞ ¡Coloca DinoFichas para apostar!");
            return;
        }

        int amount = bet.getAmount();
        int minBet = config.getInt("BlackJack.min_bet", 5);
        int maxBet = config.getInt("BlackJack.max_bet", 64);

        if (amount < minBet) {
            player.sendMessage(ChatColor.of("#FFB3BA") + "۞ La apuesta mínima es de " + minBet + " fichas.");
            return;
        }
        if (amount > maxBet) {
            player.sendMessage(ChatColor.of("#FFB3BA") + "۞ ¡Solo puedes apostar máximo " + maxBet + " fichas!");
            return;
        }

        playerBets.put(id, amount);
        inv.setItem(tokenSlot, null);

        playerHands.put(id, new ArrayList<>());
        dealerHands.put(id, new ArrayList<>());

        playerHands.get(id).add(drawCard());
        dealerHands.get(id).add(drawCard());
        playerHands.get(id).add(drawCard());
        dealerHands.get(id).add(drawCard());

        isPlaying.put(id, true);
        playerStand.put(id, false);

        updateCards(inv, id);
        setGameButtons(inv);

        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0f, 1.2f);
        player.sendMessage(ChatColor.of("#B5EAD7") + "۞ ¡Partida iniciada! Tu mano: " + calculateValue(playerHands.get(id)));

        if (calculateValue(playerHands.get(id)) == 21) {
            endRound(player, inv, true);
        }
    }

    private void hit(Player player, Inventory inv) {
        UUID id = player.getUniqueId();
        playerHands.get(id).add(drawCard());
        updateCards(inv, id);

        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.8f, 1.0f);

        int val = calculateValue(playerHands.get(id));
        if (val > 21) {
            endRound(player, inv, false);
        } else {
            player.sendMessage(ChatColor.of("#B5EAD7") + "۞ Nueva carta. Tu mano: " + val);
        }
    }

    private void stand(Player player, Inventory inv) {
        playerStand.put(player.getUniqueId(), true);
        updateCards(inv, player.getUniqueId());

        player.sendMessage(ChatColor.of("#B5EAD7") + "۞ Te plantas con: " + calculateValue(playerHands.get(player.getUniqueId())));

        new BukkitRunnable() {
            @Override
            public void run() {
                List<Card> dealerHand = dealerHands.get(player.getUniqueId());
                if (dealerHand == null) { this.cancel(); return; }

                if (calculateValue(dealerHand) < 17) {
                    dealerHand.add(drawCard());
                    updateCards(inv, player.getUniqueId());
                    player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.8f, 0.8f);
                } else {
                    this.cancel();
                    endRound(player, inv, false);
                }
            }
        }.runTaskTimer(plugin, 10L, 20L);
    }

    private void endRound(Player player, Inventory inv, boolean naturalBlackjack) {
        UUID id = player.getUniqueId();
        int playerVal = calculateValue(playerHands.get(id));
        int dealerVal = calculateValue(dealerHands.get(id));
        int bet = playerBets.get(id);

        double multiplier = 0;
        String msg = "";

        if (naturalBlackjack) {
            multiplier = config.getDouble("BlackJack.payouts.blackjack_multiplier", 2.5);
            msg = ChatColor.of("#B5EAD7") + "۞ ¡BLACKJACK! ¡Ganaste!";

            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.2f);
            player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, player.getLocation().add(0, 1, 0), 15, 0.5, 0.5, 0.5, 0.1);

        } else if (playerVal > 21) {
            msg = ChatColor.of("#FF6B6B") + "۞ ¡Te pasaste! Tu mano supera 21.";
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 0.8f);
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);

        } else if (dealerVal > 21) {
            multiplier = config.getDouble("BlackJack.payouts.win_multiplier", 2.0);
            msg = ChatColor.of("#B5EAD7") + "۞ ¡Dealer se pasó (" + dealerVal + ")! ¡Ganas!";
            triggerWinEffects(player);

        } else if (playerVal > dealerVal) {
            multiplier = config.getDouble("BlackJack.payouts.win_multiplier", 2.0);
            msg = ChatColor.of("#B5EAD7") + "۞ ¡Ganaste! (" + playerVal + " vs " + dealerVal + ")";
            triggerWinEffects(player);

        } else if (playerVal == dealerVal) {
            multiplier = 1;
            msg = ChatColor.of("#FFD3A5") + "۞ ¡Empate! Tu apuesta ha sido devuelta.";
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);

        } else {
            msg = ChatColor.of("#FF6B6B") + "۞ Pierdes. (" + playerVal + " vs " + dealerVal + ")";
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 0.8f);
        }

        player.sendMessage(msg);

        if (multiplier > 0) {
            int winAmount = (int) (bet * multiplier);
            ItemStack win = EconomyItems.createVithiumToken();
            win.setAmount(winAmount);
            player.getInventory().addItem(win).forEach((k,v) -> player.getWorld().dropItemNaturally(player.getLocation(), v));

            if (multiplier > 1) {
                player.sendMessage(ChatColor.of("#B5EAD7") + "۞ Recibes " + ChatColor.of("#FFD3A5") + winAmount + ChatColor.of("#B5EAD7") + " DinoFichas.");
            }
        }

        cleanUpGame(id);
        setupGUI(inv, player);
    }

    private void triggerWinEffects(Player player) {
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.5f);
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 1.0f);
        player.getWorld().spawnParticle(Particle.TRIAL_SPAWNER_DETECTION_OMINOUS, player.getLocation().add(0, 2, 0), 8, 0.3, 0.3, 0.3, 0.05);
    }

    private void updateCards(Inventory inv, UUID id) {
        List<Card> pHand = playerHands.get(id);
        List<Card> dHand = dealerHands.get(id);
        boolean showAll = playerStand.getOrDefault(id, false);

        dealerCardSlots.forEach(s -> inv.setItem(s, null));
        playerCardSlots.forEach(s -> inv.setItem(s, null));

        for (int i = 0; i < dHand.size(); i++) {
            if (i >= dealerCardSlots.size()) break;

            if (i == 1 && !showAll && calculateValue(pHand) != 21) {
                ItemStack hidden = new ItemStack(Material.PAPER);
                ItemMeta meta = hidden.getItemMeta();
                meta.setDisplayName(ChatColor.GRAY + "Carta Oculta");
                meta.setCustomModelData(6000);
                hidden.setItemMeta(meta);
                inv.setItem(dealerCardSlots.get(i), hidden);
            } else {
                inv.setItem(dealerCardSlots.get(i), createCardItem(dHand.get(i)));
            }
        }

        for (int i = 0; i < pHand.size(); i++) {
            if (i >= playerCardSlots.size()) break;
            inv.setItem(playerCardSlots.get(i), createCardItem(pHand.get(i)));
        }
    }

    private ItemStack createCardItem(Card card) {
        Material mat = Material.PAPER;
        switch (card.suit) {
            case "♠": mat = getSafeMaterial("BORDURE_INDENTED_BANNER_PATTERN", Material.PAPER); break;
            case "♥": mat = getSafeMaterial("MOJANG_BANNER_PATTERN", Material.PAPER); break;
            case "♦": mat = getSafeMaterial("GLOBE_BANNER_PATTERN", Material.PAPER); break;
            case "♣": mat = getSafeMaterial("CREEPER_BANNER_PATTERN", Material.PAPER); break;
        }

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.of("#FFD3A5") + card.toString());

        String key = card.rank + card.suit;
        if (cardModelData.containsKey(key)) {
            meta.setCustomModelData(cardModelData.get(key));
        }

        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        try {
            meta.addItemFlags(ItemFlag.valueOf("HIDE_ADDITIONAL_TOOLTIP")); // 1.20.5+
        } catch (IllegalArgumentException ignored) {
            meta.addItemFlags(ItemFlag.valueOf("HIDE_ITEM_SPECIFICS")); // Versiones antiguas
        }

        item.setItemMeta(meta);
        return item;
    }

    private int calculateValue(List<Card> hand) {
        int val = 0;
        int aces = 0;
        for (Card c : hand) {
            if (c.rank.equals("A")) aces++;
            else if ("JQK".contains(c.rank)) val += 10;
            else val += Integer.parseInt(c.rank);
        }
        for (int i=0; i<aces; i++) {
            if (val + 11 <= 21) val += 11;
            else val += 1;
        }
        return val;
    }

    private Card drawCard() {
        String[] suits = {"♠", "♥", "♦", "♣"};
        String[] ranks = {"A", "2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K"};
        Random r = new Random();
        return new Card(ranks[r.nextInt(ranks.length)], suits[r.nextInt(suits.length)]);
    }

    // --- EVENTOS DE CLICK Y CIERRE CON FIX PARA BEDROCK ---

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        Player p = (Player) e.getWhoClicked();

        // FIX BEDROCK: En lugar de comprobar el título de la ventana,
        // comprobamos si el jugador tiene la sesión abierta en metadata.
        if (!p.hasMetadata("blackjack_loc")) return;

        if (e.getView().getTopInventory().getSize() != 54) return;

        e.setCancelled(true);
        Inventory clickedInv = e.getClickedInventory();

        if (isPlaying.getOrDefault(p.getUniqueId(), false)) {
            if (clickedInv == e.getView().getTopInventory()) {
                if (e.getSlot() == hitButton) hit(p, e.getInventory());
                if (e.getSlot() == standButton) stand(p, e.getInventory());
            }
            return;
        }

        if (clickedInv == e.getView().getBottomInventory()) {
            e.setCancelled(false);
            return;
        }

        if (e.getSlot() == tokenSlot && clickedInv == e.getView().getTopInventory()) {
            if (e.getCurrentItem() != null && e.getCurrentItem().getType() != Material.AIR) {
                e.setCancelled(false);
                return;
            }
            ItemStack cursor = e.getCursor();
            if (cursor != null && cursor.getType() != Material.AIR) {
                if (cursor.isSimilar(EconomyItems.createVithiumToken())) {
                    e.setCancelled(false);
                } else {
                    p.sendMessage(ChatColor.of("#FFB3BA") + "۞ Solo se aceptan DinoFichas.");
                }
            }
            return;
        }

        if (e.getSlot() == dealButton && clickedInv == e.getView().getTopInventory()) {
            startGame(p, e.getInventory());
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        Player p = (Player) e.getPlayer();

        // FIX BEDROCK: Geyser a veces distorsiona el título del inventario
        // Solo revisamos si el jugador está vinculado a una mesa de BlackJack
        if (!p.hasMetadata("blackjack_loc")) return;

        if (isPlaying.getOrDefault(p.getUniqueId(), false)) {
            Location loc = (Location) p.getMetadata("blackjack_loc").get(0).value();
            startReconnectTimer(p, loc);
            return;
        }

        ItemStack tokens = e.getInventory().getItem(tokenSlot);
        if (tokens != null) p.getInventory().addItem(tokens);

        cleanupTable(p);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        if (!p.hasMetadata("blackjack_loc")) return;

        if (isPlaying.getOrDefault(p.getUniqueId(), false)) {
            Location loc = (Location) p.getMetadata("blackjack_loc").get(0).value();
            startReconnectTimer(p, loc);
        } else {
            cleanupTable(p);
        }
    }

    // --- FUNCIONES DE LIMPIEZA Y TIMER ---

    private void cleanUpGame(UUID id) {
        isPlaying.remove(id);
        playerHands.remove(id);
        dealerHands.remove(id);
        playerStand.remove(id);
        playerBets.remove(id);

        if (reconnectTimers.containsKey(id)) {
            reconnectTimers.get(id).cancel();
            reconnectTimers.remove(id);
        }
    }

    private void cleanupTable(Player p) {
        if (p.hasMetadata("blackjack_loc")) {
            Location loc = (Location) p.getMetadata("blackjack_loc").get(0).value();
            tableUsers.remove(loc);
            manager.setGameActive(loc, false);
            p.removeMetadata("blackjack_loc", plugin);
        }
        cleanUpGame(p.getUniqueId());
    }

    private void startReconnectTimer(Player player, Location loc) {
        UUID id = player.getUniqueId();

        if (reconnectTimers.containsKey(id)) reconnectTimers.get(id).cancel();

        player.sendMessage(ChatColor.of("#FFD3A5") + "۞ Tienes 60 segundos para volver a la mesa o perderás tu sesión.");

        BukkitRunnable timer = new BukkitRunnable() {
            @Override
            public void run() {
                if (isPlaying.getOrDefault(id, false)) {
                    Player onlineP = Bukkit.getPlayer(id);

                    int bet = playerBets.getOrDefault(id, 0);
                    if (bet > 0 && onlineP != null && onlineP.isOnline()) {
                        ItemStack refund = EconomyItems.createVithiumToken();
                        refund.setAmount(bet);
                        onlineP.getInventory().addItem(refund);
                        onlineP.sendMessage(ChatColor.of("#FFD3A5") + "۞ Tiempo agotado. Se te ha devuelto la apuesta.");
                    }

                    tableUsers.remove(loc);
                    manager.setGameActive(loc, false);

                    if (onlineP != null) onlineP.removeMetadata("blackjack_loc", plugin);
                    cleanUpGame(id);
                }
                reconnectTimers.remove(id);
            }
        };

        reconnectTimers.put(id, timer);
        timer.runTaskLater(plugin, 1200L);
    }

    private void cancelReconnectTimer(UUID id) {
        if (reconnectTimers.containsKey(id)) {
            reconnectTimers.get(id).cancel();
            reconnectTimers.remove(id);
        }
    }

    private static class Card {
        String rank, suit;
        public Card(String r, String s) { rank=r; suit=s; }
        public String toString() { return rank + suit; }
    }
}