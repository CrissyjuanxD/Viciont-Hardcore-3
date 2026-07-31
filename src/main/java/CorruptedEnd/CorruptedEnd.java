package CorruptedEnd;

import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public class CorruptedEnd implements Listener {
    private final JavaPlugin plugin;
    // Nombre normal del mundo para que Bukkit lo maneje sin errores
    public static final String WORLD_NAME = "corrupted_end";
    public World corruptedWorld;

    private CorruptedEndGenerator generator;
    private PortalManager portalManager;
    private BiomeEffectManager biomeEffectManager;
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
        this.structureManager = new StructureManager(plugin);
        this.mobSpawnManager = new MobSpawnManager(plugin);
        this.commands = new CorruptedEndCommands(plugin, portalManager);
    }

    public void initialize() {
        createCorruptedWorld();
        registerEvents();
        registerCommands();
        startTasks();

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (corruptedWorld != null) {
                structureManager.loadSchematics();
            }
        }, 40L);
    }

    public void createCorruptedWorld() {
        corruptedWorld = Bukkit.getWorld(WORLD_NAME);
        if (corruptedWorld != null) return;

        // Creamos el mundo 100% desde el plugin usando nuestro generador
        WorldCreator creator = new WorldCreator(WORLD_NAME);
        creator.environment(World.Environment.NORMAL); // Necesario para que funcionen los colores del cielo
        creator.generator(generator); // Nuestro generador de islas matemáticas

        try {
            corruptedWorld = creator.createWorld();
            if (corruptedWorld != null) {
                corruptedWorld.setSpawnLocation(0, 120, 0);

                // Forzamos el atardecer permanente y sin clima desde el Plugin
                corruptedWorld.setTime(7000);
                corruptedWorld.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
                corruptedWorld.setGameRule(GameRule.DO_WEATHER_CYCLE, false);

                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    portalManager.createReturnPortal();
                }, 10L);

                plugin.getLogger().info("Mundo Corrupted End generado exitosamente por el Plugin!");
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
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (corruptedWorld != null && !corruptedWorld.getPlayers().isEmpty()) {
                spawnParticles();
            }
        }, 0L, 20L);

        mobSpawnManager.startSpawning();
    }

    public void spawnParticles() {
        structureManager.spawnParticles();
    }

    public JavaPlugin getPlugin() { return plugin; }
    public World getCorruptedWorld() { return corruptedWorld; }
    public CorruptedEndGenerator getGenerator() { return generator; }
    public PortalManager getPortalManager() { return portalManager; }
    public BiomeEffectManager getBiomeEffectManager() { return biomeEffectManager; }
    public StructureManager getStructureManager() { return structureManager; }
    public MobSpawnManager getMobSpawnManager() { return mobSpawnManager; }
}