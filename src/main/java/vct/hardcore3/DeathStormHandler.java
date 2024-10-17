package vct.hardcore3;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;


import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DeathStormHandler implements Listener {
    private final JavaPlugin plugin;
    private final Map<UUID, Integer> deathCount = new HashMap<>();
    private int remainingStormSeconds = 0;

    public DeathStormHandler(JavaPlugin plugin) {
        this.plugin = plugin;
        loadStormData(); // Asegúrate de cargar los datos cuando se inicializa
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        UUID playerUUID = player.getUniqueId();
        deathCount.put(playerUUID, deathCount.getOrDefault(playerUUID, 0) + 1);

        int increment = 3600; // Cada muerte agrega 1 hora en segundos
        remainingStormSeconds += increment;
        startStorm();
        saveStormData(); // Asegúrate de guardar los datos después de cada muerte
    }

    private void startStorm() {
        World world = Bukkit.getWorlds().get(0);  // Ajusta esto según el mundo que quieras afectar
        world.setStorm(true);
        world.setThundering(true);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (remainingStormSeconds <= 0) {
                    cancel();
                    world.setStorm(false);
                    world.setThundering(false);
                    return;
                }

                int hours = remainingStormSeconds / 3600;
                int minutes = (remainingStormSeconds % 3600) / 60;
                int seconds = remainingStormSeconds % 60;
                String timeMessage = String.format(ChatColor.DARK_PURPLE + "Quedan %02d:%02d:%02d horas de DeathStorm", hours, minutes, seconds);

                for (Player player : Bukkit.getOnlinePlayers()) {
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(timeMessage));
                }

                remainingStormSeconds--;
            }
        }.runTaskTimer(plugin, 0, 20);  // Ejecuta cada segundo
    }

    public void resetStorm() {
        remainingStormSeconds = 0;
        saveStormData(); // Asegúrate de guardar los datos al reiniciar
    }

    public void addStormHours(int hours) {
        remainingStormSeconds += hours * 3600;
        saveStormData(); // Asegúrate de guardar los datos después de añadir horas
    }

    public void removeStormHours(int hours) {
        remainingStormSeconds = Math.max(remainingStormSeconds - (hours * 3600), 0);
        saveStormData(); // Asegúrate de guardar los datos después de remover horas
    }

    public void saveStormData() {
        // Implementar la lógica para guardar los datos de la tormenta
    }

    public void loadStormData() {
        // Implementar la lógica para cargar los datos de la tormenta
    }
}

