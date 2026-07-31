package Gui;

import java.util.ArrayList;
import java.util.List;

public class ViciontItem {
    private final String id;
    private final String material;
    private final int customModelData;
    private final int unlockDay;
    private final boolean hasRecipe;
    private final String action;
    private final List<TooltipLine> tooltipLines = new ArrayList<>();

    public ViciontItem(String id, String material, int customModelData, int unlockDay, boolean hasRecipe, String action) {
        this.id = id;
        this.material = material;
        this.customModelData = customModelData;
        this.unlockDay = unlockDay;
        this.hasRecipe = hasRecipe;
        this.action = action;
    }

    // Helper para crear ítems Vanilla rápidos (Ingredientes)
    public static ViciontItem vanilla(String material, String name) {
        return vanilla(material, name, 0, "#FFFFFF", false);
    }

    public static ViciontItem vanilla(String material, String name, int cmd, String color, boolean bold) {
        ViciontItem item = new ViciontItem("vanilla", material, cmd, 0, false, null);
        item.addTooltip(name, color, bold);
        return item;
    }

    public void addTooltip(String text, String color, boolean bold) {
        this.tooltipLines.add(new TooltipLine(text, color, bold));
    }

    public void addRawTooltip(String rawText) {
        this.tooltipLines.add(new TooltipLine(rawText, "", false));
    }

    public String getId() { return id; }
    public String getMaterial() { return material; }
    public int getCustomModelData() { return customModelData; }
    public int getUnlockDay() { return unlockDay; }
    public boolean hasRecipe() { return hasRecipe; }
    public String getAction() { return action; }
    public List<TooltipLine> getTooltipLines() { return tooltipLines; }

    public record TooltipLine(String text, String color, boolean bold) {}
}