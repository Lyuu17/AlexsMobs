package com.github.alexthe666.alexsmobs.mixin;

import com.github.alexthe666.alexsmobs.AlexsMobsClient;
import com.github.alexthe666.alexsmobs.entity.EntityBlueJay;
import com.github.alexthe666.alexsmobs.registry.AMTagRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.mob.HostileEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(WorldRenderer.class)
public class WorldRendererMixin {

    @Redirect(
            method = "render",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;getTeamColorValue()I")
    )
    private int getTeamColorValue(Entity entity) {

        if (entity instanceof HostileEntity && AlexsMobsClient.singingBlueJayId != -1){
            var blueJay = entity.getWorld().getEntityById(AlexsMobsClient.singingBlueJayId);
            if (blueJay instanceof EntityBlueJay jay && jay.isAlive() && jay.isMakingMonstersBlue()){
                return 0x4B95FE;
            }
        }
        if (entity instanceof ItemEntity itemEntity && itemEntity.getStack().isIn(AMTagRegistry.VOID_WORM_DROPS)){
            int fromColor = 0;
            int toColor = 0X21E5FF;
            float startR = (float) (fromColor >> 16 & 255) / 255.0F;
            float startG = (float) (fromColor >> 8 & 255) / 255.0F;
            float startB = (float) (fromColor & 255) / 255.0F;
            float endR = (float) (toColor >> 16 & 255) / 255.0F;
            float endG = (float) (toColor >> 8 & 255) / 255.0F;
            float endB = (float) (toColor & 255) / 255.0F;
            float f = (float) (Math.cos(0.4F * (entity.age + MinecraftClient.getInstance().getTickDelta())) + 1.0F) * 0.5F;
            float r = (endR - startR) * f + startR;
            float g = (endG - startG) * f + startG;
            float b = (endB - startB) * f + startB;
            return ((((int) (r * 255)) & 0xFF) << 16) |
                    ((((int) (g * 255)) & 0xFF) << 8) |
                    ((((int) (b * 255)) & 0xFF) << 0);
        }

        return entity.getTeamColorValue();
    }
}
