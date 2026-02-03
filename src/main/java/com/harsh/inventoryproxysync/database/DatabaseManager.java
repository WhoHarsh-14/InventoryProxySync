package com.harsh.inventoryproxysync.database;

import com.harsh.inventoryproxysync.config.ModConfig;

public class DatabaseManager {
    private static DatabaseHandler handler;

    public static void init() throws Exception {
        String type = ModConfig.get().databaseType.toUpperCase();
        if ("MYSQL".equals(type)) {
            handler = new MySQLHandler();
        } else {
            handler = new SQLiteHandler();
        }
        handler.connect();
    }

    public static DatabaseHandler getHandler() {
        return handler;
    }

    public static void close() {
        if (handler != null) {
            handler.disconnect();
        }
    }
}
