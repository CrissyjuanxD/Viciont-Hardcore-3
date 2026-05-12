package chat;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
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
        Player player = event.getPlayer();
        Team team = player.getScoreboard().getEntryTeam(player.getName());

        String finalPrefix = "";
        String suffix = "";
        ChatColor nameColor = ChatColor.WHITE;

        if (team != null) {
            finalPrefix = team.getPrefix();
            suffix = team.getSuffix();

            try {
                Handlers.Teams.TeamType type = Handlers.Teams.TeamType.getById(team.getName());
                if (type != null) {
                    nameColor = type.getBungeeColor();
                }
            } catch (Exception ignored) {}
        } else {
            finalPrefix = ChatColor.GRAY + "";
        }

        event.setFormat(finalPrefix + nameColor + "%1$s" + ChatColor.RESET + suffix + ChatColor.WHITE + ": %2$s");
    }

    private ChatColor getTeamColor(Team team) {
        if (team == null) {
            return ChatColor.DARK_AQUA;
        }
        String teamName = team.getName();

        switch (teamName) {
            case "Admin":
                return ChatColor.of("#ff935f");
            case "Mod":
                return ChatColor.of("#00BFFF");
            case "Helper":
                return ChatColor.of("#67E590");
            case "TSurvivor":
                return ChatColor.of("#9455ED");
            case "ZMiembro":
                return ChatColor.of("#ffa39d");
            case "ZFantasma":
                return ChatColor.of("#555555");
            default:
                return ChatColor.RESET;
        }
    }

    private String getTeamPrefix(Team team) {
        if (team == null) {
            return "";
        }
        String teamName = team.getName();
        switch (teamName) {
            case "Admin":
                return ChatColor.GRAY + "" + ChatColor.BOLD + "[" + ChatColor.GOLD + ChatColor.BOLD + "HOKAGE" + ChatColor.GRAY + ChatColor.BOLD + "]";
            case "Mod":
                return ChatColor.GRAY + "" + ChatColor.BOLD + "[" + ChatColor.AQUA + ChatColor.BOLD + "ANBU" + ChatColor.GRAY + ChatColor.BOLD + "]";
            case "Helper":
                return "\uEB89";
            case "TSurvivor":
                return "\uEB8A";
            case "ZMiembro":
                return ChatColor.GRAY + "" + ChatColor.BOLD + "[" + ChatColor.of("#ffa39d") + ChatColor.BOLD + "ALDEANO" + ChatColor.GRAY + ChatColor.BOLD + "]";
            case "ZFantasma":
                return "\uEB8C";
            default:
                return "";
        }
    }

    // Mensajes de Moderacion
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
