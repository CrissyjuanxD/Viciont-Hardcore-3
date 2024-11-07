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
        private boolean dayOneChangesApplied = false;  // Variable para rastrear si los cambios del día 1 están aplicados
    
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

        public void changeDay(int day) {
            revertCurrentDayChanges();  // Revertir los cambios antes de cambiar el día
            currentDay = day;  // Cambia el día después de revertir
            remainingDaySeconds = 86400;  // Reiniciar el temporizador al cambiar de día
            startDayTimer();  // Reiniciar el temporizador

            for (Player player : Bukkit.getOnlinePlayers()) {
                player.sendMessage(ChatColor.GOLD + "El día ha sido cambiado manualmente al día " + currentDay + ".");
            }

            applyCurrentDayChanges();  // Aplicar cambios para el nuevo día
            saveDayData();
        }


        private void applyCurrentDayChanges() {
            if (currentDay == 1 && !dayOneChangesApplied) {
                dayOneChanges.apply();
                dayOneChangesApplied = true;
            }
            // Si hay lógica adicional para otros días, agrégala aquí
        }

        private void revertCurrentDayChanges() {
            if (dayOneChangesApplied && currentDay != 1) {  // Revierte si ya se aplicó el cambio del día 1
                dayOneChanges.revert();
                dayOneChangesApplied = false;
            }
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
