package mobcap.optimization;

public class PerformanceData {
    private final double tps;
    private final long memoryUsageMB;
    private final double cpuUsage;

    public PerformanceData(double tps, long memoryUsageMB, double cpuUsage) {
        this.tps = tps;
        this.memoryUsageMB = memoryUsageMB;
        this.cpuUsage = cpuUsage;
    }

    public double getTps() {
        return tps;
    }

    public long getMemoryUsageMB() {
        return memoryUsageMB;
    }

    public double getCpuUsage() {
        return cpuUsage;
    }

    public String getFormattedTps() {
        if (tps >= 19.5) return "§a" + String.format("%.1f", tps);
        if (tps >= 18.0) return "§e" + String.format("%.1f", tps);
        return "§c" + String.format("%.1f", tps);
    }

    public String getFormattedMemory() {
        if (memoryUsageMB < 1024) return "§a" + memoryUsageMB + "MB";
        if (memoryUsageMB < 2048) return "§e" + memoryUsageMB + "MB";
        return "§c" + memoryUsageMB + "MB";
    }

    public String getFormattedCpuUsage() {
        if (cpuUsage < 50) return "§a" + String.format("%.1f%%", cpuUsage);
        if (cpuUsage < 80) return "§e" + String.format("%.1f%%", cpuUsage);
        return "§c" + String.format("%.1f%%", cpuUsage);
    }
}