package com.github.alexthe666.alexsmobs.entity.ai;

import com.github.alexthe666.alexsmobs.entity.EntityBlueJay;
import com.github.alexthe666.alexsmobs.entity.util.Maths;
import com.github.alexthe666.alexsmobs.misc.AMBlockPos;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class BlueJayAIMelee extends Goal {
    private final EntityBlueJay blueJay;
    float circlingTime = 0;
    float circleDistance = 1;
    float yLevel = 2;
    boolean clockwise = false;
    private int maxCircleTime;

    public BlueJayAIMelee(EntityBlueJay blueJay) {
        this.blueJay = blueJay;
    }

    @Override
    public boolean canStart(){
        var entity = blueJay.getTarget();
        return entity != null && entity.isAlive();
    }

    @Override
    public void start() {
        clockwise = blueJay.getRandom().nextBoolean();
        yLevel = blueJay.getRandom().nextInt(2);
        circlingTime = 0;
        maxCircleTime = 20 + blueJay.getRandom().nextInt(20);
        circleDistance = 0.5F + blueJay.getRandom().nextFloat() * 2F;
    }

    @Override
    public void stop() {
        clockwise = blueJay.getRandom().nextBoolean();
        yLevel = blueJay.getRandom().nextInt(2);
        circlingTime = 0;
        maxCircleTime = 20 + blueJay.getRandom().nextInt(20);
        circleDistance = 0.5F + blueJay.getRandom().nextFloat() * 2F;
        if(blueJay.isOnGround()){
            blueJay.setFlying(false);
        }
    }

    @Override
    public void tick() {
        if (this.blueJay.isFlying()) {
            circlingTime++;
        }
        var target = blueJay.getTarget();
        if(target != null){
            if(blueJay.distanceTo(target) < 3){
                blueJay.peck();
                target.damage(target.getDamageSources().generic(), 1);
                stop();
            }
            if(circlingTime > maxCircleTime){
                blueJay.getMoveControl().moveTo(target.getX(), target.getY() + target.getStandingEyeHeight() / 2F, target.getZ(), 1.6F);

            }else{
                var circlePos = getVultureCirclePos(target.getPos());
                if (circlePos == null) {
                    circlePos = target.getPos();
                }
                blueJay.setFlying(true);
                blueJay.getMoveControl().moveTo(circlePos.x, circlePos.y + target.getStandingEyeHeight() + 0.2F, circlePos.z, 1.6F);

            }
        }
    }

    public Vec3d getVultureCirclePos(Vec3d target) {
        float angle = (Maths.STARTING_ANGLE * 13 * (clockwise ? -circlingTime : circlingTime));
        double extraX = circleDistance * MathHelper.sin((angle));
        double extraZ = circleDistance * MathHelper.cos(angle);
        var pos = new Vec3d(target.x + extraX, target.y + yLevel, target.z + extraZ);
        if (blueJay.getWorld().isAir(AMBlockPos.fromVec3(pos))) {
            return pos;
        }
        return null;
    }
}
