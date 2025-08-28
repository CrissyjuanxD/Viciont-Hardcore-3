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

public class EconomyFlyTotem implements Listener {

    private final JavaPlugin plugin;
    private final NamespacedKey flyTotemKey;

    public EconomyFlyTotem(JavaPlugin plugin) {
        this.plugin = plugin;
        this.flyTotemKey = new NamespacedKey(plugin, "fly_totem");
    }

    public ItemStack createFlyTotem() {
        ItemStack totem = new ItemStack(Material.TOTEM_OF_UNDYING);
        ItemMeta meta = totem.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ChatColor.of("#ffcc00") + ChatColor.BOLD.toString() + "Fly Totem");

            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(ChatColor.of("#fff599") + "Este tótem te otorgará:");
            lore.add(ChatColor.of("#fff599") + "Resistencia I " + ChatColor.GRAY + "(10 segundos)");
            lore.add("");
            lore.add(ChatColor.of("#ffcc00") + "Al activarse, te concede habilidades de vuelo:");
            lore.add(ChatColor.of("#ffcc00") + "• " + ChatColor.of("#fff599") + "Levitación II " + ChatColor.GRAY + "(10 segundos)");
            lore.add(ChatColor.of("#ffcc00") + "• " + ChatColor.of("#fff599") + "Caída Suave II " + ChatColor.GRAY + "(20 segundos)");
            lore.add(ChatColor.of("#ffcc00") + "• " + ChatColor.of("#fff599") + "Empuje ascendente inicial");
            lore.add("");
            meta.setLore(lore);

            meta.setCustomModelData(7);
            PersistentDataContainer data = meta.getPersistentDataContainer();
            data.set(flyTotemKey, PersistentDataType.BYTE, (byte) 1);

            totem.setItemMeta(meta);
        }
        return totem;
    }

    public boolean isFlyTotem(ItemStack item) {
        if (item == null || item.getType() != Material.TOTEM_OF_UNDYING) return false;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;

        PersistentDataContainer data = meta.getPersistentDataContainer();
        return data.has(flyTotemKey, PersistentDataType.BYTE);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onTotemUse(EntityResurrectEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();

        ItemStack usedTotem = null;

        if (isFlyTotem(player.getInventory().getItemInMainHand())) {
            usedTotem = player.getInventory().getItemInMainHand();
        } else if (isFlyTotem(player.getInventory().getItemInOffHand())) {
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

                player.addPotionEffect(new PotionEffect(
                        PotionEffectType.LEVITATION,
                        20 * 10,
                        1,
                        true,
                        true
                ));

                player.addPotionEffect(new PotionEffect(
                        PotionEffectType.SLOW_FALLING,
                        20 * 20,
                        1,
                        true,
                        true
                ));

                player.setVelocity(new Vector(0, 1.2, 0));

                player.getWorld().playSound(player.getLocation(), Sound.ITEM_ELYTRA_FLYING, 1.8f, 1.2f);
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PHANTOM_FLAP, 1.5f, 0.8f);
                player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.5f);

                createActivationParticles(player);

                startFlightEffects(player);
            }
        }.runTaskLater(plugin, 1L);
    }

    private void createActivationParticles(Player player) {
        Location center = player.getLocation();
        World world = player.getWorld();

        for (int i = 0; i < 50; i++) {
            double angle = 2 * Math.PI * i / 50;
            double x = Math.cos(angle) * 2;
            double z = Math.sin(angle) * 2;
            Location particleLoc = center.clone().add(x, 0.5, z);

            Particle.DustOptions goldDust = new Particle.DustOptions(Color.fromRGB(255, 204, 0), 2.0f);
            Particle.DustOptions whiteDust = new Particle.DustOptions(Color.WHITE, 1.5f);

            world.spawnParticle(Particle.DUST, particleLoc, 3, 0.2, 0.2, 0.2, goldDust);
            world.spawnParticle(Particle.DUST, particleLoc.add(0, 0.5, 0), 2, 0.1, 0.1, 0.1, whiteDust);
        }

        for (int i = 0; i < 30; i++) {
            double offsetX = (Math.random() - 0.5) * 3;
            double offsetY = Math.random() * 2;
            double offsetZ = (Math.random() - 0.5) * 3;

            Location windLoc = center.clone().add(offsetX, offsetY, offsetZ);
            world.spawnParticle(Particle.CLOUD, windLoc, 1, 0.1, 0.1, 0.1, 0.02);
        }
    }

    private void startFlightEffects(Player player) {
        World world = player.getWorld();

        new BukkitRunnable() {
            int duration = 0;

            @Override
            public void run() {
                if (duration >= 200) {
                    this.cancel();
                    return;
                }

                Location playerLoc = player.getLocation();

                for (int i = 0; i < 5; i++) {
                    double angle = 2 * Math.PI * i / 5;
                    double x = Math.cos(angle) * 0.5;
                    double z = Math.sin(angle) * 0.5;

                    Location footParticle = playerLoc.clone().add(x, 0.1, z);

                    Particle.DustOptions lightGoldDust = new Particle.DustOptions(Color.fromRGB(255, 230, 150), 1.0f);
                    world.spawnParticle(Particle.DUST, footParticle, 2, 0.1, 0, 0.1, lightGoldDust);
                    world.spawnParticle(Particle.CLOUD, footParticle, 1, 0.05, 0, 0.05, 0.01);
                }

                if (player.getVelocity().length() > 0.1) {
                    Vector direction = player.getVelocity().normalize().multiply(-0.5);
                    Location trailLoc = playerLoc.clone().add(direction);

                    world.spawnParticle(Particle.CLOUD, trailLoc, 3, 0.2, 0.2, 0.2, 0.02);
                    world.spawnParticle(Particle.END_ROD, trailLoc, 2, 0.1, 0.1, 0.1, 0.01);
                }

                if (duration % 40 == 0) {
                    world.playSound(playerLoc, Sound.BLOCK_WOOL_BREAK, 0.5f, 1.8f);
                }

                duration += 5;
            }
        }.runTaskTimer(plugin, 0L, 5L);
    }
}
