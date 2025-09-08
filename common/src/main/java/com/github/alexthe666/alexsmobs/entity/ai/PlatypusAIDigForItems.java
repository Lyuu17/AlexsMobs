package com.github.alexthe666.alexsmobs.entity.ai;

import com.github.alexthe666.alexsmobs.entity.EntityPlatypus;
import com.github.alexthe666.alexsmobs.registry.AMTagRegistry;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.context.LootContextParameterSet;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.loot.context.LootContextTypes;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.event.GameEvent;

import java.util.List;

public class PlatypusAIDigForItems extends Goal {

    public static final Identifier PLATYPUS_REWARD = new Identifier("alexsmobs", "gameplay/platypus_reward");
    public static final Identifier PLATYPUS_REWARD_CHARGED = new Identifier("alexsmobs", "gameplay/platypus_supercharged_reward");
    private EntityPlatypus platypus;
    private BlockPos digPos;
    private int generatePosCooldown = 0;
    private int digTime = 0;
    private int maxDroppedItems = 3;

    public PlatypusAIDigForItems(EntityPlatypus platypus) {
        this.platypus = platypus;
    }

    private static List<ItemStack> getItemStacks(EntityPlatypus platypus) {
        var loottable = platypus.getWorld().getServer().getLootManager().getLootTable(platypus.superCharged ? PLATYPUS_REWARD_CHARGED : PLATYPUS_REWARD);
        return loottable.generateLoot((new LootContextParameterSet.Builder((ServerWorld) platypus.getWorld())).add(LootContextParameters.THIS_ENTITY, platypus).build(LootContextTypes.BARTER));
    }

    @Override
    public boolean canStart() {
        if (!platypus.isSensing()) {
            return false;
        }
        if(generatePosCooldown == 0){
            generatePosCooldown = 20 + platypus.getRandom().nextInt(20);
            digPos = genDigPos();
            maxDroppedItems = 2 + platypus.getRandom().nextInt(5);
            return digPos != null;
        }else{
            generatePosCooldown--;
            return false;
        }

    }

    @Override
    public boolean shouldContinue() {
        return platypus.getTarget() == null && platypus.isSensing() && platypus.getAttacker() == null && digPos != null && platypus.getWorld().getBlockState(digPos).isIn(AMTagRegistry.PLATYPUS_DIGABLES) && platypus.getWorld().getFluidState(digPos.up()).isIn(FluidTags.WATER);
    }

    @Override
    public void tick() {
        double dist = platypus.squaredDistanceTo(Vec3d.ofCenter(digPos.up()));
        double d0 = digPos.getX() + 0.5 - this.platypus.getX();
        double d1 = digPos.getY() + 0.5 - this.platypus.getEyeY();
        double d2 = digPos.getZ() + 0.5 - this.platypus.getZ();
        float f = (float) (MathHelper.atan2(d2, d0) * 57.2957763671875D) - 90.0F;
        if (dist < 2) {
            platypus.setVelocity(platypus.getVelocity().add(0, -0.01F, 0));
            platypus.getNavigation().stop();
            digTime++;
            if (digTime % 5 == 0) {
                SoundEvent sound = platypus.getWorld().getBlockState(digPos).getSoundGroup().getHitSound();
                platypus.emitGameEvent(GameEvent.BLOCK_ACTIVATE);
                platypus.playSound(sound, 1, 0.5F + platypus.getRandom().nextFloat() * 0.5F);
            }
            int itemDivis = (int) Math.floor(100F / maxDroppedItems);
            if(digTime % itemDivis == 0){
                List<ItemStack> lootList = getItemStacks(platypus);
                if (lootList.size() > 0) {
                    for (ItemStack stack : lootList) {
                        var e = this.platypus.dropStack(stack.copy());
                        e.velocityDirty = true;
                        e.setVelocity(e.getVelocity().multiply(0.2, 0.2, 0.2));
                    }
                }
            }
            if (digTime >= 100) {
                platypus.setSensing(false);
                platypus.setDigging(false);
                digTime = 0;
            } else {
                platypus.setDigging(true);
            }
        } else {
            platypus.setDigging(false);

            platypus.getNavigation().startMovingTo(digPos.getX(), digPos.getY() + 1, digPos.getZ(), 1);

            platypus.setYaw(f);
        }

    }

    @Override
    public void stop() {
        generatePosCooldown = 0;
        platypus.setSensing(false);
        platypus.setDigging(false);
        digPos = null;
        digTime = 0;
    }

    private BlockPos genSeafloorPos(BlockPos parent) {
        var world = platypus.getWorld();
        final var random = this.platypus.getRandom();
        int range = 15;
        for (int i = 0; i < 15; i++) {
            BlockPos seafloor = parent.add(random.nextInt(range) - range / 2, 0, random.nextInt(range) - range / 2);
            while (world.getFluidState(seafloor).isIn(FluidTags.WATER) && seafloor.getY() > 1) {
                seafloor = seafloor.down();
            }
            var state = world.getBlockState(seafloor);
            if (state.isIn(AMTagRegistry.PLATYPUS_DIGABLES)) {
                return seafloor;
            }
        }
        return null;
    }

    private BlockPos genDigPos() {
        final var random = this.platypus.getRandom();
        int range = 15;
        if (platypus.isTouchingWater()) {
            return genSeafloorPos(this.platypus.getBlockPos());
        } else {
            for (int i = 0; i < 15; i++) {
                var blockpos1 = this.platypus.getBlockPos().add(random.nextInt(range) - range / 2, 3, random.nextInt(range) - range / 2);
                while (this.platypus.getWorld().isAir(blockpos1) && blockpos1.getY() > 1) {
                    blockpos1 = blockpos1.down();
                }
                if (this.platypus.getWorld().getFluidState(blockpos1).isIn(FluidTags.WATER)) {
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
