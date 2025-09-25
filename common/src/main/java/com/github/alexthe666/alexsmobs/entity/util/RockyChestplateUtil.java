package com.github.alexthe666.alexsmobs.entity.util;

import com.github.alexthe666.alexsmobs.AlexsMobs;
import com.github.alexthe666.alexsmobs.registry.AMItemRegistry;
import com.mrcrayfish.framework.api.sync.Serializers;
import com.mrcrayfish.framework.api.sync.SyncedClassKey;
import com.mrcrayfish.framework.api.sync.SyncedDataKey;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class RockyChestplateUtil {

    private static final String ROCKY_ROLL_TICKS = "RockyRollTicksAlexsMobs";
    private static final String ROCKY_ROLL_TIMESTAMP = "RockyRollTimestampAlexsMobs";
    private static final String ROCKY_X = "RockyRollXAlexsMobs";
    private static final String ROCKY_Y = "RockyRollYAlexsMobs";
    private static final String ROCKY_Z = "RockyRollZAlexsMobs";
    private static final int MAX_ROLL_TICKS = 30;

    public static final SyncedDataKey<LivingEntity, NbtCompound> TAG = SyncedDataKey.builder(SyncedClassKey.LIVING_ENTITY, Serializers.TAG_COMPOUND)
            .id(new Identifier(AlexsMobs.MOD_ID, "rocky_roll"))
            .defaultValueSupplier(NbtCompound::new)
            .saveToFile()
            .syncMode(SyncedDataKey.SyncMode.ALL)
            .build();

    public static void rollFor(LivingEntity roller, int ticks) {
        var tag = TAG.getValue(roller);
        tag.putInt(ROCKY_ROLL_TICKS, ticks);
        if(ticks == MAX_ROLL_TICKS){
            tag.putInt(ROCKY_ROLL_TIMESTAMP, roller.age);
        }
        TAG.setValue(roller, tag);
//        if (!roller.getWorld().isClient) {
//            Citadel.sendMSGToAll(new PropertiesMessage("CitadelPatreonConfig", lassoedTag, roller.getId()));
//        }else{
//            Citadel.sendMSGToServer(new PropertiesMessage("CitadelPatreonConfig", lassoedTag, roller.getId()));
//        }
    }

    public static int getRollingTicksLeft(LivingEntity entity) {
        var tag = TAG.getValue(entity);
        if (tag.contains(ROCKY_ROLL_TICKS)) {
            return tag.getInt(ROCKY_ROLL_TICKS);
        }
        return 0;
    }

    public static int getRollingTimestamp(LivingEntity entity) {
        var tag = TAG.getValue(entity);
        if (tag.contains(ROCKY_ROLL_TIMESTAMP)) {
            return tag.getInt(ROCKY_ROLL_TIMESTAMP);
        }
        return 0;
    }

    public static boolean isWearing(LivingEntity entity) {
        return entity.getEquippedStack(EquipmentSlot.CHEST).getItem() == AMItemRegistry.ROCKY_CHESTPLATE.get();
    }

    public static boolean isRockyRolling(LivingEntity entity) {
        return isWearing(entity) && getRollingTicksLeft(entity) > 0;
    }

    public static void tickRockyRolling(LivingEntity roller) {
        if(roller.isInsideWaterOrBubbleColumn()){
            roller.setVelocity(roller.getVelocity().add(0, -0.015F, 0));
        }
        var tag = TAG.getValue(roller);
        boolean update = false;
        int rollCounter = getRollingTicksLeft(roller);
        if(rollCounter == 0){
            if(roller.isSprinting()  && !roller.isSneaking() && (!(roller instanceof PlayerEntity) || !((PlayerEntity) roller).getAbilities().flying) && canRollAgain(roller) && !roller.hasVehicle()){
                update = true;
                rollFor(roller, MAX_ROLL_TICKS);
            }
            // FIXME forge
//            if(roller instanceof PlayerEntity && ((PlayerEntity)roller).getForcedPose() == EntityPose.SWIMMING){
//                ((PlayerEntity)roller).setForcedPose(null);
//            }
        }else{
            // FIXME forge
//            if(roller instanceof PlayerEntity){
//                ((Player)roller).setForcedPose(Pose.SWIMMING);
//            }
            if(!roller.getWorld().isClient){
                for (var entity : roller.getWorld().getNonSpectatingEntities(LivingEntity.class, roller.getBoundingBox().expand(1.0F))) {
                    if (!roller.isTeammate(entity) && !entity.isTeammate(roller) && entity != roller) {
                        entity.damage(entity.getDamageSources().mobAttack(roller), 2.0F + roller.getRandom().nextFloat() * 1.0F);
                    }
                }
            }
            if(roller.fallDistance > 3.0F){
                roller.fallDistance -= 0.5F;
            }
            roller.calculateDimensions();
            var vec3 = roller.isOnGround() ? roller.getVelocity() : roller.getVelocity().multiply(0.9D, 1D, 0.9D);
            float f = roller.getYaw() * MathHelper.RADIANS_PER_DEGREE;
            float f1 = roller.isInsideWaterOrBubbleColumn() ? 0.05F : 0.15F;
            var rollDelta = new Vec3d(vec3.x + (double) (-MathHelper.sin(f) * f1), 0.0D, vec3.z + (double) (MathHelper.cos(f) * f1));
            double rollY = roller.isInsideWaterOrBubbleColumn() || roller.isSneaking() ? -0.1F : rollCounter >= MAX_ROLL_TICKS ? 0.27D : vec3.y;
            roller.setVelocity(rollDelta.add(0.0D, rollY, 0.0D));
            if(rollCounter > 1 || !roller.isSprinting()){
                rollFor(roller, rollCounter - 1);
            }
            if((roller instanceof PlayerEntity && ((PlayerEntity) roller).getAbilities().flying || roller.isSneaking()) && canRollAgain(roller)){
                rollCounter = 0;
                rollFor(roller, 0);
            }
            if(rollCounter == 0){
                update = true;
            }
        }
        if (!roller.getWorld().isClient && update) {
            TAG.setValue(roller, tag);
//            Citadel.sendMSGToAll(new PropertiesMessage("CitadelPatreonConfig", tag, roller.getId()));
        }
    }

    private static boolean canRollAgain(LivingEntity roller) {
        return roller.age - getRollingTimestamp(roller) >= 20 || Math.abs(roller.age - getRollingTimestamp(roller)) > 100;
    }
}
