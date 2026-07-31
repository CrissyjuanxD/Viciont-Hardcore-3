package vct.hardcore3;

import Blocks.Endstalactitas;
import Blocks.GuardianShulkerHeart;
import Casino.CasinoCommands;
import Casino.CasinoManager;
import Commands.*;
import CorrupcionAnsiosa.*;
import Dificultades.CustomMobs.*;
import Dificultades.Features.*;
import Dificultades.Spawning.MobSpawnDefinitions;
import Dificultades.Spawning.MobSpawnRegistry;
import EffectListener.*;
import Enchants.*;
import Events.AchievementParty.AchievementCommands;
import Events.AchievementParty.AchievementPartyHandler;
import Gui.*;
import Gui.vithiums.VithiumsCommand;
import Gui.vithiums.VithiumsManager;
import Handlers.DamageLogListener;
import Events.HotPotato.HotPotatoCommand;
import Events.HotPotato.HotPotatoHandler;
import Events.ItemParty.ItemPartyCommand;
import Events.ItemParty.ItemPartyHandler;
import Events.MissionSystem.MissionCommands;
import Events.MissionSystem.MissionHandler;
import Events.MissionSystem.MissionRewardHandler;
import Events.Skybattle.EventoHandler;
import Events.Skybattle.LavaClashCommand;
import Events.UltraWitherBattle.UltraWitherEvent;
import Habilidades.*;
import Handlers.*;
import InfestedCaves.*;
import Managers.ItemManager;
import Managers.MobManager;
import RunicSmithing.RunicSmithingGUI;
import ShopSystem.*;
import SlotMachine.SlotMachineManager;
import StatueManager.*;
import Structures.StructureCommand;
import TitleListener.*;
import items.*;
import Armors.NightVisionHelmet;
import Armors.CorruptedArmor;
import CorruptedEnd.CorruptedEnd;
import CorruptedEnd.CorruptedEndBiomeRegistry;
import items.Flashlight.FlashlightManager;
import list.VHList;
import mobcap.MobCapManager;
import mobcap.commands.MobCapCommand;
import mobcap.commands.MobCapTabCompleter;
import mobcap.config.MobCapConfig;
import mobcap.spawn.CustomSpawnManager;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.command.PluginCommand;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.plugin.java.JavaPlugin;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import chat.chatgeneral;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Objects;

public class ViciontHardcore3 extends JavaPlugin implements Listener {

    // ------------------------------------------------------------------------
    //  Constantes y estado básico
    // ------------------------------------------------------------------------

    public String Prefix = "&d&lViciont&5&lHardcore &5&l3&7➤ &f";
    public String Version;
    public static boolean shuttingDown = false;

    private static ViciontHardcore3 instance;

    // ------------------------------------------------------------------------
    //  Core / Dificultades principales
    // ------------------------------------------------------------------------

    private DayHandler dayHandler;
    private DeathStormHandler deathStormHandler;
    private NightmareMechanic nightmareMechanic;

    private DatabaseManager databaseManager;
    private TeamsHandler teamsHandler;

    private SuccessNotification successNotif;
    private ErrorNotification errorNotif;

    private TiempoCommand tiempoCommand;
    private CustomEffectManager effectManager;

    private MainMenuManager mainMenuManager;
    private MisionesGuiManager misionesGuiManager;
    private CambiosDataManager cambiosDataManager;
    private EntidadesGuiManager entidadesGuiManager;

    private MuerteAnimation muerteAnimation;
    private RuletaAnimation ruletaAnimation;

    private MobManager mobManager;
    private ItemManager itemManager;

    // ------------------------------------------------------------------------
    //  Sistemas de Misiones / Tiendas / Linterna
    // ------------------------------------------------------------------------

    private MissionHandler missionHandler;
    private MissionRewardHandler missionRewardHandler;

/*    private ShopHandler shopHandler;
    private ShopCommand shopCommand;*/

    private FlashlightManager flashlightManager;
    private AltarFunctions altarFunctions;

    // ------------------------------------------------------------------------
    //  Corrupción Ansiosa / Tótems / Items
    // ------------------------------------------------------------------------

    private CorrupcionAnsiosaManager corruptionManager;
    private CorrupcionEffectsHandler corruptionEffectsHandler;

    private DoubleLifeTotem doubleLifeTotemHandler;
    private NormalTotemHandler normalTotemHandler;
    private InvulnerableItemProtection invulnerableItemProtection;
    private LifeTotem lifeTotem;
    private SpiderTotem spiderTotem;
    private InfernalTotem infernalTotem;
    private EconomyIceTotem economyIceTotem;
    private EconomyFlyTotem economyFlyTotem;
    private EconomyItemsFunctions economyItemsFunctions;
    private ItemsEventos itemsEventos;

    private VithiumsManager vithiumsManager;

    // ------------------------------------------------------------------------
    //  Ping / Sonidos / Teams / Spawners
    // ------------------------------------------------------------------------

    private DamageLogListener damageLogListener;
    private MobSoundManager mobSoundManager;
    private GameModeTeamHandler gameModeTeamHandler;
    private CustomSpawnerHandler customSpawnerHandler;
    private TrialSpawnerHandler trialSpawnerHandler;
    private ViciontCommands viciontCommands;

    // ------------------------------------------------------------------------
    //  Mobcap
    // ------------------------------------------------------------------------

