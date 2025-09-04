package com.github.alexthe666.alexsmobs.misc;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.TagKey;

import java.util.ArrayList;

public class IngredientUtil {

    @SafeVarargs
    public static Ingredient ingredientFromTags(TagKey<Item>... tags) {
        var stacks = new ArrayList<ItemStack>();
        for (TagKey<Item> tag : tags) {
            for (RegistryEntry<Item> entry : Registries.ITEM.iterateEntries(tag)) {
                stacks.add(new ItemStack(entry.value()));
            }
        }
        return Ingredient.ofStacks(stacks.stream());
    }
}
