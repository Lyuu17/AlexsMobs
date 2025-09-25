package com.github.alexthe666.alexsmobs.misc;

import com.github.alexthe666.alexsmobs.AlexsMobs;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.util.Identifier;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;

public class SpawnBiomeConfig {
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().registerTypeAdapter(SpawnBiomeData.class, new SpawnBiomeData.Deserializer()).create();
    private final Identifier fileName;

    private SpawnBiomeConfig(Identifier fileName) {
        if (!fileName.getNamespace().endsWith(".json")) {
            this.fileName = new Identifier(fileName.getNamespace(), fileName.getPath() + ".json");
        } else {
            this.fileName = fileName;
        }

    }

//    public static SpawnBiomeData create(Identifier fileName, SpawnBiomeData dataDefault) {
//        var config = new SpawnBiomeConfig(fileName);
//        var data = config.getConfigData(dataDefault);
//        return data;
//    }

    public static <T> T getOrCreateConfigFile(File configDir, String configName, T defaults, Type type) {
        var configFile = new File(configDir, configName);
        if (!configFile.exists()) {
            try {
                FileUtils.write(configFile, GSON.toJson(defaults));
            } catch (IOException e) {
                AlexsMobs.LOGGER.error("Spawn Biome Config: Could not write " + configFile, e);
            }
        }
        try {
            return GSON.fromJson(FileUtils.readFileToString(configFile), type);
        } catch (Exception e) {
            AlexsMobs.LOGGER.error("Spawn Biome Config: Could not load " + configFile, e);
        }

        return defaults;
    }

//    private File getConfigDirFile() {
//        // FIXME forge path
////        Path configPath = FMLPaths.CONFIGDIR.get();
//        Path jsonPath = Paths.get(configPath.toAbsolutePath().toString(), fileName.getNamespace());
//        return jsonPath.toFile();
//    }
//
//    private SpawnBiomeData getConfigData(SpawnBiomeData defaultConfigData) {
//        SpawnBiomeData configData = getOrCreateConfigFile(getConfigDirFile(), fileName.getPath(), defaultConfigData, new TypeToken<SpawnBiomeData>() {
//        }.getType());
//        return configData;
//    }
}