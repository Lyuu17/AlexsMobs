package com.github.alexthe666.alexsmobs.renderer.misc;

import com.google.gson.JsonObject;
import net.minecraft.resource.metadata.ResourceMetadataReader;
import net.minecraft.util.JsonHelper;

public class VoidWormMetadataSection {
    public static final VoidWormMetadataSection.Serializer SERIALIZER = new VoidWormMetadataSection.Serializer();
    private final boolean hasEndPortalTexture;

    public VoidWormMetadataSection(){
        this.hasEndPortalTexture = false;
    }

    public VoidWormMetadataSection(boolean hasEndPortalTexture) {
        this.hasEndPortalTexture = hasEndPortalTexture;
    }

    public boolean isEndPortalTexture() {
        return this.hasEndPortalTexture;
    }

    private static class Serializer implements ResourceMetadataReader<VoidWormMetadataSection> {
        public VoidWormMetadataSection fromJson(JsonObject json) {
            return new VoidWormMetadataSection(JsonHelper.getBoolean(json, "end_portal_texture"));
        }

        @Override
        public String getKey() {
            return "void_worm";
        }
    }

}
