package com.github.alexthe666.alexsmobs.block;


import com.github.alexthe666.alexsmobs.block.entity.LeafcutterAnthillBlockEntity;
import com.github.alexthe666.alexsmobs.entity.EntityLeafcutterAnt;
import com.github.alexthe666.alexsmobs.registry.AMBlockRegistry;
import com.github.alexthe666.alexsmobs.registry.AMItemRegistry;
import com.github.alexthe666.alexsmobs.registry.AMPointOfInterestRegistry;
import com.github.alexthe666.alexsmobs.registry.AMTagRegistry;
import com.google.common.base.Predicates;
import net.minecraft.block.*;
import net.minecraft.block.entity.BeehiveBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.minecraft.world.poi.PointOfInterestStorage;
import org.jetbrains.annotations.Nullable;

public class LeafcutterAntChamberBlock extends Block {
    public static final IntProperty FUNGUS = IntProperty.of("fungus", 0, 5);

    public LeafcutterAntChamberBlock() {
        super(AbstractBlock.Settings.create()
                .mapColor(MapColor.DIRT_BROWN)
                .sounds(BlockSoundGroup.GRAVEL)
                .strength(1.3F)
                .ticksRandomly());
        this.setDefaultState(this.stateManager.getDefaultState().with(FUNGUS, 0));
    }

    @Override
    public ActionResult onUse(BlockState state, World worldIn, BlockPos pos, PlayerEntity player, Hand handIn, BlockHitResult hit) {
        int fungalLevel = state.get(FUNGUS);
        if (fungalLevel == 5) {
            boolean shroomlight = false;
            for (var blockpos : BlockPos.iterate(pos.add(-1, -1, -1), pos.add(1, 1, 1))) {
                if(worldIn.getBlockState(blockpos).getBlock() == Blocks.SHROOMLIGHT){
                    shroomlight = true;
                }
            }
            if(!shroomlight){
                this.angerNearbyAnts(worldIn, pos);
            }
            worldIn.setBlockState(pos, state.with(FUNGUS, 0));
            if(!worldIn.isClient){
                if(worldIn.random.nextInt(2) == 0){
                    var dir = Direction.random(worldIn.random);
                    if(worldIn.getBlockState(pos.up()).getBlock() == AMBlockRegistry.LEAFCUTTER_ANTHILL.get()){
                        dir = Direction.DOWN;
                    }
                    BlockPos offset = pos.offset(dir);
                    if(worldIn.getBlockState(offset).isIn(AMTagRegistry.LEAFCUTTER_PUPA_USABLE_ON) && !worldIn.isSkyVisible(offset)){
                        worldIn.setBlockState(offset, this.getDefaultState());
                    }
                }
                dropStack(worldIn, pos, new ItemStack(AMItemRegistry.GONGYLIDIA.get()));
            }
            return ActionResult.SUCCESS;
        }
        return ActionResult.FAIL;
    }

    @Override
    public void randomTick(BlockState state, ServerWorld worldIn, BlockPos pos, Random random) {
        // TODO forge
//            if (!worldIn.isAreaLoaded(pos, 3))
//                return; // Forge: prevent loading unloaded chunks when checking neighbor's light and spreading

        if(worldIn.isSkyVisible(pos.up())){
            worldIn.setBlockState(pos, Blocks.DIRT.getDefaultState());
        }
    }

    @Override
    public void afterBreak(World worldIn, PlayerEntity player, BlockPos pos, BlockState state, @Nullable BlockEntity te, ItemStack stack) {
        super.afterBreak(worldIn, player, pos, state, te, stack);
        this.angerNearbyAnts(worldIn, pos);
    }

    private void angerNearbyAnts(World world, BlockPos pos) {
        var list = world.getNonSpectatingEntities(EntityLeafcutterAnt.class, (new Box(pos)).expand(20D, 6.0D, 20D));
        PlayerEntity player = null;
        var list1 = world.getNonSpectatingEntities(PlayerEntity.class, (new Box(pos)).expand(20D, 6.0D, 20D));
        if (list1.isEmpty()) return; //Forge: Prevent Error when no players are around.
        int i = list1.size();
        player = list1.get(world.random.nextInt(i));
        if (!list.isEmpty()) {
            for (var beeentity : list) {
                if (beeentity.getTarget() == null) {
                    beeentity.setTarget(player);
                }
            }
        }
        if(!world.isClient){
            var listOfHives = ((ServerWorld) world).getPointOfInterestStorage()
                    .getPositions((poiTypeHolder -> poiTypeHolder.matchesKey(AMPointOfInterestRegistry.LEAFCUTTER_ANT_HILL.getKey())), Predicates.alwaysTrue(), pos, 50, PointOfInterestStorage.OccupationStatus.ANY)
                    .toList();
            for (var pos2 : listOfHives) {
                if(world.getBlockEntity(pos2) instanceof LeafcutterAnthillBlockEntity beehivetileentity){
                    beehivetileentity.angerAnts(player, world.getBlockState(pos2), BeehiveBlockEntity.BeeState.EMERGENCY);
                }
            }
        }
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FUNGUS);
    }

}
