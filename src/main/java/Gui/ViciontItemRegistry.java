package Gui;

import net.md_5.bungee.api.ChatColor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ViciontItemRegistry {

    private final List<ViciontItem> displayItems = new ArrayList<>();
    private final Map<String, ViciontItem> itemsById = new HashMap<>();

    public ViciontItemRegistry() {
        registerAllItems();
    }

    private void registerItem(ViciontItem item) {
        displayItems.add(item);
        itemsById.put(item.getId(), item);
    }

    // Para ingredientes que no quieres que salgan en la lista paginada de la GUI
    private void registerHiddenItem(ViciontItem item) {
        itemsById.put(item.getId(), item);
    }

    private void registerAllItems() {
        // ==========================================
        // TUS 160 ÍTEMS AQUÍ
        // ==========================================

        ViciontItem ojoCorrupto = new ViciontItem("corrupted_eye", "minecraft:spider_eye", 5, 1, false, null);
        ojoCorrupto.addRawTooltip("[\"\",{\"text\":\"Corrupted Eye\",\"bold\":true,\"color\":\"dark_purple\"},{\"text\":\"\\n\\nEste ojo corrupto se consigue\\nmantando \"},{\"text\":\"Corrupted Spiders\",\"color\":\"#9E3FC9\"},{\"text\":\".\",\"color\":\"white\"},{\"text\":\"\\n\\n\"},{\"text\":\"Tiene probabilidad de ser dropeado.\",\"color\":\"gray\"}]");
        registerItem(ojoCorrupto);

        ViciontItem carnePodridaCorrupto = new ViciontItem("corrupted_rotten_flesh", "minecraft:rotten_flesh", 5, 1, false, null);
        carnePodridaCorrupto.addRawTooltip("[\"\",{\"text\":\"Corrupted Rotten Flesh\",\"bold\":true,\"color\":\"dark_purple\"},{\"text\":\"\\n\\nEsta carne podrida corrupto se\\nconsigue mantando \"},{\"text\":\"Corrupted Zombies\",\"color\":\"#9E3FC9\"},{\"text\":\".\",\"color\":\"white\"},{\"text\":\"\\n\\n\"},{\"text\":\"Tiene probabilidad de ser dropeado.\",\"color\":\"gray\"}]");
        registerItem(carnePodridaCorrupto);

        ViciontItem carneCorrupta = new ViciontItem("corrupted_steak", "minecraft:cooked_beef", 2, 1, true, "view_recipe_corrupted_steak");
        carneCorrupta.addRawTooltip(ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "Carne Corrupta");
        carneCorrupta.addRawTooltip("");
        carneCorrupta.addRawTooltip(ChatColor.of("#ffcc99") + "Esta carne te otorga estos");
        carneCorrupta.addRawTooltip(ChatColor.of("#ffcc99") + "efectos" + ChatColor.GRAY + ":");
        carneCorrupta.addRawTooltip("");
        carneCorrupta.addRawTooltip(ChatColor.GRAY + "> " + ChatColor.of("#99cc33") + "Náuseas 1" + ChatColor.GRAY + " (" + ChatColor.of("#0099cc") + "10 s" + ChatColor.GRAY + ")");
        carneCorrupta.addRawTooltip(ChatColor.GRAY + "> " + ChatColor.of("#cc3300") + "Saturación 1" + ChatColor.GRAY + " (" + ChatColor.of("#0099cc") + "1.5 s" + ChatColor.GRAY + ")");
        carneCorrupta.addRawTooltip("");
        registerItem(carneCorrupta);

        ViciontItem placadiamante = new ViciontItem("diamond_plate", "minecraft:diamond", 1, 1, true, "view_recipe_diamond_plate");
        placadiamante.addRawTooltip("[\"\",{\"text\":\"Placa de Diamante\",\"bold\":true,\"color\":\"#74B6E7\"},{\"text\":\"\\n\\nSirve para poder craftear la\\n\"},{\"text\":\"Mesa de Encantamientos Mejorada\",\"color\":\"#CD3CD3\"},{\"text\":\".\"}]");
        registerItem(placadiamante);

        ViciontItem mesa_encantamientos_mejorada = new ViciontItem("mesa_enc_mejorada", "minecraft:green_glazed_terracotta", 1, 1, true, "view_recipe_mesa_enc_mejorada");
        mesa_encantamientos_mejorada.addRawTooltip(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "Mesa de Encantamientos Mejorada");
        mesa_encantamientos_mejorada.addRawTooltip("");
        mesa_encantamientos_mejorada.addRawTooltip(ChatColor.GRAY + "En esta mesa podrás encantar");
        mesa_encantamientos_mejorada.addRawTooltip(ChatColor.GRAY + "todos los " + ChatColor.GOLD + ChatColor.BOLD + "encantamientos");
        mesa_encantamientos_mejorada.addRawTooltip(ChatColor.GOLD + "" + ChatColor.BOLD + "bloqueados" + ChatColor.GRAY + ".");
        mesa_encantamientos_mejorada.addRawTooltip("");
        mesa_encantamientos_mejorada.addRawTooltip(ChatColor.GRAY + "Usa: ");
        mesa_encantamientos_mejorada.addRawTooltip(ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + ">>" + ChatColor.GRAY + " " + ChatColor.GRAY + ChatColor.BOLD + "4XP" + ChatColor.GRAY + " por encantamiento.");
        mesa_encantamientos_mejorada.addRawTooltip(ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + ">>" + ChatColor.GRAY + " " + ChatColor.GRAY + ChatColor.BOLD + "3 de Lápiz" + ChatColor.GRAY + " por encantamiento.");
        mesa_encantamientos_mejorada.addRawTooltip(ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + ">>" + ChatColor.GRAY + " " + ChatColor.GRAY + ChatColor.BOLD + "1 uso" + ChatColor.GRAY + " de la esencia especial");
        mesa_encantamientos_mejorada.addRawTooltip("");
        registerItem(mesa_encantamientos_mejorada);

        ViciontItem esencia_proteccion = new ViciontItem("ese_prote", "minecraft:iron_nugget", 2, 1, false, null);
        esencia_proteccion.addRawTooltip("[\"\",{\"text\":\"Esencia de Protección\",\"bold\":true,\"color\":\"#BAE1FF\"},{\"text\":\"\\n\\n\"},{\"text\":\"Con esta Esencia podrás desbloquear\",\"color\":\"white\"},{\"text\":\"\\n\"},{\"text\":\"el encantamiento\",\"color\":\"white\"},{\"text\":\" Protección\",\"bold\":true,\"color\":\"#F9E394\"},{\"text\":\" en la\",\"color\":\"white\"},{\"text\":\"\\n\"},{\"text\":\"Mesa de Encantamiento Mejorada\",\"color\":\"#CB5DE7\"},{\"text\":\".\\n\\nSe consigue en:\\n\"},{\"text\":\">\",\"color\":\"white\"},{\"text\":\"Corrupted Towers\",\"color\":\"#E89A30\"},{\"text\":\"\\n>\"},{\"text\":\"Loot vanilla (15% prob).\",\"color\":\"#4EEF92\"}]");
        registerItem(esencia_proteccion);

        ViciontItem esencia_unbreaking = new ViciontItem("ese_unbre", "minecraft:iron_nugget", 3, 1, false, null);
        esencia_unbreaking.addRawTooltip("[\"\",{\"text\":\"Esencia de Irrompibilidad\",\"bold\":true,\"color\":\"#BAE1FF\"},{\"text\":\"\\n\\n\"},{\"text\":\"Con esta Esencia podrás desbloquear\",\"color\":\"white\"},{\"text\":\"\\n\"},{\"text\":\"el encantamiento\",\"color\":\"white\"},{\"text\":\" Irrompibilidad\",\"bold\":true,\"color\":\"#F9E394\"},{\"text\":\" en la\",\"color\":\"white\"},{\"text\":\"\\n\"},{\"text\":\"Mesa de Encantamiento Mejorada\",\"color\":\"#CB5DE7\"},{\"text\":\".\\n\\nSe consigue en:\\n\"},{\"text\":\">\",\"color\":\"white\"},{\"text\":\"Corrupted Towers\",\"color\":\"#E89A30\"},{\"text\":\"\\n>\"},{\"text\":\"Loot vanilla (15% prob).\",\"color\":\"#4EEF92\"}]");
        registerItem(esencia_unbreaking);

        ViciontItem esencia_eficiency = new ViciontItem("ese_efici", "minecraft:iron_nugget", 5, 1, false, null);
        esencia_eficiency.addRawTooltip("[\"\",{\"text\":\"Esencia de Eficiencia\",\"bold\":true,\"color\":\"#BAE1FF\"},{\"text\":\"\\n\\n\"},{\"text\":\"Con esta Esencia podrás desbloquear\",\"color\":\"white\"},{\"text\":\"\\n\"},{\"text\":\"el encantamiento\",\"color\":\"white\"},{\"text\":\" Eficiencia\",\"bold\":true,\"color\":\"#F9E394\"},{\"text\":\" en la\",\"color\":\"white\"},{\"text\":\"\\n\"},{\"text\":\"Mesa de Encantamiento Mejorada\",\"color\":\"#CB5DE7\"},{\"text\":\".\\n\\nSe consigue en:\\n\"},{\"text\":\">\",\"color\":\"white\"},{\"text\":\"Corrupted Towers\",\"color\":\"#E89A30\"},{\"text\":\"\\n>\"},{\"text\":\"Loot vanilla (15% prob).\",\"color\":\"#4EEF92\"}]");
        registerItem(esencia_eficiency);

        ViciontItem esencia_fortune = new ViciontItem("ese_fortu", "minecraft:iron_nugget", 6, 1, false, null);
        esencia_fortune.addRawTooltip("[\"\",{\"text\":\"Esencia de Fortuna\",\"bold\":true,\"color\":\"#BAE1FF\"},{\"text\":\"\\n\\n\"},{\"text\":\"Con esta Esencia podrás desbloquear\",\"color\":\"white\"},{\"text\":\"\\n\"},{\"text\":\"el encantamiento\",\"color\":\"white\"},{\"text\":\" Fortuna\",\"bold\":true,\"color\":\"#F9E394\"},{\"text\":\" en la\",\"color\":\"white\"},{\"text\":\"\\n\"},{\"text\":\"Mesa de Encantamiento Mejorada\",\"color\":\"#CB5DE7\"},{\"text\":\".\\n\\nSe consigue en:\\n\"},{\"text\":\">\",\"color\":\"white\"},{\"text\":\"Corrupted Towers\",\"color\":\"#E89A30\"},{\"text\":\"\\n>\"},{\"text\":\"Loot vanilla (15% prob).\",\"color\":\"#4EEF92\"}]");
        registerItem(esencia_fortune);

        ViciontItem esencia_sharpness = new ViciontItem("ese_sharp", "minecraft:iron_nugget", 7, 1, false, null);
        esencia_sharpness.addRawTooltip("[\"\",{\"text\":\"Esencia de Filo\",\"bold\":true,\"color\":\"#BAE1FF\"},{\"text\":\"\\n\\n\"},{\"text\":\"Con esta Esencia podrás desbloquear\",\"color\":\"white\"},{\"text\":\"\\n\"},{\"text\":\"el encantamiento\",\"color\":\"white\"},{\"text\":\" Filo\",\"bold\":true,\"color\":\"#F9E394\"},{\"text\":\" en la\",\"color\":\"white\"},{\"text\":\"\\n\"},{\"text\":\"Mesa de Encantamiento Mejorada\",\"color\":\"#CB5DE7\"},{\"text\":\".\\n\\nSe consigue en:\\n\"},{\"text\":\">\",\"color\":\"white\"},{\"text\":\"Corrupted Towers\",\"color\":\"#E89A30\"},{\"text\":\"\\n>\"},{\"text\":\"Loot vanilla (15% prob).\",\"color\":\"#4EEF92\"}]");
        registerItem(esencia_sharpness);

        ViciontItem esencia_smite = new ViciontItem("ese_smite", "minecraft:iron_nugget", 8, 1, false, null);
        esencia_smite.addRawTooltip("[\"\",{\"text\":\"Esencia de Castigo\",\"bold\":true,\"color\":\"#BAE1FF\"},{\"text\":\"\\n\\n\"},{\"text\":\"Con esta Esencia podrás desbloquear\",\"color\":\"white\"},{\"text\":\"\\n\"},{\"text\":\"el encantamiento\",\"color\":\"white\"},{\"text\":\" Castigo\",\"bold\":true,\"color\":\"#F9E394\"},{\"text\":\" en la\",\"color\":\"white\"},{\"text\":\"\\n\"},{\"text\":\"Mesa de Encantamiento Mejorada\",\"color\":\"#CB5DE7\"},{\"text\":\".\\n\\nSe consigue en:\\n\"},{\"text\":\">\",\"color\":\"white\"},{\"text\":\"Corrupted Towers\",\"color\":\"#E89A30\"},{\"text\":\"\\n>\"},{\"text\":\"Loot vanilla (15% prob).\",\"color\":\"#4EEF92\"}]");
        registerItem(esencia_smite);

        ViciontItem esencia_baneofart = new ViciontItem("ese_boa", "minecraft:iron_nugget", 9, 1, false, null);
        esencia_baneofart.addRawTooltip("[\"\",{\"text\":\"Esencia de Perdición de los Artrópodos\",\"bold\":true,\"color\":\"#BAE1FF\"},{\"text\":\"\\n\\n\"},{\"text\":\"Con esta Esencia podrás desbloquear\",\"color\":\"white\"},{\"text\":\"\\n\"},{\"text\":\"el encantamiento\",\"color\":\"white\"},{\"text\":\" Perdición de los Artrópodos\",\"bold\":true,\"color\":\"#F9E394\"},{\"text\":\" en la\",\"color\":\"white\"},{\"text\":\"\\n\"},{\"text\":\"Mesa de Encantamiento Mejorada\",\"color\":\"#CB5DE7\"},{\"text\":\".\\n\\nSe consigue en:\\n\"},{\"text\":\">\",\"color\":\"white\"},{\"text\":\"Corrupted Towers\",\"color\":\"#E89A30\"},{\"text\":\"\\n>\"},{\"text\":\"Loot vanilla (15% prob).\",\"color\":\"#4EEF92\"}]");
        registerItem(esencia_baneofart);

        ViciontItem esencia_slowfall = new ViciontItem("ese_slowf", "minecraft:iron_nugget", 10, 1, false, null);
        esencia_slowfall.addRawTooltip("[\"\",{\"text\":\"Esencia de Caída de Pluma\",\"bold\":true,\"color\":\"#BAE1FF\"},{\"text\":\"\\n\\n\"},{\"text\":\"Con esta Esencia podrás desbloquear\",\"color\":\"white\"},{\"text\":\"\\n\"},{\"text\":\"el encantamiento\",\"color\":\"white\"},{\"text\":\" Caída de Pluma\",\"bold\":true,\"color\":\"#F9E394\"},{\"text\":\" en la\",\"color\":\"white\"},{\"text\":\"\\n\"},{\"text\":\"Mesa de Encantamiento Mejorada\",\"color\":\"#CB5DE7\"},{\"text\":\".\\n\\nSe consigue en:\\n\"},{\"text\":\">\",\"color\":\"white\"},{\"text\":\"Corrupted Towers\",\"color\":\"#E89A30\"},{\"text\":\"\\n>\"},{\"text\":\"Loot vanilla (15% prob).\",\"color\":\"#4EEF92\"}]");
        registerItem(esencia_slowfall);

        ViciontItem esencia_looting = new ViciontItem("ese_loot", "minecraft:iron_nugget", 11, 1, false, null);
        esencia_looting.addRawTooltip("[\"\",{\"text\":\"Esencia de Saqueo\",\"bold\":true,\"color\":\"#BAE1FF\"},{\"text\":\"\\n\\n\"},{\"text\":\"Con esta Esencia podrás desbloquear\",\"color\":\"white\"},{\"text\":\"\\n\"},{\"text\":\"el encantamiento\",\"color\":\"white\"},{\"text\":\" Saqueo\",\"bold\":true,\"color\":\"#F9E394\"},{\"text\":\" en la\",\"color\":\"white\"},{\"text\":\"\\n\"},{\"text\":\"Mesa de Encantamiento Mejorada\",\"color\":\"#CB5DE7\"},{\"text\":\".\\n\\nSe consigue en:\\n\"},{\"text\":\">\",\"color\":\"white\"},{\"text\":\"Corrupted Towers\",\"color\":\"#E89A30\"},{\"text\":\"\\n>\"},{\"text\":\"Loot vanilla (15% prob).\",\"color\":\"#4EEF92\"}]");
        registerItem(esencia_looting);

        ViciontItem esencia_depthstrider = new ViciontItem("ese_depth", "minecraft:iron_nugget", 12, 1, false, null);
        esencia_depthstrider.addRawTooltip("[\"\",{\"text\":\"Esencia de Agilidad Acuática\",\"bold\":true,\"color\":\"#BAE1FF\"},{\"text\":\"\\n\\n\"},{\"text\":\"Con esta Esencia podrás desbloquear\",\"color\":\"white\"},{\"text\":\"\\n\"},{\"text\":\"el encantamiento\",\"color\":\"white\"},{\"text\":\" Agilidad Acuática\",\"bold\":true,\"color\":\"#F9E394\"},{\"text\":\" en la\",\"color\":\"white\"},{\"text\":\"\\n\"},{\"text\":\"Mesa de Encantamiento Mejorada\",\"color\":\"#CB5DE7\"},{\"text\":\".\\n\\nSe consigue en:\\n\"},{\"text\":\">\",\"color\":\"white\"},{\"text\":\"Corrupted Towers\",\"color\":\"#E89A30\"},{\"text\":\"\\n>\"},{\"text\":\"Loot vanilla (15% prob).\",\"color\":\"#4EEF92\"}]");
        registerItem(esencia_depthstrider);

        ViciontItem esencia_power = new ViciontItem("ese_power", "minecraft:iron_nugget", 14, 1, false, null);
        esencia_power.addRawTooltip("[\"\",{\"text\":\"Esencia de Poder\",\"bold\":true,\"color\":\"#BAE1FF\"},{\"text\":\"\\n\\n\"},{\"text\":\"Con esta Esencia podrás desbloquear\",\"color\":\"white\"},{\"text\":\"\\n\"},{\"text\":\"el encantamiento\",\"color\":\"white\"},{\"text\":\" Poder\",\"bold\":true,\"color\":\"#F9E394\"},{\"text\":\" en la\",\"color\":\"white\"},{\"text\":\"\\n\"},{\"text\":\"Mesa de Encantamiento Mejorada\",\"color\":\"#CB5DE7\"},{\"text\":\".\\n\\nSe consigue en:\\n\"},{\"text\":\">\",\"color\":\"white\"},{\"text\":\"Corrupted Towers\",\"color\":\"#E89A30\"},{\"text\":\"\\n>\"},{\"text\":\"Loot vanilla (15% prob).\",\"color\":\"#4EEF92\"}]");
        registerItem(esencia_power);

        ViciontItem esencia_vacia = new ViciontItem("ese_vacia", "minecraft:iron_nugget", 20, 1, false, null);
        esencia_vacia.addRawTooltip(ChatColor.of("#bdb2ff") + "Esencia Vacía");
        esencia_vacia.addRawTooltip(" ");
        esencia_vacia.addRawTooltip(ChatColor.of("#c8b6ff") + "Esta esencia vacía");
        esencia_vacia.addRawTooltip(ChatColor.of("#c8b6ff") + "puede llegar a alcanzar");
        esencia_vacia.addRawTooltip(ChatColor.of("#c8b6ff") + "poderes " + ChatColor.of("#ffb3ba") + "inimaginables...");
        esencia_vacia.addRawTooltip("[\"\",{\"text\":\"\\nSe consigue en:\\n\"},{\"text\":\">\",\"color\":\"white\"},{\"text\":\"Corrupted Towers\",\"color\":\"#E89A30\"},{\"text\":\"\\n>\"},{\"text\":\"Loot vanilla (15% prob).\",\"color\":\"#4EEF92\"}]");
        registerItem(esencia_vacia);

        ViciontItem scrapCorrupto = new ViciontItem("corrupted_scrap", "minecraft:netherite_scrap", 5, 2, true, "view_recipe_corrupted_scrap");
        scrapCorrupto.addTooltip("Corrupted Netherite Scrap", "#AA00AA", true);
        registerItem(scrapCorrupto);

        ViciontItem cascoNetherite = new ViciontItem("netherite_helmet", "minecraft:netherite_helmet", 0, 2, true, "view_recipe_netherite_helmet");
        cascoNetherite.addTooltip("Netherite Helmet", "#FFFFFF", false);
        registerItem(cascoNetherite);

        ViciontItem chestplateRunico = new ViciontItem("runic_chestplate", "minecraft:netherite_chestplate", 2, 2, true, "view_recipe_runic_chestplate");
        chestplateRunico.addTooltip("Corrupted Netherite Chestplate", "#9966ff", true);
        registerItem(chestplateRunico);

        ViciontItem debrisCorrupto = new ViciontItem("corrupted_debris", "minecraft:ancient_debris", 5, 2, false, null);
        debrisCorrupto.addTooltip("Corrupted Ancient Debris", "#990066", true);
        registerItem(debrisCorrupto);
    }

    public List<ViciontItem> getDisplayItems() { return displayItems; }

    public ViciontItem getItem(String id) { return itemsById.get(id); }
}