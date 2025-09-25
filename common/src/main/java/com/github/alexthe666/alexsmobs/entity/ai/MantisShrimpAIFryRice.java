package com.github.alexthe666.alexsmobs.entity.ai;

import com.github.alexthe666.alexsmobs.entity.EntityMantisShrimp;
import com.github.alexthe666.alexsmobs.registry.AMItemRegistry;
import com.github.alexthe666.alexsmobs.registry.AMTagRegistry;
import net.minecraft.block.AbstractFurnaceBlock;
import net.minecraft.block.BlockState;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.MoveToTargetPosGoal;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldView;

import java.util.EnumSet;

public class MantisShrimpAIFryRice extends MoveToTargetPosGoal {

    private final EntityMantisShrimp mantisShrimp;
    private boolean wasLitPrior = false;
    private int cookingTicks = 0;

    public MantisShrimpAIFryRice(EntityMantisShrimp entityMantisShrimp) {
        super(entityMantisShrimp, 1, 8);
        this.mantisShrimp = entityMantisShrimp;
        this.setControls(EnumSet.of(Goal.Control.MOVE));
    }

    @Override
    public void stop(){
        cookingTicks = 0;
        if(!wasLitPrior){
            BlockPos blockpos = this.getTargetPos().down();
            BlockState state = mantisShrimp.getWorld().getBlockState(blockpos);
            if(state.getBlock() instanceof AbstractFurnaceBlock && !wasLitPrior){
                mantisShrimp.getWorld().setBlockState(blockpos, state.with(AbstractFurnaceBlock.LIT, false));
            }
        }
        super.stop();
    }

    @Override
    public void tick() {
        super.tick();
        BlockPos blockpos = this.getTargetPos().down();
        if(this.hasReached()){
            BlockState state = mantisShrimp.getWorld().getBlockState(blockpos);
            if(mantisShrimp.punchProgress == 0){
                mantisShrimp.punch();
            }
            if(state.getBlock() instanceof AbstractFurnaceBlock && !wasLitPrior){
                mantisShrimp.getWorld().setBlockState(blockpos, state.with(AbstractFurnaceBlock.LIT, true));
            }
            cookingTicks++;
            if(cookingTicks > 200){
                cookingTicks = 0;
                ItemStack rice = new ItemStack(AMItemRegistry.SHRIMP_FRIED_RICE.get());
                rice.setCount(mantisShrimp.getMainHandStack().getCount());
                mantisShrimp.setStackInHand(Hand.MAIN_HAND, rice);

            }
        }else{
            cookingTicks = 0;
        }
    }

    @Override
    public boolean canStart() {
        return this.mantisShrimp.getMainHandStack().isIn(AMTagRegistry.SHRIMP_RICE_FRYABLES) && !mantisShrimp.isSitting() && super.canStart();
    }

    @Override
    public boolean shouldContinue() {
        return this.mantisShrimp.getMainHandStack().isIn(AMTagRegistry.SHRIMP_RICE_FRYABLES) && !mantisShrimp.isSitting() && super.shouldContinue();
    }

    @Override
    public double getDesiredDistanceToTarget() {
        return 3.9F;
    }

    @Override
    protected boolean isTargetPos(WorldView worldIn, BlockPos pos) {
        if (!worldIn.isAir(pos.up())) {
            return false;
        } else {
            BlockState blockstate = worldIn.getBlockState(pos);
            if(blockstate.getBlock() instanceof AbstractFurnaceBlock){
                wasLitPrior = blockstate.get(AbstractFurnaceBlock.LIT);
                return true;
            }
            return blockstate.isIn(BlockTags.CAMPFIRES);
        }
    }


}
