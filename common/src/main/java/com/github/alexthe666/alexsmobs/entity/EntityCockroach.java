package com.github.alexthe666.alexsmobs.entity;

import com.github.alexthe666.alexsmobs.client.sound.SoundLaCucaracha;
import com.github.alexthe666.alexsmobs.config.AMConfig;
import com.github.alexthe666.alexsmobs.entity.ai.AnimalAIFleeLight;
import com.github.alexthe666.alexsmobs.entity.ai.CreatureAITargetItems;
import com.github.alexthe666.alexsmobs.registry.AMEntityRegistry;
import com.github.alexthe666.alexsmobs.registry.AMItemRegistry;
import com.github.alexthe666.alexsmobs.registry.AMSoundRegistry;
import com.github.alexthe666.alexsmobs.registry.AMTagRegistry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.goal.*;
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
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.recipe.Ingredient;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.*;
import net.minecraft.world.event.GameEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

public abstract class EntityCockroach extends AnimalEntity implements Shearable, ITargetsDroppedItems {

    public static final Identifier MARACA_LOOT = new Identifier("alexsmobs", "entities/cockroach_maracas");
    public static final Identifier MARACA_HEADLESS_LOOT = new Identifier("alexsmobs", "entities/cockroach_maracas_headless");
    protected static final EntityDimensions STAND_SIZE = EntityDimensions.fixed(0.7F, 0.9F);
    private static final TrackedData<Boolean> DANCING = DataTracker.registerData(EntityCockroach.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> HEADLESS = DataTracker.registerData(EntityCockroach.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> MARACAS = DataTracker.registerData(EntityCockroach.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Optional<UUID>> NEAREST_MUSICIAN = DataTracker.registerData(EntityCockroach.class, TrackedDataHandlerRegistry.OPTIONAL_UUID);
    private static final TrackedData<Boolean> BREADED = DataTracker.registerData(EntityCockroach.class, TrackedDataHandlerRegistry.BOOLEAN);
    public int randomWingFlapTick = 0;
    public float prevDanceProgress;
    public float danceProgress;
    private boolean prevStand = false;
    private boolean isJukeboxing;
    private BlockPos jukeboxPosition;
    private int laCucarachaTimer = 0;
    public int timeUntilNextEgg = this.random.nextInt(24000) + 24000;

    public EntityCockroach(EntityType<? extends AnimalEntity> type, World world) {
        super(type, world);
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createLivingAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 6.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.35F)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32.0D);
    }

    public static boolean isValidLightLevel(ServerWorldAccess p_223323_0_, BlockPos p_223323_1_, Random p_223323_2_) {
        if (p_223323_0_.getLightLevel(LightType.SKY, p_223323_1_) > p_223323_2_.nextInt(32)) {
            return false;
        } else {
            int lvt_3_1_ = p_223323_0_.toServerWorld().isThundering() ? p_223323_0_.getLightLevel(p_223323_1_, 10) : p_223323_0_.getLightLevel(p_223323_1_);
            return lvt_3_1_ <= p_223323_2_.nextInt(8);
        }
    }

    @Override
    public boolean canSpawn(WorldAccess worldIn, SpawnReason spawnReasonIn) {
        return AMEntityRegistry.rollSpawn(AMConfig.cockroachSpawnRolls, this.getRandom(), spawnReasonIn);
    }

    public static boolean canMonsterSpawnInLight(EntityType<? extends EntityCockroach> p_223325_0_, ServerWorldAccess p_223325_1_, SpawnReason p_223325_2_, BlockPos p_223325_3_, Random p_223325_4_) {
        return isValidLightLevel(p_223325_1_, p_223325_3_, p_223325_4_) && canMobSpawn(p_223325_0_, p_223325_1_, p_223325_2_, p_223325_3_, p_223325_4_);
    }

    public static <T extends MobEntity> boolean canCockroachSpawn(EntityType<EntityCockroach> entityType, ServerWorldAccess iServerWorld, SpawnReason reason, BlockPos pos, Random random) {
        return reason == SpawnReason.SPAWNER || !iServerWorld.isSkyVisible(pos) && pos.getY() <= 64 && canMonsterSpawnInLight(entityType, iServerWorld, reason, pos, random);
    }

    @Override
    public boolean canImmediatelyDespawn(double distanceToClosestPlayer) {
        return !cannotDespawn();
    }

    @Override
    public boolean cannotDespawn() {
        return super.cannotDespawn() || this.hasCustomName() || this.isBreaded() || this.isDancing() || this.hasMaracas() || this.isHeadless();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return AMSoundRegistry.COCKROACH_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return AMSoundRegistry.COCKROACH_HURT.get();
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new EscapeDangerGoal(this, 1.1D));
        this.goalSelector.add(2, new AnimalMateGoal(this, 1.0D));
        this.goalSelector.add(3, new TemptGoal(this, 1.0D, Ingredient.fromTag(AMTagRegistry.COCKROACH_FOODSTUFFS), false));
        this.goalSelector.add(4, new FleeEntityGoal<>(this, EntityCentipedeHead.class, 16, 1.3D, 1.0D));
        this.goalSelector.add(4, new FleeEntityGoal<>(this, PlayerEntity.class, 8, 1.3D, 1.0D) {
            @Override
            public boolean canStart() {
                return !EntityCockroach.this.isBreaded() && super.canStart();
            }
        });
        this.goalSelector.add(5, new AnimalAIFleeLight(this, 1.0D) {
            @Override
            public boolean canStart() {
                return !EntityCockroach.this.isBreaded() && super.canStart();
            }
        });
        this.goalSelector.add(6, new WanderAroundGoal(this, 1.0D, 80));
        this.goalSelector.add(7, new LookAtEntityGoal(this, PlayerEntity.class, 6.0F));
        this.goalSelector.add(8, new LookAroundGoal(this));
        this.targetSelector.add(1, new CreatureAITargetItems<>(this, false));
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        boolean prev = super.damage(source, amount);
        if(prev){
            randomWingFlapTick = 5 + random.nextInt(15);
            if (this.getHealth() <= 1.0F && amount > 0 && !this.isHeadless() && this.getRandom().nextInt(3) == 0) {
                this.setHeadless(true);
                if (!this.getWorld().isClient) {
                    final var serverWorld = (ServerWorld) this.getWorld();
                    for (int i = 0; i < 3; i++) {
                        serverWorld.spawnParticles(ParticleTypes.SNEEZE, this.getParticleX(0.52F), this.getBodyY(1D), this.getParticleZ(0.52F), 1, 0.0D, 0.0D, 0.0D, 0.0D);
                    }
                }
            }
        }
        return prev;
    }

