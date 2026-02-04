package com.harsh.inventoryproxysync.database;

import com.harsh.inventoryproxysync.config.ModConfig;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtSizeTracker;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisPooled;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.UUID;

public class RedisHandler implements DatabaseHandler {
    private JedisPooled jedis;
    private static final String KEY_PREFIX = "inv_sync:";

    @Override
    public void connect() throws Exception {
        ModConfig.RedisConfig config = ModConfig.get().redis;

        HostAndPort address = new HostAndPort(config.host, config.port);
        DefaultJedisClientConfig.Builder clientConfig = DefaultJedisClientConfig.builder();

        if (config.password != null && !config.password.isEmpty()) {
            if (config.user != null && !config.user.isEmpty()) {
                clientConfig.user(config.user);
            }
            clientConfig.password(config.password);
        }

        clientConfig.database(config.database);

        // JedisPooled handles connection pooling automatically
        jedis = new JedisPooled(address, clientConfig.build());
    }

    @Override
    public void disconnect() {
        if (jedis != null) {
            jedis.close();
        }
    }

    @Override
    public void saveInventory(UUID playerUUID, NbtCompound inventoryData) throws Exception {
        if (jedis == null)
            connect();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            NbtIo.writeCompressed(inventoryData, baos);
        } catch (IOException e) {
            throw new RuntimeException("Failed to compress NBT", e);
        }

        byte[] data = baos.toByteArray();
        // Use set (upsert) - this replaces existing value
        jedis.set((KEY_PREFIX + playerUUID).getBytes(), data);
    }

    @Override
    public NbtCompound loadInventory(UUID playerUUID) throws Exception {
        if (jedis == null)
            connect();

        byte[] data = jedis.get((KEY_PREFIX + playerUUID).getBytes());
        if (data != null) {
            try (ByteArrayInputStream bais = new ByteArrayInputStream(data)) {
                return NbtIo.readCompressed(bais, NbtSizeTracker.ofUnlimitedBytes());
            }
        }
        return null; // Not found
    }
}
