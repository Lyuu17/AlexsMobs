package com.github.alexthe666.alexsmobs.entity;

import com.github.alexthe666.alexsmobs.AlexsMobs;
import com.github.alexthe666.alexsmobs.block.entity.LeafcutterAnthillBlockEntity;
import com.github.alexthe666.alexsmobs.config.AMConfig;
import com.github.alexthe666.alexsmobs.entity.ai.CreatureAITargetItems;
import com.github.alexthe666.alexsmobs.entity.util.Maths;
import com.github.alexthe666.alexsmobs.misc.IngredientUtil;
import com.github.alexthe666.alexsmobs.packet.StartDancingPacket;
import com.github.alexthe666.alexsmobs.registry.*;
import com.google.common.base.Predicates;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.FuzzyTargeting;
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
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ItemStackParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.recipe.Ingredient;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.poi.PointOfInterestStorage;
import org.jetbrains.annotations.Nullable;

public class EntityManedWolf extends AnimalEntity implements ITargetsDroppedItems, IDancingMob {

    private static final TrackedData<Float> EAR_PITCH = DataTracker.registerData(EntityManedWolf.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> EAR_YAW = DataTracker.registerData(EntityManedWolf.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Boolean> DANCING = DataTracker.registerData(EntityManedWolf.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Integer> SHAKING_TIME = DataTracker.registerData(EntityManedWolf.class, TrackedDataHandlerRegistry.INTEGER);
    private static final Ingredient allFoods = IngredientUtil.ingredientFromTags(AMTagRegistry.MANED_WOLF_BREEDABLES, AMTagRegistry.MANED_WOLF_STENCH_FOODS);
    public float prevEarPitch;
    public float prevEarYaw;
    public float prevDanceProgress;
    public float danceProgress;
    public float prevShakeProgress;
    public float shakeProgress;
    private int earCooldown = 0;
    private float targetPitch;
    private float targetYaw;
    private boolean isJukeboxing;
    private BlockPos jukeboxPosition;
    private BlockPos nearestAnthill;

    public EntityManedWolf(EntityType<EntityManedWolf> animal, World level) {
        super(animal, level);
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createLivingAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 16.0D)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32.0D)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 2.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.3F);
    }

    @Override
    public boolean canSpawn(WorldAccess worldIn, SpawnReason spawnReasonIn) {
        return AMEntityRegistry.rollSpawn(AMConfig.manedWolfSpawnRolls, this.getRandom(), spawnReasonIn) && super.canSpawn(worldIn, spawnReasonIn);
    }

