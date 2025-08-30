package com.github.alexthe666.alexsmobs.fabric.entity;

import com.github.alexthe666.alexsmobs.entity.EntityBison;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.Level;

public class EntityBisonFabric extends EntityBison {

    public EntityBisonFabric(EntityType<? extends Animal> animal, Level lvl) {
        super(animal, lvl);
    }
}