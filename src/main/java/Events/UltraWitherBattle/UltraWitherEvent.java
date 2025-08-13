package Events.UltraWitherBattle;

import Commands.TiempoCommand;
import Dificultades.Features.UltraWitherDefeatedEvent;
import TitleListener.ErrorNotification;
import TitleListener.SuccessNotification;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
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
import org.bukkit.scheduler.BukkitTask;
import net.md_5.bungee.api.ChatColor;
import Dificultades.CustomMobs.UltraWitherBossHandler;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class UltraWitherEvent implements Listener {
    private final Set<String> listaEspera = new HashSet<>();
    private final Map<String, Long> cooldowns = new HashMap<>();
    private final List<Location> ubicacionesShroomlight = new ArrayList<>();
    private final List<BukkitTask> tareasActivas = new ArrayList<>();

    private boolean eventoActivo = false;
    private boolean eventoEnCurso = false;
    private boolean preparacion = false;
    private int contadorTeleport = 180; // 3 minutos
    private BukkitTask tareaContador;
    private BukkitTask tareaZonas;
    private final int MIN_JUGADORES_NORMAL = 3;
    private final int MIN_JUGADORES_SERVIDOR_VACIO = 1;
    private final int MAX_JUGADORES = 10;
    private final JavaPlugin plugin;
    private final TiempoCommand tiempoCommand;
    private static final String ULTRA_WITHER_BOSSBAR_ID = "UltraWitherEvent";

    // Handler para el Ultra Wither Boss
    private final UltraWitherBossHandler ultraWitherBossHandler;

    // Coordenadas del evento (Nether)
    private final int minX = 19886;
    private final int maxX = 20111;
    private final int minY = 137;
    private final int maxY = 182;
    private final int minZ = 19889;
    private final int maxZ = 20105;
    private final Location centroEvento = new Location(Bukkit.getWorld("world_nether"), 20000, 150, 20000);
    private final Location spawnBoss = new Location(Bukkit.getWorld("world_nether"), 20000, 151, 20000);

    // Zonas de colores para soul torch
    private final Map<String, Location[]> zonasColores = new HashMap<>();
    private final List<String> ordenZonas = Arrays.asList("roja", "verde", "azul", "amarilla");
    private int ultimaZonaEliminada = -1;

    private final File estadoArchivo;

    private final List<Location> beaconLocations = Arrays.asList(
            new Location(Bukkit.getWorld("world_nether"), 19968, 150, 20000),
            new Location(Bukkit.getWorld("world_nether"), 20000, 150, 19968),
            new Location(Bukkit.getWorld("world_nether"), 20032, 150, 20000),
            new Location(Bukkit.getWorld("world_nether"), 20000, 150, 20032)
    );

    // Colores para las zonas (protegidas y desprotegidas)
    private static final String COLOR_ROJO_PROTEGIDO = "#F26568";
    private static final String COLOR_ROJO_DESPROTEGIDO = "#7F3E48";
    private static final String COLOR_VERDE_PROTEGIDO = "#64E573";
    private static final String COLOR_VERDE_DESPROTEGIDO = "#3A694A";
    private static final String COLOR_AMARILLO_PROTEGIDO = "#EDD966";
    private static final String COLOR_AMARILLO_DESPROTEGIDO = "#716A43";
    private static final String COLOR_AZUL_PROTEGIDO = "#797BEF";
    private static final String COLOR_AZUL_DESPROTEGIDO = "#495F77";

    // Mapa para guardar las BossBars de los jugadores
    private final Map<UUID, BossBar> zonaBossBars = new HashMap<>();

    private final SuccessNotification successNotification;
    private final ErrorNotification errorNotification;

    public UltraWitherEvent(JavaPlugin plugin, TiempoCommand tiempoCommand, SuccessNotification successNotification, ErrorNotification errorNotification) {
        this.plugin = plugin;
        this.ultraWitherBossHandler = new UltraWitherBossHandler(plugin);
        this.tiempoCommand = tiempoCommand;
        this.successNotification = successNotification;
        this.errorNotification = errorNotification;
        this.estadoArchivo = new File(plugin.getDataFolder(), "estado_ultra_wither.yml");
        inicializarZonasColores();
        verificarEstadoEvento();
    }

    private void inicializarZonasColores() {
        // Zona roja
        zonasColores.put("roja", new Location[]{
                new Location(Bukkit.getWorld("world_nether"), 19968, 149, 20045),
                new Location(Bukkit.getWorld("world_nether"), 19955, 150, 20032)
        });

        // Zona verde
        zonasColores.put("verde", new Location[]{
                new Location(Bukkit.getWorld("world_nether"), 19968, 149, 19954),
                new Location(Bukkit.getWorld("world_nether"), 19955, 150, 19967)
        });

        // Zona azul
        zonasColores.put("azul", new Location[]{
                new Location(Bukkit.getWorld("world_nether"), 20033, 149, 19954),
                new Location(Bukkit.getWorld("world_nether"), 20046, 150, 19967)
        });

        // Zona amarilla
        zonasColores.put("amarilla", new Location[]{
                new Location(Bukkit.getWorld("world_nether"), 20033, 149, 20045),
                new Location(Bukkit.getWorld("world_nether"), 20046, 150, 20032)
        });
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player jugador = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null || item.getType() != Material.ECHO_SHARD) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasCustomModelData() || meta.getCustomModelData() != 800) return;

        if (!meta.hasDisplayName() || !meta.getDisplayName().equals("§e§lUltra Wither Compass")) return;

        event.setCancelled(true);

        String nombreJugador = jugador.getName();

        // Verificar cooldown
        if (cooldowns.containsKey(nombreJugador)) {
            long tiempoRestante = (cooldowns.get(nombreJugador) + 60000) - System.currentTimeMillis();
            if (tiempoRestante > 0) {
                int segundos = (int) (tiempoRestante / 1000);
                jugador.sendMessage("§c§l۞ §cDebes esperar §c§l" + segundos + " §csegundos antes de poder unirte de nuevo.");
                return;
            } else {
                cooldowns.remove(nombreJugador);
            }
        }

        // Si está en la lista, quitarlo
        if (listaEspera.contains(nombreJugador)) {
            listaEspera.remove(nombreJugador);
            cooldowns.put(nombreJugador, System.currentTimeMillis());

            // Eliminar BossBar específicamente
            Player nojugador = Bukkit.getPlayer(nombreJugador);
            if (nojugador != null) {
                String barId = nojugador.getUniqueId() + "_" + ULTRA_WITHER_BOSSBAR_ID;
                tiempoCommand.removeBossBar(barId);

                // También eliminar BossBar de zona si existe
                BossBar zonaBar = zonaBossBars.remove(nojugador.getUniqueId());
                if (zonaBar != null) {
                    zonaBar.removeAll();
                }
            }

            // Mensaje global de salida
            String mensajeSalida = "§c§l۞ " + ChatColor.of("#d5894c") + ChatColor.BOLD + nombreJugador + ChatColor.RESET + ChatColor.of("#ac5f18") + " salió de la Batalla contra el" + ChatColor.of("#903cbb") + ChatColor.BOLD + " Ultra Wither Boss " + ChatColor.RESET + "§7(" + ChatColor.GOLD + listaEspera.size() + "§7/" + ChatColor.GOLD + MAX_JUGADORES + "§7)";
            Bukkit.broadcastMessage(mensajeSalida);

            // Verificar si hay suficientes jugadores para continuar el contador
            verificarContador();
            return;
        }

        // Verificar si hay espacio
        if (listaEspera.size() >= MAX_JUGADORES) {
            jugador.sendMessage("§c§l۞ " + ChatColor.of("#c70063") + "La batalla está llena. Máximo " + ChatColor.GOLD + MAX_JUGADORES + ChatColor.RESET + ChatColor.of("#c70063") + " jugadores.");
            return;
        }

        // Agregar a la lista
        listaEspera.add(nombreJugador);

        // Mensaje global de entrada
        String mensajeEntrada = "§c§l۞ " + ChatColor.of("#d5894c") + ChatColor.BOLD + nombreJugador + ChatColor.RESET + ChatColor.of("#ef5a01") + " entró a la Batalla contra el" + ChatColor.of("#903cbb") + ChatColor.BOLD + " Ultra Wither Boss " + ChatColor.RESET + "§7(" + ChatColor.GOLD + listaEspera.size() + "§7/" + ChatColor.GOLD + MAX_JUGADORES + "§7)";
        Bukkit.broadcastMessage(mensajeEntrada);

        // Mensaje especial si el servidor tiene pocos jugadores
        int jugadoresOnline = Bukkit.getOnlinePlayers().size();
        if (jugadoresOnline < MIN_JUGADORES_NORMAL && listaEspera.size() == 1) {
            enviarMensajeAParticipantes(" ");
            enviarMensajeAParticipantes("§c§l[ADVERTENCIA] §4¡Este evento es EXTREMADAMENTE PELIGROSO con pocos jugadores en el servidor!");
            enviarMensajeAParticipantes(" ");
        }

        // Iniciar o verificar contador
        verificarContador();
    }

    private void verificarContador() {
        int jugadoresOnline = Bukkit.getOnlinePlayers().size();
        int minRequerido = jugadoresOnline < MIN_JUGADORES_NORMAL ? MIN_JUGADORES_SERVIDOR_VACIO : MIN_JUGADORES_NORMAL;

        if (listaEspera.size() >= minRequerido) {
            if (tareaContador == null || tareaContador.isCancelled()) {
                iniciarContadorTeleport();
                enviarMensajeAParticipantes("§6§l۞ " + ChatColor.of("#bc81e6") + "¡El evento comenzará en " + ChatColor.of("#ef5a01") + "3:00" + ChatColor.of("#bc81e6") + " minutos!");
            }
        } else {
            if (tareaContador != null && !tareaContador.isCancelled()) {
                tareaContador.cancel();
                limpiarBossBars();
                contadorTeleport = 180; // Reiniciar contador
                enviarMensajeAParticipantes("§6§l۞ " + ChatColor.of("#925aa5") + "Contador detenido. Se necesitan al menos " + ChatColor.GOLD + minRequerido + ChatColor.of("#925aa5") + " jugadores.");
            }
        }
    }

    private void iniciarContadorTeleport() {
        contadorTeleport = 180; // 3 minutos

        // Crear BossBar para cada jugador en la lista de espera
        for (String nombreJugador : listaEspera) {
            Player jugador = Bukkit.getPlayer(nombreJugador);
            if (jugador != null) {
                String barId = jugador.getUniqueId() + "_" + ULTRA_WITHER_BOSSBAR_ID;

                tiempoCommand.createPlayerBossBar(
                        jugador,
                        "§6§lUltra Wither Event§r§l:",
                        contadorTeleport,
                        "00:03:00",
                        "on",
                        barId
                );
            }
        }

        tareaContador = new BukkitRunnable() {
            @Override
            public void run() {
                contadorTeleport--;

                // Verificar si aún hay suficientes jugadores
                int jugadoresOnline = Bukkit.getOnlinePlayers().size();
                int minRequerido = jugadoresOnline < MIN_JUGADORES_NORMAL ? MIN_JUGADORES_SERVIDOR_VACIO : MIN_JUGADORES_NORMAL;

                if (listaEspera.size() < minRequerido) {
                    cancel();
                    contadorTeleport = 180;
                    limpiarBossBars();
                    enviarMensajeAParticipantes("§c§l۞ " +  ChatColor.of("#c70063") + "Contador cancelado por falta de jugadores.");
                    return;
                }

                // Efectos de sonido en los últimos 10 segundos
                if (contadorTeleport <= 10 && contadorTeleport > 0) {
                    reproducirSonidoAParticipantes("minecraft:block.note_block.pling", 1.0f, 0.7f);
                } else if (contadorTeleport == 0) {
                    cancel();
                    limpiarBossBars();
                    iniciarEvento();
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    private String formatTime(int seconds) {
        int m = seconds / 60;
        int s = seconds % 60;
        return String.format("%02d:%02d", m, s);
    }

    private void limpiarBossBars() {
        for (String nombreJugador : listaEspera) {
            Player jugador = Bukkit.getPlayer(nombreJugador);
            if (jugador != null) {
                String barId = jugador.getUniqueId() + "_" + ULTRA_WITHER_BOSSBAR_ID;
                tiempoCommand.removeBossBar(barId);
            }
        }
    }

    private void iniciarEvento() {
        eventoActivo = true;
        preparacion = true;

        // Obtener ubicaciones de shroomlight
        ubicacionesShroomlight.clear();
        ubicacionesShroomlight.addAll(obtenerShroomlightLocations());

        if (ubicacionesShroomlight.size() < listaEspera.size()) {
            enviarMensajeAParticipantes("§c[Error] No hay suficientes ubicaciones disponibles.");
            terminarEvento();
            return;
        }

        // Teletransportar jugadores
        Collections.shuffle(ubicacionesShroomlight);
        int i = 0;
        for (String nombreJugador : listaEspera) {
            Player jugador = Bukkit.getPlayer(nombreJugador);
            if (jugador != null && i < ubicacionesShroomlight.size()) {
                Location loc = ubicacionesShroomlight.get(i).add(0.5, 1, 0.5);
                String comando = String.format("magictp %s %.2f %.2f %.2f nether",
                        nombreJugador, loc.getX(), loc.getY(), loc.getZ());
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), comando);
                i++;
            }
        }

        // Esperar un poco y comenzar secuencia
        Bukkit.getScheduler().runTaskLater(plugin, this::iniciarSecuenciaInicio, 40L);
        guardarEstadoEvento();
    }

    private void iniciarSecuenciaInicio() {
        // Freezear jugadores
        for (String nombreJugador : listaEspera) {
            Player jugador = Bukkit.getPlayer(nombreJugador);
            if (jugador != null) {
                jugador.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, Integer.MAX_VALUE, 255, true, false, false));
            }
        }

        // Secuencia de instrucciones
        new BukkitRunnable() {
            int paso = 0;

            @Override
            public void run() {
                switch (paso) {
                    case 0:
                        String mensaje1 = "\n\n\n" +
                                ChatColor.of("#C77DFF") + ChatColor.BOLD + "۞ " +
                                ChatColor.of("#5A189A") + ChatColor.BOLD + "Instrucciones" +
                                ChatColor.of("#C77DFF") + ChatColor.BOLD + " ►" +
                                "\n\n" +
                                ChatColor.of("#BA6542") + "Bienvenidos a la batalla contra el" +
                                ChatColor.of("#903CBB") + ChatColor.BOLD + " Ultra Wither Boss." +
                                ChatColor.RESET + ChatColor.of("#BA6542") + "\nPara derrotarlo sin ninguna complicación, deberán\nsaber algunas cosas." +
                                "\n\n ";
                        enviarMensajeAParticipantes(mensaje1);
                        reproducirSonidoAParticipantes("minecraft:custom.noti", 1.0f, 1.3f);
                        break;
                    case 1:
                        String mensaje2 = "\n\n\n" +
                                ChatColor.of("#BA6542") + "Como pueden ver, a su alrededor hay " +
                                ChatColor.of("#DB864E") + ChatColor.BOLD + "4 zonas" + ChatColor.RESET +
                                ChatColor.of("#BA6542") + " con un\nfaro de distinto color: " +
                                ChatColor.of("#D45465") + ChatColor.BOLD + "rojo" + ChatColor.RESET +
                                ChatColor.of("#BA6542") + ", " +
                                ChatColor.of("#EAEA7B") + ChatColor.BOLD + "amarillo" + ChatColor.RESET +
                                ChatColor.of("#BA6542") + ", " +
                                ChatColor.of("#AEEE8D") + ChatColor.BOLD + "verde" + ChatColor.RESET +
                                ChatColor.of("#BA6542") + " y " +
                                ChatColor.of("#7B77F6") + ChatColor.BOLD + "azul" + ChatColor.RESET +
                                ChatColor.of("#BA6542") + ".\n\n ";
                        enviarMensajeAParticipantes(mensaje2);
                        reproducirSonidoAParticipantes("minecraft:custom.noti", 1.0f, 1.3f);
                        break;
                    case 2:
                        String mensaje3 = "\n\n\n" +
                                ChatColor.of("#BA6542") + "Esas zonas tienen " +
                                ChatColor.of("#D75877") + ChatColor.BOLD + "spawners" + ChatColor.RESET +
                                ChatColor.of("#BA6542") + " que están protegidos\ncon " +
                                ChatColor.of("#7BB0CC") + "antorchas de almas" + ChatColor.RESET +
                                ChatColor.of("#BA6542") + " encima.\nSi esas antorchas se quitan, comenzarán a aparecer\nmobs de " +
                                ChatColor.of("#DB864E") + ChatColor.BOLD + "forma infinita" + ChatColor.RESET +
                                ChatColor.of("#BA6542") + ".\n\n ";
                        enviarMensajeAParticipantes(mensaje3);
                        reproducirSonidoAParticipantes("minecraft:custom.noti", 1.0f, 1.3f);
                        break;
                    case 3:
                        String mensaje4 = "\n\n\n" +
                                ChatColor.of("#BA6542") + "Cada cierto tiempo, esas zonas " +
                                ChatColor.of("#DB864E") + ChatColor.BOLD + "serán desprotegidas" + ChatColor.RESET +
                                ChatColor.of("#BA6542") + ",\ny ustedes deberán colocar nuevamente las " +
                                ChatColor.of("#7BB0CC") + "antorchas\nde almas" + ChatColor.RESET +
                                ChatColor.of("#BA6542") + " sobre los " +
                                ChatColor.of("#D75877") + ChatColor.BOLD + "spawners" + ChatColor.RESET +
                                ChatColor.of("#BA6542") + " para evitar el " +
                                ChatColor.of("#DB864E") + ChatColor.BOLD + "spawn\ninfinito" + ChatColor.RESET +
                                ChatColor.of("#BA6542") + " de los mobs.\n\n ";
                        enviarMensajeAParticipantes(mensaje4);
                        reproducirSonidoAParticipantes("minecraft:custom.noti", 1.0f, 1.3f);
                    case 4:
                        String mensaje5 = "\n\n\n" +
                                ChatColor.of("#BA6542") + "A cada jugador se le darán " +
                                ChatColor.of("#DB864E") + ChatColor.BOLD + "32" + ChatColor.RESET +
                                ChatColor.of("#7BB0CC") + " antorchas de almas" + ChatColor.RESET +
                                ChatColor.of("#BA6542") + "\ny " +
                                ChatColor.of("#C750DC") + "concreto morado." + ChatColor.RESET +
                                ChatColor.of("#BA6542") + "\nEn esta zona no se podrá " +
                                ChatColor.of("#DB864E") + ChatColor.BOLD + "colocar" + ChatColor.RESET +
                                ChatColor.of("#BA6542") + " ni " +
                                ChatColor.of("#DB864E") + ChatColor.BOLD + "romper" + ChatColor.RESET +
                                ChatColor.of("#BA6542") + " ningún bloque," +
                                ChatColor.of("#BA6542") + "\n" +
                                ChatColor.of("#DB864E") + ChatColor.BOLD + "excepto" + ChatColor.RESET +
                                ChatColor.of("#BA6542") + " las " +
                                ChatColor.of("#DB864E") + ChatColor.BOLD + "antorchas de almas" + ChatColor.RESET +
                                ChatColor.of("#BA6542") + " y el " +
                                ChatColor.of("#DB864E") + ChatColor.BOLD + "concreto morado" + ChatColor.RESET +
                                ChatColor.of("#BA6542") + ".\n\n ";
                        enviarMensajeAParticipantes(mensaje5);
                        reproducirSonidoAParticipantes("minecraft:custom.noti", 1.0f, 1.3f);
                    case 5:
                        String mensaje6 = "\n\n\n" +
                                ChatColor.of("#BA6542") + "Cuando derroten al " +
                                ChatColor.of("#903CBB") + ChatColor.BOLD + "Ultra Wither Boss" + ChatColor.RESET +
                                ChatColor.of("#BA6542") + ", el evento " +
                                ChatColor.of("#DB864E") + ChatColor.BOLD + "terminará" + ChatColor.RESET +
                                ChatColor.of("#BA6542") + "\n" +
                                ChatColor.of("#BA6542") + "y serán teletransportados a sus " +
                                ChatColor.of("#92E7A7") + ChatColor.ITALIC + "spawns" + ChatColor.RESET +
                                ChatColor.of("#BA6542") + " después\nde " +
                                ChatColor.of("#DB864E") + ChatColor.BOLD + "30" + ChatColor.RESET +
                                ChatColor.of("#BA6542") + " segundos.\n\n" +
                                ChatColor.of("#EC2D43") + ChatColor.BOLD + "☠" +
                                ChatColor.of("#BA5052") + ChatColor.BOLD + " Buena Suerte, " +
                                ChatColor.of("#BA5052") + "la necesitarán" +
                                ChatColor.of("#BA5052") + ChatColor.BOLD + "!!!" +
                                ChatColor.of("#EC2D43") + ChatColor.BOLD + " ☠" +
                                "\n\n ";
                        enviarMensajeAParticipantes(mensaje6);
                        reproducirSonidoAParticipantes("minecraft:custom.noti", 1.0f, 1.3f);
                    case 6:
                        // Iniciar mensajes del Corrupted Dark Demon
                        iniciarMensajesDemon();
                        cancel();
                        break;
                }
                paso++;
            }
        }.runTaskTimer(plugin, 60L, 200L);
    }

    private void iniciarMensajesDemon() {
        String[] mensajes = {
                // Mensaje 1
                ChatColor.of("#4B3C30") + "" + ChatColor.BOLD + "Corrupted Dark Demon" + ChatColor.RESET + ":" +
                        ChatColor.of("#B25660") + ChatColor.ITALIC + " Vaya, vaya... ¿así que se atreven a entrar en mis dominios, eh?\u200b" +
                        "\n\n",

                // Mensaje 2
                ChatColor.of("#4B3C30") + "" + ChatColor.BOLD + "Corrupted Dark Demon" + ChatColor.RESET + ":" +
                        ChatColor.of("#B25660") + ChatColor.ITALIC + " Haré que una de mis " +
                        ChatColor.of("#AA1149") + ChatColor.BOLD + ChatColor.ITALIC + "creaciones" +
                        ChatColor.of("#B25660") + ChatColor.ITALIC + " destruya vuestra alma. ¡Jajaja!" +
                        "\n\n",

                // Mensaje 3
                ChatColor.of("#4B3C30") + "" + ChatColor.BOLD + "Corrupted Dark Demon" + ChatColor.RESET + ":" +
                        ChatColor.of("#B25660") + ChatColor.ITALIC + " ¡Sus insignificantes antorchas no significan nada frente a la " +
                        ChatColor.of("#595758") + ChatColor.BOLD + ChatColor.ITALIC + "eterna oscuridad" +
                        ChatColor.of("#B25660") + ChatColor.ITALIC + " que están por vivir!" +
                        "\n\n",

                // Mensaje 4
                ChatColor.of("#4B3C30") + "" + ChatColor.BOLD + "Corrupted Dark Demon" + ChatColor.RESET + ":" +
                        ChatColor.of("#B25660") + ChatColor.ITALIC + " ¿Sientes el peso de la desesperación aplastando tus insignificantes esperanzas de vivir? " +
                        ChatColor.of("#AA1149") + ChatColor.BOLD + ChatColor.ITALIC + "Qué patético." +
                        "\n\n",

                // Mensaje 5
                ChatColor.of("#4B3C30") + "" + ChatColor.BOLD + "Corrupted Dark Demon" + ChatColor.RESET + ":" +
                        ChatColor.of("#A80507") + " ¡Que comience el sufrimiento con el" +
                        ChatColor.of("#AA1149") + ChatColor.BOLD + " Ultra Wither Boss!" +
                        "\n\n\n\n "
        };

        new BukkitRunnable() {
            int indice = 0;

            @Override
            public void run() {
                if (indice >= mensajes.length) {
                    cancel();
                    spawnearBoss();
                    return;
                }

                enviarMensajeAParticipantes(mensajes[indice]);
                aplicarEfectosDarkness();
                reproducirSonidoAParticipantes("minecraft:entity.wither.ambient", 1.0f, 0.8f);

                indice++;
            }
        }.runTaskTimer(plugin, 100L, 200L);
    }

    private void aplicarEfectosDarkness() {
        for (String nombreJugador : listaEspera) {
            Player jugador = Bukkit.getPlayer(nombreJugador);
            if (jugador != null) {
                jugador.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 60, 0, true, false, false));
            }
        }
    }

    private void spawnearBoss() {
        // Spawnear el Ultra Wither Boss usando el handler personalizado
        ultraWitherBossHandler.spawnUltraWither(spawnBoss);

        // Comenzar el evento real
        eventoEnCurso = true;
        preparacion = false;

        actualizarBossBarZonas();

        // Programar actualización periódica de BossBars
        tareasActivas.add(new BukkitRunnable() {
            @Override
            public void run() {
                if (!eventoEnCurso) {
                    cancel();
                    return;
                }
                actualizarBossBarZonas();
            }
        }.runTaskTimer(plugin, 0L, 20L));

        // Dar items y efectos a jugadores
        for (String nombreJugador : listaEspera) {
            Player jugador = Bukkit.getPlayer(nombreJugador);
            if (jugador != null) {
                // Remover slowness
                jugador.removePotionEffect(PotionEffectType.SLOWNESS);

                // Dar Night Vision infinito
                jugador.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 0, true, false, false));

                // Dar items
                ItemStack soulTorch = new ItemStack(Material.SOUL_TORCH, 32);
                ItemStack purpleConcrete = new ItemStack(Material.PURPLE_CONCRETE, 64);

                // Intentar agregar al inventario, si está lleno dropear
                HashMap<Integer, ItemStack> excess = jugador.getInventory().addItem(soulTorch);
                for (ItemStack item : excess.values()) {
                    jugador.getWorld().dropItemNaturally(jugador.getLocation(), item);
                }

                jugador.getInventory().addItem(purpleConcrete);
            }
        }

        // Iniciar música del evento
        reproducirSonidoAParticipantes("minecraft:music_disc.pigstep", 0.5f, 1.0f);

        // Iniciar sistema de eliminación de zonas
        iniciarSistemaZonas();
        iniciarBuffBeacons();
    }

    private void iniciarSistemaZonas() {
        tareaZonas = new BukkitRunnable() {
            @Override
            public void run() {
                if (!eventoEnCurso) {
                    cancel();
                    return;
                }

                eliminarProteccionZonaAleatoria();
            }
        }.runTaskTimer(plugin, 1200L, (long) (1200 + Math.random() * 300)); // 60-75 segundos
    }

    private void eliminarProteccionZonaAleatoria() {
        List<Integer> zonasDisponibles = new ArrayList<>();
        for (int i = 0; i < ordenZonas.size(); i++) {
            if (i != ultimaZonaEliminada) {
                zonasDisponibles.add(i);
            }
        }

        if (zonasDisponibles.isEmpty()) {
            // Si todas han sido eliminadas, reiniciar
            for (int i = 0; i < ordenZonas.size(); i++) {
                zonasDisponibles.add(i);
            }
            ultimaZonaEliminada = -1;
        }

        int indiceZona = zonasDisponibles.get(new Random().nextInt(zonasDisponibles.size()));
        ultimaZonaEliminada = indiceZona;

        String nombreZona = ordenZonas.get(indiceZona);
        eliminarSoulTorchDeZona(nombreZona);

        actualizarBossBarZonas();

        String color = obtenerColorZona(nombreZona);
        mostrarNotificacionErrorAParticipantes();
        enviarMensajeAParticipantes("§c§l۞ §7Se ha eliminado la protección de la zona " + ChatColor.BOLD + color + nombreZona.toUpperCase() + "§7!");
        reproducirSonidoAParticipantes("minecraft:entity.wither.death", 1.0f, 1.5f);
    }

    private String obtenerColorZona(String zona) {
        switch (zona) {
            case "roja": return "§c";
            case "verde": return "§a";
            case "azul": return "§9";
            case "amarilla": return "§e";
            default: return "§7";
        }
    }

    private void eliminarSoulTorchDeZona(String zona) {
        Location[] limites = zonasColores.get(zona);
        if (limites == null || limites.length < 2) return;

        Location min = limites[0];
        Location max = limites[1];

        World world = min.getWorld();

        for (int x = Math.min(min.getBlockX(), max.getBlockX()); x <= Math.max(min.getBlockX(), max.getBlockX()); x++) {
            for (int y = Math.min(min.getBlockY(), max.getBlockY()); y <= Math.max(min.getBlockY(), max.getBlockY()); y++) {
                for (int z = Math.min(min.getBlockZ(), max.getBlockZ()); z <= Math.max(min.getBlockZ(), max.getBlockZ()); z++) {
                    Block bloque = world.getBlockAt(x, y, z);
                    if (bloque.getType() == Material.SOUL_TORCH) {
                        bloque.setType(Material.AIR);
                    }
                }
            }
        }
    }

    private void actualizarBossBarZonas() {
        // Verificar estado de protección de cada zona
        boolean rojaProtegida = estaZonaProtegida("roja");
        boolean verdeProtegida = estaZonaProtegida("verde");
        boolean azulProtegida = estaZonaProtegida("azul");
        boolean amarillaProtegida = estaZonaProtegida("amarilla");

        // Crear el texto de la BossBar
        String bossBarText = ChatColor.of(verdeProtegida ? COLOR_VERDE_PROTEGIDO : COLOR_VERDE_DESPROTEGIDO) + "█" +
                ChatColor.WHITE + "  " +
                ChatColor.of(rojaProtegida ? COLOR_ROJO_PROTEGIDO : COLOR_ROJO_DESPROTEGIDO) + "█" +
                ChatColor.WHITE + "  " +
                ChatColor.of(amarillaProtegida ? COLOR_AMARILLO_PROTEGIDO : COLOR_AMARILLO_DESPROTEGIDO) + "█" +
                ChatColor.WHITE + "  " +
                ChatColor.of(azulProtegida ? COLOR_AZUL_PROTEGIDO : COLOR_AZUL_DESPROTEGIDO) + "█";

        // Actualizar BossBar para cada jugador
        for (String nombreJugador : listaEspera) {
            Player jugador = Bukkit.getPlayer(nombreJugador);
            if (jugador != null) {
                BossBar bossBar = zonaBossBars.get(jugador.getUniqueId());

                if (bossBar == null) {
                    // Crear nueva BossBar si no existe
                    bossBar = Bukkit.createBossBar(
                            bossBarText,
                            BarColor.WHITE,
                            BarStyle.SOLID
                    );
                    bossBar.setVisible(true);
                    bossBar.addPlayer(jugador);
                    zonaBossBars.put(jugador.getUniqueId(), bossBar);
                } else {
                    // Actualizar BossBar existente
                    bossBar.setTitle(bossBarText);
                }
            }
        }
    }

    private boolean estaZonaProtegida(String zona) {
        Location[] limites = zonasColores.get(zona);
        if (limites == null || limites.length < 2) return false;

        Location min = limites[0];
        Location max = limites[1];
        World world = min.getWorld();

        int spawnersConTorch = 0;
        int totalSpawners = 0;

        for (int x = Math.min(min.getBlockX(), max.getBlockX()); x <= Math.max(min.getBlockX(), max.getBlockX()); x++) {
            for (int y = Math.min(min.getBlockY(), max.getBlockY()); y <= Math.max(min.getBlockY(), max.getBlockY()); y++) {
                for (int z = Math.min(min.getBlockZ(), max.getBlockZ()); z <= Math.max(min.getBlockZ(), max.getBlockZ()); z++) {
                    Block bloque = world.getBlockAt(x, y, z);
                    if (bloque.getType() == Material.SPAWNER) {
                        totalSpawners++;
                        Block arriba = world.getBlockAt(x, y + 1, z);
                        if (arriba.getType() == Material.SOUL_TORCH) {
                            spawnersConTorch++;
                        }
                    }
                }
            }
        }

        // La zona verde solo tiene 1 spawner, las demás tienen 2
        int spawnersNecesarios = zona.equals("verde") ? 1 : 2;
        return spawnersConTorch >= spawnersNecesarios;
    }

    private void verificarProteccionZona(Player jugador, String zona) {
        if (estaZonaProtegida(zona)) {
            String color = obtenerColorZona(zona);
            String nombreZona = zona.substring(0, 1).toUpperCase() + zona.substring(1);

            mostrarNotificacionExitoAParticipantes();
            enviarMensajeAParticipantes("§a§l۞ §7¡La zona " + color + nombreZona + " §7ha sido §aprotegida§7!");
            reproducirSonidoAParticipantes("minecraft:block.note_block.pling", 1.0f, 1.5f);

            // Actualizar BossBar
            actualizarBossBarZonas();
        }
    }

    private boolean estaEnZona(Location loc, Location[] limites) {
        if (limites.length < 2) return false;

        Location min = limites[0];
        Location max = limites[1];

        return loc.getX() >= Math.min(min.getX(), max.getX()) &&
                loc.getX() <= Math.max(min.getX(), max.getX()) &&
                loc.getY() >= Math.min(min.getY(), max.getY()) &&
                loc.getY() <= Math.max(min.getY(), max.getY()) &&
                loc.getZ() >= Math.min(min.getZ(), max.getZ()) &&
                loc.getZ() <= Math.max(min.getZ(), max.getZ());
    }

    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (!eventoEnCurso) return;

        if (!isInsideZone(event.getLocation())) return;

        Entity entity = event.getEntity();
        if (entity.getType() == EntityType.PIGLIN ||
                entity.getType() == EntityType.ZOMBIFIED_PIGLIN ||
                entity.getType() == EntityType.ENDERMAN) {
            event.setCancelled(true);
        }
    }

    private void iniciarBuffBeacons() {
        tareasActivas.add(new BukkitRunnable() {
            @Override
            public void run() {
                if (!eventoEnCurso) {
                    cancel();
                    return;
                }

                for (String nombreJugador : listaEspera) {
                    Player jugador = Bukkit.getPlayer(nombreJugador);
                    if (jugador != null && isInsideZone(jugador.getLocation())) {
                        // Verificar proximidad a beacons
                        for (Location beacon : beaconLocations) {
                            if (jugador.getLocation().distance(beacon) <= 5) {
                                aplicarBuffBeacon(jugador);
                                break; // Solo aplicar una vez aunque esté cerca de varios
                            }
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 100L)); // Cada 5 segundos
    }

    private void aplicarBuffBeacon(Player jugador) {
        jugador.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 120, 0, true, false));
        jugador.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 120, 0, true, false));
        jugador.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 120, 0, true, false));

        jugador.spawnParticle(Particle.HEART, jugador.getLocation().add(0, 1, 0), 5, 0.5, 0.5, 0.5, 0.1);
    }

    @EventHandler
    public void onUltraWitherDefeated(UltraWitherDefeatedEvent event) {
        if (!eventoEnCurso) return;

        if (isInsideZone(event.getWither().getLocation())) {
            finalizarEvento();
        }
    }


    private void finalizarEvento() {
        reproducirSonidoAParticipantes("minecraft:ui.toast.challenge_complete", 1.0f, 1.0f);

        if (tareaZonas != null && !tareaZonas.isCancelled()) {
            tareaZonas.cancel();
        }

        // Teletransportar jugadores a shroomlight
        int i = 0;
        for (String nombreJugador : listaEspera) {
            Player jugador = Bukkit.getPlayer(nombreJugador);
            if (jugador != null && i < ubicacionesShroomlight.size()) {
                Location loc = ubicacionesShroomlight.get(i).add(0.5, 1, 0.5);
                String comando = String.format("magictp %s %.2f %.2f %.2f nether",
                        nombreJugador, loc.getX(), loc.getY(), loc.getZ());
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), comando);

                // Remover efectos
                jugador.removePotionEffect(PotionEffectType.NIGHT_VISION);
                i++;
            }
        }

        // Restaurar zonas de colores
        restaurarZonasColores();

        // Limpiar purple concrete
        limpiarPurpleConcrete();

        // Teleport final después de 30 segundos
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (String nombreJugador : listaEspera) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "magictp " + nombreJugador + " spawn");
            }
            terminarEvento();
        }, 600L); // 30 segundos
    }

    private void restaurarZonasColores() {
        for (Map.Entry<String, Location[]> entry : zonasColores.entrySet()) {
            Location[] limites = entry.getValue();
            if (limites.length < 2) continue;

            Location min = limites[0];
            Location max = limites[1];
            World world = min.getWorld();

            // Restaurar soul torches en spawners (buscar spawners y poner soul torch encima)
            for (int x = Math.min(min.getBlockX(), max.getBlockX()); x <= Math.max(min.getBlockX(), max.getBlockX()); x++) {
                for (int y = Math.min(min.getBlockY(), max.getBlockY()); y <= Math.max(min.getBlockY(), max.getBlockY()); y++) {
                    for (int z = Math.min(min.getBlockZ(), max.getBlockZ()); z <= Math.max(min.getBlockZ(), max.getBlockZ()); z++) {
                        Block bloque = world.getBlockAt(x, y, z);
                        if (bloque.getType() == Material.COBWEB) {
                            bloque.setType(Material.AIR);
                        }
                        if (bloque.getType() == Material.SPAWNER) {
                            Block encima = world.getBlockAt(x, y + 1, z);
                            if (encima.getType() == Material.AIR) {
                                encima.setType(Material.SOUL_TORCH);
                            }
                        }
                    }
                }
            }
        }

        actualizarBossBarZonas();
    }

    private void limpiarPurpleConcrete() {
        World world = Bukkit.getWorld("world_nether");
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Block bloque = world.getBlockAt(x, y, z);
                    if (bloque.getType() == Material.PURPLE_CONCRETE || bloque.getType() == Material.COBWEB) {
                        bloque.setType(Material.AIR);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!eventoActivo || !preparacion) return;

        Player jugador = event.getPlayer();
        if (!listaEspera.contains(jugador.getName())) return;

        // Si está en preparación, freezear
        Location desde = event.getFrom();
        Location hacia = event.getTo();

        if (hacia != null && (desde.getX() != hacia.getX() || desde.getZ() != hacia.getZ())) {
            // Mini teleport para retroceder
            jugador.teleport(desde);
        }
    }

