package com.github.alexthe666.alexsmobs.misc;

import com.github.alexthe666.alexsmobs.AlexsMobs;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import net.minecraft.item.ItemStack;
import net.minecraft.resource.JsonDataLoader;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profiler;
import org.apache.logging.log4j.Level;

import java.util.List;
import java.util.Map;

public class CapsidRecipeManager extends JsonDataLoader {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().registerTypeAdapter(CapsidRecipe.class, new CapsidRecipe.Deserializer()).create();

    private final List<CapsidRecipe> capsidRecipes = Lists.newArrayList();

    public CapsidRecipeManager() {
        super(GSON, "capsid_recipes");
    }

    @Override
    protected void apply(Map<Identifier, JsonElement> jsonMap, ResourceManager resourceManager, Profiler profile) {
        this.capsidRecipes.clear();
        ImmutableMap.Builder<Identifier, CapsidRecipe> builder = ImmutableMap.builder();
        AlexsMobs.LOGGER.log(Level.ALL, "Loading in capsid_recipes jsons...");
        jsonMap.forEach((Identifier, jsonElement) -> {
            try {
                CapsidRecipe capsidRecipe = GSON.fromJson(jsonElement, CapsidRecipe.class);
                builder.put(Identifier, capsidRecipe);
            } catch (Exception exception) {
                AlexsMobs.LOGGER.error("Couldn't parse capsid recipe {}", Identifier, exception);
            }
        });
        ImmutableMap<Identifier, CapsidRecipe> immutablemap = builder.build();
        immutablemap.forEach((Identifier, capsidRecipe) -> {
            capsidRecipes.add(capsidRecipe);
        });
    }

    public CapsidRecipe getRecipeFor(ItemStack stack){
        for(CapsidRecipe recipe : capsidRecipes){
            if(recipe.matches(stack)){
                return recipe;
            }
        }

        return null;
    }

    public List<CapsidRecipe> getCapsidRecipes() {
        return capsidRecipes;
    }

    @Override
    public String getName() {
        return "CapsidRecipeManager";
    }
}
