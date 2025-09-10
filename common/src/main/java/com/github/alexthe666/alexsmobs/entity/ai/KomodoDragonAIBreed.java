package com.github.alexthe666.alexsmobs.entity.ai;

import com.github.alexthe666.alexsmobs.entity.EntityKomodoDragon;
import net.minecraft.entity.ai.goal.AnimalMateGoal;
import net.minecraft.server.world.ServerWorld;

public class KomodoDragonAIBreed extends AnimalMateGoal {
    boolean withPartner;
    private final EntityKomodoDragon komodo;
    int selfBreedTime = 0;

    public KomodoDragonAIBreed(EntityKomodoDragon entityKomodoDragon, double v) {
        super(entityKomodoDragon, v);
        this.komodo = entityKomodoDragon;
    }

    @Override
    public boolean canStart(){
        boolean prev = super.canStart();
        withPartner = prev;
        return withPartner || animal.isInLove();
    }

    @Override
    public boolean shouldContinue() {
        return withPartner ? super.shouldContinue() : selfBreedTime < 60;
    }

    @Override
    public void stop() {
        super.stop();
        selfBreedTime = 0;
    }

    @Override
    public void tick() {
        if(withPartner){
            super.tick();
        }else{
            this.animal.getNavigation().stop();
            ++this.selfBreedTime;
            if (this.selfBreedTime >= 60) {
                this.spawnParthogenicBaby();
            }
        }
    }

    @Override
    protected void breed() {
        for(int i = 0; i < 2 + this.animal.getRandom().nextInt(2); i++){
            this.animal.breed((ServerWorld)this.world, this.mate);
        }
        komodo.slaughterCooldown = 200;
    }

    private void spawnParthogenicBaby() {
        for(int i = 0; i < 2 + this.animal.getRandom().nextInt(2); i++){
            this.animal.breed((ServerWorld)this.world, this.animal);
        }
        komodo.slaughterCooldown = 200;
    }
}
