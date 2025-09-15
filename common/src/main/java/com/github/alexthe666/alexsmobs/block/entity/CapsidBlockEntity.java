package com.github.alexthe666.alexsmobs.block.entity;


import com.github.alexthe666.alexsmobs.AlexsMobs;
import com.github.alexthe666.alexsmobs.block.CapsidBlock;
import com.github.alexthe666.alexsmobs.misc.CapsidRecipe;
import com.github.alexthe666.alexsmobs.registry.AMBlockEntityRegistry;
import com.github.alexthe666.alexsmobs.registry.AMEntityRegistry;
import com.github.alexthe666.alexsmobs.registry.AMSoundRegistry;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.EndRodBlock;
import net.minecraft.block.entity.LockableContainerBlockEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.Random;

public class CapsidBlockEntity extends LockableContainerBlockEntity implements SidedInventory {
    private static final int[] slotsTop = new int[]{0};
    public int ticksExisted;
    public float prevFloatUpProgress;
    public float floatUpProgress;
    public float prevYawSwitchProgress;
    public float yawSwitchProgress;
    public boolean vibratingThisTick = false;
//    net.minecraftforge.common.util.LazyOptional<? extends net.minecraftforge.items.IItemHandler>[] handlers =
//            net.minecraftforge.items.wrapper.SidedInvWrapper.create(this, Direction.UP, Direction.DOWN);
    private float yawTarget = 0;
    private int transformTime = 0;
    private boolean fnaf = false;
    private CapsidRecipe lastRecipe = null;
    private DefaultedList<ItemStack> stacks = DefaultedList.ofSize(1, ItemStack.EMPTY);

    public CapsidBlockEntity(BlockPos pos, BlockState state) {
        super(AMBlockEntityRegistry.CAPSID.get(), pos, state);
    }

    public void tick(World level, BlockPos pos, BlockState state, CapsidBlockEntity entity) {
        prevFloatUpProgress = floatUpProgress;
        prevYawSwitchProgress = yawSwitchProgress;
        ticksExisted++;
        vibratingThisTick = false;
        if (!this.getStack(0).isEmpty()) {
            var up = level.getBlockEntity(this.pos.up());
            if (up instanceof Inventory inventory) {
                if (floatUpProgress >= 1) {
                    var itemStack = transfer(inventory, this.getStack(0).copy(), Direction.DOWN);
                    if (itemStack.isEmpty()) {
                        this.setStack(0, ItemStack.EMPTY);
                        this.markDirty();
                    }

                    yawTarget = 0F;
                    floatUpProgress = 0F;
                    yawSwitchProgress = 0F;
                } else {
                    if (up instanceof CapsidBlockEntity) {
                        yawTarget = MathHelper.wrapDegrees(((CapsidBlockEntity) up).getBlockAngle() - this.getBlockAngle());
                    }else{
                        yawTarget = 0F;
                    }
                    if(yawTarget < yawSwitchProgress){
                        yawSwitchProgress += yawTarget * 0.1F;
                    }else if(yawTarget > yawSwitchProgress){
                        yawSwitchProgress += yawTarget * 0.1F;
                    }
                    floatUpProgress += 0.05F;
                }
            } else {
                floatUpProgress = 0F;
            }
            if(this.getStack(0).getItem() == Items.ENDER_EYE && level.getBlockState(this.getPos().down()).getBlock() == Blocks.END_ROD && level.getBlockState(this.getPos().down()).get(EndRodBlock.FACING).getAxis() == Direction.Axis.Y){
                vibratingThisTick = true;
                if(transformTime > 20){
                    this.setStack(0, ItemStack.EMPTY);
                    this.world.breakBlock(this.getPos(), false);
                    this.world.breakBlock(this.getPos().down(), false);
                    var phage = AMEntityRegistry.ENDERIOPHAGE.get().create(level);
                    phage.setPos(this.getPos().getX() + 0.5F, this.getPos().getY() - 1.0F, this.getPos().getZ() + 0.5F);
                    phage.setVariant(0);
                    if(!level.isClient){
                        level.spawnEntity(phage);
                    }
                }
            }else if(!this.getStack(0).isEmpty() && level.getBlockState(this.getPos().up()).getBlock() != this.getCachedState().getBlock()){
                if(lastRecipe != null && lastRecipe.matches(this.getStack(0))){
                    floatUpProgress = 0.0F;
                    vibratingThisTick = true;
                    if(transformTime == 1 && (AlexsMobs.isAprilFools() || new Random().nextInt(100) == 0)){
                        fnaf = true;
                        level.playSound(null, this.getPos(), AMSoundRegistry.MOSQUITO_CAPSID_CONVERT.get(), SoundCategory.BLOCKS, 1.0F, 1.0F);
                    }
                    if(transformTime > (fnaf ? Math.max(160, lastRecipe.getTime()) : lastRecipe.getTime())) {
                        var current = this.getStack(0).copy();
                        current.decrement(1);
                        fnaf = false;
                        if(!current.isEmpty()){
                            var itemEntity = new ItemEntity(this.world, this.getPos().getX() + 0.5F, this.getPos().getY() + 0.5F, this.getPos().getZ() + 0.5F, current);
                            if(!level.isClient){
                                level.spawnEntity(itemEntity);
                            }
                        }
                        this.setStack(0, lastRecipe.getResult().copy());
                    }
                }
            }
        }
        if(!vibratingThisTick){
            transformTime = 0;
        }else{
            transformTime++;
        }
    }

