package chat;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.Listener;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;

public class chatgeneral implements Listener {
    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        String message = event.getMessage();
        Player player = event.getPlayer();
        Scoreboard scoreboard = player.getScoreboard();
        Team team = scoreboard.getEntryTeam(player.getName());

        // Obtiene los prefijos y sufijos del equipo
        String prefix = team != null ? team.getPrefix() : "";
        String suffix = team != null ? team.getSuffix() : "";
        ChatColor teamColor = team != null ? team.getColor() : ChatColor.WHITE;

        // Formatea el mensaje para eliminar los "<>"
        String formattedMessage = ChatColor.translateAlternateColorCodes('&', prefix + teamColor + player.getName() + ChatColor.RESET + suffix + "&7 ➤ &f" + message);

        // Establece el formato del mensaje
        event.setFormat(formattedMessage);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {


        Player player = event.getPlayer();
        Block block = event.getClickedBlock();
        if (block != null && block.getType() == Material.CHEST && event.getAction() == Action.RIGHT_CLICK_BLOCK && !player.isSneaking()) {
            SimpleDateFormat formatter = new SimpleDateFormat("dd/MM, HH:mm:ss");
            Date date = new Date();
            Bukkit.getConsoleSender().sendMessage(ChatColor.DARK_PURPLE + (player.getName() + " HA ABIERTO UN COFRE, COORDENADAS " + block.getLocation() + ", FECHA: ").toUpperCase() + ChatColor.GOLD + formatter.format(date).toUpperCase());
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        if (block.getType() == Material.CHEST) {
            SimpleDateFormat formatter = new SimpleDateFormat("dd/MM, HH:mm:ss");
            Date date = new Date();
            Bukkit.getConsoleSender().sendMessage(ChatColor.DARK_PURPLE + (player.getName() + " HA ROTO UN COFRE, COORDENADAS " + block.getLocation() + ", FECHA: ").toUpperCase() + ChatColor.GOLD + formatter.format(date).toUpperCase());
        }
    }
    @EventHandler
    public void onInventoryCreative(InventoryCreativeEvent event) {
        Player player = (Player) event.getWhoClicked();
        Scoreboard scoreboard = Objects.requireNonNull(Bukkit.getScoreboardManager()).getMainScoreboard();
        Team team = scoreboard.getEntryTeam(player.getName());
        ItemStack item = event.getCurrentItem();
        if ((player.isOp() || (team != null && team.getName().equals("mod"))) && item != null && item.getType() != Material.AIR && event.getSlotType() == InventoryType.SlotType.QUICKBAR) {
            SimpleDateFormat formatter = new SimpleDateFormat("dd/MM, HH:mm:ss");
            Date date = new Date();
            Bukkit.getConsoleSender().sendMessage(ChatColor.DARK_PURPLE + (player.getName() + " HA SACADO " + item.getAmount() + " " + item.getType() + " DEL MODO CREATIVO, FECHA: ").toUpperCase() + ChatColor.YELLOW + formatter.format(date).toUpperCase());
        }
    }

}
