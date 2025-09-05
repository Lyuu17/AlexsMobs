package com.github.alexthe666.alexsmobs.misc;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

public class AMDamageTypes {

    public static final RegistryKey<DamageType> FARSEER = RegistryKey.of(RegistryKeys.DAMAGE_TYPE, new Identifier("alexsmobs:farseer"));
    public static final RegistryKey<DamageType> FREDDY = RegistryKey.of(RegistryKeys.DAMAGE_TYPE, new Identifier("alexsmobs:freddy"));

    public static DamageSource causeFarseerDamage(LivingEntity attacker){
        return new DamageSourceRandomMessages(attacker.getWorld().getRegistryManager().getOptional(RegistryKeys.DAMAGE_TYPE).get().entryOf(FARSEER), attacker);
    }

    public static DamageSource causeFreddyBearDamage(LivingEntity attacker){
        return new DamageSource(attacker.getWorld().getRegistryManager().getOptional(RegistryKeys.DAMAGE_TYPE).get().entryOf(FREDDY), attacker);
    }

    private static class DamageSourceRandomMessages extends DamageSource {

        public DamageSourceRandomMessages(RegistryEntry<DamageType> damageTypeHolder, @Nullable Entity entity1, @Nullable Entity entity2, @Nullable Vec3d from) {
            super(damageTypeHolder, entity1, entity2, from);
        }

        public DamageSourceRandomMessages(RegistryEntry<DamageType> damageTypeHolder, @Nullable Entity entity1, @Nullable Entity entity2) {
            super(damageTypeHolder, entity1, entity2);
        }

        public DamageSourceRandomMessages(RegistryEntry<DamageType> damageTypeHolder, Vec3d from) {
            super(damageTypeHolder, from);
        }

        public DamageSourceRandomMessages(RegistryEntry<DamageType> damageTypeHolder, @Nullable Entity entity) {
            super(damageTypeHolder, entity);
        }

        public DamageSourceRandomMessages(RegistryEntry<DamageType> p_270475_) {
            super(p_270475_);
        }

        @Override
        public Text getDeathMessage(LivingEntity attacked) {
            int type = attacked.getRandom().nextInt(3);
            var livingentity = attacked.getPrimeAdversary();
            String s = "death.attack." + this.getName() + "_" + type;
            String s1 = s + ".player";
            return livingentity != null ? Text.translatable(s1, attacked.getDisplayName(), livingentity.getDisplayName()) : Text.translatable(s, attacked.getDisplayName());
        }
    }
}
