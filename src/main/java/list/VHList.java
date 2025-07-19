package list;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.*;

public class VHList extends BukkitRunnable {

    private final JavaPlugin plugin;
    private int messageIndex = 0;
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
        String header = ChatColor.DARK_GRAY + "●" + ChatColor.GRAY + ChatColor.BOLD + "" + ChatColor.STRIKETHROUGH + "                 " + ChatColor.DARK_GRAY + "●" +
                ChatColor.LIGHT_PURPLE + ChatColor.BOLD + ChatColor.STRIKETHROUGH + "                 " + ChatColor.DARK_GRAY + "●" +
                ChatColor.GRAY + ChatColor.BOLD + ChatColor.STRIKETHROUGH + "                 " + ChatColor.DARK_GRAY + "●\n" +
                ChatColor.GRAY + " \n" +
                ChatColor.WHITE + "" + ChatColor.BOLD + "        \uE073        \n" +
                ChatColor.GRAY + " \n" +
                ChatColor.GRAY + " \n";
        String footer = ChatColor.GRAY + " \n" +
                footerMessages[messageIndex] + " \n" +
                ChatColor.GRAY + " \n" +
                ChatColor.DARK_GRAY + "" + ChatColor.BOLD + "●" + ChatColor.GRAY + ChatColor.BOLD + "" + ChatColor.STRIKETHROUGH + "           " + ChatColor.DARK_GRAY + "●" +
                ChatColor.LIGHT_PURPLE + ChatColor.BOLD + "" + ChatColor.STRIKETHROUGH + "           " + ChatColor.DARK_GRAY + "●" +
                ChatColor.GRAY + ChatColor.BOLD + "" + ChatColor.STRIKETHROUGH + "     " +
                ChatColor.DARK_PURPLE + ChatColor.BOLD + "∨" + ChatColor.GRAY + ChatColor.BOLD + "" + ChatColor.STRIKETHROUGH + "      " + ChatColor.DARK_GRAY + "●" +
                ChatColor.LIGHT_PURPLE + ChatColor.BOLD + "" + ChatColor.STRIKETHROUGH + "           " + ChatColor.DARK_GRAY + "●" +
                ChatColor.GRAY + ChatColor.BOLD + "" + ChatColor.STRIKETHROUGH + "           " +
                ChatColor.DARK_GRAY + ChatColor.BOLD + "●";
        player.setPlayerListHeaderFooter(header, footer);

        Scoreboard scoreboard = player.getScoreboard();
        Team team = scoreboard.getEntryTeam(player.getName());

        String suffix = team != null ? team.getSuffix() : "";
        String unicode = getUnicodeForTeam(team);
        String color = getColorForTeam(team);

        String coloredName = ChatColor.WHITE + unicode + " " + color + player.getName() + suffix + " ";
        player.setPlayerListName(coloredName);
    }

    public void updateHealthScoreboard(Player player) {
        Scoreboard scoreboard = player.getScoreboard();

        // Crear o obtener el objetivo de salud
        Objective healthObjective = scoreboard.getObjective("Healthvct");
        if (healthObjective == null) {
            healthObjective = scoreboard.registerNewObjective("Healthvct", "health",
                    ChatColor.DARK_PURPLE + "❤ Vida", RenderType.HEARTS);
            healthObjective.setDisplaySlot(DisplaySlot.PLAYER_LIST);
        }
    }


    // Método para obtener el Unicode según el equipo
    private String getUnicodeForTeam(Team team) {
        if (team != null) {
            String teamName = team.getName();
            switch (teamName) {
                case "Admin":
                    return "\uEB8D"; // Unicode para Admin
                case "Mod":
                    return "\uEB8E"; // Unicode para Mod
                case "Helper":
                    return "\uEB92"; // Unicode para Helper
                case "TSurvivor":
                    return "\uEB8F"; // Unicode para Survivor
                case "Survivor+":
                    return "\uEB90"; // Unicode para Survivor+
                case "ZFantasma":
                    return "\uEB91"; // Unicode para Fantasma
                default:
                    return "";
            }
        }
        return "";
    }

    private String getColorForTeam(Team team) {
        if (team != null) {
            String teamName = team.getName();
            switch (teamName) {
                case "Admin":
                    return ChatColor.of("#F6763A").toString(); // Color para Admin
                case "Mod":
                    return ChatColor.of("#00BFFF").toString(); // Color para Mod
                case "Helper":
                    return ChatColor.of("#67E590").toString(); // Color para Helper
                case "TSurvivor":
                    return ChatColor.of("#9455ED").toString(); // Color para Survivor
                case "Survivor+":
                    return ChatColor.of("#F4C657").toString(); // Color para Survivor+
                case "ZFantasma":
                    return ChatColor.of("#555555").toString(); // Color para Fantasma
                default:
                    return ChatColor.WHITE.toString();
            }
        }
        return ChatColor.GRAY.toString();
    }

}
