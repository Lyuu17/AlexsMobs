package com.github.alexthe666.alexsmobs.renderer;

import com.github.alexthe666.alexsmobs.AlexsMobs;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.*;
import net.minecraft.client.render.block.entity.EndPortalBlockEntityRenderer;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import org.joml.Matrix4f;

public class AMRenderLayers extends RenderLayer {

    public static final Identifier STATIC_TEXTURE = new Identifier("alexsmobs:textures/static.png");

    private static boolean encounteredMultiConsumerError = false;

    protected static final RenderPhase.Texturing RAINBOW_TEXTURING = new RenderPhase.Texturing("entity_glint_texturing", () -> {
        setupRainbowTexturing(1.2F, 4L);
    }, RenderSystem::resetTextureMatrix);
    protected static final RenderPhase.Texturing COMB_JELLY_TEXTURING = new RenderPhase.Texturing("entity_glint_texturing", () -> {
        setupRainbowTexturing(2F, 16L);
    }, RenderSystem::resetTextureMatrix);
    protected static final RenderPhase.Texturing RAINBOW_TEXTURING_LARGE = new RenderPhase.Texturing("entity_glint_texturing", () -> {
        setupRainbowTexturing2(5F, 14L);
    }, RenderSystem::resetTextureMatrix);
    protected static final RenderPhase.Texturing WEEZER_TEXTURING = new RenderPhase.Texturing("entity_glint_texturing", () -> {
        setupRainbowTexturing2(7F, 16L);
    }, RenderSystem::resetTextureMatrix);
    protected static final RenderPhase.Texturing STATIC_PORTAL_TEXTURING = new RenderPhase.Texturing("entity_glint_texturing", () -> {
        setupStaticTexturing(1.1F, 12L);
    }, RenderSystem::resetTextureMatrix);
    protected static final RenderPhase.Texturing STATIC_PARTICLE_TEXTURING = new RenderPhase.Texturing("entity_glint_texturing", () -> {
        setupStaticTexturing(0.1F, 12L);
    }, RenderSystem::resetTextureMatrix);
    protected static final RenderPhase.Texturing STATIC_ENTITY_TEXTURING = new RenderPhase.Texturing("entity_glint_texturing", () -> {
        setupStaticTexturing(3F, 12L);
    }, RenderSystem::resetTextureMatrix);



