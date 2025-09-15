package com.github.alexthe666.alexsmobs.entity;

import com.github.alexthe666.alexsmobs.entity.ai.CreatureAITargetItems;
import com.github.alexthe666.alexsmobs.registry.AMEntityRegistry;
import com.github.alexthe666.alexsmobs.registry.AMSoundRegistry;
import com.github.alexthe666.alexsmobs.registry.AMTagRegistry;
import com.iafenvoy.uranus.animation.Animation;
import com.iafenvoy.uranus.animation.AnimationHandler;
import com.iafenvoy.uranus.animation.IAnimatedEntity;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.FuzzyTargeting;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.ChickenEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.passive.RabbitEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.recipe.Ingredient;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import org.jetbrains.annotations.Nullable;

public class EntityTasmanianDevil extends AnimalEntity implements IAnimatedEntity, ITargetsDroppedItems {

    private int animationTick;
    private Animation currentAnimation;
    public static final Animation ANIMATION_HOWL = Animation.create(40);
    public static final Animation ANIMATION_ATTACK = Animation.create(8);
    private static final TrackedData<Boolean> BASKING = DataTracker.registerData(EntityTasmanianDevil.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> SITTING = DataTracker.registerData(EntityTasmanianDevil.class, TrackedDataHandlerRegistry.BOOLEAN);
    public float prevBaskProgress;
    public float prevSitProgress;
    public float baskProgress;
    public float sitProgress;
    private int sittingTime;
    private int maxSitTime;
    private int scareMobsTime = 0;

    public EntityTasmanianDevil(EntityType<EntityTasmanianDevil> type, World world) {
        super(type, world);
    }

    public boolean shouldMove() {
        return !isSitting() && !isBasking();
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return AMSoundRegistry.TASMANIAN_DEVIL_IDLE.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return AMSoundRegistry.TASMANIAN_DEVIL_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return AMSoundRegistry.TASMANIAN_DEVIL_HURT.get();
    }

    @Override
    protected void initGoals() {
        super.initGoals();
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new MeleeAttackGoal(this, 1.5D, true));
        this.goalSelector.add(2, new TemptGoal(this, 1.1D, Ingredient.fromTag(AMTagRegistry.TASMANIAN_DEVIL_HOWLING_FOODS), false){
            @Override
            public void tick(){
                super.tick();
                if(EntityTasmanianDevil.this.getAnimation() == NO_ANIMATION){
                    EntityTasmanianDevil.this.setBasking(false);
                    EntityTasmanianDevil.this.setSitting(false);
                }
            }
        });
        this.goalSelector.add(3, new WanderAroundGoal(this, 1D, 60));
        this.goalSelector.add(4, new AnimalMateGoal(this, 1.0D));
        this.goalSelector.add(5, new LookAtEntityGoal(this, PlayerEntity.class, 6.0F));
        this.goalSelector.add(6, new LookAroundGoal(this));
        this.targetSelector.add(1, (new RevengeGoal(this, EntityTasmanianDevil.class)).setGroupRevenge());
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, AnimalEntity.class, 120, false, false, (p_213487_0_) -> p_213487_0_ instanceof ChickenEntity || p_213487_0_ instanceof RabbitEntity));
        this.targetSelector.add(3, new CreatureAITargetItems<>(this, false, 30));
    }

    public void killed(ServerWorld world, LivingEntity entity) {
        if(this.getRandom().nextBoolean() && (entity instanceof AnimalEntity || entity.getGroup() == EntityGroup.UNDEAD)){
            entity.dropStack(new ItemStack(Items.BONE));
        }
    }

    @Override
    public void travel(Vec3d vec3d) {
        if (!this.shouldMove()) {
            if (this.getNavigation().getCurrentPath() != null) {
                this.getNavigation().stop();
            }
            vec3d = Vec3d.ZERO;
        }
        super.travel(vec3d);
    }

    public void setSitting(boolean sit) {
        this.dataTracker.set(SITTING, sit);
    }

    public boolean isSitting() {
        return this.dataTracker.get(SITTING);
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(BASKING, false);
        this.dataTracker.startTracking(SITTING, false);
    }

    public boolean isBasking() {
        return this.dataTracker.get(BASKING);
    }

    public void setBasking(boolean basking) {
        this.dataTracker.set(BASKING, basking);
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createLivingAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 14.0D)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.3F)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 2F);
    }

    @Override
    public boolean isBreedingItem(ItemStack stack) {
        return stack.getItem().isFood() && stack.getItem().getFoodComponent() != null && stack.getItem().getFoodComponent().isMeat() && !stack.isIn(AMTagRegistry.TASMANIAN_DEVIL_HOWLING_FOODS);
    }

    @Override
    public void tick(){
        super.tick();
        this.prevBaskProgress = this.baskProgress;
        this.prevSitProgress = this.sitProgress;

        if (this.isSitting()) {
            if (sitProgress < 5F)
                sitProgress++;
        } else {
            if (sitProgress > 0F)
                sitProgress--;
        }

        if (this.isBasking()) {
            if (baskProgress < 5F)
                baskProgress++;
        } else {
            if (baskProgress > 0F)
                baskProgress--;
        }

        if (!this.getWorld().isClient) {
            if (this.getTarget() != null && this.getAnimation() == ANIMATION_ATTACK && this.getAnimationTick() == 5 && this.canSee(this.getTarget())) {
                float f1 = this.getYaw() * MathHelper.RADIANS_PER_DEGREE;
                this.setVelocity(this.getVelocity().add(-MathHelper.sin(f1) * 0.02F, 0.0D, MathHelper.cos(f1) * 0.02F));
                getTarget().takeKnockback(1F, getTarget().getX() - this.getX(), getTarget().getZ() - this.getZ());
                this.getTarget().damage(this.getDamageSources().mobAttack(this), (float) this.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE).getBaseValue());
            }
            if ((isSitting() || isBasking()) && ++sittingTime > maxSitTime) {
                this.setSitting(false);
                this.setBasking(false);
                sittingTime = 0;
                maxSitTime = 75 + random.nextInt(50);
            }
            if (this.getVelocity().lengthSquared() < 0.03D && this.getAnimation() == NO_ANIMATION && !this.isBasking() && !this.isSitting() && random.nextInt(100) == 0) {
                sittingTime = 0;
                maxSitTime = 100 + random.nextInt(550);
                if (this.getRandom().nextBoolean()) {
                    this.setSitting(true);
                    this.setBasking(false);
                } else {
                    this.setSitting(false);
                    this.setBasking(true);
                }
            }
        }
        if(this.getAnimation() == ANIMATION_HOWL && this.getAnimationTick() == 1){
            this.emitGameEvent(GameEvent.ENTITY_ROAR);
            this.playSound(AMSoundRegistry.TASMANIAN_DEVIL_ROAR.get(), this.getSoundVolume() * 2F, this.getSoundPitch());
        }
        if(this.getAnimation() == ANIMATION_HOWL && this.getAnimationTick() > 3){
            scareMobsTime = 40;
        }
        if(scareMobsTime > 0) {
            var list = this.getWorld().getNonSpectatingEntities(HostileEntity.class, this.getBoundingBox().expand(16, 8, 16));
            for (var e : list) {
                e.setTarget(null);
                e.setAttacker(null);
                if(scareMobsTime % 5 == 0){
                    var vec = FuzzyTargeting.findFrom(e, 20, 7, this.getPos());
                    if(vec != null){
                        e.getNavigation().startMovingTo(vec.x, vec.y, vec.z, 1.5D);
                    }
                }

            }
            scareMobsTime--;
        }
        if(this.getTarget() != null && this.getTarget().isAlive() && (this.getAttacker() == null || !this.getAttacker().isAlive()) ){
            this.setAttacker(this.getTarget());
        }
        if((this.isSitting() || this.isBasking()) && (this.getTarget() != null || this.isInLove())) {
            this.setSitting(false);
            this.setBasking(false);
        }
        AnimationHandler.INSTANCE.updateAnimations(this);
    }

    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        ItemStack itemstack = player.getStackInHand(hand);
        Item item = itemstack.getItem();
        ActionResult type = super.interactMob(player, hand);
        if (itemstack.isIn(AMTagRegistry.TASMANIAN_DEVIL_HOWLING_FOODS) && this.getAnimation() != ANIMATION_HOWL) {
            this.emitGameEvent(GameEvent.EAT);
            this.playSound(SoundEvents.ENTITY_FOX_EAT, this.getSoundVolume(), this.getSoundPitch());
            this.dropItem(item.getRecipeRemainder());
            if (!player.isCreative()) {
                itemstack.decrement(1);
            }
            this.setAnimation(ANIMATION_HOWL);
            return ActionResult.SUCCESS;
        }
        return type;
    }

    @Override
    public boolean tryAttack(Entity entityIn) {
        if (this.getAnimation() == NO_ANIMATION) {
            this.setAnimation(ANIMATION_ATTACK);
        }
        return true;
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
        if(animation == ANIMATION_HOWL){
            this.setSitting(true);
            this.setBasking(false);
            maxSitTime = Math.max(25, maxSitTime);
        }
    }

    @Override
    public Animation[] getAnimations() {
        return new Animation[]{ANIMATION_ATTACK, ANIMATION_HOWL};
    }

    @Nullable
    @Override
    public PassiveEntity createChild(ServerWorld world, PassiveEntity entity) {
        return AMEntityRegistry.TASMANIAN_DEVIL.get().create(world);
    }

    @Override
    public boolean canTargetItem(ItemStack stack) {
        return stack.getItem().isFood() && stack.getItem().getFoodComponent() != null && stack.getItem().getFoodComponent().isMeat() || stack.getItem() == Items.BONE;
    }

    @Override
    public void onGetItem(ItemEntity e) {
        this.emitGameEvent(GameEvent.EAT);
        if(e.getStack().getItem() == Items.BONE){
            dropBonemeal();
            this.playSound(SoundEvents.ENTITY_SKELETON_STEP, this.getSoundVolume(), this.getSoundPitch());
        } else{
            this.playSound(SoundEvents.ENTITY_FOX_EAT, this.getSoundVolume(), this.getSoundPitch());
            this.heal(5);
        }
    }

    public void dropBonemeal(){
        var stack = new ItemStack(Items.BONE_MEAL);
        for(int i = 0; i < 3 + random.nextInt(1); i++){
            this.dropStack(stack);
        }
    }
}
