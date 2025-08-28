package Casino;

import Armors.CopperArmor;
import Armors.CorruptedArmor;
import Armors.NightVisionHelmet;
import Blocks.CorruptedAncientDebris;
import Blocks.Endstalactitas;
import Blocks.GuardianShulkerHeart;
import Dificultades.CustomMobs.CustomBoat;
import Dificultades.CustomMobs.QueenBeeHandler;
import Dificultades.DayOneChanges;
import Enchants.EnhancedEnchantmentTable;
import Events.UltraWitherBattle.UltraWitherCompass;
import items.*;
import org.bukkit.*;
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
import org.joml.Vector3f;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SlotMachine implements Listener {
    private final JavaPlugin plugin;
    private final String title = ChatColor.of("#FF6B35") + "" + ChatColor.BOLD + "Máquina Tragamonedas";

    // Nuevos slots para GUI expandida
    private final int[] animationSlots = {12, 13, 14, 21, 22, 23}; // Slots para animación
    private final int[] resultSlots = {30, 31, 32};
    private final int[] reel1Slots = {12, 21, 30}; // Primer rodillo (columna izquierda)
    private final int[] reel2Slots = {13, 22, 31}; // Segundo rodillo (columna centro)
    private final int[] reel3Slots = {14, 23, 32}; // Tercer rodillo (columna derecha)
    private final int tokenSlot = 49; // Slot para fichas
    private final int spinButton = 43; // Botón para girar
    private final int closeButton = 0; // Botón cerrar

    private final Material[] symbols = {Material.COAL, Material.IRON_INGOT, Material.GOLD_INGOT, Material.DIAMOND, Material.EMERALD};
    private final Map<UUID, Boolean> isSpinning = new ConcurrentHashMap<>();
    private final Map<Location, UUID> machineUsers = new ConcurrentHashMap<>();
    private final Map<Location, List<ItemDisplay>> activeDisplays = new ConcurrentHashMap<>();
    private final Map<Location, BukkitRunnable> activeAnimations = new ConcurrentHashMap<>();

    // NUEVO: Mapa para mantener el estado de la animación
    private final Map<Location, AnimationState> animationStates = new ConcurrentHashMap<>();

    private final File configFile;
    private FileConfiguration config;

    // Posiciones relativas para los ItemDisplay (representando los 3 rodillos)
    private final double[] reelOffsets = {-0.7, 0.0, 0.7}; // X offset for each reel
    private final double displayHeight = 2.5; // Height above the block

    private final DoubleLifeTotem doubleLifeTotem;
    private final LifeTotem lifeTotem;
    private final SpiderTotem spiderTotem;
    private final InfernalTotem infernalTotem;
    private final EconomyIceTotem economyIceTotem;
    private final EconomyFlyTotem economyFlyTotem;
    private final BootNetheriteEssence bootNetheriteEssence;
    private final LegginsNetheriteEssence legginsNetheriteEssence;
    private final ChestplateNetheriteEssence chestplateNetheriteEssence;
    private final HelmetNetheriteEssence helmetNetheriteEssence;
    private final CorruptedUpgrades corruptedUpgrades;
    private final CorruptedSoul corruptedSoul;
    private final CorruptedAncientDebris corruptedAncientDebris;
    private final GuardianShulkerHeart guardianShulkerHeart;
    private final CustomBoat customBoat;
    private final TridenteEspectral tridenteEspectral;

    private static class AnimationState {
        Material[] finalResults = new Material[3]; // Resultados finales para cada rodillo
        Material[][] currentSymbols = new Material[3][3]; // Símbolos actuales [rodillo][posición]
        boolean[] reelStopped = new boolean[3]; // Estado de cada rodillo
        int currentTick = 0;
        int maxTicks = 60;

        public AnimationState() {
            Arrays.fill(reelStopped, false);
            // Inicializar todos los símbolos
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3; j++) {
                    currentSymbols[i][j] = Material.COAL; // Valor por defecto
                }
            }
        }

        public boolean isReelStopped(int reel) {
            return reelStopped[reel];
        }

        public void stopReel(int reel) {
            reelStopped[reel] = true;
            currentSymbols[reel][2] = finalResults[reel];
        }
    }

    public SlotMachine(JavaPlugin plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "SlotMachine.yml");

        this.doubleLifeTotem = new DoubleLifeTotem(plugin);
        this.lifeTotem = new LifeTotem(plugin);
        this.spiderTotem = new SpiderTotem(plugin);
        this.infernalTotem = new InfernalTotem(plugin);
        this.economyIceTotem = new EconomyIceTotem(plugin);
        this.economyFlyTotem = new EconomyFlyTotem(plugin);
        this.bootNetheriteEssence = new BootNetheriteEssence(plugin);
        this.legginsNetheriteEssence = new LegginsNetheriteEssence(plugin);
        this.chestplateNetheriteEssence = new ChestplateNetheriteEssence(plugin);
        this.helmetNetheriteEssence = new HelmetNetheriteEssence(plugin);
        this.corruptedUpgrades = new CorruptedUpgrades(plugin);
        this.corruptedSoul = new CorruptedSoul(plugin);
        this.corruptedAncientDebris = new CorruptedAncientDebris(plugin);
        this.guardianShulkerHeart = new GuardianShulkerHeart(plugin);
        this.customBoat = new CustomBoat(plugin);
        this.tridenteEspectral = new TridenteEspectral(plugin);

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
            defaultConfig.set("SlotMachine.minerales.two_out_of_three", Arrays.asList("coal 12", "iron_ingot 10", "gold_ingot 8"));
            defaultConfig.set("SlotMachine.minerales.three_out_of_three", Arrays.asList("diamond 5", "netherite_scrap 3", "pepita_infernal 3", "corrupted_netherite_scrap 2"));

            defaultConfig.set("SlotMachine.itemsvarios.two_out_of_three", Arrays.asList("bread 16", "cooked_beef 8"));
            defaultConfig.set("SlotMachine.itemsvarios.three_out_of_three", Arrays.asList("totem_of_undying 1", "enchanted_golden_apple 2"));

            defaultConfig.set("SlotMachine.pociones.two_out_of_three", Arrays.asList("potion 3", "splash_potion 2"));
            defaultConfig.set("SlotMachine.pociones.three_out_of_three", Arrays.asList("ultra_pocion_resistencia_fuego 1"));

            defaultConfig.set("SlotMachine.vithiums_fichas.two_out_of_three", Arrays.asList("vithiums_fichas 5", "vithiums_fichas 8"));
            defaultConfig.set("SlotMachine.vithiums_fichas.three_out_of_three", Arrays.asList("vithiums_fichas 15", "vithiums_fichas 20"));

            defaultConfig.set("SlotMachine.totems.two_out_of_three", Arrays.asList("totem_of_undying 1"));
            defaultConfig.set("SlotMachine.totems.three_out_of_three", Arrays.asList("doubletotem 1", "lifetotem 1"));

            // Configuración de probabilidades
            defaultConfig.set("SlotMachine.probabilities.two_out_of_three", 0.15);
            defaultConfig.set("SlotMachine.probabilities.three_out_of_three", 0.05);

            defaultConfig.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Error creando configuración de Slot Machine: " + e.getMessage());
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getClickedBlock() == null || event.getClickedBlock().getType() != Material.ORANGE_GLAZED_TERRACOTTA) return;

        event.setCancelled(true);
        Player player = event.getPlayer();
        Location blockLoc = event.getClickedBlock().getLocation();

        // Verificar si hay una animación en curso
        BukkitRunnable currentAnimation = activeAnimations.get(blockLoc);
        if (currentAnimation != null) {
            UUID currentUser = machineUsers.get(blockLoc);
            if (currentUser != null && currentUser.equals(player.getUniqueId())) {
                openSlotMachine(player, blockLoc);
            } else {
                player.sendMessage(ChatColor.of("#FF6B6B") + "۞ La máquina está en uso y hay una animación en curso.");
            }
            return;
        }

        // Verificar si la máquina ya está siendo usada
        UUID currentUser = machineUsers.get(blockLoc);
        if (currentUser != null) {
            Player currentPlayer = Bukkit.getPlayer(currentUser);
            if (currentPlayer != null && currentPlayer.isOnline()) {
                player.sendMessage(ChatColor.of("#FF6B6B") + "۞ Esta máquina está siendo usada por " + currentPlayer.getName());
                return;
            } else {
                machineUsers.remove(blockLoc);
                cleanupDisplays(blockLoc);
            }
        }

        // Verificar fichas
        if (!hasVithiumTokens(player)) {
            player.sendMessage(ChatColor.of("#FFB3BA") + "۞ Necesitas Vithium Fichas para usar la máquina tragamonedas.");
            return;
        }

        // Registrar usuario y abrir máquina
        machineUsers.put(blockLoc, player.getUniqueId());
        openSlotMachine(player, blockLoc);
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

    private void openSlotMachine(Player player, Location machineLoc) {
        Inventory inv = Bukkit.createInventory(null, 54, title);
        setupGUI(inv, player, machineLoc);
        player.openInventory(inv);

        // Guardar la ubicación de la máquina en metadata del jugador
        player.setMetadata("slot_machine_location", new org.bukkit.metadata.FixedMetadataValue(plugin, machineLoc));

        // Sonido de apertura
        machineLoc.getWorld().playSound(machineLoc, Sound.BLOCK_CHEST_OPEN, 1.0f, 1.2f);
    }

    private void setupGUI(Inventory inv, Player player, Location machineLoc) {
        // Botón de cerrar (slot 0)
        ItemStack closeItem = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = closeItem.getItemMeta();
        closeMeta.setDisplayName(ChatColor.of("#FF6B6B") + "" + ChatColor.BOLD + "Cerrar");
        closeMeta.setCustomModelData(1000);
        closeItem.setItemMeta(closeMeta);
        inv.setItem(closeButton, closeItem);

        // Slot para fichas (slot 49) - SIN ITEM POR DEFECTO
        // Dejamos el slot vacío para evitar duplicaciones

        // Botón de girar (slot 43)
        ItemStack spinItem = new ItemStack(Material.LEVER);
        ItemMeta spinMeta = spinItem.getItemMeta();
        spinMeta.setDisplayName(ChatColor.of("#B5EAD7") + "" + ChatColor.BOLD + "¡GIRAR!");
        spinMeta.setLore(Arrays.asList(
                "",
                ChatColor.of("#C7CEEA") + "Coloca una " + ChatColor.of("#FFD3A5") + "Vithium Ficha",
                ChatColor.of("#C7CEEA") + "en el slot inferior y haz clic aquí",
                ""
        ));
        spinMeta.setCustomModelData(1000);
        spinItem.setItemMeta(spinMeta);
        inv.setItem(spinButton, spinItem);

        // ARREGLADO: Verificar si hay animación en curso para este jugador y máquina
        AnimationState animState = animationStates.get(machineLoc);
        if (animState != null && activeAnimations.containsKey(machineLoc) && isSpinning.getOrDefault(player.getUniqueId(), false)) {
            updateGUIFromAnimationState(inv, animState);
        } else {
            // Inicializar cada posición con símbolos aleatorios diferentes
            Random rand = new Random();
            for (int reel = 0; reel < 3; reel++) {
                int[] reelSlots = reel == 0 ? reel1Slots : (reel == 1 ? reel2Slots : reel3Slots);

                for (int pos = 0; pos < 3; pos++) {
                    Material symbol;
                    do {
                        symbol = symbols[rand.nextInt(symbols.length)];
                        // Asegurarse de que no se repita el símbolo en la misma columna
                    } while (pos > 0 && symbol == inv.getItem(reelSlots[pos-1]).getType());

                    setSymbolInSlot(inv, reelSlots[pos], symbol, pos == 2); // El último slot es el resultado
                }
            }
        }

        // Llenar espacios vacíos con paneles grises
        for (int i = 0; i < inv.getSize(); i++) {
            if (i != closeButton && i != tokenSlot && i != spinButton &&
                    !isAnimationSlot(i) && !isResultSlot(i) && inv.getItem(i) == null) {

                ItemStack frame = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
                ItemMeta frameMeta = frame.getItemMeta();
                frameMeta.setDisplayName(" ");
                frameMeta.setCustomModelData(1000);
                frame.setItemMeta(frameMeta);
                inv.setItem(i, frame);
            }
        }

        // Agregar indicador visual GRIS para el slot de fichas
        ItemStack tokenIndicator = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta tokenMeta = tokenIndicator.getItemMeta();
        tokenMeta.setDisplayName(ChatColor.of("#FFD3A5") + "" + ChatColor.BOLD + "Coloca Ficha Aquí");
        tokenMeta.setLore(Arrays.asList(
                "",
                ChatColor.of("#C7CEEA") + "Arrastra una " + ChatColor.of("#FFD3A5") + "Vithium Ficha",
                ChatColor.of("#C7CEEA") + "a este slot para apostar",
                ""
        ));
        tokenMeta.setCustomModelData(1000);
        tokenIndicator.setItemMeta(tokenMeta);

        // Colocar indicadores alrededor del slot de ficha
        int[] indicatorSlots = {40, 41, 42, 48, 50, 57, 58};
        for (int slot : indicatorSlots) {
            if (slot < 54) {
                inv.setItem(slot, tokenIndicator);
            }
        }
    }

    // ARREGLADO: Nuevo método que usa el estado de animación correctamente
    private void updateGUIFromAnimationState(Inventory inv, AnimationState animState) {
        for (int reel = 0; reel < 3; reel++) {
            boolean isStopped = animState.isReelStopped(reel);
            int[] reelSlots = reel == 0 ? reel1Slots : (reel == 1 ? reel2Slots : reel3Slots);

            for (int pos = 0; pos < 3; pos++) {
                Material symbol = animState.currentSymbols[reel][pos];
                boolean isFinal = isStopped && pos == 2; // Solo el último slot en dorado

                setSymbolInSlot(inv, reelSlots[pos], symbol, isFinal);
            }
        }
    }

    private boolean isAnimationSlot(int slot) {
        return slot == 12 || slot == 21 || slot == 30 ||  // Rodillo 1
                slot == 13 || slot == 22 || slot == 31 ||  // Rodillo 2
                slot == 14 || slot == 23 || slot == 32;    // Rodillo 3
    }

    private boolean isResultSlot(int slot) {
        for (int resultSlot : resultSlots) {
            if (resultSlot == slot) return true;
        }
        return false;
    }

    private void setRandomSymbol(Inventory inv, int slot, boolean isGray) {
        Material symbol = symbols[new Random().nextInt(symbols.length)];
        ItemStack item = new ItemStack(symbol);
        ItemMeta meta = item.getItemMeta();

        String colorCode = isGray ? ChatColor.GRAY + "" : ChatColor.of("#FFD3A5") + "" + ChatColor.BOLD;
        meta.setDisplayName(colorCode + getSymbolName(symbol));
        meta.setCustomModelData(100); // Custom model data correcto
        item.setItemMeta(meta);
        inv.setItem(slot, item);
    }

    private String getSymbolName(Material symbol) {
        return switch (symbol) {
            case COAL -> "Carbón";
            case IRON_INGOT -> "Hierro";
            case GOLD_INGOT -> "Oro";
            case DIAMOND -> "Diamante";
            case EMERALD -> "Esmeralda";
            default -> symbol.name();
        };
    }

    private String getCategoryFromSymbol(Material symbol) {
        return switch (symbol) {
            case COAL -> "itemsvarios";
            case IRON_INGOT -> "pociones";
            case GOLD_INGOT -> "vithiums_fichas";
            case DIAMOND -> "minerales";
            case EMERALD -> "totems";
            default -> "itemsvarios";
        };
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(title)) return;

        Player player = (Player) event.getWhoClicked();

        // Permitir interacción completa con el inventario del jugador
        if (event.getClickedInventory() == player.getInventory()) {
            return; // No cancelar - permitir interacción normal
        }

        // Solo manejar clicks en el inventario de la slot machine
        if (event.getClickedInventory() != event.getView().getTopInventory()) {
            return;
        }

        // Si está girando, cancelar todas las interacciones excepto cerrar
        if (isSpinning.getOrDefault(player.getUniqueId(), false)) {
            if (event.getSlot() == closeButton) {
                event.setCancelled(false);
                return;
            }
            event.setCancelled(true);
            return;
        }

        // Manejar slot de fichas (49)
        if (event.getSlot() == tokenSlot) {
            handleTokenSlot(event);
            return;
        }

        // Para todos los otros slots, cancelar por defecto
        event.setCancelled(true);

        if (event.getCurrentItem() == null) return;

        // Manejar botones específicos
        switch (event.getSlot()) {
            case 0 -> player.closeInventory(); // Botón cerrar
            case 43 -> { // Botón girar
                ItemStack wager = event.getInventory().getItem(tokenSlot);
                if (wager != null && wager.isSimilar(EconomyItems.createVithiumToken())) {
                    startSpinAnimation(player, event.getInventory());
                } else {
                    player.sendMessage(ChatColor.of("#FFB3BA") + "۞ ¡Coloca una Vithium Ficha primero!");
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.8f);
                }
            }
        }
    }

    private void handleTokenSlot(InventoryClickEvent event) {
        ItemStack cursor = event.getCursor();
        ItemStack current = event.getCurrentItem();
        ItemStack vithiumToken = EconomyItems.createVithiumToken();

        // Si está colocando una ficha
        if (cursor != null && cursor.isSimilar(vithiumToken)) {
            event.setCancelled(false);
            return;
        }

        // Si está sacando una ficha
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

        // Devolver CUALQUIER item que esté en el slot de fichas
        ItemStack itemInTokenSlot = event.getInventory().getItem(tokenSlot);
        if (itemInTokenSlot != null) {
            HashMap<Integer, ItemStack> remaining = player.getInventory().addItem(itemInTokenSlot);
            if (!remaining.isEmpty()) {
                for (ItemStack item : remaining.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), item);
                }
            }
        }

        // Si no hay animación activa, liberar la máquina y limpiar displays
        if (player.hasMetadata("slot_machine_location")) {
            Location machineLoc = (Location) player.getMetadata("slot_machine_location").get(0).value();

            // Solo liberar si no hay animación en curso
            if (!activeAnimations.containsKey(machineLoc)) {
                machineUsers.remove(machineLoc);
                cleanupDisplays(machineLoc);
            }

            player.removeMetadata("slot_machine_location", plugin);
        }

        // NO detener animación si está activa - debe continuar aunque se cierre la GUI
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        // Limpiar displays cuando se descargan chunks
        List<Location> toRemove = new ArrayList<>();
        for (Location loc : activeDisplays.keySet()) {
            if (loc.getChunk().equals(event.getChunk())) {
                cleanupDisplays(loc);
                toRemove.add(loc);
            }
        }
        toRemove.forEach(activeDisplays::remove);
        toRemove.forEach(machineUsers::remove);
        toRemove.forEach(activeAnimations::remove);
        toRemove.forEach(animationStates::remove); // NUEVO: limpiar estados de animación
    }

    private void createItemDisplays(Location machineLoc) {
        if (activeDisplays.containsKey(machineLoc)) {
            cleanupDisplays(machineLoc);
        }

        List<ItemDisplay> displays = new ArrayList<>();
        World world = machineLoc.getWorld();

        // Asegurarse de que el chunk esté cargado
        if (!machineLoc.getChunk().isLoaded()) {
            machineLoc.getChunk().load();
        }

        // Posiciones centradas para cada rodillo
        double[] reelPositions = {-0.5, 0.0, 0.5}; // Centrado perfecto en X
        double displayHeight = 2.0; // Altura ajustada

        for (int i = 0; i < 3; i++) {
            Location displayLoc = machineLoc.clone()
                    .add(reelPositions[i], displayHeight, 0.5) // Centrado en X y Z
                    .add(0.5, 0, 0.5); // Ajuste para centrar en el bloque

            ItemDisplay display = world.spawn(displayLoc, ItemDisplay.class, itemDisplay -> {
                // Crear item con custom model data
                ItemStack displayItem = new ItemStack(symbols[0]);
                ItemMeta meta = displayItem.getItemMeta();
                meta.setCustomModelData(100);
                displayItem.setItemMeta(meta);

                itemDisplay.setItemStack(displayItem);
                itemDisplay.setBillboard(Display.Billboard.CENTER);

                // Configuración de transformación
                Transformation transformation = itemDisplay.getTransformation();
                transformation.getScale().set(0.4f); // Tamaño ajustado
                transformation.getTranslation().set(0, 0, 0); // Posición exacta
                itemDisplay.setTransformation(transformation);

                // Configuración importante para evitar desapariciones
                itemDisplay.setPersistent(true); // Hacerlo persistente
                itemDisplay.setInvulnerable(true); // Invulnerable a daños
                itemDisplay.setGlowing(true); // Efecto de brillo
                itemDisplay.setBrightness(new Display.Brightness(15, 15)); // Máxima iluminación
                itemDisplay.setViewRange(1.5f); // Rango de visualización más amplio
            });

            // Forzar que el display se mantenga
            display.setPersistent(true);
            displays.add(display);
        }

        activeDisplays.put(machineLoc, displays);

        // Forzar guardado del chunk
        world.getChunkAt(machineLoc).setForceLoaded(true);
    }

    private void updateDisplayItems(Location machineLoc, Material[] symbols) {
        List<ItemDisplay> displays = activeDisplays.get(machineLoc);
        if (displays == null || displays.size() != 3) {
            // Si los displays desaparecieron, recrearlos
            if (machineLoc != null && machineLoc.getWorld() != null) {
                createItemDisplays(machineLoc);
                displays = activeDisplays.get(machineLoc);
                if (displays == null) return;
            } else {
                return;
            }
        }

        for (int i = 0; i < 3 && i < displays.size(); i++) {
            ItemDisplay display = displays.get(i);
            if (display == null || !display.isValid()) {
                // Recrear display si es necesario
                createItemDisplays(machineLoc);
                return;
            }

            try {
                ItemStack displayItem = new ItemStack(symbols[i]);
                ItemMeta meta = displayItem.getItemMeta();
                meta.setCustomModelData(100);
                displayItem.setItemMeta(meta);

                // Actualizar el display
                display.setItemStack(displayItem);

                // Reforzar persistencia
                display.setPersistent(true);
            } catch (Exception e) {
                plugin.getLogger().warning("Error al actualizar ItemDisplay: " + e.getMessage());
                // Recrear displays si hay error
                createItemDisplays(machineLoc);
                return;
            }
        }
    }

    private void cleanupDisplays(Location machineLoc) {
        List<ItemDisplay> displays = activeDisplays.get(machineLoc);
        if (displays != null) {
            // Asegurarse de que el chunk esté cargado
            if (machineLoc != null && machineLoc.getWorld() != null && !machineLoc.getChunk().isLoaded()) {
                machineLoc.getChunk().load();
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
            activeDisplays.remove(machineLoc);

            // Liberar el chunk si es necesario
            if (machineLoc != null && machineLoc.getWorld() != null) {
                machineLoc.getChunk().setForceLoaded(false);
            }
        }
    }

    // COMPLETAMENTE ARREGLADO: Método de animación corregido
    private void startSpinAnimation(Player player, Inventory inv) {
        if (isSpinning.getOrDefault(player.getUniqueId(), false)) return;

        isSpinning.put(player.getUniqueId(), true);

        // Consumir ficha
        ItemStack wager = inv.getItem(tokenSlot);
        if (wager != null) {
            wager.setAmount(wager.getAmount() - 1);
            if (wager.getAmount() <= 0) {
                inv.setItem(tokenSlot, null);
            }
        }

        Location machineLoc = null;
        if (player.hasMetadata("slot_machine_location")) {
            machineLoc = (Location) player.getMetadata("slot_machine_location").get(0).value();
        }

        final Location finalMachineLoc = machineLoc;

        // Crear estado de animación
        AnimationState animState = new AnimationState();
        Random random = new Random();

        // Generar resultados finales y símbolos iniciales
        for (int reel = 0; reel < 3; reel++) {
            animState.finalResults[reel] = symbols[random.nextInt(symbols.length)];

            // Inicializar cada posición del rodillo con símbolos diferentes
            for (int pos = 0; pos < 3; pos++) {
                animState.currentSymbols[reel][pos] = symbols[(reel + pos) % symbols.length];
            }
        }

        if (finalMachineLoc != null) {
            animationStates.put(finalMachineLoc, animState);
        }

        // Crear displays 3D
        if (finalMachineLoc != null) {
            createItemDisplays(finalMachineLoc);
            // Actualizar inmediatamente con los símbolos iniciales
            updateDisplayItems(finalMachineLoc, new Material[]{
                    animState.currentSymbols[0][2], // Símbolo inferior del primer rodillo
                    animState.currentSymbols[1][2], // Símbolo inferior del segundo rodillo
                    animState.currentSymbols[2][2]  // Símbolo inferior del tercer rodillo
            });

            finalMachineLoc.getWorld().playSound(finalMachineLoc, Sound.BLOCK_PISTON_EXTEND, 1.0f, 1.0f);
            finalMachineLoc.getWorld().playSound(finalMachineLoc, Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.2f);
        }

        BukkitRunnable animationTask = new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    // Verificar y mantener los displays
                    if (finalMachineLoc != null) {
                        List<ItemDisplay> displays = activeDisplays.get(finalMachineLoc);
                        if (displays == null || displays.size() != 3 || displays.stream().anyMatch(d -> d == null || !d.isValid())) {
                            createItemDisplays(finalMachineLoc);
                        }

                        // Asegurar que el chunk permanezca cargado
                        finalMachineLoc.getChunk().setForceLoaded(true);
                    }

                    animState.currentTick++;

                    // Tiempos de detención para cada rodillo (animación en cascada)
                    int[] stopTimes = {
                            animState.maxTicks - 20, // Primer rodillo
                            animState.maxTicks - 10, // Segundo rodillo
                            animState.maxTicks       // Tercer rodillo
                    };

                    for (int reel = 0; reel < 3; reel++) {
                        if (!animState.isReelStopped(reel)) {
                            if (animState.currentTick >= stopTimes[reel]) {
                                // Detener este rodillo
                                animState.stopReel(reel);
                                if (finalMachineLoc != null) {
                                    finalMachineLoc.getWorld().playSound(finalMachineLoc, Sound.BLOCK_ANVIL_LAND, 0.5f, 1.5f);

                                    // Actualizar el display 3D con el símbolo final cuando se detiene
                                    Material[] currentDisplaySymbols = new Material[3];
                                    for (int i = 0; i < 3; i++) {
                                        currentDisplaySymbols[i] = animState.isReelStopped(i) ?
                                                animState.finalResults[i] :
                                                animState.currentSymbols[i][2];
                                    }
                                    updateDisplayItems(finalMachineLoc, currentDisplaySymbols);
                                }
                            } else {
                                // Animación del rodillo
                                if (animState.currentTick > stopTimes[reel] - 15) {
                                    // Últimos 15 ticks - mostrar más el símbolo final
                                    for (int pos = 0; pos < 3; pos++) {
                                        if (random.nextDouble() < 0.3) {
                                            animState.currentSymbols[reel][pos] = animState.finalResults[reel];
                                        } else {
                                            animState.currentSymbols[reel][pos] = getNextRandomSymbol(
                                                    animState.currentSymbols[reel][pos],
                                                    animState.finalResults[reel]
                                            );
                                        }
                                    }
                                } else {
                                    // Animación normal - cada posición cambia independientemente
                                    for (int pos = 0; pos < 3; pos++) {
                                        animState.currentSymbols[reel][pos] = getNextRandomSymbol(
                                                animState.currentSymbols[reel][pos],
                                                null
                                        );
                                    }
                                }
                            }
                        }

                        // Actualizar GUI
                        if (player.getOpenInventory().getTitle().equals(title)) {
                            updateGUIForReel(inv, reel, animState);
                        }
                    }

                    // Actualizar displays 3D con los símbolos inferiores de cada rodillo
                    if (finalMachineLoc != null) {
                        Material[] displaySymbols = new Material[3];
                        for (int i = 0; i < 3; i++) {
                            displaySymbols[i] = animState.currentSymbols[i][2]; // Símbolo inferior
                        }
                        updateDisplayItems(finalMachineLoc, displaySymbols);
                    }

                    // Sonido de animación
                    if (finalMachineLoc != null && animState.currentTick % 4 == 0) {
                        finalMachineLoc.getWorld().playSound(finalMachineLoc, Sound.BLOCK_NOTE_BLOCK_HAT, 0.3f, 1.0f + (animState.currentTick * 0.02f));
                    }

                    // Finalizar animación
                    if (animState.currentTick >= animState.maxTicks) {
                        finishAnimation(player, inv, finalMachineLoc, animState);
                        this.cancel();
                    }

                } catch (Exception e) {
                    plugin.getLogger().severe("Error en la animación de SlotMachine: " + e.getMessage());
                    // Limpiar y cancelar si hay error
                    if (finalMachineLoc != null) {
                        cleanupDisplays(finalMachineLoc);
                        machineUsers.remove(finalMachineLoc);
                        activeAnimations.remove(finalMachineLoc);
                        animationStates.remove(finalMachineLoc);
                    }
                    isSpinning.put(player.getUniqueId(), false);
                    this.cancel();
                }
            }

            private Material getNextRandomSymbol(Material current, Material preferred) {
                Material next;
                int attempts = 0;

                do {
                    // 30% de probabilidad de mostrar el símbolo preferido (si existe)
                    if (preferred != null && random.nextDouble() < 0.3) {
                        next = preferred;
                    } else {
                        // Seleccionar aleatoriamente, evitando repeticiones
                        List<Material> possible = new ArrayList<>(Arrays.asList(symbols));
                        possible.remove(current); // Evitar repetición

                        if (possible.isEmpty()) {
                            next = symbols[random.nextInt(symbols.length)];
                        } else {
                            next = possible.get(random.nextInt(possible.size()));
                        }
                    }
                    attempts++;
                } while (next == current && attempts < 10); // Prevenir bucles infinitos

                return next;
            }
        };

        if (finalMachineLoc != null) {
            activeAnimations.put(finalMachineLoc, animationTask);
        }

        animationTask.runTaskTimer(plugin, 0L, 2L);
    }

    private void updateGUIForReel(Inventory inv, int reel, AnimationState animState) {
        // Determinar qué slots actualizar según el rodillo
        int[] slotsToUpdate;
        switch (reel) {
            case 0: slotsToUpdate = reel1Slots; break; // 12, 21, 30
            case 1: slotsToUpdate = reel2Slots; break; // 13, 22, 31
            case 2: slotsToUpdate = reel3Slots; break; // 14, 23, 32
            default: return;
        }

        // Actualizar cada posición del rodillo
        for (int pos = 0; pos < 3; pos++) {
            Material symbol = animState.currentSymbols[reel][pos];
            boolean isFinal = animState.isReelStopped(reel) && pos == 2; // Solo el último slot en dorado

            setSymbolInSlot(inv, slotsToUpdate[pos], symbol, isFinal);
        }
    }

    private int[] getAnimationSlotsForReel(int reel) {
        return switch (reel) {
            case 0 -> new int[]{animationSlots[0], animationSlots[1]}; // slots 12 y 13
            case 1 -> new int[]{animationSlots[2], animationSlots[3]}; // slots 21 y 22
            case 2 -> new int[]{animationSlots[4], animationSlots[5]}; // slots 14 y 23
            default -> new int[0];
        };
    }

    private void setSymbolInSlot(Inventory inv, int slot, Material symbol, boolean isFinal) {
        ItemStack item = new ItemStack(symbol);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(isFinal ?
                ChatColor.of("#FFD3A5") + "" + ChatColor.BOLD + getSymbolName(symbol) : // Dorado si es final
                ChatColor.GRAY + getSymbolName(symbol)); // Gris si está girando
        meta.setCustomModelData(100);
        item.setItemMeta(meta);
        inv.setItem(slot, item);
    }

    private void finishAnimation(Player player, Inventory inv, Location machineLoc, AnimationState animState) {
        // Mostrar resultados finales en dorado
        for (int i = 0; i < 3; i++) {
            setSymbolInSlot(inv, resultSlots[i], animState.finalResults[i], true);
        }

        // Actualizar displays 3D con los resultados finales
        if (machineLoc != null) {
            updateDisplayItems(machineLoc, animState.finalResults);
        }

        checkWin(player, inv, machineLoc, animState.finalResults);
        isSpinning.put(player.getUniqueId(), false);

        // Limpiar después de mostrar resultado
        if (machineLoc != null) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    cleanupDisplays(machineLoc);
                    machineUsers.remove(machineLoc);
                    activeAnimations.remove(machineLoc);
                    animationStates.remove(machineLoc);
                }
            }.runTaskLater(plugin, 100L); // 5 segundos
        }
    }

