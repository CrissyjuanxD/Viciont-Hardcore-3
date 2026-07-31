package Managers;

import Armors.CopperArmor;
import Armors.CorruptedArmor;
import Armors.NightVisionHelmet;
import Blocks.CorruptedAncientDebris;
import Blocks.Endstalactitas;
import Blocks.GuardianShulkerHeart;
import Dificultades.CustomMobs.CustomBoat;
import Dificultades.DayOneChanges;
import Enchants.EnhancedEnchantmentTable;
import Enchants.EssenceFactory;
import Events.UltraWitherBattle.UltraWitherCompass;
import Habilidades.HabilidadesBook;
import items.*;
import items.Flashlight.FlashlightItem;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import vct.hardcore3.ViciontHardcore3;

import java.util.ArrayList;
import java.util.List;

public class ItemManager {

    private final ViciontHardcore3 plugin;

    private final DoubleLifeTotem doubleLifeTotem;
    private final LifeTotem lifeTotem;
    private final SpiderTotem spiderTotem;
    private final InfernalTotem infernalTotem;
    private final EconomyIceTotem economyIceTotem;
    private final EconomyFlyTotem economyFlyTotem;
    private final BootNetheriteEssence bootNetheriteEssence;
    private final LegginsNetheriteEssence legginsNetheriteEssence;
    private final ChestplateNetheriteEssence chestplateNetheriteEssence;
    private final HelmetNetheriteEssence helmetNetheriteEssence;
    private final CorruptedUpgrades corruptedUpgrades;
    private final CorruptedSoul corruptedSoul;
    private final CorruptedAncientDebris corruptedAncientDebris;
    private final GuardianShulkerHeart guardianShulkerHeart;
    private final CustomBoat customBoat;
    private final TridenteEspectral tridenteEspectral;
    private final FlashlightItem flashlightItem;
    private final ItemsEventos itemsEventos;

    private final List<String> registeredItems;

    public ItemManager(ViciontHardcore3 plugin) {
        this.plugin = plugin;
        this.doubleLifeTotem = new DoubleLifeTotem(plugin);
        this.lifeTotem = new LifeTotem(plugin);
        this.spiderTotem = new SpiderTotem(plugin);
        this.infernalTotem = new InfernalTotem(plugin);
        this.economyIceTotem = new EconomyIceTotem(plugin);
        this.economyFlyTotem = new EconomyFlyTotem(plugin);
        this.bootNetheriteEssence = new BootNetheriteEssence(plugin);
        this.legginsNetheriteEssence = new LegginsNetheriteEssence(plugin);
        this.chestplateNetheriteEssence = new ChestplateNetheriteEssence(plugin);
        this.helmetNetheriteEssence = new HelmetNetheriteEssence(plugin);
        this.corruptedUpgrades = new CorruptedUpgrades(plugin);
        this.corruptedSoul = new CorruptedSoul(plugin);
        this.corruptedAncientDebris = new CorruptedAncientDebris(plugin);
        this.guardianShulkerHeart = new GuardianShulkerHeart(plugin);
        this.customBoat = new CustomBoat(plugin);
        this.tridenteEspectral = new TridenteEspectral(plugin);
        this.flashlightItem = new FlashlightItem(plugin);
        this.itemsEventos = new ItemsEventos(plugin);

        this.registeredItems = new ArrayList<>();
        cargarNombresDeItems();
    }

