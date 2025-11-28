package Bosses;

import org.bukkit.*;
import org.bukkit.entity.Player;

public class AreaZone {

    public enum Shape { CIRCULAR, SQUARE }

    private final Location center;
    private final int radius;
    private final Shape shape;

    public AreaZone(Location center, int radius, Shape shape) {
        this.center = center.clone();
        this.radius = radius;
        this.shape = shape;
    }

    public boolean isInside(Location loc) {
        if (!loc.getWorld().equals(center.getWorld())) return false;

        if (shape == Shape.CIRCULAR) {
            return loc.distanceSquared(center) <= radius * radius;
        }

        // SQUARE
        return Math.abs(loc.getX() - center.getX()) <= radius &&
                Math.abs(loc.getZ() - center.getZ()) <= radius;
    }

    public void debug(Player p) {
        World w = center.getWorld();
        if (shape == Shape.CIRCULAR) {
            for (double angle = 0; angle < 360; angle += 3) {
                double rad = Math.toRadians(angle);
                Location l = center.clone().add(Math.cos(rad)*radius, 0, Math.sin(rad)*radius);
                w.spawnParticle(Particle.DUST, l, 1, new Particle.DustOptions(Color.WHITE, 1));
            }
        } else {
            for (int x=-radius; x<=radius; x++) {
                draw(center.clone().add(x,0,-radius));
                draw(center.clone().add(x,0, radius));
            }
            for (int z=-radius; z<=radius; z++) {
                draw(center.clone().add(-radius,0,z));
                draw(center.clone().add( radius,0,z));
            }
        }
    }

    private void draw(Location loc) {
        loc.getWorld().spawnParticle(
                Particle.END_ROD, loc, 1, 0, 0, 0, 0
        );
    }
}
