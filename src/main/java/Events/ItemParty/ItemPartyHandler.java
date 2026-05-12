package Events.ItemParty;

import Commands.TiempoCommand;
import TitleListener.RuletaAnimation;
import items.ItemsPartyRecolect;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.*;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class ItemPartyHandler implements Listener {

    private final JavaPlugin plugin;
    private final TiempoCommand tiempoCommand;
    private final RuletaAnimation ruletaAnimation;
    private final ItemsPartyRecolect partyItems;
    private long lastScoreUpdate = 0L;

    private boolean eventoActivo = false;
    private String itemToCollect;
    private String collectionMethod;
    private String collectionTarget;
    private String eventDuration;
    private int safePlayers;
    private Scoreboard eventScoreboard;

    private final Map<String, Integer> playerItems = new LinkedHashMap<>();
    private final Map<String, Long> playerTimestamp = new HashMap<>();
    private final Set<String> participants = new HashSet<>();

    private File configFile, playersFile;
    private FileConfiguration config, playersConfig;

    private final Map<UUID, String> timerIdsByPlayer = new HashMap<>();
    private final Map<UUID, BossBar> puntosBars = new HashMap<>();

    private final NamespacedKey KEY_ORIGIN;
    private final NamespacedKey KEY_COUNTED;

    public ItemPartyHandler(JavaPlugin plugin, TiempoCommand tiempoCommand) {
        this.plugin = plugin;
        this.tiempoCommand = tiempoCommand;
        this.ruletaAnimation = new RuletaAnimation(plugin);
        this.partyItems = new ItemsPartyRecolect(plugin);
        this.KEY_ORIGIN = new NamespacedKey(plugin, "itemparty_origin");
        this.KEY_COUNTED = new NamespacedKey(plugin, "itemparty_counted");
        loadConfig();
        checkAllPunishments();
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

    private void syncTeamsToEventScoreboard() {
        if (eventScoreboard == null) return;
        Scoreboard mainBoard = Bukkit.getScoreboardManager().getMainScoreboard();

        for (Team mainTeam : mainBoard.getTeams()) {
            Team newTeam = eventScoreboard.getTeam(mainTeam.getName());
            if (newTeam == null) {
                newTeam = eventScoreboard.registerNewTeam(mainTeam.getName());
            }
            newTeam.setPrefix(mainTeam.getPrefix());
            newTeam.setSuffix(mainTeam.getSuffix());
            newTeam.setColor(mainTeam.getColor());
            newTeam.setOption(Team.Option.NAME_TAG_VISIBILITY, mainTeam.getOption(Team.Option.NAME_TAG_VISIBILITY));
            newTeam.setOption(Team.Option.COLLISION_RULE, mainTeam.getOption(Team.Option.COLLISION_RULE));

            for (String entry : mainTeam.getEntries()) {
                if (!newTeam.hasEntry(entry)) {
                    newTeam.addEntry(entry);
                }
            }
        }
    }

    public void iniciarEvento() {
        if (eventoActivo) { Bukkit.broadcastMessage("§c¡El evento ya está activo!"); return; }
        if (Bukkit.getOnlinePlayers().size() < 3) {
            Bukkit.broadcastMessage("§c¡Se necesitan al menos 3 jugadores para iniciar el evento!");
            return;
        }
        eventoActivo = true;
        playerItems.clear(); playerTimestamp.clear(); participants.clear();

        if (eventScoreboard == null) {
            eventScoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        }

        syncTeamsToEventScoreboard();

        Objective oldObj = eventScoreboard.getObjective("itemparty");
        if (oldObj != null) oldObj.unregister();

        // Título de Viciont
        Objective obj = eventScoreboard.registerNewObjective("itemparty", "dummy",
                Component.text("\u3201\uE084\u3201\u3201").color(TextColor.fromHexString("#ae52e3")).decorate(TextDecoration.BOLD));
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);

        for (Player p : Bukkit.getOnlinePlayers()) {
            participants.add(p.getName());
            playerItems.put(p.getName(), 0);
            playerTimestamp.put(p.getName(), System.currentTimeMillis());
            p.playSound(p.getLocation(), Sound.MUSIC_DISC_OTHERSIDE, 100f, 1f);
            p.setScoreboard(eventScoreboard);
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
                        + "{\"text\":\"¡Ha comenzado la \",\"color\":\"#c55cf3\"},"
                        + "{\"text\":\"FIESTA DE ITEMS\",\"bold\":true,\"color\":\"#ae52e3\"},"
                        + "{\"text\":\"!\\n\\n\",\"color\":\"#c55cf3\"},"
                        + "{\"text\":\"Recolecta ítems \",\"color\":\"#c55cf3\"},"
                        + "{\"text\":\"" + targetText + "\",\"color\":\"#c55cf3\"},"
                        + "{\"text\":\"\\n\\nLos jugadores con menos items\\nactivarán totem o morirán\\ny se harán chiquitos por 4h.\\n\",\"color\":\"#c55cf3\"},"
                        + "{\"text\":\"" + (collectionMethod.equals("block") ? "\\n§7Los bloques no dropean materiales normales.\\n " : "") + "\"}"
                        + "]";

        for (Player p : Bukkit.getOnlinePlayers()) ruletaAnimation.playAnimation(p, "rosa", "evento", "center", jsonMessage);

        int seconds = parseTimeToSeconds(eventDuration);
        for (Player p : Bukkit.getOnlinePlayers()) {
            String id = p.getUniqueId() + "_itemparty_timer";
            if (!timerIdsByPlayer.containsKey(p.getUniqueId())) {
                timerIdsByPlayer.put(p.getUniqueId(), id);
                // Nombre bossbar Viciont
                tiempoCommand.createPlayerBossBar(p, "§5§lItem§d§lParty§f:", seconds, eventDuration, "off", id);
            }
            if (!puntosBars.containsKey(p.getUniqueId())) {
                BossBar puntosBar = Bukkit.createBossBar("§3§lPuntos§f: §b0", BarColor.WHITE, BarStyle.SOLID);
                puntosBar.addPlayer(p);
                puntosBars.put(p.getUniqueId(), puntosBar);
            }
        }
        actualizarScoreboard();
        Bukkit.getScheduler().runTaskLater(plugin, this::terminarEvento, seconds * 20L);
    }

    public void terminarEvento() {
        if (!eventoActivo) return;
        eventoActivo = false;

        // ======= LIMPIAR SCOREBOARD =======
        for (Player p : Bukkit.getOnlinePlayers()) {
            Scoreboard board = p.getScoreboard();
            if (board != null) {
                Objective objective = board.getObjective("itemparty");
                if (objective != null) objective.unregister();

                for (Team t : new ArrayList<>(board.getTeams())) {
                    if (t.getName().startsWith("itp_") || t.getName().equalsIgnoreCase("itp_title") || t.getName().startsWith("space_")) {
                        t.unregister();
                    }
                }

                for (String entry : new ArrayList<>(board.getEntries())) {
                    board.resetScores(entry);
                }

                p.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
            }
            p.stopSound(Sound.MUSIC_DISC_OTHERSIDE);
        }

        eventScoreboard = null;

        // ======= LIMPIAR BOSSBARS =======
        timerIdsByPlayer.values().forEach(tiempoCommand::removeBossBar);
        timerIdsByPlayer.clear();
        for (BossBar bar : puntosBars.values()) bar.removeAll();
        puntosBars.clear();

        // ======= LÓGICA DE CASTIGOS (COMBINADA) =======
        List<Map.Entry<String, Integer>> sorted = ordenarPlayers();
        int total = sorted.size();

        int safeCount;
        if (safePlayers >= 0) {
            safeCount = safePlayers;
        } else {
            safeCount = Math.max(0, total + safePlayers);
        }

        List<String> losersNames = new ArrayList<>();
        long durationMillis = 4L * 60L * 60L * 1000L;
        long endTime = System.currentTimeMillis() + durationMillis;
        int tickDuration = 4 * 60 * 60 * 20;

        for (int i = safeCount; i < total; i++) {
            if (i >= sorted.size()) break;
            String name = sorted.get(i).getKey();
            if (name.equals("----")) continue;

            losersNames.add(name);
            Player p = Bukkit.getPlayer(name);
            UUID uuid = (p != null) ? p.getUniqueId() : Bukkit.getOfflinePlayer(name).getUniqueId();
            playersConfig.set("punishments." + uuid, endTime);

            if (p != null && p.isOnline()) {
                // Castigo de Quaso (Lentitud, Debilidad, Tamaño)
                aplicarCastigo(p, tickDuration);
                p.playSound(p.getLocation(), Sound.ENTITY_WITCH_CELEBRATE, 1f, 0.5f);

                // Castigo de Viciont (Daño Letal)
                p.damage(1000.0);
            }
        }
        savePlayersConfig();

        // ======= MENSAJES FINALES =======
        Bukkit.broadcastMessage(" ");
        Bukkit.broadcastMessage("§d۞ §5§lEl evento §d§lFiesta de Items §5§lha terminado.");

        if (!losersNames.isEmpty()) {
            String listaPerdedores = String.join(", ", losersNames);
            Bukkit.broadcastMessage(" ");
            Bukkit.broadcastMessage("§cLos jugadores " + listaPerdedores + " han perdido el evento.");
            Bukkit.broadcastMessage("§cHan recibido daño letal y su tamaño será reducido por 4 horas.");
            Bukkit.broadcastMessage(" ");
        } else {
            Bukkit.broadcastMessage("§a¡Increíble! Nadie ha perdido esta vez.");
        }

        // ======= ANUNCIO TOP =======
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Bukkit.broadcastMessage("§5§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            Bukkit.broadcastMessage("§d§l         TOP JUGADORES");
            Bukkit.broadcastMessage(" ");
            for (int i = 0; i < Math.min(5, sorted.size()); i++) {
                Map.Entry<String, Integer> e = sorted.get(i);
                if (e.getKey().equals("----")) continue;
                String color = (i == 0) ? "§6§l" : (i == 1) ? "§f§l" : (i == 2) ? "§c§l" : "§b";
                Bukkit.broadcastMessage(color + (i + 1) + ". " + e.getKey() + " §7- §f" + e.getValue() + " items");
            }
            Bukkit.broadcastMessage(" ");
            Bukkit.broadcastMessage("§5§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            playerItems.clear(); playerTimestamp.clear(); participants.clear();
        }, 40L);
    }

    public boolean quitarCastigoManualmente(String playerName) {
        Player target = Bukkit.getPlayer(playerName);
        UUID uuid = (target != null) ? target.getUniqueId() : Bukkit.getOfflinePlayer(playerName).getUniqueId();

        if (playersConfig.contains("punishments." + uuid)) {
            playersConfig.set("punishments." + uuid, null);
            savePlayersConfig();
            if (target != null && target.isOnline()) {
                restaurarJugador(target);
                target.sendMessage("§a¡Tu castigo ha sido retirado administrativamente!");
            }
            return true;
        }
        return false;
    }

    private void aplicarCastigo(Player p, int durationTicks) {
        p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, durationTicks, 0, false, false, true));
        p.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, durationTicks, 0, false, false, true));
        try {
            AttributeInstance scale = p.getAttribute(Attribute.GENERIC_SCALE);
            if (scale != null) scale.setBaseValue(0.5);
        } catch (Throwable ignored) {}
    }

    private void restaurarJugador(Player p) {
        p.removePotionEffect(PotionEffectType.SLOWNESS);
        p.removePotionEffect(PotionEffectType.WEAKNESS);
        try {
            AttributeInstance scale = p.getAttribute(Attribute.GENERIC_SCALE);
            if (scale != null) scale.setBaseValue(1.0);
        } catch (Throwable ignored) {}
    }

    private void checkAllPunishments() {
        if (!playersConfig.contains("punishments")) return;
        long now = System.currentTimeMillis();
        for (String uuidStr : playersConfig.getConfigurationSection("punishments").getKeys(false)) {
            long end = playersConfig.getLong("punishments." + uuidStr);
            if (now > end) {
                playersConfig.set("punishments." + uuidStr, null);
            }
        }
        savePlayersConfig();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player p = event.getPlayer();
        String uuidStr = p.getUniqueId().toString();

        if (eventoActivo && participants.contains(p.getName())) {
            BossBar bar = puntosBars.get(p.getUniqueId());
            if (bar == null) {
                bar = Bukkit.createBossBar("§3§lPuntos§f: §b0", BarColor.WHITE, BarStyle.SOLID);
                puntosBars.put(p.getUniqueId(), bar);
            }
            if (!bar.getPlayers().contains(p)) bar.addPlayer(p);

            Scoreboard mainBoard = Bukkit.getScoreboardManager().getMainScoreboard();
            Team mainTeam = mainBoard.getEntryTeam(p.getName());
            if (mainTeam != null && eventScoreboard != null) {
                Team evTeam = eventScoreboard.getTeam(mainTeam.getName());
                if (evTeam != null && !evTeam.hasEntry(p.getName())) {
                    evTeam.addEntry(p.getName());
                }
            }

            int current = playerItems.getOrDefault(p.getName(), 0);
            bar.setTitle("§3§lPuntos§f: §b" + current);
        }

        if (playersConfig.contains("punishments." + uuidStr)) {
            long endTime = playersConfig.getLong("punishments." + uuidStr);
            long now = System.currentTimeMillis();
            if (now > endTime) {
                restaurarJugador(p);
                playersConfig.set("punishments." + uuidStr, null);
                savePlayersConfig();
            } else {
                long remainingMillis = endTime - now;
                int ticks = (int) (remainingMillis / 50);
                if (ticks > 0) aplicarCastigo(p, ticks);
            }
        }
    }

    private void savePlayersConfig() {
        try { playersConfig.save(playersFile); }
        catch (IOException ex) { plugin.getLogger().warning("Error guardando itempartyplayers.yml"); }
    }

    // ========================= SCOREBOARD OPTIMIZADA (Estética Viciont) =========================
    private void actualizarScoreboard() {
        if (!eventoActivo || eventScoreboard == null) return;
        long now = System.currentTimeMillis();
        if (now - lastScoreUpdate < 500) return;
        lastScoreUpdate = now;

        List<Map.Entry<String, Integer>> sorted = ordenarPlayers();
        int total = sorted.size();
        int dangerPlayers = Math.abs(safePlayers);
        int safeCount = Math.max(0, total - dangerPlayers);

        while (sorted.size() < 8) sorted.add(Map.entry("----", 0));

        Objective obj = eventScoreboard.getObjective("itemparty");
        if (obj == null) return;

        int score = 15;

        // Título Estático Viciont
        Team titleTeam = getOrCreateTeam(eventScoreboard, "itp_title");
        String titleEntry = "§r§r§r";
        if (!titleTeam.hasEntry(titleEntry)) titleTeam.addEntry(titleEntry);
        titleTeam.prefix(Component.text("       \uE083")
                .color(TextColor.fromHexString("#ae52e3"))
                .decorate(TextDecoration.BOLD));
        obj.getScore(titleEntry).setScore(score--);

        // TOP SECTION (1 al 6)
        for (int i = 0; i < 6; i++) {
            Map.Entry<String, Integer> e = (i < safeCount && i < sorted.size()) ? sorted.get(i) : Map.entry("----", 0);
            String nombre = e.getKey();
            int valor = e.getValue();
            if (nombre.length() > 10) nombre = nombre.substring(0, 10) + "..";

            Team team = getOrCreateTeam(eventScoreboard, "itp_top_" + i);
            String hiddenKey = "§" + i;
            if (!team.hasEntry(hiddenKey)) team.addEntry(hiddenKey);

            TextColor colorNum = TextColor.fromHexString("#ae52e3");
            TextColor colorName = (i == 0) ? TextColor.fromHexString("#e5d480") : TextColor.fromHexString("#4aa5dc");

            Component linea = nombre.equals("----")
                    ? Component.text(" " + (i + 1) + ". ----", colorNum)
                    : Component.text(" " + (i + 1) + ". ", colorNum)
                    .append(Component.text(nombre + " ", colorName))
                    .append(Component.text("→ ", NamedTextColor.GRAY))
                    .append(Component.text(valor, NamedTextColor.WHITE));

            team.prefix(linea);
            obj.getScore(hiddenKey).setScore(score--);

            // Espacio de separación (Relleno) Viciont Style
            setSpace(obj, score--, 15 + i);
        }

        // DANGER SECTION (Zona Peligro)
        for (int i = 0; i < dangerPlayers; i++) {
            int index = safeCount + i;
            if (index >= sorted.size()) break;

            Map.Entry<String, Integer> e = sorted.get(index);
            String nombre = e.getKey();
            int valor = e.getValue();
            if (nombre.length() > 9) nombre = nombre.substring(0, 9) + "..";

            Team team = getOrCreateTeam(eventScoreboard, "itp_warn_" + i);
            String hiddenKey = "§c" + " ".repeat(i + 1);
            if (!team.hasEntry(hiddenKey)) team.addEntry(hiddenKey);

            Component linea = Component.text(" ⚠. ", NamedTextColor.RED)
                    .append(Component.text(nombre + " ", TextColor.fromHexString("#ee749e")))
                    .append(Component.text("→ ", NamedTextColor.GRAY))
                    .append(Component.text(valor, NamedTextColor.WHITE));

            team.prefix(linea);
            obj.getScore(hiddenKey).setScore(score--);

            if (i == 0) setSpace(obj, score--, 25 + i);
        }

        // Asegurar que los jugadores nuevos ven la scoreboard
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (participants.contains(p.getName()) && p.getScoreboard() != eventScoreboard) {
                p.setScoreboard(eventScoreboard);
            }
        }
    }

    private void setSpace(Objective obj, int score, int uniqueId) {
        Team t = getOrCreateTeam(eventScoreboard, "space_" + uniqueId);
        String entry = "§" + (char)('a' + (uniqueId % 20)) + "§r";
        if (!t.hasEntry(entry)) t.addEntry(entry);
        t.prefix(Component.text(""));
        obj.getScore(entry).setScore(score);
    }

    private Team getOrCreateTeam(Scoreboard board, String name) {
        Team t = board.getTeam(name);
        if (t == null) t = board.registerNewTeam(name);
        return t;
    }

    private List<Map.Entry<String, Integer>> ordenarPlayers() {
        return playerItems.entrySet().stream()
                .sorted((a, b) -> {
                    int cmp = Integer.compare(b.getValue(), a.getValue());
                    if (cmp == 0) return Long.compare(playerTimestamp.getOrDefault(a.getKey(), 0L), playerTimestamp.getOrDefault(b.getKey(), 0L));
                    return cmp;
                })
                .collect(Collectors.toList());
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

        Byte origin = meta.getPersistentDataContainer().get(KEY_ORIGIN, PersistentDataType.BYTE);
        if (origin == null || origin != (byte)1) return;

        Byte counted = meta.getPersistentDataContainer().get(KEY_COUNTED, PersistentDataType.BYTE);
        if (counted != null && counted == (byte)1) return;

        if (!puntosBars.containsKey(player.getUniqueId())) {
            BossBar puntosBar = Bukkit.createBossBar("§fMis Puntos: §b0", BarColor.WHITE, BarStyle.SOLID);
            puntosBar.addPlayer(player);
            puntosBars.put(player.getUniqueId(), puntosBar);
        }

        meta.getPersistentDataContainer().set(KEY_COUNTED, PersistentDataType.BYTE, (byte)1);
        stack.setItemMeta(meta);

        int amount = stack.getAmount();
        int current = playerItems.getOrDefault(player.getName(), 0) + amount;
        playerItems.put(player.getName(), current);
        playerTimestamp.put(player.getName(), System.currentTimeMillis());

        BossBar puntosBar = puntosBars.get(player.getUniqueId());
        if (puntosBar != null) {
            puntosBar.setTitle("§3§lPuntos§f: §b" + current);
        }

        Bukkit.getScheduler().runTaskLater(plugin, this::actualizarScoreboard, 1L);
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

    @EventHandler
    public void onUse(PlayerInteractEvent e) {
        if (!eventoActivo) return;
        if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        ItemStack inHand = e.getPlayer().getInventory().getItemInMainHand();
        if (inHand == null || !inHand.hasItemMeta()) return;
        if (!partyItems.isPartyItem(inHand)) return;

        inHand.setAmount(inHand.getAmount() - 1);
        e.getPlayer().getWorld().playSound(e.getPlayer().getLocation(), Sound.ENTITY_GENERIC_EAT, 1f, 1.2f);

        e.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 100, 3, true, true));
        e.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 100, 0, true, true));
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
        playersConfig.set("punishments", null);
        playersConfig.set("players", null);
        try { playersConfig.save(playersFile); Bukkit.broadcastMessage("§aArchivo itempartyplayers.yml reiniciado correctamente."); }
        catch (IOException e) { Bukkit.broadcastMessage("§cError al reiniciar itempartyplayers.yml: " + e.getMessage()); }
    }
}