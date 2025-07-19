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

import java.util.*;
import java.util.stream.Collectors;

public class TiempoCommand implements CommandExecutor, TabCompleter {

    private final JavaPlugin plugin;
    private final Map<String, BossBar> bossBars = new HashMap<>();
    private final Map<String, Integer> timers = new HashMap<>();
    private final Map<String, Boolean> soundSettings = new HashMap<>();
    private final Map<String, Integer> taskIds = new HashMap<>();
    private final Map<String, String> displayNames = new HashMap<>();
    private final Map<UUID, Set<String>> playerBossBars = new HashMap<>();

    private int unnamedCounter = 1;

    public TiempoCommand(JavaPlugin plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(new TiempoListener(this), plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (label.equalsIgnoreCase("addtiempo")) {
            return handleAddTiempo(sender, args);
        }

        if (label.equalsIgnoreCase("removetiempo")) {
            return handleRemoveTiempo(sender, args);
        }

        if (label.equalsIgnoreCase("tiempoview")) {
            return handleTiempoView(sender, args);
        }

        return false;
    }

    private boolean handleAddTiempo(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "Uso: /addtiempo [player/@a] [nombre opcional] <tiempo> [on/off]");
            return true;
        }

        // Reconstruir los argumentos para manejar nombres con espacios
        List<String> argsList = new ArrayList<>(Arrays.asList(args));

        // Determinar si el primer argumento es un jugador o @a
        String target = "all"; // Por defecto para todos
        int startIndex = 0;

        if (argsList.get(0).startsWith("@") || Bukkit.getPlayer(argsList.get(0)) != null) {
            target = argsList.get(0);
            startIndex = 1;
        }

        // Buscar el argumento de tiempo (hh:mm:ss)
        int timeIndex = -1;
        String timeString = "";
        for (int i = startIndex; i < argsList.size(); i++) {
            if (argsList.get(i).matches("\\d{2}:\\d{2}:\\d{2}")) {
                timeIndex = i;
                timeString = argsList.get(i);
                break;
            }
        }

        if (timeIndex == -1) {
            sender.sendMessage(ChatColor.RED + "Formato de tiempo inválido. Usa hh:mm:ss.");
            return true;
        }

        // El nombre es todo entre el target y el tiempo
        String name = "";
        if (timeIndex > startIndex) {
            name = String.join(" ", argsList.subList(startIndex, timeIndex));
            name = ChatColor.translateAlternateColorCodes('&', name);
        }

        // El sonido es el argumento después del tiempo si existe
        String soundOption = "on";
        if (timeIndex < argsList.size() - 1) {
            soundOption = argsList.get(timeIndex + 1).toLowerCase();
            if (!soundOption.equals("on") && !soundOption.equals("off")) {
                sender.sendMessage(ChatColor.RED + "Uso: 'on' o 'off' para sonido.");
                return true;
            }
        }

        if (!timeString.matches("\\d{2}:\\d{2}:\\d{2}")) {
            sender.sendMessage(ChatColor.RED + "Formato de tiempo inválido. Usa hh:mm:ss.");
            return true;
        }

        int totalSeconds = parseTimeToSeconds(timeString);
        if (totalSeconds <= 0) {
            sender.sendMessage(ChatColor.RED + "El tiempo debe ser mayor que 0.");
            return true;
        }

        if (name.isEmpty()) {
            name = "sinNombre" + unnamedCounter++;
        }

        String customId = null;
        if ((argsList.size() - timeIndex) > 2) { // Hay argumentos adicionales después del sonido
            customId = argsList.get(argsList.size() - 1);
        }

        // Crear la bossbar para los jugadores especificados
        if (target.equalsIgnoreCase("@a") || target.equals("all")) {
            createBossBar(name, totalSeconds, timeString, soundOption);
            sender.sendMessage(ChatColor.GREEN + "Se agregó el temporizador con el nombre: " + ChatColor.RESET + name + ChatColor.GREEN + " para todos los jugadores.");
        } else {
            Player player = Bukkit.getPlayer(target);
            if (player != null) {
                if (customId != null) {
                    createPlayerBossBar(player, name, totalSeconds, timeString, soundOption, customId);
                    } else {
                        createPlayerBossBar(player, name, totalSeconds, timeString, soundOption);
                    }
                sender.sendMessage(ChatColor.GREEN + "Se agregó el temporizador con el nombre: " + ChatColor.RESET + name + ChatColor.GREEN + " para " + player.getName());
            } else {
                sender.sendMessage(ChatColor.RED + "Jugador no encontrado.");
                return true;
            }
        }

