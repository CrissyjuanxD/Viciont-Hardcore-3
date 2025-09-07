package CorruptedEnd;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public class CorruptedEnd implements Listener {
    private final JavaPlugin plugin;
    public static final String WORLD_NAME = "corrupted_end";
    public World corruptedWorld;

    private CorruptedEndGenerator generator;
    private PortalManager portalManager;
    private BiomeEffectManager biomeEffectManager;
    private LootManager lootManager;
    private StructureManager structureManager;
    private MobSpawnManager mobSpawnManager;
    private CorruptedEndCommands commands;

    public CorruptedEnd(JavaPlugin plugin) {
        this.plugin = plugin;
        initializeComponents();
    }

    private void initializeComponents() {
        this.generator = new CorruptedEndGenerator(plugin);
        this.portalManager = new PortalManager(plugin, this);
        this.biomeEffectManager = new BiomeEffectManager(plugin);
        this.lootManager = new LootManager(plugin);
        this.structureManager = new StructureManager(plugin);
        this.mobSpawnManager = new MobSpawnManager(plugin);
        this.commands = new CorruptedEndCommands(plugin, portalManager, lootManager);
    }

    public void initialize() {
        createCorruptedWorld();
        registerEvents();
        registerCommands();
        startTasks();

        // Cargar schematics después de crear el mundo
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (corruptedWorld != null) {
                structureManager.loadSchematics();
            }
        }, 20L);
    }

    public void createCorruptedWorld() {
        corruptedWorld = Bukkit.getWorld(WORLD_NAME);
        if (corruptedWorld != null) return;

        WorldCreator creator = new WorldCreator(WORLD_NAME);
        creator.environment(World.Environment.THE_END);
        creator.generator(generator);

        try {
            corruptedWorld = creator.createWorld();
            if (corruptedWorld != null) {
                corruptedWorld.setSpawnLocation(0, 120, 0);

                // Crear portal de retorno solo la primera vez
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    portalManager.createReturnPortal();
                }, 10L);

                plugin.getLogger().info("Mundo Corrupted End creado exitosamente!");
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error al crear el mundo Corrupted End: " + e.getMessage());
        }
    }

    private void registerEvents() {
        plugin.getServer().getPluginManager().registerEvents(portalManager, plugin);
        plugin.getServer().getPluginManager().registerEvents(biomeEffectManager, plugin);
        plugin.getServer().getPluginManager().registerEvents(structureManager, plugin);
        plugin.getServer().getPluginManager().registerEvents(mobSpawnManager, plugin);
    }

    private void registerCommands() {
        commands.registerCommands();
    }

    private void startTasks() {
        // Task para partículas
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (corruptedWorld != null && !corruptedWorld.getPlayers().isEmpty()) {
                spawnParticles();
            }
        }, 0L, 20L);

        // Task para spawneo de mobs
        mobSpawnManager.startSpawning();
    }

    public void spawnParticles() {
        structureManager.spawnParticles();
    }

    // Getters
    public JavaPlugin getPlugin() { return plugin; }
    public World getCorruptedWorld() { return corruptedWorld; }
    public CorruptedEndGenerator getGenerator() { return generator; }
    public PortalManager getPortalManager() { return portalManager; }
    public BiomeEffectManager getBiomeEffectManager() { return biomeEffectManager; }
    public LootManager getLootManager() { return lootManager; }
    public StructureManager getStructureManager() { return structureManager; }
    public MobSpawnManager getMobSpawnManager() { return mobSpawnManager; }
}