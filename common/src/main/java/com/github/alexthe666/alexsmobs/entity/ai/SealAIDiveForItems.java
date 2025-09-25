package com.github.alexthe666.alexsmobs.entity.ai;

import com.github.alexthe666.alexsmobs.entity.EntitySeal;
import com.github.alexthe666.alexsmobs.registry.AMTagRegistry;
import net.minecraft.block.BlockState;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.context.LootContextParameterSet;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.loot.context.LootContextTypes;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;

import java.util.List;

public class SealAIDiveForItems extends Goal {

    private final EntitySeal seal;
    private PlayerEntity thrower;
    private BlockPos digPos;
    private boolean returnToPlayer = false;
    private int digTime = 0;
    public static final Identifier SEAL_REWARD = new Identifier("alexsmobs","gameplay/seal_reward");

    public SealAIDiveForItems(EntitySeal seal) {
        this.seal = seal;
    }

    private static List<ItemStack> getItemStacks(EntitySeal seal) {
        var loottable = seal.getWorld().getServer().getLootManager().getLootTable(SEAL_REWARD);
        return loottable.generateLoot((new LootContextParameterSet.Builder((ServerWorld) seal.getWorld()))
                .add(LootContextParameters.THIS_ENTITY, seal)
                .build(LootContextTypes.BARTER));
    }

    @Override
    public boolean canStart() {
        if (seal.feederUUID == null || seal.getWorld().getPlayerByUuid(seal.feederUUID) == null || seal.revengeCooldown > 0) {
            return false;
        }
        thrower = seal.getWorld().getPlayerByUuid(seal.feederUUID);
        digPos = genDigPos();
        return thrower != null && digPos != null;
    }

    @Override
    public boolean shouldContinue() {
        return seal.getTarget() == null && seal.revengeCooldown == 0 && seal.getAttacker() == null && thrower != null && seal.feederUUID != null && digPos != null && seal.getWorld().getFluidState(digPos.up()).isIn(FluidTags.WATER);
    }

    @Override
    public void tick() {
        seal.setBasking(false);
        if (returnToPlayer) {
            seal.getNavigation().startMovingTo(thrower, 1D);
            if (seal.distanceTo(thrower) < 2D) {
                ItemStack stack = seal.getMainHandStack().copy();
                seal.setStackInHand(Hand.MAIN_HAND, ItemStack.EMPTY);
                var item = seal.dropStack(stack);
                if (item != null) {
                    double d0 = thrower.getX() - this.seal.getX();
                    double d1 = thrower.getEyeY() - this.seal.getEyeY();
                    double d2 = thrower.getZ() - this.seal.getZ();
                    double lvt_7_1_ = MathHelper.sqrt((float) (d0 * d0 + d2 * d2));
                    float pitch = (float) (-(MathHelper.atan2(d1, lvt_7_1_) * 57.2957763671875D));
                    float yaw = (float) (MathHelper.atan2(d2, d0) * (double) MathHelper.DEGREES_PER_RADIAN) - 90.0F;
                    float f8 = MathHelper.sin(pitch * MathHelper.RADIANS_PER_DEGREE);
                    float f2 = MathHelper.cos(pitch * MathHelper.RADIANS_PER_DEGREE);
                    float f3 = MathHelper.sin(yaw * MathHelper.RADIANS_PER_DEGREE);
                    float f4 = MathHelper.cos(yaw * MathHelper.RADIANS_PER_DEGREE);
                    float f5 = seal.getRandom().nextFloat() * MathHelper.TAU;
                    float f6 = 0.02F * seal.getRandom().nextFloat();
                    item.setVelocity((double) (-f3 * f2 * 0.5F) + Math.cos(f5) * (double) f6, -f8 * 0.2F + 0.1F + (seal.getRandom().nextFloat() - seal.getRandom().nextFloat()) * 0.1F, (double) (f4 * f2 * 0.5F) + Math.sin(f5) * (double) f6);
                }
                seal.feederUUID = null;
                stop();
            }
        } else {
            double dist = seal.squaredDistanceTo(Vec3d.ofCenter(digPos.up()));
            double d0 = digPos.getX() + 0.5 - this.seal.getX();
            double d1 = digPos.getY() + 0.5 - this.seal.getEyeY();
            double d2 = digPos.getZ() + 0.5 - this.seal.getZ();
            float f = (float)(MathHelper.atan2(d2, d0) * 57.2957763671875D) - 90.0F;

            if (dist < 2) {
                seal.getNavigation().stop();
                digTime++;
                if(digTime  % 5 == 0){
                    SoundEvent sound = seal.getWorld().getBlockState(digPos).getSoundGroup().getHitSound();
                    seal.playSound(sound, 1, 0.5F + seal.getRandom().nextFloat() * 0.5F);
                }
                if (digTime >= 100) {
                    List<ItemStack> lootList = getItemStacks(seal);
                    if (!lootList.isEmpty()) {
                        ItemStack copy = lootList.remove(0);
                        copy = copy.copy();
                        this.seal.setStackInHand(Hand.MAIN_HAND, copy);
                        for (ItemStack stack : lootList) {
                            this.seal.dropStack(stack.copy());
                        }
                        this.returnToPlayer = true;
                    }
                    seal.setDigging(false);
                    digTime = 0;
                }else{
                    seal.setDigging(true);
                }
            }else{
                seal.setDigging(false);
                seal.getNavigation().startMovingTo(digPos.getX(), digPos.getY(), digPos.getZ(), 1);
                seal.setYaw(f);
            }
        }
    }

    @Override
    public void stop() {
        seal.setDigging(false);
        digPos = null;
        thrower = null;
        digTime = 0;
        returnToPlayer = false;
        seal.fishFeedings = 0;
        if(!seal.getMainHandStack().isEmpty()){
            seal.dropStack(seal.getMainHandStack().copy());
            seal.setStackInHand(Hand.MAIN_HAND, ItemStack.EMPTY);
        }
    }

    private BlockPos genSeafloorPos(BlockPos parent) {
        var world = seal.getWorld();
        final Random random = this.seal.getRandom();
        int range = 15;
        for (int i = 0; i < 15; i++) {
            BlockPos seafloor = parent.add(random.nextInt(range) - range / 2, 0, random.nextInt(range) - range / 2);
            while (world.getFluidState(seafloor).isIn(FluidTags.WATER) && seafloor.getY() > 1) {
                seafloor = seafloor.down();
            }
            BlockState state = world.getBlockState(seafloor);
            if (state.isIn(AMTagRegistry.SEAL_DIGABLES)) {
                return seafloor;
            }
        }
        return null;
    }

    private BlockPos genDigPos() {
        final Random random = this.seal.getRandom();
        int range = 15;
        if (seal.isTouchingWater()) {
            return genSeafloorPos(this.seal.getBlockPos());
        } else {
            for (int i = 0; i < 15; i++) {
                BlockPos blockpos1 = this.seal.getBlockPos().add(random.nextInt(range) - range / 2, 3, random.nextInt(range) - range / 2);
                while (this.seal.getWorld().isAir(blockpos1) && blockpos1.getY() > 1) {
                    blockpos1 = blockpos1.down();
                }
                if (this.seal.getWorld().getFluidState(blockpos1).isIn(FluidTags.WATER)) {
                    BlockPos pos3 = genSeafloorPos(blockpos1);
                    if (pos3 != null) {
                        return pos3;
                    }
                }
            }
        }
        return null;
    }
}
