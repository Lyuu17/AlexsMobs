package com.github.alexthe666.alexsmobs.entity;

import com.github.alexthe666.alexsmobs.config.AMConfig;
import com.github.alexthe666.alexsmobs.entity.ai.*;
import com.github.alexthe666.alexsmobs.registry.AMEntityRegistry;
import com.github.alexthe666.alexsmobs.registry.AMSoundRegistry;
import com.github.alexthe666.alexsmobs.registry.AMTagRegistry;
import com.iafenvoy.uranus.animation.Animation;
import com.iafenvoy.uranus.animation.AnimationHandler;
import com.iafenvoy.uranus.animation.IAnimatedEntity;
import net.minecraft.block.BlockState;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.control.MoveControl;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.FishEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.passive.TurtleEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ItemStackParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.recipe.Ingredient;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.event.GameEvent;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Predicate;

public class EntityShoebill extends AnimalEntity implements IAnimatedEntity, ITargetsDroppedItems {

    public static final Animation ANIMATION_FISH = Animation.create(40);
    public static final Animation ANIMATION_BEAKSHAKE = Animation.create(20);
    public static final Animation ANIMATION_ATTACK = Animation.create(20);
    private static final TrackedData<Boolean> FLYING = DataTracker.registerData(EntityShoebill.class, TrackedDataHandlerRegistry.BOOLEAN);
    public float prevFlyProgress;
    public float flyProgress;
    public int revengeCooldown = 0;
    private int animationTick;
    private Animation currentAnimation;
    private boolean isLandNavigator;
    public int fishingCooldown = 1200 + random.nextInt(1200);
    public int lureLevel = 0;
    public int luckLevel = 0;
    public static final Predicate<LivingEntity> TARGET_BABY  = LivingEntity::isBaby;

    public EntityShoebill(EntityType<EntityShoebill> type, World world) {
        super(type, world);
        this.setPathfindingPenalty(PathNodeType.WATER, 0.0F);
        this.setPathfindingPenalty(PathNodeType.WATER_BORDER, 0.0F);
        switchNavigator(false);
    }

