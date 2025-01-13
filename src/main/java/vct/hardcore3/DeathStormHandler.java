package vct.hardcore3;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import org.bukkit.event.player.PlayerBedEnterEvent;

public class DeathStormHandler implements Listener {
    private final JavaPlugin plugin;
    private final DayHandler dayHandler;
    private final Map<UUID, Integer> deathCount = new HashMap<>();
    private int remainingStormSeconds = 0;
    private boolean isDeathStormActive = false;
    private static boolean isDeathMessageActive = false;
    private final Random random = new Random();

    private static final net.md_5.bungee.api.ChatColor COLOR_PRIMARIO = net.md_5.bungee.api.ChatColor.of("#9179D4");
    private static final net.md_5.bungee.api.ChatColor COLOR_TIEMPO = net.md_5.bungee.api.ChatColor.of("#7700A0");
    private static final net.md_5.bungee.api.ChatColor COLOR_GRIS = net.md_5.bungee.api.ChatColor.GRAY;

    public DeathStormHandler(JavaPlugin plugin, DayHandler dayHandler) {
        this.plugin = plugin;
        this.dayHandler = dayHandler;
        loadStormData();
    }

    public static void setDeathMessageActive(boolean isActive) {
        isDeathMessageActive = isActive;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        UUID playerUUID = player.getUniqueId();
        deathCount.put(playerUUID, deathCount.getOrDefault(playerUUID, 0) + 1);

        int currentDay = dayHandler.getCurrentDay();
        int increment;

        // Calcular el incremento según el día actual
        if (currentDay >= 15 && currentDay < 20) {
            increment = (currentDay - 14);
        } else if (currentDay >= 20) {
            increment = (currentDay - 19);
        } else {
            increment = currentDay;
        }

        remainingStormSeconds += 3600 * increment;
        isDeathStormActive = true;
        isDeathMessageActive = true;

        startStorm();
        saveStormData();

        new BukkitRunnable() {
            @Override
            public void run() {
                isDeathMessageActive = false;
            }
        }.runTaskLater(plugin, 25 * 20);
    }

    @EventHandler
    public void onWeatherChange(WeatherChangeEvent event) {
        if (!event.toWeatherState()) {
            if (isDeathStormActive) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPlayerBedEnter(PlayerBedEnterEvent event) {
        if (isDeathStormActive) {
            event.getPlayer().sendMessage(ChatColor.GRAY + "No puedes dormir durante una DeathStorm.");
            event.setCancelled(true);
        }
    }

    private void startStorm() {
        World world = Bukkit.getWorlds().get(0);

        if (stormTask != null && !stormTask.isCancelled()) {
            stormTask.cancel();
        }

        world.setStorm(true);
        world.setThundering(true);

        stormTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (remainingStormSeconds <= 0) {
                    cancel();
                    world.setStorm(false);
                    world.setThundering(false);
                    isDeathStormActive = false;
                    return;
                }

                if (isDeathMessageActive) {
                    return;
                }

                int hours = remainingStormSeconds / 3600;
                int minutes = (remainingStormSeconds % 3600) / 60;
                int seconds = remainingStormSeconds % 60;

                TextComponent message = new TextComponent();
                TextComponent quedanText = new TextComponent("Quedan ");
                quedanText.setColor(COLOR_PRIMARIO);
                message.addExtra(quedanText);

                TextComponent clockSymbol = new TextComponent("⌚ ");
                clockSymbol.setColor(COLOR_GRIS);
                message.addExtra(clockSymbol);
                TextComponent timeComponent = new TextComponent(
                        String.format("%02d:%02d:%02d", hours, minutes, seconds)
                );
                timeComponent.setColor(COLOR_TIEMPO);
                timeComponent.setBold(true);
                timeComponent.setUnderlined(true);
                message.addExtra(timeComponent);
                TextComponent stormMessage = new TextComponent(" horas de DeathStorm");
                stormMessage.setColor(COLOR_PRIMARIO);
                message.addExtra(stormMessage);

                for (Player player : Bukkit.getOnlinePlayers()) {
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR, message);
                }

                remainingStormSeconds--;
                spawnRandomLightning(world);
            }
        };

