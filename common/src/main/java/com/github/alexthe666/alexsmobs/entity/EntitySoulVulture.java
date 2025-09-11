package com.github.alexthe666.alexsmobs.entity;

import com.github.alexthe666.alexsmobs.config.AMConfig;
import com.github.alexthe666.alexsmobs.entity.ai.DirectPathNavigator;
import com.github.alexthe666.alexsmobs.entity.ai.EntityAINearestTarget3D;
import com.github.alexthe666.alexsmobs.entity.ai.GroundPathNavigatorWide;
import com.github.alexthe666.alexsmobs.entity.util.Maths;
import com.github.alexthe666.alexsmobs.registry.AMEntityRegistry;
import com.github.alexthe666.alexsmobs.registry.AMSoundRegistry;
import com.github.alexthe666.alexsmobs.registry.AMTagRegistry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EntityGroup;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.Flutterer;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.control.MoveControl;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.AbstractPiglinEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.Optional;

public class EntitySoulVulture extends HostileEntity implements Flutterer {

    public static final Identifier SOUL_LOOT = new Identifier("alexsmobs", "entities/soul_vulture_heart");
    private static final TrackedData<Boolean> FLYING = DataTracker.registerData(EntitySoulVulture.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> TACKLING = DataTracker.registerData(EntitySoulVulture.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Optional<BlockPos>> PERCH_POS = DataTracker.registerData(EntitySoulVulture.class, TrackedDataHandlerRegistry.OPTIONAL_BLOCK_POS);
    private static final TrackedData<Integer> SOUL_LEVEL = DataTracker.registerData(EntitySoulVulture.class, TrackedDataHandlerRegistry.INTEGER);
    public float prevFlyProgress;
    public float flyProgress;
    public float prevTackleProgress;
    public float tackleProgress;
    private boolean isLandNavigator;
    private int perchSearchCooldown = 0;
    private int landingCooldown = 0;
    private int tackleCooldown = 0;

    public EntitySoulVulture(EntityType<? extends EntitySoulVulture> type, World worldIn) {
        super(type, worldIn);
        switchNavigator(true);
    }

    @Override
    public EntityGroup getGroup() {
        return EntityGroup.UNDEAD;
    }

    @Nullable
    protected Identifier getLootTableId() {
        return hasSoulHeart() ? SOUL_LOOT : super.getLootTableId();
    }

    @Override
    public boolean canSpawn(WorldAccess worldIn, SpawnReason spawnReasonIn) {
        return AMEntityRegistry.rollSpawn(AMConfig.soulVultureSpawnRolls, this.getRandom(), spawnReasonIn);
    }

    public static boolean canVultureSpawn(EntityType<? extends MobEntity> typeIn, ServerWorldAccess worldIn, SpawnReason reason, BlockPos pos, Random randomIn) {
        BlockPos blockpos = pos.down();
        boolean spawnBlock = worldIn.getBlockState(blockpos).isIn(AMTagRegistry.SOUL_VULTURE_SPAWNS);
        return reason == SpawnReason.SPAWNER || spawnBlock && canMobSpawn(AMEntityRegistry.SOUL_VULTURE.get(), worldIn, reason, pos, randomIn);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return AMSoundRegistry.SOUL_VULTURE_IDLE.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return AMSoundRegistry.SOUL_VULTURE_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return AMSoundRegistry.SOUL_VULTURE_HURT.get();
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createLivingAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 12.0D)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 18.0D)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 4.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.25F);
    }

    public boolean isPerchBlock(BlockPos pos, BlockState state) {
        return getWorld().isAir(pos.up()) && getWorld().isAir(pos.up(2)) && state.isIn(AMTagRegistry.SOUL_VULTURE_PERCHES);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(1, new AICirclePerch(this));
        this.goalSelector.add(2, new AIFlyRandom(this));
        this.goalSelector.add(3, new AITackleMelee(this));
        this.goalSelector.add(4, new LookAtEntityGoal(this, PlayerEntity.class, 20F));
        this.goalSelector.add(5, new LookAroundGoal(this));
        this.targetSelector.add(1, new RevengeGoal(this, EntitySoulVulture.class));
        this.targetSelector.add(2, new EntityAINearestTarget3D<>(this, PlayerEntity.class, true));
        this.targetSelector.add(2, new EntityAINearestTarget3D<>(this, AbstractPiglinEntity.class, true));
        this.targetSelector.add(3, new EntityAINearestTarget3D<>(this, MerchantEntity.class, true));
    }

    @Override
    public int getLimitPerChunk() {
        return 1;
    }

    @Override
    public boolean spawnsTooManyForEachTry(int sizeIn) {
        return true;
    }

    private void switchNavigator(boolean onLand) {
        if (onLand) {
            this.moveControl = new MoveControl(this);
            this.navigation = new GroundPathNavigatorWide(this, getWorld());
            this.isLandNavigator = true;
        } else {
            this.moveControl = new MoveHelper(this);
            this.navigation = new DirectPathNavigator(this, getWorld());
            this.isLandNavigator = false;
        }
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(FLYING, false);
        this.dataTracker.startTracking(TACKLING, false);
        this.dataTracker.startTracking(PERCH_POS, Optional.empty());
        this.dataTracker.startTracking(SOUL_LEVEL, 0);
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound compound) {
        super.writeCustomDataToNbt(compound);
        compound.putBoolean("Flying", this.isInAir());
        if(this.getPerchPos() != null){
            compound.putInt("PerchX", this.getPerchPos().getX());
            compound.putInt("PerchY", this.getPerchPos().getY());
            compound.putInt("PerchZ", this.getPerchPos().getZ());
        }
        compound.putInt("SoulLevel", this.getSoulLevel());
        compound.putInt("LandingCooldown", landingCooldown);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound compound) {
        super.readCustomDataFromNbt(compound);
        this.setFlying(compound.getBoolean("Flying"));
        this.setSoulLevel(compound.getInt("SoulLevel"));
        this.landingCooldown = compound.getInt("LandingCooldown");
        if(compound.contains("PerchX") && compound.contains("PerchY") && compound.contains("PerchZ")){
            this.setPerchPos(new BlockPos(compound.getInt("PerchX"), compound.getInt("PerchY"), compound.getInt("PerchZ")));
        }
    }

    @Override
    public boolean isInAir() {
        return this.dataTracker.get(FLYING);
    }

    public void setFlying(boolean flying) {
        this.dataTracker.set(FLYING, flying);
    }

    public boolean isTackling() {
        return this.dataTracker.get(TACKLING);
    }

    public void setTackling(boolean tackling) {
        this.dataTracker.set(TACKLING, tackling);
    }

    public BlockPos getPerchPos() {
        return this.dataTracker.get(PERCH_POS).orElse(null);
    }

    public void setPerchPos(BlockPos pos) {
        this.dataTracker.set(PERCH_POS, Optional.ofNullable(pos));
    }

    public int getSoulLevel() {
        return this.dataTracker.get(SOUL_LEVEL);
    }

    public void setSoulLevel(int tackling) {
        this.dataTracker.set(SOUL_LEVEL, tackling);
    }

    @Override
    public void tick() {
        super.tick();
        this.prevTackleProgress = tackleProgress;
        this.prevFlyProgress = flyProgress;
        if (!this.getWorld().isClient) {
            if(perchSearchCooldown > 0){
                perchSearchCooldown--;
            }
            if(this.getTarget() != null && this.getTarget().isAlive()){
                this.setPerchPos(this.getTarget().getBlockPos().up(7));
            }else{
                if (this.getPerchPos() != null && !isPerchBlock(this.getPerchPos(), getWorld().getBlockState(this.getPerchPos()))) {
                    this.setPerchPos(null);
                }
            }
            if (this.getPerchPos() == null && perchSearchCooldown == 0) {
                perchSearchCooldown = 20 + random.nextInt(20);
                this.setPerchPos(this.findNewPerchPos());
            }
            if (!isInAir() && landingCooldown == 0 && (this.getPerchPos() == null || this.shouldLeavePerch(this.getPerchPos()))) {
                this.setFlying(true);
            }
            if (!isInAir() && this.getTarget() != null){
                this.setFlying(true);
            }

            if(landingCooldown > 0 && isInAir() && this.isOnGround() && this.getTarget() == null){
                this.setFlying(false);
            }
        }
        final boolean flying = isInAir();
        if (flying) {
            if (this.isLandNavigator)
                switchNavigator(false);

            if (flyProgress < 5F)
                flyProgress++;
        } else {
            if (!this.isLandNavigator)
                switchNavigator(true);

            if (flyProgress > 0F)
                flyProgress--;
        }

        if (this.isTackling()) {
            if (tackleProgress < 5F)
                tackleProgress++;
        } else {
            if (tackleProgress > 0F)
                tackleProgress--;
        }

        if(landingCooldown > 0){
            landingCooldown--;
        }
        if(tackleCooldown > 0){
            tackleCooldown--;
        }
        if (isInAir()) {
            this.setNoGravity(true);
        } else {
            this.setNoGravity(false);
        }
        if (this.getWorld().isClient  && hasSoulHeart()) {
            final float radius = 0.25F + random.nextFloat() * 1F;
            final float fly = this.flyProgress * 0.2F;
            final float wingSpread = 15F + 65 * fly + random.nextInt(5);
            final float angle = (Maths.STARTING_ANGLE * ((random.nextBoolean() ? -1 : 1) * (wingSpread + 180) + this.bodyYaw));
            final float angleMotion = (Maths.STARTING_ANGLE * this.bodyYaw);
            final double extraX = radius * MathHelper.sin(MathHelper.PI + angle);
            final double extraZ = radius * MathHelper.cos(angle);
            final double mov = this.getVelocity().length();
            final double extraXMotion = -mov * MathHelper.sin((float) (Math.PI + angleMotion));
            final double extraZMotion = -mov * MathHelper.cos(angleMotion);
            final double yRandom = 0.2F + random.nextFloat() * 0.3F;
            this.getWorld().addParticle(ParticleTypes.SOUL_FIRE_FLAME, this.getX() + extraX, this.getY() + yRandom, this.getZ() + extraZ, extraXMotion, random.nextFloat() * 0.1F, extraZMotion);
        }
    }

    @Environment(EnvType.CLIENT)
    public void handleStatus(byte id) {
        if(id == 68){
            for (int i = 0; i < 6 + random.nextInt(3); i++) {
                final double d2 = this.random.nextGaussian() * 0.02D;
                final double d0 = this.random.nextGaussian() * 0.02D;
                final double d1 = this.random.nextGaussian() * 0.02D;
                this.getWorld().addParticle(ParticleTypes.SOUL, this.getX() + (double) (this.random.nextFloat() * this.getWidth()) - (double) this.getWidth() * 0.5F, this.getY() + this.getHeight() * 0.5F + (double) (this.random.nextFloat() * this.getHeight() * 0.5F), this.getZ() + (double) (this.random.nextFloat() * this.getWidth()) - (double) this.getWidth() * 0.5F, d0, d1, d2);
            }
        }else{
            super.handleStatus(id);
        }
    }

    public BlockPos findNewPerchPos() {
        BlockState beneathState = getWorld().getBlockState(this.getVelocityAffectingPos());
        if(isPerchBlock(this.getVelocityAffectingPos(), beneathState)){
            return this.getVelocityAffectingPos();
        }
        BlockPos blockpos = null;
        var random = Random.create();
        int range = 14;
        for (int i = 0; i < 15; i++) {
            var blockpos1 = this.getBlockPos().add(random.nextInt(range) - range / 2, 3, random.nextInt(range) - range / 2);
            while (this.getWorld().isAir(blockpos1) && blockpos1.getY() > 1) {
                blockpos1 = blockpos1.down();
            }
            if (isPerchBlock(blockpos1, getWorld().getBlockState(blockpos1))) {
                blockpos = blockpos1;
            }
        }
        return blockpos;
    }

    private boolean shouldLeavePerch(BlockPos perchPos) {
        return this.squaredDistanceTo(Vec3d.ofCenter(perchPos)) > 13 || landingCooldown == 0;
    }

    @Override
    protected void fall(double y, boolean onGroundIn, BlockState state, BlockPos pos) {
    }

    public boolean shouldSwoop(){
        return this.getTarget() != null && this.tackleCooldown == 0;
    }

    public boolean hasSoulHeart() {
        return getSoulLevel() > 2;
    }

    class AICirclePerch extends Goal {
        private final EntitySoulVulture vulture;
        float speed = 1;
        float circlingTime = 0;
        float circleDistance = 5;
        float maxCirclingTime = 80;
        boolean clockwise = false;
        private BlockPos targetPos;
        private int yLevel = 1;

        public AICirclePerch(EntitySoulVulture vulture) {
            this.vulture = vulture;
            this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
        }

        @Override
        public boolean canStart() {
            return !vulture.shouldSwoop() && this.vulture.isInAir() && this.vulture.getPerchPos() != null;
        }

        @Override
        public void start() {
            circlingTime = 0;
            speed = 0.8F + random.nextFloat() * 0.4F;
            yLevel = vulture.random.nextInt(3);
            maxCirclingTime = 360 + this.vulture.random.nextInt(80);
            circleDistance = 5 + this.vulture.random.nextFloat() * 5;
            clockwise = this.vulture.random.nextBoolean();
        }

        @Override
        public void stop() {
            circlingTime = 0;
            speed = 0.8F + random.nextFloat() * 0.4F;
            yLevel = vulture.random.nextInt(3);
            maxCirclingTime = 360 + this.vulture.random.nextInt(80);
            circleDistance = 5 + this.vulture.random.nextFloat() * 5;
            clockwise = this.vulture.random.nextBoolean();
            this.vulture.tackleCooldown = 0;
        }

        @Override
        public void tick() {
            BlockPos encircle = vulture.getPerchPos();
            double localSpeed = speed;
            if(this.vulture.getTarget() != null){
                localSpeed *= 1.55D;
            }
            if (encircle != null) {
                circlingTime++;
                if(circlingTime > 360){
                    vulture.getMoveControl().moveTo(encircle.getX() + 0.5D, encircle.getY() + 1.1D, encircle.getZ() + 0.5D, localSpeed);
                    if(vulture.verticalCollision || this.vulture.squaredDistanceTo(encircle.getX() + 0.5D, encircle.getY() + 1.1D, encircle.getZ() + 0.5D) < 1D){
                        vulture.setFlying(false);
                        vulture.setVelocity(Vec3d.ZERO);
                        vulture.landingCooldown = 400 + random.nextInt(1200);
                        stop();
                    }
                }else{
                    BlockPos circlePos = getVultureCirclePos(encircle);
                    if (circlePos != null) {
                        vulture.getMoveControl().moveTo(circlePos.getX() + 0.5D, circlePos.getY() + 0.5D, circlePos.getZ() + 0.5D, localSpeed);
                    }
                }
            }
        }

        @Override
        public boolean shouldContinue() {
            return this.canStart();
        }

        public BlockPos getVultureCirclePos(BlockPos target) {
            final float angle = (Maths.THREE_STARTING_ANGLE * (clockwise ? -circlingTime : circlingTime));
            final double extraX = circleDistance * MathHelper.sin((angle));
            final double extraZ = circleDistance * MathHelper.cos(angle);
            BlockPos pos = new BlockPos((int) (target.getX() + extraX), target.getY() + 1 + yLevel, (int) (target.getZ() + extraZ));
            if (vulture.getWorld().isAir(pos)) {
                return pos;
            }
            return null;
        }
    }

    public boolean isTargetBlocked(Vec3d target) {
        var Vector3d = new Vec3d(this.getX(), this.getEyeY(), this.getZ());
        return this.getWorld().raycast(new RaycastContext(Vector3d, target, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, this)).getType() != HitResult.Type.MISS;
    }

    static class MoveHelper extends MoveControl {
        private final EntitySoulVulture parentEntity;

        public MoveHelper(EntitySoulVulture bird) {
            super(bird);
            this.parentEntity = bird;
        }

        @Override
        public void tick() {
            if (this.state == MoveControl.State.MOVE_TO) {
                var vector3d = new Vec3d(this.targetX - parentEntity.getX(), this.targetY - parentEntity.getY(), this.targetZ - parentEntity.getZ());
                final double d5 = vector3d.length();
                if (d5 < 0.3) {
                    this.state = MoveControl.State.WAIT;
                    parentEntity.setVelocity(parentEntity.getVelocity().multiply(0.5D));
                } else {
                    parentEntity.setVelocity(parentEntity.getVelocity().add(vector3d.multiply(this.speed * 0.05D / d5)));
                    var vector3d1 = parentEntity.getVelocity();
                    parentEntity.setYaw(-((float) MathHelper.atan2(vector3d1.x, vector3d1.z)) * MathHelper.DEGREES_PER_RADIAN);
                    parentEntity.bodyYaw = parentEntity.getYaw();

                }

            }
        }

        private boolean canReach(Vec3d p_220673_1_, int p_220673_2_) {
            var axisalignedbb = this.parentEntity.getBoundingBox();

            for (int i = 1; i < p_220673_2_; ++i) {
                axisalignedbb = axisalignedbb.offset(p_220673_1_);
                if (!this.parentEntity.getWorld().isSpaceEmpty(this.parentEntity, axisalignedbb)) {
                    return false;
                }
            }

            return true;
        }
    }

    private class AIFlyRandom extends Goal {

        private final EntitySoulVulture vulture;
        private BlockPos target = null;

        public AIFlyRandom(EntitySoulVulture vulture) {
            this.vulture = vulture;
            this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
        }

        @Override
        public boolean canStart() {
            if(vulture.getPerchPos() != null || vulture.shouldSwoop()){
                return false;
            }
            var movementcontroller = this.vulture.getMoveControl();
            if(!movementcontroller.isMoving() || target == null){
                target = getBlockInViewVulture();
                if(target != null){
                    this.vulture.getMoveControl().moveTo(target.getX() + 0.5D, target.getY() + 0.5D, target.getZ() + 0.5D, 1.0D);
                }
                return true;
            }
            return false;
        }

        @Override
        public boolean shouldContinue() {
            if(vulture.getPerchPos() != null || vulture.shouldSwoop()){
                return false;
            }
            return target != null && vulture.squaredDistanceTo(Vec3d.ofCenter(target)) > 2.4D && vulture.getMoveControl().isMoving() && !vulture.horizontalCollision;
        }

        @Override
        public void stop(){
            target = null;
        }

        @Override
        public void tick() {
            if (target == null) {
                target = getBlockInViewVulture();
            }
            if (target != null) {
                this.vulture.getMoveControl().moveTo(target.getX() + 0.5D, target.getY() + 0.5D, target.getZ() + 0.5D, 1.0D);
                if(vulture.squaredDistanceTo(Vec3d.ofCenter(target)) < 2.5F){
                    target = null;
                }
            }
        }

        public BlockPos getBlockInViewVulture() {
            final float radius = 0.75F * (0.7F * 6) * -3 - vulture.getRandom().nextInt(10);
            final float neg = vulture.getRandom().nextBoolean() ? 1 : -1;
            final float renderYawOffset = vulture.bodyYaw;
            final float angle = (Maths.STARTING_ANGLE * renderYawOffset) + 3.15F + (vulture.getRandom().nextFloat() * neg);
            final double extraX = radius * MathHelper.sin(MathHelper.PI + angle);
            final double extraZ = radius * MathHelper.cos(angle);
            BlockPos radialPos = new BlockPos((int) (vulture.getX() + extraX), (int) vulture.getY(), (int) (vulture.getZ() + extraZ));
            while(getWorld().isAir(radialPos) && radialPos.getY() > 2){
                radialPos = radialPos.down();
            }
            BlockPos newPos = radialPos.up(vulture.getY() - radialPos.getY() > 16 ? 4 : vulture.getRandom().nextInt(5) + 5);
            if (!vulture.isTargetBlocked(Vec3d.ofCenter(newPos)) && vulture.squaredDistanceTo(Vec3d.ofCenter(newPos)) > 6) {
                return newPos;
            }
            return null;
        }
    }

    private class AITackleMelee extends Goal {

        private final EntitySoulVulture vulture;

        public AITackleMelee(EntitySoulVulture vulture) {
            this.vulture = vulture;
            this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
        }

        @Override
        public boolean canStart() {
            if(vulture.getTarget() != null && vulture.shouldSwoop()) {
                vulture.setFlying(true);
                return true;
            }
            return false;
        }


        @Override
        public void stop(){
            vulture.setTackling(false);
        }

        @Override
        public void tick() {
            vulture.setTackling(vulture.isInAir());
            if (vulture.getTarget() != null) {
                this.vulture.getMoveControl().moveTo(vulture.getTarget().getX(), vulture.getTarget().getY() + vulture.getTarget().getStandingEyeHeight(), vulture.getTarget().getZ(), 2.0D);
                final double d0 = this.vulture.getX() - this.vulture.getTarget().getX();
                final double d2 = this.vulture.getZ() - this.vulture.getTarget().getZ();
                final float f = (float)(MathHelper.atan2(d2, d0) * (double)MathHelper.DEGREES_PER_RADIAN) - 90.0F;
                vulture.setYaw(f);
                vulture.bodyYaw = vulture.getYaw();
                if (vulture.getBoundingBox().expand(0.3F, 0.3F, 0.3F).intersects(vulture.getTarget().getBoundingBox()) && vulture.tackleCooldown == 0) {
                    tackleCooldown = 100 + random.nextInt(200);
                    float dmg = (float) vulture.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE).getValue();
                    if(vulture.getTarget().damage(vulture.getDamageSources().mobAttack(vulture), dmg)){
                        if(vulture.getHealth() < vulture.getMaxHealth() - dmg && vulture.getSoulLevel() < 5){
                            this.vulture.setSoulLevel(vulture.getSoulLevel() + 1);
                            this.vulture.heal(dmg);
                            this.vulture.getWorld().sendEntityStatus(vulture, (byte)68);
                        }
                    }
                    stop();
                }
            }
        }
    }
}
