package list;

import Handlers.DayHandler;
import Handlers.Teams.TeamType;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.*;

public class VHList extends BukkitRunnable {

    private final JavaPlugin plugin;
    private final DayHandler dayHandler;
    private int messageIndex = 0;

    // ─── Paleta morada/rosa pastel ───────────────────────────────────────────
    private static final String COL_SEP1      = ChatColor.of("#c084fc").toString();
    private static final String COL_SEP2      = ChatColor.of("#f0abfc").toString();
    private static final String COL_DOT       = ChatColor.of("#581c87").toString();
    private static final String COL_DAY_LABEL = ChatColor.of("#7e22ce").toString();
    private static final String COL_DAY_NUM   = ChatColor.of("#e879f9").toString();
    private static final String COL_ONLINE_LB = ChatColor.of("#c084fc").toString();
    private static final String COL_PING_LB   = ChatColor.of("#f0abfc").toString();
    private static final String COL_NUMBERS   = ChatColor.of("#f5d0fe").toString();
    private static final String COL_WHITE     = ChatColor.WHITE.toString();
    private static final String COL_GRAY      = ChatColor.GRAY.toString();

    // ─── Colores de vida ─────────────────────────────────────────────────────
    private static final String COL_HP_HIGH = ChatColor.of("#c084fc").toString(); // morado  > 12
    private static final String COL_HP_MID  = ChatColor.of("#fb923c").toString(); // naranja  6-12
    private static final String COL_HP_LOW  = ChatColor.of("#ef4444").toString(); // rojo    <= 6

    private static String separator() {
        return COL_DOT + ChatColor.BOLD + "●" +
                COL_SEP1 + ChatColor.BOLD + "" + ChatColor.STRIKETHROUGH + "                 " + COL_DOT + "●" +
                COL_SEP2 + ChatColor.BOLD + "" + ChatColor.STRIKETHROUGH + "                 " + COL_DOT + "●" +
                COL_SEP1 + ChatColor.BOLD + "" + ChatColor.STRIKETHROUGH + "                 " + COL_DOT + "●\n";
    }

    private static final String HEADER_STATIC =
            COL_GRAY + " \n" +
                    ChatColor.WHITE + "" + ChatColor.BOLD + "        \uE073        \n" +
                    COL_GRAY + " \n" +
                    COL_GRAY + " \n" +
                    COL_GRAY + " \n";

    private final String[] footerMessages = {
            COL_WHITE + ChatColor.BOLD + "Owner/Dev: "  + COL_SEP2    + "CrissyjuanxD",
            COL_WHITE + ChatColor.BOLD + "Builds: "     + COL_SEP1    + "Pepepoi",
            COL_WHITE + ChatColor.BOLD + "Helper: "     + COL_DAY_NUM + "Rompope05",
            COL_WHITE + ChatColor.BOLD + "Lore: "       + COL_SEP2    + "Torath"
    };

