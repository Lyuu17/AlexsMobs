package com.github.alexthe666.alexsmobs.entity;

import com.github.alexthe666.alexsmobs.config.AMConfig;
import com.github.alexthe666.alexsmobs.entity.ai.AnimalAIWanderRanged;
import com.github.alexthe666.alexsmobs.entity.ai.DirectPathNavigator;
import com.github.alexthe666.alexsmobs.entity.ai.MimiCubeAIRangedAttack;
import com.github.alexthe666.alexsmobs.registry.AMEntityRegistry;
import com.github.alexthe666.alexsmobs.registry.AMItemRegistry;
import com.github.alexthe666.alexsmobs.registry.AMSoundRegistry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.RangedAttackMob;
import net.minecraft.entity.ai.control.MoveControl;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.RangedWeaponItem;
import net.minecraft.item.TridentItem;
import net.minecraft.particle.ItemStackParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.UseAction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Difficulty;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.event.GameEvent;

public class EntityMimicube extends HostileEntity implements RangedAttackMob {

    private static final TrackedData<Integer> ATTACK_TICK = DataTracker.registerData(EntityMimicube.class, TrackedDataHandlerRegistry.INTEGER);
    private final MimiCubeAIRangedAttack aiArrowAttack = new MimiCubeAIRangedAttack(this, 1.0D, 10, 15.0F);
    private final MeleeAttackGoal aiAttackOnCollide = new MeleeAttackGoal(this, 1.2D, false);
    public float squishAmount;
    public float squishFactor;
    public float prevSquishFactor;
    public float leftSwapProgress = 0;
    public float prevLeftSwapProgress = 0;
    public float rightSwapProgress = 0;
    public float prevRightSwapProgress = 0;
    public float helmetSwapProgress = 0;
    public float prevHelmetSwapProgress = 0;
    public float prevAttackProgress;
    public float attackProgress;
    private boolean wasOnGround;
    private int eatingTicks;

