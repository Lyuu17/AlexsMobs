package com.github.alexthe666.alexsmobs.entity;

import com.github.alexthe666.alexsmobs.config.AMConfig;
import com.github.alexthe666.alexsmobs.entity.ai.AnimalAISwimBottom;
import com.github.alexthe666.alexsmobs.entity.ai.AquaticMoveController;
import com.github.alexthe666.alexsmobs.entity.ai.EntityAINearestTarget3D;
import com.github.alexthe666.alexsmobs.entity.util.Maths;
import com.github.alexthe666.alexsmobs.registry.AMEffectRegistry;
import com.github.alexthe666.alexsmobs.registry.AMEntityRegistry;
import com.github.alexthe666.alexsmobs.registry.AMItemRegistry;
import com.github.alexthe666.alexsmobs.registry.AMParticleRegistry;
import com.iafenvoy.uranus.animation.Animation;
import com.iafenvoy.uranus.animation.AnimationHandler;
import com.iafenvoy.uranus.animation.IAnimatedEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.ai.pathing.SwimNavigation;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.mob.DrownedEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.WaterCreatureEntity;
import net.minecraft.entity.passive.SchoolingFishEntity;
import net.minecraft.entity.passive.SquidEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public class EntityFrilledShark extends WaterCreatureEntity implements IAnimatedEntity, Bucketable {

    public static final Animation ANIMATION_ATTACK = Animation.create(17);
    private static final TrackedData<Boolean> DEPRESSURIZED = DataTracker.registerData(EntityFrilledShark.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> FROM_BUCKET = DataTracker.registerData(EntityFrilledShark.class, TrackedDataHandlerRegistry.BOOLEAN);
    public float prevOnLandProgress;
    public float onLandProgress;
    private int animationTick;
    private Animation currentAnimation;

    public EntityFrilledShark(EntityType<? extends EntityFrilledShark> type, World worldIn) {
        super(type, worldIn);
        this.moveControl = new AquaticMoveController(this, 1F);
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createLivingAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 20D)
                .add(EntityAttributes.GENERIC_ARMOR, 0.0D)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 3.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.2F)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32.0D);
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(DEPRESSURIZED, false);
        this.dataTracker.startTracking(FROM_BUCKET, false);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(1, new MoveIntoWaterGoal(this));
        this.goalSelector.add(2, new AIMelee());
        this.goalSelector.add(3, new AnimalAISwimBottom(this, 0.8F, 7));
        this.goalSelector.add(4, new SwimAroundGoal(this, 0.8F, 3));
        this.goalSelector.add(5, new LookAroundGoal(this));
        this.goalSelector.add(6, new ChaseBoatGoal(this));
        this.targetSelector.add(1, (new RevengeGoal(this)));
        this.targetSelector.add(2, new EntityAINearestTarget3D<>(this, SquidEntity.class, 40, false, true, null));
        this.targetSelector.add(2, new EntityAINearestTarget3D<>(this, EntityMimicOctopus.class, 70, false, true, null));
        this.targetSelector.add(3, new EntityAINearestTarget3D<>(this, SchoolingFishEntity.class, 100, false, true, null));
        this.targetSelector.add(4, new EntityAINearestTarget3D<>(this, EntityBlobfish.class, 70, false, true, null));
        this.targetSelector.add(5, new EntityAINearestTarget3D<>(this, DrownedEntity.class, 4, false, true, null));
    }

    @Override
    public boolean canSpawn(WorldAccess worldIn, SpawnReason spawnReasonIn) {
        return AMEntityRegistry.rollSpawn(AMConfig.frilledSharkSpawnRolls, this.getRandom(), spawnReasonIn);
    }

    public static boolean canFrilledSharkSpawn(EntityType<EntityFrilledShark> entityType, ServerWorldAccess iServerWorld, SpawnReason reason, BlockPos pos, Random random) {
        return reason == SpawnReason.SPAWNER || iServerWorld.isWater(pos) && iServerWorld.isWater(pos.up());
    }

    @Override
    public boolean isFromBucket() {
        return this.dataTracker.get(FROM_BUCKET);
    }

    @Override
    public void setFromBucket(boolean p_203706_1_) {
        this.dataTracker.set(FROM_BUCKET, p_203706_1_);
    }

    @Override
    @NotNull
    public SoundEvent getBucketFillSound() {
        return SoundEvents.ITEM_BUCKET_FILL_FISH;
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound compound) {
        super.writeCustomDataToNbt(compound);
        compound.putBoolean("FromBucket", this.isFromBucket());
        compound.putBoolean("Depressurized", this.isDepressurized());
    }

    @Override
    public boolean cannotDespawn() {
        return super.cannotDespawn() || this.isFromBucket();
    }

    @Override
    public boolean canImmediatelyDespawn(double p_213397_1_) {
        return !this.isFromBucket() && !this.hasCustomName();
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound compound) {
        super.readCustomDataFromNbt(compound);
        this.setFromBucket(compound.getBoolean("FromBucket"));
        this.setDepressurized(compound.getBoolean("Depressurized"));
    }

    private void doInitialPosing(WorldAccess world) {
        BlockPos down = this.getBlockPos();
        while(!world.getFluidState(down).isEmpty() && down.getY() > 1){
            down = down.down();
        }
        this.setPos(down.getX() + 0.5F, down.getY() + 1, down.getZ() + 0.5F);
    }

    @Nullable
    @Override
    public EntityData initialize(ServerWorldAccess worldIn, LocalDifficulty difficultyIn, SpawnReason reason, @Nullable EntityData spawnDataIn, @Nullable NbtCompound dataTag) {
        if (reason == SpawnReason.NATURAL) {
            doInitialPosing(worldIn);
        }
        return super.initialize(worldIn, difficultyIn, reason, spawnDataIn, dataTag);
    }

    @Override
    public boolean canSpawn(WorldView worldIn) {
        return worldIn.doesNotIntersectEntities(this);
    }

    public boolean isDepressurized() {
        return this.dataTracker.get(DEPRESSURIZED);
    }

    public void setDepressurized(boolean depressurized) {
        this.dataTracker.set(DEPRESSURIZED, depressurized);
    }

    @Override
    protected EntityNavigation createNavigation(World worldIn) {
        return new SwimNavigation(this, worldIn);
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENTITY_COD_DEATH;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return SoundEvents.ENTITY_COD_HURT;
    }

    @Override
    @NotNull
    public ItemStack getBucketItem() {
        var stack = new ItemStack(AMItemRegistry.FRILLED_SHARK_BUCKET.get());
        if (this.hasCustomName()) {
            stack.setCustomName(this.getCustomName());
        }
        return stack;
    }

    @Override
    public void copyDataToStack(@NotNull ItemStack bucket) {
        if (this.hasCustomName()) {
            bucket.setCustomName(this.getCustomName());
        }
        NbtCompound platTag = new NbtCompound();
        this.writeCustomDataToNbt(platTag);
        NbtCompound compound = bucket.getOrCreateNbt();
        compound.put("FrilledSharkData", platTag);
    }

    @Override
    public void copyDataFromNbt(@NotNull NbtCompound compound) {
        if (compound.contains("FrilledSharkData")) {
            this.readCustomDataFromNbt(compound.getCompound("FrilledSharkData"));
        }
    }

    @Override
    @NotNull
    protected ActionResult interactMob(@NotNull PlayerEntity player, @NotNull Hand hand) {
        return Bucketable.tryBucket(player, hand, this).orElse(super.interactMob(player, hand));
    }

    @Override
    public void travel(Vec3d travelVector) {
        if (this.canMoveVoluntarily() && this.isTouchingWater()) {
            this.updateVelocity(this.getMovementSpeed(), travelVector);
            this.move(MovementType.SELF, this.getVelocity());
            this.setVelocity(this.getVelocity().multiply(0.9D, 0.6D, 0.9D));
            if (this.getTarget() == null) {
                this.setVelocity(this.getVelocity().add(0.0D, -0.005D, 0.0D));
            }
        } else {
            super.travel(travelVector);
        }

    }

    @Override
    public void updateLimbs(boolean flying) {
        float f1 = (float)MathHelper.magnitude(this.getX() - this.prevX, this.getY() - this.prevY, this.getZ() - this.prevZ);
        float f2 = Math.min(f1 * 8.0F, 1.0F);
        this.limbAnimator.updateLimbs(f2, 0.4F);
    }

    @Override
    public void tick() {
        super.tick();
        this.prevOnLandProgress = onLandProgress;
        if (!this.isTouchingWater() && onLandProgress < 5F) {
            onLandProgress++;
        }
        if (this.isTouchingWater() && onLandProgress > 0F) {
            onLandProgress--;
        }
        if (this.isTouchingWater()) {
            this.setVelocity(this.getVelocity().multiply(1.0D, 0.8D, 1.0D));
        }
        boolean clear = hasClearance();
        if (this.isDepressurized() && clear) {
            this.setDepressurized(false);
        }
        if (!isDepressurized() && !clear) {
            this.setDepressurized(true);
        }
        if (!this.getWorld().isClient && this.getTarget() != null && this.getAnimation() == ANIMATION_ATTACK && this.getAnimationTick() == 12) {
            float f1 = this.getYaw() * MathHelper.RADIANS_PER_DEGREE;
            this.setVelocity(this.getVelocity().add(-MathHelper.sin(f1) * 0.06F, 0.0D, MathHelper.cos(f1) * 0.06F));
            if (this.getTarget().damage(this.getDamageSources().mobAttack(this), (float) this.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE).getBaseValue())){
                this.getTarget().addStatusEffect(new StatusEffectInstance(AMEffectRegistry.EXSANGUINATION.get(), 60, 2));
                if(random.nextInt(15) == 0 && this.getTarget() instanceof SquidEntity){
                    this.dropItem(AMItemRegistry.SERRATED_SHARK_TOOTH.get());
                }
            }

        }
        AnimationHandler.INSTANCE.updateAnimations(this);
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        if (source.getAttacker() instanceof DrownedEntity) {
            amount *= 0.5F;
        }
        return super.damage(source, amount);
    }

    private boolean hasClearance() {
        BlockPos.Mutable blockpos$mutable = new BlockPos.Mutable();
        for (int l1 = 0; l1 < 10; ++l1) {
            var blockstate = getWorld().getBlockState(blockpos$mutable.set(this.getX(), this.getY() + l1, this.getZ()));
            if (!blockstate.getFluidState().isIn(FluidTags.WATER)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public Animation getAnimation() {
        return currentAnimation;
    }

    @Override
    public void setAnimation(Animation animation) {
        currentAnimation = animation;
    }

    public boolean isKaiju() {
        String s = Formatting.strip(this.getName().getString());
        return s != null && (s.toLowerCase().contains("kamata kun") || s.toLowerCase().contains("kamata-kun"));
    }

    @Override
    public Animation[] getAnimations() {
        return new Animation[]{ANIMATION_ATTACK};
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
        if (this.getAnimation() == NO_ANIMATION) {
            this.setAnimation(ANIMATION_ATTACK);
        }
        return true;
    }

    @Environment(EnvType.CLIENT)
    public void handleStatus(byte id) {
        if (id == 68) {
            double d2 = this.random.nextGaussian() * 0.1D;
            double d0 = this.random.nextGaussian() * 0.1D;
            double d1 = this.random.nextGaussian() * 0.1D;
            float radius = this.getWidth() * 0.8F;
            float angle = (Maths.STARTING_ANGLE * this.bodyYaw);
            double extraX = radius * MathHelper.sin(MathHelper.PI + angle);
            double extraZ = radius * MathHelper.cos(angle);
            double x = this.getX() + extraX + d0;
            double y = this.getY() + this.getHeight() * 0.15F + d1;
            double z = this.getZ() + extraZ + d2;
            getWorld().addParticle(AMParticleRegistry.TEETH_GLINT.get(), x, y, z, this.getVelocity().x, this.getVelocity().y, this.getVelocity().z);
        } else {
            super.handleStatus(id);
        }
    }

    private class AIMelee extends Goal {

        public AIMelee() {
            this.setControls(EnumSet.of(Control.MOVE));
        }

        @Override
        public boolean canStart() {
            return EntityFrilledShark.this.getTarget() != null && EntityFrilledShark.this.getTarget().isAlive();
        }

        @Override
        public void tick() {
            LivingEntity target = EntityFrilledShark.this.getTarget();
            double speed = 1.0F;
            if (EntityFrilledShark.this.distanceTo(target) < 10) {
                if (EntityFrilledShark.this.distanceTo(target) < 1.9D) {
                    EntityFrilledShark.this.tryAttack(target);
                    speed = 0.8F;
                } else {
                    speed = 0.6F;
                    EntityFrilledShark.this.lookAtEntity(target, 70, 70);
                    if (target instanceof SquidEntity) {
                        var mouth = EntityFrilledShark.this.getPos();
                        float squidSpeed = 0.07F;
                        ((SquidEntity) target).setSwimmingVector((float) (mouth.x - target.getX()) * squidSpeed, (float) (mouth.y - target.getEyeY()) * squidSpeed, (float) (mouth.z - target.getZ()) * squidSpeed);
                        EntityFrilledShark.this.getWorld().sendEntityStatus(EntityFrilledShark.this, (byte) 68);
                    }
                }
            }
            if (target instanceof DrownedEntity || target instanceof PlayerEntity) {
                speed = 1.0F;
            }
            EntityFrilledShark.this.getNavigation().startMovingTo(target, speed);
        }
    }
}
