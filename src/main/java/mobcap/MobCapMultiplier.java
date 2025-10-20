package mobcap;

public enum MobCapMultiplier {
    NORMAL(1, "Normal"),
    DOUBLE(2, "Doble"),
    TRIPLE(3, "Triple");

    private final int multiplier;
    private final String displayName;

    MobCapMultiplier(int multiplier, String displayName) {
        this.multiplier = multiplier;
        this.displayName = displayName;
    }

    public int getMultiplier() {
        return multiplier;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static MobCapMultiplier fromString(String value) {
        for (MobCapMultiplier mult : values()) {
            if (mult.name().equalsIgnoreCase(value) ||
                    mult.getDisplayName().equalsIgnoreCase(value)) {
                return mult;
            }
        }
        return NORMAL;
    }
}