package com.github.alexthe666.alexsmobs.fabric.entity;

import com.github.alexthe666.alexsmobs.entity.EntityMungus;
import net.minecraft.entity.EntityType;
import net.minecraft.world.World;

public class EntityMungusFabric extends EntityMungus {

    public EntityMungusFabric(EntityType<EntityMungusFabric> animal, World lvl) {
        super(animal, lvl);
    }
}