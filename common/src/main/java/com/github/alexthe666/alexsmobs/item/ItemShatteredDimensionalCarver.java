package com.github.alexthe666.alexsmobs.item;

import com.github.alexthe666.alexsmobs.entity.EntityVoidPortal;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

public class ItemShatteredDimensionalCarver extends ItemDimensionalCarver {

    public ItemShatteredDimensionalCarver(Item.Settings props) {
        super(props);
    }

    @Override
    public void onPortalOpen(World worldIn, LivingEntity player, EntityVoidPortal portal, Direction dir) {
        portal.setAttachmentFacing(dir);
        portal.setShattered(true);
        portal.setLifespan(2000);
        portal.exitDimension = worldIn.getRegistryKey();
        BlockPos playerPos = player.getBlockPos();
        if (dir == Direction.DOWN) {
            portal.setDestination(new BlockPos(playerPos.getX(), worldIn.getBottomY() + 1, playerPos.getZ()));
        } else if (dir == Direction.UP) {
            portal.setDestination(new BlockPos(playerPos.getX(), worldIn.getTopY() - 1, playerPos.getZ()));
        }else{
            double worldBorderDistance = worldIn.getWorldBorder().getDistanceInsideBorder(playerPos.getX(), playerPos.getZ()) - 5D;
            BlockPos millionPos = playerPos.offset(dir.getOpposite(), (int) Math.min(worldBorderDistance, 1000000));
            portal.setDestination(millionPos);
        }
    }
}
