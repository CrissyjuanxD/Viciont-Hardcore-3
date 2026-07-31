package Events.ItemParty;

import com.crissyjuanxd.viciontguiplugin.api.GuiBuilder;
import com.crissyjuanxd.viciontguiplugin.api.GuiElementBuilder;
import com.crissyjuanxd.viciontguiplugin.api.GuiTarget;
import com.crissyjuanxd.viciontguiplugin.api.ViciontGuiAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class ItemPartyHudManager {

    private static final Map<UUID, Set<String>> viewerKnownPlayers = new HashMap<>();

    public static void clearCache() {
        viewerKnownPlayers.clear();
    }

    public static void updateHud(JavaPlugin plugin, Player viewer, List<Map.Entry<String, Integer>> sorted, int dangerCount) {
        GuiBuilder mainHud = GuiBuilder.create("hud_itemparty").target(GuiTarget.HUD).fixedScale(true);
        GuiBuilder preHud = GuiBuilder.create("hud_itemparty").target(GuiTarget.HUD).fixedScale(true);

        int baseX = -12; // Base de anclaje derecho

        // 1. HUD completo bajado un poco (de -90 a -75)
        int startY = -75;

        // 2. Título bajado 3 puntos relativos (de -35 a -32)
        mainHud.element(GuiElementBuilder.image("itp_title", "viciontguis:textures/gui/itemparty_title.png")
                .anchor("right").position(baseX - 45, startY - 32).size(95, 48));
        preHud.element(GuiElementBuilder.image("itp_title", "viciontguis:textures/gui/itemparty_title.png")
                .anchor("right").position(baseX - 45, startY - 32).size(95, 48));

        Map<String, Integer> ranks = new HashMap<>();
        for (int i = 0; i < sorted.size(); i++) {
            ranks.put(sorted.get(i).getKey(), i + 1);
        }

        int maxTotal = 10;
        int maxNormals = Math.max(0, maxTotal - dangerCount);

        List<Map.Entry<String, Integer>> selected = new ArrayList<>();
        Set<String> selectedNames = new HashSet<>();

        // 1. TOP Normales
        for (Map.Entry<String, Integer> entry : sorted) {
            int rank = ranks.get(entry.getKey());
            // Solo añadimos si no está en la zona de penalizados y aún hay espacio para normales
            if (rank <= sorted.size() - dangerCount && selected.size() < maxNormals) {
                selected.add(entry);
                selectedNames.add(entry.getKey());
            }
        }

        // 2. PENALIZADOS
        int dangerStartIndex = Math.max(0, sorted.size() - dangerCount);
        for (int i = dangerStartIndex; i < sorted.size(); i++) {
            if (!selectedNames.contains(sorted.get(i).getKey())) {
                selected.add(sorted.get(i));
                selectedNames.add(sorted.get(i).getKey());
            }
        }

        // 3. JUGADOR ACTUAL (Garantizar que NO se quede fuera si es normal y no está en el top)
        if (!selectedNames.contains(viewer.getName())) {
            int viewerRank = ranks.get(viewer.getName());
            boolean viewerIsPenalized = viewerRank > (sorted.size() - dangerCount);

            if (!viewerIsPenalized) {
                for (int i = selected.size() - 1; i >= 0; i--) {
                    int r = ranks.get(selected.get(i).getKey());
                    if (r <= (sorted.size() - dangerCount)) {
                        String removed = selected.remove(i).getKey();
                        selectedNames.remove(removed);
                        break;
                    }
                }
                // Añadimos al viewer
                for (Map.Entry<String, Integer> e : sorted) {
                    if (e.getKey().equals(viewer.getName())) {
                        selected.add(e);
                        selectedNames.add(e.getKey());
                        break;
                    }
                }
            }
        }

        // 4. FILTRAR LOS DE 0 PUNTOS (Fix Punto 5: Los penalizados SIEMPRE pasan el filtro)
        Iterator<Map.Entry<String, Integer>> it = selected.iterator();
        while (it.hasNext()) {
            Map.Entry<String, Integer> entry = it.next();
            int rank = ranks.get(entry.getKey());
            boolean isPenalized = rank > (sorted.size() - dangerCount);

            if (entry.getValue() == 0 && !isPenalized) {
                it.remove();
            }
        }

        // Reordenar visualmente por rango
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
            boolean isPenalized = rank > (sorted.size() - dangerCount);

            currentKnown.add(pName);
            int targetY = startY + (slot * rowStep);

            if (!previousKnown.contains(pName)) {
                hasNewAppearing = true;
                addPlayerToHud(preHud, pName, score, rank, baseX, 250, isViewer, isPenalized);
            } else {
                addPlayerToHud(preHud, pName, score, rank, baseX, targetY, isViewer, isPenalized);
            }

            addPlayerToHud(mainHud, pName, score, rank, baseX, targetY, isViewer, isPenalized);
            slot++;
        }

        // 5. JUGADORES FANTASMA (Fix Punto 4: Animar hacia abajo a los que salen del top)
        Set<String> droppedPlayers = new HashSet<>(previousKnown);
        droppedPlayers.removeAll(currentKnown);
        for (String droppedName : droppedPlayers) {
            int score = 0; int rank = 99; boolean isPenalized = false;
            // Intentar recuperar los datos visuales del jugador caído para que baje con su textura correcta
            for (Map.Entry<String, Integer> e : sorted) {
                if (e.getKey().equals(droppedName)) {
                    score = e.getValue();
                    rank = ranks.get(droppedName);
                    isPenalized = rank > (sorted.size() - dangerCount);
                    break;
                }
            }
            // Mandamos a los que salieron a Y=250 para que el mod los anime bajando y desapareciendo
            addPlayerToHud(preHud, droppedName, score, rank, baseX, 250, droppedName.equals(viewer.getName()), isPenalized);
            addPlayerToHud(mainHud, droppedName, score, rank, baseX, 250, droppedName.equals(viewer.getName()), isPenalized);
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

    private static void addPlayerToHud(GuiBuilder hud, String name, int score, int rank, int xPos, int yPos, boolean isViewer, boolean isPenalized) {
        String texture;
        if (isPenalized) {
            texture = "viciontguis:textures/gui/itemparty_fondotextpenalizados.png";
        } else if (isViewer) {
            texture = "viciontguis:textures/gui/itemparty_fondotextplayer.png";
        } else {
            texture = (rank % 2 != 0) ? "viciontguis:textures/gui/itemparty_fondotext1.png" : "viciontguis:textures/gui/itemparty_fondotext2.png";
        }

        String shortName = name;
        if (shortName.length() > 16) shortName = shortName.substring(0, 16);

        float speed = 8.0f;
        int width = 105;
        float textScale = 0.60f;

        hud.element(GuiElementBuilder.image("bg_" + name, texture)
                .anchor("right").position(xPos - 45, yPos).size(width, 16).animSpeed(speed));

        hud.element(GuiElementBuilder.text("rnk_" + name, "N" + rank + ".", "#FFFFFF", textScale, true)
                .anchor("right").position(xPos - 92, yPos - 2).textAlign("left").animSpeed(speed));

        hud.element(GuiElementBuilder.text("txt_" + name, shortName, "#FFFFFF", textScale, true)
                .anchor("right").position(xPos - 76, yPos - 2).textAlign("left").animSpeed(speed));

        // Fix Punto 3: Alineación a la izquierda creciendo hacia la derecha (Posición X ajustada a -25)
        hud.element(GuiElementBuilder.text("pts_" + name, String.valueOf(score), "#FFFFFF", textScale, true)
                .anchor("right").position(xPos - 12, yPos - 2).textAlign("left").animSpeed(speed));
    }
}