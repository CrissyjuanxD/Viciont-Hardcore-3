package Events.Skybattle;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.*;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class EventoHandler implements Listener {
    private final Set<String> participantes = new HashSet<>();
    private List<Location> ubicacionesShroomlightOriginales = new ArrayList<>();
    private final List<BukkitTask> tareasActivas = new ArrayList<>();
    private boolean eventoActivo = false;
    private final int MAX_PARTICIPANTES = 20;
    private final JavaPlugin plugin;
    private final CofresHandler cofresHandler;

    private int tiempoBorde = 120;
    private int minX = 19904;
    private int maxX = 20096;
    private int minY = 27;
    private int maxY = 110;
    private int minZ = 19904;
    private int maxZ = 20096;
    private boolean bordeReduciendose = false;
    private final File estadoArchivo;


    public EventoHandler(JavaPlugin plugin) {
        this.plugin = plugin;
        this.cofresHandler = new CofresHandler(plugin);
        this.estadoArchivo = new File(plugin.getDataFolder(), "estado_evento.yml");
        verificarEstadoEvento();
    }

    public void iniciarEvento() {
        eventoActivo = true;
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "ruletavct [\"\",{\"text\":\"\\n\"},{\"text\":\"\\u06de Evento\",\"bold\":true,\"color\":\"#F977F9\"},{\"text\":\" \\u27a4\",\"bold\":true,\"color\":\"gray\"},{\"text\":\"\\n\\n\"},{\"text\":\"¡Ha comenzado el evento \",\"color\":\"#BA7FD0\"},{\"text\":\"LAVACLASH\",\"bold\":true,\"color\":\"#D98836\"},{\"text\":\"!\\nLos primeros \",\"color\":\"#BA7FD0\"},{\"text\":\"20\",\"bold\":true,\"color\":\"#BA7FD0\"},{\"text\":\" jugadores en obtener\\nun \",\"color\":\"#BA7FD0\"},{\"text\":\"Viciont Ticket\",\"bold\":true,\"color\":\"#E9BF66\"},{\"text\":\" participarán\\n\\nPara obtener el ticket deberan romper un \",\"color\":\"#BA7FD0\"},{\"text\":\"\\n\"},{\"text\":\"Bloque de Diamante!\",\"bold\":true,\"color\":\"#57A9CB\"},{\"text\":\"\\n \"}]");
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "gamerule sendCommandFeedback false");
        //test
        guardarEstadoEvento();
    }

    public void forzarEvento() {
        if (eventoActivo && participantes.size() < MAX_PARTICIPANTES) {
            String jsonMessage = "[\"\","
                    + "{\"text\":\"\\n\"},"
                    + "{\"text\":\"\\u06de Evento\",\"bold\":true,\"color\":\"#F977F9\"},"
                    + "{\"text\":\" \\u27a4\",\"bold\":true,\"color\":\"gray\"},"
                    + "{\"text\":\"\\n\\n\"},"
                    + "{\"text\":\"Forzando\",\"bold\":true,\"color\":\"#DB5EAF\"},"
                    + "{\"text\":\" el inicio del evento con \\nlos jugadores actuales!\",\"color\":\"#BA7FD0\"}"
                    + "]";

            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "tellraw @a " + jsonMessage);

            teletransportarJugadores();
        }
    }

    public void terminarEvento() {
        eventoActivo = false;
        participantes.clear();
        Bukkit.broadcastMessage("§c۞ El evento ha terminado.");
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "stopsound @a record minecraft:custom.music1_skybattle");
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "stopsound @a record minecraft:custom.music2_skybattle");
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "stopdisco");
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "tick rate 20");
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "effect clear @a minecraft:speed");
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "effect clear @a minecraft:night_vision");
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "gamerule sendCommandFeedback true");

        for (Player p : Bukkit.getOnlinePlayers()) {
            ScoreboardManager manager = Bukkit.getScoreboardManager();
            Scoreboard board = manager.getNewScoreboard();
            p.setScoreboard(board);
        }

        // Restaurar los shroomlights
        restaurarShroomlights();
        // Eliminar bloques de purple_concrete
        eliminarPurpleConcrete();
        // Restaurar contenido de los cofres al finalizar el evento
        restaurarContenidoCofres();
        // Reiniciar variables del borde
        reiniciarVariablesBorde();
        // Guardar estado del evento
        guardarEstadoEvento();
        // Cargar y restaurar cofres desde archivo (si existen)
        cargarCofresDesdeArchivo();
        restaurarContenidoCofres();
        cancelarTareasActivas();

        // Programar la eliminación del archivo después de 5 segundos (100 ticks)
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            File archivo = new File(plugin.getDataFolder(), "contenido_cofres.yml");
            if (archivo.exists()) {
                if (archivo.delete()) {
                    plugin.getLogger().info("Archivo de cofres eliminado correctamente.");
                } else {
                    plugin.getLogger().warning("No se pudo eliminar el archivo de cofres.");
                }
            }
        }, 140L);
    }

    private boolean isInsideZone(Location loc) {
        return loc.getBlockX() >= minX && loc.getBlockX() <= maxX
                && loc.getBlockY() >= minY && loc.getBlockY() <= maxY
                && loc.getBlockZ() >= minZ && loc.getBlockZ() <= maxZ;
    }

    private void reiniciarVariablesBorde() {
        minX = 19904;
        maxX = 20096;
        minZ = 19904;
        maxZ = 20096;
        tiempoBorde = 120;
        bordeReduciendose = false;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (!eventoActivo || participantes.size() >= MAX_PARTICIPANTES) return;

        Player jugador = event.getPlayer();
        if (participantes.contains(jugador.getName())) {
            return;
        }

        if (event.getBlock().getType() == Material.DIAMOND_BLOCK) {
            ItemStack ticket = new ItemStack(Material.ECHO_SHARD);
            ItemMeta meta = ticket.getItemMeta();
            meta.setDisplayName("§e§lViciont Ticket");
            meta.setCustomModelData(1);
            ticket.setItemMeta(meta);
            jugador.getInventory().addItem(ticket);
            participantes.add(jugador.getName());

            String jsonMessage = "[\"\","
                    + "{\"text\":\"\u06de\",\"color\":\"#BA7FD0\"},"
                    + "{\"text\":\" " + jugador.getName() + "\",\"bold\":true,\"color\":\"#863ECF\"},"
                    + "{\"text\":\" ha obtenido el \",\"color\":\"#BA7FD0\"},"
                    + "{\"text\":\"Viciont ticket\",\"bold\":true,\"color\":\"#E9BF66\"},"
                    + "{\"text\":\" - Ticket:\",\"color\":\"#BA7FD0\"},"
                    + "{\"text\":\" " + participantes.size() + "\",\"bold\":true,\"color\":\"#863ECF\"},"
                    + "{\"text\":\"/\",\"bold\":true,\"color\":\"#BA7FD0\"},"
                    + "{\"text\":\"" + MAX_PARTICIPANTES + "\",\"bold\":true,\"color\":\"#863ECF\"},"
                    + "{\"text\":\"\\n \"}"
                    + "]";

            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "tellraw @a " + jsonMessage);

            if (participantes.size() == MAX_PARTICIPANTES) {
                Bukkit.broadcastMessage("§e۞ ¡Ya no hay más tickets disponibles! Los jugadores serán teletransportados en 10 segundos.");
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        teletransportarJugadores();
                    }
                }.runTaskLater(plugin, 200L);
            }
        }
    }

    private void teletransportarJugadores() {
        List<Location> shroomlightLocations = obtenerShroomlightLocations();

        if (shroomlightLocations.size() < participantes.size()) {
            Bukkit.broadcastMessage("§c[Error] No hay suficientes ubicaciones disponibles para teletransportar a los jugadores.");
            return;
        }

        guardarContenidoCofres();
        eliminarMobsExistentes();

        Collections.shuffle(shroomlightLocations);

        int i = 0;
        for (String jugador : participantes) {
            Player p = Bukkit.getPlayer(jugador);
            if (p != null && i < shroomlightLocations.size()) {
                // Obtener la ubicación y convertirla a coordenadas
                Location loc = shroomlightLocations.get(i).add(0.5, 1, 0.5);
                double x = loc.getX();
                double y = loc.getY();
                double z = loc.getZ();
                String comando = String.format("magic tp %s %.2f %.2f %.2f", jugador, x, y, z);
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), comando);
                i++;
            }
        }
    }


    private List<Location> obtenerShroomlightLocations() {
        List<Location> locations = new ArrayList<>();
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Location loc = new Location(Bukkit.getWorld("world"), x, y, z);
                    if (loc.getBlock().getType() == Material.SHROOMLIGHT) {
                        locations.add(loc);
                    }
                }
            }
        }
        return locations;
    }

    public void iniciarSecuenciaInicioSkyBattle() {
        cancelarTareasActivas();
        int duracionPrimerSonido = 390 * 20;

        eventoActivo = true;

        new BukkitRunnable() {
            int contador = 0;

            @Override
            public void run() {
                switch (contador) {
                    case 0:
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "stopsound @a * minecraft:custom.music1_skybattle");
                        break;
                    case 1:
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "stopsound @a * minecraft:custom.music2_skybattle");
                        break;
                    case 2:
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "playsound minecraft:custom.music2_skybattle record @a ~ ~ ~ 0 0.1 0.1");
                        break;
                    case 3:
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "stopsound @a * minecraft:custom.music2_skybattle");
                        this.cancel();
                        break;
                }
                contador++;
            }
        }.runTaskTimer(plugin, 0, 20);

        // Esperar unos ticks para asegurar que los sonidos precargados sean cargados correctamente
        BukkitTask tarea1 = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "playsound minecraft:custom.music1_skybattle record @a ~ ~ ~ 1 1 1");

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (eventoActivo) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "stopsound @a * minecraft:custom.music1_skybattle");
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "playsound minecraft:custom.music2_skybattle record @a ~ ~ ~ 1 1 1");

                    BukkitTask tarea2 = Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        if (eventoActivo) {
                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "tick rate 25");
                            for (Player player : Bukkit.getOnlinePlayers()) {
                                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 2, true, false, false)); // Velocidad III
                            }
                            for (Player player : Bukkit.getOnlinePlayers()) {
                                player.sendTitle("\uE077", " ", 10, 50, 10);
                            }
                        }
                    }, 180);
                    tareasActivas.add(tarea2);
                }
            }, duracionPrimerSonido + 20);
        }, 20);
        tareasActivas.add(tarea1);

        new BukkitRunnable() {
            int contador = 10;

            @Override
            public void run() {
                if (!eventoActivo) {
                    this.cancel();
                    return;
                }
                if (contador > 0) {
                    String color = "#D172F6";
                    if (contador == 3) {
                        color = "yellow";
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "playsound minecraft:custom.321_fight_mortal_kombat record @a ~ ~ ~ 1 1 1");
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "playsound minecraft:block.note_block.pling record @a ~ ~ ~ 1 1.8 1");
                    } else if (contador == 2) {
                        color = "gold";
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "playsound minecraft:block.note_block.pling record @a ~ ~ ~ 1 1.9 1");
                    } else if (contador == 1) {
                        color = "red";
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "difficulty peaceful");
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "playsound minecraft:block.note_block.pling record @a ~ ~ ~ 1 2 1");
                    } else if (contador == 10) {
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "playsound minecraft:block.note_block.pling record @a ~ ~ ~ 1 1.1 1");
                    } else if (contador == 9) {
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "playsound minecraft:block.note_block.pling record @a ~ ~ ~ 1 1.2 1");
                    } else if (contador == 8) {
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "playsound minecraft:block.note_block.pling record @a ~ ~ ~ 1 1.3 1");
                    } else if (contador == 7) {
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "playsound minecraft:block.note_block.pling record @a ~ ~ ~ 1 1.4 1");
                    } else if (contador == 6) {
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "playsound minecraft:block.note_block.pling record @a ~ ~ ~ 1 1.5 1");
                    } else if (contador == 5) {
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "playsound minecraft:block.note_block.pling record @a ~ ~ ~ 1 1.6 1");
                    } else if (contador == 4) {
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "playsound minecraft:block.note_block.pling record @a ~ ~ ~ 1 1.7 1");
                    }
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "title @a times 0 40 0");
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "title @a title [\"\",{\"text\":\"Start\",\"bold\":true,\"color\":\"#D172F6\"},{\"text\":\":\",\"bold\":true,\"color\":\"gray\"}]");
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "title @a subtitle [\"\",{\"text\":\"\\u25b6\",\"bold\":true},{\"text\":\"" + contador + "\",\"bold\":true,\"color\":\"" + color + "\"},{\"text\":\"\\u25c0\",\"bold\":true}]");
                    contador--;
                } else {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "title @a title {\"text\":\"Fight!\",\"bold\":true,\"color\":\"#7E5FE6\"}");
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "title @a subtitle {\"text\":\"\",\"bold\":true,\"color\":\"#7E5FE6\"}");
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "playsound minecraft:custom.fight_box record @a ~ ~ ~ 1 1 1");
                    this.cancel();
                    if (eventoActivo) {
                        iniciarSkyBattle();
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "playdisco");
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "title @a times 15 25 15");
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "difficulty hard");
                    }
                }
            }
        }.runTaskTimer(plugin, 350, 20);
    }


    public void iniciarSkyBattle() {

        // Código existente para iniciar SkyBattle
        for (String jugador : participantes) {
            Player p = Bukkit.getPlayer(jugador);
            if (p != null) {
                p.getInventory().clear();
                p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 200, 4));
                p.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 0));// 10 segundos de resistencia

                ItemStack[] kit = {
                        new ItemStack(Material.LEATHER_CHESTPLATE),
                        new ItemStack(Material.LEATHER_LEGGINGS),
                        new ItemStack(Material.LEATHER_BOOTS),
                        new ItemStack(Material.STONE_SWORD),
                        new ItemStack(Material.COOKED_BEEF, 32),
                        new ItemStack(Material.IRON_PICKAXE),
                        new ItemStack(Material.PURPLE_CONCRETE, 64)
                };

                for (ItemStack item : kit) {
                    if (item.getType() == Material.LEATHER_CHESTPLATE ||
                            item.getType() == Material.LEATHER_LEGGINGS || item.getType() == Material.LEATHER_BOOTS) {
                        ItemMeta meta = item.getItemMeta();
                        meta.addEnchant(Enchantment.PROTECTION, 1, true);
                        item.setItemMeta(meta);
                    }
                    p.getInventory().addItem(item);
                }

                inicializarScoreboard();
            }
        }

        // Guardar ubicaciones originales y romper todos los shroomlight debajo de los jugadores
        ubicacionesShroomlightOriginales = obtenerShroomlightLocations();
        for (Location loc : ubicacionesShroomlightOriginales) {
            loc.getBlock().setType(Material.AIR);
        }

        iniciarReduccionBorde();

        aplicarDanioFueraDelBorde();
    }

    private void inicializarScoreboard() {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        Scoreboard board = manager.getNewScoreboard();

        Objective objective = board.registerNewObjective("skybattle", "dummy", "\u3201\uE080\u3201\u3201 ");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        addLine(objective, "§r  ", 9);
        addLine(objective, "        \uE078", 8);
        addLine(objective, "§r   ", 7);
        addLine(objective, "§r     ", 6);
        addLine(objective, "§r      ", 5);

        addLine(objective, "§r       ", 4);
        addLine(objective, "§r        ", 4);

        addLine(objective, "  §5\uD83D\uDDE1§6§l Players Left§r§l: §a§l" + participantes.size(), 3);

        addLine(objective, "§r          ", 2);

        addLine(objective, "  §e⚠§c§l Borde§r§l: §7§l02:00", 1);

        addLine(objective, "§r           ", 0);

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.setScoreboard(board);
        }
    }

    private void addLine(Objective objective, String text, int score) {
        Score line = objective.getScore(text);
        line.setScore(score);
    }

    private void actualizarScoreboard() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            Scoreboard board = player.getScoreboard();
            if (board == null) continue;

            Objective objective = board.getObjective("skybattle");
            if (objective == null) continue;

            // Actualizar los valores dinámicos
            String jugadoresRestantesTexto = "  §5\uD83D\uDDE1§6§l Players Left§r§l: §a§l" + participantes.size();
            String tiempoBordeTexto = bordeReduciendose ? "§4§l> > >" :
                    (tiempoBorde == -1 ? "§7§l00:00" : "§7§l" + obtenerTiempoBorde());

            // Actualizar jugadores restantes
            for (String entry : board.getEntries()) {
                if (entry.startsWith("  §5\uD83D\uDDE1§6§l Players Left§r§l: §a§l")) {
                    board.resetScores(entry);
                    break;
                }
            }
            objective.getScore(jugadoresRestantesTexto).setScore(3);

            // Actualizar borde
            for (String entry : board.getEntries()) {
                if (entry.startsWith("  §e⚠§c§l Borde")) {
                    board.resetScores(entry);
                    break;
                }
            }
            objective.getScore("  §e⚠§c§l Borde§r§l: " + tiempoBordeTexto).setScore(1);
        }
    }


    private String obtenerTiempoBorde() {
        if (tiempoBorde == -1) {
            return "00:00";
        }
        int minutos = tiempoBorde / 60;
        int segundos = tiempoBorde % 60;
        return String.format("%02d:%02d", minutos, segundos);
    }

    private void mostrarBordeParticulas(int minX, int maxX, int minZ, int maxZ) {
        World world = Bukkit.getWorld("world");

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                if ((x == minX || x == maxX || z == minZ || z == maxZ) &&
                        !(x == 20000 && z == 20000)) {
                    for (int y = 27; y <= 110; y++) {
                        world.spawnParticle(Particle.ANGRY_VILLAGER, new Location(world, x, y, z), 1);
                    }
                }
            }
        }
    }

    private void mostrarBordeParticulasContinuo() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!eventoActivo) {
                    this.cancel();
                    return;
                }
                mostrarBordeParticulas(minX, maxX, minZ, maxZ);
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    private void iniciarReduccionBorde() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (participantes.size() <= 1 || !eventoActivo) {
                    this.cancel();
                    return;
                }

                if (tiempoBorde <= 0 && !bordeReduciendose) {
                    bordeReduciendose = true;

                    // Comprobar si el borde ha alcanzado las coordenadas finales
                    if (minX >= 19999 && maxX <= 20001 && minZ >= 19999 && maxZ <= 20001) {
                        minX = 19999;
                        maxX = 20001;
                        minZ = 19999;
                        maxZ = 20001;
                        tiempoBorde = -1;
                        Bukkit.broadcastMessage("§§§e§l۞§6§l El borde ha llegado a su limite.§r§§ ");
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "title @a title {\"text\":\" \",\"bold\":true,\"color\":\"red\"}");
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "title @a subtitle [\"\",{\"text\":\"\\u26a0\",\"bold\":true,\"color\":\"yellow\"},{\"text\":\" El borde ya no se moverá.\",\"bold\":true,\"color\":\"gold\"},{\"text\":\" \\u26a0\",\"bold\":true,\"color\":\"yellow\"}]");
                        bordeReduciendose = false;
                        actualizarScoreboard();
                        this.cancel();
                        return;
                    }

                    Bukkit.broadcastMessage("§§§c§l۞§4§l El borde ha comenzado a reducirse!!§r§§ ");
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "title @a title {\"text\":\" \",\"bold\":true,\"color\":\"red\"}");
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "title @a subtitle [\"\",{\"text\":\"\\u26a0\",\"bold\":true,\"color\":\"red\"},{\"text\":\" Borde reduciéndose. \",\"bold\":true,\"color\":\"dark_red\"},{\"text\":\"\\u26a0\",\"bold\":true,\"color\":\"red\"}]");

                    new BukkitRunnable() {
                        int steps = 10;
                        int stepSize = 2;

                        @Override
                        public void run() {
                            if (steps <= 0) {
                                tiempoBorde = 120;
                                bordeReduciendose = false;
                                Bukkit.broadcastMessage("§§§6§l۞ §c§lEl borde se ha detenido. §r§l§§§7§lTiempo reiniciado a§6§l 2 minutos");
                                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "title @a title {\"text\":\" \",\"bold\":true,\"color\":\"red\"}");
                                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "title @a subtitle [\"\",{\"text\":\"\\u26a0\",\"bold\":true,\"color\":\"gold\"},{\"text\":\" Borde Detenido.\",\"bold\":true,\"color\":\"red\"},{\"text\":\" \\u26a0\",\"bold\":true,\"color\":\"gold\"}]");
                                actualizarScoreboard();
                                this.cancel();
                            } else {
                                // Reducir borde
                                minX = Math.min(minX + stepSize, 19999);
                                maxX = Math.max(maxX - stepSize, 20001);
                                minZ = Math.min(minZ + stepSize, 19999);
                                maxZ = Math.max(maxZ - stepSize, 20001);

                                mostrarBordeParticulas(minX, maxX, minZ, maxZ);
                                steps--;
                            }
                        }
                    }.runTaskTimer(plugin, 0L, 20L);
                }

                actualizarScoreboard();

                tiempoBorde--;
            }
        }.runTaskTimer(plugin, 0L, 15L);

        mostrarBordeParticulasContinuo();
    }

    private void procesarEliminacionJugador(Player jugador, Player atacante) {
        jugador.setHealth(jugador.getMaxHealth());

        jugador.getInventory().forEach(item -> {
            if (item != null) {
                jugador.getWorld().dropItemNaturally(jugador.getLocation(), item);
            }
        });
        jugador.getInventory().clear();

        Location deathLocation = jugador.getLocation();
        jugador.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, deathLocation.add(0, 1, 0), 100, 1, 1, 1, 0.5);

        String mensaje = "§8§l[§c§l☠§8§l]§6§l " + jugador.getName() + " §r§7ha perdido el §7§lLavaClash";
        if (atacante != null) {
            mensaje += " §r§7por §6§l" + atacante.getName();

            atacante.sendTitle(
                    "",
                    "§5§l\ud83d\udde1 §c§l \u2620§r§6" + jugador.getName(),
                    15, 25, 15
            );

            atacante.playSound(atacante.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 2.0f);
            atacante.playSound(atacante.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 2.0f, 2.0f);
            atacante.playSound(atacante.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 2.0f, 2.0f);
            atacante.playSound(atacante.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 2.0f, 2.0f);
        }

        Bukkit.broadcastMessage(mensaje);
        Bukkit.getOnlinePlayers().forEach(player -> {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 2.0f, 0.5f);
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 2.0f, 0.1f);
        });

        // Teletransportar al jugador fuera del evento
        Location ubiEspec = new Location(jugador.getWorld(), 20015.00, 106.00, 20000.27, -1979.64f, 0.46f);
        jugador.teleport(ubiEspec);

        // Eliminar al jugador de la lista de participantes
        participantes.remove(jugador.getName());
        actualizarScoreboard();

        if (participantes.size() == 1) {
            declararGanador();
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (eventoActivo && event.getEntity() instanceof Player) {
            Player jugador = (Player) event.getEntity();

            if (participantes.contains(jugador.getName())) {
                double nuevaVida = jugador.getHealth() - event.getFinalDamage();
                if (nuevaVida <= 0.3) {
                    event.setCancelled(true);

                    Player atacante = getAtacante(jugador);

                    procesarEliminacionJugador(jugador, atacante);
                }
            }
        }
    }


    private Player getAtacante(Player jugador) {
        EntityDamageEvent lastDamage = jugador.getLastDamageCause();
        if (lastDamage instanceof EntityDamageByEntityEvent) {
            EntityDamageByEntityEvent damageByEntityEvent = (EntityDamageByEntityEvent) lastDamage;
            Entity damager = damageByEntityEvent.getDamager();

            if (damager instanceof Player) {
                return (Player) damager;
            }
            if (damager instanceof Projectile) {
                Projectile projectile = (Projectile) damager;
                if (projectile.getShooter() instanceof Player) {
                    return (Player) projectile.getShooter();
                }
            }
            if (damager instanceof TNTPrimed) {
                TNTPrimed tnt = (TNTPrimed) damager;
                if (tnt.getSource() instanceof Player) {
                    return (Player) tnt.getSource();
                }
            }
            if (damager instanceof Creeper) {
                Creeper creeper = (Creeper) damager;
                if (creeper.getTarget() instanceof Player) {
                    return (Player) creeper.getTarget();
                }
            }
        }
        if (lastDamage.getCause() == EntityDamageEvent.DamageCause.FALL ||
                lastDamage.getCause() == EntityDamageEvent.DamageCause.LAVA ||
                lastDamage.getCause() == EntityDamageEvent.DamageCause.CONTACT ||
                lastDamage.getCause() == EntityDamageEvent.DamageCause.FIRE ||
                lastDamage.getCause() == EntityDamageEvent.DamageCause.FIRE_TICK ||
                lastDamage.getCause() == EntityDamageEvent.DamageCause.MAGIC ||
                lastDamage.getCause() == EntityDamageEvent.DamageCause.POISON ||
                lastDamage.getCause() == EntityDamageEvent.DamageCause.CUSTOM ||
                lastDamage.getCause() == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION ||
                lastDamage.getCause() == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION ||
                lastDamage.getCause() == EntityDamageEvent.DamageCause.DRYOUT ||
                lastDamage.getCause() == EntityDamageEvent.DamageCause.PROJECTILE) {
            return jugador.getKiller();
        }

        return null;
    }


    private void declararGanador() {
        for (String jugador : participantes) {
            Player ganador = Bukkit.getPlayer(jugador);
            if (ganador != null) {
                Bukkit.broadcastMessage("§d۞§6§l " + ganador.getName() + " §f§lha ganado el evento §e§lLavaClash!§7§l.");

                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.sendTitle(
                            "\uE082 ",
                            "§8§l>§6§l " + ganador.getName() + "§8§l <",
                            15, 200, 15
                    );

                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "playsound minecraft:ui.toast.challenge_complete record @a ~ ~ ~ 5 1 1");
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "playsound minecraft:custom.win_box record @a ~ ~ ~ 2 1 1");
                }

                // Teletransportar al ganador a la ubicación de espectadores
                Location ubiEspec = new Location(ganador.getWorld(), 20015.00, 106.00, 20000.27, -1979.64f, 0.46f);
                ganador.teleport(ubiEspec);

                terminarEvento();
                break;
            }
        }
    }



    private void restaurarShroomlights() {
        for (Location loc : ubicacionesShroomlightOriginales) {
            loc.getBlock().setType(Material.SHROOMLIGHT);
        }
    }

    private void eliminarPurpleConcrete() {
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Location loc = new Location(Bukkit.getWorld("world"), x, y, z);
                    if (loc.getBlock().getType() == Material.PURPLE_CONCRETE || loc.getBlock().getType() == Material.COBWEB) {
                        loc.getBlock().setType(Material.AIR);
                    }
                }
            }
        }
    }

    private void aplicarDanioFueraDelBorde() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!eventoActivo) {
                    this.cancel();
                    return;
                }

                for (String jugador : participantes) {
                    Player p = Bukkit.getPlayer(jugador);
                    if (p != null && p.getGameMode() == GameMode.SURVIVAL) {
                        Location loc = p.getLocation();
                        if (loc.getX() < minX || loc.getX() > maxX || loc.getZ() < minZ || loc.getZ() > maxZ) {
                            double nuevaVida = p.getHealth() - 1.0;
                            if (nuevaVida <= 0.3) {
                                procesarEliminacionJugador(p, null);
                            } else {
                                p.damage(1.0);
                            }
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 20L); // Ejecutar cada segundo
    }


    private void guardarEstadoEvento() {
        FileConfiguration config = YamlConfiguration.loadConfiguration(estadoArchivo);
        config.set("eventoActivo", eventoActivo);
        try {
            config.save(estadoArchivo);
        } catch (IOException e) {
            plugin.getLogger().severe("No se pudo guardar el estado del evento: " + e.getMessage());
        }
    }

    private void verificarEstadoEvento() {
        if (!estadoArchivo.exists()) return;

        FileConfiguration config = YamlConfiguration.loadConfiguration(estadoArchivo);
        boolean estadoGuardado = config.getBoolean("eventoActivo", false);

        if (estadoGuardado) {
            plugin.getLogger().warning("El evento estaba activo antes del reinicio. Finalizando automáticamente...");
            terminarEvento();
        }
    }

    // Evento para permitir romper cualquier bloque en modo creativo
    @EventHandler
    public void onBlockEvent(BlockBreakEvent event) {
        Block block = event.getBlock();
        Player player = event.getPlayer();

        // Si el jugador está en modo creativo, permitir romper cualquier bloque
        if (player.getGameMode() == GameMode.CREATIVE) {
            return; // Permite romper cualquier bloque sin restricciones
        }

        if (isInsideZone(block.getLocation())) {
            if (block.getType() == Material.PURPLE_CONCRETE || block.getType() == Material.COBWEB) {
                // Permitir romper pero no dropear el bloque
                event.setDropItems(false);
            } else {
                // Cancelar si no es PURPLE_CONCRETE
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "No puedes romper este bloque aquí.");
            }
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Block block = event.getBlock();
        Player player = event.getPlayer();

        if (player.getGameMode() == GameMode.CREATIVE) {
            return;
        }

        // Si el evento no está activo, no permitir colocar ningún bloque, ni PURPLE_CONCRETE, siempre y cuando esté en las coordenadas correctas
        if (!eventoActivo) {
            if (isInsideZone(block.getLocation())) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "No puedes colocar este bloque aquí.");
            }
            return;
        }

        // Si el evento está activo, solo permitir colocar PURPLE_CONCRETE
        if (isInsideZone(block.getLocation())) {
            if (block.getType() == Material.PURPLE_CONCRETE || block.getType() == Material.COBWEB) {
                // Devolver el bloque al inventario dependiendo de la mano que lo colocó
                Bukkit.getScheduler().runTask(plugin, () -> {
                    EquipmentSlot hand = event.getHand(); // Determinar qué mano usó
                    if (hand == EquipmentSlot.HAND) {
                        // Si se colocó desde la mano principal
                        player.getInventory().addItem(new ItemStack(Material.PURPLE_CONCRETE, 1));
                    } else if (hand == EquipmentSlot.OFF_HAND) {
                        // Si se colocó desde la mano secundaria, incrementar la cantidad existente
                        ItemStack offHandItem = player.getInventory().getItemInOffHand();
                        if (offHandItem.getType() == Material.PURPLE_CONCRETE) {
                            // Incrementar la cantidad en la offhand
                            offHandItem.setAmount(offHandItem.getAmount() + 1);
                            player.getInventory().setItemInOffHand(offHandItem);
                        } else {
                            // Si no hay PURPLE_CONCRETE en la offhand, agregarlo
                            player.getInventory().setItemInOffHand(new ItemStack(Material.PURPLE_CONCRETE, 1));
                        }
                    }
                });
            } else if (block.getType() == Material.TNT) {
                // Si el bloque colocado es TNT, activarla al instante (comienza el temporizador)
                TNTPrimed tnt = (TNTPrimed) block.getWorld().spawn(block.getLocation(), TNTPrimed.class);
                tnt.setFuseTicks(80); // Tiempo predeterminado de fusión (4 segundos)
                block.setType(Material.AIR); // Eliminar el bloque de TNT colocado
            } else {
                // Cancelar la colocación de cualquier otro bloque
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "No puedes colocar este bloque aquí.");
            }
        }
    }

    // Prevenir daño por explosiones
    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        if (isInsideZone(event.getLocation())) {
            event.blockList().removeIf(block -> block.getType() != Material.PURPLE_CONCRETE);
        }
    }

    @EventHandler
    public void onEntitySpawn(EntitySpawnEvent event) {
        // Si el evento está activo y la entidad está dentro de la zona
        if (eventoActivo && isInsideZone(event.getLocation())) {
            if (event.getEntity() instanceof Monster) {
                // Verificar si es un spawn con Spawner Egg
                if (event instanceof CreatureSpawnEvent creatureEvent) {

                    if (creatureEvent.getSpawnReason() == CreatureSpawnEvent.SpawnReason.SPAWNER_EGG) {
                        // Asignar el mob al jugador cercano
                        Player player = creatureEvent.getEntity().getWorld().getNearbyEntities(event.getLocation(), 1, 1, 1).stream()
                                .filter(e -> e instanceof Player)
                                .map(e -> (Player) e)
                                .findFirst()
                                .orElse(null);

                        if (player != null) {
                            Monster mob = (Monster) event.getEntity();
                            mob.setPersistent(true); // Para evitar que desaparezca

                            // Configurar que no ataque al jugador que lo generó
                            mob.setTarget(null);
                            mob.setAware(true); // Asegura que los mobs sean conscientes de otros jugadores
                            mob.addScoreboardTag("owner:" + player.getUniqueId()); // Guardar la referencia del jugador como propietario
                        }
                    } else {
                        // Cancelar spawns no permitidos
                        event.setCancelled(true);
                    }
                } else {
                    // Cancelar cualquier otro spawn que no sea manejado
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        if (eventoActivo) {
            for (Entity entity : event.getChunk().getEntities()) {
                // Si es un mob hostil y está dentro de la zona
                if (entity instanceof Monster mob && isInsideZone(entity.getLocation())) {

                    // Si no tiene un propietario, eliminarlo
                    if (!mob.getScoreboardTags().stream().anyMatch(tag -> tag.startsWith("owner:"))) {
                        mob.remove();
                    }
                }
            }
        }
    }

    // Método para evitar que los mobs con Spawner Egg ataquen al propietario
    @EventHandler
    public void onEntityTarget(EntityTargetLivingEntityEvent event) {
        if (eventoActivo && event.getEntity() instanceof Monster mob && event.getTarget() instanceof Player player) {

            // Si el mob tiene un propietario y el objetivo es ese propietario, cancelar el ataque
            if (mob.getScoreboardTags().contains("owner:" + player.getUniqueId())) {
                event.setCancelled(true);
            }
        }
    }

    public void eliminarMobsExistentes() {
        if (eventoActivo) {
            for (World world : Bukkit.getWorlds()) {
                for (Entity entity : world.getEntities()) {
                    // Si es un mob hostil y está dentro de la zona
                    if (entity instanceof Monster mob && isInsideZone(entity.getLocation())) {

                        // Si no tiene un propietario, eliminarlo
                        if (!mob.getScoreboardTags().stream().anyMatch(tag -> tag.startsWith("owner:"))) {
                            mob.remove();
                        }
                    }
                }
            }
        }
    }

    public void guardarContenidoCofres() {
        cofresHandler.guardarContenidoCofres(minX, maxX, minZ, maxZ);
    }

    public void restaurarContenidoCofres() {
        cofresHandler.restaurarContenidoCofres();
    }

    public void cargarCofresDesdeArchivo() {
        cofresHandler.cargarCofresDesdeArchivo();
    }

    private void cancelarTareasActivas() {
        for (BukkitTask tarea : tareasActivas) {
            if (tarea != null && !tarea.isCancelled()) {
                tarea.cancel();
            }
        }
        tareasActivas.clear();
    }

}
