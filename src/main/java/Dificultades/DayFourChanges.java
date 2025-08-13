package Dificultades;

import Dificultades.CustomMobs.CorruptedInfernalSpider;
import Dificultades.CustomMobs.GuardianBlaze;
import Dificultades.CustomMobs.GuardianCorruptedSkeleton;
import Dificultades.CustomMobs.QueenBeeHandler;
import Dificultades.Features.MobCapManager;
import Enchants.EssenceFactory;
import items.EmblemItems;
import items.UpgradeNTItems;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.inventory.PrepareSmithingEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffectType;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import Handlers.DayHandler;


import java.util.*;

public class DayFourChanges implements Listener {
    private final JavaPlugin plugin;
    private boolean isApplied = false;
    private final Map<Location, Long> altarCooldowns = new HashMap<>();
    private final Random random = new Random();
    private final GuardianBlaze blazespawmer;
    private final GuardianCorruptedSkeleton guardianCorruptedSkeleton;
    private final CorruptedInfernalSpider corruptedInfernalSpider;
    private final DayHandler dayHandler;

    private final NamespacedKey uuidKey;
    private final NamespacedKey upgradeKey;

    public DayFourChanges(JavaPlugin plugin, DayHandler handler) {
        this.plugin = plugin;
        this.dayHandler = handler;
        this.blazespawmer = new GuardianBlaze(plugin);
        this.guardianCorruptedSkeleton = new GuardianCorruptedSkeleton(plugin);
        this.corruptedInfernalSpider = new CorruptedInfernalSpider(plugin);
        this.uuidKey = new NamespacedKey(plugin, "creator_uuid");
        this.upgradeKey = new NamespacedKey(plugin, "is_upgrade");
    }

    public void apply() {
        if (!isApplied) {
            Bukkit.getPluginManager().registerEvents(this, plugin);
            isApplied = true;
            registerUpgradeRecipes();
            registerEmblemRecipes();
            blazespawmer.apply();
            guardianCorruptedSkeleton.apply();
            corruptedInfernalSpider.apply();
        }
    }

