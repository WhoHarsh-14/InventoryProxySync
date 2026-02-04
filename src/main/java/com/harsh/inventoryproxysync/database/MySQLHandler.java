package com.harsh.inventoryproxysync.database;

import com.harsh.inventoryproxysync.config.ModConfig;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtSizeTracker;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.sql.*;
import java.util.UUID;

public class MySQLHandler implements DatabaseHandler {
    private Connection connection;

    @Override
    public void connect() throws Exception {
        ModConfig.MySQLConfig config = ModConfig.get().mysql;
        String url = "jdbc:mysql://" + config.host + ":" + config.port + "/" + config.database + "?autoReconnect=true&useSSL=false";
        
        Class.forName("com.mysql.cj.jdbc.Driver");
        connection = DriverManager.getConnection(url, config.user, config.password);
        createTable();
    }

    private void createTable() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS player_inventories (" +
                    "uuid VARCHAR(36) PRIMARY KEY, " +
                    "inventory_data LONGBLOB" +
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
    
    // Helper to ensure connection is alive
    private void checkConnection() throws Exception {
        if (connection == null || connection.isClosed() || !connection.isValid(2)) {
            connect();
        }
    }

    @Override
    public void saveInventory(UUID playerUUID, NbtCompound inventoryData) throws Exception {
        checkConnection();

        String sql = "INSERT INTO player_inventories (uuid, inventory_data) VALUES (?, ?) " +
                     "ON DUPLICATE KEY UPDATE inventory_data = VALUES(inventory_data)";
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
        checkConnection();

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
        return null;
    }
}
