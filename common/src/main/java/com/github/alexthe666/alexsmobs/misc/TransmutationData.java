package com.github.alexthe666.alexsmobs.misc;

import com.github.alexthe666.alexsmobs.config.AMConfig;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import org.jetbrains.annotations.Nullable;

import java.util.Random;

public class TransmutationData {
    private final Object2DoubleMap<ItemStack> itemstackData = new Object2DoubleOpenHashMap<>();

    public void onTransmuteItem(ItemStack beingTransmuted, ItemStack turnedInto){
        double fromWeight = getWeight(beingTransmuted);
        double toWeight = getWeight(turnedInto);
        putWeight(beingTransmuted, fromWeight + calculateAddWeight(beingTransmuted.getCount()));
        putWeight(turnedInto, toWeight + calculateRemoveWeight(turnedInto.getCount()));
    }

    public double getWeight(ItemStack stack){
        for(Object2DoubleMap.Entry<ItemStack> entry : itemstackData.object2DoubleEntrySet()){
            if(ItemStack.canCombine(stack, entry.getKey())){
                return entry.getDoubleValue();
            }
        }
        return 0.0;
    }

    private static double calculateAddWeight(int count){
        return Math.log(Math.pow(count, AMConfig.transmutingWeightAddStep));
    }

    private static double calculateRemoveWeight(int count){
        return -Math.log(Math.pow(count, AMConfig.transmutingWeightRemoveStep));
    }

    public void putWeight(ItemStack stack, double newWeight){
        ItemStack replace = stack;
        for(ItemStack entry : itemstackData.keySet()){
            if(ItemStack.canCombine(stack, entry)){
                replace = entry;
                break;
            }
        }
        itemstackData.put(replace, Math.max(newWeight, 0.0F));
    }

    @Nullable
    public ItemStack getRandomItem(Random random) {
        ItemStack result = null;
        double bestValue = Double.MAX_VALUE;
        for(Object2DoubleMap.Entry<ItemStack> entry : itemstackData.object2DoubleEntrySet()){
            if(entry.getDoubleValue() <= 0.0){
                continue;
            }

            final double value = -Math.log(random.nextDouble()) / entry.getDoubleValue();
            if (value < bestValue) {
                bestValue = value;
                result = entry.getKey().copy();
            }
        }
        return result;
    }

    public NbtCompound saveAsNBT(){
        NbtCompound compound = new NbtCompound();
        var listTag = new NbtList();
        for(Object2DoubleMap.Entry<ItemStack> entry : itemstackData.object2DoubleEntrySet()) {
            NbtCompound tag = new NbtCompound();
            tag.put("Item", entry.getKey().writeNbt(new NbtCompound()));
            tag.putDouble("Weight", entry.getDoubleValue());
            listTag.add(tag);
        }
        compound.put("TransmutationData", listTag);
        return compound;
    }

    public static TransmutationData fromNBT(NbtCompound compound){
        TransmutationData data = new TransmutationData();
        if (compound.contains("TransmutationData")) {
            var listtag = compound.getList("TransmutationData", 10);
            for (int i = 0; i < listtag.size(); ++i) {
                NbtCompound innerTag = listtag.getCompound(i);
                try{
                    ItemStack from = ItemStack.fromNbt(innerTag.getCompound("Item"));
                    if(!from.isEmpty()){
                        data.putWeight(from, innerTag.getDouble("Weight"));
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
        return data;
    }

    public double getTotalWeight() {
        return itemstackData.values().doubleStream().sum();
    }
}
