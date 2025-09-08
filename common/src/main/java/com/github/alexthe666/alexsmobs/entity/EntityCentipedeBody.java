package com.github.alexthe666.alexsmobs.entity;

import com.github.alexthe666.alexsmobs.AlexsMobs;
import com.github.alexthe666.alexsmobs.misc.AMBlockPos;
import com.github.alexthe666.alexsmobs.packet.HurtMultipartPacket;
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
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.function.BooleanBiFunction;
import net.minecraft.util.math.BlockPos;
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

public class EntityCentipedeBody extends MobEntity implements IHurtableMultipart {

    private static final TrackedData<Integer> BODYINDEX = DataTracker.registerData(EntityCentipedeBody.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Float> BODY_XROT = DataTracker.registerData(EntityCentipedeBody.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Optional<UUID>> PARENT_UUID = DataTracker.registerData(EntityCentipedeBody.class, TrackedDataHandlerRegistry.OPTIONAL_UUID);
    private static final TrackedData<Optional<UUID>> CHILD_UUID = DataTracker.registerData(EntityCentipedeBody.class, TrackedDataHandlerRegistry.OPTIONAL_UUID);
    public EntityDimensions multipartSize;
    protected float radius;
    protected float angleYaw;
    protected float damageMultiplier = 1;
    private double prevHeight = 0;

    public EntityCentipedeBody(EntityType<? extends EntityCentipedeBody> type, World worldIn) {
        super(type, worldIn);
        multipartSize = type.getDimensions();
    }

    @Override
    public boolean cannotDespawn() {
        return super.cannotDespawn() || this.getParent() != null;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return  source.isOf(DamageTypes.IN_WALL)  || super.isInvulnerableTo(source);
    }

    @Override
    public EntityGroup getGroup() {
        return EntityGroup.ARTHROPOD;
    }

    @Override
    public boolean hasNoGravity() {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        inNetherPortal = false;
        this.setVelocity(Vec3d.ZERO);
        if (this.age > 1) {
            final var parent = getParent();
            calculateDimensions();
            if (parent != null && !this.getWorld().isClient) {
                if (parent instanceof final LivingEntity parentEntity) {
                    if ((parentEntity.hurtTime > 0 || parentEntity.deathTime > 0)) {
                        AlexsMobs.sendMSGToAll(new HurtMultipartPacket(this.getId(), parent.getId(), 0));
                        this.hurtTime = parentEntity.hurtTime;
                        this.deathTime = parentEntity.deathTime;
                    }
                }
                if (parent.isRemoved()) {
                    this.remove(RemovalReason.DISCARDED);
                }
            } else if (!this.getWorld().isClient && age > 20) {
                remove(RemovalReason.DISCARDED);
            }
        }
    }

    public EntityCentipedeBody(EntityType t, LivingEntity parent, float radius, float angleYaw, float offsetY) {
        super(t, parent.getWorld());
        this.setParent(parent);
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
        if (compound.containsUuid("ChildUUID")) {
            this.setChildId(compound.getUuid("ChildUUID"));
        }
        this.setBodyIndex(compound.getInt("BodyIndex"));
        this.angleYaw = compound.getFloat("PartAngle");
        this.radius = compound.getFloat("PartRadius");
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(PARENT_UUID, Optional.empty());
        this.dataTracker.startTracking(CHILD_UUID, Optional.empty());
        this.dataTracker.startTracking(BODYINDEX, 0);
        this.dataTracker.startTracking(BODY_XROT, 0F);
    }

    public Entity getParent() {
        final UUID id = getParentId();
        if (id != null && !this.getWorld().isClient) {
            return ((ServerWorld) getWorld()).getEntity(id);
        }
        return null;
    }

    public void setParent(Entity entity) {
        this.setParentId(entity.getUuid());
    }

    public Entity getChild() {
        final UUID id = getChildId();
        if (id != null && !this.getWorld().isClient) {
            return ((ServerWorld) getWorld()).getEntity(id);
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
    public boolean isPartOf(Entity entity) {
        return this == entity || this.getParent() == entity;
    }

    @Override
    public boolean damage(DamageSource source, float damage) {
        final var parent = getParent();
        final boolean prev = parent != null && parent.damage(source, damage * this.damageMultiplier);
        if (prev && !this.getWorld().isClient) {
            AlexsMobs.sendMSGToAll(new HurtMultipartPacket(this.getId(), parent.getId(), damage * this.damageMultiplier));
        }
        return prev;
    }

    @Override
    public boolean canHit() {
        return true;
    }

    @Override
    public void tickCramming() {
        final List<Entity> entities = this.getWorld().getOtherEntities(this, this.getBoundingBox().stretch(0.2D, 0.0D, 0.2D));
        final Entity parent = this.getParent();
        if (parent != null) {
            entities.stream().filter(entity -> entity != parent && !(entity instanceof EntityCentipedeBody) && entity.isPushable()).forEach(entity -> entity.pushAwayFrom(parent));
        }
    }

    @Override
    public boolean startRiding(Entity entityIn) {
        if(!(entityIn instanceof AbstractMinecartEntity || entityIn instanceof BoatEntity)){
            return super.startRiding(entityIn);
        }
        return false;
    }

    public int getBodyIndex() {
        return this.dataTracker.get(BODYINDEX);
    }

    public void setBodyIndex(int index) {
        this.dataTracker.set(BODYINDEX, index);
    }

    @Nullable
    public UUID getParentId() {
        return this.dataTracker.get(PARENT_UUID).orElse(null);
    }

    public void setParentId(@Nullable UUID uniqueId) {
        this.dataTracker.set(PARENT_UUID, Optional.ofNullable(uniqueId));
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createLivingAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 10.0D)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32.0D)
                .add(EntityAttributes.GENERIC_ARMOR, 6.0D)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 8.0D)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.5F)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.25F);
    }

    public Vec3d tickMultipartPosition(int headId, float parentOffset, Vec3d parentPosition, float parentXRot, float ourYRot, boolean doHeight) {
        final float yDif = doHeight ? 1.0F - 0.95F * (float)Math.min(Math.abs(parentPosition.y - this.getY()), 1.0F) : 1F;
        final var parentFront = parentPosition.add(calcOffsetVec(yDif * parentOffset * this.getScaleFactor(), parentXRot, ourYRot));
        final var parentButt = parentPosition.add(calcOffsetVec(yDif * -parentOffset * this.getScaleFactor(), parentXRot, ourYRot));
        final var ourButt = parentButt.add(calcOffsetVec((yDif * -getBackOffset() - 0.5F * this.getWidth()) * this.getScaleFactor(), this.getPitch(), ourYRot));
        final var avg = new Vec3d((parentButt.x + ourButt.x) / 2F, (parentButt.y + ourButt.y) / 2F, (parentButt.z + ourButt.z) / 2F);
        final double d0 = parentButt.x - ourButt.x;
        final double d2 = parentButt.z - ourButt.z;
        final double d3 = Math.sqrt(d0 * d0 + d2 * d2);
        final double hgt = doHeight ? (getLowPartHeight(parentButt.x, parentButt.y, parentButt.z) + getHighPartHeight(ourButt.x, ourButt.y, ourButt.z)) : 0;
        if(Math.abs(prevHeight - hgt) > 0.2){
            prevHeight = hgt;
        }
        if(!isOpaqueBlockAt(parentFront.x,parentFront.y + 0.4F, parentFront.z) && Math.abs(prevHeight) > 1){
            prevHeight = 0;
        }
        final double partYDest = MathHelper.clamp(prevHeight, -0.4F, 0.4F);
        final float f = (float) (MathHelper.atan2(d2, d0) * 57.2957763671875D) - 90.0F;
        final float rawAngle = MathHelper.wrapDegrees((float) (-(MathHelper.atan2(partYDest, d3) * MathHelper.DEGREES_PER_RADIAN)));
        final float f2 = this.limitAngle(this.getPitch(), rawAngle, 10);
        this.setPitch(f2);
        this.dataTracker.set(BODY_XROT, f2);
        this.setYaw(f);
        this.headYaw= f;
        this.refreshPositionAndAngles(avg.x, avg.y, avg.z, f, f2);
        return avg;
    }

    @Override
    public float getPitch() {
        return this.dataTracker.get(BODY_XROT);
    }

    public double getLowPartHeight(double x, double yIn, double z) {
        if(isFluidAt(x, yIn, z)){
            return 0.0;
        }
        double checkAt = 0D;
        while (checkAt > -3D && !isOpaqueBlockAt(x,yIn + checkAt, z)) {
            checkAt -= 0.2D;
        }
        return checkAt;
    }

    public double getHighPartHeight(double x, double yIn, double z) {
        if(isFluidAt(x, yIn, z)){
            return 0.0;
        }
        double checkAt = 0D;
        while (checkAt <= 3) {
            if(isOpaqueBlockAt(x, yIn + checkAt, z)) {
                checkAt += 0.2D;
            }else{
                break;
            }
        }
        return checkAt;
    }

    public boolean isFluidAt(double x, double y, double z) {
        if (this.noClip) {
            return false;
        } else {
            return !getWorld().getFluidState(AMBlockPos.fromCoords(x, y, z)).isEmpty();
        }
    }

    public boolean isOpaqueBlockAt(double x, double y, double z) {
        if (this.noClip) {
            return false;
        } else {
            final float f = 1F;
            final var vec3 = new Vec3d(x, y, z);
            final var axisalignedbb = Box.of(vec3, f, 1.0E-6D, f);
            return this.getWorld().getStatesInBox(axisalignedbb).filter(Predicate.not(AbstractBlock.AbstractBlockState::isAir)).anyMatch((p_185969_) -> {
                final BlockPos blockpos = AMBlockPos.fromVec3(vec3);
                return p_185969_.shouldSuffocate(this.getWorld(), blockpos)
                        && VoxelShapes.matchesAnywhere(p_185969_.getCollisionShape(this.getWorld(), blockpos).offset(vec3.x, vec3.y, vec3.z), VoxelShapes.cuboid(axisalignedbb), BooleanBiFunction.AND);
            });
        }
    }

    @Override
    public boolean canBreatheInWater() {
        return true;
    }

    public float getBackOffset() {
        return 0.5F;
    }

    @Override
    public void onAttackedFromServer(LivingEntity parent, float damage, DamageSource damageSource) {
        if(parent.deathTime > 0){
            this.deathTime = parent.deathTime;
        }
        if(parent.hurtTime > 0){
            this.hurtTime = parent.hurtTime;
        }
    }
}