package com.github.alexthe666.alexsmobs.item;

import com.github.alexthe666.alexsmobs.client.render.item.AMItemstackRenderer;
import com.github.alexthe666.alexsmobs.entity.EntityVoidPortal;
import com.github.alexthe666.alexsmobs.registry.AMItemRegistry;
import com.google.common.collect.Lists;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.world.World;

import java.util.List;

public class ItemShatteredDimensionalCarver extends ItemDimensionalCarver implements IItemRender {

    private static List<ItemStack> DIMENSIONAL_CARVER_SHARDS;

    public ItemShatteredDimensionalCarver(Item.Settings props) {
        super(props);
    }

    private static List<ItemStack> getDimensionalCarverShards() {
        if (DIMENSIONAL_CARVER_SHARDS == null || DIMENSIONAL_CARVER_SHARDS.isEmpty()) {
            DIMENSIONAL_CARVER_SHARDS = Util.make(Lists.newArrayList(), (list) -> {
                list.add(AMItemRegistry.DIMENSIONAL_CARVER_SHARD_0.get().getDefaultStack());
                list.add(AMItemRegistry.DIMENSIONAL_CARVER_SHARD_1.get().getDefaultStack());
                list.add(AMItemRegistry.DIMENSIONAL_CARVER_SHARD_2.get().getDefaultStack());
                list.add(AMItemRegistry.DIMENSIONAL_CARVER_SHARD_3.get().getDefaultStack());
                list.add(AMItemRegistry.DIMENSIONAL_CARVER_SHARD_4.get().getDefaultStack());
                list.add(AMItemRegistry.DIMENSIONAL_CARVER_SHARD_5.get().getDefaultStack());
                list.add(AMItemRegistry.DIMENSIONAL_CARVER_SHARD_6.get().getDefaultStack());
                list.add(AMItemRegistry.DIMENSIONAL_CARVER_SHARD_7.get().getDefaultStack());
                list.add(AMItemRegistry.DIMENSIONAL_CARVER_SHARD_8.get().getDefaultStack());
                list.add(AMItemRegistry.DIMENSIONAL_CARVER_SHARD_9.get().getDefaultStack());
                list.add(AMItemRegistry.DIMENSIONAL_CARVER_SHARD_10.get().getDefaultStack());
            });
        }
        return DIMENSIONAL_CARVER_SHARDS;
    }

    @Override
    public void onPortalOpen(World worldIn, LivingEntity player, EntityVoidPortal portal, Direction dir) {
        portal.setAttachmentFacing(dir);
        portal.setShattered(true);
        portal.setLifespan(2000);
        portal.exitDimension = worldIn.getRegistryKey();
        BlockPos playerPos = player.getBlockPos();
        if (dir == Direction.DOWN) {
            portal.setDestination(new BlockPos(playerPos.getX(), worldIn.getBottomY() + 1, playerPos.getZ()));
        } else if (dir == Direction.UP) {
            portal.setDestination(new BlockPos(playerPos.getX(), worldIn.getTopY() - 1, playerPos.getZ()));
        }else{
            double worldBorderDistance = worldIn.getWorldBorder().getDistanceInsideBorder(playerPos.getX(), playerPos.getZ()) - 5D;
            BlockPos millionPos = playerPos.offset(dir.getOpposite(), (int) Math.min(worldBorderDistance, 1000000));
            portal.setDestination(millionPos);
        }
    }

    @Override
    public void render(ItemStack stack, ModelTransformationMode mode, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {
        var world = MinecraftClient.getInstance().world;

        matrices.translate(0.5F, 0.5f, 0.5f);
        float f = AMItemstackRenderer.getTicks() + MinecraftClient.getInstance().getTickDelta();
        List<ItemStack> shards = getDimensionalCarverShards();
        matrices.push();
        if(mode == ModelTransformationMode.FIRST_PERSON_LEFT_HAND){
            matrices.translate(-0.2F, 0, 0);
            matrices.scale(1.3F, 1.3F, 1.3F);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(60));
        }
        for (int i = 0; i < shards.size(); i++) {
            matrices.push();
            ItemStack shard = shards.get(i);
            matrices.translate((float) Math.sin(f * 0.15F + i * 1F) * 0.035F, -(float) Math.cos(f * 0.15F + i * 1F) * 0.035F, (float) Math.cos(f * 0.15F + i * 0.5F + Math.PI / 2F) * 0.025F);
            MinecraftClient.getInstance().getItemRenderer().renderItem(shard, mode, mode == ModelTransformationMode.GROUND ? light : 240, overlay, matrices, vertexConsumers, world, 0);
            matrices.pop();
        }
        matrices.pop();
    }
}
