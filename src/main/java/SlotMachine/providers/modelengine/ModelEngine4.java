package SlotMachine.providers.modelengine;

import com.ticxo.modelengine.api.ModelEngineAPI;
import com.ticxo.modelengine.api.generator.blueprint.ModelBlueprint;
import com.ticxo.modelengine.api.model.ActiveModel;
import com.ticxo.modelengine.api.model.ModeledEntity;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Optional;
import java.util.UUID;

/**
 * Proveedor para ModelEngine 4 - Basado en DTools3
 */
public class ModelEngine4 {

    private final JavaPlugin plugin;

    public ModelEngine4(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public UUID spawnModel(LivingEntity entity, String modelId) {
        try {
            ModelBlueprint modelBlueprint = ModelEngineAPI.getBlueprint(modelId);
            if (modelBlueprint == null) {
                plugin.getLogger().warning("Model blueprint not found: " + modelId);
                return null;
            }

            Location location = entity.getLocation();
            ModeledEntity modeledEntity = ModelEngineAPI.createModeledEntity(entity);

            // Configurar rotación del modelo
            modeledEntity.getBase().getBodyRotationController().setYHeadRot(location.getYaw());
            modeledEntity.getBase().getBodyRotationController().setYBodyRot(location.getYaw());
            modeledEntity.getBase().getBodyRotationController().setXHeadRot(location.getPitch());

            // Crear y añadir modelo activo
            ActiveModel activeModel = ModelEngineAPI.createActiveModel(modelBlueprint);
            modeledEntity.addModel(activeModel, false);

            plugin.getLogger().info("Created ModelEngine 4 model: " + modelId + " on entity: " + entity.getUniqueId());
            return entity.getUniqueId();

        } catch (Exception e) {
            plugin.getLogger().severe("Error creating ModelEngine 4 model: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public void playAnimation(UUID slotMachineUniqueId, String modelId, String animationId) {
        try {
            ModeledEntity modeledEntity = ModelEngineAPI.getModeledEntity(slotMachineUniqueId);
            if (modeledEntity != null) {
                modeledEntity.getModel(modelId).ifPresent(activeModel -> {
                    activeModel.getAnimationHandler().playAnimation(animationId, 0.0, 0.0, 1.0, false);
                    plugin.getLogger().info("Playing animation: " + animationId + " on model: " + modelId);
                });
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error playing animation: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public boolean hasAnimation(UUID slotMachineUniqueId, String modelId) {
        try {
            ModeledEntity modeledEntity = ModelEngineAPI.getModeledEntity(slotMachineUniqueId);
            if (modeledEntity == null) {
                return false;
            }

            Optional<ActiveModel> model = modeledEntity.getModel(modelId);
            return model.filter(value -> !value.getAnimationHandler().hasFinishedAllAnimations()).isPresent();

        } catch (Exception e) {
            plugin.getLogger().severe("Error checking animation: " + e.getMessage());
            return false;
        }
    }

    public boolean removeModel(Entity entity) {
        try {
            ModeledEntity modeledEntity = ModelEngineAPI.getModeledEntity(entity.getUniqueId());
            if (modeledEntity == null) {
                return false;
            }

            // Remover todos los modelos
            modeledEntity.getModels().clear();

            plugin.getLogger().info("Removed ModelEngine 4 model from entity: " + entity.getUniqueId());
            return true;

        } catch (Exception e) {
            plugin.getLogger().severe("Error removing model: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public boolean hasModel(Entity entity) {
        try {
            ModeledEntity modeledEntity = ModelEngineAPI.getModeledEntity(entity.getUniqueId());
            return modeledEntity != null && !modeledEntity.getModels().isEmpty();
        } catch (Exception e) {
            return false;
        }
    }
}