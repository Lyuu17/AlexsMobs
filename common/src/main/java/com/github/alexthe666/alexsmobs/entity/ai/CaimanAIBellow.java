package com.github.alexthe666.alexsmobs.entity.ai;

import com.github.alexthe666.alexsmobs.entity.EntityCaiman;
import com.github.alexthe666.alexsmobs.registry.AMSoundRegistry;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.math.Vec3d;

import java.util.EnumSet;

public class CaimanAIBellow extends Goal {

    private final EntityCaiman caiman;
    private int bellowTime = 0;

    public CaimanAIBellow(EntityCaiman caiman) {
        this.caiman = caiman;
        this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
    }

    @Override
    public boolean canStart() {
        return caiman.getTarget() == null && caiman.bellowCooldown <= 0 && caiman.isInsideWaterOrBubbleColumn() && !caiman.shouldFollow();
    }

    @Override
    public boolean shouldContinue() {
        return super.shouldContinue() && bellowTime < 60;
    }

    @Override
    public void stop() {
        bellowTime = 0;
        caiman.bellowCooldown = 1000 + caiman.getRandom().nextInt(1000);
        caiman.setBellowing(false);
    }

    @Override
    public void tick(){
        if(caiman.isInsideWaterOrBubbleColumn()){
            final double d1 = caiman.getFluidHeight(FluidTags.WATER);
            caiman.getNavigation().stop();
            if(d1 > 0.3F){
                final double d2 = Math.pow(d1 - 0.3F, 2);
                caiman.setVelocity(new Vec3d(caiman.getVelocity().x, Math.min(d2 * 0.08F, 0.04F), caiman.getVelocity().z));
            }else{
                caiman.setVelocity(new Vec3d(caiman.getVelocity().x, -0.02F, caiman.getVelocity().z));
            }
            if(d1 > 0.19F && d1 < 0.5F){
                bellowTime++;
                caiman.playSound(AMSoundRegistry.CAIMAN_SPLASH.get(), 1, caiman.getSoundPitch());
                caiman.setBellowing(true);
            }
        }
    }
}
