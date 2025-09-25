package com.github.alexthe666.alexsmobs.entity;

import com.github.alexthe666.alexsmobs.config.AMConfig;
import com.github.alexthe666.alexsmobs.entity.ai.*;
import com.github.alexthe666.alexsmobs.entity.util.Maths;
import com.github.alexthe666.alexsmobs.misc.AMBlockPos;
import com.github.alexthe666.alexsmobs.registry.AMEntityRegistry;
import com.github.alexthe666.alexsmobs.registry.AMSoundRegistry;
import com.github.alexthe666.alexsmobs.registry.AMTagRegistry;
import com.google.common.base.Predicates;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.block.BlockState;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.LootTables;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.recipe.Ingredient;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.EntityView;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.event.GameEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.UUID;

public class EntityCosmaw extends TameableEntity implements ITargetsDroppedItems, Flutterer, IFollower {

    private static final TrackedData<Float> COSMAW_PITCH = DataTracker.registerData(EntityCosmaw.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Integer> ATTACK_TICK = DataTracker.registerData(EntityCosmaw.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Boolean> SITTING = DataTracker.registerData(EntityCosmaw.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Integer> COMMAND = DataTracker.registerData(EntityCosmaw.class, TrackedDataHandlerRegistry.INTEGER);
    public float clutchProgress;
    public float prevClutchProgress;
    public float openProgress;
    public float prevOpenProgress;
    public float prevCosmawPitch;
    public float biteProgress;
    public float prevBiteProgress;
    private float stuckRot = random.nextInt(3) * 90;
    private UUID fishThrowerID;
    private int heldItemTime;
    private BlockPos lastSafeTpPosition;

    public EntityCosmaw(EntityType<EntityCosmaw> type, World lvl) {
        super(type, lvl);
        this.moveControl = new FlightMoveController(this, 1F, false, true);
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createLivingAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 20.0D)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32.0D)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 1.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.3F);
    }

    @Override
    public boolean canSpawn(WorldAccess worldIn, SpawnReason spawnReasonIn) {
        return AMEntityRegistry.rollSpawn(AMConfig.cosmawSpawnRolls, this.getRandom(), spawnReasonIn);
    }

    public static boolean canCosmawSpawn(EntityType<? extends AnimalEntity> animal, WorldAccess worldIn, SpawnReason reason, BlockPos pos, Random random) {
        return !worldIn.getBlockState(pos.down()).isAir();
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(COSMAW_PITCH, 0.0F);
        this.dataTracker.startTracking(ATTACK_TICK, 0);
        this.dataTracker.startTracking(COMMAND, 0);
        this.dataTracker.startTracking(SITTING, false);

    }

    @Override
    protected void tickInVoid() {
    }

