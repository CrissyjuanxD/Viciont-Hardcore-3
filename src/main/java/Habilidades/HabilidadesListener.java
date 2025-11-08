package Habilidades;

import com.destroystokyo.paper.event.player.PlayerJumpEvent;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.*;

public class HabilidadesListener implements Listener {

    private final JavaPlugin plugin;
    private final HabilidadesManager manager;
    private final HabilidadesEffects effects;
    private final Map<UUID, Integer> jumpCount = new HashMap<>();
    private final Map<UUID, Long> lastJumpTime = new HashMap<>();
    private final Map<UUID, Long> lastAbsorptionTime = new HashMap<>();
    private final Map<UUID, Boolean> hasTouchedGround = new HashMap<>();
    private final Map<UUID, Boolean> wasOnGround = new HashMap<>();
    private final Set<UUID> cooldownPlayers = new HashSet<>();
    private final Set<UUID> isJumping = new HashSet<>();
    private final Set<UUID> cooldownJump = new HashSet<>();
    private final Set<UUID> pressingSpace = new HashSet<>();
    private final Random random = new Random();

    public HabilidadesListener(JavaPlugin plugin, HabilidadesManager manager, HabilidadesEffects effects) {
        this.plugin = plugin;
        this.manager = manager;
        this.effects = effects;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if ((event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK)) {
            if (item != null && item.getType() == Material.BOOK && item.hasItemMeta()) {
                if (item.getItemMeta().hasCustomModelData() &&
                        item.getItemMeta().getCustomModelData() == 9999 &&
                        item.getItemMeta().getDisplayName().equals(ChatColor.of("#C77DFF") + "" + ChatColor.BOLD + "Libro de Habilidades")) {

                    event.setCancelled(true);
                    HabilidadesGUI gui = new HabilidadesGUI(plugin, manager, ((vct.hardcore3.ViciontHardcore3) plugin).getDayHandler());
                    gui.openHabilidadesGUI(player);
                }
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        effects.reapplyAllEffects(player, manager);
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            effects.reapplyAllEffects(player, manager);
        }, 10L);
    }


    @EventHandler
    public void onPlayerToggleFlight(PlayerToggleFlightEvent event) {
        Player player = event.getPlayer();

        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
            return;
        }

        if (!player.isOnGround() && hasDoubleJumpAbility(player)) {
            event.setCancelled(true);

            int maxJumps = getMaxJumps(player);
            int currentJumps = jumpCount.getOrDefault(player.getUniqueId(), 0);

            if (currentJumps < maxJumps) {
                performDoubleJump(player);
                jumpCount.put(player.getUniqueId(), currentJumps + 1);
            }
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        if (player.isOnGround()) {
            jumpCount.remove(player.getUniqueId());
            isJumping.remove(player.getUniqueId());
            player.setAllowFlight(false);
        } else if (hasDoubleJumpAbility(player) && !isJumping.contains(player.getUniqueId())) {
            isJumping.add(player.getUniqueId());
            player.setAllowFlight(true);
        }
    }

    private boolean hasDoubleJumpAbility(Player player) {
        return manager.hasHabilidad(player.getUniqueId(), HabilidadesType.AGILIDAD, 3) ||
                manager.hasHabilidad(player.getUniqueId(), HabilidadesType.AGILIDAD, 4);
    }

    private int getMaxJumps(Player player) {
        if (manager.hasHabilidad(player.getUniqueId(), HabilidadesType.AGILIDAD, 4)) {
            return 2;
        } else if (manager.hasHabilidad(player.getUniqueId(), HabilidadesType.AGILIDAD, 3)) {
            return 1;
        }
        return 0;
    }

