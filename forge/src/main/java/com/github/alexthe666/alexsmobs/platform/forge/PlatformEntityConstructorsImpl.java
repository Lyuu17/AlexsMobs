package com.github.alexthe666.alexsmobs.platform.forge;

import com.github.alexthe666.alexsmobs.forge.entity.EntityAlligatorSnappingTurtleForge;
import com.github.alexthe666.alexsmobs.forge.entity.EntityBisonForge;
import com.github.alexthe666.alexsmobs.forge.entity.EntityCockroachForge;
import com.github.alexthe666.alexsmobs.forge.entity.EntityMungusForge;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;

public class PlatformEntityConstructorsImpl {

    public static EntityType.Builder<EntityAlligatorSnappingTurtleForge> createAlligatorSnappingTurtleEntityBuilder(SpawnGroup category) {
        return EntityType.Builder.create(EntityAlligatorSnappingTurtleForge::new, category);
    }

    public static EntityType.Builder<EntityBisonForge> createBisonEntityBuilder(SpawnGroup category) {
        return EntityType.Builder.create(EntityBisonForge::new, category);
    }

    public static EntityType.Builder<EntityCockroachForge> createCockroachEntityBuilder(SpawnGroup category) {
        return EntityType.Builder.create(EntityCockroachForge::new, category);
    }

    public static EntityType.Builder<EntityMungusForge> createMungusEntityBuilder(SpawnGroup category) {
        return EntityType.Builder.create(EntityMungusForge::new, category);
    }
}