    @Override
    public boolean isBreedingItem(ItemStack stack) {
        return stack.isIn(AMTagRegistry.COCKROACH_BREEDABLES);
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound compound) {
        super.writeCustomDataToNbt(compound);
        compound.putBoolean("Maracas", this.hasMaracas());
        compound.putBoolean("Dancing", this.isDancing());
        compound.putBoolean("Breaded", this.isBreaded());
        compound.putInt("EggTime", this.timeUntilNextEgg);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound compound) {
        super.readCustomDataFromNbt(compound);
        this.setMaracas(compound.getBoolean("Maracas"));
        this.setDancing(compound.getBoolean("Dancing"));
        this.setBreaded(compound.getBoolean("Breaded"));
        if (compound.contains("EggTime")) {
            this.timeUntilNextEgg = compound.getInt("EggTime");
        }
    }

    @NotNull
    protected Identifier getLootTableId() {
        return this.hasMaracas() ? this.isHeadless() ? MARACA_HEADLESS_LOOT : MARACA_LOOT : super.getLootTableId();
    }

    @Override
    public float getPathfindingFavor(BlockPos pos, WorldView worldIn) {
        return 0.5F - Math.max(worldIn.getLightLevel(LightType.BLOCK, pos), worldIn.getLightLevel(LightType.SKY, pos));
    }

    @NotNull
    @Override
    public EntityGroup getGroup() {
        return EntityGroup.ARTHROPOD;
    }

    @NotNull
    @Override
    public EntityDimensions getDimensions(EntityPose poseIn) {
        return isDancing() ? STAND_SIZE.scaled(this.getScaleFactor()) : super.getDimensions(poseIn);
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return source.isOf(DamageTypes.FALL) || source.isOf(DamageTypes.DROWN) || source.isOf(DamageTypes.IN_WALL)  || source.isIn(DamageTypeTags.IS_EXPLOSION) || source.getName().equals("anvil") || super.isInvulnerableTo(source);
    }

