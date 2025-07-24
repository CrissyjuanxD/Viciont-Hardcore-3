    package Handlers;
    
    import Dificultades.*;
    import org.bukkit.Bukkit;
    import org.bukkit.configuration.file.YamlConfiguration;
    import org.bukkit.entity.Player;
    import org.bukkit.plugin.java.JavaPlugin;
    import org.bukkit.ChatColor;
    import org.bukkit.scheduler.BukkitRunnable;

    import java.io.File;
    import java.io.IOException;
    
    public class DayHandler {
        private final JavaPlugin plugin;
        private int currentDay = 1;
        private int remainingDaySeconds = 86400;
        private BukkitRunnable dayTask;
        private DayOneChanges dayOneChanges;
        private DayTwoChanges dayTwoChange;
        private DayFourChanges dayFourChanges;
        private DayFiveChange dayFiveChanges;
        private DaySixChanges daySixChanges;
        private DaySevenChanges daySevenChanges;
        private DayEightChanges dayEightChanges;
        private DayNineChanges dayNineChanges;
        private DayTenChanges dayTenChanges;
        private DayTwelveChanges dayTwelveChanges;
        private DayThirteenChanges dayThirteenChanges;
        private DayFourteenChanges dayFourteenChanges;
        private DayFifteenChanges dayFifteenChanges;
        private DaySixteenChanges daySixteenChanges;
    
        public DayHandler(JavaPlugin plugin) {
            this.plugin = plugin;
            dayOneChanges = new DayOneChanges(plugin, this);
            dayTwoChange = new DayTwoChanges(plugin);
            dayFourChanges = new DayFourChanges(plugin,this);
            dayFiveChanges = new DayFiveChange(plugin, this);
            daySixChanges = new DaySixChanges(plugin, this);
            daySevenChanges = new DaySevenChanges(plugin, this);
            dayEightChanges = new DayEightChanges(plugin, this);
            dayNineChanges = new DayNineChanges(plugin, this);
            dayTenChanges = new DayTenChanges(plugin, this);
            dayTwelveChanges = new DayTwelveChanges(plugin, this);
            dayThirteenChanges = new DayThirteenChanges(plugin, this);
            dayFourteenChanges = new DayFourteenChanges(plugin, this);
            dayFifteenChanges = new DayFifteenChanges(plugin, this);
            daySixteenChanges = new DaySixteenChanges(plugin, this);
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

        private void advanceDay() {
            currentDay++;
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.sendMessage(ChatColor.GOLD + "¡Es el día " + currentDay + "!");
            }
            applyCurrentDayChanges();
            saveDayData();
        }

        public void changeDay(int day) {
            revertCurrentDayChanges();
            currentDay = day;
            remainingDaySeconds = 86400;
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
            if (currentDay >= 5) {
                dayFiveChanges.apply();
            }
            if (currentDay >= 6) {
                daySixChanges.apply();
            }
            if (currentDay >= 7) {
                daySevenChanges.apply();
            }
            if (currentDay >= 8) {
                dayEightChanges.apply();
            }
            if (currentDay >= 9) {
                dayNineChanges.apply();
            }
            if (currentDay >= 10) {
                dayTenChanges.apply();
            }
            if (currentDay >= 12) {
                dayTwelveChanges.apply();
            }
            if (currentDay >= 13) {
                dayThirteenChanges.apply();
            }
            if (currentDay >= 14) {
                dayFourteenChanges.apply();
            }
            if (currentDay >= 15) {
                dayFifteenChanges.apply();
            }
            if (currentDay >= 16) {
                daySixteenChanges.apply();
            }
        }

        private void revertCurrentDayChanges() {
            if (currentDay < 16) {
                daySixteenChanges.revert();
            }
            if (currentDay < 15) {
                dayFifteenChanges.revert();
            }
            if (currentDay < 14) {
                dayFourteenChanges.revert();
            }
            if (currentDay < 13) {
                dayThirteenChanges.revert();
            }
            if (currentDay < 12) {
                dayTwelveChanges.revert();
            }
            if (currentDay < 10) {
                dayTenChanges.revert();
            }
            if (currentDay < 9) {
                dayNineChanges.revert();
            }
            if (currentDay < 8) {
                dayEightChanges.revert();
            }
            if (currentDay < 7) {
                daySevenChanges.revert();
            }
            if (currentDay < 6) {
                daySixChanges.revert();
            }
            if (currentDay < 5) {
                dayFiveChanges.revert();
            }
            if (currentDay < 4) {
                dayFourChanges.revert();
            }
            if (currentDay < 2) {
                dayTwoChange.revert();
            }
            if (currentDay < 1) {
                dayOneChanges.revert();
            }
        }

        public int getCurrentDay() {
            return currentDay;
        }

        private void saveDayData() {
            try {
                File file = new File(plugin.getDataFolder(), "DayandStorm.yml");
                YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

                config.set("DiaActual", currentDay);

                config.save(file);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void loadDayData() {
            File file = new File(plugin.getDataFolder(), "DayandStorm.yml");
            if (file.exists()) {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                currentDay = config.getInt("DiaActual", 1);
            }
        }
    }
