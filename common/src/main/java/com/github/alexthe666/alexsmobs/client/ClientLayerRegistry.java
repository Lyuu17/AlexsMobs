package com.github.alexthe666.alexsmobs.client;

import com.github.alexthe666.alexsmobs.registry.AMBlockRegistry;
import dev.architectury.registry.client.rendering.RenderTypeRegistry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.RenderLayer;

@Environment(EnvType.CLIENT)
public class ClientLayerRegistry {

    public static void init() {
//        List<EntityType<? extends LivingEntity>> entityTypes = ImmutableList.copyOf(
//                Registries.ENTITY_TYPE.streamEntries().map(RegistryEntry.Reference::value)
//                        .filter(DefaultAttributeRegistry::hasDefinitionFor)
//                        .map(entityType -> (EntityType<? extends LivingEntity>) entityType)
//                        .collect(Collectors.toList()));
//        entityTypes.forEach((entityType -> {
//            addLayerIfApplicable(entityType, event);
//        }));
//        for (var skinType : event.getSkins()){
//            event.getSkin(skinType).addLayer(new LayerRainbow(event.getSkin(skinType)));
//        }

        RenderTypeRegistry.register(RenderLayer.getCutout(),
                AMBlockRegistry.BANANA_PEEL.get(),
                AMBlockRegistry.HUMMINGBIRD_FEEDER.get(),
                AMBlockRegistry.BISON_CARPET.get(),
                AMBlockRegistry.BISON_FUR_BLOCK.get(),
                AMBlockRegistry.TRIOPS_EGGS.get(),
                AMBlockRegistry.VOID_WORM_BEAK.get());
        RenderTypeRegistry.register(RenderLayer.getTranslucent(),
                AMBlockRegistry.BANANA_SLUG_SLIME_BLOCK.get(),
                AMBlockRegistry.CAPSID.get(),
                AMBlockRegistry.CRYSTALIZED_BANANA_SLUG_MUCUS.get(),
                AMBlockRegistry.ENDER_RESIDUE.get(),
                AMBlockRegistry.RAINBOW_GLASS.get(),
                AMBlockRegistry.SKUNK_SPRAY.get(),
                AMBlockRegistry.TRANSMUTATION_TABLE.get());

//        RenderTypeRegistry.register(new LayerRainbow(renderer), ObjectRegistry.CAKE_STAND.get());
    }

//    private static void addLayerIfApplicable(EntityType<? extends LivingEntity> entityType, EntityRenderersEvent.AddLayers event) {
//        LivingEntityRenderer renderer = null;
//        if(entityType != EntityType.ENDER_DRAGON){
//            try{
//                renderer = event.getRenderer(entityType);
//            }catch (Exception e){
//                AlexsMobs.LOGGER.warn("Could not apply rainbow color layer to " + ForgeRegistries.ENTITY_TYPES.getKey(entityType) + ", has custom renderer that is not LivingEntityRenderer.");
//            }
//            if(renderer != null){
//                renderer.addLayer(new LayerRainbow(renderer));
//            }
//        }
//    }
}