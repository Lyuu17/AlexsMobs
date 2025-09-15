package com.github.alexthe666.alexsmobs.item;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class ItemTabIcon extends ItemInventoryOnly {
    public ItemTabIcon(Item.Settings properties) {
        super(properties);
    }

    public static boolean hasCustomEntityDisplay(ItemStack stack){
        return stack.getNbt() != null && stack.getNbt().contains("DisplayEntityType");
    }

    public static String getCustomDisplayEntityString(ItemStack stack){
        return stack.getNbt().getString("DisplayEntityType");
    }

    // FIXME forge
//    @Override
//    public void initializeClient(java.util.function.Consumer<IClientItemExtensions> consumer) {
//        consumer.accept((IClientItemExtensions)AlexsMobs.PROXY.getISTERProperties());
//    }
//
//    @Nullable
//    public static EntityType getEntityType(@Nullable NbtCompound tag) {
//        if (tag != null && tag.contains("DisplayEntityType")) {
//            String entityType = tag.getString("DisplayEntityType");
//           return ForgeRegistries.ENTITY_TYPES.getValue(Identifier.tryParse(entityType));
//        }
//        return null;
//    }
}
