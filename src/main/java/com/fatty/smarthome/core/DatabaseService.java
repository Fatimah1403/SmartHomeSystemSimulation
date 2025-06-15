package com.fatty.smarthome.core;

import com.fatty.smarthome.devices.Light;
import com.fatty.smarthome.devices.SecurityCamera;
import com.fatty.smarthome.devices.SmartDevice;
import com.fatty.smarthome.devices.Thermostat;
import com.fatty.smarthome.util.SQLiteConnector;
import com.fatty.smarthome.util.SmartHomeException;

import java.io.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// placeholder for now, value added
public class DatabaseService {
    private static final String LOG_FILE = "device_log.txt";
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private  static DatabaseService instance;

    private DatabaseService() throws SQLException {
        initializeDatabase();

    }
    public static synchronized DatabaseService getInstance() throws SQLException {
        if (instance == null) {
            instance = new DatabaseService();
        }
        return instance;
    }

    private void initializeDatabase() throws SQLException {
        try {
            // Use SQLiteConnector to create all tables
            SQLiteConnector.createAllTables();
            System.out.println("✅ Database initialized successfully");
        } catch (SQLException e) {
            System.err.println("❌ Database initialization failed: " + e.getMessage());
            throw e;  // Re-throw to maintain the method contract
        }
    }
    /**
     * Save a single device
     */
    @SuppressWarnings("SqlResolve")

