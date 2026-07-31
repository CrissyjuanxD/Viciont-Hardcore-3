package Dificultades.CustomMobs;

import Dificultades.Features.MobSoundManager;
import net.md_5.bungee.api.ChatColor;
import Handlers.DayHandler;
import items.CorruptedMobItems;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import vct.hardcore3.ViciontHardcore3;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class CorruptedSpider implements Listener, Gui.GuiMobProvider {
    private final JavaPlugin plugin;
    private final DayHandler dayHandler;
    private final NamespacedKey corrupedtedspiderKey;
    private static boolean eventsRegistered = false;

    public CorruptedSpider(JavaPlugin plugin, DayHandler dayHandler) {
        this.plugin = plugin;
        this.dayHandler = dayHandler;
        this.corrupedtedspiderKey = new NamespacedKey(plugin, "corruptedspider");
        MobSoundManager.register(corrupedtedspiderKey, Sound.ENTITY_SPIDER_AMBIENT, Sound.ENTITY_SPIDER_STEP, 0.6f, 1.0f);

        ((ViciontHardcore3) plugin).getEntidadesGuiManager().registerMob(this);
    }

    public void apply() {
        if (!eventsRegistered) {
            Bukkit.getPluginManager().registerEvents(this, plugin);
            eventsRegistered = true;

            ((ViciontHardcore3) plugin).getEntidadesGuiManager().unlockMob(getEntityId());
        }
    }

    public void revert() {
        if (eventsRegistered) {
            for (World world : Bukkit.getWorlds()) {
                for (Entity entity : world.getEntities()) {
                    if (entity instanceof Spider spider && isCorruptedSpider(spider)) {
                        spider.remove();
                    }
                }
            }
            eventsRegistered = false;

            ((ViciontHardcore3) plugin).getEntidadesGuiManager().lockMob(getEntityId());
        }
    }

    // ==========================================
    // IMPLEMENTACIÓN DE GUIMOBPROVIDER
    // ==========================================
    @Override
    public String getEntityId() { return "corrupted_spider"; }

    @Override
    public String getEntityType() { return "minecraft:spider"; }

    @Override
    public String getName() { return "Corrupted Spider"; }

    @Override
    public String getColor() { return "#AA00AA"; }

    @Override
    public int getScale() { return 22; }

    @Override
    public List<String> getDynamicAttributes() {
        int currentDay = dayHandler.getCurrentDay();

        double health = 16.0;
        double damage = 3.0;

/*        if (currentDay >= 9) {
            health += 8.0;
            damage += 2.0;
        }*/

        String cRojo = ChatColor.of("#F51916").toString();
        String cAzul =ChatColor.of("#54A1D1").toString();
        String cNaranja = ChatColor.of("#F29329").toString();
        String cVerde = ChatColor.of("#8AF58F").toString();
        String cAmarillo = ChatColor.of("#EEDB7E").toString();
        String cMoradoText = ChatColor.of("#BE7DE9").toString();
        String cBlanco = ChatColor.WHITE.toString();

        List<String> attributes = new ArrayList<>();
        attributes.add(cRojo + "❤ " + cMoradoText + "Vida" + cBlanco + ": " + health);
        attributes.add(cAzul + "🗡 " + cMoradoText + "Daño de Ataque" + cBlanco + ": " + damage);
        attributes.add(cNaranja + "⚠ " + cMoradoText + "Rango DE vision" + cBlanco + ": 32.0");
        attributes.add(cVerde + "⚡ " + cMoradoText + "Efectos" + cBlanco + ": Velocidad I");
        attributes.add(cAmarillo + "🕸 " + cMoradoText + "Habilidad" + cBlanco + ": Enredadera");
        return attributes;
    }

    @Override
    public List<String> getDescription() {
        return Arrays.asList(
                "Una araña mutada que se mueve a velocidades",
                "extremas.",
                "",
                "Al morderte, generará una telaraña",
                "directamente en tus pies para atraparte.",
                "Puedes evitar este efecto si logras",
                "bloquear su ataque a tiempo con un escudo."
        );
    }

    public Spider spawnCorruptedSpider(Location location) {
        Spider corruptedSpider = (Spider) location.getWorld().spawnEntity(location, EntityType.SPIDER);
        applyCorruptedSpiderAttributes(corruptedSpider);
        return corruptedSpider;
    }

    public void transformspawnCorruptedSpider(Spider spider) {
        applyCorruptedSpiderAttributes(spider);
    }

    private void applyCorruptedSpiderAttributes(Spider spider) {
        spider.setCustomName(ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "Corrupted Spider");
        spider.setCustomNameVisible(false);
        spider.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(3.0);
        spider.getAttribute(Attribute.GENERIC_FOLLOW_RANGE).setBaseValue(32);
        spider.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 0));
        spider.getPersistentDataContainer().set(corrupedtedspiderKey, PersistentDataType.BYTE, (byte) 1);
    }


    @EventHandler
    public void onSpiderHit(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Spider && event.getEntity() instanceof Player) {
            Spider spider = (Spider) event.getDamager();
            Player player = (Player) event.getEntity();

             if (isCorruptedSpider(spider) && !player.isBlocking()) {
                player.getLocation().getBlock().setType(Material.COBWEB);
            }
        }
    }

    //SONIDOS
    @EventHandler
    public void onCorruptedSpiderHurt(EntityDamageEvent event) {
        if (event.getEntity() instanceof Spider spider && isCorruptedSpider(spider)) {
            spider.getWorld().playSound(spider.getLocation(), Sound.ENTITY_SPIDER_HURT, SoundCategory.HOSTILE, 1.0f, 0.6f);
        }
    }

    @EventHandler
    public void onCorruptedSpiderDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof Spider spider && isCorruptedSpider(spider)) {
            spider.getWorld().playSound(spider.getLocation(), Sound.ENTITY_SPIDER_DEATH, SoundCategory.HOSTILE, 1.0f, 0.6f);

            if (Math.random() <= 0.35) {
                spider.getWorld().dropItemNaturally(spider.getLocation(), CorruptedMobItems.createCorruptedSpiderEye());
            }
        }
    }

    public NamespacedKey getCorruptedSpiderKey() {
        return  corrupedtedspiderKey;
    }

    public boolean isCorruptedSpider(Spider spider) {
        return spider.getPersistentDataContainer().has(corrupedtedspiderKey, PersistentDataType.BYTE);
    }

}
