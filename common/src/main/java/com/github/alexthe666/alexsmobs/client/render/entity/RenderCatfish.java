package com.github.alexthe666.alexsmobs.client.render.entity;

import com.github.alexthe666.alexsmobs.client.model.ModelCatfishLarge;
import com.github.alexthe666.alexsmobs.client.model.ModelCatfishMedium;
import com.github.alexthe666.alexsmobs.client.model.ModelCatfishSmall;
import com.github.alexthe666.alexsmobs.entity.EntityCatfish;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class RenderCatfish extends MobEntityRenderer<EntityCatfish, EntityModel<EntityCatfish>> {
    private static final Identifier TEXTURE = new Identifier("alexsmobs:textures/entity/catfish_small.png");
    private static final Identifier TEXTURE_MEDIUM = new Identifier("alexsmobs:textures/entity/catfish_medium.png");
    private static final Identifier TEXTURE_LARGE = new Identifier("alexsmobs:textures/entity/catfish_large.png");
    private static final Identifier TEXTURE_SPIT = new Identifier("alexsmobs:textures/entity/catfish_small_spit.png");
    private static final Identifier TEXTURE_SPIT_MEDIUM = new Identifier("alexsmobs:textures/entity/catfish_medium_spit.png");
    private static final Identifier TEXTURE_SPIT_LARGE = new Identifier("alexsmobs:textures/entity/catfish_large_spit.png");
    private final ModelCatfishSmall modelSmall = new ModelCatfishSmall();
    private final ModelCatfishMedium modelMedium = new ModelCatfishMedium();
    private final ModelCatfishLarge modelLarge = new ModelCatfishLarge();

    public RenderCatfish(EntityRendererFactory.Context renderManagerIn) {
        super(renderManagerIn, new ModelCatfishSmall(), 0.5F);
    }

    @Override
    protected void scale(EntityCatfish entitylivingbaseIn, MatrixStack matrixStackIn, float partialTickTime) {
        if (entitylivingbaseIn.getCatfishSize() == 2) {
            model = modelLarge;
        } else if (entitylivingbaseIn.getCatfishSize() == 1) {
            model = modelMedium;
        } else {
            model = modelSmall;
        }
    }

    @Override
    public Identifier getTexture(EntityCatfish entity) {
        if(entity.getCatfishSize() == 2){
            return entity.isSpitting() ? TEXTURE_SPIT_LARGE : TEXTURE_LARGE;
        }
        if(entity.getCatfishSize() == 1){
            return entity.isSpitting() ? TEXTURE_SPIT_MEDIUM : TEXTURE_MEDIUM;
        }
        return entity.isSpitting() ? TEXTURE_SPIT : TEXTURE;
    }
}
