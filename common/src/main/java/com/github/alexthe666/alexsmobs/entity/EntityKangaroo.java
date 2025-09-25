package com.github.alexthe666.alexsmobs.entity;

import com.github.alexthe666.alexsmobs.AlexsMobs;
import com.github.alexthe666.alexsmobs.config.AMConfig;
import com.github.alexthe666.alexsmobs.entity.ai.*;
import com.github.alexthe666.alexsmobs.packet.KangarooEatPacket;
import com.github.alexthe666.alexsmobs.packet.KangarooInventorySyncPacket;
import com.github.alexthe666.alexsmobs.registry.AMEntityRegistry;
import com.github.alexthe666.alexsmobs.registry.AMSoundRegistry;
import com.github.alexthe666.alexsmobs.registry.AMTagRegistry;
import com.iafenvoy.uranus.animation.Animation;
import com.iafenvoy.uranus.animation.AnimationHandler;
import com.iafenvoy.uranus.animation.IAnimatedEntity;
import dev.architectury.registry.menu.ExtendedMenuProvider;
import dev.architectury.registry.menu.MenuRegistry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.control.JumpControl;
import net.minecraft.entity.ai.control.MoveControl;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.InventoryChangedListener;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.recipe.Ingredient;
import net.minecraft.screen.Generic3x3ContainerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.EntityView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.event.GameEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class EntityKangaroo extends TameableEntity implements InventoryChangedListener, IAnimatedEntity, IFollower {

    public static final Animation ANIMATION_EAT_GRASS = Animation.create(30);
    public static final Animation ANIMATION_KICK = Animation.create(15);
    public static final Animation ANIMATION_PUNCH_R = Animation.create(13);
    public static final Animation ANIMATION_PUNCH_L = Animation.create(13);
    private static final TrackedData<Boolean> STANDING = DataTracker.registerData(EntityKangaroo.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> SITTING = DataTracker.registerData(EntityKangaroo.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Integer> COMMAND = DataTracker.registerData(EntityKangaroo.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> VISUAL_FLAG = DataTracker.registerData(EntityKangaroo.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> POUCH_TICK = DataTracker.registerData(EntityKangaroo.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> HELMET_INDEX = DataTracker.registerData(EntityKangaroo.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> SWORD_INDEX = DataTracker.registerData(EntityKangaroo.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> CHEST_INDEX = DataTracker.registerData(EntityKangaroo.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Boolean> FORCED_SIT = DataTracker.registerData(EntityKangaroo.class, TrackedDataHandlerRegistry.BOOLEAN);
    public float prevPouchProgress;
    public float pouchProgress;
    public float sitProgress;
    public float prevSitProgress;
    public float standProgress;
    public float prevStandProgress;
    public float totalMovingProgress;
    public float prevTotalMovingProgress;
    public int maxStandTime = 75;
    public SimpleInventory kangarooInventory;
    private int animationTick;
    private Animation currentAnimation;
    private int jumpTicks;
    private int jumpDuration;
    private boolean wasOnGround;
    private int currentMoveTypeDuration;
    private int standingTime = 0;
    private int sittingTime = 0;
    private int maxSitTime = 75;
    private int eatCooldown = 0;
    private int carrotFeedings = 0;
    private int clientArmorCooldown = 0;

    public EntityKangaroo(EntityType<EntityKangaroo> type, World world) {
        super(type, world);
        initKangarooInventory();
        this.jumpControl = new JumpHelperController(this);
        this.moveControl = new EntityKangaroo.MoveHelperController(this);
    }

    public static <T extends MobEntity> boolean canKangarooSpawn(EntityType<? extends AnimalEntity> animal, WorldAccess worldIn, SpawnReason reason, BlockPos pos, Random random) {
        boolean spawnBlock = worldIn.getBlockState(pos.down()).isIn(AMTagRegistry.KANGAROO_SPAWNS);
        return spawnBlock && worldIn.getBaseLightLevel(pos, 0) > 8;
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createLivingAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 22.0D)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.5F)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 4F)
                .add(EntityAttributes.GENERIC_ATTACK_KNOCKBACK, 0);
    }

    @Nullable
    @Override
    public LivingEntity getControllingPassenger() {
        return null;
    }

    @Override
    protected void updateLeash() {
        super.updateLeash();
        var lvt_1_1_ = this.getHoldingEntity();
        if (lvt_1_1_ != null && lvt_1_1_.getWorld() == this.getWorld()) {
            this.setPositionTarget(lvt_1_1_.getBlockPos(), 5);
            float lvt_2_1_ = this.distanceTo(lvt_1_1_);
            if (this.isSitting()) {
                if (lvt_2_1_ > 10.0F) {
                    this.detachLeash(true, true);
                }

                return;
            }

            this.updateForLeashLength(lvt_2_1_);
            if (lvt_2_1_ > 10.0F) {
                this.detachLeash(true, true);
                this.goalSelector.disableControl(Goal.Control.MOVE);
            } else if (lvt_2_1_ > 6.0F) {
                double lvt_3_1_ = (lvt_1_1_.getX() - this.getX()) / (double) lvt_2_1_;
                double lvt_5_1_ = (lvt_1_1_.getY() - this.getY()) / (double) lvt_2_1_;
                double lvt_7_1_ = (lvt_1_1_.getZ() - this.getZ()) / (double) lvt_2_1_;
                this.setVelocity(this.getVelocity().add(Math.copySign(lvt_3_1_ * lvt_3_1_ * 0.4D, lvt_3_1_), Math.copySign(lvt_5_1_ * lvt_5_1_ * 0.4D, lvt_5_1_), Math.copySign(lvt_7_1_ * lvt_7_1_ * 0.4D, lvt_7_1_)));
            } else {
                this.goalSelector.enableControl(Goal.Control.MOVE);
                float lvt_3_2_ = 2.0F;
                try {
                    var lvt_4_1_ = (new Vec3d(lvt_1_1_.getX() - this.getX(), lvt_1_1_.getY() - this.getY(), lvt_1_1_.getZ() - this.getZ())).normalize().multiply(Math.max(lvt_2_1_ - 2.0F, 0.0F));
                    this.getNavigation().startMovingTo(this.getX() + lvt_4_1_.x, this.getY() + lvt_4_1_.y, this.getZ() + lvt_4_1_.z, this.getFollowLeashSpeed());
                } catch (Exception ignored) {
                }
            }
        }

    }

    public boolean forcedSit() {
        return this.dataTracker.get(FORCED_SIT);
    }

    public boolean isRoger() {
        String s = Formatting.strip(this.getName().getString());
        return s != null && s.equalsIgnoreCase("roger");
    }

    @Override
    public boolean canSpawn(WorldAccess worldIn, SpawnReason spawnReasonIn) {
        return AMEntityRegistry.rollSpawn(AMConfig.kangarooSpawnRolls, this.getRandom(), spawnReasonIn);
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return AMSoundRegistry.KANGAROO_IDLE.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return AMSoundRegistry.KANGAROO_IDLE.get();
    }

    private void initKangarooInventory() {
        SimpleInventory animalchest = this.kangarooInventory;
        this.kangarooInventory = new SimpleInventory(9) {
            @Override
            public void onClose(PlayerEntity player) {
                EntityKangaroo.this.dataTracker.set(POUCH_TICK, 10);
                EntityKangaroo.this.resetKangarooSlots();
            }

            @Override
            public boolean canPlayerUse(PlayerEntity player) {
                return EntityKangaroo.this.isAlive() && !EntityKangaroo.this.inNetherPortal;
            }
        };
        kangarooInventory.addListener(this);
        if (animalchest != null) {
            int i = Math.min(animalchest.size(), this.kangarooInventory.size());
            for (int j = 0; j < i; ++j) {
                ItemStack itemstack = animalchest.getStack(j);
                if (!itemstack.isEmpty()) {
                    this.kangarooInventory.setStack(j, itemstack.copy());
                }
            }
            resetKangarooSlots();
        }

    }

    @Override
    protected void dropInventory() {
        super.dropInventory();
        for (int i = 0; i < kangarooInventory.size(); i++) {
            this.dropStack(kangarooInventory.getStack(i));
        }
        kangarooInventory.clear();
    }

    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        ItemStack itemstack = player.getStackInHand(hand);
        Item item = itemstack.getItem();
        ActionResult type = super.interactMob(player, hand);
        if (!isTamed() && itemstack.isIn(AMTagRegistry.KANGAROO_TAMEABLES)) {
            this.eat(player, hand, itemstack);
            this.emitGameEvent(GameEvent.EAT);
            this.playSound(SoundEvents.ENTITY_HORSE_EAT, this.getSoundVolume(), this.getSoundPitch());
            carrotFeedings++;
            if (carrotFeedings > 10 && getRandom().nextInt(2) == 0 || carrotFeedings > 15) {
                this.setOwner(player);
                this.getWorld().sendEntityStatus(this, (byte) 7);
            } else {
                this.getWorld().sendEntityStatus(this, (byte) 6);
            }
            return ActionResult.SUCCESS;
        }
        if (isTamed() && this.getHealth() < this.getMaxHealth() && item.isFood() && item.getFoodComponent() != null && !item.getFoodComponent().isMeat()) {
            this.eat(player, hand, itemstack);
            this.emitGameEvent(GameEvent.EAT);
            this.playSound(SoundEvents.ENTITY_HORSE_EAT, this.getSoundVolume(), this.getSoundPitch());
            this.heal(item.getFoodComponent().getHunger());
            return ActionResult.SUCCESS;
        }
        var actionResult = itemstack.useOnEntity(player, this, hand);
        if (actionResult != ActionResult.SUCCESS && type != ActionResult.SUCCESS && isTamed() && isOwner(player) && !isBreedingItem(itemstack)) {
            if (player.isSneaking()) {
                if(!this.isBaby()){
                    this.openGUI(player);
                    this.removeAllPassengers();
                    this.dataTracker.set(POUCH_TICK, -1);
                }
            } else {
                this.setCommand(this.getCommand() + 1);
                if (this.getCommand() == 3) {
                    this.setCommand(0);
                }
                player.sendMessage(Text.translatable("entity.alexsmobs.all.command_" + this.getCommand(), this.getName()), true);
                boolean sit = this.getCommand() == 2;
                if (sit) {
                    this.dataTracker.set(FORCED_SIT, true);
                    this.setSitting(true);
                } else {
                    this.dataTracker.set(FORCED_SIT, false);
                    maxSitTime = 0;
                    this.setSitting(false);
                }
            }
            return ActionResult.SUCCESS;
        }

        return type;
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound compound) {
        super.writeCustomDataToNbt(compound);
        compound.putBoolean("KangarooSitting", this.isSitting());
        compound.putBoolean("KangarooSittingForced", this.forcedSit());
        compound.putBoolean("Standing", this.isStanding());
        compound.putInt("Command", this.getCommand());
        compound.putInt("HelmetInvIndex", this.dataTracker.get(HELMET_INDEX));
        compound.putInt("SwordInvIndex", this.dataTracker.get(SWORD_INDEX));
        compound.putInt("ChestInvIndex", this.dataTracker.get(CHEST_INDEX));
        if (kangarooInventory != null) {
            var nbttaglist = new NbtList();
            for (int i = 0; i < this.kangarooInventory.size(); ++i) {
                ItemStack itemstack = this.kangarooInventory.getStack(i);
                if (!itemstack.isEmpty()) {
                    NbtCompound CompoundNBT = new NbtCompound();
                    CompoundNBT.putByte("Slot", (byte) i);
                    itemstack.setNbt(CompoundNBT);
                    nbttaglist.add(CompoundNBT);
                }
            }
            compound.put("Items", nbttaglist);
        }
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound compound) {
        super.readCustomDataFromNbt(compound);
        this.setSitting(compound.getBoolean("KangarooSitting"));
        this.dataTracker.set(FORCED_SIT, compound.getBoolean("KangarooSittingForced"));
        this.setStanding(compound.getBoolean("Standing"));
        this.setCommand(compound.getInt("Command"));
        this.dataTracker.set(HELMET_INDEX, compound.getInt("HelmetInvIndex"));
        this.dataTracker.set(SWORD_INDEX, compound.getInt("SwordInvIndex"));
        this.dataTracker.set(CHEST_INDEX, compound.getInt("ChestInvIndex"));
        if (kangarooInventory != null) {
            var nbttaglist = compound.getList("Items", 10);
            this.initKangarooInventory();
            for (int i = 0; i < nbttaglist.size(); ++i) {
                NbtCompound CompoundNBT = nbttaglist.getCompound(i);
                int j = CompoundNBT.getByte("Slot") & 255;
                this.kangarooInventory.setStack(j, ItemStack.fromNbt(CompoundNBT));
            }
        } else {
            var nbttaglist = compound.getList("Items", 10);
            this.initKangarooInventory();
            for (int i = 0; i < nbttaglist.size(); ++i) {
                NbtCompound CompoundNBT = nbttaglist.getCompound(i);
                int j = CompoundNBT.getByte("Slot") & 255;
                this.initKangarooInventory();
                this.kangarooInventory.setStack(j, ItemStack.fromNbt(CompoundNBT));
            }
        }
        resetKangarooSlots();
    }

    public void openGUI(PlayerEntity playerEntity) {
        if (!this.getWorld().isClient && (!this.hasPassenger(playerEntity))) {
            MenuRegistry.openMenu((ServerPlayerEntity) playerEntity, new ExtendedMenuProvider() {
                @Override
                public void saveExtraData(PacketByteBuf buf) {
                }

                @Override
                public Text getDisplayName() {
                    return Text.translatable("entity.alexsmobs.kangaroo.pouch");
                }

                @Override
                public @NotNull ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
                    return new Generic3x3ContainerScreenHandler(syncId, playerInventory, kangarooInventory);
                }
            });
        }
    }

    @Override
    public boolean isSitting() {
        return this.dataTracker.get(SITTING);
    }

    @Override
    public void setSitting(boolean sit) {
        this.dataTracker.set(SITTING, sit);
    }

    public boolean isStanding() {
        return this.dataTracker.get(STANDING);
    }

    public void setStanding(boolean standing) {
        this.dataTracker.set(STANDING, standing);
    }

    public int getCommand() {
        return this.dataTracker.get(COMMAND);
    }

    public void setCommand(int command) {
        this.dataTracker.set(COMMAND, command);
    }

    public int getVisualFlag() {
        return this.dataTracker.get(VISUAL_FLAG);
    }

    public void setVisualFlag(int visualFlag) {
        this.dataTracker.set(VISUAL_FLAG, visualFlag);
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(STANDING, false);
        this.dataTracker.startTracking(SITTING, false);
        this.dataTracker.startTracking(FORCED_SIT, false);
        this.dataTracker.startTracking(COMMAND, 0);
        this.dataTracker.startTracking(VISUAL_FLAG, 0);
        this.dataTracker.startTracking(POUCH_TICK, 0);
        this.dataTracker.startTracking(CHEST_INDEX, -1);
        this.dataTracker.startTracking(HELMET_INDEX, -1);
        this.dataTracker.startTracking(SWORD_INDEX, -1);
    }

    @Override
    protected EntityNavigation createNavigation(World worldIn) {
        return new GroundPathNavigatorWide(this, worldIn, 2F);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new SitGoal(this));
        this.goalSelector.add(1, new KangarooAIMelee(this, 1.2D, false));
        this.goalSelector.add(2, new SwimGoal(this));
        this.goalSelector.add(2, new TameableAIFollowOwner(this, 1.2D, 5.0F, 2.0F, false));
        this.goalSelector.add(3, new AnimalMateGoal(this, 1D));
        this.goalSelector.add(4, new AnimalAIRideParent(this, 1.25D));
        this.goalSelector.add(4, new TemptGoal(this, 1.2D, Ingredient.fromTag(AMTagRegistry.KANGAROO_TAMEABLES), false));
        this.goalSelector.add(5, new AnimalAIWanderRanged(this, 110, 1.2D, 10, 7));
        this.goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 10.0F));
        this.goalSelector.add(7, new LookAroundGoal(this));
        this.targetSelector.add(1, new TrackOwnerAttackerGoal(this));
        this.targetSelector.add(2, new AttackWithOwnerGoal(this));
        this.targetSelector.add(3, (new AnimalAIHurtByTargetNotBaby(this)));
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return super.canAddPassenger(passenger) && this.dataTracker.get(POUCH_TICK) == 0;
    }

    @Override
    public double getMountedHeightOffset() {
        return (double) this.getHeight() * 0.35F;
    }

    //FIXME forge
//    @Override
//    public void onAddedToWorld() {
//        super.onAddedToWorld();
//        updateClientInventory();
//    }

    @Override
    public void tick() {
        super.tick();
        boolean moving = this.getVelocity().lengthSquared() > 0.03D;
        int pouchTick = this.dataTracker.get(POUCH_TICK);
        this.prevTotalMovingProgress = totalMovingProgress;
        this.prevPouchProgress = pouchProgress;
        this.prevSitProgress = sitProgress;
        this.prevStandProgress = standProgress;

        if (this.isSitting()) {
            if (sitProgress < 5F) {
                sitProgress++;
            }
        } else {
            if (sitProgress > 0F)
                sitProgress--;
        }

        if (eatCooldown > 0) {
            eatCooldown--;
        }

        if (this.isStanding()) {
            if (standProgress < 5F)
                standProgress++;
        } else {
            if (standProgress > 0F)
                standProgress--;
        }

        if (moving) {
            if (totalMovingProgress < 5F) {
                totalMovingProgress++;
            }
        } else {
            if (totalMovingProgress > 0F)
                totalMovingProgress--;
        }

        if (pouchTick != 0 && pouchProgress < 5) {
            pouchProgress += 1;
        }
        if (pouchTick == 0 && pouchProgress > 0) {
            pouchProgress -= 1;
        }
        if (pouchTick > 0) {
            this.dataTracker.set(POUCH_TICK, pouchTick - 1);
        }
        if (isStanding() && ++standingTime > maxStandTime) {
            this.setStanding(false);
            standingTime = 0;
            maxStandTime = 75 + random.nextInt(50);
        }
        if (isSitting() && !forcedSit() && ++sittingTime > maxSitTime) {
            this.setSitting(false);
            sittingTime = 0;
            maxSitTime = 75 + random.nextInt(50);
        }
        if (!this.getWorld().isClient && this.getAnimation() == NO_ANIMATION && this.getCommand() != 1 && !this.isStanding() && !this.isSitting() && random.nextInt(1500) == 0) {
            maxSitTime = 500 + random.nextInt(350);
            this.setSitting(true);
        }
        if (!forcedSit() && this.isSitting() && (this.getTarget() != null || this.isStanding())) {
            this.setSitting(false);
        }
        if (this.getAnimation() == NO_ANIMATION && !this.isStanding() && !this.isSitting() && random.nextInt(1500) == 0) {
            maxStandTime = 75 + random.nextInt(50);
            this.setStanding(true);
        }
        if (this.forcedSit() && !this.hasPassengers() && this.isTamed()) {
            this.setSitting(true);
        }
        if (!this.getWorld().isClient) {
            if (this.age == 1) {
                updateClientInventory();
            }

            if (!moving && this.getAnimation() == NO_ANIMATION && !this.isSitting() && !this.isStanding()) {
                if ((getRandom().nextInt(180) == 0 || this.getHealth() < this.getMaxHealth() && getRandom().nextInt(40) == 0) && getWorld().getBlockState(this.getBlockPos().down()).isOf(Blocks.GRASS_BLOCK)) {
                    this.setAnimation(ANIMATION_EAT_GRASS);
                }
            }
            if (this.getAnimation() == ANIMATION_EAT_GRASS && this.getAnimationTick() == 20 && this.getHealth() < this.getMaxHealth() && getWorld().getBlockState(this.getBlockPos().down()).isOf(Blocks.GRASS_BLOCK)) {
                this.heal(6);
                this.getWorld().syncWorldEvent(2001, getBlockPos().down(), Block.getRawIdFromState(Blocks.GRASS_BLOCK.getDefaultState()));
                this.getWorld().setBlockState(getBlockPos().down(), Blocks.DIRT.getDefaultState(), 2);
            }
            if (this.getHealth() < this.getMaxHealth() && this.isTamed() && eatCooldown == 0) {
                eatCooldown = 20 + random.nextInt(40);
                if (!this.kangarooInventory.isEmpty()) {
                    var foodStack = ItemStack.EMPTY;
                    for (int i = 0; i < this.kangarooInventory.size(); i++) {
                        ItemStack stack = this.kangarooInventory.getStack(i);
                        if (stack.getItem().isFood() && stack.getItem().getFoodComponent() != null && !stack.getItem().getFoodComponent().isMeat()) {
                            foodStack = stack;
                        }
                    }
                    if (!foodStack.isEmpty() && foodStack.getItem().getFoodComponent() != null) {
                        AlexsMobs.sendMSGToAll(new KangarooEatPacket(this.getId(), foodStack));
                        this.heal(foodStack.getItem().getFoodComponent().getHunger() * 2);
                        foodStack.decrement(1);
                        this.emitGameEvent(GameEvent.EAT);
                        this.playSound(SoundEvents.ENTITY_GENERIC_EAT, this.getSoundVolume(), this.getSoundPitch());
                    }
                }
            }
        }
        if (this.jumpTicks < this.jumpDuration) {
            ++this.jumpTicks;
        } else if (this.jumpDuration != 0) {
            this.jumpTicks = 0;
            this.jumpDuration = 0;
            this.setJumping(false);
        }
        LivingEntity attackTarget = this.getTarget();
        if (attackTarget != null && this.canSee(attackTarget)) {
            if (distanceTo(attackTarget) < attackTarget.getWidth() + this.getWidth() + 1) {
                if (this.getAnimation() == ANIMATION_KICK && this.getAnimationTick() == 8) {
                    attackTarget.takeKnockback(1.3F, MathHelper.sin(this.getYaw() * MathHelper.RADIANS_PER_DEGREE), -MathHelper.cos(this.getYaw() * MathHelper.RADIANS_PER_DEGREE));
                    this.tryAttack(this.getTarget());
                }
                if ((this.getAnimation() == ANIMATION_PUNCH_L) && this.getAnimationTick() == 6) {
                    float rot = getYaw() + 90;
                    attackTarget.takeKnockback(0.85F, MathHelper.sin(rot * MathHelper.RADIANS_PER_DEGREE), -MathHelper.cos(rot * MathHelper.RADIANS_PER_DEGREE));
                    this.tryAttack(this.getTarget());
                }
                if ((this.getAnimation() == ANIMATION_PUNCH_R) && this.getAnimationTick() == 6) {
                    float rot = getYaw() - 90;
                    attackTarget.takeKnockback(0.85F, MathHelper.sin(rot * MathHelper.RADIANS_PER_DEGREE), -MathHelper.cos(rot * MathHelper.RADIANS_PER_DEGREE));
                    this.tryAttack(this.getTarget());
                }
            }
            this.lookAtEntity(attackTarget, 360, 360);
        }
        if (this.isBaby() && attackTarget != null) {
            this.setTarget(null);
        }
        if (this.hasPassengers()) {
            this.dataTracker.set(POUCH_TICK, 10);
            this.setStanding(true);
            maxStandTime = 25;
        }

        if (this.hasVehicle()) {
            if (this.isBaby() && this.getVehicle() instanceof EntityKangaroo mount) {
                this.setYaw(mount.bodyYaw);
                this.headYaw= mount.bodyYaw;
                this.bodyYaw = mount.bodyYaw;
            }
            if (this.getVehicle() instanceof EntityKangaroo && !this.isBaby()) {
                this.dismountVehicle();
            }
        }
        if (clientArmorCooldown > 0) {
            clientArmorCooldown--;
        }
        if (this.age > 5 && !this.getWorld().isClient && clientArmorCooldown == 0 && this.isTamed()) {
            this.updateClientInventory();
            clientArmorCooldown = 20;
        }
        AnimationHandler.INSTANCE.updateAnimations(this);
    }

    @Override
    public boolean tryAttack(Entity entityIn) {
        boolean prev = super.tryAttack(entityIn);
        if (prev) {
            if (!this.getMainHandStack().isEmpty()) {
                damageItem(this.getMainHandStack());
            }
        }
        return prev;
    }

    @Override
    public boolean damage(DamageSource src, float amount) {
        boolean prev = super.damage(src, amount);
        if (prev) {
            if (!this.getEquippedStack(EquipmentSlot.HEAD).isEmpty()) {
                damageItem(this.getEquippedStack(EquipmentSlot.HEAD));
            }
            if (!this.getEquippedStack(EquipmentSlot.CHEST).isEmpty()) {
                damageItem(this.getEquippedStack(EquipmentSlot.CHEST));
            }
        }
        return prev;
    }

    private void damageItem(ItemStack stack) {
        if (stack != null) {
            stack.damage(1, this.getRandom(), null);
            if (stack.getDamage() <= 0) {
                stack.decrement(1);
            }
        }
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return super.isInvulnerableTo(source) || source.isOf(DamageTypes.IN_WALL);
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
    public MoveControl getMoveControl() {
        return this.moveControl;
    }

    @Override
    public void travel(Vec3d vec3d) {
        if (this.isSitting() || this.getAnimation() == ANIMATION_EAT_GRASS) {
            if (this.getNavigation().getCurrentPath() != null) {
                this.getNavigation().stop();
            }
            vec3d = Vec3d.ZERO;
        }
        super.travel(vec3d);
    }

    private void checkLandingDelay() {
        this.updateMoveTypeDuration();
        this.disableJumpControl();
    }

    @Override
    public EntityNavigation getNavigation() {
        return this.navigation;
    }

    @Nullable
    @Override
    public Entity getControllingVehicle() {
        return this.getVehicle() instanceof EntityKangaroo ? null : super.getControllingVehicle();
    }

    private void enableJumpControl() {
        if (jumpControl instanceof EntityKangaroo.JumpHelperController) {
            ((EntityKangaroo.JumpHelperController) this.jumpControl).setCanJump(true);
        }
    }

    private void disableJumpControl() {
        if (jumpControl instanceof EntityKangaroo.JumpHelperController) {
            ((EntityKangaroo.JumpHelperController) this.jumpControl).setCanJump(false);
        }
    }

    private void updateMoveTypeDuration() {
        if (this.moveControl.getSpeed() < 2D) {
            this.currentMoveTypeDuration = 2;
        } else {
            this.currentMoveTypeDuration = 1;
        }
    }

    private void calculateRotationYaw(double x, double z) {
        this.setYaw( (float) (MathHelper.atan2(z - this.getZ(), x - this.getX()) * (double) MathHelper.DEGREES_PER_RADIAN) - 90.0F);
    }

    @Override
    public boolean shouldSpawnSprintingParticles() {
        return false;
    }

    @Override
    public void mobTick() {
        super.mobTick();

        if (this.currentMoveTypeDuration > 0) {
            --this.currentMoveTypeDuration;
        }

        if (this.isOnGround()) {
            if (!this.wasOnGround) {
                this.setJumping(false);
                this.checkLandingDelay();
            }

            if (this.currentMoveTypeDuration == 0) {
                LivingEntity livingentity = this.getTarget();
                if (livingentity != null && this.squaredDistanceTo(livingentity) < 16.0D) {
                    this.calculateRotationYaw(livingentity.getX(), livingentity.getZ());
                    this.moveControl.moveTo(livingentity.getX(), livingentity.getY(), livingentity.getZ(), this.moveControl.getSpeed());
                    this.startJumping();
                    this.wasOnGround = true;
                }
            }
            if (this.jumpControl instanceof JumpHelperController rabbitController) {
                if (!rabbitController.getIsJumping()) {
                    if (this.moveControl.isMoving() && this.currentMoveTypeDuration == 0) {
                        this.startJumping();
                    }
                } else if (!rabbitController.canJump()) {
                    this.enableJumpControl();
                }
            }
        }

        this.wasOnGround = this.isOnGround();
    }

    public float getJumpCompletion(float partialTicks) {
        return this.jumpDuration == 0 ? 0.0F : ((float) this.jumpTicks + partialTicks) / (float) this.jumpDuration;
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
        if (animation == ANIMATION_KICK) {
            this.setStanding(true);
            maxStandTime = 30;
        } else if (animation == ANIMATION_PUNCH_R) {
            this.setStanding(true);
            maxStandTime = 15;
        } else if (animation == ANIMATION_PUNCH_L) {
            this.setStanding(true);
            maxStandTime = 15;
        }
    }

    @Override
    public Animation[] getAnimations() {
        return new Animation[]{ANIMATION_EAT_GRASS, ANIMATION_KICK, ANIMATION_PUNCH_L, ANIMATION_PUNCH_R};
    }

    @Nullable
    @Override
    public PassiveEntity createChild(ServerWorld world, PassiveEntity entity) {
        return AMEntityRegistry.KANGAROO.get().create(world);
    }

    public void setMovementSpeed(double newSpeed) {
        this.getNavigation().setSpeed(newSpeed);
        this.moveControl.moveTo(this.moveControl.getTargetX(), this.moveControl.getTargetY(), this.moveControl.getTargetZ(), newSpeed);
    }

    @Override
    protected float getJumpVelocity() {
        return 0.5F;
    }

    @Override
    public boolean handleFallDamage(float distance, float damageMultiplier, DamageSource source) {
        return false;
    }

    @Override
    protected void fall(double y, boolean onGroundIn, BlockState state, BlockPos pos) {
    }

    @Override
    public boolean isBreedingItem(ItemStack stack) {
        return stack.isIn(AMTagRegistry.KANGAROO_BREEDABLES);
    }

    public void resetKangarooSlots() {
        if (!this.getWorld().isClient) {
            int swordIndex = -1;
            double swordDamage = 0;
            int helmetIndex = -1;
            double helmetArmor = 0;
            int chestplateIndex = -1;
            double chestplateArmor = 0;
            for (int i = 0; i < this.kangarooInventory.size(); ++i) {
                var stack = this.kangarooInventory.getStack(i);
                if (!stack.isEmpty()) {
                    double dmg = getDamageForItem(stack);
                    if (dmg > 0 && dmg > swordDamage) {
                        swordDamage = dmg;
                        swordIndex = i;
                    }
                    if (MobEntity.getPreferredEquipmentSlot(stack) == EquipmentSlot.HEAD && !this.isBaby() && helmetIndex == -1) {
                        helmetIndex = i;
                    }
                    if (stack.getItem() instanceof ArmorItem armorItem && !this.isBaby()) {
                        if (armorItem.getSlotType() == EquipmentSlot.HEAD) {
                            double prot = getProtectionForItem(stack, EquipmentSlot.HEAD);
                            if (prot > 0 && prot > helmetArmor) {
                                helmetArmor = prot;
                                helmetIndex = i;
                            }
                        }
                        if (armorItem.getSlotType() == EquipmentSlot.CHEST) {
                            double prot = getProtectionForItem(stack, EquipmentSlot.CHEST);
                            if (prot > 0 && prot > chestplateArmor) {
                                chestplateArmor = prot;
                                chestplateIndex = i;
                            }
                        }
                    }
                }
            }
            this.dataTracker.set(SWORD_INDEX, swordIndex);
            this.dataTracker.set(CHEST_INDEX, chestplateIndex);
            this.dataTracker.set(HELMET_INDEX, helmetIndex);
            updateClientInventory();
        }
    }

    private void updateClientInventory() {
        if (!this.getWorld().isClient) {
            for (int i = 0; i < 9; i++) {
                AlexsMobs.sendMSGToAll(new KangarooInventorySyncPacket(this.getId(), i, kangarooInventory.getStack(i)));
            }
        }
    }

    @Nullable
    private Map<EquipmentSlot, ItemStack> getEquipmentChanges() {
        Map<EquipmentSlot, ItemStack> map = null;

        for (var equipmentslottype : EquipmentSlot.values()) {
            ItemStack itemstack;
            switch (equipmentslottype.getType()) {
                case HAND -> itemstack = this.getItemInHand(equipmentslottype);
                case ARMOR -> itemstack = this.getArmorInSlot(equipmentslottype);
                default -> {
                    continue;
                }
            }

            ItemStack itemstack1 = this.getEquippedStack(equipmentslottype);
            //FIXME forge
//            if (!ItemStack.matches(itemstack1, itemstack)) {
//                net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent(this, equipmentslottype, itemstack, itemstack1));
//                if (map == null) {
//                    map = Maps.newEnumMap(EquipmentSlot.class);
//                }
//
//                map.put(equipmentslottype, itemstack1);
//                if (!itemstack.isEmpty()) {
//                    this.getAttributes().removeAttributeModifiers(itemstack.getAttributeModifiers(equipmentslottype));
//                }
//
//                if (!itemstack1.isEmpty()) {
//                    this.getAttributes().addTransientAttributeModifiers(itemstack1.getAttributeModifiers(equipmentslottype));
//                }
//            }
        }

        return map;
    }

    public ItemStack getEquippedStack(EquipmentSlot slotIn) {
        return switch (slotIn.getType()) {
            case HAND -> getItemInHand(slotIn);
            case ARMOR -> getArmorInSlot(slotIn);
        };
    }

    private ItemStack getArmorInSlot(EquipmentSlot slot) {
        int helmIndex = this.dataTracker.get(HELMET_INDEX);
        int chestIndex = this.dataTracker.get(CHEST_INDEX);
        return slot == EquipmentSlot.HEAD && helmIndex >= 0 ? kangarooInventory.getStack(helmIndex) : slot == EquipmentSlot.CHEST && chestIndex >= 0 ? kangarooInventory.getStack(chestIndex) : ItemStack.EMPTY;
    }

    private ItemStack getItemInHand(EquipmentSlot slot) {
        int index = this.dataTracker.get(SWORD_INDEX);
        return slot == EquipmentSlot.MAINHAND && index >= 0 ? kangarooInventory.getStack(index) : ItemStack.EMPTY;
    }

    public double getDamageForItem(ItemStack itemStack) {
        var map = itemStack.getAttributeModifiers(EquipmentSlot.MAINHAND);
        if (!map.isEmpty()) {
            double d = 0;
            for (var mod : map.get(EntityAttributes.GENERIC_ATTACK_DAMAGE)) {
                d += mod.getValue();
            }
            return d;
        }
        return 0;
    }

    public double getProtectionForItem(ItemStack itemStack, EquipmentSlot type) {
        var map = itemStack.getAttributeModifiers(type);
        if (!map.isEmpty()) {
            double d = 0;
            for (var mod : map.get(EntityAttributes.GENERIC_ARMOR)) {
                d += mod.getValue();
            }
            return d;
        }
        return 0;
    }

    @Override
    protected void jump() {
        super.jump();

        if (!this.getWorld().isClient) {
            this.getWorld().sendEntityStatus(this, (byte) 1);
        }
    }

    public boolean hasJumper() {
        return jumpControl instanceof JumpHelperController;
    }

    public void startJumping() {
        if (!this.isSitting() || this.isTouchingWater()) {
            this.setJumping(true);
            this.jumpDuration = 16;
            this.jumpTicks = 0;
        }

    }

    @Environment(EnvType.CLIENT)
    public void handleStatus(byte id) {
        if (id == 1) {
            this.spawnSprintingParticles();
            this.jumpDuration = 16;
            this.jumpTicks = 0;
        } else {
            super.handleStatus(id);
        }
    }

    @Override
    public boolean shouldFollow() {
        return this.getCommand() == 1;
    }

    @Override
    public void onInventoryChanged(Inventory iInventory) {
        this.resetKangarooSlots();
    }

    @Override
    public EntityView method_48926() {
        return getWorld();
    }

    static class MoveHelperController extends MoveControl {
        private final EntityKangaroo kangaroo;
        private double nextJumpSpeed;

        public MoveHelperController(EntityKangaroo kangaroo) {
            super(kangaroo);
            this.kangaroo = kangaroo;
        }

        @Override
        public void tick() {
            if (this.kangaroo.hasJumper() && this.kangaroo.isOnGround() && !this.kangaroo.jumping && !((EntityKangaroo.JumpHelperController) this.kangaroo.jumpControl).getIsJumping()) {
                this.kangaroo.setMovementSpeed(0.0D);
            } else if (this.isMoving()) {
                this.kangaroo.setMovementSpeed(this.nextJumpSpeed);
            }

            super.tick();
        }

        /**
         * Sets the speed and location to move to
         */
        @Override
        public void moveTo(double x, double y, double z, double speedIn) {
            if (this.kangaroo.isTouchingWater()) {
                speedIn = 1.5D;
            }

            super.moveTo(x, y, z, speedIn);
            if (speedIn > 0.0D) {
                this.nextJumpSpeed = speedIn;
            }

        }
    }

    public static class JumpHelperController extends JumpControl {
        private final EntityKangaroo kangaroo;
        private boolean canJump;

        public JumpHelperController(EntityKangaroo kangaroo) {
            super(kangaroo);
            this.kangaroo = kangaroo;
        }

        public boolean getIsJumping() {
            return this.active;
        }

        public boolean canJump() {
            return this.canJump;
        }

        public void setCanJump(boolean canJumpIn) {
            this.canJump = canJumpIn;
        }

        @Override
        public void tick() {
            if (this.active) {
                this.kangaroo.startJumping();
                this.active = false;
            }
        }
    }
}