    public static final RenderLayer COMBJELLY_RAINBOW_GLINT = of("cj_rainbow_glint", VertexFormats.POSITION_TEXTURE, VertexFormat.DrawMode.QUADS, 256, false, false, RenderLayer.MultiPhaseParameters.builder().program(RenderPhase.ENTITY_GLINT_PROGRAM).texture(new RenderPhase.Texture(new Identifier("alexsmobs:textures/entity/rainbow_jelly_overlays/glint_rainbow.png"), true, false)).writeMaskState(RenderPhase.ALL_MASK).cull(RenderPhase.DISABLE_CULLING).depthTest(EQUAL_DEPTH_TEST).transparency(NO_TRANSPARENCY).texturing(COMB_JELLY_TEXTURING).build(false));
    public static final RenderLayer RAINBOW_GLINT = of("rainbow_glint", VertexFormats.POSITION_TEXTURE, VertexFormat.DrawMode.QUADS, 256, true, true, RenderLayer.MultiPhaseParameters.builder().program(RenderPhase.ENTITY_GLINT_PROGRAM).texture(new RenderPhase.Texture(new Identifier("alexsmobs:textures/entity/rainbow_jelly_overlays/glint_rainbow.png"), true, false)).writeMaskState(RenderPhase.ALL_MASK).cull(RenderPhase.DISABLE_CULLING).depthTest(EQUAL_DEPTH_TEST).transparency(GLINT_TRANSPARENCY).texturing(RAINBOW_TEXTURING).overlay(RenderPhase.ENABLE_OVERLAY_COLOR).build(true));
    public static final RenderLayer TRANS_GLINT = of("trans_glint", VertexFormats.POSITION_TEXTURE, VertexFormat.DrawMode.QUADS, 256, false, false, RenderLayer.MultiPhaseParameters.builder().program(RenderPhase.ENTITY_GLINT_PROGRAM).texture(new RenderPhase.Texture(new Identifier("alexsmobs:textures/entity/rainbow_jelly_overlays/glint_trans.png"), true, false)).writeMaskState(RenderPhase.ALL_MASK).cull(RenderPhase.DISABLE_CULLING).depthTest(EQUAL_DEPTH_TEST).transparency(GLINT_TRANSPARENCY).texturing(RAINBOW_TEXTURING).overlay(RenderPhase.ENABLE_OVERLAY_COLOR).build(true));
    public static final RenderLayer NONBI_GLINT = of("nonbi_glint", VertexFormats.POSITION_TEXTURE, VertexFormat.DrawMode.QUADS, 256, false, false, RenderLayer.MultiPhaseParameters.builder().program(RenderPhase.ENTITY_GLINT_PROGRAM).texture(new RenderPhase.Texture(new Identifier("alexsmobs:textures/entity/rainbow_jelly_overlays/glint_nonbi.png"), true, false)).writeMaskState(RenderPhase.ALL_MASK).cull(RenderPhase.DISABLE_CULLING).depthTest(EQUAL_DEPTH_TEST).transparency(GLINT_TRANSPARENCY).texturing(RAINBOW_TEXTURING).overlay(RenderPhase.ENABLE_OVERLAY_COLOR).build(true));
    public static final RenderLayer BI_GLINT = of("bi_glint", VertexFormats.POSITION_TEXTURE, VertexFormat.DrawMode.QUADS, 256, false, false, RenderLayer.MultiPhaseParameters.builder().program(RenderPhase.ENTITY_GLINT_PROGRAM).texture(new RenderPhase.Texture(new Identifier("alexsmobs:textures/entity/rainbow_jelly_overlays/glint_bi.png"), true, false)).writeMaskState(RenderPhase.ALL_MASK).cull(RenderPhase.DISABLE_CULLING).depthTest(EQUAL_DEPTH_TEST).transparency(GLINT_TRANSPARENCY).texturing(RAINBOW_TEXTURING).overlay(RenderPhase.ENABLE_OVERLAY_COLOR).build(true));
    public static final RenderLayer ACE_GLINT = of("ace_glint", VertexFormats.POSITION_TEXTURE, VertexFormat.DrawMode.QUADS, 256, false, false, RenderLayer.MultiPhaseParameters.builder().program(RenderPhase.ENTITY_GLINT_PROGRAM).texture(new RenderPhase.Texture(new Identifier("alexsmobs:textures/entity/rainbow_jelly_overlays/glint_ace.png"), true, false)).writeMaskState(RenderPhase.ALL_MASK).cull(RenderPhase.DISABLE_CULLING).depthTest(EQUAL_DEPTH_TEST).transparency(GLINT_TRANSPARENCY).texturing(RAINBOW_TEXTURING).overlay(RenderPhase.ENABLE_OVERLAY_COLOR).build(true));
    public static final RenderLayer BRAZIL_GLINT = of("brazil_glint", VertexFormats.POSITION_TEXTURE, VertexFormat.DrawMode.QUADS, 256, false, false, RenderLayer.MultiPhaseParameters.builder().program(RenderPhase.ENTITY_GLINT_PROGRAM).texture(new RenderPhase.Texture(new Identifier("alexsmobs:textures/entity/rainbow_jelly_overlays/glint_brazil.png"), true, false)).writeMaskState(RenderPhase.ALL_MASK).cull(RenderPhase.DISABLE_CULLING).depthTest(EQUAL_DEPTH_TEST).transparency(GLINT_TRANSPARENCY).texturing(RAINBOW_TEXTURING_LARGE).overlay(RenderPhase.ENABLE_OVERLAY_COLOR).build(true));
    public static final RenderLayer WEEZER_GLINT = of("weezer_glint", VertexFormats.POSITION_TEXTURE, VertexFormat.DrawMode.QUADS, 256, false, false, RenderLayer.MultiPhaseParameters.builder().program(RenderPhase.ENTITY_GLINT_PROGRAM).texture(new RenderPhase.Texture(new Identifier("alexsmobs:textures/entity/rainbow_jelly_overlays/glint_weezer.png"), false, false)).writeMaskState(RenderPhase.ALL_MASK).cull(RenderPhase.DISABLE_CULLING).depthTest(EQUAL_DEPTH_TEST).transparency(GLINT_TRANSPARENCY).texturing(WEEZER_TEXTURING).overlay(RenderPhase.ENABLE_OVERLAY_COLOR).build(true));
    public static final RenderLayer STATIC_PORTAL = of("static_portal", VertexFormats.POSITION_TEXTURE, VertexFormat.DrawMode.QUADS, 256, false, false, RenderLayer.MultiPhaseParameters.builder().program(RenderPhase.ENTITY_GLINT_PROGRAM).texture(new RenderPhase.Texture(STATIC_TEXTURE, false, false)).writeMaskState(RenderPhase.ALL_MASK).cull(RenderPhase.DISABLE_CULLING).depthTest(EQUAL_DEPTH_TEST).texturing(STATIC_PORTAL_TEXTURING).overlay(RenderPhase.ENABLE_OVERLAY_COLOR).transparency(RenderPhase.TRANSLUCENT_TRANSPARENCY).build(true));
    public static final RenderLayer STATIC_PARTICLE = of("static_particle", VertexFormats.POSITION_TEXTURE, VertexFormat.DrawMode.QUADS, 256, false, false, RenderLayer.MultiPhaseParameters.builder().program(RenderPhase.ENTITY_GLINT_PROGRAM).texture(new RenderPhase.Texture(STATIC_TEXTURE, false, false)).writeMaskState(RenderPhase.ALL_MASK).cull(RenderPhase.DISABLE_CULLING).depthTest(EQUAL_DEPTH_TEST).texturing(STATIC_PARTICLE_TEXTURING).overlay(RenderPhase.ENABLE_OVERLAY_COLOR).transparency(RenderPhase.TRANSLUCENT_TRANSPARENCY).build(true));
    public static final RenderLayer STATIC_ENTITY = of("static_entity", VertexFormats.POSITION_TEXTURE, VertexFormat.DrawMode.QUADS, 256, false, false, RenderLayer.MultiPhaseParameters.builder().program(RenderPhase.ENTITY_GLINT_PROGRAM).texture(new RenderPhase.Texture(STATIC_TEXTURE, false, false)).writeMaskState(RenderPhase.ALL_MASK).cull(RenderPhase.DISABLE_CULLING).depthTest(EQUAL_DEPTH_TEST).texturing(STATIC_ENTITY_TEXTURING).overlay(RenderPhase.ENABLE_OVERLAY_COLOR).transparency(RenderPhase.TRANSLUCENT_TRANSPARENCY).build(true));
    public static final RenderLayer VOID_WORM_PORTAL_OVERLAY = of("void_worm_portal_overlay", VertexFormats.POSITION_TEXTURE, VertexFormat.DrawMode.QUADS, 256, false, false, RenderLayer.MultiPhaseParameters.builder().program(RenderPhase.END_PORTAL_PROGRAM).depthTest(EQUAL_DEPTH_TEST).cull(RenderPhase.DISABLE_CULLING).transparency(NO_TRANSPARENCY).texture(RenderPhase.Textures.create().add(EndPortalBlockEntityRenderer.SKY_TEXTURE, false, false).add(EndPortalBlockEntityRenderer.PORTAL_TEXTURE, false, false).build()).build(false));

