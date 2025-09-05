package com.github.alexthe666.alexsmobs.entity;

import com.github.alexthe666.alexsmobs.AlexsMobs;
import com.github.alexthe666.alexsmobs.config.AMConfig;
import com.github.alexthe666.alexsmobs.entity.ai.DirectPathNavigator;
import com.github.alexthe666.alexsmobs.entity.ai.EntityAINearestTarget3D;
import com.github.alexthe666.alexsmobs.entity.ai.FlightMoveController;
import com.github.alexthe666.alexsmobs.entity.ai.GroundPathNavigatorWide;
import com.github.alexthe666.alexsmobs.entity.util.Maths;
import com.github.alexthe666.alexsmobs.misc.AMBlockPos;
import com.github.alexthe666.alexsmobs.packet.MosquitoDismountPacket;
import com.github.alexthe666.alexsmobs.packet.MosquitoMountPlayerPacket;
import com.github.alexthe666.alexsmobs.registry.AMEffectRegistry;
import com.github.alexthe666.alexsmobs.registry.AMEntityRegistry;
import com.github.alexthe666.alexsmobs.registry.AMParticleRegistry;
import com.github.alexthe666.alexsmobs.registry.AMSoundRegistry;
import net.minecraft.block.BlockState;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.FuzzyTargeting;
import net.minecraft.entity.ai.control.MoveControl;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.*;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.*;
import net.minecraft.world.event.GameEvent;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.function.Predicate;

public class EntityEnderiophage extends AnimalEntity implements Monster, Flutterer {

    private static final TrackedData<Float> PHAGE_PITCH = DataTracker.registerData(EntityEnderiophage.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Boolean> FLYING = DataTracker.registerData(EntityEnderiophage.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> MISSING_EYE = DataTracker.registerData(EntityEnderiophage.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Float> PHAGE_SCALE = DataTracker.registerData(EntityEnderiophage.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Integer> VARIANT = DataTracker.registerData(EntityEnderiophage.class, TrackedDataHandlerRegistry.INTEGER);
    private static final Predicate<LivingEntity> ENDERGRADE_OR_INFECTED = (entity) -> entity instanceof EntityEndergrade || entity.hasStatusEffect(AMEffectRegistry.ENDER_FLU.get());
    public float prevPhagePitch;
    public float tentacleAngle;
    public float lastTentacleAngle;
    public float phageRotation;
    public float prevFlyProgress;
    public float flyProgress;
    public int passengerIndex = 0;
    public float prevEnderiophageScale = 1F;
    private float rotationVelocity;
    private int slowDownTicks = 0;
    private float randomMotionSpeed;
    private boolean isLandNavigator;
    private int timeFlying = 0;
    private int fleeAfterStealTime = 0;
    private int attachTime = 0;
    private int dismountCooldown = 0;
    private int squishCooldown = 0;
    private PathAwareEntity angryEnderman = null;

    public EntityEnderiophage(EntityType<? extends EntityEnderiophage> type, World world) {
        super(type, world);
        this.rotationVelocity = 1.0F / (this.random.nextFloat() + 1.0F) * 0.2F;
        switchNavigator(false);
        this.experiencePoints = 5;
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createLivingAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 20.0D)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 16.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.15F)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 2F);
    }

    public static boolean canEnderiophageSpawn(EntityType<? extends AnimalEntity> animal, WorldAccess worldIn, SpawnReason reason, BlockPos pos, Random random) {
        return true;
    }

    @Override
    public boolean canSpawn(WorldAccess worldIn, SpawnReason spawnReasonIn) {
        return AMEntityRegistry.rollSpawn(AMConfig.enderiophageSpawnRolls, this.getRandom(), spawnReasonIn);
    }

    private void doInitialPosing(WorldAccess world) {
        BlockPos down = this.getPhageGround(this.getBlockPos());
        this.setPos(down.getX() + 0.5F, down.getY() + 1, down.getZ() + 0.5F);
    }

    @Nullable
    @Override
    public EntityData initialize(ServerWorldAccess worldIn, LocalDifficulty difficultyIn, SpawnReason reason, @Nullable EntityData spawnDataIn, @Nullable NbtCompound dataTag) {
        if (reason == SpawnReason.NATURAL) {
            doInitialPosing(worldIn);
        }
        setSkinForDimension();
        return super.initialize(worldIn, difficultyIn, reason, spawnDataIn, dataTag);
    }

    @Override
    public int getLimitPerChunk() {
        return 2;
    }

    public float getPhageScale() {
        return this.dataTracker.get(PHAGE_SCALE);
    }

    public void setPhageScale(float scale) {
        this.dataTracker.set(PHAGE_SCALE, scale);
    }

    public int getVariant() {
        return this.dataTracker.get(VARIANT);
    }

    public void setVariant(int variant) {
        this.dataTracker.set(VARIANT, variant);
    }

    @Override
    protected void initGoals() {
        super.initGoals();
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new FlyTowardsTarget(this));
        this.goalSelector.add(2, new AIWalkIdle());
        this.targetSelector.add(1, new EntityAINearestTarget3D<>(this, EndermanEntity.class, 15, true, true, null) {
            @Override
            public boolean canStart() {
                return EntityEnderiophage.this.isMissingEye() && super.canStart();
            }

            @Override
            public boolean shouldContinue() {
                return EntityEnderiophage.this.isMissingEye() && super.shouldContinue();
            }
        });
        this.targetSelector.add(1, new EntityAINearestTarget3D<>(this, LivingEntity.class, 15, true, true, ENDERGRADE_OR_INFECTED) {
            @Override
            public boolean canStart() {
                return !EntityEnderiophage.this.isMissingEye() && EntityEnderiophage.this.fleeAfterStealTime == 0 && super.canStart();
            }

            @Override
            public boolean shouldContinue() {
                return !EntityEnderiophage.this.isMissingEye() && super.shouldContinue();
            }
        });
        this.targetSelector.add(3, new RevengeGoal(this, EndermanEntity.class));

    }

