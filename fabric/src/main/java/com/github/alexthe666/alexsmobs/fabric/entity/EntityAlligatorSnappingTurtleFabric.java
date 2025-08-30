package com.github.alexthe666.alexsmobs.fabric.entity;

import com.github.alexthe666.alexsmobs.entity.EntityAlligatorSnappingTurtle;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.Level;

public class EntityAlligatorSnappingTurtleFabric extends EntityAlligatorSnappingTurtle {

    public EntityAlligatorSnappingTurtleFabric(EntityType<? extends Animal> type, Level worldIn) {
        super(type, worldIn);
    }
}
