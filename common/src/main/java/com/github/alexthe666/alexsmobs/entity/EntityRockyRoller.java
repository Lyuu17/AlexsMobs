package com.github.alexthe666.alexsmobs.entity;

import com.github.alexthe666.alexsmobs.config.AMConfig;
import com.github.alexthe666.alexsmobs.entity.ai.AdvancedPathNavigateNoTeleport;
import com.github.alexthe666.alexsmobs.entity.ai.MovementControllerCustomCollisions;
import com.github.alexthe666.alexsmobs.entity.util.Maths;
import com.github.alexthe666.alexsmobs.registry.AMEffectRegistry;
import com.github.alexthe666.alexsmobs.registry.AMEntityRegistry;
import com.github.alexthe666.alexsmobs.registry.AMSoundRegistry;
import com.github.alexthe666.alexsmobs.registry.AMTagRegistry;
import com.iafenvoy.uranus.object.entity.collision.ICustomCollisions;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LandingBlock;
import net.minecraft.block.PointedDripstoneBlock;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.ai.pathing.LandPathNodeMaker;
import net.minecraft.entity.ai.pathing.PathNodeNavigator;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.*;
import net.minecraft.world.event.GameEvent;

import java.util.EnumSet;
import java.util.function.Supplier;

public class EntityRockyRoller extends HostileEntity implements IAdjustCollision, ICustomCollisions {