    private void switchNavigator(boolean onLand) {
        if (onLand) {
            this.moveControl = new MoveControl(this);
            this.navigation = new GroundPathNavigatorWide(this, getWorld());
            this.isLandNavigator = true;
        } else {
            this.moveControl = new FlightMoveController(this, 1F, false, true);
            this.navigation = new DirectPathNavigator(this, getWorld());
            this.isLandNavigator = false;
        }
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(VARIANT, 0);
        this.dataTracker.startTracking(PHAGE_PITCH, 0F);
        this.dataTracker.startTracking(PHAGE_SCALE, 1F);
        this.dataTracker.startTracking(FLYING, false);
        this.dataTracker.startTracking(MISSING_EYE, false);
    }

    @Override
    protected void fall(double y, boolean onGroundIn, BlockState state, BlockPos pos) {
    }

    public boolean isInOverworld() {
        return this.getWorld().getRegistryKey() == World.OVERWORLD && !this.isAiDisabled();
    }

    public boolean isInNether() {
        return this.getWorld().getRegistryKey() == World.NETHER && !this.isAiDisabled();
    }

    public void setStandardFleeTime() {
        this.fleeAfterStealTime = 20;
    }

    @Override
    public void tickRiding() {
        Entity entity = this.getVehicle();
        if (this.hasVehicle() && !entity.isAlive()) {
            this.stopRiding();
        } else {
            this.setVelocity(0, 0, 0);
            this.tick();
            if (this.hasVehicle()) {
                attachTime++;
                Entity mount = this.getVehicle();
                if (mount instanceof LivingEntity target) {
                    passengerIndex = mount.getPassengerList().indexOf(this);
                    this.bodyYaw = target.bodyYaw;
                    this.setYaw(mount.getYaw());
                    this.headYaw= target.headYaw;
                    this.prevYaw = target.headYaw;
                    float radius = mount.getWidth();
                    float angle = (Maths.STARTING_ANGLE * (target.bodyYaw + passengerIndex * 90F));
                    double extraX = radius * MathHelper.sin(MathHelper.PI + angle);
                    double extraZ = radius * MathHelper.cos(angle);
                    this.setPos(mount.getX() + extraX, Math.max(mount.getY() + mount.getStandingEyeHeight() * 0.25F, mount.getY()), mount.getZ() + extraZ);
                    if (!mount.isAlive() || mount instanceof PlayerEntity && ((PlayerEntity) mount).isCreative()) {
                        this.dismountVehicle();
                    }
                    this.setPhagePitch(0F);
                    if (!this.getWorld().isClient && attachTime > 15) {
                        float dmg = 1F;
                        if (target.getHealth() > target.getMaxHealth() * 0.2F) {
                            dmg = 6F;
                        }
                        if ((target.getHealth() < 1.5D || mount.damage(this.getDamageSources().mobAttack(this), dmg)) && mount instanceof LivingEntity) {
                            dismountCooldown = 100;
                            if (mount instanceof EndermanEntity) {
                                this.setMissingEye(false);
                                this.emitGameEvent(GameEvent.EAT);
                                this.playSound(SoundEvents.ENTITY_ENDER_EYE_DEATH, this.getSoundVolume(), this.getSoundPitch());
                                this.heal(5);
                                ((EndermanEntity) mount).addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 400));
                                this.fleeAfterStealTime = 400;
                                this.setFlying(true);
                                this.angryEnderman = (PathAwareEntity) mount;
                            } else {
                                if (random.nextInt(3) == 0) {
                                    if (!this.isMissingEye()) {
                                        if (target.getStatusEffect(AMEffectRegistry.ENDER_FLU.get()) == null) {
                                            target.addStatusEffect(new StatusEffectInstance(AMEffectRegistry.ENDER_FLU.get(), 12000));
                                        } else {
                                            StatusEffectInstance inst = target.getStatusEffect(AMEffectRegistry.ENDER_FLU.get());
                                            int duration = 12000;
                                            int level = 0;
                                            if (inst != null) {
                                                duration = inst.getDuration();
                                                level = inst.getAmplifier();
                                            }
                                            target.removeStatusEffect(AMEffectRegistry.ENDER_FLU.get());
                                            target.addStatusEffect(new StatusEffectInstance(AMEffectRegistry.ENDER_FLU.get(), duration, Math.min(level + 1, 4)));
                                        }
                                        this.heal(5);
                                        this.emitGameEvent(GameEvent.ENTITY_ROAR);
                                        this.playSound(SoundEvents.ENTITY_ITEM_BREAK, this.getSoundVolume(), this.getSoundPitch());
                                        this.setMissingEye(true);
                                    }
                                    if (!this.getWorld().isClient) {
                                        this.setTarget(null);
                                        this.onAttacking(null);
                                        this.setAttacker(null);
                                        this.goalSelector.getRunningGoals().forEach(Goal::stop);
                                        this.targetSelector.getRunningGoals().forEach(Goal::stop);
                                    }
                                }
                            }
                        }
                        if (target.getHealth() <= 0 || this.fleeAfterStealTime > 0 || this.isMissingEye() && !(mount instanceof EndermanEntity) || !this.isMissingEye() && mount instanceof EndermanEntity) {
                            this.dismountVehicle();
                            this.setTarget(null);
                            dismountCooldown = 100;
                            AlexsMobs.sendMSGToAll(new MosquitoDismountPacket(this.getId(), mount.getId()));
                            this.setFlying(true);
                        }
                    }
                }

            }
        }

    }

    //FIXME forge
