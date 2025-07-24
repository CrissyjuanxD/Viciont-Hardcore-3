package Dificultades;

import Armors.CorruptedArmor;
import Blocks.Endstalactitas;
import Dificultades.CustomMobs.Bombita;
import Dificultades.CustomMobs.CorruptedCreeper;
import Dificultades.CustomMobs.GuardianShulker;
import Dificultades.CustomMobs.Iceologer;
import Handlers.DayHandler;
import items.BlazeItems;
import items.CorruptedNetheriteItems;
import items.EndItems;
import items.EnderiteTools;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.*;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.inventory.FurnaceBurnEvent;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.event.inventory.FurnaceStartSmeltEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

public class DayFourteenChanges implements Listener {
    private final JavaPlugin plugin;
    private boolean isApplied = false;
    private final DayHandler dayHandler;
    private final GuardianShulker guardianShulker;
    private final Iceologer iceologer;
    private final Random random = new Random();
    private final Map<Location, Long> altarCooldowns = new HashMap<>();

    public DayFourteenChanges(JavaPlugin plugin, DayHandler handler) {
        this.plugin = plugin;
        this.dayHandler = handler;
        this.guardianShulker = new GuardianShulker(plugin);
        this.iceologer = new Iceologer(plugin);
    }

    public void apply() {
        if (!isApplied) {
            isApplied = true;
            Bukkit.getPluginManager().registerEvents(this, plugin);
            guardianShulker.apply();
            registerEndRecipes();
            registerEnderiteToolsUpgrades();

            new BukkitRunnable() {
                @Override
                public void run() {
                    if (!isApplied) {
                        this.cancel();
                        return;
                    }

                    for (Player player : Bukkit.getOnlinePlayers()) {
                        World world = player.getWorld();
                        Location playerLoc = player.getLocation();

                        world.getNearbyEntities(playerLoc, 60, 60, 60, entity ->
                                entity.getType() == EntityType.COW
                        ).forEach(entity -> {
                            Location loc = entity.getLocation();
                            entity.remove();
                            iceologer.spawnIceologer(loc);
                        });
                    }
                }
            }.runTaskTimer(plugin, 0L, 80L);
        }
    }

    public void revert() {
        if (isApplied) {
            isApplied = false;
            guardianShulker.revert();

            //remover crafteos
            Bukkit.removeRecipe(new NamespacedKey(plugin, "endstalactita_item"));
            Bukkit.removeRecipe(new NamespacedKey(plugin, "fragmento_enderite_item"));
            Bukkit.removeRecipe(new NamespacedKey(plugin, "plantilla_enderite_item"));
            Bukkit.removeRecipe(new NamespacedKey(plugin, "enderite_sword_upgrade"));
            Bukkit.removeRecipe(new NamespacedKey(plugin, "enderite_axe_upgrade"));
            Bukkit.removeRecipe(new NamespacedKey(plugin, "enderite_pickaxe_upgrade"));
            Bukkit.removeRecipe(new NamespacedKey(plugin, "enderite_shovel_upgrade"));
            Bukkit.removeRecipe(new NamespacedKey(plugin, "enderite_hoe_upgrade"));

            HandlerList.unregisterAll(this);
        }
    }

    //SPAWNS
    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (!isApplied) return;

        Entity entity = event.getEntity();

        if (entity.getType() == EntityType.COW) {
            Location loc = entity.getLocation();
            entity.remove();
            iceologer.spawnIceologer(loc);
        }