        stormTask.runTaskTimer(plugin, 0, 20);
    }

    private BukkitRunnable stormTask;
    private void spawnRandomLightning(World world) {
        int currentDay = dayHandler.getCurrentDay();

        // Configurar intervalos y número de rayos según el día
        int minStrikes, maxStrikes;
        long intervalTicks;
        if (currentDay >= 20) {
            minStrikes = 6;
            maxStrikes = 15;
            intervalTicks = 20L;
        } else if (currentDay >= 15) {
            minStrikes = 4;
            maxStrikes = 7;
            intervalTicks = 140L;
        } else {
            minStrikes = 1;
            maxStrikes = 1;
            intervalTicks = 2500L;
        }

        // Obtener la cantidad de rayos a generar
        int lightningCount = random.nextInt(maxStrikes - minStrikes + 1) + minStrikes;

        // Mapear chunks con jugadores
        Map<Location, Integer> chunkPlayerMap = new HashMap<>();
        for (Player player : world.getPlayers()) {
            Location chunkCenter = player.getLocation().getChunk().getBlock(8, 0, 8).getLocation();
            chunkPlayerMap.put(chunkCenter, chunkPlayerMap.getOrDefault(chunkCenter, 0) + 1);
        }

        // Filtrar los chunks con más de un jugador si es antes del día 30
        if (currentDay < 20) {
            chunkPlayerMap.entrySet().removeIf(entry -> entry.getValue() > 1);
        }

        new BukkitRunnable() {
            int strikesRemaining = lightningCount;

            @Override
            public void run() {
                if (strikesRemaining <= 0 || !isDeathStormActive) {
                    cancel();
                    return;
                }

                for (Location chunkCenter : chunkPlayerMap.keySet()) {
                    // Limitar a un rayo por área de 6x6 chunks
                    spawnLightning(world, chunkCenter, currentDay >= 20);
                    break;
                }

                strikesRemaining--;
            }
        }.runTaskTimer(plugin, 4, intervalTicks);
    }

    private void spawnLightning(World world, Location location, boolean afterDay20) {
        // Tamaño del área en chunks (6x6) convertido a bloques (96x96)
        int chunkRange = 13 * 16;
        Location lightningLocation;
        int maxAttempts = 100;
        int attempts = 0;

        do {
            // Generar un desplazamiento aleatorio dentro del área de 6x6 chunks
            double offsetX = (random.nextDouble() - 0.5) * chunkRange;
            double offsetZ = (random.nextDouble() - 0.5) * chunkRange;

            lightningLocation = location.clone().add(offsetX, 0, offsetZ);
            lightningLocation.setY(world.getHighestBlockYAt(lightningLocation));

            // Verificar si el chunk contiene jugadores
            Chunk targetChunk = lightningLocation.getChunk();
            boolean chunkHasPlayers = world.getPlayers().stream()
                    .anyMatch(player -> player.getLocation().getChunk().equals(targetChunk));

            // Si no es después del día 20, evitamos los chunks con jugadores
            if (!afterDay20 && chunkHasPlayers) {
                attempts++;
            } else {
                break;
            }
        } while (attempts < maxAttempts);

        if (attempts >= maxAttempts) {
            lightningLocation = location;
        }

        if (afterDay20 || dayHandler.getCurrentDay() >= 10) {
            world.strikeLightning(lightningLocation);
        } else {
            world.strikeLightningEffect(lightningLocation);
        }

        // Obtener el bloque impactado
        Block block = world.getBlockAt(lightningLocation);
        int currentDay = dayHandler.getCurrentDay();

        if (afterDay20 || currentDay >= 15) {
            if (block.getType() != Material.OBSIDIAN && block.getType() != Material.BEDROCK) {
                block.setType(Material.FIRE);
            }
        } else {
            block.setType(block.getType());
        }
    }

    public void resetStorm() {
        remainingStormSeconds = 0;
        isDeathStormActive = false;
        saveStormData();
    }

    public void addStormHours(int hours) {
        remainingStormSeconds += hours * 3600;
        isDeathStormActive = true;
        saveStormData();
    }

    public void removeStormHours(int hours) {
        remainingStormSeconds = Math.max(remainingStormSeconds - (hours * 3600), 0);
        if (remainingStormSeconds == 0) {
            isDeathStormActive = false;
        }
        saveStormData();
    }

    public void loadStormData() {
        try {
            Path path = Paths.get(plugin.getDataFolder().getAbsolutePath(), "stormdata.txt");
            if (Files.exists(path)) {
                remainingStormSeconds = Integer.parseInt(new String(Files.readAllBytes(path)).trim());
                Bukkit.getLogger().info("Storm data loaded: " + remainingStormSeconds + " seconds remaining.");
                if (remainingStormSeconds > 0) {
                    isDeathStormActive = true;
                    startStorm();
                }
            }
        } catch (IOException e) {
            Bukkit.getLogger().severe("Error loading storm data: " + e.getMessage());
        } catch (NumberFormatException e) {
            Bukkit.getLogger().severe("Invalid number format in stormdata.txt: " + e.getMessage());
        }
    }

    public void saveStormData() {
        try {
            Path path = Paths.get(plugin.getDataFolder().getAbsolutePath(), "stormdata.txt");
            Files.createDirectories(path.getParent());
            Files.write(path, String.valueOf(remainingStormSeconds).getBytes());
            Bukkit.getLogger().info("Storm data saved: " + remainingStormSeconds + " seconds remaining.");
        } catch (IOException e) {
            Bukkit.getLogger().severe("Error saving storm. " + e.getMessage());
        }
    }
}