    protected static final RenderPhase.Transparency WORM_TRANSPARANCY = new RenderPhase.Transparency("RenderPhase.TRANSLUCENT_TRANSPARENCY", () -> {
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE, GlStateManager.SrcFactor.ONE, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
    }, () -> {
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
    });

    protected static final RenderPhase.Transparency MIMICUBE_TRANSPARANCY = new RenderPhase.Transparency("mimicube_transparency", () -> {
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SrcFactor.ONE, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
    }, () -> {
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
    });

    protected static final RenderPhase.Transparency GHOST_TRANSPARANCY = new RenderPhase.Transparency("translucent_ghost_transparency", () -> {
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GlStateManager.SrcFactor.ONE, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SrcFactor.ONE, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
    }, () -> {
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
    });

    public AMRenderLayers(String p_173178_, VertexFormat p_173179_, VertexFormat.DrawMode p_173180_, int p_173181_, boolean p_173182_, boolean p_173183_, Runnable p_173184_, Runnable p_173185_) {
        super(p_173178_, p_173179_, p_173180_, p_173181_, p_173182_, p_173183_, p_173184_, p_173185_);
    }

    public static RenderLayer getTransparentMimicube(Identifier texture) {
        var lvt_1_1_ = RenderLayer.MultiPhaseParameters.builder()
                .texture(new RenderPhase.Texture(texture, false, false))
                .program(RenderPhase.ENTITY_TRANSLUCENT_CULL_PROGRAM)
                .transparency(RenderPhase.TRANSLUCENT_TRANSPARENCY)
                .overlay(RenderPhase.ENABLE_OVERLAY_COLOR)
                .target(TRANSLUCENT_TARGET)
                .cull(RenderPhase.ENABLE_CULLING)
                .lightmap(RenderPhase.ENABLE_LIGHTMAP)
                .writeMaskState((RenderPhase.ALL_MASK))
                .depthTest(RenderPhase.LEQUAL_DEPTH_TEST)
                .build(true);
        return of("mimicube", VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, VertexFormat.DrawMode.QUADS, 256, true, true, lvt_1_1_);
    }

