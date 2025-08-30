// common/src/main/java/com/example/platform/PlatformEntityConstructors.java

package com.github.alexthe666.alexsmobs.platform;

import com.github.alexthe666.alexsmobs.entity.EntityAlligatorSnappingTurtle;
import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;

public class PlatformEntityConstructors {

    @ExpectPlatform
    public static <T extends EntityAlligatorSnappingTurtle> EntityType.Builder<T> createAlligatorSnappingTurtleEntityBuilder(SpawnGroup category) {
        throw new AssertionError("Platform-specific code did not load");
    }
//
//    @ExpectPlatform
//    public static <T extends Entity> EntityType.Builder<T> createBisonEntityBuilder(MobCategory category) {
//        throw new AssertionError("Platform-specific code did not load");
//    }
//
//    @ExpectPlatform
//    public static <T extends Entity> EntityType.Builder<T> createCochroachEntityBuilder(MobCategory category) {
//        throw new AssertionError("Platform-specific code did not load");
//    }
//
//    @ExpectPlatform
//    public static <T extends Entity> EntityType.Builder<T> createMungusEntityBuilder(MobCategory category) {
//        throw new AssertionError("Platform-specific code did not load");
//    }
}
