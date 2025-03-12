package Security.PingMonitor;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class PingMonitor {

    private final JavaPlugin plugin;
    private final File dataFolder;
    private final Map<UUID, Long> lastHighPingTime = new HashMap<>();
    private final int pingThreshold = 750; // Umbral de ping en ms
    private final long cooldownMillis = 10000; // 10 seg de cooldown para no spamear registros

    public PingMonitor(JavaPlugin plugin) {
        this.plugin = plugin;
        this.dataFolder = new File(plugin.getDataFolder(), "ms");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs(); // Crear la carpeta si no existe
        }
    }

    public void startMonitoring() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    int ping = player.getPing();
                    if (ping >= pingThreshold) {
                        handleHighPing(player, ping);
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 20L); // Ejecutar cada segundo (20 ticks)
    }

    private void handleHighPing(Player player, int ping) {
        UUID playerId = player.getUniqueId();
        long currentTime = System.currentTimeMillis();

        // Evitar registrar múltiples veces en un corto período
        long lastTime = lastHighPingTime.getOrDefault(playerId, 0L);
        if ((currentTime - lastTime) < cooldownMillis) {
            return; // Ya se registró un ping alto en los últimos X segundos
        }
        lastHighPingTime.put(playerId, currentTime);

        // Crear el registro en el archivo YAML
        File file = new File(dataFolder, "jugadoresms.yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        // Obtener la fecha y hora actual
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());

        // Sección: "<Nombre>.<timestamp>"
        String playerKey = player.getName() + "." + timestamp;
        config.set(playerKey + ".ping", ping);
        config.set(playerKey + ".world", player.getWorld().getName());
        config.set(playerKey + ".location", player.getLocation().toString());
        config.set(playerKey + ".last_command", getLastCommand(player));
        config.set(playerKey + ".time_on_server", getTimeOnServer(player));

        // "Verificar" si tu plugin está en el stack trace
        // (No es realmente fiable para ping, pero lo incluimos como curiosidad)
        String pluginCausante = checkPluginCausante();
        config.set(playerKey + ".causante", pluginCausante);

        try {
            config.save(file); // Guardar el archivo
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "No se pudo guardar el archivo jugadoresms.yml", e);
        }
    }

    // Obtener el último comando ejecutado por el jugador
    // Necesitarías algún plugin que registre comandos o manejarlo manualmente en un Listener
    private String getLastCommand(Player player) {
        return "No disponible";
    }

    // Obtener el tiempo que el jugador ha estado en el servidor (desde su primer ingreso)
    private String getTimeOnServer(Player player) {
        long timeMillis = System.currentTimeMillis() - player.getFirstPlayed();
        long hours = timeMillis / 3600000;
        long minutes = (timeMillis % 3600000) / 60000;
        return hours + "h " + minutes + "m";
    }


    private String checkPluginCausante() {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        for (StackTraceElement element : stackTrace) {
            if (element.getClassName().startsWith("vct.hardcore3")) {
                return element.getClassName() + "." + element.getMethodName() + "()";
            }
        }
        return "NULL";
    }
}
