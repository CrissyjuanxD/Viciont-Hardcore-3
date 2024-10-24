package vct.hardcore3;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.ChatColor;
import org.bukkit.scheduler.BukkitRunnable;
import Dificultades.DayOneChanges;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class DayHandler {
    private final JavaPlugin plugin;
    private int currentDay = 1;
    private int remainingDaySeconds = 86400; // 24 horas en segundos
    private BukkitRunnable dayTask;
    private DayOneChanges dayOneChanges;

    public DayHandler(JavaPlugin plugin) {
        this.plugin = plugin;
        dayOneChanges = new DayOneChanges(plugin); // Inicializar DayOneChanges
        loadDayData();  // Cargar los datos del día al inicio
        startDayTimer(); // Iniciar el temporizador de días
        applyCurrentDayChanges(); // Aplicar cambios del día actual
    }

    // Iniciar o reiniciar el temporizador de días
    private void startDayTimer() {
        if (dayTask != null && !dayTask.isCancelled()) {
            dayTask.cancel();
        }

        dayTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (remainingDaySeconds <= 0) {
                    advanceDay();
                    remainingDaySeconds = 86400; // Reiniciar a 24 horas
                }

                remainingDaySeconds--;
            }
        };

        dayTask.runTaskTimer(plugin, 0, 20);  // Ejecutar cada segundo
    }

    // Avanzar el día
    private void advanceDay() {
        currentDay++;
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(ChatColor.GOLD + "¡Es el día " + currentDay + "!");
        }
        applyCurrentDayChanges(); // Aplicar cambios del nuevo día
        saveDayData();
    }

    // Cambiar el día manualmente
    public void changeDay(int day) {
        revertCurrentDayChanges(); // Revertir cambios del día anterior

        currentDay = day;
        remainingDaySeconds = 86400;  // Reiniciar el temporizador al cambiar de día
        startDayTimer();  // Reiniciar el temporizador
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(ChatColor.GOLD + "El día ha sido cambiado manualmente al día " + currentDay + ".");
        }
        applyCurrentDayChanges(); // Aplicar cambios del nuevo día
        saveDayData();
    }

    // Aplicar cambios según el día actual
    private void applyCurrentDayChanges() {
        if (currentDay == 1) {
            dayOneChanges.apply(); // Aplica cambios del día 1
        }
        // Agregar más días según sea necesario
    }

    // Revertir cambios del día actual
    private void revertCurrentDayChanges() {
        if (currentDay < 1) {
            dayOneChanges.revert(); // Revertir cambios del día 1
        }
        // Revertir cambios de días adicionales si es necesario
    }

    public int getCurrentDay() {
        return currentDay;
    }

    // Guardar el día actual en un archivo
    private void saveDayData() {
        try {
            Path path = Paths.get(plugin.getDataFolder().getAbsolutePath(), "daydata.txt");
            Files.createDirectories(path.getParent());
            Files.write(path, String.valueOf(currentDay).getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Cargar el día desde un archivo
    private void loadDayData() {
        try {
            Path path = Paths.get(plugin.getDataFolder().getAbsolutePath(), "daydata.txt");
            if (Files.exists(path)) {
                currentDay = Integer.parseInt(new String(Files.readAllBytes(path)));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
