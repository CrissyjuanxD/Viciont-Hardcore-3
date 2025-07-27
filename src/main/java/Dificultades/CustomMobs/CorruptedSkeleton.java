package Dificultades.CustomMobs;

import items.CorruptedMobItems;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;
import org.bukkit.scheduler.BukkitRunnable;
import net.md_5.bungee.api.ChatColor;
import Handlers.DayHandler;

import java.util.*;

public class CorruptedSkeleton implements Listener {

    private final JavaPlugin plugin;
    private final NamespacedKey corruptedKey;
    private final NamespacedKey variantKey;
    private final NamespacedKey arrowVariantKey;
    private boolean eventsRegistered = false;
    private final Random random = new Random();
    private final DayHandler dayHandler;

    public enum Variant {
        LIME("#80C71F", "Lime"),
        GREEN("#3F4E1B", "Green"),
        YELLOW("#FED83E", "Yellow"),
        ORANGE("#F99039", "Orange"),
        RED("#B02E26", "Red");

        private final String hexColor;
        private final String displayName;

        Variant(String hexColor, String displayName) {
            this.hexColor = hexColor;
            this.displayName = displayName;
        }

        public String getHexColor() {
            return hexColor;
        }

        public String getDisplayName() {
            return ChatColor.BOLD + displayName;
        }
    }

    public CorruptedSkeleton(JavaPlugin plugin, DayHandler handler) {
        this.plugin = plugin;
        this.corruptedKey = new NamespacedKey(plugin, "corrupted_skeleton");
        this.variantKey = new NamespacedKey(plugin, "corrupted_variant");
        this.arrowVariantKey = new NamespacedKey(plugin, "arrow_variant");
        this.dayHandler = handler;
        apply();
    }

    public void apply() {
        if (!eventsRegistered) {
            plugin.getServer().getPluginManager().registerEvents(this, plugin);
            eventsRegistered = true;
        }
    }

    public void revert() {
        eventsRegistered = false;
    }

    public Skeleton spawnCorruptedSkeleton(Location location, String variantName) {
        World world = location.getWorld();
        if (world == null) return null;

        Skeleton skeleton = (Skeleton) world.spawnEntity(location, EntityType.SKELETON);

        Variant variant = null;
        if (variantName != null) {
            try {
                variant = Variant.valueOf(variantName.toUpperCase());
            } catch (IllegalArgumentException e) {
                Bukkit.getLogger().warning("Variante no válida: " + variantName);
            }
        }

        if (variant == null) {
            variant = getRandomVariant();
        }

        applyCorruptedSkeletonAttributes(skeleton, variant);
        equipSkeleton(skeleton, variant);
        return skeleton;
    }

