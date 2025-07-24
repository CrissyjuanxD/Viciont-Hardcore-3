package Dificultades;

import Dificultades.CustomMobs.*;
import Dificultades.Features.SpectralEyeSpawner;
import Handlers.CustomBrewingRecipe;
import Handlers.DayHandler;
import items.*;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.*;
import org.bukkit.block.Biome;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.*;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public class DayThirteenChanges implements Listener {
    private final JavaPlugin plugin;
    private final DayHandler dayHandler;
    private boolean isApplied = false;
    private final Random random = new Random();

    private final SpectralEye spectralEye;
    private final EnderGhast enderGhast;
    private final EnderCreeper enderCreeper;
    private final EnderSilverfish enderSilverfish;

    private final Bombita bombita;
    private final CorruptedZombies corruptedZombies;
    private final UltraCorruptedSpider ultraCorruptedSpider;
    private final CorruptedSkeleton corruptedSkeleton;

    private final SpectralEyeSpawner spectralEyeSpawner;

    private final List<CustomBrewingRecipe> brewingRecipes = new ArrayList<>();
    private final Map<UUID, Long> cooldownPlayers = new HashMap<>();
    private final NamespacedKey explosiveShulkerKey;
    private final NamespacedKey shulkerTNTKey;


    public DayThirteenChanges(JavaPlugin plugin, DayHandler handler) {
        this.plugin = plugin;
        this.dayHandler = handler;
        this.spectralEye = new SpectralEye(plugin);
        this.enderGhast = new EnderGhast(plugin);
        this.enderCreeper = new EnderCreeper(plugin);
        this.enderSilverfish = new EnderSilverfish(plugin);

        this.bombita = new Bombita(plugin);
        this.corruptedZombies = new CorruptedZombies(plugin);
        this.ultraCorruptedSpider = new UltraCorruptedSpider(plugin);
        this.corruptedSkeleton = new CorruptedSkeleton(plugin, handler);

        this.spectralEyeSpawner = new SpectralEyeSpawner(plugin, spectralEye);

        this.explosiveShulkerKey = new NamespacedKey(plugin, "explosive_shulker");
        this.shulkerTNTKey = new NamespacedKey(plugin, "shulker_tnt");
    }

    public void apply() {
        if (!isApplied) {
            isApplied = true;
            Bukkit.getPluginManager().registerEvents(this, plugin);
            Bukkit.getPluginManager().registerEvents(spectralEyeSpawner, plugin);
            registerRecipes();
            spectralEye.apply();
            enderGhast.apply();
            enderCreeper.apply();
            enderSilverfish.apply();
        }
    }

    public void revert() {
        if (isApplied) {
            isApplied = false;
            spectralEye.revert();
            enderGhast.revert();
            enderCreeper.revert();
            enderSilverfish.revert();

            Bukkit.removeRecipe(new NamespacedKey(plugin, "guardian_powder"));
            Bukkit.removeRecipe(new NamespacedKey(plugin, "apilate_gold_block"));
            Bukkit.removeRecipe(new NamespacedKey(plugin, "ender_emblem"));
            Bukkit.removeRecipe(new NamespacedKey(plugin, "corrupted_golden_apple"));

            HandlerList.unregisterAll(this);
            HandlerList.unregisterAll(spectralEyeSpawner);
        }
    }

    //SPAWNS

    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (!isApplied) return;
        handleRandomMobConversion(event);
        handleSpectralEyeConversion(event);
    }

    private boolean isSpecialMob(Entity entity) {
        return entity.getPersistentDataContainer().has(enderGhast.getEnderGhastKey(), PersistentDataType.BYTE) ||
                entity.getPersistentDataContainer().has(enderCreeper.getEnderCreeperKey(), PersistentDataType.BYTE) ||
                entity.getPersistentDataContainer().has(enderSilverfish.getEnderSilverFishKey(), PersistentDataType.BYTE) ||
                entity.getPersistentDataContainer().has(bombita.getBombitaKey(), PersistentDataType.BYTE) ||
                entity.getPersistentDataContainer().has(corruptedZombies.getCorruptedKey(), PersistentDataType.BYTE) ||
                entity.getPersistentDataContainer().has(ultraCorruptedSpider.getUltraCorruptedSpiderKey(), PersistentDataType.BYTE) ||
                entity.getPersistentDataContainer().has(corruptedSkeleton.getCorruptedKey(), PersistentDataType.BYTE);
    }

    private void handleRandomMobConversion(CreatureSpawnEvent event) {

        if (event.getEntityType() != EntityType.ENDERMAN ||
                event.getLocation().getWorld().getEnvironment() != World.Environment.THE_END) {
            return;
        }

        // Verificar si ya es un mob especial
        if (isSpecialMob(event.getEntity())) {
            return;
        }

        Enderman enderman = (Enderman) event.getEntity();
        Location loc = enderman.getLocation();

        double randomValue = random.nextDouble();

        boolean isEnderMob = random.nextInt(9) == 0;
        boolean isOtherMob = !isEnderMob && random.nextInt(14) == 0;

        if (isEnderMob) {
            // Distribución equitativa entre los 3 ender mobs
            double enderMobChoice = random.nextDouble();
            if (enderMobChoice < 0.30) { // Ender Ghast
                enderGhast.spawnEnderGhast(loc);
            } else if (enderMobChoice < 0.70) {
                enderCreeper.spawnEnderCreeper(loc);
            } else { // Ender Silverfish
                enderSilverfish.spawnEnderSilverfish(loc);
            }
        } else if (isOtherMob) {
            // Distribución equitativa entre los otros 4 mobs
            double otherMobChoice = random.nextDouble();
            if (otherMobChoice < 0.25) { // Bombita
                bombita.spawnBombita(loc);
            } else if (otherMobChoice < 0.5) { // Corrupted Zombies
                corruptedZombies.spawnCorruptedZombie(loc);
            } else if (otherMobChoice < 0.75) { // Ultra Corrupted Spider
                ultraCorruptedSpider.spawnUltraCorruptedSpider(loc);
            } else { // Corrupted Skeleton
                corruptedSkeleton.spawnCorruptedSkeleton(loc, null);
            }
        } else {
            return; // No se convierte, es un enderman normal
        }

        enderman.remove();
    }

    private void handleSpectralEyeConversion(CreatureSpawnEvent event) {
        if (event.getEntityType() != EntityType.PHANTOM) return;

        if (event.getEntity().getPersistentDataContainer()
                .has(spectralEye.getSpectralEyeKey(), PersistentDataType.BYTE)) {
            return;
        }

        if (random.nextInt(5) != 0) return;

        Phantom phantom = (Phantom) event.getEntity();
        Location loc = phantom.getLocation();

        spectralEye.spawnSpectralEye(loc);
        phantom.remove();
    }

    //CRAFTEOS

    public void registerRecipes() {

        ShapedRecipe gpowder = new ShapedRecipe(new NamespacedKey(plugin, "guardian_powder"), BlazeItems.createGuardianBlazePowder());
        gpowder.shape("   ", " GS", "   ");
        gpowder.setIngredient('S', Material.NETHERITE_SCRAP);
        gpowder.setIngredient('G', new RecipeChoice.ExactChoice(BlazeItems.createBlazeRod()));

        ShapedRecipe apilategb = new ShapedRecipe(new NamespacedKey(plugin, "apilate_gold_block"), CorruptedGoldenApple.createApilateGoldBlock());
        apilategb.shape("   ", " GG", "   ");
        apilategb.setIngredient('G', Material.GOLD_BLOCK);

        ShapedRecipe corruptedAppleRecipe = new ShapedRecipe(new NamespacedKey(plugin, "corrupted_golden_apple"), CorruptedGoldenApple.createCorruptedGoldenApple());
        corruptedAppleRecipe.shape("GGG", "GAG", "GGG");
        corruptedAppleRecipe.setIngredient('G', new RecipeChoice.ExactChoice(CorruptedGoldenApple.createApilateGoldBlock()));
        corruptedAppleRecipe.setIngredient('A', Material.GOLDEN_APPLE);

        ShapedRecipe endemblem = new ShapedRecipe(new NamespacedKey(plugin, "ender_emblem"), EmblemItems.createEndEmblem());
        endemblem.shape("DED", "CWC", "NPN");
        endemblem.setIngredient('D', Material.DIAMOND_BLOCK);
        endemblem.setIngredient('E', new RecipeChoice.ExactChoice(EmblemItems.createNetherEmblem()));
        endemblem.setIngredient('C', new RecipeChoice.ExactChoice(CorruptedNetheriteItems.createCorruptedNetheriteIngot()));
        endemblem.setIngredient('W', new RecipeChoice.ExactChoice(EmblemItems.createcorruptedNetherStar()));
        endemblem.setIngredient('N', Material.NETHER_STAR);
        endemblem.setIngredient('P', new RecipeChoice.ExactChoice(ItemsTotems.createWhiteEnderPearl()));


        brewingRecipes.add(new CustomBrewingRecipe(
                BlazeItems.createGuardianBlazePowder(),
                potion -> potion.setItemMeta(BlazeItems.createPotionOfFireResistance().getItemMeta())
        ));

        Bukkit.addRecipe(gpowder);
        Bukkit.addRecipe(apilategb);
        Bukkit.addRecipe(endemblem);
        Bukkit.addRecipe(corruptedAppleRecipe);
    }

    @EventHandler
    public void onPlayerConsume(PlayerItemConsumeEvent event) {
        if (!isApplied) return;
        ItemStack item = event.getItem();
        if (item.isSimilar(CorruptedGoldenApple.createCorruptedGoldenApple())) {
            CorruptedGoldenApple.applyEffects(event.getPlayer());
        }
    }

    @EventHandler
    public void onBrew(BrewEvent event) {
        if (!isApplied) return;
        BrewerInventory inventory = event.getContents();
        ItemStack ingredient = inventory.getIngredient();

        brewingRecipes.stream()
                .filter(recipe -> recipe.matches(ingredient))
                .findFirst()
                .ifPresent(recipe -> {
                    for (int i = 0; i < 3; i++) {
                        ItemStack item = inventory.getItem(i);
                        if (item != null && item.getType() == Material.POTION) {
                            recipe.applyResult(item);
                        }
                    }
                });
    }

    //CAMBIOS

    @EventHandler
    public void onShulkerShoot(EntityShootBowEvent event) {
        if (!isApplied) return;
        if (!(event.getEntity() instanceof Shulker)) return;

        if (event.getProjectile() instanceof ShulkerBullet bullet) {
            // Marcar el proyectil como especial usando PersistentDataContainer
            bullet.getPersistentDataContainer().set(
                    new NamespacedKey(plugin, "explosive_shulker_bullet"),
                    PersistentDataType.BYTE,
                    (byte) 1
            );
        }
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!isApplied) return;
        if (!(event.getEntity() instanceof ShulkerBullet bullet)) return;

        // Verificar si es un proyectil especial
        if (bullet.getPersistentDataContainer().has(
                new NamespacedKey(plugin, "explosive_shulker_bullet"),
                PersistentDataType.BYTE
        )) {
            // Crear explosión power 2 (que rompe bloques y daña entidades)
            bullet.getWorld().createExplosion(
                    bullet.getLocation(),
                    2.0f,
                    true,
                    true
            );
        }
    }

    @EventHandler
    public void onExplosionDamageItems(EntityDamageByEntityEvent event) {
        if (!isApplied) return;

        // 2. Proteger los ítems Shulker Shell en el suelo
        if (event.getEntity() instanceof Item item) {
            if (item.getItemStack().getType() == Material.SHULKER_SHELL) {
                // Cancelar daño si proviene de una explosión o fuego
                if (event.getDamager() instanceof TNTPrimed ||
                        event.getCause() == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION ||
                        event.getCause() == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION ||
                        event.getCause() == EntityDamageEvent.DamageCause.FIRE_TICK ||
                        event.getCause() == EntityDamageEvent.DamageCause.FIRE) {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void onItemSpawn(ItemSpawnEvent event) {
        if (!isApplied) return;

        // 3. Marcar las Shulker Shells para protección
        if (event.getEntity().getItemStack().getType() == Material.SHULKER_SHELL) {
            event.getEntity().getPersistentDataContainer().set(
                    new NamespacedKey(plugin, "protected_shulker_shell"),
                    PersistentDataType.BYTE,
                    (byte) 1
            );
        }
    }

    @EventHandler
    public void onEntityCombust(EntityCombustEvent event) {
        if (!isApplied) return;

        // 4. Prevenir que se quemen las Shulker Shells
        if (event.getEntity() instanceof Item item) {
            if (item.getItemStack().getType() == Material.SHULKER_SHELL ||
                    (item.getPersistentDataContainer().has(
                            new NamespacedKey(plugin, "protected_shulker_shell"),
                            PersistentDataType.BYTE))) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onShulkerDeath(EntityDeathEvent event) {
        if (!isApplied) return;
        if (!(event.getEntity() instanceof Shulker shulker)) return;

        // Dejar TNT al morir (con triple poder)
        Location loc = shulker.getLocation();
        TNTPrimed tnt = loc.getWorld().spawn(loc, TNTPrimed.class);
        tnt.setFuseTicks(50);
        tnt.setYield(10f);

        // Marcar la TNT como especial
        tnt.getPersistentDataContainer().set(shulkerTNTKey, PersistentDataType.BYTE, (byte) 1);

        // 30% de probabilidad de dropear Shulker Shell
        if (Math.random() > 0.40) {
            event.getDrops().removeIf(item -> item.getType() == Material.SHULKER_SHELL);
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!isApplied) return;

        if (event.getEntity() instanceof Shulker) {
            if (event.getCause() == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION ||
                    event.getCause() == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION) {
                // Reducir daño en 80%
                event.setDamage(event.getDamage() * 0.3);
            }
        }
    }


    @EventHandler
    public void onCreaturesEnderSpawn(CreatureSpawnEvent event) {
        if (!isApplied) return;

        // Endermans en The End con Fuerza II infinita
        if (event.getEntityType() == EntityType.ENDERMAN &&
                event.getLocation().getWorld().getEnvironment() == World.Environment.THE_END) {
            LivingEntity enderman = event.getEntity();
            enderman.addPotionEffect(new PotionEffect(
                    PotionEffectType.STRENGTH,
                    Integer.MAX_VALUE,
                    1,
                    false,
                    false
            ));
        }

        // Endermites en cualquier dimensión con Fuerza III infinita
        if (event.getEntityType() == EntityType.ENDERMITE) {
            LivingEntity endermite = event.getEntity();
            endermite.addPotionEffect(new PotionEffect(
                    PotionEffectType.STRENGTH,
                    Integer.MAX_VALUE,
                    2,
                    false,
                    false
            ));
        }
    }

    @EventHandler
    public void onPotionEffectApply(EntityPotionEffectEvent event) {
        if (!isApplied) return;

        // Solo aplicar a jugadores
        if (!(event.getEntity() instanceof Player)) return;

        // Remover efecto de invisibilidad en The End
        if (event.getEntity().getWorld().getEnvironment() == World.Environment.THE_END &&
                event.getNewEffect() != null &&
                event.getNewEffect().getType() == PotionEffectType.INVISIBILITY) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPortalEnter(PlayerPortalEvent event) {
        if (!isApplied) return;

        Player player = event.getPlayer();

        // Handle End Portal cases
        if (event.getCause() == PlayerTeleportEvent.TeleportCause.END_PORTAL) {
            if (event.getFrom().getWorld().getEnvironment() == World.Environment.THE_END) {
                // Traveling from End to Overworld
                if (!hasItem(player, EmblemItems.createOverworldEmblem())) {
                    event.setCancelled(true);
                    player.sendMessage(ChatColor.RED + "۞ Necesitas un Overworld Emblem para regresar al Overworld!");
                }
            } else {
                // Traveling to the End
                if (!hasItem(player, EmblemItems.createEndEmblem())) {
                    event.setCancelled(true);
                    player.sendMessage(ChatColor.RED + "۞ Necesitas un End Emblem para entrar al End!");
                }
            }
        }
    }

    public static boolean hasItem(Player player, ItemStack target) {
        return DayFourChanges.hasItem(player, target);
    }

    @EventHandler
    public void onPlayerVoidDamage(EntityDamageEvent event) {
        if (!isApplied) return;

        if (!(event.getEntity() instanceof Player player)) return;
        if (event.getCause() != EntityDamageEvent.DamageCause.VOID) return;
        if (player.getWorld().getEnvironment() != World.Environment.THE_END) return;

        // Verificar cooldown de 5 segundos
        long currentTime = System.currentTimeMillis();
        if (cooldownPlayers.containsKey(player.getUniqueId())) {
            long lastDamageTime = cooldownPlayers.get(player.getUniqueId());
            if (currentTime - lastDamageTime < 5000) {
                event.setCancelled(true);
                return;
            }
        }

        boolean tieneTotem = player.getInventory().getItemInMainHand().getType() == Material.TOTEM_OF_UNDYING ||
                player.getInventory().getItemInOffHand().getType() == Material.TOTEM_OF_UNDYING;

        if (tieneTotem) {
            cooldownPlayers.put(player.getUniqueId(), currentTime);

            event.setCancelled(true);
            player.setNoDamageTicks(0);

            player.damage(player.getHealth() + 500.0);

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isValid() && !player.isDead()) {
                    player.addPotionEffect(new PotionEffect(
                            PotionEffectType.LEVITATION,
                            3 * 20,
                            100,
                            false,
                            false
                    ));

                    player.addPotionEffect(new PotionEffect(
                            PotionEffectType.SLOW_FALLING,
                            50 * 20,
                            0,
                            false,
                            false
                    ));

                    player.playSound(player.getLocation(), Sound.ITEM_TRIDENT_THUNDER, 1.0f, 2.0f);
                }
            }, 5L);
        }
    }
}
