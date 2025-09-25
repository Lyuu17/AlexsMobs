package com.github.alexthe666.alexsmobs.misc;

import com.github.alexthe666.alexsmobs.registry.AMBlockRegistry;
import com.github.alexthe666.alexsmobs.registry.AMRecipeRegistry;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.inventory.RecipeInputInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.SpecialCraftingRecipe;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

public class RecipeBisonUpgrade extends SpecialCraftingRecipe {

    public RecipeBisonUpgrade(Identifier idIn, CraftingRecipeCategory category) {
        super(idIn, category);
    }

    @Override
    public boolean matches(RecipeInputInventory inv, World worldIn) {
        return !createBoots(inv).isEmpty();
    }

    @Override
    public ItemStack craft(RecipeInputInventory container, DynamicRegistryManager registryAccess) {
        return createBoots(container);
    }

    private ItemStack createBoots(RecipeInputInventory container) {
        var boots = ItemStack.EMPTY;
        int fur = 0;
        for (int j = 0; j < container.size(); ++j) {
            var itemstack1 = container.getStack(j);
            if (itemstack1.isOf(AMBlockRegistry.BISON_FUR_BLOCK.get().asItem())) {
                fur++;
            }
        }
        if(fur == 1){
            for (int j = 0; j < container.size(); ++j) {
                var itemstack1 = container.getStack(j);
                boolean notFurred = !itemstack1.hasNbt() || itemstack1.getNbt() != null && !itemstack1.getNbt().getBoolean("BisonFur");
                if (!itemstack1.isEmpty() && notFurred && LivingEntity.getPreferredEquipmentSlot(itemstack1) == EquipmentSlot.FEET) {
                    boots = itemstack1;
                }
            }
            if(!boots.isEmpty()){
                ItemStack stack = boots.copy();
                NbtCompound tag = stack.getOrCreateNbt();
                tag.putBoolean("BisonFur", true);
                stack.setNbt(tag);
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean fits(int x, int y) {
        return x * y >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return AMRecipeRegistry.BISON_UPGRADE.get();
    }
}
