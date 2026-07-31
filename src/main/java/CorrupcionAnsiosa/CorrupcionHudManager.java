package CorrupcionAnsiosa;

import com.crissyjuanxd.viciontguiplugin.api.GuiBuilder;
import com.crissyjuanxd.viciontguiplugin.api.GuiElementBuilder;
import com.crissyjuanxd.viciontguiplugin.api.GuiTarget;
import com.crissyjuanxd.viciontguiplugin.api.ViciontGuiAPI;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;

public class CorrupcionHudManager implements Listener {

    private final Plugin plugin;
    private final CorrupcionAnsiosaManager corruptionManager;

    public CorrupcionHudManager(Plugin plugin, CorrupcionAnsiosaManager corruptionManager) {
        this.plugin = plugin;
        this.corruptionManager = corruptionManager;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;

            double currentCorruption = corruptionManager.getCorruption(player);
            updateHud(player, currentCorruption);
        }, 70L);
    }

    public static void updateHud(Player player, double corruption) {
        String colorCode;
        if (corruption >= 90) colorCode = "#A777E9";
        else if (corruption >= 80) colorCode = "#8D22E3";
        else if (corruption >= 70) colorCode = "#E4AA2F";
        else if (corruption >= 60) colorCode = "#FA6208";
        else colorCode = "#D70D0F";

        JsonArray iconMsg = new JsonArray();
        iconMsg.add("");
        JsonObject icon = new JsonObject();
        icon.addProperty("text", "\uE049");
        icon.addProperty("color", "#FFFFFF");
        iconMsg.add(icon);

        JsonArray numMsg = new JsonArray();
        numMsg.add("");
        JsonObject num = new JsonObject();
        num.addProperty("text", String.format("%.0f", corruption));
        numMsg.add(num);

        JsonArray percMsg = new JsonArray();
        percMsg.add("");
        JsonObject perc = new JsonObject();
        perc.addProperty("text", "%");
        percMsg.add(perc);

        int numLength = String.format("%.0f", corruption).length();
        int percOffsetX = (numLength == 3) ? 142 : 135;

        GuiBuilder hud = GuiBuilder.create("hud_corrupcion").target(GuiTarget.HUD)
            /*    .element(GuiElementBuilder.richText("porcentaje_icono", iconMsg, 50, null, null)
                        .anchor("bottom_center")
                        .position(91, -14)
                        .scale(1.0f))*/

                .element(GuiElementBuilder.image("porcentaje_icono", "viciontguis:textures/gui/corrupcion_anciosa.png")
                        .anchor("bottom_center")
                        .position(106, -12)
                        .size(22, 22))

                .element(GuiElementBuilder.richText("porcentaje_texto", numMsg, 100, null, colorCode)
                        .anchor("bottom_center")
                        .position(119, -14)
                        .scale(1.2f)
                        .outline(true))

                .element(GuiElementBuilder.richText("porcentaje_simbolo", percMsg, 50, null, "#FFFFFF")
                        .anchor("bottom_center")
                        .position(percOffsetX, -14)
                        .scale(1.2f)
                        .outline(true));

        ViciontGuiAPI.setOverlay(player, hud);
    }
}