    public static RenderLayer getEyesFlickering(Identifier p_228652_0_, float lightLevel) {
        var lvt_1_1_ = new RenderPhase.Texture(p_228652_0_, false, false);
        return of("eye_flickering", VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, VertexFormat.DrawMode.QUADS, 256, false, true,
                RenderLayer.MultiPhaseParameters.builder()
                        .texture(lvt_1_1_)
                        .program(RenderPhase.ENTITY_TRANSLUCENT_CULL_PROGRAM)
                        .transparency(RenderPhase.TRANSLUCENT_TRANSPARENCY)
                        .cull(RenderPhase.DISABLE_CULLING)
                        .lightmap(RenderPhase.ENABLE_LIGHTMAP)
                        .overlay(RenderPhase.ENABLE_OVERLAY_COLOR)
                        .build(false));
    }

    public static RenderLayer getFullBright(Identifier p_228652_0_) {
        var lvt_1_1_ = new RenderPhase.Texture(p_228652_0_, false, false);
        return of("full_bright", VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, VertexFormat.DrawMode.QUADS, 256, false, true,
                RenderLayer.MultiPhaseParameters.builder()
                        .texture(lvt_1_1_)
                        .program(RenderPhase.ENTITY_TRANSLUCENT_CULL_PROGRAM)
                        .transparency(RenderPhase.TRANSLUCENT_TRANSPARENCY)
                        .cull(RenderPhase.DISABLE_CULLING)
                        .lightmap(RenderPhase.ENABLE_LIGHTMAP)
                        .overlay(RenderPhase.ENABLE_OVERLAY_COLOR)
                        .build(false));
    }

    public static RenderLayer getFreddy(Identifier p_228652_0_) {
        var lvt_1_1_ = new RenderPhase.Texture(p_228652_0_, false, false);
        return of("freddy", VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, VertexFormat.DrawMode.QUADS, 256, false, true,
                RenderLayer.MultiPhaseParameters.builder()
                        .texture(lvt_1_1_)
                        .program(RenderPhase.ENTITY_TRANSLUCENT_CULL_PROGRAM)
                        .transparency(RenderPhase.TRANSLUCENT_TRANSPARENCY)
                        .lightmap(RenderPhase.DISABLE_LIGHTMAP)
                        .cull(RenderPhase.DISABLE_CULLING)
                        .overlay(RenderPhase.ENABLE_OVERLAY_COLOR)
                        .build(true));
    }

