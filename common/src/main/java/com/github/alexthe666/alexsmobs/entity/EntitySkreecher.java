package com.github.alexthe666.alexsmobs.entity;

import com.github.alexthe666.alexsmobs.config.AMConfig;
import com.github.alexthe666.alexsmobs.entity.ai.EntityAINearestTarget3D;
import com.github.alexthe666.alexsmobs.misc.AMBlockPos;
import com.github.alexthe666.alexsmobs.registry.AMEntityRegistry;
import com.github.alexthe666.alexsmobs.registry.AMParticleRegistry;
import com.github.alexthe666.alexsmobs.registry.AMSoundRegistry;
import com.github.alexthe666.alexsmobs.registry.AMTagRegistry;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.control.MoveControl;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.ai.pathing.BirdNavigation;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.ai.pathing.MobNavigation;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.WardenEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.function.BooleanBiFunction;
import net.minecraft.util.math.*;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.Difficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.event.GameEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;
import java.util.function.Predicate;

public class EntitySkreecher extends HostileEntity {

    public static final float MAX_DIST_TO_CEILING = 4f;
    private static final TrackedData<Boolean> CLINGING = DataTracker.registerData(EntitySkreecher.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> JUMPING_UP = DataTracker.registerData(EntitySkreecher.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> CLAPPING = DataTracker.registerData(EntitySkreecher.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Float> DIST_TO_CEILING = DataTracker.registerData(EntitySkreecher.class, TrackedDataHandlerRegistry.FLOAT);
    protected static final EntityDimensions GROUND_SIZE = EntityDimensions.changing(0.99F, 1.35F);
    public float prevClingProgress;
    public float clingProgress;
    public float prevClapProgress;
    public float clapProgress;
    public float prevDistanceToCeiling;
    private int clapTick = 0;
    private int clingCooldown = 0;
    private boolean isUpsideDownNavigator;
    private boolean hasAttemptedWardenSpawning;
    private boolean hasGroundSize = false;

    public EntitySkreecher(EntityType<? extends HostileEntity> type, World level) {
        super(type, level);
        switchNavigator(false);
    }

    @Override
    protected void initGoals() {
        super.initGoals();
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(3, new FleeEntityGoal<>(this, WardenEntity.class, 6.0F, 1.0D, 1.2D));
        this.goalSelector.add(2, new FollowTargetGoal());
        this.goalSelector.add(3, new WanderUpsideDownGoal());
        this.goalSelector.add(6, new LookAroundGoal(this));
        this.goalSelector.add(7, new LookAtEntityGoal(this, LivingEntity.class, 30F));
        this.targetSelector.add(1, new RevengeGoal(this));
        this.targetSelector.add(2, new EntityAINearestTarget3D<>(this, PlayerEntity.class, true) {
            @Override
            protected @NotNull Box getSearchBox(double targetDistance) {
                var bb = this.mob.getBoundingBox().expand(16, 1F, 16);
                return new Box(bb.minX, -64, bb.minZ, bb.maxX, 320, bb.maxZ);
            }
        });
    }

    @Override
    public boolean canSpawn(WorldAccess worldIn, SpawnReason spawnReasonIn) {
        return AMEntityRegistry.rollSpawn(AMConfig.skreecherSpawnRolls, this.getRandom(), spawnReasonIn);
    }

    public static boolean checkSkreecherSpawnRules(EntityType<? extends HostileEntity> animal, ServerWorldAccess worldIn, SpawnReason reason, BlockPos pos, Random random) {
        boolean isOnSculk = worldIn.getBlockState(pos.down()).isOf(Blocks.SCULK);
        return worldIn.getDifficulty() != Difficulty.PEACEFUL && isSpawnDark(worldIn, pos, random) && isOnSculk;
    }

    @Override
    public int getLimitPerChunk() {
        return 1;
    }

    private void switchNavigator(boolean clinging) {
        if (clinging) {
            this.moveControl = new MoveController();
            this.navigation = createScreecherNavigation(getWorld());
            this.isUpsideDownNavigator = true;
        } else {
            this.moveControl = new MoveControl(this);
            this.navigation = new MobNavigation(this, getWorld());
            this.isUpsideDownNavigator = false;
        }
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createLivingAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 2D)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 1.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.2F)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 64F);
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(DIST_TO_CEILING, 0F);
        this.dataTracker.startTracking(CLINGING, false);
        this.dataTracker.startTracking(JUMPING_UP, false);
        this.dataTracker.startTracking(CLAPPING, false);
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return AMSoundRegistry.SKREECHER_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return AMSoundRegistry.SKREECHER_HURT.get();
    }