    private void cargarNombresDeItems() {
        String[] items = {
                "doubletotem", "lifetotem", "spidertotem", "infernaltotem", "aguijon_real", "upgrade_vacio",
                "fragmento_upgrade", "duplicador", "fragmento_infernal", "pepita_infernal", "corrupted_nether_star",
                "nether_emblem", "overworld_emblem", "end_relic", "corrupted_steak", "placa_diamante",
                "mesa_encantamientos_mejorada", "casco_night_vision", "corrupted_helmet_armor",
                "corrupted_chestplate_armor", "corrupted_leggings_armor", "corrupted_boots_armor",
                "enderite_sword", "enderite_axe", "enderite_pickaxe", "enderite_shovel", "enderite_hoe",
                "leggins_netherite_essence", "boot_netherite_essence", "chestplate_netherite_essence",
                "helmet_netherite_essence", "helmet_netherite_upgrade", "chestplate_netherite_upgrade",
                "leggins_netherite_upgrade", "boot_netherite_upgrade", "cooper_helmet", "cooper_chestplate",
                "cooper_leggings", "cooper_boots", "corrupted_netherite_scrap", "corrupted_netherite_ingot",
                "corrupted_powder", "corrupted_bone_lime", "corrupted_bone_green", "corrupted_bone_yellow",
                "corrupted_bone_orange", "corrupted_bone_red", "corrupted_rotten", "corrupted_spidereyes",
                "corrupted_soul", "corrupted_ancient_debris", "guardian_shulker_heart", "endstalactitas",
                "toxicspidereye", "infernalcreeperpowder", "whiteenderpearl", "specialtotem", "customboat",
                "fuel", "varita_guardian_blaze", "polvo_guardian_blaze", "ultra_pocion_resistencia_fuego",
                "guardian_shulker_shell", "enderite_nugget", "enderite_fragment", "end_amatist", "enderite_ingot",
                "enderite_upgrades", "vithiums", "vithiums_fichas", "monedero", "mochila", "mochila_verde", "mochila_roja",
                "mochila_azul", "mochila_morada", "enderbag", "gancho", "panic_apple", "yunque_nivel_1",
                "yunque_nivel_2", "icetotem", "flytotem", "corrupted_golden_apple", "apilate_gold_block",
                "orbe_de_vida", "wither_compass", "icecrystal", "tridente_espectral", "linterna",
                "libro_habilidades", "fragmento_de_cordura", "manzana_marchita", "compuesto_s13",
                "serum_de_serenidad", "sculk_crystal_raw", "sculk_crystal_fragment", "runa_vacia", "runa_de_sculk",
                "manzana_vida", "pluma_levitacion", "ficha_mision",

                // ESENCIAS AÑADIDAS AQUÍ
                "esencia_proteccion", "esencia_irrompibilidad", "esencia_eficiencia", "esencia_fortuna",
                "esencia_filo", "esencia_castigo", "esencia_artropodos", "esencia_caida", "esencia_saqueo",
                "esencia_agilidad", "esencia_poder", "esencia_vacia"
        };
        for (String item : items) {
            registeredItems.add(item);
        }
    }

    public ItemStack getItem(String itemName, int cantidad, Player target) {
        return getItem(itemName, cantidad, target, -1);
    }

