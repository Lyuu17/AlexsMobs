package com.github.alexthe666.alexsmobs.entity;

import com.github.alexthe666.alexsmobs.block.BlockReptileEgg;
import com.github.alexthe666.alexsmobs.config.AMConfig;
import com.github.alexthe666.alexsmobs.entity.ai.*;
import com.github.alexthe666.alexsmobs.entity.util.Maths;
import com.github.alexthe666.alexsmobs.registry.*;
import com.iafenvoy.uranus.animation.Animation;
import com.iafenvoy.uranus.animation.AnimationHandler;
import com.iafenvoy.uranus.animation.IAnimatedEntity;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.control.MoveControl;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
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

import java.util.function.Predicate;

public class EntityCrocodile extends TameableEntity implements IAnimatedEntity, ISemiAquatic {

    public static final Animation ANIMATION_LUNGE = Animation.create(23);
    public static final Animation ANIMATION_DEATHROLL = Animation.create(40);
    public static final Predicate<LivingEntity> NOT_CREEPER = (entity) -> entity.isAlive() && !(entity instanceof CreeperEntity);
    private static final TrackedData<Byte> CLIMBING = DataTracker.registerData(EntityCrocodile.class, TrackedDataHandlerRegistry.BYTE);
    private static final TrackedData<Boolean> SITTING = DataTracker.registerData(EntityCrocodile.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> DESERT = DataTracker.registerData(EntityCrocodile.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> HAS_EGG = DataTracker.registerData(EntityCrocodile.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> IS_DIGGING = DataTracker.registerData(EntityCrocodile.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Integer> STUN_TICKS = DataTracker.registerData(EntityCrocodile.class, TrackedDataHandlerRegistry.INTEGER);
    public float groundProgress = 0;
    public float prevGroundProgress = 0;
    public float swimProgress = 0;
    public float prevSwimProgress = 0;
    public float baskingProgress = 0;
    public float prevBaskingProgress = 0;
    public float grabProgress = 0;
    public float prevGrabProgress = 0;
    public int baskingType = 0;
    public boolean forcedSit = false;
    private int baskingTimer = 0;
    private int swimTimer = -1000;
    private int ticksSinceInWater = 0;
    private int passengerTimer = 0;
    private boolean isLandNavigator;
    private boolean hasSpedUp = false;
    private int animationTick;
    private Animation currentAnimation;

    public EntityCrocodile(EntityType<? extends EntityCrocodile> type, World worldIn) {
        super(type, worldIn);
        this.setPathfindingPenalty(PathNodeType.WATER, 0.0F);
        this.setPathfindingPenalty(PathNodeType.WATER_BORDER, 0.0F);
        switchNavigator(false);
        this.baskingType = random.nextInt(1);
    }

    public static boolean canCrocodileSpawn(EntityType type, WorldAccess worldIn, SpawnReason reason, BlockPos pos, Random randomIn) {
        boolean spawnBlock = worldIn.getBlockState(pos.down()).isIn(AMTagRegistry.CROCODILE_SPAWNS);
        return spawnBlock && pos.getY() < worldIn.getSeaLevel() + 4;
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createLivingAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 30.0D)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 15)
                .add(EntityAttributes.GENERIC_ARMOR, 8.0D)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 10.0D)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.4F)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.25F);
    }

    @Override
    public boolean canSpawn(WorldAccess worldIn, SpawnReason spawnReasonIn) {
        return AMEntityRegistry.rollSpawn(AMConfig.crocSpawnRolls, this.getRandom(), spawnReasonIn);
    }

    @Override
    public int getLimitPerChunk() {
        return 2;
    }

    @Override
    public boolean spawnsTooManyForEachTry(int sizeIn) {
        return false;
    }

    @Override
    protected void onGrowUp() {
        super.onGrowUp();
        if (!this.isBaby() && this.getWorld().getGameRules().getBoolean(GameRules.DO_MOB_LOOT)) {
            this.dropStack(new ItemStack(AMItemRegistry.CROCODILE_SCUTE.get(), random.nextInt(1) + 1), 1);
        }
    }

    @Nullable
    @Override
    public EntityData initialize(ServerWorldAccess worldIn, LocalDifficulty difficultyIn, SpawnReason reason, @Nullable EntityData spawnDataIn, @Nullable NbtCompound dataTag) {
        this.setDesert(this.isBiomeDesert(worldIn, this.getBlockPos()));
        return super.initialize(worldIn, difficultyIn, reason, spawnDataIn, dataTag);
    }

    private boolean isBiomeDesert(WorldAccess worldIn, BlockPos position) {
        return worldIn.getBiome(position).isIn(AMTagRegistry.SPAWNS_DESERT_CROCODILES);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return isBaby() ? AMSoundRegistry.CROCODILE_BABY.get() : AMSoundRegistry.CROCODILE_IDLE.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return AMSoundRegistry.CROCODILE_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return AMSoundRegistry.CROCODILE_HURT.get();
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound compound) {
        super.writeCustomDataToNbt(compound);
        compound.putBoolean("CrocodileSitting", this.isSitting());
        compound.putBoolean("Desert", this.isDesert());
        compound.putBoolean("ForcedToSit", this.forcedSit);
        compound.putInt("BaskingStyle", this.baskingType);
        compound.putInt("BaskingTimer", this.baskingTimer);
        compound.putInt("SwimTimer", this.swimTimer);
        compound.putInt("StunTimer", this.getStunTicks());
        compound.putBoolean("HasEgg", this.hasEgg());
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound compound) {
        super.readCustomDataFromNbt(compound);
        this.setSitting(compound.getBoolean("CrocodileSitting"));
        this.setDesert(compound.getBoolean("Desert"));
        this.forcedSit = compound.getBoolean("ForcedToSit");
        this.baskingType = compound.getInt("BaskingStyle");
        this.baskingTimer = compound.getInt("BaskingTimer");
        this.swimTimer = compound.getInt("SwimTimer");
        this.setHasEgg(compound.getBoolean("HasEgg"));
        this.setStunTicks(compound.getInt("StunTimer"));
    }

    private void switchNavigator(boolean onLand) {
        if (onLand) {
            this.moveControl = new MoveControl(this);
            this.navigation = new GroundPathNavigatorWide(this, getWorld());
            this.isLandNavigator = true;
        } else {
            this.moveControl = new AquaticMoveController(this, 1F);
            this.navigation = new SemiAquaticPathNavigator(this, getWorld());
            this.isLandNavigator = false;
        }
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(SITTING, false);
        this.dataTracker.startTracking(DESERT, false);
        this.dataTracker.startTracking(HAS_EGG, false);
        this.dataTracker.startTracking(IS_DIGGING, false);
        this.dataTracker.startTracking(CLIMBING, (byte) 0);
        this.dataTracker.startTracking(STUN_TICKS, 0);
    }

    public boolean isBesideClimbableBlock() {
        return (this.dataTracker.get(CLIMBING) & 1) != 0;
    }

    public void setBesideClimbableBlock(boolean climbing) {
        byte b0 = this.dataTracker.get(CLIMBING);
        if (climbing) {
            b0 = (byte) (b0 | 1);
        } else {
            b0 = (byte) (b0 & -2);
        }
        this.dataTracker.set(CLIMBING, b0);
    }

    @Override
    public void tick() {
        super.tick();
        this.prevGroundProgress = groundProgress;
        this.prevSwimProgress = swimProgress;
        this.prevBaskingProgress = baskingProgress;
        this.prevGrabProgress = grabProgress;

        final boolean ground = !this.isTouchingWater();
        final boolean groundAnimate = !this.isTouchingWater();
        final boolean basking = groundAnimate && this.isSitting();
        final boolean grabbing = !this.getPassengerList().isEmpty();

        if (!ground && this.isLandNavigator) {
            switchNavigator(false);
        }
        if (ground && !this.isLandNavigator) {
            switchNavigator(true);
        }

        if (groundAnimate) {
            if (this.groundProgress < 10F)
                this.groundProgress++;

            if (this.swimProgress > 0F)
                this.swimProgress--;
        } else {
            if (this.groundProgress > 0F)
                this.groundProgress--;

            if (this.swimProgress < 10F)
                this.swimProgress++;
        }

        if (basking) {
            if (this.baskingProgress < 10F)
                this.baskingProgress++;
        } else {
            if (this.baskingProgress > 0F)
                this.baskingProgress--;
        }

        if (grabbing) {
            if (this.grabProgress < 10F)
                this.grabProgress++;
        } else {
            if (this.grabProgress > 0F)
                this.grabProgress--;
        }

        if (this.getTarget() == null) {
            if (hasSpedUp) {
                hasSpedUp = false;
                this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).setBaseValue(0.25F);
            }
        } else {
            if (!hasSpedUp) {
                hasSpedUp = true;
                this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).setBaseValue(0.28F);
            }
        }

        if (!this.getWorld().isClient) {
            this.setBesideClimbableBlock(this.horizontalCollision);
        }
        if (baskingTimer < 0) {
            baskingTimer++;
        }
        if (passengerTimer > 0 && this.getPassengerList().isEmpty()) {
            passengerTimer = 0;
        }
        if (!this.getWorld().isClient) {
            if (isTouchingWater()) {
                swimTimer++;
                ticksSinceInWater = 0;
            } else {
                ticksSinceInWater++;
                swimTimer--;
            }

            if (!this.isTouchingWater() && this.isOnGround()) {
                if (!this.isTamed()) {
                    if (!this.isSitting() && baskingTimer == 0 && this.getTarget() == null && this.getNavigation().isIdle()) {
                        this.setSitting(true);
                        this.baskingTimer = 1000 + random.nextInt(750);
                    }
                    if (this.isSitting() && (baskingTimer <= 0 || this.getTarget() != null || swimTimer < -1000)) {
                        this.setSitting(false);
                        this.baskingTimer = -2000 - random.nextInt(750);
                    }
                    if (this.isSitting() && baskingTimer > 0) {
                        baskingTimer--;
                    }
                }
            }
            if (this.getStunTicks() == 0 && this.isAlive() && this.getTarget() != null && this.getAnimation() == ANIMATION_LUNGE && (getWorld().getDifficulty() != Difficulty.PEACEFUL || !(this.getTarget() instanceof PlayerEntity)) && this.getAnimationTick() > 5 && this.getAnimationTick() < 9) {
                final float f1 = this.getYaw() * MathHelper.RADIANS_PER_DEGREE;
                this.setVelocity(this.getVelocity().add(-MathHelper.sin(f1) * 0.02F, 0.0D, MathHelper.cos(f1) * 0.02F));
                if (this.distanceTo(this.getTarget()) < 3.5F && this.canSee(this.getTarget())) {
                    boolean flag = this.getTarget().isBlocking();
                    if (!flag) {
                        if (this.getTarget().getWidth() < this.getWidth() && this.getPassengerList().isEmpty() && !this.getTarget().isSneaking()) {
                            this.getTarget().startRiding(this, true);
                        }
                    }
                    if (flag) {
                        if (this.getTarget() instanceof final PlayerEntity player) {
                            this.damageShieldFor(player, (float) this.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE).getBaseValue());
                        }
                        if (this.getStunTicks() == 0) {
                            this.setStunTicks(25 + random.nextInt(20));
                        }
                    } else {
                        this.getTarget().damage(this.getDamageSources().mobAttack(this), (float) this.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE).getBaseValue());
                    }
                    this.playSound(AMSoundRegistry.CROCODILE_BITE.get(), this.getSoundVolume(), this.getSoundPitch());

                }
            }
            if (this.isAlive() && this.getTarget() != null && this.isTouchingWater() && (getWorld().getDifficulty() != Difficulty.PEACEFUL || !(this.getTarget() instanceof PlayerEntity))) {
                if (this.getTarget().getVehicle() != null && this.getTarget().getVehicle() == this) {
                    if (this.getAnimation() == NO_ANIMATION) {
                        this.setAnimation(ANIMATION_DEATHROLL);
                    }
                    if (this.getAnimation() == ANIMATION_DEATHROLL && this.getAnimationTick() % 10 == 0 && this.distanceTo(this.getTarget()) < 5D) {
                        this.getTarget().damage(this.getDamageSources().mobAttack(this), 5);
                    }
                }
            }
        }
        if (this.getAnimation() == ANIMATION_DEATHROLL) {
            this.getNavigation().stop();
        }
        if (this.isInLove() && this.getTarget() != null) {
            this.setTarget(null);
        }
        if (this.getStunTicks() > 0) {
            this.setStunTicks(this.getStunTicks() - 1);
            if (this.getWorld().isClient) {
                final float angle = (Maths.STARTING_ANGLE * this.bodyYaw);
                final double headX = 1.5F * getScaleFactor() * MathHelper.sin(MathHelper.PI + angle);
                final double headZ = 1.5F * getScaleFactor() * MathHelper.cos(angle);
                for (int i = 0; i < 5; i++) {
                    final float innerAngle = (Maths.STARTING_ANGLE * (this.bodyYaw + age * 5) * (i + 1));
                    final double extraX = 0.5F * MathHelper.sin((float) (Math.PI + innerAngle));
                    final double extraZ = 0.5F * MathHelper.cos(innerAngle);
                    getWorld().addParticle(ParticleTypes.CRIT, true, this.getX() + headX + extraX, this.getEyeY() + 0.5F, this.getZ() + headZ + extraZ, 0, 0, 0);
                }
            }
        }
        AnimationHandler.INSTANCE.updateAnimations(this);
    }

    protected void damageShieldFor(PlayerEntity holder, float damage) {
        // FIXME forge
//        if (holder.getActiveItem().canPerformAction(ToolActions.SHIELD_BLOCK)) {
//            if (!this.getWorld().isClient) {
//                holder.incrementStat(Stats.USED.getOrCreateStat(holder.getActiveItem().getItem()));
//            }
//
//            if (damage >= 3.0F) {
//                int i = 1 + MathHelper.floor(damage);
//                var hand = holder.getUsedItemHand();
//                holder.getActiveItem().hurtAndBreak(i, holder, (p_213833_1_) -> {
//                    p_213833_1_.broadcastBreakEvent(hand);
//                    net.minecraftforge.event.ForgeEventFactory.onafterBreakItem(holder, holder.getActiveItem(), hand);
//                });
//                if (holder.getActiveItem().isEmpty()) {
//                    if (hand == Hand.MAIN_HAND) {
//                        holder.equipStack(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
//                    } else {
//                        holder.equipStack(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
//                    }
//                    holder.playSound(SoundEvents.ITEM_SHIELD_BREAK, 0.8F, 0.8F + this.getWorld().random.nextFloat() * 0.4F);
//                }
//            }
//
//        }
    }

    @Override
    protected boolean isImmobile() {
        return super.isImmobile() || this.getStunTicks() > 0;
    }

    public boolean canRiderInteract() {
        return true;
    }

    public boolean shouldRiderSit() {
        return false;
    }

    @Override
    public boolean isTeammate(Entity entityIn) {
        if (this.isTamed()) {
            var livingentity = this.getOwner();
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

    @Override
    public void updatePassengerPosition(Entity passenger, Entity.PositionUpdater moveFunc) {
        if (!this.getPassengerList().isEmpty()) {
            this.bodyYaw = MathHelper.wrapDegrees(this.getYaw() - 180F);
        }
        if (this.hasPassenger(passenger)) {
            final float radius = 2F;
            final float angle = (Maths.STARTING_ANGLE * this.bodyYaw);
            final double extraX = radius * MathHelper.sin(MathHelper.PI + angle);
            final double extraZ = radius * MathHelper.cos(angle);
            passenger.setPos(this.getX() + extraX, this.getY() + 0.1F, this.getZ() + extraZ);
            passengerTimer++;
            if (this.isAlive() && passengerTimer > 0 && passengerTimer % 40 == 0) {
                passenger.damage(this.getDamageSources().mobAttack(this), 2);
            }
        }
    }

    @Nullable
    public LivingEntity getControllingPassenger() {
        return null;
    }

    @Override
    public boolean isClimbing() {
        return isTouchingWater() && this.isBesideClimbableBlock();
    }

    @Override
    public boolean isPushedByFluids() {
        return false;
    }

    @Override
    public boolean canSpawn(WorldView worldIn) {
        return worldIn.doesNotIntersectEntities(this);
    }

    @Override
    public boolean tryAttack(Entity entityIn) {
        if (this.getAnimation() == NO_ANIMATION && this.getPassengerList().isEmpty() && this.getStunTicks() == 0) {
            this.setAnimation(ANIMATION_LUNGE);
        }
        return true;
    }

    @Override
    public void travel(Vec3d travelVector) {
        if(isSitting()){
            super.travel(Vec3d.ZERO);
        }else if (this.canMoveVoluntarily() && this.isTouchingWater()) {
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
    public boolean isInvulnerableTo(DamageSource source) {
        return source.isOf(DamageTypes.DROWN) || source.isOf(DamageTypes.IN_WALL)  || super.isInvulnerableTo(source);
    }

    @Override
    public boolean canBreatheInWater() {
        return true;
    }

    @Override
    public float getPathfindingFavor(BlockPos pos, WorldView worldIn) {
        return super.getPathfindingFavor(pos, worldIn);
    }

    @Override
    public boolean shouldLeaveWater() {
        if (!this.getPassengerList().isEmpty()) {
            return false;
        }
        if (this.getTarget() != null && !this.getTarget().isTouchingWater()) {
            return true;
        }
        return swimTimer > 600;
    }

    @Override
    public boolean shouldStopMoving() {
        return this.getAnimation() == ANIMATION_DEATHROLL || this.isSitting();
    }

    @Override
    public int getWaterSearchRange() {
        return this.getPassengerList().isEmpty() ? 15 : 45;
    }

    @Override
    public boolean isSitting() {
        return this.dataTracker.get(SITTING);
    }

    @Override
    public void setSitting(boolean sit) {
        this.dataTracker.set(SITTING, sit);
    }

    public boolean isDesert() {
        return this.dataTracker.get(DESERT);
    }

    public void setDesert(boolean desert) {
        this.dataTracker.set(DESERT, desert);
    }

    public boolean hasEgg() {
        return this.dataTracker.get(HAS_EGG);
    }

    private void setHasEgg(boolean hasEgg) {
        this.dataTracker.set(HAS_EGG, hasEgg);
    }

    public boolean isDigging() {
        return this.dataTracker.get(IS_DIGGING);
    }

    private void setDigging(boolean isDigging) {
        this.dataTracker.set(IS_DIGGING, isDigging);
    }

    public int getStunTicks() {
        return this.dataTracker.get(STUN_TICKS);
    }

    private void setStunTicks(int stun) {
        this.dataTracker.set(STUN_TICKS, stun);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new SitGoal(this));
        this.goalSelector.add(1, new MateGoal(this, 1.0D));
        this.goalSelector.add(1, new LayEggGoal(this, 1.0D));
        this.goalSelector.add(2, new BreatheAirGoal(this));
        this.goalSelector.add(2, new AnimalAIFindWater(this));
        this.goalSelector.add(2, new AnimalAILeaveWater(this));
        this.goalSelector.add(4, new CrocodileAIMelee(this, 1, true));
        this.goalSelector.add(5, new CrocodileAIRandomSwimming(this, 1.0D, 7));
        this.goalSelector.add(6, new LookAroundGoal(this));
        this.goalSelector.add(7, new LookAtEntityGoal(this, PlayerEntity.class, 6.0F));
        this.targetSelector.add(1, (new AnimalAIHurtByTargetNotBaby(this)).setGroupRevenge());
        this.targetSelector.add(2, new TrackOwnerAttackerGoal(this));
        this.targetSelector.add(3, new AttackWithOwnerGoal(this));
        this.targetSelector.add(4, new EntityAINearestTarget3D<>(this, PlayerEntity.class, 80, false, true, null) {
            @Override
            public boolean canStart() {
                return !isBaby() && !isTamed() && getWorld().getDifficulty() != Difficulty.PEACEFUL && super.canStart();
            }
        });
        this.targetSelector.add(5, new EntityAINearestTarget3D<>(this, LivingEntity.class, 180, false, true, AMEntityRegistry.buildPredicateFromTag(AMTagRegistry.CROCODILE_TARGETS)) {
            @Override
            public boolean canStart() {
                return !isBaby() && !isTamed() && super.canStart();
            }
        });
        this.targetSelector.add(6, new EntityAINearestTarget3D<>(this, HostileEntity.class, 180, false, true, NOT_CREEPER) {
            @Override
            public boolean canStart() {
                return !isBaby() && isTamed() && super.canStart();
            }
        });
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        if (this.isInvulnerableTo(source)) {
            return false;
        } else {
            var entity = source.getAttacker();
            this.setSitting(false);
            if (entity != null && this.isTamed() && !(entity instanceof PlayerEntity) && !(entity instanceof PersistentProjectileEntity)) {
                amount = (amount + 1.0F) / 3.0F;
            }
            return super.damage(source, amount);
        }
    }

    @Nullable
    @Override
    public PassiveEntity createChild(ServerWorld world, PassiveEntity entity) {
        return AMEntityRegistry.CROCODILE.get().create(world);
    }

    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        final ItemStack itemstack = player.getStackInHand(hand);
        final var item = itemstack.getItem();
        if (item == Items.NAME_TAG) {
            return super.interactMob(player, hand);
        }
        if (isTamed() && item.isFood() && item.getFoodComponent() != null && item.getFoodComponent().isMeat() && this.getHealth() < this.getMaxHealth()) {
            this.eat(player, hand, itemstack);
            this.heal(10);
            this.emitGameEvent(GameEvent.EAT);
            this.playSound(SoundEvents.ENTITY_GENERIC_EAT, this.getSoundVolume(), this.getSoundPitch());
            return ActionResult.SUCCESS;
        }
        final var type = super.interactMob(player, hand);
        final var actionResult = itemstack.useOnEntity(player, this, hand);
        if (actionResult != ActionResult.SUCCESS && type != ActionResult.SUCCESS && isTamed() && isOwner(player) && !isBreedingItem(itemstack)) {
            if (this.isSitting()) {
                this.forcedSit = false;
                this.setSitting(false);
            } else {
                this.forcedSit = true;
                this.setSitting(true);
            }
            return ActionResult.SUCCESS;
        }
        return type;
    }

    @Override
    public void setTarget(@Nullable LivingEntity entitylivingbaseIn) {
        if (!this.isBaby()) {
            super.setTarget(entitylivingbaseIn);
        }
    }

    @Override
    public boolean isBreedingItem(ItemStack stack) {
        return stack.isIn(AMTagRegistry.CROCODILE_BREEDABLES);
    }

    @Override
    public boolean shouldEnterWater() {
        if (!this.getPassengerList().isEmpty()) {
            return true;
        }
        return this.getTarget() == null && !this.isSitting() && this.baskingTimer <= 0 && !shouldLeaveWater() && swimTimer <= -1000;
    }

    @Override
    public Animation getAnimation() {
        return currentAnimation;
    }

    @Override
    public void setAnimation(Animation animation) {
        currentAnimation = animation;
    }

    @Override
    public int getAnimationTick() {
        return animationTick;
    }

    @Override
    public void setAnimationTick(int tick) {
        animationTick = tick;
    }

    @Override
    public Animation[] getAnimations() {
        return new Animation[]{ANIMATION_LUNGE, ANIMATION_DEATHROLL};
    }

    public boolean isCrowned() {
        String s = Formatting.strip(this.getName().getString());
        return s != null && (s.toLowerCase().contains("crown") || s.toLowerCase().contains("king") || s.toLowerCase().contains("rool"));
    }

    @Override
    public EntityView method_48926() {
        return getWorld();
    }

    static class MateGoal extends AnimalMateGoal {
        private final EntityCrocodile crocodile;

        MateGoal(EntityCrocodile crocodile, double speedIn) {
            super(crocodile, speedIn);
            this.crocodile = crocodile;
        }

        @Override
        public boolean canStart() {
            return super.canStart() && !this.crocodile.hasEgg();
        }

        @Override
        protected void breed() {
            var serverplayerentity = this.animal.getLovingPlayer();
            if (serverplayerentity == null && this.mate.getLovingPlayer() != null) {
                serverplayerentity = this.mate.getLovingPlayer();
            }

            if (serverplayerentity != null) {
                serverplayerentity.incrementStat(Stats.ANIMALS_BRED);
                Criteria.BRED_ANIMALS.trigger(serverplayerentity, this.animal, this.mate, this.animal);
            }

            this.crocodile.setHasEgg(true);
            this.animal.resetLoveTicks();
            this.mate.resetLoveTicks();
            this.animal.setBreedingAge(6000);
            this.mate.setBreedingAge(6000);

            if (this.world.getGameRules().getBoolean(GameRules.DO_MOB_LOOT)) {
                final Random random = this.animal.getRandom();
                this.world.spawnEntity(new ExperienceOrbEntity(this.world, this.animal.getX(), this.animal.getY(), this.animal.getZ(), random.nextInt(7) + 1));
            }

        }
    }

    static class LayEggGoal extends MoveToTargetPosGoal {
        private final EntityCrocodile turtle;

        LayEggGoal(EntityCrocodile turtle, double speedIn) {
            super(turtle, speedIn, 16);
            this.turtle = turtle;
        }

        @Override
        public void stop() {
        }

        @Override
        public boolean canStart() {
            return this.turtle.hasEgg() && super.canStart();
        }

        @Override
        public boolean shouldContinue() {
            return super.shouldContinue() && this.turtle.hasEgg();
        }

        @Override
        public double getDesiredDistanceToTarget() {
            return turtle.getWidth() + 0.5D;
        }

        @Override
        public void tick() {
            super.tick();
            turtle.setSitting(false);
            turtle.baskingTimer = -100;
            if (!this.turtle.isTouchingWater() && this.hasReached()) {
                final BlockPos blockpos = this.turtle.getBlockPos();
                final World world = this.turtle.getWorld();
                turtle.emitGameEvent(GameEvent.BLOCK_PLACE);
                world.playSound(null, blockpos, SoundEvents.ENTITY_TURTLE_LAY_EGG, SoundCategory.BLOCKS, 0.3F, 0.9F + world.random.nextFloat() * 0.2F);
                world.setBlockState(this.targetPos.up(), AMBlockRegistry.CROCODILE_EGG.get().getDefaultState().with(BlockReptileEgg.EGGS, this.turtle.random.nextInt(1) + 1), 3);
                this.turtle.setHasEgg(false);
                this.turtle.setDigging(false);
                this.turtle.setLoveTicks(600);
            }

        }

        @Override
        protected boolean isTargetPos(WorldView worldIn, BlockPos pos) {
            return worldIn.isAir(pos.up()) && BlockReptileEgg.isProperHabitat(worldIn, pos);
        }
    }
}
