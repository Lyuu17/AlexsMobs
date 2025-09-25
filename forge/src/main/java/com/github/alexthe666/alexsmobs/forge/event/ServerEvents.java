package com.github.alexthe666.alexsmobs.forge.event;

import com.github.alexthe666.alexsmobs.AlexsMobs;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = AlexsMobs.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ServerEvents {
    
    /*


    private static final Random RAND = new Random();

    @SubscribeEvent
    public static void onEntityResize(EntityEvent.Size event) {
        if (event.getEntity() instanceof PlayerEntity entity) {
            final var potions = entity.getActiveStatusEffects();
            if (event.getEntity().getWorld() != null && potions != null && !potions.isEmpty()
                && potions.containsKey(AMEffectRegistry.CLINGING)) {
                if (EffectClinging.isUpsideDown(entity)) {
                    float minus = event.getOldSize().height - event.getOldEyeHeight();
                    event.setNewEyeHeight(minus);
                }
            }
        }
    }

    @SubscribeEvent
    public void onLootLevelEvent(LootingLevelEvent event) {
        DamageSource src = event.getDamageSource();
        if (src != null) {
            if (src.getEntity() instanceof EntitySnowLeopard) {
                event.setLootingLevel(event.getLootingLevel() + 2);
            }
        }

    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onHarvestCheck(PlayerEvent.HarvestCheck event){
        if(event.getEntity() != null && event.getEntity().isHolding(AMItemRegistry.GHOSTLY_PICKAXE.get()) && ItemGhostlyPickaxe.shouldStoreInGhost(event.getEntity(), event.getEntity().getMainHandStack())){
            //stops drops from being spawned
            event.setCanHarvest(false);
        }
    }*/
}
