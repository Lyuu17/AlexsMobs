package com.github.alexthe666.alexsmobs.registry;

import com.github.alexthe666.alexsmobs.AlexsMobs;
import com.github.alexthe666.alexsmobs.effect.EffectEnderFlu;
import com.github.alexthe666.alexsmobs.effect.EffectMosquitoRepellent;
import com.github.alexthe666.alexsmobs.effect.EffectOiled;
import com.github.alexthe666.alexsmobs.effect.EffectPowerDown;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionUtil;
import net.minecraft.registry.RegistryKeys;

public class AMEffectRegistry {

    public static final DeferredRegister<StatusEffect> EFFECT_DEF_REG = DeferredRegister.create(AlexsMobs.MOD_ID, RegistryKeys.STATUS_EFFECT);
//    public static final DeferredRegister<Potion> POTION_DEF_REG = DeferredRegister.create(AlexsMobs.MOD_ID, Registries.POTION);
//
//    public static final RegistrySupplier<StatusEffect> KNOCKBACK_RESISTANCE = EFFECT_DEF_REG.register("knockback_resistance", EffectKnockbackResistance::new);
//    public static final RegistrySupplier<StatusEffect> LAVA_VISION = EFFECT_DEF_REG.register("lava_vision", EffectLavaVision::new);
//    public static final RegistrySupplier<StatusEffect> SUNBIRD_BLESSING = EFFECT_DEF_REG.register("sunbird_blessing", ()-> new EffectSunbird(false));
//    public static final RegistrySupplier<StatusEffect> SUNBIRD_CURSE = EFFECT_DEF_REG.register("sunbird_curse", ()-> new EffectSunbird(true));
//    public static final RegistrySupplier<StatusEffect> POISON_RESISTANCE = EFFECT_DEF_REG.register("poison_resistance", EffectPoisonResistance::new);
    public static final RegistrySupplier<StatusEffect> OILED = EFFECT_DEF_REG.register("oiled", EffectOiled::new);
//    public static final RegistrySupplier<StatusEffect> ORCAS_MIGHT = EFFECT_DEF_REG.register("orcas_might", EffectOrcaMight::new);
//    public static final RegistrySupplier<StatusEffect> BUG_PHEROMONES = EFFECT_DEF_REG.register("bug_pheromones", EffectBugPheromones::new);
//    public static final RegistrySupplier<StatusEffect> SOULSTEAL = EFFECT_DEF_REG.register("soulsteal", EffectSoulsteal::new);
//    public static final RegistrySupplier<StatusEffect> CLINGING = EFFECT_DEF_REG.register("clinging", EffectClinging::new);
    public static final RegistrySupplier<StatusEffect> ENDER_FLU = EFFECT_DEF_REG.register("ender_flu", EffectEnderFlu::new);
//    public static final RegistrySupplier<StatusEffect> FEAR = EFFECT_DEF_REG.register("fear", ()-> new EffectFear());
//    public static final RegistrySupplier<StatusEffect> TIGERS_BLESSING = EFFECT_DEF_REG.register("tigers_blessing", EffectTigersBlessing::new);
//    public static final RegistrySupplier<StatusEffect> DEBILITATING_STING = EFFECT_DEF_REG.register("debilitating_sting", EffectDebilitatingSting::new);
//    public static final RegistrySupplier<StatusEffect> EXSANGUINATION = EFFECT_DEF_REG.register("exsanguination", EffectExsanguination::new);
//    public static final RegistrySupplier<StatusEffect> EARTHQUAKE = EFFECT_DEF_REG.register("earthquake", EffectEarthquake::new);
//    public static final RegistrySupplier<StatusEffect> FLEET_FOOTED = EFFECT_DEF_REG.register("fleet_footed", EffectFleetFooted::new);
    public static final RegistrySupplier<StatusEffect> POWER_DOWN = EFFECT_DEF_REG.register("power_down", EffectPowerDown::new);

