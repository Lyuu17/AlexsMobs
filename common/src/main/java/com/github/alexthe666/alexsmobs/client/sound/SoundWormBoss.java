package com.github.alexthe666.alexsmobs.client.sound;

import com.github.alexthe666.alexsmobs.entity.EntityVoidWorm;
import com.github.alexthe666.alexsmobs.registry.AMSoundRegistry;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.MovingSoundInstance;
import net.minecraft.sound.SoundCategory;

public class SoundWormBoss extends MovingSoundInstance {

    public static final Int2ObjectMap<SoundWormBoss> WORMBOSS_SOUND_MAP = new Int2ObjectOpenHashMap<>();

    private final EntityVoidWorm voidWorm;
    private int ticksExisted = 0;

    public SoundWormBoss(EntityVoidWorm worm) {
        super(AMSoundRegistry.MUSIC_WORMBOSS.get(), SoundCategory.RECORDS, worm.getRandom());
        this.voidWorm = worm;
        this.attenuationType = AttenuationType.NONE;
        this.repeat = true;
        this.repeatDelay = 0;
        this.x = this.voidWorm.getX();
        this.y = this.voidWorm.getY();
        this.z = this.voidWorm.getZ();
    }

    @Override
    public boolean canPlay() {
        return !this.voidWorm.isSilent() && WORMBOSS_SOUND_MAP.get(this.voidWorm.getId()) == this;
    }

    public boolean isNearest() {
        float dist = 400;
        for(var wormBoss : WORMBOSS_SOUND_MAP.values()){
            if(wormBoss != this && distanceSq(wormBoss.x, wormBoss.y, wormBoss.z) < dist * dist && wormBoss.canPlay()){
                return false;
            }
        }
        return true;
    }

    public double distanceSq(double p_218140_1_, double p_218140_3_, double p_218140_5_) {
        double lvt_10_1_ = this.getX() - p_218140_1_;
        double lvt_12_1_ = this.getY() - p_218140_3_;
        double lvt_14_1_ = this.getZ() - p_218140_5_;
        return lvt_10_1_ * lvt_10_1_ + lvt_12_1_ * lvt_12_1_ + lvt_14_1_ * lvt_14_1_;
    }

    @Override
    public void tick() {
        if(ticksExisted % 100 == 0){
            MinecraftClient.getInstance().getMusicTracker().stop();

        }
        if (!this.voidWorm.isRemoved() && this.voidWorm.isAlive()) {
            this.volume = 1;
            this.pitch = 1;
            this.x = this.voidWorm.getX();
            this.y = this.voidWorm.getY();
            this.z = this.voidWorm.getZ();
        } else {
            this.setDone();
            WORMBOSS_SOUND_MAP.remove(voidWorm.getId());
        }
        ticksExisted++;
    }

}
