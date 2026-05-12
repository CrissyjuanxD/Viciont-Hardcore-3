package TitleListener;

import Handlers.DamageLogListener;
import Handlers.DayHandler;
import net.kyori.adventure.text.TranslatableComponent;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import Handlers.DeathStormHandler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.ArrayList;

public class MuerteHandler implements Listener {
    private final JavaPlugin plugin;
    private final MuerteAnimation muerteAnimation;
    private final DamageLogListener damageLogListener;
    private final DeathStormHandler deathStormHandler;
    private final DayHandler dayHandler;
    private BukkitRunnable currentDeathMessageTask;
    private static volatile boolean isDeathMessageActive = false;
    private final Map<Player, Location> deathLocations = new HashMap<>();

    public MuerteHandler(JavaPlugin plugin, DamageLogListener damageLogListener, DeathStormHandler deathStormHandler, DayHandler dayHandler) {
        this.plugin = plugin;
        this.damageLogListener = damageLogListener;
        this.deathStormHandler = deathStormHandler;
        this.dayHandler = dayHandler;
        this.muerteAnimation = new MuerteAnimation(plugin);
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        UUID playerId = player.getUniqueId();

        // Pausar el DamageLog al instante para limpiar la pantalla
        Bukkit.getOnlinePlayers().forEach(onlinePlayer -> {
            UUID onlinePlayerId = onlinePlayer.getUniqueId();
            if (damageLogListener != null) {
                damageLogListener.pauseActionBarForPlayer(onlinePlayerId);
            }
        });

        Location deathLocation = player.getLocation();
        deathLocations.put(player, deathLocation);
        String playerNames = player.getName();

        muerteAnimation.playAnimation(player, "");

        Component original = event.deathMessage();
        if (original == null) return;

        // Configuración de Colores Hexadecimales
        TextColor colorPrincipal = TextColor.fromHexString("#8930DA");
        TextColor colorSecundario = TextColor.fromHexString("#BE9BE4");

        Component formatted;

        if (original instanceof TranslatableComponent translatable) {
            String key = translatable.key();
            List<Component> args = translatable.args();
            List<Component> coloredArgs = new ArrayList<>();

            for (int i = 0; i < args.size(); i++) {
                if (i == 0) {
                    coloredArgs.add(Component.text(player.getName()).color(colorPrincipal).decorate(TextDecoration.BOLD));
                } else {
                    coloredArgs.add(forceColorAndBold(args.get(i), colorPrincipal));
                }
            }

            // AQUI ESTÁ EL TRUCO: Usar el caracter invisible (\u200B) en lugar de n!
            formatted = Component.text("\u200B").append(
                    Component.translatable(key, coloredArgs).color(colorSecundario)
            );

        } else {
            // Y AQUÍ TAMBIÉN:
            formatted = Component.text("\u200B")
                    .append(Component.text(player.getName()).color(colorPrincipal).decorate(TextDecoration.BOLD))
                    .append(Component.text(" ha muerto").color(colorSecundario));
        }

        Component actionBarMessage = formatted;

        // FEEDBACK VISUAL INMEDIATO: Le enviamos el Action Bar al instante SOLO a la víctima
        player.sendActionBar(actionBarMessage);

        if (currentDeathMessageTask != null && !currentDeathMessageTask.isCancelled()) {
            currentDeathMessageTask.cancel();
        }

        currentDeathMessageTask = new BukkitRunnable() {
            int duration = 7 * 20;
            int counter = 0;

            @Override
            public void run() {
                if (counter < duration) {
                    sendActionBarToAllPlayers(actionBarMessage);
                    counter += 20;
                } else {
                    isDeathMessageActive = false;
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        Bukkit.getOnlinePlayers().forEach(onlinePlayer -> {
                            UUID onlinePlayerId = onlinePlayer.getUniqueId();
                            if (onlinePlayerId.equals(player.getUniqueId())) return;

                            if (damageLogListener != null && damageLogListener.isPlayerInDamageLog(onlinePlayerId)) {
                                damageLogListener.resumeActionBarForPlayer(onlinePlayerId);
                            }
                        });
                    });

                    Bukkit.getScheduler().runTask(plugin, () -> {
                        if (damageLogListener != null) {
                            damageLogListener.resumeActionBarForPlayer(player.getUniqueId());
                        }
                    });
                    this.cancel();
                }
            }
        };
        // RETRASO DE 1 SEGUNDO (20 Ticks) ANTES DE MANDAR EL ACTION BAR AL RESTO
        currentDeathMessageTask.runTaskTimer(plugin, 20, 20);

        new BukkitRunnable() {
            @Override
            public void run() {
                executeBukkitCommands(player);

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        Bukkit.getOnlinePlayers().forEach(p -> p.playSound(p.getLocation(), Sound.BLOCK_END_PORTAL_SPAWN, 100000.0f, 0.1f));

                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                deathStormHandler.addDeathStormTime(player);

                                new BukkitRunnable() {
                                    @Override
                                    public void run() {

                                        int day = dayHandler.getCurrentDay();
                                        double chance;
                                        int amplifierBonus;

                                        if (day <= 3) {
                                            chance = 0.15;
                                            amplifierBonus = 0;
                                        } else if (day <= 7) {
                                            chance = 0.30;
                                            amplifierBonus = 2;
                                        } else if (day <= 11) {
                                            chance = 0.45;
                                            amplifierBonus = 3;
                                        } else if (day <= 20) {
                                            chance = 0.60;
                                            amplifierBonus = 4;
                                        } else {
                                            chance = 0.80;
                                            amplifierBonus = 5;
                                        }

                                        if (Math.random() <= chance) {
                                            applyCurseToAllPlayers(amplifierBonus);
                                            sendTitleToAllPlayers(playerNames);
                                        }
                                    }
                                }.runTaskLater(plugin, 4 * 20);
                            }
                        }.runTaskLater(plugin, 10);
                    }
                }.runTaskLater(plugin, 3 * 20);
            }
        }.runTaskLater(plugin, 10 * 20);
    }

    public static boolean isDeathMessageActive() {
        return isDeathMessageActive;
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();

            if (player.getGameMode() == GameMode.SPECTATOR && event.getCause() == EntityDamageEvent.DamageCause.VOID) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        Location deathLocation = deathLocations.remove(player);

        if (deathLocation != null) {
            event.setRespawnLocation(deathLocation);
        }
    }

    private void executeBukkitCommands(Player player) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(p.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 100000.0f, 2f);
            p.playSound(p.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 100000.0f, 0.1f);
            p.playSound(p.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 100000.0f, 0.1f);
            p.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 10, 2));
        }
    }

    private void applyCurseToAllPlayers(int amplifierBonus) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(p.getLocation(), Sound.ITEM_TOTEM_USE, 100000.0f, 0.1f);
            p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_DEATH, 100000.0f, 0.7f);

            p.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 14 * 20, amplifierBonus));
            p.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 6 * 20, 2 + amplifierBonus));
            p.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 20 * 20, 9 + amplifierBonus));
            p.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 7 * 20, 2 + amplifierBonus));
            p.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 10 * 20, 3 + amplifierBonus));
        }
    }

    private void sendTitleToAllPlayers(String playerName) {
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            onlinePlayer.sendTitle(
                    ChatColor.DARK_GRAY + "" + ChatColor.MAGIC + "Ｏ" + ChatColor.RESET + ChatColor.DARK_PURPLE + " ᴍᴀʟᴅɪᴄɪᴏɴ ʟɪʙᴇʀᴀᴅᴀ " + ChatColor.RESET + ChatColor.GRAY + ChatColor.MAGIC + "Ｏ",
                    ChatColor.GRAY + "" + ChatColor.BOLD + "۞ "
                            + ChatColor.GOLD + playerName
                            + ChatColor.GRAY + " ʜᴀ ᴇɴᴏᴊᴀᴅᴏ ᴀʟ "
                            + ChatColor.DARK_GRAY + "" + ChatColor.BOLD + "ᴅᴀʀᴋ ᴅᴇᴍᴏɴ"
                            + ChatColor.GRAY + " ۞",
                    10,
                    70,
                    20
            );
        }
    }

    private void sendActionBarToAllPlayers(Component message) {
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            onlinePlayer.sendActionBar(message);
        }
    }

    private Component forceColorAndBold(Component component, TextColor color) {
        Component base = component.color(color).decorate(TextDecoration.BOLD);

        if (!component.children().isEmpty()) {
            List<Component> recoloredChildren = new ArrayList<>();
            for (Component child : component.children()) {
                recoloredChildren.add(forceColorAndBold(child, color));
            }
            base = base.children(recoloredChildren);
        }
        return base;
    }
}