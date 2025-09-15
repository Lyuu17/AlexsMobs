package com.github.alexthe666.alexsmobs.platform;

import com.github.alexthe666.alexsmobs.entity.EntityAlligatorSnappingTurtle;
import com.github.alexthe666.alexsmobs.entity.EntityBison;
import com.github.alexthe666.alexsmobs.entity.EntityCockroach;
import com.github.alexthe666.alexsmobs.entity.EntityMungus;
import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;

public class PlatformEntityConstructors {

    @ExpectPlatform
    public static <T extends EntityAlligatorSnappingTurtle> EntityType.Builder<T> createAlligatorSnappingTurtleEntityBuilder(SpawnGroup category) {
        throw new AssertionError("Platform-specific code did not load");
    }

    @ExpectPlatform
    public static <T extends EntityBison> EntityType.Builder<T> createBisonEntityBuilder(SpawnGroup category) {
        throw new AssertionError("Platform-specific code did not load");
    }

    @ExpectPlatform
    public static <T extends EntityCockroach> EntityType.Builder<T> createCockroachEntityBuilder(SpawnGroup category) {
        throw new AssertionError("Platform-specific code did not load");
    }

    @ExpectPlatform
    public static <T extends EntityMungus> EntityType.Builder<T> createMungusEntityBuilder(SpawnGroup category) {
        throw new AssertionError("Platform-specific code did not load");
    }
}
