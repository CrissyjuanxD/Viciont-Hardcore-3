package Dificultades;

import Armors.CorruptedArmor;
import Blocks.CorruptedAncientDebris;
import Dificultades.CustomMobs.*;
import Dificultades.Features.MobCapManager;
import Enchants.EssenceFactory;
import Handlers.DayHandler;
import items.*;
import org.bukkit.*;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.Campfire;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.event.inventory.FurnaceStartSmeltEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.*;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import net.md_5.bungee.api.ChatColor;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class DayEightChanges implements Listener {
    private final DayHandler dayHandler;
    private final JavaPlugin plugin;
    private final MobCapManager mobCapManager;
    private final Random random = new Random();
    private boolean isApplied = false;

    private final CorruptedMagmaCube corruptedMagmaCube;
    private final BuffBreeze buffBreeze;
    private final InvertedGhast invertedGhast;
    private final NetheriteVexGuardian netheriteVexGuardian;
    private final CorruptedZombies corruptedZombies;
    private final CorruptedSkeleton corruptedSkeleton;
    private final BruteImperial bruteImperial;

    private final CorruptedSoul corruptedSoul;
    private final CorruptedAncientDebris corruptedAncientDebris;
    //Armor
    private final HelmetNetheriteEssence helmetNetheriteEssence;
    private final ChestplateNetheriteEssence chestplateNetheriteEssence;
    private final LegginsNetheriteEssence leggingsNetheriteEssence;
    private final BootNetheriteEssence bootsNetheriteEssence;
    private final CorruptedUpgrades corruptedUpgrades;

    private final CorruptedArmor corruptedArmor;

    public DayEightChanges(JavaPlugin plugin, DayHandler handler) {
        this.plugin = plugin;
        this.dayHandler = handler;
        this.corruptedMagmaCube = new CorruptedMagmaCube(plugin);;
        this.buffBreeze = new BuffBreeze(plugin);
        this.invertedGhast = new InvertedGhast(plugin);
        this.netheriteVexGuardian = new NetheriteVexGuardian(plugin);
        this.corruptedZombies = new CorruptedZombies(plugin);
        this.corruptedSkeleton = new CorruptedSkeleton(plugin, handler);
        this.bruteImperial = new BruteImperial(plugin);
        this.corruptedSoul = new CorruptedSoul(plugin);
        this.corruptedAncientDebris = new CorruptedAncientDebris(plugin);
        //Armor
        this.helmetNetheriteEssence = new HelmetNetheriteEssence(plugin);
        this.chestplateNetheriteEssence = new ChestplateNetheriteEssence(plugin);
        this.leggingsNetheriteEssence = new LegginsNetheriteEssence(plugin);
        this.bootsNetheriteEssence = new BootNetheriteEssence(plugin);
        this.corruptedUpgrades = new CorruptedUpgrades(plugin);
        this.corruptedArmor = new CorruptedArmor(plugin);

        this.mobCapManager = MobCapManager.getInstance(plugin);
    }

    public void apply() {
        if (!isApplied) {
            isApplied = true;
            Bukkit.getPluginManager().registerEvents(this, plugin);
            corruptedMagmaCube.apply();
            buffBreeze.apply();
            netheriteVexGuardian.apply();
            bruteImperial.apply();
            CustomCorruptedNetheriteCraft();
            registerCorruptedArmorUpgrades();
            mobCapManager.updateMobCap(2);
        }
    }

    public void revert() {
        if (isApplied) {
            isApplied = false;
            corruptedMagmaCube.revert();
            buffBreeze.revert();
            netheriteVexGuardian.revert();
            bruteImperial.revert();

            Bukkit.removeRecipe(new NamespacedKey(plugin, "corrupted_netherite_ingot"));
            Bukkit.removeRecipe(new NamespacedKey(plugin, "helmet_netherite_upgrade"));
            Bukkit.removeRecipe(new NamespacedKey(plugin, "chestplate_netherite_upgrade"));
            Bukkit.removeRecipe(new NamespacedKey(plugin, "leggins_netherite_upgrade"));
            Bukkit.removeRecipe(new NamespacedKey(plugin, "boots_netherite_upgrade"));
            Bukkit.removeRecipe(new NamespacedKey(plugin, "corrupted_soul"));

            Bukkit.removeRecipe(new NamespacedKey(plugin, "corrupted_helmet_upgrade"));
            Bukkit.removeRecipe(new NamespacedKey(plugin, "corrupted_chestplate_upgrade"));
            Bukkit.removeRecipe(new NamespacedKey(plugin, "corrupted_leggings_upgrade"));
            Bukkit.removeRecipe(new NamespacedKey(plugin, "corrupted_boots_upgrade"));

            HandlerList.unregisterAll(this);
        }
    }

    //SPAWNS
    //Pigling Globo
    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (!isApplied) return;

        if (shouldConvertImperialBruteSpawn(event)) {
            PiglinBrute brute = (PiglinBrute) event.getEntity();
            convertToImperialBrute(brute);
            return;
        }

        handleCorruptedMagmaConversion(event);
        handleBuffBreezeConversion(event);
        handleStraytoCEConversion(event);
        handleBoggedtoCEConversion(event);
        handleHusktoCZConversion(event);
        handleBruteonversion(event);
    }

    private void handleCorruptedMagmaConversion(CreatureSpawnEvent event) {
        if (event.getLocation().getWorld().getEnvironment() != World.Environment.NETHER) {
            return;
        }

        if (event.getLocation().getBlock().getBiome() != Biome.BASALT_DELTAS) {
            return;
        }

        if (event.getEntityType() != EntityType.MAGMA_CUBE) return;

        if (event.getEntity().getPersistentDataContainer()
                .has(corruptedMagmaCube.getMagmaCorruptedKey(), PersistentDataType.BYTE)) {
            return;
        }

        if (random.nextInt(4) != 0) return;

        MagmaCube magmaCube = (MagmaCube) event.getEntity();
        Location loc = magmaCube.getLocation();

        corruptedMagmaCube.spawnCorruptedMagmaCube(loc);
        magmaCube.remove();

    }

    private void handleBuffBreezeConversion(CreatureSpawnEvent event) {

        if (event.getLocation().getWorld().getEnvironment() != World.Environment.NORMAL) {
            return;
        }

        if (event.getEntityType() != EntityType.BREEZE) return;

        if (event.getEntity().getPersistentDataContainer()
                .has(buffBreeze.getBuffBreezeKey(), PersistentDataType.BYTE)) {
            return;
        }

        if (random.nextInt(3) != 0) return;

        Breeze breeze = (Breeze) event.getEntity();
        Location loc = breeze.getLocation();

        buffBreeze.spawnBuffBreeze(loc);
        breeze.remove();
    }

    private void handleStraytoCEConversion(CreatureSpawnEvent event) {
        if (event.getEntityType() != EntityType.STRAY) return;

        if (event.getEntity().getPersistentDataContainer()
                .has(corruptedSkeleton.getCorruptedKey(), PersistentDataType.BYTE)) {
            return;
        }

        if (random.nextInt(3) != 0) return;

        Stray stray = (Stray) event.getEntity();
        Location loc = stray.getLocation();

        corruptedSkeleton.spawnCorruptedSkeleton(loc, null);
        stray.remove();
    }

    private void handleBoggedtoCEConversion(CreatureSpawnEvent event) {
        if (event.getEntityType() != EntityType.BOGGED) return;

        if (event.getEntity().getPersistentDataContainer()
                .has(corruptedSkeleton.getCorruptedKey(), PersistentDataType.BYTE)) {
            return;
        }

        if (random.nextInt(4) != 0) return;

        Bogged bogged = (Bogged) event.getEntity();
        Location loc = bogged.getLocation();

        corruptedSkeleton.spawnCorruptedSkeleton(loc, null);
        bogged.remove();
    }

    private void handleHusktoCZConversion(CreatureSpawnEvent event) {
        if (event.getEntityType() != EntityType.HUSK) return;

        if (event.getEntity().getPersistentDataContainer()
                .has(corruptedZombies.getCorruptedKey(), PersistentDataType.BYTE)) {
            return;
        }

        if (random.nextInt(3) != 0) return;

        Husk husk = (Husk) event.getEntity();
        Location loc = husk.getLocation();

        corruptedZombies.spawnCorruptedZombie(loc);
        husk.remove();
    }

    private void handleBruteonversion(CreatureSpawnEvent event) {
        if (event.getEntityType() != EntityType.PIGLIN) return;

        if (event.getEntity().getPersistentDataContainer()
                .has(bruteImperial.getBruteImperialKey(), PersistentDataType.BYTE)) {
            return;
        }

        if (random.nextInt(25) != 0) return;

        Piglin piglin = (Piglin) event.getEntity();
        Location loc = piglin.getLocation();

        bruteImperial.spawnBruteImperial(loc);
        piglin.remove();
    }

    private boolean shouldConvertImperialBruteSpawn(CreatureSpawnEvent event) {
        return isApplied &&
                event.getEntityType() == EntityType.PIGLIN_BRUTE &&
                !event.getEntity().getPersistentDataContainer()
                        .has(bruteImperial.getBruteImperialKey(), PersistentDataType.BYTE);
    }

    private void convertToImperialBrute(PiglinBrute brute) {
        Location loc = brute.getLocation();
        bruteImperial.transformToBruteImperial(brute);
    }

    //Crafteos

    private void CustomCorruptedNetheriteCraft() {

        //Corrupted Netherite Ingot
        ShapedRecipe corruptedNetheriteIngot = new ShapedRecipe(new NamespacedKey(plugin, "corrupted_netherite_ingot"), CorruptedNetheriteItems.createCorruptedNetheriteIngot());
        corruptedNetheriteIngot.shape("SSG", "SSS", "GSG");
        corruptedNetheriteIngot.setIngredient('S', new RecipeChoice.ExactChoice(CorruptedNetheriteItems.createCorruptedScrapNetherite()));
        corruptedNetheriteIngot.setIngredient('G', Material.GOLD_BLOCK);

        //HelmetNetheriteUpgrade
        ShapedRecipe helmetNetheriteUpgrade = new ShapedRecipe(new NamespacedKey(plugin, "helmet_netherite_upgrade"), corruptedUpgrades.createHelmetNetheriteUpgrade());
        helmetNetheriteUpgrade.shape("   ", "CNC", " E ");
        helmetNetheriteUpgrade.setIngredient('C', new RecipeChoice.ExactChoice(CorruptedNetheriteItems.createCorruptedNetheriteIngot()));
        helmetNetheriteUpgrade.setIngredient('E', new RecipeChoice.ExactChoice(helmetNetheriteEssence.createHelmetNetheriteEssence()));
        helmetNetheriteUpgrade.setIngredient('N', Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE);

        //ChestplateNetheriteUpgrade
        ShapedRecipe chestplateNetheriteUpgrade = new ShapedRecipe(new NamespacedKey(plugin, "chestplate_netherite_upgrade"), corruptedUpgrades.createChestplateNetheriteUpgrade());
        chestplateNetheriteUpgrade.shape("   ", "CNC", " E ");
        chestplateNetheriteUpgrade.setIngredient('C', new RecipeChoice.ExactChoice(CorruptedNetheriteItems.createCorruptedNetheriteIngot()));
        chestplateNetheriteUpgrade.setIngredient('E', new RecipeChoice.ExactChoice(chestplateNetheriteEssence.createChestplateNetheriteEssence()));
        chestplateNetheriteUpgrade.setIngredient('N', Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE);

        //LegginsNetheriteUpgrade
        ShapedRecipe legginsNetheriteUpgrade = new ShapedRecipe(new NamespacedKey(plugin, "leggins_netherite_upgrade"), corruptedUpgrades.createLeggingsNetheriteUpgrade());
        legginsNetheriteUpgrade.shape("   ", "CNC", " E ");
        legginsNetheriteUpgrade.setIngredient('C', new RecipeChoice.ExactChoice(CorruptedNetheriteItems.createCorruptedNetheriteIngot()));
        legginsNetheriteUpgrade.setIngredient('E', new RecipeChoice.ExactChoice(leggingsNetheriteEssence.createLegginsNetheriteEssence()));
        legginsNetheriteUpgrade.setIngredient('N', Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE);

        //BootsNetheriteUpgrade
        ShapedRecipe bootsNetheriteUpgrade = new ShapedRecipe(new NamespacedKey(plugin, "boots_netherite_upgrade"), corruptedUpgrades.createBootsNetheriteUpgrade());
        bootsNetheriteUpgrade.shape("   ", "CNC", " E ");
        bootsNetheriteUpgrade.setIngredient('C', new RecipeChoice.ExactChoice(CorruptedNetheriteItems.createCorruptedNetheriteIngot()));
        bootsNetheriteUpgrade.setIngredient('E', new RecipeChoice.ExactChoice(bootsNetheriteEssence.createBootNetheriteEssence()));
        bootsNetheriteUpgrade.setIngredient('N', Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE);

        //CORRUPTED SOUL
        ShapedRecipe ItemcorruptedSoul = new ShapedRecipe(new NamespacedKey(plugin, "corrupted_soul"), corruptedSoul.createCorruptedSoulEssence());
        ItemcorruptedSoul.shape("BRB", "BVB", "PBE");
        ItemcorruptedSoul.setIngredient('B', Material.BONE);
        ItemcorruptedSoul.setIngredient('R', new RecipeChoice.ExactChoice(CorruptedMobItems.createCorruptedMeet()));
        ItemcorruptedSoul.setIngredient('V', new RecipeChoice.ExactChoice(EssenceFactory.createVoidEssence()));
        ItemcorruptedSoul.setIngredient('P', new RecipeChoice.ExactChoice(CorruptedMobItems.createCorruptedPowder()));
        ItemcorruptedSoul.setIngredient('E', new RecipeChoice.ExactChoice(CorruptedMobItems.createCorruptedSpiderEye()));

        Bukkit.addRecipe(corruptedNetheriteIngot);
        Bukkit.addRecipe(helmetNetheriteUpgrade);
        Bukkit.addRecipe(chestplateNetheriteUpgrade);
        Bukkit.addRecipe(legginsNetheriteUpgrade);
        Bukkit.addRecipe(bootsNetheriteUpgrade);
        Bukkit.addRecipe(ItemcorruptedSoul);
    }

    private void registerCorruptedArmorUpgrades() {
        // Helmet upgrade
        SmithingTransformRecipe corruptedHelmetRecipe = new SmithingTransformRecipe(
                new NamespacedKey(plugin, "corrupted_helmet_upgrade"),
                CorruptedArmor.createCorruptedHelmet(),
                new RecipeChoice.ExactChoice(corruptedUpgrades.createHelmetNetheriteUpgrade()),
                new RecipeChoice.MaterialChoice(Material.NETHERITE_HELMET),
                new RecipeChoice.ExactChoice(CorruptedNetheriteItems.createCorruptedNetheriteIngot())
        );

        // Chestplate upgrade
        SmithingTransformRecipe corruptedChestplateRecipe = new SmithingTransformRecipe(
                new NamespacedKey(plugin, "corrupted_chestplate_upgrade"),
                CorruptedArmor.createCorruptedChestplate(),
                new RecipeChoice.ExactChoice(corruptedUpgrades.createChestplateNetheriteUpgrade()),
                new RecipeChoice.MaterialChoice(Material.NETHERITE_CHESTPLATE),
                new RecipeChoice.ExactChoice(CorruptedNetheriteItems.createCorruptedNetheriteIngot())
        );

        // Leggings upgrade
        SmithingTransformRecipe corruptedLeggingsRecipe = new SmithingTransformRecipe(
                new NamespacedKey(plugin, "corrupted_leggings_upgrade"),
                CorruptedArmor.createCorruptedLeggings(),
                new RecipeChoice.ExactChoice(corruptedUpgrades.createLeggingsNetheriteUpgrade()),
                new RecipeChoice.MaterialChoice(Material.NETHERITE_LEGGINGS),
                new RecipeChoice.ExactChoice(CorruptedNetheriteItems.createCorruptedNetheriteIngot())
        );

        // Boots upgrade
        SmithingTransformRecipe corruptedBootsRecipe = new SmithingTransformRecipe(
                new NamespacedKey(plugin, "corrupted_boots_upgrade"),
                CorruptedArmor.createCorruptedBoots(),
                new RecipeChoice.ExactChoice(corruptedUpgrades.createBootsNetheriteUpgrade()),
                new RecipeChoice.MaterialChoice(Material.NETHERITE_BOOTS),
                new RecipeChoice.ExactChoice(CorruptedNetheriteItems.createCorruptedNetheriteIngot())
        );

        // Register all recipes
        Bukkit.addRecipe(corruptedHelmetRecipe);
        Bukkit.addRecipe(corruptedChestplateRecipe);
        Bukkit.addRecipe(corruptedLeggingsRecipe);
        Bukkit.addRecipe(corruptedBootsRecipe);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (!isApplied) return;
        if (event.getBlock().getType() == Material.ANCIENT_DEBRIS) {
            event.setDropItems(false);

            ItemStack corruptedancientdebris = corruptedAncientDebris.createcorruptedancientdebris();
            event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), corruptedancientdebris);

            if (random.nextDouble() <= 0.20) {
                Location loc = event.getBlock().getLocation();
                netheriteVexGuardian.spawnNetheriteVexGuardian(loc);
            }
        }
    }

    private boolean isBlastFurnace(Block block) {
        return block.getType() == Material.BLAST_FURNACE;
    }

    // Evento para modificar el tiempo de cocción cuando empieza
    @EventHandler
    public void onFurnaceStartSmelt(FurnaceStartSmeltEvent event) {
        if (!isApplied) return;

        // Verificar si es Corrupted Ancient Debris en un alto horno
        if (isBlastFurnace(event.getBlock()) &&
                event.getSource().isSimilar(corruptedAncientDebris.createcorruptedancientdebris())) {

            // Cambiar el tiempo de cocción a 3 minutos (3600 ticks)
            event.setTotalCookTime(3600);

            // Efectos al empezar
            Block furnace = event.getBlock();
            furnace.getWorld().playSound(furnace.getLocation(),
                    Sound.BLOCK_FIRE_AMBIENT, 1.0f, 2.0f);
            furnace.getWorld().spawnParticle(Particle.SCULK_SOUL,
                    furnace.getLocation().add(0.5, 0.5, 0.5), 10, 0.3, 0.2, 0.3, 0.02);
        }
    }

    // Evento para cuando termina la cocción
    @EventHandler
    public void onFurnaceSmelt(FurnaceSmeltEvent event) {
        if (!isApplied) return;

        // Verificar si es Corrupted Ancient Debris en un alto horno
        if (isBlastFurnace(event.getBlock()) &&
                event.getSource().isSimilar(corruptedAncientDebris.createcorruptedancientdebris())) {

            // Reemplazar el resultado por el lingote corrupto
            event.setResult(CorruptedNetheriteItems.createCorruptedScrapNetherite());

            // Efectos al terminar
            Block furnace = event.getBlock();
            furnace.getWorld().playSound(furnace.getLocation(),
                    Sound.BLOCK_FIRE_EXTINGUISH, 1.0f, 0.6f);
            furnace.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME,
                    furnace.getLocation().add(0.5, 0.5, 0.5), 30, 0.3, 0.2, 0.3, 0.1);
            furnace.getWorld().spawn(furnace.getLocation().add(0.5, 0.5, 0.5),
                    ExperienceOrb.class).setExperience(8);
        }
    }

    // Evento para prevenir la cocción en hornos normales
    @EventHandler
    public void onNormalFurnaceSmelt(FurnaceSmeltEvent event) {
        if (!isApplied) return;

        if (event.getSource().isSimilar(corruptedAncientDebris.createcorruptedancientdebris()) &&
                !isBlastFurnace(event.getBlock())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!isApplied) return;

        if (event.getCause() == EntityDamageEvent.DamageCause.FIRE || event.getCause() == EntityDamageEvent.DamageCause.FIRE_TICK ||
                event.getCause() == EntityDamageEvent.DamageCause.LAVA || event.getCause() == EntityDamageEvent.DamageCause.HOT_FLOOR) {
            event.setDamage(event.getDamage() * 3);
        }
        if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
            event.setDamage(event.getDamage() * 3);
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!isApplied) return;

        // Verificar que sea click derecho (no físico ni izquierdo)
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null) return;

        Material type = clickedBlock.getType();

        // Lista de materiales peligrosos
        if (type == Material.OAK_BUTTON ||
                type == Material.OAK_DOOR ||
                type.toString().endsWith("_DOOR") ||
                type.toString().endsWith("_BUTTON")) {

            Player player = event.getPlayer();
            player.damage(5000000);

            // Efectos de sonido y visuales
            player.getWorld().playSound(player.getLocation(),
                    Sound.ENTITY_PLAYER_HURT, 2.0f, 0.5f);
            player.getWorld().spawnParticle(Particle.ANGRY_VILLAGER,
                    player.getLocation(), 5, 0.5, 0.5, 0.5, 0.1);
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!isApplied || !hasActuallyMoved(event.getFrom(), event.getTo())) return;

        Player player = event.getPlayer();
        Location loc = player.getLocation();
        World world = player.getWorld();

        // Verificar condiciones
        if (world.hasStorm() &&
                isRainPossible(loc) &&
                isExposedToRain(player)) {

            // 1/500,000 de probabilidad por tick
            if (random.nextInt(500000) == 0) {
                applyRainDamage(player);
            }
        }
    }

    // Método para verificar biomas donde puede llover
    private boolean isRainPossible(Location loc) {
        Biome biome = loc.getBlock().getBiome();

        // Lista de biomas donde nunca llueve
        return !(biome == Biome.BADLANDS ||
                biome == Biome.WOODED_BADLANDS ||
                biome == Biome.ERODED_BADLANDS ||
                biome == Biome.DESERT ||
                biome == Biome.SAVANNA ||
                biome == Biome.SAVANNA_PLATEAU ||
                biome == Biome.WINDSWEPT_SAVANNA);
    }

    // Método mejorado para verificar exposición
    private boolean isExposedToRain(Player player) {
        Location eyeLoc = player.getEyeLocation();
        return player.getWorld().getHighestBlockYAt(eyeLoc) <= eyeLoc.getY() &&
                eyeLoc.getBlock().getType() == Material.AIR;
    }

    private boolean hasActuallyMoved(Location from, Location to) {
        return from.getBlockX() != to.getBlockX() ||
                from.getBlockY() != to.getBlockY() ||
                from.getBlockZ() != to.getBlockZ();
    }

    private void applyRainDamage(Player player) {
        player.damage(1.0);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_HURT, 1.0f, 1.2f);
        player.spawnParticle(Particle.ANGRY_VILLAGER, player.getEyeLocation(), 10, 0.5, 0.5, 0.5, 0.1);
    }

    @EventHandler
    public void onPlayerEnterTrialChamber(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Location to = event.getTo();
        if (to == null) return;

        Block block = to.getBlock();

        boolean isNearTrialSpawner = false;
        int radius = 20; // Rango en bloques

        outerLoop:
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    Material type = block.getRelative(x, y, z).getType();
                    if (type == Material.TRIAL_SPAWNER) {
                        isNearTrialSpawner = true;
                        break outerLoop;
                    }
                }
            }
        }

        if (isNearTrialSpawner && !player.hasPotionEffect(PotionEffectType.TRIAL_OMEN)) {
            // Aplica el efecto por 1 hora (72000 ticks)
            player.addPotionEffect(new PotionEffect(PotionEffectType.TRIAL_OMEN, 72000, 0, false, true, true));
        }
    }

    @EventHandler
    public void onDrinkMilk(PlayerItemConsumeEvent event) {
        if (!isApplied) return;

        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item.getType() == Material.MILK_BUCKET) {
            if (player.hasPotionEffect(PotionEffectType.TRIAL_OMEN)) {
                event.setCancelled(true); // Cancela el consumo del cubo de leche
                player.sendMessage("§c۞ No puedes eliminar el efecto de Trial Omen con leche.");
            }
        }
    }

}
