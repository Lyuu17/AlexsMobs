package com.github.alexthe666.alexsmobs;

import com.github.alexthe666.alexsmobs.client.ClientLayerRegistry;
import com.github.alexthe666.alexsmobs.event.ClientEvents;
import com.github.alexthe666.alexsmobs.item.ItemBloodSprayer;
import com.github.alexthe666.alexsmobs.item.ItemHemolymphBlaster;
import com.github.alexthe666.alexsmobs.item.ItemTarantulaHawkElytra;
import com.github.alexthe666.alexsmobs.item.ItemTendonWhip;
import com.github.alexthe666.alexsmobs.registry.*;
import dev.architectury.registry.item.ItemPropertiesRegistry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public class AlexsMobsClient {

    private static int pupfishChunkX = 0;
    private static int pupfishChunkZ = 0;

    public static int renderStaticScreenFor = 0;
    public static long lastStaticTick = -1;
    public static int singingBlueJayId = -1;

    public static void init() {
        ClientEvents.init();

        AMCreativeTabRegistry.DEF_REG.register();
        AMMenuRegistry.init(false);
        AMRendererRegistry.registerEntityRenderers();
        AMRendererRegistry.registerBlockEntityRenderers();
        AMModelLayerRegistry.register();

        try {
            ItemPropertiesRegistry.register(AMItemRegistry.BLOOD_SPRAYER.get(), new Identifier("empty"), (stack, world, livingEntity, j) -> {
                return !ItemBloodSprayer.isUsable(stack) || livingEntity instanceof PlayerEntity && ((PlayerEntity) livingEntity).getItemCooldownManager().isCoolingDown(AMItemRegistry.BLOOD_SPRAYER.get()) ? 1.0F : 0.0F;
            });
            ItemPropertiesRegistry.register(AMItemRegistry.HEMOLYMPH_BLASTER.get(), new Identifier("empty"), (stack, world, livingEntity, j) -> {
                return !ItemHemolymphBlaster.isUsable(stack) || livingEntity instanceof PlayerEntity && ((PlayerEntity) livingEntity).getItemCooldownManager().isCoolingDown(AMItemRegistry.HEMOLYMPH_BLASTER.get()) ? 1.0F : 0.0F;
            });
            ItemPropertiesRegistry.register(AMItemRegistry.TARANTULA_HAWK_ELYTRA.get(), new Identifier("broken"), (stack, world, livingEntity, j) -> {
                return ItemTarantulaHawkElytra.isUsable(stack) ? 0.0F : 1.0F;
            });
            ItemPropertiesRegistry.register(AMItemRegistry.SHIELD_OF_THE_DEEP.get(), new Identifier("blocking"), (stack, world, livingEntity, j) -> {
                return livingEntity != null && livingEntity.isUsingItem() && livingEntity.getActiveItem() == stack ? 1.0F : 0.0F;
            });
            ItemPropertiesRegistry.register(AMItemRegistry.SOMBRERO.get(), new Identifier("silly"), (stack, world, livingEntity, j) -> {
                return AlexsMobs.isAprilFools() ? 1.0F : 0.0F;
            });
            ItemPropertiesRegistry.register(AMItemRegistry.TENDON_WHIP.get(), new Identifier("active"), (stack, world, holder, j) -> {
                return ItemTendonWhip.isActive(stack, holder) ? 1.0F : 0.0F;
            });
            ItemPropertiesRegistry.register(AMItemRegistry.PUPFISH_LOCATOR.get(), new Identifier("in_chunk"), (stack, world, entity, j) -> {
                int x = pupfishChunkX * 16;
                int z = pupfishChunkZ * 16;
                if (entity != null && entity.getX() >= x && entity.getX() <= x + 16 && entity.getZ() >= z && entity.getZ() <= z + 16) {
                    return 1.0F;
                }
                return 0.0F;
            });
            ItemPropertiesRegistry.register(AMItemRegistry.SKELEWAG_SWORD.get(), new Identifier("blocking"), (stack, world, livingEntity, j) -> {
                return livingEntity != null && livingEntity.isUsingItem() && livingEntity.getActiveItem() == stack ? 1.0F : 0.0F;
            });
        } catch (Exception e) {
            AlexsMobs.LOGGER.warn("Could not load item models for weapons");
        }

        ClientLayerRegistry.init();
    }

    public static void process() {

    }

    public static void setPupfishChunkForItem(int chunkX, int chunkZ) {
        pupfishChunkX = chunkX;
        pupfishChunkZ = chunkZ;
    }

    public static boolean isFarFromCamera(double x, double y, double z) {
        var minecraftClient = MinecraftClient.getInstance();
        return minecraftClient.gameRenderer.getCamera().getPos().squaredDistanceTo(x, y, z) >= 256.0D;
    }

    public static void processVisualFlag(Entity entity, int flag) {
        if (entity == MinecraftClient.getInstance().player && flag == 87) {
            renderStaticScreenFor = 60;
        }
    }
}
