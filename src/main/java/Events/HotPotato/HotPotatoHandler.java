package Events.HotPotato;

import Commands.TiempoCommand;
import Habilidades.HabilidadesEffects;
import Habilidades.HabilidadesManager;
import Handlers.EventInventoryManager;
import TitleListener.RuletaAnimation;
import net.md_5.bungee.api.ChatColor;
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
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class HotPotatoHandler implements Listener {

    private final JavaPlugin plugin;
    private final HabilidadesManager habilidadesManager;
    private final HabilidadesEffects habilidadesEffects;
    private final RuletaAnimation eventoAnimation;
    private EventInventoryManager eventInventoryManager;
    private final List<String> participantes = new ArrayList<>();
    private final List<String> vivos = new ArrayList<>();
    private final List<String> bombasActuales = new ArrayList<>();
    private final Map<String, Integer> vecesConBomba = new HashMap<>();
    private final Map<String, String> poderActual = new HashMap<>();
    private final Map<String, Integer> tiempoPoder = new HashMap<>();
    private final Map<String, Integer> cargasRelentizadora = new HashMap<>();
    private final List<Entity> poderesEnSuelo = new ArrayList<>();
    private final List<BukkitTask> tareasActivas = new ArrayList<>();

    private final Map<String, Long> cooldownCactus = new HashMap<>();

    private boolean eventoIniciado = false;
    private boolean enBatalla = false;
    private boolean tpRealizado = false;
    private boolean rondaEnPausa = false; // NUEVO: Evita que se pasen la papa mientras explotan

    private int arenaMinX, arenaMaxX;
    private int arenaMinY, arenaMaxY;
    private int arenaMinZ, arenaMaxZ;

    private int minX, maxX;
    private int minY, maxY;
    private int minZ, maxZ;
    private Location zonaEspectadores;

    private int rondaActual = 0;
    private int totalRondasCalculadas = 0;
    private int rondaInicioBorde = 5;
    private List<Integer> secuenciaBombas = new ArrayList<>();
    private BukkitTask timerRonda;
    private BukkitTask taskActionBar;
    private BukkitTask taskRotacionPoderes;
    private BukkitTask taskSaturacionDanio;

    private int tiempoBorde = 120;
    private boolean bordeReduciendose = false;
    private BukkitTask taskBordeParticulas;
    private BukkitTask taskReduccionBorde;
    private BukkitTask taskReduccionBordeContinuo;

    private Map<String, Scoreboard> playerScoreboards = new HashMap<>();

    private final TiempoCommand tiempoCommand;
    private final String EVENT_TIMER_ID = "hotpotatoGlobalTimer";

    private final List<String> cancionesDisponibles = new ArrayList<>();
    private int indexCancion = 0;

    private final Map<String, String> originalTeams = new HashMap<>();

    private File configFile;
    private FileConfiguration config;
    private int tiempoRondaSegundos = 120;
    private String velocidadRondas = "rapida";
    private boolean poderesActivados = true;
    private String timerStart = "00:04:00";

    public HotPotatoHandler(JavaPlugin plugin, TiempoCommand tiempoCommand, HabilidadesManager habilidadesManager, HabilidadesEffects habilidadesEffects) {
        this.plugin = plugin;
        this.tiempoCommand = tiempoCommand;
        this.habilidadesManager = habilidadesManager;
        this.habilidadesEffects = habilidadesEffects;
        this.eventoAnimation = new RuletaAnimation(plugin);

        crearYcargarConfig();

        cancionesDisponibles.addAll(Arrays.asList(
                "minecraft:music_disc.creator", "minecraft:music_disc.precipice",
                "minecraft:music_disc.mellohi", "minecraft:music_disc.stal",
                "minecraft:music_disc.tears", "minecraft:music_disc.wait",
                "minecraft:music_disc.otherside", "minecraft:music_disc.pigstep",
                "minecraft:music_disc.relic", "minecraft:music_disc.lava_chicken"
        ));
    }

    public void crearYcargarConfig() {
        configFile = new File(plugin.getDataFolder(), "hotpotatoconfig.yml");

        if (!configFile.exists()) {
            configFile.getParentFile().mkdirs();
            try {
                configFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        config = YamlConfiguration.loadConfiguration(configFile);

        config.options().header(
                "==========================================================\n" +
                        "Configuración General de HotPotato\n" +
                        "==========================================================\n" +
                        "tiempo_por_ronda_segundos: Cuánto dura cada ronda.\n" +
                        "poderes_activados: Si los ítems especiales aparecen en el suelo.\n" +
                        "timer_start: Cuánto tiempo esperan en el lobby antes del TP.\n" +
                        "modo_rondas:\n" +
                        "  'rapida' -> Escala la cantidad de bombas en +1 por ronda.\n" +
                        "  'lenta'  -> Sube, se mantiene, sube, se mantiene.\n" +
                        "  'max'    -> Fuerza a que haya un máximo de 10 rondas (o las\n" +
                        "              máximas posibles con los jugadores actuales).\n" +
                        "=========================================================="
        );
        config.options().copyHeader(true);

        if (!config.contains("tiempo_por_ronda_segundos")) {
            config.set("tiempo_por_ronda_segundos", 120);
            config.set("modo_rondas", "max");
            config.set("poderes_activados", true);
            config.set("timer_start", "00:04:00");

            config.set("zona.minX", 20903);
            config.set("zona.maxX", 21078);
            config.set("zona.minY", 71);
            config.set("zona.maxY", 103);
            config.set("zona.minZ", 20920);
            config.set("zona.maxZ", 21081);

            config.set("espectadores.x", 21000.5);
            config.set("espectadores.y", 100.0);
            config.set("espectadores.z", 21000.5);

            try { config.save(configFile); } catch (IOException ignored) {}
        }

        tiempoRondaSegundos = config.getInt("tiempo_por_ronda_segundos", 120);
        velocidadRondas = config.getString("modo_rondas", "rapida").toLowerCase();
        poderesActivados = config.getBoolean("poderes_activados", true);
        timerStart = config.getString("timer_start", "00:04:00");

        arenaMinX = config.getInt("zona.minX", 20903);
        arenaMaxX = config.getInt("zona.maxX", 21078);
        arenaMinY = config.getInt("zona.minY", 71);
        arenaMaxY = config.getInt("zona.maxY", 103);
        arenaMinZ = config.getInt("zona.minZ", 20920);
        arenaMaxZ = config.getInt("zona.maxZ", 21081);

        minX = arenaMinX;
        maxX = arenaMaxX;
        minY = arenaMinY;
        maxY = arenaMaxY;
        minZ = arenaMinZ;
        maxZ = arenaMaxZ;

        double espX = config.getDouble("espectadores.x", 21000.5);
        double espY = config.getDouble("espectadores.y", 100.0);
        double espZ = config.getDouble("espectadores.z", 21000.5);
        this.zonaEspectadores = new Location(Bukkit.getWorld("world"), espX, espY, espZ);
    }

    private boolean isInsideArena(Location loc) {
        return loc.getBlockX() >= arenaMinX && loc.getBlockX() <= arenaMaxX
                && loc.getBlockY() >= arenaMinY && loc.getBlockY() <= arenaMaxY
                && loc.getBlockZ() >= arenaMinZ && loc.getBlockZ() <= arenaMaxZ;
    }

    private List<Player> getJugadoresEnZona() {
        return Bukkit.getOnlinePlayers().stream()
                .filter(p -> isInsideArena(p.getLocation()))
                .collect(Collectors.toList());
    }

    private void enviarMensajeZona(String mensaje) {
        getJugadoresEnZona().forEach(p -> p.sendMessage(mensaje));
    }

    public void setEventInventoryManager(EventInventoryManager manager) {
        this.eventInventoryManager = manager;
    }

    // AGREGAR METODO PARA QUE EL MANAGER SEPA SI ESTA PARTICIPANDO
    public boolean isParticipante(String name) {
        return participantes.contains(name);
    }

    private void inicializarTeamHotPotato() {
        Scoreboard mainBoard = Bukkit.getScoreboardManager().getMainScoreboard();
        Team t = mainBoard.getTeam(Handlers.Teams.TeamType.HOTPOTATO.getId());
        if (t == null) {
            t = mainBoard.registerNewTeam(Handlers.Teams.TeamType.HOTPOTATO.getId());
        }
        t.setPrefix(Handlers.Teams.TeamType.HOTPOTATO.getChatPrefix());
        t.setColor(Handlers.Teams.TeamType.HOTPOTATO.getBukkitColor());
        t.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS);
        t.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
    }

    private void aplicarTeamHotPotato(Player p) {
        Scoreboard mainBoard = Bukkit.getScoreboardManager().getMainScoreboard();
        Team oldTeam = mainBoard.getEntryTeam(p.getName());
        if (oldTeam != null) {
            originalTeams.put(p.getName(), oldTeam.getName());
        } else {
            originalTeams.put(p.getName(), null);
        }

        Team hpTeam = mainBoard.getTeam(Handlers.Teams.TeamType.HOTPOTATO.getId());
        if (hpTeam != null) hpTeam.addEntry(p.getName());
    }

    private void restaurarTeamOriginal(String playerName) {
        Scoreboard mainBoard = Bukkit.getScoreboardManager().getMainScoreboard();
        Team hpTeam = mainBoard.getTeam(Handlers.Teams.TeamType.HOTPOTATO.getId());

        if (hpTeam != null && hpTeam.hasEntry(playerName)) {
            hpTeam.removeEntry(playerName);
        }

        if (originalTeams.containsKey(playerName)) {
            String oldTeamName = originalTeams.get(playerName);
            if (oldTeamName != null) {
                Team old = mainBoard.getTeam(oldTeamName);
                if (old != null) {
                    old.addEntry(playerName);
                }
            }
            originalTeams.remove(playerName);
        }
    }

    public void iniciarEvento() {
        if (eventoIniciado) return;
        eventoIniciado = true;
        participantes.clear();

        inicializarTeamHotPotato();
        iniciarLimpiezaDeMobs();

        for (Player p : Bukkit.getOnlinePlayers()) {
            participantes.add(p.getName());
            aplicarTeamHotPotato(p);
        }

        World world = Bukkit.getWorld("world");
        if (world != null) world.setGameRule(GameRule.SEND_COMMAND_FEEDBACK, false);

        int totalSeconds = parseTimeToSeconds(timerStart);
        int minutosTimer = totalSeconds / 60;
        int segundosRestantesTimer = totalSeconds % 60;

        String tiempoText = minutosTimer + " minutos";
        if (segundosRestantesTimer > 0) {
            tiempoText += " y " + segundosRestantesTimer + " segundos";
        }

        String jsonStart = "[\"\",{\"text\":\"\\u06de\",\"bold\":true,\"color\":\"dark_purple\"},{\"text\":\" Evento\",\"bold\":true,\"color\":\"light_purple\"},{\"text\":\" \\u25ba\",\"bold\":true,\"color\":\"gray\"},{\"text\":\"\\n\\n\"},{\"text\":\"Ha empezado el evento \",\"bold\":true,\"color\":\"#C66869\"},{\"text\":\"\\u201c\",\"bold\":true,\"color\":\"gray\"},{\"text\":\"Hot\",\"bold\":true,\"color\":\"red\"},{\"text\":\"Potato\",\"bold\":true,\"color\":\"gold\"},{\"text\":\"\\u201d\",\"bold\":true,\"color\":\"gray\"},{\"text\":\".\",\"bold\":true,\"color\":\"#C66869\"},{\"text\":\"\\n\"},{\"text\":\"Todos los jugadores activos están incluidos en el evento.\",\"color\":\"#DCA26E\"},{\"text\":\"\\n\"},{\"text\":\"Todos los jugadores serán teletransportados en \",\"color\":\"#DCA26E\"},{\"text\":\"" + tiempoText + "\",\"color\":\"#64ABD4\"},{\"text\":\".\",\"color\":\"#DCA26E\"},{\"text\":\"\\n\\n\"},{\"text\":\"Ojo:\",\"bold\":true,\"color\":\"#EF7A1B\"},{\"text\":\"\\n\"},{\"text\":\"Es obligatorio que los jugadores no tengan nada en el inventario.\",\"color\":\"#D54225\"},{\"text\":\"\\n \"}]";
        for (Player p : Bukkit.getOnlinePlayers()) eventoAnimation.playAnimation(p, "rosa", "evento", "center", jsonStart);

        tiempoCommand.createBossBar(EVENT_TIMER_ID, totalSeconds, timerStart, "on");
        tiempoCommand.updateBossBarDisplayName(EVENT_TIMER_ID, "§cHot§6Potato§f:");

        tareasActivas.add(Bukkit.getScheduler().runTaskLater(plugin, this::teletransportarJugadores, totalSeconds * 20L));
    }

    public void forzarTeletransporte() {
        if (!eventoIniciado) {
            eventoIniciado = true;
            participantes.clear();
            inicializarTeamHotPotato();

            for (Player p : Bukkit.getOnlinePlayers()) {
                participantes.add(p.getName());
                aplicarTeamHotPotato(p);
            }
            World world = Bukkit.getWorld("world");
            if (world != null) world.setGameRule(GameRule.SEND_COMMAND_FEEDBACK, false);
        }

        cancelarTareasActivas();
        tiempoCommand.removeBossBar(EVENT_TIMER_ID);

        Bukkit.broadcastMessage("§e۞ Teletransporte al evento forzado.");
        teletransportarJugadores();
    }

    private void teletransportarJugadores() {
        tpRealizado = true;
        World world = Bukkit.getWorld("world");
        eliminarMobsExistentes();

        int i = 0;
        for (String nombre : participantes) {
            Player p = Bukkit.getPlayer(nombre);
            if (p != null) {
                // AQUI GUARDAMOS EL INVENTARIO ANTES DE LIMPIAR
                if (eventInventoryManager != null) {
                    eventInventoryManager.saveAndClearInventory(p);
                } else {
                    p.getInventory().clear();
                }
                p.setHealth(p.getMaxHealth());
                p.setGameMode(GameMode.SURVIVAL);
                Location loc = getSafeLocation(world);

                habilidadesManager.disableHabilidades(p);
                habilidadesEffects.reapplyAllEffects(p, habilidadesManager);

                Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                    String comando = String.format(Locale.US, "magictp %s %.2f %.2f %.2f", nombre, loc.getX(), loc.getY(), loc.getZ());
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), comando);
                }, i * 5L);
                i++;
            }
        }
        Bukkit.getScheduler().runTaskLater(plugin, () -> enviarMensajeZona("§e۞ Todos los jugadores han sido teletransportados a la arena."), i * 5L + 10L);
    }

    public void mostrarReglas() {
        String[] reglas = {
                "[\"\",{\"text\":\"\\n\\n\\n\\n\\n\"},{\"text\":\"Bienvenid@s a\",\"bold\":true,\"color\":\"#4E90AB\"},{\"text\":\" Hot\",\"bold\":true,\"color\":\"red\"},{\"text\":\"Potato\",\"bold\":true,\"color\":\"gold\"},{\"text\":\"\\n\"},{\"text\":\"Este evento está basado en \",\"color\":\"#EBAB65\"},{\"text\":\"TNT Tag\",\"bold\":true,\"color\":\"red\"},{\"text\":\".\\nUn jugador tendrá una papa caliente,\\nla cual deberá pasar a otro jugador.\",\"color\":\"#EBAB65\"},{\"text\":\"\\n\\n\\n\"}]",
                "[\"\",{\"text\":\"\\n\\n\\n\\n\\n\"},{\"text\":\"En el mapa habrá múltiples poderes que darán\\nventaja a los jugadores, como:\",\"color\":\"#EBAB65\"},{\"text\":\"\\n\"},{\"text\":\"Velocidad\",\"bold\":true,\"color\":\"#59B3E4\"},{\"text\":\", \",\"bold\":true,\"color\":\"#EBAB65\"},{\"text\":\"Protección\",\"bold\":true,\"color\":\"gray\"},{\"text\":\", \",\"bold\":true,\"color\":\"#EBAB65\"},{\"text\":\"Proyectil Relentizador\",\"bold\":true,\"color\":\"#59E494\"},{\"text\":\"\\ne \",\"bold\":true,\"color\":\"#EBAB65\"},{\"text\":\"Invisibilidad\",\"bold\":true,\"color\":\"#BC59E4\"},{\"text\":\".\",\"bold\":true,\"color\":\"#EBAB65\"},{\"text\":\"\\n\\n\\n\"}]",
                "[\"\",{\"text\":\"\\n\\n\\n\\n\\n\"},{\"text\":\"El jugador que tenga la bomba\\ntendrá \",\"color\":\"#EBAB65\"},{\"text\":\"" + (tiempoRondaSegundos / 60) + " minutos\",\"bold\":true,\"color\":\"dark_aqua\"},{\"text\":\" para pasársela\\na otro jugador.\\nSi no pasas la bomba, \",\"color\":\"#EBAB65\"},{\"text\":\"explotarás\",\"bold\":true,\"color\":\"red\"},{\"text\":\".\",\"color\":\"#EBAB65\"},{\"text\":\"\\n\\n\\n\"}]",
                "[\"\",{\"text\":\"\\n\\n\\n\\n\\n\"},{\"text\":\"El último jugador que quede en pie gana.\",\"color\":\"#EBAB65\"},{\"text\":\"\\n\"},{\"text\":\"¡\",\"bold\":true,\"color\":\"dark_purple\"},{\"text\":\"Buena suerte a todos\",\"bold\":true,\"color\":\"light_purple\"},{\"text\":\"!\",\"bold\":true,\"color\":\"dark_purple\"},{\"text\":\"\\n\\n\\n\"}]"
        };

        getJugadoresEnZona().forEach(p -> p.playSound(p.getLocation(), "minecraft:music_disc.stal", SoundCategory.RECORDS, Float.MAX_VALUE, 1.3f));

        for (int i = 0; i < reglas.length; i++) {
            final String regla = reglas[i];
            tareasActivas.add(Bukkit.getScheduler().runTaskLater(plugin, () -> {
                for (Player p : getJugadoresEnZona()) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "tellraw " + p.getName() + " " + regla);
                    p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, SoundCategory.AMBIENT, Float.MAX_VALUE, 1.3f);
                }
            }, i * 160L));
        }
    }

    public void iniciarBatalla() {
        if (!tpRealizado) {
            Bukkit.broadcastMessage("§cPrimero deben ser teletransportados los jugadores.");
            return;
        }

        minX = arenaMinX;
        maxX = arenaMaxX;
        minZ = arenaMinZ;
        maxZ = arenaMaxZ;
        tiempoBorde = 120;
        bordeReduciendose = false;

        cancelarTareasActivas();

        getJugadoresEnZona().forEach(p -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "stopsound " + p.getName()));

        new BukkitRunnable() {
            int contador = 10;

            @Override
            public void run() {
                if (!eventoIniciado) {
                    this.cancel();
                    return;
                }

                if (contador > 0) {
                    String color = "#D172F6";
                    float pitch = 1.0f;

                    if (contador == 3) { color = "yellow"; pitch = 1.8f; }
                    else if (contador == 2) { color = "gold"; pitch = 1.9f; }
                    else if (contador == 1) { color = "red"; pitch = 2.0f; }
                    else if (contador == 10) {
                        pitch = 1.1f;
                        getJugadoresEnZona().forEach(p -> p.playSound(p.getLocation(), "minecraft:music_disc.lava_chicken", SoundCategory.RECORDS, Float.MAX_VALUE, 0.8f));
                    } else if (contador == 9) pitch = 1.2f;
                    else if (contador == 8) pitch = 1.3f;
                    else if (contador == 7) pitch = 1.4f;
                    else if (contador == 6) pitch = 1.5f;
                    else if (contador == 5) pitch = 1.6f;
                    else if (contador == 4) pitch = 1.7f;

                    for (Player p : getJugadoresEnZona()) {
                        p.playSound(p.getLocation(), "minecraft:block.note_block.pling", SoundCategory.RECORDS, Float.MAX_VALUE, pitch);
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "title " + p.getName() + " times 0 40 0");
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "title " + p.getName() + " title [\"\",{\"text\":\"Start\",\"bold\":true,\"color\":\"#D172F6\"},{\"text\":\":\",\"bold\":true,\"color\":\"gray\"}]");
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "title " + p.getName() + " subtitle [\"\",{\"text\":\"\\u25b6\",\"bold\":true},{\"text\":\"" + contador + "\",\"bold\":true,\"color\":\"" + color + "\"},{\"text\":\"\\u25c0\",\"bold\":true}]");
                    }
                    contador--;
                } else {
                    for (Player p : getJugadoresEnZona()) {
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "title " + p.getName() + " title {\"text\":\"Fight!\",\"bold\":true,\"color\":\"#7E5FE6\"}");
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "title " + p.getName() + " subtitle {\"text\":\"\",\"bold\":true,\"color\":\"#7E5FE6\"}");
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "title " + p.getName() + " times 15 25 15");
                    }
                    this.cancel();

                    enBatalla = true;
                    rondaEnPausa = false;
                    vivos.clear();
                    vecesConBomba.clear();
                    poderActual.clear();
                    bombasActuales.clear();

                    Collections.shuffle(cancionesDisponibles);

                    for (String nombre : participantes) {
                        Player p = Bukkit.getPlayer(nombre);
                        if (p != null && p.isOnline()) {
                            vivos.add(nombre);
                            vecesConBomba.put(nombre, 0);
                        }
                    }

                    calcularRondas();
                    inicializarScoreboard();
                    iniciarTaskActionBar();
                    iniciarRotacionPoderes();
                    iniciarSaturacionYDanioEntorno();
                    siguienteRonda();
                    mostrarBordeParticulasContinuo();
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    private void calcularRondas() {
        int total = vivos.size();
        secuenciaBombas.clear();

        if (total <= 3) {
            secuenciaBombas.add(Math.max(1, total - 1));
        } else {
            int restantes = total;

            if (velocidadRondas.equals("max")) {
                int cantidadEliminar = restantes - 3;

                if (cantidadEliminar <= 9) {
                    for (int i = 0; i < cantidadEliminar; i++) {
                        secuenciaBombas.add(1);
                    }
                } else {
                    int basePorRonda = cantidadEliminar / 9;
                    int residuo = cantidadEliminar % 9;

                    for (int i = 0; i < 9; i++) {
                        int muertesEstaRonda = basePorRonda + (i < residuo ? 1 : 0);
                        secuenciaBombas.add(muertesEstaRonda);
                    }
                }
                secuenciaBombas.add(2);
            }
            // --- MODO RAPIDA/LENTA ---
            else {
                int cantidadBombasActual = 1;
                boolean subir = true;

                while (restantes > 3) {
                    if (restantes - cantidadBombasActual < 3) {
                        cantidadBombasActual = restantes - 3;
                    }
                    if (cantidadBombasActual <= 0) cantidadBombasActual = 1;

                    secuenciaBombas.add(cantidadBombasActual);
                    restantes -= cantidadBombasActual;

                    if (restantes > 3) {
                        if (velocidadRondas.equals("rapida")) {
                            cantidadBombasActual++;
                        } else {
                            if (secuenciaBombas.size() % 2 == 0) {
                                if (subir) cantidadBombasActual++;
                                else cantidadBombasActual = Math.max(1, cantidadBombasActual - 1);
                                subir = !subir;
                            }
                        }
                    }
                }
                secuenciaBombas.add(2);
            }
        }

        totalRondasCalculadas = secuenciaBombas.size();
        rondaActual = 0;

        if (totalRondasCalculadas < 5) {
            rondaInicioBorde = 1;
        } else {
            rondaInicioBorde = totalRondasCalculadas - 3;
            if (rondaInicioBorde < 1) rondaInicioBorde = 1;
        }
    }

    private void siguienteRonda() {
        if (vivos.size() <= 1) {
            declararGanador();
            return;
        }

        if (rondaActual >= secuenciaBombas.size()) {
            declararGanador();
            return;
        }

        rondaEnPausa = false;
        int bombasEstaRonda = secuenciaBombas.get(rondaActual);
        rondaActual++;

        limpiarArena();
        bombasActuales.clear();
        poderActual.clear();
        tiempoPoder.clear();
        cargasRelentizadora.clear();

        String cancion = cancionesDisponibles.get(indexCancion % cancionesDisponibles.size());
        indexCancion++;
        getJugadoresEnZona().forEach(p -> {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "stopsound " + p.getName() + " record");
            p.playSound(p.getLocation(), cancion, SoundCategory.RECORDS, Float.MAX_VALUE, 1.0f);
        });

        for (String nombre : vivos) {
            Player p = Bukkit.getPlayer(nombre);
            if (p != null) {
                p.getInventory().clear();
                quitarEfectos(p);
            }
        }

        List<String> vivosCopy = new ArrayList<>(vivos);
        Collections.shuffle(vivosCopy);
        for (int i = 0; i < bombasEstaRonda && i < vivosCopy.size(); i++) {
            darBomba(vivosCopy.get(i));
        }

        for (Player p : getJugadoresEnZona()) {
            p.sendMessage("§e۞ ¡La ronda " + rondaActual + " ha comenzado!");
            p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, Float.MAX_VALUE, 1f);
        }

        int m = tiempoRondaSegundos / 60;
        int s = tiempoRondaSegundos % 60;
        String timerFormat = String.format("%02d:%02d:%02d", 0, m, s);
        tiempoCommand.createBossBar(EVENT_TIMER_ID, tiempoRondaSegundos, timerFormat, "on");
        tiempoCommand.updateBossBarDisplayName(EVENT_TIMER_ID, "§6Explotara en§f:");

        actualizarScoreboard();

        if (poderesActivados) {
            int maxPoderes = Math.max(1, vivos.size() / 2);
            for(int i = 0; i < maxPoderes; i++) {
                long maxTime = Math.max(20, (tiempoRondaSegundos - 25) * 20);
                tareasActivas.add(Bukkit.getScheduler().runTaskLater(plugin, this::spawnRandomPowerup, (long) (Math.random() * maxTime)));
            }
        }

        if (rondaActual >= rondaInicioBorde) {
            long ticksParaBorde = (tiempoRondaSegundos - 23) * 20L;
            if (ticksParaBorde < 0) ticksParaBorde = 0;

            tareasActivas.add(Bukkit.getScheduler().runTaskLater(plugin, this::iniciarReduccionBorde, ticksParaBorde));
        }

        timerRonda = Bukkit.getScheduler().runTaskLater(plugin, this::finalizarRonda, tiempoRondaSegundos * 20L);
        tareasActivas.add(timerRonda);
    }

    private void finalizarRonda() {
        if (!enBatalla) return;

        rondaEnPausa = true;

        for (String pName : participantes) {
            Player p = Bukkit.getPlayer(pName);
            if (p != null) Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "stopsound " + p.getName() + " record");
        }

        List<String> aEliminar = new ArrayList<>(bombasActuales);

        for (int i = 0; i < aEliminar.size(); i++) {
            final String victima = aEliminar.get(i);
            tareasActivas.add(Bukkit.getScheduler().runTaskLater(plugin, () -> matarJugadorBomba(victima), 60L + (i * 60L)));
        }

        long delaySiguiente = 60L + (aEliminar.size() * 60L) + 40L;
        tareasActivas.add(Bukkit.getScheduler().runTaskLater(plugin, this::siguienteRonda, delaySiguiente));
    }

    private void matarJugadorBomba(String nombre) {
        Player p = Bukkit.getPlayer(nombre);
        if (p != null && p.isOnline()) {
            p.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, p.getLocation(), 2);
            p.sendMessage("§c¡Has explotado!");

            getJugadoresEnZona().forEach(z -> {
                z.sendMessage("§8§l[§c§l☠§8§l]§6§l " + nombre + " §r§7ha explotado.");
                z.playSound(z.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, Float.MAX_VALUE, 1f);
            });

            procesarMuerte(p, null, "Bomba");
        }
    }

    private void procesarMuerte(Player jugador, Player atacante, String causa) {
        String eliminado = jugador.getName();

        if (vivos.contains(eliminado)) {
            boolean eraBomba = bombasActuales.contains(eliminado);
            vivos.remove(eliminado);
            bombasActuales.remove(eliminado);

            restaurarTeamOriginal(eliminado);
            jugador.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());

            jugador.setHealth(jugador.getMaxHealth());
            jugador.getInventory().clear();

            if (eventInventoryManager != null) {
                eventInventoryManager.restoreInventory(jugador);
            }

            quitarEfectos(jugador);
            habilidadesManager.enableHabilidades(jugador);
            habilidadesEffects.reapplyAllEffects(jugador, habilidadesManager);
            jugador.teleport(zonaEspectadores);

            String mensaje = "";
            if (causa.equals("Bomba")) {
                // Ya enviado en matarJugadorBomba
            } else if (causa.equals("Entorno")) {
                mensaje = "§8§l[§c§l☠§8§l]§6§l " + jugador.getName() + " §r§7ha muerto por el daño del evento.";
                jugador.sendMessage("§c¡Has muerto por daño de entorno!");
            } else {
                mensaje = "§8§l[§c§l☠§8§l]§6§l " + jugador.getName() + " §r§7ha sido eliminado.";
            }

            if (!mensaje.isEmpty()) {
                enviarMensajeZona(mensaje);
                getJugadoresEnZona().forEach(p -> {
                    p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 2.0f, 0.5f);
                    p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 2.0f, 0.1f);
                });
            }

            actualizarScoreboard();

            if (vivos.size() <= 1) {
                declararGanador();
            } else if (eraBomba && bombasActuales.isEmpty() && !causa.equals("Bomba")) {
                avanzarRondaPrematuramente();
            }
        }
    }

    private void avanzarRondaPrematuramente() {
        if (timerRonda != null) timerRonda.cancel();

        rondaEnPausa = true; // Se adelanta el tiempo muerto
        tiempoCommand.removeBossBar(EVENT_TIMER_ID);

        for (String pName : participantes) {
            Player p = Bukkit.getPlayer(pName);
            if (p != null) Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "stopsound " + p.getName() + " record");
        }

        enviarMensajeZona("§e§l¡Todas las bombas han sido eliminadas! Avanzando ronda...");
        tareasActivas.add(Bukkit.getScheduler().runTaskLater(plugin, this::siguienteRonda, 40L));
    }

    private void iniciarSaturacionYDanioEntorno() {
        taskSaturacionDanio = new BukkitRunnable() {
            @Override
            public void run() {
                if (!enBatalla) {
                    this.cancel();
                    return;
                }

                for (String nombre : vivos) {
                    Player p = Bukkit.getPlayer(nombre);
                    if (p != null && p.isOnline()) {

                        p.setFoodLevel(20);
                        p.setSaturation(20f);

                        Location loc = p.getLocation();
                        Block bloqueAbajo = loc.clone().subtract(0, 0.1, 0).getBlock();

                        boolean recibirDanioBorde = false;
                        boolean recibirDanioCactus = false;

                        if (loc.getX() < minX || loc.getX() > maxX || loc.getZ() < minZ || loc.getZ() > maxZ) {
                            recibirDanioBorde = true;
                        }

                        if (bloqueAbajo.getType() == Material.LIME_TERRACOTTA) {
                            recibirDanioCactus = true;
                            long time = cooldownCactus.getOrDefault(p.getName(), 0L);
                            if (System.currentTimeMillis() - time > 4000) {
                                p.sendMessage(ChatColor.RED + "¡Está prohibido estar encima de un cactus, baja rápido antes de que seas eliminado!");
                                cooldownCactus.put(p.getName(), System.currentTimeMillis());
                            }
                        }

                        if (recibirDanioBorde || recibirDanioCactus) {
                            double danioAmount = recibirDanioCactus ? 2.0 : 1.0;

                            double nuevaVida = p.getHealth() - danioAmount;
                            if (nuevaVida <= 0.3) {
                                procesarMuerte(p, null, "Entorno");
                            } else {
                                if (p.getGameMode() == GameMode.CREATIVE || p.getGameMode() == GameMode.SPECTATOR) {
                                    p.setHealth(nuevaVida);
                                    p.playEffect(EntityEffect.HURT);
                                } else {
                                    p.damage(danioAmount);
                                }
                            }
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
        tareasActivas.add(taskSaturacionDanio);
    }

    private void darBomba(String nombre) {
        Player p = Bukkit.getPlayer(nombre);
        if (p == null) return;

        bombasActuales.add(nombre);
        poderActual.remove(nombre);
        vecesConBomba.put(nombre, vecesConBomba.getOrDefault(nombre, 0) + 1);

        p.sendTitle("§c§lTIENES LA BOMBA", "", 10, 40, 10);
        quitarEfectos(p);
        p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 0, false, false));
        p.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, Integer.MAX_VALUE, 0, false, false));

        ItemStack papa = new ItemStack(Material.BAKED_POTATO);
        ItemMeta meta = papa.getItemMeta();
        meta.setDisplayName("§c§lPAPA CALIENTE");
        meta.addEnchant(Enchantment.FIRE_ASPECT, 1, true);
        papa.setItemMeta(meta);

        llenarHotbar(p, papa);
        p.getInventory().setHelmet(new ItemStack(Material.TNT));
        actualizarScoreboard();
    }

    private void quitarBomba(String nombre) {
        Player p = Bukkit.getPlayer(nombre);
        bombasActuales.remove(nombre);
        if (p != null) {
            p.sendTitle("§a§lYA NO TIENES BOMBA", "", 10, 40, 10);
            quitarEfectos(p);
            p.getInventory().clear();
        }
        actualizarScoreboard();
    }

    @EventHandler
    public void onGolpe(EntityDamageByEntityEvent e) {
        if (!enBatalla || rondaEnPausa) return;
        if (!(e.getEntity() instanceof Player) || !(e.getDamager() instanceof Player)) return;

        Player victima = (Player) e.getEntity();
        Player atacante = (Player) e.getDamager();
        e.setDamage(0);

        if (bombasActuales.contains(atacante.getName()) && !bombasActuales.contains(victima.getName())) {
            if ("Proteccion".equals(poderActual.get(victima.getName()))) {
                poderActual.remove(victima.getName());
                victima.getInventory().clear();
                victima.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 200, 0));
                victima.getWorld().playSound(victima.getLocation(), Sound.ITEM_TOTEM_USE, 1f, 1f);
                victima.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, victima.getLocation(), 50);
                victima.playEffect(EntityEffect.TOTEM_RESURRECT);
                actualizarScoreboard();
                return;
            }

            quitarBomba(atacante.getName());
            darBomba(victima.getName());
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        if (!enBatalla || !poderesActivados || rondaEnPausa) return;
        Player p = e.getPlayer();
        if (!vivos.contains(p.getName()) || bombasActuales.contains(p.getName())) return;

        for (Entity ent : new ArrayList<>(poderesEnSuelo)) {
            if (ent.isValid() && ent.getLocation().distanceSquared(p.getLocation()) < 2.5 && isInsideArena(ent.getLocation())) {
                if (poderActual.containsKey(p.getName())) continue;

                String poder = ent.getPersistentDataContainer().get(new NamespacedKey(plugin, "poder"), org.bukkit.persistence.PersistentDataType.STRING);
                if (poder != null) {
                    darPoder(p, poder);
                    ent.remove();
                    poderesEnSuelo.remove(ent);
                }
            }
        }
    }

    private void darPoder(Player p, String poder) {
        poderActual.put(p.getName(), poder);
        p.getInventory().clear();

        String jsonMsg = "[\"\",{\"text\":\"\\u06de \",\"bold\":true,\"color\":\"gold\"},{\"text\":\"El jugador \",\"bold\":true,\"color\":\"white\"},{\"text\":\"" + p.getName() + " \",\"bold\":true,\"color\":\"gold\"},{\"text\":\"acaba de obtener el poder: \",\"bold\":true,\"color\":\"white\"}";

        if (poder.equals("Velocidad")) {
            p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 400, 1));
            ItemStack item = new ItemStack(Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE);
            ItemMeta m = item.getItemMeta();
            m.setDisplayName("§b§lVelocidad II");
            m.setLore(Arrays.asList("§7Otorga Velocidad II por 20s."));
            item.setItemMeta(m);
            llenarHotbar(p, item);
            tiempoPoder.put(p.getName(), 20);
            jsonMsg += ",{\"text\":\" Velocidad\",\"bold\":true,\"color\":\"dark_aqua\"}]";

        } else if (poder.equals("Bola Relentizadora")) {
            ItemStack item = new ItemStack(Material.WIND_CHARGE, 2);
            ItemMeta m = item.getItemMeta();
            m.setDisplayName("§a§lProyectil Relentizador");
            m.setLore(Arrays.asList("§7Lanza a la bomba para", "§7darle lentitud y ceguera."));
            item.setItemMeta(m);
            p.getInventory().setItem(0, item);
            cargasRelentizadora.put(p.getName(), 2);
            jsonMsg += ",{\"text\":\" Proyectil Relentizador\",\"bold\":true,\"color\":\"green\"}]";

        } else if (poder.equals("Invisibilidad")) {
            p.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 400, 0));
            ItemStack item = new ItemStack(Material.POTION);
            ItemMeta m = item.getItemMeta();
            m.setDisplayName("§5§lInvisibilidad");
            m.setLore(Arrays.asList("§7Otorga Invisibilidad por 20s."));
            item.setItemMeta(m);
            llenarHotbar(p, item);
            tiempoPoder.put(p.getName(), 20);
            jsonMsg += ",{\"text\":\" Invisibilidad\",\"bold\":true,\"color\":\"blue\"}]";

        } else if (poder.equals("Proteccion")) {
            ItemStack item = new ItemStack(Material.TOTEM_OF_UNDYING);
            ItemMeta m = item.getItemMeta();
            m.setDisplayName("§7§lProtección");
            m.setLore(Arrays.asList("§7Bloquea un golpe de bomba y", "§7te da Velocidad para huir."));
            item.setItemMeta(m);
            llenarHotbar(p, item);
            jsonMsg += ",{\"text\":\" Proteccion\",\"bold\":true,\"color\":\"gray\"}]";
        }

        for (Player online : getJugadoresEnZona()) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "tellraw " + online.getName() + " " + jsonMsg);
        }
        actualizarScoreboard();
    }

    @EventHandler
    public void onProjectileLaunch(ProjectileLaunchEvent e) {
        if (!enBatalla || rondaEnPausa) return;
        if (e.getEntity() instanceof WindCharge charge && charge.getShooter() instanceof Player p) {
            if (vivos.contains(p.getName()) && "Bola Relentizadora".equals(poderActual.get(p.getName()))) {
                int c = cargasRelentizadora.getOrDefault(p.getName(), 2) - 1;
                cargasRelentizadora.put(p.getName(), c);
                if (c <= 0) {
                    poderActual.remove(p.getName());
                    p.getInventory().clear();
                    actualizarScoreboard();
                }
            }
        }
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent e) {
        if (!enBatalla || rondaEnPausa) return;
        if (e.getEntity() instanceof WindCharge charge) {
            if (charge.getShooter() instanceof Player tirador && e.getHitEntity() instanceof Player victima) {
                if (bombasActuales.contains(victima.getName())) {
                    victima.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 400, 0));
                    victima.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 400, 0));
                    tirador.sendMessage("§a¡Has relentizado a la bomba!");
                }
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (enBatalla && vivos.contains(e.getWhoClicked().getName())) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerDrop(PlayerDropItemEvent e) {
        if (enBatalla && vivos.contains(e.getPlayer().getName())) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onConsume(PlayerItemConsumeEvent e) {
        if (enBatalla && vivos.contains(e.getPlayer().getName())) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (enBatalla && vivos.contains(event.getPlayer().getName())) {
            procesarMuerte(event.getPlayer(), null, "Desconexion");
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!enBatalla || !(event.getEntity() instanceof Player)) return;

        Player jugador = (Player) event.getEntity();

        if (vivos.contains(jugador.getName())) {
            double nuevaVida = jugador.getHealth() - event.getFinalDamage();
            if (nuevaVida <= 0.3) {
                event.setCancelled(true);
                procesarMuerte(jugador, null, event.getCause() == EntityDamageEvent.DamageCause.FALL ? "Caida" : "Otro Daño");
            }
        }
    }

    @EventHandler
    public void onRegen(EntityRegainHealthEvent e) {
        if (enBatalla && e.getEntity() instanceof Player) {
            Player p = (Player) e.getEntity();
            if (vivos.contains(p.getName()) && e.getRegainReason() == EntityRegainHealthEvent.RegainReason.SATIATED) {
                e.setCancelled(true);
            }
        }
    }

    private void iniciarTaskActionBar() {
        taskActionBar = new BukkitRunnable() {
            @Override
            public void run() {
                if (!enBatalla) {
                    this.cancel();
                    return;
                }
                for (String nombre : vivos) {
                    Player p = Bukkit.getPlayer(nombre);
                    if (p != null) {
                        String poder = poderActual.getOrDefault(nombre, "Ninguno");
                        String msg = "";
                        if (poder.equals("Velocidad")) {
                            int t = tiempoPoder.getOrDefault(nombre, 0);
                            msg = "[\"\",{\"text\":\"\\u06de \",\"bold\":true,\"color\":\"light_purple\"},{\"text\":\"Poder \",\"bold\":true,\"color\":\"dark_purple\"},{\"text\":\"Velocidad\",\"bold\":true,\"color\":\"dark_aqua\"},{\"text\":\": 00:" + String.format("%02d", t) + "\",\"color\":\"white\"}]";
                            if(t>0) tiempoPoder.put(nombre, t-1);
                            else { poderActual.remove(nombre); p.getInventory().clear(); actualizarScoreboard(); }
                        } else if (poder.equals("Invisibilidad")) {
                            int t = tiempoPoder.getOrDefault(nombre, 0);
                            msg = "[\"\",{\"text\":\"\\u06de \",\"bold\":true,\"color\":\"light_purple\"},{\"text\":\"Poder \",\"bold\":true,\"color\":\"dark_purple\"},{\"text\":\"Invisibilidad\",\"bold\":true,\"color\":\"blue\"},{\"text\":\": 00:" + String.format("%02d", t) + "\",\"color\":\"white\"}]";
                            if(t>0) tiempoPoder.put(nombre, t-1);
                            else { poderActual.remove(nombre); p.getInventory().clear(); actualizarScoreboard(); }
                        } else if (poder.equals("Proteccion")) {
                            msg = "[\"\",{\"text\":\"\\u06de \",\"bold\":true,\"color\":\"light_purple\"},{\"text\":\"Poder \",\"bold\":true,\"color\":\"dark_purple\"},{\"text\":\"Proteccion\",\"bold\":true,\"color\":\"gray\"},{\"text\":\": Activado\",\"color\":\"white\"}]";
                        } else if (poder.equals("Bola Relentizadora")) {
                            int c = cargasRelentizadora.getOrDefault(nombre, 2);
                            msg = "[\"\",{\"text\":\"\\u06de \",\"bold\":true,\"color\":\"light_purple\"},{\"text\":\"Poder \",\"bold\":true,\"color\":\"dark_purple\"},{\"text\":\"Proyectil Relentizador\",\"bold\":true,\"color\":\"green\"},{\"text\":\": " + c + "\",\"color\":\"white\"}]";
                        }

                        if (!msg.isEmpty()) {
                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "title " + p.getName() + " actionbar " + msg);
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
        tareasActivas.add(taskActionBar);
    }

    private void iniciarRotacionPoderes() {
        taskRotacionPoderes = new BukkitRunnable() {
            @Override
            public void run() {
                if(!enBatalla) { this.cancel(); return; }
                for(Entity ent : poderesEnSuelo) {
                    if(ent.isValid()) {
                        Location loc = ent.getLocation();
                        loc.setYaw(loc.getYaw() + 5f);
                        ent.teleport(loc);
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
        tareasActivas.add(taskRotacionPoderes);
    }

    private void declararGanador() {
        enBatalla = false;
        cancelarTareasActivas();

        String ganador = vivos.isEmpty() ? "Nadie" : vivos.get(0);

        getJugadoresEnZona().forEach(p -> {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "stopsound " + p.getName() + " record");
            p.sendMessage("§d۞§6§l " + ganador + " §f§lha ganado el evento §c§lHot§6§lPotato§7§l.");
            p.sendTitle("§6§lGANADOR ", "§8§l>§6§l " + ganador + "§8§l <", 15, 150, 15);
            p.playSound(p.getLocation(), "minecraft:ui.toast.challenge_complete", SoundCategory.RECORDS, Float.MAX_VALUE, 1);
        });

        StringBuilder msjTop = new StringBuilder();
        msjTop.append("\n§5§m                                                 \n");
        msjTop.append("         §6§lTop Jugadores\n\n");

        List<Map.Entry<String, Integer>> listaTop = new ArrayList<>(vecesConBomba.entrySet());
        listaTop.sort((a, b) -> b.getValue().compareTo(a.getValue()));
        listaTop.removeIf(e -> e.getKey().equals(ganador));

        msjTop.append("§7§l1. §e").append(ganador).append(" §8§l(Bombas: §c§l").append(vecesConBomba.getOrDefault(ganador, 0)).append("§8§l)\n");
        for(int i = 0; i < Math.min(2, listaTop.size()); i++) {
            msjTop.append("§7§l").append(i+2).append(". §f").append(listaTop.get(i).getKey())
                    .append(" §8§l(Bombas: §c§l").append(listaTop.get(i).getValue()).append("§8§l)\n");
        }
        msjTop.append("§5§m                                                 \n");

        enviarMensajeZona(msjTop.toString());

        Bukkit.getScheduler().runTaskLater(plugin, this::terminarEvento, 20 * 20L); // 20s y fin
    }

    public void terminarEvento() {
        eventoIniciado = false;
        tpRealizado = false;
        enBatalla = false;
        rondaEnPausa = false;
        limpiarArena();
        cancelarTareasActivas();

        tiempoCommand.removeBossBar(EVENT_TIMER_ID);

        for (String pName : participantes) {
            Player p = Bukkit.getPlayer(pName);
            restaurarTeamOriginal(pName);
            if (p != null) {
                p.getInventory().clear();
                // DEVOLVER INVENTARIO A TODOS LOS QUE SEGUÍAN PARTICIPANDO
                if (eventInventoryManager != null) {
                    eventInventoryManager.restoreInventory(p);
                }
                habilidadesManager.enableHabilidades(p);
                habilidadesEffects.reapplyAllEffects(p, habilidadesManager);
                p.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "magictp " + p.getName() + " spawn");
            }
        }

        participantes.clear();
        vivos.clear();
        bombasActuales.clear();
        poderActual.clear();
        vecesConBomba.clear();
        tiempoPoder.clear();
        cargasRelentizadora.clear();
        originalTeams.clear();

        for (Scoreboard board : playerScoreboards.values()) {
            if (board != null) {
                Objective obj = board.getObjective("hotpotato");
                if (obj != null) {
                    obj.unregister();
                }
            }
        }
        playerScoreboards.clear();

        World world = Bukkit.getWorld("world");
        if (world != null) {
            world.setGameRule(GameRule.SEND_COMMAND_FEEDBACK, true);
        }
    }

    private void inicializarScoreboard() {
        playerScoreboards.clear();
        Scoreboard mainBoard = Bukkit.getScoreboardManager().getMainScoreboard();

        for (Player p : getJugadoresEnZona()) {
            Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();

            for (Team mainTeam : mainBoard.getTeams()) {
                Team newTeam = board.registerNewTeam(mainTeam.getName());
                newTeam.setPrefix(mainTeam.getPrefix());
                newTeam.setSuffix(mainTeam.getSuffix());
                newTeam.setColor(mainTeam.getColor());
                newTeam.setOption(Team.Option.NAME_TAG_VISIBILITY, mainTeam.getOption(Team.Option.NAME_TAG_VISIBILITY));
                newTeam.setOption(Team.Option.COLLISION_RULE, mainTeam.getOption(Team.Option.COLLISION_RULE));

                for (String entry : mainTeam.getEntries()) {
                    newTeam.addEntry(entry);
                }
            }

            Objective obj = board.registerNewObjective("hotpotato", "dummy", "§c§lHOT§6§lPOTATO");
            obj.setDisplaySlot(DisplaySlot.SIDEBAR);

            Team tPlayers = board.registerNewTeam("sb_players");
            tPlayers.addEntry(ChatColor.AQUA.toString());
            tPlayers.setPrefix("  §c§l♦ §6§lJugadores§f§l: ");
            tPlayers.setSuffix("§a0");
            obj.getScore(ChatColor.AQUA.toString()).setScore(4);

            Team tPoder = board.registerNewTeam("sb_poder");
            tPoder.addEntry(ChatColor.RED.toString());
            tPoder.setPrefix("  §4§l⚠ §9§lPoder§f§l: ");
            tPoder.setSuffix("§7Ninguno");
            obj.getScore(ChatColor.RED.toString()).setScore(2);

            obj.getScore("§r     ").setScore(5);
            obj.getScore("§r      ").setScore(3);
            obj.getScore("§r          ").setScore(1);

            p.setScoreboard(board);
            playerScoreboards.put(p.getName(), board);
        }
    }

    private void actualizarScoreboard() {
        for (Player p : getJugadoresEnZona()) {
            Scoreboard board = playerScoreboards.get(p.getName());
            if (board != null) {
                Team tPlayers = board.getTeam("sb_players");
                if (tPlayers != null) tPlayers.setSuffix("§3§l" + vivos.size());

                Team tPoder = board.getTeam("sb_poder");
                if (tPoder != null) {
                    String poder = poderActual.getOrDefault(p.getName(), "Ninguno");
                    String color = switch (poder) {
                        case "Velocidad" -> "§b§l";
                        case "Invisibilidad" -> "§5§l";
                        case "Proteccion" -> "§7§l";
                        case "Bola Relentizadora" -> "§a§l";
                        default -> "§8§l";
                    };
                    if (bombasActuales.contains(p.getName())) {
                        poder = "BOMBA";
                        color = "§c§l";
                    }
                    tPoder.setSuffix(color + poder);
                }
            }
        }
    }

    private void iniciarReduccionBorde() {
        if (vivos.size() <= 1 || !enBatalla) return;

        int cX = arenaMinX + (arenaMaxX - arenaMinX) / 2;
        int cZ = arenaMinZ + (arenaMaxZ - arenaMinZ) / 2;

        if (minX >= cX - 12 && maxX <= cX + 12 && minZ >= cZ - 12 && maxZ <= cZ + 12) {
            minX = cX - 12; maxX = cX + 12; minZ = cZ - 12; maxZ = cZ + 12;
            enviarMensajeZona("§§§e§l۞§6§l El borde ha llegado a su límite.§r§§ ");
            for (Player p : getJugadoresEnZona()) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "title " + p.getName() + " title {\"text\":\" \",\"bold\":true,\"color\":\"red\"}");
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "title " + p.getName() + " subtitle [\"\",{\"text\":\"\\u26a0\",\"bold\":true,\"color\":\"yellow\"},{\"text\":\" El borde ya no se moverá.\",\"bold\":true,\"color\":\"gold\"},{\"text\":\" \\u26a0\",\"bold\":true,\"color\":\"yellow\"}]");
                p.playSound(p.getLocation(), "block.note_block.bell", SoundCategory.MASTER, Float.MAX_VALUE, 0.1f);
            }
            return;
        }

        new BukkitRunnable() {
            int countdown = 3;
            @Override
            public void run() {
                if (!enBatalla) {
                    this.cancel();
                    return;
                }

                if (countdown == 0) {
                    enviarMensajeZona("§§§c§l۞§4§l El borde ha comenzado a reducirse!!§r§§ ");
                    for (Player p : getJugadoresEnZona()) {
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "title " + p.getName() + " title {\"text\":\" \",\"bold\":true,\"color\":\"red\"}");
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "title " + p.getName() + " subtitle [\"\",{\"text\":\"\\u26a0\",\"bold\":true,\"color\":\"red\"},{\"text\":\" Borde reduciéndose. \",\"bold\":true,\"color\":\"dark_red\"},{\"text\":\"\\u26a0\",\"bold\":true,\"color\":\"red\"}]");
                        p.playSound(p.getLocation(), "block.note_block.bell", SoundCategory.MASTER, Float.MAX_VALUE, 2f);
                    }
                    iniciarReduccionBordeContinuo(cX, cZ);
                    this.cancel();
                } else {
                    String color = switch (countdown) { case 3 -> "§e§l"; case 2 -> "§6§l"; default -> "§c§l"; };
                    enviarMensajeZona("§c۞§7§l El §4§lborde§7§l se reducirá en " + color + countdown);
                    getJugadoresEnZona().forEach(p -> p.playSound(p.getLocation(), "block.note_block.bell", SoundCategory.MASTER, Float.MAX_VALUE, 1.5f));
                    countdown--;
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    private void iniciarReduccionBordeContinuo(int cX, int cZ) {
        taskReduccionBordeContinuo = new BukkitRunnable() {
            int steps = 10;
            int stepSize = 2; // Cantidad de bloques que se reduce por segundo

            @Override
            public void run() {
                if (steps <= 0 || !enBatalla) {
                    enviarMensajeZona("§§§6§l۞ §c§lEl borde se ha detenido. §r§l§§");
                    for (Player p : getJugadoresEnZona()) {
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "title " + p.getName() + " title {\"text\":\" \",\"bold\":true,\"color\":\"red\"}");
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "title " + p.getName() + " subtitle [\"\",{\"text\":\"\\u26a0\",\"bold\":true,\"color\":\"gold\"},{\"text\":\" Borde Detenido.\",\"bold\":true,\"color\":\"red\"},{\"text\":\" \\u26a0\",\"bold\":true,\"color\":\"gold\"}]");
                        p.playSound(p.getLocation(), "block.note_block.bell", SoundCategory.MASTER, Float.MAX_VALUE, 0.1f);
                    }
                    this.cancel();
                } else {
                    // Reducción gradual
                    minX = Math.min(minX + stepSize, cX - 12);
                    maxX = Math.max(maxX - stepSize, cX + 12);
                    minZ = Math.min(minZ + stepSize, cZ - 12);
                    maxZ = Math.max(maxZ - stepSize, cZ + 12);

                    steps--;
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
        tareasActivas.add(taskReduccionBordeContinuo);
    }

    private void mostrarBordeParticulasContinuo() {
        if (taskBordeParticulas != null && !taskBordeParticulas.isCancelled()) {
            return;
        }
        taskBordeParticulas = new BukkitRunnable() {
            @Override
            public void run() {
                if (!enBatalla) { this.cancel(); return; }
                mostrarBordeParticulas(minX, maxX, minZ, maxZ);
            }
        }.runTaskTimer(plugin, 0L, 20L);
        tareasActivas.add(taskBordeParticulas);
    }

    private void mostrarBordeParticulas(int minX, int maxX, int minZ, int maxZ) {
        List<Player> espectadores = getJugadoresEnZona();
        if (espectadores.isEmpty()) return;
        Particle particula = Particle.SONIC_BOOM;
        int stepY = 6, stepXZ = 4;
        for (int y = arenaMinY; y <= arenaMaxY; y += stepY) {
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
        Location loc = new Location(Bukkit.getWorld("world"), x + 0.5, y + 0.5, z + 0.5);
        for (Player player : players) {
            if (player.getLocation().distanceSquared(loc) < 1600) {
                player.spawnParticle(p, loc, 1, 0, 0, 0, 0);
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (isInsideArena(event.getBlock().getLocation()) && event.getPlayer().getGameMode() != GameMode.CREATIVE) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (isInsideArena(event.getBlock().getLocation()) && event.getPlayer().getGameMode() != GameMode.CREATIVE) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntitySpawn(EntitySpawnEvent event) {
        if (eventoIniciado && isInsideArena(event.getLocation()) && event.getEntity() instanceof Monster) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        if (eventoIniciado) {
            for (Entity entity : event.getChunk().getEntities()) {
                if (entity instanceof Monster mob && isInsideArena(entity.getLocation())) {
                    mob.remove();
                }
            }
        }
    }

    public void eliminarMobsExistentes() {
        if (eventoIniciado) {
            for (World world : Bukkit.getWorlds()) {
                for (Entity entity : world.getEntities()) {
                    if (entity instanceof Monster mob && isInsideArena(entity.getLocation())) {
                        mob.remove();
                    }
                }
            }
        }
    }

    private void iniciarLimpiezaDeMobs() {
        BukkitTask taskLimpieza = new BukkitRunnable() {
            @Override
            public void run() {
                if (!eventoIniciado) {
                    this.cancel();
                    return;
                }
                World world = Bukkit.getWorld("world");
                if (world != null) {
                    for (Entity entity : world.getEntities()) {
                        if (entity instanceof Mob && isInsideArena(entity.getLocation())) {
                            entity.remove();
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 50L);
        tareasActivas.add(taskLimpieza);
    }

    private void spawnRandomPowerup() {
        if (!enBatalla) return;
        World w = Bukkit.getWorld("world");
        Location loc = getSafeLocation(w);

        if(loc.getBlockX() < minX || loc.getBlockX() > maxX || loc.getBlockZ() < minZ || loc.getBlockZ() > maxZ) return;

        loc.add(0, 0.5, 0);

        String[] poderes = {"Velocidad", "Bola Relentizadora", "Invisibilidad", "Proteccion"};
        String poderElegido = poderes[new Random().nextInt(poderes.length)];

        ItemDisplay display = (ItemDisplay) w.spawnEntity(loc, EntityType.ITEM_DISPLAY);
        display.setGlowing(true);
        display.setCustomNameVisible(true);

        ItemStack displayItem;
        if (poderElegido.equals("Velocidad")) {
            display.setCustomName("§b§lVelocidad");
            displayItem = new ItemStack(Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE);
        } else if (poderElegido.equals("Bola Relentizadora")) {
            display.setCustomName("§a§lProyectil Relentizador");
            displayItem = new ItemStack(Material.WIND_CHARGE);
        } else if (poderElegido.equals("Invisibilidad")) {
            display.setCustomName("§5§lInvisibilidad");
            displayItem = new ItemStack(Material.POTION);
        } else {
            display.setCustomName("§7§lProtección");
            displayItem = new ItemStack(Material.TOTEM_OF_UNDYING);
        }

        display.setItemStack(displayItem);
        display.getPersistentDataContainer().set(new NamespacedKey(plugin, "poder"), org.bukkit.persistence.PersistentDataType.STRING, poderElegido);
        poderesEnSuelo.add(display);
    }

    private Location getSafeLocation(World world) {
        for (int i = 0; i < 50; i++) {
            int x = minX + new Random().nextInt(maxX - minX + 1);
            int z = minZ + new Random().nextInt(maxZ - minZ + 1);
            int y = arenaMaxY;
            while(y > arenaMinY) {
                Material base = world.getBlockAt(x, y, z).getType();
                Material head = world.getBlockAt(x, y + 1, z).getType();
                Material sky = world.getBlockAt(x, y + 2, z).getType();

                if((base == Material.SAND || base == Material.RED_SAND) && !head.isSolid() && !sky.isSolid()) {
                    return new Location(world, x + 0.5, y + 1, z + 0.5);
                }
                y--;
            }
        }
        int fallbackX = minX + new Random().nextInt(maxX - minX + 1);
        int fallbackZ = minZ + new Random().nextInt(maxZ - minZ + 1);
        return new Location(world, fallbackX + 0.5, world.getHighestBlockYAt(fallbackX, fallbackZ) + 1, fallbackZ + 0.5);
    }

    private void llenarHotbar(Player p, ItemStack item) {
        p.getInventory().clear();
        for (int i = 0; i < 9; i++) {
            p.getInventory().setItem(i, item);
        }
    }

    private void quitarEfectos(Player p) {
        for (PotionEffect effect : p.getActivePotionEffects()) {
            p.removePotionEffect(effect.getType());
        }
    }

    private void limpiarArena() {
        for (Entity ent : poderesEnSuelo) ent.remove();
        poderesEnSuelo.clear();
    }

    private void cancelarTareasActivas() {
        for (BukkitTask tarea : tareasActivas) {
            if (tarea != null && !tarea.isCancelled()) {
                tarea.cancel();
            }
        }
        tareasActivas.clear();
    }

    //COMANDOS ADMINISTRATIVOS Y HELPER

    public void addParticipante(CommandSender sender, String nombre) {
        Player p = Bukkit.getPlayerExact(nombre);
        if (p == null) {
            sender.sendMessage("§cEse jugador no está conectado o no existe.");
            return;
        }
        if (!participantes.contains(nombre)) {
            participantes.add(nombre);
            aplicarTeamHotPotato(p);
            sender.sendMessage("§aJugador " + nombre + " añadido a HotPotato.");
            if (tpRealizado) {
                // GUARDAR EL INVENTARIO SI ENTRA A MITAD DE EVENTO
                if (eventInventoryManager != null) {
                    eventInventoryManager.saveAndClearInventory(p);
                } else {
                    p.getInventory().clear();
                }
                p.teleport(getSafeLocation(p.getWorld()));
                habilidadesManager.disableHabilidades(p);
                habilidadesEffects.reapplyAllEffects(p, habilidadesManager);
            }
        }
    }

    public void removeParticipante(CommandSender sender, String nombre) {
        Player p = Bukkit.getPlayerExact(nombre);
        if (p == null) {
            sender.sendMessage("§cEse jugador no está conectado o no existe.");
            return;
        }
        if (participantes.contains(nombre)) {
            participantes.remove(nombre);
            restaurarTeamOriginal(nombre);
            p.getInventory().clear();

            if (eventInventoryManager != null) {
                eventInventoryManager.restoreInventory(p);
            }

            habilidadesManager.enableHabilidades(p);
            habilidadesEffects.reapplyAllEffects(p, habilidadesManager);
            sender.sendMessage("§cJugador " + nombre + " removido de HotPotato.");
            if (tpRealizado) p.teleport(zonaEspectadores);
        }
    }

    public void listParticipantes(CommandSender sender) {
        sender.sendMessage("§eParticipantes (" + participantes.size() + "): §f" + String.join(", ", participantes));
    }

    private int parseTimeToSeconds(String timeString) {
        String[] parts = timeString.split(":");
        int hours = Integer.parseInt(parts[0]);
        int minutes = Integer.parseInt(parts[1]);
        int seconds = Integer.parseInt(parts[2]);
        return hours * 3600 + minutes * 60 + seconds;
    }
}