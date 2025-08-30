package com.github.alexthe666.alexsmobs.registry;

import com.github.alexthe666.alexsmobs.misc.AMAdvancementTrigger;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.util.Identifier;

public class AMAdvancementTriggerRegistry {

    public static final AMAdvancementTrigger MOSQUITO_SICK = new AMAdvancementTrigger(new Identifier("alexsmobs:mosquito_sick"));
    public static final AMAdvancementTrigger EMU_DODGE = new AMAdvancementTrigger(new Identifier("alexsmobs:emu_dodge"));
    public static final AMAdvancementTrigger STOMP_LEAFCUTTER_ANTHILL = new AMAdvancementTrigger(new Identifier("alexsmobs:stomp_leafcutter_anthill"));
    public static final AMAdvancementTrigger BALD_EAGLE_CHALLENGE = new AMAdvancementTrigger(new Identifier("alexsmobs:bald_eagle_challenge"));
    public static final AMAdvancementTrigger VOID_WORM_SUMMON = new AMAdvancementTrigger(new Identifier("alexsmobs:void_worm_summon"));
    public static final AMAdvancementTrigger VOID_WORM_SPLIT = new AMAdvancementTrigger(new Identifier("alexsmobs:void_worm_split"));
    public static final AMAdvancementTrigger VOID_WORM_SLAY_HEAD = new AMAdvancementTrigger(new Identifier("alexsmobs:void_worm_kill"));
    public static final AMAdvancementTrigger SEAGULL_STEAL = new AMAdvancementTrigger(new Identifier("alexsmobs:seagull_steal"));
    public static final AMAdvancementTrigger LAVIATHAN_FOUR_PASSENGERS = new AMAdvancementTrigger(new Identifier("alexsmobs:laviathan_four_passengers"));
    public static final AMAdvancementTrigger TRANSMUTE_1000_ITEMS = new AMAdvancementTrigger(new Identifier("alexsmobs:transmute_1000_items"));
    public static final AMAdvancementTrigger UNDERMINE_UNDERMINER = new AMAdvancementTrigger(new Identifier("alexsmobs:undermine_underminer"));

    public static final AMAdvancementTrigger ELEPHANT_SWAG = new AMAdvancementTrigger(new Identifier("alexsmobs:elephant_swag"));
    public static final AMAdvancementTrigger SKUNK_SPRAY = new AMAdvancementTrigger(new Identifier("alexsmobs:skunk_spray"));

    public static void init(){
        Criteria.register(MOSQUITO_SICK);
        Criteria.register(EMU_DODGE);
        Criteria.register(STOMP_LEAFCUTTER_ANTHILL);
        Criteria.register(BALD_EAGLE_CHALLENGE);
        Criteria.register(VOID_WORM_SUMMON);
        Criteria.register(VOID_WORM_SPLIT);
        Criteria.register(VOID_WORM_SLAY_HEAD);
        Criteria.register(SEAGULL_STEAL);
        Criteria.register(LAVIATHAN_FOUR_PASSENGERS);
        Criteria.register(TRANSMUTE_1000_ITEMS);
        Criteria.register(UNDERMINE_UNDERMINER);
        Criteria.register(ELEPHANT_SWAG);
        Criteria.register(SKUNK_SPRAY);
    }

}
