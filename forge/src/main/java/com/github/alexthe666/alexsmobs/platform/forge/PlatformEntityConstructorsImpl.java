package com.github.alexthe666.alexsmobs.platform.forge;

import com.github.alexthe666.alexsmobs.forge.entity.EntityAlligatorSnappingTurtleForge;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;

public class PlatformEntityConstructorsImpl {

    public static EntityType.Builder<EntityAlligatorSnappingTurtleForge> createAlligatorSnappingTurtleEntityBuilder(SpawnGroup category) {
        return EntityType.Builder.create(EntityAlligatorSnappingTurtleForge::new, category);
    }

//    public static EntityType.Builder<EntityBisonForge> createBisonEntityBuilder(MobCategory category) {
//        return EntityType.Builder.of(EntityBisonForge::new, category);
//    }
//
//    public static EntityType.Builder<EntityCockroachForge> createCochroachEntityBuilder(MobCategory category) {
//        return EntityType.Builder.of(EntityCockroachForge::new, category);
//    }
//
//    public static EntityType.Builder<EntityMungusForge> createMungusEntityBuilder(MobCategory category) {
//        return EntityType.Builder.of(EntityMungusForge::new, category);
//    }
}
