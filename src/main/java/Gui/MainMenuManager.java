package Gui;

import com.crissyjuanxd.viciontguiplugin.api.GuiBuilder;
import com.crissyjuanxd.viciontguiplugin.api.GuiElementBuilder;
import com.crissyjuanxd.viciontguiplugin.api.GuiTarget;
import com.crissyjuanxd.viciontguiplugin.api.ViciontGuiAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import vct.hardcore3.ViciontHardcore3;

public class MainMenuManager implements Listener {

    private final ViciontHardcore3 plugin;

    public MainMenuManager(ViciontHardcore3 plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        // Si se abre desde 'G' o Inventario
        ViciontGuiAPI.onAction("open_main", (player, guiId, action) -> openMainMenu(player, true));
        // Si se abre desde una flecha de atrás de un submenú
        ViciontGuiAPI.onAction("return_main", (player, guiId, action) -> openMainMenu(player, false));
    }

    private void openMainMenu(Player player, boolean playOpenSound) {
        GuiBuilder builder = GuiBuilder.create("menu_principal")
                .closeSound("minecraft:custom.gui.cerrar_menu", 1.0f, 1.0f);

        if (playOpenSound) {
            builder.openSound("minecraft:custom.gui.abrir_menu", 1.0f, 1.0f);
        }

        builder.element(GuiElementBuilder.image("centro_logo", "viciontguis:textures/gui/center.png")
                        .position(0, 0).size(120, 120))
                .element(GuiElementBuilder.button("btn_misiones", "viciontguis:textures/gui/misiones.png", "open_misiones")
                        .position(0, -110).size(64, 64).tooltipLine("Misiones", "#B263F9", false)
                        .hoverSound("minecraft:custom.gui.pasar_cursor", 2.0f, 1.0f)
                        .clickSound("minecraft:custom.gui.presionar_boton", 1.0f, 2.0f))
                .element(GuiElementBuilder.button("btn_entidades", "viciontguis:textures/gui/entidades.png", "open_entidades")
                        .position(0, 110).size(64, 64).tooltipLine("Entidades", "#ED1C24", false)
                        .hoverSound("minecraft:custom.gui.pasar_cursor", 2.0f, 1.0f)
                        .clickSound("minecraft:custom.gui.presionar_boton", 1.0f, 2.0f))
                .element(GuiElementBuilder.button("btn_cambios", "viciontguis:textures/gui/cambios.png", "open_cambios")
                        .position(-110, 0).size(64, 64).tooltipLine("Cambios", "#FFC90E", false)
                        .hoverSound("minecraft:custom.gui.pasar_cursor", 2.0f, 1.0f)
                        .clickSound("minecraft:custom.gui.presionar_boton", 1.0f, 2.0f))
                .element(GuiElementBuilder.button("btn_recetas", "viciontguis:textures/gui/recetas.png", "open_recetas")
                        .position(110, 0).size(64, 64).tooltipLine("Recetas", "#4EF27E", false)
                        .hoverSound("minecraft:custom.gui.pasar_cursor", 2.0f, 1.0f)
                        .clickSound("minecraft:custom.gui.presionar_boton", 1.0f, 2.0f))
                .element(GuiElementBuilder.button("btn_fiesta_logros", "viciontguis:textures/gui/fiestalogros.png", null)
                        .position(-80, 90).size(50, 50).tooltipLine("Fiesta de Logros", "#FFAEC9", false)
                        .hoverSound("minecraft:custom.gui.pasar_cursor", 2.0f, 1.0f)
                        .clickSound("minecraft:custom.gui.presionar_boton", 1.0f, 2.0f))
                .element(GuiElementBuilder.button("btn_fiesta_crafteos", "viciontguis:textures/gui/fiestacrafteos.png", null)
                        .position(80, 90).size(50, 50).tooltipLine("Fiesta de Crafteos", "#B97A57", false)
                        .hoverSound("minecraft:custom.gui.pasar_cursor", 2.0f, 1.0f)
                        .clickSound("minecraft:custom.gui.presionar_boton", 1.0f, 2.0f));

        ViciontGuiAPI.openScreen(player, builder);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;

            GuiBuilder inv = GuiBuilder.create("inv_test").target(GuiTarget.INVENTORY)
                    .element(GuiElementBuilder.button("btn_center", "viciontguis:textures/gui/menu_inv.png", "open_main")
                            .anchor("center").position(55, -12).size(20, 20)
                            .tooltipLine("Viciont Menú", "#7511CD", false)
                            .hoverSound("minecraft:custom.gui.pasar_cursor", 2.0f, 1.0f)
                            .clickSound("minecraft:custom.gui.presionar_boton", 1.0f, 2.0f));

            ViciontGuiAPI.setOverlay(player, inv);

        }, 80L);
    }
}