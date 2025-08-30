package com.github.alexthe666.alexsmobs.item;

import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.block.Block;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsage;
import net.minecraft.nbt.NbtCompound;

public class AMBlockItem extends BlockItem implements CustomTabBehavior {

    private final RegistrySupplier<Block> blockSupplier;

    public AMBlockItem(RegistrySupplier<Block> blockSupplier, Item.Settings props) {
        super(null, props);
        this.blockSupplier = blockSupplier;
    }

    @Override
    public Block getBlock() {
        return blockSupplier.get();
    }

    @Override
    public void onItemEntityDestroyed(ItemEntity itemEntity) {
        if (this.blockSupplier.get() instanceof ShulkerBoxBlock) {
            var itemstack = itemEntity.getStack();
            var compoundtag = getBlockEntityNbt(itemstack);
            if (compoundtag != null && compoundtag.contains("Items", 9)) {
                var listtag = compoundtag.getList("Items", 10);
                ItemUsage.spawnItemContents(itemEntity, listtag.stream()
                        .map(NbtCompound.class::cast)
                        .map(ItemStack::fromNbt));
            }
        }
    }

    //TODO
//    @Override
//    public boolean damage(DamageSource damage) {
//        return super.damage(damage) && (this != AMBlockRegistry.TRANSMUTATION_TABLE.get().asItem() || !damage.isIn(DamageTypeTags.IS_EXPLOSION));
//    }

    //FIXME forge
//    @Override
//    public void fillItemCategory(CreativeModeTab.Output contents) {
//        if(blockSupplier.equals(AMBlockRegistry.SAND_CIRCLE) || blockSupplier.equals(AMBlockRegistry.RED_SAND_CIRCLE)){
//
//        }else{
//            contents.accept(this);
//        }
//    }

    //TODO
//    @Override
//    public ActionResult useOnBlock(ItemUsageContext context) {
//        return blockSupplier.equals(AMBlockRegistry.TRIOPS_EGGS) ? InteractionResult.PASS : super.useOnBlock(context);
//    }

    //TODO
//    @Override
//    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
//        if(blockSupplier.equals(AMBlockRegistry.TRIOPS_EGGS)){
//            BlockHitResult blockhitresult = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
//            BlockHitResult blockhitresult1 = blockhitresult.withPosition(blockhitresult.getBlockPos().above());
//            InteractionResult interactionresult = super.useOn(new UseOnContext(player, hand, blockhitresult1));
//            return new InteractionResultHolder<>(interactionresult, player.getItemInHand(hand));
//        }else{
//            return super.use(level, player, hand);
//        }
//    }
}
