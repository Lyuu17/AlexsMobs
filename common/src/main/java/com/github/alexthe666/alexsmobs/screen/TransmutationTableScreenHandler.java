package com.github.alexthe666.alexsmobs.screen;

import com.github.alexthe666.alexsmobs.AlexsMobs;
import com.github.alexthe666.alexsmobs.block.entity.TransmutationTableBlockEntity;
import com.github.alexthe666.alexsmobs.config.AMConfig;
import com.github.alexthe666.alexsmobs.packet.RequestTransmutationRerollPacket;
import com.github.alexthe666.alexsmobs.packet.TransmuteFromMenuPacket;
import com.github.alexthe666.alexsmobs.registry.AMMenuRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registries;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Identifier;

import java.util.Objects;

public class TransmutationTableScreenHandler extends ScreenHandler {

    private final PlayerEntity player;
    private final Slot transmuteSlot;
    private final TransmutationTableBlockEntity table;

    public final SimpleInventory inventory = new SimpleInventory(1);

    public TransmutationTableScreenHandler(int syncId, PlayerInventory playerInventory, PacketByteBuf packetByteBuf) {
        this(syncId, playerInventory, Objects.requireNonNull((TransmutationTableBlockEntity) playerInventory.player.getWorld().getBlockEntity(packetByteBuf.readBlockPos())));
    }

    public TransmutationTableScreenHandler(int syncId, PlayerInventory playerInventory, TransmutationTableBlockEntity table) {
        super(AMMenuRegistry.TRANSMUTATION_TABLE, syncId);
        this.table = table;
        this.player = playerInventory.player;
        this.addSlot(transmuteSlot = new Slot(this.inventory, 0, 83, 83) {
            @Override
            public boolean canInsert(ItemStack stack) {
                Identifier name = Registries.ITEM.getId(stack.getItem());
                return stack.getMaxCount() > 1 && (name == null || !AMConfig.transmutationBlacklist.contains(name.toString()));
            }

            @Override
            public void setStack(ItemStack stack) {
                super.setStack(stack);

                if (table != null && !table.hasPossibilities()) {
                    if (player.getWorld().isClient) {
                        AlexsMobs.sendMSGToServer(new RequestTransmutationRerollPacket(table.getPos(), player.getUuid()));
                    }
                }
            }
        });
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, 119 + i * 18));
            }
        }

        for (int k = 0; k < 9; ++k) {
            this.addSlot(new Slot(playerInventory, k, 8 + k * 18, 177));
        }
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return this.inventory.canPlayerUse(player);
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slotIndex) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(slotIndex);
        if (slot != null && slot.hasStack()) {
            var itemstack1 = slot.getStack();
            itemstack = itemstack1.copy();
            if (slotIndex != 0) {
                if (!this.insertItem(itemstack1, 0, 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.insertItem(itemstack1, 1, 36, false)) {
                return ItemStack.EMPTY;
            }

            if (itemstack1.isEmpty()) {
                slot.setStack(ItemStack.EMPTY);
            }

            slot.markDirty();
            if (itemstack1.getCount() == itemstack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTakeItem(player, itemstack1);
            this.sendContentUpdates();
        }

        return itemstack;
    }

    @Override
    public boolean onButtonClick(PlayerEntity player, int buttonId) {
        if(player.getWorld().isClient){
            AlexsMobs.sendMSGToServer(new TransmuteFromMenuPacket(player.getId(), buttonId));
        }
        return true;
    }

    public void transmute(PlayerEntity player, int buttonId){
        ItemStack from = transmuteSlot.getStack();
        int cost = AMConfig.transmutingExperienceCost;
        ItemStack setTo = table.getPossibility(buttonId).copy();
        double divisible = from.getMaxCount() / (double)setTo.getMaxCount();
        if(!player.getWorld().isClient && table != null && divisible > 0 && table.hasPossibilities() && !from.isEmpty() && (player.experienceLevel >= cost || player.getAbilities().creativeMode)){
            int newStackSize = (int)Math.floor(from.getCount() / divisible);
            setTo.setCount(Math.max(newStackSize, 1));
            transmuteSlot.setStack(setTo);
            player.addExperienceLevels(-cost);
            table.postTransmute(player, from, setTo);
        }
    }

    @Override
    public void onClosed(PlayerEntity player) {
        super.onClosed(player);
        this.dropInventory(player, this.inventory);
    }
}