    private MobCapManager mobCapManager;
    private MobCapConfig config;
    private CustomSpawnManager spawnManager;

    // ------------------------------------------------------------------------
    //  Habilidades
    // ------------------------------------------------------------------------

    private HabilidadesManager habilidadesManager;
    private HabilidadesGUI habilidadesGUI;
    private HabilidadesListener habilidadesListener;
    private HabilidadesEffects habilidadesEffects;
    private CustomItemRegistry customItemRegistry;

    // ------------------------------------------------------------------------
    //  Eventos
    // ------------------------------------------------------------------------

    private EventoHandler eventoHandler;
    private AchievementPartyHandler achievementPartyHandler;
    private AchievementCommands achievementCommands;
    private UltraWitherEvent ultraWitherEvent;
    private ItemPartyHandler itemPartyHandler;
    private HotPotatoHandler hotPotatoHandler;

    // ------------------------------------------------------------------------
    //  Casino
    // ------------------------------------------------------------------------

    private CasinoManager casinoManager;
    private SlotMachineManager slotMachineManager;

    // ------------------------------------------------------------------------
    //  Mobs / Bosses / Entidades
    // ------------------------------------------------------------------------
    private RemoveParticlesCreeper removeParticlesCreeper;
    private CustomDolphin customDolphin;
    private CustomBoat customBoat;
    private SpawnerInfestedGolems spawnerInfestedGolems;
    private InfestedGolems infestedGolems;

    /*private HellishBeeHandler hellishBeeHandler;*/
    private InfestedBeeHandler infestedBeeHandler;
    /*private QueenBeeHandler queenBeeHandler;*/
    private UltraWitherBossHandler ultraWitherBossHandler;

    private StatueDebugManager debugManager;
    private StatueSchematic statueSchematic;

    // ------------------------------------------------------------------------
    //  Bloques / Armaduras
    // ------------------------------------------------------------------------

    private Endstalactitas endstalactitas;
    private GuardianShulkerHeart guardianShulkerHeart;

    private NightVisionHelmet nightVisionHelmet;
    private CorruptedArmor corruptedArmor;
    private EnderiteSwordListener enderiteSwordListener;
    private TridenteEspectral tridenteEspectral;

    // ------------------------------------------------------------------------
    //  Dimensiones
    // ------------------------------------------------------------------------

    private CorruptedEnd corruptedEnd;

    public static final String WORLD_NAME = "infested_caves";
    private InfestedGenerator generator;
    private PortalManager portalManager;
    private InfestedListeners listeners;
    private InfestedCaveAmbient infestedAmbient;
    private StructureManager structureManager;
    private Null_Runes nullRunes;

    // ------------------------------------------------------------------------
    //  Inicipialización y apagado
    // ------------------------------------------------------------------------

    @Override
    public void onEnable() {
        instance = this;
        this.Version = getDescription().getVersion();

        logStartup();
        registerBaseListeners();
        saveDefaultConfig();

        this.databaseManager = new DatabaseManager(this);
        this.teamsHandler = new TeamsHandler();
        this.teamsHandler.loadTeams();

        initViciontGuis();
        initMobSoundSystem();
        initCoreDayAndDeathStormSystem();
        SpawnSystem();
        initViciontGuisItems();
        initTiempoSystem();
        initCorruptionSystem();
        itemandmobManager();
        initItemsSystem();
        initMissionSystem();
        initChatTeamsAndFirstJoinSystem();
        initAltarSystem();
        initGeneralCommandsAndCustomSpawners();
        initAsyncAndUtilitySystems();
        initEnchantSystem();
        initAnimationAndTitleSystem();
        initStructureSystem();
        statueEffectSystem();
        initGameplaySystem();
        initLootSystem();
        initHabilidadesSystem();
        initEventsSystem();
        initEventCommandsSystem();
        initShopSystem();
        initCasinoSystem();
        initVithiumsSystem();
        initFlashlightSystem();
        initBlocksSystem();
        initMobsAndBossesSystem();
        initMobCapSystem();
        initPublicCommandSystem();
        initCorruptedEndSystem();
        initInfestedCavesDimension();

        getLogger().info("ViciontHardcore3 habilitado completamente.");
    }

