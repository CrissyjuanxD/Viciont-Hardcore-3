package Commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.StringUtil;

import java.util.*;

public class TiempoCommand implements CommandExecutor, TabCompleter {

    private final JavaPlugin plugin;
    private final Map<String, BossBar> bossBars = new HashMap<>();
    private final Map<String, Integer> timers = new HashMap<>();
    private final Map<String, Integer> initialTimers = new HashMap<>();
    private final Map<String, Boolean> soundSettings = new HashMap<>();
    private final Map<String, Integer> taskIds = new HashMap<>();
    private final Map<String, String> displayNames = new HashMap<>();
    private final Map<UUID, Set<String>> playerBossBars = new HashMap<>();
    private final Map<String, String> timerActions = new HashMap<>();

    private int unnamedCounter = 1;

    public TiempoCommand(JavaPlugin plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(new TiempoListener(this), plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!label.equalsIgnoreCase("timers")) return false;

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();
        String[] subArgs = Arrays.copyOfRange(args, 1, args.length);

        switch (subCommand) {
            case "crear":
                return handleAddTiempo(sender, subArgs);
            case "remover":
                return handleRemoveTiempo(sender, subArgs);
            case "lista":
                return handleTiempoView(sender, subArgs);
            case "editar":
                return handleEditTiempo(sender, subArgs);
            default:
                sendHelp(sender);
                return true;
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== Sistema de Timers ===");
        sender.sendMessage(ChatColor.YELLOW + "/timers crear " + ChatColor.WHITE + "<jugador/@a> [nombre=\"...\"] [tiempo=hh:mm:ss] [sonido=on/off] [comando=\"...\"]");
        sender.sendMessage(ChatColor.YELLOW + "/timers remover " + ChatColor.WHITE + "<\"Nombre del Timer\">");
        sender.sendMessage(ChatColor.YELLOW + "/timers editar " + ChatColor.WHITE + "<\"Nombre Actual\"> [nombre=\"Nuevo\"] [tiempo=hh:mm:ss] [sonido=on/off] [comando=\"nuevo comando\"]");
        sender.sendMessage(ChatColor.YELLOW + "/timers lista " + ChatColor.WHITE + "<jugador/all>");
    }

    // --- SUBCOMANDO: CREAR ---
    private boolean handleAddTiempo(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Uso: /timers crear <jugador/@a> [tiempo=hh:mm:ss] [nombre=\"...\"] [sonido=on/off] [comando=\"...\"]");
            return true;
        }

        String target = args[0];
        String fullArgs = String.join(" ", Arrays.copyOfRange(args, 1, args.length));

        String name = "sinNombre" + unnamedCounter++;
        String timeString = "";
        String soundOption = "on";
        String actionCommand = null;

        // Extraer Nombre
        if (fullArgs.contains("nombre=\"")) {
            int start = fullArgs.indexOf("nombre=\"") + 8;
            int end = fullArgs.indexOf("\"", start);
            if (end != -1) {
                name = ChatColor.translateAlternateColorCodes('&', fullArgs.substring(start, end));
            }
        }

        // Extraer Tiempo
        if (fullArgs.contains("tiempo=")) {
            int start = fullArgs.indexOf("tiempo=") + 7;
            int end = fullArgs.indexOf(" ", start);
            if (end == -1) end = fullArgs.length();
            timeString = fullArgs.substring(start, end);
        }

        // Extraer Sonido
        if (fullArgs.contains("sonido=")) {
            int start = fullArgs.indexOf("sonido=") + 7;
            int end = fullArgs.indexOf(" ", start);
            if (end == -1) end = fullArgs.length();
            soundOption = fullArgs.substring(start, end).toLowerCase();
        }

        // Extraer Comando
        if (fullArgs.contains("comando=\"")) {
            int start = fullArgs.indexOf("comando=\"") + 9;
            int end = fullArgs.indexOf("\"", start);
            if (end != -1) {
                actionCommand = fullArgs.substring(start, end);
            }
        }

        if (timeString.isEmpty() || !timeString.matches("\\d{2}:\\d{2}:\\d{2}")) {
            sender.sendMessage(ChatColor.RED + "Debes especificar un tiempo válido. Ej: tiempo=00:05:00");
            return true;
        }

        int totalSeconds = parseTimeToSeconds(timeString);
        if (totalSeconds <= 0) {
            sender.sendMessage(ChatColor.RED + "El tiempo debe ser mayor que 0.");
            return true;
        }

        if (!soundOption.equals("on") && !soundOption.equals("off")) {
            soundOption = "on";
        }

        if (target.equalsIgnoreCase("@a") || target.equals("all")) {
            createBossBar(name, totalSeconds, timeString, soundOption, actionCommand);
            sender.sendMessage(ChatColor.GREEN + "Timer Global creado: " + ChatColor.RESET + name);
        } else {
            Player player = Bukkit.getPlayer(target);
            if (player != null) {
                createPlayerBossBar(player, name, totalSeconds, timeString, soundOption, null, actionCommand);
                sender.sendMessage(ChatColor.GREEN + "Timer creado para " + player.getName() + ": " + ChatColor.RESET + name);
            } else {
                sender.sendMessage(ChatColor.RED + "Jugador no encontrado.");
            }
        }
        return true;
    }