    @NotNull
    @Override
    public ActionResult interactMob(@NotNull PlayerEntity playerEntity, @NotNull Hand hand) {
        ItemStack lvt_3_1_ = playerEntity.getStackInHand(hand);
        if (lvt_3_1_.getItem() == AMItemRegistry.MARACA.get() && this.isAlive() && !this.hasMaracas()) {
            this.setMaracas(true);
            lvt_3_1_.decrement(1);
            return ActionResult.success(this.getWorld().isClient);
        } else if (lvt_3_1_.getItem() != AMItemRegistry.MARACA.get() && this.isAlive() && this.hasMaracas()) {
            this.setMaracas(false);
            this.setDancing(false);
            this.dropStack(new ItemStack(AMItemRegistry.MARACA.get()));
            return ActionResult.SUCCESS;
        } else {
            return super.interactMob(playerEntity, hand);
        }
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(DANCING, false);
        this.dataTracker.startTracking(HEADLESS, false);
        this.dataTracker.startTracking(MARACAS, false);
        this.dataTracker.startTracking(NEAREST_MUSICIAN, Optional.empty());
        this.dataTracker.startTracking(BREADED, false);
    }

    public boolean isDancing() {
        return this.dataTracker.get(DANCING);
    }

    public void setDancing(boolean dancing) {
        this.dataTracker.set(DANCING, dancing);
    }

    public boolean isHeadless() {
        return this.dataTracker.get(HEADLESS);
    }

    public void setHeadless(boolean head) {
        this.dataTracker.set(HEADLESS, head);
    }

    public boolean hasMaracas() {
        return this.dataTracker.get(MARACAS);
    }

    public void setMaracas(boolean head) {
        this.dataTracker.set(MARACAS, head);
    }

    public boolean isBreaded() {
        return this.dataTracker.get(BREADED);
    }

    public void setBreaded(boolean breaded) {
        this.dataTracker.set(BREADED, breaded);
    }

    @Nullable
    public UUID getNearestMusicianId() {
        return this.dataTracker.get(NEAREST_MUSICIAN).orElse(null);
    }

    @Override
    public void tick() {
        super.tick();
        prevDanceProgress = danceProgress;
        final boolean dance = this.isJukeboxing || isDancing();
        if (this.jukeboxPosition == null || !this.jukeboxPosition.isWithinDistance(this.getPos(), 3.46D) || !this.getWorld().getBlockState(this.jukeboxPosition).isOf(Blocks.JUKEBOX)) {
            this.isJukeboxing = false;
            this.jukeboxPosition = null;
        }
        if (this.getStandingEyeHeight() > this.getHeight()) {
            this.calculateDimensions();
        }

        if (dance) {
            if (danceProgress < 5F)
                danceProgress++;
        } else {
            if (danceProgress > 0F)
                danceProgress--;
        }

        if (!this.isOnGround() || random.nextInt(200) == 0) {
            randomWingFlapTick = 5 + random.nextInt(15);
        }
        if (randomWingFlapTick > 0) {
            randomWingFlapTick--;
        }
        if (prevStand != dance) {
            if (hasMaracas()) {
                tellOthersImPlayingLaCucaracha();
            }
            this.calculateDimensions();
        }
        if (!hasMaracas()) {
            var musician = this.getNearestMusician();
            if (musician != null) {
                if (!musician.isAlive() || this.distanceTo(musician) > 10 || musician instanceof EntityCockroach && !((EntityCockroach) musician).hasMaracas()) {
                    this.setNearestMusician(null);
                    this.setDancing(false);
                } else {
                    this.setDancing(true);
                }
            }
        }
        if (hasMaracas()) {
            laCucarachaTimer++;
            if (laCucarachaTimer % 20 == 0 && random.nextFloat() < 0.3F) {
                tellOthersImPlayingLaCucaracha();
            }
            this.setDancing(true);
            if (!this.isSilent()) {
                this.getWorld().sendEntityStatus(this, (byte) 67);
            }
        } else {
            laCucarachaTimer = 0;
        }
        if (!this.getWorld().isClient && this.isAlive() && !this.isBaby() && --this.timeUntilNextEgg <= 0) {
           var dropped = this.dropItem(AMItemRegistry.COCKROACH_OOTHECA.get());
           if(dropped != null){
               dropped.setToDefaultPickupDelay();
           }
            this.timeUntilNextEgg = this.random.nextInt(24000) + 24000;

        }
        prevStand = dance;
    }

