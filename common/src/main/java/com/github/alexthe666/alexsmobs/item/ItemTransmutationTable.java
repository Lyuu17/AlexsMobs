package com.github.alexthe666.alexsmobs.item;

import com.github.alexthe666.alexsmobs.client.model.ModelTransmutationTable;
import com.github.alexthe666.alexsmobs.registry.AMBlockRegistry;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;

public class ItemTransmutationTable extends AMBlockItem implements IItemRender {

    private static final Identifier TRANSMUTATION_TABLE_TEXTURE = new Identifier("alexsmobs:textures/entity/farseer/transmutation_table.png");
    private static final Identifier TRANSMUTATION_TABLE_GLOW_TEXTURE = new Identifier("alexsmobs:textures/entity/farseer/transmutation_table_glow.png");
    private static final Identifier TRANSMUTATION_TABLE_OVERLAY = new Identifier("alexsmobs:textures/entity/farseer/transmutation_table_overlay.png");
    private static final ModelTransmutationTable TRANSMUTATION_TABLE_MODEL = new ModelTransmutationTable(0F);
    private static final ModelTransmutationTable TRANSMUTATION_TABLE_OVERLAY_MODEL = new ModelTransmutationTable(0.01F);

    public ItemTransmutationTable(Settings props) {
        super(AMBlockRegistry.TRANSMUTATION_TABLE, props);
    }

    @Override
    public void render(ItemStack stack, ModelTransformationMode mode, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {
        matrices.push();
        matrices.translate(0.5F, 1.6F, 0.5F);
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-180));
        TRANSMUTATION_TABLE_MODEL.resetToDefaultPose();
        TRANSMUTATION_TABLE_MODEL.render(matrices, vertexConsumers.getBuffer(RenderLayer.getEntityCutoutNoCull(TRANSMUTATION_TABLE_TEXTURE)), light, overlay, 1.0F, 1.0F, 1.0F, 1.0F);
        TRANSMUTATION_TABLE_MODEL.render(matrices, vertexConsumers.getBuffer(RenderLayer.getEntityTranslucentEmissive(TRANSMUTATION_TABLE_GLOW_TEXTURE)), light, overlay, 1.0F, 1.0F, 1.0F, 1.0F);
        TRANSMUTATION_TABLE_OVERLAY_MODEL.resetToDefaultPose();
        var staticyOverlay = vertexConsumers.getBuffer(RenderLayer.getEyes(TRANSMUTATION_TABLE_OVERLAY));
        TRANSMUTATION_TABLE_OVERLAY_MODEL.render(matrices, staticyOverlay, light, overlay, 1.0F, 1.0F, 1.0F, 1.0F);
        matrices.pop();
    }
}
