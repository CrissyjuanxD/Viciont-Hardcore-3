package Dificultades.Features;

import Dificultades.CustomMobs.InfestedGolems;
import org.bukkit.*;
import org.bukkit.block.BlockState;
import org.bukkit.block.TileState;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class SpawnerInfestedGolems implements Listener, CommandExecutor {

    private final JavaPlugin plugin;
    private final InfestedGolems infestedGolemsLogic;
    private final NamespacedKey spawnerItemKey;

    private static SpawnerInfestedGolems instance;

    // Ubicaciones de los spawners activos
    private static final Set<Location> registeredSpawners = new HashSet<>();

    // Mapa de Cooldowns INDIVIDUALES
    private static final Map<Location, Long> cooldowns = new HashMap<>();

    // Mapa de Cooldown de GRUPO
    private static final Map<Location, Long> groupReliefTimers = new HashMap<>();

    // Mapa para recordar el objetivo de spawn de cada grupo
    private static final Map<Location, Integer> groupSpawnTargets = new HashMap<>();

    private File spawnersFile;
    private FileConfiguration spawnersConfig;

    private static final int GROUP_RADIUS = 80;
    private static final int MIN_COOLDOWN_SEC = 60;
    private static final int MAX_COOLDOWN_SEC = 120;
    private static final int RELIEF_COOLDOWN_SEC = 30;

    public SpawnerInfestedGolems(JavaPlugin plugin, InfestedGolems infestedGolemsLogic) {
        this.plugin = plugin;
        instance = this;
        this.infestedGolemsLogic = infestedGolemsLogic;
        this.spawnerItemKey = new NamespacedKey(plugin, "infested_spawner_item");

        loadSpawners();
        startSpawnerTask();

        plugin.getCommand("giveInfestedSpawner").setExecutor(this);
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    private void startSpawnerTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (registeredSpawners.isEmpty()) return;

                long currentTime = System.currentTimeMillis();
                Set<Location> processed = new HashSet<>();

                // Usamos una copia para iterar seguramente
                for (Location loc : new HashSet<>(registeredSpawners)) {
                    if (processed.contains(loc)) continue;

                    if (!loc.getWorld().isChunkLoaded(loc.getBlockX() >> 4, loc.getBlockZ() >> 4)) continue;

                    // CAMBIO: Verificamos que sea SPAWNER
                    if (loc.getBlock().getType() != Material.SPAWNER) continue;

                    // 1. Formar Grupo
                    List<Location> group = getSpawnerGroup(loc);
                    processed.addAll(group);

                    // 2. Determinar el "Líder" del grupo
                    group.sort(Comparator.comparingDouble(Location::getX)
                            .thenComparingDouble(Location::getY)
                            .thenComparingDouble(Location::getZ));
                    Location groupLeader = group.get(0);

                    // 3. Análisis del Grupo
                    int currentActive = 0;
                    List<Location> readyToSpawn = new ArrayList<>();
                    boolean isGroupInReliefCooldown = false;

                    for (Location spawner : group) {
                        if (InfestedGolems.isSpawnerOccupied(spawner)) {
                            currentActive++;
                        } else {
                            if (groupReliefTimers.containsKey(spawner)) {
                                if (currentTime < groupReliefTimers.get(spawner)) {
                                    isGroupInReliefCooldown = true;
                                } else {
                                    groupReliefTimers.remove(spawner);
                                }
                            }

                            if (!cooldowns.containsKey(spawner) || currentTime >= cooldowns.get(spawner)) {
                                readyToSpawn.add(spawner);
                            } else {
                                if (Math.random() < 0.1) {
                                    spawner.getWorld().spawnParticle(Particle.WAX_OFF, spawner.clone().add(0.5, 1, 0.5), 3, 0.2, 0.2, 0.2, 0.05);
                                }
                            }
                        }
                    }

                    // 4. LÓGICA DE CANTIDAD ALEATORIA
                    int absoluteMax = (int) Math.ceil(group.size() / 2.0);
                    if (absoluteMax < 1) absoluteMax = 1;

                    int targetForThisWave;

                    if (currentActive == 0) {
                        targetForThisWave = 1 + new Random().nextInt(absoluteMax);
                        groupSpawnTargets.put(groupLeader, targetForThisWave);
                    } else {
                        targetForThisWave = groupSpawnTargets.getOrDefault(groupLeader, absoluteMax);
                    }

                    if (targetForThisWave > absoluteMax) targetForThisWave = absoluteMax;

                    // 5. Decidir si spawnear
                    if (currentActive < targetForThisWave && !readyToSpawn.isEmpty() && !isGroupInReliefCooldown) {

                        Location target = readyToSpawn.get(new Random().nextInt(readyToSpawn.size()));
                        if (isPlayerNearby(target)) {
                            spawnGolemAt(target);
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    private List<Location> getSpawnerGroup(Location center) {
        List<Location> group = new ArrayList<>();
        for (Location loc : registeredSpawners) {
            if (loc.getWorld().equals(center.getWorld())) {
                double distance = Math.sqrt(Math.pow(loc.getX() - center.getX(), 2) + Math.pow(loc.getZ() - center.getZ(), 2));
                if (distance <= GROUP_RADIUS) {
                    group.add(loc);
                }
            }
        }
        return group;
    }

    private boolean isPlayerNearby(Location loc) {
        for (Player p : loc.getWorld().getPlayers()) {
            if (p.getLocation().distanceSquared(loc) < 60 * 60) {
                return true;
            }
        }
        return false;
    }

    private void spawnGolemAt(Location spawnerLoc) {
        Location spawnLoc = spawnerLoc.clone().add(0.5, 1.0, 0.5);
        infestedGolemsLogic.spawnInfestedGolem(spawnLoc, spawnerLoc);

        spawnLoc.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, spawnLoc, 1);
        spawnLoc.getWorld().playSound(spawnLoc, Sound.BLOCK_BEACON_ACTIVATE, 2.0f, 0.5f);
    }

    // --- COOLDOWN MANAGEMENT ---

    public static void notifyGolemDeath(Location spawnerLoc) {
        long currentTime = System.currentTimeMillis();

        int delaySeconds = MIN_COOLDOWN_SEC + new Random().nextInt(MAX_COOLDOWN_SEC - MIN_COOLDOWN_SEC + 1);
        long cooldownEnd = currentTime + (delaySeconds * 1000L);
        cooldowns.put(spawnerLoc, cooldownEnd);

        long reliefEnd = currentTime + (RELIEF_COOLDOWN_SEC * 1000L);
        groupReliefTimers.put(spawnerLoc, reliefEnd);

        if (spawnerLoc.getWorld() != null) {
            spawnerLoc.getWorld().playSound(spawnerLoc, Sound.BLOCK_BEACON_DEACTIVATE, 1.0f, 0.5f);
            spawnerLoc.getWorld().spawnParticle(Particle.SOUL, spawnerLoc.clone().add(0.5, 1, 0.5), 15, 0.3, 0.3, 0.3, 0.05);

            new BukkitRunnable() {
                double radius = 0.5;
                final double maxRadius = 3.5;
                final Location center = spawnerLoc.clone().add(0.5, 1.2, 0.5);

                @Override
                public void run() {
                    if (radius > maxRadius) {
                        this.cancel();
                        return;
                    }
                    for (int degree = 0; degree < 360; degree += 20) {
                        double radians = Math.toRadians(degree);
                        double x = Math.cos(radians) * radius;
                        double z = Math.sin(radians) * radius;
                        center.getWorld().spawnParticle(Particle.END_ROD,
                                center.clone().add(x, 0, z),
                                1, 0, 0, 0, 0);
                    }
                    radius += 0.3;
                }
            }.runTaskTimer(instance.plugin, 0L, 2L);
        }
    }

    // --- COMANDOS Y EVENTOS ---

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player player) {
            // CAMBIO: Material.SPAWNER
            ItemStack spawner = new ItemStack(Material.SPAWNER);
            ItemMeta meta = spawner.getItemMeta();
            meta.setDisplayName(ChatColor.DARK_RED + "" + ChatColor.BOLD + "Infested Spawner");
            meta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Colócalo para crear un spawner",
                    ChatColor.GRAY + "de Infested Golems."
            ));
            meta.getPersistentDataContainer().set(spawnerItemKey, PersistentDataType.BYTE, (byte) 1);
            spawner.setItemMeta(meta);
            player.getInventory().addItem(spawner);
            player.sendMessage(ChatColor.GREEN + "Has recibido un Infested Spawner.");
            return true;
        }
        return false;
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        // CAMBIO: Verificar Material.SPAWNER
        if (item.getType() == Material.SPAWNER && item.hasItemMeta()) {
            PersistentDataContainer container = item.getItemMeta().getPersistentDataContainer();
            if (container.has(spawnerItemKey, PersistentDataType.BYTE)) {

                // CRUCIAL: Inyectar la data en el bloque real (TileEntity)
                // Esto hace que WorldEdit pueda copiar el spawner y siga funcionando
                BlockState state = event.getBlockPlaced().getState();
                if (state instanceof TileState) {
                    TileState tileState = (TileState) state;
                    tileState.getPersistentDataContainer().set(spawnerItemKey, PersistentDataType.BYTE, (byte) 1);
                    tileState.update();
                }

                Location loc = event.getBlock().getLocation();
                registeredSpawners.add(loc);
                saveSpawners();

                event.getPlayer().sendMessage(ChatColor.GREEN + "Spawner de Infested Golem colocado y registrado.");
                loc.getWorld().playSound(loc, Sound.BLOCK_METAL_PLACE, 1f, 1f);
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        // CAMBIO: Verificar Material.SPAWNER
        if (event.getBlock().getType() == Material.SPAWNER) {
            Location loc = event.getBlock().getLocation();
            if (registeredSpawners.contains(loc)) {
                registeredSpawners.remove(loc);
                cooldowns.remove(loc);
                groupReliefTimers.remove(loc);
                groupSpawnTargets.remove(loc);
                saveSpawners();
                event.getPlayer().sendMessage(ChatColor.RED + "Spawner de Infested Golem eliminado.");
                event.setDropItems(false);

                // Dropear el item customizado
                ItemStack customDrop = new ItemStack(Material.SPAWNER);
                ItemMeta meta = customDrop.getItemMeta();
                meta.setDisplayName(ChatColor.DARK_RED + "" + ChatColor.BOLD + "Infested Spawner");
                meta.getPersistentDataContainer().set(spawnerItemKey, PersistentDataType.BYTE, (byte) 1);
                customDrop.setItemMeta(meta);

                loc.getWorld().dropItemNaturally(loc, customDrop);
            }
        }
    }

    // --- PERSISTENCIA ---

    private void loadSpawners() {
        spawnersFile = new File(plugin.getDataFolder(), "spawners_infested.yml");
        if (!spawnersFile.exists()) {
            try {
                spawnersFile.createNewFile();
            } catch (IOException e) { e.printStackTrace(); }
        }
        spawnersConfig = YamlConfiguration.loadConfiguration(spawnersFile);
        registeredSpawners.clear();
        List<String> locs = spawnersConfig.getStringList("locations");
        for (String s : locs) {
            try { registeredSpawners.add(stringToLoc(s)); } catch (Exception e) { }
        }
    }

    private void saveSpawners() {
        List<String> locs = new ArrayList<>();
        for (Location loc : registeredSpawners) { locs.add(locToString(loc)); }
        spawnersConfig.set("locations", locs);
        try { spawnersConfig.save(spawnersFile); } catch (IOException e) { e.printStackTrace(); }
    }

    private String locToString(Location loc) {
        return loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
    }

    private Location stringToLoc(String s) {
        String[] parts = s.split(",");
        return new Location(Bukkit.getWorld(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3]));
    }

    public void shutdown() {
        saveSpawners();
        registeredSpawners.clear();
        cooldowns.clear();
        groupReliefTimers.clear();
        groupSpawnTargets.clear();
    }
}