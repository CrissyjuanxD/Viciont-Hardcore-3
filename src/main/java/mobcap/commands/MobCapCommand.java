package mobcap.commands;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import mobcap.MobCapInfo;
import mobcap.MobCapManager;
import mobcap.MobCapMultiplier;
import mobcap.config.MobCapConfig;
import mobcap.optimization.PerformanceData;
import mobcap.spawn.SpawnMode;

import java.util.Map;

public class MobCapCommand implements CommandExecutor {
    private final MobCapManager mobCapManager;
    private final MobCapConfig config;

    public MobCapCommand(MobCapManager mobCapManager, MobCapConfig config) {
        this.mobCapManager = mobCapManager;
        this.config = config;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("mobcapinfo")) {
            return handleMobCapInfo(sender);
        }

        if (!command.getName().equalsIgnoreCase("mobcap")) {
            return false;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "info":
                return handleMobCapInfo(sender);
            case "setbase":
                return handleSetBaseMobCap(sender, args);
            case "doble":
                return handleSetMultiplier(sender, MobCapMultiplier.DOUBLE);
            case "triple":
                return handleSetMultiplier(sender, MobCapMultiplier.TRIPLE);
            case "normal":
                return handleSetMultiplier(sender, MobCapMultiplier.NORMAL);
            case "habilitar":
            case "enable":
                return handleToggleSystem(sender, true);
            case "deshabilitar":
            case "disable":
                return handleToggleSystem(sender, false);
            case "reset":
                return handleResetMobCap(sender);
            case "reload":
                return handleReload(sender);
            case "mob":
                return handleMobConfig(sender, args);
            default:
                sendHelp(sender);
                return true;
        }
    }

    private boolean handleMobCapInfo(CommandSender sender) {
        MobCapInfo info = mobCapManager.getMobCapInfo();
        PerformanceData perf = info.getPerformanceData();

        sender.sendMessage("§e§l=== MobCap Information ===");
        sender.sendMessage("§7Estado: " + (info.isEnabled() ? "§aHabilitado" : "§cDeshabilitado"));
        sender.sendMessage("§7MobCap Base: §a" + info.getBaseMobCap());
        sender.sendMessage("§7Multiplicador: §b" + info.getMultiplier().getDisplayName() + " §7(x" + info.getMultiplier().getMultiplier() + ")");
        sender.sendMessage("§7MobCap Efectivo: §b" + info.getEffectiveMobCap());
        sender.sendMessage("§7Jugadores Online: §e" + info.getPlayerCount());
        sender.sendMessage("§7Optimización Activa: " + (info.isOptimizationActive() ? "§aActiva" : "§cInactiva"));
        sender.sendMessage("");
        sender.sendMessage("§e§lRendimiento del Servidor:");
        sender.sendMessage("§7TPS: " + perf.getFormattedTps());
        sender.sendMessage("§7Memoria: " + perf.getFormattedMemory());
        sender.sendMessage("§7CPU: " + perf.getFormattedCpuUsage());
        sender.sendMessage("");
        sender.sendMessage("§e§lDetalles por Mundo:");

        for (World world : Bukkit.getWorlds()) {
            String worldName = world.getName();
            int original = info.getOriginalLimits().getOrDefault(worldName, -1);
            int current = info.getCurrentLimits().getOrDefault(worldName, -1);

            sender.sendMessage(String.format("§7%s: §f%d §7(original: §8%d§7)",
                    worldName, current, original));
        }

        return true;
    }

    private boolean handleSetBaseMobCap(CommandSender sender, String[] args) {
        if (!sender.hasPermission("mobcap.admin")) {
            sender.sendMessage("§cNo tienes permiso para usar este comando.");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage("§cUso: /mobcap setbase <número>");
            return true;
        }

        try {
            int mobCap = Integer.parseInt(args[1]);
            if (mobCap < 1 || mobCap > 500) {
                sender.sendMessage("§cEl valor debe estar entre 1 y 500.");
                return true;
            }

            mobCapManager.setBaseMobCap(mobCap);
            sender.sendMessage("§aMobCap base establecido a: §b" + mobCap);

            if (sender instanceof Player) {
                Bukkit.getLogger().info(sender.getName() + " changed base mob cap to: " + mobCap);
            }

        } catch (NumberFormatException e) {
            sender.sendMessage("§cDebes introducir un número válido.");
        }

        return true;
    }

    private boolean handleSetMultiplier(CommandSender sender, MobCapMultiplier multiplier) {
        if (!sender.hasPermission("mobcap.admin")) {
            sender.sendMessage("§cNo tienes permiso para usar este comando.");
            return true;
        }

        mobCapManager.setMultiplier(multiplier);
        sender.sendMessage("§eMobCap " + multiplier.getDisplayName() + " activado.");

        if (sender instanceof Player) {
            Bukkit.getLogger().info(sender.getName() + " set mob cap multiplier to: " + multiplier.getDisplayName());
        }

        return true;
    }

    private boolean handleToggleSystem(CommandSender sender, boolean enable) {
        if (!sender.hasPermission("mobcap.admin")) {
            sender.sendMessage("§cNo tienes permiso para usar este comando.");
            return true;
        }

        mobCapManager.setEnabled(enable);
        String status = enable ? "habilitado" : "deshabilitado";
        sender.sendMessage("§eSistema de MobCap " + status + ".");

        if (sender instanceof Player) {
            Bukkit.getLogger().info(sender.getName() + " " + (enable ? "enabled" : "disabled") + " mob cap system");
        }

        return true;
    }

    private boolean handleResetMobCap(CommandSender sender) {
        if (!sender.hasPermission("mobcap.admin")) {
            sender.sendMessage("§cNo tienes permiso para usar este comando.");
            return true;
        }

        mobCapManager.resetMobCap();
        sender.sendMessage("§aMobCap restablecido a valores originales.");

        if (sender instanceof Player) {
            Bukkit.getLogger().info(sender.getName() + " reset mob cap to original values");
        }

        return true;
    }

    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("mobcap.admin")) {
            sender.sendMessage("§cNo tienes permiso para usar este comando.");
            return true;
        }

        mobCapManager.reload();
        sender.sendMessage("§aMobCap recargado correctamente.");

        return true;
    }

    private boolean handleMobConfig(CommandSender sender, String[] args) {
        if (!sender.hasPermission("mobcap.admin")) {
            sender.sendMessage("§cNo tienes permiso para usar este comando.");
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage("§cUso: /mobcap mob <tipo> <vanilla/custom>");
            sender.sendMessage("§eEjemplo: /mobcap mob zombie custom");
            return true;
        }

        try {
            EntityType entityType = EntityType.valueOf(args[1].toUpperCase());
            SpawnMode mode = SpawnMode.fromString(args[2]);

            config.setMobSpawnMode(entityType, mode);
            sender.sendMessage("§aMob §b" + entityType.name() + " §aconfigurado como: §e" + mode.getDescription());

        } catch (IllegalArgumentException e) {
            sender.sendMessage("§cTipo de mob inválido: " + args[1]);
        }

        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§e§l=== MobCap Commands ===");
        sender.sendMessage("§7/mobcapinfo §f- Muestra información detallada del MobCap");
        sender.sendMessage("§7/mobcap info §f- Alias de mobcapinfo");

        if (sender.hasPermission("mobcap.admin")) {
            sender.sendMessage("§7/mobcap setbase <número> §f- Establece el MobCap base");
            sender.sendMessage("§7/mobcap doble §f- Duplica la MobCap");
            sender.sendMessage("§7/mobcap triple §f- Triplica la MobCap");
            sender.sendMessage("§7/mobcap normal §f- MobCap normal (x1)");
            sender.sendMessage("§7/mobcap habilitar §f- Habilita el sistema");
            sender.sendMessage("§7/mobcap deshabilitar §f- Deshabilita el sistema");
            sender.sendMessage("§7/mobcap reset §f- Restablece valores originales");
            sender.sendMessage("§7/mobcap reload §f- Recarga la configuración");
            sender.sendMessage("§7/mobcap mob <tipo> <vanilla/custom> §f- Configura spawn de mob");
        }
    }
}