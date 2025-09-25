package com.github.alexthe666.alexsmobs.entity;

import com.github.alexthe666.alexsmobs.block.EnderResidueBlock;
import com.github.alexthe666.alexsmobs.client.sound.SoundWormBoss;
import com.github.alexthe666.alexsmobs.config.AMConfig;
import com.github.alexthe666.alexsmobs.entity.ai.DirectPathNavigator;
import com.github.alexthe666.alexsmobs.entity.ai.EntityAINearestTarget3D;
import com.github.alexthe666.alexsmobs.entity.ai.FlightMoveController;
import com.github.alexthe666.alexsmobs.entity.util.Maths;
import com.github.alexthe666.alexsmobs.misc.AMBlockPos;
import com.github.alexthe666.alexsmobs.platform.PlatformEvent;
import com.github.alexthe666.alexsmobs.registry.*;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.*;
import net.minecraft.world.event.GameEvent;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Optional;
import java.util.UUID;

public class EntityVoidWorm extends HostileEntity {

    public static final Identifier SPLITTER_LOOT = new Identifier("alexsmobs", "entities/void_worm_splitter");
    private static final TrackedData<Optional<UUID>> CHILD_UUID = DataTracker.registerData(EntityVoidWorm.class, TrackedDataHandlerRegistry.OPTIONAL_UUID);
    private static final TrackedData<Optional<UUID>> SPLIT_FROM_UUID = DataTracker.registerData(EntityVoidWorm.class, TrackedDataHandlerRegistry.OPTIONAL_UUID);
    private static final TrackedData<Integer> SEGMENT_COUNT = DataTracker.registerData(EntityVoidWorm.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> JAW_TICKS = DataTracker.registerData(EntityVoidWorm.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Float> WORM_ANGLE = DataTracker.registerData(EntityVoidWorm.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> SPEEDMOD = DataTracker.registerData(EntityVoidWorm.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Boolean> SPLITTER = DataTracker.registerData(EntityVoidWorm.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Integer> PORTAL_TICKS = DataTracker.registerData(EntityVoidWorm.class, TrackedDataHandlerRegistry.INTEGER);
    private final ServerBossBar bossInfo = (ServerBossBar) (new ServerBossBar(this.getDisplayName(), BossBar.Color.BLUE, BossBar.Style.PROGRESS)).setDarkenSky(true);
    public float prevWormAngle;
    public float prevJawProgress;
    public float jawProgress;
    public Vec3d teleportPos = null;
    public EntityVoidPortal portalTarget = null;
    public boolean fullyThrough = true;
    public boolean updatePostSummon = false;
    private int makePortalCooldown = 0;
    private int stillTicks = 0;
    private int blockBreakCounter;
    private int makeIdlePortalCooldown = 200 + random.nextInt(800);

    public EntityVoidWorm(EntityType<EntityVoidWorm> type, World worldIn) {
        super(type, worldIn);
        this.experiencePoints = 10;
        this.moveControl = new FlightMoveController(this, 1F, false, true);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return AMSoundRegistry.VOID_WORM_IDLE.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return AMSoundRegistry.VOID_WORM_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return AMSoundRegistry.VOID_WORM_HURT.get();
    }

    @Override
    protected float getSoundVolume() {
        return isSilent() ? 0 : 5;
    }

    @Override
    public boolean canSpawn(WorldAccess worldIn, SpawnReason spawnReasonIn) {
        return AMEntityRegistry.rollSpawn(AMConfig.voidWormSpawnRolls, this.getRandom(), spawnReasonIn);
    }

    public static boolean canVoidWormSpawn(EntityType<?> animal, WorldAccess worldIn, SpawnReason reason, BlockPos pos, Random random) {
        return true;
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createLivingAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, AMConfig.voidWormMaxHealth)
                .add(EntityAttributes.GENERIC_ARMOR, 4.0D)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 256.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.3F)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 5);
    }

    @Nullable
    protected Identifier getLootTableId() {
        return this.isSplitter() ? SPLITTER_LOOT : super.getLootTableId();
    }

    @Override
    public void kill() {
        this.remove(RemovalReason.DISCARDED);
    }

    @Override
    public void onDeath(DamageSource cause) {
       super.onDeath(cause);
       if(!this.getWorld().isClient && !this.isSplitter()){
           if(cause != null && cause.getAttacker() instanceof ServerPlayerEntity) {
               AMAdvancementTriggerRegistry.VOID_WORM_SLAY_HEAD.trigger((ServerPlayerEntity) cause.getAttacker());
           }
       }
    }

    @Override
    public ItemEntity dropStack(ItemStack stack) {
        var itementity = this.dropStack(stack, 0.0F);
        if (itementity != null) {
            itementity.setNoGravity(true);
            itementity.setGlowing(true);
            itementity.setCovetedItem();
        }
        return itementity;
    }

    @Override
    protected void drop(DamageSource source) {
    }

    private void placeDropsSafely(Collection<ItemEntity> drops) {
        BlockPos pos = this.getBlockPos();
        while(!getWorld().getBlockState(pos).isReplaceable() && pos.getY() < getWorld().getTopY() - 2){
            pos = pos.up();
        }
        int radius = 2;
        var residue = AMBlockRegistry.ENDER_RESIDUE.get().getDefaultState().with(EnderResidueBlock.SLOW_DECAY, true);
        for(int x = -radius; x <= radius; x++){
            for(int y = -radius; y <= radius; y++){
                for(int z = -radius; z <= radius; z++){
                    double sq = x * x + y * y + z * z;
                    var pos1 = pos.add(x, y, z);
                    var state = getWorld().getBlockState(pos1);
                    if(sq <= radius * radius && sq >= (radius * radius) - 2.0F && (state.isReplaceable() || state.isOf(AMBlockRegistry.ENDER_RESIDUE.get()))){
                        getWorld().setBlockState(pos1, residue);
                    }
                }
            }
        }
        getWorld().setBlockState(pos, Blocks.AIR.getDefaultState());
        for(var drop : drops){
            drop.setPosition(Vec3d.ofBottomCenter(pos));
            drop.setGlowing(true);
            drop.setNoGravity(true);
            drop.setToDefaultPickupDelay();
            drop.setNeverDespawn();
            drop.setVelocity(Vec3d.ZERO);
            getWorld().spawnEntity(drop);
        }

    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return source.isOf(DamageTypes.FALL) || source.isOf(DamageTypes.DROWN) || source.isOf(DamageTypes.IN_WALL)  || source.isOf(DamageTypes.LAVA) || source.isOf(DamageTypes.OUT_OF_WORLD) || source.isIn(DamageTypeTags.IS_FIRE) || super.isInvulnerableTo(source);
    }

    @Override
    public boolean canImmediatelyDespawn(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    protected void initGoals() {
        super.initGoals();
        this.goalSelector.add(1, new EntityVoidWorm.AIEnterPortal());
        this.goalSelector.add(2, new EntityVoidWorm.AIAttack());
        this.goalSelector.add(3, new EntityVoidWorm.AIFlyIdle());
        this.targetSelector.add(1, new EntityAINearestTarget3D<>(this, PlayerEntity.class, 10, false, true, null));
        this.targetSelector.add(2, new EntityAINearestTarget3D<>(this, EnderDragonEntity.class, 10, false, true, null));
    }

    @Override
    protected EntityNavigation createNavigation(World worldIn) {
        return new DirectPathNavigator(this, getWorld());
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound compound) {
        super.readCustomDataFromNbt(compound);
        if (compound.containsUuid("ChildUUID")) {
            this.setChildId(compound.getUuid("ChildUUID"));
        }
        this.setWormSpeed(compound.getFloat("WormSpeed"));
        this.setSplitter(compound.getBoolean("Splitter"));
        this.setPortalTicks(compound.getInt("PortalTicks"));
        this.makeIdlePortalCooldown = compound.getInt("MakePortalTime");
        this.makePortalCooldown = compound.getInt("MakePortalCooldown");
        if (this.hasCustomName()) {
            this.bossInfo.setName(this.getDisplayName());
        }

    }

    @Override
    public void setCustomName(@Nullable Text name) {
        super.setCustomName(name);
        this.bossInfo.setName(this.getDisplayName());
    }

    @Override
    public boolean hasNoGravity() {
        return true;
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound compound) {
        super.writeCustomDataToNbt(compound);
        if (this.getChildId() != null) {
            compound.putUuid("ChildUUID", this.getChildId());
        }
        compound.putInt("PortalTicks", getPortalTicks());
        compound.putInt("MakePortalTime", makeIdlePortalCooldown);
        compound.putInt("MakePortalCooldown", makePortalCooldown);
        compound.putFloat("WormSpeed", getWormSpeed());
        compound.putBoolean("Splitter", isSplitter());
    }

    public Entity getChild() {
        UUID id = getChildId();
        if (id != null && !this.getWorld().isClient) {
            return ((ServerWorld) getWorld()).getEntity(id);
        }
        return null;
    }

    @Override
    public boolean canBeLeashedBy(PlayerEntity player) {
        return true;
    }

    @Override
    public int getXpToDrop(){
        return this.isSplitter() ? 8 : 50;
    }

    @Override
    public void tick() {
        super.tick();
        prevWormAngle = this.getWormAngle();
        prevJawProgress = this.jawProgress;
        float threshold = 0.05F;
        if (this.isSplitter()) {
            this.experiencePoints = 10;
        } else {
            this.experiencePoints = 70;
        }
        if (this.prevYaw - this.getYaw() > threshold) {
            this.setWormAngle(this.getWormAngle() + 15);
        } else if (this.prevYaw - this.getYaw() < -threshold) {
            this.setWormAngle(this.getWormAngle() - 15);
        } else if (this.getWormAngle() > 0) {
            this.setWormAngle(Math.max(this.getWormAngle() - 20, 0));
        } else if (this.getWormAngle() < 0) {
            this.setWormAngle(Math.min(this.getWormAngle() + 20, 0));
        }
        if (!this.getWorld().isClient) {
            if (!fullyThrough) {
                this.setVelocity(this.getVelocity().multiply(0.9F, 0.9F, 0.9F).add(0, -0.01, 0));
            } else {
                this.setVelocity(this.getVelocity().add(0, 0.01, 0));
            }
        }
        if (Math.abs(this.prevX - this.getX()) < 0.01F && Math.abs(this.prevY - this.getY()) < 0.01F && Math.abs(this.prevZ - this.getZ()) < 0.01F) {            stillTicks++;
        } else {
            stillTicks = 0;
        }
        if (stillTicks > 40 && makePortalCooldown == 0) {
            createStuckPortal();
        }
        if (makePortalCooldown > 0) {
            makePortalCooldown--;
        }
        if (makeIdlePortalCooldown > 0) {
            makeIdlePortalCooldown--;
        }
        if (makeIdlePortalCooldown == 0 && random.nextInt(100) == 0) {
            this.createPortalRandomDestination();
            makeIdlePortalCooldown = 200 + random.nextInt(1000);
        }
        if (this.dataTracker.get(JAW_TICKS) > 0) {
            if (this.jawProgress < 5) {
                jawProgress++;
            }
            this.dataTracker.set(JAW_TICKS, this.dataTracker.get(JAW_TICKS) - 1);
        } else {
            if (this.jawProgress > 0) {
                jawProgress--;
            }
        }
        if (this.isAlive()) {
            for (var entity : this.getWorld().getNonSpectatingEntities(LivingEntity.class, this.getBoundingBox().expand(2.0D))) {
                if (!entity.isPartOf(this) && !(entity instanceof EntityVoidWormPart) && !entity.isTeammate(this) && entity != this) {
                    launch(entity, false);
                }
            }
            this.setStepHeight(2F);
        }else{
            this.setVelocity(new Vec3d(0, 0.03F, 0));
        }
        bodyYaw = getYaw();
        final float f2 = (float) -((float) this.getVelocity().y * (double) MathHelper.DEGREES_PER_RADIAN);
        this.setPitch(f2);
        this.setStepHeight(2F);
        if (!this.getWorld().isClient) {
            Entity child = getChild();
            if (child == null) {
                LivingEntity partParent = this;
                int tailstart = Math.min(3 + random.nextInt(2), getSegmentCount());
                int segments = getSegmentCount();
                for (int i = 0; i < segments; i++) {
                    float scale = 1F + (i / (float) segments) * 0.5F;
                    boolean tail = false;
                    if (i >= segments - tailstart) {
                        tail = true;
                        scale = scale * 0.85F;
                    }
                    EntityVoidWormPart part = new EntityVoidWormPart(AMEntityRegistry.VOID_WORM_PART.get(), partParent,  1.0F + (scale * (tail ? 0.65F : 0.3F)) + (i == 0 ? 0.8F : 0), 180, i == 0 ? -0.0F : i == segments - tailstart ? -0.3F : 0);
                    part.setInvulnerable(partParent.isInvulnerable());
                    part.setParent(partParent);
                    if (updatePostSummon) {
                        part.setPortalTicks(i * 2);
                    }
                    part.setBodyIndex(i);
                    part.setTail(tail);
                    part.setWormScale(scale);
                    if (partParent == this) {
                        this.setChildId(part.getUuid());
                    } else if (partParent instanceof EntityVoidWormPart) {
                        ((EntityVoidWormPart) partParent).setChildId(part.getUuid());
                    }
                    part.setInitialPartPos(this);
                    partParent = part;
                    getWorld().spawnEntity(part);
                }
            }
        }
        if (this.getPortalTicks() > 0) {
            this.setPortalTicks(this.getPortalTicks() - 1);
            if (this.getPortalTicks() == 2 && teleportPos != null) {
                this.setPos(teleportPos.x, teleportPos.y, teleportPos.z);
                teleportPos = null;
            }
        }
        if (this.portalTarget != null && this.portalTarget.getLifespan() < 5) {
            this.portalTarget = null;
        }
        this.bossInfo.setPercent(this.getHealth() / this.getMaxHealth());
        breakBlock();
        if (updatePostSummon) {
            updatePostSummon = false;
        }
        if (!this.isSilent() && !this.getWorld().isClient) {
            this.getWorld().sendEntityStatus(this, (byte) 67);
        }
    }

    public double getBaseMaxHealth() {
        return this.getAttributeBaseValue(EntityAttributes.GENERIC_MAX_HEALTH);
    }

    public void setBaseMaxHealth(double maxHealth, boolean heal){
        this.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(maxHealth);
        if(heal){
            this.heal(this.getMaxHealth());
        }
    }

    @Override
    protected void updatePostDeath() {
        ++this.deathTime;
        if (this.deathTime == (this.isSplitter() ? 20 : 80) && !this.getWorld().isClient()) {
            DamageSource source = this.getRecentDamageSource() == null ? getDamageSources().generic() : this.getRecentDamageSource();
            Entity entity = source.getAttacker();

            //FIXME forge
//            final int i = net.minecraftforge.common.ForgeHooks.getLootingLevel(this, entity, source);
//            this.captureDrops(new java.util.ArrayList<>());

            final boolean flag = this.playerHitTimer > 0;
            if (this.shouldDropLoot() && this.getWorld().getGameRules().getBoolean(GameRules.DO_MOB_LOOT)) {
                this.dropLoot(source, flag);
                //FIXME forge
//                this.dropCustomDeathLoot(source, i, flag);
            }
            this.dropInventory();
            this.dropXp();

            //FIXME
//            Collection<ItemEntity> drops = captureDrops(null);
//
//            if (!net.minecraftforge.common.ForgeHooks.onLivingDrops(this, source, drops, i, lastHurtByPlayerTime > 0)){
//                if(!drops.isEmpty()){
//                    this.placeDropsSafely(drops);
//                }
//            }
            this.getWorld().sendEntityStatus(this, (byte)60);
            this.remove(Entity.RemovalReason.KILLED);
        }
    }

    @Override
    public void onStartedTrackingBy(ServerPlayerEntity player) {
        super.onStartedTrackingBy(player);
        this.bossInfo.addPlayer(player);
    }

    @Override
    public void onStoppedTrackingBy(ServerPlayerEntity player) {
        super.onStoppedTrackingBy(player);
        this.bossInfo.removePlayer(player);
    }

    public void teleportTo(Vec3d vec) {
        this.setPortalTicks(10);
        teleportPos = vec;
        fullyThrough = false;
        if (this.getChild() instanceof EntityVoidWormPart) {
            ((EntityVoidWormPart) this.getChild()).teleportTo(this.getPos(), teleportPos);
        }
    }

    private void launch(Entity e, boolean huge) {
        if (e.isOnGround()) {
            final double d0 = e.getX() - this.getX();
            final double d1 = e.getZ() - this.getZ();
            final double d2 = Math.max(d0 * d0 + d1 * d1, 0.001D);
            final float f = huge ? 2F : 0.5F;
            e.addVelocity(d0 / d2 * f, huge ? 0.5D : 0.2F, d1 / d2 * f);
        }
    }

    public void resetWormScales() {
        if (!this.getWorld().isClient) {
            Entity child = getChild();
            if (child == null) {
                LivingEntity nextPart = this;
                final int tailstart = Math.min(3 + random.nextInt(2), getSegmentCount());
                final int segments = getSegmentCount();
                int i = 0;
                while (nextPart instanceof EntityVoidWormPart) {
                    EntityVoidWormPart part = ((EntityVoidWormPart) ((EntityVoidWormPart) nextPart).getChild());
                    i++;
                    final float scale = 1F + (i / (float) segments) * 0.5F;
                    final boolean tail = i >= segments - tailstart;
                    part.setTail(tail);
                    part.setWormScale(scale);
                    part.radius = 1.0F + (scale * (tail ? 0.65F : 0.3F)) + (i == 0 ? 0.8F : 0F);
                    part.offsetY = i == 0 ? -0.0F : i == segments - tailstart ? -0.3F : 0;
                    nextPart = part;
                }
            }
        }
    }

    @Nullable
    @Override
    public EntityData initialize(ServerWorldAccess worldIn, LocalDifficulty difficultyIn, SpawnReason
            reason, @Nullable EntityData spawnDataIn, @Nullable NbtCompound dataTag) {
        this.setSegmentCount(25 + random.nextInt(15));
        this.setPitch(0.0F);
        this.setBaseMaxHealth(AMConfig.voidWormMaxHealth, true);
        return super.initialize(worldIn, difficultyIn, reason, spawnDataIn, dataTag);
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(SPLIT_FROM_UUID, Optional.empty());
        this.dataTracker.startTracking(CHILD_UUID, Optional.empty());
        this.dataTracker.startTracking(SEGMENT_COUNT, 10);
        this.dataTracker.startTracking(JAW_TICKS, 0);
        this.dataTracker.startTracking(WORM_ANGLE, 0F);
        this.dataTracker.startTracking(SPEEDMOD, 1F);
        this.dataTracker.startTracking(SPLITTER, false);
        this.dataTracker.startTracking(PORTAL_TICKS, 0);
    }

    public float getWormAngle() {
        return this.dataTracker.get(WORM_ANGLE);
    }

    public void setWormAngle(float progress) {
        this.dataTracker.set(WORM_ANGLE, progress);
    }

    public float getWormSpeed() {
        return this.dataTracker.get(SPEEDMOD);
    }

    public void setWormSpeed(float progress) {
        if (getWormSpeed() != progress) {
            moveControl = new FlightMoveController(this, progress, false, true);
        }
        this.dataTracker.set(SPEEDMOD, progress);
    }

    public boolean isSplitter() {
        return this.dataTracker.get(SPLITTER);
    }

    public void setSplitter(boolean splitter) {
        this.dataTracker.set(SPLITTER, splitter);
    }

    public void openMouth(int time) {
        this.dataTracker.set(JAW_TICKS, time);
    }

    public boolean isMouthOpen() {
        return this.dataTracker.get(JAW_TICKS) >= 5F;
    }

    @Nullable
    public UUID getChildId() {
        return this.dataTracker.get(CHILD_UUID).orElse(null);
    }

    public void setChildId(@Nullable UUID uniqueId) {
        this.dataTracker.set(CHILD_UUID, Optional.ofNullable(uniqueId));
    }

    @Nullable
    public UUID getSplitFromUUID() {
        return this.dataTracker.get(SPLIT_FROM_UUID).orElse(null);
    }

    public void setSplitFromUuid(@Nullable UUID uniqueId) {
        this.dataTracker.set(SPLIT_FROM_UUID, Optional.ofNullable(uniqueId));
    }

    public int getPortalTicks() {
        return this.dataTracker.get(PORTAL_TICKS);
    }

    public void setPortalTicks(int ticks) {
        this.dataTracker.set(PORTAL_TICKS, ticks);
    }

    public int getSegmentCount() {
        return this.dataTracker.get(SEGMENT_COUNT);
    }

    public void setSegmentCount(int command) {
        this.dataTracker.set(SEGMENT_COUNT, command);
    }

    @Override
    public void tickCramming() {
        var entities = this.getWorld().getOtherEntities(this, this.getBoundingBox().stretch(0.20000000298023224D, 0.0D, 0.20000000298023224D));
        entities.stream().filter(entity -> !(entity instanceof EntityVoidWormPart) && entity.isPushable()).forEach(entity -> entity.pushAwayFrom(this));
    }

    @Override
    public void pushAwayFrom(Entity entityIn) {
    }

    public void createStuckPortal() {
        if (this.getTarget() != null) {
            createPortal(this.getTarget().getPos().add(random.nextInt(8) - 4, 2 + random.nextInt(3), random.nextInt(8) - 4));
        } else {
            Vec3d vec = Vec3d.ofCenter(getWorld().getTopPosition(Heightmap.Type.MOTION_BLOCKING, this.getBlockPos().up(random.nextInt(10) + 10)));
            createPortal(vec);
        }
    }

    public void createPortal(Vec3d to) {
        createPortal(this.getPos().add(this.getRotationVector().multiply(20)), to, null);
    }

    public void createPortalRandomDestination() {
        Vec3d vec = null;
        for (int i = 0; i < 15; i++) {
            BlockPos pos = AMBlockPos.fromCoords(this.getX() + random.nextInt(60) - 30, 0, this.getZ() + random.nextInt(60) - 30);
            BlockPos height = getWorld().getTopPosition(Heightmap.Type.MOTION_BLOCKING, pos);
            if(height.getY() < 10){
                height = height.up(50 + random.nextInt(50));
            }else{
                height = height.up(random.nextInt(30));
            }
            if (getWorld().isAir(height)) {
                vec = Vec3d.ofBottomCenter(height);
            }
        }
        if (vec != null) {
            createPortal(this.getPos().add(this.getRotationVector().multiply(20)), vec, null);
        }
    }

    public void createPortal(Vec3d from, Vec3d to, @Nullable Direction outDir) {
        if (!this.getWorld().isClient && portalTarget == null) {
            var Vector3d = new Vec3d(this.getX(), this.getEyeY(), this.getZ());
            var result = this.getWorld().raycast(new RaycastContext(Vector3d, from, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, this));
            var vec = result.getPos() != null ? result.getPos() : this.getPos();
            if (result instanceof BlockHitResult) {
                var result1 = (BlockHitResult) result;
                vec = vec.add(Vec3d.of(result1.getSide().getVector()));
            }
            var portal = AMEntityRegistry.VOID_PORTAL.get().create(getWorld());
            portal.setPosition(vec.x, vec.y, vec.z);
            var dirVec = vec.subtract(this.getPos());
            Direction dir = Direction.getFacing(dirVec.x, dirVec.y, dirVec.z);
            portal.setAttachmentFacing(dir);
            portal.setLifespan(10000);
            if (!this.getWorld().isClient) {
                getWorld().spawnEntity(portal);
            }
            portalTarget = portal;
            portal.setDestination(AMBlockPos.fromCoords(to.x, to.y, to.z), outDir);
            makePortalCooldown = 300;
        }
    }

    public void resetPortalLogic() {
        portalTarget = null;
        stillTicks = 0;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    public void breakBlock() {
        if (this.blockBreakCounter > 0) {
            --this.blockBreakCounter;
            return;
        }
        boolean flag = false;
        if (!this.getWorld().isClient && this.blockBreakCounter == 0 && PlatformEvent.getMobGriefingEvent(getWorld(), this)) {
            for (int a = (int) Math.round(this.getBoundingBox().minX); a <= (int) Math.round(this.getBoundingBox().maxX); a++) {
                for (int b = (int) Math.round(this.getBoundingBox().minY) - 1; (b <= (int) Math.round(this.getBoundingBox().maxY) + 1) && (b <= 127); b++) {
                    for (int c = (int) Math.round(this.getBoundingBox().minZ); c <= (int) Math.round(this.getBoundingBox().maxZ); c++) {
                        var pos = new BlockPos(a, b, c);
                        var state = getWorld().getBlockState(pos);
                        var fluidState = getWorld().getFluidState(pos);
                        var block = state.getBlock();
                        if (!state.isAir() && !state.getOutlineShape(getWorld(), pos).isEmpty() && state.isIn(AMTagRegistry.VOID_WORM_BREAKABLES) && fluidState.isEmpty()) {
                            if (block != Blocks.AIR) {
                                this.setVelocity(this.getVelocity().multiply(0.6F, 1, 0.6F));
                                flag = true;
                                getWorld().breakBlock(pos, true);
                                if (state.isIn(BlockTags.ICE)) {
                                    getWorld().setBlockState(pos, Blocks.WATER.getDefaultState());
                                }
                            }
                        }
                    }
                }
            }
        }
        if (flag) {
            blockBreakCounter = 10;
        }
    }

    public boolean isTargetBlocked(Vec3d target) {
        var Vector3d = new Vec3d(this.getX(), this.getEyeY(), this.getZ());

        return this.getWorld().raycast(new RaycastContext(Vector3d, target, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, this)).getType() != HitResult.Type.MISS;
    }

    public Vec3d getBlockInViewAway(Vec3d fleePos, float radiusAdd) {
        final float radius = (0.75F * (0.7F * 6) * -3 - this.getRandom().nextInt(24)) * radiusAdd;
        final float neg = this.getRandom().nextBoolean() ? 1 : -1;
        final float renderYawOffset = this.bodyYaw;
        final float angle = (Maths.STARTING_ANGLE * renderYawOffset) + 3.15F + (this.getRandom().nextFloat() * neg);
        final double extraX = radius * MathHelper.sin(MathHelper.PI + angle);
        final double extraZ = radius * MathHelper.cos(angle);
        BlockPos radialPos = AMBlockPos.fromCoords(fleePos.x + extraX, 0, fleePos.z + extraZ);
        BlockPos ground = getGround(radialPos);
        final int distFromGround = (int) this.getY() - ground.getY();
        final int flightHeight = 10 + this.getRandom().nextInt(20);
        BlockPos newPos = ground.up(distFromGround > 8 ? flightHeight : this.getRandom().nextInt(10) + 15);
        if (!this.isTargetBlocked(Vec3d.ofCenter(newPos)) && this.squaredDistanceTo(Vec3d.ofCenter(newPos)) > 1) {
            return Vec3d.ofCenter(newPos);
        }
        return null;
    }

    public Vec3d getBlockInViewAwaySlam(Vec3d fleePos, int slamHeight) {
        final float radius = 3 + random.nextInt(3);
        final float neg = this.getRandom().nextBoolean() ? 1 : -1;
        final float renderYawOffset = this.bodyYaw;
        final float angle = (Maths.STARTING_ANGLE * renderYawOffset) + 3.15F + (this.getRandom().nextFloat() * neg);
        final double extraX = radius * MathHelper.sin(MathHelper.PI + angle);
        final double extraZ = radius * MathHelper.cos(angle);
        BlockPos radialPos = AMBlockPos.fromCoords(fleePos.x + extraX, 0, fleePos.z + extraZ);
        BlockPos ground = getHeighestAirAbove(radialPos, slamHeight);
        if (!this.isTargetBlocked(Vec3d.ofCenter(ground)) && this.squaredDistanceTo(Vec3d.ofCenter(ground)) > 1) {
            return Vec3d.ofCenter(ground);
        }
        return null;
    }

    private BlockPos getHeighestAirAbove(BlockPos radialPos, int limit) {
        BlockPos position = AMBlockPos.fromCoords(radialPos.getX(), this.getY(), radialPos.getZ());
        while (position.getY() < 256 && position.getY() < this.getY() + limit && getWorld().isAir(position)) {
            position = position.up();
        }
        return position;
    }

    private BlockPos getGround(BlockPos in) {
        BlockPos position = AMBlockPos.fromCoords(in.getX(), this.getY(), in.getZ());
        while (position.getY() > -63 && !getWorld().getBlockState(position).isSolid()) {
            position = position.down();
        }
        if (position.getY() < -62) {
            return position.up(120 + random.nextInt(5));
        }
        return position;
    }

    @Override
    public boolean isTeammate(Entity entityIn) {
        return super.isTeammate(entityIn) || this.getSplitFromUUID() != null && this.getSplitFromUUID().equals(entityIn.getUuid()) ||
                entityIn instanceof EntityVoidWorm && ((EntityVoidWorm) entityIn).getSplitFromUUID() != null && ((EntityVoidWorm) entityIn).getSplitFromUUID().equals(entityIn.getUuid());
    }

    private void spit(Vec3d shotAt, boolean portal) {
        shotAt = shotAt.rotateY(-this.getYaw() * MathHelper.RADIANS_PER_DEGREE);
        EntityVoidWormShot shot = new EntityVoidWormShot(this.getWorld(), this);
        final double d0 = shotAt.x;
        final double d1 = shotAt.y;
        final double d2 = shotAt.z;
        final float f = MathHelper.sqrt((float) (d0 * d0 + d2 * d2)) * 0.35F;

        shot.shoot(d0, d1 + (double) f, d2, 0.5F, 3.0F);
        if (!this.isSilent()) {
            this.emitGameEvent(GameEvent.PROJECTILE_SHOOT);
            this.getWorld().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ENTITY_DROWNED_SHOOT, this.getSoundCategory(), 1.0F, 1.0F + (this.random.nextFloat() - this.random.nextFloat()) * 0.2F);
        }
        this.openMouth(5);
        this.getWorld().spawnEntity(shot);
    }

    private boolean wormAttack(Entity entity, DamageSource source, float dmg) {
        dmg *= AMConfig.voidWormDamageModifier;
        return entity instanceof EnderDragonEntity ? ((EnderDragonEntity) entity).parentDamage(source, dmg * 0.5F) : entity.damage(source, dmg);
    }

    public void playHurtSoundWorm(DamageSource source) {
        this.playHurtSound(source);
    }

    private enum AttackMode {
        CIRCLE,
        SLAM_RISE,
        SLAM_FALL,
        PORTAL
    }

    private class AIFlyIdle extends Goal {
        protected final EntityVoidWorm voidWorm;
        protected double x;
        protected double y;
        protected double z;

        public AIFlyIdle() {
            super();
            this.setControls(EnumSet.of(Control.MOVE));
            this.voidWorm = EntityVoidWorm.this;
        }

        @Override
        public boolean canStart() {
            if (this.voidWorm.hasPassengers() || this.voidWorm.portalTarget != null || (voidWorm.getTarget() != null && voidWorm.getTarget().isAlive()) || this.voidWorm.hasVehicle()) {
                return false;
            } else {
                var lvt_1_1_ = this.getPosition();
                if (lvt_1_1_ == null) {
                    return false;
                } else {
                    this.x = lvt_1_1_.x;
                    this.y = lvt_1_1_.y;
                    this.z = lvt_1_1_.z;
                    return true;
                }
            }
        }

        @Override
        public void tick() {
            voidWorm.getMoveControl().moveTo(x, y, z, 1F);
        }

        @Nullable
        protected Vec3d getPosition() {
            Vec3d vector3d = voidWorm.getPos();
            return voidWorm.getBlockInViewAway(vector3d, 1);
        }

        @Override
        public boolean shouldContinue() {
            return voidWorm.squaredDistanceTo(x, y, z) > 20F && this.voidWorm.portalTarget == null && !voidWorm.horizontalCollision && (voidWorm.getTarget() == null || !voidWorm.getTarget().isAlive());
        }

        @Override
        public void start() {
            voidWorm.getMoveControl().moveTo(x, y, z, 1F);
        }

        @Override
        public void stop() {
            this.voidWorm.getNavigation().stop();
            super.stop();
        }
    }

    public class AIAttack extends Goal {

        private AttackMode mode = AttackMode.CIRCLE;
        private int modeTicks = 0;
        private int maxCircleTime = 500;
        private Vec3d moveTo = null;

        public AIAttack() {
            super();
            this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
        }

        @Override
        public boolean canStart() {
            return EntityVoidWorm.this.getTarget() != null && EntityVoidWorm.this.getTarget().isAlive();
        }

        @Override
        public void stop() {
            mode = AttackMode.CIRCLE;
            modeTicks = 0;
        }

        @Override
        public void start() {
            mode = AttackMode.CIRCLE;
            maxCircleTime = 60 + random.nextInt(200);
        }

        @Override
        public void tick() {
            LivingEntity target = EntityVoidWorm.this.getTarget();
            boolean flag = false;
            float speed = 1;
            for (Entity entity : EntityVoidWorm.this.getWorld().getNonSpectatingEntities(LivingEntity.class, EntityVoidWorm.this.getBoundingBox().expand(2.0D))) {
                if (!entity.isPartOf(EntityVoidWorm.this) && !(entity instanceof EntityVoidWormPart) && !entity.isTeammate(EntityVoidWorm.this) && entity != EntityVoidWorm.this) {
                    if (EntityVoidWorm.this.isMouthOpen()) {
                        launch(entity, true);
                        flag = true;
                        wormAttack(entity, EntityVoidWorm.this.getDamageSources().mobAttack(EntityVoidWorm.this), 8.0F + random.nextFloat() * 8.0F);
                    } else {
                        EntityVoidWorm.this.openMouth(15);
                    }
                }
            }
            if (target != null) {
                if (mode == AttackMode.CIRCLE) {
                    if (moveTo == null || EntityVoidWorm.this.squaredDistanceTo(moveTo) < 16 || EntityVoidWorm.this.horizontalCollision) {
                        moveTo = EntityVoidWorm.this.getBlockInViewAway(target.getPos(), 0.4F + random.nextFloat() * 0.2F);
                    }
                    int interval = EntityVoidWorm.this.getHealth() < EntityVoidWorm.this.getMaxHealth() && !EntityVoidWorm.this.isSplitter() ? 15 : 40;
                    if (modeTicks % interval == 0) {
                        EntityVoidWorm.this.spit(new Vec3d(3, 3, 0), false);
                        EntityVoidWorm.this.spit(new Vec3d(-3, 3, 0), false);
                        EntityVoidWorm.this.spit(new Vec3d(3, -3, 0), false);
                        EntityVoidWorm.this.spit(new Vec3d(-3, -3, 0), false);
                    }
                    modeTicks++;
                    if (modeTicks > maxCircleTime) {
                        maxCircleTime = 60 + random.nextInt(200);
                        mode = AttackMode.SLAM_RISE;
                        modeTicks = 0;
                        moveTo = null;
                    }
                } else if (mode == AttackMode.SLAM_RISE) {
                    if (moveTo == null) {
                        moveTo = EntityVoidWorm.this.getBlockInViewAwaySlam(target.getPos(), 20 + random.nextInt(20));
                    }
                    if (moveTo != null) {
                        if (EntityVoidWorm.this.getY() > target.getY() + 15) {
                            moveTo = null;
                            modeTicks = 0;
                            mode = AttackMode.SLAM_FALL;
                        }
                    }
                } else if (mode == AttackMode.SLAM_FALL) {
                    speed = 2;
                    EntityVoidWorm.this.lookAtEntity(target, 360, 360);
                    moveTo = target.getPos();
                    if (EntityVoidWorm.this.horizontalCollision) {
                        moveTo = new Vec3d(target.getX(), EntityVoidWorm.this.getY() + 3, target.getZ());
                    }
                    EntityVoidWorm.this.openMouth(20);
                    if (EntityVoidWorm.this.squaredDistanceTo(moveTo) < 4 || flag) {
                        mode = AttackMode.CIRCLE;
                        moveTo = null;
                        modeTicks = 0;
                    }
                }
            }
            if (!EntityVoidWorm.this.canSee(target) && random.nextInt(100) == 0) {
                if (EntityVoidWorm.this.makePortalCooldown == 0) {
                    var to = new Vec3d(target.getX(), target.getBoundingBox().maxY + 0.1, target.getZ());
                    EntityVoidWorm.this.createPortal(EntityVoidWorm.this.getPos().add(EntityVoidWorm.this.getRotationVector().multiply(20)), to, Direction.UP);
                    EntityVoidWorm.this.makePortalCooldown = 50;
                    mode = AttackMode.SLAM_FALL;
                }
            }
            if (moveTo != null && EntityVoidWorm.this.portalTarget == null) {
                EntityVoidWorm.this.getMoveControl().moveTo(moveTo.x, moveTo.y, moveTo.z, speed);
            }
        }
    }

    public class AIEnterPortal extends Goal {

        public AIEnterPortal() {
            super();
            this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
        }

        @Override
        public boolean canStart() {
            return EntityVoidWorm.this.portalTarget != null;
        }

        @Override
        public void tick() {
            if (EntityVoidWorm.this.portalTarget != null) {
                noClip = true;
                double centerX = EntityVoidWorm.this.portalTarget.getX();
                double centerY = EntityVoidWorm.this.portalTarget.getBodyY(0.5F);
                double centerZ = EntityVoidWorm.this.portalTarget.getZ();
                double d0 = centerX - EntityVoidWorm.this.getX();
                double d1 = centerY - EntityVoidWorm.this.getBodyY(0.5F);
                double d2 = centerZ - EntityVoidWorm.this.getZ();
                Vec3d vec = new Vec3d(d0, d1, d2);
                if(vec.length() > 1F){
                    vec = vec.normalize();
                }
                vec = vec.multiply(0.4F);
                EntityVoidWorm.this.setVelocity(EntityVoidWorm.this.getVelocity().add(vec));
            }
        }

        @Override
        public void stop() {
            noClip = false;
        }
    }

    @Environment(EnvType.CLIENT)
    public void handleStatus(byte id) {
        if (id == 67) {
            final float f2 = MinecraftClient.getInstance().options.getSoundVolume(SoundCategory.MUSIC);
            if (f2 <= 0) {
                SoundWormBoss.WORMBOSS_SOUND_MAP.clear();
            } else {
                SoundWormBoss sound = SoundWormBoss.WORMBOSS_SOUND_MAP.computeIfAbsent(this.getId(), i -> new SoundWormBoss(this));
                if (!MinecraftClient.getInstance().getSoundManager().isPlaying(sound) && sound.isNearest()) {
                    MinecraftClient.getInstance().getSoundManager().play(sound);
                }
            }
        } else {
            super.handleStatus(id);
        }
    }
}
