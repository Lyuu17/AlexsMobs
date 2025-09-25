package com.github.alexthe666.alexsmobs.entity;

import com.github.alexthe666.alexsmobs.config.AMConfig;
import com.github.alexthe666.alexsmobs.entity.ai.*;
import com.github.alexthe666.alexsmobs.entity.util.Maths;
import com.github.alexthe666.alexsmobs.registry.AMEntityRegistry;
import com.github.alexthe666.alexsmobs.registry.AMSoundRegistry;
import com.github.alexthe666.alexsmobs.registry.AMTagRegistry;
import com.iafenvoy.uranus.animation.Animation;
import com.iafenvoy.uranus.animation.AnimationHandler;
import com.iafenvoy.uranus.animation.IAnimatedEntity;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.FuzzyTargeting;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ItemStackParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.recipe.Ingredient;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.server.network.ServerPlayerEntity;
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
import net.minecraft.world.*;
import net.minecraft.world.event.GameEvent;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public class EntityGorilla extends TameableEntity implements IAnimatedEntity, ITargetsDroppedItems {
    public static final Animation ANIMATION_BREAKBLOCK_R = Animation.create(20);
    public static final Animation ANIMATION_BREAKBLOCK_L = Animation.create(20);
    public static final Animation ANIMATION_POUNDCHEST = Animation.create(40);
    public static final Animation ANIMATION_ATTACK = Animation.create(20);
    protected static final EntityDimensions SILVERBACK_SIZE = EntityDimensions.changing(1.35F, 1.95F);
    private static final TrackedData<Boolean> SILVERBACK = DataTracker.registerData(EntityGorilla.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> STANDING = DataTracker.registerData(EntityGorilla.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> SITTING = DataTracker.registerData(EntityGorilla.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> EATING = DataTracker.registerData(EntityGorilla.class, TrackedDataHandlerRegistry.BOOLEAN);
    public int maxStandTime = 75;
    public float prevStandProgress;
    public float prevSitProgress;
    public float standProgress;
    public float sitProgress;
    public boolean forcedSit = false;
    private int animationTick;
    private Animation currentAnimation;
    private int standingTime = 0;
    private int eatingTime;
    @Nullable
    private EntityGorilla caravanHead;
    @Nullable
    private EntityGorilla caravanTail;
    private int sittingTime = 0;
    private int maxSitTime = 75;
    @Nullable
    private UUID bananaThrowerID = null;
    private boolean hasSilverbackAttributes = false;
    public int poundChestCooldown = 0;

    public EntityGorilla(EntityType<EntityGorilla> type, World worldIn) {
        super(type, worldIn);
        this.setPathfindingPenalty(PathNodeType.WATER, -1.0F);
        this.setPathfindingPenalty(PathNodeType.LEAVES, 0.0F);
        this.setStepHeight(1.1F);
    }

    @Override
    protected EntityNavigation createNavigation(World worldIn) {
        return new AdvancedPathNavigateNoTeleport(this, worldIn, false);
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createLivingAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 30.0D)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32.0D)
                .add(EntityAttributes.GENERIC_ARMOR, 0.0D)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 7.0D)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.5F)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.25F);
    }

    public static boolean isTameableFood(ItemStack stack) {
        return stack.isIn(AMTagRegistry.BANANAS);
    }

    public static boolean canGorillaSpawn(EntityType<EntityGorilla> gorilla, WorldAccess worldIn, SpawnReason reason, BlockPos p_223317_3_, Random random) {
        BlockState blockstate = worldIn.getBlockState(p_223317_3_.down());
        return (blockstate.isIn(AMTagRegistry.GORILLA_SPAWNS) || blockstate.isOf(Blocks.AIR)) && worldIn.getBaseLightLevel(p_223317_3_, 0) > 8;
    }

    @Override
    public boolean canSpawn(WorldAccess worldIn, SpawnReason spawnReasonIn) {
        return AMEntityRegistry.rollSpawn(AMConfig.gorillaSpawnRolls, this.getRandom(), spawnReasonIn);
    }

    @Override
    public boolean isBreedingItem(ItemStack stack) {
        Item item = stack.getItem();
        return isTamed() && stack.isIn(AMTagRegistry.GORILLA_BREEDABLES);
    }

    @Override
    public int getLimitPerChunk() {
        return 8;
    }

    @Override
    public boolean spawnsTooManyForEachTry(int sizeIn) {
        return false;
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        if (this.isInvulnerableTo(source)) {
            return false;
        } else {
            var entity = source.getAttacker();
            this.setSitting(false);
            if (entity != null && this.isTamed() && !(entity instanceof PlayerEntity) && !(entity instanceof PersistentProjectileEntity)) {
                amount = (amount + 1.0F) / 2.0F;
            }
            return super.damage(source, amount);
        }
    }

    @Override
    protected void initGoals() {
        super.initGoals();
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new SitGoal(this));
        this.goalSelector.add(2, new MeleeAttackGoal(this, 1.2D, true));
        this.goalSelector.add(2, new GorillaAIFollowCaravan(this, 0.8D));
        this.goalSelector.add(3, new GorillaAIChargeLooker(this, 1.6D));
        this.goalSelector.add(4, new TameableAITempt(this, 1.1D, Ingredient.fromTag(AMTagRegistry.GORILLA_TAMEABLES), false));
        this.goalSelector.add(4, new AnimalAIRideParent(this, 1.25D));
        this.goalSelector.add(6, new AIWalkIdle(this, 0.8D));
        this.goalSelector.add(5, new GorillaAIForageLeaves(this));
        this.goalSelector.add(5, new AnimalMateGoal(this, 1.0D));
        this.goalSelector.add(7, new LookAtEntityGoal(this, PlayerEntity.class, 6.0F));
        this.goalSelector.add(8, new LookAroundGoal(this));
        this.targetSelector.add(1, new CreatureAITargetItems<>(this, false));
        this.targetSelector.add(1, (new RevengeGoal(this)).setGroupRevenge());
        this.targetSelector.add(1, new TrackOwnerAttackerGoal(this));
        this.targetSelector.add(2, new AttackWithOwnerGoal(this));
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return AMSoundRegistry.GORILLA_IDLE.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return AMSoundRegistry.GORILLA_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return AMSoundRegistry.GORILLA_HURT.get();
    }

    @Override
    public boolean tryAttack(Entity entityIn) {
        if (this.getAnimation() == NO_ANIMATION) {
            this.setAnimation(ANIMATION_ATTACK);
        }
        return true;
    }

    @Override
    public void travel(Vec3d vec3d) {
        if (this.isSitting()) {
            if (this.getNavigation().getCurrentPath() != null) {
                this.getNavigation().stop();
            }
            vec3d = Vec3d.ZERO;
        }
        super.travel(vec3d);
    }

    @Nullable
    @Override
    public EntityData initialize(ServerWorldAccess worldIn, LocalDifficulty difficultyIn, SpawnReason reason, @Nullable EntityData spawnDataIn, @Nullable NbtCompound dataTag) {
        if (spawnDataIn instanceof PassiveData lvt_6_1_) {
            if (lvt_6_1_.getSpawnedCount() == 0) {
                this.setSilverback(true);
            }
        } else {
            this.setSilverback(this.getRandom().nextBoolean());
        }

        return super.initialize(worldIn, difficultyIn, reason, spawnDataIn, dataTag);
    }

    @Nullable
    public EntityGorilla getNearestSilverback(WorldAccess world, double dist) {
        List<? extends EntityGorilla> list = world.getNonSpectatingEntities(this.getClass(), this.getBoundingBox().expand(dist, dist / 2, dist));
        if (list.isEmpty()) {
            return null;
        }
        EntityGorilla gorilla = null;
        double d0 = Double.MAX_VALUE;
        for (var gorrila2 : list) {
            if (gorrila2.isSilverback()) {
                double d1 = this.squaredDistanceTo(gorrila2);
                if (!(d1 > d0)) {
                    d0 = d1;
                    gorilla = gorrila2;
                }
            }
        }
        return gorilla;
    }

    @Override
    public EntityDimensions getDimensions(EntityPose poseIn) {
        return isSilverback() && !isBaby() ? SILVERBACK_SIZE.scaled(this.getScaleFactor()) : super.getDimensions(poseIn);
    }

    @Override
    public void updatePassengerPosition(Entity passenger, Entity.PositionUpdater moveFunc) {
        if (this.hasPassenger(passenger)) {
            this.setSitting(false);
            if (passenger instanceof EntityGorilla babyGorilla) {
                babyGorilla.setStanding(this.isStanding());
                babyGorilla.setSitting(this.isSitting());
                babyGorilla.bodyYaw = this.bodyYaw;
            }
            float sitAdd = -0.03F * this.sitProgress;
            float standAdd = -0.03F * this.standProgress;
            float radius = standAdd + sitAdd;
            float angle = (Maths.STARTING_ANGLE * this.bodyYaw);
            double extraX = radius * MathHelper.sin(MathHelper.PI + angle);
            double extraZ = radius * MathHelper.cos(angle);
            passenger.setPos(this.getX() + extraX, this.getY() + this.getMountedHeightOffset() + passenger.getHeightOffset(), this.getZ() + extraZ);
        }
    }

    @Override
    public double getMountedHeightOffset() {
        return (double) this.getHeight() * 0.65F * getGorillaScale() * (isSilverback() ? 0.75F : 1.0F);
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(SILVERBACK, false);
        this.dataTracker.startTracking(STANDING, false);
        this.dataTracker.startTracking(SITTING, false);
        this.dataTracker.startTracking(EATING, false);
    }

    public boolean isSilverback() {
        return this.dataTracker.get(SILVERBACK);
    }

    public void setSilverback(boolean silver) {
        this.dataTracker.set(SILVERBACK, silver);
    }

    public boolean isStanding() {
        return this.dataTracker.get(STANDING);
    }

    public void setStanding(boolean standing) {
        this.dataTracker.set(STANDING, standing);
    }

    @Override
    public boolean isSitting() {
        return this.dataTracker.get(SITTING);
    }

    @Override
    public void setSitting(boolean sit) {
        this.dataTracker.set(SITTING, sit);
    }

    public boolean isEating() {
        return this.dataTracker.get(EATING);
    }

    public void setEating(boolean eating) {
        this.dataTracker.set(EATING, eating);
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound compound) {
        super.writeCustomDataToNbt(compound);
        compound.putBoolean("Silverback", this.isSilverback());
        compound.putBoolean("Standing", this.isStanding());
        compound.putBoolean("GorillaSitting", this.isSitting());
        compound.putBoolean("ForcedToSit", this.forcedSit);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound compound) {
        super.readCustomDataFromNbt(compound);
        this.setSilverback(compound.getBoolean("Silverback"));
        this.setStanding(compound.getBoolean("Standing"));
        this.setSitting(compound.getBoolean("GorillaSitting"));
        this.forcedSit = compound.getBoolean("ForcedToSit");
    }

    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        ItemStack itemstack = player.getStackInHand(hand);
        Item item = itemstack.getItem();
        if (itemstack.getItem() == Items.NAME_TAG) {
            return super.interactMob(player, hand);
        }
        if (isTamed() && isTameableFood(itemstack) && this.getHealth() < this.getMaxHealth()) {
            this.heal(5);
            this.eat(player, hand, itemstack);
            this.emitGameEvent(GameEvent.EAT);
            this.playSound(SoundEvents.ENTITY_GENERIC_EAT, this.getSoundVolume(), this.getSoundPitch());
            return ActionResult.SUCCESS;
        }
        var type = super.interactMob(player, hand);
        var actionResult = itemstack.useOnEntity(player, this, hand);
        if (actionResult != ActionResult.SUCCESS && type != ActionResult.SUCCESS && isTamed() && isOwner(player) && !isBreedingItem(itemstack)) {
            if (this.isSitting()) {
                this.forcedSit = false;
                this.setSitting(false);
            } else {
                this.forcedSit = true;
                this.setSitting(true);
            }
            return ActionResult.SUCCESS;
        }
        return type;
    }

    @Override
    public Animation getAnimation() {
        return currentAnimation;
    }

    @Override
    public void setAnimation(Animation animation) {
        currentAnimation = animation;
        if (animation == ANIMATION_POUNDCHEST) {
            this.maxStandTime = 45;
            this.setStanding(true);
        }
        if (animation == ANIMATION_ATTACK) {
            this.maxStandTime = 10;
            this.setStanding(true);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.getStackInHand(Hand.MAIN_HAND).isEmpty() && this.canTargetItem(this.getStackInHand(Hand.MAIN_HAND))) {
            this.setEating(true);
            this.setSitting(true);
            this.setStanding(false);
        }
        if (isEating() && !this.canTargetItem(this.getStackInHand(Hand.MAIN_HAND))) {
            this.setEating(false);
            eatingTime = 0;
            if (!forcedSit) {
                this.setSitting(true);
            }
        }
        if (isEating()) {
            eatingTime++;
            if (!this.getMainHandStack().isIn(ItemTags.LEAVES)) {
                for (int i = 0; i < 3; i++) {
                    double d2 = this.random.nextGaussian() * 0.02D;
                    double d0 = this.random.nextGaussian() * 0.02D;
                    double d1 = this.random.nextGaussian() * 0.02D;
                    this.getWorld().addParticle(new ItemStackParticleEffect(ParticleTypes.ITEM, this.getStackInHand(Hand.MAIN_HAND)), this.getX() + (double) (this.random.nextFloat() * this.getWidth()) - (double) this.getWidth() * 0.5F, this.getY() + this.getHeight() * 0.5F + (double) (this.random.nextFloat() * this.getHeight() * 0.5F), this.getZ() + (double) (this.random.nextFloat() * this.getWidth()) - (double) this.getWidth() * 0.5F, d0, d1, d2);
                }
            }
            if (eatingTime % 5 == 0) {
                this.emitGameEvent(GameEvent.EAT);
                this.playSound(SoundEvents.ENTITY_PANDA_EAT, this.getSoundVolume(), this.getSoundPitch());
            }
            if (eatingTime > 100) {
                ItemStack stack = this.getStackInHand(Hand.MAIN_HAND);
                if (!stack.isEmpty()) {
                    this.heal(4);
                    if (isTameableFood(stack) && bananaThrowerID != null) {
                        if (getRandom().nextFloat() < 0.3F) {
                            this.setTamed(true);
                            this.setOwnerUuid(this.bananaThrowerID);
                            PlayerEntity player = getWorld().getPlayerByUuid(bananaThrowerID);
                            if (player instanceof ServerPlayerEntity) {
                                Criteria.TAME_ANIMAL.trigger((ServerPlayerEntity) player, this);
                            }
                            this.getWorld().sendEntityStatus(this, (byte) 7);
                        } else {
                            this.getWorld().sendEntityStatus(this, (byte) 6);
                        }
                    }
                    if (stack.getItem().hasRecipeRemainder()) {
                        this.dropItem(stack.getItem().getRecipeRemainder());
                    }
                    stack.decrement(1);
                }
                eatingTime = 0;
            }
        }
        prevSitProgress = sitProgress;
        prevStandProgress = standProgress;

        if (this.isSitting()) {
            if (sitProgress < 10F)
                sitProgress++;
        } else {
            if (sitProgress > 0F)
                sitProgress--;
        }

        if (this.isStanding()) {
            if (standProgress < 10F)
                standProgress++;
        } else {
            if (standProgress > 0F)
                standProgress--;
        }

        if (this.hasVehicle() && this.getVehicle() instanceof EntityGorilla mount) {
            if(!this.isBaby()){
                this.dismountVehicle();
            }else{
                this.setYaw( mount.bodyYaw);
                this.headYaw= mount.bodyYaw;
                this.bodyYaw = mount.bodyYaw;
            }
        }
        if (isStanding() && ++standingTime > maxStandTime) {
            this.setStanding(false);
            standingTime = 0;
            maxStandTime = 75 + random.nextInt(50);
        }
        if (!forcedSit && isSitting() && ++sittingTime > maxSitTime) {
            this.setSitting(false);
            sittingTime = 0;
            maxSitTime = 75 + random.nextInt(50);
        }
        if (!forcedSit && this.isSitting() && (this.getTarget() != null || this.isStanding()) && !this.isEating()) {
            this.setSitting(false);
        }
        if (!this.getWorld().isClient && this.getAnimation() == NO_ANIMATION && !this.isStanding() && !this.isSitting() && random.nextInt(1500) == 0) {
            maxSitTime = 300 + random.nextInt(250);
            this.setSitting(true);
        }
        if (this.forcedSit && !this.hasPassengers() && this.isTamed()) {
            this.setSitting(true);
        }
        if (sitProgress == 0 && poundChestCooldown <= 0 && this.isSilverback() && random.nextInt(800) == 0 && this.getAnimation() == NO_ANIMATION && !this.isSitting() && !this.isAiDisabled() && this.getMainHandStack().isEmpty()) {
            this.setAnimation(ANIMATION_POUNDCHEST);
        }
        if (!this.getWorld().isClient && this.getTarget() != null && this.getAnimation() == ANIMATION_ATTACK && this.getAnimationTick() == 10) {
            float f1 = this.getYaw() * MathHelper.RADIANS_PER_DEGREE;
            this.setVelocity(this.getVelocity().add(-MathHelper.sin(f1) * 0.02F, 0.0D, MathHelper.cos(f1) * 0.02F));
            getTarget().takeKnockback(1F, getTarget().getX() - this.getX(), getTarget().getZ() - this.getZ());
            this.getTarget().damage(this.getDamageSources().mobAttack(this), (float) this.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE).getBaseValue());
        }
        if (!hasSilverbackAttributes && isSilverback() && !isBaby()) {
            hasSilverbackAttributes = true;
            calculateDimensions();
            this.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(50F);
            this.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE).setBaseValue(10F);
            this.heal(50F);
        }
        if (hasSilverbackAttributes && !isSilverback() && !isBaby()) {
            hasSilverbackAttributes = false;
            calculateDimensions();
            this.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(30F);
            this.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE).setBaseValue(8F);
            this.heal(30F);
        }
        if(poundChestCooldown > 0){
            poundChestCooldown--;
        }
        AnimationHandler.INSTANCE.updateAnimations(this);
    }

    @Nullable
    @Override
    public LivingEntity getControllingPassenger() {
        return null;
    }

    @Override
    public EntityNavigation getNavigation() {
        return this.navigation;
    }

    @Nullable
    @Override
    public Entity getControllingVehicle() {
        return this.getVehicle() instanceof EntityGorilla ? null : super.getControllingVehicle();
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
    public boolean canTargetItem(ItemStack stack) {
        return stack.isIn(AMTagRegistry.GORILLA_FOODSTUFFS);
    }

    @Override
    public void onGetItem(ItemEntity targetEntity) {
        ItemStack duplicate = targetEntity.getStack().copy();
        duplicate.setCount(1);
        if (!this.getStackInHand(Hand.MAIN_HAND).isEmpty() && !this.getWorld().isClient) {
            this.dropStack(this.getStackInHand(Hand.MAIN_HAND), 0.0F);
        }
        this.setStackInHand(Hand.MAIN_HAND, duplicate);
        Entity thrower = targetEntity.getOwner();
        if (EntityGorilla.isTameableFood(targetEntity.getStack()) && thrower != null && !this.isTamed()) {
            bananaThrowerID = thrower.getUuid();
        }
    }

    @Override
    public Animation[] getAnimations() {
        return new Animation[]{ANIMATION_BREAKBLOCK_R, ANIMATION_BREAKBLOCK_L, ANIMATION_POUNDCHEST, ANIMATION_ATTACK};
    }

    @Nullable
    @Override
    public PassiveEntity createChild(ServerWorld world, PassiveEntity entity) {
        return AMEntityRegistry.GORILLA.get().create(world);
    }

    public void leaveCaravan() {
        if (this.caravanHead != null) {
            this.caravanHead.caravanTail = null;
        }

        this.caravanHead = null;
    }

    public void joinCaravan(EntityGorilla caravanHeadIn) {
        this.caravanHead = caravanHeadIn;
        this.caravanHead.caravanTail = this;
    }

    public boolean hasCaravanTrail() {
        return this.caravanTail != null;
    }

    public boolean inCaravan() {
        return this.caravanHead != null;
    }

    @Nullable
    public EntityGorilla getCaravanHead() {
        return this.caravanHead;
    }

    public float getGorillaScale() {
        return isBaby() ? 0.5F : isSilverback() ? 1.3F : 1.0F;
    }

    public boolean isDonkeyKong() {
        String s = Formatting.strip(this.getName().getString());
        return s != null && (s.toLowerCase().contains("donkey") && s.toLowerCase().contains("kong") || s.equalsIgnoreCase("dk"));
    }

    public boolean isFunkyKong() {
        String s = Formatting.strip(this.getName().getString());
        return s != null && (s.toLowerCase().contains("funky") && s.toLowerCase().contains("kong"));
    }

    @Override
    public EntityView method_48926() {
        return getWorld();
    }

    private class AIWalkIdle extends WanderAroundGoal {
        public AIWalkIdle(EntityGorilla entityGorilla, double v) {
            super(entityGorilla, v);
        }

        @Override
        public boolean canStart() {
            this.chance = EntityGorilla.this.isSilverback() ? 10 : 120;
            return super.canStart();
        }

        @Nullable
        protected Vec3d getWanderTarget() {
            return FuzzyTargeting.find(this.mob, EntityGorilla.this.isSilverback() ? 25 : 10, 7);
        }

    }
}
