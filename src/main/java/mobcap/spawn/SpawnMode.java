package mobcap.spawn;

public enum SpawnMode {
    VANILLA("vanilla", "Spawn normal de Minecraft"),
    CUSTOM("custom", "Spawn personalizado (día y noche)");

    private final String configName;
    private final String description;

    SpawnMode(String configName, String description) {
        this.configName = configName;
        this.description = description;
    }

    public String getConfigName() {
        return configName;
    }

    public String getDescription() {
        return description;
    }

    public static SpawnMode fromString(String value) {
        for (SpawnMode mode : values()) {
            if (mode.configName.equalsIgnoreCase(value) || mode.name().equalsIgnoreCase(value)) {
                return mode;
            }
        }
        return VANILLA;
    }
}