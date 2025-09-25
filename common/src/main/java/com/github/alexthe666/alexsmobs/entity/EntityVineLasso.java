package com.github.alexthe666.alexsmobs.entity;

import com.github.alexthe666.alexsmobs.entity.util.VineLassoUtil;
import com.github.alexthe666.alexsmobs.registry.AMEntityRegistry;
import com.github.alexthe666.alexsmobs.registry.AMItemRegistry;
import dev.architectury.networking.NetworkManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.AbstractBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.ItemStack;
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

public class EntityVineLasso extends Entity {
    private UUID ownerUUID;
    private int ownerNetworkId;
    private boolean leftOwner;

    public EntityVineLasso(EntityType<EntityVineLasso> p_i50162_1_, World p_i50162_2_) {
        super(p_i50162_1_, p_i50162_2_);
    }

    public EntityVineLasso(World worldIn, LivingEntity entity) {
        this(AMEntityRegistry.VINE_LASSO.get(), worldIn);
        this.setShooter(entity);
        this.setPos(entity.getX(), entity.getEyeY() + (double)0.15F, entity.getZ());
    }

    protected static float lerpRotation(float p_234614_0_, float p_234614_1_) {
        while (p_234614_1_ - p_234614_0_ < -180.0F) {
            p_234614_0_ -= 360.0F;
        }

        while (p_234614_1_ - p_234614_0_ >= 180.0F) {
            p_234614_0_ += 360.0F;
        }

        return MathHelper.lerp(0.2F, p_234614_0_, p_234614_1_);
    }

    @Override
    public Packet<ClientPlayPacketListener> createSpawnPacket() {
        return NetworkManager.createAddEntityPacket(this);
    }

    @Override
    public void tick() {
        if (!leftOwner) {
            leftOwner = this.checkLeftOwner();
        }
        super.tick();
        Vec3d vector3d = this.getVelocity();
        HitResult raytraceresult = ProjectileUtil.getCollision(this, this::canHitEntity);
        if (raytraceresult != null && raytraceresult.getType() != HitResult.Type.MISS) {
            this.onImpact(raytraceresult);
        }

        this.updateRotation();
        if(this.getOwner() != null && this.distanceTo(this.getOwner()) > 15){
            this.removeAndAddToInventory();
        }
        if (this.getWorld().getStatesInBox(this.getBoundingBox()).noneMatch(AbstractBlock.AbstractBlockState::isAir) && !this.isTouchingWater() && !this.isInLava()) {
            this.removeAndAddToInventory();
        }else {
            final double d0 = this.getX() + vector3d.x;
            final double d1 = this.getY() + vector3d.y;
            final double d2 = this.getZ() + vector3d.z;
            this.setVelocity(vector3d.multiply(0.99F));
            if (!this.hasNoGravity()) {
                this.setVelocity(this.getVelocity().add(0.0D, -0.02F, 0.0D));
            }

            this.setPos(d0, d1, d2);
        }
    }

    protected void onEntityHit(EntityHitResult p_213868_1_) {
        Entity entity = this.getOwner();
        if (entity instanceof LivingEntity && p_213868_1_.getEntity() != getOwner() && p_213868_1_.getEntity() instanceof LivingEntity && !VineLassoUtil.hasLassoData((LivingEntity) p_213868_1_.getEntity())) {
            this.remove(RemovalReason.DISCARDED);
            VineLassoUtil.lassoTo((LivingEntity) entity, (LivingEntity)p_213868_1_.getEntity());
        }
    }

    private void removeAndAddToInventory(){
        Entity entity = this.getOwner();
        ItemStack item = new ItemStack(AMItemRegistry.VINE_LASSO.get());
        if(!this.isRemoved()){
            if (!(entity instanceof PlayerEntity) || !((PlayerEntity) entity).giveItemStack(item)) {
                this.dropStack(item);
            }
        }
        this.remove(RemovalReason.DISCARDED);
    }

    protected void onHitBlock(BlockHitResult p_230299_1_) {
        if (!this.getWorld().isClient) {
            this.removeAndAddToInventory();
        }
    }

    public void setShooter(@Nullable Entity entityIn) {
        if (entityIn != null) {
            ownerUUID = entityIn.getUuid();
            ownerNetworkId = entityIn.getId();
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
    protected void initDataTracker() {
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
        Entity entity = this.getOwner();
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
        Vec3d vector3d = (new Vec3d(x, y, z)).normalize().add(this.random.nextGaussian() * (double) 0.0075F * (double) inaccuracy, this.random.nextGaussian() * (double) 0.0075F * (double) inaccuracy, this.random.nextGaussian() * (double) 0.0075F * (double) inaccuracy).multiply(velocity);
        this.setVelocity(vector3d);
        final float f = MathHelper.sqrt((float)(vector3d.x * vector3d.x + vector3d.z * vector3d.z));
        this.setYaw( (float) (MathHelper.atan2(vector3d.x, vector3d.z) * (double) MathHelper.DEGREES_PER_RADIAN));
        this.setPitch((float) (MathHelper.atan2(vector3d.y, f) * (double) MathHelper.DEGREES_PER_RADIAN));
        this.prevYaw = this.getYaw();
        this.prevPitch = this.getPitch();
    }

    public void shootFromRotation(Entity p_234612_1_, float p_234612_2_, float p_234612_3_, float p_234612_4_, float p_234612_5_, float p_234612_6_) {
        final float f = -MathHelper.sin(p_234612_3_ * MathHelper.RADIANS_PER_DEGREE) * MathHelper.cos(p_234612_2_ * MathHelper.RADIANS_PER_DEGREE);
        final float f1 = -MathHelper.sin((p_234612_2_ + p_234612_4_) * MathHelper.RADIANS_PER_DEGREE);
        final float f2 = MathHelper.cos(p_234612_3_ * MathHelper.RADIANS_PER_DEGREE) * MathHelper.cos(p_234612_2_ * MathHelper.RADIANS_PER_DEGREE);
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

    @Environment(EnvType.CLIENT)
    public void setVelocityClient(double x, double y, double z) {
        this.setVelocity(x, y, z);
        if (this.prevPitch == 0.0F && this.prevYaw == 0.0F) {
            float f = MathHelper.sqrt((float)(x * x + z * z));
            this.setPitch((float) (MathHelper.atan2(y, f) * (double) MathHelper.DEGREES_PER_RADIAN));
            this.setYaw((float) (MathHelper.atan2(x, z) * (double) MathHelper.DEGREES_PER_RADIAN));
            this.prevPitch = this.getPitch();
            this.prevYaw = this.getYaw();
            this.refreshPositionAndAngles(this.getX(), this.getY(), this.getZ(), this.getYaw(), this.getPitch());
        }
    }

    protected boolean canHitEntity(Entity p_230298_1_) {
        if (!p_230298_1_.isSpectator() && p_230298_1_.isAlive() && p_230298_1_.canHit()) {
            Entity entity = this.getOwner();
            return entity == null || leftOwner || !entity.isConnectedThroughVehicle(p_230298_1_);
        } else {
            return false;
        }
    }

    protected void updateRotation() {
        Vec3d vector3d = this.getVelocity();
        float f = MathHelper.sqrt((float)(vector3d.x * vector3d.x + vector3d.z * vector3d.z));
        this.setPitch(lerpRotation(this.prevPitch, (float) (MathHelper.atan2(vector3d.y, f) * (double) MathHelper.DEGREES_PER_RADIAN)));
        this.setYaw(this.getYaw() + 20);
    }
}