        return true;
    }

    private boolean handleRemoveTiempo(CommandSender sender, String[] args) {
        if (args.length == 0) {
            boolean removed = removeFirstUnnamedBossBar();
            if (removed) {
                sender.sendMessage(ChatColor.GREEN + "Se eliminó la primera BossBar sin nombre.");
            } else {
                sender.sendMessage(ChatColor.RED + "No hay BossBars sin nombre.");
            }
            return true;
        }

        if (args.length >= 1) {
            // Unir todos los argumentos y limpiar comillas y códigos de color
            String inputName = String.join(" ", args)
                    .replace("\"", "")
                    .replace("&", "");  // Eliminar códigos de color

            // Buscar coincidencia exacta primero
            for (Map.Entry<String, String> entry : displayNames.entrySet()) {
                String plainName = ChatColor.stripColor(entry.getValue());
                if (plainName.equalsIgnoreCase(inputName)) {
                    removeBossBarInternal(entry.getKey());
                    sender.sendMessage(ChatColor.GREEN + "Se eliminó el temporizador con el nombre: " + entry.getValue());
                    return true;
                }
            }

            // Si no se encontró coincidencia exacta, buscar parcial
            for (Map.Entry<String, String> entry : displayNames.entrySet()) {
                String plainName = ChatColor.stripColor(entry.getValue());
                if (plainName.equalsIgnoreCase(inputName) || plainName.contains(inputName)) {
                    removeBossBarInternal(entry.getKey());
                    sender.sendMessage(ChatColor.GREEN + "Se eliminó el temporizador con el nombre: " + entry.getValue());
                    return true;
                }
            }

            sender.sendMessage(ChatColor.RED + "No se encontró una BossBar con ese nombre.");
            return true;
        }

        sender.sendMessage(ChatColor.RED + "Uso: /removetiempo [nombre opcional]");
        return true;
    }

    private boolean handleTiempoView(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(ChatColor.RED + "Uso: /tiempoview <jugador/all>");
            return true;
        }

        String target = args[0];

        if (target.equalsIgnoreCase("all")) {
            // Mostrar todas las bossbars activas
            if (bossBars.isEmpty()) {
                sender.sendMessage(ChatColor.YELLOW + "No hay BossBars de tiempo activas.");
                return true;
            }

            sender.sendMessage(ChatColor.GOLD + "=== Todas las BossBars de Tiempo ===");
            for (Map.Entry<String, BossBar> entry : bossBars.entrySet()) {
                String barId = entry.getKey();
                BossBar bar = entry.getValue();
                int timeLeft = timers.getOrDefault(barId, 0);
                boolean soundOn = soundSettings.getOrDefault(barId, true);
                String displayName = displayNames.getOrDefault(barId, barId);

                // Determinar si es global o de jugador
                String owner = "Global";
                if (barId.contains("_")) {
                    try {
                        UUID playerId = UUID.fromString(barId.split("_")[0]);
                        Player player = Bukkit.getPlayer(playerId);
                        owner = player != null ? player.getName() : "Jugador desconocido";
                    } catch (IllegalArgumentException e) {
                        owner = "Jugador desconocido";
                    }
                }

                sender.sendMessage(ChatColor.GREEN + "Dueño: " + ChatColor.WHITE + owner);
                sender.sendMessage(ChatColor.GREEN + "Nombre: " + ChatColor.WHITE + displayName);
                sender.sendMessage(ChatColor.GREEN + "Tiempo restante: " + ChatColor.WHITE + formatTime(timeLeft));
                sender.sendMessage(ChatColor.GREEN + "Sonido: " + (soundOn ? ChatColor.GREEN + "ON" : ChatColor.RED + "OFF"));
                sender.sendMessage("");
            }
        } else {
            // Mostrar bossbars de un jugador específico
            Player targetPlayer = Bukkit.getPlayer(target);
            if (targetPlayer == null) {
                sender.sendMessage(ChatColor.RED + "Jugador no encontrado.");
                return true;
            }

            Set<String> playerBars = playerBossBars.getOrDefault(targetPlayer.getUniqueId(), new HashSet<>());

            if (playerBars.isEmpty()) {
                sender.sendMessage(ChatColor.YELLOW + targetPlayer.getName() + " no tiene BossBars de tiempo activas.");
                return true;
            }

            sender.sendMessage(ChatColor.GOLD + "=== BossBars de Tiempo de " + targetPlayer.getName() + " ===");
            for (String barId : playerBars) {
                if (bossBars.containsKey(barId)) {
                    BossBar bar = bossBars.get(barId);
                    int timeLeft = timers.getOrDefault(barId, 0);
                    boolean soundOn = soundSettings.getOrDefault(barId, true);
                    String displayName = displayNames.getOrDefault(barId, barId);

                    sender.sendMessage(ChatColor.GREEN + "Nombre: " + ChatColor.WHITE + displayName);
                    sender.sendMessage(ChatColor.GREEN + "Tiempo restante: " + ChatColor.WHITE + formatTime(timeLeft));
                    sender.sendMessage(ChatColor.GREEN + "Sonido: " + (soundOn ? ChatColor.GREEN + "ON" : ChatColor.RED + "OFF"));
                    sender.sendMessage("");
                }
            }
        }

        return true;
    }

    public void createBossBar(String name, int totalSeconds, String timeString, String soundOption) {
        if (bossBars.containsKey(name)) {
            removeBossBar(name);
        }

        BossBar bossBar = Bukkit.createBossBar(name, BarColor.WHITE, BarStyle.SOLID);
        bossBar.setVisible(true);

        Bukkit.getOnlinePlayers().forEach(bossBar::addPlayer);

        bossBars.put(name, bossBar);
        timers.put(name, totalSeconds);
        soundSettings.put(name, soundOption.equals("on"));
        displayNames.put(name, name);

        startTimer(name, totalSeconds, bossBar, timeString);
    }

    // Versión original (5 parámetros)
    public void createPlayerBossBar(Player player, String name, int totalSeconds, String timeString, String soundOption) {
        // Generar un ID automático basado en el UUID del jugador y el nombre
        String barId = player.getUniqueId() + "_" + name;
        createPlayerBossBar(player, name, totalSeconds, timeString, soundOption, barId);
    }

    // Versión extendida (6 parámetros)
    public void createPlayerBossBar(Player player, String name, int totalSeconds, String timeString, String soundOption, String barId) {
        if (bossBars.containsKey(barId)) {
            removeBossBar(barId);
        }

        BossBar bossBar = Bukkit.createBossBar(name, BarColor.WHITE, BarStyle.SOLID);
        bossBar.setVisible(true);
        bossBar.addPlayer(player);

        bossBars.put(barId, bossBar);
        timers.put(barId, totalSeconds);
        soundSettings.put(barId, soundOption.equals("on"));
        displayNames.put(barId, name);

        playerBossBars.computeIfAbsent(player.getUniqueId(), k -> new HashSet<>()).add(barId);
        startTimer(barId, totalSeconds, bossBar, timeString);
    }

    private void startTimer(String name, int totalSeconds, BossBar bossBar, String timeString) {
        int taskId = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            if (!timers.containsKey(name)) return;

            int timeLeft = timers.get(name);
            if (timeLeft <= 0) {
                bossBar.setTitle(ChatColor.RED + "Tiempo Terminado!");
                bossBar.setProgress(0.0);

                // Reproducir sonido solo para los jugadores que ven esta bossbar
                if (name.contains("_")) { // Es una bossbar de jugador
                    UUID playerId = UUID.fromString(name.split("_")[0]);
                    Player player = Bukkit.getPlayer(playerId);
                    if (player != null) {
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_DIDGERIDOO, 1.0f, 0.1f);
                    }
                } else { // Es una bossbar global
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        if (bossBar.getPlayers().contains(player)) {
                            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_DIDGERIDOO, 1.0f, 0.1f);
                        }
                    }
                }

                Bukkit.getScheduler().runTaskLater(plugin, () -> removeBossBar(name), 50L);
                return;
            }

            timeLeft--;
            timers.put(name, timeLeft);

            String timeStringFormatted = formatTime(timeLeft);
            String displayName = displayNames.getOrDefault(name, name);

            if (!name.startsWith("sinNombre")) {
                bossBar.setTitle(displayName + " " + ChatColor.WHITE + timeStringFormatted);
            } else {
                bossBar.setTitle(timeStringFormatted);
            }

            double progress = (double) timeLeft / totalSeconds;
            bossBar.setProgress(Math.max(0.0, progress));

            if (soundSettings.getOrDefault(name, true)) {
                if (name.contains("_")) { // Es una bossbar de jugador
                    UUID playerId = UUID.fromString(name.split("_")[0]);
                    Player player = Bukkit.getPlayer(playerId);
                    if (player != null) {
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1f, 1.5f);
                    }
                } else { // Es una bossbar global
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        if (bossBar.getPlayers().contains(player)) {
                            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1f, 1.5f);
                        }
                    }
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

    public boolean removeBossBar(String name) {
        // Primero intentar coincidencia exacta
        if (bossBars.containsKey(name)) {
            removeBossBarInternal(name);
            return true;
        }

        // Si no, buscar por nombre de visualización (incluyendo coincidencia parcial)
        for (Map.Entry<String, String> entry : displayNames.entrySet()) {
            if (entry.getValue().equalsIgnoreCase(name) || entry.getKey().equalsIgnoreCase(name)) {
                removeBossBarInternal(entry.getKey());
                return true;
            }
        }

        // Finalmente, buscar por coincidencia parcial en nombres con espacios
        for (String key : new ArrayList<>(bossBars.keySet())) {
            String displayName = displayNames.getOrDefault(key, key);
            if (displayName.equalsIgnoreCase(name) ||
                    key.equalsIgnoreCase(name) ||
                    displayName.contains(name) ||
                    key.contains(name)) {
                removeBossBarInternal(key);
                return true;
            }
        }

        return false;
    }

    private void removeBossBarInternal(String key) {
        if (taskIds.containsKey(key)) {
            Bukkit.getScheduler().cancelTask(taskIds.get(key));
            taskIds.remove(key);
        }

        BossBar bossBar = bossBars.get(key);
        if (bossBar != null) {
            bossBar.removeAll();

            // Si es una bossbar de jugador, quitarla de su registro
            if (key.contains("_")) {
                try {
                    UUID playerId = UUID.fromString(key.split("_")[0]);
                    playerBossBars.computeIfPresent(playerId, (uuid, bars) -> {
                        bars.remove(key);
                        return bars.isEmpty() ? null : bars;
                    });
                } catch (IllegalArgumentException e) {
                    // Ignorar si el UUID no es válido
                }
            }
        }

        bossBars.remove(key);
        timers.remove(key);
        soundSettings.remove(key);
        displayNames.remove(key);
    }

    public BossBar getBossBar(String name) {
        return bossBars.get(name);
    }

    public void updateBossBarDisplayName(String id, String newDisplayName) {
        if (displayNames.containsKey(id)) {
            displayNames.put(id, newDisplayName);
            BossBar bossBar = bossBars.get(id);
            if (bossBar != null) {
                // Reconstruir el título completamente con el tiempo actual
                int timeLeft = timers.getOrDefault(id, 0);
                String timeStringFormatted = formatTime(timeLeft);
                bossBar.setTitle(newDisplayName + " " + ChatColor.WHITE + timeStringFormatted);
            }
        }
    }

    private int parseTimeToSeconds(String timeString) {
        String[] parts = timeString.split(":");
        int hours = Integer.parseInt(parts[0]);
        int minutes = Integer.parseInt(parts[1]);
        int seconds = Integer.parseInt(parts[2]);
        return hours * 3600 + minutes * 60 + seconds;
    }

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
                String currentArg = args[0].toLowerCase();
                for (String displayName : displayNames.values()) {
                    String plainName = ChatColor.stripColor(displayName);
                    if (plainName.toLowerCase().startsWith(currentArg)) {
                        if (plainName.contains(" ")) {
                            // Mostrar con comillas solo si contiene espacios
                            completions.add("\"" + plainName + "\"");
                        } else {
                            completions.add(plainName);
                        }
                    }
                }
            }
        } else if (label.equalsIgnoreCase("addtiempo")) {
            if (args.length == 1) {
                // Sugerir jugadores online o @a
                completions.add("@a");
                for (Player player : Bukkit.getOnlinePlayers()) {
                    completions.add(player.getName());
                }

                if (args[0].matches("\\d{2}:\\d{2}:\\d{2}")) {
                    completions.add("on");
                    completions.add("off");
                } else {
                    completions.add("00:05:00");
                    completions.add("00:10:00");
                    completions.add("00:30:00");
                    completions.add("01:00:00");
                }
            } else if (args.length == 2) {
                if (!args[0].matches("\\d{2}:\\d{2}:\\d{2}") && !args[0].equalsIgnoreCase("@a") &&
                        Bukkit.getPlayer(args[0]) == null) {
                    completions.add("00:05:00");
                    completions.add("00:10:00");
                    completions.add("00:30:00");
                    completions.add("01:00:00");
                } else {
                    completions.add("on");
                    completions.add("off");
                }
            } else if (args.length == 3) {
                completions.add("on");
                completions.add("off");
            }
        } else if (label.equalsIgnoreCase("tiempoview")) {
            if (args.length == 1) {
                completions.add("all");
                for (Player player : Bukkit.getOnlinePlayers()) {
                    completions.add(player.getName());
                }
            }
        }
        return completions;
    }
}