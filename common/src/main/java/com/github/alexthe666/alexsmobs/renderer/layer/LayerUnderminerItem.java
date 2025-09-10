package com.github.alexthe666.alexsmobs.renderer.layer;

import com.github.alexthe666.alexsmobs.entity.EntityUnderminer;
import com.github.alexthe666.alexsmobs.model.ModelUnderminerDwarf;
import com.github.alexthe666.alexsmobs.registry.AMItemRegistry;
import com.github.alexthe666.alexsmobs.renderer.RenderUnderminer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.ModelWithArms;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.math.RotationAxis;

public class LayerUnderminerItem extends FeatureRenderer<EntityUnderminer, EntityModel<EntityUnderminer>> {

    public LayerUnderminerItem(RenderUnderminer render) {
        super(render);
    }

    public void render(MatrixStack matrixStackIn, VertexConsumerProvider bufferIn, int packedLightIn, EntityUnderminer entitylivingbaseIn, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        if(!entitylivingbaseIn.isFullyHidden()){
            ItemStack itemstack = entitylivingbaseIn.getEquippedStack(EquipmentSlot.MAINHAND);
            if(RenderUnderminer.renderWithPickaxe){
                itemstack = new ItemStack(AMItemRegistry.GHOSTLY_PICKAXE.get());
            }
            matrixStackIn.push();
            matrixStackIn.push();
            float f = entitylivingbaseIn.getMainArm() == Arm.LEFT ? 0.1F : -0.1F;
            float f1 = entitylivingbaseIn.isDwarf() ? 0.5F : 0.45F;
            if(entitylivingbaseIn.isDwarf()){
                matrixStackIn.translate(0F,  1F, 0F);
                f *= 0.3F;
            }else{
                matrixStackIn.translate(0F,  0.2F, 0);
            }
            translateToHand(entitylivingbaseIn.getMainArm(), matrixStackIn);
            matrixStackIn.translate(f,  f1,  -0.15F);

            matrixStackIn.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-90));
            matrixStackIn.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180));
            var renderer = MinecraftClient.getInstance().getEntityRenderDispatcher().getHeldItemRenderer();
            renderer.renderItem(entitylivingbaseIn, itemstack, ModelTransformationMode.THIRD_PERSON_RIGHT_HAND, false, matrixStackIn, bufferIn, packedLightIn);
            matrixStackIn.pop();
            matrixStackIn.pop();
        }
    }

    protected void translateToHand(Arm arm, MatrixStack matrixStack) {
        if(getContextModel() instanceof ModelUnderminerDwarf){
            ((ModelUnderminerDwarf)getContextModel()).setArmAngle(arm, matrixStack);
        }else if(getContextModel() instanceof ModelWithArms){
            ((ModelWithArms)getContextModel()).setArmAngle(arm, matrixStack);
        }
    }
}
