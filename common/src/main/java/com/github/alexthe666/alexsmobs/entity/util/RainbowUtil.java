package com.github.alexthe666.alexsmobs.entity.util;

import com.github.alexthe666.alexsmobs.AlexsMobs;
import com.github.alexthe666.alexsmobs.config.AMConfig;
import com.github.alexthe666.alexsmobs.item.ItemRainbowJelly;
import com.github.alexthe666.alexsmobs.misc.AMSimplexNoise;
import com.mrcrayfish.framework.api.sync.Serializers;
import com.mrcrayfish.framework.api.sync.SyncedClassKey;
import com.mrcrayfish.framework.api.sync.SyncedDataKey;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.awt.*;
import java.util.Locale;

public class RainbowUtil {

    public static final SyncedDataKey<LivingEntity, NbtCompound> TAG = SyncedDataKey.builder(SyncedClassKey.LIVING_ENTITY, Serializers.TAG_COMPOUND)
            .id(new Identifier(AlexsMobs.MOD_ID, "rainbow"))
            .defaultValueSupplier(NbtCompound::new)
            .saveToFile()
            .syncMode(SyncedDataKey.SyncMode.ALL)
            .build();

    private static final String RAINBOW_TYPE = "RainbowTypeAlexsMobs";

    public static void setRainbowType(LivingEntity entity, int type) {
        var tag = TAG.getValue(entity);
        tag.putInt(RAINBOW_TYPE, type);
        TAG.setValue(entity, tag);
//        if (!entity.getWorld().isClient) {
//            Citadel.sendMSGToAll(new PropertiesMessage("CitadelPatreonConfig", tag, entity.getId()));
//        }else{
//            Citadel.sendMSGToServer(new PropertiesMessage("CitadelPatreonConfig", tag, entity.getId()));
//        }
    }

    public static int getRainbowType(LivingEntity entity) {
        var tag = TAG.getValue(entity);
        if (tag.contains(RAINBOW_TYPE)) {
            return tag.getInt(RAINBOW_TYPE);
        }
        return 0;
    }

    public static int getRainbowTypeFromStack(ItemStack stack){
        var name = stack.toHoverableText().getString().toLowerCase(Locale.ROOT);
        return ItemRainbowJelly.RainbowType.getFromString(name).ordinal() + 1;
    }

    public static int calculateGlassColor(BlockPos pos) {
        float f = (float)AMConfig.rainbowGlassFidelity;
        float f1 = (float)((AMSimplexNoise.noise((pos.getX() + f) / f, (pos.getY() + f) / f, (pos.getZ() + f) / f) + 1.0F) * 0.5F);
        return Color.HSBtoRGB(f1, 1.0F, 1.0F);
    }
}