    @Override
    public void onDisable() {
        Bukkit.getConsoleSender().sendMessage(
                ChatColor.translateAlternateColorCodes('&',
                        Prefix + "&aha sido deshabilitado!, &eVersion: " + Version));

        // Guardar datos de DeathStorm
        if (deathStormHandler != null) {
            deathStormHandler.saveStormData();
        } else {
            Bukkit.getLogger().severe("deathStormHandler is null, cannot save storm data.");
        }

        // Guardar DamageLog
        if (damageLogListener != null) {
            try {
                damageLogListener.saveDamageLogState();
            } catch (Exception e) {
                getLogger().severe("Error al guardar DamageLogState: " + e.getMessage());
            }
        }

        // Apagar Nightmare
        if (nightmareMechanic != null) {
            nightmareMechanic.onDisableNightmare();
        } else {
            Bukkit.getLogger().severe("nightmareMechanic is null, cannot disable nightmare.");
        }

        // Custom Spawners
        if (customSpawnerHandler != null) {
            customSpawnerHandler.shutdown();
        } else {
            Bukkit.getLogger().severe("customSpawnerHandler is null, cannot shutdown custom spawners.");
        }

        if (trialSpawnerHandler != null) {
            trialSpawnerHandler.shutdown();
        } else {
            Bukkit.getLogger().severe("trialSpawnerHandler is null, cannot shutdown custom spawners.");
        }

        // MobCap
        if (config != null) {
            MobCapManager.getInstance(this, config).shutdown();
        }


        // Casino
        if (slotMachineManager != null) {
            slotMachineManager.shutdown();
        }

        if (vithiumsManager != null) {
            vithiumsManager.saveConfig();
        }

        // Sonidos
        if (mobSoundManager != null) {
            mobSoundManager.shutdown();
        }

        // Linterna
        if (flashlightManager != null) {
            flashlightManager.shutdown();
        }

        // Corrupción Ansiosa
        if (corruptionManager != null) {
            corruptionManager.saveData();
        }

        if (effectManager != null) {
            effectManager.cleanupAllEffects();
        }

        if (spawnerInfestedGolems != null) {
            spawnerInfestedGolems.shutdown();
        }
        if (infestedGolems != null) {
            infestedGolems.revert();
        }

        if (missionHandler != null) {
            missionHandler.forceSaveAllOnShutdown();
        }

        if (debugManager != null && statueSchematic != null) {
            debugManager.cleanup();
            statueSchematic.cleanup();
        }


        // Limpieza de bosses/abejas
        cleanupBossHandlers();

        // Corrupted End: aquí podrías añadir corruptedEnd.shutdown() si la implementas
        // if (corruptedEnd != null) corruptedEnd.shutdown();

        shuttingDown = true;
    }

    // ------------------------------------------------------------------------
    //  Inicialización por sistemas (onEnable)
    // ------------------------------------------------------------------------

    private void logStartup() {
        Bukkit.getConsoleSender().sendMessage(
                ChatColor.translateAlternateColorCodes('&',
                        Prefix + "&aha sido habilitado!, &eVersion: " + Version));
    }

    private void registerBaseListeners() {
        Bukkit.getServer().getPluginManager().registerEvents(this, this);
    }

    private void initCoreDayAndDeathStormSystem() {
        // DayHandler
        dayHandler = new DayHandler(this);

        // DeathStorm
        deathStormHandler = new DeathStormHandler(this, dayHandler);
        Bukkit.getPluginManager().registerEvents(deathStormHandler, this);

        // Comandos DeathStorm
        PluginCommand deathStormCmd = getCommand("deathstorm");
        if (deathStormCmd != null) {
            DeathStormCommand executor = new DeathStormCommand(deathStormHandler);
            deathStormCmd.setExecutor(executor);
            deathStormCmd.setTabCompleter(executor);
        }

        // Comando cambio de día
        PluginCommand changeDayCommand = getCommand("cambiardia");
        if (changeDayCommand != null) {
            changeDayCommand.setExecutor(new DayCommandHandler(dayHandler));
        }

        // Cargar datos existentes
        deathStormHandler.loadStormData();
    }

    private void SpawnSystem() {
        MobSpawnRegistry spawnRegistry = new MobSpawnRegistry(this, dayHandler, deathStormHandler);
        Bukkit.getPluginManager().registerEvents(spawnRegistry, this);

        MobSpawnDefinitions spawnDefinitions = new MobSpawnDefinitions(this, spawnRegistry, dayHandler, deathStormHandler);
        spawnDefinitions.registerAll();
    }

    // Método exclusivo para iniciar nuestras clases GUI
    private void initViciontGuis() {
        this.mainMenuManager = new MainMenuManager(this);

        //CAMBIOS
        this.cambiosDataManager = new CambiosDataManager(this, this.databaseManager);
        getServer().getPluginManager().registerEvents(new CambiosJoinListener(this.cambiosDataManager), this);

        CambiosCommand cambiosCommand = new CambiosCommand(cambiosDataManager);
        Objects.requireNonNull(getCommand("cambios")).setExecutor(cambiosCommand);
        Objects.requireNonNull(getCommand("cambios")).setTabCompleter(cambiosCommand);

        this.entidadesGuiManager = new EntidadesGuiManager(this);

        new CambiosGuiManager(this.cambiosDataManager);

        getLogger().info("Sistemas de ViciontGuis (Menú, Misiones, Entidades, Recetas, Cambios) activados en el Plugin.");
    }
    private void initViciontGuisItems() {
        new ItemsGuiManager(this.dayHandler);
    }

    private void initMobSoundSystem() {
        mobSoundManager = new MobSoundManager(this);
    }

    private void initTiempoSystem() {
        // Registrar el comando para el temporizador
        tiempoCommand = new TiempoCommand(this);

        Objects.requireNonNull(getCommand("timers")).setExecutor(tiempoCommand);
        Objects.requireNonNull(getCommand("timers")).setTabCompleter(tiempoCommand);
    }

    private void initCorruptionSystem() {
        corruptionManager = new CorrupcionAnsiosaManager(this);
        corruptionEffectsHandler = new CorrupcionEffectsHandler(this, corruptionManager);

        Bukkit.getPluginManager().registerEvents(new CorrupcionJoinListener(corruptionManager), this);
        Bukkit.getPluginManager().registerEvents(new CorrupcionAnsiosaConsumiblesListener(corruptionManager), this);

        new CorrupcionHudManager(this, corruptionManager);

        Objects.requireNonNull(getCommand("ca"))
                .setExecutor(new CorrupcionAnsiosaCommand(corruptionManager));

        getLogger().info("Corrupción Ansiosa inicializada correctamente.");
    }

