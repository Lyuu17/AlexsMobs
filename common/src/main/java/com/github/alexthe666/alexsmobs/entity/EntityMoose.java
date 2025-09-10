package com.github.alexthe666.alexsmobs.entity;

import com.github.alexthe666.alexsmobs.config.AMConfig;
import com.github.alexthe666.alexsmobs.entity.ai.AnimalAIHurtByTargetNotBaby;
import com.github.alexthe666.alexsmobs.entity.ai.AnimalAIPanicBaby;
import com.github.alexthe666.alexsmobs.entity.ai.AnimalAIWanderRanged;
import com.github.alexthe666.alexsmobs.entity.ai.MooseAIJostle;
import com.github.alexthe666.alexsmobs.registry.AMEntityRegistry;
import com.github.alexthe666.alexsmobs.registry.AMItemRegistry;
import com.github.alexthe666.alexsmobs.registry.AMSoundRegistry;
import com.github.alexthe666.alexsmobs.registry.AMTagRegistry;
import com.iafenvoy.uranus.animation.Animation;
import com.iafenvoy.uranus.animation.AnimationHandler;
import com.iafenvoy.uranus.animation.IAnimatedEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.goal.*;
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
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.ShovelItem;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.recipe.Ingredient;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.event.GameEvent;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

public class EntityMoose extends AnimalEntity implements IAnimatedEntity {

