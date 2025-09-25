package com.github.alexthe666.alexsmobs.entity;

import com.github.alexthe666.alexsmobs.config.AMConfig;
import com.github.alexthe666.alexsmobs.entity.ai.AnimalAIWanderRanged;
import com.github.alexthe666.alexsmobs.entity.ai.GroundPathNavigatorWide;
import com.github.alexthe666.alexsmobs.registry.AMEntityRegistry;
import com.github.alexthe666.alexsmobs.registry.AMParticleRegistry;
import com.github.alexthe666.alexsmobs.registry.AMSoundRegistry;
import com.github.alexthe666.alexsmobs.registry.AMTagRegistry;
import net.minecraft.block.BlockState;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.event.GameEvent;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class EntityGuster extends HostileEntity {

    private static final TrackedData<Integer> LIFT_ENTITY = DataTracker.registerData(EntityGuster.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> VARIANT = DataTracker.registerData(EntityGuster.class, TrackedDataHandlerRegistry.INTEGER);
    private LivingEntity liftedEntity;
    private int liftingTime = 0;
    private int maxLiftTime = 40;
    private int shootingTicks;
    public static final Identifier RED_LOOT = new Identifier("alexsmobs", "entities/guster_red");
    public static final Identifier SOUL_LOOT = new Identifier("alexsmobs", "entities/guster_soul");

    public EntityGuster(EntityType<? extends EntityGuster> type, World worldIn) {
        super(type, worldIn);
        this.setStepHeight(1);
        this.setPathfindingPenalty(PathNodeType.WATER, -1.0F);
    }

    @Override
    public int getMinAmbientSoundDelay() {
        return 80;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return AMSoundRegistry.GUSTER_IDLE.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return AMSoundRegistry.GUSTER_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return AMSoundRegistry.GUSTER_HURT.get();
    }

    @Override
    public boolean hurtByWater() {
        return true;
    }

    @Nullable
    protected Identifier getLootTableId() {
        return this.getVariant() == 2 ? SOUL_LOOT : this.getVariant() == 1 ? RED_LOOT : super.getLootTableId();
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createLivingAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 16.0D)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32.0D)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 1.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.2D);
    }

    public static boolean canGusterSpawn(EntityType animal, WorldAccess worldIn, SpawnReason reason, BlockPos pos, Random random) {
        boolean spawnBlock = worldIn.getBlockState(pos.down()).isIn(BlockTags.SAND);
        return spawnBlock && (!AMConfig.limitGusterSpawnsToWeather || worldIn.getLevelProperties() != null && (worldIn.getLevelProperties().isThundering() || worldIn.getLevelProperties().isRaining()) || isBiomeNether(worldIn, pos));
    }

    @Override
    public boolean canSpawn(WorldAccess worldIn, SpawnReason spawnReasonIn) {
        return AMEntityRegistry.rollSpawn(AMConfig.gusterSpawnRolls, this.getRandom(), spawnReasonIn);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new MeleeGoal());
        this.goalSelector.add(1, new AnimalAIWanderRanged(this, 60, 1.0D, 10, 7));
        this.goalSelector.add(2, new LookAtEntityGoal(this, PlayerEntity.class, 8.0F));
        this.goalSelector.add(2, new LookAroundGoal(this));
        this.targetSelector.add(1, new RevengeGoal(this));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
        this.targetSelector.add(3, new ActiveTargetGoal<>(this, MerchantEntity.class, true));
    }

    @Override
    protected EntityNavigation createNavigation(World worldIn) {
        return new GroundPathNavigatorWide(this, worldIn);
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
    }

    @Override
    public void pushAway(Entity entityIn) {
        if (this.getLiftedEntity() == null && liftingTime >= 0 && !(entityIn instanceof EntityGuster)) {
            this.setLiftedEntity(entityIn.getId());
            maxLiftTime = 30 + random.nextInt(30);
        }
    }

    public boolean hasLiftedEntity() {
        return this.dataTracker.get(LIFT_ENTITY) != 0;
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(LIFT_ENTITY, 0);
        this.dataTracker.startTracking(VARIANT, 0);
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        if (this.isInvulnerableTo(source)) {
            return false;
        } else {
            if (source.isIn(DamageTypeTags.IS_PROJECTILE)) {
                amount = (amount + 1.0F) / 3.0F;
            }
            return super.damage(source, amount);
        }
    }

    private void spit(LivingEntity target) {
        var sghot = new EntitySandShot(this.getWorld(), this);
        double d0 = target.getX() - this.getX();
        double d1 = target.getBodyY(0.3333333333333333D) - sghot.getY();
        double d2 = target.getZ() - this.getZ();
        float f = MathHelper.sqrt((float)(d0 * d0 + d2 * d2)) * 0.35F;
        sghot.shoot(d0, d1 + (double) f, d2, 1F, 10.0F);
        sghot.setVariant(this.getVariant());
        if (!this.isSilent()) {
            this.emitGameEvent(GameEvent.PROJECTILE_SHOOT);
            this.getWorld().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.BLOCK_SAND_BREAK, this.getSoundCategory(), 1.0F, 1.0F + (this.random.nextFloat() - this.random.nextFloat()) * 0.2F);
        }
        this.getWorld().spawnEntity(sghot);
    }

    @Override
    public double getEyeY() {
        return this.getY() + 1.0F;
    }

    @Nullable
    public Entity getLiftedEntity() {
        if (!this.hasLiftedEntity()) {
            return null;
        } else {
            return this.getWorld().getEntityById(this.dataTracker.get(LIFT_ENTITY));
        }
    }

    @Nullable
    public EntityData initialize(ServerWorldAccess worldIn, LocalDifficulty difficultyIn, SpawnReason
            reason, @Nullable EntityData spawnDataIn, @Nullable NbtCompound dataTag) {
        if(isBiomeNether(worldIn, this.getBlockPos())){
            this.setVariant(2);
        }else if(isBiomeRed(worldIn, this.getBlockPos())){
            this.setVariant(1);
        }else{
            this.setVariant(0);
        }
        this.setAir(this.getMaxAir());
        this.setPitch(0.0F);
        return super.initialize(worldIn, difficultyIn, reason, spawnDataIn, dataTag);
    }

    private void setLiftedEntity(int p_175463_1_) {
        this.dataTracker.set(LIFT_ENTITY, p_175463_1_);
    }

    public int getVariant() {
        return this.dataTracker.get(VARIANT);
    }

    public void setVariant(int variant) {
        this.dataTracker.set(VARIANT, variant);
    }

    @Override
    public void tickMovement() {
        super.tickMovement();
        Entity lifted = this.getLiftedEntity();
        if (lifted == null && !this.getWorld().isClient && this.age % 15 == 0) {
            List<ItemEntity> list = this.getWorld().getNonSpectatingEntities(ItemEntity.class, this.getBoundingBox().expand(0.8F));
            ItemEntity closestItem = null;
            for (int i = 0; i < list.size(); ++i) {
                ItemEntity entity = list.get(i);
                if (entity.isOnGround() && (closestItem == null || this.distanceTo(closestItem) > this.distanceTo(entity))) {
                    closestItem = entity;
                }
            }
            if (closestItem != null) {
                this.setLiftedEntity(closestItem.getId());
                maxLiftTime = 30 + random.nextInt(30);
            }
        }
        float f = (float) this.getY();
        if (this.isAlive()) {
            var type = this.getVariant() == 2 ? AMParticleRegistry.GUSTER_SAND_SPIN_SOUL.get() : this.getVariant() == 1 ? AMParticleRegistry.GUSTER_SAND_SPIN_RED.get() : AMParticleRegistry.GUSTER_SAND_SPIN.get();
            for (int j = 0; j < 4; ++j) {
                float f1 = (this.random.nextFloat() * 2.0F - 1.0F) * this.getWidth() * 0.95F;
                float f2 = (this.random.nextFloat() * 2.0F - 1.0F) * this.getWidth() * 0.95F;
                this.getWorld().addParticle(type, this.getX() + (double) f1, f, this.getZ() + (double) f2, this.getX(), this.getY() + random.nextFloat() * this.getHeight() + 0.2F, this.getZ());
            }
        }
        if (lifted != null && liftingTime >= 0) {
            liftingTime++;
            float resist = 1F;
            if (lifted instanceof LivingEntity) {
                resist = (float) MathHelper.clamp((1.0D - ((LivingEntity) lifted).getAttributeValue(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE)), 0, 1);
            }
            float radius = 1F + (liftingTime * 0.05F);
            if (lifted instanceof ItemEntity) {
                radius = 0.2F + (liftingTime * 0.025F);
            }
            float angle = liftingTime * -0.25F;
            double extraX = this.getX() + radius * MathHelper.sin(MathHelper.PI + angle);
            double extraZ = this.getZ() + radius * MathHelper.cos(angle);
            double d0 = (extraX - lifted.getX()) * resist;
            double d1 = (extraZ - lifted.getZ()) * resist;
            lifted.setVelocity(d0, 0.1 * resist, d1);
            lifted.velocityDirty = true;
            if (liftingTime > maxLiftTime) {
                this.setLiftedEntity(0);
                liftingTime = -20;
                maxLiftTime = 30 + random.nextInt(30);
            }
        } else if (liftingTime < 0) {
            liftingTime++;
        } else if (this.getTarget() != null && this.distanceTo(this.getTarget()) < this.getWidth() + 1F && !(this.getTarget() instanceof EntityGuster)) {
            this.setLiftedEntity(this.getTarget().getId());
            maxLiftTime = 30 + random.nextInt(30);
        }
        if (!this.getWorld().isClient && shootingTicks >= 0) {
            if (shootingTicks <= 0) {
                if (this.getTarget() != null && (lifted == null || lifted.getId() != this.getTarget().getId()) && this.isAlive()) {
                    this.spit(this.getTarget());
                }
                shootingTicks = 40 + random.nextInt(40);
            } else {
                shootingTicks--;
            }
        }
        var vector3d = this.getVelocity();
        if (!this.isOnGround() && vector3d.y < 0.0D) {
            this.setVelocity(vector3d.multiply(1.0D, 0.6D, 1.0D));
        }
    }

    public boolean isGooglyEyes() {
        var s = Formatting.strip(this.getName().getString());
        return s != null && s.toLowerCase().contains("tweester");
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound compound) {
        super.writeCustomDataToNbt(compound);
        compound.putInt("Variant", this.getVariant());
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound compound) {
        super.readCustomDataFromNbt(compound);
        this.setVariant(compound.getInt("Variant"));
    }

    private static boolean isBiomeRed(WorldAccess worldIn, BlockPos position) {
        return worldIn.getBiome(position).isIn(AMTagRegistry.SPAWNS_RED_GUSTERS);
    }

    private static boolean isBiomeNether(WorldAccess worldIn, BlockPos position) {
        return worldIn.getBiome(position).isIn(AMTagRegistry.SPAWNS_SOUL_GUSTERS);
    }

    public static int getColorForVariant(int variant){
        if(variant == 2){
            return 0X4E3D33;
        }else if(variant == 1){
            return 0XC66127;
        }else{
            return 0XF3C389;
        }
    }

    private class MeleeGoal extends Goal {

        public MeleeGoal() {
        }

        @Override
        public boolean canStart() {
            return EntityGuster.this.getTarget() != null;
        }

        @Override
        public void tick() {
            var thrownEntity = EntityGuster.this.getLiftedEntity();
            if (EntityGuster.this.getTarget() != null) {
                if (thrownEntity != null && thrownEntity.getId() == EntityGuster.this.getTarget().getId()) {
                    EntityGuster.this.getNavigation().stop();
                } else {
                    EntityGuster.this.getNavigation().startMovingTo(EntityGuster.this.getTarget(), 1.25F);
                }
            }
        }
    }
}
