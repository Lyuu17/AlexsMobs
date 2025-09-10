package com.github.alexthe666.alexsmobs.registry;

import com.github.alexthe666.alexsmobs.AlexsMobs;
import com.github.alexthe666.alexsmobs.model.layered.ModelSombrero;
import dev.architectury.registry.client.level.entity.EntityModelLayerRegistry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.Dilation;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public class AMModelLayerRegistry {

    public static final EntityModelLayer AM_ELYTRA = createLocation("am_elytra", "main");
    public static final EntityModelLayer SITTING_WANDERING_VILLAGER = createLocation("sitting_wandering_villager", "main");
    public static final EntityModelLayer ROADRUNNER_BOOTS = createLocation("roadrunner_boots", "main");
    public static final EntityModelLayer MOOSE_HEADGEAR = createLocation("moose_headgear", "main");
    public static final EntityModelLayer FRONTIER_CAP = createLocation("frontier_cap", "main");
    public static final EntityModelLayer SPIKED_TURTLE_SHELL = createLocation("spiked_turtle_shell", "main");
    public static final EntityModelLayer FEDORA = createLocation("fedora", "main");
    public static final EntityModelLayer SOMBRERO = createLocation("sombrero", "main");
    public static final EntityModelLayer SOMBRERO_GOOFY_FASHION = createLocation("sombrero_goofy_fashion", "main");
    public static final EntityModelLayer FROSTSTALKER_HELMET = createLocation("froststalker_helmet", "main");
    public static final EntityModelLayer ROCKY_CHESTPLATE = createLocation("rocky_chestplate", "main");
    public static final EntityModelLayer FLYING_FISH_BOOTS = createLocation("flying_fish_boots", "main");
    public static final EntityModelLayer NOVELTY_HAT = createLocation("novelty_hat", "main");
    public static final EntityModelLayer UNDERMINER = createLocation("underminer", "main");
    public static final EntityModelLayer UNSETTLING_KIMONO = createLocation("unsettling_kimono", "main");

    private static EntityModelLayer createLocation(String model, String layer) {
        return new EntityModelLayer(new Identifier(AlexsMobs.MOD_ID, model), layer);
    }

    public static void register() {
        EntityModelLayerRegistry.register(SOMBRERO, () -> ModelSombrero.createArmorLayer(Dilation.NONE));
        EntityModelLayerRegistry.register(SOMBRERO_GOOFY_FASHION, () -> ModelSombrero.createArmorLayerAprilFools(Dilation.NONE));
        EntityModelLayerRegistry.register(UNDERMINER, () -> TexturedModelData.of(BipedEntityModel.getModelData(Dilation.NONE, 0.05F), 64, 64) );
    }
}