    public static final RegistrySupplier<StatusEffect> MOSQUITO_REPELLENT = EFFECT_DEF_REG.register("mosquito_repellent", EffectMosquitoRepellent::new);
//    public static final RegistrySupplier<Potion> KNOCKBACK_RESISTANCE_POTION = POTION_DEF_REG.register("knockback_resistance", ()-> new Potion(new StatusEffectInstance(KNOCKBACK_RESISTANCE.get(), 3600)));
//    public static final RegistrySupplier<Potion> LONG_KNOCKBACK_RESISTANCE_POTION = POTION_DEF_REG.register("long_knockback_resistance", ()-> new Potion(new StatusEffectInstance(KNOCKBACK_RESISTANCE.get(), 9600)));
//    public static final RegistrySupplier<Potion> STRONG_KNOCKBACK_RESISTANCE_POTION = POTION_DEF_REG.register("strong_knockback_resistance", ()-> new Potion(new StatusEffectInstance(KNOCKBACK_RESISTANCE.get(), 1800, 1)));
//    public static final RegistrySupplier<Potion> LAVA_VISION_POTION = POTION_DEF_REG.register("lava_vision", ()-> new Potion(new StatusEffectInstance(LAVA_VISION.get(), 3600)));
//    public static final RegistrySupplier<Potion> LONG_LAVA_VISION_POTION = POTION_DEF_REG.register("long_lava_vision", ()-> new Potion(new StatusEffectInstance(LAVA_VISION.get(), 9600)));
//    public static final RegistrySupplier<Potion> SPEED_III_POTION = POTION_DEF_REG.register("speed_iii", ()-> new Potion(new StatusEffectInstance(MobEffects.MOVEMENT_SPEED, 2200, 2)));
//    public static final RegistrySupplier<Potion> POISON_RESISTANCE_POTION = POTION_DEF_REG.register("poison_resistance", ()-> new Potion(new StatusEffectInstance(POISON_RESISTANCE.get(), 3600)));
//    public static final RegistrySupplier<Potion> LONG_POISON_RESISTANCE_POTION = POTION_DEF_REG.register("long_poison_resistance", ()-> new Potion(new StatusEffectInstance(POISON_RESISTANCE.get(), 9600)));
//    public static final RegistrySupplier<Potion> BUG_PHEROMONES_POTION = POTION_DEF_REG.register("bug_pheromones", ()-> new Potion(new StatusEffectInstance(BUG_PHEROMONES.get(), 3600)));
//    public static final RegistrySupplier<Potion> LONG_BUG_PHEROMONES_POTION = POTION_DEF_REG.register("long_bug_pheromones", ()-> new Potion(new StatusEffectInstance(BUG_PHEROMONES.get(), 9600)));
//    public static final RegistrySupplier<Potion> SOULSTEAL_POTION = POTION_DEF_REG.register("soulsteal", ()-> new Potion(new StatusEffectInstance(SOULSTEAL.get(), 3600)));
//    public static final RegistrySupplier<Potion> LONG_SOULSTEAL_POTION = POTION_DEF_REG.register("long_soulsteal", ()-> new Potion(new StatusEffectInstance(SOULSTEAL.get(), 9600)));
//    public static final RegistrySupplier<Potion> STRONG_SOULSTEAL_POTION = POTION_DEF_REG.register("strong_soulsteal", ()-> new Potion(new StatusEffectInstance(SOULSTEAL.get(), 1800, 1)));
//    public static final RegistrySupplier<Potion> CLINGING_POTION = POTION_DEF_REG.register("clinging", ()-> new Potion(new StatusEffectInstance(CLINGING.get(), 3600)));
//    public static final RegistrySupplier<Potion> LONG_CLINGING_POTION = POTION_DEF_REG.register("long_clinging", ()-> new Potion(new StatusEffectInstance(CLINGING.get(), 9600)));

    public static ItemStack createPotion(RegistrySupplier<Potion> potion){
        return PotionUtil.setPotion(new ItemStack(Items.POTION), potion.get());
    }

    public static ItemStack createPotion(Potion potion){
        return PotionUtil.setPotion(new ItemStack(Items.POTION), potion);
    }

