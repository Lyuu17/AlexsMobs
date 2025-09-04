package com.github.alexthe666.alexsmobs.renderer;

import com.github.alexthe666.alexsmobs.entity.EntityCrimsonMosquito;
import com.github.alexthe666.alexsmobs.model.ModelCrimsonMosquito;
import com.github.alexthe666.alexsmobs.renderer.layer.LayerCrimsonMosquitoBlood;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class RenderCrimsonMosquito extends MobEntityRenderer<EntityCrimsonMosquito, ModelCrimsonMosquito> {
    private static final Identifier TEXTURE = new Identifier("alexsmobs:textures/entity/crimson_mosquito.png");
    private static final Identifier TEXTURE_SICK = new Identifier("alexsmobs:textures/entity/crimson_mosquito_blue.png");
    private static final Identifier TEXTURE_FLY = new Identifier("alexsmobs:textures/entity/crimson_mosquito_fly.png");
    private static final Identifier TEXTURE_SICK_FLY = new Identifier("alexsmobs:textures/entity/crimson_mosquito_fly_blue.png");

    public RenderCrimsonMosquito(EntityRendererFactory.Context renderManagerIn) {
        super(renderManagerIn, new ModelCrimsonMosquito(), 0.6F);
        this.addFeature(new LayerCrimsonMosquitoBlood(this));
    }

    @Override
    protected void scale(EntityCrimsonMosquito entitylivingbaseIn, MatrixStack matrixStackIn, float partialTickTime) {
        float mosScale = entitylivingbaseIn.prevMosquitoScale + (entitylivingbaseIn.getMosquitoScale() - entitylivingbaseIn.prevMosquitoScale) * partialTickTime;
        matrixStackIn.scale(mosScale * 1.2F, mosScale * 1.2F, mosScale * 1.2F);
    }

    @Override
    protected boolean isShaking(EntityCrimsonMosquito fly) {
        return fly.isSick() || fly.getFleeingEntityId() != -1;
    }

    @Override
    protected void setupTransforms(EntityCrimsonMosquito entityLiving, MatrixStack matrixStackIn, float ageInTicks, float rotationYaw, float partialTicks) {
        if (this.isShaking(entityLiving)) {
            rotationYaw += (float) (Math.cos((double) entityLiving.age * 7F) * Math.PI * (double) 0.9F);
            float vibrate = 0.05F * entityLiving.getMosquitoScale();
            matrixStackIn.translate((entityLiving.getRandom().nextFloat() - 0.5F) * vibrate, (entityLiving.getRandom().nextFloat() - 0.5F) * vibrate, (entityLiving.getRandom().nextFloat() - 0.5F) * vibrate);
        }
        super.setupTransforms(entityLiving, matrixStackIn, ageInTicks, rotationYaw, partialTicks);
    }

    @Override
    public Identifier getTexture(EntityCrimsonMosquito entity) {
        if (entity.isSick()) {
            return entity.isFromFly() ? TEXTURE_SICK_FLY : TEXTURE_SICK;
        }
        return entity.isFromFly() ? TEXTURE_FLY : TEXTURE;
    }
}
