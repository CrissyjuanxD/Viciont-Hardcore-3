package Dificultades;

import Dificultades.CustomMobs.GuardianBlaze;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.inventory.PrepareSmithingEvent;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerBedLeaveEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffectType;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.inventory.ItemStack;


import java.util.*;

public class DayFourChanges implements Listener {
    private final JavaPlugin plugin;
    private boolean isApplied = false;
    private final Set<Player> playersInBed = new HashSet<>();
    private final int requiredPlayers = 4;
    private final GuardianBlaze blazespawmer;

    private final NamespacedKey uuidKey;
    private final NamespacedKey upgradeKey;
    private ItemStack upgradeVacio;

    public DayFourChanges(JavaPlugin plugin) {
        this.plugin = plugin;
        this.blazespawmer = new GuardianBlaze(plugin);
        this.uuidKey = new NamespacedKey(plugin, "creator_uuid");
        this.upgradeKey = new NamespacedKey(plugin, "is_upgrade");

        upgradeVacio = createCustomItem(Material.ECHO_SHARD, 100, "Upgrade Vacío");
    }

    public void apply() {
        if (!isApplied) {
            Bukkit.getPluginManager().registerEvents(this, plugin);
            isApplied = true;
            Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), "gamerule playersSleepingPercentage 250");
            registerRecipes();
            blazespawmer.apply();
        }
    }

    public void revert() {
        if (isApplied) {
            isApplied = false;
            Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), "gamerule playersSleepingPercentage 0");
            blazespawmer.revert();
        }
    }

    public ItemStack getUpgradeVacio() {
        if (upgradeVacio == null) {
            upgradeVacio = createCustomItem(Material.ECHO_SHARD, 100, "Upgrade Vacío");
        }
        return upgradeVacio.clone();
    }


    // Definir las capas del altar
    private final Material[][][] altarLayers = {
            // Layer 1
            {
                    {Material.HONEY_BLOCK, Material.HONEYCOMB_BLOCK, Material.HONEYCOMB_BLOCK, Material.HONEYCOMB_BLOCK, Material.HONEY_BLOCK},
                    {Material.HONEYCOMB_BLOCK, Material.HONEY_BLOCK, Material.HONEYCOMB_BLOCK, Material.HONEY_BLOCK, Material.HONEYCOMB_BLOCK},
                    {Material.HONEYCOMB_BLOCK, Material.HONEYCOMB_BLOCK, Material.HONEYCOMB_BLOCK, Material.HONEYCOMB_BLOCK, Material.HONEYCOMB_BLOCK},
                    {Material.HONEYCOMB_BLOCK, Material.HONEY_BLOCK, Material.HONEYCOMB_BLOCK, Material.HONEY_BLOCK, Material.HONEYCOMB_BLOCK},
                    {Material.HONEY_BLOCK, Material.HONEYCOMB_BLOCK, Material.HONEYCOMB_BLOCK, Material.HONEYCOMB_BLOCK, Material.HONEY_BLOCK}
            },

            // Layer 2
            {
                    {Material.HONEY_BLOCK, Material.AIR, Material.AIR, Material.AIR, Material.HONEY_BLOCK},
                    {Material.AIR, Material.AIR, Material.TORCH, Material.AIR, Material.AIR},
                    {Material.AIR, Material.TORCH, Material.HONEYCOMB_BLOCK, Material.TORCH, Material.AIR},
                    {Material.AIR, Material.AIR, Material.TORCH, Material.AIR, Material.AIR},
                    {Material.HONEY_BLOCK, Material.AIR, Material.AIR, Material.AIR, Material.HONEY_BLOCK}
            },

            // Layer 3
            {
                    {Material.AIR, Material.AIR, Material.AIR, Material.AIR, Material.AIR},
                    {Material.AIR, Material.AIR, Material.AIR, Material.AIR, Material.AIR},
                    {Material.AIR, Material.AIR, Material.BEE_NEST, Material.AIR, Material.AIR},
                    {Material.AIR, Material.AIR, Material.AIR, Material.AIR, Material.AIR},
                    {Material.AIR, Material.AIR, Material.AIR, Material.AIR, Material.AIR}
            },

            // Layer 4
            {
                    {Material.AIR, Material.AIR, Material.AIR, Material.AIR, Material.AIR},
                    {Material.AIR, Material.AIR, Material.AIR, Material.AIR, Material.AIR},
                    {Material.AIR, Material.AIR, Material.HONEY_BLOCK, Material.AIR, Material.AIR},
                    {Material.AIR, Material.AIR, Material.AIR, Material.AIR, Material.AIR},
                    {Material.AIR, Material.AIR, Material.AIR, Material.AIR, Material.AIR}
            }
    };

    private void checkAltarLayers(Location location, Player player) {
        int baseY = location.getBlockY(); // Altura base del Bee Nest

        for (int y = 0; y < altarLayers.length; y++) { // Iterar capas
            player.sendMessage(ChatColor.GOLD + "Layer " + (y + 1) + ":");
            StringBuilder layerOutput = new StringBuilder();

            for (int x = -2; x <= 2; x++) { // Iterar filas
                for (int z = -2; z <= 2; z++) { // Iterar columnas
                    Material expected = altarLayers[y][x + 2][z + 2];
                    Material actual = Objects.requireNonNull(location.getWorld()).getBlockAt(
                            location.getBlockX() + x,
                            baseY - 2 + y, // Calcular altura relativa usando Layer 3 como base
                            location.getBlockZ() + z
                    ).getType();

                    if (expected == actual) {
                        layerOutput.append(ChatColor.GREEN).append(getSymbol(expected)).append(" ");
                    } else {
                        layerOutput.append(ChatColor.RED).append(getSymbol(expected)).append(" ");
                    }
                }
                player.sendMessage(layerOutput.toString());
                layerOutput.setLength(0); // Limpiar para la siguiente fila
            }
        }
    }

    // Devuelve el símbolo representativo para el material
    private String getSymbol(Material material) {
        switch (material) {
            case HONEY_BLOCK -> {
                return "HB";
            }
            case HONEYCOMB_BLOCK -> {
                return "HC";
            }
            case TORCH -> {
                return "T";
            }
            case BEE_NEST -> {
                return "N";
            }
            case AIR -> {
                return "A";
            }
            default -> {
                return "?";
            }
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!isApplied) return;
        if (event.getHand() != EquipmentSlot.HAND) return;

        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Block block = event.getClickedBlock();
            Player player = event.getPlayer();

            if (block != null && block.getType() == Material.BEE_NEST) {
                Location altarLocation = block.getLocation();

                // Verificar y enviar el mensaje de error por capas
                checkAltarLayers(altarLocation, player);

                if (isValidQueenBeeAltar(altarLocation)) {
                    if (player.getPotionEffect(PotionEffectType.BAD_OMEN) != null) {
                        if (isQueenBeeSpawned(altarLocation)) {
                            player.sendMessage(ChatColor.RED + "Ya se ha invocado una Reina Abeja en este altar.");
                        } else {
                            spawnQueenBee(altarLocation);
                            player.getWorld().playSound(altarLocation, Sound.ENTITY_BEE_STING, 5.0f, 0.1f);
                            player.getWorld().playSound(altarLocation, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 5.0f, 0.1f);
                            player.getWorld().setStorm(true);
                            player.getWorld().setWeatherDuration(2000);
                        }
                    } else {
                        player.sendMessage(ChatColor.RED + "Necesitas el efecto Bad Omen para invocar a la Reina Abeja.");
                    }
                }
            }
        }
    }

    private boolean isValidQueenBeeAltar(Location location) {
        int baseY = location.getBlockY();

        for (int y = 0; y < altarLayers.length; y++) {
            for (int x = -2; x <= 2; x++) {
                for (int z = -2; z <= 2; z++) {
                    Material expected = altarLayers[y][x + 2][z + 2];
                    Material actual = Objects.requireNonNull(location.getWorld()).getBlockAt(
                            location.getBlockX() + x,
                            baseY - 2 + y,
                            location.getBlockZ() + z
                    ).getType();

                    if (expected != actual) {
                        return false;
                    }
                }
            }
        }
        return true;
    }


    private boolean isQueenBeeSpawned(Location altarLocation) {
        for (Entity entity : Objects.requireNonNull(altarLocation.getWorld()).getNearbyEntities(altarLocation, 50, 50, 50)) {
            if (entity instanceof Bee bee && "Abeja Reina".equals(bee.getCustomName())) {
                return true;
            }
        }
        return false;
    }

    private void spawnQueenBee(Location altarLocation) {
        Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "spawnvct queenbee " + altarLocation.getBlockX() + " " + altarLocation.getBlockY() + " " + altarLocation.getBlockZ());
    }


    @EventHandler
    public void onPlayerBedEnter(PlayerBedEnterEvent event) {
        if (!isApplied) return;

        Player player = event.getPlayer();
        World world = player.getWorld();

        if (world.getTime() >= 12530 && world.getTime() <= 23458 && !world.isThundering()) {
            if (!playersInBed.contains(player)) {
                playersInBed.add(player);
            }

            int inBed = playersInBed.size();
            Bukkit.broadcastMessage(ChatColor.GOLD + String.format(" ۞ Faltan %d/%d jugadores para saltar la noche.", inBed, requiredPlayers));

            // Si ya hay suficientes jugadores, saltar la noche
            if (inBed >= requiredPlayers) {
                Bukkit.broadcastMessage(ChatColor.GOLD + "¡Suficientes jugadores están en cama! Saltando la noche...");
                world.setTime(0);
                playersInBed.clear();
            }
        }
    }

    @EventHandler
    public void onPlayerBedLeave(PlayerBedLeaveEvent event) {
        if (!isApplied) return;

        Player player = event.getPlayer();

        if (playersInBed.remove(player)) {
            int inBed = playersInBed.size();
            Bukkit.broadcastMessage(ChatColor.GOLD + String.format(" ۞ Faltan %d/%d jugadores para saltar la noche.", inBed, requiredPlayers));
        }
    }

    @EventHandler
    public void onEntityCombust(EntityCombustEvent event) {
        if (!isApplied) return;
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            event.setDuration(Integer.MAX_VALUE);
        }
    }

    //-----------------------------------------
    //Gameplay Netherite
    //-----------------------------------------

    // Registrar recetas personalizadas
    private void registerRecipes() {

        // ===== Fragmento de Upgrade =====
        ItemStack fragmentoUpgrade = createCustomItem(Material.ECHO_SHARD, 101, "Fragmento de Upgrade de Netherite");
        ShapedRecipe fragmentRecipe = new ShapedRecipe(new NamespacedKey(plugin, "fragment_upgrade"), fragmentoUpgrade);
        fragmentRecipe.shape("SDS", "DSD", "SDS");
        fragmentRecipe.setIngredient('D', Material.DIAMOND);
        fragmentRecipe.setIngredient('S', Material.NETHERITE_SCRAP);
        Bukkit.addRecipe(fragmentRecipe);


        // ===== Duplicador =====
        ItemStack duplicador = createCustomItem(Material.ECHO_SHARD, 102, "Duplicador");
        ShapedRecipe duplicadorRecipe = new ShapedRecipe(new NamespacedKey(plugin, "duplicador"), duplicador);
        duplicadorRecipe.shape("BDB", "DSD", "BSB");
        duplicadorRecipe.setIngredient('B', Material.NETHER_BRICK);
        duplicadorRecipe.setIngredient('D', Material.DIAMOND);
        duplicadorRecipe.setIngredient('S', Material.NETHERITE_SCRAP);
        Bukkit.addRecipe(duplicadorRecipe);

        // ===== Upgrade de Netherite =====
        ItemStack netheriteUpgrade = new ItemStack(Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE);
        ShapedRecipe netheriteRecipe = new ShapedRecipe(new NamespacedKey(plugin, "netherite_upgrade"), netheriteUpgrade);
        netheriteRecipe.shape("FNF", "FUF", "FFF");
        netheriteRecipe.setIngredient('F', new RecipeChoice.ExactChoice(fragmentoUpgrade)); // CMD 101
        netheriteRecipe.setIngredient('U', new RecipeChoice.ExactChoice(upgradeVacio));// CMD 100
        netheriteRecipe.setIngredient('N', Material.NETHERRACK);
        Bukkit.addRecipe(netheriteRecipe);

        ItemStack netheriteDuplicado = new ItemStack(Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE, 2);
        ShapedRecipe netheriteDuplicadoRecipe = new ShapedRecipe(new NamespacedKey(plugin, "netherite_duplicado"), netheriteDuplicado);
        netheriteDuplicadoRecipe.shape("PNP", "PUP", "PPP");
        netheriteDuplicadoRecipe.setIngredient('P', new RecipeChoice.ExactChoice(duplicador)); // CMD 102
        netheriteDuplicadoRecipe.setIngredient('U', new RecipeChoice.ExactChoice(netheriteUpgrade));// CMD 101
        netheriteDuplicadoRecipe.setIngredient('N', Material.NETHERRACK);
        Bukkit.addRecipe(netheriteDuplicadoRecipe);
    }

    // Crear ítems personalizados
    public ItemStack createCustomItem(Material material, int modelData, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(name);
            meta.setCustomModelData(modelData);
            item.setItemMeta(meta);
        }

        return item;
    }

    @EventHandler
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        if (event.getRecipe() == null) return;

        ItemStack result = event.getRecipe().getResult();
        if (result != null && result.getType() == Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE) {
            if (event.getView().getPlayer() instanceof Player player) {
                // Crear un nuevo upgrade con nombre personalizado
                ItemStack netheriteUpgrade = createNetheriteUpgrade(player);
                event.getInventory().setResult(netheriteUpgrade);
            }
        }
    }

    // Crear Upgrade de Netherite con UUID
    public ItemStack createNetheriteUpgrade(Player player) {
        ItemStack item = new ItemStack(Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            // Guardar UUID y nombre en el Lore
            PersistentDataContainer data = meta.getPersistentDataContainer();
            data.set(uuidKey, PersistentDataType.STRING, player.getUniqueId().toString());
            data.set(upgradeKey, PersistentDataType.BOOLEAN, true);

            // Lore personalizado
            meta.setLore(Arrays.asList("§7Upgrade de §a" + player.getName()));
            item.setItemMeta(meta);
        }

        return item;
    }


    // Validar si el jugador puede usar la Upgrade
    public boolean canUseUpgrade(Player player, ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;

        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer data = meta.getPersistentDataContainer();

        // Verificar si tiene clave de Upgrade
        if (!data.has(upgradeKey, PersistentDataType.BOOLEAN)) return false;

        // Verificar si el UUID coincide
        String uuid = data.get(uuidKey, PersistentDataType.STRING);
        return uuid != null && uuid.equals(player.getUniqueId().toString());
    }

    @EventHandler
    public void onSmithingPrepare(PrepareSmithingEvent event) {
        // Obtener la plantilla en el slot 1
        ItemStack upgrade = event.getInventory().getItem(1); // Slot del template

        // Validar si hay una plantilla
        if (upgrade == null || upgrade.getType() != Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE) return;

        Player player = (Player) event.getView().getPlayer();

        // Validar si el jugador puede usar la plantilla
        if (!canUseUpgrade(player, upgrade)) {
            event.setResult(null); // Bloquear el resultado
            player.sendMessage("§c¡No puedes usar una plantilla que no te pertenece!");
        }
    }


    @EventHandler
    public void onCraftPrepare(PrepareItemCraftEvent event) {
        CraftingInventory inventory = event.getInventory();
        ItemStack[] matrix = inventory.getMatrix(); // Obtener los ítems del crafteo
        Player player = (Player) event.getView().getPlayer();

        // Validar si es una receta personalizada
        if (!isCustomRecipe(matrix)) { // Solo verificar recetas personalizadas
            return;
        }

        // Variables para ingredientes específicos
        ItemStack upgrade = null; // Para Netherite Upgrade
        boolean validDuplicators = true; // Para validar duplicadores en posiciones correctas

        // Revisar la cuadrícula del crafteo
        for (int i = 0; i < matrix.length; i++) {
            ItemStack item = matrix[i];

            // Verificar si el slot está vacío
            if (item == null || !item.hasItemMeta()) {
                if (i == 4) { // Slot central debe tener la Upgrade
                    validDuplicators = false; // Falta el upgrade
                }
                continue;
            }

            ItemMeta meta = item.getItemMeta();

            // Posición central (Netherite Upgrade)
            if (i == 4) {
                if (item.getType() == Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE) {
                    upgrade = item; // Asignar la Upgrade
                } else {
                    validDuplicators = false; // Error en el centro
                }
            }
            // Posición superior central (Netherrack)
            else if (i == 1) {
                if (item.getType() != Material.NETHERRACK) {
                    validDuplicators = false; // Error en la posición superior
                }
            }
            // Todos los demás slots (Duplicadores)
            else {
                if (meta.hasCustomModelData() && meta.getCustomModelData() == 102) {
                    continue; // Es un duplicador válido
                } else {
                    validDuplicators = false; // Error en duplicador
                }
            }
        }

        // Validar el crafteo completo
        if (validDuplicators && upgrade != null) {
            if (!canUseUpgrade(player, upgrade)) {
                inventory.setResult(null); // Bloquear el crafteo si el UUID no coincide
                player.sendMessage("§c¡No puedes duplicar esta Upgrade de Netherite!");
            } else {
                // Crear el resultado con 2 ítems duplicados
                ItemStack duplicatedUpgrade = createNetheriteUpgrade(player);
                duplicatedUpgrade.setAmount(2); // Configurar la cantidad
                inventory.setResult(duplicatedUpgrade);
            }
        } else {
            inventory.setResult(null);
        }
    }

    /**
     * Consumir manualmente los ingredientes al craftear.
     */
    @EventHandler
    public void onCraftItem(CraftItemEvent event) {
        CraftingInventory inventory = event.getInventory();
        ItemStack[] matrix = inventory.getMatrix(); // Obtener los ítems del crafteo

        // Validar si es una receta personalizada
        if (!isCustomRecipe(matrix)) {
            return; // Deja pasar las recetas vanilla
        }

        // Evitar duplicación al procesar shift+click
        if (event.isShiftClick()) {
            // Calcular cuántas veces puede hacerse el crafteo
            int maxCrafts = Integer.MAX_VALUE; // Iniciar con un valor alto
            for (ItemStack item : matrix) {
                if (item == null || item.getType() == Material.AIR) continue;

                int amount = item.getAmount();
                if (item.getType() == Material.NETHERRACK ||
                        item.getType() == Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE ||
                        (item.hasItemMeta() && item.getItemMeta().hasCustomModelData() &&
                                item.getItemMeta().getCustomModelData() == 102)) {
                    maxCrafts = Math.min(maxCrafts, amount); // Determinar el mínimo posible
                }
            }

            // Si no hay suficientes ingredientes para más de 1 crafteo, bloquear
            if (maxCrafts < 1) {
                event.setCancelled(true);
                return;
            }
        }

        // Consumir los ingredientes
        for (int i = 0; i < matrix.length; i++) {
            ItemStack item = matrix[i];
            if (item == null) continue;

            // Reducir el conteo de duplicadores y materiales
            if (item.hasItemMeta()) {
                ItemMeta meta = item.getItemMeta();
                if (meta.hasCustomModelData() && meta.getCustomModelData() == 102) {
                    item.setAmount(item.getAmount() - 1); // Consumir 1 duplicador
                }
            } else if (item.getType() == Material.NETHERRACK ||
                    item.getType() == Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE) {
                item.setAmount(item.getAmount() - 1); // Consumir Netherrack o Upgrade
            }
        }
        inventory.setMatrix(matrix); // Actualizar la cuadrícula
    }

    /**
     * Verifica si la receta es personalizada según el patrón esperado.
     */
    private boolean isCustomRecipe(ItemStack[] matrix) {
        return matrix.length == 9 &&
                matrix[4] != null && matrix[4].getType() == Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE &&
                matrix[1] != null && matrix[1].getType() == Material.NETHERRACK &&
                checkDuplicators(matrix);
    }

    /**
     * Verifica si los duplicadores están en las posiciones correctas.
     */
    private boolean checkDuplicators(ItemStack[] matrix) {
        int[] duplicatorSlots = {0, 2, 3, 5, 6, 7, 8}; // Posiciones de los duplicadores
        for (int slot : duplicatorSlots) {
            ItemStack item = matrix[slot];
            if (item == null || !item.hasItemMeta()) return false; // Debe tener meta
            ItemMeta meta = item.getItemMeta();
            if (!meta.hasCustomModelData() || meta.getCustomModelData() != 102) return false; // Validar duplicador
        }
        return true; // Todos los duplicadores son válidos
    }
}
