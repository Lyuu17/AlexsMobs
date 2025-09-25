package com.github.alexthe666.alexsmobs.entity;

import com.github.alexthe666.alexsmobs.config.AMConfig;
import com.github.alexthe666.alexsmobs.entity.util.Maths;
import com.github.alexthe666.alexsmobs.misc.AMBlockPos;
import com.github.alexthe666.alexsmobs.registry.*;
import com.google.common.base.Predicates;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BeaconBlockEntity;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.control.MoveControl;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PhantomEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.poi.PointOfInterestStorage;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class EntitySunbird extends AnimalEntity implements Flutterer {

    public static final Predicate<? super Entity> SCORCH_PRED = (com.google.common.base.Predicate<Entity>) e -> e.isAlive() && e.getType().isIn(AMTagRegistry.SUNBIRD_SCORCH_TARGETS);

    private static final TrackedData<Boolean> SCORCHING = DataTracker.registerData(EntitySunbird.class, TrackedDataHandlerRegistry.BOOLEAN);
    public float birdPitch = 0;
    public float prevBirdPitch = 0;
    private int beaconSearchCooldown = 50;
    private BlockPos beaconPos = null;
    private boolean orbitClockwise = false;
    private float prevScorchProgress;
    private float scorchProgress;
    private int fullScorchTime;

    public EntitySunbird(EntityType<EntitySunbird> type, World worldIn) {
        super(type, worldIn);
        this.moveControl = new MoveHelperController(this);
        orbitClockwise = Random.create().nextBoolean();
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(SCORCHING, false);
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createLivingAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 20.0D)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 64.0D)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 2.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 1F);
    }

    public static boolean canSunbirdSpawn(EntityType<? extends MobEntity> typeIn, WorldAccess worldIn, SpawnReason reason, BlockPos pos, Random randomIn) {
        return true;
    }

    @Override
    public boolean canSpawn(WorldAccess worldIn, SpawnReason spawnReasonIn) {
        return AMEntityRegistry.rollSpawn(AMConfig.sunbirdSpawnRolls, this.getRandom(), spawnReasonIn);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return AMSoundRegistry.SUNBIRD_IDLE.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return AMSoundRegistry.SUNBIRD_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return AMSoundRegistry.SUNBIRD_HURT.get();
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(3, new RandomFlyGoal(this));
        this.goalSelector.add(4, new LookAtEntityGoal(this, PlayerEntity.class, 32F));
        this.goalSelector.add(5, new LookAroundGoal(this));
    }

    public float getBrightness() {
        return 1.0F;
    }

    @Override
    public boolean hasNoGravity() {
        return true;
    }

    @Override
    public boolean handleFallDamage(float distance, float damageMultiplier, DamageSource source) {
        return false;
    }

    @Override
    protected void fall(double y, boolean onGroundIn, BlockState state, BlockPos pos) {
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        boolean prev = super.damage(source, amount);
        if (prev) {
            if (source.getAttacker() != null) {
                if (source.getAttacker() instanceof LivingEntity) {
                    var hurter = (LivingEntity) source.getAttacker();
                    if (hurter.hasStatusEffect(AMEffectRegistry.SUNBIRD_BLESSING.get())) {
                        hurter.removeStatusEffect(AMEffectRegistry.SUNBIRD_BLESSING.get());
                    }
                    hurter.addStatusEffect(new StatusEffectInstance(AMEffectRegistry.SUNBIRD_CURSE.get(), 600, 0));
                }
            }
            return prev;
        }
        return prev;
    }

    @Override
    public void travel(Vec3d travelVector) {
        if (this.isTouchingWater()) {
            this.updateVelocity(0.02F, travelVector);
            this.move(MovementType.SELF, this.getVelocity());
            this.setVelocity(this.getVelocity().multiply(0.8F));
        } else if (this.isInLava()) {
            this.updateVelocity(0.02F, travelVector);
            this.move(MovementType.SELF, this.getVelocity());
            this.setVelocity(this.getVelocity().multiply(0.5D));
        } else {
            BlockPos ground = AMBlockPos.fromCoords(this.getX(), this.getY() - 1.0D, this.getZ());
            float f = 0.91F;
            if (this.isOnGround()) {
                // before getFriction(this.level(), ground, this)
                // arguments arent used
                f = this.getWorld().getBlockState(ground).getBlock().getSlipperiness() * 0.91F;
            }

            //float f1 = 0.16277137F / (f * f * f);
            f = 0.91F;
            //FIXME doesnt make any sense
            if (this.isOnGround()) {
                f = this.getWorld().getBlockState(ground).getBlock().getSlipperiness() * 0.91F;
            }
            this.updateLimbs(true);

            this.updateVelocity(0.2F, travelVector);
            this.move(MovementType.SELF, this.getVelocity());
            this.setVelocity(this.getVelocity().multiply(f));
        }

        this.updateLimbs(false);
    }

    @Override
    public void tick() {
        super.tick();
        prevBirdPitch = this.birdPitch;
        prevScorchProgress = this.scorchProgress;
        float f2 = (float) -((float) this.getVelocity().y * (double) MathHelper.DEGREES_PER_RADIAN);
        this.birdPitch = f2;
        if (this.getWorld().isClient) {
            final float radius = 0.35F + random.nextFloat() * 3.5F;
            final float angle = (Maths.STARTING_ANGLE * ((random.nextBoolean() ? -85F : 85F) + this.bodyYaw));
            final float angleMotion = (Maths.STARTING_ANGLE * this.bodyYaw);
            final double extraX = radius * MathHelper.sin(MathHelper.PI + angle);
            final double extraZ = radius * MathHelper.cos(angle);
            final double extraXMotion = -0.2F * MathHelper.sin((float) (Math.PI + angleMotion));
            final double extraZMotion = -0.2F * MathHelper.cos(angleMotion);
            final double yRandom = 0.2F + random.nextFloat() * 0.3F;
            this.getWorld().addParticle(AMParticleRegistry.SUNBIRD_FEATHER.get(), this.getX() + extraX, this.getY() + yRandom, this.getZ() + extraZ, extraXMotion, 0D, extraZMotion);
        } else {
            if (this.age % 100 == 0) {
                if(!this.isScorching() && !getScorchingMobs().isEmpty()){
                    this.setScorching(true);
                }
                var playerList = this.getWorld().getEntitiesByClass(PlayerEntity.class, this.getScorchArea(), Predicates.alwaysTrue());
                for (var e : playerList) {
                    if (!e.hasStatusEffect(AMEffectRegistry.SUNBIRD_BLESSING.get()) && !e.hasStatusEffect(AMEffectRegistry.SUNBIRD_CURSE.get())) {
                        e.addStatusEffect(new StatusEffectInstance(AMEffectRegistry.SUNBIRD_BLESSING.get(), 600, 0));
                    }
                }
            }
            if (beaconSearchCooldown > 0) {
                beaconSearchCooldown--;
            }
            if (beaconSearchCooldown <= 0) {
                beaconSearchCooldown = 100 + random.nextInt(200);
                if (getWorld() instanceof ServerWorld) {
                    List<BlockPos> beacons = this.getNearbyBeacons(this.getBlockPos(), (ServerWorld) getWorld(), 64);
                    BlockPos closest = null;
                    for (BlockPos pos : beacons) {
                        if (closest == null || this.squaredDistanceTo(closest.getX(), closest.getY(), closest.getZ()) > this.squaredDistanceTo(pos.getX(), pos.getY(), pos.getZ())) {
                            if (isValidBeacon(pos)) {
                                closest = pos;
                            }
                        }
                    }
                    if (closest != null && isValidBeacon(closest)) {
                        beaconPos = closest;
                    }
                }
                if (beaconPos != null) {

                    if (!isValidBeacon(beaconPos) && this.age > 40) {
                        this.beaconPos = null;
                    }
                }
            }
        }

        final boolean scorching = this.isScorching();
        if (scorching) {
            if (scorchProgress < 20F)
                scorchProgress++;
        } else {
            if (scorchProgress > 0F)
                scorchProgress--;
        }

        if (scorching && scorchProgress == 20F && !this.getWorld().isClient) {
            if(fullScorchTime > 30){
                this.setScorching(false);
            }else if(fullScorchTime % 5 == 0){
                for (Entity e : getScorchingMobs()) {
                    e.setOnFireFor(4);
                    if (e instanceof PhantomEntity) {
                        ((PhantomEntity) e).addStatusEffect(new StatusEffectInstance(AMEffectRegistry.SUNBIRD_CURSE.get(), 200, 0));
                    }
                }
            }
            fullScorchTime++;
        }else{
            fullScorchTime = 0;
        }
    }

    private List<LivingEntity> getScorchingMobs(){
        return this.getWorld().getEntitiesByClass(LivingEntity.class, this.getScorchArea(), SCORCH_PRED);
    }

    public boolean isScorching() {
        return this.dataTracker.get(SCORCHING);
    }

    public void setScorching(boolean scorching) {
        this.dataTracker.set(SCORCHING, Boolean.valueOf(scorching));
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound compound) {
        super.readCustomDataFromNbt(compound);
        if (compound.contains("BeaconPosX")) {
            int i = compound.getInt("BeaconPosX");
            int j = compound.getInt("BeaconPosY");
            int k = compound.getInt("BeaconPosZ");
            this.beaconPos = new BlockPos(i, j, k);
        } else {
            this.beaconPos = null;
        }
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound compound) {
        super.writeCustomDataToNbt(compound);
        BlockPos blockpos = this.beaconPos;
        if (blockpos != null) {
            compound.putInt("BeaconPosX", blockpos.getX());
            compound.putInt("BeaconPosY", blockpos.getY());
            compound.putInt("BeaconPosZ", blockpos.getZ());
        }
    }

    private Box getScorchArea() {
        return this.getBoundingBox().expand(15, 32, 15);
    }

    @Nullable
    @Override
    public PassiveEntity createChild(ServerWorld world, PassiveEntity entity) {
        return null;
    }

    public boolean isTargetBlocked(Vec3d target) {
        var Vector3d = new Vec3d(this.getX(), this.getEyeY(), this.getZ());
        return this.getWorld().raycast(new RaycastContext(Vector3d, target, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, this)).getType() != HitResult.Type.MISS;
    }

    private List<BlockPos> getNearbyBeacons(BlockPos blockpos, ServerWorld world, int range) {
        var pointofinterestmanager = world.getPointOfInterestStorage();
        var stream = pointofinterestmanager.getPositions((poiTypeHolder -> poiTypeHolder.matchesKey(AMPointOfInterestRegistry.BEACON.getKey())), Predicates.alwaysTrue(), blockpos, range, PointOfInterestStorage.OccupationStatus.ANY);
        return stream.collect(Collectors.toList());
    }

    private boolean isValidBeacon(BlockPos pos) {
        var te = getWorld().getBlockEntity(pos);
        return te instanceof BeaconBlockEntity && !((BeaconBlockEntity) te).getBeamSegments().isEmpty();
    }

    @Override
    public boolean isInAir() {
        return true;
    }

    public float getScorchProgress(float partialTick){
        return (prevScorchProgress + (scorchProgress - prevScorchProgress) * partialTick) / 20F;
    }

    static class MoveHelperController extends MoveControl {
        private final EntitySunbird parentEntity;

        public MoveHelperController(EntitySunbird sunbird) {
            super(sunbird);
            this.parentEntity = sunbird;
        }

        @Override
        public void tick() {
            if (this.state == MoveControl.State.MOVE_TO) {
                Vec3d vector3d = new Vec3d(this.targetX - parentEntity.getX(), this.targetY - parentEntity.getY(), this.targetZ - parentEntity.getZ());
                final double d0 = vector3d.length();
                if (d0 < parentEntity.getBoundingBox().getAverageSideLength()) {
                    this.state = MoveControl.State.WAIT;
                    parentEntity.setVelocity(parentEntity.getVelocity().multiply(0.5D));
                } else {
                    parentEntity.setVelocity(parentEntity.getVelocity().add(vector3d.multiply(this.speed * 0.05D / d0)));
                    if (parentEntity.getTarget() == null) {
                        Vec3d vector3d1 = parentEntity.getVelocity();
                        parentEntity.setYaw(-((float) MathHelper.atan2(vector3d1.x, vector3d1.z)) * MathHelper.DEGREES_PER_RADIAN);
                        parentEntity.bodyYaw = parentEntity.getYaw();
                    } else {
                        final double d2 = parentEntity.getTarget().getX() - parentEntity.getX();
                        final double d1 = parentEntity.getTarget().getZ() - parentEntity.getZ();
                        parentEntity.setYaw(-((float) MathHelper.atan2(d2, d1)) * MathHelper.DEGREES_PER_RADIAN);
                        parentEntity.bodyYaw = parentEntity.getYaw();
                    }
                }

            }
        }

        private boolean canReach(Vec3d p_220673_1_, int p_220673_2_) {
            var axisalignedbb = this.parentEntity.getBoundingBox();

            for (int i = 1; i < p_220673_2_; ++i) {
                axisalignedbb = axisalignedbb.offset(p_220673_1_);
                if (!this.parentEntity.getWorld().isSpaceEmpty(this.parentEntity, axisalignedbb)) {
                    return false;
                }
            }

            return true;
        }
    }

    static class RandomFlyGoal extends Goal {
        private final EntitySunbird parentEntity;
        private BlockPos target = null;

        public RandomFlyGoal(EntitySunbird sunbird) {
            this.parentEntity = sunbird;
            this.setControls(EnumSet.of(Goal.Control.MOVE));
        }

        @Override
        public boolean canStart() {
            MoveControl movementcontroller = this.parentEntity.getMoveControl();
            if (!movementcontroller.isMoving() || target == null) {
                if (parentEntity.beaconPos != null) {
                    target = getBlockInViewBeacon(parentEntity.beaconPos, 5 + parentEntity.random.nextInt(1));
                } else {
                    target = getBlockInViewSunbird();
                }
                if (target != null) {
                    this.parentEntity.getMoveControl().moveTo(target.getX() + 0.5D, target.getY() + 0.5D, target.getZ() + 0.5D, parentEntity.beaconPos != null ? 0.8D : 1.0D);
                }
                return true;
            }
            return false;
        }

        @Override
        public boolean shouldContinue() {
            return target != null && parentEntity.squaredDistanceTo(Vec3d.ofCenter(target)) > 2.4D && parentEntity.getMoveControl().isMoving() && !parentEntity.horizontalCollision;
        }

        @Override
        public void stop() {
            target = null;
        }

        @Override
        public void tick() {
            if (target == null) {
                if (parentEntity.beaconPos != null) {
                    target = getBlockInViewBeacon(parentEntity.beaconPos, 5 + parentEntity.random.nextInt(1));
                } else {
                    target = getBlockInViewSunbird();
                }
            }
            if(parentEntity.beaconPos != null && parentEntity.random.nextInt(100) == 0){
                parentEntity.orbitClockwise = parentEntity.random.nextBoolean();
            }
            if (target != null) {
                this.parentEntity.getMoveControl().moveTo(target.getX() + 0.5D, target.getY() + 0.5D, target.getZ() + 0.5D, parentEntity.beaconPos != null ? 0.8D : 1.0D);
                if (parentEntity.squaredDistanceTo(Vec3d.ofCenter(target)) < 2.5F) {
                    target = null;
                }
            }
        }

        private BlockPos getBlockInViewBeacon(BlockPos orbitPos, float gatheringCircleDist) {
            final float angle = (Maths.STARTING_ANGLE * (float) 9 * (parentEntity.orbitClockwise ? -parentEntity.age : parentEntity.age));
            final double extraX = gatheringCircleDist * MathHelper.sin((angle));
            final double extraZ = gatheringCircleDist * MathHelper.cos(angle);
            if (orbitPos != null) {
                BlockPos pos = AMBlockPos.fromCoords(orbitPos.getX() + extraX, orbitPos.getY() + parentEntity.random.nextInt(2) + 2, orbitPos.getZ() + extraZ);
                if (parentEntity.getWorld().isAir(new BlockPos(pos))) {
                    return pos;
                }
            }
            return null;
        }

        public BlockPos getBlockInViewSunbird() {
            final float radius = 0.75F * (0.7F * 6) * -3 - parentEntity.getRandom().nextInt(24);
            final float neg = parentEntity.getRandom().nextBoolean() ? 1 : -1;
            final float renderYawOffset = parentEntity.bodyYaw;
            final float angle = (Maths.STARTING_ANGLE * renderYawOffset) + 3.15F + (parentEntity.getRandom().nextFloat() * neg);
            final double extraX = radius * MathHelper.sin(MathHelper.PI + angle);
            final double extraZ = radius * MathHelper.cos(angle);
            BlockPos radialPos = AMBlockPos.fromCoords(parentEntity.getX() + extraX, 0, parentEntity.getZ() + extraZ);
            BlockPos ground = parentEntity.getWorld().getTopPosition(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, radialPos);
            final int distFromGround = (int) parentEntity.getY() - ground.getY();
            final int flightHeight = Math.max(ground.getY(), 230 + parentEntity.getRandom().nextInt(40)) - ground.getY();
            BlockPos newPos = radialPos.up(distFromGround > 16 ? flightHeight : (int) parentEntity.getY() + parentEntity.getRandom().nextInt(16) + 1);
            if (!parentEntity.isTargetBlocked(Vec3d.ofCenter(newPos)) && parentEntity.squaredDistanceTo(Vec3d.ofCenter(newPos)) > 6) {
                return newPos;
            }
            return null;
        }

    }
}
