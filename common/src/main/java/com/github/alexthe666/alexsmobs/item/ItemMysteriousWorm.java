package com.github.alexthe666.alexsmobs.item;

import com.github.alexthe666.alexsmobs.client.model.ModelMysteriousWorm;
import com.github.alexthe666.alexsmobs.config.AMConfig;
import com.github.alexthe666.alexsmobs.entity.EntityVoidWorm;
import com.github.alexthe666.alexsmobs.registry.AMAdvancementTriggerRegistry;
import com.github.alexthe666.alexsmobs.registry.AMEntityRegistry;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;

import java.util.Random;
import java.util.UUID;

public class ItemMysteriousWorm extends Item implements IItemRender, IItemEntity {

    private static final ModelMysteriousWorm MYTERIOUS_WORM_MODEL = new ModelMysteriousWorm();
    private static final Identifier MYTERIOUS_WORM_TEXTURE = new Identifier("alexsmobs:textures/item/mysterious_worm_model.png");

    public ItemMysteriousWorm(Settings props) {
        super(props);
    }

    @Override
    public boolean onEntityItemUpdate(ItemEntity entity) {
        if(AMConfig.voidWormSummonable){
            String dim = entity.getWorld().getRegistryKey().getValue().toString();
            if(AMConfig.voidWormSpawnDimensions.contains(dim) && entity.getY() < -60 && !entity.isRemoved()){
                entity.kill();
                EntityVoidWorm worm = AMEntityRegistry.VOID_WORM.get().create(entity.getWorld());
                worm.setPos(entity.getX(), 0, entity.getZ());
                worm.setSegmentCount(25 + new Random().nextInt(15));
                worm.setPitch(-90.0F);
                worm.updatePostSummon = true;
                worm.setBaseMaxHealth(AMConfig.voidWormMaxHealth, true);

                if(!entity.getWorld().isClient){
                    var thrower = entity.getOwner();
                    if(thrower != null){
                        UUID uuid = thrower.getUuid();
                        if(entity.getWorld().getPlayerByUuid(uuid) instanceof ServerPlayerEntity){
                            AMAdvancementTriggerRegistry.VOID_WORM_SUMMON.trigger((ServerPlayerEntity) entity.getWorld().getPlayerByUuid(uuid));
                        }
                    }
                    entity.getWorld().spawnEntity(worm);
                }
            }
        }
        return false;
    }

    @Override
    public void render(ItemStack stack, ModelTransformationMode mode, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {
        matrices.push();
        matrices.translate(0, -2F, 0);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-180));
        MYTERIOUS_WORM_MODEL.animateStack(stack);
        MYTERIOUS_WORM_MODEL.render(matrices, vertexConsumers.getBuffer(RenderLayer.getEntityCutoutNoCull(MYTERIOUS_WORM_TEXTURE)), light, overlay, 1.0F, 1.0F, 1.0F, 1.0F);
        matrices.pop();
    }
}