    @Override
    public boolean damage(DamageSource source, float value){
        this.setClinging(false);
        this.setClapping(false);
        clingCooldown = 200 + random.nextInt(200);
        return super.damage(source, value);
    }

    @Override
    public void tick() {
        super.tick();
        prevClapProgress = clapProgress;
        prevClingProgress = clingProgress;
        prevDistanceToCeiling = this.getDistanceToCeiling();
        boolean clingVisually = this.isClinging() || this.isJumpingUp() || this.jumping;
        if (clingVisually && clingProgress < 5F) {
            clingProgress++;
        }
        if (!clingVisually && clingProgress > 0F && this.getDistanceToCeiling() == 0) {
            clingProgress--;
        }

        final boolean clapping = this.isClapping();
        if (clapping) {
            if (clapProgress < 5F)
                clapProgress++;
        } else {
            if (clapProgress > 0F)
                clapProgress--;
        }

        if (!this.getWorld().isClient) {
            float technicalDistToCeiling = calculateDistanceToCeiling();
            float gap = Math.max(technicalDistToCeiling - this.getDistanceToCeiling(), 0F);
            if(this.isClinging()){
                this.setNoGravity(true);
                if (technicalDistToCeiling > MAX_DIST_TO_CEILING || !isAlive() || clingCooldown > 0 /*FIXME forge || this.isInFluidType()*/) {
                    this.setClinging(false);
                }
                float goal = Math.min(technicalDistToCeiling, MAX_DIST_TO_CEILING);
                if(this.getDistanceToCeiling() < goal){
                    this.setDistanceToCeiling(Math.min(goal, prevDistanceToCeiling + 0.15F));
                }
                if(this.getDistanceToCeiling() > goal){
                    this.setDistanceToCeiling(Math.max(goal, prevDistanceToCeiling - 0.15F));
                }
                if(this.getDistanceToCeiling() < 1F){
                    gap = -0.03F;
                }
                this.setVelocity(this.getVelocity().add(0, gap * 0.5F, 0));
            }else{
                this.setNoGravity(false);
                if (technicalDistToCeiling < MAX_DIST_TO_CEILING && clingCooldown <= 0) {
                    this.setClinging(true);
                }
                this.setDistanceToCeiling(Math.max(0, prevDistanceToCeiling - 0.5F));
                if(this.isOnGround() && clingCooldown <= 0 && !this.isJumpingUp() && this.isAlive() && random.nextFloat() < 0.0085F && technicalDistToCeiling > MAX_DIST_TO_CEILING && !this.getWorld().isSkyVisible(this.getBlockPos())){
                    this.setJumpingUp(true);
                }
            }
        }
        if(this.isJumpingUp()){
            if(this.isAlive() && !this.getWorld().isSkyVisible(this.getBlockPos()) && (!this.verticalCollision || this.isOnGround())){
                this.setDistanceToCeiling(1.5F);
                this.setVelocity(this.getVelocity().add(0, 0.2F, 0));
                for(int i = 0; i < 3; i++){
                    this.getWorld().addParticle(ParticleTypes.SCULK_CHARGE_POP, this.getParticleX(0.5F), this.getY() - 0.2F, this.getParticleZ(0.5F), 0, -0.2F, 0);
                }
            }else{
                this.setJumpingUp(false);
            }
        }
        if(clingCooldown > 0){
            clingCooldown--;
        }
        if(!this.isAlive() || clingCooldown > 0 && this.isClinging()){
            this.setVelocity(this.getVelocity().add(0, -0.25F, 0));
        }
        if (this.isClinging() && !this.isUpsideDownNavigator) {
            switchNavigator(true);
        }
        if (!this.isClinging() && this.isUpsideDownNavigator) {
            switchNavigator(false);
        }
        if(this.isClapping() && this.isAlive() && clingCooldown <= 0){
            float dir = this.isClinging() ? -0.5F : 0.1F;
            if(clapTick % 8 == 0){
                this.playSound(AMSoundRegistry.SKREECHER_CLAP.get(), this.getSoundVolume() * 3F, this.getSoundPitch());
                this.emitGameEvent(GameEvent.ENTITY_ROAR);
                angerAllNearbyWardens();
                this.getWorld().addParticle(AMParticleRegistry.SKULK_BOOM.get(), this.getX(), this.getEyeY(), this.getZ(), 0, dir, 0);
            }else if(clapTick % 15 == 0){
                this.playSound(AMSoundRegistry.SKREECHER_CALL.get(), this.getSoundVolume() * 4F, this.getSoundPitch());
            }
            if(clapTick >= 100){
                if(!hasAttemptedWardenSpawning && AMConfig.skreechersSummonWarden){
                    hasAttemptedWardenSpawning = true;
                    BlockPos spawnAt = this.getBlockPos().down();
                    while(spawnAt.getY() > -64 && !getWorld().getBlockState(spawnAt).isSideSolidFullSquare(getWorld(), spawnAt, Direction.UP)){
                        spawnAt = spawnAt.down();
                    }
                    var holder = getWorld().getBiome(spawnAt);
                    if(!this.getWorld().isClient && getNearbyWardens().isEmpty() && holder.isIn(AMTagRegistry.SKREECHERS_CAN_SPAWN_WARDENS)){
                        var warden = EntityType.WARDEN.create(this.getWorld());

                        warden.refreshPositionAndAngles(this.getX(), spawnAt.getY() + 1, this.getZ(), this.getYaw(), 0.0F);
                        warden.initialize((ServerWorld)getWorld(), getWorld().getLocalDifficulty(this.getBlockPos()), SpawnReason.TRIGGERED, null, null);
                        warden.updateAttackTarget(this);
                        warden.increaseAngerAt(this, 79, false);
                        this.getWorld().spawnEntity(warden);

                    }
                }
            }
            clapTick++;
            if(!this.getWorld().isClient){
                if(this.getTarget() != null && this.getTarget().isAlive() && this.canSee(this.getTarget()) && !this.getTarget().hasStatusEffect(StatusEffects.INVISIBILITY) && !this.hasStatusEffect(StatusEffects.BLINDNESS)) {
                    double horizDist = this.getTarget().getPos().subtract(this.getPos()).horizontalLength();
                    if (horizDist > 20) {
                        this.setClapping(false);
                    }
                }else{
                    this.setClapping(false);
                }
            }
        }
        if(!this.isClinging() && !hasGroundSize){
            calculateDimensions();
            hasGroundSize = true;
        }
        if(this.isClinging() && hasGroundSize){
            calculateDimensions();
            hasGroundSize = false;
        }
    }