    public ItemStack getItem(String itemName, int cantidad, Player target, int usosEspeciales) {
        ItemStack item = null;

        switch (itemName.toLowerCase()) {
            case "doubletotem": item = doubleLifeTotem.createDoubleLifeTotem(); break;
            case "lifetotem": item = lifeTotem.createLifeTotem(); break;
            case "spidertotem": item = spiderTotem.createSpiderTotem(); break;
            case "infernaltotem": item = infernalTotem.createInfernalTotem(); break;
            case "aguijon_real": item = EmblemItems.createAgujonReal(); break;
            case "upgrade_vacio": item = UpgradeNTItems.createUpgradeVacio(); break;
            case "fragmento_upgrade": item = UpgradeNTItems.createFragmentoUpgrade(); break;
            case "duplicador": item = UpgradeNTItems.createDuplicador(); break;
            case "fragmento_infernal": item = EmblemItems.createFragmentoInfernal(); break;
            case "pepita_infernal": item = EmblemItems.createPepitaInfernal(); break;
            case "corrupted_nether_star": item = EmblemItems.createcorruptedNetherStar(); break;
            case "nether_emblem": item = EmblemItems.createNetherEmblem(); break;
            case "overworld_emblem": item = EmblemItems.createOverworldEmblem(); break;
            case "end_relic": item = EmblemItems.createEndEmblem(); break;
            case "corrupted_steak": item = DayOneChanges.corruptedSteak(); break;
            case "placa_diamante": item = EnhancedEnchantmentTable.createDiamondPlate(); break;
            case "mesa_encantamientos_mejorada": item = EnhancedEnchantmentTable.createEnhancedEnchantmentTable(); break;
            case "casco_night_vision": item = NightVisionHelmet.createNightVisionHelmet(); break;
            case "corrupted_helmet_armor": item = CorruptedArmor.createCorruptedHelmet(); break;
            case "corrupted_chestplate_armor": item = CorruptedArmor.createCorruptedChestplate(); break;
            case "corrupted_leggings_armor": item = CorruptedArmor.createCorruptedLeggings(); break;
            case "corrupted_boots_armor": item = CorruptedArmor.createCorruptedBoots(); break;
            case "enderite_sword": item = EnderiteTools.createEnderiteSword(); break;
            case "enderite_axe": item = EnderiteTools.createEnderiteAxe(); break;
            case "enderite_pickaxe": item = EnderiteTools.createEnderitePickaxe(); break;
            case "enderite_shovel": item = EnderiteTools.createEnderiteShovel(); break;
            case "enderite_hoe": item = EnderiteTools.createEnderiteHoe(); break;
            case "leggins_netherite_essence": item = legginsNetheriteEssence.createLegginsNetheriteEssence(); break;
            case "boot_netherite_essence": item = bootNetheriteEssence.createBootNetheriteEssence(); break;
            case "chestplate_netherite_essence": item = chestplateNetheriteEssence.createChestplateNetheriteEssence(); break;
            case "helmet_netherite_essence": item = helmetNetheriteEssence.createHelmetNetheriteEssence(); break;
            case "helmet_netherite_upgrade": item = corruptedUpgrades.createHelmetNetheriteUpgrade(); break;
            case "chestplate_netherite_upgrade": item = corruptedUpgrades.createChestplateNetheriteUpgrade(); break;
            case "leggins_netherite_upgrade": item = corruptedUpgrades.createLeggingsNetheriteUpgrade(); break;
            case "boot_netherite_upgrade": item = corruptedUpgrades.createBootsNetheriteUpgrade(); break;
            case "cooper_helmet": item = CopperArmor.createCopperHelmet(); break;
            case "cooper_chestplate": item = CopperArmor.createCopperChestplate(); break;
            case "cooper_leggings": item = CopperArmor.createCopperLeggings(); break;
            case "cooper_boots": item = CopperArmor.createCopperBoots(); break;
            case "corrupted_netherite_scrap": item = CorruptedNetheriteItems.createCorruptedScrapNetherite(); break;
            case "corrupted_netherite_ingot": item = CorruptedNetheriteItems.createCorruptedNetheriteIngot(); break;
            case "corrupted_powder": item = CorruptedMobItems.createCorruptedPowder(); break;
            case "corrupted_bone_lime": item = CorruptedMobItems.createCorruptedBone(CorruptedMobItems.BoneVariant.LIME); break;
            case "corrupted_bone_green": item = CorruptedMobItems.createCorruptedBone(CorruptedMobItems.BoneVariant.GREEN); break;
            case "corrupted_bone_yellow": item = CorruptedMobItems.createCorruptedBone(CorruptedMobItems.BoneVariant.YELLOW); break;
            case "corrupted_bone_orange": item = CorruptedMobItems.createCorruptedBone(CorruptedMobItems.BoneVariant.ORANGE); break;
            case "corrupted_bone_red": item = CorruptedMobItems.createCorruptedBone(CorruptedMobItems.BoneVariant.RED); break;
            case "corrupted_rotten": item = CorruptedMobItems.createCorruptedMeet(); break;
            case "corrupted_spidereyes": item = CorruptedMobItems.createCorruptedSpiderEye(); break;
            case "corrupted_soul": item = corruptedSoul.createCorruptedSoulEssence(); break;
            case "corrupted_ancient_debris": item = corruptedAncientDebris.createcorruptedancientdebris(); break;
            case "guardian_shulker_heart": item = guardianShulkerHeart.createGuardianShulkerHeart(); break;
            case "endstalactitas": item = Endstalactitas.createEndstalactita(); break;
            case "toxicspidereye": item = ItemsTotems.createToxicSpiderEye(); break;
            case "infernalcreeperpowder": item = ItemsTotems.createInfernalCreeperPowder(); break;
            case "whiteenderpearl": item = ItemsTotems.createWhiteEnderPearl(); break;
            case "specialtotem": item = ItemsTotems.createSpecialTotem(); break;
            case "customboat": if(target != null) item = customBoat.createBoatItem(target); break;
            case "fuel": item = customBoat.createFuelItem(); break;
            case "varita_guardian_blaze": item = BlazeItems.createBlazeRod(); break;
            case "polvo_guardian_blaze": item = BlazeItems.createGuardianBlazePowder(); break;
            case "ultra_pocion_resistencia_fuego": item = BlazeItems.createPotionOfFireResistance(); break;
            case "guardian_shulker_shell": item = EndItems.createGuardianShulkerShell(); break;
            case "enderite_nugget": item = EndItems.createEnderiteNugget(cantidad); break;
            case "enderite_fragment": item = EndItems.createFragmentoEnderite(); break;
            case "end_amatist": item = EndItems.createEndAmatist(cantidad); break;
            case "enderite_ingot": item = EndItems.createIngotEnderite(); break;
            case "enderite_upgrades": item = EndItems.createEnderiteUpgrades(); break;
            case "vithiums": item = EconomyItems.createVithiumCoin(); break;
            case "vithiums_fichas": item = EconomyItems.createVithiumToken(); break;
            case "monedero": item = EconomyItems.createMonedero(); break;
            case "mochila": item = EconomyItems.createNormalMochila(); break;
            case "mochila_verde": item = EconomyItems.createGreenMochila(); break;
            case "mochila_roja": item = EconomyItems.createRedMochila(); break;
            case "mochila_azul": item = EconomyItems.createBlueMochila(); break;
            case "mochila_morada": item = EconomyItems.createPurpleMochila(); break;
            case "enderbag": item = EconomyItems.createEnderBag(); break;
            case "gancho": item = EconomyItems.createGancho(); break;
            case "panic_apple": item = EconomyItems.createManzanaPanico(); break;
            case "yunque_nivel_1": item = EconomyItems.createYunqueReparadorNivel1(); break;
            case "yunque_nivel_2": item = EconomyItems.createYunqueReparadorNivel2(); break;
            case "icetotem": item = economyIceTotem.createIceTotem(); break;
            case "flytotem": item = economyFlyTotem.createFlyTotem(); break;
            case "corrupted_golden_apple": item = CorruptedGoldenApple.createCorruptedGoldenApple(); break;
            case "apilate_gold_block": item = CorruptedGoldenApple.createApilateGoldBlock(); break;
            case "orbe_de_vida": item = ReviveItems.createResurrectOrb(); break;
            case "wither_compass": item = UltraWitherCompass.createUltraWitherCompass(); break;
            case "icecrystal": item = ItemsTotems.createIceCrystal(); break;
            case "tridente_espectral": item = tridenteEspectral.createSpectralTrident(); break;
            case "linterna": item = flashlightItem.createFlashlight(); break;
            case "libro_habilidades": item = HabilidadesBook.createHabilidadesBook(); break;
            case "fragmento_de_cordura": item = CorrupcionAnsiosaItems.createFragmentoCordura(); break;
            case "manzana_marchita": item = CorrupcionAnsiosaItems.createManzanaMarchita(); break;
            case "compuesto_s13": item = CorrupcionAnsiosaItems.createCompuestoS13(); break;
            case "serum_de_serenidad": item = CorrupcionAnsiosaItems.createSerumSerenidad(); break;
            case "sculk_crystal_raw": item = InfestedCaveItems.createRawSculkCrystal(cantidad); break;
            case "sculk_crystal_fragment": item = InfestedCaveItems.createSculkCrystalFragment(); break;
            case "runa_vacia": item = InfestedCaveItems.createEmptyRune(); break;
            case "runa_de_sculk": item = InfestedCaveItems.createNormalRune(); break;
            case "manzana_vida": item = itemsEventos.createManzanaVida(); break;
            case "pluma_levitacion": item = itemsEventos.createPlumaLevitacion(); break;
            case "ficha_mision": int numMision = (usosEspeciales > 0) ? usosEspeciales : 1; item = new Events.MissionSystem.FichaMision(plugin).createToken(numMision, "Misión Asignada");break;

            // ESENCIAS AÑADIDAS AL SWITCH
            case "esencia_proteccion": item = EssenceFactory.createProtectionEssence(usosEspeciales); break;
            case "esencia_irrompibilidad": item = EssenceFactory.createUnbreakingEssence(usosEspeciales); break;
            case "esencia_eficiencia": item = EssenceFactory.createEfficiencyEssence(usosEspeciales); break;
            case "esencia_fortuna": item = EssenceFactory.createFortuneEssence(usosEspeciales); break;
            case "esencia_filo": item = EssenceFactory.createSharpnessEssence(usosEspeciales); break;
            case "esencia_castigo": item = EssenceFactory.createSmiteEssence(usosEspeciales); break;
            case "esencia_artropodos": item = EssenceFactory.createBaneOfArthropodsEssence(usosEspeciales); break;
            case "esencia_caida": item = EssenceFactory.createFeatherFallingEssence(usosEspeciales); break;
            case "esencia_saqueo": item = EssenceFactory.createLootingEssence(usosEspeciales); break;
            case "esencia_agilidad": item = EssenceFactory.createDepthStriderEssence(usosEspeciales); break;
            case "esencia_poder": item = EssenceFactory.createPowerEssence(usosEspeciales); break;
            case "esencia_vacia": item = EssenceFactory.createVoidEssence(); break;
            default:
                return null;
        }

        if (item != null && !itemName.equalsIgnoreCase("enderite_nugget") && !itemName.equalsIgnoreCase("end_amatist") && !itemName.equalsIgnoreCase("sculk_crystal_raw")) {
            item.setAmount(cantidad);
        }
        return item;
    }

    public List<String> getRegisteredItems() {
        return registeredItems;
    }
}