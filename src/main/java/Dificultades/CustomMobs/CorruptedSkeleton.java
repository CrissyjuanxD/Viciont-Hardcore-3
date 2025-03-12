package Dificultades.CustomMobs;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;
import org.bukkit.scheduler.BukkitRunnable;
import net.md_5.bungee.api.ChatColor;
import vct.hardcore3.DayHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

public class CorruptedSkeleton implements Listener {

    private final JavaPlugin plugin;
    private final NamespacedKey corruptedKey;
    private final NamespacedKey variantKey;
    private final NamespacedKey arrowVariantKey;
    private boolean eventsRegistered = false;
    private final Random random = new Random();
    private final DayHandler dayHandler;

    // Posibles variantes
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
        apply(); // Registrar eventos
    }

    public void apply() {
        if (!eventsRegistered) {
            plugin.getServer().getPluginManager().registerEvents(this, plugin);
            eventsRegistered = true;
        }
    }

    public void revert() {
        // Si se desregistrasen los listeners, se debería implementar
        eventsRegistered = false;
    }

    /**
     * Spawnea un Corrupted Skeleton en la ubicación dada.
     * Se elige de forma aleatoria una variante y se le asigna:
     * - Banner en la cabeza (con los patrones solicitados)
     * - Arco con los encantamientos correspondientes
     * - Armadura de cuero personalizada
     * - Datos persistentes para identificarlo y su variante
     */
    public Skeleton spawnCorruptedSkeleton(Location location, String variantName) {
        World world = location.getWorld();
        if (world == null) return null;

        Skeleton skeleton = (Skeleton) world.spawnEntity(location, EntityType.SKELETON);

        // Obtener la variante
        Variant variant = null;
        if (variantName != null) {
            try {
                variant = Variant.valueOf(variantName.toUpperCase());
            } catch (IllegalArgumentException e) {
                // Si la variante no es válida, notificar y usar una aleatoria
                Bukkit.getLogger().warning("Variante no válida: " + variantName);
            }
        }

        // Si no se especificó una variante válida, usar la lógica del día
        if (variant == null) {
            variant = getVariantBasedOnDay(null);
        }

        // Aplicar atributos y equipar al esqueleto
        applyCorruptedSkeletonAttributes(skeleton, variant);
        equipSkeleton(skeleton, variant);
        return skeleton;
    }

    /**
     * Aplica atributos básicos y marca el esqueleto como corrupto, guardando su variante.
     * Además, asigna la salud base según la variante:
     * - LIME, GREEN y YELLOW: 20 puntos de salud (10 corazones)
     * - ORANGE y RED: 40 puntos de salud (20 corazones)
     */
    private void applyCorruptedSkeletonAttributes(Skeleton skeleton, Variant variant) {
        // Aplicar el color hexadecimal al nombre
        ChatColor color = ChatColor.of(variant.getHexColor());
        skeleton.setCustomName(color + variant.getDisplayName() + " " + ChatColor.BOLD + "Corrupted Skeleton");
        skeleton.setCustomNameVisible(true);

        // Asignar salud base según la variante
        double maxHealth = (variant == Variant.ORANGE || variant == Variant.RED) ? 40.0 : 20.0;
        skeleton.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(maxHealth);
        skeleton.setHealth(maxHealth);

        // Aplicar resistencia
        skeleton.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, Integer.MAX_VALUE, 0));

        // Marcamos en su persistent data container
        PersistentDataContainer container = skeleton.getPersistentDataContainer();
        container.set(corruptedKey, PersistentDataType.BYTE, (byte) 1);
        container.set(variantKey, PersistentDataType.STRING, variant.name());
    }

    private Variant getVariantBasedOnDay(String variantName) {
        int currentDay = dayHandler.getCurrentDay(); // Asume que DayHandler tiene un método estático para obtener el día
        Random random = new Random();

        if (variantName != null) {
            try {
                return Variant.valueOf(variantName.toUpperCase());
            } catch (IllegalArgumentException e) {
                // Si la variante no es válida, continuar con la lógica del día
            }
        }

        if (currentDay >= 16) {
            // Día 16+: todas las variantes tienen la misma probabilidad
            return Variant.values()[random.nextInt(Variant.values().length)];
        } else if (currentDay >= 10) {
            // Día 10-15: 40% LIME, GREEN, YELLOW | 30% ORANGE, RED
            int chance = random.nextInt(100);
            if (chance < 40) {
                return Variant.values()[random.nextInt(3)]; // LIME, GREEN, YELLOW
            } else if (chance < 70) {
                return Variant.ORANGE;
            } else {
                return Variant.RED;
            }
        } else if (currentDay >= 7) {
            // Día 7-9: 50% LIME, GREEN, YELLOW | 30% ORANGE | 20% RED
            int chance = random.nextInt(100);
            if (chance < 50) {
                return Variant.values()[random.nextInt(3)]; // LIME, GREEN, YELLOW
            } else if (chance < 80) {
                return Variant.ORANGE;
            } else {
                return Variant.RED;
            }
        } else {
            // Día 1-6: todas las variantes tienen la misma probabilidad
            return Variant.values()[random.nextInt(Variant.values().length)];
        }
    }

    /**
     * Equipa al esqueleto con:
     * - Un casco: un banner personalizado según la variante (con el color base y patrones en orden)
     * - Un arco con encantamientos según la variante.
     * - Armadura de cuero personalizada según la variante.
     */
    private void equipSkeleton(Skeleton skeleton, Variant variant) {
        // Crear el banner para el casco
        DyeColor baseColor = DyeColor.valueOf(variant.name());
        ItemStack banner = new ItemStack(getBannerMaterial(baseColor)); // Usar el material correcto según el color base
        BannerMeta bannerMeta = (BannerMeta) banner.getItemMeta();
        if (bannerMeta != null) {
            // Agregamos los patrones en orden:
            List<Pattern> patterns = new ArrayList<>();
            patterns.add(new Pattern(DyeColor.PURPLE, PatternType.CIRCLE));
            patterns.add(new Pattern(DyeColor.PURPLE, PatternType.TRIANGLES_TOP));
            patterns.add(new Pattern(DyeColor.PURPLE, PatternType.TRIANGLES_BOTTOM));
            bannerMeta.setPatterns(patterns);
            banner.setItemMeta(bannerMeta);
        }
        skeleton.getEquipment().setHelmet(banner);

        // Crear el arco con encantamientos según la variante
        ItemStack bow = new ItemStack(Material.BOW);
        ItemMeta bowMeta = bow.getItemMeta();
        if (bowMeta != null) {
            // Se asignan niveles según la variante
            switch (variant) {
                case LIME:
                    bowMeta.addEnchant(Enchantment.POWER, 1, true); // Power I
                    bowMeta.addEnchant(Enchantment.PUNCH, 1, true);     // Retroceso I
                    break;
                case GREEN:
                    bowMeta.addEnchant(Enchantment.POWER, 2, true); // Power II
                    bowMeta.addEnchant(Enchantment.PUNCH, 2, true);      // Retroceso II
                    break;
                case YELLOW:
                    bowMeta.addEnchant(Enchantment.POWER, 3, true); // Power III
                    bowMeta.addEnchant(Enchantment.PUNCH, 3, true);      // Retroceso III
                    break;
                case ORANGE:
                    bowMeta.addEnchant(Enchantment.POWER, 5, true); // Power V
                    bowMeta.addEnchant(Enchantment.PUNCH, 5, true);      // Retroceso V
                    break;
                case RED:
                    // Usamos niveles altos para simular Power X y Retroceso X
                    bowMeta.addEnchant(Enchantment.POWER, 10, true);
                    bowMeta.addEnchant(Enchantment.PUNCH, 10, true);
                    break;
            }
            bow.setItemMeta(bowMeta);
        }
        skeleton.getEquipment().setItemInMainHand(bow);

        // Equipar la armadura de cuero personalizada con atributos
        skeleton.getEquipment().setChestplate(createArmorPiece(Material.LEATHER_CHESTPLATE, variant, 8, 2, EquipmentSlot.CHEST)); // +8 Armor, +2 Toughness
        skeleton.getEquipment().setLeggings(createArmorPiece(Material.LEATHER_LEGGINGS, variant, 6, 2, EquipmentSlot.LEGS));    // +6 Armor, +2 Toughness
        skeleton.getEquipment().setBoots(createArmorPiece(Material.LEATHER_BOOTS, variant, 3, 2, EquipmentSlot.FEET));          // +3 Armor, +2 Toughness
    }

    private ItemStack createArmorPiece(Material material, Variant variant, double armor, double toughness, EquipmentSlot slot) {
        ItemStack armorPiece = new ItemStack(material);
        LeatherArmorMeta meta = (LeatherArmorMeta) armorPiece.getItemMeta();
        if (meta != null) {
            // Convertir HEX a Color RGB
            String hex = variant.getHexColor().substring(1);
            int r = Integer.parseInt(hex.substring(0, 2), 16);
            int g = Integer.parseInt(hex.substring(2, 4), 16);
            int b = Integer.parseInt(hex.substring(4, 6), 16);

            // Establecer color en la armadura de cuero
            meta.setColor(Color.fromRGB(r, g, b));

            // Establecer el nombre de la armadura
            meta.setDisplayName("ArmorCorruptedSkeleton");

            // Añadir encantamiento Protección II
            meta.addEnchant(Enchantment.PROTECTION, 2, true);

            // Añadir atributos de armadura y resistencia
            if (armor > 0) {
                meta.addAttributeModifier(
                        Attribute.GENERIC_ARMOR,
                        new AttributeModifier(
                                UUID.randomUUID(), "generic.armor", armor,
                                AttributeModifier.Operation.ADD_NUMBER, slot // Usar el slot correcto
                        )
                );
            }
            if (toughness > 0) {
                meta.addAttributeModifier(
                        Attribute.GENERIC_ARMOR_TOUGHNESS,
                        new AttributeModifier(
                                UUID.randomUUID(), "generic.armor_toughness", toughness,
                                AttributeModifier.Operation.ADD_NUMBER, slot // Usar el slot correcto
                        )
                );
            }

            armorPiece.setItemMeta(meta);
        }
        return armorPiece;
    }


    /**
     * Devuelve el material del banner según el color base.
     */
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

    /*
     * EVENTOS
     */

    /**
     * Cuando un esqueleto dispare su arco, si es un Corrupted Skeleton,
     * se almacena en la flecha la variante para luego aplicar el efecto en el impacto.
     */
    @EventHandler
    public void onEntityShootBow(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Skeleton)) return;
        Skeleton shooter = (Skeleton) event.getEntity();
        PersistentDataContainer container = shooter.getPersistentDataContainer();
        if (!container.has(corruptedKey, PersistentDataType.BYTE)) return; // No es corrupto

        // Recuperamos la variante
        if (!container.has(variantKey, PersistentDataType.STRING)) return;
        String variantName = container.get(variantKey, PersistentDataType.STRING);
        if (variantName == null) return;

        // Al disparar, si la flecha es Arrow, le asignamos la variante
        if (event.getProjectile() instanceof Arrow) {
            Arrow arrow = (Arrow) event.getProjectile();
            arrow.getPersistentDataContainer().set(arrowVariantKey, PersistentDataType.STRING, variantName);
        }
    }

    /**
     * Cuando la flecha impacta, si proviene de un Corrupted Skeleton,
     * se le aplica el efecto correspondiente según su variante.
     */
    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Arrow)) return;
        Arrow arrow = (Arrow) event.getEntity();
        PersistentDataContainer container = arrow.getPersistentDataContainer();
        if (!container.has(arrowVariantKey, PersistentDataType.STRING)) return;
        String variantName = container.get(arrowVariantKey, PersistentDataType.STRING);
        if (variantName == null) return;

        // Obtener la variante
        Variant variant = Variant.valueOf(variantName);

        // Aplicar efectos si impacta en una entidad
        if (event.getHitEntity() instanceof LivingEntity) {
            LivingEntity target = (LivingEntity) event.getHitEntity();
            applyArrowEffects(target, variant, arrow.getLocation());
        }

        // Aplicar explosión si impacta en un bloque (solo para ORANGE y RED)
        if (event.getHitBlock() != null && (variant == Variant.ORANGE || variant == Variant.RED)) {
            Location impactLocation = event.getHitBlock().getLocation();
            impactLocation.getWorld().createExplosion(impactLocation, variant == Variant.ORANGE ? 3f : 4f, false, false);
        }

        // Eliminar la flecha
        arrow.remove();
    }

    /**
     * Según la variante, aplica el efecto especial de la flecha sobre el objetivo.
     */
    private void applyArrowEffects(LivingEntity target, Variant variant, Location impactLocation) {
        switch (variant) {
            case LIME:
                target.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 5 * 20, 9));
                target.addPotionEffect(new PotionEffect(PotionEffectType.INSTANT_DAMAGE, 20, 0));
                break;
            case GREEN:
                target.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 5 * 20, 2));
                target.addPotionEffect(new PotionEffect(PotionEffectType.INSTANT_DAMAGE, 20, 0));
                break;
            case YELLOW:
                target.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 5 * 20, 9));
                target.addPotionEffect(new PotionEffect(PotionEffectType.INSTANT_DAMAGE, 20, 0));
                break;
            case ORANGE:
                impactLocation.getWorld().createExplosion(impactLocation, 3f, false, false);
                break;
            case RED:
                impactLocation.getWorld().createExplosion(impactLocation, 4f, false, false);
                break;
        }
    }

    /**
     * (Opcional) Una tarea que, por ejemplo, cada cierto tiempo pueda buscar
     * esqueletos corruptos y ejecutar acciones extra o animaciones.
     */
    public void startTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                // Ejemplo: iterar por todos los mundos y buscar esqueletos corruptos
                for (World world : Bukkit.getWorlds()) {
                    for (Entity entity : world.getEntitiesByClass(Skeleton.class)) {
                        Skeleton sk = (Skeleton) entity;
                        if (sk.getPersistentDataContainer().has(corruptedKey, PersistentDataType.BYTE)) {
                            // Aquí podrías agregar efectos continuos o comportamiento especial
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 100L);
    }

    public NamespacedKey getCorruptedKey() {
        return corruptedKey;
    }
}