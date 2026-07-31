package Gui;

import Handlers.DatabaseManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CambiosDataManager {
    private final JavaPlugin plugin;
    private final DatabaseManager dbManager;

    private final List<CambioEntry> entradas = new ArrayList<>();
    private final Map<UUID, Set<String>> leidosPorJugador = new ConcurrentHashMap<>();

    public CambiosDataManager(JavaPlugin plugin, DatabaseManager dbManager) {
        this.plugin = plugin;
        this.dbManager = dbManager;
        loadData();
    }

    public void loadData() {
        entradas.clear();
        entradas.addAll(dbManager.loadAllGuiCambios());
    }

    public void addCambio(String tipo, String rawMessage, String jsonMessage) {
        String colorCode = switch (tipo.toLowerCase()) {
            case "cambio" -> "#F0CC90";
            case "estructura" -> "#A175D6";
            case "anuncio" -> "#9FF0C8";
            case "evento" -> "#AE78C6";
            default -> "#FFFFFF";
        };

        String tipoCapitalizado = tipo.substring(0, 1).toUpperCase() + tipo.substring(1).toLowerCase();

        // Calcular el número automáticamente basándose en los existentes
        int numero = entradas.stream()
                .filter(e -> e.tipo.equalsIgnoreCase(tipoCapitalizado))
                .mapToInt(e -> e.numero)
                .max().orElse(0) + 1;

        String id = tipoCapitalizado + "_" + numero;
        CambioEntry entry = new CambioEntry(id, tipoCapitalizado, numero, colorCode, rawMessage, jsonMessage);

        entradas.add(entry);

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> dbManager.saveGuiCambio(entry));
    }

    public void editCambio(String id, String newRawMessage, String newJsonMessage) {
        for (CambioEntry entry : entradas) {
            if (entry.id.equalsIgnoreCase(id)) {
                entry.rawMessage = newRawMessage;
                entry.jsonMessage = newJsonMessage;
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> dbManager.saveGuiCambio(entry));
                break;
            }
        }
    }

    public boolean deleteCambio(String id) {
        boolean removed = entradas.removeIf(e -> e.id.equalsIgnoreCase(id));
        if (removed) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> dbManager.deleteGuiCambio(id));
        }
        return removed;
    }

    // Manejo de caché para jugadores (Llamado al entrar y salir del server)
    public void loadPlayer(UUID uuid) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Set<String> leidos = dbManager.loadCambiosLeidos(uuid);
            leidosPorJugador.put(uuid, leidos);
        });
    }

    public void unloadPlayer(UUID uuid) {
        leidosPorJugador.remove(uuid);
    }

    public List<CambioEntry> getEntradas() { return entradas; }

    public boolean isLeido(Player player, String id) {
        return leidosPorJugador.getOrDefault(player.getUniqueId(), new HashSet<>()).contains(id);
    }

    public void setLeido(Player player, String id, boolean leido) {
        Set<String> leidos = leidosPorJugador.computeIfAbsent(player.getUniqueId(), k -> new HashSet<>());
        if (leido) leidos.add(id); else leidos.remove(id);

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            dbManager.setCambioLeido(player.getUniqueId(), id, leido);
        });
    }

    public static class CambioEntry {
        public String id;
        public String tipo;
        public int numero;
        public String colorCode;
        public String rawMessage;
        public String jsonMessage;

        public CambioEntry(String id, String tipo, int numero, String colorCode, String rawMessage, String jsonMessage) {
            this.id = id; this.tipo = tipo; this.numero = numero; this.colorCode = colorCode;
            this.rawMessage = rawMessage; this.jsonMessage = jsonMessage;
        }
        public String getTitulo() { return tipo + " #" + numero; }
    }
}