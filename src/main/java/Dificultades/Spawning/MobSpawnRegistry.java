package Dificultades.Spawning;

import Handlers.DayHandler;
import Handlers.DeathStormHandler;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class MobSpawnRegistry implements Listener {

    private final JavaPlugin plugin;
    private final DayHandler dayHandler;
    private final DeathStormHandler deathStormHandler;
    private final Random random = new Random();

    private final Map<EntityType, List<SpawnRule>> rulesByTrigger = new HashMap<>();
    private final List<SpawnRule> globalRules = new ArrayList<>(); // reglas sin .triggers(), aplican a cualquier tipo
    private boolean dirty = false;

    private static final Set<CreatureSpawnEvent.SpawnReason> HARD_SKIP_REASONS = EnumSet.of(
            CreatureSpawnEvent.SpawnReason.CUSTOM,
            CreatureSpawnEvent.SpawnReason.SPAWNER
           /* CreatureSpawnEvent.SpawnReason.COMMAND*/
    );

    private boolean debugAllowCommandSpawns = false;

    public MobSpawnRegistry(JavaPlugin plugin, DayHandler dayHandler, DeathStormHandler deathStormHandler) {
        this.plugin = plugin;
        this.dayHandler = dayHandler;
        this.deathStormHandler = deathStormHandler;
    }

    public void register(SpawnRule rule) {
        if (rule.isGlobal()) {
            globalRules.add(rule);
        } else {
            for (EntityType type : rule.triggerTypes) {
                rulesByTrigger.computeIfAbsent(type, k -> new ArrayList<>()).add(rule);
            }
        }
        dirty = true;
    }

    public void unregister(String ruleId) {
        rulesByTrigger.values().forEach(list -> list.removeIf(r -> r.id.equals(ruleId)));
        globalRules.removeIf(r -> r.id.equals(ruleId));
    }

    public void setDebugAllowCommandSpawns(boolean allow) { this.debugAllowCommandSpawns = allow; }
    public boolean isDebugAllowCommandSpawns() { return debugAllowCommandSpawns; }

    private void resortIfDirty() {
        if (!dirty) return;
        Comparator<SpawnRule> bySpecificityDesc =
                Comparator.comparingDouble(SpawnRule::specificity).reversed();
        for (List<SpawnRule> list : rulesByTrigger.values()) {
            list.sort(bySpecificityDesc);
        }
        globalRules.sort(bySpecificityDesc);
        dirty = false;
    }

    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (HARD_SKIP_REASONS.contains(event.getSpawnReason())) return;
        if (event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.COMMAND && !debugAllowCommandSpawns) return;

        resortIfDirty();

        List<SpawnRule> specific = rulesByTrigger.get(event.getEntityType());

        // Fusionamos reglas específicas del tipo + reglas globales, ya ordenadas
        // por especificidad cada una. Mezclamos manteniendo ese orden relativo
        // (merge de dos listas ya ordenadas).
        List<SpawnRule> candidates = mergeSorted(specific, globalRules);
        if (candidates.isEmpty()) return;

        int day = dayHandler.getCurrentDay();
        World.Environment env = event.getLocation().getWorld().getEnvironment();
        boolean stormActive = deathStormHandler != null && deathStormHandler.isDeathStormActive();

        for (SpawnRule rule : candidates) {
            if (day < rule.minDay) continue;
            if (rule.maxDay != -1 && day > rule.maxDay) continue;
            if (!rule.environments.isEmpty() && !rule.environments.contains(env)) continue;

            if (rule.stormRequirement == SpawnRule.StormRequirement.ACTIVE && !stormActive) continue;
            if (rule.stormRequirement == SpawnRule.StormRequirement.INACTIVE && stormActive) continue;

            if (rule.markerKey != null &&
                    event.getEntity().getPersistentDataContainer().has(rule.markerKey, PersistentDataType.BYTE)) {
                continue;
            }

            if (!rule.extraCondition.test(event)) continue;
            if (random.nextDouble() >= rule.chance) continue;

            rule.action.accept(event, day);

            if (rule.exclusive) return;
        }
    }

    private List<SpawnRule> mergeSorted(List<SpawnRule> a, List<SpawnRule> b) {
        if (a == null || a.isEmpty()) return b;
        if (b.isEmpty()) return a;

        List<SpawnRule> merged = new ArrayList<>(a.size() + b.size());
        int i = 0, j = 0;
        while (i < a.size() && j < b.size()) {
            if (a.get(i).specificity() >= b.get(j).specificity()) {
                merged.add(a.get(i++));
            } else {
                merged.add(b.get(j++));
            }
        }
        while (i < a.size()) merged.add(a.get(i++));
        while (j < b.size()) merged.add(b.get(j++));
        return merged;
    }
}