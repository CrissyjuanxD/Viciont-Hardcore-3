package vct.hardcore3;

import Blocks.Endstalactitas;
import Blocks.GuardianShulkerHeart;
import Casino.CasinoManager;
import Commands.*;
import Dificultades.*;
import Dificultades.CustomMobs.*;
import Dificultades.Features.*;
import Enchants.*;
import Events.AchievementParty.AchievementCommands;
import Events.AchievementParty.AchievementGUI;
import Events.AchievementParty.AchievementPartyHandler;
import Events.DamageLogListener;
import Events.ItemParty.ItemPartyHandler;
import Events.MissionSystem.MissionHandler;
import Events.MissionSystem.MissionRewardHandler;
import Events.Skybattle.EventoHandler;
import Events.UltraWitherBattle.UltraWitherEvent;
import Habilidades.*;
import Handlers.*;
import Security.PingMonitor.PingMonitor;
import SlotMachine.SlotMachineManager;
import Structures.StructureCommand;
import TitleListener.*;
import items.*;
import Armors.NightVisionHelmet;
import Armors.CorruptedArmor;
import CorruptedEnd.CorruptedEnd;
import items.Flashlight.FlashlightManager;
import list.VHList;
import mobcap.MobCapManager;
import mobcap.commands.MobCapCommand;
import mobcap.commands.MobCapTabCompleter;
import mobcap.config.MobCapConfig;
import mobcap.spawn.CustomSpawnManager;
import org.bukkit.*;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.plugin.java.JavaPlugin;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import chat.chatgeneral;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class ViciontHardcore3 extends JavaPlugin implements Listener {

    public String Prefix = "&d&lViciont&5&lHardcore &5&l3&7➤ &f";
    public String Version = getDescription().getVersion();
    public static boolean shuttingDown = false;

    // Manejadores de Dificultades

    private DeathStormHandler deathStormHandler;
    private DayHandler dayHandler;
    private SuccessNotification successNotif;
    private ErrorNotification errorNotif;
    private NightmareMechanic nightmareMechanic;

    // Sistema de Misiones
    private MissionHandler missionHandler;
    private MissionSystemCommands missionSystemCommands;
    private MissionRewardHandler missionRewardHandler;

    // Sistema de Tiendas
    private ShopHandler shopHandler;
    private ShopCommand shopCommand;

    // Sistema de Linterna
    private FlashlightManager flashlightManager;

    // Cambios de días

    private DayOneChanges dayOneChanges;
    private DayTwoChanges dayTwoChanges;
    private DayFourChanges dayFourChanges;
    private DayFiveChange dayFiveChanges;
    private DaySevenChanges daySevenChanges;
    private DaySixChanges daySixChanges;
    private DayEightChanges dayEightChanges;
    private DayNineChanges dayNineChanges;
    private DayTenChanges dayTenChanges;
    private DayTwelveChanges dayTwelveChanges;
    private DayThirteenChanges dayThirteenChanges;
    private DayFourteenChanges dayFourteenChanges;
    private DayFifteenChanges dayFifteenChanges;
    private DaySixteenChanges daySixteenChanges;

    // Manejadores de totems e items

    private DoubleLifeTotem doubleLifeTotemHandler;
    private NormalTotemHandler normalTotemHandler;
    private InvulnerableItemProtection invulnerableItemProtection;
    private LifeTotem lifeTotem;
    private SpiderTotem spiderTotem;
    private InfernalTotem infernalTotem;
    private EconomyIceTotem economyIceTotem;
    private EconomyFlyTotem economyFlyTotem;

    // Otros manejadores y listeners

    private PingMonitor pingMonitor;
    private DamageLogListener damageLogListener;
    private EconomyItemsFunctions economyItemsFunctions;
    private MobSoundManager mobSoundManager;
    private GameModeTeamHandler gameModeTeamHandler;
    private CustomSpawnerHandler customSpawnerHandler;
    private ViciontCommands viciontCommands;

    //Mobcap
    private MobCapManager mobCapManager;
    private MobCapConfig config;
    private CustomSpawnManager spawnManager;

    // Habilidades
    private HabilidadesManager habilidadesManager;
    private HabilidadesGUI habilidadesGUI;
    private HabilidadesListener habilidadesListener;
    private HabilidadesEffects habilidadesEffects;

    // Manejadores de Estructuras

    private StructureCommand structureCommand;

    // Eventos

    private EventoHandler eventoHandler;
    private AchievementPartyHandler achievementPartyHandler;
    private AchievementCommands achievementCommands;
    private AchievementGUI achievementGUI;
    private UltraWitherEvent ultraWitherEvent;
    private ItemPartyHandler itemPartyHandler;

    // Casino
    private CasinoManager casinoManager;
    private SlotMachineManager slotMachineManager;

    // Mobs

    private RemoveParticlesCreeper removeParticlesCreeper;
    private CorruptedZombies corruptedZombies;
    private CorruptedInfernalSpider corruptedinfernalSpider;
    private CustomDolphin customDolphin;
    private CorruptedCreeper corruptedCreeper;
    private CustomBoat customBoat;

    // Bosses

    private HellishBeeHandler hellishBeeHandler;
    private InfestedBeeHandler infestedBeeHandler;
    private QueenBeeHandler queenBeeHandler;
    private UltraWitherBossHandler ultraWitherBossHandler;

    // Bloques

    private Endstalactitas endstalactitas;
    private GuardianShulkerHeart guardianShulkerHeart;

    // Armors Y Herramentas

    private NightVisionHelmet nightVisionHelmet;
    private CorruptedArmor corruptedArmor;
    private EnderiteSwordListener enderiteSwordListener;
    private TridenteEspectral tridenteEspectral;

    // Dimension

    private CorruptedEnd corruptedEnd;

    @Override
    public void onEnable() {
        Bukkit.getConsoleSender().sendMessage(
                ChatColor.translateAlternateColorCodes('&', Prefix + "&aha sido habilitado!, &eVersion: " + Version));

        // Registra los eventos de esta clase
        Bukkit.getServer().getPluginManager().registerEvents(this, this);

        // Inicializa correctamente el DayHandler
        dayHandler = new DayHandler(this);

        // DeathStorm
        deathStormHandler = new DeathStormHandler(this, dayHandler);
        getServer().getPluginManager().registerEvents(deathStormHandler, this);

        // Comandos de DeathStorm
        PluginCommand resetCommand = getCommand("resetdeathstorm");
        PluginCommand addCommand = getCommand("adddeathstorm");
        PluginCommand removeCommand = getCommand("removedeathstorm");
        PluginCommand stopDeathStorm = getCommand("stopdeathstorm");

        DeathStormCommand deathStormCommand = new DeathStormCommand(deathStormHandler);
        if (resetCommand != null) resetCommand.setExecutor(deathStormCommand);
        if (addCommand != null) addCommand.setExecutor(deathStormCommand);
        if (removeCommand != null) removeCommand.setExecutor(deathStormCommand);
        if (stopDeathStorm != null) stopDeathStorm.setExecutor(deathStormCommand);

        // Comandos de días
        PluginCommand changeDayCommand = getCommand("cambiardia");

        if (changeDayCommand != null) {
            changeDayCommand.setExecutor(new DayCommandHandler(dayHandler));
        }

        // Cargar datos de DeathStorm al iniciar
        deathStormHandler.loadStormData();

        //BedEvents
        BedEvents bedEvents = new BedEvents(this, dayHandler, deathStormHandler);
        getServer().getPluginManager().registerEvents(bedEvents, this);

        // Registra evento de revivir
        ReviveCommand reviveCommand = new ReviveCommand(this);
        ReviveCoordsCommand reviveCoordsCommand = new ReviveCoordsCommand(this);
        this.getCommand("revive").setExecutor(reviveCommand);
        this.getCommand("revive").setTabCompleter(reviveCommand);
        this.getCommand("revivecoords").setExecutor(reviveCoordsCommand);

        // Inicializa los manejadores de eventos de totems e items
        doubleLifeTotemHandler = new DoubleLifeTotem(this);
        normalTotemHandler = new NormalTotemHandler(this, dayHandler);
        lifeTotem = new LifeTotem(this);
        spiderTotem = new SpiderTotem(this);
        infernalTotem = new InfernalTotem(this);
        economyIceTotem = new EconomyIceTotem(this);
        economyFlyTotem = new EconomyFlyTotem(this);
        invulnerableItemProtection = new InvulnerableItemProtection(this);
        economyItemsFunctions = new EconomyItemsFunctions(this);

        // Registra los eventos de totems e items
        getServer().getPluginManager().registerEvents(normalTotemHandler, this);
        getServer().getPluginManager().registerEvents(doubleLifeTotemHandler, this);
        getServer().getPluginManager().registerEvents(lifeTotem, this);
        getServer().getPluginManager().registerEvents(spiderTotem, this);
        getServer().getPluginManager().registerEvents(infernalTotem, this);
        getServer().getPluginManager().registerEvents(economyIceTotem, this);
        getServer().getPluginManager().registerEvents(economyFlyTotem, this);
        getServer().getPluginManager().registerEvents(invulnerableItemProtection, this);
        getServer().getPluginManager().registerEvents(economyItemsFunctions, this);

        // Registrar eventos de chat y teams
        chatgeneral chatGeneralHandler = new chatgeneral();
        gameModeTeamHandler = new GameModeTeamHandler(this);
        FirstJoinHandler firstJoinHandler = new FirstJoinHandler(this, missionSystemCommands);
        getServer().getPluginManager().registerEvents(chatGeneralHandler, this);
        getServer().getPluginManager().registerEvents(gameModeTeamHandler, this);
        getServer().getPluginManager().registerEvents(firstJoinHandler, this);

        // Registrar el Ping Monitor
        pingMonitor = new PingMonitor(this);
        pingMonitor.startMonitoring();
        getLogger().info("PingMonitor activado.");

        //comandos generales
        Objects.requireNonNull(this.getCommand("spawnvct")).setExecutor(new SpawnMobs(this, dayHandler));
        Objects.requireNonNull(this.getCommand("eggvct")).setExecutor(new EggSpawnerCommand(this));
        ItemsCommands itemsCommands = new ItemsCommands(this);
        customSpawnerHandler = new CustomSpawnerHandler(this, dayHandler);
        new GiveSpawnerCommand(this);
        this.getCommand("reloadcustomspawn").setExecutor(new ReloadCustomSpawnCommand(customSpawnerHandler));
        getCommand("givevct").setExecutor(itemsCommands);
        getCommand("givevct").setTabCompleter(itemsCommands);
        getServer().getPluginManager().registerEvents(customSpawnerHandler, this);


        // Registrar el comando para el temporizador;
        TiempoCommand tiempoCommand = new TiempoCommand(this);
        this.getCommand("addtiempo").setExecutor(tiempoCommand);
        this.getCommand("removetiempo").setExecutor(tiempoCommand);
        this.getCommand("tiempoview").setExecutor(tiempoCommand);
        this.getCommand("addtiempo").setTabCompleter(tiempoCommand);
        this.getCommand("removetiempo").setTabCompleter(tiempoCommand);
        this.getCommand("tiempoview").setTabCompleter(tiempoCommand);

        // Registrar eventos de list
        new VHList(this).runTaskTimer(this, 0, 10);

        // Damage Log Listener
        damageLogListener = new DamageLogListener(this);
        getServer().getPluginManager().registerEvents(damageLogListener, this);

        // Registrar el comando "giveessence"
        GiveEssenceCommand giveEssenceCommand = new GiveEssenceCommand();
        Objects.requireNonNull(this.getCommand("giveessence")).setExecutor(giveEssenceCommand);

        // Registra la clase EnhancedEnchantmentTable para crear los ítems y recetas
        new EnhancedEnchantmentTable(this);
        getServer().getPluginManager().registerEvents(new EnhancedEnchantmentGUI(this), this);
        getServer().getPluginManager().registerEvents(new EnchantDelete(this), this);

        // Inicializar RuletaAnimation, TP, Disco y MuerteAnimation
        RuletaAnimation ruletaAnimation = new RuletaAnimation(this);
        MuerteAnimation muerteAnimation = new MuerteAnimation(this);
        BonusAnimation bonusAnimation = new BonusAnimation(this);
        SuccessNotification successNotif = new SuccessNotification(this);
        ErrorNotification errorNotif = new ErrorNotification(this);
        MuerteHandler muertehandler = new MuerteHandler(this, damageLogListener, deathStormHandler, dayHandler);
        DiscoCommand discoCommand = new DiscoCommand(this);
        this.getCommand("magictp").setExecutor(new MagicTP(this));
        this.getCommand("playdisco").setExecutor(discoCommand);
        this.getCommand("stopdisco").setExecutor(discoCommand);
        getServer().getPluginManager().registerEvents(discoCommand, this);
        getServer().getPluginManager().registerEvents(muertehandler, this);

        // Registrar el comando y su ejecutor
        getCommand("ruletavct").setExecutor(new RuletaCommand(ruletaAnimation));
        getCommand("muertevct").setExecutor(new MuerteCommand(muerteAnimation));
        getCommand("bonusvct").setExecutor(new BonusCommand(bonusAnimation));

        SnowballDamage snowballDamage1 = new SnowballDamage(this);
        FireResistanceHandler fireResistanceHandler = new FireResistanceHandler(this);
        getServer().getPluginManager().registerEvents(snowballDamage1, this);
        getServer().getPluginManager().registerEvents(fireResistanceHandler, this);

        //Registrar Estructuras
        structureCommand = new StructureCommand(this);

        // Inicializa los cambios de días
        dayOneChanges = new DayOneChanges(this, dayHandler);
        dayTwoChanges = new DayTwoChanges(this);
        dayFourChanges = new DayFourChanges(this, dayHandler);
        dayFiveChanges = new DayFiveChange(this, dayHandler);
        daySixChanges = new DaySixChanges(this, dayHandler);
        daySevenChanges = new DaySevenChanges(this, dayHandler);
        dayEightChanges = new DayEightChanges(this, dayHandler);
        dayNineChanges = new DayNineChanges(this, dayHandler);
        dayTenChanges = new DayTenChanges(this, dayHandler);
        dayTwelveChanges = new DayTwelveChanges(this, dayHandler);
        dayThirteenChanges = new DayThirteenChanges(this, dayHandler);
        dayFourteenChanges = new DayFourteenChanges(this, dayHandler);
        dayFifteenChanges = new DayFifteenChanges(this, dayHandler);
        daySixteenChanges = new DaySixteenChanges(this, dayHandler);


        //Nightmare Event
        this.nightmareMechanic = new NightmareMechanic(this, tiempoCommand, successNotif, deathStormHandler, damageLogListener);
        NightmareCommand nightmareCommand = new NightmareCommand(this, nightmareMechanic);
        this.getCommand("addnightmare").setExecutor(nightmareCommand);
        this.getCommand("removenightmare").setExecutor(nightmareCommand);
        this.getCommand("resetnightmarecooldown").setExecutor(nightmareCommand);

        //Loottables
        getServer().getPluginManager().registerEvents(new LootHandler(this), this);

        // Inicializar EventoHandler y Eventos Generales
        this.eventoHandler = new EventoHandler(this);
        this.ultraWitherEvent = new UltraWitherEvent(this, tiempoCommand, successNotif, errorNotif);
        this.achievementPartyHandler = new AchievementPartyHandler(this);
        this.achievementGUI = new AchievementGUI(this, achievementPartyHandler);
        achievementCommands = new AchievementCommands(achievementPartyHandler);
        this.itemPartyHandler = new Events.ItemParty.ItemPartyHandler(this, tiempoCommand);
        getServer().getPluginManager().registerEvents(eventoHandler, this);
        getServer().getPluginManager().registerEvents(ultraWitherEvent, this);
        getServer().getPluginManager().registerEvents(achievementPartyHandler, this);
        getServer().getPluginManager().registerEvents(itemPartyHandler, this);

        this.getCommand("addlogro").setExecutor(achievementCommands);
        this.getCommand("addlogro").setTabCompleter(achievementCommands);
        this.getCommand("removelogro").setExecutor(achievementCommands);
        this.getCommand("removelogro").setTabCompleter(achievementCommands);

        // Sistema de Misiones
        this.missionHandler = new MissionHandler(this, dayHandler);
        missionSystemCommands = new MissionSystemCommands(this, dayHandler);
        missionRewardHandler = new MissionRewardHandler(this, missionSystemCommands.getMissionHandler());
        missionHandler.registerAllMissionListeners();
        /*getServer().getPluginManager().registerEvents(missionHandler, this);*/

        // Sistema de Tiendas
        shopHandler = new ShopHandler(this);
        shopCommand = new ShopCommand(this, shopHandler);
        this.getCommand("spawntienda").setExecutor(shopCommand);
        this.getCommand("spawntienda").setTabCompleter(shopCommand);

        // Sistema de Casino
        casinoManager = new CasinoManager(this);
        slotMachineManager = new SlotMachineManager(this);

        getServer().getScheduler().runTaskLater(this, () -> {
            if (slotMachineManager != null) {
                slotMachineManager.loadSlotMachinesFromFile();
            }
        }, 20L);

        // Sistema de Linterna
        flashlightManager = new FlashlightManager(this);
        this.getCommand("flashlight").setExecutor(new Commands.FlashlightCommand(this, flashlightManager));

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
                    case "reglas":
                        eventoHandler.espera1();
                        break;
                    case "resetpurple":
                        eventoHandler.eliminarPurpleConcrete();
                        break;
                    case "logros":
                        achievementPartyHandler.startEvent(sender);
                        break;
                    case "itemparty":
                        itemPartyHandler.iniciarEvento();
                        break;
                    default:
                        sender.sendMessage("Uso incorrecto del comando. Usa /start <evento1|skybattle|force|reglas|resetpurple|logros|itemparty>");
                }
            } else {
                sender.sendMessage("Uso incorrecto del comando. Usa /start <evento1|skybattle|force|reglas|resetpurple|logros|itemparty>");
            }
            return true;
        });

        this.getCommand("end").setExecutor((sender, command, label, args) -> {
            if (args.length == 1 && args[0].equalsIgnoreCase("evento1")) {
                eventoHandler.terminarEvento();
            } else if (args.length == 1 && args[0].equalsIgnoreCase("logros")) {
                achievementPartyHandler.endEvent(sender);
            } else if (args.length == 1 && args[0].equalsIgnoreCase("itemparty")) {
                itemPartyHandler.terminarEvento();
            } else {
                sender.sendMessage("Uso incorrecto del comando. Usa /end <evento1|logros|itemparty>");
            }
            return true;
        });

        // En tu clase principal, añade esto al registrar comandos
        this.getCommand("evento1").setExecutor((sender, command, label, args) -> {
            if (args.length > 0 && args[0].equalsIgnoreCase("participantes")) {
                eventoHandler.gestionarParticipantes(sender, Arrays.copyOfRange(args, 1, args.length));
                return true;
            }
            sender.sendMessage(ChatColor.RED + "Uso: /evento participantes <list|add|remove> [jugador]");
            return true;
        });

        this.getCommand("reset").setExecutor((sender, command, label, args) -> {
            if (args.length == 1 && args[0].equalsIgnoreCase("logros")) {
                achievementPartyHandler.resetEvent(sender);
                return true;
            } else if (args.length == 1 && args[0].equalsIgnoreCase("itempartyplayers")) {
                itemPartyHandler.resetPlayersFile();
                return true;
            }
            return false;
        });

        this.getCommand("logros").setExecutor((sender, command, label, args) -> {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                achievementGUI.openAchievementGUI(player);
                return true;
            }
            return false;
        });

        this.getCommand("reloadevent").setExecutor((sender, command, label, args) -> {
            if (args.length == 1 && args[0].equalsIgnoreCase("itempartyconfig")) {
                itemPartyHandler.reloadConfig();
                sender.sendMessage("§aConfiguración de ItemParty recargada correctamente.");
                return true;
            }
            return false;
        });

        // manejador de sonidos
        mobSoundManager = new MobSoundManager(this);
        /*getServer().getPluginManager().registerEvents(mobSoundManager, this);*/

        //Manejador de bloques
        endstalactitas = new Endstalactitas(this);
        guardianShulkerHeart = new GuardianShulkerHeart(this);
        getServer().getPluginManager().registerEvents(endstalactitas, this);
        getServer().getPluginManager().registerEvents(guardianShulkerHeart, this);

        //Instancias y registros mobs
        corruptedZombies = new CorruptedZombies(this);
        corruptedCreeper = new CorruptedCreeper(this);
        customDolphin = new CustomDolphin(this);
        customBoat = new CustomBoat(this);
        getServer().getPluginManager().registerEvents(customBoat, this);
        getServer().getPluginManager().registerEvents(customDolphin, this);
        corruptedinfernalSpider = new CorruptedInfernalSpider(this);

        removeParticlesCreeper = new RemoveParticlesCreeper(this);
        getServer().getPluginManager().registerEvents(removeParticlesCreeper, this);

        //CInstancia de Bosses
        infestedBeeHandler = new InfestedBeeHandler(this);
        hellishBeeHandler = new HellishBeeHandler(this);
        queenBeeHandler = new QueenBeeHandler(this);
        ultraWitherBossHandler = new UltraWitherBossHandler(this);

        //Armors y herramientas
        nightVisionHelmet = new NightVisionHelmet(this);
        corruptedArmor = new CorruptedArmor(this);
        enderiteSwordListener = new EnderiteSwordListener(this);
        tridenteEspectral = new TridenteEspectral(this);
        getServer().getPluginManager().registerEvents(nightVisionHelmet, this);
        getServer().getPluginManager().registerEvents(corruptedArmor, this);
        getServer().getPluginManager().registerEvents(enderiteSwordListener, this);
        getServer().getPluginManager().registerEvents(tridenteEspectral, this);

        //MOBCAP
        config = new MobCapConfig(this);
        mobCapManager = MobCapManager.getInstance(this, config);
        spawnManager = new CustomSpawnManager(this, config);

        MobCapCommand commandExecutor = new MobCapCommand(mobCapManager, config);
        MobCapTabCompleter tabCompleter = new MobCapTabCompleter();

        Objects.requireNonNull(getCommand("mobcap")).setExecutor(commandExecutor);
        Objects.requireNonNull(getCommand("mobcap")).setTabCompleter(tabCompleter);
        Objects.requireNonNull(getCommand("mobcapinfo")).setExecutor(commandExecutor);

        Bukkit.getPluginManager().registerEvents(spawnManager, this);

        getLogger().info("Logica de MobCap habilitada correctamente!");

        //Habilidades System
        habilidadesManager = new HabilidadesManager(this);
        habilidadesEffects = new HabilidadesEffects(this);
        habilidadesGUI = new HabilidadesGUI(this, habilidadesManager, dayHandler);
        habilidadesListener = new HabilidadesListener(this, habilidadesManager, habilidadesEffects);

        getServer().getPluginManager().registerEvents(habilidadesGUI, this);
        getServer().getPluginManager().registerEvents(habilidadesListener, this);


        HabilidadesCommand habilidadesCommand = new HabilidadesCommand(habilidadesManager);
        Objects.requireNonNull(getCommand("habilidades")).setExecutor(habilidadesCommand);
        Objects.requireNonNull(getCommand("habilidades")).setTabCompleter(habilidadesCommand);

        getLogger().info("Sistema de Habilidades habilitado correctamente!");

        //Comando Publico
        viciontCommands = new ViciontCommands(this, deathStormHandler, dayHandler);
        getCommand("viciont").setExecutor(viciontCommands);
        getCommand("viciont").setTabCompleter(viciontCommands);

        //Dimensiones Corrupted End

        this.corruptedEnd = new CorruptedEnd(this);
        getServer().getPluginManager().registerEvents(corruptedEnd, this);

        Bukkit.getScheduler().runTaskLater(this, () -> {
            if (Bukkit.getWorld(CorruptedEnd.WORLD_NAME) == null) {
                corruptedEnd.createCorruptedWorld();
            } else {
                corruptedEnd.corruptedWorld = Bukkit.getWorld(CorruptedEnd.WORLD_NAME);
            }

            corruptedEnd.initialize();

        }, 20L);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (corruptedEnd.corruptedWorld != null) {
                    corruptedEnd.spawnParticles();
                }
            }
        }.runTaskTimer(this, 40L, 20L);

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

        if (nightmareMechanic != null) {
            nightmareMechanic.onDisableNightmare();
        } else {
            Bukkit.getLogger().severe("nightmareMechanic is null, cannot disable nightmare.");
        }

        if (customSpawnerHandler != null) {
            customSpawnerHandler.shutdown();
        } else {
            Bukkit.getLogger().severe("customSpawnerHandler is null, cannot disable nightmare.");
        }


        MobCapManager.getInstance(this, config).shutdown();
        economyItemsFunctions.onDisable();

        // Limpiar SlotMachineManager
        if (slotMachineManager != null) {
            slotMachineManager.shutdown();
        }

        // Limpiar MobSoundManager
        if (mobSoundManager != null) {
            mobSoundManager.shutdown();
        }

        // Limpiar sistema de linterna
        if (flashlightManager != null) {
            flashlightManager.shutdown();
        }

