package Habilidades;

public enum HabilidadesType {
    VITALIDAD("Vitalidad"),
    RESISTENCIA("Resistencia"),
    AGILIDAD("Agilidad");

    private final String displayName;

    HabilidadesType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
