package com.github.alexthe666.alexsmobs.entity.ai;

import com.github.alexthe666.alexsmobs.block.LeafcutterAntChamberBlock;
import com.github.alexthe666.alexsmobs.block.LeafcutterAnthillBlock;
import com.github.alexthe666.alexsmobs.block.entity.LeafcutterAnthillBlockEntity;
import com.github.alexthe666.alexsmobs.entity.EntityAnteater;
import com.github.alexthe666.alexsmobs.entity.EntityLeafcutterAnt;
import com.github.alexthe666.alexsmobs.misc.AMBlockPos;
import com.github.alexthe666.alexsmobs.platform.PlatformEvent;
import com.github.alexthe666.alexsmobs.registry.AMBlockRegistry;
import com.github.alexthe666.alexsmobs.registry.AMItemRegistry;
import com.iafenvoy.uranus.animation.IAnimatedEntity;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BeehiveBlockEntity;
import net.minecraft.command.argument.EntityAnchorArgumentType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.ai.goal.MoveToTargetPosGoal;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.context.LootContextParameterSet;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.loot.context.LootContextTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.WorldView;

import java.util.List;

public class AnteaterAIRaidNest extends MoveToTargetPosGoal {

    public static final Identifier ANTEATER_REWARD = new Identifier("alexsmobs", "gameplay/anteater_reward");
    private final EntityAnteater anteater;
    private int idleAtHiveTime = 0;
    private boolean isAboveDestinationAnteater;
    private boolean shootTongue;
    private int maxEatingTime = 0;

    public AnteaterAIRaidNest(EntityAnteater anteater) {
        super(anteater, 1D, 32, 8);
        this.anteater = anteater;
    }

    private static List<ItemStack> getItemStacks(EntityAnteater anteater) {
        var loottable = anteater.getWorld().getServer().getLootManager().getLootTable(ANTEATER_REWARD);
        return loottable.generateLoot((new LootContextParameterSet.Builder((ServerWorld) anteater.getWorld()))
                .add(LootContextParameters.THIS_ENTITY, anteater)
                .build(LootContextTypes.BARTER));
    }

    private void dropDigItems(){
        var lootList = getItemStacks(anteater);
        if (!lootList.isEmpty()) {
            for (ItemStack stack : lootList) {
                var e = this.anteater.dropStack(stack.copy());
                e.velocityDirty = true;
                e.setVelocity(e.getVelocity().multiply(0.2, 0.2, 0.2));
            }
        }
    }

    @Override
    public boolean canStart() {
        return !anteater.isBaby() && super.canStart() && anteater.eatAntCooldown <= 0;
    }

    @Override
    public boolean shouldContinue() {
        return super.shouldContinue() && anteater.eatAntCooldown <= 0;
    }

    @Override
    public void start() {
        super.start();
        maxEatingTime = 150 + anteater.getRandom().nextInt(200);
    }

    @Override
    public void stop() {
        super.stop();
        idleAtHiveTime = 0;
        maxEatingTime = 150 + anteater.getRandom().nextInt(200);
        anteater.setLeaning(false);
        anteater.resetAntCooldown();
    }

    @Override
    public double getDesiredDistanceToTarget() {
        return 1.2D;
    }

    @Override
    public void tick() {
        super.tick();
        BlockPos blockpos = this.getTargetPos();
        if (!isWithinXZDist(blockpos, this.mob.getPos(), this.getDesiredDistanceToTarget())) {
            this.isAboveDestinationAnteater = false;
            ++this.tryingTime;
            if (this.shouldResetPath()) {
                this.mob.getNavigation().startMovingTo((double) ((float) blockpos.getX()) + 0.5D, blockpos.getY(), (double) ((float) blockpos.getZ()) + 0.5D, this.speed);
            }
        } else {
            this.isAboveDestinationAnteater = true;
            --this.tryingTime;
        }

        if (this.hasReached()) {
            anteater.lookAt(EntityAnchorArgumentType.EntityAnchor.EYES, new Vec3d(targetPos.getX() + 0.5D, targetPos.getY() - 1, targetPos.getZ() + 0.5));
            if (this.idleAtHiveTime >= 20 && this.idleAtHiveTime % 20 == 0) {
                shootTongue = anteater.getRandom().nextInt(2) == 0;
                if(shootTongue){
                    this.eatHive();
                }else{
                    this.breakHiveEffect();
                }
            }
            ++this.idleAtHiveTime;
            if (shootTongue && anteater.getAnimation() == IAnimatedEntity.NO_ANIMATION) {
                anteater.setLeaning(false);
                anteater.setAnimation(EntityAnteater.ANIMATION_TOUNGE_IDLE);
            }else if (anteater.getAnimation() == IAnimatedEntity.NO_ANIMATION) {
                anteater.setLeaning(true);
                anteater.setAnimation(anteater.getRandom().nextBoolean() ? EntityAnteater.ANIMATION_SLASH_L : EntityAnteater.ANIMATION_SLASH_R);
            }
            if(this.idleAtHiveTime > maxEatingTime){
                stop();
            }
        }

    }

