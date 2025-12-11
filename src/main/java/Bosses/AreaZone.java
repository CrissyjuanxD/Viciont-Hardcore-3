package Bosses;

import org.bukkit.*;
import org.bukkit.entity.Player;

public class AreaZone {

    public enum Shape { CIRCULAR, SQUARE }

    private final Location center;
    private final int radius;
    private final int heightUp;
    private final int heightDown;
    private final Shape shape;

    public AreaZone(Location center, int radius, int heightUp, int heightDown, Shape shape) {
        this.center = center.clone();
        this.radius = radius;
        this.heightUp = heightUp;
        this.heightDown = heightDown;
        this.shape = shape;
    }

    public boolean isInside(Location loc) {
        if (!loc.getWorld().equals(center.getWorld())) return false;

        // 1. VALIDAR ALTURA (Y)
        double y = loc.getY();
        double centerY = center.getY();

        if (y > centerY + heightUp || y < centerY - heightDown) {
            return false;
        }

/*        if (shape == Shape.CIRCULAR) {
            return loc.distanceSquared(center) <= radius * radius;
        }*/

        if (shape == Shape.CIRCULAR) {
            return Math.pow(loc.getX() - center.getX(), 2) + Math.pow(loc.getZ() - center.getZ(), 2) <= radius * radius;
        }

        // SQUARE
        return Math.abs(loc.getX() - center.getX()) <= radius &&
                Math.abs(loc.getZ() - center.getZ()) <= radius;
    }

    public void debug(Player p) {
        World w = center.getWorld();
        if (w == null) return;

        // Dibujar el Límite SUPERIOR (Techo) - Color AQUA
        drawBorder(w, center.clone().add(0, heightUp, 0), Color.AQUA);

        // Dibujar el Límite INFERIOR (Suelo/Profundidad) - Color ROJO
        drawBorder(w, center.clone().subtract(0, heightDown, 0), Color.RED);

        // Dibujar el CENTRO (Referencia) - Color BLANCO (Opcional, solo una partícula central)
        w.spawnParticle(Particle.END_ROD, center, 1, 0, 0, 0, 0);
    }

    private void drawBorder(World w, Location loc, Color color) {
        Particle.DustOptions dust = new Particle.DustOptions(color, 1.2f);

        if (shape == Shape.CIRCULAR) {
            for (double angle = 0; angle < 360; angle += 5) {
                double rad = Math.toRadians(angle);
                Location l = loc.clone().add(Math.cos(rad)*radius, 0, Math.sin(rad)*radius);
                w.spawnParticle(Particle.DUST, l, 1, dust);
            }
        } else {
            // Cuadrado: Dibujar 4 líneas
            double minX = loc.getX() - radius;
            double maxX = loc.getX() + radius;
            double minZ = loc.getZ() - radius;
            double maxZ = loc.getZ() + radius;
            double y = loc.getY();

            // Lado X
            for (double x = minX; x <= maxX; x += 1.0) {
                w.spawnParticle(Particle.DUST, new Location(w, x, y, minZ), 1, dust);
                w.spawnParticle(Particle.DUST, new Location(w, x, y, maxZ), 1, dust);
            }
            // Lado Z
            for (double z = minZ; z <= maxZ; z += 1.0) {
                w.spawnParticle(Particle.DUST, new Location(w, minX, y, z), 1, dust);
                w.spawnParticle(Particle.DUST, new Location(w, maxX, y, z), 1, dust);
            }
        }
    }

    // Getters útiles para ataques
    public int getHeightUp() { return heightUp; }
    public int getHeightDown() { return heightDown; }
    public Location getCenter() { return center.clone(); }
}
