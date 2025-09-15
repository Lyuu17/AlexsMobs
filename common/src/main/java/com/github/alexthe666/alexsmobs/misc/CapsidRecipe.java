package com.github.alexthe666.alexsmobs.misc;

import com.google.gson.*;
import com.iafenvoy.uranus.client.model.tabula.JsonUtils;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.ShapedRecipe;
import net.minecraft.util.collection.DefaultedList;

import java.lang.reflect.Type;

public class CapsidRecipe {
    private final DefaultedList<Ingredient> ingredients;
    private ItemStack result = ItemStack.EMPTY;
    private int time = 0;

    public CapsidRecipe(DefaultedList<Ingredient> ingredients, ItemStack result, int time) {
        this.result = result;
        this.ingredients = ingredients;
        this.time = time;
    }

    private static DefaultedList<Ingredient> readIngredients(JsonArray ingredientArray) {
        DefaultedList<Ingredient> defaultedList = DefaultedList.of();

        for (int i = 0; i < ingredientArray.size(); ++i) {
            Ingredient ingredient = Ingredient.fromJson(ingredientArray.get(i));
            if (!ingredient.isEmpty()) {
                defaultedList.add(ingredient);
            }
        }
        return defaultedList;
    }

    public ItemStack getResult() {
        return result;
    }

    public DefaultedList<Ingredient> getIngredients() {
        return ingredients;
    }

    public int getTime() {
        return time;
    }

    public boolean matches(ItemStack... stacks) {
        IntList taken = new IntArrayList();
        ItemStack[] copy = new ItemStack[stacks.length];
        for (int j = 0; j < copy.length; j++) {
            copy[j] = stacks[j].copy();
            for (int i = 0; i < ingredients.size(); i++) {
                if (ingredients.get(i).test(copy[j])) {
                    taken.add(j);
                    copy[j].decrement(1);
                }
            }
        }
        return taken.size() >= ingredients.size();
    }

    public static class Deserializer implements JsonDeserializer<CapsidRecipe> {

        @Override
        public CapsidRecipe deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject jsonobject = json.getAsJsonObject();
            int time = JsonUtils.getInt(jsonobject, "time");
            ItemStack result = ItemStack.EMPTY;
            if (jsonobject.has("result")) {
                result = ShapedRecipe.outputFromJson(JsonUtils.getJsonObject(jsonobject, "result"));
            }
            DefaultedList<Ingredient> DefaultedList = readIngredients(JsonUtils.getJsonArray(jsonobject, "ingredients"));
            return new CapsidRecipe(DefaultedList, result, time);
        }

    }
}