    private void itemandmobManager() {
        itemManager = new ItemManager(this);
        mobManager = new MobManager(this, dayHandler);
    }

    private void initItemsSystem() {
        // Tótems protección de ítems Armor y Herramientas
        doubleLifeTotemHandler = new DoubleLifeTotem(this);
        normalTotemHandler = new NormalTotemHandler(this, dayHandler, corruptionManager, corruptionEffectsHandler);
        lifeTotem = new LifeTotem(this);
        spiderTotem = new SpiderTotem(this);
        infernalTotem = new InfernalTotem(this);
        economyIceTotem = new EconomyIceTotem(this);
        economyFlyTotem = new EconomyFlyTotem(this);
        invulnerableItemProtection = new InvulnerableItemProtection(this);
        economyItemsFunctions = new EconomyItemsFunctions(this, databaseManager);
        nightVisionHelmet = new NightVisionHelmet(this);
        corruptedArmor = new CorruptedArmor(this);
        enderiteSwordListener = new EnderiteSwordListener(this);
        tridenteEspectral = new TridenteEspectral(this);
        itemsEventos = new ItemsEventos(this);

        Bukkit.getPluginManager().registerEvents(normalTotemHandler, this);
        Bukkit.getPluginManager().registerEvents(doubleLifeTotemHandler, this);
        Bukkit.getPluginManager().registerEvents(lifeTotem, this);
        Bukkit.getPluginManager().registerEvents(spiderTotem, this);
        Bukkit.getPluginManager().registerEvents(infernalTotem, this);
        Bukkit.getPluginManager().registerEvents(economyIceTotem, this);
        Bukkit.getPluginManager().registerEvents(economyFlyTotem, this);
        Bukkit.getPluginManager().registerEvents(invulnerableItemProtection, this);
        Bukkit.getPluginManager().registerEvents(economyItemsFunctions, this);
        Bukkit.getPluginManager().registerEvents(nightVisionHelmet, this);
        Bukkit.getPluginManager().registerEvents(corruptedArmor, this);
        Bukkit.getPluginManager().registerEvents(enderiteSwordListener, this);
        Bukkit.getPluginManager().registerEvents(tridenteEspectral, this);
        Bukkit.getPluginManager().registerEvents(itemsEventos, this);

        getCommand("mochilas").setExecutor(new MochilaCommand(economyItemsFunctions));
        getCommand("delmochilas").setExecutor(new MochilaCommand(economyItemsFunctions));
    }

    private void initVithiumsSystem() {
        this.vithiumsManager = new VithiumsManager(this, databaseManager);

        VithiumsCommand vithiumsCommand = new VithiumsCommand(this.vithiumsManager);
        Objects.requireNonNull(getCommand("vithiums")).setExecutor(vithiumsCommand);
        Objects.requireNonNull(getCommand("vithiums")).setTabCompleter(vithiumsCommand);

        getLogger().info("Sistema de Vithiums habilitado correctamente.");
    }

    private void initMissionSystem() {
        // Handler principal de misiones
        this.missionHandler = new MissionHandler(this, databaseManager, dayHandler);

        this.misionesGuiManager = new MisionesGuiManager(this.missionHandler);

        MissionCommands missionCommands = new MissionCommands(this.missionHandler, this.misionesGuiManager);

        Objects.requireNonNull(getCommand("missions")).setExecutor(missionCommands);
        Objects.requireNonNull(getCommand("misiones")).setExecutor(missionCommands);
        Objects.requireNonNull(getCommand("missions")).setTabCompleter(missionCommands);

        this.missionRewardHandler = new MissionRewardHandler(this, missionHandler);

        this.missionHandler.registerAllMissionListeners();
        Objects.requireNonNull(getCommand("testtoast")).setExecutor(new testtiastcommand(this));

    }

    private void initChatTeamsAndFirstJoinSystem() {
        chatgeneral chatGeneralHandler = new chatgeneral();
        gameModeTeamHandler = new GameModeTeamHandler(this);

        FirstJoinHandler firstJoinHandler = new FirstJoinHandler(this, missionHandler, corruptionManager, databaseManager, teamsHandler);

        Bukkit.getPluginManager().registerEvents(chatGeneralHandler, this);
        Bukkit.getPluginManager().registerEvents(firstJoinHandler, this);
        Bukkit.getPluginManager().registerEvents(gameModeTeamHandler, this);
    }

