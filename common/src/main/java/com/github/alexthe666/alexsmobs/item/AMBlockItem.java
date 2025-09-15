package com.github.alexthe666.alexsmobs.item;

import com.github.alexthe666.alexsmobs.registry.AMBlockRegistry;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.block.Block;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

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

    @Override
    public boolean damage(DamageSource damage) {
        return super.damage(damage) && (this != AMBlockRegistry.TRANSMUTATION_TABLE.get().asItem() || !damage.isIn(DamageTypeTags.IS_EXPLOSION));
    }

    @Override
    public void fillItemCategory(ItemGroup.Entries contents) {
        if(blockSupplier.equals(AMBlockRegistry.SAND_CIRCLE) || blockSupplier.equals(AMBlockRegistry.RED_SAND_CIRCLE)){

        }else{
            contents.add(this);
        }
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        return blockSupplier.equals(AMBlockRegistry.TRIOPS_EGGS) ? ActionResult.PASS : super.useOnBlock(context);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        if(blockSupplier.equals(AMBlockRegistry.TRIOPS_EGGS)){
            var blockhitresult = raycast(world, player, RaycastContext.FluidHandling.SOURCE_ONLY);
            var blockhitresult1 = blockhitresult.withBlockPos(blockhitresult.getBlockPos().up());
            var interactionresult = super.useOnBlock(new ItemUsageContext(player, hand, blockhitresult1));
            return new TypedActionResult<>(interactionresult, player.getStackInHand(hand));
        }else{
            return super.use(world, player, hand);
        }
    }
}
