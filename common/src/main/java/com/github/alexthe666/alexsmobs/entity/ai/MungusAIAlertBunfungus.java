package com.github.alexthe666.alexsmobs.entity.ai;

import com.github.alexthe666.alexsmobs.entity.EntityBunfungus;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.ai.goal.TrackTargetGoal;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.util.math.Box;
import net.minecraft.world.GameRules;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public class MungusAIAlertBunfungus extends TrackTargetGoal {
    private static final TargetPredicate HURT_BY_TARGETING = TargetPredicate.createNonAttackable().ignoreVisibility().ignoreDistanceScalingFactor();
    private static final int ALERT_RANGE_Y = 10;
    private boolean alertSameType;
    private int timestamp;
    private final Class<?>[] toIgnoreDamage;
    @Nullable
    private Class<?>[] toIgnoreAlert;

    public MungusAIAlertBunfungus(PathAwareEntity p_26039_, Class<?>... p_26040_) {
        super(p_26039_, true);
        this.toIgnoreDamage = p_26040_;
        this.setControls(EnumSet.of(Control.TARGET));
    }

    @Override
    public boolean canStart() {
        int i = this.mob.getLastAttackedTime();
        var livingentity = this.mob.getAttacker();
        if (i != this.timestamp && livingentity != null) {
            if (livingentity.getType() == EntityType.PLAYER && this.mob.getWorld().getGameRules().getBoolean(GameRules.UNIVERSAL_ANGER)) {
                return false;
            } else {
                for(Class<?> oclass : this.toIgnoreDamage) {
                    if (oclass.isAssignableFrom(livingentity.getClass())) {
                        return false;
                    }
                }

                return this.canTrack(livingentity, HURT_BY_TARGETING);
            }
        } else {
            return false;
        }
    }

    @Override
    public void start() {
        this.mob.setTarget(this.mob.getAttacker());
        this.target = this.mob.getTarget();
        this.timestamp = this.mob.getLastAttackedTime();
        this.maxTimeWithoutVisibility = 300;
        this.alertOthers();
        super.start();
    }

    protected void alertOthers() {
        double d0 = this.getFollowRange();
        var aabb = Box.from(this.mob.getPos()).expand(d0, 10.0D, d0);
        var list = this.mob.getWorld().getEntitiesByClass(EntityBunfungus.class, aabb, EntityPredicates.EXCEPT_SPECTATOR);
        var iterator = list.iterator();

        while(true) {
            EntityBunfungus mob;
            while(true) {
                if (!iterator.hasNext()) {
                    return;
                }

                mob = iterator.next();
                if (this.mob != mob && mob.getTarget() == null && !mob.isTeammate(this.mob.getAttacker()) && mob.defendsMungusAgainst(this.mob.getAttacker())) {
                    if (this.toIgnoreAlert == null) {
                        break;
                    }

                    boolean flag = false;

                    for(Class<?> oclass : this.toIgnoreAlert) {
                        if (mob.getClass() == oclass) {
                            flag = true;
                            break;
                        }
                    }

                    if (!flag) {
                        break;
                    }
                }
            }

            this.alertOther(mob, this.mob.getAttacker());
        }
    }

    protected void alertOther(MobEntity p_26042_, LivingEntity p_26043_) {
        p_26042_.setTarget(p_26043_);
    }
}
