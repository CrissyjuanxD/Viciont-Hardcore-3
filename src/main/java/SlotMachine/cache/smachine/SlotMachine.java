package SlotMachine.cache.smachine;

import SlotMachine.api.SlotMachineModel;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cache principal de SlotMachine - Basado en DTools3
 */
public class SlotMachine {

    private final String id;
    private final String name;
    private final String modelId;
    private final String idleAnimation;
    private final String itemRequired;
    private final double force;
    private final boolean messageBroadcast;
    private final List<String> messages;
    private final Map<String, SlotM> slots;

    // Máquinas activas
    private final Map<Location, SlotMachineModel> activeMachines = new ConcurrentHashMap<>();

    public SlotMachine(String id, ConfigurationSection section) {
        this.id = id;
        this.name = section.getString("name", id);
        this.modelId = section.getString("data.model-id", "casinod3");
        this.idleAnimation = section.getString("data.idle-animation", "idle");
        this.itemRequired = section.getString("data.item-required", "dedita_naranja");
        this.force = section.getDouble("data.force", 0.3);
        this.messageBroadcast = section.getBoolean("message-broadcast", false);
        this.messages = section.getStringList("messages");

        // Cargar slots
        this.slots = new ConcurrentHashMap<>();
        ConfigurationSection slotsSection = section.getConfigurationSection("slots");
        if (slotsSection != null) {
            for (String slotId : slotsSection.getKeys(false)) {
                ConfigurationSection slotSection = slotsSection.getConfigurationSection(slotId);
                if (slotSection != null) {
                    slots.put(slotId, new SlotM(slotId, slotSection));
                }
            }
        }
    }

    public boolean canUse(Player player, Location location) {
        SlotMachineModel machine = activeMachines.get(location);
        return machine != null && !machine.isActive();
    }

    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public String getModelId() { return modelId; }
    public String getIdleAnimation() { return idleAnimation; }
    public String getItemRequired() { return itemRequired; }
    public double getForce() { return force; }
    public boolean isMessageBroadcast() { return messageBroadcast; }
    public List<String> getMessages() { return messages; }
    public Map<String, SlotM> getSlots() { return slots; }
    public Map<Location, SlotMachineModel> getActiveMachines() { return activeMachines; }
}