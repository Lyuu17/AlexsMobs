package com.github.alexthe666.alexsmobs.registry;

import com.github.alexthe666.alexsmobs.AlexsMobs;
import dev.architectury.registry.registries.DeferredRegister;
import net.minecraft.block.entity.BannerPattern;
import net.minecraft.registry.RegistryKeys;

public class AMBannerRegistry {

    public static final DeferredRegister<BannerPattern> DEF_REG = DeferredRegister.create(AlexsMobs.MOD_ID, RegistryKeys.BANNER_PATTERN);

    static {
        DEF_REG.register("bear", () -> new BannerPattern("bear"));
        DEF_REG.register("australia_0", () -> new BannerPattern("australia_0"));
        DEF_REG.register("australia_1", () -> new BannerPattern("australia_1"));
        DEF_REG.register("new_mexico", () -> new BannerPattern("new_mexico"));
        DEF_REG.register("brazil", () -> new BannerPattern("brazil"));
    }
}
