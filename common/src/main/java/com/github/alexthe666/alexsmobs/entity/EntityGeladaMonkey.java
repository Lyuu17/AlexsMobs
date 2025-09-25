package com.github.alexthe666.alexsmobs.entity;

import com.github.alexthe666.alexsmobs.config.AMConfig;
import com.github.alexthe666.alexsmobs.entity.ai.AnimalAIHerdPanic;
import com.github.alexthe666.alexsmobs.entity.ai.GeladaAIGroom;
import com.github.alexthe666.alexsmobs.misc.IngredientUtil;
import com.github.alexthe666.alexsmobs.registry.AMEntityRegistry;
import com.github.alexthe666.alexsmobs.registry.AMSoundRegistry;
import com.github.alexthe666.alexsmobs.registry.AMTagRegistry;
import com.iafenvoy.uranus.animation.Animation;
import com.iafenvoy.uranus.animation.AnimationHandler;
import com.iafenvoy.uranus.animation.IAnimatedEntity;
import net.minecraft.entity.*;
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
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.Random;

public class EntityGeladaMonkey extends AnimalEntity implements IAnimatedEntity, IHerdPanic {

    public static final Animation ANIMATION_SWIPE_R = Animation.create(13);
    public static final Animation ANIMATION_SWIPE_L = Animation.create(13);
    public static final Animation ANIMATION_GROOM = Animation.create(35);
    public static final Animation ANIMATION_CHEST = Animation.create(35);
    private static final TrackedData<Boolean> LEADER = DataTracker.registerData(EntityGeladaMonkey.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> SITTING = DataTracker.registerData(EntityGeladaMonkey.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> HAS_TARGET = DataTracker.registerData(EntityGeladaMonkey.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Integer> GRASS_TIME = DataTracker.registerData(EntityGeladaMonkey.class, TrackedDataHandlerRegistry.INTEGER);
    public float prevSitProgress;
    public float sitProgress;
    public boolean isGrooming = false;
    public int groomerID = -1;
    private int animationTick;
    private Animation currentAnimation;
    private int sittingTime;
    private int maxSitTime;
    private int leaderFightTime;
    private RevengeGoal RevengeGoal = null;
    private ActiveTargetGoal<EntityGeladaMonkey> leaderFightGoal = null;
    private int revengeCooldown = 0;
    private boolean hasSpedUp = false;

    public EntityGeladaMonkey(EntityType<EntityGeladaMonkey> type, World lvl) {
        super(type, lvl);
        this.setPathfindingPenalty(PathNodeType.WATER, -1.0F);
    }

    @Override
    public boolean canSpawn(WorldAccess worldIn, SpawnReason spawnReasonIn) {
        return AMEntityRegistry.rollSpawn(AMConfig.geladaMonkeySpawnRolls, this.getRandom(), spawnReasonIn);
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createLivingAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 18.0D)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 4.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.25F)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32.0D);
    }

    @Override
    public int getLimitPerChunk() {
        return 10;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return AMSoundRegistry.GELADA_MONKEY_IDLE.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return AMSoundRegistry.GELADA_MONKEY_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return AMSoundRegistry.GELADA_MONKEY_HURT.get();
    }

