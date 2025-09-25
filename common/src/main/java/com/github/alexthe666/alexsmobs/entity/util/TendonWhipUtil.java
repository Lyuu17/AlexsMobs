package com.github.alexthe666.alexsmobs.entity.util;

import com.github.alexthe666.alexsmobs.AlexsMobs;
import com.github.alexthe666.alexsmobs.entity.EntityTendonSegment;
import com.mrcrayfish.framework.api.registry.RegistryContainer;
import com.mrcrayfish.framework.api.sync.Serializers;
import com.mrcrayfish.framework.api.sync.SyncedClassKey;
import com.mrcrayfish.framework.api.sync.SyncedDataKey;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

import java.util.UUID;

@RegistryContainer
public class TendonWhipUtil {

    private static final String LAST_TENDON_UUID = "LastTendonUUIDAlexsMobs";
    private static final String LAST_TENDON_ID = "LastTendonIDAlexsMobs";

    public static final SyncedDataKey<LivingEntity, NbtCompound> TAG = SyncedDataKey.builder(SyncedClassKey.LIVING_ENTITY, Serializers.TAG_COMPOUND)
            .id(new Identifier(AlexsMobs.MOD_ID, "tendon_whip"))
            .defaultValueSupplier(() -> {
                var nbt = new NbtCompound();
                nbt.putInt(LAST_TENDON_ID, -1);
                return nbt;
            })
            .saveToFile()
            .syncMode(SyncedDataKey.SyncMode.ALL)
            .build();

    public static void setLastTendon(LivingEntity entity, EntityTendonSegment tendon) {
        var tag = TAG.getValue(entity);
        if (tendon == null) {
            tag.remove(LAST_TENDON_UUID);
            tag.putInt(LAST_TENDON_ID, -1);
        } else {
            tag.putUuid(LAST_TENDON_UUID, tendon.getUuid());
            tag.putInt(LAST_TENDON_ID, tendon.getId());
        }
        TAG.setValue(entity, tag);
    }

    private static UUID getLastTendonUUID(LivingEntity entity) {
        var tag = TAG.getValue(entity);
        if (tag.contains(LAST_TENDON_UUID)) {
            return tag.getUuid(LAST_TENDON_UUID);
        } else {
            return null;
        }
    }

    public static int getLastTendonId(LivingEntity entity) {
        var tag = TAG.getValue(entity);
        if (tag.contains(LAST_TENDON_ID)) {
            return tag.getInt(LAST_TENDON_ID);
        } else {
            return -1;
        }
    }

    public static void retractFarTendons(World level, LivingEntity player) {
        EntityTendonSegment last = getLastTendon(player);
        if (last != null) {
            last.remove(Entity.RemovalReason.DISCARDED);
            setLastTendon(player, null);
        }
    }

    public static boolean canLaunchTendons(World level, LivingEntity player) {
        EntityTendonSegment last = getLastTendon(player);
        if (last != null) {
            return last.isRemoved() || last.distanceTo(player) > 30;
        }
        return true;
    }

    public static EntityTendonSegment getLastTendon(LivingEntity player) {
        var uuid = getLastTendonUUID(player);
        int id = getLastTendonId(player);
        if (!player.getWorld().isClient) {
            if (uuid != null) {
                var e = player.getWorld().getEntityById(id);
                return e instanceof EntityTendonSegment ? (EntityTendonSegment) e : null;
            }
        } else {
            if (id != -1) {
                var e = player.getWorld().getEntityById(id);
                return e instanceof EntityTendonSegment ? (EntityTendonSegment) e : null;
            }
        }
        return null;
    }
}