    public EntityMimicube(EntityType<EntityMimicube> type, World world) {
        super(type, world);
        this.moveControl = new MimicubeMoveHelper(this);
        this.navigation = new DirectPathNavigator(this, world);
        this.setCombatTask();
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createLivingAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 30.0D)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32.0D)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 2.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.45F);
    }

    @Override
    public boolean canSpawn(WorldAccess worldIn, SpawnReason spawnReasonIn) {
        return AMEntityRegistry.rollSpawn(AMConfig.mimicubeSpawnRolls, this.getRandom(), spawnReasonIn);
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(ATTACK_TICK, 0);

    }

    @Override
    public boolean tryAttack(Entity entityIn) {
        this.dataTracker.set(ATTACK_TICK, 5);
        return true;
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(1, new AnimalAIWanderRanged(this, 60, 1.0D, 10, 7));
        this.goalSelector.add(2, new LookAtEntityGoal(this, PlayerEntity.class, 8.0F));
        this.goalSelector.add(2, new LookAroundGoal(this));
        this.targetSelector.add(1, new RevengeGoal(this));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
        this.targetSelector.add(3, new ActiveTargetGoal<>(this, MerchantEntity.class, true));
    }

    public void setCombatTask() {
        if (this.getWorld() != null && !this.getWorld().isClient) {
            this.goalSelector.remove(this.aiAttackOnCollide);
            this.goalSelector.remove(this.aiArrowAttack);
            ItemStack itemstack = this.getMainHandStack();
            if (itemstack.getItem() instanceof RangedWeaponItem || itemstack.getItem() instanceof TridentItem) {
                int i = 10;
                if (this.getWorld().getDifficulty() != Difficulty.HARD) {
                    i = 30;
                }

                this.aiArrowAttack.setAttackCooldown(i);
                this.goalSelector.add(4, this.aiArrowAttack);
            } else {
                this.goalSelector.add(4, this.aiAttackOnCollide);
            }

        }
    }

    public void attackEntityWithRangedAttackTrident(LivingEntity target, float distanceFactor) {
        var tridententity = new TridentEntity(this.getWorld(), this, new ItemStack(Items.TRIDENT));
        double d0 = target.getX() - this.getX();
        double d1 = target.getBodyY(0.3333333333333333D) - tridententity.getY();
        double d2 = target.getZ() - this.getZ();
        double d3 = MathHelper.sqrt((float)(d0 * d0 + d2 * d2));
        tridententity.setVelocity(d0, d1 + d3 * (double) 0.2F, d2, 1.6F, (float) (14 - this.getWorld().getDifficulty().getId() * 4));
        this.emitGameEvent(GameEvent.PROJECTILE_SHOOT);
        this.playSound(SoundEvents.ENTITY_DROWNED_SHOOT, 1.0F, 1.0F / (this.getRandom().nextFloat() * 0.4F + 0.8F));
        this.getWorld().spawnEntity(tridententity);
    }

    @Override
    public void attack(LivingEntity target, float distanceFactor) {
        if (this.getMainHandStack().getItem() instanceof TridentItem) {
            attackEntityWithRangedAttackTrident(target, distanceFactor);
            return;
        }
        ItemStack itemstack = this.getProjectileType(this.getMainHandStack());
        PersistentProjectileEntity abstractarrowentity = this.fireArrow(itemstack, distanceFactor);
        //FIXME forge
//        if (this.getMainHandStack().getItem() instanceof BowItem)
//            abstractarrowentity = ((BowItem) this.getMainHandStack().getItem()).customArrow(abstractarrowentity);
        double d0 = target.getX() - this.getX();
        double d1 = target.getBodyY(0.3333333333333333D) - abstractarrowentity.getY();
        double d2 = target.getZ() - this.getZ();
        double d3 = MathHelper.sqrt((float)(d0 * d0 + d2 * d2));
        abstractarrowentity.setVelocity(d0, d1 + d3 * (double) 0.2F, d2, 1.6F, (float) (14 - this.getWorld().getDifficulty().getId() * 4));
        this.emitGameEvent(GameEvent.PROJECTILE_SHOOT);
        this.playSound(SoundEvents.ENTITY_SKELETON_SHOOT, 1.0F, 1.0F / (this.getRandom().nextFloat() * 0.4F + 0.8F));
        this.getWorld().spawnEntity(abstractarrowentity);
    }

    protected PersistentProjectileEntity fireArrow(ItemStack arrowStack, float distanceFactor) {
        return ProjectileUtil.createArrowProjectile(this, arrowStack, distanceFactor);
    }

    @Override
    public boolean canUseRangedWeapon(RangedWeaponItem p_230280_1_) {
        return p_230280_1_ == Items.BOW;
    }

    @Override
    public void equipStack(EquipmentSlot slotIn, ItemStack stack) {
        switch (slotIn) {
            case HEAD -> {
                if (!ItemStack.areItemsEqual(stack, this.getEquippedStack(EquipmentSlot.HEAD))) {
                    helmetSwapProgress = 5;
                    this.getWorld().sendEntityStatus(this, (byte) 45);
                }
            }
            case MAINHAND -> {
                if (!ItemStack.areItemsEqual(stack, this.getEquippedStack(EquipmentSlot.MAINHAND))) {
                    rightSwapProgress = 5;
                    this.getWorld().sendEntityStatus(this, (byte) 46);
                }
            }
            case OFFHAND -> {
                if (!ItemStack.areItemsEqual(stack, this.getEquippedStack(EquipmentSlot.OFFHAND))) {
                    leftSwapProgress = 5;
                    this.getWorld().sendEntityStatus(this, (byte) 47);
                }
            }
        }
        super.equipStack(slotIn, stack);
        if (!this.getWorld().isClient) {
            this.setCombatTask();
        }
    }

    @Environment(EnvType.CLIENT)
    @Override
    public void handleStatus(byte id) {
        super.handleStatus(id);
        switch (id) {
            case 45 -> helmetSwapProgress = 5;
            case 46 -> rightSwapProgress = 5;
            case 47 -> leftSwapProgress = 5;
            default -> {}
        }
    }

    public boolean isBlocking() {
        return this.getMainHandStack().getUseAction().equals(UseAction.BLOCK) || this.getOffHandStack().getUseAction().equals(UseAction.BLOCK);
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        var trueSource = source.getAttacker();
        if (trueSource instanceof LivingEntity attacker) {
            if (!attacker.getEquippedStack(EquipmentSlot.HEAD).isEmpty()) {
                this.equipStack(EquipmentSlot.HEAD, mimicStack(attacker.getEquippedStack(EquipmentSlot.HEAD)));
            }
            if (!attacker.getEquippedStack(EquipmentSlot.OFFHAND).isEmpty()) {
                this.equipStack(EquipmentSlot.OFFHAND, mimicStack(attacker.getEquippedStack(EquipmentSlot.OFFHAND)));
            }
            if (!attacker.getEquippedStack(EquipmentSlot.MAINHAND).isEmpty()) {
                this.equipStack(EquipmentSlot.MAINHAND, mimicStack(attacker.getEquippedStack(EquipmentSlot.MAINHAND)));
            }
        }
        return super.damage(source, amount);
    }

    private ItemStack mimicStack(ItemStack stack){
        ItemStack copy = stack.copy();
        if(copy.isDamageable()){
            copy.setDamage(copy.getMaxDamage());
        }
        return copy;
    }

    @Override
    public void tick() {
        super.tick();
        this.squishFactor += (this.squishAmount - this.squishFactor) * 0.5F;
        this.prevSquishFactor = this.squishFactor;
        this.prevHelmetSwapProgress = this.helmetSwapProgress;
        this.prevRightSwapProgress = this.rightSwapProgress;
        this.prevLeftSwapProgress = this.leftSwapProgress;
        this.prevAttackProgress = attackProgress;
        if (rightSwapProgress > 0F) {
            rightSwapProgress -= 0.5F;
        }
        if (leftSwapProgress > 0F) {
            leftSwapProgress -= 0.5F;
        }
        if (helmetSwapProgress > 0F) {
            helmetSwapProgress -= 0.5F;
        }
        if (this.isOnGround() && !this.wasOnGround) {

            for (int j = 0; j < 8; ++j) {
                float f = this.random.nextFloat() * MathHelper.TAU;
                float f1 = this.random.nextFloat() * 0.5F + 0.5F;
                float f2 = MathHelper.sin(f) * 0.5F * f1;
                float f3 = MathHelper.cos(f) * 0.5F * f1;
                this.getWorld().addParticle(new ItemStackParticleEffect(ParticleTypes.ITEM, new ItemStack(AMItemRegistry.MIMICREAM.get())), this.getX() + (double)f2, this.getY(), this.getZ() + (double)f3, 0.0D, 0.0D, 0.0D);
            }

            this.playSound(this.getSquishSound(), this.getSoundVolume(), ((this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F) / 0.8F);
            this.squishAmount = -0.35F;
        } else if (!this.isOnGround() && this.wasOnGround) {
            this.squishAmount = 2F;
        }
        if(this.isTouchingWater()){
            this.setVelocity(this.getVelocity().add(0, 0.05D, 0));
        }
        if (this.getOffHandStack().getItem().isFood() && this.getHealth() < this.getMaxHealth()) {
            if (eatingTicks < 100) {
                for (int i = 0; i < 3; i++) {
                    double d2 = this.random.nextGaussian() * 0.02D;
                    double d0 = this.random.nextGaussian() * 0.02D;
                    double d1 = this.random.nextGaussian() * 0.02D;
                    this.getWorld().addParticle(new ItemStackParticleEffect(ParticleTypes.ITEM, this.getStackInHand(Hand.OFF_HAND)), this.getX() + (double) (this.random.nextFloat() * this.getWidth()) - (double) this.getWidth() * 0.5F, this.getY() + this.getHeight() * 0.5F + (double) (this.random.nextFloat() * this.getHeight() * 0.5F), this.getZ() + (double) (this.random.nextFloat() * this.getWidth()) - (double) this.getWidth() * 0.5F, d0, d1, d2);
                }
                if (eatingTicks % 6 == 0) {
                    this.emitGameEvent(GameEvent.EAT);
                    this.playSound(SoundEvents.ENTITY_GENERIC_EAT, this.getSoundVolume(), this.getSoundPitch());
                }
                eatingTicks++;
            }
            if (eatingTicks == 100) {
                this.emitGameEvent(GameEvent.EAT);
                this.playSound(SoundEvents.ENTITY_PLAYER_BURP, this.getSoundVolume(), this.getSoundPitch());
                this.getOffHandStack().decrement(1);
                this.heal(5);
                eatingTicks = 0;
            }
        } else if (this.getMainHandStack().getItem().isFood() && this.getHealth() < this.getMaxHealth()) {
            if (eatingTicks < 100) {
                for (int i = 0; i < 3; i++) {
                    double d2 = this.random.nextGaussian() * 0.02D;
                    double d0 = this.random.nextGaussian() * 0.02D;
                    double d1 = this.random.nextGaussian() * 0.02D;
                    this.getWorld().addParticle(new ItemStackParticleEffect(ParticleTypes.ITEM, this.getStackInHand(Hand.MAIN_HAND)), this.getX() + (double) (this.random.nextFloat() * this.getWidth()) - (double) this.getWidth() * 0.5F, this.getY() + this.getHeight() * 0.5F + (double) (this.random.nextFloat() * this.getHeight() * 0.5F), this.getZ() + (double) (this.random.nextFloat() * this.getWidth()) - (double) this.getWidth() * 0.5F, d0, d1, d2);
                }
                this.emitGameEvent(GameEvent.EAT);
                this.playSound(SoundEvents.ENTITY_GENERIC_EAT, this.getSoundVolume(), this.getSoundPitch());
                if (eatingTicks % 6 == 0) {
                    this.emitGameEvent(GameEvent.EAT);
                    this.playSound(SoundEvents.ENTITY_GENERIC_EAT, this.getSoundVolume(), this.getSoundPitch());
                }
                eatingTicks++;
            }
            if (eatingTicks == 100) {
                this.emitGameEvent(GameEvent.EAT);
                this.playSound(SoundEvents.ENTITY_PLAYER_BURP, this.getSoundVolume(), this.getSoundPitch());
                this.getMainHandStack().decrement(1);
                this.heal(5);
            }
        } else {
            eatingTicks = 0;
        }
        this.wasOnGround = this.isOnGround();
        this.alterSquishAmount();
        LivingEntity livingentity = this.getTarget();
        if (livingentity != null && this.squaredDistanceTo(livingentity) < 144D) {
            this.moveControl.moveTo(livingentity.getX(), livingentity.getY(), livingentity.getZ(), this.moveControl.getSpeed());
            this.wasOnGround = true;
        }
        if (this.dataTracker.get(ATTACK_TICK) > 0) {
            if (this.dataTracker.get(ATTACK_TICK) == 2 && this.getTarget() != null && this.distanceTo(this.getTarget()) < 2.3D) {
                super.tryAttack(this.getTarget());
            }
            this.dataTracker.set(ATTACK_TICK, this.dataTracker.get(ATTACK_TICK) - 1);
            if (attackProgress < 3F) {
                attackProgress++;
            }
        } else {
            if (attackProgress > 0F) {
                attackProgress--;
            }
        }

    }

    @Override
    protected float getDropChance(EquipmentSlot slotIn) {
        return 0;
    }

    private SoundEvent getSquishSound() {
        return AMSoundRegistry.MIMICUBE_JUMP.get();
    }

    private SoundEvent getJumpSound() {
        return AMSoundRegistry.MIMICUBE_JUMP.get();
    }

    @Override
    protected void jump() {
        Vec3d vector3d = this.getVelocity();
        this.setVelocity(vector3d.x, this.getJumpVelocity(), vector3d.z);
        this.velocityDirty = true;
    }

    protected int getJumpDelay() {
        return this.random.nextInt(20) + 10;
    }

    protected void alterSquishAmount() {
        this.squishAmount *= 0.6F;
    }

    public boolean shouldShoot() {
        return this.getMainHandStack().getItem() instanceof RangedWeaponItem || this.getMainHandStack().getItem() instanceof TridentItem;
    }

    private static class MimicubeMoveHelper extends MoveControl {
        private final EntityMimicube slime;
        private float yRot;
        private int jumpDelay;
        private boolean isAggressive;

        public MimicubeMoveHelper(EntityMimicube slimeIn) {
            super(slimeIn);
            this.slime = slimeIn;
            this.yRot = 180.0F * slimeIn.getYaw() / MathHelper.PI;
        }

        public void setDirection(float yRotIn, boolean aggressive) {
            this.yRot = yRotIn;
            this.isAggressive = aggressive;
        }

        public void setSpeed(double speedIn) {
            this.speed = speedIn;
            this.state = MoveControl.State.MOVE_TO;
        }

        @Override
        public void tick() {
            if (this.entity.isOnGround()) {
                this.entity.setMovementSpeed((float) (this.speed * this.entity.getAttributeValue(EntityAttributes.GENERIC_MOVEMENT_SPEED)));
                if (this.jumpDelay-- <= 0 && this.state != State.WAIT) {
                    this.jumpDelay = this.slime.getJumpDelay();
                    if (this.entity.getTarget() != null) {
                        this.jumpDelay /= 3;
                    }

                    this.slime.getJumpControl().setActive();
                    this.slime.playSound(this.slime.getJumpSound(), this.slime.getSoundVolume(), this.slime.getSoundPitch());
                } else {
                    this.slime.sidewaysSpeed = 0.0F;
                    this.slime.forwardSpeed = 0.0F;
                    this.entity.setMovementSpeed(0.0F);
                }
            }
            super.tick();
        }
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return AMSoundRegistry.MIMICUBE_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return AMSoundRegistry.MIMICUBE_HURT.get();
    }

}

