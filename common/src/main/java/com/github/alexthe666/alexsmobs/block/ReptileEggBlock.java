package com.github.alexthe666.alexsmobs.block;

import com.github.alexthe666.alexsmobs.entity.EntityCaiman;
import com.github.alexthe666.alexsmobs.entity.EntityCrocodile;
import com.github.alexthe666.alexsmobs.entity.EntityPlatypus;
import com.github.alexthe666.alexsmobs.registry.AMTagRegistry;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.BatEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import org.jetbrains.annotations.Nullable;

public class ReptileEggBlock<T extends Entity> extends Block {
    public static final IntProperty HATCH = Properties.HATCH;
    public static final IntProperty EGGS = Properties.EGGS;
    private static final VoxelShape ONE_EGG_SHAPE = Block.createCuboidShape(3.0D, 0.0D, 3.0D, 12.0D, 7.0D, 12.0D);
    private static final VoxelShape MULTI_EGG_SHAPE = Block.createCuboidShape(1.0D, 0.0D, 1.0D, 15.0D, 7.0D, 15.0D);
    private final RegistrySupplier<EntityType<T>> births;

    public ReptileEggBlock(RegistrySupplier<EntityType<T>> births) {
        super(AbstractBlock.Settings.create()
                .mapColor(MapColor.PALE_YELLOW)
                .strength(0.5F)
                .sounds(BlockSoundGroup.METAL)
                .ticksRandomly()
                .nonOpaque());
        this.setDefaultState(this.stateManager.getDefaultState()
                .with(HATCH, 0)
                .with(EGGS, 1));
        this.births = births;
    }

    public static boolean hasProperHabitat(BlockView reader, BlockPos blockReader) {
        return isProperHabitat(reader, blockReader.down());
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
                var bb = new Box(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1).expand(25, 25, 25);
                if (trampler instanceof LivingEntity) {
                    var list = worldIn.getEntitiesByClass(MobEntity.class, bb, living -> living.isAlive() && living.getType() == births.get());
                    for (var living : list) {
                        if (!(living instanceof TameableEntity) || !((TameableEntity)living).isTamed() || !((TameableEntity)living).isOwner((LivingEntity) trampler)) {
                            living.setTarget((LivingEntity) trampler);
                        }
                    }
                }
                BlockState blockstate = worldIn.getBlockState(pos);
                this.removeOneEgg(worldIn, pos, blockstate);

            }

        }
    }

    private void removeOneEgg(World worldIn, BlockPos pos, BlockState state) {
        worldIn.playSound(null, pos, SoundEvents.ENTITY_TURTLE_EGG_BREAK, SoundCategory.BLOCKS, 0.7F, 0.9F + worldIn.random.nextFloat() * 0.2F);
        int i = state.get(EGGS);
        if (i <= 1) {
            worldIn.breakBlock(pos, false);
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
                worldIn.playSound(null, pos, SoundEvents.ENTITY_TURTLE_EGG_CRACK, SoundCategory.BLOCKS, 0.7F, 0.9F + random.nextFloat() * 0.2F);
                worldIn.emitGameEvent(GameEvent.BLOCK_DESTROY, pos, GameEvent.Emitter.of(state));
                worldIn.setBlockState(pos, state.with(HATCH, i + 1), 2);
            } else {
                worldIn.playSound(null, pos, SoundEvents.ENTITY_TURTLE_EGG_HATCH, SoundCategory.BLOCKS, 0.7F, 0.9F + random.nextFloat() * 0.2F);
                worldIn.emitGameEvent(GameEvent.BLOCK_DESTROY, pos, GameEvent.Emitter.of(state));
                worldIn.removeBlock(pos, false);
                for (int j = 0; j < state.get(EGGS); ++j) {
                    worldIn.syncWorldEvent(2001, pos, Block.getRawIdFromState(state));
                    var fromType = births.get().create(worldIn);
                    if(fromType instanceof AnimalEntity animal){
                        animal.setBreedingAge(-24000);
                        animal.setPositionTarget(pos, 20);
                    }
                    var biome = worldIn.getBiome(pos);
                    fromType.refreshPositionAndAngles((double) pos.getX() + 0.3D + (double) j * 0.2D, pos.getY(), (double) pos.getZ() + 0.3D, 0.0F, 0.0F);
                    if (!worldIn.isClient) {
                        var closest = worldIn.getClosestPlayer(pos.getX() + 0.5F, pos.getY() + 0.5F, pos.getZ() + 0.5F, 20, EntityPredicates.EXCEPT_SPECTATOR);
                        if (closest != null) {
                            if(fromType instanceof TameableEntity tamableAnimal){
                                tamableAnimal.setTamed(true);
                                tamableAnimal.setSitting(true);
                                tamableAnimal.setOwner(closest);
                            }
                            if(fromType instanceof EntityCrocodile crocodile){
                                crocodile.setDesert(biome.isIn(AMTagRegistry.SPAWNS_DESERT_CROCODILES));
                            }
                        }
                        worldIn.spawnEntity(fromType);
                    }
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
        if ((double) f < 0.8D && (double) f > 0.65D) {
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
        return blockstate.getBlock() == this ? blockstate.with(EGGS, Integer.valueOf(Math.min(4, blockstate.get(EGGS) + 1))) : super.getPlacementState(context);
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
        // Crocodile, Caiman, Platypus, and Bat can't trample
        if (trampler instanceof EntityCrocodile || trampler instanceof EntityCaiman || trampler instanceof EntityPlatypus || trampler instanceof BatEntity) {
            return false;
        }

        if (!(trampler instanceof LivingEntity)) {
            return false;
        }

        // Players can always trample
        if (trampler instanceof PlayerEntity) {
            return true;
        }

        // Check mobGriefing gamerule for non-player mobs
        return world.getGameRules().getBoolean(GameRules.DO_MOB_GRIEFING);
    }
}
