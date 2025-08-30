package com.github.alexthe666.alexsmobs.misc;

import com.google.gson.JsonObject;
import net.minecraft.advancement.criterion.AbstractCriterion;
import net.minecraft.advancement.criterion.AbstractCriterionConditions;
import net.minecraft.advancement.criterion.ConstructBeaconCriterion;
import net.minecraft.predicate.NumberRange;
import net.minecraft.predicate.entity.AdvancementEntityPredicateDeserializer;
import net.minecraft.predicate.entity.AdvancementEntityPredicateSerializer;
import net.minecraft.predicate.entity.LootContextPredicate;
import net.minecraft.util.Identifier;

public class AMAdvancementTrigger extends AbstractCriterion<AMAdvancementTrigger.Instance> {
    public final Identifier id;

    public AMAdvancementTrigger(Identifier resourceLocation) {
        this.id = resourceLocation;
    }

    @Override
    public Identifier getId() {
        return id;
    }

    @Override
    protected Instance conditionsFromJson(JsonObject obj, LootContextPredicate playerPredicate, AdvancementEntityPredicateDeserializer predicateDeserializer) {
        return new AMAdvancementTrigger.Instance(id, playerPredicate);
    }

    public static class Instance extends AbstractCriterionConditions {

        public Instance(Identifier id, LootContextPredicate entity) {
            super(id, entity);
        }

        public static ConstructBeaconCriterion.Conditions forLevel(NumberRange.IntRange intRange) {
            return new ConstructBeaconCriterion.Conditions(LootContextPredicate.EMPTY, intRange);
        }

        @Override
        public JsonObject toJson(AdvancementEntityPredicateSerializer predicateSerializer) {
            return super.toJson(predicateSerializer);
        }
    }
}
