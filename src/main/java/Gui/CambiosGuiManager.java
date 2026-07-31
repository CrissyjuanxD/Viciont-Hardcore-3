package Gui;

import com.crissyjuanxd.viciontguiplugin.ViciontGuiPlugin;
import com.crissyjuanxd.viciontguiplugin.api.GuiBuilder;
import com.crissyjuanxd.viciontguiplugin.api.GuiElementBuilder;
import com.crissyjuanxd.viciontguiplugin.api.PagedContentProvider;
import com.crissyjuanxd.viciontguiplugin.api.ViciontGuiAPI;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class CambiosGuiManager implements PagedContentProvider {

    private final CambiosDataManager dataManager;
    private final ViciontGuiPlugin guiPlugin;

    public CambiosGuiManager(CambiosDataManager dataManager) {
        this.dataManager = dataManager;
        this.guiPlugin = JavaPlugin.getPlugin(ViciontGuiPlugin.class);

        ViciontGuiAPI.onAction("open_cambios", (player, guiId, action) -> openMenuCambios(player));
        ViciontGuiAPI.onAction("resume_cambios", (player, guiId, action) -> resumeMenuCambios(player));

        ViciontGuiAPI.onActionPrefix("view_cambio_", (player, guiId, action) -> {
            String id = action.replace("view_cambio_", "");
            openCambioLectura(player, id, false);
        });

        ViciontGuiAPI.onActionPrefix("toggle_cambio_", (player, guiId, action) -> {
            String id = action.replace("toggle_cambio_", "");
            boolean actual = dataManager.isLeido(player, id);
            dataManager.setLeido(player, id, !actual);

            // True para que se actualice la ventana SIN animación de cierre o apertura.
            openCambioLectura(player, id, true);
        });
    }

    public void openMenuCambios(Player player) {
        guiPlugin.pagedMenus().open(player, "menu_cambios", this);
    }

    public void resumeMenuCambios(Player player) {
        guiPlugin.pagedMenus().resume(player, "menu_cambios", this);
    }

    @Override
    public int getPageCount(Player player) {
        return Math.max(1, (int) Math.ceil(dataManager.getEntradas().size() / 16.0));
    }

    @Override
    public void applyBackground(GuiBuilder builder, Player player, int page) {
        builder.closeSound("minecraft:custom.gui.cerrar_menu", 1.0f, 1.0f)
                .background("viciontguis:textures/gui/fondo_cambios.png", 370, 304)
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
        int[] colX = {-124, -41, 41, 124};
        int[] rowY = {-44, -2, 40, 82};

        List<CambiosDataManager.CambioEntry> entradas = dataManager.getEntradas();
        int startIndex = page * 16;

        for (int i = 0; i < 16; i++) {
            int entryIndex = startIndex + i;
            if (entryIndex >= entradas.size()) break;

            CambiosDataManager.CambioEntry entry = entradas.get(entryIndex);
            boolean leido = dataManager.isLeido(player, entry.id);

            int c = i % 4;
            int r = i / 4;

            String tex = leido ? "viciontguis:textures/gui/cambio_leido.png" : "viciontguis:textures/gui/cambio_no_leido.png";

            elements.add(GuiElementBuilder.button("c_" + entry.id, tex, "view_cambio_" + entry.id)
                    .position(colX[c], rowY[r]).size(70, 31)
                    .hoverSound("minecraft:custom.gui.pasar_cursor", 2.0f, 1.0f));

            elements.add(GuiElementBuilder.text("c_" + entry.id + "_label", entry.getTitulo(), entry.colorCode, 0.66f, true)
                    .position(colX[c], rowY[r] - 3));
        }

        return elements;
    }

    private void openCambioLectura(Player player, String id, boolean isUpdate) {
        CambiosDataManager.CambioEntry entry = dataManager.getEntradas().stream()
                .filter(e -> e.id.equals(id)).findFirst().orElse(null);

        if (entry == null) return;

        boolean leido = dataManager.isLeido(player, id);
        JsonArray richMsgArray;
        try {
            richMsgArray = JsonParser.parseString(entry.jsonMessage).getAsJsonArray();
        } catch (Exception e) {
            richMsgArray = new JsonArray();
        }

        // GUI Overlay: Sonidos "CYI" para aperturas y salidas
        GuiBuilder builder = GuiBuilder.create("overlay_cambio_lectura")
                .openSound("minecraft:custom.gui.abrir_menus_cyi", 1.0f, 1.0f)
                .closeSound("minecraft:custom.gui.cerrar_menus_cyi", 1.0f, 1.0f)
                .background("viciontguis:textures/gui/fondo_cambios_lectura.png", 340, 260)

                // Fondo invisible (para salir dando click al aire)
                .element(GuiElementBuilder.invisibleButton("bg_close", "resume_cambios").position(0, 0).size(2000, 2000)
                        .clickSound("minecraft:custom.gui.cerrar_menus_cyi", 1.0f, 1.0f))

                // Botón atrás
                .element(GuiElementBuilder.button("btn_overlay_back", "viciontguis:textures/gui/flecha_menu_anterior.png", "resume_cambios")
                        .position(-126, -92).size(35, 36).tooltipLine("Atrás", "#FF5555", false)
                        .hoverSound("minecraft:custom.gui.pasar_cursor", 2.0f, 1.0f)
                        .clickSound("minecraft:custom.gui.cerrar_menus_cyi", 1.0f, 1.0f))

                .element(GuiElementBuilder.text("titulo", entry.getTitulo(), entry.colorCode, 1.6f, true).position(0, -100))
                .element(GuiElementBuilder.richText("mensaje", richMsgArray, 300, 140, "#FFFFFF").position(-150, -60))

                // Botón confirmar lectura
                .element(GuiElementBuilder.button("btn_confirmar", "viciontguis:textures/gui/confirmar_lectura.png", "toggle_cambio_" + id)
                        .position(0, 95).size(32, 32).tooltipLine(leido ? "Marcar como no leído" : "Marcar como leído", "#FFFFFF", false)
                        .hoverSound("minecraft:custom.gui.pasar_cursor", 2.0f, 1.0f)
                        .clickSound("minecraft:custom.gui.presionar_boton", 1.3f, 2.0f)); // PITCH 1.3

        if (leido) {
            builder.element(GuiElementBuilder.text("visto", "✔", "#55FF55", 1.4f, true).position(0, 88));
        }

        builder.element(GuiElementBuilder.text("hint", "Dale click para confirmar lectura", "#AAAAAA", 0.7f, false).position(0, 112));

        if (isUpdate) {
            ViciontGuiAPI.updateScreen(player, builder);
        } else {
            ViciontGuiAPI.openScreen(player, builder);
        }
    }
}