    @Override
    public boolean canSpawn(WorldAccess worldIn, SpawnReason spawnReasonIn) {
        return AMEntityRegistry.rollSpawn(AMConfig.shoebillSpawnRolls, this.getRandom(), spawnReasonIn);
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createLivingAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 10D)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 4.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.2F)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32.0D);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return AMSoundRegistry.SHOEBILL_HURT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return AMSoundRegistry.SHOEBILL_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return AMSoundRegistry.SHOEBILL_HURT.get();
    }

    @Override
    public boolean isBreedingItem(ItemStack stack) {
        return false;
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        boolean prev = super.damage(source, amount);
        if (prev && source.getAttacker() != null && !(source.getAttacker() instanceof FishEntity)) {
            double range = 15;
            int fleeTime = 100 + getRandom().nextInt(150);
            this.revengeCooldown = fleeTime;
            List<? extends EntityShoebill> list = this.getWorld().getNonSpectatingEntities(this.getClass(), this.getBoundingBox().expand(range, range / 2, range));
            for (EntityShoebill gaz : list) {
                gaz.revengeCooldown = fleeTime;
            }
        }
        return prev;
    }

    private void switchNavigator(boolean onLand) {
        if (onLand) {
            this.moveControl = new MoveControl(this);
            this.navigation = new GroundPathNavigatorWide(this, getWorld());
            this.isLandNavigator = true;
        } else {
            this.moveControl = new FlightMoveController(this, 0.7F, false);
            this.navigation = new DirectPathNavigator(this, getWorld());
            this.isLandNavigator = false;
        }
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(FLYING, false);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new AnimalAIWadeSwimming(this));
        this.goalSelector.add(1, new ShoebillAIFish(this));
        this.goalSelector.add(3, new MeleeAttackGoal(this, 1.2D, true));
        this.goalSelector.add(4, new ShoebillAIFlightFlee(this));
        this.goalSelector.add(5, new TemptGoal(this, 1.1D, Ingredient.fromTag(AMTagRegistry.SHOEBILL_FOODSTUFFS), false));
        this.goalSelector.add(6, new WanderAroundGoal(this, 1D, 1400));
        this.goalSelector.add(7, new LookAroundGoal(this));
        this.goalSelector.add(7, new LookAtEntityGoal(this, PlayerEntity.class, 6.0F));
        this.targetSelector.add(1, new EntityAINearestTarget3D(this, FishEntity.class, 30, false, true, null));
        this.targetSelector.add(2, new CreatureAITargetItems(this, false, 10));
        this.targetSelector.add(3, (new RevengeGoal(this, PlayerEntity.class)).setGroupRevenge());
        this.targetSelector.add(4, new ActiveTargetGoal(this, EntityAlligatorSnappingTurtle.class, 40, false, false, TARGET_BABY));
        this.targetSelector.add(5, new ActiveTargetGoal(this, TurtleEntity.class, 40, false, false, TARGET_BABY));
        this.targetSelector.add(6, new ActiveTargetGoal(this, EntityCrocodile.class, 40, false, false, TARGET_BABY));
        this.targetSelector.add(7, new ActiveTargetGoal(this, EntityCaiman.class, 40, false, false, TARGET_BABY));
        this.targetSelector.add(8, new EntityAINearestTarget3D(this, EntityTerrapin.class, 100, false, true, null));
    }

    public boolean isTargetBlocked(Vec3d target) {
        var Vector3d = new Vec3d(this.getX(), this.getEyeY(), this.getZ());
        return this.getWorld().raycast(new RaycastContext(Vector3d, target, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, this)).getType() != HitResult.Type.MISS;
    }

    @Override
    public boolean handleFallDamage(float distance, float damageMultiplier, DamageSource source) {
        return false;
    }

    @Override
    protected void fall(double y, boolean onGroundIn, BlockState state, BlockPos pos) {
    }

    @Override
    public void tick() {
        super.tick();
        if(this.isTouchingWater()){
            this.setStepHeight(1.2F);
        }else{
            this.setStepHeight(0.6F);
        }
        prevFlyProgress = flyProgress;

        final boolean flying = isFlying();

        if (flying) {
            if (flyProgress < 5F)
                flyProgress++;
        } else {
            if (flyProgress > 0F)
                flyProgress--;
        }

        if (revengeCooldown > 0) {
            revengeCooldown--;
        }
        if (revengeCooldown == 0 && this.getAttacker() != null) {
            this.setAttacker(null);
        }
        if (!this.getWorld().isClient) {
            if(fishingCooldown > 0){
                fishingCooldown--;
            }
            if(this.getAnimation() == NO_ANIMATION && this.getRandom().nextInt(700) == 0){
                this.setAnimation(ANIMATION_BEAKSHAKE);
            }

            if (flying) {
                if (this.isLandNavigator)
                    switchNavigator(false);
            } else {
                if (!this.isLandNavigator)
                    switchNavigator(true);
            }

            if (this.revengeCooldown > 0 && !this.isFlying()) {
                if (this.isOnGround() || this.isTouchingWater()) {
                    this.setFlying(false);
                }
            }

            if (isFlying()) {
                this.setNoGravity(true);
            } else {
                this.setNoGravity(false);
            }
        }
        if (!this.getWorld().isClient && this.getTarget() != null && this.getAnimation() == ANIMATION_ATTACK && this.getAnimationTick() == 9 && this.canSee(this.getTarget())) {
            getTarget().takeKnockback(0.3F, getTarget().getX() - this.getX(), getTarget().getZ() - this.getZ());
            this.getTarget().damage(this.getDamageSources().mobAttack(this), (float) this.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE).getBaseValue());
        }
        AnimationHandler.INSTANCE.updateAnimations(this);
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound compound) {
        super.writeCustomDataToNbt(compound);
        compound.putBoolean("Flying", this.isFlying());
        compound.putInt("FishingTimer", this.fishingCooldown);
        compound.putInt("FishingLuck", this.luckLevel);
        compound.putInt("FishingLure", this.lureLevel);
        compound.putInt("RevengeCooldownTimer", this.revengeCooldown);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound compound) {
        super.readCustomDataFromNbt(compound);
        this.setFlying(compound.getBoolean("Flying"));
        this.fishingCooldown = compound.getInt("FishingTimer");
        this.luckLevel = compound.getInt("FishingLuck");
        this.lureLevel = compound.getInt("FishingLure");
        this.revengeCooldown = compound.getInt("RevengeCooldownTimer");

    }

    @Override
    protected float getBaseMovementSpeedMultiplier() {
        return 0.98F;
    }

    @Override
    public boolean tryAttack(Entity entityIn) {
        if (this.getAnimation() == NO_ANIMATION) {
            this.setAnimation(ANIMATION_ATTACK);
        }
        return true;
    }

    @Override
    public boolean isFlying() {
        return this.dataTracker.get(FLYING);
    }

    @Override
    public void setFlying(boolean flying) {
        this.dataTracker.set(FLYING, flying);
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
        return new Animation[]{ANIMATION_FISH, ANIMATION_BEAKSHAKE, ANIMATION_ATTACK};
    }

    @Override
    public ActionResult interactMob(PlayerEntity p_230254_1_, Hand p_230254_2_) {
        ItemStack lvt_3_1_ = p_230254_1_.getStackInHand(p_230254_2_);
         if (lvt_3_1_.isIn(AMTagRegistry.SHOEBILL_LUCK_FOODS) && this.isAlive()) {
             if(this.luckLevel < 10) {
                 luckLevel = MathHelper.clamp(luckLevel + 1, 0, 10);
                 for (int i = 0; i < 6 + random.nextInt(3); i++) {
                     double d2 = this.random.nextGaussian() * 0.02D;
                     double d0 = this.random.nextGaussian() * 0.02D;
                     double d1 = this.random.nextGaussian() * 0.02D;
                     this.getWorld().addParticle(new ItemStackParticleEffect(ParticleTypes.ITEM, lvt_3_1_), this.getX() + (double) (this.random.nextFloat() * this.getWidth()) - (double) this.getWidth() * 0.5F, this.getY() + this.getHeight() * 0.5F + (double) (this.random.nextFloat() * this.getHeight() * 0.5F), this.getZ() + (double) (this.random.nextFloat() * this.getWidth()) - (double) this.getWidth() * 0.5F, d0, d1, d2);
                 }
                 this.emitGameEvent(GameEvent.EAT);
                 this.playSound(SoundEvents.ENTITY_CAT_EAT, this.getSoundVolume(), this.getSoundPitch());
                 lvt_3_1_.decrement(1);
                 return ActionResult.success(this.getWorld().isClient);
             }else{
                 if(this.getAnimation() == NO_ANIMATION){
                     this.setAnimation(ANIMATION_BEAKSHAKE);
                 }
                 return ActionResult.SUCCESS;
             }
         } else if (lvt_3_1_.isIn(AMTagRegistry.SHOEBILL_LURE_FOODS) && this.isAlive()) {
             if(this.lureLevel < 10){
                 lureLevel = MathHelper.clamp(lureLevel + 1, 0, 10);
                 fishingCooldown = MathHelper.clamp(fishingCooldown - 200, 200, 2400);
                 for (int i = 0; i < 6 + random.nextInt(3); i++) {
                     double d2 = this.random.nextGaussian() * 0.02D;
                     double d0 = this.random.nextGaussian() * 0.02D;
                     double d1 = this.random.nextGaussian() * 0.02D;
                     this.getWorld().addParticle(new ItemStackParticleEffect(ParticleTypes.ITEM, lvt_3_1_), this.getX() + (double) (this.random.nextFloat() * this.getWidth()) - (double) this.getWidth() * 0.5F, this.getY() + this.getHeight() * 0.5F + (double) (this.random.nextFloat() * this.getHeight() * 0.5F), this.getZ() + (double) (this.random.nextFloat() * this.getWidth()) - (double) this.getWidth() * 0.5F, d0, d1, d2);
                 }
                 lvt_3_1_.decrement(1);
                 this.emitGameEvent(GameEvent.EAT);
                 this.playSound(SoundEvents.ENTITY_CAT_EAT, this.getSoundVolume(), this.getSoundPitch());
                 return ActionResult.success(this.getWorld().isClient);
             }else{
                 if(this.getAnimation() == NO_ANIMATION){
                     this.setAnimation(ANIMATION_BEAKSHAKE);
                 }
                 return ActionResult.SUCCESS;
             }

         } else {
            return super.interactMob(p_230254_1_, p_230254_2_);
        }
    }

    @Nullable
    @Override
    public PassiveEntity createChild(ServerWorld world, PassiveEntity entity) {
        return AMEntityRegistry.SHOEBILL.get().create(world);
    }

    @Override
    public boolean canTargetItem(ItemStack stack) {
        return stack.isIn(AMTagRegistry.SHOEBILL_FOODSTUFFS) || stack.isIn(AMTagRegistry.SHOEBILL_LUCK_FOODS) && luckLevel < 10 || stack.isIn(AMTagRegistry.SHOEBILL_LURE_FOODS) && lureLevel < 10;
    }

    public void resetFishingCooldown(){
        fishingCooldown = Math.max(1200 + random.nextInt(1200) - lureLevel * 120, 200);
    }

    @Override
    public void onGetItem(ItemEntity e) {
        this.emitGameEvent(GameEvent.EAT);
        this.playSound(SoundEvents.ENTITY_CAT_EAT, this.getSoundVolume(), this.getSoundPitch());
        if (e.getStack().isIn(AMTagRegistry.SHOEBILL_LUCK_FOODS)) {
            luckLevel = MathHelper.clamp(luckLevel + 1, 0, 10);
        }
        else if (e.getStack().isIn(AMTagRegistry.SHOEBILL_LURE_FOODS)) {
            lureLevel = MathHelper.clamp(lureLevel + 1, 0, 10);
        }
        this.heal(5);
    }
}
