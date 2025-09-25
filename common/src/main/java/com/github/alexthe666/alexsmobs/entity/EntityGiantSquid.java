package com.github.alexthe666.alexsmobs.entity;


import com.github.alexthe666.alexsmobs.AlexsMobs;
import com.github.alexthe666.alexsmobs.config.AMConfig;
import com.github.alexthe666.alexsmobs.entity.ai.AquaticMoveController;
import com.github.alexthe666.alexsmobs.entity.ai.EntityAINearestTarget3D;
import com.github.alexthe666.alexsmobs.registry.AMEntityRegistry;
import com.github.alexthe666.alexsmobs.registry.AMItemRegistry;
import com.github.alexthe666.alexsmobs.registry.AMSoundRegistry;
import com.github.alexthe666.alexsmobs.registry.AMTagRegistry;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.control.YawAdjustingLookControl;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.MoveIntoWaterGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.entity.ai.pathing.SwimNavigation;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.GuardianEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.WaterCreatureEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.*;
import net.minecraft.world.event.GameEvent;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;
import java.util.function.Supplier;

public class EntityGiantSquid extends WaterCreatureEntity implements IAdjustCollision, IMultipartEntity {

    private static final TrackedData<Float> SQUID_PITCH = DataTracker.registerData(EntityGiantSquid.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> DEPRESSURIZATION = DataTracker.registerData(EntityGiantSquid.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Boolean> OVERRIDE_BODYROT = DataTracker.registerData(EntityGiantSquid.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> GRABBING = DataTracker.registerData(EntityGiantSquid.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> CAPTURED = DataTracker.registerData(EntityGiantSquid.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> BLUE = DataTracker.registerData(EntityGiantSquid.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Integer> GRAB_ENTITY = DataTracker.registerData(EntityGiantSquid.class, TrackedDataHandlerRegistry.INTEGER);
    public final EntityGiantSquidPart mantlePart1;
    public final EntityGiantSquidPart mantlePart2;
    public final EntityGiantSquidPart mantlePart3;
    public final EntityGiantSquidPart tentaclesPart1;
    public final EntityGiantSquidPart tentaclesPart2;
    public final EntityGiantSquidPart tentaclesPart3;
    public final EntityGiantSquidPart tentaclesPart4;
    public final EntityGiantSquidPart tentaclesPart5;
    public final EntityGiantSquidPart tentaclesPart6;
    public final EntityGiantSquidPart mantleCollisionPart;
    public final EntityGiantSquidPart[] allParts;
    public final float[][] ringBuffer = new float[64][2];
    public int ringBufferIndex = -1;
    public float prevSquidPitch;
    public float prevDepressurization;
    public float grabProgress;
    public float prevGrabProgress;
    public float dryProgress;
    public float prevDryProgress;
    public float capturedProgress;
    public float prevCapturedProgress;
    public int humTick = 0;
    private int holdTime;
    private int resetCapturedStateIn;

    public EntityGiantSquid(EntityType<? extends EntityGiantSquid> type, World level) {
        super(type, level);
        this.setPathfindingPenalty(PathNodeType.WATER, 0.0F);
        this.mantlePart1 = new EntityGiantSquidPart(this, 0.9F, 0.9F);
        this.mantlePart2 = new EntityGiantSquidPart(this, 1.2F, 1.2F);
        this.mantlePart3 = new EntityGiantSquidPart(this, 0.45F, 0.45F);
        this.tentaclesPart1 = new EntityGiantSquidPart(this, 0.9F, 0.9F);
        this.tentaclesPart2 = new EntityGiantSquidPart(this, 1F, 1F);
        this.tentaclesPart3 = new EntityGiantSquidPart(this, 1.2F, 1.2F);
        this.tentaclesPart4 = new EntityGiantSquidPart(this, 1.2F, 1.2F);
        this.tentaclesPart5 = new EntityGiantSquidPart(this, 1.2F, 1.2F);
        this.tentaclesPart6 = new EntityGiantSquidPart(this, 1.2F, 1.2F);
        this.mantleCollisionPart = new EntityGiantSquidPart(this, 2.9F, 2.9F, true);
        this.allParts = new EntityGiantSquidPart[]{this.mantlePart1, this.mantlePart2, this.mantlePart3, this.mantleCollisionPart, this.tentaclesPart1, this.tentaclesPart2, this.tentaclesPart3, this.tentaclesPart4, this.tentaclesPart5, this.tentaclesPart6};
        this.lookControl = new YawAdjustingLookControl(this, 4);
        this.moveControl = new AquaticMoveController(this, 1.2F, 5);
    }

    @Override
    public boolean canSpawn(WorldAccess worldIn, SpawnReason spawnReasonIn) {
        return AMEntityRegistry.rollSpawn(AMConfig.giantSquidSpawnRolls, this.getRandom(), spawnReasonIn);
    }

    public static <T extends MobEntity> boolean canGiantSquidSpawn(EntityType<T> entityType, ServerWorldAccess iServerWorld, SpawnReason reason, BlockPos pos, Random random) {
        return reason == SpawnReason.SPAWNER || iServerWorld.isWater(pos) && iServerWorld.isWater(pos.up());
    }

    @Nullable
    @Override
    public EntityData initialize(ServerWorldAccess worldIn, LocalDifficulty difficultyIn, SpawnReason reason, EntityData spawnDataIn, NbtCompound dataTag) {
        if (reason == SpawnReason.NATURAL) {
            doInitialPosing(worldIn);
        }
        return super.initialize(worldIn, difficultyIn, reason, spawnDataIn, dataTag);
    }

    private void doInitialPosing(ServerWorldAccess world) {
        var down = this.getBlockPos();
        while(!world.getFluidState(down).isEmpty() && down.getY() > 1){
            down = down.down();
        }
        this.setPos(down.getX() + 0.5F, down.getY() + 3 + random.nextInt(3), down.getZ() + 0.5F);
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return AMSoundRegistry.GIANT_SQUID_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return AMSoundRegistry.GIANT_SQUID_HURT.get();
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createLivingAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 38.0D)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 8.0D)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 16.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.25F);
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(SQUID_PITCH, 0F);
        this.dataTracker.startTracking(OVERRIDE_BODYROT, false);
        this.dataTracker.startTracking(DEPRESSURIZATION, 0F);
        this.dataTracker.startTracking(GRABBING, false);
        this.dataTracker.startTracking(CAPTURED, false);
        this.dataTracker.startTracking(BLUE, false);
        this.dataTracker.startTracking(GRAB_ENTITY, -1);
    }

    @Nullable
    public Entity getGrabbedEntity() {
        if (!this.getWorld().isClient || this.dataTracker.get(GRAB_ENTITY) == -1) {
            return this.getTarget();
        } else {
            return this.getWorld().getEntityById(this.dataTracker.get(GRAB_ENTITY));
        }
    }

    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        return super.interactMob(player, hand);
    }

    @Override
    protected EntityNavigation createNavigation(World worldIn) {
        return new SwimNavigation(this, worldIn);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(1, new MoveIntoWaterGoal(this));
        this.goalSelector.add(1, new AIAvoidWhales());
        this.goalSelector.add(2, new AIMelee());
        this.goalSelector.add(3, new AIDeepwaterSwimming());
        this.goalSelector.add(5, new LookAroundGoal(this));
        this.targetSelector.add(1, (new RevengeGoal(this, EntityCachalotWhale.class)));
        this.targetSelector.add(2, new EntityAINearestTarget3D<>(this, GuardianEntity.class, 20, true, true, null) {
            @Override
            public boolean canStart() {
                return super.canStart();
            }
        });
        this.targetSelector.add(3, new EntityAINearestTarget3D<>(this, LivingEntity.class, 70, false, true, AMEntityRegistry.buildPredicateFromTag(AMTagRegistry.GIANT_SQUID_TARGETS)) {
            @Override
            public boolean canStart() {
                return  !EntityGiantSquid.this.isInsideWaterOrBubbleColumn() && !EntityGiantSquid.this.isCaptured() && super.canStart();
            }
        });
    }

    @Override
    public void tickMovement() {
        super.tickMovement();
        if (!this.isAiDisabled()) {
            if (this.ringBufferIndex < 0) {
                //initial population of buffer
                for (int i = 0; i < this.ringBuffer.length; ++i) {
                    this.ringBuffer[i][0] = 180 + this.getYaw();
                    this.ringBuffer[i][1] = this.getSquidPitch();
                }
            }
            this.ringBufferIndex++;
            if (this.ringBufferIndex == this.ringBuffer.length) {
                this.ringBufferIndex = 0;
            }
            this.ringBuffer[this.ringBufferIndex][0] = this.bodyYaw;
            this.ringBuffer[this.ringBufferIndex][1] = this.getSquidPitch();
        }
    }

    @Override
    public void tick() {
        super.tick();
        if(this.age % 100 == 0){
            this.heal(2);
        }
        float f = MathHelper.wrapDegrees(180 + this.getYaw());
        this.bodyYaw = rotlerp(this.bodyYaw, f, 180);
        prevSquidPitch = getSquidPitch();
        prevDepressurization = getDepressurization();
        prevDryProgress = dryProgress;
        prevGrabProgress = grabProgress;
        prevCapturedProgress = capturedProgress;
        if (!this.isTouchingWater() && dryProgress < 5F) {
            dryProgress++;
        }
        if (this.isTouchingWater() && dryProgress > 0F) {
            dryProgress--;
        }

        if (this.isGrabbing()) {
            if (grabProgress < 5F)
                grabProgress += 0.25F;
        } else {
            if (grabProgress > 0F)
                grabProgress -= 0.25F;
        }

        if (this.isCaptured()) {
            if (capturedProgress < 5F)
                capturedProgress += 0.5F;
        } else {
            if (capturedProgress > 0F)
                capturedProgress -= 0.5F;
        }

        if (this.isGrabbing()) {
            var target = getGrabbedEntity();
            if(!this.getWorld().isClient && target != null){
                this.dataTracker.set(GRAB_ENTITY, target.getId());
                if (holdTime % 20 == 0 && holdTime > 30) {
                    target.damage(this.getDamageSources().mobAttack(this), 3 + random.nextInt(5));
                }
            }
            if (target != null && target.isAlive()) {
                this.setPitch(0);
                float invert = 1F - grabProgress * 0.2F;
                var extraVec = new Vec3d(0, 0, 2F + invert * 7F).rotateX(-this.getPitch() * MathHelper.RADIANS_PER_DEGREE).rotateY(-this.bodyYaw * MathHelper.RADIANS_PER_DEGREE);
                var minus = new Vec3d(this.getX() + extraVec.x - target.getX(), this.getY() + extraVec.y - target.getY(), this.getZ() + extraVec.z - target.getZ());
                target.setVelocity(minus);
            }
            holdTime++;
            if (holdTime > 1000) {
                holdTime = 0;
                this.setGrabbing(false);
            }
        } else {
            holdTime = 0;
        }
        if (!this.isAiDisabled()) {
            var avector3d = new Vec3d[this.allParts.length];
            for (int j = 0; j < this.allParts.length; ++j) {
                this.allParts[j].collideWithNearbyEntities();
                avector3d[j] = new Vec3d(this.allParts[j].getX(), this.allParts[j].getY(), this.allParts[j].getZ());
            }
            final float pitch = this.getPitch() * MathHelper.RADIANS_PER_DEGREE * 0.8F;
            this.mantleCollisionPart.setPos(this.getX(), this.getY() - ((this.mantleCollisionPart.getHeight() - this.getStandingEyeHeight()) * 0.5F) * (1F - dryProgress * 0.2F), this.getZ());
            this.setPartPositionFromBuffer(this.mantlePart1, pitch, 0.9F, 0);
            this.setPartPositionFromBuffer(this.mantlePart2, pitch, 1.6F, 0);
            this.setPartPositionFromBuffer(this.mantlePart3, pitch, 2.45F, 0);
            this.setPartPositionFromBuffer(this.tentaclesPart1, pitch, -0.8F, 0);
            this.setPartPositionFromBuffer(this.tentaclesPart2, pitch, -1.5F, 0);
            this.setPartPositionFromBuffer(this.tentaclesPart3, pitch, -2.3F, 5);
            this.setPartPositionFromBuffer(this.tentaclesPart4, pitch, -3.4F, 10);
            this.setPartPositionFromBuffer(this.tentaclesPart5, pitch, -5.4F, 15);
            this.setPartPositionFromBuffer(this.tentaclesPart6, pitch, -7.4F, 20);
            if (this.isInsideWaterOrBubbleColumn()) {
                if (this.mantleCollisionPart.scale != 1F) {
                    this.mantleCollisionPart.scale = 1F;
                    this.mantleCollisionPart.calculateDimensions();
                }
            } else {
                if (this.mantleCollisionPart.scale != 0.25F) {
                    this.mantleCollisionPart.scale = 0.25F;
                    this.mantleCollisionPart.calculateDimensions();
                }
            }
            for (int l = 0; l < this.allParts.length; ++l) {
                this.allParts[l].prevX = avector3d[l].x;
                this.allParts[l].prevY = avector3d[l].y;
                this.allParts[l].prevZ = avector3d[l].z;
                this.allParts[l].lastRenderX = avector3d[l].x;
                this.allParts[l].lastRenderY = avector3d[l].y;
                this.allParts[l].lastRenderZ = avector3d[l].z;
            }
            this.setNoGravity(this.isTouchingWater());
        }
        if (!this.getWorld().isClient) {
            if (this.getSquidPitch() > 0F) {
                float decrease = Math.min(2F, this.getSquidPitch());
                this.decrementSquidPitch(decrease);
            }
            if (this.getSquidPitch() < 0F) {
                float decrease = Math.min(2F, -this.getSquidPitch());
                this.incrementSquidPitch(decrease);
            }
            if (this.isInsideWaterOrBubbleColumn()) {
                float dist = (float) this.getVelocity().y * 45;
                if (dataTracker.get(OVERRIDE_BODYROT)) {
                    this.decrementSquidPitch(dist);
                } else {
                    this.incrementSquidPitch(dist);
                }
            }
            if (!this.isOnGround() && this.getFluidHeight(FluidTags.WATER) < this.getHeight()) {
                this.setVelocity(this.getVelocity().add(0, -0.1F, 0));
            }
            float pressure = getDepressuregetWorld();
            if (this.getDepressurization() < pressure) {
                this.setDepressurization(this.getDepressurization() + 0.1F);
            }
            if (this.getDepressurization() > pressure) {
                this.setDepressurization(this.getDepressurization() - 0.1F);
            }
        }
        if (this.isHumming()) {
            if (humTick % 20 == 0) {
                this.playSound(AMSoundRegistry.GIANT_SQUID_GAMES.get(), this.getSoundVolume(), 1);
                humTick = 0;
            }
            humTick++;
        }
        if(!this.getWorld().isClient){
            if(resetCapturedStateIn > 0){
                resetCapturedStateIn--;
            }else{
                this.setCaptured(false);
            }
        }
    }

    private boolean isHumming() {
        var s = Formatting.strip(this.getName().getString());
        return s != null && s.toLowerCase().contains("squid games!!") || AlexsMobs.isAprilFools();
    }

    public float getRingBuffer(int bufferOffset, float partialTicks, boolean pitch) {
        int i = (this.ringBufferIndex - bufferOffset) & 63;
        int j = (this.ringBufferIndex - bufferOffset - 1) & 63;
        int k = pitch ? 1 : 0;
        float prevBuffer = this.ringBuffer[j][k];
        float buffer = this.ringBuffer[i][k];
        float end = prevBuffer + (buffer - prevBuffer) * partialTicks;
        return rotlerp(prevBuffer, end, 10);
    }

    private void setPartPosition(EntityGiantSquidPart part, double offsetX, double offsetY, double offsetZ, float offsetScale) {
        part.setPosition(this.getX() + offsetX * offsetScale * part.scale, this.getY() + offsetY * offsetScale * part.scale, this.getZ() + offsetZ * offsetScale * part.scale);
    }

    private void setPartPositionFromBuffer(EntityGiantSquidPart part, float pitch, float offsetScale, int ringBufferOffset) {
        float f2 = MathHelper.sin(getRingBuffer(ringBufferOffset, 1.0F, false) * MathHelper.RADIANS_PER_DEGREE) * (1 - Math.abs((this.getPitch()) / 90F));
        float f3 = MathHelper.cos(getRingBuffer(ringBufferOffset, 1.0F, false) * MathHelper.RADIANS_PER_DEGREE) * (1 - Math.abs((this.getPitch()) / 90F));
        setPartPosition(part, f2, pitch, -f3, offsetScale);
    }

    @Override
    public int getMaxLookPitchChange() {
        return 1;
    }

    @Override
    public int getMaxHeadRotation() {
        return 3;
    }

    @Override
    public void travel(Vec3d travelVector) {
        if (this.canMoveVoluntarily() && this.isTouchingWater()) {
            if (dataTracker.get(OVERRIDE_BODYROT)) {
                travelVector = new Vec3d(travelVector.x, travelVector.y, -travelVector.z);
            }
            this.updateVelocity(this.getMovementSpeed(), travelVector);
            double d = this.getTarget() == null ? 0.6D : 0.9D;
            this.setVelocity(this.getVelocity().multiply(0.9D, d, 0.9D));
            this.move(MovementType.SELF, this.getVelocity());
        } else {
            super.travel(travelVector);
        }
    }

    @Override
    public boolean canBreatheInWater() {
        return true;
    }

    @Override
    public boolean isPushedByFluids() {
        return false;
    }

    @Override
    public EntityGroup getGroup() {
        return EntityGroup.AQUATIC;
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound compound) {
        super.readCustomDataFromNbt(compound);
        this.setBlue(compound.getBoolean("Blue"));
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound compound) {
        super.writeCustomDataToNbt(compound);
        compound.putBoolean("Blue", isBlue());
    }

    @Override
    public boolean canSpawn(WorldView worldIn) {
        return worldIn.doesNotIntersectEntities(this);
    }

    public float getDepressurization() {
        return MathHelper.clamp(dataTracker.get(DEPRESSURIZATION), 0, 1F);
    }

    public void setDepressurization(float depressurization) {
        dataTracker.set(DEPRESSURIZATION, depressurization);
    }

    public float getSquidPitch() {
        return MathHelper.clamp(dataTracker.get(SQUID_PITCH), -90, 90);
    }

    public void setSquidPitch(float pitch) {
        dataTracker.set(SQUID_PITCH, pitch);
    }

    public void incrementSquidPitch(float pitch) {
        dataTracker.set(SQUID_PITCH, getSquidPitch() + pitch);
    }

    public void decrementSquidPitch(float pitch) {
        dataTracker.set(SQUID_PITCH, getSquidPitch() - pitch);
    }

    public boolean isGrabbing() {
        return this.dataTracker.get(GRABBING);
    }

    public void setGrabbing(boolean running) {
        this.dataTracker.set(GRABBING, running);
    }

    public boolean isCaptured() {
        return this.dataTracker.get(CAPTURED);
    }

    public void setCaptured(boolean running) {
        this.dataTracker.set(CAPTURED, running);
    }

    public boolean isBlue() {
        return this.dataTracker.get(BLUE);
    }

    public void setBlue(boolean t) {
        this.dataTracker.set(BLUE, t);
    }

    @Override
    public void pushAway(Entity entity) {
        if (!this.isCaptured()) {
            super.pushAway(entity);
        }
    }

    @Override
    public void updateLimbs(boolean flying) {
        float f1 = (float)MathHelper.magnitude(this.getX() - this.prevX, this.getY() - this.prevY, this.getZ() - this.prevZ);
        float f2 = Math.min(f1 * 8.0F, 1.0F);
        this.limbAnimator.updateLimbs(f2, 0.4F);
    }

    @Override
    public boolean isCollidable() {
        return this.isAlive();
    }

    @Override
    public float getSoundPitch() {
        return getSquidPitch();
    }

    @Override
    public Vec3d adjustMovementForCollisions(Vec3d movement, Supplier<Vec3d> supplier) {
        if (this.isRegionUnloaded() || !this.isInsideWaterOrBubbleColumn()) {
            return supplier.get();
        }

        var aabb = this.mantleCollisionPart.getBoundingBox();
        List<VoxelShape> list = this.getWorld().getEntityCollisions(this, aabb.stretch(movement));
        var vec3 = movement.lengthSquared() == 0.0D ? movement : adjustMovementForCollisions(this, movement, aabb, this.getWorld(), list);
        boolean flag = movement.x != vec3.x;
        boolean flag1 = movement.y != vec3.y;
        boolean flag2 = movement.z != vec3.z;
        boolean flag3 = this.isOnGround() || flag1 && movement.y < 0.0D;
        if (this.getStepHeight() > 0.0F && flag3 && (flag || flag2)) {
            var vec31 = adjustMovementForCollisions(this, new Vec3d(movement.x, this.getStepHeight(), movement.z), aabb, this.getWorld(), list);
            var vec32 = adjustMovementForCollisions(this, new Vec3d(0.0D, this.getStepHeight(), 0.0D), aabb.expand(movement.x, 0.0D, movement.z), this.getWorld(), list);
            if (vec32.y < (double) this.getStepHeight()) {
                var vec33 = Entity.adjustMovementForCollisions(this, new Vec3d(movement.x, 0.0D, movement.z), aabb.offset(vec32), this.getWorld(), list).add(vec32);
                if (vec33.horizontalLengthSquared() > vec31.horizontalLengthSquared()) {
                    vec31 = vec33;
                }
            }

            if (vec31.horizontalLengthSquared() > vec3.horizontalLengthSquared()) {
                return vec31.add(Entity.adjustMovementForCollisions(this, new Vec3d(0.0D, -vec31.y + movement.y, 0.0D), aabb.offset(vec31), this.getWorld(), list));
            }
        }

        return vec3;
    }

    @Override
    public PartEntity<?>[] getParts() {
        return this.allParts;
    }

    public boolean attackEntityPartFrom(EntityGiantSquidPart part, DamageSource source, float amount) {
        return this.damage(source, amount);
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return source.isOf(DamageTypes.IN_WALL) || super.isInvulnerableTo(source);
    }

    public void directPitch(double d0, double d1, double d2, double d3) {
        boolean shift = this.dataTracker.get(OVERRIDE_BODYROT);
        float add = shift ? 90.0F : -90.0F;
        float f = (float) (MathHelper.atan2(d2, d0) * 57.2957763671875D) + add;
        this.setYaw(this.rotlerp(this.getYaw(), f, shift ? 10 : 5));
    }

    @Override
    public float getPitch(float partialTick) {
        return prevSquidPitch + (getSquidPitch() - prevSquidPitch) * partialTick;
    }

    @Override
    public float getYaw(float partialTick) {
        return partialTick == 1.0F ? this.bodyYaw : MathHelper.lerp(partialTick, this.prevBodyYaw, this.bodyYaw);
    }

    protected float rotlerp(float in, float target, float maxShift) {
        float f = MathHelper.wrapDegrees(target - in);
        if (f > maxShift) {
            f = maxShift;
        }

        if (f < -maxShift) {
            f = -maxShift;
        }

        float f1 = in + f;
        if (f1 < 0.0F) {
            f1 += 360.0F;
        } else if (f1 > 360.0F) {
            f1 -= 360.0F;
        }

        return f1;
    }

    private float getDepressuregetWorld() {
        var blockpos$mutable = new BlockPos.Mutable();
        int waterLevelAbove = 0;
        while (waterLevelAbove < 10) {
            var blockstate = getWorld().getBlockState(blockpos$mutable.set(this.getX(), this.getY() + waterLevelAbove, this.getZ()));
            if (!blockstate.getFluidState().isIn(FluidTags.WATER) && !blockstate.isSolid()) {
                break;
            } else {
                waterLevelAbove++;
            }
        }
        return 1F - (waterLevelAbove / 10F);
    }

    private boolean canFitAt(BlockPos pos) {
        return true;
    }

    public boolean tickCaptured(EntityCachalotWhale whale) {
        resetCapturedStateIn = 25;
        if (random.nextInt(13) == 0) {
            spawnInk();
            whale.damage(this.getDamageSources().mobAttack(this), 4 + random.nextInt(4));
            if (random.nextFloat() <= 0.3F) {
                this.setCaptured(false);
                if(random.nextFloat() < 0.2F){
                    this.dropStack(AMItemRegistry.LOST_TENTACLE.get().getDefaultStack());
                }
                return true;
            }
        }
        this.setCaptured(true);
        this.setSquidPitch(0);
        return false;
    }

    @Override
    public void handleStatus(byte id) {
        super.handleStatus(id);
    }

    @Override
    public boolean damage(DamageSource src, float f) {
        if (super.damage(src, f) && this.getLastAttacker() != null && !this.isCaptured() && random.nextBoolean()) {
            this.spawnInk();
            return true;
        } else {
            return false;
        }
    }

    private void spawnInk() {
        this.emitGameEvent(GameEvent.ENTITY_INTERACT);
        this.playSound(SoundEvents.ENTITY_SQUID_SQUIRT, this.getSoundVolume(), 0.5F * this.getSoundPitch());
        if (!this.getWorld().isClient) {
            var inkDirection = new Vec3d(0, 0, 1.2F).rotateX(-this.getPitch() * MathHelper.RADIANS_PER_DEGREE).rotateY(-this.bodyYaw * MathHelper.RADIANS_PER_DEGREE);
            var vec3 = this.getPos().add(inkDirection);
            for (int i = 0; i < 30; ++i) {
                var vec32 = inkDirection.add(random.nextFloat() - 0.5F, random.nextFloat() - 0.5F, random.nextFloat() - 0.5F).multiply(0.8D + (double) (this.random.nextFloat() * 2.0F));
                ((ServerWorld) this.getWorld()).spawnParticles(ParticleTypes.SQUID_INK, vec3.x, vec3.y + 0.5D, vec3.z, 0, vec32.x, vec32.y, vec32.z, 0.1F);
            }
        }
    }

    private class AIAvoidWhales extends Goal {

        private EntityCachalotWhale whale;
        private Vec3d moveTo;
        private int runDelay;

        public AIAvoidWhales() {
            this.setControls(EnumSet.of(Goal.Control.MOVE));
        }

        @Override
        public boolean canStart() {
            if (EntityGiantSquid.this.isInsideWaterOrBubbleColumn() && !EntityGiantSquid.this.horizontalCollision && !EntityGiantSquid.this.isCaptured() && runDelay-- <= 0) {
                EntityCachalotWhale closest = null;
                float dist = 50;
                for (var dude : EntityGiantSquid.this.getWorld().getNonSpectatingEntities(EntityCachalotWhale.class, EntityGiantSquid.this.getBoundingBox().expand(dist))) {
                    if (closest == null || dude.distanceTo(EntityGiantSquid.this) < closest.distanceTo(EntityGiantSquid.this)) {
                        closest = dude;
                    }
                }
                if (closest != null) {
                    whale = closest;
                    return true;
                }
                runDelay = 50 + random.nextInt(50);
            }

            return false;
        }

        @Override
        public boolean shouldContinue() {
            return whale != null && whale.isAlive() && !EntityGiantSquid.this.horizontalCollision && EntityGiantSquid.this.distanceTo(whale) < 60;
        }

        @Override
        public void tick() {
            if (whale != null && whale.isAlive()) {
                double dist = EntityGiantSquid.this.distanceTo(whale);
                var vec = EntityGiantSquid.this.getPos().subtract(whale.getPos()).normalize();
                var vec2 = EntityGiantSquid.this.getPos().add(vec.multiply(12 + random.nextInt(5)));
                EntityGiantSquid.this.getNavigation().startMovingTo(vec2.x, vec2.y, vec2.z, dist < 20 ? 1.9F : 1.3F);
            }
        }

        @Override
        public void stop() {
            whale = null;
            moveTo = null;
        }
    }

    private class AIDeepwaterSwimming extends Goal {

        private BlockPos moveTo;

        public AIDeepwaterSwimming() {
            this.setControls(EnumSet.of(Goal.Control.MOVE));
        }

        @Override
        public boolean canStart() {
            if (EntityGiantSquid.this.hasPassengers() || EntityGiantSquid.this.getTarget() != null && !EntityGiantSquid.this.isGrabbing() || !EntityGiantSquid.this.isTouchingWater() && !EntityGiantSquid.this.isInLava()) {
                return false;
            } else {
                if (EntityGiantSquid.this.getNavigation().isIdle() || EntityGiantSquid.this.getRandom().nextInt(30) == 0) {
                    BlockPos found = findTargetPos();
                    if (found != null) {
                        moveTo = found;
                        return true;
                    }
                }
                return false;
            }
        }

        private BlockPos findTargetPos() {
            var r = EntityGiantSquid.this.getRandom();
            for (int i = 0; i < 15; i++) {
                var pos = EntityGiantSquid.this.getBlockPos().add(r.nextInt(16) - 8, r.nextInt(32) - 16, r.nextInt(16) - 8);
                if (EntityGiantSquid.this.getWorld().isWater(pos) && EntityGiantSquid.this.canFitAt(pos)) {
                    return getDeeperTarget(pos);
                }
            }
            return null;
        }

        private BlockPos getDeeperTarget(BlockPos waterAtPos){
            var surface = new BlockPos(waterAtPos);
            var seafloor = new BlockPos(waterAtPos);
            while (EntityGiantSquid.this.getWorld().isWater(surface) && surface.getY() < 320){
                surface = surface.up();
            }
            while (EntityGiantSquid.this.getWorld().isWater(seafloor) && seafloor.getY() > -64){
                seafloor = seafloor.down();
            }
            int distance = surface.getY() - seafloor.getY();
            if(distance < 10){
                return waterAtPos;
            }else{
                int i = (int) (distance * 0.4);
                return seafloor.up(1 + EntityGiantSquid.this.getRandom().nextInt(i));
            }
        }

        @Override
        public void start() {
            EntityGiantSquid.this.getNavigation().startMovingTo(moveTo.getX() + 0.5F, moveTo.getY() + 0.5F, moveTo.getZ() + 0.5F, 1.0F);
        }

        @Override
        public boolean shouldContinue() {
            return false;
        }
    }

    private class AIMelee extends Goal {

        @Override
        public boolean canStart() {
            return EntityGiantSquid.this.isInsideWaterOrBubbleColumn() && EntityGiantSquid.this.getTarget() != null && EntityGiantSquid.this.getTarget().isAlive();
        }

        @Override
        public void tick() {
            var squid = EntityGiantSquid.this;
            var target = EntityGiantSquid.this.getTarget();
            double dist = squid.distanceTo(target);
            if (squid.canSee(target) && dist < 7.0F) {
                squid.setGrabbing(true);
            } else {
                var moveBodyTo = target.getPos();
                squid.getNavigation().startMovingTo(moveBodyTo.x, moveBodyTo.y, moveBodyTo.z, 1.0F);
            }
            if (dist < 14.0F) {
                squid.dataTracker.set(OVERRIDE_BODYROT, true);
            } else {
                squid.dataTracker.set(OVERRIDE_BODYROT, false);
            }
        }

        @Override
        public void stop() {
            EntityGiantSquid.this.dataTracker.set(OVERRIDE_BODYROT, false);
            EntityGiantSquid.this.setGrabbing(false);
        }
    }
}
