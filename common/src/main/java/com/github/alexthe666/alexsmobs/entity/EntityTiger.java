package com.github.alexthe666.alexsmobs.entity;

import com.github.alexthe666.alexsmobs.config.AMConfig;
import com.github.alexthe666.alexsmobs.entity.ai.*;
import com.github.alexthe666.alexsmobs.entity.util.Maths;
import com.github.alexthe666.alexsmobs.misc.AMBlockPos;
import com.github.alexthe666.alexsmobs.registry.*;
import com.iafenvoy.uranus.animation.Animation;
import com.iafenvoy.uranus.animation.AnimationHandler;
import com.iafenvoy.uranus.animation.IAnimatedEntity;
import com.iafenvoy.uranus.object.entity.collision.ICustomCollisions;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.ai.pathing.LandPathNodeMaker;
import net.minecraft.entity.ai.pathing.PathNodeNavigator;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.mob.Angerable;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.TimeHelper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.intprovider.UniformIntProvider;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;
import net.minecraft.world.event.GameEvent;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class EntityTiger extends AnimalEntity implements IAdjustCollision, ICustomCollisions, IAnimatedEntity, Angerable, ITargetsDroppedItems {

    public static final Animation ANIMATION_PAW_R = Animation.create(15);
    public static final Animation ANIMATION_PAW_L = Animation.create(15);
    public static final Animation ANIMATION_TAIL_FLICK = Animation.create(45);
    public static final Animation ANIMATION_LEAP = Animation.create(20);
    private static final TrackedData<Boolean> WHITE = DataTracker.registerData(EntityTiger.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> RUNNING = DataTracker.registerData(EntityTiger.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> SITTING = DataTracker.registerData(EntityTiger.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> SLEEPING = DataTracker.registerData(EntityTiger.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> STEALTH_MODE = DataTracker.registerData(EntityTiger.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> HOLDING = DataTracker.registerData(EntityTiger.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Integer> ANGER_TIME = DataTracker.registerData(EntityTiger.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> LAST_SCARED_MOB_ID = DataTracker.registerData(EntityTiger.class, TrackedDataHandlerRegistry.INTEGER);
    private static final UniformIntProvider ANGRY_TIMER = TimeHelper.betweenSeconds(40, 80);
    private static final Predicate<LivingEntity> NO_BLESSING_EFFECT = (mob) -> !mob.hasStatusEffect(AMEffectRegistry.TIGERS_BLESSING.get());
    public float prevSitProgress;
    public float sitProgress;
    public float prevSleepProgress;
    public float sleepProgress;
    public float prevHoldProgress;
    public float holdProgress;
    public float prevStealthProgress;
    public float stealthProgress;
    private int animationTick;
    private Animation currentAnimation;
    private boolean hasSpedUp = false;
    private UUID lastHurtBy;
    private int sittingTime;
    private int maxSitTime;
    private int holdTime = 0;
    private int prevScaredMobId = -1;
    private boolean dontSitFlag = false;

    public EntityTiger(EntityType<EntityTiger> type, World worldIn) {
        super(type, worldIn);
        this.setPathfindingPenalty(PathNodeType.WATER, 0);
        this.setPathfindingPenalty(PathNodeType.WATER_BORDER, 0);
        this.moveControl = new MovementControllerCustomCollisions(this);
    }

    public static boolean canTigerSpawn(EntityType<? extends AnimalEntity> animal, WorldAccess worldIn, SpawnReason reason, BlockPos pos, Random random) {
        return worldIn.getBaseLightLevel(pos, 0) > 8;
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createLivingAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 50D)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 12.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.25F)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 86);
    }

    @Override
    public boolean canSpawn(WorldAccess worldIn, SpawnReason spawnReasonIn) {
        return AMEntityRegistry.rollSpawn(AMConfig.tigerSpawnRolls, this.getRandom(), spawnReasonIn);
    }

    @Override
    public float getPathfindingFavor(BlockPos pos, WorldView worldIn) {
        return worldIn.getFluidState(pos.down()).isEmpty() && worldIn.getFluidState(pos).isIn(FluidTags.WATER) ? 0.0F : super.getPathfindingFavor(pos, worldIn);
    }

    @Override
    public boolean canSpawn(WorldView worldIn) {
        return !worldIn.containsFluid(this.getBoundingBox());
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound compound) {
        super.writeCustomDataToNbt(compound);
        compound.putBoolean("TigerSitting", this.isSitting());
        compound.putBoolean("TigerSleeping", this.isSleeping());
        compound.putBoolean("White", this.isWhite());
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound compound) {
        super.readCustomDataFromNbt(compound);
        this.setSitting(compound.getBoolean("TigerSitting"));
        this.setSleeping(compound.getBoolean("TigerSleeping"));
        this.setWhite(compound.getBoolean("White"));
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(WHITE, false);
        this.dataTracker.startTracking(RUNNING, false);
        this.dataTracker.startTracking(SITTING, false);
        this.dataTracker.startTracking(STEALTH_MODE, false);
        this.dataTracker.startTracking(HOLDING, false);
        this.dataTracker.startTracking(SLEEPING, false);
        this.dataTracker.startTracking(ANGER_TIME, 0);
        this.dataTracker.startTracking(LAST_SCARED_MOB_ID, -1);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(1, new SwimGoal(this));
        this.goalSelector.add(2, new AnimalAIPanicBaby(this, 1.25D));
        this.goalSelector.add(3, new AIMelee());
        this.goalSelector.add(5, new AnimalMateGoal(this, 1.0D));
        this.goalSelector.add(6, new FollowParentGoal(this, 1.1D));
        this.goalSelector.add(7, new AnimalAIWanderRanged(this, 60, 1.0D, 14, 7));
        this.goalSelector.add(8, new LookAtEntityGoal(this, PlayerEntity.class, 25F));
        this.goalSelector.add(8, new LookAroundGoal(this));
        this.targetSelector.add(1, new CreatureAITargetItems<>(this, false, 10));
        this.targetSelector.add(2, (new AngerGoal(this)));
        this.targetSelector.add(3, new AttackPlayerGoal());
        this.targetSelector.add(4, new ActiveTargetGoal<>(this, LivingEntity.class, 220, false, false, AMEntityRegistry.buildPredicateFromTag(AMTagRegistry.TIGER_TARGETS)) {
            @Override
            public boolean canStart() {
                return !EntityTiger.this.isBaby() && super.canStart();
            }
        });
        this.targetSelector.add(5, new UniversalAngerGoal<>(this, true));
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return isStealth() ? super.getAmbientSound() : getAngerTime() > 0 ? AMSoundRegistry.TIGER_ANGRY.get() : AMSoundRegistry.TIGER_IDLE.get();
    }

    @Override
    public int getMinAmbientSoundDelay() {
        return getAngerTime() > 0 ? 40 : 80;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return AMSoundRegistry.TIGER_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return AMSoundRegistry.TIGER_HURT.get();
    }

    @Override
    protected float getBaseMovementSpeedMultiplier() {
        return 0.99F;
    }

    public boolean shouldMove() {
        return !isSitting() && !isSleeping() && !this.isHolding();
    }

    @Override
    public double getAttackDistanceScalingFactor(@Nullable Entity lookingEntity) {
        if (this.isStealth()) {
            return 0.2D;
        }
        return super.getAttackDistanceScalingFactor(lookingEntity);
    }

    @Override
    public boolean isBreedingItem(ItemStack stack) {
        return stack.isIn(AMTagRegistry.TIGER_BREEDABLES);
    }

    //killEntity
    @Override
    public void updateKilledAdvancementCriterion(Entity entityKilled, int score, DamageSource damageSource) {
        this.heal(5);
        super.updateKilledAdvancementCriterion(entityKilled, score, damageSource);
    }

    @Override
    public void travel(Vec3d vec3d) {
        if (!this.shouldMove()) {
            if (this.getNavigation().getCurrentPath() != null) {
                this.getNavigation().stop();
            }
            vec3d = Vec3d.ZERO;
        }
        super.travel(vec3d);
    }

    @Override
    protected EntityNavigation createNavigation(World worldIn) {
        return new Navigator(this, worldIn);
    }

    public boolean isWhite() {
        return this.dataTracker.get(WHITE);
    }

    public void setWhite(boolean white) {
        this.dataTracker.set(WHITE, white);
    }

    public boolean isRunning() {
        return this.dataTracker.get(RUNNING);
    }

    public void setRunning(boolean running) {
        this.dataTracker.set(RUNNING, running);
    }

    public boolean isSitting() {
        return this.dataTracker.get(SITTING);
    }

    public void setSitting(boolean bar) {
        this.dataTracker.set(SITTING, bar);
    }

    public boolean isStealth() {
        return this.dataTracker.get(STEALTH_MODE);
    }

    public void setStealth(boolean bar) {
        this.dataTracker.set(STEALTH_MODE, bar);
    }

    public boolean isHolding() {
        return this.dataTracker.get(HOLDING);
    }

    public void setHolding(boolean running) {
        this.dataTracker.set(HOLDING, running);
    }

    @Override
    public boolean isSleeping() {
        return this.dataTracker.get(SLEEPING);
    }

    public void setSleeping(boolean sleeping) {
        this.dataTracker.set(SLEEPING, sleeping);
    }

    public int getAngerTime() {
        return this.dataTracker.get(ANGER_TIME);
    }

    public void setAngerTime(int time) {
        this.dataTracker.set(ANGER_TIME, time);
    }

    @Override
    public UUID getAngryAt() {
        return this.lastHurtBy;
    }

    @Override
    public void setAngryAt(@Nullable UUID target) {
        this.lastHurtBy = target;
    }

    @Override
    public void chooseRandomAngerTime() {
        this.setAngerTime(ANGRY_TIMER.get(this.random));
    }

    @Override
    protected void mobTick() {
        if (!this.getWorld().isClient) {
            this.tickAngerLogic((ServerWorld) this.getWorld(), false);
        }
    }

    @Override
    public boolean collidesWithStateAtPos(BlockPos pos, BlockState blockstate) {
        return !(blockstate.getBlock() == Blocks.BAMBOO || blockstate.isIn(BlockTags.LEAVES)) && super.collidesWithStateAtPos(pos, blockstate);
    }

    @Override
    public Vec3d adjustMovementForCollisions(Vec3d vec3, Supplier<Vec3d> supplier) {
        return ICustomCollisions.getAllowedMovementForEntity(this, vec3);
    }

    @Override
    public void tick() {
        super.tick();
        prevSitProgress = sitProgress;
        prevSleepProgress = sleepProgress;
        prevHoldProgress = holdProgress;
        prevStealthProgress = stealthProgress;

        final boolean sitting = isSitting();
        final boolean sleeping = isSleeping();
        final boolean holding = isHolding();
        final boolean stealth = isStealth();

        if (sitting) {
            if (sitProgress < 5F)
                sitProgress++;
        } else {
            if (sitProgress > 0F)
                sitProgress--;
        }

        if (sleeping) {
            if (sleepProgress < 5F)
                sleepProgress++;
        } else {
            if (sleepProgress > 0F)
                sleepProgress--;
        }

        if (holding) {
            if (holdProgress < 5F)
                holdProgress++;
        } else {
            if (holdProgress > 0F)
                holdProgress--;
        }

        if (stealth) {
            if (stealthProgress < 10F)
                stealthProgress += 0.25F;
        } else {
            if (stealthProgress > 0F)
                stealthProgress--;
        }

        if (!this.getWorld().isClient) {
            if (isRunning() && !hasSpedUp) {
                hasSpedUp = true;
                this.setStepHeight(1F);
                this.setSprinting(true);
                this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).setBaseValue(0.4F);
            }
            if (!isRunning() && hasSpedUp) {
                hasSpedUp = false;
                this.setStepHeight(0.6F);
                this.setSprinting(false);
                this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).setBaseValue(0.25F);
            }
            if ((isSitting() || isSleeping()) && (++sittingTime > maxSitTime || this.getTarget() != null || this.isInLove() || dontSitFlag || this.isInsideWaterOrBubbleColumn())) {
                this.setSitting(false);
                this.setSleeping(false);
                sittingTime = 0;
                maxSitTime = 100 + random.nextInt(50);
            }
            if (this.getTarget() == null && !dontSitFlag && this.getVelocity().lengthSquared() < 0.03D && this.getAnimation() == NO_ANIMATION && !this.isSleeping() && !this.isSitting() && !this.isInsideWaterOrBubbleColumn() && random.nextInt(100) == 0) {
                sittingTime = 0;
                if (this.getRandom().nextBoolean()) {
                    maxSitTime = 100 + random.nextInt(550);
                    this.setSitting(true);
                    this.setSleeping(false);
                } else {
                    maxSitTime = 200 + random.nextInt(550);
                    this.setSitting(false);
                    this.setSleeping(true);
                }
            }
            if (this.getVelocity().lengthSquared() < 0.03D && this.getAnimation() == NO_ANIMATION && !this.isSleeping() && !this.isSitting() && random.nextInt(100) == 0) {
                this.setAnimation(ANIMATION_TAIL_FLICK);
            }
        }
        if (this.isHolding()) {
            this.setSprinting(false);
            this.setRunning(false);
            Entity target = this.getTarget();
            if (!this.getWorld().isClient && target != null && target.isAlive()) {
                this.setPitch(0);
                final float radius = 1.0F + target.getWidth() * 0.5F;
                final float angle = (Maths.STARTING_ANGLE * this.bodyYaw);
                final double extraX = radius * MathHelper.sin(MathHelper.PI + angle);
                final double extraZ = radius * MathHelper.cos(angle);
                final double extraY = -0.5F;
                var minus = new Vec3d(this.getX() + extraX - target.getX(), this.getY() + extraY - target.getY(), this.getZ() + extraZ - target.getZ());
                target.setVelocity(minus);
                target.velocityDirty = true;
                if (holdTime % 20 == 0) {
                    target.damage(this.getDamageSources().mobAttack(this), 5 + this.getRandom().nextInt(2));
                }
                if(target.distanceTo(this) > 8.0F){
                    this.setHolding(false);
                    holdTime = 150;
                }
            }
            holdTime++;
            if (holdTime > 100) {
                holdTime = 0;
                this.setHolding(false);
            }
        } else {
            holdTime = 0;
        }
        if (prevScaredMobId != this.dataTracker.get(LAST_SCARED_MOB_ID) && this.getWorld().isClient) {
            var e = getWorld().getEntityById(this.dataTracker.get(LAST_SCARED_MOB_ID));
            if (e != null) {
                final double d2 = this.random.nextGaussian() * 0.1D;
                final double d0 = this.random.nextGaussian() * 0.1D;
                final double d1 = this.random.nextGaussian() * 0.1D;
                this.getWorld().addParticle(AMParticleRegistry.SHOCKED.get(), e.getX(), e.getEyeY() + e.getHeight() * 0.15F + (double) (this.random.nextFloat() * e.getHeight() * 0.15F), e.getZ(), d0, d1, d2);
            }
        }
        if(this.getTarget() != null && this.getTarget().hasStatusEffect(AMEffectRegistry.TIGERS_BLESSING.get())){
            this.setTarget(null);
            this.setAttacker(null);
        }
        prevScaredMobId = this.dataTracker.get(LAST_SCARED_MOB_ID);
        AnimationHandler.INSTANCE.updateAnimations(this);
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        final boolean prev = super.damage(source, amount);
        if (prev) {
            if (source.getAttacker() != null) {
                if (source.getAttacker() instanceof LivingEntity hurter) {
                    if (hurter.hasStatusEffect(AMEffectRegistry.TIGERS_BLESSING.get())) {
                        hurter.removeStatusEffect(AMEffectRegistry.TIGERS_BLESSING.get());
                    }
                }
            }
        }
        return prev;
    }

    @Override
    public boolean handleFallDamage(float distance, float damageMultiplier, DamageSource source) {
        return false;
    }

    @Override
    protected void fall(double y, boolean onGroundIn, BlockState state, BlockPos pos) {
    }

    public BlockPos getLightPosition() {
        BlockPos pos = AMBlockPos.fromVec3(this.getPos());
        if (!getWorld().getBlockState(pos).isOpaque()) {
            return pos.up();
        }
        return pos;
    }

    @Nullable
    @Override
    public PassiveEntity createChild(ServerWorld world, PassiveEntity entity) {
        final boolean whiteOther = entity instanceof EntityTiger && ((EntityTiger) entity).isWhite();
        EntityTiger baby = AMEntityRegistry.TIGER.get().create(world);
        double whiteChance = 0.1D;
        if (this.isWhite() && whiteOther) {
            whiteChance = 0.8D;
        }
        if (this.isWhite() != whiteOther) {
            whiteChance = 0.4D;
        }
        baby.setWhite(random.nextDouble() < whiteChance);
        return baby;
    }

    @Override
    public boolean canPassThrough(BlockPos mutablePos, BlockState blockstate, VoxelShape voxelshape) {
        return blockstate.getBlock() == Blocks.BAMBOO || blockstate.isIn(BlockTags.LEAVES);
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
    public Animation[] getAnimations() {
        return new Animation[]{ANIMATION_PAW_R, ANIMATION_PAW_L, ANIMATION_LEAP, ANIMATION_TAIL_FLICK};
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
    protected void pushAway(Entity entityIn) {
        if (!this.isHolding() || entityIn != this.getTarget()) {
            super.pushAway(entityIn);
        }
    }

    @Override
    public boolean canTargetItem(ItemStack stack) {
        return stack.getItem().isFood() && stack.getItem().getFoodComponent() != null && stack.getItem().getFoodComponent().isMeat() && stack.getItem() != Items.ROTTEN_FLESH;
    }

    public double getMaxDistToItem() {
        return 3.0D;
    }

    @Override
    public void onGetItem(ItemEntity e) {
        this.dontSitFlag = false;
        var stack = e.getStack();
        if (stack.getItem().isFood() && stack.getItem().getFoodComponent() != null && stack.getItem().getFoodComponent().isMeat() && stack.getItem() != Items.ROTTEN_FLESH) {
            this.emitGameEvent(GameEvent.EAT);
            this.playSound(SoundEvents.ENTITY_CAT_EAT, this.getSoundPitch(), this.getSoundVolume());
            this.heal(5);
            Entity thrower = e.getOwner();
            if (thrower != null && random.nextFloat() < getChanceForEffect(stack) && getWorld().getPlayerByUuid(thrower.getUuid()) != null) {
                PlayerEntity player = getWorld().getPlayerByUuid(thrower.getUuid());
                player.addStatusEffect(new StatusEffectInstance(AMEffectRegistry.TIGERS_BLESSING.get(), 12000));
                this.setTarget(null);
                this.setAttacker(null);
            }
        }
    }

    public void onFindTarget(ItemEntity e) {
        this.dontSitFlag = true;
        this.setSitting(false);
        this.setSleeping(false);
    }

    public double getChanceForEffect(ItemStack stack) {
        if (stack.getItem() == Items.PORKCHOP || stack.getItem() == Items.COOKED_PORKCHOP) {
            return 0.4F;
        }
        if (stack.getItem() == Items.CHICKEN || stack.getItem() == Items.COOKED_CHICKEN) {
            return 0.3F;
        }
        return 0.1F;
    }

    @Override
    protected void jump() {
        if (!this.isSleeping() && !this.isSitting()) {
            super.jump();
        }
    }

    static class TigerNodeEvaluator extends LandPathNodeMaker {
        @Override
        protected PathNodeType adjustNodeType(BlockView level, BlockPos pos, PathNodeType typeIn) {
            return typeIn == PathNodeType.LEAVES || level.getBlockState(pos).getBlock() == Blocks.BAMBOO ? PathNodeType.OPEN : super.adjustNodeType(level, pos, typeIn);
        }
    }

    static class Navigator extends GroundPathNavigatorWide {

        public Navigator(MobEntity mob, World world) {
            super(mob, world, 1.2F);
        }

        @Override
        protected PathNodeNavigator createPathNodeNavigator(int i) {
            this.nodeMaker = new TigerNodeEvaluator();
            return new PathNodeNavigator(this.nodeMaker, i);
        }
    }

    private class AIMelee extends Goal {
        private final EntityTiger tiger;
        private int jumpAttemptCooldown = 0;

        public AIMelee() {
            this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
            tiger = EntityTiger.this;
        }

        @Override
        public boolean canStart() {
            return tiger.getTarget() != null && tiger.getTarget().isAlive();
        }

        @Override
        public void tick() {
            if (jumpAttemptCooldown > 0) {
                jumpAttemptCooldown--;
            }
            LivingEntity target = tiger.getTarget();
            if (target != null && target.isAlive()) {
                final double dist = tiger.distanceTo(target);
                if (dist < 10 && tiger.getAttacker() != null && tiger.getAttacker().isAlive()) {
                    tiger.setStealth(false);
                } else {
                    if (dist > 20) {
                        tiger.setRunning(false);
                        tiger.setStealth(true);
                    }
                }
                if (dist <= 20) {
                    tiger.setStealth(false);
                    tiger.setRunning(true);
                    if (tiger.dataTracker.get(LAST_SCARED_MOB_ID) != target.getId()) {
                        tiger.dataTracker.set(LAST_SCARED_MOB_ID, target.getId());
                        target.addStatusEffect(new StatusEffectInstance(AMEffectRegistry.FEAR.get(), 100, 0, true, false));
                    }
                }
                if (dist < 12 && tiger.getAnimation() == NO_ANIMATION && tiger.isOnGround() && jumpAttemptCooldown == 0 && !tiger.isHolding()) {
                    tiger.setAnimation(ANIMATION_LEAP);
                    jumpAttemptCooldown = 70;
                }
                if ((jumpAttemptCooldown > 0 || tiger.isInsideWaterOrBubbleColumn()) && !tiger.isHolding() && tiger.getAnimation() == NO_ANIMATION && dist < 4 + target.getWidth()) {
                    tiger.setAnimation(tiger.getRandom().nextBoolean() ? ANIMATION_PAW_L : ANIMATION_PAW_R);
                }
                if (dist < 4 + target.getWidth() && (tiger.getAnimation() == ANIMATION_PAW_L || tiger.getAnimation() == ANIMATION_PAW_R) && tiger.getAnimationTick() == 8) {
                    target.damage(tiger.getDamageSources().mobAttack(tiger), 7 + tiger.getRandom().nextInt(5));
                }
                if (tiger.getAnimation() == ANIMATION_LEAP) {
                    tiger.getNavigation().stop();
                    Vec3d vec = target.getPos().subtract(tiger.getPos());
                    tiger.setYaw(-((float) MathHelper.atan2(vec.x, vec.z)) * MathHelper.DEGREES_PER_RADIAN);
                    tiger.bodyYaw = tiger.getYaw();
                    if (tiger.getAnimationTick() >= 5 && tiger.getAnimationTick() < 11 && tiger.isOnGround()) {
                        Vec3d vector3d1 = new Vec3d(target.getX() - this.tiger.getX(), 0.0D, target.getZ() - this.tiger.getZ());
                        if (vector3d1.lengthSquared() > 1.0E-7D) {
                            vector3d1 = vector3d1.normalize().multiply(Math.min(dist, 15) * 0.2F);
                        }
                        this.tiger.setVelocity(vector3d1.x, vector3d1.y + 0.3F + 0.1F * MathHelper.clamp(target.getEyeY() - this.tiger.getY(), 0, 2), vector3d1.z);
                    }
                    if (dist < target.getWidth() + 3 && tiger.getAnimationTick() >= 15) {
                        target.damage(tiger.getDamageSources().mobAttack(tiger), 2);
                        tiger.setRunning(false);
                        tiger.setStealth(false);
                        tiger.setHolding(true);
                    }
                } else {
                    if(target != null){
                        tiger.getNavigation().startMovingTo(target, tiger.isStealth() ? 0.75F : 1.0F);
                    }
                }
            }
        }

        @Override
        public void stop() {
            tiger.setStealth(false);
            tiger.setRunning(false);
            tiger.setHolding(false);
        }
    }

    class AttackPlayerGoal extends ActiveTargetGoal<PlayerEntity> {

        public AttackPlayerGoal() {
            super(EntityTiger.this, PlayerEntity.class, 100, false, true, NO_BLESSING_EFFECT);
        }

        @Override
        public boolean canStart() {
            if (EntityTiger.this.isBaby()) {
                return false;
            } else {
                return super.canStart();
            }
        }

        @Override
        protected double getFollowRange() {
            return 4.0D;
        }
    }

    class AngerGoal extends RevengeGoal {
        AngerGoal(EntityTiger beeIn) {
            super(beeIn);
        }

        @Override
        public boolean shouldContinue() {
            return EntityTiger.this.hasAngerTime() && super.shouldContinue();
        }

        @Override
        public void start() {
            super.start();
            if (EntityTiger.this.isBaby()) {
                this.callSameTypeForRevenge();
                this.stop();
            }

        }

        @Override
        protected void setMobEntityTarget(MobEntity mobIn, LivingEntity targetIn) {
            if (!mobIn.isBaby()) {
                super.setMobEntityTarget(mobIn, targetIn);
            }
        }
    }
}
