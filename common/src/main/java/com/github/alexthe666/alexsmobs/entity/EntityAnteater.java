package com.github.alexthe666.alexsmobs.entity;


import com.github.alexthe666.alexsmobs.config.AMConfig;
import com.github.alexthe666.alexsmobs.entity.ai.*;
import com.github.alexthe666.alexsmobs.registry.AMEntityRegistry;
import com.github.alexthe666.alexsmobs.registry.AMSoundRegistry;
import com.github.alexthe666.alexsmobs.registry.AMTagRegistry;
import com.iafenvoy.uranus.animation.Animation;
import com.iafenvoy.uranus.animation.AnimationHandler;
import com.iafenvoy.uranus.animation.IAnimatedEntity;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.Angerable;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.recipe.Ingredient;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TimeHelper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.intprovider.UniformIntProvider;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.event.GameEvent;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Predicate;

public class EntityAnteater extends AnimalEntity implements Angerable, IAnimatedEntity, ITargetsDroppedItems {

    public static final Animation ANIMATION_SLASH_R = Animation.create(20);
    public static final Animation ANIMATION_TOUNGE_IDLE = Animation.create(10);
    public static final Animation ANIMATION_SLASH_L = Animation.create(20);
    private static final TrackedData<Boolean> STANDING = DataTracker.registerData(EntityAnteater.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> LEANING_DOWN = DataTracker.registerData(EntityAnteater.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> ANT_ON_TONGUE = DataTracker.registerData(EntityAnteater.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Integer> ANGER_TIME = DataTracker.registerData(EntityAnteater.class, TrackedDataHandlerRegistry.INTEGER);
    public float prevStandProgress;
    public float standProgress;
    public float prevTongueProgress;
    public float tongueProgress;
    public float prevLeaningProgress;
    public float leaningProgress;
    public int eatAntCooldown = 0;
    public int ticksAntOnTongue = 0;
    private int animationTick;
    private Animation currentAnimation;
    private int maxStandTime = 75;
    private int standingTime = 0;
    private int antsEatenRecently = 0;
    private int heldItemTime;
    private UUID lastHurtBy;
    private static final UniformIntProvider ANGRY_TIMER = TimeHelper.betweenSeconds(30, 60);

    public EntityAnteater(EntityType<EntityAnteater> type, World world) {
        super(type, world);
        this.setStepHeight(1);
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createLivingAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 20.0D)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 6D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.25F)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32.0D)
                .add(EntityAttributes.GENERIC_ATTACK_KNOCKBACK, 0);
    }

    public static boolean canAnteaterSpawn(EntityType<? extends AnimalEntity> animal, WorldAccess worldIn, SpawnReason reason, BlockPos pos, Random random) {
        return worldIn.getBaseLightLevel(pos, 0) > 8;
    }

    @Override
    public boolean canSpawn(WorldAccess worldIn, SpawnReason spawnReasonIn) {
        return AMEntityRegistry.rollSpawn(AMConfig.anteaterSpawnRolls, this.getRandom(), spawnReasonIn);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(1, new SwimGoal(this));
        this.goalSelector.add(2, new AIMelee());
        this.goalSelector.add(3, new AnteaterAIRaidNest(this));
        this.goalSelector.add(4, new AnimalMateGoal(this, 1D));
        this.goalSelector.add(5, new AnimalAIRideParent(this, 1.25D));
        this.goalSelector.add(6, new TemptGoal(this, 1.2D, Ingredient.fromTag(AMTagRegistry.ANTEATER_FOODSTUFFS), false));
        this.goalSelector.add(7, new AnimalAIWanderRanged(this, 110, 1.0D, 10, 7));
        this.goalSelector.add(8, new LookAtEntityGoal(this, PlayerEntity.class, 10.0F));
        this.goalSelector.add(9, new LookAroundGoal(this));
        this.targetSelector.add(1, new CreatureAITargetItems<>(this, false, false, 25, 16));
        this.targetSelector.add(2, (new AnimalAIHurtByTargetNotBaby(this)));
        this.targetSelector.add(3, new AITargetAnts());
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return super.isInvulnerableTo(source) || source.getSource() != null && source.getSource() instanceof EntityLeafcutterAnt;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return AMSoundRegistry.ANTEATER_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return AMSoundRegistry.ANTEATER_HURT.get();
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound compound) {
        super.writeCustomDataToNbt(compound);
        compound.putBoolean("Standing", this.isStanding());
        compound.putInt("AntCooldown", this.eatAntCooldown);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound compound) {
        super.readCustomDataFromNbt(compound);
        this.setStanding(compound.getBoolean("Standing"));
        this.eatAntCooldown = compound.getInt("AntCooldown");
    }

