// NMSUtils.java
package Dificultades.CustomMobs;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;

public class NMSUtils {

    public static String getServerVersion() {
        return Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
    }

    public static Class<?> getNMSClass(String className) {
        try {
            return Class.forName("net.minecraft.server." + getServerVersion() + "." + className);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Class<?> getCraftClass(String className) {
        try {
            return Class.forName("org.bukkit.craftbukkit." + getServerVersion() + "." + className);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Object getNMSEntity(Entity bukkitEntity) {
        try {
            return bukkitEntity.getClass().getMethod("getHandle").invoke(bukkitEntity);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}