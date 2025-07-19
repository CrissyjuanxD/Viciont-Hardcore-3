package items;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.*;
import org.bukkit.entity.Player;
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
import vct.hardcore3.ViciontHardcore3;

import java.util.ArrayList;
import java.util.List;

public class LifeTotem implements Listener {

    private final JavaPlugin plugin;
    private final NamespacedKey lifeTotemKey;

    public LifeTotem(JavaPlugin plugin) {
        this.plugin = plugin;
        this.lifeTotemKey = new NamespacedKey(plugin, "life_totem");
    }

    public ItemStack createLifeTotem() {
        ItemStack totem = new ItemStack(Material.TOTEM_OF_UNDYING);
        ItemMeta meta = totem.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ChatColor.of("#33ccff") + ChatColor.BOLD.toString() + "Life Totem");

            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(ChatColor.of("#33ffcc") + "Este tótem te otorgará");
            lore.add(ChatColor.of("#66ffff") + ChatColor.BOLD.toString() + "Absorción 3 " + ChatColor.of("#33ffcc") + "por " + ChatColor.of("#66ffff") + ChatColor.BOLD + "15 " + ChatColor.of("#33ffcc") + "segundos.");
            lore.add("");
            lore.add(ChatColor.GRAY + ChatColor.BOLD.toString() + "Al consumirlo te tepeara a tu ultimo punto de spawn.");
            lore.add("");
            meta.setLore(lore);

            meta.setCustomModelData(3);
            PersistentDataContainer data = meta.getPersistentDataContainer();
            data.set(lifeTotemKey, PersistentDataType.BYTE, (byte) 1);

            totem.setItemMeta(meta);
        }
        return totem;
    }

    public boolean isLifeTotem(ItemStack item) {
        if (item == null || item.getType() != Material.TOTEM_OF_UNDYING) return false;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;

        PersistentDataContainer data = meta.getPersistentDataContainer();
        return data.has(lifeTotemKey, PersistentDataType.BYTE);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onTotemUse(EntityResurrectEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();

        ItemStack usedTotem = null;
        boolean isMainHand = false;

        if (isLifeTotem(player.getInventory().getItemInMainHand())) {
            usedTotem = player.getInventory().getItemInMainHand();
            isMainHand = true;
        } else if (isLifeTotem(player.getInventory().getItemInOffHand())) {
            usedTotem = player.getInventory().getItemInOffHand();
        }

        if (usedTotem == null) return;

        // Aplicar absorción inmediatamente
        player.addPotionEffect(new PotionEffect(
                PotionEffectType.ABSORPTION,
                20 * 15,
                2,
                true,
                true
        ));

        // Nueva lógica de spawn idéntica a /magictp
        new BukkitRunnable() {
            @Override
            public void run() {
                World overworld = Bukkit.getWorlds().get(0);
                // Hacer la variable final para que pueda ser usada en la clase interna
                final Location spawnLocation;

                Location bedSpawn = player.getBedSpawnLocation();
                if (bedSpawn != null) {
                    spawnLocation = bedSpawn;
                } else {
                    spawnLocation = overworld.getSpawnLocation();
                }

                Location safeSpawnLocation = findSafeLocation(spawnLocation);

                // Efectos pre-teleport
                player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.8f);
                player.spawnParticle(Particle.PORTAL, player.getLocation(), 50);

                // Teleportar
                player.teleport(safeSpawnLocation);

                // Efectos post-teleport
                player.playSound(safeSpawnLocation, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.8f);
                player.spawnParticle(Particle.PORTAL, safeSpawnLocation, 50);

                // Animación después de 2 segundos
                final Location finalLocation = safeSpawnLocation;
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        player.playSound(finalLocation, Sound.BLOCK_BEACON_ACTIVATE, 1.5f, 0.8f);
                        player.playSound(finalLocation, Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1.0f, 1.2f);
                        player.playSound(finalLocation, Sound.ITEM_TOTEM_USE, 1.0f, 1.5f);

                        for (int i = 0; i < 20; i++) {
                            double angle = 2 * Math.PI * i / 20;
                            double x = 2 * Math.cos(angle);
                            double z = 2 * Math.sin(angle);
                            Location particleLoc = finalLocation.clone().add(x, 0.5, z);
                            player.spawnParticle(Particle.SNOWFLAKE, particleLoc, 3);
                            player.spawnParticle(Particle.END_ROD, particleLoc, 1);
                        }

                        player.spawnParticle(Particle.FIREWORK, finalLocation, 30, 0.5, 0.5, 0.5, 0.2);
                    }
                }.runTaskLater(plugin, 10L);
            }
        }.runTaskLater(plugin, 1L);
    }

    private Location findSafeLocation(Location location) {
        // Asegurarse de que la ubicación es segura (no en el aire o dentro de bloques)
        location = location.clone();

        // Buscar el bloque sólido más alto
        while (location.getBlock().getType().isAir() && location.getY() > 0) {
            location.subtract(0, 1, 0);
        }

        // Ajustar a 1 bloque arriba del suelo
        location.add(0, 1, 0);

        // Verificar que la cabeza y los pies no estén en bloques sólidos
        if (!location.getBlock().getType().isSolid() &&
                !location.clone().add(0, 1, 0).getBlock().getType().isSolid()) {
            return location;
        }

        // Si no es seguro, buscar una ubicación cercana
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                Location testLoc = location.clone().add(x, 0, z);
                if (!testLoc.getBlock().getType().isSolid() &&
                        !testLoc.clone().add(0, 1, 0).getBlock().getType().isSolid()) {
                    return testLoc;
                }
            }
        }

        // Si no se encuentra ubicación segura, devolver la original
        return location.getWorld().getHighestBlockAt(location).getLocation().add(0, 1, 0);
    }
}