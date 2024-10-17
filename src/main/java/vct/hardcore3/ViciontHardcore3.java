package vct.hardcore3;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Objects;

public class ViciontHardcore3 extends JavaPlugin implements Listener {

    public String Prefix = "&d&lViciont&5&lHardcore &5&l3&7➤ &f";
    public String Version = getDescription().getVersion();

    public void onEnable() {
        Bukkit.getConsoleSender().sendMessage(
                ChatColor.translateAlternateColorCodes('&', Prefix + "&aha sido habilitado!, &eVersion: " + Version));
        // Registra los eventos de esta clase
        Bukkit.getServer().getPluginManager().registerEvents(this, this);

        Objects.requireNonNull(this.getCommand("revive")).setExecutor(new ReviveCommand(this));
        getServer().getPluginManager().registerEvents(new DoubleLifeTotemHandler(this), this);
        getLogger().info("DoubleLifeTotemHandler registered!");
    }

    public void onDisable() {
        Bukkit.getConsoleSender().sendMessage(
                ChatColor.translateAlternateColorCodes('&', Prefix + "&aha sido deshabilitado!, &eVersion: " + Version));
    }
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        String message = ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "∨ " + event.getPlayer().getName() + ChatColor.RESET + ChatColor.LIGHT_PURPLE + " ha entrado a Viciont Hardcore 3";
        event.setJoinMessage(message);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        String message = ChatColor.GRAY + "" + ChatColor.BOLD + "∨ " + event.getPlayer().getName() + ChatColor.RESET + ChatColor.GRAY + " ha salido de Viciont Hardcore 3";
        event.setQuitMessage(message);
    }

}