    public VHList(JavaPlugin plugin, DayHandler dayHandler) {
        this.plugin     = plugin;
        this.dayHandler = dayHandler;

        this.runTaskTimer(plugin, 0L, 20L);

        new BukkitRunnable() {
            @Override public void run() {
                messageIndex = (messageIndex + 1) % footerMessages.length;
            }
        }.runTaskTimer(plugin, 0L, 200L);
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            updateTablistForPlayer(player);
            updatePlayerName(player);
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    //  HEADER + FOOTER
    // ────────────────────────────────────────────────────────────────────────
    public void updateTablistForPlayer(Player player) {

        int online = Bukkit.getOnlinePlayers().size();
        int ping   = player.getPing();
        int day    = dayHandler.getCurrentDay();

        // ── Header ──────────────────────────────────────────────────────────
        String header = separator() +
                HEADER_STATIC +
                COL_ONLINE_LB + "📊 Online: " + COL_NUMBERS + online +
                COL_GRAY      + "   |   " +
                COL_PING_LB   + "📶 Ping: " + COL_NUMBERS + ping + " ms\n";

        // ── Footer: créditos → día en línea con separador → separador inf ──
// ── Footer ──
        String footer =
                COL_GRAY + " \n" +
                        footerMessages[messageIndex] +
                        COL_GRAY + " | " +
                        COL_DAY_LABEL + ChatColor.BOLD + "Día: " +
                        COL_DAY_NUM   + ChatColor.BOLD + day + "\n" +
                        COL_GRAY + " \n" +
                        // Separador inferior con ∨ original de Viciont
                        COL_DOT  + ChatColor.BOLD + "●" +
                        COL_SEP1 + ChatColor.BOLD + "" + ChatColor.STRIKETHROUGH + "           " + COL_DOT + "●" +
                        COL_SEP2 + ChatColor.BOLD + "" + ChatColor.STRIKETHROUGH + "           " + COL_DOT + "●" +
                        COL_SEP1 + ChatColor.BOLD + "" + ChatColor.STRIKETHROUGH + "     " +
                        COL_DAY_LABEL + ChatColor.BOLD + "∨" +
                        COL_SEP1 + ChatColor.BOLD + "" + ChatColor.STRIKETHROUGH + "      " + COL_DOT + "●" +
                        COL_SEP2 + ChatColor.BOLD + "" + ChatColor.STRIKETHROUGH + "           " + COL_DOT + "●" +
                        COL_SEP1 + ChatColor.BOLD + "" + ChatColor.STRIKETHROUGH + "           " +
                        COL_DOT  + ChatColor.BOLD + "●";

        player.setPlayerListHeaderFooter(header, footer);
    }

    // ────────────────────────────────────────────────────────────────────────
    //  NOMBRE + VIDA MANUAL ALINEADA (sin scoreboard)
    // ────────────────────────────────────────────────────────────────────────
    public void updatePlayerName(Player player) {

        // ── Prefijo de rango ─────────────────────────────────────────────────
        Scoreboard mainScoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        Team team = mainScoreboard.getEntryTeam(player.getName());

        String tabPrefix = "";
        String colorHex  = COL_GRAY;
        String suffix    = "";

        if (team != null) {
            suffix = team.getSuffix() != null ? team.getSuffix() : "";
            TeamType type = TeamType.getById(team.getName());
            if (type != null) {
                tabPrefix = type.getTabPrefix();
                colorHex  = type.getBungeeColor().toString();
            } else {
                tabPrefix = team.getPrefix() != null ? team.getPrefix() : "";
            }
        }

        // ── Vida + absorción ──────────────────────────────────────────────────
        int hp         = (int) Math.ceil(player.getHealth());
        int absorption = (int) Math.ceil(player.getAbsorptionAmount());
        int totalHp    = hp + absorption;

        // Símbolo y color según vida base
        String heartSymbol;
        String heartColor;
        if (hp > 12) {
            heartSymbol = "💜";
            heartColor  = COL_HP_HIGH;
        } else if (hp > 6) {
            heartSymbol = "🧡";
            heartColor  = COL_HP_MID;
        } else {
            heartSymbol = "❤";
            heartColor  = COL_HP_LOW;
        }

        // ── Nombre con padding fijo para alinear el corazón ──────────────────
        // El nombre ocupa máximo 16 chars en MC, rellenamos con espacios
        String rawName   = tabPrefix + player.getName() + suffix;
        // Contamos solo caracteres visibles (sin códigos de color) para el padding
        String cleanName = ChatColor.stripColor(rawName);
        int    padNeeded = Math.max(0, 17 - cleanName.length());
        String padding   = " ".repeat(padNeeded);

        // Formato final: [rango]nombre[padding][corazón][número]
        String coloredName =
                COL_WHITE + tabPrefix +
                        colorHex  + player.getName() + suffix +
                        padding   +
                        heartColor + heartSymbol + totalHp;

        if (!coloredName.equals(player.getPlayerListName())) {
            player.setPlayerListName(coloredName);
        }

        // Limpia cualquier scoreboard de vida/ping que haya quedado
        Scoreboard scoreboard = player.getScoreboard();
        Objective oldHealth = scoreboard.getObjective("tabHealth");
        if (oldHealth != null) oldHealth.unregister();
        Objective oldPing = scoreboard.getObjective("tabPing");
        if (oldPing != null) oldPing.unregister();
    }
}