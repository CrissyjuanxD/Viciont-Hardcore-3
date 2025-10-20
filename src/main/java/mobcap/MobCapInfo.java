package mobcap;
import mobcap.optimization.PerformanceData;

import java.util.Map;

public class MobCapInfo {
    private final int baseMobCap;
    private final MobCapMultiplier multiplier;
    private final int effectiveMobCap;
    private final Map<String, Integer> originalLimits;
    private final Map<String, Integer> currentLimits;
    private final int playerCount;
    private final boolean optimizationActive;
    private final boolean enabled;
    private final PerformanceData performanceData;

    public MobCapInfo(int baseMobCap, MobCapMultiplier multiplier, int effectiveMobCap,
                      Map<String, Integer> originalLimits, Map<String, Integer> currentLimits,
                      int playerCount, boolean optimizationActive, boolean enabled,
                      PerformanceData performanceData) {
        this.baseMobCap = baseMobCap;
        this.multiplier = multiplier;
        this.effectiveMobCap = effectiveMobCap;
        this.originalLimits = originalLimits;
        this.currentLimits = currentLimits;
        this.playerCount = playerCount;
        this.optimizationActive = optimizationActive;
        this.enabled = enabled;
        this.performanceData = performanceData;
    }

    // Getters
    public int getBaseMobCap() { return baseMobCap; }
    public MobCapMultiplier getMultiplier() { return multiplier; }
    public int getEffectiveMobCap() { return effectiveMobCap; }
    public Map<String, Integer> getOriginalLimits() { return originalLimits; }
    public Map<String, Integer> getCurrentLimits() { return currentLimits; }
    public int getPlayerCount() { return playerCount; }
    public boolean isOptimizationActive() { return optimizationActive; }
    public boolean isEnabled() { return enabled; }
    public PerformanceData getPerformanceData() { return performanceData; }
}