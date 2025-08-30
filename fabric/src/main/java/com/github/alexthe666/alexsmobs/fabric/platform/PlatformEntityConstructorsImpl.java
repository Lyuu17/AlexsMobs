// forge/src/main/java/com/example/platform/PlatformEntityConstructorsImpl.java

package com.github.alexthe666.alexsmobs.fabric.platform;

import com.github.alexthe666.alexsmobs.fabric.entity.EntityAlligatorSnappingTurtleFabric;
import com.github.alexthe666.alexsmobs.fabric.entity.EntityBisonFabric;
import com.github.alexthe666.alexsmobs.fabric.entity.EntityCockroachFabric;
import com.github.alexthe666.alexsmobs.fabric.entity.EntityMungusFabric;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public class PlatformEntityConstructorsImpl {

    public static EntityType.Builder<EntityAlligatorSnappingTurtleFabric> createAlligatorSnappingTurtleEntityBuilder(MobCategory category) {
        return EntityType.Builder.of(EntityAlligatorSnappingTurtleFabric::new, category);
    }

    public static EntityType.Builder<EntityBisonFabric> createBisonEntityBuilder(MobCategory category) {
        return EntityType.Builder.of(EntityBisonFabric::new, category);
    }

    public static EntityType.Builder<EntityCockroachFabric> createCockroachEntityBuilder(MobCategory category) {
        return EntityType.Builder.of(EntityCockroachFabric::new, category);
    }

    public static EntityType.Builder<EntityMungusFabric> createMungusEntityBuilder(MobCategory category) {
        return EntityType.Builder.of(EntityMungusFabric::new, category);
    }
}
