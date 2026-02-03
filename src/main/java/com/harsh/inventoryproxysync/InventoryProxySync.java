package com.harsh.inventoryproxysync;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InventoryProxySync implements ModInitializer {
    public static final String MOD_ID = "inventoryproxysync";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("InventoryProxySync initializing...");
        
        // Initialize Config
        com.harsh.inventoryproxysync.config.ModConfig.load();
        LOGGER.info("Configuration loaded. Database Type: " + com.harsh.inventoryproxysync.config.ModConfig.get().databaseType);
        
        // Initialize Database
        try {
            com.harsh.inventoryproxysync.database.DatabaseManager.init();
            LOGGER.info("Database initialized successfully.");
        } catch (Exception e) {
            LOGGER.error("Failed to initialize database!", e);
        }

        // Register Events
        com.harsh.inventoryproxysync.event.ModEvents.register();
    }
}
