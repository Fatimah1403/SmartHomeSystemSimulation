package com.fatty.smarthome.core;

import com.fatty.smarthome.devices.Light;
import com.fatty.smarthome.devices.SecurityCamera;
import com.fatty.smarthome.devices.SmartDevice;
import com.fatty.smarthome.devices.Thermostat;
import com.fatty.smarthome.util.SmartHomeException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class FacadeSmartHome {
    private static volatile FacadeSmartHome instance;
    private final SmartHome smartHome;
    private final List<String> commandHistory;
    // Change the devices list to thread-safe implementation
    private final List<SmartDevice> devices = new CopyOnWriteArrayList<>();

    // Add device lookup cache for performance
    private final Map<String, SmartDevice> deviceCache = new ConcurrentHashMap<>();

    private FacadeSmartHome() {
        smartHome = new SmartHome();
        commandHistory = new ArrayList<>();
    }

    /**
     * Returns the singleton instance of FacadeSmartHome.
     * @return The singleton instance
     */
    public static FacadeSmartHome getTheInstance() {
        if (instance == null) {
            synchronized (FacadeSmartHome.class) {
                if (instance == null) {
                    instance = new FacadeSmartHome();
                }
            }
        }
        return instance;
    }
    /**
     * Thread-safe method to add a device
     */
    public synchronized void addDevice(SmartDevice device) throws SmartHomeException {
        if (device == null) {
            throw new IllegalArgumentException("Device cannot be null");
        }

        // Check for duplicate
        if (deviceCache.containsKey(device.getName())) {
            throw new IllegalArgumentException("Device already exists: " + device.getName());
        }

        devices.add(device);
        deviceCache.put(device.getName(), device);

        // Also add to the internal SmartHome instance
        smartHome.addDevice(device);
        System.out.println("✅ Device added: " + device.getName());
    }

    /**
     * Thread-safe method to remove a device
     */
    public synchronized boolean removeDevice(String deviceName) {
        SmartDevice device = deviceCache.remove(deviceName);
        if (device != null) {
            devices.remove(device);
            System.out.println("✅ Device removed: " + deviceName);
            return true;
        }
        return false;
    }

    /**
     * Get device by name (thread-safe)
     */
    public Optional<SmartDevice> getDevice(String name) {
        return Optional.ofNullable(deviceCache.get(name));
    }

    /**
     *
     * VALUE-ADDED: Gets all devices for GUI display
     * Exposes device list for GUI without breaking encapsulation
     * @return List of all devices
     */
    /**
     * Get a copy of all devices (thread-safe)
     */
    public List<SmartDevice> getDevices() {
        return new ArrayList<>(devices);
    }

    /**
     * Get devices by type (thread-safe)
     */
    public List<SmartDevice> getDevicesByType(Class<? extends SmartDevice> type) {
        return devices.stream()
                .filter(type::isInstance)
                .collect(Collectors.toList());
    }

    /**
     * Execute operation on all devices (thread-safe)
     */
    public void executeOnAllDevices(java.util.function.Consumer<SmartDevice> operation) {
        devices.parallelStream().forEach(operation);
    }

    /**
     * Get device count by type
     */
    public Map<String, Long> getDeviceCountByType() {
        return devices.stream()
                .collect(Collectors.groupingBy(
                        device -> device.getClass().getSimpleName(),
                        Collectors.counting()
                ));
    }

    /**
     * Check if system has devices
     */
    public boolean hasDevices() {
        return !devices.isEmpty();
    }



    /**
     * Processes smart home commands.
     * @param command The command (e.g., add, turnOn, setTemp)
     * @param deviceName The device name
     * @param value Additional parameter (e.g., device type, temperature)
     * @return Result message
     * @throws SmartHomeException If command is invalid or fails
     */
    public String smartHomeAccess(String command, String deviceName, String value) throws SmartHomeException {
        String commandEntry = command + (deviceName.isEmpty() ? "" : " " + deviceName) + (value.isEmpty() ? "" : " " + value);
        commandHistory.add(commandEntry.trim());
        return switch (command.toLowerCase()) {
            case "add" -> {
                SmartDevice device = switch (value.toLowerCase()) {
                    case "light" -> new Light(deviceName);
                    case "thermostat" -> new Thermostat(deviceName);
                    case "camera" -> new SecurityCamera(deviceName);
                    default -> throw new SmartHomeException("Invalid device type: " + value);
                };
                smartHome.addDevice(device);
                yield "Added " + deviceName;
            }
            case "turnon" -> controlDevice(deviceName, true);
            case "turnoff" -> controlDevice(deviceName, false);
            case "settemp" -> {
                int temp;
                try {
                    temp = Integer.parseInt(value);
                } catch (NumberFormatException e) {
                    throw new SmartHomeException("Invalid temperature: " + value);
                }
                if (temp < 10 || temp > 32) {
                    throw new SmartHomeException("Temperature must be between 10 and 32°C");
                }
                for (SmartDevice d : smartHome.getDevices()) {
                    if (d instanceof Thermostat t && t.getName().equals(deviceName)) {
                        t.setTemperature(temp);
                        smartHome.saveDevice(t);
                        yield "Set " + deviceName + " to " + temp + "°C";
                    }
                }
                throw new SmartHomeException("Thermostat not found: " + deviceName);
            }
            case "automate" -> {
                smartHome.runAutomation(new com.fatty.smarthome.core.LightAutomationRule());
                yield "Automation rule applied";
            }
            case "report" -> smartHome.reportStatus();
            case "clearlog" -> {
                smartHome.clearLogFile();
                yield "Log file cleared successfully";
            }
            case "history" -> commandHistory.isEmpty() ? "No commands executed" : String.join("\n", commandHistory);
            case "readlog" -> {
                List<com.fatty.smarthome.core.DatabaseService.LogEntry> logs = smartHome.readLog();
                if (logs.isEmpty()) {
                    yield "Log file is empty";
                }
                StringBuilder logOutput = new StringBuilder();
                for (com.fatty.smarthome.core.DatabaseService.LogEntry entry : logs) {
                    logOutput.append(String.format("%s: %s - %s\n", entry.getTimestamp(), entry.getDeviceName(), entry.getStatus()));
                }
                yield logOutput.toString();
            }
            default -> throw new SmartHomeException("Invalid command: " + command);
        };
    }
    private String setTemperature(String deviceName, String value) throws SmartHomeException {
        try {
            int temp = Integer.parseInt(value);
            for (SmartDevice device : smartHome.getDevices()) {
                if (device.getName().equals(deviceName)) {
                    if (device instanceof Thermostat) {
                        int oldTemp = ((Thermostat) device).getTemperature();
                        ((Thermostat) device).setTemperature(temp);
                        int newTemp = ((Thermostat) device).getTemperature();
                        if (newTemp == oldTemp) {
                            return "Temperature unchanged: " + newTemp + "°C";
                        }
                        smartHome.saveDevice(device);
                        return "Set " + deviceName + " to " + temp + "°C";
                    }
                    throw new SmartHomeException("Device is not a thermostat: " + deviceName);
                }
            }
            throw new SmartHomeException("Device not found: " + deviceName);
        } catch (NumberFormatException e) {
            throw new SmartHomeException("Invalid temperature value: " + value);
        }
    }

    private String controlDevice(String deviceName, boolean turnOn) throws SmartHomeException {
        for (SmartDevice device : smartHome.getDevices()) {
            if (device.getName().equals(deviceName)) {
                if (turnOn) {
                    device.turnOn();
                    smartHome.saveDevice(device);
                    return deviceName + " turned ON";
                } else {
                    device.turnOff();
                    smartHome.saveDevice(device);
                    return deviceName + " turned OFF";
                }
            }
        }
        throw new SmartHomeException("Device not found: " + deviceName);
    }

//    public void reset() {
//        smartHome.reset();
//        commandHistory.clear();
//    }
    /**
     * Reset the system (thread-safe)
     */
    public synchronized void reset() {
        devices.clear();
        deviceCache.clear();
        System.out.println("✅ System reset complete");
    }
}