package com.github.alexthe666.alexsmobs.client.model.layered;

import com.github.alexthe666.alexsmobs.client.model.ModelAnteater;
import com.github.alexthe666.alexsmobs.client.render.entity.RenderAnteater;
import com.github.alexthe666.alexsmobs.entity.EntityAnteater;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.math.RotationAxis;

public class LayerAnteaterBaby extends FeatureRenderer<EntityAnteater, ModelAnteater> {

    public LayerAnteaterBaby(RenderAnteater render) {
        super(render);
    }

    @Override
    public void render(MatrixStack matrixStackIn, VertexConsumerProvider bufferIn, int packedLightIn, EntityAnteater roo, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        if(roo.hasPassengers() && !roo.isBaby()){
            for(var passenger : roo.getPassengerList()){
                float riderRot = passenger.prevYaw + (passenger.getYaw() - passenger.prevYaw) * partialTicks;
                var render = MinecraftClient.getInstance().getEntityRenderDispatcher().getRenderer(passenger);
                EntityModel modelBase = null;
                if (render instanceof LivingEntityRenderer) {
                    modelBase = ((LivingEntityRenderer) render).getModel();
                }
                if(modelBase != null){
                    matrixStackIn.push();
                    translateToPouch(matrixStackIn);
                    matrixStackIn.translate(0, -0.12F, 0.1F);
                    matrixStackIn.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(180F));
                    matrixStackIn.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(riderRot + 180F));
                    renderEntity(passenger, 0, 0, 0, 0, partialTicks, matrixStackIn, bufferIn, packedLightIn);
                    matrixStackIn.pop();
                }

            }
        }

    }

    public <E extends Entity> void renderEntity(E entityIn, double x, double y, double z, float yaw, float partialTicks, MatrixStack matrixStack, VertexConsumerProvider bufferIn, int packedLight) {
        EntityRenderer<? super E> render = null;
        EntityRenderDispatcher manager = MinecraftClient.getInstance().getEntityRenderDispatcher();
        try {
            render = manager.getRenderer(entityIn);

            if (render != null) {
                try {
                    render.render(entityIn, yaw, partialTicks, matrixStack, bufferIn, packedLight);
                } catch (Throwable throwable1) {
                    throw new CrashException(CrashReport.create(throwable1, "Rendering entity in world"));
                }
            }
        } catch (Throwable throwable3) {
            var crashreport = CrashReport.create(throwable3, "Rendering entity in world");
            var crashreportcategory = crashreport.addElement("Entity being rendered");
            entityIn.populateCrashReport(crashreportcategory);
            var crashreportcategory1 = crashreport.addElement("Renderer details");
            crashreportcategory1.add("Assigned renderer", render);
            crashreportcategory1.add("Rotation", yaw);
            crashreportcategory1.add("Delta", partialTicks);
            throw new CrashException(crashreport);
        }
    }

    protected void translateToPouch(MatrixStack matrixStack) {
        this.getContextModel().root.translateAndRotate(matrixStack);
        this.getContextModel().body.translateAndRotate(matrixStack);
    }
}
