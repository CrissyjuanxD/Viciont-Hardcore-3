package TitleListener;

import Events.DamageLogListener;
import Handlers.DayHandler;
import net.kyori.adventure.text.TranslatableComponent;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
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
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import Handlers.DeathStormHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MuerteHandler implements Listener {
    private final JavaPlugin plugin;
    private final MuerteAnimation muerteAnimation;
    private final DamageLogListener damageLogListener;
    private final DeathStormHandler deathStormHandler;
    private final DayHandler dayHandler;
    private BukkitRunnable currentDeathMessageTask;
    private static volatile boolean isDeathMessageActive = false;
    private final Map<Player, Location> deathLocations = new HashMap<>();

    public MuerteHandler(JavaPlugin plugin, DamageLogListener damageLogListener, DeathStormHandler deathStormHandler, DayHandler dayHandler) {
        this.plugin = plugin;
        this.damageLogListener = damageLogListener;
        this.deathStormHandler = deathStormHandler;
        this.dayHandler = dayHandler;
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
        });

        Location deathLocation = player.getLocation();
        deathLocations.put(player, deathLocation);
        String playerNames = player.getName();

        muerteAnimation.playAnimation(player, "");

        Component original = event.deathMessage();
        if (original == null) return;

        Component formatted;

        // ✅ Si el mensaje es traducible, reconstruimos con color
        if (original instanceof TranslatableComponent translatable) {
            String key = translatable.key();
            java.util.List<Component> args = translatable.args();
            java.util.List<Component> coloredArgs = new java.util.ArrayList<>();

            for (int i = 0; i < args.size(); i++) {
                Component arg = args.get(i);
                Component colored;

                if (i == 0) {
                    // Víctima (jugador) → morado y negrita
                    colored = arg.color(NamedTextColor.DARK_PURPLE);
                } else {
                    // Todo lo demás (asesino, arma, proyectil, etc.) → dorado forzado
                    colored = forceColor(arg, NamedTextColor.GOLD);
                }

                coloredArgs.add(colored);
            }

            // Reconstrucción completa del mensaje traducido
            formatted = Component.translatable(
                    key,
                    coloredArgs
            ).color(NamedTextColor.GRAY);

        } else {
            // Fallback por si no es traducible (raro)
            formatted = Component.text(player.getName(), NamedTextColor.DARK_PURPLE)
                    .append(Component.text(" ha muerto", NamedTextColor.GRAY));
        }

        Component actionBarMessage = formatted;

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
                            }
                        });
                    });

                    // Reactivación especial para el jugador que murió (sin delay)
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        if (damageLogListener != null) {
                            damageLogListener.resumeActionBarForPlayer(player.getUniqueId());
                        }
                    });
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
                        Bukkit.getOnlinePlayers().forEach(p -> p.playSound(p.getLocation(), Sound.BLOCK_END_PORTAL_SPAWN, 100000.0f, 0.1f));

                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                deathStormHandler.addDeathStormTime(player);

                                new BukkitRunnable() {
                                    @Override
                                    public void run() {

                                        int day = dayHandler.getCurrentDay();
                                        double chance;
                                        int amplifierBonus;

                                        if (day <= 3) {
                                            chance = 0.15;
                                            amplifierBonus = 0;
                                        } else if (day <= 7) {
                                            chance = 0.30;
                                            amplifierBonus = 2;
                                        } else if (day <= 11) {
                                            chance = 0.45;
                                            amplifierBonus = 3;
                                        } else if (day <= 20) {
                                            chance = 0.60;
                                            amplifierBonus = 4;
                                        } else {
                                            chance = 0.80;
                                            amplifierBonus = 5;
                                        }

                                        if (Math.random() <= chance) {
                                            applyCurseToAllPlayers(amplifierBonus);
                                            sendTitleToAllPlayers(playerNames);
                                        }
                                    }
                                }.runTaskLater(plugin, 4 * 20);
                            }
                        }.runTaskLater(plugin, 10);
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
            p.playSound(p.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 100000.0f, 2f);
            p.playSound(p.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 100000.0f, 0.1f);
            p.playSound(p.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 100000.0f, 0.1f);
            p.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 10, 2));
        }
    }

    private void applyCurseToAllPlayers(int amplifierBonus) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(p.getLocation(), Sound.ITEM_TOTEM_USE, 100000.0f, 0.1f);
            p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_DEATH, 100000.0f, 0.7f);

            p.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 14 * 20, amplifierBonus));
            p.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 6 * 20, 2 + amplifierBonus));
            p.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 20 * 20, 9 + amplifierBonus));
            p.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 7 * 20, 2 + amplifierBonus));
            p.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 10 * 20, 3 + amplifierBonus));
        }
    }

    private void sendTitleToAllPlayers(String playerName) {
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            onlinePlayer.sendTitle(
                    ChatColor.DARK_GRAY + "" + ChatColor.MAGIC + "Ｏ" + ChatColor.RESET + ChatColor.DARK_PURPLE + " ᴍᴀʟᴅɪᴄɪᴏɴ ʟɪʙᴇʀᴀᴅᴀ " + ChatColor.RESET + ChatColor.GRAY + ChatColor.MAGIC + "Ｏ", // 🟣 título principal
                    ChatColor.GRAY + "" + ChatColor.BOLD + "۞ "
                            + ChatColor.GOLD + playerName
                            + ChatColor.GRAY + " ʜᴀ ᴇɴᴏᴊᴀᴅᴏ ᴀʟ "
                            + ChatColor.DARK_GRAY + "" + ChatColor.BOLD + "ᴅᴀʀᴋ ᴅᴇᴍᴏɴ"
                            + ChatColor.GRAY + " ۞", // subtítulo formateado
                    10, // fadeIn ticks
                    70, // stay ticks
                    20  // fadeOut ticks
            );
        }
    }

    private void sendActionBarToAllPlayers(Component message) {
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            onlinePlayer.sendActionBar(message);
        }
    }

    private Component forceColor(Component component, NamedTextColor color, TextDecoration... decorations) {
        Component base = component.color(color);
        for (TextDecoration deco : decorations) {
            base = base.decorate(deco);
        }

        // Aplicar el color a todos los hijos
        if (!component.children().isEmpty()) {
            java.util.List<Component> recoloredChildren = new java.util.ArrayList<>();
            for (Component child : component.children()) {
                recoloredChildren.add(forceColor(child, color, decorations));
            }
            base = base.children(recoloredChildren);
        }
        return base;
    }

}
