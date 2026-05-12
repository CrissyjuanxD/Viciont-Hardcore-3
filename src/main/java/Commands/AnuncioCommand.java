package Commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AnuncioCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // 1. Permisos
        if (!sender.hasPermission("ismanu.admin")) {
            sender.sendMessage(ChatColor.RED + "No tienes permiso para usar esto.");
            return true;
        }

        // 2. Verificar si escribió un mensaje
        if (args.length == 0) {
            sender.sendMessage(ChatColor.RED + "Uso correcto: /anuncio <mensaje>");
            return true;
        }

        // 3. Unir los argumentos en una sola frase
        String mensaje = String.join(" ", args);

        // Escapar comillas dobles para no romper el JSON si el mensaje las contiene
        mensaje = mensaje.replace("\"", "\\\"");

        // 4. Construir el JSON exacto que pediste
        // Nota: Las barras invertidas (\) deben escaparse como (\\) dentro de los Strings de Java.
        String jsonMessage = "[\"\"," +
                "{\"text\":\"\\n\"}," +
                "{\"text\":\"\\u06de\",\"bold\":true,\"color\":\"#E95B1E\"}," +
                "{\"text\":\" Anuncio\",\"bold\":true,\"color\":\"#DB7C26\"}," +
                "{\"text\":\" \\u25ba\",\"bold\":true,\"color\":\"gray\"}," +
                "{\"text\":\"\\n\\n\"}," +
                "{\"text\":\"" + mensaje + "\",\"color\":\"#9AE47C\"}," +
                "{\"text\":\"\\n \"}" +
                "]";

        // 5. Ejecutar el comando tellraw desde la consola para todos (@a)
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "tellraw @a " + jsonMessage);

        // 6. Reproducir sonido a todos los jugadores (El "Pling" clásico de notificaciones)
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.3f);
        }

        return true;
    }
}