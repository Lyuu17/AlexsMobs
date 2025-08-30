package com.github.alexthe666.alexsmobs.misc;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class AMBlockPos {

    public static BlockPos fromCoords(double x, double y, double z){
        return new BlockPos((int) x, (int) y, (int) z);
    }

    public static BlockPos fromVec3(Vec3d vec3){
        return fromCoords(vec3.x, vec3.y, vec3.z);
    }
}
