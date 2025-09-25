package com.github.alexthe666.alexsmobs.fabric;

import com.github.alexthe666.alexsmobs.AlexsMobsClient;
import com.github.alexthe666.alexsmobs.client.model.layered.*;
import com.github.alexthe666.alexsmobs.fabric.client.render.AMArmorRenderer;
import com.github.alexthe666.alexsmobs.fabric.item.DynamicItemRenderer;
import com.github.alexthe666.alexsmobs.registry.AMItemRegistry;
import com.github.alexthe666.alexsmobs.registry.AMModelLayerRegistry;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.ArmorRenderer;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.minecraft.entity.EquipmentSlot;

public final class AlexsMobsFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        AlexsMobsClient.init();

        ArmorRenderer.register(new AMArmorRenderer("textures/armor/fedora.png", ModelFedora::new, AMModelLayerRegistry.FEDORA, EquipmentSlot.HEAD), AMItemRegistry.FEDORA.get());
        ArmorRenderer.register(new AMArmorRenderer("textures/armor/flying_fish_boots.png", ModelFlyingFishBoots::new, AMModelLayerRegistry.FLYING_FISH_BOOTS, EquipmentSlot.FEET), AMItemRegistry.FLYING_FISH_BOOTS.get());
        ArmorRenderer.register(new AMArmorRenderer("textures/armor/frontier_cap.png", ModelFrontierCap::new, AMModelLayerRegistry.FRONTIER_CAP, EquipmentSlot.HEAD), AMItemRegistry.FRONTIER_CAP.get());
        ArmorRenderer.register(new AMArmorRenderer("textures/armor/froststalker_helmet.png", ModelFroststalkerHelmet::new, AMModelLayerRegistry.FROSTSTALKER_HELMET, EquipmentSlot.HEAD), AMItemRegistry.FROSTSTALKER_HELMET.get());
        ArmorRenderer.register(new AMArmorRenderer("textures/armor/moose_headgear.png", ModelMooseHeadgear::new, AMModelLayerRegistry.MOOSE_HEADGEAR, EquipmentSlot.HEAD), AMItemRegistry.MOOSE_HEADGEAR.get());
        ArmorRenderer.register(new AMArmorRenderer("textures/armor/novelty_hat.png", ModelNoveltyHat::new, AMModelLayerRegistry.NOVELTY_HAT, EquipmentSlot.HEAD), AMItemRegistry.NOVELTY_HAT.get());
        ArmorRenderer.register(new AMArmorRenderer("textures/armor/roadrunner_boots.png", ModelRoadrunnerBoots::new, AMModelLayerRegistry.ROADRUNNER_BOOTS, EquipmentSlot.FEET), AMItemRegistry.ROADDRUNNER_BOOTS.get());
        ArmorRenderer.register(new AMArmorRenderer("textures/armor/rocky_chestplate.png", ModelRockyChestplate::new, AMModelLayerRegistry.ROCKY_CHESTPLATE, EquipmentSlot.CHEST), AMItemRegistry.ROCKY_CHESTPLATE.get());
        ArmorRenderer.register(new AMArmorRenderer("textures/armor/sombrero.png", ModelSombrero::new, AMModelLayerRegistry.SOMBRERO, EquipmentSlot.HEAD), AMItemRegistry.SOMBRERO.get());
        ArmorRenderer.register(new AMArmorRenderer("textures/armor/spiked_turtle_shell.png", ModelSpikedTurtleShell::new, AMModelLayerRegistry.SPIKED_TURTLE_SHELL, EquipmentSlot.HEAD), AMItemRegistry.SPIKED_TURTLE_SHELL.get());
        ArmorRenderer.register(new AMArmorRenderer("textures/armor/tarantula_hawk_elytra.png", ModelAMElytra::new, AMModelLayerRegistry.AM_ELYTRA, EquipmentSlot.CHEST), AMItemRegistry.TARANTULA_HAWK_ELYTRA.get());
        ArmorRenderer.register(new AMArmorRenderer("textures/armor/unsettling_kimono.png", ModelUnsettlingKimono::new, AMModelLayerRegistry.UNSETTLING_KIMONO, EquipmentSlot.CHEST), AMItemRegistry.UNSETTLING_KIMONO.get());

        BuiltinItemRendererRegistry.INSTANCE.register(AMItemRegistry.DIMENSIONAL_CARVER.get(), new DynamicItemRenderer());
        BuiltinItemRendererRegistry.INSTANCE.register(AMItemRegistry.FALCONRY_GLOVE.get(), new DynamicItemRenderer());
        BuiltinItemRendererRegistry.INSTANCE.register(AMItemRegistry.MYSTERIOUS_WORM.get(), new DynamicItemRenderer());
        BuiltinItemRendererRegistry.INSTANCE.register(AMItemRegistry.SHATTERED_DIMENSIONAL_CARVER.get(), new DynamicItemRenderer());
        BuiltinItemRendererRegistry.INSTANCE.register(AMItemRegistry.SHIELD_OF_THE_DEEP.get(), new DynamicItemRenderer());
        BuiltinItemRendererRegistry.INSTANCE.register(AMItemRegistry.SKELEWAG_SWORD.get(), new DynamicItemRenderer());
        BuiltinItemRendererRegistry.INSTANCE.register(AMItemRegistry.STINK_RAY.get(), new DynamicItemRenderer());
        BuiltinItemRendererRegistry.INSTANCE.register(AMItemRegistry.TAB_ICON.get(), new DynamicItemRenderer());
        BuiltinItemRendererRegistry.INSTANCE.register(AMItemRegistry.TRANSMUTATION_TABLE.get(), new DynamicItemRenderer());
        BuiltinItemRendererRegistry.INSTANCE.register(AMItemRegistry.VINE_LASSO.get(), new DynamicItemRenderer());

        BuiltinItemRendererRegistry.INSTANCE.register(AMItemRegistry.EFFECT_ITEM.get(), new DynamicItemRenderer());
        BuiltinItemRendererRegistry.INSTANCE.register(AMItemRegistry.FANCY_ITEM.get(), new DynamicItemRenderer());
    }
}