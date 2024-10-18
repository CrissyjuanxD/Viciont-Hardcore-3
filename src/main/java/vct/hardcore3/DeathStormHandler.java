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
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.event.player.PlayerBedEnterEvent; // Añadimos la importación para capturar el evento de dormir

public class DeathStormHandler implements Listener {
    private final JavaPlugin plugin;
    private final Map<UUID, Integer> deathCount = new HashMap<>();
    private int remainingStormSeconds = 0;
    private int currentDay = 1;
    private boolean isDeathStormActive = false;  // Variable para marcar si está activa la DeathStorm

    public DeathStormHandler(JavaPlugin plugin) {
        this.plugin = plugin;
        loadStormData(); // Asegúrate de cargar los datos cuando se inicializa
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        UUID playerUUID = player.getUniqueId();
        deathCount.put(playerUUID, deathCount.getOrDefault(playerUUID, 0) + 1);

        int increment = 3600 * currentDay; // Cada muerte agrega horas basadas en el día actual
        remainingStormSeconds += increment;
        isDeathStormActive = true;  // Activamos la tormenta de muerte
        startStorm();
        saveStormData(); // Asegúrate de guardar los datos después de cada muerte
    }

    @EventHandler
    public void onWeatherChange(WeatherChangeEvent event) {
        // Si es una DeathStorm, evitamos que se pueda quitar el clima solo cuando es por dormir
        if (!event.toWeatherState()) {
            boolean isDeathTrainActive = checkIfDeathTrainActive();
            if (!isDeathTrainActive) {
                event.setCancelled(false); // Permite cancelar la tormenta si no está activo el evento DeathStorm
            } else {
                event.setCancelled(true); // No permite cancelar la tormenta si el evento DeathStorm está activo
            }
        }
    }

    @EventHandler
    public void onPlayerBedEnter(PlayerBedEnterEvent event) {
        if (isDeathStormActive) {  // Si la DeathStorm está activa
            event.getPlayer().sendMessage(ChatColor.GRAY + "No puedes dormir durante una DeathStorm.");
            event.setCancelled(true);  // Cancelamos la acción de dormir
        }
    }

    private boolean checkIfDeathTrainActive() {
        return isDeathStormActive;  // Indicamos si la DeathStorm está activa o no
    }

    public void changeDay(int day) {
        currentDay = day;
        saveStormData();
    }

    public int getCurrentDay() {
        return currentDay;
    }

    private BukkitRunnable stormTask; // Agrega esta variable

    private void startStorm() {
        World world = Bukkit.getWorlds().get(0);  // Ajusta según el mundo que quieras afectar

        // Cancela cualquier tarea de tormenta previa si está activa
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
                    isDeathStormActive = false;  // Desactivamos la DeathStorm cuando termina el tiempo
                    return;
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

        // Inicia la nueva tarea
        stormTask.runTaskTimer(plugin, 0, 20);  // Ejecuta cada segundo
    }

    public void resetStorm() {
        remainingStormSeconds = 0;
        isDeathStormActive = false;  // Desactivamos la DeathStorm al reiniciar
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

    public void saveStormData() {
        try {
            Path path = Paths.get(plugin.getDataFolder().getAbsolutePath(), "stormdata.txt");
            Files.createDirectories(path.getParent());
            Files.write(path, (currentDay + "\n" + remainingStormSeconds).getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loadStormData() {
        try {
            Path path = Paths.get(plugin.getDataFolder().getAbsolutePath(), "stormdata.txt");
            if (Files.exists(path)) {
                String[] data = new String(Files.readAllBytes(path)).split("\n");
                currentDay = Integer.parseInt(data[0]);
                remainingStormSeconds = Integer.parseInt(data[1]);
                if (remainingStormSeconds > 0) {
                    isDeathStormActive = true;  // Reactivamos la DeathStorm si el tiempo restante es mayor a 0
                    startStorm();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