    private void initGeneralCommandsAndCustomSpawners() {
        // spawnvct
        Objects.requireNonNull(this.getCommand("spawnvct"))
                .setExecutor(new SpawnMobs(this, mobManager));

        // Items generales
        ItemsCommands itemsCommands = new ItemsCommands(this, itemManager);

        // Spawners custom
        customSpawnerHandler = new CustomSpawnerHandler(this, dayHandler, mobManager);
        new GiveSpawnerCommand(this,mobManager); // Se registra dentro del constructor

        Objects.requireNonNull(this.getCommand("reloadcustomspawn"))
                .setExecutor(new ReloadCustomSpawnCommand(customSpawnerHandler));

        trialSpawnerHandler = new TrialSpawnerHandler(this, itemManager, mobManager);
        Bukkit.getPluginManager().registerEvents(new SpawnNotifier(mobManager), this);
        Bukkit.getPluginManager().registerEvents(trialSpawnerHandler, this);

        // Registramos el comando de Trial Spawner (que le dará el ítem al jugador)
        new TrialSpawnerCommand(this, mobManager);

        Objects.requireNonNull(this.getCommand("givevct")).setExecutor(itemsCommands);
        Objects.requireNonNull(this.getCommand("givevct")).setTabCompleter(itemsCommands);

        getCommand("anuncio").setExecutor(new AnuncioCommand());

        Bukkit.getPluginManager().registerEvents(customSpawnerHandler, this);

        getCommand("viciontreload").setExecutor(new ViciontReloadCommand(this, databaseManager));

        //revive
        ReviveCommand reviveCommand = new ReviveCommand(this);
        ReviveCoordsCommand reviveCoordsCommand = new ReviveCoordsCommand(this);
        this.getCommand("revive").setExecutor(reviveCommand);
        this.getCommand("revive").setTabCompleter(reviveCommand);
        this.getCommand("revivecoords").setExecutor(reviveCoordsCommand);
    }

    private void initAsyncAndUtilitySystems() {
        // Lista VHList
        new VHList(this, dayHandler);

        // Damage log
        damageLogListener = new DamageLogListener(this);
        Bukkit.getPluginManager().registerEvents(damageLogListener, this);

        LootManager lootManager = new LootManager(this);
        LootCommand cmdExecutor = new LootCommand(lootManager);

        PluginCommand cmd = getCommand("LootTableVC");
        if (cmd != null) {
            cmd.setExecutor(cmdExecutor);
            cmd.setTabCompleter(cmdExecutor);
        }
    }

    private void initAltarSystem() {
        this.altarFunctions = new AltarFunctions(this);
        getLogger().info("Sistema de Altares y Cooldowns persistentes cargado.");
    }

    private void initEnchantSystem() {
        new EnhancedEnchantmentTable(this);

        Bukkit.getPluginManager().registerEvents(new EnhancedEnchantmentGUI(this, dayHandler), this);

        Bukkit.getPluginManager().registerEvents(new EnchantDelete(this), this);
        new RunicSmithingGUI(this);
    }

    private void initAnimationAndTitleSystem() {
        ruletaAnimation = new RuletaAnimation(this);
        muerteAnimation = new MuerteAnimation(this);
        BonusAnimation bonusAnimation = new BonusAnimation(this);

        successNotif = new SuccessNotification(this);
        errorNotif = new ErrorNotification(this);

        MuerteHandler muertehandler = new MuerteHandler(this, damageLogListener, deathStormHandler, dayHandler);

        // Comandos de teleport / disco
        DiscoCommand discoCommand = new DiscoCommand(this);
        Objects.requireNonNull(this.getCommand("magictp")).setExecutor(new MagicTP(this));
        Objects.requireNonNull(this.getCommand("playdisco")).setExecutor(discoCommand);
        Objects.requireNonNull(this.getCommand("stopdisco")).setExecutor(discoCommand);

        Bukkit.getPluginManager().registerEvents(discoCommand, this);
        Bukkit.getPluginManager().registerEvents(muertehandler, this);

        RuletaCommand ruletaCmd = new RuletaCommand(ruletaAnimation, this.cambiosDataManager);
        Objects.requireNonNull(this.getCommand("ruletavct")).setExecutor(ruletaCmd);
        Objects.requireNonNull(this.getCommand("ruletavct")).setTabCompleter(ruletaCmd);

        // Comandos de muerte / bonus
        Objects.requireNonNull(this.getCommand("muertevct"))
                .setExecutor(new MuerteCommand(muerteAnimation));
        Objects.requireNonNull(this.getCommand("bonusvct"))
                .setExecutor(new BonusCommand(bonusAnimation));

        // Listeners adicionales
        SnowballDamage snowballDamage1 = new SnowballDamage(this);
        FireResistanceHandler fireResistanceHandler = new FireResistanceHandler(this);
        Bukkit.getPluginManager().registerEvents(snowballDamage1, this);
        Bukkit.getPluginManager().registerEvents(fireResistanceHandler, this);
    }

    private void initStructureSystem() {
        // --- CAMBIO AQUÍ: Le pasamos ruletaAnimation al StructureCommand ---
        new StructureCommand(this, ruletaAnimation, cambiosDataManager);
    }

    private void statueEffectSystem() {
        StatueManager statueManager = new StatueManager(this);
        this.debugManager    = new StatueDebugManager(this, statueManager);
        this.statueSchematic = new StatueSchematic(this, statueManager);
        this.statueSchematic.scanAllWorlds();

        StatueGUI statueGUI = new StatueGUI(this, statueManager);
        StatueListener statueListener = new StatueListener(this, statueManager, statueGUI, statueSchematic);

        StatueCommand statueCommand = new StatueCommand(debugManager);
        getCommand("statue").setExecutor(statueCommand);
        getCommand("statue").setTabCompleter(statueCommand);

        // Registrar Eventos
        getServer().getPluginManager().registerEvents(statueListener, this);
        getServer().getPluginManager().registerEvents(statueGUI, this);
        getServer().getPluginManager().registerEvents(statueSchematic, this);

        // Cargar estatuas ya existentes en el mundo (por si hubo reload)
        statueManager.loadStatues();
    }