    @Override
    public boolean occludeVibrationSignals() {
        return true;
    }

    public void angerAllNearbyWardens(){
        for (var warden : getNearbyWardens()) {
            if(warden.canSee(this)){
                warden.increaseAngerAt(this, 100, false);
            }
        }
    }

    private List<WardenEntity> getNearbyWardens(){
        var angerBox = new Box(this.getX() - 35, this.getY() + (isClinging() ? 5F : 25F), this.getZ() - 35F, this.getX() + 35F, -64, this.getZ() + 35F);
        return this.getWorld().getNonSpectatingEntities(WardenEntity.class, angerBox);
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound compound) {
        super.writeCustomDataToNbt(compound);
        compound.putBoolean("Clinging", this.isClinging());
        compound.putDouble("CeilDist", this.getDistanceToCeiling());
        compound.putBoolean("SummonedWarden", this.hasAttemptedWardenSpawning);
        compound.putInt("ClingCooldown", this.clingCooldown);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound compound) {
        super.readCustomDataFromNbt(compound);
        this.setClinging(compound.getBoolean("Clinging"));
        this.setDistanceToCeiling((float)compound.getDouble("CeilDist"));
        this.hasAttemptedWardenSpawning = compound.getBoolean("SummonedWarden");
        this.clingCooldown = compound.getInt("ClingCooldown");
    }

    public EntityDimensions getDimensions(EntityPose poseIn) {
        return isClinging() ? super.getDimensions(poseIn) : GROUND_SIZE.scaled(this.getScaleFactor());
    }

    public boolean isClinging() {
        return this.dataTracker.get(CLINGING);
    }

    public void setClinging(boolean upsideDown) {
        this.dataTracker.set(CLINGING, upsideDown);
    }

