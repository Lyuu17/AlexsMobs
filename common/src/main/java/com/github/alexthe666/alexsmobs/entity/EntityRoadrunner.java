package com.github.alexthe666.alexsmobs.entity;

import com.github.alexthe666.alexsmobs.AlexsMobs;
import com.github.alexthe666.alexsmobs.config.AMConfig;
import com.github.alexthe666.alexsmobs.entity.ai.AnimalAIWanderRanged;
import com.github.alexthe666.alexsmobs.registry.AMEntityRegistry;
import com.github.alexthe666.alexsmobs.registry.AMItemRegistry;
import com.github.alexthe666.alexsmobs.registry.AMSoundRegistry;
import com.github.alexthe666.alexsmobs.registry.AMTagRegistry;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.recipe.Ingredient;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import org.jetbrains.annotations.Nullable;

public class EntityRoadrunner extends AnimalEntity {

    public float oFlapSpeed;
    public float oFlap;
    public float wingRotDelta = 1.0F;
    public float wingRotation;
    public float destPos;
    public float prevAttackProgress;
    public float attackProgress;
    private static final TrackedData<Integer> ATTACK_TICK = DataTracker.registerData(EntityRoadrunner.class, TrackedDataHandlerRegistry.INTEGER);
    public int timeUntilNextFeather = this.random.nextInt(24000) + 24000;
    private boolean hasMeepSpeed = false;

