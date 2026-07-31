package Gui;

import com.crissyjuanxd.viciontguiplugin.ViciontGuiPlugin;
import com.crissyjuanxd.viciontguiplugin.api.GuiBuilder;
import com.crissyjuanxd.viciontguiplugin.api.GuiElementBuilder;
import com.crissyjuanxd.viciontguiplugin.api.PagedContentProvider;
import com.crissyjuanxd.viciontguiplugin.api.ViciontGuiAPI;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class EntidadesGuiManager implements PagedContentProvider {

    private final ViciontGuiPlugin guiPlugin;

    private final Map<String, GuiMobProvider> registeredMobs = new LinkedHashMap<>();
    private final Set<String> unlockedMobs = new HashSet<>();

    public EntidadesGuiManager(JavaPlugin plugin) {
        this.guiPlugin = JavaPlugin.getPlugin(ViciontGuiPlugin.class);
        ViciontGuiAPI.onAction("open_entidades", (player, guiId, action) -> openEntidades(player));
    }

    public void registerMob(GuiMobProvider provider) {
        registeredMobs.put(provider.getEntityId(), provider);
    }

    public void unlockMob(String id) {
        unlockedMobs.add(id);
    }

    public void lockMob(String id) {
        unlockedMobs.remove(id);
    }

    public void openEntidades(Player player) {
        guiPlugin.pagedMenus().open(player, "menu_entidades", this);
    }

    @Override
    public int getPageCount(Player player) {
        return Math.max(1, (int) Math.ceil(registeredMobs.size() / 10.0));
    }

    @Override
    public void applyBackground(GuiBuilder builder, Player player, int page) {
        builder.closeSound("minecraft:custom.gui.cerrar_menu", 1.0f, 1.0f)
                .background("viciontguis:textures/gui/fondo_entidades.png", 370, 304)
                .element(GuiElementBuilder.button("btn_back", "viciontguis:textures/gui/flecha_menu_anterior.png", "return_main")
                        .position(-136, -105).size(35, 36).tooltipLine("Volver al Menú Principal", "#FFFFFF", false)
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
        int[] colX = {-126, -63, 0, 63, 126};
        int[] rowY = {-20, 69};

        int startIndex = page * 10;
        List<GuiMobProvider> mobsList = new ArrayList<>(registeredMobs.values());

        for (int i = 0; i < 10; i++) {
            int mobIndex = startIndex + i;
            if (mobIndex >= mobsList.size()) break;

            int c = i % 5;
            int r = i / 5;
            int x = colX[c];
            int y = rowY[r];

            GuiMobProvider mob = mobsList.get(mobIndex);
            boolean isUnlocked = unlockedMobs.contains(mob.getEntityId());

            if (isUnlocked) {
                GuiElementBuilder entityBtn = GuiElementBuilder.entity("e" + mobIndex, mob.getEntityType(), mob.getName(), mob.getScale())
                        .texture("viciontguis:textures/gui/entidad_descubierta.png")
                        .position(x, y).size(58, 81)
                        .tooltipLine(mob.getName(), mob.getColor(), true)
                        .tooltipLine("", "#FFFFFF", false)
                        .hoverSound("minecraft:custom.gui.pasar_cursor", 2.0f, 1.0f); // Hover Entidad desbloqueada

                for (String attribute : mob.getDynamicAttributes()) {
                    entityBtn.tooltipLine(attribute, "#FFFFFF", false);
                }

                entityBtn.tooltipLine("", "#FFFFFF", false);

                for (String descLine : mob.getDescription()) {
                    entityBtn.tooltipLine(descLine, "#FFFFFF", false);
                }
                elements.add(entityBtn);
            } else {
                elements.add(GuiElementBuilder.button("e" + mobIndex, "viciontguis:textures/gui/entidad_no_descubierta.png", null)
                        .position(x, y).size(58, 81)
                        .tooltipLine("???", "#A0A0A0", true)
                        .tooltipLine("Entidad no descubierta.", "#A0A0A0", false)
                        .hoverSound("minecraft:custom.gui.pasar_cursor", 2.0f, 1.0f)); // Hover Entidad bloqueada
            }
        }
        return elements;
    }
}