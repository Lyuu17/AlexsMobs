package com.github.alexthe666.alexsmobs.entity;

import dev.architectury.networking.NetworkManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public abstract class EntityMobProjectile extends Entity {
    private UUID ownerUUID;
    private int ownerNetworkId;
    private boolean leftOwner;

    public EntityMobProjectile(EntityType<? extends EntityMobProjectile> type, World level) {
        super(type, level);
    }

    public EntityMobProjectile(EntityType type, World worldIn, MobEntity shooter) {
        this(type, worldIn);
        this.setShooter(shooter);
    }

    protected Vec3d calcOffsetVec(Vec3d offset, float xRot, float yRot){
        return offset.rotateX(xRot * MathHelper.RADIANS_PER_DEGREE).rotateY(-yRot * MathHelper.RADIANS_PER_DEGREE);
    }

    protected static float lerpRotation(float f, float f1) {
        while (f1 - f < -180.0F) {
            f -= 360.0F;
        }

        while (f1 - f >= 180.0F) {
            f += 360.0F;
        }

        return MathHelper.lerp(0.2F, f, f1);
    }

    @Override
    public Packet<ClientPlayPacketListener> createSpawnPacket() {
        return NetworkManager.createAddEntityPacket(this);
    }

    @Override
    protected void initDataTracker() {
    }

    @Override
    public void tick() {
        if (!this.leftOwner) {
            this.leftOwner = this.checkLeftOwner();
        }
        this.doBehavior();
        super.tick();
        Vec3d vector3d = this.getVelocity();
        HitResult raytraceresult = ProjectileUtil.getCollision(this, this::canHitEntity);
        if (raytraceresult != null && raytraceresult.getType() != HitResult.Type.MISS) {
            this.onImpact(raytraceresult);
        }
        double d0 = this.getX() + vector3d.x;
        double d1 = this.getY() + vector3d.y;
        double d2 = this.getZ() + vector3d.z;
        this.updateRotation();
        if (this.isInsideWall() && (!isTouchingWater() || removeInWater())) {
            this.remove(RemovalReason.DISCARDED);
        } else if (this.isInsideWaterOrBubbleColumn() && this.removeInWater()) {
            this.remove(RemovalReason.DISCARDED);
        } else {
            this.setVelocity(vector3d.multiply(0.99F));
            this.setPos(d0, d1, d2);
        }
    }

    protected boolean removeInWater(){
        return true;
    }

    public abstract void doBehavior();

    protected void onEntityHit(EntityHitResult result) {
        Entity entity = this.getShooter();
        if (entity instanceof LivingEntity) {
            boolean b = result.getEntity().damage(getDamageSources().mobProjectile(this, (LivingEntity) entity), this.getDamage());

        }
        this.remove(RemovalReason.DISCARDED);
    }

    protected abstract float getDamage();

    protected void onHitBlock(BlockHitResult p_230299_1_) {
        if (!this.getWorld().isClient) {
            this.remove(RemovalReason.DISCARDED);
        }
    }

    @Nullable
    public Entity getShooter() {
        if (this.ownerUUID != null && this.getWorld() instanceof ServerWorld) {
            return ((ServerWorld) this.getWorld()).getEntity(this.ownerUUID);
        } else {
            return this.ownerNetworkId != 0 ? this.getWorld().getEntityById(this.ownerNetworkId) : null;
        }
    }

    public void setShooter(@Nullable Entity entityIn) {
        if (entityIn != null) {
            this.ownerUUID = entityIn.getUuid();
            this.ownerNetworkId = entityIn.getId();
        }

    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound compound) {
        if (this.ownerUUID != null) {
            compound.putUuid("Owner", this.ownerUUID);
        }

        if (this.leftOwner) {
            compound.putBoolean("LeftOwner", true);
        }
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound compound) {
        if (compound.containsUuid("Owner")) {
            this.ownerUUID = compound.getUuid("Owner");
        }

        this.leftOwner = compound.getBoolean("LeftOwner");
    }

    private boolean checkLeftOwner() {
        Entity entity = this.getShooter();
        if (entity != null) {
            for (Entity entity1 : this.getWorld().getOtherEntities(this, this.getBoundingBox().stretch(this.getVelocity()).expand(1.0D), (p_234613_0_) -> !p_234613_0_.isSpectator() && p_234613_0_.canHit())) {
                if (entity1.getRootVehicle() == entity.getRootVehicle()) {
                    return false;
                }
            }
        }

        return true;
    }

    public void shoot(double x, double y, double z, float velocity, float inaccuracy) {
        Vec3d vector3d = (new Vec3d(x, y, z))
                .normalize()
                .add(this.random.nextGaussian() * (double) 0.0075F * (double) inaccuracy, this.random.nextGaussian() * (double) 0.0075F * (double) inaccuracy, this.random.nextGaussian() * (double) 0.0075F * (double) inaccuracy)
                .multiply(velocity);
        this.setVelocity(vector3d);
        float f = MathHelper.sqrt((float) vector3d.horizontalLengthSquared());
        this.setYaw((float) (MathHelper.atan2(vector3d.x, vector3d.z) * (double) MathHelper.DEGREES_PER_RADIAN));
        this.setPitch((float) (MathHelper.atan2(vector3d.y, f) * (double) MathHelper.DEGREES_PER_RADIAN));
        this.prevYaw = this.getYaw();
        this.prevPitch = this.getPitch();
    }

    public void shootFromRotation(Entity p_234612_1_, float p_234612_2_, float p_234612_3_, float p_234612_4_, float p_234612_5_, float p_234612_6_) {
        float f = -MathHelper.sin(p_234612_3_ * MathHelper.RADIANS_PER_DEGREE) * MathHelper.cos(p_234612_2_ * MathHelper.RADIANS_PER_DEGREE);
        float f1 = -MathHelper.sin((p_234612_2_ + p_234612_4_) * MathHelper.RADIANS_PER_DEGREE);
        float f2 = MathHelper.cos(p_234612_3_ * MathHelper.RADIANS_PER_DEGREE) * MathHelper.cos(p_234612_2_ * MathHelper.RADIANS_PER_DEGREE);
        this.shoot(f, f1, f2, p_234612_5_, p_234612_6_);
        Vec3d vector3d = p_234612_1_.getVelocity();
        this.setVelocity(this.getVelocity().add(vector3d.x, p_234612_1_.isOnGround() ? 0.0D : vector3d.y, vector3d.z));
    }

    /**
     * Called when this EntityFireball hits a block or entity.
     */
    protected void onImpact(HitResult result) {
        HitResult.Type raytraceresult$type = result.getType();
        if (raytraceresult$type == HitResult.Type.ENTITY) {
            this.onEntityHit((EntityHitResult) result);
        } else if (raytraceresult$type == HitResult.Type.BLOCK) {
            this.onHitBlock((BlockHitResult) result);
        }
    }

    @Override
    @Environment(EnvType.CLIENT)
    public void setVelocityClient(double x, double y, double z) {
        this.setVelocity(x, y, z);
        if (this.prevPitch == 0.0F && this.prevYaw == 0.0F) {
            float f = MathHelper.sqrt((float) (x * x + z * z));
            this.setPitch((float) (MathHelper.atan2(y, f) * (double) MathHelper.DEGREES_PER_RADIAN));
            this.setYaw((float) (MathHelper.atan2(x, z) * (double) MathHelper.DEGREES_PER_RADIAN));
            this.prevPitch = this.getPitch();
            this.prevYaw = this.getYaw();
            this.refreshPositionAndAngles(this.getX(), this.getY(), this.getZ(), this.getYaw(), this.getPitch());
        }
    }

    protected boolean canHitEntity(Entity inQuestion) {
        if (!inQuestion.isSpectator() && inQuestion.isAlive() && inQuestion.canHit()) {
            Entity entity = this.getShooter();
            return (entity == null || this.leftOwner || !entity.isConnectedThroughVehicle(inQuestion)) && (entity == null || inQuestion == null || !isSameTeam(entity, inQuestion));
        } else {
            return false;
        }
    }

    public boolean isSameTeam(Entity shooter, Entity entity) {
        if(shooter instanceof TameableEntity tamableAnimal && tamableAnimal.isTamed()){
            if(entity instanceof TameableEntity alsoTameable && alsoTameable.isTamed()){
                if(alsoTameable.getOwnerUuid() != null && tamableAnimal.getOwnerUuid() != null && tamableAnimal.getOwnerUuid().equals(alsoTameable.getOwnerUuid())){
                    return true;
                }
            }
            return tamableAnimal.getOwnerUuid() != null && tamableAnimal.getOwnerUuid().equals(entity.getUuid()) || shooter.isTeammate(entity);
        }
        return shooter.isTeammate(entity);
    }

    protected void updateRotation() {
        Vec3d vector3d = this.getVelocity();
        float f = MathHelper.sqrt((float) vector3d.horizontalLength());
        this.setPitch(lerpRotation(this.prevPitch, (float) (MathHelper.atan2(vector3d.y, f) * (double) MathHelper.DEGREES_PER_RADIAN)));
        this.setYaw(lerpRotation(this.prevYaw, (float) (MathHelper.atan2(vector3d.x, vector3d.z) * (double) MathHelper.DEGREES_PER_RADIAN)));
    }
}

