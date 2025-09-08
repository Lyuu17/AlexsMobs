package com.github.alexthe666.alexsmobs.entity.ai;

import com.github.alexthe666.alexsmobs.entity.EntityCrow;
import com.github.alexthe666.alexsmobs.entity.util.Maths;
import com.github.alexthe666.alexsmobs.misc.AMBlockPos;
import net.minecraft.entity.EntityGroup;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class CrowAIMelee extends Goal {
    private final EntityCrow crow;
    float circlingTime = 0;
    float circleDistance = 1;
    float yLevel = 2;
    boolean clockwise = false;
    private int maxCircleTime;

    public CrowAIMelee(EntityCrow crow) {
        this.crow = crow;
    }

    @Override
    public boolean canStart(){
        return crow.getTarget() != null && !crow.isSitting() && crow.getCommand() != 3;
    }

    @Override
    public void start() {
        clockwise = crow.getRandom().nextBoolean();
        yLevel = crow.getRandom().nextInt(2);
        circlingTime = 0;
        maxCircleTime = 20 + crow.getRandom().nextInt(100);
        circleDistance = 1F + crow.getRandom().nextFloat() * 3F;
    }

    @Override
    public void stop() {
        clockwise = crow.getRandom().nextBoolean();
        yLevel = crow.getRandom().nextInt(2);
        circlingTime = 0;
        maxCircleTime = 20 + crow.getRandom().nextInt(100);
        circleDistance = 1F + crow.getRandom().nextFloat() * 3F;
        if(crow.isOnGround()){
            crow.setFlying(false);
        }
    }

    @Override
    public void tick() {
        var target = crow.getTarget();
        if(target != null){
            if(circlingTime > maxCircleTime){
                crow.getMoveControl().moveTo(target.getX(), target.getY() + target.getStandingEyeHeight() / 2F, target.getZ(), 1.3F);
                if(crow.distanceTo(target) < 2){
                    crow.peck();
                    if(target.getGroup() == EntityGroup.UNDEAD){
                        target.damage(target.getDamageSources().generic(), 4);
                    }else{
                        target.damage(target.getDamageSources().generic(), 1);
                    }

                    stop();
                }
            }else{
                var circlePos = getVultureCirclePos(target.getPos());
                if (circlePos == null) {
                    circlePos = target.getPos();
                }
                crow.setFlying(true);
                crow.getMoveControl().moveTo(circlePos.x, circlePos.y + target.getStandingEyeHeight() + 0.2F, circlePos.z, 1F);
            }
        }
        if (this.crow.isFlying()) {
            circlingTime++;
        }
    }

    public Vec3d getVultureCirclePos(Vec3d target) {
        float angle = (Maths.EIGHT_STARTING_ANGLE * (clockwise ? -circlingTime : circlingTime));
        double extraX = circleDistance * MathHelper.sin((angle));
        double extraZ = circleDistance * MathHelper.cos(angle);
        var pos = new Vec3d(target.x + extraX, target.y + yLevel, target.z + extraZ);
        if (crow.getWorld().isAir(AMBlockPos.fromVec3(pos))) {
            return pos;
        }
        return null;
    }
}
