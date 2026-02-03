package com.harsh.inventoryproxysync.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class ModConfig {
    private static ModConfig INSTANCE;
    private static final File CONFIG_FILE = FabricLoader.getInstance().getConfigDir().resolve("inventoryproxysync.json").toFile();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public String databaseType = "SQLITE"; // SQLITE, MYSQL
    public MySQLConfig mysql = new MySQLConfig();
    public SQLiteConfig sqlite = new SQLiteConfig();

    public static class MySQLConfig {
        public String host = "localhost";
        public int port = 3306;
        public String database = "minecraft";
        public String user = "root";
        public String password = "password";
    }

    public static class SQLiteConfig {
        public String path = "inventoryproxysync.db";
    }

    public static void load() {
        if (CONFIG_FILE.exists()) {
            try (FileReader reader = new FileReader(CONFIG_FILE)) {
                INSTANCE = GSON.fromJson(reader, ModConfig.class);
            } catch (IOException e) {
                e.printStackTrace();
                INSTANCE = new ModConfig();
            }
        } else {
            INSTANCE = new ModConfig();
            save();
        }
    }

    public static void save() {
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(INSTANCE, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static ModConfig get() {
        return INSTANCE;
    }
}
