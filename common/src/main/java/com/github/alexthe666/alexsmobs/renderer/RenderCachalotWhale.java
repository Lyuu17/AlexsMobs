package com.github.alexthe666.alexsmobs.renderer;

import com.github.alexthe666.alexsmobs.entity.EntityCachalotPart;
import com.github.alexthe666.alexsmobs.entity.EntityCachalotWhale;
import com.github.alexthe666.alexsmobs.model.ModelCachalotWhale;
import com.github.alexthe666.alexsmobs.renderer.layer.LayerCachalotWhaleCapturedSquid;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class RenderCachalotWhale extends MobEntityRenderer<EntityCachalotWhale, ModelCachalotWhale> {
    private static final Identifier TEXTURE = new Identifier("alexsmobs:textures/entity/cachalot/cachalot_whale.png");
    private static final Identifier TEXTURE_SLEEPING = new Identifier("alexsmobs:textures/entity/cachalot/cachalot_whale_sleeping.png");
    private static final Identifier TEXTURE_ALBINO = new Identifier("alexsmobs:textures/entity/cachalot/cachalot_whale_albino.png");
    private static final Identifier TEXTURE_ALBINO_SLEEPING = new Identifier("alexsmobs:textures/entity/cachalot/cachalot_whale_albino_sleeping.png");

    public RenderCachalotWhale(EntityRendererFactory.Context renderManagerIn) {
        super(renderManagerIn, new ModelCachalotWhale(), 4.2F);
        this.addFeature(new LayerCachalotWhaleCapturedSquid(this));
    }

    @Override
    protected void scale(EntityCachalotWhale entitylivingbaseIn, MatrixStack matrixStackIn, float partialTickTime) {
    }

    @Override
    public boolean shouldRender(EntityCachalotWhale livingEntityIn, Frustum camera, double camX, double camY, double camZ) {
        if (super.shouldRender(livingEntityIn, camera, camX, camY, camZ)) {
            return true;
        } else {
            for(EntityCachalotPart part : livingEntityIn.whaleParts){
                if(camera.isVisible(part.getBoundingBox())){
                    return true;
                }
            }
            return false;
        }
    }

    @Override
    public Identifier getTexture(EntityCachalotWhale entity) {
        if(entity.isAlbino()){
            return entity.isSleeping() || entity.isBeached() ? TEXTURE_ALBINO_SLEEPING : TEXTURE_ALBINO;
        }else {
            return entity.isSleeping() || entity.isBeached()  ? TEXTURE_SLEEPING : TEXTURE;
        }
    }
}
