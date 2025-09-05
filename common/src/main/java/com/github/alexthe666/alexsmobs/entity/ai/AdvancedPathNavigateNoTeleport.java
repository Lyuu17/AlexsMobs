package com.github.alexthe666.alexsmobs.entity.ai;

import com.iafenvoy.uranus.object.entity.pathfinding.raycoms.AdvancedPathNavigate;
import com.iafenvoy.uranus.object.entity.pathfinding.raycoms.PathingStuckHandler;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.world.World;

public class AdvancedPathNavigateNoTeleport extends AdvancedPathNavigate {

    private final boolean wide;

    public AdvancedPathNavigateNoTeleport(MobEntity entity, World world, MovementType type, boolean climbing, boolean wide) {
        super(entity, world, type, entity.getWidth(), entity.getHeight(), PathingStuckHandler.createStuckHandler());
        this.getPathingOptions().setCanClimb(climbing);
        this.wide = wide;
    }

    public AdvancedPathNavigateNoTeleport(MobEntity entity, World world, boolean wide) {
        this(entity, world, MovementType.WALKING, false, wide);
    }

    @Override
    protected boolean isAtValidPosition() {
        // ignore dismounting logic
        return true;
    }

    @Override
    public float getNodeReachProximity() {
        return wide ? this.ourEntity.getWidth() * 0.75F : super.getNodeReachProximity();
    }
}
