package chat;

import org.bukkit.Bukkit;
import net.md_5.bungee.api.ChatColor;
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
        Player player = event.getPlayer();
        Scoreboard scoreboard = player.getScoreboard();
        Team team = scoreboard.getEntryTeam(player.getName());

        // Prefijos y sufijos del equipo
        String prefix = (team != null) ? team.getPrefix() : "";
        String suffix = (team != null) ? team.getSuffix() : "";

        // Color del nombre basado en el equipo
        ChatColor playerNameColor = getTeamColor(team);

        // Formatear el mensaje con colores correctos
        String formattedMessage = prefix + playerNameColor + player.getName() + ChatColor.RESET + suffix + ChatColor.WHITE + ": " + event.getMessage();

        event.setFormat(formattedMessage);
    }

    // Método para obtener el color del equipo
    private ChatColor getTeamColor(Team team) {
        if (team == null) {
            return ChatColor.WHITE; // Si el jugador no tiene equipo, color blanco
        }
        String teamName = team.getName();

            // Personalizar colores según el nombre del equipo
            switch (teamName) {
                case "Admin":
                    return ChatColor.of("#F6763A");
                case "Mod":
                    return ChatColor.of("#00BFFF");
                case "Helper":
                    return ChatColor.of("#67E590");
                case "TSurvivor":
                    return ChatColor.of("#9455ED");
                case "Survivor+":
                    return ChatColor.of("#F4C657");
                case "ZFantasma":
                    return ChatColor.of("#555555");
                default:
                    return ChatColor.RESET; // Color por defecto si no hay un equipo registrado
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
