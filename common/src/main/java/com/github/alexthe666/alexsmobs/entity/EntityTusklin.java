package com.github.alexthe666.alexsmobs.entity;

import com.github.alexthe666.alexsmobs.AlexsMobs;
import com.github.alexthe666.alexsmobs.config.AMConfig;
import com.github.alexthe666.alexsmobs.entity.ai.*;
import com.github.alexthe666.alexsmobs.entity.util.Maths;
import com.github.alexthe666.alexsmobs.registry.AMEntityRegistry;
import com.github.alexthe666.alexsmobs.registry.AMItemRegistry;
import com.github.alexthe666.alexsmobs.registry.AMSoundRegistry;
import com.github.alexthe666.alexsmobs.registry.AMTagRegistry;
import com.iafenvoy.uranus.animation.Animation;
import com.iafenvoy.uranus.animation.AnimationHandler;
import com.iafenvoy.uranus.animation.IAnimatedEntity;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.event.GameEvent;
import org.jetbrains.annotations.Nullable;

public class EntityTusklin extends AnimalEntity implements IAnimatedEntity {

    public static final Animation ANIMATION_RUT = Animation.create(26);
    public static final Animation ANIMATION_GORE_L = Animation.create(25);
    public static final Animation ANIMATION_GORE_R = Animation.create(25);
    public static final Animation ANIMATION_FLING = Animation.create(15);
    public static final Animation ANIMATION_BUCK = Animation.create(15);
    private static final TrackedData<Boolean> SADDLED = DataTracker.registerData(EntityTusklin.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Integer> PASSIVETICKS = DataTracker.registerData(EntityTusklin.class, TrackedDataHandlerRegistry.INTEGER);
    private int animationTick;
    private Animation currentAnimation;
    private int ridingTime = 0;
    private int entityToLaunchId = -1;
    private int conversionTime = 0;

    public EntityTusklin(EntityType<? extends AnimalEntity> type, World level) {
        super(type, level);
        this.setStepHeight(1.1F);
    }

    @Override
    public boolean canSpawn(WorldAccess worldIn, SpawnReason spawnReasonIn) {
        return AMEntityRegistry.rollSpawn(AMConfig.tusklinSpawnRolls, this.getRandom(), spawnReasonIn);
    }

    public static boolean canTusklinSpawn(EntityType<? extends AnimalEntity> animal, WorldAccess worldIn, SpawnReason reason, BlockPos pos, Random random) {
        return worldIn.getBaseLightLevel(pos, 0) > 8 && (worldIn.getBlockState(pos.down()).isSolid() || worldIn.getBlockState(pos.down()).isIn(AMTagRegistry.TUSKLIN_SPAWNS));
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createLivingAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 40D)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 9.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.3F)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.9F)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 0.9F);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return AMSoundRegistry.TUSKLIN_IDLE.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return AMSoundRegistry.TUSKLIN_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return AMSoundRegistry.TUSKLIN_HURT.get();
    }

    @Override
    protected EntityNavigation createNavigation(World worldIn) {
        return new AdvancedPathNavigateNoTeleport(this, worldIn, true);
    }

    public boolean isInNether() {
        return this.getWorld().getRegistryKey() == World.NETHER && !this.isAiDisabled();
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(1, new SwimGoal(this));
        this.goalSelector.add(2, new AnimalAIMeleeNearby(this, 15, 1.25D));
        this.goalSelector.add(3, new TameableAIRide(this, 2D, false) {
            @Override
            public boolean shouldMoveForward() {
                return true;
            }

            @Override
            public boolean shouldMoveBackwards() {
                return false;
            }
        });
        this.goalSelector.add(4, new AnimalAIPanicBaby(this, 1.25D));
        this.goalSelector.add(5, new AnimalMateGoal(this, 1.0D));
        this.goalSelector.add(6, new FollowParentGoal(this, 1.1D));
        this.goalSelector.add(7, new AnimalAIWanderRanged(this, 120, 0.6F, 14, 7));
        this.goalSelector.add(8, new LookAtEntityGoal(this, PlayerEntity.class, 15.0F));
        this.goalSelector.add(8, new LookAroundGoal(this));
        this.targetSelector.add(1, (new AnimalAIHurtByTargetNotBaby(this)).setGroupRevenge());
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, 100, true, false, this::shouldAngerAt));
    }

    // FIXME should be overrided?
    public boolean shouldAngerAt(LivingEntity p_21675_) {
        return this.canTarget(p_21675_);
    }

    @Override
    protected Vec3d getControlledMovementInput(PlayerEntity player, Vec3d deltaIn) {
        return new Vec3d(0, 0, 1);
    }

    @Override
    protected void tickControlled(PlayerEntity player, Vec3d vec3) {
        super.tickControlled(player, vec3);
        this.setRotation(player.getYaw(), player.getPitch() * 0.25F);
        this.prevYaw = this.bodyYaw = this.headYaw= this.getYaw();
        this.setStepHeight(1);
        this.getNavigation().stop();
        this.setTarget(null);
        this.setSprinting(true);
    }

    @Override
    protected float getSaddledSpeed(PlayerEntity rider) {
        return (float)(this.getAttributeValue(EntityAttributes.GENERIC_MOVEMENT_SPEED));
    }

    @Nullable
    public LivingEntity getControllingPassenger() {
        if (this.isSaddled()) {
            for (var passenger : this.getPassengerList()) {
                if (passenger instanceof PlayerEntity) {
                    return (PlayerEntity) passenger;
                }
            }
        }
        return null;
    }

    @Override
    public boolean canTarget(LivingEntity entity) {
        boolean prev = super.canTarget(entity);

        if (entity instanceof PlayerEntity && (this.getAttacker() == null || !this.getAttacker().equals(entity))) {
            if (this.getPassiveTicks() > 0 || isMushroom(entity.getStackInHand(Hand.MAIN_HAND)) || isMushroom(entity.getStackInHand(Hand.OFF_HAND))) {
                return false;
            }
        }
        return prev;
    }

    @Override
    public boolean tryAttack(Entity entityIn) {
        if (this.getAnimation() == IAnimatedEntity.NO_ANIMATION) {
            final int anim = this.random.nextInt(3);
            switch (anim) {
                case 0 -> this.setAnimation(ANIMATION_FLING);
                case 1 -> this.setAnimation(ANIMATION_GORE_L);
                case 2 -> this.setAnimation(ANIMATION_GORE_R);
            }
        }
        return true;
    }

    @Override
    public void updatePassengerPosition(Entity passenger, Entity.PositionUpdater moveFunc) {
        if (this.hasPassenger(passenger)) {
            float radius = 0.4F;
            if (this.getAnimation() == ANIMATION_GORE_L || this.getAnimation() == ANIMATION_GORE_R) {
                if (this.getAnimationTick() <= 4) {
                    radius -= this.getAnimationTick() * 0.1F;
                } else {
                    radius -= -0.4F + Math.min(this.getAnimationTick() - 4, 4) * 0.1F;
                }
            }
            if (this.getAnimation() == ANIMATION_BUCK) {
                if (this.getAnimationTick() < 5) {
                    radius -= this.getAnimationTick() * 0.1F;
                } else if (this.getAnimationTick() < 10) {
                    radius -= 0.4F - (this.getAnimationTick() - 5) * 0.1F;
                }
            }
            final float angle = (Maths.STARTING_ANGLE * this.bodyYaw);
            final double extraX = radius * MathHelper.sin(MathHelper.PI + angle);
            final double extraZ = radius * MathHelper.cos(angle);
            passenger.setPos(this.getX() + extraX, this.getY() + this.getMountedHeightOffset() + passenger.getHeightOffset(), this.getZ() + extraZ);
        }
    }

    @Override
    public double getMountedHeightOffset() {
        final float f = this.limbAnimator.getPos();
        final float f1 = this.limbAnimator.getSpeed();
        float f2 = 0;
        if (this.getAnimation() == ANIMATION_FLING) {
            if (this.getAnimationTick() <= 3F) {
                f2 = this.getAnimationTick() * -0.1F;
            } else {
                f2 = -0.3F + MathHelper.clamp(this.getAnimationTick() - 3, 0, 3) * 0.1F;
            }
        }
        if (this.getAnimation() == ANIMATION_BUCK) {
            if (this.getAnimationTick() < 5) {
                f2 = (this.getAnimationTick() * 0.2F) * 0.8F;
            } else if (this.getAnimationTick() < 10) {
                f2 = (0.8F - (this.getAnimationTick() - 5) * 0.2F) * 0.8F;
            }
        }
        return (double) this.getHeight() - 0.3D + (float) (Math.abs(Math.sin(f * 0.7F) * (double) f1 * 0.0625F * 1.6F)) + f2;
    }

    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        ItemStack itemstack = player.getStackInHand(hand);
        Item item = itemstack.getItem();
        if (item == Items.SADDLE && !this.isSaddled() && !this.isBaby()) {
            if (!player.isCreative()) {
                itemstack.decrement(1);
            }
            this.setSaddled(true);
            return ActionResult.SUCCESS;
        }
        if (item == AMItemRegistry.PIGSHOES.get() && this.getShoeStack().isEmpty() && !this.isBaby()) {
            this.setShoeStack(itemstack.copy());
            if (!player.isCreative()) {
                itemstack.decrement(1);
            }
            return ActionResult.SUCCESS;
        }
        if (isMushroom(itemstack) && (this.getPassiveTicks() <= 0 || this.getHealth() < this.getMaxHealth())) {
            if (!player.isCreative()) {
                itemstack.decrement(1);
            }
            this.heal(6);
            this.setPassiveTicks(this.getPassiveTicks() + 1200);
            return ActionResult.SUCCESS;
        }
        ActionResult type = super.interactMob(player, hand);
        if (type != ActionResult.SUCCESS && !isBreedingItem(itemstack)) {
            if (!player.isSneaking() && !this.isBaby() && this.isSaddled() && this.getAnimation() != ANIMATION_BUCK) {
                player.startRiding(this);
                return ActionResult.SUCCESS;
            }
        }
        return type;
    }

    @Override
    public boolean isBreedingItem(ItemStack stack) {
        return stack.isIn(AMTagRegistry.TUSKLIN_BREEDABLES);
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(SADDLED, false);
        this.dataTracker.startTracking(PASSIVETICKS, 0);
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound p_31808_) {
        super.writeCustomDataToNbt(p_31808_);
        if (!this.getShoeStack().isEmpty()) {
            p_31808_.put("ShoeItem", this.getShoeStack().writeNbt(new NbtCompound()));
        }
        p_31808_.putInt("PassiveTicks", this.getPassiveTicks());

        p_31808_.putBoolean("Saddle", this.isSaddled());
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound p_31795_) {
        super.readCustomDataFromNbt(p_31795_);
        this.setSaddled(p_31795_.getBoolean("Saddle"));
        this.setPassiveTicks(p_31795_.getInt("PassiveTicks"));
        var NbtCompound = p_31795_.getCompound("ShoeItem");
        if (NbtCompound != null && !NbtCompound.isEmpty()) {
            var itemstack = ItemStack.fromNbt(NbtCompound);
            if (itemstack.isEmpty()) {
                AlexsMobs.LOGGER.warn("Unable to load item from: {}", NbtCompound);
            }
            this.setShoeStack(itemstack);
        }
    }

    public boolean isMushroom(ItemStack stack) {
        return stack.isIn(AMTagRegistry.TUSKLIN_FOODSTUFFS);
    }

    public int getPassiveTicks() {
        return this.dataTracker.get(PASSIVETICKS);
    }

    private void setPassiveTicks(int passiveTicks) {
        this.dataTracker.set(PASSIVETICKS, passiveTicks);
    }

    public boolean isSaddled() {
        return this.dataTracker.get(SADDLED);
    }

    public void setSaddled(boolean saddled) {
        this.dataTracker.set(SADDLED, saddled);
    }

    @Override
    protected void dropInventory() {
        super.dropInventory();
        if (this.isSaddled()) {
            if (!this.getWorld().isClient) {
                this.dropItem(Items.SADDLE);
            }
        }
        if (!this.getShoeStack().isEmpty()) {
            if (!this.getWorld().isClient) {
                this.dropStack(this.getShoeStack().copy());
            }
        }
        this.setSaddled(false);
        this.setShoeStack(ItemStack.EMPTY);
    }

    public ItemStack getShoeStack() {
        return this.getEquippedStack(EquipmentSlot.FEET);
    }

    public void setShoeStack(ItemStack shoe) {
        this.equipStack(EquipmentSlot.FEET, shoe);
    }

    @Override
    public void tick() {
        super.tick();
        if(isInNether()) {
            conversionTime++;
            if (conversionTime > 300 && !this.getWorld().isClient) {
                var hoglin = this.convertTo(EntityType.HOGLIN, false);
                if(hoglin != null){
                    hoglin.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 200, 0));
                    this.dropInventory();
                    getWorld().spawnEntity(hoglin);
                    this.remove(RemovalReason.DISCARDED);
                }
            }
        }
        if (entityToLaunchId != -1 && this.isAlive()) {
            Entity launch = this.getWorld().getEntityById(entityToLaunchId);
            this.removeAllPassengers();
            entityToLaunchId = -1;
            if (launch != null && !launch.hasVehicle()) {
                if (launch instanceof LivingEntity) {
                    launch.setPosition(this.getEyePos().add(0, 1, 0));
                    float rot = 180F + this.getYaw();
                    float strength = (float) (getLaunchStrength() * (1.0D - ((LivingEntity) launch).getAttributeValue(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE)));
                    float x = MathHelper.sin(rot * MathHelper.RADIANS_PER_DEGREE);
                    float z = -MathHelper.cos(rot * MathHelper.RADIANS_PER_DEGREE);
                    if (!(strength <= 0.0D)) {
                        launch.velocityDirty = true;
                        Vec3d vec3 = this.getVelocity();
                        Vec3d vec31 = vec3.add((new Vec3d(x, 0.0D, z)).normalize().multiply(strength));
                        launch.setVelocity(vec31.x, strength, vec31.z);
                    }
                }
            }
        }
        if (this.getAnimation() == ANIMATION_BUCK && this.getAnimationTick() >= 5) {
            Entity passenger = this.getControllingPassenger();
            if (passenger instanceof LivingEntity) {
                entityToLaunchId = passenger.getId();
            }
        }
        if (!this.getWorld().isClient) {
            if (this.hasPassengers()) {
                ridingTime++;
                if (ridingTime >= this.getMaxRidingTime() && this.getAnimation() != ANIMATION_BUCK) {
                    this.setAnimation(ANIMATION_BUCK);
                }
            } else {
                ridingTime = 0;
            }
            if (this.isAlive() && ridingTime > 0 && this.getVelocity().horizontalLengthSquared() > 0.1D) {
                for (var entity : this.getWorld().getNonSpectatingEntities(LivingEntity.class, this.getBoundingBox().expand(1.0D))) {
                    if (!(entity instanceof EntityTusklin) && !entity.isConnectedThroughVehicle(this)) {
                        entity.damage(this.getDamageSources().mobAttack(this), 4F + random.nextFloat() * 3.0F);
                        if (entity.isOnGround()) {
                            double d0 = entity.getX() - this.getX();
                            double d1 = entity.getZ() - this.getZ();
                            double d2 = Math.max(d0 * d0 + d1 * d1, 0.001D);
                            float f = 0.5F;
                            entity.addVelocity(d0 / d2 * f, f, d1 / d2 * f);
                        }
                    }
                }
                this.setStepHeight(2F);
            }else{
                this.setStepHeight(1.1F);
            }
            if (this.getTarget() != null && this.canSee(this.getTarget()) && distanceTo(this.getTarget()) < this.getTarget().getWidth() + this.getWidth() + 1.8F) {
                if (this.getAnimation() == ANIMATION_FLING && this.getAnimationTick() == 6) {
                    this.getTarget().damage(this.getDamageSources().mobAttack(this), (float) this.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE));
                    knockbackTarget(this.getTarget(), 0.9F, 0F);
                }
                if ((this.getAnimation() == ANIMATION_GORE_L) && this.getAnimationTick() == 6) {
                    this.getTarget().damage(this.getDamageSources().mobAttack(this), (float) this.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE));
                    knockbackTarget(this.getTarget(), 0.5F, -90F);
                }
                if ((this.getAnimation() == ANIMATION_GORE_R) && this.getAnimationTick() == 6) {
                    this.getTarget().damage(this.getDamageSources().mobAttack(this), (float) this.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE));
                    knockbackTarget(this.getTarget(), 0.5F, 90F);
                }
            }
        }
        if (this.getAnimation() == ANIMATION_RUT && this.getAnimationTick() == 23) {
            if (getWorld().getBlockState(this.getBlockPos().down()).isOf(Blocks.GRASS_BLOCK) && getRandom().nextInt(3) == 0) {
                if (this.isBaby()) {
                    if (getWorld().getBlockState(this.getBlockPos()).isReplaceable() && random.nextInt(3) == 0) {
                        getWorld().setBlockState(this.getBlockPos(), Blocks.BROWN_MUSHROOM.getDefaultState());
                        this.emitGameEvent(GameEvent.BLOCK_DESTROY);
                        this.playSound(SoundEvents.ITEM_CROP_PLANT, this.getSoundVolume(), this.getSoundPitch());
                    }
                }
                this.getWorld().syncWorldEvent(2001, getBlockPos().down(), Block.getRawIdFromState(Blocks.GRASS_BLOCK.getDefaultState()));
                this.getWorld().setBlockState(getBlockPos().down(), Blocks.DIRT.getDefaultState(), 2);
                this.heal(5);
            }
        }
        if (!this.getWorld().isClient && this.getAnimation() == NO_ANIMATION && getRandom().nextInt(isBaby() ? 140 : 70) == 0 && (this.getAttacker() == null || this.distanceTo(this.getAttacker()) > 30)) {
            if (getWorld().getBlockState(this.getBlockPos().down()).isOf(Blocks.GRASS_BLOCK) && getRandom().nextInt(3) == 0) {
                this.setAnimation(ANIMATION_RUT);
            }
        }
        AnimationHandler.INSTANCE.updateAnimations(this);
    }

    private float getLaunchStrength() {
        return this.getShoeStack().isOf(AMItemRegistry.PIGSHOES.get()) ? 0.4F : 0.9F;
    }

    private int getMaxRidingTime() {
        return this.getShoeStack().isOf(AMItemRegistry.PIGSHOES.get()) ? 160 : 60;
    }

    private void knockbackTarget(LivingEntity entity, float strength, float angle) {
        float rot = getYaw() + angle;
        if(entity != null){
            entity.takeKnockback(strength, MathHelper.sin(rot * MathHelper.RADIANS_PER_DEGREE), -MathHelper.cos(rot * MathHelper.RADIANS_PER_DEGREE));
        }
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
    public Animation getAnimation() {
        return currentAnimation;
    }

    @Override
    public void setAnimation(Animation animation) {
        currentAnimation = animation;
    }

    @Override
    public EntityData initialize(ServerWorldAccess worldIn, LocalDifficulty difficultyIn, SpawnReason reason, @Nullable EntityData spawnDataIn, @Nullable NbtCompound dataTag) {
        if (spawnDataIn == null) {
            spawnDataIn = new PassiveEntity.PassiveData(0.34F);
        }

        return super.initialize(worldIn, difficultyIn, reason, spawnDataIn, dataTag);
    }

    @Override
    public Animation[] getAnimations() {
        return new Animation[]{ANIMATION_RUT, ANIMATION_GORE_L, ANIMATION_GORE_R, ANIMATION_FLING, ANIMATION_BUCK};
    }

    @Nullable
    @Override
    public PassiveEntity createChild(ServerWorld level, PassiveEntity mob) {
        return AMEntityRegistry.TUSKLIN.get().create(getWorld());
    }
}
