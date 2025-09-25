package com.github.alexthe666.alexsmobs.client.sound;

import com.github.alexthe666.alexsmobs.entity.EntityGrizzlyBear;
import com.github.alexthe666.alexsmobs.registry.AMSoundRegistry;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.sound.MovingSoundInstance;
import net.minecraft.sound.SoundCategory;

// NOT USED
public class SoundBearMusicBox extends MovingSoundInstance {

    public static final Int2ObjectMap<SoundBearMusicBox> BEAR_MUSIC_BOX_SOUND_MAP = new Int2ObjectOpenHashMap<>();

    private final EntityGrizzlyBear bear;

    public SoundBearMusicBox(EntityGrizzlyBear bear) {
        super(AMSoundRegistry.APRIL_FOOLS_MUSIC_BOX.get(), SoundCategory.RECORDS, bear.getRandom());
        this.bear = bear;
        this.attenuationType = AttenuationType.LINEAR;
        this.repeat = true;
        this.repeatDelay = 0;
        this.x = this.bear.getX();
        this.y = this.bear.getY();
        this.z = this.bear.getZ();
    }

    @Override
    public boolean canPlay() {
        return this.bear.getAprilFoolsFlag() == 4 && BEAR_MUSIC_BOX_SOUND_MAP.get(this.bear.getId()) == this;
    }

    public boolean isOnlyMusicBox() {
        for(var s : BEAR_MUSIC_BOX_SOUND_MAP.values()){
            if(s != this && distanceSq(s.x, s.y, s.z) < 16 && s.canPlay()){
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
        if (!this.bear.isRemoved() && this.bear.isAlive() && this.bear.getAprilFoolsFlag() == 4) {
            this.volume = 3;
            this.pitch = 1;
            this.x = this.bear.getX();
            this.y = this.bear.getY();
            this.z = this.bear.getZ();
        } else {
            this.setDone();
            BEAR_MUSIC_BOX_SOUND_MAP.remove(bear.getId());
        }
    }

}
