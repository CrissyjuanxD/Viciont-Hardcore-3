package Handlers.Teams;

import net.md_5.bungee.api.ChatColor;

public enum TeamType {

    ADMIN("Admin", "#F6763A",
            "\uEB87 ", "\uEB87 ",
            org.bukkit.ChatColor.GOLD, "01_Admin"),

    MOD("Mod", "#00BFFF",
            "\uEB88 ", "\uEB88 ",
            org.bukkit.ChatColor.DARK_AQUA, "02_Mod"),

    T_HELPER("THelper", "#67E590",
            "\uEB89 ", "\uEB89 ",
            org.bukkit.ChatColor.GREEN, "03_Helper"),

    T_SURVIVOR("TSurvivor", "#9455ED",
            "\uEB8A ", "\uEB8F ",
            org.bukkit.ChatColor.LIGHT_PURPLE, "04_TSurvivor"),

    Z_MIEMBRO("ZMiembro", "#F4C657",
            "\uEB8B ", "\uEB90 ",
            org.bukkit.ChatColor.YELLOW, "99_Miembro"),

    LAVACLASH("LavaClash", "#FFD294",
            ChatColor.GRAY + "" + ChatColor.BOLD + "[" + ChatColor.GOLD + ChatColor.BOLD + "Lava" + ChatColor.YELLOW + ChatColor.BOLD + "Clash" + ChatColor.GRAY + ChatColor.BOLD + "] ",
            ChatColor.GRAY + "" + ChatColor.BOLD + "[" + ChatColor.GOLD + ChatColor.BOLD + "Lv" + ChatColor.YELLOW + ChatColor.BOLD + "C" + ChatColor.GRAY + ChatColor.BOLD + "] ",
            org.bukkit.ChatColor.DARK_AQUA, "05_LavaClash"),

    ITEMPARTY("Itemparty", "#C056E6",
            ChatColor.GRAY + "" + ChatColor.BOLD + "[" + ChatColor.DARK_AQUA + ChatColor.BOLD + "ITEM" + ChatColor.LIGHT_PURPLE + ChatColor.BOLD + "PARTY" + ChatColor.GRAY + ChatColor.BOLD + "] ",
            ChatColor.GRAY + "" + ChatColor.BOLD + "[" + ChatColor.DARK_AQUA + ChatColor.BOLD + "I" + ChatColor.LIGHT_PURPLE + ChatColor.BOLD + "PT" + ChatColor.GRAY + ChatColor.BOLD + "] ",
            org.bukkit.ChatColor.DARK_PURPLE, "06_Itemparty"),

    HOTPOTATO("HotPotato", "#FCA37D",
            ChatColor.GRAY + "" + ChatColor.BOLD + "[" + ChatColor.DARK_RED + ChatColor.BOLD + "HOT" + ChatColor.RED + ChatColor.BOLD + "POTATO" + ChatColor.GRAY + ChatColor.BOLD + "] ",
            ChatColor.GRAY + "" + ChatColor.BOLD + "[" + ChatColor.DARK_RED + ChatColor.BOLD + "H" + ChatColor.RED + ChatColor.BOLD + "PO" + ChatColor.GRAY + ChatColor.BOLD + "] ",
            org.bukkit.ChatColor.GOLD, "07_HotPotato"),

    Z_FANTASMA("ZFantasma", "#555555",
            "\uEB8C ", "\uEB91 ",
            org.bukkit.ChatColor.DARK_GRAY, "99_Fantasma");

    private final String id;
    private final String hexColor;
    private final String chatPrefix;
    private final String tabPrefix;
    private final org.bukkit.ChatColor bukkitColor;
    private final String priority;

    TeamType(String id, String hexColor, String chatPrefix, String tabPrefix, org.bukkit.ChatColor bukkitColor, String priority) {
        this.id = id;
        this.hexColor = hexColor;
        this.chatPrefix = chatPrefix;
        this.tabPrefix = tabPrefix;
        this.bukkitColor = bukkitColor;
        this.priority = priority;
    }

    public String getId() { return id; }
    public ChatColor getBungeeColor() { return ChatColor.of(hexColor); }
    public String getChatPrefix() { return chatPrefix; }
    public String getTabPrefix() { return tabPrefix; }
    public org.bukkit.ChatColor getBukkitColor() { return bukkitColor; }
    public String getPriority() { return priority; }

    public static TeamType getById(String id) {
        for (TeamType type : values()) {
            if (type.getId().equalsIgnoreCase(id)) return type;
        }
        return null;
    }
}