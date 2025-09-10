package com.github.alexthe666.alexsmobs.entity.ai;

import com.github.alexthe666.alexsmobs.AlexsMobs;
import net.minecraft.entity.ai.NoPenaltyTargeting;
import net.minecraft.entity.ai.goal.WanderAroundGoal;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructurePiece;
import net.minecraft.structure.StructureStart;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.gen.structure.Structure;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class MonsterAIWalkThroughHallsOfStructure extends WanderAroundGoal {

    private TagKey<Structure> structureTagKey;
    private double maximumDistance = 0;
    private double maximumYDistance = 3;

    private int errorCooldown = 0;

    public MonsterAIWalkThroughHallsOfStructure(PathAwareEntity mob, double speed, int chance, TagKey<Structure> structureTagKey, double maximumDistance) {
        super(mob, speed, chance, false);
        this.structureTagKey = structureTagKey;
        this.maximumDistance = 32;
    }

    @Override
    public boolean canStart() {
        if(errorCooldown > 0){
            errorCooldown--;
        }
        return super.canStart();
    }

    @Override
    public void tick(){
        super.tick();
        if(errorCooldown > 0){
            errorCooldown--;
        }
    }

    @Nullable
    @Override
    protected Vec3d getWanderTarget() {
        var start = getNearestStructure(mob.getBlockPos());
        if(start != null && start.hasChildren() && errorCooldown <= 0){
            List<StructurePiece> pieces = start.getChildren();
            List<BlockPos> validPieceCenters = new ArrayList<>();
            for (StructurePiece piece : pieces) {
                var boundingbox = piece.getBoundingBox();
                BlockPos blockpos = boundingbox.getCenter();
                BlockPos blockpos1 = new BlockPos(blockpos.getX(), boundingbox.getMinY(), blockpos.getZ());
                double yDist = Math.abs(blockpos1.getY() - mob.getBlockPos().getY());
                if (this.mob.squaredDistanceTo(Vec3d.ofCenter(blockpos1)) <= this.maximumDistance * this.maximumDistance && yDist < maximumYDistance) {
                    validPieceCenters.add(blockpos1);
                }
            }
            if (!validPieceCenters.isEmpty()) {
                BlockPos randomCenter = validPieceCenters.size() > 1 ? validPieceCenters.get(mob.getRandom().nextInt(validPieceCenters.size() - 1)) : validPieceCenters.get(0);
                return Vec3d.ofCenter(randomCenter.add(mob.getRandom().nextInt(2) - 1, 0, mob.getRandom().nextInt(2) - 1));

            }
        }
        return getPositionTowardsAnywhere();
    }


    @Nullable
    private Vec3d getPositionTowardsAnywhere() {
        return NoPenaltyTargeting.find(this.mob, 10, 7);
    }

    @Nullable
    private StructureStart getNearestStructure(BlockPos pos){
        var serverWorld = (ServerWorld)this.mob.getWorld();
        try{
            var start = serverWorld.getStructureAccessor().getStructureContaining(pos, structureTagKey);
            if(start.hasChildren()){
                return start;
            }else{
                var nearestOf = serverWorld.locateStructure(structureTagKey, pos, (int)(this.maximumDistance / 16), false);
                if(nearestOf == null || nearestOf.getSquaredDistance(this.mob.getX(), this.mob.getY(), this.mob.getZ()) > 256 || !serverWorld.canSetBlock(nearestOf)){
                    return null;
                }
                return serverWorld.getStructureAccessor().getStructureContaining(nearestOf, structureTagKey);
            }
        }catch (Exception e){
            AlexsMobs.LOGGER.warn("{} encountered an issue searching for a nearby structure.", this.mob);
            errorCooldown = 2000 + this.mob.getRandom().nextInt(2000);
            return null;
        }
    }
}
