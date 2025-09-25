package com.github.alexthe666.alexsmobs.block;


import com.github.alexthe666.alexsmobs.block.entity.LeafcutterAnthillBlockEntity;
import com.github.alexthe666.alexsmobs.entity.EntityLeafcutterAnt;
import com.github.alexthe666.alexsmobs.entity.EntityManedWolf;
import com.github.alexthe666.alexsmobs.registry.AMAdvancementTriggerRegistry;
import com.github.alexthe666.alexsmobs.registry.AMItemRegistry;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BeehiveBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class LeafcutterAnthillBlock extends BlockWithEntity {

    public LeafcutterAnthillBlock() {
        super(AbstractBlock.Settings.create()
                .sounds(BlockSoundGroup.GRAVEL)
                .strength(0.75F));
    }

    @Override
    public ActionResult onUse(BlockState state, World worldIn, BlockPos pos, PlayerEntity player, Hand handIn, BlockHitResult hit) {
        if (worldIn.getBlockEntity(pos) instanceof LeafcutterAnthillBlockEntity hill) {
            var heldItem = player.getStackInHand(handIn);
            if (heldItem.getItem() == AMItemRegistry.GONGYLIDIA.get() && hill.hasQueen()) {
                hill.releaseQueens();
                if (!player.isCreative()) {
                    heldItem.decrement(1);
                }
            }
            return ActionResult.SUCCESS;
        }
        return ActionResult.PASS;
    }
    
    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Override
    public void onBreak(World worldIn, BlockPos pos, BlockState state, PlayerEntity player) {
        if (!worldIn.isClient && player.isCreative() && worldIn.getGameRules().getBoolean(GameRules.DO_TILE_DROPS)) {
            var tileentity = worldIn.getBlockEntity(pos);
            if (tileentity instanceof LeafcutterAnthillBlockEntity anthivetileentity) {
                var itemstack = new ItemStack(this);
                boolean flag = !anthivetileentity.hasNoAnts();
                if (!flag) {
                    return;
                }
                if (flag) {
                    var compoundnbt = new NbtCompound();
                    compoundnbt.put("Ants", anthivetileentity.getAnts());
                    itemstack.setSubNbt("BlockEntityTag", compoundnbt);
                }
                var compoundnbt1 = new NbtCompound();
                itemstack.setSubNbt("BlockStateTag", compoundnbt1);
                var itementity = new ItemEntity(worldIn, pos.getX(), pos.getY(), pos.getZ(), itemstack);
                itementity.setToDefaultPickupDelay();
                worldIn.spawnEntity(itementity);
            }
        }

        super.onBreak(worldIn, pos, state, player);
    }

    @Override
    public void onLandedUpon(World worldIn, BlockState state, BlockPos pos, Entity entityIn, float fallDistance) {
        if (entityIn instanceof LivingEntity && !(entityIn instanceof EntityManedWolf)) {
            this.angerNearbyAnts(worldIn, (LivingEntity) entityIn, pos);
            if (!worldIn.isClient && worldIn.getBlockEntity(pos) instanceof LeafcutterAnthillBlockEntity beehivetileentity) {
                beehivetileentity.angerAnts((LivingEntity) entityIn, worldIn.getBlockState(pos), BeehiveBlockEntity.BeeState.EMERGENCY);
                if(entityIn instanceof ServerPlayerEntity){
                    AMAdvancementTriggerRegistry.STOMP_LEAFCUTTER_ANTHILL.trigger((ServerPlayerEntity) entityIn);
                }
            }
        }
        super.onLandedUpon(worldIn, state, pos, entityIn, fallDistance);
    }

    @Override
    public void afterBreak(World worldIn, PlayerEntity player, BlockPos pos, BlockState state, @Nullable BlockEntity te, ItemStack stack) {
        super.afterBreak(worldIn, player, pos, state, te, stack);
        if (!worldIn.isClient && te instanceof LeafcutterAnthillBlockEntity beehivetileentity) {
            if (EnchantmentHelper.getLevel(Enchantments.SILK_TOUCH, stack) == 0) {
                beehivetileentity.angerAnts(player, state, BeehiveBlockEntity.BeeState.EMERGENCY);
                worldIn.updateComparators(pos, this);
                this.angerNearbyAnts(worldIn, pos);
            }
        }
    }

    private void angerNearbyAnts(World world, BlockPos pos) {
        var list = world.getNonSpectatingEntities(EntityLeafcutterAnt.class, (new Box(pos)).expand(20D, 6.0D, 20D));
        if (!list.isEmpty()) {
            var list1 = world.getNonSpectatingEntities(PlayerEntity.class, (new Box(pos)).expand(20D, 6.0D, 20D));
            if (list1.isEmpty()) return; //Forge: Prevent Error when no players are around.
            int i = list1.size();
            for (var beeentity : list) {
                if (beeentity.getTarget() == null) {
                    beeentity.setTarget(list1.get(world.random.nextInt(i)));
                }
            }
        }
    }

    private void angerNearbyAnts(World world, LivingEntity entity, BlockPos pos) {
        var list = world.getNonSpectatingEntities(EntityLeafcutterAnt.class, (new Box(pos)).expand(20D, 6.0D, 20D));
        if (!list.isEmpty()) {
            for (var beeentity : list) {
                if (beeentity.getTarget() == null) {
                    beeentity.setTarget(entity);
                }
            }
        }
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new LeafcutterAnthillBlockEntity(pos, state);
    }
}
