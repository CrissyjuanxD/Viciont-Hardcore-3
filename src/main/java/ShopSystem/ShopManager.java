package ShopSystem;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ShopManager {

    private final JavaPlugin plugin;
    private final File tradesFile;
    public final NamespacedKey shopKey;
    public final NamespacedKey shopIdKey;

    public final Map<UUID, String> activeShops = new ConcurrentHashMap<>();
    public final Map<UUID, Integer> editingTradeIndex = new ConcurrentHashMap<>();
    public final Map<UUID, String> editingSlotType = new ConcurrentHashMap<>();

    public ShopManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.shopKey = new NamespacedKey(plugin, "shop_type");
        this.shopIdKey = new NamespacedKey(plugin, "shop_id");
        this.tradesFile = new File(plugin.getDataFolder(), "tradeos.yml");

        if (!tradesFile.exists()) {
            try {
                tradesFile.getParentFile().mkdirs();
                tradesFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Error creando tradeos.yml: " + e.getMessage());
            }
        }
    }

    public void spawnShop(String name, Location location, Villager.Type type, Villager.Profession profession) {
        Villager villager = (Villager) location.getWorld().spawnEntity(location, EntityType.VILLAGER);
        String shopId = UUID.randomUUID().toString();
        String coloredName = ChatColor.translateAlternateColorCodes('&', name);

        villager.setCustomName(coloredName);
        villager.setCustomNameVisible(true);
        villager.setAI(false);
        villager.setSilent(true);
        villager.setInvulnerable(true);

        villager.setVillagerType(type != null ? type : Villager.Type.PLAINS);
        villager.setProfession(profession != null ? profession : Villager.Profession.NONE);

        if (villager.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED) != null)
            villager.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(0.0);

        villager.getPersistentDataContainer().set(shopKey, PersistentDataType.STRING, "custom_shop");
        villager.getPersistentDataContainer().set(shopIdKey, PersistentDataType.STRING, shopId);

        initializeEmptyTrades(villager);
        saveShopTrades(shopId, villager.getRecipes());
    }

    private void initializeEmptyTrades(Villager villager) {
        List<MerchantRecipe> recipes = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            ItemStack emptyItem = createEmptyTradeItem();
            MerchantRecipe recipe = new MerchantRecipe(emptyItem, 9999);
            recipe.addIngredient(emptyItem);
            recipes.add(recipe);
        }
        villager.setRecipes(recipes);
    }

    public ItemStack createEmptyTradeItem() {
        ItemStack item = new ItemStack(Material.STRUCTURE_VOID);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GRAY + "Vacío");
            item.setItemMeta(meta);
        }
        return item;
    }

    public boolean isEmpty(ItemStack item) {
        return item == null || item.getType() == Material.AIR || item.getType() == Material.STRUCTURE_VOID;
    }

    public boolean isMatch(ItemStack invItem, ItemStack reqItem) {
        if (isEmpty(invItem) || isEmpty(reqItem)) return false;
        if (invItem.getType() != reqItem.getType()) return false;

        ItemMeta invMeta = invItem.getItemMeta();
        ItemMeta reqMeta = reqItem.getItemMeta();

        if (reqMeta != null && reqMeta.hasCustomModelData()) {
            return invMeta != null && invMeta.hasCustomModelData() && invMeta.getCustomModelData() == reqMeta.getCustomModelData();
        }
        if (reqMeta != null && reqMeta.hasDisplayName()) {
            return invMeta != null && invMeta.hasDisplayName() && invMeta.getDisplayName().equals(reqMeta.getDisplayName());
        }

        return true;
    }

    public void updateVillagerTrade(Villager villager, int tradeNumber, String slotType, ItemStack item) {
        List<MerchantRecipe> recipes = new ArrayList<>(villager.getRecipes());

        while (recipes.size() <= tradeNumber) {
            ItemStack emptyItem = createEmptyTradeItem();
            MerchantRecipe recipe = new MerchantRecipe(emptyItem, 9999);
            recipe.addIngredient(emptyItem);
            recipes.add(recipe);
        }

        MerchantRecipe currentRecipe = recipes.get(tradeNumber);
        List<ItemStack> ingredients = new ArrayList<>(currentRecipe.getIngredients());
        ItemStack result = currentRecipe.getResult();

        switch (slotType) {
            case "Ingrediente 1":
                if (isEmpty(item)) {
                    if (!ingredients.isEmpty()) ingredients.set(0, createEmptyTradeItem());
                } else {
                    if (ingredients.isEmpty()) ingredients.add(item);
                    else ingredients.set(0, item);
                }
                break;
            case "Ingrediente 2":
                if (isEmpty(item)) {
                    if (ingredients.size() > 1) ingredients.remove(1);
                } else {
                    if (ingredients.size() < 2) {
                        if (ingredients.isEmpty()) ingredients.add(createEmptyTradeItem());
                        ingredients.add(item);
                    } else {
                        ingredients.set(1, item);
                    }
                }
                break;
            case "Producto":
                result = isEmpty(item) ? createEmptyTradeItem() : item;
                break;
        }

        MerchantRecipe newRecipe = new MerchantRecipe(isEmpty(result) ? createEmptyTradeItem() : result, 9999);

        for (ItemStack ingredient : ingredients) {
            if (!isEmpty(ingredient)) newRecipe.addIngredient(ingredient);
        }
        if (newRecipe.getIngredients().isEmpty()) newRecipe.addIngredient(createEmptyTradeItem());

        recipes.set(tradeNumber, newRecipe);
        villager.setRecipes(recipes);
    }

    public void saveShopTrades(String shopId, List<MerchantRecipe> recipes) {
        FileConfiguration config = YamlConfiguration.loadConfiguration(tradesFile);
        config.set("shops." + shopId + ".trades", null);

        for (int i = 0; i < recipes.size(); i++) {
            MerchantRecipe recipe = recipes.get(i);
            String basePath = "shops." + shopId + ".trades." + i;

            List<ItemStack> ingredients = recipe.getIngredients();
            for (int j = 0; j < ingredients.size(); j++) {
                ItemStack ingredient = ingredients.get(j);
                if (!isEmpty(ingredient)) config.set(basePath + ".ingredients." + j, ingredient);
            }

            ItemStack result = recipe.getResult();
            if (!isEmpty(result)) config.set(basePath + ".result", result);
            else config.set(basePath + ".result", createEmptyTradeItem());

            config.set(basePath + ".maxUses", recipe.getMaxUses());
        }

        try { config.save(tradesFile); } catch (IOException e) { plugin.getLogger().severe("Error guardando tradeos: " + e.getMessage()); }
    }

    public void loadShopTrades(String shopId, Villager villager) {
        FileConfiguration config = YamlConfiguration.loadConfiguration(tradesFile);
        if (!config.contains("shops." + shopId)) return;

        List<MerchantRecipe> recipes = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            String basePath = "shops." + shopId + ".trades." + i;

            if (config.contains(basePath)) {
                ItemStack result = config.getItemStack(basePath + ".result");

                if (result != null) {
                    MerchantRecipe recipe = new MerchantRecipe(result, 9999);
                    for (int j = 0; j < 2; j++) {
                        ItemStack ingredient = config.getItemStack(basePath + ".ingredients." + j);
                        if (!isEmpty(ingredient)) recipe.addIngredient(ingredient);
                    }
                    if (recipe.getIngredients().isEmpty()) {
                        recipe.addIngredient(createEmptyTradeItem());
                    }
                    recipes.add(recipe);
                } else {
                    ItemStack emptyItem = createEmptyTradeItem();
                    MerchantRecipe recipe = new MerchantRecipe(emptyItem, 9999);
                    recipe.addIngredient(emptyItem);
                    recipes.add(recipe);
                }
            } else {
                ItemStack emptyItem = createEmptyTradeItem();
                MerchantRecipe recipe = new MerchantRecipe(emptyItem, 9999);
                recipe.addIngredient(emptyItem);
                recipes.add(recipe);
            }
        }
        villager.setRecipes(recipes);
    }

    public void removeShopFromFile(String shopId) {
        FileConfiguration config = YamlConfiguration.loadConfiguration(tradesFile);
        config.set("shops." + shopId, null);
        try { config.save(tradesFile); } catch (IOException e) {}
    }

    public Villager getVillagerById(String shopId) {
        if (shopId == null) return null;
        for (org.bukkit.World world : Bukkit.getWorlds()) {
            for (Villager v : world.getEntitiesByClass(Villager.class)) {
                if (shopId.equals(v.getPersistentDataContainer().get(shopIdKey, PersistentDataType.STRING))) {
                    return v;
                }
            }
        }
        return null;
    }

    public JavaPlugin getPlugin() {
        return plugin;
    }
}