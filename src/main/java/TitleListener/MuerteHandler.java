package TitleListener;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;

public class MuerteHandler implements Listener {
    private final JavaPlugin plugin;


    public MuerteHandler(JavaPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        String playerName = player.getName();
        String deathCause = (player.getLastDamageCause() != null && player.getLastDamageCause().getCause() != null)
                ? player.getLastDamageCause().getCause().toString().replace("_", " ").toLowerCase()
                : "desconocida";

        // Cambiar el modo de juego a espectador
        player.setGameMode(GameMode.SPECTATOR);

        // Asignar al jugador al equipo "Fantasma"
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

        // Ejecutar comando /muertevct desde la consola
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "muertevct");

        // Mostrar en el action bar por más tiempo (por ejemplo, 5 segundos)
        String actionBarMessage = ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + playerName + ChatColor.RESET + ChatColor.GRAY + ChatColor.BOLD + " ha muerto por " + ChatColor.GOLD + ChatColor.BOLD + deathCause;
        new BukkitRunnable() {
            int duration = 5 * 20; // 5 segundos en ticks (20 ticks por segundo)
            int counter = 0;

            @Override
            public void run() {
                if (counter < duration) {
                    sendActionBarToAllPlayers(actionBarMessage);
                    counter += 20; // Incrementa cada segundo
                } else {
                    this.cancel(); // Detiene el Runnable después de 5 segundos
                }
            }
        }.runTaskTimer(plugin, 0, 20); // Se ejecuta cada segundo

        // Programar comandos después de 6 y 3 segundos
        new BukkitRunnable() {
            @Override
            public void run() {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(),"playsound minecraft:block.respawn_anchor.deplete ambient @a ~ ~ ~ 100000 0.5");
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(),"effect give @a minecraft:darkness 5 2");

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "playsound minecraft:block.end_portal.spawn ambient @a ~ ~ ~ 100000 0.1");

                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "playsound minecraft:item.trident.thunder ambient @a ~ ~ ~ 100000 0.1");

                                new BukkitRunnable() {
                                    @Override
                                    public void run() {
                                        String playerName = player.getName();
                                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "title @a times 50 40 50");
                                        String titleJson = String.format(
                                                "/title @a title [\"\",{\"text\":\"%s\",\"color\":\"dark_gray\",\"obfuscated\":true}]",
                                                playerName
                                        );
                                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), titleJson);
                                        String subtitleJson = "/title @a subtitle [\"\",{\"text\":\"Entro al sufrimiento eterno de Viciont\",\"color\":\"dark_purple\",\"bold\":true}]";
                                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), subtitleJson);
                                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "playsound minecraft:item.totem.use ambient @a ~ ~ ~ 100000 0.1");
                                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "playsound minecraft:entity.ender_dragon.death ambient @a ~ ~ ~ 100000 0.7");
                                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "effect give @a minecraft:blindness 15 3");
                                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "effect give @a minecraft:poison 6 0");
                                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "effect give @a minecraft:mining_fatigue 20 9");
                                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "effect give @a minecraft:levitation 7 0");
                                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "effect give @a minecraft:nausea 10 3");
                                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(),"title @a times 10 20 10");
                                    }
                                }.runTaskLater(plugin, 4 * 20); // Esperar 4 segundos
                            }
                        }.runTaskLater(plugin, 20); // Esperar 1 segundo
                    }
                }.runTaskLater(plugin, 3 * 20); // Esperar 3 segundos
            }
        }.runTaskLater(plugin, 6 * 20); // Esperar 6 segundos (después de la animación)
    }

    private void sendActionBarToAllPlayers(String message) {
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            onlinePlayer.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(message));
        }
    }
}
