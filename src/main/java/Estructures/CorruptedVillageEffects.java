package Estructures;

/*
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import vct.hardcore3.ViciontHardcore3;

public class CorruptedVillageEffects implements Listener {
    private final JavaPlugin plugin;

    public CorruptedVillageEffects(JavaPlugin plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Location location = player.getLocation();

        if (isInCorruptedVillage(location)) {
            if (isBeforeDayOne()) {
                applyBlindnessAndDamage(player);
            }
            applyDarknessEffect(player);
        }
    }

    private boolean isInCorruptedVillage(Location location) {
        // Lógica para verificar si el jugador está en una aldea corrupta
        return false;
    }

    private boolean isBeforeDayOne() {
        return ((ViciontHardcore3) plugin).getDayHandler().getCurrentDay() < 1;
    }

    private void applyDarknessEffect(Player player) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 20, 1, true, false));
    }

    private void applyBlindnessAndDamage(Player player) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!isInCorruptedVillage(player.getLocation()) || !isBeforeDayOne()) {
                    cancel();
                    return;
                }
                player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20, 1, true, false));
                player.damage(2);
            }
        }.runTaskTimer(plugin, 0, 40); // Cada segundo
    }
}
*/
