package com.github.alexthe666.alexsmobs.entity;

import com.github.alexthe666.alexsmobs.config.AMConfig;
import com.github.alexthe666.alexsmobs.entity.ai.AnimalAIHerdPanic;
import com.github.alexthe666.alexsmobs.entity.ai.AnimalAIWanderRanged;
import com.github.alexthe666.alexsmobs.registry.AMEntityRegistry;
import com.github.alexthe666.alexsmobs.registry.AMSoundRegistry;
import com.github.alexthe666.alexsmobs.registry.AMTagRegistry;
import com.iafenvoy.uranus.animation.Animation;
import com.iafenvoy.uranus.animation.AnimationHandler;
import com.iafenvoy.uranus.animation.IAnimatedEntity;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.goal.*;
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
import net.minecraft.nbt.NbtCompound;
import net.minecraft.recipe.Ingredient;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class EntityGazelle extends AnimalEntity implements IAnimatedEntity, IHerdPanic {

    private int animationTick;
    private Animation currentAnimation;
    public static final Animation ANIMATION_FLICK_EARS = Animation.create(20);
    public static final Animation ANIMATION_FLICK_TAIL = Animation.create(14);
    public static final Animation ANIMATION_EAT_GRASS = Animation.create(30);
    private boolean hasSpedUp = false;
    private int revengeCooldown = 0;
    private static final TrackedData<Boolean> RUNNING = DataTracker.registerData(EntityGazelle.class, TrackedDataHandlerRegistry.BOOLEAN);

    public EntityGazelle(EntityType<? extends EntityGazelle> type, World worldIn) {
        super(type, worldIn);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new AnimalAIHerdPanic(this, 1.1D));
        this.goalSelector.add(2, new AnimalMateGoal(this, 1.0D));
        this.goalSelector.add(4, new FollowParentGoal(this, 1.1D));
        this.goalSelector.add(4, new TemptGoal(this, 1.1D, Ingredient.fromTag(AMTagRegistry.GAZELLE_BREEDABLES), false));
        this.goalSelector.add(5, new AnimalAIWanderRanged(this, 100, 1.0D, 25, 7));
        this.goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 15.0F));
        this.goalSelector.add(7, new LookAroundGoal(this));
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return AMSoundRegistry.GAZELLE_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return AMSoundRegistry.GAZELLE_HURT.get();
    }

    @Override
    public boolean canSpawn(WorldAccess worldIn, SpawnReason spawnReasonIn) {
        return AMEntityRegistry.rollSpawn(AMConfig.gazelleSpawnRolls, this.getRandom(), spawnReasonIn) && super.canSpawn(worldIn, spawnReasonIn);
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
        boolean prev = super.damage(source, amount);
        if(prev){
            double range = 15;
            int fleeTime = 100 + getRandom().nextInt(150);
            this.revengeCooldown = fleeTime;
            List<? extends EntityGazelle> list = this.getWorld().getNonSpectatingEntities(this.getClass(), this.getBoundingBox().expand(range, range/2, range));
            for(EntityGazelle gaz : list){
                gaz.revengeCooldown = fleeTime;

            }
        }
        return prev;
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(RUNNING, false);
    }

    public boolean isRunning() {
        return this.dataTracker.get(RUNNING);
    }

    public void setRunning(boolean running) {
        this.dataTracker.set(RUNNING, running);
    }

    @Override
    public boolean isBreedingItem(ItemStack stack) {
        return stack.isIn(AMTagRegistry.GAZELLE_BREEDABLES);
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
    public void tick() {
        super.tick();
        if(!this.getWorld().isClient){
            if(this.getAnimation() == NO_ANIMATION && getRandom().nextInt(70) == 0 && (this.getAttacker() == null || this.distanceTo(this.getAttacker()) > 30)){
                if(getWorld().getBlockState(this.getBlockPos().down()).isOf(Blocks.GRASS_BLOCK) && getRandom().nextInt(3) == 0){
                    this.setAnimation(ANIMATION_EAT_GRASS);
                }else{
                    this.setAnimation(getRandom().nextBoolean()  ? ANIMATION_FLICK_EARS : ANIMATION_FLICK_TAIL);
                }
            }
            if(revengeCooldown >= 0){
                revengeCooldown--;
            }
            if(revengeCooldown == 0 && this.getAttacker() != null){
                this.setAttacker(null);
            }
            this.setRunning(revengeCooldown > 0);
            if(isRunning() && !hasSpedUp){
                hasSpedUp = true;
                this.setSprinting(true);
                this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).setBaseValue(0.475F);
            }
            if(!isRunning() && hasSpedUp){
                hasSpedUp = false;
                this.setSprinting(false);
                this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).setBaseValue(0.25F);
            }
        }
        AnimationHandler.INSTANCE.updateAnimations(this);
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound compound) {
        super.writeCustomDataToNbt(compound);
        compound.putBoolean("GazelleRunning", this.isRunning());
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound compound) {
        super.readCustomDataFromNbt(compound);
        this.setRunning(compound.getBoolean("GazelleRunning"));
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
        return new Animation[]{ANIMATION_FLICK_EARS, ANIMATION_FLICK_TAIL, ANIMATION_EAT_GRASS};
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createLivingAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 8.0D)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 2.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.25F)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32.0D);
    }

    @Nullable
    @Override
    public PassiveEntity createChild(ServerWorld world, PassiveEntity entity) {
        return AMEntityRegistry.GAZELLE.get().create(world);
    }

    @Override
    public void onPanic() {
    }

    @Override
    public boolean canPanic() {
        return true;
    }
}
