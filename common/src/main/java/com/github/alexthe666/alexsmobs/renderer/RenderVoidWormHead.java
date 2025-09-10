package com.github.alexthe666.alexsmobs.renderer;

import com.github.alexthe666.alexsmobs.entity.EntityVoidWorm;
import com.github.alexthe666.alexsmobs.model.ModelVoidWorm;
import com.github.alexthe666.alexsmobs.renderer.layer.LayerVoidWormGlow;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;

public class RenderVoidWormHead extends MobEntityRenderer<EntityVoidWorm, ModelVoidWorm> {
    private static final Identifier TEXTURE = new Identifier("alexsmobs:textures/entity/void_worm/void_worm_head.png");
    private static final Identifier TEXTURE_GLOW = new Identifier("alexsmobs:textures/entity/void_worm/void_worm_head_glow.png");

    public RenderVoidWormHead(EntityRendererFactory.Context renderManagerIn) {
        super(renderManagerIn, new ModelVoidWorm(0.0f), 1F);
        this.addFeature(new LayerVoidWormGlow(this, renderManagerIn.getResourceManager(), new ModelVoidWorm(1.001F)){
            @Override
            public Identifier getGlowTexture(LivingEntity worm){
                return TEXTURE_GLOW;
            }
            @Override
            public boolean isGlowing(LivingEntity worm){
                return true;
            }
            @Override
            public float getAlpha(LivingEntity livingEntity){
                return 1.0F;
            }
        });
    }


    @NotNull
    @Override
    protected RenderLayer getRenderLayer(EntityVoidWorm jelly, boolean normal, boolean invis, boolean outline) {
        Identifier Identifier = this.getTexture(jelly);
        if (invis) {
            return RenderLayer.getItemEntityTranslucentCull(Identifier);
        } else if (normal) {
            return RenderLayer.getEntityTranslucent(Identifier);
        } else {
            return outline ? RenderLayer.getOutline(Identifier) : null;
        }
    }

    @Override
    public boolean shouldRender(EntityVoidWorm worm, Frustum camera, double camX, double camY, double camZ) {
        return worm.getPortalTicks() <= 0 && super.shouldRender(worm, camera, camX, camY, camZ);
    }

    @NotNull
    @Override
    public Identifier getTexture(EntityVoidWorm entity) {
        return TEXTURE;
    }
}