    private void performDoubleJump(Player player) {
        Vector velocity = player.getVelocity();
        velocity.setY(0.8);
        player.setVelocity(velocity);

        Location loc = player.getLocation();
        World world = loc.getWorld();

        for (int i = 0; i < 15; i++) {
            double offsetX = (Math.random() - 0.5) * 0.5;
            double offsetZ = (Math.random() - 0.5) * 0.5;
            world.spawnParticle(Particle.POOF, loc.clone().add(offsetX, 0.1, offsetZ), 1, 0, 0, 0, 0);
        }

        world.spawnParticle(Particle.SONIC_BOOM, loc.clone().add(0, 0.5, 0), 1);

        player.playSound(loc, Sound.ENTITY_ENDER_DRAGON_FLAP, 0.5f, 1.5f);
        player.playSound(loc, Sound.ENTITY_WARDEN_SONIC_BOOM, 0.2f, 2.0f);
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;

        Player player = (Player) event.getEntity();

        if (event.getCause() == EntityDamageEvent.DamageCause.PROJECTILE &&
                manager.hasHabilidad(player.getUniqueId(), HabilidadesType.RESISTENCIA, 2)) {

            if (random.nextDouble() < 0.15) {
                event.setCancelled(true);
                playBlockEffect(player);
                player.sendMessage(ChatColor.of("#C77DFF") + "¡Proyectil bloqueado!");
            }
        }

        if ((player.getHealth() - event.getFinalDamage()) <= 6) {
            long currentTime = System.currentTimeMillis();
            long lastAbsorption = lastAbsorptionTime.getOrDefault(player.getUniqueId(), 0L);

            // Cooldown de 5 segundos
            if (currentTime - lastAbsorption >= 5000) {
                if (manager.hasHabilidad(player.getUniqueId(), HabilidadesType.VITALIDAD, 3)) {
                    if (random.nextDouble() < 0.15) {
                        applyAbsorption(player, 2);
                        lastAbsorptionTime.put(player.getUniqueId(), currentTime);
                    }
                } else if (manager.hasHabilidad(player.getUniqueId(), HabilidadesType.VITALIDAD, 4)) {
                    if (random.nextDouble() < 0.15) {
                        applyAbsorption(player, 3);
                        lastAbsorptionTime.put(player.getUniqueId(), currentTime);
                    }
                }
            }
        }
    }

    private void applyAbsorption(Player player, int amplifier) {
        // Remover absorción existente para evitar stacking
        player.removePotionEffect(PotionEffectType.ABSORPTION);

        // Aplicar nueva absorción
        player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 200, amplifier, false, false));
        player.sendMessage(ChatColor.of("#C77DFF") + "¡Absorción activada!");

        // Efectos visuales y de sonido
        Location loc = player.getLocation();
        player.getWorld().spawnParticle(Particle.HEART, loc.add(0, 1, 0), 10, 0.5, 0.5, 0.5, 0);
        player.playSound(loc, Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.9f);
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) return;

        Player player = (Player) event.getEntity();
        Entity damager = event.getDamager();

        if (damager instanceof Monster || damager instanceof Slime || damager instanceof Phantom) {
            if (manager.hasHabilidad(player.getUniqueId(), HabilidadesType.RESISTENCIA, 3)) {
                if (random.nextDouble() < 0.15) {
                    event.setCancelled(true);
                    playBlockEffect(player);
                    player.sendMessage(ChatColor.of("#C77DFF") + "¡Golpe bloqueado!");
                }
            }
        }
    }

    private void playBlockEffect(Player player) {
        Location loc = player.getLocation().add(0, 1, 0);
        World world = loc.getWorld();

        world.spawnParticle(Particle.ELECTRIC_SPARK, loc, 20, 0.5, 0.5, 0.5, 0.1);
        world.spawnParticle(Particle.ENCHANT, loc, 10, 0.3, 0.3, 0.3, 0);

        player.playSound(loc, Sound.ITEM_SHIELD_BLOCK, 1.0f, 1.5f);
        player.playSound(loc, Sound.BLOCK_ANVIL_LAND, 0.3f, 2.0f);
    }
}
