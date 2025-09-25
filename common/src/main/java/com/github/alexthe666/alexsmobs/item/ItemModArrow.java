package com.github.alexthe666.alexsmobs.item;

import com.github.alexthe666.alexsmobs.entity.projectile.EntitySharkToothArrow;
import com.github.alexthe666.alexsmobs.registry.AMItemRegistry;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.ArrowItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public class ItemModArrow extends ArrowItem {
    public ItemModArrow(Item.Settings group) {
        super(group);
    }

    public PersistentProjectileEntity createArrow(World worldIn, ItemStack stack, LivingEntity shooter) {
        if(this == AMItemRegistry.SHARK_TOOTH_ARROW.get()){
            var arrowentity = new EntitySharkToothArrow(worldIn, shooter);
            arrowentity.initFromStack(stack);
            return arrowentity;
        }else {
            return super.createArrow(worldIn, stack, shooter);
        }
    }

}
