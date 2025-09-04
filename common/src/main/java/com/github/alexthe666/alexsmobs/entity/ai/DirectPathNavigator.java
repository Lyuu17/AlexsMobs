package com.github.alexthe666.alexsmobs.entity.ai;

import net.minecraft.entity.ai.pathing.MobNavigation;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.world.World;

public class DirectPathNavigator extends MobNavigation {

    private final MobEntity mob;
    private float yMobOffset = 0;

    public DirectPathNavigator(MobEntity mob, World world) {
        this(mob, world, 0);
    }

    public DirectPathNavigator(MobEntity mob, World world, float yMobOffset) {
        super(mob, world);
        this.mob = mob;
        this.yMobOffset = yMobOffset;
    }

    @Override
    public void tick() {
        ++this.tickCount;
    }

    @Override
    public boolean startMovingTo(double x, double y, double z, double speedIn) {
        mob.getMoveControl().moveTo(x, y, z, speedIn);
        return true;
    }

    @Override
    public boolean startMovingAlong(Path path, double speedIn) {
        mob.getMoveControl().moveTo(entity.getX(), entity.getY() + yMobOffset, entity.getZ(), speedIn);
        return true;
    }

}
