package Events.ItemParty;

import Commands.TiempoCommand;
import TitleListener.RuletaAnimation;
import items.ItemsPartyRecolect;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.NamedTextColor;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class ItemPartyHandler implements Listener {

    // ===== Colores (HEX en §x-RRGGBB) =====
    private static final String HEX_NUM_Y_WARN = "#ae52e3";
    private static final String HEX_TOP1_NAME  = "#e5d480";
    private static final String HEX_TOP2_6     = "#4aa5dc";
    private static final String HEX_WARN_NAME  = "#ee749e";
    private static final String GRIS = "§7";
    private static final String BLANCO = "§f";

    private final JavaPlugin plugin;
    private final TiempoCommand tiempoCommand;
    private final RuletaAnimation ruletaAnimation;
    private final ItemsPartyRecolect partyItems;
    private long lastScoreUpdate = 0L;

    private boolean eventoActivo = false;
    private String itemToCollect;        // caramelo | piruleta | algodon | soda
    private String collectionMethod;     // block | mob
    private String collectionTarget;     // dirt | zombie | corruptedzombie, etc.
    private String eventDuration;        // HH:MM:SS
    private int safePlayers;             // p.ej. -2 => 2 en peligro

    private final Map<String, Integer> playerItems = new LinkedHashMap<>();
    private final Map<String, Long> playerTimestamp = new HashMap<>();
    private final Set<String> participants = new HashSet<>();

    private File configFile, playersFile;
    private FileConfiguration config, playersConfig;

    // Ya NO se usa task periódico — solo actualizamos on-pickup / cambios
    private final Map<UUID, String> timerIdsByPlayer = new HashMap<>();
    private final Map<UUID, BossBar> puntosBars = new HashMap<>();

    // Tags del drop del evento (sin UID ⇒ pueden stackear)
    private final NamespacedKey KEY_ORIGIN;    // marca de “viene del evento”
    private final NamespacedKey KEY_COUNTED;   // 0 en el suelo, 1 al ser recogido (para no contar dos veces)

    public ItemPartyHandler(JavaPlugin plugin, TiempoCommand tiempoCommand) {
        this.plugin = plugin;
        this.tiempoCommand = tiempoCommand;
        this.ruletaAnimation = new RuletaAnimation(plugin);
        this.partyItems = new ItemsPartyRecolect(plugin);
        this.KEY_ORIGIN = new NamespacedKey(plugin, "itemparty_origin");
        this.KEY_COUNTED = new NamespacedKey(plugin, "itemparty_counted");
        loadConfig();
    }

    private void loadConfig() {
        File dir = new File(plugin.getDataFolder(), "itemparty");
        if (!dir.exists()) dir.mkdirs();

        configFile = new File(dir, "itempartyconfig.yml");
        playersFile = new File(dir, "itempartyplayers.yml");

        if (!configFile.exists()) plugin.saveResource("itemparty/itempartyconfig.yml", false);
        if (!playersFile.exists()) plugin.saveResource("itemparty/itempartyplayers.yml", false);

        config = YamlConfiguration.loadConfiguration(configFile);
        playersConfig = YamlConfiguration.loadConfiguration(playersFile);

        itemToCollect    = config.getString("item_to_collect", "caramelo").toLowerCase();
        collectionMethod = config.getString("collection_method", "block").toLowerCase();
        collectionTarget = config.getString("collection_target", "dirt").toLowerCase();
        eventDuration    = config.getString("event_duration", "00:10:00");
        safePlayers      = config.getInt("safe_players", -2);
    }

    public void reloadConfig() { loadConfig(); }

    // ===== Inicio / Fin =====
    public void iniciarEvento() {
        if (eventoActivo) { Bukkit.broadcastMessage("§c¡El evento ya está activo!"); return; }
        if (Bukkit.getOnlinePlayers().size() < 3) {
            Bukkit.broadcastMessage("§c¡Se necesitan al menos 3 jugadores para iniciar el evento!");
            return;
        }
        eventoActivo = true;
        playerItems.clear(); playerTimestamp.clear(); participants.clear();

        for (Player p : Bukkit.getOnlinePlayers()) {
            participants.add(p.getName());
            playerItems.put(p.getName(), 0);
            playerTimestamp.put(p.getName(), System.currentTimeMillis());
        }

        String targetText = collectionMethod.equals("block")
                ? "rompiendo §b§l" + getBlockDisplayName(collectionTarget)
                : "matando §b§l" + getMobDisplayName(collectionTarget);

        String jsonMessage =
                "[\"\","
                        + "{\"text\":\"\\n\"},"
                        + "{\"text\":\"\\u06de Evento\",\"bold\":true,\"color\":\"#F977F9\"},"
                        + "{\"text\":\" \\u27a4\",\"bold\":true,\"color\":\"gray\"},"
                        + "{\"text\":\"\\n\\n\"},"
                        + "{\"text\":\"¡Ha comenzado el evento \",\"color\":\"#c55cf3\"},"
                        + "{\"text\":\"FIESTA DE ITEMS\",\"bold\":true,\"color\":\"#ae52e3\"},"
                        + "{\"text\":\"!\\n\\n\",\"color\":\"#c55cf3\"},"
                        + "{\"text\":\"Recolecta ítems \",\"color\":\"#c55cf3\"},"
                        + "{\"text\":\"" + targetText + "\",\"color\":\"#c55cf3\"},"
                        + "{\"text\":\"\\n\\nLos jugadores con menos items\\nactivarán totem o morirán\\n\",\"color\":\"#c55cf3\"},"
                        + "{\"text\":\"" + (collectionMethod.equals("block")
                        ? "\\n§7Los bloques no dropean materiales normales.\\n "
                        : "") + "\"}"
                        + "]";
        for (Player p : Bukkit.getOnlinePlayers()) ruletaAnimation.playAnimation(p, jsonMessage);

        // Timer por jugador (ID único) + pintar tablero inicial una vez
        int seconds = parseTimeToSeconds(eventDuration);
        for (Player p : Bukkit.getOnlinePlayers()) {
            participants.add(p.getName());
            playerItems.put(p.getName(), 0);
            playerTimestamp.put(p.getName(), System.currentTimeMillis());

            String id = p.getUniqueId() + "_itemparty_timer";

            if (!timerIdsByPlayer.containsKey(p.getUniqueId())) {
                timerIdsByPlayer.put(p.getUniqueId(), id);
                tiempoCommand.createPlayerBossBar(p, "§5§lItem§d§lParty§f:", seconds, eventDuration, "off", id);
            }

            if (!puntosBars.containsKey(p.getUniqueId())) {
                BossBar puntosBar = Bukkit.createBossBar("§3§lPuntos§f: §b0", BarColor.WHITE, BarStyle.SOLID);
                puntosBar.addPlayer(p);
                puntosBars.put(p.getUniqueId(), puntosBar);
            }
        }
        actualizarScoreboard(); // render inicial
        Bukkit.getScheduler().runTaskLater(plugin, this::terminarEvento, seconds * 20L);
    }

    public void terminarEvento() {
        if (!eventoActivo) return;
        eventoActivo = false;

        // ======= 🧹 LIMPIAR SCOREBOARD COMPLETAMENTE =======
        for (Player p : Bukkit.getOnlinePlayers()) {
            Scoreboard board = p.getScoreboard();

            if (board != null) {
                // 1️⃣ Eliminar el objetivo principal del evento
                Objective objective = board.getObjective("itemparty");
                if (objective != null) objective.unregister();

                // 2️⃣ Eliminar todos los equipos del evento
                for (Team t : new ArrayList<>(board.getTeams())) {
                    if (t.getName().startsWith("itp_") || t.getName().equalsIgnoreCase("itp_title")) {
                        t.unregister();
                    }
                }

                // 3️⃣ Limpiar todas las entradas visibles
                for (String entry : new ArrayList<>(board.getEntries())) {
                    board.resetScores(entry);
                }

                // 4️⃣ Restaurar el scoreboard principal del servidor (evita overlays)
                p.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
            }
        }

        // ======= 🕓 LIMPIAR BOSSBARS =======
        timerIdsByPlayer.values().forEach(tiempoCommand::removeBossBar);
        timerIdsByPlayer.clear();
        for (BossBar bar : puntosBars.values()) {
            bar.removeAll();
        }
        puntosBars.clear();

        // ======= 🧾 LÓGICA FINAL DEL EVENTO =======
        List<Map.Entry<String, Integer>> sorted = ordenarPlayers();
        int total = sorted.size();
        int safeCount = Math.max(0, total + safePlayers);

        Map<String, Integer> puntosSnapshot = new HashMap<>(playerItems);

        Bukkit.broadcastMessage(" ");
        Bukkit.broadcastMessage("§d۞ §5§lEl evento §d§lFiesta de Items §5§lha terminado.");
        Bukkit.broadcastMessage(" ");

        // Jugadores castigados (menos ítems)
        for (int i = safeCount; i < total; i++) {
            String name = sorted.get(i).getKey();
            Player p = Bukkit.getPlayer(name);
            if (p != null && p.isOnline()) {
                p.damage(1000.0); // activa totem o muere
            }
        }

        // ======= 🏆 ANUNCIO TOP =======
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Bukkit.broadcastMessage(" ");
            Bukkit.broadcastMessage("§5§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            Bukkit.broadcastMessage("§d§l         TOP JUGADORES");
            Bukkit.broadcastMessage(" ");
            for (int i = 0; i < Math.min(5, sorted.size()); i++) {
                Map.Entry<String, Integer> e = sorted.get(i);
                String color = (i == 0) ? "§6§l" : (i == 1) ? "§f§l" : (i == 2) ? "§c§l" : "§b";
                Bukkit.broadcastMessage(color + (i + 1) + ". " + e.getKey() + " §7- §f" + e.getValue() + " items");
            }
            Bukkit.broadcastMessage(" ");
            Bukkit.broadcastMessage("§5§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            Bukkit.broadcastMessage(" ");
            for (Player p : Bukkit.getOnlinePlayers()) {
                int pos = 0;
                for (int i = 0; i < sorted.size(); i++) {
                    if (sorted.get(i).getKey().equalsIgnoreCase(p.getName())) {
                        pos = i + 1;
                        break;
                    }
                }
                int puntos = puntosSnapshot.getOrDefault(p.getName(), 0);
                if (pos > 0) {
                    p.sendMessage("§7━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                    p.sendMessage("§dTu posición final: §f#" + pos + " §7(" + puntos + " puntos)");
                    p.sendMessage("§7━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                }
            }
        }, 20L);

        // ======= 🧠 RESET VARIABLES =======
        playerItems.clear();
        playerTimestamp.clear();
        participants.clear();
    }


// ======== MÉTODOS DE SCOREBOARD ACTUALIZADOS (FINAL) ========

    // ========================= SCOREBOARD OPTIMIZADA =========================
    private void actualizarScoreboard() {
        if (!eventoActivo) return;

        long now = System.currentTimeMillis();
        if (now - lastScoreUpdate < 400) return; // Máx. 2 actualizaciones por segundo
        lastScoreUpdate = now;

        List<Map.Entry<String, Integer>> sorted = ordenarPlayers();
        int total = sorted.size();
        int dangerPlayers = Math.abs(safePlayers);
        int safeCount = Math.max(0, total - dangerPlayers);

        // Asegurar 8 líneas (relleno)
        while (sorted.size() < 8) sorted.add(Map.entry("----", 0));

        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (!participants.contains(viewer.getName())) continue;

            Scoreboard board = viewer.getScoreboard();
            Objective obj = board.getObjective("itemparty");

            if (obj == null) {
                obj = board.registerNewObjective(
                        "itemparty",
                        "dummy",
                        Component.text("\u3201\uE084\u3201\u3201")
                                .color(TextColor.fromHexString("#ae52e3"))
                                .decorate(net.kyori.adventure.text.format.TextDecoration.BOLD)
                );
                obj.setDisplaySlot(DisplaySlot.SIDEBAR);

                // Inicializar Teams una vez (no recrearlas cada update)
                board.registerNewTeam("itp_title");
                for (int i = 0; i < 6; i++) board.registerNewTeam("itp_top_" + i);
                for (int i = 0; i < dangerPlayers; i++) board.registerNewTeam("itp_warn_" + i);
            }

            int score = 15;

            // === Línea de título ===
            Team titleTeam = board.getTeam("itp_title");
            if (titleTeam != null) {
                String titleEntry = "§r§r§r";
                if (!titleTeam.getEntries().contains(titleEntry)) titleTeam.addEntry(titleEntry);
                titleTeam.prefix(Component.text("       \uE083")
                        .color(TextColor.fromHexString("#ae52e3"))
                        .decorate(net.kyori.adventure.text.format.TextDecoration.BOLD));
                obj.getScore(titleEntry).setScore(score--);
            }

            // === TOP 1..6 ===
            for (int i = 0; i < 6; i++) {
                Map.Entry<String, Integer> e = (i < safeCount && i < sorted.size()) ? sorted.get(i) : Map.entry("----", 0);
                String nombre = e.getKey();
                int valor = e.getValue();
                if (nombre.length() > 10) nombre = nombre.substring(0, 10) + "..";

                TextColor colorNum = TextColor.fromHexString("#ae52e3");
                TextColor colorName = (i == 0)
                        ? TextColor.fromHexString("#e5d480")
                        : TextColor.fromHexString("#4aa5dc");

                Component linea = nombre.equals("----")
                        ? Component.text(" " + (i + 1) + ". ----", colorNum)
                        : Component.text(" " + (i + 1) + ". ", colorNum)
                        .append(Component.text(nombre + " ", colorName))
                        .append(Component.text("→", NamedTextColor.GRAY))
                        .append(Component.text(String.valueOf(valor), NamedTextColor.WHITE));

                String entryKey = "§r" + " ".repeat(i + 5);
                Team team = board.getTeam("itp_top_" + i);
                if (team != null) {
                    if (!team.getEntries().contains(entryKey)) team.addEntry(entryKey);
                    team.prefix(linea);
                }
                obj.getScore(entryKey).setScore(score--);

                // Espacio visual entre el 1º y 2º
                obj.getScore("§r" + " ".repeat(15 + i)).setScore(score--);
            }

            // === Zona ⚠ (en peligro) ===
            for (int i = 0; i < dangerPlayers; i++) {
                int index = safeCount + i;
                if (index >= sorted.size()) break;

                Map.Entry<String, Integer> e = sorted.get(index);
                String nombre = e.getKey();
                int valor = e.getValue();
                if (nombre.length() > 9) nombre = nombre.substring(0, 9) + "..";

                Component linea = Component.text(" ⚠. ", TextColor.fromHexString("#ae52e3"))
                        .append(Component.text(nombre + " ", TextColor.fromHexString("#ee749e")))
                        .append(Component.text("→", NamedTextColor.GRAY))
                        .append(Component.text(String.valueOf(valor), NamedTextColor.WHITE));

                String entryKey = "§r§r§r§" + i;
                Team team = board.getTeam("itp_warn_" + i);
                if (team != null) {
                    if (!team.getEntries().contains(entryKey)) team.addEntry(entryKey);
                    team.prefix(linea);
                }
                obj.getScore(entryKey).setScore(score--);

                if (i == 0) obj.getScore("§r" + " ".repeat(25 + i)).setScore(score--);
            }
        }
    }



    /**
     * Línea vacía escalonada invisible
     */
    private void addBlankLine(Objective obj, int score, int index) {
        String base = "§r" + " ".repeat(Math.min(12, index + 5));
        obj.getScore(base).setScore(score);
    }

    /**
     * Ordenar jugadores (descendente por ítems y desempate por tiempo)
     */
    private List<Map.Entry<String, Integer>> ordenarPlayers() {
        return playerItems.entrySet().stream()
                .sorted((a, b) -> {
                    int cmp = Integer.compare(b.getValue(), a.getValue());
                    if (cmp == 0)
                        return Long.compare(playerTimestamp.getOrDefault(a.getKey(), 0L),
                                playerTimestamp.getOrDefault(b.getKey(), 0L));
                    return cmp;
                })
                .collect(Collectors.toList());
    }


    private void actualizarPlayersFile(List<Map.Entry<String,Integer>> sorted) {
        playersConfig.set("players", null);
        for (int i = 0; i < sorted.size(); i++) {
            Map.Entry<String,Integer> e = sorted.get(i);
            playersConfig.set("players." + e.getKey() + ".position", i + 1);
            playersConfig.set("players." + e.getKey() + ".items", e.getValue());
            playersConfig.set("players." + e.getKey() + ".timestamp", playerTimestamp.getOrDefault(e.getKey(), 0L));
        }
        try { playersConfig.save(playersFile); }
        catch (IOException ex) { plugin.getLogger().warning("No se pudo guardar itempartyplayers.yml: " + ex.getMessage()); }
    }

    // ===== Drops y recogida =====
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (!eventoActivo || !collectionMethod.equals("block")) return;
        Player player = event.getPlayer();
        if (!participants.contains(player.getName())) return;

        Block block = event.getBlock();
        if (!block.getType().toString().equalsIgnoreCase(collectionTarget)) return;

        event.setDropItems(false);
        ItemStack drop = buildConfiguredItem();
        tagAsEventItem(drop);
        block.getWorld().dropItemNaturally(block.getLocation(), drop);

    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (!eventoActivo || !collectionMethod.equals("mob")) return;
        Player killer = event.getEntity().getKiller();
        if (killer == null || !participants.contains(killer.getName())) return;

        String mobType = getMobType(event.getEntity());
        if (!mobType.equalsIgnoreCase(collectionTarget)) return;

        ItemStack drop = buildConfiguredItem();
        tagAsEventItem(drop);
        event.getEntity().getWorld().dropItemNaturally(event.getEntity().getLocation(), drop);
    }

    @EventHandler
    public void onPickup(EntityPickupItemEvent event) {
        if (!eventoActivo || !(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();
        if (!participants.contains(player.getName())) return;

        ItemStack stack = event.getItem().getItemStack();
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return;

        // Solo cuenta si viene del evento
        Byte origin = meta.getPersistentDataContainer().get(KEY_ORIGIN, PersistentDataType.BYTE);
        if (origin == null || origin != (byte)1) return;

        // Evitar doble conteo del MISMO stack
        Byte counted = meta.getPersistentDataContainer().get(KEY_COUNTED, PersistentDataType.BYTE);
        if (counted != null && counted == (byte)1) return;

        // Crear BossBar si el jugador no la tiene (por reconexión o inicialización tardía)
        if (!puntosBars.containsKey(player.getUniqueId())) {
            BossBar puntosBar = Bukkit.createBossBar("§fMis Puntos: §b0", BarColor.WHITE, BarStyle.SOLID);
            puntosBar.addPlayer(player);
            puntosBars.put(player.getUniqueId(), puntosBar);
        }


        // Marcar stack como contado (se propagará al inventario y seguirá stackeando con otros ya contados)
        meta.getPersistentDataContainer().set(KEY_COUNTED, PersistentDataType.BYTE, (byte)1);
        stack.setItemMeta(meta);

        // subir contador (sumamos la CANTIDAD que se recoge en este stack)
        int amount = stack.getAmount();
        int current = playerItems.getOrDefault(player.getName(), 0) + amount;
        playerItems.put(player.getName(), current);
        playerTimestamp.put(player.getName(), System.currentTimeMillis());

        BossBar puntosBar = puntosBars.get(player.getUniqueId());
        if (puntosBar != null) {
            puntosBar.setTitle("§3§lPuntos§f: §b" + current);
        }

        Bukkit.getScheduler().runTaskLater(plugin, this::actualizarScoreboard, 1L); // SOLO aquí (y en cambios puntuales)
    }

    private void tagAsEventItem(ItemStack is) {
        ItemMeta meta = is.getItemMeta();
        meta.getPersistentDataContainer().set(KEY_ORIGIN, PersistentDataType.BYTE, (byte)1);
        meta.getPersistentDataContainer().set(KEY_COUNTED, PersistentDataType.BYTE, (byte)0);
        is.setItemMeta(meta);
    }

    private ItemStack buildConfiguredItem() {
        switch (itemToCollect) {
            case "caramelo": return partyItems.createCaramelo();
            case "piruleta": return partyItems.createPiruleta();
            case "algodon":
            case "algodondeazucar":
            case "algodon_de_azucar": return partyItems.createAlgodon();
            case "soda":     return partyItems.createSoda();
            default:
                plugin.getLogger().warning("[ItemParty] item_to_collect inválido: " + itemToCollect + " (usando caramelo).");
                return partyItems.createCaramelo();
        }
    }

    // ===== Consumir ítem (click derecho) ⇒ Speed IV + Reg I por 5s =====
    @EventHandler
    public void onUse(PlayerInteractEvent e) {
        if (!eventoActivo) return;
        if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        ItemStack inHand = e.getPlayer().getInventory().getItemInMainHand();
        if (inHand == null || !inHand.hasItemMeta()) return;
        if (!partyItems.isPartyItem(inHand)) return;

        // Consumir 1
        inHand.setAmount(inHand.getAmount() - 1);
        e.getPlayer().getWorld().playSound(e.getPlayer().getLocation(), Sound.ENTITY_GENERIC_EAT, 1f, 1.2f);

        // Efectos: 5s (100 ticks)
        e.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 100, 3, true, true));       // IV => amplifier 3
        e.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 100, 0, true, true)); // I  => amplifier 0
    }

    // ===== MISC =====
