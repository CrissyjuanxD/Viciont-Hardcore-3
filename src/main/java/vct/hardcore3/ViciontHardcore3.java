package vct.hardcore3;

import Commands.*;
import Dificultades.*;
import Dificultades.CustomMobs.CorruptedZombies;
import Dificultades.CustomMobs.CustomDolphin;
import Enchants.*;
import Estructures.CorruptedVillage;
import Events.DamageLogListener;
import Events.Skybattle.EventoHandler;
import Security.PingMonitor.PingMonitor;
import TitleListener.*;
import items.DoubleLifeTotem;
import list.VHList;
import org.bukkit.*;
import org.bukkit.command.PluginCommand;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.ChatColor;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import chat.chatgeneral;

import java.util.Objects;

public class ViciontHardcore3 extends JavaPlugin implements Listener {

    public String Prefix = "&d&lViciont&5&lHardcore &5&l3&7➤ &f";
    public String Version = getDescription().getVersion();
    private DeathStormHandler deathStormHandler;
    private DayHandler dayHandler;
    private RuletaAnimation ruletaAnimation;
    private MuerteAnimation muerteAnimation;
    private BonusAnimation bonusAnimation;
    private DayOneChanges dayOneChanges;
    private DayTwoChanges dayTwoChanges;
    private DayFourChanges dayFourChanges;
    private DaySixChanges daySixChanges;
    private DayTenChanges dayTenChanges;
    private DoubleLifeTotem doubleLifeTotemHandler;
    private NormalTotemHandler normalTotemHandler;
    private PingMonitor pingMonitor;
    public static boolean shuttingDown = false;
    private DamageLogListener damageLogListener;

    private EventoHandler eventoHandler;
    private GameModeTeamHandler gameModeTeamHandler;

    private CorruptedZombies corruptedZombies;
    private CustomDolphin customDolphin;


