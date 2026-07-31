package Gui;

import Events.MissionSystem.Mission;
import Events.MissionSystem.MissionData;
import Events.MissionSystem.MissionHandler;
import com.crissyjuanxd.viciontguiplugin.ViciontGuiPlugin;
import com.crissyjuanxd.viciontguiplugin.api.GuiBuilder;
import com.crissyjuanxd.viciontguiplugin.api.GuiElementBuilder;
import com.crissyjuanxd.viciontguiplugin.api.PagedContentProvider;
import com.crissyjuanxd.viciontguiplugin.api.ViciontGuiAPI;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MisionesGuiManager implements PagedContentProvider {

    private final MissionHandler missionHandler;
    private final ViciontGuiPlugin guiPlugin;

    public MisionesGuiManager(MissionHandler missionHandler) {
        this.missionHandler = missionHandler;
        this.guiPlugin = JavaPlugin.getPlugin(ViciontGuiPlugin.class);

        ViciontGuiAPI.onAction("open_misiones", (player, guiId, action) -> openMisiones(player));
    }

    public void openMisiones(Player player) {
        guiPlugin.pagedMenus().open(player, "menu_misiones", this);
    }

    private String getMissionUnicode(int missionNum) {
        return switch (missionNum) {
            case 1 -> "\uE000";
            case 2 -> "\uE001";
            case 3 -> "\uE002";
            default -> "";
        };
    }

    @Override
    public int getPageCount(Player player) {
        int total = missionHandler.getMissions().size();
        return Math.max(1, (int) Math.ceil(total / 15.0));
    }

    @Override
    public void applyBackground(GuiBuilder builder, Player player, int page) {
        builder.closeSound("minecraft:custom.gui.cerrar_menu", 1.0f, 1.0f)
                .background("viciontguis:textures/gui/fondo_misiones.png", 370, 304)
                .element(GuiElementBuilder.button("btn_back", "viciontguis:textures/gui/flecha_menu_anterior.png", "return_main")
                        .position(-136, -105).size(35, 36)
                        .tooltipLine("Volver al Menú Principal", "#FFFFFF", false)
                        .hoverSound("minecraft:custom.gui.pasar_cursor", 2.0f, 1.0f)
                        .clickSound("minecraft:custom.gui.presionar_boton2", 1.0f, 2.0f)) // PITCH 1.0
                .element(GuiElementBuilder.button("btn_prev", "viciontguis:textures/gui/flecha_izq.png", "prev_page")
                        .position(-49, 122).size(35, 36)
                        .hoverSound("minecraft:custom.gui.pasar_cursor", 2.0f, 1.0f)
                        .clickSound("minecraft:custom.gui.presionar_boton2", 0.8f, 2.0f)) // PITCH 0.8
                .element(GuiElementBuilder.button("btn_next", "viciontguis:textures/gui/flecha_der.png", "next_page")
                        .position(49, 122).size(35, 36)
                        .hoverSound("minecraft:custom.gui.pasar_cursor", 2.0f, 1.0f)
                        .clickSound("minecraft:custom.gui.presionar_boton2", 0.8f, 2.0f)); // PITCH 0.8

        int total = getPageCount(player);
        builder.element(GuiElementBuilder.text("page_ind", "Página " + (page + 1) + "/" + total, "#FFFFFF", 0.8f, true).position(0, 119));
    }

    @Override
    public List<GuiElementBuilder> buildPageElements(Player player, int page) {
        List<GuiElementBuilder> elements = new ArrayList<>();

        int[] colX = {-109, -54, 0, 54, 109};
        int[] rowY = {-22, 28, 78};

        int startIndex = page * 15;
        Map<Integer, Mission> allMissions = missionHandler.getMissions();
        int totalMissions = allMissions.size();

        for (int i = 0; i < 15; i++) {
            int missionNum = startIndex + i + 1;

            if (missionNum > totalMissions) break;

            int c = i % 5;
            int r = i / 5;
            int x = colX[c];
            int y = rowY[r];

            Mission mission = allMissions.get(missionNum);
            MissionData data = missionHandler.getData(player, missionNum);

            GuiElementBuilder button = GuiElementBuilder.button("m" + missionNum, "", null)
                    .position(x, y).size(45, 46)
                    .hoverSound("minecraft:custom.gui.pasar_cursor", 2.0f, 1.0f); // Hover sobre misiones

            if (mission == null) {
                button.texture("viciontguis:textures/gui/misiones_no_descubiertas.png")
                        .tooltipLine("???", "#A0A0A0", true)
                        .tooltipLine("Misión no implementada.", "#A0A0A0", false);
            } else if (!data.isActive()) {
                button.texture("viciontguis:textures/gui/misiones_no_descubiertas.png")
                        .tooltipLine("???", "#A0A0A0", true)
                        .tooltipLine("Misión no descubierta.", "#A0A0A0", false);
            } else {
                if (data.isCompleted()) {
                    button.texture("viciontguis:textures/gui/misiones_completadas.png");
                    button.tooltipLine(mission.getName(), "#90EE90", false);
                } else {
                    button.texture("viciontguis:textures/gui/misiones_no_completadas.png");
                    button.tooltipLine(mission.getName(), "#FFB6C1", false);
                }

                for (String line : mission.getDescription().split("\n")) {
                    button.tooltipLine(line, "#D3D3D3", false);
                }

                button.tooltipLine("", "#FFFFFF", false);

                if (data.isCompleted()) {
                    button.tooltipLine("✔ Completada", "#98FB98", false);
                } else {
                    button.tooltipLine("✖ Pendiente", "#FFA07A", false);
                }

                addMissionSpecificProgress(missionNum, data, button);
            }

            elements.add(button);

            if (mission != null && data.isActive()) {
                String unicodeItem = getMissionUnicode(missionNum);

                if (!unicodeItem.isEmpty()) {
                    elements.add(GuiElementBuilder.text("m" + missionNum + "_icon", unicodeItem, "#FFFFFF", 0.8f, false)
                            .position(x, y - 5));
                }

                if (data.isCompleted()) {
                    elements.add(GuiElementBuilder.image("m" + missionNum + "_check", "viciontguis:textures/gui/mision_completada_check.png")
                            .position(x, y).size(45, 47));
                }
            }
        }

        return elements;
    }

    private void addMissionSpecificProgress(int missionNum, MissionData data, GuiElementBuilder lore) {
        if (missionNum == 1) {
            lore.tooltipLine("", "#FFFFFF", false);
            lore.tooltipLine("Progreso de armadura:", "#F0E68C", false);

            String[] armorPieces = {"helmet", "chestplate", "leggings", "boots"};
            String[] armorNames = {"Casco", "Peto", "Pantalones", "Botas"};

            for (int i = 0; i < armorPieces.length; i++) {
                boolean hasEnchant = data.getProgressBool("protection_" + armorPieces[i]);
                lore.tooltipLine("- " + armorNames[i] + " con Protección IV", hasEnchant ? "#98FB98" : "#D3D3D3", false);
            }
        } else if (missionNum == 2) {
            lore.tooltipLine("", "#FFFFFF", false);
            lore.tooltipLine("Progreso de preparación:", "#F0E68C", false);

            boolean raidCompleted = data.getProgressBool("raid_completed");
            int goldenApplesCrafted = data.getProgressInt("golden_apples_crafted");

            lore.tooltipLine("- Raid completada", raidCompleted ? "#98FB98" : "#D3D3D3", false);
            lore.tooltipLine("- Manzanas de oro: " + goldenApplesCrafted + "/40", "#DDA0DD", false);
        } else if (missionNum == 3) {
            lore.tooltipLine("", "#FFFFFF", false);
            lore.tooltipLine("Progreso de combate:", "#F0E68C", false);
            lore.tooltipLine("- Reina Derrotada: " + (data.isCompleted() ? "✔" : "✖"), data.isCompleted() ? "#98FB98" : "#FFA07A", false);
        } else if (missionNum == 5) {
            lore.tooltipLine("", "#FFFFFF", false);
            lore.tooltipLine("Progreso de armadura:", "#F0E68C", false);

            String[] armorPieces = {"helmet", "chestplate", "leggings", "boots"};
            String[] armorNames = {"Casco", "Peto", "Pantalones", "Botas"};

            for (int i = 0; i < armorPieces.length; i++) {
                boolean hasArmor = data.getProgressBool("netherite_armor_" + armorPieces[i]);
                boolean hasProtection = data.getProgressBool("protection_" + armorPieces[i]);

                if (hasArmor && hasProtection) {
                    lore.tooltipLine("- " + armorNames[i] + " de Netherite con Prot IV", "#98FB98", false);
                } else if (hasArmor) {
                    lore.tooltipLine("- " + armorNames[i] + " de Netherite (sin Prot IV)", "#F0E68C", false);
                } else {
                    lore.tooltipLine("- " + armorNames[i] + " de Netherite con Prot IV", "#D3D3D3", false);
                }
            }
        } else if (missionNum == 6) {
            lore.tooltipLine("", "#FFFFFF", false);
            lore.tooltipLine("Progreso de eliminaciones:", "#F0E68C", false);

            int zombies = data.getProgressInt("corrupted_zombies_killed");
            int spiders = data.getProgressInt("corrupted_spiders_killed");

            lore.tooltipLine("- Corrupted Zombies: " + zombies + "/25", "#DDA0DD", false);
            lore.tooltipLine("- Corrupted Spiders: " + spiders + "/25", "#DDA0DD", false);
        } else if (missionNum == 7) {
            lore.tooltipLine("", "#FFFFFF", false);
            lore.tooltipLine("Progreso de eliminaciones:", "#F0E68C", false);

            int skeletons = data.getProgressInt("corrupted_skeletons_killed");
            int creepers = data.getProgressInt("corrupted_creepers_killed");

            lore.tooltipLine("- Corrupted Skeletons: " + skeletons + "/30", "#DDA0DD", false);
            lore.tooltipLine("- Corrupted Creepers: " + creepers + "/30", "#DDA0DD", false);
        } else if (missionNum == 8) {
            lore.tooltipLine("", "#FFFFFF", false);
            lore.tooltipLine("Progreso de armadura corrupta:", "#F0E68C", false);

            String[] armorPieces = {"helmet", "chestplate", "leggings", "boots"};
            String[] armorNames = {"Casco", "Peto", "Pantalones", "Botas"};

            for (int i = 0; i < armorPieces.length; i++) {
                boolean hasArmor = data.getProgressBool("corrupted_armor_" + armorPieces[i]);
                lore.tooltipLine("- " + armorNames[i] + " Corrupto", hasArmor ? "#98FB98" : "#D3D3D3", false);
            }
        } else if (missionNum == 9) {
            lore.tooltipLine("", "#FFFFFF", false);
            lore.tooltipLine("Progreso de raids:", "#F0E68C", false);

            int raids = data.getProgressInt("raids_completed");
            lore.tooltipLine("- Raids completadas: " + raids + "/5", "#DDA0DD", false);
        } else if (missionNum == 10) {
            lore.tooltipLine("", "#FFFFFF", false);
            lore.tooltipLine("Progreso de totems:", "#F0E68C", false);

            lore.tooltipLine("- Infernal Totem", data.getProgressBool("totems_infernal") ? "#98FB98" : "#D3D3D3", false);
            lore.tooltipLine("- Spider Totem", data.getProgressBool("totems_spider") ? "#98FB98" : "#D3D3D3", false);
            lore.tooltipLine("- Life Totem", data.getProgressBool("totems_life") ? "#98FB98" : "#D3D3D3", false);
        } else if (missionNum == 11) {
            lore.tooltipLine("", "#FFFFFF", false);
            lore.tooltipLine("Progreso de tiempo:", "#F0E68C", false);

            long timeInMushroom = data.getProgressLong("time_in_mushroom");
            lore.tooltipLine("- Tiempo en Mushroom Island: " + timeInMushroom + "/23500", "#DDA0DD", false);
        } else if (missionNum == 12) {
            lore.tooltipLine("", "#FFFFFF", false);
            lore.tooltipLine("Progreso de eliminaciones:", "#F0E68C", false);

            int bombitas = data.getProgressInt("bombitas_killed");
            int brutes = data.getProgressInt("brutes_killed");

            lore.tooltipLine("- Bombitas: " + bombitas + "/30", "#DDA0DD", false);
            lore.tooltipLine("- Brutes Imperiales: " + brutes + "/20", "#DDA0DD", false);
        }
    }
}