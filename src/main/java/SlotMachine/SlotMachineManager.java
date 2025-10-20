package SlotMachine;

import SlotMachine.api.SlotMachineModel;
import SlotMachine.cache.smachine.SlotM;
import SlotMachine.cache.smachine.SlotMachine;
import SlotMachine.cache.tools.smachine.SlotMachineTool;
import SlotMachine.listeners.SlotMachineListener;
import SlotMachine.providers.modelengine.ModelEngine3;
import SlotMachine.providers.modelengine.ModelEngine4;
import SlotMachine.commands.SlotMachineCommand;
import SlotMachine.utils.ItemCreator;
import org.bukkit.command.PluginCommand;
import org.bukkit.inventory.ItemStack;
import vct.hardcore3.ViciontHardcore3;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.Sound;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gestor principal del sistema de Slot Machine - Basado en DTools3
 */
public class SlotMachineManager {

    private final ViciontHardcore3 plugin;
    private SlotMachineTool slotMachineTool;
    private SlotMachineListener listener;
    private ItemCreator itemCreator;
    private ModelEngine4 modelEngine4;
    private ModelEngine3 modelEngine3;
    private boolean useModelEngine4;
    private SlotMachineCommand slotMachineCommand;
    private final Map<Location, SlotMachineModel> activeMachines = new ConcurrentHashMap<>();
    private final Random random = new Random();

    public SlotMachineManager(ViciontHardcore3 plugin) {
        this.plugin = plugin;
        initialize();
    }

    private void initialize() {
        // Cargar configuración
        loadConfiguration();

        // Inicializar utilidades
        this.itemCreator = new ItemCreator(plugin);

        // Detectar ModelEngine
        detectModelEngine();

        // Inicializar listener
        this.listener = new SlotMachineListener(plugin, this);

        // Registrar eventos
        plugin.getServer().getPluginManager().registerEvents(listener, plugin);

        // Inicializar y registrar comandos
        initializeCommands();

        plugin.getLogger().info("SlotMachine system initialized successfully!");
    }