    public static RenderLayer getFrilledSharkTeeth(Identifier p_228652_0_) {
        var lvt_1_1_ = new RenderPhase.Texture(p_228652_0_, false, false);
        return of("sharkteeth", VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, VertexFormat.DrawMode.QUADS, 256, false, true,
                RenderLayer.MultiPhaseParameters.builder()
                        .texture(lvt_1_1_)
                        .program(RenderPhase.ENTITY_TRANSLUCENT_CULL_PROGRAM)
                        .transparency(NO_TRANSPARENCY)
                        .cull(RenderPhase.DISABLE_CULLING)
                        .lightmap(RenderPhase.ENABLE_LIGHTMAP)
                        .overlay(RenderPhase.ENABLE_OVERLAY_COLOR)
                        .build(false));
    }

    public static RenderLayer getEyesNoCull(Identifier p_228652_0_) {
        var lvt_1_1_ = new RenderPhase.Texture(p_228652_0_, false, false);
        return of("eyes_no_cull", VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, VertexFormat.DrawMode.QUADS, 256, false, true,
                RenderLayer.MultiPhaseParameters.builder()
                        .texture(lvt_1_1_)
                        .program(RenderPhase.ENTITY_TRANSLUCENT_CULL_PROGRAM)
                        .transparency(ADDITIVE_TRANSPARENCY)
                        .writeMaskState(RenderPhase.COLOR_MASK)
                        .cull(RenderPhase.DISABLE_CULLING)
                        .build(false));
    }

    public static RenderLayer getSpectreBones(Identifier p_228652_0_) {
        var lvt_1_1_ = new RenderPhase.Texture(p_228652_0_, false, false);
        return of("spectre_bones", VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, VertexFormat.DrawMode.QUADS, 256, false, true,
                RenderLayer.MultiPhaseParameters.builder()
                        .texture(lvt_1_1_)
                        .program(RenderPhase.EYES_PROGRAM)
                        .transparency(GHOST_TRANSPARANCY)
                        .depthTest(LEQUAL_DEPTH_TEST)
                        .writeMaskState(RenderPhase.ALL_MASK)
                        .cull(RenderPhase.DISABLE_CULLING)
                        .lightmap(RenderPhase.DISABLE_LIGHTMAP)
                        .overlay(RenderPhase.ENABLE_OVERLAY_COLOR)
                        .build(false));
    }

    public static RenderLayer getGhost(Identifier p_228652_0_) {
        var lvt_1_1_ = new RenderPhase.Texture(p_228652_0_, false, false);
        return of("ghost_am", VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, VertexFormat.DrawMode.QUADS, 262144, false, true,
                RenderLayer.MultiPhaseParameters.builder()
                        .texture(lvt_1_1_)
                        .program(RenderPhase.EYES_PROGRAM)
                        .writeMaskState(RenderPhase.ALL_MASK)
                        .depthTest(EQUAL_DEPTH_TEST)
                        .lightmap(RenderPhase.DISABLE_LIGHTMAP)
                        .overlay(RenderPhase.ENABLE_OVERLAY_COLOR)
                        .transparency(GHOST_TRANSPARANCY)
                        .cull(RenderPhase.DISABLE_CULLING)
                        .build(true));
    }

    public static RenderLayer getEyesAlphaEnabled(Identifier locationIn) {
        var RenderLayer$compositestate = RenderLayer.MultiPhaseParameters.builder()
                .program(RenderLayer.EYES_PROGRAM)
                .texture(new RenderPhase.Texture(locationIn, false, false))
                .transparency(WORM_TRANSPARANCY)
                .cull(RenderPhase.DISABLE_CULLING)
                .lightmap(RenderPhase.ENABLE_LIGHTMAP)
                .overlay(RenderPhase.ENABLE_OVERLAY_COLOR)
                .depthTest(EQUAL_DEPTH_TEST)
                .build(true);
        return of("eye_alpha", VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, VertexFormat.DrawMode.QUADS, 256, true, false, RenderLayer$compositestate);
    }

