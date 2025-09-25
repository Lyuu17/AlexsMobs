package com.github.alexthe666.alexsmobs.entity.util;

import com.github.alexthe666.alexsmobs.AlexsMobs;
import com.mrcrayfish.framework.api.sync.Serializers;
import com.mrcrayfish.framework.api.sync.SyncedClassKey;
import com.mrcrayfish.framework.api.sync.SyncedDataKey;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class VineLassoUtil {

    private static final String LASSO_PACKET = "LassoSentPacketAlexsMobs";
    private static final String LASSO_REMOVED = "LassoRemovedAlexsMobs";
    private static final String LASSOED_TO_TAG = "LassoOwnerAlexsMobs";
    private static final String LASSOED_TO_ENTITY_ID_TAG = "LassoOwnerIDAlexsMobs";

    public static final SyncedDataKey<LivingEntity, NbtCompound> TAG = SyncedDataKey.builder(SyncedClassKey.LIVING_ENTITY, Serializers.TAG_COMPOUND)
            .id(new Identifier(AlexsMobs.MOD_ID, "vine_lasso"))
            .defaultValueSupplier(NbtCompound::new)
            .saveToFile()
            .syncMode(SyncedDataKey.SyncMode.ALL)
            .build();

    public static void lassoTo(@Nullable LivingEntity lassoer, LivingEntity lassoed) {
        var lassoedTag = TAG.getValue(lassoed);
        if (lassoer == null) {
            lassoedTag.putUuid(LASSOED_TO_TAG, UUID.randomUUID());
            lassoedTag.putInt(LASSOED_TO_ENTITY_ID_TAG, -1);
            lassoedTag.putBoolean(LASSO_REMOVED, true);
        } else {
            if (!lassoedTag.contains(LASSOED_TO_ENTITY_ID_TAG) || lassoedTag.getInt(LASSOED_TO_ENTITY_ID_TAG) == -1) {
                lassoedTag.putUuid(LASSOED_TO_TAG, lassoer.getUuid());
                lassoedTag.putInt(LASSOED_TO_ENTITY_ID_TAG, lassoer.getId());
                lassoedTag.putBoolean(LASSO_REMOVED, false);
            }
        }
        TAG.setValue(lassoed, lassoedTag);
    }

    public static boolean hasLassoData(LivingEntity lasso) {
        var lassoedTag = TAG.getValue(lasso);
        return lassoedTag.contains(LASSOED_TO_ENTITY_ID_TAG) && !lassoedTag.getBoolean(LASSO_REMOVED) && lassoedTag.getInt(LASSOED_TO_ENTITY_ID_TAG) != -1;
    }

    public static Entity getLassoedTo(LivingEntity lassoed) {
        var lassoedTag = TAG.getValue(lassoed);
        if (lassoedTag.getBoolean(LASSO_REMOVED)) {
            return null;
        }
        if (hasLassoData(lassoed)) {
            if (lassoed.getWorld().isClient && lassoedTag.contains(LASSOED_TO_ENTITY_ID_TAG)) {
                int i = lassoedTag.getInt(LASSOED_TO_ENTITY_ID_TAG);
                if (i != -1) {
                    var found = lassoed.getWorld().getEntityById(i);
                    if (found != null) {
                        return found;
                    } else {
                        var uuid = lassoedTag.getUuid(LASSOED_TO_TAG);
                        if (uuid != null) {
                            return lassoed.getWorld().getPlayerByUuid(uuid);
                        }
                    }
                }
            } else if (lassoed.getWorld() instanceof ServerWorld) {
                var uuid = lassoedTag.getUuid(LASSOED_TO_TAG);
                if (uuid != null) {
                    var found = ((ServerWorld) lassoed.getWorld()).getEntity(uuid);
                    if (found != null) {
                        lassoedTag.putInt(LASSOED_TO_ENTITY_ID_TAG, found.getId());
                        return found;
                    }
                }
            }
        }
        return null;
    }

    public static void tickLasso(LivingEntity lassoed) {
        var tag = TAG.getValue(lassoed);
        if (!lassoed.getWorld().isClient) {
            if (tag.contains(LASSO_PACKET) || tag.getBoolean(LASSO_REMOVED)) {
                tag.putBoolean(LASSO_PACKET, false);
                TAG.setValue(lassoed, tag);
            }
        }
        var lassoedOwner = VineLassoUtil.getLassoedTo(lassoed);
        if (lassoedOwner != null) {
            double distance = lassoed.distanceTo(lassoedOwner);

            if (lassoed instanceof MobEntity mob) {
                if (distance > 3.0F) {
                    mob.getNavigation().startMovingTo(lassoedOwner, 1.0F);
                } else {
                    mob.getNavigation().stop();
                }
            }
            if (distance > 10) {
                double d0 = (lassoedOwner.getX() - lassoed.getX()) / distance;
                double d1 = (lassoedOwner.getY() - lassoed.getY()) / distance;
                double d2 = (lassoedOwner.getZ() - lassoed.getZ()) / distance;
                double yd = Math.copySign(d1 * d1 * 0.4D, d1);
                if (lassoed instanceof PlayerEntity) {
                    yd = 0;
                }
                lassoed.setVelocity(lassoed.getVelocity().add(Math.copySign(d0 * d0 * 0.4D, d0), yd, Math.copySign(d2 * d2 * 0.4D, d2)));
            }
        }
    }
}