    @Override
    public boolean isBreedingItem(ItemStack stack) {
        return stack.isIn(AMTagRegistry.ANTEATER_BREEDABLES);
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(STANDING, Boolean.FALSE);
        this.dataTracker.startTracking(ANT_ON_TONGUE, Boolean.FALSE);
        this.dataTracker.startTracking(LEANING_DOWN, Boolean.FALSE);
        this.dataTracker.startTracking(ANGER_TIME, 0);
    }

    @Override
    public int getAngerTime() {
        return this.dataTracker.get(ANGER_TIME);
    }

    @Override
    public void setAngerTime(int time) {
        this.dataTracker.set(ANGER_TIME, time);
    }

    @Override
    public UUID getAngryAt() {
        return this.lastHurtBy;
    }

    @Override
    public void setAngryAt(@Nullable UUID target) {
        this.lastHurtBy = target;
    }

    @Override
    public void chooseRandomAngerTime() {
        this.setAngerTime(ANGRY_TIMER.get(this.random));
    }

    public boolean isStanding() {
        return this.dataTracker.get(STANDING);
    }

    public void setStanding(boolean standing) {
        this.dataTracker.set(STANDING, standing);
    }

    public boolean hasAntOnTongue() {
        return this.dataTracker.get(ANT_ON_TONGUE);
    }

    public void setAntOnTongue(boolean standing) {
        this.dataTracker.set(ANT_ON_TONGUE, standing);
    }

    @Override
    public boolean collidesWith(Entity entity) {
        return !(entity instanceof EntityLeafcutterAnt) && super.collidesWith(entity);
    }

    @Override
    public void pushAway(Entity entity) {
        if (!(entity instanceof EntityLeafcutterAnt)) {
            super.pushAway(entity);
        }
    }

    public boolean isLeaning() {
        return this.dataTracker.get(LEANING_DOWN);
    }

    public void setLeaning(boolean leaning) {
        this.dataTracker.set(LEANING_DOWN, leaning);
    }

    @Override
    protected boolean isImmobile() {
        return super.isImmobile();
    }

