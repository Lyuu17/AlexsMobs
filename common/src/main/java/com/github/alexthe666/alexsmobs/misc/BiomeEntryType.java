package com.github.alexthe666.alexsmobs.misc;

public enum BiomeEntryType {
    REGISTRY_NAME(false),
    BIOME_TAG(false);

    private final boolean deprecated;

    BiomeEntryType(boolean depreciated){
        this.deprecated = depreciated;
    }

    public boolean isDeprecated() {
        return deprecated;
    }
}