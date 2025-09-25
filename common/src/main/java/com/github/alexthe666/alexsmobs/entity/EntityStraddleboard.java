package com.github.alexthe666.alexsmobs.entity;


import com.github.alexthe666.alexsmobs.registry.AMEnchantmentRegistry;
import com.github.alexthe666.alexsmobs.registry.AMEntityRegistry;
import com.github.alexthe666.alexsmobs.registry.AMItemRegistry;
import dev.architectury.networking.NetworkManager;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.*;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockLocating;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class EntityStraddleboard extends Entity implements JumpingMount {

    private static final TrackedData<ItemStack> ITEMSTACK = DataTracker.registerData(EntityStraddleboard.class, TrackedDataHandlerRegistry.ITEM_STACK);
    private static final TrackedData<Integer> TIME_SINCE_HIT = DataTracker.registerData(EntityStraddleboard.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> COLOR = DataTracker.registerData(EntityStraddleboard.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Boolean> DEFAULT_COLOR = DataTracker.registerData(EntityStraddleboard.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Float> BOARD_ROT = DataTracker.registerData(EntityStraddleboard.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Boolean> REMOVE_SOON = DataTracker.registerData(EntityStraddleboard.class, TrackedDataHandlerRegistry.BOOLEAN);
    public float prevBoardRot = 0;
    private boolean rocking;
    private float rockingIntensity;
    private float rockingAngle;
    private float prevRockingAngle;
    private int extinguishTimer = 0;

    private int jumpFor = 0;
    private int lSteps;
    private double lx;
    private double ly;
    private double lz;
    private double lyr;
    private double lxr;
    private double lxd;
    private double lyd;
    private double lzd;

    private int rideForTicks = 0;

    private float boardForwards = 0.0F;
    private int removeIn;
    private PlayerEntity returnToPlayer = null;

    public EntityStraddleboard(EntityType<? extends EntityStraddleboard> p_i48580_1_, World p_i48580_2_) {
        super(p_i48580_1_, p_i48580_2_);
        this.intersectionChecked = true;
    }

    public EntityStraddleboard(World worldIn, double x, double y, double z) {
        this(AMEntityRegistry.STRADDLEBOARD.get(), worldIn);
        this.setPos(x, y, z);
        this.setVelocity(Vec3d.ZERO);
        this.prevX = x;
        this.prevY = y;
        this.prevZ = z;
    }

    public static boolean canVehicleCollide(Entity p_242378_0_, Entity entity) {
        return (entity.isCollidable() || entity.isPushable()) && !p_242378_0_.isConnectedThroughVehicle(entity);
    }

    @Override
    protected float getEyeHeight(EntityPose poseIn, EntityDimensions sizeIn) {
        return sizeIn.height;
    }

    @Override
    protected void initDataTracker() {
        this.dataTracker.startTracking(TIME_SINCE_HIT, 0);
        this.dataTracker.startTracking(ITEMSTACK, new ItemStack(AMItemRegistry.STRADDLEBOARD.get()));
        this.dataTracker.startTracking(DEFAULT_COLOR, true);
        this.dataTracker.startTracking(COLOR, 0);
        this.dataTracker.startTracking(BOARD_ROT, 0F);
        this.dataTracker.startTracking(REMOVE_SOON, false);
    }

    //FIXME forge
//    public boolean shouldRiderSit() {
//        return false;
//    }

    @Override
    public boolean collidesWith(Entity entity) {
        return canVehicleCollide(this, entity);
    }

    @Override
    protected Vec3d positionInPortal(Direction.Axis axis, BlockLocating.Rectangle result) {
        return LivingEntity.positionInPortal(super.positionInPortal(axis, result));
    }

    @Override
    public double getMountedHeightOffset() {
        return 0.5D;
    }

    public float getBoardRot(){
        return this.dataTracker.get(BOARD_ROT);
    }

    public void setBoardRot(float f){
        this.dataTracker.set(BOARD_ROT, f);
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        if (this.isInvulnerableTo(source)) {
            return false;
        } else if (!this.getWorld().isClient && !this.isRemoved()) {
            this.dataTracker.set(REMOVE_SOON, true);
            return true;
        } else {
            return true;
        }
    }

    private ItemStack getItemBoard() {
        return this.getItemStack();
    }

    @Override
    public void pushAwayFrom(Entity entityIn) {
        if (entityIn instanceof EntityStraddleboard) {
            if (entityIn.getBoundingBox().minY < this.getBoundingBox().maxY) {
                super.pushAwayFrom(entityIn);
            }
        } else if (entityIn.getBoundingBox().minY <= this.getBoundingBox().minY) {
            super.pushAwayFrom(entityIn);
        }

    }

    public boolean isRemoveLogic() {
        return this.dataTracker.get(REMOVE_SOON) || this.isRemoved();
    }

    @Override
    public boolean isCollidable() {
        return !this.isRemoveLogic();
    }

    @Override
    public boolean isPushable() {
        return !this.isRemoveLogic();
    }

    @Override
    public boolean canHit() {
        return !this.isRemoveLogic();
    }

    @Override
    public boolean shouldSave() {
        return !this.isRemoveLogic();
    }

    @Override
    public boolean isAttackable() {
        return !this.isRemoveLogic();
    }

    public boolean isDefaultColor() {
        return this.dataTracker.get(DEFAULT_COLOR);
    }

    public void setDefaultColor(boolean bar) {
        this.dataTracker.set(DEFAULT_COLOR, Boolean.valueOf(bar));
    }

    public int getColor() {
        if (isDefaultColor()) {
            return 0XADC3D7;
        }
        return this.dataTracker.get(COLOR);
    }

    public void setColor(int index) {
        this.dataTracker.set(COLOR, index);
    }

    @Override
    public void tick() {
        super.tick();
        float boardRot = this.getBoardRot();
        if(jumpFor > 0){
            jumpFor--;
        }
        if (this.getTimeSinceHit() > 0) {
            this.setTimeSinceHit(this.getTimeSinceHit() - 1);
        }
        if (extinguishTimer > 0) {
            extinguishTimer--;
        }
        if (this.dataTracker.get(REMOVE_SOON)) {
            this.removeIn--;
            this.setBoardRot((float) Math.sin(this.removeIn * 0.3F * Math.PI) * 50F);
            if (this.removeIn <= 0 && !this.getWorld().isClient) {
                this.removeIn = 0;
                boolean drop;
                if(this.getEnchant(AMEnchantmentRegistry.STRADDLE_BOARDRETURN.get()) > 0){
                    drop = returnToPlayer != null && !returnToPlayer.giveItemStack(this.getItemBoard());
                }else{
                    drop = true;
                }
                if(drop){
                    this.dropStack(this.getItemStack().copy());
                }
                this.discard();
            }
        }
        var player = getControllingPlayer();
        if (this.getWorld().isClient) {
            if (this.lSteps > 0) {
                double d5 = this.getX() + (this.lx - this.getX()) / (double) this.lSteps;
                double d6 = this.getY() + (this.ly - this.getY())  / (double) this.lSteps;
                double d7 = this.getZ() + (this.lz - this.getZ()) / (double) this.lSteps;
                this.setYaw(MathHelper.wrapDegrees((float) this.lyr));
                this.setPitch(this.getPitch() + (float) (this.lxr - (double) this.getPitch()) / (float) this.lSteps);
                --this.lSteps;
                this.setPos(d5, d6, d7);
                this.setRotation(this.getYaw(), this.getPitch());
            } else {
                this.refreshPosition();
                this.setRotation(this.getYaw(), this.getPitch());
            }
        } else {
            this.checkBlockCollision();
            float slowdown = this.isInsideWaterOrBubbleColumn() || isOnGround() ? 0.05F : 0.98F;
            tickMovement();
            this.move(MovementType.SELF, this.getVelocity());
            this.setVelocity(this.getVelocity().multiply(slowdown, slowdown, slowdown));
            float f2 = (float) -((float) this.getVelocity().y * 0.5F * (double) MathHelper.DEGREES_PER_RADIAN);
            this.setPitch(MathHelper.stepUnwrappedAngleTowards(this.getPitch(), f2, 5));

            if (player != null) {
                returnToPlayer = player;
                rideForTicks++;
                if (this.age % 50 == 0) {
                    if (getEnchant(AMEnchantmentRegistry.STRADDLE_LAVAWAX.get()) > 0) {
                        player.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 100, 0, true, false));
                    }
                }
                if (player.getFireTicks() > 0 && extinguishTimer == 0) {
                    player.extinguish();
                }
                this.setYaw(MathHelper.stepUnwrappedAngleTowards(this.getYaw(), player.getYaw(), 6));
                var deltaMovement = this.getVelocity();
                if (deltaMovement.y > -0.5D) {
                    this.fallDistance = 1.0F;
                }

                float slow = player.forwardSpeed < 0 ? 0 : player.forwardSpeed * 0.115F;

                float threshold = 3F;
                boolean flag = false;
                float boardRot1 = boardRot;
                if (this.prevYaw - this.getYaw() > threshold) {
                    boardRot1 += 10;
                    flag = true;
                }
                if (this.prevYaw - this.getYaw() < -threshold) {
                    boardRot1 -= 10;
                    flag = true;
                }
                if (!flag) {
                    if (boardRot1 > 0) {
                        boardRot1 = Math.max(boardRot1 - 5, 0);
                    }
                    if (boardRot1 < 0) {
                        boardRot1 = Math.min(boardRot1 + 5, 0);
                    }
                }

                this.setBoardRot(MathHelper.stepUnwrappedAngleTowards(boardRot, MathHelper.clamp(boardRot1, -25, 25), 5));

                boardForwards = slow;

                if(player.isSneaking() || !this.isAlive() || this.dataTracker.get(REMOVE_SOON)){
                    this.removeAllPassengers();
                }
                if (player.isInsideWall()) {
                    this.removeAllPassengers();
                    this.damage(getDamageSources().generic(), 100);
                }
            }else{
                rideForTicks = 0;
            }
        }
        prevBoardRot = boardRot;
    }

    private void tickMovement() {
        this.velocityDirty = true;
        float moveForwards = Math.min(boardForwards, 1.0F);
        float yRot = this.getYaw();
        var prev = this.getVelocity();
        float gravity = isOnLava() ? 0.0F : isInLava() ? 0.1F : -1;
        float f1 = -MathHelper.sin(yRot * ((float) Math.PI / 180F));
        float f2 = MathHelper.cos(yRot * ((float) Math.PI / 180F));
        var moveVec = new Vec3d(f1, 0, f2).multiply(moveForwards);
        var vec31 = prev.multiply(0.975F).add(moveVec);
        float jumpGravity = gravity;
        if(jumpFor > 0){
            float jumpRunsOutIn = jumpFor < 5 ? jumpFor / 5F : 1F;
            jumpGravity += jumpRunsOutIn + jumpRunsOutIn * 1F;
        }
        this.setVelocity(vec31.x, jumpGravity, vec31.z);
    }

    private boolean isOnLava() {
        var ourPos = BlockPos.ofFloored(this.getX(), this.getY() + 0.4F, this.getZ());
        var underPos = this.getSteppingPos();
        return this.getWorld().getFluidState(underPos).isIn(FluidTags.LAVA) && !this.getWorld().getFluidState(ourPos).isIn(FluidTags.LAVA);
    }

    @Override
    public void updateTrackedPositionAndAngles(double x, double y, double z, float yr, float xr, int steps, boolean b) {
        this.lx = x;
        this.ly = y;
        this.lz = z;
        this.lyr = yr;
        this.lxr = xr;
        this.lSteps = steps;
        this.setVelocity(this.lxd, this.lyd, this.lzd);
    }

    @Override
    public void setVelocityClient(double lerpX, double lerpY, double lerpZ) {
        this.lxd = lerpX;
        this.lyd = lerpY;
        this.lzd = lerpZ;
        this.setVelocity(this.lxd, this.lyd, this.lzd);
    }

    @Override
    public double getEyeY() {
        return this.getY() + 0.3F;
    }

    @Nullable
    @Override
    public LivingEntity getControllingPassenger() {
        return getControllingPlayer();
    }

    @Override
    public boolean isLogicalSideForUpdatingMovement() {
        return false;
    }

    @Nullable
    public PlayerEntity getControllingPlayer(){
        for (Entity passenger : this.getPassengerList()) {
            if (passenger instanceof PlayerEntity playerEntity) {
                return playerEntity;
            }
        }
        return null;
    }

    @Override
    protected void addPassenger(Entity passenger) {
        super.addPassenger(passenger);
        if (this.isLogicalSideForUpdatingMovement() && this.lSteps > 0) {
            this.lSteps = 0;
            this.updatePositionAndAngles(this.lx, this.ly, this.lz, (float) this.lyr, (float) this.lxr);
        }
    }

    @Override
    public void onTrackedDataSet(TrackedData<?> data) {
        super.onTrackedDataSet(data);
        if (REMOVE_SOON.equals(data)) {
            this.removeIn = 5;
        }
    }

    @Override
    public ActionResult interact(PlayerEntity player, Hand hand) {
        if (player.shouldCancelInteraction()) {
            return ActionResult.PASS;
        } else {
            if (!this.getWorld().isClient) {
                return player.startRiding(this) ? ActionResult.CONSUME : ActionResult.PASS;
            } else {
                return ActionResult.SUCCESS;
            }
        }
    }

    @Override
    public Packet<ClientPlayPacketListener> createSpawnPacket() {
        return NetworkManager.createAddEntityPacket(this);
    }


    /**
     * Gets the time since the last hit.
     */
    public int getTimeSinceHit() {
        return this.dataTracker.get(TIME_SINCE_HIT);
    }

    /**
     * Sets the time to count down from since the last time entity was hit.
     */
    public void setTimeSinceHit(int timeSinceHit) {
        this.dataTracker.set(TIME_SINCE_HIT, timeSinceHit);
    }

    public float getRockingAngle(float partialTicks) {
        return MathHelper.lerp(partialTicks, this.prevRockingAngle, this.rockingAngle);
    }

    @Override
    protected Entity.MoveEffect getMoveEffect() {
        return MoveEffect.EVENTS;
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound compound) {
        this.setDefaultColor(compound.getBoolean("IsDefColor"));
        if (compound.contains("BoardStack")) {
            this.setItemStack(ItemStack.fromNbt(compound.getCompound("BoardStack")));
        }
        this.setColor(compound.getInt("Color"));
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound compound) {
        compound.putBoolean("IsDefColor", this.isDefaultColor());
        compound.putInt("Color", this.getColor());
        if (!this.getItemStack().isEmpty()) {
            NbtCompound stackTag = new NbtCompound();
            this.getItemStack().writeNbt(stackTag);
            compound.put("BoardStack", stackTag);
        }
    }

    @Override
    public void setJumpStrength(int i) {
    }

    @Override
    public boolean canJump() {
        return isOnLava();
    }

    @Override
    public void startJumping(int i) {
        this.velocityDirty = true;
        if(canJump()){
            float f = 0.075F + getEnchant(AMEnchantmentRegistry.STRADDLE_JUMP.get()) * 0.05F;
            jumpFor = 5 + (int)(i * f);
        }
    }

    private int getEnchant(Enchantment enchantment) {
        return EnchantmentHelper.getLevel(enchantment, this.getItemBoard());
    }

    public boolean shouldSerpentFriend() {
        return getEnchant(AMEnchantmentRegistry.STRADDLE_SERPENTFRIEND.get()) > 0;
    }

    @Override
    public Vec3d updatePassengerForDismount(LivingEntity entity) {
        return new Vec3d(this.getX(), this.getY() + 2F, this.getZ());
    }

    @Override
    public void stopJumping() {
    }

    public ItemStack getItemStack() {
        return this.dataTracker.get(ITEMSTACK);
    }

    public void setItemStack(ItemStack item) {
        this.dataTracker.set(ITEMSTACK, item);
    }
}
