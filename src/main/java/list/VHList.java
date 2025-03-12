package list;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

public class VHList extends BukkitRunnable {

    private final JavaPlugin plugin;

    public VHList(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            updateTablistForPlayer(player);
        }
    }

    public void updateTablistForPlayer(Player player) {
        // (Cabecera y pie de página se mantienen igual)
        String header = ChatColor.DARK_GRAY + "●" + ChatColor.GRAY + ChatColor.BOLD + "" + ChatColor.STRIKETHROUGH + "                 " +
                ChatColor.LIGHT_PURPLE + ChatColor.BOLD + ChatColor.STRIKETHROUGH + "                 " +
                ChatColor.GRAY + ChatColor.BOLD + ChatColor.STRIKETHROUGH + "                 " + ChatColor.DARK_GRAY + "●\n" +
                ChatColor.GRAY + " \n" +
                ChatColor.WHITE + "" + ChatColor.BOLD + "        \uE073        \n" +
                ChatColor.GRAY + " \n" +
                ChatColor.GRAY + " \n";
        String footer = ChatColor.GRAY + " \n" +
                ChatColor.WHITE + "" + ChatColor.BOLD + "Creado por: " + ChatColor.DARK_AQUA + "CrissyjuanxD" + " \n" +
                ChatColor.GRAY + " \n" +
                ChatColor.DARK_GRAY + "" + ChatColor.BOLD + "●" + ChatColor.GRAY + ChatColor.BOLD + "" + ChatColor.STRIKETHROUGH + "           " +
                ChatColor.LIGHT_PURPLE + ChatColor.BOLD + "" + ChatColor.STRIKETHROUGH + "           " +
                ChatColor.GRAY + ChatColor.BOLD + "" + ChatColor.STRIKETHROUGH + "     " +
                ChatColor.DARK_PURPLE + ChatColor.BOLD + "∨" + ChatColor.GRAY + ChatColor.BOLD + "" + ChatColor.STRIKETHROUGH + "      " +
                ChatColor.LIGHT_PURPLE + ChatColor.BOLD + "" + ChatColor.STRIKETHROUGH + "           " +
                ChatColor.GRAY + ChatColor.BOLD + "" + ChatColor.STRIKETHROUGH + "           " +
                ChatColor.DARK_GRAY + ChatColor.BOLD + "●";
        player.setPlayerListHeaderFooter(header, footer);

        // Obtenemos datos del equipo
        Scoreboard scoreboard = player.getScoreboard();
        Team team = scoreboard.getEntryTeam(player.getName());
        String suffix = team != null ? team.getSuffix() : "";
        String unicode = getUnicodeForTeam(team);
        String color = getColorForTeam(team);

        // Construimos el nombre coloreado (puedes ajustarlo a tu gusto)
        String coloredName = ChatColor.WHITE + unicode + " " + color + player.getName() + suffix + " ";

        // Calculamos la versión sin colores para medir su ancho en píxeles
        String plainName = ChatColor.stripColor(coloredName);

        // Definimos el ancho en píxeles que queremos para el bloque del nombre
        // (Puedes ajustar este valor hasta que te quede alineado)
        int targetPixelWidth = 115;

        // Obtenemos el ancho actual del nombre
        int namePixelWidth = getPixelWidth(plainName);
        // Calculamos cuántos píxeles faltan
        int missingPixels = targetPixelWidth - namePixelWidth;

        // Ahora, agregamos espacios hasta acercarnos al ancho objetivo.
        // Cada espacio en la fuente por defecto suele medir aproximadamente 4 píxeles.
        int spacesToAdd = missingPixels / 4;
        StringBuilder padding = new StringBuilder();
        for (int i = 0; i < spacesToAdd; i++) {
            padding.append(" ");
        }

        // Obtenemos la vida del jugador (como entero)
        int vida = (int) player.getHealth();
        // Definimos un símbolo (por ejemplo, un corazón)
        String simbolo = ChatColor.RED + "❤";

        // Reconstruimos el string final con el nombre coloreado, el padding y la vida
        String finalTabName = coloredName + padding.toString() + " " + simbolo + " " + ChatColor.WHITE + vida;
        player.setPlayerListName(finalTabName);
    }

    public enum DefaultFontInfo {
        A('A', 5), a('a', 5),
        B('B', 5), b('b', 5),
        C('C', 5), c('c', 5),
        D('D', 5), d('d', 5),
        E('E', 5), e('e', 5),
        F('F', 5), f('f', 4),
        G('G', 5), g('g', 5),
        H('H', 5), h('h', 5),
        I('I', 3), i('i', 1),
        J('J', 5), j('j', 5),
        K('K', 5), k('k', 4),
        L('L', 5), l('l', 1),
        M('M', 5), m('m', 5),
        N('N', 5), n('n', 5),
        O('O', 5), o('o', 5),
        P('P', 5), p('p', 5),
        Q('Q', 5), q('q', 5),
        R('R', 5), r('r', 5),
        S('S', 5), s('s', 5),
        T('T', 5), t('t', 4),
        U('U', 5), u('u', 5),
        V('V', 5), v('v', 5),
        W('W', 5), w('w', 5),
        X('X', 5), x('x', 5),
        Y('Y', 5), y('y', 5),
        Z('Z', 5), z('z', 5),
        NUM_0('0', 5), NUM_1('1', 5),
        NUM_2('2', 5), NUM_3('3', 5),
        NUM_4('4', 5), NUM_5('5', 5),
        NUM_6('6', 5), NUM_7('7', 5),
        NUM_8('8', 5), NUM_9('9', 5),
        SPACE(' ', 3),
        EXCLAMATION('!', 1),
        DASH('-', 3),
        QUESTION('?', 5);


        private final char character;
        private final int length;

        DefaultFontInfo(char character, int length) {
            this.character = character;
            this.length = length;
        }

        public char getCharacter() {
            return character;
        }

        public int getLength() {
            return length;
        }

        public static int getWidth(char c) {
            for (DefaultFontInfo dfi : values()) {
                if (dfi.getCharacter() == c) {
                    return dfi.getLength();
                }
            }
            return 6; // Si no está en la lista, usamos 6 como aproximación
        }
    }

    public int getPixelWidth(String text) {
        int width = 0;
        for (char c : text.toCharArray()) {
            // Sumamos el ancho del carácter según DefaultFontInfo
            width += DefaultFontInfo.getWidth(c);
            // Agregamos 1 píxel extra como espacio entre caracteres
            width++;
        }
        return width;
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
                    return ""; // Sin Unicode para otros equipos
            }
        }
        return ""; // Sin Unicode si no hay equipo
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
                    return ChatColor.WHITE.toString(); // Color predeterminado
            }
        }
        return ChatColor.GRAY.toString(); // Color predeterminado si no hay equipo
    }
}