    private void loadConfiguration() {
        File configFile = new File(plugin.getDataFolder(), "tools/slot_machines.yml");
        if (!configFile.exists()) {
            plugin.saveResource("tools/slot_machines.yml", false);
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        this.slotMachineTool = new SlotMachineTool(config);
    }

    private void detectModelEngine() {
        try {
            // Intentar ModelEngine 4 primero
            Class.forName("com.ticxo.modelengine.api.ModelEngineAPI");
            Class.forName("com.ticxo.modelengine.api.model.ModeledEntity");
            this.modelEngine4 = new ModelEngine4(plugin);
            this.useModelEngine4 = true;
            plugin.getLogger().info("Detected ModelEngine 4");
        } catch (ClassNotFoundException e) {
            try {
                // Intentar ModelEngine 3
                Class.forName("com.ticxo.modelengine.ModelEngine");
                this.modelEngine3 = new ModelEngine3(plugin);
                this.useModelEngine4 = false;
                plugin.getLogger().info("Detected ModelEngine 3");
            } catch (ClassNotFoundException e2) {
                plugin.getLogger().warning("ModelEngine not found, using fallback");
                this.useModelEngine4 = false;
            }
        }
    }

    private void initializeCommands() {
        this.slotMachineCommand = new SlotMachineCommand(plugin, this);

        PluginCommand command = plugin.getCommand("slotmachine");
        if (command != null) {
            command.setExecutor(slotMachineCommand);
            command.setTabCompleter(slotMachineCommand);
        }
    }

    public boolean createSlotMachine(Location location, Player creator, String machineId) {
        SlotMachine slotMachine = slotMachineTool.getSlotMachine(machineId);
        if (slotMachine == null) {
            slotMachine = slotMachineTool.getDefaultSlotMachine();
            if (slotMachine == null) {
                creator.sendMessage(ChatColor.of("#FF6B6B") + "۞ Error: SlotMachine no configurada.");
                return false;
            }
        }

        // Verificar si ya existe una máquina en esa ubicación
        if (activeMachines.containsKey(location)) {
            creator.sendMessage(ChatColor.of("#FF6B6B") + "۞ Ya existe una Slot Machine en esa ubicación.");
            return false;
        }

        // Crear entidad ArmorStand
        ArmorStand entity = (ArmorStand) location.getWorld().spawnEntity(location, EntityType.ARMOR_STAND);
        entity.setInvulnerable(true);
        entity.setInvisible(true);
        entity.setGravity(false);
        entity.setAI(false);
        entity.setCollidable(true);
        entity.setSilent(true);
        entity.setCustomName("SlotMachine");
        entity.setCustomNameVisible(false);

        // Metadata para identificar como slot machine
        entity.setMetadata("slot_machine", new FixedMetadataValue(plugin, slotMachine.getModelId()));

        // Crear modelo 3D
        UUID modelUUID = null;
        if (useModelEngine4 && modelEngine4 != null) {
            modelUUID = modelEngine4.spawnModel(entity, slotMachine.getModelId());
        } else if (modelEngine3 != null) {
            // Para ModelEngine3 mantener compatibilidad
            boolean created = modelEngine3.createModel(entity, slotMachine.getModelId());
            if (created) {
                modelUUID = entity.getUniqueId();
            }
        }

        if (modelUUID == null) {
            plugin.getLogger().warning("Failed to create 3D model for slot machine");
        }

        // Crear modelo de datos
        SlotMachineModel model = new SlotMachineModel(slotMachine.getId(), location);
        model.setEntity(entity);
        model.setModelUUID(modelUUID);
        activeMachines.put(location, model);

        creator.sendMessage(ChatColor.of("#B5EAD7") + "۞ Slot Machine creada exitosamente!");

        // Guardar en archivo para persistencia
        saveSlotMachineToFile(model);
        return true;
    }

    public boolean removeSlotMachine(Location location) {
        // Buscar la máquina más cercana en un radio de 2 bloques
        SlotMachineModel model = null;
        Location foundLocation = null;

        for (Map.Entry<Location, SlotMachineModel> entry : activeMachines.entrySet()) {
            if (entry.getKey().getWorld().equals(location.getWorld()) &&
                    entry.getKey().distance(location) <= 2.0) {
                model = entry.getValue();
                foundLocation = entry.getKey();
                break;
            }
        }

        if (model == null) {
            return false;
        }

        // Remover del mapa
        activeMachines.remove(foundLocation);

        if (model.getEntity() != null) {
            if (useModelEngine4 && modelEngine4 != null) {
                modelEngine4.removeModel(model.getEntity());
            } else if (modelEngine3 != null) {
                modelEngine3.removeModel(model.getEntity());
            }
            model.getEntity().remove();
        }

        removeSlotMachineFromFile(foundLocation);
        return true;
    }

    private void saveSlotMachineToFile(SlotMachineModel model) {
        try {
            File dataFile = new File(plugin.getDataFolder(), "slotmachines.yml");
            FileConfiguration config = YamlConfiguration.loadConfiguration(dataFile);

            String key = locationToString(model.getLocation());
            config.set("machines." + key + ".id", model.getId());
            config.set("machines." + key + ".location.world", model.getLocation().getWorld().getName());
            config.set("machines." + key + ".location.x", model.getLocation().getX());
            config.set("machines." + key + ".location.y", model.getLocation().getY());
            config.set("machines." + key + ".location.z", model.getLocation().getZ());
            config.set("machines." + key + ".location.yaw", model.getLocation().getYaw());
            config.set("machines." + key + ".location.pitch", model.getLocation().getPitch());

            config.save(dataFile);
        } catch (Exception e) {
            plugin.getLogger().severe("Error saving slot machine: " + e.getMessage());
        }
    }

    private void removeSlotMachineFromFile(Location location) {
        try {
            File dataFile = new File(plugin.getDataFolder(), "slotmachines.yml");
            if (!dataFile.exists()) return;

            FileConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
            String key = locationToString(location);
            config.set("machines." + key, null);
            config.save(dataFile);
        } catch (Exception e) {
            plugin.getLogger().severe("Error removing slot machine from file: " + e.getMessage());
        }
    }

    private String locationToString(Location loc) {
        return loc.getWorld().getName() + "_" +
                (int)loc.getX() + "_" +
                (int)loc.getY() + "_" +
                (int)loc.getZ();
    }

    public void loadSlotMachinesFromFile() {
        try {
            File dataFile = new File(plugin.getDataFolder(), "slotmachines.yml");
            if (!dataFile.exists()) return;

            FileConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
            if (!config.contains("machines")) return;

            for (String key : config.getConfigurationSection("machines").getKeys(false)) {
                String worldName = config.getString("machines." + key + ".location.world");
                double x = config.getDouble("machines." + key + ".location.x");
                double y = config.getDouble("machines." + key + ".location.y");
                double z = config.getDouble("machines." + key + ".location.z");
                float yaw = (float) config.getDouble("machines." + key + ".location.yaw");
                float pitch = (float) config.getDouble("machines." + key + ".location.pitch");
                String machineId = config.getString("machines." + key + ".id", "default");

                org.bukkit.World world = plugin.getServer().getWorld(worldName);
                if (world == null) continue;

                Location location = new Location(world, x, y, z, yaw, pitch);

                // Recrear la máquina
                recreateSlotMachine(location, machineId);
            }

            plugin.getLogger().info("Loaded " + activeMachines.size() + " slot machines from file");
        } catch (Exception e) {
            plugin.getLogger().severe("Error loading slot machines: " + e.getMessage());
        }
    }

    private void recreateSlotMachine(Location location, String machineId) {
        SlotMachine slotMachine = slotMachineTool.getSlotMachine(machineId);
        if (slotMachine == null) {
            slotMachine = slotMachineTool.getDefaultSlotMachine();
        }

        // Crear entidad ArmorStand
        ArmorStand entity = (ArmorStand) location.getWorld().spawnEntity(location, EntityType.ARMOR_STAND);
        entity.setInvulnerable(true);
        entity.setInvisible(true);
        entity.setGravity(false);
        entity.setAI(false);
        entity.setCollidable(true);
        entity.setSilent(true);
        entity.setCustomName("SlotMachine");
        entity.setCustomNameVisible(false);

        // Metadata
        entity.setMetadata("slot_machine", new FixedMetadataValue(plugin, slotMachine.getModelId()));

        // Crear modelo 3D
        UUID modelUUID = null;
        if (useModelEngine4 && modelEngine4 != null) {
            modelUUID = modelEngine4.spawnModel(entity, slotMachine.getModelId());
        } else if (modelEngine3 != null) {
            boolean created = modelEngine3.createModel(entity, slotMachine.getModelId());
            if (created) {
                modelUUID = entity.getUniqueId();
            }
        }

        // Crear modelo de datos
        SlotMachineModel model = new SlotMachineModel(slotMachine.getId(), location);
        model.setEntity(entity);
        model.setModelUUID(modelUUID);
        activeMachines.put(location, model);
    }

    public void reloadConfiguration() {
        // Limpiar máquinas existentes antes de recargar
        for (SlotMachineModel model : activeMachines.values()) {
            if (model.getEntity() != null) {
                if (useModelEngine4 && modelEngine4 != null) {
                    modelEngine4.removeModel(model.getEntity());
                } else if (modelEngine3 != null) {
                    modelEngine3.removeModel(model.getEntity());
                }
                model.getEntity().remove();
            }
        }
        activeMachines.clear();

        loadConfiguration();
        // Recargar máquinas desde archivo
        loadSlotMachinesFromFile();
    }

    public int getActiveMachinesCount() {
        return activeMachines.size();
    }

    public int getLoadedConfigsCount() {
        return slotMachineTool.getSlotMachines().size();
    }

    public boolean isModelEngineAvailable() {
        return modelEngine4 != null || modelEngine3 != null;
    }

    public List<String> getAvailableMachineIds() {
        return new ArrayList<>(slotMachineTool.getSlotMachines().keySet());
    }

    public SlotMachineModel getSlotMachine(Location location) {
        return activeMachines.get(location);
    }

    public void startUsing(SlotMachine slotMachine, SlotMachineModel model, Player player) {
        // Implementar lógica de uso
        model.setActive(true);
        model.setLastUsed(System.currentTimeMillis());

        // Ejecutar spin
        executeSpin(slotMachine, model, player);
    }

    private void executeSpin(SlotMachine slotMachine, SlotMachineModel model, Player player) {
        // Lógica de spin basada en DTools3
        Map<String, SlotM> slots = slotMachine.getSlots();
        if (slots.isEmpty()) {
            player.sendMessage(ChatColor.of("#FF6B6B") + "۞ Error: No hay slots configurados.");
            model.setActive(false);
            return;
        }

        // Calcular resultado basado en probabilidades
        SlotM selectedSlot = calculateSlotResult(slots);

        if (selectedSlot == null) {
            player.sendMessage(ChatColor.of("#FF6B6B") + "۞ Error en el cálculo de probabilidades.");
            model.setActive(false);
            return;
        }

        player.sendMessage(ChatColor.of("#B5EAD7") + "۞ ¡Girando la máquina!");

        // Ejecutar resultado después del tiempo de espera
        long waitTicks = (long) (selectedSlot.getWaitTime() * 20); // Convertir segundos a ticks

        // Reproducir sonido de apuesta
        playBetSound(player.getLocation());

        // Reproducir animación si está disponible
        if (model.getModelUUID() != null && useModelEngine4 && modelEngine4 != null) {
            modelEngine4.playAnimation(model.getModelUUID(), slotMachine.getModelId(), selectedSlot.getAnimation());
        }

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            executeSlotResult(slotMachine, selectedSlot, player);
            model.setActive(false);
        }, waitTicks);
    }

    private void playBetSound(Location location) {
        // Reproducir sonido a todos los jugadores cercanos (30 bloques)
        for (Player nearbyPlayer : location.getWorld().getPlayers()) {
            if (nearbyPlayer.getLocation().distance(location) <= 30) {
                try {
                    // Intentar reproducir el sonido personalizado
                    nearbyPlayer.playSound(location, "dtools3:tools.casino.bet", 1.0f, 0.8f);
                } catch (Exception e) {
                    // Si falla, intentar con formato alternativo
                    try {
                        nearbyPlayer.playSound(location, Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
                    } catch (Exception ex) {
                        plugin.getLogger().warning("No se pudo reproducir sonido de apuesta");
                    }
                }
            }
        }
    }
    private SlotM calculateSlotResult(Map<String, SlotM> slots) {
        // Calcular probabilidades acumulativas
        double totalProbability = 0.0;
        for (SlotM slot : slots.values()) {
            totalProbability += slot.getProbability();
        }

        // Generar número aleatorio
        double randomValue = random.nextDouble() * totalProbability;

        // Seleccionar slot basado en probabilidad
        double currentProbability = 0.0;
        for (SlotM slot : slots.values()) {
            currentProbability += slot.getProbability();
            if (randomValue <= currentProbability) {
                return slot;
            }
        }

        // Fallback al primer slot
        return slots.values().iterator().next();
    }

    private void executeSlotResult(SlotMachine slotMachine, SlotM slot, Player player) {
        if (slot.hasReward()) {
            // Dar recompensa
            ItemStack reward = itemCreator.createItem(slot.getItemRewardId(), slot.getItemRewardAmount());
            if (reward != null) {
                player.getInventory().addItem(reward);

                String message = ChatColor.of("#B5EAD7") + "۞ ¡Ganaste " + slot.getItemRewardAmount() + "x " + ChatColor.BOLD + slot.getName() + "!";

                if (slotMachine.isMessageBroadcast()) {
                    // Broadcast a todos los jugadores
                    for (Player onlinePlayer : plugin.getServer().getOnlinePlayers()) {
                        onlinePlayer.sendMessage(message);
                    }
                } else {
                    // Solo al jugador
                    player.sendMessage(message);
                }

                // Reproducir sonido de victoria
                if (slot.getSound() != null && !slot.getSound().isEmpty()) {
                    playSlotMachineSound(player, slot.getSound());
                }
            } else {
                player.sendMessage(ChatColor.of("#FF6B6B") + "۞ Error al crear la recompensa.");
            }
        } else {
            // Sin recompensa
            player.sendMessage(ChatColor.of("#FF6B6B") + "۞ ¡No hay suerte esta vez!");
        }
    }

    private void playSlotMachineSound(Player player, String soundName) {
        try {
            if (soundName.startsWith("dtools3:")) {
                // Sonido personalizado - convertir a formato Bukkit
                String bukkitSound = soundName.replace("dtools3:", "").replace(".", "_").toUpperCase();
                try {
                    Sound sound = Sound.valueOf(bukkitSound);
                    player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
                } catch (IllegalArgumentException e) {
                    // Si no existe el sonido, no reproducir nada
                    plugin.getLogger().warning("Sonido no encontrado: " + soundName);
                }
            } else {
                // Sonido vanilla
                try {
                    Sound sound = Sound.valueOf(soundName.toUpperCase());
                    player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Sonido vanilla no encontrado: " + soundName);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error reproduciendo sonido: " + e.getMessage());
        }
    }

    public void cleanupPlayerMachines(Player player) {
        // Limpiar máquinas del jugador
        for (SlotMachineModel model : activeMachines.values()) {
            if (model.isActive()) {
                model.setActive(false);
            }
        }
    }

    public void shutdown() {
        // Limpiar todas las máquinas activas
        for (SlotMachineModel model : activeMachines.values()) {
            if (model.getEntity() != null) {
                if (useModelEngine4 && modelEngine4 != null) {
                    modelEngine4.removeModel(model.getEntity());
                } else if (modelEngine3 != null) {
                    modelEngine3.removeModel(model.getEntity());
                }
                model.getEntity().remove();
            }
        }
        activeMachines.clear();

        // Guardar estado actual
        try {
            File dataFile = new File(plugin.getDataFolder(), "slotmachines.yml");
            if (dataFile.exists()) {
                FileConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
                config.set("machines", null);
                config.save(dataFile);
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error saving slot machines on shutdown: " + e.getMessage());
        }

        plugin.getLogger().info("SlotMachine system shutdown completed.");
    }

    // Getters
    public SlotMachineTool getSlotMachineTool() { return slotMachineTool; }
    public SlotMachineListener getListener() { return listener; }
    public ItemCreator getItemCreator() { return itemCreator; }
}