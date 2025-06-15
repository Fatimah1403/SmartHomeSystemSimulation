package com.fatty.smarthome.core;


import com.fatty.smarthome.devices.Light;
import com.fatty.smarthome.devices.SecurityCamera;
import com.fatty.smarthome.devices.SmartDevice;
import com.fatty.smarthome.devices.Thermostat;
import com.fatty.smarthome.util.SmartHomeException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for persisting (saving) device states using both binary and JSON formats.
 * This allows the application to save its state and restore it later
 */
public class PersistenceService {
    // file names for storing data
    private static final String BINARY_FILE = "devices_states.dat";
    private static final String JSON_FILE = "devices_states.json";
    private static final String AUTOMATION_FILE = "automation_rules.dat";

    /**
     *
     */
    // Gson instance for JSON conversion
    private final Gson gson;

    // Database service for enhanced persistence
    private final DatabaseService dbService;
    private boolean useDatabasePrimary = true;

    public PersistenceService() throws SQLException {
        // Configure Gson with pretty printing and custom date handling
        this.gson = new GsonBuilder()
                .setPrettyPrinting() // Makes JSON human-readable
                .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
                .create();

        // Initialize database service
        this.dbService = DatabaseService.getInstance();
        // create the data  files if they don't exist
        createFilesIfNotExist();
    }/**
     * Check if database is available and operational
     * @return true if database is available, false otherwise
     */
    public boolean isDatabaseAvailable() {
        try {
            // Check if database service exists and is operational
            if (dbService != null) {
                // Try a simple operation to verify connection
                dbService.getSystemSummary();
                return true;
            }
        } catch (Exception e) {
            // Database is not available
        }
        return false;
    }
    /**
     * Load device states directly from database
     * @return List of device states from database
     * @throws SmartHomeException if database operation fails
     */
    public List<DeviceState> loadDeviceStatesFromDatabase() throws SmartHomeException {
        if (!isDatabaseAvailable()) {
            return new ArrayList<>();
        }

        try {
            List<SmartDevice> devices = dbService.loadAllDevices();
            // Convert SmartDevice objects to DeviceState objects
            return devices.stream()
                    .map(this::convertToDeviceState)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new SmartHomeException("Failed to load device states from database", e);
        }
    }

    /**
     * Save device states directly to database
     * @param devices List of devices to save
     * @throws SmartHomeException if database operation fails
     */
    public void saveDeviceStatesToDatabase(List<SmartDevice> devices) throws SmartHomeException {
        if (!isDatabaseAvailable()) {
            throw new SmartHomeException("Database is not available");
        }

        try {
            dbService.saveAllDevices(devices);
            System.out.println("Saved " + devices.size() + " devices to database.");
        } catch (Exception e) {
            throw new SmartHomeException("Failed to save devices to database", e);
        }
    }


