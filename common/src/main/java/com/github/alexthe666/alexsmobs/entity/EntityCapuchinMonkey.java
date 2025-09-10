package com.github.alexthe666.alexsmobs.entity;

import com.github.alexthe666.alexsmobs.config.AMConfig;
import com.github.alexthe666.alexsmobs.entity.ai.*;
import com.github.alexthe666.alexsmobs.entity.util.Maths;
import com.github.alexthe666.alexsmobs.misc.IngredientUtil;
import com.github.alexthe666.alexsmobs.registry.*;
import com.iafenvoy.uranus.animation.Animation;
import com.iafenvoy.uranus.animation.AnimationHandler;
import com.iafenvoy.uranus.animation.IAnimatedEntity;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.recipe.Ingredient;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.*;
import net.minecraft.world.event.GameEvent;
import org.jetbrains.annotations.Nullable;

public class EntityCapuchinMonkey extends TameableEntity implements IAnimatedEntity, IFollower, ITargetsDroppedItems {

    public static final Animation ANIMATION_THROW = Animation.create(12);
    public static final Animation ANIMATION_HEADTILT = Animation.create(15);
    public static final Animation ANIMATION_SCRATCH = Animation.create(20);

    protected static final TrackedData<Boolean> DART = DataTracker.registerData(EntityCapuchinMonkey.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> SITTING = DataTracker.registerData(EntityCapuchinMonkey.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Integer> COMMAND = DataTracker.registerData(EntityCapuchinMonkey.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> VARIANT = DataTracker.registerData(EntityCapuchinMonkey.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> DART_TARGET = DataTracker.registerData(EntityCapuchinMonkey.class, TrackedDataHandlerRegistry.INTEGER);
    public float prevSitProgress;
    public float sitProgress;
    public boolean forcedSit = false;
    public boolean attackDecision = false;//true for ranged, false for melee
    private int animationTick;
    private Animation currentAnimation;
    private int sittingTime = 0;
    private int maxSitTime = 75;
    private boolean hasSlowed = false;
    private int rideCooldown = 0;
    private Ingredient temptItems = null;

    public EntityCapuchinMonkey(EntityType<? extends EntityCapuchinMonkey> type, World worldIn) {
        super(type, worldIn);
        this.setPathfindingPenalty(PathNodeType.LEAVES, 0.0F);
    }

    public static boolean isTameableFood(ItemStack stack) {
        return stack.isIn(AMTagRegistry.CAPUCHIN_MONKEY_TAMEABLES);
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createLivingAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 10.0D)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 2.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.4F)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32.0D);
    }

    public static <T extends MobEntity> boolean canCapuchinSpawn(EntityType<EntityCapuchinMonkey> gorilla, WorldAccess worldIn, SpawnReason reason, BlockPos p_223317_3_, Random random) {
        BlockState blockstate = worldIn.getBlockState(p_223317_3_.down());
        return (blockstate.isIn(AMTagRegistry.CAPUCHIN_MONKEY_SPAWNS) || blockstate.isOf(Blocks.AIR)) && worldIn.getBaseLightLevel(p_223317_3_, 0) > 8;
    }

    @Override
    public int getLimitPerChunk() {
        return 8;
    }

    @Override
    public boolean spawnsTooManyForEachTry(int sizeIn) {
        return false;
    }

    @Override
    public boolean canSpawn(WorldAccess worldIn, SpawnReason spawnReasonIn) {
        return AMEntityRegistry.rollSpawn(AMConfig.capuchinMonkeySpawnRolls, this.getRandom(), spawnReasonIn);
    }

    public Ingredient getAllFoods(){
        if(temptItems == null){
            temptItems = IngredientUtil.ingredientFromTags(AMTagRegistry.CAPUCHIN_MONKEY_BREEDABLES, AMTagRegistry.CAPUCHIN_MONKEY_FOODSTUFFS);
        }
        return temptItems;
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        if (this.isInvulnerableTo(source)) {
            return false;
        } else {
            var entity = source.getAttacker();
            if (entity != null && this.isTamed() && !(entity instanceof PlayerEntity) && !(entity instanceof PersistentProjectileEntity)) {
                amount = (amount + 1.0F) / 4.0F;
            }
            return super.damage(source, amount);
        }
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(1, new SwimGoal(this));
        this.goalSelector.add(2, new SitGoal(this));
        this.goalSelector.add(3, new CapuchinAIMelee(this, 1, true));
        this.goalSelector.add(3, new CapuchinAIRangedAttack(this, 1, 20, 15));
        this.goalSelector.add(6, new TameableAIFollowOwner(this, 1.0D, 10.0F, 2.0F, false));
        this.goalSelector.add(4, new TemptGoal(this, 1.1D, Ingredient.fromTag(AMTagRegistry.CAPUCHIN_MONKEY_TAMEABLES), true) {
            @Override
            public void tick() {
                super.tick();
                if (this.mob.squaredDistanceTo(this.closestPlayer) < 6.25D && this.mob.getRandom().nextInt(14) == 0) {
                    ((EntityCapuchinMonkey) this.mob).setAnimation(ANIMATION_HEADTILT);
                }
            }
        });
        this.goalSelector.add(7, new AnimalMateGoal(this, 1.0D));
        this.goalSelector.add(8, new WanderAroundGoal(this, 1.0D, 60));
        this.goalSelector.add(10, new LookAtEntityGoal(this, PlayerEntity.class, 8.0F));
        this.goalSelector.add(10, new LookAroundGoal(this));
        this.targetSelector.add(1, new CreatureAITargetItems<>(this, false));
        this.targetSelector.add(2, new TrackOwnerAttackerGoal(this));
        this.targetSelector.add(3, new AttackWithOwnerGoal(this));
        this.targetSelector.add(4, (new RevengeGoal(this, EntityCapuchinMonkey.class, EntityTossedItem.class)).setGroupRevenge());
        this.targetSelector.add(5, new CapuchinAITargetBalloons(this, true));
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return AMSoundRegistry.CAPUCHIN_MONKEY_IDLE.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return AMSoundRegistry.CAPUCHIN_MONKEY_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return AMSoundRegistry.CAPUCHIN_MONKEY_HURT.get();
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
    public void writeCustomDataToNbt(NbtCompound compound) {
        super.writeCustomDataToNbt(compound);
        compound.putBoolean("MonkeySitting", this.isSitting());
        compound.putBoolean("HasDart", this.hasDart());
        compound.putBoolean("ForcedToSit", this.forcedSit);
        compound.putInt("Command", this.getCommand());
        compound.putInt("Variant", this.getVariant());
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound compound) {
        super.readCustomDataFromNbt(compound);
        this.setSitting(compound.getBoolean("MonkeySitting"));
        this.forcedSit = compound.getBoolean("ForcedToSit");
        this.setCommand(compound.getInt("Command"));
        this.setDart(compound.getBoolean("HasDart"));
        this.setVariant(compound.getInt("Variant"));
    }

    @Override
    public void tick() {
        super.tick();
        this.prevSitProgress = this.sitProgress;
        if (this.isSitting()) {
            if (sitProgress < 10F)
                sitProgress++;
        } else {
            if (sitProgress > 0F)
                sitProgress--;
        }

        if (!forcedSit && isSitting() && ++sittingTime > maxSitTime) {
            this.setSitting(false);
            sittingTime = 0;
            maxSitTime = 75 + random.nextInt(50);
        }
        if (!this.getWorld().isClient && this.getAnimation() == NO_ANIMATION && !this.isSitting() && this.getCommand() != 1 && random.nextInt(1500) == 0) {
            maxSitTime = 300 + random.nextInt(250);
            this.setSitting(true);
        }
        this.setStepHeight(2);
        if (!forcedSit && this.isSitting() && (this.getDartTarget() != null || this.getCommand() == 1)) {
            this.setSitting(false);
        }

        if (!this.getWorld().isClient) {
            if (this.getTarget() != null && this.getAnimation() == ANIMATION_SCRATCH && this.getAnimationTick() == 10) {
                float f1 = this.getYaw() * MathHelper.RADIANS_PER_DEGREE;
                this.setVelocity(this.getVelocity().add(-MathHelper.sin(f1) * 0.3F, 0.0D, MathHelper.cos(f1) * 0.3F));
                getTarget().takeKnockback(1F, getTarget().getX() - this.getX(), getTarget().getZ() - this.getZ());
                this.getTarget().damage(this.getDamageSources().mobAttack(this), (float) this.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE).getBaseValue());
                this.setAttackDecision(this.getTarget());
            }
            if (this.getDartTarget() != null && this.getDartTarget().isAlive() && this.getAnimation() == ANIMATION_THROW && this.getAnimationTick() == 5) {
                final Vec3d vector3d = this.getDartTarget().getVelocity();
                final double d0 = this.getDartTarget().getX() + vector3d.x - this.getX();
                final double d1 = this.getDartTarget().getEyeY() - (double) 1.1F - this.getY();
                final double d2 = this.getDartTarget().getZ() + vector3d.z - this.getZ();
                final float f = MathHelper.sqrt((float)(d0 * d0 + d2 * d2));
                EntityTossedItem tossedItem = new EntityTossedItem(this.getWorld(), this);
                tossedItem.setDart(this.hasDart());
                tossedItem.setPitch(tossedItem.getPitch() - 20F);
                tossedItem.setVelocity(d0, d1 + (double) (f * 0.2F), d2, hasDart() ? 1.15F : 0.75F, 8.0F);
                if (!this.isSilent()) {
                    this.getWorld().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ENTITY_WITCH_THROW, this.getSoundCategory(), 1.0F, 0.8F + this.random.nextFloat() * 0.4F);
                    this.emitGameEvent(GameEvent.PROJECTILE_SHOOT);
                }
                this.getWorld().spawnEntity(tossedItem);
                this.setAttackDecision(this.getDartTarget());
            }
        }
        if (rideCooldown > 0) {
            rideCooldown--;
        }
        if (!this.getWorld().isClient && getAnimation() == NO_ANIMATION && this.getRandom().nextInt(300) == 0) {
            setAnimation(ANIMATION_HEADTILT);
        }
        if (!this.getWorld().isClient && this.isSitting()) {
            this.getNavigation().stop();
        }
        AnimationHandler.INSTANCE.updateAnimations(this);
    }

