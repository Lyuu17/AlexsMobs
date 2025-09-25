package com.github.alexthe666.alexsmobs.registry;

import com.github.alexthe666.alexsmobs.AlexsMobs;
import com.github.alexthe666.alexsmobs.client.model.ModelWanderingVillagerRider;
import com.github.alexthe666.alexsmobs.client.model.layered.*;
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
        
        EntityModelLayerRegistry.register(SITTING_WANDERING_VILLAGER, () -> TexturedModelData.of(ModelWanderingVillagerRider.getModelData(), 64, 64));
        EntityModelLayerRegistry.register(UNDERMINER, () -> TexturedModelData.of(BipedEntityModel.getModelData(Dilation.NONE, 0.05F), 64, 64) );
        EntityModelLayerRegistry.register(ROADRUNNER_BOOTS, () -> ModelRoadrunnerBoots.createArmorLayer(new Dilation(0.5F)));
        EntityModelLayerRegistry.register(MOOSE_HEADGEAR, () -> ModelMooseHeadgear.createArmorLayer(new Dilation(0.5F)));
        EntityModelLayerRegistry.register(FRONTIER_CAP, () -> ModelFrontierCap.createArmorLayer(new Dilation(0.5F)));
        EntityModelLayerRegistry.register(SPIKED_TURTLE_SHELL, () -> ModelSpikedTurtleShell.createArmorLayer(new Dilation(0.5F)));
        EntityModelLayerRegistry.register(FEDORA, () -> ModelFedora.createArmorLayer(new Dilation(0.5F)));
        EntityModelLayerRegistry.register(AM_ELYTRA, () -> ModelAMElytra.createLayer(new Dilation(1.0F)));
        EntityModelLayerRegistry.register(SOMBRERO, () -> ModelSombrero.createArmorLayer(new Dilation(0.5F)));
        EntityModelLayerRegistry.register(SOMBRERO_GOOFY_FASHION, () -> ModelSombrero.createArmorLayerAprilFools(new Dilation(0.5F)));
        EntityModelLayerRegistry.register(FROSTSTALKER_HELMET, () -> ModelFroststalkerHelmet.createArmorLayer(new Dilation(0.5F)));
        EntityModelLayerRegistry.register(ROCKY_CHESTPLATE, () -> ModelRockyChestplate.createArmorLayer(new Dilation(0.7F)));
        EntityModelLayerRegistry.register(FLYING_FISH_BOOTS, () -> ModelFlyingFishBoots.createArmorLayer(new Dilation(0.5F)));
        EntityModelLayerRegistry.register(NOVELTY_HAT, () -> ModelNoveltyHat.createArmorLayer(new Dilation(0.5F)));
        EntityModelLayerRegistry.register(UNSETTLING_KIMONO, () -> ModelUnsettlingKimono.createArmorLayer(new Dilation(0.5F)));
    }
}