    private static ItemStack transfer(Inventory inventory, ItemStack itemStack, @Nullable Direction direction) {
        if (inventory instanceof SidedInventory sidedInventory && direction != null) {
            int[] is = sidedInventory.getAvailableSlots(direction);
            for(int i = 0; i < is.length && !itemStack.isEmpty(); ++i) {
                itemStack = insertStack(inventory, itemStack, is[i]);
            }
        } else {
            int sidedInventory = inventory.size();
            for(int is = 0; is < sidedInventory && !itemStack.isEmpty(); ++is) {
                itemStack = insertStack(inventory, itemStack, is);
            }
        }
        return itemStack;
    }

    private static ItemStack insertStack(Inventory inventory, ItemStack stack, int slot) {
        ItemStack existing = inventory.getStack(slot);
        var dirty = false;

        if (inventory.isValid(slot, stack)) {
            if (existing.isEmpty()) {
                inventory.setStack(slot, stack.copy());
                dirty = true;
                return ItemStack.EMPTY;
            } else if (ItemStack.canCombine(existing, stack)) {
                int maxInsert = Math.min(inventory.getMaxCountPerStack(), existing.getMaxCount()) - existing.getCount();
                if (maxInsert > 0) {
                    int toInsert = Math.min(maxInsert, stack.getCount());
                    existing.increment(toInsert);
                    stack.decrement(toInsert);
                    dirty = toInsert > 0;
                }
            }
            if (dirty) {
                inventory.markDirty();
            }
        }
        return stack.isEmpty() ? ItemStack.EMPTY : stack;
    }

    @Override
    public int size() {
        return this.stacks.size();
    }

    @Override
    public ItemStack getStack(int index) {
        return this.stacks.get(index);
    }

    @Override
    public ItemStack removeStack(int index, int count) {
        if (!this.stacks.get(index).isEmpty()) {
            ItemStack itemstack;

            if (this.stacks.get(index).getCount() <= count) {
                itemstack = this.stacks.get(index);
                this.stacks.set(index, ItemStack.EMPTY);
            } else {
                itemstack = this.stacks.get(index).split(count);

                if (this.stacks.get(index).isEmpty()) {
                    this.stacks.set(index, ItemStack.EMPTY);
                }

            }
            return itemstack;
        } else {
            return ItemStack.EMPTY;
        }
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        this.stacks.set(slot, stack);
        if (!stack.isEmpty() && stack.getCount() > this.getMaxCountPerStack()) {
            stack.setCount(this.getMaxCountPerStack());
        }
        lastRecipe = AlexsMobs.getCapsidRecipeManager().getRecipeFor(stack);
        this.writeNbt(this.toInitialChunkDataNbt());
        // FIXME crash
//        if (!world.isClient) {
//            AlexsMobs.sendMSGToAll(new UpdateCapsidPacket(this.getPos().asLong(), stacks.get(0)));
//        }
    }

