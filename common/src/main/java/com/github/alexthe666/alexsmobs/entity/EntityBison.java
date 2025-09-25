package com.github.alexthe666.alexsmobs.entity;

import com.github.alexthe666.alexsmobs.config.AMConfig;
import com.github.alexthe666.alexsmobs.entity.ai.AdvancedPathNavigateNoTeleport;
import com.github.alexthe666.alexsmobs.entity.ai.AnimalAIHurtByTargetNotBaby;
import com.github.alexthe666.alexsmobs.entity.ai.AnimalAIPanicBaby;
import com.github.alexthe666.alexsmobs.entity.ai.AnimalAIWanderRanged;
import com.github.alexthe666.alexsmobs.platform.PlatformEvent;
import com.github.alexthe666.alexsmobs.registry.AMEntityRegistry;
import com.github.alexthe666.alexsmobs.registry.AMItemRegistry;
import com.github.alexthe666.alexsmobs.registry.AMSoundRegistry;
import com.github.alexthe666.alexsmobs.registry.AMTagRegistry;
import com.iafenvoy.uranus.animation.Animation;
import com.iafenvoy.uranus.animation.AnimationHandler;
import com.iafenvoy.uranus.animation.IAnimatedEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.SnowBlock;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.ShovelItem;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.recipe.Ingredient;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.event.GameEvent;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public abstract class EntityBison extends AnimalEntity implements IAnimatedEntity, Shearable {

    public static final Animation ANIMATION_PREPARE_CHARGE = Animation.create(40);
    public static final Animation ANIMATION_EAT = Animation.create(35);
    public static final Animation ANIMATION_ATTACK = Animation.create(15);
    private static final TrackedData<Boolean> SHEARED = DataTracker.registerData(EntityBison.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> SNOWY = DataTracker.registerData(EntityBison.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> CHARGING = DataTracker.registerData(EntityBison.class, TrackedDataHandlerRegistry.BOOLEAN);
    public float prevChargeProgress;
    public float chargeProgress;
    private int animationTick;
    private Animation currentAnimation;
    private int snowTimer = 0;
    private boolean permSnow = false;
    private int blockBreakCounter;
    private int chargeCooldown = random.nextInt(2000);
    private EntityBison chargePartner;
    private boolean hasChargedSpeed = false;
    protected int feedingsSinceLastShear = 0;

    protected EntityBison(EntityType<? extends AnimalEntity> animal, World lvl) {
        super(animal, lvl);
        this.setStepHeight(1.1F);
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createLivingAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 40.0D)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 8.0D)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.25F)
                .add(EntityAttributes.GENERIC_ATTACK_KNOCKBACK, 2.0D);
    }

    @Override
    public boolean canSpawn(WorldAccess worldIn, SpawnReason spawnReasonIn) {
        return AMEntityRegistry.rollSpawn(AMConfig.bisonSpawnRolls, this.getRandom(), spawnReasonIn);
    }

    @Override
    public EntityData initialize(ServerWorldAccess worldIn, LocalDifficulty difficultyIn, SpawnReason reason, @Nullable EntityData spawnDataIn, @Nullable NbtCompound dataTag) {
        if (spawnDataIn == null) {
            spawnDataIn = new PassiveEntity.PassiveData(0.25F);
        }
        return super.initialize(worldIn, difficultyIn, reason, spawnDataIn, dataTag);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return AMSoundRegistry.BISON_IDLE.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return AMSoundRegistry.BISON_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return AMSoundRegistry.BISON_HURT.get();
    }

    @Override
    protected void playStepSound(BlockPos p_28301_, BlockState p_28302_) {
        this.playSound(SoundEvents.ENTITY_COW_STEP, 0.1F, 1.0F);
    }

    public boolean isSnowy() {
        return this.dataTracker.get(SNOWY);
    }

    public void setSnowy(boolean honeyed) {
        this.dataTracker.set(SNOWY, honeyed);
    }

    @Override
    protected void initGoals() {
        super.initGoals();
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new MeleeAttackGoal(this, 1D, true));
        this.goalSelector.add(3, new AnimalAIPanicBaby(this, 1.25D));
        this.goalSelector.add(4, new AnimalMateGoal(this, 1.0D));
        this.goalSelector.add(4, new TemptGoal(this, 1.0D, Ingredient.fromTag(AMTagRegistry.BISON_BREEDABLES), false));
        this.goalSelector.add(5, new FollowParentGoal(this, 1.1D));
        this.goalSelector.add(6, new AIChargeFurthest());
        this.goalSelector.add(7, new AnimalAIWanderRanged(this, 70, 1.0D, 18, 7));
        this.goalSelector.add(7, new LookAtEntityGoal(this, PlayerEntity.class, 15.0F));
        this.goalSelector.add(7, new LookAroundGoal(this));
        this.targetSelector.add(1, (new AIAttackNearPlayers()));
        this.targetSelector.add(2, (new AnimalAIHurtByTargetNotBaby(this)));

    }

    @Override
    public boolean isBreedingItem(ItemStack stack) {
        return stack.isIn(AMTagRegistry.BISON_BREEDABLES);
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(SHEARED, false);
        this.dataTracker.startTracking(SNOWY, false);
        this.dataTracker.startTracking(CHARGING, false);
    }

    @Nullable
    @Override
    public PassiveEntity createChild(ServerWorld world, PassiveEntity entity) {
        return AMEntityRegistry.BISON.get().create(world);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound compound) {
        super.readCustomDataFromNbt(compound);
        this.setSnowy(compound.getBoolean("Snowy"));
        this.setSheared(compound.getBoolean("Sheared"));
        this.permSnow = compound.getBoolean("SnowPerm");
        this.chargeCooldown = compound.getInt("ChargeCooldown");
        this.feedingsSinceLastShear = compound.getInt("Feedings");
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound compound) {
        super.writeCustomDataToNbt(compound);
        compound.putBoolean("Snowy", this.isSnowy());
        compound.putBoolean("Sheared", this.isSheared());
        compound.putBoolean("SnowPerm", this.permSnow);
        compound.putInt("ChargeCooldown", this.chargeCooldown);
        compound.putInt("Feedings", this.feedingsSinceLastShear);
    }

    @Override
    protected EntityNavigation createNavigation(World worldIn) {
        return new AdvancedPathNavigateNoTeleport(this, worldIn, true);
    }

    @Override
    public void tick() {
        super.tick();
        this.prevChargeProgress = this.chargeProgress;
        if (this.isCharging() && chargeProgress < 5F) {
            chargeProgress++;
        }
        if (!this.isCharging() && chargeProgress > 0F) {
            chargeProgress--;
        }
        if (!this.getWorld().isClient) {
            if (snowTimer == 0) {
                snowTimer = 200 + random.nextInt(400);
                if (this.isSnowy()) {
                    if (!permSnow) {
                        if (this.getFireTicks() > 0 || this.isInsideWaterOrBubbleColumn() || !EntityGrizzlyBear.isSnowingAt(getWorld(), this.getBlockPos().up())) {
                            this.setSnowy(false);
                        }
                    }
                } else {
                    if (EntityGrizzlyBear.isSnowingAt(getWorld(), this.getBlockPos())) {
                        this.setSnowy(true);
                    }
                }
            }

            var attackTarget = this.getTarget();
            if (this.getVelocity().lengthSquared() < 0.05D && this.getAnimation() == NO_ANIMATION && (attackTarget == null || !attackTarget.isAlive())) {
                if ((getRandom().nextInt(600) == 0 && getWorld().getBlockState(this.getBlockPos().down()).isOf(Blocks.GRASS_BLOCK))) {
                    this.setAnimation(ANIMATION_EAT);
                }
            }
            if(this.getAnimation() == ANIMATION_EAT && this.getAnimationTick() == 30 && getWorld().getBlockState(this.getBlockPos().down()).isOf(Blocks.GRASS_BLOCK)){
                this.feedingsSinceLastShear++;
                BlockPos down = this.getBlockPos().down();
                this.getWorld().syncWorldEvent(2001, down, Block.getRawIdFromState(Blocks.GRASS_BLOCK.getDefaultState()));
                this.getWorld().setBlockState(down, Blocks.DIRT.getDefaultState(), 2);
            }

            if (isCharging()) {
                if (!hasChargedSpeed) {
                    this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).setBaseValue(0.65F);
                    hasChargedSpeed = true;
                }
            } else {
                if (hasChargedSpeed) {
                    this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).setBaseValue(0.25F);
                    hasChargedSpeed = false;
                }
            }

            if (attackTarget != null && attackTarget.isAlive() && this.isAlive()) {
                final double dist = this.distanceTo(attackTarget);
                if (this.canSee(attackTarget)) {
                    this.lookAtEntity(attackTarget, 30, 30);
                    this.bodyYaw = this.getYaw();
                }
                if (dist < this.getWidth() + 3.0F) {
                    if (this.getAnimation() == ANIMATION_ATTACK && this.getAnimationTick() > 8 && dist < this.getWidth() + 1.0F && this.canSee(attackTarget)) {
                        float dmg = (float) this.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE).getBaseValue();
                        if (attackTarget instanceof WolfEntity) {
                            dmg = 2;
                        }
                        this.launch(attackTarget, isCharging());
                        if (isCharging()) {
                            dmg += 3;
                            this.setCharging(false);
                        }
                        attackTarget.damage(this.getDamageSources().mobAttack(this), dmg);
                    }
                } else if (!this.isCharging()) {
                    final Animation animation = this.getAnimation();
                    if(animation == NO_ANIMATION){
                        this.setAnimation(ANIMATION_PREPARE_CHARGE);
                    }
                    else if (animation == ANIMATION_PREPARE_CHARGE) {
                        this.getNavigation().stop();
                        if (this.getAnimationTick() > 30) {
                            this.setCharging(true);
                        }
                    }
                }
            }
        }
        if (chargeCooldown > 0) {
            chargeCooldown--;
        }
        if(feedingsSinceLastShear >= 5 && this.isSheared()){
            feedingsSinceLastShear = 0;
            this.setSheared(false);
        }
        if (!this.getWorld().isClient && this.isCharging() && (this.getTarget() == null && this.chargePartner == null || this.isInsideWaterOrBubbleColumn())) {
            this.setCharging(false);
        }
        AnimationHandler.INSTANCE.updateAnimations(this);
    }

    @Override
    public boolean tryAttack(Entity entityIn) {
        if (this.getAnimation() == NO_ANIMATION) {
            this.setAnimation(ANIMATION_ATTACK);
        }
        return true;
    }

    public boolean isSheared() {
        return this.dataTracker.get(SHEARED);
    }

    public void setSheared(boolean b) {
        this.dataTracker.set(SHEARED, b);
    }

    private void launch(Entity launch, boolean huge) {
        final float rot = 180F + this.getYaw();
        final float hugeScale = huge ? 4F : 0.6F;
        final float strength = (float) (hugeScale *  (1.0D - ((LivingEntity) launch).getAttributeValue(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE)));
        final float rotRad = rot * MathHelper.RADIANS_PER_DEGREE;
        final float x = MathHelper.sin(rotRad);
        final float z = -MathHelper.cos(rotRad);
        launch.velocityDirty = true;
        final Vec3d vec3 = this.getVelocity();
        final Vec3d vec31 = vec3.add((new Vec3d(x, 0.0D, z)).normalize().multiply(strength));
        launch.setVelocity(vec31.x, huge ? 1F : 0.5F, vec31.z);
        launch.setOnGround(false);
    }

    private void knockbackTarget(LivingEntity entity, float strength, float angle) {
        float rot = getYaw() + angle;
        if(entity != null){
            entity.takeKnockback(strength, MathHelper.sin(rot * MathHelper.RADIANS_PER_DEGREE), -MathHelper.cos(rot * MathHelper.RADIANS_PER_DEGREE));
        }
    }

    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        final var itemstack = player.getStackInHand(hand);
        final var item = itemstack.getItem();
        final var type = super.interactMob(player, hand);
        if (!this.getWorld().isClient) {
            if (item == Items.SNOW && !this.isSnowy()) {
                this.eat(player, hand, itemstack);
                this.permSnow = true;
                this.setSnowy(true);
                this.playSound(SoundEvents.BLOCK_SNOW_PLACE, this.getSoundVolume(), this.getSoundPitch());
                this.emitGameEvent(GameEvent.ENTITY_INTERACT);
                return ActionResult.SUCCESS;
            }

            if (item instanceof ShovelItem && this.isSnowy()) {
                this.permSnow = false;
                if (!player.isCreative()) {
                    itemstack.damage(1, this.getRandom(), player instanceof ServerPlayerEntity ? (ServerPlayerEntity) player : null);
                }
                this.setSnowy(false);
                this.playSound(SoundEvents.BLOCK_SNOW_BREAK, this.getSoundVolume(), this.getSoundPitch());
                this.emitGameEvent(GameEvent.ENTITY_INTERACT);
                return ActionResult.SUCCESS;
            }
        }
        return type;
    }

    @Override
    public void mobTick() {
        super.mobTick();
        breakBlock();
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
                        final BlockPos pos = new BlockPos(a, b, c);
                        final BlockState state = getWorld().getBlockState(pos);
                        final Block block = state.getBlock();
                        if (block == Blocks.SNOW && state.get(SnowBlock.LAYERS) <= 1) {
                            this.setVelocity(this.getVelocity().multiply(0.6F, 1, 0.6F));
                            flag = true;
                            getWorld().breakBlock(pos, true);
                        }
                    }
                }
            }
        }
        if (flag) {
            blockBreakCounter = this.isCharging() && this.getTarget() != null ? 2 : 20;
        }
    }

    @Override
    public int getAnimationTick() {
        return animationTick;
    }

    @Override
    public void setAnimationTick(int i) {
        animationTick = i;
    }

    @Override
    public Animation getAnimation() {
        return currentAnimation;
    }

    @Override
    public void setAnimation(Animation animation) {
        currentAnimation = animation;
    }

    @Override
    public Animation[] getAnimations() {
        return new Animation[]{ANIMATION_PREPARE_CHARGE, ANIMATION_ATTACK, ANIMATION_EAT};
    }

    @Override
    public void sheared(SoundCategory category) {
        getWorld().playSoundFromEntity(null, this, SoundEvents.ENTITY_SHEEP_SHEAR, category, 1.0F, 1.0F);
        this.emitGameEvent(GameEvent.ENTITY_INTERACT);
        this.setSheared(true);
        this.feedingsSinceLastShear = 0;
        for (int i = 0; i < 2 + random.nextInt(2); i++) {
            this.dropItem(AMItemRegistry.BISON_FUR.get());
        }
    }

    public boolean isCharging() {
        return this.dataTracker.get(CHARGING);
    }

    public void setCharging(boolean charging) {
        this.dataTracker.set(CHARGING, charging);
    }

    @Override
    public boolean isShearable() {
        return !isSheared() && !isBaby();
    }

    public boolean isValidCharging() {
        return !this.isBaby() && this.isAlive() && chargeCooldown == 0 && !this.isInsideWaterOrBubbleColumn();
    }


    public void pushBackJostling(EntityBison bison, float strength) {
        applyKnockbackFromBuffalo(strength, bison.getX() - this.getX(), bison.getZ() - this.getZ());
    }

    private void applyKnockbackFromBuffalo(float strength, double ratioX, double ratioZ) {
        //FIXME forge
//        net.minecraftforge.event.entity.living.LivingKnockBackEvent event = net.minecraftforge.common.ForgeHooks.onLivingKnockBack(this, strength, ratioX, ratioZ);
//        if (event.isCanceled()) return;
//        strength = event.getStrength();
//        ratioX = event.getRatioX();
//        ratioZ = event.getRatioZ();
//        if (!(strength <= 0.0F)) {
//            this.velocityDirty = true;
//            Vec3d vector3d = this.getVelocity();
//            Vec3d vector3d1 = (new Vec3d(ratioX, 0.0D, ratioZ)).normalize().scale(strength);
//            this.setVelocity(vector3d.x / 2.0D - vector3d1.x, 0.3F, vector3d.z / 2.0D - vector3d1.z);
//        }
    }

    private void resetChargeCooldown() {
        this.setCharging(false);
        this.chargePartner = null;
        this.chargeCooldown = 1000 + random.nextInt(2000);
    }

    private class AIChargeFurthest extends Goal {

        public AIChargeFurthest() {
            this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
        }

        @Override
        public boolean canStart() {
            if (EntityBison.this.isValidCharging()) {
                if (EntityBison.this.chargePartner != null && EntityBison.this.chargePartner.isValidCharging() && EntityBison.this.chargePartner != EntityBison.this) {
                    EntityBison.this.chargePartner.chargePartner = EntityBison.this;
                    return true;
                } else if (random.nextInt(100) == 0) {
                    EntityBison furthest = null;
                    for (final var bison : EntityBison.this.getWorld().getNonSpectatingEntities(EntityBison.class, EntityBison.this.getBoundingBox().expand(15F))) {
                        if (bison.chargeCooldown == 0 && !bison.isBaby() && !bison.isPartOf(EntityBison.this)) {
                            if (furthest == null || EntityBison.this.distanceTo(furthest) < EntityBison.this.distanceTo(bison)) {
                                furthest = bison;
                            }
                        }
                    }
                    if (furthest != null && furthest != EntityBison.this) {
                        EntityBison.this.chargePartner = furthest;
                        furthest.chargePartner = EntityBison.this;
                        return true;
                    }
                }
            }
            return false;
        }

        @Override
        public boolean shouldContinue() {
            return EntityBison.this.isValidCharging() && EntityBison.this.chargePartner != null && EntityBison.this.chargePartner.isValidCharging() && !EntityBison.this.chargePartner.isPartOf(EntityBison.this);
        }

        @Override
        public void tick() {
            EntityBison.this.lookAtEntity(EntityBison.this.chargePartner, 30, 30);
            EntityBison.this.bodyYaw = EntityBison.this.getYaw();
            if (!EntityBison.this.isCharging()) {
                final Animation bisonAnimation = EntityBison.this.getAnimation();
                if (bisonAnimation == NO_ANIMATION || bisonAnimation == ANIMATION_PREPARE_CHARGE && EntityBison.this.getAnimationTick() > 35) {
                    EntityBison.this.setCharging(true);
                }
            } else {
                final float dist = EntityBison.this.distanceTo(EntityBison.this.chargePartner);
                EntityBison.this.getNavigation().startMovingTo(EntityBison.this.chargePartner, 1.0F);
                if (EntityBison.this.canSee(EntityBison.this.chargePartner)) {
                    final float flingAnimAt = EntityBison.this.getWidth() + 1.0F;
                    if (dist < flingAnimAt && EntityBison.this.getAnimation() == ANIMATION_ATTACK) {
                        if (EntityBison.this.getAnimationTick() > 8) {
                            boolean flag = false;
                            if (EntityBison.this.isOnGround()) {
                                EntityBison.this.pushBackJostling(EntityBison.this.chargePartner, 0.2F);
                                flag = true;
                            }
                            if (EntityBison.this.chargePartner.isOnGround()) {
                                EntityBison.this.chargePartner.pushBackJostling(EntityBison.this, 0.9F);
                                flag = true;
                            }
                            if (flag) {
                                EntityBison.this.resetChargeCooldown();
                            }
                        }
                    } else {
                        final float startFlingAnimAt = EntityBison.this.getWidth() + 3.0F;
                        if (dist < startFlingAnimAt && EntityBison.this.getAnimation() != ANIMATION_ATTACK) {
                            EntityBison.this.setAnimation(ANIMATION_ATTACK);
                        }
                    }
                }
            }
        }
    }

    class AIAttackNearPlayers extends ActiveTargetGoal<PlayerEntity> {
        public AIAttackNearPlayers() {
            super(EntityBison.this, PlayerEntity.class, 80, true, true, null);
        }

        @Override
        public boolean canStart() {
            if (EntityBison.this.isBaby() || EntityBison.this.isInLove()) {
                return false;
            } else {
                return super.canStart();
            }
        }

        @Override
        protected double getFollowRange() {
            return 3.0D;
        }
    }
}
