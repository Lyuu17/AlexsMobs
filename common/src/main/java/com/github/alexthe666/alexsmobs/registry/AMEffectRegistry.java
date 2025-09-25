package com.github.alexthe666.alexsmobs.registry;

import com.github.alexthe666.alexsmobs.AlexsMobs;
import com.github.alexthe666.alexsmobs.entity.effect.*;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionUtil;
import net.minecraft.potion.Potions;
import net.minecraft.recipe.BrewingRecipeRegistry;
import net.minecraft.registry.RegistryKeys;

public class AMEffectRegistry {

    public static final DeferredRegister<StatusEffect> EFFECT_DEF_REG = DeferredRegister.create(AlexsMobs.MOD_ID, RegistryKeys.STATUS_EFFECT);
    public static final DeferredRegister<Potion> POTION_DEF_REG = DeferredRegister.create(AlexsMobs.MOD_ID, RegistryKeys.POTION);
//
    public static final RegistrySupplier<StatusEffect> KNOCKBACK_RESISTANCE = EFFECT_DEF_REG.register("knockback_resistance", EffectKnockbackResistance::new);
    public static final RegistrySupplier<StatusEffect> LAVA_VISION = EFFECT_DEF_REG.register("lava_vision", EffectLavaVision::new);
    public static final RegistrySupplier<StatusEffect> SUNBIRD_BLESSING = EFFECT_DEF_REG.register("sunbird_blessing", ()-> new EffectSunbird(false));
    public static final RegistrySupplier<StatusEffect> SUNBIRD_CURSE = EFFECT_DEF_REG.register("sunbird_curse", ()-> new EffectSunbird(true));
    public static final RegistrySupplier<StatusEffect> POISON_RESISTANCE = EFFECT_DEF_REG.register("poison_resistance", EffectPoisonResistance::new);
    public static final RegistrySupplier<StatusEffect> OILED = EFFECT_DEF_REG.register("oiled", EffectOiled::new);
    public static final RegistrySupplier<StatusEffect> ORCAS_MIGHT = EFFECT_DEF_REG.register("orcas_might", EffectOrcaMight::new);
    public static final RegistrySupplier<StatusEffect> BUG_PHEROMONES = EFFECT_DEF_REG.register("bug_pheromones", EffectBugPheromones::new);
    public static final RegistrySupplier<StatusEffect> SOULSTEAL = EFFECT_DEF_REG.register("soulsteal", EffectSoulsteal::new);
    public static final RegistrySupplier<StatusEffect> CLINGING = EFFECT_DEF_REG.register("clinging", EffectClinging::new);
    public static final RegistrySupplier<StatusEffect> ENDER_FLU = EFFECT_DEF_REG.register("ender_flu", EffectEnderFlu::new);
    public static final RegistrySupplier<StatusEffect> FEAR = EFFECT_DEF_REG.register("fear", EffectFear::new);
    public static final RegistrySupplier<StatusEffect> TIGERS_BLESSING = EFFECT_DEF_REG.register("tigers_blessing", EffectTigersBlessing::new);
    public static final RegistrySupplier<StatusEffect> DEBILITATING_STING = EFFECT_DEF_REG.register("debilitating_sting", EffectDebilitatingSting::new);
    public static final RegistrySupplier<StatusEffect> EXSANGUINATION = EFFECT_DEF_REG.register("exsanguination", EffectExsanguination::new);
    public static final RegistrySupplier<StatusEffect> EARTHQUAKE = EFFECT_DEF_REG.register("earthquake", EffectEarthquake::new);
    public static final RegistrySupplier<StatusEffect> FLEET_FOOTED = EFFECT_DEF_REG.register("fleet_footed", EffectFleetFooted::new);
    public static final RegistrySupplier<StatusEffect> POWER_DOWN = EFFECT_DEF_REG.register("power_down", EffectPowerDown::new);
    public static final RegistrySupplier<StatusEffect> MOSQUITO_REPELLENT = EFFECT_DEF_REG.register("mosquito_repellent", EffectMosquitoRepellent::new);

