package com.github.alexthe666.alexsmobs.tileentity;

import com.github.alexthe666.alexsmobs.block.BlockVoidWormBeak;
import com.github.alexthe666.alexsmobs.registry.AMTileEntityRegistry;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;

public class TileEntityVoidWormBeak extends BlockEntity {

    private float chompProgress;
    private float prevChompProgress;
    public int ticksExisted;


    public TileEntityVoidWormBeak(BlockPos pos, BlockState state) {
        super(AMTileEntityRegistry.VOID_WORM_BEAK.get(), pos, state);
    }

    public static void commonTick(World level, BlockPos pos, BlockState state, TileEntityVoidWormBeak entity) {
        entity.tick();
    }

    public void tick() {
        prevChompProgress = chompProgress;
        boolean powered = false;
        if(getCachedState().getBlock() instanceof BlockVoidWormBeak){
            powered = getCachedState().get(BlockVoidWormBeak.POWERED);
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
            for (LivingEntity entity : world.getNonSpectatingEntities(LivingEntity.class, new Box((double) i - d0, (double) j - d0, (double) k - d0, (double) i + d0, (double) j + d0, (double) k + d0))) {
                entity.damage(entity.getDamageSources().generic(), 5);
            }
        }
        ticksExisted++;
    }

    public float getChompProgress(float partialTick){
        return prevChompProgress + (chompProgress - prevChompProgress) * partialTick;
    }
}
