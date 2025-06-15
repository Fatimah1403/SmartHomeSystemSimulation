package com.fatty.smarthome.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class SQLiteConnector {
    private final static String URL = "jdbc:sqlite:databasesmart.db";

    public static Connection connect() {
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(URL);
            // Removed the print statement to reduce verbosity
        } catch (SQLException e) {
            System.err.println("❌ Failed to connect to SQLite database: " + e.getMessage());
            throw new RuntimeException("Database connection failed", e);
        }
        return conn;
    }

    /**
     * Create all tables needed for the Smart Home system
     */
    public static void createAllTables() throws SQLException {
        try (Connection conn = connect();
             Statement stmt = conn.createStatement()) {

            System.out.println("✅ Connected to SQLite database");

            // Create devices table
            String deviceTableSql = """
                CREATE TABLE IF NOT EXISTS devices (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL UNIQUE,
                    type TEXT NOT NULL,
                    status TEXT DEFAULT 'OFF' CHECK(status IN ('ON', 'OFF')),
                    value INTEGER DEFAULT 0,
                    location TEXT DEFAULT 'Unknown',
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                );
                """;
            stmt.execute(deviceTableSql);
            System.out.println("✅ Device table created");

            // Create event logs table
            String eventLogsSql = """
                CREATE TABLE IF NOT EXISTS event_logs (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    device_name TEXT NOT NULL,
                    action TEXT NOT NULL,
                    old_value TEXT,
                    new_value TEXT,
                    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (device_name) REFERENCES devices(name)
                );
                """;
            stmt.execute(eventLogsSql);
            System.out.println("✅ Event logs table created");

            // Create power usage table
            String powerUsageSql = """
                CREATE TABLE IF NOT EXISTS power_usage (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    device_name TEXT NOT NULL,
                    power_watts REAL NOT NULL,
                    duration_minutes INTEGER DEFAULT 60,
                    energy_kwh REAL DEFAULT 0,
                    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (device_name) REFERENCES devices(name)
                );
                """;
            stmt.execute(powerUsageSql);
            System.out.println("✅ Power usage table created");

            // Create automation rules table
            String automationRulesSql = """
                CREATE TABLE IF NOT EXISTS automation_rules (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    rule_name TEXT NOT NULL,
                    trigger_device TEXT NOT NULL,
                    trigger_condition TEXT NOT NULL,
                    action_device TEXT NOT NULL,
                    action_command TEXT NOT NULL,
                    is_active INTEGER DEFAULT 1,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (trigger_device) REFERENCES devices(name),
                    FOREIGN KEY (action_device) REFERENCES devices(name)
                );
                """;
            stmt.execute(automationRulesSql);
            System.out.println("✅ Automation rules table created");

            // Create indexes
            String[] indexes = {
                    "CREATE INDEX IF NOT EXISTS idx_device_type ON devices(type);",
                    "CREATE INDEX IF NOT EXISTS idx_event_device ON event_logs(device_name);",
                    "CREATE INDEX IF NOT EXISTS idx_event_timestamp ON event_logs(timestamp);",
                    "CREATE INDEX IF NOT EXISTS idx_power_device ON power_usage(device_name);",
                    "CREATE INDEX IF NOT EXISTS idx_power_timestamp ON power_usage(timestamp);"
            };

            for (String indexSql : indexes) {
                stmt.execute(indexSql);
            }
            System.out.println("✅ Database indexes created");

        } catch (SQLException e) {
            System.err.println("❌ Database setup failed: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Create the devices table
     */
    public static void createDeviceTable() throws SQLException {
        String sql = """
            CREATE TABLE IF NOT EXISTS devices (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL UNIQUE,
                type TEXT NOT NULL,
                status TEXT DEFAULT 'OFF' CHECK(status IN ('ON', 'OFF')),
                value INTEGER DEFAULT 0,
                location TEXT DEFAULT 'Unknown',
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            );
            """;

        try (Connection conn = connect();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            System.out.println("✅ Device table created");
        } catch (SQLException e) {
            System.err.println("❌ Device table creation failed: " + e.getMessage());
            throw e;  // Throw the SQLException instead of RuntimeException
        }
    }

    /**
     * Create the event logs table
     */
    public static void createEventLogsTable() throws SQLException {
        String sql = """
            CREATE TABLE IF NOT EXISTS event_logs (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                device_name TEXT NOT NULL,
                action TEXT NOT NULL,
                old_value TEXT,
                new_value TEXT,
                timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (device_name) REFERENCES devices(name)
            );
            """;

        try (Connection conn = connect();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            System.out.println("✅ Event logs table created");
        } catch (SQLException e) {
            System.err.println("❌ Event logs table creation failed: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Create the power usage table
     */
    public static void createPowerUsageTable() throws SQLException {
        String sql = """
            CREATE TABLE IF NOT EXISTS power_usage (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                device_name TEXT NOT NULL,
                power_watts REAL NOT NULL,
                duration_minutes INTEGER DEFAULT 60,
                energy_kwh REAL DEFAULT 0,
                timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (device_name) REFERENCES devices(name)
            );
            """;

        try (Connection conn = connect();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            System.out.println("✅ Power usage table created");
        } catch (SQLException e) {
            System.err.println("❌ Power usage table creation failed: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Create the automation rules table
     */
    public static void createAutomationRulesTable() throws SQLException {
        String sql = """
            CREATE TABLE IF NOT EXISTS automation_rules (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                rule_name TEXT NOT NULL,
                trigger_device TEXT NOT NULL,
                trigger_condition TEXT NOT NULL,
                action_device TEXT NOT NULL,
                action_command TEXT NOT NULL,
                is_active INTEGER DEFAULT 1,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (trigger_device) REFERENCES devices(name),
                FOREIGN KEY (action_device) REFERENCES devices(name)
            );
            """;

        try (Connection conn = connect();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            System.out.println("✅ Automation rules table created");
        } catch (SQLException e) {
            System.err.println("❌ Automation rules table creation failed: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Create indexes for better performance
     */
    public static void createIndexes() throws SQLException {
        String[] indexes = {
                "CREATE INDEX IF NOT EXISTS idx_device_type ON devices(type);",
                "CREATE INDEX IF NOT EXISTS idx_event_device ON event_logs(device_name);",
                "CREATE INDEX IF NOT EXISTS idx_event_timestamp ON event_logs(timestamp);",
                "CREATE INDEX IF NOT EXISTS idx_power_device ON power_usage(device_name);",
                "CREATE INDEX IF NOT EXISTS idx_power_timestamp ON power_usage(timestamp);"
        };

        try (Connection conn = connect();
             Statement stmt = conn.createStatement()) {
            for (String indexSql : indexes) {
                stmt.execute(indexSql);
            }
            System.out.println("✅ Database indexes created");
        } catch (SQLException e) {
            System.err.println("❌ Index creation failed: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Disconnect from database
     */
    public static void disconnect(Connection conn) {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                System.err.println("❌ Failed to close connection: " + e.getMessage());
            }
        }
    }

    /**
     * Main method for testing
     */
    public static void main(String[] args) {
        System.out.println("Creating Smart Home database tables...");
        try {
            createAllTables();
            System.out.println("Database setup complete!");
        } catch (SQLException e) {
            System.err.println("Database setup failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}