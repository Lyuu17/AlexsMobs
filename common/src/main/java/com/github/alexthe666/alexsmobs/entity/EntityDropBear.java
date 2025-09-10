package com.github.alexthe666.alexsmobs.entity;

import com.github.alexthe666.alexsmobs.config.AMConfig;
import com.github.alexthe666.alexsmobs.entity.ai.DirectPathNavigator;
import com.github.alexthe666.alexsmobs.entity.ai.EntityAINearestTarget3D;
import com.github.alexthe666.alexsmobs.entity.ai.FlightMoveController;
import com.github.alexthe666.alexsmobs.entity.ai.GroundPathNavigatorWide;
import com.github.alexthe666.alexsmobs.entity.util.Maths;
import com.github.alexthe666.alexsmobs.registry.AMEntityRegistry;
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
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.*;
import net.minecraft.world.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.Random;

public class EntityDropBear extends HostileEntity implements IAnimatedEntity {

    public static final Animation ANIMATION_BITE = Animation.create(9);
    public static final Animation ANIMATION_SWIPE_R = Animation.create(15);
    public static final Animation ANIMATION_SWIPE_L = Animation.create(15);
    public static final Animation ANIMATION_JUMPUP = Animation.create(20);
    private static final TrackedData<Boolean> UPSIDE_DOWN = DataTracker.registerData(EntityDropBear.class, TrackedDataHandlerRegistry.BOOLEAN);
    public float prevUpsideDownProgress;
    public float upsideDownProgress;
    public boolean fallRotation = random.nextBoolean();
    private int animationTick;
    private boolean jumpingUp = false;
    private Animation currentAnimation;
    private int upwardsFallingTicks = 0;
    private boolean isUpsideDownNavigator;

