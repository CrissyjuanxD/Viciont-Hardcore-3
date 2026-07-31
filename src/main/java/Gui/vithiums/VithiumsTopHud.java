package Gui.vithiums;

import com.crissyjuanxd.viciontguiplugin.api.GuiBuilder;
import com.crissyjuanxd.viciontguiplugin.api.GuiElementBuilder;
import com.crissyjuanxd.viciontguiplugin.api.GuiTarget;
import com.crissyjuanxd.viciontguiplugin.api.ViciontGuiAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class VithiumsTopHud {

    private static final Map<UUID, Set<String>> viewerKnownPlayers = new HashMap<>();

    public static void removeHud(Player player) {
        ViciontGuiAPI.removeOverlay(player, "hud_vithiums_top");
        viewerKnownPlayers.remove(player.getUniqueId());
    }

    public static void updateHud(JavaPlugin plugin, Player viewer, List<Map.Entry<String, Integer>> sorted) {
        GuiBuilder mainHud = GuiBuilder.create("hud_vithiums_top").target(GuiTarget.HUD).fixedScale(true);
        GuiBuilder preHud = GuiBuilder.create("hud_vithiums_top").target(GuiTarget.HUD).fixedScale(true);

        int baseX = -12;
        int startY = -75;

        mainHud.element(GuiElementBuilder.image("vth_title", "viciontguis:textures/gui/vithiums_title.png")
                .anchor("right").position(baseX - 45, startY - 25).size(95, 48));
        preHud.element(GuiElementBuilder.image("vth_title", "viciontguis:textures/gui/vithiums_title.png")
                .anchor("right").position(baseX - 45, startY - 25).size(95, 48));

        Map<String, Integer> ranks = new HashMap<>();
        for (int i = 0; i < sorted.size(); i++) ranks.put(sorted.get(i).getKey(), i + 1);

        List<Map.Entry<String, Integer>> selected = new ArrayList<>();
        Set<String> selectedNames = new HashSet<>();

        // Agregar top 9
        for (int i = 0; i < Math.min(9, sorted.size()); i++) {
            selected.add(sorted.get(i));
            selectedNames.add(sorted.get(i).getKey());
        }

        // Agregar al viewer si no está en el top 9
        if (!selectedNames.contains(viewer.getName())) {
            for (Map.Entry<String, Integer> entry : sorted) {
                if (entry.getKey().equals(viewer.getName())) {
                    selected.add(entry);
                    break;
                }
            }
        } else if (sorted.size() > 9) {
            // Si el viewer ya está en el top 9, podemos mostrar el 10 normal
            selected.add(sorted.get(9));
        }

        selected.sort(Comparator.comparingInt(e -> ranks.get(e.getKey())));

        Set<String> previousKnown = viewerKnownPlayers.getOrDefault(viewer.getUniqueId(), new HashSet<>());
        Set<String> currentKnown = new HashSet<>();
        boolean hasNewAppearing = false;

        int slot = 0;
        int rowStep = 18;

        for (Map.Entry<String, Integer> entry : selected) {
            String pName = entry.getKey();
            int score = entry.getValue();
            int rank = ranks.get(pName);
            boolean isViewer = pName.equals(viewer.getName());

            currentKnown.add(pName);
            int targetY = startY + (slot * rowStep);

            if (!previousKnown.contains(pName)) {
                hasNewAppearing = true;
                addPlayerToHud(preHud, pName, score, rank, baseX, 250, isViewer);
            } else {
                addPlayerToHud(preHud, pName, score, rank, baseX, targetY, isViewer);
            }

            addPlayerToHud(mainHud, pName, score, rank, baseX, targetY, isViewer);
            slot++;
        }

        Set<String> droppedPlayers = new HashSet<>(previousKnown);
        droppedPlayers.removeAll(currentKnown);
        for (String droppedName : droppedPlayers) {
            int score = 0; int rank = 99;
            for (Map.Entry<String, Integer> e : sorted) {
                if (e.getKey().equals(droppedName)) {
                    score = e.getValue();
                    rank = ranks.get(droppedName);
                    break;
                }
            }
            addPlayerToHud(preHud, droppedName, score, rank, baseX, 250, droppedName.equals(viewer.getName()));
            addPlayerToHud(mainHud, droppedName, score, rank, baseX, 250, droppedName.equals(viewer.getName()));
        }

        viewerKnownPlayers.put(viewer.getUniqueId(), currentKnown);

        if (hasNewAppearing) {
            ViciontGuiAPI.setOverlay(viewer, preHud);
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (viewerKnownPlayers.containsKey(viewer.getUniqueId())) {
                    ViciontGuiAPI.setOverlay(viewer, mainHud);
                }
            }, 2L);
        } else {
            ViciontGuiAPI.setOverlay(viewer, mainHud);
        }
    }

    private static void addPlayerToHud(GuiBuilder hud, String name, int score, int rank, int xPos, int yPos, boolean isViewer) {
        String texture = isViewer ? "viciontguis:textures/gui/vithiums_fondotextplayer.png" : "viciontguis:textures/gui/vithiums_fondotext.png";
        String shortName = name.length() > 16 ? name.substring(0, 16) : name;
        float speed = 8.0f;
        int width = 105;
        float textScale = 0.60f;

        hud.element(GuiElementBuilder.image("bg_" + name, texture)
                .anchor("right").position(xPos - 45, yPos).size(width, 16).animSpeed(speed));

        hud.element(GuiElementBuilder.text("rnk_" + name, "N" + rank + ".", "#FFFFFF", textScale, true)
                .anchor("right").position(xPos - 92, yPos - 2).textAlign("left").animSpeed(speed));

        hud.element(GuiElementBuilder.text("txt_" + name, shortName, "#FFFFFF", textScale, true)
                .anchor("right").position(xPos - 76, yPos - 2).textAlign("left").animSpeed(speed));

        hud.element(GuiElementBuilder.text("pts_" + name, String.valueOf(score), "#FFFFFF", textScale, true)
                .anchor("right").position(xPos - 12, yPos - 2).textAlign("left").animSpeed(speed));
    }
}