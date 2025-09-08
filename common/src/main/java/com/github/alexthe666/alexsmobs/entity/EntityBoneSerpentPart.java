package com.github.alexthe666.alexsmobs.entity;

import com.github.alexthe666.alexsmobs.AlexsMobs;
import com.github.alexthe666.alexsmobs.packet.HurtMultipartPacket;
import com.google.common.collect.ImmutableList;
import net.minecraft.entity.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class EntityBoneSerpentPart extends LivingEntity implements IHurtableMultipart {

    private static final TrackedData<Boolean> TAIL = DataTracker.registerData(EntityBoneSerpentPart.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Integer> BODYINDEX = DataTracker.registerData(EntityBoneSerpentPart.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Optional<UUID>> PARENT_UUID = DataTracker.registerData(EntityBoneSerpentPart.class, TrackedDataHandlerRegistry.OPTIONAL_UUID);
    public EntityDimensions multipartSize;
    protected float radius;
    protected float angleYaw;
    protected float offsetY;
    protected float damageMultiplier = 1;

    public EntityBoneSerpentPart(EntityType<? extends EntityBoneSerpentPart> t, World world) {
        super(t, world);
        multipartSize = t.getDimensions();
    }

    public EntityBoneSerpentPart(EntityType t, LivingEntity parent, float radius, float angleYaw, float offsetY) {
        super(t, parent.getWorld());
        this.setParent(parent);
        this.radius = radius;
        this.angleYaw = (angleYaw + 90.0F) * MathHelper.RADIANS_PER_DEGREE;
        this.offsetY = offsetY;
    }

    @Override
    public EntityGroup getGroup() {
        return EntityGroup.UNDEAD;
    }

    @Override
    public boolean startRiding(Entity entityIn) {
        if(!(entityIn instanceof AbstractMinecartEntity || entityIn instanceof BoatEntity)){
            return super.startRiding(entityIn);
        }
        return false;
    }

    @Nullable
    public ItemStack getPickBlockStack() {
        var parent = this.getParent();
        return parent != null ? parent.getPickBlockStack() : ItemStack.EMPTY;
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createLivingAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 10.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.15F);
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound compound) {
        super.writeCustomDataToNbt(compound);
        if (this.getParentId() != null) {
            compound.putUuid("ParentUUID", this.getParentId());
        }
        compound.putBoolean("TailPart", isTail());
        compound.putInt("BodyIndex", getBodyIndex());
        compound.putFloat("PartAngle", angleYaw);
        compound.putFloat("PartRadius", radius);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound compound) {
        super.readCustomDataFromNbt(compound);
        if (compound.containsUuid("ParentUUID")) {
            this.setParentId(compound.getUuid("ParentUUID"));
        }
        this.setTail(compound.getBoolean("TailPart"));
        this.setBodyIndex(compound.getInt("BodyIndex"));
        this.angleYaw = compound.getFloat("PartAngle");
        this.radius = compound.getFloat("PartRadius");
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(PARENT_UUID, Optional.empty());
        this.dataTracker.startTracking(TAIL, false);
        this.dataTracker.startTracking(BODYINDEX, 0);
    }

    @Nullable
    public UUID getParentId() {
        return this.dataTracker.get(PARENT_UUID).orElse(null);
    }

    public void setParentId(@Nullable UUID uniqueId) {
        this.dataTracker.set(PARENT_UUID, Optional.ofNullable(uniqueId));
    }

    public void setInitialPartPos(Entity parent) {
        this.setPos(parent.prevX + this.radius * Math.cos(parent.getYaw() * MathHelper.RADIANS_PER_DEGREE + this.angleYaw), parent.prevY + this.offsetY, parent.prevZ + this.radius * Math.sin(parent.getYaw() * MathHelper.RADIANS_PER_DEGREE + this.angleYaw));
    }

    @Override
    public void tick() {
        inNetherPortal = false;
        if (this.age > 10) {
            Entity parent = getParent();
            calculateDimensions();
            if (parent != null && !this.getWorld().isClient) {
                this.setNoGravity(true);
                this.setPos(parent.prevX + this.radius * Math.cos(parent.prevYaw * MathHelper.RADIANS_PER_DEGREE + this.angleYaw), parent.prevY + this.offsetY, parent.prevZ + this.radius * Math.sin(parent.prevYaw * MathHelper.RADIANS_PER_DEGREE + this.angleYaw));
                final double d0 = parent.getX() - this.getX();
                final double d1 = parent.getY() - this.getY();
                final double d2 = parent.getZ() - this.getZ();
                final float f2 = -((float) (MathHelper.atan2(d1, MathHelper.sqrt((float)(d0 * d0 + d2 * d2))) * MathHelper.DEGREES_PER_RADIAN));
                this.setPitch(this.limitAngle(this.getPitch(), f2, 5.0F));
                this.scheduleVelocityUpdate();
                this.setYaw(parent.prevYaw);
                this.headYaw= this.getYaw();
                this.bodyYaw = this.prevYaw;
                if (parent instanceof LivingEntity) {
                    if(!this.getWorld().isClient && (((LivingEntity) parent).hurtTime > 0 || ((LivingEntity) parent).deathTime > 0)){
                        AlexsMobs.sendMSGToAll(new HurtMultipartPacket(this.getId(), parent.getId(), 0));
                        this.hurtTime = ((LivingEntity) parent).hurtTime;
                        this.deathTime = ((LivingEntity) parent).deathTime;
                    }
                }
                this.tickCramming();
                if (parent.isRemoved() && !this.getWorld().isClient) {
                    this.remove(RemovalReason.DISCARDED);
                }
            } else if (age > 20 && !this.getWorld().isClient) {
                remove(RemovalReason.DISCARDED);
            }
        }
        super.tick();
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
        List<Entity> entities = this.getWorld().getOtherEntities(this, this.getBoundingBox().stretch(0.2D, 0.0D, 0.2D));
        Entity parent = this.getParent();
        if (parent != null) {
            entities.stream().filter(entity -> entity != parent && !(entity instanceof EntityBoneSerpentPart) && entity.isPushable()).forEach(entity -> entity.pushAwayFrom(parent));
        }
    }

    @Override
    public ActionResult interact(PlayerEntity player, Hand hand) {
        Entity parent = getParent();

        return parent != null ? parent.interact(player, hand) : ActionResult.PASS;
    }

    @Override
    public boolean damage(DamageSource source, float damage) {
        final Entity parent = getParent();
        final boolean prev = parent != null && parent.damage(source, damage * this.damageMultiplier);
        if (prev && !this.getWorld().isClient) {
            AlexsMobs.sendMSGToAll(new HurtMultipartPacket(this.getId(), parent.getId(), damage * this.damageMultiplier));
        }
        return prev;
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

    //FIXME
//    public boolean shouldContinuePersisting() {
//        return isAddedToWorld() || this.isRemoved();
//    }
}