    public boolean isClapping() {
        return this.dataTracker.get(CLAPPING);
    }

    public void setClapping(boolean clapping) {
        this.dataTracker.set(CLAPPING, clapping);
        if(!clapping){
            clapTick = 0;
        }
    }

    public boolean isJumpingUp() {
        return this.dataTracker.get(JUMPING_UP);
    }

    public void setJumpingUp(boolean jumping) {
        this.dataTracker.set(JUMPING_UP, Boolean.valueOf(jumping));
    }

    protected BlockPos getPositionAbove(float height) {
        return AMBlockPos.fromCoords(this.getPos().x, this.getBoundingBox().maxY + height + 0.5000001D, this.getPos().z);
    }

    protected EntityNavigation createScreecherNavigation(World level) {
        var flyingpathnavigation = new BirdNavigation(this, level) {
            @Override
            public boolean isValidPosition(BlockPos pos) {
                int airAbove = 0;
                while(getWorld().getBlockState(pos).isAir() && airAbove < MAX_DIST_TO_CEILING + 2){
                    pos = pos.up();
                    airAbove++;
                }
                return airAbove < Math.min(MAX_DIST_TO_CEILING, random.nextInt((int)MAX_DIST_TO_CEILING));
            }
        };
        flyingpathnavigation.setCanPathThroughDoors(false);
        flyingpathnavigation.setCanSwim(false);
        return flyingpathnavigation;
    }

    private float calculateDistanceToCeiling(){
        BlockPos ceiling = this.getCeilingOf(this.getBlockPos());
        return (float) (ceiling.getY() - this.getBoundingBox().maxY);
    }

    private boolean isOpaqueBlockAt(double x, double y, double z) {
        if (this.noClip) {
            return false;
        } else {
            final double d = 0.3F;
            final Vec3d vec3 = new Vec3d(x, y, z);
            final var axisAlignedBB = Box.of(vec3, d, 1.0E-6D, d);
            return this.getWorld().getStatesInBox(axisAlignedBB).filter(Predicate.not(AbstractBlock.AbstractBlockState::isAir)).anyMatch((p_185969_) -> {
                BlockPos blockpos = AMBlockPos.fromVec3(vec3);
                return p_185969_.shouldSuffocate(this.getWorld(), blockpos) && VoxelShapes.matchesAnywhere(p_185969_.getCollisionShape(this.getWorld(), blockpos).offset(vec3.x, vec3.y, vec3.z), VoxelShapes.cuboid(axisAlignedBB), BooleanBiFunction.AND);
            });
        }
    }

    @Override
    public boolean handleFallDamage(float distance, float damageMultiplier, DamageSource source) {
        return false;
    }

    @Override
    protected void fall(double y, boolean onGroundIn, BlockState state, BlockPos pos) {
    }

    public float getDistanceToCeiling() {
        return this.dataTracker.get(DIST_TO_CEILING);
    }

    public void setDistanceToCeiling(float dist) {
        this.dataTracker.set(DIST_TO_CEILING, dist);
    }

    @Override
    public void travel(Vec3d travelVector) {
        if (this.canMoveVoluntarily() && this.isClinging() /*FIXME forge && !this.isInFluidType()*/) {
            this.updateVelocity(this.getMovementSpeed(), travelVector);
            this.move(MovementType.SELF, this.getVelocity());
            this.setVelocity(this.getVelocity().multiply(0.75D));
        } else {
            super.travel(travelVector);
        }

    }

    public BlockPos getCeilingOf(BlockPos usPos){
        while (!getWorld().getBlockState(usPos).isSideSolidFullSquare(getWorld(), usPos, Direction.DOWN) && usPos.getY() < getWorld().getTopY()){
            usPos = usPos.up();
        }
        return usPos;
    }

    class WanderUpsideDownGoal extends WanderAroundGoal {

        private int stillTicks = 0;

        public WanderUpsideDownGoal() {
            super(EntitySkreecher.this, 1D, 25);
        }

