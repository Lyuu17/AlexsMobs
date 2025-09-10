package com.github.alexthe666.alexsmobs.entity;

import com.github.alexthe666.alexsmobs.AlexsMobs;
import com.github.alexthe666.alexsmobs.packet.HurtMultipartPacket;
import com.github.alexthe666.alexsmobs.registry.AMAdvancementTriggerRegistry;
import com.github.alexthe666.alexsmobs.registry.AMEntityRegistry;
import com.github.alexthe666.alexsmobs.registry.AMParticleRegistry;
import com.google.common.collect.ImmutableList;
import net.minecraft.entity.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class EntityVoidWormPart extends LivingEntity implements IHurtableMultipart {

    protected static final EntityDimensions SIZE_BASE = EntityDimensions.changing(1.2F, 1.95F);
    protected static final EntityDimensions TAIL_SIZE = EntityDimensions.changing(1.6F, 2F);
    private static final TrackedData<Boolean> TAIL = DataTracker.registerData(EntityVoidWormPart.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Integer> BODYINDEX = DataTracker.registerData(EntityVoidWormPart.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Float> WORM_SCALE = DataTracker.registerData(EntityVoidWormPart.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> WORM_YAW = DataTracker.registerData(EntityVoidWormPart.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> WORM_ANGLE = DataTracker.registerData(EntityVoidWormPart.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Optional<UUID>> PARENT_UUID = DataTracker.registerData(EntityVoidWormPart.class, TrackedDataHandlerRegistry.OPTIONAL_UUID);
    private static final TrackedData<Optional<UUID>> CHILD_UUID = DataTracker.registerData(EntityVoidWormPart.class, TrackedDataHandlerRegistry.OPTIONAL_UUID);
    private static final TrackedData<Integer> PORTAL_TICKS = DataTracker.registerData(EntityVoidWormPart.class, TrackedDataHandlerRegistry.INTEGER);
    public EntityDimensions multipartSize;
    public float prevWormAngle;
    protected float radius;
    protected float angleYaw;
    protected float offsetY;
    protected float damageMultiplier = 1;
    private float prevWormYaw = 0;
    private Vec3d teleportPos = null;
    private Vec3d enterPos = null;
    private boolean doesParentControlPos = false;

    public EntityVoidWormPart(EntityType<EntityVoidWormPart> t, World world) {
        super(t, world);
        multipartSize = t.getDimensions();
    }

    public EntityVoidWormPart(EntityType<EntityVoidWormPart> t, LivingEntity parent, float radius, float angleYaw, float offsetY) {
        super(t, parent.getWorld());
        this.setParent(parent);
        this.radius = radius;
        this.angleYaw = (angleYaw + 90.0F) * MathHelper.RADIANS_PER_DEGREE;
        this.offsetY = offsetY;
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createLivingAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 30.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.15F);
    }

    @Override
    public void pushAwayFrom(Entity entityIn) {
    }

    @Override
    public void kill() {
        this.remove(RemovalReason.DISCARDED);
    }

    public EntityDimensions getDimensions(EntityPose poseIn) {
        return this.isTail() ? TAIL_SIZE.scaled(getScaleFactor()) : super.getDimensions(poseIn);
    }

    public float getWormScale() {
        return this.dataTracker.get(WORM_SCALE);
    }

    public void setWormScale(float scale) {
        this.dataTracker.set(WORM_SCALE, scale);
    }

    @Override
    public float getScaleFactor() {
        return getWormScale() + 0.5F;
    }

    public boolean startRiding(Entity entityIn) {
        if (!(entityIn instanceof AbstractMinecartEntity || entityIn instanceof BoatEntity)) {
            return super.startRiding(entityIn);
        }
        return false;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return source.isOf(DamageTypes.FALL) || source.isOf(DamageTypes.DROWN) || source.isOf(DamageTypes.OUT_OF_WORLD) || source.isOf(DamageTypes.IN_WALL)  || source.isOf(DamageTypes.LAVA) || source.isIn(DamageTypeTags.IS_FIRE) || super.isInvulnerableTo(source);
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound compound) {
        super.writeCustomDataToNbt(compound);
        if (this.getParentId() != null) {
            compound.putUuid("ParentUUID", this.getParentId());
        }
        if (this.getChildId() != null) {
            compound.putUuid("ChildUUID", this.getChildId());
        }
        compound.putBoolean("TailPart", isTail());
        compound.putInt("BodyIndex", getBodyIndex());
        compound.putInt("PortalTicks", getPortalTicks());
        compound.putFloat("PartAngle", angleYaw);
        compound.putFloat("WormScale", this.getWormScale());
        compound.putFloat("PartRadius", radius);
        compound.putFloat("PartYOffset", offsetY);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound compound) {
        super.readCustomDataFromNbt(compound);
        if (compound.containsUuid("ParentUUID")) {
            this.setParentId(compound.getUuid("ParentUUID"));
        }
        if (compound.containsUuid("ChildUUID")) {
            this.setChildId(compound.getUuid("ChildUUID"));
        }
        this.setTail(compound.getBoolean("TailPart"));
        this.setBodyIndex(compound.getInt("BodyIndex"));
        this.setPortalTicks(compound.getInt("PortalTicks"));
        this.angleYaw = compound.getFloat("PartAngle");
        this.setWormScale(compound.getFloat("WormScale"));
        this.radius = compound.getFloat("PartRadius");
        this.offsetY = compound.getFloat("PartYOffset");
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(PARENT_UUID, Optional.empty());
        this.dataTracker.startTracking(CHILD_UUID, Optional.empty());
        this.dataTracker.startTracking(TAIL, false);
        this.dataTracker.startTracking(BODYINDEX, 0);
        this.dataTracker.startTracking(WORM_SCALE, 1F);
        this.dataTracker.startTracking(WORM_YAW, 0F);
        this.dataTracker.startTracking(WORM_ANGLE, 0F);
        this.dataTracker.startTracking(PORTAL_TICKS, 0);
    }

    @Nullable
    public UUID getParentId() {
        return this.dataTracker.get(PARENT_UUID).orElse(null);
    }

    public void setParentId(@Nullable UUID uniqueId) {
        this.dataTracker.set(PARENT_UUID, Optional.ofNullable(uniqueId));
    }

    @Nullable
    public UUID getChildId() {
        return this.dataTracker.get(CHILD_UUID).orElse(null);
    }

    public void setChildId(@Nullable UUID uniqueId) {
        this.dataTracker.set(CHILD_UUID, Optional.ofNullable(uniqueId));
    }

    public void setInitialPartPos(Entity parent) {
        this.setPos(parent.prevX + this.radius * Math.cos(parent.getYaw() * (Math.PI / 180.0F) + this.angleYaw), parent.prevY + this.offsetY, parent.prevZ + this.radius * Math.sin(parent.getYaw() * (Math.PI / 180.0F) + this.angleYaw));
    }

    public float getWormAngle() {
        return this.dataTracker.get(WORM_ANGLE);
    }

    public void setWormAngle(float progress) {
        this.dataTracker.set(WORM_ANGLE, progress);
    }

    public int getPortalTicks() {
        return this.dataTracker.get(PORTAL_TICKS);
    }

    public void setPortalTicks(int ticks) {
        this.dataTracker.set(PORTAL_TICKS, ticks);
    }

    @Override
    public void tick() {
        inNetherPortal = false;
        prevWormAngle = this.getWormAngle();
        prevWormYaw = this.dataTracker.get(WORM_YAW);
        this.setVelocity(Vec3d.ZERO);
        radius = 1.0F + (this.getWormScale() * (this.isTail() ? 0.65F : 0.3F)) + (this.getBodyIndex() == 0 ? 0.8F : 0);
        if (this.age > 3) {
            Entity parent = getParent();
            calculateDimensions();
            if (parent != null && !this.getWorld().isClient) {
                this.setNoGravity(true);
                var parentVec = parent.getPos().subtract(parent.prevX, parent.prevY, parent.prevZ);
                double restrictRadius = MathHelper.clamp(radius - parentVec.lengthSquared() * 0.25F, radius * 0.5F, radius);
                if (parent instanceof EntityVoidWorm) {
                    restrictRadius *= (isTail() ? 0.8F : 0.4F);
                }
                final double x = parent.getX() + restrictRadius * Math.cos(parent.getYaw() * (Math.PI / 180.0F) + this.angleYaw);
                final double yStretch = Math.abs(parent.getY() - parent.prevY) > this.getWidth() ? parent.getY() : parent.prevY;
                final double y = yStretch + this.offsetY * getWormScale();
                final double z = parent.getZ() + restrictRadius * Math.sin(parent.getYaw() * (Math.PI / 180.0F) + this.angleYaw);

                final double d0 = parent.prevX - this.getX();
                final double d1 = parent.prevY - this.getY();
                final double d2 = parent.prevZ - this.getZ();
                final float yaw = (float) (MathHelper.atan2(d2, d0) * (double) MathHelper.DEGREES_PER_RADIAN) - 90.0F;
                final float pitch = parent.getPitch();
                if (this.getPortalTicks() <= 1 && !doesParentControlPos) {
                    //double d3 = d0 * d0 + d1 * d1 + d2 * d2;
                    final float f2 = -((float) (MathHelper.atan2(d1, MathHelper.sqrt((float) (d0 * d0 + d2 * d2))) * (double) MathHelper.DEGREES_PER_RADIAN));
                    this.setPos(x, y, z);
                    this.setPitch(this.limitAngle(this.getPitch(), f2, 5.0F));
                    this.setYaw(yaw);
                    this.dataTracker.set(WORM_YAW, getYaw());
                }
                this.scheduleVelocityUpdate();
                this.headYaw= this.getYaw();
                this.bodyYaw = pitch;
                if (parent instanceof LivingEntity) {
                    if (!this.getWorld().isClient && (((LivingEntity) parent).hurtTime > 0 || ((LivingEntity) parent).deathTime > 0)) {
                        AlexsMobs.sendMSGToAll(new HurtMultipartPacket(this.getId(), parent.getId(), 0));
                        this.hurtTime = ((LivingEntity) parent).hurtTime;
                        this.deathTime = ((LivingEntity) parent).deathTime;
                    }
                }
                this.tickCramming();
                if (parent.isRemoved() && !this.getWorld().isClient) {
                    this.remove(RemovalReason.DISCARDED);
                }
                if (parent instanceof EntityVoidWorm) {
                    this.setWormAngle(((EntityVoidWorm) parent).prevWormAngle);
                } else if (parent instanceof EntityVoidWormPart) {
                    this.setWormAngle(((EntityVoidWormPart) parent).prevWormAngle);
                }
            } else if (this.age > 20 && !this.getWorld().isClient) {
                remove(RemovalReason.DISCARDED);
            }
        }
        if (this.age % 400 == 0) {
            this.heal(1);
        }
        super.tick();
        if (doesParentControlPos && enterPos != null) {
            this.requestTeleport(enterPos.x, enterPos.y, enterPos.z);
        }
        if (this.getPortalTicks() > 0) {
            this.setPortalTicks(this.getPortalTicks() - 1);
            if (this.getPortalTicks() <= 5 && teleportPos != null) {
                Vec3d vec = teleportPos;
                this.requestTeleport(vec.x, vec.y, vec.z);
                this.lastRenderX = vec.x;
                this.lastRenderY = vec.y;
                this.lastRenderZ = vec.z;
                if (this.getPortalTicks() == 5 && this.getChild() instanceof EntityVoidWormPart) {
                    ((EntityVoidWormPart) this.getChild()).teleportTo(enterPos, teleportPos);
                }
                teleportPos = null;
            } else if (this.getPortalTicks() > 5 && enterPos != null) {
                this.requestTeleport(enterPos.x, enterPos.y, enterPos.z);
            }
            if (this.getPortalTicks() == 0) {
                doesParentControlPos = false;
            }
        }
    }

    @Override
    protected void updatePostDeath() {
        ++this.deathTime;
        if (this.deathTime == 20) {
            this.remove(RemovalReason.DISCARDED); //Forge keep data until we revive player
            for (int i = 0; i < 30; ++i) {
                final double d0 = this.random.nextGaussian() * 0.02D;
                final double d1 = this.random.nextGaussian() * 0.02D;
                final double d2 = this.random.nextGaussian() * 0.02D;
                this.getWorld().addParticle(AMParticleRegistry.WORM_PORTAL.get(), this.getParticleX(1.0D), this.getRandomBodyY(), this.getParticleZ(1.0D), d0, d1, d2);
            }
        }
    }

    @Override
    public void onDeath(DamageSource cause) {
        EntityVoidWorm worm = this.getWorm();
        if (worm != null) {
            final int segments = Math.max(worm.getSegmentCount() / 2 - 1, 1);
            worm.setSegmentCount(segments);
            if (this.getChild() instanceof EntityVoidWormPart) {
                var segment = (EntityVoidWormPart) this.getChild();
                var worm2 = AMEntityRegistry.VOID_WORM.get().create(getWorld());
                worm2.setAiDisabled(worm.isAiDisabled());
                worm2.setInvulnerable(worm.isInvulnerable());
                worm2.copyPositionAndRotation(this);
                segment.copyPositionAndRotation(this);
                worm2.setChildId(segment.getUuid());
                worm2.setSegmentCount(segments);
                segment.setParent(worm2);
                if (!this.getWorld().isClient) {
                    getWorld().spawnEntity(worm2);
                }
                worm2.setSplitter(true);
                worm2.setBaseMaxHealth(worm.getBaseMaxHealth() / 2F, true);
                worm2.setSplitFromUuid(worm.getUuid());
                worm2.setWormSpeed((float) MathHelper.clamp(worm.getWormSpeed() * 0.8, 0.4F, 1F));
                worm2.resetWormScales();
                if (!this.getWorld().isClient) {
                    if (cause != null && cause.getAttacker() instanceof ServerPlayerEntity) {
                        AMAdvancementTriggerRegistry.VOID_WORM_SPLIT.trigger((ServerPlayerEntity) cause.getAttacker());
                    }
                }
            }
            worm.resetWormScales();
        }
    }

    @Override
    public boolean isTeammate(Entity entityIn) {
        var worm = this.getWorm();
        return super.isTeammate(entityIn) || worm != null && worm.isTeammate(entityIn);
    }

    public EntityVoidWorm getWorm() {
        Entity parent = this.getParent();
        while (parent instanceof EntityVoidWormPart) {
            parent = ((EntityVoidWormPart) parent).getParent();
        }
        if (parent instanceof EntityVoidWorm) {
            return (EntityVoidWorm) parent;
        }
        return null;
    }

    public Entity getChild() {
        UUID id = getChildId();
        if (id != null && !this.getWorld().isClient) {
            return ((ServerWorld) getWorld()).getEntity(id);
        }
        return null;
    }

    public Entity getParent() {
        UUID id = getParentId();
        if (id != null && !this.getWorld().isClient) {
            return ((ServerWorld) getWorld()).getEntity(id);
        }
        return null;
    }

    public void setParent(Entity entity) {
        this.setParentId(entity.getUuid());
    }

    @Override
    public boolean isPartOf(Entity entity) {
        return this == entity || this.getParent() == entity;
    }

    @Override
    public boolean canHit() {
        return true;
    }

    @Nullable
    public ItemStack getPickBlockStack() {
        var parent = this.getParent();
        return parent != null ? parent.getPickBlockStack() : ItemStack.EMPTY;
    }

    @Override
    public Arm getMainArm() {
        return null;
    }

//    @Override
//    public Packet<ClientPlayPacketListener> createSpawnPacket() {
//        return (Packet<ClientPlayPacketListener>) NetworkHooks.getEntitySpawningPacket(this);
//    }

    @Override
    public void tickCramming() {
        List<Entity> entities = this.getWorld().getOtherEntities(this, this.getBoundingBox().stretch(0.20000000298023224D, 0.0D, 0.20000000298023224D));
        Entity parent = this.getParent();
        if (parent != null) {
            entities.stream().filter(entity -> !entity.isPartOf(parent) && !(entity instanceof EntityVoidWormPart) && entity.isPushable()).forEach(entity -> entity.pushAwayFrom(parent));

        }
    }

    @Override
    public ActionResult interact(PlayerEntity player, Hand hand) {
        Entity parent = getParent();
        return parent != null ? parent.interact(player, hand) : ActionResult.PASS;
    }

    public boolean isHurt() {
        return this.getHealth() <= getHealthThreshold();
    }

    public double getHealthThreshold() {
        return 5D;
    }

    @Override
    public boolean damage(DamageSource source, float damage) {
        if (super.damage(source, damage)) {
            var worm = this.getWorm();
            if (worm != null) {
                worm.playHurtSoundWorm(source);
            }
            return true;
        }
        return false;
    }

    @Override
    public Iterable<ItemStack> getArmorItems() {
        return ImmutableList.of();
    }

    @Override
    public ItemStack getEquippedStack(EquipmentSlot slotIn) {
        return ItemStack.EMPTY;
    }

    @Override
    public void equipStack(EquipmentSlot slotIn, ItemStack stack) {
    }

    public boolean isTail() {
        return this.dataTracker.get(TAIL);
    }

    public void setTail(boolean tail) {
        this.dataTracker.set(TAIL, tail);
    }

    public int getBodyIndex() {
        return this.dataTracker.get(BODYINDEX);
    }

    public void setBodyIndex(int index) {
        this.dataTracker.set(BODYINDEX, index);
    }

    public boolean shouldNotExist() {
        Entity parent = getParent();
        return !parent.isAlive();
    }

    @Override
    public void onAttackedFromServer(LivingEntity parent, float damage, DamageSource damageSource) {
        if (parent.deathTime > 0) {
            this.deathTime = parent.deathTime;
        }
        if (parent.hurtTime > 0) {
            this.hurtTime = parent.hurtTime;
        }
    }
// FIXME
//    public boolean shouldContinuePersisting() {
//        return isAddedToWorld() || this.isRemoved();
//    }

    public float getWormYaw(float partialTicks) {
        return partialTicks == 0 ? this.dataTracker.get(WORM_YAW) : prevWormYaw + (this.dataTracker.get(WORM_YAW) - prevWormYaw) * partialTicks;
    }

    public void teleportTo(Vec3d enterPos, Vec3d to) {
        this.setPortalTicks(10);
        teleportPos = to;
        this.enterPos = enterPos;
        var worm = this.getWorm();
        if (worm != null) {
            if (this.getChild() == null) {
                worm.fullyThrough = true;
            }
        }
    }

}
