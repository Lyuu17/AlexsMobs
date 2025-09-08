package com.github.alexthe666.alexsmobs.entity;

import com.github.alexthe666.alexsmobs.config.AMConfig;
import com.github.alexthe666.alexsmobs.entity.ai.AnimalAIFleeLight;
import com.github.alexthe666.alexsmobs.registry.AMEntityRegistry;
import com.github.alexthe666.alexsmobs.registry.AMSoundRegistry;
import net.minecraft.block.BlockState;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.*;
import net.minecraft.world.event.GameEvent;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class EntityCentipedeHead extends HostileEntity {

    private static final TrackedData<Optional<UUID>> CHILD_UUID = DataTracker.registerData(EntityCentipedeHead.class, TrackedDataHandlerRegistry.OPTIONAL_UUID);
    private static final TrackedData<Integer> CHILD_ID = DataTracker.registerData(EntityCentipedeHead.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> SEGMENT_COUNT = DataTracker.registerData(EntityCentipedeHead.class, TrackedDataHandlerRegistry.INTEGER);
    public final float[] ringBuffer = new float[64];
    public int ringBufferIndex = -1;
    private EntityCentipedeBody[] parts;

    public EntityCentipedeHead(EntityType<? extends EntityCentipedeHead> type, World worldIn) {
        super(type, worldIn);
        this.experiencePoints = 13;
        this.setStepHeight(3);
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createLivingAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 35.0D)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32.0D)
                .add(EntityAttributes.GENERIC_ARMOR, 6.0D)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 8.0D)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.5F)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.22F)
                .add(EntityAttributes.GENERIC_ATTACK_KNOCKBACK, 0);
    }

    public static <T extends MobEntity> boolean canCentipedeSpawn(EntityType<EntityCentipedeHead> entityType, ServerWorldAccess iServerWorld, SpawnReason reason, BlockPos pos, Random random) {
        return reason == SpawnReason.SPAWNER || !iServerWorld.isSkyVisible(pos) && pos.getY() <= AMConfig.caveCentipedeSpawnHeight && canSpawnInDark(entityType, iServerWorld, reason, pos, random);
    }

    @Override
    public boolean canSpawn(WorldAccess worldIn, SpawnReason spawnReasonIn) {
        return AMEntityRegistry.rollSpawn(AMConfig.caveCentipedeSpawnRolls, this.getRandom(), spawnReasonIn) && super.canSpawn(worldIn, spawnReasonIn);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new MeleeAttackGoal(this, 1.4D, false));
        this.goalSelector.add(2, new WanderAroundGoal(this, 1.0D, 13, false));
        this.goalSelector.add(3, new LookAtEntityGoal(this, PlayerEntity.class, 6.0F));
        this.goalSelector.add(4, new LookAroundGoal(this));
        this.goalSelector.add(5, new AnimalAIFleeLight(this, 1.0D, 75, 5));
        this.targetSelector.add(1, (new RevengeGoal(this)));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, 20, true, true, null));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, MerchantEntity.class, 20, true, true, null));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, EntityCockroach.class, 45, true, true, null));
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return AMSoundRegistry.CENTIPEDE_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return AMSoundRegistry.CENTIPEDE_HURT.get();
    }

    @Override
    public EntityGroup getGroup() {
        return EntityGroup.ARTHROPOD;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState blockIn) {
        this.playSound(AMSoundRegistry.CENTIPEDE_WALK.get(), 1F, 1.0F);
    }

    @Override
    public int getMaxLookPitchChange() {
        return 1;
    }

    @Override
    public int getMaxHeadRotation() {
        return 1;
    }

    @Override
    public int getMaxLookYawChange() {
        return 1;
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(CHILD_UUID, Optional.empty());
        this.dataTracker.startTracking(CHILD_ID, -1);
        this.dataTracker.startTracking(SEGMENT_COUNT, 5);
    }

    @Override
    public boolean tryAttack(Entity entityIn) {
        if (super.tryAttack(entityIn)) {
            if (entityIn instanceof LivingEntity) {
                final int i;
                final Difficulty difficulty = this.getWorld().getDifficulty();
                if (difficulty == Difficulty.NORMAL) {
                    i = 10;
                } else if (difficulty == Difficulty.HARD) {
                    i = 20;
                } else {
                    i = 3;
                }
                ((LivingEntity) entityIn).addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, i * 20, 1));
            }
            this.playSound(AMSoundRegistry.CENTIPEDE_ATTACK.get(), this.getSoundVolume(), this.getSoundPitch());
            this.emitGameEvent(GameEvent.ENTITY_INTERACT);
            return true;
        } else {
            return false;
        }
    }

    public int getSegmentCount() {
        return Math.max(this.dataTracker.get(SEGMENT_COUNT), 1);
    }

    public void setSegmentCount(int segments) {
        this.dataTracker.set(SEGMENT_COUNT, segments);
    }

    @Nullable
    public UUID getChildId() {
        return this.dataTracker.get(CHILD_UUID).orElse(null);
    }

    public void setChildId(@Nullable UUID uniqueId) {
        this.dataTracker.set(CHILD_UUID, Optional.ofNullable(uniqueId));
    }

    public Entity getChild() {
        final UUID id = getChildId();
        if (id != null && !this.getWorld().isClient) {
            return ((ServerWorld) getWorld()).getEntity(id);
        }
        return null;
    }

    @Override
    public void tickCramming() {
        final List<Entity> entities = this.getWorld().getOtherEntities(this, this.getBoundingBox().stretch(0.2D, 0.0D, 0.2D));
        entities.stream().filter(entity -> !(entity instanceof EntityCentipedeBody) && entity.isPushable()).forEach(entity -> entity.pushAwayFrom(this));
    }

    @Nullable
    @Override
    public EntityData initialize(ServerWorldAccess worldIn, LocalDifficulty difficultyIn, SpawnReason reason, @Nullable EntityData spawnDataIn, @Nullable NbtCompound dataTag) {
        this.setSegmentCount(random.nextInt(4) + 5);
        return super.initialize(worldIn, difficultyIn, reason, spawnDataIn, dataTag);
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound compound) {
        super.writeCustomDataToNbt(compound);
        if (this.getChildId() != null) {
            compound.putUuid("ChildUUID", this.getChildId());
        }
        compound.putInt("SegCount", getSegmentCount());
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound compound) {
        super.readCustomDataFromNbt(compound);
        if (compound.containsUuid("ChildUUID")) {
            this.setChildId(compound.getUuid("ChildUUID"));
        }
        this.setSegmentCount(compound.getInt("SegCount"));
    }

    private boolean shouldReplaceParts() {
        if (parts == null || parts[0] == null || parts.length != this.getSegmentCount()) {
            return true;
        }
        for (int i = 0; i < this.getSegmentCount(); i++) {
            if (parts[i] == null) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return source.isOf(DamageTypes.IN_WALL)  || super.isInvulnerableTo(source);
    }

    @Override
    public void tick() {
        super.tick();
        inNetherPortal = false;
        this.bodyYaw = MathHelper.clamp(this.getYaw(), this.bodyYaw - 2, this.bodyYaw + 2);
        this.headYaw= this.bodyYaw;
        if (this.ringBufferIndex < 0) {
            Arrays.fill(this.ringBuffer, this.bodyYaw);
        }
        if (updateRingBuffer() || ringBufferIndex < 0) {
            this.ringBufferIndex++;
        }
        if (this.ringBufferIndex == this.ringBuffer.length) {
            this.ringBufferIndex = 0;
        }
        this.ringBuffer[this.ringBufferIndex] = this.getYaw();

        if (!this.getWorld().isClient) {
            final Entity child = getChild();
            if (child == null) {
                LivingEntity partParent = this;
                parts = new EntityCentipedeBody[this.getSegmentCount()];
                var prevPos = this.getPos();
                float backOffset = 0.45F;
                for (int i = 0; i < this.getSegmentCount(); i++) {
                    final var part = createBody(partParent, i == this.getSegmentCount() - 1);
                    part.setParent(partParent);
                    part.setBodyIndex(i);
                    if (partParent == this) {
                        this.setChildId(part.getUuid());
                        this.dataTracker.set(CHILD_ID, part.getId());
                    }
                    if (partParent instanceof final EntityCentipedeBody body) {
                        body.setChildId(part.getUuid());
                    }
                    part.setPosition(part.tickMultipartPosition(this.getId(), backOffset, prevPos, this.getPitch(), getYawForPart(i), false));
                    getWorld().spawnEntity(part);
                    parts[i] = part;
                    partParent = part;
                    backOffset = part.getBackOffset();
                    prevPos = part.getPos();
                }
            }
            if (age > 1) {
                if (shouldReplaceParts() && this.getChild() instanceof EntityCentipedeBody) {
                    parts = new EntityCentipedeBody[this.getSegmentCount()];
                    parts[0] = (EntityCentipedeBody) this.getChild();
                    this.dataTracker.set(CHILD_ID, parts[0].getId());
                    int i = 1;
                    while (i < parts.length && parts[i - 1].getChild() instanceof EntityCentipedeBody) {
                        parts[i] = (EntityCentipedeBody) parts[i - 1].getChild();
                        i++;
                    }
                }
                var prev = this.getPos();
                float xRot = this.getPitch();
                float backOffset = 0.45F;
                for (int i = 0; i < this.getSegmentCount(); i++) {
                    if (this.parts[i] != null) {
                        final float reqRot = getYawForPart(i);
                        prev = parts[i].tickMultipartPosition(this.getId(), backOffset, prev, xRot, reqRot, true);
                        xRot = parts[i].getPitch();
                        backOffset = parts[i].getBackOffset();
                    }
                }
            }
        }
    }

    private boolean updateRingBuffer() {
        return this.getVelocity().lengthSquared() >= 0.005D;
    }

    public EntityCentipedeBody createBody(LivingEntity parent, boolean tail) {
        return tail ? new EntityCentipedeBody(AMEntityRegistry.CENTIPEDE_TAIL.get(), parent, 0.84F, 180, 0) : new EntityCentipedeBody(AMEntityRegistry.CENTIPEDE_BODY.get(), parent, 0.84F, 180, 0);
    }

    @Override
    public boolean canBeLeashedBy(PlayerEntity player) {
        return true;
    }

    private float getYawForPart(int i) {
        return this.getRingBuffer(4 + i * 4, 1.0F);
    }

    public float getRingBuffer(int bufferOffset, float partialTicks) {
        if (this.isDead()) {
            partialTicks = 0.0F;
        }

        partialTicks = 1.0F - partialTicks;
        final int i = this.ringBufferIndex - bufferOffset & 63;
        final int j = this.ringBufferIndex - bufferOffset - 1 & 63;
        final float d0 = this.ringBuffer[i];
        final float d1 = this.ringBuffer[j] - d0;
        return MathHelper.wrapDegrees(d0 + d1 * partialTicks);
    }
}