    @Override
    protected void initGoals() {
        super.initGoals();
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new EscapeDangerGoal(this, 1.5D));
        this.goalSelector.add(2, new AnimalMateGoal(this, 1.0D));
        this.goalSelector.add(3, new TemptGoal(this, 1.1D, allFoods, false));
        this.goalSelector.add(4, new WanderAroundGoal(this, 1D, 60));
        this.goalSelector.add(5, new FollowParentGoal(this, 1D));
        this.goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 6.0F));
        this.goalSelector.add(7, new LookAroundGoal(this));
        this.targetSelector.add(1, new CreatureAITargetItems<>(this, false, 30));
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(EAR_PITCH, 0F);
        this.dataTracker.startTracking(EAR_YAW, 0F);
        this.dataTracker.startTracking(SHAKING_TIME, 0);
        this.dataTracker.startTracking(DANCING, false);
    }

    public float getEarYaw() {
        return this.dataTracker.get(EAR_YAW);
    }

    public void setEarYaw(float yaw) {
        this.dataTracker.set(EAR_YAW, yaw);
    }

    public float getEarPitch() {
        return this.dataTracker.get(EAR_PITCH);
    }

    public void setEarPitch(float pitch) {
        this.dataTracker.set(EAR_PITCH, pitch);
    }

    public boolean isDancing() {
        return this.dataTracker.get(DANCING);
    }

    public void setDancing(boolean dancing) {
        this.dataTracker.set(DANCING, dancing);
        this.isJukeboxing = dancing;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return AMSoundRegistry.MANED_WOLF_IDLE.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return AMSoundRegistry.MANED_WOLF_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return AMSoundRegistry.MANED_WOLF_HURT.get();
    }

    private void attractAnimals() {
        if (this.getShakingTime() % 5 == 0) {
            var list = this.getWorld().getNonSpectatingEntities(AnimalEntity.class, this.getBoundingBox().expand(16, 8, 16));
            for (var e : list) {
                if(!(e instanceof EntityManedWolf) && !(e instanceof TameableEntity tamedMob && tamedMob.isInSittingPose())){
                    e.setTarget(null);
                    e.setAttacker(null);
                    var vec = FuzzyTargeting.findTo(e, 20, 7, this.getPos());
                    if (vec != null) {
                        e.getNavigation().startMovingTo(vec.x, vec.y, vec.z, 1.5D);
                    }
                }
            }

        }
    }

    private void pollinateAnthill(){
        if(nearestAnthill != null && getWorld().getBlockEntity(nearestAnthill) instanceof LeafcutterAnthillBlockEntity){
            if(this.getShakingTime() % 5 == 0){
                this.getNavigation().startMovingTo(nearestAnthill.getX() + 0.5F, nearestAnthill.getY() + 1F, nearestAnthill.getZ() + 0.5F, 1F);
            }
            if(nearestAnthill.isWithinDistance(this.getPos(), 6) && this.getShakingTime() % 20 == 0){
                ((LeafcutterAnthillBlockEntity)getWorld().getBlockEntity(nearestAnthill)).growFungus();
            }
        }
    }

    private void findAnthill(){
        if(nearestAnthill == null || !(getWorld().getBlockEntity(nearestAnthill) instanceof LeafcutterAnthillBlockEntity)){
            var listOfHives = ((ServerWorld) getWorld()).getPointOfInterestStorage()
                    .getPositions((poiTypeHolder -> poiTypeHolder.matchesKey(AMPointOfInterestRegistry.LEAFCUTTER_ANT_HILL.getKey())), Predicates.alwaysTrue(), this.getBlockPos(), 10, PointOfInterestStorage.OccupationStatus.ANY)
                    .toList();
            BlockPos nearest = null;
            for (var pos : listOfHives) {
                if (nearest == null || pos.getSquaredDistance(this.getBlockPos()) < nearest.getSquaredDistance(this.getBlockPos())) {
                    nearest = pos;
                }
            }
            nearestAnthill = nearest;
        }
    }

    @Override
    public void setJukeboxPos(BlockPos pos) {
        this.jukeboxPosition = pos;
    }

    public boolean isShaking() {
        return this.getShakingTime() > 0;
    }

    public int getShakingTime() {
        return this.dataTracker.get(SHAKING_TIME);
    }

    public void setShakingTime(int shaking) {
        this.dataTracker.set(SHAKING_TIME, shaking);
    }

    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        ItemStack itemstack = player.getStackInHand(hand);
        ActionResult type = super.interactMob(player, hand);
        if (itemstack.isIn(AMTagRegistry.MANED_WOLF_STENCH_FOODS) && !this.isShaking() && this.getMainHandStack().isEmpty()) {
            this.eat(player, hand, itemstack);
            eatItemEffect(itemstack);
            this.setShakingTime(100 + random.nextInt(30));
            return ActionResult.SUCCESS;
        } else {
            return type;
        }
    }

    private void eatItemEffect(ItemStack heldItemMainhand) {
        for (int i = 0; i < 2 + random.nextInt(2); i++) {
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

    @Override
    public void tick() {
        super.tick();
        prevEarPitch = this.getEarPitch();
        prevEarYaw = this.getEarYaw();
        prevDanceProgress = danceProgress;
        prevShakeProgress = shakeProgress;
        if (!this.getWorld().isClient) {
            updateEars();
        }
        boolean dance = isDancing();
        if (this.jukeboxPosition == null || !this.jukeboxPosition.isWithinDistance(this.getPos(), 15) || !this.getWorld().getBlockState(this.jukeboxPosition).isOf(Blocks.JUKEBOX)) {
            this.isJukeboxing = false;
            this.setDancing(false);
            this.jukeboxPosition = null;
        }
        if (dance && danceProgress < 5F) {
            danceProgress++;
        }
        if (!dance && danceProgress > 0F) {
            danceProgress--;
        }
        if (this.isShaking() && shakeProgress < 5F) {
            shakeProgress++;
        }
        if (!this.isShaking() && shakeProgress > 0F) {
            shakeProgress--;
        }
        if (this.isShaking()) {
            this.setShakingTime(this.getShakingTime() - 1);
            if (this.getWorld().isClient) {
                double d0 = this.random.nextGaussian() * 0.02D;
                double d1 = 0.05F + this.random.nextGaussian() * 0.02D;
                double d2 = this.random.nextGaussian() * 0.02D;
                this.getWorld().addParticle(AMParticleRegistry.SMELLY.get(), this.getParticleX(0.7F), this.getBodyY(0.6F), this.getParticleZ(0.7F), d0, d1, d2);
            }else{
                attractAnimals();
                findAnthill();
                if(this.nearestAnthill != null){
                    pollinateAnthill();
                }
            }
        }
    }

    private void updateEars() {
        final float pitchDist = Math.abs(targetPitch - this.getEarPitch());
        final float yawDist = Math.abs(targetYaw - this.getEarYaw());
        if (earCooldown <= 0 && this.random.nextInt(30) == 0 && pitchDist <= 0.1F && yawDist <= 0.1F) {
            targetPitch = MathHelper.clamp(random.nextFloat() * 60F - 30, -30, 30);
            targetYaw = MathHelper.clamp(random.nextFloat() * 60F - 30, -30, 30);
            earCooldown = 8 + random.nextInt(15);
        }

        if (pitchDist > 0.1F) {
            if (this.getEarPitch() < this.targetPitch) {
                this.setEarPitch(this.getEarPitch() + Math.min(pitchDist, 4F));
            }
            if (this.getEarPitch() > this.targetPitch) {
                this.setEarPitch(this.getEarPitch() - Math.min(pitchDist, 4F));
            }
        }

        if (yawDist > 0.1F) {
            if (this.getEarYaw() < this.targetYaw) {
                this.setEarYaw(this.getEarYaw() + Math.min(yawDist, 4F));
            }
            if (this.getEarYaw() > this.targetYaw) {
                this.setEarYaw(this.getEarYaw() - Math.min(yawDist, 4F));
            }
        }

        if (earCooldown > 0) {
            earCooldown--;
        }
    }

    @Override
    public boolean isBreedingItem(ItemStack stack) {
        return !stack.isIn(AMTagRegistry.MANED_WOLF_STENCH_FOODS) && allFoods.test(stack);
    }

    @Override
    public void travel(Vec3d vec3d) {
        if (this.isDancing() || danceProgress > 0) {
            if (this.getNavigation().getCurrentPath() != null) {
                this.getNavigation().stop();
            }
            vec3d = Vec3d.ZERO;
        }
        super.travel(vec3d);
    }

    @Override
    public boolean canTargetItem(ItemStack stack) {
        return allFoods.test(stack) && !this.isShaking();
    }

    @Override
    public void onGetItem(ItemEntity e) {
        eatItemEffect(e.getStack());
        if (e.getStack().isIn(AMTagRegistry.MANED_WOLF_STENCH_FOODS)) {
            this.setShakingTime(100 + random.nextInt(30));
        }
    }

    @Nullable
    @Override
    public PassiveEntity createChild(ServerWorld world, PassiveEntity entity) {
        return AMEntityRegistry.MANED_WOLF.get().create(world);
    }

    @Environment(EnvType.CLIENT)
    public void setNearbySongPlaying(BlockPos pos, boolean isPartying) {
        AlexsMobs.sendMSGToServer(new StartDancingPacket(this.getId(), isPartying, pos));
        this.setDancing(isPartying);
        if (isPartying) {
            this.setJukeboxPos(pos);
        } else {
            this.setJukeboxPos(null);
        }
    }

    public boolean isEnder() {
        String s = Formatting.strip(this.getName().getString());
        return s != null && (s.toLowerCase().contains("plummet") || s.toLowerCase().contains("ender"));
    }

}
