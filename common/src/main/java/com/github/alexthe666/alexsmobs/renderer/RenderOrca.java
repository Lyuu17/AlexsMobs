package com.github.alexthe666.alexsmobs.renderer;

import com.github.alexthe666.alexsmobs.entity.EntityOrca;
import com.github.alexthe666.alexsmobs.model.ModelOrca;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class RenderOrca extends MobEntityRenderer<EntityOrca, ModelOrca> {
    private static final Identifier TEXTURE_NE = new Identifier("alexsmobs:textures/entity/orca_ne.png");
    private static final Identifier TEXTURE_NW = new Identifier("alexsmobs:textures/entity/orca_nw.png");
    private static final Identifier TEXTURE_SE = new Identifier("alexsmobs:textures/entity/orca_se.png");
    private static final Identifier TEXTURE_SW = new Identifier("alexsmobs:textures/entity/orca_sw.png");

    public RenderOrca(EntityRendererFactory.Context renderManagerIn) {
        super(renderManagerIn, new ModelOrca(), 1.0F);
    }

    @Override
    protected void scale(EntityOrca entitylivingbaseIn, MatrixStack matrixStackIn, float partialTickTime) {
        matrixStackIn.scale(1.3F, 1.3F, 1.3F);
    }

    @Override
    public Identifier getTexture(EntityOrca entity) {
        return switch (entity.getVariant()) {
            case 0 -> TEXTURE_NE;
            case 1 -> TEXTURE_NW;
            case 2 -> TEXTURE_SE;
            default -> TEXTURE_SW;
        };
    }
}
