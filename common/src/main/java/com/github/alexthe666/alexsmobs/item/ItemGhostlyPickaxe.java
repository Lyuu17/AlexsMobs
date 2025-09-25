package com.github.alexthe666.alexsmobs.item;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.PickaxeItem;
import net.minecraft.item.ToolMaterials;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.stat.Stats;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ItemGhostlyPickaxe extends PickaxeItem {

    public ItemGhostlyPickaxe(Settings props) {
        super(ToolMaterials.IRON, 1, -2.8F, props);
    }

    public static boolean shouldStoreInGhost(LivingEntity player, ItemStack stack){
        return player instanceof PlayerEntity && ((PlayerEntity)player).getInventory().getEmptySlot() == -1 ;
    }

    @Override
    public float getMiningSpeedMultiplier(ItemStack stack, BlockState blockState) {
        return blockState.isIn(BlockTags.PICKAXE_MINEABLE) ? 20.0F : 1.0F;
    }

    @Override
    public boolean postMine(ItemStack stack, World level, BlockState state, BlockPos pos, LivingEntity user) {
        if(shouldStoreInGhost(user, stack)){
            if(user instanceof PlayerEntity player){
                player.incrementStat(Stats.MINED.getOrCreateStat(state.getBlock()));
                player.addExhaustion(0.005F);
            }
            if(!level.isClient){
                var blockentity = state.hasBlockEntity() ? level.getBlockEntity(pos) : null;
                Block.getDroppedStacks(state, (ServerWorld)level, pos, blockentity, user, stack).forEach((item) -> {
                    putItemInGhostInventoryOrDrop(user, stack, item);
                });
                state.onStacksDropped((ServerWorld)level, pos, stack, true);
                int fortuneLevel = EnchantmentHelper.getLevel(Enchantments.FORTUNE, stack);
                int silkTouchLevel = EnchantmentHelper.getLevel(Enchantments.SILK_TOUCH, stack);
                // FIXME forge
//                int exp = state.getExpDrop((ServerWorld)level, level.random, pos, fortuneLevel, silkTouchLevel);
//                if(exp > 0){
//                    state.getBlock().dropExperience((ServerWorld)level, pos, exp);
//                }
            }
        }
        return super.postMine(stack, level, state, pos, user);
    }

    private static void putItemInGhostInventoryOrDrop(LivingEntity user, ItemStack pickaxe, ItemStack item) {
        NbtCompound NbtCompound = pickaxe.getOrCreateNbt();
        var container = new SimpleInventory(9);
        if(NbtCompound.contains("Items")){
            container.readNbtList(NbtCompound.getList("Items", 10));
        }
        if(user instanceof PlayerEntity player){
            if(player.getInventory().insertStack(item)){
                return;
            }else if(container.canInsert(item)){
                ItemStack leftover = container.addStack(item);
                NbtCompound.put("Items", container.toNbtList());
                pickaxe.setNbt(NbtCompound);
                item = leftover;

            }
        }
        if(!item.isEmpty()){
            user.dropStack(item);
        }
    }

    @Override
    public void inventoryTick(ItemStack stack, World level, Entity entity, int i, boolean offhand) {
        super.inventoryTick(stack, level, entity, i, offhand);
        if(entity instanceof PlayerEntity player){
            if(player.age % 3 == 0){
                var NbtCompound = stack.getOrCreateNbt();
                var container = new SimpleInventory(9);
                boolean flag = false;
                if(NbtCompound.contains("Items")){
                    container.readNbtList(NbtCompound.getList("Items", 10));
                }
                for(int slot = 0; slot < container.size(); slot++) {
                    var stackAt = container.getStack(slot);
                    if(!stackAt.isEmpty() && player.giveItemStack(stackAt)){
                        container.removeStack(slot, stack.getCount());
                        flag = true;
                        break;
                    }
                }
                if (flag) {
                    NbtCompound.put("Items", container.toNbtList());
                    stack.setNbt(NbtCompound);
                }
            }
        }
    }

    @Override
    public boolean canRepair(ItemStack pickaxe, ItemStack stack) {
        return stack.isOf(Items.PHANTOM_MEMBRANE);
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World worldIn, List<Text> tooltip, TooltipContext flagIn) {
        super.appendTooltip(stack, worldIn, tooltip, flagIn);
        var NbtCompound = stack.getNbt();
        if (NbtCompound != null && NbtCompound.contains("Items", 9)) {
            var container = new SimpleInventory(9);
            container.readNbtList(NbtCompound.getList("Items", 10));
            int i = 0;
            int j = 0;

            for(int slot = 0; slot < container.size(); slot++) {
                var itemstack = container.getStack(slot);
                if (!itemstack.isEmpty()) {
                    ++j;
                    if (i <= 4) {
                        ++i;
                        var mutablecomponent = itemstack.getName().copy();
                        mutablecomponent.append(" x").append(String.valueOf(itemstack.getCount()));
                        tooltip.add(mutablecomponent.formatted(Formatting.DARK_AQUA));
                    }
                }
            }

            if (j - i > 0) {
                tooltip.add(Text.translatable("container.shulkerBox.more", j - i).formatted(Formatting.DARK_AQUA, Formatting.ITALIC));
            }
        }
    }

    private void dropAllContents(World level, Vec3d vec3, ItemStack pickaxe){
        var NbtCompound = pickaxe.getNbt();
        if (NbtCompound != null && NbtCompound.contains("Items", 9)) {
            var container = new SimpleInventory(9);
            container.readNbtList(NbtCompound.getList("Items", 10));
            for (int slot = 0; slot < container.size(); slot++) {
                var itemstack = container.getStack(slot);
                if (!itemstack.isEmpty()) {
                    var itemEntity = new ItemEntity(level, vec3.x, vec3.y, vec3.z, itemstack.copy());
                    if(level.spawnEntity(itemEntity)){
                        container.removeStack(slot, itemstack.getCount());
                    }
                }
            }
            NbtCompound.put("Items", container.toNbtList());
            pickaxe.setNbt(NbtCompound);
        }
    }

    @Override
    public void onItemEntityDestroyed(ItemEntity itemEntity) {
        dropAllContents(itemEntity.getWorld(), itemEntity.getPos(), itemEntity.getStack());
    }

//    // FIXME forge
//    @Override
//    public boolean damage(int amount, Random random, @Nullable ServerPlayerEntity player) {
//        int i = super.damage(amount, random, player);
//        if(i + stack.getDamageValue() >= stack.getMaxDamage() && entity != null){
//            dropAllContents(entity.getWorld(), entity.getPos(), stack);
//        }
//        return i;
//    }
}
