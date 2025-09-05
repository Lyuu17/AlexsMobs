package com.github.alexthe666.alexsmobs.entity.ai;

import com.github.alexthe666.alexsmobs.AlexsMobs;
import com.github.alexthe666.alexsmobs.entity.EntityGrizzlyBear;
import com.github.alexthe666.alexsmobs.misc.AMDamageTypes;
import com.github.alexthe666.alexsmobs.packet.SendVisualFlagFromServerPacket;
import com.github.alexthe666.alexsmobs.registry.AMEffectRegistry;
import com.github.alexthe666.alexsmobs.registry.AMSoundRegistry;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.event.GameEvent;

import java.util.EnumSet;

public class GrizzlyBearAIAprilFools extends Goal {

    private final EntityGrizzlyBear bear;
    private PlayerEntity target;
    private int runDelay = 0;
    private final double maxDistance = 13;
    private int powerOutTimer = 0;
    private int musicBoxTimer = 0;
    private int maxMusicBoxTime = 0;
    private int leapTimer = 0;

    public GrizzlyBearAIAprilFools(EntityGrizzlyBear bear){
        this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
        this.bear = bear;
    }

    @Override
    public boolean canStart() {
        if(!bear.isBaby() && AlexsMobs.isAprilFools() && runDelay-- <= 0 && bear.getRandom().nextInt(30) == 0){
            runDelay = 400 + bear.getRandom().nextInt(350);
            var nearestPlayer = bear.getWorld().getClosestPlayer(bear.getX(), bear.getY(), bear.getZ(), maxDistance, entity -> {
                return bear.canSee(entity) &&(!(entity instanceof PlayerEntity) || !((PlayerEntity) entity).hasStatusEffect(AMEffectRegistry.POWER_DOWN.get()));
            });
            if(nearestPlayer != null){
                target = nearestPlayer;
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean shouldContinue() {
        return target != null && bear.distanceTo(target) < maxDistance * 2;
    }

    @Override
    public void start(){
        maxMusicBoxTime = 100 + bear.getRandom().nextInt(130);
    }

    @Override
    public void tick() {
        super.tick();
        double dist = bear.distanceTo(target);
        bear.getLookControl().lookAt(this.target.getX(), this.target.getEyeY(), this.target.getZ());
        if(dist <= 6 && bear.canSee(target)){
            bear.getNavigation().stop();
            if(bear.getAprilFoolsFlag() == 5){
                leapTimer++;
                if(leapTimer == 7){
                    AlexsMobs.sendMSGToAll(new SendVisualFlagFromServerPacket(target.getId(), 87));
                }
                if(leapTimer >= 10){
                    bear.setAprilFoolsFlag(0);
                    if(bear.getWorld().getLevelProperties().isHardcore()){
                        target.damage(AMDamageTypes.causeFreddyBearDamage(bear), target.getMaxHealth() - 1);
                        target.setHealth(1);
                    }else{
                        target.damage(AMDamageTypes.causeFreddyBearDamage(bear), target.getMaxHealth() + 1000F);
                    }
                    stop();
                    return;
                }
            }else if(bear.getAprilFoolsFlag() < 4) {
                if(powerOutTimer == 0){
                    target.addStatusEffect(new StatusEffectInstance(AMEffectRegistry.POWER_DOWN.get(), 2 * (maxMusicBoxTime + 100), 0, false, false, true));
                }
                powerOutTimer++;
                if (powerOutTimer >= 60) {
                    bear.setAprilFoolsFlag(4);
                    powerOutTimer = 0;
                }else{
                    bear.setAprilFoolsFlag(3);
                }
            }else{
                if(musicBoxTimer == 0){
                    bear.getWorld().sendEntityStatus(bear, (byte) 67);
                }
                musicBoxTimer++;
                if (musicBoxTimer >= maxMusicBoxTime) {
                    if(bear.getAprilFoolsFlag() != 5){
                        bear.getWorld().sendEntityStatus(bear, (byte) 68);
                        bear.setAprilFoolsFlag(5);
                        bear.emitGameEvent(GameEvent.ENTITY_ROAR);
                        bear.playSound(AMSoundRegistry.APRIL_FOOLS_SCREAM.get(), 3, 1);
                        musicBoxTimer = 0;
                    }
                }
            }
            if(bear.getAprilFoolsFlag() < 2){
                bear.setAprilFoolsFlag(2);
            }
        }else{
            bear.getNavigation().startMovingTo(target, 1.2F);
            if(bear.getAprilFoolsFlag() < 1){
                bear.setAprilFoolsFlag(1);
            }
        }
    }

    @Override
    public void stop(){
        target = null;
        runDelay = 100 + bear.getRandom().nextInt(100);
        bear.setAprilFoolsFlag(0);
        powerOutTimer = 0;
        musicBoxTimer = 0;
        leapTimer = 0;
    }
}
