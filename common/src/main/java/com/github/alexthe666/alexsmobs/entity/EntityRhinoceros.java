package com.github.alexthe666.alexsmobs.entity;

import com.github.alexthe666.alexsmobs.config.AMConfig;
import com.github.alexthe666.alexsmobs.entity.ai.AdvancedPathNavigateNoTeleport;
import com.github.alexthe666.alexsmobs.entity.ai.AnimalAIHurtByTargetNotBaby;
import com.github.alexthe666.alexsmobs.entity.ai.AnimalAIPanicBaby;
import com.github.alexthe666.alexsmobs.entity.ai.AnimalAIWanderRanged;
import com.github.alexthe666.alexsmobs.misc.IngredientUtil;
import com.github.alexthe666.alexsmobs.registry.AMEntityRegistry;
import com.github.alexthe666.alexsmobs.registry.AMSoundRegistry;
import com.github.alexthe666.alexsmobs.registry.AMTagRegistry;
import com.google.common.collect.Lists;
import com.iafenvoy.uranus.animation.Animation;
import com.iafenvoy.uranus.animation.AnimationHandler;
import com.iafenvoy.uranus.animation.IAnimatedEntity;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.raid.RaiderEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtList;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionUtil;
import net.minecraft.potion.Potions;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.event.GameEvent;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

public class EntityRhinoceros extends AnimalEntity implements IAnimatedEntity {

    public static final Animation ANIMATION_FLICK_EARS = Animation.create(20);
    public static final Animation ANIMATION_EAT_GRASS = Animation.create(35);
    public static final Animation ANIMATION_FLING = Animation.create(15);
    public static final Animation ANIMATION_SLASH = Animation.create(30);
    private static final TrackedData<String> APPLIED_POTION = DataTracker.registerData(EntityRhinoceros.class, TrackedDataHandlerRegistry.STRING);
    private static final TrackedData<Integer> POTION_LEVEL = DataTracker.registerData(EntityRhinoceros.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> INFLICTED_COUNT = DataTracker.registerData(EntityRhinoceros.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> POTION_DURATION = DataTracker.registerData(EntityRhinoceros.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Optional<UUID>> DATA_TRUSTED_ID_0 = DataTracker.registerData(EntityRhinoceros.class, TrackedDataHandlerRegistry.OPTIONAL_UUID);
    private static final TrackedData<Optional<UUID>> DATA_TRUSTED_ID_1 = DataTracker.registerData(EntityRhinoceros.class, TrackedDataHandlerRegistry.OPTIONAL_UUID);
    private static final TrackedData<Boolean> ANGRY = DataTracker.registerData(EntityRhinoceros.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final Object2IntMap<String> potionToColor = new Object2IntOpenHashMap<>();
    private int animationTick;
    private Animation currentAnimation;

    public EntityRhinoceros(EntityType<EntityRhinoceros> type, World level) {
        super(type, level);
        this.setStepHeight(1.1F);
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createLivingAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 60.0D)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 8.0D)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.25F)
                .add(EntityAttributes.GENERIC_ARMOR, 12.0D)
                .add(EntityAttributes.GENERIC_ARMOR_TOUGHNESS, 4.0D)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.9D)
                .add(EntityAttributes.GENERIC_ATTACK_KNOCKBACK, 2.0D);
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(DATA_TRUSTED_ID_0, Optional.empty());
        this.dataTracker.startTracking(DATA_TRUSTED_ID_1, Optional.empty());
        this.dataTracker.startTracking(APPLIED_POTION, "");
        this.dataTracker.startTracking(POTION_LEVEL, 0);
        this.dataTracker.startTracking(INFLICTED_COUNT, 0);
        this.dataTracker.startTracking(POTION_DURATION, 0);
        this.dataTracker.startTracking(ANGRY, false);
    }