    private void tellOthersImPlayingLaCucaracha() {
        var list = this.getWorld().getEntitiesByClass(EntityCockroach.class, this.getMusicianDistance(), EntityPredicates.EXCEPT_SPECTATOR);
        for (var roach : list) {
            if (!roach.hasMaracas()) {
                roach.setNearestMusician(this.getUuid());
            }
        }
    }

    private Box getMusicianDistance() {
        return this.getBoundingBox().expand(10, 10, 10);
    }

    @Environment(EnvType.CLIENT)
    public void handleStatus(byte id) {
        if (id == 67) {
            if (this.isAlive()) {
                SoundLaCucaracha sound;
                if (SoundLaCucaracha.COCKROACH_SOUND_MAP.get(this.getId()) == null) {
                    sound = new SoundLaCucaracha(this);
                    SoundLaCucaracha.COCKROACH_SOUND_MAP.put(this.getId(), sound);
                } else {
                    sound = SoundLaCucaracha.COCKROACH_SOUND_MAP.get(this.getId());
                }
                if (!MinecraftClient.getInstance().getSoundManager().isPlaying(sound) && sound.canPlay() && sound.isOnlyCockroach()) {
                    MinecraftClient.getInstance().getSoundManager().play(sound);
                }
            }
        } else {
            super.handleStatus(id);
        }
    }

    public Entity getNearestMusician() {
        final UUID id = getNearestMusicianId();
        if (id != null && !this.getWorld().isClient) {
            return ((ServerWorld) getWorld()).getEntity(id);
        }
        return null;
    }

    public void setNearestMusician(@Nullable UUID uniqueId) {
        this.dataTracker.set(NEAREST_MUSICIAN, Optional.ofNullable(uniqueId));
    }

    public void setNearbySongPlaying(BlockPos pos, boolean isPartying) {
        this.jukeboxPosition = pos;
        this.isJukeboxing = isPartying;
    }

    @Nullable
    @Override
    public PassiveEntity createChild(@NotNull ServerWorld serverWorld, @NotNull PassiveEntity ageableEntity) {
        final var roach = AMEntityRegistry.COCKROACH.get().create(serverWorld);
        roach.setBreaded(true);
        return roach;
    }

    @Override
    public boolean isShearable() {
        return this.isAlive() && !this.isBaby() && !isHeadless();
    }

    @Override
    public void sheared(@NotNull SoundCategory category) {
        this.damage(getDamageSources().generic(), 0F);
        getWorld().playSoundFromEntity(null, this, SoundEvents.ENTITY_SHEEP_SHEAR, category, 1.0F, 1.0F);
        this.emitGameEvent(GameEvent.ENTITY_INTERACT);
        this.setHeadless(true);
    }

    @Override
    public boolean canTargetItem(ItemStack stack) {
        return stack.getItem().isFood() || stack.isIn(AMTagRegistry.COCKROACH_BREEDABLES);
    }

    @Override
    public void travel(@NotNull Vec3d vec3d) {
        if (this.isDancing() || danceProgress > 0) {
            if (this.getNavigation().getCurrentPath() != null) {
                this.getNavigation().stop();
            }
            vec3d = Vec3d.ZERO;
        }
        super.travel(vec3d);
    }

    @Override
    public void onGetItem(ItemEntity e) {
        if (e.getStack().getItem() == AMItemRegistry.MARACA.get()) {
            this.setMaracas(true);
        } else {
            if (e.getStack().getItem().hasRecipeRemainder()) {
                this.dropStack(e.getStack().getItem().getRecipeRemainder().getDefaultStack().copy());
            }
            this.heal(5);
            if (e.getStack().isIn(AMTagRegistry.COCKROACH_FOODSTUFFS)) {
                this.setBreaded(true);
            }
        }
    }
}