    private void applyCorruptedSkeletonAttributes(Skeleton skeleton, Variant variant) {
        ChatColor color = ChatColor.of(variant.getHexColor());
        skeleton.setCustomName(color + variant.getDisplayName() + " " + ChatColor.BOLD + "Corrupted Skeleton");
        skeleton.setCustomNameVisible(true);

        double maxHealth = (variant == Variant.ORANGE || variant == Variant.RED) ? 40.0 : 20.0;
        Objects.requireNonNull(skeleton.getAttribute(Attribute.GENERIC_MAX_HEALTH)).setBaseValue(maxHealth);
        skeleton.setHealth(maxHealth);

        skeleton.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, Integer.MAX_VALUE, 0));

        PersistentDataContainer container = skeleton.getPersistentDataContainer();
        container.set(corruptedKey, PersistentDataType.BYTE, (byte) 1);
        container.set(variantKey, PersistentDataType.STRING, variant.name());
    }

    private Variant getRandomVariant() {
        return Variant.values()[random.nextInt(Variant.values().length)];
    }

    private void equipSkeleton(Skeleton skeleton, Variant variant) {
        DyeColor baseColor = DyeColor.valueOf(variant.name());
        ItemStack banner = new ItemStack(getBannerMaterial(baseColor));
        BannerMeta bannerMeta = (BannerMeta) banner.getItemMeta();
        if (bannerMeta != null) {
            List<Pattern> patterns = new ArrayList<>();
            patterns.add(new Pattern(DyeColor.PURPLE, PatternType.CIRCLE));
            patterns.add(new Pattern(DyeColor.PURPLE, PatternType.TRIANGLES_TOP));
            patterns.add(new Pattern(DyeColor.PURPLE, PatternType.TRIANGLES_BOTTOM));
            bannerMeta.setPatterns(patterns);
            banner.setItemMeta(bannerMeta);
        }
        Objects.requireNonNull(skeleton.getEquipment()).setHelmet(banner);

        ItemStack bow = new ItemStack(Material.BOW);
        ItemMeta bowMeta = bow.getItemMeta();
        if (bowMeta != null) {
            switch (variant) {
                case LIME:
                    bowMeta.addEnchant(Enchantment.POWER, 1, true);
                    break;
                case GREEN:
                    bowMeta.addEnchant(Enchantment.POWER, 2, true);
                    break;
                case YELLOW:
                    bowMeta.addEnchant(Enchantment.POWER, 5, true);
                    bowMeta.addEnchant(Enchantment.PUNCH, 20, true);
                    break;
                case ORANGE:
                    bowMeta.addEnchant(Enchantment.POWER, 5, true);
                    break;
                case RED:
                    bowMeta.addEnchant(Enchantment.POWER, 15, true);
                    break;
            }
            bow.setItemMeta(bowMeta);
        }
        skeleton.getEquipment().setItemInMainHand(bow);

        ItemStack arrows = new ItemStack(Material.TIPPED_ARROW, 64);
        PotionMeta arrowMeta = (PotionMeta) arrows.getItemMeta();
        if (arrowMeta != null) {
            arrowMeta.addCustomEffect(new PotionEffect(PotionEffectType.INSTANT_DAMAGE, 1, 0), true);
            arrows.setItemMeta(arrowMeta);
        }
        skeleton.getEquipment().setItemInOffHand(arrows);

        if (variant == Variant.RED) {
            ItemStack netheriteAxe = new ItemStack(Material.NETHERITE_AXE);
            ItemMeta axeMeta = netheriteAxe.getItemMeta();
            if (axeMeta != null) {
                axeMeta.addEnchant(Enchantment.SHARPNESS, 5, true);
                axeMeta.addEnchant(Enchantment.KNOCKBACK, 2, true);
                netheriteAxe.setItemMeta(axeMeta);
            }
            skeleton.getPersistentDataContainer().set(
                    new NamespacedKey(plugin, "netherite_axe"),
                    PersistentDataType.BYTE,
                    (byte) 1
            );
        }

        skeleton.getEquipment().setChestplate(createArmorPiece(Material.LEATHER_CHESTPLATE, variant, 8, 2, EquipmentSlot.CHEST)); // +8 Armor, +2 Toughness
        skeleton.getEquipment().setLeggings(createArmorPiece(Material.LEATHER_LEGGINGS, variant, 6, 2, EquipmentSlot.LEGS));    // +6 Armor, +2 Toughness
        skeleton.getEquipment().setBoots(createArmorPiece(Material.LEATHER_BOOTS, variant, 3, 2, EquipmentSlot.FEET));          // +3 Armor, +2 Toughness
    }

    private ItemStack createArmorPiece(Material material, Variant variant, double armor, double toughness, EquipmentSlot slot) {
        ItemStack armorPiece = new ItemStack(material);
        LeatherArmorMeta meta = (LeatherArmorMeta) armorPiece.getItemMeta();
        if (meta != null) {
            String hex = variant.getHexColor().substring(1);
            int r = Integer.parseInt(hex.substring(0, 2), 16);
            int g = Integer.parseInt(hex.substring(2, 4), 16);
            int b = Integer.parseInt(hex.substring(4, 6), 16);

            meta.setColor(Color.fromRGB(r, g, b));
            meta.setDisplayName("ArmorCorruptedSkeleton");
            meta.addEnchant(Enchantment.PROTECTION, 2, true);

            if (armor > 0) {
                meta.addAttributeModifier(
                        Attribute.GENERIC_ARMOR,
                        new AttributeModifier(
                                UUID.randomUUID(), "generic.armor", armor,
                                AttributeModifier.Operation.ADD_NUMBER, slot
                        )
                );
            }
            if (toughness > 0) {
                meta.addAttributeModifier(
                        Attribute.GENERIC_ARMOR_TOUGHNESS,
                        new AttributeModifier(
                                UUID.randomUUID(), "generic.armor_toughness", toughness,
                                AttributeModifier.Operation.ADD_NUMBER, slot
                        )
                );
            }

            armorPiece.setItemMeta(meta);
        }
        return armorPiece;
    }

    private Material getBannerMaterial(DyeColor color) {
        switch (color) {
            case LIME: return Material.LIME_BANNER;
            case GREEN: return Material.GREEN_BANNER;
            case YELLOW: return Material.YELLOW_BANNER;
            case ORANGE: return Material.ORANGE_BANNER;
            case RED: return Material.RED_BANNER;
            default: return Material.BLACK_BANNER;
        }
    }

    @EventHandler
    public void onEntityShootBow(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Skeleton)) return;
        Skeleton shooter = (Skeleton) event.getEntity();
        PersistentDataContainer container = shooter.getPersistentDataContainer();
        if (!container.has(corruptedKey, PersistentDataType.BYTE)) return;

        if (!container.has(variantKey, PersistentDataType.STRING)) return;
        String variantName = container.get(variantKey, PersistentDataType.STRING);
        if (variantName == null) return;

        if (event.getProjectile() instanceof Arrow) {
            Arrow arrow = (Arrow) event.getProjectile();
            arrow.getPersistentDataContainer().set(arrowVariantKey, PersistentDataType.STRING, variantName);

            Variant variant = Variant.valueOf(variantName);
            if (arrow instanceof TippedArrow) {
                TippedArrow tippedArrow = (TippedArrow) arrow;
                switch (variant) {
                    case LIME, GREEN, YELLOW:
                        tippedArrow.addCustomEffect(new PotionEffect(PotionEffectType.INSTANT_DAMAGE, 1, 0), true);
                        arrow.setDamage(4.0);
                        break;
                    case ORANGE:
                    case RED:
                        arrow.setCustomName("Explosive Arrow");
                        break;
                }
            }
        }
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Arrow)) return;
        Arrow arrow = (Arrow) event.getEntity();
        PersistentDataContainer container = arrow.getPersistentDataContainer();
        if (!container.has(arrowVariantKey, PersistentDataType.STRING)) return;
        String variantName = container.get(arrowVariantKey, PersistentDataType.STRING);
        if (variantName == null) return;

        Variant variant = Variant.valueOf(variantName);

        if (event.getHitEntity() instanceof LivingEntity) {
            LivingEntity target = (LivingEntity) event.getHitEntity();
            applyArrowEffects(target, variant, arrow.getLocation(), arrow);
        }

    }

    private void applyArrowEffects(LivingEntity target, Variant variant, Location impactLocation, Arrow arrow) {
        boolean isShielding = target instanceof Player && ((Player) target).isBlocking();
        switch (variant) {
            case LIME:
                if (!isShielding) {
                    target.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 5 * 20, 9));
                }
                break;
            case GREEN:
                if (!isShielding) {
                    target.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 5 * 20, 9));
                }
                break;
            case YELLOW:
                if (!isShielding) {
                    target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 5 * 20, 9));
                }
                break;
            case ORANGE:
                if (!isShielding) {
                    Objects.requireNonNull(impactLocation.getWorld()).createExplosion(impactLocation, 2f, false, false);
                }
                break;
            case RED:
                if (!isShielding && arrow.getShooter() instanceof Skeleton) {
                    Skeleton shooter = (Skeleton) arrow.getShooter();

                    Location teleportLocation = shooter.getLocation().clone()
                            .add(shooter.getLocation().getDirection().multiply(-1))
                            .setDirection(target.getLocation().getDirection());

                    teleportLocation.setY(shooter.getLocation().getY());
                    if (teleportLocation.getBlock().getType().isSolid()) {
                        teleportLocation.add(0, 1, 0);
                    }

                    target.getWorld().spawnParticle(Particle.PORTAL, target.getLocation(), 100);
                    target.getWorld().playSound(target.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.5f);

                    target.teleport(teleportLocation);
                    target.getWorld().spawnParticle(Particle.PORTAL, teleportLocation, 5);

                    double distance = target.getLocation().distance(shooter.getLocation());
                    if (distance <= 4) {
                        equipMeleeWeapon(shooter);

                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                if (shooter.isValid() && !shooter.isDead()) {
                                    if (shooter.getLocation().distance(target.getLocation()) > 4) {
                                        equipBow(shooter);
                                        this.cancel();
                                    }
                                } else {
                                    this.cancel();
                                }
                            }
                        }.runTaskTimer(plugin, 0, 20);
                    } else {
                        equipBow(shooter);
                    }
                }
                break;
        }
    }

    private void equipMeleeWeapon(Skeleton skeleton) {
        if (!skeleton.getPersistentDataContainer().has(
                new NamespacedKey(plugin, "netherite_axe"),
                PersistentDataType.BYTE
        )) return;

        ItemStack netheriteAxe = new ItemStack(Material.NETHERITE_AXE);
        ItemMeta axeMeta = netheriteAxe.getItemMeta();
        if (axeMeta != null) {
            axeMeta.addEnchant(Enchantment.SHARPNESS, 5, true);
            axeMeta.addEnchant(Enchantment.KNOCKBACK, 2, true);
            axeMeta.setUnbreakable(true);
            netheriteAxe.setItemMeta(axeMeta);
        }

        Objects.requireNonNull(skeleton.getEquipment()).setItemInMainHand(netheriteAxe);
        skeleton.getEquipment().setItemInMainHandDropChance(0.0f);
    }

    private void equipBow(Skeleton skeleton) {
        ItemStack bow = new ItemStack(Material.BOW);
        ItemMeta bowMeta = bow.getItemMeta();
        if (bowMeta != null) {
            bowMeta.addEnchant(Enchantment.POWER, 15, true);
            bowMeta.addEnchant(Enchantment.INFINITY, 1, true);
            bowMeta.setUnbreakable(true);
            bow.setItemMeta(bowMeta);
        }

        Objects.requireNonNull(skeleton.getEquipment()).setItemInMainHand(bow);
        skeleton.getEquipment().setItemInMainHandDropChance(0.0f);

        if (skeleton.getEquipment().getItemInOffHand().getType() != Material.ARROW) {
            skeleton.getEquipment().setItemInOffHand(new ItemStack(Material.ARROW));
        }
    }

    @EventHandler
    public void onCorruptedSkeletonDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Skeleton)) return;

        Skeleton skeleton = (Skeleton) event.getEntity();
        PersistentDataContainer container = skeleton.getPersistentDataContainer();

        if (!container.has(corruptedKey, PersistentDataType.BYTE)) return;

        event.getDrops().clear();

        double baseDropChance = 0.50;
        double lootingBonus = 0;
        double doubleDropChance = 0;

        if (skeleton.getKiller() != null) {
            ItemStack weapon = skeleton.getKiller().getInventory().getItemInMainHand();
            if (weapon != null && weapon.getEnchantments().containsKey(Enchantment.LOOTING)) {
                int lootingLevel = weapon.getEnchantmentLevel(Enchantment.LOOTING);

                switch (lootingLevel) {
                    case 1:
                        lootingBonus = 0.10;
                        break;
                    case 2:
                        lootingBonus = 0.20;
                        break;
                    case 3:
                        lootingBonus = 0.25;
                        doubleDropChance = 0.30;
                        break;
                }
            }
        }

        double totalDropChance = baseDropChance + lootingBonus;

        if (Math.random() <= totalDropChance) {
            if (container.has(variantKey, PersistentDataType.STRING)) {
                String variantName = container.get(variantKey, PersistentDataType.STRING);
                CorruptedMobItems.BoneVariant variant = CorruptedMobItems.BoneVariant.valueOf(variantName);
                ItemStack bone = CorruptedMobItems.createCorruptedBone(variant);

                if (doubleDropChance > 0 && Math.random() <= doubleDropChance) {
                    bone.setAmount(2);
                }

                event.getDrops().add(bone);
            }
        }

        skeleton.getWorld().playSound(skeleton.getLocation(), Sound.ENTITY_SKELETON_DEATH, 1f, 0.6f);
    }

    public void startTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (World world : Bukkit.getWorlds()) {
                    for (Entity entity : world.getEntitiesByClass(Skeleton.class)) {
                        Skeleton sk = (Skeleton) entity;
                        sk.getPersistentDataContainer().has(corruptedKey, PersistentDataType.BYTE);
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 100L);
    }

    public NamespacedKey getCorruptedKey() {
        return corruptedKey;
    }
}