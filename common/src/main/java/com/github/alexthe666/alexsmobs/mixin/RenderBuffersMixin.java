package com.github.alexthe666.alexsmobs.mixin;

import com.github.alexthe666.alexsmobs.client.render.entity.AMRenderLayers;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferBuilderStorage;
import net.minecraft.client.render.RenderLayer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.SortedMap;

@Mixin(BufferBuilderStorage.class)
public class RenderBuffersMixin {
	@Shadow
	@Final
	private SortedMap<RenderLayer, BufferBuilder> entityBuilders;

	@Unique
	private static void alexsmobs$assignBufferBuilder(SortedMap<RenderLayer, BufferBuilder> builderStorage, RenderLayer type) {
		builderStorage.put(type, new BufferBuilder(type.getExpectedBufferSize()));
	}

	@Inject(method = "<init>()V", at = @At("RETURN"))
	private void alexsmobs$onInit(CallbackInfo ci) {
		alexsmobs$assignBufferBuilder(entityBuilders, AMRenderLayers.COMBJELLY_RAINBOW_GLINT);
		alexsmobs$assignBufferBuilder(entityBuilders, AMRenderLayers.VOID_WORM_PORTAL_OVERLAY);
		alexsmobs$assignBufferBuilder(entityBuilders, AMRenderLayers.STATIC_PORTAL);
		alexsmobs$assignBufferBuilder(entityBuilders, AMRenderLayers.STATIC_PARTICLE);
		alexsmobs$assignBufferBuilder(entityBuilders, AMRenderLayers.STATIC_ENTITY);
	}
}