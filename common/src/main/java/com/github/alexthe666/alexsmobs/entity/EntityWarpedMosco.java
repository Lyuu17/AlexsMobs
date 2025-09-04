package com.github.alexthe666.alexsmobs.entity;

import com.github.alexthe666.alexsmobs.config.AMConfig;
import com.github.alexthe666.alexsmobs.entity.ai.DirectPathNavigator;
import com.github.alexthe666.alexsmobs.entity.ai.EntityAINearestTarget3D;
import com.github.alexthe666.alexsmobs.entity.ai.FlightMoveController;
import com.github.alexthe666.alexsmobs.entity.ai.GroundPathNavigatorWide;
import com.github.alexthe666.alexsmobs.entity.util.Maths;
import com.github.alexthe666.alexsmobs.misc.AMBlockPos;
import com.github.alexthe666.alexsmobs.registry.AMEntityRegistry;
import com.github.alexthe666.alexsmobs.registry.AMSoundRegistry;
import com.github.alexthe666.alexsmobs.registry.AMTagRegistry;
import com.iafenvoy.uranus.animation.Animation;
import com.iafenvoy.uranus.animation.AnimationHandler;
import com.iafenvoy.uranus.animation.IAnimatedEntity;
import net.minecraft.block.BlockState;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.FuzzyTargeting;
import net.minecraft.entity.ai.control.MoveControl;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.event.GameEvent;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public class EntityWarpedMosco extends HostileEntity implements IAnimatedEntity {

    public static final Animation ANIMATION_PUNCH_R = Animation.create(25);
    public static final Animation ANIMATION_PUNCH_L = Animation.create(25);
    public static final Animation ANIMATION_SLAM = Animation.create(35);
    public static final Animation ANIMATION_SUCK = Animation.create(60);
    public static final Animation ANIMATION_SPIT = Animation.create(60);
    private static final TrackedData<Boolean> FLYING = DataTracker.registerData(EntityWarpedMosco.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> HAND_SIDE = DataTracker.registerData(EntityWarpedMosco.class, TrackedDataHandlerRegistry.BOOLEAN);
    public float flyLeftProgress;
    public float prevLeftFlyProgress;
    public float flyRightProgress;
    public float prevFlyRightProgress;
    private int animationTick;
    private Animation currentAnimation;
    private boolean isLandNavigator;
    private int timeFlying;
    private int loopSoundTick = 0;

    public EntityWarpedMosco(EntityType entityType, World world) {
        super(entityType, world);
        this.experiencePoints = 30;
        switchNavigator(false);
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createLivingAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 100D)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 128.0D)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 10.0D)
                .add(EntityAttributes.GENERIC_ARMOR, 10D)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 1D)
                .add(EntityAttributes.GENERIC_ARMOR_TOUGHNESS, 2D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.3D);
    }

    @Override
    public EntityGroup getGroup() {
        return EntityGroup.ARTHROPOD;
    }

    private static Animation getRandomAttack(Random rand) {
        return switch (rand.nextInt(4)) {
            case 0 -> ANIMATION_PUNCH_L;
            case 1 -> ANIMATION_PUNCH_R;
            case 2 -> ANIMATION_SLAM;
            case 3 -> ANIMATION_SUCK;
            default -> ANIMATION_SUCK;
        };
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return AMSoundRegistry.WARPED_MOSCO_IDLE.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return AMSoundRegistry.WARPED_MOSCO_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return AMSoundRegistry.WARPED_MOSCO_HURT.get();
    }

    @Override
    protected void initGoals() {
        super.initGoals();
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(0, new AttackGoal());
        this.goalSelector.add(4, new AIWalkIdle());
        this.goalSelector.add(4, new LookAtEntityGoal(this, PlayerEntity.class, 32F));
        this.goalSelector.add(5, new LookAroundGoal(this));
        this.targetSelector.add(1, new RevengeGoal(this, EntityCrimsonMosquito.class, EntityWarpedMosco.class));
        this.targetSelector.add(2, new EntityAINearestTarget3D<>(this, PlayerEntity.class, true));
        this.targetSelector.add(2, new EntityAINearestTarget3D<>(this, LivingEntity.class, 50, false, true, AMEntityRegistry.buildPredicateFromTag(AMTagRegistry.CRIMSON_MOSQUITO_TARGETS)));
    }

    private void switchNavigator(boolean onLand) {
        if (onLand) {
            this.moveControl = new MoveControl(this);
            this.navigation = new GroundPathNavigatorWide(this, getWorld());
            this.isLandNavigator = true;
        } else {
            this.moveControl = new FlightMoveController(this, 0.7F, false);
            this.navigation = new DirectPathNavigator(this, getWorld());
            this.isLandNavigator = false;
        }
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(FLYING, false);
        this.dataTracker.startTracking(HAND_SIDE, true);
    }

    @Override
    protected void fall(double y, boolean onGroundIn, BlockState state, BlockPos pos) {
    }

    public boolean isFlying() {
        return this.dataTracker.get(FLYING);
    }

    public void setFlying(boolean flying) {
        setDashRight(flying != this.isFlying() ? random.nextBoolean() : this.isDashRight());
        this.dataTracker.set(FLYING, flying);
    }

    public boolean isDashRight() {
        return this.dataTracker.get(HAND_SIDE);
    }

    public void setDashRight(boolean right) {
        this.dataTracker.set(HAND_SIDE, right);
    }

    @Override
    public void tick() {
        super.tick();
        prevFlyRightProgress = flyRightProgress;
        prevLeftFlyProgress = flyLeftProgress;
        final boolean dashRight = isDashRight();
        final boolean flying = isFlying();
        if (flying && dashRight && flyRightProgress < 5F) {
            flyRightProgress++;
        }
        if ((!flying || !dashRight) && flyRightProgress > 0F) {
            flyRightProgress--;
        }
        if (flying && !dashRight && flyLeftProgress < 5F) {
            flyLeftProgress++;
        }
        if ((!flying || dashRight) && flyLeftProgress > 0F) {
            flyLeftProgress--;
        }
        if (!this.getWorld().isClient) {
            if (flying) {
                if (this.isLandNavigator)
                    switchNavigator(false);
            } else {
                if (!this.isLandNavigator)
                    switchNavigator(true);
            }
        }
        if (flying) {
            if (loopSoundTick == 0) {
                this.playSound(AMSoundRegistry.MOSQUITO_LOOP.get(), this.getSoundVolume(), this.getSoundPitch() * 0.3F);
            }
            loopSoundTick++;
            if (loopSoundTick > 100) {
                loopSoundTick = 0;
            }
            timeFlying++;
            this.setNoGravity(true);
            if (this.hasVehicle() || this.hasPassengers()) {
                this.setFlying(false);
            }
        } else {
            timeFlying = 0;
            this.setNoGravity(false);
        }
        //FIXME forge
//        if (this.horizontalCollision && net.minecraftforge.event.ForgeEventFactory.getMobGriefingEvent(this.getWorld(), this)) {
//            boolean flag = false;
//            AABB axisalignedbb = this.getBoundingBox().expand(0.2D);
//            for (BlockPos blockpos : BlockPos.betweenClosed(MathHelper.floor(axisalignedbb.minX), MathHelper.floor(axisalignedbb.minY), MathHelper.floor(axisalignedbb.minZ), MathHelper.floor(axisalignedbb.maxX), MathHelper.floor(axisalignedbb.maxY), MathHelper.floor(axisalignedbb.maxZ))) {
//                BlockState blockstate = this.getWorld().getBlockState(blockpos);
//                if (blockstate.is(AMTagRegistry.WARPED_MOSCO_BREAKABLES)) {
//                    flag = this.getWorld().destroyBlock(blockpos, true, this) || flag;
//                }
//            }
//            if (!flag && this.isOnGround()) {
//                this.jumpFromGround();
//            }
//        }

        var target = this.getTarget();
        if (target != null && this.isAlive()) {
            if (this.getAnimation() == ANIMATION_SUCK && this.getAnimationTick() == 3 && this.distanceTo(target) < 4.7F) {
                target.startRiding(this, true);
            }
            if (this.getAnimation() == ANIMATION_SLAM) {
                if (this.getAnimationTick() == 19) {
                    for (Entity entity : this.getWorld().getNonSpectatingEntities(LivingEntity.class, this.getBoundingBox().expand(5.0D))) {
                        if (!isTeammate(entity) && !(entity instanceof EntityWarpedMosco) && entity != this) {
                            entity.damage(this.getDamageSources().mobAttack(this), 10.0F + random.nextFloat() * 8.0F);
                            launch(entity, true);
                        }
                    }

                }
            }
            if ((this.getAnimation() == ANIMATION_PUNCH_R || this.getAnimation() == ANIMATION_PUNCH_L) && this.getAnimationTick() == 13) {
                if (this.distanceTo(target) < 4.7F) {
                    target.damage(this.getDamageSources().mobAttack(this), (float) this.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE));
                    knockbackRidiculous(target, 0.9F);
                }
            }
        }
        if (this.getAnimation() == ANIMATION_SLAM && this.getAnimationTick() == 19) {
            spawnGroundEffects();
        }

        AnimationHandler.INSTANCE.updateAnimations(this);
    }

    public void spawnGroundEffects() {
        final float radius = 2.3F;
        final double extraY = 0.8F;
        for (int i = 0; i < 4; i++) {
            for (int i1 = 0; i1 < 20 + random.nextInt(12); i1++) {
                final double motionX = getRandom().nextGaussian() * 0.07D;
                final double motionY = getRandom().nextGaussian() * 0.07D;
                final double motionZ = getRandom().nextGaussian() * 0.07D;
                final float angle = (Maths.STARTING_ANGLE * this.bodyYaw) + i1;
                final double extraX = radius * MathHelper.sin(MathHelper.PI + angle);
                final double extraZ = radius * MathHelper.cos(angle);
                var ground = getMoscoGround(new BlockPos(MathHelper.floor(this.getX() + extraX), MathHelper.floor(this.getY() + extraY) - 1, MathHelper.floor(this.getZ() + extraZ)));
                var state = this.getWorld().getBlockState(ground);
                if (state.isSolid()) {
                    if (this.getWorld().isClient) {
                        getWorld().addParticle(new BlockStateParticleEffect(ParticleTypes.BLOCK, state), true, this.getX() + extraX, ground.getY() + extraY, this.getZ() + extraZ, motionX, motionY, motionZ);
                    }
                }
            }
        }
    }

    private void launch(Entity e, boolean huge) {
        if (e.isOnGround()) {
            final double d0 = e.getX() - this.getX();
            final double d1 = e.getZ() - this.getZ();
            final double d2 = Math.max(d0 * d0 + d1 * d1, 0.001D);
            final float f = huge ? 2F : 0.5F;
            e.addVelocity(d0 / d2 * f, huge ? 0.5D : 0.2F, d1 / d2 * f);
        }
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
    public void setAnimationTick(int i) {
        animationTick = i;
    }

    @Override
    public Animation[] getAnimations() {
        return new Animation[]{ANIMATION_PUNCH_L, ANIMATION_PUNCH_R, ANIMATION_SLAM, ANIMATION_SUCK, ANIMATION_SPIT};
    }

    private BlockPos getMoscoGround(BlockPos in) {
        BlockPos position = new BlockPos(in.getX(),
                (int) this.getY(),
                in.getZ());
        while (position.getY() > -62 && !getWorld().getBlockState(position).isSolid() && getWorld().getFluidState(position).isEmpty()) {
            position = position.down();
        }
        return position;
    }

    public Vec3d getBlockGrounding(Vec3d fleePos) {
        float radius = 0.75F * (0.7F * 6) * -3 - this.getRandom().nextInt(24);
        float neg = this.getRandom().nextBoolean() ? 1 : -1;
        float renderYawOffset = this.bodyYaw;
        float angle = (Maths.STARTING_ANGLE * renderYawOffset) + 3.15F + (this.getRandom().nextFloat() * neg);
        double extraX = radius * MathHelper.sin(MathHelper.PI + angle);
        double extraZ = radius * MathHelper.cos(angle);
        var radialPos = AMBlockPos.fromCoords(fleePos.x + extraX, getY(), fleePos.z + extraZ);
        var ground = this.getMoscoGround(radialPos);
        if (ground.getY() == -62) {
            return this.getPos();
        } else {
            ground = this.getBlockPos();
            while (ground.getY() > -62 && !getWorld().getBlockState(ground).isSolid()) {
                ground = ground.down();
            }
        }
        if (!this.isTargetBlocked(Vec3d.ofCenter(ground.up()))) {
            return Vec3d.ofCenter(ground);
        }
        return null;
    }

    public Vec3d getBlockInViewAway(Vec3d fleePos, float radiusAdd) {
        float radius = 0.75F * (0.7F * 6) * -3 - this.getRandom().nextInt(24) - radiusAdd;
        float neg = this.getRandom().nextBoolean() ? 1 : -1;
        float renderYawOffset = this.bodyYaw;
        float angle = (Maths.STARTING_ANGLE * renderYawOffset) + 3.15F + (this.getRandom().nextFloat() * neg);
        double extraX = radius * MathHelper.sin(MathHelper.PI + angle);
        double extraZ = radius * MathHelper.cos(angle);
        BlockPos radialPos = new BlockPos((int) (fleePos.x + extraX), 0, (int) (fleePos.z + extraZ));
        BlockPos ground = getMoscoGround(radialPos);
        int distFromGround = (int) this.getY() - ground.getY();
        int flightHeight = 4 + this.getRandom().nextInt(10);
        BlockPos newPos = ground.up(distFromGround > 8 ? flightHeight : this.getRandom().nextInt(6) + 1);
        if (!this.isTargetBlocked(Vec3d.ofCenter(newPos)) && this.squaredDistanceTo(Vec3d.ofCenter(newPos)) > 1) {
            return Vec3d.ofCenter(newPos);
        }
        return null;
    }

    public void knockbackRidiculous(LivingEntity target, float power) {
        target.takeKnockback(power, this.getX() - target.getX(), this.getZ() - target.getZ());
        float knockbackResist = (float) MathHelper.clamp((1.0D - this.getAttributeValue(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE)), 0, 1);
        target.setVelocity(target.getVelocity().add(0, knockbackResist * power * 0.45F, 0));
    }

    public boolean isTargetBlocked(Vec3d target) {
        var Vector3d = new Vec3d(this.getX(), this.getEyeY(), this.getZ());

        return this.getWorld().raycast(new RaycastContext(Vector3d, target, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, this)).getType() != HitResult.Type.MISS;
    }

    private boolean isOverLiquid() {
        var position = this.getBlockPos();
        while (position.getY() > 2 && getWorld().isAir(position)) {
            position = position.down();
        }
        return !getWorld().getFluidState(position).isEmpty();
    }

    @Override
    public void travel(Vec3d travelVector) {
        if ((this.getAnimation() == ANIMATION_SUCK || this.getAnimation() == ANIMATION_SLAM) && this.getAnimationTick() > 8) {
            if (this.getNavigation().getCurrentPath() != null) {
                this.getNavigation().stop();
            }
            travelVector = Vec3d.ZERO;
            super.travel(travelVector);
            return;
        }
        super.travel(travelVector);
    }

    @Override
    public void updatePassengerPosition(Entity passenger, Entity.PositionUpdater moveFunc) {
        super.updatePassengerPosition(passenger, moveFunc);
        if (hasPassenger(passenger)) {
            int tick = 5;
            if (this.getAnimation() == ANIMATION_SUCK) {
                tick = this.getAnimationTick();
            } else {
                passenger.stopRiding();
            }
            float radius = 2F;
            float angle = (Maths.STARTING_ANGLE * this.bodyYaw);
            double extraX = radius * MathHelper.sin(MathHelper.PI + angle);
            double extraZ = radius * MathHelper.cos(angle);
            double extraY = tick < 10 ? 0 : 0.15F * MathHelper.clamp(tick - 10, 0, 15);
            passenger.setPos(this.getX() + extraX, this.getY() + extraY + 0.1F, this.getZ() + extraZ);
            if ((tick - 10) % 4 == 0) {
                this.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 100, 1));
                passenger.damage(this.getDamageSources().mobAttack(this), (float) this.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE));
            }
        }
    }

    //FIXME
