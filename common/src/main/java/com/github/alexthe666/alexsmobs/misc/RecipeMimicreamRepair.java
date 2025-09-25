package com.github.alexthe666.alexsmobs.misc;

import com.github.alexthe666.alexsmobs.config.AMConfig;
import com.github.alexthe666.alexsmobs.registry.AMItemRegistry;
import com.github.alexthe666.alexsmobs.registry.AMRecipeRegistry;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.inventory.RecipeInputInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.SpecialCraftingRecipe;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.world.World;

public class RecipeMimicreamRepair extends SpecialCraftingRecipe {

    public RecipeMimicreamRepair(Identifier idIn, CraftingRecipeCategory category) {
        super(idIn, category);
    }

    /**
     * Used to check if a recipe matches current crafting inventory
     */
    public boolean matches(RecipeInputInventory inv, World worldIn) {
        if(!AMConfig.mimicreamRepair){
            return false;
        }
        var damageableStack = ItemStack.EMPTY;
        int mimicreamCount = 0;

        for (int j = 0; j < inv.size(); ++j) {
            var itemstack1 = inv.getStack(j);
            if (!itemstack1.isEmpty()) {
                if (itemstack1.isDamageable() && !isBlacklisted(itemstack1)) {
                    damageableStack = itemstack1;
                } else {
                    if (itemstack1.getItem() == AMItemRegistry.MIMICREAM.get()) {
                        mimicreamCount++;
                    }
                }
            }
        }

        return !damageableStack.isEmpty() && mimicreamCount >= 8;
    }

    public boolean isBlacklisted(ItemStack stack) {
        Identifier name = Registries.ITEM.getId(stack.getItem());
        return name != null && AMConfig.mimicreamBlacklist.contains(name.toString());
    }

    /**
     * Returns an Item that is the result of this recipe
     */
    @Override
    public ItemStack craft(RecipeInputInventory inv, DynamicRegistryManager registryAccess) {
        var damageableStack = ItemStack.EMPTY;
        int mimicreamCount = 0;

        for (int j = 0; j < inv.size(); ++j) {
            var itemstack1 = inv.getStack(j);
            if (!itemstack1.isEmpty()) {
                if (itemstack1.isDamageable() && !isBlacklisted(itemstack1)) {
                    damageableStack = itemstack1;
                } else {
                    if (itemstack1.getItem() == AMItemRegistry.MIMICREAM.get()) {
                        mimicreamCount++;
                    }
                }
            }
        }

        if (!damageableStack.isEmpty() && mimicreamCount >= 8) {
            var itemstack2 = damageableStack.copy();
            var compoundnbt = damageableStack.getNbt().copy();

            if(damageableStack.isOf(AMItemRegistry.GHOSTLY_PICKAXE.get()) && compoundnbt.contains("Items")){
                compoundnbt.remove("Items");
            }
            var oldNBTList = compoundnbt.getList("Enchantments", 10);
            var newNBTList = new NbtList();
            Identifier mendingName = Registries.ENCHANTMENT.getId(Enchantments.MENDING);
            for (int i = 0; i < oldNBTList.size(); ++i) {
                NbtCompound compoundnbt2 = oldNBTList.getCompound(i);
                Identifier Identifier1 = Identifier.tryParse(compoundnbt2.getString("id"));
                if (Identifier1 == null || !Identifier1.equals(mendingName)) {
                    newNBTList.add(compoundnbt2);
                }
            }
            compoundnbt.put("Enchantments", newNBTList);
            itemstack2.setNbt(compoundnbt);
            itemstack2.setDamage(itemstack2.getMaxDamage());
            return itemstack2;
        } else {
            return ItemStack.EMPTY;
        }
    }

    @Override
    public DefaultedList<ItemStack> getRemainder(RecipeInputInventory inv) {
        var nonnulllist = DefaultedList.ofSize(inv.size(), ItemStack.EMPTY);

        for (int i = 0; i < nonnulllist.size(); ++i) {
            var itemstack = inv.getStack(i);
            if (itemstack.getItem().hasRecipeRemainder()) {
                nonnulllist.set(i, itemstack.getItem().getRecipeRemainder().getDefaultStack());
            } else if (itemstack.getItem().isDamageable()) {
                var itemstack1 = itemstack.copy();
                itemstack1.setCount(1);
                nonnulllist.set(i, itemstack1);
                break;
            }
        }

        return nonnulllist;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return AMRecipeRegistry.MIMICREAM_RECIPE.get();
    }

    /**
     * Used to determine if this recipe can fit in a grid of the given width/height
     */
    @Override
    public boolean fits(int width, int height) {
        return width >= 3 && height >= 3;
    }
}
