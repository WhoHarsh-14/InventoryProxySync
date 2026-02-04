package com.harsh.inventoryproxysync.database;

import com.harsh.inventoryproxysync.config.ModConfig;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtSizeTracker;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;

import java.io.*;
import java.sql.*;
import java.util.UUID;

public class SQLiteHandler implements DatabaseHandler {
    private Connection connection;

    @Override
    public void connect() throws Exception {
        String path = ModConfig.get().sqlite.path;
        File dbFile = new File(path);
        // Ensure parent exists
        if (dbFile.getParentFile() != null) {
            dbFile.getParentFile().mkdirs();
        }
        
        Class.forName("org.sqlite.JDBC");
        connection = DriverManager.getConnection("jdbc:sqlite:" + path);
        createTable();
    }

    private void createTable() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS player_inventories (" +
                    "uuid VARCHAR(36) PRIMARY KEY, " +
                    "inventory_data BLOB" +
                    ")");
        }
    }

    @Override
    public void disconnect() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void saveInventory(UUID playerUUID, NbtCompound inventoryData) throws Exception {
        if (connection == null || connection.isClosed()) connect();

        String sql = "INSERT OR REPLACE INTO player_inventories (uuid, inventory_data) VALUES (?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, playerUUID.toString());
            
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            NbtIo.writeCompressed(inventoryData, baos);
            pstmt.setBytes(2, baos.toByteArray());
            
            pstmt.executeUpdate();
        }
    }

    @Override
    public NbtCompound loadInventory(UUID playerUUID) throws Exception {
        if (connection == null || connection.isClosed()) connect();

        String sql = "SELECT inventory_data FROM player_inventories WHERE uuid = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, playerUUID.toString());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    byte[] data = rs.getBytes("inventory_data");
                    if (data != null) {
                        try (ByteArrayInputStream bais = new ByteArrayInputStream(data)) {
                            return NbtIo.readCompressed(bais, NbtSizeTracker.ofUnlimitedBytes());
                        }
                    }
                }
            }
        }
        return null; // No data found
    }
}