    /**
     *
     * Creates the data files if they don't exist.
     * This ensures we don't get FileNotFoundException when trying to read.
     */
    private void createFilesIfNotExist() {
        try {
            // Create binary file if it doesn't exist
            Path binaryPath = Paths.get(BINARY_FILE);
            if (!Files.exists(binaryPath)) {
                Files.createFile(binaryPath);
                System.out.println("Created binary file: " + BINARY_FILE);
            }
            // Create JSON file if it doesn't exist
            Path jsonPath = Paths.get(JSON_FILE);
            if (!Files.exists(jsonPath)) {
                Files.createFile(jsonPath);
                System.out.println("Created JSON file: " + JSON_FILE);
            }
            // Create automation file if it doesn't exist
            Path automationPath = Paths.get(AUTOMATION_FILE);
            if (!Files.exists(automationPath)) {
                Files.createFile(automationPath);
                System.out.println("Created automation file: " + AUTOMATION_FILE);
            }

        } catch (IOException e) {
            System.err.println("Warning: Could not create data files: " + e.getMessage());
        }
    }
    /**
     *
     * Save the states of a list of devices to a binary file.
     * @param devices List of devices to save
     * @throws SmartHomeException if save fails
     */
    public void saveDeviceStatesBinary(List<SmartDevice> devices) throws SmartHomeException {
        // First, save to database
        try {
            dbService.saveAllDevices(devices);
        } catch (Exception e) {
            System.err.println("Warning: Database save failed, falling back to file only: " + e.getMessage());
        }

        // Then save to binary file as backup
        List<DeviceState> states = devices.stream()
                .map(this::convertToDeviceState)
                .collect(Collectors.toList());

        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(BINARY_FILE)))  {
            oos.writeObject(states); // convert to JSON and write to file
            System.out.println("Saved" + states.size() + " device to binary file.");
        } catch (IOException e) {
            throw new SmartHomeException("Failed to save devices states to binary file: ", e);
        }
    }
    /**
     * Load the states of a list of devices from a binary file.
     * Now tries database first, then falls back to file
     * @return List of devices
     * @throws SmartHomeException if load fails
     */
    @SuppressWarnings("unchecked") // Suppress warnings for casting Object to List<DeviceState>
    public List<DeviceState> loadDeviceStatesBinary() throws SmartHomeException {
        // Try database first if enabled
        if (useDatabasePrimary) {
            try {
                List<SmartDevice> dbDevices = dbService.loadAllDevices();
                if (!dbDevices.isEmpty()) {
                    // Convert SmartDevice to DeviceState for compatibility
                    return dbDevices.stream()
                            .map(this::convertToDeviceState)
                            .collect(Collectors.toList());
                }
            } catch (Exception e) {
                System.err.println("Database load failed, falling back to file: " + e.getMessage());
            }
        }
        // Fall back to file loading
        File file = new File(BINARY_FILE);

        // Return empty list if file doesn't exist or is empty
        if (!file.exists() || file.length() == 0){
            return new ArrayList<>();
        }
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(BINARY_FILE))) {
            // Read and cast the object back to List<DeviceState>
            return (List<com.fatty.smarthome.core.DeviceState>) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new SmartHomeException("Failed to load device states from binary file", e);
        }
    }
    /**
     * Save the states of a list of devices to a JSON file.
     * Now also saves to database
     * @param devices List of devices to save
     * @throws SmartHomeException if save fails
     */
    public void saveDeviceStatesJson(List<SmartDevice> devices) throws SmartHomeException {
        // First, save to database
        try {
            dbService.saveAllDevices(devices);
        } catch (Exception e) {
            System.err.println("Warning: Database save failed, falling back to file only: " + e.getMessage());
        }

        // Then save to JSON file as backup
        List<DeviceState> states = devices.stream()
                .map(this::convertToDeviceState)
                .collect(Collectors.toList());

        try (FileWriter writer = new FileWriter(JSON_FILE)) {
            gson.toJson(states, writer); // convert to JSON and write to file
            System.out.println("Saved " + states.size() + " devices to JSON file.");
        } catch (IOException e) {
            throw new SmartHomeException("Failed to save devices states to JSON file: ", e);
        }
    }
    /**
     * Load the states of a list of devices from a JSON file.
     * Now tries database first, then falls back to file
     * @return List of devices
     * @throws SmartHomeException if load fails
     */
    public List<DeviceState> loadDeviceStatesJson() throws SmartHomeException {
        // Try database first if enabled
        if (useDatabasePrimary) {
            try {
                List<SmartDevice> dbDevices = dbService.loadAllDevices();
                if (!dbDevices.isEmpty()) {
                    // Convert SmartDevice to DeviceState for compatibility
                    return dbDevices.stream()
                            .map(this::convertToDeviceState)
                            .collect(Collectors.toList());
                }
            } catch (Exception e) {
                System.err.println("Database load failed, falling back to file: " + e.getMessage());
            }
        }

        // Fall back to JSON file loading
        File file = new File(JSON_FILE);
        if (!file.exists() || file.length() == 0) {
            return new ArrayList<>();
        }
        try (FileReader reader = new FileReader(JSON_FILE)) {
            // TypeToken is used to preserve generic type information at runtime
            Type listType = new TypeToken<List<DeviceState>>() {}.getType();
            List<DeviceState> states = gson.fromJson(reader, listType); // convert JSON to List<DeviceState>
            return states != null ? states : new ArrayList<>();
        } catch (IOException e) {
            throw new SmartHomeException("Failed to load device states from JSON file", e);
        }
    }

    /**
     * Convert a SmartDevice object to a DeviceState object.
     * @param device SmartDevice object
     * @return DeviceState object
     */
    private DeviceState convertToDeviceState(SmartDevice device) {
        DeviceState state = new DeviceState(
                device.getName(),
                device.getClass().getSimpleName(),
                device.isOn(),
                device.getStatus()
        );
        // Handle device specific properties
        if (device instanceof Thermostat thermostat)
            state.setTemperature(thermostat.getTemperature());
        else if (device instanceof SecurityCamera camera) {
            state.setRecording(camera.isOn());
        }
        return state;
    }
    /**
     * Reconstruct a SmartDevice from a DeviceState
     */
    public SmartDevice reconstructDevice(DeviceState state) throws SmartHomeException {
        // create the appropriate device type based on the saved type name

        String deviceType = state.getDeviceType().toLowerCase();

        SmartDevice device = switch (deviceType) {
            case "light" -> new Light(state.getDeviceName());
            case "thermostat" -> {
                Thermostat t = new Thermostat(state.getDeviceName());
                if (state.getTemperature() != null) {
                    t.setTemperature(state.getTemperature());
                }
                yield t;
            }
            case "securitycamera" -> new SecurityCamera(state.getDeviceName());
            default -> throw new SmartHomeException("Unknown device type: " + state.getDeviceType());
        };

        // Restore the on/off state
        if (state.isOn()) {
            device.turnOn();
        } else {
            device.turnOff();
        }
        return device;
    }
    /**
     * Load devices directly as SmartDevice objects (for database integration)
     */
    public List<SmartDevice> loadDevicesDirectly() throws SmartHomeException {
        // Try database first
        try {
            List<SmartDevice> devices = dbService.loadAllDevices();
            if (!devices.isEmpty()) {
                return devices;
            }
        } catch (Exception e) {
            System.err.println("Database load failed: " + e.getMessage());
        }

        // Fall back to loading from JSON and reconstructing
        List<DeviceState> states = loadDeviceStatesJson();
        List<SmartDevice> devices = new ArrayList<>();

        for (DeviceState state : states) {
            try {
                devices.add(reconstructDevice(state));
            } catch (Exception e) {
                System.err.println("Failed to reconstruct device: " + state.getDeviceName());
            }
        }

        return devices;
    }
    /**
     * Save all devices
     */
    public void saveAllDevices(List<SmartDevice> devices) throws SmartHomeException {
        saveDeviceStatesJson(devices);
        saveDeviceStatesBinary(devices);
    }

    /**
     * NEW METHOD: Log device action to database
     */
    public void logDeviceAction(SmartDevice device, String action) {
        if (dbService != null) {
            String oldValue = device.isOn() ? "ON" : "OFF";
            String newValue = action.contains("ON") ? "ON" : "OFF";
            dbService.logAction(device.getName(), action, oldValue, newValue);
        }
    }
    /**
     * NEW METHOD: Get device statistics from database
     */
    public String getDeviceStatistics(String deviceName) {
        if (dbService != null) {
            var stats = dbService.getDeviceStatistics(deviceName);
            StringBuilder sb = new StringBuilder();
            sb.append("Statistics for ").append(deviceName).append(":\n");
            stats.forEach((key, value) ->
                    sb.append("  ").append(key).append(": ").append(value).append("\n"));
            return sb.toString();
        }
        return "Database not available";
    }

    /**
     * NEW METHOD: Get system summary from database
     */
    public String getSystemSummary() {
        if (dbService != null) {
            return dbService.getSystemSummary();
        }
        return "Database not available";
    }

    /**
     * NEW METHOD: Save automation rule to database
     */
    public void saveAutomationRule(String ruleName, String triggerDevice, String triggerCondition,
                                   String actionDevice, String actionCommand) throws SmartHomeException {
        if (dbService != null) {
            dbService.saveAutomationRule(ruleName, triggerDevice, triggerCondition,
                    actionDevice, actionCommand);
        }
    }

    /**
     * NEW METHOD: Save power usage data
     */
    public void logPowerUsage(String deviceName, double powerWatts) {
        if (dbService != null) {
            dbService.savePowerUsage(deviceName, powerWatts);
        }
    }

    /**
     * NEW METHOD: Set whether to use database as primary storage
     */
    public void setUseDatabasePrimary(boolean useDatabase) {
        this.useDatabasePrimary = useDatabase;
        System.out.println(useDatabase ?
                "📊 Using database as primary storage" :
                "📁 Using files as primary storage");
    }

    /**
     *
     * Custom adapter for LocalDateTime serialization in JSON.
     * This is needed because Gson doesn't know how to handle LocalDateTime by default.
     */
    private static class LocalDateTimeAdapter implements com.google.gson.JsonSerializer<LocalDateTime>,
            com.google.gson.JsonDeserializer<LocalDateTime> {

        @Override
        public com.google.gson.JsonElement serialize(LocalDateTime src, Type typeOfSrc,
                                                     com.google.gson.JsonSerializationContext context) {
            return new com.google.gson.JsonPrimitive(src.toString());
        }

        @Override
        public LocalDateTime deserialize(com.google.gson.JsonElement json, Type typeOfT,
                                         com.google.gson.JsonDeserializationContext context) {
            return LocalDateTime.parse(json.getAsString());
        }
    }
}
