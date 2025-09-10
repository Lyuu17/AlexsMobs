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
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import org.jetbrains.annotations.Nullable;

public class EntitySnowLeopard extends AnimalEntity implements IAnimatedEntity, ITargetsDroppedItems {

    public static final Animation ANIMATION_ATTACK_R = Animation.create(13);
    public static final Animation ANIMATION_ATTACK_L = Animation.create(13);
    private int animationTick;
    private Animation currentAnimation;
    public float prevSneakProgress;
    public float sneakProgress;
    public float prevTackleProgress;
    public float tackleProgress;
    public float prevSitProgress;
    public float sitProgress;
    private static final TrackedData<Boolean> TACKLING = DataTracker.registerData(EntitySnowLeopard.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> SLEEPING = DataTracker.registerData(EntitySnowLeopard.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> SITTING = DataTracker.registerData(EntitySnowLeopard.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> SL_SNEAKING = DataTracker.registerData(EntitySnowLeopard.class, TrackedDataHandlerRegistry.BOOLEAN);
    private boolean hasSlowedDown = false;
    private int sittingTime = 0;
    private int maxSitTime = 75;
    public float prevSleepProgress;
    public float sleepProgress;

    public EntitySnowLeopard(EntityType<? extends EntitySnowLeopard> type, World worldIn) {
        super(type, worldIn);
        this.setStepHeight(2F);
    }

    @Override
    protected EntityNavigation createNavigation(World worldIn) {
        return new AdvancedPathNavigateNoTeleport(this, worldIn, false);
    }

    @Override
    public boolean canSpawn(WorldAccess worldIn, SpawnReason spawnReasonIn) {
        return AMEntityRegistry.rollSpawn(AMConfig.snowLeopardSpawnRolls, this.getRandom(), spawnReasonIn);
    }

    public static <T extends MobEntity> boolean canSnowLeopardSpawn(EntityType<EntitySnowLeopard> snowleperd, WorldAccess worldIn, SpawnReason reason, BlockPos p_223317_3_, Random random) {
        return worldIn.getBlockState(p_223317_3_.down()).isIn(AMTagRegistry.SNOW_LEOPARD_SPAWNS) && worldIn.getBaseLightLevel(p_223317_3_, 0) > 8;
    }

    @Override
    public boolean isBreedingItem(ItemStack stack) {
        return stack.isIn(AMTagRegistry.SNOW_LEOPARD_BREEDABLES);
    }

    @Override
    protected void fall(double y, boolean onGroundIn, BlockState state, BlockPos pos) {
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(1, new SwimGoal(this));
        this.goalSelector.add(2, new AnimalAIPanicBaby(this, 1.25D));
        this.goalSelector.add(3, new SnowLeopardAIMelee(this));
        this.goalSelector.add(5, new AnimalMateGoal(this, 1.0D));
        this.goalSelector.add(6, new FollowParentGoal(this, 1.1D));
        this.goalSelector.add(7, new WanderAroundGoal(this,  1.0D, 70));
        this.goalSelector.add(8, new LookAtEntityGoal(this, PlayerEntity.class, 15.0F));
        this.goalSelector.add(8, new LookAroundGoal(this));
        this.targetSelector.add(1, (new AnimalAIHurtByTargetNotBaby(this)));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, LivingEntity.class, 10, false, true, AMEntityRegistry.buildPredicateFromTag(AMTagRegistry.SNOW_LEOPARD_TARGETS)));
        this.targetSelector.add(3, new CreatureAITargetItems<>(this, false, 30));
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createLivingAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 30D)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 6.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.35F)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 64F)
                .add(EntityAttributes.GENERIC_ATTACK_KNOCKBACK, 0.0);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return AMSoundRegistry.SNOW_LEOPARD_IDLE.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return AMSoundRegistry.SNOW_LEOPARD_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return AMSoundRegistry.SNOW_LEOPARD_HURT.get();
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(SITTING, false);
        this.dataTracker.startTracking(SLEEPING, false);
        this.dataTracker.startTracking(SL_SNEAKING, false);
        this.dataTracker.startTracking(TACKLING, false);
    }

    public boolean isSitting() {
        return this.dataTracker.get(SITTING);
    }

    public void setSitting(boolean bar) {
        this.dataTracker.set(SITTING, bar);
    }

    public boolean isTackling() {
        return this.dataTracker.get(TACKLING);
    }

    public void setTackling(boolean bar) {
        this.dataTracker.set(TACKLING, bar);
    }

    public boolean isSLSneaking() {
        return this.dataTracker.get(SL_SNEAKING);
    }

    public void setSlSneaking(boolean bar) {
        this.dataTracker.set(SL_SNEAKING, bar);
    }

    @Nullable
    @Override
    public PassiveEntity createChild(ServerWorld world, PassiveEntity entity) {
        return AMEntityRegistry.SNOW_LEOPARD.get().create(world);
    }

    @Override
    public void tick(){
        super.tick();
        this.prevSitProgress = sitProgress;
        this.prevSneakProgress = sneakProgress;
        this.prevTackleProgress = tackleProgress;
        this.prevSleepProgress = sleepProgress;

        final boolean sitting = isSitting();
        final boolean slSneaking = isSLSneaking();
        final boolean tackling = isTackling();
        final boolean sleeping = isSleeping();

        if (sitting) {
            if (sitProgress < 5F) {
                sitProgress += 0.5F;
            }
        } else {
            if (sitProgress > 0F) {
                sitProgress -= 0.5F;
            }
        }

        if (slSneaking) {
            if (sneakProgress < 5F) {
                sneakProgress += 0.5F;
            }
        } else {
            if (sneakProgress > 0F) {
                sneakProgress -= 0.5F;
            }
        }

        if (tackling) {
            if (tackleProgress < 3F) {
                tackleProgress++;
            }
        } else {
            if (tackleProgress > 0F) {
                tackleProgress--;
            }
        }

        if (sleeping) {
            if (sleepProgress < 5F) {
                sleepProgress += 0.5F;
            }
        } else {
            if (sleepProgress > 0F) {
                sleepProgress -= 0.5F;
            }
        }

        if(slSneaking && !hasSlowedDown){
            hasSlowedDown = true;
            this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).setBaseValue(0.25F);
        }
        if(!slSneaking && hasSlowedDown){
            hasSlowedDown = false;
            this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).setBaseValue(0.35F);
        }
        if(tackling){
            this.bodyYaw = this.getYaw();
        }
        if(!this.getWorld().isClient) {
            if (this.getTarget() != null && (this.isSitting() || this.isSleeping())) {
                this.setSitting(false);
                this.setSleeping(false);
            }
            if ((isSitting() || isSleeping()) && (++sittingTime > maxSitTime || this.getTarget() != null || this.isInLove() || this.isInsideWaterOrBubbleColumn())) {
                this.setSitting(false);
                this.setSleeping(false);
                sittingTime = 0;
                maxSitTime = 100 + random.nextInt(50);
            }
            if (this.getTarget() == null && this.getVelocity().lengthSquared() < 0.03D && this.getAnimation() == NO_ANIMATION && !this.isSleeping() && !this.isSitting() && !this.isInsideWaterOrBubbleColumn() && random.nextInt(340) == 0) {
                sittingTime = 0;
                if (this.getRandom().nextInt(2) != 0) {
                    maxSitTime = 200 + random.nextInt(800);
                    this.setSitting(true);
                    this.setSleeping(false);
                } else {
                    maxSitTime = 2000 + random.nextInt(2600);
                    this.setSitting(false);
                    this.setSleeping(true);
                }
            }
        }
        LivingEntity attackTarget = this.getTarget();
        if (attackTarget != null) {
            if (distanceTo(attackTarget) < attackTarget.getWidth() + this.getWidth() + 0.6D && this.canSee(attackTarget)) {
                if (this.getAnimation() == ANIMATION_ATTACK_L && this.getAnimationTick() == 7) {
                    tryAttack(attackTarget);
                    float rot = getYaw() + 90;
                    attackTarget.takeKnockback(0.5F, MathHelper.sin(rot * MathHelper.RADIANS_PER_DEGREE), -MathHelper.cos(rot * MathHelper.RADIANS_PER_DEGREE));
                }
                if (this.getAnimation() == ANIMATION_ATTACK_R && this.getAnimationTick() == 7) {
                    tryAttack(attackTarget);
                    float rot = getYaw() - 90;
                    attackTarget.takeKnockback(0.5F, MathHelper.sin(rot * MathHelper.RADIANS_PER_DEGREE), -MathHelper.cos(rot * MathHelper.RADIANS_PER_DEGREE));
                }

            }
        }
        AnimationHandler.INSTANCE.updateAnimations(this);
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        final boolean prev = super.damage(source, amount);
        if (prev) {
            sittingTime = 0;
            this.setSleeping(false);
            this.setSitting(false);
        }
        return prev;
    }

    @Override
    public void travel(Vec3d vec3d) {
        if (this.isSitting() || this.isSleeping()) {
            if (this.getNavigation().getCurrentPath() != null) {
                this.getNavigation().stop();
            }
            vec3d = Vec3d.ZERO;
        }
        super.travel(vec3d);
    }

    @Override
    protected boolean isImmobile() {
        return super.isImmobile();
    }

    @Override
    public boolean isSleeping() {
        return this.dataTracker.get(SLEEPING);
    }

    public void setSleeping(boolean sleeping) {
        this.dataTracker.set(SLEEPING, sleeping);
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
    public int getAnimationTick() {
        return animationTick;
    }

    @Override
    public void setAnimationTick(int tick) {
        animationTick = tick;
    }

    @Override
    public Animation[] getAnimations() {
        return new Animation[]{ANIMATION_ATTACK_L, ANIMATION_ATTACK_R};
    }

    @Override
    public boolean canTargetItem(ItemStack stack) {
        return stack.getItem().isFood() && stack.getItem().getFoodComponent() != null && stack.getItem().getFoodComponent().isMeat();
    }

    @Override
    public void onGetItem(ItemEntity e) {
        this.heal(5);
    }
}
