package vct.hardcore3;

import org.bukkit.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.block.Block;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.entity.Player;
import org.bukkit.inventory.SmithingInventory;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.World.Environment;
import org.bukkit.World;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
public class ViciontHardcore3 extends JavaPlugin implements Listener {

    public String Prefix = "&d&lViciont&5&lHardcore &5&l3&7➤ &f";
    public String Version = getDescription().getVersion();

    public void onEnable() {
        Bukkit.getConsoleSender().sendMessage(
                ChatColor.translateAlternateColorCodes('&', Prefix + "&aha sido habilitado!, &eVersion: " + Version));
        // Registra los eventos de esta clase
        Bukkit.getServer().getPluginManager().registerEvents(this, this);
        //this.getCommand("revive").setExecutor(new ReviveCommand(this));
    }
    public void onDisable() {
        Bukkit.getConsoleSender().sendMessage(
                ChatColor.translateAlternateColorCodes('&', Prefix + "&aha sido deshabilitado!, &eVersion: " + Version));
    }

}
