package com.github.alexthe666.alexsmobs.entity;

import com.github.alexthe666.alexsmobs.config.AMConfig;
import com.github.alexthe666.alexsmobs.entity.ai.*;
import com.github.alexthe666.alexsmobs.misc.AMBlockPos;
import com.github.alexthe666.alexsmobs.registry.AMEntityRegistry;
import com.github.alexthe666.alexsmobs.registry.AMSoundRegistry;
import com.github.alexthe666.alexsmobs.registry.AMTagRegistry;
import com.iafenvoy.uranus.animation.Animation;
import com.iafenvoy.uranus.animation.AnimationHandler;
import com.iafenvoy.uranus.animation.IAnimatedEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.NoPenaltyTargeting;
import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ItemStackParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.EntityView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.event.GameEvent;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class EntityRaccoon extends TameableEntity implements IAnimatedEntity, IFollower, ITargetsDroppedItems, ILootsChests {

    private static final TrackedData<Boolean> STANDING = DataTracker.registerData(EntityRaccoon.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> SITTING = DataTracker.registerData(EntityRaccoon.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> BEGGING = DataTracker.registerData(EntityRaccoon.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> WASHING = DataTracker.registerData(EntityRaccoon.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Optional<BlockPos>> WASH_POS = DataTracker.registerData(EntityRaccoon.class, TrackedDataHandlerRegistry.OPTIONAL_BLOCK_POS);
    private static final TrackedData<Integer> COMMAND = DataTracker.registerData(EntityRaccoon.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> CARPET_COLOR = DataTracker.registerData(EntityRaccoon.class, TrackedDataHandlerRegistry.INTEGER);
    public float prevStandProgress;
    public float standProgress;
    public float prevBegProgress;
    public float begProgress;
    public float prevWashProgress;
    public float washProgress;
    public float prevSitProgress;
    public float sitProgress;
    public int maxStandTime = 75;
    private int standingTime = 0;
    private int stealCooldown = 0;
    public int lookForWaterBeforeEatingTimer = 0;
    private int animationTick;
    private Animation currentAnimation;
    private int pickupItemCooldown = 0;
    @Nullable
    private UUID eggThrowerUUID = null;
    public boolean forcedSit = false;
    public static final Animation ANIMATION_ATTACK = Animation.create(12);
    private static final TargetPredicate VILLAGER_STEAL_PREDICATE = TargetPredicate.createAttackable().setBaseMaxDistance(20.0D).ignoreVisibility();
    private static final TargetPredicate IRON_GOLEM_PREDICATE = TargetPredicate.createAttackable().setBaseMaxDistance(20.0D).ignoreVisibility();

    public EntityRaccoon(EntityType<? extends EntityRaccoon> type, World world) {
        super(type, world);
        this.setPathfindingPenalty(PathNodeType.WATER_BORDER, 0.0F);
    }

    @Override
    protected float getBaseMovementSpeedMultiplier() {
        return 0.98F;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return AMSoundRegistry.RACCOON_IDLE.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return AMSoundRegistry.RACCOON_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return AMSoundRegistry.RACCOON_HURT.get();
    }

    @Override
    public boolean canSpawn(WorldAccess worldIn, SpawnReason spawnReasonIn) {
        return AMEntityRegistry.rollSpawn(AMConfig.raccoonSpawnRolls, this.getRandom(), spawnReasonIn);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new SitGoal(this));
        this.goalSelector.add(1, new AnimalMateGoal(this, 1.0D));
        this.goalSelector.add(2, new RaccoonAIWash(this));
        this.goalSelector.add(3, new TameableAIFollowOwner(this, 1.3D, 10.0F, 2.0F, false));
        this.goalSelector.add(4, new SwimGoal(this));
        this.goalSelector.add(5, new PounceAtTargetGoal(this, 0.4F));
        this.goalSelector.add(6, new MeleeAttackGoal(this, 1.1D, true));
        this.goalSelector.add(7, new AnimalAILootChests(this, 16));
        this.goalSelector.add(8, new FollowParentGoal(this, 1.1D));
        this.goalSelector.add(9, new RaccoonAIBeg(this, 0.65D));
        this.goalSelector.add(10, new AnimalAIPanicBaby(this, 1.25D));
        this.goalSelector.add(11, new AIStealFromVillagers(this));
        this.goalSelector.add(12, new StrollGoal(200));
        this.goalSelector.add(13, new TameableAIDestroyTurtleEggs(this, 1.0D, 3));
        this.goalSelector.add(14, new AnimalAIWanderRanged(this, 120, 1.0D, 14, 7));
        this.goalSelector.add(15, new LookAtEntityGoal(this, PlayerEntity.class, 15.0F));
        this.goalSelector.add(15, new LookAroundGoal(this));
        this.targetSelector.add(1, (new AnimalAIHurtByTargetNotBaby(this)));
        this.targetSelector.add(1, new CreatureAITargetItems<>(this, false));
        this.targetSelector.add(3, new TrackOwnerAttackerGoal(this));
        this.targetSelector.add(4, new AttackWithOwnerGoal(this));
    }

    @Override
    public boolean isTeammate(Entity entityIn) {
        if (entityIn instanceof EntityBlueJay jay) {
            return jay.getRaccoonUUID() != null && jay.getRaccoonUUID().equals(this.getUuid());
        }
        if (this.isTamed()) {
            var livingentity = this.getOwner();
            if (entityIn == livingentity) {
                return true;
            }
            if (entityIn instanceof TameableEntity) {
                return ((TameableEntity) entityIn).isOwner(livingentity);
            }
            if (livingentity != null) {
                return livingentity.isTeammate(entityIn);
            }
        }

        return super.isTeammate(entityIn);
    }

    @Override
    public boolean tryAttack(Entity entityIn) {
        if (this.getAnimation() == NO_ANIMATION) {
            this.setAnimation(ANIMATION_ATTACK);
        }
        return true;
    }

    @Override
    protected void dropInventory() {
        super.dropInventory();
        if (this.getColor() != null) {
            if (!this.getWorld().isClient) {
                this.dropItem(this.getCarpetItemBeingWorn());
            }
            this.setColor(null);
        }
    }

    @Nullable
    public DyeColor getColor() {
        int lvt_1_1_ = this.dataTracker.get(CARPET_COLOR);
        return lvt_1_1_ == -1 ? null : DyeColor.byId(lvt_1_1_);
    }

    public void setColor(@Nullable DyeColor color) {
        this.dataTracker.set(CARPET_COLOR, color == null ? -1 : color.getId());
    }

    public Item getCarpetItemBeingWorn() {
        if (this.getColor() != null) {
            return EntityElephant.DYE_COLOR_ITEM_MAP.get(this.getColor());
        }
        return Items.AIR;
    }

    @Override
    public boolean isBreedingItem(ItemStack stack) {
        return stack.isIn(AMTagRegistry.RACCOON_BREEDABLES);
    }

    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        ItemStack itemstack = player.getStackInHand(hand);
        ActionResult type = super.interactMob(player, hand);
        boolean owner = this.isTamed() && isOwner(player);
        if (itemstack.isIn(AMTagRegistry.RACCOON_TEAMING_FOODS) && bondWithBlueJays(player.getUuid())) {
            this.eat(player, hand, itemstack);
            this.getWorld().sendEntityStatus(this, (byte) 93);
            return ActionResult.SUCCESS;
        } else if (this.isTamed() && !this.getMainHandStack().isEmpty()) {
            if (!this.getWorld().isClient) {
                this.dropStack(this.getMainHandStack().copy());
            }
            this.setStackInHand(Hand.MAIN_HAND, ItemStack.EMPTY);
            pickupItemCooldown = 60;
            return ActionResult.SUCCESS;
        } else if (owner && itemstack.isIn(ItemTags.WOOL_CARPETS)) {
            DyeColor color = EntityElephant.getCarpetColor(itemstack);
            if (color != this.getColor()) {
                if (this.getColor() != null) {
                    this.dropItem(this.getCarpetItemBeingWorn());
                }
                this.emitGameEvent(GameEvent.ENTITY_INTERACT);
                this.playSound(SoundEvents.ENTITY_LLAMA_SWAG, 1.0F, (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F);
                itemstack.decrement(1);
                this.setColor(color);
                return ActionResult.SUCCESS;
            }
            return ActionResult.PASS;
            //FIXME
//        } else if (owner && this.getColor() != null && itemstack.is(Tags.Items.SHEARS)) {
//            this.emitGameEvent(GameEvent.ENTITY_INTERACT);
//            this.playSound(SoundEvents.ENTITY_SHEEP_SHEAR, 1.0F, (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F);
//            if (this.getColor() != null) {
//                this.spawnAtLocation(this.getCarpetItemBeingWorn());
//            }
//            this.setColor(null);
//            return ActionResult.SUCCESS;
        } else if (isTamed() && isRaccoonFood(itemstack) && !isBreedingItem(itemstack) && this.getHealth() < this.getMaxHealth()) {
            if (this.getMainHandStack().isEmpty()) {
                var copy = itemstack.copy();
                copy.setCount(1);
                this.setStackInHand(Hand.MAIN_HAND, copy);
                this.onEatItem();
                if (itemstack.getItem().hasRecipeRemainder()) {
                    this.dropItem(itemstack.getItem().getRecipeRemainder());
                }
                if (!player.isCreative()) {
                    itemstack.decrement(1);
                }
                this.setStackInHand(Hand.MAIN_HAND, ItemStack.EMPTY);
            } else {
                this.emitGameEvent(GameEvent.EAT);
                this.playSound(SoundEvents.ENTITY_GENERIC_EAT, this.getSoundVolume(), this.getSoundPitch());
                this.heal(5);
            }
            this.eat(player, hand, itemstack);
            return ActionResult.SUCCESS;
        }
        var actionResult = itemstack.useOnEntity(player, this, hand);
        if (actionResult != ActionResult.SUCCESS && type != ActionResult.SUCCESS && isTamed() && isOwner(player) && !isRaccoonFood(itemstack)) {
            if (!player.isSneaking()) {
                this.setCommand(this.getCommand() + 1);
                if (this.getCommand() == 3) {
                    this.setCommand(0);
                }
                player.sendMessage(Text.translatable("entity.alexsmobs.all.command_" + this.getCommand(), this.getName()), true);
                boolean sit = this.getCommand() == 2;
                if (sit) {
                    this.forcedSit = true;
                    this.setSitting(true);
                } else {
                    this.forcedSit = false;
                    this.setSitting(false);
                }
                return ActionResult.SUCCESS;
            }
        }
        return type;
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound compound) {
        super.writeCustomDataToNbt(compound);
        compound.putBoolean("RacSitting", this.isSitting());
        compound.putBoolean("ForcedToSit", this.forcedSit);
        compound.putInt("RacCommand", this.getCommand());
        compound.putInt("Carpet", this.dataTracker.get(CARPET_COLOR));
        compound.putInt("StealCooldown", stealCooldown);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound compound) {
        super.readCustomDataFromNbt(compound);
        this.setSitting(compound.getBoolean("RacSitting"));
        this.forcedSit = compound.getBoolean("ForcedToSit");
        this.setCommand(compound.getInt("RacCommand"));
        this.dataTracker.set(CARPET_COLOR, compound.getInt("Carpet"));
        this.stealCooldown = compound.getInt("StealCooldown");

    }

    public void setCommand(int command) {
        this.dataTracker.set(COMMAND, command);
    }

    public int getCommand() {
        return this.dataTracker.get(COMMAND);
    }

    @Override
    public void setSitting(boolean sit) {
        this.dataTracker.set(SITTING, sit);
    }

    @Override
    public boolean isSitting() {
        return this.dataTracker.get(SITTING);
    }

    public static boolean isRaccoonFood(ItemStack stack) {
        return stack.isFood() || stack.isIn(AMTagRegistry.RACCOON_FOODSTUFFS);
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createLivingAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 9D)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 2.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.25F)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32.0D);
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        if (this.isInvulnerableTo(source)) {
            return false;
        } else {
            var entity = source.getAttacker();
            this.setSitting(false);
            if (entity != null && this.isTamed() && !(entity instanceof PlayerEntity) && !(entity instanceof PersistentProjectileEntity)) {
                amount = (amount + 1.0F) / 4.0F;
            }
            return super.damage(source, amount);
        }
    }

    @Override
    protected void updateGoalControls() {
        boolean flag = !(this.getControllingPassenger() instanceof MobEntity);
        boolean flag1 = !(this.getVehicle() instanceof BoatEntity);
        boolean flag2 = this.getFirstPassenger() instanceof EntityBlueJay;
        this.goalSelector.setControlEnabled(Goal.Control.MOVE, flag || flag2);
        this.goalSelector.setControlEnabled(Goal.Control.JUMP, flag && flag1 || flag2);
        this.goalSelector.setControlEnabled(Goal.Control.LOOK, flag || flag2);
    }

    @Override
    public void tick() {
        super.tick();
        this.prevStandProgress = this.standProgress;
        this.prevBegProgress = this.begProgress;
        this.prevWashProgress = this.washProgress;
        this.prevSitProgress = this.sitProgress;

        if (this.isStanding()) {
            if (standProgress < 5F)
                standProgress++;
        } else {
            if (standProgress > 0F)
                standProgress--;
        }

        if (this.isBegging()) {
            if (begProgress < 5F)
                begProgress++;
        } else {
            if (begProgress > 0F)
                begProgress--;
        }

        if (this.isWashing()) {
            if (washProgress < 5F)
                washProgress++;
        } else {
            if (washProgress > 0F)
                washProgress--;
        }

        if (this.isSitting()) {
            if (sitProgress < 5F)
                sitProgress++;
        } else {
            if (sitProgress > 0F)
                sitProgress--;
        }

        if (isStanding() && ++standingTime > maxStandTime) {
            this.setStanding(false);
            standingTime = 0;
            maxStandTime = 75 + random.nextInt(50);
        }
        if (!this.getWorld().isClient) {
            if (lookForWaterBeforeEatingTimer > 0) {
                lookForWaterBeforeEatingTimer--;
            } else if (!isWashing() && canTargetItem(this.getMainHandStack())) {
                onEatItem();
                if (this.getMainHandStack().getItem().hasRecipeRemainder()) {
                    this.dropItem(this.getMainHandStack().getItem().getRecipeRemainder());
                }
                this.getMainHandStack().decrement(1);
            }
        }
        if (isWashing()) {
            if (getWashPos() != null) {
                var washingPos = getWashPos();
                if (this.squaredDistanceTo(washingPos.getX() + 0.5D, washingPos.getY() + 0.5D, washingPos.getZ() + 0.5D) < 3) {
                    for (int j = 0; (float) j < 4; ++j) {
                        double d2 = (this.random.nextDouble());
                        double d3 = (this.random.nextDouble());
                        Vec3d vector3d = this.getVelocity();

                        this.getWorld().addParticle(ParticleTypes.SPLASH, washingPos.getX() + d2, (double) (washingPos.getY() + 0.8F), washingPos.getZ() + d3, vector3d.x, vector3d.y, vector3d.z);
                    }
                } else {
                    setWashing(false);
                }
            }
        }
        if (!this.getWorld().isClient && this.getTarget() != null && this.canSee(this.getTarget()) && this.distanceTo(this.getTarget()) < 4 && this.getAnimation() == ANIMATION_ATTACK && this.getAnimationTick() == 5) {
            float f1 = this.getYaw() * MathHelper.RADIANS_PER_DEGREE;
            this.setVelocity(this.getVelocity().add(-MathHelper.sin(f1) * -0.06F, 0.0D, MathHelper.cos(f1) * -0.06F));
            this.getTarget().takeKnockback(0.35F, getTarget().getX() - this.getX(), getTarget().getZ() - this.getZ());
            this.getTarget().damage(this.getDamageSources().mobAttack(this), (float) this.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE).getBaseValue());
        }
        if (stealCooldown > 0) {
            stealCooldown--;
        }
        if (pickupItemCooldown > 0) {
            pickupItemCooldown--;
        }
        AnimationHandler.INSTANCE.updateAnimations(this);
    }

    public void onEatItem() {
        this.heal(10);
        this.getWorld().sendEntityStatus(this, (byte) 92);
        this.emitGameEvent(GameEvent.EAT);
        this.playSound(SoundEvents.ENTITY_GENERIC_EAT, this.getSoundVolume(), this.getSoundPitch());
    }

    public void postWashItem(ItemStack stack) {
        if (stack.isIn(AMTagRegistry.RACCOON_TAMEABLES) && eggThrowerUUID != null && !this.isTamed()) {
            if (getRandom().nextFloat() < 0.3F) {
                this.setTamed(true);
                this.setOwnerUuid(eggThrowerUUID);
                PlayerEntity player = getWorld().getPlayerByUuid(eggThrowerUUID);
                if (player instanceof ServerPlayerEntity) {
                    Criteria.TAME_ANIMAL.trigger((ServerPlayerEntity) player, this);
                }
                this.getWorld().sendEntityStatus(this, (byte) 7);
            } else {
                this.getWorld().sendEntityStatus(this, (byte) 6);
            }
        }
    }

    @Environment(EnvType.CLIENT)
    public void handleStatus(byte id) {
        if (id == 92) {
            for (int i = 0; i < 6 + random.nextInt(3); i++) {
                double d2 = this.random.nextGaussian() * 0.02D;
                double d0 = this.random.nextGaussian() * 0.02D;
                double d1 = this.random.nextGaussian() * 0.02D;
                this.getWorld().addParticle(new ItemStackParticleEffect(ParticleTypes.ITEM, this.getStackInHand(Hand.MAIN_HAND)), this.getX() + (double) (this.random.nextFloat() * this.getWidth()) - (double) this.getWidth() * 0.5F, this.getY() + this.getHeight() * 0.5F + (double) (this.random.nextFloat() * this.getHeight() * 0.5F), this.getZ() + (double) (this.random.nextFloat() * this.getWidth()) - (double) this.getWidth() * 0.5F, d0, d1, d2);
            }
        } else if (id == 93) {
            for (int i = 0; i < 6 + random.nextInt(3); i++) {
                double d2 = this.random.nextGaussian() * 0.02D;
                double d0 = this.random.nextGaussian() * 0.02D;
                double d1 = this.random.nextGaussian() * 0.02D;
                this.getWorld().addParticle(new ItemStackParticleEffect(ParticleTypes.ITEM, new ItemStack(Items.GLOW_BERRIES)), this.getX() + (double) (this.random.nextFloat() * this.getWidth()) - (double) this.getWidth() * 0.5F, this.getY() + this.getHeight() * 0.5F + (double) (this.random.nextFloat() * this.getHeight() * 0.5F), this.getZ() + (double) (this.random.nextFloat() * this.getWidth()) - (double) this.getWidth() * 0.5F, d0, d1, d2);
            }
        } else {
            super.handleStatus(id);
        }
    }

    //FIXME
//    public boolean canBeRiddenUnderFluidType(FluidType type, Entity rider) {
//        return true;
//    }

    public boolean isStanding() {
        return this.dataTracker.get(STANDING);
    }

    public void setStanding(boolean standing) {
        this.dataTracker.set(STANDING, standing);
    }

    public boolean isBegging() {
        return this.dataTracker.get(BEGGING);
    }

    public void setBegging(boolean begging) {
        this.dataTracker.set(BEGGING, begging);
    }

    public boolean isWashing() {
        return this.dataTracker.get(WASHING);
    }

    public void setWashing(boolean washing) {
        this.dataTracker.set(WASHING, washing);
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(STANDING, false);
        this.dataTracker.startTracking(SITTING, false);
        this.dataTracker.startTracking(BEGGING, false);
        this.dataTracker.startTracking(WASHING, false);
        this.dataTracker.startTracking(CARPET_COLOR, -1);
        this.dataTracker.startTracking(COMMAND, 0);
        this.dataTracker.startTracking(WASH_POS, Optional.empty());
    }


    public BlockPos getWashPos() {
        return this.dataTracker.get(WASH_POS).orElse(null);
    }

    public void setWashPos(BlockPos washingPos) {
        this.dataTracker.set(WASH_POS, Optional.ofNullable(washingPos));
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
        if (animation == ANIMATION_ATTACK) {
            maxStandTime = 15;
            this.setStanding(true);
        }
    }

    @Override
    public Animation[] getAnimations() {
        return new Animation[]{ANIMATION_ATTACK};
    }


    @Nullable
    @Override
    public PassiveEntity createChild(ServerWorld world, PassiveEntity entity) {
        return AMEntityRegistry.RACCOON.get().create(world);
    }

    @Override
    public void travel(Vec3d vec3d) {
        if (this.isSitting() || this.isWashing()) {
            if (this.getNavigation().getCurrentPath() != null) {
                this.getNavigation().stop();
            }
            vec3d = Vec3d.ZERO;
        }
        super.travel(vec3d);
    }

    @Override
    public boolean shouldFollow() {
        return getCommand() == 1;
    }

    @Override
    public boolean canTargetItem(ItemStack stack) {
        return isRaccoonFood(stack) && pickupItemCooldown == 0;
    }

    @Override
    public void onGetItem(ItemEntity e) {
        lookForWaterBeforeEatingTimer = 100;
        var duplicate = e.getStack().copy();
        duplicate.setCount(1);
        if (!this.getStackInHand(Hand.MAIN_HAND).isEmpty() && !this.getWorld().isClient) {
            this.dropStack(this.getStackInHand(Hand.MAIN_HAND), 0.0F);
        }
        Entity thrower = e.getOwner();
        if (e.getStack().isIn(AMTagRegistry.RACCOON_TEAMING_FOODS) && thrower != null && bondWithBlueJays(thrower.getUuid())) {
            this.getWorld().sendEntityStatus(this, (byte) 93);
        } else {
            this.setStackInHand(Hand.MAIN_HAND, duplicate);
        }
        if (e.getStack().isIn(AMTagRegistry.RACCOON_TAMEABLES) && thrower != null) {
            eggThrowerUUID = thrower.getUuid();
        } else {
            eggThrowerUUID = null;
        }
    }

    @Override
    public double getMountedHeightOffset() {
        return (double) this.getHeight() * 0.45D;
    }

    private boolean bondWithBlueJays(UUID uuid) {
        var allyBox = this.getBoundingBox().expand(48);
        boolean any = false;
        for (var entity : this.getWorld().getNonSpectatingEntities(EntityBlueJay.class, allyBox)) {
            if (entity.getFeedTime() > 0 && entity.getLastFeederUUID() != null && entity.getLastFeederUUID().equals(uuid)) {
                entity.setRaccoon(this);
                entity.setFeedTime(0);
                any = true;
            }
        }
        return any;
    }

    @Override
    public boolean isLootable(Inventory inventory) {
        for (int i = 0; i < inventory.size(); i++) {
            if (shouldLootItem(inventory.getStack(i))) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean shouldLootItem(ItemStack stack) {
        return isRaccoonFood(stack);
    }

    public boolean isHoldingSugar() {
        return this.getMainHandStack().isIn(AMTagRegistry.RACOON_DISSOLVES);
    }

    @Override
    public EntityView method_48926() {
        return getWorld();
    }

    class StrollGoal extends MoveThroughVillageGoal {
        public StrollGoal(int p_i50726_3_) {
            super(EntityRaccoon.this, 1.0D, true, p_i50726_3_, () -> false);
        }

        @Override
        public void start() {
            super.start();
        }

        @Override
        public boolean canStart() {
            return super.canStart() && this.canFoxMove();
        }

        @Override
        public boolean shouldContinue() {
            return super.shouldContinue() && this.canFoxMove();
        }

        private boolean canFoxMove() {
            return !EntityRaccoon.this.isWashing() && !EntityRaccoon.this.isSitting() && EntityRaccoon.this.getTarget() == null;
        }
    }

    public BlockPos getLightPosition() {
        BlockPos pos = AMBlockPos.fromVec3(this.getPos());
        if (!getWorld().getBlockState(pos).isOpaque()) {
            return pos.up();
        }
        return pos;
    }

    public boolean isRigby() {
        final String name = Formatting.strip(this.getName().getString());
        if (name == null)
            return false;

        final String lowercaseName = name.toLowerCase(Locale.ROOT);
        return lowercaseName.contains("rigby");
    }

    private class AIStealFromVillagers extends Goal {
        EntityRaccoon raccoon;
        MerchantEntity target;
        int golemCheckTime = 0;
        int cooldown = 0;
        int fleeTime = 0;

        private AIStealFromVillagers(EntityRaccoon raccoon) {
            this.raccoon = raccoon;
            this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
        }

        @Override
        public boolean canStart() {
            if (cooldown > 0) {
                cooldown--;
                return false;
            } else if (raccoon != null && raccoon.stealCooldown == 0 && raccoon.getMainHandStack() != null && raccoon.getMainHandStack().isEmpty()) {
                MerchantEntity villager = getNearbyVillagers();
                if (!isGolemNearby() && villager != null) {
                    target = villager;
                }
                cooldown = 150;
                return target != null;
            }
            return false;
        }

        @Override
        public boolean shouldContinue() {
            return target != null && raccoon != null;
        }

        @Override
        public void stop() {
            target = null;
            cooldown = 200 + random.nextInt(200);
            golemCheckTime = 0;
            fleeTime = 0;
        }

        @Override
        public void tick() {
            if (target != null) {
                golemCheckTime++;
                if (fleeTime > 0) {
                    fleeTime--;
                    if (raccoon.getNavigation().isIdle()) {
                        var fleevec = NoPenaltyTargeting.findFrom(raccoon, 16, 7, raccoon.getPos());
                        if (fleevec != null) {
                            raccoon.getNavigation().startMovingTo(fleevec.x, fleevec.y, fleevec.z, 1.3F);
                        }
                    }
                    if (fleeTime == 0) {
                        stop();
                    }
                } else {
                    raccoon.getNavigation().startMovingTo(target, 1.0D);
                    if (raccoon.distanceTo(target) < 1.7F) {
                        raccoon.setStanding(true);
                        raccoon.maxStandTime = 15;
                        var offers = target.getOffers();
                        if (offers == null || offers.isEmpty() || offers.size() < 1) {
                            stop();
                        } else {
                            var offer = offers.get(offers.size() <= 1 ? 0 : raccoon.getRandom().nextInt(offers.size() - 1));
                            if (offer != null) {
                                var stealStack = offer.getSellItem().getItem() == Items.EMERALD ? offer.getOriginalFirstBuyItem() : offer.getSellItem();
                                if (stealStack.isEmpty()) {
                                    stop();
                                } else {
                                    offer.use();
                                    var copy = stealStack.copy();
                                    copy.setCount(1);
                                    raccoon.setStackInHand(Hand.MAIN_HAND, copy);
                                    fleeTime = 60 + random.nextInt(60);
                                    raccoon.getNavigation().stop();
                                    lookForWaterBeforeEatingTimer = 120 + random.nextInt(60);
                                    target.damage(getDamageSources().generic(), 0);
                                    raccoon.stealCooldown = 24000 + random.nextInt(48000);
                                }
                            }
                        }
                    }
                    if (golemCheckTime % 30 == 0 && random.nextBoolean() && isGolemNearby()) {
                        stop();
                    }
                }
            }
        }

        @Nullable
        private boolean isGolemNearby() {
            List<IronGolemEntity> lvt_1_1_ = raccoon.getWorld().getTargets(IronGolemEntity.class, IRON_GOLEM_PREDICATE, raccoon, raccoon.getBoundingBox().expand(25.0D));
            return !lvt_1_1_.isEmpty();
        }

        @Nullable
        private MerchantEntity getNearbyVillagers() {
            List<MerchantEntity> lvt_1_1_ = raccoon.getWorld().getTargets(MerchantEntity.class, VILLAGER_STEAL_PREDICATE, raccoon, raccoon.getBoundingBox().expand(20.0D));
            double lvt_2_1_ = 10000;
            MerchantEntity lvt_4_1_ = null;

            for (var lvt_6_1_ : lvt_1_1_) {
                if (lvt_6_1_.getHealth() > 2.0F && !lvt_6_1_.getOffers().isEmpty() && raccoon.squaredDistanceTo(lvt_6_1_) < lvt_2_1_) {
                    lvt_4_1_ = lvt_6_1_;
                    lvt_2_1_ = raccoon.squaredDistanceTo(lvt_6_1_);
                }
            }

            return lvt_4_1_;
        }

    }
}
