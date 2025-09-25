package com.github.alexthe666.alexsmobs.entity;

import com.github.alexthe666.alexsmobs.registry.AMEntityRegistry;
import dev.architectury.networking.NetworkManager;
import net.minecraft.block.AbstractBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

public class EntitySquidGrapple extends Entity {

    private static final TrackedData<Optional<UUID>> OWNER_UUID = DataTracker.registerData(EntitySquidGrapple.class, TrackedDataHandlerRegistry.OPTIONAL_UUID);
    private static final TrackedData<Direction> ATTACHED_FACE = DataTracker.registerData(EntitySquidGrapple.class, TrackedDataHandlerRegistry.FACING);
    private static final TrackedData<Boolean> WITHDRAWING = DataTracker.registerData(EntitySquidGrapple.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Optional<BlockPos>> ATTACHED_POS = DataTracker.registerData(EntitySquidGrapple.class, TrackedDataHandlerRegistry.OPTIONAL_BLOCK_POS);
    private int ticksWithdrawing = 0;

    public EntitySquidGrapple(EntityType<EntitySquidGrapple> type, World level) {
        super(type, level);
    }

    public EntitySquidGrapple(World worldIn, LivingEntity player, boolean rightHand) {
        this(AMEntityRegistry.SQUID_GRAPPLE.get(), worldIn);
        this.setOwnerId(player.getUuid());
        float rot = player.headYaw+ (rightHand ? 60 : -60);
        this.setPos(player.getX() - (double) (player.getWidth()) * 0.5D * (double) MathHelper.sin(rot * MathHelper.RADIANS_PER_DEGREE), player.getEyeY() - (double) 0.2F, player.getZ() + (double) (player.getWidth()) * 0.5D * (double) MathHelper.cos(rot * MathHelper.RADIANS_PER_DEGREE));
    }

    protected static float lerpRotation(float f2, float f3) {
        while (f3 - f2 < -180.0F) {
            f2 -= 360.0F;
        }

        while (f3 - f2 >= 180.0F) {
            f2 += 360.0F;
        }

        return MathHelper.lerp(0.2F, f2, f3);
    }

    public void shoot(double x, double y, double z, float velocity, float inaccuracy) {
        Vec3d vector3d = (new Vec3d(x, y, z)).normalize().add(this.random.nextGaussian() * (double) 0.0075F * (double) inaccuracy, this.random.nextGaussian() * (double) 0.0075F * (double) inaccuracy, this.random.nextGaussian() * (double) 0.0075F * (double) inaccuracy).multiply(velocity);
        this.setVelocity(vector3d);
        float f = MathHelper.sqrt((float) (vector3d.x * vector3d.x + vector3d.z * vector3d.z));
        this.setYaw(MathHelper.wrapDegrees((float) (MathHelper.atan2(vector3d.x, vector3d.z) * (double) MathHelper.DEGREES_PER_RADIAN) + 180));
        this.setPitch((float) (MathHelper.atan2(vector3d.y, f) * (double) MathHelper.DEGREES_PER_RADIAN));
        this.prevYaw = this.getYaw();
        this.prevPitch = this.getPitch();
    }

    public Direction getAttachmentFacing() {
        return this.dataTracker.get(ATTACHED_FACE);
    }

    public void setAttachmentFacing(Direction direction){
        this.dataTracker.set(ATTACHED_FACE, direction);
    }

    @Nullable
    public UUID getOwnerId() {
        return this.dataTracker.get(OWNER_UUID).orElse(null);
    }

    public void setOwnerId(@Nullable UUID uniqueId) {
        this.dataTracker.set(OWNER_UUID, Optional.ofNullable(uniqueId));
    }


    public BlockPos getStuckToPos() {
        return this.dataTracker.get(ATTACHED_POS).orElse(null);
    }

    public void setStuckToPos(BlockPos harvestedPos) {
        this.dataTracker.set(ATTACHED_POS, Optional.ofNullable(harvestedPos));
    }

    @Override
    protected void initDataTracker() {
        this.dataTracker.startTracking(OWNER_UUID, Optional.empty());
        this.dataTracker.startTracking(ATTACHED_FACE, Direction.DOWN);
        this.dataTracker.startTracking(ATTACHED_POS, Optional.empty());
        this.dataTracker.startTracking(WITHDRAWING, false);
    }

    public Entity getOwner() {
        UUID id = getOwnerId();
        if (id != null && !this.getWorld().isClient) {
            return ((ServerWorld) getWorld()).getEntity(id);
        }
        return getOwnerId() == null ? null : getWorld().getPlayerByUuid(getOwnerId());
    }

    public boolean isWithdrawing(){
        return this.dataTracker.get(WITHDRAWING);
    }

    public void setWithdrawing(boolean withdrawing){
        this.dataTracker.set(WITHDRAWING, withdrawing);
    }

    @Override
    public void tick() {
        this.prevPitch = this.getPitch();
        this.prevYaw = this.getYaw();
        Entity entity = this.getOwner();
        if(!this.getWorld().isClient){
            if(entity == null || !entity.isAlive()){
                this.discard();
            }else if (entity.isSneaking()) {
                this.setWithdrawing(true);
            }
        }
        if(this.isWithdrawing() && entity != null){
            super.tick();
            ticksWithdrawing++;
            this.setStuckToPos(null);
            var withDrawTo = entity.getEyePos().add(0, -0.2F, 0);
            if(withDrawTo.distanceTo(this.getPos()) > 1.2F && ticksWithdrawing < 200){
                var move = new Vec3d(withDrawTo.x - this.getX(), withDrawTo.y - this.getY(), withDrawTo.z - this.getZ());
                var vector3d = move.normalize().multiply(1.2D);
                this.setVelocity(vector3d.multiply(0.99));
                double d0 = this.getX() + vector3d.x;
                double d1 = this.getY() + vector3d.y;
                double d2 = this.getZ() + vector3d.z;
                float f = MathHelper.sqrt((float) (move.x * move.x + move.z * move.z));
                if(!this.getWorld().isClient){
                    this.setYaw(MathHelper.wrapDegrees((float) (-MathHelper.atan2(move.x, move.z) * (double) MathHelper.DEGREES_PER_RADIAN)) - 180);
                    this.setPitch((float) (MathHelper.atan2(move.y, f) * (double) MathHelper.DEGREES_PER_RADIAN));
                    this.prevYaw = this.getYaw();
                    this.prevPitch = this.getPitch();
                }
                this.setPos(d0, d1, d2);
            }else{
                this.discard();
            }
        }else if (this.getWorld().isClient || this.getWorld().isChunkLoaded(this.getBlockPos())) {
            if(this.getStuckToPos() == null){
                super.tick();
                var vector3d = this.getVelocity();
                var raytraceresult = ProjectileUtil.getCollision(this, newentity -> false);
                if (raytraceresult != null && raytraceresult.getType() != HitResult.Type.MISS) {
                    this.onImpact(raytraceresult);
                }
                this.checkBlockCollision();
                double d0 = this.getX() + vector3d.x;
                double d1 = this.getY() + vector3d.y;
                double d2 = this.getZ() + vector3d.z;
                this.updateRotation();
                this.setVelocity(vector3d.multiply(0.99));
                if (this.getWorld().getStatesInBox(this.getBoundingBox()).noneMatch(AbstractBlock.AbstractBlockState::isAir) && !this.isTouchingWater()) {
                    this.setVelocity(Vec3d.ZERO);

                } else {
                    this.setPos(d0, d1, d2);
                }
                if (!this.hasNoGravity()) {
                    this.setVelocity(this.getVelocity().add(0.0D, -0.1F, 0.0D));
                }
            }else{
                var state = this.getWorld().getBlockState(this.getStuckToPos());
                var vec3 = new Vec3d(this.getStuckToPos().getX() + 0.5F, this.getStuckToPos().getY() + 0.5F, this.getStuckToPos().getZ() + 0.5F);
                var offset = new Vec3d(this.getAttachmentFacing().getOffsetX() * 0.55F, this.getAttachmentFacing().getOffsetY() * 0.55F, this.getAttachmentFacing().getOffsetZ() * 0.55F);
                this.setPosition(vec3.add(offset));
                float targetX = this.getPitch();
                float targetY = this.getYaw();
                switch (this.getAttachmentFacing()) {
                    case UP -> targetX = 0;
                    case DOWN -> targetX = 180;
                    case NORTH -> {
                        targetX = -90;
                        targetY = 0;
                    }
                    case EAST -> {
                        targetX = -90;
                        targetY = 90;
                    }
                    case SOUTH -> {
                        targetX = -90;
                        targetY = 180;
                    }
                    case WEST -> {
                        targetX = -90;
                        targetY = -90;
                    }
                }
                this.setPitch(targetX);
                this.setYaw(targetY);
                if(entity != null && entity.distanceTo(this) > 2){
                    float entitySwing = 1.0F;
                    if(entity instanceof LivingEntity living){
                        float detract = living.sidewaysSpeed * living.sidewaysSpeed + living.upwardSpeed * living.upwardSpeed + living.forwardSpeed * living.forwardSpeed;
                        entitySwing -= Math.min(1.0F, Math.sqrt(detract) * 0.333F);
                    }
                    var move = new Vec3d(this.getX() - entity.getX(), this.getY() - (double)entity.getStandingEyeHeight() / 2.0D - entity.getY(), this.getZ() - entity.getZ());
                    entity.setVelocity(entity.getVelocity().add(move.normalize().multiply(0.2D * entitySwing)));
                    if(!entity.isOnGround()){
                       entity.fallDistance = 0.0F;
                    }
                }
                if(state.isAir()){
                    this.setWithdrawing(true);
                }
            }
        } else {
            discard();
        }

    }

    protected float rotlerp(float in, float target, float maxShift) {
        float f = MathHelper.wrapDegrees(target - in);
        if (f > maxShift) {
            f = maxShift;
        }

        if (f < -maxShift) {
            f = -maxShift;
        }

        float f1 = in + f;
        if (f1 < 0.0F) {
            f1 += 360.0F;
        } else if (f1 > 360.0F) {
            f1 -= 360.0F;
        }

        return f1;
    }

    private void updateRotation() {
    }

    protected void onImpact(HitResult result) {
        HitResult.Type raytraceresult$type = result.getType();
        if (!this.getWorld().isClient && raytraceresult$type == HitResult.Type.BLOCK && this.getStuckToPos() == null) {
            this.setVelocity(Vec3d.ZERO);
            this.setStuckToPos(((BlockHitResult)result).getBlockPos());
            this.setAttachmentFacing(((BlockHitResult)result).getSide());
        }

    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound compound) {
        if (this.getOwnerId() != null) {
            compound.putUuid("OwnerUUID", this.getOwnerId());
        }
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound compound) {
        if (compound.containsUuid("OwnerUUID")) {
            this.setOwnerId(compound.getUuid("OwnerUUID"));
        }
    }

    @Override
    public Packet<ClientPlayPacketListener> createSpawnPacket() {
        return NetworkManager.createAddEntityPacket(this);
    }
}
