package com.github.alexthe666.alexsmobs.entity.util;

import com.github.alexthe666.alexsmobs.AlexsMobs;
import com.github.alexthe666.alexsmobs.registry.AMItemRegistry;
import com.mrcrayfish.framework.api.sync.Serializers;
import com.mrcrayfish.framework.api.sync.SyncedClassKey;
import com.mrcrayfish.framework.api.sync.SyncedDataKey;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class FlyingFishBootsUtil {

    private static final String BOOST_TICKS = "FlyingFishBoostAlexsMobs";
    private static final int MIN_BOOST_TIME = 35;

    public static final SyncedDataKey<LivingEntity, NbtCompound> TAG = SyncedDataKey.builder(SyncedClassKey.LIVING_ENTITY, Serializers.TAG_COMPOUND)
            .id(new Identifier(AlexsMobs.MOD_ID, "tendon_whip"))
            .defaultValueSupplier(NbtCompound::new)
            .saveToFile()
            .syncMode(SyncedDataKey.SyncMode.ALL)
            .build();

    public static void setBoostTicks(LivingEntity entity, int ticks) {
        var tag = TAG.getValue(entity);
        tag.putInt(BOOST_TICKS, ticks);
        TAG.setValue(entity, tag);

//        if (!entity.getWorld().isClient) {
//            Citadel.sendMSGToAll(new PropertiesMessage("CitadelPatreonConfig", lassoedTag, entity.getId()));
//        }else{
//            Citadel.sendMSGToServer(new PropertiesMessage("CitadelPatreonConfig", lassoedTag, entity.getId()));
//        }
    }

    public static int getBoostTicks(LivingEntity entity) {
        var tag = TAG.getValue(entity);
        if (tag.contains(BOOST_TICKS)) {
            return tag.getInt(BOOST_TICKS);
        }
        return 0;
    }

    public static boolean isWearing(LivingEntity entity) {
        return entity.getEquippedStack(EquipmentSlot.FEET).getItem() == AMItemRegistry.FLYING_FISH_BOOTS.get();
    }

    public static void tickFlyingFishBoots(LivingEntity fishy) {
        int boostTime = getBoostTicks(fishy);
        if(boostTime <= 15 && fishy.isInsideWaterOrBubbleColumn() && !fishy.isOnGround()){
            if(fishy.getFluidHeight(FluidTags.WATER) < 0.4F && fishy.jumping &&( !(fishy instanceof PlayerEntity) || !((PlayerEntity) fishy).getAbilities().flying)){
                final var rand = fishy.getRandom();
                boostTime = MIN_BOOST_TIME;
                var forward = new Vec3d(0, 0.0F, 0.5F + rand.nextFloat() * 1.2F).rotateX(-fishy.getPitch() * MathHelper.RADIANS_PER_DEGREE).rotateY(-fishy.getHeadYaw() * MathHelper.RADIANS_PER_DEGREE);
                var delta = fishy.getVelocity().add(forward);
                fishy.setVelocity(delta.x, 0.3 + rand.nextFloat() * 0.3F, delta.z);
                fishy.setYaw(fishy.getHeadYaw());
            }
        }
        if(boostTime > 0){
            if(!fishy.isInsideWaterOrBubbleColumn() && !fishy.isOnGround()){
                if(fishy.getVelocity().y < 0){
                    fishy.setVelocity(fishy.getVelocity().multiply(1F, 0.75F, 1F));
                }
                fishy.setPose(EntityPose.FALL_FLYING);
            }
            setBoostTicks(fishy, boostTime - 1);
        }
    }
}
