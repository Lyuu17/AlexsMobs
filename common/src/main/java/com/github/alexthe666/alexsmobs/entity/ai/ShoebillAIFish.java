package com.github.alexthe666.alexsmobs.entity.ai;

import com.github.alexthe666.alexsmobs.entity.EntityShoebill;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.loot.LootTables;
import net.minecraft.loot.context.LootContextParameterSet;
import net.minecraft.loot.context.LootContextType;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.event.GameEvent;

import java.util.EnumSet;

public class ShoebillAIFish extends Goal {

    private final EntityShoebill bird;
    private BlockPos waterPos = null;
    private BlockPos targetPos = null;
    private int executionChance = 0;
    private final Direction[] HORIZONTALS = new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};
    private int idleTime = 0;
    private int navigateTime = 0;

    public ShoebillAIFish(EntityShoebill bird) {
        this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
        this.bird = bird;
    }

    @Override
    public void stop() {
        targetPos = null;
        waterPos = null;
        idleTime = 0;
        navigateTime = 0;
        this.bird.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (targetPos != null && waterPos != null) {
            double dist = bird.squaredDistanceTo(Vec3d.ofCenter(waterPos));
            if (dist <= 1F) {
                navigateTime = 0;
                double d0 = waterPos.getX() + 0.5D - bird.getX();
                double d2 = waterPos.getZ() + 0.5D - bird.getZ();
                float yaw = (float)(MathHelper.atan2(d2, d0) * (double)MathHelper.DEGREES_PER_RADIAN) - 90.0F;
                bird.setYaw(yaw);
                bird.headYaw= yaw;
                bird.bodyYaw = yaw;
                bird.getNavigation().stop();
                idleTime++;
                if(idleTime > 25){
                    bird.setAnimation(EntityShoebill.ANIMATION_FISH);
                }
                if(idleTime > 45 && bird.getAnimation() == EntityShoebill.ANIMATION_FISH){
                    this.bird.emitGameEvent(GameEvent.ITEM_INTERACT_START);
                    this.bird.playSound(SoundEvents.ENTITY_GENERIC_SPLASH, 0.7F, 0.5F + bird.getRandom().nextFloat());
                    this.bird.resetFishingCooldown();
                    this.spawnFishingLoot();
                    this.stop();
                }
            }else{
                navigateTime++;
                bird.getNavigation().startMovingTo(waterPos.getX(), waterPos.getY(), waterPos.getZ(), 1.2D);
            }
            if(navigateTime > 3600){
                this.stop();
            }
        }
    }

    @Override
    public boolean shouldContinue() {
        return targetPos != null && bird.fishingCooldown == 0 && bird.revengeCooldown == 0 && !bird.isFlying();
    }

    public void spawnFishingLoot() {
        double luck = 0D + bird.luckLevel * 0.5F;
        LootContextParameterSet.Builder lootcontext$builder = new LootContextParameterSet.Builder((ServerWorld) this.bird.getWorld());
        lootcontext$builder.luck((float) luck); // Forge: add player & looted bird to LootContext
        var lootparameterset$builder = LootContextType.create();
        var loottable = bird.getWorld().getServer().getLootManager().getLootTable(LootTables.FISHING_GAMEPLAY);
        var result = loottable.generateLoot(lootcontext$builder.build(lootparameterset$builder.build()));
        for (var itemstack : result) {
            var item = new ItemEntity(this.bird.getWorld(), this.bird.getX() + 0.5F, this.bird.getY(), this.bird.getZ(), itemstack);
            if (!this.bird.getWorld().isClient) {
                this.bird.getWorld().spawnEntity(item);
            }
        }
    }

    @Override
    public boolean canStart() {
        if(!bird.isFlying() && bird.fishingCooldown == 0 && bird.getRandom().nextInt(30) == 0){
            if(bird.isTouchingWater()){
                waterPos = bird.getBlockPos();
                targetPos = waterPos;
                return true;
            }else{
                waterPos = generateTarget();
                if (waterPos != null) {
                    targetPos = getLandPos(waterPos);
                    return targetPos != null;
                }
            }

        }
        return false;
    }

    public BlockPos generateTarget() {
        BlockPos blockpos = null;
        final Random random = this.bird.getRandom();
        int range = 32;
        for (int i = 0; i < 15; i++) {
            var blockpos1 = this.bird.getBlockPos().add(random.nextInt(range) - range / 2, 3, random.nextInt(range) - range / 2);
            while (this.bird.getWorld().isAir(blockpos1) && blockpos1.getY() > 1) {
                blockpos1 = blockpos1.down();
            }
            if (isConnectedToLand(blockpos1)) {
                blockpos = blockpos1;
            }
        }
        return blockpos;
    }

    public boolean isConnectedToLand(BlockPos pos) {
        if (this.bird.getWorld().getFluidState(pos).isIn(FluidTags.WATER)) {
            for (var dir : HORIZONTALS) {
                var offsetPos = pos.offset(dir);
                if (this.bird.getWorld().getFluidState(offsetPos).isEmpty() && this.bird.getWorld().getFluidState(offsetPos.up()).isEmpty()) {
                    return true;
                }
            }
        }
        return false;
    }

    public BlockPos getLandPos(BlockPos pos) {
        if (this.bird.getWorld().getFluidState(pos).isIn(FluidTags.WATER)) {
            for (var dir : HORIZONTALS) {
                var offsetPos = pos.offset(dir);
                if (this.bird.getWorld().getFluidState(offsetPos).isEmpty() && this.bird.getWorld().getFluidState(offsetPos.up()).isEmpty()) {
                    return offsetPos;
                }
            }
        }
        return null;
    }
}
