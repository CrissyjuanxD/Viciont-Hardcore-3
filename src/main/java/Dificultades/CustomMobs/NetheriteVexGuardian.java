package Dificultades.CustomMobs;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.Team;
import net.md_5.bungee.api.ChatColor;

public class NetheriteVexGuardian implements Listener {
    private final JavaPlugin plugin;
    private final NamespacedKey vexGuardianKey;
    private boolean eventsRegistered = false;

    public NetheriteVexGuardian(JavaPlugin plugin) {
        this.plugin = plugin;
        this.vexGuardianKey = new NamespacedKey(plugin, "netherite_vex_guardian");
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
                    if  (entity instanceof Vex vex && isNetheriteVexGuardian(vex)) {
                        vex.remove();
                    }
                }
            }
            eventsRegistered = false;
        }
    }

    public Vex spawnNetheriteVexGuardian(Location location) {
        Vex vex = (Vex) location.getWorld().spawnEntity(location, EntityType.VEX);
        applyVexGuardianAttributes(vex);
        return vex;
    }

    private void applyVexGuardianAttributes(Vex vex) {
        vex.setCustomName(ChatColor.of("#B87333") + "" + ChatColor.BOLD + "Netherite Vex Guardian");
        vex.setCustomNameVisible(false);
        vex.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(10);
        vex.setHealth(10);
        vex.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(6);
        vex.getAttribute(Attribute.GENERIC_FOLLOW_RANGE).setBaseValue(32);

        ItemStack ironSword = new ItemStack(Material.IRON_SWORD);
        ItemMeta swordMeta = ironSword.getItemMeta();
        swordMeta.addEnchant(Enchantment.KNOCKBACK, 3, true);
        swordMeta.addEnchant(Enchantment.FIRE_ASPECT, 1, true);
        ironSword.setItemMeta(swordMeta);
        vex.getEquipment().setItemInMainHand(ironSword);

        vex.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, Integer.MAX_VALUE, 0));
        vex.getPersistentDataContainer().set(vexGuardianKey, PersistentDataType.BYTE, (byte) 1);
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Vex vex) || !isNetheriteVexGuardian(vex)) {
            return;
        }
        if (event.getEntity() instanceof Player player) {
            player.setFireTicks(Integer.MAX_VALUE);
        }
    }

    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Vex vex) || !isNetheriteVexGuardian(vex)) {
            return;
        }

        event.getDrops().clear();
        event.setDroppedExp(40);

        vex.getWorld().spawnParticle(Particle.END_ROD, vex.getLocation(), 50, 0.5, 0.5, 0.5, 0.1);
        vex.getWorld().playSound(vex.getLocation(), Sound.ENTITY_VEX_DEATH, 5.0f, 0.6f);
    }

    @EventHandler
    public void onNetheriteVexGuardianHurt(EntityDamageEvent event) {
        if (event.getEntity() instanceof Vex vex && isNetheriteVexGuardian(vex)) {
            vex.getWorld().playSound(vex.getLocation(), Sound.ENTITY_VEX_HURT, SoundCategory.HOSTILE, 1.0f, 0.6f);
        }
    }

    public NamespacedKey getVexGuardianKey() {
        return vexGuardianKey;
    }

    public boolean isNetheriteVexGuardian(Vex vex) {
        return vex.getPersistentDataContainer().has(vexGuardianKey, PersistentDataType.BYTE);
    }
}