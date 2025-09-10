package com.github.alexthe666.alexsmobs.entity;

import com.github.alexthe666.alexsmobs.config.AMConfig;
import com.github.alexthe666.alexsmobs.entity.ai.AquaticMoveController;
import com.github.alexthe666.alexsmobs.entity.ai.EntityAINearestTarget3D;
import com.github.alexthe666.alexsmobs.entity.ai.SemiAquaticPathNavigator;
import com.github.alexthe666.alexsmobs.entity.util.Maths;
import com.github.alexthe666.alexsmobs.misc.AMBlockPos;
import com.github.alexthe666.alexsmobs.registry.AMEntityRegistry;
import com.github.alexthe666.alexsmobs.registry.AMItemRegistry;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.GuardianEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.WaterCreatureEntity;
import net.minecraft.entity.passive.SchoolingFishEntity;
import net.minecraft.entity.passive.SquidEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;

import java.util.EnumSet;
import java.util.function.Predicate;

public class EntityHammerheadShark extends WaterCreatureEntity {

    private static final Predicate<LivingEntity> INJURED_PREDICATE = (mob) -> mob.getHealth() <= mob.getMaxHealth() / 2D;

    public EntityHammerheadShark(EntityType<? extends EntityHammerheadShark> type, World worldIn) {
        super(type, worldIn);
        this.moveControl = new AquaticMoveController(this, 1F);
    }

    @Override
    public boolean canSpawn(WorldAccess worldIn, SpawnReason spawnReasonIn) {
        return AMEntityRegistry.rollSpawn(AMConfig.hammerheadSharkSpawnRolls, this.getRandom(), spawnReasonIn);
    }

    @Override
    protected EntityNavigation createNavigation(World worldIn) {
        return new SemiAquaticPathNavigator(this, worldIn);
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENTITY_COD_DEATH;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return SoundEvents.ENTITY_COD_HURT;
    }

    @Override
    public void travel(Vec3d travelVector) {
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
    protected void initGoals() {
        this.goalSelector.add(1, new MoveIntoWaterGoal(this));
        this.goalSelector.add(1, new CirclePreyGoal(this, 1F));
        this.goalSelector.add(4, new SwimAroundGoal(this, 0.6F, 7));
        this.goalSelector.add(4, new LookAroundGoal(this));
        this.goalSelector.add(8, new ChaseBoatGoal(this));
        this.goalSelector.add(9, new FleeEntityGoal<>(this, GuardianEntity.class, 8.0F, 1.0D, 1.0D));
        this.targetSelector.add(1, (new RevengeGoal(this)));
        this.targetSelector.add(2, new EntityAINearestTarget3D<>(this, LivingEntity.class, 50, false, true, INJURED_PREDICATE));
        this.targetSelector.add(2, new EntityAINearestTarget3D<>(this, SquidEntity.class, 50, false, true, null));
        this.targetSelector.add(2, new EntityAINearestTarget3D<>(this, EntityMimicOctopus.class, 80, false, true, null));
        this.targetSelector.add(3, new EntityAINearestTarget3D<>(this, SchoolingFishEntity.class, 70, false, true, null));
    }

    public boolean isTargetBlocked(Vec3d target) {
        var Vector3d = new Vec3d(this.getX(), this.getEyeY(), this.getZ());
        return this.getWorld().raycast(new RaycastContext(Vector3d, target, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, this)).getType() == HitResult.Type.BLOCK;
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createLivingAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 30D)
                .add(EntityAttributes.GENERIC_ARMOR, 0.0D)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 5.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.5F)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32.0D)
                .add(EntityAttributes.GENERIC_ATTACK_KNOCKBACK, 0);
    }

    public static <T extends MobEntity> boolean canHammerheadSharkSpawn(EntityType<EntityHammerheadShark> p_223364_0_, WorldAccess p_223364_1_, SpawnReason reason, BlockPos p_223364_3_, Random p_223364_4_) {
        if (p_223364_3_.getY() > 45 && p_223364_3_.getY() < p_223364_1_.getSeaLevel()) {
            return p_223364_1_.getFluidState(p_223364_3_).isIn(FluidTags.WATER);
        } else {
            return false;
        }
    }

    private static class CirclePreyGoal extends Goal {
        EntityHammerheadShark shark;
        float speed;
        float circlingTime = 0;
        float circleDistance = 5;
        float maxCirclingTime = 80;
        boolean clockwise = false;

        public CirclePreyGoal(EntityHammerheadShark shark, float speed) {
            this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
            this.shark = shark;
            this.speed = speed;
        }

        @Override
        public boolean canStart() {
            return this.shark.getTarget() != null;
        }

        @Override
        public boolean shouldContinue() {
            return this.shark.getTarget() != null;
        }

        @Override
        public void start(){
            circlingTime = 0;
            maxCirclingTime = 360 + this.shark.random.nextInt(80);
            circleDistance = 5 + this.shark.random.nextFloat() * 5;
            clockwise = this.shark.random.nextBoolean();
        }

        @Override
        public void stop(){
            circlingTime = 0;
            maxCirclingTime = 360 + this.shark.random.nextInt(80);
            circleDistance = 5 + this.shark.random.nextFloat() * 5;
            clockwise = this.shark.random.nextBoolean();
        }

        @Override
        public void tick(){
            LivingEntity prey = this.shark.getTarget();
            if(prey != null){
                double dist = this.shark.distanceTo(prey);
                if(circlingTime >= maxCirclingTime){
                    shark.lookAtEntity(prey, 30.0F, 30.0F);
                    shark.getNavigation().startMovingTo(prey, 1.5D);
                    if(dist < 2D){
                        shark.tryAttack(prey);
                        if(shark.random.nextFloat() < 0.3F){
                            shark.dropStack(new ItemStack(AMItemRegistry.SHARK_TOOTH.get()));
                        }
                        stop();
                    }
                }else{
                    if(dist <= 25){
                        circlingTime++;
                        BlockPos circlePos = getSharkCirclePos(prey);
                        if(circlePos != null){
                            shark.getNavigation().startMovingTo(circlePos.getX() + 0.5D, circlePos.getY() + 0.5D, circlePos.getZ() + 0.5D, 0.6D);
                        }
                    }else{
                        shark.lookAtEntity(prey, 30.0F, 30.0F);
                        shark.getNavigation().startMovingTo(prey, 0.8D);
                    }
                }
            }
        }

        public BlockPos getSharkCirclePos(LivingEntity target) {
            float angle = (Maths.STARTING_ANGLE * (clockwise ? -circlingTime : circlingTime));
            double extraX = circleDistance * MathHelper.sin((angle));
            double extraZ = circleDistance * MathHelper.cos(angle);
            BlockPos ground = AMBlockPos.fromCoords(target.getX() + 0.5F + extraX, shark.getY(), target.getZ() + 0.5F + extraZ);
            if(shark.getWorld().getFluidState(ground).isIn(FluidTags.WATER)){
                return ground;

            }
            return null;
    }
    }
}