    private static final TrackedData<Boolean> ROLLING = DataTracker.registerData(EntityRockyRoller.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> ANGRY = DataTracker.registerData(EntityRockyRoller.class, TrackedDataHandlerRegistry.BOOLEAN);
    public float rollProgress;
    public float prevRollProgress;
    public int rollCounter = 0;
    public float clientRoll = 0;
    private int maxRollTime = 50;
    private Vec3d rollDelta;
    private float rollYRot;
    private int rollCooldown = 0;
    private int earthquakeCooldown = 0;

    public EntityRockyRoller(EntityType<? extends HostileEntity> monster, World level) {
        super(monster, level);
        this.experiencePoints = 8;
        this.moveControl = new MovementControllerCustomCollisions(this);
    }

    @Override
    public boolean canSpawn(WorldAccess worldIn, SpawnReason spawnReasonIn) {
        return AMEntityRegistry.rollSpawn(AMConfig.rockyRollerSpawnRolls, this.getRandom(), spawnReasonIn);
    }

    public static boolean checkRockyRollerSpawnRules(EntityType<? extends HostileEntity> animal, ServerWorldAccess worldIn, SpawnReason reason, BlockPos pos, Random random) {
        return worldIn.getDifficulty() != Difficulty.PEACEFUL && isSpawnDark(worldIn, pos, random) && (worldIn.getBlockState(pos.down()).isIn(AMTagRegistry.ROCKY_ROLLER_SPAWNS) || worldIn.getBlockState(pos.down()).isSolid());
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createLivingAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 10.0D)
                .add(EntityAttributes.GENERIC_ARMOR, 20.0D)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 20.0D)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.7F)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 2.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.25F);
    }

    @Override
    protected EntityNavigation createNavigation(World worldIn) {
        return new Navigator(this, worldIn);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new AIMelee());
        this.goalSelector.add(2, new AIRollIdle(this));
        this.goalSelector.add(3, new WanderAroundFarGoal(this, 0.8D));
        this.goalSelector.add(4, new LookAtEntityGoal(this, PlayerEntity.class, 8.0F));
        this.goalSelector.add(5, new LookAroundGoal(this));
        this.targetSelector.add(1, new ActiveTargetGoal<>(this, MerchantEntity.class, false, true));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, false, true));
        this.targetSelector.add(3, new RevengeGoal(this));
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(ANGRY, false);
        this.dataTracker.startTracking(ROLLING, false);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return AMSoundRegistry.ROCKY_ROLLER_IDLE.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return AMSoundRegistry.ROCKY_ROLLER_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return AMSoundRegistry.ROCKY_ROLLER_HURT.get();
    }

    @Override
    public void tick() {
        super.tick();
        prevRollProgress = rollProgress;

        if (isRolling()) {
            if (rollProgress < 5F)
                rollProgress++;
        } else {
            if (rollProgress > 0F)
                rollProgress--;
        }

        if (!this.getWorld().isClient) {
            this.setAngry(this.getTarget() != null && this.getTarget().isAlive() && this.squaredDistanceTo(this.getTarget()) < 20 * 20);
        }
        if (this.isRolling() && rollCooldown <= 0) {
            this.handleRoll();
            if (this.isAngry() && this.isAlive()) {
                for (var entity : this.getWorld().getNonSpectatingEntities(LivingEntity.class, this.getBoundingBox().expand(0.3F))) {
                    if (!isTeammate(entity) && entity != this) {
                        entity.damage(this.getDamageSources().mobAttack(this), (isTarget(entity) ? 5.0F : 2.0F) + random.nextFloat() * 2.0F);
                        launch(entity, isTarget(entity));
                        if (isTarget(entity)) {
                            maxRollTime = rollCounter + 10;
                        }
                    }
                }
            }
            if (this.rollCounter > 2 && !this.isMoving() || !this.isAlive()) {
                this.setRolling(false);
            }
            this.setStepHeight(1F);
        } else {
            this.setStepHeight(0.66F);
            this.rollCounter = 0;
        }
        if (rollCooldown > 0) {
            rollCooldown--;
        }
        if (earthquakeCooldown > 0) {
            earthquakeCooldown--;
        }
    }

    private boolean isMoving() {
        return this.getVelocity().lengthSquared() > 0.02D;
    }

    private void earthquake() {
        boolean flag = false;
        var list = this.getWorld().getNonSpectatingEntities(LivingEntity.class, this.getBoundingBox().expand(6, 8, 6));
        for (var e : list) {
            if (!(e instanceof EntityRockyRoller) && e.isAlive()) {
                e.addStatusEffect(new StatusEffectInstance(AMEffectRegistry.EARTHQUAKE.get(), 20, 0, false, false, true));
                flag = true;
            }
        }
        if (!this.getWorld().isSkyVisible(this.getBlockPos()) && this.getWorld().getGameRules().getBoolean(GameRules.DO_MOB_GRIEFING)) {
            var ceil = this.getBlockPos().add(0, 2, 0);
            while ((!getWorld().getBlockState(ceil).isSolid() || getWorld().getBlockState(ceil).getBlock() == Blocks.POINTED_DRIPSTONE) && ceil.getY() < getWorld().getTopY()) {
                ceil = ceil.up();
            }
            final int i = 2 + random.nextInt(2);
            final int j = 2 + random.nextInt(2);
            final int k = 2 + random.nextInt(2);
            final float f = (float) (i + j + k) * 0.333F + 0.5F;
            final double fTimesF = f * f;

            for (var blockpos1 : BlockPos.iterate(ceil.add(-i, -j, -k), ceil.add(i, j, k))) {
                if (blockpos1.getSquaredDistance(ceil) <= fTimesF && getWorld().getBlockState(blockpos1).getBlock() instanceof LandingBlock) {
                    if (isHangingDripstone(blockpos1)) {
                        while (isHangingDripstone(blockpos1.up()) && blockpos1.getY() < getWorld().getTopY()) {
                            blockpos1 = blockpos1.up();
                        }
                        if (isHangingDripstone(blockpos1)) {
                            Vec3d vec3 = Vec3d.ofBottomCenter(blockpos1);
                            var fallingblockentity = FallingBlockEntity.spawnFromBlock(getWorld(), new BlockPos((int) vec3.x, (int) vec3.y, (int) vec3.z), getWorld().getBlockState(blockpos1));
                            this.getWorld().breakBlock(blockpos1, false);
                            this.getWorld().spawnEntity(fallingblockentity);
                        }
                    } else {
                        this.getWorld().scheduleBlockTick(blockpos1, getWorld().getBlockState(blockpos1).getBlock(), 2);
                    }
                    flag = true;
                }
            }
        }
        if(flag){
            this.emitGameEvent(GameEvent.ENTITY_ROAR);
            this.playSound(AMSoundRegistry.ROCKY_ROLLER_EARTHQUAKE.get(), this.getSoundVolume(), this.getSoundPitch());
        }
    }

    private boolean isHangingDripstone(BlockPos pos) {
        return getWorld().getBlockState(pos).getBlock() instanceof PointedDripstoneBlock && getWorld().getBlockState(pos).get(PointedDripstoneBlock.VERTICAL_DIRECTION) == Direction.DOWN;
    }

    private boolean isTarget(Entity entity) {
        return this.getTarget() != null && this.getTarget().isPartOf(entity);
    }

    public boolean isRolling() {
        return this.dataTracker.get(ROLLING);
    }

    public void setRolling(boolean rolling) {
        this.dataTracker.set(ROLLING, rolling);
    }

    public boolean isAngry() {
        return this.dataTracker.get(ANGRY);
    }

    public void setAngry(boolean angry) {
        this.dataTracker.set(ANGRY, angry);
    }

    private void handleRoll() {
        ++this.rollCounter;
        if (!this.getWorld().isClient) {
            if (this.horizontalCollision && earthquakeCooldown == 0 & this.isAngry()) {
                earthquakeCooldown = maxRollTime;
                this.earthquake();
            }
            if (this.rollCounter > maxRollTime) {
                this.setRolling(false);
                this.rollCooldown = 10 + random.nextInt(10);
                this.rollCounter = 0;
                this.setVelocity(Vec3d.ZERO);
            } else {
                var vec3 = this.getVelocity();
                if (this.rollCounter == 1) {
                    float f = this.getYaw() * MathHelper.RADIANS_PER_DEGREE;
                    float f1 = this.isBaby() ? 0.2F : 0.35F;
                    this.rollYRot = this.getYaw();
                    this.rollDelta = new Vec3d(vec3.x + (double) (-MathHelper.sin(f) * f1), 0.0D, vec3.z + (double) (MathHelper.cos(f) * f1));
                    this.setVelocity(this.rollDelta.add(0.0D, 0.27D, 0.0D));
                } else {
                    this.setYaw(rollYRot);
                    this.setHeadYaw(rollYRot);
                    this.setBodyYaw(rollYRot);
                    this.setVelocity(this.rollDelta.x, vec3.y, this.rollDelta.z);
                }
            }
        }
    }

    private void rollFor(int time) {
        if (this.rollCooldown == 0) {
            this.maxRollTime = time;
            earthquakeCooldown = 0;
            this.setRolling(true);
        }
    }

    private void launch(Entity e, boolean huge) {
        if (e.isOnGround()) {
            final double d0 = e.getX() - this.getX();
            final double d1 = e.getZ() - this.getZ();
            final double d2 = Math.max(d0 * d0 + d1 * d1, 0.001D);
            final float f = huge ? 1.0F : 0.35F;
            e.addVelocity(d0 / d2 * f, huge ? 0.5D : 0.2F, d1 / d2 * f);
        }
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return source.getName().equals("fallingStalactite") || super.isInvulnerableTo(source);
    }

    @Override
    public int getSafeFallDistance() {
        return super.getSafeFallDistance() * 2;
    }

    @Override
    public boolean handleFallDamage(float distance, float damageMultiplier, DamageSource source) {
        return false;
    }

    @Override
    protected void fall(double y, boolean onGroundIn, BlockState state, BlockPos pos) {
    }

    @Override
    public boolean isCollidable() {
        return this.isAlive();
    }

    @Override
    public void pushAwayFrom(Entity entity) {
        entity.setVelocity(entity.getVelocity().add(this.getVelocity()));
    }

    @Override
    public boolean canPassThrough(BlockPos blockPos, BlockState blockstate, VoxelShape voxelShape) {
        return blockstate.getBlock() instanceof PointedDripstoneBlock;
    }

    @Override
    public boolean collidesWithStateAtPos(BlockPos pos, BlockState blockstate) {
        return !(blockstate.getBlock() instanceof PointedDripstoneBlock) && super.collidesWithStateAtPos(pos, blockstate);
    }

    @Override
    public Vec3d adjustMovementForCollisions(Vec3d vec3, Supplier<Vec3d> supplier) {
        return ICustomCollisions.getAllowedMovementForEntity(this, vec3);
    }

    @Override
    public boolean damage(DamageSource dmg, float amount) {
        if (!this.isMoving() && !dmg.isOf(DamageTypes.MAGIC) && dmg.getSource() instanceof LivingEntity livingentity && !(livingentity instanceof EntityRockyRoller)) {
            if (!dmg.isOf(DamageTypes.EXPLOSION) && !livingentity.velocityModified) {
                livingentity.damage(getDamageSources().thorns(this), 2.0F);
            }
        }
        return super.damage(dmg, amount);
    }

    static class RockyRollerNodeEvaluator extends LandPathNodeMaker {
        @Override
        protected PathNodeType adjustNodeType(BlockView level, BlockPos pos, PathNodeType typeIn) {
            return level.getBlockState(pos).getBlock() instanceof PointedDripstoneBlock ? PathNodeType.OPEN : super.adjustNodeType(level, pos, typeIn);
        }
    }

    class AIRollIdle extends Goal {
        EntityRockyRoller rockyRoller;

        public AIRollIdle(EntityRockyRoller p_29328_) {
            this.rockyRoller = p_29328_;
            this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK, Goal.Control.JUMP));
        }

        @Override
        public boolean canStart() {
            if (this.rockyRoller.isOnGround()) {
                if (rockyRoller.isRolling() || rockyRoller.rollCooldown > 0 || rockyRoller.getTarget() != null && rockyRoller.getTarget().isAlive()) {
                    return false;
                } else {
                    float f = rockyRoller.getYaw() * MathHelper.RADIANS_PER_DEGREE;
                    int i = 0;
                    int j = 0;
                    float f1 = -MathHelper.sin(f);
                    float f2 = MathHelper.cos(f);
                    if ((double) Math.abs(f1) > 0.5D) {
                        i = (int) ((float) i + f1 / Math.abs(f1));
                    }

                    if ((double) Math.abs(f2) > 0.5D) {
                        j = (int) ((float) j + f2 / Math.abs(f2));
                    }

                    return rockyRoller.getWorld().getBlockState(rockyRoller.getBlockPos().add(i, -1, j)).isAir();
                }
            }
            return false;
        }

        @Override
        public boolean shouldContinue() {
            return false;
        }

        @Override
        public void start() {
            this.rockyRoller.rollFor(30 + random.nextInt(30));
        }

        @Override
        public boolean canStop() {
            return false;
        }
    }

    private class AIMelee extends Goal {

        private BlockPos rollFromPos = null;
        private int rollTimeout = 0;

        public AIMelee() {
            this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
        }

        @Override
        public boolean canStart() {
            return EntityRockyRoller.this.getTarget() != null && EntityRockyRoller.this.getTarget().isAlive() && !EntityRockyRoller.this.isRolling();
        }

        @Override
        public void tick() {
            LivingEntity enemy = EntityRockyRoller.this.getTarget();
            double d0 = this.validRollDistance(enemy);
            double distToEnemySqr = EntityRockyRoller.this.distanceTo(enemy);
            if (rollFromPos == null || enemy.squaredDistanceTo(rollFromPos.getX() + 0.5F, rollFromPos.getY() + 0.5F, rollFromPos.getZ() + 0.5) > 60 || !canEntitySeePosition(enemy, rollFromPos)) {
                rollFromPos = getRollAtPosition(enemy);
            }
            EntityRockyRoller.this.lookAtEntity(enemy, 100, 5);

            if (rollTimeout < 40 && rollFromPos != null && (distToEnemySqr <= d0 && EntityRockyRoller.this.squaredDistanceTo(rollFromPos.getX() + 0.5F, rollFromPos.getY() + 0.5F, rollFromPos.getZ() + 0.5) > 2.25F)) {
                EntityRockyRoller.this.getNavigation().startMovingTo(rollFromPos.getX() + 0.5F, rollFromPos.getY() + 0.5F, rollFromPos.getZ() + 0.5F, 1.6D);
                rollTimeout++;
            } else {
                double d1 = enemy.getX() - EntityRockyRoller.this.getX();
                double d2 = enemy.getZ() - EntityRockyRoller.this.getZ();
                float f = (float) (MathHelper.atan2(d2, d1) * (double) MathHelper.DEGREES_PER_RADIAN) - 90.0F;
                EntityRockyRoller.this.setYaw(f);
                EntityRockyRoller.this.bodyYaw = f;
                EntityRockyRoller.this.rollFor(30 + random.nextInt(40));
            }
        }

        @Override
        public void stop() {
            super.stop();
            rollTimeout = 0;
        }

        protected double validRollDistance(LivingEntity attackTarget) {
            return 3.0F + attackTarget.getWidth();
        }

        private boolean canEntitySeePosition(LivingEntity entity, BlockPos destinationBlock) {
            var Vector3d = new Vec3d(entity.getX(), entity.getY() + 0.5F, entity.getZ());
            var blockVec = Vec3d.ofCenter(destinationBlock);
            var result = entity.getWorld().raycast(new RaycastContext(Vector3d, blockVec, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, entity));
            return result != null && (result.getBlockPos().equals(destinationBlock) || entity.getWorld().getBlockState(result.getBlockPos()).getBlock() == Blocks.POINTED_DRIPSTONE);
        }

        public BlockPos getRollAtPosition(Entity target) {
            float radius = EntityRockyRoller.this.getRandom().nextInt(2) + 6 + target.getWidth();
            int orbit = EntityRockyRoller.this.getRandom().nextInt(360);
            float angle = (Maths.STARTING_ANGLE * orbit);
            double extraX = radius * MathHelper.sin(MathHelper.PI + angle);
            double extraZ = radius * MathHelper.cos(angle);
            BlockPos circlePos = new BlockPos((int) (target.getX() + extraX), (int) target.getEyeY(), (int) (target.getZ() + extraZ));
            while (!EntityRockyRoller.this.getWorld().getBlockState(circlePos).isAir() && circlePos.getY() < EntityRockyRoller.this.getWorld().getTopY()) {
                circlePos = circlePos.up();
            }
            while (!EntityRockyRoller.this.getWorld().getBlockState(circlePos.down()).hasSolidTopSurface(EntityRockyRoller.this.getWorld(), circlePos.down(), EntityRockyRoller.this) && circlePos.getY() > 1) {
                circlePos = circlePos.down();
            }
            if (EntityRockyRoller.this.getPathfindingFavor(circlePos) > -1) {
                return circlePos;
            }
            return null;
        }
    }

    static class Navigator extends AdvancedPathNavigateNoTeleport {

        public Navigator(MobEntity mob, World world) {
            super(mob, world, true);
        }

        @Override
        protected PathNodeNavigator createPathNodeNavigator(int i) {
            this.nodeMaker = new RockyRollerNodeEvaluator();
            return new PathNodeNavigator(this.nodeMaker, i);
        }
    }
}