    public void revert() {
        if (isApplied) {
            isApplied = false;
            blazespawmer.revert();
            guardianCorruptedSkeleton.revert();
            corruptedInfernalSpider.revert();

            Bukkit.removeRecipe(new NamespacedKey(plugin, "fragment_upgrade"));
            Bukkit.removeRecipe(new NamespacedKey(plugin, "duplicador"));
            Bukkit.removeRecipe(new NamespacedKey(plugin, "netherite_upgrade"));
            Bukkit.removeRecipe(new NamespacedKey(plugin, "netherite_duplicado"));

            Bukkit.removeRecipe(new NamespacedKey(plugin, "nether_emblem"));
            Bukkit.removeRecipe(new NamespacedKey(plugin, "overworld_emblem"));
            Bukkit.removeRecipe(new NamespacedKey(plugin, "fragmento_infernal"));

            HandlerList.unregisterAll(this);
        }
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
        int baseY = location.getBlockY();

        for (int y = 0; y < altarLayers.length; y++) {
            player.sendMessage(ChatColor.GOLD + "Layer " + (y + 1) + ":");
            StringBuilder layerOutput = new StringBuilder();

            for (int x = -2; x <= 2; x++) {
                for (int z = -2; z <= 2; z++) {
                    Material expected = altarLayers[y][x + 2][z + 2];
                    Material actual = Objects.requireNonNull(location.getWorld()).getBlockAt(
                            location.getBlockX() + x,
                            baseY - 2 + y,
                            location.getBlockZ() + z
                    ).getType();

                    if (expected == actual) {
                        layerOutput.append(ChatColor.GREEN).append(getSymbol(expected)).append(" ");
                    } else {
                        layerOutput.append(ChatColor.RED).append(getSymbol(expected)).append(" ");
                    }
                }
                player.sendMessage(layerOutput.toString());
                layerOutput.setLength(0);
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

    private boolean isOnCooldown(Location loc) {
        long currentTime = System.currentTimeMillis();
        long lastUsed = altarCooldowns.getOrDefault(loc, 0L);
        if (currentTime - lastUsed < 2000) return true;
        altarCooldowns.put(loc, currentTime);
        return false;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!isApplied) return;
        if (event.getHand() != EquipmentSlot.HAND) return;

        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Block block = event.getClickedBlock();
            Player player = event.getPlayer();

            if (block != null && block.getType() == Material.BEE_NEST) {
                if (isOnCooldown(block.getLocation())) return;
                Location altarLocation = block.getLocation();

                // Verificar y enviar el mensaje de error por capas
                checkAltarLayers(altarLocation, player);

                if (isValidQueenBeeAltar(altarLocation)) {
                    if (player.getPotionEffect(PotionEffectType.BAD_OMEN) != null) {
                        if (isQueenBeeSpawned(altarLocation)) {
                            player.sendMessage(ChatColor.RED + "۞ Ya se ha invocado una Reina Abeja en este altar.");
                        } else {
                            spawnQueenBee(altarLocation);
                            player.removePotionEffect(PotionEffectType.BAD_OMEN);
                            int radius = 50;
                            for (Player nearbyPlayer : Bukkit.getOnlinePlayers()) {
                                if (nearbyPlayer.getWorld().equals(altarLocation.getWorld()) &&
                                        nearbyPlayer.getLocation().distanceSquared(altarLocation) <= radius * radius) {

                                    nearbyPlayer.playSound(altarLocation, Sound.ENTITY_BEE_STING, 5.0f, 0.1f);
                                    nearbyPlayer.playSound(altarLocation, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 5.0f, 0.1f);
                                    nearbyPlayer.playSound(altarLocation, "minecraft:custom.emuerte" , 5.0f, 0.1f);
                                    nearbyPlayer.playSound(altarLocation, "minecraft:custom.music2_skybattle", SoundCategory.RECORDS, 1.0f, 0.1f);
                                }
                            }
                            player.getWorld().setStorm(true);
                            player.getWorld().setWeatherDuration(2000);
                        }
                    } else {
                        player.sendMessage(ChatColor.RED + "۞ Necesitas el efecto Bad Omen para invocar a la Reina Abeja.");
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
        World world = altarLocation.getWorld();
        if (world == null) return;

        Location center = altarLocation.clone().add(0.5, 0.5, 0.5);
        int totalSteps = 100;
        int finalY = 7;

        new BukkitRunnable() {
            int step = 0;

            @Override
            public void run() {
                if (step <= totalSteps) {
                    double progress = (double) step / totalSteps;
                    double yOffset = progress * finalY;

                    Location particleLoc = center.clone().add(0, yOffset, 0);
                    world.spawnParticle(Particle.END_ROD, particleLoc, 5, 0.1, 0, 0.1, 0.01);

                    if (step % 20 == 0) {
                        world.playSound(center, Sound.BLOCK_HONEY_BLOCK_SLIDE, 3.0f, 0.5f + (float)progress);
                    }

                    step++;
                } else if (step <= totalSteps + 40) {
                    // Expansión circular de partículas (2 segundos)
                    double angle = (step - totalSteps) * (Math.PI / 20); // 2 vueltas
                    for (int i = 0; i < 360; i += 10) {
                        double radians = Math.toRadians(i);
                        double radius = 1.5 + Math.sin(angle) * 1.5;
                        double x = Math.cos(radians) * radius;
                        double z = Math.sin(radians) * radius;
                        Location loc = center.clone().add(0, finalY, 0).add(x, 0, z);
                        world.spawnParticle(Particle.END_ROD, loc, 1, 0, 0, 0, 0);
                    }

                    if ((step - totalSteps) % 10 == 0) {
                        world.playSound(center.clone().add(0, finalY, 0), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 3.0f, 1.5f);
                    }

                    step++;
                } else {
                    Location spawnLocation = center.clone().add(0, finalY, 0);
                    world.spawnParticle(Particle.EXPLOSION, spawnLocation, 1);
                    world.spawnParticle(Particle.TOTEM_OF_UNDYING, spawnLocation, 100, 0.5, 1, 0.5, 0.1);
                    world.playSound(spawnLocation, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 5.0f, 1.0f);

                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "spawnvct queenbee " +
                            spawnLocation.getBlockX() + " " +
                            spawnLocation.getBlockY() + " " +
                            spawnLocation.getBlockZ());

                    destroyAltarStructure(altarLocation);

                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void destroyAltarStructure(Location baseLocation) {
        int baseY = baseLocation.getBlockY();
        World world = baseLocation.getWorld();
        if (world == null) return;

        for (int y = 0; y < altarLayers.length; y++) {
            for (int x = -2; x <= 2; x++) {
                for (int z = -2; z <= 2; z++) {
                    Location blockLoc = new Location(
                            world,
                            baseLocation.getBlockX() + x,
                            baseY - 2 + y,
                            baseLocation.getBlockZ() + z
                    );
                    world.getBlockAt(blockLoc).setType(Material.AIR);
                }
            }
        }
    }

    @EventHandler
    public void onEntityCombust(EntityCombustEvent event) {
        if (!isApplied) return;
        if (event.getEntity() instanceof Player player) {
            player.setFireTicks(Integer.MAX_VALUE);
        }
    }

    //-----------------------------------------
    //Gameplay Netherite
    //-----------------------------------------

    private void registerUpgradeRecipes() {

        // ===== Fragmento de Upgrade =====
        ShapedRecipe fragmentRecipe = new ShapedRecipe(new NamespacedKey(plugin, "fragment_upgrade"), UpgradeNTItems.createFragmentoUpgrade());
        fragmentRecipe.shape("SDS", "D D", "SDS");
        fragmentRecipe.setIngredient('D', Material.DIAMOND);
        fragmentRecipe.setIngredient('S', Material.NETHERITE_SCRAP);
        Bukkit.addRecipe(fragmentRecipe);

        // ===== Duplicador =====
        ShapedRecipe duplicadorRecipe = new ShapedRecipe(new NamespacedKey(plugin, "duplicador"), UpgradeNTItems.createDuplicador());
        duplicadorRecipe.shape("BDB", "DSD", "BSB");
        duplicadorRecipe.setIngredient('B', Material.NETHER_BRICK);
        duplicadorRecipe.setIngredient('D', Material.DIAMOND);
        duplicadorRecipe.setIngredient('S', Material.NETHERITE_SCRAP);
        Bukkit.addRecipe(duplicadorRecipe);

        // ===== Upgrade de Netherite =====
        ItemStack netheriteUpgrade = new ItemStack(Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE);
        ShapedRecipe netheriteRecipe = new ShapedRecipe(new NamespacedKey(plugin, "netherite_upgrade"), netheriteUpgrade);
        netheriteRecipe.shape("FNF", "FUF", "FFF");
        netheriteRecipe.setIngredient('F', new RecipeChoice.ExactChoice(UpgradeNTItems.createFragmentoUpgrade())); // CMD 101
        netheriteRecipe.setIngredient('U', new RecipeChoice.ExactChoice(UpgradeNTItems.createUpgradeVacio()));// CMD 100
        netheriteRecipe.setIngredient('N', Material.NETHERRACK);
        Bukkit.addRecipe(netheriteRecipe);

        ItemStack netheriteDuplicado = new ItemStack(Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE);
        ShapedRecipe netheriteDuplicadoRecipe = new ShapedRecipe(new NamespacedKey(plugin, "netherite_duplicado"), netheriteDuplicado);
        netheriteDuplicadoRecipe.shape("DPD", "PUP", "NPN");
        netheriteDuplicadoRecipe.setIngredient('P', new RecipeChoice.ExactChoice(UpgradeNTItems.createDuplicador())); // CMD 102
        netheriteDuplicadoRecipe.setIngredient('U', new RecipeChoice.ExactChoice(netheriteUpgrade));// CMD 101
        netheriteDuplicadoRecipe.setIngredient('N', Material.NETHERRACK);
        netheriteDuplicadoRecipe.setIngredient('D', Material.DIAMOND);
        Bukkit.addRecipe(netheriteDuplicadoRecipe);
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
            PersistentDataContainer data = meta.getPersistentDataContainer();
            data.set(uuidKey, PersistentDataType.STRING, player.getUniqueId().toString());
            data.set(upgradeKey, PersistentDataType.BOOLEAN, true);

            meta.setLore(Arrays.asList("§7Upgrade de §a" + player.getName()));
            item.setItemMeta(meta);
        }

        return item;
    }

    private boolean isCorruptedRecipe(Recipe recipe) {
        if (!(recipe instanceof ShapedRecipe)) return false;

        ShapedRecipe shaped = (ShapedRecipe) recipe;
        return shaped.getKey().getNamespace().equals(plugin.getName()) &&
                shaped.getKey().getKey().contains("corrupted");
    }

    // Validar si el jugador puede usar la Upgrade
    public boolean canUseUpgrade(Player player, ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;

        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer data = meta.getPersistentDataContainer();

        if (!data.has(upgradeKey, PersistentDataType.BOOLEAN)) return false;

        String uuid = data.get(uuidKey, PersistentDataType.STRING);
        return uuid != null && uuid.equals(player.getUniqueId().toString());
    }

    @EventHandler
    public void onSmithingPrepare(PrepareSmithingEvent event) {
        ItemStack upgrade = event.getInventory().getItem(1);

        if (upgrade == null || upgrade.getType() != Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE) return;

        Player player = (Player) event.getView().getPlayer();

        if (!canUseUpgrade(player, upgrade)) {
            event.setResult(null);
            player.sendMessage("§c۞ No puedes usar una plantilla que no te pertenece!");
        }
    }

    @EventHandler
    public void onCraftPrepare(PrepareItemCraftEvent event) {
        if (event.getRecipe() == null ||
                !(event.getRecipe() instanceof ShapedRecipe) ||
                !((ShapedRecipe)event.getRecipe()).getKey().equals(new NamespacedKey(plugin, "netherite_duplicado"))) {
            return;
        }

        CraftingInventory inventory = event.getInventory();
        ItemStack[] matrix = inventory.getMatrix();
        Player player = (Player) event.getView().getPlayer();
        // Validación por posiciones específicas
        boolean isValid = true;
        ItemStack upgrade = null;

        for (int i = 0; i < matrix.length; i++) {
            ItemStack item = matrix[i];

            switch (i) {
                case 0: case 2:
                    if (item == null || item.getType() != Material.DIAMOND) {
                        isValid = false;
                    }
                    break;

                case 1: case 3: case 5: case 7:
                    if (item == null || !item.hasItemMeta() ||
                            item.getItemMeta().getCustomModelData() != 102) {
                        isValid = false;
                    }
                    break;

                case 4:
                    if (item != null && item.getType() == Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE) {
                        upgrade = item;
                    } else {
                        isValid = false;
                    }
                    break;

                case 6: case 8:
                    if (item == null || item.getType() != Material.NETHERRACK) {
                        isValid = false;
                    }
                    break;

                default:
                    break;
            }

            if (!isValid) break;
        }

        if (isValid && upgrade != null) {
            if (!canUseUpgrade(player, upgrade)) {
                inventory.setResult(null);
                player.sendMessage("§c۞ No puedes duplicar esta Upgrade de Netherite!");
            } else {
                ItemStack duplicatedUpgrade = createNetheriteUpgrade(player);
                duplicatedUpgrade.setAmount(2);
                inventory.setResult(duplicatedUpgrade);
            }
        } else {
            inventory.setResult(null);
        }
    }

    @EventHandler
    public void onCraftItem(CraftItemEvent event) {
        if (!(event.getRecipe() instanceof ShapedRecipe) ||
                !((ShapedRecipe)event.getRecipe()).getKey().equals(new NamespacedKey(plugin, "netherite_duplicado"))) {
            return;
        }

        event.setCancelled(true);

        CraftingInventory inv = event.getInventory();
        Player player = (Player) event.getWhoClicked();

        ItemStack upgrade = inv.getMatrix()[4];
        if (upgrade == null || !canUseUpgrade(player, upgrade)) {
            player.sendMessage("§c۞ No puedes duplicar esta Upgrade de Netherite!");
            return;
        }

        int amountToCraft = event.isShiftClick() ? calculateMaxCrafts(inv) : 1;

        if (amountToCraft <= 0) return;

        ItemStack result = createNetheriteUpgrade(player);
        result.setAmount(2 * amountToCraft);

        consumeIngredients(inv, amountToCraft);

        HashMap<Integer, ItemStack> remaining = player.getInventory().addItem(result);
        if (!remaining.isEmpty()) {
            for (ItemStack item : remaining.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), item);
            }
        }

        player.updateInventory();
    }

    private int calculateMaxCrafts(CraftingInventory inv) {
        int max = Integer.MAX_VALUE;
        ItemStack[] matrix = inv.getMatrix();

        for (int i = 0; i < matrix.length; i++) {
            if (matrix[i] == null) continue;

            int required = 1;
            if (i == 4) {
                required = 1;
            }

            max = Math.min(max, matrix[i].getAmount() / required);
        }

        return max;
    }

    private void consumeIngredients(CraftingInventory inv, int times) {
        ItemStack[] matrix = inv.getMatrix();

        for (int i = 0; i < matrix.length; i++) {
            if (matrix[i] == null) continue;

            int toRemove = 1 * times;
            if (i == 4) {
                toRemove = 1 * times;
            }

            matrix[i].setAmount(matrix[i].getAmount() - toRemove);
            if (matrix[i].getAmount() <= 0) {
                matrix[i] = null;
            }
        }

        inv.setMatrix(matrix);
    }

    // Nether Emblem y Overworld Emblem

    private void registerEmblemRecipes() {

        // Receta del Nether Emblem
        ShapedRecipe netherEmblemRecipe = new ShapedRecipe(new NamespacedKey(plugin, "nether_emblem"), EmblemItems.createNetherEmblem());
        netherEmblemRecipe.shape("EIE", "OAO", "EIE");

        netherEmblemRecipe.setIngredient('E', new RecipeChoice.ExactChoice(EssenceFactory.createProtectionEssence())); // Cualquier esencia
        netherEmblemRecipe.setIngredient('O', Material.GOLD_BLOCK);
        netherEmblemRecipe.setIngredient('I', new RecipeChoice.ExactChoice(EmblemItems.createFragmentoInfernal()));
        netherEmblemRecipe.setIngredient('A', new RecipeChoice.ExactChoice(QueenBeeHandler.createAguijonAbejaReina())); // Aguijón de Abeja Reina

        // Receta del Overworld Emblem
        ShapedRecipe overworldEmblemRecipe = new ShapedRecipe(new NamespacedKey(plugin, "overworld_emblem"), EmblemItems.createOverworldEmblem());
        overworldEmblemRecipe.shape("WWW", "NEN", "GSG");

        overworldEmblemRecipe.setIngredient('W', Material.WITHER_SKELETON_SKULL);
        overworldEmblemRecipe.setIngredient('S', Material.NETHERITE_SCRAP);
        overworldEmblemRecipe.setIngredient('E', new RecipeChoice.ExactChoice(EmblemItems.createNetherEmblem()));
        overworldEmblemRecipe.setIngredient('N', Material.NETHERITE_INGOT);
        overworldEmblemRecipe.setIngredient('G', Material.GRASS_BLOCK);

        //Receta del Fragmento Infernal

        ShapedRecipe fragementoInfernal = new ShapedRecipe(new NamespacedKey(plugin, "fragmento_infernal"), EmblemItems.createFragmentoInfernal());
        fragementoInfernal.shape("GPP", "PGP", "PPG");

        fragementoInfernal.setIngredient('G', Material.GILDED_BLACKSTONE);
        fragementoInfernal.setIngredient('P', new RecipeChoice.ExactChoice(EmblemItems.createPepitaInfernal()));

        Bukkit.addRecipe(netherEmblemRecipe);
        Bukkit.addRecipe(overworldEmblemRecipe);
        Bukkit.addRecipe(fragementoInfernal);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (!isApplied) return;
        if (event.getBlock().getType() == Material.GILDED_BLACKSTONE) {
            if (Math.random() <= 0.25) {
                event.setDropItems(false);
            }

            if (Math.random() <= 0.10) {
                ItemStack pepitaInfernal = EmblemItems.createPepitaInfernal();
                event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), pepitaInfernal);

                String[] mobs = {"bombita", "iceologer", "corruptedzombie", "corruptedspider", "corruptedinfernalspider"};
                String randomMob = mobs[(int) (Math.random() * mobs.length)];

                Location loc = event.getBlock().getLocation();
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                        "spawnvct " + randomMob + " " +
                                loc.getX() + " " + loc.getY() + " " + loc.getZ());
            }
        }
    }

    @EventHandler
    public void onPortalEnter(PlayerPortalEvent event) {
        if (!isApplied) return;

        Player player = event.getPlayer();
        if (event.getCause() != PlayerTeleportEvent.TeleportCause.NETHER_PORTAL) return;

        if (event.getTo().getWorld().getEnvironment() == World.Environment.NETHER) {
            if (!hasItem(player, EmblemItems.createNetherEmblem())) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "۞ Necesitas un Nether Emblem para entrar al Nether!");
            }
        } else if (event.getTo().getWorld().getEnvironment() == World.Environment.NORMAL) {
            if (!hasItem(player, EmblemItems.createOverworldEmblem())) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "۞ Necesitas un Overworld Emblem para regresar al Overworld!");
            }
        }
    }

    public static boolean hasItem(Player player, ItemStack target) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.isSimilar(target)) {
                return true;
            }
        }
        return false;
    }

    //MOBS
    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (!isApplied) return;

        // Primero manejar la lógica de Wither Skeletons (existente)
        if (shouldConvertWitherSpawn(event)) {
            WitherSkeleton skeleton = (WitherSkeleton) event.getEntity();
            convertToCorruptedSkeleton(skeleton);
            return;
        }

        handlePiglinToSpiderConversion(event);
    }

    private void handlePiglinToSpiderConversion(CreatureSpawnEvent event) {
        if (event.getLocation().getWorld().getEnvironment() != World.Environment.NETHER) {
            return;
        }

        if (event.getEntityType() != EntityType.ZOMBIFIED_PIGLIN) return;

        // Verificar que no sea ya una araña corrupta
        if (event.getEntity().getPersistentDataContainer()
                .has(corruptedInfernalSpider.getCorruptedInfernalKey(), PersistentDataType.BYTE)) {
            return;
        }

        if (random.nextInt(10) != 0) return;

        // Convertir Piglin a Spider
        PigZombie pigZombie = (PigZombie) event.getEntity();
        Location loc = pigZombie.getLocation();

        corruptedInfernalSpider.spawnCorruptedInfernalSpider(loc);
        pigZombie.remove();
    }

    private boolean shouldConvertWitherSpawn(CreatureSpawnEvent event) {
        return isApplied &&
                event.getEntityType() == EntityType.WITHER_SKELETON &&
                !event.getEntity().getPersistentDataContainer()
                        .has(guardianCorruptedSkeleton.getGCSkeletonKey(), PersistentDataType.BYTE);
    }

    private void convertToCorruptedSkeleton(WitherSkeleton skeleton) {
        Location loc = skeleton.getLocation();
        loc.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, loc, 20, 0.5, 0.5, 0.5);

        guardianCorruptedSkeleton.transformToCorruptedSkeleton(skeleton);
    }

    @EventHandler
    public void onWitherSkeletonDeath(EntityDeathEvent event) {
        if (!isApplied) return;

        if (event.getEntityType() == EntityType.WITHER_SKELETON) {
            WitherSkeleton skeleton = (WitherSkeleton) event.getEntity();
            PersistentDataContainer data = skeleton.getPersistentDataContainer();

            if (!data.has(guardianCorruptedSkeleton.getGCSkeletonKey(), PersistentDataType.BYTE)) {
                event.getDrops().removeIf(item -> item.getType() == Material.WITHER_SKELETON_SKULL);
            }
        }
    }
}