//    @Override
//    public boolean canRiderInteract() {
//        return true;
//    }

//    public boolean shouldRiderSit() {
//        return false;
//    }

    @Override
    public boolean canSpawn(WorldAccess worldIn, SpawnReason spawnReasonIn) {
        return AMEntityRegistry.rollSpawn(AMConfig.warpedMoscoSpawnRolls, this.getRandom(), spawnReasonIn);
    }

    private void spit(LivingEntity target) {
        if (this.getAnimation() != ANIMATION_SPIT) {
            return;
        }
        this.lookAtEntity(target, 100, 100);
        this.bodyYaw = headYaw;
        for (int i = 0; i < 2 + random.nextInt(2); i++) {
            var llamaspitentity = new EntityHemolymph(this.getWorld(), this);
            double d0 = target.getX() - this.getX();
            double d1 = target.getBodyY(0.3333333333333333D) - llamaspitentity.getY();
            double d2 = target.getZ() - this.getZ();
            float f = MathHelper.sqrt((float) (d0 * d0 + d2 * d2)) * 0.2F;
            llamaspitentity.shoot(d0, d1 + (double) f, d2, 1.5F, 5.0F);
            if (!this.isSilent()) {
                this.emitGameEvent(GameEvent.PROJECTILE_SHOOT);
                this.getWorld().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ENTITY_LLAMA_SPIT, this.getSoundCategory(), 1.0F, 1.0F + (this.random.nextFloat() - this.random.nextFloat()) * 0.2F);
            }
            this.getWorld().spawnEntity(llamaspitentity);
        }
    }

    private class AIWalkIdle extends Goal {
        protected final EntityWarpedMosco mosco;
        protected double x;
        protected double y;
        protected double z;
        private boolean flightTarget = false;

        public AIWalkIdle() {
            super();
            this.setControls(EnumSet.of(Control.MOVE));
            this.mosco = EntityWarpedMosco.this;
        }

        @Override
        public boolean canStart() {
            if (this.mosco.hasPassengers() || (mosco.getTarget() != null && mosco.getTarget().isAlive()) || this.mosco.hasVehicle()) {
                return false;
            } else {
                if (this.mosco.getRandom().nextInt(30) != 0 && !mosco.isFlying()) {
                    return false;
                }
                if (this.mosco.isOnGround()) {
                    this.flightTarget = random.nextInt(8) == 0;
                } else {
                    this.flightTarget = random.nextInt(5) > 0 && mosco.timeFlying < 200;
                }
                var lvt_1_1_ = this.getPosition();
                if (lvt_1_1_ == null) {
                    return false;
                } else {
                    this.x = lvt_1_1_.x;
                    this.y = lvt_1_1_.y;
                    this.z = lvt_1_1_.z;
                    return true;
                }
            }
        }

        @Override
        public void tick() {
            if (flightTarget) {
                mosco.getMoveControl().moveTo(x, y, z, 1F);
            } else {
                this.mosco.getNavigation().startMovingTo(this.x, this.y, this.z, 1F);
            }
            if (!flightTarget && isFlying() && mosco.isOnGround()) {
                mosco.setFlying(false);
            }
            if (isFlying() && mosco.isOnGround() && mosco.timeFlying > 10) {
                mosco.setFlying(false);
            }
        }

        @Nullable
        protected Vec3d getPosition() {
            var vector3d = mosco.getPos();

            if (mosco.isOverLiquid()) {
                flightTarget = true;
            }
            if (flightTarget) {
                if (mosco.timeFlying < 50 || mosco.isOverLiquid()) {
                    return mosco.getBlockInViewAway(vector3d, 0);
                } else {
                    return mosco.getBlockGrounding(vector3d);
                }
            } else {

                return FuzzyTargeting.find(this.mosco, 20, 7);
            }
        }

        @Override
        public boolean shouldContinue() {
            if (flightTarget) {
                return mosco.isFlying() && mosco.squaredDistanceTo(x, y, z) > 20F && !mosco.horizontalCollision;
            } else {
                return (!this.mosco.getNavigation().isIdle()) && !this.mosco.hasPassengers();
            }
        }

        @Override
        public void start() {
            if (flightTarget) {
                mosco.setFlying(true);
                mosco.getMoveControl().moveTo(x, y, z, 1F);
            } else {
                this.mosco.getNavigation().startMovingTo(this.x, this.y, this.z, 1F);
            }
        }

        @Override
        public void stop() {
            this.mosco.getNavigation().stop();
            super.stop();
        }
    }

    private class AttackGoal extends Goal {
        private int upTicks = 0;
        private int dashCooldown = 0;
        private boolean ranged = false;
        private BlockPos farTarget = null;

        public AttackGoal() {
        }

        @Override
        public boolean canStart() {
            return EntityWarpedMosco.this.getTarget() != null;
        }

        @Override
        public void tick() {
            if (dashCooldown > 0) {
                dashCooldown--;
            }
            if (EntityWarpedMosco.this.getTarget() != null) {
                LivingEntity target = EntityWarpedMosco.this.getTarget();
                ranged = EntityWarpedMosco.this.shouldRangeAttack(target);
                if (EntityWarpedMosco.this.isFlying() || ranged || EntityWarpedMosco.this.distanceTo(target) > 12 && !EntityWarpedMosco.this.isTargetBlocked(target.getPos().add(0, target.getHeight() * 0.6F, 0))) {
                    float speedRush = 5F;
                    upTicks++;
                    EntityWarpedMosco.this.setFlying(true);
                    if (ranged) {
                        if (farTarget == null || EntityWarpedMosco.this.squaredDistanceTo(Vec3d.ofCenter(farTarget)) < 9) {
                            farTarget = this.getAvoidTarget(target);
                        }
                        if (farTarget != null) {
                            EntityWarpedMosco.this.getMoveControl().moveTo(farTarget.getX(), farTarget.getY() + target.getStandingEyeHeight() * 0.6F, farTarget.getZ(), 3D);
                        }
                        EntityWarpedMosco.this.setAnimation(ANIMATION_SPIT);
                        if(upTicks % 30 == 0){
                            EntityWarpedMosco.this.heal(1);
                        }
                        final int tick = EntityWarpedMosco.this.getAnimationTick();
                        switch (tick) {
                            case 10, 20, 30, 40 -> EntityWarpedMosco.this.spit(target);
                        }
                    } else {
                        if (upTicks > 20 || EntityWarpedMosco.this.distanceTo(target) < 6) {
                            EntityWarpedMosco.this.getMoveControl().moveTo(target.getX(), target.getY() + target.getStandingEyeHeight() * 0.6F, target.getZ(), speedRush);
                        } else {
                            EntityWarpedMosco.this.getMoveControl().moveTo(EntityWarpedMosco.this.getX(), EntityWarpedMosco.this.getY() + 3, EntityWarpedMosco.this.getZ(), 0.5F);
                        }
                    }
                } else {
                    EntityWarpedMosco.this.getNavigation().startMovingTo(EntityWarpedMosco.this.getTarget(), 1.25F);
                }
                if (EntityWarpedMosco.this.isFlying()) {
                    if (EntityWarpedMosco.this.distanceTo(target) < 4.3F) {
                        if (dashCooldown == 0 || target.isOnGround() || target.isInLava() || target.isTouchingWater()) {
                            target.damage(EntityWarpedMosco.this.getDamageSources().mobAttack(EntityWarpedMosco.this), 5F);
                            EntityWarpedMosco.this.knockbackRidiculous(target, 1.0F);
                            dashCooldown = 30;
                        }
                        final float groundHeight = EntityWarpedMosco.this.getMoscoGround(EntityWarpedMosco.this.getBlockPos()).getY();
                        if (Math.abs(EntityWarpedMosco.this.getY() - groundHeight) < 3.0F && !EntityWarpedMosco.this.isOverLiquid()) {
                            EntityWarpedMosco.this.timeFlying += 300;
                            EntityWarpedMosco.this.setFlying(false);
                        }
                    }
                } else {
                    if (EntityWarpedMosco.this.distanceTo(target) < 4F && EntityWarpedMosco.this.getAnimation() == NO_ANIMATION) {
                        Animation animation = getRandomAttack(random);
                        if (animation == ANIMATION_SUCK && target.hasVehicle()) {
                            animation = ANIMATION_SLAM;
                        }
                        EntityWarpedMosco.this.setAnimation(animation);
                    }
                }
            }
        }

        public BlockPos getAvoidTarget(LivingEntity target) {
            final float radius = 10 + EntityWarpedMosco.this.getRandom().nextInt(8);
            //float neg = EntityWarpedMosco.this.getRandom().nextBoolean() ? 1 : -1;
            final float angle = (Maths.STARTING_ANGLE * (target.headYaw+ 90F + EntityWarpedMosco.this.getRandom().nextInt(180)));
            final double extraX = radius * MathHelper.sin(MathHelper.PI + angle);
            final double extraZ = radius * MathHelper.cos(angle);
            var radialPos = AMBlockPos.fromCoords(target.getX() + extraX, target.getY() + 1, target.getZ() + extraZ);
            var ground = radialPos;
            if (EntityWarpedMosco.this.squaredDistanceTo(Vec3d.ofCenter(ground)) > 30) {
                if (!EntityWarpedMosco.this.isTargetBlocked(Vec3d.ofCenter(ground)) && EntityWarpedMosco.this.squaredDistanceTo(Vec3d.ofCenter(ground)) > 6) {
                    return ground;
                }
            }
            return EntityWarpedMosco.this.getBlockPos();
        }

        public void stop() {
            upTicks = 0;
            dashCooldown = 0;
            ranged = false;
        }
    }

    private boolean shouldRangeAttack(LivingEntity target) {
        if(this.getHealth() < Math.floor(this.getMaxHealth() * 0.25F)){
            return true;
        }
        return this.getHealth() < this.getHealth() * 0.5F && this.distanceTo(target) > 10;
    }
}