    private boolean isWithinXZDist(BlockPos blockpos, Vec3d positionVec, double distance) {
        return blockpos.getSquaredDistance(AMBlockPos.fromCoords(positionVec.x, blockpos.getY(), positionVec.z)) < distance * distance;
    }

    @Override
    protected boolean hasReached() {
        return this.isAboveDestinationAnteater;
    }

    private void breakHiveEffect(){
        if (PlatformEvent.getMobGriefingEvent(anteater.getWorld(), anteater)) {
            var blockstate = anteater.getWorld().getBlockState(this.targetPos);
            if (blockstate.isOf(AMBlockRegistry.LEAFCUTTER_ANTHILL.get())) {
                if (anteater.getWorld().getBlockEntity(this.targetPos) instanceof LeafcutterAnthillBlockEntity anthill) {
                    anthill.angerAntsBecauseAnteater(anteater, blockstate, BeehiveBlockEntity.BeeState.EMERGENCY);
                    anteater.getWorld().breakBlock(targetPos, false);
                    if (blockstate.getBlock() instanceof LeafcutterAnthillBlock) {
                        anteater.getWorld().setBlockState(targetPos, blockstate);
                    }
                    dropDigItems();
                }
            }else if(blockstate.isOf(AMBlockRegistry.LEAFCUTTER_ANT_CHAMBER.get())){
                anteater.getWorld().breakBlock(targetPos, false);
                anteater.getWorld().setBlockState(targetPos, blockstate);
            }
        }
    }

    private void eatHive() {
        if (PlatformEvent.getMobGriefingEvent(anteater.getWorld(), anteater)) {
            var blockstate = anteater.getWorld().getBlockState(this.targetPos);
            if (blockstate.isOf(AMBlockRegistry.LEAFCUTTER_ANTHILL.get())) {
                if (anteater.getWorld().getBlockEntity(this.targetPos) instanceof LeafcutterAnthillBlockEntity anthill) {
                    final var rand = this.anteater.getRandom();
                    anthill.angerAntsBecauseAnteater(anteater, blockstate, BeehiveBlockEntity.BeeState.EMERGENCY);
                    anteater.getWorld().updateComparators(this.targetPos, blockstate.getBlock());
                    if(!anthill.hasNoAnts()){
                        var state = anthill.shrinkFungus();
                        if(state != null && state.isOf(AMBlockRegistry.LEAFCUTTER_ANT_CHAMBER.get()) && state.get(LeafcutterAntChamberBlock.FUNGUS) >= 5){
                            var stack = new ItemStack(AMItemRegistry.GONGYLIDIA.get());
                            var itementity = new ItemEntity(anteater.getWorld(), targetPos.getX() + rand.nextFloat(), targetPos.getY() + rand.nextFloat(), targetPos.getZ() + rand.nextFloat(), stack);
                            itementity.setToDefaultPickupDelay();
                            anteater.getWorld().spawnEntity(itementity);
                        }
                        anteater.setAntOnTongue(true);
                    }
                }
            }else if(blockstate.isOf(AMBlockRegistry.LEAFCUTTER_ANT_CHAMBER.get())){
                anteater.getWorld().breakBlock(targetPos, false);
                if(blockstate.get(LeafcutterAntChamberBlock.FUNGUS) >= 5){
                    final var rand = this.anteater.getRandom();
                    ItemStack stack = new ItemStack(AMItemRegistry.GONGYLIDIA.get());
                    var itementity = new ItemEntity(anteater.getWorld(), targetPos.getX() + rand.nextFloat(), targetPos.getY() + rand.nextFloat(), targetPos.getZ() + rand.nextFloat(), stack);
                    itementity.setToDefaultPickupDelay();
                    anteater.getWorld().spawnEntity(itementity);
                }
                anteater.getWorld().setBlockState(targetPos, Blocks.COARSE_DIRT.getDefaultState());
                anteater.setAntOnTongue(true);
            }
            double d0 = 15;
            for (var leafcutter : anteater.getWorld().getNonSpectatingEntities(EntityLeafcutterAnt.class, new Box((double) targetPos.getX() - d0, (double) targetPos.getY() - d0, (double) targetPos.getZ() - d0, (double) targetPos.getX() + d0, (double) targetPos.getY() + d0, (double) targetPos.getZ() + d0))) {
                leafcutter.setAngerTime(100);
                leafcutter.setTarget(anteater);
                leafcutter.setStayOutOfHiveCountdown(400);
            }
        }
    }

    @Override
    protected boolean isTargetPos(WorldView worldIn, BlockPos pos) {
        return worldIn.getBlockState(pos).isOf(AMBlockRegistry.LEAFCUTTER_ANT_CHAMBER.get()) || worldIn.getBlockState(pos).isOf(AMBlockRegistry.LEAFCUTTER_ANTHILL.get()) && worldIn.getBlockEntity(pos) instanceof LeafcutterAnthillBlockEntity && this.isValidAnthill(pos, (LeafcutterAnthillBlockEntity)worldIn.getBlockEntity(pos));
    }

    private boolean isValidAnthill(BlockPos pos, LeafcutterAnthillBlockEntity blockEntity) {
        return blockEntity.hasAtleastThisManyAnts(2);
    }
}