    public EntityDropBear(EntityType<EntityDropBear> type, World world) {
        super(type, world);
        switchNavigator(true);
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createLivingAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 22.0D)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 20.0D)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.7F)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 2.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.25F);
    }

    public static BlockPos getLowestPos(WorldAccess world, BlockPos pos) {
        while (!world.getBlockState(pos).isSideSolidFullSquare(world, pos, Direction.DOWN) && pos.getY() < 320) {
            pos = pos.up();
        }
        return pos;
    }

    @Override
    public boolean canSpawn(WorldAccess worldIn, SpawnReason spawnReasonIn) {
        return AMEntityRegistry.rollSpawn(AMConfig.dropbearSpawnRolls, this.getRandom(), spawnReasonIn);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return AMSoundRegistry.DROPBEAR_IDLE.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return AMSoundRegistry.DROPBEAR_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return AMSoundRegistry.DROPBEAR_HURT.get();
    }

    @Override
    public boolean tryAttack(Entity entityIn) {
        if (this.getAnimation() == NO_ANIMATION) {
            this.setAnimation(random.nextBoolean() ? ANIMATION_BITE : random.nextBoolean() ? ANIMATION_SWIPE_L : ANIMATION_SWIPE_R);
        }
        return true;
    }

    @Override
    protected void initGoals() {
        super.initGoals();
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new AIDropMelee());
        this.goalSelector.add(2, new AIUpsideDownWander());
        this.goalSelector.add(6, new LookAroundGoal(this));
        this.goalSelector.add(7, new LookAtEntityGoal(this, LivingEntity.class, 30F));
        this.targetSelector.add(1, new RevengeGoal(this, EntityDropBear.class));
        this.targetSelector.add(2, new EntityAINearestTarget3D<>(this, PlayerEntity.class, true) {
            @Override
            protected @NotNull Box getSearchBox(double targetDistance) {
                var bb = this.mob.getBoundingBox().expand(targetDistance, targetDistance, targetDistance);
                return new Box(bb.minX, 0, bb.minZ, bb.maxX, 256, bb.maxZ);
            }
        });
        this.targetSelector.add(2, new EntityAINearestTarget3D<>(this, MerchantEntity.class, true) {
            @Override
            protected @NotNull Box getSearchBox(double targetDistance) {
                var bb = this.mob.getBoundingBox().expand(targetDistance, targetDistance, targetDistance);
                return new Box(bb.minX, 0, bb.minZ, bb.maxX, 256, bb.maxZ);
            }
        });
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return super.isInvulnerableTo(source) || source.isIn(DamageTypeTags.IS_FALL) || source.isOf(DamageTypes.IN_WALL);
    }

    @Override
    protected void fall(double y, boolean onGroundIn, BlockState state, BlockPos pos) {
        super.fall(y, onGroundIn, state, pos);
    }

    @Override
    protected void playBlockFallSound() {
        this.onLand();
        super.playBlockFallSound();
    }

    private void switchNavigator(boolean rightsideUp) {
        if (rightsideUp) {
            this.moveControl = new MoveControl(this);
            this.navigation = new GroundPathNavigatorWide(this, getWorld());
            this.isUpsideDownNavigator = false;
        } else {
            this.moveControl = new FlightMoveController(this, 1.1F, false);
            this.navigation = new DirectPathNavigator(this, getWorld());
            this.isUpsideDownNavigator = true;
        }
    }

    @Override
    public void tick() {
        super.tick();
        AnimationHandler.INSTANCE.updateAnimations(this);
        prevUpsideDownProgress = upsideDownProgress;
        if (this.isUpsideDown() && upsideDownProgress < 5F) {
            upsideDownProgress++;
        }
        if (!this.isUpsideDown() && upsideDownProgress > 0F) {
            upsideDownProgress--;
        }
        if (!this.getWorld().isClient) {
            BlockPos abovePos = this.getPositionAbove();
            BlockState aboveState = getWorld().getBlockState(abovePos);
            BlockState belowState = getWorld().getBlockState(this.getVelocityAffectingPos());
            BlockPos worldHeight = getWorld().getTopPosition(Heightmap.Type.MOTION_BLOCKING, this.getBlockPos());
            boolean validAboveState = aboveState.isSideSolidFullSquare(getWorld(), abovePos, Direction.DOWN);
            boolean validBelowState = belowState.isSideSolidFullSquare(getWorld(), this.getVelocityAffectingPos(), Direction.UP);
            LivingEntity attackTarget = this.getTarget();
            if (attackTarget != null && distanceTo(attackTarget) < attackTarget.getWidth() + this.getWidth() + 1 && this.canSee(attackTarget)) {
                if (this.getAnimationTick() == 6) {
                    if (this.getAnimation() == ANIMATION_BITE) {
                        final float yRotRad = this.getYaw() * MathHelper.RADIANS_PER_DEGREE;
                        attackTarget.takeKnockback(0.5F, MathHelper.sin(yRotRad), -MathHelper.cos(yRotRad));
                        this.getTarget().damage(this.getDamageSources().mobAttack(this), (float) this.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE).getBaseValue());
                    }
                } else if (this.getAnimationTick() == 9) {
                    if (this.getAnimation() == ANIMATION_SWIPE_L) {
                        final float rot = getYaw() + 90;
                        final float rotRad = rot * MathHelper.RADIANS_PER_DEGREE;
                        attackTarget.takeKnockback(0.5F, MathHelper.sin(rotRad), -MathHelper.cos(rotRad));
                        this.getTarget().damage(this.getDamageSources().mobAttack(this), (float) this.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE).getBaseValue());
                    } else if (this.getAnimation() == ANIMATION_SWIPE_R) {
                        final float rot = getYaw() - 90;
                        final float rotRad = rot * MathHelper.RADIANS_PER_DEGREE;
                        attackTarget.takeKnockback(0.5F, MathHelper.sin(rotRad), -MathHelper.cos(rotRad));
                        this.getTarget().damage(this.getDamageSources().mobAttack(this), (float) this.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE).getBaseValue());
                    }
                }
            }
            if ((attackTarget == null || attackTarget != null && !attackTarget.isAlive()) && random.nextInt(300) == 0 && this.isOnGround() && !this.isUpsideDown() && this.getY() + 2 < worldHeight.getY()) {
                if (this.getAnimation() == NO_ANIMATION) {
                    this.setAnimation(ANIMATION_JUMPUP);
                }
            }
            if (jumpingUp && this.getY() > worldHeight.getY()) {
                jumpingUp = false;
            }
            if ((this.isOnGround() && this.getAnimation() == ANIMATION_JUMPUP && this.getAnimationTick() > 10 || jumpingUp && this.getAnimation() == NO_ANIMATION)) {
                this.setVelocity(this.getVelocity().add(0, 2F, 0));
                jumpingUp = true;
            }
            if (this.isUpsideDown()) {
                jumpingUp = false;
                this.setNoGravity(!this.isOnGround());
                final float f = 0.91F;
                this.setVelocity(this.getVelocity().multiply(f, 1F, f));
                if (!this.verticalCollision) {
                    if (this.isOnGround() || validBelowState || upwardsFallingTicks > 5) {
                        this.setUpsideDown(false);
                        upwardsFallingTicks = 0;
                    } else {
                        if (!validAboveState) {
                            upwardsFallingTicks++;
                        }
                        this.setVelocity(this.getVelocity().add(0, 0.2F, 0));
                    }
                } else {
                    upwardsFallingTicks = 0;
                }
                if (this.horizontalCollision) {
                    upwardsFallingTicks = 0;
                    this.setVelocity(this.getVelocity().add(0, -0.3F, 0));
                }
                if (this.isInsideWall() && getWorld().isAir(this.getVelocityAffectingPos())) {
                    this.setPos(this.getX(), this.getY() - 1, this.getZ());
                }
            } else {
                this.setNoGravity(false);
                if (validAboveState) {
                    this.setUpsideDown(true);
                }
            }

            if (this.isUpsideDown()) {
                if (!this.isUpsideDownNavigator)
                    switchNavigator(false);
            } else {
                if (this.isUpsideDownNavigator)
                    switchNavigator(true);
            }
        }
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(UPSIDE_DOWN, false);
    }

    public boolean isUpsideDown() {
        return this.dataTracker.get(UPSIDE_DOWN);
    }

    public void setUpsideDown(boolean upsideDown) {
        this.dataTracker.set(UPSIDE_DOWN, upsideDown);
    }

    protected BlockPos getPositionAbove() {
        return new BlockPos((int) this.getPos().x, (int) (this.getBoundingBox().maxY + 0.5000001D), (int) this.getPos().z);
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
        currentAnimation = animation;
    }

    @Override
    public Animation[] getAnimations() {
        return new Animation[]{ANIMATION_BITE, ANIMATION_SWIPE_L, ANIMATION_SWIPE_R, ANIMATION_JUMPUP};
    }

    private boolean hasLineOfSightBlock(BlockPos destinationBlock) {
        var Vector3d = new Vec3d(this.getX(), this.getEyeY(), this.getZ());
        var blockVec = Vec3d.ofCenter(destinationBlock);
        var result = this.getWorld().raycast(new RaycastContext(Vector3d, blockVec, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, this));
        return result.getBlockPos().equals(destinationBlock);
    }

    private void doInitialPosing(WorldAccess world) {
        BlockPos upperPos = this.getPositionAbove().up();
        BlockPos highest = getLowestPos(world, upperPos);
        this.setPos(highest.getX() + 0.5F, highest.getY(), highest.getZ() + 0.5F);
    }

    @Nullable
    @Override
    public EntityData initialize(ServerWorldAccess worldIn, LocalDifficulty difficultyIn, SpawnReason reason, @Nullable EntityData spawnDataIn, @Nullable NbtCompound dataTag) {
        if (reason == SpawnReason.NATURAL) {
            doInitialPosing(worldIn);
        }
        return super.initialize(worldIn, difficultyIn, reason, spawnDataIn, dataTag);
    }

    private void onLand() {
        if (!this.getWorld().isClient) {
            getWorld().sendEntityStatus(this, (byte) 39);
            for (var entity : this.getWorld().getNonSpectatingEntities(LivingEntity.class, this.getBoundingBox().expand(2.5D))) {
                if (!isTeammate(entity) && !(entity instanceof EntityDropBear) && entity != this) {
                    entity.damage(this.getDamageSources().mobAttack(this), 2.0F + random.nextFloat() * 5F);
                    launch(entity, true);
                }
            }
        }
    }

    private void launch(Entity e, boolean huge) {
        if (e.isOnGround()) {
            final double d0 = e.getX() - this.getX();
            final double d1 = e.getZ() - this.getZ();
            final double d2 = Math.max(d0 * d0 + d1 * d1, 0.001D);
            final float f = 0.5F;
            e.addVelocity(d0 / d2 * f, huge ? 0.5D : 0.2F, d1 / d2 * f);
        }
    }

    @Environment(EnvType.CLIENT)
    public void handleStatus(byte id) {
        if (id == 39) {
            spawnGroundEffects();
        } else {
            super.handleStatus(id);

        }
    }

    public void spawnGroundEffects() {
        float radius = 2.3F;
        if (this.getWorld().isClient) {
            for (int i1 = 0; i1 < 20 + random.nextInt(12); i1++) {
                double motionX = getRandom().nextGaussian() * 0.07D;
                double motionY = getRandom().nextGaussian() * 0.07D;
                double motionZ = getRandom().nextGaussian() * 0.07D;
                float angle = (Maths.STARTING_ANGLE * this.bodyYaw) + i1;
                double extraX = radius * MathHelper.sin(MathHelper.PI + angle);
                double extraY = 0.8F;
                double extraZ = radius * MathHelper.cos(angle);
                BlockPos ground = getGroundPosition(new BlockPos(MathHelper.floor(this.getX() + extraX), (int) this.getY(), MathHelper.floor(this.getZ() + extraZ)));
                BlockState state = this.getWorld().getBlockState(ground);
                if (!state.isAir()) {
                    getWorld().addParticle(new BlockStateParticleEffect(ParticleTypes.BLOCK, state), true, this.getX() + extraX, ground.getY() + extraY, this.getZ() + extraZ, motionX, motionY, motionZ);
                }
            }
        }
    }

    private BlockPos getGroundPosition(BlockPos in) {
        BlockPos position = new BlockPos(in.getX(), (int) this.getY(), in.getZ());
        while (position.getY() > 2 && getWorld().isAir(position) && getWorld().getFluidState(position).isEmpty()) {
            position = position.down();
        }
        return position;
    }

    class AIUpsideDownWander extends WanderAroundGoal {

        public AIUpsideDownWander() {
            super(EntityDropBear.this, 1D, 50);
        }

        @Nullable
        @Override
        protected Vec3d getWanderTarget() {
            if (EntityDropBear.this.isUpsideDown()) {
                for (int i = 0; i < 15; i++) {
                    Random rand = new Random();
                    BlockPos randPos = EntityDropBear.this.getBlockPos().add(rand.nextInt(16) - 8, -2, rand.nextInt(16) - 8);
                    BlockPos lowestPos = EntityDropBear.getLowestPos(getWorld(), randPos);
                    if (getWorld().getBlockState(lowestPos).isSideSolidFullSquare(getWorld(), lowestPos, Direction.DOWN)) {
                        return Vec3d.ofCenter(lowestPos);
                    }
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
            if (EntityDropBear.this.isUpsideDown()) {
                double d0 = EntityDropBear.this.getX() - this.targetX;
                double d2 = EntityDropBear.this.getZ() - this.targetZ;
                double d4 = d0 * d0 + d2 * d2;
                return d4 > 4;
            } else {
                return super.shouldContinue();
            }
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
            if (EntityDropBear.this.isUpsideDown()) {
                this.mob.getMoveControl().moveTo(this.targetX, this.targetY, this.targetZ, this.speed * 0.7F);
            } else {
                this.mob.getNavigation().startMovingTo(this.targetX, this.targetY, this.targetZ, this.speed);
            }
        }

    }

    private class AIDropMelee extends Goal {
        public AIDropMelee() {
            this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
        }

        @Override
        public boolean canStart() {
            return EntityDropBear.this.getTarget() != null;
        }

        @Override
        public void tick() {
            var target = EntityDropBear.this.getTarget();
            if (target != null) {
                double dist = EntityDropBear.this.distanceTo(target);
                if (EntityDropBear.this.isUpsideDown()) {
                    double d0 = EntityDropBear.this.getX() - target.getX();
                    double d2 = EntityDropBear.this.getZ() - target.getZ();
                    double xzDistSqr = d0 * d0 + d2 * d2;
                    BlockPos ceilingPos = new BlockPos((int) target.getX(), (int) (EntityDropBear.this.getY() - 3 - random.nextInt(3)), (int) target.getZ());
                    BlockPos lowestPos = EntityDropBear.getLowestPos(getWorld(), ceilingPos);
                    EntityDropBear.this.getMoveControl().moveTo(lowestPos.getX() + 0.5F, ceilingPos.getY(), lowestPos.getZ() + 0.5F, 1.1D);
                    if (xzDistSqr < 2.5F) {
                        EntityDropBear.this.setUpsideDown(false);
                    }
                } else {
                    if (EntityDropBear.this.isOnGround()) {
                        EntityDropBear.this.getNavigation().startMovingTo(target, 1.2D);
                    }
                }
                if (dist < 3D) {
                    EntityDropBear.this.tryAttack(target);
                }
            }
        }
    }
}
