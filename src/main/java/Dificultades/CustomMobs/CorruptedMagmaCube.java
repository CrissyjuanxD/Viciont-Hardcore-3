package Dificultades.CustomMobs;

import items.LegginsNetheriteEssence;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class CorruptedMagmaCube implements Listener {
    private final JavaPlugin plugin;
    private final NamespacedKey corruptedMagmaKey;
    private boolean eventsRegistered = false;
    private final LegginsNetheriteEssence legginsNetheriteEssence;

    public CorruptedMagmaCube(JavaPlugin plugin) {
        this.plugin = plugin;
        this.legginsNetheriteEssence = new LegginsNetheriteEssence(plugin);
        this.corruptedMagmaKey = new NamespacedKey(plugin, "corrupted_magma");
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
                    if (entity instanceof MagmaCube magma &&
                            magma.getPersistentDataContainer().has(corruptedMagmaKey, PersistentDataType.BYTE)) {
                        magma.remove();
                    }
                }
            }
            eventsRegistered = false;
        }
    }

    public MagmaCube spawnCorruptedMagmaCube(Location location) {
        MagmaCube corruptedMagma = (MagmaCube) location.getWorld().spawnEntity(location, EntityType.MAGMA_CUBE);
        applyCorruptedMagmaAttributes(corruptedMagma);
        return corruptedMagma;
    }

    public void transformMagmaCube(MagmaCube magma) {
        applyCorruptedMagmaAttributes(magma);
    }

    private void applyCorruptedMagmaAttributes(MagmaCube magma) {
        magma.setCustomName(ChatColor.DARK_RED + "" + ChatColor.BOLD + "Corrupted Magma Cube");
        magma.setCustomNameVisible(false);
        magma.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(200);
        magma.setHealth(200);
        magma.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(12);
        magma.getAttribute(Attribute.GENERIC_SCALE).setBaseValue(3.0);
        magma.getAttribute(Attribute.GENERIC_FOLLOW_RANGE).setBaseValue(50);

        magma.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, Integer.MAX_VALUE, 0));
        magma.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, Integer.MAX_VALUE, 10));
        magma.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 15));
        magma.getPersistentDataContainer().set(corruptedMagmaKey, PersistentDataType.BYTE, (byte) 1);
    }

    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof MagmaCube magma &&
                magma.getPersistentDataContainer().has(corruptedMagmaKey, PersistentDataType.BYTE)) {

            event.getDrops().clear();

            if (Math.random() <= 0.08) {
                magma.getWorld().dropItemNaturally(magma.getLocation(), legginsNetheriteEssence.createLegginsNetheriteEssence());
            }
        }
    }

    public NamespacedKey getMagmaCorruptedKey() {
        return corruptedMagmaKey;
    }

    public boolean isMagmaCorrupted(MagmaCube magma) {
        return magma.getPersistentDataContainer().has(corruptedMagmaKey, PersistentDataType.BYTE);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Location from = event.getFrom();
        Location to = event.getTo();

        if (from.getBlockX() == to.getBlockX() && from.getBlockY() == to.getBlockY() && from.getBlockZ() == to.getBlockZ()) {
            return;
        }

        Location playerLocation = player.getLocation();
        double maxDistanceSquared = 30 * 30;

        for (Entity entity : player.getNearbyEntities(30, 30, 30)) {
            if (entity instanceof MagmaCube magmaCube &&
                    magmaCube.getCustomName() != null &&
                    magmaCube.getCustomName().equals(ChatColor.DARK_RED + "" + ChatColor.BOLD + "Corrupted Magma Cube") &&
                    !magmaCube.getPersistentDataContainer().has(corruptedMagmaKey, PersistentDataType.BYTE)) {

                if (playerLocation.distanceSquared(magmaCube.getLocation()) <= maxDistanceSquared) {
                    transformMagmaCube(magmaCube);
                }
            }
        }
    }
}