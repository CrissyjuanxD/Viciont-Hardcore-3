package Dificultades;

import Dificultades.CustomMobs.*;
import Handlers.DayHandler;
import items.*;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class DayTenChanges implements Listener {
    private final JavaPlugin plugin;
    private final DayHandler dayHandler;
    private boolean isApplied = false;
    private BukkitTask checkDayTask;
    private final Iceologer iceologer;
    private final Random random = new Random();
    private final WhiteEnderman whiteEnderman;
    private final InfernalCreeper infernalCreeper;
    private final UltraCorruptedSpider ultraCorruptedSpider;
    private final FastRavager fastRavager;
    private final CorruptedSpider corruptedSpider;
    private final CorruptedZombies corruptedZombies;
    private final CorruptedSoul corruptedSoul;
    private final LifeTotem lifeTotem;
    private final InfernalTotem infernalTotem;
    private final SpiderTotem spiderTotem;

    public DayTenChanges(JavaPlugin plugin, DayHandler dayHandler) {
        this.plugin = plugin;
        this.dayHandler = dayHandler;
        this.iceologer = new Iceologer(plugin);
        this.whiteEnderman = new WhiteEnderman(plugin);
        this.infernalCreeper = new InfernalCreeper(plugin);
        this.ultraCorruptedSpider = new UltraCorruptedSpider(plugin);
        this.fastRavager = new FastRavager(plugin);
        this.corruptedSpider = new CorruptedSpider(plugin);
        this.corruptedZombies = new CorruptedZombies(plugin);
        this.corruptedSoul = new CorruptedSoul(plugin);
        this.lifeTotem = new LifeTotem(plugin);
        this.infernalTotem = new InfernalTotem(plugin);
        this.spiderTotem = new SpiderTotem(plugin);
    }

    public void apply() {
        if (!isApplied) {
            isApplied = true;
            Bukkit.getPluginManager().registerEvents(this, plugin);
            whiteEnderman.apply();
            infernalCreeper.apply();
            ultraCorruptedSpider.apply();
            fastRavager.apply();
            CustomTotemsCraft();
        }
    }

    public void revert() {
        if (isApplied) {
            isApplied = false;
            whiteEnderman.revert();
            infernalCreeper.revert();
            ultraCorruptedSpider.revert();
            fastRavager.revert();

            Bukkit.removeRecipe(new NamespacedKey(plugin, "totem_especial"));
            Bukkit.removeRecipe(new NamespacedKey(plugin, "life_totem"));
            Bukkit.removeRecipe(new NamespacedKey(plugin, "infernal_totem"));
            Bukkit.removeRecipe(new NamespacedKey(plugin, "spider_totem"));

            HandlerList.unregisterAll(this);
        }
    }

    // SPAWN DE MOBS "INFERNAL CREEPER", "WHITE ENDERMAN", "ULTRA CORRUPTED SPIDER" Y "CORRUPTED ZOMBIE"

    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (!isApplied) return;

        if (shouldConvertCorruptedZombieSpawn(event)) {
            Zombie zombie = (Zombie) event.getEntity();
            convertToCorruptedZombie(zombie);
            return;
        }

        if (shouldConvertWhiteEndermanSpawn(event)) {
            Enderman enderman = (Enderman) event.getEntity();
            convertToWhiteEnderman(enderman);
            return;
        }

        handleIceologerConversion(event);
        handleSpiderConversion(event);
        handleInfernalandNormalCreeperConversion(event);
        handleFastRavagerConversion(event);
        /*handleZombieConversion(event);*/
    }

    private void handleIceologerConversion(CreatureSpawnEvent event) {
        EntityType type = event.getEntityType();

        if (type == EntityType.PILLAGER) {
            if (event.getEntity().getPersistentDataContainer()
                    .has(iceologer.getIceologerKey(), PersistentDataType.BYTE)) {
                return;
            }
            if (random.nextInt(8) == 0) {
                Location loc = event.getEntity().getLocation();
                iceologer.spawnIceologer(loc);
                event.getEntity().remove();
            }
        }
    }

    private void handleSpiderConversion(CreatureSpawnEvent event) {
        // Verificar que sea en el Overworld
        if (event.getLocation().getWorld().getEnvironment() != World.Environment.NORMAL) {
            return;
        }

        if (event.getEntityType() != EntityType.SPIDER) return;

        if (event.getEntity().getPersistentDataContainer().has(corruptedSpider.getCorruptedSpiderKey(), PersistentDataType.BYTE) ||
                event.getEntity().getPersistentDataContainer().has(ultraCorruptedSpider.getUltraCorruptedSpiderKey(), PersistentDataType.BYTE)) {
            return;
        }

        if (random.nextInt(2) != 0) return;

        Spider spider = (Spider) event.getEntity();
        Location loc = spider.getLocation();
        if (random.nextBoolean()) {
            corruptedSpider.spawnCorruptedSpider(loc);
        } else {
            ultraCorruptedSpider.spawnUltraCorruptedSpider(loc);
        }
        spider.remove();
    }

    private void handleInfernalandNormalCreeperConversion(CreatureSpawnEvent event) {
        if (event.getLocation().getWorld().getEnvironment() != World.Environment.NETHER) {
            return;
        }

        if (event.getEntityType() != EntityType.ENDERMAN) return;

        // Verificar que no sea ya una araña corrupta
        if (event.getEntity().getPersistentDataContainer()
                .has(infernalCreeper.getInfernalCreeperKey(), PersistentDataType.BYTE)) {
            return;
        }

        Enderman enderman = (Enderman) event.getEntity();
        Location loc = enderman.getLocation();

        double randomValue = random.nextDouble();

        boolean isinfernalCreeper = random.nextInt(4) == 0;
        boolean isNormalCreeper = random.nextInt(2) == 0;

        if (isinfernalCreeper) {
            infernalCreeper.spawnInfernalCreeper(loc);
        } else if (isNormalCreeper) {
            enderman.getWorld().spawn(loc, Creeper.class);
        } else {
            return;
        }
        enderman.remove();
    }

    private boolean shouldConvertWhiteEndermanSpawn(CreatureSpawnEvent event) {
        return isApplied &&
                event.getEntityType() == EntityType.ENDERMAN &&
                !event.getEntity().getPersistentDataContainer()
                        .has(whiteEnderman.getWhiteEndermanKey(), PersistentDataType.BYTE);
    }

    private void convertToWhiteEnderman(Enderman enderman) {
        if (enderman.getWorld().getEnvironment() != World.Environment.NORMAL &&
                enderman.getWorld().getEnvironment() != World.Environment.NETHER) {
            return;
        }

        Location loc = enderman.getLocation();
        whiteEnderman.transformToWhiteEnderman(enderman);
    }

    private boolean shouldConvertCorruptedZombieSpawn(CreatureSpawnEvent event) {
        return isApplied &&
                event.getEntityType() == EntityType.ZOMBIE &&
                !event.getEntity().getPersistentDataContainer()
                        .has(corruptedZombies.getCorruptedKey(), PersistentDataType.BYTE);
    }

    private void convertToCorruptedZombie(Zombie zombie) {
        Location loc = zombie.getLocation();
        corruptedZombies.transformToCorruptedZombie(zombie);
    }

    private void handleFastRavagerConversion(CreatureSpawnEvent event) {
        if (event.getLocation().getWorld().getEnvironment() != World.Environment.NETHER) {
            return;
        }

        if (event.getEntityType() != EntityType.ZOMBIFIED_PIGLIN) return;

        // Verificar que no sea ya una araña corrupta
        if (event.getEntity().getPersistentDataContainer()
                .has(fastRavager.getFastRavagerKey(), PersistentDataType.BYTE)) {
            return;
        }

        if (random.nextInt(15) != 0) return;

        // Convertir Piglin a Spider
        PigZombie pigZombie = (PigZombie) event.getEntity();
        Location loc = pigZombie.getLocation();

        fastRavager.spawnFastRavager(loc);
        pigZombie.remove();
    }

    //LOGROS (SE MANEJA EN OTRA CLASE) ACHIEVEMENTPARTYHANDLER

    //Los Zombified Piglin dejarán de dropear pepitas de oro
    @EventHandler
    public void onZombifiedPiglinDeath(EntityDeathEvent event) {
        if (!isApplied || event.getEntityType() != EntityType.ZOMBIFIED_PIGLIN) return;
        event.getDrops().removeIf(item -> item.getType() == Material.GOLD_NUGGET);
    }

    //La Arañas de cuevas al spawnear tendrán Velocidad II, Fuerza II, Resistencia II
    @EventHandler
    public void onCaveSpiderSpawn(CreatureSpawnEvent event) {
        if (!isApplied || event.getEntityType() != EntityType.CAVE_SPIDER || event.isCancelled()) return;

        CaveSpider caveSpider = (CaveSpider) event.getEntity();
        caveSpider.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, PotionEffect.INFINITE_DURATION, 1));
        caveSpider.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, PotionEffect.INFINITE_DURATION, 1));
        caveSpider.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, PotionEffect.INFINITE_DURATION, 1));
    }

    @EventHandler
    public void onZombifiedPiglinSpawn(CreatureSpawnEvent event) {
        if (!isApplied || event.getEntityType() != EntityType.ZOMBIFIED_PIGLIN) return;

        PigZombie piglin = (PigZombie) event.getEntity();
        piglin.setAngry(true);

        List<Player> nearbyPlayers = piglin.getWorld().getNearbyEntities(piglin.getLocation(), 16, 16, 16).stream()
                .filter(entity -> entity instanceof Player)
                .map(entity -> (Player) entity)
                .collect(Collectors.toList());

        if (!nearbyPlayers.isEmpty()) {
            // Elige el jugador más cercano
            Player target = nearbyPlayers.get(0);
            piglin.setTarget(target);
        }
    }

    //CRAFTEOS

    private void CustomTotemsCraft() {

        //Totem Especial
        ShapedRecipe SpecialTotem = new ShapedRecipe(new NamespacedKey(plugin, "totem_especial"), ItemsTotems.createSpecialTotem());
        SpecialTotem.shape("   ", " TT", " TT");
        SpecialTotem.setIngredient('T', Material.TOTEM_OF_UNDYING);

        //Life Totem
        ShapedRecipe CustomlifeTotem = new ShapedRecipe(new NamespacedKey(plugin, "life_totem"), lifeTotem.createLifeTotem());
        CustomlifeTotem.shape("DSD", "PEP", "DSD");
        CustomlifeTotem.setIngredient('D', Material.DIAMOND_BLOCK);
        CustomlifeTotem.setIngredient('S', new RecipeChoice.ExactChoice(corruptedSoul.createCorruptedSoulEssence()));
        CustomlifeTotem.setIngredient('P', new RecipeChoice.ExactChoice(ItemsTotems.createWhiteEnderPearl()));
        CustomlifeTotem.setIngredient('E', new RecipeChoice.ExactChoice(ItemsTotems.createSpecialTotem()));

        //Infernal Totem
        ShapedRecipe CustomInfernalTotem = new ShapedRecipe(new NamespacedKey(plugin, "infernal_totem"), infernalTotem.createInfernalTotem());
        CustomInfernalTotem.shape("GSG", "PEP", "GSG");
        CustomInfernalTotem.setIngredient('G', Material.GOLD_BLOCK);
        CustomInfernalTotem.setIngredient('S', new RecipeChoice.ExactChoice(corruptedSoul.createCorruptedSoulEssence()));
        CustomInfernalTotem.setIngredient('P', new RecipeChoice.ExactChoice(ItemsTotems.createInfernalCreeperPowder()));
        CustomInfernalTotem.setIngredient('E', new RecipeChoice.ExactChoice(ItemsTotems.createSpecialTotem()));

        //Spider Totem
        ShapedRecipe CustomSpiderTotem = new ShapedRecipe(new NamespacedKey(plugin, "spider_totem"), spiderTotem.createSpiderTotem());
        CustomSpiderTotem.shape("MSM", "UEU", "MSM");
        CustomSpiderTotem.setIngredient('M', Material.EMERALD_BLOCK);
        CustomSpiderTotem.setIngredient('S', new RecipeChoice.ExactChoice(corruptedSoul.createCorruptedSoulEssence()));
        CustomSpiderTotem.setIngredient('U', new RecipeChoice.ExactChoice(ItemsTotems.createUltraCorruptedSpiderEye()));
        CustomSpiderTotem.setIngredient('E', new RecipeChoice.ExactChoice(ItemsTotems.createSpecialTotem()));

        Bukkit.addRecipe(SpecialTotem);
        Bukkit.addRecipe(CustomlifeTotem);
        Bukkit.addRecipe(CustomInfernalTotem);
        Bukkit.addRecipe(CustomSpiderTotem);
    }

}