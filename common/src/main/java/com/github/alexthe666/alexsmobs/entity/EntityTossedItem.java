package com.github.alexthe666.alexsmobs.entity;

import com.github.alexthe666.alexsmobs.registry.AMEntityRegistry;
import com.github.alexthe666.alexsmobs.registry.AMItemRegistry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ItemStackParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class EntityTossedItem extends ThrownItemEntity {

    protected static final TrackedData<Boolean> DART = DataTracker.registerData(EntityTossedItem.class, TrackedDataHandlerRegistry.BOOLEAN);

    public EntityTossedItem(EntityType<? extends EntityTossedItem> p_i50154_1_, World p_i50154_2_) {
        super(p_i50154_1_, p_i50154_2_);
    }

    public EntityTossedItem(World worldIn, LivingEntity throwerIn) {
        super(AMEntityRegistry.TOSSED_ITEM.get(), throwerIn, worldIn);
    }

    public EntityTossedItem(World worldIn, double x, double y, double z) {
        super(AMEntityRegistry.TOSSED_ITEM.get(), x, y, z, worldIn);
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(DART, false);
    }

    public boolean isDart() {
        return this.dataTracker.get(DART);
    }

    public void setDart(boolean dart) {
        this.dataTracker.set(DART, dart);
    }

    //FIXME
//    @Override
//    public Packet<ClientPlayPacketListener> createSpawnPacket() {
//        return (Packet<ClientPlayPacketListener>) NetworkHooks.getEntitySpawningPacket(this);
//    }
//
    @Environment(EnvType.CLIENT)
    public void handleStatus(byte id) {
        if (id == 3) {
            double d0 = 0.08D;

            for(int i = 0; i < 8; ++i) {
                this.getWorld().addParticle(new ItemStackParticleEffect(ParticleTypes.ITEM, this.getItem()), this.getX(), this.getY(), this.getZ(), ((double)this.random.nextFloat() - 0.5D) * 0.08D, ((double)this.random.nextFloat() - 0.5D) * 0.08D, ((double)this.random.nextFloat() - 0.5D) * 0.08D);
            }
        }
    }

    @Override
    public void setVelocityClient(double x, double y, double z) {
        this.setVelocity(x, y, z);
        if (this.prevPitch == 0.0F && this.prevYaw == 0.0F) {
            final float f = MathHelper.sqrt((float) (x * x + z * z));
            this.setPitch((float)(MathHelper.atan2(y, f) * (double)MathHelper.DEGREES_PER_RADIAN));
            this.setYaw( (float)(MathHelper.atan2(x, z) * (double)MathHelper.DEGREES_PER_RADIAN));
            this.prevPitch = this.getPitch();
            this.prevYaw = this.getYaw();
            this.refreshPositionAndAngles(this.getX(), this.getY(), this.getZ(), this.getYaw(), this.getPitch());
        }
    }

    @Override
    public void tick() {
        super.tick();
        Vec3d vector3d = this.getVelocity();
        float f = MathHelper.sqrt((float) vector3d.horizontalLengthSquared());
        this.setPitch(lerpRotation(this.prevPitch, (float)(MathHelper.atan2(vector3d.y, f) * (double)MathHelper.DEGREES_PER_RADIAN)));
        this.setYaw( lerpRotation(this.prevYaw, (float)(MathHelper.atan2(vector3d.x, vector3d.z) * (double)MathHelper.DEGREES_PER_RADIAN)));
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

    @Override
    protected void onEntityHit(EntityHitResult p_213868_1_) {
        super.onEntityHit(p_213868_1_);
        if(this.getOwner() instanceof EntityCapuchinMonkey boss){
            if(!boss.isTeammate(p_213868_1_.getEntity()) || !boss.isTamed() && !(p_213868_1_.getEntity() instanceof EntityCapuchinMonkey)){
                p_213868_1_.getEntity().damage(getDamageSources().thrown(this, boss), isDart() ? 8 : 4);
            }
        }
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound compound) {
        compound.putBoolean("Dart", this.isDart());
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound compound) {
        this.setDart(compound.getBoolean("Dart"));
    }

    @Override
    protected void onCollision(HitResult result) {
        super.onCollision(result);
        if (!this.getWorld().isClient && (!this.isDart() || result.getType() == HitResult.Type.BLOCK)) {
            this.getWorld().sendEntityStatus(this, (byte)3);
            this.remove(RemovalReason.DISCARDED);
        }
    }

    @Override
    protected Item getDefaultItem() {
        return isDart() ? AMItemRegistry.ANCIENT_DART.get() : Items.COBBLESTONE;
    }
}