    public static final Animation ANIMATION_EAT_GRASS = Animation.create(30);
    public static final Animation ANIMATION_ATTACK = Animation.create(15);
    private static final int DAY = 24000;
    private static final TrackedData<Boolean> ANTLERED = DataTracker.registerData(EntityMoose.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> JOSTLING = DataTracker.registerData(EntityMoose.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Float> JOSTLE_ANGLE = DataTracker.registerData(EntityMoose.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Optional<UUID>> JOSTLER_UUID = DataTracker.registerData(EntityMoose.class, TrackedDataHandlerRegistry.OPTIONAL_UUID);
    private static final TrackedData<Boolean> SNOWY = DataTracker.registerData(EntityMoose.class, TrackedDataHandlerRegistry.BOOLEAN);
    public float prevJostleAngle;
    public float prevJostleProgress;
    public float jostleProgress;
    public boolean jostleDirection;
    public int jostleTimer = 0;
    public boolean instantlyTriggerJostleAI = false;
    public int jostleCooldown = 100 + random.nextInt(40);
    public int timeUntilAntlerDrop = 7 * DAY + this.random.nextInt(3) * DAY;
    private int animationTick;
    private Animation currentAnimation;
    private int snowTimer = 0;
    private boolean permSnow = false;

    public EntityMoose(EntityType<EntityMoose> type, World worldIn) {
        super(type, worldIn);
        this.setStepHeight(1.1F);
    }

    public static boolean canMooseSpawn(EntityType<? extends MobEntity> typeIn, ServerWorldAccess worldIn, SpawnReason reason, BlockPos pos, Random randomIn) {
        var blockstate = worldIn.getBlockState(pos.down());
        return (blockstate.isOf(Blocks.GRASS_BLOCK) || blockstate.isOf(Blocks.SNOW)) || blockstate.isOf(Blocks.SNOW_BLOCK) && worldIn.getBaseLightLevel(pos, 0) > 8;
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createLivingAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 55D)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 7.5D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.25F)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.5F)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32.0D);
    }

    @Override
    public boolean canSpawn(WorldAccess worldIn, SpawnReason spawnReasonIn) {
        return AMEntityRegistry.rollSpawn(AMConfig.mooseSpawnRolls, this.getRandom(), spawnReasonIn);
    }

    @Override
    protected float getBaseMovementSpeedMultiplier() {
        return 0.98F;
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(1, new SwimGoal(this));
        this.goalSelector.add(2, new MooseAIJostle(this));
        this.goalSelector.add(3, new AnimalAIPanicBaby(this, 1.25D));
        this.goalSelector.add(4, new MeleeAttackGoal(this, 1.1D, true));
        this.goalSelector.add(5, new AnimalMateGoal(this, 1.0D));
        this.goalSelector.add(6, new FollowParentGoal(this, 1.1D));
        this.goalSelector.add(7, new TemptGoal(this, 1.1D, Ingredient.fromTag(AMTagRegistry.MOOSE_BREEDABLES), false));
        this.goalSelector.add(7, new AnimalAIWanderRanged(this, 120, 1.0D, 14, 7));
        this.goalSelector.add(8, new LookAtEntityGoal(this, PlayerEntity.class, 15.0F));
        this.goalSelector.add(8, new LookAroundGoal(this));
        this.targetSelector.add(1, (new AnimalAIHurtByTargetNotBaby(this)));
    }

    @Environment(EnvType.CLIENT)
    public void handleStatus(byte id) {
        if (id == 6) {
            for (int lvt_3_1_ = 0; lvt_3_1_ < 7; ++lvt_3_1_) {
                double lvt_4_1_ = this.random.nextGaussian() * 0.02D;
                double lvt_6_1_ = this.random.nextGaussian() * 0.02D;
                double lvt_8_1_ = this.random.nextGaussian() * 0.02D;
                this.getWorld().addParticle(ParticleTypes.SMOKE, this.getParticleX(1.0D), this.getRandomBodyY() + 0.5D, this.getParticleZ(1.0D), lvt_4_1_, lvt_6_1_, lvt_8_1_);
            }
        } else {
            super.handleStatus(id);
        }
    }

    @Override
    public boolean isBreedingItem(ItemStack stack) {
        if (stack.isIn(AMTagRegistry.MOOSE_BREEDABLES) && !this.isInLove() && this.getBreedingAge() == 0) {
            if (this.getRandom().nextInt(5) == 0) {
                return true;
            } else {
                this.getWorld().sendEntityStatus(this, (byte) 6);
                return false;
            }
        }
        return false;
    }

    @Override
    public void setTarget(@Nullable LivingEntity entitylivingbaseIn) {
        if (!this.isBaby()) {
            super.setTarget(entitylivingbaseIn);
        }
    }

    @Override
    public boolean tryAttack(Entity entityIn) {
        if (this.getAnimation() == NO_ANIMATION) {
            this.setAnimation(ANIMATION_ATTACK);
        }
        return true;
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(ANTLERED, true);
        this.dataTracker.startTracking(JOSTLING, false);
        this.dataTracker.startTracking(SNOWY, false);
        this.dataTracker.startTracking(JOSTLE_ANGLE, 0F);
        this.dataTracker.startTracking(JOSTLER_UUID, Optional.empty());
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound compound) {
        super.readCustomDataFromNbt(compound);
        this.setSnowy(compound.getBoolean("Snowy"));
        if (compound.contains("AntlerTime")) {
            this.timeUntilAntlerDrop = compound.getInt("AntlerTime");
        }
        this.setAntlered(compound.getBoolean("Antlered"));
        this.jostleCooldown = compound.getInt("JostlingCooldown");
        this.permSnow = compound.getBoolean("SnowPerm");

    }

    @Override
    public void writeCustomDataToNbt(NbtCompound compound) {
        super.writeCustomDataToNbt(compound);
        compound.putBoolean("Snowy", this.isSnowy());
        compound.putBoolean("SnowPerm", this.permSnow);
        compound.putInt("AntlerTime", this.timeUntilAntlerDrop);
        compound.putBoolean("Antlered", this.isAntlered());
        compound.putInt("JostlingCooldown", this.jostleCooldown);
    }

    @Override
    public void tick() {
        super.tick();
        prevJostleProgress = jostleProgress;
        prevJostleAngle = this.getJostleAngle();

        if (this.isJostling()) {
            if (jostleProgress < 5F)
                jostleProgress++;
        } else {
            if (jostleProgress > 0F)
                jostleProgress--;
        }

        if (jostleCooldown > 0) {
            jostleCooldown--;
        }
        if (!this.getWorld().isClient && this.getAnimation() == NO_ANIMATION && getRandom().nextInt(120) == 0 && (this.getTarget() == null || !this.getTarget().isAlive()) && !this.isJostling() && this.getJostlingPartnerUUID() == null) {
            if (getWorld().getBlockState(this.getBlockPos().down()).isOf(Blocks.GRASS_BLOCK) && getRandom().nextInt(3) == 0) {
                this.setAnimation(ANIMATION_EAT_GRASS);
            }
        }
        if (timeUntilAntlerDrop > 0) {
            timeUntilAntlerDrop--;
        }
        if (timeUntilAntlerDrop == 0) {
            if (this.isAntlered()) {
                this.setAntlered(false);
                this.dropStack(new ItemStack(AMItemRegistry.MOOSE_ANTLER.get()));
                timeUntilAntlerDrop = 2 * DAY + this.random.nextInt(3) * DAY;
            } else {
                this.setAntlered(true);
                timeUntilAntlerDrop = 7 * DAY + this.random.nextInt(3) * DAY;
            }
        }
        if (this.getTarget() != null && this.getTarget().isAlive()) {
            if (this.isJostling()) {
                this.setJostling(false);
            }
            if (!this.getWorld().isClient && this.getAnimation() == ANIMATION_ATTACK && this.getAnimationTick() == 8) {
                float dmg = (float) this.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE).getBaseValue();
                if (!isAntlered()) {
                    dmg = 3;
                }
                if (this.getTarget() instanceof WolfEntity || this.getTarget() instanceof EntityOrca) {
                    dmg = 2;
                }
                getTarget().takeKnockback(1F, getTarget().getX() - this.getX(), getTarget().getZ() - this.getZ());
                this.getTarget().damage(this.getDamageSources().mobAttack(this), dmg);
            }
        }
        if(snowTimer > 0){
            snowTimer--;
        }
        if (snowTimer == 0 && !this.getWorld().isClient) {
            snowTimer = 200 + random.nextInt(400);
            if(this.isSnowy()){
                if(!permSnow){
                    if (!this.getWorld().isClient || this.getFireTicks() > 0 || this.isInsideWaterOrBubbleColumn() || !EntityGrizzlyBear.isSnowingAt(getWorld(), this.getBlockPos().up())) {
                        this.setSnowy(false);
                    }
                }
            }else{
                if (!this.getWorld().isClient && EntityGrizzlyBear.isSnowingAt(getWorld(), this.getBlockPos())) {
                    this.setSnowy(true);
                }
            }
        }
        AnimationHandler.INSTANCE.updateAnimations(this);
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        if (this.isInvulnerableTo(source)) {
            return false;
        } else {
            var entity = source.getAttacker();
            if (entity instanceof EntityOrca || entity instanceof WolfEntity) {
                amount = (amount + 1.0F) * 3.0F;
            }
            return super.damage(source, amount);
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return AMSoundRegistry.MOOSE_IDLE.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return AMSoundRegistry.MOOSE_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return AMSoundRegistry.MOOSE_HURT.get();
    }

    public boolean isAntlered() {
        return this.dataTracker.get(ANTLERED);
    }

    public void setAntlered(boolean anters) {
        this.dataTracker.set(ANTLERED, anters);
    }

    public boolean isJostling() {
        return this.dataTracker.get(JOSTLING);
    }

    public void setJostling(boolean jostle) {
        this.dataTracker.set(JOSTLING, jostle);
    }

    public float getJostleAngle() {
        return this.dataTracker.get(JOSTLE_ANGLE);
    }

    public void setJostleAngle(float scale) {
        this.dataTracker.set(JOSTLE_ANGLE, scale);
    }

    @Nullable
    public UUID getJostlingPartnerUUID() {
        return this.dataTracker.get(JOSTLER_UUID).orElse(null);
    }

    public void setJostlingPartnerUUID(@Nullable UUID uniqueId) {
        this.dataTracker.set(JOSTLER_UUID, Optional.ofNullable(uniqueId));
    }

    public boolean isSnowy() {
        return this.dataTracker.get(SNOWY);
    }

    public void setSnowy(boolean honeyed) {
        this.dataTracker.set(SNOWY, honeyed);
    }

    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        ItemStack itemstack = player.getStackInHand(hand);
        Item item = itemstack.getItem();
        ActionResult type = super.interactMob(player, hand);
        if (item == Items.SNOW && !this.isSnowy() && !this.getWorld().isClient) {
            this.eat(player, hand, itemstack);
            this.permSnow = true;
            this.setSnowy(true);
            this.emitGameEvent(GameEvent.ENTITY_INTERACT);
            this.playSound(SoundEvents.BLOCK_SNOW_PLACE, this.getSoundVolume(), this.getSoundPitch());
            return ActionResult.SUCCESS;
        }
        if (item instanceof ShovelItem && this.isSnowy() && !this.getWorld().isClient) {
            this.permSnow = false;
            if (!player.isCreative()) {
                itemstack.damage(1, this.getRandom(), player instanceof ServerPlayerEntity ? (ServerPlayerEntity) player : null);
            }
            this.setSnowy(false);
            this.emitGameEvent(GameEvent.ENTITY_INTERACT);
            this.playSound(SoundEvents.BLOCK_SNOW_BREAK, this.getSoundVolume(), this.getSoundPitch());
            return ActionResult.SUCCESS;
        }
        return type;
    }

    @Nullable
    public Entity getJostlingPartner() {
        UUID id = getJostlingPartnerUUID();
        if (id != null && !this.getWorld().isClient) {
            return ((ServerWorld) getWorld()).getEntity(id);
        }
        return null;
    }

    public void setJostlingPartner(@Nullable Entity jostlingPartner) {
        if (jostlingPartner == null) {
            this.setJostlingPartnerUUID(null);
        } else {
            this.setJostlingPartnerUUID(jostlingPartner.getUuid());
        }
    }

    public void pushBackJostling(EntityMoose entityMoose, float strength) {
        applyKnockbackFromMoose(strength, entityMoose.getX() - this.getX(), entityMoose.getZ() - this.getZ());
    }

    private void applyKnockbackFromMoose(float strength, double ratioX, double ratioZ) {
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
        return new Animation[]{ANIMATION_ATTACK, ANIMATION_EAT_GRASS};
    }

    @Nullable
    @Override
    public PassiveEntity createChild(ServerWorld world, PassiveEntity entity) {
        return AMEntityRegistry.MOOSE.get().create(world);
    }

    public boolean canJostleWith(EntityMoose moose) {
        return !moose.isJostling() && moose.isAntlered() && moose.getAnimation() == NO_ANIMATION && !moose.isBaby() && moose.getJostlingPartnerUUID() == null && moose.jostleCooldown == 0;
    }

    public void playJostleSound() {
        this.playSound(AMSoundRegistry.MOOSE_JOSTLE.get(), this.getSoundPitch(), this.getSoundVolume());
    }

}