    private void initGameplaySystem() {
        //Nightmare
        nightmareMechanic = new NightmareMechanic(this, tiempoCommand, successNotif, deathStormHandler, damageLogListener);

        //Efectos Custom
        this.effectManager = new CustomEffectManager();

        ConfusionEffect confusionEffect = new ConfusionEffect(this);
        CorruptureEffect corruptureEffect = new CorruptureEffect(this);
        EcoMuertoEffect ecoMuertoEffect = new EcoMuertoEffect(this);
        CorrupcionEffect corrupcionEffect = new CorrupcionEffect(this);
        DrenajeEffect drenajeEffect = new DrenajeEffect(this);

        effectManager.registerEffect(confusionEffect);
        effectManager.registerEffect(corruptureEffect);
        effectManager.registerEffect(ecoMuertoEffect);
        effectManager.registerEffect(corrupcionEffect);
        effectManager.registerEffect(drenajeEffect);

        getServer().getPluginManager().registerEvents(effectManager, this);
        getServer().getPluginManager().registerEvents(corruptureEffect, this);
        getServer().getPluginManager().registerEvents(ecoMuertoEffect, this);
        getServer().getPluginManager().registerEvents(corrupcionEffect, this);
        getServer().getPluginManager().registerEvents(drenajeEffect, this);

        EffectPreventionListener effectPreventionListener = new EffectPreventionListener();
        getServer().getPluginManager().registerEvents(effectPreventionListener, this);

        TotemEffectRestorer totemEffectRestorer = new TotemEffectRestorer(this);
        getServer().getPluginManager().registerEvents(totemEffectRestorer, this);

        // Eventos de cama
        BedEvents bedEvents = new BedEvents(this, dayHandler, deathStormHandler, nightmareMechanic);
        Bukkit.getPluginManager().registerEvents(bedEvents, this);

        // Comandos
        NightmareCommand nightmareCommand = new NightmareCommand(this, nightmareMechanic);
        Objects.requireNonNull(this.getCommand("addnightmare")).setExecutor(nightmareCommand);
        Objects.requireNonNull(this.getCommand("removenightmare")).setExecutor(nightmareCommand);
        Objects.requireNonNull(this.getCommand("resetnightmarecooldown")).setExecutor(nightmareCommand);
        Objects.requireNonNull(this.getCommand("levelnightmare")).setExecutor(nightmareCommand);
    }

    private void initLootSystem() {
        Bukkit.getPluginManager().registerEvents(new LootHandler(this), this);
    }

    private void initEventsSystem() {
        eventoHandler = new EventoHandler(this, habilidadesManager, habilidadesEffects);
        ultraWitherEvent = new UltraWitherEvent(this, tiempoCommand, successNotif, errorNotif);
        achievementPartyHandler = new AchievementPartyHandler(this, databaseManager);
        achievementCommands = new AchievementCommands(achievementPartyHandler);
        itemPartyHandler = new ItemPartyHandler(this, tiempoCommand);
        hotPotatoHandler = new HotPotatoHandler(this, tiempoCommand, habilidadesManager, habilidadesEffects);

        EventInventoryManager invManager = new EventInventoryManager(this, databaseManager);

        eventoHandler.setEventInventoryManager(invManager);
        hotPotatoHandler.setEventInventoryManager(invManager);

        Bukkit.getPluginManager().registerEvents(eventoHandler, this);
        Bukkit.getPluginManager().registerEvents(ultraWitherEvent, this);
        Bukkit.getPluginManager().registerEvents(achievementPartyHandler, this);
        Bukkit.getPluginManager().registerEvents(itemPartyHandler, this);
        Bukkit.getPluginManager().registerEvents(hotPotatoHandler, this);

        // Comandos de logros
        Objects.requireNonNull(this.getCommand("logros")).setExecutor(achievementCommands);
        Objects.requireNonNull(this.getCommand("logros")).setTabCompleter(achievementCommands);
    }

    private void initEventCommandsSystem() {
        LavaClashCommand lavaCmd = new LavaClashCommand(eventoHandler);
        getCommand("lavaclash").setExecutor(lavaCmd);
        getCommand("lavaclash").setTabCompleter(lavaCmd);

        // ItemParty
        ItemPartyCommand itemPartyCmd = new ItemPartyCommand(itemPartyHandler);
        getCommand("itemparty").setExecutor(itemPartyCmd);
        getCommand("itemparty").setTabCompleter(itemPartyCmd);

        HotPotatoCommand hotPotatoCmd = new HotPotatoCommand(hotPotatoHandler);
        getCommand("hotpotato").setExecutor(hotPotatoCmd);
        getCommand("hotpotato").setTabCompleter(hotPotatoCmd);
    }

