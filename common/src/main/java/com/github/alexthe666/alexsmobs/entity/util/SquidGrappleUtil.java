package com.github.alexthe666.alexsmobs.entity.util;

import com.github.alexthe666.alexsmobs.AlexsMobs;
import com.github.alexthe666.alexsmobs.entity.EntitySquidGrapple;
import com.mrcrayfish.framework.api.sync.Serializers;
import com.mrcrayfish.framework.api.sync.SyncedClassKey;
import com.mrcrayfish.framework.api.sync.SyncedDataKey;
import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

import java.util.UUID;

public class SquidGrappleUtil {

    private static final String HOOK_1 = "SquidGrappleHook1AlexsMobs";
    private static final String HOOK_2 = "SquidGrappleHook2AlexsMobs";
    private static final String HOOK_3 = "SquidGrappleHook3AlexsMobs";
    private static final String HOOK_4 = "SquidGrappleHook4AlexsMobs";
    private static final String LAST_REPLACED_HOOK = "LastSquidGrappleHookAlexsMobs";

    public static final SyncedDataKey<LivingEntity, NbtCompound> TAG = SyncedDataKey.builder(SyncedClassKey.LIVING_ENTITY, Serializers.TAG_COMPOUND)
            .id(new Identifier(AlexsMobs.MOD_ID, "squid_grapple"))
            .defaultValueSupplier(NbtCompound::new)
            .saveToFile()
            .syncMode(SyncedDataKey.SyncMode.ALL)
            .build();

    public static int onFireHook(LivingEntity entity, UUID newHookUUID) {
        var tag = TAG.getValue(entity);
        int index = getFirstAvailableHookIndex(entity);
        String indexStr = getHookStrFromIndex(index);
        if(tag.contains(indexStr)){
            EntitySquidGrapple hook = getHookEntity(entity.getWorld(), tag.getUuid(indexStr));
            if(hook != null && !hook.isRemoved()){
                hook.setWithdrawing(true);
            }
        }
        tag.putUuid(indexStr, newHookUUID);
        TAG.setValue(entity, tag);
        return index;
    }

    public static int getFirstAvailableHookIndex(LivingEntity entity){
        int nulls = getAnyNullHooks(entity);
        if(nulls != -1){
            return nulls;
        }
        int i = getHookCount(entity);
        if(i < 4){
            return i;
        }else{
            var tag = TAG.getValue(entity);
            int j = tag.getInt(LAST_REPLACED_HOOK);
            tag.putInt(LAST_REPLACED_HOOK, (j + 1) % 4);
            TAG.setValue(entity, tag);
            return j;
        }
    }

    public static String getHookStrFromIndex(int i){
        return switch (i) {
            case 0 -> HOOK_1;
            case 1 -> HOOK_2;
            case 2 -> HOOK_3;
            case 3 -> HOOK_4;
            default -> HOOK_1;
        };
    }

    public static int getAnyNullHooks(LivingEntity entity) {
        var tag = TAG.getValue(entity);
        if (!tag.contains(HOOK_1) || getHookEntity(entity.getWorld(), tag.getUuid(HOOK_1)) == null) {
            return 0;
        }
        if (!tag.contains(HOOK_2) || getHookEntity(entity.getWorld(), tag.getUuid(HOOK_2)) == null) {
            return 1;
        }
        if (!tag.contains(HOOK_3) || getHookEntity(entity.getWorld(), tag.getUuid(HOOK_3)) == null) {
            return 2;
        }
        if (!tag.contains(HOOK_4) || getHookEntity(entity.getWorld(), tag.getUuid(HOOK_4)) == null) {
            return 3;
        }
        return -1;
    }


    public static int getHookCount(LivingEntity entity) {
        var tag = TAG.getValue(entity);
        int count = 0;
        if (tag.contains(HOOK_1) && getHookEntity(entity.getWorld(), tag.getUuid(HOOK_1)) != null) {
            count++;
        }
        if (tag.contains(HOOK_2) && getHookEntity(entity.getWorld(), tag.getUuid(HOOK_2)) != null) {
            count++;
        }
        if (tag.contains(HOOK_3) && getHookEntity(entity.getWorld(), tag.getUuid(HOOK_3)) != null) {
            count++;
        }
        if (tag.contains(HOOK_4) && getHookEntity(entity.getWorld(), tag.getUuid(HOOK_4)) != null) {
            count++;
        }
        return count;
    }

    public static EntitySquidGrapple getHookEntity(World level, UUID id) {
        if (id != null && !level.isClient) {
            var e = ((ServerWorld) level).getEntity(id);
            return e instanceof EntitySquidGrapple ? (EntitySquidGrapple) e : null;
        }
        return null;
    }
}