    @Override
    public void onEnable() {
        Bukkit.getConsoleSender().sendMessage(
                ChatColor.translateAlternateColorCodes('&', Prefix + "&aha sido habilitado!, &eVersion: " + Version));

        // Registra los eventos de esta clase
        Bukkit.getServer().getPluginManager().registerEvents(this, this);

        // Registra evento de revivir
        ReviveCommand reviveCommand = new ReviveCommand(this);
        ReviveCoordsCommand reviveCoordsCommand = new ReviveCoordsCommand(this);
        this.getCommand("revive").setExecutor(reviveCommand);
        this.getCommand("revive").setTabCompleter(reviveCommand);
        this.getCommand("revivecoords").setExecutor(reviveCoordsCommand);

        // Inicializa los manejadores de eventos de totems
        doubleLifeTotemHandler = new DoubleLifeTotem(this);
        normalTotemHandler = new NormalTotemHandler(this);
        // Registra los eventos de totems
        getServer().getPluginManager().registerEvents(normalTotemHandler, this);
        getServer().getPluginManager().registerEvents(doubleLifeTotemHandler, this);


        // Inicializa correctamente el DayHandler y asigna a la variable de instancia
        dayHandler = new DayHandler(this);
        getServer().getPluginManager().registerEvents(new MuerteHandler(this), this);

        // DeathStorm
        deathStormHandler = new DeathStormHandler(this, dayHandler);
        getServer().getPluginManager().registerEvents(deathStormHandler, this);

        // Comandos de DeathStorm
        PluginCommand resetCommand = getCommand("resetdeathstorm");
        PluginCommand addCommand = getCommand("adddeathstorm");
        PluginCommand removeCommand = getCommand("removedeathstorm");

        DeathStormCommand deathStormCommand = new DeathStormCommand(deathStormHandler);
        if (resetCommand != null) resetCommand.setExecutor(deathStormCommand);
        if (addCommand != null) addCommand.setExecutor(deathStormCommand);
        if (removeCommand != null) removeCommand.setExecutor(deathStormCommand);

        // Comandos de días
        PluginCommand changeDayCommand = getCommand("cambiardia");
        PluginCommand dayCommand = getCommand("dia");

        if (changeDayCommand != null) {
            changeDayCommand.setExecutor(new DayCommandHandler(dayHandler));
        }
        if (dayCommand != null) {
            dayCommand.setExecutor(new DayCommandHandler(dayHandler));
        }

        // Cargar datos de DeathStorm al iniciar
        deathStormHandler.loadStormData();

        // Registrar eventos de chat y teams
        chatgeneral chatGeneralHandler = new chatgeneral();
        gameModeTeamHandler = new GameModeTeamHandler(this);
        FirstJoinHandler firstJoinHandler = new FirstJoinHandler(this);
        getServer().getPluginManager().registerEvents(chatGeneralHandler, this);
        getServer().getPluginManager().registerEvents(gameModeTeamHandler, this);
        getServer().getPluginManager().registerEvents(firstJoinHandler, this);

        // Registrar el comando /ping
        Objects.requireNonNull(this.getCommand("ping")).setExecutor(new PingCommand(this));
        pingMonitor = new PingMonitor(this);
        pingMonitor.startMonitoring();
        getLogger().info("PingMonitor activado.");

        //comandos generales
        Objects.requireNonNull(this.getCommand("spawnvct")).setExecutor(new SpawnMobs(this, dayHandler));
        Objects.requireNonNull(this.getCommand("eggvct")).setExecutor(new EggSpawnerCommand(this));
        ItemsCommands itemsCommands = new ItemsCommands(this);
        getCommand("givevct").setExecutor(itemsCommands);
        getCommand("givevct").setTabCompleter(itemsCommands);


        // Registrar el comando para el temporizador;
        TiempoCommand tiempoCommand = new TiempoCommand(this);
        this.getCommand("addtiempo").setExecutor(tiempoCommand);
        this.getCommand("removetiempo").setExecutor(tiempoCommand);
        this.getCommand("addtiempo").setTabCompleter(tiempoCommand);
        this.getCommand("removetiempo").setTabCompleter(tiempoCommand);

        // Registrar eventos de list
        new VHList(this).runTaskTimer(this, 0, 10);


        // Registrar el comando "giveessence"
        GiveEssenceCommand giveEssenceCommand = new GiveEssenceCommand();
        Objects.requireNonNull(this.getCommand("giveessence")).setExecutor(giveEssenceCommand);

        // Registra la clase EnhancedEnchantmentTable para crear los ítems y recetas
        new EnhancedEnchantmentTable(this);
        // Registra la GUI personalizada
        getServer().getPluginManager().registerEvents(new EnhancedEnchantmentGUI(this), this);

        // Registro de EnchantDelete
        getServer().getPluginManager().registerEvents(new EnchantDelete(this), this);

        // Inicializar RuletaAnimation, TP, Disco y MuerteAnimation
        ruletaAnimation = new RuletaAnimation(this);
        muerteAnimation = new MuerteAnimation(this);
        bonusAnimation = new BonusAnimation(this);
        DiscoCommand discoCommand = new DiscoCommand(this);
        this.getCommand("magic").setExecutor(new MagicTP(this));
        this.getCommand("playdisco").setExecutor(discoCommand);
        this.getCommand("stopdisco").setExecutor(discoCommand);
        getServer().getPluginManager().registerEvents(discoCommand, this);

        // Registrar el comando y su ejecutor
        getCommand("ruletavct").setExecutor(new RuletaCommand(ruletaAnimation));
        getCommand("muertevct").setExecutor(new MuerteCommand(muerteAnimation));
        getCommand("bonusvct").setExecutor(new BonusCommand(bonusAnimation));

        //Registrar CorruptedVillage
        getServer().getPluginManager().registerEvents(new CorruptedVillage(this), this);

        // Inicializa los cambios del día 1
        dayOneChanges = new DayOneChanges(this, dayHandler);
        dayOneChanges.registerCustomRecipe();

        // Inicializa los cambios del día 2
        dayTwoChanges = new DayTwoChanges(this);

        // Inicializa los cambios del día 4
        dayFourChanges = new DayFourChanges(this);

        // Inicializa los cambios del día 6
        daySixChanges = new DaySixChanges(this);

        // Inicializa los cambios del día 10
        dayTenChanges = new DayTenChanges(this);

        //Loottables
        getServer().getPluginManager().registerEvents(new LootHandler(this, dayFourChanges), this);

        // Inicializar EventoHandler y Eventos Generales
                this.eventoHandler = new EventoHandler(this);
                getServer().getPluginManager().registerEvents(eventoHandler, this);

                this.getCommand("start").setExecutor((sender, command, label, args) -> {
                    if (args.length == 1) {
                        switch (args[0].toLowerCase()) {
                            case "evento1":
                                eventoHandler.iniciarEvento();
                                break;
                            case "skybattle":
                                eventoHandler.iniciarSecuenciaInicioSkyBattle();
                                break;
                            case "force":
                                eventoHandler.forzarEvento();
                                break;
                            default:
                                sender.sendMessage("Uso incorrecto del comando. Usa /start <evento1|skybattle|force>");
                        }
                    } else {
                        sender.sendMessage("Uso incorrecto del comando. Usa /start <evento1|skybattle|force>");
                    }
                    return true;
                });

                this.getCommand("end").setExecutor((sender, command, label, args) -> {
                    if (args.length == 1 && args[0].equalsIgnoreCase("evento1")) {
                        eventoHandler.terminarEvento();
                    } else {
                        sender.sendMessage("Uso incorrecto del comando. Usa /end <evento1>");
                    }
                    return true;
                });

        damageLogListener = new DamageLogListener(this);
        getServer().getPluginManager().registerEvents(damageLogListener, this);

        //mobs
        corruptedZombies = new CorruptedZombies(this);

        customDolphin = new CustomDolphin(this);
        getServer().getPluginManager().registerEvents(customDolphin, this);
    }

