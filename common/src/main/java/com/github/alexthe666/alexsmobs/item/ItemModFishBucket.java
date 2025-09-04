package com.github.alexthe666.alexsmobs.item;

import com.github.alexthe666.alexsmobs.entity.EntityLobster;
import com.github.alexthe666.alexsmobs.entity.util.TerrapinTypes;
import com.github.alexthe666.alexsmobs.registry.AMEntityRegistry;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.Bucketable;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.EntityBucketItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;

public class ItemModFishBucket extends EntityBucketItem {

    public ItemModFishBucket(Supplier<? extends EntityType<?>> fishTypeIn, Fluid fluid, Item.Settings builder) {
        super(fishTypeIn.get(), fluid, SoundEvents.ITEM_BUCKET_EMPTY_FISH, builder.maxCount(1));
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        var compoundnbt = stack.getNbt();
        if (this.entityType == AMEntityRegistry.LOBSTER) {
            if (compoundnbt != null && compoundnbt.contains("BucketVariantTag", 3)) {
                int i = compoundnbt.getInt("BucketVariantTag");
                String s = "entity.alexsmobs.lobster.variant_" + EntityLobster.getVariantName(i);
                tooltip.add((Text.translatable(s)).formatted(Formatting.GRAY).formatted(Formatting.ITALIC));
            }
        }
        if (this.entityType == AMEntityRegistry.TERRAPIN.get()) {
            if (compoundnbt != null && compoundnbt.contains("TerrapinData")) {
                int i = compoundnbt.getCompound("TerrapinData").getInt("TurtleType");
                tooltip.add((Text.translatable(TerrapinTypes.values()[MathHelper.clamp(i, 0, TerrapinTypes.values().length - 1)].getTranslationName())).formatted(Formatting.GRAY).formatted(Formatting.ITALIC));
            }
        }
        if (this.entityType == AMEntityRegistry.COMB_JELLY.get()) {
            if (compoundnbt != null && compoundnbt.contains("BucketVariantTag", 3)) {
                int i = compoundnbt.getInt("BucketVariantTag");
                String s = "entity.alexsmobs.comb_jelly.variant_" + i;
                tooltip.add((Text.translatable(s)).formatted(Formatting.GRAY).formatted(Formatting.ITALIC));
            }
        }
    }

    @Override
    public void onEmptied(@Nullable PlayerEntity player, World level, ItemStack stack, BlockPos pos) {
        if (level instanceof ServerWorld) {
            this.spawnFish((ServerWorld)level, stack, pos);
            level.emitGameEvent(player, GameEvent.ENTITY_PLACE, pos);
        }
    }

    private void spawnFish(ServerWorld serverLevel, ItemStack stack, BlockPos pos) {
        var entity = this.entityType.spawn(serverLevel, stack.getNbt(), null, pos, SpawnReason.BUCKET, true, false);
        if (entity instanceof Bucketable) {
            var bucketable = (Bucketable)entity;
            bucketable.copyDataFromNbt(stack.getOrCreateNbt());
            bucketable.setFromBucket(true);
        }
        addExtraAttributes(entity, stack);
    }

    private void addExtraAttributes(Entity entity, ItemStack stack) {
//        if(entity instanceof EntityCatfish catfish){
//            if(stack.is(AMItemRegistry.SMALL_CATFISH_BUCKET.get())){
//                catfish.setCatfishSize(0);
//            }else if(stack.is(AMItemRegistry.MEDIUM_CATFISH_BUCKET.get())){
//                catfish.setCatfishSize(1);
//            }else if(stack.is(AMItemRegistry.LARGE_CATFISH_BUCKET.get())){
//                catfish.setCatfishSize(2);
//            }
//        }
    }


}
