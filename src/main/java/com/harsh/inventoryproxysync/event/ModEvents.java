package com.harsh.inventoryproxysync.event;

import com.harsh.inventoryproxysync.InventoryProxySync;
import com.harsh.inventoryproxysync.database.DatabaseManager;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.GameMode;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ModEvents {
    // Single-threaded executor to ensure saves happen in order and don't block the
    // main thread
    private static final ExecutorService saveExecutor = Executors.newSingleThreadExecutor();

    public static void register() {
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            InventoryProxySync.LOGGER.info("Stopping server, closing database connection...");
            saveExecutor.shutdown(); // Gracefully shutdown executor
            DatabaseManager.close();
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            try {
                RegistryWrapper.WrapperLookup registryLookup = server.getRegistryManager();

                NbtCompound data = DatabaseManager.getHandler().loadInventory(player.getUuid());
                if (data != null) {
                    InventoryProxySync.LOGGER.info("Loading data for player " + player.getName().getString());

                    // Inventory
                    if (data.contains("Inventory")) {
                        player.getInventory().clear();
                        player.getInventory().readNbt(data.getList("Inventory", 10));
                    }

                    // Ender Chest
                    if (data.contains("EnderChest")) {
                        player.getEnderChestInventory().clear();
                        player.getEnderChestInventory().readNbtList(data.getList("EnderChest", 10), registryLookup);
                    }

                    // Health
                    if (data.contains("Health")) {
                        player.setHealth(data.getFloat("Health"));
                    }

                    // Hunger & Saturation
                    if (data.contains("Hunger")) {
                        player.getHungerManager().setFoodLevel(data.getInt("Hunger"));
                        player.getHungerManager().setSaturationLevel(data.getFloat("Saturation"));
                    }

                    // Gamemode
                    if (data.contains("GameMode")) {
                        int gameModeId = data.getInt("GameMode");
                        GameMode gameMode = GameMode.byId(gameModeId);
                        player.changeGameMode(gameMode);
                    }

                } else {
                    InventoryProxySync.LOGGER.info("No saved data found for player " + player.getName().getString());
                }
            } catch (Exception e) {
                InventoryProxySync.LOGGER.error("Failed to load data for " + player.getName().getString(), e);
            }
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ServerPlayerEntity player = handler.getPlayer();

            // Capture data on the main thread
            RegistryWrapper.WrapperLookup registryLookup = server.getRegistryManager();
            NbtCompound data = new NbtCompound();

            // Inventory
            NbtList inventoryList = new NbtList();
            player.getInventory().writeNbt(inventoryList);
            data.put("Inventory", inventoryList);

            // Ender Chest
            NbtList enderList = player.getEnderChestInventory().toNbtList(registryLookup);
            data.put("EnderChest", enderList);

            // Health
            data.putFloat("Health", player.getHealth());

            // Hunger & Saturation
            data.putInt("Hunger", player.getHungerManager().getFoodLevel());
            data.putFloat("Saturation", player.getHungerManager().getSaturationLevel());

            // Gamemode
            data.putInt("GameMode", player.interactionManager.getGameMode().getId());

            // Offload save to background thread
            saveExecutor.submit(() -> {
                try {
                    DatabaseManager.getHandler().saveInventory(player.getUuid(), data);
                    InventoryProxySync.LOGGER
                            .info("Saved data asynchronously for player " + player.getName().getString());
                } catch (Exception e) {
                    InventoryProxySync.LOGGER.error("Failed to save data for " + player.getName().getString(), e);
                }
            });
        });
    }
}
