package com.github.alexthe666.alexsmobs.entity;

import com.github.alexthe666.alexsmobs.AlexsMobs;
import com.github.alexthe666.alexsmobs.entity.util.AnacondaPartIndex;
import com.github.alexthe666.alexsmobs.misc.AMBlockPos;
import com.github.alexthe666.alexsmobs.packet.HurtMultipartPacket;
import com.google.common.collect.ImmutableList;
import net.minecraft.block.AbstractBlock;
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
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.function.BooleanBiFunction;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

public class EntityAnacondaPart extends LivingEntity implements IHurtableMultipart {
    private static final TrackedData<Integer> BODYINDEX = DataTracker.registerData(EntityAnacondaPart.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> BODY_TYPE = DataTracker.registerData(EntityAnacondaPart.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Float> TARGET_YAW = DataTracker.registerData(EntityAnacondaPart.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Optional<UUID>> CHILD_UUID = DataTracker.registerData(EntityAnacondaPart.class, TrackedDataHandlerRegistry.OPTIONAL_UUID);
    private static final TrackedData<Optional<UUID>> PARENT_UUID = DataTracker.registerData(EntityAnacondaPart.class, TrackedDataHandlerRegistry.OPTIONAL_UUID);
    private static final TrackedData<Float> SWELL = DataTracker.registerData(EntityAnacondaPart.class, TrackedDataHandlerRegistry.FLOAT);
    public EntityDimensions multipartSize;
    private float strangleProgess;
    private float prevSwell;
    private float prevStrangleProgess;
    private int headEntityId = -1;
    private double prevHeight = 0;
//    public Vec3[] stranglePosition = new Vec3[]{
//            new Vec3(0.5, 0, 0),
//            new Vec3(-0.5, 0, 0),
//            new Vec3(-1, 0, 0),
//            new Vec3(0, 0, 0),
//            new Vec3(1, 0, 0),
//            new Vec3(0, 0, 0),
//            new Vec3(-1, 0, 0),
//    };
    private static final TrackedData<Boolean> YELLOW = DataTracker.registerData(EntityAnacondaPart.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> SHEDDING = DataTracker.registerData(EntityAnacondaPart.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> BABY = DataTracker.registerData(EntityAnacondaPart.class, TrackedDataHandlerRegistry.BOOLEAN);

    public EntityAnacondaPart(EntityType<? extends EntityAnacondaPart> t, World world) {
        super(t, world);
        multipartSize = t.getDimensions();
    }

    public EntityAnacondaPart(EntityType<? extends EntityAnacondaPart> t, LivingEntity parent) {
        super(t, parent.getWorld());
        this.setParent(parent);
    }

    @Override
    public ActionResult interact(PlayerEntity playerEntity, Hand hand) {
        return this.getParent() == null ? super.interact(playerEntity, hand) : this.getParent().interact(playerEntity, hand);
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createLivingAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 10.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.15F);
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return source.isOf(DamageTypes.IN_WALL)  || super.isInvulnerableTo(source);
    }

    public boolean isNoGravity() {
        return false;
    }


    @Override
    public void tick() {
        super.tick();

        prevStrangleProgess = strangleProgess;
        prevSwell = this.getSwell();
        inNetherPortal = false;
        this.setVelocity(Vec3d.ZERO);
        if (this.age > 1) {
            final Entity parent = getParent();
            calculateDimensions();
            if (!this.getWorld().isClient) {
                if (parent == null) {
                    this.remove(RemovalReason.DISCARDED);
                }
                if (parent != null) {
                    if (parent instanceof final LivingEntity livingEntityParent) {
                        if (livingEntityParent.hurtTime > 0 || livingEntityParent.deathTime > 0) {
                            AlexsMobs.sendMSGToAll(new HurtMultipartPacket(this.getId(), parent.getId(), 0));
                            this.hurtTime = livingEntityParent.hurtTime;
                            this.deathTime = livingEntityParent.deathTime;
                        }
                    }
                    if (parent.isRemoved()) {
                        this.remove(RemovalReason.DISCARDED);
                    }
                } else if (age > 20) {
                    remove(RemovalReason.DISCARDED);
                }
                if (this.getSwell() > 0) {
                    final float swellInc = 0.25F;
                    if (parent instanceof EntityAnaconda || parent instanceof EntityAnacondaPart && ((EntityAnacondaPart) parent).getSwell() == 0) {
                        if (this.getChild() != null) {
                            final EntityAnacondaPart child = (EntityAnacondaPart) this.getChild();
                            if (child.getPartType() == AnacondaPartIndex.TAIL) {
                                if (this.getSwell() == swellInc) {
                                    this.feedAnaconda();
                                }
                            } else {
                                child.setSwell(child.getSwell() + swellInc);
                            }
                        }
                        this.setSwell(this.getSwell() - swellInc);
                    }
                }
            }
        }
    }

    private void feedAnaconda() {
        Entity e = this.getParent();
        while (e instanceof EntityAnacondaPart) {
            e = ((EntityAnacondaPart) e).getParent();
        }

        if (e instanceof EntityAnaconda)
            ((EntityAnaconda) e).feed();
    }

    public Vec3d tickMultipartPosition(int headId, AnacondaPartIndex parentIndex, Vec3d parentPosition, float parentXRot, float parentYRot, float ourYRot, boolean doHeight) {
        final var parentButt = parentPosition.add(calcOffsetVec(-parentIndex.getBackOffset() * this.getScaleFactor(), parentXRot, parentYRot));
        final var ourButt = parentButt.add(calcOffsetVec((-this.getPartType().getBackOffset() - 0.5F * this.getWidth()) * this.getScaleFactor(), this.getPitch(), ourYRot));
        final var avg = new Vec3d((parentButt.x + ourButt.x) / 2F, (parentButt.y + ourButt.y) / 2F, (parentButt.z + ourButt.z) / 2F);
        final double d0 = parentButt.x - ourButt.x;
//        final double d1 = parentButt.y - ourButt.y;
        final double d2 = parentButt.z - ourButt.z;
        final double d3 = Math.sqrt(d0 * d0 + d2 * d2);
        final double hgt = doHeight ? (getLowPartHeight(parentButt.x, parentButt.y, parentButt.z) + getHighPartHeight(ourButt.x, ourButt.y, ourButt.z)) : 0;
        if (Math.abs(hgt - prevHeight) > 0.2F) {
            prevHeight = hgt;
        }
        final double partYDest = MathHelper.clamp(this.getScaleFactor() * prevHeight, -0.6F, 0.6F);
        final float f = (float) (MathHelper.atan2(d2, d0) * 57.2957763671875D) - 90.0F;
        final float rawAngle = MathHelper.wrapDegrees((float) (-(MathHelper.atan2(partYDest, d3) * MathHelper.DEGREES_PER_RADIAN)));
        final float f2 = this.limitAngle(this.getPitch(), rawAngle, 10F);
        this.setPitch(f2);
        this.setYaw(f);
        this.prevHeadYaw = f;
        this.refreshPositionAndAngles(avg.x, avg.y, avg.z, f, f2);
        headEntityId = headId;
        return avg;
    }

    public double getLowPartHeight(double x, double yIn, double z) {
        if (isFluidAt(x, yIn, z))
            return 0.0D;

        double checkAt = 0D;
        while (checkAt > -3D && !isOpaqueBlockAt(x,yIn + checkAt, z)) {
            checkAt -= 0.2D;
        }

        return checkAt;
    }

    public double getHighPartHeight(double x, double yIn, double z) {
        if (isFluidAt(x, yIn, z))
            return 0.0D;

        double checkAt = 0D;
        while (checkAt <= 3D) {
            if (isOpaqueBlockAt(x, yIn + checkAt, z)) {
                checkAt += 0.2D;
            } else {
                break;
            }
        }

        return checkAt;
    }

    public boolean isOpaqueBlockAt(double x, double y, double z) {
        if (this.noClip) {
            return false;
        } else {
            final double d = 1D;
            final var vec3 = new Vec3d(x, y, z);
            final var axisAlignedBB = Box.of(vec3, d, 1.0E-6D, d);
            return this.getWorld().getStatesInBox(axisAlignedBB).filter(Predicate.not(AbstractBlock.AbstractBlockState::isAir)).anyMatch((p_185969_) -> {
                var blockpos = AMBlockPos.fromVec3(vec3);
                return p_185969_.shouldSuffocate(this.getWorld(), blockpos) && VoxelShapes.matchesAnywhere(p_185969_.getCollisionShape(this.getWorld(), blockpos).offset(vec3.x, vec3.y, vec3.z), VoxelShapes.cuboid(axisAlignedBB), BooleanBiFunction.AND);
            });
        }
    }

    @Override
    public boolean canBreatheInWater() {
        return true;
    }

    @Override
    public boolean isPushedByFluids() {
        return false;
    }

    public boolean isFluidAt(double x, double y, double z) {
        if (this.noClip) {
            return false;
        } else {
            return !getWorld().getFluidState(AMBlockPos.fromCoords(x, y, z)).isEmpty();
        }
    }

    public boolean hurtHeadId(DamageSource source, float f) {
        if (headEntityId != -1) {
            Entity e = getWorld().getEntityById(headEntityId);
            if (e instanceof EntityAnaconda) {
               return e.damage(source, f);
            }
        }
        return false;
    }

    @Override
    public boolean damage(DamageSource source, float damage) {
        return hurtHeadId(source, damage);
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(CHILD_UUID, Optional.empty());
        this.dataTracker.startTracking(PARENT_UUID, Optional.empty());
        this.dataTracker.startTracking(BODYINDEX, 0);
        this.dataTracker.startTracking(BODY_TYPE, AnacondaPartIndex.NECK.ordinal());
        this.dataTracker.startTracking(TARGET_YAW, 0F);
        this.dataTracker.startTracking(SWELL, 0F);
        this.dataTracker.startTracking(YELLOW, false);
        this.dataTracker.startTracking(SHEDDING, false);
        this.dataTracker.startTracking(BABY, false);
    }

    @Override
    public void tickCramming() {
        final List<Entity> entities = this.getWorld().getOtherEntities(this, this.getBoundingBox().stretch(0.2D, 0.0D, 0.2D));
        final var parent = this.getParent();
        if (parent != null) {
            entities.stream().filter(entity -> !entity.isPartOf(parent) && !(entity instanceof EntityAnacondaPart || entity instanceof EntityAnaconda) && entity.isPushable()).forEach(entity -> entity.pushAwayFrom(parent));
        }
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
    public void equipStack(EquipmentSlot p_21036_, ItemStack p_21037_) {
    }

    @Override
    public Arm getMainArm() {
        return Arm.RIGHT;
    }

    @Override
    public void onAttackedFromServer(LivingEntity parent, float damage, DamageSource damageSource) {
        if (parent.deathTime > 0)
            this.deathTime = parent.deathTime;

        if (parent.hurtTime > 0)
            this.hurtTime = parent.hurtTime;
    }

    public Entity getParent() {
        if (!this.getWorld().isClient) {
            final UUID id = getParentId();
            if (id != null) {
                return ((ServerWorld) getWorld()).getEntity(id);
            }
        }

        return null;
    }

    public void setParent(Entity entity) {
        this.setParentId(entity.getUuid());
    }

    @Nullable
    public UUID getParentId() {
        return this.dataTracker.get(PARENT_UUID).orElse(null);
    }

    public void setParentId(@Nullable UUID uniqueId) {
        this.dataTracker.set(PARENT_UUID, Optional.ofNullable(uniqueId));
    }

    public Entity getChild() {
        if (!this.getWorld().isClient) {
            final UUID id = getChildId();
            if (id != null) {
                return ((ServerWorld) getWorld()).getEntity(id);
            }
        }

        return null;
    }

    @Nullable
    public UUID getChildId() {
        return this.dataTracker.get(CHILD_UUID).orElse(null);
    }

    public void setChildId(@Nullable UUID uniqueId) {
        this.dataTracker.set(CHILD_UUID, Optional.ofNullable(uniqueId));
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
        compound.putInt("BodyModel", getPartType().ordinal());
        compound.putInt("BodyIndex", getBodyIndex());
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
        this.setPartType(AnacondaPartIndex.fromOrdinal(compound.getInt("BodyModel")));
        this.setBodyIndex(compound.getInt("BodyIndex"));
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
    @Override
    public ItemStack getPickBlockStack() {
        Entity parent = this.getParent();
        return parent != null ? parent.getPickBlockStack() : ItemStack.EMPTY;
    }

    public int getBodyIndex() {
        return this.dataTracker.get(BODYINDEX);
    }

    public void setBodyIndex(int index) {
        this.dataTracker.set(BODYINDEX, index);
    }

    public AnacondaPartIndex getPartType() {
        return AnacondaPartIndex.fromOrdinal(this.dataTracker.get(BODY_TYPE));
    }

    public void setPartType(AnacondaPartIndex index) {
        this.dataTracker.set(BODY_TYPE, index.ordinal());
    }

    public void setTargetYaw(float f) {
        this.dataTracker.set(TARGET_YAW, f);
    }

    public void setSwell(float f) {
        this.dataTracker.set(SWELL, f);
    }

    public float getSwell(){
        return Math.min(this.dataTracker.get(SWELL), 5);
    }

    public float getSwellLerp(float partialTick) {
        return this.prevSwell + (Math.max(this.getSwell(), 0) - this.prevSwell) * partialTick;
    }

    @Override
    public float getYaw() {
        return super.getYaw();
    }

    public void setStrangleProgress(float f){
        this.strangleProgess = f;
    }

    public float getStrangleProgress(float partialTick){
        return this.prevStrangleProgess + (this.strangleProgess - this.prevStrangleProgess) * partialTick;
    }

    public void copyDataFrom(EntityAnaconda anaconda) {
        this.dataTracker.set(YELLOW, anaconda.isYellow());
        this.dataTracker.set(SHEDDING, anaconda.isShedding());
        this.dataTracker.set(BABY, anaconda.isBaby());
    }

    public boolean isYellow(){
        return this.dataTracker.get(YELLOW);
    }

    public boolean isShedding(){
        return this.dataTracker.get(SHEDDING);
    }

    @Override
    public boolean isBaby(){
        return this.dataTracker.get(BABY);
    }
}
