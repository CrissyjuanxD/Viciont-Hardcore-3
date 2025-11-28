package CorrupcionAnsiosa;

public class PlayerCorruptionData {
    private double corruption;

    public PlayerCorruptionData(double corruption) {
        this.corruption = Math.max(0, Math.min(100, corruption));
    }

    public double getCorruption() {
        return corruption;
    }

    public void setCorruption(double corruption) {
        this.corruption = Math.max(0, Math.min(100, corruption));
    }

    public void addCorruption(double amount) {
        this.corruption = Math.max(0, Math.min(100, corruption + amount));
    }

    public void removeCorruption(double amount) {
        this.corruption = Math.max(0, Math.min(100, corruption - amount));
    }

    public int getCorruptionLevel(int currentDay) {
        if (currentDay >= 20) return 3;
        if (currentDay >= 10) return 2;
        return 1;
    }

    public double getFailChance(int currentDay) {
        double corruption = getCorruption();
        int level = getCorruptionLevel(currentDay);

        // Nivel 1: sin fallos
        if (level == 1) {
            return 0.0;
        }

        // Nivel 2 (día 10+)
        if (level == 2) {
            if (corruption < 50) return 0.10;  // 10%
            if (corruption < 60) return 0.05;  // 5%
            if (corruption < 70) return 0.01;  // 1%
            return 0.0;
        }

        // Nivel 3 (día 20+)
        if (level == 3) {
            if (corruption < 50) return 1.00;  // 100%
            if (corruption < 60) return 0.30;  // 30%
            if (corruption < 70) return 0.15;  // 15%
            return 0.0;
        }

        return 0.0;
    }
}