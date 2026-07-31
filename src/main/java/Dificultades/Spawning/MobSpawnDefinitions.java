package Dificultades.Spawning;

import Dificultades.CustomMobs.*;
import Handlers.DayHandler;
import Handlers.DeathStormHandler;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Random;

/**
 * Catálogo central de todos los spawns/conversiones de mobs custom,
 * organizado por DÍA (no por mob) para que sea fácil de recorrer
 * siguiendo el itinerario del server.
 *
 * IMPORTANTE: desde que SpawnRule tiene specificity(), el ORDEN en que
 * se llama a registry.register() ya no afecta el resultado para reglas
 * exclusivas (el motor ordena solo por especificidad). Esta organización
 * por día es solo para legibilidad tuya, no es un requisito técnico.
 */
public class MobSpawnDefinitions {

    private final JavaPlugin plugin;
    private final MobSpawnRegistry registry;
    private final Random random = new Random();
    private final DeathStormHandler deathStormHandler;

    private final CorruptedZombies corruptedZombies;
    private final CorruptedSpider corruptedSpider;
    private final CorruptedCreeper corruptedCreeper;
    private final Bombita bombita;
    private final CorruptedInfernalSpider corruptedInfernalSpider;
    private final GuardianCorruptedSkeleton guardianCorruptedSkeleton;
    private final CorruptedDrowned corruptedDrowned;
    private final PiglinGlobo piglinGlobo;
    private final InvertedGhast invertedGhast;
    private final CorruptedSkeleton corruptedSkeleton;
    private final InfernalBeast infernalBeast;
    private final BuffBreeze buffBreeze;
    private final ImperialBrute imperialBrute;
    private final Iceologer iceologer;
    private final WhiteEnderman whiteEnderman;
    private final InfernalCreeper infernalCreeper;
    private final FastRavager fastRavager;
    private final ToxicSpider toxicSpider;
    private final SpectralEye spectralEye;
    private final EspectralGhast espectralGhast;
    private final EspectralCreeper espectralCreeper;
    private final EspectralSilverfish espectralSilverfish;
    private final GuardianShulker_Descartado guardianShulkerDescartado;

    public MobSpawnDefinitions(JavaPlugin plugin, MobSpawnRegistry registry, DayHandler dayHandler, DeathStormHandler deathStormHandler) {
        this.plugin = plugin;
        this.registry = registry;
        this.deathStormHandler = deathStormHandler;

        this.corruptedZombies = new CorruptedZombies(plugin, dayHandler);
        this.corruptedSpider = new CorruptedSpider(plugin, dayHandler);
        this.corruptedCreeper = new CorruptedCreeper(plugin);
        this.bombita = new Bombita(plugin);
        this.corruptedInfernalSpider = new CorruptedInfernalSpider(plugin);
        this.guardianCorruptedSkeleton = new GuardianCorruptedSkeleton(plugin);
        this.corruptedDrowned = new CorruptedDrowned(plugin);
        this.piglinGlobo = new PiglinGlobo(plugin);
        this.invertedGhast = new InvertedGhast(plugin);
        this.corruptedSkeleton = new CorruptedSkeleton(plugin, dayHandler);
        this.infernalBeast = new InfernalBeast(plugin);
        this.buffBreeze = new BuffBreeze(plugin);
        this.imperialBrute = new ImperialBrute(plugin);
        this.iceologer = new Iceologer(plugin);
        this.whiteEnderman = new WhiteEnderman(plugin);
        this.infernalCreeper = new InfernalCreeper(plugin);
        this.fastRavager = new FastRavager(plugin);
        this.toxicSpider = new ToxicSpider(plugin);
        this.spectralEye = new SpectralEye(plugin);
        this.espectralGhast = new EspectralGhast(plugin);
        this.espectralCreeper = new EspectralCreeper(plugin);
        this.espectralSilverfish = new EspectralSilverfish(plugin);
        this.guardianShulkerDescartado = new GuardianShulker_Descartado(plugin);
    }

    public void registerAll() {
        registerDay1to3();
        registerDay4();
        registerDay5();
        registerDay6();
        registerDay7();
        registerDay8();
        registerDay9();
        registerDay10();
        registerDay13();
        registerDay14();
        registerDeathStorm();
    }


