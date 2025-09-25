package com.github.alexthe666.alexsmobs.client.render.entity;

import com.github.alexthe666.alexsmobs.entity.EntityTarantulaHawk;
import com.github.alexthe666.alexsmobs.client.model.ModelTarantulaHawk;
import com.github.alexthe666.alexsmobs.client.model.ModelTarantulaHawkBaby;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RenderTarantulaHawk extends MobEntityRenderer<EntityTarantulaHawk, EntityModel<EntityTarantulaHawk>> {
    private static final Identifier TEXTURE = new Identifier("alexsmobs:textures/entity/tarantula_hawk.png");
    private static final Identifier TEXTURE_ANGRY = new Identifier("alexsmobs:textures/entity/tarantula_hawk_angry.png");
    private static final Identifier TEXTURE_NETHER = new Identifier("alexsmobs:textures/entity/tarantula_hawk_nether.png");
    private static final Identifier TEXTURE_NETHER_ANGRY = new Identifier("alexsmobs:textures/entity/tarantula_hawk_nether_angry.png");
    private static final Identifier TEXTURE_BABY = new Identifier("alexsmobs:textures/entity/tarantula_hawk_baby.png");
    private static final ModelTarantulaHawk MODEL = new ModelTarantulaHawk();
    private static final ModelTarantulaHawkBaby MODEL_BABY = new ModelTarantulaHawkBaby();

    public RenderTarantulaHawk(EntityRendererFactory.Context renderManagerIn) {
        super(renderManagerIn, MODEL, 0.5F);
    }

    @Override
    protected void scale(EntityTarantulaHawk entitylivingbaseIn, MatrixStack matrixStackIn, float partialTickTime) {
        if(entitylivingbaseIn.isBaby()){
            this.model = MODEL_BABY;
        }else{
            this.model = MODEL;
            matrixStackIn.scale(0.9F, 0.9F, 0.9F);
            float f = entitylivingbaseIn.prevDragProgress + (entitylivingbaseIn.dragProgress - entitylivingbaseIn.prevDragProgress) * partialTickTime;
            matrixStackIn.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(f * 180 * 0.2F));
        }
    }

    @Override
    protected boolean isShaking(EntityTarantulaHawk hawk) {
        return hawk.isScared();
    }

    @Nullable
    @Override
    protected RenderLayer getRenderLayer(EntityTarantulaHawk hawk, boolean b0, boolean b1, boolean b2) {
        Identifier Identifier = this.getTexture(hawk);
        if (b1) {
            return RenderLayer.getItemEntityTranslucentCull(Identifier);
        } else if (b0) {
            return RenderLayer.getEntityTranslucent(Identifier);
        } else {
            return b2 ? RenderLayer.getOutline(Identifier) : null;
        }
    }

    @NotNull
    @Override
    public Identifier getTexture(EntityTarantulaHawk entity) {
        return entity.isBaby() ? TEXTURE_BABY : entity.isNether() ? entity.isAngry() ? TEXTURE_NETHER_ANGRY : TEXTURE_NETHER : entity.isAngry() ? TEXTURE_ANGRY : TEXTURE;
    }
}