/*    // NUEVO: Método helper para actualizar slots de animación
    private void updateAnimationSlot(Inventory inv, int reel, Material symbol, boolean isAnimationSlot) {
        ItemStack item = new ItemStack(symbol);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GRAY + getSymbolName(symbol));
        meta.setCustomModelData(100);
        item.setItemMeta(meta);

        if (isAnimationSlot) {
            // Actualizar slots de animación correspondientes
            switch (reel) {
                case 0:
                    inv.setItem(animationSlots[0], item); // slot 12
                    inv.setItem(animationSlots[1], item); // slot 13
                    break;
                case 1:
                    inv.setItem(animationSlots[2], item); // slot 21
                    inv.setItem(animationSlots[3], item); // slot 22
                    break;
                case 2:
                    inv.setItem(animationSlots[4], item); // slot 14
                    inv.setItem(animationSlots[5], item); // slot 23
                    break;
            }
        } else {
            // Actualizar slot de resultado
            if (reel < resultSlots.length) {
                inv.setItem(resultSlots[reel], item);
            }
        }
    }*/

    private void checkWin(Player player, Inventory inv, Location machineLoc, Material[] results) {
        // Contar símbolos iguales
        Map<Material, Integer> symbolCount = new HashMap<>();
        for (Material symbol : results) {
            if (symbol != null) {
                symbolCount.put(symbol, symbolCount.getOrDefault(symbol, 0) + 1);
            }
        }

        // Determinar tipo de victoria
        Material winningSymbol = null;
        boolean threeOfAKind = false;
        boolean twoOfAKind = false;

        for (Map.Entry<Material, Integer> entry : symbolCount.entrySet()) {
            if (entry.getValue() == 3) {
                winningSymbol = entry.getKey();
                threeOfAKind = true;
                break;
            } else if (entry.getValue() == 2) {
                winningSymbol = entry.getKey();
                twoOfAKind = true;
            }
        }

        if (threeOfAKind || twoOfAKind) {
            giveReward(player, winningSymbol, threeOfAKind, machineLoc);
        } else {
            // Sonido de pérdida
            if (machineLoc != null) {
                machineLoc.getWorld().playSound(machineLoc, Sound.BLOCK_NOTE_BLOCK_BASS, SoundCategory.VOICE, 1.0f, 0.5f);
                machineLoc.getWorld().playSound(machineLoc, Sound.ENTITY_VILLAGER_NO, SoundCategory.VOICE, 0.8f, 0.8f);
            }
            player.sendMessage(ChatColor.RED + "¡No hay suerte esta vez!");
        }
    }

    private void giveReward(Player player, Material symbol, boolean threeOfAKind, Location machineLoc) {
        String category = getCategoryFromSymbol(symbol);
        String rewardType = threeOfAKind ? "three_out_of_three" : "two_out_of_three";

        List<String> rewards = config.getStringList("SlotMachine." + category + "." + rewardType);

        if (rewards.isEmpty()) {
            player.sendMessage(ChatColor.of("#FFB3BA") + "۞ No hay recompensas configuradas para esta categoría.");
            return;
        }

        // Seleccionar recompensa aleatoria
        String selectedReward = rewards.get(new Random().nextInt(rewards.size()));
        String[] parts = selectedReward.split(" ");

        if (parts.length != 2) {
            player.sendMessage(ChatColor.of("#FFB3BA") + "۞ Error en la configuración de recompensas.");
            return;
        }

        String itemName = parts[0];
        int amount;

        try {
            amount = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.of("#FFB3BA") + "۞ Error en la cantidad de la recompensa.");
            return;
        }

        // Crear item
        ItemStack rewardItem = createItemFromName(itemName, amount);
        if (rewardItem == null) {
            player.sendMessage(ChatColor.of("#FFB3BA") + "۞ Item no reconocido: " + itemName);
            return;
        }

        // Dar recompensa
        HashMap<Integer, ItemStack> remaining = player.getInventory().addItem(rewardItem);
        if (!remaining.isEmpty()) {
            for (ItemStack item : remaining.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), item);
            }
        }

        // Mensajes y sonidos
        String multiplier = threeOfAKind ? "x3" : "x2";
        String itemDisplayName = rewardItem.getItemMeta().getDisplayName();
        if (itemDisplayName == null || itemDisplayName.isEmpty()) {
            itemDisplayName = getSymbolName(rewardItem.getType());
        }

        player.sendMessage(ChatColor.of("#B5EAD7") + "۞ ¡Has ganado " +
                ChatColor.of("#FFD3A5") + itemDisplayName +
                ChatColor.of("#B5EAD7") + " x" + amount + "!");

        // Sonidos de victoria para todos los jugadores cercanos
        if (machineLoc != null) {
            if (threeOfAKind) {
                machineLoc.getWorld().playSound(machineLoc, Sound.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.VOICE, 1.0f, 1.0f);
                machineLoc.getWorld().playSound(machineLoc, Sound.ENTITY_PLAYER_LEVELUP, SoundCategory.VOICE, 1.0f, 1.5f);
                machineLoc.getWorld().playSound(machineLoc, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, SoundCategory.VOICE, 0.8f, 1.2f);
            } else {
                machineLoc.getWorld().playSound(machineLoc, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.VOICE, 1.0f, 1.5f);
                machineLoc.getWorld().playSound(machineLoc, Sound.BLOCK_NOTE_BLOCK_CHIME, SoundCategory.VOICE, 1.0f, 1.2f);
            }

            // Efectos de partículas para victoria
            World world = machineLoc.getWorld();
            Location effectLoc = machineLoc.clone().add(0.5, 2, 0.5);

            if (threeOfAKind) {
                world.spawnParticle(Particle.TOTEM_OF_UNDYING, effectLoc, 15, 0.5, 0.5, 0.5, 0.1);
                world.spawnParticle(Particle.ELECTRIC_SPARK, effectLoc, 10, 0.3, 0.3, 0.3, 0.05);
            } else {
                world.spawnParticle(Particle.TRIAL_SPAWNER_DETECTION_OMINOUS, effectLoc, 8, 0.3, 0.3, 0.3, 0.05);
            }
        }
    }

    // [El resto de métodos createItemFromName y createCustomItem permanecen igual]
    private ItemStack createItemFromName(String itemName, int amount) {
        // Primero intentar items vanilla
        try {
            Material material = Material.valueOf(itemName.toUpperCase());
            return new ItemStack(material, amount);
        } catch (IllegalArgumentException e) {
            // No es item vanilla, intentar items custom
            return createCustomItem(itemName, amount);
        }
    }

    private ItemStack createCustomItem(String itemName, int amount) {
        ItemStack item = null;
        int cantidad = 1;
        Player target = null;

        switch (itemName.toLowerCase()) {
            case "doubletotem":
                item = doubleLifeTotem.createDoubleLifeTotem();
                break;
            case "lifetotem":
                item = lifeTotem.createLifeTotem();
                break;
            case "spidertotem":
                item = spiderTotem.createSpiderTotem();
                break;
            case "infernaltotem":
                item = infernalTotem.createInfernalTotem();
                break;
            case "aguijon_abeja_reina":
                item = QueenBeeHandler.createAguijonAbejaReina();
                break;
            case "upgrade_vacio":
                item = UpgradeNTItems.createUpgradeVacio();
                break;
            case "fragmento_upgrade":
                item = UpgradeNTItems.createFragmentoUpgrade();
                break;
            case "duplicador":
                item = UpgradeNTItems.createDuplicador();
                break;
            case "fragmento_infernal":
                item = EmblemItems.createFragmentoInfernal();
                break;
            case "pepita_infernal":
                item = EmblemItems.createPepitaInfernal();
                break;
            case "corrupted_nether_star":
                item = EmblemItems.createcorruptedNetherStar();
                break;
            case "nether_emblem":
                item = EmblemItems.createNetherEmblem();
                break;
            case "overworld_emblem":
                item = EmblemItems.createOverworldEmblem();
                break;
            case "end_relic":
                item = EmblemItems.createEndEmblem();
                break;
            case "corrupted_steak":
                item = DayOneChanges.corruptedSteak();
                break;
            case "placa_diamante":
                item = EnhancedEnchantmentTable.createDiamondPlate();
                break;
            case "mesa_encantamientos_mejorada":
                item = EnhancedEnchantmentTable.createEnhancedEnchantmentTable();
                break;
            case "casco_night_vision":
                item = NightVisionHelmet.createNightVisionHelmet();
                break;
            case "corrupted_helmet_armor":
                item = CorruptedArmor.createCorruptedHelmet();
                break;
            case "corrupted_chestplate_armor":
                item = CorruptedArmor.createCorruptedChestplate();
                break;
            case "corrupted_leggings_armor":
                item = CorruptedArmor.createCorruptedLeggings();
                break;
            case "corrupted_boots_armor":
                item = CorruptedArmor.createCorruptedBoots();
                break;
            case "enderite_sword":
                item = EnderiteTools.createEnderiteSword();
                break;
            case "enderite_axe":
                item = EnderiteTools.createEnderiteAxe();
                break;
            case "enderite_pickaxe":
                item = EnderiteTools.createEnderitePickaxe();
                break;
            case "enderite_shovel":
                item = EnderiteTools.createEnderiteShovel();
                break;
            case "enderite_hoe":
                item = EnderiteTools.createEnderiteHoe();
                break;
            case "leggins_netherite_essence":
                item = legginsNetheriteEssence.createLegginsNetheriteEssence();
                break;
            case "boot_netherite_essence":
                item = bootNetheriteEssence.createBootNetheriteEssence();
                break;
            case "chestplate_netherite_essence":
                item = chestplateNetheriteEssence.createChestplateNetheriteEssence();
                break;
            case "helmet_netherite_essence":
                item = helmetNetheriteEssence.createHelmetNetheriteEssence();
                break;
            case "helmet_netherite_upgrade":
                item = corruptedUpgrades.createHelmetNetheriteUpgrade();
                break;
            case "chestplate_netherite_upgrade":
                item = corruptedUpgrades.createChestplateNetheriteUpgrade();
                break;
            case "leggins_netherite_upgrade":
                item = corruptedUpgrades.createLeggingsNetheriteUpgrade();
                break;
            case "boot_netherite_upgrade":
                item = corruptedUpgrades.createBootsNetheriteUpgrade();
                break;
            case "cooper_helmet":
                item = CopperArmor.createCopperHelmet();
                break;
            case "cooper_chestplate":
                item = CopperArmor.createCopperChestplate();
                break;
            case "cooper_leggings":
                item = CopperArmor.createCopperLeggings();
                break;
            case "cooper_boots":
                item = CopperArmor.createCopperBoots();
                break;
            case "corrupted_netherite_scrap":
                item = CorruptedNetheriteItems.createCorruptedScrapNetherite();
                break;
            case "corrupted_netherite_ingot":
                item = CorruptedNetheriteItems.createCorruptedNetheriteIngot();
                break;
            case "corrupted_powder":
                item = CorruptedMobItems.createCorruptedPowder();
                break;
            case "corrupted_bone_lime":
                item = CorruptedMobItems.createCorruptedBone(CorruptedMobItems.BoneVariant.LIME);
                break;
            case "corrupted_bone_green":
                item = CorruptedMobItems.createCorruptedBone(CorruptedMobItems.BoneVariant.GREEN);
                break;
            case "corrupted_bone_yellow":
                item = CorruptedMobItems.createCorruptedBone(CorruptedMobItems.BoneVariant.YELLOW);
                break;
            case "corrupted_bone_orange":
                item = CorruptedMobItems.createCorruptedBone(CorruptedMobItems.BoneVariant.ORANGE);
                break;
            case "corrupted_bone_red":
                item = CorruptedMobItems.createCorruptedBone(CorruptedMobItems.BoneVariant.RED);
                break;
            case "corrupted_rotten":
                item = CorruptedMobItems.createCorruptedMeet();
                break;
            case "corrupted_spidereyes":
                item = CorruptedMobItems.createCorruptedSpiderEye();
                break;
            case "corrupted_soul":
                item = corruptedSoul.createCorruptedSoulEssence();
                break;

            //BLOQUES
            case "corrupted_ancient_debris":
                item = corruptedAncientDebris.createcorruptedancientdebris();
                break;
            case "guardian_shulker_heart":
                item = guardianShulkerHeart.createGuardianShulkerHeart();
                break;
            case "endstalactitas":
                item = Endstalactitas.createEndstalactita();
                break;

            //VARIOS
            case "toxicspidereye":
                item = ItemsTotems.createToxicSpiderEye();
                break;
            case "infernalcreeperpowder":
                item = ItemsTotems.createInfernalCreeperPowder();
                break;
            case "whiteenderpearl":
                item = ItemsTotems.createWhiteEnderPearl();
                break;
            case "specialtotem":
                item = ItemsTotems.createSpecialTotem();
                break;
            case "customboat":
                item = customBoat.createBoatItem(target);
                break;
            case "fuel":
                item = customBoat.createFuelItem();
                break;
            case "varita_guardian_blaze":
                item = BlazeItems.createBlazeRod();
                break;
            case "polvo_guardian_blaze":
                item = BlazeItems.createGuardianBlazePowder();
                break;
            case "ultra_pocion_resistencia_fuego":
                item = BlazeItems.createPotionOfFireResistance();
                break;
            case "guardian_shulker_shell":
                item = EndItems.createGuardianShulkerShell();
                break;
            case "enderite_nugget":
                item = EndItems.createEnderiteNugget(cantidad);
                break;
            case "enderite_fragment":
                item = EndItems.createFragmentoEnderite();
                break;
            case "end_amatist":
                item = EndItems.createEndAmatist(cantidad);
                break;
            case "enderite_ingot":
                item = EndItems.createIngotEnderite();
                break;
            case "enderite_upgrades":
                item = EndItems.createEnderiteUpgrades();
                break;

            //Economy Items
            case "vithiums":
                item = EconomyItems.createVithiumCoin();
                break;
            case "vithiums_fichas":
                item = EconomyItems.createVithiumToken();
                break;
            case "mochila":
                item = EconomyItems.createNormalMochila();
                break;
            case "mochila_verde":
                item = EconomyItems.createGreenMochila();
                break;
            case "mochila_roja":
                item = EconomyItems.createRedMochila();
                break;
            case "mochila_azul":
                item = EconomyItems.createBlueMochila();
                break;
            case "mochila_morada":
                item = EconomyItems.createPurpleMochila();
                break;
            case "mochila_negra":
                item = EconomyItems.createBlackMochila();
                break;
            case "mochila_blanca":
                item = EconomyItems.createWhiteMochila();
                break;
            case "mochila_amarilla":
                item = EconomyItems.createYellowMochila();
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
            case "yunque_nivel_1":
                item = EconomyItems.createYunqueReparadorNivel1();
                break;
            case "yunque_nivel_2":
                item = EconomyItems.createYunqueReparadorNivel2();
                break;
            case "icetotem":
                item = economyIceTotem.createIceTotem();
                break;
            case "flytotem":
                item = economyFlyTotem.createFlyTotem();
                break;

            //Otros Items
            case "corrupted_golden_apple":
                item = CorruptedGoldenApple.createCorruptedGoldenApple();
                break;
            case "apilate_gold_block":
                item = CorruptedGoldenApple.createApilateGoldBlock();
                break;
            case "orbe_de_vida":
                item = ReviveItems.createResurrectOrb();
                break;
            case "wither_compass":
                item = UltraWitherCompass.createUltraWitherCompass();
                break;
            case "icecrystal":
                item = ItemsTotems.createIceCrystal();
                break;
            case "tridente_espectral":
                item = tridenteEspectral.createSpectralTrident();
                break;
            default:
                return null;
        }

        if (item != null) {
            item.setAmount(amount);
        }

        return item;
    }
}