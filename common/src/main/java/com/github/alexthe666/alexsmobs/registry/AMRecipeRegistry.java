package com.github.alexthe666.alexsmobs.registry;

import com.github.alexthe666.alexsmobs.AlexsMobs;
import com.github.alexthe666.alexsmobs.misc.RecipeBisonUpgrade;
import com.github.alexthe666.alexsmobs.misc.RecipeMimicreamRepair;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.SpecialRecipeSerializer;
import net.minecraft.registry.RegistryKeys;

public class AMRecipeRegistry {

    public static final DeferredRegister<RecipeSerializer<?>> DEF_REG = DeferredRegister.create(AlexsMobs.MOD_ID, RegistryKeys.RECIPE_SERIALIZER);
    public static final RegistrySupplier<RecipeSerializer<?>> MIMICREAM_RECIPE = DEF_REG.register("mimicream_repair", () -> new SpecialRecipeSerializer<>(RecipeMimicreamRepair::new));
    public static final RegistrySupplier<RecipeSerializer<?>> BISON_UPGRADE = DEF_REG.register("bison_upgrade", () -> new SpecialRecipeSerializer<>(RecipeBisonUpgrade::new));
}