    @Override
    protected void initGoals() {
        super.initGoals();
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new MeleeAttackGoal(this, 1.5D, true) {
            @Override
            protected double getSquaredMaxAttackDistance(LivingEntity attackTarget) {
                return super.getSquaredMaxAttackDistance(attackTarget) + 1.5D;
            }

            @Override
            public boolean canStart() {
                return super.canStart() && EntityGeladaMonkey.this.revengeCooldown <= 0;
            }

            @Override
            public boolean shouldContinue() {
                return super.shouldContinue() && EntityGeladaMonkey.this.revengeCooldown <= 0;
            }
        });
        this.goalSelector.add(2, new AIClearGrass());
        this.goalSelector.add(3, new AnimalAIHerdPanic(this, 1.5D));
        this.goalSelector.add(4, new FollowParentGoal(this, 1.1D));
        this.goalSelector.add(5, new AnimalMateGoal(this, 1.0D));
        this.goalSelector.add(6, new TemptGoal(this, 1.0D, IngredientUtil.ingredientFromTags(AMTagRegistry.GELADA_MONKEY_BREEDABLES, AMTagRegistry.GELADA_MONKEY_LAND_CLEARING_FOODS), false));
        this.goalSelector.add(7, new GeladaAIGroom(this));
        this.goalSelector.add(8, new WanderAroundGoal(this, 1D, 120));
        this.goalSelector.add(9, new LookAtEntityGoal(this, PlayerEntity.class, 6.0F));
        this.goalSelector.add(10, new LookAroundGoal(this));
        this.targetSelector.add(1, RevengeGoal = (new RevengeGoal(this, EntityGeladaMonkey.class).setGroupRevenge()));
        this.targetSelector.add(2, leaderFightGoal = new ActiveTargetGoal<>(this, EntityGeladaMonkey.class, 70, false, false, (monkey) -> EntityGeladaMonkey.this.isLeader() && EntityGeladaMonkey.this.leaderFightTime == 0 && ((EntityGeladaMonkey) monkey).isLeader() && ((EntityGeladaMonkey) monkey).leaderFightTime == 0));
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound compound) {
        super.writeCustomDataToNbt(compound);
        compound.putBoolean("Leader", this.isLeader());
        compound.putInt("GrassTime", this.getClearGrassTime());
        compound.putInt("FightTime", this.leaderFightTime);
        compound.putBoolean("MonkeySitting", this.isSitting());
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound compound) {
        super.readCustomDataFromNbt(compound);
        this.setLeader(compound.getBoolean("Leader"));
        this.setClearGrassTime(compound.getInt("GrassTime"));
        this.setSitting(compound.getBoolean("MonkeySitting"));
        this.leaderFightTime = compound.getInt("FightTime");
    }

    @Override
    public boolean isBreedingItem(ItemStack stack) {
        return stack.isIn(AMTagRegistry.GELADA_MONKEY_BREEDABLES);
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(LEADER, false);
        this.dataTracker.startTracking(SITTING, false);
        this.dataTracker.startTracking(HAS_TARGET, false);
        this.dataTracker.startTracking(GRASS_TIME, 0);
    }

    public boolean isLeader() {
        return this.dataTracker.get(LEADER) && !this.isBaby();
    }

    public void setLeader(boolean leader) {
        this.dataTracker.set(LEADER, leader);
    }

    public boolean isSitting() {
        return this.dataTracker.get(SITTING);
    }

    public void setSitting(boolean sit) {
        this.dataTracker.set(SITTING, sit);
    }

    public boolean isAggro() {
        return this.dataTracker.get(HAS_TARGET);
    }

    public void setAggro(boolean sit) {
        this.dataTracker.set(HAS_TARGET, sit);
    }

    public int getClearGrassTime() {
        return this.dataTracker.get(GRASS_TIME);
    }

    public void setClearGrassTime(int i) {
        this.dataTracker.set(GRASS_TIME, i);
    }

    @Override
    public void tick() {
        super.tick();
        this.prevSitProgress = this.sitProgress;

        if (this.isSitting()) {
            if (sitProgress < 5F)
                sitProgress++;
        } else {
            if (sitProgress > 0F)
                sitProgress--;
        }

        if (!this.getWorld().isClient) {
            if (isSitting() && ++sittingTime > maxSitTime) {
                this.setSitting(false);
                sittingTime = 0;
                maxSitTime = 75 + random.nextInt(50);
            }
            if (this.getVelocity().lengthSquared() < 0.03D && this.getAnimation() == NO_ANIMATION && !this.isSitting() && random.nextInt(500) == 0) {
                sittingTime = 0;
                maxSitTime = 200 + random.nextInt(550);
                this.setSitting(true);
            }
            if (this.isSitting() && (this.getTarget() != null || this.isInLove())) {
                this.setSitting(false);
            }
            if (this.getTarget() != null && (this.getAnimation() == ANIMATION_SWIPE_L || this.getAnimation() == ANIMATION_SWIPE_R) && this.getAnimationTick() == 7 && this.canSee(this.getTarget()) && this.distanceTo(this.getTarget()) < this.getHeight() + this.getTarget().getHeight() + 1) {
                getTarget().takeKnockback(0.4F, getTarget().getX() - this.getX(), getTarget().getZ() - this.getZ());
                float dmg = (float) this.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE).getBaseValue();
                if (this.isLeader() && getTarget() instanceof EntityGeladaMonkey monkey) {
                    if (monkey.isLeader()) {
                        monkey.setTarget(this);
                        monkey.leaderFightTime = this.leaderFightTime;
                        dmg = 0;
                    }
                }
                this.getTarget().damage(this.getDamageSources().mobAttack(this), dmg);
            }
            if (this.getTarget() != null && this.getTarget().isAlive()) {
                this.setAggro(true);
                if (this.isLeader() && this.getTarget() instanceof EntityGeladaMonkey monkey) {
                    if (monkey.isLeader()) {
                        this.leaderFightTime++;
                    }
                    if(leaderFightTime < 10 && random.nextInt(5) == 0 && this.getAnimation() == NO_ANIMATION){
                        this.setAnimation(ANIMATION_CHEST);
                    }
                    if (Math.max(this.leaderFightTime, monkey.leaderFightTime) >= 250) {
                        this.resetAttackAI();
                        monkey.resetAttackAI();
                    }
                }

            } else {
                this.setAggro(false);
            }
            if (this.leaderFightTime < 0) {
                this.leaderFightTime++;
            }
        }

        if (isAggro()) {
            if (!hasSpedUp) {
                hasSpedUp = true;
                this.setSprinting(true);
                this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).setBaseValue(0.31F);
            }
        } else {
            if (hasSpedUp) {
                hasSpedUp = false;
                this.setSprinting(false);
                this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).setBaseValue(0.25F);
            }
        }

        if (getClearGrassTime() > 0) {
            setClearGrassTime(getClearGrassTime() - 1);
        }
        if (getClearGrassTime() < 0) {
            setClearGrassTime(getClearGrassTime() + 1);
        }
        AnimationHandler.INSTANCE.updateAnimations(this);
    }

    private void resetAttackAI() {
        this.leaderFightTime = -500 - random.nextInt(2000);
        this.setTarget(null);
        this.setAttacker(null);
        if (leaderFightGoal != null) {
            leaderFightGoal.stop();
        }
        if (RevengeGoal != null) {
            RevengeGoal.stop();
        }
    }

    @Override
    public boolean tryAttack(Entity entityIn) {
        if (this.getAnimation() == NO_ANIMATION) {
            attackAnimation();
        }
        return true;
    }

    public float getGeladaScale() {
        return isBaby() ? 0.5F : isLeader() ? 1.15F : 1.0F;
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
        return new Animation[]{ANIMATION_SWIPE_R, ANIMATION_SWIPE_L, ANIMATION_GROOM, ANIMATION_CHEST};
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        boolean prev = super.damage(source, amount);
        if (prev) {
            Entity direct = source.getAttacker();
            if (direct instanceof EntityGeladaMonkey) {
                int fleeTime = 100 + getRandom().nextInt(5);
                this.revengeCooldown = fleeTime;
                this.revengeCooldown = 10 + getRandom().nextInt(30);
            }
        }
        return prev;
    }

    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        var itemstack = player.getStackInHand(hand);
        var type = super.interactMob(player, hand);
        if(itemstack.isIn(AMTagRegistry.GELADA_MONKEY_LAND_CLEARING_FOODS) && this.getClearGrassTime() == 0){
            this.eat(player, hand, itemstack);
            this.eatGrassWithBuddies(3 + random.nextInt(2));
            return ActionResult.SUCCESS;
        }
        return type;
    }

    @Nullable
    @Override
    public PassiveEntity createChild(ServerWorld lvl, PassiveEntity mob) {
        EntityGeladaMonkey baby = AMEntityRegistry.GELADA_MONKEY.get().create(lvl);
        baby.setLeader(random.nextInt(2) == 0);
        return baby;
    }

    public void eatGrassWithBuddies(int otherMonkies){
        int i = 300 + random.nextInt(300);
        this.setClearGrassTime(i);
        int monky = 0;
        for (var entity : this.getWorld().getNonSpectatingEntities(EntityGeladaMonkey.class, this.getBoundingBox().expand(15F))) {
            if (monky < otherMonkies && entity.getId() != this.getId() && !entity.shouldStopBeingGroomed()) {
                monky++;
                entity.setClearGrassTime(i);
            }
        }
    }

    @Override
    public void onPanic() {

    }

    @Override
    public boolean canPanic() {
        return this.getAttacker() instanceof EntityGeladaMonkey && this.random.nextInt(3) == 0;
    }

    @Override
    public void travel(Vec3d vec3d) {
        if (this.isSitting() || this.getAnimation() == ANIMATION_CHEST) {
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
        if (spawnDataIn instanceof PassiveData pack) {
            if (pack.getSpawnedCount() == 0 || pack.getSpawnedCount() > 4 && random.nextInt(2) == 0) {
                this.setLeader(true);
            }
        } else {
            this.setLeader(this.getRandom().nextInt(4) == 0);
        }

        return super.initialize(worldIn, difficultyIn, reason, spawnDataIn, dataTag);
    }

    public boolean canBeGroomed() {
        return this.groomerID == -1;
    }

    public boolean shouldStopBeingGroomed() {
        return this.getTarget() != null && this.getTarget().isAlive() || this.isInLove() || this.revengeCooldown > 0;
    }

    private class AIClearGrass extends Goal {

        private BlockPos target;

        public AIClearGrass() {
            this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
        }

        @Override
        public boolean canStart() {
            if (EntityGeladaMonkey.this.getClearGrassTime() > 0) {
                target = generateTarget();
                return target != null;
            }
            return false;
        }

        @Override
        public boolean shouldContinue() {
            return target != null && EntityGeladaMonkey.this.getWorld().getBlockState(target).isIn(AMTagRegistry.GELADA_MONKEY_GRASS);
        }

        @Override
        public void tick() {
            EntityGeladaMonkey.this.setSitting(false);
            EntityGeladaMonkey.this.getNavigation().startMovingTo(target.getX() + 0.5F, target.getY() + 0.5F, target.getZ() + 0.5F, 1.4F);
            if (EntityGeladaMonkey.this.squaredDistanceTo(Vec3d.ofCenter(target)) < 3.4F) {
                if(EntityGeladaMonkey.this.getAnimation() == NO_ANIMATION){
                    EntityGeladaMonkey.this.attackAnimation();
                }else if(EntityGeladaMonkey.this.getAnimationTick() > 7){
                    EntityGeladaMonkey.this.getWorld().breakBlock(target, true);
                }
            }
        }

        public BlockPos generateTarget() {
            BlockPos blockpos = null;
            Random random = new Random();
            int range = 7;
            for (int i = 0; i < 15; i++) {
                var blockpos1 = EntityGeladaMonkey.this.getBlockPos().add(random.nextInt(range) - range / 2, 3, random.nextInt(range) - range / 2);
                while (EntityGeladaMonkey.this.getWorld().isAir(blockpos1) && blockpos1.getY() > -63) {
                    blockpos1 = blockpos1.down();
                }
                if (EntityGeladaMonkey.this.getWorld().getBlockState(blockpos1).isIn(AMTagRegistry.GELADA_MONKEY_GRASS)) {
                    blockpos = blockpos1;
                }
            }
            return blockpos;
        }
    }

    private void attackAnimation() {
        this.setAnimation(random.nextBoolean() ? ANIMATION_SWIPE_L : ANIMATION_SWIPE_R);
    }
}
