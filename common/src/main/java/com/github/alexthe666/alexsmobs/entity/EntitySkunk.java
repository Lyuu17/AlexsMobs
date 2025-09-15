package com.github.alexthe666.alexsmobs.entity;

import com.github.alexthe666.alexsmobs.config.AMConfig;
import com.github.alexthe666.alexsmobs.misc.AMBlockPos;
import com.github.alexthe666.alexsmobs.registry.*;
import net.minecraft.block.BlockState;
import net.minecraft.block.MultifaceGrowthBlock;
import net.minecraft.entity.AreaEffectCloudEntity;
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
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.*;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.EnumSet;
import java.util.function.Predicate;

public class EntitySkunk extends AnimalEntity {

    private static final Predicate<LivingEntity> EXCEPT_CREATIVE_OR_SPECTATOR = (entity) -> !(entity instanceof PlayerEntity) || !entity.isSpectator() && !((PlayerEntity)entity).isCreative();
    private static final TrackedData<Integer> SPRAY_TIME = DataTracker.registerData(EntitySkunk.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Float> SPRAY_YAW = DataTracker.registerData(EntitySkunk.class, TrackedDataHandlerRegistry.FLOAT);

    public float prevSprayProgress;
    public float sprayProgress;
    private int prevSprayTime = 0;
    private int harassedTime;
    private int sprayCooldown;
    private Vec3d sprayAt;

    public EntitySkunk(EntityType<? extends AnimalEntity> type, World level) {
        super(type, level);
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createLivingAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 8.0D)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 1.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.25F);
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(SPRAY_YAW, 0F);
        this.dataTracker.startTracking(SPRAY_TIME, 0);
    }

    @Override
    protected void initGoals() {
        super.initGoals();
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new SprayGoal());
        this.goalSelector.add(1, new EscapeDangerGoal(this, 1.5D){
            @Override
            public void tick() {
                super.tick();
                EntitySkunk.this.harassedTime += 10;
            }
        });
        this.goalSelector.add(3, new TemptGoal(this, 1.1D, Ingredient.fromTag(AMTagRegistry.SKUNK_BREEDABLES), false));
        this.goalSelector.add(2, new AnimalMateGoal(this, 1.0D));
        this.goalSelector.add(4, new WanderAroundGoal(this, 1D, 60));
        this.goalSelector.add(5, new FollowParentGoal(this, 1D));
        this.goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 6.0F));
        this.goalSelector.add(7, new LookAroundGoal(this));
        this.goalSelector.add(3, new FleeEntityGoal<>(this, LivingEntity.class, AMEntityRegistry.buildPredicateFromTag(AMTagRegistry.SKUNK_FEARS), 10,  1.3D, 1.1D, EXCEPT_CREATIVE_OR_SPECTATOR) {
            @Override
            public boolean canStart() {
                return super.canStart() && EntitySkunk.this.getSprayTime() <= 0;
            }

            @Override
            public boolean shouldContinue() {
                return super.shouldContinue() && EntitySkunk.this.getSprayTime() <= 0;
            }

            @Override
            public void tick() {
                super.tick();
                if(targetEntity != null){
                    EntitySkunk.this.sprayAt = targetEntity.getPos();
                }
                EntitySkunk.this.harassedTime += 4;
            }
        });
    }

    @Override
    public boolean canSpawn(WorldAccess worldIn, SpawnReason spawnReasonIn) {
        return AMEntityRegistry.rollSpawn(AMConfig.skunkSpawnRolls, this.getRandom(), spawnReasonIn) && super.canSpawn(worldIn, spawnReasonIn);
    }

    @Override
    public boolean isBreedingItem(ItemStack stack) {
        return stack.isIn(AMTagRegistry.SKUNK_BREEDABLES);
    }

    public float getSprayYaw() {
        return this.dataTracker.get(SPRAY_YAW);
    }

    public void setSprayYaw(float yaw) {
        this.dataTracker.set(SPRAY_YAW, yaw);
    }

    public int getSprayTime() {
        return this.dataTracker.get(SPRAY_TIME);
    }

    public void setSprayTime(int time) {
        this.dataTracker.set(SPRAY_TIME, time);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return AMSoundRegistry.SKUNK_IDLE.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return AMSoundRegistry.SKUNK_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return AMSoundRegistry.SKUNK_HURT.get();
    }

    @Override
    public void tick(){
        super.tick();
        this.prevSprayProgress = sprayProgress;
        if(this.getSprayTime() > 0){
            if(this.sprayProgress < 5F){
                this.sprayProgress++;
            }

            this.setSprayTime(this.getSprayTime() - 1);
            if(this.getSprayTime() == 0){
                spawnLingeringCloud();
            }else if(this.getSprayTime() % 6 == 0){
                this.playSoundIfNotSilent(AMSoundRegistry.SKUNK_SPRAY.get());
            }
            this.bodyYaw = this.getYaw();
            this.setYaw(approachRotation(this.getSprayYaw(), this.getYaw() + 10, 15F));
        }
        if(this.getSprayTime() <= 0 && this.sprayProgress > 0F){
            this.sprayProgress--;
        }
        if(!this.getWorld().isClient){
            if(harassedTime > 200 && sprayCooldown == 0 && !this.isBaby()){
                harassedTime = 0;
                sprayCooldown = 200 + random.nextInt(200);
                this.setSprayTime(60 + random.nextInt(60));
            }
            if(harassedTime > 0){
                harassedTime--;
            }
            if(sprayCooldown > 0){
                sprayCooldown--;
            }
            var lastHurt = this.getAttacker();
            if(lastHurt != null){
                this.sprayAt = lastHurt.getPos();
            }
        }
        prevSprayTime = this.getSprayTime();
    }

    private void spawnLingeringCloud() {
        var collection = this.getActiveStatusEffects().values();
        if (!collection.isEmpty()) {
            final float fartDistance = 2.5F;
            var modelBack = new Vec3d(0, 0.4F, -fartDistance).rotateX(-this.getPitch() * MathHelper.RADIANS_PER_DEGREE).rotateY(-this.getYaw() * MathHelper.RADIANS_PER_DEGREE);
            var fartAt = this.getPos().add(modelBack);
            var areaeffectcloud = new AreaEffectCloudEntity(this.getWorld(), fartAt.x, fartAt.y, fartAt.z);
            areaeffectcloud.setRadius(2.5F);
            areaeffectcloud.setRadiusOnUse(-0.25F);
            areaeffectcloud.setWaitTime(20);
            areaeffectcloud.setDuration(areaeffectcloud.getDuration() / 2);
            areaeffectcloud.setRadiusGrowth(-areaeffectcloud.getRadius() / (float)areaeffectcloud.getDuration());

            for(var statusEffectInstance : collection) {
                areaeffectcloud.addEffect(new StatusEffectInstance(statusEffectInstance));
            }

            this.getWorld().spawnEntity(areaeffectcloud);
        }

    }

    @Override
    public void handleStatus(byte id) {
        if (id == 48) {
            var modelBack = new Vec3d(0, 0.4F, -0.4F).rotateX(-this.getPitch() * MathHelper.RADIANS_PER_DEGREE).rotateY(-this.getYaw() * MathHelper.RADIANS_PER_DEGREE);
            var particleFrom = this.getPos().add(modelBack);
            final float scale = random.nextFloat() * 0.5F + 1F;
            var particleTo = modelBack.multiply(scale, 1F, scale);
            for(int i = 0; i < 3; ++i) {
                final double d0 = this.random.nextGaussian() * 0.1D;
                final double d1 = this.random.nextGaussian() * 0.1D;
                final double d2 = this.random.nextGaussian() * 0.1D;
                this.getWorld().addParticle(AMParticleRegistry.SMELLY.get(), particleFrom.x, particleFrom.y, particleFrom.z, particleTo.x + d0, particleTo.y - 0.4F + d1, particleTo.z + d2);
            }
        } else {
            super.handleStatus(id);
        }
    }

    private float approachRotation(float current, float target, float max) {
        float f = MathHelper.wrapDegrees(target - current);
        if (f > max) {
            f = max;
        }

        if (f < -max) {
            f = -max;
        }

        return MathHelper.wrapDegrees(current + f);
    }

    @Nullable
    @Override
    public PassiveEntity createChild(ServerWorld level, PassiveEntity mob) {
        return AMEntityRegistry.SKUNK.get().create(getWorld());
    }

    private class SprayGoal extends Goal {
        private int actualSprayTime = 0;

        public SprayGoal() {
            this.setControls(EnumSet.of(Control.LOOK, Control.MOVE));
        }

        @Override
        public boolean canStart() {
            return EntitySkunk.this.getSprayTime() > 0;
        }

        @Override
        public void stop(){
            actualSprayTime = 0;
        }

        @Override
        public void tick(){
            EntitySkunk.this.getNavigation().stop();
            var sprayAt = getSprayAt();
            final double d0 = EntitySkunk.this.getX() - sprayAt.x;
            final double d2 = EntitySkunk.this.getZ() - sprayAt.z;
            final float f = (float)(MathHelper.atan2(d2, d0) * (double)MathHelper.DEGREES_PER_RADIAN) - 90.0F;
            EntitySkunk.this.setSprayYaw(f);
            if(EntitySkunk.this.sprayProgress >= 5F){
                getWorld().sendEntityStatus(EntitySkunk.this, (byte)48);
                if(actualSprayTime > 10 && random.nextInt(2) == 0){
                    var skunkPos = new Vec3d(EntitySkunk.this.getX(), EntitySkunk.this.getEyeY(), EntitySkunk.this.getZ());
                    final float xAdd = random.nextFloat() * 20 - 10;
                    final float yAdd = random.nextFloat() * 20 - 10;
                    final float maxSprayDist = 5F;
                    var modelBack = new Vec3d(0, 0F, -maxSprayDist).rotateX(xAdd - EntitySkunk.this.getPitch() * MathHelper.RADIANS_PER_DEGREE).rotateY((yAdd - EntitySkunk.this.getYaw()) * MathHelper.RADIANS_PER_DEGREE);
                    HitResult hitResult = EntitySkunk.this.getWorld().raycast(new RaycastContext(skunkPos, skunkPos.add(modelBack), RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, EntitySkunk.this));
                    if(hitResult != null) {
                        BlockPos pos;
                        Direction dir;
                        if (hitResult instanceof BlockHitResult block) {
                            pos = block.getBlockPos().offset(block.getSide());
                            dir = block.getSide().getOpposite();
                        } else {
                            pos = AMBlockPos.fromVec3(hitResult.getPos());
                            dir = Direction.UP;
                        }
                        BlockState currentState = getWorld().getBlockState(pos);
                        BlockState sprayState = ((MultifaceGrowthBlock) AMBlockRegistry.SKUNK_SPRAY.get()).withDirection(getWorld().getBlockState(pos), getWorld(), pos, dir);
                        if ((currentState.isAir() || currentState.isReplaceable()) && sprayState != null && sprayState.isOf(AMBlockRegistry.SKUNK_SPRAY.get())) {
                            getWorld().setBlockState(pos, sprayState);
                        }
                        double sprayDist = hitResult.getPos().subtract(skunkPos).length() / maxSprayDist;
                        var poisonBox = new Box(skunkPos, skunkPos.add(modelBack.multiply(sprayDist)).add(0, 1.5F, 0)).expand(1F);
                        Collection<StatusEffectInstance> collection = EntitySkunk.this.getActiveStatusEffects().values();
                        for (LivingEntity entity : EntitySkunk.this.getWorld().getNonSpectatingEntities(LivingEntity.class, poisonBox)) {
                            if (!(entity instanceof EntitySkunk)) {
                                entity.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 300));
                                if(entity instanceof ServerPlayerEntity serverPlayer){
                                    AMAdvancementTriggerRegistry.SKUNK_SPRAY.trigger(serverPlayer);
                                }
                                for(StatusEffectInstance StatusEffectInstance : collection) {
                                    entity.addStatusEffect(new StatusEffectInstance(StatusEffectInstance));
                                }
                            }
                        }
                    }
                }
                actualSprayTime++;
            }
        }

        private Vec3d getSprayAt() {
            var last = EntitySkunk.this.getAttacker();
            if(EntitySkunk.this.sprayAt != null){
                return EntitySkunk.this.sprayAt;
            }else if(last != null){
                return last.getPos();
            }else{
                var modelBack = new Vec3d(0, 0.4F, -1).rotateX(-EntitySkunk.this.getPitch() * MathHelper.RADIANS_PER_DEGREE).rotateY(-EntitySkunk.this.getYaw() * MathHelper.RADIANS_PER_DEGREE);
                return EntitySkunk.this.getPos().add(modelBack);
            }
        }

    }
}
