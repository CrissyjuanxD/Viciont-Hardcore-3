package items;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class EconomyIceTotem implements Listener {

    private final JavaPlugin plugin;
    private final NamespacedKey iceTotemKey;

    public EconomyIceTotem(JavaPlugin plugin) {
        this.plugin = plugin;
        this.iceTotemKey = new NamespacedKey(plugin, "ice_totem");
    }

    public ItemStack createIceTotem() {
        ItemStack totem = new ItemStack(Material.TOTEM_OF_UNDYING);
        ItemMeta meta = totem.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ChatColor.of("#00ccff") + ChatColor.BOLD.toString() + "Ice Totem");

            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(ChatColor.of("#99ffff") + "Este tótem te otorgará:");
            lore.add(ChatColor.of("#99ffff") + "Resistencia I " + ChatColor.GRAY + "(10 segundos)");
            lore.add("");
            lore.add(ChatColor.of("#00ccff") + "Al activarse, crea una explosión glacial que:");
            lore.add(ChatColor.of("#00ccff") + "• " + ChatColor.of("#99ffff") + "Empuja suavemente a los mobs hostiles");
            lore.add(ChatColor.of("#00ccff") + "• " + ChatColor.of("#99ffff") + "Inflige 5 de daño");
            lore.add(ChatColor.of("#00ccff") + "• " + ChatColor.of("#99ffff") + "Aplica Congelación I (10 segundos)");
            lore.add("");
            meta.setLore(lore);

            meta.setCustomModelData(6);
            PersistentDataContainer data = meta.getPersistentDataContainer();
            data.set(iceTotemKey, PersistentDataType.BYTE, (byte) 1);

            totem.setItemMeta(meta);
        }
        return totem;
    }

    public boolean isIceTotem(ItemStack item) {
        if (item == null || item.getType() != Material.TOTEM_OF_UNDYING) return false;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;

        PersistentDataContainer data = meta.getPersistentDataContainer();
        return data.has(iceTotemKey, PersistentDataType.BYTE);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onTotemUse(EntityResurrectEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();

        ItemStack usedTotem = null;

        if (isIceTotem(player.getInventory().getItemInMainHand())) {
            usedTotem = player.getInventory().getItemInMainHand();
        } else if (isIceTotem(player.getInventory().getItemInOffHand())) {
            usedTotem = player.getInventory().getItemInOffHand();
        }

        if (usedTotem == null) return;

        new BukkitRunnable() {
            @Override
            public void run() {
                player.addPotionEffect(new PotionEffect(
                        PotionEffectType.RESISTANCE,
                        20 * 10,
                        0,
                        true,
                        true
                ));

                player.getWorld().playSound(player.getLocation(), Sound.BLOCK_GLASS_BREAK, 1.5f, 0.8f);
                player.getWorld().playSound(player.getLocation(), Sound.BLOCK_GLASS_HIT, 1.2f, 0.6f);
                player.getWorld().spawnParticle(Particle.SNOWFLAKE, player.getLocation(), 30);
                player.getWorld().spawnParticle(Particle.ITEM_SNOWBALL, player.getLocation(), 20);

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        createIceWave(player);
                    }
                }.runTaskLater(plugin, 5L);
            }
        }.runTaskLater(plugin, 1L);
    }

    private void createIceWave(Player player) {
        Location center = player.getLocation();
        World world = player.getWorld();

        world.playSound(center, Sound.BLOCK_GLASS_BREAK, 1.8f, 0.5f);
        world.playSound(center, Sound.BLOCK_GLASS_HIT, 1.5f, 0.3f);
        world.playSound(center, Sound.ITEM_TRIDENT_RIPTIDE_3, 1.2f, 1.0f);

        for (int radius = 0; radius <= 15; radius++) {
            final int currentRadius = radius;
            new BukkitRunnable() {
                @Override
                public void run() {
                    for (int i = 0; i < 20; i++) {
                        double angle = 2 * Math.PI * i / 20;
                        double x = currentRadius * Math.cos(angle);
                        double z = currentRadius * Math.sin(angle);
                        Location particleLoc = center.clone().add(x, 0.5, z);

                        if (currentRadius % 3 == 0) {
                            world.spawnParticle(Particle.SNOWFLAKE, particleLoc, 1, 0, 0, 0, 0.03);
                        }
                        world.spawnParticle(Particle.ITEM_SNOWBALL, particleLoc, 1, 0, 0.1, 0, 0.01);

                        Particle.DustOptions blueDust = new Particle.DustOptions(Color.fromRGB(173, 216, 230), 1.0f);
                        world.spawnParticle(Particle.DUST, particleLoc, 1, 0, 0.05, 0, blueDust);
                    }
                }
            }.runTaskLater(plugin, radius * 2L);
        }

        for (Entity entity : world.getNearbyEntities(center, 15, 15, 15)) {
            if (entity instanceof LivingEntity livingEntity && !(entity instanceof Player)) {
                if (isHostileOrNeutral(livingEntity)) {
                    Vector direction = entity.getLocation().toVector()
                            .subtract(center.toVector())
                            .normalize()
                            .multiply(0.8)
                            .setY(0.3);

                    livingEntity.setVelocity(direction);

                    // Aplicar daño
                    livingEntity.damage(5);

                    livingEntity.setFreezeTicks(200);

                    livingEntity.addPotionEffect(new PotionEffect(
                            PotionEffectType.SLOWNESS,
                            20 * 10,
                            0,
                            true,
                            true
                    ));

                    // Efecto visual en el mob afectado
                    world.spawnParticle(Particle.SNOWFLAKE, livingEntity.getLocation().add(0, 1, 0), 15);
                    world.spawnParticle(Particle.ITEM_SNOWBALL, livingEntity.getLocation().add(0, 0.5, 0), 10);

                    // Crear partículas de hielo alrededor del mob
                    Particle.DustOptions iceDust = new Particle.DustOptions(Color.fromRGB(200, 230, 255), 1.5f);
                    world.spawnParticle(Particle.DUST, livingEntity.getLocation().add(0, 1, 0), 20, 0.5, 1, 0.5, iceDust);
                }
            }
        }

        // Efecto final: crear una capa de escarcha en el suelo
        new BukkitRunnable() {
            @Override
            public void run() {
                for (int x = -3; x <= 3; x++) {
                    for (int z = -3; z <= 3; z++) {
                        Location frostLoc = center.clone().add(x, 0, z);
                        if (frostLoc.getBlock().getType().isSolid()) {
                            world.spawnParticle(Particle.WHITE_ASH, frostLoc.add(0, 0.1, 0), 2, 0.1, 0, 0.1, 0.01);
                        }
                    }
                }
            }
        }.runTaskLater(plugin, 10L);
    }

    private boolean isHostileOrNeutral(LivingEntity entity) {
        return entity instanceof Monster ||
                entity instanceof Slime ||
                entity instanceof Phantom ||
                entity instanceof Bee;
    }
}
