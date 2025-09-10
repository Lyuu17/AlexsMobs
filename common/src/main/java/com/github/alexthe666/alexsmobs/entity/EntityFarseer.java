package com.github.alexthe666.alexsmobs.entity;

import com.github.alexthe666.alexsmobs.AlexsMobs;
import com.github.alexthe666.alexsmobs.config.AMConfig;
import com.github.alexthe666.alexsmobs.entity.ai.EntityAINearestTarget3D;
import com.github.alexthe666.alexsmobs.entity.util.Maths;
import com.github.alexthe666.alexsmobs.misc.AMDamageTypes;
import com.github.alexthe666.alexsmobs.packet.SendVisualFlagFromServerPacket;
import com.github.alexthe666.alexsmobs.registry.AMEntityRegistry;
import com.github.alexthe666.alexsmobs.registry.AMParticleRegistry;
import com.github.alexthe666.alexsmobs.registry.AMSoundRegistry;
import com.iafenvoy.uranus.animation.Animation;
import com.iafenvoy.uranus.animation.AnimationHandler;
import com.iafenvoy.uranus.animation.IAnimatedEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.control.MoveControl;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.ai.pathing.BirdNavigation;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.*;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public class EntityFarseer extends HostileEntity implements IAnimatedEntity {

    public static final Animation ANIMATION_EMERGE = Animation.create(50);
    private static final int HANDS = 4;
    private static final TrackedData<Boolean> ANGRY = DataTracker.registerData(EntityFarseer.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> HAS_EMERGED = DataTracker.registerData(EntityFarseer.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> MELEEING = DataTracker.registerData(EntityFarseer.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Integer> LASER_ENTITY_ID = DataTracker.registerData(EntityFarseer.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> LASER_ATTACK_LVL = DataTracker.registerData(EntityFarseer.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Float> LASER_DISTANCE = DataTracker.registerData(EntityFarseer.class, TrackedDataHandlerRegistry.FLOAT);
    public static final int LASER_ATTACK_DURATION = 10;
    public final double[][] positions = new double[64][4];
    public final float[] claspProgress = new float[HANDS];
    public final float[] prevClaspProgress = new float[HANDS];
    public final float[] strikeProgress = new float[HANDS];
    public final float[] prevStrikeProgress = new float[HANDS];
    public final boolean[] isStriking = new boolean[HANDS];
    public int posPointer = -1;
    public float angryProgress;
    public float prevAngryProgress;
    public Vec3d angryShakeVec = Vec3d.ZERO;
    public float prevLaserLvl;
    private float faceCameraProgress;
    private float prevFaceCameraProgress;
    private LivingEntity laserTargetEntity;
    private int claspingHand = -1;
    private int animationTick;
    private Animation currentAnimation;
    private int meleeCooldown = 0;

    public EntityFarseer(EntityType<EntityFarseer> type, World level) {
        super(type, level);
        this.moveControl = new MoveController();
        this.experiencePoints = 20;
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createLivingAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 70D)
                .add(EntityAttributes.GENERIC_ARMOR, 6.0D)
                .add(EntityAttributes.GENERIC_FLYING_SPEED, 0.5F)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 4.5D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.35F)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32.0D);
    }

    @Override
    public boolean canSpawn(WorldAccess worldIn, SpawnReason spawnReasonIn) {
        return AMEntityRegistry.rollSpawn(AMConfig.farseerSpawnRolls, this.getRandom(), spawnReasonIn);
    }

    @Override
    public boolean hasNoGravity() {
        return true;
    }

    public boolean handleFallDamage(float distance, float damageMultiplier) {
        return false;
    }

    @Override
    protected void fall(double y, boolean onGroundIn, BlockState state, BlockPos pos) {
    }

    @Override
    protected float getActiveEyeHeight(EntityPose pose, EntityDimensions dimensions) {
        return dimensions.height * 0.7F;
    }

    @Override
    protected EntityNavigation createNavigation(World level) {
        var flyingpathnavigation = new BirdNavigation(this, getWorld());
        flyingpathnavigation.setCanPathThroughDoors(false);
        flyingpathnavigation.setCanSwim(true);
        flyingpathnavigation.setCanEnterOpenDoors(true);
        return flyingpathnavigation;
    }

    @Override
    protected void initGoals() {
        super.initGoals();
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new AttackGoal());
        this.goalSelector.add(3, new RandomFlyGoal(this));
        this.goalSelector.add(4, new LookAtEntityGoal(this, PlayerEntity.class, 10));
        this.goalSelector.add(5, new LookAroundGoal(this));
        this.targetSelector.add(1, (new RevengeGoal(this)));
        this.targetSelector.add(2, new EntityAINearestTarget3D<>(this, PlayerEntity.class, 3, false, true, null));
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound compound) {
        super.writeCustomDataToNbt(compound);
        compound.putBoolean("Emerged", this.hasEmerged());
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound compound) {
        super.readCustomDataFromNbt(compound);
        this.setHasEmerged(compound.getBoolean("Emerged"));
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return AMSoundRegistry.FARSEER_IDLE.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return AMSoundRegistry.FARSEER_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return AMSoundRegistry.FARSEER_HURT.get();
    }

    public static boolean checkFarseerSpawnRules(EntityType<? extends HostileEntity> animal, ServerWorldAccess worldIn, SpawnReason reason, BlockPos pos, Random random) {
        return worldIn.getDifficulty() != Difficulty.PEACEFUL && isSpawnDark(worldIn, pos, random) && isFarseerArea(worldIn, pos);
    }

    private static boolean isFarseerArea(ServerWorldAccess iServerWorld, BlockPos pos) {
        return !AMConfig.restrictFarseerSpawns || iServerWorld.getWorldBorder().getDistanceInsideBorder(pos.getX(), pos.getZ()) < AMConfig.farseerBorderSpawnDistance;
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(HAS_EMERGED, false);
        this.dataTracker.startTracking(MELEEING, false);
        this.dataTracker.startTracking(ANGRY, false);
        this.dataTracker.startTracking(LASER_ENTITY_ID, -1);
        this.dataTracker.startTracking(LASER_ATTACK_LVL, 0);
        this.dataTracker.startTracking(LASER_DISTANCE, 0F);
    }

    public boolean isAngry() {
        return this.dataTracker.get(ANGRY);
    }

    public void setAngry(boolean angry) {
        this.dataTracker.set(ANGRY, Boolean.valueOf(angry));
    }

    public boolean hasLaser() {
        return this.dataTracker.get(LASER_ENTITY_ID) != -1 && this.getAnimation() != EntityFarseer.ANIMATION_EMERGE;
    }

    public int getLaserAttackLvl() {
        return this.dataTracker.get(LASER_ATTACK_LVL);
    }

    public float getLaserDistance() {
        return this.dataTracker.get(LASER_DISTANCE);
    }

    @Override
    public void onTrackedDataSet(TrackedData<?> p_32834_) {
        super.onTrackedDataSet(p_32834_);
        if (LASER_ENTITY_ID.equals(p_32834_)) {
            this.laserTargetEntity = null;
        }
    }

    @Nullable
    public LivingEntity getLaserTarget() {
        if (!this.hasLaser()) {
            return null;
        } else if (this.getWorld().isClient) {
            if (this.laserTargetEntity != null) {
                return this.laserTargetEntity;
            } else {
                var fromID = this.getWorld().getEntityById(this.dataTracker.get(LASER_ENTITY_ID));
                if (fromID instanceof LivingEntity) {
                    this.laserTargetEntity = (LivingEntity) fromID;
                    return this.laserTargetEntity;
                } else {
                    return null;
                }
            }
        } else {
            return this.getTarget();
        }
    }

    public boolean hasEmerged() {
        return this.dataTracker.get(HAS_EMERGED);
    }

    public void setHasEmerged(boolean emerged) {
        this.dataTracker.set(HAS_EMERGED, emerged);
    }

    @Override
    public void tick() {
        super.tick();
        prevFaceCameraProgress = faceCameraProgress;
        prevLaserLvl = this.getLaserAttackLvl();
        if (this.getAnimation() == ANIMATION_EMERGE) {
            this.setHasEmerged(true);
            faceCameraProgress = 1F;
        } else if (faceCameraProgress > 0.0F) {
            faceCameraProgress = Math.max(0, faceCameraProgress - 0.2F);
        }
        prevAngryProgress = angryProgress;
        for (int i = 0; i < HANDS; i++) {
            prevClaspProgress[i] = claspProgress[i];
            prevStrikeProgress[i] = strikeProgress[i];
        }
        if (this.posPointer < 0) {
            for (int i = 0; i < this.positions.length; ++i) {
                this.positions[i][0] = this.getX();
                this.positions[i][1] = this.getY();
                this.positions[i][2] = this.getZ();
                this.positions[i][3] = this.bodyYaw;
            }
        }
        if (++this.posPointer == this.positions.length) {
            this.posPointer = 0;
        }
        this.positions[this.posPointer][0] = this.getX();
        this.positions[this.posPointer][1] = this.getY();
        this.positions[this.posPointer][2] = this.getZ();
        this.positions[this.posPointer][3] = this.bodyYaw;
        if (this.isAngry() && angryProgress < 5F) {
            angryProgress++;
        }
        if (!this.isAngry() && angryProgress > 0F) {
            angryProgress--;
        }
        if (this.isAlive()) {
            if (random.nextInt(isAngry() ? 12 : 40) == 0 && claspingHand == -1) {
                int i = MathHelper.clamp(random.nextInt(HANDS), 0, 3);
                if (claspProgress[i] == 0) {
                    claspingHand = i;
                }
            }
            if (claspingHand >= 0) {
                if (claspProgress[claspingHand] < 5F) {
                    claspProgress[claspingHand]++;
                } else {
                    claspingHand = -1;
                }
            } else {
                for (int i = 0; i < HANDS; i++) {
                    if (claspProgress[i] > 0) {
                        claspProgress[i]--;
                    }
                }
            }
            if (!this.hasEmerged()) {
                this.setInvisible(true);
                if(this.getWorld().isPlayerInRange(this.getX(), this.getY(), this.getZ(), 9)){
                    this.setAnimation(ANIMATION_EMERGE);
                }
            }else{
                this.setInvisible(this.hasStatusEffect(StatusEffects.INVISIBILITY));
            }
            if (this.getAnimation() == ANIMATION_EMERGE) {
                if(this.getWorld().isClient){
                    this.getWorld().addParticle(AMParticleRegistry.STATIC_SPARK.get(), this.getParticleX(0.75F), this.getRandomBodyY(), this.getParticleZ(0.75F), (this.getRandom().nextFloat() - 0.5F) * 0.2F, this.getRandom().nextFloat() * 0.2F, (this.getRandom().nextFloat() - 0.5F) * 0.2F);
                }
                if(this.getAnimationTick() == 1){
                    this.playSound(AMSoundRegistry.FARSEER_EMERGE.get(), this.getSoundVolume(), this.getSoundPitch());
                }
            }
            var target = this.getTarget();
            if (target != null) {
                if (this.dataTracker.get(MELEEING)) {
                    if (meleeCooldown == 0) {
                        meleeCooldown = 5;
                        int i = random.nextInt(HANDS);
                        this.isStriking[i] = true;
                        this.getWorld().sendEntityStatus(this, (byte) (40 + i));
                    }
                }
            }
            if (meleeCooldown > 0) {
                meleeCooldown--;
            }
            for (int i = 0; i < HANDS; i++) {
                if (!this.isStriking[i] || !this.dataTracker.get(MELEEING)) {
                    if (strikeProgress[i] > 0F) {
                        strikeProgress[i]--;
                    }
                } else if (this.isStriking[i]) {
                    if (strikeProgress[i] < 5F) {
                        strikeProgress[i]++;
                    }
                    if (strikeProgress[i] == 5F) {
                        isStriking[i] = false;
                        this.getWorld().sendEntityStatus(this, (byte) (44 + i));
                        if (target != null && distanceTo(target) <= 4F) {
                            target.damage(this.getDamageSources().mobAttack(this), 5 + random.nextInt(5));
                        }
                    }
                }
            }

            if (this.hasLaser()) {
                var livingentity = this.getLaserTarget();
                if (livingentity != null) {
                    var hit = this.calculateLaserHit(livingentity.getEyePos());
                    this.dataTracker.set(LASER_DISTANCE, (float) hit.distanceTo(this.getEyePos()));
                    this.getLookControl().lookAt(livingentity, 90.0F, 90.0F);
                    this.getLookControl().tick();
                    double d0 = hit.x - this.getX();
                    double d1 = hit.y - this.getEyeY();
                    double d2 = hit.z - this.getZ();
                    double d3 = Math.sqrt(d0 * d0 + d1 * d1 + d2 * d2);
                    d0 = d0 / d3;
                    d1 = d1 / d3;
                    d2 = d2 / d3;
                    float progress = this.getLaserAttackLvl() / (float) LASER_ATTACK_DURATION;
                    double d4 = this.random.nextDouble();
                    while (d4 < d3 * progress) {
                        d4 += 0.5F + 2F * this.random.nextDouble();
                        double width = d4 / (d3 * progress);
                        double d5 = (random.nextDouble() - 0.5F) * width;
                        double d6 = (random.nextDouble() - 0.5F) * width;
                        this.getWorld().addParticle(AMParticleRegistry.STATIC_SPARK.get(), this.getX() + d0 * d4 + d5, this.getEyeY() + d1 * d4, this.getZ() + d2 * d4 + d6, (this.getRandom().nextFloat() - 0.5F) * 0.2F, this.getRandom().nextFloat() * 0.2F, (this.getRandom().nextFloat() - 0.5F) * 0.2F);
                    }
                }
            }
        }
        if (this.isAngry()) {
            angryShakeVec = new Vec3d(random.nextFloat() - 0.5F, random.nextFloat() - 0.5F, random.nextFloat() - 0.5F);
        } else {
            angryShakeVec = Vec3d.ZERO;
        }
        AnimationHandler.INSTANCE.updateAnimations(this);
    }

    @Environment(EnvType.CLIENT)
    public void handleStatus(byte id) {
        if (id >= 40 && id <= 43) {
            int i = id - 40;
            isStriking[i] = true;
        } else if (id >= 44 && id <= 48) {
            int i = id - 44;
            isStriking[i] = false;
        } else {
            super.handleStatus(id);
        }
    }

    public double getLatencyVar(int pointer, int index, float partialTick) {
        if (this.isDead()) {
            partialTick = 1.0F;
        }
        int i = this.posPointer - pointer & 63;
        int j = this.posPointer - pointer - 1 & 63;
        double d0 = this.positions[j][index];
        double d1 = MathHelper.wrapDegrees(this.positions[i][index] - d0);
        return d0 + d1 * partialTick;
    }

    public Vec3d getLatencyOffsetVec(int offset, float partialTick) {
        double d0 = MathHelper.lerp(partialTick, this.lastRenderX, this.getX());
        double d1 = MathHelper.lerp(partialTick, this.lastRenderY, this.getY());
        double d2 = MathHelper.lerp(partialTick, this.lastRenderZ, this.getZ());
        float renderYaw = (float) this.getLatencyVar(offset, 3, partialTick);
        return new Vec3d(this.getLatencyVar(offset, 0, partialTick) - d0, this.getLatencyVar(offset, 1, partialTick) - d1, this.getLatencyVar(offset, 2, partialTick) - d2).rotateY(renderYaw * MathHelper.RADIANS_PER_DEGREE);
    }

    public Vec3d calculateAfterimagePos(float partialTick, boolean flip, float speed) {
        float f = (partialTick + this.age) * speed;
        float f1 = 0.1F;
        var v = new Vec3d((float) Math.sin(f) * f1, (float) Math.cos(f - Math.PI / 2) * f1, -(float) Math.cos(f) * f1);
        if (flip) {
            return new Vec3d(v.z, -v.y, v.x);
        }
        return v;
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
    public Animation getAnimation() {
        return currentAnimation;
    }

    @Override
    public void setAnimation(Animation animation) {
        this.currentAnimation = animation;
    }

    @Override
    public Animation[] getAnimations() {
        return new Animation[]{ANIMATION_EMERGE};
    }

    public int getPortalFrame() {
        if (this.getAnimation() == ANIMATION_EMERGE) {
            if (this.getAnimationTick() < 10) {
                return 0;
            } else if (this.getAnimationTick() < 20) {
                return 1;
            } else if (this.getAnimationTick() < 30) {
                return 2;
            } else if (this.getAnimationTick() > 40) {
                int i = 50 - this.getAnimationTick();
                return i < 6 ? i < 3 ? 0 : 1 : 2;
            } else {
                return 3;
            }
        }
        return 0;
    }

    public float getPortalOpacity(float partialTicks) {
        if (this.getAnimation() == ANIMATION_EMERGE) {
            float tick = this.getAnimationTick() - 1 + partialTicks;
            if (tick < 5F) {
                return tick / 5F;
            }
            return 1.0F;
        }
        return 0.0F;
    }

    public float getFarseerOpacity(float partialTicks) {
        if (this.getAnimation() == ANIMATION_EMERGE) {
            float tick = this.getAnimationTick() - 1 + partialTicks;
            float prog = tick / (float) ANIMATION_EMERGE.getDuration();
            return prog > 0.5F ? (prog - 0.5F) / 0.5F : 0F;
        }
        return 1.0F;
    }

    public float getFacingCameraAmount(float partialTicks) {
        return prevFaceCameraProgress + (faceCameraProgress - prevFaceCameraProgress) * partialTicks;
    }

    public boolean canMoveVoluntarily() {
        return super.canMoveVoluntarily() && this.getAnimation() != ANIMATION_EMERGE && this.hasEmerged();
    }

    private Vec3d calculateLaserHit(Vec3d target) {
        var eyes = new Vec3d(this.getX(), this.getEyeY(), this.getZ());
        var hitResult = this.getWorld().raycast(new RaycastContext(eyes, target, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, this));
        return hitResult.getPos();
    }

    public boolean isTargetBlocked(Vec3d target) {
        var Vector3d = new Vec3d(this.getX(), this.getEyeY(), this.getZ());
        return this.getWorld().raycast(new RaycastContext(Vector3d, target, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, this)).getType() != HitResult.Type.MISS;
    }

    @Override
    public void travel(Vec3d vec3) {
        if (this.canMoveVoluntarily() || this.isLogicalSideForUpdatingMovement()) {
            if (this.isTouchingWater()) {
                this.updateVelocity(0.02F, vec3);
                this.move(MovementType.SELF, this.getVelocity());
                this.setVelocity(this.getVelocity().multiply(0.8F));
            } else if (this.isInLava()) {
                this.updateVelocity(0.02F, vec3);
                this.move(MovementType.SELF, this.getVelocity());
                this.setVelocity(this.getVelocity().multiply(0.5D));
            } else {
                this.updateVelocity(this.getMovementSpeed(), vec3);
                this.move(MovementType.SELF, this.getVelocity());
                this.setVelocity(this.getVelocity().multiply(0.91F));
            }
        }

        this.updateLimbs(false);
    }

    @Override
    public boolean isInvulnerableTo(DamageSource dmg) {
        return super.isInvulnerableTo(dmg) || this.getAnimation() == ANIMATION_EMERGE;
    }

    private static class RandomFlyGoal extends Goal {
        private final EntityFarseer parentEntity;
        private BlockPos target = null;
        private final float speed = 0.6F;
        public RandomFlyGoal(EntityFarseer mosquito) {
            this.parentEntity = mosquito;
            this.setControls(EnumSet.of(Goal.Control.MOVE));
        }

        @Override
        public boolean canStart() {
            if (this.parentEntity.getNavigation().isIdle() && this.parentEntity.getTarget() == null && this.parentEntity.getRandom().nextInt(4) == 0) {
                target = getBlockInViewFarseer();
                if (target != null) {
                    this.parentEntity.getMoveControl().moveTo(target.getX() + 0.5D, target.getY() + 0.5D, target.getZ() + 0.5D, speed);
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean shouldContinue() {
            return target != null && parentEntity.getTarget() == null;
        }

        @Override
        public void stop() {
            target = null;
        }

        @Override
        public void tick() {
            if (target != null) {
                this.parentEntity.getMoveControl().moveTo(target.getX() + 0.5D, target.getY() + 0.5D, target.getZ() + 0.5D, speed);
                if (parentEntity.squaredDistanceTo(Vec3d.ofCenter(target)) < 4D || this.parentEntity.horizontalCollision) {
                    target = null;
                }
            }
        }

        private BlockPos getFarseerGround(BlockPos in) {
            BlockPos position = new BlockPos(in.getX(), (int) parentEntity.getY(), in.getZ());
            while (position.getY() < 256 && !parentEntity.getWorld().getFluidState(position).isEmpty()) {
                position = position.up();
            }
            while (position.getY() > 1 && parentEntity.getWorld().isAir(position)) {
                position = position.down();
            }
            return position;
        }

        public BlockPos getBlockInViewFarseer() {
            float radius = 5 + parentEntity.getRandom().nextInt(10);
            float neg = parentEntity.getRandom().nextBoolean() ? 1 : -1;
            float renderYawOffset = parentEntity.getYaw();
            float angle = (Maths.STARTING_ANGLE * renderYawOffset) + 3.15F * (parentEntity.getRandom().nextFloat() * neg);
            double extraX = radius * MathHelper.sin(MathHelper.PI + angle);
            double extraZ = radius * MathHelper.cos(angle);
            var radialPos = new BlockPos((int) (parentEntity.getX() + extraX), (int) parentEntity.getY(), (int) (parentEntity.getZ() + extraZ));
            var ground = getFarseerGround(radialPos).up(2 + parentEntity.random.nextInt(2));

            if (!parentEntity.isTargetBlocked(Vec3d.ofCenter(ground.up()))) {
                return ground;
            }
            return null;
        }

    }

    private boolean canUseLaser() {
        return !this.hasStatusEffect(StatusEffects.BLINDNESS);
    }

    private class AttackGoal extends Goal {

        private boolean attackDecision = true;
        private int timeSinceLastSuccessfulAttack = 0;
        private int laserCooldown = 0;
        private int laserUseTime = 0;
        private int lasersShot = 0;

        public AttackGoal() {
            this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
        }

        @Override
        public boolean canStart() {
            return EntityFarseer.this.getTarget() != null && EntityFarseer.this.getTarget().isAlive();
        }

        @Override
        public void stop() {
            this.lasersShot = 0;
            this.laserCooldown = 0;
            this.laserUseTime = 0;
            attackDecision = EntityFarseer.this.getRandom().nextBoolean();
            EntityFarseer.this.dataTracker.set(LASER_ENTITY_ID, -1);
            timeSinceLastSuccessfulAttack = 0;
            EntityFarseer.this.setAngry(false);
        }

        @Override
        public void tick() {
            super.tick();
            LivingEntity target = EntityFarseer.this.getTarget();
            if (laserCooldown > 0) {
                laserCooldown--;
            }
            timeSinceLastSuccessfulAttack++;
            if (timeSinceLastSuccessfulAttack > 100) {
                timeSinceLastSuccessfulAttack = 0;
                attackDecision = !attackDecision;
            }
            if (target != null) {
                double dist = EntityFarseer.this.distanceTo(target);
                boolean canLaserHit = willLaserHit(target);
                if (this.laserCooldown == 0 && attackDecision && canLaserHit && dist > 2F) {
                    EntityFarseer.this.setAngry(true);
                    EntityFarseer.this.dataTracker.set(LASER_ENTITY_ID, target.getId());
                    if(laserUseTime == 0){
                        EntityFarseer.this.playSound(AMSoundRegistry.FARSEER_BEAM.get(), EntityFarseer.this.getSoundVolume(), EntityFarseer.this.getSoundPitch());
                    }
                    laserUseTime++;
                    if (laserUseTime > LASER_ATTACK_DURATION) {
                        laserUseTime = 0;
                        if (canLaserHit) {
                            float healthTenth = target.getMaxHealth() * 0.1F;
                            if (target.damage(AMDamageTypes.causeFarseerDamage(EntityFarseer.this), random.nextInt(2) + Math.max(6, healthTenth)) && !target.isAlive()){
                                AlexsMobs.sendMSGToAll(new SendVisualFlagFromServerPacket(target.getId(), 87));
                            }
                            timeSinceLastSuccessfulAttack = 0;
                        }
                        if (lasersShot++ > 5) {
                            lasersShot = 0;
                            laserCooldown = 80 + random.nextInt(40);
                            EntityFarseer.this.dataTracker.set(LASER_ENTITY_ID, -1);
                            attackDecision = EntityFarseer.this.getRandom().nextBoolean();
                        }
                    }
                    EntityFarseer.this.dataTracker.set(LASER_ATTACK_LVL, laserUseTime);
                    EntityFarseer.this.lookAtEntity(target, 180F, 180F);
                    if (dist < 17F && canLaserHit) {
                        EntityFarseer.this.getNavigation().stop();
                    } else {
                        EntityFarseer.this.getNavigation().startMovingTo(target, 1F);
                    }
                    EntityFarseer.this.dataTracker.set(MELEEING, false);
                } else {
                    if (!canLaserHit && dist > 10) {
                        EntityFarseer.this.setAngry(false);
                    }
                    if (EntityFarseer.this.hasLaser()) {
                        EntityFarseer.this.dataTracker.set(LASER_ENTITY_ID, -1);
                    }
                    EntityFarseer.this.dataTracker.set(MELEEING, dist < 4F);
                    if (dist < 4F) {
                        timeSinceLastSuccessfulAttack = 0;
                    } else {
                        EntityFarseer.this.getNavigation().startMovingTo(target, 1F);
                        EntityFarseer.this.moveControl.moveTo(target.getX(), target.getEyeY(), target.getZ(), 1F);
                    }
                }
            }
        }

        private boolean willLaserHit(LivingEntity target) {
            Vec3d vec = EntityFarseer.this.calculateLaserHit(target.getEyePos());
            return vec.distanceTo(target.getEyePos()) < 1F && EntityFarseer.this.canUseLaser();
        }
    }

    class MoveController extends MoveControl {
        private final MobEntity parentEntity;

        public MoveController() {
            super(EntityFarseer.this);
            this.parentEntity = EntityFarseer.this;
        }

        @Override
        public void tick() {
            float angle = (Maths.STARTING_ANGLE * (parentEntity.bodyYaw + 90));
            float radius = (float) Math.sin(parentEntity.age * 0.2F) * 2;
            double extraX = radius * MathHelper.sin(MathHelper.PI + angle);
            double extraY = radius * -Math.cos(angle - Math.PI / 2);
            double extraZ = radius * MathHelper.cos(angle);
            var strafPlus = new Vec3d(extraX, extraY, extraZ);
            if (this.state == MoveControl.State.MOVE_TO) {
                var vector3d = new Vec3d(this.targetX - parentEntity.getX(), this.targetY - parentEntity.getY(), this.targetZ - parentEntity.getZ());
                double d0 = vector3d.length();
                double width = parentEntity.getBoundingBox().getAverageSideLength();
                var shimmy = Vec3d.ZERO;
                var attackTarget = parentEntity.getTarget();
                if (attackTarget != null) {
                    if (parentEntity.horizontalCollision) {
                        shimmy = new Vec3d(0, 0.005, 0);
                    }
                }

                var vector3d1 = vector3d.multiply(this.speed * 0.05D / d0);
                parentEntity.setVelocity(parentEntity.getVelocity().add(vector3d1.add(strafPlus.multiply(0.003D * Math.min(d0, 100)).add(shimmy))));
                if (d0 >= width) {
                    parentEntity.setYaw(-((float) MathHelper.atan2(vector3d1.x, vector3d1.z)) * MathHelper.DEGREES_PER_RADIAN);
                    if (EntityFarseer.this.hasLaser()) {
                        parentEntity.bodyYaw = parentEntity.getYaw();
                    }
                }

            } else if (this.state == MoveControl.State.WAIT) {
                parentEntity.setVelocity(parentEntity.getVelocity().add(strafPlus.multiply(0.003D)));
            }
        }
    }
}
