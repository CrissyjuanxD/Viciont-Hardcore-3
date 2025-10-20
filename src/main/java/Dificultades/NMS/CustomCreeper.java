/*
// CustomCreeper.java
package Dificultades.NMS;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.level.Level;

public class CustomCreeper extends Creeper {

    public CustomCreeper(EntityType<? extends Creeper> entitytypes, Level world) {
        super(entitytypes, world);
    }

    @Override
    public boolean isDarkEnoughToSpawn() {
        // Siempre retorna true para permitir spawn sin importar la luz
        return true;
    }

    @Override
    public boolean checkSpawnObstruction(Level world) {
        // Permite spawn incluso si hay obstrucciones de luz
        return true;
    }
}*/
