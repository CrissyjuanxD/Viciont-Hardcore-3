package vct.hardcore3;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.event.player.PlayerBedEnterEvent;

public class DeathStormHandler implements Listener {
    private final JavaPlugin plugin;
    private final DayHandler dayHandler;  // Referencia al DayHandler
    private final Map<UUID, Integer> deathCount = new HashMap<>();
    private int remainingStormSeconds = 0;
    private boolean isDeathStormActive = false;
    private boolean isDeathMessageActive = false;


    public DeathStormHandler(JavaPlugin plugin, DayHandler dayHandler) {
        this.plugin = plugin;
        this.dayHandler = dayHandler;
        loadStormData();
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        UUID playerUUID = player.getUniqueId();
        deathCount.put(playerUUID, deathCount.getOrDefault(playerUUID, 0) + 1);

        int currentDay = dayHandler.getCurrentDay(); // Obtener el día desde DayHandler
        int increment = 3600 * currentDay; // Cada muerte agrega horas basadas en el día actual
        remainingStormSeconds += increment;
        isDeathStormActive = true;
        isDeathMessageActive = true; // Activar la bandera

        startStorm();
        saveStormData();

        // Pausar el temporizador de DeathStorm mientras se muestra el mensaje de muerte
        new BukkitRunnable() {
            @Override
            public void run() {
                isDeathMessageActive = false; // Desactivar la bandera después de 5 segundos (o el tiempo necesario)
            }
        }.runTaskLater(plugin, 5 * 20); // Ajusta el tiempo según la duración de tu mensaje de muerte
    }


    @EventHandler
    public void onWeatherChange(WeatherChangeEvent event) {
        if (!event.toWeatherState()) {
            if (isDeathStormActive) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPlayerBedEnter(PlayerBedEnterEvent event) {
        if (isDeathStormActive) {
            event.getPlayer().sendMessage(ChatColor.GRAY + "No puedes dormir durante una DeathStorm.");
            event.setCancelled(true);
        }
    }

    private BukkitRunnable stormTask;

    private void startStorm() {
        World world = Bukkit.getWorlds().get(0);

        if (stormTask != null && !stormTask.isCancelled()) {
            stormTask.cancel();
        }

        world.setStorm(true);
        world.setThundering(true);

        stormTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (remainingStormSeconds <= 0) {
                    cancel();
                    world.setStorm(false);
                    world.setThundering(false);
                    isDeathStormActive = false;
                    return;
                }

                // Verificar si el mensaje de muerte está activo
                if (isDeathMessageActive) {
                    return; // No mostrar el action bar de la tormenta mientras se muestra el mensaje de muerte
                }

                int hours = remainingStormSeconds / 3600;
                int minutes = (remainingStormSeconds % 3600) / 60;
                int seconds = remainingStormSeconds % 60;
                String timeMessage = String.format(ChatColor.DARK_PURPLE + "Quedan" + ChatColor.BOLD + " %02d:%02d:%02d" + ChatColor.RESET + ChatColor.DARK_PURPLE + " horas de DeathStorm", hours, minutes, seconds);

                for (Player player : Bukkit.getOnlinePlayers()) {
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(timeMessage));
                }

                remainingStormSeconds--;
            }
        };


        stormTask.runTaskTimer(plugin, 0, 20);
    }

    public void resetStorm() {
        remainingStormSeconds = 0;
        isDeathStormActive = false;
        saveStormData();
    }

    public void addStormHours(int hours) {
        remainingStormSeconds += hours * 3600;
        isDeathStormActive = true;  // Activamos la DeathStorm al añadir horas
        saveStormData();
    }

    public void removeStormHours(int hours) {
        remainingStormSeconds = Math.max(remainingStormSeconds - (hours * 3600), 0);
        if (remainingStormSeconds == 0) {
            isDeathStormActive = false;  // Desactivamos la DeathStorm si se quitan todas las horas
        }
        saveStormData();
    }

    public void loadStormData() {
        try {
            Path path = Paths.get(plugin.getDataFolder().getAbsolutePath(), "stormdata.txt");
            if (Files.exists(path)) {
                remainingStormSeconds = Integer.parseInt(new String(Files.readAllBytes(path)).trim());
                Bukkit.getLogger().info("Storm data loaded: " + remainingStormSeconds + " seconds remaining.");
                if (remainingStormSeconds > 0) {
                    isDeathStormActive = true;
                    startStorm();
                }
            }
        } catch (IOException e) {
            Bukkit.getLogger().severe("Error loading storm data: " + e.getMessage());
        } catch (NumberFormatException e) {
            Bukkit.getLogger().severe("Invalid number format in stormdata.txt: " + e.getMessage());
        }
    }

    public void saveStormData() {
        try {
            Path path = Paths.get(plugin.getDataFolder().getAbsolutePath(), "stormdata.txt");
            Files.createDirectories(path.getParent());
            Files.write(path, String.valueOf(remainingStormSeconds).getBytes());
            Bukkit.getLogger().info("Storm data saved: " + remainingStormSeconds + " seconds remaining.");
        } catch (IOException e) {
            Bukkit.getLogger().severe("Error saving storm data: " + e.getMessage());
        }
    }

}
