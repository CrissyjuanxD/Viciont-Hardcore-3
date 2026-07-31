package Gui;

import Handlers.DayHandler;
import com.crissyjuanxd.viciontguiplugin.ViciontGuiPlugin;
import com.crissyjuanxd.viciontguiplugin.api.GuiBuilder;
import com.crissyjuanxd.viciontguiplugin.api.GuiElementBuilder;
import com.crissyjuanxd.viciontguiplugin.api.PagedContentProvider;
import com.crissyjuanxd.viciontguiplugin.api.ViciontGuiAPI;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class ItemsGuiManager implements PagedContentProvider {

    private final ViciontItemRegistry itemRegistry;
    private final ViciontRecipeRegistry recipeRegistry;
    private final DayHandler dayHandler;
    private final ViciontGuiPlugin guiPlugin;

    public ItemsGuiManager(DayHandler dayHandler) {
        this.dayHandler = dayHandler;
        this.itemRegistry = new ViciontItemRegistry();
        this.recipeRegistry = new ViciontRecipeRegistry(this.itemRegistry);
        this.guiPlugin = JavaPlugin.getPlugin(ViciontGuiPlugin.class);

        ViciontGuiAPI.onAction("open_recetas", (player, guiId, action) -> openMenuItems(player));
        ViciontGuiAPI.onAction("resume_recetas", (player, guiId, action) -> resumeMenuItems(player));
        ViciontGuiAPI.onActionPrefix("view_recipe_", (player, guiId, action) -> openDynamicOverlay(player, action));
    }

    private void openMenuItems(Player player) {
        guiPlugin.pagedMenus().open(player, "menu_items_recetas", this);
    }

    private void resumeMenuItems(Player player) {
        guiPlugin.pagedMenus().resume(player, "menu_items_recetas", this);
    }

    @Override
    public int getPageCount(Player player) {
        return Math.max(1, (int) Math.ceil(itemRegistry.getDisplayItems().size() / 32.0));
    }

    @Override
    public void applyBackground(GuiBuilder builder, Player player, int page) {
        builder.closeSound("minecraft:custom.gui.cerrar_menu", 1.0f, 1.0f)
                .background("viciontguis:textures/gui/fondo_recetas.png", 370, 304)
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

        builder.element(GuiElementBuilder.image("leg_img1", "viciontguis:textures/gui/marco_recetas.png").position(-155, 120).size(16, 16))
                .element(GuiElementBuilder.text("leg_txt1", "Items con Recetas", "#AAAAAA", 0.66f, false).position(-110, 117))
                .element(GuiElementBuilder.image("leg_img2", "viciontguis:textures/gui/marco_item.png").position(-155, 135).size(16, 16))
                .element(GuiElementBuilder.text("leg_txt2", "Items sin Recetas", "#AAAAAA", 0.66f, false).position(-110, 132));

        int total = getPageCount(player);
        builder.element(GuiElementBuilder.text("page_ind", "Página " + (page + 1) + "/" + total, "#FFFFFF", 0.8f, true).position(0, 119));
    }

    @Override
    public List<GuiElementBuilder> buildPageElements(Player player, int page) {
        List<GuiElementBuilder> elements = new ArrayList<>();

        int[] colX = {-150, -107, -64, -21, 22, 65, 108, 151};
        int[] rowY = {-50, -7, 36, 79};

        int startIndex = page * 32;
        List<ViciontItem> allItems = itemRegistry.getDisplayItems();
        int currentDay = dayHandler.getCurrentDay();

        for (int i = 0; i < 32; i++) {
            int itemIndex = startIndex + i;
            if (itemIndex >= allItems.size()) break;

            int c = i % 8;
            int r = i / 8;
            int x = colX[c];
            int y = rowY[r];

            ViciontItem itemData = allItems.get(itemIndex);

            if (currentDay >= itemData.getUnlockDay()) {
                String frameTexture = itemData.hasRecipe()
                        ? "viciontguis:textures/gui/marco_recetas.png"
                        : "viciontguis:textures/gui/marco_item.png";

                elements.add(buildItemElement("i_" + itemIndex, itemData, frameTexture, x, y, 35));
            } else {
                elements.add(GuiElementBuilder.image("i_locked_" + itemIndex, "viciontguis:textures/gui/marco_items_no_descubierta.png")
                        .position(x, y).size(34, 35)
                        .tooltipLine("???", "#A0A0A0", true)
                        .tooltipLine("Item no descubierto.", "#A0A0A0", false));
            }
        }
        return elements;
    }

    private void openDynamicOverlay(Player player, String actionId) {
        ViciontRecipe recipe = recipeRegistry.getRecipe(actionId);
        if (recipe == null) return;

        // GUI Overlay con sonidos de subguis cyi
        GuiBuilder builder = GuiBuilder.create("overlay_" + actionId)
                .openSound("minecraft:custom.gui.abrir_menus_cyi", 1.0f, 1.0f)
                .closeSound("minecraft:custom.gui.cerrar_menus_cyi", 1.0f, 1.0f)
                .background("viciontguis:textures/gui/fondo_crafteos.png", 340, 260)

                // Fondo invisible
                .element(GuiElementBuilder.invisibleButton("bg_close", "resume_recetas").position(0, 0).size(2000, 2000)
                        .clickSound("minecraft:custom.gui.cerrar_menus_cyi", 1.0f, 1.0f))

                // Botón Atrás
                .element(GuiElementBuilder.button("btn_overlay_back", "viciontguis:textures/gui/flecha_menu_anterior.png", "resume_recetas")
                        .position(-126, -92).size(35, 36).tooltipLine("Atrás", "#FF5555", false)
                        .hoverSound("minecraft:custom.gui.pasar_cursor", 2.0f, 1.0f)
                        .clickSound("minecraft:custom.gui.cerrar_menus_cyi", 1.0f, 1.0f))

                .element(GuiElementBuilder.text("titulo", recipe.getTitle(), "#FFFFFF", 1.6f, true).position(0, -100));

        String frameInput = "viciontguis:textures/gui/marco_item.png";
        String frameOutput = "viciontguis:textures/gui/marco_recetas.png";
        String[] shape = recipe.getShape();

        int offsetY = -10;
        int slotSize = 28;

        switch (recipe.getType()) {
            case 1 -> {
                int[] xs = {-88, -52, -16};
                int[] ys = {-10 + offsetY, 26 + offsetY, 62 + offsetY};

                for (int r = 0; r < 3; r++) {
                    for (int c = 0; c < 3; c++) {
                        char key = (r < shape.length && c < shape[r].length()) ? shape[r].charAt(c) : ' ';
                        ViciontItem ingredient = recipe.getIngredients().get(key);

                        if (key != ' ' && ingredient != null) {
                            builder.element(buildItemElement("i_" + r + c, ingredient, frameInput, xs[c], ys[r], slotSize));
                        } else {
                            builder.element(GuiElementBuilder.image("empty_" + r + c, frameInput).position(xs[c], ys[r]).size(slotSize, slotSize));
                        }
                    }
                }
                builder.element(GuiElementBuilder.image("arrow", "viciontguis:textures/gui/flecha_der.png").position(32, 26 + offsetY).size(24, 24));
                builder.element(buildItemElement("res", recipe.getResult(), frameOutput, 84, 26 + offsetY, 36));
            }
            case 2 -> {
                char keyI = (shape.length > 0 && shape[0].length() > 0) ? shape[0].charAt(0) : ' ';
                char keyF = (shape.length > 0 && shape[0].length() > 1) ? shape[0].charAt(1) : ' ';

                ViciontItem input = recipe.getIngredients().get(keyI);
                ViciontItem fuel = recipe.getIngredients().get(keyF);

                if (input != null) builder.element(buildItemElement("input", input, frameInput, -60, -16 + offsetY, slotSize));
                else builder.element(GuiElementBuilder.image("empty_in", frameInput).position(-60, -16 + offsetY).size(slotSize, slotSize));

                if (fuel != null) builder.element(buildItemElement("fuel", fuel, frameInput, -60, 32 + offsetY, slotSize));
                else builder.element(GuiElementBuilder.image("empty_fuel", frameInput).position(-60, 32 + offsetY).size(slotSize, slotSize));

                builder.element(GuiElementBuilder.image("arrow", "viciontguis:textures/gui/flecha_der.png").position(0, 8 + offsetY).size(24, 24));
                builder.element(buildItemElement("res", recipe.getResult(), frameOutput, 60, 8 + offsetY, 36));
            }
            case 3 -> {
                char keyT = (shape.length > 0 && shape[0].length() > 0) ? shape[0].charAt(0) : ' ';
                char keyA = (shape.length > 0 && shape[0].length() > 1) ? shape[0].charAt(1) : ' ';
                char keyI = (shape.length > 0 && shape[0].length() > 2) ? shape[0].charAt(2) : ' ';

                ViciontItem template = recipe.getIngredients().get(keyT);
                ViciontItem armor = recipe.getIngredients().get(keyA);
                ViciontItem ingot = recipe.getIngredients().get(keyI);

                if (template != null) builder.element(buildItemElement("temp", template, frameInput, -90, 8 + offsetY, slotSize));
                else builder.element(GuiElementBuilder.image("empty_temp", frameInput).position(-90, 8 + offsetY).size(slotSize, slotSize));

                if (armor != null) builder.element(buildItemElement("armor", armor, frameInput, -50, 8 + offsetY, slotSize));
                else builder.element(GuiElementBuilder.image("empty_armor", frameInput).position(-50, 8 + offsetY).size(slotSize, slotSize));

                if (ingot != null) builder.element(buildItemElement("ingot", ingot, frameInput, -10, 8 + offsetY, slotSize));
                else builder.element(GuiElementBuilder.image("empty_ingot", frameInput).position(-10, 8 + offsetY).size(slotSize, slotSize));

                builder.element(GuiElementBuilder.image("arrow", "viciontguis:textures/gui/flecha_der.png").position(40, 8 + offsetY).size(24, 24));
                builder.element(buildItemElement("res", recipe.getResult(), frameOutput, 90, 8 + offsetY, 36));
            }
            case 4 -> {
                char keyA = (shape.length > 0 && shape[0].length() > 0) ? shape[0].charAt(0) : ' ';
                char keyB = (shape.length > 0 && shape[0].length() > 1) ? shape[0].charAt(1) : ' ';
                char keyC = (shape.length > 0 && shape[0].length() > 2) ? shape[0].charAt(2) : ' ';
                char keyD = (shape.length > 0 && shape[0].length() > 3) ? shape[0].charAt(3) : ' ';

                ViciontItem slot1 = recipe.getIngredients().get(keyA);
                ViciontItem slot2 = recipe.getIngredients().get(keyB);
                ViciontItem slot3 = recipe.getIngredients().get(keyC);
                ViciontItem slot4 = recipe.getIngredients().get(keyD);

                if (slot1 != null) builder.element(buildItemElement("s1", slot1, frameInput, -120, 8 + offsetY, slotSize));
                else builder.element(GuiElementBuilder.image("empty_s1", frameInput).position(-120, 8 + offsetY).size(slotSize, slotSize));

                if (slot2 != null) builder.element(buildItemElement("s2", slot2, frameInput, -80, 8 + offsetY, slotSize));
                else builder.element(GuiElementBuilder.image("empty_s2", frameInput).position(-80, 8 + offsetY).size(slotSize, slotSize));

                if (slot3 != null) builder.element(buildItemElement("s3", slot3, frameInput, -40, 8 + offsetY, slotSize));
                else builder.element(GuiElementBuilder.image("empty_s3", frameInput).position(-40, 8 + offsetY).size(slotSize, slotSize));

                if (slot4 != null) builder.element(buildItemElement("s4", slot4, frameInput, 0, 8 + offsetY, slotSize));
                else builder.element(GuiElementBuilder.image("empty_s4", frameInput).position(0, 8 + offsetY).size(slotSize, slotSize));

                builder.element(GuiElementBuilder.image("arrow", "viciontguis:textures/gui/flecha_der.png").position(40, 8 + offsetY).size(24, 24));
                builder.element(buildItemElement("res", recipe.getResult(), frameOutput, 90, 8 + offsetY, 36));
            }
        }
        ViciontGuiAPI.openScreen(player, builder);
    }

    private GuiElementBuilder buildItemElement(String id, ViciontItem itemData, String frame, int x, int y, int size) {
        GuiElementBuilder element = GuiElementBuilder.itemSlot(id, frame, itemData.getMaterial())
                .position(x, y).size(size, size)
                .hoverSound("minecraft:custom.gui.pasar_cursor", 2.0f, 1.0f); // Hover sobre items

        if (itemData.getCustomModelData() > 0) {
            element.customModelData(itemData.getCustomModelData());
        }

        if (itemData.getAction() != null) {
            element.action(itemData.getAction());
        }

        for (ViciontItem.TooltipLine line : itemData.getTooltipLines()) {
            element.tooltipLine(line.text(), line.color(), line.bold());
        }
        return element;
    }
}