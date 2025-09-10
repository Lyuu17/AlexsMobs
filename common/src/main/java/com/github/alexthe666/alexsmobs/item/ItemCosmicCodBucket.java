package com.github.alexthe666.alexsmobs.item;

import com.github.alexthe666.alexsmobs.registry.AMEntityRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.stat.Stats;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;

public class ItemCosmicCodBucket extends ItemModFishBucket {

    public ItemCosmicCodBucket(Item.Settings builder) {
        super(AMEntityRegistry.COSMIC_COD, Fluids.EMPTY, builder.maxCount(1));
    }

    @Override
    @NotNull
    public TypedActionResult<ItemStack> use(@NotNull World level, PlayerEntity player, @NotNull Hand hand) {
        var itemstack = player.getStackInHand(hand);
        var blockhitresult = raycast(level, player, RaycastContext.FluidHandling.NONE);
        // FIXME forge
//        var ret = net.minecraftforge.event.ForgeEventFactory.onBucketUse(player, level, itemstack, blockhitresult);
//        if (ret != null) return ret;
        if (blockhitresult.getType() == HitResult.Type.MISS) {
            return TypedActionResult.pass(itemstack);
        } else if (blockhitresult.getType() != HitResult.Type.BLOCK) {
            return TypedActionResult.pass(itemstack);
        } else {
            var blockpos = blockhitresult.getBlockPos();
            var direction = blockhitresult.getSide();
            var blockpos1 = blockpos.offset(direction);
            if (level.canPlayerModifyAt(player, blockpos) && player.canPlaceOn(blockpos1, direction, itemstack)) {
                this.onEmptied(player, level, itemstack, blockpos1);
                player.incrementStat(Stats.USED.getOrCreateStat(this));
                return TypedActionResult.success(getEmptiedStack(itemstack, player), level.isClient());
            }
        }
        return super.use(level, player, hand);
    }

}
