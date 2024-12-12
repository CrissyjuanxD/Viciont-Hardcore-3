package TitleListener;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;
import vct.hardcore3.DeathStormHandler;

import java.util.HashMap;
import java.util.Map;

public class MuerteHandler implements Listener {
    private final JavaPlugin plugin;
    private final MuerteAnimation muerteAnimation;
    private BukkitRunnable currentDeathMessageTask;
    private final Map<Player, Location> deathLocations = new HashMap<>(); // Mapa para almacenar las ubicaciones de muerte

    public MuerteHandler(JavaPlugin plugin) {
        this.plugin = plugin;
        this.muerteAnimation = new MuerteAnimation(plugin);
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        Location deathLocation = player.getLocation(); // Guardar la ubicación de la muerte
        deathLocations.put(player, deathLocation); // Almacenar la ubicación de muerte en el mapa

        String playerName = player.getName();
        String deathCause = (player.getLastDamageCause() != null && player.getLastDamageCause().getCause() != null)
                ? player.getLastDamageCause().getCause().toString().replace("_", " ").toLowerCase()
                : "desconocida";

        // Cambiar el modo de juego del jugador a Espectador
        player.setGameMode(GameMode.SPECTATOR);

        // Cancelar la teletransportación del evento de respawn
        new BukkitRunnable() {
            @Override
            public void run() {
                player.teleport(deathLocation); // Asegurarse de que el jugador permanece en la ubicación de la muerte
            }
        }.runTaskLater(plugin, 1); // Ejecutar un tick después

        // Gestionar el equipo "Fantasma" en el Scoreboard
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager != null) {
            Scoreboard scoreboard = manager.getMainScoreboard();
            Team team = scoreboard.getTeam("Fantasma");
            if (team == null) {
                team = scoreboard.registerNewTeam("Fantasma");
                team.setDisplayName(ChatColor.GRAY + "Fantasma");
            }
            team.addEntry(playerName);
        }

        muerteAnimation.playAnimation(player, ""); // Aquí estamos llamando directamente al método playAnimation

        // Preparar el mensaje del Action Bar
        String actionBarMessage = ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + playerName + ChatColor.RESET
                + ChatColor.GRAY + ChatColor.BOLD + " ha muerto por " + ChatColor.GOLD + ChatColor.BOLD + deathCause;

        // Cancelar el mensaje de muerte anterior, si existe
        if (currentDeathMessageTask != null && !currentDeathMessageTask.isCancelled()) {
            currentDeathMessageTask.cancel();
        }

        // Crear una tarea repetitiva para mostrar el Action Bar
        currentDeathMessageTask = new BukkitRunnable() {
            int duration = 7 * 20; // Duración de 7 segundos en ticks (20 ticks/segundo)
            int counter = 0;

            @Override
            public void run() {
                if (counter < duration) {
                    sendActionBarToAllPlayers(actionBarMessage);
                    counter += 20; // Incrementa cada segundo
                } else {
                    this.cancel();
                    resetDeathMessageFlag();
                }
            }
        };
        currentDeathMessageTask.runTaskTimer(plugin, 0, 20);

        // Programar efectos y sonidos en el tiempo adecuado
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

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        Location deathLocation = deathLocations.remove(player); // Obtener y eliminar la ubicación de muerte del mapa

        if (deathLocation != null) {
            event.setRespawnLocation(deathLocation); // Establecer la ubicación de respawn en la ubicación de la muerte
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
                    10, // Tiempo de aparición
                    70, // Tiempo en pantalla
                    20  // Tiempo de desaparición
            );
        }
    }

    private void resetDeathMessageFlag() {
        Bukkit.getScheduler().runTask(plugin, () -> {
            DeathStormHandler.setDeathMessageActive(false);
        });
    }

    private void sendActionBarToAllPlayers(String message) {
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            onlinePlayer.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(message));
        }
    }
}
