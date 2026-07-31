package Gui;

import java.util.List;

public interface GuiMobProvider {
    String getEntityId();
    String getEntityType();
    String getName();
    String getColor();
    int getScale();

    List<String> getDynamicAttributes();

    List<String> getDescription();

    record AttributeLine(String text, String color) {}
}