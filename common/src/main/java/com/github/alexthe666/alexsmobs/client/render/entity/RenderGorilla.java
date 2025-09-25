package com.github.alexthe666.alexsmobs.client.render.entity;

import com.github.alexthe666.alexsmobs.client.model.ModelGorilla;
import com.github.alexthe666.alexsmobs.client.model.layered.LayerGorillaItem;
import com.github.alexthe666.alexsmobs.entity.EntityGorilla;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class RenderGorilla extends MobEntityRenderer<EntityGorilla, ModelGorilla> {
    private static final Identifier TEXTURE = new Identifier("alexsmobs:textures/entity/gorilla.png");
    private static final Identifier TEXTURE_SILVERBACK = new Identifier("alexsmobs:textures/entity/gorilla_silverback.png");
    private static final Identifier TEXTURE_DK = new Identifier("alexsmobs:textures/entity/gorilla_dk.png");
    private static final Identifier TEXTURE_FUNKY = new Identifier("alexsmobs:textures/entity/gorilla_funky.png");

    public RenderGorilla(EntityRendererFactory.Context renderManagerIn) {
        super(renderManagerIn, new ModelGorilla(), 0.7F);
        this.addFeature(new LayerGorillaItem(this));
    }

    @Override
    protected void scale(EntityGorilla entitylivingbaseIn, MatrixStack matrixStackIn, float partialTickTime) {
        matrixStackIn.scale(entitylivingbaseIn.getGorillaScale(), entitylivingbaseIn.getGorillaScale(), entitylivingbaseIn.getGorillaScale());
    }

    @Override
    public Identifier getTexture(EntityGorilla entity) {
        return entity.isFunkyKong() ? TEXTURE_FUNKY : entity.isDonkeyKong() ? TEXTURE_DK : entity.isSilverback() ? TEXTURE_SILVERBACK : TEXTURE;
    }
}
