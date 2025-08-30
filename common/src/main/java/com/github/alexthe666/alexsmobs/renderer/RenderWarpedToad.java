package com.github.alexthe666.alexsmobs.renderer;

import com.github.alexthe666.alexsmobs.entity.EntityWarpedToad;
import com.github.alexthe666.alexsmobs.model.ModelWarpedToad;
import com.github.alexthe666.alexsmobs.renderer.layer.LayerWarpedToadGlow;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;

public class RenderWarpedToad extends MobEntityRenderer<EntityWarpedToad, ModelWarpedToad> {
    private static final Identifier TEXTURE = new Identifier("alexsmobs:textures/entity/warped_toad.png");
    private static final Identifier TEXTURE_BLINKING = new Identifier("alexsmobs:textures/entity/warped_toad_blink.png");
    private static final Identifier TEXTURE_PEPE = new Identifier("alexsmobs:textures/entity/warped_toad_pepe.png");
    private static final Identifier TEXTURE_PEPE_BLINKING = new Identifier("alexsmobs:textures/entity/warped_toad_pepe_blink.png");

    public RenderWarpedToad(EntityRendererFactory.Context renderManagerIn) {
        super(renderManagerIn, new ModelWarpedToad(), 0.85F);
        this.addFeature(new LayerWarpedToadGlow(this));
    }

    @Override
    protected void scale(EntityWarpedToad entitylivingbaseIn, MatrixStack matrixStackIn, float partialTickTime) {
        matrixStackIn.scale(1.25F, 1.25F, 1.25F);
    }

    @NotNull
    @Override
    public Identifier getTexture(EntityWarpedToad entity) {
        if(entity.isBased()){
            return entity.isBlinking() ? TEXTURE_PEPE_BLINKING : TEXTURE_PEPE;
        }else{
            return entity.isBlinking() ? TEXTURE_BLINKING : TEXTURE;
        }
    }
}
