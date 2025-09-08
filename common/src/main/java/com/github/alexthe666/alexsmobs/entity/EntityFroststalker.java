package com.github.alexthe666.alexsmobs.entity;

import com.github.alexthe666.alexsmobs.config.AMConfig;
import com.github.alexthe666.alexsmobs.entity.ai.*;
import com.github.alexthe666.alexsmobs.registry.AMEntityRegistry;
import com.github.alexthe666.alexsmobs.registry.AMItemRegistry;
import com.github.alexthe666.alexsmobs.registry.AMSoundRegistry;
import com.github.alexthe666.alexsmobs.registry.AMTagRegistry;
import com.iafenvoy.uranus.animation.Animation;
import com.iafenvoy.uranus.animation.AnimationHandler;
import com.iafenvoy.uranus.animation.IAnimatedEntity;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.enchantment.FrostWalkerEnchantment;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.FuzzyTargeting;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributeModifier;
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
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.tag.BiomeTags;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;
import java.util.stream.Stream;

public class EntityFroststalker extends AnimalEntity implements IAnimatedEntity, ISemiAquatic {

    public static final Identifier SPIKED_LOOT = new Identifier("alexsmobs", "entities/froststalker_spikes");
    public static final Animation ANIMATION_BITE = Animation.create(13);
    public static final Animation ANIMATION_SPEAK = Animation.create(11);
    public static final Animation ANIMATION_SLASH_L = Animation.create(12);
    public static final Animation ANIMATION_SLASH_R = Animation.create(12);
    public static final Animation ANIMATION_SHOVE = Animation.create(12);
    private static final TrackedData<Boolean> SPIKES = DataTracker.registerData(EntityFroststalker.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> TACKLING = DataTracker.registerData(EntityFroststalker.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> SPIKE_SHAKING = DataTracker.registerData(EntityFroststalker.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> BIPEDAL = DataTracker.registerData(EntityFroststalker.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Float> TURN_ANGLE = DataTracker.registerData(EntityFroststalker.class, TrackedDataHandlerRegistry.FLOAT);
    public static final Predicate<PlayerEntity> VALID_LEADER_PLAYERS = (player) -> player.getEquippedStack(EquipmentSlot.HEAD).isOf(AMItemRegistry.FROSTSTALKER_HELMET.get());
    public float bipedProgress;
    public float prevBipedProgress;
    public float tackleProgress;
    public float prevTackleProgress;
    public float spikeShakeProgress;
    public float prevSpikeShakeProgress;
    public float prevTurnAngle;
    private int animationTick;
    private Animation currentAnimation;
    private int standingTime = 400 - random.nextInt(700);
    private int currentSpeedMode = -1;
    private LivingEntity leader;
    private int packSize = 1;
    private int shakeTime = 0;
    private boolean hasSpikedArmor = false;
    private int fleeFireFlag;
    private int resetLeaderCooldown = 100;

    public EntityFroststalker(EntityType<? extends AnimalEntity> type, World level) {
        super(type, level);
        this.setPathfindingPenalty(PathNodeType.LAVA, -1.0F);
        this.setPathfindingPenalty(PathNodeType.DANGER_FIRE, -1.0F);
        this.setPathfindingPenalty(PathNodeType.DAMAGE_FIRE, -1.0F);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return AMSoundRegistry.FROSTSTALKER_IDLE.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return AMSoundRegistry.FROSTSTALKER_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return AMSoundRegistry.FROSTSTALKER_HURT.get();
    }

    @Override
    public boolean canSpawn(WorldAccess worldIn, SpawnReason spawnReasonIn) {
        return AMEntityRegistry.rollSpawn(AMConfig.froststalkerSpawnRolls, this.getRandom(), spawnReasonIn);
    }

    public static boolean canFroststalkerSpawn(EntityType<? extends AnimalEntity> animal, WorldAccess worldIn, SpawnReason reason, BlockPos pos, Random random) {
        return worldIn.getBaseLightLevel(pos, 0) > 8 && (worldIn.getBlockState(pos.down()).isIn(AMTagRegistry.FROSTSTALKER_SPAWNS) || worldIn.getBlockState(pos.down()).isSolid());
    }

    @Nullable
    protected Identifier getLootTableId() {
        return this.hasSpikes() ? SPIKED_LOOT : super.getLootTableId();
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createLivingAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 24D)
                .add(EntityAttributes.GENERIC_ARMOR, 2.0D)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 4.5D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.3F)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32.0D);
    }

    @Override
    protected void fall(double y, boolean onGroundIn, BlockState state, BlockPos pos) {
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        if(source.isIn(DamageTypeTags.IS_FIRE)){
            amount *= 2F;
        }
        boolean prev = super.damage(source, amount);
        if (prev && this.hasSpikes() && !this.isSpikeShaking() && source.getAttacker() != null && source.getAttacker().distanceTo(this) < 10) {
            this.setSpikeShaking(true);
            shakeTime = 20 + random.nextInt(60);
            standFor(shakeTime + 10);
        }
        return prev;
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this) {
            @Override
            public void tick() {
                if (EntityFroststalker.this.getRandom().nextFloat() < 0.8F) {
                    if (EntityFroststalker.this.hasSpikes()) {
                        EntityFroststalker.this.jumpUnderwater();
                    } else {
                        EntityFroststalker.this.getJumpControl().setActive();
                    }
                }
            }
        });
        this.goalSelector.add(1, new AIAvoidFire());
        this.goalSelector.add(2, new FroststalkerAIMelee(this));
        this.goalSelector.add(3, new AnimalMateGoal(this, 1.0D));
        this.goalSelector.add(4, new FollowParentGoal(this, 1.1D));
        this.goalSelector.add(5, new FroststalkerAIFollowLeader(this));
        this.goalSelector.add(6, new AnimalAIFindWater(this));
        this.goalSelector.add(7, new AnimalAILeaveWater(this));
        this.goalSelector.add(8, new AnimalAIWanderRanged(this, 90, 1.0D, 7, 7));
        this.goalSelector.add(9, new LookAtEntityGoal(this, LivingEntity.class, 15.0F));
        this.goalSelector.add(9, new LookAroundGoal(this));
        this.targetSelector.add(1, (new RevengeGoal(this, EntityFroststalker.class)).setGroupRevenge());
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, LivingEntity.class, 40, false, true, AMEntityRegistry.buildPredicateFromTag(AMTagRegistry.FROSTSTALKER_TARGETS)));
        this.targetSelector.add(3, new ActiveTargetGoal<>(this, PlayerEntity.class, 80, false, true, (livingEntity -> !livingEntity.getEquippedStack(EquipmentSlot.HEAD).isOf(AMItemRegistry.FROSTSTALKER_HELMET.get()))));
    }

    private void jumpUnderwater() {
        var pos = this.getSteppingPos();
        if(this.getWorld().isWater(pos) && !this.getWorld().isWater(pos.up())){
            this.setPos(this.getX(), this.getY() + 1, this.getZ());
            this.getWorld().setBlockState(pos, Blocks.FROSTED_ICE.getDefaultState());
            this.getWorld().scheduleBlockTick(pos, Blocks.FROSTED_ICE, MathHelper.nextInt(this.getRandom(), 60, 120));
        }
        double d0 = 0.2F;
        var vec3 = this.getVelocity();
        this.setVelocity(vec3.x, d0, vec3.z);
    }

    @Override
    public void lovePlayer(@Nullable PlayerEntity player) {
        if(player != null && isValidLeader(player)){
            super.lovePlayer(player);
        }
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(TURN_ANGLE, 0F);
        this.dataTracker.startTracking(SPIKES, true);
        this.dataTracker.startTracking(BIPEDAL, false);
        this.dataTracker.startTracking(SPIKE_SHAKING, false);
        this.dataTracker.startTracking(TACKLING, false);
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound compound) {
        super.writeCustomDataToNbt(compound);
        compound.putBoolean("Spiked", this.hasSpikes());
        compound.putBoolean("Bipedal", this.isBipedal());
        compound.putBoolean("SpikeShaking", this.isSpikeShaking());
        compound.putInt("StandingTime", standingTime);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound compound) {
        super.readCustomDataFromNbt(compound);
        this.setSpiked(compound.getBoolean("Spiked"));
        this.setBipedal(compound.getBoolean("Bipedal"));
        this.setSpikeShaking(compound.getBoolean("SpikeShaking"));
        this.standingTime = compound.getInt("StandingTime");
    }

    @Override
    public BlockPos getPositionTarget() {
        return this.leader == null ? super.getPositionTarget() : this.leader.getSteppingPos();
    }

    @Override
    public boolean hasPositionTarget() {
        return this.isFollower();
    }

    @Override
    public boolean isBreedingItem(ItemStack stack) {
        return stack.isIn(AMTagRegistry.FROSTSTALKER_BREEDABLES);
    }

    @Override
    public void tick() {
        super.tick();
        this.prevBipedProgress = bipedProgress;
        this.prevTackleProgress = tackleProgress;
        this.prevSpikeShakeProgress = spikeShakeProgress;
        this.prevTurnAngle = getTurnAngle();

        if (this.isBipedal()) {
            if (bipedProgress < 5.0F)
                bipedProgress++;
        } else {
            if (bipedProgress > 0F)
                bipedProgress--;
        }

        if (this.isTackling()) {
            if (tackleProgress < 5.0F)
                tackleProgress++;
        } else {
            if (tackleProgress > 0F)
                tackleProgress--;
        }

        if (this.isSpikeShaking()) {
            if (spikeShakeProgress < 5.0F)
                spikeShakeProgress++;

            if (currentSpeedMode != 2) {
                currentSpeedMode = 2;
                this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).setBaseValue(0.1F);
            }
        } else {
            if (spikeShakeProgress > 0F)
                spikeShakeProgress--;

            if (isBipedal()) {
                if (currentSpeedMode != 0) {
                    currentSpeedMode = 0;
                    this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).setBaseValue(0.35F);
                }
            } else {
                if (currentSpeedMode != 1) {
                    currentSpeedMode = 1;
                    this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).setBaseValue(0.25F);
                }
            }
        }

        if (this.hasSpikes()) {
            if (!hasSpikedArmor) {
                hasSpikedArmor = true;
                this.getAttributeInstance(EntityAttributes.GENERIC_ARMOR).setBaseValue(12F);
            }
        } else {
            if (hasSpikedArmor) {
                hasSpikedArmor = false;
                this.getAttributeInstance(EntityAttributes.GENERIC_ARMOR).setBaseValue(0F);
            }
        }

        if (!this.getWorld().isClient) {
            if (this.age % 200 == 0) {
                if (isWet() && !this.hasSpikes()) {
                    this.setSpiked(true);
                }
                if (this.isHotBiome() && !isWet()) {
                    this.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 400));
                    if (random.nextInt(2) == 0 && !this.isWet()) {
                        this.setSpiked(false);
                    }
                }
            }
            float threshold = 1F;
            boolean flag = false;
            if (isBipedal() && this.prevYaw - this.getYaw() > threshold) {
                this.setTurnAngle(this.getTurnAngle() + 5);
                flag = true;
            }
            if (isBipedal() && this.prevYaw - this.getYaw() < -threshold) {
                this.setTurnAngle(this.getTurnAngle() - 5);
                flag = true;
            }
            if (!flag) {
                if (this.getTurnAngle() > 0) {
                    this.setTurnAngle(Math.max(this.getTurnAngle() - 10, 0));
                }
                if (this.getTurnAngle() < 0) {
                    this.setTurnAngle(Math.min(this.getTurnAngle() + 10, 0));
                }
            }
            this.setTurnAngle(MathHelper.clamp(this.getTurnAngle(), -60, 60));
            if (standingTime > 0) {
                standingTime--;
            }
            if (standingTime < 0) {
                standingTime++;
            }

            if (standingTime <= 0 && this.isBipedal()) {
                standingTime = -200 - random.nextInt(400);
                this.setBipedal(false);
            }
            if (standingTime == 0 && !this.isBipedal() && this.getVelocity().lengthSquared() >= 0.03D) {
                standingTime = 200 + random.nextInt(600);
                this.setBipedal(true);
            }
            if (shakeTime > 0) {
                if (this.shakeTime % 5 == 0) {
                    int spikeCount = 2 + random.nextInt(4);
                    for (int i = 0; i < spikeCount; i++) {
                        float f = ((i + 1) / (float) spikeCount) * 360F;
                        var shard = new EntityIceShard(getWorld(), this);
                        shard.shootFromRotation(this, this.getPitch() - random.nextInt(40), f, 0.0F, 0.15F + random.nextFloat() * 0.2F, 1.0F);
                        getWorld().spawnEntity(shard);
                    }
                }
                shakeTime--;
            }
            if (shakeTime == 0 && this.isSpikeShaking()) {
                this.setSpikeShaking(false);
                if (random.nextInt(2) == 0) {
                    this.setSpiked(false);
                }
            }
            if (this.getTarget() != null && isValidLeader(this.getTarget())) {
                this.setTarget(null);
            }
            //Makes entire pack attack target
            if (this.getTarget() != null && !isValidLeader(this.getTarget()) && this.getTarget().isAlive() && (this.getAttacker() == null || !this.getAttacker().isAlive())) {
                this.setAttacker(this.getTarget());
            }
            LivingEntity playerTarget = null;
            if (leader instanceof PlayerEntity) {
                playerTarget = leader.getAttacking();
                if (playerTarget == null || !playerTarget.isAlive() || playerTarget instanceof EntityFroststalker) {
                    playerTarget = leader.getAttacker();
                }
            }
            if (playerTarget != null && playerTarget.isAlive() && !(playerTarget instanceof EntityFroststalker)) {
                this.setTarget(playerTarget);
            }
            boolean attackAnim = this.getAnimation() == ANIMATION_BITE && this.getAnimationTick() == 5 ||
                    this.getAnimation() == ANIMATION_SHOVE && this.getAnimationTick() == 8 ||
                    this.getAnimation() == ANIMATION_SLASH_L && this.getAnimationTick() == 7 ||
                    this.getAnimation() == ANIMATION_SLASH_R && this.getAnimationTick() == 7;
            if (this.getTarget() != null && attackAnim) {
                getTarget().takeKnockback(0.2F, getTarget().getX() - this.getX(), getTarget().getZ() - this.getZ());
                this.getTarget().damage(this.getDamageSources().mobAttack(this), (float) this.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE));
            }
        }
        if (fleeFireFlag > 0) {
            fleeFireFlag--;
        }
        if(!this.getWorld().isClient){
            if(resetLeaderCooldown > 0){
                resetLeaderCooldown--;
            }else{
                resetLeaderCooldown = 200 + this.getRandom().nextInt(200);
                this.lookForPlayerLeader();
            }
        }
        AnimationHandler.INSTANCE.updateAnimations(this);
    }

    private void lookForPlayerLeader() {
       if(!(this.leader instanceof PlayerEntity)){
           float range = 10;
           var playerList = this.getWorld().getEntitiesByClass(PlayerEntity.class, this.getBoundingBox().expand(range, range, range), EntityFroststalker.VALID_LEADER_PLAYERS);
           PlayerEntity closestPlayer = null;
           for(PlayerEntity player : playerList){
               if(closestPlayer == null || player.distanceTo(this) < closestPlayer.distanceTo(this)){
                   closestPlayer = player;
               }
           }
           if(closestPlayer != null){
               this.stopFollowing();
               this.startFollowing(closestPlayer);
           }
       }
    }

    public boolean isFleeingFire(){
        return fleeFireFlag > 0;
    }

    public boolean isHotBiome() {
        if (this.isAiDisabled()) {
            return false;
        }
        if (this.getWorld().getRegistryKey() == World.NETHER) {
            return true;
        } else {
            int i = MathHelper.floor(this.getX());
            int k = MathHelper.floor(this.getZ());
            return this.getWorld().getBiome(new BlockPos(i, 0, k)).isIn(BiomeTags.SNOW_GOLEM_MELTS);
        }
    }

    public void standFor(int time) {
        this.setBipedal(true);
        standingTime = time;
    }

    @Override
    protected float getJumpVelocity() {
        return 0.52F * this.getJumpVelocityMultiplier();
    }

    @Override
    protected void jump() {
        double d0 = (double) this.getJumpVelocity() + this.getJumpBoostVelocityModifier();
        Vec3d vec3 = this.getVelocity();
        this.setVelocity(vec3.x, d0, vec3.z);
        float f = this.getYaw() * MathHelper.RADIANS_PER_DEGREE;
        this.setVelocity(this.getVelocity().add(-MathHelper.sin(f) * 0.2F, 0, MathHelper.cos(f) * 0.2F));
        this.velocityDirty = true;
        //FIXME
//        net.minecraftforge.common.ForgeHooks.onLivingJump(this);
    }

    public void frostJump() {
        jump();
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
        return new Animation[]{ANIMATION_BITE, ANIMATION_SPEAK, ANIMATION_SLASH_L, ANIMATION_SLASH_R, ANIMATION_SHOVE};
    }

    public float getTurnAngle() {
        return this.dataTracker.get(TURN_ANGLE);
    }

    public void setTurnAngle(float progress) {
        this.dataTracker.set(TURN_ANGLE, progress);
    }

    public boolean hasSpikes() {
        return this.dataTracker.get(SPIKES);
    }

    public void setSpiked(boolean bar) {
        this.dataTracker.set(SPIKES, bar);
    }

    public boolean isTackling() {
        return this.dataTracker.get(TACKLING);
    }

    public void setTackling(boolean bar) {
        this.dataTracker.set(TACKLING, bar);
    }

    public boolean isBipedal() {
        return this.dataTracker.get(BIPEDAL);
    }

    public void setBipedal(boolean bar) {
        this.dataTracker.set(BIPEDAL, bar);
    }

    public boolean isSpikeShaking() {
        return this.dataTracker.get(SPIKE_SHAKING);
    }

    public void setSpikeShaking(boolean bar) {
        this.dataTracker.set(SPIKE_SHAKING, bar);
    }

    public boolean isFollower() {
        return this.leader != null && isValidLeader(leader);
    }

    public boolean isValidLeader(LivingEntity leader) {
        if (leader instanceof PlayerEntity) {
            if (this.getAttacker() != null && this.getAttacker().equals(leader)) {
                return false;
            }
            return leader.getEquippedStack(EquipmentSlot.HEAD).isOf(AMItemRegistry.FROSTSTALKER_HELMET.get());
        } else {
            return leader.isAlive() && leader instanceof EntityFroststalker;
        }
    }

    @Override
    public boolean tryAttack(Entity entityIn) {
        if (this.getAnimation() == NO_ANIMATION) {
            final int anim = this.random.nextInt(4);
            switch (anim) {
                case 0 -> this.setAnimation(ANIMATION_SHOVE);
                case 1 -> this.setAnimation(ANIMATION_BITE);
                case 2 -> this.setAnimation(ANIMATION_SLASH_L);
                case 3 -> this.setAnimation(ANIMATION_SLASH_R);
            }
        }
        return true;
    }

    public LivingEntity startFollowing(LivingEntity leader) {
        this.leader = leader;
        if (leader instanceof EntityFroststalker) {
            ((EntityFroststalker) leader).addFollower();
        }
        return leader;
    }

    public void stopFollowing() {
        if (this.leader instanceof EntityFroststalker) {
            ((EntityFroststalker) this.leader).removeFollower();
        }
        this.leader = null;
    }

    private void addFollower() {
        ++this.packSize;
    }

    private void removeFollower() {
        --this.packSize;
    }

    public boolean canBeFollowed() {
        return this.hasFollowers() && this.packSize < getMaxPackSize() && isValidLeader(this);
    }

    public boolean hasFollowers() {
        return this.packSize > 1;
    }

    @Override
    public int getLimitPerChunk() {
        return 6;
    }

    public int getMaxPackSize() {
        return this.getLimitPerChunk();
    }

    public void addFollowers(Stream<EntityFroststalker> p_27534_) {
        p_27534_.limit(getMaxPackSize() - this.packSize).filter((p_27538_) -> p_27538_ != this).forEach((p_27536_) -> {
            p_27536_.startFollowing(this);
        });
    }

    public boolean inRangeOfLeader() {
        return this.distanceTo(this.leader) <= 60.0D;
    }

    public void pathToLeader() {
        if (this.isFollower()) {
            double speed = 1.0D;
            if (leader instanceof PlayerEntity) {
                speed = 1.3D;
                if (this.distanceTo(leader) > 24) {
                    speed = 1.4F;
                    this.standFor(20);
                }
            }
            if (this.distanceTo(leader) > 6 && this.getNavigation().isIdle()) {
                this.getNavigation().startMovingTo(this.leader, speed);
            }
        }
    }

    @Override
    protected void applyMovementEffects(BlockPos pos) {
        int i = EnchantmentHelper.getEquipmentLevel(Enchantments.FROST_WALKER, this);
        if (i > 0 || this.hasSpikes()) {
            FrostWalkerEnchantment.freezeWater(this, this.getWorld(), pos, i == 0 ? -1 : i);
        }
        if (this.shouldRemoveSoulSpeedBoost(this.getSteppingBlockState())) {
            this.removeSoulSpeedBoost();
        }
        this.addSoulSpeedBoostIfNeeded();
    }

    @Nullable
    @Override
    public EntityData initialize(ServerWorldAccess p_27528_, LocalDifficulty p_27529_, SpawnReason p_27530_, @Nullable EntityData p_27531_, @Nullable NbtCompound p_27532_) {
        //do not call super here
        this.getAttributeInstance(EntityAttributes.GENERIC_FOLLOW_RANGE).addPersistentModifier(new EntityAttributeModifier("Random spawn bonus", this.random.nextGaussian() * 0.05D, EntityAttributeModifier.Operation.MULTIPLY_BASE));
        if (p_27531_ == null) {
            p_27531_ = new SchoolEntityData(this);
        } else {
            this.startFollowing(((SchoolEntityData) p_27531_).leader);
        }

        return p_27531_;
    }

    @Nullable
    @Override
    public PassiveEntity createChild(ServerWorld p_146743_, PassiveEntity p_146744_) {
        return AMEntityRegistry.FROSTSTALKER.get().create(p_146743_);
    }

    @Override
    public boolean shouldEnterWater() {
        return !this.hasSpikes() && (this.getTarget() == null || !this.getTarget().isAlive());
    }

    @Override
    public boolean shouldLeaveWater() {
        return this.hasSpikes() || (this.getTarget() != null && this.getTarget().isAlive());
    }

    @Override
    public boolean shouldStopMoving() {
        return false;
    }

    @Override
    public int getWaterSearchRange() {
        return 10;
    }

    public static class SchoolEntityData implements EntityData {
        public final EntityFroststalker leader;

        public SchoolEntityData(EntityFroststalker p_27553_) {
            this.leader = p_27553_;
        }
    }


    private class AIAvoidFire extends Goal {
        private final int searchLength;
        private final int verticalSearchRange;
        protected BlockPos destinationBlock;
        protected int runDelay = 20;
        private Vec3d fleeTarget;

        private AIAvoidFire() {
            searchLength = 20;
            verticalSearchRange = 1;
        }

        @Override
        public boolean shouldContinue() {
            return destinationBlock != null && isFire(EntityFroststalker.this.getWorld(), destinationBlock.mutableCopy()) && isCloseToFire(16);
        }

        public boolean isCloseToFire(double dist) {
            return destinationBlock == null || EntityFroststalker.this.squaredDistanceTo(Vec3d.ofCenter(destinationBlock)) < dist * dist;
        }

        @Override
        public boolean canStart() {
            if (this.runDelay > 0) {
                --this.runDelay;
                return false;
            } else {
                this.runDelay = 30 + EntityFroststalker.this.random.nextInt(100);
                return this.searchForDestination();
            }
        }

        @Override
        public void start() {
            EntityFroststalker.this.fleeFireFlag = 200;
            var vec = FuzzyTargeting.findFrom(EntityFroststalker.this, 15, 5, Vec3d.ofCenter(destinationBlock));
            if (vec != null) {
                EntityFroststalker.this.standFor(100 + random.nextInt(100));
                fleeTarget = vec;
                EntityFroststalker.this.getNavigation().startMovingTo(vec.x, vec.y, vec.z, 1.2F);
            }
        }

        @Override
        public void tick() {
            if (this.isCloseToFire(16)) {
                EntityFroststalker.this.fleeFireFlag = 200;
                if (fleeTarget == null || EntityFroststalker.this.squaredDistanceTo(fleeTarget) < 2F) {
                    Vec3d vec = FuzzyTargeting.findFrom(EntityFroststalker.this, 15, 5, Vec3d.ofCenter(destinationBlock));
                    if (vec != null) {
                        fleeTarget = vec;
                    }
                }
                if (fleeTarget != null) {
                    EntityFroststalker.this.getNavigation().startMovingTo(fleeTarget.x, fleeTarget.y, fleeTarget.z, 1F);
                }
            }
        }

        @Override
        public void stop() {
            fleeTarget = null;
        }

        protected boolean searchForDestination() {
            int lvt_1_1_ = this.searchLength;
            int lvt_2_1_ = this.verticalSearchRange;
            BlockPos lvt_3_1_ = EntityFroststalker.this.getBlockPos();
            BlockPos.Mutable lvt_4_1_ = new BlockPos.Mutable();

            for (int lvt_5_1_ = -8; lvt_5_1_ <= 2; lvt_5_1_++) {
                for (int lvt_6_1_ = 0; lvt_6_1_ < lvt_1_1_; ++lvt_6_1_) {
                    for (int lvt_7_1_ = 0; lvt_7_1_ <= lvt_6_1_; lvt_7_1_ = lvt_7_1_ > 0 ? -lvt_7_1_ : 1 - lvt_7_1_) {
                        for (int lvt_8_1_ = lvt_7_1_ < lvt_6_1_ && lvt_7_1_ > -lvt_6_1_ ? lvt_6_1_ : 0; lvt_8_1_ <= lvt_6_1_; lvt_8_1_ = lvt_8_1_ > 0 ? -lvt_8_1_ : 1 - lvt_8_1_) {
                            lvt_4_1_.set(lvt_3_1_, lvt_7_1_, lvt_5_1_ - 1, lvt_8_1_);
                            if (this.isFire(EntityFroststalker.this.getWorld(), lvt_4_1_)) {
                                this.destinationBlock = lvt_4_1_;
                                return true;
                            }
                        }
                    }
                }
            }

            return false;
        }

        private boolean isFire(World world, BlockPos.Mutable lvt_4_1_) {
            return world.getBlockState(lvt_4_1_).isIn(AMTagRegistry.FROSTSTALKER_FEARS);
        }

    }
}
