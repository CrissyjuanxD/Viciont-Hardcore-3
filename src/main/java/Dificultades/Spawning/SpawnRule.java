package Dificultades.Spawning;

import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.event.entity.CreatureSpawnEvent;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

public class SpawnRule {

    public enum StormRequirement { ANY, ACTIVE, INACTIVE }

    public final String id;
    public final Set<EntityType> triggerTypes; // vacío = regla GLOBAL (cualquier tipo)
    public final int minDay;
    public final int maxDay;
    public final Set<World.Environment> environments;
    public final double chance;
    public final NamespacedKey markerKey;
    public final Predicate<CreatureSpawnEvent> extraCondition;
    public final BiConsumer<CreatureSpawnEvent, Integer> action;
    public final boolean exclusive;
    public final StormRequirement stormRequirement;
    private final boolean hasCustomCondition;

    private SpawnRule(Builder b) {
        this.id = b.id;
        this.triggerTypes = b.triggerTypes;
        this.minDay = b.minDay;
        this.maxDay = b.maxDay;
        this.environments = b.environments;
        this.chance = b.chance;
        this.markerKey = b.markerKey;
        this.extraCondition = b.extraCondition;
        this.action = b.action;
        this.exclusive = b.exclusive;
        this.stormRequirement = b.stormRequirement;
        this.hasCustomCondition = b.hasCustomCondition;
    }

    public boolean isGlobal() {
        return triggerTypes.isEmpty();
    }

    public double specificity() {
        double score = 0;

        if (stormRequirement != StormRequirement.ANY) score += 1000;
        if (!environments.isEmpty()) score += 100;
        if (hasCustomCondition) score += 50;

        // Una regla global es, por naturaleza, MENOS específica que una
        // con tipos exactos: la penalizamos para que las reglas
        // "type-specific" tengan preferencia natural si compiten.
        if (isGlobal()) score -= 500;

        if (maxDay != -1) {
            int window = Math.max(0, maxDay - minDay);
            score += 200 - Math.min(200, window * 10);
        } else {
            score += minDay;
        }

        score += (1.0 - chance) * 30;

        return score;
    }

    public static Builder builder(String id) {
        return new Builder(id);
    }

    public static class Builder {
        private final String id;
        private Set<EntityType> triggerTypes = EnumSet.noneOf(EntityType.class);
        private int minDay = 0;
        private int maxDay = -1;
        private Set<World.Environment> environments = EnumSet.noneOf(World.Environment.class);
        private double chance = 1.0;
        private NamespacedKey markerKey;
        private Predicate<CreatureSpawnEvent> extraCondition = e -> true;
        private BiConsumer<CreatureSpawnEvent, Integer> action;
        private boolean exclusive = true;
        private StormRequirement stormRequirement = StormRequirement.ANY;
        private boolean hasCustomCondition = false;

        private Builder(String id) { this.id = id; }

        /** Restringe la regla a tipos exactos (indexado, rápido). Si no la llamás, la regla es GLOBAL. */
        public Builder triggers(EntityType... types) {
            this.triggerTypes = EnumSet.copyOf(List.of(types));
            return this;
        }

        public Builder dayRange(int min, int max) { this.minDay = min; this.maxDay = max; return this; }
        public Builder fromDay(int min) { this.minDay = min; return this; }

        public Builder environments(World.Environment... envs) {
            this.environments = EnumSet.copyOf(List.of(envs));
            return this;
        }

        public Builder chance(double c) { this.chance = c; return this; }
        public Builder marker(NamespacedKey key) { this.markerKey = key; return this; }

        public Builder condition(Predicate<CreatureSpawnEvent> p) {
            this.extraCondition = p;
            this.hasCustomCondition = true;
            return this;
        }

        public Builder action(BiConsumer<CreatureSpawnEvent, Integer> a) { this.action = a; return this; }
        public Builder nonExclusive() { this.exclusive = false; return this; }
        public Builder duringStorm() { this.stormRequirement = StormRequirement.ACTIVE; return this; }
        public Builder outsideStorm() { this.stormRequirement = StormRequirement.INACTIVE; return this; }

        public SpawnRule build() { return new SpawnRule(this); }
    }
}