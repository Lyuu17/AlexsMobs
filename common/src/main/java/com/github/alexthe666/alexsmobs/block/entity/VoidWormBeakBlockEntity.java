package com.github.alexthe666.alexsmobs.block.entity;

import com.github.alexthe666.alexsmobs.block.VoidWormBeakBlock;
import com.github.alexthe666.alexsmobs.registry.AMBlockEntityRegistry;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;

public class VoidWormBeakBlockEntity extends BlockEntity {

    private float chompProgress;
    private float prevChompProgress;
    public int ticksExisted;

    public VoidWormBeakBlockEntity(BlockPos pos, BlockState state) {
        super(AMBlockEntityRegistry.VOID_WORM_BEAK.get(), pos, state);
    }

    public void tick(World level, BlockPos pos, BlockState state, VoidWormBeakBlockEntity blockEntity) {
        prevChompProgress = chompProgress;
        boolean powered = false;
        if(getCachedState().getBlock() instanceof VoidWormBeakBlock){
            powered = getCachedState().get(VoidWormBeakBlock.POWERED);
        }
        if(powered && chompProgress < 5F){
            chompProgress++;
        }
        if(!powered && chompProgress > 0F){
            chompProgress--;
        }
        if(chompProgress >= 5F && !world.isClient && ticksExisted % 5 == 0){
            float i = this.getPos().getX() + 0.5F;
            float j = this.getPos().getY() + 0.5F;
            float k = this.getPos().getZ() + 0.5F;
            float d0 = 0.5F;
            for (var entity : world.getNonSpectatingEntities(LivingEntity.class, new Box((double) i - d0, (double) j - d0, (double) k - d0, (double) i + d0, (double) j + d0, (double) k + d0))) {
                entity.damage(entity.getDamageSources().generic(), 5);
            }
        }
        ticksExisted++;
    }

    public float getChompProgress(float partialTick){
        return prevChompProgress + (chompProgress - prevChompProgress) * partialTick;
    }
}
