package com.github.alexthe666.alexsmobs.renderer;

import com.github.alexthe666.alexsmobs.entity.EntityBlobfish;
import com.github.alexthe666.alexsmobs.model.ModelBlobfish;
import com.github.alexthe666.alexsmobs.model.ModelBlobfishDepressurized;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class RenderBlobfish extends MobEntityRenderer<EntityBlobfish, net.minecraft.client.render.entity.model.EntityModel<EntityBlobfish>> {
    private static final Identifier TEXTURE = new Identifier("alexsmobs:textures/entity/blobfish.png");
    private static final Identifier TEXTURE_DEPRESSURIZED = new Identifier("alexsmobs:textures/entity/blobfish_depressurized.png");
    private final ModelBlobfish modelFish = new ModelBlobfish();
    private final ModelBlobfishDepressurized modelDepressurized = new ModelBlobfishDepressurized();

    public RenderBlobfish(EntityRendererFactory.Context renderManagerIn) {
        super(renderManagerIn, new ModelBlobfish(), 0.35F);
    }

    @Override
    protected void scale(EntityBlobfish entitylivingbaseIn, MatrixStack matrixStackIn, float partialTickTime) {
        if(entitylivingbaseIn.isDepressurized()){
            model = modelDepressurized;
        }else{
            model = modelFish;
        }
        matrixStackIn.scale(entitylivingbaseIn.getBlobfishScale(), entitylivingbaseIn.getBlobfishScale(), entitylivingbaseIn.getBlobfishScale());
    }

    @Override
    public Identifier getTexture(EntityBlobfish entity) {
        return entity.isDepressurized() ? TEXTURE_DEPRESSURIZED : TEXTURE;
    }
}