    private void initShopSystem() {
/*        shopHandler = new ShopHandler(this);
        shopCommand = new ShopCommand(this, shopHandler);

        Objects.requireNonNull(this.getCommand("spawntienda")).setExecutor(shopCommand);
        Objects.requireNonNull(this.getCommand("spawntienda")).setTabCompleter(shopCommand);*/
        CustomItemRegistry.init(this, itemManager);

        //Inicializar Shop System
        ShopManager shopManager = new ShopManager(this);
        ShopGUI shopGUI = new ShopGUI(shopManager);
        ShopCommands shopCommands = new ShopCommands(shopManager, shopGUI);
        ShopListeners shopListeners = new ShopListeners(shopManager, shopGUI);

        //Registrar Eventos y Comandos
        getServer().getPluginManager().registerEvents(shopListeners, this);
        getCommand("spawnshop").setExecutor(shopCommands);
        getCommand("removeshop").setExecutor(shopCommands);
        getCommand("trade").setExecutor(shopCommands);
        getCommand("trade").setTabCompleter(shopCommands);
    }

    private void initCasinoSystem() {
        casinoManager = new CasinoManager(this);
        slotMachineManager = new SlotMachineManager(this, itemManager);

        Bukkit.getScheduler().runTaskLater(this, () -> {
            if (slotMachineManager != null) {
                slotMachineManager.loadSlotMachinesFromFile();
            }
        }, 20L);

        getCommand("casino").setExecutor(new CasinoCommands(casinoManager));
    }

    private void initFlashlightSystem() {
        flashlightManager = new FlashlightManager(this);
        Objects.requireNonNull(this.getCommand("flashlight"))
                .setExecutor(new Commands.FlashlightCommand(this, flashlightManager));
    }

    private void initBlocksSystem() {
        endstalactitas = new Endstalactitas(this);
        guardianShulkerHeart = new GuardianShulkerHeart(this);

        Bukkit.getPluginManager().registerEvents(endstalactitas, this);
        Bukkit.getPluginManager().registerEvents(guardianShulkerHeart, this);
    }

    private void initMobsAndBossesSystem() {
        customDolphin = new CustomDolphin(this);
        customBoat = new CustomBoat(this);

        infestedGolems = new InfestedGolems(this);
        infestedGolems.apply();
        spawnerInfestedGolems = new SpawnerInfestedGolems(this, infestedGolems);

        Bukkit.getPluginManager().registerEvents(customBoat, this);
        Bukkit.getPluginManager().registerEvents(customDolphin, this);

        removeParticlesCreeper = new RemoveParticlesCreeper(this);
        Bukkit.getPluginManager().registerEvents(removeParticlesCreeper, this);
        //Booses
        infestedBeeHandler = new InfestedBeeHandler(this);
        /*hellishBeeHandler = new HellishBeeHandler(this);*/
        /*queenBeeHandler = new QueenBeeHandler(this);*/
        ultraWitherBossHandler = new UltraWitherBossHandler(this);

        Objects.requireNonNull(getCommand("debugarena")).setExecutor(new DebugArenaCommand());
    }

    private void initMobCapSystem() {
        config = new MobCapConfig(this);
        mobCapManager = MobCapManager.getInstance(this, config);
        spawnManager = new CustomSpawnManager(this, config);

        MobCapCommand commandExecutor = new MobCapCommand(mobCapManager, config);
        MobCapTabCompleter tabCompleter = new MobCapTabCompleter();

        Objects.requireNonNull(getCommand("mobcap")).setExecutor(commandExecutor);
        Objects.requireNonNull(getCommand("mobcap")).setTabCompleter(tabCompleter);
        Objects.requireNonNull(getCommand("mobcapinfo")).setExecutor(commandExecutor);

        Bukkit.getPluginManager().registerEvents(spawnManager, this);
        getLogger().info("Lógica de MobCap habilitada correctamente!");
    }

    private void initHabilidadesSystem() {
        habilidadesManager = new HabilidadesManager(this);
        habilidadesEffects = new HabilidadesEffects(this);
        habilidadesGUI = new HabilidadesGUI(this, habilidadesManager, dayHandler);
        habilidadesListener = new HabilidadesListener(this, habilidadesManager, habilidadesEffects);

        Bukkit.getPluginManager().registerEvents(habilidadesGUI, this);
        Bukkit.getPluginManager().registerEvents(habilidadesListener, this);

        HabilidadesCommand habilidadesCommand = new HabilidadesCommand(habilidadesManager, habilidadesEffects);
        Objects.requireNonNull(getCommand("habilidades")).setExecutor(habilidadesCommand);
        Objects.requireNonNull(getCommand("habilidades")).setTabCompleter(habilidadesCommand);

        getLogger().info("Sistema de Habilidades habilitado correctamente!");
    }

    private void initPublicCommandSystem() {
        viciontCommands = new ViciontCommands(this, deathStormHandler, dayHandler);
        Objects.requireNonNull(getCommand("viciont")).setExecutor(viciontCommands);
        Objects.requireNonNull(getCommand("viciont")).setTabCompleter(viciontCommands);
    }

