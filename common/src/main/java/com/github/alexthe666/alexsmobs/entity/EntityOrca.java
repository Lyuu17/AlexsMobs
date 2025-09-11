package com.github.alexthe666.alexsmobs.entity;

import com.github.alexthe666.alexsmobs.config.AMConfig;
import com.github.alexthe666.alexsmobs.entity.ai.*;
import com.github.alexthe666.alexsmobs.registry.AMEffectRegistry;
import com.github.alexthe666.alexsmobs.registry.AMEntityRegistry;
import com.github.alexthe666.alexsmobs.registry.AMSoundRegistry;
import com.github.alexthe666.alexsmobs.registry.AMTagRegistry;
import com.iafenvoy.uranus.animation.Animation;
import com.iafenvoy.uranus.animation.AnimationHandler;
import com.iafenvoy.uranus.animation.IAnimatedEntity;
import net.minecraft.block.Blocks;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.ai.control.MoveControl;
import net.minecraft.entity.ai.control.YawAdjustingLookControl;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.mob.DrownedEntity;
import net.minecraft.entity.mob.GuardianEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.*;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.function.Predicate;

public class EntityOrca extends TameableEntity implements IAnimatedEntity {

    public static final Animation ANIMATION_BITE = Animation.create(8);
    public static final Animation ANIMATION_TAILSWING = Animation.create(20);
    private static final TrackedData<Integer> MOISTNESS = DataTracker.registerData(EntityOrca.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TargetPredicate PLAYER_PREDICATE = TargetPredicate.createNonAttackable().setBaseMaxDistance(24.0D).ignoreVisibility();
    private static final TrackedData<Integer> VARIANT = DataTracker.registerData(EntityOrca.class, TrackedDataHandlerRegistry.INTEGER);
    public int jumpCooldown;
    private int animationTick;
    private Animation currentAnimation;
    private int blockBreakCounter;
    public static final Predicate<LivingEntity> TARGET_BABY  = (animal) -> animal.isBaby();

    public EntityOrca(EntityType<? extends EntityOrca> type, World worldIn) {
        super(type, worldIn);
        this.setPathfindingPenalty(PathNodeType.WATER, 0.0F);
        this.moveControl = new MoveHelperController(this);
        this.lookControl = new YawAdjustingLookControl(this, 10);
    }

    @Override
    public boolean canImmediatelyDespawn(double distanceToClosestPlayer) {
        return !this.isTamed();
    }

    @Override
    public boolean canSpawn(WorldAccess worldIn, SpawnReason spawnReasonIn) {
        return AMEntityRegistry.rollSpawn(AMConfig.orcaSpawnRolls, this.getRandom(), spawnReasonIn);
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createLivingAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 60.0D)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 64.0D)
                .add(EntityAttributes.GENERIC_ARMOR, 0.0D)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 10.0D)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.7F)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 1.35F);
    }

    @Override
    protected EntityNavigation createNavigation(World worldIn) {
        return new SwimmerJumpPathNavigator(this, worldIn);
    }

    public int getMoistness() {
        return this.dataTracker.get(MOISTNESS);
    }

    public void setMoistness(int p_211137_1_) {
        this.dataTracker.set(MOISTNESS, p_211137_1_);
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(MOISTNESS, 2400);
        this.dataTracker.startTracking(VARIANT, 0);
    }

    public int getVariant() {
        return this.dataTracker.get(VARIANT);
    }

    public void setVariant(int variant) {
        this.dataTracker.set(VARIANT, Integer.valueOf(variant));
    }

    public int determineVariant(BlockPos coords){
        if(coords == null){
            return 0;
        }
        if(coords.getZ() < 0){//north
            //west
            return coords.getX() < 0 ? 1 : 0;
        }else{//south
            //west
            return coords.getX() < 0 ? 3 : 2;
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return AMSoundRegistry.ORCA_IDLE.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return AMSoundRegistry.ORCA_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return AMSoundRegistry.ORCA_DIE.get();
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new BreatheAirGoal(this));
        this.goalSelector.add(1, new MoveIntoWaterGoal(this));
        this.goalSelector.add(2, new EntityOrca.SwimWithPlayerGoal(this, 4.0D));
        this.goalSelector.add(4, new SwimAroundGoal(this, 1.0D, 10));
        this.goalSelector.add(4, new LookAroundGoal(this));
        this.goalSelector.add(5, new LookAtEntityGoal(this, PlayerEntity.class, 6.0F));
        this.goalSelector.add(5, new OrcaAIJump(this, 10));
        this.goalSelector.add(6, new OrcaAIMeleeJump(this));
        this.goalSelector.add(6, new OrcaAIMelee(this, 1.2F, true));
        this.goalSelector.add(8, new ChaseBoatGoal(this));
        this.targetSelector.add(1, (new RevengeGoal(this)).setGroupRevenge());
        this.targetSelector.add(2, new EntityAINearestTarget3D<>(this, EntityCachalotWhale.class, 25, false, false, TARGET_BABY));
        this.targetSelector.add(3, new EntityAINearestTarget3D<>(this, LivingEntity.class, 200, false, true, AMEntityRegistry.buildPredicateFromTag(AMTagRegistry.ORCA_TARGETS)));
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
        return new Animation[]{ANIMATION_BITE, ANIMATION_TAILSWING};
    }

    @Override
    public void travel(Vec3d travelVector) {
        if (this.canMoveVoluntarily() && this.isTouchingWater()) {
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
    public void mobTick() {
        super.mobTick();
        breakBlock();
    }

    public void breakBlock() {
        if (this.blockBreakCounter > 0) {
            --this.blockBreakCounter;
            return;
        }
        boolean flag = false;
        if (!this.getWorld().isClient && this.blockBreakCounter == 0) {
            for (int a = (int) Math.round(this.getBoundingBox().minX); a <= (int) Math.round(this.getBoundingBox().maxX); a++) {
                for (int b = (int) Math.round(this.getBoundingBox().minY) - 1; (b <= (int) Math.round(this.getBoundingBox().maxY) + 1) && (b <= 127); b++) {
                    for (int c = (int) Math.round(this.getBoundingBox().minZ); c <= (int) Math.round(this.getBoundingBox().maxZ); c++) {
                        var pos = new BlockPos(a, b, c);
                        var state = getWorld().getBlockState(pos);
                        var fluidState = getWorld().getFluidState(pos);
                        var block = state.getBlock();
                        if (!state.isAir() && !state.getOutlineShape(getWorld(), pos).isEmpty() && state.isIn(AMTagRegistry.ORCA_BREAKABLES) && fluidState.isEmpty()) {
                            if (block != Blocks.AIR) {
                                this.setVelocity(this.getVelocity().multiply(0.6F, 1, 0.6F));
                                flag = true;
                                if (state.isIn(BlockTags.ICE)) {
                                    getWorld().breakBlock(pos, false);
                                    getWorld().setBlockState(pos, Blocks.WATER.getDefaultState());
                                }else{
                                    getWorld().breakBlock(pos, true);
                                }
                            }
                        }
                    }
                }
            }
        }
        if (flag) {
            blockBreakCounter = 20;
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (jumpCooldown > 0) {
            jumpCooldown--;
            float f2 = (float) -((float) this.getVelocity().y * (double) MathHelper.DEGREES_PER_RADIAN);
            this.setPitch(f2);
        }
        if (this.isAiDisabled()) {
            this.setAir(this.getMaxAir());
        } else {

            if (this.isWet()) {
                this.setMoistness(2400);
            } else {
                this.setMoistness(this.getMoistness() - 1);
                if (this.getMoistness() <= 0) {
                    this.damage(getDamageSources().dryOut(), 1.0F);
                }

                if (this.isOnGround()) {
                    this.setVelocity(this.getVelocity().add((this.random.nextFloat() * 2.0F - 1.0F) * 0.2F, 0.5D, (this.random.nextFloat() * 2.0F - 1.0F) * 0.2F));
                    this.setYaw( this.random.nextFloat() * 360.0F);
                    this.setOnGround(false);
                    this.velocityDirty = true;
                }
            }

            if (this.getWorld().isClient && this.isTouchingWater() && this.getVelocity().lengthSquared() > 0.03D) {
                Vec3d vector3d = this.getRotationVec(0.0F);
                final float yRotRad = this.getYaw() * MathHelper.RADIANS_PER_DEGREE;
                final float f = MathHelper.cos(yRotRad) * 0.9F;
                final float f1 = MathHelper.sin(yRotRad) * 0.9F;
                final float f2 = 1.2F - this.random.nextFloat() * 0.7F;

                for (int i = 0; i < 2; ++i) {
                    this.getWorld().addParticle(ParticleTypes.DOLPHIN, this.getX() - vector3d.x * (double) f2 + (double) f, this.getY() - vector3d.y, this.getZ() - vector3d.z * (double) f2 + (double) f1, 0.0D, 0.0D, 0.0D);
                    this.getWorld().addParticle(ParticleTypes.DOLPHIN, this.getX() - vector3d.x * (double) f2 - (double) f, this.getY() - vector3d.y, this.getZ() - vector3d.z * (double) f2 - (double) f1, 0.0D, 0.0D, 0.0D);
                }
            }

        }
        LivingEntity attackTarget = this.getTarget();
        if (attackTarget != null && distanceTo(attackTarget) < attackTarget.getWidth() + this.getWidth() + 2) {
            if (this.getAnimation() == ANIMATION_BITE && this.getAnimationTick() == 4) {
                float damage =(float) ((int) this.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE));
                if(attackTarget instanceof DrownedEntity || attackTarget instanceof GuardianEntity){
                    damage *= 2F;
                }
                boolean flag = attackTarget.damage(this.getDamageSources().mobAttack(this), damage);
                if (flag) {
                    this.applyDamageEffects(this, attackTarget);
                    this.playSound(SoundEvents.ENTITY_DOLPHIN_ATTACK, 1.0F, 1.0F);
                }
            }
            if (this.getAnimation() == ANIMATION_TAILSWING && this.getAnimationTick() == 6) {
                float damage =(float) ((int) this.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE));
                if(attackTarget instanceof DrownedEntity || attackTarget instanceof GuardianEntity){
                    damage *= 2F;
                }
                boolean flag = attackTarget.damage(this.getDamageSources().mobAttack(this), damage);
                if (flag) {
                    this.applyDamageEffects(this, attackTarget);
                    this.playSound(SoundEvents.ENTITY_DOLPHIN_ATTACK, 1.0F, 1.0F);
                }
                final float yRotRad = this.getYaw() * MathHelper.RADIANS_PER_DEGREE;
                attackTarget.takeKnockback(1F, MathHelper.sin(yRotRad), -MathHelper.cos(yRotRad));
                float knockbackResist = (float) MathHelper.clamp((1.0D - this.getAttributeValue(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE)), 0, 1);
                this.getTarget().setVelocity(this.getTarget().getVelocity().add(0, knockbackResist * 0.4F, 0));

            }
        }
        if (attackTarget != null && attackTarget instanceof PlayerEntity && attackTarget.hasStatusEffect(AMEffectRegistry.ORCAS_MIGHT.get())) {
            attackTarget.removeStatusEffect(AMEffectRegistry.ORCAS_MIGHT.get());
        }
        AnimationHandler.INSTANCE.updateAnimations(this);
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
    public boolean tryAttack(Entity entityIn) {
        if(this.isInsideWaterOrBubbleColumn() && random.nextBoolean()){
            this.setAnimation(ANIMATION_TAILSWING);
        }else{
            this.setAnimation(ANIMATION_BITE);
        }
        return true;
    }

    @Override
    public int getMaxAir() {
        return 4800;
    }

    @Override
    protected int getNextAirOnLand(int currentAir) {
        return this.getMaxAir();
    }

    @Override
    protected float getActiveEyeHeight(EntityPose poseIn, EntityDimensions sizeIn) {
        return 1.0F;
    }

    @Override
    public int getMaxLookPitchChange() {
        return 1;
    }

    @Override
    public int getMaxHeadRotation() {
        return 1;
    }

    @Override
    public boolean isBreedingItem(ItemStack stack) {
        return stack.getItem() == Items.SALMON;
    }

    @Nullable
    @Override
    public PassiveEntity createChild(ServerWorld serverWorld, PassiveEntity p_241840_2_) {
        return AMEntityRegistry.ORCA.get().create(serverWorld);
    }

    public boolean shouldUseJumpAttack(LivingEntity attackTarget) {
        if (attackTarget.isTouchingWater()) {
            BlockPos up = attackTarget.getBlockPos().up();
            return getWorld().getFluidState(up.up()).isEmpty() && getWorld().getFluidState(up.up(2)).isEmpty() && this.jumpCooldown == 0;
        } else {
            return this.jumpCooldown == 0;
        }
    }

    @Nullable
    @Override
    public EntityData initialize(ServerWorldAccess worldIn, LocalDifficulty difficultyIn, SpawnReason
            reason, @Nullable EntityData spawnDataIn, @Nullable NbtCompound dataTag) {
        this.setAir(this.getMaxAir());
        this.setVariant(determineVariant(this.getBlockPos()));
        this.setPitch(0.0F);
        this.setMoistness(2400);
        return super.initialize(worldIn, difficultyIn, reason, spawnDataIn, dataTag);
    }

    @Override
    public boolean canBreatheInWater() {
        return false;
    }

    @Override
    public void baseTick() {
        int i = this.getAir();
        super.baseTick();
        this.updateAir(i);
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
    public boolean canSpawn(WorldView worldIn) {
        return worldIn.doesNotIntersectEntities(this);
    }

    protected void updateAir(int p_209207_1_) {
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound compound) {
        super.writeCustomDataToNbt(compound);
        compound.putInt("Moistness", this.getMoistness());
        compound.putInt("Variant", this.getVariant());
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound compound) {
        super.readCustomDataFromNbt(compound);
        this.setMoistness(compound.getInt("Moistness"));
        this.setVariant(compound.getInt("Variant"));
    }

    public void onJumpHit(LivingEntity entityIn) {
        boolean flag = entityIn.damage(this.getDamageSources().mobAttack(this), (float) ((int) this.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE)));
        if (flag) {
            this.applyDamageEffects(this, entityIn);
            this.playSound(SoundEvents.ENTITY_DOLPHIN_ATTACK, 1.0F, 1.0F);
        }
    }

    public static boolean canOrcaSpawn(EntityType<EntityOrca> p_223364_0_, WorldAccess p_223364_1_, SpawnReason reason, BlockPos p_223364_3_, Random p_223364_4_) {
        if (p_223364_3_.getY() > 45 && p_223364_3_.getY() < p_223364_1_.getSeaLevel()) {
            return p_223364_1_.getFluidState(p_223364_3_).isIn(FluidTags.WATER);
        } else {
            return false;
        }
    }

    @Override
    public EntityView method_48926() {
        return getWorld();
    }

    static class SwimWithPlayerGoal extends Goal {
        private final EntityOrca dolphin;
        private final double speed;
        private PlayerEntity targetPlayer;

        SwimWithPlayerGoal(EntityOrca dolphinIn, double speedIn) {
            this.dolphin = dolphinIn;
            this.speed = speedIn;
            this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
        }

        @Override
        public boolean canStart() {
            this.targetPlayer = this.dolphin.getWorld().getClosestPlayer(EntityOrca.PLAYER_PREDICATE, this.dolphin);
            if (this.targetPlayer == null) {
                return false;
            } else {
                return this.targetPlayer.isSwimming() && this.dolphin.getTarget() != this.targetPlayer;
            }
        }

        @Override
        public boolean shouldContinue() {
            return this.targetPlayer != null  && this.dolphin.getTarget() != this.targetPlayer && this.targetPlayer.isSwimming() && this.dolphin.squaredDistanceTo(this.targetPlayer) < 256.0D;
        }

        @Override
        public void start() {
        }

        @Override
        public void stop() {
            this.targetPlayer = null;
            this.dolphin.getNavigation().stop();
        }

        @Override
        public void tick() {
            this.dolphin.getLookControl().lookAt(this.targetPlayer, (float) (this.dolphin.getMaxHeadRotation() + 20), (float) this.dolphin.getMaxLookPitchChange());
            if (this.dolphin.squaredDistanceTo(this.targetPlayer) < 10D) {
                this.dolphin.getNavigation().stop();
            } else {
                this.dolphin.getNavigation().startMovingTo(this.targetPlayer, this.speed);
            }

            if (this.targetPlayer.isSwimming() && this.targetPlayer.getWorld().random.nextInt(6) == 0) {
                this.targetPlayer.addStatusEffect(new StatusEffectInstance(AMEffectRegistry.ORCAS_MIGHT.get(), 1000));
            }
        }
    }

    static class MoveHelperController extends MoveControl {
        private final EntityOrca dolphin;

        public MoveHelperController(EntityOrca dolphinIn) {
            super(dolphinIn);
            this.dolphin = dolphinIn;
        }

        @Override
        public void tick() {
            if (this.dolphin.isTouchingWater()) {
                this.dolphin.setVelocity(this.dolphin.getVelocity().add(0.0D, 0.005D, 0.0D));
            }

            if (this.state == MoveControl.State.MOVE_TO && !this.dolphin.getNavigation().isIdle()) {
                final double d0 = this.targetX - this.dolphin.getX();
                final double d1 = this.targetY - this.dolphin.getY();
                final double d2 = this.targetZ - this.dolphin.getZ();
                final double d3 = d0 * d0 + d1 * d1 + d2 * d2;
                if (d3 < (double) 2.5000003E-7F) {
                    this.entity.setForwardSpeed(0.0F);
                } else {
                    final float f = (float) (MathHelper.atan2(d2, d0) * (double) MathHelper.DEGREES_PER_RADIAN) - 90.0F;
                    this.dolphin.setYaw(this.wrapDegrees(this.dolphin.getYaw(), f, 10.0F));
                    this.dolphin.bodyYaw = this.dolphin.getYaw();
                    this.dolphin.headYaw= this.dolphin.getYaw();
                    final float f1 = (float) (this.speed * this.dolphin.getAttributeValue(EntityAttributes.GENERIC_MOVEMENT_SPEED));
                    if (this.dolphin.isTouchingWater()) {
                        this.dolphin.setMovementSpeed(f1 * 0.02F);
                        float f2 = -((float) (MathHelper.atan2(d1, MathHelper.sqrt((float) (d0 * d0 + d2 * d2))) * (double) MathHelper.DEGREES_PER_RADIAN));
                        f2 = MathHelper.clamp(MathHelper.wrapDegrees(f2), -85.0F, 85.0F);
                        this.dolphin.setPitch(this.wrapDegrees(this.dolphin.getPitch(), f2, 5.0F));
                        final float xRotRad = this.dolphin.getPitch() * MathHelper.RADIANS_PER_DEGREE;
                        final float f3 = MathHelper.cos(xRotRad);
                        final float f4 = MathHelper.sin(xRotRad);
                        this.dolphin.forwardSpeed = f3 * f1;
                        this.dolphin.upwardSpeed = -f4 * f1;
                    } else {
                        this.dolphin.setMovementSpeed(f1 * 0.1F);
                    }

                }
            } else {
                this.dolphin.setMovementSpeed(0.0F);
                this.dolphin.setSidewaysSpeed(0.0F);
                this.dolphin.setUpwardSpeed(0.0F);
                this.dolphin.setForwardSpeed(0.0F);
            }
        }
    }

}
