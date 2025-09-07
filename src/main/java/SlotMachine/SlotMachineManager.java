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
        entity.setVisible(false);
        entity.setGravity(false);
        entity.setInvulnerable(true);
        entity.setCanPickupItems(false);
        entity.setCustomName("SlotMachine");
        entity.setCustomNameVisible(false);
        entity.setSmall(false);
        entity.setArms(false);
        entity.setBasePlate(false);
        entity.setMarker(true);

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
        return true;
    }

    public boolean removeSlotMachine(Location location) {
        SlotMachineModel model = activeMachines.remove(location);
        if (model == null) {
            return false;
        }

        if (model.getEntity() != null) {
            if (useModelEngine4 && modelEngine4 != null) {
                modelEngine4.removeModel(model.getEntity());
            } else if (modelEngine3 != null) {
                modelEngine3.removeModel(model.getEntity());
            }
            model.getEntity().remove();
        }

        return true;
    }

    public void reloadConfiguration() {
        loadConfiguration();
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

        // Reproducir animación si está disponible
        if (model.getModelUUID() != null && useModelEngine4 && modelEngine4 != null) {
            modelEngine4.playAnimation(model.getModelUUID(), slotMachine.getModelId(), selectedSlot.getAnimation());
        }

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            executeSlotResult(slotMachine, selectedSlot, player);
            model.setActive(false);
        }, waitTicks);
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

                String message = ChatColor.of("#B5EAD7") + "۞ ¡Ganaste " + slot.getItemRewardAmount() + "x " + slot.getName() + "!";

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
                player.playSound(player.getLocation(), slot.getSound(), 1.0f, 1.0f);
            } else {
                player.sendMessage(ChatColor.of("#FF6B6B") + "۞ Error al crear la recompensa.");
            }
        } else {
            // Sin recompensa
            player.sendMessage(ChatColor.of("#FF6B6B") + "۞ ¡No hay suerte esta vez!");
            player.playSound(player.getLocation(), "dtools3:tools.casino.lose", 1.0f, 1.0f);
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

        plugin.getLogger().info("SlotMachine system shutdown completed.");
    }

    // Getters
    public SlotMachineTool getSlotMachineTool() { return slotMachineTool; }
    public SlotMachineListener getListener() { return listener; }
    public ItemCreator getItemCreator() { return itemCreator; }
}