    public static RenderLayer getEyesNoFog(Identifier locationIn) {
        var RenderPhase$texturestateshard = new RenderPhase.Texture(locationIn, false, false);
        return of("eyes_nofog", VertexFormats.POSITION_COLOR_TEXTURE, VertexFormat.DrawMode.QUADS, 256, true, false,
                RenderLayer.MultiPhaseParameters.builder()
                        .program(RenderLayer.OUTLINE_PROGRAM)
                        .texture(RenderPhase$texturestateshard)
                        .transparency(RenderLayer.LIGHTNING_TRANSPARENCY)
                        .writeMaskState(RenderLayer.ALL_MASK)
                        .cull(RenderPhase.DISABLE_CULLING)
                        .depthTest(RenderLayer.LEQUAL_DEPTH_TEST)
                        .overlay(RenderPhase.ENABLE_OVERLAY_COLOR)
                        .build(true));
    }

    public static RenderLayer getSunbirdShine() {
        return of("sunbird_shine", VertexFormats.POSITION_TEXTURE, VertexFormat.DrawMode.QUADS, 256, true, true,
                RenderLayer.MultiPhaseParameters.builder()
                        .program(RenderPhase.ENTITY_GLINT_PROGRAM)
                        .texture(new RenderPhase.Texture(new Identifier("alexsmobs:textures/entity/sunbird_shine.png"), true, true))
                        .lightmap(RenderPhase.ENABLE_LIGHTMAP)
                        .cull(RenderPhase.DISABLE_CULLING)
                        .transparency(RenderPhase.TRANSLUCENT_TRANSPARENCY)
                        .overlay(RenderPhase.ENABLE_OVERLAY_COLOR)
                        .depthTest(LEQUAL_DEPTH_TEST)
                        .build(true));
    }

    public static RenderLayer getSkulkBoom() {
        var renderState = RenderLayer.MultiPhaseParameters.builder()
                .program(RenderPhase.ENERGY_SWIRL_PROGRAM)
                .cull(RenderPhase.DISABLE_CULLING)
                .texture(new RenderPhase.Texture(new Identifier("alexsmobs:textures/particle/skulk_boom.png"), true, true))
                .transparency(RenderPhase.TRANSLUCENT_TRANSPARENCY)
                .lightmap(RenderPhase.ENABLE_LIGHTMAP)
                .overlay(RenderPhase.ENABLE_OVERLAY_COLOR)
                .writeMaskState(RenderPhase.ALL_MASK)
                .depthTest(RenderPhase.LEQUAL_DEPTH_TEST)
                .layering(RenderPhase.VIEW_OFFSET_Z_LAYERING)
                .build(false);
        return of("skulk_boom", VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, VertexFormat.DrawMode.QUADS, 256, true, true, renderState);
    }

    public static RenderLayer getUnderminer(Identifier texture) {
        var renderState = RenderLayer.MultiPhaseParameters.builder()
                .program(RenderPhase.ENERGY_SWIRL_PROGRAM)
                .cull(RenderPhase.DISABLE_CULLING)
                .texture(new RenderPhase.Texture(texture, false, false))
                .transparency(RenderPhase.TRANSLUCENT_TRANSPARENCY)
                .lightmap(RenderPhase.ENABLE_LIGHTMAP)
                .overlay(RenderPhase.ENABLE_OVERLAY_COLOR)
                .writeMaskState(RenderPhase.ALL_MASK)
                .depthTest(RenderPhase.LEQUAL_DEPTH_TEST)
                .layering(RenderPhase.NO_LAYERING)
                .build(false);
        return of("underminer", VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, VertexFormat.DrawMode.QUADS, 256, true, true, renderState);
    }


    public static RenderLayer getGhostPickaxe(Identifier texture) {
        var renderState = RenderLayer.MultiPhaseParameters.builder()
                .program(RenderPhase.ITEM_ENTITY_TRANSLUCENT_CULL_PROGRAM)
                .cull(RenderPhase.DISABLE_CULLING)
                .target(RenderPhase.ITEM_ENTITY_TARGET)
                .texture(new RenderPhase.Texture(texture, false, false))
                .transparency(RenderPhase.LIGHTNING_TRANSPARENCY)
                .lightmap(RenderPhase.ENABLE_LIGHTMAP)
                .overlay(RenderPhase.ENABLE_OVERLAY_COLOR)
                .writeMaskState(RenderPhase.ALL_MASK)
                .depthTest(RenderPhase.LEQUAL_DEPTH_TEST)
                .layering(RenderPhase.NO_LAYERING)
                .build(false);
        return of("ghost_pickaxe", VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, VertexFormat.DrawMode.QUADS, 256, true, true, renderState);
    }

