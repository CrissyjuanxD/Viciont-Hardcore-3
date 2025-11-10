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

        if (level == 1) {
            if (corruption < 70) return 0.25;
            if (corruption < 60) return 0.50;
            return 0.0;
        } else if (level == 2) {
            if (corruption < 70) return 0.75;
            if (corruption < 60) return 0.90;
            if (corruption < 50) return 0.10;
            return 0.0;
        } else if (level == 3) {
            if (corruption < 70) return 0.90;
            if (corruption < 60) return 0.30;
            if (corruption < 50) return 1.0; // 100% de fallo
            return 0.0;
        }
        return 0.0;
    }
}