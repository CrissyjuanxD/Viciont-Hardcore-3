package Events.Skybattle;

import com.sk89q.worldedit.bukkit.BukkitCommandSender;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
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
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class EventoHandler implements Listener {
    private final Set<String> participantes = new HashSet<>();
    private Map<Location, ItemStack[]> contenidoCofres = new HashMap<>();
    private boolean eventoActivo = false;
    private final int MAX_PARTICIPANTES = 20;
    private final JavaPlugin plugin;

    private int tiempoBorde = 120;
    private int minX = 19904;
    private int maxX = 20096;
    private int minZ = 19904;
    private int maxZ = 20096;
    private boolean bordeReduciendose = false;
    //test
    private final File estadoArchivo;


    public EventoHandler(JavaPlugin plugin) {
        this.plugin = plugin;
        //test
        this.estadoArchivo = new File(plugin.getDataFolder(), "estado_evento.yml");
        verificarEstadoEvento(); // Verificar el estado al cargar el plugin
    }

    public void iniciarEvento() {
        eventoActivo = true;
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "ruletavct [\"\",{\"text\":\"\\n\"},{\"text\":\"\\u06de Evento\",\"bold\":true,\"color\":\"#F977F9\"},{\"text\":\" \\u27a4\",\"bold\":true,\"color\":\"gray\"},{\"text\":\"\\n\\n\"},{\"text\":\"¡Ha comenzado el evento \",\"color\":\"#BA7FD0\"},{\"text\":\"LAVACLASH\",\"bold\":true,\"color\":\"#D98836\"},{\"text\":\"!\\nLos primeros \",\"color\":\"#BA7FD0\"},{\"text\":\"20\",\"bold\":true,\"color\":\"#BA7FD0\"},{\"text\":\" jugadores en obtener\\nun \",\"color\":\"#BA7FD0\"},{\"text\":\"Viciont Ticket\",\"bold\":true,\"color\":\"#E9BF66\"},{\"text\":\" participarán\\n\\nPara obtener el ticket deberan romper un \",\"color\":\"#BA7FD0\"},{\"text\":\"\\n\"},{\"text\":\"Bloque de Diamante!\",\"bold\":true,\"color\":\"#57A9CB\"},{\"text\":\"\\n \"}]");
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "gamerule sendCommandFeedback false");
        //test
        guardarEstadoEvento(); // Guardar estado
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

        // Limpiar el scoreboard de cada jugador
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
        }, 140L); // 100 ticks = 5 segundos
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
                }.runTaskLater(plugin, 200L); // 200 ticks = 10 segundos
            }
        }
    }

    private void teletransportarJugadores() {
        List<Location> shroomlightLocations = obtenerShroomlightLocations();

        if (shroomlightLocations.size() < participantes.size()) {
            Bukkit.broadcastMessage("§c[Error] No hay suficientes ubicaciones disponibles para teletransportar a los jugadores.");
            return;
        }

        // Guardar contenido de los cofres al teletransportar
        guardarContenidoCofres();
        // Mezclar las ubicaciones para asignarlas aleatoriamente
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

                // Usar Bukkit.dispatchCommand para ejecutar el comando
                String comando = String.format("magic tp %s %.2f %.2f %.2f", jugador, x, y, z);
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), comando);

                i++;
            }
        }
    }


    private List<Location> obtenerShroomlightLocations() {
        List<Location> locations = new ArrayList<>();
        int minX = 19904;
        int maxX = 20096;
        int minY = 27;
        int maxY = 110;
        int minZ = 19904;
        int maxZ = 20096;

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
        // Retrasar la secuencia 5 segundos (100 ticks)
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            ejecutarSecuencia();
        }, 100);
    }

    private void ejecutarSecuencia() {
        int duracionPrimerSonido = 390 * 20;

        // Reproducir el primer sonido
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "playsound minecraft:custom.music1_skybattle record @a ~ ~ ~ 1 1 1");

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            // Reproducir el segundo sonido
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "playsound minecraft:custom.music2_skybattle record @a ~ ~ ~ 1 1 1");

            // Después de 12 segundos desde el inicio de la secuencia
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "tick rate 25");
                for (Player player : Bukkit.getOnlinePlayers()) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 2, true, false, false)); // Velocidad III
                }
                for (Player player : Bukkit.getOnlinePlayers()) {
                    player.sendTitle("Fever Mode", "Activado", 10, 50, 10);
                }
            }, 140);

        }, duracionPrimerSonido + 20);

        // Programar la cuenta regresiva
        new BukkitRunnable() {
            int contador = 10;

            @Override
            public void run() {
                if (contador > 0) {
                    // Configurar título y subtítulo
                    String color = "#D172F6";
                    if (contador == 3) {
                        color = "yellow";
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "playsound minecraft:custom.321_fight_mortal_kombat record @a ~ ~ ~ 1 1 1");
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "playsound minecraft:block.note_block.pling record @a ~ ~ ~ 1 1.9 1");
                    } else if (contador == 2) {
                        color = "gold";
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "playsound minecraft:block.note_block.pling record @a ~ ~ ~ 1 1.8 1");
                    } else if (contador == 1) {
                        color = "red";
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "playsound minecraft:block.note_block.pling record @a ~ ~ ~ 1 2 1");
                    } else if (contador == 10) {
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "playsound minecraft:block.note_block.pling record @a ~ ~ ~ 1 1 1");
                    } else if (contador == 9) {
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "playsound minecraft:block.note_block.pling record @a ~ ~ ~ 1 1.1 1");
                    } else if (contador == 8) {
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "playsound minecraft:block.note_block.pling record @a ~ ~ ~ 1 1.2 1");
                    } else if (contador == 7) {
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "playsound minecraft:block.note_block.pling record @a ~ ~ ~ 1 1.3 1");
                    } else if (contador == 6) {
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "playsound minecraft:block.note_block.pling record @a ~ ~ ~ 1 1.4 1");
                    } else if (contador == 5) {
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "playsound minecraft:block.note_block.pling record @a ~ ~ ~ 1 1.5 1");
                    } else if (contador == 4) {
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "playsound minecraft:block.note_block.pling record @a ~ ~ ~ 1 1.6 1");
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

                    // Iniciar el evento SkyBattle
                    iniciarSkyBattle();
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "playdisco");
                }
            }
        }.runTaskTimer(plugin, 350, 20); // Comienza después de 18 segundos (360 ticks) y repite cada segundo (20 ticks)
    }



    private List<Location> ubicacionesShroomlightOriginales = new ArrayList<>();

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

                // Inicializar el scoreboard para cada jugador
                inicializarScoreboard();
            }
        }

        // Guardar ubicaciones originales y romper todos los shroomlight debajo de los jugadores
        ubicacionesShroomlightOriginales = obtenerShroomlightLocations();
        for (Location loc : ubicacionesShroomlightOriginales) {
            loc.getBlock().setType(Material.AIR);
        }

        // Iniciar la tarea de reducción del borde
        iniciarReduccionBorde();

        // Iniciar la tarea de aplicar daño fuera del borde
        aplicarDanioFueraDelBorde();
    }

    private void inicializarScoreboard() {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        Scoreboard board = manager.getNewScoreboard();

        Objective objective = board.registerNewObjective("skybattle", "dummy", "§6Skybattle");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        Score jugadoresRestantes = objective.getScore("§aJugadores restantes: " + participantes.size());
        jugadoresRestantes.setScore(3);

        Score borde = objective.getScore("§eBorde: 02:00");
        borde.setScore(1);

        // Asignar el scoreboard a todos los jugadores online
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.setScoreboard(board);
        }
    }

    private void actualizarScoreboard() {
        Scoreboard board = null;
        for (Player player : Bukkit.getOnlinePlayers()) {
            board = player.getScoreboard();
            if (board != null) break;
        }

        if (board != null) {
            Objective objective = board.getObjective("skybattle");

            if (objective != null) {
                for (String entry : board.getEntries()) {
                    board.resetScores(entry);
                }

                Score jugadoresRestantes = objective.getScore("§aJugadores restantes: " + participantes.size());
                jugadoresRestantes.setScore(3);

                String tiempoBordeTexto = bordeReduciendose ? "moviéndose" : (tiempoBorde == -1 ? "Ya no se moverá!" : obtenerTiempoBorde());
                Score borde = objective.getScore("§eBorde: " + tiempoBordeTexto);
                borde.setScore(1);
            }

            // Asignar el scoreboard actualizado a todos los jugadores online
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.setScoreboard(board);
            }
        }
    }



    private String obtenerTiempoBorde() {
        if (tiempoBorde == -1) {
            return "Ya no se moverá!";
        }
        int minutos = tiempoBorde / 60;
        int segundos = tiempoBorde % 60;
        return String.format("%02d:%02d", minutos, segundos);
    }

    private void mostrarBordeParticulas(int minX, int maxX, int minZ, int maxZ) {
        World world = Bukkit.getWorld("world");
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                if (x == minX || x == maxX || z == minZ || z == maxZ) {
                    for (int y = 27; y <= 110; y++) {
                        world.spawnParticle(Particle.OMINOUS_SPAWNING, new Location(world, x, y, z), 1);
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
        }.runTaskTimer(plugin, 0L, 20L); // Ejecutar cada segundo
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
                    if (minX >= 20000 && maxX <= 20000 && minZ >= 20000 && maxZ <= 20000) {
                        // Detener borde en 20000, 20000
                        maxX = 20000;
                        minX = 20000;
                        minZ = 20000;
                        maxZ = 20000;
                        tiempoBorde = -1; // Parar el contador
                        Bukkit.broadcastMessage("§e۞ El borde ya no se moverá.");
                        bordeReduciendose = false;
                        actualizarScoreboard();
                        this.cancel();
                        return;
                    }

                    // Anunciar reducción del borde
                    Bukkit.broadcastMessage("§e۞ Borde reduciéndose.");

                    // Mostrar borde reduciéndose lentamente durante 10 segundos
                    new BukkitRunnable() {
                        int steps = 10; // Número de pasos para mover el borde
                        int stepSize = 2; // Mover 2 bloques cada segundo

                        @Override
                        public void run() {
                            if (steps <= 0) {
                                tiempoBorde = 120;
                                bordeReduciendose = false;
                                actualizarScoreboard(); // Actualizar el scoreboard al finalizar la reducción
                                this.cancel();
                            } else {
                                // Mover borde lentamente
                                minX += stepSize;
                                maxX -= stepSize;
                                minZ += stepSize;
                                maxZ -= stepSize;
                                mostrarBordeParticulas(minX, maxX, minZ, maxZ);
                                steps--;
                            }
                        }
                    }.runTaskTimer(plugin, 0L, 20L); // Ejecutar cada segundo
                }

                // Actualizar el scoreboard de todos los jugadores cada segundo
                actualizarScoreboard();

                tiempoBorde--;
            }
        }.runTaskTimer(plugin, 0L, 15L); // Ejecutar cada segundo

        // Iniciar borde de partículas continuo
        mostrarBordeParticulasContinuo();
    }


    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (eventoActivo && event.getEntity() instanceof Player) {
            Player jugador = (Player) event.getEntity();
            if (participantes.contains(jugador.getName())) {
                double nuevaVida = jugador.getHealth() - event.getFinalDamage();
                if (nuevaVida <= 1) { // Menos de 0.5 corazones
                    event.setCancelled(true);
                    jugador.setHealth(jugador.getMaxHealth());

                    // Teletransportar al jugador a las coordenadas especificadas
                    Location ubiEspec = new Location(jugador.getWorld(), 20015.00, 106.00, 20000.27, -1979.64f, 0.46f);
                    jugador.teleport(ubiEspec);

                    participantes.remove(jugador.getName());

                    String mensaje = "§e۞ " + jugador.getName() + " ha perdido el skybattle";
                    if (event.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK && event instanceof EntityDamageByEntityEvent) {
                        EntityDamageByEntityEvent damageByEntityEvent = (EntityDamageByEntityEvent) event;
                        if (damageByEntityEvent.getDamager() instanceof Player) {
                            Player atacante = (Player) damageByEntityEvent.getDamager();
                            mensaje += " por " + atacante.getName();
                        }
                    }

                    Bukkit.broadcastMessage(mensaje);
                    actualizarScoreboard();

                    // Verificar si queda un solo jugador
                    if (participantes.size() == 1) {
                        declararGanador();
                    }
                }
            }
        }
    }


    private void declararGanador() {
        for (String jugador : participantes) {
            Player p = Bukkit.getPlayer(jugador);
            if (p != null) {
                Bukkit.broadcastMessage("§e۞ " + p.getName() + " ha ganado los skybattle!");
                p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "playsound minecraft:custom.win_box record @a ~ ~ ~ 1 1 1");
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
        int minX = 19904;
        int maxX = 20096;
        int minY = 27;
        int maxY = 110;
        int minZ = 19904;
        int maxZ = 20096;

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Location loc = new Location(Bukkit.getWorld("world"), x, y, z);
                    if (loc.getBlock().getType() == Material.PURPLE_CONCRETE) {
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
                            if (nuevaVida > 0.5) { // Aplicar daño solo si la nueva vida es mayor a 0.5 corazones
                                p.setLastDamageCause(new EntityDamageEvent(p, EntityDamageEvent.DamageCause.CUSTOM, 1.0));
                                p.damage(1.0);
                            } else {
                                p.setHealth(p.getMaxHealth()); // Restaurar la salud si la vida es menor o igual a 0.5 corazones
                            }
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 20L); // Ejecutar cada segundo
    }


    private void guardarContenidoCofres() {
        contenidoCofres.clear(); // Limpiar contenido previo antes de guardar

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int y = 0; y < 256; y++) { // Asumiendo que el mundo tiene una altura máxima de 256 bloques
                    Location loc = new Location(Bukkit.getWorld("world"), x, y, z);
                    Block block = loc.getBlock();
                    if (block.getType() == Material.CHEST) {
                        Chest chest = (Chest) block.getState();
                        // Crear una copia profunda del contenido del inventario
                        ItemStack[] contenidoCopia = new ItemStack[chest.getInventory().getSize()];
                        ItemStack[] contenidoOriginal = chest.getInventory().getContents();
                        for (int i = 0; i < contenidoOriginal.length; i++) {
                            contenidoCopia[i] = (contenidoOriginal[i] != null) ? contenidoOriginal[i].clone() : null;
                        }
                        contenidoCofres.put(loc, contenidoCopia);
                        System.out.println("Guardando contenido del cofre en: " + loc); // Mensaje de depuración
                    }
                }
            }
        }

        // Guardar en archivo para persistencia
        guardarCofresEnArchivo();
        System.out.println("Contenido de cofres guardado correctamente. Total de cofres: " + contenidoCofres.size());
    }

    private void restaurarContenidoCofres() {
        for (Map.Entry<Location, ItemStack[]> entry : contenidoCofres.entrySet()) {
            Location loc = entry.getKey();
            ItemStack[] contenidoOriginal = entry.getValue();
            Block block = loc.getBlock();
            if (block.getType() == Material.CHEST) {
                Chest chest = (Chest) block.getState();
                chest.getInventory().clear();
                chest.getInventory().setContents(contenidoOriginal);
                System.out.println("Restaurando contenido del cofre en: " + loc); // Mensaje de depuración
            } else {
                // Si el bloque ya no es un cofre, restaurar el cofre y su contenido
                block.setType(Material.CHEST);
                Chest chest = (Chest) block.getState();
                chest.getInventory().clear();
                chest.getInventory().setContents(contenidoOriginal);
                System.out.println("Cofre no encontrado en: " + loc + ". Creando y restaurando contenido."); // Mensaje de depuración
            }
        }

        System.out.println("Contenido de cofres restaurado correctamente. Total de cofres restaurados: " + contenidoCofres.size());
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
            terminarEvento(); // Finalizar el evento automáticamente
        }
    }

    private void guardarCofresEnArchivo() {
        File archivo = new File(plugin.getDataFolder(), "contenido_cofres.yml");
        YamlConfiguration config = new YamlConfiguration();

        for (Map.Entry<Location, ItemStack[]> entry : contenidoCofres.entrySet()) {
            Location loc = entry.getKey();
            ItemStack[] items = entry.getValue();
            String path = loc.getWorld().getName() + "."
                    + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
            config.set(path, items);
        }

        try {
            config.save(archivo);
            plugin.getLogger().info("Contenido de cofres guardado en archivo correctamente.");
        } catch (IOException e) {
            plugin.getLogger().severe("Error al guardar contenido de cofres en archivo: " + e.getMessage());
        }
    }


    private void cargarCofresDesdeArchivo() {
        File archivo = new File(plugin.getDataFolder(), "contenido_cofres.yml");
        if (!archivo.exists()) {
            plugin.getLogger().info("No se encontró archivo de cofres guardados.");
            return;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(archivo);
        contenidoCofres.clear(); // Limpiar cualquier contenido previo

        for (String worldKey : config.getKeys(false)) {
            World world = Bukkit.getWorld(worldKey);
            if (world == null) {
                plugin.getLogger().warning("El mundo especificado no existe: " + worldKey);
                continue;
            }

            ConfigurationSection section = config.getConfigurationSection(worldKey);
            if (section == null) continue;

            for (String locKey : section.getKeys(false)) {
                try {
                    String[] coords = locKey.split(",");
                    if (coords.length != 3) {
                        plugin.getLogger().warning("Coordenadas malformadas: " + locKey);
                        continue;
                    }

                    int x = Integer.parseInt(coords[0]);
                    int y = Integer.parseInt(coords[1]);
                    int z = Integer.parseInt(coords[2]);
                    Location loc = new Location(world, x, y, z);

                    List<?> listaItems = section.getList(locKey);
                    if (listaItems == null) {
                        plugin.getLogger().warning("No se encontraron items en: " + locKey);
                        continue;
                    }

                    ItemStack[] items = listaItems.toArray(new ItemStack[0]);
                    contenidoCofres.put(loc, items);
                } catch (NumberFormatException e) {
                    plugin.getLogger().warning("Error parseando coordenadas: " + locKey);
                }
            }
        }

        plugin.getLogger().info("Contenido de cofres cargado correctamente desde el archivo.");
    }

}
