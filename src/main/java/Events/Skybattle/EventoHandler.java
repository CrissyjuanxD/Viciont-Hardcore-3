package Events.Skybattle;

import Commands.TiempoCommand;
import Habilidades.HabilidadesEffects;
import Habilidades.HabilidadesManager;
import Handlers.EventInventoryManager;
import Handlers.Teams.TeamType;
import TitleListener.DiscoCommand;
import TitleListener.RuletaAnimation;
import com.viciontmedia.api.ViciontMediaAPI;
import com.viciontcinematics.api.ViciontCinematicAPI;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.chat.ComponentSerializer;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.*;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class EventoHandler implements Listener {

    private final HabilidadesManager habilidadesManager;
    private final HabilidadesEffects habilidadesEffects;
    private final RuletaAnimation eventoAnimation;
    private EventInventoryManager eventInventoryManager;
    private final DiscoCommand discoCommand;

    private final Set<String> participantes = new HashSet<>();
    private final Map<String, Integer> kills = new HashMap<>();
    private final List<String> ordenEliminados = new ArrayList<>();

    private List<Location> ubicacionesShroomlightOriginales = new ArrayList<>();
    private final Map<String, String> originalTeams = new HashMap<>();
    private final List<BukkitTask> tareasActivas = new ArrayList<>();

    private boolean eventoActivo = false;
    private boolean secuenciaBatallaIniciada = false;
    private final int MAX_PARTICIPANTES = 20;
    private final JavaPlugin plugin;
    private final CofresHandler cofresHandler;
    private Scoreboard eventoScoreboard;

    // --- CONFIGURACIÓN ---
    private File configFile;
    private FileConfiguration config;
    private String modoIngreso = "block";
    private String timerStart = "00:04:00";
    private int tiempoBordeInicial = 120;
    private int tiempoReglasSegundos = 150;
    private Location zonaEspectadores;

    private int arenaMinX, arenaMaxX, arenaMinY, arenaMaxY, arenaMinZ, arenaMaxZ;
    private int minX, maxX, minY, maxY, minZ, maxZ;
    private int tiempoBorde = 120;

    private boolean bordeReduciendose = false;
    private boolean eventoEnCurso = false;
    private boolean preparacion = false;
    private final File estadoArchivo;

    private BukkitRunnable taskBordeParticulas;
    private BukkitRunnable taskReduccionBorde;
    private BukkitRunnable taskReduccionBordeContinuo;

    // Variables para el monitor de pausa
    private BukkitTask pauseMonitorTask;
    private String lastPauseText = "";

    private Material bloqueActual;
    private String nombreBloqueActual;

    public EventoHandler(JavaPlugin plugin, HabilidadesManager habilidadesManager, HabilidadesEffects habilidadesEffects) {
        this.plugin = plugin;
        this.habilidadesManager = habilidadesManager;
        this.habilidadesEffects = habilidadesEffects;
        this.cofresHandler = new CofresHandler(plugin);
        this.eventoAnimation = new RuletaAnimation(plugin);
        this.discoCommand = new DiscoCommand(plugin);
        this.estadoArchivo = new File(plugin.getDataFolder(), "estado_evento.yml");

        crearYcargarConfig();
        verificarEstadoEvento();
    }

    public void crearYcargarConfig() {
        configFile = new File(plugin.getDataFolder(), "lavaclashconfig.yml");

        if (!configFile.exists()) {
            configFile.getParentFile().mkdirs();
            try {
                configFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        config = YamlConfiguration.loadConfiguration(configFile);

        config.options().header("=== Configuración de LavaClash ===");
        config.options().copyHeader(true);

        if (!config.contains("modo_ingreso")) {
            config.set("modo_ingreso", "block"); // "block" o "random"
            config.set("timer_start", "00:04:00");
            config.set("tiempo_borde_segundos", 120);
            config.set("tiempo_reglas_segundos", 150);

            config.set("zona.minX", 19904);
            config.set("zona.maxX", 20096);
            config.set("zona.minY", 27);
            config.set("zona.maxY", 110);
            config.set("zona.minZ", 19904);
            config.set("zona.maxZ", 20096);

            config.set("espectadores.x", 20015.00);
            config.set("espectadores.y", 106.00);
            config.set("espectadores.z", 20000.27);
            config.set("espectadores.yaw", -1979.64);
            config.set("espectadores.pitch", 0.46);

            try { config.save(configFile); } catch (IOException ignored) {}
        }

        modoIngreso = config.getString("modo_ingreso", "block");
        timerStart = config.getString("timer_start", "00:04:00");
        tiempoBordeInicial = config.getInt("tiempo_borde_segundos", 120);
        tiempoReglasSegundos = config.getInt("tiempo_reglas_segundos", 150);

        arenaMinX = config.getInt("zona.minX", 19904);
        arenaMaxX = config.getInt("zona.maxX", 20096);
        arenaMinY = config.getInt("zona.minY", 27);
        arenaMaxY = config.getInt("zona.maxY", 110);
        arenaMinZ = config.getInt("zona.minZ", 19904);
        arenaMaxZ = config.getInt("zona.maxZ", 20096);

        minX = arenaMinX;
        maxX = arenaMaxX;
        minY = arenaMinY;
        maxY = arenaMaxY;
        minZ = arenaMinZ;
        maxZ = arenaMaxZ;
        tiempoBorde = tiempoBordeInicial;

        double espX = config.getDouble("espectadores.x", 20015.00);
        double espY = config.getDouble("espectadores.y", 106.00);
        double espZ = config.getDouble("espectadores.z", 20000.27);
        float espYaw = (float) config.getDouble("espectadores.yaw", -1979.64);
        float espPitch = (float) config.getDouble("espectadores.pitch", 0.46);
        zonaEspectadores = new Location(Bukkit.getWorld("world"), espX, espY, espZ, espYaw, espPitch);
    }

    private int parseTimeToSeconds(String timeString) {
        String[] parts = timeString.split(":");
        if (parts.length == 3) {
            int hours = Integer.parseInt(parts[0]);
            int minutes = Integer.parseInt(parts[1]);
            int seconds = Integer.parseInt(parts[2]);
            return hours * 3600 + minutes * 60 + seconds;
        }
        return 240;
    }

    private boolean isInsideArenaGeneral(Location loc) {
        return loc.getBlockX() >= arenaMinX && loc.getBlockX() <= arenaMaxX
                && loc.getBlockY() >= arenaMinY && loc.getBlockY() <= arenaMaxY
                && loc.getBlockZ() >= arenaMinZ && loc.getBlockZ() <= arenaMaxZ;
    }

    private List<Player> getJugadoresEnZona() {
        return Bukkit.getOnlinePlayers().stream()
                .filter(p -> isInsideArenaGeneral(p.getLocation()))
                .collect(Collectors.toList());
    }

    private void enviarMensajeZona(String mensaje) {
        getJugadoresEnZona().forEach(p -> p.sendMessage(mensaje));
    }

    public void setEventInventoryManager(EventInventoryManager manager) {
        this.eventInventoryManager = manager;
    }

    public boolean isEventoActivo() {
        return eventoActivo;
    }

    public boolean isSecuenciaBatallaIniciada() {
        return secuenciaBatallaIniciada;
    }

    public boolean isParticipante(String name) {
        return participantes.contains(name);
    }

    public void iniciarEvento() {
        if (eventoActivo) return;
        eventoActivo = true;
        secuenciaBatallaIniciada = false;

        crearYcargarConfig();
        participantes.clear();
        ordenEliminados.clear();
        kills.clear();

        inicializarTeamLavaClashMain();

        if (modoIngreso.equalsIgnoreCase("random")) {
            List<Player> onlinePlayers = new ArrayList<>(Bukkit.getOnlinePlayers());
            Collections.shuffle(onlinePlayers);
            int amount = Math.min(MAX_PARTICIPANTES, onlinePlayers.size());

            for (int i = 0; i < amount; i++) {
                Player p = onlinePlayers.get(i);
                participantes.add(p.getName());
                aplicarTeamLavaClash(p);
            }

            String jsonMessage = "[\"\",{\"text\":\"\\n\"},{\"text\":\"\\u06de Evento\",\"bold\":true,\"color\":\"#F977F9\"},{\"text\":\" \\u27a4\",\"bold\":true,\"color\":\"gray\"},{\"text\":\"\\n\\n\"},{\"text\":\"¡Ha comenzado el evento \",\"color\":\"#c55cf3\"},{\"text\":\"LAVACLASH\",\"bold\":true,\"color\":\"#D98836\"},{\"text\":\"!\\n\",\"color\":\"#c55cf3\"},{\"text\":\"" + amount + " jugadores aleatorios han sido seleccionados.\\nSerán teletransportados en breve.\",\"color\":\"#c55cf3\"},{\"text\":\"\\n \"}]";

            for (Player p : Bukkit.getOnlinePlayers()) {
                eventoAnimation.playAnimation(p, "rosa", "evento", "center", jsonMessage);
            }

            World world = Bukkit.getWorld("world");
            if (world != null) world.setGameRule(GameRule.SEND_COMMAND_FEEDBACK, false);
            guardarEstadoEvento();

            // Solo hacemos auto-tp si se llenaron los 20 cupos obligatoriamente
            if (participantes.size() == MAX_PARTICIPANTES) {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (eventoActivo && !preparacion) {
                            teletransportarJugadores();
                        }
                    }
                }.runTaskLater(plugin, 840L); // 42s de espera para no cortar el video ni el mensaje de la ruleta
            }

        } else {
            List<Material> bloquesPosibles = List.of(
                    Material.POPPY, Material.STRIPPED_OAK_LOG, Material.GLASS,
                    Material.IRON_ORE, Material.DIAMOND_BLOCK
            );

            Map<Material, String> nombresBloques = Map.of(
                    Material.POPPY, "Flor Roja (Amapola)",
                    Material.STRIPPED_OAK_LOG, "Tronco de Roble sin corteza",
                    Material.GLASS, "Cristal",
                    Material.IRON_ORE, "Mineral de hierro",
                    Material.DIAMOND_BLOCK, "Bloque de diamante"
            );

            bloqueActual = bloquesPosibles.get(new Random().nextInt(bloquesPosibles.size()));
            nombreBloqueActual = nombresBloques.get(bloqueActual);

            String jsonMessage = "[\"\",{\"text\":\"\\n\"},{\"text\":\"\\u06de Evento\",\"bold\":true,\"color\":\"#F977F9\"},{\"text\":\" \\u27a4\",\"bold\":true,\"color\":\"gray\"},{\"text\":\"\\n\\n\"},{\"text\":\"¡Ha comenzado el evento \",\"color\":\"#c55cf3\"},{\"text\":\"LAVACLASH\",\"bold\":true,\"color\":\"#D98836\"},{\"text\":\"!\\nLos primeros \",\"color\":\"#c55cf3\"},{\"text\":\"20\",\"bold\":true,\"color\":\"#c55cf3\"},{\"text\":\" jugadores en obtener\\nun \",\"color\":\"#c55cf3\"},{\"text\":\"Manu Ticket\",\"bold\":true,\"color\":\"#E9BF66\"},{\"text\":\" participarán\\n\\nPara obtener el ticket deberan romper un \",\"color\":\"#c55cf3\"},{\"text\":\"\\n\"},{\"text\":\"" + nombreBloqueActual + "\",\"bold\":true,\"color\":\"#57A9CB\"},{\"text\":\"\\n \"}]";

            for (Player p : Bukkit.getOnlinePlayers()) {
                eventoAnimation.playAnimation(p, "rosa", "evento", "center", jsonMessage);
            }

            World world = Bukkit.getWorld("world");
            if (world != null) world.setGameRule(GameRule.SEND_COMMAND_FEEDBACK, false);
            guardarEstadoEvento();
        }
    }

    private void inicializarTeamLavaClashMain() {
        Scoreboard mainBoard = Bukkit.getScoreboardManager().getMainScoreboard();
        Team t = mainBoard.getTeam(TeamType.LAVACLASH.getId());
        if (t == null) {
            t = mainBoard.registerNewTeam(TeamType.LAVACLASH.getId());
        }
        t.setPrefix(TeamType.LAVACLASH.getChatPrefix());
        t.setColor(TeamType.LAVACLASH.getBukkitColor());
        t.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS);
        t.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
    }

    private void aplicarTeamLavaClash(Player p) {
        Scoreboard mainBoard = Bukkit.getScoreboardManager().getMainScoreboard();
        Team oldTeam = mainBoard.getEntryTeam(p.getName());
        if (oldTeam != null) {
            originalTeams.put(p.getName(), oldTeam.getName());
        } else {
            originalTeams.put(p.getName(), null);
        }

        Team lcTeam = mainBoard.getTeam(TeamType.LAVACLASH.getId());
        if (lcTeam != null) lcTeam.addEntry(p.getName());
    }

    private void restaurarTeamOriginal(String playerName) {
        Player p = Bukkit.getPlayer(playerName);
        if (p == null) return;

        Scoreboard mainBoard = Bukkit.getScoreboardManager().getMainScoreboard();
        Team lcTeam = mainBoard.getTeam(TeamType.LAVACLASH.getId());
        if (lcTeam != null) lcTeam.removeEntry(playerName);

        if (originalTeams.containsKey(playerName)) {
            String oldTeamName = originalTeams.get(playerName);
            if (oldTeamName != null) {
                Team old = mainBoard.getTeam(oldTeamName);
                if (old != null) old.addEntry(playerName);
            }
        }
    }

    public void forzarEvento() {
        if (eventoActivo && participantes.size() < MAX_PARTICIPANTES) {
            teletransportarJugadores();
        }
    }

    public void terminarEvento() {
        guardarDatosFinales();

        eventoActivo = false;
        eventoEnCurso = false;
        preparacion = false;
        secuenciaBatallaIniciada = false;

        borrarTextoPausa();
        if (pauseMonitorTask != null && !pauseMonitorTask.isCancelled()) {
            pauseMonitorTask.cancel();
        }

        for (String nombre : originalTeams.keySet()) {
            restaurarTeamOriginal(nombre);
            Player p = Bukkit.getPlayer(nombre);
            if (p != null) {
                habilidadesManager.enableHabilidades(p);
                habilidadesEffects.reapplyAllEffects(p, habilidadesManager);
            }
        }

        participantes.clear();
        originalTeams.clear();
        ordenEliminados.clear();
        kills.clear();

        for (Player p : getJugadoresEnZona()) {
            p.sendMessage(" ");
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "stopsound " + p.getName());
            p.sendMessage("§c۞ El evento ha terminado.");
            p.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        }

        if (eventoScoreboard != null) {
            Objective obj = eventoScoreboard.getObjective("skybattle");
            if (obj != null) obj.unregister();
            eventoScoreboard = null;
        }

        World world = Bukkit.getWorld("world");
        if (world != null) world.setGameRule(GameRule.SEND_COMMAND_FEEDBACK, true);

        restaurarShroomlights();
        eliminarPurpleConcrete();
        restaurarContenidoCofres();
        guardarEstadoEvento();
        cargarCofresDesdeArchivo();
        restaurarContenidoCofres();
        cancelarTareasActivas();

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            File archivo = new File(plugin.getDataFolder(), "contenido_cofres.yml");
            if (archivo.exists()) {
                archivo.delete();
            }
        }, 140L);

        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (isInsideArenaGeneral(p.getLocation())) {
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "magictp " + p.getName() + " spawn");
                    }
                }
            }
        }.runTaskLater(plugin, 300L);
    }

    private void reiniciarVariablesBorde() {
        minX = arenaMinX;
        maxX = arenaMaxX;
        minZ = arenaMinZ;
        maxZ = arenaMaxZ;
        tiempoBorde = tiempoBordeInicial;
        bordeReduciendose = false;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (!eventoActivo || preparacion || participantes.size() >= MAX_PARTICIPANTES) return;
        if (!modoIngreso.equalsIgnoreCase("block")) return;

        Player jugador = event.getPlayer();
        if (participantes.contains(jugador.getName())) return;

        if (event.getBlock().getType() == bloqueActual) {
            ItemStack ticket = new ItemStack(Material.ECHO_SHARD);
            ItemMeta meta = ticket.getItemMeta();
            meta.setDisplayName("§e§lViciont Ticket");
            meta.setCustomModelData(1);
            ticket.setItemMeta(meta);
            jugador.getInventory().addItem(ticket);

            participantes.add(jugador.getName());
            aplicarTeamLavaClash(jugador);

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
                        if (eventoActivo && !preparacion) {
                            teletransportarJugadores();
                        }
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

        int totalSegundos = parseTimeToSeconds(timerStart);
        int m = totalSegundos / 60;
        int s = totalSegundos % 60;
        String tiempoTexto = m + (s > 0 ? " min y " + s + " seg" : " minutos");

        String tellrawCommand2 = "[\"\",{\"text\":\"\\n\"},{\"text\":\"\\u06de Evento \",\"bold\":true,\"color\":\"#F977F9\"},{\"text\":\"\\u27a4\",\"color\":\"gray\"},{\"text\":\"\\n\\n\"},{\"text\":\"El evento empezará en\",\"color\":\"#C55CF3\"},{\"text\":\" " + tiempoTexto + ".\",\"bold\":true,\"color\":\"gold\"},{\"text\":\"\\n\"},{\"text\":\"Se recomienda a los\",\"color\":\"#C55CF3\"},{\"text\":\" jugadores\",\"bold\":true,\"color\":\"gold\"},{\"text\":\" que entraron en el evento\",\"color\":\"#C55CF3\"},{\"text\":\"\\n\"},{\"text\":\"que\",\"color\":\"#C55CF3\"},{\"text\":\" guarden\",\"bold\":true,\"color\":\"gold\"},{\"text\":\" sus cosas en\",\"color\":\"#C55CF3\"},{\"text\":\" cofres por seguridad.\",\"bold\":true,\"color\":\"gold\"},{\"text\":\"\\n\\n\"},{\"text\":\"IMPORTANTE\",\"bold\":true,\"color\":\"#F12C51\"},{\"text\":\":\",\"bold\":true,\"color\":\"gray\"},{\"text\":\" Guardar spawn en una cama antes del tp.\",\"bold\":true,\"color\":\"#C55CF3\"},{\"text\":\"\\n \"}]";

        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "tellraw @a " + tellrawCommand2);
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "playsound minecraft:custom.noti ambient @a ~ ~ ~ 1 1.3 1");

        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "timers crear @a tiempo=" + timerStart + " sonido=on");

        long delayTicks = (totalSegundos * 20L) + 50L;

        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
            guardarContenidoCofres();
            eliminarMobsExistentes();
            this.preparacion = true;

            Collections.shuffle(shroomlightLocations);

            int i = 0;
            for (String jugador : participantes) {
                Player p = Bukkit.getPlayer(jugador);
                if (p != null && i < shroomlightLocations.size()) {

                    if (habilidadesManager != null) {
                        habilidadesManager.disableHabilidades(p);
                    }
                    if (habilidadesEffects != null && habilidadesManager != null) {
                        habilidadesEffects.reapplyAllEffects(p, habilidadesManager);
                    }

                    if (eventInventoryManager != null) {
                        eventInventoryManager.saveAndClearInventory(p);
                    }

                    Location loc = shroomlightLocations.get(i).add(0.5, 1, 0.5);
                    String comando = String.format(Locale.US, "magictp %s %.2f %.2f %.2f", jugador, loc.getX(), loc.getY(), loc.getZ());
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), comando);
                    i++;
                }
            }
        }, delayTicks);
    }

    private List<Location> obtenerShroomlightLocations() {
        List<Location> locations = new ArrayList<>();
        for (int x = arenaMinX; x <= arenaMaxX; x++) {
            for (int y = arenaMinY; y <= arenaMaxY; y++) {
                for (int z = arenaMinZ; z <= arenaMaxZ; z++) {
                    Location loc = new Location(Bukkit.getWorld("world"), x, y, z);
                    if (loc.getBlock().getType() == Material.SHROOMLIGHT) {
                        locations.add(loc);
                    }
                }
            }
        }
        return locations;
    }

    private List<String> obtenerJugadoresOffline() {
        List<String> offline = new ArrayList<>();
        for (String nombre : participantes) {
            Player p = Bukkit.getPlayer(nombre);
            if (p == null || !p.isOnline()) {
                offline.add(nombre);
            }
        }
        return offline;
    }

    private void iniciarMonitorPausa() {
        pauseMonitorTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!eventoActivo) {
                    borrarTextoPausa();
                    this.cancel();
                    return;
                }

                List<String> offlinePlayers = obtenerJugadoresOffline();

                if (offlinePlayers.isEmpty()) {
                    borrarTextoPausa();
                    this.cancel();
                } else {
                    actualizarTextoPausa(offlinePlayers);
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    private void actualizarTextoPausa(List<String> offlinePlayers) {
        String nuevosNombres = String.join(" ", offlinePlayers);
        String newText = "&fSe ha sufrido una desconexion de los jugadores\n&#00FFFF" + nuevosNombres + "\n&fel evento ha sido pausado";

        if (!newText.equals(lastPauseText)) {
            for (Player p : getJugadoresEnZona()) {
                if (!lastPauseText.isEmpty()) {
                    ViciontMediaAPI.removeText(p, lastPauseText);
                }
                ViciontMediaAPI.sendText(p, "", 9999, "topleft", newText);
            }
            lastPauseText = newText;
        }
    }

    private void borrarTextoPausa() {
        if (!lastPauseText.isEmpty()) {
            for (Player p : getJugadoresEnZona()) {
                ViciontMediaAPI.removeText(p, lastPauseText);
            }
            lastPauseText = "";
        }
    }

    public void iniciarSecuenciaInicioSkyBattle() {
        if (!eventoActivo) return;

        List<String> offlinePlayers = obtenerJugadoresOffline();

        if (!offlinePlayers.isEmpty()) {
            if (pauseMonitorTask == null || pauseMonitorTask.isCancelled()) {
                iniciarMonitorPausa();
            } else {
                actualizarTextoPausa(offlinePlayers);
            }
            return;
        }

        if (pauseMonitorTask != null) pauseMonitorTask.cancel();
        borrarTextoPausa();

        // Evitar doble inicio
        if (secuenciaBatallaIniciada) return;
        secuenciaBatallaIniciada = true;

        ejecutarSecuenciaBatalla();
    }

    private void ejecutarSecuenciaBatalla() {
        cancelarTareasActivas();
        int duracionPrimerSonido = 390 * 20;

        getJugadoresEnZona().forEach(p -> {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "stopsound " + p.getName() + " * minecraft:custom.fondo_reglas");
        });

        BukkitTask tarea1 = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!eventoActivo) return;
            getJugadoresEnZona().forEach(p -> p.playSound(p.getLocation(), "minecraft:custom.music1_skybattle", SoundCategory.RECORDS, 0.7f, 1.0f));

            BukkitTask tarea2 = Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!eventoActivo) return;
                getJugadoresEnZona().forEach(p -> {
                    p.stopSound("minecraft:custom.music1_skybattle", SoundCategory.RECORDS);
                    p.playSound(p.getLocation(), "minecraft:custom.music2_skybattle", SoundCategory.RECORDS, 0.7f, 1.0f);
                });

                BukkitTask tarea3 = Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (!eventoActivo) return;
                    for (String nombreJugador : participantes) {
                        Player player = Bukkit.getPlayer(nombreJugador);
                        if (player != null) {
                            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 0, true, false, false));
                        }
                    }
                }, 180);
                tareasActivas.add(tarea3);
            }, duracionPrimerSonido + 20);
            tareasActivas.add(tarea2);
        }, 20);
        tareasActivas.add(tarea1);

        BukkitTask tareaCountdownAudio = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!eventoActivo) return;
            getJugadoresEnZona().forEach(p -> p.playSound(p.getLocation(), "minecraft:custom.lavaclash_countdown", SoundCategory.VOICE, 1.0f, 1.0f));
        }, 300L);
        tareasActivas.add(tareaCountdownAudio);

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
                        getJugadoresEnZona().forEach(p -> {
                            p.playSound(p.getLocation(), "minecraft:custom.321_fight_mortal_kombat", SoundCategory.RECORDS, 1.0f, 1.0f);
                        });
                    }
                    else if (contador == 2) { color = "gold"; }
                    else if (contador == 1) {
                        color = "red";
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "difficulty peaceful");
                    }

                    for (Player p : getJugadoresEnZona()) {
                        String jsonAB = "{\"text\":\"" + contador + "...\",\"bold\":true,\"color\":\"" + color + "\"}";
                        p.spigot().sendMessage(ChatMessageType.ACTION_BAR, ComponentSerializer.parse(jsonAB));
                    }
                    contador--;
                } else {
                    for (Player p : getJugadoresEnZona()) {
                        String jsonFight = "{\"text\":\"Fight!\",\"bold\":true,\"color\":\"#7E5FE6\"}";
                        p.spigot().sendMessage(ChatMessageType.ACTION_BAR, ComponentSerializer.parse(jsonFight));

                        p.playSound(p.getLocation(), "minecraft:custom.fight_box", SoundCategory.RECORDS, 1.0f, 1.0f);
                        if (discoCommand != null) {
                            discoCommand.startDisco(p);
                        }
                        ViciontMediaAPI.sendShaderApply(p, "wobbleslow");
                    }
                    this.cancel();

                    if (eventoActivo) {
                        iniciarSkyBattle();
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "difficulty hard");
                    }
                }
            }
        }.runTaskTimer(plugin, 350, 20);
    }

    public void iniciarSkyBattle() {
        this.eventoEnCurso = true;

        int colorIndex = 0;
        for (String jugador : participantes) {
            Player p = Bukkit.getPlayer(jugador);
            if (p != null) {
                darKitBatalla(p, colorIndex);
                colorIndex++;
            }
        }

        inicializarScoreboard();

        ubicacionesShroomlightOriginales = obtenerShroomlightLocations();
        for (Location loc : ubicacionesShroomlightOriginales) {
            loc.getBlock().setType(Material.AIR);
        }

        iniciarReduccionBorde();
        aplicarDanioFueraDelBorde();
    }

    private void darKitBatalla(Player p, int colorIndex) {
        p.getInventory().clear();
        p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 200, 4));
        p.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 0));

        // Paleta de 20 colores distintos para diferenciar a cada jugador
        Color[] COLORES_ARMADURA = {
                Color.fromRGB(255, 0, 0), Color.fromRGB(0, 0, 255), Color.fromRGB(0, 255, 0),
                Color.fromRGB(255, 255, 0), Color.fromRGB(255, 128, 0), Color.fromRGB(128, 0, 128),
                Color.fromRGB(255, 192, 203), Color.fromRGB(0, 255, 255), Color.fromRGB(0, 255, 128),
                Color.fromRGB(255, 0, 255), Color.fromRGB(0, 128, 128), Color.fromRGB(0, 0, 128),
                Color.fromRGB(128, 0, 0), Color.fromRGB(128, 128, 0), Color.fromRGB(255, 255, 255),
                Color.fromRGB(128, 128, 128), Color.fromRGB(0, 0, 0), Color.fromRGB(192, 192, 192),
                Color.fromRGB(255, 215, 0), Color.fromRGB(75, 0, 130)
        };

        Color colorArmadura = COLORES_ARMADURA[colorIndex % COLORES_ARMADURA.length];

        ItemStack pechera = new ItemStack(Material.LEATHER_CHESTPLATE);
        ItemStack pantalones = new ItemStack(Material.LEATHER_LEGGINGS);
        ItemStack botas = new ItemStack(Material.LEATHER_BOOTS);

        ItemStack[] armadura = {pechera, pantalones, botas};
        for (ItemStack pieza : armadura) {
            org.bukkit.inventory.meta.LeatherArmorMeta meta = (org.bukkit.inventory.meta.LeatherArmorMeta) pieza.getItemMeta();
            if (meta != null) {
                meta.setColor(colorArmadura);
                meta.addEnchant(Enchantment.PROTECTION, 1, true);
                pieza.setItemMeta(meta);
            }
        }

        p.getInventory().setChestplate(pechera);
        p.getInventory().setLeggings(pantalones);
        p.getInventory().setBoots(botas);

        p.getInventory().addItem(new ItemStack(Material.STONE_SWORD));
        p.getInventory().addItem(new ItemStack(Material.COOKED_BEEF, 32));
        p.getInventory().addItem(new ItemStack(Material.IRON_PICKAXE));
        p.getInventory().addItem(new ItemStack(Material.PURPLE_CONCRETE, 64));
    }

    private void inicializarScoreboard() {
        if (eventoScoreboard == null) {
            eventoScoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        }

        Scoreboard mainBoard = Bukkit.getScoreboardManager().getMainScoreboard();
        for (org.bukkit.scoreboard.Team mainTeam : mainBoard.getTeams()) {
            if (eventoScoreboard.getTeam(mainTeam.getName()) == null) {
                org.bukkit.scoreboard.Team t = eventoScoreboard.registerNewTeam(mainTeam.getName());
                t.setPrefix(mainTeam.getPrefix());
                t.setSuffix(mainTeam.getSuffix());
                t.setColor(mainTeam.getColor());
            }
        }

        TeamType lc = TeamType.LAVACLASH;
        org.bukkit.scoreboard.Team teamLC = eventoScoreboard.getTeam(lc.getId());
        if (teamLC == null) teamLC = eventoScoreboard.registerNewTeam(lc.getId());
        teamLC.setPrefix(lc.getChatPrefix());
        teamLC.setColor(lc.getBukkitColor());

        for (String pName : participantes) {
            if (Bukkit.getPlayer(pName) != null) {
                teamLC.addEntry(pName);
            }
        }

        Objective objective = eventoScoreboard.getObjective("skybattle");
        if (objective == null) {
            objective = eventoScoreboard.registerNewObjective("skybattle", "dummy", "\u3201\uE080\u3201\u3201 ");
            objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        }

        crearTeamScoreboard("sb_players", "  §5\uD83D\uDDE1§6§l Players Left§r§l: ", "§a§l" + participantes.size(), 3);
        crearTeamScoreboard("sb_borde", "  §e⚠§c§l Borde§r§l: ", "§7§l" + obtenerTiempoBorde(), 1);

        objective.getScore("§r  ").setScore(9);
        objective.getScore("        \uE078").setScore(8);
        objective.getScore("§r   ").setScore(7);
        objective.getScore("§r     ").setScore(6);
        objective.getScore("§r      ").setScore(5);
        objective.getScore("§r       ").setScore(4);
        objective.getScore("§r          ").setScore(2);
        objective.getScore("§r           ").setScore(0);

        for (Player player : getJugadoresEnZona()) {
            player.setScoreboard(eventoScoreboard);
        }
    }

    private void crearTeamScoreboard(String teamName, String prefix, String suffix, int score) {
        Objective obj = eventoScoreboard.getObjective("skybattle");
        org.bukkit.scoreboard.Team team = eventoScoreboard.getTeam(teamName);
        if (team == null) team = eventoScoreboard.registerNewTeam(teamName);

        String entry = ChatColor.values()[score].toString();
        team.addEntry(entry);
        team.setPrefix(prefix);
        team.setSuffix(suffix);
        obj.getScore(entry).setScore(score);
    }

    private void actualizarScoreboard() {
        if (eventoScoreboard == null) return;

        org.bukkit.scoreboard.Team tPlayers = eventoScoreboard.getTeam("sb_players");
        if (tPlayers != null) {
            tPlayers.setSuffix("§a§l" + participantes.size());
        }

        org.bukkit.scoreboard.Team tBorde = eventoScoreboard.getTeam("sb_borde");
        if (tBorde != null) {
            String tiempo = bordeReduciendose ? "§4§l> > >" : (tiempoBorde == -1 ? "§7§l00:00" : "§7§l" + obtenerTiempoBorde());
            tBorde.setSuffix(tiempo);
        }

        for (Player p : getJugadoresEnZona()) {
            if (p.getScoreboard() != eventoScoreboard) {
                p.setScoreboard(eventoScoreboard);
            }
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
        List<Player> espectadores = getJugadoresEnZona();
        if (espectadores.isEmpty()) return;

        Particle particula = Particle.ANGRY_VILLAGER;
        int stepY = 3, stepXZ = 3;

        for (int y = minY; y <= maxY; y += stepY) {
            for (int z = minZ; z <= maxZ; z += stepXZ) {
                spawnParticleIfClose(espectadores, particula, minX, y, z);
                spawnParticleIfClose(espectadores, particula, maxX, y, z);
            }
            for (int x = minX; x <= maxX; x += stepXZ) {
                spawnParticleIfClose(espectadores, particula, x, y, minZ);
                spawnParticleIfClose(espectadores, particula, x, y, maxZ);
            }
        }
    }

    private void spawnParticleIfClose(List<Player> players, Particle p, int x, int y, int z) {
        if (x == 20000 && z == 20000) return;

        Location loc = new Location(Bukkit.getWorld("world"), x + 0.5, y + 0.5, z + 0.5);
        for (Player player : players) {
            if (player.getLocation().distanceSquared(loc) < 1600) {
                player.spawnParticle(p, loc, 1, 0, 0, 0, 0);
            }
        }
    }

    private void mostrarBordeParticulasContinuo() {
        taskBordeParticulas = new BukkitRunnable() {
            @Override
            public void run() {
                if (!eventoActivo) {
                    this.cancel();
                    return;
                }
                mostrarBordeParticulas(minX, maxX, minZ, maxZ);
            }
        };
        taskBordeParticulas.runTaskTimer(plugin, 0L, 20L);
    }

    private void iniciarReduccionBorde() {
        taskReduccionBorde = new BukkitRunnable() {
            @Override
            public void run() {
                if (participantes.size() <= 1 || !eventoActivo) {
                    this.cancel();
                    return;
                }

                if (tiempoBorde <= 0 && !bordeReduciendose) {
                    bordeReduciendose = true;

                    if (minX >= 19999 && maxX <= 20001 && minZ >= 19999 && maxZ <= 20001) {
                        minX = 19999;
                        maxX = 20001;
                        minZ = 19999;
                        maxZ = 20001;
                        tiempoBorde = -1;

                        enviarMensajeZona("§§§e§l۞§6§l El borde ha llegado a su límite.§r§§ ");
                        for(Player p : getJugadoresEnZona()) {
                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "title " + p.getName() + " title {\"text\":\" \",\"bold\":true,\"color\":\"red\"}");
                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "title " + p.getName() + " subtitle [\"\",{\"text\":\"\\u26a0\",\"bold\":true,\"color\":\"yellow\"},{\"text\":\" El borde ya no se moverá.\",\"bold\":true,\"color\":\"gold\"},{\"text\":\" \\u26a0\",\"bold\":true,\"color\":\"yellow\"}]");
                            p.playSound(p.getLocation(), "block.note_block.bell", 1, 0.1f);
                        }

                        bordeReduciendose = false;
                        actualizarScoreboard();
                        this.cancel();
                        return;
                    }

                    new BukkitRunnable() {
                        int countdown = 3;

                        @Override
                        public void run() {
                            if (countdown == 0) {
                                enviarMensajeZona("§§§c§l۞§4§l El borde ha comenzado a reducirse!!§r§§ ");
                                for(Player p : getJugadoresEnZona()) {
                                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "title " + p.getName() + " title {\"text\":\" \",\"bold\":true,\"color\":\"red\"}");
                                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "title " + p.getName() + " subtitle [\"\",{\"text\":\"\\u26a0\",\"bold\":true,\"color\":\"red\"},{\"text\":\" Borde reduciéndose. \",\"bold\":true,\"color\":\"dark_red\"},{\"text\":\"\\u26a0\",\"bold\":true,\"color\":\"red\"}]");
                                    p.playSound(p.getLocation(), "block.note_block.bell", 1, 2f);
                                }

                                iniciarReduccionBordeContinuo();
                                this.cancel();
                            } else {
                                String color = switch (countdown) {
                                    case 3 -> "§e§l";
                                    case 2 -> "§6§l";
                                    case 1 -> "§c§l";
                                    default -> "§f§l";
                                };

                                enviarMensajeZona("§c۞§7§l El §4§lborde§7§l se reducirá en " + color + countdown);
                                getJugadoresEnZona().forEach(p -> p.playSound(p.getLocation(), "block.note_block.bell", 1, 1.5f));
                                countdown--;
                            }
                        }
                    }.runTaskTimer(plugin, 0L, 20L);
                }

                actualizarScoreboard();
                tiempoBorde--;
            }
        };

        taskReduccionBorde.runTaskTimer(plugin, 0L, 15L);
        mostrarBordeParticulasContinuo();
    }

    private void iniciarReduccionBordeContinuo() {
        taskReduccionBordeContinuo = new BukkitRunnable() {
            int steps = 10;
            int stepSize = 2;

            @Override
            public void run() {
                if (steps <= 0) {
                    tiempoBorde = tiempoBordeInicial;
                    bordeReduciendose = false;

                    enviarMensajeZona("§§§6§l۞ §c§lEl borde se ha detenido. §r§l§§§7§lTiempo reiniciado a§6§l " + (tiempoBordeInicial / 60) + " minutos");
                    for(Player p : getJugadoresEnZona()) {
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "title " + p.getName() + " title {\"text\":\" \",\"bold\":true,\"color\":\"red\"}");
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "title " + p.getName() + " subtitle [\"\",{\"text\":\"\\u26a0\",\"bold\":true,\"color\":\"gold\"},{\"text\":\" Borde Detenido.\",\"bold\":true,\"color\":\"red\"},{\"text\":\" \\u26a0\",\"bold\":true,\"color\":\"gold\"}]");
                        p.playSound(p.getLocation(), "block.note_block.bell", 1, 0.1f);
                    }

                    actualizarScoreboard();
                    this.cancel();
                } else {
                    minX = Math.min(minX + stepSize, 19999);
                    maxX = Math.max(maxX - stepSize, 20001);
                    minZ = Math.min(minZ + stepSize, 19999);
                    maxZ = Math.max(maxZ - stepSize, 20001);

                    mostrarBordeParticulas(minX, maxX, minZ, maxZ);
                    steps--;
                }
            }
        };

        taskReduccionBordeContinuo.runTaskTimer(plugin, 0L, 20L);
    }

    private void procesarEliminacionJugador(Player jugador, Player atacante, String eliminado, String asesino) {
        jugador.setHealth(jugador.getMaxHealth());

        jugador.getInventory().forEach(item -> {
            if (item != null) {
                jugador.getWorld().dropItemNaturally(jugador.getLocation(), item);
            }
        });
        jugador.getInventory().clear();

        if (eventInventoryManager != null) {
            eventInventoryManager.restoreInventory(jugador);
        }

        habilidadesManager.enableHabilidades(jugador);
        habilidadesEffects.reapplyAllEffects(jugador, habilidadesManager);

        if (!ordenEliminados.contains(eliminado)) {
            ordenEliminados.add(eliminado);
        }

        if (asesino != null && !asesino.isEmpty()) {
            kills.put(asesino, kills.getOrDefault(asesino, 0) + 1);
        }

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

        enviarMensajeZona(mensaje);
        getJugadoresEnZona().forEach(player -> {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 2.0f, 0.5f);
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 2.0f, 0.1f);
        });

        jugador.teleport(zonaEspectadores);

        participantes.remove(eliminado);
        actualizarScoreboard();

        if (participantes.size() == 1) {
            declararGanador();
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (!eventoActivo) return;

        Player jugador = event.getPlayer();
        String nombreJugador = jugador.getName();

        if (participantes.contains(nombreJugador)) {
            if (this.eventoEnCurso) {
                procesarEliminacionJugador(jugador, null, nombreJugador, "");
                enviarMensajeZona("§8§l[§c§l☠§8§l]§6§l " + nombreJugador + " §r§7ha abandonado el evento");
            }
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!eventoActivo || !(event.getEntity() instanceof Player)) return;

        Player jugador = (Player) event.getEntity();

        if (participantes.contains(jugador.getName())) {
            if (eventoEnCurso) {
                double nuevaVida = jugador.getHealth() - event.getFinalDamage();
                if (nuevaVida <= 0.3) {
                    event.setCancelled(true);
                    Player atacante = getAtacante(jugador);
                    procesarEliminacionJugador(jugador, atacante, jugador.getName(),
                            atacante != null ? atacante.getName() : "");
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

    public void mostrarTopJugadores() {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            StringBuilder mensaje = new StringBuilder();

            mensaje.append("\n" + ChatColor.DARK_PURPLE + "" + ChatColor.STRIKETHROUGH + "                                             " + ChatColor.RESET + "\n");
            mensaje.append("         " + ChatColor.GOLD + ChatColor.BOLD + "Top Jugadores\n\n");
            List<String> topJugadores = new ArrayList<>(ordenEliminados);
            Collections.reverse(topJugadores);

            for (String superviviente : participantes) {
                if (!topJugadores.contains(superviviente)) {
                    topJugadores.add(0, superviviente);
                }
            }

            String[] colores = {
                    ChatColor.of("#ffbf00").toString(),
                    ChatColor.of("#e3e4e5").toString(),
                    ChatColor.of("#cd7f32").toString(),
                    ChatColor.of("#00aaaa").toString(),
                    ChatColor.of("#00aaaa").toString()
            };

            for (int i = 0; i < Math.min(5, topJugadores.size()); i++) {
                String jugador = topJugadores.get(i);
                int killsJugador = kills.getOrDefault(jugador, 0);
                String colorNombre = colores[Math.min(i, colores.length - 1)];

                if (i == 3) {
                    mensaje.append("\n");
                }

                mensaje.append(String.format(
                        ChatColor.GRAY + "" + ChatColor.BOLD + "%d. " +
                                colorNombre + "%s " +
                                ChatColor.DARK_GRAY + ChatColor.BOLD + "(" +
                                ChatColor.RED + "" + ChatColor.BOLD + "%d" +
                                ChatColor.DARK_GRAY + ChatColor.BOLD + ")\n",
                        i + 1, jugador, killsJugador
                ));
            }

            mensaje.append(ChatColor.DARK_PURPLE + "" + ChatColor.STRIKETHROUGH + "                                             " + ChatColor.RESET + "\n");

            enviarMensajeZona(mensaje.toString());

        }, 100L);
    }

    private void declararGanador() {
        for (String jugador : participantes) {
            Player ganador = Bukkit.getPlayer(jugador);
            if (ganador != null) {
                cancelarEfectosBorde();
                reiniciarVariablesBorde();
                enviarMensajeZona("§d۞§6§l " + ganador.getName() + " §f§lha ganado el evento §e§lLavaClash!§7§l.");

                getJugadoresEnZona().forEach(p -> {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "stopsound " + p.getName() + " record minecraft:custom.music1_skybattle");
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "stopsound " + p.getName() + " record minecraft:custom.music2_skybattle");
                    if (discoCommand != null) {
                        discoCommand.stopDisco(p);
                    }
                    ViciontMediaAPI.sendShaderRemove(p, "wobbleslow");
                });

                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "tick rate 20");
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "effect clear @a minecraft:speed");
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "effect clear @a minecraft:night_vision");

                habilidadesManager.enableHabilidades(ganador);
                habilidadesEffects.reapplyAllEffects(ganador, habilidadesManager);

                ganador.teleport(zonaEspectadores);
                ganador.getInventory().clear();

                if (eventInventoryManager != null) {
                    eventInventoryManager.restoreInventory(ganador);
                }

                for (Player p : getJugadoresEnZona()) {
                    p.sendTitle(
                            "\uE082 ",
                            "§8§l>§6§l " + ganador.getName() + "§8§l <",
                            15, 200, 15
                    );

                    p.playSound(p.getLocation(), "minecraft:ui.toast.challenge_complete", SoundCategory.RECORDS, 5, 1);
                    p.playSound(p.getLocation(), "minecraft:custom.win_box", SoundCategory.RECORDS, 2, 1);
                }

                Bukkit.getScheduler().runTaskLater(plugin, this::mostrarTopJugadores, 100L);
                Bukkit.getScheduler().runTaskLater(plugin, this::terminarEvento, 300L);
                break;
            }
        }
    }

    private void restaurarShroomlights() {
        for (Location loc : ubicacionesShroomlightOriginales) {
            loc.getBlock().setType(Material.SHROOMLIGHT);
        }
    }

    public void eliminarPurpleConcrete() {
        for (int x = arenaMinX; x <= arenaMaxX; x++) {
            for (int y = arenaMinY; y <= arenaMaxY; y++) {
                for (int z = arenaMinZ; z <= arenaMaxZ; z++) {
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
                if (!eventoActivo || !eventoEnCurso) {
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
                                procesarEliminacionJugador(p, null, p.getName(), "");
                            } else {
                                p.damage(1.0);
                            }
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    private void guardarEstadoEvento() {
        FileConfiguration configState = YamlConfiguration.loadConfiguration(estadoArchivo);
        configState.set("eventoActivo", eventoActivo);
        try {
            configState.save(estadoArchivo);
        } catch (IOException e) {
            plugin.getLogger().severe("No se pudo guardar el estado del evento: " + e.getMessage());
        }
    }

    private void verificarEstadoEvento() {
        if (!estadoArchivo.exists()) return;

        FileConfiguration configState = YamlConfiguration.loadConfiguration(estadoArchivo);
        boolean estadoGuardado = configState.getBoolean("eventoActivo", false);

        if (estadoGuardado) {
            plugin.getLogger().warning("El evento estaba activo antes del reinicio. Finalizando automáticamente...");
            terminarEvento();
        }
    }

    @EventHandler
    public void onBlockEvent(BlockBreakEvent event) {
        Block block = event.getBlock();
        Player player = event.getPlayer();

        if (player.getGameMode() == GameMode.CREATIVE) {
            return;
        }

        if (isInsideArenaGeneral(block.getLocation())) {
            if (block.getType() == Material.PURPLE_CONCRETE || block.getType() == Material.COBWEB || block.getType() == Material.SCAFFOLDING) {
                event.setDropItems(false);
            } else {
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

        if (event.getBlockReplacedState().getType() == Material.LAVA) {
            org.bukkit.block.data.Levelled lava = (org.bukkit.block.data.Levelled) event.getBlockReplacedState().getBlockData();
            if (lava.getLevel() == 0) { // Nivel 0 significa que es el bloque fuente completo
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "No puedes colocar bloques sobre la lava.");
                return;
            }
        }

        if (!eventoActivo) {
            if (isInsideArenaGeneral(block.getLocation())) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "No puedes colocar este bloque aquí.");
            }
            return;
        }

        if (isInsideArenaGeneral(block.getLocation())) {
            if (block.getType() == Material.PURPLE_CONCRETE || block.getType() == Material.COBWEB || block.getType() == Material.SCAFFOLDING) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    EquipmentSlot hand = event.getHand();
                    if (hand == EquipmentSlot.HAND) {
                        player.getInventory().addItem(new ItemStack(Material.PURPLE_CONCRETE, 1));
                    } else if (hand == EquipmentSlot.OFF_HAND) {
                        ItemStack offHandItem = player.getInventory().getItemInOffHand();
                        if (offHandItem.getType() == Material.PURPLE_CONCRETE) {
                            offHandItem.setAmount(offHandItem.getAmount() + 1);
                            player.getInventory().setItemInOffHand(offHandItem);
                        } else {
                            player.getInventory().setItemInOffHand(new ItemStack(Material.PURPLE_CONCRETE, 1));
                        }
                    }
                });
            } else if (block.getType() == Material.TNT) {
                TNTPrimed tnt = (TNTPrimed) block.getWorld().spawn(block.getLocation(), TNTPrimed.class);
                tnt.setFuseTicks(60);
                block.setType(Material.AIR);
            } else {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "No puedes colocar este bloque aquí.");
            }
        }
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        if (isInsideArenaGeneral(event.getLocation())) {
            event.blockList().removeIf(block -> block.getType() != Material.PURPLE_CONCRETE);
        }
    }

    @EventHandler
    public void onEntitySpawn(EntitySpawnEvent event) {
        if (eventoActivo && isInsideArenaGeneral(event.getLocation())) {
            if (event.getEntity() instanceof Monster) {
                if (event instanceof CreatureSpawnEvent creatureEvent) {
                    if (creatureEvent.getSpawnReason() == CreatureSpawnEvent.SpawnReason.SPAWNER_EGG) {
                        Player player = creatureEvent.getEntity().getWorld().getNearbyEntities(event.getLocation(), 1, 1, 1).stream()
                                .filter(e -> e instanceof Player)
                                .map(e -> (Player) e)
                                .findFirst()
                                .orElse(null);

                        if (player != null) {
                            Monster mob = (Monster) event.getEntity();
                            mob.setPersistent(true);
                            mob.setTarget(null);
                            mob.setAware(true);
                            mob.addScoreboardTag("owner:" + player.getUniqueId());
                        }
                    } else {
                        event.setCancelled(true);
                    }
                } else {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        if (eventoActivo) {
            for (Entity entity : event.getChunk().getEntities()) {
                if (entity instanceof Monster mob && isInsideArenaGeneral(entity.getLocation())) {
                    if (!mob.getScoreboardTags().stream().anyMatch(tag -> tag.startsWith("owner:"))) {
                        mob.remove();
                    }
                }
            }
        }
    }

    @EventHandler
    public void onEntityTarget(EntityTargetLivingEntityEvent event) {
        if (eventoActivo && event.getEntity() instanceof Monster mob && event.getTarget() instanceof Player player) {
            if (mob.getScoreboardTags().contains("owner:" + player.getUniqueId())) {
                event.setCancelled(true);
            }
        }
    }

    public void eliminarMobsExistentes() {
        if (!eventoActivo) return;
        World world = Bukkit.getWorld("world");
        if (world == null) return;

        org.bukkit.util.BoundingBox arenaBox = new org.bukkit.util.BoundingBox(arenaMinX, arenaMinY, arenaMinZ, arenaMaxX, arenaMaxY, arenaMaxZ);

        for (Entity entity : world.getNearbyEntities(arenaBox)) {
            if (entity instanceof Monster mob) {
                if (!mob.getScoreboardTags().stream().anyMatch(tag -> tag.startsWith("owner:"))) {
                    mob.remove();
                }
            }
        }
    }

    public void espera1() {
        for (Player p : getJugadoresEnZona()) {
            p.playSound(p, "minecraft:custom.fondo_reglas", SoundCategory.RECORDS, 1.0f, 1.0f);
        }

        BukkitTask t1 = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (Player p : getJugadoresEnZona()) {
                ViciontCinematicAPI.sendPlay(p, "lavaclash", "LINEAR", true, 1);
            }
        }, 200L);
        tareasActivas.add(t1);

        BukkitTask t2 = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            getJugadoresEnZona().forEach(p -> {
                p.playSound(p, "minecraft:custom.evento_lavaclash_audio", SoundCategory.MASTER, 1.0f, 1.0f);
            });
        }, 260L);
        tareasActivas.add(t2);

        sendPersistentActionBar("{\"text\":\"Bienvenidos a todos al primer\\nevento. Este evento se llama.\\n\\\"LavaClash\\\"\"}", 260L, 120L);

        BukkitTask t3 = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            getJugadoresEnZona().forEach(p -> p.sendTitle("\uE086", "", 20, 40, 20));
        }, 350L);
        tareasActivas.add(t3);

        sendPersistentActionBar("{\"text\":\"En este evento deberán\\nlootear los cofres y\\nderrotar a sus oponentes.\"}", 380L, 80L);
        sendPersistentActionBar("{\"text\":\"Cada 2 minutos, el borde se\\nirá cerrando hacia el centro.\"}", 460L, 80L);
        sendPersistentActionBar("{\"text\":\"Todos los jugadores poseen bloques\\nque se pueden colocar infinitamente,\\nya sea para cubrirse o escapar.\"}", 540L, 160L);
        sendPersistentActionBar("{\"text\":\"Los últimos 2 jugadores que\\nqueden vivos tendrán recompensa,\\ny el primer lugar obtendrá una\\norbe de resurrección.\"}", 700L, 160L);
        sendPersistentActionBar("{\"text\":\"Buena Suerte!\"}", 860L, 40L);

        BukkitTask t9 = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            TiempoCommand tiempoCommand = (TiempoCommand) plugin.getCommand("timers").getExecutor();

            int m = tiempoReglasSegundos / 60;
            int s = tiempoReglasSegundos % 60;
            String timeFormat = String.format("%02d:%02d:%02d", 0, m, s);

            for (Player p : getJugadoresEnZona()) {
                ViciontCinematicAPI.sendPlay(p, "lavaclash_bucle", "SMOOTH", false, -1);
                ViciontMediaAPI.sendMedia(p, "instrucciones_lavaclash.png", null, -1L, 0, "center", 100, false, true);

                if (tiempoCommand != null) {
                    tiempoCommand.createPlayerBossBar(p, "sinNombre", tiempoReglasSegundos, timeFormat, "on");
                }
            }
        }, 892L);
        tareasActivas.add(t9);

        long endTicks = 892L + (tiempoReglasSegundos * 20L);
        BukkitTask t10 = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (Player p : getJugadoresEnZona()) {
                ViciontCinematicAPI.sendStop(p, "lavaclash_bucle");
                ViciontMediaAPI.stopSpecific(p, "instrucciones_lavaclash.png");
            }
        }, endTicks);
        tareasActivas.add(t10);
    }

    private void sendPersistentActionBar(String jsonText, long delayTicks, long durationTicks) {
        BukkitTask task = new BukkitRunnable() {
            long elapsed = 0;

            @Override
            public void run() {
                if (elapsed >= durationTicks) {
                    this.cancel();
                    return;
                }

                getJugadoresEnZona().forEach(p -> {
                    p.spigot().sendMessage(ChatMessageType.ACTION_BAR, ComponentSerializer.parse(jsonText));
                });

                elapsed += 20;
            }
        }.runTaskTimer(plugin, delayTicks, 20L);

        tareasActivas.add(task);
    }

    public void guardarContenidoCofres() {
        cofresHandler.guardarContenidoCofres(arenaMinX, arenaMaxX, arenaMinZ, arenaMaxZ);
    }

    public void restaurarContenidoCofres() {
        cofresHandler.restaurarContenidoCofres();
    }

    public void cargarCofresDesdeArchivo() {
        cofresHandler.cargarCofresDesdeArchivo();
    }

    private void guardarDatosFinales() {
        File archivo = new File(plugin.getDataFolder(), "resultados.yml");
        FileConfiguration resultsConfig = YamlConfiguration.loadConfiguration(archivo);

        resultsConfig.set("orden_eliminados", ordenEliminados);
        for (Map.Entry<String, Integer> entry : kills.entrySet()) {
            resultsConfig.set("kills." + entry.getKey(), entry.getValue());
        }

        try {
            resultsConfig.save(archivo);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void cancelarTareasActivas() {
        for (BukkitTask tarea : tareasActivas) {
            if (tarea != null && !tarea.isCancelled()) {
                tarea.cancel();
            }
        }
        tareasActivas.clear();
    }

    private void cancelarEfectosBorde() {
        getJugadoresEnZona().forEach(p -> {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "title " + p.getName() + " clear");
            p.stopSound("block.note_block.bell");
            if (discoCommand != null) {
                discoCommand.stopDisco(p);
            }
            ViciontMediaAPI.sendShaderRemove(p, "wobbleslow");
        });

        if (taskBordeParticulas != null) {
            taskBordeParticulas.cancel();
            taskBordeParticulas = null;
        }

        if (taskReduccionBorde != null) {
            taskReduccionBorde.cancel();
            taskReduccionBorde = null;
        }

        if (taskReduccionBordeContinuo != null) {
            taskReduccionBordeContinuo.cancel();
            taskReduccionBordeContinuo = null;
        }
    }

    public void gestionarParticipantes(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "Uso: /lavaclash <list|add|remove> [jugador]");
            return;
        }

        switch (args[0].toLowerCase()) {
            case "list":
                listarParticipantes(sender);
                break;
            case "add":
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Uso: /lavaclash add <jugador>");
                    return;
                }
                agregarParticipante(sender, args[1]);
                break;
            case "remove":
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Uso: /lavaclash remove <jugador>");
                    return;
                }
                removerParticipante(sender, args[1]);
                break;
            default:
                sender.sendMessage(ChatColor.RED + "Subcomando no válido. Usa list, add o remove");
        }
    }

    private void listarParticipantes(CommandSender sender) {
        StringBuilder mensaje = new StringBuilder();
        mensaje.append(ChatColor.GOLD).append("=== Participantes (").append(participantes.size()).append("/").append(MAX_PARTICIPANTES).append(") ===\n");

        for (String nombre : participantes) {
            Player p = Bukkit.getPlayer(nombre);
            String estado = (p != null && p.isOnline()) ? ChatColor.GREEN + "Online" : ChatColor.RED + "Offline";
            mensaje.append(ChatColor.YELLOW).append("- ").append(nombre).append(": ").append(estado).append("\n");
        }

        sender.sendMessage(mensaje.toString());
    }

    private void agregarParticipante(CommandSender sender, String nombreJugador) {
        if (participantes.contains(nombreJugador)) {
            sender.sendMessage(ChatColor.RED + "El jugador ya está en la lista de participantes");
            return;
        }

        if (participantes.size() >= MAX_PARTICIPANTES) {
            sender.sendMessage(ChatColor.RED + "No hay espacio para más participantes");
            return;
        }

        participantes.add(nombreJugador);
        Player p = Bukkit.getPlayer(nombreJugador);
        if (p != null) {
            aplicarTeamLavaClash(p);

            if (eventoEnCurso || preparacion) {
                habilidadesManager.disableHabilidades(p);
                habilidadesEffects.reapplyAllEffects(p, habilidadesManager);

                if (eventInventoryManager != null) {
                    eventInventoryManager.saveAndClearInventory(p);
                }

                if (eventoEnCurso) {
                    darKitBatalla(p, participantes.size());
                }
            }
        }
        sender.sendMessage(ChatColor.GREEN + "Jugador " + nombreJugador + " agregado al evento");

        String jsonMessage = "[\"\","
                + "{\"text\":\"\u06de\",\"color\":\"#BA7FD0\"},"
                + "{\"text\":\" " + nombreJugador + "\",\"bold\":true,\"color\":\"#863ECF\"},"
                + "{\"text\":\" ha obtenido el \",\"color\":\"#BA7FD0\"},"
                + "{\"text\":\"Manu ticket\",\"bold\":true,\"color\":\"#E9BF66\"},"
                + "{\"text\":\" - Ticket:\",\"color\":\"#BA7FD0\"},"
                + "{\"text\":\" " + participantes.size() + "\",\"bold\":true,\"color\":\"#863ECF\"},"
                + "{\"text\":\"/\",\"bold\":true,\"color\":\"#BA7FD0\"},"
                + "{\"text\":\"" + MAX_PARTICIPANTES + "\",\"bold\":true,\"color\":\"#863ECF\"},"
                + "{\"text\":\"\\n \"}"
                + "]";

        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "tellraw @a " + jsonMessage);
    }

    private void removerParticipante(CommandSender sender, String nombreJugador) {
        if (!participantes.contains(nombreJugador)) {
            sender.sendMessage(ChatColor.RED + "El jugador no está en la lista de participantes");
            return;
        }

        participantes.remove(nombreJugador);
        restaurarTeamOriginal(nombreJugador);
        Player p = Bukkit.getPlayer(nombreJugador);
        if (p != null) {
            p.getInventory().clear();
            if (eventInventoryManager != null) {
                eventInventoryManager.restoreInventory(p);
            }

            habilidadesManager.enableHabilidades(p);
            habilidadesEffects.reapplyAllEffects(p, habilidadesManager);
        }
        sender.sendMessage(ChatColor.GREEN + "Jugador " + nombreJugador + " eliminado del evento");
    }
}