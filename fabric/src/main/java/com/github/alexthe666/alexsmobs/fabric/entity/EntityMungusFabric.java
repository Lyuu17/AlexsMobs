package com.github.alexthe666.alexsmobs.fabric.entity;

import com.github.alexthe666.alexsmobs.entity.EntityMungus;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.Level;

public class EntityMungusFabric extends EntityMungus {

    public EntityMungusFabric(EntityType<? extends Animal> animal, Level lvl) {
        super(animal, lvl);
    }
}