    @Override
    public boolean tryAttack(@NotNull Entity entityIn) {
        if (this.dataTracker.get(ATTACK_TICK) == 0 && this.biteProgress == 0) {
            this.dataTracker.set(ATTACK_TICK, 5);
        }
        return true;
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new AIAttack());
        this.goalSelector.add(2, new SitGoal(this));
        this.goalSelector.add(3, new FlyingAIFollowOwner(this, 1.3D, 8.0F, 4.0F, false));
        this.goalSelector.add(4, new AIPickupOwner());
        this.goalSelector.add(5, new AnimalMateGoal(this, 1.2D));
        this.goalSelector.add(6, new AnimalAITemptDistance(this, 1.1D, Ingredient.fromTag(AMTagRegistry.COSMAW_FOODSTUFFS), false, 25) {
            @Override
            public boolean canStart() {
                return super.canStart() && EntityCosmaw.this.getMainHandStack().isEmpty();
            }

            @Override
            public boolean shouldContinue() {
                return super.shouldContinue() && EntityCosmaw.this.getMainHandStack().isEmpty();
            }
        });
        this.goalSelector.add(7, new RandomFlyGoal(this));
        this.goalSelector.add(8, new LookAtEntityGoal(this, PlayerEntity.class, 10));
        this.goalSelector.add(9, new LookAroundGoal(this));
        this.targetSelector.add(1, new CreatureAITargetItems<>(this, true));
        this.targetSelector.add(2, (new RevengeGoal(this) {
            @Override
            public boolean canStart() {
                var livingentity = this.mob.getAttacker();
                if (livingentity != null && EntityCosmaw.this.isOwner(livingentity)) {
                    return false;
                }
                return super.canStart();
            }
        }));
        this.targetSelector.add(3, new EntityAINearestTarget3D<>(this, EntityCosmicCod.class, 80, true, false, Predicates.alwaysTrue()));

    }

    @Override
    protected SoundEvent getAmbientSound() {
        return AMSoundRegistry.COSMAW_IDLE.get();
    }

    @Override
    protected SoundEvent getHurtSound(@NotNull DamageSource damageSourceIn) {
        return AMSoundRegistry.COSMAW_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return AMSoundRegistry.COSMAW_HURT.get();
    }

    @Override
    public boolean isBreedingItem(@NotNull ItemStack stack) {
        return this.isTamed() && stack.isIn(AMTagRegistry.COSMAW_BREEDABLES);
    }

    @Override
    public boolean hasNoGravity() {
        return true;
    }

    @Override
    public boolean isLeftHanded() {
        return false;
    }

    @Nullable
    @Override
    public LivingEntity getControllingPassenger() {
        return null;
    }

    public float getClampedCosmawPitch(float partialTick) {
        float f = prevCosmawPitch + (this.getCosmawPitch() - prevCosmawPitch) * partialTick;
        return MathHelper.clamp(f, -90, 90);
    }

    public float getCosmawPitch() {
        return this.dataTracker.get(COSMAW_PITCH);
    }

    public void setCosmawPitch(float pitch) {
        this.dataTracker.set(COSMAW_PITCH, pitch);
    }

    public int getCommand() {
        return this.dataTracker.get(COMMAND);
    }

    public void setCommand(int command) {
        this.dataTracker.set(COMMAND, command);
    }

    @Override
    public boolean isSitting() {
        return this.dataTracker.get(SITTING);
    }

    @Override
    public void setSitting(boolean sit) {
        this.dataTracker.set(SITTING, sit);
    }

    @Override
    public void updatePassengerPosition(@NotNull Entity passenger, @NotNull Entity.PositionUpdater moveFunc) {
        if (this.hasPassenger(passenger)) {
            final float f = this.limbAnimator.getPos();
            final float f1 = this.limbAnimator.getSpeed();
            final float bob = (float) (Math.sin(f * 0.7F) * (double) f1 * 0.0625F * 1.6F - (f1 * 0.0625F * 1.6F));
            passenger.setPos(this.getX(), this.getY() - bob + 0.3F - this.getMountedHeightOffset(), this.getZ());
        }
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound compound) {
        super.writeCustomDataToNbt(compound);
        compound.putBoolean("CosmawSitting", this.isSitting());
        compound.putInt("Command", this.getCommand());
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound compound) {
        super.readCustomDataFromNbt(compound);
        this.setSitting(compound.getBoolean("CosmawSitting"));
        this.setCommand(compound.getInt("Command"));
    }

    @Override
    public void tick() {
        super.tick();
        prevOpenProgress = openProgress;
        prevClutchProgress = clutchProgress;
        prevBiteProgress = biteProgress;
        prevCosmawPitch = this.getCosmawPitch();
        if (!this.getWorld().isClient) {
            final float f2 = (float) -((float) this.getVelocity().y * MathHelper.DEGREES_PER_RADIAN);
            this.setCosmawPitch(this.getCosmawPitch() + 0.6F * (this.getCosmawPitch() + f2) - this.getCosmawPitch());
        }

        if (isMouthOpen()) {
            if (openProgress < 5F)
                openProgress++;
        } else {
            if (openProgress > 0F)
                openProgress--;
        }

        if (hasPassengers()) {
            if (clutchProgress < 5F)
                clutchProgress++;
        } else {
            if (clutchProgress > 0F)
                clutchProgress--;
        }

        if (this.dataTracker.get(ATTACK_TICK) > 0) {
            if (biteProgress < 5F) {
                biteProgress = Math.min(5F, biteProgress + 2F);
            } else {
                if (this.getTarget() != null && this.distanceTo(this.getTarget()) < 3.3D) {
                    if (this.getTarget() instanceof EntityCosmicCod fish && !this.isTamed()) {
                        NbtCompound fishNbt = new NbtCompound();
                        fish.writeCustomDataToNbt(fishNbt);
                        fishNbt.putString("DeathLootTable", LootTables.EMPTY.toString());
                        fish.readCustomDataFromNbt(fishNbt);
                    }
                    this.getTarget().damage(this.getDamageSources().mobAttack(this), (float) this.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE));
                }
                this.dataTracker.set(ATTACK_TICK, this.dataTracker.get(ATTACK_TICK) - 1);
            }
        } else {
            if (biteProgress > 0F) {
                biteProgress -= 1F;
            }
        }
        if (!this.getMainHandStack().isEmpty()) {
            heldItemTime++;
            if (heldItemTime > 30 && canTargetItem(this.getMainHandStack())) {
                heldItemTime = 0;
                this.heal(4);
                this.emitGameEvent(GameEvent.EAT);
                this.playSound(SoundEvents.ENTITY_DOLPHIN_EAT, this.getSoundVolume(), this.getSoundPitch());
                if (this.getMainHandStack().isIn(AMTagRegistry.COSMAW_TAMEABLES) && fishThrowerID != null && !this.isTamed()) {
                    if (getRandom().nextFloat() < 0.3F) {
                        this.setTamed(true);
                        this.setCommand(1);
                        this.setOwnerUuid(this.fishThrowerID);
                        PlayerEntity player = getWorld().getPlayerByUuid(fishThrowerID);
                        if (player instanceof ServerPlayerEntity) {
                            Criteria.TAME_ANIMAL.trigger((ServerPlayerEntity) player, this);
                        }
                        this.getWorld().sendEntityStatus(this, (byte) 7);
                    } else {
                        this.getWorld().sendEntityStatus(this, (byte) 6);
                    }
                }
                if (this.getMainHandStack().getItem().hasRecipeRemainder()) {
                    this.dropItem(this.getMainHandStack().getItem().getRecipeRemainder());
                }
                this.getMainHandStack().decrement(1);
            }
        } else {
            heldItemTime = 0;
        }
        if (!this.getWorld().isClient) {
            if (this.age % 100 == 0 || lastSafeTpPosition == null) {
                BlockPos pos = getCosmawGround(this.getBlockPos());
                if (pos.getY() > 1) {
                    lastSafeTpPosition = pos;
                }
            }

            if (this.hasPassengers()) {
                if (lastSafeTpPosition != null) {
                    final double dist = this.squaredDistanceTo(Vec3d.ofCenter(lastSafeTpPosition));
                    float speed = 0.8F;
                    if(this.getY() < -40){
                        speed = 3F;
                    }
                    if (verticalCollision && dist > 14) {
                        this.setYaw(this.stuckRot);
                        if (random.nextInt(50) == 0) {
                            this.stuckRot = MathHelper.wrapDegrees(this.stuckRot + 90);

                        }
                        final float angle = (Maths.STARTING_ANGLE * stuckRot);
                        final double extraX = -2 * MathHelper.sin(MathHelper.PI + angle);
                        final double extraZ = -2 * MathHelper.cos(angle);
                        this.getMoveControl().moveTo(this.getX() + extraX, this.getY() + 2, this.getZ() + extraZ, speed);
                    } else if (lastSafeTpPosition.getY() > this.getY() + 2.3F) {
                        this.getMoveControl().moveTo(this.getX(), this.getY() + 2, this.getZ(), speed);
                    } else {
                        this.getMoveControl().moveTo(lastSafeTpPosition.getX(), lastSafeTpPosition.getY() + 2, lastSafeTpPosition.getZ(), speed);
                    }
                    if (dist < 7 && getCosmawGround(this.getBlockPos()).getY() > 1) {
                        this.removeAllPassengers();
                    }
                } else {
                    if (this.getY() < 0F) {
                        this.getVelocity().add(0, 0.75F, 0);
                    } else if (this.getY() < 80F) {
                        this.getVelocity().add(0, 0.1F, 0);
                    }
                }

            }
        }

    }

    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        final var stack = player.getStackInHand(hand);
        final var type = super.interactMob(player, hand);
        final var actionResult = stack.useOnEntity(player, this, hand);
        if (canTargetItem(stack) && this.getMainHandStack().isEmpty()) {
            final ItemStack rippedStack = stack.copy();
            rippedStack.setCount(1);
            stack.decrement(1);
            this.setStackInHand(Hand.MAIN_HAND, rippedStack);
            if (rippedStack.isIn(AMTagRegistry.COSMAW_TAMEABLES)) {
                fishThrowerID = player.getUuid();
            }
            return ActionResult.SUCCESS;
        } else if ((this.isTamed() && isOwner(player)) && !this.isBaby() && actionResult != ActionResult.SUCCESS && type != ActionResult.SUCCESS) {
            this.setCommand(this.getCommand() + 1);
            if (this.getCommand() == 3) {
                this.setCommand(0);
            }
            player.sendMessage(Text.translatable("entity.alexsmobs.all.command_" + this.getCommand(), this.getName()), true);
            final boolean sit = this.getCommand() == 2;
            this.setSitting(sit);
            return ActionResult.SUCCESS;
        }
        return type;
    }

    public boolean isMouthOpen() {
        return !this.getMainHandStack().isEmpty();
    }

    @Override
    public boolean handleFallDamage(float distance, float damageMultiplier, DamageSource source) {
        return false;
    }

    @Override
    protected void fall(double y, boolean onGroundIn, BlockState state, BlockPos pos) {
    }

    @Override
    protected EntityNavigation createNavigation(World level) {
        return new DirectPathNavigator(this, level, 0.5F);
    }

    @Override
    public boolean isTeammate(Entity entityIn) {
        if (this.isTamed()) {
            final LivingEntity livingentity = this.getOwner();
            if (entityIn == livingentity) {
                return true;
            }
            if (entityIn instanceof TameableEntity) {
                return ((TameableEntity) entityIn).isOwner(livingentity);
            }
            if (livingentity != null) {
                return livingentity.isTeammate(entityIn);
            }
        }
        return super.isTeammate(entityIn);
    }

    @Nullable
    @Override
    public PassiveEntity createChild(ServerWorld level, PassiveEntity parent) {
        return AMEntityRegistry.COSMAW.get().create(getWorld());
    }

    private BlockPos getCosmawGround(BlockPos in) {
        BlockPos position = new BlockPos(in.getX(), (int) this.getY(), in.getZ());
        while (position.getY() < 256 && !getWorld().getFluidState(position).isEmpty()) {
            position = position.up();
        }
        while (position.getY() > 1 && getWorld().isAir(position)) {
            position = position.down();
        }
        return position;
    }

    @Override
    public boolean canTargetItem(ItemStack stack) {
        return stack.isIn(AMTagRegistry.COSMAW_FOODSTUFFS);
    }

    @Override
    public void onGetItem(ItemEntity e) {
        ItemStack duplicate = e.getStack().copy();
        duplicate.setCount(1);
        if (!this.getStackInHand(Hand.MAIN_HAND).isEmpty() && !this.getWorld().isClient) {
            this.dropStack(this.getStackInHand(Hand.MAIN_HAND), 0.0F);
        }
        this.setStackInHand(Hand.MAIN_HAND, duplicate);
        Entity itemThrower = e.getOwner();
        if (e.getStack().isIn(AMTagRegistry.COSMAW_TAMEABLES) && !this.isTamed() && itemThrower != null) {
            fishThrowerID = itemThrower.getUuid();
        } else {
            fishThrowerID = null;
        }
    }

    public boolean isTargetBlocked(Vec3d target) {
        final var Vector3d = new Vec3d(this.getX(), this.getEyeY(), this.getZ());
        return this.getWorld().raycast(new RaycastContext(Vector3d, target, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, this)).getType() != HitResult.Type.MISS;
    }

    @Override
    public boolean isInAir() {
        return true;
    }

    @Override
    public boolean shouldFollow() {
        return this.getCommand() == 1 && !this.hasPassengers();
    }

    private boolean shouldWander() {
        if (this.hasPassengers()) {
            return false;
        }
        if (this.isTamed()) {
            final int command = this.getCommand();
            if (command == 2 || this.isSitting()) {
                return false;
            }
            if (command == 1 && this.getOwner() != null && this.distanceTo(this.getOwner()) < 10) {
                return true;
            }
            return command == 0;
        } else {
            return true;
        }
    }

    @Override
    public void pushAway(Entity entity) {
        if (!this.isTamed() || !(entity instanceof LivingEntity) || !isOwner((LivingEntity) entity)) {
            super.pushAway(entity);
        }
    }

    public boolean canRiderInteract() {
        return true;
    }

    public boolean shouldRiderSit() {
        return false;
    }

    @Override
    public EntityView method_48926() {
        return getWorld();
    }

    static class RandomFlyGoal extends Goal {
        private final EntityCosmaw parentEntity;
        private BlockPos target = null;

        public RandomFlyGoal(EntityCosmaw mosquito) {
            this.parentEntity = mosquito;
            this.setControls(EnumSet.of(Goal.Control.MOVE));
        }

        @Override
        public boolean canStart() {
            if (this.parentEntity.getNavigation().isIdle() && this.parentEntity.shouldWander() && this.parentEntity.getTarget() == null && this.parentEntity.getRandom().nextInt(4) == 0) {
                target = getBlockInViewCosmaw();
                if (target != null) {
                    this.parentEntity.getMoveControl().moveTo(target.getX() + 0.5D, target.getY() + 0.5D, target.getZ() + 0.5D, 1.0D);
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean shouldContinue() {
            return target != null && this.parentEntity.shouldWander() && parentEntity.getTarget() == null;
        }

        @Override
        public void stop() {
            target = null;
        }

        @Override
        public void tick() {
            if (target != null) {
                this.parentEntity.getMoveControl().moveTo(target.getX() + 0.5D, target.getY() + 0.5D, target.getZ() + 0.5D, 1.0D);
                if (parentEntity.squaredDistanceTo(Vec3d.ofCenter(target)) < 4D || this.parentEntity.horizontalCollision) {
                    target = null;
                }
            }
        }

        public BlockPos getBlockInViewCosmaw() {
            final float radius = 5 + parentEntity.getRandom().nextInt(10);
            final float neg = parentEntity.getRandom().nextBoolean() ? 1 : -1;
            final float renderYawOffset = parentEntity.getYaw();
            final float angle = (Maths.STARTING_ANGLE * renderYawOffset) + 3.15F * (parentEntity.getRandom().nextFloat() * neg);
            final double extraX = radius * MathHelper.sin(MathHelper.PI + angle);
            final double extraZ = radius * MathHelper.cos(angle);
            final BlockPos radialPos = AMBlockPos.fromCoords(parentEntity.getX() + extraX, parentEntity.getY(), parentEntity.getZ() + extraZ);
            BlockPos ground = parentEntity.getCosmawGround(radialPos);
            if (ground.getY() <= 1) {
                ground = ground.up(70 + parentEntity.random.nextInt(4));
            } else {
                ground = ground.up(2 + parentEntity.random.nextInt(2));
            }
            if (!parentEntity.isTargetBlocked(Vec3d.ofCenter(ground.up()))) {
                return ground;
            }
            return null;
        }

    }

    private class AIPickupOwner extends Goal {
        private LivingEntity owner;

        @Override
        public boolean canStart() {
            if (EntityCosmaw.this.isTamed() && EntityCosmaw.this.getOwner() != null && !EntityCosmaw.this.isSitting() && !EntityCosmaw.this.getOwner().hasVehicle()) {
                if (!EntityCosmaw.this.getOwner().isOnGround() && EntityCosmaw.this.getOwner().fallDistance > 4F) {
                    owner = EntityCosmaw.this.getOwner();
                    return true;
                }
            }
            return false;
        }

        @Override
        public void tick() {
            if (owner != null) {
                if(!owner.isFallFlying() || owner.getY() < -30F) {
                    final double dist = EntityCosmaw.this.distanceTo(owner);
                    if (dist < 3F || owner.getY() <= -50F) {
                        owner.fallDistance = 0.0F;
                        owner.startRiding(EntityCosmaw.this);
                    } else if (dist > 100F || owner.getY() <= -20F) {
                        EntityCosmaw.this.refreshPositionAfterTeleport(owner.getX(), owner.getY() - 1F, owner.getZ());
                    } else {
                        EntityCosmaw.this.getNavigation().startMovingTo(owner, 1F + Math.min(dist * 0.3F, 3));
                    }
                }
            }
        }
    }

    private class AIAttack extends Goal {

        public AIAttack() {
            this.setControls(EnumSet.of(Goal.Control.MOVE));
        }

        @Override
        public boolean canStart() {
            return EntityCosmaw.this.getTarget() != null && EntityCosmaw.this.getTarget().isAlive();
        }

        @Override
        public void tick() {
            if (EntityCosmaw.this.distanceTo(EntityCosmaw.this.getTarget()) < 3D * (EntityCosmaw.this.isBaby() ? 0.5F : 1)) {
                EntityCosmaw.this.tryAttack(EntityCosmaw.this.getTarget());
            } else {
                EntityCosmaw.this.getNavigation().startMovingTo(EntityCosmaw.this.getTarget(), 1);
            }
        }
    }
}
