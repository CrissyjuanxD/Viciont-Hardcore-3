package Casino;

import Dificultades.DayOneChanges;
import Habilidades.HabilidadesBook;
import items.*;
import items.IceBow.IceBowItem;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SlotMachine implements Listener {
    private final JavaPlugin plugin;
    private final DoubleLifeTotem doubleLifeTotem;
    private final EconomyIceTotem economyIceTotem;
    private final EconomyFlyTotem economyFlyTotem;
    private final IceBowItem iceBowItem;
    private final CasinoManager manager;

    // Título con colores del código antiguo
    private final String title = ChatColor.of("#FF6B35") + "" + ChatColor.BOLD + "Máquina Tragamonedas";

    // Slots GUI
    private final int[] reel1Slots = {12, 21, 30};
    private final int[] reel2Slots = {13, 22, 31};
    private final int[] reel3Slots = {14, 23, 32};

    private final int tokenSlot = 49;
    private final int spinButton = 43;
    private final int closeButton = 0;

    private final Material[] symbols = {Material.COAL, Material.IRON_INGOT, Material.GOLD_INGOT, Material.DIAMOND, Material.EMERALD};

    // Estados
    private final Map<UUID, Boolean> isSpinning = new ConcurrentHashMap<>();
    private final Map<Location, UUID> machineUsers = new ConcurrentHashMap<>();
    private final Map<Location, List<ItemDisplay>> activeDisplays = new ConcurrentHashMap<>();
    private final Map<Location, BukkitRunnable> activeAnimations = new ConcurrentHashMap<>();
    private final Map<Location, AnimationState> animationStates = new ConcurrentHashMap<>();
    private final Map<Location, Long> animationStartTimes = new ConcurrentHashMap<>();

    private final File configFile;
    private FileConfiguration config;

    // Clase interna para el estado de animación
    private static class AnimationState {
        Material[] finalResults = new Material[3];
        Material[][] currentSymbols = new Material[3][3];
        boolean[] reelStopped = new boolean[3];
        int currentTick = 0;
        int maxTicks = 60;

        public AnimationState() {
            Arrays.fill(reelStopped, false);
            for (int i = 0; i < 3; i++) {
                Arrays.fill(currentSymbols[i], Material.COAL);
            }
        }

        public boolean isReelStopped(int reel) { return reelStopped[reel]; }
        public void stopReel(int reel) {
            reelStopped[reel] = true;
            currentSymbols[reel][2] = finalResults[reel];
        }
    }

    public SlotMachine(JavaPlugin plugin, CasinoManager manager) {
        this.plugin = plugin;
        this.manager = manager;
        this.doubleLifeTotem = new DoubleLifeTotem(plugin);
        this.economyIceTotem = new EconomyIceTotem(plugin);
        this.economyFlyTotem = new EconomyFlyTotem(plugin);
        this.iceBowItem = new IceBowItem(plugin);
        this.configFile = new File(plugin.getDataFolder(), "SlotMachine.yml");

        loadConfig();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
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

            // Probabilidad de ganar (Porcentaje 0 - 100)
            def.set("SlotMachine.win_chance", 15.0);

            def.set("SlotMachine.minerales.three_out_of_three", Arrays.asList("diamond 5", "gold_ingot 10"));
            def.set("SlotMachine.itemsvarios.three_out_of_three", Arrays.asList("golden_apple 2", "experience_bottle 16"));
            def.set("SlotMachine.pociones.three_out_of_three", Arrays.asList("potion 1"));
            def.set("SlotMachine.vithiums_fichas.three_out_of_three", Arrays.asList("vithiums_fichas 15"));
            def.set("SlotMachine.totems.three_out_of_three", Arrays.asList("totem_of_undying 1"));

            def.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Error SlotMachine config: " + e.getMessage());
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getClickedBlock() == null) return;

        Location loc = event.getClickedBlock().getLocation();

        if (!manager.isTable(loc) || !manager.getTableType(loc).equals("slot")) return;

        event.setCancelled(true);
        Player player = event.getPlayer();

        if (activeAnimations.containsKey(loc) && animationStartTimes.containsKey(loc)) {
            long timeElapsed = System.currentTimeMillis() - animationStartTimes.get(loc);
            if (timeElapsed > 8000) {
                forceCleanup(null, loc);
            }
        }

        if (machineUsers.containsKey(loc) && !machineUsers.get(loc).equals(player.getUniqueId())) {
            Player user = Bukkit.getPlayer(machineUsers.get(loc));
            String name = (user != null) ? user.getName() : "otro jugador";
            player.sendMessage(ChatColor.of("#FF6B6B") + "۞ Esta máquina está siendo usada por " + name);
            return;
        }

        if (activeAnimations.containsKey(loc) && !isSpinning.getOrDefault(player.getUniqueId(), false)) {
            player.sendMessage(ChatColor.of("#FF6B6B") + "۞ La máquina está terminando una animación. Espera un momento.");
            return;
        }

        machineUsers.put(loc, player.getUniqueId());
        manager.setGameActive(loc, true);

        openSlotMachine(player, loc);
    }

    private void openSlotMachine(Player player, Location machineLoc) {
        Inventory inv = Bukkit.createInventory(null, 54, title);
        setupGUI(inv, player, machineLoc);
        player.openInventory(inv);
        player.setMetadata("slot_machine_location", new org.bukkit.metadata.FixedMetadataValue(plugin, machineLoc));
        machineLoc.getWorld().playSound(machineLoc, Sound.BLOCK_CHEST_OPEN, 1.0f, 1.2f);
    }

    private void setupGUI(Inventory inv, Player player, Location machineLoc) {
        ItemStack close = new ItemStack(Material.BARRIER);
        ItemMeta cm = close.getItemMeta();
        cm.setDisplayName(ChatColor.of("#FF6B6B") + "" + ChatColor.BOLD + "Cerrar");
        cm.setCustomModelData(1000);
        close.setItemMeta(cm);
        inv.setItem(closeButton, close);

        ItemStack spin = new ItemStack(Material.LEVER);
        ItemMeta sm = spin.getItemMeta();
        sm.setDisplayName(ChatColor.of("#B5EAD7") + "" + ChatColor.BOLD + "¡GIRAR!");
        sm.setLore(Arrays.asList(
                "",
                ChatColor.of("#C7CEEA") + "Coloca una " + ChatColor.of("#FFD3A5") + "DinoFicha",
                ChatColor.of("#C7CEEA") + "en el slot inferior y haz clic aquí",
                ""
        ));
        sm.setCustomModelData(1000);
        spin.setItemMeta(sm);
        inv.setItem(spinButton, spin);

        ItemStack leftLine = new ItemStack(Material.RED_DYE);
        ItemMeta leftMeta = leftLine.getItemMeta();
        leftMeta.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + "»» " + ChatColor.RED + "Línea de Premio" + ChatColor.GOLD + " »»");
        leftMeta.setCustomModelData(1000);
        leftLine.setItemMeta(leftMeta);

        inv.setItem(27, leftLine);
        inv.setItem(28, leftLine);
        inv.setItem(29, leftLine);

        ItemStack rightLine = new ItemStack(Material.RED_DYE);
        ItemMeta rightMeta = rightLine.getItemMeta();
        rightMeta.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + "«« " + ChatColor.RED + "Línea de Premio" + ChatColor.GOLD + " ««");
        rightMeta.setCustomModelData(1000);
        rightLine.setItemMeta(rightMeta);

        inv.setItem(33, rightLine);
        inv.setItem(34, rightLine);
        inv.setItem(35, rightLine);

        ItemStack glass = new ItemStack(Material.BLACK_DYE);
        ItemMeta gm = glass.getItemMeta();
        gm.setDisplayName(" ");
        gm.setCustomModelData(1000);
        glass.setItemMeta(gm);

        for (int i = 0; i < 54; i++) {
            if (inv.getItem(i) == null && i != tokenSlot && !isReelSlot(i)) {
                inv.setItem(i, glass);
            }
        }

        ItemStack tokenIndicator = new ItemStack(Material.ORANGE_DYE);
        ItemMeta tokenMeta = tokenIndicator.getItemMeta();
        tokenMeta.setDisplayName(ChatColor.of("#FFD3A5") + "" + ChatColor.BOLD + "Coloca DinoFicha Aquí");
        tokenMeta.setCustomModelData(1000);
        tokenIndicator.setItemMeta(tokenMeta);

        int[] indicatorSlots = {39, 40, 41, 48, 50};
        for (int slot : indicatorSlots) {
            inv.setItem(slot, tokenIndicator);
        }

        AnimationState animState = animationStates.get(machineLoc);
        if (animState != null && activeAnimations.containsKey(machineLoc) && isSpinning.getOrDefault(player.getUniqueId(), false)) {
            updateGUIFromAnimationState(inv, animState);
        } else {
            Random r = new Random();
            for (int i = 0; i < 3; i++) {
                int[] reel = i == 0 ? reel1Slots : (i == 1 ? reel2Slots : reel3Slots);
                for (int slot : reel) {
                    setSymbolInSlot(inv, slot, symbols[r.nextInt(symbols.length)], false);
                }
            }
        }
    }

    private boolean isReelSlot(int slot) {
        for (int s : reel1Slots) if (s == slot) return true;
        for (int s : reel2Slots) if (s == slot) return true;
        for (int s : reel3Slots) if (s == slot) return true;
        return false;
    }

    private void updateGUIFromAnimationState(Inventory inv, AnimationState animState) {
        for (int reel = 0; reel < 3; reel++) {
            int[] slots = reel == 0 ? reel1Slots : (reel == 1 ? reel2Slots : reel3Slots);
            for (int pos = 0; pos < 3; pos++) {
                Material sym = animState.currentSymbols[reel][pos];
                boolean isFinal = animState.isReelStopped(reel) && pos == 2;
                setSymbolInSlot(inv, slots[pos], sym, isFinal);
            }
        }
    }

    private void setSymbolInSlot(Inventory inv, int slot, Material mat, boolean gold) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        String name = getSymbolName(mat);
        meta.setDisplayName(gold ?
                ChatColor.of("#FFD3A5") + "" + ChatColor.BOLD + name :
                ChatColor.GRAY + name);
        meta.setCustomModelData(100);
        item.setItemMeta(meta);
        inv.setItem(slot, item);
    }

    private String getSymbolName(Material m) {
        return switch (m) {
            case COAL -> "Carbón";
            case IRON_INGOT -> "Hierro";
            case GOLD_INGOT -> "Oro";
            case DIAMOND -> "Diamante";
            case EMERALD -> "Esmeralda";
            default -> m.name();
        };
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        Player p = (Player) e.getWhoClicked();

        if (!p.hasMetadata("slot_machine_location")) return;
        if (e.getView().getTopInventory().getSize() != 54) return;
        if (e.getClickedInventory() == p.getInventory()) return;

        if (e.getSlot() == tokenSlot) {
            if (isSpinning.getOrDefault(p.getUniqueId(), false)) {
                e.setCancelled(true);
                return;
            }
            ItemStack cursor = e.getCursor();
            if (cursor != null && cursor.getType() != Material.AIR) {
                if (!cursor.isSimilar(EconomyItems.createVithiumToken())) {
                    e.setCancelled(true);
                    p.sendMessage(ChatColor.of("#FFB3BA") + "۞ Solo puedes colocar DinoFichas aquí.");
                    return;
                }
            }
            return;
        }

        e.setCancelled(true);

        if (e.getSlot() == closeButton) {
            p.closeInventory();
        } else if (e.getSlot() == spinButton) {
            startSpin(p, e.getInventory());
        }
    }

    private void startSpin(Player p, Inventory inv) {
        if (isSpinning.getOrDefault(p.getUniqueId(), false)) return;

        ItemStack bet = inv.getItem(tokenSlot);
        if (bet == null || !bet.isSimilar(EconomyItems.createVithiumToken())) {
            p.sendMessage(ChatColor.of("#FFB3BA") + "۞ ¡Coloca una DinoFicha primero!");
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.8f);
            return;
        }

        bet.setAmount(bet.getAmount() - 1);
        inv.setItem(tokenSlot, bet.getAmount() > 0 ? bet : null);

        isSpinning.put(p.getUniqueId(), true);
        Location loc = null;
        if (p.hasMetadata("slot_machine_location")) {
            loc = (Location) p.getMetadata("slot_machine_location").get(0).value();
        }
        final Location machineLoc = loc;

        AnimationState state = new AnimationState();
        Random r = new Random();

        double winChance = config.getDouble("SlotMachine.win_chance", 15.0);
        double roll = r.nextDouble() * 100;
        boolean shouldWin = roll <= winChance;

        if (shouldWin) {
            Material winningSymbol = symbols[r.nextInt(symbols.length)];
            state.finalResults[0] = winningSymbol;
            state.finalResults[1] = winningSymbol;
            state.finalResults[2] = winningSymbol;
        } else {
            do {
                state.finalResults[0] = symbols[r.nextInt(symbols.length)];
                state.finalResults[1] = symbols[r.nextInt(symbols.length)];
                state.finalResults[2] = symbols[r.nextInt(symbols.length)];
            } while (state.finalResults[0] == state.finalResults[1] && state.finalResults[1] == state.finalResults[2]);
        }

        for(int i=0; i<3; i++) {
            for(int j=0; j<3; j++) state.currentSymbols[i][j] = symbols[r.nextInt(symbols.length)];
        }

        if (machineLoc != null) {
            // Asegurarnos de borrar CUALQUIER display viejo antes de empezar uno nuevo
            cleanupDisplays(machineLoc);

            animationStates.put(machineLoc, state);
            animationStartTimes.put(machineLoc, System.currentTimeMillis());
            machineLoc.getWorld().playSound(machineLoc, Sound.BLOCK_PISTON_EXTEND, 1.0f, 1.0f);
            machineLoc.getWorld().playSound(machineLoc, Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.2f);
            createItemDisplays(machineLoc, p);
        }

        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    state.currentTick++;
                    int[] stops = {40, 50, 60};

                    for(int reel=0; reel<3; reel++) {
                        if (!state.isReelStopped(reel)) {
                            if (state.currentTick >= stops[reel]) {
                                state.stopReel(reel);
                                p.playSound(p.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.5f, 1.5f);
                            } else {
                                if (reel == 0 && state.currentTick % 4 == 0) {
                                    p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.3f, 1.0f + (state.currentTick * 0.02f));
                                }

                                state.currentSymbols[reel][2] = state.currentSymbols[reel][1];
                                state.currentSymbols[reel][1] = state.currentSymbols[reel][0];
                                state.currentSymbols[reel][0] = symbols[r.nextInt(symbols.length)];
                            }
                        }
                    }

                    if (p.hasMetadata("slot_machine_location") && p.getOpenInventory().getTopInventory().getSize() == 54) {
                        updateGUIFromAnimationState(inv, state);
                    }

                    if (machineLoc != null) {
                        Material[] displayMats = {
                                state.currentSymbols[0][2],
                                state.currentSymbols[1][2],
                                state.currentSymbols[2][2]
                        };
                        updateDisplayItems(machineLoc, displayMats);
                    }

                    if (state.currentTick >= 60) {
                        this.cancel();
                        finishSpin(p, inv, machineLoc, state);
                    }

                } catch (Exception e) {
                    this.cancel();
                    forceCleanup(p, machineLoc);
                }
            }
        };

        if (machineLoc != null) activeAnimations.put(machineLoc, task);
        task.runTaskTimer(plugin, 0L, 2L);
    }

    private void finishSpin(Player p, Inventory inv, Location loc, AnimationState state) {
        Material[] results = state.finalResults;

        for(int i=0; i<3; i++) {
            int slot = reel1Slots[2] + i;
            setSymbolInSlot(inv, slot, results[i], true);
        }

        if (results[0] == results[1] && results[1] == results[2]) {
            giveReward(p, results[0], loc);
        } else {
            p.sendMessage(ChatColor.RED + "¡No hay suerte esta vez!");
            if (loc != null) {
                loc.getWorld().playSound(loc, Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
                loc.getWorld().playSound(loc, Sound.ENTITY_VILLAGER_NO, 0.8f, 0.8f);
            }
        }

        if (loc != null) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    cleanupDisplays(loc); // Limpiar al instante
                    animationStates.remove(loc);
                    activeAnimations.remove(loc);
                    animationStartTimes.remove(loc);
                    isSpinning.put(p.getUniqueId(), false); // Liberar giro AL FINAL

                    if (!p.hasMetadata("slot_machine_location")) {
                        forceCleanup(p, loc);
                    }
                }
            }.runTaskLater(plugin, 40L); // Reducido a 2 segundos exactos para que la transición sea fluida
        } else {
            isSpinning.put(p.getUniqueId(), false);
        }
    }

    private void giveReward(Player p, Material symbol, Location loc) {
        String cat = getCategoryFromSymbol(symbol);
        List<String> rewards = config.getStringList("SlotMachine." + cat + ".three_out_of_three");

        if (rewards.isEmpty()) {
            p.sendMessage(ChatColor.of("#FFB3BA") + "۞ No hay recompensas configuradas.");
            return;
        }

        String rewStr = rewards.get(new Random().nextInt(rewards.size()));
        String[] parts = rewStr.split(" ");
        String itemCode = parts[0];
        int amount = 1;
        try { amount = Integer.parseInt(parts[1]); } catch (Exception ignored) {}

        ItemStack item = createRewardItem(itemCode, amount);

        if (item != null) {
            p.getInventory().addItem(item).forEach((k,v) -> p.getWorld().dropItemNaturally(p.getLocation(), v));

            String displayName = (item.getItemMeta().hasDisplayName()) ? item.getItemMeta().getDisplayName() : itemCode;

            p.sendMessage(ChatColor.of("#B5EAD7") + "۞ ¡Has ganado " +
                    ChatColor.of("#FFD3A5") + displayName +
                    ChatColor.of("#B5EAD7") + " x" + amount + "!");
        }

        if (loc != null) {
            loc.getWorld().playSound(loc, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
            loc.getWorld().playSound(loc, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.5f);
            loc.getWorld().playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 0.8f, 1.2f);

            loc.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, loc.clone().add(0.5, 2, 0.5), 15, 0.5, 0.5, 0.5, 0.1);
            loc.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, loc.clone().add(0.5, 2, 0.5), 10, 0.3, 0.3, 0.3, 0.05);
        }
    }

    private String getCategoryFromSymbol(Material m) {
        return switch (m) {
            case COAL -> "itemsvarios";
            case IRON_INGOT -> "pociones";
            case GOLD_INGOT -> "vithiums_fichas";
            case DIAMOND -> "minerales";
            case EMERALD -> "totems";
            default -> "itemsvarios";
        };
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        Player p = (Player) e.getPlayer();

        if (!p.hasMetadata("slot_machine_location")) return;

        ItemStack tokens = e.getInventory().getItem(tokenSlot);
        if (tokens != null) p.getInventory().addItem(tokens);

        Location loc = (Location) p.getMetadata("slot_machine_location").get(0).value();

        p.removeMetadata("slot_machine_location", plugin);

        if (!activeAnimations.containsKey(loc)) {
            forceCleanup(p, loc);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        if (p.hasMetadata("slot_machine_location")) {
            Location loc = (Location) p.getMetadata("slot_machine_location").get(0).value();
            forceCleanup(p, loc);
        }
        isSpinning.remove(p.getUniqueId());
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent e) {
        List<Location> toRemove = new ArrayList<>();
        for (Location loc : activeDisplays.keySet()) {
            if (loc.getChunk().equals(e.getChunk())) {
                cleanupDisplays(loc);
                manager.setGameActive(loc, false);
                toRemove.add(loc);
            }
        }
        toRemove.forEach(activeDisplays::remove);
        toRemove.forEach(machineUsers::remove);
        toRemove.forEach(l -> {
            if (activeAnimations.containsKey(l)) activeAnimations.get(l).cancel();
        });
        toRemove.forEach(activeAnimations::remove);
        toRemove.forEach(animationStartTimes::remove);
    }

    private void forceCleanup(Player p, Location loc) {
        if (p != null) isSpinning.remove(p.getUniqueId());
        machineUsers.remove(loc);
        if (activeAnimations.containsKey(loc)) activeAnimations.get(loc).cancel();
        activeAnimations.remove(loc);
        animationStates.remove(loc);
        animationStartTimes.remove(loc);
        cleanupDisplays(loc);
        manager.setGameActive(loc, false);
    }

    private void createItemDisplays(Location machineLoc, Player player) {
        if (!machineLoc.getChunk().isLoaded()) machineLoc.getChunk().load();

        List<ItemDisplay> displays = new ArrayList<>();

        // Normalizamos el Yaw del jugador (0 a 360)
        double yaw = player.getLocation().getYaw();
        yaw = (yaw % 360 + 360) % 360;

        // "Redondear" a la dirección cardinal más cercana (Norte, Sur, Este, Oeste)
        double cardinalYaw;
        if (yaw >= 45 && yaw < 135) {
            cardinalYaw = 90.0;  // Oeste
        } else if (yaw >= 135 && yaw < 225) {
            cardinalYaw = 180.0; // Norte
        } else if (yaw >= 225 && yaw < 315) {
            cardinalYaw = 270.0; // Este
        } else {
            cardinalYaw = 0.0;   // Sur
        }

        // Usamos el Yaw redondeado para los cálculos
        double radYaw = Math.toRadians(cardinalYaw);

        // Vector horizontal (derecha/izquierda relativos a la cara del bloque)
        double dx = Math.cos(radYaw);
        double dz = Math.sin(radYaw);

        // Vector de profundidad (empujar "adentro" del bloque)
        double pushBack = 0.2;
        double px = -Math.sin(radYaw) * pushBack;
        double pz = Math.cos(radYaw) * pushBack;

        // Rotación exacta fijada a los ejes
        float displayYaw = (float) (Math.toRadians(-cardinalYaw + 180));
        Quaternionf rotation = new Quaternionf().rotateY(displayYaw);

        double spacing = 0.325;
        double heightY = 1.525;
        float scaleSize = 0.275f;

        for(int i = 0; i < 3; i++) {
            int multiplier = i - 1;

            Location dLoc = machineLoc.clone().add(
                    0.5 + (dx * spacing * multiplier) + px,
                    heightY,
                    0.5 + (dz * spacing * multiplier) + pz
            );

            ItemDisplay d = machineLoc.getWorld().spawn(dLoc, ItemDisplay.class, display -> {
                display.setItemStack(new ItemStack(symbols[0]));
                display.setBillboard(Display.Billboard.FIXED); // Debe mantenerse en FIXED para respetar nuestra rotación manual

                Transformation t = display.getTransformation();
                t.getScale().set(scaleSize);
                t.getLeftRotation().set(rotation);
                display.setTransformation(t);

                display.setPersistent(true);
                display.setInvulnerable(true);
                display.setGlowing(true);
                display.setBrightness(new Display.Brightness(15, 15));

                display.addScoreboardTag("slot_display");
            });
            displays.add(d);
        }
        activeDisplays.put(machineLoc, displays);
    }

    private void updateDisplayItems(Location loc, Material[] mats) {
        List<ItemDisplay> displays = activeDisplays.get(loc);
        if (displays == null || displays.size() < 3) return;

        for(int i=0; i<3; i++) {
            ItemStack item = new ItemStack(mats[i]);
            ItemMeta m = item.getItemMeta();
            m.setCustomModelData(100);
            item.setItemMeta(m);
            if (displays.get(i).isValid()) displays.get(i).setItemStack(item);
        }
    }

    private void cleanupDisplays(Location loc) {
        if (activeDisplays.containsKey(loc)) {
            activeDisplays.get(loc).forEach(Entity::remove);
            activeDisplays.remove(loc);
        }

        // LIMPIEZA ABSOLUTA MEDIANTE TAGS
        if (loc.getWorld() != null && loc.getChunk().isLoaded()) {
            Location searchLoc = loc.clone().add(0.5, 2.0, 0.5);
            for (Entity e : loc.getChunk().getEntities()) {
                if (e instanceof ItemDisplay && e.getScoreboardTags().contains("slot_display")) {
                    if (e.getLocation().distanceSquared(searchLoc) < 4.0) {
                        e.remove();
                    }
                }
            }
        }
    }

    private ItemStack createRewardItem(String name, int amount) {
        ItemStack item = null;

        switch (name.toLowerCase()) {
            case "doubletotem":
                item = doubleLifeTotem.createDoubleLifeTotem();
                break;
            case "corrupted_steak":
                item = DayOneChanges.corruptedSteak();
                break;
            case "corrupted_golden_apple":
                item = CorruptedGoldenApple.createCorruptedGoldenApple();
                break;
            case "libro_habilidades":
                item = HabilidadesBook.createHabilidadesBook();
                break;
            case "dinocoins":
                item = EconomyItems.createVithiumCoin();
                break;
            case "dinofichas":
                item = EconomyItems.createVithiumToken();
                break;
            case "mochila_nivel_1":
                item = EconomyItems.createNormalMochila();
                break;
            case "mochila_nivel_2":
                item = EconomyItems.createGreenMochila();
                break;
            case "mochila_nivel_3":
                item = EconomyItems.createRedMochila();
                break;
            case "mochila_nivel_4":
                item = EconomyItems.createBlueMochila();
                break;
            case "mochila_nivel_5":
                item = EconomyItems.createPurpleMochila();
                break;
            case "enderbag":
                item = EconomyItems.createEnderBag();
                break;
            case "gancho":
                item = EconomyItems.createGancho();
                break;
            case "panic_apple":
                item = EconomyItems.createManzanaPanico();
                break;
            case "artefacto_nivel_1":
                item = EconomyItems.createYunqueReparadorNivel1();
                break;
            case "artefacto_nivel_2":
                item = EconomyItems.createYunqueReparadorNivel2();
                break;
            case "icetotem":
                item = economyIceTotem.createIceTotem();
                break;
            case "flytotem":
                item = economyFlyTotem.createFlyTotem();
                break;
            case "special_totem":
                item = ItemsTotems.createSpecialTotem();
                break;
            case "cristal_hielo":
                item = ItemsTotems.createIceCrystal();
                break;
            case "arco_hielo":
                item = iceBowItem.createIceBow();
                break;
        }

        if (item != null) {
            item.setAmount(amount);
            return item;
        }

        try {
            Material mat = Material.valueOf(name.toUpperCase());
            return new ItemStack(mat, amount);
        } catch (IllegalArgumentException e) {
            Bukkit.getLogger().warning("[SlotMachine] Error: Item desconocido '" + name + "' en config.");
            return null;
        }
    }
}