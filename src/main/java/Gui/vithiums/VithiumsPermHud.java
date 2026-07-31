package Gui.vithiums;

import com.crissyjuanxd.viciontguiplugin.api.GuiBuilder;
import com.crissyjuanxd.viciontguiplugin.api.GuiElementBuilder;
import com.crissyjuanxd.viciontguiplugin.api.GuiTarget;
import com.crissyjuanxd.viciontguiplugin.api.ViciontGuiAPI;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.bukkit.entity.Player;

public class VithiumsPermHud {

    public static void updateHud(Player player, int totalVithiums) {
        JsonArray numMsg = new JsonArray();
        numMsg.add("");
        JsonObject num = new JsonObject();
        num.addProperty("text", String.valueOf(totalVithiums));
        numMsg.add(num);

        GuiBuilder hud = GuiBuilder.create("hud_vithiums_perm").target(GuiTarget.HUD)

                .element(GuiElementBuilder.image("vithiums_icono", "viciontguis:textures/gui/vithiums.png")
                        .anchor("bottom_center")
                        .position(106, -33)
                        .size(22, 22))

                .element(GuiElementBuilder.richText("vithiums_texto", numMsg, 100, null, "#E6B3FF")
                        .anchor("bottom_center")
                        .position(119, -37)
                        .scale(1.2f)
                        .outline(true));

        ViciontGuiAPI.setOverlay(player, hud);
    }
}