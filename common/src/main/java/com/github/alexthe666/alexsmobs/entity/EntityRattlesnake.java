package com.github.alexthe666.alexsmobs.entity;

import com.github.alexthe666.alexsmobs.config.AMConfig;
import com.github.alexthe666.alexsmobs.entity.ai.AnimalAIWanderRanged;
import com.github.alexthe666.alexsmobs.registry.AMEntityRegistry;
import com.github.alexthe666.alexsmobs.registry.AMSoundRegistry;
import com.github.alexthe666.alexsmobs.registry.AMTagRegistry;
import com.iafenvoy.uranus.animation.Animation;
import com.iafenvoy.uranus.animation.AnimationHandler;
import com.iafenvoy.uranus.animation.IAnimatedEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.goal.*;
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
import net.minecraft.entity.passive.RabbitEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.event.GameEvent;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Predicate;

public class EntityRattlesnake extends AnimalEntity implements IAnimatedEntity {

    public float prevCurlProgress;
    public float curlProgress;
    public int randomToungeTick = 0;
    public int maxCurlTime = 75;
    private int curlTime = 0;
    private static final TrackedData<Boolean> RATTLING = DataTracker.registerData(EntityRattlesnake.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> CURLED = DataTracker.registerData(EntityRattlesnake.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final Predicate<LivingEntity> WARNABLE_PREDICATE = (mob) -> mob instanceof PlayerEntity && !((PlayerEntity) mob).isCreative() && !mob.isSpectator() || mob instanceof EntityRoadrunner;
    private static final Predicate<LivingEntity> TARGETABLE_PREDICATE = (mob) -> mob instanceof PlayerEntity && !((PlayerEntity) mob).isCreative() && !mob.isSpectator() || mob instanceof EntityRoadrunner;
    private int animationTick;
    private Animation currentAnimation;
    public static final Animation ANIMATION_BITE = Animation.create(20);
    private int loopSoundTick = 0;

    public EntityRattlesnake(EntityType<? extends EntityRattlesnake> type, World worldIn) {
        super(type, worldIn);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new MeleeAttackGoal(this, 1.2D, true));
        this.goalSelector.add(2, new WarnPredatorsGoal());
        this.goalSelector.add(2, new AnimalMateGoal(this, 1.0D));
        this.goalSelector.add(4, new FollowParentGoal(this, 1.1D));
        this.goalSelector.add(5, new AnimalAIWanderRanged(this, 60, 1.0D, 7, 7));
        this.goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 15.0F));
        this.goalSelector.add(7, new LookAroundGoal(this));
        this.targetSelector.add(1, new ActiveTargetGoal<>(this, RabbitEntity.class, 15, true, true, null));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, EntityJerboa.class, 15, true, true, null));
        this.targetSelector.add(3, (new RevengeGoal(this)));
        this.targetSelector.add(4, new ShortDistanceTarget());
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return AMSoundRegistry.RATTLESNAKE_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return AMSoundRegistry.RATTLESNAKE_HURT.get();
    }

    @Override
    public boolean canSpawn(WorldAccess worldIn, SpawnReason spawnReasonIn) {
        return AMEntityRegistry.rollSpawn(AMConfig.rattlesnakeSpawnRolls, this.getRandom(), spawnReasonIn);
    }

    @Override
    public boolean tryAttack(Entity entityIn) {
        this.setAnimation(ANIMATION_BITE);
        return true;
    }

    @Override
    public boolean canHaveStatusEffect(StatusEffectInstance potioneffectIn) {
        if (potioneffectIn.getEffectType() == StatusEffects.POISON) {
            return false;
        }
        return super.canHaveStatusEffect(potioneffectIn);
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(CURLED, false);
        this.dataTracker.startTracking(RATTLING, false);
    }

    public boolean isCurled() {
        return this.dataTracker.get(CURLED);
    }

    public void setCurled(boolean curled) {
        this.dataTracker.set(CURLED, curled);
    }

    public boolean isRattling() {
        return this.dataTracker.get(RATTLING);
    }

    public void setRattling(boolean rattling) {
        this.dataTracker.set(RATTLING, rattling);
    }

    @Override
    public void tick(){
        super.tick();
        prevCurlProgress = curlProgress;

        if (this.isCurled()) {
            if (curlProgress < 5F)
                curlProgress += 0.5F;
        } else {
            if (curlProgress > 0F)
                curlProgress--;
        }

        if (randomToungeTick == 0 && random.nextInt(15) == 0) {
            randomToungeTick = 10 + random.nextInt(20);
        }
        if (randomToungeTick > 0) {
            randomToungeTick--;
        }
        if (isCurled() && !isRattling() && ++curlTime > maxCurlTime) {
            this.setCurled(false);
            curlTime = 0;
            maxCurlTime = 75 + random.nextInt(50);
        }

        LivingEntity target = this.getTarget();
        if (!this.getWorld().isClient) {
            if (this.isCurled() && (target != null && target.isAlive())) {
                this.setCurled(false);
            }
            if (this.isRattling() && target == null) {
                this.setCurled(true);

            }
            if (!this.isCurled() && this.getTarget() == null && random.nextInt(500) == 0) {
                maxCurlTime = 300 + random.nextInt(250);
                this.setCurled(true);
            }
        }
        if (this.getAnimation() == ANIMATION_BITE) {
            if (this.getAnimationTick() == 4) {
                this.playSound(AMSoundRegistry.RATTLESNAKE_ATTACK.get(), getSoundVolume(), getSoundPitch());
            }
            if (this.getAnimationTick() == 8 && target != null && this.distanceTo(target) < 2D) {
                final boolean meepMeep = target instanceof EntityRoadrunner;
                final int f = isBaby() ? 2 : 1;
                target.damage(this.getDamageSources().mobAttack(this), meepMeep ? 1.0F : f * (float) getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE).getBaseValue());
                if (!meepMeep) {
                    target.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, 300, f * 2));
                }
            }
        }
        if(isRattling()){
            if(loopSoundTick == 0){
                this.emitGameEvent(GameEvent.ENTITY_ROAR);
                this.playSound(AMSoundRegistry.RATTLESNAKE_LOOP.get(), this.getSoundVolume() * 0.5F, this.getSoundPitch());
            }
            loopSoundTick++;
            if(loopSoundTick > 50){
                loopSoundTick = 0;
            }
        }
        AnimationHandler.INSTANCE.updateAnimations(this);
    }

    @Override
    public void travel(Vec3d vec3d) {
        if (this.isOnGround() && this.isCurled()) {
            if (this.getNavigation().getCurrentPath() != null) {
                this.getNavigation().stop();
            }
            vec3d = Vec3d.ZERO;
        }
        super.travel(vec3d);
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createLivingAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 8D)
                .add(EntityAttributes.GENERIC_ARMOR, 0.0D)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 2.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.28F)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32.0D);
    }

    @Override
    public boolean isBreedingItem(ItemStack stack) {
        return stack.getItem().isFood() && stack.getItem().getFoodComponent() != null && stack.getItem().getFoodComponent().isMeat();
    }

    @Nullable
    @Override
    public PassiveEntity createChild(ServerWorld world, PassiveEntity entity) {
        return AMEntityRegistry.RATTLESNAKE.get().create(world);
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
    public Animation[] getAnimations() {
        return new Animation[]{ANIMATION_BITE};
    }

    public static boolean canRattlesnakeSpawn(EntityType<? extends AnimalEntity> animal, WorldAccess worldIn, SpawnReason reason, BlockPos pos, Random random) {
        boolean spawnBlock = worldIn.getBlockState(pos.down()).isIn(AMTagRegistry.RATTLESNAKE_SPAWNS);
        return spawnBlock && worldIn.getBaseLightLevel(pos, 0) > 8;
    }

    class WarnPredatorsGoal extends Goal {
        int executionChance = 20;
        Entity target = null;

        @Override
        public boolean canStart() {
            if(EntityRattlesnake.this.getRandom().nextInt(executionChance) == 0){
                final double dist = 5D;
                List<LivingEntity> list = EntityRattlesnake.this.getWorld().getEntitiesByClass(LivingEntity.class, EntityRattlesnake.this.getBoundingBox().expand(dist, dist, dist), WARNABLE_PREDICATE);
                double d0 = Double.MAX_VALUE;
                Entity possibleTarget = null;
                for(Entity entity : list) {
                    double d1 = EntityRattlesnake.this.squaredDistanceTo(entity);
                    if (!(d1 > d0)) {
                        d0 = d1;
                        possibleTarget = entity;
                    }
                }
                target = possibleTarget;
                return !list.isEmpty();
            }
            return false;
        }

        @Override
        public boolean shouldContinue(){
            return target != null && EntityRattlesnake.this.distanceTo(target) < 5D && EntityRattlesnake.this.getTarget() == null;
        }

        @Override
        public void stop() {
            target = null;
            EntityRattlesnake.this.setRattling(false);
        }

        @Override
        public void tick(){
            EntityRattlesnake.this.setRattling(true);
            EntityRattlesnake.this.setCurled(true);
            EntityRattlesnake.this.curlTime = 0;
            EntityRattlesnake.this.getLookControl().lookAt(target, 30, 30);
        }
    }

    class ShortDistanceTarget extends ActiveTargetGoal<PlayerEntity> {
        public ShortDistanceTarget() {
            super(EntityRattlesnake.this, PlayerEntity.class, 3, true, true, TARGETABLE_PREDICATE);
        }

        @Override
        public boolean canStart() {
            if (EntityRattlesnake.this.isBaby()) {
                return false;
            } else {
                return super.canStart();
            }
        }

        @Override
        public void start(){
            super.start();
            EntityRattlesnake.this.setCurled(false);
            EntityRattlesnake.this.setRattling(true);
        }

        @Override
        protected double getFollowRange() {
            return 2D;
        }
    }

}