    public void saveDevice(SmartDevice device) throws SmartHomeException {
        String sql = """
            INSERT OR REPLACE INTO devices (name, type, status, value, location, last_updated)
            VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
            """;
        try (Connection conn = SQLiteConnector.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, device.getName());
            pstmt.setString(2, device.getClass().getSimpleName());
            pstmt.setString(3, device.isOn() ? "ON" : "OFF");

            // Handle device-specific values
            if (device instanceof Thermostat) {
                pstmt.setInt(4, ((Thermostat) device).getTemperature());
            } else {
                pstmt.setInt(4, 0);
            }

            pstmt.setString(5, device.getLocation() != null ? device.getLocation() : "Unknown");

            pstmt.executeUpdate();

            // Log the save action
            logAction(device.getName(), "SAVED", null, device.getStatus());

        } catch (SQLException e) {
            throw new SmartHomeException("Failed to save device: " + e.getMessage());
        }
    }


    /**
     * Save all devices (used by save command)
     */
    @SuppressWarnings("SqlResolve")
    public void saveAllDevices(List<SmartDevice> devices) throws SmartHomeException {
        Connection conn = null;
        try {
            conn = SQLiteConnector.connect();
            conn.setAutoCommit(false); // Start transaction

            for (SmartDevice device : devices) {
                saveDeviceInTransaction(conn, device);
            }

            conn.commit(); // Commit all changes at once
            System.out.println("✅ Saved " + devices.size() + " devices to database");

        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    // Ignore rollback errors
                }
            }
            throw new SmartHomeException("Failed to save devices: " + e.getMessage());
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    // Ignore close errors
                }
            }
        }
    }

    /**
     * Helper method to save device within a transaction
     */
    @SuppressWarnings("SqlResolve")
    private void saveDeviceInTransaction(Connection conn, SmartDevice device) throws SQLException {
        String sql = """
            INSERT OR REPLACE INTO devices (name, type, status, value, location, last_updated)
            VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
            """;

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, device.getName());
            pstmt.setString(2, device.getClass().getSimpleName());
            pstmt.setString(3, device.isOn() ? "ON" : "OFF");

            if (device instanceof Thermostat) {
                pstmt.setInt(4, ((Thermostat) device).getTemperature());
            } else {
                pstmt.setInt(4, 0);
            }

            pstmt.setString(5, device.getLocation() != null ? device.getLocation() : "Unknown");
            pstmt.executeUpdate();
        }
    }

    /**
     * Load all devices
     */
    @SuppressWarnings("SqlResolve")
    public List<SmartDevice> loadAllDevices() throws SmartHomeException {
        List<SmartDevice> devices = new ArrayList<>();
        String sql = "SELECT * FROM devices ORDER BY name";

        try (Connection conn = SQLiteConnector.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                SmartDevice device = createDeviceFromResultSet(rs);
                if (device != null) {
                    devices.add(device);
                }
            }

            System.out.println("✅ Loaded " + devices.size() + " devices from database");

        } catch (SQLException e) {
            throw new SmartHomeException("Failed to load devices: " + e.getMessage());
        }

        return devices;
    }

    /**
     * Create device object from database record
     */
    @SuppressWarnings("SqlResolve")
    private SmartDevice createDeviceFromResultSet(ResultSet rs) throws SQLException {
        String name = rs.getString("name");
        String type = rs.getString("type");
        boolean isOn = "ON".equals(rs.getString("status"));
        int value = rs.getInt("value");
        String location = rs.getString("location");

        SmartDevice device = null;

        switch (type) {
            case "Light" -> device = new Light(name);
            case "Thermostat" -> {
                device = new Thermostat(name);
                ((Thermostat) device).setTemperature(value);
            }
            case "SecurityCamera" -> device = new SecurityCamera(name);
        }

        if (device != null) {
            if (isOn) {
                device.turnOn();
            } else {
                device.turnOff();
            }
            device.setLocation(location);
        }

        return device;
    }

    /**
     * Log device actions
     */
    @SuppressWarnings("SqlResolve")
    public void logAction(String deviceName, String action, String oldValue, String newValue) {
        String sql = """
            INSERT INTO event_logs (device_name, action, old_value, new_value)
            VALUES (?, ?, ?, ?)
            """;

        try (Connection conn = SQLiteConnector.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, deviceName);
            pstmt.setString(2, action);
            pstmt.setString(3, oldValue);
            pstmt.setString(4, newValue);

            pstmt.executeUpdate();

        } catch (SQLException e) {
            // Silent fail - logging shouldn't break main functionality
        }
    }

    /**
     * Get device statistics (aggregation example)
     */
    @SuppressWarnings("SqlResolve")
    public Map<String, Object> getDeviceStatistics(String deviceName) {
        Map<String, Object> stats = new HashMap<>();

        // Query 1: Event count and last action
        String eventSql = """
            SELECT COUNT(*) as event_count, MAX(timestamp) as last_action
            FROM event_logs
            WHERE device_name = ?
            """;

        try (Connection conn = SQLiteConnector.connect();
             PreparedStatement pstmt = conn.prepareStatement(eventSql)) {

            pstmt.setString(1, deviceName);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                stats.put("eventCount", rs.getInt("event_count"));
                stats.put("lastAction", rs.getTimestamp("last_action"));
            }

        } catch (SQLException e) {
            stats.put("error", "Failed to get statistics");
        }

        // Query 2: Power usage stats
        String powerSql = """
            SELECT 
                AVG(power_watts) as avg_power,
                MAX(power_watts) as max_power,
                SUM(energy_kwh) as total_energy
            FROM power_usage
            WHERE device_name = ?
            """;

        try (Connection conn = SQLiteConnector.connect();
             PreparedStatement pstmt = conn.prepareStatement(powerSql)) {

            pstmt.setString(1, deviceName);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                stats.put("avgPower", rs.getDouble("avg_power"));
                stats.put("maxPower", rs.getDouble("max_power"));
                stats.put("totalEnergy", rs.getDouble("total_energy"));
            }

        } catch (SQLException e) {
            // Already handled
        }

        return stats;
    }

    /**
     * Get system summary (multiple tables with aggregation)
     */
    @SuppressWarnings("SqlResolve")
    public String getSystemSummary() {
        StringBuilder summary = new StringBuilder();

        try (Connection conn = SQLiteConnector.connect();
             Statement stmt = conn.createStatement()) {

            // Query 1: Device summary by type
            String deviceSql = """
                SELECT 
                    type,
                    COUNT(*) as total,
                    SUM(CASE WHEN status = 'ON' THEN 1 ELSE 0 END) as on_count
                FROM devices
                GROUP BY type
                ORDER BY type
                """;

            ResultSet rs = stmt.executeQuery(deviceSql);

            summary.append("Device Summary:\n");
            while (rs.next()) {
                summary.append(String.format("  %s: %d devices (%d on)\n",
                        rs.getString("type"),
                        rs.getInt("total"),
                        rs.getInt("on_count")));
            }

            // Query 2: Recent activity
            String activitySql = """
                SELECT COUNT(*) as activity_count
                FROM event_logs
                WHERE timestamp > datetime('now', '-24 hours')
                """;

            rs = stmt.executeQuery(activitySql);
            if (rs.next()) {
                summary.append("\nLast 24 hours: ")
                        .append(rs.getInt("activity_count"))
                        .append(" events\n");
            }

            // Query 3: Total power consumption
            String powerSql = """
                SELECT 
                    COUNT(DISTINCT device_name) as active_devices,
                    SUM(power_watts) as total_power
                FROM power_usage
                WHERE timestamp > datetime('now', '-1 hour')
                """;

            rs = stmt.executeQuery(powerSql);
            if (rs.next()) {
                summary.append(String.format("\nCurrent Power: %.2f W from %d devices\n",
                        rs.getDouble("total_power"),
                        rs.getInt("active_devices")));
            }

        } catch (SQLException e) {
            summary.append("\nError generating summary: ").append(e.getMessage());
        }

        return summary.toString();
    }

    /**
     * Save power usage data
     */
    @SuppressWarnings("SqlResolve")
    public void savePowerUsage(String deviceName, double powerWatts) {
        String sql = """
            INSERT INTO power_usage (device_name, power_watts, duration_minutes)
            VALUES (?, ?, 60)
            """;

        try (Connection conn = SQLiteConnector.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, deviceName);
            pstmt.setDouble(2, powerWatts);

            pstmt.executeUpdate();

        } catch (SQLException e) {
            // Silent fail
        }
    }

    /**
     * Get devices by location (filtering example)
     */
    @SuppressWarnings("SqlResolve")
    public List<SmartDevice> getDevicesByLocation(String location) throws SmartHomeException {
        List<SmartDevice> devices = new ArrayList<>();
        String sql = "SELECT * FROM devices WHERE location = ? ORDER BY name";

        try (Connection conn = SQLiteConnector.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, location);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                SmartDevice device = createDeviceFromResultSet(rs);
                if (device != null) {
                    devices.add(device);
                }
            }

        } catch (SQLException e) {
            throw new SmartHomeException("Failed to get devices by location: " + e.getMessage());
        }

        return devices;
    }

    /**
     * Save automation rule
     */
    @SuppressWarnings("SqlResolve")
    public void saveAutomationRule(String ruleName, String triggerDevice, String triggerCondition,
                                   String actionDevice, String actionCommand) throws SmartHomeException {
        String sql = """
            INSERT INTO automation_rules (rule_name, trigger_device, trigger_condition, action_device, action_command)
            VALUES (?, ?, ?, ?, ?)
            """;

        try (Connection conn = SQLiteConnector.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, ruleName);
            pstmt.setString(2, triggerDevice);
            pstmt.setString(3, triggerCondition);
            pstmt.setString(4, actionDevice);
            pstmt.setString(5, actionCommand);

            pstmt.executeUpdate();

            System.out.println("✅ Automation rule saved: " + ruleName);

        } catch (SQLException e) {
            throw new SmartHomeException("Failed to save automation rule: " + e.getMessage());
        }
    }

    // Add these methods to your existing DatabaseService class:

    /**
     * Clear all event logs (equivalent to clearing log file)
     */
    public void clearEventLogs() throws SmartHomeException {
        String sql = "DELETE FROM event_logs";

        try (Connection conn = SQLiteConnector.connect();
             Statement stmt = conn.createStatement()) {

            int deleted = stmt.executeUpdate(sql);
            System.out.println("✅ Cleared " + deleted + " event log entries");

        } catch (SQLException e) {
            throw new SmartHomeException("Failed to clear event logs: " + e.getMessage());
        }
    }

    /**
     * Read event logs (similar to reading log file)
     */
    public List<EventLog> readEventLogs() throws SmartHomeException {
        List<EventLog> logs = new ArrayList<>();
        String sql = """
        SELECT device_name, action, old_value, new_value, timestamp 
        FROM event_logs 
        ORDER BY timestamp DESC
        LIMIT 100
        """;

        try (Connection conn = SQLiteConnector.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                EventLog log = new EventLog(
                        rs.getString("device_name"),
                        rs.getString("action"),
                        rs.getString("old_value"),
                        rs.getString("new_value"),
                        rs.getTimestamp("timestamp")
                );
                logs.add(log);
            }

        } catch (SQLException e) {
            throw new SmartHomeException("Failed to read event logs: " + e.getMessage());
        }

        return logs;
    }

    /**
     * Delete a device from the database
     */
    public void deleteDevice(String deviceName) throws SmartHomeException {
        String sql = "DELETE FROM devices WHERE name = ?";

        try (Connection conn = SQLiteConnector.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, deviceName);
            int deleted = pstmt.executeUpdate();

            if (deleted > 0) {
                System.out.println("✅ Device deleted from database: " + deviceName);
                logAction(deviceName, "DELETED", null, null);
            }

        } catch (SQLException e) {
            throw new SmartHomeException("Failed to delete device: " + e.getMessage());
        }
    }

    /**
     * Inner class to represent an event log entry
     */
    public static class EventLog {
        private final String deviceName;
        private final String action;
        private final String oldValue;
        private final String newValue;
        private final Timestamp timestamp;

        public EventLog(String deviceName, String action, String oldValue,
                        String newValue, Timestamp timestamp) {
            this.deviceName = deviceName;
            this.action = action;
            this.oldValue = oldValue;
            this.newValue = newValue;
            this.timestamp = timestamp;
        }

        // Getters
        public String getDeviceName() { return deviceName; }
        public String getAction() { return action; }
        public String getOldValue() { return oldValue; }
        public String getNewValue() { return newValue; }
        public Timestamp getTimestamp() { return timestamp; }

        @Override
        public String toString() {
            return String.format("[%s] %s: %s %s -> %s",
                    timestamp, deviceName, action,
                    oldValue != null ? oldValue : "N/A",
                    newValue != null ? newValue : "N/A");
        }
    }

}
