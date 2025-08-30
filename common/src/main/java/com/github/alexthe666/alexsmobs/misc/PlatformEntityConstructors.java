// common/src/main/java/com/example/platform/PlatformEntityConstructors.java

package com.github.alexthe666.alexsmobs.misc;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public class PlatformEntityConstructors {

    @ExpectPlatform
    public static <T extends Entity> EntityType.Builder<T> createAlligatorSnappingTurtleEntityBuilder(MobCategory category) {
        throw new AssertionError("Platform-specific code did not load");
    }

    @ExpectPlatform
    public static <T extends Entity> EntityType.Builder<T> createBisonEntityBuilder(MobCategory category) {
        throw new AssertionError("Platform-specific code did not load");
    }

    @ExpectPlatform
    public static <T extends Entity> EntityType.Builder<T> createCochroachEntityBuilder(MobCategory category) {
        throw new AssertionError("Platform-specific code did not load");
    }

    @ExpectPlatform
    public static <T extends Entity> EntityType.Builder<T> createMungusEntityBuilder(MobCategory category) {
        throw new AssertionError("Platform-specific code did not load");
    }
}
