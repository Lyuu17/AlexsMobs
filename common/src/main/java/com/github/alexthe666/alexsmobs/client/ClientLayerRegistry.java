package com.github.alexthe666.alexsmobs.client;

//@OnlyIn(Dist.CLIENT)
//public class ClientLayerRegistry {
//
//    @SubscribeEvent
//    @OnlyIn(Dist.CLIENT)
//    public static void onAddLayers(EntityRenderersEvent.AddLayers event) {
//        List<EntityType<? extends LivingEntity>> entityTypes = ImmutableList.copyOf(
//                ForgeRegistries.ENTITY_TYPES.getValues().stream()
//                        .filter(DefaultAttributes::hasSupplier)
//                        .map(entityType -> (EntityType<? extends LivingEntity>) entityType)
//                        .collect(Collectors.toList()));
//        entityTypes.forEach((entityType -> {
//            addLayerIfApplicable(entityType, event);
//        }));
//        for (String skinType : event.getSkins()){
//            event.getSkin(skinType).addLayer(new LayerRainbow(event.getSkin(skinType)));
//        }
//    }
//
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
//}