    public ItemStack getStackInSlotOnClosing(int index) {
        if (!this.stacks.get(index).isEmpty()) {
            ItemStack itemstack = this.stacks.get(index);
            this.stacks.set(index, itemstack);
            return itemstack;
        } else {
            return ItemStack.EMPTY;
        }
    }

    @Override
    public void readNbt(NbtCompound compound) {
        super.readNbt(compound);
        this.stacks = DefaultedList.ofSize(this.size(), ItemStack.EMPTY);
        Inventories.readNbt(compound, this.stacks);
    }

    @Override
    public void writeNbt(NbtCompound compound) {
        super.writeNbt(compound);
        Inventories.writeNbt(compound, this.stacks);
    }

    @Override
    public void onOpen(PlayerEntity player) {
    }

    @Override
    public void onClose(PlayerEntity player) {
    }

    @Override
    public boolean canInsert(int index, ItemStack stack, Direction direction) {
        return true;
    }

    @Override
    public int getMaxCountPerStack() {
        return 64;
    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        return true;
    }

    @Override
    public void clear() {
        this.stacks.clear();
    }

    @Override
    public int[] getAvailableSlots(Direction side) {
        return slotsTop;
    }

    @Override
    public boolean canExtract(int index, ItemStack stack, Direction direction) {
        return false;
    }

    @Override
    public boolean hasCustomName() {
        return false;
    }

    @Override
    public boolean isValid(int index, ItemStack stack) {
        return true;
    }

    @Override
    public BlockEntityUpdateS2CPacket toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    // FIXME forge?
//    @Override
//    public void onDataPacket(ClientConnection net, BlockEntityUpdateS2CPacket packet) {
//        if (packet != null && packet.getTag() != null) {
//            this.stacks = NonNullList.withSize(this.size(), ItemStack.EMPTY);
//            ContainerHelper.loadAllItems(packet.getTag(), this.stacks);
//        }
//    }

    @Override
    public NbtCompound toInitialChunkDataNbt() {
        return this.createNbt();
    }

    @Override
    public ItemStack removeStack(int index) {
        ItemStack lvt_2_1_ = this.stacks.get(index);
        if (lvt_2_1_.isEmpty()) {
            return ItemStack.EMPTY;
        } else {
            this.stacks.set(index, ItemStack.EMPTY);
            return lvt_2_1_;
        }
    }

    @Override
    public Text getDisplayName() {
        return this.getContainerName();
    }

    @Override
    protected Text getContainerName() {
        return Text.translatable("block.alexsmobs.capsid");
    }

    @Override
    protected ScreenHandler createScreenHandler(int syncId, PlayerInventory playerInventory) {
        return null;
    }

    @Override
    public boolean isEmpty() {
        for (int i = 0; i < this.size(); i++) {
            if (!this.getStack(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public float getBlockAngle() {
        if (this.getCachedState().getBlock() instanceof CapsidBlock) {
            var dir = this.getCachedState().get(CapsidBlock.HORIZONTAL_FACING);
            return dir.asRotation();
        }
        return 0.0F;
    }

    // FIXME forge
//    @Override
//    public <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(net.minecraftforge.common.capabilities.Capability<T> capability, @Nullable Direction facing) {
//        if (!this.remove && facing != null && capability == ForgeCapabilities.ITEM_HANDLER) {
//            if (facing == Direction.DOWN)
//                return handlers[0].cast();
//            else
//                return handlers[1].cast();
//        }
//        return super.getCapability(capability, facing);
//    }
}
