package com.github.alexthe666.alexsmobs.block;

import com.github.alexthe666.alexsmobs.block.entity.TerrapinEggBlockEntity;
import com.github.alexthe666.alexsmobs.entity.EntityTerrapin;
import com.github.alexthe666.alexsmobs.entity.util.TerrapinTypes;
import com.github.alexthe666.alexsmobs.registry.AMBlockRegistry;
import com.github.alexthe666.alexsmobs.registry.AMEntityRegistry;
import com.github.alexthe666.alexsmobs.registry.AMTagRegistry;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.passive.BatEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.context.LootContextParameterSet;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class TerrapinEggBlock extends BlockWithEntity {

    public static final IntProperty HATCH = Properties.HATCH;
    public static final IntProperty EGGS = Properties.EGGS;
    private static final VoxelShape ONE_EGG_SHAPE = Block.createCuboidShape(3.0D, 0.0D, 3.0D, 12.0D, 7.0D, 12.0D);
    private static final VoxelShape MULTI_EGG_SHAPE = Block.createCuboidShape(1.0D, 0.0D, 1.0D, 15.0D, 7.0D, 15.0D);

    public TerrapinEggBlock() {
        super(Settings.create()
                .mapColor(MapColor.PALE_YELLOW)
                .strength(0.5F)
                .sounds(BlockSoundGroup.METAL)
                .ticksRandomly()
                .nonOpaque());
        this.setDefaultState(this.stateManager.getDefaultState()
                .with(HATCH, 0)
                .with(EGGS, 1));
    }

    public static boolean hasProperHabitat(BlockView reader, BlockPos blockReader) {
        return isProperHabitat(reader, blockReader.down());
    }

    @Override
    public BlockRenderType getRenderType(BlockState p_149645_1_) {
        return BlockRenderType.MODEL;
    }

    public static boolean isProperHabitat(BlockView reader, BlockPos pos) {
        return reader.getBlockState(pos).isIn(BlockTags.SAND) || reader.getBlockState(pos).isIn(AMTagRegistry.CROCODILE_SPAWNS);
    }

    @Override
    public void onSteppedOn(World worldIn, BlockPos pos, BlockState state, Entity entityIn) {
        this.tryTrample(worldIn, pos, entityIn, 100);
        super.onSteppedOn(worldIn, pos, state, entityIn);
    }

    @Override
    public void onLandedUpon(World worldIn, BlockState state, BlockPos pos, Entity entityIn, float fallDistance) {
        if (!(entityIn instanceof ZombieEntity)) {
            this.tryTrample(worldIn, pos, entityIn, 3);
        }

        super.onLandedUpon(worldIn, state, pos, entityIn, fallDistance);
    }

    private void tryTrample(World worldIn, BlockPos pos, Entity trampler, int chances) {
        if (this.canTrample(worldIn, trampler)) {
            if (!worldIn.isClient && worldIn.random.nextInt(chances) == 0) {
                var blockstate = worldIn.getBlockState(pos);
                this.removeOneEgg(worldIn, pos, blockstate);
            }
        }
    }

    private void removeOneEgg(World worldIn, BlockPos pos, BlockState state) {
        worldIn.playSound(null, pos, SoundEvents.ENTITY_TURTLE_EGG_BREAK, SoundCategory.BLOCKS, 0.7F, 0.9F + worldIn.random.nextFloat() * 0.2F);
        int i = state.get(EGGS);
        if (i <= 1) {
            worldIn.removeBlock(pos, false);
        } else {
            worldIn.setBlockState(pos, state.with(EGGS, i - 1), 2);
            worldIn.emitGameEvent(GameEvent.BLOCK_DESTROY, pos, GameEvent.Emitter.of(state));
            worldIn.syncWorldEvent(2001, pos, Block.getRawIdFromState(state));
        }

    }

    @Override
    public void randomTick(BlockState state, ServerWorld worldIn, BlockPos pos, Random random) {
        if (this.canGrow(worldIn) && hasProperHabitat(worldIn, pos)) {
            int i = state.get(HATCH);
            if (i < 2) {
                worldIn.playSound(null, pos, SoundEvents.ENTITY_TURTLE_EGG_BREAK, SoundCategory.BLOCKS, 0.7F, 0.9F + random.nextFloat() * 0.2F);
                worldIn.emitGameEvent(GameEvent.BLOCK_DESTROY, pos, GameEvent.Emitter.of(state));
                worldIn.setBlockState(pos, state.with(HATCH, i + 1), 2);
            } else {
                worldIn.playSound(null, pos, SoundEvents.ENTITY_TURTLE_EGG_HATCH, SoundCategory.BLOCKS, 0.7F, 0.9F + random.nextFloat() * 0.2F);
                worldIn.emitGameEvent(GameEvent.BLOCK_DESTROY, pos, GameEvent.Emitter.of(state));
                worldIn.removeBlock(pos, false);
                for (int j = 0; j < state.get(EGGS); ++j) {
                    worldIn.syncWorldEvent(2001, pos, Block.getRawIdFromState(state));
                    var turtleentity = AMEntityRegistry.TERRAPIN.get().create(worldIn);
                    turtleentity.setBreedingAge(-24000);
                    if(worldIn.getBlockEntity(pos) instanceof TerrapinEggBlockEntity eggTE){
                        eggTE.addAttributesToOffspring(turtleentity, random);
                    }
                    turtleentity.setFromBucket(true);
                    turtleentity.refreshPositionAndAngles((double) pos.getX() + 0.3D + (double) j * 0.2D, pos.getY(), (double) pos.getZ() + 0.3D, 0.0F, 0.0F);
                    worldIn.spawnEntity(turtleentity);
                }
            }
        }

    }

    @Override
    public void onBlockAdded(BlockState state, World worldIn, BlockPos pos, BlockState oldState, boolean isMoving) {
        if (hasProperHabitat(worldIn, pos) && !worldIn.isClient) {
            worldIn.syncWorldEvent(2005, pos, 0);
        }
    }

    private boolean canGrow(World worldIn) {
        float f = worldIn.getSkyAngle(1.0F);
        if ((double) f < 0.69D && (double) f > 0.65D) {
            return true;
        } else {
            return worldIn.random.nextInt(15) == 0;
        }
    }

    @Override
    public void afterBreak(World worldIn, PlayerEntity player, BlockPos pos, BlockState state, @Nullable BlockEntity te, ItemStack stack) {
        super.afterBreak(worldIn, player, pos, state, te, stack);
        this.removeOneEgg(worldIn, pos, state);
    }

    @Override
    public boolean canReplace(BlockState state, ItemPlacementContext useContext) {
        return useContext.getStack().getItem() == this.asItem() && state.get(EGGS) < 4 || super.canReplace(state, useContext);
    }

    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext context) {
        BlockState blockstate = context.getWorld().getBlockState(context.getBlockPos());
        return blockstate.getBlock() == this ? blockstate.with(EGGS, Math.min(4, blockstate.get(EGGS) + 1)) : super.getPlacementState(context);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView worldIn, BlockPos pos, ShapeContext context) {
        return state.get(EGGS) > 1 ? MULTI_EGG_SHAPE : ONE_EGG_SHAPE;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(HATCH, EGGS);
    }

    private boolean canTrample(World world, Entity trampler) {
        if (trampler instanceof EntityTerrapin || trampler instanceof BatEntity) {
            return false;
        }

        if (!(trampler instanceof LivingEntity)) {
            return false;
        }

        // Players can always trample
        if (trampler instanceof PlayerEntity) {
            return true;
        }

        // Cross-platform check for mob griefing gamerule
        return world.getGameRules().getBoolean(GameRules.DO_MOB_GRIEFING);
    }

    @Override
    public List<ItemStack> getDroppedStacks(BlockState state, LootContextParameterSet.Builder builder) {
        ItemStack pickaxe = builder.getOptional(LootContextParameters.TOOL);
        BlockEntity blockentity = builder.getOptional(LootContextParameters.BLOCK_ENTITY);
        boolean silkTouch = false;
        if(pickaxe != null){
            silkTouch = EnchantmentHelper.getLevel(Enchantments.SILK_TOUCH, pickaxe) > 0;
        }
        if (silkTouch && blockentity instanceof TerrapinEggBlockEntity) {
            ItemStack stack = new ItemStack(AMBlockRegistry.TERRAPIN_EGG.get());
            TerrapinEggBlockEntity egg = (TerrapinEggBlockEntity)blockentity;
            NbtCompound tag = stack.getOrCreateSubNbt("BlockEntityTag");
            NbtCompound parent1 = new NbtCompound();
            NbtCompound parent2 = new NbtCompound();
            boolean flag = false;
            if(egg.parent1 != null){
                flag = true;
                egg.parent1.writeToNBT(parent1);
            }
            if(egg.parent2 != null){
                flag = true;
                egg.parent2.writeToNBT(parent2);
            }
            if(flag){
                tag.put("Parent1Data", parent1);
                tag.put("Parent2Data", parent2);
            }
            return List.of(stack);
        }
        return List.of();
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable BlockView w, List<Text> list, TooltipContext flags) {
        super.appendTooltip(stack, w, list, flags);
        NbtCompound NbtCompound = BlockItem.getBlockEntityNbt(stack);
        if (NbtCompound != null && NbtCompound.contains("Parent1Data") && NbtCompound.contains("Parent2Data")) {
            TerrapinTypes parent1Type = TerrapinTypes.values()[MathHelper.clamp(NbtCompound.getCompound("Parent1Data").getInt("TerrapinType"), 0, TerrapinTypes.values().length - 1)];
            TerrapinTypes parent2Type = TerrapinTypes.values()[MathHelper.clamp(NbtCompound.getCompound("Parent2Data").getInt("TerrapinType"), 0, TerrapinTypes.values().length - 1)];
            String s1 = Text.translatable(parent1Type.getTranslationName()).getString();
            String s2 = Text.translatable(parent2Type.getTranslationName()).getString();
            list.add(Text.translatable("block.alexsmobs.terrapin_egg.desc", s1, s2).formatted(Formatting.GRAY));
        }
    }

    @Override
    public void onStateReplaced(BlockState state, World level, BlockPos pos, BlockState state2, boolean b) {
        if (state.isOf(AMBlockRegistry.TERRAPIN_EGG.get()) && state.get(EGGS) <= 1) {
            super.onStateReplaced(state, level, pos, state2, b);
        }
    }
    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new TerrapinEggBlockEntity(pos, state);
    }

}