    @Override
    protected void fall(double y, boolean onGroundIn, BlockState state, BlockPos pos) {
    }

    @Override
    public boolean tryAttack(Entity entityIn) {
        if (this.getAnimation() == NO_ANIMATION) {
            this.setAnimation(ANIMATION_SCRATCH);
        }
        return true;
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

    @Override
    protected void dropInventory() {
        super.dropInventory();
        if (hasDart()) {
            this.dropItem(AMItemRegistry.ANCIENT_DART.get());
        }
    }

    @Override
    public void tickRiding() {
        final Entity entity = this.getVehicle();
        if (this.hasVehicle() && !entity.isAlive()) {
            this.stopRiding();
        } else if (isTamed() && entity instanceof LivingEntity && isOwner((LivingEntity) entity)) {
            this.setVelocity(0, 0, 0);
            this.tick();
            if (this.hasVehicle()) {
                final Entity mount = this.getVehicle();
                if (mount instanceof final PlayerEntity player) {
                    this.bodyYaw = player.bodyYaw;
                    this.setYaw(player.getYaw());
                    this.headYaw= player.headYaw;
                    this.prevYaw = player.headYaw;
                    final float radius = 0F;
                    final float angle = (Maths.STARTING_ANGLE * (((LivingEntity) mount).bodyYaw - 180F));
                    final double extraX = radius * MathHelper.sin(MathHelper.PI + angle);
                    final double extraZ = radius * MathHelper.cos(angle);
                    this.setPos(mount.getX() + extraX, Math.max(mount.getY() + mount.getHeight() + 0.1, mount.getY()), mount.getZ() + extraZ);
                    attackDecision = true;
                    if (!mount.isAlive() || rideCooldown == 0 && mount.isSneaking()) {
                        this.dismountVehicle();
                        attackDecision = false;
                    }
                }
            }
        } else {
            super.tickRiding();
        }

    }

    public void setAttackDecision(Entity target) {
        if (target instanceof HostileEntity || this.hasDart()) {
            attackDecision = true;
        } else {
            attackDecision = !attackDecision;
        }
    }

    public int getCommand() {
        return this.dataTracker.get(COMMAND);
    }

    public void setCommand(int command) {
        this.dataTracker.set(COMMAND, command);
    }

    public boolean isSitting() {
        return this.dataTracker.get(SITTING);
    }

    public void setSitting(boolean sit) {
        this.dataTracker.set(SITTING, sit);
    }

    public boolean hasDartTarget() {
        return this.dataTracker.get(DART_TARGET) != -1 && this.hasDart();
    }

    public void setDartTarget(Entity entity) {
        this.dataTracker.set(DART_TARGET, entity == null ? -1 : entity.getId());
        if (entity instanceof LivingEntity target) {
            this.setTarget(target);
        }
    }

    @Nullable
    public Entity getDartTarget() {
        if (!this.hasDartTarget()) {
            return this.getTarget();
        } else {
            Entity entity = this.getWorld().getEntityById(this.dataTracker.get(DART_TARGET));
            if(entity == null || !entity.isAlive()){
                return this.getTarget();
            }else{
                return entity;
            }
        }
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(COMMAND, 0);
        this.dataTracker.startTracking(DART_TARGET, -1);
        this.dataTracker.startTracking(SITTING, false);
        this.dataTracker.startTracking(DART, false);
        this.dataTracker.startTracking(VARIANT, 0);
    }

    public boolean hasDart() {
        return this.dataTracker.get(DART);
    }

    public void setDart(boolean dart) {
        this.dataTracker.set(DART, dart);
    }

    public int getVariant() {
        return this.dataTracker.get(VARIANT);
    }

    public void setVariant(int variant) {
        this.dataTracker.set(VARIANT, Integer.valueOf(variant));
    }

    @Nullable
    @Override
    public PassiveEntity createChild(ServerWorld world, PassiveEntity entity) {
        EntityCapuchinMonkey monkey = AMEntityRegistry.CAPUCHIN_MONKEY.get().create(world);
        monkey.setVariant(this.getVariant());
        return monkey;
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
    public int getAnimationTick() {
        return animationTick;
    }

    @Override
    public void setAnimationTick(int tick) {
        animationTick = tick;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return source.isOf(DamageTypes.IN_WALL)  || super.isInvulnerableTo(source);
    }

    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        final ItemStack itemstack = player.getStackInHand(hand);
        if (isTameableFood(itemstack)) {
            if (!isTamed()) {
                this.eat(player, hand, itemstack);
                if (getRandom().nextInt(5) == 0) {
                    this.setOwner(player);
                    this.getWorld().sendEntityStatus(this, (byte) 7);
                } else {
                    this.getWorld().sendEntityStatus(this, (byte) 6);
                }
                return ActionResult.SUCCESS;
            }
            if (isTamed() && (getAllFoods().test(itemstack) && !isBreedingItem(itemstack)) && this.getHealth() < this.getMaxHealth()) {
                this.eat(player, hand, itemstack);
                this.emitGameEvent(GameEvent.EAT);
                this.playSound(SoundEvents.ENTITY_CAT_EAT, this.getSoundVolume(), this.getSoundPitch());
                this.heal(5);
                return ActionResult.SUCCESS;
            }
        }

        final var actionResult = itemstack.useOnEntity(player, this, hand);
        final var type = super.interactMob(player, hand);
        if (actionResult != ActionResult.SUCCESS && type != ActionResult.SUCCESS && isTamed() && isOwner(player) && !isBreedingItem(itemstack) && !isTameableFood(itemstack) && !getAllFoods().test(itemstack)) {
            if (!this.hasDart() && itemstack.getItem() == AMItemRegistry.ANCIENT_DART.get()) {
                this.setDart(true);
                this.eat(player, hand, itemstack);
                return ActionResult.CONSUME;
            }
            // FIXME forge
//            if (this.hasDart() && itemstack.is(Tags.Items.SHEARS)) {
//                this.setDart(false);
//                itemstack.hurtAndBreak(1, this, (p_233654_0_) -> {
//                });
//                return ActionResult.SUCCESS;
//            }
            if (player.isSneaking() && player.getPassengerList().isEmpty()) {
                this.startRiding(player);
                rideCooldown = 20;
                return ActionResult.SUCCESS;
            } else {
                this.setCommand(this.getCommand() + 1);
                if (this.getCommand() == 3) {
                    this.setCommand(0);
                }
                player.sendMessage(Text.translatable("entity.alexsmobs.all.command_" + this.getCommand(), this.getName()), true);
                final boolean sit = this.getCommand() == 2;
                if (sit) {
                    this.forcedSit = true;
                    this.setSitting(true);
                } else {
                    this.forcedSit = false;
                    this.setSitting(false);
                }
                return ActionResult.SUCCESS;
            }
        }
        return type;
    }

    @Override
    public Animation[] getAnimations() {
        return new Animation[]{ANIMATION_THROW, ANIMATION_SCRATCH};
    }

    @Override
    public boolean shouldFollow() {
        return this.getCommand() == 1;
    }

    @Override
    public boolean canTargetItem(ItemStack stack) {
        return getAllFoods().test(stack) || isTameableFood(stack);
    }

    @Override
    public boolean isBreedingItem(ItemStack stack) {
        Item item = stack.getItem();
        return isTamed() && stack.isIn(AMTagRegistry.CAPUCHIN_MONKEY_BREEDABLES);
    }

    @Override
    public void onGetItem(ItemEntity e) {
        this.heal(5);
        this.emitGameEvent(GameEvent.EAT);
        this.playSound(SoundEvents.ENTITY_CAT_EAT, this.getSoundVolume(), this.getSoundPitch());
        if (e.getStack().isIn(AMTagRegistry.BANANAS)) {
            if (getRandom().nextInt(4) == 0) {
                this.dropStack(new ItemStack(AMBlockRegistry.BANANA_PEEL.get()));
            }
        }

        Entity itemThrower = e.getOwner();
        if (e.getStack().isIn(AMTagRegistry.CAPUCHIN_MONKEY_TAMEABLES) && itemThrower != null && !this.isTamed()) {
            if (getRandom().nextInt(5) == 0) {
                this.setTamed(true);
                this.setOwnerUuid(itemThrower.getUuid());
                this.getWorld().sendEntityStatus(this, (byte) 7);
            } else {
                this.getWorld().sendEntityStatus(this, (byte) 6);
            }
        }
    }

    @Nullable
    @Override
    public EntityData initialize(ServerWorldAccess world, LocalDifficulty diff, SpawnReason spawnType, @Nullable EntityData data, @Nullable NbtCompound tag) {
        int i;
        if (data instanceof CapuchinGroupData) {
            i = ((CapuchinGroupData)data).variant;
        } else {
            i = this.random.nextInt(4);
            data = new CapuchinGroupData(i);
        }

        this.setVariant(i);
        return super.initialize(world, diff, spawnType, data, tag);
    }

    @Override
    public EntityView method_48926() {
        return getWorld();
    }

    public static class CapuchinGroupData extends PassiveEntity.PassiveData {

        public final int variant;

        CapuchinGroupData(int variant) {
            super(true);
            this.variant = variant;
        }
    }
}
