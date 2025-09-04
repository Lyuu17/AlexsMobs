package com.github.alexthe666.alexsmobs.entity;

import com.github.alexthe666.alexsmobs.registry.AMEntityRegistry;
import net.minecraft.block.AbstractBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
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

public class EntityMosquitoSpit extends Entity {
    private UUID ownerUUID;
    private int ownerNetworkId;
    private boolean leftOwner;

    public EntityMosquitoSpit(EntityType<? extends EntityMosquitoSpit> type, World worldIn) {
        super(type, worldIn);
    }

    public EntityMosquitoSpit(World worldIn, EntityCrimsonMosquito entityCrimsonMosquito) {
        this(AMEntityRegistry.MOSQUITO_SPIT.get(), worldIn);
        this.setShooter(entityCrimsonMosquito);
        this.setPos(entityCrimsonMosquito.getX() - (double)(entityCrimsonMosquito.getWidth() + 1.0F) * 0.35D * (double) MathHelper.sin(entityCrimsonMosquito.bodyYaw * MathHelper.RADIANS_PER_DEGREE), entityCrimsonMosquito.getEyeY() + (double)0.2F, entityCrimsonMosquito.getZ() + (double)(entityCrimsonMosquito.getWidth() + 1.0F) * 0.35D * (double)MathHelper.cos(entityCrimsonMosquito.bodyYaw * MathHelper.RADIANS_PER_DEGREE));
    }

    public EntityMosquitoSpit(World worldIn, LivingEntity p_i47273_2_, boolean right) {
        this(AMEntityRegistry.MOSQUITO_SPIT.get(), worldIn);
        this.setShooter(p_i47273_2_);
        float rot = p_i47273_2_.headYaw + (right ? 60 : -60);
        this.setPos(p_i47273_2_.getX() - (double)(p_i47273_2_.getWidth()) * 0.5D * (double) MathHelper.sin(rot * MathHelper.RADIANS_PER_DEGREE), p_i47273_2_.getEyeY() - (double)0.2F, p_i47273_2_.getZ() + (double)(p_i47273_2_.getWidth()) * 0.5D * (double)MathHelper.cos(rot * MathHelper.RADIANS_PER_DEGREE));
    }

    //FIXME
//    @OnlyIn(Dist.CLIENT)
//    public EntityMosquitoSpit(World worldIn, double x, double y, double z, double p_i47274_8_, double p_i47274_10_, double p_i47274_12_) {
//        this(AMEntityRegistry.MOSQUITO_SPIT.get(), worldIn);
//        this.setPos(x, y, z);
//        this.setVelocity(p_i47274_8_, p_i47274_10_, p_i47274_12_);
//    }
//
//    public EntityMosquitoSpit(PlayMessages.SpawnEntity spawnEntity, World world) {
//        this(AMEntityRegistry.MOSQUITO_SPIT.get(), world);
//    }
//
//    @Override
//    public Packet<ClientGamePacketListener> getAddEntityPacket() {
//        return (Packet<ClientGamePacketListener>) NetworkHooks.getEntitySpawningPacket(this);
//    }

    @Override
    public void tick() {
        if (!this.leftOwner) {
            this.leftOwner = this.checkLeftOwner();
        }
        super.tick();
        var vector3d = this.getVelocity();
        HitResult raytraceresult = ProjectileUtil.getCollision(this, this::canHitEntity);
        if (raytraceresult != null && raytraceresult.getType() != HitResult.Type.MISS) {
            this.onImpact(raytraceresult);
        }

        double d0 = this.getX() + vector3d.x;
        double d1 = this.getY() + vector3d.y;
        double d2 = this.getZ() + vector3d.z;

        this.updateRotation();
        if (this.getWorld().getStatesInBox(this.getBoundingBox()).noneMatch(AbstractBlock.AbstractBlockState::isAir)) {
            this.remove(RemovalReason.DISCARDED);
        } else if (this.isInsideWaterOrBubbleColumn()) {
            this.remove(RemovalReason.DISCARDED);
        } else {
            this.setVelocity(vector3d.multiply(0.99F));
            if (!this.hasNoGravity()) {
                this.setVelocity(this.getVelocity().add(0.0D, -0.06F, 0.0D));
            }

            this.setPos(d0, d1, d2);
        }
    }

    protected void onEntityHit(EntityHitResult p_213868_1_) {
        Entity entity = this.getOwner();
        Entity hitEntity = p_213868_1_.getEntity();
        if (entity instanceof LivingEntity) {
            hitEntity.damage(getDamageSources().mobProjectile(this, (LivingEntity)entity), 4.0F);
        }
        if (hitEntity instanceof EntityCrimsonMosquito && !this.getWorld().isClient) {
            EntityCrimsonMosquito mosquito = ((EntityCrimsonMosquito)hitEntity);
            mosquito.setBloodLevel(mosquito.getBloodLevel() + 1);
        }
    }

    protected void onHitBlock(BlockHitResult p_230299_1_) {
        if (!this.getWorld().isClient) {
            this.remove(RemovalReason.DISCARDED);
        }
    }

    @Override
    protected void initDataTracker() {
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
            return ((ServerWorld)this.getWorld()).getEntity(this.ownerUUID);
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

    }

