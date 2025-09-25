package com.github.alexthe666.alexsmobs.client.model.layered;

import com.github.alexthe666.alexsmobs.client.model.ModelLeafcutterAnt;
import com.github.alexthe666.alexsmobs.client.render.entity.OctopusColorRegistry;
import com.github.alexthe666.alexsmobs.client.render.entity.RenderLeafcutterAnt;
import com.github.alexthe666.alexsmobs.entity.EntityLeafcutterAnt;
import com.iafenvoy.uranus.client.model.AdvancedEntityModel;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;

public class LayerLeafcutterAntLeaf extends FeatureRenderer<EntityLeafcutterAnt, AdvancedEntityModel<EntityLeafcutterAnt>> {

    private static final Identifier TEXTURE_0 = new Identifier("alexsmobs:textures/entity/leafcutter_ant_leaf_0.png");
    private static final Identifier TEXTURE_1 = new Identifier("alexsmobs:textures/entity/leafcutter_ant_leaf_1.png");
    private static final Identifier TEXTURE_2 = new Identifier("alexsmobs:textures/entity/leafcutter_ant_leaf_2.png");

    public LayerLeafcutterAntLeaf(RenderLeafcutterAnt render) {
        super(render);
    }

    @Override
    public void render(MatrixStack matrixStackIn, VertexConsumerProvider bufferIn, int packedLightIn, EntityLeafcutterAnt entitylivingbaseIn, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        if (entitylivingbaseIn.hasLeaf() && !entitylivingbaseIn.isQueen() && this.getContextModel() instanceof ModelLeafcutterAnt) {
            final int leafType = entitylivingbaseIn.getId() % 3;
            final Identifier res = switch (leafType) {
                case 2 -> TEXTURE_2;
                case 1 -> TEXTURE_1;
                default -> TEXTURE_0;
            };
            VertexConsumer ivertexbuilder = bufferIn.getBuffer(RenderLayer.getEntityCutoutNoCull(res));
            int leafColor = MinecraftClient.getInstance().itemColors.getColor(new ItemStack(Items.JUNGLE_LEAVES), 0);
            if(entitylivingbaseIn.getHarvestedPos() != null && entitylivingbaseIn.getHarvestedState() != null){
                leafColor = OctopusColorRegistry.getBlockColor(entitylivingbaseIn.getHarvestedState());
            }
            final float f = (float)(leafColor >> 16 & 255) / 255.0F;
            final float f1 = (float)(leafColor >> 8 & 255) / 255.0F;
            final float f2 = (float)(leafColor & 255) / 255.0F;
            this.getContextModel().render(matrixStackIn, ivertexbuilder, packedLightIn, LivingEntityRenderer.getOverlay(entitylivingbaseIn, 0.0F), f, f1, f2, 1.0F);


        }
    }
}
