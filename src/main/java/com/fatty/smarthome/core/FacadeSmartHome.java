package com.fatty.smarthome.core;

import com.fatty.smarthome.devices.Light;
import com.fatty.smarthome.devices.SecurityCamera;
import com.fatty.smarthome.devices.SmartDevice;
import com.fatty.smarthome.devices.Thermostat;
import com.fatty.smarthome.util.SmartHomeException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FacadeSmartHome {
    private static volatile FacadeSmartHome instance;
    private final SmartHome smartHome;
    private final List<String> commandHistory;

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
     *
     * VALUE-ADDED: Gets all devices for GUI display
     * Exposes device list for GUI without breaking encapsulation
     * @return List of all devices
     */
    public List<SmartDevice> getDevices() {
        return smartHome.getDevices();
    }
    public Optional<SmartDevice> getDevice(String name) {
        return smartHome.getDevices().stream()
                .filter(device -> device.getName().equals(name))
                .findFirst();
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

    public void reset() {
        smartHome.reset();
        commandHistory.clear();
    }
}
