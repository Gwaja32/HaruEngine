package kr.haruserver.haruengine.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public class ConfigManager {
    private static final Path PATH = Path.of("config/haru_engine/config.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static ConfigData data = new ConfigData();

    public static class ConfigData {
        public boolean enableDynamicDistance = true; // 기본값 true
    }

    public static void load() {
        try {
            if (!Files.exists(PATH)) {
                Files.createDirectories(PATH.getParent());
                save();
            }
            data = GSON.fromJson(Files.newBufferedReader(PATH), ConfigData.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void save() {
        try (Writer writer = Files.newBufferedWriter(PATH)) {
            GSON.toJson(data, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
