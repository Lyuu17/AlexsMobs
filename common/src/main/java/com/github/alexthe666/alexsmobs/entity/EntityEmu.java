package com.github.alexthe666.alexsmobs.entity;

import com.github.alexthe666.alexsmobs.config.AMConfig;
import com.github.alexthe666.alexsmobs.entity.ai.AnimalAIHerdPanic;
import com.github.alexthe666.alexsmobs.entity.ai.AnimalAIWanderRanged;
import com.github.alexthe666.alexsmobs.registry.AMEntityRegistry;
import com.github.alexthe666.alexsmobs.registry.AMItemRegistry;
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
import net.minecraft.entity.mob.AbstractSkeletonEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PillagerEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.recipe.Ingredient;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class EntityEmu extends AnimalEntity implements IAnimatedEntity, IHerdPanic {

    public static final Animation ANIMATION_DODGE_LEFT = Animation.create(10);
    public static final Animation ANIMATION_DODGE_RIGHT = Animation.create(10);
    public static final Animation ANIMATION_PECK_GROUND = Animation.create(25);
    public static final Animation ANIMATION_SCRATCH = Animation.create(20);
    public static final Animation ANIMATION_PUZZLED = Animation.create(30);
    private static final TrackedData<Integer> VARIANT = DataTracker.registerData(EntityEmu.class, TrackedDataHandlerRegistry.INTEGER);
    private int animationTick;
    private Animation currentAnimation;
    private int revengeCooldown = 0;
    private boolean emuAttackedDirectly = false;
    public int timeUntilNextEgg = this.random.nextInt(6000) + 6000;

    public EntityEmu(EntityType<? extends EntityEmu> type, World world) {
        super(type, world);
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createLivingAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 20.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.35F)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 3F)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32.0D);
    }

    public static <T extends MobEntity> boolean canEmuSpawn(EntityType<? extends AnimalEntity> animal, WorldAccess worldIn, SpawnReason reason, BlockPos pos, Random random) {
        boolean spawnBlock = worldIn.getBlockState(pos.down()).isIn(AMTagRegistry.EMU_SPAWNS);
        return spawnBlock && worldIn.getBaseLightLevel(pos, 0) > 8;
    }

    @Override
    public boolean canSpawn(WorldAccess worldIn, SpawnReason spawnReasonIn) {
        return AMEntityRegistry.rollSpawn(AMConfig.emuSpawnRolls, this.getRandom(), spawnReasonIn);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return AMSoundRegistry.EMU_IDLE.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return AMSoundRegistry.EMU_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return AMSoundRegistry.EMU_HURT.get();
    }

    public int getVariant() {
        return this.dataTracker.get(VARIANT);
    }

    public void setVariant(int variant) {
        this.dataTracker.set(VARIANT, variant);
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(VARIANT, 0);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new MeleeAttackGoal(this, 1.3D, true) {
            @Override
            protected double getSquaredMaxAttackDistance(LivingEntity attackTarget) {
                return super.getSquaredMaxAttackDistance(attackTarget) + 2.5D;
            }

            @Override
            public boolean canStart() {
                return super.canStart() && EntityEmu.this.revengeCooldown <= 0;
            }

            @Override
            public boolean shouldContinue() {
                return super.shouldContinue() && EntityEmu.this.revengeCooldown <= 0;
            }
        });
        this.goalSelector.add(2, new AnimalAIHerdPanic(this, 1.5D));
        this.goalSelector.add(3, new AnimalMateGoal(this, 1.0D));
        this.goalSelector.add(4, new FollowParentGoal(this, 1.1D));
        this.goalSelector.add(4, new TemptGoal(this, 1.1D, Ingredient.fromTag(AMTagRegistry.EMU_BREEDABLES), false));
        this.goalSelector.add(5, new AnimalAIWanderRanged(this, 110, 1.0D, 10, 7));
        this.goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 15.0F));
        this.goalSelector.add(7, new LookAroundGoal(this));
        this.targetSelector.add(1, new MyRevengeGoal());
        if(AMConfig.emuTargetSkeletons){
            this.targetSelector.add(2, new ActiveTargetGoal<>(this, AbstractSkeletonEntity.class, false));
            this.targetSelector.add(3, new ActiveTargetGoal<>(this, PillagerEntity.class, false));
        }
    }

    @Override
    public boolean isBreedingItem(ItemStack stack) {
        return stack.isIn(AMTagRegistry.EMU_BREEDABLES);
    }

    @Override
    public boolean canTarget(LivingEntity target) {
        return !this.isBaby() && super.canTarget(target);
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        boolean prev = super.damage(source, amount);
        if (prev) {
            double range = 15;
            int fleeTime = 100 + getRandom().nextInt(5);
            this.revengeCooldown = fleeTime;
            List<? extends EntityEmu> list = this.getWorld().getNonSpectatingEntities(this.getClass(), this.getBoundingBox().expand(range, range / 2, range));
            for (var emu : list) {
                emu.revengeCooldown = fleeTime;
                if(emu.isBaby() && random.nextInt(2) == 0){
                    emu.emuAttackedDirectly = this.getAttacker() != null;
                    emu.revengeCooldown = emu.emuAttackedDirectly ? 10 + getRandom().nextInt(30) : fleeTime;
                }
            }
            emuAttackedDirectly = this.getAttacker() != null;
            this.revengeCooldown = emuAttackedDirectly ? 10 + getRandom().nextInt(30) : revengeCooldown;
        }
        return prev;
    }

    @Override
    public void travel(Vec3d travelVector) {
        this.setMovementSpeed((float) this.getAttributeValue(EntityAttributes.GENERIC_MOVEMENT_SPEED) * (this.getAnimation() == ANIMATION_PECK_GROUND || this.getAnimation() == ANIMATION_PUZZLED ? 0.15F : 1F) * (isInLava() ? 0.2F : 1F));
        super.travel(travelVector);
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.getWorld().isClient) {
            if (this.getAttacker() == null && this.getTarget() == null) {
                if (this.getVelocity().lengthSquared() < 0.03D && this.getRandom().nextInt(190) == 0 && this.getAnimation() == NO_ANIMATION) {
                    if (getRandom().nextInt(3) == 0) {
                        this.setAnimation(ANIMATION_PUZZLED);
                    } else if (this.isOnGround()) {
                        this.setAnimation(ANIMATION_PECK_GROUND);
                    }
                }
            }
            if (revengeCooldown > 0) {
                revengeCooldown--;
            }
            if (revengeCooldown <= 0 && this.getAttacker() != null && !emuAttackedDirectly) {
                this.setAttacker(null);
                revengeCooldown = 0;
            }
            LivingEntity target = getTarget();
            if (this.isAlive() && target != null && this.getAnimation() == ANIMATION_SCRATCH && this.distanceTo(target) < 4F && (this.getAnimationTick() == 8 || this.getAnimationTick() == 15)) {
                float f1 = this.getYaw() * MathHelper.RADIANS_PER_DEGREE;
                this.setVelocity(this.getVelocity().add(-MathHelper.sin(f1) * 0.02F, 0.0D, MathHelper.cos(f1) * 0.02F));
                target.takeKnockback(0.4F, target.getX() - this.getX(), target.getZ() - this.getZ());
                target.damage(this.getDamageSources().mobAttack(this), (float) this.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE).getBaseValue());
            }
        }
        if (!this.getWorld().isClient && this.isAlive() && !this.isBaby() && --this.timeUntilNextEgg <= 0) {
            this.playSound(SoundEvents.ENTITY_CHICKEN_EGG, 1.0F, (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F);
            this.dropItem(AMItemRegistry.EMU_EGG.get());
            this.timeUntilNextEgg = this.random.nextInt(6000) + 6000;
        }
        AnimationHandler.INSTANCE.updateAnimations(this);
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
        return new Animation[]{ANIMATION_DODGE_LEFT, ANIMATION_DODGE_RIGHT, ANIMATION_PECK_GROUND, ANIMATION_SCRATCH, ANIMATION_PUZZLED};
    }

    @Override
    public int getAnimationTick() {
        return animationTick;
    }

    @Override
    public void setAnimationTick(int tick) {
        animationTick = tick;
    }

    @Nullable
    @Override
    public PassiveEntity createChild(ServerWorld world, PassiveEntity entity) {
        EntityEmu emu = AMEntityRegistry.EMU.get().create(world);
        emu.setVariant(this.getVariant());
        return emu;
    }

    @Override
    public boolean tryAttack(Entity entityIn) {
        if (this.getAnimation() == NO_ANIMATION) {
            this.setAnimation(ANIMATION_SCRATCH);
        }
        return true;
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound compound) {
        super.readCustomDataFromNbt(compound);
        this.setVariant(compound.getInt("Variant"));
        if (compound.contains("EggLayTime")) {
            this.timeUntilNextEgg = compound.getInt("EggLayTime");
        }
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound compound) {
        super.writeCustomDataToNbt(compound);
        compound.putInt("Variant", this.getVariant());
        compound.putInt("EggLayTime", this.timeUntilNextEgg);
    }

    @Nullable
    @Override
    public EntityData initialize(ServerWorldAccess worldIn, LocalDifficulty difficultyIn, SpawnReason reason, @Nullable EntityData spawnDataIn, @Nullable NbtCompound dataTag) {
        if(this.random.nextInt(200) == 0){
            this.setVariant(2);
        }else if(random.nextInt(3) == 0){
            this.setVariant(1);
        }
        return super.initialize(worldIn, difficultyIn, reason, spawnDataIn, dataTag);
    }

    @Override
    public void onPanic() {
    }

    @Override
    public boolean canPanic() {
        return true;
    }

    class MyRevengeGoal extends RevengeGoal {
        public MyRevengeGoal() {
            super(EntityEmu.this);
        }

        @Override
        public void start() {
            if (EntityEmu.this.isBaby() || !emuAttackedDirectly) {
                this.setGroupRevenge();
                this.stop();
            } else {
                super.start();
            }
        }

        @Override
        protected void setMobEntityTarget(MobEntity mobIn, LivingEntity targetIn) {
            if (mobIn instanceof EntityEmu && !mobIn.isBaby() && !emuAttackedDirectly && ((EntityEmu) mobIn).revengeCooldown <= 0) {
                super.setMobEntityTarget(mobIn, targetIn);
            }

        }
    }
}
