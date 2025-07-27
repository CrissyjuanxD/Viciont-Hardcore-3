package Dificultades.CustomMobs;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class BruteImperial implements Listener {
    private final JavaPlugin plugin;
    private final NamespacedKey bruteImperialKey;
    private boolean eventsRegistered = false;

    public BruteImperial(JavaPlugin plugin) {
        this.plugin = plugin;
        this.bruteImperialKey = new NamespacedKey(plugin, "brute_imperial");
    }

    public void apply() {
        if (!eventsRegistered) {
            Bukkit.getPluginManager().registerEvents(this, plugin);
            eventsRegistered = true;
        }
    }

    public void revert() {
        if (eventsRegistered) {
            for (World world : Bukkit.getWorlds()) {
                for (Entity entity : world.getEntities()) {
                    if (entity instanceof PiglinBrute brute && isBruteImperial(brute)) {
                        brute.remove();
                    }
                }
            }
            eventsRegistered = false;
        }
    }

    public PiglinBrute spawnBruteImperial(Location location) {
        PiglinBrute bruteImperial = (PiglinBrute) location.getWorld().spawnEntity(location, EntityType.PIGLIN_BRUTE);
        applyBruteImperialAttributes(bruteImperial);
        return bruteImperial;
    }

    public void transformToBruteImperial(PiglinBrute brute) {
        applyBruteImperialAttributes(brute);
    }

    private void applyBruteImperialAttributes(PiglinBrute brute) {
        brute.setCustomName(ChatColor.YELLOW + "" + ChatColor.BOLD + "Brute Imperial");
        brute.setCustomNameVisible(true);

        brute.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(50);
        brute.setHealth(50);
        brute.getAttribute(Attribute.GENERIC_FOLLOW_RANGE).setBaseValue(50);
        brute.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(8);

        brute.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, PotionEffect.INFINITE_DURATION, 0, true, false));
        brute.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, PotionEffect.INFINITE_DURATION, 1, true, false));
        brute.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, PotionEffect.INFINITE_DURATION, 1, true, false));

        equipGoldenArmor(brute);
        equipGoldenAxe(brute);

        brute.getPersistentDataContainer().set(bruteImperialKey, PersistentDataType.BYTE, (byte) 1);
    }

    private void equipGoldenArmor(PiglinBrute brute) {
        ItemStack helmet = new ItemStack(Material.GOLDEN_HELMET);
        ItemStack chestplate = new ItemStack(Material.GOLDEN_CHESTPLATE);
        ItemStack leggings = new ItemStack(Material.GOLDEN_LEGGINGS);
        ItemStack boots = new ItemStack(Material.GOLDEN_BOOTS);

        for (ItemStack armor : new ItemStack[]{helmet, chestplate, leggings, boots}) {
            armor.addUnsafeEnchantment(Enchantment.PROTECTION, 2);
            ItemMeta meta = armor.getItemMeta();
            meta.setUnbreakable(true);
            armor.setItemMeta(meta);
        }

        brute.getEquipment().setHelmet(helmet);
        brute.getEquipment().setChestplate(chestplate);
        brute.getEquipment().setLeggings(leggings);
        brute.getEquipment().setBoots(boots);

        brute.getEquipment().setHelmetDropChance(0);
        brute.getEquipment().setChestplateDropChance(0);
        brute.getEquipment().setLeggingsDropChance(0);
        brute.getEquipment().setBootsDropChance(0);
    }

    private void equipGoldenAxe(PiglinBrute brute) {
        ItemStack goldenAxe = new ItemStack(Material.GOLDEN_AXE);
        goldenAxe.addUnsafeEnchantment(Enchantment.SHARPNESS, 5);

        ItemMeta meta = goldenAxe.getItemMeta();
        meta.setUnbreakable(true);
        goldenAxe.setItemMeta(meta);

        brute.getEquipment().setItemInMainHand(goldenAxe);
        brute.getEquipment().setItemInMainHandDropChance(0);
    }

    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof PiglinBrute brute && isBruteImperial(brute)) {

            event.getDrops().add(new ItemStack(Material.GOLDEN_APPLE, 3));
            event.getDrops().add(new ItemStack(Material.GOLD_INGOT, 5));

            Location loc = brute.getLocation();
            loc.getWorld().playSound(loc, Sound.ENTITY_PIGLIN_BRUTE_DEATH, 1.5f, 0.8f);
            loc.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, loc, 30);
        }
    }

    public NamespacedKey getBruteImperialKey() {
        return bruteImperialKey;
    }

    public boolean isBruteImperial(PiglinBrute brute) {
        return brute.getPersistentDataContainer().has(bruteImperialKey, PersistentDataType.BYTE);
    }
}