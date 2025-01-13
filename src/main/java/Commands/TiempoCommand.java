package Commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TiempoCommand implements CommandExecutor, TabCompleter {

    private final JavaPlugin plugin;
    private final Map<String, BossBar> bossBars = new HashMap<>();
    private final Map<String, Integer> timers = new HashMap<>();
    private final Map<String, Boolean> soundSettings = new HashMap<>();
    private final Map<String, Integer> taskIds = new HashMap<>();


    private int unnamedCounter = 1;

    public TiempoCommand(JavaPlugin plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(new TiempoListener(this), plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Este comando solo puede ser ejecutado por jugadores.");
            return true;
        }

        Player player = (Player) sender;

        if (label.equalsIgnoreCase("addtiempo")) {
            if (args.length < 1 || args.length > 3) {
                player.sendMessage(ChatColor.RED + "Uso: /addtiempo [nombre opcional] <tiempo> [on/off]");
                return true;
            }

            String timeString;
            String name = "";
            String soundOption = "on";

            if (args[0].matches("\\d{2}:\\d{2}:\\d{2}")) {
                timeString = args[0];
                if (args.length == 2) {
                    soundOption = args[1].toLowerCase();
                }
            } else {
                name = ChatColor.translateAlternateColorCodes('&', args[0]);
                timeString = args[1];
                if (args.length == 3) {
                    soundOption = args[2].toLowerCase();
                }
            }

            // Validar formato del tiempo (hh:mm:ss)
            if (!timeString.matches("\\d{2}:\\d{2}:\\d{2}")) {
                player.sendMessage(ChatColor.RED + "Formato de tiempo inválido. Usa hh:mm:ss.");
                return true;
            }

            // Validar si la opción de sonido es válida
            if (!soundOption.equals("on") && !soundOption.equals("off")) {
                player.sendMessage(ChatColor.RED + "Uso: 'on' o 'off' para sonido.");
                return true;
            }

            // Convertir tiempo a segundos
            int totalSeconds = parseTimeToSeconds(timeString);
            if (totalSeconds <= 0) {
                player.sendMessage(ChatColor.RED + "El tiempo debe ser mayor que 0.");
                return true;
            }

            // Si no se especifica nombre, usar nombre por defecto
            if (name.isEmpty()) {
                name = "sinNombre" + unnamedCounter++;
            }

            // Crear o actualizar la BossBar
            createBossBar(name, totalSeconds, timeString, soundOption);
            player.sendMessage(ChatColor.GREEN + "Se agregó el temporizador con el nombre: " + ChatColor.RESET + name);
            return true;
        }

        // Comando para eliminar tiempo
        if (label.equalsIgnoreCase("removetiempo")) {
            if (args.length == 0) {
                boolean removed = removeFirstUnnamedBossBar();
                if (removed) {
                    player.sendMessage(ChatColor.GREEN + "Se eliminó la primera BossBar sin nombre.");
                } else {
                    player.sendMessage(ChatColor.RED + "No hay BossBars sin nombre.");
                }
                return true;
            }

            if (args.length == 1) {
                String name = args[0];
                removeBossBar(name);
                player.sendMessage(ChatColor.GREEN + "Se eliminó el temporizador con el nombre: " + name);
                return true;
            }

            player.sendMessage(ChatColor.RED + "Uso: /removetiempo [nombre opcional]");
            return true;
        }
        return false;
    }

    // Crear o actualizar la BossBar
    private void createBossBar(String name, int totalSeconds, String timeString, String soundOption) {
        if (bossBars.containsKey(name)) {
            removeBossBar(name);
        }

        // Crear BossBar
        BossBar bossBar = Bukkit.createBossBar(name, BarColor.WHITE, BarStyle.SOLID);
        bossBar.setVisible(true);

        // Mostrar BossBar a todos los jugadores
        Bukkit.getOnlinePlayers().forEach(bossBar::addPlayer);

        bossBars.put(name, bossBar);
        timers.put(name, totalSeconds);
        soundSettings.put(name, soundOption.equals("on"));

        startTimer(name, totalSeconds, bossBar, timeString);
    }


    // Iniciar temporizador
    private void startTimer(String name, int totalSeconds, BossBar bossBar, String timeString) {
        // Guardar ID de tarea para cancelarla después
        int taskId = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            if (!timers.containsKey(name)) return;

            int timeLeft = timers.get(name);
            if (timeLeft <= 0) {
                bossBar.setTitle(ChatColor.RED + "Tiempo Terminado!");
                bossBar.setProgress(0.0);

                Bukkit.getScheduler().runTaskLater(plugin, () -> removeBossBar(name), 100L);
                return;
            }

            // Actualiza tiempo restante y titulo
            timeLeft--;
            timers.put(name, timeLeft);

            String timeStringFormatted = formatTime(timeLeft);
            if (!name.startsWith("sinNombre")) {
                bossBar.setTitle(name + " " + ChatColor.WHITE + timeStringFormatted);
            } else {
                bossBar.setTitle(timeStringFormatted);
            }

            // Actualizar progreso
            double progress = (double) timeLeft / totalSeconds;
            bossBar.setProgress(Math.max(0.0, progress));

            // Reproducir sonido si está activado
            if (soundSettings.getOrDefault(name, true)) {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1f);
                }
            }
        }, 0L, 20L);

        taskIds.put(name, taskId);
    }


    private boolean removeFirstUnnamedBossBar() {
        for (String key : new ArrayList<>(bossBars.keySet())) {
            if (key.startsWith("sinNombre")) {
                removeBossBar(key);
                return true;
            }
        }
        return false;
    }

    private void removeBossBar(String name) {
        String strippedName = ChatColor.stripColor(name);
        for (String key : new ArrayList<>(bossBars.keySet())) {
            if (ChatColor.stripColor(key).equalsIgnoreCase(strippedName)) {
                // Cancelar tarea programada si existe
                if (taskIds.containsKey(key)) {
                    Bukkit.getScheduler().cancelTask(taskIds.get(key));
                    taskIds.remove(key);
                }

                // Eliminar BossBar
                BossBar bossBar = bossBars.get(key);
                bossBar.removeAll();
                bossBars.remove(key);
                timers.remove(key);
                soundSettings.remove(key);
                break;
            }
        }
    }

    // Convertir tiempo en formato hh:mm:ss a segundos
    private int parseTimeToSeconds(String timeString) {
        String[] parts = timeString.split(":");
        int hours = Integer.parseInt(parts[0]);
        int minutes = Integer.parseInt(parts[1]);
        int seconds = Integer.parseInt(parts[2]);
        return hours * 3600 + minutes * 60 + seconds;
    }

    // Formatear tiempo en segundos a hh:mm:ss
    private String formatTime(int seconds) {
        int h = seconds / 3600;
        int m = (seconds % 3600) / 60;
        int s = seconds % 60;
        return String.format("%02d:%02d:%02d", h, m, s);
    }

    public List<BossBar> getAllBossBars() {
        return new ArrayList<>(bossBars.values());
    }


    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> completions = new ArrayList<>();

        if (label.equalsIgnoreCase("removetiempo")) {
            if (args.length == 1) {
                for (String key : bossBars.keySet()) {
                    completions.add(ChatColor.stripColor(key));
                }
            }
        } else if (label.equalsIgnoreCase("addtiempo")) {
            if (args.length == 1) {
                if (args[0].matches("\\d{2}:\\d{2}:\\d{2}")) {
                    // Si es un tiempo, sugerir opciones de sonido
                    completions.add("on");
                    completions.add("off");
                } else {
                    // Si no es tiempo, sugerir tiempos predefinidos
                    completions.add("00:05:00");
                    completions.add("00:10:00");
                    completions.add("00:30:00");
                    completions.add("01:00:00");
                }
            } else if (args.length == 2) {
                // Si el primer argumento no es un tiempo, entonces el segundo debería ser el tiempo
                if (!args[0].matches("\\d{2}:\\d{2}:\\d{2}")) {
                    completions.add("00:05:00");
                    completions.add("00:10:00");
                    completions.add("00:30:00");
                    completions.add("01:00:00");
                } else {
                    // Si el primer argumento ya es un tiempo, sugerir opciones de sonido
                    completions.add("on");
                    completions.add("off");
                }
            } else if (args.length == 3) {
                completions.add("on");
                completions.add("off");
            }
        }

        return completions;
    }


}