    @Override
    protected void mobTick() {
        if (!this.getWorld().isClient) {
            this.tickAngerLogic((ServerWorld) this.getWorld(), false);
        }
    }

    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        final ItemStack itemstack = player.getStackInHand(hand);
        final ActionResult type = super.interactMob(player, hand);
        final boolean isFoodstuff = itemstack.isIn(AMTagRegistry.ANTEATER_FOODSTUFFS);
        if (isFoodstuff) {
            final ItemStack rippedStack = itemstack.copy();
            rippedStack.setCount(1);
            this.stopAnger();
            this.heal(4);
            this.setStackInHand(Hand.MAIN_HAND, rippedStack);
            if (itemstack.isIn(AMTagRegistry.ANTEATER_BREEDABLES)) {
                return type;
            }
            this.eat(player, hand, itemstack);
            return ActionResult.SUCCESS;
        }
        return type;
    }

    @Override
    public void tick() {
        super.tick();
        prevStandProgress = standProgress;
        prevTongueProgress = tongueProgress;
        prevLeaningProgress = leaningProgress;

        if (isStanding()) {
            if (standProgress < 5F)
                standProgress++;
        } else {
            if (standProgress > 0F)
                standProgress--;
        }

        final boolean isTongueOut = this.getAnimation() == ANIMATION_TOUNGE_IDLE;
        if (isTongueOut) {
            if (tongueProgress < 5F)
                tongueProgress++;
        } else {
            if (tongueProgress > 0F)
                tongueProgress--;
        }

        if (isLeaning()) {
            if (leaningProgress < 5F)
                leaningProgress++;
        } else {
            if (leaningProgress > 0F)
                leaningProgress--;
        }

        if (isStanding() && ++standingTime > maxStandTime) {
            this.setStanding(false);
            standingTime = 0;
            maxStandTime = 75 + random.nextInt(50);
        }

        if (this.hasVehicle() && this.getVehicle() instanceof final EntityAnteater mount) {
            if (this.isBaby()) {
                this.setYaw(mount.bodyYaw);
                this.headYaw= mount.bodyYaw;
                this.bodyYaw = mount.bodyYaw;
            } else {
                this.dismountVehicle();
            }
        }

        if (eatAntCooldown > 0) {
            eatAntCooldown--;
        }
        if (antsEatenRecently >= 3 && eatAntCooldown <= 0) {
            this.resetAntCooldown();
        }
        if (ticksAntOnTongue > 10 && this.hasAntOnTongue()) {
            this.heal(6);
            this.emitGameEvent(GameEvent.EAT);
            this.playSound(SoundEvents.ENTITY_GENERIC_EAT, this.getSoundVolume(), this.getSoundPitch());
            this.setAntOnTongue(false);
        }
        if (this.hasAntOnTongue()) {
            ticksAntOnTongue++;
        } else {
            ticksAntOnTongue = 0;
        }
        if (!this.getWorld().isClient && getTongueStickOut() > 0.6F && !this.hasAntOnTongue() && antsEatenRecently < 3) {
            EntityLeafcutterAnt closestAnt = null;
            for (var entity : this.getWorld().getNonSpectatingEntities(EntityLeafcutterAnt.class, this.getBoundingBox().expand(2.6F))) {
                if (closestAnt == null || entity.distanceTo(this) < closestAnt.distanceTo(this) && this.canSee(entity)) {
                    closestAnt = entity;
                }
            }
            if (closestAnt != null) {
                closestAnt.remove(RemovalReason.KILLED);
                ticksAntOnTongue = 0;
                this.setAntOnTongue(true);
                antsEatenRecently++;
            }
        }
        if (!this.getMainHandStack().isEmpty()) {
            heldItemTime++;
            if (heldItemTime > 10 && getTongueStickOut() < 0.3F && canTargetItem(this.getMainHandStack())) {
                heldItemTime = 0;
                this.heal(4);
                this.playSound(SoundEvents.ENTITY_GENERIC_EAT, this.getSoundVolume(), this.getSoundPitch());
                this.emitGameEvent(GameEvent.EAT);
                if (this.getMainHandStack().getItem().hasRecipeRemainder()) {
                    this.dropItem(this.getMainHandStack().getItem().getRecipeRemainder());
                }
                this.stopAnger();
                this.getMainHandStack().decrement(1);
            }
        } else {
            heldItemTime = 0;
        }

        if (!this.getWorld().isClient) {
            if (getRandom().nextInt(300) == 0)
                this.setAnimation(ANIMATION_TOUNGE_IDLE);

            final var attackTarget = this.getTarget();
            if (attackTarget != null) {
                if (distanceTo(attackTarget) < attackTarget.getWidth() + this.getWidth() + 2) {
                    if (this.getAnimationTick() == 7) {
                        if (this.getAnimation() == ANIMATION_SLASH_L) {
                            tryAttack(attackTarget);
                            final float rot = getYaw() + 90;
                            attackTarget.takeKnockback(0.5F, MathHelper.sin(rot * MathHelper.RADIANS_PER_DEGREE), -MathHelper.cos(rot * MathHelper.RADIANS_PER_DEGREE));
                        } else if (this.getAnimation() == ANIMATION_SLASH_R) {
                            tryAttack(attackTarget);
                            final float rot = getYaw() - 90;
                            attackTarget.takeKnockback(0.5F, MathHelper.sin(rot * MathHelper.RADIANS_PER_DEGREE), -MathHelper.cos(rot * MathHelper.RADIANS_PER_DEGREE));
                        }
                    }
                }
            }
        }

        AnimationHandler.INSTANCE.updateAnimations(this);
    }

    public void resetAntCooldown() {
        this.eatAntCooldown = 600 + random.nextInt(1000);
        this.antsEatenRecently = 0;
    }

    public void standFor(int time) {
        this.setStanding(true);
        this.maxStandTime = time;
    }

    public float getTongueStickOut() {
        if (this.tongueProgress > 0F) {
            final double tongueM = Math.min(Math.sin(this.age * 0.15F), 0);
            return (float) -tongueM * (this.tongueProgress * 0.2F);
        }
        return 0.0F;
    }

    @Nullable
    @Override
    public PassiveEntity createChild(ServerWorld level, PassiveEntity parent) {
        return AMEntityRegistry.ANTEATER.get().create(getWorld());
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
    public boolean canTargetItem(ItemStack stack) {
        return !this.hasAntOnTongue() && stack.isIn(AMTagRegistry.INSECT_ITEMS);
    }

    @Override
    public void onGetItem(ItemEntity e) {
        final var duplicate = e.getStack().copy();
        duplicate.setCount(1);
        if (!this.getStackInHand(Hand.MAIN_HAND).isEmpty() && !this.getWorld().isClient) {
            this.dropStack(this.getStackInHand(Hand.MAIN_HAND), 0.0F);
        }
        this.setAnimation(ANIMATION_TOUNGE_IDLE);
        this.setStackInHand(Hand.MAIN_HAND, duplicate);
    }

    @Override
    public Animation[] getAnimations() {
        return new Animation[]{ANIMATION_SLASH_L, ANIMATION_SLASH_R, ANIMATION_TOUNGE_IDLE};
    }

    private boolean shouldTargetAnts() {
        return !this.hasAngerTime();
    }

    public boolean isPeter() {
        final var name = Formatting.strip(this.getName().getString());
        if (name == null)
            return false;

        final var lowercaseName = name.toLowerCase(Locale.ROOT);
        return lowercaseName.contains("peter") || lowercaseName.contains("petr") || lowercaseName.contains("zot");
    }

    @Override
    public EntityData initialize(ServerWorldAccess worldIn, LocalDifficulty difficultyIn, SpawnReason reason, @Nullable EntityData spawnDataIn, @Nullable NbtCompound dataTag) {
        if (spawnDataIn == null)
            spawnDataIn = new PassiveEntity.PassiveData(0.5F);

        return super.initialize(worldIn, difficultyIn, reason, spawnDataIn, dataTag);
    }

    private class AITargetAnts extends ActiveTargetGoal {
        private static final Predicate<EntityLeafcutterAnt> QUEEN_ANT = (entity) -> !entity.isQueen();

        public AITargetAnts() {
            super(EntityAnteater.this, EntityLeafcutterAnt.class, 30, true, false, QUEEN_ANT);
        }

        @Override
        public boolean canStart() {
            return EntityAnteater.this.shouldTargetAnts() && !EntityAnteater.this.isBaby() && !EntityAnteater.this.hasAntOnTongue() && !EntityAnteater.this.isStanding() && super.canStart();
        }

        @Override
        public boolean shouldContinue() {
            return EntityAnteater.this.shouldTargetAnts() && !EntityAnteater.this.hasAntOnTongue() && !EntityAnteater.this.isStanding() && super.shouldContinue();
        }
    }

    private class AIMelee extends Goal {
        public AIMelee() {
            this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
        }

        @Override
        public boolean canStart() {
            return EntityAnteater.this.getTarget() != null && EntityAnteater.this.getTarget().isAlive() && !EntityAnteater.this.isBaby();
        }

        @Override
        public void tick() {
            final LivingEntity enemy = EntityAnteater.this.getTarget();
            if (enemy != null) {
                final double attackReachSqr = this.getSquaredMaxAttackDistance(enemy);
                final double distToEnemySqr = EntityAnteater.this.distanceTo(enemy);
                EntityAnteater.this.lookAtEntity(enemy, 100, 5);
                if (enemy instanceof EntityLeafcutterAnt) {
                    if (distToEnemySqr <= attackReachSqr + 1.5F) {
                        EntityAnteater.this.setAnimation(ANIMATION_TOUNGE_IDLE);
                    } else {
                        EntityAnteater.this.lookAtEntity(enemy, 5, 5);
                    }
                    EntityAnteater.this.getNavigation().startMovingTo(enemy, 1.0D);
                } else {
                    if (distToEnemySqr <= attackReachSqr) {
                        EntityAnteater.this.getNavigation().startMovingTo(enemy, 1.0D);
                        EntityAnteater.this.setAnimation(EntityAnteater.this.getRandom().nextBoolean() ? ANIMATION_SLASH_L : ANIMATION_SLASH_R);
                    }
                    final double x = enemy.getX() - EntityAnteater.this.getX();
                    final double z = enemy.getZ() - EntityAnteater.this.getZ();
                    final float f = (float) (MathHelper.atan2(z, x) * MathHelper.DEGREES_PER_RADIAN) - 90.0F;
                    EntityAnteater.this.setYaw(f);
                    EntityAnteater.this.bodyYaw = f;
                    EntityAnteater.this.setStanding(true);
                }
            }
        }

        @Override
        public void stop() {
            EntityAnteater.this.setStanding(false);
            super.stop();
        }

        protected double getSquaredMaxAttackDistance(LivingEntity attackTarget) {
            return 2.0F + attackTarget.getWidth();
        }
    }

}
