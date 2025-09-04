package com.github.alexthe666.alexsmobs.platform.fabric;

import com.github.alexthe666.alexsmobs.fabric.entity.EntityAlligatorSnappingTurtleFabric;
import com.github.alexthe666.alexsmobs.fabric.entity.EntityMungusFabric;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;

public class PlatformEntityConstructorsImpl {

    public static EntityType.Builder<EntityAlligatorSnappingTurtleFabric> createAlligatorSnappingTurtleEntityBuilder(SpawnGroup category) {
        return EntityType.Builder.create(EntityAlligatorSnappingTurtleFabric::new, category);
    }
//
//    public static EntityType.Builder<EntityBisonFabric> createBisonEntityBuilder(MobCategory category) {
//        return EntityType.Builder.of(EntityBisonFabric::new, category);
//    }
//
//    public static EntityType.Builder<EntityCockroachFabric> createCockroachEntityBuilder(MobCategory category) {
//        return EntityType.Builder.of(EntityCockroachFabric::new, category);
//    }
//
    public static EntityType.Builder<EntityMungusFabric> createMungusEntityBuilder(SpawnGroup category) {
        return EntityType.Builder.create(EntityMungusFabric::new, category);
    }
}