    public static RenderLayer getGhostCrumbling(Identifier texture) {
        var texture1 = new RenderPhase.Texture(texture, false, false);
        return of("ghost_crumbling_am", VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, VertexFormat.DrawMode.QUADS, 262144, false, true,
                RenderLayer.MultiPhaseParameters.builder()
                        .texture(texture1)
                        .program(RenderPhase.ENERGY_SWIRL_PROGRAM)
                        .transparency(LIGHTNING_TRANSPARENCY)
                        .lightmap(RenderPhase.ENABLE_LIGHTMAP)
                        .overlay(RenderPhase.ENABLE_OVERLAY_COLOR)
                        .layering(RenderPhase.VIEW_OFFSET_Z_LAYERING)
                        .depthTest(RenderPhase.LEQUAL_DEPTH_TEST)
                        .cull(RenderPhase.DISABLE_CULLING)
                        .build(true));
    }

    private static void setupRainbowTexturing(float in, long time) {
        long i = Util.getMeasuringTimeMs() * time;
        float f = (float)(i % 110000L) / 110000.0F;
        float f1 = (float)(i % 30000L) / 30000.0F;
        Matrix4f matrix4f = (new Matrix4f()).translation(0, f1, 0.0F);
        matrix4f.scale(in);
        RenderSystem.setTextureMatrix(matrix4f);
    }

    private static void setupRainbowTexturing2(float in, long time){
        long i = Util.getMeasuringTimeMs() * time;
        float f = (float)(i % 110000L) / 110000.0F;
        float f1 = (float)(i % 30000L) / 30000.0F;
        float f2 = (float)Math.sin(i / 30000F);
        Matrix4f matrix4f = (new Matrix4f()).translation(f1, f2, 0.0F);
        matrix4f.scale(in);
        RenderSystem.setTextureMatrix(matrix4f);
    }

    private static void setupStaticTexturing(float in, long time){
        long i = Util.getMeasuringTimeMs() * time;
        float f = (float)(i % 110000L) / 110000.0F;
        float f1 = (float)(i % 30000L) / 30000.0F;
        float f2 = (float)Math.floor((i % 3000L) / 3000.0F * 4.0F);
        float f3 = (float)Math.sin(i / 30000F) * 0.05F;
        Matrix4f matrix4f = (new Matrix4f()).translation(f1, f2 * 0.25F + f3, 0.0F);
        matrix4f.scale(in * 1.5F, in * 0.25F, in);
        RenderSystem.setTextureMatrix(matrix4f);
    }

    public static RenderLayer getFarseerBeam() {
        var renderState = RenderLayer.MultiPhaseParameters.builder()
                .program(RenderPhase.ENERGY_SWIRL_PROGRAM)
                .cull(RenderPhase.ENABLE_CULLING)
                .texture(new RenderPhase.Texture(STATIC_TEXTURE, false, false))
                .transparency(RenderPhase.TRANSLUCENT_TRANSPARENCY)
                .lightmap(RenderPhase.ENABLE_LIGHTMAP)
                .overlay(RenderPhase.ENABLE_OVERLAY_COLOR)
                .writeMaskState(RenderPhase.COLOR_MASK)
                .depthTest(RenderPhase.LEQUAL_DEPTH_TEST)
                .layering(RenderPhase.VIEW_OFFSET_Z_LAYERING)
                .build(false);
        return of("farseer_beam", VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, VertexFormat.DrawMode.QUADS, 256, true, true, renderState);
    }

    public static VertexConsumer createMergedVertexConsumer(VertexConsumer consumer1, VertexConsumer consumer2){
        var vertexConsumer = consumer2;
        if(!encounteredMultiConsumerError){
            try{
                vertexConsumer = VertexConsumers.union(consumer1, consumer2);
            }catch (Exception e){
                AlexsMobs.LOGGER.warn("Encountered issue mixing two render types together. Likely an issue with Optifine or other rendering mod. This warning will only display once.");
                encounteredMultiConsumerError = true;
            }
        }
        return vertexConsumer;
    }
}
