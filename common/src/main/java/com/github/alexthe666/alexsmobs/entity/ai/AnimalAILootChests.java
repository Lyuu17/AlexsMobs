package com.github.alexthe666.alexsmobs.entity.ai;

import com.github.alexthe666.alexsmobs.AlexsMobs;
import com.github.alexthe666.alexsmobs.config.AMConfig;
import com.github.alexthe666.alexsmobs.entity.EntityRaccoon;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.entity.ai.goal.MoveToTargetPosGoal;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.WorldView;

import java.util.ArrayList;

public class AnimalAILootChests extends MoveToTargetPosGoal {

    private final AnimalEntity entity;
    private final ILootsChests chestLooter;
    private boolean hasOpenedChest = false;

    public AnimalAILootChests(AnimalEntity entity, int range) {
        super(entity, 1.0F, range);
        this.entity = entity;
        this.chestLooter = (ILootsChests) entity;
    }

    public boolean isChestRaidable(WorldView world, BlockPos pos) {
        if (world.getBlockState(pos).getBlock() instanceof BlockWithEntity) {
            var block = world.getBlockState(pos).getBlock();
            boolean listed = false;
            var entity = world.getBlockEntity(pos);
            if (entity instanceof Inventory inventory) {
                try {
                    if (!inventory.isEmpty() && chestLooter.isLootable(inventory)) {
                        return true;
                    }
                } catch (Exception e) {
                    AlexsMobs.LOGGER.warn("Alex's Mobs stopped a " + entity.getClass().getSimpleName() + " from causing a crash during access");
                    e.printStackTrace();
                }
            }
        }
        return false;
    }

    @Override
    public boolean canStart() {
        if (this.entity instanceof TameableEntity && ((TameableEntity) entity).isTamed()) {
            return false;
        }
        if (!AMConfig.raccoonsStealFromChests) {
            return false;
        }
        if (!this.entity.getStackInHand(Hand.MAIN_HAND).isEmpty()) {
            return false;
        }
        //FIXME forge
//        if (this.nextStartTick <= 0) {
//            if (!net.minecraftforge.event.ForgeEventFactory.getMobGriefingEvent(this.entity.getWorld(), this.entity)) {
//                return false;
//            }
//        }
        return super.canStart();
    }

    @Override
    public boolean shouldContinue() {
        return super.shouldContinue() && this.entity.getStackInHand(Hand.MAIN_HAND).isEmpty();
    }

    public boolean hasLineOfSightChest() {
        var raytraceresult = entity.getWorld().raycast(new RaycastContext(entity.getCameraPosVec(1.0F), new Vec3d(targetPos.getX() + 0.5, targetPos.getY() + 0.5, targetPos.getZ() + 0.5), RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, entity));
        if (raytraceresult instanceof BlockHitResult) {
            var pos = raytraceresult.getBlockPos();
            return pos.equals(targetPos) || entity.getWorld().isAir(pos) || this.entity.getWorld().getBlockEntity(pos) == this.entity.getWorld().getBlockEntity(targetPos);
        }
        return true;
    }

    public ItemStack getFoodFromInventory(Inventory inventory, Random random) {
        var items = new ArrayList<ItemStack>();
        for (int i = 0; i < inventory.size(); i++) {
            var stack = inventory.getStack(i);
            if (chestLooter.shouldLootItem(stack)) {
                items.add(stack);
            }
        }
        if (items.isEmpty()) {
            return ItemStack.EMPTY;
        } else if (items.size() == 1) {
            return items.get(0);
        } else {
            return items.get(random.nextInt(items.size() - 1));
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (this.targetPos != null) {
            var te = this.entity.getWorld().getBlockEntity(this.targetPos);
            if (te instanceof Inventory feeder) {
                double distance = this.entity.squaredDistanceTo(this.targetPos.getX() + 0.5F, this.targetPos.getY() + 0.5F, this.targetPos.getZ() + 0.5F);
                if (hasLineOfSightChest()) {
                    if (this.hasReached() && distance <= 3) {
                        toggleChest(feeder, false);
                        ItemStack stack = getFoodFromInventory(feeder, this.entity.getWorld().random);
                        if (stack == ItemStack.EMPTY) {
                            this.stop();
                        } else {
                            ItemStack duplicate = stack.copy();
                            duplicate.setCount(1);
                            if (!this.entity.getStackInHand(Hand.MAIN_HAND).isEmpty() && !this.entity.getWorld().isClient) {
                                this.entity.dropStack(this.entity.getStackInHand(Hand.MAIN_HAND), 0.0F);
                            }
                            this.entity.setStackInHand(Hand.MAIN_HAND, duplicate);
                            if (entity instanceof EntityRaccoon) {
                                ((EntityRaccoon) entity).lookForWaterBeforeEatingTimer = 10;
                            }
                            stack.decrement(1);
                            this.stop();
                        }
                    } else {
                        if (distance < 5 && !hasOpenedChest) {
                            hasOpenedChest = true;
                            toggleChest(feeder, true);
                        }
                    }
                }

            }

        }
    }

    @Override
    public void stop() {
        super.stop();
        if (this.targetPos != null) {
            var te = this.entity.getWorld().getBlockEntity(this.targetPos);
            if (te instanceof Inventory chest) {
                toggleChest(chest, false);
            }
        }
        this.targetPos = BlockPos.ORIGIN;
        this.hasOpenedChest = false;
    }

    @Override
    protected boolean isTargetPos(WorldView worldIn, BlockPos pos) {
        return pos != null && isChestRaidable(worldIn, pos);
    }

    public void toggleChest(Inventory te, boolean open) {
        if (te instanceof ChestBlockEntity chest) {
            if (open) {
                this.entity.getWorld().addSyncedBlockEvent(this.targetPos, chest.getCachedState().getBlock(), 1, 1);
            } else {
                this.entity.getWorld().addSyncedBlockEvent(this.targetPos, chest.getCachedState().getBlock(), 1, 0);
            }
            this.entity.getWorld().updateNeighborsAlways(targetPos, chest.getCachedState().getBlock());
            this.entity.getWorld().updateNeighborsAlways(targetPos.down(), chest.getCachedState().getBlock());
        }
    }
}
