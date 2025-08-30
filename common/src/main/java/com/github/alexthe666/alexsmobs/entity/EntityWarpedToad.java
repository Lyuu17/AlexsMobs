package com.github.alexthe666.alexsmobs.entity;

import com.github.alexthe666.alexsmobs.config.AMConfig;
import com.github.alexthe666.alexsmobs.entity.ai.*;
import com.github.alexthe666.alexsmobs.entity.util.Maths;
import com.github.alexthe666.alexsmobs.registry.AMEntityRegistry;
import com.github.alexthe666.alexsmobs.registry.AMSoundRegistry;
import com.github.alexthe666.alexsmobs.registry.AMTagRegistry;
import net.minecraft.block.BlockState;
import net.minecraft.block.LeavesBlock;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.control.MoveControl;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.ai.pathing.BirdNavigation;
import net.minecraft.entity.ai.pathing.LandPathNodeMaker;
import net.minecraft.entity.ai.pathing.MobNavigation;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.recipe.Ingredient;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.*;
import net.minecraft.world.event.GameEvent;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public class EntityWarpedToad extends TameableEntity implements ITargetsDroppedItems, IFollower, ISemiAquatic {

    private static final TrackedData<Float> TONGUE_LENGTH = DataTracker.registerData(EntityWarpedToad.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Boolean> TONGUE_OUT = DataTracker.registerData(EntityWarpedToad.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> SITTING = DataTracker.registerData(EntityWarpedToad.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Integer> COMMAND = DataTracker.registerData(EntityWarpedToad.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Boolean> JUMP_ACTIVE = DataTracker.registerData(EntityWarpedToad.class, TrackedDataHandlerRegistry.BOOLEAN);
    public float blinkProgress;
    public float prevBlinkProgress;
    public float attackProgress;
    public float prevAttackProgress;
    public float sitProgress;
    public float prevSitProgress;
    public float swimProgress;
    public float prevSwimProgress;
    public float jumpProgress;
    public float prevJumpProgress;
    public float reboundProgress;
    public float prevReboundProgress;
    private boolean isLandNavigator;
    private int swimTimer = -100;

    public EntityWarpedToad(EntityType<? extends EntityWarpedToad> entityType, World world) {
        super(entityType, world);
        this.setPathfindingPenalty(PathNodeType.WATER, 0.0F);
        this.setPathfindingPenalty(PathNodeType.LAVA, 0.0F);
        switchNavigator(false);
    }

    public boolean isBased() {
        var s = Formatting.strip(this.getName().getString());
        return s != null && s.toLowerCase().contains("pepe");
    }

    public static boolean canWarpedToadSpawn(EntityType<? extends MobEntity> typeIn, ServerWorldAccess worldIn, SpawnReason reason, BlockPos pos, Random randomIn) {
        var blockpos = pos.down();
        boolean spawnBlock = worldIn.getFluidState(blockpos).isIn(FluidTags.LAVA) || worldIn.getBlockState(blockpos).isOpaque();
        return reason == SpawnReason.SPAWNER || spawnBlock;
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createLivingAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 30.0D)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32.0D)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 2.0D)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.25F)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.2F);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return AMSoundRegistry.WARPED_TOAD_IDLE.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return AMSoundRegistry.WARPED_TOAD_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return AMSoundRegistry.WARPED_TOAD_HURT.get();
    }

    @Override
    public boolean canBreatheInWater() {
        return true;
    }

    @Override
    public boolean canSpawn(WorldAccess worldIn, SpawnReason spawnReasonIn) {
        return AMEntityRegistry.rollSpawn(AMConfig.warpedToadSpawnRolls, this.getRandom(), spawnReasonIn);
    }

    @Override
    public int getLimitPerChunk() {
        return 5;
    }

    @Override
    public boolean spawnsTooManyForEachTry(int sizeIn) {
        return false;
    }

    @Override
    public boolean canSpawn(WorldView worldIn) {
        return worldIn.doesNotIntersectEntities(this);
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        if (this.isInvulnerableTo(source)) {
            return false;
        } else {
            var entity = source.getSource();
            this.setOrderedToSit(false);
            if (entity != null && this.isTamed() && !(entity instanceof PlayerEntity) && !(entity instanceof PersistentProjectileEntity)) {
                amount = (amount + 1.0F) / 3.0F;
            }
            return super.damage(source, amount);
        }
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound compound) {
        super.writeCustomDataToNbt(compound);
        compound.putBoolean("ToadSitting", this.isOrderedToSit());
        compound.putInt("Command", this.getCommand());
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound compound) {
        super.readCustomDataFromNbt(compound);
        this.setOrderedToSit(compound.getBoolean("ToadSitting"));
        this.setCommand(compound.getInt("Command"));
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new SitGoal(this));
        this.goalSelector.add(1, new TongueAttack(this));
        this.goalSelector.add(2, new FollowOwner(this, 1.3D, 4.0F, 2.0F, false));
        this.goalSelector.add(3, new AnimalAIFindWater(this));
        this.goalSelector.add(3, new AnimalAILeaveWater(this));
        this.goalSelector.add(3, new AnimalMateGoal(this, 0.8D));
        this.goalSelector.add(4, new TemptGoal(this, 1.0D, Ingredient.fromTag(AMTagRegistry.WARPED_TOAD_FOODSTUFFS), false));
        this.goalSelector.add(5, new WarpedToadAIRandomSwimming(this, 1.0D, 7));
        this.goalSelector.add(6, new AnimalAILeapRandomly(this, 50, 7){
            @Override
            public boolean canStart(){
                return super.canStart() && !EntityWarpedToad.this.isOrderedToSit();
            }
        });
        this.goalSelector.add(7, new AnimalAIWanderRanged(this, 60, 1.0D, 5, 4));
        this.goalSelector.add(10, new LookAtEntityGoal(this, PlayerEntity.class, 10.0F));
        this.goalSelector.add(11, new LookAroundGoal(this));
        this.targetSelector.add(1, new CreatureAITargetItems<>(this, false));
        this.targetSelector.add(2, new TrackOwnerAttackerGoal(this));
        this.targetSelector.add(3, new AttackWithOwnerGoal(this));
        this.targetSelector.add(4, new EntityAINearestTarget3D<>(this, LivingEntity.class, 50, false, true, AMEntityRegistry.buildPredicateFromTag(AMTagRegistry.WARPED_TOAD_TARGETS)));
        this.targetSelector.add(5, new RevengeGoal(this));
    }

    @Override
    public void travel(Vec3d travelVector) {
        if (this.isOrderedToSit()) {
            if (this.getNavigation().getCurrentPath() != null) {
                this.getNavigation().stop();
            }
            travelVector = Vec3d.ZERO;
            super.travel(travelVector);
        }else if (this.canMoveVoluntarily() && (this.isTouchingWater() || this.isInLava())) {
            this.updateVelocity(this.getMovementSpeed(), travelVector);
            this.move(MovementType.SELF, this.getVelocity());
            this.setVelocity(this.getVelocity().multiply(0.9D));
            if (this.getTarget() == null) {
                this.setVelocity(this.getVelocity().add(0.0D, -0.005D, 0.0D));
            }
        } else {
            super.travel(travelVector);
        }

    }

    @Override
    protected float getJumpVelocity() {
        return 0.5F;
    }

    @Override
    protected void fall(double y, boolean onGroundIn, BlockState state, BlockPos pos) {
    }

    @Override
    public void mobTick() {
        super.mobTick();
    }

    @Override
    public boolean isBreedingItem(ItemStack stack) {
        return stack.isIn(AMTagRegistry.WARPED_TOAD_BREEDABLES) && isTamed();
    }

    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        var itemstack = player.getStackInHand(hand);
        var type = super.interactMob(player, hand);
        if (!isTamed() && itemstack.isIn(AMTagRegistry.WARPED_TOAD_TAMEABLES)) {
            this.eat(player, hand, itemstack);
            this.emitGameEvent(GameEvent.EAT);
            this.playSound(SoundEvents.ENTITY_STRIDER_EAT, this.getSoundVolume(), this.getSoundPitch());
            if (getRandom().nextInt(3) == 0) {
                this.setOwner(player);
                this.getWorld().sendEntityStatus(this, (byte) 7);
            } else {
                this.getWorld().sendEntityStatus(this, (byte) 6);
            }
            return ActionResult.SUCCESS;
        }
        if (isTamed() && itemstack.isIn(AMTagRegistry.WARPED_TOAD_FOODSTUFFS)) {
            if (this.getHealth() < this.getMaxHealth()) {
                this.eat(player, hand, itemstack);
                this.emitGameEvent(GameEvent.EAT);
                this.playSound(SoundEvents.ENTITY_STRIDER_EAT, this.getSoundVolume(), this.getSoundPitch());
                this.heal(5);
                return ActionResult.SUCCESS;
            }
            return ActionResult.PASS;

        }
        var actionResult = itemstack.useOnEntity(player, this, hand);
        if (actionResult != ActionResult.SUCCESS && type != ActionResult.SUCCESS && isTamed() && isOwner(player) && !isBreedingItem(itemstack)) {
            this.setCommand(this.getCommand() + 1);
            if (this.getCommand() == 3) {
                this.setCommand(0);
            }
            player.sendMessage(Text.translatable("entity.alexsmobs.all.command_" + this.getCommand(), this.getName()), true);
            boolean sit = this.getCommand() == 2;
            if (sit) {
                this.setOrderedToSit(true);
                return ActionResult.SUCCESS;
            } else {
                this.setOrderedToSit(false);
                return ActionResult.SUCCESS;
            }
        }
        return type;
    }

    @Override
    public boolean isTeammate(Entity entityIn) {
        if (this.isTamed()) {
            var livingentity = this.getOwner();
            if (entityIn == livingentity) {
                return true;
            }
            if (entityIn instanceof TameableEntity tameableEntity) {
                return tameableEntity.isOwner(livingentity);
            }
            if (livingentity != null) {
                return livingentity.isTeammate(entityIn);
            }
        }

        return super.isTeammate(entityIn);
    }

    @Override
    public boolean shouldSpawnSprintingParticles() {
        return false;
    }

    private void calculateRotationYaw(double x, double z) {
        this.setYaw((float) (MathHelper.atan2(z - this.getZ(), x - this.getX()) * (double) MathHelper.DEGREES_PER_RADIAN) - 90.0F);
    }

    @Override
    public void tickMovement() {
        super.tickMovement();
        if(this.isBaby() && this.getStandingEyeHeight() > this.getHeight()){
            this.calculateDimensions();
        }
        if (!this.getWorld().isClient) {
            if (isTouchingWater() || isInLava()) {
                if (swimTimer < 0) {
                    swimTimer = 0;
                }
                swimTimer++;
            } else {
                if (swimTimer > 0) {
                    swimTimer = 0;
                }
                swimTimer--;
            }
        }
    }

    private void switchNavigator(boolean onLand) {
        if (onLand) {
            this.moveControl = new MoveControl(this);
            this.navigation = createNavigation(getWorld());
            this.isLandNavigator = true;
        } else {
            this.moveControl = new AquaticMoveController(this, 1.2F);
            this.navigation = new BoneSerpentPathNavigator(this, getWorld());
            this.isLandNavigator = false;
        }
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(TONGUE_LENGTH, 1F);
        this.dataTracker.startTracking(TONGUE_OUT, false);
        this.dataTracker.startTracking(COMMAND, 0);
        this.dataTracker.startTracking(SITTING, false);
        this.dataTracker.startTracking(JUMP_ACTIVE, false);
    }

    public int getCommand() {
        return this.dataTracker.get(COMMAND);
    }

    public void setCommand(int command) {
        this.dataTracker.set(COMMAND, command);
    }

    public boolean isOrderedToSit() {
        return this.dataTracker.get(SITTING);
    }

    public void setOrderedToSit(boolean sit) {
        this.dataTracker.set(SITTING, sit);
    }

    @Override
    public void tick() {
        super.tick();
        prevBlinkProgress = blinkProgress;
        prevAttackProgress = attackProgress;
        prevSitProgress = sitProgress;
        prevSwimProgress = swimProgress;
        prevJumpProgress = jumpProgress;
        prevReboundProgress = reboundProgress;
        this.setStepHeight(1);

        final boolean isTechnicalBlinking = this.age % 50 > 42;
        if (isTechnicalBlinking) {
            if (blinkProgress < 5F)
                blinkProgress++;
        } else {
            if (blinkProgress > 0F)
                blinkProgress--;
        }

        final boolean isTongueOut = isTongueOut();
        if (isTongueOut && attackProgress < 5F) {
            attackProgress++;
        }
        if (!this.getWorld().isClient) {
            this.dataTracker.set(JUMP_ACTIVE, !this.isOnGround());
        }
        if (this.dataTracker.get(JUMP_ACTIVE) && !isInsideWaterOrBubbleColumn()) {
            this.bodyYaw = this.getYaw();
            this.headYaw = this.getYaw();
            if (jumpProgress < 5F) {
                jumpProgress += 0.5F;
                if (reboundProgress > 0) {
                    reboundProgress--;
                }
            }
            if (jumpProgress >= 5F) {
                if (reboundProgress < 5F) {
                    reboundProgress += 0.5F;
                }
            }
        } else {
            if (reboundProgress > 0) {
                reboundProgress = Math.max(reboundProgress - 1F, 0);
            }
            if (jumpProgress > 0) {
                jumpProgress = Math.max(jumpProgress - 1F, 0);
            }
        }
        LivingEntity entityIn = this.getTarget();
        if (entityIn != null && attackProgress > 0) {
            if (isTongueOut()) {
                final double d0 = entityIn.getX() - this.getX();
                final double d2 = entityIn.getZ() - this.getZ();
                final double d1 = entityIn.getEyeY() - this.getEyeY();
                final double d3 = MathHelper.sqrt((float) (d0 * d0 + d2 * d2));
                final float f = (float) (MathHelper.atan2(d2, d0) * (double) MathHelper.DEGREES_PER_RADIAN) - 90.0F;
                final float f1 = (float) (-(MathHelper.atan2(d1, d3) * (double) MathHelper.DEGREES_PER_RADIAN));
                this.setPitch(f1);
                this.setYaw(f);
                this.bodyYaw = this.getYaw();
                this.headYaw = this.getYaw();
            } else {
                //TODO
//                if (entityIn instanceof EntityCrimsonMosquito) {
//                    ((EntityCrimsonMosquito) entityIn).setShrink(true);
//                }
                this.setPitch(0);
                final float radius = attackProgress * 0.2F * 1.2F * (getTongueLength() - getTongueLength() * 0.4F);
                final float angle = (Maths.STARTING_ANGLE * this.bodyYaw);
                final double extraX = radius * MathHelper.sin(MathHelper.PI + angle);
                final double extraZ = radius * MathHelper.cos(angle);
                final double yHelp = entityIn.getHeight();
                var minus = new Vec3d(this.getX() + extraX - this.getTarget().getX(), this.getStandingEyeHeight() - yHelp - this.getTarget().getY(), this.getZ() + extraZ - this.getTarget().getZ());
                this.getTarget().setVelocity(minus);
                if (attackProgress == 0.5F) {
                    float damage = (float) this.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE).getValue();
                    //TODO
//                    if (entityIn instanceof EntityCrimsonMosquito) {
//                        damage = Float.MAX_VALUE;
//                    }
                    entityIn.damage(this.getDamageSources().mobAttack(this), damage);
                }
            }

//            if (attackProgress == 5 && (entityIn.getHeight() < 0.89D || entityIn instanceof EntityCrimsonMosquito) && !entityIn.hasPassenger(this)) {
//            }
        }
        if (!this.getWorld().isClient && attackProgress == 5F && isTongueOut) {
            setTongueOut(false);
            attackProgress = 4F;
        }
        if (attackProgress > 0F && !isTongueOut()) {
            attackProgress -= 0.5F;
        }

        if (isOrderedToSit()) {
            if (sitProgress < 5F)
                sitProgress++;
        } else {
            if (sitProgress > 0F)
                sitProgress--;
        }

        if (shouldSwim()) {
            if (this.isLandNavigator)
                switchNavigator(false);

            if (swimProgress < 5F)
                swimProgress++;
        } else {
            if (!this.isLandNavigator)
                switchNavigator(true);

            if (swimProgress > 0F)
                swimProgress--;
        }
    }

    public boolean shouldSwim() {
        return isTouchingWater() || isInLava();
    }

    @Override
    public boolean canTargetItem(ItemStack stack) {
        return stack.isIn(AMTagRegistry.WARPED_TOAD_FOODSTUFFS);
    }

    @Override
    public void onGetItem(ItemEntity e) {
        this.heal(5);
    }

    public boolean isBlinking() {
        return blinkProgress > 1 || blinkProgress < -1 || attackProgress > 1;
    }

    @Nullable
    @Override
    public PassiveEntity createChild(ServerWorld world, PassiveEntity entity) {
        return AMEntityRegistry.WARPED_TOAD.get().create(world);
    }

    public float getTongueLength() {
        return dataTracker.get(TONGUE_LENGTH);
    }

    public void setTongueLength(float length) {
        dataTracker.set(TONGUE_LENGTH, length);
    }

    @Override
    public boolean isPushedByFluids() {
        return false;
    }

    @Override
    public boolean shouldEnterWater() {
        return swimTimer < -200 && !isOrderedToSit() && this.getCommand() != 1;
    }

    @Override
    public boolean shouldLeaveWater() {
        return swimTimer > 600 && !isOrderedToSit() && this.getCommand() != 1;
    }

    @Override
    public boolean shouldStopMoving() {
        return isOrderedToSit();
    }

    private boolean isTongueOut() {
        return this.dataTracker.get(TONGUE_OUT);
    }

    private void setTongueOut(boolean out) {
        this.dataTracker.set(TONGUE_OUT, out);
    }

    @Override
    public int getWaterSearchRange() {
        return 8;
    }

    @Override
    public boolean shouldFollow() {
        return this.getCommand() == 1;
    }

    @Override
    public EntityView method_48926() {
        return this.getWorld();
    }

    public static class TongueAttack extends Goal {
        private final EntityWarpedToad parentEntity;
        private int spitCooldown = 0;

        public TongueAttack(EntityWarpedToad toad) {
            this.parentEntity = toad;
            this.setControls(EnumSet.of(Goal.Control.MOVE, Control.LOOK));
        }

        @Override
        public boolean canStart() {
            return parentEntity.getTarget() != null && parentEntity.getPassengerList().isEmpty();
        }

        @Override
        public boolean shouldContinue() {
            return parentEntity.getTarget() != null && parentEntity.getPassengerList().isEmpty();
        }

        @Override
        public void stop() {
            spitCooldown = 20;
            parentEntity.getNavigation().stop();
        }

        @Override
        public void tick() {
            if (spitCooldown > 0) {
                spitCooldown--;
            }
            var entityIn = parentEntity.getTarget();
            if (entityIn != null) {
                final double dist = parentEntity.distanceTo(entityIn);
                if (dist < 8 && this.parentEntity.canSee(entityIn)) {
                    if (!parentEntity.isTongueOut() && parentEntity.attackProgress == 0 && spitCooldown == 0) {
                        this.parentEntity.setTongueLength((float) Math.max(1F, dist + 2F));
                        spitCooldown = 10;
                        this.parentEntity.setTongueOut(true);
                    }
                }
                this.parentEntity.getNavigation().startMovingTo(entityIn, 1.4F);
            }
        }
    }

    public static class FollowOwner extends Goal {
        private final EntityWarpedToad tameable;
        private final WorldView world;
        private final double followSpeed;
        private final float maxDist;
        private final float minDist;
        private final boolean teleportToLeaves;
        private LivingEntity owner;
        private int timeToRecalcPath;
        private float oldWaterCost;

        public FollowOwner(EntityWarpedToad p_i225711_1_, double p_i225711_2_, float p_i225711_4_, float p_i225711_5_, boolean p_i225711_6_) {
            this.tameable = p_i225711_1_;
            this.world = p_i225711_1_.getWorld();
            this.followSpeed = p_i225711_2_;
            this.minDist = p_i225711_4_;
            this.maxDist = p_i225711_5_;
            this.teleportToLeaves = p_i225711_6_;
            this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
            if (!(p_i225711_1_.getNavigation() instanceof MobNavigation) && !(p_i225711_1_.getNavigation() instanceof BirdNavigation)) {
                throw new IllegalArgumentException("Unsupported mob type for FollowOwnerGoal");
            }
        }


        @Override
        public boolean canStart() {
            var lvt_1_1_ = this.tameable.getOwner();
            if (lvt_1_1_ == null) {
                return false;
            } else if (lvt_1_1_.isSpectator()) {
                return false;
            } else if (this.tameable.isOrderedToSit() || tameable.getCommand() != 1) {
                return false;
            } else if (this.tameable.squaredDistanceTo(lvt_1_1_) < (double) (this.minDist * this.minDist)) {
                return false;
            } else {
                this.owner = lvt_1_1_;
                return true;
            }
        }

        @Override
        public boolean shouldContinue() {
            if (this.tameable.getNavigation().isIdle()) {
                return false;
            } else if (this.tameable.isOrderedToSit() || tameable.getCommand() != 1) {
                return false;
            } else {
                return this.tameable.squaredDistanceTo(this.owner) > (double) (this.maxDist * this.maxDist);
            }
        }

        @Override
        public void start() {
            this.timeToRecalcPath = 0;
            this.oldWaterCost = this.tameable.getPathfindingPenalty(PathNodeType.WATER);
            this.tameable.setPathfindingPenalty(PathNodeType.WATER, 0.0F);
        }

        @Override
        public void stop() {
            this.owner = null;
            this.tameable.getNavigation().stop();
            this.tameable.setPathfindingPenalty(PathNodeType.WATER, this.oldWaterCost);
        }

        @Override
        public void tick() {
            this.tameable.getLookControl().lookAt(this.owner, 10.0F, (float) this.tameable.getMaxLookPitchChange());
            if (--this.timeToRecalcPath <= 0) {
                this.timeToRecalcPath = 10;
                if (!this.tameable.isLeashed() && !this.tameable.hasVehicle()) {
                    if (this.tameable.squaredDistanceTo(this.owner) >= 144.0D) {
                        this.tryToTeleportNearEntity();
                    } else {
                        this.tameable.getNavigation().startMovingTo(this.owner, this.followSpeed);
                    }

                }
            }
        }

        private void tryToTeleportNearEntity() {
            var lvt_1_1_ = this.owner.getBlockPos();
            for (int lvt_2_1_ = 0; lvt_2_1_ < 10; ++lvt_2_1_) {
                int lvt_3_1_ = this.getRandomNumber(-3, 3);
                int lvt_4_1_ = this.getRandomNumber(-1, 1);
                int lvt_5_1_ = this.getRandomNumber(-3, 3);
                boolean lvt_6_1_ = this.tryToTeleportToLocation(lvt_1_1_.getX() + lvt_3_1_, lvt_1_1_.getY() + lvt_4_1_, lvt_1_1_.getZ() + lvt_5_1_);
                if (lvt_6_1_) {
                    return;
                }
            }

        }

        private boolean tryToTeleportToLocation(int p_226328_1_, int p_226328_2_, int p_226328_3_) {
            if (Math.abs((double) p_226328_1_ - this.owner.getX()) < 2.0D && Math.abs((double) p_226328_3_ - this.owner.getZ()) < 2.0D) {
                return false;
            } else if (!this.isTeleportFriendlyBlock(new BlockPos(p_226328_1_, p_226328_2_, p_226328_3_))) {
                return false;
            } else {
                this.tameable.refreshPositionAndAngles((double) p_226328_1_ + 0.5D, p_226328_2_, (double) p_226328_3_ + 0.5D, this.tameable.getYaw(), this.tameable.getPitch());
                this.tameable.getNavigation().stop();
                return true;
            }
        }

        private boolean isTeleportFriendlyBlock(BlockPos p_226329_1_) {
            var lvt_2_1_ = LandPathNodeMaker.getLandNodeType(this.world, p_226329_1_.mutableCopy());
            if (lvt_2_1_ != PathNodeType.WALKABLE) {
                return false;
            } else {
                BlockState lvt_3_1_ = this.world.getBlockState(p_226329_1_.down());
                if (!this.teleportToLeaves && lvt_3_1_.getBlock() instanceof LeavesBlock) {
                    return false;
                } else {
                    BlockPos lvt_4_1_ = p_226329_1_.subtract(this.tameable.getBlockPos());
                    return this.world.isSpaceEmpty(this.tameable, this.tameable.getBoundingBox().offset(lvt_4_1_));
                }
            }
        }

        private int getRandomNumber(int p_226327_1_, int p_226327_2_) {
            return this.tameable.getRandom().nextInt(p_226327_2_ - p_226327_1_ + 1) + p_226327_1_;
        }
    }
}