    @Override
    protected void initGoals() {
        super.initGoals();
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new MeleeAttackGoal(this, 1.4D, true));
        this.goalSelector.add(2, new AnimalAIPanicBaby(this, 1.25D));
        this.goalSelector.add(3, new AnimalMateGoal(this, 1.0D));
        this.goalSelector.add(4, new TemptGoal(this, 1.0D, IngredientUtil.ingredientFromTags(AMTagRegistry.RHINOCEROS_FOODSTUFFS, AMTagRegistry.RHINOCEROS_BREEDABLES), false));
        this.goalSelector.add(5, new FollowParentGoal(this, 1.1D));
        this.goalSelector.add(6, new AnimalAIWanderRanged(this, 90, 1.0D, 18, 7));
        this.goalSelector.add(7, new StrollGoal(200));
        this.goalSelector.add(8, new LookAtEntityGoal(this, PlayerEntity.class, 15.0F));
        this.goalSelector.add(8, new LookAroundGoal(this));
        this.targetSelector.add(1, new DefendTrustedTargetGoal(LivingEntity.class, false, false, (entity) -> !this.trusts(entity.getUuid())));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, RaiderEntity.class, 50, true, true, null){
            @Override
            public boolean canStart(){
                return super.canStart() && !EntityRhinoceros.this.isBaby();
            }
        });
        this.targetSelector.add(3, (new AIAttackNearPlayers()));
        this.targetSelector.add(4, (new AnimalAIHurtByTargetNotBaby(this)));
    }

    @Override
    protected EntityNavigation createNavigation(World worldIn) {
        return new AdvancedPathNavigateNoTeleport(this, worldIn, true);
    }

    @Override
    public boolean canSpawn(WorldAccess worldIn, SpawnReason spawnReasonIn) {
        return AMEntityRegistry.rollSpawn(AMConfig.rhinocerosSpawnRolls, this.getRandom(), spawnReasonIn);
    }

    @Override
    public void tick() {
        super.tick();
        AnimationHandler.INSTANCE.updateAnimations(this);
        if (!this.getWorld().isClient) {
            if (this.getAnimation() == NO_ANIMATION && (this.getTarget() == null || !this.getTarget().isAlive())) {
                if (this.getVelocity().lengthSquared() < 0.03D && (getRandom().nextInt(500) == 0 && getWorld().getBlockState(this.getBlockPos().down()).isOf(Blocks.GRASS_BLOCK))) {
                    this.setAnimation(ANIMATION_EAT_GRASS);
                } else if (getRandom().nextInt(200) == 0) {
                    this.setAnimation(ANIMATION_FLICK_EARS);
                }
            }
            if (this.getAnimation() == ANIMATION_EAT_GRASS && this.getAnimationTick() == 30 && getWorld().getBlockState(this.getBlockPos().down()).isOf(Blocks.GRASS_BLOCK)) {
                BlockPos down = this.getBlockPos().down();
                this.getWorld().syncWorldEvent(2001, down, Block.getRawIdFromState(Blocks.GRASS_BLOCK.getDefaultState()));
                this.getWorld().setBlockState(down, Blocks.DIRT.getDefaultState(), 2);
                this.heal(10);
            }
            LivingEntity target = this.getTarget();
            if (target != null && target.isAlive()) {
                this.setAngry(this.distanceTo(target) < 20);
                double dist = this.distanceTo(target);
                if (canSee(target)) {
                    this.lookAtEntity(target, 30, 30);
                    this.bodyYaw = this.getYaw();
                }
                if (dist < this.getWidth() + 3.0F) {
                    if (this.getAnimation() == NO_ANIMATION) {
                        this.setAnimation(random.nextBoolean() ? ANIMATION_SLASH : ANIMATION_FLING);
                    }
                    if(dist < this.getWidth() + 1.5F && this.canSee(target)){
                        if (this.getAnimation() == ANIMATION_FLING && this.getAnimationTick() >= 5 && this.getAnimationTick() <= 8) {
                            float dmg = (float) this.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE).getBaseValue();
                            if (target instanceof RaiderEntity) {
                                dmg = 10;
                            }
                            attackWithPotion(target, dmg);
                            launch(target, 0, 1F);
                            for (var entity : this.getWorld().getNonSpectatingEntities(LivingEntity.class, this.getBoundingBox().expand(1.0D))) {
                                if(!(entity instanceof AnimalEntity) && !trusts(entity.getUuid()) && entity != target){
                                    attackWithPotion(entity, Math.max(dmg - 5, 1));
                                    launch(entity, 0, 0.5F);
                                }
                            }
                        }
                        if (this.getAnimation() == ANIMATION_SLASH && (this.getAnimationTick() >= 9 && this.getAnimationTick() <= 11 || this.getAnimationTick() >= 19 && this.getAnimationTick() <= 21)) {
                            float dmg = (float) this.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE).getBaseValue();
                            if (target instanceof RaiderEntity) {
                                dmg = 10;
                            }
                            attackWithPotion(target, dmg);
                            launch(target, this.getAnimationTick() <= 15 ? -90 : 90, 1F);
                            for (var entity : this.getWorld().getNonSpectatingEntities(LivingEntity.class, this.getBoundingBox().expand(1.0D))) {
                                if(!(entity instanceof AnimalEntity) && !trusts(entity.getUuid()) && entity != target){
                                    attackWithPotion(entity, Math.max(dmg - 5, 1));
                                    launch(entity, this.getAnimationTick() <= 15 ? -90 : 90, 0.5F);
                                }
                            }
                        }
                    }
                }
            }else{
                this.setAngry(false);
            }
        }
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        if (!isBaby()) {
            this.playSound(AMSoundRegistry.ELEPHANT_WALK.get(), 0.2F, 1.2F);
        } else {
            super.playStepSound(pos, state);
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return AMSoundRegistry.RHINOCEROS_IDLE.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return AMSoundRegistry.RHINOCEROS_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return AMSoundRegistry.RHINOCEROS_HURT.get();
    }

    @Override
    public boolean isBreedingItem(ItemStack stack) {
        return stack.isIn(AMTagRegistry.RHINOCEROS_BREEDABLES);
    }

    public String getAppliedPotionId() {
        return this.dataTracker.get(APPLIED_POTION);
    }

    public void setAppliedPotionId(String potionId) {
        this.dataTracker.set(APPLIED_POTION, potionId);
    }

    public int getPotionColor() {
        String s = this.getAppliedPotionId();
        if (s.isEmpty()) {
            return -1;
        } else {
            if (!potionToColor.containsKey(s)) {
                var effect = getPotionEffect();
                if (effect != null) {
                    int color = effect.getColor();
                    potionToColor.put(s, color);
                    return color;
                }
                return -1;
            } else {
                return potionToColor.getInt(s);
            }
        }
    }

    public StatusEffect getPotionEffect() {
        return Registries.STATUS_EFFECT.get(new Identifier(this.getAppliedPotionId()));
    }

    public int getPotionDuration() {
        return this.dataTracker.get(POTION_DURATION);
    }

    public void setPotionDuration(int time) {
        this.dataTracker.set(POTION_DURATION, time);
    }

    public int getPotionLevel() {
        return this.dataTracker.get(POTION_LEVEL);
    }

    public void setPotionLevel(int time) {
        this.dataTracker.set(POTION_LEVEL, time);
    }

    public int getInflictedCount() {
        return this.dataTracker.get(INFLICTED_COUNT);
    }

    public void setInflictedCount(int count) {
        this.dataTracker.set(INFLICTED_COUNT, count);
    }

    public void resetPotion() {
        this.setAppliedPotionId("");
        this.setPotionDuration(0);
        this.setPotionLevel(0);
        this.setInflictedCount(0);
    }

    private List<UUID> getTrustedUUIDs() {
        List<UUID> list = Lists.newArrayList();
        list.add((UUID)((Optional)this.dataTracker.get(DATA_TRUSTED_ID_0)).orElse(null));
        list.add((UUID)((Optional)this.dataTracker.get(DATA_TRUSTED_ID_1)).orElse(null));
        return list;
    }

    private void addTrustedUUID(@Nullable UUID p_28516_) {
        if (this.dataTracker.get(DATA_TRUSTED_ID_0).isPresent()) {
            this.dataTracker.set(DATA_TRUSTED_ID_1, Optional.ofNullable(p_28516_));
        } else {
            this.dataTracker.set(DATA_TRUSTED_ID_0, Optional.ofNullable(p_28516_));
        }
    }

    private void launch(Entity launch, float angle, float scale) {
        final float rot = 180F + angle + this.getYaw();
        final float hugeScale = 1.0F + random.nextFloat() * 0.5F * scale;
        final float strength = (float) (hugeScale *  (1.0D - ((LivingEntity) launch).getAttributeValue(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE)));
        final float rotRad = rot * MathHelper.RADIANS_PER_DEGREE;
        final float x = MathHelper.sin(rotRad);
        final float z = -MathHelper.cos(rotRad);
        launch.velocityDirty = true;
        Vec3d vec3 = this.getVelocity();
        Vec3d vec31 = vec3.add((new Vec3d(x, 0.0D, z)).normalize().multiply(strength));
        launch.setVelocity(vec31.x, hugeScale * 0.3F, vec31.z);
        launch.setOnGround(false);
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

    private boolean trusts(UUID uuid) {
        return this.getTrustedUUIDs().contains(uuid);
    }

    @Override
    public Animation[] getAnimations() {
        return new Animation[]{ANIMATION_FLICK_EARS, ANIMATION_EAT_GRASS, ANIMATION_FLING, ANIMATION_SLASH};
    }

    @Nullable
    @Override
    public PassiveEntity createChild(ServerWorld serverLevel, PassiveEntity ageableMob) {
        return AMEntityRegistry.RHINOCEROS.get().create(serverLevel);
    }

    public boolean isAngry() {
        return this.dataTracker.get(ANGRY);
    }

    public void setAngry(boolean angry) {
        this.dataTracker.set(ANGRY, angry);
    }

    private void attackWithPotion(LivingEntity target, float dmg) {
        var potion = this.getPotionEffect();

        target.damage(this.getDamageSources().mobAttack(this), dmg);
        if(potion != null){
            var instance = new StatusEffectInstance(potion, this.getPotionDuration(), this.getPotionLevel());
            if (!target.hasStatusEffect(potion) && target.addStatusEffect(instance)) {
                this.setInflictedCount(this.getInflictedCount() + 1);
            }
        }
        if(this.getInflictedCount() > 15 && random.nextInt(3) == 0 || this.getInflictedCount() > 20){
            this.resetPotion();
        }
    }

    @Override
    public boolean tryAttack(Entity entity) {
        if(this.getAnimation() == NO_ANIMATION){
            this.setAnimation(random.nextBoolean() ? ANIMATION_SLASH : ANIMATION_FLING);
            return true;
        }
        return false;
    }

    @Override
    public boolean isTeammate(Entity entityIn) {
        if (entityIn instanceof TameableEntity tamableAnimal && tamableAnimal.getOwnerUuid() != null && trusts(tamableAnimal.getOwnerUuid())) {
            return true;
        }
        return super.isTeammate(entityIn) || trusts(entityIn.getUuid());
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound tag) {
        super.writeCustomDataToNbt(tag);
        List<UUID> list = this.getTrustedUUIDs();
        var listtag = new NbtList();

        for (var uuid : list) {
            if (uuid != null) {
                listtag.add(NbtHelper.fromUuid(uuid));
            }
        }

        tag.put("Trusted", listtag);
        tag.putBoolean("Sleeping", this.isSleeping());
        tag.putString("PotionName", this.getAppliedPotionId());
        tag.putInt("PotionLevel", this.getPotionLevel());
        tag.putInt("PotionDuration", this.getPotionDuration());
        tag.putInt("InflictedCount", this.getInflictedCount());
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound tag) {
        super.readCustomDataFromNbt(tag);
        var listtag = tag.getList("Trusted", 11);

        for (var nbtElement : listtag) {
            this.addTrustedUUID(NbtHelper.toUuid(nbtElement));
        }

        this.setAppliedPotionId(tag.getString("PotionName"));
        this.setPotionLevel(tag.getInt("PotionLevel"));
        this.setPotionDuration(tag.getInt("PotionDuration"));
        this.setInflictedCount(tag.getInt("InflictedCount"));
    }

    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        ItemStack itemstack = player.getStackInHand(hand);
        ActionResult type = super.interactMob(player, hand);
        if(!isBaby() && (itemstack.getItem() == Items.POTION || itemstack.getItem() == Items.SPLASH_POTION || itemstack.getItem() == Items.LINGERING_POTION)){
            var contained = PotionUtil.getPotion(itemstack);
            if(applyPotion(contained)){
                this.emitGameEvent(GameEvent.ENTITY_INTERACT);
                this.playSoundIfNotSilent(SoundEvents.ITEM_DYE_USE);
                this.eat(player, hand, itemstack);
                ItemStack bottle = new ItemStack(Items.GLASS_BOTTLE);
                if(!player.giveItemStack(bottle)){
                    player.dropItem(bottle, false);
                }
                return ActionResult.SUCCESS;
            }
        } else if (itemstack.isIn(AMTagRegistry.RHINOCEROS_FOODSTUFFS) && !trusts(player.getUuid())) {
            addTrustedUUID(player.getUuid());
            this.eat(player, hand, itemstack);
            this.emitGameEvent(GameEvent.EAT);
            this.playSoundIfNotSilent(SoundEvents.ENTITY_HORSE_EAT);
            return ActionResult.SUCCESS;
        }
        return type;
    }

    public boolean applyPotion(Potion potion){
        if(potion == null || potion == Potions.WATER){
            resetPotion();
            return true;
        }else{
            if(!potion.getEffects().isEmpty()){
                StatusEffectInstance first = potion.getEffects().get(0);
                Identifier loc = Registries.STATUS_EFFECT.getId(first.getEffectType());
                if(loc != null){
                    this.setAppliedPotionId(loc.toString());
                    this.setPotionLevel(first.getAmplifier());
                    this.setPotionDuration(first.getDuration());
                    this.setInflictedCount(0);
                    return true;
                }
            }
        }
        return false;
    }

    class AIAttackNearPlayers extends ActiveTargetGoal<PlayerEntity> {
        public AIAttackNearPlayers() {
            super(EntityRhinoceros.this, PlayerEntity.class, 80, true, true, null);
        }

        @Override
        public boolean canStart() {
            if (EntityRhinoceros.this.isBaby() || EntityRhinoceros.this.isInLove() || EntityRhinoceros.this.trustsAny()) {
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

    private boolean trustsAny() {
        return this.dataTracker.get(DATA_TRUSTED_ID_0).isPresent() || this.dataTracker.get(DATA_TRUSTED_ID_1).isPresent();
    }

    class DefendTrustedTargetGoal extends ActiveTargetGoal<LivingEntity> {
        private LivingEntity trustedLastHurtBy;
        private LivingEntity trustedLastHurt;
        private LivingEntity trusted;
        private int timestamp;

        public DefendTrustedTargetGoal(Class<LivingEntity> entities, boolean b, boolean b2, Predicate<LivingEntity> pred) {
            super(EntityRhinoceros.this, entities, 10, b, b2, pred);
        }

        @Override
        public boolean canStart() {
            if ((this.reciprocalChance <= 0 || this.mob.getRandom().nextInt(this.reciprocalChance) == 0) && !this.mob.isBaby()) {
                for (var uuid : EntityRhinoceros.this.getTrustedUUIDs()) {
                    if (uuid != null && EntityRhinoceros.this.getWorld() instanceof ServerWorld) {
                        Entity entity = ((ServerWorld) EntityRhinoceros.this.getWorld()).getEntity(uuid);
                        if (entity instanceof LivingEntity livingentity) {
                            this.trusted = livingentity;
                            this.trustedLastHurtBy = livingentity.getAttacker();
                            this.trustedLastHurt = livingentity.getAttacking();
                            int i = livingentity.getLastAttackedTime();
                            int j = livingentity.getLastAttackTime();
                            if (i != this.timestamp && this.canTrack(this.trustedLastHurtBy, this.targetPredicate)) {
                                return true;
                            }
                            if (j != this.timestamp && this.canTrack(this.trustedLastHurt, this.targetPredicate)) {
                                return true;
                            }
                        }
                    }
                }

            }
            return false;
        }

        @Override
        public void start() {
            if(this.trustedLastHurtBy != null){
                this.setTargetEntity(this.trustedLastHurtBy);
                this.target = this.trustedLastHurtBy;
                if (this.trusted != null) {
                    this.timestamp = this.trusted.getLastAttackedTime();
                }
            }else{
                this.setTargetEntity(this.trustedLastHurt);
                this.target = this.trustedLastHurt;
                if (this.trusted != null) {
                    this.timestamp = this.trusted.getLastAttackTime();
                }
            }
            super.start();
        }
    }


    class StrollGoal extends MoveThroughVillageGoal {
        public StrollGoal(int timr) {
            super(EntityRhinoceros.this, 1.0D, true, timr, () -> false);
        }

        @Override
        public void start() {
            super.start();
        }

        @Override
        public boolean canStart() {
            return super.canStart() && this.canRhinoWander();
        }

        @Override
        public boolean shouldContinue() {
            return super.shouldContinue() && this.canRhinoWander();
        }

        private boolean canRhinoWander() {
            return !EntityRhinoceros.this.getTrustedUUIDs().isEmpty();
        }
    }

}