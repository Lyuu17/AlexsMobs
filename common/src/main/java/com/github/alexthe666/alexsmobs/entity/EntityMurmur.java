package com.github.alexthe666.alexsmobs.entity;

import com.github.alexthe666.alexsmobs.config.AMConfig;
import com.github.alexthe666.alexsmobs.entity.ai.AnimalAILeaveWater;
import com.github.alexthe666.alexsmobs.entity.ai.AnimalAIWanderRanged;
import com.github.alexthe666.alexsmobs.registry.AMEntityRegistry;
import com.github.alexthe666.alexsmobs.registry.AMSoundRegistry;
import com.github.alexthe666.alexsmobs.registry.AMTagRegistry;
import net.minecraft.block.BlockState;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

public class EntityMurmur extends HostileEntity implements ISemiAquatic {

    private static final TrackedData<Optional<UUID>> HEAD_UUID = DataTracker.registerData(EntityMurmur.class, TrackedDataHandlerRegistry.OPTIONAL_UUID);
    private static final TrackedData<Integer> HEAD_ID = DataTracker.registerData(EntityMurmur.class, TrackedDataHandlerRegistry.INTEGER);
    private boolean renderFakeHead = true;


    public EntityMurmur(EntityType<? extends HostileEntity> type, World level) {
        super(type, level);
        this.experiencePoints = 10;
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createLivingAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 30.0D)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 48.0D)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 3.0D)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.3F)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.2F);
    }

    protected void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new AnimalAILeaveWater(this));
        this.goalSelector.add(2, new AnimalAIWanderRanged(this, 55, 1.0D, 14, 7));
        this.targetSelector.add(0, (new RevengeGoal(this)));
    }


    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return AMSoundRegistry.MURMUR_HURT.get();
    }

    protected SoundEvent getDeathSound() {
        return AMSoundRegistry.MURMUR_HURT.get();
    }

    protected void playStepSound(BlockPos pos, BlockState blockIn) {
    }

    public static <T extends MobEntity> boolean checkMurmurSpawnRules(EntityType<EntityMurmur> entityType, ServerWorldAccess iServerWorld, SpawnReason reason, BlockPos pos, Random random) {
        return reason == SpawnReason.SPAWNER || !iServerWorld.isSkyVisible(pos) && (pos.getY() <= AMConfig.murmurSpawnHeight || iServerWorld.getBiome(pos).isIn(AMTagRegistry.SPAWNS_MURMURS_IGNORE_HEIGHT)) && canSpawnInDark(entityType, iServerWorld, reason, pos, random);
    }

    @Override
    public boolean canSpawn(WorldAccess worldIn, SpawnReason spawnReasonIn) {
        return AMEntityRegistry.rollSpawn(AMConfig.murmurSpawnRolls, this.getRandom(), spawnReasonIn) && super.canSpawn(worldIn, spawnReasonIn);
    }

    @Override
    public boolean isTeammate(Entity entity) {
        return this.getHeadUUID() != null && entity.getUuid().equals(this.getHeadUUID()) || super.isTeammate(entity);
    }

    @Override
    public EntityGroup getGroup() {
        return EntityGroup.UNDEAD;
    }

    @Override
    protected float getActiveEyeHeight(EntityPose pose, EntityDimensions dimensions) {
        return dimensions.height * 1.2F;
    }

    @Override
    protected float getBaseMovementSpeedMultiplier() {
        return 0.9F;
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(HEAD_UUID, Optional.empty());
        this.dataTracker.startTracking(HEAD_ID, -1);
    }

    @Nullable
    public UUID getHeadUUID() {
        return this.dataTracker.get(HEAD_UUID).orElse(null);
    }

    public void setHeadUUID(@Nullable UUID uniqueId) {
        this.dataTracker.set(HEAD_UUID, Optional.ofNullable(uniqueId));
    }

    public Entity getHead() {
        if (!this.getWorld().isClient) {
            UUID id = getHeadUUID();
            return id == null ? null : ((ServerWorld) getWorld()).getEntity(id);
        }else{
            int id = this.dataTracker.get(HEAD_ID);
            return id == -1 ? null : getWorld().getEntityById(id);
        }
    }

    public boolean shouldRenderFakeHead() {
        return this.renderFakeHead;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.renderFakeHead) this.renderFakeHead = false;
        this.bodyYaw = this.getYaw();
        this.headYaw= MathHelper.clamp(this.headYaw, this.bodyYaw - 70, this.bodyYaw + 70);
        if (!this.getWorld().isClient) {
            Entity head = getHead();
            if(head == null){
                LivingEntity created = createHead();
                this.setHeadUUID(created.getUuid());
                this.dataTracker.set(HEAD_ID, created.getId());
            }
        }
    }

    public Vec3d getNeckBottom(float partialTick){
        double d0 = MathHelper.lerp(partialTick, this.prevX, this.getX());
        double d1 = MathHelper.lerp(partialTick, this.prevY, this.getY());
        double d2 = MathHelper.lerp(partialTick, this.prevZ, this.getZ());
        double height = this.getHeight() - 0.4F + calculateWalkBounce(partialTick);
        var rotatedOnDeath = new Vec3d(0, height, 0);
        if(this.deathTime > 0){
            float f = ((float)this.deathTime + partialTick - 1.0F) / 20.0F * 1.6F;
            f = MathHelper.sqrt(f);
            if (f > 1.0F) {
                f = 1.0F;
            }
            rotatedOnDeath = rotatedOnDeath.add(f * 0.1F, f * 0.4F, 0).rotateZ((float) (f * Math.PI / 2F)).rotateY(-this.bodyYaw * MathHelper.RADIANS_PER_DEGREE);
        }
        return new Vec3d(d0, d1, d2).add(rotatedOnDeath);
    }

    public double calculateWalkBounce(float partialTick){
        float limbSwingAmount = this.limbAnimator.getSpeed(partialTick);
        float limbSwing = this.limbAnimator.getPos() - this.limbAnimator.getSpeed() * (1.0F - partialTick);
        return Math.abs(Math.sin(limbSwing * 0.9F) * limbSwingAmount * 0.25F);
    }

    @Override
    public boolean shouldEnterWater() {
        return false;
    }

    @Override
    public boolean shouldLeaveWater() {
        return true;
    }

    @Override
    public boolean shouldStopMoving() {
        return false;
    }

    @Override
    public int getWaterSearchRange(){
        return 5;
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound compound) {
        super.readCustomDataFromNbt(compound);
        if (compound.containsUuid("HeadUUID")) {
            this.setHeadUUID(compound.getUuid("HeadUUID"));
        }
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound compound) {
        super.writeCustomDataToNbt(compound);
        if (this.getHeadUUID() != null) {
            compound.putUuid("HeadUUID", this.getHeadUUID());
        }
    }

    private LivingEntity createHead() {
        var head = new EntityMurmurHead(this);
        getWorld().spawnEntity(head);
        return head;
    }

    public boolean isAngry(){
        Entity entity = this.getHead();
        if(entity instanceof EntityMurmurHead){
            return ((EntityMurmurHead)entity).isAngry();
        }
        return false;
    }
}
