package TitleListener;

import Events.DamageLogListener;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import Handlers.DeathStormHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MuerteHandler implements Listener {
    private final JavaPlugin plugin;
    private final MuerteAnimation muerteAnimation;
    private final DamageLogListener damageLogListener;
    private final DeathStormHandler deathStormHandler;
    private BukkitRunnable currentDeathMessageTask;
    private static volatile boolean isDeathMessageActive = false;
    private final Map<Player, Location> deathLocations = new HashMap<>();

    public MuerteHandler(JavaPlugin plugin, DamageLogListener damageLogListener, DeathStormHandler deathStormHandler) {
        this.plugin = plugin;
        this.damageLogListener = damageLogListener;
        this.deathStormHandler = deathStormHandler;
        this.muerteAnimation = new MuerteAnimation(plugin);
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        UUID playerId = player.getUniqueId();

        Bukkit.getOnlinePlayers().forEach(onlinePlayer -> {
            UUID onlinePlayerId = onlinePlayer.getUniqueId();
            if (damageLogListener != null) {
                damageLogListener.pauseActionBarForPlayer(onlinePlayerId);
            }
            if (deathStormHandler != null) {
                deathStormHandler.pauseActionBarForPlayer(onlinePlayerId);
            }
        });

        Location deathLocation = player.getLocation();
        deathLocations.put(player, deathLocation);

        String playerName = player.getName();
        String deathCause = (player.getLastDamageCause() != null && player.getLastDamageCause().getCause() != null)
                ? player.getLastDamageCause().getCause().toString().replace("_", " ").toLowerCase()
                : "desconocida";

        muerteAnimation.playAnimation(player, "");

        String actionBarMessage = ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + playerName + ChatColor.RESET
                + ChatColor.GRAY + ChatColor.BOLD + " ha muerto por " + ChatColor.GOLD + ChatColor.BOLD + deathCause;

        if (currentDeathMessageTask != null && !currentDeathMessageTask.isCancelled()) {
            currentDeathMessageTask.cancel();
        }

        currentDeathMessageTask = new BukkitRunnable() {
            int duration = 7 * 20;
            int counter = 0;

            @Override
            public void run() {
                if (counter < duration) {
                    sendActionBarToAllPlayers(actionBarMessage);
                    counter += 20;
                } else {
                    isDeathMessageActive = false;
                    // Reactivar primero solo DamageLog si está activo (excepto para el jugador muerto)
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        Bukkit.getOnlinePlayers().forEach(onlinePlayer -> {
                            UUID onlinePlayerId = onlinePlayer.getUniqueId();
                            // Si es el jugador que murió, saltar la reactivación de DamageLog
                            if (onlinePlayerId.equals(player.getUniqueId())) {
                                return;
                            }

                            if (damageLogListener != null && damageLogListener.isPlayerInDamageLog(onlinePlayerId)) {
                                damageLogListener.resumeActionBarForPlayer(onlinePlayerId);
                                // Pausar DeathStorm mientras DamageLog está activo
                                if (deathStormHandler != null) {
                                    deathStormHandler.pauseActionBarForPlayer(onlinePlayerId);
                                }
                            }
                        });
                    });

                    // Reactivación especial para el jugador que murió (sin delay)
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        if (deathStormHandler != null) {
                            // Reactivar DeathStorm inmediatamente para el jugador muerto
                            deathStormHandler.resumeActionBarForPlayer(player.getUniqueId());
                        }
                        if (damageLogListener != null) {
                            damageLogListener.resumeActionBarForPlayer(player.getUniqueId());
                        }
                    });

                    // Luego reactivar DeathStorm después de 1 segundo (20 ticks) para los demás
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        Bukkit.getOnlinePlayers().forEach(onlinePlayer -> {
                            UUID onlinePlayerId = onlinePlayer.getUniqueId();
                            // Saltar el jugador que murió (ya se reactivó)
                            if (onlinePlayerId.equals(player.getUniqueId())) {
                                return;
                            }

                            if (deathStormHandler != null) {
                                // Solo reactivar si el jugador no está en DamageLog
                                if (damageLogListener == null || !damageLogListener.isPlayerInDamageLog(onlinePlayerId)) {
                                    deathStormHandler.resumeActionBarForPlayer(onlinePlayerId);
                                }
                            }
                        });
                    }, 20);
                    this.cancel();
                }
            }
        };
        currentDeathMessageTask.runTaskTimer(plugin, 0, 20);

        new BukkitRunnable() {
            @Override
            public void run() {
                executeBukkitCommands(player);

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        Bukkit.getOnlinePlayers().forEach(p -> p.playSound(p.getLocation(), "minecraft:block.end_portal.spawn", 100000.0f, 0.1f));

                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                Bukkit.getOnlinePlayers().forEach(p -> p.playSound(p.getLocation(), "minecraft:item.trident.thunder", 100000.0f, 0.1f));

                                new BukkitRunnable() {
                                    @Override
                                    public void run() {
                                        sendTitleToAllPlayers(playerName);
                                        executeFinalBukkitCommands();
                                    }
                                }.runTaskLater(plugin, 4 * 20);
                            }
                        }.runTaskLater(plugin, 20);
                    }
                }.runTaskLater(plugin, 3 * 20);
            }
        }.runTaskLater(plugin, 9 * 20);
    }

    public static boolean isDeathMessageActive() {
        return isDeathMessageActive;
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();

            if (player.getGameMode() == GameMode.SPECTATOR && event.getCause() == EntityDamageEvent.DamageCause.VOID) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        Location deathLocation = deathLocations.remove(player);

        if (deathLocation != null) {
            event.setRespawnLocation(deathLocation);
        }
    }

    private void executeBukkitCommands(Player player) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(p.getLocation(), "minecraft:block.respawn_anchor.deplete", 100000.0f, 2f);
            p.playSound(p.getLocation(), "minecraft:block.beacon.deactivate", 100000.0f, 0.1f);
            p.playSound(p.getLocation(), "minecraft:block.beacon.power_select", 100000.0f, 0.1f);
            p.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 10, 2));
        }
    }

    private void executeFinalBukkitCommands() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 14 * 20, 0));
            p.playSound(p.getLocation(), "minecraft:item.totem.use", 100000.0f, 0.1f);
            p.playSound(p.getLocation(), "minecraft:entity.ender_dragon.death", 100000.0f, 0.7f);
            p.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 6 * 20, 0));
            p.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 20 * 20, 9));
            p.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 7 * 20, 0));
            p.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 10 * 20, 3));
        }
    }

    private void sendTitleToAllPlayers(String playerName) {
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            onlinePlayer.sendTitle(
                    ChatColor.DARK_GRAY + "" + ChatColor.MAGIC + "O" + ChatColor.RESET + ChatColor.GRAY + playerName + ChatColor.RESET + ChatColor.GRAY + ChatColor.MAGIC + "O", // Título (nombre del jugador con formato obfuscado)
                    ChatColor.DARK_PURPLE + "" + ChatColor.RESET + ChatColor.DARK_PURPLE + ChatColor.BOLD + "۞Entro al sufrimiento eterno de Viciont۞", // Subtítulo
                    10,
                    70,
                    20
            );
        }
    }

    private void sendActionBarToAllPlayers(String message) {
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            onlinePlayer.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(message));
        }
    }
}