    // ================================================================
    //  DÍA 1-3
    // ================================================================
    private void registerDay1to3() {

        // Zombie -> Corrupted Zombie (1/20)
        registry.register(SpawnRule.builder("zombie_corrupted_d1_3")
                .triggers(EntityType.ZOMBIE)
                .dayRange(1, 3)
                .chance(1.0 / 20.0)
                .marker(corruptedZombies.getCorruptedKey())
                .action((event, day) -> { event.setCancelled(true); corruptedZombies.spawnCorruptedZombie(event.getLocation()); })
                .build());

        // Spider -> Corrupted Spider (1/20), solo Overworld
        registry.register(SpawnRule.builder("spider_corrupted_d1_3")
                .triggers(EntityType.SPIDER)
                .dayRange(1, 3)
                .environments(World.Environment.NORMAL)
                .chance(1.0 / 20.0)
                .marker(corruptedSpider.getCorruptedSpiderKey())
                .action((event, day) -> corruptedSpider.transformspawnCorruptedSpider((Spider) event.getEntity()))
                .build());
    }


    // ================================================================
    //  DÍA 4
    // ================================================================
    private void registerDay4() {

        // Zombie -> Corrupted Zombie (1/12), rango 4-6
        registry.register(SpawnRule.builder("zombie_corrupted_d4_6")
                .triggers(EntityType.ZOMBIE)
                .dayRange(4, 6)
                .chance(1.0 / 12.0)
                .marker(corruptedZombies.getCorruptedKey())
                .action((event, day) -> { event.setCancelled(true); corruptedZombies.spawnCorruptedZombie(event.getLocation()); })
                .build());

        // Spider -> Corrupted Spider (1/12), rango 4-6, Overworld
        registry.register(SpawnRule.builder("spider_corrupted_d4_6")
                .triggers(EntityType.SPIDER)
                .dayRange(4, 6)
                .environments(World.Environment.NORMAL)
                .chance(1.0 / 12.0)
                .marker(corruptedSpider.getCorruptedSpiderKey())
                .action((event, day) -> corruptedSpider.transformspawnCorruptedSpider((Spider) event.getEntity()))
                .build());

        // Wither Skeleton -> Guardian Corrupted Skeleton, siempre, desde día 4
        registry.register(SpawnRule.builder("wither_skeleton_guardian_corrupted")
                .triggers(EntityType.WITHER_SKELETON)
                .fromDay(4)
                .marker(guardianCorruptedSkeleton.getGCSkeletonKey())
                .action((event, day) -> {
                    WitherSkeleton skeleton = (WitherSkeleton) event.getEntity();
                    skeleton.getWorld().spawnParticle(
                            org.bukkit.Particle.SOUL_FIRE_FLAME, skeleton.getLocation(), 20, 0.5, 0.5, 0.5);
                    guardianCorruptedSkeleton.transformToCorruptedSkeleton(skeleton);
                })
                .build());

        // Zombified Piglin -> Corrupted Infernal Spider, Nether, 1/10, desde día 4
        registry.register(SpawnRule.builder("zombified_piglin_to_infernal_spider")
                .triggers(EntityType.ZOMBIFIED_PIGLIN)
                .fromDay(4)
                .environments(World.Environment.NETHER)
                .chance(1.0 / 10.0)
                .marker(corruptedInfernalSpider.getCorruptedInfernalKey())
                .action((event, day) -> {
                    event.setCancelled(true);
                    corruptedInfernalSpider.spawnCorruptedInfernalSpider(event.getLocation());
                })
                .build());
    }


    // ================================================================
    //  DÍA 5
    // ================================================================
    private void registerDay5() {

        // Creeper -> Corrupted Creeper (1/3), Overworld, spawn NATURAL, desde día 5
        registry.register(SpawnRule.builder("corrupted_creeper")
                .triggers(EntityType.CREEPER)
                .environments(World.Environment.NORMAL)
                .fromDay(5)
                .chance(1.0 / 3.0)
                .marker(corruptedCreeper.getCorruptedCreeperKey())
                .condition(event -> event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.NATURAL)
                .action((event, day) -> {
                    event.setCancelled(true);
                    corruptedCreeper.spawnCorruptedCreeper(event.getLocation());
                })
                .build());

        // Drowned -> Corrupted Drowned (50%), desde día 5
        registry.register(SpawnRule.builder("drowned_corrupted")
                .triggers(EntityType.DROWNED)
                .fromDay(5)
                .chance(0.5)
                .marker(corruptedDrowned.getCorruptedDrownedKey())
                .action((event, day) -> {
                    event.setCancelled(true);
                    corruptedDrowned.spawnCorruptedDrowned(event.getLocation());
                })
                .build());
    }


