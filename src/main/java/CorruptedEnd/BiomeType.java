package CorruptedEnd;

import org.bukkit.Material;

public enum BiomeType {
    // Biomas originales
    SCULK_PLAINS(0, "Sculk Plains"),
    CRIMSON_WASTES(1, "Crimson Wastes"),

    // Nuevos biomas
    CELESTIAL_FOREST(2, "Celestial Forest"),
    OBSIDIAN_PEAKS(3, "Obsidian Peaks");

    private final int id;
    private final String name;

    BiomeType(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() { return id; }
    public String getName() { return name; }

    public static BiomeType fromId(int id) {
        for (BiomeType type : values()) {
            if (type.id == id) return type;
        }
        return SCULK_PLAINS;
    }

    // Materiales base para cada bioma
    public Material getPrimaryBlock() {
        switch (this) {
            case SCULK_PLAINS: return Material.SCULK;
            case CRIMSON_WASTES: return Material.CRIMSON_HYPHAE;
            case CELESTIAL_FOREST: return Material.SCULK;
            case OBSIDIAN_PEAKS: return Material.OBSIDIAN;
            default: return Material.SCULK;
        }
    }

    public Material getSecondaryBlock() {
        switch (this) {
            case SCULK_PLAINS: return Material.SCULK;
            case CRIMSON_WASTES: return Material.CRIMSON_HYPHAE;
            case CELESTIAL_FOREST: return Material.BLUE_TERRACOTTA;
            case OBSIDIAN_PEAKS: return Material.SCULK;
            default: return Material.SCULK;
        }
    }

    public Material getAccentBlock() {
        switch (this) {
            case SCULK_PLAINS: return Material.SCULK_CATALYST;
            case CRIMSON_WASTES: return Material.SCULK_VEIN;
            case CELESTIAL_FOREST: return Material.SCULK;
            case OBSIDIAN_PEAKS: return Material.HONEYCOMB_BLOCK;
            default: return Material.SCULK_CATALYST;
        }
    }

    // Materiales de decoración
    public Material[] getDecorationBlocks() {
        switch (this) {
            case SCULK_PLAINS:
                return new Material[]{Material.SCULK_SENSOR, Material.SCULK_SHRIEKER, Material.SCULK_VEIN};
            case CRIMSON_WASTES:
                return new Material[]{Material.SCULK_VEIN, Material.RED_MUSHROOM, Material.SCULK_SENSOR,
                        Material.SCULK_SHRIEKER, Material.CRIMSON_FUNGUS};
            case CELESTIAL_FOREST:
                return new Material[]{Material.SCULK_SHRIEKER, Material.SCULK_SENSOR, Material.BROWN_MUSHROOM};
            case OBSIDIAN_PEAKS:
                return new Material[]{Material.SCULK_SHRIEKER, Material.SCULK_SENSOR, Material.SCULK_CATALYST};
            default:
                return new Material[]{Material.SCULK_SENSOR, Material.SCULK_SHRIEKER};
        }
    }
}