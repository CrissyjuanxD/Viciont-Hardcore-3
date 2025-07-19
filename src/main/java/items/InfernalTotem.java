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
import vct.hardcore3.ViciontHardcore3;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class InfernalTotem implements Listener {

    private final JavaPlugin plugin;
    private final NamespacedKey infernalTotemKey;

    public InfernalTotem(JavaPlugin plugin) {
        this.plugin = plugin;
        this.infernalTotemKey = new NamespacedKey(plugin, "infernal_totem");
    }

    public ItemStack createInfernalTotem() {
        ItemStack totem = new ItemStack(Material.TOTEM_OF_UNDYING);
        ItemMeta meta = totem.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ChatColor.of("#ff3300") + ChatColor.BOLD.toString() + "Infernal Totem");

            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(ChatColor.of("#ff9966") + "Este tótem te otorgará:");
            lore.add(ChatColor.of("#ff3300") + "• " + ChatColor.of("#ff9966") + "Absorción 3 " + ChatColor.GRAY + "(15 segundos)");
            lore.add(ChatColor.of("#ff3300") + "• " + ChatColor.of("#ff9966") + "Resistencia II " + ChatColor.GRAY + "(5 segundos)");
            lore.add("");
            lore.add(ChatColor.of("#ff3300") + "Al activarse, crea una explosión infernal que:");
            lore.add(ChatColor.of("#ff3300") + "• " + ChatColor.of("#ff9966") + "Empuja mobs hostiles");
            lore.add(ChatColor.of("#ff3300") + "• " + ChatColor.of("#ff9966") + "Inflige 5 de daño");
            lore.add(ChatColor.of("#ff3300") + "• " + ChatColor.of("#ff9966") + "Aplica Wither I (10 segundos)");
            lore.add("");
            meta.setLore(lore);

            meta.setCustomModelData(5); // Usa un CustomModelData diferente
            PersistentDataContainer data = meta.getPersistentDataContainer();
            data.set(infernalTotemKey, PersistentDataType.BYTE, (byte) 1);

            totem.setItemMeta(meta);
        }
        return totem;
    }

    public boolean isInfernalTotem(ItemStack item) {
        if (item == null || item.getType() != Material.TOTEM_OF_UNDYING) return false;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;

        PersistentDataContainer data = meta.getPersistentDataContainer();
        return data.has(infernalTotemKey, PersistentDataType.BYTE);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onTotemUse(EntityResurrectEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();

        ItemStack usedTotem = null;

        if (isInfernalTotem(player.getInventory().getItemInMainHand())) {
            usedTotem = player.getInventory().getItemInMainHand();
        } else if (isInfernalTotem(player.getInventory().getItemInOffHand())) {
            usedTotem = player.getInventory().getItemInOffHand();
        }

        if (usedTotem == null) return;

        // Guardar efectos actuales antes de que el totem los elimine
        Collection<PotionEffect> currentEffects = new ArrayList<>(player.getActivePotionEffects());

        new BukkitRunnable() {
            @Override
            public void run() {
                // Restaurar efectos originales
                for (PotionEffect effect : currentEffects) {
                    player.addPotionEffect(effect);
                }

                // Aplicar nuevos efectos
                player.addPotionEffect(new PotionEffect(
                        PotionEffectType.ABSORPTION,
                        20 * 15, // 15 segundos
                        2, // Nivel 3 (0-based)
                        true,
                        true
                ));

                player.addPotionEffect(new PotionEffect(
                        PotionEffectType.RESISTANCE,
                        20 * 5, // 5 segundos
                        1, // Nivel II (0-based)
                        true,
                        true
                ));

                // Efecto de activación visual
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1.5f, 0.8f);
                player.getWorld().spawnParticle(Particle.LAVA, player.getLocation(), 20);
                player.getWorld().spawnParticle(Particle.FLAME, player.getLocation(), 50);

                // Onda expansiva después de 1 segundo (20 ticks)
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        createInfernalWave(player);
                    }
                }.runTaskLater(plugin, 5L);
            }
        }.runTaskLater(plugin, 1L);
    }

    private void createInfernalWave(Player player) {
        Location center = player.getLocation();
        World world = player.getWorld();

        // Sonido de explosión infernal
        world.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.7f);
        world.playSound(center, Sound.ENTITY_WITHER_SHOOT, 1.5f, 1.0f);

        // Efecto visual de onda expansiva
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

                        // Partículas diferentes según la distancia
                        if (currentRadius % 3 == 0) {
                            world.spawnParticle(Particle.FLAME, particleLoc, 1, 0, 0, 0, 0.05);
                        }
                        world.spawnParticle(Particle.LARGE_SMOKE, particleLoc, 1, 0, 0, 0, 0.02);
                        world.spawnParticle(Particle.ASH, particleLoc, 1, 0, 0.1, 0, 0.01);
                    }
                }
            }.runTaskLater(plugin, radius * 2L);
        }

        // Afectar a los mobs en un radio de 15 bloques
        for (Entity entity : world.getNearbyEntities(center, 15, 15, 15)) {
            if (entity instanceof LivingEntity livingEntity && !(entity instanceof Player)) {
                // Verificar si el mob es hostil o neutral
                if (isHostileOrNeutral(livingEntity)) {
                    // Empujar al mob
                    Vector direction = entity.getLocation().toVector()
                            .subtract(center.toVector())
                            .normalize()
                            .multiply(2.5) // Fuerza del empuje
                            .setY(1.5); // Pequeño empuje hacia arriba

                    livingEntity.setVelocity(direction);

                    // Aplicar daño
                    livingEntity.damage(6);

                    // Aplicar efecto Wither
                    livingEntity.addPotionEffect(new PotionEffect(
                            PotionEffectType.WITHER,
                            20 * 10, // 10 segundos
                            0, // Nivel I
                            true,
                            true
                    ));

                    // Efecto visual en el mob afectado
                    world.spawnParticle(Particle.SQUID_INK, livingEntity.getLocation().add(0, 1, 0), 10);
                }
            }
        }
    }

    private boolean isHostileOrNeutral(LivingEntity entity) {
        return entity instanceof Monster ||
                entity instanceof Slime ||
                entity instanceof Phantom ||
                entity instanceof Bee;
    }
}