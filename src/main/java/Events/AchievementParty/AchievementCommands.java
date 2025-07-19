package Events.AchievementParty;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class AchievementCommands implements CommandExecutor, org.bukkit.command.TabCompleter {
    private final AchievementPartyHandler achievementHandler;

    public AchievementCommands(AchievementPartyHandler achievementHandler) {
        this.achievementHandler = achievementHandler;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("achievements.admin")) {
            sender.sendMessage(ChatColor.RED + "No tienes permiso para usar este comando.");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Uso: /" + label + " <nombrelogro> <jugador> [item_lista|all]");
            sender.sendMessage(ChatColor.GOLD + "Logros disponibles:");
            achievementHandler.getAchievements().forEach((id, achievement) -> {
                sender.sendMessage(ChatColor.YELLOW + "- " + id + ChatColor.GRAY + ": " +
                        ChatColor.WHITE + achievement.getName());
            });
            return true;
        }

        String achievementId = args[0];
        String playerName = args[1];
        Player target = Bukkit.getPlayer(playerName);

        // Verificar si el jugador existe (online u offline)
        if (target == null && Bukkit.getOfflinePlayer(playerName).getName() == null) {
            sender.sendMessage(ChatColor.RED + "El jugador " + playerName + " no existe.");
            return true;
        }

        // Usar el nombre correcto (por si el jugador está offline)
        String actualPlayerName = target != null ? target.getName() : Bukkit.getOfflinePlayer(playerName).getName();

        if (command.getName().equalsIgnoreCase("addlogro")) {
            return handleAddAchievement(sender, achievementId, actualPlayerName, args.length > 2 ? args[2] : null);
        } else if (command.getName().equalsIgnoreCase("removelogro")) {
            return handleRemoveAchievement(sender, achievementId, actualPlayerName, args.length > 2 ? args[2] : null);
        }

        return false;
    }

    private boolean handleAddAchievement(CommandSender sender, String achievementId, String playerName, String itemArg) {
        if (!achievementHandler.isEventActive()) {
            sender.sendMessage("§cNo hay ningún evento de logros activo!");
            return true;
        }

        if (!achievementHandler.getAchievementIds().contains(achievementId)) {
            sender.sendMessage(ChatColor.RED + "Logro no válido. Logros disponibles:");
            sender.sendMessage(String.join(", ", achievementHandler.getAchievementIds()));
            return true;
        }

        Achievement achievement = achievementHandler.getAchievements().get(achievementId);

        // Manejar logros con listas (como el de las flores)
        if (achievement instanceof Achievement2 && itemArg != null) {
            Achievement2 flowerAchievement = (Achievement2) achievement;
            FileConfiguration data = YamlConfiguration.loadConfiguration(achievementHandler.getAchievementsFile());

            if (itemArg.equalsIgnoreCase("all")) {
                // Marcar todas las flores como recolectadas
                for (Material flower : flowerAchievement.getRequiredFlowers()) {
                    data.set("players." + playerName + ".achievements.collect_all_flowers.collected." + flower.name(), true);
                }

                // Verificar si ya estaba completo antes de marcar todo
                boolean wasCompleted = data.getBoolean("players." + playerName + ".achievements.collect_all_flowers.completed", false);

                try {
                    data.save(achievementHandler.getAchievementsFile());
                    sender.sendMessage(ChatColor.GREEN + "Todas las flores del logro han sido marcadas como recolectadas para " + playerName);

                    // Completar el logro si no estaba completado antes
                    if (!wasCompleted) {
                        achievementHandler.completeAchievement(playerName, achievementId);
                    }
                    return true;
                } catch (IOException e) {
                    sender.sendMessage(ChatColor.RED + "Error al guardar los cambios: " + e.getMessage());
                    return false;
                }
            } else {
                // Intentar encontrar la flor especificada
                Material flowerMaterial = null;
                try {
                    flowerMaterial = Material.valueOf(itemArg.toUpperCase());
                } catch (IllegalArgumentException e) {
                    // Material no válido
                }

                if (flowerMaterial == null || !flowerAchievement.getRequiredFlowers().contains(flowerMaterial)) {
                    sender.sendMessage(ChatColor.RED + "Flor no válida. Flores disponibles:");
                    sender.sendMessage(flowerAchievement.getRequiredFlowers().stream()
                            .map(Enum::name)
                            .collect(Collectors.joining(", ")));
                    sender.sendMessage(ChatColor.YELLOW + "O usa 'all' para marcar todas las flores");
                    return true;
                }

                // Marcar la flor específica como recolectada
                data.set("players." + playerName + ".achievements.collect_all_flowers.collected." + flowerMaterial.name(), true);

                try {
                    data.save(achievementHandler.getAchievementsFile());
                    sender.sendMessage(ChatColor.GREEN + "Flor " + flowerMaterial.name() + " marcada como recolectada para " + playerName);

                    // Verificar si ahora tiene todas las flores
                    boolean allCollected = true;
                    for (Material flower : flowerAchievement.getRequiredFlowers()) {
                        if (!data.getBoolean("players." + playerName + ".achievements.collect_all_flowers.collected." + flower.name(), false)) {
                            allCollected = false;
                            break;
                        }
                    }

                    if (allCollected && !data.getBoolean("players." + playerName + ".achievements.collect_all_flowers.completed", false)) {
                        achievementHandler.completeAchievement(playerName, achievementId);
                    }
                    return true;
                } catch (IOException e) {
                    sender.sendMessage(ChatColor.RED + "Error al guardar los cambios: " + e.getMessage());
                    return false;
                }
            }
        }

        // Manejar logros con listas (achievement5)
        if (achievement instanceof Achievement5 && itemArg != null) {
            Achievement5 blockAchievement = (Achievement5) achievement;
            FileConfiguration data = YamlConfiguration.loadConfiguration(achievementHandler.getAchievementsFile());

            if (itemArg.equalsIgnoreCase("all")) {
                // Marcar todos los bloques como rotos
                for (Material block : blockAchievement.getRequiredBlocks()) {
                    data.set("players." + playerName + ".achievements.touch_grass.broken." + block.name(), true);
                }

                // Verificar si ya estaba completo antes de marcar todo
                boolean wasCompleted = data.getBoolean("players." + playerName + ".achievements.touch_grass.completed", false);

                try {
                    data.save(achievementHandler.getAchievementsFile());
                    sender.sendMessage(ChatColor.GREEN + "Todos los bloques del logro han sido marcados como rotos para " + playerName);

                    // Completar el logro si no estaba completado antes
                    if (!wasCompleted) {
                        achievementHandler.completeAchievement(playerName, achievementId);
                    }
                    return true;
                } catch (IOException e) {
                    sender.sendMessage(ChatColor.RED + "Error al guardar los cambios: " + e.getMessage());
                    return false;
                }
            } else {
                // Intentar encontrar el bloque especificado
                Material blockMaterial = null;
                try {
                    blockMaterial = Material.valueOf(itemArg.toUpperCase());
                } catch (IllegalArgumentException e) {
                    // Material no válido
                }

                if (blockMaterial == null || !blockAchievement.getRequiredBlocks().contains(blockMaterial)) {
                    sender.sendMessage(ChatColor.RED + "Bloque no válido. Bloques disponibles:");
                    sender.sendMessage(blockAchievement.getRequiredBlocks().stream()
                            .map(Enum::name)
                            .collect(Collectors.joining(", ")));
                    sender.sendMessage(ChatColor.YELLOW + "O usa 'all' para marcar todos los bloques");
                    return true;
                }

                // Marcar el bloque específico como roto
                data.set("players." + playerName + ".achievements.touch_grass.broken." + blockMaterial.name(), true);

                try {
                    data.save(achievementHandler.getAchievementsFile());
                    sender.sendMessage(ChatColor.GREEN + "Bloque " + blockMaterial.name() + " marcado como roto para " + playerName);

                    // Verificar si ahora tiene todos los bloques
                    boolean allBroken = true;
                    for (Material block : blockAchievement.getRequiredBlocks()) {
                        if (!data.getBoolean("players." + playerName + ".achievements.touch_grass.broken." + block.name(), false)) {
                            allBroken = false;
                            break;
                        }
                    }

                    if (allBroken && !data.getBoolean("players." + playerName + ".achievements.touch_grass.completed", false)) {
                        achievementHandler.completeAchievement(playerName, achievementId);
                    }
                    return true;
                } catch (IOException e) {
                    sender.sendMessage(ChatColor.RED + "Error al guardar los cambios: " + e.getMessage());
                    return false;
                }
            }
        }
        // En el método handleAddAchievement, añadir este caso:
        if (achievement instanceof Achievement8) {
            FileConfiguration data = YamlConfiguration.loadConfiguration(achievementHandler.getAchievementsFile());
            String path = "players." + playerName + ".achievements.sculk_shrieker.broken";

            if (itemArg != null && itemArg.equalsIgnoreCase("all")) {
                // Completar todo el progreso
                data.set(path, ((Achievement8) achievement).REQUIRED_SCULK_SHRIEKERS);

                try {
                    data.save(achievementHandler.getAchievementsFile());
                    sender.sendMessage(ChatColor.GREEN + "Progreso de chilladores completado para " + playerName);

                    // Completar el logro si no estaba completado
                    if (!data.getBoolean("players." + playerName + ".achievements.sculk_shrieker.completed", false)) {
                        achievementHandler.completeAchievement(playerName, achievementId);
                    }
                    return true;
                } catch (IOException e) {
                    sender.sendMessage(ChatColor.RED + "Error al guardar los cambios: " + e.getMessage());
                    return false;
                }
            } else if (itemArg != null && itemArg.equalsIgnoreCase("down")) {
                // Reducir el contador
                int current = data.getInt(path, 0);
                if (current > 0) {
                    data.set(path, current - 1);

                    try {
                        data.save(achievementHandler.getAchievementsFile());
                        sender.sendMessage(ChatColor.GREEN + "Contador de chilladores reducido a " + (current - 1) + " para " + playerName);
                        return true;
                    } catch (IOException e) {
                        sender.sendMessage(ChatColor.RED + "Error al guardar los cambios: " + e.getMessage());
                        return false;
                    }
                } else {
                    sender.sendMessage(ChatColor.RED + "El contador ya está en 0");
                    return true;
                }
            } else {
                // Incrementar el contador
                int current = data.getInt(path, 0);
                data.set(path, current + 1);

                try {
                    data.save(achievementHandler.getAchievementsFile());
                    sender.sendMessage(ChatColor.GREEN + "Contador de chilladores incrementado a " + (current + 1) + " para " + playerName);

                    // Verificar si ahora tiene todos los chilladores
                    if (current + 1 >= ((Achievement8) achievement).REQUIRED_SCULK_SHRIEKERS &&
                            !data.getBoolean("players." + playerName + ".achievements.sculk_shrieker.completed", false)) {
                        achievementHandler.completeAchievement(playerName, achievementId);
                    }
                    return true;
                } catch (IOException e) {
                    sender.sendMessage(ChatColor.RED + "Error al guardar los cambios: " + e.getMessage());
                    return false;
                }
            }
        }


        // Para logros normales (sin lista)
        if (itemArg != null) {
            sender.sendMessage(ChatColor.YELLOW + "Este logro no requiere items. Ignorando el parámetro adicional.");
        }
        // Reemplazar addAchievement por completeAchievement
        FileConfiguration data = YamlConfiguration.loadConfiguration(achievementHandler.getAchievementsFile());
        boolean wasCompleted = data.getBoolean("players." + playerName + ".achievements." + achievementId + ".completed", false);

        if (!wasCompleted) {
            achievementHandler.completeAchievement(playerName, achievementId);
            sender.sendMessage(ChatColor.GREEN + "Logro " + achievementId + " añadido a " + playerName);
            return true;
        } else {
            sender.sendMessage(ChatColor.RED + "El jugador ya tenía este logro completado.");
        }
        return true;
    }

    private boolean handleRemoveAchievement(CommandSender sender, String achievementId, String playerName, String itemArg) {
        if (!achievementHandler.isEventActive()) {
            sender.sendMessage("§cNo hay ningún evento de logros activo!");
            return true;
        }

        if (!achievementHandler.getAchievementIds().contains(achievementId)) {
            sender.sendMessage(ChatColor.RED + "Logro no válido. Logros disponibles:");
            sender.sendMessage(String.join(", ", achievementHandler.getAchievementIds()));
            return true;
        }

        Achievement achievement = achievementHandler.getAchievements().get(achievementId);

        // Manejar logros con listas (como el de las flores)
        if (achievement instanceof Achievement2 && itemArg != null) {
            Achievement2 flowerAchievement = (Achievement2) achievement;
            FileConfiguration data = YamlConfiguration.loadConfiguration(achievementHandler.getAchievementsFile());

            if (itemArg.equalsIgnoreCase("all")) {
                // Marcar todas las flores como no recolectadas
                for (Material flower : flowerAchievement.getRequiredFlowers()) {
                    data.set("players." + playerName + ".achievements.collect_all_flowers.collected." + flower.name(), false);
                }

                // Quitar el logro completado si lo tenía
                if (data.getBoolean("players." + playerName + ".achievements.collect_all_flowers.completed", false)) {
                    data.set("players." + playerName + ".achievements.collect_all_flowers.completed", false);
                    int completed = data.getInt("players." + playerName + ".completed", 0);
                    data.set("players." + playerName + ".completed", Math.max(0, completed - 1));
                }

                try {
                    data.save(achievementHandler.getAchievementsFile());
                    sender.sendMessage(ChatColor.GREEN + "Todas las flores del logro han sido removidas para " + playerName);
                    return true;
                } catch (IOException e) {
                    sender.sendMessage(ChatColor.RED + "Error al guardar los cambios: " + e.getMessage());
                    return false;
                }
            } else {
                // Intentar encontrar la flor especificada
                Material flowerMaterial = null;
                try {
                    flowerMaterial = Material.valueOf(itemArg.toUpperCase());
                } catch (IllegalArgumentException e) {
                    // Material no válido
                }

                if (flowerMaterial == null || !flowerAchievement.getRequiredFlowers().contains(flowerMaterial)) {
                    sender.sendMessage(ChatColor.RED + "Flor no válida. Flores disponibles:");
                    sender.sendMessage(flowerAchievement.getRequiredFlowers().stream()
                            .map(Enum::name)
                            .collect(Collectors.joining(", ")));
                    sender.sendMessage(ChatColor.YELLOW + "O usa 'all' para remover todas las flores");
                    return true;
                }

                // Marcar la flor específica como no recolectada
                data.set("players." + playerName + ".achievements.collect_all_flowers.collected." + flowerMaterial.name(), false);

                // Si el logro estaba completado, quitarlo
                if (data.getBoolean("players." + playerName + ".achievements.collect_all_flowers.completed", false)) {
                    data.set("players." + playerName + ".achievements.collect_all_flowers.completed", false);
                    int completed = data.getInt("players." + playerName + ".completed", 0);
                    data.set("players." + playerName + ".completed", Math.max(0, completed - 1));
                }

                try {
                    data.save(achievementHandler.getAchievementsFile());
                    sender.sendMessage(ChatColor.GREEN + "Flor " + flowerMaterial.name() + " removida para " + playerName);
                    return true;
                } catch (IOException e) {
                    sender.sendMessage(ChatColor.RED + "Error al guardar los cambios: " + e.getMessage());
                    return false;
                }
            }
        }

        // Manejar el remove logros con listas (achievement5)
        if (achievement instanceof Achievement5 && itemArg != null) {
            Achievement5 blockAchievement = (Achievement5) achievement;
            FileConfiguration data = YamlConfiguration.loadConfiguration(achievementHandler.getAchievementsFile());

            if (itemArg.equalsIgnoreCase("all")) {
                // Marcar todos los bloques como no rotos
                for (Material block : blockAchievement.getRequiredBlocks()) {
                    data.set("players." + playerName + ".achievements.touch_grass.broken." + block.name(), false);
                }

                // Quitar el logro completado si lo tenía
                if (data.getBoolean("players." + playerName + ".achievements.touch_grass.completed", false)) {
                    data.set("players." + playerName + ".achievements.touch_grass.completed", false);
                    int completed = data.getInt("players." + playerName + ".completed", 0);
                    data.set("players." + playerName + ".completed", Math.max(0, completed - 1));
                }

                try {
                    data.save(achievementHandler.getAchievementsFile());
                    sender.sendMessage(ChatColor.GREEN + "Todos los bloques del logro han sido removidos para " + playerName);
                    return true;
                } catch (IOException e) {
                    sender.sendMessage(ChatColor.RED + "Error al guardar los cambios: " + e.getMessage());
                    return false;
                }
            } else {
                // Intentar encontrar el bloque especificado
                Material blockMaterial = null;
                try {
                    blockMaterial = Material.valueOf(itemArg.toUpperCase());
                } catch (IllegalArgumentException e) {
                    // Material no válido
                }

                if (blockMaterial == null || !blockAchievement.getRequiredBlocks().contains(blockMaterial)) {
                    sender.sendMessage(ChatColor.RED + "Bloque no válido. Bloques disponibles:");
                    sender.sendMessage(blockAchievement.getRequiredBlocks().stream()
                            .map(Enum::name)
                            .collect(Collectors.joining(", ")));
                    sender.sendMessage(ChatColor.YELLOW + "O usa 'all' para remover todos los bloques");
                    return true;
                }

                // Marcar el bloque específico como no roto
                data.set("players." + playerName + ".achievements.touch_grass.broken." + blockMaterial.name(), false);

                // Si el logro estaba completado, quitarlo
                if (data.getBoolean("players." + playerName + ".achievements.touch_grass.completed", false)) {
                    data.set("players." + playerName + ".achievements.touch_grass.completed", false);
                    int completed = data.getInt("players." + playerName + ".completed", 0);
                    data.set("players." + playerName + ".completed", Math.max(0, completed - 1));
                }

                try {
                    data.save(achievementHandler.getAchievementsFile());
                    sender.sendMessage(ChatColor.GREEN + "Bloque " + blockMaterial.name() + " removido para " + playerName);
                    return true;
                } catch (IOException e) {
                    sender.sendMessage(ChatColor.RED + "Error al guardar los cambios: " + e.getMessage());
                    return false;
                }
            }
        }
        // Manejar el remove logros con listas (achievement8)
        if (achievement instanceof Achievement8) {
            FileConfiguration data = YamlConfiguration.loadConfiguration(achievementHandler.getAchievementsFile());
            String path = "players." + playerName + ".achievements.sculk_shrieker.broken";

            if (itemArg != null && itemArg.equalsIgnoreCase("all")) {
                // Resetear todo el progreso
                data.set(path, 0);

                // Quitar el logro completado si lo tenía
                if (data.getBoolean("players." + playerName + ".achievements.sculk_shrieker.completed", false)) {
                    data.set("players." + playerName + ".achievements.sculk_shrieker.completed", false);
                    int completed = data.getInt("players." + playerName + ".completed", 0);
                    data.set("players." + playerName + ".completed", Math.max(0, completed - 1));
                }

                try {
                    data.save(achievementHandler.getAchievementsFile());
                    sender.sendMessage(ChatColor.GREEN + "Progreso de chilladores reseteado para " + playerName);
                    return true;
                } catch (IOException e) {
                    sender.sendMessage(ChatColor.RED + "Error al guardar los cambios: " + e.getMessage());
                    return false;
                }
            } else {
                // Reducir el contador
                int current = data.getInt(path, 0);
                if (current > 0) {
                    data.set(path, current - 1);

                    // Si el logro estaba completado, quitarlo
                    if (current >= ((Achievement8) achievement).REQUIRED_SCULK_SHRIEKERS &&
                            data.getBoolean("players." + playerName + ".achievements.sculk_shrieker.completed", false)) {
                        data.set("players." + playerName + ".achievements.sculk_shrieker.completed", false);
                        int completed = data.getInt("players." + playerName + ".completed", 0);
                        data.set("players." + playerName + ".completed", Math.max(0, completed - 1));
                    }

                    try {
                        data.save(achievementHandler.getAchievementsFile());
                        sender.sendMessage(ChatColor.GREEN + "Contador de chilladores reducido a " + (current - 1) + " para " + playerName);
                        return true;
                    } catch (IOException e) {
                        sender.sendMessage(ChatColor.RED + "Error al guardar los cambios: " + e.getMessage());
                        return false;
                    }
                } else {
                    sender.sendMessage(ChatColor.RED + "El contador ya está en 0");
                    return true;
                }
            }
        }

        // Para logros normales (sin lista)
        if (itemArg != null) {
            sender.sendMessage(ChatColor.YELLOW + "Este logro no requiere items. Ignorando el parámetro adicional.");
        }

        if (achievementHandler.removeAchievement(playerName, achievementId)) {
            sender.sendMessage(ChatColor.GREEN + "Logro " + achievementId + " removido de " + playerName);

            // Notificar al jugador si está online
            Player target = Bukkit.getPlayer(playerName);
            if (target != null) {
                target.sendMessage(ChatColor.RED + "Un administrador te ha removido el logro: " +
                        achievementHandler.getAchievements().get(achievementId).getName() + "!");
            }
        } else {
            sender.sendMessage(ChatColor.RED + "El jugador no tenía este logro completado.");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("achievements.admin")) {
            return Collections.emptyList();
        }

        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // Autocompletar nombres de logros
            List<String> achievementIds = new ArrayList<>(achievementHandler.getAchievementIds());
            StringUtil.copyPartialMatches(args[0], achievementIds, completions);
        } else if (args.length == 2) {
            // Autocompletar nombres de jugadores (online + algunos offline recientes)
            List<String> playerNames = new ArrayList<>();

            // Jugadores online
            Bukkit.getOnlinePlayers().forEach(p -> playerNames.add(p.getName()));

            // Jugadores offline recientes (últimos 50 jugadores vistos)
            playerNames.addAll(
                    java.util.Arrays.stream(Bukkit.getServer().getOfflinePlayers())
                            .limit(50)
                            .map(OfflinePlayer::getName)
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList())
            );

            StringUtil.copyPartialMatches(args[1], playerNames, completions);
        } else if (args.length == 3) {
            // Autocompletar items para logros con listas
            Achievement achievement = achievementHandler.getAchievements().get(args[0]);

            if (achievement instanceof Achievement2) {
                Achievement2 flowerAchievement = (Achievement2) achievement;
                List<String> flowerNames = flowerAchievement.getRequiredFlowers().stream()
                        .map(Enum::name)
                        .map(String::toLowerCase)
                        .collect(Collectors.toList());
                flowerNames.add("all"); // Añadir la opción "all"

                StringUtil.copyPartialMatches(args[2], flowerNames, completions);
            }
            else if (achievement instanceof Achievement5) {
                Achievement5 blockAchievement = (Achievement5) achievement;
                List<String> blockNames = blockAchievement.getRequiredBlocks().stream()
                        .map(Enum::name)
                        .map(String::toLowerCase)
                        .collect(Collectors.toList());
                blockNames.add("all");

                StringUtil.copyPartialMatches(args[2], blockNames, completions);
            }
            else if (achievement instanceof Achievement8) {
                List<String> options = new ArrayList<>();
                options.add("one");
                options.add("all");
                StringUtil.copyPartialMatches(args[2], options, completions);
            }
        }

        // Ordenar alfabéticamente
        Collections.sort(completions);

        return completions.size() > 50 ? completions.subList(0, 50) : completions;
    }
}