//    @Override
//    public boolean canRiderInteract() {
//        return true;
//    }

    public void onSpawnFromEffect() {
        prevEnderiophageScale = 0.2F;
        this.setPhageScale(0.2F);
    }

    public void setSkinForDimension(){
        if(isInNether()){
            this.setVariant(2);
        }else if(isInOverworld()){
            this.setVariant(1);
        }else{
            this.setVariant(0);
        }
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return AMSoundRegistry.ENDERIOPHAGE_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return AMSoundRegistry.ENDERIOPHAGE_HURT.get();
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(AMSoundRegistry.ENDERIOPHAGE_WALK.get(), 0.4F, 1.0F);
    }

    @Override
    protected float calculateNextStepSoundDistance() {
        return this.distanceTraveled + 0.3F;
    }

    @Override
    public void tick() {
        super.tick();
        prevEnderiophageScale = this.getPhageScale();
        float extraMotionSlow = 1.0F;
        float extraMotionSlowY = 1.0F;
        if (slowDownTicks > 0) {
            slowDownTicks--;
            extraMotionSlow = 0.33F;
            extraMotionSlowY = 0.1F;
        }
        if (dismountCooldown > 0) {
            dismountCooldown--;
        }
        if (squishCooldown > 0) {
            squishCooldown--;
        }
        if (!this.getWorld().isClient) {
            if (!this.hasVehicle() && attachTime != 0) {
                attachTime = 0;
            }
            if (fleeAfterStealTime > 0) {
                if (angryEnderman != null) {
                    Vec3d vec = this.getBlockInViewAway(angryEnderman.getPos(), 10);
                    if (fleeAfterStealTime < 5) {
                        if (angryEnderman instanceof Angerable) {
                            ((Angerable) angryEnderman).stopAnger();
                        }
                        try {
                            angryEnderman.goalSelector.getRunningGoals().forEach(Goal::stop);
                            angryEnderman.targetSelector.getRunningGoals().forEach(Goal::stop);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        angryEnderman = null;
                    }
                    if (vec != null) {
                        this.setFlying(true);
                        this.getMoveControl().moveTo(vec.x, vec.y, vec.z, 1.3F);
                    }
                }
                fleeAfterStealTime--;
            }
        }
        this.bodyYaw = this.getYaw();
        this.headYaw= this.getYaw();
        this.setPhagePitch(-90F);
        if (this.isAlive() && this.isInAir() && randomMotionSpeed > 0.75F && this.getVelocity().lengthSquared() > 0.02D) {
            if (this.getWorld().isClient) {
                float pitch = -this.getPhagePitch() / 90F;
                float radius = this.getWidth() * 0.2F * -pitch;
                float angle = (Maths.STARTING_ANGLE * this.getYaw());
                double extraX = radius * MathHelper.sin(MathHelper.PI + angle);
                double extraY = 0.2F - (1 - pitch) * 0.15F;
                double extraZ = radius * MathHelper.cos(angle);
                double motX = extraX * 8 + random.nextGaussian() * 0.05F;
                double motY = -0.1F;
                double motZ = extraZ + random.nextGaussian() * 0.05F;
                this.getWorld().addParticle(AMParticleRegistry.DNA.get(), this.getX() + extraX, this.getY() + extraY, this.getZ() + extraZ, motX, motY, motZ);
            }
        }
        prevPhagePitch = this.getPhagePitch();
        prevFlyProgress = flyProgress;

        if (isInAir()) {
            if (flyProgress < 5F) {
                flyProgress++;
            }
        } else {
            if (flyProgress > 0F) {
                flyProgress--;
            }
        }

        this.lastTentacleAngle = this.tentacleAngle;
        this.phageRotation += this.rotationVelocity;
        if ((double) this.phageRotation > (Math.PI * 2D)) {
            if (this.getWorld().isClient) {
                this.phageRotation = MathHelper.HALF_PI;
            } else {
                this.phageRotation = (float) ((double) this.phageRotation - (Math.PI * 2D));
                if (this.random.nextInt(10) == 0) {
                    this.rotationVelocity = 1.0F / (this.random.nextFloat() + 1.0F) * 0.2F;
                }
                this.getWorld().sendEntityStatus(this, (byte) 19);
            }
        }
        if (this.phageRotation < MathHelper.PI) {
            float f = this.phageRotation / MathHelper.PI;
            this.tentacleAngle = MathHelper.sin(f * f * MathHelper.PI) * 4.275F;
            if ((double) f > 0.75D) {
                if (squishCooldown == 0 && this.isInAir()) {
                    squishCooldown = 20;
                    this.playSound(AMSoundRegistry.ENDERIOPHAGE_SQUISH.get(), 3F, this.getSoundPitch());
                }
                this.randomMotionSpeed = 1.0F;
            } else {
                randomMotionSpeed = 0.01F;
            }
        }
        if (!this.getWorld().isClient) {
            if (isInAir() && this.isLandNavigator) {
                switchNavigator(false);
            }
            if (!isInAir() && !this.isLandNavigator) {
                switchNavigator(true);
            }
            if (this.isInAir()) {
                this.setVelocity(this.getVelocity().x * this.randomMotionSpeed * extraMotionSlow, this.getVelocity().y * this.randomMotionSpeed * extraMotionSlowY, this.getVelocity().z * this.randomMotionSpeed * extraMotionSlow);
                timeFlying++;
                if (this.isOnGround() && timeFlying > 100) {
                    this.setFlying(false);
                }
            } else {
                timeFlying = 0;
            }
            if (this.isMissingEye() && this.getTarget() != null) {
                if (!(this.getTarget() instanceof EndermanEntity)) {
                    this.setTarget(null);
                }
            }
        }
        if (!this.isOnGround() && this.getVelocity().y < 0.0D) {
            this.setVelocity(this.getVelocity().multiply(1.0D, 0.6D, 1.0D));
        }
        if (this.isInAir()) {
            float phageDist = -(float) ((Math.abs(this.getVelocity().x) + Math.abs(this.getVelocity().z)) * 6F);
            this.incrementPhagePitch(phageDist * 1);
            this.setPhagePitch(MathHelper.clamp(this.getPhagePitch(), -90, 10));
            float plateau = 2;
            if (this.getPhagePitch() > plateau) {
                this.decrementPhagePitch(phageDist * Math.abs(this.getPhagePitch()) / 90);
            }
            if (this.getPhagePitch() < -plateau) {
                this.incrementPhagePitch(phageDist * Math.abs(this.getPhagePitch()) / 90);
            }
            if (this.getPhagePitch() > 2F) {
                this.decrementPhagePitch(1);
            } else if (this.getPhagePitch() < -2) {
                this.incrementPhagePitch(1);
            }
            if (this.horizontalCollision) {
                this.setVelocity(this.getVelocity().add(0, 0.2F, 0));
            }
        } else {
            if (this.getPhagePitch() > 0F) {
                float decrease = Math.min(2, this.getPhagePitch());
                this.decrementPhagePitch(decrease);
            }
            if (this.getPhagePitch() < 0F) {
                float decrease = Math.min(2, -this.getPhagePitch());
                this.incrementPhagePitch(decrease);
            }
        }
        if (this.getPhageScale() < 1F) {
            this.setPhageScale(this.getPhageScale() + 0.05F);
        }
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound compound) {
        super.writeCustomDataToNbt(compound);
        compound.putBoolean("Flying", this.isInAir());
        compound.putBoolean("MissingEye", this.isMissingEye());
        compound.putInt("Variant", this.getVariant());
        compound.putInt("SlowDownTicks", slowDownTicks);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound compound) {
        super.readCustomDataFromNbt(compound);
        this.setFlying(compound.getBoolean("Flying"));
        this.setMissingEye(compound.getBoolean("MissingEye"));
        this.setVariant(compound.getInt("Variant"));
        this.slowDownTicks = compound.getInt("SlowDownTicks");
    }

    public boolean isMissingEye() {
        return this.dataTracker.get(MISSING_EYE);
    }

    public void setMissingEye(boolean missingEye) {
        this.dataTracker.set(MISSING_EYE, missingEye);
    }

    @Override
    public boolean isInAir() {
        return this.dataTracker.get(FLYING);
    }

    public void setFlying(boolean flying) {
        this.dataTracker.set(FLYING, flying);
    }

    public float getPhagePitch() {
        return dataTracker.get(PHAGE_PITCH);
    }

    public void setPhagePitch(float pitch) {
        dataTracker.set(PHAGE_PITCH, pitch);
    }

    public void incrementPhagePitch(float pitch) {
        dataTracker.set(PHAGE_PITCH, getPhagePitch() + pitch);
    }

    public void decrementPhagePitch(float pitch) {
        dataTracker.set(PHAGE_PITCH, getPhagePitch() - pitch);
    }

    @Override
    protected float getActiveEyeHeight(EntityPose poseIn, EntityDimensions sizeIn) {
        return 1.8F;
    }

    @Nullable
    @Override
    public PassiveEntity createChild(ServerWorld world, PassiveEntity entity) {
        return null;
    }

    private boolean isOverWaterOrVoid() {
        BlockPos position = this.getBlockPos();
        while (position.getY() > -63 && !getWorld().getBlockState(position).isSolid()) {
            position = position.down();
        }
        return !getWorld().getFluidState(position).isEmpty() || position.getY() < -63;
    }

    public Vec3d getBlockInViewAway(Vec3d fleePos, float radiusAdd) {
        float radius = 0.75F * (0.7F * 6) * -3 - this.getRandom().nextInt(24) - radiusAdd;
        float neg = this.getRandom().nextBoolean() ? 1 : -1;
        float renderYawOffset = this.bodyYaw;
        float angle = (Maths.STARTING_ANGLE * renderYawOffset) + 3.15F + (this.getRandom().nextFloat() * neg);
        double extraX = radius * MathHelper.sin(MathHelper.PI + angle);
        double extraZ = radius * MathHelper.cos(angle);
        var radialPos = new BlockPos((int) (fleePos.x + extraX), 0, (int) (fleePos.z + extraZ));
        var ground = getPhageGround(radialPos);
        int distFromGround = (int) this.getY() - ground.getY();
        int flightHeight = 6 + this.getRandom().nextInt(10);
        var newPos = ground.up(distFromGround > 8 || fleeAfterStealTime > 0 ? flightHeight : this.getRandom().nextInt(6) + 5);
        if (!this.isTargetBlocked(Vec3d.ofCenter(newPos)) && this.squaredDistanceTo(Vec3d.ofCenter(newPos)) > 1) {
            return Vec3d.ofCenter(newPos);
        }
        return null;
    }

    private BlockPos getPhageGround(BlockPos in) {
        BlockPos position = new BlockPos(in.getX(), (int) this.getY(), in.getZ());
        while (position.getY() > -63 && !getWorld().getBlockState(position).isSolid()) {
            position = position.down();
        }
        if (position.getY() < -62) {
            return position.up(120 + random.nextInt(5));
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
        BlockPos radialPos = AMBlockPos.fromCoords(fleePos.x + extraX, getY(), fleePos.z + extraZ);
        BlockPos ground = this.getPhageGround(radialPos);
        if (ground.getY() <= -63) {
            return Vec3d.ofCenter(ground, 110 + random.nextInt(20));
        } else {
            ground = this.getBlockPos();
            while (ground.getY() > -63 && !getWorld().getBlockState(ground).isSolid()) {
                ground = ground.down();
            }
        }
        if (!this.isTargetBlocked(Vec3d.ofCenter(ground.up()))) {
            return Vec3d.ofCenter(ground);
        }
        return null;
    }

    public boolean isTargetBlocked(Vec3d target) {
        var Vector3d = new Vec3d(this.getX(), this.getEyeY(), this.getZ());
        return this.getWorld().raycast(new RaycastContext(Vector3d, target, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, this)).getType() != HitResult.Type.MISS;
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        if (this.isInvulnerableTo(source)) {
            return false;
        } else {
            var entity = source.getSource();
            if (entity instanceof EndermanEntity) {
                amount = (amount + 1.0F) * 0.35F;
                angryEnderman = (EndermanEntity) entity;
            }
            return super.damage(source, amount);
        }
    }

    private class AIWalkIdle extends Goal {
        protected final EntityEnderiophage phage;
        protected double x;
        protected double y;
        protected double z;
        private boolean flightTarget = false;

        public AIWalkIdle() {
            super();
            this.setControls(EnumSet.of(Control.MOVE));
            this.phage = EntityEnderiophage.this;
        }

        @Override
        public boolean canStart() {
            if (this.phage.hasPassengers() || (phage.getTarget() != null && phage.getTarget().isAlive()) || this.phage.hasVehicle()) {
                return false;
            } else {
                if (this.phage.getRandom().nextInt(30) != 0 && !phage.isInAir() && phage.fleeAfterStealTime == 0) {
                    return false;
                }
                if (this.phage.isOnGround()) {
                    this.flightTarget = random.nextInt(12) == 0;
                } else {
                    this.flightTarget = random.nextInt(5) > 0 && phage.timeFlying < 100;
                }
                if (phage.fleeAfterStealTime > 0) {
                    this.flightTarget = true;
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
                phage.getMoveControl().moveTo(x, y, z, fleeAfterStealTime == 0 ? 1.3F : 1F);
            } else {
                this.phage.getNavigation().startMovingTo(this.x, this.y, this.z, fleeAfterStealTime == 0 ? 1.3F : 1F);
            }
            if (!flightTarget && isInAir() && phage.isOnGround()) {
                phage.setFlying(false);
            }
            if (isInAir() && phage.isOnGround() && phage.timeFlying > 100 && phage.fleeAfterStealTime == 0) {
                phage.setFlying(false);
            }
        }

        @Nullable
        protected Vec3d getPosition() {
            var vector3d = phage.getPos();
            if (phage.isOverWaterOrVoid()) {
                flightTarget = true;
            }
            if (flightTarget) {
                if (phage.timeFlying < 50 || fleeAfterStealTime > 0 || phage.isOverWaterOrVoid()) {
                    return phage.getBlockInViewAway(vector3d, 0);
                } else {
                    return phage.getBlockGrounding(vector3d);
                }
            } else {
                return FuzzyTargeting.find(this.phage, 10, 7);
            }
        }

        @Override
        public boolean shouldContinue() {
            if (flightTarget) {
                return phage.isInAir() && phage.squaredDistanceTo(x, y, z) > 2F;
            } else {
                return (!this.phage.getNavigation().isIdle()) && !this.phage.hasPassengers();
            }
        }

        @Override
        public void start() {
            if (flightTarget) {
                phage.setFlying(true);
                phage.getMoveControl().moveTo(x, y, z, fleeAfterStealTime == 0 ? 1.3F : 1F);
            } else {
                this.phage.getNavigation().startMovingTo(this.x, this.y, this.z, 1F);
            }
        }

        @Override
        public void stop() {
            this.phage.getNavigation().stop();
            super.stop();
        }
    }

    public static class FlyTowardsTarget extends Goal {
        private final EntityEnderiophage parentEntity;

        public FlyTowardsTarget(EntityEnderiophage phage) {
            this.parentEntity = phage;
            this.setControls(EnumSet.of(Goal.Control.MOVE));
        }

        @Override
        public boolean canStart() {
            return !parentEntity.hasVehicle() && parentEntity.getTarget() != null && !isBittenByPhage(parentEntity.getTarget()) && parentEntity.fleeAfterStealTime == 0;
        }

        @Override
        public boolean shouldContinue() {
            return parentEntity.getTarget() != null && !isBittenByPhage(parentEntity.getTarget()) && !parentEntity.horizontalCollision && !parentEntity.hasVehicle() && parentEntity.isInAir() && parentEntity.getMoveControl().isMoving() && parentEntity.fleeAfterStealTime == 0 && (parentEntity.getTarget() instanceof EndermanEntity || !parentEntity.isMissingEye());
        }

        public boolean isBittenByPhage(Entity entity) {
            int phageCount = 0;
            for (Entity e : entity.getPassengerList()) {
                if (e instanceof EntityEnderiophage) {
                    phageCount++;
                }
            }
            return phageCount > 3;
        }

        @Override
        public void stop() {
        }

        @Override
        public void tick() {
            if (parentEntity.getTarget() != null) {
                float width =  parentEntity.getTarget().getWidth() + parentEntity.getWidth() + 2;
                boolean isWithinReach = parentEntity.squaredDistanceTo(parentEntity.getTarget()) < width * width;
                if (parentEntity.isInAir() || isWithinReach) {
                    this.parentEntity.getMoveControl().moveTo(parentEntity.getTarget().getX(), parentEntity.getTarget().getY(), parentEntity.getTarget().getZ(), isWithinReach ? 1.6D : 1.0D);
                } else {
                    this.parentEntity.getNavigation().startMovingTo(parentEntity.getTarget().getX(), parentEntity.getTarget().getY(), parentEntity.getTarget().getZ(), 1.2D);
                }
                if (parentEntity.getTarget().getY() > this.parentEntity.getY() + 1.2F) {
                    parentEntity.setFlying(true);
                }
                if (parentEntity.dismountCooldown == 0 && parentEntity.getBoundingBox().expand(0.3, 0.3, 0.3).intersects(parentEntity.getTarget().getBoundingBox()) && !isBittenByPhage(parentEntity.getTarget())) {
                    parentEntity.startRiding(parentEntity.getTarget(), true);
                    if (!parentEntity.getWorld().isClient) {
                        AlexsMobs.sendMSGToAll(new MosquitoMountPlayerPacket(parentEntity.getId(), parentEntity.getTarget().getId()));
                    }
                }
            }
        }
    }

}