    /**
     * (abstract) Protected helper method to read subclass entity data from NBT.
     */
    @Override
    protected void readCustomDataFromNbt(NbtCompound compound) {
        if (compound.containsUuid("Owner")) {
            this.ownerUUID = compound.getUuid("Owner");
        }

        this.leftOwner = compound.getBoolean("LeftOwner");
    }

    private boolean checkLeftOwner() {
        var entity = this.getOwner();
        if (entity != null) {
            for (var entity1 : this.getWorld().getOtherEntities(this, this.getBoundingBox().stretch(this.getVelocity()).expand(1.0D), (p_234613_0_) -> !p_234613_0_.isSpectator() && p_234613_0_.canHit())) {
                if (entity1.getRootVehicle() == entity.getRootVehicle()) {
                    return false;
                }
            }
        }

        return true;
    }

    public void shoot(double x, double y, double z, float velocity, float inaccuracy) {
        var vector3d = (new Vec3d(x, y, z)).normalize().add(this.random.nextGaussian() * (double)0.0075F * (double)inaccuracy, this.random.nextGaussian() * (double)0.0075F * (double)inaccuracy, this.random.nextGaussian() * (double)0.0075F * (double)inaccuracy).multiply((double)velocity);
        this.setVelocity(vector3d);
        float f = MathHelper.sqrt((float) vector3d.horizontalLengthSquared());
        this.setYaw( (float)(MathHelper.atan2(vector3d.x, vector3d.z) * (double)MathHelper.DEGREES_PER_RADIAN));
        this.setPitch((float)(MathHelper.atan2(vector3d.y, (double)f) * (double)MathHelper.DEGREES_PER_RADIAN));
        this.prevYaw = this.getYaw();
        this.prevPitch = this.getPitch();
    }

    public void shootFromRotation(Entity entity, float x, float y, float z, float velocity, float inaccuracy) {
        float f = -MathHelper.sin(y * MathHelper.RADIANS_PER_DEGREE) * MathHelper.cos(x * MathHelper.RADIANS_PER_DEGREE);
        float f1 = -MathHelper.sin((x + z) * MathHelper.RADIANS_PER_DEGREE);
        float f2 = MathHelper.cos(y * MathHelper.RADIANS_PER_DEGREE) * MathHelper.cos(x * MathHelper.RADIANS_PER_DEGREE);
        this.shoot(f, f1, f2, velocity, inaccuracy);
        var vector3d = entity.getVelocity();
        this.setVelocity(this.getVelocity().add(vector3d.x, entity.isOnGround() ? 0.0D : vector3d.y, vector3d.z));
    }

    /**
     * Called when this EntityFireball hits a block or entity.
     */
    protected void onImpact(HitResult result) {
        HitResult.Type raytraceresult$type = result.getType();
        if (raytraceresult$type == HitResult.Type.ENTITY) {
            this.onEntityHit((EntityHitResult)result);
        } else if (raytraceresult$type == HitResult.Type.BLOCK) {
            this.onHitBlock((BlockHitResult)result);
        }

    }

    @Override
    public void setVelocityClient(double x, double y, double z) {
        this.setVelocity(x, y, z);
        if (this.prevPitch == 0.0F && this.prevYaw == 0.0F) {
            float f = MathHelper.sqrt((float) (x * x + z * z));
            this.setPitch((float)(MathHelper.atan2(y, (double)f) * (double)MathHelper.DEGREES_PER_RADIAN));
            this.setYaw( (float)(MathHelper.atan2(x, z) * (double)MathHelper.DEGREES_PER_RADIAN));
            this.prevPitch = this.getPitch();
            this.prevYaw = this.getYaw();
            this.refreshPositionAndAngles(this.getX(), this.getY(), this.getZ(), this.getYaw(), this.getPitch());
        }
    }

    protected boolean canHitEntity(Entity p_230298_1_) {
        if (!p_230298_1_.isSpectator() && p_230298_1_.isAlive() && p_230298_1_.canHit()) {
            Entity entity = this.getOwner();
            return entity == null || this.leftOwner || !entity.isConnectedThroughVehicle(p_230298_1_);
        } else {
            return false;
        }
    }

    protected void updateRotation() {
        var vector3d = this.getVelocity();
        float f = MathHelper.sqrt((float) vector3d.horizontalLengthSquared());
        this.setPitch(lerpRotation(this.prevPitch, (float)(MathHelper.atan2(vector3d.y, (double)f) * (double)MathHelper.DEGREES_PER_RADIAN)));
        this.setYaw(lerpRotation(this.prevYaw, (float)(MathHelper.atan2(vector3d.x, vector3d.z) * (double)MathHelper.DEGREES_PER_RADIAN)));
    }

    protected static float lerpRotation(float p_234614_0_, float p_234614_1_) {
        while(p_234614_1_ - p_234614_0_ < -180.0F) {
            p_234614_0_ -= 360.0F;
        }

        while(p_234614_1_ - p_234614_0_ >= 180.0F) {
            p_234614_0_ += 360.0F;
        }

        return MathHelper.lerp(0.2F, p_234614_0_, p_234614_1_);
    }
}
