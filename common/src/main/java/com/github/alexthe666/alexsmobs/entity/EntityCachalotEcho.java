package com.github.alexthe666.alexsmobs.entity;

import com.github.alexthe666.alexsmobs.registry.AMEntityRegistry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class EntityCachalotEcho extends Entity {
    private static final TrackedData<Boolean> RETURNING = DataTracker.registerData(EntityCachalotEcho.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> FASTER_ANIM = DataTracker.registerData(EntityCachalotEcho.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> GREEN = DataTracker.registerData(EntityCachalotEcho.class, TrackedDataHandlerRegistry.BOOLEAN);
    private UUID ownerUUID;
    private int ownerNetworkId;
    private boolean leftOwner;
    private boolean playerLaunched = false;

    public EntityCachalotEcho(EntityType p_i50162_1_, World p_i50162_2_) {
        super(p_i50162_1_, p_i50162_2_);
    }

    public EntityCachalotEcho(World worldIn, EntityCachalotWhale p_i47273_2_) {
        this(AMEntityRegistry.CACHALOT_ECHO.get(), worldIn);
        this.setShooter(p_i47273_2_);
    }

    public EntityCachalotEcho(World worldIn, LivingEntity p_i47273_2_, boolean right, boolean green) {
        this(AMEntityRegistry.CACHALOT_ECHO.get(), worldIn);
        this.setShooter(p_i47273_2_);
        float rot = p_i47273_2_.headYaw + (right ? 90 : -90);
        playerLaunched = true;
        this.setGreen(green);
        this.setFasterAnimation(true);
        this.setPos(p_i47273_2_.getX() - (double) (p_i47273_2_.getWidth()) * 0.5D * (double) MathHelper.sin(rot * MathHelper.RADIANS_PER_DEGREE), p_i47273_2_.getY() + 1D, p_i47273_2_.getZ() + (double) (p_i47273_2_.getWidth()) * 0.5D * (double) MathHelper.cos(rot * MathHelper.RADIANS_PER_DEGREE));
    }

    protected static float lerpRotation(float p_234614_0_, float p_234614_1_) {
        while (p_234614_1_ - p_234614_0_ < -180.0F) {
            p_234614_0_ -= 360.0F;
        }

        while (p_234614_1_ - p_234614_0_ >= 180.0F) {
            p_234614_0_ += 360.0F;
        }

        return net.minecraft.util.math.MathHelper.lerp(0.2F, p_234614_0_, p_234614_1_);
    }

    public boolean isReturning() {
        return this.dataTracker.get(RETURNING);
    }

    public void setReturning(boolean returning) {
        this.dataTracker.set(RETURNING, returning);
    }

    public boolean isFasterAnimation() {
        return this.dataTracker.get(FASTER_ANIM);
    }

    public void setFasterAnimation(boolean anim) {
        this.dataTracker.set(FASTER_ANIM, anim);
    }

    //FIXME FORGE
//    @Override
//    public Packet<ClientPlayPacketListener> getAddEntityPacket() {
//        return (Packet<ClientPlayPacketListener>) NetworkHooks.getEntitySpawningPacket(this);
//    }

    @Override
    public void tick() {
        final double yMot = MathHelper.sqrt((float)(this.getVelocity().x * this.getVelocity().x + this.getVelocity().z * this.getVelocity().z));
        this.setPitch((float) (MathHelper.atan2(this.getVelocity().y, yMot) * MathHelper.DEGREES_PER_RADIAN));
        if (!this.leftOwner) {
            this.leftOwner = this.checkLeftOwner();
        }
        super.tick();
        final var vector3d = this.getVelocity();
        final HitResult raytraceresult = ProjectileUtil.getCollision(this, this::canHit);
        if (raytraceresult.getType() != HitResult.Type.MISS) {
            this.onImpact(raytraceresult);
        }
        final Entity shooter = this.getOwner();
        if (this.isReturning() && shooter instanceof final EntityCachalotWhale whale) {
            if(whale.headPart.distanceTo(this) < whale.headPart.getWidth()){
                remove(RemovalReason.DISCARDED);
                whale.receiveEcho();
            }
        }
        if (!playerLaunched && !this.getWorld().isClient && !this.isInsideWaterOrBubbleColumn()) {
            remove(RemovalReason.DISCARDED);
        }
        if (this.age > 100) {
            remove(RemovalReason.DISCARDED);
        }

        final double d0 = this.getX() + vector3d.x;
        final double d1 = this.getY() + vector3d.y;
        final double d2 = this.getZ() + vector3d.z;

        this.updateRotation();
        if (playerLaunched) {
            this.noClip = true;
        }
        this.setVelocity(vector3d.multiply(0.99F));
        this.setNoGravity(true);
        this.setPos(d0, d1, d2);
        this.setYaw((float) (MathHelper.atan2(vector3d.x, vector3d.z) * MathHelper.DEGREES_PER_RADIAN) - 90);
    }

    protected void onEntityHit(EntityHitResult result) {
        final Entity entity = this.getOwner();
        if (isReturning()) {
            EntityCachalotWhale whale = null;
            if (entity instanceof EntityCachalotWhale) {
                whale = (EntityCachalotWhale) entity;
                if (result.getEntity() instanceof EntityCachalotWhale || result.getEntity() instanceof EntityCachalotPart) {
                    whale.receiveEcho();
                    this.remove(RemovalReason.DISCARDED);
                }
            }
        } else if (result.getEntity() != entity && !result.getEntity().isPartOf(entity)) {
            this.setReturning(true);
            if (entity instanceof EntityCachalotWhale) {
                final Vec3d vec = ((EntityCachalotWhale) entity).getReturnEchoVector();
                final double d0 = vec.x - this.getX();
                final double d1 = vec.y - this.getY();
                final double d2 = vec.z - this.getZ();
                this.setVelocity(Vec3d.ZERO);
                final EntityCachalotEcho echo = new EntityCachalotEcho(this.getWorld(), ((EntityCachalotWhale) entity));
                echo.copyPositionAndRotation(this);
                this.remove(RemovalReason.DISCARDED);
                echo.setReturning(true);
                echo.shoot(d0, d1, d2, 1, 0);
                if (!this.getWorld().isClient) {
                    getWorld().spawnEntity(echo);
                }
            }
        }
    }

    protected void onBlockHit(BlockHitResult p_230299_1_) {
        if (!this.getWorld().isClient && !playerLaunched) {
            this.remove(RemovalReason.DISCARDED);
        }
    }

    @Override
    protected void initDataTracker() {
        this.dataTracker.startTracking(RETURNING, false);
        this.dataTracker.startTracking(FASTER_ANIM, false);
        this.dataTracker.startTracking(GREEN, false);
    }

    public void setShooter(@Nullable Entity entityIn) {
        if (entityIn != null) {
            this.ownerUUID = entityIn.getUuid();
            this.ownerNetworkId = entityIn.getId();
        }

    }

    @Nullable
    public Entity getOwner() {
        if (this.ownerUUID != null && this.getWorld() instanceof ServerWorld) {
            return ((ServerWorld) this.getWorld()).getEntity(this.ownerUUID);
        } else {
            return this.ownerNetworkId != 0 ? this.getWorld().getEntityById(this.ownerNetworkId) : null;
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
        compound.putBoolean("Green", isGreen());
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound compound) {
        if (compound.containsUuid("Owner")) {
            this.ownerUUID = compound.getUuid("Owner");
        }
        this.setGreen(compound.getBoolean("Green"));
        this.leftOwner = compound.getBoolean("LeftOwner");
    }

    private boolean checkLeftOwner() {
        var entity = this.getOwner();
        if (entity != null) {
            for (var entity1 : this.getWorld().getOtherEntities(this, this.getBoundingBox()
                    .stretch(this.getVelocity()).expand(1.0D), (p_234613_0_) -> !p_234613_0_.isSpectator() && p_234613_0_.canHit())) {
                if (entity1.getRootVehicle() == entity.getRootVehicle()) {
                    return false;
                }
            }
        }

        return true;
    }

    public void shoot(double x, double y, double z, float velocity, float inaccuracy) {
        final Vec3d vector3d = (new Vec3d(x, y, z))
                .normalize()
                .add(this.random.nextGaussian() * 0.0075D * (double) inaccuracy, this.random.nextGaussian() * 0.0075D * (double) inaccuracy, this.random.nextGaussian() * 0.0075D * (double) inaccuracy)
                .multiply(velocity);
        this.setVelocity(vector3d);
        final float f = MathHelper.sqrt((float) horizontalMag(vector3d));
        this.setYaw((float) (MathHelper.atan2(vector3d.x, vector3d.z) * MathHelper.DEGREES_PER_RADIAN));
        this.setPitch((float) (MathHelper.atan2(vector3d.y, f) * MathHelper.DEGREES_PER_RADIAN));
        this.prevYaw = this.getYaw();
        this.prevPitch = this.getPitch();
    }

    private double horizontalMag(Vec3d vector3d) {
        return vector3d.x * vector3d.x + vector3d.z * vector3d.z;
    }

    public void shootFromRotation(Entity p_234612_1_, float p_234612_2_, float p_234612_3_, float p_234612_4_, float p_234612_5_, float p_234612_6_) {
        final float f3 = p_234612_3_ * MathHelper.RADIANS_PER_DEGREE;
        final float f0 = MathHelper.cos(p_234612_2_ * MathHelper.RADIANS_PER_DEGREE);
        final float f = -MathHelper.sin(f3) * f0;
        final float f1 = -MathHelper.sin((p_234612_2_ + p_234612_4_) * MathHelper.RADIANS_PER_DEGREE);
        final float f2 = MathHelper.cos(f3) * f0;
        this.shoot(f, f1, f2, p_234612_5_, p_234612_6_);
        var vector3d = p_234612_1_.getVelocity();
        this.setVelocity(this.getVelocity().add(vector3d.x, p_234612_1_.isOnGround() ? 0.0D : vector3d.y, vector3d.z));
    }

    /**
     * Called when this EntityFireball hits a block or entity.
     */
    protected void onImpact(HitResult result) {
        HitResult.Type raytraceresult$type = result.getType();
        if(playerLaunched){
            return;
        }
        if (raytraceresult$type == HitResult.Type.ENTITY) {
            this.onEntityHit((EntityHitResult) result);
        } else if (raytraceresult$type == HitResult.Type.BLOCK) {
            this.onBlockHit((BlockHitResult) result);
        }

    }

    @Override
    public void setVelocityClient(double x, double y, double z) {
        this.setVelocity(x, y, z);
        if (this.prevPitch == 0.0F && this.prevYaw == 0.0F) {
            final float f = MathHelper.sqrt((float)(x * x + z * z));
            this.setPitch((float) (MathHelper.atan2(y, f) * MathHelper.DEGREES_PER_RADIAN));
            this.setYaw((float) (MathHelper.atan2(x, z) * MathHelper.DEGREES_PER_RADIAN));
            this.prevPitch = this.getPitch();
            this.prevYaw = this.getYaw();
            this.refreshPositionAndAngles(this.getX(), this.getY(), this.getZ(), this.getYaw(), this.getPitch());
        }
    }

    protected boolean canHit(Entity p_230298_1_) {
        if(playerLaunched){
            return false;
        }
        if (this.isReturning()) {
            return p_230298_1_ instanceof EntityCachalotPart || p_230298_1_ instanceof EntityCachalotWhale;
        } else if (p_230298_1_ instanceof EntityCachalotPart) {
            return false;
        }
        if (!p_230298_1_.isSpectator() && p_230298_1_.isAlive() && p_230298_1_.canHit()) {
            var entity = this.getOwner();
            return (entity == null || this.leftOwner || !entity.isConnectedThroughVehicle(p_230298_1_));
        } else {
            return false;
        }
    }

    protected void updateRotation() {
        final Vec3d vector3d = this.getVelocity();
        final float f = MathHelper.sqrt((float)horizontalMag(vector3d));
        this.setPitch(lerpRotation(this.prevPitch, (float) (MathHelper.atan2(vector3d.y, f) * MathHelper.DEGREES_PER_RADIAN)));
        this.setYaw(lerpRotation(this.prevYaw, (float) (MathHelper.atan2(vector3d.x, vector3d.z) * MathHelper.DEGREES_PER_RADIAN)));
    }

    public boolean isGreen() {
        return dataTracker.get(GREEN);
    }

    public void setGreen(boolean bool) {
        dataTracker.set(GREEN, bool);
    }
}