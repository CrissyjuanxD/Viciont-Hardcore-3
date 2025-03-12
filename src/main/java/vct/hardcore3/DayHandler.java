    package vct.hardcore3;
    
    import Dificultades.*;
    import org.bukkit.Bukkit;
    import org.bukkit.entity.Player;
    import org.bukkit.plugin.java.JavaPlugin;
    import org.bukkit.ChatColor;
    import org.bukkit.scheduler.BukkitRunnable;

    import java.io.IOException;
    import java.nio.file.Files;
    import java.nio.file.Path;
    import java.nio.file.Paths;
    
    public class DayHandler {
        private final JavaPlugin plugin;
        private int currentDay = 1;
        private int remainingDaySeconds = 86400;
        private BukkitRunnable dayTask;
        private DayOneChanges dayOneChanges;
        private DayTwoChanges dayTwoChange;
        private DayFourChanges dayFourChanges;
        private DaySixChanges daySixChanges;
        private DaySevenChanges daySevenChanges;
        private DayTenChanges dayTenChanges;
    
        public DayHandler(JavaPlugin plugin) {
            this.plugin = plugin;
            dayOneChanges = new DayOneChanges(plugin, this);
            dayTwoChange = new DayTwoChanges(plugin);
            dayFourChanges = new DayFourChanges(plugin);
            daySixChanges = new DaySixChanges(plugin);
            daySevenChanges = new DaySevenChanges(plugin, this);
            dayTenChanges = new DayTenChanges(plugin);
            loadDayData();
            startDayTimer();
            applyCurrentDayChanges();
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
                        remainingDaySeconds = 86400;
                    }
    
                    remainingDaySeconds--;
                }
            };
    
            dayTask.runTaskTimer(plugin, 0, 20);
        }
    
        // Avanzar el día
        private void advanceDay() {
            currentDay++;
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.sendMessage(ChatColor.GOLD + "¡Es el día " + currentDay + "!");
            }
            applyCurrentDayChanges();
            saveDayData();
        }

        public void changeDay(int day) {
            revertCurrentDayChanges();  // Revertir los cambios antes de cambiar el día
            currentDay = day;
            remainingDaySeconds = 86400;  // Reiniciar el temporizador al cambiar de día
            startDayTimer();

            for (Player player : Bukkit.getOnlinePlayers()) {
                player.sendMessage(ChatColor.GOLD + "El día ha sido cambiado manualmente al día " + currentDay + ".");
            }

            applyCurrentDayChanges();
            saveDayData();
        }


        private void applyCurrentDayChanges() {
            if (currentDay >= 1) {
                dayOneChanges.apply();
            }
            if (currentDay >= 2) {
                dayTwoChange.apply();
            }
            if (currentDay >= 4) {
                dayFourChanges.apply();
            }
            if (currentDay >= 6) {
                daySixChanges.apply();
            }
            if (currentDay >= 7) {
                daySevenChanges.apply();
            }
            if (currentDay >= 10) {
                dayTenChanges.apply();
            }
        }

        private void revertCurrentDayChanges() {
            // Revertir cambios del día 10 si están aplicados
            if (currentDay < 10) {
                dayTenChanges.revert();
            }
            // Revertir cambios del día 7 si están aplicados
            if (currentDay < 7) {
                daySevenChanges.revert();
            }
            // Revertir cambios del día 6 si están aplicados
            if (currentDay < 6) {
                daySixChanges.revert();
            }
            // Revertir cambios del día 4 si están aplicados
            if (currentDay < 4) {
                dayFourChanges.revert();
            }
            // Revertir cambios del día 2 si están aplicados
            if (currentDay < 2) {
                dayTwoChange.revert();
            }
            // Revertir cambios del día 1 si están aplicados
            if (currentDay < 1) {
                dayOneChanges.revert();
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
