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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for persisting (saving) device stattes using both binary and JSON formats.
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

    public PersistenceService() {
        // Configure Gson with pretty printing and custom date handling
        this.gson = new GsonBuilder()
                .setPrettyPrinting() // Makes JSON human-readable
                .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
                .create();
        // create the data  files if they don't exist
        createFilesIfNotExist();
    }
    /**
     *
     * Creates the data files if they don't exist.
     * This ensures we don't get FileNotFoundException whren trying to read.
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
            System.err.println("Warning: Could not create data files: " + e.getMessage());;
        }
    }
    /**
     *
     * Save the states of a list of devices to a binary file.
     * @param devices List of devices to save
     * @throws SmartHomeException if save fails
     */
    public void saveDeviceStatesBinary(List<SmartDevice> devices) throws SmartHomeException {
        List<com.fatty.smarthome.core.DeviceState> states = devices.stream()
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
     *
     * Load the states of a list of devices from a binary file.
     * @return List of devices
     * @throws SmartHomeException if load fails
     */
    @SuppressWarnings("unchecked") // Suppress warnings for casting Object to List<DeviceState>
    public List<com.fatty.smarthome.core.DeviceState> loadDeviceStatesBinary() throws SmartHomeException, FileNotFoundException {
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
     *
     * Save the states of a list of devices to a JSON file.
     * @param devices List of devices to save
     * @throws SmartHomeException if save fails
     */
    public void saveDeviceStatesJson(List<SmartDevice> devices) throws SmartHomeException {
        List<com.fatty.smarthome.core.DeviceState> states = devices.stream()
                .map(this::convertToDeviceState)
                .collect(Collectors.toList());

        try (FileWriter writer = new FileWriter(JSON_FILE)) {
            gson.toJson(states, writer); // convert to JSON and write to file
            System.out.println("Saved" + states.size() + " device to JSON file.");
        } catch (IOException e) {
            throw new SmartHomeException("Failed to save devices states to JSON file: ", e);
        }
    }
    /**
     *
     * Load the states of a list of devices from a JSON file.
     * @return List of devices
     * @throws SmartHomeException if load fails
     */
    public List<com.fatty.smarthome.core.DeviceState> loadDeviceStatesJson() throws SmartHomeException {
        File file = new File(JSON_FILE);
        if (!file.exists() || file.length() == 0) {
            return new ArrayList<>();
        }
        try (FileReader reader = new FileReader(JSON_FILE)) {
            // TypeToken is used to preserve generic type information at runtime
            Type listType = new TypeToken<List<com.fatty.smarthome.core.DeviceState>>() {}.getType();
            List<com.fatty.smarthome.core.DeviceState> states = gson.fromJson(reader, listType); // convert JSON to List<DeviceState>
            return states != null ? states : new ArrayList<>();
        } catch (IOException e) {
            throw new SmartHomeException("Failed to load device states from JSON file", e);
        }
    }

    /**
     *
     * Convert a SmartDevice object to a DeviceState object.
     * @param device SmartDevice object
     * @return DeviceState object
     */
    private com.fatty.smarthome.core.DeviceState convertToDeviceState(SmartDevice device) {
        com.fatty.smarthome.core.DeviceState state = new com.fatty.smarthome.core.DeviceState(
                device.getName(),
                device.getClass().getSimpleName(),
                device.isOn,
                device.getStatus()
        );
        // Handle device specific properties
        if (device instanceof Thermostat thermostat)
            state.setTemperature(thermostat.getTemperature());
        else if (device instanceof SecurityCamera camera) {
            state.setRecording(camera.isOn);
        }
        return state;
    }
    public SmartDevice reconstructDevice(com.fatty.smarthome.core.DeviceState state) throws SmartHomeException {
        // create the appropriate device type based on the saved type name
        SmartDevice device = switch (state.getDeviceType().toLowerCase()) {
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
        }
        return device;

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