    private void initCorruptedEndSystem() {
        corruptedEnd = new CorruptedEnd(this);
        Bukkit.getPluginManager().registerEvents(corruptedEnd, this);

        Bukkit.getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onServerLoad(ServerLoadEvent event) {
                corruptedEnd.initialize();
            }
        }, ViciontHardcore3.this);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (corruptedEnd != null && corruptedEnd.corruptedWorld != null) {
                    corruptedEnd.spawnParticles();
                }
            }
        }.runTaskTimer(this, 80L, 20L);
    }

    private void initInfestedCavesDimension() {
        this.generator = new InfestedGenerator(this);
        this.structureManager = new StructureManager(this);
        this.portalManager = new PortalManager(this);

        this.listeners = new InfestedListeners(this, portalManager, structureManager);

        getCommand("infestedPortal").setExecutor(new InfestedCommands(this, portalManager));
        getCommand("infestedPortalOverworld").setExecutor(new InfestedCommands(this, portalManager));
        getCommand("InfestedCave").setExecutor(new InfestedCommands(this, portalManager));

        getServer().getPluginManager().registerEvents(listeners, this);
        getServer().getPluginManager().registerEvents(portalManager, this);

        // --- NUEVO: Inicializar Ambiente ---
        this.infestedAmbient = new InfestedCaveAmbient(this);
        getServer().getPluginManager().registerEvents(this.infestedAmbient, this);

        //Runas
        this.nullRunes = new Null_Runes(this);
        getServer().getPluginManager().registerEvents(nullRunes, this);

        // Registrar comando de test
        AmbientCommand ambientCmd = new AmbientCommand(this.infestedAmbient);
        getCommand("icambient").setExecutor(ambientCmd);
        getCommand("icambient").setTabCompleter(ambientCmd);
        // -----------------------------------

        // 5. Crear la dimensión
        createInfestedWorld();

        Bukkit.getScheduler().runTaskLater(this, () -> {
            structureManager.loadSchematics();
        }, 20L);

        getLogger().info("Infested Caves ha sido habilitado correctamente.");
    }

    @Override
    public void onLoad() {
        // ANTES de que cualquier mundo se cargue
        CorruptedEndBiomeRegistry.registerAll(this);
    }

    // ------------------------------------------------------------------------
    //  Metodos
    // ------------------------------------------------------------------------

    private void cleanupBossHandlers() {
        if (infestedBeeHandler != null) {
            try {
                infestedBeeHandler.shutdown();
                getLogger().info("InfestedBeeHandler limpiado correctamente");
            } catch (Exception e) {
                getLogger().warning("Error al limpiar InfestedBeeHandler: " + e.getMessage());
            }
            infestedBeeHandler = null;
        }

/*        if (hellishBeeHandler != null) {
            try {
                hellishBeeHandler.shutdown();
                getLogger().info("HellishBeeHandler limpiado correctamente");
            } catch (Exception e) {
                getLogger().warning("Error al limpiar HellishBeeHandler: " + e.getMessage());
            }
            hellishBeeHandler = null;
        }*/

/*        if (queenBeeHandler != null) {
            try {
                queenBeeHandler.shutdown();
                getLogger().info("QueenBeeHandler limpiado correctamente");
            } catch (Exception e) {
                getLogger().warning("Error al limpiar QueenBeeHandler: " + e.getMessage());
            }
            queenBeeHandler = null;
        }*/

        if (ultraWitherBossHandler != null) {
            try {
                ultraWitherBossHandler.shutdown();
                getLogger().info("UltraWitherBossHandler limpiado correctamente");
            } catch (Exception e) {
                getLogger().warning("Error al limpiar UltraWitherBossHandler: " + e.getMessage());
            }
            ultraWitherBossHandler = null;
        }
    }
    // ------------------------------------------------------------------------
    //  Eventos básicos (join / quit / world load)
    // ------------------------------------------------------------------------

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        String message = ChatColor.WHITE + "\uDB80\uDC65 " +
                ChatColor.RESET + ChatColor.of("#B83EFF") + ChatColor.BOLD + event.getPlayer().getName() +
                ChatColor.RESET + ChatColor.of("#FF009F") + " se ha conectado.";
        event.setJoinMessage(message);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        String message = ChatColor.WHITE + "\uDB80\uDC63 " +
                ChatColor.RESET + ChatColor.of("#B8B8B8") + ChatColor.BOLD + event.getPlayer().getName() +
                ChatColor.RESET + ChatColor.of("#7C7981") + " se ha desconectado.";
        event.setQuitMessage(message);
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        if (mobCapManager != null && mobCapManager.isInitialized()) {
            Bukkit.getScheduler().runTaskLater(this, () -> {
                mobCapManager.handleNewWorld(event.getWorld());
            }, 20L);
        }
    }

    public void createInfestedWorld() {
        if (Bukkit.getWorld(WORLD_NAME) == null) {
            WorldCreator creator = new WorldCreator(WORLD_NAME);
            creator.generator(generator);
            World world = creator.createWorld();

            if (world != null) {
                // Evitar ciclo día/noche si quieres que sea oscuro siempre
                world.setGameRule(org.bukkit.GameRule.DO_DAYLIGHT_CYCLE, false);
                world.setTime(18000); // Medianoche
                getLogger().info("Dimensión " + WORLD_NAME + " cargada/creada.");
            }
        }
    }

    // ------------------------------------------------------------------------
    //  Getters útiles
    // ------------------------------------------------------------------------

    public static ViciontHardcore3 getInstance() {
        return instance;
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

    public MobCapManager getMobCapManager() {
        return mobCapManager;
    }

    public MobCapConfig getMobCapConfig() {
        return config;
    }

    public CustomSpawnManager getSpawnManager() {
        return spawnManager;
    }

    public CustomEffectManager getEffectManager() {
        return effectManager;
    }

    public PortalManager getPortalManager() { return portalManager; }

    public StructureManager getStructureManager() { return structureManager; }

    public EntidadesGuiManager getEntidadesGuiManager() {return entidadesGuiManager;}
}