        @Nullable
        @Override
        protected Vec3d getWanderTarget() {
            if (EntitySkreecher.this.isClinging()) {
                int distance = 16;
                for (int i = 0; i < 15; i++) {
                    var rand = Random.create();
                    BlockPos randPos = EntitySkreecher.this.getBlockPos().add(rand.nextInt(distance * 2) - distance, (int) -MAX_DIST_TO_CEILING, rand.nextInt(distance * 2) - distance);
                    BlockPos lowestPos = EntitySkreecher.this.getCeilingOf(randPos).down(rand.nextInt((int)MAX_DIST_TO_CEILING));
                    return Vec3d.ofCenter(lowestPos);
                }
                return null;
            } else {
                return super.getWanderTarget();
            }
        }

        @Override
        public boolean canStart() {
            return super.canStart();
        }

        @Override
        public boolean shouldContinue() {
            return super.shouldContinue();
        }

        @Override
        public void stop() {
            super.stop();
            this.targetX = 0;
            this.targetY = 0;
            this.targetZ = 0;
        }

        @Override
        public void start() {
            this.stillTicks = 0;
            this.mob.getNavigation().startMovingTo(this.targetX, this.targetY, this.targetZ, this.speed);
        }
    }

    class MoveController extends MoveControl {
        private final MobEntity parentEntity;

        public MoveController() {
            super(EntitySkreecher.this);
            this.parentEntity = EntitySkreecher.this;
        }

        public void tick() {
            if (this.state == MoveControl.State.MOVE_TO) {
                Vec3d vector3d = new Vec3d(this.targetX - parentEntity.getX(), this.targetY - parentEntity.getY(), this.targetZ - parentEntity.getZ());
                double d0 = vector3d.length();
                double width = parentEntity.getBoundingBox().getAverageSideLength();
                Vec3d vector3d1 = vector3d.multiply(this.speed * 0.035D / d0);
                float verticalSpeed = 0.15F;
                parentEntity.setVelocity(parentEntity.getVelocity().add(vector3d1.multiply(1F, verticalSpeed, 1F)));
                if(parentEntity.getTarget() != null){
                    double d1 = parentEntity.getTarget().getZ() - parentEntity.getZ();
                    double d3 = parentEntity.getTarget().getY() - parentEntity.getY();
                    double d2 = parentEntity.getTarget().getX() - parentEntity.getX();
                    float f = MathHelper.sqrt((float)(d2 * d2 + d1 * d1));
                    parentEntity.setYaw(-((float) MathHelper.atan2(d2, d1)) * MathHelper.DEGREES_PER_RADIAN);
                    parentEntity.setPitch((float) (MathHelper.atan2(d3, f) * (double) MathHelper.DEGREES_PER_RADIAN));
                    parentEntity.bodyYaw = parentEntity.getYaw();
                }else if (d0 >= width) {
                    parentEntity.setYaw(-((float) MathHelper.atan2(vector3d1.x, vector3d1.z)) * MathHelper.DEGREES_PER_RADIAN);
                }
            }
        }
    }

    private class FollowTargetGoal extends Goal {

        public FollowTargetGoal() {
            this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
        }

        @Override
        public boolean canStart() {
            return EntitySkreecher.this.getTarget() != null && EntitySkreecher.this.getTarget().isAlive() && EntitySkreecher.this.clingCooldown <= 0;
        }

        @Override
        public void start(){
            EntitySkreecher.this.playSound(AMSoundRegistry.SKREECHER_DETECT.get(), EntitySkreecher.this.getSoundVolume() * 6F, EntitySkreecher.this.getSoundPitch());
        }

        @Override
        public void tick(){
            LivingEntity target = EntitySkreecher.this.getTarget();
            if(target != null){
                if(EntitySkreecher.this.isClinging()){
                    BlockPos ceilAbove = EntitySkreecher.this.getCeilingOf(target.getBlockPos().up());
                    EntitySkreecher.this.getNavigation().startMovingTo(target.getX(), ceilAbove.getY() - random.nextFloat() * MAX_DIST_TO_CEILING, target.getZ(), 1.2F);
                }else{
                    EntitySkreecher.this.getNavigation().startMovingTo(target.getX(), target.getY(), target.getZ(), 1F);
                }
                Vec3d vec = target.getPos().subtract(EntitySkreecher.this.getPos());
                EntitySkreecher.this.getLookControl().lookAt(target, 360.0F, 180.0F);
                if(vec.horizontalLength() < 2.5F && EntitySkreecher.this.clingCooldown == 0){
                    EntitySkreecher.this.setClapping(true);
                }
            }
        }
    }
}