/*        if (mobCapManager != null) {
            mobCapManager.shutdown();
        }*/

        // Guardar el estado de los Infested bee
        cleanup();

        // Limpiar Corrupted End
        if (corruptedEnd != null) {
        }

        shuttingDown = true;

    }

    private void cleanup() {
        // Limpiar el handler antes de cancelar tareas
        if (infestedBeeHandler != null) {
            try {
                infestedBeeHandler.shutdown();
                getLogger().info("InfestedBeeHandler limpiado correctamente");
            } catch (Exception e) {
                getLogger().warning("Error al limpiar InfestedBeeHandler: " + e.getMessage());
            }
            infestedBeeHandler = null;
        }

        if (hellishBeeHandler != null) {
            try {
                hellishBeeHandler.shutdown();
                getLogger().info("HellishBeeHandler limpiado correctamente");
            } catch (Exception e) {
                getLogger().warning("Error al limpiar HellishBeeHandler: " + e.getMessage());
            }
            hellishBeeHandler = null;
        }

        if (queenBeeHandler != null) {
            try {
                queenBeeHandler.shutdown();
                getLogger().info("QueenBeeHandler limpiado correctamente");
            } catch (Exception e) {
                getLogger().warning("Error al limpiar QueenBeeHandler: " + e.getMessage());
            }
            queenBeeHandler = null;
        }

        if (ultraWitherBossHandler != null) {
            try {
                ultraWitherBossHandler.shutdown();
                getLogger().info("UltraWitherBossHandler limpiado correctamente");
            } catch (Exception e) {
                getLogger().warning("Error al limpiar UltraWitherBossHandler: " + e.getMessage());
            }
            ultraWitherBossHandler = null;
        }

        // Cancelar todas las tareas pendientes del plugin
        /*Bukkit.getScheduler().cancelTasks(this);*/
    }


    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        String message = ChatColor.WHITE + "\uDB80\uDC65 " + ChatColor.RESET + ChatColor.of("#B83EFF") + ChatColor.BOLD + event.getPlayer().getName() + ChatColor.RESET + ChatColor.of("#FF009F") + " se ha conectado.";
        event.setJoinMessage(message);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        String message = ChatColor.WHITE + "\uDB80\uDC63 " + ChatColor.RESET + ChatColor.of("#B8B8B8") + ChatColor.BOLD + event.getPlayer().getName() + ChatColor.RESET + ChatColor.of("#7C7981") + " se ha desconectado.";
        event.setQuitMessage(message);
    }

    public DayHandler getDayHandler() {
        return dayHandler;
    }

    public DoubleLifeTotem getDoubleLifeTotemHandler() {
        return doubleLifeTotemHandler;
    }

    public SuccessNotification getSuccessNotifier() {
        return successNotif;
    }

    public SlotMachineManager getSlotMachineManager() {
        return slotMachineManager;
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        if (mobCapManager != null && mobCapManager.isInitialized()) {
            Bukkit.getScheduler().runTaskLater(this, () -> {
                mobCapManager.handleNewWorld(event.getWorld());
            }, 20L);
        }
    }

    public MobCapManager getMobCapManager() {
        return mobCapManager;
    }

    public MobCapConfig getMobCapConfig() {
        return config;
    }

    public CustomSpawnManager getSpawnManager() {
        return spawnManager;
    }

}