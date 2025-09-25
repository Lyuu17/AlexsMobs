package com.github.alexthe666.alexsmobs.client.sound;

import com.github.alexthe666.alexsmobs.entity.EntityCockroach;
import com.github.alexthe666.alexsmobs.registry.AMSoundRegistry;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.sound.MovingSoundInstance;
import net.minecraft.sound.SoundCategory;

public class SoundLaCucaracha extends MovingSoundInstance {

    public static final Int2ObjectMap<SoundLaCucaracha> COCKROACH_SOUND_MAP = new Int2ObjectOpenHashMap<>();

    private final EntityCockroach cockroach;

    public SoundLaCucaracha(EntityCockroach cockroach) {
        super(AMSoundRegistry.LA_CUCARACHA.get(), SoundCategory.RECORDS, cockroach.getRandom());
        this.cockroach = cockroach;
        this.attenuationType = AttenuationType.LINEAR;
        this.repeat = true;
        this.repeatDelay = 0;
        this.x = this.cockroach.getX();
        this.y = this.cockroach.getY();
        this.z = this.cockroach.getZ();
    }

    @Override
    public boolean canPlay() {
        return !this.cockroach.isSilent() && this.cockroach.hasMaracas() && this.cockroach.isDancing() && COCKROACH_SOUND_MAP.get(this.cockroach.getId()) == this;
    }

    public boolean isOnlyCockroach() {
        for(var cucaracha : COCKROACH_SOUND_MAP.values()){
            if(cucaracha != this && distanceSq(cucaracha.x, cucaracha.y, cucaracha.z) < 16 && cucaracha.canPlay()){
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
        if (!this.cockroach.isRemoved() && this.cockroach.isAlive() && this.cockroach.isDancing() && this.cockroach.hasMaracas()) {
            this.volume = 1;
            this.pitch = 1;
            this.x = this.cockroach.getX();
            this.y = this.cockroach.getY();
            this.z = this.cockroach.getZ();
        } else {
            this.setDone();
            COCKROACH_SOUND_MAP.remove(cockroach.getId());
        }
    }

}