/*    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        if (!eventoActivo) return;
        String name = e.getPlayer().getName();
        participants.remove(name);
        playerItems.remove(name);
        playerTimestamp.remove(name);
        actualizarScoreboard();
    }*/

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        if (!eventoActivo) return;

        Player p = e.getPlayer();
        if (!participants.contains(p.getName())) return;

        BossBar bar = puntosBars.get(p.getUniqueId());
        if (bar == null) {
            bar = Bukkit.createBossBar("§3§lPuntos§f: §b0", BarColor.WHITE, BarStyle.SOLID);
            puntosBars.put(p.getUniqueId(), bar);
        }
        if (!bar.getPlayers().contains(p)) {
            bar.addPlayer(p);
        }

        int current = playerItems.getOrDefault(p.getName(), 0);
        bar.setTitle("§3§lPuntos§f: §b" + current);
    }


    private String getMobType(Entity entity) {
        if (entity.getCustomName() != null) return entity.getCustomName().toLowerCase().replace(" ", "");
        return entity.getType().toString().toLowerCase();
    }
    private String getBlockDisplayName(String n) { return n.replace("_", " ").toUpperCase(); }
    private String getMobDisplayName(String n) { return n.replace("_", " ").toUpperCase(); }

    private int parseTimeToSeconds(String t) {
        String[] p = t.split(":");
        return Integer.parseInt(p[0])*3600 + Integer.parseInt(p[1])*60 + Integer.parseInt(p[2]);
    }
    public void resetPlayersFile() {
        playersConfig.set("players", null);
        try { playersConfig.save(playersFile); Bukkit.broadcastMessage("§aArchivo itempartyplayers.yml reiniciado correctamente."); }
        catch (IOException e) { Bukkit.broadcastMessage("§cError al reiniciar itempartyplayers.yml: " + e.getMessage()); }
    }
}
