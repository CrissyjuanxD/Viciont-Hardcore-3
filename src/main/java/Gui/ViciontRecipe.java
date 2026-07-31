package Gui;

import java.util.HashMap;
import java.util.Map;

public class ViciontRecipe {
    private final String id;
    private final int type; // 1=Crafteo, 2=Horno, 3=Herreria, 4=Runica
    private final String title;
    private final ViciontItem result;
    private String[] shape;
    private final Map<Character, ViciontItem> ingredients = new HashMap<>();

    public ViciontRecipe(String id, int type, String title, ViciontItem result) {
        this.id = id;
        this.type = type;
        this.title = title;
        this.result = result;
    }

    public ViciontRecipe shape(String... shape) {
        this.shape = shape;
        return this;
    }

    public ViciontRecipe setIngredient(char key, ViciontItem item) {
        this.ingredients.put(key, item);
        return this;
    }

    public String getId() { return id; }
    public int getType() { return type; }
    public String getTitle() { return title; }
    public ViciontItem getResult() { return result; }
    public String[] getShape() { return shape; }
    public Map<Character, ViciontItem> getIngredients() { return ingredients; }
}