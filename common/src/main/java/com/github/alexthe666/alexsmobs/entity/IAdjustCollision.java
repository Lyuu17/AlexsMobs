package com.github.alexthe666.alexsmobs.entity;

import net.minecraft.util.math.Vec3d;

import java.util.function.Supplier;

public interface IAdjustCollision {
    Vec3d adjustMovementForCollisions(Vec3d vec3, Supplier<Vec3d> supplier);
}
