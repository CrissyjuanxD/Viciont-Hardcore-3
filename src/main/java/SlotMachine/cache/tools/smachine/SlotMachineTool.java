package SlotMachine.cache.tools.smachine;

import SlotMachine.cache.smachine.SlotMachine;
import org.bukkit.configuration.ConfigurationSection;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tool para manejar SlotMachines - Basado en DTools3
 */
public class SlotMachineTool {
    
    private final Map<String, SlotMachine> slotMachines = new ConcurrentHashMap<>();
    private final boolean enabled;
    private final SlotMachineFrame config;
    
    public SlotMachineTool(ConfigurationSection section) {
        this.enabled = section.getBoolean("enabled", true);
        this.config = new SlotMachineFrame(section.getConfigurationSection("config"));
        
        // Cargar slot machines
        ConfigurationSection machinesSection = section.getConfigurationSection("slot_machines");
        if (machinesSection != null) {
            for (String machineId : machinesSection.getKeys(false)) {
                ConfigurationSection machineSection = machinesSection.getConfigurationSection(machineId);
                if (machineSection != null) {
                    slotMachines.put(machineId, new SlotMachine(machineId, machineSection));
                }
            }
        }
    }
    
    public SlotMachine getSlotMachine(String id) {
        return slotMachines.get(id);
    }
    
    public SlotMachine getDefaultSlotMachine() {
        return slotMachines.get("default");
    }
    
    // Getters
    public boolean isEnabled() { return enabled; }
    public SlotMachineFrame getConfig() { return config; }
    public Map<String, SlotMachine> getSlotMachines() { return slotMachines; }
}