    public static final RegistrySupplier<Potion> KNOCKBACK_RESISTANCE_POTION = POTION_DEF_REG.register("knockback_resistance", ()-> new Potion(new StatusEffectInstance(KNOCKBACK_RESISTANCE.get(), 3600)));
    public static final RegistrySupplier<Potion> LONG_KNOCKBACK_RESISTANCE_POTION = POTION_DEF_REG.register("long_knockback_resistance", ()-> new Potion(new StatusEffectInstance(KNOCKBACK_RESISTANCE.get(), 9600)));
    public static final RegistrySupplier<Potion> STRONG_KNOCKBACK_RESISTANCE_POTION = POTION_DEF_REG.register("strong_knockback_resistance", ()-> new Potion(new StatusEffectInstance(KNOCKBACK_RESISTANCE.get(), 1800, 1)));
    public static final RegistrySupplier<Potion> LAVA_VISION_POTION = POTION_DEF_REG.register("lava_vision", ()-> new Potion(new StatusEffectInstance(LAVA_VISION.get(), 3600)));
    public static final RegistrySupplier<Potion> LONG_LAVA_VISION_POTION = POTION_DEF_REG.register("long_lava_vision", ()-> new Potion(new StatusEffectInstance(LAVA_VISION.get(), 9600)));
    public static final RegistrySupplier<Potion> SPEED_III_POTION = POTION_DEF_REG.register("speed_iii", ()-> new Potion(new StatusEffectInstance(StatusEffects.SPEED, 2200, 2)));
    public static final RegistrySupplier<Potion> POISON_RESISTANCE_POTION = POTION_DEF_REG.register("poison_resistance", ()-> new Potion(new StatusEffectInstance(POISON_RESISTANCE.get(), 3600)));
    public static final RegistrySupplier<Potion> LONG_POISON_RESISTANCE_POTION = POTION_DEF_REG.register("long_poison_resistance", ()-> new Potion(new StatusEffectInstance(POISON_RESISTANCE.get(), 9600)));
    public static final RegistrySupplier<Potion> BUG_PHEROMONES_POTION = POTION_DEF_REG.register("bug_pheromones", ()-> new Potion(new StatusEffectInstance(BUG_PHEROMONES.get(), 3600)));
    public static final RegistrySupplier<Potion> LONG_BUG_PHEROMONES_POTION = POTION_DEF_REG.register("long_bug_pheromones", ()-> new Potion(new StatusEffectInstance(BUG_PHEROMONES.get(), 9600)));
    public static final RegistrySupplier<Potion> SOULSTEAL_POTION = POTION_DEF_REG.register("soulsteal", ()-> new Potion(new StatusEffectInstance(SOULSTEAL.get(), 3600)));
    public static final RegistrySupplier<Potion> LONG_SOULSTEAL_POTION = POTION_DEF_REG.register("long_soulsteal", ()-> new Potion(new StatusEffectInstance(SOULSTEAL.get(), 9600)));
    public static final RegistrySupplier<Potion> STRONG_SOULSTEAL_POTION = POTION_DEF_REG.register("strong_soulsteal", ()-> new Potion(new StatusEffectInstance(SOULSTEAL.get(), 1800, 1)));
    public static final RegistrySupplier<Potion> CLINGING_POTION = POTION_DEF_REG.register("clinging", ()-> new Potion(new StatusEffectInstance(CLINGING.get(), 3600)));
    public static final RegistrySupplier<Potion> LONG_CLINGING_POTION = POTION_DEF_REG.register("long_clinging", ()-> new Potion(new StatusEffectInstance(CLINGING.get(), 9600)));

    public static ItemStack createPotion(Potion potion){
        return PotionUtil.setPotion(new ItemStack(Items.POTION), potion);
    }

    public static void init(){

        // TODO incompatibility
        // Potions.POISON with RATTLESNAKE_RATTLE => POISON_BOTTLE
        // LAVA_BOTTLE with BONE_SERPENT_TOOTH => LAVA_VISION_POTION
        // POISON_BOTTLE with CENTIPEDE_LEG => POISON_RESISTANCE_POTION
        // KOMODO_SPIT_BOTTLE with CENTIPEDE_LEG => POISON_RESISTANCE_POTION

        BrewingRecipeRegistry.registerPotionRecipe(Potions.STRENGTH, AMItemRegistry.BEAR_FUR.get(), KNOCKBACK_RESISTANCE_POTION.get());
        BrewingRecipeRegistry.registerPotionRecipe(KNOCKBACK_RESISTANCE_POTION.get(), Items.REDSTONE, LONG_KNOCKBACK_RESISTANCE_POTION.get());
        BrewingRecipeRegistry.registerPotionRecipe(KNOCKBACK_RESISTANCE_POTION.get(), Items.GLOWSTONE_DUST, STRONG_KNOCKBACK_RESISTANCE_POTION.get());
        BrewingRecipeRegistry.registerPotionRecipe(LAVA_VISION_POTION.get(), Items.REDSTONE, LONG_LAVA_VISION_POTION.get());
        BrewingRecipeRegistry.registerPotionRecipe(POISON_RESISTANCE_POTION.get(), AMItemRegistry.KOMODO_SPIT.get(), LONG_POISON_RESISTANCE_POTION.get());
        BrewingRecipeRegistry.registerPotionRecipe(Potions.STRONG_SWIFTNESS, AMItemRegistry.GAZELLE_HORN.get(), SPEED_III_POTION.get());
        BrewingRecipeRegistry.registerPotionRecipe(Potions.AWKWARD, AMItemRegistry.COCKROACH_WING.get(), BUG_PHEROMONES_POTION.get());
        BrewingRecipeRegistry.registerPotionRecipe(BUG_PHEROMONES_POTION.get(), Items.REDSTONE, LONG_BUG_PHEROMONES_POTION.get());
        BrewingRecipeRegistry.registerPotionRecipe(Potions.AWKWARD, AMItemRegistry.SOUL_HEART.get(), SOULSTEAL_POTION.get());
        BrewingRecipeRegistry.registerPotionRecipe(SOULSTEAL_POTION.get(), Items.REDSTONE, LONG_SOULSTEAL_POTION.get());
        BrewingRecipeRegistry.registerPotionRecipe(SOULSTEAL_POTION.get(), Items.GLOWSTONE_DUST, STRONG_SOULSTEAL_POTION.get());
        BrewingRecipeRegistry.registerPotionRecipe(Potions.AWKWARD, AMItemRegistry.DROPBEAR_CLAW.get(), CLINGING_POTION.get());
        BrewingRecipeRegistry.registerPotionRecipe(CLINGING_POTION.get(), Items.REDSTONE, LONG_CLINGING_POTION.get());
    }
}
