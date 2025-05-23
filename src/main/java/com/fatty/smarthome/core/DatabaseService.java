package com.fatty.smarthome.core;

import com.fatty.smarthome.devices.SmartDevice;
import com.fatty.smarthome.util.SmartHomeException;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

// placeholder for now, value added
public class DatabaseService {
    private static final String LOG_FILE = "device_log.txt";
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static class LogEntry {
        private final String timestamp;
        private final String deviceName;
        private final String status;

        public LogEntry(String timestamp, String deviceName, String status) {
            this.timestamp = timestamp;
            this.deviceName = deviceName;
            this.status = status;
        }
        public String getTimestamp() {
            return timestamp;
        }
        public String getDeviceName() {
            return deviceName;
        }
        public String getStatus() {
            return status;
        }
    }


    /**
     * save(SmartDevice device) / INTENT / EXAMPLE / DEFINITIONS / PRECONDITIONS / POSTCONDITIONS
     * INTENT: Persist device status to device_log.txt in CSV format with timestamp, creating the file if it doesn't exist.
     * EXAMPLE: save(new Light("Living Room Light")) -> "[2025-05-15 10:02:03],Living Room Light,OFF" in file.
     * DEFINITIONS: device - The SmartDevice to log.
     * PRECONDITIONS: device is not null.
     * POSTCONDITIONS: Device status is appended to device_log.txt (created if absent); SmartHomeException thrown if I/O fails.
     */
    public void save(SmartDevice device) throws SmartHomeException {
        if (device == null) {
            throw new SmartHomeException("Cannot save null device");
        }
        BufferedWriter writer = null;
        try {
            // VALUE-ADDED: added the file existence check and creation logic for robust file I/O
            File logFile = new File(LOG_FILE);
            if (!logFile.exists()) {
                logFile.createNewFile();
                System.out.println("Created log file: " + LOG_FILE);
            }
            writer = new BufferedWriter(new FileWriter(LOG_FILE, true));
            // VALUE-ADDED: Added null check to prevent NullPointerException
            if (writer == null) {
                throw new SmartHomeException("Failed to initialize writer for log file: " + LOG_FILE);
            }
            String timestamp = LocalDateTime.now().format(formatter);
            String status = device.getStatus().replace(",", ""); // remove commas from status
            String logEntry = String.format("%s,%s,%s\n", timestamp, device.getName(), status);
            writer.write(logEntry);
            //VALUE-ADDED: user feedback for successful login
            System.out.println("Logged to file" + logEntry.trim());

        } catch (IOException e) {
            throw new SmartHomeException("Failed to write to log file: " + LOG_FILE, e);
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    System.out.println("Error closing writer: " + e.getMessage());
                }
            }

        }

    }
    public void clearLogFile() throws SmartHomeException {
        File file = new File(LOG_FILE);
        try {
            if (!file.exists()) {
                file.createNewFile();
                System.out.println("Created log file: " + LOG_FILE);
            }
            if (!file.canWrite()) {
                throw new SmartHomeException("Cannot write to log file: " + LOG_FILE);
            }
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(LOG_FILE))) {
                writer.write(""); // Truncates file to empty
                System.out.println("Log file cleared: " + LOG_FILE);

            }
        } catch (IOException e) {
            throw new SmartHomeException("Failed to clear log file: " + e.getMessage());
        }
    }
    public void validateLog() throws SmartHomeException {
        try (BufferedReader reader = new BufferedReader(new FileReader(LOG_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length != 3) {
                    throw new SmartHomeException("Invalid log entry: " + line);
                }
            }
        } catch (IOException e) {
            throw new SmartHomeException("Failed to validate log file: " + LOG_FILE, e);
        }
    }
    public List<LogEntry> readLog() throws SmartHomeException {
        List<LogEntry> logEntries = new ArrayList<>();
        File file = new File(LOG_FILE);
        if (!file.exists()) {
            return logEntries; // Return empty list if file doesn't exist
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(LOG_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length != 3) {
                    throw new SmartHomeException("Invalid log entry: " + line);
                }
                logEntries.add(new LogEntry(parts[0], parts[1], parts[2]));
            }
        } catch (IOException e) {
            throw new SmartHomeException("Failed to read log file: " + LOG_FILE, e);
        }
        return logEntries;
    }
}