    // --- SUBCOMANDO: EDITAR ---
    private boolean handleEditTiempo(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Uso: /timers editar <\"Nombre Actual\"> [nombre=\"Nuevo\"] [tiempo=hh:mm:ss] [sonido=on/off] [comando=\"nuevo comando\"]");
            return true;
        }

        String fullArgs = String.join(" ", args);
        String targetName = "";

        if (fullArgs.startsWith("\"")) {
            int endQuote = fullArgs.indexOf("\"", 1);
            if (endQuote != -1) {
                targetName = fullArgs.substring(1, endQuote);
                fullArgs = fullArgs.substring(endQuote + 1).trim();
            }
        } else {
            sender.sendMessage(ChatColor.RED + "El nombre del timer a editar debe estar entre comillas. Ej: \"Mi Timer\"");
            return true;
        }

        String barId = getBossBarIdByName(targetName);
        if (barId == null) {
            sender.sendMessage(ChatColor.RED + "No se encontró un timer activo con ese nombre.");
            return true;
        }

        boolean edited = false;

        if (fullArgs.contains("nombre=\"")) {
            int start = fullArgs.indexOf("nombre=\"") + 8;
            int end = fullArgs.indexOf("\"", start);
            if (end != -1) {
                String newName = ChatColor.translateAlternateColorCodes('&', fullArgs.substring(start, end));
                updateBossBarDisplayName(barId, newName);
                sender.sendMessage(ChatColor.GREEN + "Nombre actualizado a: " + ChatColor.RESET + newName);
                edited = true;
            }
        }

        if (fullArgs.contains("tiempo=")) {
            int start = fullArgs.indexOf("tiempo=") + 7;
            int end = fullArgs.indexOf(" ", start);
            if (end == -1) end = fullArgs.length();
            String newTime = fullArgs.substring(start, end);

            if (newTime.matches("\\d{2}:\\d{2}:\\d{2}")) {
                int newSeconds = parseTimeToSeconds(newTime);
                timers.put(barId, newSeconds);
                initialTimers.put(barId, newSeconds);
                sender.sendMessage(ChatColor.GREEN + "Tiempo actualizado a: " + newTime);
                edited = true;
            } else {
                sender.sendMessage(ChatColor.RED + "Formato de tiempo inválido. Ignorado.");
            }
        }

        if (fullArgs.contains("sonido=")) {
            int start = fullArgs.indexOf("sonido=") + 7;
            int end = fullArgs.indexOf(" ", start);
            if (end == -1) end = fullArgs.length();
            String newSound = fullArgs.substring(start, end).toLowerCase();

            if (newSound.equals("on") || newSound.equals("off")) {
                soundSettings.put(barId, newSound.equals("on"));
                sender.sendMessage(ChatColor.GREEN + "Sonido actualizado a: " + newSound.toUpperCase());
                edited = true;
            }
        }

        if (fullArgs.contains("comando=\"")) {
            int start = fullArgs.indexOf("comando=\"") + 9;
            int end = fullArgs.indexOf("\"", start);
            if (end != -1) {
                String newCmd = fullArgs.substring(start, end);
                if (newCmd.equalsIgnoreCase("none")) {
                    timerActions.remove(barId);
                    sender.sendMessage(ChatColor.GREEN + "Acción/Comando eliminado.");
                } else {
                    timerActions.put(barId, newCmd);
                    sender.sendMessage(ChatColor.GREEN + "Acción actualizada a: /" + newCmd);
                }
                edited = true;
            }
        }

        if (!edited) {
            sender.sendMessage(ChatColor.YELLOW + "No se especificó ninguna propiedad válida para editar.");
        }
        return true;
    }

    // --- SUBCOMANDO: REMOVER ---
    private boolean handleRemoveTiempo(CommandSender sender, String[] args) {
        if (args.length == 0) {
            boolean removed = removeFirstUnnamedBossBar();
            if (removed) {
                sender.sendMessage(ChatColor.GREEN + "Se eliminó la primera BossBar sin nombre.");
            } else {
                sender.sendMessage(ChatColor.RED + "No hay BossBars activas para eliminar.");
            }
            return true;
        }

        String inputName = String.join(" ", args).replace("\"", "").replace("&", "");

        String barId = getBossBarIdByName(inputName);
        if (barId != null) {
            String realName = displayNames.get(barId);
            removeBossBarInternal(barId);
            sender.sendMessage(ChatColor.GREEN + "Se eliminó el temporizador: " + ChatColor.RESET + realName);
            return true;
        }

        sender.sendMessage(ChatColor.RED + "No se encontró una BossBar con ese nombre.");
        return true;
    }

    // --- SUBCOMANDO: LISTA ---
    private boolean handleTiempoView(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(ChatColor.RED + "Uso: /timers lista <jugador/all>");
            return true;
        }

        String target = args[0];

        if (target.equalsIgnoreCase("all")) {
            if (bossBars.isEmpty()) {
                sender.sendMessage(ChatColor.YELLOW + "No hay BossBars de tiempo activas.");
                return true;
            }

            sender.sendMessage(ChatColor.GOLD + "=== Todas las BossBars de Tiempo ===");
            for (String barId : bossBars.keySet()) {
                printTimerInfo(sender, barId);
            }
        } else {
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
                printTimerInfo(sender, barId);
            }
        }
        return true;
    }

    private void printTimerInfo(CommandSender sender, String barId) {
        int timeLeft = timers.getOrDefault(barId, 0);
        boolean soundOn = soundSettings.getOrDefault(barId, true);
        String displayName = displayNames.getOrDefault(barId, barId);
        String action = timerActions.getOrDefault(barId, "Ninguna");

        String owner = "Global";
        if (barId.contains("_")) {
            try {
                UUID playerId = UUID.fromString(barId.split("_")[0]);
                Player player = Bukkit.getPlayer(playerId);
                owner = player != null ? player.getName() : "Desconectado";
            } catch (Exception ignored) {}
        }

        sender.sendMessage(ChatColor.GREEN + "Dueño: " + ChatColor.WHITE + owner);
        sender.sendMessage(ChatColor.GREEN + "Nombre: " + ChatColor.WHITE + displayName);
        sender.sendMessage(ChatColor.GREEN + "Tiempo restante: " + ChatColor.WHITE + formatTime(timeLeft));
        sender.sendMessage(ChatColor.GREEN + "Sonido: " + (soundOn ? ChatColor.GREEN + "ON" : ChatColor.RED + "OFF"));
        sender.sendMessage(ChatColor.GREEN + "Acción: " + ChatColor.WHITE + action);
        sender.sendMessage("");
    }

    // ========================================================================
    // --- LÓGICA INTERNA DE TIMERS Y MÉTODOS DE COMPATIBILIDAD EXTERNA ---
    // ========================================================================

    public void createBossBar(String name, int totalSeconds, String timeString, String soundOption) {
        createBossBar(name, totalSeconds, timeString, soundOption, null);
    }

    public void createBossBar(String name, int totalSeconds, String timeString, String soundOption, String action) {
        if (bossBars.containsKey(name)) removeBossBarInternal(name);

        BossBar bossBar = Bukkit.createBossBar(name, BarColor.WHITE, BarStyle.SOLID);
        bossBar.setVisible(true);
        Bukkit.getOnlinePlayers().forEach(bossBar::addPlayer);

        bossBars.put(name, bossBar);
        timers.put(name, totalSeconds);
        initialTimers.put(name, totalSeconds);
        soundSettings.put(name, soundOption.equals("on"));
        displayNames.put(name, name);
        if (action != null && !action.isEmpty()) timerActions.put(name, action);

        startTimer(name, bossBar);
    }

    public void createPlayerBossBar(Player player, String name, int totalSeconds, String timeString, String soundOption) {
        createPlayerBossBar(player, name, totalSeconds, timeString, soundOption, null, null);
    }

    public void createPlayerBossBar(Player player, String name, int totalSeconds, String timeString, String soundOption, String customId) {
        createPlayerBossBar(player, name, totalSeconds, timeString, soundOption, customId, null);
    }

    public void createPlayerBossBar(Player player, String name, int totalSeconds, String timeString, String soundOption, String customId, String action) {
        String barId = customId != null ? customId : player.getUniqueId() + "_" + name;
        if (bossBars.containsKey(barId)) removeBossBarInternal(barId);

        BossBar bossBar = Bukkit.createBossBar(name, BarColor.WHITE, BarStyle.SOLID);
        bossBar.setVisible(true);
        bossBar.addPlayer(player);

        bossBars.put(barId, bossBar);
        timers.put(barId, totalSeconds);
        initialTimers.put(barId, totalSeconds);
        soundSettings.put(barId, soundOption.equals("on"));
        displayNames.put(barId, name);
        if (action != null && !action.isEmpty()) timerActions.put(barId, action);

        playerBossBars.computeIfAbsent(player.getUniqueId(), k -> new HashSet<>()).add(barId);
        startTimer(barId, bossBar);
    }

    private void startTimer(String barId, BossBar bossBar) {
        int taskId = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            if (!timers.containsKey(barId)) return;

            int timeLeft = timers.get(barId);

            if (timeLeft <= 0) {
                bossBar.setTitle(ChatColor.RED + "Tiempo Terminado!");
                bossBar.setProgress(0.0);

                if (timeLeft == 0) {
                    if (timerActions.containsKey(barId)) {
                        String cmdToRun = timerActions.get(barId);
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmdToRun);
                    }
                    Bukkit.getScheduler().runTaskLater(plugin, () -> removeBossBarInternal(barId), 50L);
                }

                if (barId.contains("_")) {
                    try {
                        UUID playerId = UUID.fromString(barId.split("_")[0]);
                        Player p = Bukkit.getPlayer(playerId);
                        if (p != null) p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_DIDGERIDOO, 1.0f, 0.1f);
                    } catch (Exception ignored) {}
                } else {
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        if (bossBar.getPlayers().contains(p)) {
                            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_DIDGERIDOO, 1.0f, 0.1f);
                        }
                    }
                }

                // Restamos para que en el próximo tick caiga en -1 y -2
                timers.put(barId, timeLeft - 1);
                return;
            }

            timeLeft--;
            timers.put(barId, timeLeft);

            String timeStringFormatted = formatTime(timeLeft);
            String displayName = displayNames.getOrDefault(barId, barId);

            if (!displayName.startsWith("sinNombre")) {
                bossBar.setTitle(displayName + " " + ChatColor.WHITE + timeStringFormatted);
            } else {
                bossBar.setTitle(timeStringFormatted);
            }

            int initialTime = initialTimers.getOrDefault(barId, 1);
            double progress = (double) timeLeft / initialTime;
            bossBar.setProgress(Math.max(0.0, Math.min(1.0, progress)));

            if (soundSettings.getOrDefault(barId, true)) {
                if (barId.contains("_")) {
                    try {
                        UUID playerId = UUID.fromString(barId.split("_")[0]);
                        Player p = Bukkit.getPlayer(playerId);
                        if (p != null) p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1f, 1.5f);
                    } catch (Exception ignored) {}
                } else {
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        if (bossBar.getPlayers().contains(p)) {
                            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1f, 1.5f);
                        }
                    }
                }
            }
        }, 0L, 20L);

        taskIds.put(barId, taskId);
    }

    private String getBossBarIdByName(String searchName) {
        String cleanSearch = ChatColor.stripColor(searchName).toLowerCase();

        for (Map.Entry<String, String> entry : displayNames.entrySet()) {
            String cleanDisplay = ChatColor.stripColor(entry.getValue()).toLowerCase();
            if (cleanDisplay.equals(cleanSearch)) return entry.getKey();
        }
        for (Map.Entry<String, String> entry : displayNames.entrySet()) {
            String cleanDisplay = ChatColor.stripColor(entry.getValue()).toLowerCase();
            if (cleanDisplay.contains(cleanSearch)) return entry.getKey();
        }
        return null;
    }

    public boolean removeBossBar(String name) {
        if (bossBars.containsKey(name)) {
            removeBossBarInternal(name);
            return true;
        }

        String barId = getBossBarIdByName(name);
        if (barId != null) {
            removeBossBarInternal(barId);
            return true;
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
            if (key.contains("_")) {
                try {
                    UUID playerId = UUID.fromString(key.split("_")[0]);
                    playerBossBars.computeIfPresent(playerId, (uuid, bars) -> {
                        bars.remove(key);
                        return bars.isEmpty() ? null : bars;
                    });
                } catch (Exception ignored) {}
            }
        }

        bossBars.remove(key);
        timers.remove(key);
        initialTimers.remove(key);
        soundSettings.remove(key);
        displayNames.remove(key);
        timerActions.remove(key);
    }

    private boolean removeFirstUnnamedBossBar() {
        for (String key : new ArrayList<>(bossBars.keySet())) {
            if (key.startsWith("sinNombre")) {
                removeBossBarInternal(key);
                return true;
            }
        }
        return false;
    }

    public void updateBossBarDisplayName(String id, String newDisplayName) {
        if (displayNames.containsKey(id)) {
            displayNames.put(id, newDisplayName);
        }
    }

    public List<BossBar> getAllBossBars() {
        return new ArrayList<>(bossBars.values());
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

    // --- TAB COMPLETER INTELIGENTE ---
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> completions = new ArrayList<>();
        if (!label.equalsIgnoreCase("timers")) return completions;

        if (args.length == 1) {
            completions.addAll(Arrays.asList("crear", "remover", "lista", "editar"));
            return StringUtil.copyPartialMatches(args[0], completions, new ArrayList<>());
        }

        String subCommand = args[0].toLowerCase();

        if (subCommand.equals("crear")) {
            if (args.length == 2) {
                completions.add("@a");
                for (Player p : Bukkit.getOnlinePlayers()) completions.add(p.getName());
            } else if (args.length > 2) {
                String lastArg = args[args.length - 1];
                if (!lastArg.contains("=")) { // Sólo sugiere si no está escribiendo el valor actualmente
                    String fullStr = String.join(" ", args).toLowerCase();
                    List<String> options = new ArrayList<>(Arrays.asList("nombre=\"\"", "tiempo=00:00:00", "sonido=on/off", "comando=\"\""));

                    // Filtrar opciones que ya fueron usadas
                    if (fullStr.contains("nombre=")) options.remove("nombre=\"\"");
                    if (fullStr.contains("tiempo=")) options.remove("tiempo=00:00:00");
                    if (fullStr.contains("sonido=")) options.remove("sonido=on/off");
                    if (fullStr.contains("comando=")) options.remove("comando=\"\"");

                    completions.addAll(options);
                }
            }
        }
        else if (subCommand.equals("editar")) {
            if (args.length == 2) {
                for (String name : displayNames.values()) {
                    String clean = ChatColor.stripColor(name);
                    completions.add("\"" + clean + "\"");
                }
            } else if (args.length > 2) {
                String lastArg = args[args.length - 1];
                if (!lastArg.contains("=")) {
                    String fullStr = String.join(" ", args).toLowerCase();
                    List<String> options = new ArrayList<>(Arrays.asList("nombre=\"\"", "tiempo=00:00:00", "sonido=on/off", "comando=\"\""));

                    // Filtrar opciones usadas en editar
                    if (fullStr.contains("nombre=")) options.remove("nombre=\"\"");
                    if (fullStr.contains("tiempo=")) options.remove("tiempo=00:00:00");
                    if (fullStr.contains("sonido=")) options.remove("sonido=on/off");
                    if (fullStr.contains("comando=")) options.remove("comando=\"\"");

                    completions.addAll(options);
                }
            }
        }
        else if (subCommand.equals("remover")) {
            if (args.length == 2) {
                for (String name : displayNames.values()) {
                    String clean = ChatColor.stripColor(name);
                    completions.add("\"" + clean + "\"");
                }
            }
        }
        else if (subCommand.equals("lista")) {
            if (args.length == 2) {
                completions.add("all");
                for (Player p : Bukkit.getOnlinePlayers()) completions.add(p.getName());
            }
        }

        if (!completions.isEmpty() && !completions.get(0).startsWith("\"")) {
            return StringUtil.copyPartialMatches(args[args.length - 1], completions, new ArrayList<>());
        }

        return completions;
    }
}