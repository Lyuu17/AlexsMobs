package com.github.alexthe666.alexsmobs.entity;

import com.github.alexthe666.alexsmobs.AlexsMobs;
import com.github.alexthe666.alexsmobs.config.AMConfig;
import com.github.alexthe666.alexsmobs.entity.ai.AnimalAIWanderRanged;
import com.github.alexthe666.alexsmobs.entity.ai.CreatureAITargetItems;
import com.github.alexthe666.alexsmobs.packet.StartDancingPacket;
import com.github.alexthe666.alexsmobs.registry.AMEntityRegistry;
import com.github.alexthe666.alexsmobs.registry.AMSoundRegistry;
import com.github.alexthe666.alexsmobs.registry.AMTagRegistry;
import net.minecraft.block.Blocks;
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
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ShovelItem;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.recipe.Ingredient;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.*;
import net.minecraft.world.event.GameEvent;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public class EntityRainFrog extends AnimalEntity implements ITargetsDroppedItems,IDancingMob {

    private static final TrackedData<Integer> VARIANT = DataTracker.registerData(EntityRainFrog.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> STANCE_TIME = DataTracker.registerData(EntityRainFrog.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> ATTACK_TIME = DataTracker.registerData(EntityRainFrog.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> DANCE_TIME = DataTracker.registerData(EntityRainFrog.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Boolean> BURROWED = DataTracker.registerData(EntityRainFrog.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> DISTURBED = DataTracker.registerData(EntityRainFrog.class, TrackedDataHandlerRegistry.BOOLEAN);
    public float burrowProgress;
    public float prevBurrowProgress;
    public float danceProgress;
    public float prevDanceProgress;
    public float attackProgress;
    public float prevAttackProgress;
    public float stanceProgress;
    public float prevStanceProgress;
    private int burrowCooldown = 0;
    private int weatherCooldown = 0;
    private boolean isJukeboxing;
    private BlockPos jukeboxPosition;

    public EntityRainFrog(EntityType<EntityRainFrog> rainFrog, World lvl) {
        super(rainFrog, lvl);
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createLivingAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 6.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.2F)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32.0D);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new TemptGoal(this, 1.0D, Ingredient.fromTag(AMTagRegistry.RAIN_FROG_BREEDABLES), false));
        this.goalSelector.add(2, new AnimalMateGoal(this, 1.0D));
        this.goalSelector.add(3, new FleeEntityGoal<>(this, EntityRattlesnake.class, 9, 1.3D, 1.0D));
        this.goalSelector.add(5, new AIBurrow());
        this.goalSelector.add(6, new AnimalAIWanderRanged(this, 20, 1.0D, 10, 7));
        this.goalSelector.add(7, new LookAtEntityGoal(this, PlayerEntity.class, 10.0F));
        this.goalSelector.add(8, new LookAroundGoal(this));
        this.targetSelector.add(1, new CreatureAITargetItems<>(this, false));
    }

    public static boolean canRainFrogSpawn(EntityType animal, WorldAccess worldIn, SpawnReason reason, BlockPos pos, Random random) {
        boolean spawnBlock = worldIn.getBlockState(pos.down()).isIn(AMTagRegistry.RAIN_FROG_SPAWNS);
        return spawnBlock && worldIn.getLevelProperties() != null && (worldIn.getLevelProperties().isThundering() || worldIn.getLevelProperties().isRaining());
    }

    @Override
    public boolean canSpawn(WorldAccess worldIn, SpawnReason spawnReasonIn) {
        return AMEntityRegistry.rollSpawn(AMConfig.rainFrogSpawnRolls, this.getRandom(), spawnReasonIn);
    }

    public boolean isBurrowed() {
        return this.dataTracker.get(BURROWED);
    }

    public void setBurrowed(boolean burrowed) {
        this.dataTracker.set(BURROWED, burrowed);
    }

    public boolean isDisturbed() {
        return this.dataTracker.get(DISTURBED);
    }

    public void setDisturbed(boolean burrowed) {
        this.dataTracker.set(DISTURBED, burrowed);
    }

    public int getVariant() {
        return this.dataTracker.get(VARIANT);
    }

    public void setVariant(int variant) {
        this.dataTracker.set(VARIANT, variant);
    }

    public int getStanceTime() {
        return this.dataTracker.get(STANCE_TIME);
    }

    public void setStanceTime(int stanceTime) {
        this.dataTracker.set(STANCE_TIME, stanceTime);
    }

    public int getAttackTime() {
        return this.dataTracker.get(ATTACK_TIME);
    }

    public void setAttackTime(int attackTime) {
        this.dataTracker.set(ATTACK_TIME, attackTime);
    }

    public int getDanceTime() {
        return this.dataTracker.get(DANCE_TIME);
    }

    public void setDanceTime(int danceTime) {
        this.dataTracker.set(DANCE_TIME, danceTime);
    }

    @Override
    public boolean isBreedingItem(ItemStack stack) {
        return stack.isIn(AMTagRegistry.RAIN_FROG_BREEDABLES);
    }

    @Nullable
    @Override
    public PassiveEntity createChild(ServerWorld world, PassiveEntity entity) {
        EntityRainFrog frog = AMEntityRegistry.RAIN_FROG.get().create(world);
        frog.setVariant(this.getVariant());
        frog.setDisturbed(true);
        return frog;
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(VARIANT, 0);
        this.dataTracker.startTracking(STANCE_TIME, 0);
        this.dataTracker.startTracking(ATTACK_TIME, 0);
        this.dataTracker.startTracking(DANCE_TIME, 0);
        this.dataTracker.startTracking(BURROWED, false);
        this.dataTracker.startTracking(DISTURBED, false);
    }

    @Override
    public void tick() {
        super.tick();
        prevBurrowProgress = burrowProgress;
        prevDanceProgress = danceProgress;
        prevAttackProgress = attackProgress;
        prevStanceProgress = stanceProgress;

        if (this.isBurrowed()) {
            if (burrowProgress < 5F)
                burrowProgress += 0.5F;
        } else {
            if (burrowProgress > 0F)
                burrowProgress -= 0.5F;
        }

        if (this.burrowCooldown > 0) {
            this.burrowCooldown--;
        }

        if (this.getStanceTime() > 0) {
            this.setStanceTime(this.getStanceTime() - 1);
            if (this.stanceProgress < 5F) {
                this.stanceProgress++;
            }
        } else {
            if (this.stanceProgress > 0F) {
                this.stanceProgress--;
            }
        }

        if (this.getAttackTime() > 0) {
            this.setAttackTime(this.getAttackTime() - 1);
            if (this.attackProgress < 5F) {
                this.attackProgress += 2.5F;
            }
        } else {
            if (this.attackProgress > 0F) {
                this.attackProgress -= 0.5F;
            }
        }

        boolean dancing = this.getDanceTime() > 0 || this.isJukeboxing;
        if (dancing) {
            if (this.danceProgress < 5F) {
                this.danceProgress++;
            }
        } else {
            if (this.danceProgress > 0F) {
                this.danceProgress--;
            }
        }

        if (this.getDanceTime() > 0) {
            this.setBurrowed(false);
            this.setDanceTime(this.getDanceTime() - 1);
            if(this.getDanceTime() == 1 && weatherCooldown <= 0 && getWorld().getGameRules().getBoolean(GameRules.DO_WEATHER_CYCLE)){
                changeWeather();
            }
        }
        if(weatherCooldown > 0){
            weatherCooldown--;
        }
        if (this.jukeboxPosition == null || !this.jukeboxPosition.isWithinDistance(this.getPos(), 15) || !this.getWorld().getBlockState(this.jukeboxPosition).isOf(Blocks.JUKEBOX)) {
            this.isJukeboxing = false;
            this.setDanceTime(0);
            this.jukeboxPosition = null;
        }
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        boolean prev = super.damage(source, amount);
        if (prev && source.getSource() instanceof LivingEntity) {
            if (this.getStanceTime() <= 0) {
                this.setStanceTime(30 + random.nextInt(20));
            }
            this.setBurrowed(false);
        }
        return prev;
    }

    @Override
    public boolean canImmediatelyDespawn(double distanceToClosestPlayer) {
        return !cannotDespawn();
    }

    @Override
    public boolean cannotDespawn() {
        return super.cannotDespawn() || this.isDisturbed();
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return source.isOf(DamageTypes.IN_WALL)  || super.isInvulnerableTo(source);
    }

    @Override
    public boolean isSleeping() {
        return this.isBurrowed();
    }

    @Override
    public void updateLimbs(boolean flying) {
        float f1 = (float)MathHelper.magnitude(this.getX() - this.prevX, 0, this.getZ() - this.prevZ);
        float f2 = Math.min(f1 * 128.0F, 1.0F);
        this.limbAnimator.updateLimbs(f2, 0.4F);
    }

    @Nullable
    @Override
    public EntityData initialize(ServerWorldAccess worldIn, LocalDifficulty difficultyIn, SpawnReason reason, @Nullable EntityData spawnDataIn, @Nullable NbtCompound dataTag) {
        this.setVariant(random.nextInt(3));
        return super.initialize(worldIn, difficultyIn, reason, spawnDataIn, dataTag);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound compound) {
        super.readCustomDataFromNbt(compound);
        this.setDisturbed(compound.getBoolean("Disturbed"));
        this.setVariant(compound.getInt("Variant"));
        this.weatherCooldown = compound.getInt("WeatherCooldown");
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound compound) {
        super.writeCustomDataToNbt(compound);
        compound.putBoolean("Disturbed", isDisturbed());
        compound.putInt("Variant", getVariant());
        compound.putInt("WeatherCooldown", this.weatherCooldown);
    }

    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        ItemStack itemstack = player.getStackInHand(hand);
        Item item = itemstack.getItem();
        ActionResult type = super.interactMob(player, hand);
        if (item instanceof ShovelItem && (this.isBurrowed() || !this.isDisturbed()) && !this.getWorld().isClient) {
            this.ambientSoundChance = 1000;
            if (!player.isCreative()) {
                itemstack.damage(1, this.getRandom(), player instanceof ServerPlayerEntity ? (ServerPlayerEntity) player : null);
            }
            this.setStanceTime(20 + random.nextInt(30));
            this.setBurrowed(false);
            this.setDisturbed(true);
            this.burrowCooldown += 150 + random.nextInt(120);
            this.emitGameEvent(GameEvent.ENTITY_INTERACT);
            this.playSound(SoundEvents.BLOCK_SAND_BREAK, this.getSoundVolume(), this.getSoundPitch());
            return ActionResult.SUCCESS;
        }
        return type;
    }

    @Override
    public void travel(Vec3d travelVector) {
        if (this.isBurrowed() || this.getDanceTime() > 0) {
            if (this.getNavigation().getCurrentPath() != null) {
                this.getNavigation().stop();
            }
            travelVector = Vec3d.ZERO;
            super.travel(travelVector);
            return;
        }
        if (this.canMoveVoluntarily() && this.isTouchingWater()) {
            this.updateVelocity(this.getMovementSpeed(), travelVector);
            this.move(MovementType.SELF, this.getVelocity());
            this.setVelocity(this.getVelocity().multiply(0.9D));
            if (this.getTarget() == null) {
                this.setVelocity(this.getVelocity().add(0.0D, -0.005D, 0.0D));
            }
        } else {
            super.travel(travelVector);
        }
    }

    @Override
    public void onFindTarget(ItemEntity e) {
        this.setBurrowed(false);
        this.burrowCooldown += 50;
    }

    @Override
    public boolean canTargetItem(ItemStack stack) {
        return stack.isIn(AMTagRegistry.INSECT_ITEMS);
    }

    @Override
    public void onGetItem(ItemEntity e) {
        this.setAttackTime(10);
        this.heal(2);
    }

    private void changeWeather(){
        int time = 24000 + 1200 * random.nextInt(10);
        int type = 0;
        if(!this.getWorld().isRaining()){
            type = random.nextInt(1) + 1;
        }
        if(this.getWorld() instanceof ServerWorld serverWorld){
            if(type == 0){
                serverWorld.setWeather(time, 0, false, false);
            }else {
                serverWorld.setWeather(0, time, true, type == 2);
            }
        }
        weatherCooldown = time + 24000;
    }

    public void setNearbySongPlaying(BlockPos pos, boolean isPartying) {
        AlexsMobs.sendMSGToServer(new StartDancingPacket(this.getId(), isPartying, pos));
        if (isPartying) {
            this.setJukeboxPos(pos);
        } else {
            this.setJukeboxPos(null);
        }
    }

    @Override
    public void setDancing(boolean dancing) {
        this.setDanceTime(dancing && weatherCooldown == 0 ? 240 + random.nextInt(200) : 0);
    }

    @Override
    public void setJukeboxPos(BlockPos pos) {
        this.jukeboxPosition = pos;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return getStanceTime() > 0 ? AMSoundRegistry.RAIN_FROG_HURT.get() : AMSoundRegistry.RAIN_FROG_IDLE.get();
    }

    @Override
    public int getMinAmbientSoundDelay() {
        return getStanceTime() > 0 ? 10 : 80;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return AMSoundRegistry.RAIN_FROG_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return AMSoundRegistry.RAIN_FROG_HURT.get();
    }

    private class AIBurrow extends Goal {
        private BlockPos sand = null;
        private int burrowedTime = 0;

        public AIBurrow() {
            this.setControls(EnumSet.of(Goal.Control.MOVE));
        }

        @Override
        public boolean canStart() {
            if (!EntityRainFrog.this.isBurrowed() && EntityRainFrog.this.burrowCooldown == 0 && EntityRainFrog.this.random.nextInt(200) == 0) {
                this.burrowedTime = 0;
                sand = findSand();
                return sand != null;
            }
            return false;
        }

        @Override
        public boolean shouldContinue() {
            return burrowedTime < 300;
        }

        public BlockPos findSand() {
            BlockPos blockpos = null;

            for (var blockpos1 : BlockPos.iterate(MathHelper.floor(EntityRainFrog.this.getX() - 4.0D), MathHelper.floor(EntityRainFrog.this.getY() - 1.0D), MathHelper.floor(EntityRainFrog.this.getZ() - 4.0D), MathHelper.floor(EntityRainFrog.this.getX() + 4.0D), EntityRainFrog.this.getBlockY(), MathHelper.floor(EntityRainFrog.this.getZ() + 4.0D))) {
                if (EntityRainFrog.this.getWorld().getBlockState(blockpos1).isIn(BlockTags.SAND)) {
                    blockpos = blockpos1;
                    break;
                }
            }
            return blockpos;
        }

        @Override
        public void tick() {
            if (EntityRainFrog.this.isBurrowed()) {
                burrowedTime++;
                if (!EntityRainFrog.this.getSteppingBlockState().isIn(BlockTags.SAND)) {
                    EntityRainFrog.this.setBurrowed(false);
                }
            } else if (sand != null) {
                EntityRainFrog.this.getNavigation().startMovingTo(sand.getX() + 0.5F, sand.getY() + 1F, sand.getZ() + 0.5F, 1F);
                if (EntityRainFrog.this.getSteppingBlockState().isIn(BlockTags.SAND)) {
                    EntityRainFrog.this.setBurrowed(true);
                    EntityRainFrog.this.getNavigation().stop();
                    sand = null;
                } else {
                    EntityRainFrog.this.setBurrowed(false);
                }
            }
        }

        @Override
        public void stop() {
            EntityRainFrog.this.setBurrowed(false);
            EntityRainFrog.this.burrowCooldown = 120 + random.nextInt(1200);
            this.sand = null;
        }
    }
}