        handleGuardianShulkerConversion(event);
    }

    private void handleGuardianShulkerConversion(CreatureSpawnEvent event) {
        if (event.getLocation().getWorld().getEnvironment() != World.Environment.THE_END) {
            return;
        }

        if (event.getEntityType() != EntityType.SHULKER) return;

        if (event.getEntity().getPersistentDataContainer()
                .has(guardianShulker.getGuardianShulkerKey(), PersistentDataType.BYTE)) {
            return;
        }

        if (random.nextInt(50) != 0) return;

        Shulker shulker = (Shulker) event.getEntity();
        Location loc = shulker.getLocation();

        guardianShulker.spawnGuardianShulker(loc);
        shulker.remove();

    }

    //CRAFTEOS

    public void registerEndRecipes() {

        ShapedRecipe endstalactitaItem = new ShapedRecipe(new NamespacedKey(plugin, "endstalactita_item"), Endstalactitas.createEndstalactita());
        endstalactitaItem.shape("EAE", "EAE", "EAE");
        endstalactitaItem.setIngredient('E', new RecipeChoice.ExactChoice(EndItems.createEnderiteNugget(1)));
        endstalactitaItem.setIngredient('A', new RecipeChoice.ExactChoice(EndItems.createEndAmatist(1)));

        ShapedRecipe fragmentoEnderiteItem = new ShapedRecipe(new NamespacedKey(plugin, "fragmento_enderite_item"), EndItems.createFragmentoEnderite());
        fragmentoEnderiteItem.shape("ESE", "EGE", "ESE");
        fragmentoEnderiteItem.setIngredient('E', new RecipeChoice.ExactChoice(Endstalactitas.createEndstalactita()));
        fragmentoEnderiteItem.setIngredient('S', Material.SHULKER_SHELL);
        fragmentoEnderiteItem.setIngredient('G', new RecipeChoice.ExactChoice(EndItems.createGuardianShulkerShell()));

        //Plantilla
        ShapedRecipe plantillaEnderiteItem = new ShapedRecipe(new NamespacedKey(plugin, "plantilla_enderite_item"), EndItems.createEnderiteUpgrades());
        plantillaEnderiteItem.shape("DAD", "ENE", " C ");
        plantillaEnderiteItem.setIngredient('D', Material.DIAMOND_BLOCK);
        plantillaEnderiteItem.setIngredient('A', Material.END_CRYSTAL);
        plantillaEnderiteItem.setIngredient('N', Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE);
        plantillaEnderiteItem.setIngredient('E', new RecipeChoice.ExactChoice(EndItems.createIngotEnderite()));
        plantillaEnderiteItem.setIngredient('C', new RecipeChoice.ExactChoice(CorruptedNetheriteItems.createCorruptedNetheriteIngot()));

        Bukkit.addRecipe(endstalactitaItem);
        Bukkit.addRecipe(fragmentoEnderiteItem);
        Bukkit.addRecipe(plantillaEnderiteItem);

    }

    private void registerEnderiteToolsUpgrades() {

        SmithingTransformRecipe enderiteSwordRecipe = new SmithingTransformRecipe(
                new NamespacedKey(plugin, "enderite_sword_upgrade"),
                EnderiteTools.createEnderiteSword(),
                new RecipeChoice.ExactChoice(EndItems.createEnderiteUpgrades()),
                new RecipeChoice.MaterialChoice(Material.NETHERITE_SWORD),
                new RecipeChoice.ExactChoice(Endstalactitas.createEndstalactita())
        );

        SmithingTransformRecipe enderiteAxeRecipe = new SmithingTransformRecipe(
                new NamespacedKey(plugin, "enderite_axe_upgrade"),
                EnderiteTools.createEnderiteAxe(),
                new RecipeChoice.ExactChoice(EndItems.createEnderiteUpgrades()),
                new RecipeChoice.MaterialChoice(Material.NETHERITE_AXE),
                new RecipeChoice.ExactChoice(Endstalactitas.createEndstalactita())
        );

        SmithingTransformRecipe enderitePickaxeRecipe = new SmithingTransformRecipe(
                new NamespacedKey(plugin, "enderite_pickaxe_upgrade"),
                CorruptedArmor.createCorruptedLeggings(),
                new RecipeChoice.ExactChoice(EndItems.createEnderiteUpgrades()),
                new RecipeChoice.MaterialChoice(Material.NETHERITE_PICKAXE),
                new RecipeChoice.ExactChoice(Endstalactitas.createEndstalactita())
        );

        SmithingTransformRecipe enderiteShovelRecipe = new SmithingTransformRecipe(
                new NamespacedKey(plugin, "enderite_shovel_upgrade"),
                EnderiteTools.createEnderiteShovel(),
                new RecipeChoice.ExactChoice(EndItems.createEnderiteUpgrades()),
                new RecipeChoice.MaterialChoice(Material.NETHERITE_SHOVEL),
                new RecipeChoice.ExactChoice(Endstalactitas.createEndstalactita())
        );

        SmithingTransformRecipe enderiteHoeRecipe = new SmithingTransformRecipe(
                new NamespacedKey(plugin, "enderite_hoe_upgrade"),
                EnderiteTools.createEnderiteHoe(),
                new RecipeChoice.ExactChoice(EndItems.createEnderiteUpgrades()),
                new RecipeChoice.MaterialChoice(Material.NETHERITE_HOE),
                new RecipeChoice.ExactChoice(Endstalactitas.createEndstalactita())
        );

        Bukkit.addRecipe(enderiteSwordRecipe);
        Bukkit.addRecipe(enderiteAxeRecipe);
        Bukkit.addRecipe(enderitePickaxeRecipe);
        Bukkit.addRecipe(enderiteShovelRecipe);
        Bukkit.addRecipe(enderiteHoeRecipe);
    }

    private boolean isBlastFurnace(Block block) {
        return block.getType() == Material.BLAST_FURNACE;
    }

    @EventHandler
    public void onFurnaceStartSmelt(FurnaceStartSmeltEvent event) {
        if (!isApplied) return;

        // Solo procesar en blast furnace
        if (isBlastFurnace(event.getBlock()) &&
                event.getSource().isSimilar(EndItems.createFragmentoEnderite())) {
            // Configurar tiempo de cocción extendido (4 minutos = 4800 ticks)
            event.setTotalCookTime(4800);

            // Efectos especiales
            Location loc = event.getBlock().getLocation().add(0.5, 0.5, 0.5);
            loc.getWorld().playSound(loc, Sound.BLOCK_BLASTFURNACE_FIRE_CRACKLE, 1.0f, 1.5f);
            loc.getWorld().spawnParticle(Particle.TRIAL_SPAWNER_DETECTION_OMINOUS, loc, 5, 0.3, 0.2, 0.3, 0.02);
        }
    }

    @EventHandler
    public void onFurnaceSmelt(FurnaceSmeltEvent event) {
        if (!isApplied) return;

        // Solo procesar en blast furnace
        if (isBlastFurnace(event.getBlock()) &&
                event.getSource().isSimilar(EndItems.createFragmentoEnderite())) {
            // Establecer el resultado manualmente
            event.setResult(EndItems.createIngotEnderite());

            // Efectos de finalización
            Location loc = event.getBlock().getLocation().add(0.5, 0.5, 0.5);
            loc.getWorld().playSound(loc, Sound.BLOCK_FIRE_EXTINGUISH, 0.8f, 1.2f);
            loc.getWorld().spawnParticle(Particle.DRAGON_BREATH, loc, 30, 0.3, 0.2, 0.3, 0.1);
            loc.getWorld().spawn(loc, ExperienceOrb.class).setExperience(25);
        }
    }

    @EventHandler
    public void onOtherFurnaceAttempt(FurnaceSmeltEvent event) {
        if (!isApplied) return;

        if (event.getSource().isSimilar(EndItems.createFragmentoEnderite()) &&
                !isBlastFurnace(event.getBlock())) {
            event.setCancelled(true);
        }
    }

    //CAMBIOS

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();

        if (block.getType() != Material.AMETHYST_CLUSTER) return;

        if (block.getWorld().getEnvironment() == World.Environment.THE_END) {
            event.setDropItems(false);

            int amount = random.nextInt(3) + 1;
            block.getWorld().dropItemNaturally(block.getLocation(), EndItems.createEndAmatist(amount));

        }
    }

    // ALTAR GUARDIAN SHULKER

    // Definir las capas del altar
    private final Material[][][] altarLayers = {
            // Layer 1
            {
                    {Material.HONEYCOMB_BLOCK, Material.PURPUR_BLOCK, Material.PURPUR_BLOCK, Material.PURPUR_BLOCK, Material.HONEYCOMB_BLOCK},
                    {Material.PURPUR_BLOCK, Material.PURPUR_BLOCK, Material.HONEYCOMB_BLOCK, Material.PURPUR_BLOCK, Material.PURPUR_BLOCK},
                    {Material.PURPUR_BLOCK, Material.HONEYCOMB_BLOCK, Material.PURPUR_BLOCK, Material.HONEYCOMB_BLOCK, Material.PURPUR_BLOCK},
                    {Material.PURPUR_BLOCK, Material.PURPUR_BLOCK, Material.HONEYCOMB_BLOCK, Material.PURPUR_BLOCK, Material.PURPUR_BLOCK},
                    {Material.HONEYCOMB_BLOCK, Material.PURPUR_BLOCK, Material.PURPUR_BLOCK, Material.PURPUR_BLOCK, Material.HONEYCOMB_BLOCK}
            },

            // Layer 2
            {
                    {Material.AIR, Material.PURPUR_PILLAR, Material.AIR, Material.PURPUR_PILLAR, Material.AIR},
                    {Material.PURPUR_PILLAR, Material.END_ROD, Material.AIR, Material.END_ROD, Material.PURPUR_PILLAR},
                    {Material.AIR, Material.AIR, Material.END_ROD, Material.AIR, Material.AIR},
                    {Material.PURPUR_PILLAR, Material.END_ROD, Material.AIR, Material.END_ROD, Material.PURPUR_PILLAR},
                    {Material.AIR, Material.PURPUR_PILLAR, Material.AIR, Material.PURPUR_PILLAR, Material.AIR}
            },

            // Layer 3
            {
                    {Material.AIR, Material.PURPUR_PILLAR, Material.AIR, Material.PURPUR_PILLAR, Material.AIR},
                    {Material.PURPUR_PILLAR, Material.AIR, Material.AIR, Material.AIR, Material.PURPUR_PILLAR},
                    {Material.AIR, Material.AIR, Material.END_ROD, Material.AIR, Material.AIR},
                    {Material.PURPUR_PILLAR, Material.AIR, Material.AIR, Material.AIR, Material.PURPUR_PILLAR},
                    {Material.AIR, Material.PURPUR_PILLAR, Material.AIR, Material.PURPUR_PILLAR, Material.AIR}
            },

            // Layer 4
            {
                    {Material.AIR, Material.AIR, Material.AIR, Material.AIR, Material.AIR},
                    {Material.AIR, Material.AIR, Material.AIR, Material.AIR, Material.AIR},
                    {Material.AIR, Material.AIR, Material.PURPLE_GLAZED_TERRACOTTA, Material.AIR, Material.AIR},
                    {Material.AIR, Material.AIR, Material.AIR, Material.AIR, Material.AIR},
                    {Material.AIR, Material.AIR, Material.AIR, Material.AIR, Material.AIR}
            }
    };

    private void checkAltarLayers(Location location, Player player) {
        int baseY = location.getBlockY();

        for (int y = 0; y < altarLayers.length; y++) {
            player.sendMessage(net.md_5.bungee.api.ChatColor.GOLD + "Layer " + (y + 1) + ":");
            StringBuilder layerOutput = new StringBuilder();

            for (int x = -2; x <= 2; x++) {
                for (int z = -2; z <= 2; z++) {
                    Material expected = altarLayers[y][x + 2][z + 2];
                    Material actual = Objects.requireNonNull(location.getWorld()).getBlockAt(
                            location.getBlockX() + x,
                            baseY - 3 + y,
                            location.getBlockZ() + z
                    ).getType();

                    if (expected == actual) {
                        layerOutput.append(net.md_5.bungee.api.ChatColor.GREEN).append(getSymbol(expected)).append(" ");
                    } else {
                        layerOutput.append(net.md_5.bungee.api.ChatColor.RED).append(getSymbol(expected)).append(" ");
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
            case PURPUR_PILLAR -> {
                return "PP";
            }
            case HONEYCOMB_BLOCK -> {
                return "HC";
            }
            case END_ROD -> {
                return "ER";
            }
            case PURPLE_GLAZED_TERRACOTTA -> {
                return "G";
            }
            case PURPUR_BLOCK -> {
                return "P";
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

            if (block != null && block.getType() == Material.PURPLE_GLAZED_TERRACOTTA) {
                if (isOnCooldown(block.getLocation())) return;
                Location altarLocation = block.getLocation();

                // Verificar y enviar el mensaje de error por capas
                checkAltarLayers(altarLocation, player);

                if (isValidGuardianShulkerAltar(altarLocation)) {
                    if (player.getPotionEffect(PotionEffectType.BAD_OMEN) != null) {
                        if (isGuardianShulkerSpawned(altarLocation)) {
                            player.sendMessage(net.md_5.bungee.api.ChatColor.RED + "۞ Ya se ha invocado una Reina Abeja en este altar.");
                        } else {
                            spawnGuardianShulker(altarLocation);
                            player.removePotionEffect(PotionEffectType.BAD_OMEN);
                            int radius = 50;
                            for (Player nearbyPlayer : Bukkit.getOnlinePlayers()) {
                                if (nearbyPlayer.getWorld().equals(altarLocation.getWorld()) &&
                                        nearbyPlayer.getLocation().distanceSquared(altarLocation) <= radius * radius) {

                                    nearbyPlayer.playSound(altarLocation, Sound.ENTITY_SHULKER_DEATH, 5.0f, 0.1f);
                                    nearbyPlayer.playSound(altarLocation, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 5.0f, 0.1f);
                                    nearbyPlayer.playSound(altarLocation, "minecraft:custom.emuerte" , 5.0f, 0.1f);
                                }
                            }
                        }
                    } else {
                        player.sendMessage(ChatColor.RED + "۞ Necesitas el efecto Bad Omen para invocar al Guardian Shulker.");
                    }
                }
            }
        }
    }

    private boolean isValidGuardianShulkerAltar(Location location) {
        int baseY = location.getBlockY();

        for (int y = 0; y < altarLayers.length; y++) {
            for (int x = -2; x <= 2; x++) {
                for (int z = -2; z <= 2; z++) {
                    Material expected = altarLayers[y][x + 2][z + 2];
                    Material actual = Objects.requireNonNull(location.getWorld()).getBlockAt(
                            location.getBlockX() + x,
                            baseY - 3 + y,
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


    private boolean isGuardianShulkerSpawned(Location altarLocation) {
        for (Entity entity : Objects.requireNonNull(altarLocation.getWorld()).getNearbyEntities(altarLocation, 50, 50, 50)) {
            if (entity instanceof Shulker shulker && "Guardian Shulker".equals(shulker.getCustomName())) {
                return true;
            }
        }
        return false;
    }

    private void spawnGuardianShulker(Location altarLocation) {
        World world = altarLocation.getWorld();
        if (world == null) return;

        Location center = altarLocation.clone().add(0.5, 0.5, 0.5);
        int totalSteps = 100;
        int finalY = -2;

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
                        world.playSound(center, Sound.ENTITY_SHULKER_SHOOT, 3.0f, 0.5f + (float)progress);
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

                    guardianShulker.spawnGuardianShulker(spawnLocation);

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
                            baseY - 3 + y,
                            baseLocation.getBlockZ() + z
                    );
                    world.getBlockAt(blockLoc).setType(Material.AIR);
                }
            }
        }
    }

}
