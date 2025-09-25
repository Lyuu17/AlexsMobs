package com.github.alexthe666.alexsmobs.block.entity;

import com.github.alexthe666.alexsmobs.block.EndPirateDoorBlock;
import com.github.alexthe666.alexsmobs.registry.AMBlockEntityRegistry;
import com.github.alexthe666.alexsmobs.registry.AMSoundRegistry;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.event.GameEvent;

public class EndPirateDoorBlockEntity extends BlockEntity {

    private float openProgress;
    private float prevOpenProgress;
    private float wiggleProgress;
    private float prevWiggleProgress;
    public int wiggleTime;
    public int ticksExisted;

    public EndPirateDoorBlockEntity(BlockPos pos, BlockState state) {
        super(AMBlockEntityRegistry.END_PIRATE_DOOR.get(), pos, state);
        if(state.getBlock() instanceof EndPirateDoorBlock && state.get(EndPirateDoorBlock.OPEN)){
            openProgress = 1F;
            prevOpenProgress = 1F;
        }
    }


//    @OnlyIn(Dist.CLIENT)
//    public net.minecraft.world.phys.AABB getRenderBoundingBox() {
//        return new net.minecraft.world.phys.AABB(worldPosition, worldPosition.offset(1, 3, 1));
//    }

    public void tick() {
        prevOpenProgress = openProgress;
        prevWiggleProgress = wiggleProgress;
        boolean opened = false;
        if(getCachedState().getBlock() instanceof EndPirateDoorBlock){
            opened = getCachedState().get(EndPirateDoorBlock.OPEN);
        }
        if(opened && openProgress == 0F || !opened && openProgress == 1F){
            this.world.emitGameEvent(GameEvent.BLOCK_ACTIVATE, this.getPos(), GameEvent.Emitter.of(this.getCachedState()));
            this.world.playSound(null, this.getPos(), AMSoundRegistry.END_PIRATE_DOOR.get(), SoundCategory.BLOCKS, 1F, 1F);
        }
        if(opened && openProgress < 1F){
            openProgress += 0.25F;
        }
        if(!opened && openProgress > 0F){
            openProgress -= 0.25F;
        }
        if(openProgress >= 1F && prevOpenProgress < 1F || openProgress <= 0F && prevOpenProgress > 0F){
            wiggleTime = 5;
        }
        if(wiggleTime > 0){
            wiggleTime--;
            if(wiggleProgress < 1F){
                wiggleProgress += 0.25F;
            }
        }else{
            if(wiggleProgress > 0F){
                wiggleProgress -= 0.25F;
            }
        }
        ticksExisted++;
    }

    public float getOpenProgress(float partialTick){
        return prevOpenProgress + (openProgress - prevOpenProgress) * partialTick;
    }

    public float getWiggleProgress(float partialTick){
        return prevWiggleProgress + (wiggleProgress - prevWiggleProgress) * partialTick;
    }
}
