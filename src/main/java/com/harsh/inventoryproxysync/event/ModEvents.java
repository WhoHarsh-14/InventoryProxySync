package com.harsh.inventoryproxysync.event;

import com.harsh.inventoryproxysync.InventoryProxySync;
import com.harsh.inventoryproxysync.database.DatabaseManager;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.network.ServerPlayerEntity;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

public class ModEvents {
    public static void register() {
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            InventoryProxySync.LOGGER.info("Stopping server, closing database connection...");
            DatabaseManager.close();
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            try {
                NbtCompound data = DatabaseManager.getHandler().loadInventory(player.getUuid());
                if (data != null) {
                    InventoryProxySync.LOGGER.info("Loading inventory for player " + player.getName().getString());
                    if (data.contains("Inventory")) {
                        player.getInventory().clear();
                        player.getInventory().readNbt(data.getList("Inventory", 10));
                    }
                    if (data.contains("EnderChest")) {
                        player.getEnderChestInventory().clear();
                        player.getEnderChestInventory().readNbt(data.getList("EnderChest", 10));
                    }
                } else {
                    InventoryProxySync.LOGGER.info("No saved inventory found for player " + player.getName().getString());
                }
            } catch (Exception e) {
                InventoryProxySync.LOGGER.error("Failed to load inventory for " + player.getName().getString(), e);
            }
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            try {
                NbtCompound data = new NbtCompound();
                
                NbtList inventoryList = new NbtList();
                player.getInventory().writeNbt(inventoryList);
                data.put("Inventory", inventoryList);
                
                NbtList enderList = new NbtList();
                player.getEnderChestInventory().writeNbt(enderList);
                data.put("EnderChest", enderList);

                DatabaseManager.getHandler().saveInventory(player.getUuid(), data);
                InventoryProxySync.LOGGER.info("Saved inventory for player " + player.getName().getString());
            } catch (Exception e) {
                InventoryProxySync.LOGGER.error("Failed to save inventory for " + player.getName().getString(), e);
            }
        });
    }
}