    @Override
    public void onDisable() {
        Bukkit.getConsoleSender().sendMessage(
                ChatColor.translateAlternateColorCodes('&', Prefix + "&aha sido deshabilitado!, &eVersion: " + Version));

        // Guardar datos de DeathStorm al deshabilitar
        if (deathStormHandler != null) {
            deathStormHandler.saveStormData();
        } else {
            Bukkit.getLogger().severe("deathStormHandler is null, cannot save storm data.");
        }

        if (damageLogListener != null) {
            try {
                damageLogListener.saveDamageLogState();
            } catch (Exception e) {
                getLogger().severe("Error al guardar DamageLogState: " + e.getMessage());
            }
        }

        shuttingDown = true;

    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        String message = ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "۞ " + ChatColor.RESET + ChatColor.LIGHT_PURPLE + ChatColor.BOLD + event.getPlayer().getName() + ChatColor.RESET + ChatColor.LIGHT_PURPLE + " ha entrado a" + ChatColor.RESET + ChatColor.GOLD + ChatColor.BOLD + " Viciont Hardcore 3";
        event.setJoinMessage(message);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        String message = ChatColor.GRAY + "" + ChatColor.BOLD + "۞ " + ChatColor.RESET + ChatColor.GRAY + ChatColor.BOLD + event.getPlayer().getName() + ChatColor.RESET + ChatColor.GRAY + " ha salido de" + ChatColor.RESET + ChatColor.GOLD + ChatColor.BOLD + " Viciont Hardcore 3";
        event.setQuitMessage(message);
    }

    public DayHandler getDayHandler() {
        return dayHandler;
    }

    public DoubleLifeTotem getDoubleLifeTotemHandler() {
        return doubleLifeTotemHandler;
    }

}