    public static void init(){
//        BrewingRecipeRegistry.addRecipe(new ProperBrewingRecipe(Ingredient.of(createPotion(Potions.STRENGTH)), Ingredient.of(AMItemRegistry.BEAR_FUR.get()), createPotion(KNOCKBACK_RESISTANCE_POTION)));
//        BrewingRecipeRegistry.addRecipe(new ProperBrewingRecipe(Ingredient.of(createPotion(KNOCKBACK_RESISTANCE_POTION)), Ingredient.of(Items.REDSTONE), createPotion(LONG_KNOCKBACK_RESISTANCE_POTION)));
//        BrewingRecipeRegistry.addRecipe(new ProperBrewingRecipe(Ingredient.of(createPotion(KNOCKBACK_RESISTANCE_POTION)), Ingredient.of(Items.GLOWSTONE_DUST), createPotion(STRONG_KNOCKBACK_RESISTANCE_POTION)));
//        BrewingRecipeRegistry.addRecipe(new ProperBrewingRecipe(Ingredient.of(AMItemRegistry.LAVA_BOTTLE.get()), Ingredient.of(AMItemRegistry.BONE_SERPENT_TOOTH.get()), createPotion(LAVA_VISION_POTION)));
//        BrewingRecipeRegistry.addRecipe(new ProperBrewingRecipe(Ingredient.of(createPotion(LAVA_VISION_POTION)), Ingredient.of(Items.REDSTONE), createPotion(LONG_LAVA_VISION_POTION)));
//        BrewingRecipeRegistry.addRecipe(new ProperBrewingRecipe(Ingredient.of(createPotion(Potions.POISON)), Ingredient.of(AMItemRegistry.RATTLESNAKE_RATTLE.get()), new ItemStack(AMItemRegistry.POISON_BOTTLE.get())));
//        BrewingRecipeRegistry.addRecipe(new ProperBrewingRecipe(Ingredient.of(AMItemRegistry.POISON_BOTTLE.get()), Ingredient.of(AMItemRegistry.CENTIPEDE_LEG.get()), createPotion(POISON_RESISTANCE_POTION)));
//        BrewingRecipeRegistry.addRecipe(new ProperBrewingRecipe(Ingredient.of(AMItemRegistry.KOMODO_SPIT_BOTTLE.get()), Ingredient.of(AMItemRegistry.CENTIPEDE_LEG.get()), createPotion(POISON_RESISTANCE_POTION)));
//        BrewingRecipeRegistry.addRecipe(new ProperBrewingRecipe(Ingredient.of(createPotion(POISON_RESISTANCE_POTION)), Ingredient.of(AMItemRegistry.KOMODO_SPIT.get()), createPotion(LONG_POISON_RESISTANCE_POTION)));
//        BrewingRecipeRegistry.addRecipe(new ProperBrewingRecipe(Ingredient.of(createPotion(Potions.STRONG_SWIFTNESS)), Ingredient.of(AMItemRegistry.GAZELLE_HORN.get()), createPotion(SPEED_III_POTION)));
//        BrewingRecipeRegistry.addRecipe(new ProperBrewingRecipe(Ingredient.of(createPotion(Potions.AWKWARD)), Ingredient.of(AMItemRegistry.COCKROACH_WING.get()), createPotion(BUG_PHEROMONES_POTION)));
//        BrewingRecipeRegistry.addRecipe(new ProperBrewingRecipe(Ingredient.of(createPotion(BUG_PHEROMONES_POTION)), Ingredient.of(Items.REDSTONE), createPotion(LONG_BUG_PHEROMONES_POTION)));
//        BrewingRecipeRegistry.addRecipe(new ProperBrewingRecipe(Ingredient.of(createPotion(Potions.AWKWARD)), Ingredient.of(AMItemRegistry.SOUL_HEART.get()), createPotion(SOULSTEAL_POTION)));
//        BrewingRecipeRegistry.addRecipe(new ProperBrewingRecipe(Ingredient.of(createPotion(SOULSTEAL_POTION)), Ingredient.of(Items.REDSTONE), createPotion(LONG_SOULSTEAL_POTION)));
//        BrewingRecipeRegistry.addRecipe(new ProperBrewingRecipe(Ingredient.of(createPotion(SOULSTEAL_POTION)), Ingredient.of(Items.GLOWSTONE_DUST), createPotion(STRONG_SOULSTEAL_POTION)));
//        BrewingRecipeRegistry.addRecipe(new ProperBrewingRecipe(Ingredient.of(createPotion(Potions.AWKWARD)), Ingredient.of(AMItemRegistry.DROPBEAR_CLAW.get()), createPotion(CLINGING_POTION)));
//        BrewingRecipeRegistry.addRecipe(new ProperBrewingRecipe(Ingredient.of(createPotion(CLINGING_POTION)), Ingredient.of(Items.REDSTONE), createPotion(LONG_CLINGING_POTION)));
//
//
    }
}