    public EntityRoadrunner(EntityType<? extends EntityRoadrunner> type, World worldIn) {
        super(type, worldIn);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new EscapeDangerGoal(this, 1.1D));
        this.goalSelector.add(1, new MeleeAttackGoal(this, 1.0D, false));
        this.goalSelector.add(2, new AnimalMateGoal(this, 1.0D));
        this.goalSelector.add(4, new FollowParentGoal(this, 1.1D));
        this.goalSelector.add(4, new TemptGoal(this, 1.1D, Ingredient.fromTag(AMTagRegistry.ROADRUNNER_BREEDABLES), false));
        this.goalSelector.add(5, new AnimalAIWanderRanged(this, 50, 1.0D, 25, 7));
        this.goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 6.0F));
        this.goalSelector.add(7, new LookAroundGoal(this));
        this.targetSelector.add(1, new ActiveTargetGoal<>(this, EntityRattlesnake.class, 55, true, true, null));
        this.targetSelector.add(2, (new RevengeGoal(this, EntityRattlesnake.class, PlayerEntity.class)).setGroupRevenge());
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound compound) {
        super.readCustomDataFromNbt(compound);
        if (compound.contains("FeatherTime")) {
            this.timeUntilNextFeather = compound.getInt("FeatherTime");
        }

    }

    @Override
    public boolean canSpawn(WorldAccess worldIn, SpawnReason spawnReasonIn) {
        return AMEntityRegistry.rollSpawn(AMConfig.roadrunnerSpawnRolls, this.getRandom(), spawnReasonIn);
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound compound) {
        super.writeCustomDataToNbt(compound);
        compound.putInt("FeatherTime", this.timeUntilNextFeather);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return isMeep() || random.nextInt(2000) == 0 ? AMSoundRegistry.ROADRUNNER_MEEP.get() : AMSoundRegistry.ROADRUNNER_IDLE.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return AMSoundRegistry.ROADRUNNER_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return AMSoundRegistry.ROADRUNNER_HURT.get();
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(ATTACK_TICK, 0);

    }

    @Override
    public boolean tryAttack(Entity entityIn) {
        this.dataTracker.set(ATTACK_TICK, 5);
        return true;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return source.isOf(DamageTypes.CACTUS) || source.getName().equals("anvil") || super.isInvulnerableTo(source);
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createLivingAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 8.0D)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 1.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.45F)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 10D);
    }

    @Override
    public void tickMovement() {
        super.tickMovement();
        this.oFlap = this.wingRotation;
        this.prevAttackProgress = attackProgress;
        this.oFlapSpeed = this.destPos;
        this.destPos = (float) ((double) this.destPos + (double) (this.isOnGround() ? -1 : 4) * 0.3D);
        this.destPos = MathHelper.clamp(this.destPos, 0.0F, 1.0F);
        if (!this.isOnGround() && this.wingRotDelta < 1.0F) {
            this.wingRotDelta = 1.0F;
        }
        if (!this.getWorld().isClient && this.isAlive() && !this.isBaby() && --this.timeUntilNextFeather <= 0) {
            this.dropItem(AMItemRegistry.ROADRUNNER_FEATHER.get());
            this.timeUntilNextFeather = this.random.nextInt(24000) + 24000;
        }
        this.wingRotDelta = (float) ((double) this.wingRotDelta * 0.9D);
        var vector3d = this.getVelocity();
        if (!this.isOnGround() && vector3d.y < 0.0D) {
            this.setVelocity(vector3d.multiply(1.0D, 0.8D, 1.0D));
        }
        this.wingRotation += this.wingRotDelta * 2.0F;

        if(this.dataTracker.get(ATTACK_TICK) > 0){
            if(this.dataTracker.get(ATTACK_TICK) == 2 && this.getTarget() != null && this.distanceTo(this.getTarget()) < 1.3D){
                this.getTarget().damage(this.getDamageSources().mobAttack(this), 2);
            }
            this.dataTracker.set(ATTACK_TICK, this.dataTracker.get(ATTACK_TICK) - 1);
            if(attackProgress < 5F){
                attackProgress++;
            }
        }else{
            if(attackProgress > 0F){
                attackProgress--;
            }
        }
    }

    @Override
    public void tick(){
        super.tick();

        if (isMeep()) {
            if (!hasMeepSpeed) {
                this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).setBaseValue(1F);
                hasMeepSpeed = true;
            }
        } else {
            if (hasMeepSpeed) {
                this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).setBaseValue(0.45F);
                hasMeepSpeed = false;
            }
        }

        if (this.getWorld().isClient && this.isMeep() && this.isOnGround() && !this.isInsideWaterOrBubbleColumn() && this.getVelocity().lengthSquared() > 0.03D) {
            Vec3d vector3d = this.getRotationVec(0.0F);
            final float yRotRad = this.getYaw() * MathHelper.RADIANS_PER_DEGREE;
            float f = MathHelper.cos(yRotRad) * 0.2F;
            float f1 = MathHelper.sin(yRotRad) * 0.2F;
            float f2 = 1.2F - this.random.nextFloat() * 0.7F;
            for (int i = 0; i < 2; ++i) {
                this.getWorld().addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, this.getX() - vector3d.x * (double) f2 + (double) f, this.getY() + random.nextFloat() * 0.2F, this.getZ() - vector3d.z * (double) f2 + (double) f1, 0.0D, 0.0D, 0.0D);
                this.getWorld().addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, this.getX() - vector3d.x * (double) f2 - (double) f, this.getY() + random.nextFloat() * 0.2F, this.getZ() - vector3d.z * (double) f2 - (double) f1, 0.0D, 0.0D, 0.0D);
            }
        }
    }

    @Override
    public boolean handleFallDamage(float distance, float damageMultiplier, DamageSource source) {
        return false;
    }

    @Override
    protected void fall(double y, boolean onGroundIn, BlockState state, BlockPos pos) {
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState blockIn) {
        if(!this.isMeep()){
            this.playSound(SoundEvents.ENTITY_CHICKEN_STEP, 0.15F, 1.0F);
        }
    }

    @Override
    public boolean isBreedingItem(ItemStack stack) {
        return stack.isIn(AMTagRegistry.ROADRUNNER_BREEDABLES);
    }

    @Nullable
    @Override
    public PassiveEntity createChild(ServerWorld world, PassiveEntity entity) {
        return AMEntityRegistry.ROADRUNNER.get().create(world);
    }

    public static boolean canRoadrunnerSpawn(EntityType<? extends AnimalEntity> animal, WorldAccess worldIn, SpawnReason reason, BlockPos pos, Random random) {
        boolean spawnBlock = worldIn.getBlockState(pos.down()).isIn(AMTagRegistry.ROADRUNNER_SPAWNS);
        return spawnBlock && worldIn.getBaseLightLevel(pos, 0) > 8;
    }

    public boolean isMeep(){
        String s = Formatting.strip(this.getName().getString());
        return (s != null && s.toLowerCase().contains("meep")) || AlexsMobs.isAprilFools();
    }
}
