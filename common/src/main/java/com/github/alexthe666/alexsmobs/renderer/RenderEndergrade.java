package com.github.alexthe666.alexsmobs.renderer;

import com.github.alexthe666.alexsmobs.entity.EntityEndergrade;
import com.github.alexthe666.alexsmobs.model.ModelEndergrade;
import com.github.alexthe666.alexsmobs.renderer.layer.LayerEndergradeSaddle;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

public class RenderEndergrade extends MobEntityRenderer<EntityEndergrade, ModelEndergrade> {
    private static final Identifier TEXTURE = new Identifier("alexsmobs:textures/entity/endergrade.png");

    public RenderEndergrade(EntityRendererFactory.Context renderManagerIn) {
        super(renderManagerIn, new ModelEndergrade(), 0.6F);
        this.addFeature(new LayerEndergradeSaddle(this));
    }

    @Nullable
    @Override
    protected RenderLayer getRenderLayer(EntityEndergrade p_230496_1_, boolean p_230496_2_, boolean p_230496_3_, boolean p_230496_4_) {
        Identifier Identifier = this.getTexture(p_230496_1_);
        if (p_230496_3_) {
            return RenderLayer.getItemEntityTranslucentCull(Identifier);
        } else if (p_230496_2_) {
            return RenderLayer.getEntityTranslucent(Identifier);
        } else {
            return p_230496_4_ ? RenderLayer.getOutline(Identifier) : null;
        }
    }

    @Override
    protected void scale(EntityEndergrade entitylivingbaseIn, MatrixStack matrixStackIn, float partialTickTime) {
        matrixStackIn.scale(1.2F, 1.2F, 1.2F);
    }

    @Override
    public Identifier getTexture(EntityEndergrade entity) {
        return TEXTURE;
    }
}
