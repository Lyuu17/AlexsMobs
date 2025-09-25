package com.github.alexthe666.alexsmobs.block.entity;

import com.github.alexthe666.alexsmobs.block.EndPirateAnchorBlock;
import com.github.alexthe666.alexsmobs.block.EndPirateAnchorWinchBlock;
import com.github.alexthe666.alexsmobs.registry.AMBlockEntityRegistry;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class EndPirateAnchorWinchBlockEntity extends BlockEntity {

    public float clientRoll;
    public int windCounter = 0;
    private int prevTargetChainLength;
    private int targetChainLength = 0;
    private float prevMaximumChainLength;
    private float chainLength;
    private float prevChainLength;
    private int windTime = 0;
    private int ticksExisted = 0;
    private float windProgress;
    private float prevWindProgress;
    private boolean draggingAnchor;
    private boolean anchorEW;
    private boolean pullingUp;
    private boolean hasPower;
    private int anchorPlaceCooldown = 0;

    public EndPirateAnchorWinchBlockEntity(BlockPos pos, BlockState state) {
        super(AMBlockEntityRegistry.END_PIRATE_ANCHOR_WINCH.get(), pos, state);
        prevTargetChainLength = targetChainLength;
    }

    private int calcChainLength(boolean goBelowAnchor) {
        BlockPos down = this.getPos().down();
        while (world != null && down.getY() > world.getBottomY() && !isAnchorTop(world, down) && (isAir(down) || isAnchorChain(world, down))) {
            down = down.down();
        }
        int i = 0;
        if (isAnchorTop(world, down) || goBelowAnchor) {

            if (goBelowAnchor){// && level.getBlockState(down.down(2)).getBlock() == AMBlockRegistry.END_PIRATE_ANCHOR.get()) {
                i = this.getPos().getY() - 1 - keepMovingBelowAnchor(down.down(2));
            } else {
                i = this.getPos().getY() - 1 - down.getY();
            }
        }
        if (draggingAnchor) {
            return i - 3;
        }
        return i;
    }

    private int keepMovingBelowAnchor(BlockPos below) {
        while (below.getY() > world.getBottomY() && isAir(below)) {
            below = below.down();
        }
        return below.getY();
    }

    private boolean isAir(BlockPos pos) {
        return world.isAir(pos) || isAnchorChain(world, pos) || world.getBlockState(pos).isReplaceable();
    }

    private boolean isAnchorChain(World level, BlockPos pos) {
        return level.getBlockState(pos).getBlock() instanceof EndPirateAnchorBlock && level.getBlockState(pos).get(EndPirateAnchorBlock.PIECE) == EndPirateAnchorBlock.PieceType.CHAIN;
    }

    private boolean isAnchorTop(World level, BlockPos pos) {
        return level.getBlockState(pos).getBlock() instanceof EndPirateAnchorBlock && level.getBlockState(pos.down(2)).getBlock() instanceof EndPirateAnchorBlock && level.getBlockState(pos.down(2)).get(EndPirateAnchorBlock.PIECE) == EndPirateAnchorBlock.PieceType.ANCHOR;
    }

    public void tick(World level, BlockPos pos, BlockState state, EndPirateAnchorWinchBlockEntity entity) {
        prevChainLength = chainLength;
        prevWindProgress = windProgress;
        prevTargetChainLength = targetChainLength;
        ticksExisted++;
        boolean powered = false;
        if(getCachedState().getBlock() instanceof EndPirateAnchorWinchBlock){
            powered = getCachedState().get(EndPirateAnchorWinchBlock.POWERED);
        }
        if(powered && pullingUp){
            sendDownChains();
        }
        if(!powered && !pullingUp){
            pullUpChains();
        }
        if (chainLength < targetChainLength) {
            chainLength = Math.min(chainLength + 0.1F, targetChainLength);
        }
        if (chainLength > targetChainLength) {
            chainLength = Math.max(chainLength - 0.1F, targetChainLength);
        }
        if (Math.abs(targetChainLength - chainLength) > 0.2F) {
            windTime = 5;
        }
        if (windTime > 0) {
            windCounter++;
            windTime--;
            if (windProgress < 1F) {
                windProgress += 0.25F;
            }
        } else {
            windCounter = 0;
            if (windProgress > 0F) {
                windProgress -= 0.25F;
            }
        }
        if (anchorPlaceCooldown > 0) {
            anchorPlaceCooldown--;
        }
        if (chainLength != targetChainLength && isWindingUp() && !draggingAnchor) {
            var down = this.getPos();
            if (anchorPlaceCooldown == 0 && (checkAndBreakAnchor(down.down()) || checkAndBreakAnchor(down.down(1 + (int) Math.ceil(chainLength))))) {
                draggingAnchor = true;
            }
        }
        if (chainLength == targetChainLength && draggingAnchor) {
            int offset = isWindingUp() ? 0 : targetChainLength;
            if (anchorPlaceCooldown == 0 && tryPlaceAnchor(offset)) {
                draggingAnchor = false;
            }
        }
    }

//    @Environment(EnvType.CLIENT)
//    public Box getBoundingBox() {
//        return INFINITE_EXTENT_AABB;
//    }

    public boolean checkAndBreakAnchor(BlockPos down) {
        if (world.getBlockState(down).getBlock() instanceof EndPirateAnchorBlock) {
            anchorEW = world.getBlockState(down).get(EndPirateAnchorBlock.EASTORWEST);
            BlockPos actualAnchorPos = down.down(2);
            if (world.getBlockState(actualAnchorPos).getBlock() instanceof EndPirateAnchorBlock) {
                EndPirateAnchorBlock.removeAnchor(world, actualAnchorPos, world.getBlockState(actualAnchorPos));
                this.removeChainBlocks();
                return true;
            }
        }
        return false;
    }

    public boolean tryPlaceAnchor(int offset) {
        BlockPos at = this.getPos().down(3 + offset);
        if (EndPirateAnchorBlock.isClearForPlacement(this.world, at, anchorEW)) {
            BlockState anchorState = null;//AMBlockRegistry.END_PIRATE_ANCHOR.get().getDefaultState().with(BlockEndPirateAnchor.EASTORWEST, anchorEW);
            this.world.setBlockState(at, anchorState, 2);
            EndPirateAnchorBlock.placeAnchor(world, at, anchorState);
            placeChainBlocks(offset);
            return true;
        }
        return false;
    }


    private void placeChainBlocks(int offset) {
        BlockPos at = this.getPos().down(3 + offset);
        BlockPos chainPos = at.up(3);
        while (chainPos.getY() < this.getPos().getY() - 1 && isAir(chainPos)) {
          //  this.world.setBlockState(chainPos, AMBlockRegistry.END_PIRATE_ANCHOR.get().getDefaultState().with(BlockEndPirateAnchor.PIECE, BlockEndPirateAnchor.PieceType.CHAIN).setValue(BlockEndPirateAnchor.EASTORWEST, anchorEW), 3);
            chainPos = chainPos.up();
        }
    }

    private void removeChainBlocks() {
        BlockPos chainPos = this.getPos().down(1 + (int) Math.ceil(chainLength));
        while (chainPos.getY() < this.getPos().getY()) {
            if(isAnchorChain(world, chainPos)){
                this.world.setBlockState(chainPos, Blocks.AIR.getDefaultState(), 3);
            }
            chainPos = chainPos.up();
        }
    }

    public void recalculateChains() {
        if (targetChainLength != 0) {
            prevMaximumChainLength = targetChainLength;
        }
        BlockPos at = this.getPos().down(1);
        if (isAnchorTop(world, at) && anchorPlaceCooldown == 0 && checkAndBreakAnchor(at)) {
            draggingAnchor = true;
        }
        targetChainLength = calcChainLength(draggingAnchor);
    }

    public void sendDownChains() {
        recalculateChains();
        pullingUp = false;
    }

    public void pullUpChains() {
        if (targetChainLength != 0) {
            prevMaximumChainLength = targetChainLength;
        }
        targetChainLength = 0;
        pullingUp = true;
    }

    public void onInteract(){

    }

    public float getChainLengthForRender() {
        return Math.max(targetChainLength, prevMaximumChainLength);
    }

    public float getChainLength(float partialTick) {
        return prevChainLength + (chainLength - prevChainLength) * partialTick;
    }

    public float getWindProgress(float partialTick) {
        return prevWindProgress + (windProgress - prevWindProgress) * partialTick;
    }

    public boolean isAnchorEW() {
        return anchorEW;
    }

    public boolean isWinching() {
        return windTime > 0;
    }

    public boolean isWindingUp() {
        return pullingUp;
    }

    public boolean hasAnchor() {
        return draggingAnchor;
    }

    @Override
    public void readNbt(NbtCompound compound) {
        super.readNbt(compound);
        this.pullingUp = compound.getBoolean("PullingUp");
        this.draggingAnchor = compound.getBoolean("DraggingAnchor");
        this.anchorEW = compound.getBoolean("EWAnchor");
        this.prevChainLength = this.chainLength = compound.getFloat("ChainLength");
        this.targetChainLength = compound.getInt("TargetChainLength");
    }

    @Override
    protected void writeNbt(NbtCompound compound) {
        super.writeNbt(compound);
        compound.putBoolean("PullingUp", pullingUp);
        compound.putBoolean("DraggingAnchor", draggingAnchor);
        compound.putBoolean("EWAnchor", anchorEW);
        compound.putFloat("ChainLength", chainLength);
        compound.putInt("TargetChainLength", targetChainLength);
    }
}
