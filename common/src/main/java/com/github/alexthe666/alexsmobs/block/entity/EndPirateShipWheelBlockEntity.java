package com.github.alexthe666.alexsmobs.block.entity;

import com.github.alexthe666.alexsmobs.registry.AMBlockEntityRegistry;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

public class EndPirateShipWheelBlockEntity extends BlockEntity {

    private float wheelRot;
    private float prevWheelRot;
    private float targetWheelRot;
    public int ticksExisted;

    public EndPirateShipWheelBlockEntity(BlockPos pos, BlockState state) {
        super(AMBlockEntityRegistry.END_PIRATE_SHIP_WHEEL.get(), pos, state);
    }

//    @OnlyIn(Dist.CLIENT)
//    public AABB getRenderBoundingBox() {
//        return new AABB(worldPosition.offset(-2, -2, -2), worldPosition.offset(2, 2, 2));
//    }

    public void tick(World world1, BlockPos pos, BlockState state1, EndPirateShipWheelBlockEntity endPirateShipWheelBlockEntity) {
        prevWheelRot = wheelRot;
        float scale = Math.abs(Math.abs(targetWheelRot - wheelRot) / 180F);
        float progress = MathHelper.clamp(10F * scale, 1, 10);
        if(wheelRot < targetWheelRot){
            wheelRot = Math.min(targetWheelRot, wheelRot + progress);
        }
        if(wheelRot > targetWheelRot){
            wheelRot = Math.max(targetWheelRot, wheelRot - progress);
        }
        ticksExisted++;
    }

    public void rotate(boolean clockwise){
        if(Math.abs(wheelRot - targetWheelRot) < 90F){
            if(clockwise){
                targetWheelRot += 180;
            }else{
                targetWheelRot -= 180;

            }
        }
    }

    public float getWheelRot(float partialTick){
        return prevWheelRot + (wheelRot - prevWheelRot) * partialTick;
    }
}