    // ================================================================
    //  DÍA 6
    // ================================================================
    private void registerDay6() {
        // (Ver sección DeathStorm al final: el buff de mobs durante tormenta empieza día 6.)
    }


    // ================================================================
    //  DÍA 7
    // ================================================================
    private void registerDay7() {

        // Zombie -> Corrupted Zombie (1/6), rango 7-8
        registry.register(SpawnRule.builder("zombie_corrupted_d7_8")
                .triggers(EntityType.ZOMBIE)
                .dayRange(7, 8)
                .chance(1.0 / 6.0)
                .marker(corruptedZombies.getCorruptedKey())
                .action((event, day) -> { event.setCancelled(true); corruptedZombies.spawnCorruptedZombie(event.getLocation()); })
                .build());

        // Spider -> Corrupted Spider (1/6), rango 7-8, Overworld
        registry.register(SpawnRule.builder("spider_corrupted_d7_8")
                .triggers(EntityType.SPIDER)
                .dayRange(7, 8)
                .environments(World.Environment.NORMAL)
                .chance(1.0 / 6.0)
                .marker(corruptedSpider.getCorruptedSpiderKey())
                .action((event, day) -> corruptedSpider.transformspawnCorruptedSpider((Spider) event.getEntity()))
                .build());

        // Skeleton -> Corrupted Skeleton (1/4), rango 7-9, Overworld/Nether/End
        registry.register(SpawnRule.builder("skeleton_corrupted_d7_9")
                .triggers(EntityType.SKELETON)
                .dayRange(7, 9)
                .environments(World.Environment.NORMAL, World.Environment.NETHER, World.Environment.THE_END)
                .chance(1.0 / 4.0)
                .marker(corruptedSkeleton.getCorruptedKey())
                .action((event, day) -> { event.setCancelled(true); corruptedSkeleton.spawnCorruptedSkeleton(event.getLocation(), null); })
                .build());

        // Ghast -> Inverted Ghast (50%), Nether, SOLO día 7 exacto
        registry.register(SpawnRule.builder("ghast_inverted_d7")
                .triggers(EntityType.GHAST)
                .dayRange(7, 7)
                .environments(World.Environment.NETHER)
                .chance(0.5)
                .condition(event -> !isAlreadyCustomGhast(event.getEntity()))
                .action((event, day) -> {
                    invertedGhast.spawnInvertedGhast(event.getLocation());
                    event.getEntity().remove();
                })
                .build());

        // Bat -> Blaze (1/8), sin marker
        registry.register(SpawnRule.builder("bat_to_blaze")
                .triggers(EntityType.BAT)
                .fromDay(7)
                .chance(1.0 / 8.0)
                .action((event, day) -> {
                    event.setCancelled(true);
                    event.getLocation().getWorld().spawnEntity(event.getLocation(), EntityType.BLAZE);
                })
                .build());

        // Chicken / Pig -> Ravager (siempre)
        registry.register(SpawnRule.builder("chicken_pig_to_ravager")
                .triggers(EntityType.CHICKEN, EntityType.PIG)
                .fromDay(7)
                .action((event, day) -> {
                    event.setCancelled(true);
                    event.getLocation().getWorld().spawnEntity(event.getLocation(), EntityType.RAVAGER);
                })
                .build());

        // --- Modificadores (no conversión) desde día 7 ---

        // Creeper eléctrico por defecto, salvo que ya sea Corrupted Creeper
        registry.register(SpawnRule.builder("creeper_electric_default")
                .triggers(EntityType.CREEPER)
                .fromDay(7)
                .nonExclusive()
                .condition(event -> !corruptedCreeper.isCorruptedCreeper(event.getEntity()))
                .action((event, day) -> ((Creeper) event.getEntity()).setPowered(true))
                .build());

        // Piglin / Zombified Piglin con armadura de diamante completa
        registry.register(SpawnRule.builder("piglin_diamond_armor")
                .triggers(EntityType.PIGLIN, EntityType.ZOMBIFIED_PIGLIN)
                .fromDay(7)
                .nonExclusive()
                .action((event, day) -> {
                    LivingEntity le = (LivingEntity) event.getEntity();
                    if (le.getEquipment() == null) return;
                    le.getEquipment().setHelmet(new ItemStack(Material.DIAMOND_HELMET));
                    le.getEquipment().setChestplate(new ItemStack(Material.DIAMOND_CHESTPLATE));
                    le.getEquipment().setLeggings(new ItemStack(Material.DIAMOND_LEGGINGS));
                    le.getEquipment().setBoots(new ItemStack(Material.DIAMOND_BOOTS));
                })
                .build());

        // Phantom con el doble de vida y Fuerza IV (solo si no tiene nombre custom)
        registry.register(SpawnRule.builder("phantom_buffed")
                .triggers(EntityType.PHANTOM)
                .fromDay(7)
                .nonExclusive()
                .condition(event -> event.getEntity().getCustomName() == null)
                .action((event, day) -> {
                    Phantom phantom = (Phantom) event.getEntity();
                    phantom.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(40.0);
                    phantom.setHealth(40.0);
                    phantom.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, Integer.MAX_VALUE, 3));
                    phantom.getAttribute(Attribute.GENERIC_SCALE).setBaseValue(3.0);
                })
                .build());
    }


    // ================================================================
    //  DÍA 8
    // ================================================================
    private void registerDay8() {

        // Husk -> Corrupted Zombie (1/3)
        registry.register(SpawnRule.builder("husk_to_corrupted_zombie")
                .triggers(EntityType.HUSK)
                .fromDay(8)
                .chance(1.0 / 3.0)
                .marker(corruptedZombies.getCorruptedKey())
                .action((event, day) -> { event.setCancelled(true); corruptedZombies.spawnCorruptedZombie(event.getLocation()); })
                .build());

        // Stray -> Corrupted Skeleton (1/3)
        registry.register(SpawnRule.builder("stray_corrupted")
                .triggers(EntityType.STRAY)
                .fromDay(8)
                .chance(1.0 / 3.0)
                .marker(corruptedSkeleton.getCorruptedKey())
                .action((event, day) -> { event.setCancelled(true); corruptedSkeleton.spawnCorruptedSkeleton(event.getLocation(), null); })
                .build());

        // Bogged -> Corrupted Skeleton (1/4)
        registry.register(SpawnRule.builder("bogged_corrupted")
                .triggers(EntityType.BOGGED)
                .fromDay(8)
                .chance(1.0 / 4.0)
                .marker(corruptedSkeleton.getCorruptedKey())
                .action((event, day) -> { event.setCancelled(true); corruptedSkeleton.spawnCorruptedSkeleton(event.getLocation(), null); })
                .build());

        // Ghast -> 50% Piglin Globo / 50% Inverted Ghast (a partir de día 8, chance 50% de convertir)
        registry.register(SpawnRule.builder("ghast_split_d8_plus")
                .triggers(EntityType.GHAST)
                .fromDay(8)
                .environments(World.Environment.NETHER)
                .chance(0.5)
                .condition(event -> !isAlreadyCustomGhast(event.getEntity()))
                .action((event, day) -> {
                    if (random.nextBoolean()) {
                        piglinGlobo.spawnPiglinGlobo(event.getLocation());
                    } else {
                        invertedGhast.spawnInvertedGhast(event.getLocation());
                    }
                    event.getEntity().remove();
                })
                .build());

        // Piglin Brute -> Imperial Brute (siempre)
        registry.register(SpawnRule.builder("piglin_brute_to_imperial")
                .triggers(EntityType.PIGLIN_BRUTE)
                .fromDay(8)
                .marker(imperialBrute.getBruteImperialKey())
                .action((event, day) -> imperialBrute.transformToBruteImperial((PiglinBrute) event.getEntity()))
                .build());

        // Piglin -> Imperial Brute (1/25)
        registry.register(SpawnRule.builder("piglin_to_imperial")
                .triggers(EntityType.PIGLIN)
                .fromDay(8)
                .chance(1.0 / 25.0)
                .marker(imperialBrute.getBruteImperialKey())
                .action((event, day) -> {
                    event.setCancelled(true);
                    imperialBrute.spawnBruteImperial(event.getLocation());
                })
                .build());

        // Breeze -> Buff Breeze (1/3), Overworld
        registry.register(SpawnRule.builder("breeze_buffed")
                .triggers(EntityType.BREEZE)
                .fromDay(8)
                .environments(World.Environment.NORMAL)
                .chance(1.0 / 3.0)
                .marker(buffBreeze.getBuffBreezeKey())
                .action((event, day) -> {
                    event.setCancelled(true);
                    buffBreeze.spawnBuffBreeze(event.getLocation());
                })
                .build());

        // Hoglin -> Infernal Beast (1/6), Nether
        registry.register(SpawnRule.builder("hoglin_to_infernal_beast")
                .triggers(EntityType.HOGLIN)
                .fromDay(8)
                .environments(World.Environment.NETHER)
                .chance(1.0 / 6.0)
                .marker(infernalBeast.getInfernalBeastKey())
                .action((event, day) -> {
                    event.setCancelled(true);
                    infernalBeast.spawnInfernalBeast(event.getLocation());
                })
                .build());
    }


    // ================================================================
    //  DÍA 9
    // ================================================================
    private void registerDay9() {

        // Zombie -> Corrupted Zombie (1/4), solo día 9
        registry.register(SpawnRule.builder("zombie_corrupted_d9")
                .triggers(EntityType.ZOMBIE)
                .dayRange(9, 9)
                .chance(1.0 / 4.0)
                .marker(corruptedZombies.getCorruptedKey())
                .action((event, day) -> { event.setCancelled(true); corruptedZombies.spawnCorruptedZombie(event.getLocation()); })
                .build());

        // Spider -> Corrupted Spider (1/4), solo día 9, Overworld
        registry.register(SpawnRule.builder("spider_corrupted_d9")
                .triggers(EntityType.SPIDER)
                .dayRange(9, 9)
                .environments(World.Environment.NORMAL)
                .chance(1.0 / 4.0)
                .marker(corruptedSpider.getCorruptedSpiderKey())
                .action((event, day) -> corruptedSpider.transformspawnCorruptedSpider((Spider) event.getEntity()))
                .build());

        // Raiders: resistencia al fuego + buffs específicos por tipo (modificador, no conversión)
        registry.register(SpawnRule.builder("raiders_fire_resistance")
                .triggers(EntityType.PILLAGER, EntityType.VINDICATOR, EntityType.EVOKER,
                        EntityType.RAVAGER, EntityType.WITCH, EntityType.ILLUSIONER)
                .fromDay(9)
                .nonExclusive()
                .action((event, day) -> {
                    LivingEntity raider = (LivingEntity) event.getEntity();
                    raider.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, Integer.MAX_VALUE, 0, false, false));

                    if (event.getEntityType() == EntityType.PILLAGER) {
                        ItemStack crossbow = new ItemStack(Material.CROSSBOW);
                        crossbow.addEnchantment(Enchantment.QUICK_CHARGE, 2);
                        ((Pillager) raider).getEquipment().setItemInMainHand(crossbow);
                    }

                    if (event.getEntityType() == EntityType.VINDICATOR) {
                        raider.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, Integer.MAX_VALUE, 1, false, false));
                    }

                    if (event.getEntityType() == EntityType.RAVAGER) {
                        raider.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, Integer.MAX_VALUE, 1, false, false));
                        raider.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 0, false, false));
                        raider.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, Integer.MAX_VALUE, 0, false, false));
                    }
                })
                .build());
    }


    // ================================================================
    //  DÍA 10
    // ================================================================
    private void registerDay10() {

        // Zombie -> Corrupted Zombie (100%, siempre)
        registry.register(SpawnRule.builder("zombie_corrupted_d10_plus")
                .triggers(EntityType.ZOMBIE)
                .fromDay(10)
                .chance(1.0)
                .marker(corruptedZombies.getCorruptedKey())
                .action((event, day) -> { event.setCancelled(true); corruptedZombies.spawnCorruptedZombie(event.getLocation()); })
                .build());

        // Spider: siempre se convierte, 25% Toxic (Cave Spider) / 75% Corrupted
        registry.register(SpawnRule.builder("spider_split_d10_plus")
                .triggers(EntityType.SPIDER)
                .fromDay(10)
                .environments(World.Environment.NORMAL)
                .condition(event ->
                        !event.getEntity().getPersistentDataContainer().has(corruptedSpider.getCorruptedSpiderKey(), PersistentDataType.BYTE) &&
                                !event.getEntity().getPersistentDataContainer().has(toxicSpider.getUltraCorruptedSpiderKey(), PersistentDataType.BYTE))
                .action((event, day) -> {
                    Spider spider = (Spider) event.getEntity();
                    if (random.nextInt(4) == 0) {
                        spider.remove();
                        CaveSpider cave = (CaveSpider) spider.getWorld().spawnEntity(spider.getLocation(), EntityType.CAVE_SPIDER);
                        toxicSpider.transformspawnToxicSpider(cave);
                    } else {
                        corruptedSpider.transformspawnCorruptedSpider(spider);
                    }
                })
                .build());

        // Skeleton -> Corrupted Skeleton (1/3), Overworld/Nether/End
        registry.register(SpawnRule.builder("skeleton_corrupted_d10_plus")
                .triggers(EntityType.SKELETON)
                .fromDay(10)
                .environments(World.Environment.NORMAL, World.Environment.NETHER, World.Environment.THE_END)
                .chance(1.0 / 3.0)
                .marker(corruptedSkeleton.getCorruptedKey())
                .action((event, day) -> { event.setCancelled(true); corruptedSkeleton.spawnCorruptedSkeleton(event.getLocation(), null); })
                .build());

        // Enderman en Nether: 1/3 se convierte en Creeper, si no -> White Enderman
        registry.register(SpawnRule.builder("enderman_nether_creeper_or_white")
                .triggers(EntityType.ENDERMAN)
                .fromDay(10)
                .environments(World.Environment.NETHER)
                .marker(whiteEnderman.getWhiteEndermanKey())
                .action((event, day) -> {
                    if (random.nextInt(3) == 0) {
                        event.setCancelled(true);
                        event.getLocation().getWorld().spawnEntity(event.getLocation(), EntityType.CREEPER);
                    } else {
                        whiteEnderman.transformToWhiteEnderman((Enderman) event.getEntity());
                    }
                })
                .build());

        // Enderman en Overworld -> siempre White Enderman
        registry.register(SpawnRule.builder("enderman_normal_white")
                .triggers(EntityType.ENDERMAN)
                .fromDay(10)
                .environments(World.Environment.NORMAL)
                .marker(whiteEnderman.getWhiteEndermanKey())
                .action((event, day) -> whiteEnderman.transformToWhiteEnderman((Enderman) event.getEntity()))
                .build());

        // Pillager -> Iceologer (1/8)
        registry.register(SpawnRule.builder("pillager_to_iceologer")
                .triggers(EntityType.PILLAGER)
                .fromDay(10)
                .chance(1.0 / 8.0)
                .marker(iceologer.getIceologerKey())
                .action((event, day) -> {
                    event.setCancelled(true);
                    iceologer.spawnIceologer(event.getLocation());
                })
                .build());

        // Monstruos genéricos del Nether -> Infernal Creeper (1/14)
        registry.register(SpawnRule.builder("nether_infernal_creeper")
                .fromDay(10)
                .environments(World.Environment.NETHER)
                .chance(1.0 / 14.0)
                .condition(event -> event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.NATURAL
                        && event.getEntity() instanceof Monster)
                .action((event, day) -> {
                    event.setCancelled(true);
                    infernalCreeper.spawnInfernalCreeper(event.getLocation());
                })
                .build());

        // Monstruos genéricos del Nether -> Fast Ravager (1/20)
        registry.register(SpawnRule.builder("nether_fast_ravager")
/*                .triggers(EntityType.ZOMBIE, EntityType.SKELETON, EntityType.CREEPER, EntityType.SPIDER,
                        EntityType.ENDERMAN, EntityType.WITCH, EntityType.PILLAGER, EntityType.VINDICATOR)*/
                .fromDay(10)
                .environments(World.Environment.NETHER)
                .chance(1.0 / 20.0)
                .condition(event -> event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.NATURAL
                        && event.getEntity() instanceof Monster)
                .action((event, day) -> {
                    event.setCancelled(true);
                    fastRavager.spawnFastRavager(event.getLocation());
                })
                .build());

        // --- Modificadores (no conversión) desde día 10 ---

        // Cave Spider con Velocidad/Fuerza/Resistencia II
        registry.register(SpawnRule.builder("cave_spider_buffed")
                .triggers(EntityType.CAVE_SPIDER)
                .fromDay(10)
                .nonExclusive()
                .action((event, day) -> {
                    CaveSpider cs = (CaveSpider) event.getEntity();
                    cs.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, PotionEffect.INFINITE_DURATION, 1));
                    cs.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, PotionEffect.INFINITE_DURATION, 1));
                    cs.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, PotionEffect.INFINITE_DURATION, 1));
                })
                .build());

        // Zombified Piglin siempre agresivo, target inmediato
        registry.register(SpawnRule.builder("zombified_piglin_aggressive")
                .triggers(EntityType.ZOMBIFIED_PIGLIN)
                .fromDay(10)
                .nonExclusive()
                .action((event, day) -> {
                    PigZombie piglin = (PigZombie) event.getEntity();
                    piglin.setAngry(true);
                    piglin.getWorld().getNearbyEntities(piglin.getLocation(), 16, 16, 16).stream()
                            .filter(e -> e instanceof Player)
                            .map(e -> (Player) e)
                            .findFirst()
                            .ifPresent(piglin::setTarget);
                })
                .build());
    }


    // ================================================================
    //  DÍA 13
    // ================================================================
    private void registerDay13() {

        // Enderman en The End -> roll ponderado a 4 "familias" de mobs
        registry.register(SpawnRule.builder("end_enderman_family_roll")
                .triggers(EntityType.ENDERMAN)
                .fromDay(13)
                .environments(World.Environment.THE_END)
                .condition(event -> !isSpecialEndMob(event.getEntity()))
                .action((event, day) -> {
                    Enderman enderman = (Enderman) event.getEntity();
                    org.bukkit.Location loc = enderman.getLocation();

                    boolean isEnderMob = random.nextInt(9) == 0;
                    boolean isOtherMob = !isEnderMob && random.nextInt(14) == 0;

                    if (isEnderMob) {
                        double pick = random.nextDouble();
                        if (pick < 0.30) espectralGhast.spawnEnderGhast(loc);
                        else if (pick < 0.70) espectralCreeper.spawnEnderCreeper(loc);
                        else espectralSilverfish.spawnEnderSilverfish(loc);
                    } else if (isOtherMob) {
                        double pick = random.nextDouble();
                        if (pick < 0.25) bombita.spawnBombita(loc);
                        else if (pick < 0.50) corruptedZombies.spawnCorruptedZombie(loc);
                        else if (pick < 0.75) toxicSpider.spawnToxicSpider(loc);
                        else corruptedSkeleton.spawnCorruptedSkeleton(loc, null);
                    } else {
                        return;
                    }

                    enderman.remove();
                })
                .build());

        // Spectral Eye (1/25), Overworld/Nether/End
        registry.register(SpawnRule.builder("spectral_eye")
                .fromDay(13)
                .environments(World.Environment.NORMAL, World.Environment.NETHER, World.Environment.THE_END)
                .chance(1.0 / 30.0)
                .marker(spectralEye.getSpectralEyeKey())
                .condition(event -> event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.NATURAL
                        && event.getEntity() instanceof Monster)
                .action((event, day) -> {
                    event.setCancelled(true);
                    spectralEye.spawnSpectralEye(event.getLocation());
                })
                .build());

        // --- Modificadores (no conversión) desde día 13 ---

        // Enderman en The End con Fuerza II infinita
        registry.register(SpawnRule.builder("end_enderman_strength")
                .triggers(EntityType.ENDERMAN)
                .fromDay(13)
                .environments(World.Environment.THE_END)
                .nonExclusive()
                .action((event, day) -> event.getEntity()
                        .addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, Integer.MAX_VALUE, 1, false, false)))
                .build());

        // Endermite con Fuerza III infinita, cualquier dimensión
        registry.register(SpawnRule.builder("endermite_strength")
                .triggers(EntityType.ENDERMITE)
                .fromDay(13)
                .nonExclusive()
                .action((event, day) -> event.getEntity()
                        .addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, Integer.MAX_VALUE, 2, false, false)))
                .build());
    }


    // ================================================================
    //  DÍA 14
    // ================================================================
    private void registerDay14() {

        // Creeper -> Bombita (1/3), Overworld, con prioridad automática sobre Corrupted Creeper
        // gracias a specificity() (misma condición de entorno, pero además excluye Corrupted Creeper).
        registry.register(SpawnRule.builder("bombita_creeper")
                .triggers(EntityType.CREEPER)
                .environments(World.Environment.NORMAL)
                .fromDay(14)
                .chance(1.0 / 3.0)
                .marker(bombita.getBombitaKey())
                .condition(event -> !corruptedCreeper.isCorruptedCreeper(event.getEntity()))
                .action((event, day) -> bombita.transformToBombita((Creeper) event.getEntity()))
                .build());

        // Cow -> Iceologer (siempre)
        registry.register(SpawnRule.builder("cow_to_iceologer")
                .triggers(EntityType.COW)
                .fromDay(14)
                .action((event, day) -> {
                    event.setCancelled(true);
                    iceologer.spawnIceologer(event.getLocation());
                })
                .build());

        // Shulker -> Guardian Shulker (1/50), The End
        registry.register(SpawnRule.builder("shulker_to_guardian")
                .triggers(EntityType.SHULKER)
                .fromDay(14)
                .environments(World.Environment.THE_END)
                .chance(1.0 / 50.0)
                .marker(guardianShulkerDescartado.getGuardianShulkerKey())
                .action((event, day) -> {
                    event.setCancelled(true);
                    guardianShulkerDescartado.spawnGuardianShulker(event.getLocation());
                })
                .build());
    }


    // ================================================================
    //  DEATHSTORM (transversal a varios días — WIP)
    // ================================================================
    private void registerDeathStorm() {

        // Buff de mobs durante tormenta (Strength+Speed), desde día 6.
        // Reemplaza el bloque equivalente que hoy está en
        // DeathStormHandler.onMonsterSpawnForDeathStorm() — si migras esto,
        // borra ese bloque para no aplicar el buff dos veces.
        registry.register(SpawnRule.builder("storm_monster_buff")
                .triggers(EntityType.ZOMBIE, EntityType.SKELETON, EntityType.CREEPER, EntityType.SPIDER,
                        EntityType.ENDERMAN, EntityType.HUSK, EntityType.STRAY, EntityType.DROWNED)
                .fromDay(6)
                .duringStorm()
                .nonExclusive()
                .action((event, day) -> {
                    if (deathStormHandler == null) return;
                    int duration = Math.max(20, deathStormHandler.getRemainingStormSeconds() * 20);
                    LivingEntity mob = (LivingEntity) event.getEntity();
                    mob.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, duration, 0, false, true));
                    mob.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, duration, 0, false, true));
                })
                .build());

        /*
        // --- Ejemplo: Zombie exclusivo de tormenta. Día 1-4 leve (1/20), día 5+ dura (1/10).
        // Gracias a specificity(), no importa en qué orden queden respecto a las reglas
        // normales de zombie: duringStorm() ya les da +1000 de prioridad automática.
        registry.register(SpawnRule.builder("storm_zombie_mild_d1_4")
                .triggers(EntityType.ZOMBIE)
                .dayRange(1, 4)
                .duringStorm()
                .chance(1.0 / 20.0)
                .marker(stormZombie.getStormZombieKey())
                .action((event, day) -> stormZombie.transformToStormZombie((Zombie) event.getEntity()))
                .build());

        registry.register(SpawnRule.builder("storm_zombie_harsh_d5_plus")
                .triggers(EntityType.ZOMBIE)
                .fromDay(5)
                .duringStorm()
                .chance(1.0 / 10.0)
                .marker(stormZombie.getStormZombieKey())
                .action((event, day) -> stormZombie.transformToStormZombie((Zombie) event.getEntity()))
                .build());
        */

        /*
        // --- Ejemplo: "boss" raro de tormenta, día 10+, probabilidad muy baja ---
        registry.register(SpawnRule.builder("storm_elite_rare")
                .triggers(EntityType.SKELETON, EntityType.ZOMBIE)
                .fromDay(10)
                .duringStorm()
                .chance(1.0 / 150.0)
                .marker(stormElite.getStormEliteKey())
                .action((event, day) -> {
                    event.setCancelled(true);
                    stormElite.spawnStormElite(event.getLocation());
                })
                .build());
        */
    }


    // ================================================================
    //  Helpers
    // ================================================================
    private boolean isAlreadyCustomGhast(Entity ghast) {
        return ghast.getPersistentDataContainer().has(invertedGhast.getInvertedGhastKey(), PersistentDataType.BYTE) ||
                ghast.getPersistentDataContainer().has(piglinGlobo.getPiglinGloboKey(), PersistentDataType.BYTE);
    }

    private boolean isSpecialEndMob(Entity entity) {
        var pdc = entity.getPersistentDataContainer();
        return pdc.has(espectralGhast.getEnderGhastKey(), PersistentDataType.BYTE) ||
                pdc.has(espectralCreeper.getEnderCreeperKey(), PersistentDataType.BYTE) ||
                pdc.has(espectralSilverfish.getEnderSilverFishKey(), PersistentDataType.BYTE) ||
                pdc.has(bombita.getBombitaKey(), PersistentDataType.BYTE) ||
                pdc.has(corruptedZombies.getCorruptedKey(), PersistentDataType.BYTE) ||
                pdc.has(toxicSpider.getUltraCorruptedSpiderKey(), PersistentDataType.BYTE) ||
                pdc.has(corruptedSkeleton.getCorruptedKey(), PersistentDataType.BYTE);
    }
}