/*    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        String nombreJugador = event.getPlayer().getName();
        if (listaEspera.contains(nombreJugador)) {
            listaEspera.remove(nombreJugador);

            if (eventoActivo && eventoEnCurso) {
                enviarMensajeAParticipantes("§c" + nombreJugador + " ha abandonado la batalla.");

                // Si solo queda un jugador o menos, terminar evento
                if (listaEspera.size() <= 1) {
                    enviarMensajeAParticipantes("§c¡No quedan suficientes jugadores! Terminando evento...");
                    Bukkit.getScheduler().runTaskLater(plugin, this::terminarEvento, 60L);
                }
            } else {
                // Mensaje global si aún no empezó
                Bukkit.broadcastMessage("§c§l[Ultra Wither] §e" + nombreJugador + " §7salió de la Batalla contra el Ultra Wither Boss §8(" + listaEspera.size() + "/" + MAX_JUGADORES + ")");
                verificarContador();
            }
        }
    }*/

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (!isInsideZone(event.getBlock().getLocation())) return;

        Player jugador = event.getPlayer();
        Block bloque = event.getBlock();

        if (jugador.getGameMode() == GameMode.CREATIVE) return;

        if (bloque.getType() == Material.SOUL_TORCH || bloque.getType() == Material.PURPLE_CONCRETE || bloque.getType() == Material.COBWEB) {
            if (!eventoEnCurso) {
                event.setCancelled(true);
                jugador.sendMessage("§c¡No puedes romper bloques fuera del evento!");
            }
            // Durante el evento se pueden romper
        } else {
            event.setCancelled(true);
            jugador.sendMessage("§c¡Solo puedes romper Soul Torch y Purple Concrete!");
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!isInsideZone(event.getBlock().getLocation())) return;

        Player jugador = event.getPlayer();
        Block bloque = event.getBlock();

        if (jugador.getGameMode() == GameMode.CREATIVE) return;

        if (bloque.getType() == Material.SOUL_TORCH) {
            if (!eventoEnCurso) {
                event.setCancelled(true);
                jugador.sendMessage("§c¡No puedes colocar bloques fuera del evento!");
            }

            Block bloqueAbajo = bloque.getLocation().subtract(0, 1, 0).getBlock();
            if (bloqueAbajo.getType() == Material.SPAWNER) {
                for (Map.Entry<String, Location[]> entry : zonasColores.entrySet()) {
                    String zona = entry.getKey();
                    Location[] limites = entry.getValue();

                    if (estaEnZona(bloque.getLocation(), limites)) {
                        verificarProteccionZona(jugador, zona);
                        break;
                    }
                }
            }
        // Durante el evento se pueden colocar
        } else if (bloque.getType() == Material.PURPLE_CONCRETE) {
            if (!eventoEnCurso) {
                event.setCancelled(true);
                jugador.sendMessage("§c¡No puedes colocar bloques fuera del evento!");
                return;
            }

            // Verificar que no esté en zona de colores
            if (estaEnZonaColor(bloque.getLocation())) {
                event.setCancelled(true);
                jugador.sendMessage("§c¡No puedes colocar Purple Concrete en las zonas de colores!");
                return;
            }

            // Dar purple concrete infinito
            Bukkit.getScheduler().runTask(plugin, () -> {
                EquipmentSlot hand = event.getHand();
                if (hand == EquipmentSlot.HAND) {
                    jugador.getInventory().addItem(new ItemStack(Material.PURPLE_CONCRETE, 1));
                } else if (hand == EquipmentSlot.OFF_HAND) {
                    ItemStack offHandItem = jugador.getInventory().getItemInOffHand();
                    if (offHandItem.getType() == Material.PURPLE_CONCRETE) {
                        offHandItem.setAmount(offHandItem.getAmount() + 1);
                        jugador.getInventory().setItemInOffHand(offHandItem);
                    } else {
                        jugador.getInventory().setItemInOffHand(new ItemStack(Material.PURPLE_CONCRETE, 1));
                    }
                }
            });
        } else {
            event.setCancelled(true);
            jugador.sendMessage("§c¡Solo puedes colocar Soul Torch y Purple Concrete!");
        }
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        if (isInsideZone(event.getLocation())) {
            event.blockList().clear(); // No romper bloques por explosiones
        }
    }

    private boolean estaEnZonaColor(Location loc) {
        for (Location[] limites : zonasColores.values()) {
            if (limites.length < 2) continue;

            Location min = limites[0];
            Location max = limites[1];

            if (loc.getX() >= Math.min(min.getX(), max.getX()) && loc.getX() <= Math.max(min.getX(), max.getX()) &&
                    loc.getY() >= Math.min(min.getY(), max.getY()) && loc.getY() <= Math.max(min.getY(), max.getY()) &&
                    loc.getZ() >= Math.min(min.getZ(), max.getZ()) && loc.getZ() <= Math.max(min.getZ(), max.getZ())) {
                return true;
            }
        }
        return false;
    }

    private boolean isInsideZone(Location loc) {
        return loc.getX() >= minX && loc.getX() <= maxX &&
                loc.getY() >= minY && loc.getY() <= maxY &&
                loc.getZ() >= minZ && loc.getZ() <= maxZ &&
                loc.getWorld().getName().equals("world_nether");
    }

    private List<Location> obtenerShroomlightLocations() {
        List<Location> locations = new ArrayList<>();
        World nether = Bukkit.getWorld("world_nether");

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Location loc = new Location(nether, x, y, z);
                    if (loc.getBlock().getType() == Material.SHROOMLIGHT) {
                        locations.add(loc);
                    }
                }
            }
        }
        return locations;
    }

    private void enviarMensajeAParticipantes(String mensaje) {
        for (String nombreJugador : listaEspera) {
            Player jugador = Bukkit.getPlayer(nombreJugador);
            if (jugador != null) {
                jugador.sendMessage(mensaje);
            }
        }
    }

    private void reproducirSonidoAParticipantes(String sonido, float volumen, float tono) {
        for (String nombreJugador : listaEspera) {
            Player jugador = Bukkit.getPlayer(nombreJugador);
            if (jugador != null) {
                jugador.playSound(jugador.getLocation(), sonido, volumen, tono);
            }
        }
    }

    private void mostrarNotificacionExitoAParticipantes() {
        for (String nombreJugador : listaEspera) {
            Player jugador = Bukkit.getPlayer(nombreJugador);
            if (jugador != null) {
                successNotification.showSuccess(jugador);
            }
        }
    }

    private void mostrarNotificacionErrorAParticipantes() {
        for (String nombreJugador : listaEspera) {
            Player jugador = Bukkit.getPlayer(nombreJugador);
            if (jugador != null) {
                errorNotification.showSuccess(jugador);
            }
        }
    }


    public void terminarEvento() {
        eventoActivo = false;
        eventoEnCurso = false;
        preparacion = false;

        limpiarBossBars();

        for (BossBar bossBar : zonaBossBars.values()) {
            bossBar.removeAll();
        }
        zonaBossBars.clear();

        // Cancelar tareas
        if (tareaContador != null && !tareaContador.isCancelled()) {
            tareaContador.cancel();
        }
        if (tareaZonas != null && !tareaZonas.isCancelled()) {
            tareaZonas.cancel();
        }

        for (BukkitTask tarea : tareasActivas) {
            if (tarea != null && !tarea.isCancelled()) {
                tarea.cancel();
            }
        }
        tareasActivas.clear();

        // Limpiar listas
        listaEspera.clear();
        cooldowns.clear();
        ubicacionesShroomlight.clear();

        // Reiniciar variables
        contadorTeleport = 180;
        ultimaZonaEliminada = -1;

        Bukkit.broadcastMessage("§4§l[Ultra Wither] §7El evento ha terminado.");

        guardarEstadoEvento();
    }

    public void forzarInicioEvento() {
        if (!listaEspera.isEmpty()) {
            if (tareaContador != null && !tareaContador.isCancelled()) {
                tareaContador.cancel();
            }
            iniciarEvento();
        }
    }

    private void guardarEstadoEvento() {
        FileConfiguration config = YamlConfiguration.loadConfiguration(estadoArchivo);
        config.set("eventoActivo", eventoActivo);
        try {
            config.save(estadoArchivo);
        } catch (IOException e) {
            plugin.getLogger().severe("No se pudo guardar el estado del evento Ultra Wither: " + e.getMessage());
        }
    }

    private void verificarEstadoEvento() {
        if (!estadoArchivo.exists()) return;

        FileConfiguration config = YamlConfiguration.loadConfiguration(estadoArchivo);
        boolean estadoGuardado = config.getBoolean("eventoActivo", false);

        if (estadoGuardado) {
            plugin.getLogger().warning("El evento Ultra Wither estaba activo antes del reinicio. Finalizando automáticamente...");
            terminarEvento();
        }
    }

    // Métodos para comandos
    public void mostrarEstado(CommandSender sender) {
        sender.sendMessage("§6=== Estado Ultra Wither Event ===");
        sender.sendMessage("§7Evento activo: " + (eventoActivo ? "§aVerdadero" : "§cFalso"));
        sender.sendMessage("§7En curso: " + (eventoEnCurso ? "§aVerdadero" : "§cFalso"));
        sender.sendMessage("§7Preparación: " + (preparacion ? "§aVerdadero" : "§cFalso"));
        sender.sendMessage("§7Jugadores en lista: §e" + listaEspera.size() + "/" + MAX_JUGADORES);

        if (!listaEspera.isEmpty()) {
            sender.sendMessage("§7Participantes: §e" + String.join(", ", listaEspera));
        }

        if (tareaContador != null && !tareaContador.isCancelled()) {
            int minutos = contadorTeleport / 60;
            int segundos = contadorTeleport % 60;
            sender.sendMessage("§7Tiempo hasta teleport: §e" + minutos + ":" + String.format("%02d", segundos));
        }
    }

    public Set<String> getListaEspera() {
        return new HashSet<>(listaEspera);
    }

    public boolean isEventoActivo() {
        return eventoActivo;
    }
}
