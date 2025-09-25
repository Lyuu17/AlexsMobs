package com.github.alexthe666.alexsmobs.entity;

import com.github.alexthe666.alexsmobs.config.AMConfig;
import com.github.alexthe666.alexsmobs.entity.ai.*;
import com.github.alexthe666.alexsmobs.entity.util.Maths;
import com.github.alexthe666.alexsmobs.misc.ModTagsCompat;
import com.github.alexthe666.alexsmobs.registry.AMAdvancementTriggerRegistry;
import com.github.alexthe666.alexsmobs.registry.AMEntityRegistry;
import com.github.alexthe666.alexsmobs.registry.AMSoundRegistry;
import com.github.alexthe666.alexsmobs.registry.AMTagRegistry;
import com.google.common.collect.Maps;
import com.iafenvoy.uranus.animation.Animation;
import com.iafenvoy.uranus.animation.AnimationHandler;
import com.iafenvoy.uranus.animation.IAnimatedEntity;
import dev.architectury.registry.menu.ExtendedMenuProvider;
import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.DyedCarpetBlock;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.FuzzyTargeting;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.loot.context.LootContextParameterSet;
import net.minecraft.loot.context.LootContextTypes;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ItemStackParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.recipe.Ingredient;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.*;
import net.minecraft.world.event.GameEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class EntityElephant extends TameableEntity implements ITargetsDroppedItems, IAnimatedEntity {

    public static final Animation ANIMATION_TRUMPET_0 = Animation.create(20);
    public static final Animation ANIMATION_TRUMPET_1 = Animation.create(30);
    public static final Animation ANIMATION_CHARGE_PREPARE = Animation.create(25);
    public static final Animation ANIMATION_STOMP = Animation.create(20);
    public static final Animation ANIMATION_FLING = Animation.create(25);
    public static final Animation ANIMATION_EAT = Animation.create(30);
    public static final Animation ANIMATION_BREAKLEAVES = Animation.create(20);
    protected static final EntityDimensions TUSKED_SIZE = EntityDimensions.fixed(3.7F, 3.75F);
    private static final TrackedData<Boolean> TUSKED = DataTracker.registerData(EntityElephant.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> SITTING = DataTracker.registerData(EntityElephant.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> STANDING = DataTracker.registerData(EntityElephant.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> CHESTED = DataTracker.registerData(EntityElephant.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Integer> CARPET_COLOR = DataTracker.registerData(EntityElephant.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Boolean> TRADER = DataTracker.registerData(EntityElephant.class, TrackedDataHandlerRegistry.BOOLEAN);
    public static final Map<DyeColor, Item> DYE_COLOR_ITEM_MAP = Util.make(Maps.newHashMap(), (map) -> {
        map.put(DyeColor.WHITE, Items.WHITE_CARPET);
        map.put(DyeColor.ORANGE, Items.ORANGE_CARPET);
        map.put(DyeColor.MAGENTA, Items.MAGENTA_CARPET);
        map.put(DyeColor.LIGHT_BLUE, Items.LIGHT_BLUE_CARPET);
        map.put(DyeColor.YELLOW, Items.YELLOW_CARPET);
        map.put(DyeColor.LIME, Items.LIME_CARPET);
        map.put(DyeColor.PINK, Items.PINK_CARPET);
        map.put(DyeColor.GRAY, Items.GRAY_CARPET);
        map.put(DyeColor.LIGHT_GRAY, Items.LIGHT_GRAY_CARPET);
        map.put(DyeColor.CYAN, Items.CYAN_CARPET);
        map.put(DyeColor.PURPLE, Items.PURPLE_CARPET);
        map.put(DyeColor.BLUE, Items.BLUE_CARPET);
        map.put(DyeColor.BROWN, Items.BROWN_CARPET);
        map.put(DyeColor.GREEN, Items.GREEN_CARPET);
        map.put(DyeColor.RED, Items.RED_CARPET);
        map.put(DyeColor.BLACK, Items.BLACK_CARPET);
    });
    private static final Identifier TRADER_LOOT = new Identifier("alexsmobs", "gameplay/trader_elephant_chest");
    public boolean forcedSit = false;
    public float prevSitProgress;
    public float sitProgress;
    public float prevStandProgress;
    public float standProgress;
    public int maxStandTime = 75;
    public boolean aiItemFlag = false;
    public SimpleInventory elephantInventory;
    private int animationTick;
    private Animation currentAnimation;
    private final boolean hasTuskedAttributes = false;
    private int standingTime = 0;
    @Nullable
    private EntityElephant caravanHead;
    @Nullable
    private EntityElephant caravanTail;
    private boolean hasChestVarChanged = false;
    private boolean hasChargedSpeed = false;
    private boolean charging;
    private int chargeCooldown = 0;
    private int chargingTicks = 0;
    @Nullable
    private UUID blossomThrowerUUID = null;
    private int despawnDelay = 47999;

    public EntityElephant(EntityType<? extends EntityElephant> type, World world) {
        super(type, world);
        initElephantInventory();
        this.setStepHeight(1.1F);
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createLivingAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 85.0D)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32.0D)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.9F)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 10.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.35F);
    }

    @Nullable
    public static DyeColor getCarpetColor(ItemStack stack) {
        var lvt_1_1_ = Block.getBlockFromItem(stack.getItem());
        return lvt_1_1_ instanceof DyedCarpetBlock ? ((DyedCarpetBlock) lvt_1_1_).getDyeColor() : null;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return AMSoundRegistry.ELEPHANT_IDLE.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return AMSoundRegistry.ELEPHANT_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return AMSoundRegistry.ELEPHANT_DIE.get();
    }

    @Override
    public boolean canSpawn(WorldAccess worldIn, SpawnReason spawnReasonIn) {
        return AMEntityRegistry.rollSpawn(AMConfig.elephantSpawnRolls, this.getRandom(), spawnReasonIn);
    }

    private void initElephantInventory() {
        var animalchest = this.elephantInventory;
        this.elephantInventory = new SimpleInventory(54){
            @Override
            public boolean canPlayerUse(PlayerEntity player) {
                return EntityElephant.this.isAlive() && !EntityElephant.this.inNetherPortal;
            }
        };
        if (animalchest != null) {
            int i = Math.min(animalchest.size(), this.elephantInventory.size());
            for (int j = 0; j < i; ++j) {
                var itemstack = animalchest.getStack(j);
                if (!itemstack.isEmpty()) {
                    this.elephantInventory.setStack(j, itemstack.copy());
                }
            }
        }
    }

    @Override
    protected EntityNavigation createNavigation(World worldIn) {
        return new AdvancedPathNavigateNoTeleport(this, worldIn, true);
    }

    @Override
    public int getMaxHeadRotation() {
        return super.getMaxHeadRotation();
    }

    @Override
    protected boolean isImmobile() {
        return super.isImmobile() || this.isSitting() || this.getAnimation() == ANIMATION_CHARGE_PREPARE && this.getAnimationTick() < 10;
    }

    @Override
    protected void initGoals() {
        super.initGoals();
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new SitGoal(this));
        this.goalSelector.add(2, new MeleeAttackGoal(this, 1D, true));
        this.goalSelector.add(2, new MyEscapeDangerGoal());
        this.goalSelector.add(2, new ElephantAIVillagerRide(this, 1D));
        this.goalSelector.add(3, new AnimalMateGoal(this, 1.0D));
        this.goalSelector.add(4, new TemptGoal(this, 1.0D, Ingredient.fromTag(AMTagRegistry.ELEPHANT_TAMEABLES), false));
        this.goalSelector.add(5, new ElephantAIForageLeaves(this));
        this.goalSelector.add(6, new FollowParentGoal(this, 1D));
        this.goalSelector.add(7, new ElephantAIFollowCaravan(this, 0.5D));
        this.goalSelector.add(8, new FleeEntityGoal<>(this, BeeEntity.class, 6.0F, 1.0D, 1.2D));
        this.goalSelector.add(9, new EntityElephant.AIWalkIdle(this, 0.5D));
        this.targetSelector.add(1, new MyRevengeGoal().setGroupRevenge());
        this.targetSelector.add(2, new TrackOwnerAttackerGoal(this));
        this.targetSelector.add(3, new AttackWithOwnerGoal(this));
        this.targetSelector.add(4, new CreatureAITargetItems<>(this, false));
    }

    @Override
    public boolean isBreedingItem(ItemStack stack) {
        var item = stack.getItem();
        return isTamed() && stack.isIn(AMTagRegistry.ELEPHANT_BREEDABLES);
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        if (!isBaby()) {
            this.playSound(AMSoundRegistry.ELEPHANT_WALK.get(), 0.2F, 1.0F);
        } else {
            super.playStepSound(pos, state);
        }
    }

    @Nullable
    @Override
    public LivingEntity getControllingPassenger() {
        for (Entity passenger : this.getPassengerList()) {
            if (passenger instanceof PlayerEntity) {
                return (LivingEntity) passenger;
            }
        }
        return null;
    }

    @Nullable
    public MerchantEntity getControllingVillager() {
        for (Entity passenger : this.getPassengerList()) {
            if (passenger instanceof MerchantEntity) {
                return (MerchantEntity) passenger;
            }
        }
        return null;
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(TUSKED, false);
        this.dataTracker.startTracking(SITTING, false);
        this.dataTracker.startTracking(STANDING, false);
        this.dataTracker.startTracking(CHESTED, false);
        this.dataTracker.startTracking(TRADER, false);
        this.dataTracker.startTracking(CARPET_COLOR, -1);
    }

    @Override
    public void tick() {
        super.tick();
        prevSitProgress = sitProgress;
        prevStandProgress = standProgress;

        if (isSitting()) {
            if (this.sitProgress < 5F)
                this.sitProgress++;
        } else {
            if (this.sitProgress > 0F)
                this.sitProgress--;
        }

        if (isStanding()) {
            if (this.standProgress < 5F)
                this.standProgress += 0.5F;
        } else {
            if (this.standProgress > 0F)
                this.standProgress -= 0.5F;
        }

        if (isStanding() && ++standingTime > maxStandTime) {
            this.setStanding(false);
            standingTime = 0;
            maxStandTime = 75 + random.nextInt(50);
        }
        if (isSitting() && isStanding()) {
            this.setStanding(false);
        }
        if (hasChestVarChanged && elephantInventory != null && !this.isChested()) {
            for (int i = 3; i < 18; i++) {
                if (!elephantInventory.getStack(i).isEmpty()) {
                    if (!this.getWorld().isClient) {
                        this.dropStack(elephantInventory.getStack(i), 1);
                    }
                    elephantInventory.removeStack(i);
                }
            }
            hasChestVarChanged = false;
        }
        if (!hasTuskedAttributes && isTusked() && !isBaby()) {
            calculateDimensions();
        }
        if (hasTuskedAttributes && !isTusked() && !isBaby()) {
            calculateDimensions();
        }
        if (charging) {
            chargingTicks++;
        }
        if (!this.getMainHandStack().isEmpty() && this.canTargetItem(this.getMainHandStack())) {
            if (this.getAnimation() == NO_ANIMATION) {
                this.setAnimation(ANIMATION_EAT);
            }
            if (this.getAnimation() == ANIMATION_EAT && this.getAnimationTick() == 17) {
                this.eatItemEffect(this.getMainHandStack());
                if (this.getMainHandStack().isIn(AMTagRegistry.ELEPHANT_TAMEABLES) && !this.isTamed() && (!isTusked() || isBaby()) && blossomThrowerUUID != null) {
                    if (random.nextInt(3) == 0) {
                        this.setTamed(true);
                        this.setOwnerUuid(blossomThrowerUUID);
                        PlayerEntity player = this.getWorld().getPlayerByUuid(blossomThrowerUUID);
                        if (player != null) {
                            this.setOwner(player);
                        }
                        for (Entity passenger : this.getPassengerList()) {
                            passenger.dismountVehicle();
                        }
                        this.getWorld().sendEntityStatus(this, (byte) 7);
                    } else {
                        this.getWorld().sendEntityStatus(this, (byte) 6);
                    }
                }
                this.setStackInHand(Hand.MAIN_HAND, ItemStack.EMPTY);
                this.heal(10);
            }
        }
        if (chargeCooldown > 0) {
            chargeCooldown--;
        }
        if (charging) {
            chargingTicks++;
        } else {
            chargingTicks = 0;
        }
        if (this.getAnimation() == ANIMATION_CHARGE_PREPARE) {
            this.bodyYaw = getYaw();
            if (this.getAnimationTick() == 20) {
                this.charging = true;
            }
        }
        if (this.getControllingPassenger() != null && charging && chargingTicks > 100) {
            this.charging = false;
            this.chargeCooldown = 200;
        }
        LivingEntity target = this.getTarget();
        double maxAttackMod = 0.0F;
        if (this.getControllingPassenger() != null && this.getControllingPassenger() instanceof PlayerEntity rider) {
            if (rider.getAttacking() != null && !this.isTeammate(rider.getAttacking())) {
                UUID preyUUID = rider.getAttacking().getUuid();
                if (!this.getUuid().equals(preyUUID)) {
                    target = rider.getAttacking();
                    maxAttackMod = 4F;
                }
            }
        }
        if (!this.getWorld().isClient && target != null) {
            if (this.distanceTo(target) > this.getWidth() * 0.5F + 0.5F && this.getControllingPassenger() == null && this.isTusked() && this.canSee(target) && this.getAnimation() == NO_ANIMATION && !charging && chargeCooldown == 0) {
                this.setAnimation(ANIMATION_CHARGE_PREPARE);
            }
            if (this.getAnimation() == ANIMATION_CHARGE_PREPARE && this.getControllingPassenger() == null) {
                this.lookAtEntity(target, 360, 30);
                this.bodyYaw = getYaw();
                if (this.getAnimationTick() == 20) {
                    this.charging = true;
                }
            }
            if (this.distanceTo(target) < 10D && charging) {
                this.setAnimation(ANIMATION_FLING);
            }
            if (this.distanceTo(target) < 2.1D && charging) {
                target.takeKnockback(1F, target.getX() - this.getX(), target.getZ() - this.getZ());
                target.velocityDirty = true;
                target.setVelocity(target.getVelocity().add(0, 0.7F, 0));
                target.damage(this.getDamageSources().mobAttack(this), 2.4F * (float) this.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE).getBaseValue());
                launch(target, true);
                this.charging = false;
                this.chargeCooldown = 400;
            }
            double dist = this.distanceTo(target);
            if (dist < 4.5D + maxAttackMod && this.getAnimation() == ANIMATION_FLING && this.getAnimationTick() == 15) {
                target.takeKnockback(1F, target.getX() - this.getX(), target.getZ() - this.getZ());
                target.setVelocity(target.getVelocity().add(0, 0.3F, 0));
                launch(target, false);
                target.damage(this.getDamageSources().mobAttack(this), (float) this.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE).getBaseValue());
            }
            if (dist < 4.5D + maxAttackMod && this.getAnimation() == ANIMATION_STOMP && this.getAnimationTick() == 17) {
                target.takeKnockback(0.3F, target.getX() - this.getX(), target.getZ() - this.getZ());
                target.damage(this.getDamageSources().mobAttack(this), (float) this.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE).getBaseValue());
            }
        }
        if (!this.getWorld().isClient && this.getTarget() == null && this.getControllingPassenger() == null) {
            charging = false;
        }
        if (charging && !hasChargedSpeed) {
            this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).setBaseValue(0.65F);
            hasChargedSpeed = true;
        }
        if (!charging && hasChargedSpeed) {
            this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).setBaseValue(0.35F);
            hasChargedSpeed = false;
        }
        if (!this.getWorld().isClient && this.getRandom().nextInt(400) == 0 && this.getAnimation() == NO_ANIMATION) {
            this.setAnimation(this.getRandom().nextBoolean() ? ANIMATION_TRUMPET_0 : ANIMATION_TRUMPET_1);
        }
        if (this.getAnimation() == ANIMATION_TRUMPET_0 && this.getAnimationTick() == 8 || this.getAnimation() == ANIMATION_TRUMPET_1 && this.getAnimationTick() == 4) {
            this.emitGameEvent(GameEvent.ENTITY_ROAR);
            this.playSound(AMSoundRegistry.ELEPHANT_TRUMPET.get(), this.getSoundVolume(), this.getSoundPitch());
        }
        if (this.isAlive() && charging) {
            for (Entity entity : this.getWorld().getNonSpectatingEntities(LivingEntity.class, this.getBoundingBox().expand(1.0D))) {
                if (!(this.isTamed() && isTeammate(entity)) && !(!this.isTamed() && entity instanceof EntityElephant) && entity != this) {
                    entity.damage(this.getDamageSources().mobAttack(this), 8.0F + random.nextFloat() * 8.0F);
                    launch(entity, true);
                }
            }
            this.setStepHeight(2F);
        }else{
            this.setStepHeight(1.1F);
        }
        if (!isTamed() && isTrader()) {
            if (!this.getWorld().isClient) {
                this.tryDespawn();
            }
        }
        if (this.getTarget() != null && !this.getTarget().isAlive()) {
            this.setTarget(null);
        }
        AnimationHandler.INSTANCE.updateAnimations(this);
    }

    @Override
    public void tickMovement() {
        super.tickMovement();
        if (this.isBaby() && this.getStandingEyeHeight() > this.getHeight()) {
            this.calculateDimensions();
        }
    }

    private boolean canDespawn() {
        return !this.isTamed() && this.isTrader();
    }

    private void tryDespawn() {
        if (this.canDespawn()) {
            if (this.getControllingVillager() instanceof WanderingTraderEntity) {
                int riderDelay = ((WanderingTraderEntity) this.getControllingVillager()).getDespawnDelay();
                if (riderDelay > 0) {
                    this.despawnDelay = riderDelay;
                }
            }
            this.despawnDelay = this.despawnDelay - 1;
            if (this.despawnDelay <= 0) {
                this.detachLeash(true, false);
                this.elephantInventory.clear();
                if(this.getControllingVillager() != null){
                    this.getControllingVillager().remove(RemovalReason.DISCARDED);
                }
                this.remove(RemovalReason.DISCARDED);
            }
        }
    }

    private void launch(Entity e, boolean huge) {
        if (e.isOnGround()) {
            double d0 = e.getX() - this.getX();
            double d1 = e.getZ() - this.getZ();
            double d2 = Math.max(d0 * d0 + d1 * d1, 0.001D);
            float f = huge ? 2F : 0.5F;
            e.addVelocity(d0 / d2 * f, huge ? 0.5D : 0.2F, d1 / d2 * f);
        }
    }

    private void eatItemEffect(ItemStack heldItemMainhand) {
        this.emitGameEvent(GameEvent.EAT);
        this.playSound(SoundEvents.ENTITY_STRIDER_EAT, this.getSoundPitch(), this.getSoundVolume());
        for (int i = 0; i < 8 + random.nextInt(3); i++) {
            double d2 = this.random.nextGaussian() * 0.02D;
            double d0 = this.random.nextGaussian() * 0.02D;
            double d1 = this.random.nextGaussian() * 0.02D;
            float radius = this.getWidth() * 0.65F;
            float angle = (Maths.STARTING_ANGLE * this.bodyYaw);
            double extraX = radius * MathHelper.sin(MathHelper.PI + angle);
            double extraZ = radius * MathHelper.cos(angle);
            ParticleEffect data = new ItemStackParticleEffect(ParticleTypes.ITEM, heldItemMainhand);
            if (heldItemMainhand.getItem() instanceof BlockItem) {
                data = new BlockStateParticleEffect(ParticleTypes.BLOCK, ((BlockItem) heldItemMainhand.getItem()).getBlock().getDefaultState());
            }
            this.getWorld().addParticle(data, this.getX() + extraX, this.getY() + this.getHeight() * 0.6F, this.getZ() + extraZ, d0, d1, d2);
        }
    }

    private boolean isChargePlayer(Entity controllingPassenger) {
        return true;
    }

    @Override
    public boolean tryAttack(Entity entityIn) {
        if (this.getAnimation() == NO_ANIMATION && !this.charging) {
            this.setAnimation(random.nextBoolean() ? ANIMATION_FLING : ANIMATION_STOMP);
        }
        return true;
    }

    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        boolean owner = this.isTamed() && isOwner(player);
        ActionResult type = super.interactMob(player, hand);
        if (isChested() && player.isSneaking()) {
            this.openGUI(player);
            return ActionResult.SUCCESS;
        } else if (canTargetItem(stack) && this.getMainHandStack().isEmpty()) {
            ItemStack rippedStack = stack.copy();
            rippedStack.setCount(1);
            stack.decrement(1);
            this.setStackInHand(Hand.MAIN_HAND, rippedStack);
            if (rippedStack.isIn(AMTagRegistry.ELEPHANT_TAMEABLES)) {
                blossomThrowerUUID = player.getUuid();
            }
            return ActionResult.SUCCESS;
        } else if (owner && stack.isIn(ItemTags.WOOL_CARPETS)) {
            DyeColor color = getCarpetColor(stack);
            if (color != this.getColor()) {
                if (this.getColor() != null) {
                    this.dropItem(this.getCarpetItemBeingWorn());
                }
                this.emitGameEvent(GameEvent.ENTITY_INTERACT);
                this.playSound(SoundEvents.ENTITY_LLAMA_SWAG, 1.0F, (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F);
                if(!this.getWorld().isClient && player instanceof ServerPlayerEntity serverPlayer){
                    AMAdvancementTriggerRegistry.ELEPHANT_SWAG.trigger(serverPlayer);
                }
                stack.decrement(1);
                this.setColor(color);
                return ActionResult.SUCCESS;
            }
            return ActionResult.PASS;
        } else if (owner && this.getColor() != null && ModTagsCompat.isAnyShear(stack)) {
            this.emitGameEvent(GameEvent.ENTITY_INTERACT);
            this.playSound(SoundEvents.ENTITY_SHEEP_SHEAR, 1.0F, (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F);
            if (this.getColor() != null) {
                this.dropItem(this.getCarpetItemBeingWorn());
            }
            this.setColor(null);
            return ActionResult.SUCCESS;
        } else if (owner && !this.isChested() && ModTagsCompat.isAnyWoodenChest(stack)) {
            this.setChested(true);
            this.emitGameEvent(GameEvent.ENTITY_INTERACT);
            this.playSound(SoundEvents.ENTITY_DONKEY_CHEST, 1.0F, (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F);
            if (!player.getAbilities().creativeMode) {
                stack.decrement(1);
            }
            return ActionResult.success(this.getWorld().isClient);
        } else if (owner && isChested() && ModTagsCompat.isAnyShear(stack)) {
            this.emitGameEvent(GameEvent.ENTITY_INTERACT);
            this.playSound(SoundEvents.ENTITY_SHEEP_SHEAR, 1.0F, (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F);
            this.dropItem(Blocks.CHEST);
            for (int i = 0; i < elephantInventory.size(); i++) {
                this.dropStack(elephantInventory.getStack(i));
            }
            elephantInventory.clear();
            this.setChested(false);
            return ActionResult.SUCCESS;
        } else if (owner && !this.isBaby() && type != ActionResult.CONSUME) {
            if(!this.getWorld().isClient){
                player.startRiding(this);
            }
            return ActionResult.SUCCESS;
        }
        return type;
    }

    @Override
    public EntityDimensions getDimensions(EntityPose poseIn) {
        return isTusked() && !isBaby() ? TUSKED_SIZE : super.getDimensions(poseIn);
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
        return new Animation[]{ANIMATION_TRUMPET_0, ANIMATION_TRUMPET_1, ANIMATION_CHARGE_PREPARE, ANIMATION_STOMP, ANIMATION_FLING, ANIMATION_EAT, ANIMATION_BREAKLEAVES};
    }

    @Override
    public int getAnimationTick() {
        return animationTick;
    }

    @Override
    public void setAnimationTick(int tick) {
        animationTick = tick;
    }

    public Item getCarpetItemBeingWorn() {
        if (this.getColor() != null) {
            return DYE_COLOR_ITEM_MAP.get(this.getColor());
        }
        return Items.AIR;
    }

    @Override
    protected void dropInventory() {
        super.dropInventory();
        if (this.isChested()) {
            if (!this.getWorld().isClient) {
                this.dropItem(Blocks.CHEST);
            }
            for (int i = 0; i < elephantInventory.size(); i++) {
                this.dropStack(elephantInventory.getStack(i));
            }
            elephantInventory.clear();
            this.setChested(false);
        }
        if (!this.isTrader() && this.getColor() != null) {
            if (!this.getWorld().isClient) {
                this.dropItem(this.getCarpetItemBeingWorn());
            }
            this.setColor(null);
        }
    }

    @Nullable
    @Override
    public PassiveEntity createChild(ServerWorld world, PassiveEntity entity) {
        var baby = AMEntityRegistry.ELEPHANT.get().create(world);
        baby.setTusked(this.getNearestTusked(getWorld(), 15) == null || random.nextInt(2) == 0);
        return baby;
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound compound) {
        super.writeCustomDataToNbt(compound);
        compound.putBoolean("Tusked", this.isTusked());
        compound.putBoolean("ElephantSitting", this.isSitting());
        compound.putBoolean("Standing", this.isStanding());
        compound.putBoolean("Chested", this.isChested());
        compound.putBoolean("Trader", this.isTrader());
        compound.putBoolean("ForcedToSit", this.forcedSit);
        compound.putBoolean("Tamed", this.isTamed());
        compound.putInt("ChargeCooldown", this.chargeCooldown);
        compound.putInt("Carpet", this.dataTracker.get(CARPET_COLOR));
        compound.putInt("DespawnDelay", this.despawnDelay);
        if (elephantInventory != null) {
            var nbttaglist = new NbtList();
            for (int i = 0; i < this.elephantInventory.size(); ++i) {
                var itemstack = this.elephantInventory.getStack(i);
                if (!itemstack.isEmpty()) {
                    var CompoundNBT = new NbtCompound();
                    CompoundNBT.putByte("Slot", (byte) i);
                    itemstack.writeNbt(CompoundNBT);
                    nbttaglist.add(CompoundNBT);
                }
            }
            compound.put("Items", nbttaglist);
        }
    }

    @Override
    public boolean canHaveStatusEffect(StatusEffectInstance potioneffectIn) {
        if (potioneffectIn.getEffectType() == StatusEffects.WITHER) {
            return false;
        }
        return super.canHaveStatusEffect(potioneffectIn);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound compound) {
        super.readCustomDataFromNbt(compound);
        this.setTamed(compound.getBoolean("Tamed"));
        this.setTusked(compound.getBoolean("Tusked"));
        this.setStanding(compound.getBoolean("Standing"));
        this.setSitting(compound.getBoolean("ElephantSitting"));
        this.setChested(compound.getBoolean("Chested"));
        this.setTrader(compound.getBoolean("Trader"));
        this.forcedSit = compound.getBoolean("ForcedToSit");
        this.chargeCooldown = compound.getInt("ChargeCooldown");
        this.dataTracker.set(CARPET_COLOR, compound.getInt("Carpet"));
        if (elephantInventory != null) {
            var nbttaglist = compound.getList("Items", 10);
            this.initElephantInventory();
            for (int i = 0; i < nbttaglist.size(); ++i) {
                NbtCompound CompoundNBT = nbttaglist.getCompound(i);
                int j = CompoundNBT.getByte("Slot") & 255;
                this.elephantInventory.setStack(j, ItemStack.fromNbt(CompoundNBT));
            }
        } else {
            var nbttaglist = compound.getList("Items", 10);
            this.initElephantInventory();
            for (int i = 0; i < nbttaglist.size(); ++i) {
                NbtCompound CompoundNBT = nbttaglist.getCompound(i);
                int j = CompoundNBT.getByte("Slot") & 255;
                this.initElephantInventory();
                this.elephantInventory.setStack(j, ItemStack.fromNbt(CompoundNBT));
            }
        }
        if (compound.contains("DespawnDelay", 99)) {
            this.despawnDelay = compound.getInt("DespawnDelay");
        }

    }

    public boolean isChested() {
        return this.dataTracker.get(CHESTED);
    }

    public void setChested(boolean chested) {
        this.dataTracker.set(CHESTED, chested);
        this.hasChestVarChanged = true;
    }

    public boolean setSlot(int inventorySlot, @Nullable ItemStack itemStackIn) {
        int j = inventorySlot - 500 + 2;
        if (j >= 0 && j < this.elephantInventory.size()) {
            this.elephantInventory.setStack(j, itemStackIn);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void onDeath(DamageSource cause) {
        super.onDeath(cause);
        if (elephantInventory != null && !this.getWorld().isClient) {
            for (int i = 0; i < elephantInventory.size(); ++i) {
                var itemstack = elephantInventory.getStack(i);
                if (!itemstack.isEmpty()) {
                    this.dropStack(itemstack, 0.0F);
                }
            }
        }
    }

    public boolean isStanding() {
        return this.dataTracker.get(STANDING);
    }

    public void setStanding(boolean standing) {
        this.dataTracker.set(STANDING, standing);
    }

    @Override
    public boolean isSitting() {
        return this.dataTracker.get(SITTING);
    }

    @Override
    public void setSitting(boolean sit) {
        this.dataTracker.set(SITTING, sit);
    }

    @Nullable
    public DyeColor getColor() {
        int lvt_1_1_ = this.dataTracker.get(CARPET_COLOR);
        return lvt_1_1_ == -1 ? null : DyeColor.byId(lvt_1_1_);
    }

    public void setColor(@Nullable DyeColor color) {
        this.dataTracker.set(CARPET_COLOR, color == null ? -1 : color.getId());
    }

    @Nullable
    @Override
    public EntityData initialize(ServerWorldAccess worldIn, LocalDifficulty difficultyIn, SpawnReason reason, @Nullable EntityData spawnDataIn, @Nullable NbtCompound dataTag) {
        if (spawnDataIn instanceof PassiveData lvt_6_1_) {
            if (lvt_6_1_.getSpawnedCount() == 0) {
                this.setTusked(true);
            }
        }else{
            this.setTusked(this.getRandom().nextBoolean());
        }

        return super.initialize(worldIn, difficultyIn, reason, spawnDataIn, dataTag);
    }

    @Nullable
    public EntityElephant getNearestTusked(WorldAccess world, double dist) {
        List<? extends EntityElephant> list = world.getNonSpectatingEntities(this.getClass(), this.getBoundingBox().expand(dist, dist / 2, dist));
        if (list.isEmpty()) {
            return null;
        }
        EntityElephant elephant1 = null;
        double d0 = Double.MAX_VALUE;
        for (EntityElephant elephant : list) {
            if (elephant.isTusked()) {
                double d1 = this.squaredDistanceTo(elephant);
                if (!(d1 > d0)) {
                    d0 = d1;
                    elephant1 = elephant;
                }
            }
        }
        return elephant1;
    }

    public boolean isTusked() {
        return this.dataTracker.get(TUSKED);
    }

    public void setTusked(boolean tusked) {
        boolean prev = isTusked();
        if (!prev && tusked) {
            this.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(110.0D);
            this.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE).setBaseValue(15.0D);
            this.setHealth(150.0F);
        } else {
            this.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(85.0D);
            this.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE).setBaseValue(10.0D);
        }
        this.dataTracker.set(TUSKED, tusked);
    }

    @Override
    public boolean canTargetItem(ItemStack stack) {
        return stack.isIn(AMTagRegistry.ELEPHANT_FOODSTUFFS);
    }

    @Override
    public void onGetItem(ItemEntity e) {
        var duplicate = e.getStack().copy();
        duplicate.setCount(1);
        if (!this.getStackInHand(Hand.MAIN_HAND).isEmpty() && !this.getWorld().isClient) {
            this.dropStack(this.getStackInHand(Hand.MAIN_HAND), 0.0F);
        }
        Entity itemThrower = e.getOwner();
        if (duplicate.isIn(AMTagRegistry.ELEPHANT_TAMEABLES) && itemThrower != null) {
            blossomThrowerUUID = itemThrower.getUuid();
        } else {
            blossomThrowerUUID = null;
        }
        this.setStackInHand(Hand.MAIN_HAND, duplicate);
        this.aiItemFlag = false;
    }

    @Override
    public void onFindTarget(ItemEntity e) {
        this.aiItemFlag = true;
    }

    public void addElephantLoot(@Nullable PlayerEntity player, int seed) {
        if (this.getWorld().getServer() != null) {
            var loottable = this.getWorld().getServer().getLootManager().getLootTable(TRADER_LOOT);

            var lootcontext$builder = (new LootContextParameterSet.Builder((ServerWorld) this.getWorld()));

            loottable.supplyInventory(this.elephantInventory, lootcontext$builder.build(LootContextTypes.EMPTY), seed);
        }
    }

    public void leaveCaravan() {
        if (this.caravanHead != null) {
            this.caravanHead.caravanTail = null;
        }

        this.caravanHead = null;
    }

    public void joinCaravan(EntityElephant caravanHeadIn) {
        this.caravanHead = caravanHeadIn;
        this.caravanHead.caravanTail = this;
    }

    public boolean hasCaravanTrail() {
        return this.caravanTail != null;
    }

    public boolean inCaravan() {
        return this.caravanHead != null;
    }

    @Nullable
    public EntityElephant getCaravanHead() {
        return this.caravanHead;
    }

    @Override
    public double getMaxDistToItem() {
        return Math.pow(this.getWidth() + 3.0F, 2.0F);
    }

    @Override
    public void updatePassengerPosition(Entity passenger, Entity.PositionUpdater moveFunc) {
        if (this.hasPassenger(passenger)) {
            float standAdd = -0.3F * standProgress;
            float scale = this.isBaby() ? 0.5F : this.isTusked() ? 1.1F : 1.0F;
            float sitAdd = -0.065F * sitProgress;
            float scaleY = scale * (2.4F * sitAdd - 0.4F * standAdd);
            if (passenger instanceof MerchantEntity) {
                scaleY -= 0.3F;
            }
            float radius = scale * (0.5F + standAdd);
            float angle = (Maths.STARTING_ANGLE * this.bodyYaw);
            if (this.getAnimation() == ANIMATION_CHARGE_PREPARE) {
                float sinWave = MathHelper.sin((float) (Math.PI * (this.getAnimationTick() / 25F)));
                radius += sinWave * 0.2F * scale;
            }
            if (this.getAnimation() == ANIMATION_STOMP) {
                float sinWave = MathHelper.sin((float) (Math.PI * (this.getAnimationTick() / 20F)));
                radius -= sinWave * 1.0F * scale;
                scaleY += sinWave * 0.7F * scale;
            }
            double extraX = radius * MathHelper.sin(MathHelper.PI + angle);
            double extraZ = radius * MathHelper.cos(angle);

            passenger.setPos(this.getX() + extraX, this.getY() + this.getMountedHeightOffset() + scaleY + passenger.getHeightOffset(), this.getZ() + extraZ);
        }
    }

    @Override
    protected Vec3d getControlledMovementInput(PlayerEntity player, Vec3d deltaIn) {
        if (player.forwardSpeed != 0) {
            float f = player.forwardSpeed < 0.0F ? 0.5F : 1.0F;
            return new Vec3d(player.sidewaysSpeed * 0.25F, 0.0D, player.forwardSpeed * 0.5F * f);
        } else {
            this.setSprinting(false);
        }
        return Vec3d.ZERO;
    }

    @Override
    protected void tickControlled(PlayerEntity player, Vec3d vec3) {
        super.tickControlled(player, vec3);
        if(player.forwardSpeed != 0 || player.sidewaysSpeed != 0){
            this.setRotation(player.getYaw(), player.getPitch() * 0.25F);
            this.prevYaw = this.bodyYaw = this.headYaw= this.getYaw();
            this.setStepHeight(1);
            this.getNavigation().stop();
            this.setTarget(null);
            this.setSprinting(true);
        }
    }

    @Override
    protected float getSaddledSpeed(PlayerEntity rider) {
        return (float)(this.getAttributeValue(EntityAttributes.GENERIC_MOVEMENT_SPEED));
    }

    @Override
    public double getMountedHeightOffset() {
        float scale = this.isBaby() ? 0.5F : this.isTusked() ? 1.1F : 1.0F;
        float f = Math.min(0.25F, this.limbAnimator.getSpeed());
        float f1 = this.limbAnimator.getPos();
        float sitAdd = 0.01F * 0;
        float standAdd = 0.07F * 0;
        return (double) this.getHeight() - 0.05F - scale * ((double) (0.1F * MathHelper.cos(f1 * 1.4F) * 1.4F * f) + sitAdd + standAdd);
    }

    @Override
    public boolean isTeammate(Entity entityIn) {
        if (this.isTamed()) {
            LivingEntity livingentity = this.getOwner();
            if (entityIn == livingentity) {
                return true;
            }
            if (entityIn instanceof TameableEntity) {
                return ((TameableEntity) entityIn).isOwner(livingentity);
            }
            if (livingentity != null) {
                return livingentity.isTeammate(entityIn);
            }
        }

        return super.isTeammate(entityIn);
    }

    @Override
    public void travel(Vec3d vec3d) {
        if (this.isSitting()) {
            if (this.getNavigation().getCurrentPath() != null) {
                this.getNavigation().stop();
            }
            vec3d = Vec3d.ZERO;
        }
        super.travel(vec3d);
    }

    public void openGUI(PlayerEntity playerEntity) {
        if (!this.getWorld().isClient && (!this.hasPassenger(playerEntity))) {
            MenuRegistry.openMenu((ServerPlayerEntity) playerEntity, new ExtendedMenuProvider() {
                @Override
                public void saveExtraData(PacketByteBuf buf) {
                }

                @Override
                public Text getDisplayName() {
                    return Text.translatable("entity.alexsmobs.elephant.chest");
                }

                @Override
                public @NotNull ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
                    return GenericContainerScreenHandler.createGeneric9x6(syncId, playerInventory, elephantInventory);
                }
            });
        }
    }

    public boolean isTrader() {
        return this.dataTracker.get(TRADER);
    }

    public void setTrader(boolean trader) {
        this.dataTracker.set(TRADER, trader);
    }

    public boolean triggerCharge(ItemStack stack) {
        if (this.getControllingPassenger() != null && chargeCooldown == 0 && !charging && this.getAnimation() == NO_ANIMATION && this.isTusked()) {
            this.setAnimation(ANIMATION_CHARGE_PREPARE);
            this.eatItemEffect(stack);
            this.heal(2);
            return true;
        }
        return false;
    }

    public boolean canSpawnWithTraderHere() {
        return this.getWorld().isChunkLoaded(this.getBlockPos()) && this.canSpawn(getWorld()) && getWorld().isAir(this.getBlockPos().up(4));
    }

    @Override
    public EntityView method_48926() {
        return getWorld();
    }

    private class AIWalkIdle extends WanderAroundGoal {
        public AIWalkIdle(EntityElephant e, double v) {
            super(e, v);
        }

        @Override
        public boolean canStart() {
            this.chance = EntityElephant.this.isTusked() || !EntityElephant.this.inCaravan() ? 50 : 120;
            return super.canStart();
        }

        @Nullable
        @Override
        protected Vec3d getWanderTarget() {
            return FuzzyTargeting.find(this.mob, EntityElephant.this.isTusked() || !EntityElephant.this.inCaravan() ? 25 : 10, 7);
        }

    }

    class MyRevengeGoal extends RevengeGoal {
        public MyRevengeGoal() {
            super(EntityElephant.this);
        }

        @Override
        public void start() {
            if (EntityElephant.this.isBaby() || !EntityElephant.this.isTusked()) {
                this.callSameTypeForRevenge();
                this.stop();
            } else {
                super.start();
            }
        }

        @Override
        protected void setMobEntityTarget(MobEntity mobIn, LivingEntity targetIn) {
            if (mobIn instanceof EntityElephant && (!mobIn.isBaby() || !((EntityElephant) mobIn).isTusked())) {
                super.setMobEntityTarget(mobIn, targetIn);
            }
        }
    }

    class MyEscapeDangerGoal extends EscapeDangerGoal {
        public MyEscapeDangerGoal() {
            super(EntityElephant.this, 1.0D);
        }

        @Override
        public boolean canStart() {
            return (EntityElephant.this.isBaby() || !EntityElephant.this.isTusked() || EntityElephant.this.isOnFire()) && super.canStart();
        }
    }
}
