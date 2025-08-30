// forge/src/main/java/com/example/platform/PlatformEntityConstructorsImpl.java

package com.github.alexthe666.alexsmobs.forge.platform;

import com.github.alexthe666.alexsmobs.forge.entity.EntityAlligatorSnappingTurtleForge;
import com.github.alexthe666.alexsmobs.forge.entity.EntityBisonForge;
import com.github.alexthe666.alexsmobs.forge.entity.EntityCockroachForge;
import com.github.alexthe666.alexsmobs.forge.entity.EntityMungusForge;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public class PlatformEntityConstructorsImpl {

    public static EntityType.Builder<EntityAlligatorSnappingTurtleForge> createAlligatorSnappingTurtleEntityBuilder(MobCategory category) {
        return EntityType.Builder.of(EntityAlligatorSnappingTurtleForge::new, category)
                .setTrackingRange(10);
    }

    public static EntityType.Builder<EntityBisonForge> createBisonEntityBuilder(MobCategory category) {
        return EntityType.Builder.of(EntityBisonForge::new, category)
                .setTrackingRange(10);
    }

    public static EntityType.Builder<EntityCockroachForge> createCochroachEntityBuilder(MobCategory category) {
        return EntityType.Builder.of(EntityCockroachForge::new, category)
                .setTrackingRange(5);
    }

    public static EntityType.Builder<EntityMungusForge> createMungusEntityBuilder(MobCategory category) {
        return EntityType.Builder.of(EntityMungusForge::new, category)
                .setTrackingRange(10);
    }
}
