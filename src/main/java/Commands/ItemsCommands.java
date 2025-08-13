package Commands;

import Armors.CopperArmor;
import Armors.CorruptedArmor;
import Blocks.CorruptedAncientDebris;
import Blocks.Endstalactitas;
import Blocks.GuardianShulkerHeart;
import Dificultades.CustomMobs.CustomBoat;
import Dificultades.CustomMobs.QueenBeeHandler;
import Dificultades.DayFourChanges;
import Dificultades.DayOneChanges;
import Enchants.EnhancedEnchantmentTable;
import Events.UltraWitherBattle.UltraWitherCompass;
import items.*;
import Armors.NightVisionHelmet;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import vct.hardcore3.ViciontHardcore3;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ItemsCommands implements CommandExecutor, TabCompleter {

    private final ViciontHardcore3 plugin;
    private final DoubleLifeTotem doubleLifeTotem;
    private final LifeTotem lifeTotem;
    private final SpiderTotem spiderTotem;
    private final InfernalTotem infernalTotem;
    private final BootNetheriteEssence bootNetheriteEssence;
    private final LegginsNetheriteEssence legginsNetheriteEssence;
    private final ChestplateNetheriteEssence chestplateNetheriteEssence;
    private final HelmetNetheriteEssence helmetNetheriteEssence;
    private final CorruptedUpgrades corruptedUpgrades;
    private final CorruptedSoul corruptedSoul;
    private final CorruptedAncientDebris corruptedAncientDebris;
    private final GuardianShulkerHeart guardianShulkerHeart;
    private final CustomBoat customBoat;

    public ItemsCommands(ViciontHardcore3 plugin) {
        this.plugin = plugin;
        this.doubleLifeTotem = new DoubleLifeTotem(plugin);
        this.lifeTotem = new LifeTotem(plugin);
        this.spiderTotem = new SpiderTotem(plugin);
        this.infernalTotem = new InfernalTotem(plugin);
        this.bootNetheriteEssence = new BootNetheriteEssence(plugin);
        this.legginsNetheriteEssence = new LegginsNetheriteEssence(plugin);
        this.chestplateNetheriteEssence = new ChestplateNetheriteEssence(plugin);
        this.helmetNetheriteEssence = new HelmetNetheriteEssence(plugin);
        this.corruptedUpgrades = new CorruptedUpgrades(plugin);
        this.corruptedSoul = new CorruptedSoul(plugin);
        this.corruptedAncientDebris = new CorruptedAncientDebris(plugin);
        this.guardianShulkerHeart = new GuardianShulkerHeart(plugin);
        this.customBoat = new CustomBoat(plugin);
        plugin.getCommand("givevct").setExecutor(this);
        plugin.getCommand("givevct").setTabCompleter(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§cUso: /givevct <item> [cantidad] [jugador]");
            return true;
        }

        String itemName = args[0].toLowerCase();
        int cantidad = 1;
        Player target = null;

        if (args.length > 1) {
            try {
                cantidad = Integer.parseInt(args[1]);
                if (cantidad <= 0) {
                    sender.sendMessage("§cLa cantidad debe ser mayor a 0.");
                    return true;
                }
            } catch (NumberFormatException e) {
                target = Bukkit.getPlayerExact(args[1]);
                if (target == null) {
                    sender.sendMessage("§cEl jugador '" + args[1] + "' no está en línea.");
                    return true;
                }
            }
        }

        if (args.length > 2) {
            target = Bukkit.getPlayerExact(args[2]);
            if (target == null) {
                sender.sendMessage("§cEl jugador '" + args[2] + "' no está en línea.");
                return true;
            }
        }

        if (target == null) {
            if (sender instanceof Player) {
                target = (Player) sender;
            } else {
                sender.sendMessage("§cDebes especificar un jugador si ejecutas el comando desde la consola.");
                return true;
            }
        }

        ItemStack item;
        switch (itemName) {
            case "doubletotem":
                item = doubleLifeTotem.createDoubleLifeTotem();
                item.setAmount(cantidad);
                break;
            case "lifetotem":
                item = lifeTotem.createLifeTotem();
                item.setAmount(cantidad);
                break;
            case "spidertotem":
                item = spiderTotem.createSpiderTotem();
                item.setAmount(cantidad);
                break;
            case "infernaltotem":
                item = infernalTotem.createInfernalTotem();
                item.setAmount(cantidad);
                break;
            case "aguijon_abeja_reina":
                item = QueenBeeHandler.createAguijonAbejaReina();
                item.setAmount(cantidad);
                break;
            case "upgrade_vacio":
                item = UpgradeNTItems.createUpgradeVacio();
                item.setAmount(cantidad);
                break;
            case "fragmento_upgrade":
                item = UpgradeNTItems.createFragmentoUpgrade();
                item.setAmount(cantidad);
                break;
            case "duplicador":
                item = UpgradeNTItems.createDuplicador();
                item.setAmount(cantidad);
                break;
            case "fragmento_infernal":
                item = EmblemItems.createFragmentoInfernal();
                item.setAmount(cantidad);
                break;
            case "pepita_infernal":
                item = EmblemItems.createPepitaInfernal();
                item.setAmount(cantidad);
                break;
            case "corrupted_nether_star":
                item = EmblemItems.createcorruptedNetherStar();
                item.setAmount(cantidad);
                break;
            case "nether_emblem":
                item = EmblemItems.createNetherEmblem();
                item.setAmount(cantidad);
                break;
            case "overworld_emblem":
                item = EmblemItems.createOverworldEmblem();
                item.setAmount(cantidad);
                break;
            case "end_relic":
                item = EmblemItems.createEndEmblem();
                item.setAmount(cantidad);
                break;
            case "corrupted_steak":
                item = DayOneChanges.corruptedSteak();
                item.setAmount(cantidad);
                break;
            case "placa_diamante":
                item = EnhancedEnchantmentTable.createDiamondPlate();
                item.setAmount(cantidad);
                break;
            case "mesa_encantamientos_mejorada":
                item = EnhancedEnchantmentTable.createEnhancedEnchantmentTable();
                item.setAmount(cantidad);
                break;
            case "casco_night_vision":
                item = NightVisionHelmet.createNightVisionHelmet();
                item.setAmount(cantidad);
                break;
            case "corrupted_helmet_armor":
                item = CorruptedArmor.createCorruptedHelmet();
                item.setAmount(cantidad);
                break;
            case "corrupted_chestplate_armor":
                item = CorruptedArmor.createCorruptedChestplate();
                item.setAmount(cantidad);
                break;
            case "corrupted_leggings_armor":
                item = CorruptedArmor.createCorruptedLeggings();
                item.setAmount(cantidad);
                break;
            case "corrupted_boots_armor":
                item = CorruptedArmor.createCorruptedBoots();
                item.setAmount(cantidad);
                break;
            case "enderite_sword":
                item = EnderiteTools.createEnderiteSword();
                item.setAmount(cantidad);
                break;
            case "enderite_axe":
                item = EnderiteTools.createEnderiteAxe();
                item.setAmount(cantidad);
                break;
            case "enderite_pickaxe":
                item = EnderiteTools.createEnderitePickaxe();
                item.setAmount(cantidad);
                break;
            case "enderite_shovel":
                item = EnderiteTools.createEnderiteShovel();
                item.setAmount(cantidad);
                break;
            case "enderite_hoe":
                item = EnderiteTools.createEnderiteHoe();
                item.setAmount(cantidad);
                break;
            case "leggins_netherite_essence":
                item = legginsNetheriteEssence.createLegginsNetheriteEssence();
                item.setAmount(cantidad);
                break;
            case "boot_netherite_essence":
                item = bootNetheriteEssence.createBootNetheriteEssence();
                item.setAmount(cantidad);
                break;
            case "chestplate_netherite_essence":
                item = chestplateNetheriteEssence.createChestplateNetheriteEssence();
                item.setAmount(cantidad);
                break;
            case "helmet_netherite_essence":
                item = helmetNetheriteEssence.createHelmetNetheriteEssence();
                item.setAmount(cantidad);
                break;
            case "helmet_netherite_upgrade":
                item = corruptedUpgrades.createHelmetNetheriteUpgrade();
                item.setAmount(cantidad);
                break;
            case "chestplate_netherite_upgrade":
                item = corruptedUpgrades.createChestplateNetheriteUpgrade();
                item.setAmount(cantidad);
                break;
            case "leggins_netherite_upgrade":
                item = corruptedUpgrades.createLeggingsNetheriteUpgrade();
                item.setAmount(cantidad);
                break;
            case "boot_netherite_upgrade":
                item = corruptedUpgrades.createBootsNetheriteUpgrade();
                item.setAmount(cantidad);
                break;
            case "cooper_helmet":
                item = CopperArmor.createCopperHelmet();
                item.setAmount(cantidad);
                break;
            case "cooper_chestplate":
                item = CopperArmor.createCopperChestplate();
                item.setAmount(cantidad);
                break;
            case "cooper_leggings":
                item = CopperArmor.createCopperLeggings();
                item.setAmount(cantidad);
                break;
            case "cooper_boots":
                item = CopperArmor.createCopperBoots();
                item.setAmount(cantidad);
                break;
            case "corrupted_netherite_scrap":
                item = CorruptedNetheriteItems.createCorruptedScrapNetherite();
                item.setAmount(cantidad);
                break;
            case "corrupted_netherite_ingot":
                item = CorruptedNetheriteItems.createCorruptedNetheriteIngot();
                item.setAmount(cantidad);
                break;
            case "corrupted_powder":
                item = CorruptedMobItems.createCorruptedPowder();
                item.setAmount(cantidad);
                break;
            case "corrupted_bone_lime":
                item = CorruptedMobItems.createCorruptedBone(CorruptedMobItems.BoneVariant.LIME);
                item.setAmount(cantidad);
                break;
            case "corrupted_bone_green":
                item = CorruptedMobItems.createCorruptedBone(CorruptedMobItems.BoneVariant.GREEN);
                item.setAmount(cantidad);
                break;
            case "corrupted_bone_yellow":
                item = CorruptedMobItems.createCorruptedBone(CorruptedMobItems.BoneVariant.YELLOW);
                item.setAmount(cantidad);
                break;
            case "corrupted_bone_orange":
                item = CorruptedMobItems.createCorruptedBone(CorruptedMobItems.BoneVariant.ORANGE);
                item.setAmount(cantidad);
                break;
            case "corrupted_bone_red":
                item = CorruptedMobItems.createCorruptedBone(CorruptedMobItems.BoneVariant.RED);
                item.setAmount(cantidad);
                break;
            case "corrupted_rotten":
                item = CorruptedMobItems.createCorruptedMeet();
                item.setAmount(cantidad);
                break;
            case "corrupted_spidereyes":
                item = CorruptedMobItems.createCorruptedSpiderEye();
                item.setAmount(cantidad);
                break;
            case "corrupted_soul":
                item = corruptedSoul.createCorruptedSoulEssence();
                item.setAmount(cantidad);
                break;

                //BLOQUES
            case "corrupted_ancient_debris":
                item = corruptedAncientDebris.createcorruptedancientdebris();
                item.setAmount(cantidad);
                break;
            case "guardian_shulker_heart":
                item = guardianShulkerHeart.createGuardianShulkerHeart();
                item.setAmount(cantidad);
                break;
            case "endstalactitas":
                item = Endstalactitas.createEndstalactita();
                item.setAmount(cantidad);
                break;

                //VARIOS
            case "ultracorruptedspidereye":
                item = ItemsTotems.createUltraCorruptedSpiderEye();
                item.setAmount(cantidad);
                break;
            case "infernalcreeperpowder":
                item = ItemsTotems.createInfernalCreeperPowder();
                item.setAmount(cantidad);
                break;
            case "whiteenderpearl":
                item = ItemsTotems.createWhiteEnderPearl();
                item.setAmount(cantidad);
                break;
            case "specialtotem":
                item = ItemsTotems.createSpecialTotem();
                item.setAmount(cantidad);
                break;
            case "customboat":
                item = customBoat.createBoatItem(target);
                item.setAmount(cantidad);
                break;
            case "fuel":
                item = customBoat.createFuelItem();
                item.setAmount(cantidad);
                break;
            case "varita_guardian_blaze":
                item = BlazeItems.createBlazeRod();
                item.setAmount(cantidad);
                break;
            case "polvo_guardian_blaze":
                item = BlazeItems.createGuardianBlazePowder();
                item.setAmount(cantidad);
                break;
            case "ultra_pocion_resistencia_fuego":
                item = BlazeItems.createPotionOfFireResistance();
                item.setAmount(cantidad);
                break;
            case "guardian_shulker_shell":
                item = EndItems.createGuardianShulkerShell();
                item.setAmount(cantidad);
                break;
            case "enderite_nugget":
                item = EndItems.createEnderiteNugget(cantidad);
                item.setAmount(cantidad);
                break;
            case "enderite_fragment":
                item = EndItems.createFragmentoEnderite();
                item.setAmount(cantidad);
                break;
            case "end_amatist":
                item = EndItems.createEndAmatist(cantidad);
                item.setAmount(cantidad);
                break;
            case "enderite_ingot":
                item = EndItems.createIngotEnderite();
                item.setAmount(cantidad);
                break;
            case "enderite_upgrades":
                item = EndItems.createEnderiteUpgrades();
                item.setAmount(cantidad);
                break;

                //Economy Items
            case "vithiums":
                item = EconomyItems.createVithiumCoin();
                item.setAmount(cantidad);
                break;
            case "vithiums_fichas":
                item = EconomyItems.createVithiumToken();
                item.setAmount(cantidad);
                break;
            case "mochila":
                item = EconomyItems.createNormalMochila();
                item.setAmount(cantidad);
                break;
            case "mochila_verde":
                item = EconomyItems.createGreenMochila();
                item.setAmount(cantidad);
                break;
            case "mochila_roja":
                item = EconomyItems.createRedMochila();
                item.setAmount(cantidad);
                break;
            case "mochila_azul":
                item = EconomyItems.createBlueMochila();
                item.setAmount(cantidad);
                break;
            case "mochila_morada":
                item = EconomyItems.createPurpleMochila();
                item.setAmount(cantidad);
                break;
            case "mochila_negra":
                item = EconomyItems.createBlackMochila();
                item.setAmount(cantidad);
                break;
            case "mochila_blanca":
                item = EconomyItems.createWhiteMochila();
                item.setAmount(cantidad);
                break;
            case "mochila_amarilla":
                item = EconomyItems.createYellowMochila();
                item.setAmount(cantidad);
                break;
            case "enderbag":
                item = EconomyItems.createEnderBag();
                item.setAmount(cantidad);
                break;
            case "gancho":
                item = EconomyItems.createGancho();
                item.setAmount(cantidad);
                break;
            case "panic_apple":
                item = EconomyItems.createManzanaPanico();
                item.setAmount(cantidad);
                break;
            case "yunque_nivel_1":
                item = EconomyItems.createYunqueReparadorNivel1();
                item.setAmount(cantidad);
                break;
            case "yunque_nivel_2":
                item = EconomyItems.createYunqueReparadorNivel2();
                item.setAmount(cantidad);
                break;

                //Otros Items
            case "corrupted_golden_apple":
                item = CorruptedGoldenApple.createCorruptedGoldenApple();
                item.setAmount(cantidad);
                break;
            case "apilate_gold_block":
                item = CorruptedGoldenApple.createApilateGoldBlock();
                item.setAmount(cantidad);
                break;
            case "orbe_de_vida":
                item = ReviveItems.createResurrectOrb();
                item.setAmount(cantidad);
                break;
            case "wither_compass":
                item = UltraWitherCompass.createUltraWitherCompass();
                item.setAmount(cantidad);
                break;
            default:
                sender.sendMessage("§cEse item no existe.");
                return true;
        }

        target.getInventory().addItem(item);
        sender.sendMessage("§aHas dado " + cantidad + "x " + itemName + " a " + target.getName() + ".");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.add("doubletotem");
            completions.add("lifetotem");
            completions.add("spidertotem");
            completions.add("infernaltotem");
            completions.add("aguijon_abeja_reina");
            completions.add("upgrade_vacio");
            completions.add("fragmento_upgrade");
            completions.add("duplicador");
            completions.add("fragmento_infernal");
            completions.add("pepita_infernal");
            completions.add("corrupted_nether_star");
            completions.add("nether_emblem");
            completions.add("overworld_emblem");
            completions.add("end_relic");
            completions.add("corrupted_steak");
            completions.add("placa_diamante");
            completions.add("mesa_encantamientos_mejorada");
            completions.add("casco_night_vision");
            completions.add("corrupted_helmet_armor");
            completions.add("corrupted_chestplate_armor");
            completions.add("corrupted_leggings_armor");
            completions.add("corrupted_boots_armor");
            completions.add("enderite_sword");
            completions.add("enderite_axe");
            completions.add("enderite_pickaxe");
            completions.add("enderite_shovel");
            completions.add("enderite_hoe");
            completions.add("helmet_netherite_essence");
            completions.add("chestplate_netherite_essence");
            completions.add("leggins_netherite_essence");
            completions.add("boot_netherite_essence");
            completions.add("helmet_netherite_upgrade");
            completions.add("chestplate_netherite_upgrade");
            completions.add("leggins_netherite_upgrade");
            completions.add("boot_netherite_upgrade");
            completions.add("cooper_helmet");
            completions.add("cooper_chestplate");
            completions.add("cooper_leggings");
            completions.add("cooper_boots");
            completions.add("corrupted_netherite_scrap");
            completions.add("corrupted_netherite_ingot");
            completions.add("corrupted_powder");
            completions.add("corrupted_bone_lime");
            completions.add("corrupted_bone_green");
            completions.add("corrupted_bone_yellow");
            completions.add("corrupted_bone_orange");
            completions.add("corrupted_bone_red");
            completions.add("corrupted_rotten");
            completions.add("corrupted_spidereyes");
            completions.add("corrupted_soul");
            completions.add("corrupted_ancient_debris");
            completions.add("ultracorruptedspidereye");
            completions.add("infernalcreeperpowder");
            completions.add("whiteenderpearl");
            completions.add("specialtotem");
            completions.add("customboat");
            completions.add("fuel");
            completions.add("varita_guardian_blaze");
            completions.add("polvo_guardian_blaze");
            completions.add("ultra_pocion_resistencia_fuego");
            completions.add("guardian_shulker_shell");
            completions.add("enderite_nugget");
            completions.add("enderite_fragment");
            completions.add("end_amatist");
            completions.add("endstalactitas");
            completions.add("enderite_ingot");
            completions.add("enderite_upgrades");
            completions.add("vithiums");
            completions.add("vithiums_fichas");
            completions.add("mochila");
            completions.add("mochila_verde");
            completions.add("mochila_roja");
            completions.add("mochila_azul");
            completions.add("mochila_morada");
            completions.add("mochila_negra");
            completions.add("mochila_blanca");
            completions.add("mochila_amarilla");
            completions.add("enderbag");
            completions.add("gancho");
            completions.add("panic_apple");
            completions.add("yunque_nivel_1");
            completions.add("yunque_nivel_2");
            completions.add("corrupted_golden_apple");
            completions.add("apilate_gold_block");
            completions.add("orbe_de_vida");
            completions.add("wither_compass");
        } else if (args.length == 2) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                completions.add(player.getName());
            }
        } else if (args.length == 3) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[2].toLowerCase()))
                    .collect(Collectors.toList());
        }

        return completions;
    }
}
