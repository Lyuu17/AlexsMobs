package com.github.alexthe666.alexsmobs.item;

import com.github.alexthe666.alexsmobs.entity.EntityFlutter;
import com.github.alexthe666.alexsmobs.registry.AMEntityRegistry;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.FluidModificationItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class ItemFlutterPot extends Item implements FluidModificationItem {

    public ItemFlutterPot(Settings builder) {
        super(builder.maxCount(1));
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        var world = context.getWorld();
        var blockpos = context.getBlockPos();
        if(!world.isClient){
            if(this.placeFish((ServerWorld)world, context.getStack(), blockpos) && (context.getPlayer() == null || !context.getPlayer().isCreative())){
                context.getStack().decrement(1);
            }
            return ActionResult.success(false);
        }else{
            return ActionResult.PASS;
        }

    }

    private boolean placeFish(ServerWorld worldIn, ItemStack stack, BlockPos pos) {
        var entity = AMEntityRegistry.FLUTTER.get().spawnFromItemStack(worldIn, stack, null, pos, SpawnReason.BUCKET, true, false);
        if (entity instanceof EntityFlutter) {
            NbtCompound compoundnbt = stack.getOrCreateNbt();
            if(compoundnbt.contains("FlutterData")){
                entity.readCustomDataFromNbt(compoundnbt.getCompound("FlutterData"));
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean placeFluid(@Nullable PlayerEntity p_150821_, World p_150822_, BlockPos p_150823_, @Nullable BlockHitResult p_150824_) {
        return false;
    }
}
