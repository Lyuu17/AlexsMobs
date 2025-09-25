package com.github.alexthe666.alexsmobs.client.render.entity;

import com.github.alexthe666.alexsmobs.entity.EntityAnaconda;
import com.github.alexthe666.alexsmobs.entity.util.AnacondaPartIndex;
import com.github.alexthe666.alexsmobs.client.model.ModelAnaconda;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;

public class RenderAnaconda extends MobEntityRenderer<EntityAnaconda, ModelAnaconda<EntityAnaconda>> {
    private static final Identifier TEXTURE = new Identifier("alexsmobs:textures/entity/anaconda.png");
    private static final Identifier TEXTURE_SHEDDING = new Identifier("alexsmobs:textures/entity/anaconda_shedding.png");
    private static final Identifier TEXTURE_YELLOW = new Identifier("alexsmobs:textures/entity/anaconda_yellow.png");
    private static final Identifier TEXTURE_YELLOW_SHEDDING = new Identifier("alexsmobs:textures/entity/anaconda_yellow_shedding.png");

    public RenderAnaconda(EntityRendererFactory.Context renderManagerIn) {
        super(renderManagerIn, new ModelAnaconda<>(AnacondaPartIndex.HEAD), 0.3F);
    }

    @Override
    protected void scale(EntityAnaconda entitylivingbaseIn, MatrixStack matrixStackIn, float partialTickTime) {
        matrixStackIn.scale(entitylivingbaseIn.getScaleFactor(), entitylivingbaseIn.getScaleFactor(), entitylivingbaseIn.getScaleFactor());
    }

    public static Identifier getAnacondaTexture(boolean yellow, boolean shedding) {
        return yellow ? shedding ? TEXTURE_YELLOW_SHEDDING : TEXTURE_YELLOW : shedding ? TEXTURE_SHEDDING : TEXTURE;
    }

    @NotNull
    @Override
    public Identifier getTexture(EntityAnaconda entity) {
        return getAnacondaTexture(entity.isYellow(), entity.isShedding());
    }
}
