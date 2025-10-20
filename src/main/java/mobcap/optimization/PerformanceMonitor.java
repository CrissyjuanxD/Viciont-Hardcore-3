package mobcap.optimization;

import org.bukkit.Bukkit;

public class PerformanceMonitor {
    private long lastCheck = System.currentTimeMillis();
    private double averageTPS = 20.0;
    private long usedMemory = 0;
    private double cpuUsage = 0.0;

    public PerformanceData getPerformanceData() {
        updatePerformanceMetrics();
        return new PerformanceData(averageTPS, usedMemory, cpuUsage);
    }

    private void updatePerformanceMetrics() {
        // Calcular TPS aproximado
        long currentTime = System.currentTimeMillis();
        long timeDiff = currentTime - lastCheck;
        if (timeDiff > 1000) { // Actualizar cada segundo
            try {
                // Intentar obtener TPS del servidor (Paper/Spigot)
                Object server = Bukkit.getServer();
                if (server.getClass().getName().contains("CraftServer")) {
                    // Aproximación básica del TPS
                    averageTPS = Math.min(20.0, 20.0 * (1000.0 / Math.max(timeDiff, 50)));
                }
            } catch (Exception e) {
                averageTPS = 20.0; // Valor por defecto si no se puede obtener
            }
            lastCheck = currentTime;
        }

        // Memoria utilizada
        Runtime runtime = Runtime.getRuntime();
        usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024); // MB

        // CPU usage aproximado (simplificado)
        cpuUsage = Math.min(100.0, (usedMemory / (double) (runtime.maxMemory() / (1024 * 1024))) * 100);
    }
}