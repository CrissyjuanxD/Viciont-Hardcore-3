package Enchants;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class GiveEssenceCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Este comando solo puede ser usado por jugadores.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            player.sendMessage("Por favor, especifica una esencia: proteccion, irrompibilidad, mending, eficiencia, fortuna, filo, castigo, artrópodos, caida, saqueo, agilidad, toque, poder.");
            return true;
        }

        ItemStack essence;
        switch (args[0].toLowerCase()) {
            case "proteccion":
                essence = EssenceFactory.createProtectionEssence();
                break;
            case "irrompibilidad":
                essence = EssenceFactory.createUnbreakingEssence();
                break;
            case "mending":
                essence = EssenceFactory.createMendingEssence();
                break;
            case "eficiencia":
                essence = EssenceFactory.createEfficiencyEssence();
                break;
            case "fortuna":
                essence = EssenceFactory.createFortuneEssence();
                break;
            case "filo":
                essence = EssenceFactory.createSharpnessEssence();
                break;
            case "castigo":
                essence = EssenceFactory.createSmiteEssence();
                break;
            case "artrópodos":
                essence = EssenceFactory.createBaneOfArthropodsEssence();
                break;
            case "caida":
                essence = EssenceFactory.createFeatherFallingEssence();
                break;
            case "saqueo":
                essence = EssenceFactory.createLootingEssence();
                break;
            case "agilidad":
                essence = EssenceFactory.createDepthStriderEssence();
                break;
            case "toque":
                essence = EssenceFactory.createSilkTouchEssence();
                break;
            case "poder":
                essence = EssenceFactory.createPowerEssence();
                break;
            default:
                player.sendMessage("Esencia desconocida. Usa: proteccion, irrompibilidad, mending, eficiencia, fortuna, filo, castigo, artrópodos, caida, saqueo, agilidad, toque, poder.");
                return true;
        }

        player.getInventory().addItem(essence);
        player.sendMessage("Has recibido la " + essence.getItemMeta().getDisplayName() + "!");
        return true;
    }
}

