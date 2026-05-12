package list;

import Handlers.Teams.TeamType;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.*;

public class VHList extends BukkitRunnable {

    private final JavaPlugin plugin;
    private int messageIndex = 0;

    // Header estático (Estética de Viciont)
    private static final String HEADER = ChatColor.DARK_GRAY + "●" + ChatColor.GRAY + ChatColor.BOLD + "" + ChatColor.STRIKETHROUGH + "                 " + ChatColor.DARK_GRAY + "●" +
            ChatColor.LIGHT_PURPLE + ChatColor.BOLD + ChatColor.STRIKETHROUGH + "                 " + ChatColor.DARK_GRAY + "●" +
            ChatColor.GRAY + ChatColor.BOLD + ChatColor.STRIKETHROUGH + "                 " + ChatColor.DARK_GRAY + "●\n" +
            ChatColor.GRAY + " \n" +
            ChatColor.WHITE + "" + ChatColor.BOLD + "        \uE073        \n" +
            ChatColor.GRAY + " \n" +
            ChatColor.GRAY + " \n";

    // Base del Footer (Estética de Viciont)
    private static final String FOOTER_BOTTOM = ChatColor.GRAY + " \n" +
            ChatColor.DARK_GRAY + "" + ChatColor.BOLD + "●" + ChatColor.GRAY + ChatColor.BOLD + "" + ChatColor.STRIKETHROUGH + "           " + ChatColor.DARK_GRAY + "●" +
            ChatColor.LIGHT_PURPLE + ChatColor.BOLD + "" + ChatColor.STRIKETHROUGH + "           " + ChatColor.DARK_GRAY + "●" +
            ChatColor.GRAY + ChatColor.BOLD + "" + ChatColor.STRIKETHROUGH + "     " +
            ChatColor.DARK_PURPLE + ChatColor.BOLD + "∨" + ChatColor.GRAY + ChatColor.BOLD + "" + ChatColor.STRIKETHROUGH + "      " + ChatColor.DARK_GRAY + "●" +
            ChatColor.LIGHT_PURPLE + ChatColor.BOLD + "" + ChatColor.STRIKETHROUGH + "           " + ChatColor.DARK_GRAY + "●" +
            ChatColor.GRAY + ChatColor.BOLD + "" + ChatColor.STRIKETHROUGH + "           " +
            ChatColor.DARK_GRAY + ChatColor.BOLD + "●";

    // Mensajes rotativos
    private final String[] footerMessages = {
            ChatColor.WHITE + "" + ChatColor.BOLD + "Owner/Creator: " + ChatColor.DARK_AQUA + "CrissyjuanxD",
            ChatColor.WHITE + "" + ChatColor.BOLD + "Builds: " + ChatColor.BLUE + "Pepepoi",
            ChatColor.WHITE + "" + ChatColor.BOLD + "Helper: " + ChatColor.YELLOW + "Rompope05",
            ChatColor.WHITE + "" + ChatColor.BOLD + "Lore: " + ChatColor.AQUA + "Torath"
    };

    public VHList(JavaPlugin plugin) {
        this.plugin = plugin;
        // Cambiar el mensaje cada 10 segundos (200 ticks)
        new BukkitRunnable() {
            @Override
            public void run() {
                messageIndex = (messageIndex + 1) % footerMessages.length;
            }
        }.runTaskTimer(plugin, 0L, 200L);
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            updateTablistForPlayer(player);
            updateHealthScoreboard(player);
        }
    }

    public void updateTablistForPlayer(Player player) {
        String currentFooterMessage = ChatColor.GRAY + " \n" + footerMessages[messageIndex] + " \n";
        String footer = currentFooterMessage + FOOTER_BOTTOM;

        player.setPlayerListHeaderFooter(HEADER, footer);

        Scoreboard scoreboard = player.getScoreboard();
        Team team = scoreboard.getEntryTeam(player.getName());

        String tabPrefix = "";
        String colorHex = ChatColor.WHITE.toString();
        String suffix = "";

        if (team != null) {
            suffix = team.getSuffix() != null ? team.getSuffix() : "";
            TeamType type = TeamType.getById(team.getName());

            if (type != null) {
                tabPrefix = type.getTabPrefix();
                colorHex = type.getBungeeColor().toString();
            } else {
                tabPrefix = team.getPrefix() != null ? team.getPrefix() : "";
            }
        }

        String coloredName = ChatColor.WHITE + tabPrefix + colorHex + player.getName() + suffix + " ";

        String currentName = player.getPlayerListName();
        if (currentName == null || !currentName.equals(coloredName)) {
            player.setPlayerListName(coloredName);
        }
    }

    public void updateHealthScoreboard(Player player) {
        Scoreboard scoreboard = player.getScoreboard();

        Objective healthObjective = scoreboard.getObjective("Healthvct");
        if (healthObjective == null) {
            healthObjective = scoreboard.registerNewObjective("Healthvct", "health",
                    ChatColor.DARK_PURPLE + "❤ Vida", RenderType.HEARTS);
            healthObjective.setDisplaySlot(DisplaySlot.PLAYER_LIST);
        }
    }
}