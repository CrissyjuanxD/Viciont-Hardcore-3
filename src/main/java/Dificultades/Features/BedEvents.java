package Dificultades.Features;

import Handlers.DayHandler;
import Handlers.DeathStormHandler;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.*;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Phantom;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerBedLeaveEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class BedEvents implements Listener {
    private final DayHandler dayHandler;
    private final DeathStormHandler deathStormHandler;
    private final JavaPlugin plugin;
    private final List<Player> playersInBed = new ArrayList<>();
    private final Map<Player, Long> lastPhantomReset = new HashMap<>();
    private final Random random = new Random();
    private final long PHANTOM_RESET_COOLDOWN = 60 * 60 * 1000;

    public BedEvents(JavaPlugin plugin, DayHandler dayHandler, DeathStormHandler deathStormHandler) {
        this.plugin = plugin;
        this.dayHandler = dayHandler;
        this.deathStormHandler = deathStormHandler;
    }

    @EventHandler
    public void onPlayerBedEnter(PlayerBedEnterEvent event) {
        Player player = event.getPlayer();
        World world = player.getWorld();
        int currentDay = dayHandler.getCurrentDay();

        int sleepPercentage;
        if (currentDay >= 12) {
            sleepPercentage = 200;
        } else if (currentDay >= 4) {
            sleepPercentage = 200;
        } else {
            sleepPercentage = 0;
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            world.setGameRule(GameRule.PLAYERS_SLEEPING_PERCENTAGE, sleepPercentage);
        }, 1L);

        if (currentDay < 12 && deathStormHandler.isDeathStormActive()) {
            player.sendMessage(ChatColor.GRAY + "No puedes dormir durante una DeathStorm");
            event.setCancelled(true);
            return;
        }

        if (currentDay >= 12) {
            event.setCancelled(true);
            handlePhantomReset(player);
            return;
        }

        if (world.getTime() >= 12530 && world.getTime() <= 23458 && !world.isThundering()) {
            if (currentDay >= 4 && Bukkit.getOnlinePlayers().size() < 4) {
                sendActionBar(player, ChatColor.RED + "¡No hay suficientes jugadores online para dormir! (Se necesitan 4)");
                event.setCancelled(true);
                return;
            }

            if (!playersInBed.contains(player)) {
                playersInBed.add(player);
                player.setSleepingIgnored(true);
            }

            String message = currentDay < 4
                    ? ChatColor.YELLOW + "" + ChatColor.BOLD + "۞ " + ChatColor.of("#F8F8A4") + ChatColor.BOLD + "Durmiendo" + ChatColor.GRAY + ChatColor.BOLD +": " + ChatColor.GOLD + "1" + ChatColor.GRAY + ChatColor.BOLD + "/" + ChatColor.GOLD + "1 " + ChatColor.GRAY + ChatColor.ITALIC + "(" + ChatColor.of("#F8F8A4") + "Noche saltada" + ChatColor.GRAY + ")"
                    : ChatColor.GOLD + String.format("Durmiendo: %d/4 (%.0f%%)",
                    playersInBed.size(), (playersInBed.size()/4.0)*100);

            sendGlobalActionBar(message);

            if ((currentDay < 4 && playersInBed.size() >= 1) ||
                    (currentDay >= 4 && playersInBed.size() >= 4)) {

                skipNight(world);
            }
        }
    }

    private void skipNight(World world) {
        new BukkitRunnable() {
            @Override
            public void run() {
                long current = world.getTime();
                if (current < 23800) {
                    world.setTime(current + 100);
                } else {
                    for (Player p : new ArrayList<>(playersInBed)) {
                        try {
                            if (p.isSleeping()) {
                                p.wakeup(false);
                            }
                        } catch (IllegalStateException e) {
                            plugin.getLogger().warning("No se pudo despertar a " + p.getName() + ": " + e.getMessage());
                        }
                        p.setSleepingIgnored(false);
                    }
                    playersInBed.clear();

                    if (!deathStormHandler.isDeathStormActive()) {
                        world.setStorm(false);
                        world.setThundering(false);
                    }

                    sendGlobalActionBar(ChatColor.WHITE + "۞ Buenos días!");
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    @EventHandler
    public void onPlayerBedLeave(PlayerBedLeaveEvent event) {
        Player player = event.getPlayer();
        int currentDay = dayHandler.getCurrentDay();

        if (currentDay >= 12) return;

        if (playersInBed.remove(player)) {
            player.setSleepingIgnored(false);
            String message = currentDay < 4
                    ? ChatColor.YELLOW + "Durmiendo: 0/1"
                    : ChatColor.YELLOW + String.format("Durmiendo: %d/4", playersInBed.size());

            sendGlobalActionBar(message);
        }
    }

    private void handlePhantomReset(Player player) {
        if (lastPhantomReset.containsKey(player)) {
            long timeSinceLastReset = System.currentTimeMillis() - lastPhantomReset.get(player);
            if (timeSinceLastReset < PHANTOM_RESET_COOLDOWN) {
                long remaining = (PHANTOM_RESET_COOLDOWN - timeSinceLastReset) / 60000;
                player.sendMessage(ChatColor.RED + "Espera " + remaining + " minutos para resetear phantoms");
                return;
            }
        }

        if (random.nextDouble() < 0.5) {
            lastPhantomReset.put(player, System.currentTimeMillis());
            player.setStatistic(Statistic.TIME_SINCE_REST, 0);
            player.sendMessage(ChatColor.GREEN + "Contador de phantoms reseteado!");

            if (random.nextDouble() < 0.25) {
                spawnPhantomSwarm(player);
                player.sendMessage(ChatColor.RED + "Phantoms aparecieron!");
            }
        } else {
            player.sendMessage(ChatColor.YELLOW + "Nada ocurrió al intentar dormir...");
        }
    }

    private void spawnPhantomSwarm(Player player) {
        Location loc = player.getLocation().add(0, 10, 0);
        int count = 5 + random.nextInt(6);

        for (int i = 0; i < count; i++) {
            Vector offset = new Vector(random.nextDouble() * 10 - 5, 0, random.nextDouble() * 10 - 5);
            Phantom phantom = (Phantom) player.getWorld().spawnEntity(loc.add(offset), EntityType.PHANTOM);
            phantom.setTarget(player);
        }
    }

    private void sendGlobalActionBar(String message) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(message));
        }
    }

    private void sendActionBar(Player player, String message) {
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(message));
    }
}
