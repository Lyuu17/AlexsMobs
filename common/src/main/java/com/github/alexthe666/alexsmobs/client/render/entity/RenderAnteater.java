package com.github.alexthe666.alexsmobs.client.render.entity;

import com.github.alexthe666.alexsmobs.client.model.ModelAnteater;
import com.github.alexthe666.alexsmobs.client.model.layered.LayerAnteaterBaby;
import com.github.alexthe666.alexsmobs.client.model.layered.LayerAnteaterTongueItem;
import com.github.alexthe666.alexsmobs.entity.EntityAnteater;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;

public class RenderAnteater extends MobEntityRenderer<EntityAnteater, ModelAnteater> {
    private static final Identifier TEXTURE = new Identifier("alexsmobs:textures/entity/anteater.png");
    private static final Identifier TEXTURE_PETER = new Identifier("alexsmobs:textures/entity/anteater_peter.png");

    public RenderAnteater(EntityRendererFactory.Context renderManagerIn) {
        super(renderManagerIn, new ModelAnteater(), 0.45F);
        this.addFeature(new LayerAnteaterTongueItem(this));
        this.addFeature(new LayerAnteaterBaby(this));
    }

    @Override
    public boolean shouldRender(EntityAnteater anteater, Frustum p_225626_2_, double p_225626_3_, double p_225626_5_, double p_225626_7_) {
        if(anteater.isBaby() && anteater.hasVehicle() && anteater.getVehicle() instanceof EntityAnteater){
            return false;
        }
        return super.shouldRender(anteater, p_225626_2_, p_225626_3_, p_225626_5_, p_225626_7_);
    }

    @Override
    protected void scale(EntityAnteater entitylivingbaseIn, MatrixStack matrixStackIn, float partialTickTime) {
    }

    @NotNull
    @Override
    public Identifier getTexture(EntityAnteater entity) {
        return entity.isPeter() ? TEXTURE_PETER : TEXTURE;
    }
}
