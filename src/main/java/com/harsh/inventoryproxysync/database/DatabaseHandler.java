package com.harsh.inventoryproxysync.database;

import net.minecraft.nbt.NbtCompound;

import java.util.UUID;

public interface DatabaseHandler {
    void connect() throws Exception;
    void disconnect();
    void saveInventory(UUID playerUUID, NbtCompound inventoryData) throws Exception;
    NbtCompound loadInventory(UUID playerUUID) throws Exception;
}
